/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.config;

import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.util.*;
import java.util.*;

// @i18n complete

public class EfaConfigRecord extends DataRecord {

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String NAME                = "Name";
    public static final String VALUE               = "Value";

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(NAME);                              t.add(IDataAccess.DATA_STRING);
        f.add(VALUE);                             t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(EfaConfig.DATATYPE, f, t, false);
        metaData.setKey(new String[] { NAME });
    }

    public EfaConfigRecord(EfaConfig efaConfig, MetaData metaData) {
        super(efaConfig, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<String,String,String>(getName(),null,null);
    }

    public static DataKey getKey(String name) {
        return new DataKey<String,String,String>(name,null,null);
    }

    public void setName(String name) {
        setString(NAME, name);
    }
    public String getName() {
        return getString(NAME);
    }

    public void setValue(String value) {
        setString(VALUE, value);
    }
    public String getValue() {
        String s = getString(VALUE);
        return (s == null ? "" : s);
    }

    public String[] getQualifiedNameFields() {
        return new String[] { NAME };
    }

    public String getQualifiedName() {
        String name = getName();
        return (name != null ? name : "");
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        return null;
    }

    public TableItemHeader[] getGuiTableHeader() {
        return null;
    }

    public TableItem[] getGuiTableItems() {
        return null;
    }

}
