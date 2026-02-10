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

import java.awt.Container;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.EfaBaseFrame;
import de.nmichael.efa.gui.EfaBoathouseFrame;
import de.nmichael.efa.gui.EfaCloudConfigDialog;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

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

    // API protocol versions: 1: first implementation; 2: added VERIFY statement; 3: (planned: use of ecrid keys)
    public static final int EFA_CLOUD_MAX_API_VERSION = 3;  // since August 2024. before it was '1'
    // depending on the server response the api version is adjusted to the common maximum
    public static int efa_cloud_used_api_version = 3;  // since August 2024. before it was '1'

    public static final char TX_REQ_DELIMITER = ';';
    public static final String TX_RESP_DELIMITER = ";";
    public static final char TX_QUOTATION = '"';
    public static final String URL_API_LOCATION = "/api/posttx.php";

    // ms with which the queue timer starts processing requests. Use 15 seconds to ensure the project has settled.
    private static final int QUEUE_TIMER_START_DELAY = 15000;
    // ms after which the queue timer processes the next set of requests
    static final int QUEUE_TIMER_TRIGGER_INTERVAL = 300;
    // Count of polls to run a lowa request. Multiply with QUEUE_TIMER_TRIGGER_INTERVAL for te time
    private static final int SYNCH_CHECK_PERIOD_DEFAULT = 30000;    // = 30000 ms = 0.5 minutes
    static int synch_check_polls_period = SYNCH_CHECK_PERIOD_DEFAULT / QUEUE_TIMER_TRIGGER_INTERVAL;
    static int logs_to_return = 2; // return all logs

    // every SYNCH_PERIOD the client checks for updates at the server side.
    // The update period MUST be at least 5 times the InternetAccessManager timeout.
    // The synchronisation start delay is one SYNCH_PERIOD
    static final int SYNCH_PERIOD_DEFAULT = 3600000; // = 3600 seconds = 1 hour
    static final long SYNCHRONIZATION_TIMEOUT = 15*60*1000; // the synchronisation will be forced to end after this time - 15 minutes
    static int synch_period = SYNCH_PERIOD_DEFAULT; // = 3600 seconds = 1 hour
    static final int STATS_UPLOAD_PERIOD_MIN = 86400000;  // = 86400 seconds = 24 hours

    // If a transaction is busy since more than the RETRY_AFTER_MILLISECONDS period
    // issue a new internet access request.
    public static final int RETRY_PERIOD = 120000; // = 120 seconds = 2 minutes

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
    
    private static final int EFACLOUD_LOG_MAX_SIZE = 200000; //200 kb. 
    private static final String FILENAME_PREVIOUS_SUFFIX = ".previous.log";    

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
    // Obsolete from 2.3.1: public static final int RQ_QUEUE_START_SYNCH_DELETE = 7;    // from WORKING to SYNCHRONIZING
    public static final int RQ_QUEUE_STOP_SYNCH = 8;            // form SYNCHRONIZING to WORKING
    public static final HashMap<Integer, String> RQ_QUEUE_STATE = new HashMap<Integer, String>();

    static {
        RQ_QUEUE_STATE.put(RQ_QUEUE_DEACTIVATE, "DEACTIVATE");
        RQ_QUEUE_STATE.put(RQ_QUEUE_AUTHENTICATE, "AUTHENTICATE");
        RQ_QUEUE_STATE.put(RQ_QUEUE_START, "START_RESUME");
        RQ_QUEUE_STATE.put(RQ_QUEUE_PAUSE, "PAUSE");
        RQ_QUEUE_STATE.put(RQ_QUEUE_START_SYNCH_DOWNLOAD, "SYNCH_DOWNLOAD");
        RQ_QUEUE_STATE.put(RQ_QUEUE_START_SYNCH_UPLOAD, "SYNCH_UPLOAD");
        // Obsolete from 2.3.1: RQ_QUEUE_STATE.put(RQ_QUEUE_START_SYNCH_DELETE, "SYNCH_DELETE");
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
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_STOPPED, "  ▪");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_AUTHENTICATING, "  ?");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_PAUSED, "  ║");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_DISCONNECTED, "  ⇔!");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_WORKING, "  ⇔");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_IDLE, "  ✔");
        QUEUE_STATE_SYMBOL.put(QUEUE_IS_SYNCHRONIZING, " ⟳");
    }
    
    public static final HashMap<Integer, ImageIcon> QUEUE_STATE_ICON = new HashMap<Integer, ImageIcon>();

    static {
    	QUEUE_STATE_ICON.put(QUEUE_IS_STOPPED, ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_EFACLOUD_STOPPED));
    	QUEUE_STATE_ICON.put(QUEUE_IS_AUTHENTICATING, ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_EFACLOUD_AUTHENTICATING));
    	QUEUE_STATE_ICON.put(QUEUE_IS_PAUSED, ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_EFACLOUD_PAUSED));
    	QUEUE_STATE_ICON.put(QUEUE_IS_DISCONNECTED, ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_EFACLOUD_DISCONNECTED));
    	QUEUE_STATE_ICON.put(QUEUE_IS_WORKING, ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_EFACLOUD_WORKING));
    	QUEUE_STATE_ICON.put(QUEUE_IS_IDLE, ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_EFACLOUD_IDLE));
    	QUEUE_STATE_ICON.put(QUEUE_IS_SYNCHRONIZING, ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_EFACLOUD_SYNCHRONIZING));
    }    

    private static int txID;                  // the last used transaction ID
    private static int txcID;                 // the last used transaction container ID
    private static TxRequestQueue txq = null; // the static singleton instance of this class
    private static TxResponseHandler txr;     // The handler to hand responses over to

    // the request queues and their size, locks and file paths.
    private final Vector<Integer> stateTransitionRequests = new Vector<Integer>();
    final ArrayList<Vector<Transaction>> queues = new ArrayList<Vector<Transaction>>();
    private final long[] locks = new long[TX_QUEUE_COUNT];
    private String storageLocationRoot;
    protected String efacloudLogDir;
    private long logLastModified;
    static String logFilePath;
    static String synchErrorFilePath;

    // The response queue
    private final ArrayList<TaskManager.RequestMessage> queueResponses = new ArrayList<TaskManager.RequestMessage>();

    // The queue state machine parameters
    private int state = QUEUE_IS_STOPPED;
    protected SynchControl synchControl;

    // poll timer, internet access manager and connection settings
    private Timer queueTimer;
    private long pollsCount;                   // a counter incrementing on each queue poll cycle
    private final InternetAccessManager iam;
    final String efaCloudUrl;
    private final String username;
    private final String credentials;
    private String adminCredentials = "";  // the admin credentials are used during admin sessions in efaBths
    private String adminEfaCloudUserID = "";
    public String serverWelcomeMessage;
    public String adminSessionMessage;
    private Container efaGUIroot;
    private long lastStatsUpload = 0L;

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
     * Set the admin credentials. Called by admin login in efaBths. Once set, admin credentials are used for
     * all transactions in the pending queue. Call clearAdminCredentials() to end the admin session.
     * @param adminName the admin name within the efa client
     * @param efaCloudUserID the admins efaCloudUserID
     * @param password the admins efaCloud password (plain text, not hash, will not be stored)
     */
    public void setAdminCredentials(String adminName, String efaCloudUserID, String password) {
        // combine the credentials to a transaction container prefix.
        this.adminCredentials =
                efaCloudUserID + TX_REQ_DELIMITER + CsvCodec.encodeElement(password, TX_REQ_DELIMITER, TX_QUOTATION) +
                        TX_REQ_DELIMITER;
        this.adminSessionMessage = " [admin: " + adminName + "]";
        this.adminEfaCloudUserID = efaCloudUserID;
        txq.logApiMessage("state, []: Starting admin session with efaCloudUserID " + efaCloudUserID, 0);
    }

    /**
     * Clar the admin credentials, i.e. fall back to boathouse credentials.
     */
    public void clearAdminCredentials() {
        if (txq != null)
            txq.logApiMessage("state, []: Ending admin session for efaCloudUserID " + this.adminEfaCloudUserID, 0);
        this.adminCredentials = "";
        this.adminEfaCloudUserID = "";
        this.adminSessionMessage = "";
    }

    String getCredentials() {
        return (adminCredentials.length() > 0) ? adminCredentials : credentials;
    }

    String getAdminUserID() {
        return (adminCredentials.length() > 0) ? adminEfaCloudUserID : username;
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
                //noinspection BusyWait
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
        txq.synchControl.logSynchMessage(
                International
                        .getMessage("Statuswechsel der Serverkommunikation zu {Status}", QUEUE_STATE.get(stateToSet)),
                "", null, true);
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
    public String getStateForDisplay(Boolean withState) {
    	if (txq==null) 
    		return "";
    	
    	String efaCloudStatus = "";
    	if (withState) {
    		efaCloudStatus=QUEUE_STATE_SYMBOL.get(txq.getState());
            if (efaCloudStatus == null)
                efaCloudStatus = TxRequestQueue.QUEUE_STATE_SYMBOL.get(TxRequestQueue.QUEUE_IS_AUTHENTICATING);
    	}
        if ((getQueueSize(TX_BUSY_QUEUE_INDEX) + getQueueSize(TX_PENDING_QUEUE_INDEX) +
                getQueueSize(TX_SYNCH_QUEUE_INDEX)) == 0)
            return efaCloudStatus;
        // find the first transaction for ID display
        Transaction tx = null;
        try {
	        tx = (getQueueSize(TX_BUSY_QUEUE_INDEX) > 0) ? queues.get(TX_BUSY_QUEUE_INDEX).firstElement() : ((
	                getQueueSize(TX_BUSY_QUEUE_INDEX) > 0) ? queues.get(TX_BUSY_QUEUE_INDEX).firstElement() : (((
	                getQueueSize(TX_SYNCH_QUEUE_INDEX) > 0) ? queues.get(TX_SYNCH_QUEUE_INDEX).firstElement() : null)));
        } catch (Exception e) {
        	// in some conditions, getQueuesize returns a value >0, but when getting the queue's first element,
        	// there may be no more element... an then an exception occurs. This may be as efacloud thread is running in background,
        	// and so the queue items are processed in background.
        	// as this function is used for efaBths/efaBase header only, we do not need to document an exception here.
        }
        String txID = (tx == null) ? "" : "#" + tx.ID + " ";
        return (efaCloudStatus.isEmpty() ? "" : efaCloudStatus+ " - " ) + txID + getQueueSize(TX_BUSY_QUEUE_INDEX) + "|" +
                getQueueSize(TX_PENDING_QUEUE_INDEX) + "|" + getQueueSize(TX_SYNCH_QUEUE_INDEX);
    }

    /**
     * Get an icon for display in the base frame of efaBoathouse top decoration
     * @return the image icon
     */
    public ImageIcon getStateIconForDisplay() {
    	if (txq==null) 
    		return null;
    	
		ImageIcon efaCloudStatus=QUEUE_STATE_ICON.get(txq.getState());
        if (efaCloudStatus == null) 
        	efaCloudStatus = QUEUE_STATE_ICON.get(TxRequestQueue.QUEUE_IS_AUTHENTICATING);
        return efaCloudStatus;
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
     * @param type    set 0 for Info, 1 for Error
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
            File efaCloudLogFile = new File(logFilePath);
            Boolean appendLine=true;
            
            //efacloud.log rotation: if >5 Mb, delete old efacloud.previous.log file and rename efacloud.log to efacloud.log.previous.log
            if (efaCloudLogFile.length() > EFACLOUD_LOG_MAX_SIZE) {
            	File oldPreviousLogfile=new File(logFilePath + FILENAME_PREVIOUS_SUFFIX);
            	
            	// rotate existing efacloud.log to efacloud.log.previous and delete the existing file if necessary.
            	if ((oldPreviousLogfile.exists() && oldPreviousLogfile.delete()) 
            			|| (!oldPreviousLogfile.exists())) {
            		if (efaCloudLogFile.renameTo(new File(logFilePath + FILENAME_PREVIOUS_SUFFIX))) {
            			appendLine=false;
            		}
            	}
            }
            TextResource.writeContents(logFilePath, dateString + message, appendLine);
        }
    }

    /**
     * Initialize all file paths, URL, and queues. Part of condtructor but split into separae function to improve code
     * readability.
     *
     * @param storageLocation the storageLocation directory without the File.separator ending to store the retry
     *                        transactions to.
     */
    private void initPathsAndLogs(String storageLocation) {

        // initialize log directories.
        storageLocationRoot = (storageLocation.endsWith(File.separator)) ? storageLocation
                .substring(0, storageLocation.lastIndexOf(File.separator)) : storageLocation;
        storageLocationRoot = storageLocationRoot.substring(0, storageLocationRoot.lastIndexOf(File.separator));
        storageLocationRoot = storageLocationRoot.substring(0, storageLocationRoot.lastIndexOf(File.separator));
        // remove the efacloud log and transactions directories of previous efa program versions
        efacloudLogDir = storageLocationRoot + File.separator + "log" + File.separator + Daten.project.getName();
        deleteRecursive(new File(efacloudLogDir));
        // get the project name out of the root path
        efacloudLogDir = storageLocationRoot + File.separator + "log" + File.separator + "efaCloud" +
                File.separator + Daten.project.getName();
        //noinspection ResultOfMethodCallIgnored
        new File(efacloudLogDir).mkdirs();

        // initialize log paths and cleanse files.
        SimpleDateFormat formatFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        logFilePath = efacloudLogDir + File.separator + "efacloud.log";
        synchErrorFilePath = efacloudLogDir + File.separator + "synchErrors.log";
        File efaCloudLogFile = new File(logFilePath);
        logLastModified = ((logLastModified == 0) && efaCloudLogFile.exists()) ? efaCloudLogFile
                    .lastModified() : logLastModified;
        if (!efaCloudLogFile.exists())
            TextResource.writeContents(logFilePath, formatFull.format(new Date()) + " INFO state, []: LOG STARTING\n", false);
    }

    /**
     * Initialize the queue content. This will also remove all stored queue transactions from previous efa runs.
     */
    private void initQueues() {
        // initialize the queue indices
        txID = 42;
        txcID = 42;
        for (int i = 0; i < TX_QUEUE_COUNT; i++) {
            Vector<Transaction> queue = new Vector<Transaction>();
            queues.add(queue);
            releaseQueueLock(i);
        }
    }

    /**
     * Close all logs
     */
    private void closeLogs() {
        SimpleDateFormat formatFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TextResource.writeContents(logFilePath, formatFull.format(new Date())
                    + " INFO state, []: LOG ENDING\n\n", true);
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
                        "\npassword (appr. length): " + (txq.getCredentials().length() - txq.username.length() - 2) +
                        "\nefaCloudUrl: " + txq.efaCloudUrl + "\npollsCount: " + txq.pollsCount +
                        "\nlogLastModified: " + txq.logLastModified + "\nstorageLocationRoot: " +
                        txq.storageLocationRoot + "\nsynch_period: " + TxRequestQueue.synch_period +
                        "\nsynch_check_polls_period: " + TxRequestQueue.synch_check_polls_period +
                        "\nsynchControl.timeOfLastSynch: " + synchControl.lastSynchStartedMillis +
                        "\nsynchControl.LastModifiedLimit: " + synchControl.LastModifiedLimit;
        TextResource.writeContents(efacloudLogDir + File.separator + "auditinfo.txt", txqAuditInfo, true);
    }

    /**
     * Get the log files, post the statistics locally and return all as base64 encoded zip-Archive for server upload.
     */
    private String getLogsAsZip() {
        saveAuditInformation();
        File efacloudLogDirF = new File(efacloudLogDir);
        if (efacloudLogDirF.exists()) {
            String[] logFiles = efacloudLogDirF.list();
            if (logFiles != null) {
                FileArchive fa = new FileArchive(null, "UTF-8");
                // upload statistics is deactivated since August 2024
                // add config files
                String projectFileName = storageLocationRoot + File.separator + "data" + File.separator + Daten.project.getName() +
                        ".efa2project";
                String projectFile = TextResource.getContents(new File(projectFileName), "UTF-8");
                if ((projectFile == null) || projectFile.isEmpty())
                    projectFile = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
                fa.putContent(fa.getInstance(Daten.project.getName() + ".efa2project", false), projectFile);
                String configDirName = storageLocationRoot + File.separator + "cfg" + File.separator;
                String[] cfgFnames = new String[] {
                        "configuration.efa2config", "types.efa2types", "wett.cfg", "wettdefs.cfg"
                };
                for (String cfgFname : cfgFnames) {
                    String cfgFile = TextResource.getContents(new File(configDirName + cfgFname), "UTF-8");
                    if ((cfgFile == null))
                        cfgFile = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
                    fa.putContent(fa.getInstance(cfgFname, false), cfgFile);
                }
                // add the log and synchronisation errors file
                String errorsFile = TextResource.getContents(new File(synchErrorFilePath), "UTF-8");
                if (TxRequestQueue.logs_to_return >= 1)
                    fa.putContent(fa.getInstance("synchErrors.log", false), errorsFile);
                String logFile = TextResource.getContents(new File(logFilePath), "UTF-8");
                if (TxRequestQueue.logs_to_return >= 2)
                    fa.putContent(fa.getInstance("efacloud.log", false), logFile);
                // Java8: return Base64.getEncoder().encodeToString(fa.getZipAsBytes());
                return Base64.encodeBytes(fa.getZipAsBytes()); // Java6
            } else
                return "";
        } else
            return "";
    }

    /**
     * Check the provided Url on connectivity and credentials to provide an appropriate error message to the user.
     * @return error message or "", if all is ok.
     */
    public String checkCredentials() {
        String testResponse = InternetAccessManager.getText(this.efaCloudUrl,
                "txc=" + Transaction.createSingleNopRequestContainer(this.credentials));
        // error messages are plain text, no base 64 encoding.
        if (testResponse.startsWith("#ERROR:"))
            return testResponse;
        String txContainerBase64 = testResponse.replace('-', '/').replace('*', '+').replace('_', '=').trim();
        String txContainer;
        try {
            // Java 8: txContainer = new String(Base64.getDecoder().decode(txContainerBase64),
            // StandardCharsets.UTF_8);
            txContainer = new String(Base64.decode(txContainerBase64), "UTF-8");  // Java 6
        } catch (Exception ignored) {
            return "#ERROR: Format error in response.";
        }
        String[] headerAndContent = txContainer.split(TxRequestQueue.TX_RESP_DELIMITER, 5);
        if (headerAndContent.length < 4)
            return "#ERROR: Syntax error in response.";
        try {
            int cresult_code = Integer.parseInt(headerAndContent[2]);
            if (cresult_code != 300)
                return "#ERROR: " + headerAndContent[2] + ", " + headerAndContent[3];
        } catch (NumberFormatException ignored) {
            return "#ERROR: Syntax error in response.";
        }
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

        // check the URL
        if (!efaCloudUrl.endsWith(URL_API_LOCATION)) {
            if (efaCloudUrl.endsWith("/"))
                efaCloudUrl = efaCloudUrl.substring(0, efaCloudUrl.length() - 1);
            this.efaCloudUrl = efaCloudUrl + URL_API_LOCATION;
        }
        else this.efaCloudUrl = efaCloudUrl;
        clearAdminCredentials();

        // initialize all file paths and queues
        initPathsAndLogs(storageLocation);
        initQueues();

        // wait for the system time to settle. When running on a raspberryPI this may occur after
        // program launch, even up to two - three minutes later. Before this isn't completed, do not start
        // any timer.
        String logFilePath = efacloudLogDir + File.separator + "efacloud.log";
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
        String message = (dropAction == ACTION_TX_STOP) ? "stopped" : "paused/disconnected";
        shiftTx(TX_PENDING_QUEUE_INDEX, TX_DROPPED_QUEUE_INDEX, dropAction, 0, 0);
        shiftTx(TX_SYNCH_QUEUE_INDEX, TX_DROPPED_QUEUE_INDEX, dropAction, 0, 0);
        shiftTx(TX_SYNCH_QUEUE_INDEX, TX_PENDING_QUEUE_INDEX, ACTION_TX_MOVE, 0, 0);
        txq.synchControl
                .logSynchMessage(International.getString("Änderung der Aktivtät der Serverkommunikation") + ": " + message,
                        "", null, true);
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
            txq.registerStateChangeRequest(RQ_QUEUE_RESUME);
        }
    }

    /**
     * Display the current status in the GUI.
     */
    public void showStatusAtGUI() {
        if (efaGUIroot instanceof EfaBaseFrame)
        	SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
                	((EfaBaseFrame) efaGUIroot).setTitle();
        		}
        	});              	
        else if (efaGUIroot instanceof EfaBoathouseFrame)
        	SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
                    ((EfaBoathouseFrame) efaGUIroot).updateProjectLogbookInfo();
        		}
        	});              	
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
                TxRequestQueue.RQ_QUEUE_STATE.get(stateTransitionRequests.firstElement())), "", null, true);
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

                // ============== time out for synchronization ======
                if ((txq.getState() == TxRequestQueue.QUEUE_IS_SYNCHRONIZING)
                        && ((System.currentTimeMillis() - synchControl.lastSynchStartedMillis) > SYNCHRONIZATION_TIMEOUT)) {
                	//TODO: Code is buggy. See https://github.com/nicmichael/efa/issues/257 for more info.
                	//(Upload) Sync currently will not be stopped after the sync timeout.
                    txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_RESUME);
                    txq.logApiMessage("Synchronization Timeout. "
                             + International.getString("Die Synchronisation wird beendet."), 1);
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
                                suspendQueue(ACTION_TX_PAUSE);
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
                                TaskManager.RequestMessage rq = Transaction
                                        .createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX), getCredentials());
                                iam.sendRequest(rq);
                                txq.setState(QUEUE_IS_WORKING);
                                if (currentState == QUEUE_IS_DISCONNECTED)
                                    Logger.log(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_INFO, International
                                            .getMessage("Kommunikation zu {URL} steht wieder bereit.", efaCloudUrl));
                                else if (currentState == QUEUE_IS_AUTHENTICATING)
                                    txq.synchControl
                                            .logSynchMessage(International.getString("Serverkommunkation gestartet."),
                                                    "", null, true);
                                else
                                    txq.synchControl.logSynchMessage(
                                            International.getString("Serverkommunikation wieder aufgenommen."), "",
                                            null, true);
                                showStatusAtGUI();
                            } else
                                dropInvalidStateChangeRequest();
                            break;
                        case RQ_QUEUE_START_SYNCH_DOWNLOAD:
                        case RQ_QUEUE_START_SYNCH_UPLOAD:
                        case RQ_QUEUE_START_SYNCH_UPLOAD_ALL:
                            // Obsolete from 2.3.1: case RQ_QUEUE_START_SYNCH_DELETE:
                            if ((currentState == QUEUE_IS_WORKING) || (currentState == QUEUE_IS_IDLE)) {
                                if ((queues.get(TX_PENDING_QUEUE_INDEX).size() == 0) &&
                                        (queues.get(TX_BUSY_QUEUE_INDEX).size() == 0)) {
                                    // prepare synchronization
                                    stateTransitionRequests.remove(0);
                                    txq.setState(QUEUE_IS_SYNCHRONIZING);
                                    showStatusAtGUI();
                                    saveAuditInformation();
                                    // start synchronization
                                    txq.synchControl.startSynchProcess(stateChangeRequest);
                                    // use the synchronization trigger also to update parameters and upload statistics and logs.
                                    // Because the queue state is QUEUE_IS_SYNCHRONIZING this transaction will not
                                    // be processed until the synchronization is completed.
                                    if (stateChangeRequest == RQ_QUEUE_START_SYNCH_DOWNLOAD) {
                                        // upload statistics, if due.
                                        if ((System.currentTimeMillis() - lastStatsUpload) > STATS_UPLOAD_PERIOD_MIN) {
                                            appendTransaction(TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.UPLOAD, "zip",
                                                    "filepath;efacloudLogs.zip", "contents;" + getLogsAsZip());
                                            lastStatsUpload = System.currentTimeMillis();
                                        }
                                        // Add a NOP transaction to synchronize the configuration
                                        appendTransaction(TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.NOP, "", "sleep;2");
                                        // Both transactions will use admin credentials in admin mode
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
                                            "", null, false);
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
                            // synchronization activities always use the default credentials, never the admin ones.
                            TaskManager.RequestMessage rq = Transaction
                                    .createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX), credentials);
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
                            boolean pendingHeadIsNop = (queues.get(TX_PENDING_QUEUE_INDEX).firstElement().type ==
                                    Transaction.TX_TYPE.NOP);
                            // the first NOP transaction shall always be alone in a container
                            // to ensure it is retried, until the connections is established.
                            int shiftSize = (pendingHeadIsNop) ? 1 : PENDING_QUEUE_MAX_SHIFT_SIZE;
                            shiftTx(TX_PENDING_QUEUE_INDEX, TX_BUSY_QUEUE_INDEX, ACTION_TX_SEND, 0, shiftSize);
                            // Remove te connection loss indication when the busy queue was cleared.
                            if (txq.getState() == QUEUE_IS_DISCONNECTED)
                                txq.setState((pendingHeadIsNop) ? QUEUE_IS_AUTHENTICATING : QUEUE_IS_WORKING);
                            // read the transactions to use them
                            TaskManager.RequestMessage rq = Transaction
                                    .createIamRequest(queues.get(TX_BUSY_QUEUE_INDEX), getCredentials());
                            iam.sendRequest(rq);
                        }
                        // it occurred that the queue was disconnected, but had no pending transaction. That will
                        // not reconnect then.
                        else if (txq.getState() == QUEUE_IS_DISCONNECTED) {
                            appendTransaction(TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.NOP, "", "sleep;2");
                        }
                        // check whether to start synchronisation, if neither busy nor pending requests are there
                        else if ((txq.getState() == QUEUE_IS_IDLE) &&
                                (polltime - synchControl.lastSynchStartedMillis > synch_period)) {
                            // use the opportunity to clear the done and dropped queue, which will else be a memory leak
                            while (queues.get(TX_DONE_QUEUE_INDEX).size() > DONE_QUEUE_MAX_TXS)
                                queues.get(TX_DONE_QUEUE_INDEX).remove(0);
                            while (queues.get(TX_DROPPED_QUEUE_INDEX).size() > DROPPED_QUEUE_MAX_TXS)
                                queues.get(TX_DROPPED_QUEUE_INDEX).remove(0);
                            // The synch transactions queue is never stored, so it needs not to be read from file.
                            queues.get(TX_SYNCH_QUEUE_INDEX).clear();
                            registerStateChangeRequest(RQ_QUEUE_START_SYNCH_DOWNLOAD);
                        // or run a fast synch poll, if due.
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
                    // pausing write actions shall stay in the queue
                    boolean keepOnPause = (action == ACTION_TX_PAUSE) &&
                            (sourceQueueIndex != TX_SYNCH_QUEUE_INDEX && tx.type.isWriteAction);
                    if (!keepOnPause) {
                        if (destinationQueueIndex == TX_DROPPED_QUEUE_INDEX)
                            txq.logApiMessage("#" + tx.ID + ", " + tx.type + " [" + tx.tablename + "]: " + "Transaction dropped", 1);
                        dest.add(tx);
                        // cut failed transactions length to avoid log overload
                        if (destinationQueueIndex == TX_FAILED_QUEUE_INDEX)
                            tx.shortenRecord(1024);
                        src.remove(tx);
                    }
                }
            }
            i++;
        }
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
     * Return the size of the busy queue
     *
     * @return size of the busy queue
     */
    int getBusyQueueSize() {
        return queues.get(TX_BUSY_QUEUE_INDEX).size();
    }

    /**
     * Create a new transaction with an object which will be notified on the transaction result and append it to the
     * pending queue. Used ONLY for the VERIFY transaction. If the queue is stopped, nothing will be appended.
     * If the queue is authenticating, only "nop" transactions will be appended.
     *
     * @param queueIndex   index of the queu to append the transaction to. Must be TX_PENDING_QUEUE_INDEX or
     *                     TX_FIX_QUEUE_INDEX
     * @param type         type of message to be appended
     * @param tablename    tablename for transaction
     * @param record       record data of the transaction to be created. An array of n "key; value" pairs, all entries csv
     *                     encoded. If there is no record, e. g. for a first keyfixing request, set it to (String[]) null
     */
    public Transaction appendTransaction(int queueIndex, Transaction.TX_TYPE type, String tablename, String... record) {
        if (txq.getState() == QUEUE_IS_STOPPED)
            return null;
        // in authentication mode only structure check and building is allowed
        boolean allowedTransaction = (txq.getState() != QUEUE_IS_AUTHENTICATING) //
                || type == Transaction.TX_TYPE.NOP //
                || type == Transaction.TX_TYPE.SYNCH;
        if (!allowedTransaction)
            return null;
        Transaction tx = new Transaction(-1, type, tablename, record);
        // try to get the queues lock. This will exit with a release lock exception after lock timeout.
        //noinspection StatementWithEmptyBody
        while (refuseQueueLock(queueIndex))
            // wait
            ;
        queues.get(queueIndex).add(tx);
        releaseQueueLock(queueIndex);
        return tx;
    }

    @Override
    public void sendRequest(TaskManager.RequestMessage request) {
        // This is the callback from the InternetAccessManager with the access result.
        // pass the results also through a queue, to make sure, a fast response can not overtake
        // an earlier one which is still be processed.
        queueResponses.add(request);
    }

    /**
     * Little helper to remove a directory like rm -r. Needed to manage logs.
     * see stackoverflow.com/questions/779519/delete-directories-recursively-in-java
     * @param path Root File Path
     * @return true if the file and all sub files/directories have been removed
    */
    public static boolean deleteRecursive(File path){
        if (!path.exists()) return true;
        boolean ret = true;
        if (path.isDirectory()){
            File[] files = path.listFiles();
            if (files != null)
                for (File f : files)
                    ret = ret && deleteRecursive(f);
        }
        return ret && path.delete();
    }

}
