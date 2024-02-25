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

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.*;

// @i18n complete

public class DataFileWriter extends Thread {

    public static long SAVE_INTERVAL = 10000; // 10.000 ms

    private DataFile dataFile;
    private volatile boolean writedata = false;
    private volatile long lastSave = 0;

    public DataFileWriter(DataFile dataFile) {
        this.dataFile = dataFile;
        try {
            this.SAVE_INTERVAL = Daten.efaConfig.getValueDataFileSaveInterval() * 1000;
        } catch(Exception eignore) {
        }
    }

    public void run() {
    	this.setName("DataFileWriter: "+dataFile.filename);
        if (Logger.isTraceOn(Logger.TT_FILEIO)) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_WRITETHREAD_RUNNING, "DataFileWriter["+dataFile.filename+"] running.");
        }
        while(dataFile.isStorageObjectOpen()) {
            try {
                if (writedata && System.currentTimeMillis() - lastSave > SAVE_INTERVAL) {
                    if (Logger.isTraceOn(Logger.TT_FILEIO)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_FILE_WRITETHREAD_SAVING, "DataFileWriter["+dataFile.filename+"] found new data to be saved.");
                    }
                        try {
                            dataFile.saveStorageObject(true);
                        } catch(Exception e) {
                            Logger.log(Logger.ERROR, Logger.MSG_FILE_WRITETHREAD_ERROR, "DataFileWriter["+dataFile.filename+"] failed to save data: "+e.toString());
                            Logger.log(e);
                        }
                        lastSave = System.currentTimeMillis();
                        writedata = false;
                } else {
                    Thread.sleep(SAVE_INTERVAL);
                }
            } catch(Exception eglob) {
                // no logging, also not debug exception loggin (too many interrupted exceptions)
            }
        }
        if (Logger.isTraceOn(Logger.TT_FILEIO)) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_WRITETHREAD_EXIT, "DataFileWriter["+dataFile.filename+"] exited.");
        }
    }

   synchronized public void save(boolean synchronous, boolean dataChanged) {
        if ( (synchronous || (dataChanged && !writedata) ) && Logger.isTraceOn(Logger.TT_FILEIO)) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_WRITETHREAD_SAVING, 
                    "DataFileWriter[" + dataFile.filename + "] new " + (dataChanged ? "save" : "flush") + " request queued" + (synchronous ? " (sync)" : "") + ".");
        }
        if (synchronous) {
            lastSave = 0;
        }
        if (dataChanged) {
            writedata = true;
        }
        if (!writedata) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_WRITETHREAD_SAVING, 
                    "DataFileWriter[" + dataFile.filename + "] no unsaved data.");
        }
        if (System.currentTimeMillis() - lastSave > SAVE_INTERVAL) {
            this.interrupt();
        }
        // tries * sleeptime must be greater than SAVE_INTERVAL.
        // Otherwise we might interrupt the thread while it's not in a sleep,
        // and it might go into a sleep right after...
        long sleep = SAVE_INTERVAL / 100;
        int maxTries = 150;
        while (synchronous && writedata && dataFile.isStorageObjectOpen()) {
            try {
                Thread.sleep(sleep);
                if (--maxTries % 50 == 0) {
                    this.interrupt(); // interrupt again (in case thread went to sleep)
                }
            } catch (InterruptedException e) {
                // nothing to do
            }
            if (maxTries <= 0) {
                Logger.log(Logger.ERROR, Logger.MSG_FILE_WRITETHREAD_ERROR,
                        "DataFileWriter["+dataFile.filename+"] synchronous save timed out.");
                break;
            }
        }
    }

    public void exit() {
        this.interrupt();
    }

}
