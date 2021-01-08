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

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.EfaModifyException;
import java.util.UUID;

// @i18n complete

public class Messages extends StorageObject {

    public static final String DATATYPE = "efa2messages";

    public Messages(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Nachrichten"));
        MessageRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new MessageRecord(this, MetaData.getMetaData(DATATYPE));
    }

    private long getNextMessageId() {
        AutoIncrement autoIncrement = getProject().getAutoIncrement(false);

        int tries = 0;
        long val = 0;
        try {
            while (tries++ < 100) {
                // usually autoincrement should always give a unique new id.
                // but in case our id's got out of sync, we try up to 100 times to fine a
                // new unique reservation id.
                val = autoIncrement.nextAutoIncrementLongValue(data().getStorageObjectType());
                if (val <= 0) {
                    break;
                }
                if (data().get(MessageRecord.getKey(val)) == null) {
                    break;
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return val;
    }

    public MessageRecord createMessageRecord() {
        MessageRecord r = new MessageRecord(this, MetaData.getMetaData(DATATYPE));
        r.setMessageId(getNextMessageId());
        r.setDate(DataTypeDate.today());
        r.setTime(DataTypeTime.now());
        r.setToBeMailed(true);
        return r;
    }

    public MessageRecord createAndSaveMessageRecord(String to, String subject, String text) {
        return createAndSaveMessageRecord(Daten.EFA_SHORTNAME, to, (String)null, subject, text);
    }

    public MessageRecord createAndSaveMessageRecord(String from, String to, UUID replyTo,
            String subject, String text) {
        String email = null;
        try {
            PersonRecord p = Daten.project.getPersons(false).getPerson(replyTo, System.currentTimeMillis());
            if (p != null && p.getEmail() != null && p.getEmail().length() > 0) {
                email = p.getEmail();
            }
        } catch(Exception eignore) {
        }
        return createAndSaveMessageRecord(from, to, email, subject, text);
    }
    
    public MessageRecord createAndSaveMessageRecord(String from, String to, String replyTo,
            String subject, String text) {
        MessageRecord r = new MessageRecord(this, MetaData.getMetaData(DATATYPE));
        r.setMessageId(getNextMessageId());
        r.setDate(DataTypeDate.today());
        r.setTime(DataTypeTime.now());
        r.setTo(to);
        if (replyTo != null && replyTo.trim().length() > 0) {
            r.setReplyTo(replyTo.trim());
        }
        r.setFrom(from);
        r.setSubject(subject);
        r.setText(text);
        r.setToBeMailed(true);
        if (Daten.efaConfig.getValueNotificationMarkReadAdmin() &&
                MessageRecord.TO_ADMIN.equals(to)) {
            r.setRead(true);
        }
        if (Daten.efaConfig.getValueNotificationMarkReadBoatMaintenance() &&
                MessageRecord.TO_BOATMAINTENANCE.equals(to)) {
            r.setRead(true);
        }
        try {
            data().add(r);
        } catch(Exception e) {
            Logger.log(Logger.WARNING, Logger.MSG_WARN_SAVEMESSAGE,
                    "Save Message Failed: " + e.toString());
            Logger.logdebug(e);
        }
        return r;
    }

    public long countUnreadMessages() {
        long lockId = -1;
        try {
            lockId = data().acquireGlobalLock();
            long totalCnt = data().getNumberOfRecords();
            // we cannot explicitly count records with READ set to false, as "false" is a value not stored in the record.
            // therefore, we have to count the true's, and substract them from the total count.
            long msgRead = data().countRecords(new String[] { MessageRecord.READ }, new Object[] { new Boolean(true) });
            return totalCnt - msgRead;
        } catch(Exception e) {
            Logger.logdebug(e);
            return -1;
        } finally {
            if (lockId >= 0) {
                data().releaseGlobalLock(lockId);
            }
        }
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, MessageRecord.MESSAGEID);
        }
    }

}
