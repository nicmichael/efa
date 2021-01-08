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

import de.nmichael.efa.util.*;
import java.io.*;
import java.util.*;

public class DataExport {

    public static final String FIELD_EXPORT = "export";
    public static final String EXPORT_TYPE = "type";
    public static final String EXPORT_TYPE_TEXT = "text";
    public static final String EXPORT_TYPE_ID   = "id";

    public enum Format {
        xml,
        csv
    }

    private StorageObject storageObject;
    private boolean exportAllLatest = false;
    private  long validAt;
    private Vector<DataRecord> selection;
    private String[] fields;
    private Format format;
    private String encoding;
    private String filename;
    private String exportType;
    private boolean versionized;
    private String lastError;

    public DataExport(StorageObject storageObject, long validAt, Vector<DataRecord> selection,
            String[] fields, Format format, String encoding, String filename, String exportType) {
        this.storageObject = storageObject;
        this.validAt = validAt;
        this.selection = selection;
        this.fields = fields;
        this.format = format;
        this.encoding = encoding;
        this.filename = filename;
        this.exportType = exportType;
        this.versionized = storageObject.data().getMetaData().isVersionized();
        this.exportAllLatest = versionized && selection == null && validAt < 0;
    }

    public int runExport() {
        int count = 0;
        try {
            BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,false), encoding));
            if (format == Format.xml) {
                fw.write("<?xml version=\"1.0\" encoding=\""+encoding+"\"?>\n");
                fw.write("<" + FIELD_EXPORT + " " + EXPORT_TYPE + "=\"" + exportType + "\">\n");
            }
            if (format == Format.csv) {
                for (int i=0; i<fields.length; i++) {
                    fw.write( (i > 0 ? "|" : "") + fields[i]);
                }
                fw.write("\n");
            }

            if (exportAllLatest) {
                // complete export of all records that ever existed, in their latest version
                Hashtable<DataKey, DataKey> duplicates = new Hashtable<DataKey, DataKey>();
                DataKeyIterator it = storageObject.data().getStaticIterator();
                DataKey k = it.getFirst();
                while (k != null) {
                    k = storageObject.data().getUnversionizedKey(k);
                    if (duplicates.get(k) == null) {
                        duplicates.put(k, k);
                        DataRecord r = storageObject.data().getValidLatest(k);
                        if (writeRecord(fw, r)) {
                            count++;
                        }
                    }
                    k = it.getNext();
                }
            } else if (selection == null) {
                // export of all records that are valid at a specified time
                DataKeyIterator it = storageObject.data().getStaticIterator();
                DataKey k = it.getFirst();
                while (k != null) {
                    DataRecord r = storageObject.data().get(k);
                    if (writeRecord(fw, r)) {
                        count++;
                    }
                    k = it.getNext();
                }
            } else {
                // export of all selected records
                for (DataRecord r : selection) {
                    if (writeRecord(fw, r)) {
                        count++;
                    }
                }
            }
            if (format == Format.xml) {
                fw.write("</" + FIELD_EXPORT + ">\n");
            }
            fw.close();
        } catch(Exception e) {
            Logger.logdebug(e);
            lastError = e.getMessage();
            return -1;
        }
        return count;
    }

    boolean writeRecord(BufferedWriter fw, DataRecord r) {
        try {
            if (r != null && 
                (exportAllLatest || (!r.getDeleted() && (!versionized || r.isValidAt(validAt)))) ) {
                if (format == Format.xml) {
                    fw.write("<" + DataRecord.ENCODING_RECORD + ">");
                }
                for (int i = 0; i < fields.length; i++) {
                    String value = r.getAsText(fields[i]);
                    if (format == Format.xml) {
                        if (value != null && value.length() > 0) {
                            fw.write("<" + fields[i] + ">" + EfaUtil.escapeXml(value) + "</" + fields[i] + ">");
                        }
                    }
                    if (format == Format.csv) {
                        fw.write((i > 0 ? "|" : "") + (value != null ? EfaUtil.replace(value, "|", "", true) : ""));
                    }
                }
                if (format == Format.xml) {
                    fw.write("</" + DataRecord.ENCODING_RECORD + ">\n");
                }
                if (format == Format.csv) {
                    fw.write("\n");
                }
                return true; // exported
            }
            return false; // not exported
        } catch (IOException e) {
            Logger.logdebug(e);
            return false;
        }
    }

    public String getLastError() {
        return lastError;
    }

}
