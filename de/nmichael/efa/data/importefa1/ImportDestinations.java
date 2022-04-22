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

import de.nmichael.efa.data.efawett.ZielfahrtFolge;
import de.nmichael.efa.Daten;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.efa1.*;
import de.nmichael.efa.util.*;
import java.util.*;

public class ImportDestinations extends ImportBase {

    private Ziele ziele;
    private ProjectRecord logbookRec;

    public ImportDestinations(ImportTask task, Ziele ziele, ProjectRecord logbookRec) {
        super(task);
        this.ziele = ziele;
        this.logbookRec = logbookRec;
    }

    public String getDescription() {
        return International.getString("Ziele");
    }

    private boolean isIdentical(Object o, String s) {
        if (o == null && (s == null || s.length() == 0)) {
            return true;
        }
        if (o == null || s == null) {
            return false;
        }
        return (o.toString().equals(s));
    }

    private String getZielbereiche(DatenFelder d) {
        return EfaUtil.replace(d.get(Ziele.BEREICH), "/", ";", true);
    }

    private boolean isChanged(DestinationRecord r, DatenFelder d) {
        if (!isIdentical(r.getName(), EfaUtil.replace(d.get(Ziele.NAME),"+","&",true))) {
            return true;
        }
        if (!isIdentical(Long.toString(r.getDistance().getValueInMeters()), Integer.toString(EfaUtil.zehntelString2Int(d.get(Ziele.KM))*100))) {
            return true;
        }
        if (!isIdentical((r.getDestinationAreas() == null ? "" : r.getDestinationAreas().toString()), getZielbereiche(d))) {
            return true;
        }
        if (!isIdentical(Boolean.toString(r.getStartIsBoathouse()), Boolean.toString(d.get(Ziele.STEGZIEL).equals("+")))) {
            return true;
        }
        // Well, we skip the comparison for Waters. Actually, we should check this, but I currently just don't feel like implementing this ;-)
        return false;
    }

    public boolean runImport() {
        try {
            logInfo(International.getMessage("Importiere {list} aus {file} ...", getDescription(), ziele.getFileName()));

            Destinations destinations = Daten.project.getDestinations(true);
            Waters waters = Daten.project.getWaters(true);
            long validFrom = logbookRec.getStartDate().getTimestamp(null);

            Hashtable<UUID,String> importedDestinations = new Hashtable<UUID,String>();
            DatenFelder d = ziele.getCompleteFirst();
            String[] IDXD = new String[] { DestinationRecord.NAME };
            String[] IDXW = new String[] { WatersRecord.NAME };
            while (d != null) {
                // First search, whether we have imported this destination already
                DestinationRecord r = null;
                String destinationName = d.get(Ziele.NAME);
                String mainDestinationName = task.synZiele_getMainName(destinationName);
                DataKey k = null;
                DataKey[] keys = destinations.data().getByFields(IDXD,
                        new String[] { destinationName });
                if (keys != null && keys.length > 0) {
                    // We've found one or more destinations with same Name.
                    // It can happen that the same record has existed before, but became invalid in the meantime.
                    // In this case, even if names are identical, we create a new record. Over time, we may have
                    // multiple keys with different IDs for (different) records with the same name. Therefore,
                    // we go through all of them and hope to find at least one which is valid in the scope of
                    // the current logbook.
                    for (int i=0; i<keys.length && k == null; i++) {
                        k = (destinations.data().getValidAt(keys[i], validFrom) != null ? keys[i] : null);
                    }
                } else {
                    // we have not found a person by this name that we imported already.
                    // it could be, that there is a synonym, so look up this persons's main name
                    // if it is different.
                    // we don't replace a person's name with the synonym automatically, since we
                    // want to preserve the original name (and used it to created multiple versionized
                    // records). Other than with boats, where synonyms are used for kombiboote.
                    k = task.synZiele_getKeyForMainName(mainDestinationName);
                }
                if (k != null) {
                    r = (DestinationRecord)destinations.data().getValidAt(k, validFrom);
                }

                if (r == null || isChanged(r, d)) {
                    boolean newRecord = (r == null);
                    r = destinations.createDestinationRecord((r != null ? r.getId() : UUID.randomUUID()));
                    r.setName(EfaUtil.replace(d.get(Ziele.NAME),"+","&",true));
                    r.setDistance(DataTypeDistance.parseDistance(d.get(Ziele.KM) + DataTypeDistance.KILOMETERS, false));
                    if (d.get(Ziele.BEREICH).length() > 0) {
                        r.setDestinationAreas(new ZielfahrtFolge(getZielbereiche(d)));
                    }
                    if (d.get(Ziele.STEGZIEL).equals("+")) {
                        r.setStartIsBoathouse(true);
                        r.setRoundtrip(true); // we only assume destinations where "Start und Ziel ist Bootshaus" to be roundtrips (we don't have this info in efa1, so we just guess)
                    }                    
                    if (d.get(Ziele.GEWAESSER).length() > 0) {
                        String[] a = EfaUtil.kommaList2Arr(d.get(Ziele.GEWAESSER),',');
                        DataTypeList<UUID> watersList = new DataTypeList<UUID>();
                        for (int i=0; i<a.length; i++) {
                            String name = a[i].trim();
                            if (name.length() == 0) {
                                continue;
                            }
                            WatersRecord w = null;
                            DataKey[] wkeys = waters.data().getByFields(IDXW,
                                              new String[] { name });
                            if (wkeys != null && wkeys.length > 0) {
                                w = (WatersRecord)waters.data().get(wkeys[0]);
                            }
                            if (w == null || !name.equals(w.getName())) {
                                w = waters.createWatersRecord((w != null ? w.getId() : UUID.randomUUID()));
                                w.setName(name);
                                waters.data().add(w);
                                logDetail(International.getMessage("Importiere Eintrag: {entry}", w.toString()));
                            }
                            watersList.add(w.getId());
                        }
                        if (watersList.length() > 0) {
                            r.setWatersIdList(watersList);
                        }
                    }
                    try {
                        destinations.data().addValidAt(r, validFrom);
                        task.synZiele_setKeyForMainName(mainDestinationName, r.getKey());
                        logDetail(International.getMessage("Importiere Eintrag: {entry}", r.toString()));
                    } catch(Exception e) {
                        if (newRecord) {
                            logError(International.getMessage("Import von Eintrag fehlgeschlagen: {entry} ({error})", r.toString(), e.toString()));
                        } else {
                            logWarning(International.getMessage("Änderung eines existierenden Eintrags fehlgeschlagen: {entry} ({error})", r.toString(), e.toString()));
                        }
                        Logger.logdebug(e);
                    }
                } else {
                    logDetail(International.getMessage("Identischer Eintrag: {entry}", r.toString()));
                }
                importedDestinations.put(r.getId(), r.getQualifiedName());
                d = ziele.getCompleteNext();
            }

            // mark all destinations that have *not* been imported with this run, but still have a valid version, as deleted
            DataKeyIterator it = destinations.data().getStaticIterator();
            DataKey key = it.getFirst();
            while (key != null) {
                DestinationRecord dr = destinations.getDestination((UUID)key.getKeyPart1(), validFrom);
                if (dr != null && importedDestinations.get(dr.getId()) == null) {
                    try {
                        destinations.data().changeValidity(dr, dr.getValidFrom(), validFrom);
                    } catch(Exception e) {
                        logError(International.getMessage("Gültigkeit ändern von Eintrag fehlgeschlagen: {entry} ({error})", dr.toString(), e.toString()));
                        Logger.logdebug(e);
                    }
                }
                key = it.getNext();
            }
        } catch(Exception e) {
            logError(International.getMessage("Import von {list} aus {file} ist fehlgeschlagen.", getDescription(), ziele.getFileName()));
            logError(e.toString());
            Logger.logdebug(e);
            return false;
        }
        return true;
    }

}
