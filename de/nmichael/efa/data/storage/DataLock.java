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

// @i18n complete

public class DataLock {

    public final static DataKey GLOBAL_EXCLUSIVE_LOCK = new DataKey<String,String,String>("%%%GLOBAL_LOCK%%%", null, null);

    private long lockID;
    private DataKey lockObject;
    private Thread lockOwner;
    private long lockTime;

    public DataLock(long lockID, DataKey lockObject) {
        this.lockID = lockID;
        this.lockObject = lockObject;
        this.lockOwner = Thread.currentThread();
        this.lockTime = System.currentTimeMillis();
    }

    public long getLockID() {
        return lockID;
    }

    public DataKey getLockObject() {
        return lockObject;
    }

    public long getLockTime() {
        return lockTime;
    }

    public Thread getLockOwner() {
        return lockOwner;
    }

    public String toString() {
        String threadInfo;
        try {
            threadInfo = lockOwner.getName() + "[" + lockOwner.getId() + "]";
        } catch(Exception e) {
            threadInfo = "<unknown>";
        }
        return ("Lock[" + lockID + "] Type=" + (lockObject.equals(GLOBAL_EXCLUSIVE_LOCK) ? "global" : "local Object=" + lockObject) +
               " acquired at " + lockTime + " owned by " + threadInfo);
    }

}
