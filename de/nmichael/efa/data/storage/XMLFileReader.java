/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.storage;

import de.nmichael.efa.*;
import de.nmichael.efa.util.*;
import org.xml.sax.*;

// @i18n complete

public class XMLFileReader extends XmlHandler {

    private static final int MAX_VALUE_LENGTH = 1024*1024;
    
    private XMLFile data;
    private long globalLock;

    private boolean inDataSection = false;
    private boolean inHeaderSection = false;
    private boolean inRecord = false;
    private DataRecord dataRecord = null;
    private String documentReadError = null;

    public XMLFileReader(XMLFile data, long globalLock) {
        super(XMLFile.FIELD_GLOBAL);
        this.data = data;
        this.globalLock = globalLock;
    }

    public void startElement(String uri, String localName, String qname, Attributes atts) {
        super.startElement(uri, localName, qname, atts);

        if (inDataSection && localName.equals(XMLFile.FIELD_DATA_RECORD)) {
            inRecord = true;
            dataRecord = data.getPersistence().createNewRecord();
        }
        if (localName.equals(XMLFile.FIELD_HEADER)) {
            inHeaderSection = true;
        }
        if (localName.equals(XMLFile.FIELD_DATA)) {
            inDataSection = true;
        }
    }

    public void endElement(String uri, String localName, String qname) {
        super.endElement(uri, localName, qname);

        if (localName.equals(XMLFile.FIELD_HEADER)) {
            inHeaderSection = false;
        }
        if (localName.equals(XMLFile.FIELD_DATA)) {
            inDataSection = false;
        }
        if (inDataSection && localName.equals(XMLFile.FIELD_DATA_RECORD)) {
            try {
                data.add(dataRecord,globalLock);
            } catch(Exception e) {
                Logger.log(Logger.ERROR,Logger.MSG_FILE_PARSEERROR,getLocation() + "Parse Error for Data Record "+dataRecord.toString()+": "+e.toString());
                Logger.logdebug(e);
            }
            dataRecord = null;
            inRecord = false;
            return;
        }

        if (inDataSection) {
            try {
                fieldName = data.getPersistence().transformFieldName(fieldName);
                String value = getFieldValue();
                if (value.length() > MAX_VALUE_LENGTH) {
                    Logger.log(Logger.ERROR,Logger.MSG_FILE_PARSEERROR,
                            getLocation() + "Value in field "+fieldName+" seems corrupted: It has length of " +
                                    value.length() + " bytes. Value will be truncated. Value (first 100 bytes: " +
                                    value.substring(0, 100) + ")");
                    value = value.substring(0, MAX_VALUE_LENGTH);
                }
                dataRecord.set(fieldName, value, false);
            } catch(Exception e) {
                Logger.log(Logger.ERROR,Logger.MSG_FILE_PARSEERROR,
                        getLocation() + "Parse Error for Field "+fieldName+" = "+getFieldValue()+": "+e.toString());
            }
            if (Logger.isTraceOn(Logger.TT_XMLFILE, 9)) {
                Logger.log(Logger.DEBUG,Logger.MSG_FILE_XMLTRACE,
                        "Field "+fieldName+" = "+getFieldValue());
            }
        }
        if (inHeaderSection) {
            try {
                if (fieldName.equals(XMLFile.FIELD_HEADER_PROGRAM)) {
                    if (!getFieldValue().equals(Daten.EFA)) {
                        documentReadError = getLocation() + "Unexpected Value for Header Field " + fieldName + ": " + getFieldValue();
                    }
                }
                if (fieldName.equals(XMLFile.FIELD_HEADER_VERSION)) {
                    // version handling, if necessary
                }
                if (fieldName.equals(XMLFile.FIELD_HEADER_NAME)) {
                    if (!getFieldValue().equals(data.getStorageObjectName())) {
                        documentReadError = getLocation() + "Unexpected Value for Header Field " + fieldName + ": " + getFieldValue();
                    }
                }
                if (fieldName.equals(XMLFile.FIELD_HEADER_TYPE)) {
                    if (!getFieldValue().equals(data.getStorageObjectType())) {
                        documentReadError = getLocation() + "Unexpected Value for Header Field " + fieldName + ": " + getFieldValue();
                    }
                }
                if (fieldName.equals(XMLFile.FIELD_HEADER_SCN)) {
                    data.setSCN(Long.parseLong(getFieldValue()));
                }
            } catch (Exception e) {
                documentReadError = getLocation() + "Parse Error for Header Field " + fieldName + ": " + getFieldValue();
                Logger.log(Logger.ERROR, Logger.MSG_FILE_PARSEERROR, "Parse Error for Field " + fieldName + " = " + getFieldValue() + ": " + e.toString());
            }
            if (Logger.isTraceOn(Logger.TT_XMLFILE, 9)) {
                Logger.log(Logger.DEBUG,Logger.MSG_FILE_XMLTRACE,"Field "+fieldName+" = "+getFieldValue());
            }
        }
    }

    public String getDocumentReadError() {
        return documentReadError;
    }
    
}
