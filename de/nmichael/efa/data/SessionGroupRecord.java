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
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.util.*;
import java.util.*;

// @i18n complete

public class SessionGroupRecord extends DataRecord {

    public static final String GROUP_NONE          = "NONE";
    public static final String GROUP_ADD           = "ADD";

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String ID                  = "Id";
    public static final String LOGBOOK             = "Logbook";
    public static final String NAME                = "Name";
    public static final String ROUTE               = "Route";
    public static final String ORGANIZER           = "Organizer";
    public static final String STARTDATE           = "StartDate";
    public static final String ENDDATE             = "EndDate";
    public static final String ACTIVEDAYS          = "ActiveDays";
    public static final String ECRID               = "ecrid";

    public static final String[] IDX_LOGBOOK = new String[] { LOGBOOK };

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(ID);                                t.add(IDataAccess.DATA_UUID);
        f.add(LOGBOOK);                           t.add(IDataAccess.DATA_STRING);
        f.add(NAME);                              t.add(IDataAccess.DATA_STRING);
        f.add(ROUTE);                             t.add(IDataAccess.DATA_STRING);
        f.add(ORGANIZER);                         t.add(IDataAccess.DATA_STRING);
        f.add(STARTDATE);                         t.add(IDataAccess.DATA_DATE);
        f.add(ENDDATE);                           t.add(IDataAccess.DATA_DATE);
        f.add(ACTIVEDAYS);                        t.add(IDataAccess.DATA_INTEGER);
        f.add(ECRID);                             t.add(IDataAccess.DATA_STRING);

        MetaData metaData = constructMetaData(SessionGroups.DATATYPE, f, t, false);
        metaData.setKey(new String[] { ID });
        metaData.addIndex(IDX_LOGBOOK);
    }

    public SessionGroupRecord(SessionGroups sessionGroups, MetaData metaData) {
        super(sessionGroups, metaData);
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

    public void setLogbook(String logbook) {
        setString(LOGBOOK, logbook);
    }
    public String getLogbook() {
        return getString(LOGBOOK);
    }
    public Vector<LogbookRecord> getAllReferencingLogbookRecords() {
        String logbookName = getLogbook();
        Logbook logbook = (logbookName != null ? getPersistence().getProject().getLogbook(logbookName, false) : null);
        UUID id = getId();
        if (logbook != null && id != null) {
            try {
                Vector<LogbookRecord> records = new Vector<LogbookRecord>();
                DataKeyIterator it = logbook.data().getStaticIterator();
                DataKey k = it.getFirst();
                while (k != null) {
                    LogbookRecord r = logbook.getLogbookRecord(k);
                    if (r != null && r.getSessionGroupId() != null && id.equals(r.getSessionGroupId())) {
                        records.add(r);
                    }
                    k = it.getNext();
                }
                return records;
            } catch(Exception e) {
                Logger.log(e);
            }
        }
        return null;
    }

    public void setName(String name) {
        setString(NAME, name);
    }
    public String getName() {
        return getString(NAME);
    }

    public void setRoute(String route) {
        setString(ROUTE, route);
    }
    public String getRoute() {
        return getString(ROUTE);
    }

    public void setOrganizer(String organizer) {
        setString(ORGANIZER, organizer);
    }
    public String getOrganizer() {
        return getString(ORGANIZER);
    }

    public void setStartDate(DataTypeDate date) {
        setDate(STARTDATE, date);
    }
    public DataTypeDate getStartDate() {
        return getDate(STARTDATE);
    }

    public void setEndDate(DataTypeDate date) {
        setDate(ENDDATE, date);
    }
    public DataTypeDate getEndDate() {
        return getDate(ENDDATE);
    }

    public void setActiveDays(int days) {
        setInt(ACTIVEDAYS, days);
    }
    public int getActiveDays() {
        return getInt(ACTIVEDAYS);
    }

    public String[] getQualifiedNameFields() {
        return new String[] { NAME };
    }

    public String getQualifiedName() {
        return getName();
    }

    public Object getUniqueIdForRecord() {
        return getId();
    }

    public boolean checkLogbookRecordFitsIntoRange(LogbookRecord r) {
        if (getStartDate() != null && getEndDate() != null && getStartDate().isSet() && getEndDate().isSet()) {
            if (r.getDate() != null && r.getDate().isSet() && (r.getDate().isBefore(getStartDate()) || r.getDate().isAfter(getEndDate()))) {
                return false;
            }
            if (r.getEndDate() != null && r.getEndDate().isSet() && (r.getEndDate().isBefore(getStartDate()) || r.getEndDate().isAfter(getEndDate()))) {
                return false;
            }
        }
        return true;
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        String CAT_BASEDATA     = "%01%" + International.getString("Fahrtgruppe");
        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();

        v.add(item = new ItemTypeString(LOGBOOK, getLogbook(),
                IItemType.TYPE_EXPERT, CAT_BASEDATA,
                International.getString("Fahrtenbuch")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(NAME, getName(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                International.getString("Name")));
        item.setNotNull(true);
        v.add(item = new ItemTypeString(ROUTE, getRoute(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                International.getString("Start & Ziel") + " / " +
                International.getString("Strecke")));
        if (Daten.efaConfig.getValueUseFunctionalityCanoeingGermany()) {
            v.add(item = new ItemTypeString(ORGANIZER, getOrganizer(),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                    International.getString("Veranstalter")));
        }
        v.add(item = new ItemTypeDate(STARTDATE, getStartDate(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                International.getString("Startdatum")));
        item.setNotNull(true);
        ItemTypeDate startDate = (ItemTypeDate)item;
        v.add(item = new ItemTypeDate(ENDDATE, getEndDate(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                International.getString("Enddatum")));
        item.setNotNull(true);
        ((ItemTypeDate)item).setMustBeAfter((ItemTypeDate)startDate, true);
        v.add(item = new ItemTypeInteger(ACTIVEDAYS, getActiveDays(), 1, Integer.MAX_VALUE,
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                International.getString("aktive Tage")));
        return v;
    }

    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[3];
        header[0] = new TableItemHeader(International.getString("Name"));
        header[1] = new TableItemHeader(International.getString("Startdatum"));
        header[2] = new TableItemHeader(International.getString("Enddatum"));
        return header;
    }

    public TableItem[] getGuiTableItems() {
        TableItem[] items = new TableItem[3];
        items[0] = new TableItem(getName());
        items[1] = new TableItem(getStartDate());
        items[2] = new TableItem(getEndDate());
        return items;
    }
    
}
