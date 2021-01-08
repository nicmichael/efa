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
import de.nmichael.efa.core.BackupMetaDataItem;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import java.io.FileOutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// @i18n complete

public abstract class DataAccess implements IDataAccess {

    protected StorageObject persistence;
    protected String storageLocation;
    protected String storageObjectName;
    protected String storageObjectType;
    protected String storageObjectDescription;
    protected String storageUsername;
    protected String storagePassword;
    protected String storageObjectVersion;

    protected final LinkedHashMap<String,Integer> fieldTypes = new LinkedHashMap<String,Integer>();
    protected String[] keyFields;
    protected MetaData meta;
    protected DataRecord referenceRecord;
    protected boolean inOpeningStorageObject = false;
    protected boolean isPreModifyRecordCallbackEnabled = true;

    public static IDataAccess createDataAccess(StorageObject persistence,
            int type,
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName,
            String storageObjectType,
            String storageObjectDescription) {
        IDataAccess dataAccess = null;
        switch(type) {
            case IDataAccess.TYPE_FILE_XML:
                dataAccess = (IDataAccess)new XMLFile(storageLocation, storageObjectName, storageObjectType, storageObjectDescription);
                dataAccess.setPersistence(persistence);
                return dataAccess;
            case IDataAccess.TYPE_EFA_CLOUD:
                Project p = Daten.project;
                try {
                    dataAccess = new EfaCloudStorage(storageLocation, storageObjectName, storageObjectType,
                            storageObjectDescription);
                } catch (EfaException e) {
                    Logger.log(Logger.ERROR, Logger.MSG_DATA_DATAACCESS,
                            "DataAccess initialization for " + storageObjectName + "." + storageObjectType + " (type " + type
                            + "): authorization failed.");
                    return null;
                }
                dataAccess.setPersistence(persistence);
                return dataAccess;
            case IDataAccess.TYPE_EFA_REMOTE:
                 dataAccess = (IDataAccess)new RemoteEfaClient(storageLocation, storageUsername, storagePassword, storageObjectName, storageObjectType, storageObjectDescription);
                 dataAccess.setPersistence(persistence);
                return dataAccess;
        }
        Logger.log(Logger.ERROR, Logger.MSG_DATA_DATAACCESS,
                "DataAccess for " + storageObjectName + "." + storageObjectType + " (type " + type +
                ") is null");
        return null;
    }

    public void setPersistence(StorageObject persistence) {
        this.persistence = persistence;
    }

    public StorageObject getPersistence() {
        return persistence;
    }


    public void setStorageLocation(String location) {
        this.storageLocation = location;
    }

    public String getStorageLocation() {
        return this.storageLocation;
    }

    public void setStorageObjectName(String name) {
        this.storageObjectName = name;
    }

    public String getStorageObjectName() {
        return this.storageObjectName;
    }

    public void setStorageObjectType(String type) {
        this.storageObjectType = type;
    }

    public String getStorageObjectType() {
        return this.storageObjectType;
    }

    public void setStorageObjectDescription(String description) {
        this.storageObjectDescription = description;
    }

    public String getStorageObjectDescription() {
        return this.storageObjectDescription;
    }

    public void setStorageUsername(String username) {
        this.storageUsername = username;
    }

    public void setStoragePassword(String password) {
        this.storagePassword = password;
    }

    public String getStorageUsername() {
        return this.storageUsername;
    }

    public String getStoragePassword() {
        return this.storagePassword;
    }

    public String getStorageObjectVersion() {
        return this.storageObjectVersion;
    }

    public void setStorageObjectVersion(String version) {
        this.storageObjectVersion = version;
    }

    public void registerDataField(String fieldName, int dataType) throws EfaException {
        if (fieldTypes.containsKey(fieldName)) {
            throw new EfaException(Logger.MSG_DATA_GENERICEXCEPTION,getUID() + ": Field Name is already in use: "+fieldName, Thread.currentThread().getStackTrace());
        }
        synchronized(fieldTypes) { // fieldTypes used for synchronization of fieldTypes and keyFields as well
            fieldTypes.put(fieldName, dataType);
        }
    }


    public void setKey(String[] fieldNames) throws EfaException {
        synchronized (fieldTypes) { // fieldTypes used for synchronization of fieldTypes and keyFields as well
            for (int i = 0; i < fieldNames.length; i++) {
                getFieldType(fieldNames[i]); // just to check for existence
            }
            this.keyFields = fieldNames;
        }
    }

    public void setMetaData(MetaData meta) {
        this.meta = meta;
        try {
            for (int i=0; i<meta.getNumberOfFields(); i++) {
                registerDataField(meta.getFieldName(i), meta.getFieldType(i));
            }
            setKey(meta.getKeyFields());
            String[][] indexFields = meta.getIndices();
            for (int i=0; i<indexFields.length; i++) {
                createIndex(indexFields[i]);
            }
            referenceRecord = persistence.createNewRecord();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public MetaData getMetaData() {
        return meta;
    }

    public String[] getKeyFieldNames() {
        String[] names = null;
        synchronized (fieldTypes) { // fieldTypes used for synchronization of fieldTypes and keyFields as well
            names = new String[this.keyFields.length];
            for (int i=0; i<names.length; i++) {
                names[i] = this.keyFields[i];
            }
        }
        return names;
    }

    public String[] getFieldNames() {
        return getFieldNames(true);
    }

    public String[] getFieldNames(boolean includingVirtual) {
        synchronized (fieldTypes) { // fieldTypes used for synchronization of fieldTypes and keyFields as well
            String[] keys = new String[fieldTypes.size()];
            fieldTypes.keySet().toArray(keys);
            if (includingVirtual) {
                return keys;
            } else {
                Vector<String> v = new Vector<String>();
                for (int i=0; i<keys.length; i++) {
                    if (getMetaData().getFieldType(keys[i]) != IDataAccess.DATA_VIRTUAL) {
                        v.add(keys[i]);
                    }
                }
                if (keys.length != v.size()) {
                    keys = new String[v.size()];
                    for (int i=0; i<v.size(); i++) {
                        keys[i] = v.get(i);
                    }
                }
                return keys;
            }
        }
    }

    public int getFieldType(String fieldName) throws EfaException {
        Integer i = null;
        synchronized (fieldTypes) { // fieldTypes used for synchronization of fieldTypes and keyFields as well
            i = fieldTypes.get(fieldName);
        }
        if (i == null) {
            throw new EfaException(Logger.MSG_DATA_FIELDDOESNOTEXIST, getUID() + ": Field Name does not exist: "+fieldName, Thread.currentThread().getStackTrace());
        }
        return i.intValue();
    }

    public boolean hasCompleteKey(DataRecord record) {
        if ((keyFields.length >= 1) && (record.get(keyFields[0]) == null))
            return false;
        if ((keyFields.length >= 2) && (record.get(keyFields[1]) == null))
            return false;
        if ((keyFields.length >= 3) && (record.get(keyFields[2]) == null))
            return false;
        return true;
    }

    public DataKey constructKey(DataRecord record) throws EfaException {
        Object v1 = null;
        Object v2 = null;
        Object v3 = null;

        if (keyFields.length >= 1) {
            v1 = (record != null ? record.get(keyFields[0]) : null);
        }
        if (keyFields.length >= 2) {
            v2 = (record != null ? record.get(keyFields[1]) : null);
        }
        if (keyFields.length >= 3) {
            v3 = (record != null ? record.get(keyFields[2]) : null);
        }

        return new DataKey(v1, v2, v3);
    }

    public DataKey getUnversionizedKey(DataKey key) {
        boolean[] bUnversionized = new boolean[keyFields.length];
        for (int i=0; i<keyFields.length; i++) {
            bUnversionized[i] = !keyFields[i].equals(DataRecord.VALIDFROM);
        }
        return new DataKey(key,bUnversionized); // this is the corresponding "unversionized" key (i.e. key with only unversionized fields)
    }

    public String getTypeName(int type) {
        switch(type) {
            case DATA_STRING:
                return "STRING";
            case DATA_INTEGER:
                return "INTEGER";
            case DATA_LONGINT:
                return "LONGINT";
            case DATA_DOUBLE:
                return "DOUBLE";
            case DATA_DECIMAL:
                return "DECIMAL";
            case DATA_DISTANCE:
                return "DISTANCE";
            case DATA_BOOLEAN:
                return "BOOLEAN";
            case DATA_DATE:
                return "DATE";
            case DATA_TIME:
                return "TIME";
            case DATA_UUID:
                return "UUID";
            case DATA_INTSTRING:
                return "INTSTRING";
            case DATA_PASSWORDH:
                return "PASSWORDH";
            case DATA_PASSWORDC:
                return "PASSWORDC";
            case DATA_LIST_STRING:
                return "LIST_STRING";
            case DATA_LIST_INTEGER:
                return "LIST_INTEGER";
            case DATA_LIST_UUID:
                return "LIST_UUID";
            case DATA_VIRTUAL:
                return "VIRTUAL";
            default: return "UNKNOWN";
        }
    }

    public boolean inOpeningStorageObject() {
        return this.inOpeningStorageObject;
    }

    public void setInOpeningStorageObject(boolean inOpening) {
        inOpeningStorageObject = inOpening;
    }

    public void setPreModifyRecordCallbackEnabled(boolean enabled) {
        this.isPreModifyRecordCallbackEnabled = enabled;
    }

    public boolean isPreModifyRecordCallbackEnabled() {
        return this.isPreModifyRecordCallbackEnabled && (Daten.efaConfig == null || Daten.efaConfig.getValueDataPreModifyRecordCallbackEnabled());
    }

    public void saveToXmlFile(String filename) throws EfaException {
        if (!isStorageObjectOpen()) {
            throw new EfaException(Logger.MSG_DATA_SAVEFAILED, LogString.fileWritingFailed(filename, storageLocation, "Storage Object is not open"), Thread.currentThread().getStackTrace());
        }
        try {
            FileOutputStream out = new FileOutputStream(filename, false);
            XMLFile.writeFile(this, out);
            out.close();
        } catch(Exception e) {
            throw new EfaException(Logger.MSG_DATA_SAVEFAILED, LogString.fileWritingFailed(filename, storageLocation, e.toString()), Thread.currentThread().getStackTrace());
        }
    }

    public BackupMetaDataItem saveToZipFile(String dir, ZipOutputStream zipOut) throws EfaException {
        if (!isStorageObjectOpen()) {
            throw new EfaException(Logger.MSG_DATA_SAVEFAILED, LogString.fileWritingFailed("ZIP Buffer", storageLocation, "Storage Object is not open"), Thread.currentThread().getStackTrace());
        }
        if (dir.length() > 0 && !dir.endsWith(Daten.fileSep)) {
            dir += Daten.fileSep;
        }
        String zipFileEntry = dir + getStorageObjectName() + "." + getStorageObjectType();
        long lock = -1;
        BackupMetaDataItem metaData = null;
        try {
            ZipEntry entry = new ZipEntry(zipFileEntry);
            zipOut.putNextEntry(entry);
            lock = acquireGlobalLock();
            metaData = new BackupMetaDataItem(getStorageObjectName(),
                    getStorageObjectType(),
                    zipFileEntry,
                    getStorageObjectDescription(),
                    getNumberOfRecords(),
                    getSCN());
            XMLFile.writeFile(this, zipOut);
        } catch(Exception e) {
            throw new EfaException(Logger.MSG_DATA_SAVEFAILED,
                    LogString.fileWritingFailed("ZIP Buffer", storageLocation, e.toString()), Thread.currentThread().getStackTrace());
        } finally {
            if (lock >= 0) {
                releaseGlobalLock(lock);
            }
        }
        return metaData;
    }

    // this method does NOT set the SCN to the value of the archive.
    // the SCN is always increasing!!
    public synchronized void copyFromDataAccess(IDataAccess source) throws EfaException {
        truncateAllData();
        try {
            DataKeyIterator it = source.getStaticIterator();
            ArrayList<DataRecord> recordList = new ArrayList<DataRecord>();
            DataKey k = it.getFirst();
            while (k != null) {
                recordList.add(source.get(k));
                k = it.getNext();
            }

            setInOpeningStorageObject(true); // don't update LastModified Timestamps, don't increment SCN, don't check assertions!
            if (recordList.size() > 0) {
                addAll(recordList.toArray(new DataRecord[0]), -1);
            }
        } catch (Exception e) {
            throw new EfaException(Logger.MSG_DATA_COPYFROMDATAACCESSFAILED, getUID() + 
                    ": Restore from DataAccess failed", Thread.currentThread().getStackTrace());
        } finally {
            setInOpeningStorageObject(false);
        }
    }
}
