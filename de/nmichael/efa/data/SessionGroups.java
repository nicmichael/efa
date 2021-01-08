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
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.ex.EfaModifyException;
import java.util.*;

// @i18n complete

public class SessionGroups extends StorageObject {

    public static final String DATATYPE = "efa2sessiongroups";

    public SessionGroups(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Fahrtgruppen"));
        SessionGroupRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new SessionGroupRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public SessionGroupRecord createSessionGroupRecord(UUID id, String logbook) {
        SessionGroupRecord r = new SessionGroupRecord(this, MetaData.getMetaData(DATATYPE));
        r.setId(id);
        r.setLogbook(logbook);
        return r;
    }

    public SessionGroupRecord findSessionGroupRecord(UUID id) {
        try {
            return (SessionGroupRecord)data().get(SessionGroupRecord.getKey(id));
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public SessionGroupRecord findSessionGroupRecord(String name, String logbookName) {
        try {
            DataKey[] keys = data().getByFields(
                    new String[] { SessionGroupRecord.NAME, SessionGroupRecord.LOGBOOK } ,
                    new String[] { name, logbookName });
            if (keys != null && keys.length > 0) {
                return (SessionGroupRecord)data().get(keys[0]);
            }
            return null;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public DataKey[] findAllSessionGroupKeys(String logbookName) {
        try {
            return data().getByFields(SessionGroupRecord.IDX_LOGBOOK, new String[] { logbookName });
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public String getSessionGroupName(UUID id) {
        SessionGroupRecord r = findSessionGroupRecord(id);
        if (r != null) {
            return r.getName();
        }
        return null;
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, SessionGroupRecord.ID);
            assertFieldNotEmpty(record, SessionGroupRecord.NAME);
            assertUnique(record, new String[] { SessionGroupRecord.NAME, SessionGroupRecord.LOGBOOK });
            assertFieldNotEmpty(record, SessionGroupRecord.LOGBOOK);
            assertFieldNotEmpty(record, SessionGroupRecord.STARTDATE);
            assertFieldNotEmpty(record, SessionGroupRecord.ENDDATE);

            // check whether ActiveDays is not larger than EndDate-StartDate
            DataTypeDate startDate = ((SessionGroupRecord)record).getStartDate();
            DataTypeDate endDate   = ((SessionGroupRecord)record).getEndDate();
            int activeDays = ((SessionGroupRecord)record).getActiveDays();
            if (startDate.isSet() && endDate.isSet() && activeDays != IDataAccess.UNDEFINED_INT) {
                long days = startDate.getDifferenceDays(endDate) + 1;
                if (activeDays < 1 || activeDays > days) {
                    throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                              International.getMessage("Das Feld '{field}' hat einen ungültigen Wert.", SessionGroupRecord.ACTIVEDAYS),
                              Thread.currentThread().getStackTrace());
                }
            }

            // check whether all referencing logbook records fit into this range
            Vector<LogbookRecord> logbookRecords = ((SessionGroupRecord)record).getAllReferencingLogbookRecords();
            for (int i=0; logbookRecords != null && i<logbookRecords.size(); i++) {
                LogbookRecord r = logbookRecords.get(i);
                if (!((SessionGroupRecord)record).checkLogbookRecordFitsIntoRange(r)) {
                    throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                            International.getMessage("Das Datum des Fahrtenbucheintrags {entry} liegt außerhalb des Zeitraums, "
                            + "der für die ausgewählte Fahrtgruppe '{name}' angegeben wurde.",
                            r.getEntryId().toString(), ((SessionGroupRecord)record).getName()),
                            Thread.currentThread().getStackTrace());
                }
                
            }
        }
        if (delete) {
            assertNotReferenced(record, getProject().getLogbook(((SessionGroupRecord)record).getLogbook(), false),
                    new String[] { LogbookRecord.SESSIONGROUPID });
        }
    }

}
