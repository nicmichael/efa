/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.data.efawett;

import de.nmichael.efa.*;
import de.nmichael.efa.util.Base64;
import de.nmichael.efa.core.EfaKeyStore;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;
import java.util.*;
import java.security.*;
import java.security.cert.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class DRVSignatur {

    private String _meldung = null; // Meldung als String bestehend aus den folgenden Feldern
    private String teilnNr = null;
    private String vorname = null;
    private String nachname = null;
    private String jahrgang = null;
    private int anzAbzeichen = 0;
    private int gesKm = 0;
    private int anzAbzeichenAB = 0;
    private int gesKmAB = 0;
    private int jahr = 0;
    private int letztkm = 0;
    private String sigDatum = null;
    private String _signatur = null; // Signatur als Base64-String bestehend aus den folgenden Feldern
    private byte version = -1;
    private byte keyNr = -1;
    private byte[] signatur = null;
    private int signatureState = SIG_INCOMPLETE;
    private String signatureError = null;
    public static final int SIG_VALID = 0;
    public static final int SIG_INVALID = 1;
    public static final int SIG_UNKNOWN_VERSION = 2;
    public static final int SIG_UNKNOWN_KEY = 3;
    public static final int SIG_INCOMPLETE = 4;
    public static final int SIG_KEY_NOT_VALID_FOR_YEAR = 5;
    public static final int SIG_KEY_NOT_VALID_ON_SIGDATE = 6;
    public static final int SIG_ERROR = 7;
    public static final byte VERSION = 2;
    public static final byte VERSION_MIN = 2;
    public static final byte VERSION_MAX = 3;

    // Konstruktor zum Überprüfen einer Signatur
    public DRVSignatur(String fahrtenheft) {
        fahrtenheft = EfaUtil.replace(fahrtenheft, "; ", ";", true);
        fahrtenheft = EfaUtil.replace(fahrtenheft, " ;", ";", true);
        try {

            // Felder aus String ermitteln
            Vector v = EfaUtil.split(fahrtenheft, ';');

            // Signatur ermitteln
            if (v.size() > 0) {
                this._signatur = EfaUtil.replace((String) v.get(v.size() - 1), " ", "", true); // _signatur ist immer das letzte Element
            }
            // Signatur, Version und Schlüssel ermitteln
            if (_signatur != null) {
                byte[] plainsig = Base64.decode(_signatur);
                if (plainsig == null) {
                    Logger.log(Logger.ERROR, Logger.MSG_ERR_BASE64DECODE,
                            "Failed to decode Base64 signature '" + _signatur + "' for: " + fahrtenheft);
                } else if (plainsig.length >= 3) {
                    this.version = plainsig[0];
                    this.keyNr = plainsig[1];
                    this.signatur = new byte[plainsig.length - 2];
                    for (int i = 0; i < this.signatur.length; i++) {
                        this.signatur[i] = plainsig[i + 2];
                    }
                }
            }

            // Datenfelder ermitteln
            for (int i = 0; i < v.size(); i++) {
                switch (this.version) {
                    case 2: // Version 2
                        switch (i) {
                            case 0:
                                this.teilnNr = (String) v.get(i);
                                break;
                            case 1:
                                this.vorname = (String) v.get(i);
                                break;
                            case 2:
                                this.nachname = (String) v.get(i);
                                break;
                            case 3:
                                this.jahrgang = (String) v.get(i);
                                break;
                            case 4:
                                this.anzAbzeichen = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 5:
                                this.gesKm = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 6:
                                this.anzAbzeichenAB = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 7:
                                this.gesKmAB = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 8:
                                this.jahr = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 9:
                                this.sigDatum = (String) v.get(i);
                                break;
                        }
                        break;
                    case 3: // Version 3
                        switch (i) {
                            case 0:
                                this.teilnNr = (String) v.get(i);
                                break;
                            case 1:
                                this.vorname = (String) v.get(i);
                                break;
                            case 2:
                                this.nachname = (String) v.get(i);
                                break;
                            case 3:
                                this.jahrgang = (String) v.get(i);
                                break;
                            case 4:
                                this.anzAbzeichen = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 5:
                                this.gesKm = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 6:
                                this.anzAbzeichenAB = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 7:
                                this.gesKmAB = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 8:
                                this.jahr = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 9:
                                this.letztkm = EfaUtil.string2int((String) v.get(i), 0);
                                break;
                            case 10:
                                this.sigDatum = (String) v.get(i);
                                break;
                        }
                        break;
                    default: // unbekannte Version
                        this.signatureState = SIG_UNKNOWN_VERSION;
                        this.signatureError = "Unbekannte Fahrtenheftversion: " + this.version;
                }
            }

            // gesamten Meldungsstring zusammensetzen
            _meldung = construct_meldung(false);

            checkSignature();
        } catch (Exception e) {
            this.signatureState = SIG_ERROR;
            this.signatureError = e.getMessage();
        }
    }

    private static Signature getSignature(Key key) throws Exception {
        Exception ex = null;
        for (String sigInstance : new String[] { "SHA1withDSA", "SHA256withRSA" }) {
            try {
                Signature sig = Signature.getInstance(sigInstance);
                if (key instanceof PrivateKey) {
                    sig.initSign((PrivateKey)key);
                } else {
                    sig.initVerify((PublicKey)key);
                }
                return sig;
            } catch (Exception e) {
                ex = e;
            }
        }
        throw ex;
    }

    // Konstruktor zum Erstellen einer Signatur
    public DRVSignatur(String teilnNr, 
            String vorname, String nachname, String jahrgang, int anzAbzeichen,
            int gesKm, int anzAbzeichenAB, int gesKmAB, int jahr, int letztkm, String sigDatum,
            byte version, byte keyNr, PrivateKey privKey) throws Exception {
        if (teilnNr == null || teilnNr.length() == 0) {
            throw new Exception("Ungültige Teilnehmernummer");
        }
        if (vorname == null || vorname.length() == 0) {
            throw new Exception("Ungültiger Vorname");
        }
        if (nachname == null || nachname.length() == 0) {
            throw new Exception("Ungültiger Nachname");
        }
        if (jahrgang == null || jahrgang.length() != 4) {
            throw new Exception("Ungültiger Jahrgang");
        }
        if (anzAbzeichen <= 0) {
            throw new Exception("Ungültige Anzahl an Abzeichen");
        }
        if (gesKm <= 0) {
            throw new Exception("Ungültige Anzahl an Kilometern");
        }
        if (anzAbzeichenAB < 0) {
            throw new Exception("Ungültige Anzahl an Abzeichen der Jugeng-Gruppen A/B");
        }
        if (gesKmAB < 0) {
            throw new Exception("Ungültige Anzahl an AB-Kilometern der Jugeng-Gruppen A/B");
        }
        if (anzAbzeichenAB > anzAbzeichen) {
            throw new Exception("Anzahl der Abzeichen für Jugend-Gruppen A/B ist größer als Anzahl der Abzeichen insgesamt");
        }
        if (gesKmAB > gesKm) {
            throw new Exception("Anzahl der Kilometer für Jugend-Gruppen A/B ist größer als Anzahl der Kilometer insgesamt");
        }
        if (jahr < 1900) {
            throw new Exception("Ungültiges Erfüllungsjahr");
        }
        if (letztkm < 0) {
            throw new Exception("Ungültige Kilometer für letztes Erfüllungsjahr");
        }
        if (version < VERSION_MIN || version > VERSION_MAX) {
            throw new Exception("Ungültige Version");
        }
        if (keyNr < 0) {
            throw new Exception("Ungültige Schlüssel-Nummer");
        }
        if (privKey == null) {
            throw new Exception("Ungültiger Schlüssel (Schlüssel nicht gefunden)");
        }

        // Felder setzen
        this.teilnNr = EfaUtil.removeSepFromString(teilnNr.trim(), ";");
        this.vorname = EfaUtil.removeSepFromString(vorname.trim(), ";");
        this.nachname = EfaUtil.removeSepFromString(nachname.trim(), ";");
        this.jahrgang = EfaUtil.removeSepFromString(jahrgang.trim(), ";");
        this.anzAbzeichen = anzAbzeichen;
        this.gesKm = gesKm;
        this.anzAbzeichenAB = anzAbzeichenAB;
        this.gesKmAB = gesKmAB;
        this.jahr = jahr;
        this.letztkm = letztkm;
        this.sigDatum = (sigDatum == null ? makeSigDatum(System.currentTimeMillis()) : sigDatum);
        this.version = version;
        this.keyNr = keyNr;

        this._meldung = construct_meldung(false);

        // Fahrtenheft signieren
        try {
            // String um Versions- und Key-Nummer erweitern
            String stringToSign = this._meldung + "#" + this.version + "#" + this.keyNr;

            // Input-String in byte[] umwandeln
            byte[] string = stringToSign.getBytes("ISO-8859-1");

            // String signieren
            Signature sig = getSignature(privKey);
            sig.update(string);
            this.signatur = sig.sign();

            // Schlüsselnummer der Signatur voranstellen
            byte[] keySig = new byte[this.signatur.length + 2];
            keySig[0] = this.version;
            keySig[1] = this.keyNr;
            for (int i = 0; i < this.signatur.length; i++) {
                keySig[i + 2] = this.signatur[i];
            }

            // Kodiere Signatur Base64
            this._signatur = Base64.encodeBytes(keySig);

            this.signatureState = SIG_VALID;
        } catch (Exception e) {
            this.signatureState = SIG_ERROR;
            this.signatureError = e.getMessage();
        }
    }

    public void checkSignature() {
        try {
            // Sigatur-Zustand ermitteln
            if (this.signatur == null) {
                this.signatureState = SIG_INCOMPLETE;
                this.signatureError = "Keine Signatur vorhanden";
            } else {
                if (this.version < VERSION_MIN || this.version > VERSION_MAX) {
                    this.signatureState = SIG_UNKNOWN_VERSION;
                    this.signatureError = "Ungültige Version (" + this.version + ")";
                } else {
                    if (this.keyNr < 0) {
                        this.signatureState = SIG_ERROR;
                        this.signatureError = "Ungültige Schlüssel-Nummer (" + this.keyNr + ")";
                    } else {
                        // Signatur vorhanden, gültige Version und KeyNr>=0

                        // Zertifikat prüfen
                        X509Certificate cert = getCertificate();
                        if (cert == null) {
                            this.signatureState = SIG_UNKNOWN_KEY;
                            this.signatureError = "Unbekannter Schlüssel";
                        } else {

                            // ist Schlüssel für Signaturjahr gültig?
                            if (!CertInfos.sigKeyValidForYear(cert, this.jahr)) {
                                this.signatureState = SIG_KEY_NOT_VALID_FOR_YEAR;
                                this.signatureError = "Schlüssel " + getKeyName() + " für Fahrtenhefte des Jahres " + this.jahr + " nicht gültig";
                            } else {

                                // war der Schlüssel zum Signierzeitpunkt gültig?
                                if (!CertInfos.sigKeyValidOnDate(cert, getSignatureDate())) {
                                    this.signatureState = SIG_KEY_NOT_VALID_ON_SIGDATE;
                                    String sdate = "<kein Datum>";
                                    if (getSignatureDate() != null) {
                                        GregorianCalendar cal = new GregorianCalendar();
                                        cal.setTime(getSignatureDate());
                                        sdate = cal.toString();
                                    }
                                    this.signatureError = "Schlüssel " + getKeyName() + " zum Signierzeitpunkt nicht gültig (" + sdate + ")";
                                } else {

                                    // Öffentlichen Schlüssel ermitteln
                                    PublicKey pubKey = cert.getPublicKey();
                                    if (pubKey == null) {
                                        this.signatureState = SIG_UNKNOWN_KEY;
                                        this.signatureError = "Unbekannter Schlüssel (" + getKeyName() + ")";
                                    } else {

                                        // String um Versions- und Key-Nummer erweitern
                                        String stringToSign = this._meldung + "#" + this.version + "#" + this.keyNr;

                                        // Signatur überprüfen
                                        byte[] string = stringToSign.getBytes("ISO-8859-1");
                                        Signature sig = getSignature(pubKey);
                                        sig.update(string);
                                        if (sig.verify(this.signatur)) {
                                            this.signatureState = SIG_VALID;
                                        } else {
                                            this.signatureState = SIG_INVALID;
                                            this.signatureError = "Signatur ungültig";
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            this.signatureState = SIG_ERROR;
            this.signatureError = e.getMessage();
        }
    }

    // ermittelt den Signierzeitpunkt als Date
    public Date getSignatureDate() {
        if (this.sigDatum == null) {
            return null;
        }
        if (this.sigDatum.length() != 12) {
            return null;
        }
        try {
            int tag = Integer.parseInt(this.sigDatum.substring(0, 2));
            int monat = Integer.parseInt(this.sigDatum.substring(2, 4));
            int jahr = Integer.parseInt(this.sigDatum.substring(4, 8));
            int std = Integer.parseInt(this.sigDatum.substring(8, 10));
            int min = Integer.parseInt(this.sigDatum.substring(10, 12));
            GregorianCalendar cal = new GregorianCalendar(jahr, monat - 1, tag, std, min, 0);
            return cal.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private String construct_meldung(boolean forDisplay) {
        switch (this.version) {
            case 2: // Version 2
                if (forDisplay) {
                    return teilnNr + "; " + vorname + "; " + nachname + "; " + jahrgang + "; " + anzAbzeichen + "; " + gesKm + "; " + anzAbzeichenAB + "; " + gesKmAB + "; " + jahr + "; " + sigDatum;
                }
                return teilnNr + ";" + vorname + ";" + nachname + ";" + jahrgang + ";" + anzAbzeichen + ";" + gesKm + ";" + anzAbzeichenAB + ";" + gesKmAB + ";" + jahr + ";" + sigDatum;
            case 3: // Version 3
                if (forDisplay) {
                    return teilnNr + "; " + vorname + "; " + nachname + "; " + jahrgang + "; " + anzAbzeichen + "; " + gesKm + "; " + anzAbzeichenAB + "; " + gesKmAB + "; " + jahr + "; " + letztkm + "; " + sigDatum;
                }
                return teilnNr + ";" + vorname + ";" + nachname + ";" + jahrgang + ";" + anzAbzeichen + ";" + gesKm + ";" + anzAbzeichenAB + ";" + gesKmAB + ";" + jahr + ";" + letztkm + ";" + sigDatum;
            default: // Unbekannte Version
                return null;
        }
    }

    private X509Certificate getCertificate() {
        if (Daten.keyStore == null) {
            return null;
        }
        return Daten.keyStore.getCertificate(getKeyName());
    }

    public String toString() {
        if (_meldung == null || _signatur == null) {
            return null;
        }
        return construct_meldung(true) + "; " + getSignaturString();
    }

    public int getSignatureState() {
        return signatureState;
    }

    public String getSignatureStateDescription() {
        switch (getSignatureState()) {
            case -1:
                return "";
            case DRVSignatur.SIG_VALID:
                return "Das Fahrtenheft ist gültig!";
            case DRVSignatur.SIG_INVALID:
                return "Das Fahrtenheft ist ungültig!";
            case DRVSignatur.SIG_UNKNOWN_KEY:
                return "Das Fahrtenheft kann nicht geprüft werden: Unbekannter Schlüssel!";
            case DRVSignatur.SIG_UNKNOWN_VERSION:
                return "Das Fahrtenheft kann nicht geprüft werden: Unbekannte Version!";
            case DRVSignatur.SIG_INCOMPLETE:
                return "Das Fahrtenheft ist unvollständig!";
            case DRVSignatur.SIG_KEY_NOT_VALID_FOR_YEAR:
                return "Das Fahrtenheft ist ungültig: Schlüssel ist für " + getJahr() + " nicht gültig!";
            case DRVSignatur.SIG_KEY_NOT_VALID_ON_SIGDATE:
                return "Das Fahrtenheft ist ungültig: Schlüssel war zum Erstellungszeitpunkt ungültig!";
            case DRVSignatur.SIG_ERROR:
                return "Beim Überprüfen des Fahrtenhefts trat ein Fehler auf: " + getSignatureError();
        }
        return "";
    }

    public String getSignatureError() {
        return signatureError;
    }

    public String getTeilnNr() {
        return teilnNr;
    }

    public String getVorname() {
        return vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public String getJahrgang() {
        return jahrgang;
    }

    public int getAnzAbzeichen() {
        return anzAbzeichen;
    }

    public int getGesKm() {
        return gesKm;
    }

    public int getAnzAbzeichenAB() {
        return anzAbzeichenAB;
    }

    public int getGesKmAB() {
        return gesKmAB;
    }

    public int getJahr() {
        return jahr;
    }

    public int getLetzteKm() {
        return letztkm;
    }

    public String getVorNachnameJahr() {
        return vorname + " " + nachname + " (Jahrgang " + jahr + ")";
    }
    
    public String getSignaturDatum(boolean formatted) {
        if (!formatted) {
            return sigDatum;
        }
        try {
            return sigDatum.substring(0, 2) + "."
                    + sigDatum.substring(2, 4) + "."
                    + sigDatum.substring(4, 8) + " "
                    + sigDatum.substring(8, 10) + ":"
                    + sigDatum.substring(10, 12);
        } catch (Exception e) {
            return sigDatum;
        }
    }

    public byte getVersion() {
        return version;
    }

    public byte getKeyNr() {
        return keyNr;
    }

    public String getKeyName() {
        String nr = Byte.toString(keyNr);
        if (nr.length() < 2) {
            nr = "0" + nr;
        }
        return "drv" + nr;
    }

    public static String getKeyName(byte keyNr) {
        String nr = Byte.toString(keyNr);
        if (nr.length() < 2) {
            nr = "0" + nr;
        }
        return "drv" + nr;
    }

    public String getSignaturString() {
        if (_signatur == null) {
            return _signatur;
        }
        String s = "";
        for (int i = 0; i < _signatur.length(); i++) {
            s += _signatur.charAt(i);
            if ((i + 1) % 5 == 0) {
                s += " ";
            }
        }
        return s;
    }

    public static String makeSigDatum(long time) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        return EfaUtil.leadingZeroString(cal.get(Calendar.DAY_OF_MONTH), 2)
                + EfaUtil.leadingZeroString(cal.get(Calendar.MONTH) + 1, 2)
                + EfaUtil.leadingZeroString(cal.get(Calendar.YEAR), 4)
                + EfaUtil.leadingZeroString(cal.get(Calendar.HOUR_OF_DAY), 2)
                + EfaUtil.leadingZeroString(cal.get(Calendar.MINUTE), 2);
    }

    // Testmethode
    public static void main(String[] args) {
        if (args.length != 15) {
            System.out.println("usage: DRVSignatur <keystore-file> <password> <key-alias> <teilnnr> <vorname> <nachname> <jahrgang> <abzeichen> <km> <abzeichenAB> <kmAB> <jahr> <letztkm> <sigDatum=0> <version>");
            System.exit(1);
        }
        try {
            Daten.keyStore = new EfaKeyStore(args[0], args[1].toCharArray());
            PrivateKey key = Daten.keyStore.getPrivateKey(args[2]);
            byte keynr = (byte) EfaUtil.string2date(args[2], 0, 0, 0).tag;
            DRVSignatur sig = new DRVSignatur(args[3], args[4], args[5], args[6],
                    EfaUtil.string2int(args[7], 0), EfaUtil.string2int(args[8], 0),
                    EfaUtil.string2int(args[9], 0), EfaUtil.string2int(args[10], 0),
                    EfaUtil.string2int(args[11], 0), EfaUtil.string2int(args[12], 0),
                    args[13],
                    (byte) EfaUtil.string2int(args[14], 0), keynr, key);
            System.out.println(sig.toString());
            sig.checkSignature();
            if (sig.getSignatureState() == SIG_VALID) {
                System.out.println("Signatur ist gültig!");
            } else {
                System.out.println("Signatur ist ungültig: " + sig.getSignatureError());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
