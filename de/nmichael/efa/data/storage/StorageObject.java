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

import de.nmichael.efa.ex.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete

public abstract class StorageObject {

    protected IDataAccess dataAccess;
    protected Project project;

    public StorageObject(int storageType,
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName,
            String storageObjectType,
            String storageObjectDescription) {
        dataAccess = DataAccess.createDataAccess(this, storageType, storageLocation, storageUsername, storagePassword, storageObjectName, storageObjectType, storageObjectDescription);
    }

    public void create() throws EfaException {
        dataAccess.createStorageObject();
    }

    public void open(boolean createNewIfNotExists) throws EfaException {
        try {
            if (Logger.isTraceOn(Logger.TT_CORE, 5) || Logger.isTraceOn(Logger.TT_CLOUD,1)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Opening StorageObject "
                        + getUID() + " ...");
            }
            if (createNewIfNotExists && !dataAccess.existsStorageObject()) {
                dataAccess.createStorageObject();
            } else {
                dataAccess.openStorageObject();
            }
            if (Logger.isTraceOn(Logger.TT_CORE, 5)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "StorageObject "
                        + getUID() + " opened.");
            }
        } catch(EfaException eOpen) {
            if (createNewIfNotExists) {
                try {
                    if (Logger.isTraceOn(Logger.TT_CORE, 5)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Creating StorageObject "
                                + getUID() + " ...");
                    }
                    dataAccess.createStorageObject();
                    if (Logger.isTraceOn(Logger.TT_CORE, 5)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "StorageObject "
                                + getUID() + " created.");
                    }
                } catch(EfaException eCreate) {
                    throw eCreate;
                }
            } else {
                throw eOpen;
            }
        }
    }

    public void close() throws EfaException {
        if (Logger.isTraceOn(Logger.TT_CORE, 5) || Logger.isTraceOn(Logger.TT_CLOUD,1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Closing StorageObject "
                    + getUID() + " ...");
        }
        dataAccess.closeStorageObject();
        if (Logger.isTraceOn(Logger.TT_CORE, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "StorageObject "
                    + getUID() + " closed.");
        }
    }

    public boolean isOpen() {
        return dataAccess.isStorageObjectOpen();
    }

    public IDataAccess data() {
        return dataAccess;
    }

    public abstract DataRecord createNewRecord();

    public String toString() {
        return dataAccess.getUID();
    }

    public String getUID() {
        return dataAccess.getUID();
    }

    public String getName() {
        return dataAccess.getStorageObjectName();
    }

    public String getDescription() {
        return dataAccess.getStorageObjectDescription();
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        // to be implemented in subclass, if necessary
    }

    protected void assertFieldNotEmpty(DataRecord record, String field) throws EfaModifyException {
        if (this.data().inOpeningStorageObject()) {
            return;
        }

        String s = record.getAsString(field);
        if (s == null || s.trim().length() == 0) {
            throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                    International.getMessage("Das Feld '{field}' darf nicht leer sein.", field),
                    Thread.currentThread().getStackTrace());
        }
    }

    protected void assertUnique(DataRecord record, String field) throws EfaModifyException {
        if (this.data().inOpeningStorageObject()) {
            return;
        }

        if (record.get(field) == null || record.get(field).toString().length() == 0) {
            return;
        }
        DataKey[] keys = null;
        try {
            keys = data().getByFields(new String[] { field }, 
                    new Object[] { record.get(field) });
                    // the original line was the following, but it looks wrong:
                    // we should be passing on Objects, not Strings!
                    //new String[] { record.getAsString(field) });
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        DataKey myKey = record.getKey();
        for (int i = 0; keys != null && i < keys.length; i++) {
            if (!myKey.equals(keys[i])) {
                throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                        International.getString("Es gibt bereits einen gleichnamigen Eintrag.") + " "
                        + International.getMessage("Das Feld '{field}' muß eindeutig sein.", field),
                        Thread.currentThread().getStackTrace());
            }
        }
    }

    protected void assertUnique(DataRecord record, String[] fields) throws EfaModifyException {
        if (this.data().inOpeningStorageObject()) {
            return;
        }

        DataKey[] keys = null;
        try {
            Object[] values = new Object[fields.length];
            // the original line was the following, but it looks wrong:
            // we should be passing on Objects, not Strings!
            // String[] values = new String[fields.length];
            for (int i=0; i<values.length; i++) {
                //values[i] = record.getAsString(fields[i]);
                values[i] = record.get(fields[i]);
            }
            keys = data().getByFields(fields, values);
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        DataKey myKey = record.getKey();
        for (int i = 0; keys != null && i < keys.length; i++) {
            if (!myKey.equals(keys[i])) {
                throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                        International.getString("Es gibt bereits einen gleichnamigen Eintrag.") + " "
                        + International.getMessage("Die Felder '{fields}' müssen eindeutig sein.", EfaUtil.arr2KommaList(fields)),
                        Thread.currentThread().getStackTrace());
            }
        }
    }

    /*
     * Note: referencingFields must be in the same order as the key fields of record!
     */
    protected void assertNotReferenced(DataRecord record, StorageObject referencingPersistence, String[] referencingFields) throws EfaModifyException {
        assertNotReferenced(record, referencingPersistence, referencingFields, true, null, null);
    }
    
    protected void assertNotReferenced(DataRecord record, StorageObject referencingPersistence, String[] referencingFields,
                                       boolean keyMustMatchInAllReferencingFields) throws EfaModifyException {
        assertNotReferenced(record, referencingPersistence, referencingFields, keyMustMatchInAllReferencingFields, null, null);
    }

    protected void assertNotReferenced(DataRecord record, StorageObject referencingPersistence, String[] referencingFields,
                                       boolean keyMustMatchInAllReferencingFields,
                                       String[] referencingFieldFilerNames, String[] referencingFieldFilerValues) throws EfaModifyException {
        if (this.data().inOpeningStorageObject()) {
            return;
        }

        if (this.data().getMetaData().isVersionized()) {
            return; // @todo (P8) there are lots of operations which involve deleting of versions, which trigger this check unnecessarily
        }

        String refRec = null;
        try {
            // get the key field indexes of this record (without VALID_FROM)
            String[] keyFields = record.getKeyFields();
            int keyFieldCount = keyFields.length;
            if (keyFieldCount > 1 && record.metaData.isVersionized()
                    && record.metaData.getFieldName(keyFields[keyFieldCount - 1]).equals(DataRecord.VALIDFROM)) {
                keyFieldCount--;
            }
            int[] keyFieldIdx = new int[keyFieldCount];
            for (int i = 0; i < keyFieldIdx.length; i++) {
                keyFieldIdx[i] = record.metaData.getFieldIndex(keyFields[i]);
            }

            // get the field indexes of the referencing records
            int[] referencingFieldIdx = new int[referencingFields.length];
            for (int i = 0; i < referencingFieldIdx.length; i++) {
                referencingFieldIdx[i] = referencingPersistence.data().getMetaData().getFieldIndex(referencingFields[i]);
            }

            // get the field indexes of the referencing records's filter fields ("where clause")
            int[] referencingFieldFilterIdx = (referencingFieldFilerNames != null ? new int[referencingFieldFilerNames.length] : null);
            for (int i = 0; referencingFieldFilterIdx != null && i < referencingFieldFilterIdx.length; i++) {
                referencingFieldFilterIdx[i] = referencingPersistence.data().getMetaData().getFieldIndex(referencingFieldFilerNames[i]);
            }

            // search referencingPersistence for any references to this record
            DataKeyIterator it = referencingPersistence.data().getStaticIterator();
            DataKey key = it.getFirst();
            while (key != null) {
                DataRecord r = referencingPersistence.data().get(key);
                int matching = 0;
                if (r != null && !r.getDeleted()) {

                    // check "where clause"
                    boolean checkThisRecord = (referencingFieldFilterIdx == null);
                    for (int i = 0; referencingFieldFilterIdx != null && i < referencingFieldFilterIdx.length; i++) {
                        Object o = r.get(referencingFieldFilterIdx[i]);
                        if (o == null || !o.equals(referencingFieldFilerValues[i])) {
                            break;
                        } else {
                            if (i == referencingFieldFilterIdx.length - 1) {
                                checkThisRecord = true; // all "where clause" fields match
                            }
                        }
                    }

                    // check whether references match our record
                    for (int i = 0; checkThisRecord && i < referencingFieldIdx.length; i++) {
                        Object o = r.get(referencingFieldIdx[i]);
                        if (o != null) {
                            if (o instanceof DataTypeList) {
                                DataTypeList list = (DataTypeList) o;
                                if (list.contains(record.get(keyFieldIdx[i]))) {
                                    if (keyMustMatchInAllReferencingFields) {
                                        matching++;
                                    } else {
                                        matching = referencingFieldIdx.length;
                                        break;
                                    }
                                }
                            } else {
                                if (o.equals(record.get(keyFieldIdx[(keyMustMatchInAllReferencingFields ? i : 0)]))) {
                                    if (keyMustMatchInAllReferencingFields) {
                                        matching++;
                                    } else {
                                        matching = referencingFieldIdx.length;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (matching == referencingFieldIdx.length) {
                    refRec = r.getQualifiedName();
                    break;
                }
                key = it.getNext();
            }
        } catch (Exception e) {
            Logger.logdebug(e);
            throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                    e.toString(),
                    Thread.currentThread().getStackTrace());
        }
        if (refRec != null) {
            throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                    International.getMessage("Der Datensatz kann nicht gelöscht werden, da er noch von {listtype} '{record}' genutzt wird.",
                    referencingPersistence.getDescription(), refRec),
                    Thread.currentThread().getStackTrace());
        }
    }

    public String transformFieldName(String fieldName) {
        // to be overwritten by subclass if field name changes and needs to be corrected during parsing
        return fieldName;
    }

}
