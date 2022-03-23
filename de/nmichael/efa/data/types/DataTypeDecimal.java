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

public class DataTypeDecimal {

    private long value;
    private int decimalPlaces;

    // Default Constructor
    public DataTypeDecimal() {
        unset();
    }

    // Regular Constructor
    public DataTypeDecimal(long value, int decimalPlaces) {
        this.value = value;
        this.decimalPlaces = decimalPlaces;
        normalize();
    }

    // Copy Constructor
    public DataTypeDecimal(DataTypeDecimal decimal) {
        this.value = decimal.value;
        this.decimalPlaces = decimal.decimalPlaces;
        normalize();
    }

    public static DataTypeDecimal parseDecimal(String s, boolean fromGUI) {
        DataTypeDecimal decimal = new DataTypeDecimal();
        decimal.setDecimal(s, fromGUI);
        return decimal;
    }

    public void setDecimal(long value, int decimalPlaces) {
        this.value = value;
        this.decimalPlaces = decimalPlaces;
        normalize();
    }

    public void setDecimal(String s, boolean fromGUI) {
        if (fromGUI) {
            s = EfaUtil.replace(s, Character.toString(International.getThousandsSeparator()), "");
            s = EfaUtil.replace(s, Character.toString(International.getDecimalSeparator()), ".");
        }
        value = 0;
        decimalPlaces = 0;
        boolean positive = true;
        boolean inFraction = false;
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            switch(c) {
                case ' ':
                    if (value == 0) {
                        // ignore
                    } else {
                        normalize();
                        return;
                    }
                    break;
                case '-':
                    if (value == 0 && positive) {
                        positive = false;
                    } else {
                        normalize();
                        return;
                    }
                    break;
                case '+':
                    if (value == 0 && positive) {
                        // ignore
                    } else {
                        normalize();
                        return;
                    }
                    break;
                case '.':
                case ',':
                    if (!inFraction) {
                        inFraction = true;
                    } else {
                        normalize();
                        return;
                    }
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    value = value*10l + (c - '0');
                    if (inFraction) {
                        decimalPlaces++;
                    }
                    if (value > 0 && !positive) {
                        value *= -1;
                    }
                    break;
                default:
                        normalize();
                    return;
            }
        }
        normalize();
    }

    private String toString(boolean formatted, int minDecimalPlaces, int maxDecimalPlaces) {
        if (isSet()) {
            if (decimalPlaces == 0 && minDecimalPlaces == 0) {
                return Long.toString(value);
            }
            long precision = 1;
            for (int i=0; i<decimalPlaces; i++) {
                precision *= 10;
            }
            char decimalSep = (formatted ? International.getDecimalSeparator() : '.');
            if (decimalSep != '.' && decimalSep != ',') {
                decimalSep = '.';
            }
            long fraction = value % precision;
            if (maxDecimalPlaces < decimalPlaces) {
                long precisionDiff = 1;
                for (int i = maxDecimalPlaces; i < decimalPlaces; i++) {
                    precisionDiff *= 10;
                }
                fraction = fraction / precisionDiff;
            } else if (maxDecimalPlaces > decimalPlaces) {
                long precisionDiff = 1;
                for (int i = decimalPlaces; i < maxDecimalPlaces; i++) {
                    precisionDiff *= 10;
                }
                fraction = fraction * precisionDiff;
            }
            if (maxDecimalPlaces == 0) {
                return Long.toString(value / precision);
            }
            return Long.toString(value / precision) + decimalSep + EfaUtil.long2String(fraction, minDecimalPlaces);
        }
        return "";
    }

    public String toString() {
        return toString(false, decimalPlaces, decimalPlaces);
    }

    public String getAsFormattedString() {
        return toString(true, decimalPlaces, decimalPlaces);
    }

    public String getAsFormattedString(int minDecimalPlaces, int maxDecimalPlaces) {
        return toString(true, minDecimalPlaces, maxDecimalPlaces);
    }

    public boolean isSet() {
        return decimalPlaces >= 0;
    }

    public void unset() {
        value = 0;
        decimalPlaces = -1;
    }
    
    private void normalize() {
        while (decimalPlaces > 0) {
            if (value % 10l == 0) {
                value /= 10l;
                decimalPlaces--;
            } else {
                break;
            }
        }
    }
    
    public double getValue() {
    	return value / Math.pow(10, this.decimalPlaces);
    }

    public long getValue(int decimalPlaces) {
        if (decimalPlaces == this.decimalPlaces) {
            return value;
        }
        long myValue = value;
        int myDecimalPlaces = this.decimalPlaces;
        while (decimalPlaces > myDecimalPlaces) {
            myValue *= 10l;
            myDecimalPlaces++;
        }
        while (decimalPlaces < myDecimalPlaces) {
            myValue /= 10l;
            myDecimalPlaces--;
        }
        return myValue;
    }

}
