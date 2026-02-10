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

import de.nmichael.efa.util.*;

// @i18n complete

public class ItemTypeLongLat extends ItemTypeLabelTextfield {

    private static final int TYPE_LATITUDE  = 0;
    private static final int TYPE_LONGITUDE = 1;

    public static final int ORIENTATION_NORTH = 0; // TYPE_LATITUDE  (even number)
    public static final int ORIENTATION_WEST  = 1; // TYPE_LONGITUDE (odd number)
    public static final int ORIENTATION_SOUTH = 2; // TYPE_LATITUDE  (even number)
    public static final int ORIENTATION_EAST  = 3; // TYPE_LONGITUDE (odd number)

    public static final String[] ORIENTATION = { "N", "W", "S", "E" };

    private static final String DELIM = ",";

    private int typeLongLat = -1;
    private int orientation = -1;
    private int[] coordinates = new int[3];
    
    public ItemTypeLongLat(String name, int orientation, int c1, int c2, int c3,
            int type, String category, String description) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.description = description;
        try {
            iniValue(orientation, c1, c2, c3);
        } catch(Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
                    "Invalid values for parameter " + name + "!");
        }
    }

    public IItemType copyOf() {        
        ItemTypeLongLat copy = new ItemTypeLongLat(name, orientation, coordinates[0], coordinates[1], coordinates[2], type, category, description);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setIcon((label == null ? null : label.getIcon()));
        return copy;
    }

    private void iniValue(int orientation, int c1, int c2, int c3) throws Exception {
        switch(orientation) {
            case ORIENTATION_NORTH:
            case ORIENTATION_SOUTH:
                typeLongLat = TYPE_LATITUDE;
                break;
            case ORIENTATION_WEST:
            case ORIENTATION_EAST:
                typeLongLat = TYPE_LONGITUDE;
                break;
        }
        this.orientation = orientation;
        if (typeLongLat == -1) {
            throw new Exception("Invalid value!");
        }
        coordinates[0] = c1;
        coordinates[1] = c2;
        coordinates[2] = c3;
    }

    public void parseValue(String value) {
        try {
            value = value.trim().toUpperCase();
            TMJ tmj = EfaUtil.string2date(value, 0, 0, 0);
            int orientation = -1;
            for (int i=0; i<ORIENTATION.length; i++) {
                if (value.endsWith(ORIENTATION[i])) {
                    orientation = i;
                }
            }
            if (orientation % 2 != this.orientation % 2) {
                throw new Exception("Invalid value! typeLongLat changed!");
            }
            iniValue(orientation, tmj.tag, tmj.monat, tmj.jahr);
        } catch (Exception e) {
            if (dlg == null) {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
                        "Invalid value for parameter " + name + ": " + value);
            } 
        }
    }

    public String toString() {
        return coordinates[0] + "° " + coordinates[1] + "' " + coordinates[2] + "\" " + ORIENTATION[orientation];
    }

    public int getValueOrientation() {
        return orientation;
    }

    public int[] getValueCoordinates() {
        int[] c = new int[coordinates.length];
        for (int i=0; i<c.length; i++) {
            c[i] = coordinates[i];
        }
        return c;
    }

    public void setValueOrientation(int orientation) {
        if (orientation % 2 == this.orientation % 2) {
            this.orientation = orientation;
        }
    }

    public void setValueCoordinates(int[] coordinates) {
        for (int i=0; i<this.coordinates.length; i++) {
            if (i < coordinates.length) {
                this.coordinates[i] = coordinates[i];
            }
        }
    }

    public boolean isSet() {
        return coordinates != null && coordinates[0] >= 0 && coordinates[1] >= 0 && coordinates[2] >= 0;
    }

    public boolean isValidInput() {
        if (isNotNullSet()) {
            return isSet();
        }
        return true;
    }

}
