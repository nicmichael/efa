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

import de.nmichael.efa.util.*;
import java.util.*;

// @i18n complete

public class DataKeyIterator {

    private IDataAccess dataAccess;
    private DataKey[] keys;
    private boolean dynamic;
    private long scn;
    private int i = 0;
    private Hashtable<DataKey,Integer> keyHash;

    public DataKeyIterator(IDataAccess dataAccess, DataKey[] keys, boolean dynamic) {
        this.dataAccess = dataAccess;
        this.keys = keys;
        this.dynamic = dynamic;
        try {
            scn = dataAccess.getSCN();
        } catch(Exception e) {
            Logger.logdebug(e);
        }
    }

    private void updateKeys() {
        if (!dynamic) {
            return;
        }
        try {
            if (dataAccess.getSCN() != scn) {
                keys = dataAccess.getAllKeys();
                scn = dataAccess.getSCN();
                keyHash = null;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
    }

    public synchronized DataKey getCurrent() {
        updateKeys();
        if (i < -1) {
            i = -1;
        }
        if (keys != null && i > keys.length) {
            i = keys.length;
        }
        if (keys != null && i >= 0 && i < keys.length) {
            return keys[i];
        }
        return null;
    }

    public synchronized DataKey getFirst() {
        i = 0;
        return getCurrent();
    }

    public synchronized DataKey getLast() {
        updateKeys();
        i = (keys != null ? keys.length - 1 : 0);
        return getCurrent();
    }

    public synchronized DataKey getNext() {
        i++;
        return getCurrent();
    }

    public synchronized DataKey getPrev() {
        i--;
        return getCurrent();
    }

    public synchronized DataKey goTo(int idx) {
        updateKeys();
        if (keys != null && idx >= 0 && idx < keys.length) {
            i = idx;
        }
        return getCurrent();
    }

    public synchronized DataKey goTo(DataKey key) {
        updateKeys();
        if (keys == null || key == null) {
            return null;
        }
        if (keyHash == null) {
            keyHash = new Hashtable<DataKey,Integer>();
            for (int i=0; i<keys.length; i++) {
                keyHash.put(keys[i], i);
            }
        }
        try {
            return goTo(keyHash.get(key).intValue());
        } catch(Exception e) {
            return null;
        }
    }

    public synchronized int getPosition() {
        return i;
    }

    public synchronized int size() {
        return (keys != null ? keys.length : 0);
    }

}
