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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.util.*;
import java.util.*;
import org.xml.sax.*;

// @i18n complete

public class RemoteEfaParser extends XmlHandler {

    public static String XML_EFA = "efa";

    private IDataAccess dataAccess;
    private DataRecord dummyRecord;
    private String[] keyFields;

    private Vector<RemoteEfaMessage> messages = new Vector<RemoteEfaMessage>();
    RemoteEfaMessage message;
    private DataRecord record;
    private DataKey key;

    private boolean inRequestResponse = false;
    private boolean inRecord = false;
    private boolean inKey = false;
    private boolean inFieldAdminRecord = false;

    public RemoteEfaParser(IDataAccess dataAccess) {
        super(XML_EFA);
        this.dataAccess = dataAccess;
        iniDataAccess();
    }

    // it doesn't hurt to call this method when everything has already been initialized
    private boolean iniDataAccess() {
        if (dataAccess == null && message != null) {
            String storageObjectName = message.getStorageObjectName();
            String storageObjectType = message.getStorageObjectType();
            if (storageObjectName != null && storageObjectName.length() > 0 &&
                storageObjectType != null && storageObjectType.length() > 0) {
                StorageObject p = null;
                if (Daten.project != null) {
                    // most often, we will be asked for a project storage object, so we check this first
                    boolean createNewIfDoesntExist = RemoteEfaMessage.OPERATION_CREATESTORAGEOBJECT.equals(message.getOperationName());
                    p = Daten.project.getStorageObject(storageObjectName, storageObjectType, createNewIfDoesntExist);
                }
                if (p == null) {
                    if (Daten.efaConfig.data().getStorageObjectType().equals(storageObjectType)
                            && Daten.efaConfig.data().getStorageObjectName().equals(storageObjectName)) {
                        p = Daten.efaConfig;
                    } else if (Daten.efaTypes.data().getStorageObjectType().equals(storageObjectType)
                            && Daten.efaTypes.data().getStorageObjectName().equals(storageObjectName)) {
                        p = Daten.efaTypes;
                    } else if (Daten.admins.data().getStorageObjectType().equals(storageObjectType)
                            && Daten.admins.data().getStorageObjectName().equals(storageObjectName)) {
                        p = Daten.admins;
                    }
                }
                if (p != null) {
                    dataAccess = p.data();
                }
            }
        }
        if (dummyRecord == null && dataAccess != null && dataAccess.getPersistence() != null) {
            dummyRecord = dataAccess.getPersistence().createNewRecord();
            keyFields =  dummyRecord.getKeyFields();
        }
        return (dataAccess != null);
    }

    public Vector<RemoteEfaMessage> getMessages() {
        return messages;
    }

    public void startElement(String uri, String localName, String qname, Attributes atts) {
        super.startElement(uri, localName, qname, atts);

        if (inRequestResponse && !inRecord && !inKey) {
            if (localName.equals(RemoteEfaMessage.FIELD_ADMINRECORD)) {
                inFieldAdminRecord = true;
            }
            if (localName.equals(DataRecord.ENCODING_RECORD)) {
                // begin of record
                if (!inFieldAdminRecord) {
                    if (!iniDataAccess()) {
                        return;
                    }
                    record = dataAccess.getPersistence().createNewRecord();
                } else {
                    record = Daten.admins.createNewRecord();
                }
                inRecord = true;
                return;
            }
            if (localName.equals(DataKey.ENCODING_KEY)) {
                // begin of key
                if (!iniDataAccess()) {
                    return;
                }
                inKey = true;
                try {
                    key = dataAccess.constructKey(null);
                } catch(Exception e) {
                    Logger.log(e);
                }
                return;
            }
            // begin of field (outside record and key)
            inRecord = false;
            inKey = false;
            return;
        }

        if (!inRequestResponse && localName.equals(RemoteEfaMessage.TYPE_REQUEST)) {
            // begin of request
            inRequestResponse = true;
            message = new RemoteEfaMessage(
                    EfaUtil.string2int(atts.getValue(RemoteEfaMessage.TYPE_OPERATION_ID), 0),
                    RemoteEfaMessage.Type.request,
                    atts.getValue(RemoteEfaMessage.TYPE_OPERATION_NAME));
            return;
        }

        if (!inRequestResponse && localName.equals(RemoteEfaMessage.TYPE_RESPONSE)) {
            // begin of response
            inRequestResponse = true;
            message = new RemoteEfaMessage(
                    EfaUtil.string2int(atts.getValue(RemoteEfaMessage.TYPE_OPERATION_ID), 0),
                    RemoteEfaMessage.Type.response,
                    atts.getValue(RemoteEfaMessage.TYPE_OPERATION_NAME));
            return;
        }
    }

    public void endElement(String uri, String localName, String qname) {
        super.endElement(uri, localName, qname);

        if (inRequestResponse && inRecord && localName.equals(DataRecord.ENCODING_RECORD)) {
            // end of record
            if (!inFieldAdminRecord) {
                message.addRecord(record);
            } else {
                message.setAdminRecord((AdminRecord)record);
            }
            record = null;
            inRecord = false;
            return;
        }

        if (inRequestResponse && localName.equals(RemoteEfaMessage.FIELD_ADMINRECORD)) {
            inFieldAdminRecord = false;
        }

        if (inRequestResponse && inKey && localName.equals(DataKey.ENCODING_KEY)) {
            // end of key
            message.addKey(key);
            key = null;
            inKey = false;
            return;
        }

        if (inRequestResponse && !inRecord && !inKey && localName.equals(RemoteEfaMessage.TYPE_REQUEST)) {
            // end of request
            inRequestResponse = false;
            iniDataAccess();
            message.setDataAccess(dataAccess);
            messages.add(message);
            message = null;
            return;
        }

        if (inRequestResponse && !inRecord && !inKey && localName.equals(RemoteEfaMessage.TYPE_RESPONSE)) {
            // end of response
            inRequestResponse = false;
            iniDataAccess();
            message.setDataAccess(dataAccess);
            messages.add(message);
            message = null;
            return;
        }

        if (inRequestResponse) {
            // end of field
            if (inRecord) {
                String fieldValue = getFieldValue();
                record.set(fieldName, (fieldValue != null ? fieldValue.trim() : fieldValue));
            } else if (inKey) {
                int keyFieldIdx = -1;
                if (fieldName.equals(DataKey.ENCODING_KEY_PART1)) {
                    keyFieldIdx = 0;
                }
                if (fieldName.equals(DataKey.ENCODING_KEY_PART2)) {
                    keyFieldIdx = 1;
                }
                if (fieldName.equals(DataKey.ENCODING_KEY_PART3)) {
                    keyFieldIdx = 2;
                }
                if (keyFieldIdx >= 0 && keyFieldIdx <= 2) {
                    dummyRecord.set(keyFields[keyFieldIdx], getFieldValue());
                    key.set(keyFieldIdx, dummyRecord.get(keyFields[keyFieldIdx]));
                }
            } else{
                if (!inFieldAdminRecord) {
                    message.addField(fieldName, getFieldValue());
                }
            }
        }

    }

}
