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

public class ItemTypeInteger extends ItemTypeLabelTextfield {

    public static int UNSET = IDataAccess.UNDEFINED_INT;

    private int value;
    private int min;
    private int max;

    public ItemTypeInteger(String name, int value, int min, int max, 
            int type, String category, String description) {
        this.name = name;
        this.value = value;
        this.min = min;
        this.max = max;
        this.type = type;
        this.category = category;
        this.description = description;
    }

    public ItemTypeInteger(String name, int value, int min, int max, boolean allowUnset,
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
        ItemTypeInteger copy = new ItemTypeInteger(name, value, min, max, !isNotNullSet(), type, category, description);
        copy.setFieldSize(fieldWidth, fieldHeight);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setIcon((label == null ? null : label.getIcon()));
        copy.setIsItemOnSameRowAsPreviousItem(itemOnSameRowAsPreviousItem);
        copy.setItemOnNewRow(itemOnNewRow);
        copy.setFieldGrid(fieldGridWidth,fieldGridHeight,fieldGridAnchor,fieldGridFill);
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
                this.value = Integer.parseInt(value);
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
        return Integer.toString(value);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
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
