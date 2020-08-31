package de.nmichael.efa.data.efacloud;

import de.nmichael.efa.core.Backup;
import de.nmichael.efa.data.storage.EfaCloudStorage;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.gui.EfaBaseFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class TableBuilder {

    /**
     * There are essentially two namespaces for efa2 records the efa2 record type name like
     * efa2persons and the record's table class name, like Persons. This here maps the name spaces
     */
    public static final HashMap<String, String> efaCloudTableNames = new HashMap<>();

    static {
        efaCloudTableNames.put("efa2admins", "Admins");
        efaCloudTableNames.put("efa2autoincrement", "AutoIncrement");
        efaCloudTableNames.put("efa2boatdamages", "BoatDamages");
        efaCloudTableNames.put("efa2boatreservations", "BoatReservations");
        efaCloudTableNames.put("efa2boats", "Boats");
        efaCloudTableNames.put("efa2boatstatus", "BoatStatus");
        efaCloudTableNames.put("efa2config", "Config");
        efaCloudTableNames.put("efa2clubwork", "Clubwork");
        efaCloudTableNames.put("efa2crews", "Crews");
        efaCloudTableNames.put("efa2destinations", "Destinations");
        efaCloudTableNames.put("efa2fahrtenabzeichen", "Fahrtenabzeichen");
        efaCloudTableNames.put("efa2groups", "Groups");
        efaCloudTableNames.put("efa2logbook", "Logbook");
        efaCloudTableNames.put("efa2messages", "Messages");
        efaCloudTableNames.put("efa2persons", "Persons");
        efaCloudTableNames.put("efa2project", "Project");
        efaCloudTableNames.put("efa2sessiongroups", "SessionGroups");
        efaCloudTableNames.put("efa2statistics", "Statistics");
        efaCloudTableNames.put("efa2status", "Status");
        efaCloudTableNames.put("efa2types", "Types");
        efaCloudTableNames.put("efa2waters", "Waters");
    }

    /**
     * IDataAccess interface data type name mapping to MySQL data conventions. All SQL data types
     * allow NULL and all use it as Default. Strings are by default Varchar(256), only in eight
     * cases, they are made to "Text", see also efa2fieldSpecialDefinitions.
     */
    private static final HashMap<String, String> datatypeDefaults = new HashMap<>();
    private static final HashMap<String, Integer> datatypeSizes = new HashMap<>();

    static {
        // Note to length definitions: limiting are the Logbook and Statistics tables, which hit
        // the 65k row length limitation of MySQL. Therefore "DATA_STRING" is only 192 characters
        // and DATA_LIST_STRING not more than "1536". The table statistics has then only 4.400
        // characters left. Entries are cut as soon as they reach the limit - 2 characters.
        datatypeDefaults.put("DATA_BOOLEAN", "Varchar(12) NULL DEFAULT NULL"); // true, false
        datatypeDefaults.put("DATA_DATE", "Date NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_DECIMAL", "Varchar(64) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_DISTANCE", "Varchar(64) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_DOUBLE", "Double NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_INTEGER", "Int(10) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_INTSTRING", "Varchar(64) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_LIST_INTEGER", "Varchar(192) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_LIST_STRING", "Varchar(2048) NULL DEFAULT NULL");  // see above
        datatypeDefaults.put("DATA_LIST_UUID", "Varchar(1024) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_LONGINT", "BigInt(20) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_PASSWORDC", "Varchar(256) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_PASSWORDH", "Varchar(256) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_STRING", "Varchar(192) NULL DEFAULT NULL");  // see above
        datatypeDefaults.put("DATA_STRING(TEXT)", "Text(65536) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_TIME", "Time NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_UUID", "Varchar(64) NULL DEFAULT NULL");
        datatypeDefaults.put("DATA_VIRTUAL", "Varchar(256) NULL DEFAULT NULL");
    }

    /**
     * IDataAccess interface data type index to name mapping.
     */
    public static final HashMap<Integer, String> datatypes = new HashMap<>();

    static {
        datatypes.put(IDataAccess.DATA_STRING, "DATA_STRING");
        datatypes.put(IDataAccess.DATA_INTEGER, "DATA_INTEGER");
        datatypes.put(IDataAccess.DATA_LONGINT, "DATA_LONGINT");
        datatypes.put(IDataAccess.DATA_DOUBLE, "DATA_DOUBLE");
        datatypes.put(IDataAccess.DATA_DECIMAL, "DATA_DECIMAL");
        datatypes.put(IDataAccess.DATA_DISTANCE, "DATA_DISTANCE");
        datatypes.put(IDataAccess.DATA_BOOLEAN, "DATA_BOOLEAN");
        datatypes.put(IDataAccess.DATA_DATE, "DATA_DATE");
        datatypes.put(IDataAccess.DATA_TIME, "DATA_TIME");
        datatypes.put(IDataAccess.DATA_UUID, "DATA_UUID");
        datatypes.put(IDataAccess.DATA_INTSTRING, "DATA_INTSTRING");
        datatypes.put(IDataAccess.DATA_PASSWORDH, "DATA_PASSWORDH");
        datatypes.put(IDataAccess.DATA_PASSWORDC, "DATA_PASSWORDC");
        datatypes.put(IDataAccess.DATA_LIST_STRING, "DATA_LIST_STRING");
        datatypes.put(IDataAccess.DATA_LIST_INTEGER, "DATA_LIST_INTEGER");
        datatypes.put(IDataAccess.DATA_LIST_UUID, "DATA_LIST_UUID");
        datatypes.put(IDataAccess.DATA_VIRTUAL, "DATA_VIRTUAL");
    }


    // Initialize the data type sizes, as they are in the SQL data base definition for the
    // efacloud  server. The sizes are used to limit the record vakue sizes before passing them
    // on to t* server side.
    static {
        for (String dtk : datatypeDefaults.keySet()) {
            String sqld = datatypeDefaults.get(dtk);
            if (sqld.indexOf('(') < 0) datatypeSizes.put(dtk, 0);
            else datatypeSizes.put(dtk,
                    Integer.parseInt(sqld.substring(sqld.indexOf('(') + 1, sqld.indexOf(')'))));
        }
    }


    /**
     * Data field definitions for efa data records usually follow some rules: their Java Constant
     * Identifier is the field name in upper cases, String values need no more than 256 characters
     * in length, they have no specific meaning in a data base sense. Some are different. They may
     * be unique or autoincrement in the data base, they need longer text than 256 characters or
     * their constant name values are not just upper case names asf.
     */
    private static final String[] efa2fieldSpecialDefinitions = new String[]{ //
            // autoincrement fields. They are also unique which is ensured programmatically to
            // secure the unique setting if before the autoincrement setting.
            "Logbook;$AUTOINCREMENT;;EntryId",   //
            "Messages;$AUTOINCREMENT;;MessageId",   //

            // unique fields
            "AutoIncrement;$UNIQUE;;Sequence",   //
            "BoatStatus;$UNIQUE;;BoatId",   //
            "Clubwork;$UNIQUE;;Id",   //
            "Crews;$UNIQUE;;Id",   //
            "Fahrtenabzeichen;$UNIQUE;;PersonId",   //
            "Project;$UNIQUE;;Type",   //
            "SessionGroups;$UNIQUE;;Id",   //
            "Statistics;$UNIQUE;;Id",   //
            "Status;$UNIQUE;;Id",   //
            "Waters;$UNIQUE;;Id",   //

            // String fields which need more then 256 characters length fields
            "BoatDamages;DESCRIPTION;DATA_STRING(TEXT);Description",   //
            "BoatDamages;LOGBOOKTEXT;DATA_STRING(TEXT);LogbookText",   //
            "Boats;TYPEDESCRIPTION;DATA_STRING(TEXT);TypeDescription",   //
            "BoatStatus;COMMENT;DATA_STRING(TEXT);Comment",   //
            "Clubwork;DESCRIPTION;DATA_STRING(TEXT);Description",   //
            "Logbook;COMMENTS;DATA_STRING(TEXT);Comments",   //
            "Messages;TEXT;DATA_STRING(TEXT);Text",   //
            "Project;DESCRIPTION;DATA_STRING(TEXT);Description",   //

            // Other fields which need data type corrections
            "Persons;BIRTHDAY;DATA_STRING;Birthday",   // The field may only contain the birth year.

            // Fields with efa constant names which are not the uppercase field name
            "Boats;CURRENCY;DATA_STRING;PurchasePriceCurrency",   //
            "Boats;EXCLUDEFROMSTATISTIC;DATA_BOOLEAN;ExcludeFromStatistics",   //
            "BoatReservations;VBOAT;DATA_VIRTUAL;VirtualBoat",   //
            "BoatReservations;VRESERVATIONDATE;DATA_VIRTUAL;VirtualReservationDate",   //
            "BoatReservations;VPERSON;DATA_VIRTUAL;VirtualPerson",   //
            "Clubwork;WORKDATE;DATA_DATE;Date",   //
            "Persons;EXCLUDEFROMSTATISTIC;DATA_BOOLEAN;ExcludeFromStatistics",   //
            "Persons;EXCLUDEFROMCOMPETE;DATA_BOOLEAN;ExcludeFromCompetition",   //
            "Persons;EXCLUDEFROMCLUBWORK;DATA_BOOLEAN;ExcludeFromClubwork",   //
            "Project;PROJECT_ID;DATA_UUID;ProjectID",   //
            "Project;DEPRECATED_LOGBOOKNAME;DATA_STRING;LogbookName",   //
            "Project;LASTWATERSTMPLHASH;DATA_STRING;LastWatersTemplateHash",   //
            "Project;AUTONEWCLUBWORKDATE;DATA_DATE;AutoNewClubworkDate",   //
            "Project;ASSOCIATIONGLOBALNAME;DATA_STRING;GlobalAssociationName",   //
            "Project;ASSOCIATIONGLOBALMEMBERNO;DATA_STRING;GlobalAssociationMemberNo",   //
            "Project;ASSOCIATIONGLOBALLOGIN;DATA_STRING;GlobalAssociationLogin",   //
            "Project;ASSOCIATIONREGIONALNAME;DATA_STRING;RegionalAssociationName",   //
            "Project;ASSOCIATIONREGIONALMEMBERNO;DATA_STRING;RegionalAssociationMemberNo",   //
            "Project;ASSOCIATIONREGIONALLOGIN;DATA_STRING;RegionalAssociationLogin",   //
            "Project;LAST_DRV_FA_YEAR;DATA_INTEGER;LastDrvFaYear",   //
            "Project;LAST_DRV_WS_YEAR;DATA_INTEGER;LastDrvWsYear",   //
            "Project;BOATHOUSE_IDENTIFIER;DATA_STRING;BoathouseIdentifier",   //
            "Statistics;FILTERBYPERSONTEXT;DATA_STRING;FilterByPersonText",   //
            "Statistics;SHOWLOGBOOKFIELDS;DATA_LIST_STRING;ShowLogbookFields",   //
            "Statistics;AGGRDISTANCEBARSIZE;DATA_INTEGER;AggregationDistanceBarSize",   //
            "Statistics;AGGRROWDISTANCEBARSIZE;DATA_INTEGER;AggregationRowDistanceBarSize",   //
            "Statistics;AGGRCOXDISTANCEBARSIZE;DATA_INTEGER;AggregationCoxDistanceBarSize",   //
            "Statistics;AGGRSESSIONSBARSIZE;DATA_INTEGER;AggregationSessionsBarSize",   //
            "Statistics;AGGRAVGDISTBARSIZE;DATA_INTEGER;AggregationAvgDistanceBarSize",   //
            "Statistics;AGGRDURATIONBARSIZE;DATA_INTEGER;AggregationDurationBarSize",   //
            "Statistics;AGGRSPEEDBARSIZE;DATA_INTEGER;AggregationSpeedBarSize",   //
            "Statistics;OPTIONTRUNCATEDIST;DATA_BOOLEAN;OptionTruncateDistance",   //

            // Statistics data fields which need stricter limitation because of MySQL row length
            // using 1024 characters of DATA_LIST_UUID instead of 2048 chars of DATA_LIST_STRING
            "Statistics;FILTERGENDER;DATA_LIST_UUID;FilterGender",   //
            "Statistics;FILTERSESSIONTYPE;DATA_LIST_UUID;FilterSessionType",   //
            "Statistics;FILTERBOATTYPE;DATA_LIST_UUID;FilterBoatType",   //
            "Statistics;FILTERBOATSEATS;DATA_LIST_UUID;FilterBoatSeats",   //
            "Statistics;FILTERBOATRIGGING;DATA_LIST_UUID;FilterBoatRigging",   //
            "Statistics;FILTERBOATCOXING;DATA_LIST_UUID;FilterBoatCoxing",   //
            "Statistics;FILTERBOATOWNER;DATA_LIST_UUID;FilterBoatOwner",   //

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

    // cache to hold all special fields for checking when building the tables
    private final HashMap<String, RecordFieldDefinition> specialFields = new HashMap<>();
    // the complete structure of all record types, i. e. MySQL tables.
    public HashMap<String, RecordTypeDefinition> recordTypes = new HashMap<>();
    // when starting the application, data will be synchronized by loading them from the server
    // to show the load progress, the GUI context is needed. It can be put here
    public EfaBaseFrame guiBaseFrameOnAppLoading = null;
    // The tables which need an update are asynchronously retrieved. This field caches the result
    public ArrayList<RecordTypeDefinition> tablesToUpdate;

    /**
     * Prepare a structure to gather the efa data records' structure. The structure is compiled when
     * building the efa2 data structure using a hook within the MetaData class methods.
     */
    public TableBuilder() {
        for (String def : efa2fieldSpecialDefinitions) {
            RecordFieldDefinition rfd = new RecordFieldDefinition(def);
            specialFields.put(rfd.recordType + "." + rfd.fieldName, rfd);
        }
    }

    /**
     * Checks, whether the data record field is a special field and adapts the properties, where
     * needed.
     *
     * @param rfd the record data field to check.
     */
    private void checkSpecialField(RecordFieldDefinition rfd) {
        // check whether a special field definition exists
        RecordFieldDefinition sprfd = specialFields.get(rfd.recordType + "." + rfd.fieldName);
        if (sprfd == null) return;
        // handle the special constant name values
        RecordTypeDefinition rtd = recordTypes.get(rfd.recordType);
        if (sprfd.constantName.equalsIgnoreCase("$AUTOINCREMENT"))
            rtd.autoincrements.add(rfd.fieldName);
        else if (sprfd.constantName.equalsIgnoreCase("$UNIQUE")) rtd.uniques.add(rfd.fieldName);
        else if (sprfd.constantName.equalsIgnoreCase("$KEY")) rtd.keys.add(rfd.fieldName);
        else if (!sprfd.constantName.startsWith("$")) {
            if (!sprfd.constantName.equalsIgnoreCase(rfd.fieldName))
                rfd.constantName = sprfd.constantName;
            // handle the special data type values.
            if (!sprfd.datatype.equalsIgnoreCase(rfd.datatype)) {
                rfd.setDatatype(sprfd.datatype);
            }
        }
    }

    /**
     * Adjust the data for efacloud server storage, e. g. cut a value to the maximum length or
     * reformat the date to UK-style. This is needed to avoid data base write errors at the server
     * side.
     *
     * @param value     value to be checked for its max length
     * @param tablename name of table (record type) to which it shall be written
     * @param fieldname name of field to which it shall be written
     * @return the String cut to the appropriate length, if needed.
     */
    protected String adjustForEfaCloudStorage(String value, String tablename, String fieldname) {
        RecordTypeDefinition rtd = recordTypes.get(tablename);
        if (rtd == null) return value;
        RecordFieldDefinition rtf = rtd.fields.get(fieldname);
        // reformat date to ISO format YYYY-MM-DD
        if (rtf.isDate) {
            if ((value != null) && (value.indexOf('.') > 0)) {
                String[] tmj = value.split("\\.");
                if (tmj.length < 3) return value;
                // a time String is appended to the date, keep it at end.
                if (tmj[2].indexOf(' ') > 0) return tmj[2]
                        .substring(0, tmj[2].indexOf(' ')) + "-" + tmj[1] + "-" + tmj[0] + tmj[2]
                        .substring(tmj[2].indexOf(' '));
                else return tmj[2] + "-" + tmj[1] + "-" + tmj[0];
            } else return value;
        }
        if (rtf.maxLength == 0) return value;
        if (value.length() >= rtf.maxLength) return value.substring(0, rtf.maxLength - 4) + " ...";
        else return value;
    }

    /**
     * Add a data record's metadata to the TableBuilder structure. Will do nothing, if the data
     * record type already exists.
     *
     * @param persistence persistence (local table file reference) of data record to be added, e.g.
     *                    PersonRecord.getPersistence()
     * @param metaData    metadata to add.
     */
    public synchronized void addDataRecord(StorageObject persistence, MetaData metaData) {
        if (persistence == null || persistence.data().getStorageType() != IDataAccess.TYPE_EFA_CLOUD) return;
        String recordType = efaCloudTableNames.get(persistence.data().getStorageObjectType());
        if (recordTypes.get(recordType) != null) return;
        RecordTypeDefinition rtd = new RecordTypeDefinition(recordType,
                (EfaCloudStorage) persistence.data());
        recordTypes.put(recordType, rtd);
        for (int i = 0; i < metaData.getFields().length; i++) {
            RecordFieldDefinition rfd = new RecordFieldDefinition(recordType,
                    metaData.getFields()[i], metaData.getFieldType(i));
            checkSpecialField(rfd);
            rfd.column = metaData.getFieldIndex(metaData.getFields()[i]);
            rtd.fields.put(rfd.fieldName, rfd);
            rtd.sqlDefinition.put(rfd.fieldName, rfd.sqlDefinition);
        }
        rtd.versionized = metaData.isVersionized();
    }

    /**
     * Remove a data record's metadata from the TableBuilder structure. Will do nothing, if the data
     * record type does not exist.
     *
     * @param efa2RecordType type of data record to be removed, e.g. efa2persons
     */
    public synchronized void removeDataRecord(String efa2RecordType) {
        String recordType = efaCloudTableNames.get(efa2RecordType);
        if (recordTypes.get(recordType) == null) return;
        recordTypes.remove(recordType);
    }

    /**
     * Set a key for a a data record's in the TableBuilder structure. Add the ValidFrom key for
     * versionized tables, even if not provided in "key". Will do nothing, if the data record type
     * does not exist. Will replace any existing key for the specified data record.
     *
     * @param efa2RecordType data record type for which the key shall be set, e.g. efa2persons.
     * @param key            key to set.
     */
    public synchronized void addKey(String efa2RecordType, String[] key) {
        String recordType = efaCloudTableNames.get(efa2RecordType);
        RecordTypeDefinition rtd = recordTypes.get(recordType);
        if (rtd == null) return;
        while (rtd.keys.size() > 0) rtd.keys.remove(0);
        Collections.addAll(rtd.keys, key);
        // special case: the "ValidFrom" key for versionized tables must explicitly be added.
        if (rtd.versionized && !rtd.keys.contains("ValidFrom")) rtd.keys.add("ValidFrom");
    }

    /**
     * Get a set of table names for up- and download activities
     *
     * @param useProject set true to get the project data tables
     * @param useConfig  set true to get the configuration tables
     * @return set of requested table names
     */
    public synchronized RecordTypeDefinition[] getTables(boolean useProject, boolean useConfig) {
        RecordTypeDefinition[] tables = new RecordTypeDefinition[recordTypes.keySet().size()];
        int i = 0;
        for (String tablename : recordTypes.keySet()) {
            RecordTypeDefinition rtd = recordTypes.get(tablename);
            if ((rtd.isProjectTable && useProject) || (!rtd.isProjectTable && useConfig))
                tables[i++] = rtd;
        }
        return tables;
    }

    /**
     * Build an efa table at the server side by issuing three respective API commands: createtable
     * (including all columns), autoincrement and unique. Because columns are created with the
     * table, there is no need for explicit use of addcolumns.
     *
     * @param storageType storageType of table to be built, e.g. "efa2persons".
     * @return an empty String on success, else an error message.
     */
    public String initServerTable(String storageType) {
        RecordTypeDefinition rtd = this.recordTypes.get(efaCloudTableNames.get(storageType));
        String recordType = rtd.recordType;
        TxRequestQueue txq = TxRequestQueue.getInstance();
        // create table
        int txIDstart = txq.appendTransactionPending(this, "createtable", recordType,
                rtd.tableDefinitionRecord());
        // add uniques
        if (rtd.uniques.size() > 0) for (String unique : rtd.uniques) {
            String record = unique + ";" + CsvCodec
                    .encodeElement(rtd.fields.get(unique).sqlDefinition, CsvCodec.DEFAULT_DELIMITER,
                            CsvCodec.DEFAULT_QUOTATION);
            txq.appendTransactionPending(this, "unique", recordType, record);
        }
        // add autoincrements
        if (rtd.autoincrements.size() > 0) for (String autoincrement : rtd.autoincrements) {
            // autoincrement fields must also be unique which is ensured programmatically to
            // secure the unique setting if before the autoincrement setting.
            String record = autoincrement + ";" + CsvCodec
                    .encodeElement(rtd.fields.get(autoincrement).sqlDefinition,
                            CsvCodec.DEFAULT_DELIMITER, CsvCodec.DEFAULT_QUOTATION);
            txq.appendTransactionPending(this, "unique", recordType, record);
            txq.appendTransactionPending(this, "autoincrement", recordType, record);
        }
        // append a nop transaction at the very end to wait for completion of all actions.
        Transaction tx = new Transaction(-1, "nop", rtd.recordType, new String[]{"sleep;0"});
        tx.executeSynchronously(txq);
        int failed = txq.getReceiptsFromIDonwards(txIDstart, this, false, true).size();
        if (failed > 0)
            return "Failed to initialize " + tx.tablename + ". " + failed + " transactions failed.";
        else return "";
    }

    /**
     * set the list of tables which need to be updated, because they changed at the server side.
     * This will also return those which were changed at this client. By comparing the key and the
     * last modified timestamp they can be identified and updating is skipped.
     */
    public void setTablesToUpdate(Transaction tx) {
        String[] counts = tx.getResultMessage().split(";");
        tablesToUpdate = new ArrayList<>();
        if (tx.getResultCode() < 400) {
            for (String count : counts) {
                try {
                    if (Integer.parseInt(count.split("=")[1]) > 0)
                        tablesToUpdate.add(recordTypes.get(count.split("=")[0]));
                } catch (Exception ignored) {
                }
            }
        }
    }


    /**
     * Small inner container holding a record type definition to provide structure
     */
    static class RecordTypeDefinition {
        final String recordType;
        final EfaCloudStorage persistence;
        final boolean isProjectTable;
        boolean versionized;
        HashMap<String, RecordFieldDefinition> fields = new HashMap<>();
        HashMap<String, String> sqlDefinition = new HashMap<>();
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> uniques = new ArrayList<>();
        ArrayList<String> autoincrements = new ArrayList<>();

        RecordTypeDefinition(String recordType, EfaCloudStorage persistence) {
            this.recordType = recordType;
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
            for (String fieldname : fields.keySet()) {
                RecordFieldDefinition rfd = fields.get(fieldname);
                // neither the field name nor the sqlDefinition need csv encoding.
                tdr[rfd.column] = rfd.fieldName + ";" + rfd.sqlDefinition;
            }
            return tdr;
        }

    }

    /**
     * Small inner container holding a field definition to provide structure
     */
    static class RecordFieldDefinition {
        final String recordType;
        final String fieldName;
        String constantName;
        String datatype;
        int column;
        int maxLength;
        boolean isDate;
        String sqlDefinition;

        /**
         * Set the datatype and the dependend properties datatype and sqlDefinition
         *
         * @param datatype data type to be set
         */
        void setDatatype(String datatype) {
            this.datatype = datatype;
            maxLength = (datatypeSizes.get(datatype) == null) ? 0 : (datatypeSizes
                    .get(datatype) - 2);  // two characters safety margin.
            sqlDefinition = datatypeDefaults.get(datatype);
            isDate = datatype.equalsIgnoreCase("DATA_DATE");
        }

        /**
         * Constructor
         *
         * @param fieldDef field definition. Csv String as in efa2fieldSpecialDefinitions
         */
        RecordFieldDefinition(String fieldDef) {
            String[] elements = fieldDef.split(";");
            recordType = elements[0];
            constantName = elements[1];
            setDatatype(elements[2]);
            fieldName = elements[3];
        }

        /**
         * Constructor
         *
         * @param recordType    the data record type as used in metadata, e.g. efa2persons
         * @param field         the field name, e.g. "LastName"
         * @param datatypeIndex the index of the data type, e.g. 0 for IDataAccess.DATA_STRING
         */
        RecordFieldDefinition(String recordType, String field, int datatypeIndex) {
            this.recordType = recordType;
            constantName = field.toUpperCase();
            setDatatype(TableBuilder.datatypes.get(datatypeIndex));
            maxLength = (datatypeSizes.get(datatype) == null) ? 0 : datatypeSizes.get(datatype);
            fieldName = field;
            sqlDefinition = datatypeDefaults.get(datatype);
        }

        public String toString() {
            return recordType + "." + fieldName + "(" + datatype + ")";
        }
    }
}
