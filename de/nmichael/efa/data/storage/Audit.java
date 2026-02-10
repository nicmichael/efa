/* Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.storage;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.UUID;

public class Audit extends Thread {

    private static final long MAX_MESSAGES_FILESIZE = 1024*1024;
    private static final long MAX_AUDIT_MESSAGE_BUFFER = 1024*1024;

    private static volatile boolean auditRunning = false;
    private Project project;
    private boolean correctErrors;
    private int errors = 0;
    private int warnings = 0;
    private int infos = 0;
    private StringBuilder auditMessages;
    private boolean auditMessagesMaxReached = false;

    public Audit(Project project) {
        this.project = project;
        this.correctErrors = Daten.efaConfig.getValueDataAuditCorrectErrors();
        if (!correctErrors) {
            auditWarning(Logger.MSG_DATA_AUDIT_NOTCORRECTERRORSSET,
                    "Option DataAuditCorrectErrors is NOT set. Audit will only report errors, but not fix them.");
        }
    }

    /*
     * @todo (P3) Audit - SessionGroups
     */

    private void addMessageToBuffer(String s) {
        if (s == null || s.length() == 0) {
            return;
        }
        if (auditMessages != null) {
            if (auditMessages.length() < MAX_AUDIT_MESSAGE_BUFFER) {
                auditMessages.append(s + "\n");
            } else {
                if (!auditMessagesMaxReached) {
                    auditMessages.append("*** too many messages - for all messages, see logfile ***");
                    auditMessagesMaxReached = true;
                }
            }
        }
    }

    private void auditInfo(String key, String msg) {
        String s = Logger.log(Logger.INFO, key, msg, false);
        infos++;
        addMessageToBuffer(s);
    }

    private void auditWarning(String key, String msg) {
        String s = Logger.log(Logger.WARNING, key, msg, false);
        warnings++;
        addMessageToBuffer(s);
    }

    private void auditError(String key, String msg) {
        String s = Logger.log(Logger.ERROR, key, msg, false);
        errors++;
        addMessageToBuffer(s);
    }

    private int runAuditPersistence(StorageObject p, String dataType) {
        if (p != null && p.isOpen()) {
            Logger.log(Logger.DEBUG, Logger.MSG_DATA_AUDIT, dataType + " open (" + p.toString() + ")");
            return 0;
        } else {
            auditError(Logger.MSG_DATA_AUDIT, dataType + " not open");
            return 1;
        }
    }


    // this method constructs a DataKey based on the UUID. Other keys
    // are not supported
    private boolean isReferenceInvalid(UUID id, StorageObject so, long validAt) {
        if (id == null) {
            return false;
        }
        DataKey k = new DataKey(id, null, null);
        try {
            if (validAt >= 0) {
                return so.dataAccess.getValidAt(k, validAt) == null;
            } else {
                if (so.dataAccess.getMetaData().isVersionized()) {
                    return so.dataAccess.getValidLatest(k) == null;
                } else {
                    return so.dataAccess.get(k) == null;
                }
            }
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }

    private UUID findValidReference(String name, StorageObject so, long validAt) {
        if (name == null || name.length() == 0) {
            return null;
        }
        if (so instanceof Boats) {
            BoatRecord r = ((Boats)so).getBoat(name, validAt);
            return (r != null ? r.getId() : null);
        }
        if (so instanceof Persons) {
            PersonRecord r = ((Persons)so).getPerson(name, validAt);
            return (r != null ? r.getId() : null);
        }
        if (so instanceof Destinations) {
            DestinationRecord r = ((Destinations)so).getDestination(name, validAt);
            return (r != null ? r.getId() : null);
        }
        return null;
    }

    private String getNameOfLatestInvalidRecord(UUID id, StorageObject so) {
        try {
            DataRecord[] recs = so.dataAccess.getValidAny(new DataKey(id, null, null));
            long latestValid = -1;
            DataRecord latestRecord = null;
            for (int i=0; recs != null && i<recs.length; i++) {
                if (recs[i].getValidFrom() > latestValid || latestValid < 0) {
                    latestValid = recs[i].getValidFrom();
                    latestRecord = recs[i];
                }
            }
            return (latestRecord != null ? latestRecord.getQualifiedName() : null);
        } catch (Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    private int runAuditProject() {
        int prjErr = 0;
        try {
            String email = project.getAdminEmail();
            if (email != null && email.trim().length() > 0 && !EfaUtil.isValidEmail(email)) {
                auditError(Logger.MSG_DATA_AUDIT_INVALIDREC,
                        International.getMessage("Ungültige email Adresse '{email}' in Feld '{field}'.",
                                    email, 
                                    International.getString("Projekt") + ":" + ProjectRecord.ADMINEMAIL) + " " +
                        International.getString("Bitte korrigiere die email-Adresse in den Projekt-Einstellungen."));
            }
            return prjErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditProject() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++prjErr;
        }
    }
    
    private int runAuditBoats() {
        int boatErr = 0;
        try {
            Boats boats = project.getBoats(false);
            if (boats.dataAccess.getNumberOfRecords() == 0) {
                return boatErr; // don't run check agains empty list (could be due to error opening list)
            }
            BoatStatus boatStatus = project.getBoatStatus(false);
            BoatReservations boatReservations = project.getBoatReservations(false);
            BoatDamages boatDamages = project.getBoatDamages(false);
            Groups groups = project.getGroups(false);
            Crews crews = project.getCrews(false);
            Destinations destinations = project.getDestinations(false);
            Persons persons = project.getPersons(false);

            Hashtable<UUID,Integer> boatVersions = new Hashtable<UUID,Integer>();
            int[] boathouseIds = project.getAllBoathouseIds();

            DataKeyIterator it = boats.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                BoatRecord boat = (BoatRecord)boats.data().get(k);
                if (boat.getId() == null ||
                        boat.getValidFrom() < 0 || boat.getInvalidFrom() < 0 ||
                        boat.getValidFrom() >= boat.getInvalidFrom()) {
                    auditError(Logger.MSG_DATA_AUDIT_INVALIDREC,
                            "Boat Record is invalid: " + boat.toString());
                    boatErr++;
                }
                if (boat.getDeleted()) {
                    // if this boat is marked as deleted, treat it as if it wasn't there any more!

                    // clean up all references to this boat
                    DataRecord bsr = boatStatus.getBoatStatus(boat.getId());
                    if (bsr != null) {
                        if (correctErrors) {
                            boatStatus.dataAccess.delete(bsr.getKey());
                        }
                    }
                    BoatReservationRecord[] brr = boatReservations.getBoatReservations(boat.getId());
                    if (brr != null) {
                        for (BoatReservationRecord r : brr) {
                            if (correctErrors) {
                                boatReservations.dataAccess.delete(r.getKey());
                            }
                        }
                    }
                    BoatDamageRecord[] bdr = boatDamages.getBoatDamages(boat.getId());
                    if (bdr != null) {
                        for (BoatDamageRecord r : bdr) {
                            if (correctErrors) {
                                boatDamages.dataAccess.delete(r.getKey());
                            }
                        }
                    }
                    // don't do anything else for this boat; it's deleted
                    k = it.getNext();
                    continue;
                }
                Integer versions = boatVersions.get(boat.getId());
                if (versions == null) {
                    boatVersions.put(boat.getId(), 1);
                } else {
                    boatVersions.put(boat.getId(), versions.intValue() + 1);
                }

                // check References from BoatRecord
                boolean updated = false;
                if (groups.dataAccess.getNumberOfRecords() > 0) {
                    // run check only agains non-empty list (could be due to error opening list)
                    DataTypeList<UUID> uuidList = boat.getAllowedGroupIdList();
                    boolean listChanged = false;
                    for (int i = 0; uuidList != null && i < uuidList.length(); i++) {
                        if (isReferenceInvalid(uuidList.get(i), groups, -1)) {
                            uuidList.remove(i--);
                            listChanged = true;
                            auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                    "runAuditBoats(): "
                                    + International.getString("Boot") + " " + boat.getQualifiedName() + ": "
                                    + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                    International.getString("Gruppe"),
                                    International.getString("Gruppen, die dieses Boot benutzen dürfen")));
                        }
                    }
                    if (listChanged) {
                        boat.setAllowedGroupIdList(uuidList);
                        updated = true;
                    }
                    if (isReferenceInvalid(boat.getRequiredGroupId(), groups, -1)) {
                        boat.setRequiredGroupId(null);
                        updated = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                "runAuditBoats(): "
                                + International.getString("Boot") + " " + boat.getQualifiedName() + ": "
                                + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                International.getString("Gruppe"),
                                International.getString("Gruppe, der mindestens eine Person angehören muß")));
                    }
                    if (isReferenceInvalid(boat.getDefaultCrewId(), crews, -1)) {
                        boat.setDefaultCrewId(null);
                        updated = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                "runAuditBoats(): "
                                + International.getString("Boot") + " " + boat.getQualifiedName() + ": "
                                + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                International.getString("Mannschaft"),
                                International.getString("Standard-Mannschaft")));
                    }
                    if (boat.getDefaultSessionType() != null &&
                        !Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_SESSION, boat.getDefaultSessionType())) {
                        boat.setDefaultSessionType(null);
                        updated = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                "runAuditBoats(): "
                                + International.getString("Boot") + " " + boat.getQualifiedName() + ": "
                                + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                International.getString("Fahrtart"),
                                International.getString("Standard-Fahrtart")));
                    }
                }
                if (destinations.dataAccess.getNumberOfRecords() > 0) {
                    // run check only agains non-empty list (could be due to error opening list)
                    if (isReferenceInvalid(boat.getDefaultDestinationId(), destinations, -1)) {
                        boat.setDefaultDestinationId(null);
                        updated = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                "runAuditBoats(): "
                                + International.getString("Boot") + " " + boat.getQualifiedName() + ": "
                                + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                International.getString("Ziel"),
                                International.getString("Standard-Ziel")));
                    }
                }
                if (updated) {
                    if (correctErrors) {
                        boats.data().update(boat);
                    }
                }

                BoatStatusRecord status = boatStatus.getBoatStatus(boat.getId());
                if (status == null) {
                    auditWarning(Logger.MSG_DATA_AUDIT_BOATINCONSISTENCY,
                            "No Boat Status found for Boat " + boat.getQualifiedName() + ": " + boat.toString());
                    if (correctErrors) {
                        boatStatus.data().add(boatStatus.createBoatStatusRecord(boat.getId(),
                            boat.getQualifiedName()));
                    }
                    auditInfo(Logger.MSG_DATA_AUDIT_BOATINCONSISTENCY,
                            "New Boat Status added for Boat " + boat.getQualifiedName());
                } else {
                    boolean updatedStatus = false;
                    int bid = status.getOnlyInBoathouseIdAsInt();
                    if (bid != IDataAccess.UNDEFINED_INT) {
                        boolean found = false;
                        for (int i : boathouseIds) {
                            if (i == bid) {
                                found = true;
                            }
                        }
                        if (!found) {
                            auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                    "runAuditBoats(): "
                                    + International.getString("Bootsstatus") + " " + status.getQualifiedName() + ": "
                                    + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                    International.getString("Bootshaus"),
                                    International.getString("Bootshaus")));
                            status.setOnlyInBoathouseId(IDataAccess.UNDEFINED_INT);
                            updatedStatus = true;
                        }
                    }

                // fix text field in boat status to match the current boat name
                    if (!boat.getQualifiedName().equals(status.getBoatText())) {
                        status.setBoatText(boat.getQualifiedName());
                        updatedStatus = true;
                    }
                    if (updatedStatus) {
                        if (correctErrors) {
                            boatStatus.data().update(status);
                        }
                    }
                }

                k = it.getNext();
            }

            // Boat Status
            it = boatStatus.data().getStaticIterator();
            k = it.getFirst();
            while (k != null) {
                BoatStatusRecord status = (BoatStatusRecord) boatStatus.data().get(k);
                if (status != null && !status.getUnknownBoat()) {
                    DataRecord[] boat = boats.data().getValidAny(BoatRecord.getKey(status.getBoatId(), 0));
                    if (boat == null || boat.length == 0) {
                        auditError(Logger.MSG_DATA_AUDIT_BOATINCONSISTENCY,
                                "No Boat found for Boat Status: " + status.toString());
                        boatErr++;
                        if (correctErrors) {
                            boatStatus.dataAccess.delete(status.getKey());
                        }
                        auditInfo(Logger.MSG_DATA_AUDIT_BOATINCONSISTENCY,
                                "Boat Status " + status.toString() + " deleted.");
                        k = it.getNext();
                        continue;
                    }
                }

                // check References from BoatStatus
                boolean updated = false;
                if (Daten.applID == Daten.APPL_EFABH &&
                    BoatStatusRecord.STATUS_ONTHEWATER.equals(status.getCurrentStatus())) {
                    String logbookName = status.getLogbook();
                    DataTypeIntString entryNo = status.getEntryNo();
                    if (logbookName == null || entryNo == null) {
                        status.setCurrentStatus(status.getBaseStatus());
                        status.setEntryNo(null);
                        updated = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_BOATSTATUSCORRECTED,
                                "runAuditBoats(): "
                                + International.getString("Bootsstatus") + " " + status.getBoatText() + ": "
                                + International.getMessage("Bootsstatus '{status}' korrigiert nach '{status}'.",
                                    status.getStatusDescription(BoatStatusRecord.STATUS_ONTHEWATER),
                                    status.getStatusDescription(status.getBaseStatus()))
                                + " (Logbook or EntryNo not set)");
                    }
                    if (logbookName != null && !logbookName.equals(project.getCurrentLogbookEfaBoathouse())) {
                        auditError(Logger.MSG_DATA_AUDIT_INVALIDREFFOUND,
                                "runAuditBoats(): "
                                + International.getString("Bootsstatus") + " " + status.getBoatText() + ": "
                                + International.getMessage("Boot ist unterwegs in Fahrtenbuch {name}, aber Fahrtenbuch {name} ist geöffnet.",
                                                           logbookName, project.getCurrentLogbookEfaBoathouse()) + " "
                                + International.getString("Bitte korrigiere den Status des Bootes im Admin-Modus."));
                        boatErr++;
                    } else {
                        Logbook logbook = (logbookName != null ? project.getLogbook(logbookName, false) : null);
                        if (logbook == null || logbook.dataAccess == null ||
                                logbook.dataAccess.getNumberOfRecords() == 0) {
                            status.setCurrentStatus(status.getBaseStatus());
                            status.setLogbook(null);
                            status.setEntryNo(null);
                            updated = true;
                            auditWarning(Logger.MSG_DATA_AUDIT_BOATSTATUSCORRECTED,
                                    "runAuditBoats(): "
                                    + International.getString("Bootsstatus") + " " + status.getBoatText() + ": "
                                    + International.getMessage("Bootsstatus '{status}' korrigiert nach '{status}'.",
                                    status.getStatusDescription(BoatStatusRecord.STATUS_ONTHEWATER),
                                    status.getStatusDescription(status.getBaseStatus()))
                                    + " (Logbook '" + logbookName + "' does not exist)");
                        } else {
                            LogbookRecord lr = logbook.getLogbookRecord(entryNo);
                            if (lr == null) {
                                status.setCurrentStatus(status.getBaseStatus());
                                status.setLogbook(null);
                                status.setEntryNo(null);
                                updated = true;
                                auditWarning(Logger.MSG_DATA_AUDIT_BOATSTATUSCORRECTED,
                                        "runAuditBoats(): "
                                        + International.getString("Bootsstatus") + " " + status.getBoatText() + ": "
                                        + International.getMessage("Bootsstatus '{status}' korrigiert nach '{status}'.",
                                        status.getStatusDescription(BoatStatusRecord.STATUS_ONTHEWATER),
                                        status.getStatusDescription(status.getBaseStatus()))
                                        + " (Entry #" + (entryNo != null ? entryNo.toString() : " null") + " in Logbook '" + logbookName + "' does not exist)");
                            }
                        }
                    }
                }
                if (updated) {
                    boatStatus.data().update(status);
                }

                k = it.getNext();
            }

            // Boat Reservations
            it = boatReservations.data().getStaticIterator();
            k = it.getFirst();
            while (k != null) {
                BoatReservationRecord reservation = (BoatReservationRecord)boatReservations.data().get(k);
                DataRecord[] boat = boats.data().getValidAny(BoatRecord.getKey(reservation.getBoatId(), 0));
                if (boat == null || boat.length == 0) {
                    auditError(Logger.MSG_DATA_AUDIT_BOATINCONSISTENCY,
                            "No Boat found for Boat Reservation: " + reservation.toString());
                    boatErr++;
                    if (correctErrors) {
                        boatReservations.dataAccess.delete(reservation.getKey());
                    }
                    auditInfo(Logger.MSG_DATA_AUDIT_BOATINCONSISTENCY,
                            "Boat Reservation " + reservation.toString() + " deleted.");
                    k = it.getNext();
                    continue;
                }

                // check References from BoatReservations
                if (persons.dataAccess.getNumberOfRecords() > 0) {
                    // run check only agains non-empty list (could be due to error opening list)
                    if (isReferenceInvalid(reservation.getPersonId(), persons, -1)) {
                        String name = getNameOfLatestInvalidRecord(reservation.getPersonId(), persons);
                        if (name != null) {
                            reservation.setPersonId(null);
                            reservation.setPersonName(name);
                            if (correctErrors) {
                                boatReservations.dataAccess.update(reservation);
                            }
                            auditWarning(Logger.MSG_DATA_AUDIT_REFTOTEXT,
                                    "runAuditBoats(): "
                                    + International.getString("Reservierung") + " " + boat[0].getQualifiedName() + ": "
                                    + International.getMessage("Ungültige Referenz für {item} durch '{name}' ersetzt.",
                                    International.getString("Person"), name));
                        } else {
                            reservation.setPersonId(null);
                            if (correctErrors) {
                                boatReservations.dataAccess.update(reservation);
                            }
                            auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                    "runAuditBoats(): "
                                    + International.getString("Reservierung") + " " + boat[0].getQualifiedName() + ": "
                                    + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                    International.getString("Person"),
                                    International.getString("Reserviert für")));
                        }
                    }
                }

                k = it.getNext();
            }

            it = boatDamages.data().getStaticIterator();
            k = it.getFirst();
            while (k != null) {
                BoatDamageRecord damage = (BoatDamageRecord)boatDamages.data().get(k);
                DataRecord[] boat = boats.data().getValidAny(BoatRecord.getKey(damage.getBoatId(), 0));
                if (boat == null || boat.length == 0) {
                    auditError(Logger.MSG_DATA_AUDIT_BOATINCONSISTENCY,
                            "No Boat found for Boat Damage: " + damage.toString());
                    boatErr++;
                    if (correctErrors) {
                        boatStatus.dataAccess.delete(damage.getKey());
                    }
                    auditInfo(Logger.MSG_DATA_AUDIT_BOATINCONSISTENCY,
                            "Boat Damage " + damage.toString() + " deleted.");
                    k = it.getNext();
                    continue;
                }

                // check References from BoatDamages
                boolean updated = false;
                if (persons.dataAccess.getNumberOfRecords() > 0) {
                    // run check only agains non-empty list (could be due to error opening list)
                    if (isReferenceInvalid(damage.getReportedByPersonId(), persons, -1)) {
                        String name = getNameOfLatestInvalidRecord(damage.getReportedByPersonId(), persons);
                        if (name != null) {
                            damage.setReportedByPersonId(null);
                            damage.setReportedByPersonName(name);
                            updated = true;
                            auditWarning(Logger.MSG_DATA_AUDIT_REFTOTEXT,
                                    "runAuditBoats(): "
                                    + International.getString("Bootsschaden") + " " + boat[0].getQualifiedName() + ": "
                                    + International.getMessage("Ungültige Referenz für {item} durch '{name}' ersetzt.",
                                    International.getString("Person"), name));
                        } else {
                            auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                    "runAuditBoats(): "
                                    + International.getString("Bootsschaden") + " " + boat[0].getQualifiedName() + ": "
                                    + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                    International.getString("Person"),
                                    International.getString("gemeldet von")));
                            damage.setReportedByPersonId(null);
                            updated = true;
                        }
                    }
                    if (isReferenceInvalid(damage.getFixedByPersonId(), persons, -1)) {
                        String name = getNameOfLatestInvalidRecord(damage.getFixedByPersonId(), persons);
                        if (name != null) {
                            damage.setFixedByPersonId(null);
                            damage.setFixedByPersonName(name);
                            updated = true;
                            auditWarning(Logger.MSG_DATA_AUDIT_REFTOTEXT,
                                    "runAuditBoats(): "
                                    + International.getString("Bootsschaden") + " " + boat[0].getQualifiedName() + ": "
                                    + International.getMessage("Ungültige Referenz für {item} durch '{name}' ersetzt.",
                                    International.getString("Person"), name));
                        } else {
                            auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                    "runAuditBoats(): "
                                    + International.getString("Bootsschaden") + " " + boat[0].getQualifiedName() + ": "
                                    + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                    International.getString("Person"),
                                    International.getString("behoben von")));
                            damage.setFixedByPersonId(null);
                            updated = true;
                        }
                    }
                    if (updated) {
                        if (correctErrors) {
                            boatDamages.dataAccess.update(damage);
                        }
                    }
                }

                k = it.getNext();
            }

            return boatErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditBoats() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++boatErr;
        }
    }

    private int runAuditCrews() {
        int crewErr = 0;
        try {
            Crews crews = project.getCrews(false);
            Persons persons = project.getPersons(false);
            if (persons.dataAccess.getNumberOfRecords() == 0) {
                return crewErr; // don't run check agains empty list (could be due to error opening list)
            }
            DataKeyIterator it = crews.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                CrewRecord crew = (CrewRecord)crews.data().get(k);
                boolean updated = false;
                for (int i=0; i<=LogbookRecord.CREW_MAX; i++) {
                    if (isReferenceInvalid(crew.getCrewId(i), persons, -1)) {
                        crew.setCrewId(i, null);
                        updated = true;
                    }
                }
                if (updated) {
                    if (correctErrors) {
                        crews.dataAccess.update(crew);
                    }
                }
                k = it.getNext();
            }

            return crewErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditBoats() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++crewErr;
        }
    }

    private int runAuditGroups() {
        int groupErr = 0;
        try {
            Groups groups = project.getGroups(false);
            Persons persons = project.getPersons(false);
            if (persons.dataAccess.getNumberOfRecords() == 0) {
                return groupErr; // don't run check agains empty list (could be due to error opening list)
            }
            DataKeyIterator it = groups.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                GroupRecord group = (GroupRecord) groups.data().get(k);
                DataTypeList<UUID> uuidList = group.getMemberIdList();
                boolean listChanged = false;
                for (int i = 0; uuidList != null && i < uuidList.length(); i++) {
                    if (isReferenceInvalid(uuidList.get(i), persons, -1)) {
                        uuidList.remove(i--);
                        listChanged = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                "runAuditGroups(): "
                                + International.getString("Gruppe") + " " + group.getQualifiedName() + ": "
                                + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                International.getString("Person"),
                                International.getString("Mitglieder") + " " + (i+1)));
                    }
                }
                if (listChanged) {
                    group.setMemberIdList(uuidList);
                    if (correctErrors) {
                        groups.dataAccess.update(group);
                    }
                }
                k = it.getNext();
            }

            return groupErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditGroups() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++groupErr;
        }
    }

    private int runAuditDestinations() {
        int destinationErr = 0;
        try {
            int[] boathouseIds = project.getAllBoathouseIds();
            Destinations destinations = project.getDestinations(false);
            Waters waters = project.getWaters(false);
            if (waters.dataAccess.getNumberOfRecords() == 0) {
                return destinationErr; // don't run check agains empty list (could be due to error opening list)
            }
            DataKeyIterator it = destinations.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                boolean updated = false;
                DestinationRecord destination = (DestinationRecord) destinations.data().get(k);
                DataTypeList<UUID> uuidList = destination.getWatersIdList();
                boolean listChanged = false;
                for (int i = 0; uuidList != null && i < uuidList.length(); i++) {
                    if (isReferenceInvalid(uuidList.get(i), waters, -1)) {
                        uuidList.remove(i--);
                        listChanged = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                "runAuditDestinations(): "
                                + International.getString("Ziel") + " " + destination.getQualifiedName() + ": "
                                + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                International.getString("Gewässer"),
                                International.getString("Gewässer") + " " + (i+1)));
                    }
                }

                int bid = destination.getOnlyInBoathouseIdAsInt();
                if (bid != IDataAccess.UNDEFINED_INT) {
                    boolean found = false;
                    for (int i : boathouseIds) {
                        if (i == bid) {
                            found = true;
                        }
                    }
                    if (!found) {
                        auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                                "runAuditDestinations(): "
                                + International.getString("Ziel") + " " + destination.getQualifiedName() + ": "
                                + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                                International.getString("Bootshaus"),
                                International.getString("Bootshaus")));
                        destination.setOnlyInBoathouseId(IDataAccess.UNDEFINED_INT);
                        updated = true;
                    }
                }

                if (listChanged) {
                    destination.setWatersIdList(uuidList);
                    updated = true;
                }
                if (updated) {
                    if (correctErrors) {
                        destinations.dataAccess.update(destination);
                    }
                }
                k = it.getNext();
            }

            return destinationErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditDestinations() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++destinationErr;
        }
    }

    private int runAuditWaters() {
        int watersErr = 0;
        try {
            Waters waters = project.getWaters(false);
            if (waters.dataAccess.getNumberOfRecords() == 0) {
                return watersErr; // don't run check agains empty list (could be due to error opening list)
            }
            DataKeyIterator it = waters.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                boolean updated = false;
                WatersRecord water = (WatersRecord) waters.data().get(k);
                String name = water.getName();
                if (name != null) {
                    if (name.equals("Rheinberger Gewässer")) {
                        water.setName("Rheinsberger Gewässer");
                        updated = true;
                    }
                    if (name.equals("Forgensee")) {
                        water.setName("Forggensee");
                        updated = true;
                    }
                    if (updated) {
                        auditWarning(Logger.MSG_DATA_AUDIT_NAMECORRECTED,
                                "runAuditWaters(): Misspelled name fixed (" +
                                name + " -> " + water.getName() + ")");
                    }
                }
                if (updated) {
                    if (correctErrors) {
                        waters.dataAccess.update(water);
                    }
                }
                k = it.getNext();
            }

            return watersErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditWaters() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++watersErr;
        }
    }

    private int runAuditStatus() {
        int statusErr = 0;
        try {
            Status status = project.getStatus(false);
            Calendar cal = new GregorianCalendar();
            cal.set(2015, 11, 1, 0, 0, 0); // 2015-12-01 00:00:00 (around efa 2.2.2 release time)
            DataKeyIterator it = status.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                StatusRecord r = (StatusRecord)status.data().get(k);
                boolean updated = false;
                if (r != null && r.getLastModified() < cal.getTimeInMillis()) {
                    String name = r.getStatusName();
                    if (name != null && 
                        (name.equals(International.getString("Junior(in)")) || 
                         name.equals(International.getString("Senior(in)")))) {
                        // this is a default status record that was last modified before the efa v2.2.2 release
                        if (!r.getAutoSetOnAge()) {
                            r.setAutoSetOnAge(true);
                            if (name.equals(International.getString("Junior(in)"))) {
                                r.setMaxAge(18);
                            } else {
                                r.setMinAge(19);
                            }
                            updated = true;
                            auditWarning(Logger.MSG_DATA_AUDIT_STATUSUPDATED,
                                    "runAuditStatus(): "
                                    + International.getString("Status") + " " + name + ": "
                                    + International.getString("Automatische Status-Anpassung anhand des Jahrgangs für diesen Status aktiviert."));
                        }
                    }
                }
                if (updated) {
                    if (correctErrors) {
                        status.dataAccess.update(r);
                    }
                }
                k = it.getNext();
            }

            return statusErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditStatus() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++statusErr;
        }
    }

    private int runAuditPersons() {
        int personErr = 0;
        try {
            Persons persons = project.getPersons(false);
            Boats boats = project.getBoats(false);
            Status status = project.getStatus(false);
            if (boats.dataAccess.getNumberOfRecords() == 0 ||
                status.dataAccess.getNumberOfRecords() == 0) {
                return personErr; // don't run check agains empty list (could be due to error opening list)
            }

            Calendar cal = new GregorianCalendar();
            int year = cal.get(Calendar.YEAR);
            cal.set(year, 0, 1, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long now = System.currentTimeMillis();
            
            DataKeyIterator it = persons.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                PersonRecord person = (PersonRecord) persons.data().get(k);
                boolean updated = false;
                if (isReferenceInvalid(person.getStatusId(), status, -1)) {
                    person.setStatusId(status.getStatusOther().getId());
                    updated = true;
                    auditWarning(Logger.MSG_DATA_AUDIT_REFTOTEXT,
                            "runAuditPersons(): "
                            + International.getString("Person") + " " + person.getQualifiedName() + ": "
                            + International.getMessage("Ungültige Referenz für {item} durch '{name}' ersetzt.",
                            International.getString("Status"),
                            status.getStatusOther().getQualifiedName()));
                } else {
                    if (person.isValidAt(now)) {
                        StatusRecord sr = person.getStatusRecord();
                        if (sr != null && sr.getAutoSetOnAge()) {
                            DataTypeDate birthday = person.getBirthday();
                            if (birthday != null && birthday.isSet()) {
                                int minAge = sr.getMinAge();
                                int maxAge = sr.getMaxAge();
                                int age = year - birthday.getYear();
                                if ((minAge != ItemTypeInteger.UNSET && age < minAge)
                                        || (maxAge != ItemTypeInteger.UNSET && age > maxAge)) {
                                    StatusRecord newsr = status.getStatusForAge(age, sr.isMember());
                                    if (newsr != null) {
                                        person.setStatusId(newsr.getId());
                                        if (person.getValidFrom() >= cal.getTimeInMillis()) {
                                            updated = true;
                                        } else {
                                            if (correctErrors) {
                                                persons.dataAccess.addValidAt(person, cal.getTimeInMillis());
                                                updated = false; // we've added the new version here; no need to update later
                                            }
                                        }
                                        auditWarning(Logger.MSG_DATA_AUDIT_STATUSUPDATED,
                                                "runAuditPersons(): "
                                                + International.getString("Person") + " " + person.getQualifiedName() + ": "
                                                + International.getMessage("Status aufgrund Erreichens des Alters {age} im laufenden Jahr von '{status}' nach '{status}' geändert.",
                                                        Integer.toString(age),
                                                        sr.getStatusName(),
                                                        newsr.getStatusName()));
                                    } else {
                                        auditWarning(Logger.MSG_DATA_AUDIT_STATUSUPDATED,
                                                "runAuditPersons(): "
                                                + International.getString("Person") + " " + person.getQualifiedName() + ": "
                                                + International.getMessage("Status '{status}' ungültig für das Alter {age}, aber kein geeigneter Status gefunden. Bitte korrigiere den Status manuell.",
                                                        sr.getStatusName(),
                                                        Integer.toString(age)));
                                    }
                                }
                            }
                        }
                    }
                }
                if (isReferenceInvalid(person.getDefaultBoatId(), boats, -1)) {
                    person.setDefaultBoatId(null);
                    updated = true;
                    auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                            "runAuditPersons(): "
                            + International.getString("Person") + " " + person.getQualifiedName() + ": "
                            + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                            International.getString("Boot"),
                            International.getString("Standard-Boot")));
                }
                if (updated) {
                    if (correctErrors) {
                        persons.dataAccess.update(person);
                    }
                }
                k = it.getNext();
            }

            return personErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditPersons() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++personErr;
        }
    }

    private int runAuditFahrtenabzeichen() {
        int faErr = 0;
        try {
            Fahrtenabzeichen fahrtenabzeichen = project.getFahrtenabzeichen(false);
            Persons persons = project.getPersons(false);
            if (persons.dataAccess.getNumberOfRecords() == 0) {
                return faErr; // don't run check agains empty list (could be due to error opening list)
            }
            DataKeyIterator it = fahrtenabzeichen.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                FahrtenabzeichenRecord abzeichen = (FahrtenabzeichenRecord) fahrtenabzeichen.data().get(k);
                if (persons.dataAccess.getValidLatest(PersonRecord.getKey(abzeichen.getPersonId(), -1)) == null) {
                    auditWarning(Logger.MSG_DATA_AUDIT_RECNOTFOUND,
                            "runAuditFahrtenabzeichen(): Keine Person zu Fahrtenabzeichen gefunden: " + abzeichen.toString());
                    if (correctErrors) {
                        fahrtenabzeichen.dataAccess.delete(abzeichen.getKey());
                    }
                    auditWarning(Logger.MSG_DATA_AUDIT_RECNOTFOUND,
                            "runAuditFahrtenabzeichen(): Fahrtenabzeichen " + abzeichen.toString() + " gelöscht.");
                }
                k = it.getNext();
            }

            return faErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditFahrtenabzeichen() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++faErr;
        }
    }

    private int archiveMessages(Messages messages, boolean all) throws Exception {
        Messages archived = new Messages(messages.data().getStorageType(),
                Daten.efaBakDirectory,
                null, null,
                "messages_" + EfaUtil.getCurrentTimeStampYYYYMMDD_HHMMSS());
        archived.open(true); // @todo - causes file not found exception! need to fix!!!!
        long lock = messages.data().acquireGlobalLock();
        int cntRead = 0;
        int cntUnread = 0;
        int cntMoved = 0;
        try {
            DataKeyIterator it = messages.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                MessageRecord r = (MessageRecord) messages.data().get(k);
                if (all || r.getRead()) {
                    cntRead++;
                    try {
                        archived.data().add(r);
                        messages.data().delete(k, lock);
                        cntMoved++;
                    } catch (Exception e1) {
                        Logger.log(e1);
                    }
                } else {
                    cntUnread++;
                }
                k = it.getNext();
            }
            archived.close();
            auditInfo(Logger.MSG_DATA_FILEARCHIVED,
                    (all ?
                        International.getMessage("Alle {count} Nachrichten wurden erfolgreich in die Archivdatei {filename} verschoben.",
                        cntMoved, ((DataFile) archived.data()).filename) :
                        International.getMessage("{count} gelesene Nachrichten wurden erfolgreich in die Archivdatei {filename} verschoben.",
                        cntMoved, ((DataFile) archived.data()).filename)
                        ));
        } finally {
            messages.data().releaseGlobalLock(lock);
        }
        ((DataFile) messages.data()).flush();
        return cntUnread;
    }

    private int runAuditMessages() {
        int messageErr = 0;
        try {
            Messages messages = project.getMessages(false);
            if ((messages.data().getStorageType() == IDataAccess.TYPE_FILE_XML)
                 || (messages.data().getStorageType() == IDataAccess.TYPE_EFA_CLOUD))
            {
                long size = ((DataFile)messages.data()).getFileSize();
                if (size > MAX_MESSAGES_FILESIZE) {
                    auditInfo(Logger.MSG_DATA_FILESIZEHIGH,
                            International.getMessage("Nachrichtendatei hat maximale Dateigröße überschritten. Derzeitige Größe: {size} byte", size));
                    int cntUnread = archiveMessages(messages, false);
                    size = ((DataFile) messages.data()).getFileSize();
                    if (size > 4*MAX_MESSAGES_FILESIZE) {
                        auditError(Logger.MSG_DATA_FILESIZEHIGH,
                                International.getMessage("Nachrichtendatei hat maximale Dateigröße überschritten. Derzeitige Größe: {size} byte", size) +
                                " - File Size way too high - Emergency Backup of all Messages");
                        cntUnread = archiveMessages(messages, true);
                        size = ((DataFile) messages.data()).getFileSize();
                    }
                    if (size > MAX_MESSAGES_FILESIZE / 2) {
                        auditWarning(Logger.MSG_DATA_FILESIZEHIGH,
                                International.getMessage("Nachrichtendatei ist nach Archivierung gelesener Nachrichten noch immer groß. Derzeitige Größe: {size} byte", size));
                        auditWarning(Logger.MSG_DATA_FILESIZEHIGH,
                                International.getMessage("Es gibt {count} ungelesene Nachrichten. Bitte lies die Nachrichten und markiere sie als gelesen.", cntUnread));
                    }
                    if (size > 2*MAX_MESSAGES_FILESIZE) {
                        auditError(Logger.MSG_DATA_FILESIZEHIGH,
                                International.getString("Die Nachrichtendatei ist sehr groß. Bitte lösche umgehend alte Nachrichten und markiere gelesene Nachrichten als gelesen!"));
                    }
                }
            }
            return messageErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditMessages() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++messageErr;
        }
    }

    private int runAuditStatistics() {
        int statsErr = 0;
        try {
            Statistics statistics = project.getStatistics(false);
            Hashtable<Integer,StatisticsRecord> hash = new Hashtable<Integer,StatisticsRecord>();
            DataKeyIterator it = statistics.dataAccess.getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                StatisticsRecord r = (StatisticsRecord)statistics.dataAccess.get(k);
                if ((r.getFilterBoatOwner() == null || r.getFilterBoatOwner().length() == 0) &&
                     !r.getFilterBoatOwnerAll()) {
                    r.setFilterBoatOwnerAll(true);
                    statistics.dataAccess.update(r);
                }
                hash.put(r.getPosition(), r);
                k = it.getNext();
            }
            Integer[] positions = hash.keySet().toArray(new Integer[0]);
            Arrays.sort(positions);
            boolean needsReordering = false;
            int expectedPos = 1;
            for (int i = 0; i<positions.length; i++) {
                if (positions[i] != expectedPos++) {
                    needsReordering = true;
                }
            }
            if (needsReordering && correctErrors) {
                // we should be locking, but well.. so what. This is just a cleanup at startup that
                // shouldn't happen anyhow
                try {
                    statistics.dataAccess.truncateAllData();
                    expectedPos = 1;
                    for (int i=0; i<positions.length; i++) {
                        StatisticsRecord r = hash.get(positions[i]);
                        r.setPosition(expectedPos++);
                        statistics.dataAccess.add(r);
                    }
                } catch(Exception e) {
                    auditError(Logger.MSG_DATA_AUDIT,
                            "runAuditStatistics() Caught Exception: " + e.toString());
                    Logger.log(e);
                    statsErr++;
                }
            }
            return statsErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditStatistics() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++statsErr;
        }
    }

    private int runAuditLogbook(String logbookName) {
        int logbookErr = 0;
        try {
            Boats boats = project.getBoats(false);
            Persons persons = project.getPersons(false);
            Destinations destinations = project.getDestinations(false);
            ProjectRecord prjLogkoobRec = project.getLoogbookRecord(logbookName);
            SessionGroups sessionGroups = project.getSessionGroups(false);
            BoatStatus boatStatus = project.getBoatStatus(false);
            if (boats.dataAccess.getNumberOfRecords() == 0 ||
                persons.dataAccess.getNumberOfRecords() == 0 ||
                destinations.dataAccess.getNumberOfRecords() == 0) {
                return logbookErr; // don't run check agains empty list (could be due to error opening list)
            }
            UUID id;

            boolean wasLogbookOpen = project.isLogbookOpen(logbookName);
            Logbook logbook = project.getLogbook(logbookName, false);
            DataKeyIterator it = logbook.dataAccess.getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                LogbookRecord r = (LogbookRecord)logbook.dataAccess.get(k);
                long validAt = r.getValidAtTimestamp();
                boolean updated = false;

                // Dates
                if (r.getDate() == null || !r.getDate().isSet()) {
                    auditError(Logger.MSG_DATA_AUDIT_LOGBOOKERROR,
                            "runAuditLogbook(): "
                            + International.getString("Fahrtenbuch") + " " + logbookName + " "
                            + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                            + "No Date set.");
                    logbookErr++;
                } else {
                    if (!r.getDate().isInRange(prjLogkoobRec.getStartDate(), prjLogkoobRec.getEndDate())) {
                        auditError(Logger.MSG_DATA_AUDIT_LOGBOOKERROR,
                                "runAuditLogbook(): "
                                + International.getString("Fahrtenbuch") + " " + logbookName + " "
                                + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                                + "Date " + r.getDate().toString() + " is not within defined range for this logbook (" +
                                prjLogkoobRec.getStartDate() + " - " + prjLogkoobRec.getEndDate() + ").");
                        logbookErr++;
                    }
                }
                if (r.getEndDate() != null && r.getEndDate().isSet() &&
                    r.getDate() != null && r.getDate().isSet() &&
                    r.getEndDate().isBeforeOrEqual(r.getDate())) {
                        auditError(Logger.MSG_DATA_AUDIT_LOGBOOKERROR,
                                "runAuditLogbook(): "
                                + International.getString("Fahrtenbuch") + " " + logbookName + " "
                                + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                                + "End date " + r.getEndDate().toString() + " must be after start date " + r.getDate().toString() + ".");
                        logbookErr++;
                }
                
                // Boat References
                if (r.getBoatId() != null && r.getBoatName() != null && r.getBoatName().length() > 0
                        && !isReferenceInvalid(r.getBoatId(), boats, validAt)) {
                    String name = r.getBoatName();
                    r.setBoatName(null);
                    updated = true;
                    auditWarning(Logger.MSG_DATA_AUDIT_TEXTTOREF,
                            "runAuditLogbook(): "
                            + International.getString("Fahrtenbuch") + " " + logbookName + " "
                            + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                            + International.getMessage("{item} '{name}' durch Referenz zu Datensatz '{name}' ersetzt.",
                            International.getString("Boot"), name + " [redundant]",
                            boats.getBoat(r.getBoatId(), validAt).getQualifiedName()));
                }
                if (isReferenceInvalid(r.getBoatId(), boats, validAt)) {
                    String name = getNameOfLatestInvalidRecord(r.getBoatId(), boats);
                    if (name == null) {
                        name = r.getBoatName(); // shouldn't be set, but we can at least try
                    }
                    if (name != null) {
                        r.setBoatName(name);
                        r.setBoatId(null);
                        updated = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_REFTOTEXT,
                                "runAuditLogbook(): "
                                + International.getString("Fahrtenbuch") + " " + logbookName + " "
                                + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                                + International.getMessage("Ungültige Referenz für {item} durch '{name}' ersetzt.",
                                International.getString("Boot"), name));
                    } else {
                        auditError(Logger.MSG_DATA_AUDIT_INVALIDREFFOUND,
                                "runAuditLogbook(): "
                                + International.getString("Fahrtenbuch") + " " + logbookName + " "
                                + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                                + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gefunden.",
                                International.getString("Boot"),
                                International.getString("Boot")
                                ));
                        logbookErr++;
                    }
                } else if ( (id = findValidReference(r.getBoatName(), boats, validAt)) != null) {
                    String name = r.getBoatName();
                    r.setBoatId(id);
                    r.setBoatName(null);
                    updated = true;
                    auditWarning(Logger.MSG_DATA_AUDIT_TEXTTOREF,
                            "runAuditLogbook(): "
                            + International.getString("Fahrtenbuch") + " " + logbookName + " "
                            + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                            + International.getMessage("{item} '{name}' durch Referenz zu Datensatz '{name}' ersetzt.",
                            International.getString("Boot"), name + " [" + International.getString("unbekannt") + "]",
                            boats.getBoat(id, validAt).getQualifiedName()));
                }

                // Persons
                for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                    if (r.getCrewId(i) != null && r.getCrewName(i) != null && r.getCrewName(i).length() > 0
                            && !isReferenceInvalid(r.getCrewId(i), persons, validAt)) {
                        String name = r.getCrewName(i);
                        r.setCrewName(i, null);
                        updated = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_TEXTTOREF,
                                "runAuditLogbook(): "
                                + International.getString("Fahrtenbuch") + " " + logbookName + " "
                                + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                                + International.getMessage("{item} '{name}' durch Referenz zu Datensatz '{name}' ersetzt.",
                                International.getString("Person"), name + " [redundant]",
                                persons.getPerson(r.getCrewId(i), validAt).getQualifiedName()));
                    }
                    if (isReferenceInvalid(r.getCrewId(i), persons, validAt)) {
                        String name = getNameOfLatestInvalidRecord(r.getCrewId(i), persons);
                        if (name != null) {
                            r.setCrewName(i, name);
                            r.setCrewId(i, null);
                            updated = true;
                            auditWarning(Logger.MSG_DATA_AUDIT_REFTOTEXT,
                                    "runAuditLogbook(): "
                                    + International.getString("Fahrtenbuch") + " " + logbookName + " "
                                    + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                                    + International.getMessage("Ungültige Referenz für {item} durch '{name}' ersetzt.",
                                    International.getString("Person"), name));
                        } else {
                            auditError(Logger.MSG_DATA_AUDIT_INVALIDREFFOUND,
                                    "runAuditLogbook(): "
                                    + International.getString("Fahrtenbuch") + " " + logbookName + " "
                                    + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                                    + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gefunden.",
                                    International.getString("Person"),
                                    (i == 0 ? International.getString("Steuermann") :
                                              International.getString("Mannschaft") + " " + i)));
                            logbookErr++;
                        }
                    } else if ((id = findValidReference(r.getCrewName(i), persons, validAt)) != null) {
                        String name = r.getCrewName(i);
                        r.setCrewId(i, id);
                        r.setCrewName(i, null);
                        updated = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_TEXTTOREF,
                                "runAuditLogbook(): "
                                + International.getString("Fahrtenbuch") + " " + logbookName + " "
                                + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                                + International.getMessage("{item} '{name}' durch Referenz zu Datensatz '{name}' ersetzt.",
                                International.getString("Person"), name + " [" + International.getString("unbekannt") + "]",
                                persons.getPerson(id, validAt).getQualifiedName()));
                    }
                }

                // Destination Reference
                if (r.getDestinationId() != null && r.getDestinationName() != null && r.getDestinationName().length() > 0
                        && !isReferenceInvalid(r.getDestinationId(), destinations, validAt)) {
                    String name = r.getDestinationName();
                    r.setDestinationName(null);
                    updated = true;
                    auditWarning(Logger.MSG_DATA_AUDIT_TEXTTOREF,
                            "runAuditLogbook(): "
                            + International.getString("Fahrtenbuch") + " " + logbookName + " "
                            + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                            + International.getMessage("{item} '{name}' durch Referenz zu Datensatz '{name}' ersetzt.",
                            International.getString("Ziel"), name + " [redundant]",
                            destinations.getDestination(r.getDestinationId(), validAt).getQualifiedName()));
                }
                if (isReferenceInvalid(r.getDestinationId(), destinations, validAt)) {
                    String name = getNameOfLatestInvalidRecord(r.getDestinationId(), destinations);
                    if (name != null) {
                        r.setDestinationName(name);
                        r.setDestinationId(null);
                        updated = true;
                        auditWarning(Logger.MSG_DATA_AUDIT_REFTOTEXT,
                                "runAuditLogbook(): "
                                + International.getString("Fahrtenbuch") + " " + logbookName + " "
                                + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                                + International.getMessage("Ungültige Referenz für {item} durch '{name}' ersetzt.",
                                International.getString("Ziel"), name));
                    } else {
                        auditError(Logger.MSG_DATA_AUDIT_INVALIDREFFOUND,
                                "runAuditLogbook(): "
                                + International.getString("Fahrtenbuch") + " " + logbookName + " "
                                + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                                + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gefunden.",
                                International.getString("Ziel"),
                                International.getString("Ziel")
                                ));
                        logbookErr++;
                    }
                } else if ( (id = findValidReference(r.getDestinationName(), destinations, validAt)) != null) {
                    String name = r.getDestinationName();
                    r.setDestinationId(id);
                    r.setDestinationName(null);
                    updated = true;
                    auditWarning(Logger.MSG_DATA_AUDIT_TEXTTOREF,
                            "runAuditLogbook(): "
                            + International.getString("Fahrtenbuch") + " " + logbookName + " "
                            + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                            + International.getMessage("{item} '{name}' durch Referenz zu Datensatz '{name}' ersetzt.",
                            International.getString("Ziel"), name + " [" + International.getString("unbekannt") + "]",
                            destinations.getDestination(id, validAt).getQualifiedName()));
                }

                // Session Type
                if (r.getSessionType() == null ||
                    !Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_SESSION, r.getSessionType())) {
                    r.setSessionType(EfaTypes.TYPE_SESSION_NORMAL);
                    updated = true;
                    auditWarning(Logger.MSG_DATA_AUDIT_REFTOTEXT,
                            "runAuditLogbook(): "
                            + International.getString("Fahrtenbuch") + " " + logbookName + " "
                            + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                            + International.getMessage("Ungültige Referenz für {item} durch '{name}' ersetzt.",
                            International.getString("Fahrtart"),
                            Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_NORMAL)));
                }

                // SessionGroup
                if (isReferenceInvalid(r.getSessionGroupId(), sessionGroups, -1) &&
                    sessionGroups.dataAccess.getNumberOfRecords() > 0) {
                    auditError(Logger.MSG_DATA_AUDIT_INVALIDREFFOUND,
                            "runAuditLogbook(): "
                            + International.getString("Fahrtenbuch") + " " + logbookName + " "
                            + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                            + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gefunden.",
                            International.getString("Fahrtgruppe"),
                            International.getString("Fahrtgruppe")));
                    logbookErr++;
                }

                // Open Session?
                if (r.getSessionIsOpen() &&
                        boatStatus.getBoatStatus(logbookName, r.getEntryId()) == null) {
                    r.setSessionIsOpen(false);
                    updated = true;
                    auditWarning(Logger.MSG_DATA_AUDIT_INVALIDREFDELETED,
                            "runAuditLogbook(): "
                            + International.getString("Fahrtenbuch") + " " + logbookName + " "
                            + International.getMessage("Fahrtenbucheintrag #{entryno}", r.getEntryId().toString()) + ": "
                            + International.getMessage("Ungültige Referenz für {item} in Feld '{fieldname}' gelöscht.",
                            International.getString("Bootsstatus"),
                            International.getString("Fahrt offen (Boot unterwegs)")));
                }

                if (updated) {
                    if (correctErrors) {
                        logbook.dataAccess.update(r);
                    }
                }

                k = it.getNext();
            }

            if (!wasLogbookOpen && logbook != project.getCurrentLogbook()) {
                Logger.log(Logger.DEBUG, Logger.MSG_DATA_AUDIT,
                        "runAuditLogbook("+logbookName+"): Closing Logbook after Audit.");
                logbook.close();
            }

            return logbookErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditLogbook("+logbookName+") Caught Exception: " + e.toString());
            Logger.log(e);
            return ++logbookErr;
        }
    }

    private int runAuditClubworks() {
        int clubworkErr = 0;
        String[] clubworkNames = project.getAllClubworkNames();
        if (clubworkNames != null) {
            for (String s : clubworkNames) {
                try {
                    Clubwork c = Daten.project.getClubwork(s, true);
                    if (c == null) {
                        auditError(Logger.MSG_DATA_AUDIT_OBJECTCREATIONFAILED,
                                "runAuditClubworks(): "
                                + LogString.fileCreationFailed(s, International.getString("Vereinsarbeit")));
                    } else {
                        if (Daten.project.getCurrentClubwork() != c) {
                            // only close clubwork if it is not the currently opened one
                            c.close();
                        }
                    }
                } catch (Exception e) {
                    auditError(Logger.MSG_DATA_AUDIT,
                            "runAuditClubworks(" + s + ") Caught Exception: " + e.toString());
                    Logger.log(e);
                    return ++clubworkErr;
                }
            }
        }
        return clubworkErr;
    }

    private int runAuditPurgeDeletedRecords(StorageObject so, String itemDescription) {
        int purgeErr = 0;
        long now = System.currentTimeMillis();
        long purgeAfter = 0; /*( Daten.efaConfig.getValueDataDeletedRecordPurgeDays() < 0 ||
                            Daten.efaConfig.getValueDataDeletedRecordPurgeDays() == Long.MAX_VALUE ?
                                Long.MAX_VALUE : 
                                Math.abs(Daten.efaConfig.getValueDataDeletedRecordPurgeDays() * 24*60*60 ));*/
        try {
            DataKeyIterator it = so.dataAccess.getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                DataRecord r = (DataRecord)so.dataAccess.get(k);
                if (r != null && r.getDeleted() && r.getLastModified() > 0 &&
                    now - r.getLastModified() >= purgeAfter) {
                    if (correctErrors) {
                        so.dataAccess.delete(k);
                    }
                    auditWarning(Logger.MSG_DATA_AUDIT_RECPURGED,
                            "runAuditPurgeDeletedRecords(): "
                            + International.getMessage("{item} '{name}' war durch Benutzer zur Löschung markiert worden und wurde nun endgültig gelöscht.",
                            itemDescription, r.getQualifiedName()));
                }
                k = it.getNext();
            }
            return purgeErr;
        } catch (Exception e) {
            auditError(Logger.MSG_DATA_AUDIT,
                    "runAuditPurgeDeletedRecords() Caught Exception: " + e.toString());
            Logger.log(e);
            return ++purgeErr;
        }
    }

    public boolean runAudit() {
        if (auditRunning) {
            return true;
        }
        auditRunning = true;

        try {
            if (project == null || project.isInOpeningProject() || !project.isOpen()
                    || project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                return true;
            }
            errors = 0;
            warnings = 0;
            infos = 0;
            auditMessages = new StringBuilder();
            Logger.log(Logger.DEBUG, Logger.MSG_DATA_AUDIT,
                    "Starting Project Audit for Project: " + project.getProjectName());
            addMessageToBuffer("Audit Report for Project: " + project.getProjectName());
            try {
                runAuditPersistence(project.getSessionGroups(false), SessionGroups.DATATYPE);
                runAuditPersistence(project.getPersons(false), Persons.DATATYPE);
                runAuditPersistence(project.getStatus(false), Status.DATATYPE);
                runAuditPersistence(project.getGroups(false), Groups.DATATYPE);
                runAuditPersistence(project.getFahrtenabzeichen(false), Fahrtenabzeichen.DATATYPE);
                runAuditPersistence(project.getBoats(false), Boats.DATATYPE);
                runAuditPersistence(project.getCrews(false), Crews.DATATYPE);
                runAuditPersistence(project.getBoatStatus(false), BoatStatus.DATATYPE);
                runAuditPersistence(project.getBoatReservations(false), BoatReservations.DATATYPE);
                runAuditPersistence(project.getBoatDamages(false), BoatDamages.DATATYPE);
                runAuditPersistence(project.getDestinations(false), Destinations.DATATYPE);
                runAuditPersistence(project.getWaters(false), Waters.DATATYPE);
                runAuditPersistence(project.getMessages(false), Messages.DATATYPE);

                errors += runAuditProject();
                errors += runAuditBoats();
                errors += runAuditCrews();
                errors += runAuditGroups();
                errors += runAuditDestinations();
                errors += runAuditWaters();
                errors += runAuditStatus();
                errors += runAuditPersons();
                errors += runAuditFahrtenabzeichen();
                errors += runAuditMessages();
                errors += runAuditStatistics();
                String[] logbookNames = project.getAllLogbookNames();
                for (int i = 0; logbookNames != null && i < logbookNames.length; i++) {
                    errors += runAuditLogbook(logbookNames[i]);
                }
                if (errors == 0) {
                    runAuditPurgeDeletedRecords(project.getBoats(false),
                            International.getString("Boot"));
                    runAuditPurgeDeletedRecords(project.getPersons(false),
                            International.getString("Person"));
                    runAuditPurgeDeletedRecords(project.getDestinations(false),
                            International.getString("Ziel"));
                    runAuditPurgeDeletedRecords(project.getGroups(false),
                            International.getString("Gruppe"));
                }
                errors += runAuditClubworks();
            } catch (Exception e) {
                auditError(Logger.MSG_DATA_AUDIT,
                        "runAudit() Caught Exception: " + e.toString());
                Logger.log(e);
            }
            boolean logEnd = (errors > 0 || warnings > 0 || infos > 0);
            String s = Logger.log((errors == 0 ? (logEnd ? Logger.INFO : Logger.DEBUG) : Logger.ERROR),
                    Logger.MSG_DATA_AUDIT,
                    "Project Audit completed with " + errors + " Errors, " + warnings + " Warnings and "
                    + infos + " Infos.", false);
            if (errors > 0 || warnings > 0 && auditMessages != null) {
                addMessageToBuffer(s);
                Messages messages = (Daten.project != null ? Daten.project.getMessages(false) : null);
                if (messages != null && messages.isOpen()) {
                    messages.createAndSaveMessageRecord(MessageRecord.TO_ADMIN, 
                            "Audit Report", auditMessages.toString());
                }

            }
            return errors == 0;
        } finally {
            auditRunning = false;
        }
    }

    public void run() {
    	this.setName("AuditTask");
        runAudit();
    }

}
