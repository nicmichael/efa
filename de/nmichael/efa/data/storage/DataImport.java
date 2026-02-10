/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.storage;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.util.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

public class DataImport extends ProgressTask {

	public static final String IMPORTMODE_ADD    = "ADD";           // import as new record; fail for duplicates (also for duplicate versionized records with different validity)
    public static final String IMPORTMODE_UPD    = "UPDATE";        // update existing record; fail if record doesn't exist (for versionized: if no version exists)
    public static final String IMPORTMODE_ADDUPD = "ADD_OR_UPDATE"; // add, or if duplicate, update

    // Import Options for Logbook Import
    public static final String ENTRYNO_DUPLICATE_SKIP   = "DUPLICATE_SKIP";   // if duplicate EntryId, skip entry
    public static final String ENTRYNO_DUPLICATE_ADDEND = "DUPLICATE_ADDEND"; // if duplicate EntryId, add entry with new EntryId at end
    public static final String ENTRYNO_ALWAYS_ADDEND    = "ALWAYS_ADDEND";    // add all entries with new EntryId at end

    // only relevant for versionized storage objects
    public static final String UPDMODE_UPDATEVALIDVERSION = "UPDVERSION"; // update version which is valid at specified timestamp; fail if no version is valid
    public static final String UPPMODE_CREATENEWVERSION   = "NEWVERSION"; // always create a version at specified timestamp; fail if version for exact same timestamp exists

    private static final String UTF8_BOM = "\uFEFF";
    
    private StorageObject storageObject;
    private IDataAccess dataAccess;
    private String[] fields;
    private String[] keyFields;
    private String overrideKeyField;
    private boolean versionized;
    private String filename;
    private String encoding;
    private char csvSeparator;
    private char csvQuotes;
    private String importMode;
    private String logbookEntryNoHandling;
    private long validAt;
    private String updMode;
    private int importCount = 0;
    private int errorCount = 0;
    private int warningCount = 0;
    private boolean isLogbook = false;


    public DataImport(StorageObject storageObject,
            String filename, String encoding, char csvSeparator, char csvQuotes,
            String importMode, 
            String updMode,
            String logbookEntryNoHandling,
            long validAt) {
        super();
        this.storageObject = storageObject;
        this.dataAccess = storageObject.data();
        this.versionized = storageObject.data().getMetaData().isVersionized();
        this.fields = dataAccess.getFieldNames();
        this.keyFields = dataAccess.getKeyFieldNames();
        this.filename = filename;
        this.encoding = encoding;
        this.csvSeparator = csvSeparator;
        this.csvQuotes = csvQuotes;
        this.importMode = importMode;
        this.logbookEntryNoHandling = logbookEntryNoHandling;
        this.validAt = validAt;
        this.updMode = updMode;
        this.isLogbook = storageObject.data().getStorageObjectType().equals(Logbook.DATATYPE);
    }

    public static boolean isXmlFile(String filename) {
        try {
            BufferedReader f = new BufferedReader(new FileReader(filename));
            String s = f.readLine();
            boolean xml = (s != null && s.toLowerCase().startsWith("<?xml"));
            f.close();
            return xml;
        } catch(Exception eignore) {
            return false;
        }
    }

    private Vector<String> splitFields(String s) {
        Vector<String> fields = new Vector<String>();
        boolean inQuote = false;
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); i++) {
            if (!inQuote && s.charAt(i) == csvQuotes) {
                inQuote = true;
                continue;
            }
            if (inQuote && s.charAt(i) == csvQuotes) {
                inQuote = false;
                continue;
            }
            if (!inQuote && s.charAt(i) == csvSeparator) {
                fields.add(buf.toString());
                buf = new StringBuffer();
                continue;
            }
            buf.append(s.charAt(i));
        }
        fields.add(buf.toString());
        return fields;
    }

    private void logImportFailed(DataRecord r, String msg, Exception e) {
        if (e != null) {
            Logger.logdebug(e);
        }
        logInfo("\nERROR: " + LogString.operationFailed(
                International.getMessage("Import von Datensatz {record}", r.toString()),msg));
        errorCount++;
    }

    private void logImportWarning(DataRecord r, String msg) {
        logInfo("\nWARNING: " + msg + ": " + r.toString());
        warningCount++;
    }

    private long getValidFrom(DataRecord r) {
        long rv = r.getValidFrom();
        return (rv > 0 ? rv : validAt);
    }

    private long getInvalidFrom(DataRecord r) {
        long rv = r.getInvalidFrom();
        return (rv > 0 && rv < Long.MAX_VALUE ? rv : -1);
    }

    private void addRecord(DataRecord r) {
        try {
            if (versionized) {
                long myValidAt = getValidFrom(r);
                if (r.getInvalidFrom() > 0 && r.getInvalidFrom() <= myValidAt) {
                    // we're trying to add a new record which is already invalid at the time of adding.
                    // change its validFrom to 0
                    myValidAt = 0;
                }
                dataAccess.addValidAt(r, myValidAt);
                setCurrentWorkDone(++importCount);
            } else {
                dataAccess.add(r);
                setCurrentWorkDone(++importCount);
            }
        } catch (Exception e) {
            logImportFailed(r, e.toString(), e);
        }
    }

    private void updateRecord(DataRecord r, ArrayList<String> fieldsInInport) {
        try {
            DataRecord rorig = (versionized
                    ? dataAccess.getValidAt(r.getKey(), validAt)
                    : dataAccess.get(r.getKey()));
            if (rorig == null) {
                logImportFailed(r, International.getString("Keine gültige Version des Datensatzes gefunden."), null);
                return;
            }

            // has the import record an InvalidFrom field?
            long invalidFrom = (versionized ? getInvalidFrom(r) : -1);
            if (invalidFrom <= rorig.getValidFrom()) {
                invalidFrom = -1;
            }
            boolean changed = false;

            for (int i = 0; i < fields.length; i++) {
                Object o = r.get(fields[i]);
                if ((o != null || fieldsInInport.contains(fields[i]))
                        && !r.isKeyField(fields[i])
                        && !fields[i].equals(DataRecord.LASTMODIFIED)
                        && !fields[i].equals(DataRecord.VALIDFROM)
                        && !fields[i].equals(DataRecord.INVALIDFROM)
                        && !fields[i].equals(DataRecord.INVISIBLE)
                        && !fields[i].equals(DataRecord.DELETED)) {
                    Object obefore = rorig.get(fields[i]);
                    rorig.set(fields[i], o);
                    if ( (o != null && !o.equals(obefore)) ||
                         (o == null && obefore != null) ) {
                        changed = true;
                    }
                }
            }

            if (invalidFrom <= 0) {
                long myValidAt = getValidFrom(r);
                if (!versionized || updMode.equals(UPDMODE_UPDATEVALIDVERSION)
                        || rorig.getValidFrom() == myValidAt) {
                    if (changed) {
                        dataAccess.update(rorig);
                    }
                    setCurrentWorkDone(++importCount);
                }
                if (versionized && updMode.equals(UPPMODE_CREATENEWVERSION)
                        && rorig.getValidFrom() != myValidAt) {
                    if (changed) {
                        dataAccess.addValidAt(rorig, myValidAt);
                    }
                    setCurrentWorkDone(++importCount);
                }
            } else {
                dataAccess.changeValidity(rorig, rorig.getValidFrom(), invalidFrom);
                setCurrentWorkDone(++importCount);
            }
        } catch (Exception e) {
            logImportFailed(r, e.toString(), e);
        }
    }

    private boolean importRecord(DataRecord r, ArrayList<String> fieldsInInport) {
        try {
            if (Logger.isDebugLogging()) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, 
                        "importing " + r.toString());
            }
            DataRecord[] otherVersions = null;

            if (importMode.equals(IMPORTMODE_ADD) &&
                logbookEntryNoHandling != null &&
                logbookEntryNoHandling.equals(ENTRYNO_ALWAYS_ADDEND)) {
                // determine new EntryId for logbook
                r.set(keyFields[0], ((Logbook) storageObject).getNextEntryNo());
            }

            if (isLogbook && (importMode.equals(IMPORTMODE_ADD) || importMode.equals(IMPORTMODE_ADDUPD))) {
                LogbookRecord lr = ((LogbookRecord) r);
                if (lr.getEntryId() == null || !lr.getEntryId().isSet() || lr.getEntryId().toString().length() == 0) {
                    r.set(keyFields[0], ((Logbook) storageObject).getNextEntryNo());
                }
            }

            DataKey key = r.getKey();
            if (key.getKeyPart1() == null || overrideKeyField != null) {
                // first key field is *not* set, or we're overriding the default key field
                DataKey[] keys = null;
                long searchValidAt = validAt;
                while (true) {
                    if (overrideKeyField == null) {
                        // -> search for record by QualifiedName
                        keys = dataAccess.getByFields(r.getQualifiedNameFields(), r.getQualifiedNameValues(r.getQualifiedName()),
                                (versionized ? searchValidAt : -1));
                    } else {
                        // -> search for record by user-specified key field
                        keys = dataAccess.getByFields(new String[]{overrideKeyField},
                                new String[]{r.getAsString(overrideKeyField)},
                                (versionized ? searchValidAt : -1));
                    }
                    if (versionized && r.getInvalidFrom() > 0 && r.getInvalidFrom() < searchValidAt) {
                        // the imported record's invalidFrom is set to a time before the import validAt time:
                        // someone may try to mark an existing record as deleted/invalid, so let's try to search
                        // for a valid record at the end of the validity period
                        searchValidAt = r.getInvalidFrom() - 1;
                    } else {
                        break;
                    }
                }
                if (keys != null && keys.length > 0) {
                    for (int i = 0; i < keyFields.length; i++) {
                        if (!keyFields[i].equals(DataRecord.VALIDFROM)) {
                            r.set(keyFields[i], keys[0].getKeyPart(i));
                        }
                    }
                } else {
                    for (int i = 0; i < keyFields.length; i++) {
                        if (!keyFields[i].equals(DataRecord.VALIDFROM)
                                && r.get(keyFields[i]) == null) {
                            if (dataAccess.getMetaData().getFieldType(keyFields[i]) == IDataAccess.DATA_UUID) {
                                r.set(keyFields[i], UUID.randomUUID());
                            } else {
                                logImportFailed(r, "KeyField(s) not set", null);
                                return false;
                            }
                        }
                    }
                }
            }
            key = r.getKey();

            if (versionized) {
                otherVersions = dataAccess.getValidAny(key);
            } else {
                DataRecord r1 = dataAccess.get(key);
                otherVersions = (r1 != null ? new DataRecord[] { r1 } : null);
            }

            if (importMode.equals(IMPORTMODE_ADD) &&
                otherVersions != null && otherVersions.length > 0 &&
                logbookEntryNoHandling != null &&
                logbookEntryNoHandling.equals(ENTRYNO_DUPLICATE_ADDEND)) {
                r.set(keyFields[0], ((Logbook) storageObject).getNextEntryNo());
                otherVersions = null;
            }

            if (importMode.equals(IMPORTMODE_ADD)) {
                if (otherVersions != null && otherVersions.length > 0) {
                    logImportFailed(r, International.getString("Datensatz existiert bereits"), null);
                    return false;
                } else {
                    addRecord(r);
                    return true;
                }
            }
            if (importMode.equals(IMPORTMODE_UPD)) {
                if (otherVersions == null || otherVersions.length == 0) {
                    logImportFailed(r, International.getString("Datensatz nicht gefunden"), null);
                    return false;
                } else {
                    updateRecord(r, fieldsInInport);
                    return true;
                }
            }
            if (importMode.equals(IMPORTMODE_ADDUPD)) {
                if (otherVersions != null && otherVersions.length > 0) {
                    updateRecord(r, fieldsInInport);
                } else {
                    addRecord(r);
                }
                return true;
            }
        } catch (Exception e) {
            logImportFailed(r, e.getMessage(), e);
        }
        return false;
    }

    public int runXmlImport() {
        DataImportXmlParser responseHandler = null;
        try {
            XMLReader parser = EfaUtil.getXMLReader();
            responseHandler = new DataImportXmlParser(this, dataAccess);
            parser.setContentHandler(responseHandler);
            parser.parse(new InputSource(new FileInputStream(filename)));
        } catch (Exception e) {
            logInfo(e.toString());
            errorCount++;
            Logger.log(e);
            if (Daten.isGuiAppl()) {
                Dialog.error(e.toString());
            }
        }
        return (responseHandler != null ? responseHandler.getImportedRecordsCount() : 0);
    }

    public int runCsvImport() {
        int count = 0;
        try {
            int linecnt = 0;
            String[] header = null;
            ArrayList<String> fieldsInImport = new ArrayList<String>();
            BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
            String s;
            DataRecord dummyRecord = storageObject.createNewRecord();
            while ( (s = f.readLine()) != null) {
                if (linecnt==0) {
                	//Remove Excel's UTF-8 BOM prefix on the first line
                	s = s.replace(UTF8_BOM, "").trim();
                } else {
                	s= s.trim();
                }
               
                if (s.length() == 0)  {
                    continue;
                }
                Vector<String> fields = splitFields(s);
                if (fields.size() > 0) {
                    if (linecnt == 0) {
                        // header
                        header = new String[fields.size()];
                        for (int i=0; i<fields.size(); i++) {
                            header[i] = fields.get(i);
                            if (header[i].startsWith("#") && header[i].endsWith("#") && header.length > 2) {
                                header[i] = header[i].substring(1, header[i].length()-1).trim();
                                overrideKeyField = header[i];
                            }
                            String[] equivFields = dummyRecord.getEquivalentFields(header[i]);
                            for (String ef : equivFields) {
                                fieldsInImport.add(ef);
                            }
                        }
                    } else {
                        // fields
                        DataRecord r = storageObject.createNewRecord();
                        for (int i=0; i<header.length; i++) {
                            String value = (fields.size() > i ? fields.get(i) : null);
                            if (value != null && value.length() > 0) {
                                try {
                                    // special locale handling of imported decimals
                                	// the following code produces no more nullpointer exceptions in debug mode, as we check
                                	// wether the fieldname actually exists in the target record. this is not true for virtual fields like crew1 etc.
                                	int fieldType;
                                	String fieldName=header[i];
                                	if (dummyRecord.metaData.isField(fieldName)) {
                                		fieldType=dummyRecord.getFieldType(fieldName);
                                	} else {
                                		fieldType=IDataAccess.DATA_UNKNOWN;
                                	}
                                	
                                    if ((fieldType == IDataAccess.DATA_DECIMAL)||
                                    	(fieldType == IDataAccess.DATA_DOUBLE) ||
                                    	(fieldType == IDataAccess.DATA_DISTANCE)){
                                        value = EfaUtil.replace(value, Character.toString(International.getThousandsSeparator()), "");
                                        // any decimal separator which is defined in the locale of the current language is converted to US locale for import
                                        value = EfaUtil.replace(value, Character.toString(International.getDecimalSeparator()), ".");
                                    }
                                    if (!r.setFromText(header[i], value.trim())) {
                                        logImportWarning(r, "Value '" + value + "' for Field '"+header[i] + "' corrected to '" + r.getAsText(header[i]) + "'");
                                    }
                                } catch(Exception esetvalue) {
                                    logImportWarning(r, "Cannot set value '" + value + "' for Field '"+header[i] + "': " + esetvalue.toString());
                                }
                            }
                        }
                        if (importRecord(r, fieldsInImport)) {
                            count++;
                        }
                    }
                }
                linecnt++;
            }
            f.close();
        } catch(Exception e) {
            logInfo(e.toString());
            errorCount++;
            Logger.log(e);
            if (Daten.isGuiAppl()) {
                Dialog.error(e.toString());
            }
        }
        return count;
    }

    public void run() {
        setRunning(true);
        this.logInfo(International.getString("Importiere Datensätze ..."));
        if (isXmlFile(filename)) {
            runXmlImport();
        } else {
            runCsvImport();
        }
        this.logInfo("\n\n" + International.getMessage("{count} Datensätze erfolgreich importiert.", importCount));
        this.logInfo("\n" + International.getMessage("{count} Fehler.", errorCount));
        this.logInfo("\n" + International.getMessage("{count} Warnungen.", warningCount));

        // Start the Audit in the background to find any eventual inconsistencies
        (new Audit(Daten.project)).start();

        setDone();
    }

    public int getAbsoluteWork() {
        return 100; // just a guess
    }

    public String getSuccessfullyDoneMessage() {
        return International.getMessage("{count} Datensätze erfolgreich importiert.", importCount);
    }

    public void runImport(ProgressDialog progressDialog) {
        this.start();
        if (progressDialog != null) {
            progressDialog.showDialog();
        }
    }

    public class DataImportXmlParser extends XmlHandler {

        private DataImport dataImport;
        private IDataAccess dataAccess;
        private DataRecord record;
        private ArrayList<String> fieldsInImport;
        private boolean textImport;
        private int count = 0;

        public DataImportXmlParser(DataImport dataImport, IDataAccess dataAccess) {
            super(DataExport.FIELD_EXPORT);
            this.dataImport = dataImport;
            this.dataAccess = dataAccess;
        }

        public void startElement(String uri, String localName, String qname, Attributes atts) {
            super.startElement(uri, localName, qname, atts);

            if (localName.equals(DataExport.FIELD_EXPORT)) {
                String type = atts.getValue(DataExport.EXPORT_TYPE);
                textImport = (type != null && type.equals(DataExport.EXPORT_TYPE_TEXT));
            } else if (localName.equals(DataRecord.ENCODING_RECORD)) {
                // begin of record
                record = dataAccess.getPersistence().createNewRecord();
                fieldsInImport = new ArrayList<String>();
                return;
            } else {
                if (atts.getValue("key") != null && atts.getValue("key").equalsIgnoreCase("true")) {
                    overrideKeyField = localName;
                }
            }

        }

        public void endElement(String uri, String localName, String qname) {
            super.endElement(uri, localName, qname);

            if (record != null && localName.equals(DataRecord.ENCODING_RECORD)) {
                // end of record
                if (dataImport.importRecord(record, fieldsInImport)) {
                    count++;
                }
                record = null;
                fieldsInImport = null;
            }
            String fieldValue = getFieldValue();
            if (record != null && fieldValue != null) {
                // end of field
                try {
                    if (textImport) {
                        if (!record.setFromText(fieldName, fieldValue.trim())) {
                            dataImport.logImportWarning(record, "Value '" + fieldValue + "' for Field '" + fieldName + "' corrected to '" + record.getAsText(fieldName) + "'");
                        }
                    } else {
                        record.set(fieldName, fieldValue.trim());
                    }
                    String[] equivFields = record.getEquivalentFields(fieldName);
                    for (String f : equivFields) {
                        fieldsInImport.add(f);
                    }
                } catch (Exception esetvalue) {
                    dataImport.logImportWarning(record, "Cannot set value '" + fieldValue + "' for Field '" + fieldName + "': " + esetvalue.toString());
                }
            }
        }

        public int getImportedRecordsCount() {
            return count;
        }

    }
}
