/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * Gewichtsdaten-Typ — analog zu DataTypeDistance
 * Unterstützte Einheiten: Kilogramm (kg), Gramm (g), Pounds (lb)
 *
 * Umrechnungsfaktoren:
 * 1 kg = 1000 g
 * 1 lb = 453.59237 g
 * 
 * @author Stefan Gebers
 * @version 2
 */

package de.nmichael.efa.data.types;

import de.nmichael.efa.*;
import de.nmichael.efa.util.*;
import java.util.regex.*;

public class DataTypeWeight {

    public enum UnitType {
        kg, g, lb
    }

    public static final String KILOGRAMS  = "kg";
    public static final String GRAMS      = "g";
    public static final String POUNDS     = "lb";

    // Akzeptiert Zahlen (mit Komma oder Punkt) gefolgt von optionaler Einheit
    private static Pattern pattern = Pattern.compile("([0-9,\\.]+) *([kglb]*)");

    private DataTypeDecimal value;
    private UnitType unit;

    // Default Constructor
    public DataTypeWeight() {
        unset();
    }

    // Regular Constructor
    public DataTypeWeight(DataTypeDecimal value, UnitType unit) {
        this.value = value;
        this.unit = unit;
    }

    // Copy Constructor
    public DataTypeWeight(DataTypeWeight w) {
        this.value = (w.value != null ? new DataTypeDecimal(w.value) : null);
        this.unit = w.unit;
    }

    public static String[] makeWeightUnitValueArray() {
        String[] units = new String[2];
        units[0] = KILOGRAMS;
        units[1] = POUNDS;
        return units;
    }

    public static String[] makeWeightUnitNamesArray() {
        String[] units = new String[2];
        units[0] = International.getString("Kilogramm");
        units[1] = International.getString("Pfund");
        return units;
    }

    public static DataTypeWeight parseWeight(String s, boolean fromGUI) {
        DataTypeWeight w = new DataTypeWeight();
        if (s != null) {
            Matcher m = pattern.matcher(s.trim().toLowerCase());
            if (m.lookingAt()) {
                String val = m.group(1);
                String unitStr = m.group(2);
                w.value = DataTypeDecimal.parseDecimal(val, fromGUI);
                if (unitStr.equals(KILOGRAMS)) {
                    w.unit = UnitType.kg;
                    // kg: verwenden wir 3 Nachkommastellen wie bei km-Analogie
                    w.value.setDecimal(w.value.getValue(3), 3);
                } else if (unitStr.equals(POUNDS)) {
                    w.unit = UnitType.lb;
                    // lb: ebenfalls 3 Nachkommastellen
                    w.value.setDecimal(w.value.getValue(3), 3);
                } else if (unitStr.equals(GRAMS)) {
                    w.unit = UnitType.g;
                    // g: ganze Gramm
                    w.value.setDecimal(w.value.getValue(0), 0);
                } else {
                    // keine Einheit angegeben: Standard verwenden
                    w.unit = getDefaultUnit();
                    if (w.unit == UnitType.kg || w.unit == UnitType.lb) {
                        w.value.setDecimal(w.value.getValue(3), 3);
                    } else {
                        w.value.setDecimal(w.value.getValue(0), 0);
                    }
                }
            }
        }
        return w;
    }

    public static DataTypeWeight getWeight(long valueInDefaultUnit) {
    	//@Todo this has been coded equally to DataTypeDistance.getDistance() but I am unsure if this is really correct for weight 
    	//if unittype kg, it should be KILOGRAMS instead of GRAMS.
    	//for pounds it is correct to use POUNDS as unit and not GRAMS, because the value is already in default unit (which could be pounds).
    	if (getDefaultUnit() == UnitType.kg) {
            return parseWeight(Long.toString(valueInDefaultUnit) + " " + GRAMS, false);
        } else {
            return parseWeight(Long.toString(valueInDefaultUnit) + " " + POUNDS, false);
        }
    }

    public static DataTypeWeight getWeightFromGrams(long grams) {
        DataTypeWeight w = new DataTypeWeight();
        w.value = new DataTypeDecimal(grams, 0);
        w.unit = UnitType.g;
        return w;
    }

    public void setWeight(DataTypeDecimal value, UnitType unit) {
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
            if (Daten.efaConfig.getValueDefaultWeightUnit().equals(KILOGRAMS)) {
                return UnitType.kg;
            }
            if (Daten.efaConfig.getValueDefaultWeightUnit().equals(POUNDS)) {
                return UnitType.lb;
            }
        }
        return UnitType.kg;
    }

    public static String getDefaultUnitName() {
        if (Daten.efaConfig != null) {
            if (Daten.efaConfig.getValueDefaultWeightUnit().equals(KILOGRAMS)) {
                return International.getString("Kilogramm");
            }
            if (Daten.efaConfig.getValueDefaultWeightUnit().equals(POUNDS)) {
                return International.getString("Pfund");
            }
        }
        return International.getString("Kilogramm");
    }

    public static String getDefaultUnitAbbrevation() {
        return getDefaultUnitAbbrevation(false);
    }

    public static String getDefaultUnitAbbrevation(boolean capitalized) {
        if (Daten.efaConfig != null) {
            if (Daten.efaConfig.getValueDefaultWeightUnit().equals(KILOGRAMS)) {
                return (capitalized ? "Kg" : "kg");
            }
            if (Daten.efaConfig.getValueDefaultWeightUnit().equals(POUNDS)) {
                return (capitalized ? "Lb" : "lb");
            }
        }
        return (capitalized ? "Kg" : "kg");
    }

    public static String getAllUnitAbbrevationsAsString(boolean withWordOther) {
        if (withWordOther) {
            return International.getMessage("{a} oder {b}", "kg, g", "lb");
        } else {
            return "kg, g, lb";
        }
    }

    /**
     * Konversionen:
     * 1 kg = 1000 g
     * 1 lb = 453.59237 g
     *
     * Internes Pattern wie in DataTypeDistance:
     * - value.getValue(3) wird analog benutzt für Einheiten mit 3 Dezimal-Stellen
     * - value.getValue(0) für ganze Einheit (Gramm)
     */

    public long getValueInGrams() {
        if (!isSet()) {
            return 0;
        }
        switch (unit) {
            case kg: return value.getValue(3); // kg -> grams (value.getValue(3) liefert kg*1000)
            case g : return value.getValue(0);
            case lb: return (long)Math.round(((double)value.getValue(3)) * 0.45359237); // lb -> g (factor = 453.59237/1000)
        }
        return 0;
    }

    public long getValueInKilograms() {
        long g = getValueInGrams();
        return g / 1000;
    }

    public long getValueInPounds() {
        if (!isSet()) {
            return 0;
        }
        switch (unit) {
            case kg: return (long)Math.round(((double)value.getValue(3)) / 0.45359237); // kg -> lb (value.getValue(3) = kg*1000 -> divide by 0.45359237 gives lb)
            case g : return (long)Math.round(((double)value.getValue(0)) / 453.59237); // g -> lb
            case lb: return value.getValue(3);
        }
        return 0;
    }

    public void truncateToGrams(int grams) {
        if (!isSet()) return;
        this.value.setDecimal((getValueInGrams() / grams) * grams, 0);
        this.unit = UnitType.g;
    }

    public void truncateToMainWeightUnit() {
        if (!isSet()) return;
        switch (unit) {
            case kg:
            case g:
                this.value.setDecimal(getValueInGrams() / 1000, 0);
                this.unit = UnitType.kg;
                break;
            case lb:
                this.value.setDecimal(getValueInPounds(), 0);
                this.unit = UnitType.lb;
                break;
        }
    }

    // Formatierte Strings

    public String getStringValueInKilograms() {
        return getStringValueInKilograms(false, false, 3, 3);
    }

    public String getStringValueInKilograms(boolean withUnit, boolean formatted) {
        return getStringValueInKilograms(withUnit, formatted, 3, 3);
    }

    public String getStringValueInKilograms(boolean withUnit, int minDecimalPlaces, int maxDecimalPlaces) {
        return getStringValueInKilograms(withUnit, true, minDecimalPlaces, maxDecimalPlaces);
    }

    private String getStringValueInKilograms(boolean withUnit, boolean formatted, int minDecimalPlaces, int maxDecimalPlaces) {
        DataTypeDecimal d = new DataTypeDecimal(getValueInGrams(), 3);
        return (formatted ? d.getAsFormattedString(minDecimalPlaces, maxDecimalPlaces) : d.toString())
                + (withUnit ? " " + KILOGRAMS : "");
    }

    public String getStringValueInPounds() {
        return getStringValueInPounds(false, false, 3, 3);
    }

    public String getStringValueInPounds(boolean withUnit, boolean formatted) {
        return getStringValueInPounds(withUnit, formatted, 3, 3);
    }

    public String getStringValueInPounds(boolean withUnit, int minDecimalPlaces, int maxDecimalPlaces) {
        return getStringValueInPounds(withUnit, true, minDecimalPlaces, maxDecimalPlaces);
    }

    private String getStringValueInPounds(boolean withUnit, boolean formatted, int minDecimalPlaces, int maxDecimalPlaces) {
        // Erzeugt Decimal aus Pfund-Äquivalent in "tausendsten" (analog zu Distanz)
        DataTypeDecimal d = new DataTypeDecimal((getValueInPounds()*1000)/1, 3);
        return (formatted ? d.getAsFormattedString(minDecimalPlaces, maxDecimalPlaces) : d.toString())
                + (withUnit ? " " + POUNDS : "");
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
        if (getDefaultUnit() == UnitType.kg) {
            return getStringValueInKilograms(withUnit, formatted, minDecimalPlaces, maxDecimalPlaces);
        } else {
            return getStringValueInPounds(withUnit, formatted, minDecimalPlaces, maxDecimalPlaces);
        }
    }

    public long getValueInDefaultUnit() {
        if (getDefaultUnit() == UnitType.kg) {
            return getValueInGrams();
        } else {
            return getValueInPounds();
        }
    }

    public static void main(String[] args) {
        for (int i=0; i<args.length; i++) {
            String s = args[i];
            System.out.println("Input: >>>"+s+"<<<");
            DataTypeWeight w = DataTypeWeight.parseWeight(s, true);
            System.out.println("Weight   : "+w.toString());
            System.out.println("in grams : "+w.getValueInGrams());
            System.out.println("in pounds: "+w.getValueInPounds());
            System.out.println("in kg    : "+w.getValueInKilograms());
        }
    }
}
