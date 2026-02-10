/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.util.*;

// @i18n complete

public class ItemTypeLong extends ItemTypeLabelTextfield {

    public static long UNSET = IDataAccess.UNDEFINED_LONG;

    private long value;
    private long min;
    private long max;

    public ItemTypeLong(String name, long value, long min, long max,
            int type, String category, String description) {
        this.name = name;
        this.value = value;
        this.min = min;
        this.max = max;
        this.type = type;
        this.category = category;
        this.description = description;
    }

    public IItemType copyOf() {
        ItemTypeLong copy = new ItemTypeLong(name, value, min, max, type, category, description);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setIcon((label == null ? null : label.getIcon()));
        return copy;
    }

    public void parseValue(String value) {
        if (value != null) {
            value = value.trim();
        }
        try {
            if (value.length() == 0 && !isNotNullSet()) {
                this.value = UNSET;
            } else {
                this.value = Long.parseLong(value);
                if (this.value < min) {
                    this.value = min;
                }
                if (this.value > max) {
                    this.value = max;
                }
            }
        } catch (Exception e) {
            if (dlg == null) {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
                           "Invalid value for parameter "+name+": "+value);
            }
        }
    }

    public String toString() {
        if (!isNotNullSet() && value == UNSET) {
            return "";
        }
        return Long.toString(value);
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public boolean isSet() {
        return (isNotNullSet()) || value != UNSET;
    }

    public boolean isValidInput() {
        if (isNotNullSet()) {
            return isSet();
        }
        return true;
    }

}
