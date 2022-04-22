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

import de.nmichael.efa.*;
import de.nmichael.efa.util.*;
import java.util.regex.*;

public class DataTypeDistance {

    public enum UnitType {
        km, m, mi, yd
    }
    
    public static final String KILOMETERS = "km";
    public static final String METERS     = "m";
    public static final String MILES      = "mi";
    public static final String YARDS      = "yd";

    private static Pattern pattern = Pattern.compile("([0-9,\\.]+) *([kmiyd]*)");
    
    private DataTypeDecimal value;
    private UnitType unit;

    // Default Constructor
    public DataTypeDistance() {
        unset();
    }

    // Regular Constructor
    public DataTypeDistance(DataTypeDecimal value, UnitType unit) {
        this.value = value;
        this.unit = unit;
    }

    public DataTypeDistance(long distanceInDefaultUnit) {
        if (getDefaultUnit() == UnitType.km) {
            this.value = new DataTypeDecimal(distanceInDefaultUnit, 0);
            this.unit = UnitType.m;
        } else {
            this.value = new DataTypeDecimal(distanceInDefaultUnit, 0);
            this.unit = UnitType.yd;
        }
    }

    // Copy Constructor
    public DataTypeDistance(DataTypeDistance distance) {
        this.value = (distance.value != null ? new DataTypeDecimal(distance.value) : null);
        this.unit = distance.unit;
    }

    public static String[] makeDistanceUnitValueArray() {
        String[] units = new String[2];
        units[0] = KILOMETERS;
        units[1] = MILES;
        return units;
    }

    public static String[] makeDistanceUnitNamesArray() {
        String[] units = new String[2];
        units[0] = International.getString("Kilometer");
        units[1] = International.getString("Meilen");
        return units;
    }

    public static DataTypeDistance parseDistance(String s, boolean fromGUI) {
        DataTypeDistance distance = new DataTypeDistance();
        if (s != null) {
            Matcher m = pattern.matcher(s.trim().toLowerCase());
            if (m.lookingAt()) {
                String dist = m.group(1);
                String unit = m.group(2);
                distance.value = DataTypeDecimal.parseDecimal(dist, fromGUI);
                if (unit.equals(KILOMETERS)) {
                    distance.unit = UnitType.km;
                    distance.value.setDecimal(distance.value.getValue(3), 3);
                } else if (unit.equals(MILES)) {
                    distance.unit = UnitType.mi;
                    distance.value.setDecimal(distance.value.getValue(3), 3);
                } else if (unit.equals(METERS)) {
                    distance.unit = UnitType.m;
                    distance.value.setDecimal(distance.value.getValue(0), 0);
                } else if (unit.equals(YARDS)) {
                    distance.unit = UnitType.yd;
                    distance.value.setDecimal(distance.value.getValue(0), 0);
                } else {
                    distance.unit = getDefaultUnit();
                    distance.value.setDecimal(distance.value.getValue(3), 3);
                }
            }
        }
        return distance;
    }

    public static DataTypeDistance getDistance(long distanceInDefaultUnit) {
        if (getDefaultUnit() == UnitType.km) {
            return parseDistance(Long.toString(distanceInDefaultUnit) + " " + METERS, false);
        } else {
            return parseDistance(Long.toString(distanceInDefaultUnit) + " " + YARDS, false);
        }
    }

    public static DataTypeDistance getDistanceFromMeters(long distanceInMeters) {
        DataTypeDistance dist = new DataTypeDistance();
        dist.value = new DataTypeDecimal(distanceInMeters, 0);
        dist.unit = UnitType.m;
        return dist;
    }

    public void setDistance(DataTypeDecimal value, UnitType unit) {
        this.value = (value != null ? new DataTypeDecimal(value) : null);
        this.unit = unit;
    }

    public String toString() {
        if (isSet()) {
            return value.toString() + " " + unit;
        }
        return "";
    }

    public String getAsFormattedString() {
        if (isSet()) {
            return value.getAsFormattedString() + " " + unit;
        }
        return "";
    }

    public boolean isSet() {
        return value != null && unit != null && value.isSet();
    }

    public void unset() {
        value = null;
        unit = null;
    }

    public DataTypeDecimal getValue() {
        return (value != null ? new DataTypeDecimal(value) : null);
    }

    public UnitType getUnit() {
        return unit;
    }

    public static UnitType getDefaultUnit() {
        if (Daten.efaConfig != null) {
            if (Daten.efaConfig.getValueDefaultDistanceUnit().equals(KILOMETERS)) {
                return UnitType.km;
            }
            if (Daten.efaConfig.getValueDefaultDistanceUnit().equals(MILES)) {
                return UnitType.mi;
            }
        }
        return UnitType.km;
    }

    public static String getDefaultUnitName() {
        if (Daten.efaConfig != null) {
            if (Daten.efaConfig.getValueDefaultDistanceUnit().equals(KILOMETERS)) {
                return International.getString("Kilometer");
            }
            if (Daten.efaConfig.getValueDefaultDistanceUnit().equals(MILES)) {
                return International.getString("Meilen");
            }
        }
        return International.getString("Kilometer");
    }

    public static String getDefaultUnitAbbrevation() {
        return getDefaultUnitAbbrevation(false);
    }

    public static String getDefaultUnitAbbrevation(boolean capitalized) {
        if (Daten.efaConfig != null) {
            if (Daten.efaConfig.getValueDefaultDistanceUnit().equals(KILOMETERS)) {
                return (capitalized ? International.getString("Km") : International.getString("km"));
            }
            if (Daten.efaConfig.getValueDefaultDistanceUnit().equals(MILES)) {
                return (capitalized ? International.getString("Mi") : International.getString("mi"));
            }
        }
        return (capitalized ? International.getString("Km") : International.getString("km"));
    }

    public static String getAllUnitAbbrevationsAsString(boolean withWordOther) {
        if (withWordOther) {
            return International.getMessage("{a} oder {b}",
                    "km, m, mi", "yd");
        } else {
            return "km, m, mi, yd";
        }
    }


    /**
     * 1 km = 1000m
     * 1 yd = 0.9144 m
     * 1 mi = 1.609344 km
     * 1 mi = 1760 yd
     */

    public long getValueInMeters() {
        if (!isSet()) {
            return 0;
        }
        switch(unit) {
            case km: return value.getValue(3);
            case m : return value.getValue(0);
            case mi: return (long)Math.round(((double)value.getValue(3)) * 1.609344);
            case yd: return (long)Math.round(((double)value.getValue(0)) * 0.9144);
        }
        return 0;
    }

    public long getValueInYards() {
        if (!isSet()) {
            return 0;
        }
        switch(unit) {
            case km: return (long)Math.round(((double)value.getValue(3)) / 0.9144);
            case m : return (long)Math.round(((double)value.getValue(0)) / 0.9144);
            case mi: return (value.getValue(3) * 1760)/1000;
            case yd: return value.getValue(0);
        }
        return 0;
    }

    public long getTruncatedValueInKilometers() {
        long m = getValueInMeters();
        return m / 1000;
    }

    public long getRoundedValueInKilometers() {
        long m = getValueInMeters();
        return (m / 1000) + (m % 1000 > 500 ? 1 : 0);
    }

    public void truncateToMeters(int meters) {
        if (!isSet()) {
            return;
        }
        this.value.setDecimal((getValueInMeters() / meters) * meters, 0);
        this.unit = UnitType.m;
    }

    public void truncateToMainDistanceUnit() {
        if (!isSet()) {
            return;
        }
        switch(unit) {
            case km:
            case m :
                this.value.setDecimal(getValueInMeters()/1000, 0);
                this.unit = UnitType.km;
                break;
            case mi:
            case yd:
                this.value.setDecimal(getValueInYards()/1760, 0);
                this.unit = UnitType.mi;
                break;
        }
    }

    public String getStringValueInKilometers() {
        return getStringValueInKilometers(false, false, 3, 3);
    }

    public String getStringValueInKilometers(boolean withUnit, boolean formatted) {
        return getStringValueInKilometers(withUnit, formatted, 3, 3);
    }

    public String getStringValueInKilometers(boolean withUnit, int minDecimalPlaces, int maxDecimalPlaces) {
        return getStringValueInKilometers(withUnit, true, minDecimalPlaces, maxDecimalPlaces);
    }

    private String getStringValueInKilometers(boolean withUnit, boolean formatted, int minDecimalPlaces, int maxDecimalPlaces) {
        DataTypeDecimal d = new DataTypeDecimal(getValueInMeters(), 3);
        return (formatted ? d.getAsFormattedString(minDecimalPlaces, maxDecimalPlaces) : d.toString()) + (withUnit ? " " + KILOMETERS : "");
    }

    public String getStringValueInMiles() {
        return getStringValueInMiles(false, false, 3, 3);
    }

    public String getStringValueInMiles(boolean withUnit, boolean formatted) {
        return getStringValueInMiles(withUnit, formatted, 3, 3);
    }

    public String getStringValueInMiles(boolean withUnit, int minDecimalPlaces, int maxDecimalPlaces) {
        return getStringValueInMiles(withUnit, true, minDecimalPlaces, maxDecimalPlaces);
    }

    private String getStringValueInMiles(boolean withUnit, boolean formatted, int minDecimalPlaces, int maxDecimalPlaces) {
        DataTypeDecimal d = new DataTypeDecimal((getValueInYards()*1000)/1760, 3);
        return (formatted ? d.getAsFormattedString(minDecimalPlaces, maxDecimalPlaces) : d.toString()) + (withUnit ? " " + MILES : "");
    }

    public String getStringValueInDefaultUnit() {
        return getStringValueInDefaultUnit(false, false, 3, 3);
    }

    public String getStringValueInDefaultUnit(boolean withUnit, boolean formatted) {
        return getStringValueInDefaultUnit(withUnit, formatted, 3, 3);
    }

    public String getStringValueInDefaultUnit(boolean withUnit, int minDecimalPlaces, int maxDecimalPlaces) {
        return getStringValueInDefaultUnit(withUnit, true, minDecimalPlaces, maxDecimalPlaces);
    }

    private String getStringValueInDefaultUnit(boolean withUnit, boolean formatted, int minDecimalPlaces, int maxDecimalPlaces) {
        if (getDefaultUnit() == UnitType.km) {
            return getStringValueInKilometers(withUnit, formatted, minDecimalPlaces, maxDecimalPlaces);
        } else {
            return getStringValueInMiles(withUnit, formatted, minDecimalPlaces, maxDecimalPlaces);
        }
    }

    public long getValueInDefaultUnit() {
        if (getDefaultUnit() == UnitType.km) {
            return getValueInMeters();
        } else {
            return getValueInYards();
        }
    }


    public static void main(String[] args) {
        for (int i=0; i<args.length; i++) {
            String s = args[i];
            System.out.println("Input: >>>"+s+"<<<");
            DataTypeDistance dist = DataTypeDistance.parseDistance(s, true);
            System.out.println("Distance : "+dist.toString());
            System.out.println("in meters: "+dist.getValueInMeters());
            System.out.println("in yards : "+dist.getValueInYards());
        }
    }

}
