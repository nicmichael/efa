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

import de.nmichael.efa.util.*;
import de.nmichael.efa.Daten;
import java.util.Vector;
import javax.swing.*;
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

// @i18n complete

public class XSLTWriter extends DefaultHandler {

  BufferedWriter f;
  ElwizOption option = null;
  Vector options;
  boolean skip = false;


  // Konstruktor
  // options: Vector aller Optionen
  // fileto : Ausgabedatei
  public XSLTWriter(Vector options, String fileto) throws IOException {
    super();
    this.options = options;
    f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileto),Daten.ENCODING_UTF));
  }


  // Anfang des Dokuments
  public void startDocument() {
    try { f.write("<?xml version=\"1.0\" encoding=\""+Daten.ENCODING_UTF+"\"?>\n"); } catch(IOException e) { EfaUtil.foo(); }
  }


  // Ende des Dokuments
  public void endDocument() {
    try { f.close(); } catch(IOException e) { EfaUtil.foo(); }
  }


  // Beginn eines Element-Tags
  public void startElement(String uri, String localName, String qname, Attributes atts) {
    try {
      if (uri.equals("http://www.nmichael.de/elwiz")) {

      // Elemente des elwiz-Namensraums

        if (!localName.equals("option")) { // Hauptelement
          // Namen ermitteln
          String name = null;
          for (int i=0; i<atts.getLength(); i++)
            if (atts.getLocalName(i).equals("name")) name = atts.getValue(i);

          // Element suchen
          for (int i=0; i<options.size(); i++)
            if (((ElwizOption)options.get(i)).name.equals(name)) {
              this.option = (ElwizOption)options.get(i);
              break;
            }

        } else { // Unterelement

          // Position des Unterelements für Zugriff auf options-Vektor ermitteln
          int pos = -1;
          for (int i=0; i<atts.getLength(); i++)
            if (atts.getLocalName(i).equals("pos")) pos = EfaUtil.string2int(atts.getValue(i),-1);

          if (option != null)
          switch (option.type) {
            case ElwizOption.O_OPTIONAL: // optional
              JCheckBox o1 = (JCheckBox)option.components.get(pos);
              if (!o1.isSelected()) skip = true;
              break;
            case ElwizOption.O_SELECT: // select
              JRadioButton o2 = (JRadioButton)option.components.get(pos);
              if (!o2.isSelected()) skip = true;
              ElwizSingleOption eso = (ElwizSingleOption)option.options.get(pos);
              if (!skip && eso.value != null) f.write(eso.value);
              break;
            case ElwizOption.O_VALUE: // value
              JTextField o3 = (JTextField)option.components.get(pos);
              f.write(o3.getText().trim());
              break;
          }

        }

      } else {

        // Elemente des XSLT-Namensraums

        if (skip) return; // Element unterdrücken?

        f.write("<"+qname);
        for (int i=0; i<atts.getLength(); i++) {
          f.write(" "+atts.getLocalName(i)+"=\""+EfaUtil.replace(atts.getValue(i),"<","&lt;",true)+"\"");
        }
        if (localName.equals("stylesheet")) f.write(" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:fo=\"http://www.w3.org/1999/XSL/Format\"");
        f.write(">");
      }
    } catch(IOException e) {}
  }


  // Ende eines Elements
  public void endElement(String uri, String localName, String qname) {
    try {
      if (uri.equals("http://www.nmichael.de/elwiz")) { // elwiz-Element
        if (option != null && option.name.equals(localName)) option = null;
        skip = false;
      } else { // Normales Element
        if (skip) return;

        f.write("</"+qname+">");
      }
    } catch(IOException e) {}
  }


  // Text innerhalb von Elementen
  public void characters(char[] ch, int start, int length) {
    if (skip) return;
    try {
      String s = new String(ch,start,length);
      if (s.indexOf('\n')>=0 || s.indexOf(10)>=0 || s.indexOf(13)>=0) s = s.trim();
      if (s.length()>0) f.write(s);
    } catch(IOException e) {}
  }


  // Startmethode
  public static boolean run(String filename, String fileto, Vector allOptions) {
    if (!fileto.toLowerCase().endsWith(".xsl")) fileto += ".xsl";

    XMLReader parser;
    parser = XSLTReader.tryToSetup(null);
    if (parser == null) parser = XSLTReader.tryToSetup("org.apache.xerces.parsers.SAXParser");
    if (parser == null) parser = XSLTReader.tryToSetup("javax.xml.parsers.SAXParser"); // Java 1.5
    if (parser == null) parser = XSLTReader.tryToSetup("org.apache.crimson.parser.XMLReaderImpl");

    try {
      parser.setContentHandler(new XSLTWriter(allOptions, fileto));
      parser.parse(filename);
    } catch(Exception e) {
      Logger.log(Logger.ERROR, Logger.MSG_DEBUG_ELWIZ,
              "PARSER EXCEPTION: "+e);
    }

    return true;
  }

}