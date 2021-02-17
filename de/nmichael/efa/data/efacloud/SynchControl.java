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
import de.nmichael.efa.data.BoatStatusRecord;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.EfaCloudStorage;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static de.nmichael.efa.data.efacloud.TxRequestQueue.*;

class SynchControl {

    // The names of the tables which allow the key to be modified upon server side insert
    static final String[] tables_with_key_fixing_allowed = TableBuilder.fixid_allowed.split(" ");
    static final long clockoffsetBuffer = 600000L; // max number of millis which client clock may be offset

    long timeOfLastSynch;
    long LastModifiedLimit;
    boolean synch_upload = false;

    int table_fixing_index = -1;
    ArrayList<String> tables_to_synchronize = new ArrayList<String>();
    int table_synching_index = -1;

    // when running an upload synchronization the server is first asked for all data keys and last modified
    // timestamps together with the last modification. They are put into records, sorted and qualified
    private final HashMap<DataKey, DataRecord> serverRecordsReturned = new HashMap<DataKey, DataRecord>();
    private final ArrayList<DataRecord> localRecordsToInsertAtServer = new ArrayList<DataRecord>();
    private final ArrayList<DataRecord> localRecordsToUpdateAtServer = new ArrayList<DataRecord>();

    private final TxRequestQueue txq;

    /**
     * Constructor. Initializes the queue reference set the time of last synch to 0L, forcing a full resynch on every
     * program restart.
     *
     * @param txq the queue reference
     */
    SynchControl(TxRequestQueue txq) {
        this.txq = txq;
        timeOfLastSynch = 0L;
    }

    /**
     * Write a log message to the synch log.
     *
     * @param logMessage     the message to be written
     * @param tablename      the name of the affected table
     * @param dataKey        the datakey of the affected record
     * @param logStateChange set true to append the log to the stae change log rather than the synch log
     */
    void logSynchMessage(String logMessage, String tablename, DataKey dataKey, boolean logStateChange) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dataKeyStr = (dataKey == null) ? "" : " - " + dataKey.toString();
        String info = (logStateChange) ? "STATECHANGE " : "SYNCH ";
        String dateString = format.format(new Date()) + " [" + tablename + dataKeyStr + "]: " + info + logMessage;
        String path = TxRequestQueue.logFilePaths.get("synch and activities");
        // truncate log files,
        File f = new File(path);
        if ((f.length() > 200000) && (f.renameTo(new File(path + ".previous"))))
            TextResource.writeContents(path, dateString, false);
        else
            TextResource.writeContents(path, dateString, true);
    }

    /**
     * <p>Step 1: Start the synchronization process</p><p>Start the synchronization process by appending the first
     * keyfixing request to the synching queue.</p>
     *
     * @param synch_request stet true to run an upload synchronization, false to run a download synchronization
     */
    void startSynchProcess(int synch_request) {
        // in case of manually triggered server table reset no further steps needed needed.
        if (synch_request == TxRequestQueue.RQ_QUEUE_START_SYNCH_DELETE) {
            logSynchMessage(International.getString("Delete and rebuild of server tables starting"), "@all", null,
                    true);
            Daten.tableBuilder.initAllServerTables();
            return;
        }
        table_fixing_index = 0;
        synch_upload = synch_request == TxRequestQueue.RQ_QUEUE_START_SYNCH_UPLOAD;
        String synchMessage = (synch_upload) ? International
                .getString("Synchronisation client to server (upload) starting") : International
                .getString("Synchronisation server to client (download) starting");
        logSynchMessage(synchMessage, "@all", null, true);
        // The manually triggered synchronization always takes the full set into account
        LastModifiedLimit = (synch_upload || (timeOfLastSynch < clockoffsetBuffer)) ? 0L :
                timeOfLastSynch - clockoffsetBuffer;
        timeOfLastSynch = System.currentTimeMillis();
        // request first key to be fixed. The record is empty.
        txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.KEYFIXING,
                tables_with_key_fixing_allowed[table_fixing_index], (String[]) null);
    }

    /**
     * In case an EntryId of a logbook entry is corrected, ensure that also the boat status is updated. Only by that
     * update it is ensured the trip can be closed later.
     *
     * @param currentEntryNo EntryId of the Logbook entry in the boat status
     * @param fixedEntryNo   new EntryId for this boat status
     */
    private void adjustBoatStatus(int currentEntryNo, int fixedEntryNo, long globalLock) {
        EfaCloudStorage boatstatus = Daten.tableBuilder.getPersistence("efa2boatstatus");
        DataKeyIterator it;
        try {
            it = boatstatus.getStaticIterator();
            DataKey boatstatuskey = it.getFirst();
            while (boatstatuskey != null) {
                BoatStatusRecord bsr = (BoatStatusRecord) boatstatus.get(boatstatuskey);
                if (bsr.getEntryNo() != null) {
                    int entryNo = bsr.getEntryNo().intValue();
                    if (entryNo == currentEntryNo) {
                        bsr.setEntryNo(new DataTypeIntString("" + fixedEntryNo));
                        boatstatus.update(bsr, globalLock);
                        logSynchMessage(International.getString("Korrigiere EntryNo in BoatStatus"), "efa2boatstatus",
                                boatstatuskey, false);
                    }
                }
                boatstatuskey = it.getNext();
            }
        } catch (EfaException ignored) {
            txq.logApiMessage(International.getString(
                    "Aktualisierung fehlgeschlagen beim Versuch die EntryNo zu korrigieren in efa2boatstatus."), 1);
        }
    }

    /**
     * <p>Step 2: Change a data key, based on the server response on a key fixing request.</p><p>This handles the
     * response on a keyfixing request, if a mismatched key was found (result code 303). Will fix the key locally and
     * create a new keyfixing request to inform the server on the execution. This function must only be called, if the
     * transaction result message contains the record too be fixed.</p><p>This function may be called by an insert
     * action, then ir does</p>
     *
     * @param tx        the transaction with the server response needed for fixing
     * @param tablename set to a table name, if just one fixing was required, e.g. after an insert. Set to "" if in
     *                  synchronizing to trigger the next table, when there is no more key to be fixed.
     */
    void fixOneKeyForTable(Transaction tx, String tablename) {
        EfaCloudStorage efaCloudStorage = Daten.tableBuilder.getPersistence(tx.tablename);
        ArrayList<DataRecord> dataRecords = efaCloudStorage.parseCsvTable(tx.getResultMessage());
        DataRecord oldDr = null;
        DataRecord newDr = null;
        String[] txRecordForFixedEntry = null;
        boolean correctionSucceeded = false;
        try {
            oldDr = efaCloudStorage.get(dataRecords.get(1).getKey());
            newDr = efaCloudStorage.get(dataRecords.get(0).getKey());
        } catch (EfaException ignored) {
        }
        if (oldDr == null)
            logSynchMessage(International.getString("Schlüsselkorrekturfehler. Alter Schlüssel nicht vorhanden: ") +
                    dataRecords.get(1).getKey().toString(), tx.tablename, dataRecords.get(0).getKey(), false);
        if (newDr != null)
            logSynchMessage(International.getString("Schlüsselkorrekturfehler. Neuer Schlüssel schon belegt: ") +
                    dataRecords.get(1).getKey().toString(), tx.tablename, dataRecords.get(1).getKey(), false);
        if ((oldDr != null) && (newDr == null)) {
            logSynchMessage(International.getString("Korrigiere Schlüssel von bisher ") +
                    dataRecords.get(1).getKey().toString(), tx.tablename, dataRecords.get(0).getKey(), false);
            long globalLock = 0L;
            try {
                globalLock = efaCloudStorage.acquireGlobalLock();
                // the record has a link to the boat status, so it will not be allowed to be deleted. Disable
                // temporarily the check.
                efaCloudStorage.setPreModifyRecordCallbackEnabled(false);
                efaCloudStorage.modifyLocalRecord(dataRecords.get(0), globalLock, true, false, false);
                efaCloudStorage.releaseGlobalLock(globalLock);
                logSynchMessage(International.getString("Schlüsselkorektur: Füge richtigen Datensatz hinzu: ") +
                        dataRecords.get(1).getKey().toString(), tx.tablename, dataRecords.get(0).getKey(), false);
                if (tx.tablename.equalsIgnoreCase("efa2logbook")) {
                    int oldEntryId = ((LogbookRecord) dataRecords.get(1)).getEntryId().intValue();
                    int newEntryId = ((LogbookRecord) dataRecords.get(0)).getEntryId().intValue();
                    EfaCloudStorage boatstatus = Daten.tableBuilder.getPersistence("efa2boatstatus");
                    globalLock = boatstatus.acquireGlobalLock();
                    adjustBoatStatus(oldEntryId, newEntryId, globalLock);
                    boatstatus.releaseGlobalLock(globalLock);
                    logSynchMessage(International.getString("Schlüsselkorektur: Korrigiere Bootsstatus: ") + newEntryId,
                            "efa2boatstatus", null, false);
                }
                globalLock = efaCloudStorage.acquireGlobalLock();
                efaCloudStorage.modifyLocalRecord(dataRecords.get(1), globalLock, false, false, true);
                logSynchMessage(International.getString("Schlüsselkorektur: Lösche falschen Datensatz: ") +
                        dataRecords.get(1).getKey().toString(), tx.tablename, dataRecords.get(0).getKey(), false);
                // create the record for the fixed key
                correctionSucceeded = true;
                txRecordForFixedEntry = new String[dataRecords.get(0).getKeyFields().length];
                int i = 0;
                for (String keyField : dataRecords.get(0).getKeyFields()) {
                    txRecordForFixedEntry[i] = keyField + ";" +
                            CsvCodec.encodeElement(dataRecords.get(0).getAsText(keyField), CsvCodec.DEFAULT_DELIMITER,
                                    CsvCodec.DEFAULT_QUOTATION);
                    i++;
                }
            } catch (Exception e) {
                txq.logApiMessage(International
                        .getMessage("Ausnahmefehler beim Versuch einen Schlüssel zu korrigieren in {Tabelle}: {Fehler}.",
                                tx.tablename, e.getMessage()), 1);
            } finally {
                efaCloudStorage.releaseGlobalLock(globalLock);
                efaCloudStorage.setPreModifyRecordCallbackEnabled(true);
            }
        } else {
            // if in synchronizing: trigger the next table, when there is no more key to be fixed.
            if (tablename.isEmpty() && (table_fixing_index < (tables_with_key_fixing_allowed.length - 1)))
                table_fixing_index++; // move to next table to avoid endless loop
        }
        // continue anyway, if in synchronizing
        if (tablename.isEmpty())
            txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.KEYFIXING,
                    tables_with_key_fixing_allowed[table_fixing_index], txRecordForFixedEntry);
        // confirm the fixing, but only if successfully completed. If not, this will enter an endless loop between
        // client and server of retrying the keyfixing.
        else if (txRecordForFixedEntry != null)
            txq.appendTransaction(TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.KEYFIXING, tablename, txRecordForFixedEntry);
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
            txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.KEYFIXING,
                    tables_with_key_fixing_allowed[table_fixing_index], (String[]) null);
        else
            txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.SYNCH, "@all",
                    "LastModified;" + LastModifiedLimit, "?;>");
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
        tables_to_synchronize.clear();
        StringBuilder tn = new StringBuilder();
        for (String nvp : results) {
            if (nvp.contains("=")) {
                String tablename = nvp.split("=")[0];
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
            logSynchMessage(International.getString("Starte upload Synchronisation für Tabellen."), tn.toString(), null,
                    false);
            nextTableForUploadSynch(null);  // Upload needs two steps: first synch, then insert & update txs
        } else {
            logSynchMessage(International.getString("Starte download Synchronisation für Tabellen."), tn.toString(),
                    null, false);
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
            EfaCloudStorage efaCloudStorage = Daten.tableBuilder.getPersistence(tx.tablename);
            // Read all records and all last modifications.
            ArrayList<DataRecord> returnedRecords = efaCloudStorage.parseCsvTable(tx.getResultMessage());
            for (DataRecord returnedRecord : returnedRecords) {
                // get the local record for comparison
                DataRecord localRecord = null;
                DataKey returnedKey = null;
                try {
                    boolean recordHasCompleteKey = efaCloudStorage.hasCompleteKey(returnedRecord);
                    if (recordHasCompleteKey) {
                        returnedKey = efaCloudStorage.constructKey(returnedRecord);
                        if (returnedKey != null)
                            localRecord = efaCloudStorage.get(returnedKey);
                    }
                } catch (EfaException ignored) {
                }
                if (returnedKey != null) {

                    // efaLogbook check. The server has only one logbook, the client may use another
                    // one currently. If server and local record point to  a different year, deactivate efaCloud
                    if (tx.tablename.equalsIgnoreCase("efa2logbook") && (localRecord != null)) {
                        String clientyear = ((LogbookRecord) localRecord).getAsString("Date");
                        String serveryear = ((LogbookRecord) returnedRecord).getAsString("Date");
                        if ((clientyear != null) && (serveryear != null) &&
                                !clientyear.substring(6, 10).equalsIgnoreCase(serveryear.substring(6, 10))) {
                            String wrongYear = International.getMessage(
                                    "Das Fahrtenbuch auf dem Server gehört zu einem anderen Jahr ({serverYear}), als " +
                                            "das Fahrtenbuch auf dem Client ({clientYear}). Deaktiviere efaCloud um " +
                                            "Synchronisationsfehlern vorzubeugen.", serveryear.substring(6, 10),
                                    clientyear.substring(6, 10));
                            Logger.log(Logger.WARNING, Logger.MSG_EFACLOUDSYNCH_ERROR, wrongYear);
                            txq.logApiMessage(wrongYear, 1);
                            Dialog.error(wrongYear);
                            txq.registerStateChangeRequest(RQ_QUEUE_PAUSE);
                            txq.registerStateChangeRequest(RQ_QUEUE_DEACTIVATE);
                        }
                    }

                    // identify which record is to be used.
                    long serverLastModified = returnedRecord.getLastModified();
                    long localLastModified = (localRecord == null) ? 0L : localRecord.getLastModified();
                    boolean serverMoreRecent = (serverLastModified > localLastModified);

                    // Special case autoincrement counter fields: always use the larger value, even if it is older.
                    if (tx.tablename.equalsIgnoreCase("efa2aoutoincrement")) {
                        long lmaxReturned = Long.parseLong(returnedRecord.getAsString("LongValue"));
                        long lmaxLocal = Long.parseLong(returnedRecord.getAsString("LongValue"));
                        long imaxReturned = Long.parseLong(returnedRecord.getAsString("IntValue"));
                        long imaxLocal = Long.parseLong(returnedRecord.getAsString("IntValue"));
                        serverMoreRecent = (lmaxReturned > 0) ? (lmaxReturned > lmaxLocal) : (imaxReturned > imaxLocal);
                    }

                    // identify what to do, may be nothing, in particular if the record had been changed by this client
                    String lastModification = efaCloudStorage.getLastModification(returnedRecord);
                    // a legacy problem. If the database was initialized by the client, it contains copies of
                    // the local data records which have no LastModification entries.
                    if (lastModification == null)
                        lastModification = "updated";
                    boolean isDeleted = lastModification.equalsIgnoreCase("delete");
                    boolean isUpdated = lastModification.equalsIgnoreCase("update");
                    boolean insert = (localRecord == null) && !isDeleted;
                    boolean update = (localRecord != null) && serverMoreRecent && isUpdated;
                    boolean delete = (localRecord != null) && serverMoreRecent && isDeleted;

                    // Check whether record to update matsches at least partially the new version.
                    if (update && !preUpdateRecordsCompare(localRecord, returnedRecord))
                        logSynchMessage(International.getString(
                                "Update-Konflikt bei Datensatz in der Download-Synchronisation, den zu " +
                                        "aktualisierenden " + "Datensatz bitte manuell bereinigen "), tx.tablename,
                                localRecord.getKey(), false);

                        // Run update. This update will use the LastModified and ChangeCount of the record to make
                        // it a true copy of the server side record.
                    else if (insert || update || delete) {
                        long globalLock = 0;
                        try {
                            // any add modification requires a global lock.
                            globalLock = efaCloudStorage.acquireGlobalLock();
                            efaCloudStorage.modifyLocalRecord(returnedRecord, globalLock, insert, update, delete);
                            efaCloudStorage.releaseGlobalLock(globalLock);
                            logSynchMessage(International.getMessage(
                                    "Lokale Replikation des Datensatzes nach {modification} auf dem Server.",
                                    lastModification), tx.tablename, returnedRecord.getKey(), false);
                        } catch (EfaException e) {
                            txq.logApiMessage(International
                                    .getString("Ausnahmefehler bei der lokalen Modifikation eines Datensatzes ") +
                                    e.getMessage() + "\n" + e.getStackTraceAsString(), 1);
                        } finally {
                            efaCloudStorage.releaseGlobalLock(globalLock);
                        }
                    }
                }
            }
        }
        // increase table index and issue select request
        table_synching_index++;
        if (table_synching_index < tables_to_synchronize.size()) {
            txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.SELECT,
                    tables_to_synchronize.get(table_synching_index), "LastModified;" + LastModifiedLimit, "?;>");
        }
        // if there is no more Table to be synchronized, conclude
        else
            txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_STOP_SYNCH);
    }

    /**
     * Compares two records whether it is probable that one is the update of the other. If three or more data fields
     * differ, it is not beleived that one is an update of the other and a data update conflict is created instead of
     * updating.
     *
     * @param dr1 DataRecord one to compare
     * @param dr2 DataRecord two to compare
     * @return true, if the records are of the same type and differ by less than 3 data fields (except LastModified and
     * LastModification)
     */
    private boolean preUpdateRecordsCompare(DataRecord dr1, DataRecord dr2) {
        if (dr1.getClass() != dr2.getClass())
            return false;
        int diff = 0;
        for (String field : dr1.getFields()) {
            if (dr1.getAsString(field) == null)
                diff = (dr2.getAsString(field) == null) ? 0 : 1;
            else if (dr2.getAsString(field) == null)
                diff++;
                // Use String comparison as the compareTo() implementation throws to many different exceptions.
            else if (!dr1.getAsString(field).equalsIgnoreCase(dr2.getAsString(field)))
                diff++;
        }
        return diff < 3;
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
            if (persistence == null)
                txq.logApiMessage(International.getString("Konnte folgende Tabelle nicht für das Hochladen finden: ") +
                        tx.tablename, 1);
            else {
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
                        else if (localRecord.getLastModified() > serverRecord.getLastModified()) {
                            if (preUpdateRecordsCompare(localRecord, serverRecord))
                                localRecordsToUpdateAtServer.add(localRecord);
                            else {
                                logSynchMessage(International.getString(
                                        "Update-Konflikt bei Datensatz in der Upload-Synchronisation, den zu " +
                                                "aktualisierenden Datensatz bitte manuell bereinigen "), tx.tablename,
                                        toCheck, false);
                            }
                        }
                        toCheck = it.getNext();
                    }
                } catch (EfaException e) {
                    txq.logApiMessage(International
                            .getMessage("Konnte nicht über die Datensätze iterieren bei Tabelle ", tx.tablename), 1);
                }
                // append all relevant transactions to the queue. This may be quite a lot and take a while to
                // be worked through.
                for (DataRecord localRecordToInsertAtServer : localRecordsToInsertAtServer) {
                    persistence.modifyServerRecord(localRecordToInsertAtServer, true, false, false, true);
                    logSynchMessage(International.getString("Füge Datensatz auf Server ein für Tabelle "), tx.tablename,
                            localRecordToInsertAtServer.getKey(), false);
                }
                for (DataRecord localRecordToUpdateAtServer : localRecordsToUpdateAtServer) {
                    persistence.modifyServerRecord(localRecordToUpdateAtServer, false, true, false, true);
                    logSynchMessage(International.getString("Aktualisiere Datensatz auf Server für Tabelle "),
                            tx.tablename, localRecordToUpdateAtServer.getKey(), false);
                }
            }
        }
        table_synching_index++;
        if (table_synching_index < tables_to_synchronize.size()) {
            txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.SELECT,
                    tables_to_synchronize.get(table_synching_index), "LastModified;" + LastModifiedLimit, "?;>");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = format.format(new Date(LastModifiedLimit));
            logSynchMessage(International.getString("Hole Datensätze vom Server mit Modifikation nach ") + dateString,
                    tables_to_synchronize.get(table_synching_index), null, false);
        } else {
            // This TX_QUEUE_STOP_SYNCH request will stay a while in the loop, because it will only be handled after
            // all transactions of the synch queue have been processed.
            txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_STOP_SYNCH);
            logSynchMessage(International.getString(
                    "Transaktionen für Synchronisation vollständig angestoßen. Warte auf " + "Fertigstellung."), "@all",
                    null, false);
        }
    }

}
