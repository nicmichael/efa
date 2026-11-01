/**
 * Title:        efa - electronic logbook for rowers
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.util;

import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

/**
 * Runs a download in a SwingWorker, with optional ProgressMonitor and optional synchronous wait.
 * 
 * The download can be cancelled by the user via the ProgressMonitor's Cancel button.
 *
 * <p><b>Single-use instances:</b> each {@code DownloadThread} instance is designed
 * for exactly one download. Internal state (progress, error message, the
 * underlying {@code SwingWorker}, ...) is only valid for a single run; calling
 * {@code runDownload()} more than once on the same instance throws an
 * {@link IllegalStateException}. The static convenience methods below
 * ({@code runDownload(...)}, {@code getFile(...)}) always create a new instance
 * per call, so they can be safely invoked multiple times - but each call starts
 * an independent, one-shot download and must be given its own
 * {@link URLConnection} (which itself is also not reusable for more than one
 * download).</p>
 * 
 */
public class DownloadThread {

    final static int PROGRESS_TIMETOPOPUP = 1;
    final static int CANCEL_POLL_INTERVAL = 200; // only for polling the ProgressMonitor's Cancel button
    final static int CONNECT_TIMEOUT_MS = 10000; // 10 seconds
    final static int READ_TIMEOUT_MS = 30000;    // 30 seconds

    private final ProgressMonitor progressMonitor;
    private final URLConnection conn;
    private final String localFile;
    private final boolean waitfor;
    private final ExecuteAfterDownload afterDownload;
    private volatile int downDone, downTotal;
    private volatile String exceptionText = null;
    private javax.swing.Timer cancelPollTimer;
    private Worker worker;
    // Guards against accidentally reusing a DownloadThread instance for more than
    // one download: all state fields above are only valid for a single run.
    private final java.util.concurrent.atomic.AtomicBoolean started = new java.util.concurrent.atomic.AtomicBoolean(false);

    public DownloadThread(ProgressMonitor progressMonitor, 
            URLConnection conn, 
            String local, 
            boolean waitfor, 
            ExecuteAfterDownload afterDownload) {
        this.progressMonitor = progressMonitor;
        this.conn = conn;
        this.localFile = local;
        this.waitfor = waitfor;
        this.afterDownload = afterDownload;
        this.downDone = 0;
        this.downTotal = 100; // just an initial value
    }

    /** 
     * Delivers a message like "1234 Bytes von 5678 Bytes ..." for the ProgressMonitor 
     */
    String getMessage() {
        return International.getMessage("{bytesDone} Bytes von {bytesTotal} Bytes ...", downDone, downTotal);
    }

    /**
	 * Runs the download in a SwingWorker, with optional ProgressMonitor and optional synchronous wait.
	 * @return true if download completed successfully, false if it was cancelled or failed
	 */
    private boolean runDownload() {
        if (!started.compareAndSet(false, true)) {
            // A DownloadThread instance is meant to be used for exactly one download;
            // all its state fields (exceptionText, downDone/downTotal, worker, ...)
            // would otherwise be mixed up between unrelated downloads.
            throw new IllegalStateException("DownloadThread instances must not be reused for more than one download.");
        }
        try {
            if (progressMonitor != null) {
                progressMonitor.setProgress(0);
                long len = conn.getContentLengthLong(); // long instead of int: avoids overflow for files > 2GB
                progressMonitor.setMaximum(len > 0 && len <= Integer.MAX_VALUE ? (int) len : 100);
                progressMonitor.setMillisToDecideToPopup(PROGRESS_TIMETOPOPUP);
            }
        } catch (RuntimeException e) {
            // conn could not be used (e.g. not properly connected) - clean up the
            // connection here as well, since the Worker (which would otherwise do
            // this cleanup) is never started in this case.
            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).disconnect();
            }
            exceptionText = e.toString();
            if (progressMonitor != null) {
                // The Worker (which would otherwise close the ProgressMonitor in its
                // done() method) is never started in this case, so close it here
                // to avoid leaving a stuck/undismissable progress dialog on screen.
                progressMonitor.close();
                Dialog.error(LogString.operationFailed(International.getString("Download"), exceptionText) + "\n"
                        + International.getString("Eventuell wird efa durch eine Firewall blockiert."));
            }
            return false;
        }

        worker = new Worker();

        // ProgressMonitor does not offer a cancel listener, so the Cancel
        // button has to be polled via a timer. The timer itself no longer
        // contributes any progress updates (that is handled by SwingWorker.publish/process).
        if (progressMonitor != null) {
            cancelPollTimer = new javax.swing.Timer(CANCEL_POLL_INTERVAL, e -> {
                if (progressMonitor.isCanceled() && !worker.isDone()) {
                    worker.cancel(true);
                }
                if (worker.isDone()) {
                    cancelPollTimer.stop();
                }
            });
            cancelPollTimer.start();
        }

        worker.execute();

        if (waitfor) {
            waitForWorker();
        }

        // if progressMonitor == null, we run in silent mode
        if (exceptionText != null && progressMonitor != null) {
            Dialog.error(LogString.operationFailed(International.getString("Download"), exceptionText) + "\n"
                    + International.getString("Eventuell wird efa durch eine Firewall blockiert."));
            return false;
        }
        return true;
    }

    /**
     * Synchronously waits for the download to finish.
     *
     * IMPORTANT (Swing/EDT): Almost all callers of getFile(..., true) invoke this
     * method from the Event Dispatch Thread (button handlers). A direct
     * worker.get() would block the EDT for the entire duration of the download:
     * SwingWorker delivers its publish()/process() and done() callbacks via
     * invokeLater() on the EDT, and the javax.swing.Timer used for cancel polling
     * also only fires on the EDT. While worker.get() is blocking, these events
     * would just pile up in the queue and only be processed AFTER the download
     * finishes - the progress display and Cancel button would "freeze" and
     * Windows would mark the application as "Not Responding" (the whole GUI hangs).
     *
     * Therefore - if called on the EDT - a secondary event loop (SecondaryLoop)
     * is used here. It keeps pumping events (repaint, timer, SwingWorker
     * callbacks) while waiting, and is only exited once the worker is done.
     */
    private void waitForWorker() {
        if (SwingUtilities.isEventDispatchThread()) {
            SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            worker.addPropertyChangeListener(evt -> {
                if ("state".equals(evt.getPropertyName())
                        && javax.swing.SwingWorker.StateValue.DONE == evt.getNewValue()) {
                    loop.exit();
                }
            });
            if (!worker.isDone()) {
                loop.enter();
            }
        }
        try {
            worker.get(); // waits for doInBackground() to finish, propagates errors
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (exceptionText == null) {
                exceptionText = International.getString("Abbruch");
            }
        } catch (ExecutionException e) {
            // If doInBackground() threw an unexpected (non-IOException) exception,
            // exceptionText may not have been set there. Catch up here so the
            // caller does not incorrectly assume a successful download (otherwise
            // e.g. an incomplete/empty file could be used further).
            if (exceptionText == null) {
                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                exceptionText = cause.toString();
            }
        }
    }

    /**
	 * Convenience method to run a download with optional ProgressMonitor and optional synchronous wait.
	 * Creates a new, single-use DownloadThread instance for this one download; the
	 * given {@code conn} is consumed for this single download only and must not be
	 * reused afterwards.
	 * @return true if download completed successfully, false if it was cancelled or failed
	 */
    public static boolean runDownload(Window frame, URLConnection conn, String remote, String local, boolean waitfor) {
        ProgressMonitor progressMonitor = null;
        if (frame != null) {
            progressMonitor = new ProgressMonitor(frame, International.getString("Download") + " " + remote, "", 0, 100);
        }
        DownloadThread downloadThread = new DownloadThread(progressMonitor, conn, local, waitfor, null);
        return downloadThread.runDownload();
    }
    
    /**
	 * Convenience method to run a download with optional ProgressMonitor and optional synchronous wait.
	 * Creates a new, single-use DownloadThread instance for this one download; the
	 * given {@code conn} is consumed for this single download only and must not be
	 * reused afterwards.
	 * @return true if download completed successfully, false if it was cancelled or failed
	 * */
    public static boolean runDownload(Window frame, URLConnection conn, String remote, String local, ExecuteAfterDownload afterDownload) {
        ProgressMonitor progressMonitor = null;
        if (frame != null) {
            progressMonitor = new ProgressMonitor(frame, International.getString("Download") + " " + remote, "", 0, 100);
        }
        DownloadThread downloadThread = new DownloadThread(progressMonitor, conn, local, false, afterDownload);
        return downloadThread.runDownload();
    }

    /**
	 * Convenience method to download a file from a URL or local file path, with optional ProgressMonitor and optional synchronous wait.
	 * Each call creates its own new, single-use DownloadThread instance internally,
	 * so this method can safely be called repeatedly (e.g. for multiple files).
	 * @return true if download completed successfully, false if it was cancelled or failed
	 */
    public static boolean getFile(Window frame, String remote, String local, boolean wait) {
        try {
            if (isLocalFile(remote)) {
                return copyLocalFile(remote, local);
            }
            URLConnection conn = openConnection(remote);
            return runDownload(frame, conn, remote, local, wait);
        } catch (Exception e) {
            // if frame==null, we run in "silent" mode
            if (frame != null) {
               Dialog.error(LogString.operationFailed(International.getString("Download"), e.toString()) + "\n"
                       + International.getString("Eventuell wird efa durch eine Firewall blockiert."));
            }
            return false;
        }
    }

    /**
	 * Convenience method to download a file from a URL or local file path, with optional Progress
	 * Monitor and optional synchronous wait, and a callback for success/failure.
	 * Each call creates its own new, single-use DownloadThread instance internally,
	 * so this method can safely be called repeatedly (e.g. for multiple files).
	 *  @return true if download completed successfully, false if it was cancelled or failed
	 */
    public static boolean getFile(Window frame, String remote, String local, ExecuteAfterDownload afterDownload) {
        try {
            if (isLocalFile(remote)) {
                if (copyLocalFile(remote, local)) {
                    afterDownload.success();
                    return true;
                } else {
                    afterDownload.failure(International.getString("Fehler beim Kopieren der Datei"));
                    return false;
                }
            }
            URLConnection conn = openConnection(remote);
            return runDownload(frame, conn, remote, local, afterDownload);
        } catch (Exception e) {
            if (frame != null) {
                Dialog.error(LogString.operationFailed(International.getString("Download"),e.toString()) + "\n"
                        + International.getString("Eventuell wird efa durch eine Firewall blockiert."));
            }
            if (afterDownload != null) {
                afterDownload.failure(e.toString());
            }
            return false;
        }
    }

    /**
     * Checks whether the remote specification refers to a local file
     * (no actual download via a URLConnection required).
     */
    private static boolean isLocalFile(String remote) {
        return remote == null || remote.toLowerCase().startsWith("file://");
    }

    /**
     * Oeffnet eine URLConnection mit sinnvollen Timeouts, damit ein Download
     * nicht unbegrenzt haengen bleibt (z.B. wenn eine Firewall Pakete stillschweigend verwirft).
     */
    private static URLConnection openConnection(String remote) throws IOException {
        URLConnection conn = new URL(remote).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.connect();

        // Check the HTTP response code, if applicable: connect() alone does not
        // fail for error responses like 404/500, so without this check an error
        // page returned by the server could be downloaded as if it were the
        // requested file.
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            int responseCode = httpConn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                String message = responseCode + " " + httpConn.getResponseMessage();
                httpConn.disconnect();
                throw new IOException(message);
            }
        }

        return conn;
    }

    /**
     * Copies a local file (specified by a "file:" URI or UNC path) to another local file.
     * @param remote
     * @param local
     * @return true if the copy was successful, false if it failed
     */
    private static boolean copyLocalFile(String remote, String local) {
        if (remote == null || local == null) {
            Dialog.error(LogString.operationFailed(International.getString("Download"),
                    "remote/local file path is null") + "\n");
            return false;
        }

        try {
            Path source = resolveLocalPath(remote);
            Path target = Paths.get(local);

            if (!Files.exists(source)) {
                Dialog.error(LogString.operationFailed(International.getString("Download"),
                        "Source file not found: " + source) + "\n");
                return false;
            }

            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Logger.log(Logger.INFO, Logger.MSG_EVT_ONLINEUPDATEINFO, "File successfully copied: " + source + " -> " + target);
        } catch (IOException | IllegalArgumentException e) {
            Dialog.error(LogString.operationFailed(International.getString("Download"), e.toString()) + "\n");
            return false;
        }
        return true;
    }

    /**
     * Robustly converts a "remote" path specification (possibly a file: URI,
     * e.g. with a UNC path or URL-encoded characters) into a local Path.
     * Falls back to the classic string-based prefix removal for non-URI-
     * compliant specifications (e.g. unencoded spaces).
     */
    private static Path resolveLocalPath(String remote) {
        if (remote.toLowerCase().startsWith("file:")) {
            try {
                return Paths.get(new URI(remote));
            } catch (URISyntaxException | IllegalArgumentException e) {
                // Fallback for non-URI-compliant specifications (e.g. unencoded spaces)
                String path = remote;
                if (path.toLowerCase().startsWith("file://")) {
                    path = path.substring(7);
                }
                // Windows: "file:///C:/..." -> after removing "file://" we get "/C:/...";
                // remove the leading slash in front of the drive letter
                if (path.matches("^/[a-zA-Z]:.*")) {
                    path = path.substring(1);
                }
                return Paths.get(path);
            }
        }
        return Paths.get(remote);
    }

    /**
     * The actual download work as a proper javax.swing.SwingWorker.
     * doInBackground() runs on the worker thread, process()/done() are guaranteed to run on the EDT.
     */
    private class Worker extends javax.swing.SwingWorker<Void, Integer> {

        private static final int PUBLISH_INTERVAL_MS = 250;

        @Override
        protected Void doInBackground() {
            final int BUFSIZE = 32768; // 32 KB
            byte[] buf = new byte[BUFSIZE];
            downDone = 0;
            long total = conn.getContentLengthLong(); // long instead of int: avoids overflow for files > 2GB
            downTotal = (total > 0 && total <= Integer.MAX_VALUE) ? (int) total : -1;
            long lastPublish = System.currentTimeMillis();

            // try-with-resources: InputStream and FileOutputStream are closed
            // automatically, even on exception or cancellation.
            try (InputStream in = conn.getInputStream();
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(localFile), BUFSIZE)) {

                int c;
                // read from the stream and write to the file, until EOF or cancelled
                // we do not depend on the size (downTotal) being known,
                // as some servers do not provide a Content-Length header
                while (!isCancelled() && (c = in.read(buf, 0, BUFSIZE)) > 0) {
                    out.write(buf, 0, c);
                    downDone += c;
                    // Throttle progress updates: only publish every PUBLISH_INTERVAL_MS,
                    // to avoid flooding the EDT with updates on fast/chunky connections.
                    long now = System.currentTimeMillis();
                    if (now - lastPublish >= PUBLISH_INTERVAL_MS) {
                        publish(downDone);
                        lastPublish = now;
                    }
                }

                // Make sure the final state is always reported, even if the last
                // chunk(s) were suppressed by the throttling above.
                publish(downDone);

                if (!isCancelled()) {
                    // Make sure done() correctly recognizes that the download has finished
                    // (relevant e.g. when the server does not provide a Content-Length header,
                    // in which case downTotal == -1 and would otherwise never equal downDone)
                    downTotal = downDone;
                } else {
                    exceptionText = International.getString("Abbruch");
                }
            } catch (IOException e) {
                exceptionText = (e.getMessage() != null) ? e.getMessage() : e.toString();
            } catch (RuntimeException e) {
                // Do not swallow unexpected errors (e.g. NPE) - otherwise runDownload()
                // would incorrectly report a successful download.
                exceptionText = e.toString();
            } finally {
                // Cleanly tear down/release the connection, even on error or cancellation,
                // so no sockets/connections are left open (a resource/performance issue
                // for repeated downloads, e.g. in DownloadMultipleFilesDialog).
                if (conn instanceof HttpURLConnection) {
                    ((HttpURLConnection) conn).disconnect();
                }
                // If the download was cancelled or failed, remove the partially written
                // target file so callers can never accidentally work with an incomplete file.
                if (isCancelled() || exceptionText != null) {
                    try {
                        Files.deleteIfExists(Paths.get(localFile));
                    } catch (IOException ignore) {
                        // best effort cleanup only
                    }
                }
            }
            return null;
        }

        /** 
		 * Called on the EDT after publish() was invoked in doInBackground().
		 * Multiple publish() calls may be coalesced, so chunks is delivered as a list.
		 */
        @Override
        protected void process(List<Integer> chunks) {
            // runs on the EDT; multiple publish() calls may be coalesced
            if (progressMonitor != null && !chunks.isEmpty()) {
                int latest = chunks.get(chunks.size() - 1);
                progressMonitor.setNote(getMessage());
                if (downTotal > 0) {
                    progressMonitor.setMaximum(downTotal);
                }
                progressMonitor.setProgress(latest);
            }
        }

        /**
		 * Called on the EDT after doInBackground() has finished (whether
		 * successful, with an exception, or cancelled). GUI updates can be done here.
		 */
        @Override
        protected void done() {
            // guaranteed to run on the EDT - no manual invokeLater() needed
            if (progressMonitor != null) {
                progressMonitor.close();
            }
            if (afterDownload != null) {
                if (!isCancelled() && exceptionText == null) {
                    afterDownload.success();
                } else {
                    afterDownload.failure(exceptionText != null ? exceptionText : International.getString("Abbruch"));
                }
            }
        }
    }
}
