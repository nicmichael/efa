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
import java.util.Vector;

public class PluginInfo {

    private String name;
    private String fullName;
    private String version;
    private String description;
    private String copyright;
    private String baseurl;
    private Vector<PluginFile> files = new Vector<PluginFile>();

    public PluginInfo(String name) {
        this.name = (name != null ? name.trim() : null);
    }

    public String getName() {
        return name;
    }

    public void setFullName(String fullName) {
        this.fullName = (fullName != null ? fullName.trim() : null);
    }

    public String getFullName() {
        return fullName;
    }

    public void setVersion(String version) {
        this.version = (version != null ? version.trim() : null);
    }

    public String getVersion() {
        return version;
    }

    public void setDescription(String description) {
        this.description = (description != null ? description.trim() : null);
    }

    public String getDescription() {
        return description;
    }

    public void setCopyright(String copyright) {
        this.copyright = (copyright != null ? copyright.trim() : null);
    }

    public String getCopyright() {
        return copyright;
    }

    public void setBaseUrl(String url) {
        this.baseurl = url;
    }

    public String getBaseUrl() {
        return baseurl;
    }

    public void addFile(String filename, int filesize) {
        files.add(new PluginFile(filename, filesize));
    }

    public Vector<PluginFile> getFiles() {
        return files;
    }

    public String[] getDownloadFileNames() {
        String[] names = new String[files.size()];
        for (int i=0; i<names.length; i++) {
            names[i] = baseurl + (baseurl.endsWith("/") ? "" : "/") + files.get(i).getName();
        }
        return names;
    }

    public String[] getLocalFileNames() {
        String[] names = new String[files.size()];
        for (int i=0; i<names.length; i++) {
            names[i] = Daten.efaPluginDirectory + files.get(i).getName();
        }
        return names;
    }

    public int[] getFileSizes() {
        int[] sizes = new int[files.size()];
        for (int i=0; i<sizes.length; i++) {
            sizes[i] = files.get(i).getSize();
        }
        return sizes;
    }
    
    public int getTotalSize() {
        int size = 0;
        for (int i=0; files != null && i<files.size(); i++) {
            size += files.get(i).getSize();
        }
        return size;
    }

    public boolean isInsalled() {
        return Plugins.isPluginInstalled(getName());
    }

    class PluginFile {
        private String filename;
        private int size;

        public PluginFile(String filename, int size) {
            this.filename = filename;
            this.size = size;
        }

        public String getName() {
            return filename;
        }

        public int getSize() {
            return size;
        }
    }

}
