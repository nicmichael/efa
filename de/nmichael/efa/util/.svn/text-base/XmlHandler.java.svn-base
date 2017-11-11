/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.util;

import de.nmichael.efa.Daten;
import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;
//
public class XmlHandler extends DefaultHandler {

    public static final String ENCODING = Daten.ENCODING_UTF;
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"" + ENCODING + "\"?>";

    protected Locator locator;
    protected String xmlRootElement;
    protected boolean documentComplete = false;
    protected Stack<String> xmlStack = new Stack<String>();
    protected String parentFieldName;
    protected String fieldName;
    private StringBuilder fieldValue;

    public XmlHandler(String xmlRootElement) {
        super();
        this.xmlRootElement = xmlRootElement;
    }

    public boolean isDocumentComplete() {
        return documentComplete;
    }

    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    public String getLocation() {
        return locator.getSystemId() + ":" + EfaUtil.int2String(locator.getLineNumber(), 4) + ":" + EfaUtil.int2String(locator.getColumnNumber(), 4) + ": ";
    }

    public void startDocument() {
        if (Logger.isTraceOn(Logger.TT_XMLFILE, 9)) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLTRACE, "Positions: <SystemID>:<LineNumber>:<ColumnNumber>");
        }
        if (Logger.isTraceOn(Logger.TT_XMLFILE, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLTRACE, getLocation() + "startDocument()");
        }
        documentComplete = false;
    }

    public void endDocument() {
        if (Logger.isTraceOn(Logger.TT_XMLFILE, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLTRACE, getLocation() + "endDocument()");
        }
        if (!documentComplete) {
            Logger.log(Logger.ERROR, Logger.MSG_FILE_XMLFILEINCOMPLETE,
                    getLocation() +
                    International.getString("Unvollständiges XML-Dokument gelesen"));
        }
    }

    public void startElement(String uri, String localName, String qname, Attributes atts) {
        if (Logger.isTraceOn(Logger.TT_XMLFILE, 9)) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLTRACE, getLocation() + "startElement(uri=" + uri + ", localName=" + localName + ", qname=" + qname + ")");
        }
        if (Logger.isTraceOn(Logger.TT_XMLFILE, 9)) {
            for (int i = 0; i < atts.getLength(); i++) {
                Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLTRACE, "\tattribute: uri=" + atts.getURI(i) + ", localName=" + atts.getLocalName(i) + ", qname=" + atts.getQName(i) + ", value=" + atts.getValue(i) + ", type=" + atts.getType(i));
            }
        }
        fieldName = localName;
        parentFieldName = (!xmlStack.empty() ? xmlStack.peek() : null);
        xmlStack.push(localName);
        fieldValue = new StringBuilder();
    }

    public void characters(char[] ch, int start, int length) {
        String s = new String(ch, start, length); // .trim(); // trimming here would wrongly trim away spaces within strings which are composed out of several characters() invocations!
        if (Logger.isTraceOn(Logger.TT_XMLFILE, 9)) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLTRACE, getLocation() + "characters(" + s + ")");
        }

        if (fieldName != null) {
            fieldValue.append(s);
        }
    }

    public void endElement(String uri, String localName, String qname) {
        if (Logger.isTraceOn(Logger.TT_XMLFILE, 9)) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLTRACE, getLocation() + "endElement(" + uri + "," + localName + "," + qname + ")");
        }
        fieldName = localName;
        xmlStack.pop();
        parentFieldName = (!xmlStack.empty() ? xmlStack.peek() : null);

        if (parentFieldName == null && localName.equals(xmlRootElement)) {
            // end of document
            documentComplete = true;
        }
    }

    protected String getFieldValue() {
        return (fieldValue != null ?
            fieldValue.toString().trim() : null);
    }
}
