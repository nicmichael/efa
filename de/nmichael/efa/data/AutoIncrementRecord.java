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

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.*;
import java.util.*;

// @i18n complete

public class AutoIncrementRecord extends DataRecord {

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String SEQUENCE             = "Sequence";
    public static final String INTVALUE             = "IntValue";
    public static final String LONGVALUE            = "LongValue";
    public static final String ECRID                = "ecrid";

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(SEQUENCE);                 t.add(IDataAccess.DATA_STRING);
        f.add(INTVALUE);                 t.add(IDataAccess.DATA_INTEGER);
        f.add(LONGVALUE);                t.add(IDataAccess.DATA_LONGINT);
        f.add(ECRID);                    t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(AutoIncrement.DATATYPE, f, t, false);
        metaData.setKey(new String[] { SEQUENCE });
    }

    public AutoIncrementRecord(AutoIncrement autoIncrement, MetaData metaData) {
        super(autoIncrement, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<String,String,String>(getSequence(),null,null);
    }

    public static DataKey getKey(String sequence) {
        return new DataKey<String,String,String>(sequence,null,null);
    }

    protected void setSequence(String sequence) {
        setString(SEQUENCE, sequence);
    }
    public String getSequence() {
        return getString(SEQUENCE);
    }

    protected void setIntValue(int value) {
        setInt(INTVALUE, value);
    }
    public int getIntValue() {
        return getInt(INTVALUE);
    }

    protected void setLongValue(long value) {
        setLong(LONGVALUE, value);
    }
    public long getLongValue() {
        return getLong(LONGVALUE);
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        return null; // not supported
    }

    public TableItemHeader[] getGuiTableHeader() {
        return null; // not supported
    }

    public TableItem[] getGuiTableItems() {
        return null; // not supported
    }

}
