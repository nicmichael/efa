/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.elwiz;

import de.nmichael.efa.util.Logger;
import java.util.Vector;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

// @i18n complete

public class XSLTReader extends DefaultHandler {

  static Vector allOptions;


  // Konstruktor
  public XSLTReader() {
    super();
  }


  // Start des Elements
  public void startElement(String uri, String localName, String qname, Attributes atts) {
    if (uri.equals("http://www.nmichael.de/elwiz")) {

      if (!localName.equals("option")) {
        ElwizOption o = new ElwizOption();

        // Typ
        if (localName.equals("optional")) o.type = ElwizOption.O_OPTIONAL;
        if (localName.equals("select")) o.type = ElwizOption.O_SELECT;
        if (localName.equals("value")) o.type = ElwizOption.O_VALUE;

        // Attribute
        for (int i=0; i<atts.getLength(); i++) {
          // Name
          if (atts.getLocalName(i).equals("name")) o.name = atts.getValue(i);

          // Description
          if (atts.getLocalName(i).equals("descr")) o.descr = atts.getValue(i);

        }
        allOptions.add(o);

      } else {

          ElwizSingleOption eso = new ElwizSingleOption();
          for (int i=0; i<atts.getLength(); i++) {
            if (atts.getLocalName(i).equals("descr")) eso.descr = atts.getValue(i);
            if (atts.getLocalName(i).equals("value")) eso.value = atts.getValue(i);
            if (atts.getLocalName(i).equals("selected")) eso.selected = atts.getValue(i).equals("true");
            if (atts.getLocalName(i).equals("type")) eso.type = atts.getValue(i);
          }
          if (((ElwizOption)allOptions.lastElement()).options == null)
            ((ElwizOption)allOptions.lastElement()).options = new Vector();

          ((ElwizOption)allOptions.lastElement()).options.add(eso);
      }
    }
  }

  static XMLReader tryToSetup(String className) {
    XMLReader parser = null;
    try {
      if (className != null) {
          if (Logger.isTraceOn(Logger.TT_XMLFILE)) {
              Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_ELWIZ,
                "Trying to load XML-Parser "+className+" ...");
          }
        parser = XMLReaderFactory.createXMLReader(className);
      } else {
          if (Logger.isTraceOn(Logger.TT_XMLFILE)) {
              Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_ELWIZ,
                      "Trying to load default XML-Parser ...");
          }
        parser = XMLReaderFactory.createXMLReader();
      }
    } catch(Exception e) {
      Logger.log(Logger.ERROR, Logger.MSG_DEBUG_ELWIZ,
              "PARSER EXCEPTION: "+e.toString());
      if (e.getClass().toString().indexOf("java.lang.ClassNotFoundException")>0) {
        Logger.log(Logger.ERROR, Logger.MSG_DEBUG_ELWIZ,
                className+" not found.");
        parser = null;
      }
    }
    Logger.log(Logger.INFO, Logger.MSG_DEBUG_ELWIZ,
            "XML-Parser successfully loaded.");
    return parser;
  }


  public static Vector run(String filename) {

    allOptions = new Vector();

    Logger.log(Logger.INFO, Logger.MSG_DEBUG_ELWIZ,
            "XSLTReader: Reading "+filename+" ...");

    XMLReader parser;
    parser = tryToSetup(null);
    if (parser == null) parser = tryToSetup("org.apache.xerces.parsers.SAXParser");
    if (parser == null) parser = tryToSetup("javax.xml.parsers.SAXParser"); // Java 1.5
    if (parser == null) parser = tryToSetup("org.apache.crimson.parser.XMLReaderImpl");
    if (parser == null) {
      Logger.log(Logger.ERROR, Logger.MSG_DEBUG_ELWIZ,
              "No XML-Parser found!");
      return null;
    }

    try {
      parser.setContentHandler(new XSLTReader());
      parser.parse(filename);
    } catch(Exception e) {
      Logger.log(Logger.ERROR, Logger.MSG_DEBUG_ELWIZ,
              "PARSER EXCEPTION: "+e.toString());
    }
    if (Logger.isTraceOn(Logger.TT_XMLFILE)) {
        Logger.log(Logger.DEBUG,Logger.MSG_DEBUG_ELWIZ,
            "XSLTReader: "+allOptions.size()+" elements read.");
    }

/*
    for (int i=0; i<allOptions.size(); i++) {
      ElwizOption o = (ElwizOption)allOptions.get(i);
      System.out.println(o.type+": "+o.name+"; "+o.descr);
      if (o.options != null)
        for (int j=0; j<o.options.size(); j++) {
          ElwizSingleOption eso = (ElwizSingleOption)o.options.get(j);
          System.out.println(eso.descr+"; "+eso.value+"; "+eso.selected);
        }
    }
*/

    return allOptions;
  }

}