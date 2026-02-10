/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.storage;

import de.nmichael.efa.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.ex.EfaException;
import java.util.*;
import java.io.*;

// @i18n complete

public abstract class DataFile extends DataAccess {

    public static final String BACKUP_MOSTRECENT = ".s0";
    public static final String BACKUP_OLDVERSION = ".s1";

    protected static final String ENCODING = Daten.ENCODING_UTF;
    protected String filename;
    protected String mirrorRelativeFilename;
    protected volatile boolean isOpen = false;
    private final HashMap<DataKey,DataRecord> data = new HashMap<DataKey,DataRecord>();
    private final HashMap<DataKey,ArrayList<DataKey>> versionizedKeyList = new HashMap<DataKey,ArrayList<DataKey>>();
    private final ArrayList<DataIndex> indices = new ArrayList<DataIndex>();
    protected long scn = 0;
    private DataKey[] cachedKeys; // are only updated by getAllKeys(), not automatically when data is changed!!
    private long cachedKeysSCN = 0;
    private final DataLocks dataLocks = new DataLocks();
    private DataFileWriter fileWriter;
    private Journal journal;

    public DataFile(String directory, String name, String extension, String description) {
        setStorageLocation(directory);
        setStorageObjectName(name);
        setStorageObjectType(extension);
        setStorageObjectDescription(description);
        filename = directory + (directory.endsWith(Daten.fileSep) ? "" : Daten.fileSep) +
                name + "." + extension;
        if (filename.startsWith(Daten.efaDataDirectory)) {
            mirrorRelativeFilename = Daten.efaSubdirDATA + Daten.fileSep +
                    filename.substring(Daten.efaDataDirectory.length());
        }
        if (filename.startsWith(Daten.efaCfgDirectory)) {
            mirrorRelativeFilename = Daten.efaSubdirCFG + Daten.fileSep +
                    filename.substring(Daten.efaCfgDirectory.length());
        }
    }

    /*
    public DataFile(String filename) {
        setStorageLocation(EfaUtil.getPathOfFile(filename));
        String fname = EfaUtil.getNameOfFile(filename);
        if (fname.indexOf(".") > 0) {
            setStorageObjectName(fname.substring(0,fname.indexOf(".")));
            setStorageObjectType(fname.substring(fname.indexOf(".")+1));
        } else {
            setStorageObjectName(fname);
            setStorageObjectType("");
        }
    }
    */
    
    public String getUID() {
        return "file:" + filename;
    }

    public String getFilename() {
        return filename;
    }

    private void setupJournal() {
        journal = new Journal(getStorageObjectName()+"."+getStorageObjectType(), filename);
    }

    private void closeJournal() {
        if (journal != null) {
            journal.close();
        }
    }

    public synchronized boolean existsStorageObject() throws EfaException {
        if (filename == null) {
            throw new EfaException(Logger.MSG_DATA_GENERICEXCEPTION, "No StorageObject name specified.", Thread.currentThread().getStackTrace());
        }
        return (new File(filename).exists());
    }

    public synchronized void createStorageObject() throws EfaException {
        try {
            File f = new File(storageLocation);
            if (!f.exists()) {
                f.mkdirs();
            }
            FileOutputStream fout = new FileOutputStream(filename,false);
            writeFile(fout);
            fout.close();
            scn = 0;
            setupJournal();
            isOpen = true;
            fileWriter = new DataFileWriter(this);
            fileWriter.start();
        } catch(Exception e) {
            throw new EfaException(Logger.MSG_DATA_CREATEFAILED, LogString.fileCreationFailed(filename, storageLocation, e.toString()), Thread.currentThread().getStackTrace());
        }
    }
    
    private void printFileInfoBeforeRecovery(String filename) {
        try {
            String fname = (new File(filename)).getName();
            String dirname = (new File(filename)).getParent();
            Logger.log(Logger.INFO, Logger.MSG_DATA_RECOVERYINFO,
                    "Directory listing of " + dirname + " before recovery:");
            File dir = new File(dirname);
            if (!dir.exists()) {
                Logger.log(Logger.ERROR, Logger.MSG_DATA_RECOVERYINFO,
                        "Directory " + dirname + " does not exist.");
            }
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                Logger.log(Logger.ERROR, Logger.MSG_DATA_RECOVERYINFO,
                        "Directory " + dirname + " is empty.");
            }
            for (File file : files) {
                if (file.getName().startsWith(fname)) {
                    Logger.log(Logger.INFO, Logger.MSG_DATA_RECOVERYINFO,
                            String.format("%-30s %7d byte  %s", file.getName(), file.length(), EfaUtil.getTimeStamp(file.lastModified())));
                }
            }
        } catch(Exception e) {
            Logger.log(Logger.WARNING, Logger.MSG_DATA_RECOVERYINFO, 
                    "Failed to get file information: " + e);
        }
    }

    private void saveOriginalFileBeforeRecovery(String filename) {
        String bakFile = null;
        try {
            File f = new File(filename);
            if (f.exists()) {
                bakFile = Daten.efaBakDirectory + f.getName();
                int i = 0;
                while (new File(bakFile).exists()) {
                    bakFile = Daten.efaBakDirectory + f.getName() + "." + ++i;
                }
                f.renameTo(new File(bakFile));
                Logger.log(Logger.INFO, Logger.MSG_DATA_RECOVERYORIGMOVED,
                        LogString.fileMoved(filename, International.getString("Originaldatei"), bakFile));
            }
        } catch(Exception e) {
            Logger.log(Logger.WARNING, Logger.MSG_DATA_RECOVERYORIGMOVED,
                    "Could not move " + filename + " to " + bakFile + ": " + e.toString());
        }
    }

    private boolean tryOpenStorageObject(String filename, boolean recover) throws Exception {
        String descr = getStorageObjectDescription() + " (" + getStorageObjectName() + "." + getStorageObjectType() + ")";
        if (recover) {
            Logger.log(Logger.WARNING, Logger.MSG_DATA_RECOVERYSTART,
                    LogString.operationStarted(
                        International.getMessage("Wiederherstellung von {description} aus {filename}",
                        descr, filename)));
        }
        scn = 0;
        BufferedReader fr = new BufferedReader(new InputStreamReader(new FileInputStream(filename), ENCODING));
        readFile(fr);
        fr.close();
        if (recover) {
            Logger.log(Logger.INFO, Logger.MSG_DATA_RECOVERYSTART,
                    LogString.fileOpened(filename, 
                        getStorageObjectName() + "." + getStorageObjectType() + " [SCN " + getSCN() + "]"));
            long latestScn = Journal.getLatestScnFromJournals(getStorageObjectName()+"."+getStorageObjectType(), this.filename);
            if (latestScn < 0) {
                Logger.log(Logger.ERROR, Logger.MSG_DATA_REPLAYNOJOURNAL,
                        International.getMessage("Kein Journal für Wiederherstellung von {description} gefunden. Wiederhergestellte Daten sind möglicherweise unvollständig (Datenverlust)!",
                        descr, filename));                
            } else {
                if (latestScn > scn) {
                    inOpeningStorageObject = true; // don't update LastModified Timestamps, don't increment SCN, don't check assertions!
                    isOpen = true;
                    try {
                        scn = Journal.rollForward(this, getStorageObjectName()+"."+getStorageObjectType(), this.filename, latestScn);
                    } finally {
                        inOpeningStorageObject = false;
                        isOpen = false;
                    }
                }
            }
            Logger.log(Logger.INFO, Logger.MSG_DATA_RECOVERYFINISHED,
                    LogString.operationFinished(
                        International.getMessage("Wiederherstellung von {description} aus {filename}",
                        descr, filename)) + " SCN=" + scn);
            return true;
        }
        return false;
    }

    public synchronized void openStorageObject() throws EfaException {
        String tryfilename = filename;
        boolean logex = true;
        try {
            boolean recovered = false;
            fileWriter = null;
            try {
                recovered = tryOpenStorageObject(filename, false);
            } catch(Exception e1) {
                if (!new File (filename + BACKUP_MOSTRECENT).exists() &&
                    !new File (filename + BACKUP_OLDVERSION).exists()) {
                    // no backup files found, so we don't have to even try to recover.
                    // instead, we throw an exception.
                    // our callee may then react by creating a new storage object instead, if he likes
                    logex = false;
                    throw e1;
                }
                try {
                    Logger.log(Logger.ERROR, Logger.MSG_DATA_OPENFAILED,
                            LogString.fileOpenFailed(filename, getStorageObjectName() + "." + getStorageObjectType() , e1.toString()));
                    printFileInfoBeforeRecovery(filename);
                    saveOriginalFileBeforeRecovery(filename);
                    tryfilename = filename + BACKUP_MOSTRECENT;
                    recovered = tryOpenStorageObject(tryfilename, true);
                } catch(Exception e2) {
                    Logger.log(Logger.ERROR, Logger.MSG_DATA_OPENFAILED,
                            LogString.fileOpenFailed(tryfilename, getStorageObjectName() + "." + getStorageObjectType() , e2.toString()));
                    tryfilename = filename + BACKUP_OLDVERSION;
                    recovered = tryOpenStorageObject(tryfilename, true);
                }
            }
            setupJournal();
            isOpen = true;
            fileWriter = new DataFileWriter(this);
            fileWriter.start();
            if (recovered || shouldWriteMirrorFile()) {
                saveStorageObject();
            }
            
            this.handlePostOpenStorageObject();

        } catch(Exception e) {
            if (logex) {
                Logger.log(e);
            }
            throw new EfaException(Logger.MSG_DATA_OPENFAILED, LogString.fileOpenFailed(tryfilename, storageLocation, e.toString()), Thread.currentThread().getStackTrace());
        }
    }
    
    /**
     * Method where subclasses of DataFile can run additional actions after opening the data file.
     * Empty by default.
     */
    protected void handlePostOpenStorageObject() {
    	
    }
    
    /**
     * Method where subclasses of DataFile can run additional actions short after actually closing the data file.
     * Empty by default.
     */    
    protected void handleBeforeClosingStorageObject() {
    	
    }    

    private boolean shouldWriteMirrorFile() {
        try {
            String mirrorDir = Daten.efaConfig.getValueDataMirrorDirectory();
            if (mirrorDir != null && mirrorDir.length() > 0 &&
                    new File(mirrorDir).exists() &&
                    mirrorRelativeFilename != null && mirrorRelativeFilename.length() > 0) {
                String mirrorFile = mirrorDir + (mirrorDir.endsWith(Daten.fileSep) ? "" : Daten.fileSep)
                                       + mirrorRelativeFilename;
                File f = new File(mirrorFile);
                return !f.exists();
            } else {
                return false;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }

    // This method must *not* be synchronized;
    // that would result in a deadlock between fileWriter running save(true) and the thread calling closeStorageObject()
    public void closeStorageObject() throws EfaException {
        if (!isOpen) {
            return;
        }
        try {
            if (fileWriter == null) {
                Logger.log(Logger.ERROR, Logger.MSG_DATA_CLOSEFAILED, LogString.fileCloseFailed(filename, storageLocation,
                        "File appears to be already closed (fileWriter==null)"));
                this.handleBeforeClosingStorageObject();                
                clearAllData();
                isOpen = false;
                closeJournal();
                return;
            }
            fileWriter.save(true, false);
            this.handleBeforeClosingStorageObject();
            clearAllData();
            isOpen = false;
            closeJournal();
            fileWriter.exit();
            fileWriter.join(DataFileWriter.SAVE_INTERVAL * 2);
        } catch(Exception e) {
            throw new EfaException(Logger.MSG_DATA_CLOSEFAILED, LogString.fileCloseFailed(filename, storageLocation, e.toString()), Thread.currentThread().getStackTrace());
        } finally {
            fileWriter = null;
        }
    }
   
    protected boolean createBackupFile(String originalFilename) {
        String backup0 = originalFilename + BACKUP_MOSTRECENT; // most recent backup
        String backup1 = originalFilename + BACKUP_OLDVERSION; // previous backup
        try {
            if (!new File(originalFilename).exists()) {
                return true; // nothing to do
            }
            boolean ok = true;
            if (new File(backup0).exists()) {
                if (new File(backup1).exists()) {
                    // backup1 exists! delete it first
                    ok = new File(backup1).delete();
                    if (!ok) {
                        Logger.log(Logger.WARNING, Logger.MSG_DATA_FILEBACKUPFAILED, LogString.fileDeletionFailed(backup1, "Backup File 1"));
                    }
                }
                if (ok) {
                    // backup1 doesn't exisit or has successfully been deleted
                    ok = new File(backup0).renameTo(new File(backup1));
                    if (!ok) {
                        Logger.log(Logger.WARNING, Logger.MSG_DATA_FILEBACKUPFAILED, LogString.fileRenameFailed(backup0, "Backup File 0"));
                    }
                }
            }
            if (ok) {
                // backup0 has successfully been renamed to backup1
                ok = new File(originalFilename).renameTo(new File(backup0));
                if (!ok) {
                    Logger.log(Logger.WARNING, Logger.MSG_DATA_FILEBACKUPFAILED, LogString.fileRenameFailed(originalFilename, "Original File"));
                }
            }
            return ok;
        } catch(Exception e) {
            Logger.log(Logger.WARNING, Logger.MSG_DATA_FILEBACKUPFAILED, e.toString());
            return false;
        }
    }

    public synchronized void saveStorageObject(boolean calledFromFileWriter) throws EfaException {
        long lock = -1;
        try {
            lock = acquireGlobalLock();
            if (Logger.isTraceOn(Logger.TT_FILEIO)) {
                Logger.log(Logger.DEBUG, Logger.MSG_FILE_WRITETHREAD_SAVING, "DataFileWriter[" + filename + "] got global lock, now saving ...");
            }
            saveStorageObject();
            if (Logger.isTraceOn(Logger.TT_FILEIO)) {
                Logger.log(Logger.DEBUG, Logger.MSG_FILE_WRITETHREAD_SAVING, "DataFileWriter[" + filename + "] data successfully saved.");
            }
        } finally {
            if (lock > 0) {
                releaseGlobalLock(lock);
                if (Logger.isTraceOn(Logger.TT_FILEIO)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_FILE_WRITETHREAD_SAVING, "DataFileWriter[" + filename + "] released global lock.");
                }
            }
        }
    }

    public synchronized void saveStorageObject() throws EfaException {
        if (!isStorageObjectOpen()) {
            throw new EfaException(Logger.MSG_DATA_SAVEFAILED, LogString.fileWritingFailed(filename, storageLocation, "Storage Object is not open"), Thread.currentThread().getStackTrace());
        }
        try {
            createBackupFile(filename);
            FileOutputStream fout = new FileOutputStream(filename, false);
            writeFile(fout);
            fout.close();
        } catch(Exception e) {
            throw new EfaException(Logger.MSG_DATA_SAVEFAILED, LogString.fileWritingFailed(filename, storageLocation, e.toString()), Thread.currentThread().getStackTrace());
        }
    }

    public boolean isStorageObjectOpen() {
        return isOpen;
    }

    // This method must *not* be synchronized;
    // that would result in a deadlock between fileWriter running save(true) and the
    // thread calling deleteStorageObject(), which calls closeStorageObject()
    public void deleteStorageObject() throws EfaException {
        try {
            try {
                closeStorageObject();
            } catch(Exception eignore) {
                Logger.logdebug(eignore);
            }
            File f = new File(filename);
            if (!f.delete()) {
                throw new Exception(LogString.fileDeletionFailed(filename, getStorageObjectDescription()));
            }
            if (journal != null) {
                journal.deleteAllJournals();
            }
            deleteAllBackups();
        } catch(Exception e) {
            throw new EfaException(Logger.MSG_DATA_DELETEFAILED,
                    LogString.fileDeletionFailed(filename, getStorageObjectDescription(), e.toString()), Thread.currentThread().getStackTrace());
        }
    }

    public void deleteAllBackups() throws EfaException {
        String[] backups = new String[] {
            filename + BACKUP_MOSTRECENT,
            filename + BACKUP_OLDVERSION
        };
        for (int i = 0; i < backups.length; i++) {
            String backupFIle = backups[i];
            try {
                File f = new File(backupFIle);
                if (f.isFile()) {
                    if (!f.delete()) {
                        throw new Exception(LogString.fileDeletionFailed(backupFIle, International.getString("Backup")));
                    }
                }
            } catch (Exception e) {
                throw new EfaException(Logger.MSG_DATA_DELETEFAILED, LogString.fileDeletionFailed(backupFIle, International.getString("Backup"), e.toString()), Thread.currentThread().getStackTrace());
            }
        }
    }

    public long getFileSize() {
        try {
            return (new File(this.filename)).length();
        } catch(Exception e) {
            return -1;
        }
    }

    protected abstract void readFile(BufferedReader fr) throws EfaException;
    protected abstract void writeFile(OutputStream out) throws EfaException;

    private long getLock(DataKey object) throws EfaException {
        if (!isStorageObjectOpen()) {
            throw new EfaException(Logger.MSG_DATA_GETLOCKFAILED, getUID() + ": Storage Object is not open", Thread.currentThread().getStackTrace());
        }
        long lockID = (object == null ? dataLocks.getGlobalLock() :
                                        dataLocks.getLocalLock(object) );
        if (lockID < 0) {
            throw new EfaException(Logger.MSG_DATA_GETLOCKFAILED, getUID() + ": Could not acquire " +
                    (object == null ? "global lock" :
                                      "local lock on "+object), Thread.currentThread().getStackTrace());
        }
        return lockID;
    }

    public long acquireGlobalLock() throws EfaException {
        return getLock(null);
    }

    public long acquireLocalLock(DataKey key) throws EfaException {
        return getLock(key);
    }

    public boolean releaseGlobalLock(long lockID) {
        return dataLocks.releaseGlobalLock(lockID);
    }

    public boolean releaseLocalLock(long lockID) {
        return dataLocks.releaseLocalLock(lockID);
    }

    public long getSCN() throws EfaException {
        return scn;
    }

    void setSCN(long scn) throws EfaException {
        this.scn = scn;
    }

    public void createIndex(String[] fieldNames) throws EfaException {
        int[] idxFields = new int[fieldNames.length];
        for (int i=0; i<idxFields.length; i++) {
            idxFields[i] = meta.getFieldIndex(fieldNames[i]);
        }
        indices.add(new DataIndex(idxFields));
    }

    private DataRecord modifyRecord(DataRecord record, long lockID, boolean add, boolean update, boolean delete) throws EfaException {
        long myLock = -1;
        DataRecord newRecord = null;
        if (record == null) {
            throw new EfaException(Logger.MSG_DATA_RECORDNOTFOUND, getUID() + ": Data Record is 'null' for " +
                    (add ? "add" : (update ? "update" : (delete ? "delete" : "noop"))),
                    Thread.currentThread().getStackTrace());
        }
        if ( (add && update) || (add && delete) || (update && delete) ) {
            throw new EfaException(Logger.MSG_DATA_INVALIDPARAMETER, getUID() + ": Invalid Parameter: "+add+","+update+","+delete, Thread.currentThread().getStackTrace());
        }
        if (!referenceRecord.getClass().isAssignableFrom(record.getClass())) {
            throw new EfaException(Logger.MSG_DATA_RECORDWRONGTYPE,
                    getUID() + ": Data Record "+record.toString()+" has wrong Type: " + record.getClass().getCanonicalName() + ", expected: " + referenceRecord.getClass().getCanonicalName(),
                    Thread.currentThread().getStackTrace());
        }

        if (!inOpeningStorageObject() && isPreModifyRecordCallbackEnabled()) {
                getPersistence().preModifyRecordCallback(record, add, update, delete);
        }

        DataKey key = constructKey(record);
        if (lockID <= 0) {
            // acquire a new local lock
            myLock = acquireLocalLock(key);
        } else {
            // verify existing lock
            myLock = (dataLocks.hasGlobalLock(lockID) || dataLocks.hasLocalLock(lockID, key) ? lockID : -1);
        }
        if (myLock > 0) {
            try {
                synchronized (data) {
                    DataRecord currentRecord = data.get(key);
                    if (currentRecord == null) {
                        if ( (update && !add) || delete) {
                            throw new EfaException(Logger.MSG_DATA_RECORDNOTFOUND, getUID() + ": Data Record '"+key.toString()+"' does not exist", Thread.currentThread().getStackTrace());
                        }
                    } else {
                        if ( (add && !update)) {
                            throw new EfaException(Logger.MSG_DATA_DUPLICATERECORD, getUID() + ": Data Record '"+key.toString()+"' already exists", Thread.currentThread().getStackTrace());
                        }
                    }
                    if (update && !add) {
                        if (currentRecord.getChangeCount() != record.getChangeCount() &&
                            !inOpeningStorageObject && !record.isCopyFromServer) {
                            // Throw an exception!
                            throw new EfaException(Logger.MSG_DATA_DUPLICATERECORD, getUID() + ": Update Conflict for Data Record '"+key.toString()+
                                    "': Current ChangeCount="+currentRecord.getChangeCount()+", expected ChangeCount="+record.getChangeCount(),
                                    Thread.currentThread().getStackTrace());
                            /*
                            // Logging this event was just a work-around in the past; actually, we have
                            // to throw an exception instead
                            Logger.logStackTrace(Logger.ERROR, Logger.MSG_DATA_UPDATECONFLICT,
                                    getUID() + ": Update Conflict for Data Record '"+key.toString()+
                                    "': Current ChangeCount="+currentRecord.getChangeCount()+", expected ChangeCount="+record.getChangeCount(),
                                    Thread.currentThread().getStackTrace());
                            */
                        }
                    }
                    if (!inOpeningStorageObject && !record.isCopyFromServer ) {
                        // don't update LastModified timestamp when reading saved data from file
                        // nor when copying it from the efacloud server!
                        record.setLastModified();
                        record.updateChangeCount();
                    }
                    if (add || update) {
                        newRecord = record.cloneRecord();
                        if (inOpeningStorageObject || journal.log(scn+1, (add ? Journal.Operation.add : Journal.Operation.update), record)) {
                            data.put(key, newRecord);
                            if (!inOpeningStorageObject) {
                                scn++;
                            }
                        } else {
                            throw new EfaException(Logger.MSG_DATA_JOURNALLOGFAILED, getUID() + ": Operation failed for Data Record '"+record.toString()+"'", Thread.currentThread().getStackTrace());
                        }
                        if (meta.versionized) {
                            modifyVersionizedKeys(key, add, update, delete);
                        }
                        for (DataIndex idx: indices) {
                            if (update) {
                                idx.delete(currentRecord);
                            }
                            idx.add(newRecord);
                        }
                    } else {
                        if (delete) {
                            if (inOpeningStorageObject || journal.log(scn + 1, Journal.Operation.delete, record)) {
                                data.remove(key);
                                if (!inOpeningStorageObject) {
                                    scn++;
                                }
                            } else {
                                throw new EfaException(Logger.MSG_DATA_JOURNALLOGFAILED, getUID() + ": Operation failed for Data Record '" + record.toString() + "'", Thread.currentThread().getStackTrace());
                            }
                            if (meta.versionized) {
                                modifyVersionizedKeys(key, add, update, delete);
                            }
                            for (DataIndex idx: indices) {
                                idx.delete(record); // needs record, but record must not be null
                            }
                        }
                    }
                    this.handleInModifyRecord(record, newRecord, currentRecord, key, add, update, delete);                    
                }
            } finally {
                this.handlePostModifyRecord(record, newRecord, key, add, update, delete);
            	if (lockID <= 0 && myLock > 0) {
                    releaseLocalLock(myLock);
                }
            }



            if (fileWriter != null) { // may be null while reading (opening) a file
                fileWriter.save(false, true);
            }
        } else {
            throw new EfaException(Logger.MSG_DATA_MODIFICATIONFAILED, getUID() + ": Data Record Operation failed: No Write Access", Thread.currentThread().getStackTrace());
        }
        return newRecord;
    }

    /**
     * Method where subclasses of DataFile can run additional actions short after adding, updating or deleting a record
     * Empty by default.
     */    
    protected void handleInModifyRecord(DataRecord record, DataRecord newRecord, DataRecord currentRecord, DataKey key, boolean add, boolean update, boolean delete) {
    	
    }
    
    /**
     * Method where subclasses of DataFile can run additional actions after all other actions of modifying a record.
     * Empty by default.
     */        
    protected void handlePostModifyRecord(DataRecord record, DataRecord newRecord, DataKey key, boolean add, boolean update, boolean delete) {
    	
    }
    
    private void modifyVersionizedKeys(DataKey key, boolean add, boolean update, boolean delete) {
        DataKey keyUnversionized = getUnversionizedKey(key);
        synchronized(data) { // always synchronize on data to ensure integrity!
            ArrayList<DataKey> list = versionizedKeyList.get(keyUnversionized);
            if (list == null) {
                if (add || update) {
                    list = new ArrayList<DataKey>();
                }
                if (delete) {
                    return; // nothing to do
                }
            }
            if (add || update) {
                if (!list.contains(key)) {
                    list.add(key);
                    versionizedKeyList.put(keyUnversionized, list);
                }
            }
            if (delete) {
                list.remove(key);
                if (list.size() == 0) {
                    versionizedKeyList.remove(keyUnversionized); // last key removed
                } else {
                    // no "versionizedKeyList.put(keyUnversionized, list)" necessary (we're working on the same reference of "list")
                }
            }
        }
    }

    public void add(DataRecord record) throws EfaException {
        if (meta.versionized) {
            addValidAt(record, -1, 0);
        } else {
            modifyRecord(record, 0, true, false, false);
        }
    }

    public void add(DataRecord record, long lockID) throws EfaException {
        if (meta.versionized) {
            addValidAt(record, -1, lockID);
        } else {
            modifyRecord(record, lockID, true, false, false);
        }
    }

    public DataKey addValidAt(DataRecord record, long t) throws EfaException {
        return addValidAt(record, t, 0);
    }

    public DataKey addValidAt(DataRecord record, long t, long lockID) throws EfaException {
        if (!meta.versionized) {
            throw new EfaException(Logger.MSG_DATA_INVALIDVERSIONIZEDDATA, getUID() + ": Attempt to add versionized data to an unversionized storage object", Thread.currentThread().getStackTrace());
        }
        long myLock = -1;
        if (lockID <= 0) {
            // acquire a new global lock
            myLock = acquireGlobalLock();
        } else {
            // verify existing lock
            myLock = (dataLocks.hasGlobalLock(lockID) ? lockID : -1);
        }
        if (myLock > 0) {
            try {
                synchronized (data) {
                    if (isValidAny(record.getKey())) {
                        if (t == Long.MIN_VALUE || t == Long.MAX_VALUE) {
                            addValidAtBeforeOrAfter(record, t, myLock);
                        } else {
                            if (t < 0) {
                                t = record.getValidFrom();
                                //t = System.currentTimeMillis();
                            }
                            DataRecord r1 = getValidAt(record.getKey(), t);
                            if (r1 != null) {
                                // record with (at least partially) overlapping validity (at validFrom of new record)
                                if (t == r1.getValidFrom()) {
                                    throw new EfaException(Logger.MSG_DATA_VERSIONIZEDDATACONFLICT, getUID() + ": Versionized Data Conflict (Duplicate?) for Record " + record.toString() + " at ValidFrom=" + t, Thread.currentThread().getStackTrace());
                                }
                                // add new record
                                record.setValidFrom(t);
                                record.setInvalidFrom(r1.getInvalidFrom());
                                modifyRecord(record, myLock, true, false, false);
                                // adjust InvalidFrom field for existing record
                                modifyRecord(r1, myLock, false, false, true);
                                r1.setInvalidFrom(t);
                                modifyRecord(r1, myLock, true, false, false);
                            } else {
                                // There are already records with the same key, but none that are valid at this new record's validFrom.
                                // During normal operation of efa, the validity range is always complete, i.e. there are no "holes" in validity.
                                // However, for reading data from file, holes may appear until the entire file is read.
                                // We could now check against all other records returned from getValidAny() whether there is any overlap, but we
                                // skip this as this could only happen if the file is externally modified (or if there is a bug in efa...)
                                if (t >= 0) {
                                    record.setValidFrom(t);
                                }
                                modifyRecord(record, myLock, true, false, false);
                            }
                        }
                    } else {
                        if (t >= 0) {
                            record.setValidFrom(t);
                        }
                        modifyRecord(record, myLock, true, false, false);
                    }
                }
            } finally {
                if (lockID <= 0 && myLock > 0) {
                    releaseGlobalLock(myLock);
                }
            }
        } else {
            throw new EfaException(Logger.MSG_DATA_NOLOCKHELD, getUID() + ": Attempt to add data without holding a lock", Thread.currentThread().getStackTrace());
        }
        return record.getKey();
    }

    private void addValidAtBeforeOrAfter(DataRecord record, long t, long lockID) throws EfaException {
        // Adds a record before or after a current existing one without touching the existing ones.
        // This will only add a record if the new record is valid before the first exisiting record,
        // or if it is valid beyond the validity of the last record.
        if (t == Long.MIN_VALUE) {
            // if t == MIN_VALUE, this record is added before the first existing record of this key
            DataRecord[] allr = getValidAny(record.getKey());
            long currentValidFrom = Long.MAX_VALUE;
            DataRecord currentFirst = null;
            for (int i=0; allr != null && i<allr.length; i++) {
                if (allr[i].getValidFrom() < currentValidFrom) {
                    currentValidFrom = allr[i].getValidFrom();
                    currentFirst = allr[i];
                }
            }
            if (currentFirst != null) {
                if (record.getValidFrom() >= currentFirst.getValidFrom()) {
                    return;
                }
                record.setInvalidFrom(currentFirst.getValidFrom());
                modifyRecord(record, lockID, true, false, false);
            }
        }
        if (t == Long.MAX_VALUE) {
            // if t == MAX_VALUE, this record is added after the last existing record of this key
            DataRecord[] allr = getValidAny(record.getKey());
            long currentValidFrom = Long.MIN_VALUE;
            DataRecord currentLast = null;
            for (int i=0; allr != null && i<allr.length; i++) {
                if (allr[i].getValidFrom() > currentValidFrom) {
                    currentValidFrom = allr[i].getValidFrom();
                    currentLast = allr[i];
                }
            }
            if (currentLast != null) {
                if (record.getInvalidFrom() <= currentLast.getInvalidFrom()) {
                    return;
                }
                record.setValidFrom(currentLast.getInvalidFrom());
                modifyRecord(record, lockID, true, false, false);
            }
        }
    }

    public void addAll(DataRecord[] records, long lockID) throws EfaException {
        for (DataRecord r : records) {
            add(r, lockID);
        }
    }

    public DataRecord update(DataRecord record) throws EfaException {
        return modifyRecord(record, 0, false, true, false);
    }

    public DataRecord update(DataRecord record, long lockID) throws EfaException {
        return modifyRecord(record, lockID, false, true, false);
    }

    public void delete(DataKey key) throws EfaException {
        modifyRecord(get(key), 0, false, false, true);
    }

    public void delete(DataKey key, long lockID) throws EfaException {
        modifyRecord(get(key), lockID, false, false, true);
    }

    /**
     * This function is needed to ensure the deletion is not forwarded to the efacloud server.
     * For add an update the record is used and contains already the isCopyFromServer flag = true.
     * For delete, however, the data key is normally used and this has no such flag.
     */
    protected void deleteLocal(DataKey key, long lockID) throws EfaException {
        DataRecord record = get(key);
        if (record != null) {
            record.isCopyFromServer = true;
            modifyRecord(record, lockID, false, false, true);
        }
    }

    public void deleteVersionized(DataKey key, int merge) throws EfaException {
        deleteVersionized(key, merge, 0);
    }

    public void deleteVersionized(DataKey key, int merge, long lockID) throws EfaException {
        if (!meta.versionized) {
            throw new EfaException(Logger.MSG_DATA_INVALIDVERSIONIZEDDATA, getUID() + ": Attempt to delete versionized data from an unversionized storage object", Thread.currentThread().getStackTrace());
        }
        long myLock = -1;
        if (lockID <= 0) {
            // acquire a new global lock
            myLock = acquireGlobalLock();
        } else {
            // verify existing lock
            myLock = (dataLocks.hasGlobalLock(lockID) ? lockID : -1);
        }
        if (myLock > 0) {
            try {
                synchronized (data) {
                    DataRecord r = getValidAt(key, (Long)key.getKeyPart(keyFields.length - 1)); // VALID_FROM is always the last key field!
                    if (r != null) {
                        modifyRecord(r, myLock, false, false, true);
                        if (merge != 0 && isValidAny(key)) {
                            if (merge == -1) { // merge with left record
                                DataRecord r2 = getValidAt(key, r.getValidFrom() - 1);
                                if (r2 != null) {
                                    r2.setInvalidFrom(r.getInvalidFrom());
                                    modifyRecord(r2, myLock, false, true, false);
                                }
                            }
                            if (merge == 1) { // merge with right record
                                DataRecord r2 = getValidAt(key, r.getInvalidFrom());
                                if (r2 != null) {
                                    modifyRecord(r2, myLock, false, false, true);
                                    r2.setValidFrom(r.getValidFrom());
                                    modifyRecord(r2, myLock, true, false, false);
                                }
                            }
                        }
                    }
                }
            } finally {
                if (lockID <= 0 && myLock > 0) {
                    releaseGlobalLock(myLock);
                }
            }
        } else {
            throw new EfaException(Logger.MSG_DATA_NOLOCKHELD, getUID() + ": Attempt to delete versionized data without holding a lock", Thread.currentThread().getStackTrace());
        }
    }

    public void deleteVersionizedAll(DataKey key, long deleteAt) throws EfaException {
        deleteVersionizedAll(key, deleteAt, 0);
    }

    public void deleteVersionizedAll(DataKey key, long deleteAt, long lockID) throws EfaException {
        if (!meta.versionized) {
            throw new EfaException(Logger.MSG_DATA_INVALIDVERSIONIZEDDATA, getUID() + ": Attempt to delete versionized data from an unversionized storage object", Thread.currentThread().getStackTrace());
        }
        long myLock = -1;
        if (lockID <= 0) {
            // acquire a new global lock
            myLock = acquireGlobalLock();
        } else {
            // verify existing lock
            myLock = (dataLocks.hasGlobalLock(lockID) ? lockID : -1);
        }
        if (myLock > 0) {
            try {
                synchronized (data) {
                    DataRecord[] records = persistence.data().getValidAny(key);
                    if (deleteAt < 0) {
                        // delete all versions of this record
                        for (int i = 0; records != null && i < records.length; i++) {
                            if (!records[i].getDeleted()) {
                                records[i].setDeleted(true);
                                modifyRecord(records[i], myLock, false, true, false);
                            }
                        }
                    } else {
                        // only mark "delete" (or mark invalid) starting at deleteAt
                        for (int i = 0; records != null && i < records.length; i++) {
                            if (records[i].getValidFrom() < deleteAt) {
                                if (records[i].getInvalidFrom() <= deleteAt) {
                                    // nothing to do
                                } else {
                                    records[i].setInvalidFrom(deleteAt);
                                    modifyRecord(records[i], myLock, false, true, false);
                                }
                            } else {
                                if (records.length == 1) {
                                    records[i].setDeleted(true);
                                    modifyRecord(records[i], myLock, false, true, false);
                                } else {
                                    modifyRecord(records[i], myLock, false, false, true);
                                }
                            }
                        }
                    }
                }
            } finally {
                if (lockID <= 0 && myLock > 0) {
                    releaseGlobalLock(myLock);
                }
            }
        } else {
            throw new EfaException(Logger.MSG_DATA_NOLOCKHELD, getUID() + ": Attempt to delete all versionized data without holding a lock", Thread.currentThread().getStackTrace());
        }
    }

    public void changeValidity(DataRecord record, long validFrom, long invalidFrom) throws EfaException {
        changeValidity(record, validFrom, invalidFrom, 0);
    }

    public void changeValidity(DataRecord record, long validFrom, long invalidFrom, long lockID) throws EfaException {
        if (!meta.versionized) {
            throw new EfaException(Logger.MSG_DATA_INVALIDVERSIONIZEDDATA, getUID() + ": Attempt to change versionized data in an unversionized storage object", Thread.currentThread().getStackTrace());
        }
        if (validFrom < 0 || invalidFrom <= validFrom) {
            throw new EfaException(Logger.MSG_DATA_VERSIONIZEDDATACONFLICT, getUID() + ": Attempt to change versionized data with incorrect validity", Thread.currentThread().getStackTrace());
        }
        long myLock = -1;
        if (lockID <= 0) {
            // acquire a new global lock
            myLock = acquireGlobalLock();
        } else {
            // verify existing lock
            myLock = (dataLocks.hasGlobalLock(lockID) ? lockID : -1);
        }
        if (myLock > 0) {
            try {
                synchronized (data) {
                    DataRecord rNext = null;
                    if (invalidFrom != record.getInvalidFrom()) {
                        rNext = getValidAt(record.getKey(), record.getInvalidFrom());
                        if (rNext != null) {
                            // there is a record to the right, so we first delete this right record, and
                            // later add it again with a new validFrom value, which will automatically set
                            // this record's invalidFrom accordingly
                            deleteVersionized(rNext.getKey(), -1, myLock);
                        } else {
                            // there is no record to the right, so we just change this record's invalidFrom
                            // to the new value
                            record.setInvalidFrom(invalidFrom);
                            update(record, myLock);
                        }
                    }
                    if (validFrom != record.getValidFrom()) {
                        deleteVersionized(record.getKey(), -1, myLock);
                        addValidAt(record, validFrom, myLock);
                    }
                    if (rNext != null) {
                        addValidAt(rNext, invalidFrom, myLock);
                    }
                }
            } finally {
                if (lockID <= 0 && myLock > 0) {
                    releaseGlobalLock(myLock);
                }
            }
        } else {
            throw new EfaException(Logger.MSG_DATA_NOLOCKHELD, getUID() + ": Attempt to change validity without holding a lock", Thread.currentThread().getStackTrace());
        }
    }

    public DataRecord get(DataKey key) throws EfaException {
        synchronized (data) {
            DataRecord rec = data.get(key);
            if (rec != null) {
                return rec.cloneRecord();
            }
            return null;
        }
    }

    public DataRecord[] getValidAny(DataKey key) throws EfaException {
        DataRecord[] recs;
        synchronized(data) { // always synchronize on data to ensure integrity!
            ArrayList<DataKey> list = versionizedKeyList.get(getUnversionizedKey(key));
            if (list == null || list.size() == 0) {
                return null;
            }
            recs = new DataRecord[list.size()];
            int i=0;
            for (DataKey k : list) {
                recs[i++] = get(k);
            }
        }
        return recs;
    }

    public DataRecord getValidAt(DataKey key, long t) throws EfaException {
        int validFromField;
        if (meta.versionized) {
            validFromField = keyFields.length - 1; // VALID_FROM is always the last key field!
        } else {
            return null;
        }
        synchronized(data) { // always synchronize on data to ensure integrity!
            ArrayList<DataKey> list = versionizedKeyList.get(getUnversionizedKey(key));
            if (list == null) {
                return null;
            }
            for (DataKey k : list) {
                long validFrom = (Long)k.getKeyPart(validFromField);
                if (t >= validFrom) {
                    DataRecord rec = get(k);
                    if (rec != null && (rec.isValidAt(t) ||
                            // if we change both validFrom and InvalidFrom at the same time,
                            // then for a short time during this operation, we might have a record
                            // with invalidFrom < validFrom. Since the validity range must always be
                            // >= 1, we accept a record as valid either if t is in it's validy range,
                            // or if t is exactly the validity begin
                            t == validFrom)) {
                        return rec;
                    }
                }
            }
        }
        return null;
    }

    public DataRecord getValidLatest(DataKey key) throws EfaException {
        int validFromField;
        if (meta.versionized) {
            validFromField = keyFields.length - 1; // VALID_FROM is always the last key field!
        } else {
            return null;
        }
        synchronized(data) { // always synchronize on data to ensure integrity!
            ArrayList<DataKey> list = versionizedKeyList.get(getUnversionizedKey(key));
            if (list == null) {
                return null;
            }
            DataKey latestVersionKey = null;
            DataRecord latestVersionRec = null;
            for (DataKey k : list) {
                long validFrom = (Long)k.getKeyPart(validFromField);
                if (latestVersionKey == null || validFrom > (Long)latestVersionKey.getKeyPart(validFromField)) {
                    DataRecord r = get(k);
                    if (!r.getDeleted()) {
                        latestVersionKey = k;
                        latestVersionRec = r;
                    }
                }
            }
            if (latestVersionRec != null) {
                return latestVersionRec;
            }
        }
        return null;
    }

    public DataRecord getValidNearest(DataKey key, long earliestValidAt, long latestValidAt, long preferredValidAt) throws EfaException {
        synchronized(data) { // always synchronize on data to ensure integrity!
            DataRecord r = getValidAt(key, preferredValidAt);
            if (r != null) {
                return r;
            }
            DataRecord[] records = getValidAny(key);
            long minDistance = Long.MAX_VALUE;
            for (int i=0; records != null && i<records.length; i++) {
                if (records[i].isInValidityRange(earliestValidAt, latestValidAt)) {
                    long myDist = Long.MAX_VALUE;
                    if (records[i].getInvalidFrom()-1 < preferredValidAt) {
                        myDist = preferredValidAt - records[i].getInvalidFrom()-1;
                    }
                    if (records[i].getValidFrom() > preferredValidAt) {
                        myDist = records[i].getValidFrom() - preferredValidAt;
                    }
                    if (myDist < minDistance) {
                        minDistance = myDist;
                        r = records[i];
                    }
                }
            }
            return r;
        }
    }

    public boolean isValidAny(DataKey key) throws EfaException {
        synchronized(data) { // always synchronize on data to ensure integrity!
            ArrayList<DataKey> list = versionizedKeyList.get(getUnversionizedKey(key));
            if (list == null || list.size() == 0) {
                return false;
            }
        }
        return true;
    }

    private DataIndex findIndex(int[] idxFields) {
        for (DataIndex idx : indices) {
            if (Arrays.equals(idxFields, idx.getIndexFields())) {
                return idx;
            }
        }
        return null;
    }

    public DataKey[] getByFields(String[] fieldNames, Object[] values) throws EfaException {
        return getByFields(fieldNames, values, -1);
    }

    public DataKey[] getByFields(String[] fieldNames, Object[] values, long validAt) throws EfaException {
        int[] idxFields = new int[fieldNames.length];
        for (int i=0; i<idxFields.length; i++) {
            idxFields[i] = meta.getFieldIndex(fieldNames[i]);
        }
        DataIndex idx = findIndex(idxFields);
        if (idx != null) {
            // Search by using index
            DataKey[] keys = idx.search(values);
            if (keys == null || keys.length == 0 || validAt < 0) {
                return keys;
            }
            // for versionized index search, now select only keys from the valid range
            ArrayList<DataKey> keyList = new ArrayList<DataKey>();
            for (int i=0; i<keys.length; i++) {
                DataRecord r = this.get(keys[i]);
                if (r != null && r.isValidAt(validAt)) {
                    keyList.add(keys[i]);
                }
            }
            return keyList.toArray(new DataKey[0]);
        } else {
            // Search without index
            
            // transfer field names to indices
            int[] fieldIdx = new int[fieldNames.length];
            for (int i=0; i<fieldNames.length; i++) {
                fieldIdx[i] = (values[i] != null ? meta.getFieldIndex(fieldNames[i]) : -1);
            }
            
            // now search all records for matching ones
            ArrayList<DataKey> matches = new ArrayList<DataKey>();
            DataKeyIterator it = getStaticIterator();
            DataKey key = it.getFirst();
            while (key != null) {
                DataRecord rec = this.get(key);
                if (rec != null) {
                    boolean matching = true;
                    for (int i=0; matching && i<fieldIdx.length; i++) {
                        if (fieldIdx[i] >= 0 && !values[i].equals(rec.get(fieldIdx[i]))) {
                            matching = false;
                        }
                        if (validAt >= 0 && (rec.getValidFrom() > validAt || rec.getInvalidFrom() <= validAt) ||
                                rec.getDeleted()) {
                            matching = false;
                        }
                    }
                    if (matching) {
                        matches.add(key);
                    }
                }
                key = it.getNext();
            }
            if (matches.size() > 0) {
                return matches.toArray(new DataKey[0]);
            } else {
                return null;
            }
        }
    }

    public long countRecords(String[] fieldNames, Object[] values) throws EfaException {
        DataKey[] k = getByFields(fieldNames, values);
        return (k == null ? 0 : k.length);
    }

    public long getNumberOfRecords() throws EfaException {
        synchronized(data) {
            return data.size();
        }
    }

    protected void clearAllData() {
        if (data != null) {
            synchronized (data) {
                data.clear();
                versionizedKeyList.clear();
                for (DataIndex idx : indices) {
                    idx.clear();
                }
            }
        }
    }

    public void truncateAllData() throws EfaException {
        long lockID = acquireGlobalLock();
        try {
            synchronized (data) {
                if (inOpeningStorageObject || journal.log(scn + 1, Journal.Operation.truncate, null)) {
                    clearAllData();
                    if (!inOpeningStorageObject) {
                        scn++;
                    }
                } else {
                    throw new EfaException(Logger.MSG_DATA_TRUNCATEFAILED, getUID() + ": Truncate failed", Thread.currentThread().getStackTrace());
                }
            }
        } finally {
            if (lockID > 0) {
                releaseGlobalLock(lockID);
            }
        }
        if (fileWriter != null) { // may be null while reading (opening) a file
            fileWriter.save(false, true);
        }
    }

    public DataKey[] getAllKeys() throws EfaException {
        DataKey[] keys = null;
        synchronized(data) {
            if (cachedKeys == null || getSCN() != cachedKeysSCN) {
                keys = new DataKey[data.size()];
                keys = data.keySet().toArray(keys);
            }
        }
        if (keys != null) {
            Arrays.sort(keys);
            cachedKeys = keys;
        }
        return cachedKeys;
    }

    public DataKeyIterator getStaticIterator() throws EfaException {
        return new DataKeyIterator(this, getAllKeys(), false);
    }

    public DataKeyIterator getDynamicIterator() throws EfaException {
        return new DataKeyIterator(this, getAllKeys(), true);
    }

    public DataRecord getCurrent(DataKeyIterator it) throws EfaException {
        return get(it.getCurrent());
    }

    public DataRecord getFirst(DataKeyIterator it) throws EfaException {
        return get(it.getFirst());
    }

    public DataRecord getLast(DataKeyIterator it) throws EfaException {
        return get(it.getLast());
    }

    public DataRecord getNext(DataKeyIterator it) throws EfaException {
        return get(it.getNext());
    }

    public DataRecord getPrev(DataKeyIterator it) throws EfaException {
        return get(it.getPrev());
    }

    public DataRecord getFirst() throws EfaException {
        return getFirst(getStaticIterator());
    }

    public DataRecord getLast() throws EfaException {
        return getLast(getStaticIterator());
    }

    public void flush() {
        try {
            fileWriter.save(true, false);
        } catch(Exception e) {
            Logger.log(e);
        }
    }

}
