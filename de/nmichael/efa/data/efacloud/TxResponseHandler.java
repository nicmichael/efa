package de.nmichael.efa.data.efacloud;

import de.nmichael.efa.Daten;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TxResponseHandler {

    private final TxRequestQueue txq;

    TxResponseHandler(TxRequestQueue txq) {
        this.txq = txq;
    }

    /**
     * <p>Handle the error for a transaction container.</p><p>The transactions go to the failed
     * queue for:</p><ul><li>401 => "Syntax error.",</li><li>402 => "Unknown client.",</li><li>403
     * => "Authentication failed.",</li></ul></p><p>The transactions got to the retry queue
     * for:<ul><li>404 => "Server side busy.",</li><li>406 => "Overload detected.",</li><li>407 =>
     * "No data base connection.",</li><li>500 => "Transaction container aborted."</li></ul></p>
     *
     * @param txrc container to handle the error for
     */
    private void handleTxContainerError(TxResponseContainer txrc) {
        String droppedTxIDs = txq.listIDsContainedInBusy();
        Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR, String.format(
                "TxResponseHandler.handleTxResponse error: cresult_code = %s, " +
                "cresult_message = %s Failed transactions: %s",
                txrc.cresultCode, txrc.cresultMessage, droppedTxIDs));
        try {
            txq.registerContainerResult(txrc.cresultCode, txrc.cresultMessage);
            switch (txrc.cresultCode) {
                case 402:
                case 403:
                    txq.handleAuthenticationError(txrc.cresultCode);
                    txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX,
                            TxRequestQueue.TX_FAILED_QUEUE_INDEX,
                            TxRequestQueue.ACTION_TX_CONTAINER_FAILED, 0, 0, true);
                    break;
                case 401:
                case 408:
                default:
                    txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX,
                            TxRequestQueue.TX_FAILED_QUEUE_INDEX,
                            TxRequestQueue.ACTION_TX_CONTAINER_FAILED, 0, 0, true);
                    break;
                case 404:
                case 406:
                case 407:
                case 500:
                    txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX,
                            TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.ACTION_TX_RETRY, 0,
                            0, true);
                    break;
            }
        } catch (EfaException e) {
            Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR, "TxResponseContainer: queue lock time out (clearing queue).");
        }
    }

    /**
     * <p>Handle the error for a transaction. Final fails are</p><ul><li> 400, "XHTTPrequest
     * Error." (client side generated error, javascript version only, shall never occur here);
     * </li><li>401, "Syntax error."; </li><li>402, "Unknown client ."; </li><li>403,
     * "Authentication failed.";
     * </li><li>500, "Internal server error."; </li><li>501, "Transaction invalid."; </li><li>502,
     * "Transaction failed."</li></ul><p>They will be forwarded to the Failed-Queue. Other
     * errors:</p><ul><li> 404, "Server side busy."; </li><li>405, "Wrong transaction ID.";
     * </li><li>406, "Overload detected ."; </li><li>407, "No data base connection."
     * </li></ul><p>are believed to be transient, and those transactions get a retry.</p>
     *
     * @param tx transaction to handle the error for
     * @throws EfaException on queue shift error
     */
    private void handleTxError(Transaction tx) throws EfaException {
        switch (tx.getResultCode()) {
            case 400:
            case 401:
            case 402:
            case 403:
            case 500:
            case 501:
            case 502:
                txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX,
                        TxRequestQueue.TX_FAILED_QUEUE_INDEX, TxRequestQueue.ACTION_TX_CLOSE, tx.ID,
                        1, true);
                break;
            case 404:
            case 405:
            case 406:
            case 407:
                txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_BUSY_QUEUE_INDEX,
                        TxRequestQueue.ACTION_TX_RETRY, tx.ID, 1, true);
                break;
        }
    }

    /**
     * Handle the result for a specific transaction. This part here is to run transaction success
     * operations. Will forward the transaction to handleTxError, if the resultCode is >= 400.
     *
     * @param tx transaction to handle the result for
     * @throws EfaException on queue shift error
     */
    private void handleTxResult(Transaction tx) throws EfaException {
        if (tx.getResultCode() >= 400) {
            handleTxError(tx);
            return;
        }
        if (tx.getResultCode() == 300) {
            txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_DONE_QUEUE_INDEX,
                    TxRequestQueue.ACTION_TX_CLOSE, tx.ID, 1, true);
            // Add the trigger to synchronize with the server, if the transaction was a change count
            if (tx.type.equalsIgnoreCase("synch") && tx.tablename.equalsIgnoreCase("#All")) {
                Daten.tableBuilder.setTablesToUpdate(tx);
                if (Daten.tableBuilder.tablesToUpdate.size() > 0) {
                    Thread synchThread = new Thread();

                    // and immediately add a full server synch on every restart.
                    EfaCloudSynch.runEfaCloudSynchTask(Daten.tableBuilder.guiBaseFrameOnAppLoading,
                            EfaCloudSynch.Mode.download, true, false, 0,
                            Daten.tableBuilder.guiBaseFrameOnAppLoading == null);
                }
            }

        } else if (tx.getResultCode() == 301) {
            // TODO Synch with result 301. Currently not an implemented scenario
            // for the manual synchronisation this is done synchronously by using the transaction
            // and will not need any handling here. Nothing to do for data update transactions.
            txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX, TxRequestQueue.TX_DONE_QUEUE_INDEX,
                    TxRequestQueue.ACTION_TX_CLOSE, tx.ID, 1, true);
        }
    }

    /**
     * <p>Handle the response provided by the InternetAccessManager. Takes the transaction response
     * container, parses all responses, selects the respective transactions and executes all
     * response related activities. Shifts the response to the respective result queue: done, retry
     * or failed.
     * </p><p>
     * Since tasks to the InternetAccessManager do not overlap, there is no need to correlate the
     * transaction IDs received. The tsBusy queue is emptied completely by this call. If any
     * transaction within the queue was not in the response, it is closed as failed.</p>
     *
     * @param txResp response provided by the InternetAccessManager
     */
    protected void handleTxResponse(TaskManager.RequestMessage txResp) {
        // Encoding of TaskManager.RequestMessage for the callback is:
        // title = file system location to which the file was stored, if a file is stored.
        // Dialog title for progress messages. postURLplus for aborted actions Else it is empty
        // String title = txResp.title;
        // text = result string, Usually empty for get, server response for call_post and error
        // message in case of error.
        String text = txResp.text;
        // type = type of message. TYPE_FILE_SIZE_INFO, TYPE_PROGRESS_INFO, TYPE_COMPLETED,
        // TYPE_ABORTED are available
        int type = txResp.type;
        // value = file size in byte for TYPE_FILE_SIZE_INFO, part downloaded (0 .. 1) for
        // TYPE_PROGRESS_INFO
        // and TYPE_COMPLETED_INFO, ignored here.

        if (type == InternetAccessManager.TYPE_ABORTED) {
            // the transaction container sending was aborted. move all busy transactions to
            // retry queue.
            TxResponseContainer txms = new TxResponseContainer(null);
            txq.registerContainerResult(txms.cresultCode, txms.cresultMessage);
            handleTxContainerError(txms);

            // } else if (type == InternetAccessManager.TYPE_PROGRESS_INFO) { // not relevant
            // } else if (type == InternetAccessManager.TYPE_FILE_SIZE_INFO) { // not relevant

        } else if (type == InternetAccessManager.TYPE_COMPLETED) {
            // parse the transaction response container contents
            TxResponseContainer txms = new TxResponseContainer(text);
            txq.registerContainerResult(txms.cresultCode, txms.cresultMessage);
            if (txms.cresultCode >= 400) {
                handleTxContainerError(txms);
            } else {
                // handle all transactions contained
                for (String txm : txms.txms) {
                    String[] txRespParts = txm.split(TxRequestQueue.TX_RESP_DELIMITER, 3);
                    int txID = Integer.parseInt(txRespParts[0]);
                    int resultCode = Integer.parseInt(txRespParts[1]);
                    Transaction tx = txq.getTxForID(txID, TxRequestQueue.TX_BUSY_QUEUE_INDEX);
                    if (tx != null) {
                        tx.setResultCode(resultCode);
                        tx.setCresultCode(txms.cresultCode);
                        tx.setResultAt(System.currentTimeMillis());
                        tx.setResultMessage(txRespParts[2]);
                        try {
                            handleTxResult(tx);
                        } catch (EfaException e) {
                            Logger.log(Logger.ERROR, Logger.MSG_FILE_WRITETHREAD_ERROR,
                                    String.format(
                                            "TxResponseHandler: message handling aborted for " +
                                                    "%s. " + "Error: %s.",
                                            tx.type, e.toString()));
                        }
                    }
                }
            }

            // clear the queue from the remaining transactions, for which no response was contained.
            if (txq.getBusyQueueSize() > 0) {
                Logger.log(Logger.ERROR, Logger.MSG_FILE_WRITETHREAD_ERROR, String.format(
                                "TxResponseHandler.handleTxResponse: transactions with ID: %s "
                                        + "not responded by server. They will be moved to the " + "failed queue.",
                                txq.listIDsContainedInBusy()));
                try {
                    txq.shiftTx(TxRequestQueue.TX_BUSY_QUEUE_INDEX,
                            TxRequestQueue.TX_FAILED_QUEUE_INDEX,
                            TxRequestQueue.ACTION_TX_RESP_MISSING, 0, 0, true);
                } catch (EfaException e) {
                    Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR, "TxResponseHandler: queue lock time out (clearing queue).");
                }
            }
        }
    }

    /**
     * <p>Handle the response provided by the InternetAccessManager. Takes the transaction response
     * container, parses all responses, selects the respective transactions and executes all
     * response related activities. Shifts the response to the respective result queue: done, retry
     * or failed.
     * </p>
     */
    class TxResponseContainer {
        final int cID;
        final int version;
        final int cresultCode;
        final String cresultMessage;
        final String[] txms;

        /**
         * Decode and handle the response container and return the transaction response messages as
         * String[]. If the response container parsing fails, all busy transactions are shifted to
         * the failed queue with an appropriate error in the result message and an empty String[] is
         * returned.
         *
         * @param txcResponse the response container which shall be parsed. Set null, if no response
         *                    text was received, e.g. 500 internal server error as response.)
         */
        TxResponseContainer(String txcResponse) {
            if (txcResponse == null) {
                this.cID = 0;
                this.version = 0;
                this.cresultCode = 500;
                this.cresultMessage = "no server response text received";
                this.txms = new String[0];
            } else {
                // transaction container was received, and the response is returned. Split
                // all transaction, hand over the result code and result message, trigger
                // transaction handling and close the transactions
                // decode the transaction response container
                String txContainerBase64 = txcResponse.replace('-', '/').replace('*', '+')
                        .replace('_', '=').trim();
                String txContainer = TxRequestQueue.EFA_CLOUD_VERSION + ";0;503;" +
                                "Could not decode server response. Unsupported character " +
                                        "encoding" + "." + TxRequestQueue.TX_RESP_DELIMITER;
                try {
                    txContainer = new String(Base64.getDecoder().decode(txContainerBase64),
                            StandardCharsets.UTF_8);
                } catch (Exception ignored) {
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
                if (cresult_code >= 400) {
                    handleTxContainerError(this);
                    txms = new String[0];
                } else {
                    txms = headerAndContent[4].split(Transaction.MESSAGE_SEPARATOR_STRING_REGEX);
                }
            }
        }
    }
}