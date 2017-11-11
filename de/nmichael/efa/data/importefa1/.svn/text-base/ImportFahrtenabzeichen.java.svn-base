/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.importefa1;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.EfaKeyStore;
import de.nmichael.efa.data.*;
//import de.nmichael.efa.efa1.*;
import de.nmichael.efa.data.efawett.DRVSignatur;
//import de.nmichael.efa.efa1.*;
import de.nmichael.efa.util.*;
import java.util.*;

public class ImportFahrtenabzeichen extends ImportBase {

    private ImportMetadata meta;
    private String efa1fname;

    public ImportFahrtenabzeichen(ImportTask task, String efa1fname, ImportMetadata meta) {
        super(task);
        this.meta = meta;
        this.efa1fname = efa1fname;
    }

    public String getDescription() {
        return International.onlyFor("Fahrtenabzeichen","de");
    }

    public boolean runImport() {
        try {
            de.nmichael.efa.efa1.Fahrtenabzeichen fahrtenabzeichen1 = new de.nmichael.efa.efa1.Fahrtenabzeichen(efa1fname);
            fahrtenabzeichen1.dontEverWrite();
            logInfo(International.getMessage("Importiere {list} aus {file} ...", getDescription(), efa1fname));
            if (!fahrtenabzeichen1.readFile()) {
                logError(LogString.fileOpenFailed(efa1fname, getDescription()));
                return false;
            }

            ImportMetadata keyStoreMetadata = task.getKeyStoreMetadata();
            EfaKeyStore efa1KeyStore = null;
            if (keyStoreMetadata != null && keyStoreMetadata.filename != null) {
                efa1KeyStore = new EfaKeyStore(keyStoreMetadata.filename, "efa".toCharArray());
            }

            Fahrtenabzeichen fahrtenabzeichen = Daten.project.getFahrtenabzeichen(true);
            Persons persons = Daten.project.getPersons(false); // must be imported first!

            String[] IDXP = PersonRecord.IDX_NAME_NAMEAFFIX;

            de.nmichael.efa.efa1.DatenFelder d = fahrtenabzeichen1.getCompleteFirst();
            while (d != null) {
                UUID personID = findPerson(persons, IDXP,
                        d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.VORNAME) + " " + d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.NACHNAME),
                        "", true, -1);
                if (personID != null) {
                    // create new FahrtenabzeichenRecord
                    FahrtenabzeichenRecord r = fahrtenabzeichen.createFahrtenabzeichenRecord(personID);
                    try {
                        if (d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.ANZABZEICHEN).length() > 0) {
                            r.setAbzeichen(EfaUtil.string2int(d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.ANZABZEICHEN), 0));
                        }
                        if (d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.ANZABZEICHENAB).length() > 0) {
                            r.setAbzeichenAB(EfaUtil.string2int(d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.ANZABZEICHENAB), 0));
                        }
                        if (d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.GESKM).length() > 0) {
                            r.setKilometer(EfaUtil.string2int(d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.GESKM), 0));
                        }
                        if (d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.GESKMAB).length() > 0) {
                            r.setKilometerAB(EfaUtil.string2int(d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.GESKMAB), 0));
                        }
                        if (d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.LETZTEMELDUNG).length() > 0) {
                            r.setFahrtenheft(d.get(de.nmichael.efa.efa1.Fahrtenabzeichen.LETZTEMELDUNG));
                        }
                        fahrtenabzeichen.data().add(r);
                        logDetail(International.getMessage("Importiere Eintrag: {entry}", r.toString()));
                        
                        String key = (r.getDRVSignatur() != null ?
                            DRVSignatur.getKeyName(r.getDRVSignatur().getKeyNr()) : null);
                        try {
                            if (key != null && efa1KeyStore != null) {
                                if (Daten.keyStore.getPublicKey(key) == null) {
                                    Daten.keyStore.addCertificate(key, efa1KeyStore.getCertificate(key));
                                    logDetail(International.getMessage("Importiere Eintrag: {entry}", "DRV-Schlüssel " + key));
                                }
                            }
                        } catch(Exception ekey) {
                            logError(International.getMessage("Import von Eintrag fehlgeschlagen: {entry} ({error})", "DRV-Schlüssel " + key, ekey.toString()));
                        }
                    } catch(Exception e) {
                        logError(International.getMessage("Import von Eintrag fehlgeschlagen: {entry} ({error})", r.toString(), e.toString()));
                        Logger.logdebug(e);
                    }
                }
                d = fahrtenabzeichen1.getCompleteNext();
            }
        } catch(Exception e) {
            logError(International.getMessage("Import von {list} aus {file} ist fehlgeschlagen.", getDescription(), efa1fname));
            logError(e.toString());
            Logger.logdebug(e);
            return false;
        }
        return true;
    }

}
