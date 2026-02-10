/*
 * <pre>
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael, Martin Glade
 * @version 2</pre>
 */
package de.nmichael.efa.data.efacloud;

import de.nmichael.efa.core.Backup;
import de.nmichael.efa.data.storage.EfaCloudStorage;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.util.International;

import java.util.*;

public class TableBuilder {

    /**
     * IDataAccess interface data type name mapping to MySQL data conventions. All SQL data types allow NULL and all use
     * it as Default. Strings are by default Varchar(256), only in eight cases, they are made to "Text", see also
     * efa2fieldSpecialDefinitions.
     */
    private static final HashMap<Integer, String> datatypeDefaults = new HashMap<Integer, String>();
    private static final HashMap<Integer, Integer> datatypeMaxlen = new HashMap<Integer, Integer>();

    static {
        // Note to length definitions: limiting are the Logbook and Statistics tables, which hit
        // the 65k row length limitation of MySQL. Therefor "DATA_STRING" is only 192 characters
        // and DATA_LIST_STRING not more than "1480". The table statistics has then only 4.400
        // characters left. Entries are cut as soon as they reach the limit - 2 characters.
        datatypeDefaults.put(IDataAccess.DATA_BOOLEAN, "Varchar(12) NULL DEFAULT NULL"); // true, false
        datatypeDefaults.put(IDataAccess.DATA_DATE, "Date NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_DECIMAL, "Varchar(64) NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_DISTANCE, "Varchar(64) NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_DOUBLE, "Double NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_INTEGER, "Int NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_INTSTRING, "Varchar(64) NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_LIST_INTEGER, "Varchar(192) NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_LIST_STRING, "Varchar(1480) NULL DEFAULT NULL");  // see above
        datatypeDefaults.put(IDataAccess.DATA_LIST_UUID, "Varchar(1024) NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_LONGINT, "BigInt NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_PASSWORDC, "Varchar(256) NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_PASSWORDH, "Varchar(256) NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_STRING, "Varchar(192) NULL DEFAULT NULL");  // see above
        datatypeDefaults.put(IDataAccess.DATA_TEXT, "Text(65535) NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_TIME, "Time NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_UUID, "Varchar(64) NULL DEFAULT NULL");
        datatypeDefaults.put(IDataAccess.DATA_VIRTUAL, "Varchar(256) NULL DEFAULT NULL");
    }

    /**
     * IDataAccess interface data type index to name mapping. Needed to resolve the efa2fieldSpecialDefinitions Strings
     */
    private static final HashMap<Integer, String> datatypeStrings = new HashMap<Integer, String>();

    static {
        datatypeStrings.put(IDataAccess.DATA_STRING, "DATA_STRING");
        datatypeStrings.put(IDataAccess.DATA_INTEGER, "DATA_INTEGER");
        datatypeStrings.put(IDataAccess.DATA_LONGINT, "DATA_LONGINT");
        datatypeStrings.put(IDataAccess.DATA_DOUBLE, "DATA_DOUBLE");
        datatypeStrings.put(IDataAccess.DATA_DECIMAL, "DATA_DECIMAL");
        datatypeStrings.put(IDataAccess.DATA_DISTANCE, "DATA_DISTANCE");
        datatypeStrings.put(IDataAccess.DATA_BOOLEAN, "DATA_BOOLEAN");
        datatypeStrings.put(IDataAccess.DATA_DATE, "DATA_DATE");
        datatypeStrings.put(IDataAccess.DATA_TIME, "DATA_TIME");
        datatypeStrings.put(IDataAccess.DATA_UUID, "DATA_UUID");
        datatypeStrings.put(IDataAccess.DATA_TEXT, "DATA_TEXT");
        datatypeStrings.put(IDataAccess.DATA_INTSTRING, "DATA_INTSTRING");
        datatypeStrings.put(IDataAccess.DATA_PASSWORDH, "DATA_PASSWORDH");
        datatypeStrings.put(IDataAccess.DATA_PASSWORDC, "DATA_PASSWORDC");
        datatypeStrings.put(IDataAccess.DATA_LIST_STRING, "DATA_LIST_STRING");
        datatypeStrings.put(IDataAccess.DATA_LIST_INTEGER, "DATA_LIST_INTEGER");
        datatypeStrings.put(IDataAccess.DATA_LIST_UUID, "DATA_LIST_UUID");
        datatypeStrings.put(IDataAccess.DATA_VIRTUAL, "DATA_VIRTUAL");
    }

    // Initialize the data type sizes, as they are in the SQL data base definition for the
    // efacloud server. The sizes are used to limit the record value sizes before passing them
    // on to t* server side.
    static {
        for (Integer dtk : datatypeDefaults.keySet()) {
            String sqld = datatypeDefaults.get(dtk);
            if (sqld.indexOf('(') < 0)
                // this applies for date, time, int bigint asf. which have no explicit maximum length.
                // They shall not be corrected, which is stimulated by a size 0.
                datatypeMaxlen.put(dtk, 0);
            else
                datatypeMaxlen.put(dtk, Integer.parseInt(sqld.substring(sqld.indexOf('(') + 1, sqld.indexOf(')'))));
        }
    }

    /**
     * Data field definitions for efa data records usually follow some rules: their Java Constant Identifier is the
     * field name in upper cases, String values need no more than 256 characters in length, they have no specific
     * meaning in a data base sense. Some are different. They may be unique or autoincrement in the data base, they need
     * longer text than 256 characters or their constant name values are not just upper case names asf.
     */
    private static final String[] efa2fieldSpecialDefinitions = new String[]{ //
            // autoincrement fields. They are also unique which is ensured programmatically to
            // secure the unique setting if before the autoincrement setting.
            // "efa2logbook;$AUTOINCREMENT;;EntryId",   // no autoincrement, all years in table!
            "efa2messages;$AUTOINCREMENT;;MessageId",   //
            "efaCloudUsers;$AUTOINCREMENT;;ID",   //

            // unique fields
            "efa2autoincrement;$UNIQUE;;Sequence",   //
            "efa2boatstatus;$UNIQUE;;BoatId",   //
            "efa2clubwork;$UNIQUE;;Id",   //
            "efa2crews;$UNIQUE;;Id",   //
            "efa2fahrtenabzeichen;$UNIQUE;;PersonId",   //
            "efa2messages;$UNIQUE;;MessageId",   //
            "efa2project;$UNIQUE;;Type",   //
            "efa2sessiongroups;$UNIQUE;;Id",   //
            "efa2statistics;$UNIQUE;;Id",   //
            "efa2status;$UNIQUE;;Id",   //
            "efa2waters;$UNIQUE;;Id",   //
            "efaCloudUsers;$UNIQUE;;ID",   //

            // String fields which need more than 192 characters length
            "efa2boatdamages;DESCRIPTION;DATA_TEXT;Description",   //
            "efa2boatdamages;LOGBOOKTEXT;DATA_TEXT;LogbookText",   //
            "efa2boats;TYPEDESCRIPTION;DATA_TEXT;TypeDescription",   //
            "efa2boatstatus;COMMENT;DATA_TEXT;Comment",   //
            "efa2clubwork;DESCRIPTION;DATA_TEXT;Description",   //
            "efa2groups;MEMBERIDLIST;DATA_TEXT;MemberIdList",   //
            "efa2logbook;COMMENTS;DATA_TEXT;Comments",   //
            "efa2messages;TEXT;DATA_TEXT;Text",   //
            "efa2project;DESCRIPTION;DATA_TEXT;Description",   //

            // Other fields which need data type corrections
            "efa2logbook;ALLCREWNAMES;DATA_LIST_STRING;AllCrewNames",   //  List of virtual names, not single virtual
            "efa2persons;BIRTHDAY;DATA_STRING;Birthday",   // The field may only contain the birth year.

            // Fields with efa constant names which are not the uppercase field name
            "efa2boats;CURRENCY;DATA_STRING;PurchasePriceCurrency",   //
            "efa2boats;EXCLUDEFROMSTATISTIC;DATA_BOOLEAN;ExcludeFromStatistics",   //
            "efa2boatreservations;VBOAT;DATA_VIRTUAL;VirtualBoat",   //
            "efa2boatreservations;VRESERVATIONDATE;DATA_VIRTUAL;VirtualReservationDate",   //
            "efa2boatreservations;VPERSON;DATA_VIRTUAL;VirtualPerson",   //
            "efa2clubwork;WORKDATE;DATA_DATE;Date",   //
            "efa2persons;EXCLUDEFROMSTATISTIC;DATA_BOOLEAN;ExcludeFromStatistics",   //
            "efa2persons;EXCLUDEFROMCOMPETE;DATA_BOOLEAN;ExcludeFromCompetition",   //
            "efa2persons;EXCLUDEFROMCLUBWORK;DATA_BOOLEAN;ExcludeFromClubwork",   //
            "efa2project;PROJECT_ID;DATA_UUID;ProjectID",   //
            "efa2project;DEPRECATED_LOGBOOKNAME;DATA_STRING;LogbookName",   //
            "efa2project;LASTWATERSTMPLHASH;DATA_STRING;LastWatersTemplateHash",   //
            "efa2project;AUTONEWCLUBWORKDATE;DATA_DATE;AutoNewClubworkDate",   //
            "efa2project;ASSOCIATIONGLOBALNAME;DATA_STRING;GlobalAssociationName",   //
            "efa2project;ASSOCIATIONGLOBALMEMBERNO;DATA_STRING;GlobalAssociationMemberNo",   //
            "efa2project;ASSOCIATIONGLOBALLOGIN;DATA_STRING;GlobalAssociationLogin",   //
            "efa2project;ASSOCIATIONREGIONALNAME;DATA_STRING;RegionalAssociationName",   //
            "efa2project;ASSOCIATIONREGIONALMEMBERNO;DATA_STRING;RegionalAssociationMemberNo",   //
            "efa2project;ASSOCIATIONREGIONALLOGIN;DATA_STRING;RegionalAssociationLogin",   //
            "efa2project;LAST_DRV_FA_YEAR;DATA_INTEGER;LastDrvFaYear",   //
            "efa2project;LAST_DRV_WS_YEAR;DATA_INTEGER;LastDrvWsYear",   //
            "efa2project;BOATHOUSE_IDENTIFIER;DATA_STRING;BoathouseIdentifier",   //
            "efa2statistics;FILTERBYPERSONTEXT;DATA_STRING;FilterByPersonText",   //
            "efa2statistics;SHOWLOGBOOKFIELDS;DATA_LIST_STRING;ShowLogbookFields",   //
            "efa2statistics;AGGRDISTANCEBARSIZE;DATA_INTEGER;AggregationDistanceBarSize",   //
            "efa2statistics;AGGRROWDISTANCEBARSIZE;DATA_INTEGER;AggregationRowDistanceBarSize",   //
            "efa2statistics;AGGRCOXDISTANCEBARSIZE;DATA_INTEGER;AggregationCoxDistanceBarSize",   //
            "efa2statistics;AGGRSESSIONSBARSIZE;DATA_INTEGER;AggregationSessionsBarSize",   //
            "efa2statistics;AGGRAVGDISTBARSIZE;DATA_INTEGER;AggregationAvgDistanceBarSize",   //
            "efa2statistics;AGGRDURATIONBARSIZE;DATA_INTEGER;AggregationDurationBarSize",   //
            "efa2statistics;AGGRSPEEDBARSIZE;DATA_INTEGER;AggregationSpeedBarSize",   //
            "efa2statistics;OPTIONTRUNCATEDIST;DATA_BOOLEAN;OptionTruncateDistance",   //

            // Statistics data fields which need stricter limitation because of MySQL row length
            // using 1024 characters of DATA_LIST_UUID instead of 1480 chars of DATA_LIST_STRING
            "efa2statistics;FILTERGENDER;DATA_LIST_UUID;FilterGender",   //
            "efa2statistics;FILTERSESSIONTYPE;DATA_LIST_UUID;FilterSessionType",   //
            "efa2statistics;FILTERBOATTYPE;DATA_LIST_UUID;FilterBoatType",   //
            "efa2statistics;FILTERBOATSEATS;DATA_LIST_UUID;FilterBoatSeats",   //
            "efa2statistics;FILTERBOATRIGGING;DATA_LIST_UUID;FilterBoatRigging",   //
            "efa2statistics;FILTERBOATCOXING;DATA_LIST_UUID;FilterBoatCoxing",   //
            "efa2statistics;FILTERBOATOWNER;DATA_LIST_UUID;FilterBoatOwner",   //

            // Data fields common for versionized tables. They are already added
            // within the metadata definitions an only listed for completeness.
            "$versionized;VALIDFROM;DATA_LONGINT;ValidFrom",  //
            "$versionized;INVALIDFROM;DATA_LONGINT;InvalidFrom",  //
            "$versionized;INVISIBLE;DATA_BOOLEAN;Invisible",  //
            "$versionized;DELETED;DATA_BOOLEAN;Deleted", //

            // Data fields common for all tables. They are already added
            // within the metadata definitions an only listed for completeness.
            "$allTables;CHANGECOUNT;DATA_LONGINT;ChangeCount",  //
            "$allTables;LASTMODIFIED;DATA_LONGINT;LastModified"};

    public static final int allowedMismatchesDefault = 4;
    // the number of different data fields when updating the record server to client. To avoid deletion of records by
    // key mismatch or other severe mistakes. Default is <= 4, not counting ChangeCount, LastModified, and LastModification.
    // This here are the exceptions.
    public static final HashMap<String, Integer> allowedMismatches = new HashMap<String, Integer>();
    static {
        allowedMismatches.put("efa2boatdamages", 8);
        allowedMismatches.put("efa2boatreservations", 6);
        allowedMismatches.put("efa2boats", 8);
        allowedMismatches.put("efa2boatstatus", 6);
        allowedMismatches.put("efa2crews", 0);  // All may change
        allowedMismatches.put("efa2fahrtenabzeichen", 0);  // All can change
        allowedMismatches.put("efa2groups", 8);
        allowedMismatches.put("efa2logbook", 8);  // includes virtual fields
        allowedMismatches.put("efa2messages", 0);  // All may change
        allowedMismatches.put("efa2persons", 6);
        allowedMismatches.put("efa2statistics", 0);  // All may change
    }
    public static final String fixid_allowed = "efa2logbook efa2messages efa2boatdamages efa2boatreservations";
    
    public static final HashSet<String> tablenamesWithEcrids = new HashSet<String>();
    static {
    	tablenamesWithEcrids.add("efa2autoincrement");
    	tablenamesWithEcrids.add("efa2boatdamages");
    	tablenamesWithEcrids.add("efa2boatreservations");
    	tablenamesWithEcrids.add("efa2boats");
    	tablenamesWithEcrids.add("efa2boatstatus");
    	tablenamesWithEcrids.add("efa2clubwork");
    	tablenamesWithEcrids.add("efa2crews");
    	tablenamesWithEcrids.add("efa2destinations");
    	tablenamesWithEcrids.add("efa2fahrtenabzeichen");
    	tablenamesWithEcrids.add("efa2groups");
    	tablenamesWithEcrids.add("efa2logbook");
    	tablenamesWithEcrids.add("efa2messages");
    	tablenamesWithEcrids.add("efa2persons");
    	tablenamesWithEcrids.add("efa2sessiongroups");
    	tablenamesWithEcrids.add("efa2statistics");
    	tablenamesWithEcrids.add("efa2status");
    	tablenamesWithEcrids.add("efa2waters");
    }

    // cache to hold all special fields for checking when building the tables
    private final HashMap<String, RecordFieldDefinition> specialFields = new HashMap<String, RecordFieldDefinition>();
    // the complete structure of all storage object types, i. e. MySQL tables.
    public HashMap<String, StorageObjectTypeDefinition> storageObjectTypes = new HashMap<String, StorageObjectTypeDefinition>();
    // The result of server / client db layout comparison
    public String dbLayoutComparison = "No comparison yet done.";

    /**
     * Checks whether the named table is one of the set of common tables. Currently simply checks whether its name
     * starts with "efa2".
     *
     * @param tablename the table to check
     * @return true if it is a table common to client and server.
     */
    public static boolean isServerClientCommonTable(String tablename) {
        return tablename.toLowerCase().startsWith("efa2");
    }

    /**
     * Prepare a structure to gather the efa data records' structure. The structure is compiled when building the efa2
     * data structure using a hook within the MetaData class methods.
     */
    public TableBuilder() {
        for (String def : efa2fieldSpecialDefinitions) {
            RecordFieldDefinition rfd = new RecordFieldDefinition(def);
            specialFields.put(rfd.storageObjectType + "." + rfd.fieldName, rfd);
        }
    }

    /**
     * Checks, whether the storage object type field is a special field and adapts the properties, where needed.
     *
     * @param rfd the record data field to check.
     */
    private void checkSpecialField(RecordFieldDefinition rfd) {
        // check whether a special field definition exists
        RecordFieldDefinition sprfd = specialFields.get(rfd.storageObjectType + "." + rfd.fieldName);
        if (sprfd == null)
            return;
        // handle the special constant name values
        if (sprfd.constantName.equalsIgnoreCase("$AUTOINCREMENT")) {
            rfd.isAutoincrement = "x";
        } else if (sprfd.constantName.equalsIgnoreCase("$UNIQUE")) {
            rfd.isUnique = "x";
        } else if (!sprfd.constantName.startsWith("$")) {
            if (!sprfd.constantName.equalsIgnoreCase(rfd.fieldName))
                rfd.constantName = sprfd.constantName;
            // handle the special data type values.
            if (sprfd.datatypeIndex != rfd.datatypeIndex) {
                rfd.setDatatype(sprfd.datatypeIndex);
            }
        }
    }

    /**
     * Adjust the data for efacloud server storage, e. g. cut a value to the maximum length or reformat the date to
     * UK-style. This is needed to avoid data base write errors at the server side.
     *
     * @param value     value to be checked for its max length
     * @param tablename name of table (storage object type) to which it shall be written
     * @param fieldname name of field to which it shall be written
     * @return the String cut to the appropriate length, if needed.
     */
    String adjustForEfaCloudStorage(String value, String tablename, String fieldname) {
        StorageObjectTypeDefinition rtd = storageObjectTypes.get(tablename);
        if (rtd == null)
            return value;
        RecordFieldDefinition rtf = rtd.fields.get(fieldname);
        // log a warning, if an unknown field name is hit - except the field names
        // for the logbook and clubwor names which do not exist at the client side.
        if (rtf == null) {
            if (!fieldname.equalsIgnoreCase("Logbookname")
                    && !fieldname.equalsIgnoreCase("Clubworkbookname"))
                TxRequestQueue.getInstance().logApiMessage(
                    International.getString("Warnung") + " - " +
                    International.getMessage("Nicht definierter Feldname {fieldname} " +
                            "wird ungeprüft übergeben.", fieldname), 1);
            return value;
        }
        // reformat date to ISO format YYYY-MM-DD
        if (rtf.isDate) {
            if ((value != null) && (value.indexOf('.') > 0)) {
                String[] tmj = value.split("\\.");
                if (tmj.length < 3)
                    return value;
                // a time String is appended to the date, keep it at end.
                if (tmj[2].indexOf(' ') > 0)
                    return tmj[2].substring(0, tmj[2].indexOf(' ')) + "-" + tmj[1] + "-" + tmj[0] +
                            tmj[2].substring(tmj[2].indexOf(' '));
                else
                    return tmj[2] + "-" + tmj[1] + "-" + tmj[0];
            } else
                return value;
        }
        // The maxLength == 0 indicates that no cutting shall apply
        if (rtf.maxLength == 0)
            return value;
        // do not cut numbers, Ids or similar. That will create illegalArgument exceptions when reading back.
        if (((rtf.datatypeIndex == IDataAccess.DATA_STRING) || (rtf.datatypeIndex == IDataAccess.DATA_TEXT)
                || (rtf.datatypeIndex == IDataAccess.DATA_LIST_STRING) || (rtf.datatypeIndex == IDataAccess.DATA_VIRTUAL))
                && (value.length() > rtf.maxLength))     // Do not use >= because ecrids have the exact length of 12 characters set in the server db layout
            return value.substring(0, (int) (rtf.maxLength * 0.9 - 6) ) + " ..."; // cut length to max bytes for UTF-8 String.
        else
            return value;
    }

    /**
     * Return an information on the current data base structure as is expected at the server side.
     * @return information on the current data base structure
     */
    public String getAuditInformation() {
        StringBuilder auditInformation = new StringBuilder("TableBuilder audit information:\n");
        auditInformation.append("-------------------------------\n");
        for (String storageObjectTypeName : this.storageObjectTypes.keySet()) {
            StorageObjectTypeDefinition rtd = this.storageObjectTypes.get(storageObjectTypeName);
            auditInformation.append("Table: ").append(storageObjectTypeName).append(" (record size appr.").append(rtd.recordSize)
                    .append(")\n");
            for (String recordFieldName : rtd.fields.keySet()) {
                RecordFieldDefinition rfd = rtd.fields.get(recordFieldName);
                auditInformation.append("  ").append(recordFieldName).append(" (").append(datatypeStrings.get(rfd.datatypeIndex))
                        .append(":").append(rfd.recordfieldsize).append(")\n");
            }
            auditInformation.append("\n");
        }
        auditInformation.append("\n")
                .append("Comparison of data base layout client <-> server:\n")
                .append("-------------------------------------------------\n")
                .append(this.dbLayoutComparison);
        auditInformation.append("\n").append("\n");
        return auditInformation.toString();
    }

    /**
     * Add a storage object type's metadata to the TableBuilder structure. Will do nothing, if the data storage object
     * type already exists.
     *
     * @param persistence persistence (local table file reference) of storage object type to be added, e.g.
     *                    PersonRecord.getPersistence()
     * @param metaData    metadata to add.
     */
    public synchronized void addDataRecordReference(StorageObject persistence, MetaData metaData) {
        if (persistence == null || persistence.data().getStorageType() != IDataAccess.TYPE_EFA_CLOUD)
            return;
        String storageObjectType = persistence.data().getStorageObjectType();
        if (storageObjectTypes.get(storageObjectType) != null)
            return;
        StorageObjectTypeDefinition rtd = new StorageObjectTypeDefinition(storageObjectType,
                (EfaCloudStorage) persistence.data());
        storageObjectTypes.put(storageObjectType, rtd);
        String[] keyFields = metaData.getKeyFields();
        for (int i = 0; i < metaData.getFields().length; i++) {
            RecordFieldDefinition rfd = new RecordFieldDefinition(storageObjectType, metaData.getFields()[i],
                    metaData.getFieldType(i));
            checkSpecialField(rfd);
            for (String keyField : keyFields)
                if (metaData.getFields()[i].equalsIgnoreCase(keyField))
                    rfd.isKey = "x";
            rfd.column = metaData.getFieldIndex(metaData.getFields()[i]);
            rtd.fields.put(rfd.fieldName, rfd);
            rtd.recordSize = rtd.recordSize + rfd.recordfieldsize;
        }
        // add LastModification field for all tables to declare data record deletion to the client
        RecordFieldDefinition rfd = new RecordFieldDefinition(storageObjectType, "LastModification",
                IDataAccess.DATA_STRING);
        rfd.maxLength = 8;
        rfd.column = metaData.getFields().length;
        rtd.fields.put(rfd.fieldName, rfd);
        rtd.recordSize = rtd.recordSize + rfd.recordfieldsize;
        // add ClientSideKey field at server side for tables which allow for key correction
        if (fixid_allowed.contains(storageObjectType.toLowerCase())) {
            rfd = new RecordFieldDefinition(storageObjectType, "ClientSideKey", IDataAccess.DATA_STRING);
            rfd.maxLength = 64;
            rfd.column = metaData.getFields().length + 1;
            rtd.fields.put(rfd.fieldName, rfd);
            rtd.recordSize = rtd.recordSize + rfd.recordfieldsize;
        }
        // add logbookname for logbook table on the server
        if (storageObjectType.equalsIgnoreCase("efa2logbook")) {
            rfd = new RecordFieldDefinition(storageObjectType, "Logbookname", IDataAccess.DATA_STRING);
            rfd.maxLength = 192;
            rfd.column = metaData.getFields().length + 2;
            rtd.fields.put(rfd.fieldName, rfd);
            rtd.recordSize = rtd.recordSize + rfd.recordfieldsize;
        }
        // add clubworkbookname for clubwork table on the server
        if (storageObjectType.equalsIgnoreCase("efa2clubwork")) {
            rfd = new RecordFieldDefinition(storageObjectType, "Clubworkbookname", IDataAccess.DATA_STRING);
            rfd.maxLength = 192;
            rfd.column = metaData.getFields().length + 2;
            rtd.fields.put(rfd.fieldName, rfd);
            rtd.recordSize = rtd.recordSize + rfd.recordfieldsize;
        }
        rtd.versionized = metaData.isVersionized();
    }

    /**
     * Remove a storage object type's metadata from the TableBuilder structure. Will do nothing, if the data storage
     * object type does not exist.
     *
     * @param storageObjectType type of storage object type to be removed, e.g. efa2persons
     */
    public synchronized void removeStorageType(String storageObjectType) {
        if (storageObjectTypes.get(storageObjectType) == null)
            return;
        storageObjectTypes.remove(storageObjectType);
    }

    /**
     * Set a key for a storage object type in the TableBuilder structure. Add the ValidFrom key for versionized tables,
     * even if not provided in "key". Will do nothing, if the storage object type does not exist. Will replace any
     * existing key for the specified storage object type.
     *
     * @param storageObjectType storage object type for which the key shall be set, e.g. efa2persons.
     * @param key               key to set.
     */
    public synchronized void addKey(String storageObjectType, String[] key) {
        StorageObjectTypeDefinition rtd = storageObjectTypes.get(storageObjectType);
        if (rtd == null)
            return;
        while (rtd.keys.size() > 0)
            rtd.keys.remove(0);
        Collections.addAll(rtd.keys, key);
        // special case: the "ValidFrom" key for versionized tables must explicitly be added.
        if (rtd.versionized && !rtd.keys.contains("ValidFrom"))
            rtd.keys.add("ValidFrom");
    }

    /**
     * Return the EfaCloud storage persistence object of a table.
     *
     * @param tablename the table searched.
     * @return Its Efacloud Storage Object
     */
    public EfaCloudStorage getPersistence(String tablename) {
        if (storageObjectTypes.get(tablename) == null)
            return null;
        return storageObjectTypes.get(tablename).persistence;
    }

    /**
     * Special purpose hack: adjust the record field size of the MemberIdList in efa2groups, in order to allow for
     * larger groups.
     *
     * @param newGroupMemberIdListSize new record field size (default is the default for DATA_LIST_UUID)
     */
    public void adjustGroupMemberIdListSize(int newGroupMemberIdListSize) {
        StorageObjectTypeDefinition rtd = storageObjectTypes.get("efa2groups");
        if (rtd != null) {
            RecordFieldDefinition rfd = rtd.fields.get("MemberIdList");
            if (rfd != null) {
                rtd.recordSize = rtd.recordSize - rfd.recordfieldsize + newGroupMemberIdListSize;
                rfd.maxLength = newGroupMemberIdListSize;
                rfd.recordfieldsize = newGroupMemberIdListSize;
            }
        }
    }

    /**
     * Parse a server provided data base layout and compare with local layout
     * @param serverDBLayout the server data base layout as was returned by the server within NOP rwquest.
     */
    public void mapServerDBLayout(String serverDBLayout) {
        String[] tables = serverDBLayout.split("\\|T\\|");
        if (tables.length < 2) {
            dbLayoutComparison = "No table separator detected in layout.";
            return;
        }
        StringBuilder findings = new StringBuilder();
        for (String table : tables) {
            // ignore db_layout_version value
            if (table.startsWith("t:")) {
                String[] columns = table.split("\\|C\\|");
                String tname = "unknown";
                StorageObjectTypeDefinition rtd = null;
                for (String column : columns) {
                    String cname;
                    if (column.startsWith("t:")) {
                        tname = column.substring(2);
                        rtd = storageObjectTypes.get(tname);
                        if ((rtd == null) && !tname.substring(0, 8).equalsIgnoreCase("efaCloud"))
                            findings.append("No local table '").append(tname)
                                    .append("'\n");
                    }
                    else if (column.startsWith("c:") && (rtd != null) && (column.indexOf("=") > 0)) {
                        cname = column.substring(2).split("=", 2)[0];
                        RecordFieldDefinition rfd = rtd.fields.get(cname);
                        if (rfd == null) {
                            if (!cname.equalsIgnoreCase("ecrid") && !cname.equalsIgnoreCase("ecrown")
                                    && !cname.equalsIgnoreCase("ecrhis")) {
                                findings.append("No local column '").append(cname)
                                        .append("'").append(" in table '").append(tname)
                                        .append("'\n");
                            }
                        } else {
                            String[] cdef = (column + "|padded").split("=", 2)[1].split("\\|");
                            // type;size;nullAllowed;default;unique;autoincrement
                            if (! rfd.sqlDefinition.toLowerCase(Locale.ROOT).startsWith(cdef[0].toLowerCase(Locale.ROOT)))
                                findings.append("Column type differs for '").append(cname)
                                        .append("'").append(" in table '").append(tname)
                                        .append("' - server:").append(cdef[0])
                                        .append("' - local:").append(rfd.sqlDefinition.toLowerCase(Locale.ROOT))
                                        .append("'\n");
                            if (rfd.maxLength > (Integer.parseInt(cdef[1]) + 1)) {
                                int serverLength = Integer.parseInt(cdef[1]);
                                if ((serverLength > 0) && (rfd.maxLength > 0)) {
                                    findings.append("Local column size adjusted for '").append(cname)
                                            .append("'").append(" in table '").append(tname)
                                            .append("' - to server size:").append(cdef[1])
                                            .append("' - from previous local size:").append(rfd.maxLength)
                                            .append("'\n");
                                    rfd.maxLength = serverLength;
                                }
                            }
                            if (((rfd.isUnique.length() > 0) && (cdef[4].length() == 0))
                                    || ((rfd.isUnique.length() == 0) && (cdef[4].length() > 0)))
                                findings.append("Column unique property differs for '").append(cname)
                                        .append("'").append(" in table '").append(tname)
                                        .append("' - server:").append(cdef[4])
                                        .append("' - local:").append(rfd.isUnique)
                                        .append("'\n");
                            if (((rfd.isAutoincrement.length() > 0) && (cdef[5].length() == 0))
                                    || ((rfd.isAutoincrement.length() == 0) && (cdef[5].length() > 0)))
                                findings.append("Column autoincrement property differs for '").append(cname)
                                        .append("'").append(" in table '").append(tname)
                                        .append("' - server:").append(cdef[5])
                                        .append("' - local:").append(rfd.isAutoincrement)
                                        .append("'\n");
                        }
                    }
                }
            }
        }
        dbLayoutComparison = findings.toString();
    }

    /**
     * Small inner container holding a storage object type definition to provide structure
     */
    public static class StorageObjectTypeDefinition {
        final String storageObjectType;
        public EfaCloudStorage persistence;
        final boolean isProjectTable;
        boolean versionized;
        int recordSize = 0;
        HashMap<String, RecordFieldDefinition> fields = new HashMap<String, RecordFieldDefinition>();
        ArrayList<String> keys = new ArrayList<String>();

        StorageObjectTypeDefinition(String storageObjectType, EfaCloudStorage persistence) {
            this.storageObjectType = storageObjectType;
            this.persistence = persistence;
            isProjectTable = Backup.isProjectDataAccess(persistence.getStorageObjectType());
        }

    }

    /**
     * Small inner container holding a field definition to provide structure
     */
    private static class RecordFieldDefinition {
        final String storageObjectType;
        final String fieldName;
        String constantName;
        int datatypeIndex;   // The Index as used in metaData class
        int column;
        int maxLength;
        int recordfieldsize;
        boolean isDate;
        String sqlDefinition;
        String isUnique = "";
        String isKey = "";
        String isAutoincrement = "";

        /**
         * Set properties datatype and sqlDefinition and determine the field size. Adjust to custom settings.
         *
         * @param datatypeIndex data type index as in metaData to be used
         */
        void setDatatype(int datatypeIndex) {
            this.datatypeIndex = datatypeIndex;
            maxLength = (datatypeMaxlen.get(datatypeIndex) == null) ? 0 : datatypeMaxlen.get(datatypeIndex);
            // the recordfieldsize approximates the space needed in the table record to be 8 chars for text and numbers
            recordfieldsize = ((datatypeIndex == IDataAccess.DATA_TEXT) || (maxLength == 0)) ? 8 : maxLength;
            sqlDefinition = datatypeDefaults.get(datatypeIndex);
            isDate = datatypeIndex == IDataAccess.DATA_DATE;
        }

        /**
         * Constructor
         *
         * @param fieldDef field definition. Csv String as in efa2fieldSpecialDefinitions
         */
        RecordFieldDefinition(String fieldDef) {
            String[] elements = fieldDef.split(";");
            storageObjectType = elements[0];
            constantName = elements[1];
            // resolve String representation to integer. Reverse use of the hashmap, because it will only
            // occur during bootstrap a separate static hash map definition makes limited sense. Forward use is in
            // "toString()" function, so more often.
            String dataType = elements[2];
            for (int i : datatypeStrings.keySet())
                if (datatypeStrings.get(i).equalsIgnoreCase(dataType))
                    setDatatype(i);
            fieldName = elements[3];
        }

        /**
         * Constructor
         *
         * @param recordType    the storage object type as used in metadata, e.g. efa2persons
         * @param field         the field name, e.g. "LastName"
         * @param datatypeIndex the index of the data type, e.g. 0 for IDataAccess.DATA_STRING
         */
        RecordFieldDefinition(String recordType, String field, int datatypeIndex) {
            this.storageObjectType = recordType;
            constantName = field.toUpperCase();
            setDatatype(datatypeIndex);
            fieldName = field;
            sqlDefinition = datatypeDefaults.get(datatypeIndex);
        }

        public String toString() {
            return storageObjectType + "." + fieldName + "(" + TableBuilder.datatypeStrings.get(datatypeIndex) + ")";
        }
    }
}
