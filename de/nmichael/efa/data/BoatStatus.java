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

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.ex.EfaModifyException;
import java.util.*;

// @i18n complete

public class BoatStatus extends StorageObject {

    public static final String DATATYPE = "efa2boatstatus";

    public BoatStatus(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Bootsstatus"));
        BoatStatusRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new BoatStatusRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public BoatStatusRecord createBoatStatusRecord(UUID id, String boatText) {
        BoatStatusRecord r = new BoatStatusRecord(this, MetaData.getMetaData(DATATYPE));
        r.setBoatId(id);
        r.setBoatText(boatText);
        r.setBaseStatus(BoatStatusRecord.STATUS_AVAILABLE);
        r.setCurrentStatus(BoatStatusRecord.STATUS_AVAILABLE);
        return r;
    }

    public BoatStatusRecord getBoatStatus(UUID id) {
        try {
            return (BoatStatusRecord)data().get(BoatStatusRecord.getKey(id));
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public BoatStatusRecord getBoatStatus(String logbookName, DataTypeIntString entryNo) {
        try {
            DataKey[] keys = dataAccess.getByFields(
                    new String[]{BoatStatusRecord.LOGBOOK, BoatStatusRecord.ENTRYNO},
                    new Object[]{logbookName, entryNo});
            if (keys != null && keys.length > 0) {
                return (BoatStatusRecord) dataAccess.get(keys[0]);
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public Vector<BoatStatusRecord> getBoats(String status) {
        return getBoats(status, false);
    }

    /**
     * @param getBoatsForLists - if true, this will return boats not necessarily according
     * to their status, but rather which *list* they should appear in. It might be that
     * some boats which have status ONTHEWATER are supposed to be displayed as NOTAVAILABLE
     * and therefore returned for status=NOTAVAILABLE instead.
     */
    public Vector<BoatStatusRecord> getBoats(String status, boolean getBoatsForLists) {
        try {
            int boathouseId = getProject().getMyBoathouseId();
            if (Logger.isTraceOn(Logger.TT_GUI, 8)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_BOATLISTS, 
                    "BoatStatus.getBoats(" + status + "," + getBoatsForLists + ") for boathouse=" + boathouseId);
            }
            Vector<BoatStatusRecord> v = new Vector<BoatStatusRecord>();
            DataKeyIterator it = data().getStaticIterator();
            DataKey k = it.getFirst();
            String currentLogBookEfaBoatHouse = Daten.project.getCurrentLogbookEfaBoathouse();
            Boolean showForeignLogbookEntries = Daten.efaConfig.getValueEfaDirekt_boatListShowForeignLogbookSessionsAsNotAvailable();
            Boolean showMultidaySessionsAsNotavailable = Daten.efaConfig.getValueEfaDirekt_wafaRegattaBooteAufFahrtNichtVerfuegbar();
            Boolean statusIsOnTheWater = status.equalsIgnoreCase(BoatStatusRecord.STATUS_ONTHEWATER);
            Boolean statusIsNotAvailableBoats = status.equalsIgnoreCase(BoatStatusRecord.STATUS_NOTAVAILABLE);
            
            // take care for null values. null should not happen here, but anyway
            if (currentLogBookEfaBoatHouse == null)
                currentLogBookEfaBoatHouse = "";
            while (k != null) {
                BoatStatusRecord bStatRec = (BoatStatusRecord) data().get(k);

                if (bStatRec != null && !bStatRec.getDeletedOrInvisible()) {
                    if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_BOATLISTS,
                                "  Boat: " + bStatRec.getQualifiedName() + 
                                " (boathouse " + bStatRec.getOnlyInBoathouseIdAsInt() + ": " +
                                bStatRec.getOnlyInBoathouseId() + ")");
                    }
                    //  for boats on the water show only those which have sessions in the current logbook
                    if (statusIsOnTheWater) {
                        String rLogbook = bStatRec.getLogbook();
                        if ((rLogbook != null) && rLogbook.equalsIgnoreCase(currentLogBookEfaBoatHouse)) {
                        	// now handle "onthewater" multi-day sessions, regattas, training camps and check if they have to appear on notavailable list
                        	if (!(showMultidaySessionsAsNotavailable && bStatRec.isOnTheWaterShowNotAvailable())) {
                        		v.add(bStatRec);
                        	}
                        } 
                    } else {
                        // for all other show only the boats which are in this boathouse, if they are restricted.
                        if (bStatRec.getOnlyInBoathouseIdAsInt() < 0
                                || bStatRec.getOnlyInBoathouseIdAsInt() == boathouseId) {
                            String s = (getBoatsForLists ? bStatRec.getShowInList() : bStatRec.getCurrentStatus());
                            if (s != null && s.equals(status)) {
                            	
                                v.add(bStatRec);
                            } else {
                            	if (statusIsNotAvailableBoats && showForeignLogbookEntries && bStatRec.getCurrentStatus().equalsIgnoreCase(BoatStatusRecord.STATUS_ONTHEWATER)){

                            		String rLogbook = bStatRec.getLogbook(); // r.getLogbook needs a lot of time, so determine this value late 
                                    if ((rLogbook != null) && !rLogbook.equalsIgnoreCase(currentLogBookEfaBoatHouse)) {
                                    	//boats which are on the water, but not in the current logbook, 
                                    	//shall be shown as "not available". 
                                    	v.add(bStatRec);
                                    }
                            	}
                            }
                        }
                    }
                }
                k = it.getNext();
            }
            return v;
        } catch (Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public boolean areBoatsOutOnTheWater() {
        Vector v = getBoats(BoatStatusRecord.STATUS_ONTHEWATER, true);
        return (v != null && v.size() > 0);
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, BoatStatusRecord.BOATID);

            BoatStatusRecord r = ((BoatStatusRecord)record);
            // if the current state of the Boat is not ON_THE_WATER, we should clear
            // logbook and EntryNo, as they should point to an logbookentry only when the
            // boat is on the water.
            String curState = r.getCurrentStatus();
            if (curState != null && ! curState.equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
            	if (r.getLogbook()!=null) {
            		r.setLogbook(null);
            	}
            	if (r.getEntryNo()!=null) {
            		r.setEntryNo(null);
            	}
            }
        }
    }

}
