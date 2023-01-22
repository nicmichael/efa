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

import de.nmichael.efa.util.EfaUtil;
import java.util.*;
import de.nmichael.efa.util.Logger;

// @i18n complete

public class DataLocks {

    /**
     * Lock Implementation
     *
     * There are two types of Locks:
     * - Global Exclusive Locks
     * - Local Exclusive Locks
     *
     * Global Exclusive Locks lock the entire Storage Object for exclusive (write) access.
     * When a thread holds a Global Exclusive Lock, no other Thread will be able to acquire either a Global or Local Exclusive Lock.
     * However, read access is possible at all times.
     *
     * Local Exclusive Locks lock a specific record identified by its key in the Storage Object for exclusive (write) access.
     * When a thread holds a Local Exclusive Lock, no other Thread will be able to acquire a Local Exclusive Lock for the same record.
     * Additionally, no other Tread will be able to aquire a Global Exclusive Lock.
     * However, read access is possible at all times, to both other records as also the locked record itself.
     *
     * Usecase                   Lock Required                          Notes
     * ======================================================================================
     * Add Record                Local Exclusive Lock on Record         will be granted if there is no Global Exclusive Lock,
     *                                                                  and if no other thread attempts to add a Record with
     *                                                                  identical key information at the same time.
     * Modify Record             Local Exclusive Lock on Record         will be granted if there is no Global Exclusive Lock,
     *                                                                  and if no other thread holds a Local Exclusive Lock
     *                                                                  on this record.
     * Delete Record             Local Exclusive Lock on Record         will be granted if there is no Global Exclusive Lock,
     *                                                                  and if no other thread holds a Local Exclusive Lock
     *                                                                  on this record.
     * Read Record               no Lock required                       reads will be possible at any time, but there is
     *                                                                  no level of read consistency supported.
     */

    public static final long LOCK_TIMEOUT_DEFAULT = 30000;        //  30,000 ms
    public static final long LOCK_TIMEOUT_LONG    = 120000;       // 120,000 ms
    public static       long LOCK_TIMEOUT = LOCK_TIMEOUT_DEFAULT; //  30,000 ms
    public static final long SLEEP_RETRY  =    10; //     10 ms

    private final Hashtable<DataKey,DataLock> locks = new Hashtable<DataKey,DataLock>();
    private volatile long lockID = 0;

    public static void setLockTimeout(long sec) {
        LOCK_TIMEOUT = Math.min(Math.max(sec*1000, LOCK_TIMEOUT_DEFAULT), LOCK_TIMEOUT_LONG);
        if (Logger.isTraceOn(Logger.TT_FILEIO)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_LOCKTIMEOUTSET,
                    "Data Lock Timeout set to " + LOCK_TIMEOUT);
        }
    }

    public int clearTimeouts() {
        int count = 0;
        long now = System.currentTimeMillis();
        synchronized (locks) {
            for (Iterator<DataKey> it = locks.keySet().iterator(); it.hasNext();) {
                DataLock lock = locks.get(it.next());
                if (now - lock.getLockTime() >= LOCK_TIMEOUT) {
                    Logger.log(Logger.WARNING, Logger.MSG_DATA_LOCKTIMEOUT,
                            "Lock Timeout at " + now +
                            " (" + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" +
                            "): " + lock.toString());
                    Logger.logStackTrace(Logger.WARNING, Logger.MSG_DATA_LOCKTIMEOUT, "Lock Timeout Thread",
                            Thread.currentThread().getStackTrace());
                    Logger.logStackTrace(Logger.WARNING, Logger.MSG_DATA_LOCKTIMEOUT, "Lock Owner Thread",
                            lock.getLockOwner().getStackTrace());
                    locks.remove(lock.getLockObject());
                    count++;
                }
            }
        }
        return count;
    }

    private DataLock newDataLock(DataKey object) {
        DataLock lock;
        synchronized (locks) {
            lock = new DataLock(++lockID, object);
            locks.put(object, lock);
        }
        return lock;
    }

    private DataLock tryAcquireLock(DataKey object) {
        boolean global = (object == null);
        try {
            long startTimestamp = System.currentTimeMillis();
            int tries = 0;
            do {
                if (tries++ > 0) {
                    try {
                        Thread.sleep(SLEEP_RETRY);
                    } catch (InterruptedException ie) {
                    }
                    clearTimeouts();
                }
                synchronized(locks) {
                    if (global) {
                        // try to acquire a global lock
                        if (locks.size() == 0) {
                            return newDataLock(DataLock.GLOBAL_EXCLUSIVE_LOCK);
                        }
                    } else {
                        // try to acquire a local lock
                        if (locks.get(object) == null &&
                            locks.get(DataLock.GLOBAL_EXCLUSIVE_LOCK) == null) {
                            return newDataLock(object);
                        }
                    }
                }
            } while (System.currentTimeMillis() >= startTimestamp
                    && System.currentTimeMillis() - startTimestamp < LOCK_TIMEOUT);
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        
        // Log error about current lock owner
        if (global) {
            Set<DataKey> keys = null;
            synchronized(locks) {
                keys = locks.keySet();
            }
            if (keys != null && keys.size() > 0) {
                long now = System.currentTimeMillis();
                for (DataKey key : keys) {
                    DataLock lock = null;
                    synchronized(locks) {
                        lock = locks.get(key);
                    }
                    if (lock != null) {
                        Logger.log(Logger.WARNING, Logger.MSG_DATA_GETLOCKFAILED,
                                "tryAcquireLock() failed to acquire a global lock (now=" + now + "): " +
                                "Lock " + lock.getLockID() + " held since " + lock.getLockTime() +
                                " by " + EfaUtil.getStackTrace(lock.getLockOwner()));

                    }
                }
            }
        }

        return null;
    }

    public long getGlobalLock() {
        DataLock lock = tryAcquireLock(null);
        if (lock != null) {
            return lock.getLockID();
        }
        return -1;
    }

    public long getLocalLock(DataKey object) {
        if (object == null || object.equals(DataLock.GLOBAL_EXCLUSIVE_LOCK)) {
            return -1;
        }
        DataLock lock = tryAcquireLock(object);
        if (lock != null) {
            return lock.getLockID();
        }
        return -1;
    }

    public boolean hasGlobalLock(long lockID) {
        synchronized(locks) {
            DataLock lock = locks.get(DataLock.GLOBAL_EXCLUSIVE_LOCK);
            return lock != null && lock.getLockID() == lockID;
        }
    }

    public boolean hasLocalLock(long lockID, DataKey object) {
        synchronized(locks) {
            DataLock lock = locks.get(object);
            return lock != null && lock.getLockID() == lockID;
        }
    }

    public boolean releaseGlobalLock(long lockID) {
        synchronized(locks) {
            DataLock lock = locks.get(DataLock.GLOBAL_EXCLUSIVE_LOCK);
            if (lock != null && lock.getLockID() == lockID) {
                locks.remove(DataLock.GLOBAL_EXCLUSIVE_LOCK);
                return true;
            }
        }
        return false;
    }

    public boolean releaseLocalLock(long lockID) {
        synchronized (locks) {
            for (Iterator<DataKey> it = locks.keySet().iterator(); it.hasNext();) {
                DataLock lock = locks.get(it.next());
                if (lock.getLockID() == lockID) {
                    locks.remove(lock.getLockObject());
                    return true;
                }
            }
        }
        return false;
    }

}
