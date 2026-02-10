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

import de.nmichael.efa.efa1.Mehrtagesfahrt;
import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.efa1.*;
import de.nmichael.efa.util.*;
import java.util.*;

public class ImportLogbook extends ImportBase {

    private ImportMetadata meta;
    private String efa1fname;

    private Logbook logbook;
    private Boats boats;
    private Persons persons;
    private Destinations destinations;
    private SessionGroups sessionGroups;
    private String[] boatIdx = BoatRecord.IDX_NAME_NAMEAFFIX;
    private String[] personIdx = PersonRecord.IDX_NAME_NAMEAFFIX;
    private String[] destinationIdx = DestinationRecord.IDX_NAME;
    
    public ImportLogbook(ImportTask task, String efa1fname, ImportMetadata meta) {
        super(task);
        this.meta = meta;
        this.efa1fname = efa1fname;
    }

    public String getDescription() {
        return International.getString("Fahrtenbuch");
    }

    public boolean runImport() {
        // Fahrtenbuch.fahrtenbuch replaces old Daten.fahrtenbuch
        // We need this static reference for some of the zerlegeNamen(...) methods!
        Fahrtenbuch origFahrtenbuch = Fahrtenbuch.fahrtenbuch;
        try {
            Fahrtenbuch fahrtenbuch = new Fahrtenbuch(efa1fname);
            fahrtenbuch.dontEverWrite();
            Fahrtenbuch.fahrtenbuch = fahrtenbuch;
            logInfo(International.getMessage("Importiere {list} aus {file} ...", getDescription(), efa1fname));
            if (!fahrtenbuch.readFile()) {
                logError(LogString.fileOpenFailed(efa1fname, getDescription()));
                return false;
            }

            ProjectRecord logbookRec = Daten.project.createNewLogbookRecord(meta.name);
            logbookRec.setDescription(meta.description);
            logbookRec.setStartDate(meta.firstDate);
            logbookRec.setEndDate(meta.lastDate);
            Daten.project.addLogbookRecord(logbookRec);
            long validAt = logbookRec.getStartDate().getTimestamp(null);
            ImportBoats boatsImport = new ImportBoats(task, fahrtenbuch.getDaten().boote, logbookRec);
            if (!boatsImport.runImport()) {
                logError(International.getMessage("Import von {list} aus {file} ist fehlgeschlagen.", boatsImport.getDescription(), fahrtenbuch.getDaten().bootDatei));
                logError(International.getMessage("Import von {list} aus {file} wird abgebrochen.", getDescription(), efa1fname));
                return false;
            }
            cntWarning += boatsImport.cntWarning;
            cntError += boatsImport.cntError;
            

            ImportPersons personsImport = new ImportPersons(task, fahrtenbuch.getDaten().mitglieder, logbookRec);
            if (!personsImport.runImport()) {
                logError(International.getMessage("Import von {list} aus {file} ist fehlgeschlagen.", personsImport.getDescription(), fahrtenbuch.getDaten().mitgliederDatei));
                logError(International.getMessage("Import von {list} aus {file} wird abgebrochen.", getDescription(), efa1fname));
                return false;
            }
            cntWarning += personsImport.cntWarning;
            cntError += personsImport.cntError;

            ImportDestinations destinationsImport = new ImportDestinations(task, fahrtenbuch.getDaten().ziele, logbookRec);
            if (!destinationsImport.runImport()) {
                logError(International.getMessage("Import von {list} aus {file} ist fehlgeschlagen.", destinationsImport.getDescription(), fahrtenbuch.getDaten().zieleDatei));
                logError(International.getMessage("Import von {list} aus {file} wird abgebrochen.", getDescription(), efa1fname));
                return false;
            }
            cntWarning += destinationsImport.cntWarning;
            cntError += destinationsImport.cntError;

            logbook = Daten.project.getLogbook(meta.name, true);
            sessionGroups = Daten.project.getSessionGroups(true);
            boats = Daten.project.getBoats(false);
            persons = Daten.project.getPersons(false);
            destinations = Daten.project.getDestinations(false);

            Hashtable<String,UUID> sessionGroupMapping = new Hashtable<String,UUID>();

            DatenFelder d = fahrtenbuch.getCompleteFirst();
            while (d != null) {
                LogbookRecord r = logbook.createLogbookRecord(DataTypeIntString.parseString(d.get(Fahrtenbuch.LFDNR)));
                r.setDate(DataTypeDate.parseDate(d.get(Fahrtenbuch.DATUM)));

                if (r.getDate().isBefore(meta.firstDate) ||
                    r.getDate().isAfter(meta.lastDate)) {
                    logWarning(International.getMessage("Eintrag {entry} wurde nicht importiert, da sein Datum {date} außerhalb des festgelegten Zeitraums ({fromDate} - {toDate}) liegt.",
                            r.getEntryId().toString(),r.getDate().toString(),meta.firstDate.toString(),meta.lastDate.toString()));
                }

                if (d.get(Fahrtenbuch.BOOT).length() > 0) {
                    String b = task.synBoote_getMainName(d.get(Fahrtenbuch.BOOT));
                    UUID id = findBoat(boats, boatIdx, b, false, validAt);
                    if (id != null) {
                        r.setBoatId(id);
                        BoatRecord boat = boats.getBoat(id, validAt);
                        if (boat != null) {
                            int numberOfVariants = boat.getNumberOfVariants();
                            if (numberOfVariants == 1) {
                                r.setBoatVariant(boat.getTypeVariant(0));
                            } else {
                                for (int i = 0; i < numberOfVariants; i++) {
                                    String description = boat.getTypeDescription(i);
                                    if (description != null && description.equals(d.get(Fahrtenbuch.BOOT))) {
                                        r.setBoatVariant(boat.getTypeVariant(i));
                                        break;
                                    }
                                }

                            }
                        }
                    } else {
                        r.setBoatName(b);
                    }
                }
                if (d.get(Fahrtenbuch.STM).length() > 0) {
                    UUID id = findPerson(persons, personIdx, d.get(Fahrtenbuch.STM), false, validAt);
                    if (id == null) {
                        // it coule be that a person's name has changed during a year, and old
                        // logbook entries still have the old name of the person;
                        // If there is a synonym, we use that. If not, we treat this as a unknown name.
                        // We could alternatively also lookup this person based on another validAt,
                        // but that would actually be incorrect in terms of efa2 (and also efa1 -
                        // in that case the efa1 data would be wrong).
                        id = findPerson(persons, personIdx,
                                task.synMitglieder_getMainName(d.get(Fahrtenbuch.STM)), false, validAt);
                    }
                    if (id != null) {
                        r.setCoxId(id);
                    } else {
                        r.setCoxName(d.get(Fahrtenbuch.STM));
                    }
                }
                for (int i=0; i<Fahrtenbuch.ANZ_MANNSCH; i++) {
                    if (d.get(Fahrtenbuch.MANNSCH1 + i).length() > 0) {
                        UUID id = findPerson(persons, personIdx, d.get(Fahrtenbuch.MANNSCH1 + i), false, validAt);
                        if (id == null) {
                            // see comments above
                            id = findPerson(persons, personIdx,
                                    task.synMitglieder_getMainName(d.get(Fahrtenbuch.MANNSCH1 + i)), false, validAt);
                        }
                        if (id != null) {
                            r.setCrewId(i+1, id);
                        } else {
                            r.setCrewName(i+1, d.get(Fahrtenbuch.MANNSCH1 + i));
                        }
                    }
                }
                r.setBoatCaptainPosition(EfaUtil.string2int(d.get(Fahrtenbuch.OBMANN),-1));
                r.setStartTime(DataTypeTime.parseTime(d.get(Fahrtenbuch.ABFAHRT)));
                r.setEndTime(DataTypeTime.parseTime(d.get(Fahrtenbuch.ANKUNFT)));
                if (d.get(Fahrtenbuch.ZIEL).length() > 0) {
                    UUID id = findDestination(destinations, destinationIdx, d.get(Fahrtenbuch.ZIEL), false, validAt);
                    if (id == null) {
                        // see comments above for persons on why we do it this way
                        id = findDestination(destinations, destinationIdx,
                                task.synZiele_getMainName(d.get(Fahrtenbuch.ZIEL)), false, validAt);
                    }
                    if (id != null) {
                        r.setDestinationId(id);
                    } else {
                        // since we replace "+" by "&" in the import of destinations, we need to check whether a destination
                        // with a "&" probably exists; if so, we use that one
                        id = findDestination(destinations, destinationIdx, EfaUtil.replace(d.get(Fahrtenbuch.ZIEL), "+", "&", true), false, validAt);
                        if (id != null) {
                            r.setDestinationId(id);
                        } else {
                            r.setDestinationName(d.get(Fahrtenbuch.ZIEL));
                        }
                    }
                }
                
                r.setDistance(DataTypeDistance.parseDistance(d.get(Fahrtenbuch.BOOTSKM) + DataTypeDistance.KILOMETERS, false));
                if (d.get(Fahrtenbuch.BEMERK).length() > 0) {
                    r.setComments(d.get(Fahrtenbuch.BEMERK));
                }

                if (d.get(Fahrtenbuch.FAHRTART).length() > 0) {
                    String fahrtArt = d.get(Fahrtenbuch.FAHRTART).trim();
                    String mtourName = null;
                    Mehrtagesfahrt mtour = null;

                    // in efa 1.8.3, a MultiDay tour is a tour with SessionType "NameOfTour" (*not* prefixed by TYPE_SESSION_TOUR_EFA1X1)
                    // in efa 1.9.0, a MultiDay tour is a tour with SessionType "TYPE_SESSION_TOUR_EFA1X2:NameOfTour"
                    if (fahrtArt.startsWith(EfaTypes.TYPE_SESSION_TOUR+":") || // this line is bullshit, but we keep it...
                        fahrtArt.startsWith(EfaTypes.TYPE_SESSION_TOUR_EFA1X1+":") || // this line is bullshit, but we keep it...
                        fahrtArt.startsWith(EfaTypes.TYPE_SESSION_TOUR_EFA1X2+":") ||
                        // the next line is for efa 1.8.3: If this is a unknown session type, we treat it as a MTour
                        !Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_SESSION, fahrtArt)) {
                        mtourName = Fahrtenbuch.getMehrtagesfahrtName(fahrtArt);
                        mtour = (mtourName != null && mtourName.length() > 0 ? fahrtenbuch.getMehrtagesfahrt(mtourName) : null);
                        fahrtArt = EfaTypes.TYPE_SESSION_TOUR;
                    }
                    if (Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_SESSION, fahrtArt)) {
                        r.setSessionType(fahrtArt);
                        if (mtour != null) {
                            // if all in one entry: update fields in LogbookRecord
                            if (!mtour.isEtappen) {
                                if (mtour.start != null && mtour.start.length() > 0) {
                                    r.setDate(DataTypeDate.parseDate(mtour.start));
                                }
                                if (mtour.ende != null && mtour.ende.length() > 0) {
                                    r.setEndDate(DataTypeDate.parseDate(mtour.ende));
                                }
                                if (r.getDate() != null && r.getDate().isSet() &&
                                    r.getEndDate() != null && r.getEndDate().isSet() &&
                                    r.getDate().isAfterOrEqual(r.getEndDate())) {
                                    r.setEndDate(null);
                                }
                            }
                            // set/update SessionGroup
                            UUID id = sessionGroupMapping.get(mtourName);
                            if (id == null) {
                                SessionGroupRecord sg = sessionGroups.createSessionGroupRecord(UUID.randomUUID(), meta.name);
                                id = sg.getId();
                                sg.setName(mtourName);
                                if (mtour.start != null && mtour.start.length() > 0) {
                                    sg.setStartDate(DataTypeDate.parseDate(mtour.start));
                                }
                                if (mtour.ende != null && mtour.ende.length() > 0) {
                                    sg.setEndDate(DataTypeDate.parseDate(mtour.ende));
                                }
                                if (mtour.rudertage > 0) {
                                    sg.setActiveDays(mtour.rudertage);
                                }
                                // Waters from MultiDayTour's are not imported, but get lost during import.
                                // Should this be fixed? It's only relevant for DRV-Wanderruderstatistik, so it only
                                // matters for years to come, not for any logbooks in the past.
                                // Currently, Waters are only stored in Destinations, but not in SessionGroups, which
                                // is better from a modeling perspective, but incompatible to efa1.
                                try {
                                    sessionGroups.data().add(sg);
                                    sessionGroupMapping.put(mtourName, id);
                                } catch(Exception e) {
                                    logError(International.getMessage("Import von Eintrag fehlgeschlagen: {entry} ({error})", sg.toString(), e.toString()));
                                    Logger.logdebug(e);
                                }
                            }
                            r.setSessionGroupId(id);
                        }
                    } else {
                        r.setSessionType(EfaTypes.TYPE_SESSION_NORMAL);
                    }
                } else {
                    r.setSessionType(EfaTypes.TYPE_SESSION_NORMAL);
                }

                try {
                    logbook.data().add(r);
                    logDetail(International.getMessage("Importiere Eintrag: {entry}", r.toString()));
                } catch(Exception e) {
                    logError(International.getMessage("Import von Eintrag fehlgeschlagen: {entry} ({error})", r.toString(), e.toString()));
                    Logger.logdebug(e);
                }
                d = fahrtenbuch.getCompleteNext();
            }
            logbook.close();
        } catch(Exception e) {
            logError(International.getMessage("Import von {list} aus {file} ist fehlgeschlagen.", getDescription(), efa1fname));
            logError(e.toString());
            Logger.logdebug(e);
            return false;
        } finally {
            Fahrtenbuch.fahrtenbuch = origFahrtenbuch;
        }
        return true;
    }

}
