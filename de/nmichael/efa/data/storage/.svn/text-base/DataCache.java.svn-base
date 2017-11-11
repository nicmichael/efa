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

import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.Logger;
import java.util.*;

public class DataCache {
    
    public static long MAX_AGE;

    private IDataAccess dataAccess;
    private long lastScnUpdate = -1;
    private long scn = -1;
    private long totalNumberOfRecords = -1;
    private Hashtable<DataKey,DataRecord> cache = new Hashtable<DataKey,DataRecord>();

    public DataCache(IDataAccess dataAccess, long cacheExpiryTime) {
        this.dataAccess = dataAccess;
        this.MAX_AGE = cacheExpiryTime;
    }

    public synchronized void updateCache(DataRecord record, long scn, long totalNumberOfRecords) {
        if (scn != -1 && totalNumberOfRecords != -1) {
            updateScn(scn, totalNumberOfRecords);
        }
        cache.put(record.getKey(), record);
    }

    public synchronized void updateScn(long newScn, long newTotalNumberOfRecords){
        lastScnUpdate = System.currentTimeMillis();
        if (scn != newScn) {
            cache.clear();
        }
        scn = newScn;
        totalNumberOfRecords = newTotalNumberOfRecords;
    }

    private boolean isTooOld() {
        return (scn < 0 || System.currentTimeMillis() - lastScnUpdate > MAX_AGE);
    }
    
    private synchronized void fetchScnIfTooOld() {
        if (isTooOld()) {
            try {
                long newScn = dataAccess.getSCN();
                long numberOfRecords = dataAccess.getNumberOfRecords();
                if (newScn >= 0) {
                    updateScn(newScn, numberOfRecords);
                }
            } catch(Exception e) {
                Logger.logdebug(e);
            }
        }
    }

    public synchronized long getScnIfNotTooOld() {
        if (isTooOld()) {
            return -1;
        }
        return scn;
    }

    public synchronized long getTotalNumberOfRecordsIfNotTooOld() {
        if (isTooOld()) {
            return -1;
        }
        return totalNumberOfRecords;
    }

    public synchronized DataRecord get(DataKey key) {
        fetchScnIfTooOld();
        return cache.get(key);
    }

    private synchronized void getAllRecordsFromRemote() throws EfaException {
        // the following call will automatically also prefetch all records
        // and thus update the entire cache
        DataKey[] keys = dataAccess.getAllKeys();
    }

    public synchronized DataRecord getValidAt(DataKey key, long t) throws EfaException {
        DataRecord r = getValidAtFromCache(key, t);
        if (r == null) {
            if (isTooOld() || totalNumberOfRecords != cache.size()) {
                getAllRecordsFromRemote();
                r = getValidAtFromCache(key, t);
            }
        }
        return r;
    }

    private synchronized DataRecord getValidAtFromCache(DataKey key, long t) {
        int validFromField;
        if (dataAccess.getMetaData().versionized) {
            validFromField = dataAccess.getKeyFieldNames().length - 1; // VALID_FROM is always the last key field!
        } else {
            // wrong call: not versionized
            return null;
        }
        DataKey[] keys = cache.keySet().toArray(new DataKey[0]);
        if (keys == null) {
            return null;
        }
        for (DataKey k : keys) {
            boolean sameRecord = true;
            for (int i=0; i<validFromField; i++) {
                if (k.getKeyPart(i) == null || !k.getKeyPart(i).equals(key.getKeyPart(i))) {
                    sameRecord = false;
                }
            }
            if (!sameRecord) {
                continue;
            }
            long validFrom = (Long) k.getKeyPart(validFromField);
            if (t >= validFrom) {
                DataRecord rec = get(k);
                if (rec != null && t >= rec.getValidFrom() && t < rec.getInvalidFrom()) {
                    return rec;
                }
            }
        }
        return null;
    }

    public synchronized DataRecord[] getValidAny(DataKey key) throws EfaException {
        if (!isCacheComplete()) {
            getAllRecordsFromRemote();
        }
        return getValidAnyFromCache(key);
    }

    private synchronized DataRecord[] getValidAnyFromCache(DataKey key) {
        int validFromField;
        if (dataAccess.getMetaData().versionized) {
            validFromField = dataAccess.getKeyFieldNames().length - 1; // VALID_FROM is always the last key field!
        } else {
            // wrong call: not versionized
            return null;
        }
        DataKey[] keys = cache.keySet().toArray(new DataKey[0]);
        if (keys == null) {
            return null;
        }
        ArrayList<DataRecord> recordList = new ArrayList<DataRecord>();
        for (DataKey k : keys) {
            boolean sameRecord = true;
            for (int i=0; i<validFromField; i++) {
                if (k.getKeyPart(i) == null || !k.getKeyPart(i).equals(key.getKeyPart(i))) {
                    sameRecord = false;
                }
            }
            if (!sameRecord) {
                continue;
            }
            DataRecord r = get(k);
            if (r != null) {
                recordList.add(r);
            }
        }
        if (recordList.size() > 0) {
            return recordList.toArray(new DataRecord[0]);
        }
        return null;
    }

    public synchronized DataRecord getValidLatest(DataKey key) throws EfaException {
        if (!isCacheComplete()) {
            getAllRecordsFromRemote();
        }
        return getValidLatestFromCache(key);
    }

    private synchronized DataRecord getValidLatestFromCache(DataKey key) {
        int validFromField;
        if (dataAccess.getMetaData().versionized) {
            validFromField = dataAccess.getKeyFieldNames().length - 1; // VALID_FROM is always the last key field!
        } else {
            // wrong call: not versionized
            return null;
        }
        DataKey[] keys = cache.keySet().toArray(new DataKey[0]);
        if (keys == null) {
            return null;
        }
        DataKey latestVersionKey = null;
        DataRecord latestVersionRec = null;
        for (DataKey k : keys) {
            boolean sameRecord = true;
            for (int i=0; i<validFromField; i++) {
                if (k.getKeyPart(i) == null || !k.getKeyPart(i).equals(key.getKeyPart(i))) {
                    sameRecord = false;
                }
            }
            if (!sameRecord) {
                continue;
            }
            long validFrom = (Long) k.getKeyPart(validFromField);
            if (latestVersionKey == null || validFrom > (Long) latestVersionKey.getKeyPart(validFromField)) {
                DataRecord r = get(k);
                if (!r.getDeleted()) {
                    latestVersionKey = k;
                    latestVersionRec = r;
                }
            }
        }
        if (latestVersionRec != null) {
            return latestVersionRec;
        }
        return null;
    }

    public DataKey[] getAllKeys() {
        long numberOfRecRemote = getTotalNumberOfRecordsIfNotTooOld();
        if (numberOfRecRemote == cache.size()) {
            DataKey[] keys = cache.keySet().toArray(new DataKey[0]);
            if (keys != null) {
                // keys must be sorted, as they may be used for iterators for in-order traversal
                Arrays.sort(keys);
            }
            return keys;
        }
        return null;
    }

    public synchronized boolean isCacheComplete() {
        return !isTooOld() && (totalNumberOfRecords == cache.size());
    }

}
