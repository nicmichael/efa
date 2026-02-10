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

import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.util.*;
import java.util.*;


public abstract class ImportBase {

    protected ImportTask task;
    int cntWarning = 0;
    int cntError = 0;

    public ImportBase(ImportTask task) {
        this.task = task;
    }

    public abstract String getDescription();
    public abstract boolean runImport();

    protected void logInfo(String s) {
        task.logInfo("INFO    - " + getDescription()+ " - " + s + "\n", true, true);
    }

    protected void logDetail(String s) {
        task.logInfo("DETAIL  - " + getDescription()+ " - " + s + "\n", false, true);
    }

    protected void logWarning(String s) {
        task.logInfo("WARNING - " + getDescription()+ " - " + s + "\n", true, true);
        cntWarning++;
    }

    protected void logError(String s) {
        task.logInfo("ERROR   - " + getDescription()+ " - " + s + "\n", true, true);
        cntError++;
    }

    public int getWarningCount() {
        return cntWarning;
    }

    public int getErrorCount() {
        return cntError;
    }

    protected UUID findPerson(Persons persons, String[] IDX, String name, boolean warnIfNotFound, long validAt) {
        name = name.trim();
        if (name.length() == 0) {
            return null;
        }
        String[] qname = persons.staticPersonRecord.getQualifiedNameValues(name);  //PersonRecord.tryGetNameAndAffix(name);
        return findPerson(persons, IDX, qname[0], qname[1], warnIfNotFound, validAt);
    }

    protected UUID findPerson(Persons persons, String[] IDX, String name, String affix, boolean warnIfNotFound, long validAt) {
        try {
            DataKey[] keys = persons.data().getByFields(IDX,
                    new String[]{
                        (name != null && name.length() > 0 ? name : null),
                        (affix != null && affix.length() > 0 ? affix : null)});
            if (keys != null && keys.length > 0) {
                for (int i=0; i<keys.length; i++) {
                    PersonRecord r = (PersonRecord)persons.data().get(keys[i]);
                    if (r != null && r.isValidAt(validAt) &&
                        (affix != null || r.getNameAffix() == null || r.getNameAffix().length() == 0)) {
                        return (UUID) keys[i].getKeyPart1();
                    }
                }
                return null;
            }
        } catch(Exception e) {
        }
        if (warnIfNotFound) {
            logWarning(International.getMessage("{type_of_entry} {entry} nicht in {list} gefunden.",
                            International.getString("Person"),
                            name + (affix != null && affix.length() > 0 ? " ("+affix+")" : ""),
                            International.getString("Mitglieder")));
        }
        return null;
    }

    protected UUID findBoat(Boats boats, String[] IDX, String name, boolean warnIfNotFound, long validAt) {
        name = name.trim();
        if (name.length() == 0) {
            return null;
        }
        String[] qname = BoatRecord.tryGetNameAndAffix(name);
        return findBoat(boats, IDX, qname[0], qname[1], warnIfNotFound, validAt);
    }

    protected UUID findBoat(Boats boats, String[] IDX, String boatName, String nameAffix, boolean warnIfNotFound, long validAt) {
        try {
            DataKey[] keys = boats.data().getByFields(IDX,
                    new String[]{
                        (boatName != null && boatName.length() > 0 ? boatName : null),
                        (nameAffix != null && nameAffix.length() > 0 ? nameAffix : null)});
            if (keys != null && keys.length > 0) {
                for (int i=0; i<keys.length; i++) {
                    BoatRecord r = (BoatRecord)boats.data().get(keys[i]);
                    if (r != null && r.isValidAt(validAt) &&
                        (nameAffix != null || r.getNameAffix() == null || r.getNameAffix().length() == 0)) {
                        return (UUID) keys[i].getKeyPart1();
                    }
                }
                return null;
            }
        } catch(Exception e) {
        }
        if (warnIfNotFound) {
            logWarning(International.getMessage("{type_of_entry} {entry} nicht in {list} gefunden.",
                            International.getString("Boot"),
                            boatName + (nameAffix != null && nameAffix.length() > 0 ? " ("+nameAffix+")" : ""),
                            International.getString("Bootsliste")));
        }
        return null;
    }

    protected UUID findDestination(Destinations destinations, String[] IDX, String name, boolean warnIfNotFound, long validAt) {
        name = name.trim();
        if (name.length() == 0) {
            return null;
        }
        try {
            DataKey[] keys = destinations.data().getByFields(IDX,
                    new String[]{ name });
            if (keys != null && keys.length > 0) {
                for (int i=0; i<keys.length; i++) {
                    DataRecord r = destinations.data().get(keys[i]);
                    if (r != null && r.isValidAt(validAt)) {
                        return (UUID) keys[i].getKeyPart1();
                    }
                }
                return null;
            }
        } catch(Exception e) {
        }
        if (warnIfNotFound) {
            logWarning(International.getMessage("{type_of_entry} {entry} nicht in {list} gefunden.",
                            International.getString("Ziel"),
                            name,
                            International.getString("Zielliste")));
        }
        return null;
    }

}
