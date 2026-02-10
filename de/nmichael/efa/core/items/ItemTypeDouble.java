/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Velten Heyn
 * @version 2
 */

package de.nmichael.efa.core.items;

import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.util.Logger;

// @i18n complete

public class ItemTypeDouble extends ItemTypeLabelTextfield {

    public static double UNSET = IDataAccess.UNDEFINED_DOUBLE;
    public static double MAX = Double.MAX_VALUE;
    public static double MIN = -Double.MAX_VALUE;
    

    private double value;
    private double min;
    private double max;

    public ItemTypeDouble(String name, double value, double min, double max, 
            int type, String category, String description) {
        this.name = name;
        this.value = value;
        this.min = min;
        this.max = max;
        this.type = type;
        this.category = category;
        this.description = description;
    }

    public ItemTypeDouble(String name, double value, double min, double max, boolean allowUnset,
            int type, String category, String description) {
        this.name = name;
        this.value = value;
        this.min = min;
        this.max = max;
        this.type = type;
        this.category = category;
        this.description = description;
        this.setNotNull(!allowUnset);
    }

    public IItemType copyOf() {
    	ItemTypeDouble copy = new ItemTypeDouble(name, value, min, max, !isNotNullSet(), type, category, description);
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
                // for german input format
                value = value.replaceFirst("^(-?\\d+(?:\\.\\d{3})*)?,(\\d*)$", "$1.$2");
                this.value = Double.parseDouble(value);
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
        if (value == UNSET) {
            return "";
        }
        return Double.toString(value);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
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
