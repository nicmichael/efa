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

import de.nmichael.efa.util.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.ex.EfaModifyException;
import java.util.*;

// @i18n complete

public class Persons extends StorageObject {

    public static final String DATATYPE = "efa2persons";
    public PersonRecord staticPersonRecord;

    public Persons(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Personen"));
        PersonRecord.initialize();
        staticPersonRecord = (PersonRecord)createNewRecord();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new PersonRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public PersonRecord createPersonRecord(UUID id) {
        PersonRecord r = new PersonRecord(this, MetaData.getMetaData(DATATYPE));
        r.setId(id);
        return r;
    }

    public PersonRecord getPerson(UUID id, long validAt) {
        try {
            return (PersonRecord)data().getValidAt(PersonRecord.getKey(id, validAt), validAt);
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public PersonRecord getPerson(UUID id, long earliestValidAt, long latestValidAt, long preferredValidAt) {
        try {
            return (PersonRecord)data().getValidNearest(PersonRecord.getKey(id, preferredValidAt), earliestValidAt, latestValidAt, preferredValidAt);
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    // find a record being valid at the specified time
    public PersonRecord getPerson(String personName, long validAt) {
        try {
            DataKey[] keys = data().getByFields(
                staticPersonRecord.getQualifiedNameFields(), staticPersonRecord.getQualifiedNameValues(personName), validAt);
            if (keys == null || keys.length < 1) {
                return null;
            }
            if (validAt < 0 && keys.length > 1) {
                // instead of returning just any person, first search for one that is valid today
                long now = System.currentTimeMillis();
                for (int i = 0; i < keys.length; i++) {
                    PersonRecord r = (PersonRecord) data().get(keys[i]);
                    if (r.isValidAt(now)) {
                        return r;
                    }
                }
            }
            for (int i=0; i<keys.length; i++) {
                PersonRecord r = (PersonRecord)data().get(keys[i]);
                if (r.isValidAt(validAt)) {
                    return r;
                }
            }
            return null;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    // find all records being valid at the specified time
    public PersonRecord[] getPersons(String personName, long validAt) {
        try {
            DataKey[] keys = data().getByFields(
                staticPersonRecord.getQualifiedNameFields(), staticPersonRecord.getQualifiedNameValues(personName), validAt);
            if (keys == null || keys.length < 1) {
                return null;
            }
            ArrayList<PersonRecord> list = new ArrayList<PersonRecord>();
            for (int i=0; i<keys.length; i++) {
                PersonRecord r = (PersonRecord)data().get(keys[i]);
                if (r.isValidAt(validAt)) {
                    list.add(r);
                }
            }
            if (list.size() > 0) {
                return list.toArray(new PersonRecord[0]);
            } else {
                return null;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

	// find all records being valid at the specified time
	public PersonRecord[] getPersons(UUID id, long validFrom, long validUntil) {
		try {
			DataKey[] keys = data().getByFields(
					new String[] { PersonRecord.ID }, new Object[] { id });
			if (keys == null || keys.length < 1) {
				return null;
			}
			ArrayList<PersonRecord> list = new ArrayList<PersonRecord>();
			for (int i=0; i<keys.length; i++) {
				PersonRecord r = (PersonRecord)data().get(keys[i]);
				if (r.isInValidityRange(validFrom, validUntil)) {
					list.add(r);
				}
			}
			if (list.size() > 0) {
				return list.toArray(new PersonRecord[0]);
			} else {
				return null;
			}
		} catch(Exception e) {
			Logger.logdebug(e);
			return null;
		}
	}

    // find any record being valid at least partially in the specified range
    public PersonRecord getPerson(String personName, long validFrom, long validUntil, long preferredValidAt) {
        try {
            DataKey[] keys = data().getByFields(
                staticPersonRecord.getQualifiedNameFields(), staticPersonRecord.getQualifiedNameValues(personName));
            if (keys == null || keys.length < 1) {
                return null;
            }
            PersonRecord candidate = null;
            for (int i=0; i<keys.length; i++) {
                PersonRecord r = (PersonRecord)data().get(keys[i]);
                if (r != null) {
                    if (r.isInValidityRange(validFrom, validUntil)) {
                        candidate = r;
                        if (preferredValidAt >= r.getValidFrom() && preferredValidAt < r.getInvalidFrom()) {
                            return r;
                        }
                    }
                }
            }
            return candidate;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public Vector<PersonRecord> getAllPersons(long validAt, boolean alsoDeleted, boolean alsoInvisible) {
        try {
            Vector<PersonRecord> v = new Vector<PersonRecord>();
            DataKeyIterator it = data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                PersonRecord r = (PersonRecord) data().get(k);
                if (r != null && (r.isValidAt(validAt) || (r.getDeleted() && alsoDeleted)) && (!r.getInvisible() || alsoInvisible)) {
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

    public Vector<PersonRecord> getAllPersons(long validFrom, long validUntil, boolean alsoDeleted, boolean alsoInvisible) {
        try {
            Vector<PersonRecord> v = new Vector<PersonRecord>();
            DataKeyIterator it = data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                PersonRecord r = (PersonRecord) data().get(k);
                if (r != null && (r.isInValidityRange(validFrom, validUntil) || (r.getDeleted() && alsoDeleted)) && (!r.getInvisible() || alsoInvisible)) {
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

    public boolean isPersonExist(UUID id) {
        try {
            DataRecord[] r = data().getValidAny(PersonRecord.getKey(id, -1));
            return r != null && r.length > 0;
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }

    public boolean isPersonDeleted(UUID personId) {
        try {
            DataRecord[] records = data().getValidAny(PersonRecord.getKey(personId, -1));
            if (records != null && records.length > 0) {
                return records[0].getDeleted();
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return false;
    }

    public int getNumberOfMembers(long tstmp, boolean withoutMembersExcludedFromCompetition) {
        try {
            DataKeyIterator it = dataAccess.getStaticIterator();
            DataKey k = it.getFirst();
            // actually, checking for records valid at tstmp should already
            // give us unique records, so there should be no need to use
            // a Hashtable to make sure we don't cound a person twice. But, well,
            // you never know...
            Hashtable<UUID,DataKey> uuids = new Hashtable<UUID,DataKey>();
            while (k != null) {
                PersonRecord p = (PersonRecord) dataAccess.get(k);
                if (p != null && p.isValidAt(tstmp) && !p.getDeleted() &&
                    p.isStatusMember() &&
                    (!withoutMembersExcludedFromCompetition || !p.getExcludeFromCompetition())) {
                    uuids.put(p.getId(), k);
                }
                k = it.getNext();
            }
            return uuids.size();
        } catch (Exception e) {
            Logger.log(e);
            return -1;
        }
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, PersonRecord.ID);
            assertFieldNotEmpty(record, PersonRecord.FIRSTLASTNAME);
        }
        if (delete) {
            assertNotReferenced(record, getProject().getFahrtenabzeichen(false), new String[] { FahrtenabzeichenRecord.PERSONID });
            assertNotReferenced(record, getProject().getGroups(false), new String[] { GroupRecord.MEMBERIDLIST });
            assertNotReferenced(record, getProject().getCrews(false), new String[] { CrewRecord.COXID,
                                                                                     CrewRecord.CREW1ID,
                                                                                     CrewRecord.CREW2ID,
                                                                                     CrewRecord.CREW3ID,
                                                                                     CrewRecord.CREW4ID,
                                                                                     CrewRecord.CREW5ID,
                                                                                     CrewRecord.CREW6ID,
                                                                                     CrewRecord.CREW7ID,
                                                                                     CrewRecord.CREW8ID,
                                                                                     CrewRecord.CREW9ID,
                                                                                     CrewRecord.CREW10ID,
                                                                                     CrewRecord.CREW11ID,
                                                                                     CrewRecord.CREW12ID,
                                                                                     CrewRecord.CREW14ID,
                                                                                     CrewRecord.CREW11ID,
                                                                                     CrewRecord.CREW16ID,
                                                                                     CrewRecord.CREW11ID,
                                                                                     CrewRecord.CREW17ID,
                                                                                     CrewRecord.CREW18ID,
                                                                                     CrewRecord.CREW19ID,
                                                                                     CrewRecord.CREW20ID,
                                                                                     CrewRecord.CREW21ID,
                                                                                     CrewRecord.CREW22ID,
                                                                                     CrewRecord.CREW23ID,
                                                                                     CrewRecord.CREW24ID
                                                                                   }, false);
            assertNotReferenced(record, getProject().getBoatDamages(false), new String[] { BoatDamageRecord.REPORTEDBYPERSONID,
                                                                                           BoatDamageRecord.FIXEDBYPERSONID},
                                                                                           false);
            assertNotReferenced(record, getProject().getBoatReservations(false), new String[] { BoatReservationRecord.PERSONID });
            String[] logbooks = getProject().getAllLogbookNames();
            for (int i=0; logbooks != null && i<logbooks.length; i++) {
                assertNotReferenced(record, getProject().getLogbook(logbooks[i], false), new String[] {
                                                                                       LogbookRecord.COXID,
                                                                                       LogbookRecord.CREW1ID,
                                                                                       LogbookRecord.CREW2ID,
                                                                                       LogbookRecord.CREW3ID,
                                                                                       LogbookRecord.CREW4ID,
                                                                                       LogbookRecord.CREW5ID,
                                                                                       LogbookRecord.CREW6ID,
                                                                                       LogbookRecord.CREW7ID,
                                                                                       LogbookRecord.CREW8ID,
                                                                                       LogbookRecord.CREW9ID,
                                                                                       LogbookRecord.CREW10ID,
                                                                                       LogbookRecord.CREW11ID,
                                                                                       LogbookRecord.CREW12ID,
                                                                                       LogbookRecord.CREW13ID,
                                                                                       LogbookRecord.CREW14ID,
                                                                                       LogbookRecord.CREW15ID,
                                                                                       LogbookRecord.CREW16ID,
                                                                                       LogbookRecord.CREW17ID,
                                                                                       LogbookRecord.CREW18ID,
                                                                                       LogbookRecord.CREW19ID,
                                                                                       LogbookRecord.CREW20ID,
                                                                                       LogbookRecord.CREW21ID,
                                                                                       LogbookRecord.CREW22ID,
                                                                                       LogbookRecord.CREW23ID,
                                                                                       LogbookRecord.CREW24ID,
                                                                                      }, false );
            }
        }
    }

    public ProgressTask getMergePersonsProgressTask(DataKey mainKey, DataKey[] mergeKeys) {
        return new MergePersonsProgressTask(this, mainKey, mergeKeys);
    }

    class MergePersonsProgressTask extends ProgressTask {

        private Persons persons;
        private UUID mainID;
        private UUID[] mergeIDs;
        private int absoluteWork = 100;
        private int errorCount = 0;
        private int warningCount = 0;
        private int updateCount = 0;

        public MergePersonsProgressTask(Persons persons, DataKey mainKey, DataKey[] mergeKeys) {
            this.persons = persons;
            mainID = (UUID)mainKey.getKeyPart1();
            mergeIDs = new UUID[mergeKeys.length];
            for (int i=0; i<mergeIDs.length; i++) {
                mergeIDs[i] = (UUID)mergeKeys[i].getKeyPart1();
            }
        }

        public int getAbsoluteWork() {
            return absoluteWork;
        }

        public String getSuccessfullyDoneMessage() {
            return International.getString("Datensätze erfolgreich zusammengefügt.");
        }

        private boolean isIdToBeMerged(UUID id) {
            if (id == null) {
                return false;
            }
            boolean found = false;
            for (int i=0; i<mergeIDs.length; i++) {
                if (id.equals(mergeIDs[i])) {
                    return true;
                }
            }
            return false;
        }

        public void run() {
            setRunning(true);
            logInfo(International.getString("Datensätze zusammenfügen") + " ...\n" +
                    (errorCount > 0 || warningCount > 0 ?
                        "\n[" + errorCount + " ERRORS, " + warningCount + " WARNINGS]" : ""));
            try {
                Project p = persons.getProject();
                super.resultSuccess = false;

                // Search Logbooks
                String[] logbookNames = p.getAllLogbookNames();
                absoluteWork = (logbookNames != null ? logbookNames.length : 0) + 7;
                int workDone = 0;
                if (logbookNames != null) {
                    for (String logbookName : logbookNames) {
                        logInfo("Searching logbook " + logbookName + " ...\n");
                        Logbook logbook = p.getLogbook(logbookName, false);
                        DataKeyIterator it = logbook.data().getStaticIterator();
                        for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                            LogbookRecord r = (LogbookRecord) logbook.data().get(k);
                            if (r != null) {
                                boolean changed = false;
                                for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                                    if (isIdToBeMerged(r.getCrewId(i))) {
                                        r.setCrewId(i, mainID);
                                        changed = true;
                                    }
                                }
                                if (changed) {
                                    logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                                    logbook.data().update(r);
                                    updateCount++;
                                }
                            }
                        }
                        setCurrentWorkDone(++workDone);
                    }
                }

                // @todo (P2) - merge ID's for Clubwork

                // Search Boat Damages
                logInfo("Searching Boat Damages ...\n");
                BoatDamages boatDamages = p.getBoatDamages(false);
                DataKeyIterator it = boatDamages.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    BoatDamageRecord r = (BoatDamageRecord) boatDamages.data().get(k);
                    if (r != null) {
                        boolean changed = false;
                        if (isIdToBeMerged(r.getReportedByPersonId())) {
                            r.setReportedByPersonId(mainID);
                            changed = true;
                        }
                        if (isIdToBeMerged(r.getFixedByPersonId())) {
                            r.setFixedByPersonId(mainID);
                            changed = true;
                        }
                        if (changed) {
                            logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                            boatDamages.data().update(r);
                            updateCount++;
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Search Boat Reservations
                logInfo("Searching Boat Reservations ...\n");
                BoatReservations boatReservations = p.getBoatReservations(false);
                it = boatReservations.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    BoatReservationRecord r = (BoatReservationRecord) boatReservations.data().get(k);
                    if (r != null) {
                        boolean changed = false;
                        if (isIdToBeMerged(r.getPersonId())) {
                            r.setPersonId(mainID);
                            changed = true;
                        }
                        if (changed) {
                            logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                            boatReservations.data().update(r);
                            updateCount++;
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Search Crews
                logInfo("Searching Crews ...\n");
                Crews crews = p.getCrews(false);
                it = crews.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    CrewRecord r = (CrewRecord) crews.data().get(k);
                    if (r != null) {
                        boolean changed = false;
                        for (int i=0; i<=LogbookRecord.CREW_MAX; i++) {
                            if (isIdToBeMerged(r.getCrewId(i))) {
                                r.setCrewId(i, mainID);
                                changed = true;
                            }
                        }
                        if (changed) {
                            logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                            crews.data().update(r);
                            updateCount++;
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Search Fahrtenabzeichen
                logInfo("Searching Fahrtenabzeichen ...\n");
                Fahrtenabzeichen fahrtenabzeichen = p.getFahrtenabzeichen(false);
                if (fahrtenabzeichen != null) {
                    it = fahrtenabzeichen.data().getStaticIterator();
                    for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                        FahrtenabzeichenRecord r = (FahrtenabzeichenRecord) fahrtenabzeichen.data().get(k);
                        if (r != null) {
                            boolean changed = false;
                            if (isIdToBeMerged(r.getPersonId())) {
                                fahrtenabzeichen.data().delete(r.getKey());
                                r.setPersonId(mainID);
                                fahrtenabzeichen.data().add(r);
                                changed = true;
                            }
                            if (changed) {
                                logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                                fahrtenabzeichen.data().update(r);
                                updateCount++;
                            }
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Search Groups
                logInfo("Searching Groups ...\n");
                Groups groups = p.getGroups(false);
                it = groups.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    GroupRecord r = (GroupRecord) groups.data().get(k);
                    if (r != null) {
                        boolean changed = false;
                        DataTypeList<UUID> list = r.getMemberIdList();
                        for (int i=0; list != null && i<list.length(); i++) {
                            if (isIdToBeMerged(list.get(i))) {
                                list.set(i, mainID);
                                changed = true;
                            }
                        }
                        if (changed) {
                            logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                            r.setMemberIdList(list);
                            groups.data().update(r);
                            updateCount++;
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Search Statistics
                logInfo("Searching Statistics ...\n");
                Statistics statistics = p.getStatistics(false);
                it = statistics.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    StatisticsRecord r = (StatisticsRecord) statistics.data().get(k);
                    if (r != null) {
                        boolean changed = false;
                        if (isIdToBeMerged(r.getFilterByPersonId())) {
                            r.setFilterByPersonId(mainID);
                            changed = true;
                        }
                        if (changed) {
                            logInfo("Updating record " + r.getQualifiedName() + " ...\n");
                            statistics.data().update(r);
                            updateCount++;
                        }
                    }
                }
                setCurrentWorkDone(++workDone);

                // Merge Person and delete old Persons
                logInfo("Merging Records ...\n");
                DataRecord[] mainPerson = persons.data().getValidAny(PersonRecord.getKey(mainID, 0));
                ArrayList<DataRecord> mergedPersons = new ArrayList<DataRecord>();
                for (int i=0; i<mergeIDs.length; i++) {
                    DataRecord[] mergePerson = persons.data().getValidAny(PersonRecord.getKey(mergeIDs[i], 0));
                    for (int j=0; mergePerson != null && j<mergePerson.length; j++) {
                        mergedPersons.add(mergePerson[j]);
                    }
                }
                long validFrom = Long.MAX_VALUE;
                long invalidFrom = Long.MIN_VALUE;
                for (int i=0; mainPerson != null && i<mainPerson.length; i++) {
                    if (mainPerson[i].getValidFrom() < validFrom) {
                        validFrom = mainPerson[i].getValidFrom();
                    }
                    if (mainPerson[i].getInvalidFrom() > invalidFrom) {
                        invalidFrom = mainPerson[i].getInvalidFrom();
                    }
                }
                Hashtable<Long,DataRecord> validStart = new Hashtable<Long,DataRecord>();
                for (int i=0; i<mergedPersons.size(); i++) {
                    PersonRecord r = (PersonRecord)mergedPersons.get(i).cloneRecord();
                    if (r.getValidFrom() < validFrom && validStart.get(r.getValidFrom()) == null) {
                        r.setId(mainID);
                        logInfo("Merging " + r.getQualifiedName() + " (" + r.getValidRangeString() + ") ...\n");
                        persons.data().addValidAt(r, Long.MIN_VALUE);
                        validStart.put(r.getValidFrom(), r);
                        validFrom = r.getValidFrom();
                    } else if (r.getInvalidFrom() > invalidFrom && validStart.get(invalidFrom) == null) {
                        r.setId(mainID);
                        logInfo("Merging " + r.getQualifiedName() + " (" + r.getValidRangeString() + ") ...\n");
                        persons.data().addValidAt(r, Long.MAX_VALUE);
                        validStart.put(invalidFrom, r);
                        invalidFrom = r.getInvalidFrom();
                    }
                }
                // Deleting merged Records
                logInfo("Deleting merged Records ...\n");
                for (int i=0; i<mergedPersons.size(); i++) {
                    PersonRecord r = (PersonRecord)mergedPersons.get(i);
                    logInfo("Deleting " + r.getQualifiedName() + " (" + r.getValidRangeString() + ") ...\n");
                    persons.data().delete(r.getKey());
                }
                setCurrentWorkDone(++workDone);
                super.resultSuccess = true;

            } catch(Exception e) {
                errorCount++;
                this.logInfo("\n" + International.getString("Fehler") + ": " + e.getMessage());
                this.logInfo("\n" + e.toString());
                Logger.logdebug(e);
            }
            this.logInfo("\n" + International.getMessage("{count} Datensätze wurden aktualisiert.", updateCount));
            this.logInfo("\n\n" + International.getMessage("{count} Fehler.", errorCount));
            this.logInfo("\n" + International.getMessage("{count} Warnungen.", warningCount));
            setDone();
        }
    }

}
