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

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.Base64;
import de.nmichael.efa.util.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class DataTypePasswordCrypted {

    private static final String CRYPTED = "*~c:";
    private static SecretKey key = null;

    static {
        try {
            DESKeySpec keySpec = new DESKeySpec(Daten.EFAMASTERKEY.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            key = keyFactory.generateSecret(keySpec);
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_ERR_GENERIC,
                    "Failed to initialize internal password store: " + e.toString());
            Logger.logdebug(e);
        }
    }

    private String password;

    // Default Constructor
    public DataTypePasswordCrypted() {
        unset();
    }

    // Regular Constructor
    public DataTypePasswordCrypted(String password) {
        setPassword(password);
    }

    // Copy Constructor
    public DataTypePasswordCrypted(DataTypePasswordCrypted pwd) {
        this.password = pwd.password;
    }

    public static DataTypePasswordCrypted parsePassword(String s) {
        return new DataTypePasswordCrypted(s);
    }

    public synchronized static String encrypt(String sd) {
        try {
            byte[] cleartext = sd.getBytes("UTF8");
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.encodeBytes(cipher.doFinal(cleartext));
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public synchronized static String decrypt(String se) {
        try {
            byte[] encrypedPwdBytes = Base64.decode(se);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] plainTextPwdBytes = (cipher.doFinal(encrypedPwdBytes));
            return new String(plainTextPwdBytes);
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public void setPassword(String s) {
        if (s != null) {
            s = s.trim();
            if (s.startsWith(CRYPTED)) {
                password = decrypt(s.substring(CRYPTED.length()));
            } else {
                password = s;
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
            return CRYPTED + encrypt(password);
        }
        return "";
    }

    public boolean isSet() {
        return password != null && password.length() > 0;
    }

    public void unset() {
        password = null;
    }

    public boolean equals(DataTypePasswordCrypted pwd) {
        return this.isSet() && pwd.isSet() && this.password.equals(pwd.password);
    }

 }
