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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.util.*;
import java.util.*;

// @i18n complete

public class WatersRecord extends DataRecord {

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String ID                  = "Id";
    public static final String EFBID               = "EfbId";
    public static final String NAME                = "Name";
    public static final String DETAILS             = "Details";
    public static final String ECRID               = "ecrid";

    public static final String[] IDX_NAME = new String[] { NAME };

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(ID);                                t.add(IDataAccess.DATA_UUID);
        f.add(NAME);                              t.add(IDataAccess.DATA_STRING);
        f.add(DETAILS);                           t.add(IDataAccess.DATA_STRING);
        f.add(EFBID);                             t.add(IDataAccess.DATA_STRING);
        f.add(ECRID);                             t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(Waters.DATATYPE, f, t, false);
        metaData.setKey(new String[] { ID });
        metaData.addIndex(IDX_NAME);
    }

    public WatersRecord(Waters waters, MetaData metaData) {
        super(waters, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<UUID,String,String>(getId(),null,null);
    }

    public static DataKey getKey(UUID id) {
        return new DataKey<UUID,String,String>(id,null,null);
    }

    public void setId(UUID id) {
        setUUID(ID, id);
    }
    public UUID getId() {
        return getUUID(ID);
    }

    public void setEfbId(String id) {
        setString(EFBID, id);
    }
    public String getEfbId() {
        return getString(EFBID);
    }

    public void setName(String name) {
        setString(NAME, name);
    }
    public String getName() {
        return getString(NAME);
    }

    public void setDetails(String details) {
        setString(DETAILS, details);
    }
    public String getDetails() {
        return getString(DETAILS);
    }

    public String[] getQualifiedNameFields() {
        return IDX_NAME;
    }

    public Object getUniqueIdForRecord() {
        return getId();
    }

    public String getQualifiedName() {
        String name = getName();
        return (name != null ? name : "");
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        String CAT_BASEDATA     = "%01%" + International.getString("Gewässer");
        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();
        v.add(item = new ItemTypeString(WatersRecord.NAME, getName(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Name")));
        v.add(item = new ItemTypeTextArea(WatersRecord.DETAILS, getDetails(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Details")));
        ((ItemTypeTextArea)item).setFieldSize(400, 100);
        ((ItemTypeTextArea)item).setWrap(true);
        if (Daten.efaConfig.getValueUseFunctionalityCanoeingGermany()) {
            v.add(item = new ItemTypeString(WatersRecord.EFBID, getEfbId(),
            		(Daten.efaConfig.getValueKanuEfb_AlwaysShowKanuEFBFields() ? IItemType.TYPE_PUBLIC : IItemType.TYPE_EXPERT), 
            		CAT_BASEDATA, International.onlyFor("Kanu-eFB ID","de")));
        }
        return v;
    }

    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[2];
        header[0] = new TableItemHeader(International.getString("Name"));
        header[1] = new TableItemHeader(International.getString("Details"));
        return header;
    }

    public TableItem[] getGuiTableItems() {
        TableItem[] items = new TableItem[2];
        items[0] = new TableItem(getName());
        items[1] = new TableItem(getDetails());
        return items;
    }

}
