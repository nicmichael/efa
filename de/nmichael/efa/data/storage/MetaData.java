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

import java.util.*;
import de.nmichael.efa.Daten;
import de.nmichael.efa.util.*;

public class MetaData {

    private String dataType;
    protected String[] FIELDS;
    protected int[] TYPES;
    protected HashMap<String,Integer> FIELDIDX;
    protected String[] KEY;
    protected ArrayList<String[]> indices = new ArrayList<String[]>();
    protected boolean versionized;

    private static Hashtable<String,MetaData> metaData = new Hashtable<String,MetaData>();

    private MetaData(String dataType) {
        this.dataType = dataType;
    }

    public static MetaData constructMetaData(String dataType, Vector<String> fields, Vector<Integer> types, boolean versionized) {
        MetaData m = metaData.get(dataType);
        if (m != null) {
            metaData.remove(dataType);
        }
        m = new MetaData(dataType);
        m.FIELDS = new String[fields.size()];
        m.TYPES = new int[types.size()];
        m.FIELDIDX = new HashMap<String,Integer>();
        for (int i=0; i<m.FIELDS.length; i++) {
            m.FIELDS[i] = fields.get(i);
            m.TYPES[i] = types.get(i).intValue();
            m.FIELDIDX.put(m.FIELDS[i], i);
        }
        m.versionized = versionized;
        metaData.put(dataType, m);
        return m;
    }

    public static MetaData getMetaData(String dataType) {
        return metaData.get(dataType);
    }

    // The following function is not used in efa. It may be deleted. It is the only reason for implementing the
    // TableBuilder.removeStorageType function, so dropping removeMetaData would also simplify the TableBuilder class.
    @Deprecated
    public static void removeMetaData(String dataType) {
        if (metaData.get(dataType) != null) {
            metaData.remove(dataType);
            Daten.tableBuilder.removeStorageType(dataType);
        }
    }

    public void setKey(String[] key) {
        int l = 0;
        for (int i=0; i<key.length && key[i] != null; i++) {
            l++;
        }
        if (versionized) {
            l++;
        }
        if (l > 3) {
            throw new IllegalArgumentException("Too many KEY fields for Data Type: "+dataType);
        }
        KEY = new String[l];
        for (int i=0; i<l; i++) {
            if (i < key.length) {
                KEY[i] = key[i];
            } else {
                KEY[i] = DataRecord.VALIDFROM;
            }
        }
        Daten.tableBuilder.addKey(dataType, key);
    }

    public void addIndex(String[] fieldNames) {
        indices.add(fieldNames);
    }

    public String[][] getIndices() {
        String[][] idx = new String[indices.size()][];
        for (int i=0; i<idx.length; i++) {
            idx[i] = indices.get(i);
        }
        return idx;
    }

    public int getNumberOfFields() {
        return FIELDS.length;
    }

    public int getFieldIndex(String fieldName) {
        try {
            return FIELDIDX.get(fieldName).intValue();
        } catch(Exception e) {
            if (Logger.isTraceOn(Logger.TT_XMLFILE, 1)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DATA_FIELDDOESNOTEXIST,
                    "MetaData.getIndex(\""+fieldName+"\") - Field does not exist for DataType " + dataType + "!");
            }
            Logger.logdebug(e);
            return -1;
        }
    }

    public boolean isField(String fieldName) {
        return FIELDIDX.get(fieldName) != null;
    }

    public String getFieldName(int i) {
        return FIELDS[i];
    }

    public String getFieldName(String fieldName) {
        return getFieldName(getFieldIndex(fieldName));
    }

    public int getFieldType(int i) {
        return TYPES[i];
    }

    /**
     * Returns the iDataAccess field type of a given field.
     * If fieldName does not exists, it returns iDataAccess.DATA_UNKOWN.
     * 
     * @param fieldName
     * @return iDataAccess field type value (int). If fieldName does not exist, it returns IDataAccess.DATA_UNKOWN.
     */
    public int getFieldType(String fieldName) {
        int fieldIndex = getFieldIndex(fieldName);
    	if (fieldIndex>=0) {
    		return getFieldType(fieldIndex);
    	} else {
    		return IDataAccess.DATA_UNKNOWN;
    	}
    }

    public String[] getKeyFields() {
        return Arrays.copyOf(KEY, KEY.length);
    }

    public boolean isKeyField(int fieldIdx) {
        return isKeyField(FIELDS[fieldIdx]);
    }

    public boolean isKeyField(String fieldName) {
        for (int i=0; i<KEY.length; i++) {
            if(KEY[i].equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isVersionized() {
        return versionized;
    }

    public String[] getFields() {
        return Arrays.copyOf(FIELDS, FIELDS.length);
    }

}
