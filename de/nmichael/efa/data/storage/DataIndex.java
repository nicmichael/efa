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

import java.util.*;

// @i18n complete

public class DataIndex {

    class IndexKey {
        private Object[] data;
        int hash;

        public IndexKey(Object[] data) {
            this.data = data;
            hash = 0;
            for (int i=0; i<data.length; i++) {
                hash += (data[i] != null ? data[i].hashCode() : 0);
            }
        }

        public boolean equals(Object o) {
            IndexKey other = (IndexKey)o;
            if (data.length != other.data.length) {
                return false;
            }
            for (int i=0; i<data.length; i++) {
                if (data[i] != null && other.data[i] != null && !data[i].equals(other.data[i])) {
                    return false;
                }
                if (data[i] == null && other.data[i] != null) {
                    return false;
                }
                if (data[i] != null && other.data[i] == null) {
                    return false;
                }
            }
            return true;
        }

        public int hashCode() {
            return hash;
        }

    }

    private int[] indexFields;
    private Hashtable<IndexKey,ArrayList<DataKey>> index = new Hashtable<IndexKey,ArrayList<DataKey>>();

    public DataIndex(int[] indexFields) {
        this.indexFields = Arrays.copyOf(indexFields, indexFields.length);
    }

    public void clear() {
        synchronized(index) {
            index.clear();
        }
    }

    public void add(DataRecord r) {
        Object[] values = new Object[indexFields.length];
        for (int i=0; i<values.length; i++) {
            values[i] = r.get(indexFields[i]);
        }
        DataKey key = r.getKey();
        IndexKey entry = new IndexKey(values);

        synchronized (index) {
            ArrayList<DataKey> list = index.get(entry);
            if (list == null) {
                list = new ArrayList<DataKey>();
            }

            if (!list.contains(key)) {
                list.add(key);
            }

            index.put(entry, list);
        }
    }

    public void delete(DataRecord r) {
        Object[] values = new Object[indexFields.length];
        for (int i=0; i<values.length; i++) {
            values[i] = r.get(indexFields[i]);
        }
        DataKey key = r.getKey();
        IndexKey entry = new IndexKey(values);

        synchronized (index) {
            ArrayList<DataKey> list = index.get(entry);
            if (list == null) {
                return;
            }

            list.remove(key);
            if (list.size() == 0) {
                index.remove(entry);
            }
        }
    }

    public DataKey[] search(Object[] values) {
        IndexKey entry = new IndexKey(values);
        synchronized (index) {
            ArrayList<DataKey> list = index.get(entry);
            if (list == null || list.size() == 0) {
                return null;
            }

           DataKey[] keys = new DataKey[list.size()];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = list.get(i);
            }
            return keys;
        }
    }

    public int[] getIndexFields() {
        return indexFields;
    }

}
