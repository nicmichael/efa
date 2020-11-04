/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.storage;

import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.core.BackupMetaDataItem;
import java.util.zip.ZipOutputStream;

// @i18n complete

public interface IDataAccess {

    int TYPE_FILE_XML = 1;
    int TYPE_EFA_REMOTE = 2;
    int TYPE_EFA_CLOUD = 3;

    String TYPESTRING_FILE_XML = "file/xml";
    String TYPESTRING_EFA_REMOTE = "efa/remote";
    String TYPESTRING_EFA_CLOUD = "file/efaCloud";

    // Data Types supported by IDataAccess
    // Data Type                 // Internal Java Type
    int DATA_STRING = 0;         // String
    int DATA_INTEGER = 1;        // int, Integer
    int DATA_LONGINT = 2;        // long, Long
    int DATA_DOUBLE = 3;         // double, Double
    int DATA_DECIMAL = 4;        // DataTypeDecimal
    int DATA_DISTANCE = 5;       // DataTypeDistance
    int DATA_BOOLEAN = 6;        // boolean, Boolean
    int DATA_DATE = 7;           // DataTypeDate
    int DATA_TIME = 8;           // DataTypeTime
    int DATA_UUID = 9;           // java.util.UUID
    int DATA_INTSTRING = 10;     // Number-String mixed String
    int DATA_PASSWORDH = 11;     // DataTypePasswordHashed
    int DATA_PASSWORDC = 12;     // DataTypePasswordCrypted
    int DATA_TEXT = 13;          // String. Text is used by Efacloud for fields needing > 256 characters length
    int DATA_LIST_STRING = 100;  // String-based list
    int DATA_LIST_INTEGER = 101; // Integer-based list
    int DATA_LIST_UUID = 108;    // UUID-based list
    int DATA_VIRTUAL = 999;      // Virtual String, will not be stored in file

    int UNDEFINED_INT  = Integer.MIN_VALUE + 1;
    long UNDEFINED_LONG = Long.MIN_VALUE + 1;
    double UNDEFINED_DOUBLE  = -Double.MAX_VALUE+1;


    /**
     * Sets the associated Persistence object for this Data Access.
     */
    void setPersistence(StorageObject persistence);

    /**
     * Returns the associated Persistence object for this Data Access.
     * @return the Persistence object
     */
    StorageObject getPersistence();

    /**
     * Returns the storage type of this implementation (e.g. CSV file, XML file or SQL database)
     * @return one of the TYPE_xxx constants
     */
    int getStorageType();

    /**
     * Sets the storage location (e.g. file system directory or database connect string)
     * Examples:
     * /home/efa/data/ (with or without trailing file.separator)
     * jdbc:mysql://localhost:1234/efa
     * Note: the combination StorageLocation/StorageObjectName.StorageObjectType must be unique!
     * @param location the storage location
     */
    void setStorageLocation(String location);

    /**
     * Returns the storage location (e.g. file system directory (always with trailing file.separator) or database connect string)
     * @return the storage location
     */
    String getStorageLocation();

    /**
     * Sets the storage object name (e.g. file name (without file extension) or database table name (just postfix))
     * Examples:
     * 2009 (for a logbook with the name "2009")
     * Note: the combination StorageLocation/StorageObjectName.StorageObjectType must be unique!
     * @param name the storage object name
     */
    void setStorageObjectName(String name);

    /**
     * Returns the storage object name (e.g. file name (without file extension) or database table name (just postfix))
     * @return the storage object name
     */
    String getStorageObjectName();

    /**
     * Sets the storage object type (e.g. logbook, members list, boat list, ...) which may be used as a file extension or database table name prefix
     * Examples:
     * efb (for a logbook)
     * efbm (for a members list)
     * efbb (for a boat list)
     * Note: the combination StorageLocation/StorageObjectName.StorageObjectType must be unique!
     * @param type the storage object name
     */
    void setStorageObjectType(String type);

    /**
     * Returns the storage object type (e.g. logbook, members list, boat list, ...) which may be used as a file extension or databale table name prefix
     * @return the storage object type
     */
    String getStorageObjectType();

    /**
     * Sets an informal description of this storage object, e.g. "Logbook", to be displayed on GUI items or in logfiles
     * @param description the storage object description
     */
    void setStorageObjectDescription(String description);

    /**
     * Returns an informal description of this storage object, e.g. "Logbook", to be displayed on GUI items or in logfiles
     * @return the storage object description
     */
    String getStorageObjectDescription();

    /**
     * Returns a unique ID for this Storage Object
     * @return an ID
     */
    String getUID();

    /**
     * Sets the username to access the storage object.
     * @param username the username to access the storage object
     */
    void setStorageUsername(String username);

    /**
     * Sets the password to access the storage object.
     * @param password the password to access the storage object
     */
    void setStoragePassword(String password);

    /**
     * Returns the username to access the storage object
     * @return the username to access the storage object
     */
    String getStorageUsername();

    /**
     * Returns the password to access the storage object
     * @return the password to access the storage object
     */
    String getStoragePassword();

    /**
     * Tests whether a storage object already exists.
     * For file systems, this tests whether the associated file in the file system exists.
     * In order to succeed, the storage object location, object name and objecet type must have been specified before.
     * @throws EfaException if the existance of the storage object could not be verified.
     */
    boolean existsStorageObject() throws EfaException;

    /**
     * Creates a new storage object (overwrites existing objects).
     * For file systems, this method may imply recursive creation of directories as well.
     * In order to succeed, the storage object location, object name and objecet type must have been specified before.
     * @throws EfaException if the creation of the object failed.
     */
    void createStorageObject() throws EfaException;

    /**
     * Opens an existing storage object.
     * @throws EfaException if the opening of the object failed.
     */
    void openStorageObject() throws EfaException;

    /**
     * Closes this storage object. Uncommitted changes to this object will be lost.
     * @throws EfaException if the closing of this object failed.
     */
    void closeStorageObject() throws EfaException;

    /**
     * Checks whether the storage object is currently open.
     * @return true if the storage object is open
     */
    boolean isStorageObjectOpen();

    /**
     * Checks whether the storage object is currently being opened (i.e. within openStorageObject(), which has not yet finished)
     * @return true if the storage object is currently being opened
     */
    boolean inOpeningStorageObject();

    /**
     * Deletes an existing storage object.
     * @throws EfaException if the deletion of the object failed.
     */
    void deleteStorageObject() throws EfaException;

    /**
     * Returns the current version of the storage object.
     * @return the version identifier
     * @throws EfaException
     */
    String getStorageObjectVersion() throws EfaException;

    /**
     * Sets the current version of the storage object.
     * @param version the version identifier
     * @throws EfaException
     */
    void setStorageObjectVersion(String version) throws EfaException;

    /**
     * Locks the entire storage object for exclusive write access.
     * Locking only affects DML operations. Storage Object operations
     * (as for example closing the storage object) are still permitted.
     * Locking may time out depending on the implementation of the underlying storage object.
     * @return a lock ID
     * @throws EfaException if the storage object is already locked or cannot be
     * locked at the moment (e.g. because it has already been closed)
     */
    long acquireGlobalLock() throws EfaException;

    /**
     * Locks one data record in the storage object for exclusive write access.
     * Locking only affects DML operations. Storage Object operations
     * (as for example closing the storage object) are still permitted.
     * Locking may time out depending on the implementation of the underlying storage object.
     * @return a lock ID
     * @throws EfaException if the data record or the storage object is already locked or cannot be
     * locked at the moment (e.g. because it has already been closed)
     */
    long acquireLocalLock(DataKey key) throws EfaException;

    /**
     * Releases a previous acquired global lock.
     * @param lockID the lock ID
     */
    boolean releaseGlobalLock(long lockID);

    /**
     * Releases a previous acquired local lock.
     * @param lockID the lock ID
     */
    boolean releaseLocalLock(long lockID);

    /**
     * Returns the current SCN.
     * @return the SCN
     * @throws EfaException
     */
    long getSCN() throws EfaException;

    /**
     * Registers a new data field.
     * @param fieldName the name of the new field
     * @param dataType the type of the new field
     * @throws EfaException
     */
    void registerDataField(String fieldName, int dataType) throws EfaException;

    /**
     * Creates an index on the specified fields.
     * @param fieldNames the fields to create the index on.
     * @throws EfaException
     */
    void createIndex(String[] fieldNames) throws EfaException;


    /**
     * Specifies the key fields for this storage object. The combination of key field
     * values must be unique in the storage object.
     * @param fieldNames an array of existing fields to be used as key.
     * @throws EfaException
     */
    void setKey(String[] fieldNames) throws EfaException;

    /**
     * Sets the meta data associated with this data access.
     * This must be done prior to using the data access.
     * @param meta the meta data
     */
    void setMetaData(MetaData meta);

    /**
     * Returns the meta data associated with this data access.
     * @return the meta data
     */
    MetaData getMetaData();


    /**
     * Returns the names of the key fields of this storage object.
     * @return the key field names
     */
    String[] getKeyFieldNames();

    /**
     * Returns all field names (including virtual field names) of this storage object.
     * @return the field names
     */
    String[] getFieldNames();

    /**
     * Returns all field names of this storage object.
     * @param includingVirtual  also return virtual fields
     * @return the field names
     */
    String[] getFieldNames(boolean includingVirtual);

    /**
     * Returns the field type for a given field name.
     * @param fieldName the field name
     * @return the field type
     * @throws EfaException
     */
    int getFieldType(String fieldName) throws EfaException;

    /**
     * Constructs a key from a given (non-empty) DataRecord.
     * @param record the data record
     * @return the key
     * @throws EfaException
     */
    DataKey constructKey(DataRecord record) throws EfaException;

    /**
     * Constructs an unversionized key from a given key (by removing the validFrom field).
     * @param key the versionized key
     * @return the "unversionized" key
     * @throws EfaException
     */
    DataKey getUnversionizedKey(DataKey key);

    /**
     * Adds a new data record to this storage object.
     * @param record the data record to add
     * @throws EfaException if the data record already exists or the operation fails for another reason
     */
    void add(DataRecord record) throws EfaException;

    /**
     * Adds a new data record to this storage object with a previously acquired local or global lock.
     * @param record the data record to add
     * @param lockID an ID of a previously acquired local or global lock
     * @throws EfaException if the data record already exists or the operation fails for another reason
     */
    void add(DataRecord record, long lockID) throws EfaException;

    /**
     * Adds a new data record to this storage object.
     * This data record will be valid from timestamp t. If another data record is already valid around t,
     * its validity range will be automatically adapted to end at t-1.
     * Note: This method requires a global lock!
     * @param record the data record to add
     * @param t the ValidFrom timestamp
     * @throws EfaException if the data record already exists or the operation fails for another reason
     * @returns the key of the just added record
     */
    DataKey addValidAt(DataRecord record, long t) throws EfaException;

    /**
     * Adds a new data record to this storage object with a previously acquired global lock.
     * This data record will be valid from timestamp t. If another data record is already valid around t,
     * its validity range will be automatically adapted to end at t-1.
     * Note: This method requires a global lock!
     * @param record the data record to add
     * @param t the ValidFrom timestamp
     * @param lockID an ID of a previously acquired local or global lock
     * @throws EfaException if the data record already exists or the operation fails for another reason
     * @return the key of the just added record
     */
    DataKey addValidAt(DataRecord record, long t, long lockID) throws EfaException;

    /**
     * Adds an array of new data record to this storage object.
     * @param records the data records to add
     * @throws EfaException if any of the data record already exists or the operation fails for another reason
     */
    void addAll(DataRecord[] records, long lockID) throws EfaException;

    /**
     * Updates an existing one in this storage object.
     * @param record the data record to update
     * @return the updated record
     * @throws EfaException if the data record is locked or the operation fails for another reason
     */
    DataRecord update(DataRecord record) throws EfaException;

    /**
     * Updates an existing one in this storage object with a previously acquired local or global lock.
     * @param record the data record to update
     * @param lockID an ID of a previously acquired local or global lock
     * @return the updated record
     * @throws EfaException if the data record is locked or the operation fails for another reason
     */
    DataRecord update(DataRecord record, long lockID) throws EfaException;

    /**
     * Deletes an existing data record from this storage object.
     * @param key the key of the data record to delete
     * @throws EfaException if the data record does not exist, is locked or the operation fails for another reason
     */
    void delete(DataKey key) throws EfaException;

    /**
     * Deletes an existing data record from this storage object with a previously acquired local or global lock.
     * @param key the key of the data record to delete
     * @param lockID an ID of a previously acquired local or global lock
     * @throws EfaException if the data record does not exist, is locked or the operation fails for another reason
     */
    void delete(DataKey key, long lockID) throws EfaException;

    /**
     * Deletes an existing data record from this storage object.
     * This method will adapt other data records with adjacent validity ranges, depending on the value of merge:
     * merge = 0: Don't merge, leave a "hole" where no records are valid
     * merge = -1: Merge left record, i.e. extend InvalidFrom from previous record to the InvalidFrom of the record to be deleted.
     * merge = 1: Merge right record, i.e. lower ValidFrom from next record to the ValidFrom of the record to be deleted.
     * Note: This method requires a global lock!
     * @param key the key of the data record to delete
     * @param merge specifies how to merge adjacent record's validity ranges
     * @throws EfaException if the data record does not exist, is locked or the operation fails for another reason
     */
    void deleteVersionized(DataKey key, int merge) throws EfaException;

    /**
     * Deletes an existing data record from this storage object with a previously acquired global lock.
     * This method will adapt other data records with adjacent validity ranges, depending on the value of merge:
     * merge = 0: Don't merge, leave a "hole" where no records are valid
     * merge = -1: Merge left record, i.e. extend InvalidFrom from previous record to the InvalidFrom of the record to be deleted.
     * merge = 1: Merge right record, i.e. lower ValidFrom from next record to the ValidFrom of the record to be deleted.
     * Note: This method requires a global lock!
     * @param key the key of the data record to delete
     * @param merge specifies how to merge adjacent record's validity ranges
     * @param lockID an ID of a previously acquired global lock
     * @throws EfaException if the data record does not exist, is locked or the operation fails for another reason
     */
    void deleteVersionized(DataKey key, int merge, long lockID) throws EfaException;

    void deleteVersionizedAll(DataKey key, long deleteAt) throws EfaException;

    /**
     * Deletes all versions of an existing data record from this storage object with a previously acquired global lock.
     * Depending on the parameter deleteAt, this method will
     * if deleteAt == -1: mark all versions of this record as deleted
     * if deleteAt >= 0:
     *   - leave all versions before deleteAt untouched
     *   - change the validity of the version that is currently valid at deleteAt to become invalid at deleteAt
     *   - physically delete all versions which become valid after deleteAt, unless this is the last version of this record
     *     (in this case, it will only be marked deleted to make sure that at least one version still physically remains)
     * Note: This method requires a global lock!
     * @param key the key of the data record to delete
     * @param deleteAt the time at which the records should be deleted
     * @param lockID an ID of a previously acquired global lock
     * @throws EfaException if the data record does not exist, is locked or the operation fails for another reason
     */
    void deleteVersionizedAll(DataKey key, long deleteAt, long lockID) throws EfaException;

    /**
     * Changes the validity range of an existing data record from this storage object.
     * This method will adapt other data records with adjacent validity ranges, if necessary.
     * @param record the record to be changed
     * @param validFrom the new validFrom value
     * @param invalidFrom the new invalidFrom value
     * @throws EfaException if the data record does not exist, is locked or the operation fails for another reason
     */
    void changeValidity(DataRecord record, long validFrom, long invalidFrom) throws EfaException;

    /**
     * Changes the validity range of an existing data record from this storage object with a previously acquired global lock.
     * This method will adapt other data records with adjacent validity ranges, if necessary.
     * Note: This method requires a global lock!
     * @param record the record to be changed
     * @param validFrom the new validFrom value
     * @param invalidFrom the new invalidFrom value
     * @param lockID an ID of a previously acquired global lock
     * @throws EfaException if the data record does not exist, is locked or the operation fails for another reason
     */
    void changeValidity(DataRecord record, long validFrom, long invalidFrom, long lockID) throws EfaException;

    /**
     * Retrieves an existing data record from this storage object.
     * @param key the key of the data record to retrieve
     * @throws EfaException if the data record is locked or the operation fails for another reason
     */
    DataRecord get(DataKey key) throws EfaException;

    /**
     * Retrieves all existing data records valid at any point in time from this storage object.
     * @param key the key of the data record to retrieve (with or without the validity information)
     * @throws EfaException if the data record is locked or the operation fails for another reason
     */
    DataRecord[] getValidAny(DataKey key) throws EfaException;

    /**
     * Retrieves an existing data record valid at a specified point in time from this storage object.
     * @param key the key of the data record to retrieve (with or without the validity information)
     * @param t the time at which this record is valid
     * @throws EfaException if the data record does not exist, is locked or the operation fails for another reason
     */
    DataRecord getValidAt(DataKey key, long t) throws EfaException;

    /**
     * Retrieves the latest valid version of an existing data record from this storage object.
     * @param key the key of the data record to retrieve (with or without the validity information)
     * @throws EfaException if the data record does not exist, is locked or the operation fails for another reason
     */
    DataRecord getValidLatest(DataKey key) throws EfaException;

    /**
     * Retrieves the nearest valid version of an existing data record from this storage object.
     * @param key the key of the data record to retrieve (with or without the validity information)
     * @param earliestValidAt earlist timestamp which is accepted to fall into the validity range of the record
     * @param latestValidAt latest timestamp which is accepted to fall into the validity range of the record
     * @param preferredValidAt preferred validity shall be as close as possible to this timestamp
     * @throws EfaException if the data record does not exist, is locked or the operation fails for another reason
     */
    DataRecord getValidNearest(DataKey key, long earliestValidAt, long latestValidAt, long preferredValidAt) throws EfaException;

    /**
     * Returns true if there are any data records valid at any point in time from this storage object.
     * @param key the key of the data record to retrieve (with or without the validity information)
     * @throws EfaException if the data record is locked or the operation fails for another reason
     */
    boolean isValidAny(DataKey key) throws EfaException;

    /**
     * Retrieves all keys for data records specified through fieldNames and values
     * @param fieldNames the field names for the corresponding values
     * @param values the values to search for
     * @return all matching keys
     * @throws EfaException
     */
    DataKey[] getByFields(String[] fieldNames, Object[] values) throws EfaException;

    /**
     * Retrieves all keys valid at validAt for data records specified through fieldNames and values
     * @param fieldNames the field names for the corresponding values
     * @param values the values to search for
     * @param validAt the time the record shall be valid, or -1 if any
     * @return all matching keys
     * @throws EfaException
     */
    DataKey[] getByFields(String[] fieldNames, Object[] values, long validAt) throws EfaException;

    /**
     * Returns a count of all the records where all fields match the gives values
     * @return the number of data records
     * @throws EfaException
     */
    long countRecords(String[] fieldNames, Object[] values) throws EfaException;

    /**
     * Returns the number of data records in this storage object.
     * @return the number of data records
     * @throws EfaException
     */
    long getNumberOfRecords() throws EfaException;

    /**
     * Truncates (deletes) all data records in this storage object.
     * @throws EfaException
     */
    void truncateAllData() throws EfaException;

    /**
     * Truncates all records from this storage objects and copies the data from
     * source, thus re-building this storage object from source' contents
     * @param source
     * @throws EfaException
     */
    void copyFromDataAccess(IDataAccess source) throws EfaException;

    String getTypeName(int type);

    DataKey[] getAllKeys() throws EfaException;
    DataKeyIterator getStaticIterator() throws EfaException;
    DataKeyIterator getDynamicIterator() throws EfaException;
    /*
    DataRecord getCurrent(DataKeyIterator it) throws EfaException;
    DataRecord getFirst(DataKeyIterator it) throws EfaException;
    DataRecord getLast(DataKeyIterator it) throws EfaException;
    DataRecord getNext(DataKeyIterator it) throws EfaException;
    DataRecord getPrev(DataKeyIterator it) throws EfaException;
    */
    DataRecord getFirst() throws EfaException;
    DataRecord getLast() throws EfaException;

    void setPreModifyRecordCallbackEnabled(boolean enabled);
    boolean isPreModifyRecordCallbackEnabled();
    void setInOpeningStorageObject(boolean inOpening);

    void saveToXmlFile(String filename) throws EfaException;
    BackupMetaDataItem saveToZipFile(String dir, ZipOutputStream zipOut) throws EfaException;

}
