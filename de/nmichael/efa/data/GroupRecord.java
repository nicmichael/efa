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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;

import javax.swing.ImageIcon;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeColor;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

// @i18n complete

public class GroupRecord extends DataRecord implements IItemFactory {

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String ID                  = "Id";
    public static final String NAME                = "Name";
    public static final String COLOR               = "Color";
    public static final String MEMBERIDLIST        = "MemberIdList";
    public static final String ECRID               = "ecrid";

    public static final String[] IDX_NAME = new String[] { NAME };

    private static final String CAT_BASEDATA = "%01%" + International.getString("Gruppe");
    private static final String GUIITEM_MEMBERIDLIST = "GUIITEM_MEMBERIDLIST";

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(ID);                                t.add(IDataAccess.DATA_UUID);
        f.add(NAME);                              t.add(IDataAccess.DATA_STRING);
        f.add(COLOR);                             t.add(IDataAccess.DATA_STRING);
        f.add(MEMBERIDLIST);                      t.add(IDataAccess.DATA_LIST_UUID);
        f.add(ECRID);                             t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(Groups.DATATYPE, f, t, true);
        metaData.setKey(new String[] { ID }); // plus VALID_FROM
        metaData.addIndex(IDX_NAME);
    }

    public GroupRecord(Groups groups, MetaData metaData) {
        super(groups, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<UUID,Long,String>(getId(),getValidFrom(),null);
    }

    public static DataKey getKey(UUID id, long validFrom) {
        return new DataKey<UUID,Long,String>(id ,validFrom, null);
    }

    public void setId(UUID id) {
        setUUID(ID, id);
    }
    public UUID getId() {
        return getUUID(ID);
    }

    public void setName(String name) {
        setString(NAME, name);
    }
    public String getName() {
        return getString(NAME);
    }

    public void setColor(String color) {
        setString(COLOR, color);
    }
    public String getColor() {
        return getString(COLOR);
    }

    public void setMemberIdList(DataTypeList<UUID> list) {
        setList(MEMBERIDLIST, list);
    }
    public DataTypeList<UUID> getMemberIdList() {
        return getList(MEMBERIDLIST, IDataAccess.DATA_UUID);
    }

    public int getNumberOfMembers() {
        DataTypeList list = getMemberIdList();
        return (list == null ? 0 : list.length());
    }

    public String getMemberIdListAsString(long validAt) {
        DataTypeList<UUID> list = getMemberIdList();
        if (list == null) {
            return null;
        }
        StringBuffer s = new StringBuffer();
        Persons persons = getPersistence().getProject().getPersons(false);
        for (int i=0; i<list.length(); i++) {
            UUID id = list.get(i);
            PersonRecord r = persons.getPerson(id, validAt);
            if (r != null) {
                s.append((s.length() > 0 ? "; " : "") + r.getQualifiedName());
            }
        }
        return s.toString();
    }

    public boolean setPersonInGroup(UUID personId, boolean beInGroup) {
        DataTypeList<UUID> memberList = getMemberIdList();
        boolean changed = true;
        if (beInGroup) {
            if (memberList == null) {
                memberList = new DataTypeList<UUID>();
            }
            if (!memberList.contains(personId)) {
                memberList.add(personId);
                changed = true;
            }
        } else {
            if (memberList != null) {
                if (memberList.contains(personId)) {
                    memberList.remove(personId);
                    changed = true;
                }
            }
        }
        if (changed) {
            setMemberIdList(memberList);
        }
        return changed;
    }

    public String getQualifiedName() {
        String name = getName();
        return (name != null ? name : "");
    }

    public String[] getQualifiedNameFields() {
        return IDX_NAME;
    }

    public Object getUniqueIdForRecord() {
        return getId();
    }

    public String getAsText(String fieldName) {
        if (fieldName.equals(MEMBERIDLIST)) {
            return getMemberIdListAsString(System.currentTimeMillis());
        }
        return super.getAsText(fieldName);
    }

    public boolean setFromText(String fieldName, String value) {
        if (fieldName.equals(MEMBERIDLIST)) {
            Vector<String> values = EfaUtil.split(value, ';');
            DataTypeList<UUID> list = new DataTypeList<UUID>();
            Persons persons = getPersistence().getProject().getPersons(false);
            for (int i=0; i<values.size(); i++) {
                PersonRecord pr = persons.getPerson(values.get(i).trim(), -1);
                if (pr != null) {
                    list.add(pr.getId());
                }
            }
            if (list.length() > 0) {
                set(fieldName, list);
            }
        } else {
            return super.setFromText(fieldName, value);
        }
        return (value.equals(getAsText(fieldName)));
    }

    public IItemType[] getDefaultItems(String itemName) {
        if (itemName.equals(GroupRecord.GUIITEM_MEMBERIDLIST)) {
            IItemType[] items = new IItemType[1];
            items[0] = getGuiItemTypeStringAutoComplete(BoatRecord.ALLOWEDGROUPIDLIST, null,
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                    getPersistence().getProject().getPersons(false), getPreferredValidFrom(),
                    getPreferredInvalidFrom()-1,
                    International.getString("Mitglied"));
            items[0].setFieldSize(300, -1);
            return items;
        }
        return null;
    }

    private long getPreferredValidFrom() {
        try {
            Logbook l = persistence.getProject().getCurrentLogbook();
            return Math.max(l.getValidFrom(), getValidFrom());
        } catch(Exception e) {
            return getValidFrom();
        }
    }

    private long getPreferredInvalidFrom() {
        try {
            Logbook l = persistence.getProject().getCurrentLogbook();
            return Math.min(l.getInvalidFrom(), getInvalidFrom());
        } catch(Exception e) {
            return getInvalidFrom();
        }
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        Vector<IItemType[]> itemList;

        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();

        v.add(item = new ItemTypeString(GroupRecord.NAME, getName(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Gruppenname")));

        v.add(item = new ItemTypeColor(GroupRecord.COLOR, getColor(), "",
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Farbe"),true));

        DataTypeList<UUID> members = getMemberIdList();
        itemList = new Vector<IItemType[]>();
        for (int i=0; members != null && i<members.length(); i++) {
            IItemType[] items = getDefaultItems(GUIITEM_MEMBERIDLIST);
            ((ItemTypeStringAutoComplete)items[0]).setId(members.get(i));
            itemList.add(items);
        }
        v.add(item = new ItemTypeItemList(GUIITEM_MEMBERIDLIST, itemList, this,
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Mitglieder")));
        ((ItemTypeItemList)item).setAppendPositionToEachElement(true);
        ((ItemTypeItemList)item).setRepeatTitle(false);
        ((ItemTypeItemList) item).setXForAddDelButtons(3);
        ((ItemTypeItemList) item).setPadYbetween(0);

        return v;
    }

    public void saveGuiItems(Vector<IItemType> items) {
        Persons persons = getPersistence().getProject().getPersons(false);

        for(IItemType item : items) {
            String name = item.getName();
            if (name.equals(GUIITEM_MEMBERIDLIST) && item.isChanged()) {
                ItemTypeItemList list = (ItemTypeItemList)item;
                Hashtable<String,UUID> uuidList = new Hashtable<String,UUID>();
                for (int i=0; i<list.size(); i++) {
                    IItemType[] typeItems = list.getItems(i);
                    Object uuid = ((ItemTypeStringAutoComplete)typeItems[0]).getId(typeItems[0].toString());
                    if (uuid != null && uuid.toString().length() > 0) {
                        String text = ((ItemTypeStringAutoComplete)typeItems[0]).getValue();
                        if (text == null) {
                            text = "";
                        }
                        // sort based on text:uuid
                        uuidList.put(text + ":" + uuid.toString(), (UUID)uuid);
                    }
                }
                String[] keyArr = uuidList.keySet().toArray(new String[0]);
                Arrays.sort(keyArr);
                DataTypeList<UUID> memberList = new DataTypeList<UUID>();
                for (String key : keyArr) {
                    memberList.add(uuidList.get(key));
                }
                setMemberIdList(memberList);
            }
        }

        super.saveGuiItems(items);
    }

    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[2];
        header[0] = new TableItemHeader(International.getString("Gruppenname"));
        header[1] = new TableItemHeader(International.getString("Anzahl Mitglieder"));
        return header;
    }

    public TableItem[] getGuiTableItems() {
        TableItem[] items = new TableItem[2];
        items[0] = new TableItem(getName());
        items[1] = new TableItem(Integer.toString(getNumberOfMembers()));
        items[0].addIcon(this.getColorIconForGroup());
        return items;
    }
    
    private ImageIcon getColorIconForGroup() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(EfaUtil.getColor(this.getColor()));
	    g.fillOval(0, 0, 16,16);
        return new ImageIcon(image);    		
    }
}

