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
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.efa1.*;
import de.nmichael.efa.util.*;
import java.util.*;

public class ImportGroups extends ImportBase {

    private ImportMetadata meta;
    private String efa1fname;

    public ImportGroups(ImportTask task, String efa1fname, ImportMetadata meta) {
        super(task);
        this.meta = meta;
        this.efa1fname = efa1fname;
    }

    public String getDescription() {
        return International.getString("Gruppen");
    }

    public boolean runImport() {
        try {
            Gruppen gruppen = new Gruppen(efa1fname);
            gruppen.dontEverWrite();
            logInfo(International.getMessage("Importiere {list} aus {file} ...", getDescription(), efa1fname));
            if (!gruppen.readFile()) {
                logError(LogString.fileOpenFailed(efa1fname, getDescription()));
                return false;
            }

            Groups groups = Daten.project.getGroups(true);
            Persons persons = Daten.project.getPersons(false); // must be imported first!

            String[] IDXP = new String[] { PersonRecord.FIRSTNAME, PersonRecord.LASTNAME, PersonRecord.ASSOCIATION };
            Hashtable<String,UUID> groupMapping = new Hashtable<String,UUID>();
            Vector<String> d = gruppen.getGruppen();
            for (String g : d) {
                // Groups are unique, and not versionized in efa1. Therefore, we just import them into an empty group list.
                // We don't need to check whether this group already exists (it can't exist).
                GroupRecord r = groups.createGroupRecord(UUID.randomUUID());
                DataTypeList<UUID> list = new DataTypeList<UUID>();
                r.setName(g);
                Vector<GruppenMitglied> gm = gruppen.getGruppenMitglieder(g);
                for (GruppenMitglied m : gm) {
                    DataKey[] keys = persons.data().getByFields(IDXP,
                            new String[] {
                                        (m.vorname.length() > 0 ? m.vorname : null),
                                        (m.nachname.length() > 0 ? m.nachname : null),
                                        (m.verein.length() > 0 ? m.verein : null) });
                    PersonRecord pr = (keys != null && keys.length > 0 ? (PersonRecord)persons.data().get(keys[0]) : null);
                    if (pr != null) {
                        list.add(pr.getId());
                    } else {
                        logWarning(International.getMessage("{type_of_entry} {entry} nicht in {list} gefunden.",
                            International.getString("Person"),
                            m.toString(),
                            International.getString("Mitgliederliste")));
                    }
                }
                r.setMemberIdList(list);
                try {
                    groups.data().add(r);
                    groupMapping.put(r.getName(), r.getId());
                    logDetail(International.getMessage("Importiere Eintrag: {entry}", r.toString()));
                } catch(Exception e) {
                    logError(International.getMessage("Import von Eintrag fehlgeschlagen: {entry} ({error})", r.toString(), e.toString()));
                    Logger.logdebug(e);
                }
            }
            task.setGroupMapping(groupMapping);
        } catch(Exception e) {
            logError(International.getMessage("Import von {list} aus {file} ist fehlgeschlagen.", getDescription(), efa1fname));
            logError(e.toString());
            Logger.logdebug(e);
            return false;
        }
        return true;
    }

}
