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

public class Groups extends StorageObject {

    public static final String DATATYPE = "efa2groups";
    public GroupRecord staticGroupRecord;

    public Groups(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Gruppen"));
        GroupRecord.initialize();
        staticGroupRecord = (GroupRecord)createNewRecord();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new GroupRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public GroupRecord createGroupRecord(UUID id) {
        GroupRecord r = new GroupRecord(this, MetaData.getMetaData(DATATYPE));
        r.setId(id);
        return r;
    }

    public GroupRecord findGroupRecord(UUID id, long validAt) {
        try {
            return (GroupRecord)data().getValidAt(GroupRecord.getKey(id, -1), validAt);
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public GroupRecord[] getGroupsForPerson(UUID personId, long validAt) {
        return getGroupsForPerson(personId, validAt, -1, -1);
    }

    public GroupRecord[] getGroupsForPerson(UUID personId, long validFrom, long validUntil) {
        return getGroupsForPerson(personId, -1, validFrom, validUntil);
    }

    private GroupRecord[] getGroupsForPerson(UUID personId, long validAt, long validFrom, long validUntil) {
        ArrayList<GroupRecord> groups = new ArrayList<GroupRecord>();
        try {
            DataKeyIterator it = data().getStaticIterator();
            DataKey k = it.getFirst();
            Hashtable<UUID,String> uniqueList = new Hashtable<UUID,String>();
            while (k != null) {
                GroupRecord r = (GroupRecord)data().get(k);
                if ( (validAt != -1 && r.isValidAt(validAt)) ||
                     (validFrom != -1 && validUntil != -1 && r.isInValidityRange(validFrom, validUntil)) ) {
                    DataTypeList<UUID> memberList = r.getMemberIdList();
                    if (memberList != null && memberList.contains(personId)) {
                        if (uniqueList.get(r.getId()) == null) {
                            groups.add(r);
                            uniqueList.put(r.getId(), "foo");
                        }
                    }
                }
                k = it.getNext();
            }
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
        if (groups.size() == 0) {
            return null;
        } else {
            return groups.toArray(new GroupRecord[0]);
        }
    }

    public void setGroupsForPerson(UUID personId, UUID[] groupIdList, long validAt) {
        try {
            DataKeyIterator it = data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                GroupRecord r = (GroupRecord)data().get(k);
                if (r.isValidAt(validAt)) {
                    boolean personToBeIngroup = false;
                    for (UUID id : groupIdList) {
                        if (r.getId().equals(id)) {
                            personToBeIngroup = true;
                            break;
                        }
                    }
                    if (r.setPersonInGroup(personId, personToBeIngroup)) {
                        data().update(r);
                    }
                }
                k = it.getNext();
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
    }

    public GroupRecord findGroupRecord(String groupName, long validAt) {
        try {
            DataKey[] keys = data().getByFields(
                staticGroupRecord.getQualifiedNameFields(), staticGroupRecord.getQualifiedNameValues(groupName), validAt);
            if (keys == null || keys.length < 1) {
                return null;
            }
            for (int i=0; i<keys.length; i++) {
                GroupRecord r = (GroupRecord)data().get(keys[i]);
                if (r != null && r.isValidAt(validAt)) {
                    return r;
                }
            }
            return null;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }


    public boolean isGroupDeleted(UUID groupId) {
        try {
            DataRecord[] records = data().getValidAny(GroupRecord.getKey(groupId, -1));
            if (records != null && records.length > 0) {
                return records[0].getDeleted();
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return false;
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, GroupRecord.ID);
            assertFieldNotEmpty(record, GroupRecord.NAME);
            // the following assertion cannot be enforced, as it forbids to split a 
            // version and create multi-versionized entries
            // assertUnique(record, GroupRecord.NAME);
        }
        if (delete) {
            assertNotReferenced(record, getProject().getBoats(false), new String[] { BoatRecord.ALLOWEDGROUPIDLIST });
            assertNotReferenced(record, getProject().getBoats(false), new String[] { BoatRecord.REQUIREDGROUPID });
        }
    }

}
