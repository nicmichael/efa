/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data;

import de.nmichael.efa.data.efawett.CertInfos;
import de.nmichael.efa.data.efawett.DRVSignatur;
import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.data.efawett.ESigFahrtenhefte;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.data.efawett.EfaWettClient;
import de.nmichael.efa.gui.SimpleInputDialog;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import de.nmichael.efa.util.Base64;
import javax.swing.JDialog;

// @i18n complete

public class Fahrtenabzeichen extends StorageObject {

    public static final String DATATYPE = "efa2fahrtenabzeichen";
    private String meldungEingespielteFahrtenhefte;

    public Fahrtenabzeichen(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.onlyFor("Fahrtenabzeichen","de"));
        FahrtenabzeichenRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new FahrtenabzeichenRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public FahrtenabzeichenRecord createFahrtenabzeichenRecord(UUID id) {
        FahrtenabzeichenRecord r = new FahrtenabzeichenRecord(this, MetaData.getMetaData(DATATYPE));
        r.setPersonId(id);
        r.setKilometer(0);
        r.setAbzeichen(0);
        r.setKilometerAB(0);
        r.setAbzeichenAB(0);
        return r;
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, FahrtenabzeichenRecord.PERSONID);
        }
    }

    public FahrtenabzeichenRecord getFahrtenabzeichen(UUID personId) {
        try {
            return (FahrtenabzeichenRecord)data().get(FahrtenabzeichenRecord.getKey(personId));
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public static boolean isNameInFahrtenabzeichenEqualPersonRecord(String firstName, String lastName, PersonRecord p) {
        String name = (firstName != null ? firstName : "");
        if (lastName != null && lastName.length() > 0) {
            name = (name.length() > 0 ? name + " " : "") + lastName;
        }
        return name.equals(p.getFirstLastName());
    }

    public FahrtenabzeichenRecord getFahrtenabzeichen(String vorname, String nachname) {
        try {
            DataKeyIterator it = data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                FahrtenabzeichenRecord fa = (FahrtenabzeichenRecord)data().get(k);
                if (fa != null && fa.getPersonId() != null) {
                    UUID id = fa.getPersonId();
                    PersonRecord p = fa.getPersonRecord();
                    if (p != null && isNameInFahrtenabzeichenEqualPersonRecord(vorname, nachname, p)) {
                        return fa;
                    }
                    DataRecord[] records = getProject().getPersons(false).data().getValidAny(PersonRecord.getKey(id, -1));
                    for (int i=0; records != null && i<records.length; i++) {
                        p = (PersonRecord)records[i];
                        if (p != null && isNameInFahrtenabzeichenEqualPersonRecord(vorname, nachname, p)) {
                            return fa;
                        }
                    }
                }
                k = it.getNext();
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public FahrtenabzeichenRecord createFahrtenabzeichen(String vorname, String nachname) {
        try {
            PersonRecord p = getProject().getPersons(false).getPerson(vorname + " " + nachname, -1);
            if (p != null && p.getId() != null) {
                return createFahrtenabzeichenRecord(p.getId());
            }
            // if we haven't found a matching person, it might be because of a Name Affix.
            // try to find person just based on first and last name
            DataKey[] keys = getProject().getPersons(false).data().getByFields(
                    new String[] { PersonRecord.FIRSTNAME, PersonRecord.LASTNAME},
                    new String[] { vorname, nachname },
                    -1);
            if (keys != null && keys.length > 0) {
                p = (PersonRecord)getProject().getPersons(false).data().get(keys[0]);
                if (p != null && p.getId() != null) {
                    return createFahrtenabzeichenRecord(p.getId());
                }
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public boolean downloadKey(String keyname) {
        if (Daten.keyStore == null) {
            return false;
        }
        String keyfile = null;
        switch (Dialog.auswahlDialog("Unbekannter Schlüssel",
                "Um die Signatur zu prüfen, benötigt efa den öffentlichen Schlüssel '" + keyname + "'.\n"
                + "efa kann diesen Schlüssel aus dem Internet herunterladen oder ihn\n"
                + "aus einer zuvor heruntergeladenen Datei einlesen.\n"
                + "Was möchtest Du tun?",
                "Schlüssel aus Internet herunterladen", "Schlüssel aus Datei einlesen")) {
            case 0:
                String localFile = Daten.efaTmpDirectory + keyname + ".cert";
                if (Daten.wettDefs == null || Daten.wettDefs.efw_drv_url_pubkeys == null
                        || Daten.wettDefs.efw_drv_url_pubkeys.length() == 0) {
                    Dialog.error("Es ist keine Adresse zum Abrufen des Schlüssels konfiguriert.\n"
                            + "Bitte öffne im Menü 'Administration' den Punkt 'Wettbewerbskonfiguration'\n"
                            + "und aktualisiere die Konfigurationsdaten.");
                    return false;
                }
                if (!Dialog.okAbbrDialog("Internet-Verbindung herstellen",
                        "Bitte stelle nun eine Verbindung mit dem Internet her und klicke OK.")) {
                    return false;
                }
                String remoteFile = Daten.wettDefs.efw_drv_url_pubkeys + "/" + keyname + ".cert";
                JDialog parent = null;
                try {
                    parent = (JDialog) Dialog.frameCurrent();
                } catch (Exception eee) {
                }
                if (!DownloadThread.getFile(parent, remoteFile, localFile, true) || !EfaUtil.canOpenFile(localFile)) {
                    Dialog.error("Der Schlüssel konnte nicht heruntergeladen werden.");
                    return false;
                }
                keyfile = localFile;
                break;
            case 1:
                keyfile = Dialog.dateiDialog(Dialog.frameCurrent(), "Öffentlichen Schlüssel auswählen",
                        "Öffentlicher Schlüssel (*.cert)", "cert", Daten.efaMainDirectory, false);
                if (keyfile == null) {
                    return false;
                }
                break;
            default:
                return false;
        }
        if (!EfaUtil.canOpenFile(keyfile)) {
            return false;
        }
        if (importKey(keyfile)) {
            EfaUtil.deleteFile(keyfile);
            return true;
        }
        return false;
    }

    public boolean importKey(String keyfile) {
        try {
            InputStream inStream = new FileInputStream(keyfile);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
            inStream.close();
            String alias = CertInfos.getAliasName(cert);
            if (alias.endsWith(".cert")) {
                alias = alias.substring(0, alias.length() - 5);
            }
            if (alias.length() == 0) {
                Dialog.error("Der Dateiname des öffentlichen Schlüssels ist ungültig.");
                return false;
            }
            if (!Daten.keyStore.addCertificate(alias, cert)) {
                Dialog.error("Fehler beim Hinzufügen des Schlüssels: " + Daten.keyStore.getLastError());
                return false;
            }

            String info = CertInfos.getCertInfos(cert, keyfile);
            if (info != null && info.length() > 0) {
                Dialog.infoDialog("Schlüssel importiert",
                        "Der Schlüssel '" + alias + "' wurde erfolgreich importiert.\n"
                        + "\nZertifikatdaten:\n" + info);
            }

        } catch (Exception e) {
            Dialog.error("Fehler beim Hinzufügen des Schlüssels: " + e.toString());
            return false;
        }
        return true;
    }

    public boolean downloadFahrtenhefte(String[] qnr, String[] wett, String localFile) {
        meldungEingespielteFahrtenhefte = "";

        boolean success = true;
        if (localFile != null) {
            if (!fahrtenhefteEinspielen(localFile)) {
                success = false;
            }
        }
        if (qnr != null && wett != null) {
            if (qnr.length == 1) {
                if (!downloadFahrtenhefte(qnr[0])) {
                    success = false;
                }
            } else {
                int firstYear = 9999;
                int lastYear = 0;
                for (int i = 0; i < qnr.length && i < wett.length; i++) {
                    int y = EfaUtil.string2date(wett[i], -1, 0, 0).tag;
                    if (y > -1 && y < firstYear) {
                        firstYear = y;
                    }
                    if (y > -1 && y > lastYear) {
                        lastYear = y;
                    }
                }
                switch (Dialog.auswahlDialog("Bestätigungsdateien herunterladen",
                        "Es wurden Bestätigungsdateien der Jahre " + firstYear + " - " + lastYear + " gefunden.",
                        "alle Bestätigungsdateien herunterladen", "nur neueste Bestätiungsdatei herunterladen", true)) {
                    case 0:
                        for (int i = 0; i < qnr.length && i < wett.length && qnr[i].length() > 0; i++) {
                            if (!downloadFahrtenhefte(qnr[i])) {
                                success = false;
                            }
                        }
                        break;
                    case 1:
                        if (!downloadFahrtenhefte(null)) {
                            success = false;
                        }
                        break;
                    default:
                        success = false;
                }
            }
        }
        Dialog.infoDialog("Bestätigungsdateien eingespielt", meldungEingespielteFahrtenhefte);
        return success;
    }

    private boolean downloadFahrtenhefte(String qnr) {
        EfaWettClient.reusePasswordForNextRequest();
        String request = EfaWettClient.makeScriptRequestString(EfaWettClient.VERBAND_DRV, EfaWettClient.ACTION_ABRUFEN, (qnr != null ? "qnr=" + qnr : null), null);
        String qnrtxt = "[Quittungsnummber " + qnr + "]";
        if (request == null) {
            Dialog.error(qnrtxt + "Die Bestätigungsdatei konnten nicht heruntergeladen werden!");
            return false;
        }
        String localFile = Daten.efaTmpDirectory + "drvSigFahrtenhefte.efwsig";
        if (!DownloadThread.getFile((JDialog) Dialog.frameCurrent(), request, localFile, true)) {
            Dialog.error(qnrtxt + "Die Bestätigungsdatei konnte nicht heruntergeladen werden!");
            return false;
        }
        try {
            BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(localFile), Daten.ENCODING_ISO));
            String s = f.readLine();
            if (s != null && s.startsWith("ERROR")) {
                Dialog.error(qnrtxt + s);
                return false;
            }
            f.close();
        } catch (Exception e) {
            Dialog.error(qnrtxt + "Die heruntergeladene Bestätigungsdatei konnte nicht geöffnet werden: " + e.toString());
            return false;
        }
        boolean success = fahrtenhefteEinspielen(localFile);
        EfaUtil.deleteFile(localFile);
        return success;
    }

    private boolean fahrtenhefteEinspielen(String localFile) {
        try {
            ESigFahrtenhefte sigfile = new ESigFahrtenhefte(localFile);
            sigfile.readFile();
            Vector<DRVSignatur> fahrtenhefte = sigfile.getFahrtenhefte();

            // ggf. Schlüssel importieren
            if (Daten.keyStore != null && sigfile.keyName != null && sigfile.keyDataBase64 != null) {
                if (Daten.keyStore.getCertificate(sigfile.keyName) == null) {
                    byte[] data = Base64.decode(sigfile.keyDataBase64);
                    String certfile = Daten.efaTmpDirectory + sigfile.keyName + ".cert";
                    try {
                        FileOutputStream fout = new FileOutputStream(certfile);
                        fout.write(data);
                        fout.close();
                        importKey(certfile);
                    } catch (Exception ee) {
                        Dialog.error("Fehler beim Importieren des Schlüssels " + sigfile.keyName + ": " + ee.toString());
                    }
                }
            }

            int sigError = 0;
            int sigErrorUnknownKey = 0;
            int importedCnt = 0;
            String keyname = null;
            String nichtImportiert = null;
            for (int i = 0; i < fahrtenhefte.size(); i++) {
                DRVSignatur sig = (DRVSignatur) fahrtenhefte.get(i);
                UUID personID = sigfile.getIdForSignature(sig);
                FahrtenabzeichenRecord fa = null;
                if (personID != null) {
                    fa = getFahrtenabzeichen(personID);
                }
                if (fa == null) {
                    fa = getFahrtenabzeichen(sig.getVorname(), sig.getNachname());
                }
                boolean newRecord = false;
                if (fa == null) {
                    if (personID != null) {
                        fa = createFahrtenabzeichenRecord(personID);
                    }
                    if (fa == null) {
                        fa = createFahrtenabzeichen(sig.getVorname(), sig.getNachname());
                    }
                    newRecord = true;
                }
                
                // if we didn't find a person record, prompt the user to enter one;
                // we cannot import a person without a record, otherwise the audit will
                // cleanup this record again.
                if (fa == null || (fa != null && !fa.existsPersonRecord())) {
                    if (Daten.isGuiAppl()) {
                        ItemTypeStringAutoComplete item = (fa != null ?
                                fa.getUnknownPersonInputField(sig.getVorNachnameJahr()) :
                                ((FahrtenabzeichenRecord)Daten.project.getFahrtenabzeichen(false).createNewRecord()).getUnknownPersonInputField(sig.getVorNachnameJahr()));
                        if (SimpleInputDialog.showInputDialog(Dialog.frameCurrent(), 
                                International.onlyFor("Person nicht gefunden", "de"), item)) {
                            UUID id = (UUID)item.getId(item.getValueFromField());
                            if (id != null) {
                                if (fa == null) {
                                    // first check whether this person might already have a fahrtenabzeichen record
                                    fa = getFahrtenabzeichen(id);
                                    if (fa != null) {
                                        newRecord = false;
                                    }
                                }
                                if (fa != null) {
                                    fa.setPersonId(id);
                                } else {
                                    fa = Daten.project.getFahrtenabzeichen(false).createFahrtenabzeichenRecord(id);
                                }
                            } else {
                                fa = null; // do not import
                            }
                        } else {
                            fa = null; // do not import
                        }
                    } else {
                        fa = null; // do not import
                    }                        
                }
                
                if (fa != null) {
                    fa.setFahrtenheft(sig.toString());
                    fa.setAbzeichen(sig.getAnzAbzeichen());
                    fa.setKilometer(sig.getGesKm());
                    fa.setAbzeichenAB(sig.getAnzAbzeichenAB());
                    fa.setKilometerAB(sig.getGesKmAB());
                    if (newRecord) {
                        data().add(fa);
                    } else {
                        data().update(fa);
                    }
                    importedCnt++;
                } else {
                    nichtImportiert = (nichtImportiert != null ? nichtImportiert + "\n" : "") +
                            sig.getVorname() + " " + sig.getNachname();
                    continue;
                }
                sig.checkSignature();
                switch (sig.getSignatureState()) {
                    case DRVSignatur.SIG_VALID:
                        // nothing to do
                        break;
                    case DRVSignatur.SIG_UNKNOWN_KEY:
                        sigError++;
                        sigErrorUnknownKey++;
                        keyname = sig.getKeyName();
                        break;
                    default:
                        sigError++;
                        break;
                }
            }
            if (nichtImportiert != null) {
                Dialog.error("Folgende Fahrtenhefte wurden nicht importiert,\n" +
                             "da keine entsprechenden Mitglieder in der Personenliste gefunden wurden:\n" +
                             nichtImportiert);
            }
            String sigGueltigInfo;
            if (sigError == 0) {
                sigGueltigInfo = "alle Fahrtenhefte sind gültig";
            } else {
                sigGueltigInfo = sigErrorUnknownKey + " Fahrtenhefte konnten nicht geprüft werden, da der Schlüssel unbekannt ist";
                if (sigError > sigErrorUnknownKey) {
                    sigGueltigInfo += "\n" + sigError + " Fahrtenhefte sind ungültig";
                }
            }
            meldungEingespielteFahrtenhefte += (meldungEingespielteFahrtenhefte.length() > 0 ? "\n" : "")
                    + "Quittungsnummer " + sigfile.quittungsnr + ": " + importedCnt + " signierte Fahrtenhefte eingespielt (" + sigGueltigInfo + ").";
            if (sigErrorUnknownKey > 0) {
                downloadKey(keyname);
            }
        } catch (Exception e) {
            Dialog.error("Fehler beim Lesen der Bestätigungsdatei: " + e.toString());
            return false;
        }
        return true;
    }
}
