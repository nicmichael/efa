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

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * <p>Little singleton helper class to load a resource from web in a different task.</p>
 * <p>Encoding
 * of TaskManager.RequestMessage for the access request is:<ul> <li> title = download URL </li> <li> text = file system
 * location to store file to, or call_post parameters in case of call_post </li>
 * <li> type = type of internet Access. TYPE_POST_PARAMETERS, TYPE_GET_BINARY and TYPE_GET_TEXT are
 * available
 * </li> <li> value = text encoding. Available are ISO-8859-1 (default) or UTF-8, use parameters:
 * VALUE_TEXT_ENCODING_ISO8859_1, VALUE_TEXT_ENCODING_UTF8 </li> </ul> </p> <p>Encoding of TaskManager.RequestMessage
 * for the callback is:<ul> <li> title = file system location to which the file was stored, if a file is stored. Dialog
 * title for progress messages. postURLplus, if a post request was aborted. Else it is empty</li> <li> text = result
 * string, Usually empty for get, server response for call_post and error mesage in case of error. </li> <li> type =
 * type of message. TYPE_FILE_SIZE_INFO, TYPE_PROGRESS_INFO, TYPE_COMPLETED, TYPE_ABORTED are available
 * </li> <li> value = filesize in byte for TYPE_FILE_SIZE_INFO, part downloaded (0 .. 1) for
 * TYPE_PROGRESS_INFO and TYPE_COMPLETED_INFO</li> </ul> </p>
 *
 * @author mgSoft
 */
public class InternetAccessManager implements TaskManager.RequestDispatcherIF {

    public static final int TYPE_POST_PARAMETERS = 0;
    public static final int TYPE_GET_BINARY = 1;
    public static final int TYPE_GET_TEXT = 2;

    public static final int VALUE_TEXT_ENCODING_ISO8859_1 = 0;
    public static final int VALUE_TEXT_ENCODING_UTF8 = 1;

    public static final int TYPE_PROGRESS_INFO = 1;
    public static final int TYPE_ABORTED = 2;
    public static final int TYPE_COMPLETED = 3;
    public static final int TYPE_PENDING = 4;
    // public static final int TYPE_FILE_SIZE_INFO = 5;

    private static final int UPDATE_INTERVAL_BYTES = 16 * 1024;
    private static final int INFO_INTERVAL_MILLIS = 500;

    // delay in ms between two "please wait ..." messages..
    public static int MONITOR_PERIOD = 5000;
    // count of monitor periods after which the transaction is aborted.
    public static int TIME_OUT_MONITOR_PERIODS = 5;
    // Period of polls on send queue
    private static final int POLL_PERIOD_MS = 100;
    // Start delay of polls on send queue
    private static final int POLL_START_DELAY_MS = 3000;

    private static InternetAccessManager iam = null;
    private final TaskManager taskManager;
    private final InternetAccessHandler internetAccessHandler;
    private TaskManager.RequestDispatcherIF callback;
    private TaskManager.RequestMessage callBackMsg;
    // for debugging set debugFilePath to a valid location.
    public String debugFilePath = null;

    /**
     * Simple text reader, see stackoverflow.com/questions/4328711/read-url-to-string-in-few-lines-of-java-code.
     * This is synchronous, i. e. it pauses the program execution until the response is available.
     * So be careful when using it.
     *
     * @param url url to read text from
     * @param postParamaters parameters to be posted. Set "" to create get request.
     * @return the text retrieved. On errors: an error message preceded by "#ERROR: "
     */
    public static String getText(String url, String postParamaters) {
        URL website;
        try {
            website = new URL(url);
        } catch (MalformedURLException mfe) {
            return "#ERROR: " + mfe;
        }
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) website.openConnection();
        } catch (IOException ioe) {
            return "#ERROR: " + ioe;
        }
        if (! postParamaters.isEmpty()) {
            try {
                connection.setRequestMethod("POST");
            } catch (ProtocolException pe) {
                return "#ERROR: " + pe;
            }
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + postParamaters.getBytes().length);
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            // Send request
            DataOutputStream wr;
            try {
                wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(postParamaters);
                wr.flush();
                wr.close();
            } catch (IOException ioe) {
                return "#ERROR: " + ioe;
            }
        }

        BufferedReader in;
        StringBuilder response = new StringBuilder();
        try {
            in = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            in.close();
        } catch (IOException ioe) {
            return "#ERROR: " + ioe;
        }
        return response.toString();
    }

    /**
     * Constructor. Instantiates the corresponding task manager. Private for singleton usage.
     */
    private InternetAccessManager() {
        internetAccessHandler = new InternetAccessHandler();
        this.taskManager = new TaskManager(internetAccessHandler, POLL_PERIOD_MS, POLL_START_DELAY_MS);
    }

    /**
     * Get the internet access manager. It decouples internet post and response from program execution.
     *
     * @return the one and only instance of internet access to the efaCloud server.
     */
    public static InternetAccessManager getInstance() {
        if (iam == null)
            iam = new InternetAccessManager();
        return iam;
    }

    /**
     * @param request <p>request sent. Shall be <ol><li>title = URL, or postURLplus including
     *                parameters, for generation see petPostURLplus method.</li><li>text = path in file system to which
     *                the response shall be stored. May be empty or null on call_post requests, then a text response is
     *                expected and the text returned.<li>type = see type constants of InternetAccessManager
     *                class</li><li>value = no relevance .</li></li></ol></p><p>In the cal back expect the following
     *                parameters:
     *                <ol><li>title = path to which the response was saved to in case of
     *                success</li><li>text = message recieved, e.g. relevant at simple call_post
     *                requests.</li><li>type = see InternetAccessManager class
     *                constants</li><li>value = filesize
     *                received.</li></ol></p>
     */
    @Override
    public void sendRequest(TaskManager.RequestMessage request) {
        /* if (debug) {
            debugLog.append(request).append("\n");
            callBackMsg = new TaskManager.RequestMessage("debugging", "0;300;ok",
                    TYPE_COMPLETED, 0, this);
            if (callback != null) callback.sendRequest(callBackMsg);
        }
        else */
        taskManager.addTask(request);
    }

    /**
     * Terminate the internet access manager by terminating the task manager and dropping the link to the singleton
     * InternetAccessManager instance. Call this method prior to dropping the task manager, e. g. by re-instantiation.
     */
    public void cancel() {
        taskManager.cancel();
        iam = null;
    }

    /**
     * This here is the real worker class, wrapped in the manager. By this means it can be achieved, that the public
     * method handleRequest of the RequestHandlerIF is not publicly visible.
     */
    private class InternetAccessHandler implements TaskManager.RequestHandlerIF {

        final InternetAccessMonitor iamMonitor;
        final InternetAccessManager iamParent = InternetAccessManager.this;

        HttpURLConnection connection;
        InputStream inputStream;

        InternetAccessHandler() {
            iamMonitor = new InternetAccessMonitor();
        }

        /**
         * To time out, close down the input stream and the connection.
         */
        private void timeOut() throws IOException {
            if (inputStream != null)
                inputStream.close();
            if (connection != null)
                connection.disconnect();
        }

        /**
         * <p></p>Execute a post and return the result as TaskManager.RequestMessage to the
         * callback. The result will also be stored to outFpath, if outFpath is not empty. The message returned
         * contains:<ul><li>title: the outFpath</li><li>text: the text returned, if the content type of the response was
         * "text..." or possibly an error message</li><li>type: the result type, see Pparameters of this class
         * definition</li><li>value: always 0.0</li><li>sender: this instance</li> </ul><p> Based on a snippet taken
         * from "www.xyzws.com/Javafaq/how-to-use-httpurlconnection-call_post-data-to-web-server/139"</p>
         *
         * @param postURLplus URL for call_post request plus "?" plus encoded parameters for POST request. The
         *                    urlParameters is a URL encoded string. Example: String urlParameters = "fName=" +
         *                    URLEncoder.encode("???", "UTF-8") + "&lName=" + URLEncoder.encode("???", "UTF-8")
         * @param outFpath    file to store response to. The response may be text or binary. If outFpath is empty,
         *                    nothing will be stored, e. g. for expected text.
         */
        private void executePost(String postURLplus, String outFpath) {

            URL url;
            String[] postURLplusParts = postURLplus.split("\\?", 2);
            String postURL = postURLplusParts[0];
            String urlParameters = postURLplusParts[1];

            try {
                // Create connection
                url = new URL(postURL);
                connection = (HttpURLConnection) url.openConnection();
                iamMonitor.notifyActivity();
            } catch (Exception e) {
                callBackMsg = new TaskManager.RequestMessage(postURL,
                        String.format("#ERROR: Failed to open %s: %s", postURL, e.getMessage()), TYPE_ABORTED, 0.0,
                        iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);
                return;
            }
            try {
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                connection.setRequestProperty("Content-Length", "" + urlParameters.getBytes().length);
                connection.setRequestProperty("Content-Language", "en-US");

                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                // Send request
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                iamMonitor.notifyActivity();
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
                iamMonitor.notifyActivity();

                int resp = connection.getResponseCode();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    callBackMsg = new TaskManager.RequestMessage(postURLplus,
                            String.format("#ERROR: url does not take the post call to %s with resp code HTTP %s",
                                    postURL, resp), TYPE_ABORTED, 0.0, this);
                    if (callback != null)
                        callback.sendRequest(callBackMsg);
                    return;
                }

                // Get Response, either as text or as binary.
                inputStream = connection.getInputStream();
                String contentType = connection.getContentType().toLowerCase(Locale.US);
                int fileSize = connection.getContentLength();
                StringBuilder resb = new StringBuilder();
                if (contentType.contains("text")) {
                    ArrayList<String> rlist = readTextInputStream("UTF-8", outFpath, fileSize);
                    for (String line : rlist)
                        resb.append(line).append('\n');
                } else if ((outFpath != null) && !outFpath.isEmpty()) {
                    saveInputStreamToFile(outFpath, fileSize);
                }
                connection.disconnect();
                // call back
                callBackMsg = new TaskManager.RequestMessage(outFpath, resb.toString(), TYPE_COMPLETED, fileSize,
                        iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);
            } catch (Exception e) {
                callBackMsg = new TaskManager.RequestMessage(postURLplus,
                        String.format("#ERROR: Exception downloading %s: %s", postURL, e.getMessage()), TYPE_ABORTED,
                        0.0, iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);
            }
        }

        /**
         * Show download progress. Will do nothing, if callback was not set.
         *
         * @param bytesRead currently avaiöabe bytes read.
         * @param fileSize  full file size to download, if known.
         */
        private void showProgress(String filePath, int bytesRead, int fileSize) {
            if (callback == null)
                return;
            if (fileSize <= 0)
                callback.sendRequest(
                        new TaskManager.RequestMessage("Downloading ...", filePath, TYPE_PROGRESS_INFO, 0.0,
                                iamParent));
            else
                callback.sendRequest(new TaskManager.RequestMessage("Downloading ...", filePath, TYPE_PROGRESS_INFO,
                        ((double) bytesRead) / fileSize, iamParent));
        }

        /**
         * Save an Input stream to a binary file. Used for both get and Post requests.
         *
         * @param outFpath    path of file to save input stream to. File will be overwritten without warning. Missing
         *                    paths will be created. Set to null or "" to skip writing.
         * @param charsetName name of character set to be used, e. g. UTF-8
         * @param fileSize    size of file usually as reported in the return protocol of the call_post or get request.
         * @return the read file contents as an Array List of lines read.
         * @throws IOException any internet access exception this may be.
         */
        private ArrayList<String> readTextInputStream(String charsetName, String outFpath, int fileSize) throws
                IOException {

            int bytesRead = 0;
            int lastUpdate = 0;
            ArrayList<String> fileContents = new ArrayList<String>();

            InputStreamReader inReader = new InputStreamReader(inputStream, charsetName);
            BufferedReader textInputReader = new BufferedReader(inReader);

            String inputLine = textInputReader.readLine();
            long lastInfoAt = System.currentTimeMillis();
            try {
                while (inputLine != null) {
                    fileContents.add(inputLine);
                    iamMonitor.notifyActivity();
                    bytesRead = bytesRead + inputLine.length();
                    if (bytesRead > (lastUpdate + UPDATE_INTERVAL_BYTES)) {
                        lastUpdate = bytesRead;
                        if (System.currentTimeMillis() - lastInfoAt > INFO_INTERVAL_MILLIS) {
                            showProgress(outFpath, bytesRead, fileSize);
                            lastInfoAt = System.currentTimeMillis();
                        }
                    }
                    inputLine = textInputReader.readLine();
                }
            } catch (IOException e) {
                fileContents = null;
                callBackMsg = new TaskManager.RequestMessage(outFpath,
                        String.format("#ERROR: Exception downloading to file %s: %s", outFpath, e.getMessage()),
                        TYPE_ABORTED, 0.0, iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);
            }
            try {
                textInputReader.close();
            } catch (Exception ignored) {
            }
            iamMonitor.notifyActivity();
            if ((outFpath != null) && !outFpath.isEmpty() && (fileContents != null)) {
                //noinspection ResultOfMethodCallIgnored
                new File(outFpath).getParentFile().mkdirs();
                FileOutputStream out = new FileOutputStream(outFpath, false);
                TextResource.writeContents(out, fileContents, false);
            }
            return fileContents;
        }

        /**
         * Save an Input stream to a binary file. Used for both get and Post requests.
         *
         * @param outFpath path of file to save input stream to. File will be overwritten without warning. Missing paths
         *                 will be created.
         * @param fileSize size of file usually as reported in the return protocol of the call_post or get request.
         * @throws IOException any internet access exception this may be.
         */
        private void saveInputStreamToFile(String outFpath, long fileSize) throws IOException {

            int bytesRead = 0;
            int lastUpdate = 0;
            //noinspection ResultOfMethodCallIgnored
            new File(outFpath).getParentFile().mkdirs();
            FileOutputStream fos = new java.io.FileOutputStream(outFpath);
            BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);

            byte[] data = new byte[1024];
            int x;
            long lastInfoAt = System.currentTimeMillis();
            try {
                // this may be interrupted via an exception in case of time out by closing of
                // inputStream.
                while ((x = inputStream.read(data, 0, 1024)) >= 0) {
                    bytesRead = bytesRead + x;
                    bout.write(data, 0, x);
                    if (bytesRead > (lastUpdate + UPDATE_INTERVAL_BYTES)) {
                        lastUpdate = bytesRead;
                        iamMonitor.notifyActivity();
                        if (System.currentTimeMillis() - lastInfoAt > INFO_INTERVAL_MILLIS) {
                            showProgress(outFpath, bytesRead, (int) fileSize);
                            lastInfoAt = System.currentTimeMillis();
                        }
                    }
                }
            } catch (IOException e) {
                // typically this would happen, when a internet access timeout occurs.
                try {
                    fos.close();
                    bout.close();
                    fos = null;
                    bout = null;
                    //noinspection ResultOfMethodCallIgnored
                    new File(outFpath).delete();
                } catch (Exception ignored) {
                }
                callBackMsg = new TaskManager.RequestMessage(outFpath,
                        String.format("#ERROR: Exception downloading to file %s: %s", outFpath, e.getMessage()),
                        TYPE_ABORTED, 0.0, iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);
            }
            if (fos != null)
                fos.flush();
            if (bout != null)
                bout.flush();
            if (fos != null)
                fos.close();
            if (bout != null)
                bout.close();
            iamMonitor.notifyActivity();
            try {
                inputStream.close();
            } catch (Exception ignored) {
            }
        }

        /**
         * Retrieve a web text resource using the charset provided.
         *
         * @param getURL      url to get text from
         * @param outFpath    file path to put the text to.
         * @param charsetName charset to be used for reading. Passed to the InputStreamReader for retrieving the
         *                    resource. Thus, even, if the retrieved file has an appropriate charset meta tag, this will
         *                    not be recognized
         */
        private void executeGetForText(String getURL, String outFpath, String charsetName) {

            int fileSize;

            URL urlToRead;
            URLConnection urlConnection;
            try {
                urlToRead = new URL(getURL);
                urlConnection = urlToRead.openConnection();
                urlConnection.connect();
                fileSize = urlConnection.getContentLength();
                iamMonitor.notifyActivity();
            } catch (Exception e) {
                callBackMsg = new TaskManager.RequestMessage(outFpath,
                        String.format("#ERROR: Failed to open %s: %s", getURL, e.getMessage()), TYPE_ABORTED, 0.0,
                        iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);
                return;
            }

            try {
                inputStream = urlToRead.openStream();
                readTextInputStream(charsetName, outFpath, fileSize);

                // call back
                callBackMsg = new TaskManager.RequestMessage(outFpath, "", TYPE_COMPLETED, fileSize, iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);

            } catch (IOException e) {
                callBackMsg = new TaskManager.RequestMessage(outFpath,
                        String.format("#ERROR: Exception downloading %s to %s: %s", getURL, outFpath, e.getMessage()),
                        TYPE_ABORTED, fileSize, iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);
            }
        }

        /**
         * Retrieve a web binary resource into the provided file path.
         *
         * @param getURL   url to get text from
         * @param outFpath file path to put the data to.
         */
        private void executeGetForBinary(String getURL, String outFpath) {
            int fileSize;
            URL urlToRead;
            try {
                urlToRead = new URL(getURL);
                URLConnection urlConnection = urlToRead.openConnection();
                urlConnection.connect();
                fileSize = urlConnection.getContentLength();
                iamMonitor.notifyActivity();
            } catch (Exception e) {
                callBackMsg = new TaskManager.RequestMessage(outFpath,
                        String.format("#ERROR: Failed to open %s: %s", getURL, e.getMessage()), TYPE_ABORTED, 0.0,
                        iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);
                return;
            }
            // read file
            /*
             * snippet from
             * http://stackoverflow.com/questions/5867189/android-how-to
             * -download-binary-files-from-the-internet
             */
            try {
                inputStream = new java.io.BufferedInputStream(new java.net.URL(getURL).openStream());
                saveInputStreamToFile(outFpath, fileSize);
                // call back
                callBackMsg = new TaskManager.RequestMessage(outFpath, "", TYPE_COMPLETED, fileSize, iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);

            } catch (Exception e) {
                callBackMsg = new TaskManager.RequestMessage(outFpath,
                        String.format("#ERROR: Exception downloading %s to %s: %s", getURL, outFpath, e.getMessage()),
                        TYPE_ABORTED, 0.0, iamParent);
                if (callback != null)
                    callback.sendRequest(callBackMsg);
            }
        }

        /**
         * Note: do not use by main Thread to avoid "android.os.NetworkOnMainThreadException" which will show up, but
         * most typically not be visible.
         */
        @Override
        public void handleRequest(TaskManager.RequestMessage msg) {
            if (msg == null)
                return;
            // Debugging support.
            if (debugFilePath != null) {
                TextResource.writeContents(debugFilePath,
                        "TaskManager.RequestMessage msg: \ntitle: " + msg.title + "\ntext: " + msg.text, true);
            }
            String enc = "UTF-8";
            if (((int) msg.value) == VALUE_TEXT_ENCODING_ISO8859_1)
                enc = "ISO-8859-1";
            if (((int) msg.value) == VALUE_TEXT_ENCODING_UTF8)
                enc = "UTF-8";
            callback = ((msg.sender instanceof TaskManager.RequestDispatcherIF)) ?
                    (TaskManager.RequestDispatcherIF) msg.sender : null;
            if ((msg.sender != null) && !(msg.sender instanceof TaskManager.RequestDispatcherIF))
                System.out.printf("Warning: InternetAccessManager call with invalid callback %s%n",
                        msg.sender.toString());
            iamMonitor.start();
            msg.started = System.currentTimeMillis();
            // POST to URL
            if (msg.type == TYPE_POST_PARAMETERS)
                executePost(msg.title, msg.text);
                // GET from URL
            else if (msg.type == TYPE_GET_BINARY)
                executeGetForBinary(msg.title, msg.text);
            else if (msg.type == TYPE_GET_TEXT)
                executeGetForText(msg.title, msg.text, enc);
            msg.completed = System.currentTimeMillis();
            iamMonitor.stop();
            callback = null;
        }
    }

    /**
     * This here is a wathdog class. If internet access times out, it will close all activity.
     */
    private class InternetAccessMonitor {

        final InternetAccessManager iamParent = InternetAccessManager.this;
        private boolean activityBusy = false;
        private boolean activityRegistered = true;
        private int idleCounter = 0;

        InternetAccessMonitor() {
            TimerTask activityMonitorTask = new TimerTask() {
                public void run() {
                    if (activityBusy) {
                        if (!activityRegistered) {
                            callBackMsg = new TaskManager.RequestMessage("Please wait...", "Internet access pending.",
                                    TYPE_PENDING, 0.0, InternetAccessMonitor.this.iamParent);
                            idleCounter++;
                            if (idleCounter > TIME_OUT_MONITOR_PERIODS) {
                                callBackMsg = new TaskManager.RequestMessage("Please wait...",
                                        "Internet access aborted. Time out.", TYPE_ABORTED, 0.0,
                                        InternetAccessMonitor.this.iamParent);
                                try {
                                    InternetAccessMonitor.this.iamParent.internetAccessHandler.timeOut();
                                } catch (Exception ignored) {
                                }
                            }
                            if (callback != null)
                                callback.sendRequest(callBackMsg);
                        } else
                            activityRegistered = false;
                    }
                }
            };
            Timer activityMonitor = new Timer();
            activityMonitor.scheduleAtFixedRate(activityMonitorTask, MONITOR_PERIOD, MONITOR_PERIOD);
        }

        /**
         * The monitor will be started with all acitivities triggered. Activities like opening a connection or reading
         * of bytes will be registered. This can manage a regular time out for internet connectivity failurs.
         */
        private void start() {
            activityBusy = true;
            activityRegistered = true;
            idleCounter = 0;
        }

        /**
         * Call this method to register any activity at the internaet.
         */
        private void notifyActivity() {
            activityRegistered = true;
            idleCounter = 0;
        }

        /**
         * Stop the monitor after completion or time out of an internet activity.
         */
        private void stop() {
            activityBusy = false;
            activityRegistered = false;
            idleCounter = 0;
        }

    }

}