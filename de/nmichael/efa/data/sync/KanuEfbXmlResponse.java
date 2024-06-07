/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.sync;

import de.nmichael.efa.util.*;
import java.util.*;
import org.xml.sax.*;

// @i18n complete

public class KanuEfbXmlResponse extends XmlHandler {

    public static String ROOT_ELEMENT = "xml";
    public static String LOGIN = "header"; // pseudo-field, only used for internal Hashtable

    private KanuEfbSyncTask efb;
    private Locator locator;

    private Vector<Hashtable<String,String>> data = new Vector<Hashtable<String,String>>(); // Vector of (fieldName,fieldValue)
    Hashtable<String,String> fields;

    private boolean inResponse = false;
    private boolean inRecord = false;

    private String responseName;
    private String recordName;

    public KanuEfbXmlResponse(KanuEfbSyncTask efb) {
        super(ROOT_ELEMENT);
        this.efb = efb;
    }

    public void startElement(String uri, String localName, String qname, Attributes atts) {
        super.startElement(uri, localName, qname, atts);
        if (localName.equals(ROOT_ELEMENT)) {
            return;
        }

        if (inResponse && !inRecord) {
            inRecord = true;
            recordName = localName;
        }
        if (localName.equals("response")) {
            inResponse = true;
            responseName = atts.getValue("command");
            recordName = null;
        }
        if (!inResponse) {
            inRecord = true;
            recordName = LOGIN;
        }
    }

    public void endElement(String uri, String localName, String qname) {
        super.endElement(uri, localName, qname);
        
        if (inRecord && recordName != null && 
                !fieldName.equals(recordName) && localName.equals(fieldName)) {
            // add field content
            if (fields == null) {
                fields = new Hashtable<String,String>();
            }
            fields.put(fieldName.toLowerCase(), getFieldValue());
        }

        if (inRecord && recordName != null && 
                (localName.equals(recordName) || 
                 localName.equals(ROOT_ELEMENT))) { // make sure to also add record for login response, which doesn't really have a "recordName"
            data.add(fields);
            fields = null;
            inRecord = false;
            recordName = null;
        }

        if (inResponse && localName.equals("response")) {
            inResponse = false;
        }

    }

    public Vector<Hashtable<String,String>> getData() {
        return data;
    }

    public void printAll() {
        Logger.log(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO, "-- XML RESPONSE START --");
        Logger.log(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO, "    ResponseName="+responseName);
        for (int i=0; data != null && i<data.size(); i++) {
            Hashtable<String,String> fields = data.get(i);
            String[] fieldNames = fields.keySet().toArray(new String[0]);
            for (int j=0; fieldNames != null && j<fieldNames.length; j++) {
                String value = fields.get(fieldNames[j]);
                Logger.log(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO, "    " + EfaUtil.int2String(i, 4)+":"+fieldNames[j]+"="+value);
            }
        }
        Logger.log(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO, "-- XML RESPONSE END --");
    }

    public boolean isResponseOk(String requestType) {
        return documentComplete &&
                (requestType == null || requestType.equals(responseName));
    }

    public int getNumberOfRecords() {
        return (data == null ? 0 : data.size());
    }

    public Hashtable<String,String> getFields(int idx) {
        if (data == null) {
            return null;
        }
        return data.get(idx);
    }

    public String getValue(int idx, String fieldName) {
        if (data == null) {
            return null;
        }
        Hashtable<String,String> fields = data.get(idx);
        if (fields == null) {
            return null;
        }
        return fields.get(fieldName.toLowerCase());
    }

}
