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

// @i18n complete

public class AutoIncrement extends StorageObject {

    public static final String DATATYPE = "efa2autoincrement";

    public AutoIncrement(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, "AutoIncrement");
        AutoIncrementRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new AutoIncrementRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public AutoIncrementRecord createAutoIncrementRecord(String sequence) {
        AutoIncrementRecord r = new AutoIncrementRecord(this, MetaData.getMetaData(DATATYPE));
        r.setSequence(sequence);
        return r;
    }

    public int nextAutoIncrementIntValue(String sequence) {
        long lock = -1;
        try {
            DataKey k = AutoIncrementRecord.getKey(sequence);
            lock = data().acquireLocalLock(k);
            AutoIncrementRecord r = (AutoIncrementRecord)data().get(k);
            if (r != null) {
                int seq = r.getIntValue() + 1;
                if (seq < 1) {
                    seq = 1;
                }
                r.setIntValue(seq);
                data().update(r, lock);
                return seq;
            } else {
                r = createAutoIncrementRecord(sequence);
                r.setIntValue(1);
                data().add(r, lock);
                return 1;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        } finally {
            if (lock != -1) {
                data().releaseLocalLock(lock);
            }
        }
        return -1;
    }

    public long nextAutoIncrementLongValue(String sequence) {
        long lock = -1;
        try {
            DataKey k = AutoIncrementRecord.getKey(sequence);
            lock = data().acquireLocalLock(k);
            AutoIncrementRecord r = (AutoIncrementRecord)data().get(k);
            if (r != null) {
                long seq = r.getLongValue() + 1;
                if (seq < 1) {
                    seq = 1;
                }
                r.setLongValue(seq);
                data().update(r, lock);
                return seq;
            } else {
                r = createAutoIncrementRecord(sequence);
                r.setLongValue(1);
                data().add(r, lock);
                return 1;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        } finally {
            if (lock != -1) {
                data().releaseLocalLock(lock);
            }
        }
        return -1;
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, AutoIncrementRecord.SEQUENCE);
        }
    }

}
