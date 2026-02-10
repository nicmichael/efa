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

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class RemoteEfaMessage {

    // Request Types and Parameters
    public static final String TYPE_REQUEST             = "Req";
    public static final String TYPE_RESPONSE            = "Resp";
    public static final String TYPE_OPERATION_ID        = "Id";
    public static final String TYPE_OPERATION_NAME      = "Op";

    // Operation Names
    public static final String OPERATION_RESULT                  = "Res";
    public static final String OPERATION_EXISTSSTORAGEOBJECT     = "ExistsObj";
    public static final String OPERATION_OPENSTORAGEOBJECT       = "OpenObj";
    public static final String OPERATION_CREATESTORAGEOBJECT     = "CreateObj";
    public static final String OPERATION_ISSTORAGEOBJECTOPEN     = "IsObjOpen";
    public static final String OPERATION_CLOSESTORAGEOBJECT      = "CloseObj";
    public static final String OPERATION_DELETESTORAGEOBJECT     = "DeleteObj";
    public static final String OPERATION_ACQUIREGLOBALLOCK       = "AcqGLock";
    public static final String OPERATION_ACQUIRELOCALLOCK        = "AcqLLock";
    public static final String OPERATION_RELEASEGLOBALLOCK       = "RelGLock";
    public static final String OPERATION_RELEASELOCALLOCK        = "RelLLock";
    public static final String OPERATION_GETNUMBEROFRECORDS      = "GetNoOfRec";
    public static final String OPERATION_GETSCN                  = "GetSCN";
    public static final String OPERATION_ADD                     = "Add";
    public static final String OPERATION_ADDVALIDAT              = "AddValidAt";
    public static final String OPERATION_ADDALL                  = "AddAll";
    public static final String OPERATION_GET                     = "Get";
    public static final String OPERATION_GETALLKEYS              = "GetAllKeys";
    public static final String OPERATION_GETBYFIELDS             = "GetByFields";
    public static final String OPERATION_GETVALIDANY             = "GetValidAny";
    public static final String OPERATION_GETVALIDAT              = "GetValidAt";
    public static final String OPERATION_GETVALIDLATEST          = "GetValidLatest";
    public static final String OPERATION_GETVALIDNEAREST         = "GetValidNearest";
    public static final String OPERATION_ISVALIDANY              = "IsValidAny";
    public static final String OPERATION_UPDATE                  = "Update";
    public static final String OPERATION_CHANGEVALIDITY          = "ChangeValidity";
    public static final String OPERATION_DELETE                  = "Delete";
    public static final String OPERATION_DELETEVERSIONIZED       = "DeleteVersionized";
    public static final String OPERATION_DELETEVERSIONIZEDALL    = "DeleteVersionizedAll";
    public static final String OPERATION_TRUNCATEALLDATA         = "TruncateAllData";
    public static final String OPERATION_GETFIRST                = "GetFirst";
    public static final String OPERATION_GETLAST                 = "GetLast";
    public static final String OPERATION_COUNTRECORDS            = "CountRecs";
    public static final String OPERATION_SETINOPENINGMODE        = "SetInOpeningMode";

    public static final String OPERATION_CMD_EXITEFA             = "CmdExitEfa";
    public static final String OPERATION_CMD_ONLINEUPDATE        = "CmdOnlineUpdate";

    // Field Names for Operations
    public static final String FIELD_SESSIONID          = "SID";
    public static final String FIELD_ADMINRECORD        = "AdminRecord"; // for login
    public static final String FIELD_RESULTCODE         = "RCode";
    public static final String FIELD_RESULTTEXT         = "RText";
    public static final String FIELD_USERNAME           = "Username";
    public static final String FIELD_PASSWORD           = "Password";
    public static final String FIELD_STORAGEOBJECTTYPE  = "ObjType";
    public static final String FIELD_STORAGEOBJECTNAME  = "ObjName";
    public static final String FIELD_SCN                = "SCN";
    public static final String FIELD_TOTALRECORDCOUNT   = "RecCnt";
    public static final String FIELD_LOCKID             = "LockId";
    public static final String FIELD_LONGVALUE          = "LongValue";
    public static final String FIELD_TIMESTAMP          = "Timestamp";
    public static final String FIELD_VALIDFROM          = "ValidFrom";
    public static final String FIELD_INVALIDFROM        = "InvalidFrom";
    public static final String FIELD_MERGE              = "Merge";
    public static final String FIELD_PREFETCH           = "Prefetch";
    public static final String FIELD_FIELDNAME          = "FieldName";
    public static final String FIELD_FIELDVALUE         = "FieldValue";
    public static final String FIELD_PID                = "PID";
    public static final String FIELD_BOOLEAN            = "Boolean";

    // Result Codes
    public static final int RESULT_OK                   =  0;
    public static final int RESULT_FALSE                =  1;
    public static final int ERROR_UNKNOWN               =  2;
    public static final int ERROR_INVALIDREQUEST        =  3;
    public static final int ERROR_UNABLETOCOMPLY        =  4;
    public static final int ERROR_INVALIDLOGIN          =  5;
    public static final int ERROR_INVALIDSESSIONID      =  6;
    public static final int ERROR_NOPERMISSION          =  7;
    public static final int ERROR_NOSTORAGEOBJECT       =  8;
    public static final int ERROR_UNKNOWNSTORAGEOBJECT  =  9;
    public static final int ERROR_SELFLOGIN             = 10;
    public static final int ERROR_NOTYETSUPPORTED       = 99;

    enum Type {
        request,
        response
    }

    private int msgId;
    private Type type;
    private String operation;

    private IDataAccess dataAccess;
    private Vector<DataRecord> records;
    private Vector<DataKey> keys;
    private Hashtable<String,String> fields;
    private AdminRecord adminRecord;
    private int sizeEstimate; // just for statistic purposes

    public RemoteEfaMessage(int msgId, Type type, String operation) {
        this.msgId = msgId;
        this.type = type;
        this.operation = operation;
    }

    public RemoteEfaMessage(int msgId, Type type, String operation, String storageObjectType, String storageObjectName) {
        this.msgId = msgId;
        this.type = type;
        this.operation = operation;
        this.addField(FIELD_STORAGEOBJECTTYPE, storageObjectType);
        this.addField(FIELD_STORAGEOBJECTNAME, storageObjectName);
    }

    public void setDataAccess(IDataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public IDataAccess getDataAccesss() {
        return dataAccess;
    }

    public int getMsgId() {
        return msgId;
    }

    public Type getType() {
        return type;
    }

    public String getOperationName() {
        return operation;
    }

    public String getSessionId() {
        return getFieldValue(FIELD_SESSIONID);
    }

    public String getStorageObjectType() {
        return getFieldValue(FIELD_STORAGEOBJECTTYPE);
    }

    public String getStorageObjectName() {
        return getFieldValue(FIELD_STORAGEOBJECTNAME);
    }

    public String getUsername() {
        return getFieldValue(FIELD_USERNAME);
    }

    public String getPassword() {
        return getFieldValue(FIELD_PASSWORD);
    }

    public String getPid() {
        return getFieldValue(FIELD_PID);
    }

    public boolean getBoolean() {
        try {
            return Boolean.parseBoolean(getFieldValue(FIELD_BOOLEAN));
        } catch(Exception e) {
            return false;
        }
    }

    public long getScn() {
        return EfaUtil.string2long(getFieldValue(FIELD_SCN), -1);
    }

    public long getRecCnt() {
        return EfaUtil.string2long(getFieldValue(FIELD_TOTALRECORDCOUNT), -1);
    }

    public long getLockId() {
        return EfaUtil.string2long(getFieldValue(FIELD_LOCKID), -1);
    }

    public long getLongValue() {
        return EfaUtil.string2long(getFieldValue(FIELD_LONGVALUE), -1);
    }

    public long getTimestamp() {
        return EfaUtil.string2long(getFieldValue(FIELD_TIMESTAMP), -1);
    }

    public long getValidFrom() {
        return EfaUtil.string2long(getFieldValue(FIELD_VALIDFROM), -1);
    }

    public long getInvalidFrom() {
        return EfaUtil.string2long(getFieldValue(FIELD_INVALIDFROM), -1);
    }

    public int getMerge() {
        return EfaUtil.string2int(getFieldValue(FIELD_MERGE), -1);
    }

    public boolean getPrefetch() {
        try {
            return Boolean.parseBoolean(getFieldValue(FIELD_PREFETCH));
        } catch(Exception eignore) {
            return false;
        }
    }

    public int getResultCode() {
        try {
            return EfaUtil.string2int(getFieldValue(FIELD_RESULTCODE), -1);
        } catch(Exception e) {
            return -1;
        }
    }

    public String getResultText() {
        return getFieldValue(FIELD_RESULTTEXT);
    }

    public void addRecord(DataRecord record) {
        if (records == null) {
            records = new Vector<DataRecord>();
        }
        records.add(record);
    }

    public void addKey(DataKey key) {
        if (keys == null) {
            keys = new Vector<DataKey>();
        }
        keys.add(key);
    }

    public void addField(String fieldName, String fieldValue) {
        if (fields == null) {
            fields = new Hashtable<String,String>();
        }
        fields.put(fieldName, fieldValue);
    }

    public String[] getFields() {
        if (fields == null) {
            return null;
        }
        return fields.keySet().toArray(new String[0]);
    }

    public String getFieldValue(String fieldName) {
        if (fields == null) {
            return null;
        }
        return fields.get(fieldName);
    }

    // index starts with 0!!
    public void addFieldArrayElement(int i, String fieldName, String fieldValue) {
        addField(FIELD_FIELDNAME  + i, fieldName);
        addField(FIELD_FIELDVALUE + i, fieldValue);
    }

    public String[] getFieldArrayNames() {
        int count = 0;
        while(getFieldValue(FIELD_FIELDNAME + count) != null) {
            count++;
        }
        String[] a = new String[count];
        for (int i=0; i<count; i++) {
            a[i] = getFieldValue(FIELD_FIELDNAME + i);
        }
        return a;
    }

    public String[] getFieldArrayValues() {
        int count = 0;
        while(getFieldValue(FIELD_FIELDNAME + count) != null) { // for the number of fields, we check for FIELD_FIELDNAME, not for FIELD_FIELDVALUE!!
            count++;
        }
        String[] a = new String[count];
        for (int i=0; i<count; i++) {
            a[i] = getFieldValue(FIELD_FIELDVALUE + i);
            if (a[i] != null && a[i].length() == 0) {
                a[i] = null; // empty fields must be null for getByFields(...)
            }
        }
        return a;
    }

    public void setAdminRecord(AdminRecord admin) {
        this.adminRecord = admin;
    }

    public AdminRecord getAdminRecord() {
        return adminRecord;
    }

    public int getNumberOfRecords() {
        return (records != null ? records.size() : 0);
    }

    public DataRecord getRecord(int i) {
        return (records == null || records.size() <= i ? null : records.get(i));
    }

    public DataRecord[] getRecords() {
        if (records == null || records.size() == 0) {
            return null;
        }
        DataRecord[] a = new DataRecord[records.size()];
        for (int i=0; i<a.length; i++) {
            a[i] = records.get(i);
        }
        return a;
    }

    public int getNumberOfKeys() {
        return (keys != null ? keys.size() : 0);
    }

    public DataKey getKey(int i) {
        return (keys == null || keys.size() <= i ? null : keys.get(i));
    }

    public DataKey[] getKeys() {
        if (keys == null || keys.size() == 0) {
            return null;
        }
        DataKey[] a = new DataKey[keys.size()];
        for (int i=0; i<a.length; i++) {
            a[i] = keys.get(i);
        }
        return a;
    }

    public String toString() {
        StringBuffer s = new StringBuffer();

        if (type == Type.request) {
            s.append("<" + TYPE_REQUEST + " " + 
                    TYPE_OPERATION_ID + "=\"" + getMsgId() + "\" " +
                    TYPE_OPERATION_NAME + "=\"" + getOperationName() + "\"" +
                    ">");
        }
        if (type == Type.response) {
            s.append("<" + TYPE_RESPONSE + " " + 
                    TYPE_OPERATION_ID + "=\"" + getMsgId() + "\" " +
                    TYPE_OPERATION_NAME + "=\"" + getOperationName() + "\"" +
                    ">");
        }

        if (fields != null) {
            String[] fieldNames = getFields();
            for (int i=0; i<fieldNames.length; i++) {
                s.append("<" +  fieldNames[i] + ">" + 
                        EfaUtil.escapeXml(fields.get(fieldNames[i])) + "</" +
                        fieldNames[i] + ">");
            }
        }

        if (adminRecord != null) {
                s.append("<" +  FIELD_ADMINRECORD + ">" +
                        adminRecord.encodeAsString() +
                        "</" + FIELD_ADMINRECORD + ">");
        }

        if (records != null) {
            for (int i=0; i<records.size(); i++) {
                s.append(records.get(i).encodeAsString());
            }
        }

        if (keys != null) {
            for (int i=0; i<keys.size(); i++) {
                s.append(keys.get(i).encodeAsString());
            }
        }

        if (type == Type.request) {
            s.append("</" + TYPE_REQUEST + ">");
        }
        if (type == Type.response) {
            s.append("</" + TYPE_RESPONSE + ">");
        }

        return s.toString();
    }

    // ===================================== Requests =====================================

    public static RemoteEfaMessage createRequestData(int msgId, String storageObjectType, String storageObjectName, String operation) {
        RemoteEfaMessage r = new RemoteEfaMessage(msgId, Type.request, operation, storageObjectType, storageObjectName);
        return r;
    }


    // ===================================== Responses =====================================

    public static RemoteEfaMessage createResponseResult(int msgId, int resultCode, String resultText) {
        RemoteEfaMessage r = new RemoteEfaMessage(msgId, Type.response, OPERATION_RESULT);
        r.addField(FIELD_RESULTCODE, Integer.toString(resultCode));
        if (resultText != null) {
            r.addField(FIELD_RESULTTEXT, resultText);
        }
        return r;
    }


    // ===================================== Compression Handling =====================================

    public static EfaMessageInputStream getBufferedInputStream(InputStream in, long timeoutSec) {
        try {
            // RemoteEfaServer has to call this method without a timeout (timeoutSec == 0).
            // RemoteEfaClient should supply a timeout value to avoid blocking
            if (timeoutSec > 0) {
                long timeout = timeoutSec*100;

                // first do 10 fast loops to not waist too much time while waiting for input.
                // if during fast loop no input becomes available, fall back to slow mode.
                int fastCount = 0;

                while (in.available() <= 0) {
                    try {
                        Thread.sleep( (fastCount++ < 10 ? 1 : 10) );
                    } catch (InterruptedException eignore) {
                    }
                    if (fastCount < 10) {
                        continue;
                    }
                    if (--timeout <= 0) {
                        Logger.log(Logger.ERROR, Logger.MSG_REFA_ERRORTIMEOUT,
                                "Response Receive Timeout [" + timeoutSec + " sec]");
                        return null;
                    }
                }
            }
            int sizeEstimate = in.available();
            ZipInputStream zip = new ZipInputStream(in);
            ZipEntry entry = zip.getNextEntry();
            return new EfaMessageInputStream(new BufferedInputStream(zip), sizeEstimate);
        } catch(Exception e) {
            return null;
        }
    }

    public static OutputStream getOutputStream(OutputStream out) {
        try {
            ZipOutputStream zip = new ZipOutputStream(out);
            zip.putNextEntry(new ZipEntry("efa"));
            return zip;
        } catch(Exception e) {
            return null;
        }
    }

    // ===================================== Message Size ====================================

    public void setMessageSizeEstimate(int size) {
        sizeEstimate = size;
    }

    public int getMessageSizeEstimate() {
        return sizeEstimate;
    }

    // ===================================== Input Stream Wrapper ====================================

    static class EfaMessageInputStream {

        BufferedInputStream in;
        int size;

        public EfaMessageInputStream(BufferedInputStream in, int size) {
            this.in = in;
            this.size = size;
        }


    }

}
