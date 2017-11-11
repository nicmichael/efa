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
import de.nmichael.efa.ex.EfaModifyException;
import java.util.*;

// @i18n complete

public class Crews extends StorageObject {

    public static final String DATATYPE = "efa2crews";
    public CrewRecord staticCrewRecord;

    public Crews(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Mannschaften"));
        CrewRecord.initialize();
        staticCrewRecord = (CrewRecord)createNewRecord();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new CrewRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public CrewRecord createCrewRecord(UUID id) {
        CrewRecord r = new CrewRecord(this, MetaData.getMetaData(DATATYPE));
        r.setId(id);
        return r;
    }

    public CrewRecord getCrew(UUID id) {
        try {
            return (CrewRecord)data().get(CrewRecord.getDataKey(id));
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public CrewRecord findCrewRecord(String crewName) {
        try {
            DataKey[] keys = data().getByFields(
                staticCrewRecord.getQualifiedNameFields(), staticCrewRecord.getQualifiedNameValues(crewName));
            if (keys == null || keys.length < 1) {
                return null;
            }
            for (int i=0; i<keys.length; i++) {
                CrewRecord r = (CrewRecord)data().get(keys[i]);
                if (!r.getDeleted()) {
                    return r;
                }
            }
            return null;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, CrewRecord.ID);
            assertFieldNotEmpty(record, CrewRecord.NAME);
            assertUnique(record, CrewRecord.NAME);
        }
        if (delete) {
            assertNotReferenced(record, getProject().getBoats(false), new String[] { BoatRecord.DEFAULTCREWID } );
        }
    }

}
