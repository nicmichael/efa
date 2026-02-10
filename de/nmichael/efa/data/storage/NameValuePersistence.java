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

// @i18n complete

public class NameValuePersistence { /*extends Persistence {

    public static final String DATATYPE = "efa2namevalue";

    public NameValuePersistence(int storageType, String storageLocation, String storageObjectName, String description) {
        super(storageType, storageLocation, storageObjectName, DATATYPE, description);
        NameValueDataRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new NameValueDataRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public NameValueDataRecord createNameValueDataRecord(String name, String value) {
        NameValueDataRecord r = new NameValueDataRecord(this, MetaData.getMetaData(DATATYPE));
        r.setName(name);
        r.setValue(value);
        return r;
    }

    public NameValueDataRecord getRecord(String name) {
        try {
            return ((NameValueDataRecord)data().get(NameValueDataRecord.getKey(name)));
        } catch(Exception e) {
            return null;
        }
    }

    public String getValue(String name) {
        try {
            return ((NameValueDataRecord)data().get(NameValueDataRecord.getKey(name))).getValue();
        } catch(Exception e) {
            return null;
        }
    }

    public boolean addValue(String name, String value) {
        try {
            NameValueDataRecord r = createNameValueDataRecord(name, value);
            data().add(r);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    public boolean updateValue(String name, String value) {
        try {
            NameValueDataRecord r = (NameValueDataRecord)data().get(NameValueDataRecord.getKey(name));
            r.setValue(value);
            data().update(r);
            return true;
        } catch(Exception e) {
            return false;
        }
    }
*/
}
