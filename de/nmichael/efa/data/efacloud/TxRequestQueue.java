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

import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.gui.EfaCloudConfigDialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>The efaCloud transaction request queue manager.</p>
 *
 * <p>Transaction queues are used to handle the communication with the efaCloud server.
 * All transactions are first locally executed and then appended to a transactions pending queue. From that queue they
 * are forwarded to the efacloud server via the InternetAccessManager. The transactions-pending queue is checked three
 * times a second. And all pending transactions (max. 30) are packaged into a single transaction container.</p>
 * <p>The protocol is meant to be used in a serial manner, i. e. no further client request
 * container is issued as long as another one is open and neither completed nor timed out. The transactions of a
 * transaction container waiting for a server response are called the “busy transactions”. When the server response is
 * received, the busy transactions are moved to the transactions done, or depending on the response code, to the retry
 * or failed queues.</p>
 * <p>Timeout and retry</p>
 * <p>A transaction container times out at the client side after 30 seconds without a response. All
 * messages of that container are transferred into a transactions-retry queue. Retries will be triggered all 20 minutes
 * and upon each application start. Upon the retry trigger all transactions from the retry queue are moved back to the
 * pending-transactions queue.</p>
 * <p>In summary there are the following transaction queues:</p>
 *     <ol><li> transactions synch (in-memory)
 *     </li><li> transactions pending (in-memory)
 *     </li><li> transactions busy (in-memory)
 *     </li><li> transactions done (locally on disc)
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

    // ms with which the queue timer starts processing requests
    public static final int QUEUE_TIMER_START_DELAY = 1000;
    // ms after which the queue timer processes the next set of requests
    public static final int QUEUE_TIMER_TRIGGER_INTERVAL = 300;

    // ms with which the watchdog timer starts processing requests
    public static final int WATCHDOG_TIMER_START_DELAY = 120000;
    // ms after which the queue timer processes the next set of requests
    public static final int WATCHDOG_TIMER_TRIGGER_INTERVAL = 600000;

    // every SYNCH_PERIOD the client checks for updates at the server side.
    // The update period MUST be at least 5 times the InternetAccessManager timeout.
    // The synchronisation start delay is one SYNCH_PERIOD
    public static final int SYNCH_PERIOD = 600000; // = 600 seconds = 10 minutes
    // If a transaction is busy since more than the RETRY_AFTER_MILLISECONDS period
    // issue a new internet access request.
    public static final int RETRY_PERIOD = 600000; // = 60 seconds = 1 minute

    // timeout for holding a queue locked for manipulation.
    public static final long QUEUE_LOCK_TIMEOUT = 1000;
    // Maximum number of transactions shifted into the pending queue, i. e. of transactions per
    // internet access request. If the internet access is blocked, this will pile upt internet
    // access requests rather than transactions in the pending transactions queue.
    public static final int PENDING_QUEUE_MAX_SHIFT_SIZE = 10;

    // Transaction queue indices and names.
    public static final int TX_SYNCH_QUEUE_INDEX = 0;
    public static final int TX_PENDING_QUEUE_INDEX = 1;
    public static final int TX_BUSY_QUEUE_INDEX = 2;
    public static final int TX_DONE_QUEUE_INDEX = 3;
    public static final int TX_FAILED_QUEUE_INDEX = 4;
    public static final int TX_DROPPED_QUEUE_INDEX = 5;
    public static final String[] TX_QUEUE_NAMES = new String[]{"txFix", "txPending", "txBusy", "txDone", "txFailed",
            "txDropped"};
    public static final int TX_QUEUE_COUNT = TX_QUEUE_NAMES.length;

    // Queue shift actions codes to trigger the shift related status modification.
    public static final int ACTION_TX_SEND = 100;
    public static final int ACTION_TX_RETRY = 101;
    public static final int ACTION_TX_ABORT = 102;
    public static final int ACTION_TX_CLOSE = 103;
    public static final int ACTION_TX_RESP_MISSING = 104;
    public static final int ACTION_TX_CONTAINER_FAILED = 105;
    public static final int ACTION_TX_STOP = 106;
    public static final int ACTION_TX_PAUSE = 107;
    public static final int ACTION_TX_MOVE = 108;    // Used to move a transaction from Synch to Pending.

    // TxQueue state machine change request constants
    public static final int RQ_QUEUE_KEEP_STATE = 0;
    public static final int RQ_QUEUE_DEACTIVATE = 1;
    public static final int RQ_QUEUE_AUTHENTICATE = 2;
    public static final int RQ_QUEUE_PAUSE = 3;
    public static final int RQ_QUEUE_RESUME = 4;
    public static final int RQ_QUEUE_START = 4;    // also 4, no difference between resume after pause and starting.
    public static final int RQ_QUEUE_START_SYNCH_DOWNLOAD = 5;   // Manual synchronization request
    public static final int RQ_QUEUE_START_SYNCH_UPLOAD = 6;     // Manual synchronization request
    public static final int RQ_QUEUE_START_AUTO_SYNCH = 7;       // Automatic periodic download  synchronization
    public static final int RQ_QUEUE_STOP_SYNCH = 8;
    // TxQueue state machine state constants
    public static final int QUEUE_IS_STOPPED = 11;
    public static final int QUEUE_IS_AUTHENTICATING = 12;
    public static final int QUEUE_IS_PAUSED = 130;
    public static final int QUEUE_IS_DISCONNECTED = 131;
    public static final int QUEUE_IS_WORKING = 14;
    public static final int QUEUE_IS_SYNCHING = 15;
    public static final HashMap<Integer, String> QUEUE_STATE = new HashMap<>();

    static {
        QUEUE_STATE.put(QUEUE_IS_STOPPED, "STOPPED");
        QUEUE_STATE.put(QUEUE_IS_AUTHENTICATING, "AUTHENTICATING");
        QUEUE_STATE.put(QUEUE_IS_PAUSED, "PAUSED");
        QUEUE_STATE.put(QUEUE_IS_DISCONNECTED, "DISCONNECTED");
        QUEUE_STATE.put(QUEUE_IS_WORKING, "WORKING");
        QUEUE_STATE.put(QUEUE_IS_SYNCHING, "SYNCHING");
    }

    private static int txID;                  // the last used transaction ID
    private static int txcID;                 // the last used transaction container ID
    private static TxRequestQueue txq = null; // the static singleton instance of this class
    private static TxResponseHandler txr;     // The handler to hand responses over to

    // the request queues and their size, locks and file paths.
    private final ArrayList<Vector<Transaction>> queues = new ArrayList<>();
    private final long[] locks = new long[TX_QUEUE_COUNT];
    private final String[] txFilePath = new String[TX_QUEUE_COUNT];
    String apiActivityLogFilePath;
    String apiErrorLogFilePath;
    String synchLogFilePath;
    String stateChangeLogFilePath;

    // The response queue
    private final ArrayList<TaskManager.RequestMessage> queueResponses = new ArrayList<>();

    // The queue state machine parameters
    private int state = QUEUE_IS_STOPPED;
    private int stateChangeRequest = RQ_QUEUE_KEEP_STATE;
    SynchControl synchControl;

    // poll timer, internet access manager and connection settings
    private Timer queueTimer;
    private long pollsCount;                   // a counter incrementing on each queue poll cycle
    private long watchdogPreviousPollsCount;   // the pollsCount value at the previous watchdog trigger event.
    private final InternetAccessManager iam;
    String storageLocationQueues;
    String storageLocationLogs;
    String efaCloudUrl;
    String username;
    String credentials;

    /**
     * Get a new transaction ID and increment the counter.
     *
     * @return the new transaction ID
     */
    static int getTxId() {
        txID++;
        return txID;
    }

    /**
     * Get a new transaction container ID and increment the counter.
     *
     * @return the new transaction container ID
     */
    static int getTxcId() {
        txcID++;
        return txcID;
    }

    /**
     * Static transaction queue initialization. The three parameters are just taken once. After creation, this procedure
     * just returns the instance, as does the simplified getInstance() Method.
     *
     * @param efaCloudUrl     URL of efaCoud Server
     * @param username        username for the efaDB server access
     * @param password        password for the efaDB server access
     * @param storageLocation the storageLocation directory without the File.separator ending to store the retry
     *                        transactions to.
     * @return singleton queue instance
     * @throws EfaException If the first nop operation returns an authentication failure.
     */
    public static TxRequestQueue getInstance(String efaCloudUrl, String username, String password,
                                             String storageLocation) throws
            EfaException {
        if (txq == null)
            txq = new TxRequestQueue(efaCloudUrl, username, password, storageLocation);
        return txq;
    }

    /**
     * Static transaction queue getter. The queue must be initialized first. If not initialized, this returns null.
     *
     * @return singleton queue instance
     */
    public static TxRequestQueue getInstance() {
        return txq;
    }

    /**
     * Simple getter
     *
     * @return the state of operation, e. g. TX_QUEUE_WORKING
     */
    public int getState() {
        return state;
    }

    /**
     * Simple getter
     *
     * @return the size of the respective queue
     */
    public int getQueueSize(int queueIndex) {
        return queues.get(queueIndex).size();
    }

    /**
     * Append a log message to the synch log.
     *
     * @param error the error which shall be logged. Note that API errors are not logged in the standard way, because
     *              such logging will create a message which itself turn can fail which creates another message asf
     *              ending possibly in en endless loop.
     */
    void logError(String error) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = format.format(new Date()) + " [API-Error]: ";
        TextResource.writeContents(TxRequestQueue.getInstance().apiErrorLogFilePath, dateString + error, true);
    }

    /**
     * Initialize all file paths, URL, and queues. Part of condtructor but split into separae function to improve code
     * readability.
     *
     * @param efaCloudUrl     efaCloud server URL. Note the special feaure to allow for all certificates when using
     *                        httpx as protocol identifier instead of https.
     * @param storageLocation the storageLocation directory without the File.separator ending to store the retry
     *                        transactions to.
     */
    private void initPathsAndLogs(String efaCloudUrl, String storageLocation) {
        // initialize log directories.
        storageLocationQueues = storageLocation;
        String storageLocationRoot = (storageLocation.endsWith(File.separator)) ? storageLocation
                .substring(0, storageLocation.lastIndexOf(File.separator)) : storageLocation;
        String prName = storageLocationRoot.substring(storageLocationRoot.lastIndexOf(File.separator) + 1);
        storageLocationRoot = storageLocationRoot.substring(0, storageLocationRoot.lastIndexOf(File.separator));
        storageLocationRoot = storageLocationRoot.substring(0, storageLocationRoot.lastIndexOf(File.separator));
        storageLocationLogs = storageLocationRoot + File.separator + "log";
        String efacloudQueuesDir = storageLocationLogs + File.separator + prName + File.separator + "efacloudqueues";
        String efacloudLogDir = storageLocationLogs + File.separator + prName + File.separator + "efacloudlogs";
        //noinspection ResultOfMethodCallIgnored
        new File(efacloudQueuesDir).mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File(efacloudLogDir).mkdirs();
        // initialize logs.
        synchLogFilePath = efacloudLogDir + File.separator + "efacloudSynch.log";
        stateChangeLogFilePath = efacloudLogDir + File.separator + "efacloudStates.log";
        apiActivityLogFilePath = efacloudLogDir + File.separator + "efacloudApiActivity.log";
        apiErrorLogFilePath = efacloudLogDir + File.separator + "efacloudApiErrors.log";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = format.format(new Date()) + " [all]: LOG STARTING\n";
        TextResource.writeContents(synchLogFilePath, dateString, false);
        TextResource.writeContents(stateChangeLogFilePath, dateString, false);
        TextResource.writeContents(apiActivityLogFilePath, dateString, false);
        TextResource.writeContents(apiErrorLogFilePath, dateString, false);
        // This shall be uncommented to trigger debug logging in the iam.
        // iam.debugFilePath = storageLocationQueues + "iam.txcs";

        // check and cleanse the URL
        if (efaCloudUrl.startsWith("httpx")) {
            iam.setAllowAllCertificates(true);
            this.efaCloudUrl = "https" + efaCloudUrl.substring("httpx".length());
        } else
            this.efaCloudUrl = efaCloudUrl;
        if (!this.efaCloudUrl.endsWith(URL_API_LOCATION)) {
            if (this.efaCloudUrl.endsWith("/"))
                this.efaCloudUrl = this.efaCloudUrl.substring(0, this.efaCloudUrl.length() - 1);
            this.efaCloudUrl += URL_API_LOCATION;
        }

        // initialize the queues
        txID = 42;
        txcID = 42;
        for (int i = 0; i < TX_QUEUE_COUNT; i++) {
            Vector<Transaction> queue = new Vector<>();
            queues.add(queue);
            txFilePath[i] = efacloudQueuesDir + File.separator + TX_QUEUE_NAMES[i] + ".txs";
            // permanent queues shall be read from file upon initialization, but only then and except the busy queue.
            if (isPermanentQueue(i))
                if (i != TX_BUSY_QUEUE_INDEX) {
                    readQueueFromFile(i);
                    // ensure that the next transaction ID is greater than all of those read from previous queues
                    for (Transaction tx : queues.get(i))
                        if (tx.ID > txID)
                            txID = tx.ID + 1;
                } else
                    // clear also the disk contents of the busy queue, because these transactions are no more open.
                    TextResource.writeContents(txFilePath[i], "", false);
            releaseQueueLock(i);
        }
    }

    /**
     * Private constructor to run the queue as singleton class. There must not be more than one queue per client
     * active.
     *
     * @param efaCloudUrl     efaCloud server URL. Note the special feaure to allow for all certificates when using
     *                        httpx as protocol identifier instead of https.
     * @param username        efaCloud server username
     * @param password        efaCloud server password
     * @param storageLocation the storageLocation directory without the File.separator ending to store the retry
     *                        transactions to.
     */
    private TxRequestQueue(String efaCloudUrl, String username, String password, String storageLocation) {

        // combine the credentials to a transaction container prefix.
        this.username = username;
        this.credentials =
                username + TX_REQ_DELIMITER + CsvCodec.encodeElement(password, TX_REQ_DELIMITER, TX_QUOTATION) +
                        TX_REQ_DELIMITER;

        // initialize the internet access manager
        iam = InternetAccessManager.getInstance();

        // initialize all file paths and queues
        initPathsAndLogs(efaCloudUrl, storageLocation);

        // initialize the response and synchronization handler
        txr = new TxResponseHandler(this);
        synchControl = new SynchControl(this);

        // start queue timer and watchdog
        pollsCount = 0L;
        startQueueTimer();
        watchdogPreviousPollsCount = 0L;
        startWatchDog();
        requestStateChange(RQ_QUEUE_AUTHENTICATE);

    }

    /**
     * <p>Pause the queue execution.</p><p>This will happen either on an internet connection timeout or a manual pause
     * request. Removes all read requests from the busy and pending queues in normal state. If in synchronisation state,
     * it moves all keyfixing confirmations from the synch queue to the pending queue and terminates the synchronisation
     * by changing the state to 'paused'.</p>
     */
    private void pauseQueue() {
        state = QUEUE_IS_PAUSED;
        stateChangeRequest = RQ_QUEUE_KEEP_STATE;
        shiftTx(TX_PENDING_QUEUE_INDEX, TX_DROPPED_QUEUE_INDEX, ACTION_TX_PAUSE, 0, 0);
        shiftTx(TX_SYNCH_QUEUE_INDEX, TX_DROPPED_QUEUE_INDEX, ACTION_TX_PAUSE, 0, 0);
        shiftTx(TX_SYNCH_QUEUE_INDEX, TX_PENDING_QUEUE_INDEX, ACTION_TX_MOVE, 0, 0);
        txq.synchControl.logMessage("Queue activity paused", "@all", null, true);
    }

    /**
     * <p>Stop the queue execution.</p><p>This will always be manually triggered. Removes all but keyfixing
     * confirmation requests from the busy, pending and synching queues. Moves the keyfixing confirmations form the
     * synch to the pending queue. Terminates the synchronisation by changing the state to 'stopped'.</p>
     */
    private void stopQueue() {
        state = QUEUE_IS_STOPPED;
        stateChangeRequest = RQ_QUEUE_KEEP_STATE;
        shiftTx(TX_PENDING_QUEUE_INDEX, TX_DROPPED_QUEUE_INDEX, ACTION_TX_STOP, 0, 0);
        shiftTx(TX_SYNCH_QUEUE_INDEX, TX_DROPPED_QUEUE_INDEX, ACTION_TX_STOP, 0, 0);
        shiftTx(TX_SYNCH_QUEUE_INDEX, TX_PENDING_QUEUE_INDEX, ACTION_TX_MOVE, 0, 0);
        txq.synchControl.logMessage("Queue activity stopped", "@all", null, true);
    }

    /**
     * Check the busy queue for a timeout event. If so, increment the retry counter of all busy transactions, remove
     * read transactions from the queue and switch from Synch state back to normal, i. e. 'disconnected'. Finally issue
     * a retry request.
     */
    private void checkForAndHandleTimeout() {
        long now = System.currentTimeMillis();
        long firstTxSentAt = queues.get(TX_BUSY_QUEUE_INDEX).firstElement().getSentAt();
        if (now - firstTxSentAt > RETRY_PERIOD) {
            // Increase the retry counter
            shiftTx(TX_BUSY_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, ACTION_TX_RETRY, 0, 0);
            // Remove read activities from the queues
            pauseQueue();
            // Indicate the connection loss. This has no effect on the queue handling.
            state = QUEUE_IS_DISCONNECTED;
            TaskManager.RequestMessage rq = Transaction.createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX));
            iam.sendRequest(rq);
        }
    }

    /**
     * Start the queue timer, triggering all efacloud client to server transactions.
     */
    private void startQueueTimer() {
        // initialize the queue poll timer.
        TimerTask queueTimerTask = new TimerTask() {
            public void run() {
                pollsCount++;
                long polltime = System.currentTimeMillis();
                // if a timer task encounters an unhandled exception, it stops and the timer dies.
                // that must be avoided at any case.

                // ============== handle responses ===============
                if (!queueResponses.isEmpty()) {
                    try {
                        TaskManager.RequestMessage rm = queueResponses.remove(0);
                        txr.handleTxcResponse(rm);
                    } catch (Exception e) {
                        Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR, International
                                .getString("Ausnahmefehler bei der Behandlung einer Efacloud-Serverantwort: ") +
                                e.toString());
                        Logger.log(e);
                    }
                }

                // ============== handle state change ===============
                switch (stateChangeRequest) {
                    case RQ_QUEUE_DEACTIVATE:
                        stopQueue();
                        EfaCloudConfigDialog.deactivateEfacloud();
                        break;
                    case RQ_QUEUE_AUTHENTICATE:
                        state = QUEUE_IS_AUTHENTICATING;
                        stateChangeRequest = RQ_QUEUE_KEEP_STATE;
                        // start with first transaction a "nop" transaction to verify the credentials.
                        appendTransaction(TX_PENDING_QUEUE_INDEX, "nop", "", "sleep;0");
                        // add a second synch @all transaction to check whether the tables have been initialized. Use
                        // a filter which returns a count close to 0 for all tables to save server side performance.
                        long now = System.currentTimeMillis();
                        appendTransaction(TX_PENDING_QUEUE_INDEX, "synch", "@all", "LastModified;" + now, "?;>");
                        break;
                    case RQ_QUEUE_PAUSE:
                        pauseQueue();
                        break;
                    case RQ_QUEUE_RESUME:
                        state = QUEUE_IS_WORKING;
                        stateChangeRequest = RQ_QUEUE_KEEP_STATE;
                        txq.synchControl.logMessage("Queue activity resumed", "@all", null, true);
                        break;
                    case RQ_QUEUE_START_SYNCH_DOWNLOAD:
                    case RQ_QUEUE_START_SYNCH_UPLOAD:
                    case RQ_QUEUE_START_AUTO_SYNCH:
                        if ((queues.get(TX_PENDING_QUEUE_INDEX).size() == 0) &&
                                (queues.get(TX_BUSY_QUEUE_INDEX).size() == 0)) {
                            state = QUEUE_IS_SYNCHING;
                            txq.synchControl.startSynchProcess(stateChangeRequest);
                            stateChangeRequest = RQ_QUEUE_KEEP_STATE;
                        }
                        break;
                    case RQ_QUEUE_STOP_SYNCH:          // End of synchronisation process, no manual option.
                        if (queues.get(TX_SYNCH_QUEUE_INDEX).size() == 0) {
                            state = QUEUE_IS_WORKING;
                            stateChangeRequest = RQ_QUEUE_KEEP_STATE;
                            txq.synchControl.logMessage("Synchronisation transactions completed", "@all", null, false);
                            txq.synchControl.logMessage("Synchronisation completed", "@all", null, true);
                        }
                        break;
                }

                // skip any further activity if paused or stopped.
                if ((txq.state == TxRequestQueue.QUEUE_IS_PAUSED) || (txq.state == TxRequestQueue.QUEUE_IS_STOPPED))
                    return;

                // ============== handle requests ===============
                try {
                    // handle synchronization with first priority, if started. It will then not be interrupted.
                    if (state == QUEUE_IS_SYNCHING) {
                        // check busy requests for age. Initiate a retry, if needed. The retry ends the
                        // synchronisation state.
                        if (queues.get(TX_BUSY_QUEUE_INDEX).size() > 0)
                            checkForAndHandleTimeout();
                        else {
                            shiftTx(TX_SYNCH_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, ACTION_TX_SEND, 0,
                                    PENDING_QUEUE_MAX_SHIFT_SIZE);
                            TaskManager.RequestMessage rq = Transaction
                                    .createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX));
                            iam.sendRequest(rq);
                        }
                    }
                    // normal poll of queue
                    else {
                        // check busy requests for age. Initiate a retry, if needed. The retry is
                        // always triggered on all transactions of the busy queue.
                        if (queues.get(TX_BUSY_QUEUE_INDEX).size() > 0)
                            checkForAndHandleTimeout();
                            // handle new pending requests, if currently no request is busy
                        else if (queues.get(TX_PENDING_QUEUE_INDEX).size() > 0) {
                            shiftTx(TX_PENDING_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, ACTION_TX_SEND, 0,
                                    PENDING_QUEUE_MAX_SHIFT_SIZE);
                            // Remove te connection loss indication when the busy queue was cleared.
                            if (state == QUEUE_IS_DISCONNECTED)
                                state = QUEUE_IS_WORKING;
                            // read the transactions to use them
                            TaskManager.RequestMessage rq = Transaction
                                    .createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX));
                            iam.sendRequest(rq);
                        }
                        // check whether to start synchronisation, if neither busy nor pending requests are there
                        else if (polltime - synchControl.timeOfLastSynch > SYNCH_PERIOD) {
                            // The synch transactions queue is never stored, so it needs not to be read from file.
                            queues.get(TX_SYNCH_QUEUE_INDEX).clear();
                            requestStateChange(RQ_QUEUE_START_AUTO_SYNCH);
                        }
                    }

                } catch (Exception e) {
                    Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR, International
                            .getString("Ausnahmefehler bei der Behandlung einer Efacloud-Serveranfrage: ") +
                            e.toString());
                    Logger.log(e);
                }
            }
        };
        queueTimer = new Timer();
        queueTimer.scheduleAtFixedRate(queueTimerTask, QUEUE_TIMER_START_DELAY, QUEUE_TIMER_TRIGGER_INTERVAL);
    }

    /**
     * <p><b>ONLY TO BE CALLED BY EFACLOUD DEACTIVATION</b></p><p>Terminate all efacloud activities. This stops the
     * efaCloud poll timer and watchdog, the internet access manager's activities and drops the link to the singleton
     * TxRequestQueue instance.</p>
     */
    public void terminate() {
        queueTimer.cancel();
        iam.terminate();
        txq = null;
    }

    /**
     * Start the watchdog. It will check every 10 minutes whether the queueTimer is still running by comparing the last
     * polltimer count with the current one.. If
     */
    private void startWatchDog() {
        TimerTask watchdogTask = new TimerTask() {
            @Override
            public void run() {
                if (txq.watchdogPreviousPollsCount == txq.pollsCount) {
                    Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_WARNING, International
                            .getString("Efacloud watchdog hat angeschlagen. Der Timer wurde neu gestartet"));
                    queueTimer.cancel();
                    startQueueTimer();
                }
                txq.watchdogPreviousPollsCount = txq.pollsCount;
            }
        };
        Timer watchDog = new Timer();
        watchDog.scheduleAtFixedRate(watchdogTask, WATCHDOG_TIMER_START_DELAY, WATCHDOG_TIMER_TRIGGER_INTERVAL);
    }

    /**
     * Request a state change of the transaction queue. This is only setting the request flag. The change is always
     * performed during a poll cycle, to avoid changes at an intermediate state. See constants. Request flags are reset
     * after the request wa executed. If another state change request is pending, this will do nothing.
     *
     * @param stateChangeRequest one of TX_QUEUE_STOP, TX_QUEUE_PAUSE, TX_QUEUE_RESUME, TX_QUEUE_START_SYNCH,
     *                           TX_QUEUE_STOP_SYNCH
     */
    public synchronized void requestStateChange(int stateChangeRequest) {
        if (this.stateChangeRequest == RQ_QUEUE_KEEP_STATE)
            this.stateChangeRequest = stateChangeRequest;
    }

    /**
     * Get the current transaction for the given ID
     *
     * @param txID         ID of the transaction to get
     * @param queueIndex   index of queue to be searched.
     * @param getQueueLock Set true, if the lock of the queue was already obtained.
     * @return transaction with this ID. If within the queue there is no such ID, null is returned.
     */
    Transaction getTxForID(int txID, int queueIndex, boolean getQueueLock) {
        // try to get the queues lock. This will exit with a release lock exception after lock timeout.
        if (getQueueLock)
            //noinspection StatementWithEmptyBody
            while (refuseQueueLock(queueIndex))
                // wait
                ;
        Transaction txForID = null;
        for (Transaction tx : queues.get(queueIndex))
            if (tx.ID == txID)
                txForID = tx;
        if (getQueueLock)
            releaseQueueLock(queueIndex);
        return txForID;
    }

    /**
     * Tell, whether e a queue is stored on disk. Stored on disk are the queues for transactions pending, busy or
     * failed. In memory queues are those for synchronization, dropped, or done transactions. They may be lost when
     * rebooting.
     *
     * @param queueIndex the queue, for which the property is requested
     * @return true, if the queue is stored on disc and false , if it resides in memory.
     */
    private boolean isPermanentQueue(int queueIndex) {
        return queueIndex == TX_PENDING_QUEUE_INDEX || queueIndex == TX_BUSY_QUEUE_INDEX ||
                queueIndex == TX_FAILED_QUEUE_INDEX;
    }

    /**
     * Shift all or one transactions from source queue to destination queue and register the respective transaction. If
     * sourceQueueIndex == destinationQueueIndex, only the action is registered. If the action is Pause, insert, update
     * and delete transactions are not move, but only the action recorded.
     *
     * @param sourceQueueIndex      index of source queue
     * @param destinationQueueIndex index of destination queue
     * @param action                action to register with the shift
     * @param txID                  ID of transaction to shift. Set 0 to shift all transactions of the source queue.
     * @param maxNumber             Maximum number of transactions to shift. Set to 0 for all transactions regardless of
     *                              number.
     */
    void shiftTx(int sourceQueueIndex, int destinationQueueIndex, int action, int txID, int maxNumber) {

        Vector<Transaction> src = queues.get(sourceQueueIndex);
        Vector<Transaction> dest = queues.get(destinationQueueIndex);
        boolean registerActionOnly = (sourceQueueIndex == destinationQueueIndex);
        // try to get the queues lock. This will exit with a release lock exception after lock timeout.
        //noinspection StatementWithEmptyBody
        while (refuseQueueLock(sourceQueueIndex))
            // wait
            ;
        if (!registerActionOnly)
            //noinspection StatementWithEmptyBody
            while (refuseQueueLock(destinationQueueIndex))
                // wait
                ;

        // fix the count of elements to be shifted, before starting the shift. This will create
        // predictable results, if during the shift a transaction is appended.
        int chunkSize = src.size();
        if ((chunkSize > maxNumber) && (maxNumber > 0))
            chunkSize = maxNumber;

        int i = 0;
        while (((txID > 0) && (i == 0))  // single transaction shift
                || ((txID == 0) && !src.isEmpty() && (i < chunkSize))) {
            Transaction tx = (txID > 0) ? getTxForID(txID, sourceQueueIndex, false) : src.get(0);
            if (tx != null) {
                registerAction(tx, action);
                if (!registerActionOnly) {
                    // Before moving the transaction check whether this is pausing or stopping the queue. When
                    // pausing write actions and key fixing confirmations shall stay, when stopping only key fixing
                    // confirmations .
                    boolean isKeyFixingConfirmation = (tx.type.equalsIgnoreCase("keyfixing") && tx.hasRecord());
                    boolean isWriteAction = (tx.type.equalsIgnoreCase("insert") || tx.type.equalsIgnoreCase("update") ||
                            tx.type.equalsIgnoreCase("delete"));
                    boolean keepOnPause = (action == ACTION_TX_PAUSE) &&
                            ((sourceQueueIndex == TX_SYNCH_QUEUE_INDEX) ? isKeyFixingConfirmation : (
                                    isKeyFixingConfirmation || isWriteAction));
                    boolean keepOnStop = (action == ACTION_TX_STOP) && isKeyFixingConfirmation;
                    if (!keepOnPause && !keepOnStop) {
                        if (destinationQueueIndex == TxRequestQueue.TX_DROPPED_QUEUE_INDEX)
                            tx.logMessage("drop");
                        dest.add(tx);
                        src.remove(tx);
                    }
                }
            }
            i++;
        }
        if (isPermanentQueue(sourceQueueIndex))
            writeQueueToFile(sourceQueueIndex);
        if (isPermanentQueue(destinationQueueIndex))
            writeQueueToFile(destinationQueueIndex);
        releaseQueueLock(sourceQueueIndex);
        releaseQueueLock(destinationQueueIndex);
    }

    /**
     * Register the container result with all busy transactions.
     *
     * @param cresultCode    the result code for the container result
     * @param cresultMessage the result message for the container result
     */
    void registerContainerResult(int cresultCode, String cresultMessage) {
        // try to get the queues lock. This will exit with a release lock exception after lock timeout.
        //noinspection StatementWithEmptyBody
        while (refuseQueueLock(TX_BUSY_QUEUE_INDEX))
            // wait
            ;
        Vector<Transaction> src = queues.get(TX_BUSY_QUEUE_INDEX);
        for (Transaction tx : src) {
            if (tx != null) {
                tx.setCresultCode(cresultCode);
                tx.setCresultMessage(cresultMessage);
            }
        }
        // size didn't change, so no change to queueSizes
        writeQueueToFile(TX_BUSY_QUEUE_INDEX);
        releaseQueueLock(TX_BUSY_QUEUE_INDEX);
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
                break;
            case ACTION_TX_RETRY:
                tx.setSentAt(System.currentTimeMillis());
                tx.setRetries(tx.getRetries() + 1);
                break;
            case ACTION_TX_RESP_MISSING:
                tx.setResultAt(System.currentTimeMillis());
                tx.setResultCode(503);
                tx.setResultMessage("No server response in returned transaction response container");
                tx.setClosedAt(System.currentTimeMillis());
                break;
            case ACTION_TX_CONTAINER_FAILED:
                tx.setResultAt(System.currentTimeMillis());
                tx.setResultCode(504);
                tx.setResultMessage("transaction response container failed");
                tx.setClosedAt(System.currentTimeMillis());
                break;
            case ACTION_TX_MOVE:
                // do nothing. This is to move a transaction from synch to pending queue.
                break;
        }
    }

    /**
     * get a lock for a queue, i.e. prevent all others to use it.
     *
     * @param queueIndex index of queue, for which the lock shall be provided
     */
    private boolean refuseQueueLock(int queueIndex) {
        long now = System.currentTimeMillis();
        if (locks[queueIndex] == -1) {
            locks[queueIndex] = now;
            return false;
        } else {
            if ((now - locks[queueIndex]) > QUEUE_LOCK_TIMEOUT) {
                releaseQueueLock(queueIndex);
                Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_WARNING, International
                        .getMessage("EfaCloud Transaktions-Timeout für {queueName} Queue. Lock wurde zurückgenommen",
                                TX_QUEUE_NAMES[queueIndex]), false);
                return false;
            } else
                return true;
        }
    }

    /**
     * clear all queues and remove all queue contents from the file system. Shall only be triggered by efacloud
     * activation.
     */
    public void clearAllQueues() {
        for (String txFilePath : this.txFilePath) {
            File queueFile = new File(txFilePath);
            if (queueFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                queueFile.renameTo(new File(txFilePath + ".bak"));
                TextResource.writeContents(txFilePath, "", false);
            }
        }
        for (Vector<Transaction> queue : queues)
            queue.clear();
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
     * <p>Write a queue to the storage. Transactions will be written to the file and deleted from the respective queue
     * after writing.
     * </p><p> YOU MUST OBTAIN THE QUEUE LOCK BEFORE AND RELEASE IT AFTERWARDS, because this procedure
     * does not manipulate the locks.</p>
     *
     * @param queueIndex queue index of the queue to be written.
     */
    private void writeQueueToFile(int queueIndex) {
        Vector<Transaction> qw = queues.get(queueIndex);
        // compile queue
        StringBuilder txFileContents = new StringBuilder();
        if (!qw.isEmpty()) {
            for (Transaction tx : qw) {
                tx.appendTxFullString(txFileContents);
                txFileContents.append(Transaction.MESSAGE_SEPARATOR_STRING);
            }
        }
        // Write queue
        String qString = (qw.size() == 0) ? "" : txFileContents
                .substring(0, txFileContents.length() - Transaction.MESSAGE_SEPARATOR_STRING.length());
        boolean written = TextResource.writeContents(txFilePath[queueIndex], qString, false);
        if (!written)
            Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_WARNING, International.getMessage(
                    "Fehler beim Schreiben einer Transaktion für Tabelle {tablename} auf den permanenten Speicher. " +
                            "Transaktion: {transaction}}", txFilePath[queueIndex], qString));
    }

    /**
     * <p>Read the permanently stored transactions from the storage. The respective queue will be
     * cleared and filled with the file's contents. The file's content will stay on the storage after reading.</p><p>
     * YOU MUST OBTAIN THE QUEUE LOCK BEFORE AND RELEASE IT AFTERWARDS, because this procedure does not manipulate the
     * locks.</p>
     *
     * @param queueIndex queue index of the queue to be read.
     */
    private void readQueueFromFile(int queueIndex) {
        Vector<Transaction> qr = queues.get(queueIndex);
        // clear queue first
        qr.clear();
        // read file
        File txFile = new File(txFilePath[queueIndex]);
        if (!txFile.exists())
            return;
        String txFileContents = TextResource.getContents(txFile, "UTF-8");
        if (txFileContents == null || txFileContents.length() == 0)
            return;
        // parse and add transactions
        String[] txsRead = txFileContents.split(Transaction.MESSAGE_SEPARATOR_STRING_REGEX);
        for (String txRead : txsRead)
            if (txRead.length() > 1) {
                Transaction tx = Transaction.parseTxFullString(txRead);
                if (tx != null)
                    queues.get(queueIndex).add(tx);
            }
    }

    /**
     * Return the size of the busy queue
     *
     * @return size of the busy queue
     */
    int getBusyQueueSize() {
        return queues.get(TxRequestQueue.TX_BUSY_QUEUE_INDEX).size();
    }

    /**
     * Create a new transaction and append it to the pending queue. If the queue is stopped, nothing will be appended.
     * If the queue is authenticating, only "nop" transactions will be appended.
     *
     * @param queueIndex index of the queu to append the transaction to. Must be TX_PENDING_QUEUE_INDEX or
     *                   TX_FIX_QUEUE_INDEX
     * @param type       type of message to be appended
     * @param tablename  tablename for transaction
     * @param record     record data of the transaction to be created. An array of n "key; value" pairs, all entries csv
     *                   encoded. If there is no record, e. g. for a first keyfixing request, set it to (String[]) null
     */
    public void appendTransaction(int queueIndex, String type, String tablename, String... record) {
        if (state == QUEUE_IS_STOPPED)
            return;
        // in authentication mode only structure check and building is allowed
        boolean allowedTransaction = (state != QUEUE_IS_AUTHENTICATING) //
                || type.equalsIgnoreCase("nop") //
                || type.equalsIgnoreCase("synch") //
                || type.equalsIgnoreCase("createtable") //
                || type.equalsIgnoreCase("unique") //
                || type.equalsIgnoreCase("autoincrement");
        if (!allowedTransaction)
            return;
        Transaction tx = new Transaction(-1, type, tablename, record);
        // try to get the queues lock. This will exit with a release lock exception after lock timeout.
        //noinspection StatementWithEmptyBody
        while (refuseQueueLock(queueIndex))
            // wait
            ;
        queues.get(queueIndex).add(tx);
        if (isPermanentQueue(queueIndex))
            writeQueueToFile(queueIndex);
        releaseQueueLock(queueIndex);
    }

    @Override
    public void sendRequest(TaskManager.RequestMessage request) {
        // This is the callback from the InternetAccessManager with the access result.
        // pass the results also through a queue, to make sure, a fast response can not overtake
        // an earlier one which is still be processed.
        queueResponses.add(request);
    }

}
