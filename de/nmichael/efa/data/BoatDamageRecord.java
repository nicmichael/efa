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

import java.awt.GridBagConstraints;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JDialog;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListenerDataRecordTable;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeDateTime;
import de.nmichael.efa.core.items.ItemTypeDecimal;
import de.nmichael.efa.core.items.ItemTypeLabelHeader;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDecimal;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.dataedit.BoatDamageEditDialog;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete

public class BoatDamageRecord extends DataRecord {

    public static final String SEVERITY_FULLYUSEABLE   = "FULLYUSEABLE";
    public static final String SEVERITY_LIMITEDUSEABLE = "LIMITEDUSEABLE";
    public static final String SEVERITY_NOTUSEABLE     = "NOTUSEABLE";

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String BOATID               = "BoatId";
    public static final String DAMAGE               = "Damage";
    public static final String DESCRIPTION          = "Description";
    public static final String SEVERITY             = "Severity";
    public static final String FIXED                = "Fixed";
    public static final String REPORTDATE           = "ReportDate";
    public static final String REPORTTIME           = "ReportTime";
    public static final String FIXDATE              = "FixDate";
    public static final String FIXTIME              = "FixTime";
    public static final String REPORTEDBYPERSONID   = "ReportedByPersonId";
    public static final String REPORTEDBYPERSONNAME = "ReportedByPersonName";
    public static final String FIXEDBYPERSONID      = "FixedByPersonId";
    public static final String FIXEDBYPERSONNAME    = "FixedByPersonName";
    public static final String REPAIRCOSTS          = "RepairCosts";
    public static final String CLAIM                = "Claim";
    public static final String NOTES                = "Notes";
    public static final String LOGBOOKTEXT          = "LogbookText";
    public static final String ECRID                = "ecrid";

    public static final String[] IDX_BOATID = new String[] { BOATID };

    public static final String GUIITEM_REPORTDATETIME = "GUIITEM_REPORTDATETIME";
    public static final String GUIITEM_FIXDATETIME    = "GUIITEM_FIXDATETIME";
    
	public static final int COLUMN_ID_BOAT_NAME=0;        
	public static final int COLUMN_ID_DAMAGE=1;  
	public static final int COLUMN_ID_REPORTDATE=2;
	
    private boolean showOnlyAddDamageFields = false;

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(BOATID);                   t.add(IDataAccess.DATA_UUID);
        f.add(DAMAGE);                   t.add(IDataAccess.DATA_INTEGER);
        f.add(DESCRIPTION);              t.add(IDataAccess.DATA_STRING);
        f.add(SEVERITY);                 t.add(IDataAccess.DATA_STRING);
        f.add(FIXED);                    t.add(IDataAccess.DATA_BOOLEAN);
        f.add(REPORTDATE);               t.add(IDataAccess.DATA_DATE);
        f.add(REPORTTIME);               t.add(IDataAccess.DATA_TIME);
        f.add(FIXDATE);                  t.add(IDataAccess.DATA_DATE);
        f.add(FIXTIME);                  t.add(IDataAccess.DATA_TIME);
        f.add(REPORTEDBYPERSONID);       t.add(IDataAccess.DATA_UUID);
        f.add(REPORTEDBYPERSONNAME);     t.add(IDataAccess.DATA_STRING);
        f.add(FIXEDBYPERSONID);          t.add(IDataAccess.DATA_UUID);
        f.add(FIXEDBYPERSONNAME);        t.add(IDataAccess.DATA_STRING);
        f.add(REPAIRCOSTS);              t.add(IDataAccess.DATA_DECIMAL);
        f.add(CLAIM);                    t.add(IDataAccess.DATA_BOOLEAN);
        f.add(NOTES);                    t.add(IDataAccess.DATA_STRING);
        f.add(LOGBOOKTEXT);              t.add(IDataAccess.DATA_STRING);
        f.add(ECRID);                    t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(BoatDamages.DATATYPE, f, t, false);
        metaData.setKey(new String[] { BOATID, DAMAGE });
        metaData.addIndex(IDX_BOATID);
    }

    public BoatDamageRecord(BoatDamages boatDamage, MetaData metaData) {
        super(boatDamage, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<UUID,Integer,String>(getBoatId(),getDamage(),null);
    }

    public static DataKey getKey(UUID id, int res) {
        return new DataKey<UUID,Integer,String>(id,res,null);
    }

    public boolean isValidAt(long validAt) {
        return true;
        // BOat Damages are always valid and should be shown even if the boat is invalid
        //return getPersistence().getProject().getBoats(false).isValidAt(getBoatId(), validAt);
    }

    public boolean getDeleted() {
        return getPersistence().getProject().getBoats(false).isBoatDeleted(getBoatId());
    }

    public void setBoatId(UUID id) {
        setUUID(BOATID, id);
    }
    public UUID getBoatId() {
        return getUUID(BOATID);
    }

    public BoatRecord getBoatRecord() {
        try {
            Boats boats = getPersistence().getProject().getBoats(false);
            long t = (getReportDate() != null && getReportTime() != null ?
                getReportDate().getTimestamp(getReportTime()) :
                System.currentTimeMillis() );
            BoatRecord r = boats.getBoat(getBoatId(), t);
            if (r == null) {
                r = boats.getAnyBoatRecord(getBoatId());
            }
            return r;
        } catch (Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public String getBoatAsName() {
        BoatRecord r = getBoatRecord();
        return (r != null ? r.getQualifiedName() : null);
    }
    
    public void setDamage(int no) {
        setInt(DAMAGE, no);
    }
    public int getDamage() {
        return getInt(DAMAGE);
    }

    public void setDescription(String description) {
        setString(DESCRIPTION, description);
    }
    public String getDescription() {
        return getString(DESCRIPTION);
    }

    public void setSeverity(String severity) {
        setString(SEVERITY, severity);
    }
    public String getSeverity() {
        return getString(SEVERITY);
    }
    public String getSeverityDescription() {
        String s = getSeverity();
        if (s != null && s.equals(SEVERITY_FULLYUSEABLE)) {
            return International.getString("Boot voll benutzbar");
        }
        if (s != null && s.equals(SEVERITY_LIMITEDUSEABLE)) {
            return International.getString("Boot eingeschränkt benutzbar");
        }
        if (s != null && s.equals(SEVERITY_NOTUSEABLE)) {
            return International.getString("Boot nicht benutzbar");
        }
        return International.getString("unbekannt");
    }

    public void setFixed(boolean isFixed) {
        setBool(FIXED, isFixed);
    }
    public boolean getFixed() {
        return getBool(FIXED);
    }

    public int getPriority() {
        if (getFixed()) {
            return 9;
        }
        String severity = getSeverity();
        if (severity != null && severity.equals(SEVERITY_NOTUSEABLE)) {
            return 1;
        }
        if (severity != null && severity.equals(SEVERITY_LIMITEDUSEABLE)) {
            return 2;
        }
        if (severity != null && severity.equals(SEVERITY_FULLYUSEABLE)) {
            return 3;
        }
        return 5;
    }

    public void setReportDate(DataTypeDate date) {
        setDate(REPORTDATE, date);
    }
    public DataTypeDate getReportDate() {
        return getDate(REPORTDATE);
    }

    public void setReportTime(DataTypeTime time) {
        setTime(REPORTTIME, time);
    }
    public DataTypeTime getReportTime() {
        return getTime(REPORTTIME);
    }

    public void setFixDate(DataTypeDate date) {
        setDate(FIXDATE, date);
    }
    public DataTypeDate getFixDate() {
        return getDate(FIXDATE);
    }

    public void setFixTime(DataTypeTime time) {
        setTime(FIXTIME, time);
    }
    public DataTypeTime getFixTime() {
        return getTime(FIXTIME);
    }

    public void setReportedByPersonId(UUID id) {
        setUUID(REPORTEDBYPERSONID, id);
    }
    public UUID getReportedByPersonId() {
        return getUUID(REPORTEDBYPERSONID);
    }

    public void setReportedByPersonName(String name) {
        setString(REPORTEDBYPERSONNAME, name);
    }
    public String getReportedByPersonName() {
        return getString(REPORTEDBYPERSONNAME);
    }

    public String getReportedByPersonAsName() {
        UUID id = getReportedByPersonId();
        if (id != null) {
            try {
                Persons persons = getPersistence().getProject().getPersons(false);
                return persons.getPerson(id, getReportDate().getTimestamp(getReportTime())).getQualifiedName();
            } catch(Exception e) {
                Logger.logdebug(e);
                return null;
            }
        } else {
            return getReportedByPersonName();
        }
    }

    public void setFixedByPersonId(UUID id) {
        setUUID(FIXEDBYPERSONID, id);
    }
    public UUID getFixedByPersonId() {
        return getUUID(FIXEDBYPERSONID);
    }

    public void setFixedByPersonName(String name) {
        setString(FIXEDBYPERSONNAME, name);
    }
    public String getFixedByPersonName() {
        return getString(FIXEDBYPERSONNAME);
    }

    public void setRepairCosts(DataTypeDecimal costs) {
        setDecimal(REPAIRCOSTS, costs);
    }
    public DataTypeDecimal getRepairCosts() {
        return getDecimal(REPAIRCOSTS);
    }

    public void setClaim(boolean isClaim) {
        setBool(CLAIM, isClaim);
    }
    public boolean getClaim() {
        return getBool(CLAIM);
    }

    public String getFixedByPersonAsName() {
        UUID id = getFixedByPersonId();
        if (id != null) {
            try {
                Persons persons = getPersistence().getProject().getPersons(false);
                return persons.getPerson(id, getFixDate().getTimestamp(getFixTime())).getQualifiedName();
            } catch(Exception e) {
                Logger.logdebug(e);
                return null;
            }
        } else {
            return getFixedByPersonName();
        }
    }

    public void setNotes(String notes) {
        setString(NOTES, notes);
    }
    public String getNotes() {
        return getString(NOTES);
    }

    public void setLogbookText(String text) {
        setString(LOGBOOKTEXT, text);
    }
    public String getLogbookText() {
        return getString(LOGBOOKTEXT);
    }

    public String getCompleteDamageInfo() {
        StringBuffer s = new StringBuffer();
        s.append(International.getMessage("Bootsschaden für {boat}", getBoatAsName()) + "\n==============================================\n");
        s.append(International.getString("Beschreibung") + ": " + getDescription() + "\n");
        s.append(International.getString("Schwere des Schadens") + ": " + getSeverityDescription() + "\n");
        s.append(International.getString("gemeldet am") + ": " + DataTypeDate.getDateTimeString(getReportDate(), getReportTime()) + "\n");
        s.append(International.getString("gemeldet von") + ": " + getReportedByPersonAsName() + "\n");
        return s.toString();
    }

    public String getShortDamageInfo() {
        return International.getString("Bootsschaden") + ": " +
                getDescription() + " (" + getSeverityDescription() + ")";
    }

    public static boolean isCommentBoatDamage(String s) {
        return s != null && s.startsWith(International.getString("Bootsschaden") + ": ");
    }

    public long getRepairDays() {
        DataTypeDate d1 = this.getReportDate();
        DataTypeDate d2 = this.getFixDate();
        if (d1 == null || d2 == null ||
            !d1.isSet() || !d2.isSet() ||
            d2.isBefore(d1)) {
            return 0;
        }
        return d2.getDifferenceDays(d1);
    }
    
    public String getAsText(String fieldName) {
        if (fieldName.equals(BOATID)) {
            return getBoatAsName();
        }
        if (fieldName.equals(REPORTEDBYPERSONID)) {
            if (get(REPORTEDBYPERSONID) != null) {
                return this.getReportedByPersonAsName();
            } else {
                return null;
            }
        }
        if (fieldName.equals(FIXEDBYPERSONID)) {
            if (get(FIXEDBYPERSONID) != null) {
                return this.getFixedByPersonAsName();
            } else {
                return null;
            }
        }
        return super.getAsText(fieldName);
    }

    public boolean setFromText(String fieldName, String value) {
        if (fieldName.equals(BOATID)) {
            Boats boats = getPersistence().getProject().getBoats(false);
            BoatRecord br = boats.getBoat(value, -1);
            if (br != null) {
                set(fieldName, br.getId());
            }
        } else if (fieldName.equals(REPORTEDBYPERSONID) ||
            fieldName.equals(FIXEDBYPERSONID)) {
            Persons persons = getPersistence().getProject().getPersons(false);
            PersonRecord pr = persons.getPerson(value, -1);
            if (pr != null) {
                set(fieldName, pr.getId());
            }
        } else {
            return super.setFromText(fieldName, value);
        }
        return (value.equals(getAsText(fieldName)));
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        String CAT_BASEDATA     = "%01%" + International.getString("Bootsschaden");
        String CAT_DETAILS      = "%02%" + International.getString("Details");
        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();
        v.add(item = new ItemTypeLabelHeader("GUI_BOAT_NAME",
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, " "+International.getMessage("Bootsschaden für {boat}", getBoatAsName())));
        item.setPadding(0, 0, 0, 10);
        item.setFieldGrid(6,GridBagConstraints.EAST, GridBagConstraints.BOTH);

        v.add(item = new ItemTypeString(BoatDamageRecord.DESCRIPTION, getDescription(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Beschreibung")));
        item.setNotNull(true);
        item.setFieldGrid(4,GridBagConstraints.EAST, GridBagConstraints.BOTH);
        v.add(item = new ItemTypeStringList(SEVERITY, getSeverity(),
                new String[] { "", SEVERITY_NOTUSEABLE, SEVERITY_LIMITEDUSEABLE, SEVERITY_FULLYUSEABLE },
                new String[] { "--- " + International.getString("bitte wählen") + " ---",
                               International.getString("Boot nicht benutzbar"),
                               International.getString("Boot eingeschränkt benutzbar"),
                               International.getString("Boot voll benutzbar")
                },
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                International.getString("Schwere des Schadens")));
        item.setNotNull(true);
        item.setFieldGrid(4,GridBagConstraints.EAST, GridBagConstraints.BOTH);
       
        v.add(item = new ItemTypeDateTime(GUIITEM_REPORTDATETIME, getReportDate(), getReportTime(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("gemeldet am")));
        item.setFieldGrid(1,GridBagConstraints.EAST, GridBagConstraints.BOTH);
        item.setPadding(0, 0, 10, 0);
        item.setFieldSize(150, 0);
        if (showOnlyAddDamageFields) {
            item.setEnabled(false);
        }
        
        v.add(item = getGuiItemTypeStringAutoComplete(BoatDamageRecord.REPORTEDBYPERSONID, null,
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                    getPersistence().getProject().getPersons(false), System.currentTimeMillis(), System.currentTimeMillis(),
                    International.getString("gemeldet von")));
        item.setFieldGrid(4, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);

        if (getReportedByPersonId() != null) {
            ((ItemTypeStringAutoComplete)item).setId(getReportedByPersonId());
        } else {
            ((ItemTypeStringAutoComplete)item).parseAndShowValue(getReportedByPersonName());
        }        
        ((ItemTypeStringAutoComplete)item).setNotNull(true);
        ((ItemTypeStringAutoComplete)item).setAlternateFieldNameForPlainText(BoatDamageRecord.REPORTEDBYPERSONNAME);

        if (!showOnlyAddDamageFields) {
            v.add(item = new ItemTypeString(BoatDamageRecord.LOGBOOKTEXT, getLogbookText(),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Fahrt")));
            item.setPadding(0, 0, 10, 0);
            item.setFieldGrid(4,GridBagConstraints.EAST, GridBagConstraints.BOTH);

            v.add(item = new ItemTypeString(BoatDamageRecord.NOTES, getNotes(),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Bemerkungen")));
            item.setFieldGrid(4,GridBagConstraints.EAST, GridBagConstraints.BOTH);

            
            
            v.add(item = new ItemTypeBoolean(BoatDamageRecord.FIXED, getFixed(),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Schaden wurde behoben")));
            item.setPadding(0, 0, 10, 0);
            v.add(item = new ItemTypeDateTime(GUIITEM_FIXDATETIME, getFixDate(), getFixTime(),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("behoben am")));
            item.setFieldGrid(1,GridBagConstraints.EAST, GridBagConstraints.BOTH);
            item.setFieldSize(150, 0);
            
            v.add(item = getGuiItemTypeStringAutoComplete(BoatDamageRecord.FIXEDBYPERSONID, null,
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                    getPersistence().getProject().getPersons(false), System.currentTimeMillis(), System.currentTimeMillis(),
                    International.getString("behoben von")));
            item.setFieldGrid(4, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
            if (getFixedByPersonId() != null) {
                ((ItemTypeStringAutoComplete) item).setId(getFixedByPersonId());
            } else {
                ((ItemTypeStringAutoComplete) item).parseAndShowValue(getFixedByPersonName());
            }
            ((ItemTypeStringAutoComplete) item).setAlternateFieldNameForPlainText(BoatDamageRecord.FIXEDBYPERSONNAME);

            
            v.add(item = new ItemTypeDecimal(BoatDamageRecord.REPAIRCOSTS, getRepairCosts(), 2, true,
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Reparaturkosten")));
            item.setFieldGrid(1,GridBagConstraints.EAST, GridBagConstraints.BOTH);
            item.setFieldSize(150, 0);

            v.add(item = new ItemTypeBoolean(BoatDamageRecord.CLAIM, getClaim(),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Versicherungsfall")));
        }
        return v;
    }

    public void saveGuiItems(Vector<IItemType> items) {
        for (IItemType item : items) {
            if (item.getName().equals(GUIITEM_REPORTDATETIME)) {
                setReportDate(((ItemTypeDateTime)item).getDate());
                setReportTime(((ItemTypeDateTime)item).getTime());
            }
            if (item.getName().equals(GUIITEM_FIXDATETIME)) {
                setFixDate(((ItemTypeDateTime)item).getDate());
                setFixTime(((ItemTypeDateTime)item).getTime());
            }
        }
        super.saveGuiItems(items);
    }

    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[5];
        header[0] = new TableItemHeader(International.getString("Boot"));
        header[1] = new TableItemHeader(International.getString("Schaden"));
        header[2] = new TableItemHeader(International.getString("gemeldet am"));
        header[3] = new TableItemHeader(International.getString("behoben am"));
        header[4] = new TableItemHeader(International.getString("Priorität"));
        return header;
    }

    public TableItem[] getGuiTableItems() {
        TableItem[] items = new TableItem[5];
        items[0] = new TableItem(getBoatAsName());
        items[1] = new TableItem(getDescription());
        items[2] = new TableItem(DataTypeDate.getDateTimeString(getReportDate(), getReportTime()));
        items[3] = new TableItem(DataTypeDate.getDateTimeString(getFixDate(), getFixTime()));
        items[4] = new TableItem(Integer.toString(getPriority()));
        if (!getFixed()) {
            items[0].setMarked(true);
            items[1].setMarked(true);
            items[2].setMarked(true);
            items[3].setMarked(true);
            items[4].setMarked(true);
        }
        return items;
    }

    public String getQualifiedName() {
        return International.getMessage("Schaden für {boat}", getBoatAsName());
    }

    public String[] getQualifiedNameFields() {
        return IDX_BOATID;
    }

    public void setShowOnlyAddDamageFields(boolean showOnlyAddDamageFields) {
        this.showOnlyAddDamageFields = showOnlyAddDamageFields;
    }
    
    public static boolean deleteCallbackForGUIs(JDialog parent, IItemListenerDataRecordTable caller, AdminRecord admin, StorageObject persistence,DataRecord[] records) {
        BoatDamageRecord unfixedDamage = null;
        for (int i=0; records != null && i<records.length; i++) {
            if (records[i] != null && !((BoatDamageRecord)records[i]).getFixed()) {
                unfixedDamage = (BoatDamageRecord)records[i];
                break;
            }
        }
        if (unfixedDamage == null) {
            return true;
        }

        switch(Dialog.auswahlDialog(International.getString("Bootsschaden löschen"),
                International.getString("Möchtest du den Bootsschaden als behoben markieren, oder " +
                                        "einen irrtümlich gemeldeten Schaden komplett löschen?"),
                International.getString("als behoben markieren"),
                International.getString("irrtümlich gemeldeten Schaden löschen"))) {
            case 0:
                BoatDamageEditDialog dlg = (BoatDamageEditDialog)caller.createNewDataEditDialog(parent, persistence, unfixedDamage);
                dlg.setAdmin(admin);
                ItemTypeBoolean fixed = (ItemTypeBoolean)dlg.getItem(BoatDamageRecord.FIXED);
                if (fixed != null) {
                    fixed.setValue(true);
                    fixed.setChanged();
                    dlg.itemListenerAction(fixed, null);
                    dlg.setFixedWasChanged();
                }
                IItemType focus = dlg.getItem(BoatDamageRecord.FIXEDBYPERSONID);
                if (focus != null) {
                    dlg.setRequestFocus(focus);
                }
                dlg.showDialog();
                return false;
            case 1:
                return true;
            default:
                return false;
        }
    }

}
