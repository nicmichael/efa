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

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.efacloud.*;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
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
        if (this.txQueue == null)
            Dialog.error(International.getMessage("Fehler bei der Initialisierung der Tabelle {tablename}. " +
                    "Bitte prüfe die Projektkonfiguration.", tablename));
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
        // keys may have to be removed at the API, in order not to lose them locally, work with a clone.
        DataRecord apiRecord = dataRecord.cloneRecord();
        ArrayList<String> record = new ArrayList<String>();
        if (add) {
            // the data record must have an ecrid when added.
            String ecrid = dataRecord.getAsText(Ecrid.ECRID_FIELDNAME);
            if ((ecrid == null) || (ecrid.isEmpty())) {
                ecrid = Ecrid.generate();
                if (Logger.isTraceOn(Logger.TT_CLOUD, 1)) {
                	Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFACLOUD, "Creating eCrid: "+ecrid +" " + dataRecord.getKeyAsTextDescription() +" " + dataRecord.getClass().getName() + (dataRecord.metaData.isVersionized() ? " (" +dataRecord.getValidFromTimeString()+ " - " + dataRecord.getValidUntilTimeString() +  ")": ""));
                }
                dataRecord.setFromText(Ecrid.ECRID_FIELDNAME, ecrid);
                apiRecord.setFromText(Ecrid.ECRID_FIELDNAME, ecrid);
                // store the same ecrid locally
                try {
                    modifyLocalRecord(dataRecord, false, true, false);
                    Ecrid.iEcrids.put(ecrid, dataRecord);
    				if (Logger.isTraceOn(Logger.TT_CLOUD, 6)) {
						Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFACLOUD, "EcridIndex: Adding (1) '" + ecrid + "': " + dataRecord.getKeyAsTextDescription() + " "+ dataRecord.getClass().getName()+ (dataRecord.metaData.isVersionized()? " (" + dataRecord.getValidFromTimeString() + " - "+ dataRecord.getValidUntilTimeString() + ")": ""));
					}                    
                } catch (EfaException e) {
                    // if the local storage fails, synchronisation will fix the problem later.
                	Logger.logdebug(e);
                }
            }
            // the data record must not have an autoincrement field, this is to be set by the server.
            if (dataRecord instanceof LogbookRecord)
                apiRecord.setFromText("EntryId", null);
            else if (dataRecord instanceof BoatDamageRecord)
                apiRecord.setFromText("Damage", null);
            else if (dataRecord instanceof BoatReservationRecord)
                apiRecord.setFromText("Reservation", null);
            else if (dataRecord instanceof MessageRecord)
                apiRecord.setFromText("MessageId", null);
        }
        for (String field : apiRecord.getFields()) {
            String value = apiRecord.getString(field);
            int fieldType = apiRecord.getFieldType(field);
            // numeric fields must not be set to null. They must have a value
            boolean updateOnlyIfNotEmpty = (fieldType == IDataAccess.DATA_BOOLEAN) ||
                    (fieldType == IDataAccess.DATA_INTEGER) ||
                    (fieldType == IDataAccess.DATA_LONGINT) ||
                    (fieldType == IDataAccess.DATA_DECIMAL) ||
                    (fieldType == IDataAccess.DATA_DOUBLE) ||
                    (fieldType == IDataAccess.DATA_DATE) ||
                    (fieldType == IDataAccess.DATA_TIME);
            if ((value != null && !value.isEmpty()) || (update && !updateOnlyIfNotEmpty))
                record.add(field + ";" + CsvCodec.encodeElement(value, CsvCodec.DEFAULT_DELIMITER,
                        CsvCodec.DEFAULT_QUOTATION));   // fields need no csv encoding
        }
        // for logbooks and clubworkbooks several files map to one efaCloud table. The filename is
        // passed as Logbookname of Clubworkbookname field of the record. Now the file used may
        // change during operation, e.g. when merging person objects. Therefore the current storage
        // object record needs to be adjusted.
        if (tablename.equalsIgnoreCase("efa2logbook") || tablename.equalsIgnoreCase("efa2clubworkbook")) {
            TableBuilder.StorageObjectTypeDefinition rtd = Daten.tableBuilder.storageObjectTypes.get(tablename);
            rtd.persistence = (EfaCloudStorage) dataRecord.getPersistence().data();
        }
        String[] rArray = record.toArray(new String[0]);
        int queueIndex = (useSynchQueue) ? TX_SYNCH_QUEUE_INDEX : TX_PENDING_QUEUE_INDEX;
        txQueue.appendTransaction(queueIndex, type, tablename, rArray);
    }

    /**
     * Copy server a server side data record into this local database. This will use the standard modifyDataRecord
     * procedure of its super class XMLfile, but add a flag to the record. That way the standard procedure, can
     * distinguish this call from a normal call and bypass the call of the modifyServerRecord.
     * When updating and the existing record which is found using the ecrid has a different an efa data key,
     * The record is not updated, but the existing one is deleted and the new dataRecord inserted.
     *
     * @param dataRecord the new or changed record, or the record to delete
     * @param add        set true, if the modify transaction is an insert to
     * @param update     set true, if the modify transaction is an update
     * @param delete     set true, if the modify transaction is a record deletion
     * @throws EfaException if the data manipulation at XMLfile ran into an error.
     * @return true, if the modification was executed, false else. Execution may be suppressed due to unsufficient
     * key matching.
     */
    public boolean modifyLocalRecord(DataRecord dataRecord, boolean add, boolean update, boolean delete) throws
            EfaException {
        dataRecord.isCopyFromServer = true;
        boolean done = false;
        long globalLock = -1L;
        try {
            if (add) {
                globalLock = acquireGlobalLock();
                add(dataRecord, globalLock);
                releaseGlobalLock(globalLock);
                return true;
            }
            if (update || delete) {
                DataKey newKey = dataRecord.getKey();
                // Try using the ecrid first
                DataRecord existingRecord = Ecrid.iEcrids.get(dataRecord.getAsText(Ecrid.ECRID_FIELDNAME));
                if ((existingRecord != null) && (existingRecord.getAsText(Ecrid.ECRID_FIELDNAME) != null)) {
                    // both existing and new record have an ecrid and they match, because the lookup used the ecrid
                    // if the efa keys match, the operation can be executed
                    DataKey existingKey = existingRecord.getKey();
                    if (newKey.compareTo(existingKey) == 0) {
                        globalLock = acquireGlobalLock();
                        if (update)
                            update(dataRecord, globalLock);
                        else {
                            if (get(existingKey) != null)
                                // the record was not yet locally deleted.
                                delete(existingKey, globalLock);
                        }
                        releaseGlobalLock(globalLock);
                        done = true;
                    } else {
                        // if the local record has a different efa key, insert the dataRecord instead of updating it and do not delete.
                        if (update) {
                            // editing the logbook while the session is open is usually prohibited. Allow temporarily
                            boolean isModifyRecordCallbackEnabled = isPreModifyRecordCallbackEnabled();
                            if (dataRecord instanceof LogbookRecord)
                                setPreModifyRecordCallbackEnabled(false);
                            globalLock = acquireGlobalLock();
                            deleteLocal(existingRecord.getKey(), globalLock);
                            add(dataRecord, globalLock);
                            releaseGlobalLock(globalLock);
                            // reset isModifyRecordCallbackEnabled to cached value
                            setPreModifyRecordCallbackEnabled(isModifyRecordCallbackEnabled);
                            done = true;
                        }
                        // if the local record has a different efa key, do not delete.
                    }

                    // the local record was not found using the ecrid. Try the standard efa key matchhing instead
                } else {
                    // the local record was not found by ecrid matching
                    if (existingRecord == null)
                        existingRecord = get(newKey);
                    // if the existing record has an ecrid identifier it is a different one and must not be modified,
                    // except for the autoincrement record which will always be synchronized to the server
                    if ((existingRecord != null) &&
                            ((existingRecord.getAsText(Ecrid.ECRID_FIELDNAME) == null)
                                    || (existingRecord instanceof AutoIncrementRecord))) {
                        // dataRecord and existingRecord have the same efa data key
                        globalLock = acquireGlobalLock();
                        if (update)
                            update(dataRecord, globalLock);
                        else
                            deleteLocal(constructKey(dataRecord), globalLock);
                        releaseGlobalLock(globalLock);
                        done = true;
                    }
                }
                return done;
            }
            return false;
        } catch (EfaException e) {
            if (globalLock >= 0L)
                releaseGlobalLock(globalLock);
            throw e;
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
                    else {
                    	//Bugfix EFA#74 / https://github.com/nicmichael/efa/issues/138
                    	// the field "ClientSideKey" exists in efaCloud, but not in efa.
                    	// ignore this field when setting values.
                    	if (!fieldName.equals("ClientSideKey")) {
                            // the dataRecord.set() function will ignore fields outside its metadata definition
                            dataRecord.set(fieldName, value, false);
                    	}
                    }
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
    
    /**
     * Special efacloud code which needs to be run after opening a datafile.
     * All ecrids in the newly opened file need to be put into the ecrid cache.
     */
    protected void handlePostOpenStorageObject() {
        if (isEfaCloudAndTableWithEcrid()) {
            if (Logger.isTraceOn(Logger.TT_CLOUD, 1)) {
            	Logger.log(Logger.DEBUG, "Loading ecrids for: "+this.filename);
            }        	
        	Ecrid.addAll(this.getPersistence());
        }
    }
    
    /**
     * Special efacloud code which needs to be run right before closing a datafile.
     * All ecrids of the datafile need to be removed from the ecrid index to avoid excessive memory consumption.
     */
    protected void handleBeforeClosingStorageObject() {
        //remove all ecrids from the ecrid cache from the current storage object
        if (isEfaCloudAndTableWithEcrid()) {
			if (Logger.isTraceOn(Logger.TT_CLOUD, 1)) {
				Logger.log(Logger.DEBUG, "Removing ecrids for: " + this.filename);
			}        	
        	Ecrid.removeAll(this.getPersistence());
        }        	
    }
    
    /**
     * Special efacloud code which needs to be run when modifying a record.
     * This is mostly handling the ecrid index.
     * 
     * When adding a new record, it is added to the ecrid index.
     * When updating an exiting record, it is removed from the ecrid index and added (again),
     * so that the ecrid index points to the newest record.
     * When deleting an existing record, the record is removed from the ecrid index.
     * 
     */
    protected void handleInModifyRecord(DataRecord record, DataRecord newRecord, DataRecord currentRecord, DataKey key, boolean add, boolean update, boolean delete) {
        if (isEfaCloudAndTableWithEcrid()) {
        	String myEcrid = record.getAsString(Ecrid.ECRID_FIELDNAME);        	

        	if ((add|update) && (myEcrid != null)) {
	        	//if we are updating local record, we need to update the ecrid cache with the new Record.
	        	//but only if an ecrid exists for the current record.
	        	if (Ecrid.iEcrids.containsKey(myEcrid)) {
	        		DataRecord removed=Ecrid.iEcrids.remove(myEcrid);
					if (Logger.isTraceOn(Logger.TT_CLOUD, 6)) {
						Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFACLOUD, "EcridIndex: Update '" + myEcrid + "': " + removed.getKeyAsTextDescription() + " "
										+ removed.getClass().getName()
										+ (removed.metaData.isVersionized()
												? " (" + removed.getValidFromTimeString() + " - "
														+ removed.getValidUntilTimeString() + ")"
												: ""));
					}
				} else {
					if (Logger.isTraceOn(Logger.TT_CLOUD, 6)) {
						Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFACLOUD, "EcridIndex: Adding (2) '" + myEcrid + "': " + newRecord.getKeyAsTextDescription() + " "
										+ newRecord.getClass().getName()
										+ (newRecord.metaData.isVersionized()
												? " (" + newRecord.getValidFromTimeString() + " - "
														+ newRecord.getValidUntilTimeString() + ")"
												: ""));
					}
				}
	        	
	        	Ecrid.iEcrids.put(myEcrid, newRecord);
	        	
        	} else if (delete && (myEcrid != null) ) {
                //handle ecrids, if available
        		DataRecord removed=Ecrid.iEcrids.remove(myEcrid);
				if (Logger.isTraceOn(Logger.TT_CLOUD, 6)) {
					Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFACLOUD, "EcridIndex: Removing '" + myEcrid + "': " + removed.getKeyAsTextDescription() + " "
									+ removed.getClass().getName()
									+ (removed.metaData.isVersionized()
											? " (" + removed.getValidFromTimeString() + " - "
													+ removed.getValidUntilTimeString() + ")"
											: ""));
				}        		
        	} else if (delete && myEcrid == null) {
        		EfaUtil.foo();// should not take place
        	}
        }
    }
    
    /**
     * Special efacloud code which needs to be run when modifying a record, in a late stage of DataFile.modifyRecord.
     * 
     */
    protected void handlePostModifyRecord(DataRecord record, DataRecord newRecord, DataKey key, boolean add, boolean update, boolean delete) {
        // check whether an efacloud server shall also be updated, and trigger update, if needed.
        StorageObject rp = record.getPersistence();
        
        // check whether this is an efa Cloud storage object, and the record is not a server copy anyway
        if (rp.dataAccess.getStorageType() == IDataAccess.TYPE_EFA_CLOUD && !inOpeningStorageObject && !record.isCopyFromServer) {
            // if so, trigger server modification
        	this.modifyServerRecord(add || update ? newRecord : record, add, update, delete, false);
        }
    }
    

    
    
    private boolean isEfaCloudAndTableWithEcrid() {
        String fName = filename.substring(filename.lastIndexOf('.') + 1);
        
        boolean retVal = ((Daten.project != null) 
        		&& (Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD)
        		&& TableBuilder.tablenamesWithEcrids.contains(fName));
        
        if (retVal==false) {// just for debug purposes some logging
            if (Logger.isTraceOn(Logger.TT_CLOUD, 1)) {
            	Logger.log(Logger.DEBUG, "No Table with eCrid: "+filename+" "+fName+ "project is null ?"+(Daten.project == null));
            }
        }
        return retVal;
    }

}
