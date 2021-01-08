/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Velten Heyn
 * @version 2
 */
package de.nmichael.efa.data;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.gui.EfaBoathouseFrame;
import de.nmichael.efa.gui.NewClubworkBookDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

import javax.swing.*;
import java.util.*;

// @i18n complete
public class Clubwork extends StorageObject {

    public static final String DATATYPE = "efa2clubwork";
    //    public ClubworkRecord staticClubworkRecord;
    private String name;
    private ProjectRecord projectRecord;
    private EfaBoathouseFrame efaBoathouseFrame;

    public Clubwork(int storageType,
                    String storageLocation,
                    String storageUsername,
                    String storagePassword,
                    String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE,
                International.getString("Vereinsarbeit") + " " + storageObjectName);
        ClubworkRecord.initialize();
//        staticClubworkRecord = (ClubworkRecord)createNewRecord();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new ClubworkRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public void setEfaBoathouseFrame(EfaBoathouseFrame efaBoathouseFrame) {
        this.efaBoathouseFrame = efaBoathouseFrame;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public DataTypeDate getStartDate() {
        return this.getProjectRecord().getStartDate();
    }

    public DataTypeDate getEndDate() {
        return this.getProjectRecord().getEndDate();
    }

    public ProjectRecord getProjectRecord() {
        if (projectRecord != null) {
            return projectRecord;
        } else {
            return Daten.project.getClubworkBookRecord(name);
        }
    }

    public void setProjectRecord(ProjectRecord r) {
        this.projectRecord = r;
    }

    public ClubworkRecord createClubworkRecord(UUID id) {
        ClubworkRecord r = new ClubworkRecord(this, MetaData.getMetaData(DATATYPE));
        r.setId(id);
        return r;
    }

    public Vector<ClubworkRecord> getAllClubworkRecords(boolean alsoDeleted, boolean alsoInvisible) {
        try {
            Vector<ClubworkRecord> v = new Vector<ClubworkRecord>();
            DataKeyIterator it = data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                ClubworkRecord r = (ClubworkRecord) data().get(k);
                if (r != null && ((r.getDeleted() && alsoDeleted)) && (!r.getInvisible() || alsoInvisible)) {
                    v.add(r);
                }
                k = it.getNext();
            }
            return v;
        } catch (Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, ClubworkRecord.ID);
            assertFieldNotEmpty(record, ClubworkRecord.PERSONID);
            assertFieldNotEmpty(record, ClubworkRecord.WORKDATE);
            assertFieldNotEmpty(record, ClubworkRecord.DESCRIPTION);
            assertFieldNotEmpty(record, ClubworkRecord.HOURS);
        }
    }

    public void doCarryOver(JDialog parent) {
        String title = International.getString("Vereinsarbeit-Übertrag");
        String message = International.getString("Möchtest du den Übertrag für das letzte Vereinsarbeitsbuch erstellen und in das aktuell offene "
                + "übertragen? Oder möchtest du den Übertrag für das aktuelle Buch erstellen und in das kommende Buch übertragen?");
        String inDieses = International.getString("Letztes in Dieses");
        String inNächstes = International.getString("Dieses in Kommendes");
        int num = Dialog.auswahlDialog(title, message, inDieses, inNächstes);

        if (num != 2 /* == CANCEL */) {
            doCarryOver(num, parent);
        }
    }

    public void doCarryOver(int thisOrNext, JDialog parent) {
        if (Daten.project == null || !Daten.project.isOpen()) {
            Dialog.error(International.getString("Kein Projekt geöffnet."));
            return;
        }
        Clubwork current = Daten.project.getCurrentClubwork();
        if (current == null || !current.isOpen()) {
            Dialog.error(International.getString("Kein Vereinsarbeitsbuch geöffnet."));
            return;
        }

        String[] names = Daten.project.getAllClubworkNames();
        DataTypeDate date = (thisOrNext == 0) ? new DataTypeDate(0) : new DataTypeDate(1, 1, 3000);
        Clubwork sourceOrTarget = null;
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) {
                continue;
            }

            Clubwork clubwork = Daten.project.getClubwork(names[i], false);
            if (thisOrNext == 0) {
                DataTypeDate start = clubwork.getStartDate();
                if (start.isBefore(current.getStartDate())) {
                    if (start.isAfter(date)) {
                        date = start;
                        sourceOrTarget = clubwork;
                    }
                }
            } else {
                DataTypeDate end = clubwork.getEndDate();
                if (end.isAfter(current.getEndDate())) {
                    if (end.isBefore(date)) {
                        date = end;
                        sourceOrTarget = clubwork;
                    }
                }
            }
        }

        if (thisOrNext == 0) {
            if (sourceOrTarget == null) {
                Dialog.error(International.getString("Kein vorheriges Vereinsarbeitsbuch gefunden."));
                return;
            }
            doCarryOver(sourceOrTarget, current);
        } else {
            if (sourceOrTarget == null) {
                int res = Dialog.yesNoDialog("Hinweis", International.getString("Kein kommendes Vereinsarbeitsbuch gefunden. Soll eines erstellt "
                        + "werden?"));
                if (res == Dialog.YES) {
                    NewClubworkBookDialog dlg = new NewClubworkBookDialog(parent);
                    String clubworkName = dlg.newClubworkBookDialog();
                    sourceOrTarget = Daten.project.getClubwork(clubworkName, false);
                } else {
                    return;
                }
            }
            doCarryOver(current, sourceOrTarget);
        }
    }

    public void doCarryOver(Clubwork from, Clubwork to) {
        ProjectRecord pr = Daten.project.getClubworkBookRecord(from.getName());
        if (pr == null) {
            Dialog.error(International.getMessage("Kein Vereinsarbeitsbuch '{name}' gefunden.", from.getName()));
            return;
        }

        long lock = -1;
        Hashtable<UUID, Double> hourAggregation = new Hashtable<UUID, Double>();
        try {
            DataKeyIterator it = from.data().getStaticIterator();
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                ClubworkRecord r = (ClubworkRecord) from.data().get(k);
                UUID personId = r.getPersonId();

                if (personId == null) {
                    continue;
                }
                Double hours = hourAggregation.get(personId);
                if (hours == null) {
                    hours = 0.0;
                }
                // aggregate
                hours += r.getHours();
                hourAggregation.put(personId, hours);
            }

            lock = to.data().acquireGlobalLock();
            it = to.data().getStaticIterator();
            boolean deleteOldCarryOver = false;
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                ClubworkRecord r = (ClubworkRecord) to.data().get(k);
                if (r.getFlag() == ClubworkRecord.Flags.CarryOver) {
                    if (deleteOldCarryOver == false) {
                        int res = Dialog.yesNoDialog("Hinweis", International.getString("Es existieren Überträge im Ziel-Vereinsarbeitsbuch. Diese "
                                + "löschen?"));
                        if (res == Dialog.YES) {
                            deleteOldCarryOver = true;
                            to.data().delete(k);
                        } else {
                            break;
                        }
                    } else {
                        to.data().delete(k);
                    }
                }
            }

            double sDefaultMonthlyClubworkTargetHours = pr.getDefaultMonthlyClubworkTargetHours();
            double sTransferableClubworkHours = pr.getTransferableClubworkHours();

            // Save Carry Over
            Set<Map.Entry<UUID, Double>> entries = hourAggregation.entrySet();
            if (entries.size() > 0) {
                int successSaved = 0;
                for (Map.Entry<UUID, Double> entry : entries) {
                    UUID personId = entry.getKey();
                    Double hours = entry.getValue();

                    Persons persons = Daten.project.getPersons(false);
                    Integer month = from.getStartDate().getMonthsDifference(from.getEndDate());
                    if (personId != null && persons != null) {
                        PersonRecord[] personRecords = persons.getPersons(personId, from.getStartDate().getTimestamp(null),
                                from.getEndDate().getTimestamp(null));
                        if (personRecords != null && personRecords.length > 0) {
                            for (int i = 0; i < personRecords.length; i++) {
                                //vh if (personRecords[i].isStatusMember()) {
                                    month = personMemberMonthToFullYear(personRecords[i], month, from);
                                //}
                            }
                        }
                    }

                    double clubworkTargetHours = Math.round(sDefaultMonthlyClubworkTargetHours * month * 100) / 100d;
                    double max = clubworkTargetHours + sTransferableClubworkHours;
                    double min = clubworkTargetHours - sTransferableClubworkHours;

                    ClubworkRecord record = to.createClubworkRecord(UUID.randomUUID());
                    record.setPersonId(personId);
                    record.setWorkDate(DataTypeDate.today());
                    record.setDescription(International.getString("Übertrag"));
                    record.setFlag(ClubworkRecord.Flags.CarryOver);

                    if (hours == null) {
                        hours = 0.0;
                    } else if (hours > max) {
                        record.setHours(sTransferableClubworkHours);
                    } else if (hours < min) {
                        record.setHours(-sTransferableClubworkHours);
                    } else {
                        record.setHours(hours - clubworkTargetHours);
                    }

                    try {
                        to.data().add(record);
                        successSaved++;
                    } catch (Exception eignore) {
                        Logger.logdebug(eignore);
                    }
                }
                if (successSaved > 0) {
                    Dialog.infoDialog(International.getMessage("{thing} erfolgreich berechnet.", International.getString("Übertrag")));
                    efaBoathouseFrame.openClubwork(to.getName());
                } else {
                    Dialog.error(International.getMessage("{thing} konnte nicht berechnet werden!", International.getString("Übertrag")));
                }
            } else {
                Dialog.error(International.getMessage("Keine Einträge im aktuellen {book} gefunden!", International.getString("Vereinsarbeitsbuch")));
            }

            // Save Yearly Credit
			/*Vector<PersonRecord> persons = Daten.project.getPersons(false).getAllPersons(-1, false, false);

             Iterator<PersonRecord> personItr = persons.iterator();
             while (personItr.hasNext()) {
             PersonRecord person = personItr.next();

             ClubworkRecord record = clubwork.createClubworkRecord(UUID.randomUUID());
             record.setPersonId(person.getId());
             record.setWorkDate(DataTypeDate.today());
             record.setDescription(International.getString("Gutschrift (jährlich)"));
             record.setHours(person.getYearlyClubworkCredit());

             try {
             clubwork.data().add(record);
             } catch (Exception eignore) {
             Logger.logdebug(eignore);
             }
             }*/
        } catch (Exception e) {
            String message = International.getString("efa hat soeben versucht den Übertrag für die Vereinsarbeit zu berechnen.") + "\n"
                    + International.getString("Bei diesem Vorgang traten jedoch FEHLER auf.") + "\n\n"
                    + International.getString("Ein Protokoll ist in der Logdatei (Admin-Modus: Logdatei anzeigen) zu finden.");

            Dialog.infoDialog(International.getString("Vereinsarbeit-Übertrag"), message);
            Logger.logdebug(e);
            Logger.log(Logger.ERROR, Logger.MSG_ERR_GENERIC,
                    LogString.operationAborted(International.getString("Vereinsarbeit-Übertrag")));
            Messages messages = Daten.project.getMessages(false);
            messages.createAndSaveMessageRecord(Daten.EFA_SHORTNAME, MessageRecord.TO_ADMIN,
                    (String) null, International.getString("Vereinsarbeit-Übertrag"),
                    message);
        } finally {
            if (to != null && lock >= 0) {
                to.data().releaseGlobalLock(lock);
            }
        }
    }

    private static Integer personMemberMonthToFullYear(PersonRecord person, int month, Clubwork fromClubwork) {
        long fromLong = person.getValidFrom();
        long toLong = person.getInvalidFrom();

        if (fromLong > 0) {
            DataTypeDate from = new DataTypeDate(fromLong);
            if (fromClubwork.getStartDate().isBeforeOrEqual(from) && fromClubwork.getEndDate().isAfter(from)) {
                month -= from.getMonth();
            }
        }
        if (toLong > 0) {
            DataTypeDate to = new DataTypeDate(toLong);
            if (fromClubwork.getEndDate().isAfterOrEqual(to) && fromClubwork.getStartDate().isBefore(to)) {
                month -= (12 - to.getMonth() - 1);
            }
        }
        return month;
    }
}
