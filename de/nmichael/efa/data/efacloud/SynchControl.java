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
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.EfaCloudStorage;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import static de.nmichael.efa.data.LogbookRecord.*;
import static de.nmichael.efa.data.efacloud.TxRequestQueue.*;

class SynchControl {

    // The names of the tables which allow the key to be modified upon server side insert
    static final String[] tables_with_key_fixing_allowed = TableBuilder.fixid_allowed.split(" ");
    static final long clockoffsetBuffer = 600000L; // max number of millis which client clock may be offset, 10 mins
    static final long synch_upload_look_back_ms = 15 * 24 * 3600000L; // period in past to check for upload
    static final long surely_newer_after_ms = 60000L; // one-minute time difference accepted for timestamps server <-> client
    
    private static final int SYNCHERRORS_LOG_MAX_SIZE = 200000; //200 kb. 
    private static final String FILENAME_PREVIOUS_SUFFIX = ".previous.log";

    long lastSynchStartedMillis;
    long LastModifiedLimit;
    boolean synch_upload = false;
    boolean synch_upload_all = false;
    boolean synch_download_all = false;
    // efaCloudRolleBths is true if the efacloud user role is that of a boathouse. If true, this will enforce
    // pre-modification checks during download synchronization
    boolean efaCloudRolleBths = true;
    // isBoathouseApp is true, if this s run as efaBoathouse. This will enforce pre-modification checks during download
    // synchronization
    boolean isBoathouseApp = true;

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
        lastSynchStartedMillis = 0L;
    }

    /**
     * Write a log message to the synch log.
     *
     * @param logMessage     the message to be written
     * @param tablename      the name of the affected table
     * @param dataKey        the datakey of the affected record
     * @param logStateChange set true to start entry with STATECHANGE rather than SYNCH
     * @param isError        set true to log a synchronization error in the respective file.
     */
    private void logSynchMessage(String logMessage, String tablename, DataKey dataKey, boolean logStateChange, boolean isError) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dataKeyStr = (dataKey == null) ? "" : " - " + dataKey.toString();
        String info = (logStateChange) ? "STATECHANGE " : "SYNCH ";
        String dateString = format.format(new Date()) + " INFO state, [" + tablename + dataKeyStr + "]: " + info + logMessage;
        String path = (isError) ? synchErrorFilePath : TxRequestQueue.logFilePath;
        // truncate log files,
        File synchErrorsFile = new File(path);
        
        Boolean appendLine=true;
        
        //synchErrors.log rotation: if >5 Mb, delete old synchErrors.previous.log file and rename synchErrors.log to synchErrors.log.previous.log
        if (synchErrorsFile.length() > SYNCHERRORS_LOG_MAX_SIZE) {
        	File oldPreviousLogfile=new File(path + FILENAME_PREVIOUS_SUFFIX);
        	
        	// rotate existing efacloud.log to efacloud.log.previous and delete the existing file if necessary.
        	if ((oldPreviousLogfile.exists() && oldPreviousLogfile.delete()) 
        			|| (!oldPreviousLogfile.exists())) {
        		if (synchErrorsFile.renameTo(new File(path + FILENAME_PREVIOUS_SUFFIX))) {
        			appendLine=false;
        		}
        	}
        }
        
        TextResource.writeContents(path, dateString, appendLine);
    }

    /**
     * Write a log message to the synch log.
     *
     * @param logMessage     the message to be written
     * @param tablename      the name of the affected table
     * @param dataKey        the datakey of the affected record
     * @param logStateChange set true to start entry with STATECHANGE rather than SYNCH
     */
    void logSynchMessage(String logMessage, String tablename, DataKey dataKey, boolean logStateChange) {
        logSynchMessage(logMessage, tablename, dataKey, logStateChange, false);
    }

    /**
     * <p>Step 1: Start the synchronization process</p><p>Start the synchronization process by appending the first
     * keyfixing request to the synching queue.</p>
     *
     * @param synch_request stet true to run an upload synchronization, false to run a download synchronization
     */
    void startSynchProcess(int synch_request) {
        table_fixing_index = 0;
        synch_upload_all = synch_request == TxRequestQueue.RQ_QUEUE_START_SYNCH_UPLOAD_ALL;
        synch_upload = (synch_request == TxRequestQueue.RQ_QUEUE_START_SYNCH_UPLOAD) || synch_upload_all;
        synch_download_all = !synch_upload && (lastSynchStartedMillis < clockoffsetBuffer);
        String synchMessage = (synch_upload) ? International
                .getString("Synchronisation client to server (upload) starting") : International
                .getString("Synchronisation server to client (download) starting");
        logSynchMessage(synchMessage, "@all", null, true);
        // The manually triggered synchronization always takes the full set into account
        LastModifiedLimit = (synch_upload_all || synch_download_all) ? 0L : (synch_upload) ?
                System.currentTimeMillis() - synch_upload_look_back_ms : lastSynchStartedMillis - clockoffsetBuffer;
        lastSynchStartedMillis = System.currentTimeMillis();
        // Key fixing on synchronisation has become obsolete in August 2024.
        // txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.KEYFIXING,
        //        tables_with_key_fixing_allowed[table_fixing_index], (String[]) null);
        // start synchronisation
        txq.appendTransaction(TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.SYNCH, "@all",
                "LastModified;" + LastModifiedLimit, "?;>");

    }

    /**
     * In case an EntryId of a logbook entry is corrected, ensure that also the boat status is updated. Only by that
     * update it is ensured the trip can be closed later.
     *
     * @param boatId The UUID of the boat in the boat status
     * @param fixedEntryNo   new EntryId for this boat status
     */
    protected void adjustBoatStatus(UUID boatId, int fixedEntryNo) {
        EfaCloudStorage boatstatus = Daten.tableBuilder.getPersistence("efa2boatstatus");
        DataKeyIterator it;
        try {
            it = boatstatus.getStaticIterator();
            DataKey boatstatuskey = it.getFirst();
            while (boatstatuskey != null) {
                BoatStatusRecord bsr = (BoatStatusRecord) boatstatus.get(boatstatuskey);
                if (bsr.getBoatId() != null) {
                    if (bsr.getBoatId().compareTo(boatId) == 0) {
                        bsr.setEntryNo(new DataTypeIntString("" + fixedEntryNo));
                        long globalLock = boatstatus.acquireGlobalLock();
                        boatstatus.update(bsr, globalLock);
                        boatstatus.releaseGlobalLock(globalLock);
                        logSynchMessage(International.getString("Korrigiere EntryNo in BoatStatus"), "efa2boatstatus",
                                boatstatuskey, false);
                    }
                }
                boatstatuskey = it.getNext();
            }
        } catch (EfaException ignored) {
            txq.logApiMessage(International.getString(
                    "Aktualisierung fehlgeschlagen beim Versuch die EntryNo zu korrigieren in BoatStatus."), 1);
        }
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
            logSynchMessage(International.getString("Starte Upload Synchronisation für Tabellen."), tn.toString(), null,
                    false);
            nextTableForUploadSynch(null);  // Upload needs two steps: first synch, then insert & update txs
        } else {
            logSynchMessage(International.getString("Starte Download Synchronisation für Tabellen."), tn.toString(),
                    null, false);
            nextTableForDownloadSelect(null);  // Download runs in a single step, staring immediately with select
        }
    }

    /**
     * Compare two autoincrement records and return true, if the server number value is larger than the local one.
     * @param local the local autoincrement value
     * @param server the server autoincrement value
     * @return true, if the server number value is larger than the local one. False also in case of one value being null,
     * or in case of mismatching sequence field.
     */
    private boolean updateAutoincrement (DataRecord local, DataRecord server) {
        if ((local == null) || (server == null))
            return false;
        if (!local.getAsString(AutoIncrementRecord.SEQUENCE).equalsIgnoreCase(server.getAsString(AutoIncrementRecord.SEQUENCE)))
            return false;
        if ((local.getAsString(AutoIncrementRecord.LONGVALUE) != null)
                && (server.getAsString(AutoIncrementRecord.LONGVALUE) != null)
                && (Long.parseLong(local.getAsString(AutoIncrementRecord.LONGVALUE)) < Long.parseLong(server.getAsString(AutoIncrementRecord.LONGVALUE))))
            return true;
        if ((local.getAsString(AutoIncrementRecord.INTVALUE) != null)
                && (server.getAsString(AutoIncrementRecord.INTVALUE) != null)
                && (Integer.parseInt(local.getAsString(AutoIncrementRecord.INTVALUE)) < Integer.parseInt(server.getAsString(AutoIncrementRecord.INTVALUE))))
            return true;
        if ((local.getAsString(Ecrid.ECRID_FIELDNAME) != null)
                && (server.getAsString(Ecrid.ECRID_FIELDNAME) != null)
                && !local.getAsString(Ecrid.ECRID_FIELDNAME).contentEquals(server.getAsString(Ecrid.ECRID_FIELDNAME)))
            return true;
        return false;
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
            if (efaCloudStorage != null) {

                // if it is a full synch collect all local data Keys to find unmatched local records
                HashMap<String, DataRecord> unmatchedLocalKeys = new HashMap<String, DataRecord>();
                if (synch_download_all) {
                    try {
                        DataKeyIterator localTableIterator = efaCloudStorage.getStaticIterator();
                        DataKey dataKey = localTableIterator.getFirst();
                        DataRecord cachedRecord = efaCloudStorage.get(dataKey);
                        while (dataKey != null) {
                            String ecrid = cachedRecord.getAsString(ECRID);
                            String cacheKey = ((ecrid == null) || ecrid.isEmpty()) ? dataKey.encodeAsString() : ecrid;
                            unmatchedLocalKeys.put(cacheKey, cachedRecord);
                            dataKey = localTableIterator.getNext();
                            cachedRecord = efaCloudStorage.get(dataKey);
                        }
                    } catch (EfaException ignored) {
                        // if combined upload fails by whatever reason, ignore it.
                    }
                }

                ArrayList<DataRecord> returnedRecords = efaCloudStorage.parseCsvTable(tx.getResultMessage());
                for (DataRecord returnedRecord : returnedRecords) {
                    // get the local record for comparison
                    DataRecord localRecord = null;
                    DataKey returnedKey = null;
                    DataKey localKey = null;
                    String returnedEcrid = null;
                    String localEcrid = null;
                    String cacheKey = "";
                    try {
                        // retrieve the local record matching the server side record. Use the ecrid, if available and
                        // registered, else use the regular data key
                        returnedEcrid = returnedRecord.getAsString(ECRID);
                        boolean returnedHasCompleteKey = efaCloudStorage.hasCompleteKey(returnedRecord);
                        returnedKey = efaCloudStorage.constructKey(returnedRecord);
                        if ((returnedEcrid != null) && (Ecrid.iEcrids.get(returnedEcrid) != null))
                            // use ecrid to get the local record
                            localRecord = Ecrid.iEcrids.get(returnedEcrid);
                        else if (returnedHasCompleteKey)
                            // use the returned efa key as fallback, if the ecrid was not identified in the all-tables-ecrid index
                            localRecord = efaCloudStorage.get(returnedKey);
                        if (localRecord != null) {
                            localKey = efaCloudStorage.constructKey(localRecord);
                            localEcrid = localRecord.getAsString(ECRID);
                        }
                    } catch (EfaException ignored) {
                    }
                    if ((returnedKey != null) || (returnedEcrid != null)) {

                        // remove the reference to this record from the cached list
                        if (synch_download_all && (localRecord != null)) {
                            if ((localEcrid != null) && localEcrid.equalsIgnoreCase(returnedEcrid))
                                unmatchedLocalKeys.put(localEcrid, null);
                            else if ((localKey != null) && localKey.compareTo(returnedKey) == 0)
                                unmatchedLocalKeys.put(localKey.encodeAsString(), null);
                        }

                        // identify which record is to be used.
                        long serverLastModified = returnedRecord.getLastModified();
                        long localLastModified = (localRecord == null) ? 0L : localRecord.getLastModified();
                        boolean serverRecordHasEcrid = (returnedRecord.getAsString(Ecrid.ECRID_FIELDNAME) != null);
                        boolean localRecordHasNoEcrid = (localRecord != null) &&
                                (localRecord.getAsString(Ecrid.ECRID_FIELDNAME) == null);
                        boolean addEcridToLocal = serverRecordHasEcrid && localRecordHasNoEcrid;
                        boolean serverMoreRecent = (serverLastModified > (localLastModified + surely_newer_after_ms));

                        // identify whether a data key has changed. Can only happen, if ecrid is given and valid.
                        boolean keyHasChanged = (localKey != null) && (localKey.compareTo(returnedKey) != 0);

                        // identify what to do, may be nothing, in particular if the record had been changed by this client
                        String lastModification = efaCloudStorage.getLastModification(returnedRecord);
                        // a legacy problem. If the database was initialized by the client, it contains copies of
                        // the local data records which have no LastModification entries.
                        // TODO: if statement can be removed once versions 2.3.0_xx become obsolete
                        if (lastModification == null)
                            lastModification = "updated";
                        boolean ecridMismatch = (localRecord == null) ||
                                (serverRecordHasEcrid && (localRecord.getAsString(Ecrid.ECRID_FIELDNAME) != null) &&
                                        !localRecord.getAsString(Ecrid.ECRID_FIELDNAME).equals(returnedRecord.getAsString(Ecrid.ECRID_FIELDNAME)));
                        boolean updateAutoincrement = (tx.tablename.equalsIgnoreCase(AutoIncrement.DATATYPE)
                                && updateAutoincrement(localRecord, returnedRecord));

                        boolean isDeleted = lastModification.equalsIgnoreCase("delete");
                        boolean isUpdated = lastModification.equalsIgnoreCase("update");
                        boolean insert = (localRecord == null) && !isDeleted;
                        boolean update = (localRecord != null) && ((serverMoreRecent && isUpdated) || addEcridToLocal
                                || keyHasChanged || updateAutoincrement);
                        boolean delete = (localRecord != null) && serverMoreRecent && isDeleted;
                        boolean localMoreRecent = (serverLastModified < (localLastModified - surely_newer_after_ms));
                        boolean localRecentChange = ((System.currentTimeMillis() - localLastModified) <
                                synch_upload_look_back_ms);

                        // Run update. This update will use the LastModified and ChangeCount of the record to make
                        // it a true copy of the server side record.
                        if (insert || update || delete) {
                            try {
                                // any add modification requires a global lock.
                                boolean modified = efaCloudStorage.modifyLocalRecord(returnedRecord, insert, update, delete);
                                if (modified)
                                    logSynchMessage(International.getMessage(
                                        "Lokale Replikation des Datensatzes nach {modification} auf dem Server.",
                                        lastModification), tx.tablename, returnedRecord.getKey(), false);
                            } catch (EfaException e) {
                                String errorMessage = International.getMessage(
                                        "Ausnahmefehler bei der lokalen Modifikation eines Datensatzes in {Tabelle} ",
                                        tx.tablename) + " // " + e.getMessage() + " // " + returnedRecord.toString() +
                                            " // " + e.getStackTraceAsString();
                                txq.logApiMessage(errorMessage, 1);
                                logSynchMessage(errorMessage, tx.tablename, returnedRecord.getKey(), false, true);
                            }
                        }
                        // local copy is more recent, upload it, if a full download was requested
                        else if (synch_download_all && localMoreRecent && localRecentChange && (localRecord != null)) {
                            // the server record will usually be updated. But if the efa keys are identical and the ecrids are not
                            // it shall rather be inserted.
                            efaCloudStorage.modifyServerRecord(localRecord, ecridMismatch, !ecridMismatch, false, true);
                            logSynchMessage(International.getString("Aktualisiere Datensatz auf Server für Tabelle") + " ",
                                    efaCloudStorage.getStorageObjectType(), localRecord.getKey(), false);
                        }
                    }
                }
                // if a full download is executed, use this to also upload recent local insertions
                if (synch_download_all) {
                    for (String unmatched : unmatchedLocalKeys.keySet()) {
                        DataRecord cachedRecord = unmatchedLocalKeys.get(unmatched);
                        if (cachedRecord != null) {
                            // cachedRecord != null indicates, that the locally existing record was not part
                            // of the download server table. Insert it, if sufficiently recent
                            boolean localRecentChange = ((System.currentTimeMillis() - cachedRecord.getLastModified()) <
                                    synch_upload_look_back_ms);
                            if (localRecentChange) {
                                efaCloudStorage.modifyServerRecord(cachedRecord, true, false, false, true);
                                logSynchMessage(
                                        International.getString("Füge Datensatz auf Server ein für Tabelle") + " ", tx.tablename,
                                        cachedRecord.getKey(), false);
                            }
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
                txq.logApiMessage(International.getMessage("Konnte folgende Tabelle nicht für das Hochladen finden: {table}",
                        tx.tablename), 1);
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
                        long localLastModified = localRecord.getLastModified();
                        if (serverRecord == null) {
                            if (localLastModified > LastModifiedLimit)
                                localRecordsToInsertAtServer.add(localRecord);
                        } else if (localLastModified > serverRecord.getLastModified()) {
                            String preUpdateRecordsCompareResult = ""; // removed August 2024: preUpdateRecordsCompare(localRecord, serverRecord, tx.tablename);
                            if (!preUpdateRecordsCompareResult.isEmpty())
                                localRecordsToUpdateAtServer.add(localRecord);
                            else {
                                logSynchMessage(International.getMessage(
                                        "Update-Konflikt bei Datensatz in der {type}-Synchronisation. Unterschiedlich sind: {fields}",
                                        "Upload", preUpdateRecordsCompareResult) +
                                        " " + International.getString("Bitte bereinige den Datensatz manuell."), tx.tablename,
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
                    logSynchMessage(International.getString("Füge Datensatz auf Server ein für Tabelle") + " ", tx.tablename,
                            localRecordToInsertAtServer.getKey(), false);
                }
                for (DataRecord localRecordToUpdateAtServer : localRecordsToUpdateAtServer) {
                    persistence.modifyServerRecord(localRecordToUpdateAtServer, false, true, false, true);
                    logSynchMessage(International.getString("Aktualisiere Datensatz auf Server für Tabelle") + " ",
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
            logSynchMessage(International.getMessage("Hole Datensätze vom Server mit Modifikation nach {date}", dateString),
                    tables_to_synchronize.get(table_synching_index), null, false);
        } else {
            // This TX_QUEUE_STOP_SYNCH request will stay a while in the loop, because it will only be handled after
            // all transactions of the synch queue have been processed.
            txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_STOP_SYNCH);
            logSynchMessage(International.getString(
                    "Transaktionen für Synchronisation vollständig angestoßen. Warte auf Fertigstellung."), "",
                    null, false);
        }
    }

}