/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core;

import de.nmichael.efa.util.EfaUtil;

public class BackupMetaDataItem {

    private String name;
    private String type;
    private String filename;
    private String description;
    private long records;
    private long scn;

    public BackupMetaDataItem() {
    }

    public BackupMetaDataItem(String name, String type, String filename, String description,
            long records, long scn) {
        this.name = name;
        this.type = type;
        this.filename = filename;
        this.description = description;
        this.records = records;
        this.scn = scn;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFileName(String filename) {
        this.filename = filename;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setScn(long scn) {
        this.scn = scn;
    }

    public void setNumberOfRecords(long records) {
        this.records = records;
    }

    public String getName() {
        return name;
    }
    
    public String getType() {
        return type;
    }

    public String getNameAndType() {
        return name + "." + type;
    }

    public String getKeyForTable() {
        String pc = "C";
        if (Backup.isProjectDataAccess(type)) {
            pc = "P";
        }
        return pc + "_" + description + "_" + name + "." + type;
    }

    public String getFileName() {
        return filename;
    }

    public String getFileNameWithSlash() {
        return EfaUtil.replace(filename, "\\", "/", true);
    }

    public String getFileNameWithBackslash() {
        return EfaUtil.replace(filename, "/", "\\", true);
    }

    public String getDescription() {
        return description;
    }

    public long getScn() {
        return scn;
    }

    public long getNumberOfRecords() {
        return records;
    }
}
