/*
  Title:        efa - elektronisches Fahrtenbuch für Ruderer Copyright:    Copyright (c) 2001-2011
  by Nicolas Michael Website:      http://efa.nmichael.de/ License:      GNU General Public License
  v2

  @author Nicolas Michael, Martin Glade (efacloud adaptation)
 * @version 2
 */

package de.nmichael.efa.data.storage;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.efacloud.*;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

import java.util.ArrayList;

/**
 * In order to provide a local cache, the standard XML file storage is used, but a function to
 * trigger the web transactions are added.
 */
public class EfaCloudStorage extends XMLFile {

    private final TxRequestQueue txQueue;
    private final String tablename;
    EfaCloudSynch efaCloudSynch;
    private int succeded;
    private int skipped;
    private int failed;

    public int getFailed() {
        return failed;
    }

    private DataKey serverModifiedRecordKey;

    /**
     * Create a Web DB access. This is a local storage (csv) for caching and offline usage and an
     * interface to an efaDB server.
     *
     * @param efaCloudURL     URL of efaCoud Server
     * @param directory       directory of the cached file
     * @param storageUsername username for the efaDB server access
     * @param storagePassword password for the efaDB server access
     * @param filename        filename of the cached file
     * @param extension       extension of the cached file. This also indicated the data base table
     *                        schema
     * @param description     The storage type description
     * @throws EfaException   If the first nop operation returns an authentication failure.
     */
    public EfaCloudStorage(String efaCloudURL, String directory, String storageUsername,
                           String storagePassword, String filename, String extension,
                           String description) throws EfaException {
        super(directory, filename, extension, description);
        this.txQueue = TxRequestQueue
                .getInstance(efaCloudURL, storageUsername, storagePassword, directory);
        tablename = TableBuilder.efaCloudTableNames.get(getStorageObjectType());
    }

    public int getStorageType() {
        return IDataAccess.TYPE_EFA_CLOUD;
    }

    /**
     * Build a modify transaction for the efacloud api and append it to the server transaction
     * queue.
     *
     * @param dataRecord the new record
     * @param add        set true, if the modify transaction is an insert to
     * @param update     set true, if the modify transaction is an update
     * @param delete     set true, if the modify transaction is a record deletion
     * @param synchronously     set true, to enforce synchronous transaction handling
     * @return modification transaction
     */
    public Transaction modifyServerRecord(DataRecord dataRecord, boolean add, boolean update,
                                   boolean delete, boolean synchronously) {
        return modifyServerRecord(dataRecord, add, update, delete, synchronously, null);
    }

    /**
     * Build a modify transaction for the efacloud api and append it to the server transaction
     * queue.
     *
     * @param dataRecord the new or changed record, or the record to delete
     * @param add        set true, if the modify transaction is an insert to
     * @param update     set true, if the modify transaction is an update
     * @param delete     set true, if the modify transaction is a record deletion
     * @param synchronously     set true, to enforce synchronous transaction handling
     * @param sender     the sender of the transaction. The object link will be used to filter on
     *                   own transaction receipts for statistics and logging as is used for
     *                   synchronisation tasks.
     * @return modification transaction
     */
    private Transaction modifyServerRecord(DataRecord dataRecord, boolean add, boolean update,
                                   boolean delete, boolean synchronously, Object sender) {
        String type = (add) ? "insert" : (update) ? "update" : (delete) ? "delete" : "nop";
        ArrayList<String> record = new ArrayList<>();
        for (String field : dataRecord.getFields()) {
            String value = dataRecord.getString(field);
            if ((value != null && value.length() > 0) || update) record.add(field + ";" + CsvCodec
                    .encodeElement(value, CsvCodec.DEFAULT_DELIMITER,
                            CsvCodec.DEFAULT_QUOTATION));   // fields need no csv encoding
        }
        String[] rArray = record.toArray(new String[0]);
        Transaction tx = new Transaction(-1, type, tablename, rArray);
        if (sender != null) tx.sender = sender;
        if (synchronously)
            tx.executeSynchronously(txQueue);
        else
            txQueue.appendTransactionPending(tx);
        return tx;
    }

    /**
     * Transport a side data into the local data base. This will use the standard modifyDataRecord
     * procedure of its super class XMLfile, but remember the data key. That way the standard
     * procedure, can distinguish this call from a normal call and bypass the call of the the
     * modifyServerRecord.
     *
     * @param dataRecord the new or changed record, or the record to delete
     * @param lock       the lock which shall be used (usually a global lock)
     * @param add        set true, if the modify transaction is an insert to
     * @param update     set true, if the modify transaction is an update
     * @param delete     set true, if the modify transaction is a record deletion
     * @throws EfaException if the data manipulation at XMLfile ran into an error.
     */
    protected void modifyLocalRecord(DataRecord dataRecord, long lock, boolean add, boolean update,
                                     boolean delete) throws EfaException {
        this.serverModifiedRecordKey = constructKey(dataRecord);
        if (add) add(dataRecord, lock);
        if (update) update(dataRecord, lock);
        if (delete) delete(constructKey(dataRecord), lock);
        this.serverModifiedRecordKey = null;
    }

    /**
     * Synchronous upload all data. Expects an empty table at the server side. Returns after all
     * uploads have been performed.
     *
     * @return the records uploaded or -1 in case of errors
     */
    public String uploadAllRecords(EfaCloudSynch efaCloudSynch) {

        this.efaCloudSynch = efaCloudSynch;

        String dropResult = Daten.tableBuilder.initServerTable(getStorageObjectType());
        String logMsg = (dropResult.isEmpty()) ? LogString
                .efaCloudSynchInit(getStorageObjectDescription()) : LogString
                .efaCloudSynchFailed(filename, getStorageObjectDescription(),
                        "Server table initialization failed: " + dropResult);
        this.efaCloudSynch.logMsg(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_INFO, logMsg);

        int requested = 0;
        int txIDstart = 0;

        DataKeyIterator keyIterator = null;
        try {
            keyIterator = getStaticIterator();
            requested = getAllKeys().length;
        } catch (EfaException e) {
            e.printStackTrace();
        }
        if (keyIterator == null)
            return "Cannot initialize the key iterator.";
        int n = 0;

        DataKey k = keyIterator.getFirst();
        while (k != null) {
            DataRecord r = null;
            try {
                r = get(k);
            } catch (EfaException ignored) {
                // TODO: check needed action
            }
            if (r == null) continue;
            Transaction txModified = modifyServerRecord(r, true, false, false, false, this);
            int txID = txModified.ID;
            if (txIDstart == 0) txIDstart = txID;
            n++;
            if (n % 50 == 0) {
                // Wait for transactions to complete
                Transaction tx = new Transaction(-1, "synch", tablename,
                        new String[]{"LastModified;0"});
                tx.executeSynchronously(txQueue);
                // log progress to user
                int serverSideCount = Integer.parseInt(tx.getResultMessage().split("=")[1]);
                int failed = txQueue.getReceiptsFromIDonwards(txIDstart, this, false, true).size();
                this.efaCloudSynch.logMsg(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_INFO, LogString
                        .efaCloudSynchProgress(getStorageObjectDescription(), requested,
                                serverSideCount, failed));
            }
            k = keyIterator.getNext();
        }
        // append a nop transaction at the very end to wait for completion of all uploads.
        Transaction tx = new Transaction(-1, "synch", tablename, new String[]{"LastModified;0"});
        // int serverSideCount = Integer.parseInt(tx.getResultMessage().split("=")[1]);
        tx.executeSynchronously(txQueue);
        int serverSideCount = Integer.parseInt(tx.getResultMessage().split("=")[1]);
        // int successful = txQueue.getReceiptsFromIDonwards(txIDstart, this, true, false).size();
        int failed = txQueue.getReceiptsFromIDonwards(txIDstart, this, false, true).size();
        logMsg = LogString
                .efaCloudSynchSuccessfull(getStorageObjectDescription(), serverSideCount, failed);
        this.efaCloudSynch.logMsg(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_INFO, logMsg);
        this.efaCloudSynch = null;
        return (failed > 0) ? logMsg : "";
    }

    /**
     * Synchronous download of all data.
     *
     * @param efaCloudSynch         the synchronisation engine to pass the log information to. set
     *                              null to skip logging.
     * @param lastModifiedLaterThan The value passed on to the server to filter for records modified
     *                              later than this timestamp. Set 0 to get all.
     * @return the result message of the transaction, i.e. the csv String representing the values
     * returned by the server.
     */
    public String downloadRecords(EfaCloudSynch efaCloudSynch, long lastModifiedLaterThan) {
        this.efaCloudSynch = efaCloudSynch;
        String logMsg = LogString.efaCloudSynchInit(getStorageObjectDescription());
        this.efaCloudSynch.logMsg(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_INFO, logMsg);
        Transaction tx = new Transaction(-1, "select", tablename,
                new String[]{"LastModified;" + lastModifiedLaterThan});
        tx.executeSynchronously(txQueue);
        return tx.getResultMessage();
    }

    /**
     * Read a csv String and fill this storage object with its data. Clear this StorageObject
     * first.
     *
     * @param csvString csv dta to parse
     * @param fullTable set true, to clear the table and replace it. Set false, to update records
     *                  with matching keys.
     * @throws EfaException if the global lock for reading could not be got.
     */
    public synchronized String readString(String csvString, boolean fullTable) throws EfaException {
        long lock;
        try {
            lock = acquireGlobalLock();
        } catch (EfaException e) {
            throw new EfaException(Logger.MSG_DATA_READFAILED, LogString
                    .fileReadFailed(filename, storageLocation,
                            "Cannot get Global Lock for server data import."),
                    Thread.currentThread().getStackTrace());
        }

        String csvParseResult;
        if (fullTable) clearAllData();

        // this call now reads the data
        csvParseResult = parse(csvString, lock, fullTable);

        // conclude
        String logMsg = (csvParseResult.length() == 0) ? LogString
                .efaCloudSynchSuccessfull(getStorageObjectDescription(), succeded + skipped,
                        failed) : LogString
                .efaCloudSynchFailed(getStorageObjectName(), getStorageObjectDescription(),
                        csvParseResult);
        this.efaCloudSynch.logMsg(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_INFO, logMsg);
        releaseGlobalLock(lock);
        return csvParseResult;
    }

    /**
     * Parse a csv String into the efacloud local data base. Used for data download only. Modify
     * data records, if existing and add those which are new. Deletes those which are not contained
     * in the download file. Skips those for which server and client record are identical in
     * values.
     *
     * @param csvString   csv String as was downloaded from the efacloud server using the select
     *                    command without any filter.
     * @param globalLock  the global lock needed for any restore activity.
     * @param deleteExtra Set true to delete those local records which are not contained in the
     *                    csvString.
     */
    public String parse(String csvString, long globalLock, boolean deleteExtra) {
        // parse the text
        char d = CsvCodec.DEFAULT_DELIMITER;
        char q = CsvCodec.DEFAULT_QUOTATION;
        int MAX_VALUE_LENGTH = 65536;
        DataRecord dataRecord;
        StringBuilder documentReadError = new StringBuilder();
        ArrayList<String> lines = CsvCodec.splitLines(csvString, q);
        if (lines.isEmpty()) {
            return "Info: Table is empty.";
        }
        ArrayList<String> header = CsvCodec.splitEntries(lines.get(0), d, q, true);
        succeded = 0;
        skipped = 0;
        failed = 0;
        ArrayList<DataKey> serverKeys = new ArrayList<>();
        // compare with existing records and modify, if needed.
        StorageObject storageObject = this.getPersistence();
        for (int i = 1; i < lines.size(); i++) {
            ArrayList<String> row = CsvCodec.splitEntries(lines.get(i), d, q, true);
            if (row.size() != header.size()) documentReadError.append(International.getMessage(
                    "Parsing CSV-String warning. Row {row} length {rlen} does not match the " +
                            "header " + "length {hlen}. ",
                    i, row.size(), header.size()));
            // Read the record by creating a new one and reading the fields into it.
            dataRecord = storageObject.createNewRecord();
            int c = 0;
            for (String fieldName : header) {
                try {
                    fieldName = storageObject.transformFieldName(fieldName);
                    String value = (row.get(c) != null ? row.get(c).trim() : null);
                    if ((value != null) && (value.length() > MAX_VALUE_LENGTH)) {
                        Logger.log(Logger.ERROR, Logger.MSG_FILE_PARSEERROR,
                                "EfaCloudStorage: Value in field " + fieldName + " seems " +
                                        "corrupted: It has length of " + value
                                        .length() + " bytes. Value will be truncated. Value " +
                                        "(first 100 bytes: " + value
                                        .substring(0, 100) + ")");
                        value = value.substring(0, MAX_VALUE_LENGTH);
                    }
                    if (value != null && value.isEmpty()) value = null;
                    dataRecord.set(fieldName, value, false);
                    c++;
                } catch (Exception e) {
                    Logger.log(Logger.ERROR, Logger.MSG_FILE_PARSEERROR,
                            "EfaCloudStorage: Parse Error for Field " + fieldName + " = " + ((c < row
                                    .size()) ? row.get(c) : "???") + ": " + e.toString());
                    failed++;
                }
            }
            // compare the new one with what is existing locally.
            try {
                DataKey serverKey = constructKey(dataRecord);
                serverKeys.add(serverKey);
                DataRecord currentRecord = get(serverKey);
                if (currentRecord == null) {
                    // add the record, because the Id is not known.
                    modifyLocalRecord(dataRecord, globalLock, true, false, false);
                    succeded++;
                } else if (equalInValues(currentRecord, dataRecord)) {
                    skipped++;
                } else {
                    // update the record, because it is locally known and the locaL version
                    // differs from the server side version.
                    if (dataRecord.getLastModified() >= currentRecord.getLastModified()) {
                        modifyLocalRecord(dataRecord, globalLock, false, true, false);
                        succeded++;
                    } else {
                        // local version is younger. Keep it and issue an error. This can only happen
                        // if a write transaction was lost on its way to the server.
                        String errorMessage =
                                "Server record is older than local record in " + getStorageObjectDescription() + ". Keeping local record with key " + serverKey
                                        .toString();
                        documentReadError.append(errorMessage);
                        Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR, errorMessage);
                        failed++;
                    }
                }
            } catch (EfaException e) {
                documentReadError.append(e.getMessage());
                failed++;
            }
        }
        // now check which of the local records are not contained in the server side ones.
        if (deleteExtra) {
            try {
                ArrayList<DataKey> toDelete = new ArrayList<>();
                DataKeyIterator it = getStaticIterator();
                DataKey dk = it.getFirst();
                while (dk != null) {
                    boolean matched = false;
                    for (DataKey dks : serverKeys)
                        if (dks.compareTo(dk) == 0) {
                            matched = true;
                            break;
                        }
                    if (!matched) toDelete.add(dk);
                    dk = it.getNext();
                }
                for (DataKey dkd : toDelete) {
                    this.serverModifiedRecordKey = dkd;
                    modifyLocalRecord(get(dkd), globalLock, false, true, false);
                    this.serverModifiedRecordKey = null;
                }
            } catch (EfaException e) {
                documentReadError.append("Failed to delete local extra record: ")
                        .append(e.getMessage());
            }
        }
        return documentReadError.toString();
    }

    /**
     * Provide a check for the super class Data File, whether a modification was server triggered.
     *
     * @param recordToModify record which is bound to be modified in DataFile
     * @return true, if this record modification was triggered by the server.
     */
    public boolean isServerToLocalModification(DataKey recordToModify) {
        return ((serverModifiedRecordKey != null) && (serverModifiedRecordKey
                .compareTo(recordToModify) == 0));
    }

    /**
     * Returns true, if all fields in both records have the same value. Compares the String value of
     * the fields. Null and "" are not equal.
     *
     * @param a the first data record
     * @param b the other data record
     * @return true, if both are equal.
     */
    private boolean equalInValues(DataRecord a, DataRecord b) {
        if (!a.getClass().equals(b.getClass())) return false;
        for (String field : a.getFields()) {
            if (!a.getString(field).equalsIgnoreCase(b.getString(field))) return false;
        }
        return true;
    }

}
