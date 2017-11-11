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
import de.nmichael.efa.ex.*;
import java.util.*;

// @i18n complete

public class Statistics extends StorageObject {

    public static final String DATATYPE = "efa2statistics";

    public Statistics(int storageType,
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Statistiken"));
        StatisticsRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new StatisticsRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public StatisticsRecord createStatisticsRecord(UUID id) {
        StatisticsRecord r = new StatisticsRecord(this, MetaData.getMetaData(DATATYPE));
        r.setId(id);
        r.setDefaults();
        r.setPosition(getHighestPosition() + 1);
        return r;
    }

    public int getHighestPosition() {
        try {
            int max = 0;
            DataKeyIterator it = dataAccess.getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                StatisticsRecord r = (StatisticsRecord)dataAccess.get(k);
                max = Math.max(max, r.getPosition());
                k = it.getNext();
            }
            return max;
        } catch(Exception e) {
            Logger.logdebug(e);
            return 0;
        }
    }

    public void moveRecord(StatisticsRecord r, int direction) {
        boolean origRecordDeleted = false;
        long lock = -1;
        try {
            int oldPos = r.getPosition();
            int newPos = oldPos + direction;
            if (oldPos == newPos || newPos < 1 || newPos > getHighestPosition()) {
                return;
            }
            lock = dataAccess.acquireGlobalLock();
            dataAccess.delete(r.getKey(), lock);
            origRecordDeleted = true;
            DataKeyIterator it = dataAccess.getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                StatisticsRecord r2 = (StatisticsRecord)dataAccess.get(k);
                if (r2.getPosition() == newPos) {
                    r2.setPosition(oldPos);
                    dataAccess.update(r2, lock);
                    break;
                }
                k = it.getNext();
            }
            r.setPosition(newPos);
            dataAccess.add(r, lock);
            origRecordDeleted = false;
        } catch(Exception e) {
            Logger.logdebug(e);
            Dialog.error(e.toString());
        } finally {
            if (lock >= 0) {
                dataAccess.releaseGlobalLock(lock);
            }
            if (origRecordDeleted) {
                try {
                    // we add this record again *without* holding a lock
                    // the exception that caused us to go into this code path
                    // may have been caused by the lock timing out, so can't be
                    // sure we're still holding it
                    dataAccess.add(r);
                } catch(Exception e2) {
                    Logger.logdebug(e2);
                }
            }
        }
    }

    public StatisticsRecord getStatistics(UUID id) {
        try {
            return (StatisticsRecord)data().get(StatisticsRecord.getKey(id));
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public StatisticsRecord findStatisticsByName(String name) {
        try {
            DataKey[] keys = data().getByFields(new String[] { StatisticsRecord.NAME }, new String[] { name });
            if (keys != null && keys.length > 0) {
                return (StatisticsRecord) data().get(keys[0]);
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, StatisticsRecord.ID);
            assertFieldNotEmpty(record, StatisticsRecord.NAME);
            assertFieldNotEmpty(record, StatisticsRecord.POSITION);
            assertUnique(record, StatisticsRecord.NAME);
            assertUnique(record, StatisticsRecord.POSITION);
            StatisticsRecord r = (StatisticsRecord)record;
            if (r.getPubliclyAvailable() && r.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett) {
                throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                                "Das Erstellen von Meldedateien in öffentliche Statistiken ist nicht erlaubt.",
                                Thread.currentThread().getStackTrace());
            }
        }
    }

}
