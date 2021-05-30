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
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.Admins;
import de.nmichael.efa.gui.EfaBaseFrame;
import de.nmichael.efa.gui.EfaBoathouseFrame;
import de.nmichael.efa.gui.EfaCloudConfigDialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>The efaCloud transaction request queue manager.</p>
 *
 * <p>Transaction queues are used to handle the communication with the efaCloud server.
 * All transactions are first locally executed and then appended to a transactions pending queue. From that queue they
 * are forwarded to the efaCloud server via the InternetAccessManager. The transactions-pending queue is checked three
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

    // ms with which the queue timer starts processing requests. Use 3 seconds to ensure the project has settled.
    private static final int QUEUE_TIMER_START_DELAY = 3000;
    // ms after which the queue timer processes the next set of requests
    static final int QUEUE_TIMER_TRIGGER_INTERVAL = 300;
    // Count of polls to run a lowa request. Multiply with QUEUE_TIMER_TRIGGER_INTERVAL for te time
    private static final int SYNCH_CHECK_PERIOD_DEFAULT = 30000;    // = 30000 ms = 0.5 minutes
    static int synch_check_polls_period = SYNCH_CHECK_PERIOD_DEFAULT / QUEUE_TIMER_TRIGGER_INTERVAL;

    // every SYNCH_PERIOD the client checks for updates at the server side.
    // The update period MUST be at least 5 times the InternetAccessManager timeout.
    // The synchronisation start delay is one SYNCH_PERIOD
    static final int SYNCH_PERIOD_DEFAULT = 3600000; // = 3600 seconds = 1 hour
    static int synch_period = SYNCH_PERIOD_DEFAULT; // = 3600 seconds = 1 hour

    // If a transaction is busy since more than the RETRY_AFTER_MILLISECONDS period
    // issue a new internet access request.
    private static final int RETRY_PERIOD = 120000; // = 120 seconds = 2 minute

    // timeout for holding a queue locked for manipulation.
    private static final long QUEUE_LOCK_TIMEOUT = 5000;
    // Maximum number of transactions shifted into the pending queue, i. e. of transactions per
    // internet access request. If the internet access is blocked, this will pile upt internet
    // access requests rather than transactions in the pending transactions queue.
    private static final int PENDING_QUEUE_MAX_SHIFT_SIZE = 10;
    // Maximum count of transactions in the done and dropped queues, needed only for debugging. Upload transaction
    // can use considerable memory space
    private static final int DONE_QUEUE_MAX_TXS = 50;
    private static final int DROPPED_QUEUE_MAX_TXS = 50;

    // Transaction queue indices and names.
    public static final int TX_SYNCH_QUEUE_INDEX = 0;
    public static final int TX_PENDING_QUEUE_INDEX = 1;
    public static final int TX_BUSY_QUEUE_INDEX = 2;
    public static final int TX_DONE_QUEUE_INDEX = 3;
    public static final int TX_FAILED_QUEUE_INDEX = 4;
    public static final int TX_DROPPED_QUEUE_INDEX = 5;
    public static final String[] TX_QUEUE_NAMES = new String[]{"txSynch", "txPending", "txBusy", "txDone", "txFailed"
            , "txDropped"};
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
    public static final int RQ_QUEUE_START_SYNCH_UPLOAD = 6;    // from WORKING to SYNCHRONIZING the last 30 days
    public static final int RQ_QUEUE_START_SYNCH_UPLOAD_ALL = 61; // from WORKING to SYNCHRONIZING all data sets
    public static final int RQ_QUEUE_START_SYNCH_DELETE = 7;    // from WORKING to SYNCHRONIZING
    public static final int RQ_QUEUE_STOP_SYNCH = 8;            // form SYNCHRONIZING to WORKING
    public static final HashMap<Integer, String> RQ_QUEUE_STATE = new HashMap<Integer, String>();

    static {
        RQ_QUEUE_STATE.put(RQ_QUEUE_DEACTIVATE, "DEACTIVATE");
        RQ_QUEUE_STATE.put(RQ_QUEUE_AUTHENTICATE, "AUTHENTICATE");
        RQ_QUEUE_STATE.put(RQ_QUEUE_START, "START_RESUME");
        RQ_QUEUE_STATE.put(RQ_QUEUE_PAUSE, "PAUSE");
        RQ_QUEUE_STATE.put(RQ_QUEUE_START_SYNCH_DOWNLOAD, "SYNCH_DOWNLOAD");
        RQ_QUEUE_STATE.put(RQ_QUEUE_START_SYNCH_UPLOAD, "SYNCH_UPLOAD");
        RQ_QUEUE_STATE.put(RQ_QUEUE_START_SYNCH_DELETE, "SYNCH_DELETE");
        RQ_QUEUE_STATE.put(RQ_QUEUE_STOP_SYNCH, "STOP_SYNCH");
    }

    // TxQueue state machine state constants
    // Note: there is no "deactivated" queue state, because if efaCloud is deactivated, there is no queue instance.
    public static final int QUEUE_IS_STOPPED = 11;
    public static final int QUEUE_IS_AUTHENTICATING = 12;
    public static final int QUEUE_IS_PAUSED = 130;
    public static final int QUEUE_IS_DISCONNECTED = 131;
    public static final int QUEUE_IS_WORKING = 14;
    public static final int QUEUE_IS_IDLE = 140;            // the idle state is "working" with empty queues
    public static final int QUEUE_IS_SYNCHRONIZING = 15;
    public static final HashMap<Integer, String> QUEUE_STATE = new HashMap<Integer, String>();

    static {
        QUEUE_STATE.put(QUEUE_IS_STOPPED, "STOPPED");
        QUEUE_STATE.put(QUEUE_IS_AUTHENTICATING, "AUTHENTICATING");
        QUEUE_STATE.put(QUEUE_IS_PAUSED, "PAUSED");
        QUEUE_STATE.put(QUEUE_IS_DISCONNECTED, "DISCONNECTED");
        QUEUE_STATE.put(QUEUE_IS_IDLE, "IDLE");
        QUEUE_STATE.put(QUEUE_IS_WORKING, "WORKING");
        QUEUE_STATE.put(QUEUE_IS_SYNCHRONIZING, "SYNCHRONIZING");
    }

    public static final HashMap<Integer, String> QUEUE_STATE_SYMBOL = new HashMap<Integer, String>();

    static {
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_STOPPED, "  \u25aa");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_AUTHENTICATING, "  ?");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_PAUSED, "  \u2551");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_DISCONNECTED, "  \u21ce!");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_WORKING, "  \u21d4");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_IDLE, "  \u2714");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_SYNCHRONIZING, " \u27f3");
    }

    private static int txID;                  // the last used transaction ID
    private static int txcID;                 // the last used transaction container ID
    private static TxRequestQueue txq = null; // the static singleton instance of this class
    private static TxResponseHandler txr;     // The handler to hand responses over to

    // the request queues and their size, locks and file paths.
    private final Vector<Integer> stateTransitionRequests = new Vector<Integer>();
    final ArrayList<Vector<Transaction>> queues = new ArrayList<Vector<Transaction>>();
    private final long[] locks = new long[TX_QUEUE_COUNT];
    private final String[] txFilePath = new String[TX_QUEUE_COUNT];
    private String storageLocationRoot;
    private String efacloudLogDir;
    private long logLastModified;

    private static final long LOG_PERIOD_MILLIS = 14 * (24 * 3600 * 1000);
    static final HashMap<String, String> logFileNames = new HashMap<String, String>();
    static final HashMap<String, String> logFilePaths = new HashMap<String, String>();

    static {
        logFileNames.put("synch and activities", "efacloud.log");
        logFileNames.put("API statistics", "efacloudApiStatistics.log");
        logFileNames.put("internet statistics", "efacloudInternetStatistics.log");
    }

    // The response queue
    private final ArrayList<TaskManager.RequestMessage> queueResponses = new ArrayList<TaskManager.RequestMessage>();

    // The queue state machine parameters
    private int state = QUEUE_IS_STOPPED;
    SynchControl synchControl;

    // poll timer, internet access manager and connection settings
    private Timer queueTimer;
    private long pollsCount;                   // a counter incrementing on each queue poll cycle
    private final InternetAccessManager iam;
    String efaCloudUrl;
    String username;
    String credentials;
    public String serverWelcomeMessage;
    private Container efaGUIroot;

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
     * Static transaction queue initialization. Will be used by project opening. WILL DELETE ANY EXISTING QUEUE to avoid
     * that with a project change there are two queues open.. The three parameters are just taken once. After creation,
     * this procedure just returns the instance, as does the simplified getInstance() Method.
     *
     * @param efaCloudUrl     URL of efaCoud Server
     * @param username        username for the efaDB server access
     * @param password        password for the efaDB server access
     * @param storageLocation the storageLocation directory without the File.separator ending to store the retry
     *                        transactions to.
     * @return singleton queue instance
     */
    public static TxRequestQueue getInstance(String efaCloudUrl, String username, String password,
                                             String storageLocation) {
        if (txq != null)
            txq.cancel();
        txq = new TxRequestQueue(efaCloudUrl, username, password, storageLocation);
        String objectRef = txq.toString();
        objectRef = objectRef.substring(objectRef.indexOf("@"));
        Logger.log(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_INFO,
                International.getMessage("Server Kommunikation {objectID} zu {URL} gestartet", objectRef, efaCloudUrl));
        return txq;
    }

    /**
     * If the GUI root container is null, find it and remember it for later status display. If it was already set, do
     * nothing.
     *
     * @param efaCallingUIelement Container which triggered the setting.
     */
    public void setEfaGUIrootContainer(Container efaCallingUIelement) {
        if (this.efaGUIroot != null)
            return;
        // identify the efa root Container (for either efa boathouse or efa Base)
        Container efaGUIroot = efaCallingUIelement;
        while ((efaGUIroot != null) && !(efaGUIroot instanceof EfaBaseFrame) &&
                !(efaGUIroot instanceof EfaBoathouseFrame))
            efaGUIroot = efaGUIroot.getParent();
        this.efaGUIroot = efaGUIroot;
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
     * For raspberryPi without clock the system time changes after a little while when having received the ntp answer.
     * That confuses all timers. Use the last log file time stamp to check, wheher this is still to be expected.
     */
    private void waitForTimeToSettle(String logFilePath) {
        File logToCheck = new File(logFilePath);
        if (!logToCheck.exists())
            return;
        // check whether time needs settling
        long lastModifiedLog = logToCheck.lastModified();
        if (System.currentTimeMillis() > lastModifiedLog)
            return;
        // wait for time to settle
        logApiMessage(International.getString("Warte darauf, dass die Systemzeit korrigiert wird."), 1);
        int settleWaitCounter = 0;
        while (System.currentTimeMillis() < lastModifiedLog) {
            settleWaitCounter++;
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
        // add another 5 seconds
        logApiMessage(International
                .getMessage("Systemzeit nach {Sekunden} aktualisiert.", "" + ((double) settleWaitCounter / 100)), 1);
        settleWaitCounter = 0;
        while (settleWaitCounter < 50) {
            settleWaitCounter++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        logApiMessage(International.getString("Beende das Startzeitwarten."), 1);
    }

    /**
     * Simple setter. Will not do value checks but just set.
     *
     * @param stateToSet new state to set. The normal way to set a state is to request a state change. This is just the
     *                   inner function to change the value.
     */
    private void setState(int stateToSet) {
        state = stateToSet;
    }

    /**
     * Simple getter
     *
     * @return the state of operation, e. g. TX_QUEUE_WORKING
     */
    public int getState() {
        if ((state == QUEUE_IS_WORKING) &&
                ((getQueueSize(TX_BUSY_QUEUE_INDEX) + getQueueSize(TX_PENDING_QUEUE_INDEX)) == 0))
            return QUEUE_IS_IDLE;
        return state;
    }

    /**
     * Get a status for display in the base frame or boathouse frame top decoration.
     *
     * @return the state of operation, e. g. TX_QUEUE_WORKING
     */
    public String getStateForDisplay() {
        String efaCloudStatus = QUEUE_STATE_SYMBOL.get(txq.getState());
        if (efaCloudStatus == null)
            efaCloudStatus = TxRequestQueue.QUEUE_STATE_SYMBOL.get(TxRequestQueue.QUEUE_IS_AUTHENTICATING);
        if ((getQueueSize(TX_BUSY_QUEUE_INDEX) + getQueueSize(TX_PENDING_QUEUE_INDEX) +
                getQueueSize(TX_SYNCH_QUEUE_INDEX)) == 0)
            return efaCloudStatus;
        // find the first transaction for ID display
        Transaction tx = (getQueueSize(TX_BUSY_QUEUE_INDEX) > 0) ? queues.get(TX_BUSY_QUEUE_INDEX).firstElement() : ((
                getQueueSize(TX_BUSY_QUEUE_INDEX) > 0) ? queues.get(TX_BUSY_QUEUE_INDEX).firstElement() : (((
                getQueueSize(TX_SYNCH_QUEUE_INDEX) > 0) ? queues.get(TX_SYNCH_QUEUE_INDEX).firstElement() : null)));
        String txID = (tx == null) ? "" : "#" + tx.ID + " ";
        return efaCloudStatus + " - " + txID + getQueueSize(TX_BUSY_QUEUE_INDEX) + "|" +
                getQueueSize(TX_PENDING_QUEUE_INDEX) + "|" + getQueueSize(TX_SYNCH_QUEUE_INDEX);
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
     * @param type    set 0 for Info, 1 for Error, 2 for API Statistics, 3 for Internet Statistics
     */
    public void logApiMessage(String message, int type) {

        // remove line breaks in log message
        if (type < 2) {
            message = message.replace("\n", " // ");
            if (message.length() > 1024)
                message = message.substring(0, 1024) + " ...";
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String info = (type == 1) ? " ERROR " : " INFO ";
        String dateString = format.format(new Date()) + info;
        if (type < 2) {
            // truncate log files,
            File f = new File(logFilePaths.get("synch and activities"));
            if ((f.length() > 200000) && (f.renameTo(new File(logFilePaths.get("synch and activities") + ".previous"))))
                TextResource.writeContents(logFilePaths.get("synch and activities"),
                        "[Log continued]\n" + dateString + message, false);
            else
                TextResource.writeContents(logFilePaths.get("synch and activities"), dateString + message, true);
        } else if (type == 2)  // for statistics the message contains the entire data set.
            TextResource.writeContents(logFilePaths.get("API statistics"), message, false);
        else if (type == 3)  // for statistics the message contains the entire data set.
            TextResource.writeContents(logFilePaths.get("internet statistics"), message, false);

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
        storageLocationRoot = (storageLocation.endsWith(File.separator)) ? storageLocation
                .substring(0, storageLocation.lastIndexOf(File.separator)) : storageLocation;
        storageLocationRoot = storageLocationRoot.substring(0, storageLocationRoot.lastIndexOf(File.separator));
        storageLocationRoot = storageLocationRoot.substring(0, storageLocationRoot.lastIndexOf(File.separator));
        // get the project name out of the root path
        efacloudLogDir = storageLocationRoot + File.separator + "log" + File.separator + Daten.project.getName() +
                File.separator + "efacloudlogs";
        //noinspection ResultOfMethodCallIgnored
        new File(efacloudLogDir).mkdirs();

        // initialize log paths and cleanse files.
        SimpleDateFormat formatFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat formatDay = new SimpleDateFormat("yyyy-MM-dd");
        long now = System.currentTimeMillis();
        for (String logFileName : logFileNames.keySet()) {
            String logFilePath = efacloudLogDir + File.separator + logFileNames.get(logFileName);
            logLastModified = ((logLastModified == 0) && (new File(logFilePath)).exists()) ? (new File(logFilePath))
                    .lastModified() : logLastModified;
            String logFileContents = TextResource.getContents(new File(logFilePath), "UTF-8");
            StringBuilder cleansedLogFile = new StringBuilder();
            if ((logFileName.equalsIgnoreCase("API statistics")) ||
                    (logFileName.equalsIgnoreCase("Internet statistics"))) {
                if (logFileContents != null) {
                    String[] logFileLines = logFileContents.split("\n");
                    for (String logFileLine : logFileLines) {
                        if (!logFileLine.isEmpty()) {
                            long logEntryTime = 0L;
                            if (logFileLine.indexOf(";") > 0) {
                                String logEntryDate = logFileLine.substring(0, logFileLine.indexOf(";"));
                                try {
                                    logEntryTime = Long.parseLong(logEntryDate);
                                } catch (Exception ignored) {
                                    // delete all lines which do not start with a date by making their logEntryTime = 0
                                }
                            }
                            if ((now - logEntryTime) < LOG_PERIOD_MILLIS)
                                cleansedLogFile.append(logFileLine).append("\n");
                        }
                    }
                }
                if (logFileName.equalsIgnoreCase("API statistics"))
                    initStatisticsCsv(cleansedLogFile.toString());
                else if (logFileName.equalsIgnoreCase("Internet statistics"))
                    InternetAccessManager.initStatisticsCsv(cleansedLogFile.toString());
            } else
                cleansedLogFile.append(formatFull.format(new Date())).append(" [@all]: LOG STARTING\n");
            TextResource.writeContents(logFilePath, cleansedLogFile.toString(), false);
            logFilePaths.put(logFileName, logFilePath);
        }

        // check the URL
        this.efaCloudUrl = efaCloudUrl;
        if (!this.efaCloudUrl.endsWith(URL_API_LOCATION)) {
            if (this.efaCloudUrl.endsWith("/"))
                this.efaCloudUrl = this.efaCloudUrl.substring(0, this.efaCloudUrl.length() - 1);
            this.efaCloudUrl += URL_API_LOCATION;
        }
    }

    /**
     * Initialize the queue content. This will also remove all stored queue transactions from previous efa runs.
     */
    private void initQueues() {
        String efacloudQueuesDir =
                storageLocationRoot + File.separator + "log" + File.separator + Daten.project.getName() +
                        File.separator + "efacloudqueues";
        //noinspection ResultOfMethodCallIgnored
        new File(efacloudQueuesDir).mkdirs();
        // initialize the queue indices
        txID = 42;
        txcID = 42;
        for (int i = 0; i < TX_QUEUE_COUNT; i++) {
            Vector<Transaction> queue = new Vector<Transaction>();
            queues.add(queue);
            txFilePath[i] = efacloudQueuesDir + File.separator + TX_QUEUE_NAMES[i] + ".txs";
            // clear also the disk contents of the busy queue, because these transactions are no more open.
            TextResource.writeContents(txFilePath[i], "", false);
            releaseQueueLock(i);
        }
    }

    /**
     * Close all logs
     */
    private void closeLogs() {
        for (String logFileName : logFileNames.keySet()) {
            String logFilePath = logFilePaths.get(logFileName);
            SimpleDateFormat formatFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            TextResource.writeContents(logFilePath, formatFull.format(new Date()) + " [@all]: LOG ENDING\n\n", true);
        }
    }

    /**
     * provide an audit file of the current setup for support purposes.
     */
    public void saveAuditInformation() {
        TextResource.writeContents(efacloudLogDir + File.separator + "auditinfo.txt",
                Daten.tableBuilder.getAuditInformation(), false);
        String txqAuditInfo =
                "Transaction queue audit information:\n" + "------------------------------------\n" + "\ntxq: " +
                        txq.toString() + "\nstate: " + txq.state + "\nusername: " + txq.username +
                        "\npassword (appr. length): " + (txq.credentials.length() - txq.username.length() - 2) +
                        "\nefaCloudUrl: " + txq.efaCloudUrl + "\npollsCount: " + txq.pollsCount +
                        "\nlogLastModified: " + txq.logLastModified + "\nstorageLocationRoot: " +
                        txq.storageLocationRoot + "\nsynch_period: " + TxRequestQueue.synch_period +
                        "\nsynch_check_polls_period: " + TxRequestQueue.synch_check_polls_period +
                        "\nsynchControl.timeOfLastSynch: " + synchControl.timeOfLastSynch +
                        "\nsynchControl.LastModifiedLimit: " + synchControl.LastModifiedLimit;
        TextResource.writeContents(efacloudLogDir + File.separator + "auditinfo.txt", txqAuditInfo, true);
    }

    /**
     * Get the log files, post the statistics locally and return all as base64 encoded zip-Archive for server upload.
     */
    private String getLogsAsZip() {
        String txqStatistics = txq.getStatisticsCsv();
        String iamStatistics = iam.getStatisticsCsv();
        saveAuditInformation();
        logApiMessage(txqStatistics, 2);
        logApiMessage(iamStatistics, 3);
        File efacloudLogDirF = new File(efacloudLogDir);
        if (efacloudLogDirF.exists()) {
            String[] logFiles = efacloudLogDirF.list();
            if (logFiles != null) {
                FileArchive fa = new FileArchive(null, "UTF-8");
                for (String logFile : logFiles)
                    if (!logFile.endsWith("previous"))
                        fa.putContent(fa.getInstance(logFile, false),
                                TextResource.getContents(new File(efacloudLogDir + File.separator + logFile), "UTF-8"));
                fa.putContent(fa.getInstance("txqStatistics.csv", false), txqStatistics);
                fa.putContent(fa.getInstance("iamStatistics.csv", false), iamStatistics);
                // Java8: return Base64.getEncoder().encodeToString(fa.getZipAsBytes());
                return Base64.encodeBytes(fa.getZipAsBytes()); // Java6
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

        this.username = username;
        // combine the credentials to a transaction container prefix.
        this.credentials =
                username + TX_REQ_DELIMITER + CsvCodec.encodeElement(password, TX_REQ_DELIMITER, TX_QUOTATION) +
                        TX_REQ_DELIMITER;

        // initialize all file paths and queues
        initPathsAndLogs(efaCloudUrl, storageLocation);
        initQueues();

        // wait for the system time to settle. When running on a raspberryPI this may occur after
        // program launch, even up to two - three minutes later. Before this isn't completed, do not start
        // any timer.
        String logFilePath = efacloudLogDir + File.separator + logFileNames.get(logFileNames.get(0));
        waitForTimeToSettle(logFilePath);

        // initialize the internet access manager. This shall be the last action in the constructor.
        startTimers();
        iam = InternetAccessManager.getInstance();

        // initialize the response and synchronization handler
        txr = new TxResponseHandler(this);
        synchControl = new SynchControl(this);
    }

    /**
     * Start the que timers after queue generation. This must be outside the constructor to ensure the singleton txq
     * static reference is fully constructed before the timer start.
     */
    private void startTimers() {
        // start queue timer and watchdog
        pollsCount = 0L;
        try {
            startQueueTimer();
            registerStateChangeRequest(RQ_QUEUE_AUTHENTICATE);
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR,
                    International.getString("efaCloud konnte nicht gestartet werden.") +
                            " " + International.getString("Fehlermeldung") + ": " +
                            e.getMessage());
            if (queueTimer != null)
                queueTimer.cancel();
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
        txq.setState((dropAction == ACTION_TX_STOP) ? QUEUE_IS_STOPPED : QUEUE_IS_PAUSED);
        String message = (dropAction == ACTION_TX_STOP) ? "stopped" : "paused";
        shiftTx(TX_PENDING_QUEUE_INDEX, TX_DROPPED_QUEUE_INDEX, dropAction, 0, 0);
        shiftTx(TX_SYNCH_QUEUE_INDEX, TX_DROPPED_QUEUE_INDEX, dropAction, 0, 0);
        shiftTx(TX_SYNCH_QUEUE_INDEX, TX_PENDING_QUEUE_INDEX, ACTION_TX_MOVE, 0, 0);
        txq.synchControl
                .logSynchMessage(International.getString("Änderung der Aktivtät der Serverkommunikation") + ": " + message,
                        "@all", null, true);
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
            // Increase the retry counter and update the sentAt timestamp.
            shiftTx(TX_BUSY_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, ACTION_TX_RETRY, 0, 0);
            // if this happens the first time, notify and cleanse.
            if (txq.getState() != QUEUE_IS_DISCONNECTED) {
                // Remove read activities from the queues
                suspendQueue(ACTION_TX_PAUSE);
                // Indicate the connection loss. This has no effect on the queue handling.
                Logger.log(Logger.WARNING, Logger.MSG_EFACLOUDSYNCH_INFO, International
                        .getMessage("Kommunikation zu {URL} ist gestört. Setze Status auf 'DISCONNECTED'",
                                efaCloudUrl));
            }
            txq.setState(QUEUE_IS_DISCONNECTED);
            TaskManager.RequestMessage rq = Transaction.createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX));
            iam.sendRequest(rq);
        }
    }

    /**
     * Full application restart to reset all queues and communication. The function is provided for the raspberry bootup
     * system time issue.
     */
    void restartEfa() {
        if (efaGUIroot instanceof EfaBoathouseFrame) {
            // Indicate the restart cause. This has no effect on the queue handling.
            Logger.log(Logger.WARNING, Logger.MSG_EFACLOUDSYNCH_INFO, International
                    .getMessage("Kommunikation zu '{URL}' ist nachhaltig gestört. Versuche einen Neustart des Programms zur Behebung.",
                            efaCloudUrl));
            AdminRecord admin = Daten.admins.getAdmin(Admins.SUPERADMIN);
            ((EfaBoathouseFrame) efaGUIroot).cancel(null, EfaBoathouseFrame.EFA_EXIT_REASON_AUTORESTART, admin, true);
        }
    }

    /**
     * Display the current status in the GUI.
     */
    public void showStatusAtGUI() {
        if (efaGUIroot instanceof EfaBaseFrame)
            ((EfaBaseFrame) efaGUIroot).setTitle();
        else if (efaGUIroot instanceof EfaBoathouseFrame)
            ((EfaBoathouseFrame) efaGUIroot).updateProjectLogbookInfo();
    }

    /**
     * Drop an invalid state change request at start of the state change request queue to avoid state locking. Provide a
     * state change log message.
     */
    private void dropInvalidStateChangeRequest() {
        if (stateTransitionRequests.size() == 0)
            return;
        txq.synchControl.logSynchMessage(International.getMessage(
                "Ungültiger Wechsel des efaCloud-Kommunikationsstatus verworfen: in {Status} kann {Requested} " +
                        "({Bedeutung}) nicht als Folge auftreten.", TxRequestQueue.QUEUE_STATE.get(txq.getState()),
                "" + stateTransitionRequests.firstElement(),
                TxRequestQueue.RQ_QUEUE_STATE.get(stateTransitionRequests.firstElement())), "@all", null, true);
        stateTransitionRequests.remove(0);
    }

    /**
     * Start the queue timer, triggering all efaCloud client to server transactions.
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
                        logApiMessage(International
                                .getMessage("Ausnahmefehler bei der Behandlung einer efaCloud-Serverantwort: {Fehler}",
                                        e.toString()), 1);
                    }
                }

                // ============== handle state change ===============
                try {
                    int currentState = txq.getState();
                    int stateChangeRequest = -1;
                    if (stateTransitionRequests.size() > 0)
                        stateChangeRequest = stateTransitionRequests.firstElement();
                    switch (stateChangeRequest) {
                        case RQ_QUEUE_DEACTIVATE:
                            if ((currentState == QUEUE_IS_AUTHENTICATING) || (currentState == QUEUE_IS_DISCONNECTED) ||
                                    (currentState == QUEUE_IS_PAUSED)) {
                                stateTransitionRequests.remove(0);
                                txq.suspendQueue(ACTION_TX_STOP);
                                EfaCloudConfigDialog.deactivateEfacloud();
                                showStatusAtGUI();
                            } else
                                dropInvalidStateChangeRequest();
                            break;
                        case RQ_QUEUE_AUTHENTICATE:
                            if (currentState == QUEUE_IS_STOPPED) {
                                stateTransitionRequests.remove(0);
                                txq.setState(QUEUE_IS_AUTHENTICATING);
                                showStatusAtGUI();
                                // start with first transaction a "nop" transaction to verify the credentials.
                                appendTransaction(TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.NOP, "", "sleep;2");
                                // add a second synch @all transaction to check whether the tables have been
                                // initialized.
                                // Use a filter which returns a count close to 0 for all tables to save server side
                                // performance.
                                long now = System.currentTimeMillis();
                                appendTransaction(TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.SYNCH, "@all",
                                        "LastModified;" + now, "?;>");
                            } else
                                dropInvalidStateChangeRequest();
                            break;
                        case RQ_QUEUE_PAUSE:
                            if ((currentState == QUEUE_IS_WORKING) || (currentState == QUEUE_IS_IDLE) ||
                                    (currentState == QUEUE_IS_AUTHENTICATING) ||
                                    (currentState == QUEUE_IS_SYNCHRONIZING)) {
                                stateTransitionRequests.remove(0);
                                suspendQueue(RQ_QUEUE_PAUSE);
                                showStatusAtGUI();
                            } else
                                dropInvalidStateChangeRequest();
                            break;
                        case RQ_QUEUE_RESUME:
                            // case RQ_QUEUE_START:  (same value)
                            if ((currentState == QUEUE_IS_PAUSED) || (currentState == QUEUE_IS_DISCONNECTED) ||
                                    (currentState == QUEUE_IS_AUTHENTICATING)) {
                                stateTransitionRequests.remove(0);
                                // Redo the first transaction. Increase the retry counter and update the sentAt timestamp.
                                shiftTx(TX_BUSY_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, ACTION_TX_RETRY, 0, 0);
                                TaskManager.RequestMessage rq = Transaction.createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX));
                                iam.sendRequest(rq);
                                txq.setState(QUEUE_IS_WORKING);
                                if (currentState == QUEUE_IS_DISCONNECTED)
                                    Logger.log(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_INFO, International
                                            .getMessage("Kommunikation zu {URL} steht wieder bereit.", efaCloudUrl));
                                else if (currentState == QUEUE_IS_AUTHENTICATING)
                                    txq.synchControl
                                            .logSynchMessage(International.getString("Serverkommunkation gestartet."),
                                                    "@all", null, true);
                                else
                                    txq.synchControl.logSynchMessage(
                                            International.getString("Serverkommunikation wieder aufgenommen."), "@all",
                                            null, true);
                                showStatusAtGUI();
                            } else
                                dropInvalidStateChangeRequest();
                            break;
                        case RQ_QUEUE_START_SYNCH_DOWNLOAD:
                        case RQ_QUEUE_START_SYNCH_UPLOAD:
                        case RQ_QUEUE_START_SYNCH_UPLOAD_ALL:
                        case RQ_QUEUE_START_SYNCH_DELETE:
                            if ((currentState == QUEUE_IS_WORKING) || (currentState == QUEUE_IS_IDLE)) {
                                if ((queues.get(TX_PENDING_QUEUE_INDEX).size() == 0) &&
                                        (queues.get(TX_BUSY_QUEUE_INDEX).size() == 0)) {
                                    stateTransitionRequests.remove(0);
                                    txq.setState(QUEUE_IS_SYNCHRONIZING);
                                    showStatusAtGUI();
                                    saveAuditInformation();
                                    txq.synchControl.startSynchProcess(stateChangeRequest);
                                    // use the synchronization trigger also to gather statistics and logs.
                                    if (stateChangeRequest == RQ_QUEUE_START_SYNCH_DOWNLOAD) {
                                        // because the queue state is QUEUE_IS_SYNCHRONIZING this transaction will not
                                        // be processed until the synchronization is completed. It will nevertheless
                                        // not contain the synchronization process transactions.
                                        appendTransaction(TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.UPLOAD, "zip",
                                                "filepath;efacloudLogs.zip", "contents;" + getLogsAsZip());
                                        // Add a NOP transaction to synchronize the configuration
                                        appendTransaction(TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.NOP, "", "sleep;2");
                                    }
                                }
                            } else
                                dropInvalidStateChangeRequest();
                            break;
                        case RQ_QUEUE_STOP_SYNCH:          // End of synchronisation process, no manual option.
                            if (currentState == QUEUE_IS_SYNCHRONIZING) {
                                if (queues.get(TX_SYNCH_QUEUE_INDEX).size() == 0) {
                                    stateTransitionRequests.remove(0);
                                    txq.setState(QUEUE_IS_WORKING);
                                    showStatusAtGUI();
                                    txq.synchControl.logSynchMessage(
                                            International.getString("Synchronisationstransaktionen abgeschlossen"),
                                            "@all", null, false);
                                }
                            } else
                                dropInvalidStateChangeRequest();
                            break;
                    }
                } catch (Exception e) {
                    logApiMessage(International.getMessage(
                            "Ausnahmefehler bei der Behandlung einer efaCloud-Statusänderungsanforderung: {Fehler}",
                            e.toString()), 1);
                }

                // skip any further activity if paused or stopped.
                if ((txq == null) || (txq.getState() == QUEUE_IS_PAUSED) || (txq.getState() == QUEUE_IS_STOPPED))
                    return;

                // ============== handle requests ===============
                try {
                    // handle synchronization with first priority, if started. It will then not be interrupted.
                    if (txq.getState() == QUEUE_IS_SYNCHRONIZING) {
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
                            // the first NOP transaction shall always be alone in a container
                            // to ensure it is retried, until the connections is established.
                            int shiftSize = (queues.get(TX_PENDING_QUEUE_INDEX).firstElement().type ==
                                    Transaction.TX_TYPE.NOP) ? 1 : PENDING_QUEUE_MAX_SHIFT_SIZE;
                            shiftTx(TX_PENDING_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, ACTION_TX_SEND, 0, shiftSize);
                            // Remove te connection loss indication when the busy queue was cleared.
                            if (txq.getState() == QUEUE_IS_DISCONNECTED)
                                txq.setState(QUEUE_IS_WORKING);
                            // read the transactions to use them
                            TaskManager.RequestMessage rq = Transaction
                                    .createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX));
                            iam.sendRequest(rq);
                        }
                        // it occurred that the queue was disconnected, but had no pending transaction. That will
                        // not reconnect then.
                        else if (txq.getState() == QUEUE_IS_DISCONNECTED) {
                            appendTransaction(TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.NOP, "", "sleep;2");
                        }
                        // check whether to start synchronisation, if neither busy nor pending requests are there
                        else if ((txq.getState() == QUEUE_IS_IDLE) &&
                                (polltime - synchControl.timeOfLastSynch > synch_period)) {
                            // use the opportunity to clear the done and dropped queue, which will else be a memory leak
                            while (queues.get(TX_DONE_QUEUE_INDEX).size() > DONE_QUEUE_MAX_TXS)
                                queues.get(TX_DONE_QUEUE_INDEX).remove(0);
                            while (queues.get(TX_DROPPED_QUEUE_INDEX).size() > DROPPED_QUEUE_MAX_TXS)
                                queues.get(TX_DROPPED_QUEUE_INDEX).remove(0);
                            // The synch transactions queue is never stored, so it needs not to be read from file.
                            queues.get(TX_SYNCH_QUEUE_INDEX).clear();
                            registerStateChangeRequest(RQ_QUEUE_START_SYNCH_DOWNLOAD);
                        } else if ((txq.getState() == QUEUE_IS_IDLE) &&
                                ((pollsCount % synch_check_polls_period) == (synch_check_polls_period - 1))) {
                            // send it to the internet access manager.
                            String postURLplus = txq.efaCloudUrl + "?lowa=" + username;
                            // For the InternetAccessManager semantics of a RequestMessage, see InternetAccessManager
                            // class information.
                            TaskManager.RequestMessage rq = new TaskManager.RequestMessage(postURLplus, "",
                                    InternetAccessManager.TYPE_POST_PARAMETERS, 0.0, txq);
                            iam.sendRequest(rq);
                        }
                    }

                } catch (Exception e) {
                    logApiMessage(International
                            .getMessage("Ausnahmefehler bei der Behandlung einer efaCloud-Serveranfrage: {Fehler}",
                                    e.toString()), 1);
                }
            }
        };
        queueTimer = new Timer();
        queueTimer.scheduleAtFixedRate(queueTimerTask, QUEUE_TIMER_START_DELAY, QUEUE_TIMER_TRIGGER_INTERVAL);
    }

    /**
     * <p><b>ONLY TO BE CALLED BY EFACLOUD DEACTIVATION OR PROJECT CHANGE</b></p><p>Terminate all efaCloud activities.
     * This stops the efaCloud poll timer and watchdog, the internet access manager's activities and drops the link to
     * the singleton TxRequestQueue instance.</p>
     */
    public void cancel() {
        Logger.log(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_INFO, "Schließe Server-Kommunikation.");
        queueTimer.cancel();
        iam.cancel();
        clearAllQueues();
        txq.synchControl = null;
        closeLogs();
        txr = null;
        txq = null;
        // update status display, remove efaCloud part
        showStatusAtGUI();
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
     * Check the busy queue head
     *
     * @return true, if the first transaction within the busy queue is a NOP transaction. This shall always be kept,
     * never dropped.
     */
    boolean busyHeadIsNop() {
        return (txq.queues.get(TX_BUSY_QUEUE_INDEX).size() > 0) &&
                (txq.queues.get(TX_BUSY_QUEUE_INDEX).firstElement().type == Transaction.TX_TYPE.NOP);
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
                        if (destinationQueueIndex == TX_DROPPED_QUEUE_INDEX)
                            txq.logApiMessage("#" + tx.ID + ", " + tx.type + " [" + tx.tablename + "]: " + "Transaction dropped", 1);
                        dest.add(tx);
                        // cut failed transactions length to avoid log overload
                        if (destinationQueueIndex == TX_FAILED_QUEUE_INDEX)
                            tx.shortenRecord(1024);
                        addStatistics(tx, destinationQueueIndex);
                        src.remove(tx);
                    }
                }
            }
            i++;
        }
        if (isPermanentQueue(sourceQueueIndex))
            writeQueueToFile(sourceQueueIndex);
        if (isPermanentQueue(destinationQueueIndex) && (sourceQueueIndex != destinationQueueIndex))
            writeQueueToFile(destinationQueueIndex);
        releaseQueueLock(sourceQueueIndex);
        releaseQueueLock(destinationQueueIndex);
        if (!registerActionOnly)
            showStatusAtGUI();
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
                tx.setResultMessage(Transaction.TX_RESULT_CODES.get(503));
                tx.setClosedAt(System.currentTimeMillis());
                break;
            case ACTION_TX_CONTAINER_FAILED:
                tx.setResultAt(System.currentTimeMillis());
                tx.setResultCode(504);
                tx.setResultMessage(Transaction.TX_RESULT_CODES.get(504));
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
                logApiMessage(International
                        .getMessage("efaCloud Transaktions-Timeout für {Typ} Queue. Lock wurde zurückgenommen.",
                                TX_QUEUE_NAMES[queueIndex]), 1);
                return false;
            } else
                return true;
        }
    }

    /**
     * clear all queues and remove all queue contents from the file system. Shall only be triggered by efaCloud
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
                tx.appendTxFullString(txFileContents, 0);
                txFileContents.append(Transaction.MESSAGE_SEPARATOR_STRING);
            }
        }
        // Write queue
        String qString = (qw.size() == 0) ? "" : txFileContents
                .substring(0, txFileContents.length() - Transaction.MESSAGE_SEPARATOR_STRING.length());
        boolean written = TextResource.writeContents(txFilePath[queueIndex], qString, false);
        if (!written)
            logApiMessage(International.getMessage(
                    "Fehler beim Schreiben einer Transaktion für Queue {Typ} auf den permanenten Speicher. " +
                            "Transaktion: {Transaktion}", txFilePath[queueIndex], qString), 1);
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
        return queues.get(TX_BUSY_QUEUE_INDEX).size();
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
    public void appendTransaction(int queueIndex, Transaction.TX_TYPE type, String tablename, String... record) {
        if (txq.getState() == QUEUE_IS_STOPPED)
            return;
        // in authentication mode only structure check and building is allowed
        boolean allowedTransaction = (txq.getState() != QUEUE_IS_AUTHENTICATING) //
                || type == Transaction.TX_TYPE.NOP //
                || type == Transaction.TX_TYPE.SYNCH//
                || type == Transaction.TX_TYPE.CREATETABLE //
                || type == Transaction.TX_TYPE.UNIQUE //
                || type == Transaction.TX_TYPE.AUTOINCREMENT;
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
        String lastTablename = "";
        csv.append("created;type;tablename;waitedMillis;retries;processedMillis;result\n");
        for (int i = 0; i < STATISTICS_BUFFER_SIZE; i++) {
            int index = (STATISTICS_BUFFER_SIZE + statisticsBufferIndex - i) % STATISTICS_BUFFER_SIZE;
            StatisticsRecord sr = statisticsRecords[index];
            if (sr != null) {
                csv.append(sr.created).append(";");
                csv.append(sr.type.typeString).append(";");
                if (!sr.tablename.equalsIgnoreCase(lastTablename)) {
                    csv.append(sr.tablename).append(";");
                    lastTablename = sr.tablename;
                } else
                    csv.append(".;");
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
        for (String statisticsLine : statisticsLines) {
            entries = CsvCodec.splitEntries(statisticsLine);
            long created = 0L;
            try {
                created = Long.parseLong(entries.get(0));
            } catch (Exception e) {
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

