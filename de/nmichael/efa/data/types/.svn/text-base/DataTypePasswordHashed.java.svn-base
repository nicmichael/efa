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

public class DataTypePasswordHashed {

    private static final String CRYPTED = "*~c:";

    private String password;

    // Default Constructor
    public DataTypePasswordHashed() {
        unset();
    }

    // Regular Constructor
    public DataTypePasswordHashed(String password) {
        setPassword(password);
    }

    // Copy Constructor
    public DataTypePasswordHashed(DataTypePasswordHashed pwd) {
        this.password = pwd.password;
    }

    public static DataTypePasswordHashed parsePassword(String s) {
        return new DataTypePasswordHashed(s);
    }

    public static String encrypt(String s) {
        return EfaUtil.getSHA(s);
    }

    public static String decrypt(String s) {
        return s;
    }

    public void setPassword(String s) {
        if (s != null) {
            s = s.trim();
            if (s.startsWith(CRYPTED)) {
                password = s.substring(CRYPTED.length());
            } else {
                password = encrypt(s);
            }
        } else {
            password = null;
        }
    }

    public String getPassword() {
        if (isSet()) {
            return password;
        } else {
            return null;
        }
    }

    public String toString() {
        if (isSet()) {
            return CRYPTED + password;
        }
        return "";
    }

    public boolean isSet() {
        return password != null && password.length() > 0;
    }

    public void unset() {
        password = null;
    }

    public boolean equals(DataTypePasswordHashed pwd) {
        return this.isSet() && pwd.isSet() && this.password.equals(pwd.password);
    }

 }
