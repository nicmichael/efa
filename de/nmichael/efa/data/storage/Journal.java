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
import java.io.*;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class Journal {

    public static final String JHEADER_JOPENED = "###Journal opened";
    public static final String JHEADER_SOBJECT = "###Storage Object";
    public static final String JHEADER_JNUMBER = "###Journal Number";
    public static final String JHEADER_JGROUP  = "###Journal Group ";
    public static final String JHEADER_1STSCN  = "###Journal 1stSCN";
    public static final String JHEADER_LASTFIELD = JHEADER_1STSCN;

    private static final long scnsPerJournal = 1000;
    private static final int numberOfJournals = 3;

    private boolean FLUSH_WRITES = true;

    enum Operation {
        add,
        update,
        delete,
        truncate
    }

    private String storageObjectName;
    private String storageObjectFilename;

    private long fwnr = -1;
    private BufferedWriter fw;
    private BufferedReader fr;
    private String fwname = null;

    public Journal(String storageObjectName, String storageObjectFilename) {
        this.storageObjectName = storageObjectName;
        this.storageObjectFilename = storageObjectFilename;
        try {
            this.FLUSH_WRITES = Daten.efaConfig.getValueDataFileSynchronousJournal();
        } catch(Exception eignore) {
        }
    }

    public static String getOperationName(Operation operation) {
        switch (operation) {
            case add:
                return "Add";
            case update:
                return "Upd";
            case delete:
                return "Del";
            case truncate:
                return "Trc";
        }
        return null;
    }

    public static Operation getOperationEnum(String operation) {
        if (operation.equals("Add")) {
            return Operation.add;
        }
        if (operation.equals("Upd")) {
            return Operation.update;
        }
        if (operation.equals("Del")) {
            return Operation.delete;
        }
        if (operation.equals("Trc")) {
            return Operation.truncate;
        }
        return null;
    }

    public static String encodeCommand(Operation operation, DataRecord r) {
        return encodeCommand(new StringBuffer(), operation, r);
    }

    public static String encodeCommand(StringBuffer s, Operation operation, DataRecord r) {
        s.append(getOperationName(operation) + ":");
        if (operation != Operation.truncate) {
            s.append(r.encodeAsString());
        }
        return s.toString();
    }

    public boolean close() {
        try {
            if (fw != null) {
                fw.close();
                fwnr = -1;
                fwname = null;
            }
            if (fr != null) {
                fr.close();
                fwnr = -1;
                fwname = null;
            }
        } catch(Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_DATA_JOURNALOPENFAILED,
                        LogString.fileCloseFailed(fwname, International.getString("Journal"), e.toString()));
            return false;
        }
        return true;
    }

    public String getStorageObjectName() {
        return storageObjectName;
    }

    public long getJournalNumber(long scn) {
        return scn / scnsPerJournal;
    }

    public long getJournalGroup(long scn) {
        return getJournalNumber(scn) % numberOfJournals;
    }

    public String getJournalGroupName(long group) {
        return storageObjectFilename + ".j" + group;
    }

    public String getJournalName(long scn) {
        long jg = getJournalGroup(scn);
        return getJournalGroupName(jg);
    }

    public boolean isOpenNewJournal(long scn) {
        return scn == 1 || (scn % scnsPerJournal) == 0;
    }

    private BufferedWriter openForAppend(long scn) {
        if (scn < 1) {
            return null;
        }
        long jnr = getJournalNumber(scn);
        if (jnr != fwnr || fw == null) {
            String journalName = getJournalName(scn);
            try {
                if (isOpenNewJournal(scn)) {
                    // open with overwrite
                    fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(journalName, false), Daten.ENCODING_UTF));
                    fw.write(JHEADER_JOPENED + ": " + EfaUtil.getCurrentTimeStampYYYY_MM_DD_HH_MM_SS() + "\n");
                    fw.write(JHEADER_SOBJECT + ": " + storageObjectName + "\n");
                    fw.write(JHEADER_JNUMBER + ": " + jnr + "\n");
                    fw.write(JHEADER_JGROUP + ": " + getJournalGroup(scn) + "\n");
                    fw.write(JHEADER_1STSCN + ": " + scn + "\n");
                } else {
                    // open with append
                    fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(journalName, true), Daten.ENCODING_UTF));
                }
                fwnr = jnr;
                fwname = journalName;
            } catch (Exception e) {
                Logger.log(Logger.ERROR, Logger.MSG_DATA_JOURNALOPENFAILED,
                        LogString.fileCreationFailed(journalName, International.getString("Journal"), e.toString()));
                fw = null;
                fwnr = -1;
                fwname = null;
            }
        }
        return fw;
    }

    private boolean openForRead(long jgrp) {
        String journalName = getJournalGroupName(jgrp);
        try {
            if (!new File(journalName).exists()) {
                return false;
            }
            fr = new BufferedReader(new InputStreamReader(new FileInputStream(journalName), Daten.ENCODING_UTF));
            String s;
            boolean headerComplete = false;
            while ((s = fr.readLine()) != null) {
                s = s.trim();
                if (s.startsWith(JHEADER_SOBJECT)) {
                    storageObjectName = s.substring(JHEADER_SOBJECT.length() + 2);
                }
                if (s.startsWith(JHEADER_LASTFIELD)) {
                    break;
                }
            }
            return true;
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_DATA_JOURNALOPENFAILED,
                    LogString.fileOpenFailed(journalName, International.getString("Journal"), e.toString()));
            fr = null;
            return false;
        }
    }

    private String readNextLine() {
        try {
            return fr.readLine();
        } catch(Exception e) {
            return null;
        }
    }

    private long getScnFromJournalLine(String s) {
        try {
            s = s.trim();
            if (s.startsWith("#")) {
                int pos = s.indexOf(":");
                if (pos > 0) {
                    return Long.parseLong(s.substring(1, pos));
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return -1;
    }

    private long getTimestampFromJournalLine(String s) {
        try {
            s = s.trim();
            if (s.startsWith("#")) {
                int pos1 = s.indexOf(":");
                int pos2 = s.indexOf(":", pos1+1);
                if (pos1 > 0 && pos2 > 0) {
                    return Long.parseLong(s.substring(pos1+1, pos2));
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return -1;
    }

    private Operation getOperationFromJournalLine(String s) {
        try {
            s = s.trim();
            if (s.startsWith("#")) {
                int pos1 = s.indexOf(":");
                int pos2 = s.indexOf(":", pos1+1);
                int pos3 = s.indexOf(":", pos2+1);
                if (pos2 > 0 && pos3 > 0) {
                    return getOperationEnum(s.substring(pos2+1, pos3));
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    private DataRecord getDataRecordFromJournalLine(String s, IDataAccess dataAccess) {
        try {
            s = s.trim();
            if (s.startsWith("#")) {
                int pos1 = s.indexOf(":");
                int pos2 = s.indexOf(":", pos1+1);
                int pos3 = s.indexOf(":", pos2+1);
                if (pos3 > 0) {
                    s = s.substring(pos3+1);
                    if (s.length() > 0) {
                        DataRecord r = dataAccess.getPersistence().createNewRecord();
                        XMLReader parser = EfaUtil.getXMLReader();
                        DataRecordReader dataRecordReader = new DataRecordReader(r);
                        parser.setContentHandler(dataRecordReader);
                        parser.parse(new InputSource(new StringReader(XmlHandler.XML_HEADER + s)));
                        return r;
                    }
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    private long readLastScn() {
        try {
            String s;
            long scn = -1;
            while ( (s = fr.readLine()) != null) {
                s = s.trim();
                if (s.startsWith("#")) {
                    int pos = s.indexOf(":");
                    if (pos > 0) {
                        scn = Long.parseLong(s.substring(1, pos));
                    }
                }
            }
            return scn;
        } catch (Exception e) {
            return -1;
        }
    }

    public String getLogString(long scn, Operation operation, DataRecord r) {
        StringBuffer s = new StringBuffer();
        s.append("#" + scn + ":");
        s.append(System.currentTimeMillis() + ":");
        return encodeCommand(s, operation, r);
    }

    public boolean log(long scn, Operation operation, DataRecord r) {
        try {
            BufferedWriter f = openForAppend(scn);
            String s = getLogString(scn, operation, r);
            if (s == null || s.length() == 0) {
                Logger.log(Logger.ERROR, Logger.MSG_DATA_JOURNALWRITEFAILED,
                        LogString.fileWritingFailed(fwname, International.getString("Journal"), "empty log string"));
                return false;
            }
            f.write(s + "\n");
            if (FLUSH_WRITES) {
                f.flush();
            }
        } catch(Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_DATA_JOURNALWRITEFAILED,
                        LogString.fileWritingFailed(fwname, International.getString("Journal"), e.toString()));
            return false;
        }
        return true;
    }

    public void deleteAllJournals() throws EfaException {
        for (int i = 0; i < numberOfJournals; i++) {
            String filename = getJournalGroupName(i);
            try {
                File f = new File(filename);
                if (f.isFile()) {
                    if (!f.delete()) {
                        throw new Exception(LogString.fileDeletionFailed(filename, International.getString("Journal")));
                    }
                }
            } catch (Exception e) {
                throw new EfaException(Logger.MSG_DATA_DELETEFAILED, LogString.fileDeletionFailed(filename, International.getString("Journal"), e.toString()), Thread.currentThread().getStackTrace());
            }
        }
    }

    public static long getLatestScnFromJournals(String storageObjectName, String storageObjectFilename) {
        long scn = -1;
        for (int i=0; i<numberOfJournals; i++) {
            Journal j = new Journal(storageObjectName, storageObjectFilename);
            if (j.openForRead(i) && storageObjectName.equals(j.getStorageObjectName())) {
                 scn = Math.max(scn, j.readLastScn());
            }
        }
        return scn;
    }

    public static long rollForward(DataFile dataFile,
            String storageObjectName, String storageObjectFilename, long latestScn) throws Exception {

        long myScn = dataFile.getSCN() + 1;
        Logger.log(Logger.INFO, Logger.MSG_DATA_REPLAYSTART,
                    LogString.operationStarted(
                        International.getMessage("Nachfahren von Änderungen bis SCN {scn}",latestScn)));

        long jgrpLast = -1;
        Journal j = new Journal(storageObjectName, storageObjectFilename);
        while (myScn <= latestScn) {
            long jgrp = j.getJournalGroup(myScn);
            if (jgrp != jgrpLast) {
                j.close();
                j.openForRead(jgrp);
                jgrpLast = jgrp;
            }

            String s = j.readNextLine();
            if (s == null) {
                Logger.log(Logger.ERROR, Logger.MSG_DATA_REPLAYINCOMPLETE,
                        LogString.operationStarted(
                        International.getString("Nachfahren von Änderungen unvollständig")));
                break;
            }
            long thisScn = j.getScnFromJournalLine(s);
            if (thisScn < myScn) {
                continue;
            }
            Operation op = j.getOperationFromJournalLine(s);
            DataRecord r = j.getDataRecordFromJournalLine(s, dataFile);

            if (thisScn != myScn) {
                throw new Exception ("Expected SCN " + myScn + ", but found SCN " + thisScn);
            }
            if (op == null) {
                throw new Exception ("No Operation found for SCN " + myScn);
            }
            if (op != Operation.truncate && r == null) {
                throw new Exception ("No Data Record found for SCN " + myScn);
            }
            try {
                switch (op) {
                    case add:
                        dataFile.add(r);
                        break;
                    case update:
                        dataFile.update(r);
                        break;
                    case delete:
                        dataFile.delete(r.getKey());
                        break;
                    case truncate:
                        dataFile.truncateAllData();
                        break;
                }
                myScn++;
            } catch (Exception e) {
                Logger.log(e);
                throw new Exception("Failed to apply journal entry '" + s + "': " + e);
            }
        }
        if (j != null) {
            j.close();
        }
        Logger.log(Logger.INFO, Logger.MSG_DATA_REPLAYFINISHED,
                    LogString.operationFinished(
                        International.getMessage("Nachfahren von Änderungen bis SCN {scn}", myScn-1)));
        return myScn-1;
    }

}
