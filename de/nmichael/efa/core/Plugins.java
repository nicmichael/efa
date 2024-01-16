/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.FTPClient;
import de.nmichael.efa.util.Help;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.PDFWriter;
import de.nmichael.efa.util.XmlHandler;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Hashtable;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class Plugins {

    public static final String PLUGIN_FTP = "ftp";
    public static final String PLUGIN_HELP = "help";
    public static final String PLUGIN_JSUNTIMES = "jsuntimes";
    public static final String PLUGIN_MAIL = "mail";
    public static final String PLUGIN_PDF = "pdf";
    public static final String PLUGIN_WEATHER = "weather";
    public static final String PLUGIN_FLATLAF = "flatlaf";

    private Hashtable<String,PluginInfo> pluginInfos;

    public Plugins(Hashtable<String,PluginInfo> pluginInfos) {
        this.pluginInfos = pluginInfos;
    }

    public String[] getAllPluginNames() {
        String[] keys = pluginInfos.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        return keys;
    }

    public PluginInfo getPluginInfo(String pluginName) {
        return pluginInfos.get(pluginName);
    }

    public static boolean isPluginInstalled(String pluginName) {
        if (pluginName == null) {
            return false;
        }

        if (pluginName.equals(PLUGIN_FTP)) {
            try {
                FTPClient tmp = new FTPClient(null, null, null, null, null, null,0);
                tmp.runUpload();
                return true;
            } catch (NoClassDefFoundError e) {
                return false;
            } catch (Exception e) {
            	// we just catch the exception instead of logging it.
            	// there can be no successful upload with the ftp client with the parameters given.
            	EfaUtil.foo();
            }
        }

        if (pluginName.equals(PLUGIN_HELP)) {
            try {
                Help.getHelpSet();
                return true;
            } catch (NoClassDefFoundError e) {
                return false;
            }
        }

        if (pluginName.equals(PLUGIN_JSUNTIMES)) {
            try {
                de.nmichael.efa.util.SunRiseSet tmp = new de.nmichael.efa.util.SunRiseSet();
                return true;
            } catch (NoClassDefFoundError e) {
                return false;
            }
        }

        if (pluginName.equals(PLUGIN_MAIL)) {
            try {
                de.nmichael.efa.core.EmailSenderThread tmp = new de.nmichael.efa.core.EmailSenderThread();
                return true;
            } catch (NoClassDefFoundError e) {
                return false;
            }
        }

        if (pluginName.equals(PLUGIN_PDF)) {
            try {
                PDFWriter tmp = new PDFWriter(null, null);
                return true;
            } catch (NoClassDefFoundError e) {
                return false;
            }
        }

        if (pluginName.equals(PLUGIN_WEATHER)) {
            try {
                oauth.signpost.basic.DefaultOAuthConsumer tmp = new oauth.signpost.basic.DefaultOAuthConsumer("foo", "bar");
                return true;
            } catch (NoClassDefFoundError e) {
                return false;
            }
        }
        
        if (pluginName.equals(PLUGIN_FLATLAF)) {
        	// During initialization of efa programs it is determined if flatlaf library is actually present.
        	// a failure mostly comes from a missing flatlaf-3.2.5.jar in the classpath.
        	return Daten.flatLafInitializationOK;
        }        

        return false;
    }

    public static Plugins getPluginInfoFromLocalFile() {
        try {
            XMLReader parser = EfaUtil.getXMLReader();
            PluginInfoFileParser pp = new PluginInfoFileParser();
            parser.setContentHandler(pp);
            InputStream in = Plugins.class.getResourceAsStream(Daten.FILEPATH + Daten.PLUGIN_INFO_FILE);
            parser.parse(new InputSource(in));
            return new Plugins(pp.getPlugins());
        } catch (Exception e) {
            Logger.logdebug(e);
            Dialog.error(e.getMessage());
            return null;
        }
    }

    public Hashtable<String, String> getPluginHtmlInfos() {
        Hashtable<String, String> items = new Hashtable<String, String>();
        String[] names = getAllPluginNames();
        for (String name : names) {
            PluginInfo p = getPluginInfo(name);
            StringBuilder s = new StringBuilder();
            boolean installed = p.isInsalled();
            s.append("<html><table width=\"100%\" bgcolor=\"#" +  (installed ? "ccffcc" : "ffcccc") + "\"><tr><td><font color=#000000");
            s.append("<b>" + International.getString("Plugin") + ": " + name + " (" + p.getFullName()+ ")</b>");
            s.append(" - " + International.getString("Version") + ": " + p.getVersion() + "<br>");
            s.append(p.getCopyright().replaceAll("(\\r\\n|\\n)", "<br>") + "<br>");
            s.append("<i>" + p.getDescription().replaceAll("(\\r\\n|\\n)", "<br>") + "</i><br>");
            s.append("</font></td></tr></table></html>");
            items.put(name, s.toString());
        }
        return items;
    }

}

class PluginInfoFileParser extends XmlHandler {

    public static String XML_PLUGINS = "Plugins";
    public static String XML_PLUGIN = "Plugin";
    public static String XML_PLUGIN_NAME = "name";
    public static String XML_FULLNAME = "FullName";
    public static String XML_VERSION = "Version";
    public static String XML_DESCRIPTION = "Description";
    public static String XML_DESCRIPTION_LANG = "lang";
    public static String XML_COPYRIGHT = "Copyright";
    public static String XML_FILES = "Files";
    public static String XML_FILES_BASEURL = "baseurl";
    public static String XML_FILE = "File";
    public static String XML_FILE_SIZE = "size";

    private PluginInfo plugin;
    private Hashtable<String,PluginInfo> plugins = new Hashtable<String,PluginInfo>();
    private String descrlang;
    private int filesize;

    public PluginInfoFileParser() {
        super(XML_PLUGINS);
    }

    public Hashtable<String,PluginInfo> getPlugins() {
        return plugins;
    }

    public void startElement(String uri, String localName, String qname, Attributes atts) {
        super.startElement(uri, localName, qname, atts);

        if (localName.equals(XML_PLUGIN)) {
            plugin = new PluginInfo(atts.getValue(XML_PLUGIN_NAME));
        }

        if (plugin != null && localName.equals(XML_DESCRIPTION)) {
            descrlang = atts.getValue(XML_DESCRIPTION_LANG);
        }

        if (plugin != null && localName.equals(XML_FILES)) {
            plugin.setBaseUrl(atts.getValue(XML_FILES_BASEURL));
        }

        if (plugin != null && localName.equals(XML_FILE)) {
            filesize = EfaUtil.string2int(atts.getValue(XML_FILE_SIZE), 0);
        }
    }

    public void endElement(String uri, String localName, String qname) {
        super.endElement(uri, localName, qname);

        if (plugin != null) {
            // end of field
            
            if (fieldName.equals(XML_PLUGIN)) {
                plugins.put(plugin.getName(), plugin);
                plugin = null;
            }

            if (fieldName.equals(XML_FULLNAME)) {
                plugin.setFullName(getFieldValue());
            }

            if (fieldName.equals(XML_VERSION)) {
                plugin.setVersion(getFieldValue());
            }

            if (fieldName.equals(XML_DESCRIPTION)) {
                if (descrlang != null) {
                    if (International.getLanguageID().equals(descrlang) ||
                        (descrlang.equals("en") && plugin.getDescription() == null)) {
                        plugin.setDescription(getFieldValue());
                    }
                }
            }

            if (fieldName.equals(XML_COPYRIGHT)) {
                plugin.setCopyright(getFieldValue());
            }

            if (fieldName.equals(XML_FILE)) {
                plugin.addFile(getFieldValue(), filesize);
            }
        }

    }

}
