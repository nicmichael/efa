/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.core;

import de.nmichael.efa.util.EfaUtil;
import java.security.*;
import java.security.cert.*;
import java.io.*;
import java.util.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class EfaKeyStore {

    private KeyStore keyStore;
    private String filename;
    private char[] password;
    private String lastError;

    public EfaKeyStore(String filename, char[] password) {
        this.filename = filename;
        this.password = password;
        this.lastError = null;
        reload();
    }

    public boolean reload() {
        try {
            keyStore = KeyStore.getInstance("JKS");
            if (EfaUtil.canOpenFile(filename)) {
                InputStream is = new FileInputStream(filename);
                keyStore.load(is, password);
            } else {
                keyStore.load(null, password);
            }
        } catch (Exception e) {
            keyStore = null;
            lastError = e.toString();
            return false;
        }
        return true;
    }

    public boolean isKeyStoreReady() {
        return keyStore != null;
    }

    public String getFileName() {
        return filename;
    }

    public PublicKey getPublicKey(String alias) {
        if (keyStore == null) {
            lastError = "Kein Schlüsselspeicher geladen";
            return null;
        }
        lastError = null;
        PublicKey key = null;
        try {
            key = keyStore.getCertificate(alias).getPublicKey();
        } catch (Exception e) {
            lastError = e.toString();
        }
        return key;
    }

    public PrivateKey getPrivateKey(String alias) {
        if (keyStore == null) {
            lastError = "Kein Schlüsselspeicher geladen";
            return null;
        }
        lastError = null;
        PrivateKey key = null;
        try {
            key = (PrivateKey) keyStore.getKey(alias, password);
        } catch (Exception e) {
            lastError = e.toString();
        }
        return key;
    }

    public X509Certificate getCertificate(String alias) {
        if (keyStore == null) {
            lastError = "Kein Schlüsselspeicher geladen";
            return null;
        }
        X509Certificate cert = null;
        try {
            cert = (X509Certificate) keyStore.getCertificate(alias);
        } catch (Exception e) {
            lastError = e.toString();
        }
        return cert;
    }

    public Enumeration getAliases() {
        if (keyStore == null) {
            lastError = "Kein Schlüsselspeicher geladen";
            return null;
        }
        Enumeration en = null;
        try {
            en = keyStore.aliases();
        } catch (Exception e) {
            lastError = e.toString();
        }
        return en;
    }

    public int size() {
        if (keyStore == null) {
            lastError = "Kein Schlüsselspeicher geladen";
            return 0;
        }
        try {
            return keyStore.size();
        } catch (Exception e) {
            lastError = e.toString();
            return 0;
        }
    }
    /*
    public boolean addPublicKey(String alias, PublicKey key) {
    if (keyStore == null) {
    lastError = "Kein Schlüsselspeicher geladen";
    return false;
    }
    if (privKeys) {
    lastError = "Dieser Schlüsselspeicher ist ausschließlich für private Schlüssel gedacht";
    return false;
    }
    lastError = null;
    try {
    keyStore.setKeyEntry(alias,key,password,null);
    keyStore.store(new FileOutputStream(filename),password);
    } catch(Exception e) {
    lastError = e.toString();
    return false;
    }
    return true;
    }
     */

    public boolean addCertificate(String alias, java.security.cert.Certificate cert) {
        if (keyStore == null) {
            lastError = "Kein Schlüsselspeicher geladen";
            return false;
        }
        lastError = null;
        try {
            keyStore.setCertificateEntry(alias, cert);
            keyStore.store(new FileOutputStream(filename), password);
        } catch (Exception e) {
            lastError = e.toString();
            return false;
        }
        return true;
    }

    public String getLastError() {
        return lastError;
    }
}
