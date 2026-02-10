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

import de.nmichael.efa.util.*;

public class DataTypeIntString implements Cloneable, Comparable<DataTypeIntString> {

    private String value;
    private String appendedString;

    // Default Constructor
    public DataTypeIntString() {
    }

    // Regular Constructor
    public DataTypeIntString(String value) {
        this.value = value;
    }

    // Copy Constructor
    public DataTypeIntString(DataTypeIntString string) {
        this.value = string.value;
    }

    public static DataTypeIntString parseString(String s) {
        return new DataTypeIntString(s);
    }

    public String toString() {
        if (value != null) {
            return value;
        }
        return "";
    }

    public int length() {
        if (value != null) {
            return value.length();
        }
        return 0;
    }

    public boolean equals(Object o) {
        try {
            return compareTo((DataTypeIntString)o) == 0;
        } catch(Exception e) {
            return false;
        }
    }

    public boolean isSet() {
        return value != null;
    }

    public int intValue() {
        return EfaUtil.stringFindInt(value, Integer.MIN_VALUE);
    }

    public int compareTo(DataTypeIntString o) {
        if (value == null) {
            if (o.value == null) {
                return 0;
            }
            return -1;
        }
        if (o.value == null) {
            return 1;
        }
        int i1 = intValue();
        int i2 = o.intValue();
        if (i1 < i2) {
            return -1;
        }
        if (i1 > i2) {
            return 1;
        }
        return value.compareTo(o.value);
    }

    public int hashCode() {
        return (value != null ? value.hashCode() : -1);
    }
    
    public String toAppendedString() {
        if (value != null) {
            return value + (appendedString != null ? appendedString : "");
        }
        return "";
    }

    public void append(String s) {
        appendedString = (appendedString != null ? appendedString : "") + s;
    }

}
