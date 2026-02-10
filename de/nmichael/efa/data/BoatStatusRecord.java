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

import java.awt.AWTEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.UUID;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeButton;
import de.nmichael.efa.core.items.ItemTypeLabelHeader;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

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
    public static final String ECRID               = "ecrid";

    public static final int COLUMN_ID_BOAT_NAME = 0;
    public static final int COLUMN_ID_BOAT_BASE_STATUS = 1;
    public static final int COLUMN_ID_BOAT_CURRENT_STATUS = 2;
    public static final int COLUMN_ID_BOAT_SESSION_LOGBOOK = 3;
    public static final int COLUMN_ID_BOAT_COMMENT = 4;
    
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
        f.add(ECRID);                    t.add(IDataAccess.DATA_STRING);
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

    /**
     * Return the qualified name of the boat identified by the boatstatus record's boat id, valid at the given timestamp.
     * @param validAt timestamp (System.currentTimeMillis)
     * @return Qualified name of the boat, null if no boatrecord can be found for the BoatID.
     */
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

    /**
     * Determines the BoatStautsRecord Destination, depending  on the current logbook.
     * @return Destination and DestinationVariant name
     */
    public String getDestination() {
   	
    	if (getLogbook().equalsIgnoreCase(Daten.project.getCurrentLogbook().getName())) {
    		LogbookRecord r = getLogbookRecord(); 	
	    	if (r==null) {
	        	return International.getString("Fehler: kein Fahrtenbucheintrag zu Boot auf Fahrt");
	        } else {
	        	return r.getDestinationAndVariantName();
	        }
    	} else {
    		// boatstatus not in the current logbook, so only display sessionno and logbookname.
    		// this is safe, as this data is in the boatstatus, and we do not load a "foreign" logbook on the fly
    		return "#"+getEntryNoAndLogbook();
    	}
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
            
            if (l==null) {
            	return null;
            } else {
            	return l.getLogbookRecord(getEntryNo());
            }
            	
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
    
    /**
     * Get the name of the boat owner of the current boatstatusrecord's boat.
     * @return Boat owner's name, or empty string if boat cannot be found for this BoatStatusRecord.
     */
    public String getBoatOwner() {
    	  Boats boats = getPersistence().getProject().getBoats(false);
          String boatOwner = "";
          if (boats != null && getBoatId() != null) {
              BoatRecord r = boats.getBoat(getBoatId(), System.currentTimeMillis());
              if (r != null) {
                  boatOwner = r.getOwner();
              }
          } 
          return boatOwner;    	
    }

    /**
     * Get the name of the boat of the current boatstatusrecord.
     * @return Boat's qualified name, or user-entered name of a foreign boat, with suffix "(unknown boat)".
     */
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

        v.add(item = new ItemTypeLabelHeader("GUI_BOAT_NAME",
                IItemType.TYPE_PUBLIC, CAT_STATUS, " "+International.getMessage("Bootsstatus für {boat}", getBoatNameAsString(System.currentTimeMillis()))));
        item.setPadding(0, 0, 0, 10);

        item.setFieldGrid(2,GridBagConstraints.EAST, GridBagConstraints.BOTH);
        
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
        if ((getCurrentStatus() != null && getCurrentStatus().equals(STATUS_ONTHEWATER))
        		|| (getEntryNo()!=null || (getLogbook() != null && getLogbook().trim().length()>0))){
            v.add(item = EfaGuiUtils.createHintWordWrap(BoatStatusRecord.ENTRYNO, IItemType.TYPE_PUBLIC, CAT_STATUS,
                    International.getMessage("Eintrag in Lfd. Nr. {entryNo} in Fahrtenbuch {logbook}", 
                            (getEntryNo() != null ? getEntryNo().toString() : "("+International.getString("leer")+")"), getLogbook()),3,5,5,600));
            item.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);

        }
        v.add(item = new ItemTypeString(BoatStatusRecord.COMMENT, getComment(),
                IItemType.TYPE_PUBLIC, CAT_STATUS, International.getString("Bemerkung")));
        
        return v;
    }
    
    public TableItemHeader[] getGuiTableHeader() {
    	boolean multipleBoathouses = false;
        try {
            multipleBoathouses = (getPersistence().getProject().getNumberOfBoathouses() > 1);
        } catch(Exception eingore) {
            EfaUtil.foo();
        }

        int cols = 6;
        if (multipleBoathouses) {cols++;}

        TableItemHeader[] header = new TableItemHeader[cols];
        int col=0;
        header[col++] = new TableItemHeader(International.getString("Boot"));
        if (multipleBoathouses) {
            header[col++] = new TableItemHeader(International.getString("Bootshaus"));
        }
        header[col++] = new TableItemHeader(International.getString("Basis-Status"));
        header[col++] = new TableItemHeader(International.getString("aktueller Status"));
        header[col++] = new TableItemHeader(International.getString("Fahrt")+"/"+International.getString("Fahrtenbuch"));
        header[col++] = new TableItemHeader(International.getString("Bemerkung"));
        header[col++] = new TableItemHeader(International.getString("Eigentümer"));
        return header;
    }

    public TableItem[] getGuiTableItems() {
        boolean multipleBoathouses = false;
        try {
            multipleBoathouses = (getPersistence().getProject().getNumberOfBoathouses() > 1);
        } catch(Exception eingore) {
            EfaUtil.foo();
        }
        int cols = 6;
        if (multipleBoathouses) {
            cols++;
        }
        int col=0;
        TableItem[] items = new TableItem[cols];
        items[col++] = new TableItem(getBoatName());
        if (multipleBoathouses) {
            items[col++] = new TableItem(getOnlyInBoathouseName());
        }        
        items[col++] = new TableItem(getStatusDescription(getBaseStatus()));
        items[col++] = new TableItem(getStatusDescription(getCurrentStatus()));
        items[col++] = new TableItem(getEntryNoAndLogbook());
        items[col++] = new TableItem(getComment());
        items[col++] = new TableItem(getBoatOwner());
        return items;
    }

    
    private String getEntryNoAndLogbook() {
        String retVal="";
        if (getEntryNo() != null) { 
        	retVal+=getEntryNo().toString().trim();
        } 
        if (getLogbook() != null) {
        	if (retVal.length()>0) {
        		retVal+= " / ";
        	}
        	retVal+=getLogbook();
        }
        
        return retVal;    	
    }
    
    /**
     * @return Empty if boat is not to be shown exclusively in a single boathouse, else it retruns the respective name of the boathouse.
     */
    public String getOnlyInBoathouseName() {
        int id = getOnlyInBoathouseIdAsInt();
        if (id <= 0) {
            return null;
        }
        try {
            return getPersistence().getProject().getBoathouseName(id);
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return International.getString("Bootshaus") + " " + id;
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
