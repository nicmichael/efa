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
import de.nmichael.efa.gui.EfaBaseFrame;
import de.nmichael.efa.gui.EfaBoathouseFrame;
import de.nmichael.efa.gui.EfaCloudConfigDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import java.io.*;
import java.text.ParseException;
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

    // ms with which the queue timer starts processing requests. Use 5 seconds to ensure the program has settled.
    public static final int QUEUE_TIMER_START_DELAY = 5000;
    // ms after which the queue timer processes the next set of requests
    public static final int QUEUE_TIMER_TRIGGER_INTERVAL = 300;

    // ms with which the watchdog timer starts processing requests
    public static final int WATCHDOG_TIMER_START_DELAY = 120000;
    // ms after which the queue timer processes the next set of requests
    public static final int WATCHDOG_TIMER_TRIGGER_INTERVAL = 600000;

    // every SYNCH_PERIOD the client checks for updates at the server side.
    // The update period MUST be at least 5 times the InternetAccessManager timeout.
    // The synchronisation start delay is one SYNCH_PERIOD
    public static final int SYNCH_PERIOD = 60000; // = 60 seconds = 1 minutes
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
    // Note: there is no "activate" request, because if efaCloud is deactivated, there is no queue instance.
    public static final int RQ_QUEUE_DEACTIVATE = 1;            // from AUTHENTICATING, WORKING, SYNCHRONIZING,
    // or PAUSED to DEACTIVATED
    public static final int RQ_QUEUE_AUTHENTICATE = 2;          // from STOPPED to AUTHENTICATING
    public static final int RQ_QUEUE_START = 3;                 // from AUTHENTICATING to WORKING
    public static final int RQ_QUEUE_PAUSE = 4;                 // from WORKING to PAUSED
    public static final int RQ_QUEUE_RESUME = 3;                // from PAUSED, DISCONNECTED to WORKING
    public static final int RQ_QUEUE_START_SYNCH_DOWNLOAD = 5;  // from WORKING to SYNCHRONIZING
    public static final int RQ_QUEUE_START_SYNCH_UPLOAD = 6;    // from WORKING to SYNCHRONIZING
    public static final int RQ_QUEUE_START_SYNCH_DELETE = 7;    // from WORKING to SYNCHRONIZING
    public static final int RQ_QUEUE_STOP_SYNCH = 8;            // form SYNCHRONIZING to WORKING

    // TxQueue state machine state constants
    // Note: there is no "deactivated" queue state, because if efaCloud is deactivated, there is no queue instance.
    public static final int QUEUE_IS_STOPPED = 11;
    public static final int QUEUE_IS_AUTHENTICATING = 12;
    public static final int QUEUE_IS_PAUSED = 130;
    public static final int QUEUE_IS_DISCONNECTED = 131;
    public static final int QUEUE_IS_WORKING = 14;
    public static final int QUEUE_IS_SYNCHRONIZING = 15;
    public static final HashMap<Integer, String> QUEUE_STATE = new HashMap<>();

    static {
        QUEUE_STATE.put(QUEUE_IS_STOPPED, "STOPPED");
        QUEUE_STATE.put(QUEUE_IS_AUTHENTICATING, "AUTHENTICATING");
        QUEUE_STATE.put(QUEUE_IS_PAUSED, "PAUSED");
        QUEUE_STATE.put(QUEUE_IS_DISCONNECTED, "DISCONNECTED");
        QUEUE_STATE.put(QUEUE_IS_WORKING, "WORKING");
        QUEUE_STATE.put(QUEUE_IS_SYNCHRONIZING, "SYNCHRONIZING");
    }
    public static final HashMap<Integer, String> QUEUE_STATE_SYMBOL = new HashMap<>();

    static {
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_STOPPED, "  (\u21d4 \u25aa)");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_AUTHENTICATING, "  (\u21d4 ?)");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_PAUSED, "  (\u21d4 \u2551)");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_DISCONNECTED, "  (\u21ce !)");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_WORKING, "  (\u21d4 \u2714)");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_SYNCHRONIZING, " (\u21d4 \u27f3)");
    }

    private static int txID;                  // the last used transaction ID
    private static int txcID;                 // the last used transaction container ID
    private static TxRequestQueue txq = null; // the static singleton instance of this class
    private static TxResponseHandler txr;     // The handler to hand responses over to

    // the request queues and their size, locks and file paths.
    private final Vector<Integer> stateTransitionRequests = new Vector<>();
    private final ArrayList<Vector<Transaction>> queues = new ArrayList<>();
    private final long[] locks = new long[TX_QUEUE_COUNT];
    private final String[] txFilePath = new String[TX_QUEUE_COUNT];
    private String efacloudLogDir;

    private static final long LOG_PERIOD_MILLIS = 14 * (24 * 3600 * 1000);
    static final HashMap<String, String> logFilePaths = new HashMap<>();

    static {
        logFilePaths.put("efacloud synchronization", "efacloudSynch.log");
        logFilePaths.put("efacloud status", "efacloudStates.log");
        logFilePaths.put("API activity", "efacloudApiActivity.log");
        logFilePaths.put("API errors", "efacloudApiErrors.log");
        logFilePaths.put("API statistics", "efacloudApiStatistics.log");
        logFilePaths.put("internet statistics", "efacloudInternetStatistics.log");
    }

    // The response queue
    private final ArrayList<TaskManager.RequestMessage> queueResponses = new ArrayList<>();

    // The queue state machine parameters
    private int state = QUEUE_IS_STOPPED;
    SynchControl synchControl;

    // poll timer, internet access manager and connection settings
    private Timer queueTimer;
    private Timer watchDog;
    private long pollsCount;                   // a counter incrementing on each queue poll cycle
    private long watchdogPreviousPollsCount;   // the pollsCount value at the previous watchdog trigger event.
    private final InternetAccessManager iam;
    String storageLocationQueues;
    String storageLocationLogs;
    String efaCloudUrl;
    String username;
    String credentials;
    private EfaBaseFrame efaBaseFrame;
    private EfaBoathouseFrame efaBoathouseFrame;

    // Statistics buffer
    private static final int STATISTICS_BUFFER_SIZE = 5000;
    private static int statisticsBufferIndex = 0;
    private static StatisticsRecord[] statisticsRecords;

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
     * Simple setter. Either of both objects shall be null.
     * @param efaBaseFrame setter object. Used for status display
     * @param efaBoathouseFrame setter object. Used for status display
     */
    public void setEfaBaseDialog(EfaBaseFrame efaBaseFrame, EfaBoathouseFrame efaBoathouseFrame) {
        this.efaBaseFrame = efaBaseFrame;
        this.efaBoathouseFrame = efaBoathouseFrame;
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
     * Append a log message to the API log.
     *
     * @param message the message which shall be logged. Note that API message are not logged in the standard way,
     *                because such logging will create a message which itself turn can fail which creates another
     *                message asf ending possibly in en endless loop.
     * @param type    set 1 for Error, 2 for API Statistics, 3 for Internet Statistics
     */
    void logApiMessage(String message, int type) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = format.format(new Date()) + " [API-Error]: ";
        if (type == 1)
            TextResource.writeContents(TxRequestQueue.logFilePaths.get("API errors"), dateString + message, true);
        else if (type == 2)
            TextResource.writeContents(TxRequestQueue.logFilePaths.get("API statistics"), message, false);
        else if (type == 3)
            TextResource.writeContents(TxRequestQueue.logFilePaths.get("internet statistics"), message, false);

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
        efacloudLogDir = storageLocationLogs + File.separator + prName + File.separator + "efacloudlogs";
        //noinspection ResultOfMethodCallIgnored
        new File(efacloudQueuesDir).mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File(efacloudLogDir).mkdirs();

        // initialize log paths and cleanse files.
        SimpleDateFormat formatFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat formatDay = new SimpleDateFormat("yyyy-MM-dd");
        long now = System.currentTimeMillis();
        for (String logFilePath : TxRequestQueue.logFilePaths.keySet()) {
            String fullLogFilePath = efacloudLogDir + File.separator + logFilePaths.get(logFilePath);
            String logFileContents = TextResource.getContents(new File(fullLogFilePath), "UTF-8");
            StringBuilder cleansedLogFile = new StringBuilder();
            if (logFileContents != null) {
                String[] logFileLines = logFileContents.split("\n");
                for (String logFileLine : logFileLines) {
                    if (!logFileLine.isEmpty()) {
                        String logEntryDate = logFileLine.substring(0, 10);
                        Date dayLogEntry = new Date(0L);
                        try {
                            dayLogEntry = formatDay.parse(logEntryDate);
                        } catch (Exception ignored) {
                            // delete all lines which do not start with a date by making their dayLogEntry = 1.1.1970.
                        }
                        if ((now - dayLogEntry.getTime()) < LOG_PERIOD_MILLIS)
                            cleansedLogFile.append(logFileLine).append("\n");
                    }
                }
            }
            if (logFilePath.equalsIgnoreCase("API statistics"))
                initStatisticsCsv(cleansedLogFile.toString());
            else if (logFilePath.equalsIgnoreCase("Internet statistics"))
                InternetAccessManager.initStatisticsCsv(cleansedLogFile.toString());
            else
                cleansedLogFile.append(formatFull.format(new Date())).append(" [all]: LOG STARTING\n");
            TextResource.writeContents(fullLogFilePath, cleansedLogFile.toString(), false);
            logFilePaths.put(logFilePath, fullLogFilePath);
        }

        // check the URL
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
     * Get the log files, post the statistics locally and return all as base64 encoded zip-Archive for server upload.
     */
    private String getLogsAsZip() {
        String txqStatistics = txq.getStatisticsCsv();
        String iamStatistics = iam.getStatisticsCsv();
        logApiMessage(txqStatistics, 2);
        logApiMessage(iamStatistics, 3);
        File efacloudLogDirF = new File(efacloudLogDir);
        if (efacloudLogDirF.exists()) {
            String[] logFiles = efacloudLogDirF.list();
            if (logFiles != null) {
                FileArchive fa = new FileArchive(null, "UTF-8");
                for (String logFile : logFiles)
                    fa.putContent(fa.getInstance(logFile, false),
                            TextResource.getContents(new File(efacloudLogDir + File.separator + logFile), "UTF-8"));
                fa.putContent(fa.getInstance("txqStatistics.csv", false), txqStatistics);
                fa.putContent(fa.getInstance("iamStatistics.csv", false), iamStatistics);
                return Base64.getEncoder().encodeToString(fa.getZipAsBytes());
            } else
                return "";
        } else
            return "";
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
        watchdogPreviousPollsCount = 0L;
        try {
            startQueueTimer();
            startWatchDog();
            registerStateChangeRequest(RQ_QUEUE_AUTHENTICATE);
        } catch (Exception e) {
            Dialog.error("Efacloud konnte nicht gestartet werden.\nFehlermeldung: " + e.getMessage());
            if (queueTimer != null) queueTimer.cancel();
            if (watchDog != null) watchDog.cancel();
        }
    }

    /**
     * <p>Suspend the queue execution.</p><p>This will happen either on an internet connection timeout or a manual
     * pause or deactivate request. Removes all read requests (if paused) or read and write requests (if stopped) from
     * pending queues. Moves all keyfixing confirmations from the synchronization queue to the pending queue and
     * terminates any synchronisation.</p><p>Because the pending queue is permanent, the program can be terminated
     * afterwards without losing requests.</p>
     *
     * @param dropAction the action to be taken, one of ACTION_TX_STOP or ACTION_TX_PAUSE
     */
    private void suspendQueue(int dropAction) {
        state = (dropAction == ACTION_TX_STOP) ? QUEUE_IS_STOPPED : QUEUE_IS_PAUSED;
        String message = (dropAction == ACTION_TX_STOP) ? "stopped" : "paused";
        shiftTx(TX_PENDING_QUEUE_INDEX, TX_DROPPED_QUEUE_INDEX, dropAction, 0, 0);
        shiftTx(TX_SYNCH_QUEUE_INDEX, TX_DROPPED_QUEUE_INDEX, dropAction, 0, 0);
        shiftTx(TX_SYNCH_QUEUE_INDEX, TX_PENDING_QUEUE_INDEX, ACTION_TX_MOVE, 0, 0);
        txq.synchControl.logSynchMessage("Queue activity " + message, "@all", null, true);
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
            suspendQueue(ACTION_TX_PAUSE);
            // Indicate the connection loss. This has no effect on the queue handling.
            state = QUEUE_IS_DISCONNECTED;
            TaskManager.RequestMessage rq = Transaction.createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX));
            iam.sendRequest(rq);
        }
    }

    /**
     * Display the current status in the GUI.
     */
    private void showStatusAtGUI() {
        if (efaBaseFrame != null)
            efaBaseFrame.setTitle();
        else if (efaBoathouseFrame != null)
            efaBoathouseFrame.updateProjectLogbookInfo();
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
                int stateChangeRequest = -1;
                if (stateTransitionRequests.size() > 0)
                    stateChangeRequest = stateTransitionRequests.firstElement();
                switch (stateChangeRequest) {
                    case RQ_QUEUE_DEACTIVATE:
                        if ((state == QUEUE_IS_AUTHENTICATING) || (state == QUEUE_IS_WORKING) ||
                                (state == QUEUE_IS_SYNCHRONIZING) || (state == QUEUE_IS_PAUSED)) {
                            stateTransitionRequests.remove(0);
                            txq.suspendQueue(ACTION_TX_STOP);
                            EfaCloudConfigDialog.deactivateEfacloud();
                            showStatusAtGUI();
                        }
                        break;
                    case RQ_QUEUE_AUTHENTICATE:
                        if (state == QUEUE_IS_STOPPED) {
                            stateTransitionRequests.remove(0);
                            state = QUEUE_IS_AUTHENTICATING;
                            showStatusAtGUI();
                            // start with first transaction a "nop" transaction to verify the credentials.
                            appendTransaction(TX_PENDING_QUEUE_INDEX, "nop", "", "sleep;0");
                            // add a second synch @all transaction to check whether the tables have been
                            // initialized.
                            // Use
                            // a filter which returns a count close to 0 for all tables to save server side
                            // performance.
                            long now = System.currentTimeMillis();
                            appendTransaction(TX_PENDING_QUEUE_INDEX, "synch", "@all", "LastModified;" + now, "?;>");
                        }
                        break;
                    case RQ_QUEUE_PAUSE:
                        if (state == QUEUE_IS_WORKING) {
                            stateTransitionRequests.remove(0);
                            suspendQueue(RQ_QUEUE_PAUSE);
                            showStatusAtGUI();
                        }
                        break;
                    case RQ_QUEUE_RESUME:
                        // case RQ_QUEUE_START:  (same value)
                        if ((state == QUEUE_IS_PAUSED) || (state == QUEUE_IS_DISCONNECTED) ||
                                (state == QUEUE_IS_AUTHENTICATING)) {
                            stateTransitionRequests.remove(0);
                            state = QUEUE_IS_WORKING;
                            showStatusAtGUI();
                            txq.synchControl.logSynchMessage("Queue activity started", "@all", null, true);
                        }
                        break;
                    case RQ_QUEUE_START_SYNCH_DOWNLOAD:
                    case RQ_QUEUE_START_SYNCH_UPLOAD:
                    case RQ_QUEUE_START_SYNCH_DELETE:
                        if (state == QUEUE_IS_WORKING) {
                            if ((queues.get(TX_PENDING_QUEUE_INDEX).size() == 0) &&
                                    (queues.get(TX_BUSY_QUEUE_INDEX).size() == 0)) {
                                stateTransitionRequests.remove(0);
                                state = QUEUE_IS_SYNCHRONIZING;
                                showStatusAtGUI();
                                txq.synchControl.startSynchProcess(stateChangeRequest);
                                // use the synchronization trigger also to gather statistics and logs.
                                if (stateChangeRequest == RQ_QUEUE_START_SYNCH_DOWNLOAD) {
                                    // because the queue state is QUEUE_IS_SYNCHRONIZING this transaction will not
                                    // be processed until the synchronization is completed. It will nevertheless
                                    // not contain the synchronization process transactions.
                                    appendTransaction(TX_PENDING_QUEUE_INDEX, "upload", "zip",
                                            "filepath;efacloudLogs.zip", "contents;" + getLogsAsZip());
                                }
                            }
                        }
                        break;
                    case RQ_QUEUE_STOP_SYNCH:          // End of synchronisation process, no manual option.
                        if (state == QUEUE_IS_SYNCHRONIZING) {
                            if (queues.get(TX_SYNCH_QUEUE_INDEX).size() == 0) {
                                stateTransitionRequests.remove(0);
                                state = QUEUE_IS_WORKING;
                                showStatusAtGUI();
                                txq.synchControl
                                        .logSynchMessage("Synchronisation transactions completed", "@all", null, false);
                                txq.synchControl.logSynchMessage("Synchronisation completed", "@all", null, true);
                            }
                        }
                        break;
                }

                // skip any further activity if paused or stopped.
                if ((txq.state == TxRequestQueue.QUEUE_IS_PAUSED) || (txq.state == TxRequestQueue.QUEUE_IS_STOPPED))
                    return;

                // ============== handle requests ===============
                try {
                    // handle synchronization with first priority, if started. It will then not be interrupted.
                    if (state == QUEUE_IS_SYNCHRONIZING) {
                        // check busy requests for age. Initiate a retry, if needed. The retry ends the
                        // synchronisation state.
                        if (queues.get(TX_BUSY_QUEUE_INDEX).size() > 0)
                            checkForAndHandleTimeout();
                        else if (queues.get(TX_SYNCH_QUEUE_INDEX).size() > 0) {
                            shiftTx(TX_SYNCH_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, ACTION_TX_SEND, 0,
                                    PENDING_QUEUE_MAX_SHIFT_SIZE);
                            TaskManager.RequestMessage rq = Transaction
                                    .createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX));
                            iam.sendRequest(rq);
                        } else
                            txq.registerStateChangeRequest(RQ_QUEUE_STOP_SYNCH);
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
                            registerStateChangeRequest(RQ_QUEUE_START_SYNCH_DOWNLOAD);
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
        watchDog = new Timer();
        watchDog.scheduleAtFixedRate(watchdogTask, WATCHDOG_TIMER_START_DELAY, WATCHDOG_TIMER_TRIGGER_INTERVAL);
    }

    /**
     * Request a state change of the transaction queue. This is only appending the state change request to the state
     * change request request queue. The change is always performed during a poll cycle, to avoid changes at an
     * intermediate state.
     *
     * @param stateChangeRequest one of RQ_QUEUE_*** constant values
     */
    public synchronized void registerStateChangeRequest(int stateChangeRequest) {
        // a state change request can not be repeated, e.g. for synchronization.
        if (stateTransitionRequests.contains(stateChangeRequest))
            return;
        if (stateChangeRequest == RQ_QUEUE_STOP_SYNCH)
            // Stop synch always gets priority. Manual synch may be busy when an automatic synch is requested which
            // results in a deadlock, if the request for synch start precedes the request for synch stop
            stateTransitionRequests.add(0, stateChangeRequest);
        else
            stateTransitionRequests.add(stateChangeRequest);
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
                    boolean isKeyFixingConfirmation = ((tx.type == Transaction.TX_TYPE.KEYFIXING) && tx.hasRecord());
                    boolean keepOnPause = (action == ACTION_TX_PAUSE) &&
                            ((sourceQueueIndex == TX_SYNCH_QUEUE_INDEX) ? isKeyFixingConfirmation : (
                                    isKeyFixingConfirmation || tx.type.isWriteAction));
                    boolean keepOnStop = (action == ACTION_TX_STOP) && isKeyFixingConfirmation;
                    if (!keepOnPause && !keepOnStop) {
                        if (destinationQueueIndex == TxRequestQueue.TX_DROPPED_QUEUE_INDEX)
                            tx.logMessage("drop");
                        dest.add(tx);
                        addStatistics(tx, destinationQueueIndex);
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
     * <p>Write a queue to the storage. Transactions will be written to the file and deleted from the respective
     * queue after writing.
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
                    "Fehler beim Schreiben einer Transaktion für Tabelle {tablename} auf den permanenten Speicher" +
                            ". " + "Transaktion: {transaction}}", txFilePath[queueIndex], qString));
    }

    /**
     * <p>Read the permanently stored transactions from the storage. The respective queue will be
     * cleared and filled with the file's contents. The file's content will stay on the storage after reading .</p><p>
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
        Transaction tx = new Transaction(-1, Transaction.TX_TYPE.getType(type), tablename, record);
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

    /**
     * Add a statistics entry for a transaction shift, if applicable
     *
     * @param tx               transaction shifted
     * @param destinationQueue index of the destination queue of the shift
     */
    void addStatistics(Transaction tx, int destinationQueue) {
        if ((destinationQueue != TX_DROPPED_QUEUE_INDEX) && (destinationQueue != TX_FAILED_QUEUE_INDEX) &&
                (destinationQueue != TX_DONE_QUEUE_INDEX))
            return;
        statisticsRecords[statisticsBufferIndex] = new StatisticsRecord(tx, destinationQueue);
        statisticsBufferIndex++;
        statisticsBufferIndex = (statisticsBufferIndex % STATISTICS_BUFFER_SIZE);
    }

    /**
     * Get a statistics log for the last STATISTICS_BUFFER_SIZE internet access activities for offline analysis.
     *
     * @return the statistics log entries as csv (';'-separated), first line is header.
     */
    private String getStatisticsCsv() {
        StringBuilder csv = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        csv.append("created;type;tablename;waitedMillis;retries;processedMillis;result\n");
        for (int i = 0; i < STATISTICS_BUFFER_SIZE; i++) {
            int index = (STATISTICS_BUFFER_SIZE + statisticsBufferIndex - i) % STATISTICS_BUFFER_SIZE;
            StatisticsRecord sr = statisticsRecords[index];
            if (sr != null) {
                csv.append(sdf.format(new Date(sr.created))).append(";");
                csv.append(sr.type.typeString).append(";");
                csv.append(sr.tablename).append(";");
                csv.append(sr.waitedMillis).append(";");
                csv.append(sr.retries).append(";");
                csv.append(sr.processedMillis).append(";");
                csv.append(sr.result).append("\n");
            }
        }
        return csv.toString();
    }

    /**
     * Parse a csv-String into the statistics buffer
     *
     * @param csv String to parse. Same format as with getStatisticsCsv()
     */
    private static void initStatisticsCsv(String csv) {
        String[] statisticsLines = csv.split("\n");
        // headerStr = "created;type;tablename;waitedMillis;retries;processedMillis;result", see getStatisticsCsv()
        ArrayList<String> entries;
        statisticsBufferIndex = 0;
        statisticsRecords = new StatisticsRecord[STATISTICS_BUFFER_SIZE];
        if (csv.isEmpty())
            return;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (String statisticsLine : statisticsLines) {
            entries = CsvCodec.splitEntries(statisticsLine);
            long created = 0L;
            try {
                created = sdf.parse(entries.get(0)).getTime();
            } catch (ParseException e) {
                entries.clear();   // Header line and incorrect lines will not be used.
            }
            if (entries.size() == 7) {
                final Transaction.TX_TYPE type = Transaction.TX_TYPE.getType(entries.get(1));
                final String tablename = entries.get(2);
                final int waitedMillis = Integer.parseInt(entries.get(3));
                final int retries = Integer.parseInt(entries.get(4));
                final int processedMillis = Integer.parseInt(entries.get(5));
                final String result = entries.get(6);
                StatisticsRecord sr = new StatisticsRecord(type, tablename, created, waitedMillis, retries,
                        processedMillis, result);
                if (statisticsBufferIndex < statisticsRecords.length)
                    statisticsRecords[statisticsBufferIndex] = sr;
                statisticsBufferIndex++;
                statisticsBufferIndex = (statisticsBufferIndex % STATISTICS_BUFFER_SIZE);
            }
        }
    }

    /**
     * Little container for a statistics record to facilitate API statistics gathering.
     */
    static class StatisticsRecord {
        final Transaction.TX_TYPE type;
        final String tablename;
        final long created;
        final int waitedMillis;
        final int retries;
        final int processedMillis;
        final String result;

        /**
         * Simple constructor
         *
         * @param tx               transaction shifted
         * @param destinationQueue index of the destination queue of the shift
         */
        StatisticsRecord(Transaction tx, int destinationQueue) {
            this.type = tx.type;
            this.tablename = tx.tablename;
            this.created = tx.createdAt;
            this.waitedMillis = (tx.getSentAt() == 0) ? 0 : (int) (tx.getSentAt() - tx.createdAt);
            this.processedMillis = (tx.getClosedAt() == 0) ? 0 : (int) (tx.getClosedAt() - tx.getSentAt());
            this.retries = (int) tx.getRetries();
            this.result = (destinationQueue == TX_DROPPED_QUEUE_INDEX) ? "dropped" : ((destinationQueue ==
                    TX_FAILED_QUEUE_INDEX) ? "failed" : "done");
        }

        /**
         * Simple constructor
         *
         * @param type            The transaction type
         * @param tablename       The transactions target table
         * @param created         timestamp of transaction creation
         * @param waitedMillis    wait time in milli seconds
         * @param retries         retries of the transaction due to internet connection failures
         * @param processedMillis time of processing the transactions
         * @param result          transactio result type
         */
        StatisticsRecord(Transaction.TX_TYPE type, String tablename, long created, int waitedMillis, int retries,
                         int processedMillis, String result) {
            this.type = type;
            this.tablename = tablename;
            this.created = created;
            this.waitedMillis = waitedMillis;
            this.processedMillis = processedMillis;
            this.retries = retries;
            this.result = result;
        }
    }

}

