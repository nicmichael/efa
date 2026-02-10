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
import de.nmichael.efa.data.efawett.Zielfahrt;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.util.*;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

// @i18n complete

public class LogbookRecord extends DataRecord {

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String ENTRYID          = "EntryId";
    public static final String DATE             = "Date";
    public static final String ENDDATE          = "EndDate";

    // Boat is either represented by BOATID,BOATVARIANT or by BOATNAME
    public static final String BOATID           = "BoatId";
    public static final String BOATVARIANT      = "BoatVariant";
    public static final String BOATNAME         = "BoatName";

    public static final String ALLCREWNAMES     = "AllCrewNames";
    // each person is either represented by xxxID or xxxNAME
    public static final String COXID            = "CoxId";
    public static final String COXNAME          = "CoxName";
    public static final String CREW1ID          = "Crew1Id";
    public static final String CREW1NAME        = "Crew1Name";
    public static final String CREW2ID          = "Crew2Id";
    public static final String CREW2NAME        = "Crew2Name";
    public static final String CREW3ID          = "Crew3Id";
    public static final String CREW3NAME        = "Crew3Name";
    public static final String CREW4ID          = "Crew4Id";
    public static final String CREW4NAME        = "Crew4Name";
    public static final String CREW5ID          = "Crew5Id";
    public static final String CREW5NAME        = "Crew5Name";
    public static final String CREW6ID          = "Crew6Id";
    public static final String CREW6NAME        = "Crew6Name";
    public static final String CREW7ID          = "Crew7Id";
    public static final String CREW7NAME        = "Crew7Name";
    public static final String CREW8ID          = "Crew8Id";
    public static final String CREW8NAME        = "Crew8Name";
    public static final String CREW9ID          = "Crew9Id";
    public static final String CREW9NAME        = "Crew9Name";
    public static final String CREW10ID         = "Crew10Id";
    public static final String CREW10NAME       = "Crew10Name";
    public static final String CREW11ID         = "Crew11Id";
    public static final String CREW11NAME       = "Crew11Name";
    public static final String CREW12ID         = "Crew12Id";
    public static final String CREW12NAME       = "Crew12Name";
    public static final String CREW13ID         = "Crew13Id";
    public static final String CREW13NAME       = "Crew13Name";
    public static final String CREW14ID         = "Crew14Id";
    public static final String CREW14NAME       = "Crew14Name";
    public static final String CREW15ID         = "Crew15Id";
    public static final String CREW15NAME       = "Crew15Name";
    public static final String CREW16ID         = "Crew16Id";
    public static final String CREW16NAME       = "Crew16Name";
    public static final String CREW17ID         = "Crew17Id";
    public static final String CREW17NAME       = "Crew17Name";
    public static final String CREW18ID         = "Crew18Id";
    public static final String CREW18NAME       = "Crew18Name";
    public static final String CREW19ID         = "Crew19Id";
    public static final String CREW19NAME       = "Crew19Name";
    public static final String CREW20ID         = "Crew20Id";
    public static final String CREW20NAME       = "Crew20Name";
    public static final String CREW21ID         = "Crew21Id";
    public static final String CREW21NAME       = "Crew21Name";
    public static final String CREW22ID         = "Crew22Id";
    public static final String CREW22NAME       = "Crew22Name";
    public static final String CREW23ID         = "Crew23Id";
    public static final String CREW23NAME       = "Crew23Name";
    public static final String CREW24ID         = "Crew24Id";
    public static final String CREW24NAME       = "Crew24Name";

    // BoatCaptain is the Number of the Boats's Captain (0 = Cox, 1 = Crew1, ...)
    public static final String BOATCAPTAIN      = "BoatCaptain";

    public static final String STARTTIME        = "StartTime";
    public static final String ENDTIME          = "EndTime";

    // Destination is either represented as DestinationId or DestinationName
    public static final String DESTINATIONID    = "DestinationId";
    public static final String DESTINATIONNAME  = "DestinationName";
    public static final String DESTINATIONVARIANTNAME = "DestinationVariantName";

    // Additional Waters can be a combination of both UUID-List and String-List
    public static final String WATERSIDLIST     = "WatersIdList";
    public static final String WATERSNAMELIST   = "WatersNameList";

    public static final String DISTANCE         = "Distance";
    public static final String COMMENTS         = "Comments";
    public static final String SESSIONTYPE      = "SessionType";
    public static final String SESSIONGROUPID   = "SessionGroupId";

    public static final String OPEN             = "Open";
    public static final String EFBSYNCTIME      = "EfbSyncTime";

    public static final String EXP_BOAT         = "Boat";
    public static final String EXP_COX          = "Cox";
    public static final String EXP_CREW         = "Crew";
    public static final String EXP_DESTINATION  = "Destination";
    public static final String EXP_WATERSLIST   = "WatersList";
    public static final String EXP_SESSIONGROUP = "SessionGroup";
    public static final String ECRID            = "ecrid";

    // =========================================================================
    // Supplementary Constants
    // =========================================================================

    // General
    public static final int CREW_MAX = 24;
    public static final String WATERS_SEPARATORS = ",;+";

    // =========================================================================
    // Temporary Fields for Evaluation (not stored in the persistent record!)
    // =========================================================================
    public Zielfahrt zielfahrt;

    public static String getCrewFieldNameId(int pos) {
        if (pos == 0) {
            return COXID;
        }
        return "Crew"+pos+"Id";
    }

    public static String getCrewFieldNameName(int pos) {
        if (pos == 0) {
            return COXNAME;
        }
        return "Crew"+pos+"Name";
    }

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(ENTRYID);             t.add(IDataAccess.DATA_INTSTRING);
        f.add(DATE);                t.add(IDataAccess.DATA_DATE);
        f.add(ENDDATE);             t.add(IDataAccess.DATA_DATE);
        f.add(BOATID);              t.add(IDataAccess.DATA_UUID);
        f.add(BOATVARIANT);         t.add(IDataAccess.DATA_INTEGER);
        f.add(BOATNAME);            t.add(IDataAccess.DATA_STRING);
        f.add(ALLCREWNAMES);        t.add(IDataAccess.DATA_VIRTUAL);
        f.add(COXID);               t.add(IDataAccess.DATA_UUID);
        f.add(COXNAME);             t.add(IDataAccess.DATA_STRING);
        f.add(CREW1ID);             t.add(IDataAccess.DATA_UUID);
        f.add(CREW1NAME);           t.add(IDataAccess.DATA_STRING);
        f.add(CREW2ID);             t.add(IDataAccess.DATA_UUID);
        f.add(CREW2NAME);           t.add(IDataAccess.DATA_STRING);
        f.add(CREW3ID);             t.add(IDataAccess.DATA_UUID);
        f.add(CREW3NAME);           t.add(IDataAccess.DATA_STRING);
        f.add(CREW4ID);             t.add(IDataAccess.DATA_UUID);
        f.add(CREW4NAME);           t.add(IDataAccess.DATA_STRING);
        f.add(CREW5ID);             t.add(IDataAccess.DATA_UUID);
        f.add(CREW5NAME);           t.add(IDataAccess.DATA_STRING);
        f.add(CREW6ID);             t.add(IDataAccess.DATA_UUID);
        f.add(CREW6NAME);           t.add(IDataAccess.DATA_STRING);
        f.add(CREW7ID);             t.add(IDataAccess.DATA_UUID);
        f.add(CREW7NAME);           t.add(IDataAccess.DATA_STRING);
        f.add(CREW8ID);             t.add(IDataAccess.DATA_UUID);
        f.add(CREW8NAME);           t.add(IDataAccess.DATA_STRING);
        f.add(CREW9ID);             t.add(IDataAccess.DATA_UUID);
        f.add(CREW9NAME);           t.add(IDataAccess.DATA_STRING);
        f.add(CREW10ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW10NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW11ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW11NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW12ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW12NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW13ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW13NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW14ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW14NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW15ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW15NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW16ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW16NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW17ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW17NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW18ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW18NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW19ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW19NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW20ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW20NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW21ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW21NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW22ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW22NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW23ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW23NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(CREW24ID);            t.add(IDataAccess.DATA_UUID);
        f.add(CREW24NAME);          t.add(IDataAccess.DATA_STRING);
        f.add(BOATCAPTAIN);         t.add(IDataAccess.DATA_INTEGER);
        f.add(STARTTIME);           t.add(IDataAccess.DATA_TIME);
        f.add(ENDTIME);             t.add(IDataAccess.DATA_TIME);
        f.add(DESTINATIONID);       t.add(IDataAccess.DATA_UUID);
        f.add(DESTINATIONNAME);     t.add(IDataAccess.DATA_STRING);
        f.add(DESTINATIONVARIANTNAME); t.add(IDataAccess.DATA_STRING);
        f.add(WATERSIDLIST);        t.add(IDataAccess.DATA_LIST_UUID);
        f.add(WATERSNAMELIST);      t.add(IDataAccess.DATA_LIST_STRING);
        f.add(DISTANCE);            t.add(IDataAccess.DATA_DISTANCE);
        f.add(COMMENTS);            t.add(IDataAccess.DATA_STRING);
        f.add(SESSIONTYPE);         t.add(IDataAccess.DATA_STRING);
        f.add(SESSIONGROUPID);      t.add(IDataAccess.DATA_UUID);
        f.add(EFBSYNCTIME);         t.add(IDataAccess.DATA_LONGINT);
        f.add(OPEN);                t.add(IDataAccess.DATA_BOOLEAN);
        f.add(ECRID);               t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(Logbook.DATATYPE, f, t, false);
        metaData.setKey(new String[] { ENTRYID });
    }

    public LogbookRecord(Logbook logbook, MetaData metaData) {
        super(logbook, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<DataTypeIntString,String,String>(getEntryId(),null,null);
    }

    public static DataKey getKey(DataTypeIntString entryNo) {
        return new DataKey<DataTypeIntString,String,String>(entryNo,null,null);
    }

    public void setEntryId(DataTypeIntString entryId) {
        setIntString(ENTRYID, entryId);
    }
    public DataTypeIntString getEntryId() {
        return getIntString(ENTRYID);
    }

    public void setDate(DataTypeDate date) {
        setDate(DATE, date);
    }
    public DataTypeDate getDate() {
        return getDate(DATE);
    }

    public void setEndDate(DataTypeDate date) {
        setDate(ENDDATE, date);
    }
    public DataTypeDate getEndDate() {
        return getDate(ENDDATE);
    }

    public int getNumberOfDays() {
        int days = 1;
        DataTypeDate startDate = getDate();
        DataTypeDate endDate = getEndDate();
        if (startDate != null && startDate.isSet()
                && endDate != null && endDate.isSet()) {
            days = (int) endDate.getDifferenceDays(startDate) + 1;
        }
        return days;
    }

    public void setBoatId(UUID id) {
        setUUID(BOATID, id);
    }
    public UUID getBoatId() {
        return getUUID(BOATID);
    }

    public void setBoatVariant(int variant) {
        setInt(BOATVARIANT, variant);
    }
    public int getBoatVariant() {
        return getInt(BOATVARIANT);
    }

    public void setBoatName(String name) {
        setString(BOATNAME, name);
    }
    public String getBoatName() {
        return getString(BOATNAME);
    }

    public void setCoxId(UUID id) {
        setUUID(COXID, id);
    }
    public UUID getCoxId() {
        return getUUID(COXID);
    }

    public void setCoxName(String name) {
        setString(COXNAME, name);
    }
    public String getCoxName() {
        return getString(COXNAME);
    }

    public void setCrewId(int pos, UUID id) {
        setUUID(getCrewFieldNameId(pos), id);
    }
    public UUID getCrewId(int pos) {
        return getUUID(getCrewFieldNameId(pos));
    }

    public void setCrewName(int pos, String name) {
        setString(getCrewFieldNameName(pos), name);
    }
    public String getCrewName(int pos) {
        return getString(getCrewFieldNameName(pos));
    }

    public void setBoatCaptainPosition(int pos) {
        setInt(BOATCAPTAIN, pos);
    }
    public int getBoatCaptainPosition() {
        return getInt(BOATCAPTAIN);
    }

    public void setStartTime(DataTypeTime time) {
        setTime(STARTTIME, time);
    }
    public DataTypeTime getStartTime() {
        DataTypeTime time = getTime(STARTTIME);
        if (time != null) {
            time.enableSeconds(false);
        }
        return time;
    }

    public void setEndTime(DataTypeTime time) {
        setTime(ENDTIME, time);
    }
    public DataTypeTime getEndTime() {
        DataTypeTime time = getTime(ENDTIME);
        if (time != null) {
            time.enableSeconds(false);
        }
        return time;
    }

    public void setDestinationId(UUID id) {
        setUUID(DESTINATIONID, id);
    }
    public UUID getDestinationId() {
        return getUUID(DESTINATIONID);
    }

    public void setDestinationName(String name) {
        setString(DESTINATIONNAME, name);
    }
    public String getDestinationName() {
        return getString(DESTINATIONNAME);
    }

    public void setDestinationVariantName(String name) {
        setString(DESTINATIONVARIANTNAME, name);
    }
    public String getDestinationVariantName() {
        return getString(DESTINATIONVARIANTNAME);
    }

    public static String[] getDestinationNameAndVariantFromString(String s) {
        int pos = s.indexOf(DestinationRecord.DESTINATION_VARIANT_SEPARATOR);
        String[] names = new String[2];
        names[0] = (pos < 0 ? s.trim() : s.substring(0, pos).trim());
        names[1] = (pos >= 0 ? s.substring(pos+1).trim() : "");
        return names;
    }

    public void setWatersIdList(DataTypeList<UUID> list) {
        setList(WATERSIDLIST, list);
    }
    public DataTypeList<UUID> getWatersIdList() {
        return getList(WATERSIDLIST, IDataAccess.DATA_UUID);
    }

    public void setWatersNameList(DataTypeList<String> list) {
        setList(WATERSNAMELIST, list);
    }
    public DataTypeList<String> getWatersNameList() {
        return getList(WATERSNAMELIST, IDataAccess.DATA_STRING);
    }

    public String getWatersNamesStringList() {
        StringBuilder s = new StringBuilder();
        try {
            DataTypeList<UUID> wIdList = getWatersIdList();
            DataTypeList<String> wNameList = getWatersNameList();
            if ((wIdList == null || wIdList.length() == 0) &&
                (wNameList == null || wNameList.length() == 0)) {
                return "";
            }
            Waters waters = getPersistence().getProject().getWaters(false);
            for (int i=0; wIdList != null && i<wIdList.length(); i++) {
                WatersRecord w = waters.getWaters(wIdList.get(i));
                if (w != null && w.getName() != null && w.getName().length() > 0) {
                    s.append( (s.length() > 0 ? ", " : "") + w.getName());
                }
            }
            for (int i=0; wNameList != null && i<wNameList.length(); i++) {
                String w = wNameList.get(i);
                if (w != null && w.length() > 0) {
                    s.append( (s.length() > 0 ? ", " : "") + w);
                }
            }
            return s.toString();
        } catch(Exception e) {
            Logger.logdebug(e);
            return "";
        }
    }

    public void setDistance(DataTypeDistance distance) {
        setDistance(DISTANCE, distance);
    }
    public DataTypeDistance getDistance() {
        return getDistance(DISTANCE);
    }

    public void setComments(String comments) {
        setString(COMMENTS, comments);
    }
    public String getComments() {
        return getString(COMMENTS);
    }

    public void setSessionType(String type) {
        setString(SESSIONTYPE, type);
    }
    public String getSessionType() {
        return Daten.efaTypes.getSessionType(getString(SESSIONTYPE));
    }

    public void setSessionGroupId(UUID id) {
        setUUID(SESSIONGROUPID, id);
    }
    public UUID getSessionGroupId() {
        return getUUID(SESSIONGROUPID);
    }
    public SessionGroupRecord getSessionGroup() {
        UUID id = getUUID(SESSIONGROUPID);
        if (id != null) {
            SessionGroups sessionGroups = getPersistence().getProject().getSessionGroups(false);
            if (sessionGroups != null) {
                return sessionGroups.findSessionGroupRecord(id);
            }
        }
        return null;
    }
    public String getSessionGroupAsName() {
        return getAsText(SESSIONGROUPID);
    }

    public void setSyncTime(long syncTime) {
        setLong(EFBSYNCTIME, syncTime);
    }
    public long getSyncTime() {
        return getLong(EFBSYNCTIME);
    }

    public void setSessionIsOpen(boolean open) {
        setBool(OPEN, open);
    }
    public boolean getSessionIsOpen() {
        return getBool(OPEN);
    }
    
    public boolean isRowingOrCanoeingSession() {
        String stype = getSessionType();
        return stype == null || 
               (!stype.equals(EfaTypes.TYPE_SESSION_ERG) && 
                !stype.equals(EfaTypes.TYPE_SESSION_MOTORBOAT));
    }

    protected Object getVirtualColumn(int fieldIdx) {
        if (getFieldName(fieldIdx).equals(ALLCREWNAMES)) {
            return getAllCoxAndCrewAsNameString();
        }
        return null;
    }

    public BoatRecord getBoatRecord(long validAt) {
        try {
            UUID id = getBoatId();
            if (id != null) {
                return getPersistence().getProject().getBoats(false).getBoat(id, validAt);
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    private PersonRecord getPersonRecord(int pos, long validAt) {
        try {
            UUID id = null;
            if (pos == 0) {
                id = getCoxId();
            }
            if (pos >= 1 && pos <= CREW_MAX) {
                id = getCrewId(pos);
            }
            if (id != null) {
                return getPersistence().getProject().getPersons(false).getPerson(id, validAt);
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public PersonRecord getCoxRecord(long validAt) {
        return getPersonRecord(0, validAt);
    }

    public PersonRecord getCrewRecord(int pos, long validAt) {
        return getPersonRecord(pos, validAt);
    }

    public DestinationRecord getDestinationRecord(long validAt) {
        try {
            UUID id = getDestinationId();
            if (id != null) {
                return getPersistence().getProject().getDestinations(false).getDestination(id, validAt);
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public String getBoatAsName() {
        return getBoatAsName(getValidAtTimestamp());
    }

    public String getBoatAsName(long validAt) {
        String name = null;
        BoatRecord b = getBoatRecord(validAt);
        if (b != null) {
            name = b.getQualifiedName();
        }
        if (name == null || name.length() == 0) {
            name = getBoatName();
        }
        if (name != null) {
            return name;
        }
        return "";
    }

    private String getPersonAsName(int pos, long validAt) {
        String name = null;
        if (validAt < 0) {
            validAt = getValidAtTimestamp();
        }
        PersonRecord p = getPersonRecord(pos, validAt);
        if (p != null) {
            name = p.getQualifiedName();
        }
        if (name == null || name.length() == 0) {
            if (pos == 0) {
                name = getCoxName();
            }
            if (pos >= 1 && pos <= CREW_MAX) {
                name = getCrewName(pos);
            }
        }
        if (name != null) {
            return name;
        }
        return "";
    }

    public String getCoxAsName() {
        return getCoxAsName(getValidAtTimestamp());
    }

    public String getCoxAsName(long validAt) {
        return getPersonAsName(0, validAt);
    }

    public String getCrewAsName(int pos) {
        return getCrewAsName(pos, getValidAtTimestamp());
    }

    public String getCrewAsName(int pos, long validAt) {
        return getPersonAsName(pos, validAt);
    }

    public Vector<String> getAllCoxAndCrewAsNames() {
        return getAllCoxAndCrewAsNames(getValidAtTimestamp());
    }

    public Vector<String> getAllCoxAndCrewAsNames(long validAt) {
        Vector<String> v = new Vector<String>();
        String s;
        for (int i=0; i<=CREW_MAX; i++) {
            if ( (s = getPersonAsName(i, validAt)).length() > 0) {
                v.add(s);
            }
        }
        return v;
    }

    public String getAllCoxAndCrewAsNameString() {
        return getAllCoxAndCrewAsNameString(getValidAtTimestamp());
    }

    public String getAllCoxAndCrewAsNameString(long validAt) {
        Vector<String> v = getAllCoxAndCrewAsNames(validAt);
        String sep = (Daten.efaConfig.getValueNameFormatIsFirstNameFirst() ? ", " : "; ");
        StringBuffer s = new StringBuffer();
        for (int i=0; v != null && i < v.size(); i++) {
            s.append( (s.length() > 0 ? sep : "") + v.get(i));
        }
        return s.toString();
    }

    public int getNumberOfCrewMembers() {
        int c = 0;
        long validAt = getValidAtTimestamp();
        for (int i=0; i<=CREW_MAX; i++) {
            if (getPersonAsName(i, validAt).length() > 0) {
                c++;
            }
        }
        return c;
    }

    public String getDestinationAndVariantName() {
        return getDestinationAndVariantName(getValidAtTimestamp());
    }

    public String getDestinationAndVariantName(boolean prefixedByWaters, boolean postfixedByBoathouse) {
        return getDestinationAndVariantName(getValidAtTimestamp(), prefixedByWaters, postfixedByBoathouse);
    }
    
    public String getDestinationAndVariantName(long validAt) {
        return getDestinationAndVariantName(validAt, false, true);
    }

/*    public String getDestinationAndVariantName(long validAt, boolean prefixedByWaters) {
        return getDestinationAndVariantName(validAt, prefixedByWaters, true);
    }
*/
    public String getDestinationAndVariantName(long validAt, boolean prefixedByWaters, boolean postfixedByBoathouse) {
        String name = null;
        if (validAt < 0) {
            validAt = getValidAtTimestamp();
        }
        DestinationRecord d = getDestinationRecord(validAt);
        if (d != null) {
            name = (postfixedByBoathouse ? d.getQualifiedName() : d.getName());
        }
        if (name == null || name.length() == 0) {
            name = getDestinationName();
        }
        String variant = getDestinationVariantName();
        if (variant != null && variant.length() > 0) {
            name = name + " " + DestinationRecord.DESTINATION_VARIANT_SEPARATOR + " " + variant;
        }
        if (name != null) {
            return (prefixedByWaters && d != null ? d.getWatersNamesStringListPrefix() + name : name);
        }
        return "";
    }

    public static long getValidAtTimestamp(DataTypeDate d, DataTypeTime t) {
        if (d != null && d.isSet()) {
            return d.getTimestamp(t);
        }
        return System.currentTimeMillis();
    }

    public long getValidAtTimestamp() {
        return getValidAtTimestamp(getDate(), getStartTime());
    }

    public static int getCrewNoFromFieldName(String field) {
        if (field == null) {
            return -1;
        }
        if (field.equals(COXID) || field.equals(COXNAME)) {
            return 0;
        }
        if (field.equals(CREW1ID) || field.equals(CREW1NAME)) {
            return 1;
        }
        if (field.equals(CREW2ID) || field.equals(CREW2NAME)) {
            return 2;
        }
        if (field.equals(CREW3ID) || field.equals(CREW3NAME)) {
            return 3;
        }
        if (field.equals(CREW4ID) || field.equals(CREW4NAME)) {
            return 4;
        }
        if (field.equals(CREW5ID) || field.equals(CREW5NAME)) {
            return 5;
        }
        if (field.equals(CREW6ID) || field.equals(CREW6NAME)) {
            return 6;
        }
        if (field.equals(CREW7ID) || field.equals(CREW7NAME)) {
            return 7;
        }
        if (field.equals(CREW8ID) || field.equals(CREW8NAME)) {
            return 8;
        }
        if (field.equals(CREW9ID) || field.equals(CREW9NAME)) {
            return 9;
        }
        if (field.equals(CREW10ID) || field.equals(CREW10NAME)) {
            return 10;
        }
        if (field.equals(CREW11ID) || field.equals(CREW11NAME)) {
            return 11;
        }
        if (field.equals(CREW12ID) || field.equals(CREW12NAME)) {
            return 12;
        }
        if (field.equals(CREW13ID) || field.equals(CREW13NAME)) {
            return 13;
        }
        if (field.equals(CREW14ID) || field.equals(CREW14NAME)) {
            return 14;
        }
        if (field.equals(CREW15ID) || field.equals(CREW15NAME)) {
            return 15;
        }
        if (field.equals(CREW16ID) || field.equals(CREW16NAME)) {
            return 16;
        }
        if (field.equals(CREW17ID) || field.equals(CREW17NAME)) {
            return 17;
        }
        if (field.equals(CREW18ID) || field.equals(CREW18NAME)) {
            return 18;
        }
        if (field.equals(CREW19ID) || field.equals(CREW19NAME)) {
            return 19;
        }
        if (field.equals(CREW20ID) || field.equals(CREW20NAME)) {
            return 20;
        }
        if (field.equals(CREW21ID) || field.equals(CREW21NAME)) {
            return 21;
        }
        if (field.equals(CREW22ID) || field.equals(CREW22NAME)) {
            return 22;
        }
        if (field.equals(CREW23ID) || field.equals(CREW23NAME)) {
            return 23;
        }
        if (field.equals(CREW24ID) || field.equals(CREW24NAME)) {
            return 24;
        }
        return -1;
    }

    public String[] getEquivalentFields(String fieldName) {
        if (fieldName.equals(EXP_BOAT) || fieldName.equals(BOATID) || fieldName.equals(BOATNAME)) {
            return new String[] { BOATID, BOATNAME };
        }
        if (fieldName.equals(EXP_COX) || fieldName.equals(COXID) || fieldName.equals(COXNAME)) {
            return new String[] { COXID, COXNAME };
        }
        for (int i = 0; i <= CREW_MAX; i++) {
            if (fieldName.equals(EXP_CREW+i) || fieldName.equals(getCrewFieldNameId(i)) || fieldName.equals(getCrewFieldNameName(i))) {
                return new String[]{ getCrewFieldNameId(i), getCrewFieldNameName(i) };
            }
        }
        if (fieldName.equals(EXP_DESTINATION) || fieldName.equals(DESTINATIONID) || fieldName.equals(DESTINATIONNAME)) {
            return new String[] { DESTINATIONID, DESTINATIONNAME };
        }
        if (fieldName.equals(EXP_WATERSLIST) || fieldName.equals(WATERSIDLIST) || fieldName.equals(WATERSNAMELIST)) {
            return new String[] { WATERSIDLIST, WATERSNAMELIST };
        }
        if (fieldName.equals(EXP_SESSIONGROUP) || fieldName.equals(SESSIONGROUPID)) {
            return new String[] { SESSIONGROUPID };
        }
        return super.getEquivalentFields(fieldName);
    }

    public String[] getFieldNamesForTextExport(boolean includingVirtual) {
        String[] allFields = getPersistence().data().getFieldNames(includingVirtual);
        ArrayList<String> expFields = new ArrayList<String>();
        for (String f : allFields) {
            boolean found = false;
            if (f.equals(BOATID)) {
                expFields.add(EXP_BOAT);
                continue;
            }
            for (int i=0; i<=CREW_MAX; i++) {
                if (f.equals(this.getCrewFieldNameId(i))) {
                    expFields.add((i == 0 ? EXP_COX : EXP_CREW + i));
                    found = true;
                    break;
                }
            }
            if (f.equals(DESTINATIONID)) {
                expFields.add(EXP_DESTINATION);
                continue;
            }
            if (f.equals(WATERSIDLIST)) {
                expFields.add(EXP_WATERSLIST);
                continue;
            }
            if (f.equals(SESSIONGROUPID)) {
                expFields.add(EXP_SESSIONGROUP);
                continue;
            }
            if (f.equals(BOATNAME) ||
                f.equals(DESTINATIONNAME) ||
                f.equals(WATERSNAMELIST)) {
                continue;
            }
            for (int i=0; i<=CREW_MAX; i++) {
                if (f.equals(this.getCrewFieldNameName(i))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                expFields.add(f);
            }
        }
        return expFields.toArray(new String[0]);
    }

    public String getAsText(String fieldName) {
        if (fieldName.equals(BOATID) || fieldName.equals(EXP_BOAT)) {
            return getBoatAsName();
        }
        for (int i=0; i<=CREW_MAX; i++) {
            if (fieldName.equals(getCrewFieldNameId(i))
                    || fieldName.equals(EXP_COX) || fieldName.equals(EXP_CREW+i)) {
                return getCrewAsName(i);
            }
        }
        if (fieldName.equals(DESTINATIONID) || fieldName.equals(EXP_DESTINATION)) {
            return getDestinationAndVariantName();
        }
        if (fieldName.equals(WATERSIDLIST) || fieldName.equals(EXP_WATERSLIST)) {
            return getWatersNamesStringList();
        }
        if (fieldName.equals(SESSIONGROUPID) || fieldName.equals(EXP_SESSIONGROUP)) {
            return (getSessionGroup() != null ? getSessionGroup().getName() : "");
        }
        if (fieldName.equals(SESSIONTYPE)) {
            String s = getAsString(fieldName);
            if (s != null) {
                return Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, s);
            }
            return null;
        }
        return super.getAsText(fieldName);
    }

    public boolean setFromText(String fieldName, String value) {
        if (fieldName.equals(BOATID) || fieldName.equals(EXP_BOAT)) {
            fieldName = BOATID;
            Boats boats = getPersistence().getProject().getBoats(false);
            BoatRecord br = boats.getBoat(value, getValidAtTimestamp());
            if (br != null) {
                set(fieldName, br.getId());
            } else {
                fieldName = BOATNAME;
                set(fieldName, value);
            }
            return (value.equals(getAsText(fieldName)));
        }
        for (int i=0; i<=CREW_MAX; i++) {
            if (fieldName.equals(getCrewFieldNameId(i))
                || fieldName.equals(EXP_COX) || fieldName.equals(EXP_CREW+i)) {
                fieldName = getCrewFieldNameId(i);
                Persons persons = getPersistence().getProject().getPersons(false);
                PersonRecord pr = persons.getPerson(value, getValidAtTimestamp());
                if (pr != null) {
                    set(fieldName, pr.getId());
                } else {
                    fieldName = getCrewFieldNameName(i);
                    set(fieldName, value);
                }
                return (value.equals(getAsText(fieldName)));
            }
        }
        if (fieldName.equals(DESTINATIONID) || fieldName.equals(EXP_DESTINATION)) {
            fieldName = DESTINATIONID;
            Destinations destinations = getPersistence().getProject().getDestinations(false);
            DestinationRecord dr = destinations.getDestination(value, getValidAtTimestamp());
            if (dr != null) {
                set(fieldName, dr.getId());
            } else {
                fieldName = DESTINATIONNAME;
                set(fieldName, value);
            }
            return (value.equals(getAsText(fieldName)));
        }
        if (fieldName.equals(WATERSIDLIST) || fieldName.equals(EXP_WATERSLIST)) {
            fieldName = WATERSIDLIST;
            Vector<String> values = EfaUtil.split(value, ',');
            DataTypeList<UUID> list = new DataTypeList<UUID>();
            Waters waters = getPersistence().getProject().getWaters(false);
            for (int i=0; i<values.size(); i++) {
                WatersRecord wr = waters.findWatersByName(values.get(i).trim());
                if (wr != null) {
                    list.add(wr.getId());
                }
            }
            if (list.length() > 0) {
                set(fieldName, list);
            } else {
                DataTypeList l = DataTypeList.parseList(value, IDataAccess.DATA_STRING);
                if (l != null && l.length() > 0) {
                    fieldName = WATERSNAMELIST;
                    set(fieldName, value);
                }
            }
            return (value.equals(getAsText(fieldName)));
        }
        if (fieldName.equals(SESSIONGROUPID) || fieldName.equals(EXP_SESSIONGROUP)) {
            fieldName = SESSIONGROUPID;
            SessionGroups sessiongroups = getPersistence().getProject().getSessionGroups(false);
            SessionGroupRecord sr = sessiongroups.findSessionGroupRecord(value,
                    ((Logbook)getPersistence()).getName());
            if (sr != null) {
                set(fieldName, sr.getId());
            }
            return (value.equals(getAsText(fieldName)));
        }
        if (fieldName.equals(SESSIONTYPE)) {
            String s = Daten.efaTypes.getTypeForValue(EfaTypes.CATEGORY_SESSION, value);
            if (s != null) {
                set(fieldName, s);
            }
            return (value.equals(getAsText(fieldName)));
        }
        return super.setFromText(fieldName, value);
    }

    public void setLogbookField(String fieldName, Object data) {
        super.set(fieldName, data);
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        /**
         * CAUTION:
         * The implementation of this method is *NOT* complete. It is *NOT* intended to display
         * the current record properly for editing. All it does is return dummy GUI fields which
         * are used, for example, by BatchEditDialog, to figure out data content and labels for
         * the various fields.
         */
        String CAT_BASEDATA     = "%01%" + International.getString("Fahrt");

        AutoCompleteList autoBoats = new AutoCompleteList();
        autoBoats.setDataAccess(getPersistence().getProject().getBoats(false).data(), 0, Long.MAX_VALUE);
        AutoCompleteList autoPersons = new AutoCompleteList();
        autoPersons.setDataAccess(getPersistence().getProject().getPersons(false).data(), 0, Long.MAX_VALUE);
        AutoCompleteList autoDestinations = new AutoCompleteList();
        autoDestinations.setDataAccess(getPersistence().getProject().getDestinations(false).data(), 0, Long.MAX_VALUE);
        AutoCompleteList autoWaters = new AutoCompleteList();
        autoWaters.setDataAccess(getPersistence().getProject().getWaters(false).data(), 0, Long.MAX_VALUE);

        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();

        v.add(item = new ItemTypeString(LogbookRecord.ENTRYID, "", IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Lfd. Nr.")));
        v.add(item = new ItemTypeLabel(LogbookRecord.OPEN, IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Fahrt offen (Boot unterwegs)")));
        v.add(item = new ItemTypeDate(LogbookRecord.DATE, new DataTypeDate(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Datum")));
        v.add(item = new ItemTypeDate(LogbookRecord.ENDDATE, new DataTypeDate(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Enddatum")));
        v.add(item = new ItemTypeStringAutoComplete(LogbookRecord.EXP_BOAT, "", IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Boot"), true));
        ((ItemTypeStringAutoComplete)item).setAutoCompleteData(autoBoats);
        v.add(item = new ItemTypeStringList(LogbookRecord.BOATVARIANT, "",
                null, null,
                IItemType.TYPE_PUBLIC, null, International.getString("Bootsvariante")));
        v.add(item = new ItemTypeStringAutoComplete(LogbookRecord.ALLCREWNAMES, "", IItemType.TYPE_INTERNAL, null,
                International.getString("Mannschaft"), true));
        ((ItemTypeStringAutoComplete) item).setAutoCompleteData(autoPersons);
        v.add(item = new ItemTypeStringAutoComplete(LogbookRecord.EXP_COX, "", IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Steuermann"), true));
        ((ItemTypeStringAutoComplete)item).setAutoCompleteData(autoPersons);
        for (int i=1; i<=CREW_MAX; i++) {
            v.add(item = new ItemTypeStringAutoComplete(LogbookRecord.EXP_CREW+i, "", IItemType.TYPE_PUBLIC, null,
                    International.getString("Mannschaft") + " " + Integer.toString(i), true));
            ((ItemTypeStringAutoComplete)item).setAutoCompleteData(autoPersons);
        }
        v.add(item = new ItemTypeStringList(LogbookRecord.BOATCAPTAIN, "",
                LogbookRecord.getBoatCaptainValues(), LogbookRecord.getBoatCaptainDisplay(),
                IItemType.TYPE_PUBLIC, null, International.getString("Obmann")));
        v.add(item = new ItemTypeTime(LogbookRecord.STARTTIME, new DataTypeTime(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Abfahrt")));
        v.add(item = new ItemTypeTime(LogbookRecord.ENDTIME, new DataTypeTime(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Ankunft")));
        v.add(item = new ItemTypeStringAutoComplete(LogbookRecord.EXP_DESTINATION, "", IItemType.TYPE_PUBLIC, null,
                International.getStringWithMnemonic("Ziel") + " / " +
                International.getStringWithMnemonic("Strecke"), true));
        ((ItemTypeStringAutoComplete)item).setAutoCompleteData(autoDestinations);
        v.add(item = new ItemTypeStringAutoComplete(LogbookRecord.EXP_WATERSLIST, "", IItemType.TYPE_PUBLIC, null,
                International.getStringWithMnemonic("Gewässer"), true));
        ((ItemTypeStringAutoComplete)item).setAutoCompleteData(autoWaters);
        v.add(item = new ItemTypeDistance(LogbookRecord.DISTANCE, null, IItemType.TYPE_PUBLIC, null,
                DataTypeDistance.getDefaultUnitName()));
        v.add(item = new ItemTypeString(LogbookRecord.COMMENTS, null, IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Bemerkungen")));
        v.add(item = new ItemTypeStringList(LogbookRecord.SESSIONTYPE, EfaTypes.TYPE_SESSION_NORMAL,
                EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, null, International.getString("Fahrtart")));
        v.add(item = new ItemTypeStringAutoComplete(LogbookRecord.EXP_SESSIONGROUP,
                "", IItemType.TYPE_PUBLIC, null,
                International.getStringWithMnemonic("Fahrtgruppe"), true));
        return v;
    }

    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[6];
        header[0] = new TableItemHeader(International.getString("Lfd. Nr."));
        header[1] = new TableItemHeader(International.getString("Datum"));
        header[2] = new TableItemHeader(International.getString("Boot"));
        header[3] = new TableItemHeader(International.getString("Mannschaft"));
        header[4] = new TableItemHeader(International.getString("Ziel") + " / " +
                                        International.getString("Strecke"));
        header[5] = new TableItemHeader(DataTypeDistance.getDefaultUnitName());
        return header;
    }

    public TableItem[] getGuiTableItems() {
        TableItem[] items = new TableItem[6];
        items[0] = new TableItem(getAsText(ENTRYID));
        items[1] = new TableItem(getAsText(DATE));
        items[2] = new TableItem(getBoatAsName());
        items[3] = new TableItem(getAllCoxAndCrewAsNameString());
        items[4] = new TableItem(getDestinationAndVariantName());
        items[5] = new TableItem(getAsText(DISTANCE));
        return items;
    }

    public String getQualifiedName() {
        String name = (getEntryId() != null ? getEntryId().toString() : "?") +
                      " (" + (getDate() != null ? getDate().toString() : "?") + ")";
        return name;
    }

    public String[] getQualifiedNameFields() {
        return new String[] { ENTRYID };
    }

    public String getLogbookRecordAsStringDescription() {
        try {
            StringBuffer s = new StringBuffer();
            s.append(International.getMessage("#{entry} vom {date} mit {boat}",
                    (getEntryId() != null ? getEntryId().toString() :
                        International.getString("Fahrtenbucheintrag")),
                    (getDate() != null ? getDate().toString() : "?"),
                    getBoatAsName()) + ": ");
            s.append(getAllCoxAndCrewAsNameString() + ": ");
            s.append( (getStartTime() != null ? getStartTime().toString() : "?") +
                    " " + International.getString("bis") + " " +
                     (getEndTime() != null ? getEndTime().toString() : "?") + " " +
                     International.getString("Uhr") + ": ");
            s.append(getDestinationAndVariantName() + " (" +
                    (getDistance() != null ? getDistance().toString() : "?") + ")");
            if (getComments() != null && getComments().length() > 0) {
                s.append("; " + getComments());
            }
            return s.toString();
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public static String[] getBoatCaptainValues() {
        String[] _bcValues = new String[LogbookRecord.CREW_MAX + 2];
        _bcValues[0] = "";
        for (int i=0; i<=LogbookRecord.CREW_MAX; i++) {
            _bcValues[i+1] = Integer.toString(i);
        }
        return _bcValues;
    }

    public static String[] getBoatCaptainDisplay() {
        String[] _bcNames = new String[LogbookRecord.CREW_MAX + 2];
        _bcNames[0] = International.getString("keine Angabe");
        for (int i=0; i<=LogbookRecord.CREW_MAX; i++) {
            _bcNames[i+1] = (i == 0 ? International.getString("Steuermann") :
                International.getString("Nummer") + " " + Integer.toString(i));
        }
        return _bcNames;
    }

    public long getEntryElapsedTimeInMinutes() {
        DataTypeTime timeFrom = getStartTime();
        DataTypeTime timeTo = getEndTime();
        if (timeFrom == null || !timeFrom.isSet()
                || timeTo == null || !timeTo.isSet()) {
            return 0;
        }
        DataTypeDate dateFrom = getDate();
        DataTypeDate dateTo   = getEndDate();
        boolean multiDay =  (dateFrom != null && dateFrom.isSet() &&
            dateTo != null && dateTo.isSet() &&
            dateTo.isAfter(dateFrom));

        if (!multiDay) {
            return (timeTo.getTimeAsSeconds() - timeFrom.getTimeAsSeconds()) / 60;
        } else {
            return (dateTo.getTimestamp(timeTo) - dateFrom.getTimestamp(timeFrom)) / 60000;
        }
    }

    public boolean saveRecordToXmlFile(String filename) {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), Daten.ENCODING_UTF));
            out.write(XmlHandler.XML_HEADER + "\n");
            out.write("<" + DataExport.FIELD_EXPORT + " " +
                    DataExport.EXPORT_TYPE + "=\"" + DataExport.EXPORT_TYPE_ID + "\">\n");
            out.write(encodeAsString() + "\n");
            out.write("</" + DataExport.FIELD_EXPORT + ">\n");
            out.close();
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }
        return true;
    }

}
