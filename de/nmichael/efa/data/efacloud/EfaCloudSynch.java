/*
  Title:        efa - elektronisches Fahrtenbuch für Ruderer Copyright:    Copyright (c) 2001-2011
  by Nicolas Michael Website:      http://efa.nmichael.de/ License:      GNU General Public License
  v2

  @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.efacloud;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.ProgressTask;

import java.util.ArrayList;

public class EfaCloudSynch {

    public enum Mode {
        upload, download, none
    }

    private final Mode mode;
    private final TableBuilder.RecordTypeDefinition[] synchTables;
    private final boolean synchProject;
    private final boolean synchConfig;
    private StringBuilder synchErrors;
    private SynchTask synchTask;
    private final int totalWork;
    private int totalWorkDone = 0;

    private static ProgressDialog progressDialog = null;

    /**
     * Constructor. Note: if synchProject || synchConfig == false, nothing will be done.
     *
     * @param mode the mode of synchronisation
     */
    private EfaCloudSynch(Mode mode, boolean synchProject, boolean synchConfig) {
        this.synchConfig = synchConfig;
        this.synchProject = synchProject;
        this.mode = mode;
        synchTables = Daten.tableBuilder.getTables(synchProject, synchConfig);
        totalWork = synchTables.length;
    }

    /**
     * Upload all data of the synchTables set. Make sure, the synchTables set is appropriately set
     * before. The method updates the progress task and returns only after all uploads are
     * completed.
     *
     * @return the count of successfully processed tables. And the synchErrors StringBuilder will be
     * an empty String on complete success, else an error description.
     */
    private int uploadData() {
        int tablesSuccessful = 0;
        synchErrors = new StringBuilder();
        int n = 0;
        for (TableBuilder.RecordTypeDefinition data : synchTables) {
            n++;   // only for error logging
            try {
                if (!data.persistence.isStorageObjectOpen()) data.persistence.openStorageObject();
                String synchError = data.persistence.uploadAllRecords(this);
                if (synchError.isEmpty()) tablesSuccessful++;
                else synchErrors.append(synchError);
            } catch (Exception e) {
                logMsg(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR, LogString
                        .efaCloudSynchFailed("(" + n + ")",
                                data.persistence.getStorageObjectDescription(), e.toString()));
                Logger.logdebug(e);
            }
            if (synchTask != null) {
                synchTask.setCurrentWorkDone(++totalWorkDone);
            }
        }
        return tablesSuccessful;
    }

    /**
     * Download data for synchronization. Daten.tableBuilder.tablesToUpdate must be set prior to
     * this call. The method updates the progress task and returns only after all downloads are
     * completed.
     *
     * @return the count of successfully processed tables. And the synchErrors StringBuilder will be
     * an empty String on complete success, else an error description.
     */
    private int downloadData() {
        int tablesSuccessful = 0;
        synchErrors = new StringBuilder();
        ArrayList<TableBuilder.RecordTypeDefinition> synchTables =
                Daten.tableBuilder.tablesToUpdate;
        for (TableBuilder.RecordTypeDefinition data : synchTables) {
            try {
                if (!data.persistence.isStorageObjectOpen()) data.persistence.openStorageObject();
                String csvString = data.persistence
                        .downloadRecords(this, synchTask.lastModifiedLaterThan);
                if (csvString.startsWith("#")) logMsg(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR,
                        LogString.efaCloudSynchFailed(data.persistence.getUID(),
                                data.persistence.getStorageObjectDescription(), csvString));
                else {
                    String synchError = data.persistence
                            .readString(csvString, synchTask.lastModifiedLaterThan == 0);
                    if (synchError.isEmpty()) tablesSuccessful++;
                    else synchErrors.append(synchError);
                }
            } catch (Exception e) {
                logMsg(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR, LogString
                        .efaCloudSynchFailed(data.persistence.getUID(),
                                data.persistence.getStorageObjectDescription(), e.toString()));
                Logger.logdebug(e);
            }
            if (synchTask != null) {
                synchTask.setCurrentWorkDone(++totalWorkDone);
            }
        }
        return tablesSuccessful;
    }

    /**
     * run an upload to or download from the efaCloud server.
     *
     * @param synchTask the asynchronous task which runs the upload
     */
    private void runSynch(SynchTask synchTask) {

        if (synchTables == null || synchTables.length == 0) return;
        this.synchTask = synchTask;

        try {
            String currentProjectName = (Daten.project == null) ? null : Daten.project
                    .getProjectName();
            String items = synchProject && synchConfig ? International
                    .getString("Projekt") + " '" + currentProjectName + "' " + International
                    .getString("und") + " " + International
                    .getString("efa-Konfiguration") : synchProject ? International
                    .getString("Projekt") + " '" + currentProjectName + "'" : International
                    .getString("efa-Konfiguration");
            logMsg(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_STARTED, International
                    .getMessage("Starte efaCloud upload von {items} auf {efacloudURL} ...",
                            items, Daten.project.getProjectRecord().getEfaCoudURL()));

            int cnt = (synchTask.mode == Mode.upload) ? uploadData() : downloadData();
            logMsg(Logger.INFO, Logger.MSG_EFACLOUDSYNCH_FINISHEDINFO, International
                    .getMessage("{n} Objekte ohne Fehler synchronisiert.", cnt));
            if (!synchErrors.toString().isEmpty())
                logMsg(Logger.WARNING, Logger.MSG_EFACLOUDSYNCH_FINISHEDWITHERRORS, LogString
                        .efaCloudSynchFailed(International.getString("Synchronisation"),
                                International.getString("Synchronisation mit Fehlern abgeschlossen."),
                                synchErrors.toString()));
        } catch (Exception e) {
            logMsg(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_FAILED, LogString
                    .operationFailed(International.getString("EfaCloudSynch"), e.toString()));
            Logger.logdebug(e);
        } finally {
            TxRequestQueue.getInstance().synchBusy = false;
        }
    }

    /**
     * Little helper method for logging messages to both the progress screen and the prgram log.
     *
     * @param type type of message to be logged
     * @param key  short standardized key indication the message content
     * @param msg  the message content.
     */
    public void logMsg(String type, String key, String msg) {
        if (synchTask.silent) return;
        Logger.log(type, key, msg);
        if (synchTask != null && !type.equals(Logger.DEBUG)) {
            synchTask.logInfo(msg + "\n");
        }
    }

    /**
     * Run Method for uploading a project to EfaCloud. This method will instantiate the needed
     * synchronisation task which will build its own EfaCloudSynch object and run the respective
     * task while updating the progress display.
     *
     * @param parentDialog          the parent dialog which triggered the upload and will be parent
     *                              for the progress display.
     * @param mode                  the mode of synchronization
     * @param synchProject          set true to also synchronize the project data
     * @param synchConfig           set true to also synchronize the configuration data
     * @param lastModifiedLaterThan synchronize only those record which have a lastModdified
     *                              timestamp greater than the provided value.
     * @param silent                true to suppress the progress dialog.
     */
    public static void runEfaCloudSynchTask(BaseDialog parentDialog, Mode mode,
                                            boolean synchProject, boolean synchConfig,
                                            long lastModifiedLaterThan, boolean silent) {
        SynchTask synchTask = new SynchTask(mode, synchProject, synchConfig, lastModifiedLaterThan,
                silent);
        String progressDialogTitle;
        switch (mode) {
            case download:
                progressDialogTitle = International.getString("Download vom efaCloud Server");
                break;
            case upload:
                progressDialogTitle = International
                        .getString("Upload auf den efaCloud Server");
                break;
            default:
                progressDialogTitle = "Synchronisation mit efaCloud Server";
        }
        if (parentDialog != null)
            progressDialog = new ProgressDialog(parentDialog, progressDialogTitle, synchTask,
                    false);
        TxRequestQueue.getInstance().synchBusy = true;
        synchTask.start();
        if (!silent && (progressDialog != null)) {
            // The download synchTask must use a non modal dialog, because it is called from a
            // transaction response handling. If it does not return, this block the transaction
            // queue polling and therefore prevents the synchronisation from happening.
            if (mode == Mode.download) progressDialog.showNonModalDialog();
            else progressDialog.showDialog();
        }
    }

    /**
     * This task is run asynchronously and still provides a progress display, because it may require
     * some time.
     */
    public static class SynchTask extends ProgressTask {

        private final EfaCloudSynch efaCloudSynch;
        final long lastModifiedLaterThan;
        final boolean silent;
        final Mode mode;
        boolean success = false;

        /**
         * Constructor for Creating an Upload or Download
         *
         * @param mode                  download or upload.
         * @param synchProject          synchronize project files
         * @param synchConfig           synchronize configuration files
         * @param lastModifiedLaterThan synchronize only those record which have a lastModdified
         *                              timestamp greater than the provided value.
         * @param silent                true to suppress the progress dialog.
         */
        public SynchTask(Mode mode, boolean synchProject, boolean synchConfig,
                         long lastModifiedLaterThan, boolean silent) {
            super();
            this.mode = mode;
            this.lastModifiedLaterThan = lastModifiedLaterThan;
            this.silent = silent;
            efaCloudSynch = new EfaCloudSynch(mode, synchProject, synchConfig);
        }

        /**
         * run method for this ProgressTask Runnable
         */
        public void run() {
            setRunning(true);
            success = false;
            efaCloudSynch.runSynch(this);
            setDone();
            setRunning(false);
            if (progressDialog != null) progressDialog.dispose();
        }

        /**
         * Simple getter, required by ProgressTask extension
         *
         * @return absolute work value
         */
        public int getAbsoluteWork() {
            return efaCloudSynch.totalWork;
        }

        /**
         * Simple getter, required by ProgressTask extension
         *
         * @return message to be shown upon synch task completion
         */
        public String getSuccessfullyDoneMessage() {
            if (success) {
                switch (efaCloudSynch.mode) {
                    case upload:
                        return LogString.operationSuccessfullyCompleted(
                                International.getString("Upload auf den efaCloud Server"));
                    case download:
                        return LogString.operationSuccessfullyCompleted(
                                International.getString("Download vom efaCloud Server"));
                }
                return LogString
                        .operationSuccessfullyCompleted(International.getString("Synchronisation mit efaCloud Server"));

            } else {
                return null;
            }
        }

        public String getErrorDoneMessage() {
            if (!success) {
                switch (efaCloudSynch.mode) {
                    case upload:
                        return LogString.operationFailed(
                                International.getString("Upload auf den efaCloud Server"));
                    case download:
                        return LogString.operationFailed(
                                International.getString("Download vom efaCloud Server"));
                }
                return LogString
                        .operationFailed(International.getString("Synchronisation mit efaCloud Server"));
            } else {
                return null;
            }
        }
    }
}