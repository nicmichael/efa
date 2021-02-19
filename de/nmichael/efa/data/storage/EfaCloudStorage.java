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

package de.nmichael.efa.data.storage;

import de.nmichael.efa.data.efacloud.*;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import java.util.ArrayList;

import static de.nmichael.efa.data.efacloud.TxRequestQueue.TX_PENDING_QUEUE_INDEX;
import static de.nmichael.efa.data.efacloud.TxRequestQueue.TX_SYNCH_QUEUE_INDEX;

/**
 * In order to provide a local cache, the standard XML file storage is used, but a function to trigger the web
 * transactions are added.
 */
public class EfaCloudStorage extends XMLFile {

    // for debugging 5.1.21 only
    public final TxRequestQueue txQueue;
    private final String tablename;

    /**
     * Create a Web DB access. This is a local storage (csv) for caching and offline usage and an interface to an efaDB
     * server.
     *
     * @param directory       directory of the cached file
     * @param filename        filename of the cached file
     * @param extension       extension of the cached file. This also indicated the data base table schema
     * @param description     The storage type description
     * @throws EfaException If the first nop operation returns an authentication failure.
     */
    public EfaCloudStorage(String directory, String filename, String extension, String description) throws
            EfaException {
        super(directory, filename, extension, description);
        this.txQueue = TxRequestQueue.getInstance();
        tablename = getStorageObjectType();
    }

    @Override
    public int getStorageType() {
        return IDataAccess.TYPE_EFA_CLOUD;
    }

    /**
     * Simple getter for use by the off-package SynchControl class
     *
     * @param dataRecord data record hich shall have a LastModification value set
     * @return The LastModification value: insert, update, or delete.
     */
    public String getLastModification(DataRecord dataRecord) {
        return dataRecord.LastModification;
    }

    /**
     * Build a modify transaction for the efacloud api and append it to the server transaction queue. The selected queue
     * depends on the mode: it will be either the synching queue (Synchronization mode) or the pending queue (all
     * other).
     *
     * @param dataRecord    the new or changed record, or the record to delete
     * @param add           set true, if the modify transaction is an insert to
     * @param update        set true, if the modify transaction is an update
     * @param delete        set true, if the modify transaction is a record deletion
     * @param useSynchQueue set true, to append the transaction to the synchronization transactions queue rather than to
     *                      the pending transactions queue
     */
    public void modifyServerRecord(DataRecord dataRecord, boolean add, boolean update, boolean delete,
                                   boolean useSynchQueue) {
        Transaction.TX_TYPE type = (add) ? Transaction.TX_TYPE.INSERT : (update) ? Transaction.TX_TYPE.UPDATE :
                (delete) ? Transaction.TX_TYPE.DELETE : Transaction.TX_TYPE.NOP;
        ArrayList<String> record = new ArrayList<String>();
        for (String field : dataRecord.getFields()) {
            String value = dataRecord.getString(field);
            if ((value != null && value.length() > 0) || update)
                record.add(field + ";" + CsvCodec.encodeElement(value, CsvCodec.DEFAULT_DELIMITER,
                        CsvCodec.DEFAULT_QUOTATION));   // fields need no csv encoding
        }
        String[] rArray = record.toArray(new String[0]);
        int queueIndex = (useSynchQueue) ? TX_SYNCH_QUEUE_INDEX : TX_PENDING_QUEUE_INDEX;
        txQueue.appendTransaction(queueIndex, type, tablename, rArray);
    }

    /**
     * Copy server a server side data record into this local data base. This will use the standard modifyDataRecord
     * procedure of its super class XMLfile, but add a flag to the record. That way the standard procedure, can
     * distinguish this call from a normal call and bypass the call of the the modifyServerRecord.
     *
     * @param dataRecord the new or changed record, or the record to delete
     * @param lock       the lock which shall be used (usually a global lock)
     * @param add        set true, if the modify transaction is an insert to
     * @param update     set true, if the modify transaction is an update
     * @param delete     set true, if the modify transaction is a record deletion
     * @throws EfaException if the data manipulation at XMLfile ran into an error.
     */
    public void modifyLocalRecord(DataRecord dataRecord, long lock, boolean add, boolean update, boolean delete) throws
            EfaException {
        dataRecord.isCopyFromServer = true;
        if (add)
            add(dataRecord, lock);
        if (update)
            update(dataRecord, lock);
        if (delete) {
            deleteLocal(constructKey(dataRecord), lock);
        }
    }

    /**
     * Parse a csv table into a list of data records for further handling. This reads only data fields which are part of
     * the data record template and additionally puts the value of "LastModification" into the LastModification field of
     * the dataRecord Object.
     *
     * @param csvString the table to be parsed. The first line must be the header
     * @return ArrayList<DataRecord> with all retrieved data records.
     */
    public ArrayList<DataRecord> parseCsvTable(String csvString) {
        ArrayList<DataRecord> ret = new ArrayList<DataRecord>();
        if (csvString.trim().isEmpty())
            return ret;
        // parse the text
        char d = CsvCodec.DEFAULT_DELIMITER;
        char q = CsvCodec.DEFAULT_QUOTATION;
        int MAX_VALUE_LENGTH = 65536;
        DataRecord dataRecord;
        ArrayList<String> lines = CsvCodec.splitLines(csvString, q);
        if (lines.isEmpty())
            return ret;
        ArrayList<String> header = CsvCodec.splitEntries(lines.get(0));
        // compare with existing records and modify, if needed.
        StorageObject storageObject = this.getPersistence();
        for (int i = 1; i < lines.size(); i++) {
            ArrayList<String> row = CsvCodec.splitEntries(lines.get(i));
            if (row.size() != header.size())
                Logger.log(Logger.WARNING, Logger.MSG_FILE_PARSEERROR, International.getMessage(
                        "Csv-String nicht korrekt. In der Zeile {row} weicht die Länge {rlen} " +
                                "von der Länge der Kopfzeile {hlen} ab. ", i, row.size(), header.size()));
            // Read the record by creating a new one and reading the fields into it.
            dataRecord = storageObject.createNewRecord();
            int c = 0;
            for (String fieldName : header) {
                try {
                    fieldName = storageObject.transformFieldName(fieldName);
                    String value = (row.get(c) != null ? row.get(c).trim() : null);
                    if ((value != null) && (value.length() > MAX_VALUE_LENGTH)) {
                        Logger.log(Logger.ERROR, Logger.MSG_FILE_PARSEERROR, International.getMessage(
                                "efaCloud Tabelle {tablename}: Wert in Datenfeld {datafield} zu lang mit {length} " +
                                        "Bytes. Er wird gekürzt. Die ersten 100 Bytes sind: {value}", tablename,
                                fieldName, "" + value.length(), value.substring(0, 100)));
                        value = value.substring(0, MAX_VALUE_LENGTH);
                    }
                    if (value != null && value.isEmpty())
                        value = null;
                    // Take the value in. Special handling for the server side only field LastModification which will
                    // not become part of the data records field list.
                    // It will only be used during the synchronization process.
                    if (fieldName.equals("LastModification"))
                        dataRecord.LastModification = value;
                    else
                        // the dataRecord.set() function will ignore fields outside its metadata definition
                        dataRecord.set(fieldName, value, false);
                    c++;
                } catch (Exception e) {
                    Logger.log(Logger.ERROR, Logger.MSG_FILE_PARSEERROR, International.getMessage(
                            "efaCloud Tabelle {tablename}: Lesefehler für Datenfeld {datafiled} = {value}: {error}",
                            tablename, fieldName, ((c < row.size()) ? row.get(c) : "???"), e.toString()));
                }
            }
            ret.add(dataRecord);
        }
        return ret;
    }

}
