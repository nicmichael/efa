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
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.efa1.*;
import de.nmichael.efa.util.*;
import java.util.*;

public class ImportCrews extends ImportBase {

    private ImportMetadata meta;
    private String efa1fname;

    public ImportCrews(ImportTask task, String efa1fname, ImportMetadata meta) {
        super(task);
        this.meta = meta;
        this.efa1fname = efa1fname;
    }

    public String getDescription() {
        return International.getString("Mannschaften");
    }

    public boolean runImport() {
        try {
            Mannschaften mannschaften = new Mannschaften(efa1fname);
            mannschaften.dontEverWrite();
            logInfo(International.getMessage("Importiere {list} aus {file} ...", getDescription(), efa1fname));
            if (!mannschaften.readFile()) {
                logError(LogString.fileOpenFailed(efa1fname, getDescription()));
                return false;
            }

            Crews crews = Daten.project.getCrews(true);
            Boats boats = Daten.project.getBoats(false); // must be imported first!
            Persons persons = Daten.project.getPersons(false); // must be imported first!
            Destinations destinations = Daten.project.getDestinations(false); // must be imported first!

            String[] IDXP = PersonRecord.IDX_NAME_NAMEAFFIX;
            String[] IDXB = BoatRecord.IDX_NAME_NAMEAFFIX;
            String[] IDXD = DestinationRecord.IDX_NAME;
            
            DatenFelder d = mannschaften.getCompleteFirst();
            while (d != null) {
                // Crews are unique, and not versionized in efa1. Therefore, we just import them into an empty crew list.
                // We don't need to check whether this crew already exists (it can't exist).

                // create new CrewRecord
                CrewRecord r = crews.createCrewRecord(UUID.randomUUID());
                r.setName(d.get(Mannschaften.BOOT));
                UUID id;
                id = findPerson(persons, IDXP, d.get(Mannschaften.STM), true, -1);
                if (id != null) {
                    r.setCoxId(id);
                }
                for (int i=0; i<Fahrtenbuch.ANZ_MANNSCH; i++) {
                    id = findPerson(persons, IDXP, d.get(Mannschaften.MANNSCH1 + i), true, -1);
                    if (id != null) {
                        r.setCrewId(i+1, id);
                    }
                }
                int pos = EfaUtil.string2int(d.get(Mannschaften.OBMANN), -1);
                if (pos >= 0) {
                    r.setBoatCaptainPosition(pos);
                }
                try {
                    crews.data().add(r);
                    logDetail(International.getMessage("Importiere Eintrag: {entry}", r.toString()));
                } catch(Exception e) {
                    logError(International.getMessage("Import von Eintrag fehlgeschlagen: {entry} ({error})", r.toString(), e.toString()));
                    Logger.logdebug(e);
                }

                // update BoatRecord
                String boatName = Mitglieder.getName(d.get(Mannschaften.BOOT));
                String clubName = Mitglieder.getVerein(d.get(Mannschaften.BOOT));
                try {
                    DataKey[] keys = boats.data().getByFields(IDXB,
                            new String[]{
                                (boatName.length() > 0 ? boatName : null),
                                (clubName.length() > 0 ? clubName : null)});
                    for (int i=0; keys != null && i<keys.length; i++) {
                        BoatRecord b = (BoatRecord)boats.data().get(keys[i]);
                        if (b != null) {
                            b.setDefaultCrewId(r.getId());
                            if (d.get(Mannschaften.FAHRTART).length() > 0) {
                                b.setDefaultSessionType(d.get(Mannschaften.FAHRTART));
                            }
                            id = findDestination(destinations, IDXD, d.get(Mannschaften.ZIEL), true, -1);
                            if (id != null) {
                                b.setDefaultDestinationId(id);
                            }
                            boats.data().update(b);
                        }
                    }
                } catch (Exception e) {
                    Logger.logdebug(e);
                }

                d = mannschaften.getCompleteNext();
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
