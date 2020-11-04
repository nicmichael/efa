/*
 * <pre>
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael, Martin Glade
 * @version 2</pre>
 */
package de.nmichael.efa.data.efacloud;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.EfaCloudStorage;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static de.nmichael.efa.data.efacloud.TxRequestQueue.RQ_QUEUE_START_SYNCH_UPLOAD;
import static de.nmichael.efa.data.efacloud.TxRequestQueue.TX_SYNCH_QUEUE_INDEX;

class SynchControl {

    // The names of the tables which allow the key to be modified upon server side insert
    static final String[] tables_with_key_fixing_allowed = TableBuilder.fixid_allowed.split(" ");
    static final long clockoffsetBuffer = 600000L; // max number of millis which client clock may be offset

    long timeOfLastSynch;
    long LastModifiedLimit;
    boolean synch_upload = false;

    int table_fixing_index = -1;
    ArrayList<String> tables_to_synchronize = new ArrayList<>();
    int table_synching_index = -1;

    // when running an upload synchronization the server is first asked for all data keys and last modified
    // timestamps together with the last modification. They are put into records, sorted and qualified
    private final HashMap<DataKey, DataRecord> serverRecordsReturned = new HashMap<>();
    private final ArrayList<DataRecord> localRecordsToInsertAtServer = new ArrayList<>();
    private final ArrayList<DataRecord> localRecordsToUpdateAtServer = new ArrayList<>();

    private final TxRequestQueue txq;

    /**
     * Constructor. Initializes the queue reference and file paths.
     *
     * @param txq the queue reference
     */
    SynchControl(TxRequestQueue txq) {
        this.txq = txq;
        timeOfLastSynch = System.currentTimeMillis();
        LastModifiedLimit = timeOfLastSynch - clockoffsetBuffer;
    }

    /**
     * Write a log message to the synch log.
     *
     * @param logMessage     the message to be written
     * @param tablename      the name of the affected table
     * @param dataKey        the datakey of the affected record
     * @param logStateChange set true to append the log to the stae change log rather than the synch log
     */
    void logMessage(String logMessage, String tablename, DataKey dataKey, boolean logStateChange) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dataKeyStr = (dataKey == null) ? "" : " - " + dataKey.toString();
        String dateString = format.format(new Date()) + " [" + tablename + dataKeyStr + "]: " + logMessage;
        TextResource
                .writeContents((logStateChange) ? txq.stateChangeLogFilePath : txq.synchLogFilePath, dateString, true);
    }

    /**
     * <p>Step 1: Start the synchronization process</p><p>Start the synchronization process by appending the first
     * keyfixing request to the synching queue.</p>
     *
     * @param synch_request stet true to run an upload synchronization, false to run a download synchronization
     */
    void startSynchProcess(int synch_request) {
        table_fixing_index = 0;
        this.synch_upload = synch_request == TxRequestQueue.RQ_QUEUE_START_SYNCH_UPLOAD;
        boolean autoTrigger = synch_request == TxRequestQueue.RQ_QUEUE_START_AUTO_SYNCH;
        logMessage("Synchronisation " + ((synch_upload) ? "upload" : "download") + " starting", "@all", null, true);
        // The manually triggered synchronization always takes the full set into account
        LastModifiedLimit = (autoTrigger) ? timeOfLastSynch - clockoffsetBuffer : 0L;
        timeOfLastSynch = System.currentTimeMillis();
        // request first key to be fixed. The record is empty.
        txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, "keyfixing", tables_with_key_fixing_allowed[table_fixing_index],
                (String[]) null);
    }

    /**
     * <p>Step 2: Change a data key, based on the server response on a key fixing request.</p><p>This handles the
     * response on a keyfixing request, if a mismatched key was found (result code 303). Will fix the key locally and
     * create a new keyfixing request to inform the server on the execution. This function must only be called, if the
     * transaction result message contains the record too be fixed.</p>
     *
     * @param tx the transaction with the server response needed for fixing
     */
    void fixOneKeyForTable(Transaction tx) {
        EfaCloudStorage efaCloudStorage = Daten.tableBuilder.getPersistence(tx.tablename);
        ArrayList<DataRecord> dataRecords = efaCloudStorage.parseCsvTable(tx.getResultMessage());
        DataRecord oldDr = null;
        DataRecord newDr = null;
        try {
            oldDr = efaCloudStorage.get(dataRecords.get(1).getKey());
            newDr = efaCloudStorage.get(dataRecords.get(0).getKey());
        } catch (EfaException ignored) {
        }
        if (oldDr == null)
            logMessage(International.getString("Schlüsselkorekturfehler. Alter Schlüssel nicht vorhanden: ") +
                    dataRecords.get(1).getKey().toString(), tx.tablename, dataRecords.get(0).getKey(), false);
        if (newDr != null)
            logMessage(International.getString("Schlüsselkorekturfehler. Neuer Schlüssel schon belegt: ") +
                    dataRecords.get(1).getKey().toString(), tx.tablename, dataRecords.get(0).getKey(), false);
        logMessage(International.getString("Korrigiere Schlüssel von bisher ") + dataRecords.get(1).getKey().toString(),
                tx.tablename, dataRecords.get(0).getKey(), false);
        try {
            long lock = efaCloudStorage.acquireGlobalLock();
            if (oldDr != null)
                efaCloudStorage.modifyLocalRecord(dataRecords.get(1), lock, false, false, true);
            if (newDr == null)
                efaCloudStorage.modifyLocalRecord(dataRecords.get(0), lock, true, false, false);
        } catch (Exception ignored) {
            Logger.log(Logger.WARNING, Logger.MSG_EFACLOUDSYNCH_ERROR, International.getString(
                    "Konnte globalen Lock nicht bekommen beim Versuch einen Schlüssel zu korrigieren in Tabelle ") +
                    tx.tablename);
        }
        // create the record for the fixed key
        String[] txRecord = new String[dataRecords.get(0).getKeyFields().length];
        int i = 0;
        for (String keyField : dataRecords.get(0).getKeyFields()) {
            txRecord[i] = keyField + ";" +
                    CsvCodec.encodeElement(dataRecords.get(0).getAsText(keyField), CsvCodec.DEFAULT_DELIMITER,
                            CsvCodec.DEFAULT_QUOTATION);
            i++;
        }
        // return fixed record and request next key to be fixed
        txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, "keyfixing", tables_with_key_fixing_allowed[table_fixing_index],
                txRecord);
    }

    /**
     * <p>Step 3: Continue the key fixing process with the next table, if the response to a keyfixing request is
     * 300;ok.</p><p>Increase the table_fixing_index and issue another first "keyfixing request with an empty record.
     * When all tables are done, this issues a synch @all request.</p><p>The synch @all request will filter on the
     * LastModified timestamp. For server-to-client-synchronization (downlad) it will use the last synch timestamp and
     * add a 'clockoffsetBuffer' overlapping period for the case of mismatching clocks between server and client,
     * because the LastModified value is set by the modifier, which may be the server or another client. For
     * client_to_server-Synchronisation (upload) it will always use 0L, so read all records.</p>
     */
    void fixKeysForNextTable() {
        table_fixing_index++;
        if (table_fixing_index < tables_with_key_fixing_allowed.length)
            txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, "keyfixing", tables_with_key_fixing_allowed[table_fixing_index],
                    (String[]) null);
        else
            txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, "synch", "@all", "LastModified;" + LastModifiedLimit, "?;>");
    }

    /**
     * <p>Step 4: Build the list of tables which need data synchronisation.</p><p>Based on a synch @all response this
     * functions builds the list of tables which need data synchronisation. It will start the data synchronization based
     * on the result by calling either "nextTableForDownloadSelect(null)" or nextTableForUploadSynch(null).</p>
     *
     * @param tx the transaction with the server response needed for this step
     */
    void buildSynchTableListAndStartSynch(Transaction tx) {
        String[] results = tx.getResultMessage().split(";");
        String tablename = "";
        tables_to_synchronize.clear();
        StringBuilder tn = new StringBuilder();
        for (String nvp : results) {
            if (nvp.contains("=")) {
                tablename = nvp.split("=")[0];
                String countStr = nvp.split("=")[1];
                if (TableBuilder.isServerClientCommonTable(tablename) &&  // do not synch server only tables
                        (synch_upload ||   // upload synchronization always checks all available tables.
                                !countStr.equalsIgnoreCase("0"))) {
                    tables_to_synchronize.add(tablename);
                    tn.append(tablename).append(", ");
                }
            }
        }
        // start synchronization
        table_synching_index = -1;
        if (synch_upload) {
            logMessage(International.getString("Starte upload Synchronisation für Tabellen."), tn.toString(), null,
                    false);
            nextTableForUploadSynch(null);  // Upload needs two steps: first synch, then insert & update txs
        } else {
            logMessage(International.getString("Starte download Synchronisation für Tabellen."), tn.toString(), null,
                    false);
            nextTableForDownloadSelect(null);  // Download runs in a single step, staring immediately with select
        }
    }

    /**
     * <p>Step 5, download</p><p>If a transaction is provided, read all returned records, decide whether a local record
     * shall be inserted, updated, or deleted and execute the identified modification.</p><p>Then increase the
     * table_synching_index and append a select statement for the indexed table. If the index hits the end of the set of
     * tables to be synchronized, request a state change back to normal.</p>
     *
     * @param tx the transaction with the server response needed for this step. Set null to start the cycle
     */
    void nextTableForDownloadSelect(Transaction tx) {
        if ((tx != null) && (tx.getResultMessage() != null)) {
            // a response message is received. Handle included records
            EfaCloudStorage persistence = Daten.tableBuilder.getPersistence(tx.tablename);
            ArrayList<DataRecord> returnedRecords = persistence.parseCsvTable(tx.getResultMessage());
            for (DataRecord returnedRecord : returnedRecords) {
                // get the local record for comparison
                DataRecord localRecord = null;
                DataKey returnedKey = null;
                try {
                    returnedKey = persistence.constructKey(returnedRecord);
                    if (returnedKey != null)
                        localRecord = persistence.get(returnedKey);
                } catch (EfaException ignored) {
                }
                if (returnedKey != null) {
                    // identify needed action, may be none, in particular if the record had been changed by this client
                    long serverLastModified = returnedRecord.getLastModified();
                    long localLastModified = (localRecord == null) ? 0L : localRecord.getLastModified();
                    String lastModification = persistence.getLastModification(returnedRecord);
                    // a legacy problem. If the database was initialized by the client, it contains copies of
                    // the local data records which have no LastModification entries.
                    if (lastModification == null)
                        lastModification = "updated";
                    boolean isDeleted = lastModification.equalsIgnoreCase("delete");
                    boolean isUpdated = lastModification.equalsIgnoreCase("update");
                    boolean insert = (localRecord == null) && !isDeleted;
                    boolean update = (localRecord != null) && (serverLastModified > localLastModified) && isUpdated;
                    boolean delete = (localRecord != null) && (serverLastModified > localLastModified) && isDeleted;
                    // Run update. This update will use the LastModified and ChangeCount of the record to make
                    // it a true copy of the server side record.
                    if (insert || update || delete) {
                        try {
                            long localLock = persistence.acquireLocalLock(returnedKey);
                            persistence.modifyLocalRecord(returnedRecord, localLock, insert, update, delete);
                            persistence.releaseLocalLock(localLock);
                            logMessage(International
                                    .getString("Lokale Replikation des Datensatzes nach Server-seitigem ") +
                                    lastModification, tx.tablename, returnedRecord.getKey(), false);
                        } catch (EfaException ignored) {
                        }
                    }
                }
            }
        }
        // increase table index and issue select request
        table_synching_index++;
        if (table_synching_index < (tables_to_synchronize.size() - 1)) {
            txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, "select", tables_to_synchronize.get(table_synching_index),
                    "LastModified;" + LastModifiedLimit, "?;>");
        }
        // if there is no more Table to be synchronized, conclude
        else
            txq.requestStateChange(TxRequestQueue.RQ_QUEUE_STOP_SYNCH);
    }

    /**
     * <p>Step 5, upload</p><p>If a transaction is provided, read all returned extended keys, decide whether a server
     * record shall be inserted or updated and add the respective local record to one of two arrays of records to be
     * inserted or updated. Append a transaction to the queue per inserted (all of those first) and updated
     * records.</p><p>Then increase the * table_synching_index and append a synch statement for the indexed table. If
     * the index hits the end of the set of tables to be synchronized, request a state change back to normal.</p>
     *
     * @param tx the transaction with the server response needed for this step. Set null to start the cycle
     */
    void nextTableForUploadSynch(Transaction tx) {
        if (tx != null) {
            // a response message is received. Handle included records
            EfaCloudStorage persistence = Daten.tableBuilder.getPersistence(tx.tablename);
            ArrayList<DataRecord> returnedRecords = persistence.parseCsvTable(tx.getResultMessage());
            serverRecordsReturned.clear();
            localRecordsToInsertAtServer.clear();
            localRecordsToUpdateAtServer.clear();
            for (DataRecord returnedRecord : returnedRecords)
                serverRecordsReturned.put(returnedRecord.getKey(), returnedRecord);
            // compile the list of actionable records
            try {
                DataKeyIterator it = persistence.getStaticIterator();
                DataKey toCheck = it.getFirst();
                while (toCheck != null) {
                    DataRecord localRecord = persistence.get(toCheck);
                    DataRecord serverRecord = serverRecordsReturned.get(toCheck);
                    if (serverRecord == null)
                        localRecordsToInsertAtServer.add(localRecord);
                    else if (localRecord.getLastModified() > serverRecord.getLastModified())
                        localRecordsToUpdateAtServer.add(localRecord);
                    toCheck = it.getNext();
                }
            } catch (EfaException e) {
                Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR,
                        International.getString("Konnte nicht über die Datensätze iterieren bei Tabelle ") +
                                tx.tablename);
            }
            // append all relevant transactions to the queue. This may be quite a lot and take a while to
            // be worked through.
            for (DataRecord localRecordToInsertAtServer : localRecordsToInsertAtServer) {
                persistence.modifyServerRecord(localRecordToInsertAtServer, true, false, false, true);
                logMessage(International.getString("Füge Datensatz auf Server ein für Tabelle "), tx.tablename,
                        localRecordToInsertAtServer.getKey(), false);
            }
            for (DataRecord localRecordToUpdateAtServer : localRecordsToUpdateAtServer) {
                persistence.modifyServerRecord(localRecordToUpdateAtServer, false, true, false, true);
                logMessage(International.getString("Aktualisiere Datensatz auf Server für Tabelle "), tx.tablename,
                        localRecordToUpdateAtServer.getKey(), false);
            }
        }
        table_synching_index++;
        if (table_synching_index < tables_to_synchronize.size()) {
            txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, "select", tables_to_synchronize.get(table_synching_index),
                    "LastModified;" + LastModifiedLimit, "?;>");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = format.format(new Date());
            logMessage(International.getString("Hole Datensätze vom Server mit Modifikation nach ") + dateString,
                    tables_to_synchronize.get(table_synching_index), null, false);
        } else {
            // This TX_QUEUE_STOP_SYNCH request will stay a while in the loop, because it will only be handled after
            // all transactions of the synch queue have been processed.
            txq.requestStateChange(TxRequestQueue.RQ_QUEUE_STOP_SYNCH);
            logMessage(International.getString(
                    "Transaktionen für Synchronisation vollständig angestoßen. Warte auf " + "Fertigstellung."), "@all",
                    null, false);
        }
    }

}
