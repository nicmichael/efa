/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.util;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.ProgressMonitor;

public class DownloadThread {

    final static int PROGRESS_TIMETOPOPUP = 1;
    final static int PROGRESS_TIMERINTERVAL = 20;

    private ProgressMonitor progressMonitor;
    private URLConnection conn;
    private String localFile;
    private boolean waitfor;
    private ExecuteAfterDownload afterDownload;
    private Thread thread;
    private int downDone, downTotal;
    private InputStream i;
    private FileOutputStream o;
    private String exceptionText = null;
    private javax.swing.Timer timer;
    private boolean aborted = false;

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
        this.downTotal = 100; // nur ini-Wert
    }

    Thread go() {
        final SwingWorker worker = new SwingWorker() {
            public Object construct() {
                return new ActualTask(afterDownload);
            }
        };
        return (thread = worker.start());
    }

    int getLengthOfTask() {
        return downTotal;
    }

    int getCurrent() {
        return downDone;
    }

    void stop() {
        try {
            i.close();
            o.close();
            exceptionText = International.getString("Abbruch");
        } catch (IOException e) {
            i = null;
            o = null;
        }
    }

    boolean done() {
        return (downTotal == downDone);
    }

    String getMessage() {
        return International.getMessage("{bytesDone} Bytes von {bytesTotal} Bytes ...", downDone, downTotal);
    }

    void setAborted() {
        aborted = true;
    }

    void exit() {
        // thr.destroy();
    }

    private boolean runDownload() {
        timer = new javax.swing.Timer(PROGRESS_TIMERINTERVAL, new TimerListener(this));
        if (progressMonitor != null) {
            progressMonitor.setProgress(0);
            progressMonitor.setMaximum(conn.getContentLength());
            progressMonitor.setMillisToDecideToPopup(PROGRESS_TIMETOPOPUP);
        }
        go();
        timer.start();
        if (waitfor) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
        // if progrssMonitor == null, we run in silent mode
        if (exceptionText != null && progressMonitor != null) {
            Dialog.error(LogString.operationFailed(International.getString("Download"), exceptionText) + "\n"
                    + International.getString("Eventuell wird efa durch eine Firewall blockiert."));
            return false;
        }
        return true;
    }

    public static boolean runDownload(Window frame, URLConnection conn, String remote, String local, boolean waitfor) {
        ProgressMonitor progressMonitor = null;
        if (frame != null) {
            progressMonitor = new ProgressMonitor(frame, International.getString("Download") + " " + remote, "", 0, 100);
        }
        DownloadThread downloadThread = new DownloadThread(progressMonitor, conn, local, waitfor, null);
        return downloadThread.runDownload();
    }

    public static boolean runDownload(Window frame, URLConnection conn, String remote, String local, ExecuteAfterDownload afterDownload) {
        ProgressMonitor progressMonitor = null;
        if (frame != null) {
            progressMonitor = new ProgressMonitor(frame, International.getString("Download") + " " + remote, "", 0, 100);
        }
        DownloadThread downloadThread = new DownloadThread(progressMonitor, conn, local, false, afterDownload);
        return downloadThread.runDownload();
    }

    public static boolean getFile(Window frame, String remote, String local, boolean wait) {
        try {
            URLConnection conn = new URL(remote).openConnection();
            conn.connect();
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

    public static boolean getFile(Window frame, String remote, String local, ExecuteAfterDownload afterDownload) {
        try {
            URLConnection conn = new URL(remote).openConnection();
            conn.connect();
            return runDownload(frame, conn, remote, local, afterDownload);
        } catch (Exception e) {
            if (frame != null) {
                Dialog.error(LogString.operationFailed(International.getString("Download"),e.toString()) + "\n"
                        + International.getString("Eventuell wird efa durch eine Firewall blockiert."));
            }
            return false;
        }
    }


    class ActualTask {

        ActualTask(ExecuteAfterDownload afterDownload) {
            try {
                int BUFSIZE = 1500;
                byte[] buf = new byte[BUFSIZE];
                i = conn.getInputStream();
                o = new FileOutputStream(localFile);
                int c = 0;
                downDone = 0;
                downTotal = conn.getContentLength();
                while ((c = i.read(buf, 0, BUFSIZE)) > 0 && !aborted) {
                    o.write(buf, 0, c);
                    downDone += c;
                }
                i.close();
                o.close();
                if (afterDownload != null) {
                    if (!aborted && downDone == downTotal) {
                        afterDownload.success();
                    } else {
                        afterDownload.failure(International.getString("Abbruch"));
                    }
                }
            } catch (IOException e) {
                exceptionText = e.getMessage();
                if (afterDownload != null) {
                    afterDownload.failure(e.getMessage());
                }
            }
        }
    }

    class TimerListener implements ActionListener {

        private DownloadThread downloadThread;

        public TimerListener(DownloadThread downloadThread) {
            this.downloadThread = downloadThread;
        }

        public void actionPerformed(ActionEvent evt) {
            if (progressMonitor != null) {
                if (progressMonitor.isCanceled() || downloadThread.done()) {
                    if (progressMonitor.isCanceled()) {
                        downloadThread.setAborted();
                    }
                    progressMonitor.close();
//                downloadThread.stop();
                    downloadThread.exit();
                    timer.stop();
                } else {
                    progressMonitor.setNote(downloadThread.getMessage());
                    progressMonitor.setMaximum(downloadThread.getLengthOfTask());
                    progressMonitor.setProgress(downloadThread.getCurrent());
                }
            }
        }
    }
}
