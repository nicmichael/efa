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

import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.util.*;
import javax.swing.*;

// @i18n complete

public class ItemTypeDistance extends ItemTypeLabelTextfield {

    private DataTypeDistance value;

    public ItemTypeDistance(String name, DataTypeDistance value,
            int type, String category, String description) {
        this.name = name;
        this.value = (value == null ? new DataTypeDistance() : value);
        this.type = type;
        this.category = category;
        this.description = description;
    }

    public IItemType copyOf() {
    	ItemTypeDistance copy = new ItemTypeDistance(name, new  DataTypeDistance(value), type, category, description);
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
                this.value.unset();
            } else {
                this.value = DataTypeDistance.parseDistance(value, true);
            }
        } catch (Exception e) {
            if (dlg == null) {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
                           "Invalid value for parameter "+name+": "+value);
            }
        }
    }

    public String toString() {
        if (!isNotNullSet() && !value.isSet()) {
            return "";
        }
        return value.toString();
    }

    public DataTypeDistance getValue() {
        return new DataTypeDistance(value);
    }

    public void setValue(DataTypeDistance value) {
        this.value = new DataTypeDistance(value);
        showValue();
    }

    public boolean isSet() {
        return (isNotNullSet()) || value.isSet();
    }

    public boolean isValidInput() {
        if (isNotNullSet()) {
            return isSet();
        }
        return true;
    }

    // @Override
    public void showValue() {
        if (field != null) {
            String s = toString();
            char decsep = International.getDecimalSeparator();
            char othsep = (decsep == ',' ? '.' : ',');
            int idx = s.indexOf(othsep);
            if (idx >= 0) {
                s = s.substring(0, idx) + decsep + s.substring(idx + 1);
            }
            ((JTextField)field).setText(s);
        }
    }

}
