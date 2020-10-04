package de.nmichael.efa.data.efacloud;

import de.nmichael.efa.Daten;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import java.io.File;
import java.util.*;

/**
 * <p>The efaCloud transaction request queue manager.</p>
 *
 * <p>Transaction queues are used to handle the communication with the efaCloud server.
 * All transactions are first locally executed and then appended to a transactions-pending queue.
 * From that queue they are forwarded to the efacloud server via the InternetAccessManager. The
 * transactions-pending queue is checked three times a second. And all pending transactions (max.
 * 30) are packaged into a single transaction container.</p>
 * <p>The protocol is meant to be used in a serial manner, i. e. no further client request
 * container is issued as long as another one is open and neither completed nor timed out. The
 * transactions of a transaction container waiting for a server response are called the “busy
 * transactions”. When the server response is received, the busy transactions are moved to the
 * transactions-done, or depending on the response code, to the retry or failed queues.</p>
 * <p>Timeout and retry queue</p>
 * <p>A transaction container times out at the client side after 30 seconds without a response. All
 * messages of that container are transferred into a transactions-retry queue. Retries will be
 * triggered all 20 minutes and upon each application start. Upon the retry trigger all transactions
 * from the retry queue are moved back to the pending-transactions queue.</p>
 * <p>In summary there are the following transaction queues:</p>
 *     <ol><li> transactions-pending (in-memory)
 *     </li><li> transactions busy (in-memory)
 *     </li><li> transactions-retry (locally on disc)
 *     </li><li> transactions-done (locally on disc)
 *     </li><li> transactions permanently failed (locally on disc)</li></ol>
 * <p>Queues cached locally on disc are preserved even after reboot of the local machine. In-memory
 * transactions are lost when closing down the efa client or rebooting.</p>
 */
public class TxRequestQueue implements TaskManager.RequestDispatcherIF {

    public static final int EFA_CLOUD_VERSION = 1;

    public static final char TX_REQ_DELIMITER = ';';
    public static final String TX_RESP_DELIMITER = ";";
    public static final char TX_QUOTATION = '"';
    public static final String URL_API_LOCATION = "/api/posttx.php";
    public static final long ONE_DAY_MILLIS = 24 * 3600 * 1000;

    // ms with which the queue timer starts processing requests
    public static final int TIMER_PENDING_START_DELAY = 1000;
    // ms after which the queue timer processes the next set of requests
    public static final int TIMER_PENDING_TRIGGER_INTERVAL = 300;

    // every TIMER_HOUSEKEEPING_POLL_INTERVALS housekeeping jobs will be performed.
    public static final int HOUSEKEEPING_POLL_INTERVALS = 240000; // = haldf a day
    // every TIMER_UPDATE_POLL_INTERVALS the client checks for updates at the server side.
    // The update period MUST be at least 5 times the InternetAccessManager timeout.
    public static final int DELTA_SYNCH_POLL_INTERVALS = 1000; // = 300 seconds
    // If a transaction is busy since more than the RETRY_AFTER_MILLISECONDS period
    // issue a new internet access request.
    public static final int RETRY_AFTER_MILLISECONDS = 30000;

    // timeout for holding a queue locked for manipulation.
    public static final long QUEUE_LOCK_TIMEOUT = 1000;
    // Maximum number of transactions shifted into the pending queue, i. e. of transactions per
    // internet access request. If the internet access is blocked, this will pile upt internet
    // access requests rather than transactions in the pending transactions queue.
    public static final int PENDING_QUEUE_MAX_SHIFT_SIZE = 10;

    public static HashMap<Integer, String> txCodes = new HashMap<>();
    private static int txID;
    private static int txcID;
    private static long pollsCount;
    private static TxRequestQueue txq = null;
    private static TxResponseHandler txr;

    static {
        txCodes.put(300, "Transaction completed.");
        txCodes.put(301, "Primary key modified.");
        txCodes.put(400,
                "XHTTPrequest Error."); // (client side generated error, javascript version only)
        txCodes.put(401, "Syntax error.");
        txCodes.put(402, "Unknown client.");
        txCodes.put(403, "Authentication failed.");
        txCodes.put(404, "Server side busy.");
        txCodes.put(405, "Wrong transaction ID."); // (client side generated error)
        txCodes.put(406, "Overload detected.");
        txCodes.put(407, "No data base connection.");
        txCodes.put(408, "Time out of synchronous transaction."); // (client side generated error)
        txCodes.put(500, "Internal server error.");
        txCodes.put(501, "Transaction invalid.");
        txCodes.put(502, "Transaction failed.");
    }

    // In order to be able to move a transaction quickly, a queue reference
    // is provided.
    public static final int TX_PENDING_QUEUE_INDEX = 0;
    public static final int TX_BUSY_QUEUE_INDEX = 1;
    public static final int TX_DONE_QUEUE_INDEX = 2;
    public static final int TX_FAILED_QUEUE_INDEX = 3;
    public static final String[] TX_QUEUE_NAMES = new String[]{"txPending", "txBusy", "txDone",
            "txFailed"};
    // days, after which transactions are discarded from permanent storage to avoid
    // performance issues cause by increasing file size.
    public static final int[] TX_PERMANENT_QUEUE_DROP_DAYS = new int[]{-1, -1, 14, 14, 90};
    // maximum count of transactions, before they are discarded from permanent storage to avoid
    // performance issues cause by increasing file size. Note: pending and busy may be written as
    // well for debug purposes.
    public static final int[] TX_PERMANENT_QUEUE_DROP_COUNT = new int[]{50, 50, 50, 100, 100};
    public static final int TX_QUEUE_COUNT = TX_QUEUE_NAMES.length;

    // Queue shift actions incur a transaction modification These are the action codes
    // to trigger the modification.
    public static final int ACTION_TX_SEND = 100;
    public static final int ACTION_TX_RETRY = 101;
    public static final int ACTION_TX_ABORT = 102;
    public static final int ACTION_TX_CLOSE = 103;
    public static final int ACTION_TX_RESP_MISSING = 104;
    public static final int ACTION_TX_CONTAINER_FAILED = 105;

    private final ArrayList<Vector<Transaction>> queues = new ArrayList<>();
    private final ArrayList<TaskManager.RequestMessage> queueResponses = new ArrayList<>();
    private final long[] locks = new long[TX_QUEUE_COUNT];
    private final String[] txFilePath = new String[TX_QUEUE_COUNT];
    private final ArrayList<Transaction.TxReceipt> txReceipts = new ArrayList<>();

    private Timer queueTimer;
    private final InternetAccessManager iam;
    protected boolean synchBusy = false;
    protected String efaCloudUrl;
    protected String username;
    protected String credentials;

    /**
     * Private constructor to run the queue as singleton class. There must not be more than one
     * queue per client active.
     *
     * @param efaCloudUrl     efaCloud server URL. Note the special feaure to allow for all
     *                        certificates when using httpx as protocol identifier instead of
     *                        https.
     * @param username        efaCloud server username
     * @param password        efaCloud server password
     * @param storageLocation the storageLocation directory without the File.separator ending to
     *                        store the retry transactions to.
     */
    private TxRequestQueue(String efaCloudUrl, String username, String password,
                           String storageLocation) {

        // initialize the internet access manager
        iam = InternetAccessManager.getInstance();
        // This shall be uncommented to trigger debug logging in the iam.
        // iam.debugFilePath = storageLocation + "iam.efa2txcontainer";

        // check and cleanse the URL
        if (efaCloudUrl.startsWith("httpx")) {
            iam.setAllowAllCertificates(true);
            this.efaCloudUrl = "https" + efaCloudUrl.substring("httpx".length());
        } else this.efaCloudUrl = efaCloudUrl;
        if (!this.efaCloudUrl.endsWith(URL_API_LOCATION)) {
            if (this.efaCloudUrl.endsWith("/"))
                this.efaCloudUrl = this.efaCloudUrl.substring(0, this.efaCloudUrl.length() - 1);
            this.efaCloudUrl += URL_API_LOCATION;
        }

        // combine the credentials to a transaction container prefix.
        this.username = username;
        this.credentials = username + TX_REQ_DELIMITER + CsvCodec
                .encodeElement(password, TX_REQ_DELIMITER, TX_QUOTATION) + TX_REQ_DELIMITER;

        // initialize the txID auto incrementation and poll counter (for the retry queue)
        txID = 42;
        txcID = 42;
        pollsCount = 0L;
        txr = new TxResponseHandler(this);
        // assemble the queues. The retry queue will not be read on restart, but after thte first
        // retry poll interval.
        for (int i = 0; i < locks.length; i++) {
            Vector<Transaction> queue = new Vector<>();
            queues.add(queue);
            txFilePath[i] = storageLocation + TX_QUEUE_NAMES[i] + ".efa2cloudqueue";
            releaseQueueLock(i);
        }

        startQueueTimer();

        // start a watchdog
        // TODO watchdog

    }

    /**
     * In case the authentication fails stop the transaction queue, and alert an error.
     * @param resultCode the resultCode of the transaction container.
     */
    protected void handleAuthenticationError(int resultCode) {
        if ((resultCode == 402) || (resultCode == 403)) {
            queueTimer.cancel();
            Dialog.error(International.getMessage(
                    "Anmeldung von {username} auf efaCloud server {efaCloudUrl} fehlgeschlagen.",
                    username, efaCloudUrl));
        }
    }

    /**
     * Start the queue timer, triggering all efacloud client to server transactions.
     */
    private void startQueueTimer() {
        // initialize the queue poll timer.
        TimerTask queueTimerTask = new TimerTask() {
            public void run() {
                // if a timer task encounters an unhandled exception, it stops and the timer dies.
                // that must be avoided at any case.
                try {
                    // initialization will be performed rather here than in the constructor.
                    if (pollsCount == 0) {
                        // start operation by adding as first transaction a "nop" transaction to verify the
                        // credentials.
                        appendTransactionPending(this, "nop", "", "sleep;0");
                        // and immediately add a full server synch on every restart.
                        appendTransactionPending(this, "synch", "#All", "LastModified;0");
                    }
                    pollsCount++;
                    // distinguish the poll scenarios, slowest first.
                    // housekeeping
                    if (pollsCount % HOUSEKEEPING_POLL_INTERVALS == 0) {
                        clearReceipts();
                    }
                    // server delta synchronisation trigger
                    else if (pollsCount % DELTA_SYNCH_POLL_INTERVALS == 0) {
                        // the update thread is asynchronous. If not, this would stop the
                        // timer from polling.
                        long lastModifiedLaterThan = System
                                .currentTimeMillis() - 2 * DELTA_SYNCH_POLL_INTERVALS * TIMER_PENDING_TRIGGER_INTERVAL;
                        // and immediately add a full server synch on every restart.
                        appendTransactionPending(this, "synch", "#All",
                                "LastModified;" + lastModifiedLaterThan);
                    }
                    // normal poll of queue
                    else {
                        // handle new pending requests, if currently no request is busy
                        if (queues.get(TX_BUSY_QUEUE_INDEX).isEmpty() && !queues
                                .get(TX_PENDING_QUEUE_INDEX).isEmpty()) {
                            try {
                                shiftTx(TX_PENDING_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, ACTION_TX_SEND,
                                        0, PENDING_QUEUE_MAX_SHIFT_SIZE, false);
                                TaskManager.RequestMessage rq = Transaction
                                        .createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX));
                                iam.sendRequest(rq);
                            } catch (EfaException e) {
                                Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR,
                                        String.format(
                                                "TxRequestQueue error (efacloud URL: %s): " +
                                                        "%s",
                                                TxRequestQueue.this.efaCloudUrl, e.toString()));
                                Logger.log(e);
                            }
                        }
                        // check busy requests for age. Initiate a retry, if needed. The retry is
                        // always triggered on all transactions of the queue.
                        else if (!queues.get(TX_BUSY_QUEUE_INDEX).isEmpty()) {
                            long now = System.currentTimeMillis();
                            long firstTxSentAt = queues.get(TX_BUSY_QUEUE_INDEX).firstElement()
                                    .getSentAt();
                            if (now - firstTxSentAt > RETRY_AFTER_MILLISECONDS) {
                                try {
                                    shiftTx(TX_BUSY_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX,
                                            ACTION_TX_RETRY, 0, 0, false);
                                    TaskManager.RequestMessage rq = Transaction
                                            .createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX));
                                    iam.sendRequest(rq);
                                } catch (EfaException e) {
                                    Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR,
                                            String.format(
                                                    "TxRequestQueue error (efacloud URL: %s): "
                                                            + "%s",
                                                    TxRequestQueue.this.efaCloudUrl, e.toString()));
                                    Logger.log(e);
                                }
                            }
                        }

                        // parallel action toi the request path is to poll responses
                        if (!queueResponses.isEmpty()) {
                            TaskManager.RequestMessage rm = queueResponses.remove(0);
                            txr.handleTxResponse(rm);
                        }

                    }
                } catch (Exception e) {
                    Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR,
                            "TxRequestQueue Runtime exception: " + e.toString());
                    Logger.log(e);
                }
            }
        };
        queueTimer = new Timer();
        queueTimer.scheduleAtFixedRate(queueTimerTask, TIMER_PENDING_START_DELAY,
                TIMER_PENDING_TRIGGER_INTERVAL);
    }

    /**
     * Static transaction queue initialization. The three parameters are just taken once. After
     * creation, this procedure just returns the instance, as does the simplified getInstance()
     * Method.
     *
     * @param efaCloudUrl     URL of efaCoud Server
     * @param username        username for the efaDB server access
     * @param password        password for the efaDB server access
     * @param storageLocation the storageLocation directory without the File.separator ending to
     *                        store the retry transactions to.
     * @return singleton queue instance
     * @throws EfaException If the first nop operation returns an authentication failure.
     */
    public static TxRequestQueue getInstance(String efaCloudUrl, String username, String password,
                                             String storageLocation) throws EfaException {
        if (txq == null) txq = new TxRequestQueue(efaCloudUrl, username, password, storageLocation);
        return txq;
    }

    /**
     * Static transaction queue getter. The queue must be initialized first. If not initialized,
     * this returns null.
     *
     * @return singleton queue instance
     */
    public static TxRequestQueue getInstance() {
        return txq;
    }

    /**
     * Get the current transaction for the given ID
     *
     * @param txID       ID of the transaction to get
     * @param queueIndex index of queue to be searched.
     * @return transaction with this ID. If within the queue there is no such ID, null is returned.
     */
    protected Transaction getTxForID(int txID, int queueIndex) {
        Vector<Transaction> queue = queues.get(queueIndex);
        for (Transaction tx : queue)
            if (tx.ID == txID) return tx;
        return null;
    }

    /**
     * Tell, whether e a queue is stored on disc.
     *
     * @param queueIndex the queue, for which the property is requested
     * @return true, if the queue is stored on disc and false , if it resides in memory.
     */
    private boolean isPermanentQueue(int queueIndex) {
        return queueIndex == TX_DONE_QUEUE_INDEX || queueIndex == TX_FAILED_QUEUE_INDEX;
    }

    /**
     * Obtain a list of all IDs contained in the busy queue for logging and debugging purposes.
     *
     * @return the list of all IDs contained
     */
    protected java.lang.String listIDsContainedInBusy() {
        if (isPermanentQueue(TxRequestQueue.TX_BUSY_QUEUE_INDEX))
            readQueueFromFile(TxRequestQueue.TX_BUSY_QUEUE_INDEX);
        if (queues.get(TxRequestQueue.TX_BUSY_QUEUE_INDEX).isEmpty()) return "";
        StringBuilder txIDs = new StringBuilder();
        for (Transaction tx : queues.get(TxRequestQueue.TX_BUSY_QUEUE_INDEX))
            txIDs.append(tx.ID).append(", ");
        return txIDs.toString().substring(0, txIDs.length() - 2);
    }

    /**
     * Shift all or one transactions from source queue to destination queue and register the
     * respective transaction. If sourceQueueIndex == destinationQueueIndex, only the action is
     * registered. If the action is ACTION_TX_RETRY and the transaction is synchronous, the
     * transaction is moved to the failed queue in any case, not caring about the
     * destinationQueueIndex.
     *
     * @param sourceQueueIndex      index of source queue
     * @param destinationQueueIndex index of destination queue
     * @param action                action to register with the shift
     * @param txID                  ID of transaction to shift. Set 0 to shift all transactions of
     *                              the source queue.
     * @param maxNumber             Maximum number of transactions to shift. Set to 0 for all
     *                              transactions regardless of number.
     * @param releaseTxLock         If this is true, the transaction lock is released after the
     *                              shift procedure. This shall be set for all shifts into the
     *                              retry, done or failed queue.
     * @throws EfaException if the lock for the queue can not be obtained and is released by time
     *                      out.
     */
    protected void shiftTx(int sourceQueueIndex, int destinationQueueIndex, int action, int txID,
                           int maxNumber, boolean releaseTxLock) throws EfaException {
        Vector<Transaction> src = queues.get(sourceQueueIndex);
        Vector<Transaction> dest = queues.get(destinationQueueIndex);
        boolean registerActionOnly = sourceQueueIndex == destinationQueueIndex;
        // If a transaction has a lock, it will not be retried, but aborted. This is
        // to avoid, that the task waiting for completion is stuck.
        // try to get lock. This will exit with a release lock exception after lock timeout.
        //noinspection StatementWithEmptyBody
        while (refuseQueueLock(sourceQueueIndex) || (!registerActionOnly && refuseQueueLock(
                destinationQueueIndex)) || ((action == ACTION_TX_RETRY) && refuseQueueLock(
                TxRequestQueue.TX_FAILED_QUEUE_INDEX)))
            // wait
            ;

        if (isPermanentQueue(sourceQueueIndex)) readQueueFromFile(sourceQueueIndex);
        if (isPermanentQueue(destinationQueueIndex)) readQueueFromFile(destinationQueueIndex);
        // fix the count of elements to be shifted, before starting the shift. This will create
        // predictable results, if during the shift a transaction is appended.
        int chunkSize = src.size();
        if ((chunkSize > maxNumber) && (maxNumber > 0)) chunkSize = maxNumber;

        int i = 0;
        if (txID == 0) while (!src.isEmpty() && (i < chunkSize)) {
            Transaction tx = src.get(0);
            if (tx != null) {
                if (tx.lock && (action == ACTION_TX_RETRY)) {
                    registerAction(tx, ACTION_TX_ABORT);
                    if (tx.getResultCode() == 0) tx.setResultCode(tx.getCresultCode());
                    if (tx.getResultMessage().isEmpty())
                        tx.setResultMessage(tx.getCresultMessage());
                    src.remove(0);
                    readQueueFromFile(TX_FAILED_QUEUE_INDEX);
                    Vector<Transaction> failed = queues.get(TX_FAILED_QUEUE_INDEX);
                    failed.add(tx);
                    writeQueueToFile(TX_FAILED_QUEUE_INDEX);
                } else {
                    registerAction(tx, action);
                    if (!registerActionOnly) {
                        src.remove(0);
                        dest.add(tx);
                    }
                }
                if (tx.lock && releaseTxLock) tx.confirmExecution();
            }
            i++;
        }
        else {
            Transaction tx = getTxForID(txID, sourceQueueIndex);
            if (tx != null) {
                if (tx.lock && (action == ACTION_TX_RETRY)) {
                    src.remove(0);
                    registerAction(tx, ACTION_TX_ABORT);
                    readQueueFromFile(TX_FAILED_QUEUE_INDEX);
                    Vector<Transaction> failed = queues.get(TX_FAILED_QUEUE_INDEX);
                    failed.add(tx);
                    writeQueueToFile(TX_FAILED_QUEUE_INDEX);
                } else {
                    registerAction(tx, action);
                    if (!registerActionOnly) {
                        src.remove(0);
                        dest.add(tx);
                    }
                }
                if (tx.lock && releaseTxLock) tx.confirmExecution();
            }
        }
        // write result and release locks on queues
        if (isPermanentQueue(destinationQueueIndex)) writeQueueToFile(destinationQueueIndex);
        releaseQueueLock(sourceQueueIndex);
        releaseQueueLock(destinationQueueIndex);
        // The failed que had a lock when retry was triggered.
        if (action == ACTION_TX_RETRY) releaseQueueLock(TX_FAILED_QUEUE_INDEX);
    }

    /**
     * Register the container result with all busy transactions
     *
     * @param cresultCode    the result code for the container result
     * @param cresultMessage the result message for the container result
     */
    protected void registerContainerResult(int cresultCode, String cresultMessage) {
        Vector<Transaction> src = queues.get(TX_BUSY_QUEUE_INDEX);
        for (Transaction tx : src) {
            if (tx != null) {
                tx.setCresultCode(cresultCode);
                tx.setCresultMessage(cresultMessage);
            }
        }
    }

    /**
     * Register an action performed on the transaction
     *
     * @param tx     transaction to modify
     * @param action action to register
     */
    private void registerAction(Transaction tx, int action) {
        switch (action) {
            case ACTION_TX_SEND:
                tx.setSentAt(System.currentTimeMillis());
                break;
            case ACTION_TX_ABORT:
            case ACTION_TX_CLOSE:
                tx.setResultAt(System.currentTimeMillis());
                tx.setClosedAt(System.currentTimeMillis());
                txReceipts.add(new Transaction.TxReceipt(tx));
                break;
            case ACTION_TX_RETRY:
                tx.setSentAt(System.currentTimeMillis());
                tx.setRetries(tx.getRetries() + 1);
                break;
            case ACTION_TX_RESP_MISSING:
                tx.setResultAt(System.currentTimeMillis());
                tx.setResultCode(503);
                tx.setResultMessage("No server response in returned transaction response container.");
                tx.setClosedAt(System.currentTimeMillis());
                txReceipts.add(new Transaction.TxReceipt(tx));
                break;
            case ACTION_TX_CONTAINER_FAILED:
                tx.setResultAt(System.currentTimeMillis());
                tx.setResultCode(504);
                tx.setResultMessage("transaction response container failed.");
                tx.setClosedAt(System.currentTimeMillis());
                txReceipts.add(new Transaction.TxReceipt(tx));
                break;
        }
    }

    /**
     * Get all transaction receipts for this sender and the given filter to monitor results.
     *
     * @param txID       lowest transaction ID to use.
     * @param sender     sender for which the filter shall apply. Must not be null.
     * @param successful filter on successful transactions
     * @param failed     filter on failed transactions (set failed & successful to get all)
     * @return the transaction receipts filtered.
     */
    public ArrayList<Transaction.TxReceipt> getReceiptsFromIDonwards(int txID, Object sender,
                                                                     boolean successful,
                                                                     boolean failed) {
        ArrayList<Transaction.TxReceipt> receipts = new ArrayList<>();
        for (Transaction.TxReceipt txReceipt : txReceipts)
            if ((txReceipt.ID >= txID) && (txReceipt.sender == sender))
                if ((failed && (txReceipt.resultCode >= 400)) || (successful && (txReceipt.resultCode < 400)))
                    receipts.add(txReceipt);
        return receipts;
    }

    /**
     * Clear the receipts queue, remove those older than 3 days. Should be done regularly.
     */
    private void clearReceipts() {
        long minClosedAt = System.currentTimeMillis() - 3 * 24 * 3600000;
        int pos = 0;
        while (pos < txReceipts.size()) {
            if (txReceipts.get(pos).closedAt < minClosedAt) txReceipts.remove(pos);
            else pos++;
        }
    }

    /**
     * get a lock for a queue, i.e. prevent all others to use it.
     *
     * @param queueIndex index of queue, for which the lock shall be provided
     */
    private boolean refuseQueueLock(int queueIndex) throws EfaException {
        long now = System.currentTimeMillis();
        if (locks[queueIndex] == -1) {
            locks[queueIndex] = now;
            return false;
        } else {
            if ((now - locks[queueIndex]) > QUEUE_LOCK_TIMEOUT) {
                releaseQueueLock(queueIndex);
                throw new EfaException(Logger.MSG_DATA_GENERICEXCEPTION, String.format(
                        "EfaCloud transaction queue timeout for %s queue. Forced release.",
                        TX_QUEUE_NAMES[queueIndex]), null);
            } else return true;
        }
    }

    /**
     * release a lock for a queue, i.e. allow others to use it.
     *
     * @param queueIndex index of queue, for which the lock shall be released
     */
    private void releaseQueueLock(int queueIndex) {
        locks[queueIndex] = -1;
    }

    /**
     * Write a queue to the storage. Transactions will be written and deleted from the respective
     * queue after writing. You must obtain the queue lock first and release if afterwards, because
     * this procedure does not manipulate the locks.
     *
     * @param queueIndex queue index of the queue to be written.
     */
    private void writeQueueToFile(int queueIndex) {

        String qString = "";
        Vector<Transaction> qw = queues.get(queueIndex);
        // If the queue is empty the file is cleared by writing an empty String
        if (!qw.isEmpty()) {
            // all queues are First in first out, i. e. oldest first. Remove oldest
            long now = System.currentTimeMillis();
            Transaction txToCheck = qw.firstElement();
            boolean tooOld;
            if (TX_PERMANENT_QUEUE_DROP_DAYS[queueIndex] > 0) do {
                long ageInDays = (now - txToCheck.getResultAt()) / ONE_DAY_MILLIS;
                tooOld = (ageInDays > TX_PERMANENT_QUEUE_DROP_DAYS[queueIndex]);
                if (tooOld) qw.remove(0);
                if (!qw.isEmpty()) txToCheck = qw.firstElement();
            } while (!qw.isEmpty() && tooOld);
            // Remove extra count
            while (qw.size() > TX_PERMANENT_QUEUE_DROP_COUNT[queueIndex]) qw.remove(0);
            // Prepare String to write
            StringBuilder txFileContents = new StringBuilder();
            for (Transaction tx : qw)
                txFileContents.append(tx.getFullTxString())
                        .append(Transaction.MESSAGE_SEPARATOR_STRING);
            qString = (qw.size() == 0) ? "" : txFileContents.substring(0,
                    txFileContents.length() - Transaction.MESSAGE_SEPARATOR_STRING.length());
        }
        // Write queue
        boolean written = TextResource.writeContents(txFilePath[queueIndex], qString, false);
        if (!written) Logger.log(Logger.ERROR, Logger.MSG_FILE_WRITETHREAD_ERROR, String.format(
                        "TxRequestQueue.writeQueueToFile: failed to write queue %s to disc.",
                        txFilePath[queueIndex]));
    }

    /**
     * <p>Read the permanently stored transactions from the storage. The respective queue will be
     * cleared immediately, then filled with the files contents and the files content will be
     * deleted from the storage after reading.</p><p> YOU MUST OBTAIN THE QUEUE LOCK BEFORE AND
     * RELEASE IT AFTERWARDS, because this procedure does not manipulate the locks.</p>
     *
     * @param queueIndex queue index of the queue to be read.
     */
    private void readQueueFromFile(int queueIndex) {
        queues.get(queueIndex).clear();
        File txFile = new File(txFilePath[queueIndex]);
        if (!txFile.exists()) return;
        String txFileContents = TextResource.getContents(txFile, "UTF-8");
        if (txFileContents == null || txFileContents.length() == 0) return;
        String[] txsRead = txFileContents.split(Transaction.MESSAGE_SEPARATOR_STRING_REGEX);
        for (String txRead : txsRead)
            if (txRead.length() > 1) {
                Transaction tx = Transaction.parseFullTxString(txRead);
                queues.get(queueIndex).add(tx);
            }
        // Remove contents from queue file.
        boolean written = TextResource.writeContents(txFilePath[queueIndex], "", false);
        if (!written) Logger.log(Logger.ERROR, Logger.MSG_FILE_WRITETHREAD_ERROR, String.format(
                        "TxRequestQueue.writeQueueToFile: failed to clear queue %s an disc.",
                        txFilePath[queueIndex]));
    }

    /**
     * Return the size of the busy queue
     *
     * @return size of the busy queue
     */
    protected int getBusyQueueSize() {
        return queues.get(TxRequestQueue.TX_BUSY_QUEUE_INDEX).size();
    }

    /**
     * Get a new transaction ID and increment the counter.
     *
     * @return the new transaction ID
     */
    protected static int getTxId() {
        txID++;
        return txID;
    }

    /**
     * Get a new transaction container ID and increment the counter.
     *
     * @return the new transaction container ID
     */
    protected static int getTxcId() {
        txcID++;
        return txcID;
    }

    /**
     * Create a new transaction and append it to the pending queue.
     *
     * @param sender    the sender of the transaction. Needed for result statistics with
     *                  getReceiptsFromIDonwards()
     * @param type      type of message to be appended
     * @param tablename tablename for transaction
     * @param record    record for transaction
     * @return the ID of the appended transaction
     */
    protected int appendTransactionPending(Object sender, String type, String tablename,
                                           String... record) {
        Transaction tx = new Transaction(-1, type, tablename, record);
        tx.sender = sender;
        queues.get(TX_PENDING_QUEUE_INDEX).add(tx);
        return tx.ID;
    }

    /**
     * Append a transaction to the pending queue.
     */
    public void appendTransactionPending(Transaction tx) {
        queues.get(TX_PENDING_QUEUE_INDEX).add(tx);
    }

    @Override
    public void sendRequest(TaskManager.RequestMessage request) {
        // This is the callback from the InternetAccessManager with the access result.
        // pass the results also through a queue, to make sure, a fast response can not overtake
        // an earlier one which is still be processed.
        queueResponses.add(request);
    }

}
