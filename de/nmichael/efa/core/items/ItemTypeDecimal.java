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

import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDecimal;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.util.*;
import javax.swing.JTextField;

// @i18n complete

public class ItemTypeDecimal extends ItemTypeLabelTextfield {

    private DataTypeDecimal value;
    private int decimalPlaces;
    private boolean onlyPositiveOrNull;

    public ItemTypeDecimal(String name, DataTypeDecimal value, int decimalPlaces, boolean onlyPositiveOrNull,
            int type, String category, String description) {
        this.name = name;
        this.value = (value != null ? value : new DataTypeDecimal());
        this.decimalPlaces = decimalPlaces;
        this.onlyPositiveOrNull = onlyPositiveOrNull;
        this.type = type;
        this.category = category;
        this.description = description;
    }

    public IItemType copyOf() {
        ItemTypeDecimal copy = new ItemTypeDecimal(name, new DataTypeDecimal(value), decimalPlaces, onlyPositiveOrNull, type, category, description);
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
                this.value = DataTypeDecimal.parseDecimal(value, true);
                if (onlyPositiveOrNull && this.value.getValue(decimalPlaces) < 0) {
                    this.value.setDecimal(0, decimalPlaces);
                }
            }
        } catch (Exception e) {
            if (dlg == null) {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
                           "Invalid value for parameter "+name+": "+value);
            }
        }
    }

    public void showValue() {
        super.showValue();
        if (field != null) {
            ((JTextField)field).setText(value.getAsFormattedString(decimalPlaces, decimalPlaces));
        }
    }
    public String toString() {
        if (!isNotNullSet() && !value.isSet()) {
            return "";
        }
        return value.toString();
    }

    public long getValue() {
        return value.getValue(decimalPlaces);
    }

    public void setValue(long value) {
        this.value.setDecimal(value, decimalPlaces);
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

}
