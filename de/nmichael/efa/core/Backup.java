/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.core;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.Admins;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.storage.XMLFile;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Email;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.ProgressTask;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Backup {

    public static final String BACKUP_META = "backup.meta";

    enum Mode {
        create,
        restore
    }

    private IDataAccess currentProjectDataAccess;
    private String currentProjectName;
    private String backupDir;
    private String backupFile;
    private String backupEmail;
    private boolean backupProject;
    private boolean backupConfig;
    private String zipFile;
    private String lastErrorMsg;
    private BackupTask backupTask;
    private BackupMetaData backupMetaData;
    private String[] restoreObjects;
    private Mode mode;
    private boolean openOrCreateProjectForRestore;
    private int totalWork = 0;
    private int totalWorkDone = 0;
    private StringBuilder msgOut = new StringBuilder();

    public Backup(String backupDir, String backupFile,
            boolean backupProject,
            boolean backupConfig) {
        this.backupDir = backupDir;
        this.backupFile = backupFile;
        this.backupProject = backupProject;
        this.backupConfig = backupConfig;
        this.mode = Mode.create;
    }

    public Backup(String backupDir, String backupFile, String backupEmail,
            boolean backupProject,
            boolean backupConfig) {
        this.backupDir = backupDir;
        this.backupFile = backupFile;
        this.backupEmail = backupEmail;
        this.backupProject = backupProject;
        this.backupConfig = backupConfig;
        this.mode = Mode.create;
    }

    public Backup(String backupZipFile, String[] restoreObjects,
            boolean openOrCreateProjectForRestore) {
        this.zipFile = backupZipFile;
        this.restoreObjects = restoreObjects;
        this.openOrCreateProjectForRestore = openOrCreateProjectForRestore;
        this.mode = Mode.restore;
    }

    public Mode getMode() {
        return mode;
    }

    private void getCurrentProjectInfo() {
        if (Daten.project == null) {
            currentProjectDataAccess = null;
            currentProjectName = null;
        } else {
            currentProjectDataAccess = Daten.project.data();
            currentProjectName = Daten.project.getProjectName();
            if (Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                currentProjectDataAccess = Daten.project.getRemoteDataAccess();
                currentProjectName = Daten.project.getProjectRemoteProjectName();
            }
        }
    }

    private int backupStorageObjects(IDataAccess[] dataAccesses,
            ZipOutputStream zipOut, String dir) {
        int successful = 0;
        for (IDataAccess data : dataAccesses) {
            try {
                if (!data.isStorageObjectOpen()) {
                    data.openStorageObject();
                }
                BackupMetaDataItem meta = data.saveToZipFile(dir, zipOut);
                backupMetaData.addMetaDataItem(meta);
                successful++;
                logMsg(Logger.INFO, Logger.MSG_BACKUP_BACKUPINFO,
                        LogString.fileSuccessfullyArchived(data.getUID(),
                        data.getStorageObjectDescription()));
            } catch (Exception e) {
                logMsg(Logger.ERROR, Logger.MSG_BACKUP_BACKUPERROR,
                        LogString.fileArchivingFailed(data.getUID(),
                        data.getStorageObjectDescription(), e.toString()));
                Logger.logdebug(e);
            }
            if (backupTask != null) {
                backupTask.setCurrentWorkDone(++totalWorkDone);
            }
        }
        return successful;
    }

    public static boolean isProjectDataAccess(String type) {
        return !type.equals(EfaConfig.DATATYPE) &&
               !type.equals(Admins.DATATYPE) &&
               !type.equals(EfaTypes.DATATYPE);
    }

    private IDataAccess getDataAccess(String name, String type, boolean isRemoteProject) {
        try {
            if (type.equals(EfaConfig.DATATYPE)) {
                if (isRemoteProject) {
                    return new EfaConfig(Daten.project.getProjectStorageType(),
                            Daten.project.getProjectStorageLocation(),
                            Daten.project.getProjectStorageUsername(),
                            Daten.project.getProjectStoragePassword()).data();
                } else {
                    return Daten.efaConfig.data();
                }
            }
            if (type.equals(Admins.DATATYPE)) {
                if (isRemoteProject) {
                    return new Admins(Daten.project.getProjectStorageType(),
                            Daten.project.getProjectStorageLocation(),
                            Daten.project.getProjectStorageUsername(),
                            Daten.project.getProjectStoragePassword()).data();
                } else {
                    return Daten.admins.data();
                }
            }
            if (type.equals(EfaTypes.DATATYPE)) {
                if (isRemoteProject) {
                    return new EfaTypes(Daten.project.getProjectStorageType(),
                            Daten.project.getProjectStorageLocation(),
                            Daten.project.getProjectStorageUsername(),
                            Daten.project.getProjectStoragePassword()).data();
                } else {
                    return Daten.efaTypes.data();
                }
            }
            return Daten.project.getStorageObjectDataAccess(name, type, true);
        } catch(Exception e) {
            logMsg(Logger.ERROR, Logger.MSG_DATA_ACCESSFAILED,
                    "Could not get DataAccess for "+name+"."+type);
            Logger.logdebug(e);
            return null;
        }
    }

    private boolean restoreStorageObject(BackupMetaDataItem meta, boolean isRemoteProject,
            ZipFile zip) {
        logMsg(Logger.INFO, Logger.MSG_BACKUP_RESTOREINFO,
                International.getMessage("Wiederherstellung von {description} '{name}' ...",
                meta.getDescription(), meta.getNameAndType()));
        try {
            ZipEntry entry = zip.getEntry(meta.getFileName());
            if (entry == null) {
                entry = zip.getEntry(meta.getFileNameWithSlash()); // should never happen (only if backup.meta has been manipulated manually)
            }
            if (entry == null) {
                entry = zip.getEntry(meta.getFileNameWithBackslash()); // should never happen (only if backup.meta has been manipulated manually)
            }
            if (entry == null) {
                logMsg(Logger.ERROR, Logger.MSG_BACKUP_RESTOREERROR,
                        LogString.fileRestoreFailed(meta.getFileName(),
                        meta.getNameAndType(),
                        "File not found in ZIP Archive"));
            }
            InputStream in = zip.getInputStream(entry);

            if (isProjectDataAccess(meta.getType())) {
                if (currentProjectName == null) {
                    getCurrentProjectInfo();
                }
                if (backupMetaData.getProjectName() != null &&
                    !backupMetaData.getProjectName().equals(currentProjectName) &&
                    openOrCreateProjectForRestore) {
                    openOrCreateProject(backupMetaData.getProjectName());
                }
                if (currentProjectName == null ||
                    backupMetaData.getProjectName() == null ||
                    !currentProjectName.equals(backupMetaData.getProjectName())) {
                    logMsg(Logger.ERROR, Logger.MSG_BACKUP_RESTOREERROR,
                            LogString.fileRestoreFailed(meta.getNameAndType(),
                            meta.getDescription(),
                            International.getMessage("Daten des Projekts {name} können nur in diesem auch wiederhergestellt werden, aber derzeit ist Projekt {name} geöffnet.",
                                                      backupMetaData.getProjectName(),
                                                      (currentProjectName != null ? currentProjectName :
                                                          "<--->"))));
                    return false;
                }
            }

            IDataAccess dataAccess = getDataAccess(meta.getName(), meta.getType(), isRemoteProject);
            if (dataAccess == null) {
                logMsg(Logger.ERROR, Logger.MSG_BACKUP_RESTOREERROR,
                        LogString.fileRestoreFailed(meta.getNameAndType(),
                        meta.getDescription(),  "DataAccess not found"));
                return false;
            }
            if (!dataAccess.isStorageObjectOpen()) {
                dataAccess.openStorageObject();
            }

            XMLFile zipDataAccess = new XMLFile(zipFile + "@@",
                    meta.getName(), meta.getType(), meta.getDescription());
            zipDataAccess.setPersistence(dataAccess.getPersistence());
            zipDataAccess.setMetaData(dataAccess.getMetaData());
            zipDataAccess.readFromInputStream(in);
            if (zipDataAccess.getSCN() != meta.getScn() ||
                zipDataAccess.getNumberOfRecords() != meta.getNumberOfRecords()) {
                throw new Exception("Unexpected SCN " + zipDataAccess.getSCN() +
                                    " or Record Count " + zipDataAccess.getNumberOfRecords() +
                                    " read from ZIP file; expected SCN " + meta.getScn() +
                                    " and Record Count " + meta.getNumberOfRecords() + ".");
            }
            dataAccess.copyFromDataAccess(zipDataAccess);

            logMsg(Logger.INFO, Logger.MSG_BACKUP_RESTOREINFO,
                    LogString.fileSuccessfullyRestored(meta.getNameAndType(),
                    meta.getDescription()) +
                    " [new SCN=" + dataAccess.getSCN() + ", Records=" + dataAccess.getNumberOfRecords() + "]");
            if (backupTask != null) {
                backupTask.setCurrentWorkDone(++totalWorkDone);
            }
        } catch (Exception e) {
            logMsg(Logger.ERROR, Logger.MSG_BACKUP_RESTOREERROR,
                    LogString.fileRestoreFailed(meta.getNameAndType(),
                    meta.getDescription(), e.toString()));
            Logger.logdebug(e);
            return false;
        }
        return true;
    }

    // backupTask is null for CLI backup, and set for GUI Backup
    public int runBackup(BackupTask backupTask) {
        this.backupTask = backupTask;
        lastErrorMsg = null;
        int successful = 0;
        int errors = 0;

        try {
            if ((!backupProject && !backupConfig)) {
                return -1;
            }
            boolean isRemoteProject = Daten.project != null && Daten.project.isOpen() &&
                    Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE;

            if (backupProject) {
                getCurrentProjectInfo();
            }

            backupMetaData = new BackupMetaData( (backupProject ? currentProjectName : null) );

            String items = (backupProject && backupConfig
                    ? International.getString("Projekt") + " '" + currentProjectName + "' " +
                      International.getString("und") + " " +
                      International.getString("efa-Konfiguration")
                    : (backupProject
                    ? International.getString("Projekt") + " '" + currentProjectName + "'"
                    : (backupConfig
                    ? International.getString("efa-Konfiguration")
                    : null)));
            logMsg(Logger.INFO, Logger.MSG_BACKUP_BACKUPSTARTED,
                    International.getMessage("Starte Backup von {items} ...", items));

            if (backupDir.length() > 0 && !backupDir.endsWith(Daten.fileSep)) {
                backupDir += Daten.fileSep;
            }
            String backupName = "efaBackup_" + EfaUtil.getCurrentTimeStampYYYYMMDD_HHMMSS();
            if (backupFile == null || backupFile.length() == 0) {
                backupFile = backupName + ".zip";
            }
            zipFile = backupDir + backupFile;

            FileOutputStream outFile = new FileOutputStream(zipFile);
            ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outFile));

            int cnt;

            if (backupProject) {
                cnt = backupStorageObjects(new IDataAccess[]{currentProjectDataAccess}, zipOut, Daten.efaSubdirDATA);
                successful += cnt;
                errors += (1 - cnt);

                Vector<StorageObject> storageObjects = Daten.project.getAllDataAndLogbooks();
                IDataAccess[] dataAccesses = new IDataAccess[storageObjects.size()];
                for (int i = 0; i < storageObjects.size(); i++) {
                    dataAccesses[i] = storageObjects.get(i).data();
                }
                totalWork = storageObjects.size() + (backupConfig ? 3 : 0);
                cnt = backupStorageObjects(dataAccesses, zipOut, Daten.efaSubdirDATA + Daten.fileSep + currentProjectName);
                successful += cnt;
                errors += (dataAccesses.length - cnt);
            }

            if (backupConfig) {
                IDataAccess[] dataAccesses = new IDataAccess[3];
                dataAccesses[0] = getDataAccess(null, EfaConfig.DATATYPE, false);
                dataAccesses[1] = getDataAccess(null, Admins.DATATYPE, false);
                dataAccesses[2] = getDataAccess(null, EfaTypes.DATATYPE, false);
                totalWork = (totalWork > 0 ? totalWork : 3);
                cnt = backupStorageObjects(dataAccesses, zipOut, Daten.efaSubdirCFG);
                successful += cnt;
                errors += (dataAccesses.length - cnt);
            }

            backupMetaData.write(zipOut);
            zipOut.close();
            
            logMsg(Logger.INFO, Logger.MSG_BACKUP_BACKUPFINISHEDINFO,
                    International.getMessage("{n} Objekte in {filename} gesichert.",
                    successful, zipFile));
            if (errors == 0) {
                if (backupEmail != null) {
                    String subject = International.getMessage("Backup vom {date}", EfaUtil.getCurrentTimeStampDD_MM_YYYY());
                    String text = subject + "\n\n" + msgOut.toString();
                    if (Daten.applID == Daten.APPL_CLI) {
                        Email.sendMessage(backupEmail,
                                subject, text, new String[]{zipFile}, true);
                    } else {
                        Email.enqueueMessage(backupEmail,
                                subject, text, new String[]{zipFile}, true);
                    }
                }
                logMsg(Logger.INFO, Logger.MSG_BACKUP_BACKUPFINISHED,
                        LogString.operationSuccessfullyCompleted(International.getString("Backup")));
            } else {
                logMsg(Logger.INFO, Logger.MSG_BACKUP_BACKUPFINISHEDWITHERRORS,
                        LogString.operationFinishedWithErrors(International.getString("Backup"), errors));
            }
        } catch (Exception e) {
            lastErrorMsg = LogString.operationFailed(International.getString("Backup"), e.toString());
            logMsg(Logger.ERROR, Logger.MSG_BACKUP_BACKUPFAILED,
                    lastErrorMsg);
            Logger.logdebug(e);
            return -1;
        }
        return errors;
    }

    // backupTask is null for CLI backup, and set for GUI Backup
    public int runRestore(BackupTask backupTask) {
        this.backupTask = backupTask;
        lastErrorMsg = null;
        int successful = 0;
        int errors = 0;

        if (zipFile == null || zipFile.length() == 0) {
            return -1;
        }

        backupMetaData = new BackupMetaData(null);
        if (!backupMetaData.read(zipFile)) {
            lastErrorMsg = LogString.fileOpenFailed(zipFile, International.getString("Archiv"), 
                    International.getString("Datei ist kein gültiges Backup"));
            logMsg(Logger.ERROR, Logger.MSG_BACKUP_RESTOREFAILED,
                    lastErrorMsg);
            return -1;
        }
        try {
            boolean isRemoteProject = Daten.project != null && Daten.project.isOpen() &&
                    Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE;

            logMsg(Logger.INFO, Logger.MSG_BACKUP_RESTORESTARTED,
                   International.getMessage("Starte Wiederherstellung von Projekt {name} mit {count} Objekten in {zip} ...",
                   (backupMetaData.getProjectName() != null ? backupMetaData.getProjectName() :
                       "<" + International.getString("unbekannt") + ">"),
                   (restoreObjects == null || restoreObjects.length == 0 ?
                       backupMetaData.size() : restoreObjects.length),
                        zipFile));

            ZipFile zip = new ZipFile(zipFile);
            if (restoreObjects == null || restoreObjects.length == 0) {
                totalWork = backupMetaData.size();
                for (int i=0; i<backupMetaData.size(); i++) {
                    if (restoreStorageObject(backupMetaData.getItem(i), isRemoteProject, zip)) {
                        successful++;
                    } else {
                        errors++;
                    }
                }
            } else {
                totalWork = restoreObjects.length;
                Hashtable<String,String> restoreObjectsHash = new Hashtable<String,String>();
                for (String s : restoreObjects) {
                    restoreObjectsHash.put(s, "foo");
                }
                // we have to restore objects in the order of BackupMetaData, not the order in the ZIP file!
                for (int i=0; i<backupMetaData.size(); i++) {
                    BackupMetaDataItem meta = backupMetaData.getItem(i);
                    if (restoreObjectsHash.get(meta.getNameAndType()) == null) {
                        continue; // this object was not selected to be restored
                    }
                    restoreObjectsHash.remove(meta.getNameAndType());
                    if (restoreStorageObject(meta,
                            isRemoteProject, zip)) {
                        successful++;
                    } else {
                        errors++;
                    }
                }
                // if there were objects selected that we have not restored, print an error
                if (restoreObjectsHash.size() > 0) {
                    String[] notRestoredObjects = restoreObjectsHash.keySet().toArray(new String[0]);
                    for (String s : notRestoredObjects) {
                        logMsg(Logger.ERROR, Logger.MSG_BACKUP_RESTOREERROR,
                                LogString.fileRestoreFailed(s,
                                "<" + International.getString("unbekannt") + ">",
                                "Object not found"));
                        errors++;
                    }
                }
            }

            logMsg(Logger.INFO, Logger.MSG_BACKUP_RESTOREFINISHEDINFO,
                    International.getMessage("{n} Objekte wiederhergestellt.",
                    successful, zipFile));

            // re-open project
            if (Daten.applID != Daten.APPL_CLI) {
                String pName = Daten.project.getProjectName();
                logMsg(Logger.INFO, Logger.MSG_BACKUP_REOPENINGFILES,
                        LogString.fileClosing(pName, International.getString("Projekt")));
                Daten.project.closeAllStorageObjects();
                logMsg(Logger.INFO, Logger.MSG_BACKUP_REOPENINGFILES,
                        LogString.fileOpening(pName, International.getString("Projekt")));
                Daten.project.openProject(pName, true);
                logMsg(Logger.INFO, Logger.MSG_EVT_PROJECTOPENED,
                        LogString.fileOpened(pName, International.getString("Projekt")));
            }

            if (errors == 0) {
                logMsg(Logger.INFO, Logger.MSG_BACKUP_RESTOREFINISHED,
                        LogString.operationSuccessfullyCompleted(International.getString("Wiederherstellung")));
            } else {
                logMsg(Logger.INFO, Logger.MSG_BACKUP_RESTOREFINISHEDWITHERRORS,
                        LogString.operationFinishedWithErrors(International.getString("Wiederherstellung"), errors));
            }
        } catch (Exception e) {
            lastErrorMsg = LogString.operationFailed(International.getString("Wiederherstellung"), e.toString());
            logMsg(Logger.ERROR, Logger.MSG_BACKUP_RESTOREFAILED,
                    lastErrorMsg);
            Logger.logdebug(e);
            return -1;
        }
        return errors;
    }

    private void openOrCreateProject(String newProjectName) {
        try {
            // close project, if a wrong project is open
            if (Daten.project != null) {
                logMsg(Logger.INFO, Logger.MSG_BACKUP_REOPENINGFILES,
                        LogString.fileClosing(Daten.project.getProjectName(), International.getString("Projekt")));
                Daten.project.closeAllStorageObjects();
            }

            // try to open correct project
            logMsg(Logger.INFO, Logger.MSG_BACKUP_REOPENINGFILES,
                    LogString.fileOpening(newProjectName, International.getString("Projekt")));
            if (Daten.project.openProjectSilent(newProjectName, false)) {
                logMsg(Logger.INFO, Logger.MSG_EVT_PROJECTOPENED,
                        LogString.fileOpened(newProjectName, International.getString("Projekt")));
            } else {
                // if opening failed, create a new project
                logMsg(Logger.WARNING, Logger.MSG_BACKUP_REOPENINGFILES,
                        LogString.fileOpenFailed(newProjectName, International.getString("Projekt")));
                Project prj = new Project(newProjectName);
                try {
                    prj.create();
                    prj.setEmptyProject(newProjectName);
                    prj.setProjectStorageType(IDataAccess.TYPE_FILE_XML);
                    prj.close();
                    Project.openProject(newProjectName, false);
                    logMsg(Logger.INFO, Logger.MSG_EVT_PROJECTOPENED,
                            LogString.fileNewCreated(newProjectName, International.getString("Projekt")));
                } catch (Exception ecreate) {
                    logMsg(Logger.ERROR, Logger.MSG_BACKUP_REOPENINGFILES,
                            LogString.fileCreationFailed(newProjectName, International.getString("Projekt"),
                            ecreate.toString()));
                    openOrCreateProjectForRestore = false;
                }
            }
        } catch (Exception e) {
            logMsg(Logger.ERROR, Logger.MSG_BACKUP_REOPENINGFILES,
                    LogString.fileOpenFailed(newProjectName, International.getString("Projekt"),
                    e.toString()));
            openOrCreateProjectForRestore = false;
        }
        getCurrentProjectInfo();
    }

    public String getLastErrorMessage() {
        return lastErrorMsg;
    }

    public String getZipFile() {
        return zipFile;
    }

    private void logMsg(String type, String key, String msg) {
        Logger.log(type, key, msg);
        if (backupTask != null && !type.equals(Logger.DEBUG)) {
            backupTask.logInfo(msg + "\n");
        }
        msgOut.append(msg + "\n");
    }

    public int getTotalWork() {
        return totalWork;
    }

    // Run Method for Creating a Backup
    public static void runCreateBackupTask(BaseDialog parentDialog,
            String backupDir,
            String backupFile,
            boolean backupProject,
            boolean backupConfig) {
        BackupTask backupTask = new BackupTask(backupDir, backupFile,
                backupProject, backupConfig);
        ProgressDialog progressDialog = new ProgressDialog(parentDialog,
                International.getString("Backup erstellen"), backupTask, false);
        backupTask.startBackup(progressDialog);
    }

    // Run Method for Restoring a Backup
    public static void runRestoreBackupTask(BaseDialog parentDialog,
            String backupZipFile, String[] restoreObjects,
            boolean openOrCreateProjectForRestore) {
        BackupTask backupTask = new BackupTask(backupZipFile, restoreObjects,
                openOrCreateProjectForRestore);
        ProgressDialog progressDialog = new ProgressDialog(parentDialog,
                International.getString("Backup einspielen"), backupTask, false);
        backupTask.startBackup(progressDialog);
    }

}

class BackupTask extends ProgressTask {

    private Backup backup;
    boolean success = false;

    // Constructor for Creating a Backup
    public BackupTask(String backupDir, String backupFile,
            boolean backupProject,
            boolean backupConfig) {
        super();
        backup = new Backup(backupDir, backupFile, backupProject, backupConfig);
    }

    // Constructor for Creating an email Backup
    public BackupTask(String email,
            boolean backupProject,
            boolean backupConfig) {
        super();
        backup = new Backup(Daten.efaTmpDirectory, null, email, backupProject, backupConfig);
    }

    // Constructor for Restoring a Backup
    public BackupTask(String backupZipFile, String[] restoreObjects,
            boolean openOrCreateProjectForRestore) {
        super();
        backup = new Backup(backupZipFile, restoreObjects, openOrCreateProjectForRestore);
    }

    public void startBackup(ProgressDialog progressDialog) {
        this.start();
        if (progressDialog != null) {
            progressDialog.showDialog();
        }
    }

    public void run() {
        setRunning(true);
        success = false;
        switch(backup.getMode()) {
            case create:
                success = backup.runBackup(this) == 0;
                break;
            case restore:
                success = backup.runRestore(this) == 0;
                break;
        }
        setDone();
    }

    public int getAbsoluteWork() {
        return backup.getTotalWork();
    }

    public String getSuccessfullyDoneMessage() {
        if (success) {
            switch (backup.getMode()) {
                case create:
                    return LogString.operationSuccessfullyCompleted(
                            International.getString("Backup")) + "\n\n" +
                            International.getString("Archiv") + ": \n" +
                            backup.getZipFile();
                case restore:
                    return LogString.operationSuccessfullyCompleted(
                            International.getString("Wiederherstellung"));
            }
            return LogString.operationSuccessfullyCompleted(
                            International.getString("Operation"));

        } else {
            return null;
        }
    }

    public String getErrorDoneMessage() {
        if (!success) {
            switch (backup.getMode()) {
                case create:
                    return LogString.operationFailed(International.getString("Backup"));
                case restore:
                    return LogString.operationFailed(International.getString("Wiederherstellung"));
            }
            return LogString.operationFailed(International.getString("Operation"));
        } else {
            return null;
        }
    }
}
