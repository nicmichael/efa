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
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.efa1.*;
import de.nmichael.efa.util.*;
import java.util.*;

public class ImportBoatStatus extends ImportBase {

    private ImportMetadata meta;
    private String efa1fname;

    public ImportBoatStatus(ImportTask task, String efa1fname, ImportMetadata meta) {
        super(task);
        this.meta = meta;
        this.efa1fname = efa1fname;
    }

    public String getDescription() {
        return International.getString("Bootsstatus");
    }

    public boolean runImport() {
        try {
            BootStatus bootStatus = new BootStatus(efa1fname);
            bootStatus.dontEverWrite();
            logInfo(International.getMessage("Importiere {list} aus {file} ...", getDescription(), efa1fname));
            if (!bootStatus.readFile()) {
                logError(LogString.fileOpenFailed(efa1fname, getDescription()));
                return false;
            }

            BoatStatus boatStatus = Daten.project.getBoatStatus(true);
            BoatReservations boatReservations = Daten.project.getBoatReservations(true);
            BoatDamages boatDamages = Daten.project.getBoatDamages(true);
            Boats boats = Daten.project.getBoats(false); // must be imported first!
            Persons persons = Daten.project.getPersons(false); // must be imported first!
            String[] IDXB = new String[] { BoatRecord.NAME, BoatRecord.OWNER };
            String[] IDXP = new String[] { PersonRecord.FIRSTNAME, PersonRecord.LASTNAME, PersonRecord.ASSOCIATION };

            DatenFelder d = bootStatus.getCompleteFirst();
            Hashtable<UUID,String> imported = new Hashtable<UUID,String>();
            while (d != null) {
                String b = task.synBoote_getMainName(d.get(BootStatus.NAME));
                UUID boatID = findBoat(boats, IDXB, b, true, -1);
                if (boatID != null && imported.get(boatID) == null) {
                    // normally the BoatStatusRecord should already have been created when the boat was created...
                    BoatStatusRecord rs = boatStatus.getBoatStatus(boatID);
                    boolean newRecord = false;
                    if (rs == null) {
                        // create new BoatStatusRecord
                        rs = boatStatus.createBoatStatusRecord(boatID, b);
                        newRecord = true;
                    }

                    if ((d.get(BootStatus.STATUS) != null && d.get(BootStatus.STATUS).equals(BootStatus.STAT_VORUEBERGEHEND_VERSTECKEN)) ||
                        (d.get(BootStatus.UNBEKANNTESBOOT) != null && d.get(BootStatus.UNBEKANNTESBOOT).equals("+"))) {
                        logError("Bootsstatus für Boot '"+d.get(BootStatus.NAME)+"' kann nicht importiert werden. Bitte beende die Fahrt in efa1 vor dem Import.");
                    }

                    try {
                        if (d.get(BootStatus.STATUS).length() > 0) {
                            String s = d.get(BootStatus.STATUS);
                            if (s.equals(BoatStatusRecord.STATUS_HIDE) ||
                                s.equals(BoatStatusRecord.STATUS_AVAILABLE) ||
                                s.equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
                                rs.setBaseStatus(s);
                            } else {
                                rs.setBaseStatus(BoatStatusRecord.STATUS_AVAILABLE);
                            }
                            if (s.equals(BoatStatusRecord.STATUS_HIDE) ||
                                s.equals(BoatStatusRecord.STATUS_AVAILABLE) ||
                                s.equals(BoatStatusRecord.STATUS_ONTHEWATER) ||
                                s.equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
                                rs.setCurrentStatus(s);
                            } else {
                                rs.setCurrentStatus(BoatStatusRecord.STATUS_AVAILABLE);
                            }
                        }
                        if (d.get(BootStatus.LFDNR).length() > 0) {
                            rs.setEntryNo(new DataTypeIntString(d.get(BootStatus.LFDNR)));
                            // Actually, we would have to set BootStatus.LOGBOOK as well.
                            // However, we don't know which the current logbook in efa1 is (unless we would read the efa1 config file as well).
                            // So we leave this empty. If logbook is null, efa boathouse will at runtime assume the then opened logbook as the one.
                        }
                        if (d.get(BootStatus.BEMERKUNG).length() > 0) {
                            rs.setComment(d.get(BootStatus.BEMERKUNG));
                        }
                        if (newRecord) {
                            boatStatus.data().add(rs);
                        } else {
                            boatStatus.data().update(rs);
                        }
                        imported.put(boatID, b); // to avoid importing duplicates because of synonyms/Kombiboote
                        logDetail(International.getMessage("Importiere Eintrag: {entry}", rs.toString()));
                    } catch(Exception e) {
                        logError(International.getMessage("Import von Eintrag fehlgeschlagen: {entry} ({error})", rs.toString(), e.toString()));
                        Logger.logdebug(e);
                    }

                    // BoatReservations
                    Vector<BoatReservation> reservierungen = BootStatus.getReservierungen(d);
                    for (int i=0; reservierungen != null && i<reservierungen.size(); i++) {
                        BoatReservationRecord rr = boatReservations.createBoatReservationsRecord(boatID, i+1);
                        BoatReservation r = reservierungen.get(i);
                        try {
                            if (r.isOneTimeReservation()) {
                                rr.setType(BoatReservationRecord.TYPE_ONETIME);
                                if (r.getDateFrom() != null && r.getDateFrom().length() > 0) {
                                    rr.setDateFrom(DataTypeDate.parseDate(r.getDateFrom()));
                                }
                                if (r.getDateTo() != null && r.getDateTo().length() > 0) {
                                    rr.setDateTo(DataTypeDate.parseDate(r.getDateTo()));
                                }
                                if (r.getTimeFrom() != null && r.getTimeFrom().length() > 0) {
                                    rr.setTimeFrom(DataTypeTime.parseTime(r.getTimeFrom()));
                                }
                                if (r.getTimeTo() != null && r.getTimeTo().length() > 0) {
                                    rr.setTimeTo(DataTypeTime.parseTime(r.getTimeTo()));
                                }
                            } else {
                                rr.setType(BoatReservationRecord.TYPE_WEEKLY);
                                if (r.getWeekdayFrom() != null && r.getWeekdayFrom().length() > 0) {
                                    rr.setDayOfWeek(r.getWeekdayFrom());
                                }
                            }
                            if (r.getForName() != null && r.getForName().length() > 0) {
                                UUID id = findPerson(persons, IDXP, r.getForName(), false, -1);
                                if (id != null) {
                                    rr.setPersonId(id);
                                } else {
                                    rr.setPersonName(r.getForName());
                                }
                            }
                            if (r.getReason() != null && r.getReason().length() > 0) {
                                rr.setReason(r.getReason());
                            }
                            boatReservations.data().add(rr);
                            logDetail(International.getMessage("Importiere Eintrag: {entry}", rr.toString()));
                        } catch (Exception e) {
                            logError(International.getMessage("Import von Eintrag fehlgeschlagen: {entry} ({error})", rr.toString(), e.toString()));
                            Logger.logdebug(e);
                        }
                    }

                    // BoatDamages
                    if (d.get(BootStatus.BOOTSSCHAEDEN).length() > 0) {
                        BoatDamageRecord rd = boatDamages.createBoatDamageRecord(boatID, 1);
                        try {
                            rd.setDescription(d.get(BootStatus.BOOTSSCHAEDEN));
                            boatDamages.data().add(rd);
                            logDetail(International.getMessage("Importiere Eintrag: {entry}", rd.toString()));
                        } catch (Exception e) {
                            logError(International.getMessage("Import von Eintrag fehlgeschlagen: {entry} ({error})", rd.toString(), e.toString()));
                            Logger.logdebug(e);
                        }
                    }
                } else {
                    if (boatID == null) {
                        logWarning(International.getMessage("{type_of_entry} {entry} nicht in {list} gefunden.",
                                International.getString("Boot"), b,
                                International.getString("Bootsliste")));
                    }
                }
                d = bootStatus.getCompleteNext();
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
