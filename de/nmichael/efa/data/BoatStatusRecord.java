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
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.util.*;
import java.util.*;

// @i18n complete

public class BoatStatusRecord extends DataRecord {

    // Status Keys (identical to old efa 1.x BootStatus (if changed, make sure to adapt import!)
    public static final String STATUS_HIDE            = "HIDE";             // BaseStatus                CurrentStatus
    public static final String STATUS_AVAILABLE       = "AVAILABLE";        // BaseStatus   StatusList   CurrentStatus
    public static final String STATUS_NOTAVAILABLE    = "NOTAVAILABLE";     // BaseStatus   StatusList   CurrentStatus
    public static final String STATUS_ONTHEWATER      = "ONTHEWATER";       //              StatusList   CurrentStatus

    public static final int ARRAY_STRINGLIST_VALUES  = 1;
    public static final int ARRAY_STRINGLIST_DISPLAY = 2;

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String BOATID              = "BoatId";        // most point to an exisiting boat, unless UNKNOWNBOAT=true
    public static final String BOATTEXT            = "BoatText";      // Boat Name copied from BoatRecord or set explicitly for unknown boats
    public static final String UNKNOWNBOAT         = "UnknownBoat";   // must be set to "true" if this boat does not appear in the boat list
    public static final String BASESTATUS          = "BaseStatus";    // the base status that this boat falls back to if it's not on the water
    public static final String CURRENTSTATUS       = "CurrentStatus"; // current status - may be on the water
    public static final String SHOWINLIST          = "ShowInList";    // the status list this boat appears in
    public static final String ONLYINBOATHOUSEID   = "OnlyInBoathouseId";
    public static final String LOGBOOK             = "Logbook";       // the name of the logbook EntryNo is pointing to
    public static final String ENTRYNO             = "EntryNo";       // the EntryNo if this boat in ONTHEWATER
    public static final String COMMENT             = "Comment";

    protected static String CAT_STATUS       = "%06%" + International.getString("Bootsstatus");
    
    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(BOATID);                   t.add(IDataAccess.DATA_UUID);
        f.add(BOATTEXT);                 t.add(IDataAccess.DATA_STRING);
        f.add(UNKNOWNBOAT);              t.add(IDataAccess.DATA_BOOLEAN);
        f.add(BASESTATUS);               t.add(IDataAccess.DATA_STRING);
        f.add(CURRENTSTATUS);            t.add(IDataAccess.DATA_STRING);
        f.add(SHOWINLIST);               t.add(IDataAccess.DATA_STRING);
        f.add(ONLYINBOATHOUSEID);        t.add(IDataAccess.DATA_STRING);
        f.add(LOGBOOK);                  t.add(IDataAccess.DATA_STRING);
        f.add(ENTRYNO);                  t.add(IDataAccess.DATA_INTSTRING);
        f.add(COMMENT);                  t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(BoatStatus.DATATYPE, f, t, false);
        metaData.setKey(new String[] { BOATID });
        metaData.addIndex(new String[] { CURRENTSTATUS });
        metaData.addIndex(new String[] { SHOWINLIST });
    }

    public BoatStatusRecord(BoatStatus boatStatus, MetaData metaData) {
        super(boatStatus, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<UUID,String,String>(getBoatId(),null,null);
    }

    public static DataKey getKey(UUID id) {
        return new DataKey<UUID,String,String>(id,null,null);
    }

    public boolean isValidAt(long validAt) {
        return getUnknownBoat() ||
                getPersistence().getProject().getBoats(false).isValidAt(getBoatId(), validAt);
    }

    public boolean getDeleted() {
        return getPersistence().getProject().getBoats(false).isBoatDeleted(getBoatId());
    }

    public boolean getInvisible() {
        return getPersistence().getProject().getBoats(false).isBoatInvisible(getBoatId());
    }

    public boolean getDeletedOrInvisible() {
        return getPersistence().getProject().getBoats(false).isBoatDeletedOrInvisible(getBoatId());
    }

    public void setBoatId(UUID id) {
        setUUID(BOATID, id);
    }
    public UUID getBoatId() {
        return getUUID(BOATID);
    }

    public String getBoatNameAsString(long validAt) {
        Boats b = getPersistence().getProject().getBoats(false);
        if (b != null) {
            BoatRecord r = b.getBoat(getBoatId(), validAt);
            if (r != null) {
                return r.getQualifiedName();
            }
        }
        return null;
    }

    public BoatRecord getBoatRecord(long validAt) {
        Boats b = getPersistence().getProject().getBoats(false);
        if (b != null) {
            return b.getBoat(getBoatId(), validAt);
        }
        return null;
    }

    public void setBoatText(String text) {
        setString(BOATTEXT, text);
    }
    public String getBoatText() {
        return getString(BOATTEXT);
    }

    public void setUnknownBoat(boolean unknown) {
        setBool(UNKNOWNBOAT, unknown);
    }
    public boolean getUnknownBoat() {
        return getBool(UNKNOWNBOAT);
    }

    public void setBaseStatus(String status) {
        if (status.equals(STATUS_HIDE) ||
            status.equals(STATUS_AVAILABLE) ||
            status.equals(STATUS_NOTAVAILABLE)) {
            setString(BASESTATUS, status);
        }
    }
    public String getBaseStatus() {
        return getString(BASESTATUS);
    }

    public void setCurrentStatus(String status) {
        if (status.equals(STATUS_HIDE) ||
            status.equals(STATUS_AVAILABLE) ||
            status.equals(STATUS_NOTAVAILABLE) ||
            status.equals(STATUS_ONTHEWATER)) {
            setString(CURRENTSTATUS, status);
        }
    }
    public String getCurrentStatus() {
        return getString(CURRENTSTATUS);
    }

    public void setShowInList(String status) {
        if (status == null ||
            status.equals(STATUS_AVAILABLE) ||
            status.equals(STATUS_NOTAVAILABLE) ||
            status.equals(STATUS_ONTHEWATER)) {
            setString(SHOWINLIST, status);
        }
    }

    public String getShowInList() {
        String s = getString(SHOWINLIST);
        if (s == null || s.length() == 0) {
            s = getCurrentStatus();
        }
        if (s == null || s.length() == 0 || s.equals(STATUS_HIDE)) {
            return null;
        }
        return s;
    }

    public static boolean isOnTheWaterShowNotAvailable(String sessionType, DataTypeDate sessionEndDate) {
        return Daten.efaConfig.getValueEfaDirekt_wafaRegattaBooteAufFahrtNichtVerfuegbar() &&
                (  (sessionEndDate != null && sessionEndDate.isSet()) ||
                   (sessionType != null && (sessionType.equals(EfaTypes.TYPE_SESSION_TOUR) ||
                                            sessionType.equals(EfaTypes.TYPE_SESSION_REGATTA) ||
                                            sessionType.equals(EfaTypes.TYPE_SESSION_JUMREGATTA) ||
                                            sessionType.equals(EfaTypes.TYPE_SESSION_TRAININGCAMP)))
                         );
    }

    public boolean isOnTheWaterShowNotAvailable() {
        LogbookRecord r = getLogbookRecord();
        return r != null && isOnTheWaterShowNotAvailable(r.getSessionType(), r.getEndDate());
    }

    public void setOnlyInBoathouseId(int boathouseId) {
        setString(ONLYINBOATHOUSEID, (boathouseId < 0 ? null : Integer.toString(boathouseId)));
    }
    public int getOnlyInBoathouseIdAsInt() {
        return EfaUtil.string2int(getOnlyInBoathouseId(), IDataAccess.UNDEFINED_INT);
    }
    public String getOnlyInBoathouseId() {
        return getString(ONLYINBOATHOUSEID);
    }

    public void setLogbook(String logbook) {
        setString(LOGBOOK, logbook);
    }
    public String getLogbook() {
        return getString(LOGBOOK);
    }

    public void setEntryNo(DataTypeIntString entryNo) {
        setIntString(ENTRYNO, entryNo);
    }
    public DataTypeIntString getEntryNo() {
        return getIntString(ENTRYNO);
    }

    public LogbookRecord getLogbookRecord() {
        try {
            Logbook l = Daten.project.getLogbook(getLogbook(), false);
            return l.getLogbookRecord(getEntryNo());
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public void setComment(String comment) {
        setString(COMMENT, comment);
    }
    public String getComment() {
        return getString(COMMENT);
    }

    private String getBoatName() {
        Boats boats = getPersistence().getProject().getBoats(false);
        String boatName = "?";
        if (boats != null && getBoatId() != null) {
            BoatRecord r = boats.getBoat(getBoatId(), System.currentTimeMillis());
            if (r != null) {
                boatName = r.getQualifiedName();
            }
        } 
        if (boatName != null && boatName.equals("?") &&
            getBoatText() != null && getBoatText().length() > 0) {
            boatName = getBoatText() + " (" + International.getString("unbekanntes Boot") + ")";
        }
        return boatName;
    }

    public String getQualifiedName() {
        return getBoatName() + ": " + getStatusDescription(this.getCurrentStatus());
    }

    public String[] getQualifiedNameFields() {
        return new String[] { BOATID };
    }

    public String getAsText(String fieldName) {
        if (fieldName.equals(ONLYINBOATHOUSEID)) {
            Object o = get(fieldName);
            if (o == null) {
                return "";
            }
            String s = (String)o;
            if (s.length() == 0) {
                return "";
            }
            return getPersistence().getProject().getBoathouseName(EfaUtil.string2int(s, -1));
        }
        return super.getAsText(fieldName);
    }

    public boolean setFromText(String fieldName, String value) {
        if (fieldName.equals(ONLYINBOATHOUSEID)) {
            if (value == null || value.length() == 0) {
                set(fieldName, null);
            } else {
                int id = getPersistence().getProject().getBoathouseId(value);
                if (id > 0) {
                    set(fieldName, Integer.toString(id));
                } else {
                    set(fieldName, null);
                }
            }
        } else {
            return super.setFromText(fieldName, value);
        }
        return (value.equals(getAsText(fieldName)));
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();

        v.add(item = new ItemTypeLabel("GUI_BOAT_NAME",
                IItemType.TYPE_PUBLIC, CAT_STATUS, International.getMessage("Bootsstatus für {boat}", getBoatNameAsString(System.currentTimeMillis()))));
        item.setPadding(0, 0, 0, 10);

        v.add(item = new ItemTypeString(BoatStatusRecord.BOATTEXT, getBoatText(),
                IItemType.TYPE_EXPERT, CAT_STATUS,
                International.getString("Bootsname")));
        v.add(item = new ItemTypeBoolean(BoatStatusRecord.UNKNOWNBOAT, getUnknownBoat(),
                IItemType.TYPE_EXPERT, CAT_STATUS,
                International.getString("unbekanntes Boot")));
        v.add(item = new ItemTypeStringList(BoatStatusRecord.BASESTATUS, getBaseStatus(),
                makeStatusTypeArray(BASESTATUS, ARRAY_STRINGLIST_VALUES), makeStatusTypeArray(BASESTATUS, ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, CAT_STATUS,
                International.getString("Basis-Status")));
        v.add(item = new ItemTypeStringList(BoatStatusRecord.CURRENTSTATUS, getCurrentStatus(),
                makeStatusTypeArray(CURRENTSTATUS, ARRAY_STRINGLIST_VALUES), makeStatusTypeArray(CURRENTSTATUS, ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_EXPERT, CAT_STATUS,
                International.getString("aktueller Status")));
        v.add(item = new ItemTypeStringList(BoatStatusRecord.SHOWINLIST, getShowInList(),
                makeStatusTypeArray(SHOWINLIST, ARRAY_STRINGLIST_VALUES), makeStatusTypeArray(SHOWINLIST, ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_EXPERT, CAT_STATUS,
                International.getString("anzeigen in Liste")));
        if (getPersistence().getProject().getNumberOfBoathouses() > 1) {
            v.add(item = new ItemTypeStringList(BoatStatusRecord.ONLYINBOATHOUSEID, getOnlyInBoathouseId(),
                    getPersistence().getProject().makeBoathouseArray(EfaTypes.ARRAY_STRINGLIST_VALUES),
                    getPersistence().getProject().makeBoathouseArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                    IItemType.TYPE_PUBLIC, CAT_STATUS,
                    International.getString("nur anzeigen in Bootshaus")));
        }
        if (getCurrentStatus() != null && getCurrentStatus().equals(STATUS_ONTHEWATER)) {
            v.add(item = new ItemTypeLabel(BoatStatusRecord.ENTRYNO,
                    IItemType.TYPE_PUBLIC, CAT_STATUS,
                    International.getMessage("Eintrag in Lfd. Nr. {entryNo} in Fahrtenbuch {logbook}", 
                    (getEntryNo() != null ? getEntryNo().toString() : "NOENTRYNO"), getLogbook())));
        }
        v.add(item = new ItemTypeString(BoatStatusRecord.COMMENT, getComment(),
                IItemType.TYPE_PUBLIC, CAT_STATUS, International.getString("Bemerkung")));
        
        return v;
    }

    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[4];
        header[0] = new TableItemHeader(International.getString("Boot"));
        header[1] = new TableItemHeader(International.getString("Basis-Status"));
        header[2] = new TableItemHeader(International.getString("aktueller Status"));
        header[3] = new TableItemHeader(International.getString("Bemerkung"));
        return header;
    }

    public TableItem[] getGuiTableItems() {
        TableItem[] items = new TableItem[4];
        items[0] = new TableItem(getBoatName());
        items[1] = new TableItem(getStatusDescription(getBaseStatus()));
        items[2] = new TableItem(getStatusDescription(getCurrentStatus()));
        items[3] = new TableItem(getComment());
        return items;
    }

    public static String getStatusDescription(String stype) {
        if (stype == null) {
            return null;
        }
        if (stype.equals(STATUS_HIDE)) {
            return International.getString("nicht anzeigen");
        }
        if (stype.equals(STATUS_AVAILABLE)) {
            return International.getString("verfügbar");
        }
        if (stype.equals(STATUS_NOTAVAILABLE)) {
            return International.getString("nicht verfügbar");
        }
        if (stype.equals(STATUS_ONTHEWATER)) {
            return International.getString("unterwegs");
        }
        return null;
    }

    public static String[] makeStatusTypeArray(String stype, int type) {
        int cnt = -1;
        int offset = 0;
        if (stype.equals(BoatStatusRecord.BASESTATUS)) {
            cnt = 3;
        }
        if (stype.equals(BoatStatusRecord.CURRENTSTATUS)) {
            cnt = 4;
        }
        if (stype.equals(BoatStatusRecord.SHOWINLIST)) {
            cnt = 3;
            offset = 1;
        }
        if (cnt < 0) {
            return null;
        }
        String[] status = new String[cnt];
        for(int i=0; i<status.length; i++) {
            String sname = null;
            switch(i+offset) {
                case 0:
                    sname = STATUS_HIDE;
                    break;
                case 1:
                    sname = STATUS_AVAILABLE;
                    break;
                case 2:
                    sname = STATUS_NOTAVAILABLE;
                    break;
                case 3:
                    sname = STATUS_ONTHEWATER;
                    break;
            }
            status[i] = (type == ARRAY_STRINGLIST_VALUES ?
                sname :
                getStatusDescription(sname));
        }
        return status;
    }

    public static String createStatusString(String fahrttype, String ziel, 
            DataTypeDate date, DataTypeTime time, String person, String enddate) {
        String datum = (date != null ? date.toString() : "");
        String zeit = (time != null ? time.toString() : "");
        String aufFahrtart = "";
        if (Daten.efaTypes != null && fahrttype != null) {
            if (fahrttype.equals(EfaTypes.TYPE_SESSION_REGATTA)) {
                aufFahrtart = " "
                        + International.getMessage("auf {trip_type}", Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_REGATTA));
            }
            if (fahrttype.equals(EfaTypes.TYPE_SESSION_JUMREGATTA)) {
                aufFahrtart = " "
                        + International.getMessage("auf {trip_type}", Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_JUMREGATTA));
            }
            if (fahrttype.equals(EfaTypes.TYPE_SESSION_TRAININGCAMP)) {
                aufFahrtart = " "
                        + International.getMessage("auf {trip_type}", Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_TRAININGCAMP));
            }
            if (fahrttype.startsWith(EfaTypes.TYPE_SESSION_TOUR)) {
                aufFahrtart = " "
                        + International.getMessage("auf {trip_type}", Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_TOUR));
            }
        }
        String nachZiel = "";
        if (aufFahrtart.length() == 0 && ziel.length() > 0) {
            nachZiel = " " + International.getMessage("nach {destination}", ziel);
        }
        return International.getString("unterwegs") + aufFahrtart + nachZiel
                + " " + International.getMessage("seit {date}", datum)
                + (zeit.trim().length() > 0 ? " " + International.getMessage("um {time}", zeit) : "")
                + (enddate != null && enddate.length() > 0 ? " " + International.getMessage("bis {timestamp}", enddate) : "")
                + " " + International.getMessage("mit {crew}", person);
    }


}
