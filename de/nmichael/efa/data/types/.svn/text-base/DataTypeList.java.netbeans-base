/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.types;

import java.util.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.storage.*;

public class DataTypeList<T> {

    public static final String LISTITEM_STRING_SEPARATOR = ";";
    private static final String L0VALUE = "~";
    private ArrayList<T> list = null;

    // Default Constructor
    public DataTypeList() {
        unset();
    }

    // Regular Constructor
    public DataTypeList(ArrayList<T> list) {
        this.list = list;
    }

    public DataTypeList(T[] array) {
        this.list = new ArrayList<T>();
        for (int i=0; array != null && i<array.length; i++) {
            this.list.add(array[i]);
        }
    }

    // Copy Constructor
    public DataTypeList(DataTypeList list) {
        this.list = new ArrayList<T>(list.list.size());
        for (Object t : list.list) {
            this.list.add((T)t);
        }
    }

    public static DataTypeList parseList(String s, int dataType) {
        StringTokenizer tok = new StringTokenizer(s,LISTITEM_STRING_SEPARATOR);
        ArrayList list = null;
        switch(dataType) {
            case IDataAccess.DATA_STRING:
            case IDataAccess.DATA_LIST_STRING: // we accept both the value's data type as well as the list's data type
                list = new ArrayList<String>();
                break;
            case IDataAccess.DATA_INTEGER:
            case IDataAccess.DATA_LIST_INTEGER: // we accept both the value's data type as well as the list's data type
                list = new ArrayList<Integer>();
                break;
            case IDataAccess.DATA_LONGINT:
                list = new ArrayList<Long>();
                break;
            case IDataAccess.DATA_DECIMAL:
                list = new ArrayList<DataTypeDecimal>();
                break;
            case IDataAccess.DATA_DISTANCE:
                list = new ArrayList<DataTypeDistance>();
                break;
            case IDataAccess.DATA_BOOLEAN:
                list = new ArrayList<Boolean>();
                break;
            case IDataAccess.DATA_DATE:
                list = new ArrayList<DataTypeDate>();
                break;
            case IDataAccess.DATA_TIME:
                list = new ArrayList<DataTypeTime>();
                break;
            case IDataAccess.DATA_UUID:
            case IDataAccess.DATA_LIST_UUID: // we accept both the value's data type as well as the list's data type
                list = new ArrayList<UUID>();
                break;
            case IDataAccess.DATA_INTSTRING:
                list = new ArrayList<DataTypeIntString>();
                break;
            case IDataAccess.DATA_PASSWORDH:
                list = new ArrayList<DataTypePasswordHashed>();
            case IDataAccess.DATA_PASSWORDC:
                list = new ArrayList<DataTypePasswordCrypted>();
        }
        while(tok.hasMoreTokens()) {
            String t = tok.nextToken();
            if (t.equals(L0VALUE)) {
                t = "";
            }
            Object o = DataRecord.transformDataStringToType(t, dataType);
            list.add(o);
        }
        return new DataTypeList(list);
    }

    private String getStringValue(T t) {
        String item = t.toString();
        item = EfaUtil.removeSepFromString(item, ";<>"+L0VALUE).trim();
        if (item.length() == 0) {
            item = L0VALUE;
        }
        return item;
    }

    public String toString() {
        if (isSet()) {
            StringBuilder s = new StringBuilder();
            for (T t : list) {
                String item = getStringValue(t);
                s.append((s.length() == 0 ? item : LISTITEM_STRING_SEPARATOR + item));
            }
            return s.toString();
        }
        return "";
    }

    public boolean isSet() {
        return list != null;
    }

    public void unset() {
        list = null;
    }

    public void add(T item) {
        if (list == null) {
            list = new ArrayList<T>();
        }
        synchronized(list) {
            list.add(item);
        }
    }

    public void addAll(DataTypeList<T> addlist) {
        if (list == null) {
            list = new ArrayList<T>();
        }
        synchronized(list) {
            for (int i=0; i<addlist.length(); i++) {
                T t = addlist.get(i);
                if (!list.contains(t)) {
                    list.add(t);
                }
            }
        }
    }

    public int length() {
        if (list == null) {
            return 0;
        }
        synchronized(list) {
            return list.size();
        }
    }

    public T get(int idx) {
        if (list == null) {
            return null;
        }
        synchronized (list) {
            if (idx < 0 || idx >= list.size()) {
                return null;
            }
            return list.get(idx);
        }
    }

    public void set(int idx, T value) {
        if (list == null) {
            return;
        }
        synchronized (list) {
            if (idx < 0 || idx >= list.size()) {
                return;
            }
            list.set(idx, value);
        }
    }

    public T remove(int idx) {
        if (list == null) {
            return null;
        }
        synchronized (list) {
            if (idx < 0 || idx >= list.size()) {
                return null;
            }
            return list.remove(idx);
        }
    }

    public boolean remove(T value) {
        if (list == null) {
            return false;
        }
        synchronized (list) {
            return list.remove(value);
        }
    }

    public boolean contains(T value) {
        if (list == null) {
            return false;
        }
        synchronized (list) {
            return list.contains(value);
        }
    }

}
