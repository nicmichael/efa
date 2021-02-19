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
import de.nmichael.efa.util.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class TableBuilder {

    /**
     * IDataAccess interface data type name mapping to MySQL data conventions. All SQL data types allow NULL and all use
     * it as Default. Strings are by default Varchar(256), only in eight cases, they are made to "Text", see also
     * efa2fieldSpecialDefinitions.
     */
    private static final HashMap<Integer, String> datatypeDefaults = new HashMap<Integer, String>();
    private static final HashMap<Integer, Integer> datatypeSizes = new HashMap<Integer, Integer>();

    static {
        // Note to length definitions: limiting are the Logbook and Statistics tables, which hit
        // the 65k row length limitation of MySQL. Therefore "DATA_STRING" is only 192 characters
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
        datatypeDefaults.put(IDataAccess.DATA_TEXT, "Text(65536) NULL DEFAULT NULL");
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
                // this applies for date, time, int bigint asf. which have no explicit size definition.
                // They shall not be corrected, which is stimulated by a size 0.
                datatypeSizes.put(dtk, 0);
            else
                datatypeSizes.put(dtk, Integer.parseInt(sqld.substring(sqld.indexOf('(') + 1, sqld.indexOf(')'))));
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

            // unique fields
            "efa2autoincrement;$UNIQUE;;Sequence",   //
            "efa2boatstatus;$UNIQUE;;BoatId",   //
            "efa2clubwork;$UNIQUE;;Id",   //
            "efa2crews;$UNIQUE;;Id",   //
            "efa2fahrtenabzeichen;$UNIQUE;;PersonId",   //
            "efa2project;$UNIQUE;;Type",   //
            "efa2sessiongroups;$UNIQUE;;Id",   //
            "efa2statistics;$UNIQUE;;Id",   //
            "efa2status;$UNIQUE;;Id",   //
            "efa2waters;$UNIQUE;;Id",   //

            // String fields which need more then 192 characters length
            "efa2boatdamages;DESCRIPTION;DATA_TEXT;Description",   //
            "efa2boatdamages;LOGBOOKTEXT;DATA_TEXT;LogbookText",   //
            "efa2boats;TYPEDESCRIPTION;DATA_TEXT;TypeDescription",   //
            "efa2boatstatus;COMMENT;DATA_TEXT;Comment",   //
            "efa2clubwork;DESCRIPTION;DATA_TEXT;Description",   //
            "efa2logbook;COMMENTS;DATA_TEXT;Comments",   //
            "efa2messages;TEXT;DATA_TEXT;Text",   //
            "efa2project;DESCRIPTION;DATA_TEXT;Description",   //

            // Other fields which need data type corrections
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

    public static final String fixid_allowed = "efa2logbook efa2messages efa2boatdamages efa2boatreservations";

    // cache to hold all special fields for checking when building the tables
    private final HashMap<String, RecordFieldDefinition> specialFields = new HashMap<String, RecordFieldDefinition>();
    // the complete structure of all storage object types, i. e. MySQL tables.
    public HashMap<String, StorageObjectTypeDefinition> storageObjectTypes = new HashMap<String, StorageObjectTypeDefinition>();

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
        StorageObjectTypeDefinition rtd = storageObjectTypes.get(rfd.storageObjectType);
        if (sprfd.constantName.equalsIgnoreCase("$AUTOINCREMENT"))
            rtd.autoincrements.add(rfd.fieldName);
        else if (sprfd.constantName.equalsIgnoreCase("$UNIQUE"))
            rtd.uniques.add(rfd.fieldName);
        else if (sprfd.constantName.equalsIgnoreCase("$KEY"))
            rtd.keys.add(rfd.fieldName);
        else if (!sprfd.constantName.startsWith("$")) {
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
        if ((rtf == null) && !fieldname.equalsIgnoreCase("Logbookname")) {
            TxRequestQueue.getInstance().logApiMessage(
                    International.getMessage("Warnung - Nicht definierter Feldname: {Feldname}. " +
                            "Wird ungeprüft übergeben.", fieldname), 1);
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
        if (value.length() >= rtf.maxLength)
            return value.substring(0, rtf.maxLength - 4) + " ...";
        else
            return value;
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
        for (int i = 0; i < metaData.getFields().length; i++) {
            RecordFieldDefinition rfd = new RecordFieldDefinition(storageObjectType, metaData.getFields()[i],
                    metaData.getFieldType(i));
            checkSpecialField(rfd);
            rfd.column = metaData.getFieldIndex(metaData.getFields()[i]);
            rtd.fields.put(rfd.fieldName, rfd);
        }
        // add LastModification field for all tables to declare data record deletion to the client
        RecordFieldDefinition rfd = new RecordFieldDefinition(storageObjectType, "LastModification",
                IDataAccess.DATA_STRING);
        rfd.maxLength = 8;
        rfd.column = metaData.getFields().length;
        rtd.fields.put(rfd.fieldName, rfd);
        // add ClientSideKey field for tables which allow for key correction
        if (fixid_allowed.contains(storageObjectType.toLowerCase())) {
            rfd = new RecordFieldDefinition(storageObjectType, "ClientSideKey", IDataAccess.DATA_STRING);
            rfd.maxLength = 64;
            rfd.column = metaData.getFields().length + 1;
            rtd.fields.put(rfd.fieldName, rfd);
        }
        // add logbookname for logbook table, the server
        if (storageObjectType.toLowerCase().equalsIgnoreCase("efa2logbook")) {
            rfd = new RecordFieldDefinition(storageObjectType, "Logbookname", IDataAccess.DATA_STRING);
            rfd.maxLength = 256;
            rfd.column = metaData.getFields().length + 2;
            rtd.fields.put(rfd.fieldName, rfd);
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
     * Get a set of table names for up- and download activities
     *
     * @param useProject set true to get the project data tables
     * @param useConfig  set true to get the configuration tables
     * @return set of requested table names
     */
    public synchronized StorageObjectTypeDefinition[] getTables(boolean useProject, boolean useConfig) {
        StorageObjectTypeDefinition[] tables = new StorageObjectTypeDefinition[storageObjectTypes.keySet().size()];
        int i = 0;
        for (String tablename : storageObjectTypes.keySet()) {
            StorageObjectTypeDefinition rtd = storageObjectTypes.get(tablename);
            if ((rtd.isProjectTable && useProject) || (!rtd.isProjectTable && useConfig))
                tables[i++] = rtd;
        }
        return tables;
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
     * (Re)Build all efa tables by iterating through all using initServerTable. All tables will be empty afterwards.
     */
    public void initAllServerTables() {
        for (String storageObjectType : storageObjectTypes.keySet())
            initServerTable(storageObjectType);
    }

    /**
     * Build an efa table at the server side by issuing three respective API commands: createtable (including all
     * columns), autoincrement and unique. This method is performed synchronously. Note the defintion of "createtable":
     * If a table with the given name exists, it will be dropped
     *
     * @param storageObjectType storageType of table to be built, e.g. "efa2persons".
     */
    public void initServerTable(String storageObjectType) {
        StorageObjectTypeDefinition rtd = this.storageObjectTypes.get(storageObjectType);
        String tablename = rtd.storageObjectType;
        TxRequestQueue txq = TxRequestQueue.getInstance();
        // create table
        txq.appendTransaction(TxRequestQueue.TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.CREATETABLE, tablename,
                rtd.tableDefinitionRecord());
        // add uniques
        if (rtd.uniques.size() > 0)
            for (String unique : rtd.uniques) {
                String record = unique + ";" +
                        CsvCodec.encodeElement(rtd.fields.get(unique).sqlDefinition, CsvCodec.DEFAULT_DELIMITER,
                                CsvCodec.DEFAULT_QUOTATION);
                txq.appendTransaction(TxRequestQueue.TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.UNIQUE, tablename, record);
            }
        // add autoincrements
        if (rtd.autoincrements.size() > 0)
            for (String autoincrement : rtd.autoincrements) {
                // autoincrement fields must also be unique which is ensured programmatically to
                // secure the unique setting if before the autoincrement setting.
                String record = autoincrement + ";" +
                        CsvCodec.encodeElement(rtd.fields.get(autoincrement).sqlDefinition, CsvCodec.DEFAULT_DELIMITER,
                                CsvCodec.DEFAULT_QUOTATION);
                txq.appendTransaction(TxRequestQueue.TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.UNIQUE, tablename, record);
                txq.appendTransaction(TxRequestQueue.TX_SYNCH_QUEUE_INDEX, Transaction.TX_TYPE.AUTOINCREMENT, tablename, record);
            }
    }

    /**
     * Small inner container holding a storage object type definition to provide structure
     */
    private static class StorageObjectTypeDefinition {
        final String storageObjectType;
        final EfaCloudStorage persistence;
        final boolean isProjectTable;
        boolean versionized;
        HashMap<String, RecordFieldDefinition> fields = new HashMap<String, RecordFieldDefinition>();
        ArrayList<String> keys = new ArrayList<String>();
        ArrayList<String> uniques = new ArrayList<String>();
        ArrayList<String> autoincrements = new ArrayList<String>();

        StorageObjectTypeDefinition(String storageObjectType, EfaCloudStorage persistence) {
            this.storageObjectType = storageObjectType;
            this.persistence = persistence;
            isProjectTable = Backup.isProjectDataAccess(persistence.getStorageObjectType());
        }

        /**
         * Return the full table definition record, consisting of field;value - Strings
         *
         * @return full table definition record
         */
        String[] tableDefinitionRecord() {
            String[] tdr = new String[fields.size()];
            //# Use the //# commented lines to create a set of SQL table definitions for documentation or testing
            //# StringBuilder sqlCmd = new StringBuilder();
            //# sqlCmd.append("CREATE TABLE `").append(persistence.getStorageObjectType()).append("` (\n");
            for (String fieldname : fields.keySet()) {
                RecordFieldDefinition rfd = fields.get(fieldname);
                // neither the field name nor the sqlDefinition need csv encoding.
                tdr[rfd.column] = rfd.fieldName + ";" + rfd.sqlDefinition;
                //# sqlCmd.append("`").append(rfd.fieldName).append("` ").append(rfd.sqlDefinition).append(",\n");
            }
            //# String sqlcmd = sqlCmd.substring(0, sqlCmd.length() - 2) + "\n);";
            //# TextResource.writeContents("/ramdisk/" + persistence.getStorageObjectType() + ".sql", sqlcmd, false);
            return tdr;
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
        boolean isDate;
        String sqlDefinition;

        /**
         * Set properties datatype and sqlDefinition
         *
         * @param datatypeIndex data type index as in metaData to be used
         */
        void setDatatype(int datatypeIndex) {
            this.datatypeIndex = datatypeIndex;
            maxLength = (datatypeSizes.get(datatypeIndex) == null) ? 0 : (datatypeSizes.get(datatypeIndex) -
                    2);  // two characters safety margin.
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
            for (int i : datatypeStrings.keySet())
                if (datatypeStrings.get(i).equalsIgnoreCase(elements[2]))
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
            maxLength = (datatypeSizes.get(datatypeIndex) == null) ? 0 : datatypeSizes.get(datatypeIndex);
            fieldName = field;
            sqlDefinition = datatypeDefaults.get(datatypeIndex);
        }

        public String toString() {
            return storageObjectType + "." + fieldName + "(" + TableBuilder.datatypeStrings.get(datatypeIndex) + ")";
        }
    }
}
