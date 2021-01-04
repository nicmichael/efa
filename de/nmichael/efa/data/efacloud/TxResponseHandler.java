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

import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// import java.nio.charset.StandardCharsets;  Java 8 only
// import java.util.Base64;  Java 8 only
import java.text.SimpleDateFormat;
import java.util.Date;

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
        String transactionString = "#" + tx.ID + ", " + tx.type + ": " + tx.getResultCode() + " - " +
                Transaction.TX_RESULT_CODES.get(tx.getResultCode());
        String dateString = format.format(new Date()) + " [receive for " + tx.tablename + "]: ";
        TextResource.writeContents(TxRequestQueue.logFilePaths.get("API activity"), dateString + transactionString,
                true);
    }

    /**
     * In case the authentication fails stop the transaction queue, and alert an error.
     *
     * @param resultCode the resultCode of the transaction container.
     */
    void handleAuthenticationError(int resultCode) {
        String errorMessage = International.getMessage(
                "Anmeldung von {username} auf efaCloud server {efaCloudUrl} fehlgeschlagen. Unbekannter Fehlertyp. " +
                        "Deaktiviere Efacloud.", txq.username, txq.efaCloudUrl);
        if ((resultCode == 402) || (resultCode == 403)) {
            txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_DEACTIVATE);
            errorMessage = International.getMessage(
                    "Anmeldung von {username} auf efaCloud server {efaCloudUrl} abgelehnt. Deaktiviere Efacloud.",
                    txq.username, txq.efaCloudUrl);
        } else if (resultCode == 500) {
            errorMessage = International.getMessage(
                    "Anmeldung von {username} auf efaCloud server {efaCloudUrl} führte zu einem Severfehler " +
                            "(500 internal Server Error). Bitte prüfen sie die Server-Installation. Deaktiviere " +
                            "Efacloud.", txq.username, txq.efaCloudUrl);
        }
        Dialog.error(errorMessage);
        Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR, errorMessage);
    }

    /**
     * <p>Handle the error for a transaction container.</p><p>The transactions go to the failed
     * queue for:</p><ul><li>401 => "Syntax error",</li><li>402 => "Unknown client",</li><li>403 => "Authentication
     * failed",</li><li>500 => "Internal Server Error",</li><li>503 => "Could not decode server
     * response",</li></ul></p><p>The transactions get a retry:<ul><li>404 => "Server side busy",</li><li>406 =>
     * "Overload detected",</li><li>407 => "No data base connection",</li></ul></p><p>Authentication failures 402 and
     * 403 create an additional error log entry and warning dialog.</p>
     *
     * @param txrc container to handle the error for
     */
    private void handleTxcError(TxResponseContainer txrc) {
        String droppedTxCnt = "" + txq.getQueueSize(TxRequestQueue.TX_BUSY_QUEUE_INDEX);
        txq.logApiMessage(International.getMessage(
                "CONTAINER-Fehler bei der Behandlung einer Serverantwort: cresult_code = {resultCode}, " +
                        "cresult_message = {resultMessage}. Betroffene, damit gescheiterte Transaktionen: " +
                        "{droppedCount}", txrc.cresultCode, txrc.cresultMessage, droppedTxCnt), 1);
        // Any error shall force a fallback to normal, because the synchronization process relies on the answers and
        // if they don't come it will not end and continue to block the normal communication
        if (txq.getState() == TxRequestQueue.QUEUE_IS_SYNCHRONIZING) {
            txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_RESUME);
            txq.logApiMessage(International.getString("Die Synchronisation wird beendet."), 1);
        }
        txq.registerContainerResult(txrc.cresultCode, txrc.cresultMessage);
        if (txq.getState() == TxRequestQueue.QUEUE_IS_AUTHENTICATING) {
            handleAuthenticationError(txrc.cresultCode);
            txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_DEACTIVATE);
        } else
            switch (txrc.cresultCode) {
                case 402:  // "Unknown client"
                case 403:  // "Authentication failed"
                    handleAuthenticationError(txrc.cresultCode);
                case 401:  // "Syntax error"
                case 500:  // "Internal server error"
                case 503:  // "Could not decode server response"
                default:
                    txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_FAILED_QUEUE_INDEX,
                            TxRequestQueue.ACTION_TX_CONTAINER_FAILED, 0, 0);
                    if (txq.getState() == TxRequestQueue.QUEUE_IS_SYNCHRONIZING)
                        txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_STOP_SYNCH);
                    break;
                case 404:  // "Server side busy"
                case 406:  // "Overload detected"
                case 407:  // "No data base connection"
                    txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_BUSY_QUEUE_INDEX,
                            TxRequestQueue.ACTION_TX_RETRY, 0, 0);
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
        tx.appendTxFullString(txString);
        txq.logApiMessage(International.getMessage(
                "Transaktions-Fehler bei der Behandlung einer Serverantwort: result_code = {resultCode}, " +
                        "result_message = {resultMessage}, Transaktion: {transaction}", "" + tx.getResultCode(),
                tx.getResultMessage(), txString.toString()), 1);
        // Any error shall force a fallback to normal, because the synchronization process relies on the answers and
        // if they don't come it will not end and continue to block the normal communication
        if (txq.getState() == TxRequestQueue.RQ_QUEUE_STOP_SYNCH) {
            txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_RESUME);
            txq.logApiMessage(International.getString("Die Synchronisation wird beendet."), 1);
        }
        switch (tx.getResultCode()) {
            case 401:  // "Syntax error"
            case 501:  // "Transaction invalid"
            case 502:  // "Transaction failed"
                txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_FAILED_QUEUE_INDEX,
                        TxRequestQueue.ACTION_TX_CLOSE, tx.ID, 1);
                break;
            case 404:  // "Server side busy"
            case 405:  // "Wrong transaction ID"
            case 406:  // "Overload detected"
            case 407:  // "No data base connection"
                txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_BUSY_QUEUE_INDEX,
                        TxRequestQueue.ACTION_TX_RETRY, tx.ID, 1);
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
                if (tx.type == Transaction.TX_TYPE.KEYFIXING) {
                    if (tx.getResultCode() == 303)   // result 303 indicates the key change
                        txq.synchControl.fixOneKeyForTable(tx);
                    else  // after all keys of one table are fixed, move to next or trigger next step
                        txq.synchControl.fixKeysForNextTable();
                } else if (tx.type == Transaction.TX_TYPE.SYNCH) {
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
                    txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_DONE_QUEUE_INDEX,
                            TxRequestQueue.ACTION_TX_CLOSE, tx.ID, 1);
            }
            // one special case for an ok result: If the queue is authenticating, put it to working
            if (txq.getState() == TxRequestQueue.QUEUE_IS_AUTHENTICATING) {
                txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_START);
                if (tx.type == Transaction.TX_TYPE.SYNCH) {
                    String[] counts = tx.getResultMessage().split(";");
                    if (counts.length < 10) {
                        String warningMessage = International.getMessage(
                                "Auf dem Efacloud-Server wurden nur {count} Tabellen und damit weniger als die " +
                                        "erforderliche Anzahl gefunden.\n" +
                                        "Sollen die efa-Tabellen jetzt initialisiert werden?", counts.length);
                        if (Dialog.yesNoDialog(International.getString("Server Datenbank unvollständig."),
                                warningMessage) == Dialog.YES)
                            txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_START_SYNCH_DELETE);
                    }
                }
            }
            // no specific handling for other ok-type responses.
            // move transaction to the done queue
            txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_DONE_QUEUE_INDEX,
                    TxRequestQueue.ACTION_TX_CLOSE, tx.ID, 1);
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
            handleTxcError(txrc);

            // } else if (type == InternetAccessManager.TYPE_PROGRESS_INFO) { // not relevant
            // } else if (type == InternetAccessManager.TYPE_FILE_SIZE_INFO) { // not relevant

        } else if (type == InternetAccessManager.TYPE_COMPLETED) {
            // parse the transaction response container contents
            TxResponseContainer txrc = new TxResponseContainer(text);
            txq.registerContainerResult(txrc.cresultCode, txrc.cresultMessage);
            if (txrc.cresultCode >= 400) {
                handleTxcError(txrc);
            } else {
                // handle all transactions contained
                for (String txm : txrc.txms) {
                    String[] txRespParts = txm.split(TxRequestQueue.TX_RESP_DELIMITER, 3);
                    int txID = 0;
                    int resultCode = 501;
                    try {
                        txID = Integer.parseInt(txRespParts[0]);
                        resultCode = Integer.parseInt(txRespParts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                    Transaction tx = txq.getTxForID(txID, TxRequestQueue.TX_BUSY_QUEUE_INDEX, true);
                    if (tx != null) {
                        tx.setResultCode(resultCode);
                        tx.setCresultCode(txrc.cresultCode);
                        tx.setResultAt(System.currentTimeMillis());
                        tx.setResultMessage(txRespParts[2]);
                        handleTxResult(tx);
                    }
                }
            }

            // clear the queue from the remaining transactions, for which no response was contained.
            if (txq.getBusyQueueSize() > 0) {
                String droppedTxCnt = "" + txq.getQueueSize(TxRequestQueue.TX_BUSY_QUEUE_INDEX);
                txq.logApiMessage(International.getMessage(
                        "Container-Fehler in einer Serverantwort: {droppedCount} Transaktionen " +
                                "im Container ohne Serverantwort. Sie gelten als gescheitert, vgl. txFailedQueue " +
                                "Datei", droppedTxCnt), 1);
                txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_FAILED_QUEUE_INDEX,
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
                this.cresultCode = 500;
                this.cresultMessage = "Transaction container aborted";
                this.txms = new String[0];
            } else {
                // transaction container was received, and the response is returned. Split all transaction, hand over
                // the result code and result message, trigger transaction handling and close the transactions decode
                // the transaction response container
                String txContainerBase64 = txcResponse.replace('-', '/').replace('*', '+').replace('_', '=').trim();
                String txContainer = "";
                try {
                    // Java 8: txContainer = new String(Base64.getDecoder().decode(txContainerBase64), StandardCharsets.UTF_8);
                    txContainer = new String(Base64.decode(txContainerBase64), "UTF-8");  // Java 6
                } catch (Exception ignored) {
                    txContainer = TxRequestQueue.EFA_CLOUD_VERSION + ";0;503;" + Transaction.TX_RESULT_CODES.get(503) +
                            TxRequestQueue.TX_RESP_DELIMITER;
                }

                // parse the transaction response container header
                String[] headerAndContent = txContainer.split(TxRequestQueue.TX_RESP_DELIMITER, 5);
                // TODO: handling of cID and version information in transaction response container.
                int cID = 0;
                int version = 0;
                int cresult_code = 0;
                try {
                    cID = Integer.parseInt(headerAndContent[0]);
                    version = Integer.parseInt(headerAndContent[1]);
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