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
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.XMLWriter;
import de.nmichael.efa.util.XmlHandler;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class BackupMetaData extends XMLWriter {

    public static final String BACKUP_META = "backup.meta";
    
    public static final String FIELD_GLOBAL      = "EfaBackup";
    public static final String FIELD_TIMESTAMP   = "TimeStamp";
    public static final String FIELD_PROJECTNAME = "ProjectName";
    public static final String FIELD_OBJECT      = "Object";
    public static final String FIELD_NAME        = "Name";
    public static final String FIELD_TYPE        = "Type";
    public static final String FIELD_FILENAME    = "FileName";
    public static final String FIELD_DESCRIPTION = "Description";
    public static final String FIELD_RECORDS     = "Records";
    public static final String FIELD_SCN         = "Scn";

    private ArrayList<BackupMetaDataItem> data = new ArrayList<BackupMetaDataItem>();
    private String zipFileName;
    private long timestamp;
    private String projectName;

    public BackupMetaData(String projectName) {
        timestamp = System.currentTimeMillis();
        this.projectName = projectName;
    }

    public void addMetaDataItem(String name, String type, String filename, String description,
            long records, long scn) {
        data.add(new BackupMetaDataItem(name, type, filename, description, records, scn));
    }

    public void addMetaDataItem(BackupMetaDataItem meta) {
        data.add(meta);
    }

    public void setTimeStamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public void setProjectName(String name) {
        this.projectName = name;
    }

    public String getProjectName() {
        return projectName;
    }

    public int size() {
        return data.size();
    }

    public BackupMetaDataItem getItem(int i) {
        return data.get(i);
    }

    public BackupMetaDataItem getItem(String nameAndType) {
        for (int i=0; i<size(); i++) {
            BackupMetaDataItem meta = getItem(i);
            if (meta.getNameAndType().equals(nameAndType)) {
                return meta;
            }
        }
        return null;
    }

    public BackupMetaDataItem getTableItem(String tableKey) {
        for (int i=0; i<size(); i++) {
            BackupMetaDataItem meta = getItem(i);
            if (meta.getKeyForTable().equals(tableKey)) {
                return meta;
            }
        }
        return null;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public boolean write(ZipOutputStream zipOut) {
        try {
            ZipEntry entry = new ZipEntry(BACKUP_META);
            zipOut.putNextEntry(entry);
            writeHeader(zipOut);
            xmltagStart(FIELD_GLOBAL);
            xmltag(FIELD_TIMESTAMP, Long.toString(timestamp));
            if (projectName != null) {
                xmltag(FIELD_PROJECTNAME, projectName);
            }
            for (BackupMetaDataItem object : data) {
                xmltagStart(FIELD_OBJECT);
                xmltag(FIELD_NAME, object.getName());
                xmltag(FIELD_TYPE, object.getType());
                xmltag(FIELD_FILENAME, object.getFileName());
                xmltag(FIELD_DESCRIPTION, object.getDescription());
                xmltag(FIELD_RECORDS, Long.toString(object.getNumberOfRecords()));
                xmltag(FIELD_SCN, Long.toString(object.getScn()));
                xmltagEnd(FIELD_OBJECT);
            }
            xmltagEnd(FIELD_GLOBAL);
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }
        return true;
    }

    public boolean read(String zipFile) {
        try {
            zipFileName = null;
            try (ZipFile zip = new ZipFile(zipFile)) {
				ZipEntry entry = zip.getEntry(BACKUP_META);
				InputStream in = zip.getInputStream(entry);
				XMLReader parser = EfaUtil.getXMLReader();
				BackupMetaDataParser xmlhandler = new BackupMetaDataParser(this);
				parser.setContentHandler(xmlhandler);
				parser.parse(new InputSource(in));
			}
            zipFileName = zipFile;
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }
        return true;
    }

}

class BackupMetaDataParser extends XmlHandler {

    private BackupMetaData metaData;
    private BackupMetaDataItem item;

    public BackupMetaDataParser(BackupMetaData metaData) {
        super(BackupMetaData.FIELD_GLOBAL);
        this.metaData = metaData;
    }

    public void startElement(String uri, String localName, String qname, Attributes atts) {
        super.startElement(uri, localName, qname, atts);

        if (localName.equals(BackupMetaData.FIELD_OBJECT)) {
            item = new BackupMetaDataItem();
        }

    }

    public void endElement(String uri, String localName, String qname) {
        super.endElement(uri, localName, qname);

        if (localName.equals(BackupMetaData.FIELD_OBJECT)) {
            metaData.addMetaDataItem(item);
            item = null;
        }

        if (localName.equals(BackupMetaData.FIELD_TIMESTAMP)) {
            metaData.setTimeStamp(Long.parseLong(getFieldValue()));
        }

        if (localName.equals(BackupMetaData.FIELD_PROJECTNAME)) {
            metaData.setProjectName(getFieldValue());
        }

        if (item != null) {
            if (localName.equals(BackupMetaData.FIELD_NAME)) {
                item.setName(getFieldValue());
            }
            if (localName.equals(BackupMetaData.FIELD_TYPE)) {
                item.setType(getFieldValue());
            }
            if (localName.equals(BackupMetaData.FIELD_FILENAME)) {
                item.setFileName(getFieldValue());
            }
            if (localName.equals(BackupMetaData.FIELD_DESCRIPTION)) {
                item.setDescription(getFieldValue());
            }
            if (localName.equals(BackupMetaData.FIELD_RECORDS)) {
                item.setNumberOfRecords(Long.parseLong(getFieldValue()));
            }
            if (localName.equals(BackupMetaData.FIELD_SCN)) {
                item.setScn(Long.parseLong(getFieldValue()));
            }
        }
    }

}
