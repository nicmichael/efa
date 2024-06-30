/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data;

import de.nmichael.efa.util.*;
import de.nmichael.efa.ex.*;
import de.nmichael.efa.data.storage.*;
import java.util.*;

// @i18n complete

public class Boats extends StorageObject {

    public static final String DATATYPE = "efa2boats";
    public BoatRecord staticBoatRecord;

    public Boats(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Boote"));
        BoatRecord.initialize();
        staticBoatRecord = (BoatRecord)createNewRecord();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new BoatRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public BoatRecord createBoatRecord(UUID id) {
        BoatRecord r = new BoatRecord(this, MetaData.getMetaData(DATATYPE));
        r.setId(id);
        return r;
    }

    public DataKey addNewBoatRecord(BoatRecord boat, long validFrom) throws EfaException {
        DataKey k = data().addValidAt(boat, validFrom);
        getProject().getBoatStatus(false).data().add(getProject().getBoatStatus(false).createBoatStatusRecord(boat.getId(), boat.getQualifiedName()));
        return k;
    }

    public BoatRecord getBoat(UUID id, long validAt) {
        try {
            return (BoatRecord)data().getValidAt(BoatRecord.getKey(id, validAt), validAt);
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public BoatRecord getBoat(UUID id, long earliestValidAt, long latestValidAt, long preferredValidAt) {
        try {
            return (BoatRecord)data().getValidNearest(BoatRecord.getKey(id, preferredValidAt), earliestValidAt, latestValidAt, preferredValidAt);
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    // find a record being valid at the specified time
    public BoatRecord getBoat(String boatName, long validAt) {
        try {
            DataKey[] keys = data().getByFields(
                staticBoatRecord.getQualifiedNameFields(), staticBoatRecord.getQualifiedNameValues(boatName), validAt);
            if (keys == null || keys.length < 1) {
                return null;
            }
            for (int i=0; i<keys.length; i++) {
                BoatRecord r = (BoatRecord)data().get(keys[i]);
                if (r.isValidAt(validAt)) {
                    return r;
                }
            }
            return null;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }
    
    /**
     * Get a Boat record for the case insensitive name of the boat
     * @param boatName 
     * @param validAt
     * @return 
     */
    public BoatRecord getBoatCaseInsensitive(String boatName, long validAt) {
    	try {
    		String[] boatNameContents= staticBoatRecord.getQualifiedNameValues(boatName);
            DataKey[] keys = data().getAllKeys();
            
            for (int curKey=0; curKey<keys.length; curKey++) {
            	BoatRecord myBoatRec=(BoatRecord) data().get(keys[curKey]);
            	
            	String recName = myBoatRec.getName().trim();
            	String boatNameName = boatNameContents[0].trim();
            	if (recName.equalsIgnoreCase(boatNameName)) {
            		//hey, we have a match on the boat name
            		if (boatNameContents[1]!=null && !boatNameContents[1].trim().isEmpty()){
            			if (myBoatRec.getNameAffix().equalsIgnoreCase(boatNameContents[1].trim())) {
            				return myBoatRec;
            			} else {
            				continue; //next item
            			}
            		} else {
            			return myBoatRec;
            		}
            	}
            }
            return null;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }    	
    }
    
    // find any record being valid at least partially in the specified range
    public BoatRecord getBoat(String boatName, long validFrom, long validUntil, long preferredValidAt) {
        try {
            DataKey[] keys = data().getByFields(
                    staticBoatRecord.getQualifiedNameFields(), staticBoatRecord.getQualifiedNameValues(boatName));
            if (keys == null || keys.length < 1) {
                return null;
            }
            BoatRecord candidate = null;
            for (int i=0; i<keys.length; i++) {
                BoatRecord r = (BoatRecord)data().get(keys[i]);
                if (r != null) {
                    if (r.isInValidityRange(validFrom, validUntil)) {
                        candidate = r;
                        if (preferredValidAt >= r.getValidFrom() && preferredValidAt < r.getInvalidFrom()) {
                            return r;
                        }
                    }
                }
            }
            return candidate;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public boolean isValidAt(UUID boatId, long validAt) {
        try {
            DataRecord r = data().getValidAt(BoatRecord.getKey(boatId, validAt), validAt);
            return r != null;
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return false;
    }

    // the idea of this function is to provide any boat record for boatId, if it doesn't matter which version we get
    public BoatRecord getAnyBoatRecord(UUID boatId) {
        try {
            DataKey k = BoatRecord.getKey(boatId, -1);
            // first try to find the currently valid record (this is usually a fast operation, especially for remote access)
            DataRecord record = data().getValidAt(k, System.currentTimeMillis());
            if (record != null) {
                return (BoatRecord)record;
            }
            // if we haven't found a record, go for some other version of this record (if any)
            DataRecord[] records = data().getValidAny(BoatRecord.getKey(boatId, -1));
            if (records != null && records.length > 0) {
                return (BoatRecord)records[0];
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public boolean isBoatDeleted(UUID boatId) {
        BoatRecord r = getAnyBoatRecord(boatId);
        return (r != null && r.getDeleted());
    }

    public boolean isBoatInvisible(UUID boatId) {
        BoatRecord r = getAnyBoatRecord(boatId);
        return (r != null && r.getInvisible());
    }

    public boolean isBoatDeletedOrInvisible(UUID boatId) {
        BoatRecord r = getAnyBoatRecord(boatId);
        return (r != null && (r.getDeleted() || r.getInvisible()));
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, BoatRecord.ID);
            assertFieldNotEmpty(record, BoatRecord.NAME);
        }
        if (delete) {
            assertNotReferenced(record, getProject().getBoatDamages(false), new String[] { BoatDamageRecord.BOATID } );
            assertNotReferenced(record, getProject().getBoatReservations(false), new String[] { BoatReservationRecord.BOATID } );
            assertNotReferenced(record, getProject().getBoatStatus(false), new String[] { BoatStatusRecord.BOATID } );
            String[] logbooks = getProject().getAllLogbookNames();
            for (int i=0; logbooks != null && i<logbooks.length; i++) {
                assertNotReferenced(record, getProject().getLogbook(logbooks[i], false), new String[] { LogbookRecord.BOATID } );
            }
        }
    }

    public ProgressTask getMergeBoatsProgressTask(DataKey mainKey, DataKey[] mergeKeys) {
        return new Boats.MergeBoatsProgressTask(this, mainKey, mergeKeys);
    }

    class MergeBoatsProgressTask extends ProgressTask {

        private Boats boats;
        private UUID mainID;
        private UUID[] mergeIDs;
        private int absoluteWork = 100;
        private int errorCount = 0;
        private int warningCount = 0;
        private int updateCount = 0;

        public MergeBoatsProgressTask(Boats boats, DataKey mainKey, DataKey[] mergeKeys) {
            this.boats = boats;
            mainID = (UUID)mainKey.getKeyPart1();
            mergeIDs = new UUID[mergeKeys.length];
            for (int i=0; i<mergeIDs.length; i++) {
                mergeIDs[i] = (UUID)mergeKeys[i].getKeyPart1();
            }
        }

        public int getAbsoluteWork() {
            return absoluteWork;
        }

        public String getSuccessfullyDoneMessage() {
            return International.getString("Datensätze erfolgreich zusammengefügt.") +
                    (errorCount > 0 || warningCount > 0 ?
                        "\n[" + errorCount + " ERRORS, " + warningCount + " WARNINGS]" : "");
        }

        private boolean isIdToBeMerged(UUID id) {
            if (id == null) {
                return false;
            }
            boolean found = false;
            for (int i=0; i<mergeIDs.length; i++) {
                if (id.equals(mergeIDs[i])) {
                    return true;
                }
            }
            return false;
        }

        public void run() {
            setRunning(true);
            logInfo(International.getString("Datensätze zusammenfügen") + " ...\n");
            try {
                Project p = boats.getProject();
                super.resultSuccess = false;

                // Search Logbooks
                String[] logbookNames = p.getAllLogbookNames();
                absoluteWork = (logbookNames != null ? logbookNames.length : 0) + 6;
                int workDone = 0;
                if (logbookNames != null) {
                    for (String logbookName : logbookNames) {
                        logInfo("Searching logbook " + logbookName + " ...\n");
                        Logbook logbook = p.getLogbook(logbookName, false);
                        DataKeyIterator it = logbook.data().getStaticIterator();
                        for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                            LogbookRecord r = (LogbookRecord) logbook.data().get(k);
                            if (r != null) {
                                boolean changed = false;
                                if (isIdToBeMerged(r.getBoatId())) {
                                    r.setBoatId(mainID);
                                    changed = true;
                                }
                                if (changed) {
                                    logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                                    logbook.data().update(r);
                                    updateCount++;
                                }
                            }
                        }
                        setCurrentWorkDone(++workDone);
                    }
                }

                // Search Boat Statis
                logInfo("Searching Boat Status ...\n");
                BoatStatus boatStatus = p.getBoatStatus(false);
                DataKeyIterator it = boatStatus.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    BoatStatusRecord r = (BoatStatusRecord) boatStatus.data().get(k);
                    if (r != null) {
                        if (isIdToBeMerged(r.getBoatId())) {
                            logInfo("Deleting record " + r.getQualifiedName() + " ...\n");
                            boatStatus.data().delete(k);
                            updateCount++;
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Search Boat Damages
                logInfo("Searching Boat Damages ...\n");
                BoatDamages boatDamages = p.getBoatDamages(false);
                it = boatDamages.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    BoatDamageRecord r = (BoatDamageRecord) boatDamages.data().get(k);
                    if (r != null) {
                        boolean changed = false;
                        if (isIdToBeMerged(r.getBoatId())) {
                            r.setBoatId(mainID);
                            changed = true;
                        }
                        if (changed) {
                            logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                            boatDamages.data().update(r);
                            updateCount++;
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Search Boat Reservations
                logInfo("Searching Boat Reservations ...\n");
                BoatReservations boatReservations = p.getBoatReservations(false);
                it = boatReservations.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    BoatReservationRecord r = (BoatReservationRecord) boatReservations.data().get(k);
                    if (r != null) {
                        boolean changed = false;
                        if (isIdToBeMerged(r.getBoatId())) {
                            r.setBoatId(mainID);
                            changed = true;
                        }
                        if (changed) {
                            logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                            boatReservations.data().update(r);
                            updateCount++;
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Search Persons
                logInfo("Searching Persons ...\n");
                Persons persons = p.getPersons(false);
                it = persons.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    PersonRecord r = (PersonRecord) persons.data().get(k);
                    if (r != null) {
                        boolean changed = false;
                        if (isIdToBeMerged(r.getDefaultBoatId())) {
                            r.setDefaultBoatId(mainID);
                            changed = true;
                        }
                        if (changed) {
                            logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                            persons.data().update(r);
                            updateCount++;
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Search Statistics
                logInfo("Searching Statistics ...\n");
                Statistics statistics = p.getStatistics(false);
                it = statistics.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    StatisticsRecord r = (StatisticsRecord) statistics.data().get(k);
                    if (r != null) {
                        boolean changed = false;
                        if (isIdToBeMerged(r.getFilterByBoatId())) {
                            r.setFilterByBoatId(mainID);
                            changed = true;
                        }
                        if (changed) {
                            logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                            statistics.data().update(r);
                            updateCount++;
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Merge Boats and delete old Boats
                logInfo("Merging Records ...\n");
                DataRecord[] mainBoat = boats.data().getValidAny(BoatRecord.getKey(mainID, 0));
                ArrayList<DataRecord> mergedBoats = new ArrayList<DataRecord>();
                for (int i=0; i<mergeIDs.length; i++) {
                    DataRecord[] mergeBoat = boats.data().getValidAny(BoatRecord.getKey(mergeIDs[i], 0));
                    for (int j=0; mergeBoat != null && j<mergeBoat.length; j++) {
                        mergedBoats.add(mergeBoat[j]);
                    }
                }
                long validFrom = Long.MAX_VALUE;
                long invalidFrom = Long.MIN_VALUE;
                for (int i=0; mainBoat != null && i<mainBoat.length; i++) {
                    if (mainBoat[i].getValidFrom() < validFrom) {
                        validFrom = mainBoat[i].getValidFrom();
                    }
                    if (mainBoat[i].getInvalidFrom() > invalidFrom) {
                        invalidFrom = mainBoat[i].getInvalidFrom();
                    }
                }
                Hashtable<Long,DataRecord> validStart = new Hashtable<Long,DataRecord>();
                for (int i=0; i<mergedBoats.size(); i++) {
                    BoatRecord r = (BoatRecord)mergedBoats.get(i).cloneRecord();
                    if (r.getValidFrom() < validFrom && validStart.get(r.getValidFrom()) == null) {
                        r.setId(mainID);
                        logInfo("Merging " + r.getQualifiedName() + " (" + r.getValidRangeString() + ") ...\n");
                        boats.data().addValidAt(r, Long.MIN_VALUE);
                        validStart.put(r.getValidFrom(), r);
                        validFrom = r.getValidFrom();
                    } else if (r.getInvalidFrom() > invalidFrom && validStart.get(invalidFrom) == null) {
                        r.setId(mainID);
                        logInfo("Merging " + r.getQualifiedName() + " (" + r.getValidRangeString() + ") ...\n");
                        boats.data().addValidAt(r, Long.MAX_VALUE);
                        validStart.put(invalidFrom, r);
                        invalidFrom = r.getInvalidFrom();
                    }
                }
                // Deleting merged Records
                logInfo("Deleting merged Records ...\n");
                for (int i=0; i<mergedBoats.size(); i++) {
                    BoatRecord r = (BoatRecord)mergedBoats.get(i);
                    logInfo("Deleting " + r.getQualifiedName() + " (" + r.getValidRangeString() + ") ...\n");
                    boats.data().delete(r.getKey());
                }
                setCurrentWorkDone(++workDone);
                super.resultSuccess = true;
                
            } catch(Exception e) {
                errorCount++;
                this.logInfo("\n" + International.getString("Fehler") + ": " + e.getMessage());
                this.logInfo("\n" + e.toString());
                Logger.logdebug(e);
            }
            this.logInfo("\n" + International.getMessage("{count} Datensätze wurden aktualisiert.", updateCount));
            this.logInfo("\n\n" + International.getMessage("{count} Fehler.", errorCount));
            this.logInfo("\n" + International.getMessage("{count} Warnungen.", warningCount));
            setDone();
        }
    }

}
