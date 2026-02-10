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
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static de.nmichael.efa.data.efacloud.TxRequestQueue.TX_BUSY_QUEUE_INDEX;

// import java.nio.charset.StandardCharsets;  Java 8 only
// import java.util.Base64;  Java 8 only

public class TxResponseHandler {

    private final TxRequestQueue txq;

    TxResponseHandler(TxRequestQueue txq) {
        this.txq = txq;
    }

    /**
     * Append a log message to the synch log.
     *
     * @param tx the transaction triggering the log activity
     */
    void logMessage(Transaction tx) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String transactionString = tx.type + "_RESP [" + tx.tablename + "]: " + tx.getResultCode() + " - " +
                Transaction.TX_RESULT_CODES.get(tx.getResultCode());
        if (tx.getResultCode() > 300)
            transactionString += ". " + ((tx.getResultMessage().length() > 100) ?
                    tx.getResultMessage().substring(0, 100) + " ..." : tx.getResultMessage());
        String dateString = format.format(new Date()) + " INFO tx" + tx.ID + ", ";
        TextResource
                .writeContents(TxRequestQueue.logFilePath, dateString + transactionString,
                        true);
    }

    /**
     * In case the authentication fails alert an error. There will be nothing else done, because this actually can
     * happen an efaCloud startup on a Raspberry Pi due to an erroneous System time. So it just keeps the transaction at
     * front of the queue and triggers retries.
     *
     * @param resultCode the resultCode of the transaction container.
     */
    void handleAuthenticationError(int resultCode) {
        String errorMessage = International.getMessage(
                "Anmeldung von {username} auf efaCloud Server {efaCloudUrl} fehlgeschlagen.",
                txq.getAdminUserID(), txq.efaCloudUrl) +
                " " + International.getMessage("Fehler: {resultCode}", resultCode);
        if ((resultCode == 402) || (resultCode == 403)) {
            errorMessage = International
                    .getMessage("Anmeldung von {username} auf efaCloud Server {efaCloudUrl} abgelehnt.",
                            txq.getAdminUserID(),
                            txq.efaCloudUrl);
        } else if (resultCode == 506) {
            errorMessage = International
                    .getMessage("Anmeldung von {username} auf efaCloud Server {efaCloudUrl} fehlgeschlagen.",
                            txq.getAdminUserID(), txq.efaCloudUrl) +
                    " " + International.getString("Fehler bei der Internet-Verbindung oder der Serverkonfiguration.");
        }
        txq.saveAuditInformation();
        txq.logApiMessage(errorMessage, 1);
        Logger.log(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_ERROR, errorMessage);
        txq.serverWelcomeMessage = errorMessage;
    }

    /**
     * <p>Handle the error for a transaction container.</p><p>The transactions go to the failed
     * queue for:</p><ul><li>401 => "Syntax error",</li><li>402 => "Unknown client",</li><li>403 => "Authentication
     * failed",</li><li>500 => "Internal Server Error",</li><li>505 => "Server response empty",</li><li>506 => "Internet
     * connection aborted",</li><li>507 => "Could not decode server response",</li></ul></p><p>The transactions get a
     * retry:<ul><li>404 => "Server side busy",</li><li>406 => "Overload detected",</li><li>407 => "No data base
     * connection",
     * </li></ul></p><p>Authentication
     * failures 402 and 403 create an additional error log entry and warning dialog.</p>
     *
     * @param txrc container to handle the error for
     */
    private void handleTxcError(TxResponseContainer txrc, String errorMessage) {
        String droppedTxCnt = "" + txq.getQueueSize(TX_BUSY_QUEUE_INDEX);
        txq.logApiMessage(International.getString("Container-Fehler bei der Behandlung einer Serverantwort") +
                String.format(": cresult_code = %s, cresult_message = %s, errorMessage = %s.",
                        txrc.cresultCode, txrc.cresultMessage, errorMessage) +
                        International.getMessage("Betroffene (gescheiterte) Transaktionen: {droppedCount}", droppedTxCnt), 1);
        txq.registerContainerResult(txrc.cresultCode, txrc.cresultMessage);
        if (txq.getState() == TxRequestQueue.QUEUE_IS_AUTHENTICATING) {
            handleAuthenticationError(txrc.cresultCode);
            // do never remove a failed authentication transaction
        } else
            switch (txrc.cresultCode) {
                case 402:  // "Unknown client"
                case 403:  // "Authentication failed"
                    handleAuthenticationError(txrc.cresultCode);
                    // do never remove a failed authentication transaction
                    break;
                case 404:  // "Server side busy"
                case 406:  // "Overload detected"
                case 407:  // "No database connection"
                case 506:  // "Internet connection aborted"
                    txq.shiftTx(TX_BUSY_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, TxRequestQueue.ACTION_TX_RETRY, 0, 0);
                    if (txq.getState() == TxRequestQueue.QUEUE_IS_SYNCHRONIZING)
                        txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_STOP_SYNCH);
                    break;
                case 401:  // "Syntax error"
                case 500:  // "Internal server error"
                case 505:  // "Server response empty"
                case 507:  // "Could not decode server response"
                default:
                    txq.shiftTx(TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_FAILED_QUEUE_INDEX,
                            TxRequestQueue.ACTION_TX_CONTAINER_FAILED, 0, 0);
                    if (txq.getState() == TxRequestQueue.QUEUE_IS_SYNCHRONIZING)
                        txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_STOP_SYNCH);
                    break;
            }
    }

    /**
     * <p>Handle the error for a transaction. Final fails are</p><ul><li>401, "Syntax error"; </li><li>501,
     * "Transaction invalid"; </li><li>502, "Transaction failed"</li></ul><p>They will be forwarded to the Failed-Queue.
     * Other errors:</p><ul><li> 404, "Server side busy"; </li><li>405, "Wrong transaction ID";
     * </li><li>406, "Overload detected "; </li><li>407, "No data base connection"
     * </li></ul><p>are believed to be transient, and those transactions get a retry.</p>
     *
     * @param tx transaction to handle the error for
     */
    private void handleTxError(Transaction tx) {
        StringBuilder txString = new StringBuilder();
        tx.appendTxFullString(txString, 500);
        txq.logApiMessage(International.getMessage(
                "Transaktions-Fehler bei der Behandlung einer Serverantwort: result_code = {resultCode}, " +
                        "result_message = {resultMessage}, Transaktion: {transaction}", "" + tx.getResultCode(),
                tx.getResultMessage(), txString.toString()), 1);
        switch (tx.getResultCode()) {
            case 401:  // "Syntax error"
            case 501:  // "Transaction invalid"
            case 502:  // "Transaction failed"
                txq.shiftTx(TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_FAILED_QUEUE_INDEX, TxRequestQueue.ACTION_TX_CLOSE,
                        tx.ID, 1);
                if (tx.type.isTableStructureEdit) Dialog.error(
                        International.getMessage("Fehler bei Tabellenstrukturänderung: {type}. " +
                                "Strukturänderungen erfordern eine besondere Berechtigung.", tx.type.typeString));
                break;
            case 404:  // "Server side busy"
            case 405:  // "Wrong transaction ID"
            case 406:  // "Overload detected"
            case 407:  // "No data base connection"
                txq.shiftTx(TX_BUSY_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, TxRequestQueue.ACTION_TX_RETRY, tx.ID, 1);
                break;
        }
    }

    /**
     * Handle the result for a specific transaction. This part here is to run transaction success operations. Will
     * forward the transaction to handleTxError, if the resultCode is >= 400.
     *
     * @param tx transaction to handle the result for
     */
    private void handleTxResult(Transaction tx) {

        logMessage(tx);

        // handle an error
        if (tx.getResultCode() >= 400) {
            handleTxError(tx);
            return;
        }

        // handle a positive response.  
        if (tx.getResultCode() >= 300) {
            // handle synchronization responses
            if (txq.getState() == TxRequestQueue.QUEUE_IS_SYNCHRONIZING) {
                try {
                    if (tx.type == Transaction.TX_TYPE.SYNCH) {
                        if (tx.tablename.equalsIgnoreCase("@all"))
                            // the response statement on the @all request is the starting point for both
                            // synchronization directions
                            txq.synchControl.buildSynchTableListAndStartSynch(tx);
                    } else if (tx.type == Transaction.TX_TYPE.SELECT) {
                        if (txq.synchControl.synch_upload)
                            txq.synchControl.nextTableForUploadSynch(tx);
                        else
                            txq.synchControl.nextTableForDownloadSelect(tx);
                    } else
                        // no specific handling for other ok-type responses.
                        // move transaction to the done queue
                        txq.shiftTx(TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_DONE_QUEUE_INDEX,
                                TxRequestQueue.ACTION_TX_CLOSE, tx.ID, 1);
                } catch (Exception e) {
                    StringBuilder stackTrace = new StringBuilder();
                    for (StackTraceElement ste : e.getStackTrace()) {
                        stackTrace.append(ste).append("\n");
                    }
                    txq.logApiMessage(International
                            .getMessage("Ausnahme-Fehler bei der Behandlung einer Serverantwort: {error}",
                                    stackTrace.toString()), 1);
                }
            }
            // one special case for an ok result: If the queue is authenticating or disconnected, put it to working
            if ((txq.getState() == TxRequestQueue.QUEUE_IS_AUTHENTICATING) ||
                    (txq.getState() == TxRequestQueue.QUEUE_IS_DISCONNECTED)) {
                txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_START);
                if (tx.type == Transaction.TX_TYPE.SYNCH) {
                    String[] counts = tx.getResultMessage().split(";");
                    if (counts.length < 10) {
                        String warningMessage = International.getMessage(
                                "Auf dem efaCloud-Server wurden nur {count} Tabellen und damit weniger als die " +
                                        "erforderliche Anzahl gefunden. " +
                                        "Bitte prüfe die Server-Installation.", counts.length);
                        Dialog.infoDialog(International.getString("Server Datenbank unvollständig."), warningMessage);
                    }
                } else if (tx.type == Transaction.TX_TYPE.NOP) {
                    // Parameter settings can be transported via the NOP transaction.
                    String[] cfg = tx.getResultMessage().split(";");
                    for (String param : cfg) {
                        if (param.contains("=")) {
                            String name = param.split("=", 2)[0];
                            if (name.equalsIgnoreCase("server_welcome_message")) {
                                txq.serverWelcomeMessage = param.split("=", 2)[1].replace("//", "\n");
                                txq.synchControl.efaCloudRolleBths = txq.serverWelcomeMessage.contains("Rolle: bths");
                                txq.synchControl.isBoathouseApp = (Daten.applID == Daten.APPL_EFABH);
                            } else if (name.equalsIgnoreCase("db_layout"))
                                Daten.tableBuilder.mapServerDBLayout(param.split("=", 2)[1]);
                            else {
                                int val = -1;
                                try {
                                    val = Integer.parseInt(param.split("=", 2)[1]);
                                } catch (Exception ignored) {
                                }
                                if (val > 0) {
                                    if (name.equalsIgnoreCase("synch_check_period"))
                                        TxRequestQueue.synch_check_polls_period =
                                                1000 * val / TxRequestQueue.QUEUE_TIMER_TRIGGER_INTERVAL;
                                    else if (name.equalsIgnoreCase("synch_period"))
                                        TxRequestQueue.synch_period = 1000 * val;
                                    else if (name.equalsIgnoreCase("group_memberidlist_size"))
                                        Daten.tableBuilder.adjustGroupMemberIdListSize(val);
                                    else if (name.equalsIgnoreCase("logs_to_return"))
                                        TxRequestQueue.logs_to_return = val;
                                }
                                if (val == 0) {
                                    if (name.equalsIgnoreCase("synch_check_period"))
                                        TxRequestQueue.synch_check_polls_period = Integer.MAX_VALUE;
                                    else if (name.equalsIgnoreCase("synch_period"))
                                        TxRequestQueue.synch_period = TxRequestQueue.SYNCH_PERIOD_DEFAULT;
                                    // if group_memberidlist_size is 0 do nothing, the default is already set.
                                    else if (name.equalsIgnoreCase("logs_to_return"))
                                        TxRequestQueue.logs_to_return = val;
                                }
                            }
                        }
                    }
                    txq.saveAuditInformation();
                }
            }
            // key change now handled differently, not based on the responce code, but the response message
            // if ((txq.getState() != TxRequestQueue.QUEUE_IS_SYNCHRONIZING) && (tx.getResultCode() == 303))
            //    txq.synchControl.fixOneKeyForTable(tx, tx.tablename);
            // adjust auto-incremented key
            this.adjustIncrmentedKey(tx);
            // no specific handling for other ok-type responses.
            // move transaction to the done queue
            txq.shiftTx(TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_DONE_QUEUE_INDEX, TxRequestQueue.ACTION_TX_CLOSE, tx.ID,
                    1);
        }
    }

    /**
     * parse the transaction response for a new numeric, auto-incremented key. If a key change ist detected,
     * propagated the change to the local data record including the adjustment of the EntryNo in the boatStatus
     * record for open session.
     * @param tx the transaction after parsing the response message
     */
    private void adjustIncrmentedKey(Transaction tx) {
        // only
        if (((tx.type == Transaction.TX_TYPE.INSERT) || (tx.type == Transaction.TX_TYPE.UPDATE))
                && (tx.tablename.equals(Logbook.DATATYPE) || tx.tablename.equals(Messages.DATATYPE) ||
                tx.tablename.equals(BoatDamages.DATATYPE)||
                tx.tablename.equals(BoatReservations.DATATYPE))) {
            // Declare the key field names for the four possible data types
            HashMap<String, String> keyFields = new HashMap<>();
            keyFields.put(Logbook.DATATYPE, LogbookRecord.ENTRYID);
            keyFields.put(Messages.DATATYPE, MessageRecord.MESSAGEID);
            keyFields.put(BoatDamages.DATATYPE, BoatDamageRecord.DAMAGE);
            keyFields.put(BoatReservations.DATATYPE, BoatReservationRecord.RESERVATION);
            // parse returned keys
            String[] returnedKeys = tx.getResultMessage().split(";");
            HashMap<String, String> returnedMap = new HashMap<>();
            for (String returnedKey : returnedKeys)
                if (returnedKey.contains("="))
                    returnedMap.put(returnedKey.split("=")[0], returnedKey.split("=")[1]);
            // update the current record
            if (Ecrid.iEcrids.get(returnedMap.get(Ecrid.ECRID_FIELDNAME)) != null) {
                DataRecord current = Ecrid.iEcrids.get(returnedMap.get(Ecrid.ECRID_FIELDNAME));
                DataRecord updated = current.cloneRecord();
                // only update for a incrementing key field change
                boolean keyFieldCchanged = false;
                for (String dataType : keyFields.keySet()) {
                    String keyField = keyFields.get(dataType);
                    if ((tx.tablename.equals(dataType) && (returnedMap.get(keyField) != null) &&
                            ! returnedMap.get(keyField).equalsIgnoreCase(current.getAsString(keyField)))) {
                        updated.setFromText(keyField, returnedMap.get(keyField));
                        keyFieldCchanged = true;
                    }
                }
                if (keyFieldCchanged) {
                    EfaCloudStorage efaCloudStorage = Daten.tableBuilder.getPersistence(tx.tablename);
                    try {
                        efaCloudStorage.modifyLocalRecord(updated, false, true, false);
                        // check and update the boat status record, if the current trip is open
                        // you will have to use a new lock, because the old one uses the wrong table
                        if (tx.tablename.equals(Logbook.DATATYPE) && (((LogbookRecord) current).getSessionIsOpen()))
                            txq.synchControl.adjustBoatStatus(((LogbookRecord) current).getBoatId(),
                                    ((LogbookRecord) updated).getEntryId().intValue());
                    } catch (EfaException e) {
                        // if immediate update does not work, synchronisation will care for it
                    }
                }
            }
        }
    }

    /**
     * <p>Handle the response provided by the InternetAccessManager. Takes the transaction response
     * container, parses all responses, selects the respective transactions and executes all response related
     * activities. Shifts the response to the respective result queue: done, retry or failed.
     * </p><p>
     * Since tasks to the InternetAccessManager do not overlap, there is no need to correlate the transaction IDs
     * received. The tsBusy queue is emptied completely by this call. If any transaction within the queue was not in the
     * response, it is closed as failed.</p>
     *
     * @param txcResp response provided by the InternetAccessManager
     */
    void handleTxcResponse(TaskManager.RequestMessage txcResp) {
        // Encoding of TaskManager.RequestMessage for the callback is:
        // title = file system location to which the file was stored, if a file is stored.
        // Dialog title for progress messages. postURLplus for aborted actions Else it is empty
        // String title = txResp.title;
        // text = result string, Usually empty for get, server response for call_post and error
        // message in case of error.
        String text = txcResp.text;
        // type = type of message. TYPE_FILE_SIZE_INFO, TYPE_PROGRESS_INFO, TYPE_COMPLETED,
        // TYPE_ABORTED are available
        int type = txcResp.type;
        // value = file size in byte for TYPE_FILE_SIZE_INFO, part downloaded (0 .. 1) for
        // TYPE_PROGRESS_INFO
        // and TYPE_COMPLETED_INFO, ignored here.

        if (type == InternetAccessManager.TYPE_ABORTED) {
            // the transaction container sending was aborted.
            TxResponseContainer txrc = new TxResponseContainer(null);
            handleTxcError(txrc, text + ";;" + txcResp.title);

            // } else if (type == InternetAccessManager.TYPE_PROGRESS_INFO) { // not relevant
            // } else if (type == InternetAccessManager.TYPE_FILE_SIZE_INFO) { // not relevant

        } else if (type == InternetAccessManager.TYPE_COMPLETED) {
            // parse the transaction response container contents
            TxResponseContainer txrc = new TxResponseContainer(text);
            txq.registerContainerResult(txrc.cresultCode, txrc.cresultMessage);
            if (txrc.cresultCode >= 400) {
                handleTxcError(txrc, "Anwendungsfehler");
            } else if (txrc.cresultCode == 304) {
                long lowa = Long.MIN_VALUE;
                try {
                    lowa = Long.parseLong(txrc.cresultMessage);
                } catch (Exception ignored) {
                    // just drop invalid responses to a synch check
                }
                if (lowa > txq.synchControl.lastSynchStartedMillis)
                    txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_START_SYNCH_DOWNLOAD);
            } else {
                // handle all transactions contained
                for (String txm : txrc.txms) {
                    String[] txRespParts = txm.split(TxRequestQueue.TX_RESP_DELIMITER, 3);
                    int txID = 0;
                    int resultCode = 501;  // Default if parsing fails
                    try {
                        txID = Integer.parseInt(txRespParts[0]);
                        resultCode = Integer.parseInt(txRespParts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                    Transaction tx = txq.getTxForID(txID, TX_BUSY_QUEUE_INDEX, true);
                    if (tx != null) {
                        tx.setResultCode(resultCode);
                        tx.setCresultCode(txrc.cresultCode);
                        tx.setResultAt(System.currentTimeMillis());
                        tx.setResultMessage(txRespParts[2]);
                        handleTxResult(tx);
                    }
                }
            }

            // clear the queue from the remaining transactions, for which no response was contained, except in
            // authenticating state. Then upon all errors it shall be retried.
            if ((txq.getBusyQueueSize() > 0) && (!txq.busyHeadIsNop())) {
                String droppedTxCnt = "" + txq.getQueueSize(TX_BUSY_QUEUE_INDEX);
                txq.logApiMessage(International.getMessage(
                        "Container-Fehler in einer Serverantwort: {droppedCount} Transaktionen " +
                                "im Container ohne Serverantwort. Sie gelten als gescheitert, vgl. txFailedQueue " +
                                "Datei", droppedTxCnt), 1);
                txq.shiftTx(TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_FAILED_QUEUE_INDEX,
                        TxRequestQueue.ACTION_TX_RESP_MISSING, 0, 0);
            }
        }
    }

    /**
     * <p>Handle the response provided by the InternetAccessManager. Takes the transaction response
     * container, parses all responses, selects the respective transactions and executes all response related
     * activities. Shifts the response to the respective result queue: done, retry or failed.
     * </p>
     */
    static class TxResponseContainer {
        final int cID;
        final int version;
        final int cresultCode;
        final String cresultMessage;
        final String[] txms;

        /**
         * Decode and handle the response container and return the transaction response messages as String[]. If the
         * response container parsing fails, all busy transactions are shifted to the failed queue with an appropriate
         * error in the result message and an empty String[] is returned.
         *
         * @param txcResponse the response container which shall be parsed. Set null, if the container was aborted.
         */
        TxResponseContainer(String txcResponse) {
            if (txcResponse == null) {
                this.cID = 0;
                this.version = 0;
                this.cresultCode = 506;
                this.cresultMessage = "Internet connection aborted";
                this.txms = new String[0];
            } else {
                // Synch check response is received, detected by the ";" character
               if (txcResponse.contains(";")) {
                    this.cID = 0;
                    this.version = 0;
                    this.cresultCode = 304;  // "Valid synchronisation check response"
                    this.cresultMessage = txcResponse.split(";", 2)[0];
                    this.txms = new String[0];
                    return;
                }
                // transaction container was received, and the response is returned. Split all transaction, hand over
                // the result code and result message, trigger transaction handling and close the transactions decode
                // the transaction response container
                String txContainerBase64 = txcResponse.replace('-', '/').replace('*', '+').replace('_', '=').trim();
                String txContainer;
                try {
                    // Java 8: txContainer = new String(Base64.getDecoder().decode(txContainerBase64),
                    // StandardCharsets.UTF_8);
                    txContainer = new String(Base64.decode(txContainerBase64), "UTF-8");  // Java 6
                } catch (Exception ignored) {
                    txContainer = TxRequestQueue.efa_cloud_used_api_version + ";0;503;" + Transaction.TX_RESULT_CODES.get(503) +
                            TxRequestQueue.TX_RESP_DELIMITER;
                }

                // parse the transaction response container header
                String[] headerAndContent = txContainer.split(TxRequestQueue.TX_RESP_DELIMITER, 5);
                int cID = 0;
                int version = 0;
                int cresult_code = 0;
                try {
                    cID = Integer.parseInt(headerAndContent[0]);
                    version = Integer.parseInt(headerAndContent[1]);
                    // adjust API protocol version to the common maximum
                    if (version > TxRequestQueue.efa_cloud_used_api_version)
                        TxRequestQueue.efa_cloud_used_api_version = Math.min(version,
                                TxRequestQueue.EFA_CLOUD_MAX_API_VERSION);
                    cresult_code = Integer.parseInt(headerAndContent[2]);
                } catch (NumberFormatException ignored) {
                }
                this.cID = cID;
                this.version = version;
                this.cresultCode = cresult_code;
                this.cresultMessage = headerAndContent[3];
                if ((headerAndContent.length > 4) && (headerAndContent[4] != null))
                    txms = headerAndContent[4].split(Transaction.MESSAGE_SEPARATOR_STRING_REGEX);
                else
                    txms = new String[0];
            }
        }
    }
}