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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemListenerDataRecordTable;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeDecimal;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.core.items.ItemTypeItemList;
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
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.gui.dataedit.BoatDamageEditDialog;
import de.nmichael.efa.gui.dataedit.BoatReservationEditDialog;
import de.nmichael.efa.gui.dataedit.DataEditDialog;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete

public class BoatRecord extends DataRecord implements IItemFactory, IItemListenerDataRecordTable {

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String ID                    = "Id";
    public static final String EFBID                 = "EfbId";
    public static final String NAME                  = "Name";
    public static final String NAMEAFFIX             = "NameAffix";
    public static final String OWNER                 = "Owner";
    public static final String LASTVARIANT           = "LastVariant";
    public static final String DEFAULTVARIANT        = "DefaultVariant";
    public static final String TYPEVARIANT           = "TypeVariant";
    public static final String TYPEDESCRIPTION       = "TypeDescription";
    public static final String TYPETYPE              = "TypeType";
    public static final String TYPESEATS             = "TypeSeats";
    public static final String TYPERIGGING           = "TypeRigging";
    public static final String TYPECOXING            = "TypeCoxing";
    // RESERVATIONS stored in BoatReservations
    // DAMAGES stored in BoatDamages
    public static final String ALLOWEDGROUPIDLIST    = "AllowedGroupIdList";
    public static final String MAXNOTINGROUP         = "MaxNotInGroup";
    public static final String REQUIREDGROUPID       = "RequiredGroupId";
    public static final String ONLYWITHBOATCAPTAIN   = "OnlyWithBoatCaptain";
    public static final String MANUFACTURER          = "Manufacturer";
    public static final String MODEL                 = "Model";
    public static final String MAXCREWWEIGHT         = "MaxCrewWeight";
    public static final String MANUFACTIONDATE       = "ManufactionDate";
    public static final String SERIALNO              = "SerialNo";
    public static final String PURCHASEDATE          = "PurchaseDate";
    public static final String PURCHASEPRICE         = "PurchasePrice";
    public static final String SELLINGDATE           = "SellingDate";
    public static final String SELLINGPRICE          = "SellingPrice";
    public static final String INSURANCEVALUE        = "InsuranceValue";
    public static final String CURRENCY              = "PurchasePriceCurrency";
    public static final String DEFAULTCREWID         = "DefaultCrewId";
    public static final String DEFAULTSESSIONTYPE    = "DefaultSessionType";
    public static final String DEFAULTDESTINATIONID  = "DefaultDestinationId";
    public static final String EXCLUDEFROMSTATISTIC= "ExcludeFromStatistics";
    public static final String FREEUSE1              = "FreeUse1";
    public static final String FREEUSE2              = "FreeUse2";
    public static final String FREEUSE3              = "FreeUse3";
    public static final String ECRID                 = "ecrid";

    public static final String[] IDX_NAME_NAMEAFFIX = new String[] { NAME, NAMEAFFIX };

    private static String GUIITEM_BOATTYPES          = "GUIITEM_BOATTYPES";
    private static String GUIITEM_ALLOWEDGROUPIDLIST = "GUIITEM_ALLOWEDGROUPIDLIST";
    private static String GUIITEM_RESERVATIONS       = "GUIITEM_RESERVATIONS";
    private static String GUIITEM_DAMAGES            = "GUIITEM_DAMAGES";
    private static String GUIITEM_DEFAULTBOATTYPE    = "GUIITEM_DEFAULTBOATTYPE";
    private ButtonGroup buttonGroup = new ButtonGroup();

    public static final int COLUMN_ID_BOAT_NAME = 0;
    public static final int COLUMN_ID_BOAT_TYPE = 1;
    public static final int COLUMN_ID_BOAT_OWNER = 2;
    
    private static Pattern qnamePattern = Pattern.compile("(.+) \\(([^\\(\\)]+)\\)");

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(ID);                                t.add(IDataAccess.DATA_UUID);
        f.add(NAME);                              t.add(IDataAccess.DATA_STRING);
        f.add(NAMEAFFIX);                         t.add(IDataAccess.DATA_STRING);
        f.add(OWNER);                             t.add(IDataAccess.DATA_STRING);
        f.add(LASTVARIANT);                       t.add(IDataAccess.DATA_INTEGER);
        f.add(DEFAULTVARIANT);                    t.add(IDataAccess.DATA_INTEGER);
        f.add(TYPEVARIANT);                       t.add(IDataAccess.DATA_LIST_INTEGER);
        f.add(TYPEDESCRIPTION);                   t.add(IDataAccess.DATA_LIST_STRING);
        f.add(TYPETYPE);                          t.add(IDataAccess.DATA_LIST_STRING);
        f.add(TYPESEATS);                         t.add(IDataAccess.DATA_LIST_STRING);
        f.add(TYPERIGGING);                       t.add(IDataAccess.DATA_LIST_STRING);
        f.add(TYPECOXING);                        t.add(IDataAccess.DATA_LIST_STRING);
        f.add(ALLOWEDGROUPIDLIST);                t.add(IDataAccess.DATA_LIST_UUID);
        f.add(MAXNOTINGROUP);                     t.add(IDataAccess.DATA_INTEGER);
        f.add(REQUIREDGROUPID);                   t.add(IDataAccess.DATA_UUID);
        f.add(ONLYWITHBOATCAPTAIN);               t.add(IDataAccess.DATA_BOOLEAN);
        f.add(MANUFACTURER);                      t.add(IDataAccess.DATA_STRING);
        f.add(MODEL);                             t.add(IDataAccess.DATA_STRING);
        f.add(MAXCREWWEIGHT);                     t.add(IDataAccess.DATA_INTEGER);
        f.add(MANUFACTIONDATE);                   t.add(IDataAccess.DATA_DATE);
        f.add(SERIALNO);                          t.add(IDataAccess.DATA_STRING);
        f.add(PURCHASEDATE);                      t.add(IDataAccess.DATA_DATE);
        f.add(PURCHASEPRICE);                     t.add(IDataAccess.DATA_DECIMAL);
        f.add(SELLINGDATE);                       t.add(IDataAccess.DATA_DATE);
        f.add(SELLINGPRICE);                      t.add(IDataAccess.DATA_DECIMAL);
        f.add(CURRENCY);                          t.add(IDataAccess.DATA_STRING);
        f.add(INSURANCEVALUE);                    t.add(IDataAccess.DATA_DECIMAL);
        f.add(DEFAULTCREWID);                     t.add(IDataAccess.DATA_UUID);
        f.add(DEFAULTSESSIONTYPE);                t.add(IDataAccess.DATA_STRING);
        f.add(DEFAULTDESTINATIONID);              t.add(IDataAccess.DATA_UUID);
        f.add(EXCLUDEFROMSTATISTIC);              t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FREEUSE1);                          t.add(IDataAccess.DATA_STRING);
        f.add(FREEUSE2);                          t.add(IDataAccess.DATA_STRING);
        f.add(FREEUSE3);                          t.add(IDataAccess.DATA_STRING);
        f.add(EFBID);                             t.add(IDataAccess.DATA_STRING);
        f.add(ECRID);                             t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(Boats.DATATYPE, f, t, true);
        metaData.setKey(new String[] { ID }); // plus VALID_FROM
        metaData.addIndex(IDX_NAME_NAMEAFFIX);
    }

    public BoatRecord(Boats boats, MetaData metaData) {
        super(boats, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<UUID,Long,String>(getId(),getValidFrom(),null);
    }

    public static DataKey getKey(UUID id, long validFrom) {
        return new DataKey<UUID,Long,String>(id,validFrom,null);
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

    public void setNameAffix(String affix) {
        setString(NAMEAFFIX, affix);
    }
    public String getNameAffix() {
        return getString(NAMEAFFIX);
    }

    public void setOwner(String owner) {
        setString(OWNER, owner);
    }
    public String getOwner() {
        return getString(OWNER);
    }
    public String getOwnerOwnOrOther() {
        String s = getString(OWNER);
        if (s == null || s.length() == 0) {
            return StatisticsRecord.BOWNER_OWN;
        } else {
            return StatisticsRecord.BOWNER_OTHER;
        }
    }

    public int getNumberOfVariants() {
        DataTypeList l = getList(TYPEVARIANT, IDataAccess.DATA_INTEGER);
        return (l != null ? l.length() : 0);
    }
    
    public int getVariantIndex(int variant) {
        DataTypeList l = getList(TYPEVARIANT, IDataAccess.DATA_INTEGER);
        for (int i=0; l != null && i<l.length(); i++) {
            Integer v = (Integer)l.get(i);
            if (v.intValue() == variant) {
                return i;
            }
        }
        return -1;
    }

    public int getTypeVariant(int idx) {
        DataTypeList l = getList(TYPEVARIANT, IDataAccess.DATA_INTEGER);
        if (l == null || idx < 0 || idx >= l.length()) {
            return -1;
        } else {
            return ((Integer)l.get(idx)).intValue();
        }
    }

    public String getTypeDescription(int idx) {
        DataTypeList l = getList(TYPEDESCRIPTION, IDataAccess.DATA_STRING);
        if (l == null || idx < 0 || idx >= l.length()) {
            return null;
        } else {
            return ((String)l.get(idx));
        }
    }

    public String getTypeType(int idx) {
        DataTypeList l = getList(TYPETYPE, IDataAccess.DATA_STRING);
        if (l == null || idx < 0 || idx >= l.length()) {
            return null;
        } else {
            return (String)l.get(idx);
        }
    }

    public String getTypeSeats(int idx) {
        DataTypeList l = getList(TYPESEATS, IDataAccess.DATA_STRING);
        if (l == null || idx < 0 || idx >= l.length()) {
            return null;
        } else {
            return (String)l.get(idx);
        }
    }

    public String getTypeRigging(int idx) {
        DataTypeList l = getList(TYPERIGGING, IDataAccess.DATA_STRING);
        if (l == null || idx < 0 || idx >= l.length()) {
            return null;
        } else {
            return (String)l.get(idx);
        }
    }

    public String getTypeCoxing(int idx) {
        DataTypeList l = getList(TYPECOXING, IDataAccess.DATA_STRING);
        if (l == null || idx < 0 || idx >= l.length()) {
            return null;
        } else {
            return (String)l.get(idx);
        }
    }

    public int addTypeVariant(String description, String type, String seats, String rigging, String coxing, String isDefault) {
        int variant = getInt(LASTVARIANT);
        if (variant == IDataAccess.UNDEFINED_INT || variant < 0) {
            variant = 0;
        }
        variant++; // start with 1 as first variant

        if (description == null) {
            description = "";
        }
        if (type == null) {
            type = EfaTypes.TYPE_BOAT_OTHER;
        }
        if (seats == null) {
            seats = EfaTypes.TYPE_NUMSEATS_OTHER;
        }
        if (rigging == null) {
            rigging = EfaTypes.TYPE_RIGGING_OTHER;
        }
        if (coxing == null) {
            coxing = EfaTypes.TYPE_COXING_OTHER;
        }

        DataTypeList lvariant = getList(TYPEVARIANT, IDataAccess.DATA_INTEGER);
        DataTypeList ldescription = getList(TYPEDESCRIPTION, IDataAccess.DATA_STRING);
        DataTypeList ltype = getList(TYPETYPE, IDataAccess.DATA_STRING);
        DataTypeList lseats = getList(TYPESEATS, IDataAccess.DATA_STRING);
        DataTypeList lrigging = getList(TYPERIGGING, IDataAccess.DATA_STRING);
        DataTypeList lcoxing = getList(TYPECOXING, IDataAccess.DATA_STRING);

        if (lvariant == null) {
            lvariant = DataTypeList.parseList(Integer.toString(variant), IDataAccess.DATA_INTEGER);
        } else {
            lvariant.add(variant);
        }
        if (ldescription == null) {
            ldescription = DataTypeList.parseList(description, IDataAccess.DATA_STRING);
        } else {
            ldescription.add(description);
        }
        if (ltype == null) {
            ltype = DataTypeList.parseList(type, IDataAccess.DATA_STRING);
        } else {
            ltype.add(type);
        }
        if (lseats == null) {
            lseats = DataTypeList.parseList(seats, IDataAccess.DATA_STRING);
        } else {
            lseats.add(seats);
        }
        if (lrigging == null) {
            lrigging = DataTypeList.parseList(rigging, IDataAccess.DATA_STRING);
        } else {
            lrigging.add(rigging);
        }
        if (lcoxing == null) {
            lcoxing = DataTypeList.parseList(coxing, IDataAccess.DATA_STRING);
        } else {
            lcoxing.add(coxing);
        }

        setInt(LASTVARIANT, variant);
        setList(TYPEVARIANT, lvariant);
        setList(TYPEDESCRIPTION, ldescription);
        setList(TYPETYPE, ltype);
        setList(TYPESEATS, lseats);
        setList(TYPERIGGING, lrigging);
        setList(TYPECOXING, lcoxing);

        if (isDefault != null && isDefault.equals(Boolean.TRUE.toString())) {
            setDefaultVariant(variant);
        }

        return variant;
    }

    public int getLastVariant() {
        return getInt(LASTVARIANT);
    }

    public int getDefaultVariant() {
        return getInt(DEFAULTVARIANT);
    }

    public void setDefaultVariant(int variant) {
        setInt(DEFAULTVARIANT, variant);
    }

    public boolean setTypeVariant(int idx, String description, String type, String seats, String rigging, String coxing, String isDefault) {
        if (description == null) {
            description = "";
        }
        if (type == null) {
            type = EfaTypes.TYPE_BOAT_OTHER;
        }
        if (seats == null) {
            seats = EfaTypes.TYPE_NUMSEATS_OTHER;
        }
        if (rigging == null) {
            rigging = EfaTypes.TYPE_RIGGING_OTHER;
        }
        if (coxing == null) {
            coxing = EfaTypes.TYPE_COXING_OTHER;
        }

        DataTypeList ldescription = getList(TYPEDESCRIPTION, IDataAccess.DATA_STRING);
        DataTypeList ltype = getList(TYPETYPE, IDataAccess.DATA_STRING);
        DataTypeList lseats = getList(TYPESEATS, IDataAccess.DATA_STRING);
        DataTypeList lrigging = getList(TYPERIGGING, IDataAccess.DATA_STRING);
        DataTypeList lcoxing = getList(TYPECOXING, IDataAccess.DATA_STRING);

        // ldescription might be null because it only contains 0-length strings
        if (ldescription == null && ltype != null && ltype.length() > 0) {
            ldescription = DataTypeList.parseList("", IDataAccess.DATA_STRING);
        }
        while (ldescription != null && idx >= ldescription.length()) {
            ldescription.add(""); // might be null because it only contains 0-length strings
        }        

        if (idx < 0 ||
                ldescription == null || idx >= ldescription.length() ||
                ltype == null || idx >= ltype.length() ||
                lseats == null || idx >= lseats.length() ||
                lrigging == null || idx >= lrigging.length() ||
                lcoxing == null || idx >= lcoxing.length() ) {
            return false;
        }

        ldescription.set(idx, description);
        ltype.set(idx, type);
        lseats.set(idx, seats);
        lrigging.set(idx, rigging);
        lcoxing.set(idx, coxing);

        setList(TYPEDESCRIPTION, ldescription);
        setList(TYPETYPE, ltype);
        setList(TYPESEATS, lseats);
        setList(TYPERIGGING, lrigging);
        setList(TYPECOXING, lcoxing);

        if (isDefault != null && isDefault.equals(Boolean.TRUE.toString())) {
            setDefaultVariant(getTypeVariant(idx));
        }

        return true;
    }

    public boolean deleteTypeVariant(int idx) {
        DataTypeList lvariant = getList(TYPEVARIANT, IDataAccess.DATA_INTEGER);
        DataTypeList ldescription = getList(TYPEDESCRIPTION, IDataAccess.DATA_STRING);
        DataTypeList ltype = getList(TYPETYPE, IDataAccess.DATA_STRING);
        DataTypeList lseats = getList(TYPESEATS, IDataAccess.DATA_STRING);
        DataTypeList lrigging = getList(TYPERIGGING, IDataAccess.DATA_STRING);
        DataTypeList lcoxing = getList(TYPECOXING, IDataAccess.DATA_STRING);

        if (ldescription == null) { // fixing the description list, which may be null if it only contains "" strings
            ldescription = DataTypeList.parseList("", IDataAccess.DATA_STRING);
        }
        while (ldescription != null && idx >= ldescription.length()) {
            ldescription.add(""); // might be null because it only contains 0-length strings
        }

        if (idx < 0 ||
                lvariant == null || idx >= lvariant.length() ||
                ldescription == null || idx >= ldescription.length() ||
                ltype == null || idx >= ltype.length() ||
                lseats == null || idx >= lseats.length() ||
                lrigging == null || idx >= lrigging.length() ||
                lcoxing == null || idx >= lcoxing.length() ) {
            return false;
        }

        boolean resetDefaultVariant = false;
        if (getDefaultVariant() == getTypeVariant(idx)) {
            resetDefaultVariant = true;
        }

        lvariant.remove(idx);
        ldescription.remove(idx);
        ltype.remove(idx);
        lseats.remove(idx);
        lrigging.remove(idx);
        lcoxing.remove(idx);

        setList(TYPEVARIANT, lvariant);
        setList(TYPEDESCRIPTION, ldescription);
        setList(TYPETYPE, ltype);
        setList(TYPESEATS, lseats);
        setList(TYPERIGGING, lrigging);
        setList(TYPECOXING, lcoxing);

        if (resetDefaultVariant) {
            int v = getTypeVariant(0);
            if (v > 0) {
                setDefaultVariant(v);
            }
        }

        return true;
    }

    public void setAllowedGroupIdList(DataTypeList<UUID> list) {
        setList(ALLOWEDGROUPIDLIST, list);
    }
    public DataTypeList<UUID> getAllowedGroupIdList() {
        return getList(ALLOWEDGROUPIDLIST, IDataAccess.DATA_UUID);
    }

    public Vector<String> getAllowedGroupsAsNameVector(long validAt) {
        DataTypeList<UUID> list = getAllowedGroupIdList();
        if (list == null || list.length() == 0) {
            return new Vector<String>();
        }
        Vector<String> v = new Vector<String>();
        Groups p = this.getPersistence().getProject().getGroups(false);
        for (int i=0; p != null && i < list.length(); i++) {
            GroupRecord r = p.findGroupRecord(list.get(i), validAt);
            if (r != null) {
                v.add(r.getQualifiedName());
            }
        }
        return v;
    }

    public String getAllowedGroupsAsNameString(long validAt) {
        Vector<String> v = getAllowedGroupsAsNameVector(validAt);
        if (v == null || v.size() == 0) {
            return "";
        }
        return EfaUtil.vector2string(v, ", ");
    }

    public void setMaxNotInGroup(int maxNotInGroup) {
        setInt(MAXNOTINGROUP, maxNotInGroup);
    }
    public int getMaxNotInGroup() {
        return getInt(MAXNOTINGROUP);
    }

    public void setRequiredGroupId(UUID id) {
        setUUID(REQUIREDGROUPID, id);
    }
    public UUID getRequiredGroupId() {
        return getUUID(REQUIREDGROUPID);
    }
    public String getRequiredGroupAsName(long validAt) {
        UUID id = getUUID(REQUIREDGROUPID);
        Groups groups = getPersistence().getProject().getGroups(false);
        if (groups != null) {
            DataRecord r = groups.findGroupRecord(id, validAt);
            if (r != null) {
                return r.getQualifiedName();
            }
        }
        return null;
    }

    public void setOnlyWithBoatCaptain(boolean onlyWithBoatCaptain) {
        setBool(ONLYWITHBOATCAPTAIN, onlyWithBoatCaptain);
    }
    public boolean getOnlyWithBoatCaptain() {
        return getBool(ONLYWITHBOATCAPTAIN);
    }

    public void setManufacturer(String manufacturer) {
        setString(MANUFACTURER, manufacturer);
    }
    public String getManufacturer() {
        return getString(MANUFACTURER);
    }

    public void setModel(String model) {
        setString(MODEL, model);
    }
    public String getModel() {
        return getString(MODEL);
    }

    public void setMaxCrewWeight(int weight) {
        setInt(MAXCREWWEIGHT, weight);
    }
    public int getMaxCrewWeight() {
        return getInt(MAXCREWWEIGHT);
    }

    public void setManufactionDate(DataTypeDate date) {
        setDate(MANUFACTIONDATE, date);
    }
    public DataTypeDate getManufactionDate() {
        return getDate(MANUFACTIONDATE);
    }

    public void setSerialNo(String serialNo) {
        setString(SERIALNO, serialNo);
    }
    public String getSerialNo() {
        return getString(SERIALNO);
    }

    public void setPurchaseDate(DataTypeDate date) {
        setDate(PURCHASEDATE, date);
    }
    public DataTypeDate getPurchaseDate() {
        return getDate(PURCHASEDATE);
    }

    public void setPurchasePrice(DataTypeDecimal price) {
        setDecimal(PURCHASEPRICE, price);
    }
    public DataTypeDecimal getPurchasePrice() {
        return getDecimal(PURCHASEPRICE);
    }


    public void setCurrency(String currency) {
        setString(CURRENCY, currency);
    }
    public String getCurrency() {
        return getString(CURRENCY);
    }

    public void setSellingDate(DataTypeDate date) {
        setDate(SELLINGDATE, date);
    }
    public DataTypeDate getSellingDate() {
        return getDate(SELLINGDATE);
    }

    public void setSellingPrice(DataTypeDecimal price) {
        setDecimal(SELLINGPRICE, price);
    }
    public DataTypeDecimal getSellingPrice() {
        return getDecimal(SELLINGPRICE);
    }


    public void setInsuranceValue(DataTypeDecimal price) {
        setDecimal(INSURANCEVALUE, price);
    }
    public DataTypeDecimal getInsuranceValue() {
        return getDecimal(INSURANCEVALUE);
    }


    public void setDefaultCrewId(UUID id) {
        setUUID(DEFAULTCREWID, id);
    }
    public UUID getDefaultCrewId() {
        return getUUID(DEFAULTCREWID);
    }
    public String getDefaultCrewAsName() {
        UUID id = getUUID(DEFAULTCREWID);
        Crews crews = getPersistence().getProject().getCrews(false);
        if (crews != null) {
            DataRecord r = crews.getCrew(id);
            if (r != null) {
                return r.getQualifiedName();
            }
        }
        return null;
    }

    public void setDefaultSessionType(String type) {
        setString(DEFAULTSESSIONTYPE, type);
    }
    public String getDefaultSessionType() {
        return Daten.efaTypes.getSessionType(getString(DEFAULTSESSIONTYPE));
    }

    public void setDefaultDestinationId(UUID id) {
        setUUID(DEFAULTDESTINATIONID, id);
    }
    public UUID getDefaultDestinationId() {
        return getUUID(DEFAULTDESTINATIONID);
    }
    public String getDefaultDestinationAsName(long validAt) {
        UUID id = getUUID(DEFAULTDESTINATIONID);
        Destinations destinations = getPersistence().getProject().getDestinations(false);
        if (destinations != null) {
            DataRecord r = destinations.getDestination(id, validAt);
            if (r != null) {
                return r.getQualifiedName();
            }
        }
        return null;
    }

    public void setExcludeFromPublicStatistics(boolean exclude) {
        setBool(EXCLUDEFROMSTATISTIC, exclude);
    }
    public boolean getExcludeFromPublicStatistics() {
        return getBool(EXCLUDEFROMSTATISTIC);
    }

    public void setFreeUse1(String s) {
        setString(FREEUSE1, s);
    }
    public String getFreeUse1() {
        return getString(FREEUSE1);
    }

    public void setFreeUse2(String s) {
        setString(FREEUSE2, s);
    }
    public String getFreeUse2() {
        return getString(FREEUSE2);
    }

    public void setFreeUse3(String s) {
        setString(FREEUSE3, s);
    }
    public String getFreeUse3() {
        return getString(FREEUSE3);
    }

    public String getQualifiedVariantName(int variant) {
        int idx = getVariantIndex(variant);
        String s = getTypeDescription(idx);
        if (s != null) {
            return s;
        }
        return "";
    }

    public String getQualifiedName() {
        String name = getName();
        if (name != null && name.length() > 0 && getNameAffix() != null && getNameAffix().length() > 0) {
            name = name + " (" + getNameAffix() + ")";
        }
        return (name != null ? name : "");
    }

    public String[] getQualifiedNameFields() {
        return IDX_NAME_NAMEAFFIX;
    }

    public String[] getQualifiedNameValues(String qname) {
        Matcher m = qnamePattern.matcher(qname);
        if (m.matches()) {
            return new String[] {
                m.group(1).trim(),
                m.group(2).trim()
            };
        } else {
            return new String[] {
                qname.trim(),
                null
            };
        }
    }

    public static String[] tryGetNameAndAffix(String s) {
        Matcher m = qnamePattern.matcher(s);
        if (m.matches()) {
            return new String[] {
                m.group(1).trim(),
                m.group(2).trim()
            };
        } else {
            return new String[] {
                s.trim(),
                null
            };
        }
    }

    public Object getUniqueIdForRecord() {
        return getId();
    }

    public int getNumberOfSeats(int idx) {
        return getNumberOfSeats(idx, 0);
    }

    public int getNumberOfSeats(int idx, int defaultIfOther) {
        return EfaUtil.stringFindInt(getTypeSeats(idx), defaultIfOther);
    }

    public static String getDetailedBoatType(String tBoatType, String tNumSeats, String tCoxing) {
        return International.getMessage("{boattype} {numseats} {coxedornot}",
                Daten.efaTypes.getValue(EfaTypes.CATEGORY_BOAT, tBoatType),
                Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, tNumSeats),
                Daten.efaTypes.getValue(EfaTypes.CATEGORY_COXING, tCoxing));
    }

    public static String getDetailedBoatType(BoatRecord r, int idx) {
        if (r == null || Daten.efaTypes == null) {
            return null;
        }
        return getDetailedBoatType(r.getTypeType(idx), r.getTypeSeats(idx), r.getTypeCoxing(idx));
    }

    public String getDetailedBoatType(int idx) {
        return getDetailedBoatType(this, idx);
    }

    public String getShortBoatType(int idx) {
        int seats = getNumberOfSeats(idx);
        String rig = getTypeRigging(idx);
        String cox = getTypeCoxing(idx);
        if (rig == null || cox == null || seats == 0) {
            return getDetailedBoatType(idx);
        }
        if (!rig.equals(EfaTypes.TYPE_RIGGING_SCULL) &&
            !rig.equals(EfaTypes.TYPE_RIGGING_SWEEP)) {
            return getDetailedBoatType(idx);
        }
        if (!cox.equals(EfaTypes.TYPE_COXING_COXED) &&
            !cox.equals(EfaTypes.TYPE_COXING_COXLESS)) {
            return getDetailedBoatType(idx);
        }
        boolean skull = rig.equals(EfaTypes.TYPE_RIGGING_SCULL);
        boolean coxed = cox.equals(EfaTypes.TYPE_COXING_COXED);
        if (seats % 2 == 1 && !skull) {
            return getDetailedBoatType(idx);
        }
        return Integer.toString(seats) + (skull ? "x" : "") + (coxed ? "+" : "-");
    }

    public String getGeneralNumberOfSeatsType(int idx) {
        String numSeats = getTypeSeats(idx);
        if (numSeats.equals(EfaTypes.TYPE_NUMSEATS_2X)) {
            return EfaTypes.TYPE_NUMSEATS_2;
        }
        if (numSeats.equals(EfaTypes.TYPE_NUMSEATS_4X)) {
            return EfaTypes.TYPE_NUMSEATS_4;
        }
        if (numSeats.equals(EfaTypes.TYPE_NUMSEATS_6X)) {
            return EfaTypes.TYPE_NUMSEATS_6;
        }
        if (numSeats.equals(EfaTypes.TYPE_NUMSEATS_8X)) {
            return EfaTypes.TYPE_NUMSEATS_8;
        }
        return numSeats;
    }

    public String getGeneralNumberOfSeatsValue(int idx) {
        return Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, getGeneralNumberOfSeatsType(idx));
    }

    public String getQualifiedBoatTypeName(int idx) {
        String name = getTypeDescription(idx);
        String type = getDetailedBoatType(idx);
        if (name == null || name.length() == 0) {
            return (type != null && type.length() > 0 ? type : toString());
        }
        return name + (type != null && type.length() > 0 ? " (" + type + ")" : "");
    }

    public String getQualifiedBoatTypeShortName(int idx) {
        String name = getTypeDescription(idx);
        String type = getShortBoatType(idx);
        if (name == null || name.length() == 0) {
            return (type != null && type.length() > 0 ? type : toString());
        }
        return name + (type != null && type.length() > 0 ? " (" + type + ")" : "");
    }

    public BoatStatusRecord getBoatStatus() {
        try {
            return getPersistence().getProject().getBoatStatus(false).getBoatStatus(this.getId());
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    /**
     * Determine if a boat has at least a variant as a one-seater.
     * Does not check if the BoatRecord is valid at the current time.
     * 
     * @param boatRec BoatRecord (not null)
     * @return true if Boat has at least one variant as a One-Seater
     */
    public boolean isOneSeaterBoat() {

        for (int boatVariant=0; boatVariant<this.getNumberOfVariants(); boatVariant++) {
            if (this.getNumberOfSeats(boatVariant)==1) {
            	return true;
            }
        }
        //none of the variants is a OneSeater
        return false;
    }    

    public IItemType[] getDefaultItems(String itemName) {
        if (itemName.equals(BoatRecord.GUIITEM_BOATTYPES)) {
            IItemType[] items = new IItemType[7];
            String CAT_BASEDATA = "%01%" + International.getString("Basisdaten");
            items[0] = new ItemTypeInteger(BoatRecord.TYPEVARIANT, 0, 0, Integer.MAX_VALUE,
                    IItemType.TYPE_INTERNAL, CAT_BASEDATA, International.getString("Variante"));
            items[1] = new ItemTypeString(BoatRecord.TYPEDESCRIPTION, "",
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Beschreibung"));
            items[2] = new ItemTypeStringList(BoatRecord.TYPETYPE, Daten.efaTypes.TYPE_BOAT_OTHER,
                    EfaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                    International.getString("Bootstyp"));
            items[3] = new ItemTypeStringList(BoatRecord.TYPESEATS, Daten.efaTypes.TYPE_NUMSEATS_OTHER,
                    EfaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                    International.getString("Bootsplätze"));
            items[4] = new ItemTypeStringList(BoatRecord.TYPERIGGING, Daten.efaTypes.TYPE_RIGGING_OTHER,
                    EfaTypes.makeBoatRiggingArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes.makeBoatRiggingArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                    International.getString("Riggerung"));
            items[5] = new ItemTypeStringList(BoatRecord.TYPECOXING, Daten.efaTypes.TYPE_COXING_OTHER,
                    EfaTypes.makeBoatCoxingArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes.makeBoatCoxingArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                    International.getString("Steuerung"));
            items[6] = new ItemTypeBoolean(BoatRecord.GUIITEM_DEFAULTBOATTYPE, false,
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                    International.getString("Standard-Bootstyp"));
            ((ItemTypeBoolean)items[6]).setUseRadioButton(true, buttonGroup);
            return items;
        }
        if (itemName.equals(BoatRecord.GUIITEM_ALLOWEDGROUPIDLIST)) {
            IItemType[] items = new IItemType[1];
            String CAT_USAGE = "%03%" + International.getString("Benutzung");
            items[0] = getGuiItemTypeStringAutoComplete(BoatRecord.ALLOWEDGROUPIDLIST, null,
                    IItemType.TYPE_PUBLIC, CAT_USAGE,
                    getPersistence().getProject().getGroups(false), getValidFrom(), getInvalidFrom()-1,
                    International.getString("Gruppe"));
            items[0].setFieldSize(300, -1);
            return items;
        }
        return null;
    }

    public Hashtable<String,TableItem[]> getTableItems(String itemName, Hashtable<String,IItemType[]> data) {
        if (itemName.equals(BoatRecord.GUIITEM_RESERVATIONS)) {
            if (data == null) {
                return null;
            }
            String[] keys = data.keySet().toArray(new String[0]);
            Hashtable<String,TableItem[]> tableItems = new Hashtable<String,TableItem[]>();
            for (int i=0; i<keys.length; i++) {
                
            }
        }
        return null;
    }

    public String getAsText(String fieldName) {
        if (fieldName.equals(TYPEVARIANT) ||
            fieldName.equals(TYPEDESCRIPTION)) {
            DataTypeList list = (DataTypeList)get(fieldName);
            if (list != null) {
                return list.toString();
            }
            return null;
        }
        if (fieldName.equals(TYPETYPE) ||
            fieldName.equals(TYPESEATS) ||
            fieldName.equals(TYPERIGGING) ||
            fieldName.equals(TYPECOXING)) {
            DataTypeList list = (DataTypeList)get(fieldName);
            if (list == null) {
                return null;
            } else {
                String cat = null;
                if (fieldName.equals(TYPETYPE)) {
                    cat = EfaTypes.CATEGORY_BOAT;
                }
                if (fieldName.equals(TYPESEATS)) {
                    cat = EfaTypes.CATEGORY_NUMSEATS;
                }
                if (fieldName.equals(TYPERIGGING)) {
                    cat = EfaTypes.CATEGORY_RIGGING;
                }
                if (fieldName.equals(TYPECOXING)) {
                    cat = EfaTypes.CATEGORY_COXING;
                }
                String s = null;
                for (int i=0; i<list.length(); i++) {
                    s = (s != null ? s + ";" : "") + Daten.efaTypes.getValue(cat, (String)list.get(i));
                }
                return s;
            }
        }
        if (fieldName.equals(ALLOWEDGROUPIDLIST)) {
            DataTypeList list = (DataTypeList)get(fieldName);
            if (list == null) {
                return null;
            } else {
                return getAllowedGroupsAsNameString(System.currentTimeMillis());
            }
        }
        if (fieldName.equals(REQUIREDGROUPID)) {
            return getRequiredGroupAsName(System.currentTimeMillis());
        }
        if (fieldName.equals(DEFAULTCREWID)) {
            return getDefaultCrewAsName();
        }
        if (fieldName.equals(DEFAULTSESSIONTYPE)) {
            String s = getAsString(fieldName);
            if (s != null) {
                return Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, s);
            }
            return null;
        }
        if (fieldName.equals(DEFAULTDESTINATIONID)) {
            return getDefaultDestinationAsName(System.currentTimeMillis());
        }
        return super.getAsText(fieldName);
    }

    public boolean setFromText(String fieldName, String value) {
        if (fieldName.equals(TYPEVARIANT)) {
            DataTypeList list = DataTypeList.parseList(value, IDataAccess.DATA_INTEGER);
            if (list != null && list.isSet() && list.length() > 0) {
                set(fieldName, list);
            }
        } else if (fieldName.equals(TYPEDESCRIPTION)) {
            DataTypeList list = DataTypeList.parseList(value, IDataAccess.DATA_STRING);
            if (list != null && list.isSet() && list.length() > 0) {
                set(fieldName, list);
            }
        } else if (fieldName.equals(TYPETYPE) ||
            fieldName.equals(TYPESEATS) ||
            fieldName.equals(TYPERIGGING) ||
            fieldName.equals(TYPECOXING)) {
            DataTypeList list = DataTypeList.parseList(value, IDataAccess.DATA_STRING);
            if (list != null && list.isSet() && list.length() > 0) {
                String cat = null;
                if (fieldName.equals(TYPETYPE)) {
                    cat = EfaTypes.CATEGORY_BOAT;
                }
                if (fieldName.equals(TYPESEATS)) {
                    cat = EfaTypes.CATEGORY_NUMSEATS;
                }
                if (fieldName.equals(TYPERIGGING)) {
                    cat = EfaTypes.CATEGORY_RIGGING;
                }
                if (fieldName.equals(TYPECOXING)) {
                    cat = EfaTypes.CATEGORY_COXING;
                }
                String s = null;
                for (int i=0; i<list.length(); i++) {
                    s = Daten.efaTypes.getTypeForValue(cat, (String)list.get(i));
                    if (s != null) {
                        list.set(i, s);
                    }
                }
                set(fieldName, list);
            }
        } else if (fieldName.equals(ALLOWEDGROUPIDLIST)) {
            Vector<String> values = EfaUtil.split(value, ',');
            DataTypeList<UUID> list = new DataTypeList<UUID>();
            Groups groups = getPersistence().getProject().getGroups(false);
            for (int i=0; i<values.size(); i++) {
                GroupRecord gr = groups.findGroupRecord(values.get(i).trim(), -1);
                if (gr != null) {
                    list.add(gr.getId());
                }
            }
            if (list.length() > 0) {
                set(fieldName, list);
            }
        } else if (fieldName.equals(REQUIREDGROUPID)) {
            Groups groups = getPersistence().getProject().getGroups(false);
            GroupRecord gr = groups.findGroupRecord(value, -1);
            if (gr != null) {
                set(fieldName, gr.getId());
            }
        } else if (fieldName.equals(DEFAULTCREWID)) {
            Crews crews = getPersistence().getProject().getCrews(false);
            CrewRecord gr = crews.findCrewRecord(value);
            if (gr != null) {
                set(fieldName, gr.getId());
            }
        } else if (fieldName.equals(DEFAULTSESSIONTYPE)) {
            String s = Daten.efaTypes.getTypeForValue(EfaTypes.CATEGORY_SESSION, value);
            if (s != null) {
                set(fieldName, s);
            }
        } else if (fieldName.equals(DEFAULTDESTINATIONID)) {
            Destinations destinations = getPersistence().getProject().getDestinations(false);
            DestinationRecord dr = destinations.getDestination(value, -1);
            if (dr != null) {
                set(fieldName, dr.getId());
            }
        } else {
            return super.setFromText(fieldName, value);
        }
        return (value.equals(getAsText(fieldName)));
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        String CAT_BASEDATA     = "%01%" + International.getString("Basisdaten");
        String CAT_MOREDATA     = "%02%" + International.getString("Weitere Daten");
        String CAT_USAGE        = "%03%" + International.getString("Benutzung");
        String CAT_RESERVATIONS = "%04%" + International.getString("Reservierungen");
        String CAT_DAMAGES      = "%05%" + International.getString("Bootsschäden");
        String CAT_FREEUSE      = "%07%" + International.getString("Freie Verwendung");

        Groups groups = getPersistence().getProject().getGroups(false);
        Crews crews = getPersistence().getProject().getCrews(false);
        Destinations destinations = getPersistence().getProject().getDestinations(false);
        BoatStatus boatStatus = getPersistence().getProject().getBoatStatus(false);
        BoatReservations boatReservations = getPersistence().getProject().getBoatReservations(false);
        BoatDamages boatDamages = getPersistence().getProject().getBoatDamages(false);
        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();
        Vector<IItemType[]> itemList;

        // CAT_BASEDATA
        v.add(item = new ItemTypeString(BoatRecord.NAME, getName(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Name")));
        ((ItemTypeString)item).setNotAllowedCharacters("()");
        v.add(item = new ItemTypeString(BoatRecord.NAMEAFFIX, getNameAffix(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Namenszusatz")));
        ((ItemTypeString)item).setNotAllowedCharacters("()");
        v.add(item = new ItemTypeString(BoatRecord.OWNER, getOwner(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Eigentümer") +
                 " (" + International.getString("Fremdboot") + ")"));
        ((ItemTypeString)item).setNotAllowedCharacters("()");

        itemList = new Vector<IItemType[]>();
        for (int i=0; i<getNumberOfVariants(); i++) {
            IItemType[] items = getDefaultItems(GUIITEM_BOATTYPES);
            items[0].parseValue(Integer.toString(getTypeVariant(i)));
            items[1].parseValue(getTypeDescription(i));
            items[2].parseValue(getTypeType(i));
            items[3].parseValue(getTypeSeats(i));
            items[4].parseValue(getTypeRigging(i));
            items[5].parseValue(getTypeCoxing(i));
            items[6].parseValue(Boolean.toString(getTypeVariant(i) == getDefaultVariant()));
            itemList.add(items);
        }
        v.add(item = new ItemTypeItemList(GUIITEM_BOATTYPES, itemList, this,
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Bootstyp")));
        ((ItemTypeItemList)item).setMinNumberOfItems(1);
        item.setPadding(0, 0, 20, 0);

        // CAT_MOREDATA
        v.add(item = new ItemTypeString(BoatRecord.MANUFACTURER, getManufacturer(),
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Hersteller")));
        v.add(item = new ItemTypeString(BoatRecord.MODEL, getModel(),
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Modell")));
        v.add(item = new ItemTypeInteger(BoatRecord.MAXCREWWEIGHT, getMaxCrewWeight(), 0, Integer.MAX_VALUE, true,
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Maximales Mannschaftsgewicht")));
        v.add(item = new ItemTypeDate(BoatRecord.MANUFACTIONDATE, getManufactionDate(),
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Herstellungsdatum")));
        ((ItemTypeDate)item).setAllowYearOnly(true);
        v.add(item = new ItemTypeString(BoatRecord.SERIALNO, getSerialNo(),
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Seriennummer")));
        v.add(item = new ItemTypeDate(BoatRecord.PURCHASEDATE, getPurchaseDate(),
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Kaufdatum")));
        ((ItemTypeDate)item).setAllowYearOnly(true);
        v.add(item = new ItemTypeDecimal(BoatRecord.PURCHASEPRICE, getPurchasePrice(), 2, true,
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Kaufpreis")));
        v.add(item = new ItemTypeDate(BoatRecord.SELLINGDATE, getSellingDate(),
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Verkaufdatum")));
        ((ItemTypeDate)item).setAllowYearOnly(true);
        v.add(item = new ItemTypeDecimal(BoatRecord.SELLINGPRICE, getSellingPrice(), 2, true,
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Verkaufspreis")));
        v.add(item = new ItemTypeDecimal(BoatRecord.INSURANCEVALUE, getInsuranceValue(), 2, true,
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Versicherungswert")));
        v.add(item = new ItemTypeString(BoatRecord.CURRENCY, getCurrency(),
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Währung")));
        v.add(item = new ItemTypeBoolean(PersonRecord.EXCLUDEFROMSTATISTIC, getExcludeFromPublicStatistics(),
                IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("von allgemein verfügbaren Statistiken ausnehmen")));
        if (Daten.efaConfig.getValueUseFunctionalityCanoeingGermany()) {
            v.add(item = new ItemTypeString(BoatRecord.EFBID, getEfbId(),
                    (Daten.efaConfig.getValueKanuEfb_AlwaysShowKanuEFBFields() ? IItemType.TYPE_PUBLIC : IItemType.TYPE_EXPERT), 
                    CAT_MOREDATA, International.onlyFor("Kanu-eFB ID","de")));
        }

        // CAT_USAGE
        itemList = new Vector<IItemType[]>();
        DataTypeList<UUID> agList = getAllowedGroupIdList();
        for (int i=0; agList != null && i<agList.length(); i++) {
            IItemType[] items = getDefaultItems(GUIITEM_ALLOWEDGROUPIDLIST);
            ((ItemTypeStringAutoComplete)items[0]).setId(agList.get(i));
            itemList.add(items);
        }
        v.add(item = new ItemTypeItemList(GUIITEM_ALLOWEDGROUPIDLIST, itemList, this,
                IItemType.TYPE_PUBLIC, CAT_USAGE, International.getString("Gruppen, die dieses Boot benutzen dürfen")));
        ((ItemTypeItemList)item).setXForAddDelButtons(3);
        ((ItemTypeItemList)item).setPadYbetween(0);
        ((ItemTypeItemList)item).setRepeatTitle(false);
        ((ItemTypeItemList)item).setAppendPositionToEachElement(true);
        v.add(item = new ItemTypeInteger(BoatRecord.MAXNOTINGROUP, getMaxNotInGroup(), 0, Integer.MAX_VALUE, true,
                IItemType.TYPE_PUBLIC, CAT_USAGE, International.getString("Maxmimale Personenzahl nicht aus erlaubten Gruppen")));
        v.add(item = getGuiItemTypeStringAutoComplete(BoatRecord.REQUIREDGROUPID, getRequiredGroupId(),
                IItemType.TYPE_PUBLIC, CAT_USAGE, 
                groups, getValidFrom(), getInvalidFrom()-1,
                International.getString("Gruppe, der mindestens eine Person angehören muß")));
        item.setFieldSize(300, -1);
        
        v.add(createHeader("_WeitereEigenschaften", IItemType.TYPE_PUBLIC, CAT_USAGE, International.getString("Weitere Eigenschaften"), 2));
        
        v.add(item = getGuiItemTypeStringAutoComplete(BoatRecord.DEFAULTCREWID, getDefaultCrewId(),
                IItemType.TYPE_PUBLIC, CAT_USAGE,
                crews, getValidFrom(), getInvalidFrom()-1,
                International.getString("Standard-Mannschaft")));
        item.setFieldSize(300, -1);
        v.add(item = new ItemTypeStringList(BoatRecord.DEFAULTSESSIONTYPE, getDefaultSessionType(),
                EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES, true), EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY, true),
                IItemType.TYPE_PUBLIC, CAT_USAGE,
                International.getString("Standard-Fahrtart")));
        v.add(item = getGuiItemTypeStringAutoComplete(BoatRecord.DEFAULTDESTINATIONID, getDefaultDestinationId(),
                IItemType.TYPE_PUBLIC, CAT_USAGE,
                destinations, getValidFrom(), getInvalidFrom()-1,
                International.getString("Standard-Ziel")));
        item.setFieldSize(300, -1);

        v.add(item = new ItemTypeBoolean(BoatRecord.ONLYWITHBOATCAPTAIN, getOnlyWithBoatCaptain(),
                IItemType.TYPE_PUBLIC, CAT_USAGE, International.getString("Boot darf nur mit Obmann genutzt werden")));
        
        // CAT_RESERVATIONS
        if (getId() != null && admin != null && admin.isAllowedEditBoatReservation()) {
            v.add(item = new ItemTypeDataRecordTable(GUIITEM_RESERVATIONS,
                    boatReservations.createNewRecord().getGuiTableHeader(),
                    boatReservations, 0, admin,
                    BoatReservationRecord.BOATID, getId().toString(),
                    null, null, null, this,
                    IItemType.TYPE_PUBLIC, CAT_RESERVATIONS, International.getString("Reservierungen")));
            ((ItemTypeDataRecordTable) item).setButtonPanelPosition(BorderLayout.NORTH);
            item.setFieldSize(850, -1);
            item.setPadding(10,10,10,10);
            item.setFieldGrid(1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH);
        }
        
        // CAT_DAMAGES
        if (getId() != null && admin != null && admin.isAllowedEditBoatDamages()) {
            v.add(item = new ItemTypeDataRecordTable(GUIITEM_DAMAGES,
                    boatDamages.createNewRecord().getGuiTableHeader(),
                    boatDamages, 0, admin,
                    BoatDamageRecord.BOATID, getId().toString(),
                    null, null, null, this,
                    IItemType.TYPE_PUBLIC, CAT_DAMAGES, International.getString("Bootsschäden")));
            ((ItemTypeDataRecordTable) item).setButtonPanelPosition(BorderLayout.NORTH);
            item.setFieldSize(850, -1);
            item.setPadding(10, 10, 10, 10);
            item.setFieldGrid(1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH);
        }

        // CAT_STATUS
        if (getId() != null && admin != null && admin.isAllowedEditBoatStatus()) {
            BoatStatusRecord boatStatusRecord = boatStatus.getBoatStatus(getId());
            if (boatStatusRecord != null) {
                v.addAll(boatStatusRecord.getGuiItems(admin));
            }
        }

        // CAT_FREEUSE
        v.add(item = new ItemTypeString(BoatRecord.FREEUSE1, getFreeUse1(),
                IItemType.TYPE_PUBLIC, CAT_FREEUSE, International.getString("Freie Verwendung") + " 1"));
        v.add(item = new ItemTypeString(BoatRecord.FREEUSE2, getFreeUse2(),
                IItemType.TYPE_PUBLIC, CAT_FREEUSE, International.getString("Freie Verwendung") + " 2"));
        v.add(item = new ItemTypeString(BoatRecord.FREEUSE3, getFreeUse3(),
                IItemType.TYPE_PUBLIC, CAT_FREEUSE, International.getString("Freie Verwendung") + " 3"));
  
        return v;
    }

    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[3];
        header[0] = new TableItemHeader(International.getString("Name"));
        header[1] = new TableItemHeader(International.getString("Bootstyp"));
        header[2] = new TableItemHeader(International.getString("Eigentümer"));
        return header;
    }

    public TableItem[] getGuiTableItems() {
        TableItem[] items = new TableItem[3];
        items[0] = new TableItem(getQualifiedName());
        String type = "";
        if (getNumberOfVariants() > 0) {
            type = getDetailedBoatType(0);
        }
        if (getNumberOfVariants() > 1) {
            type = type + " ...";
        }
        items[1] = new TableItem(type);
        items[2] = new TableItem(getOwner());
        items[0].addIcon(this.createGroupPieIcon(16,16));
        items[0].setToolTipText(this.createTooltipForGroups());
        return items;
    }

    private ImageIcon createGroupPieIcon(int iconWidth, int iconHeight) {
    	 Color[] colors = this.getBoatGroupsPieColors(null); 
    	 return (colors !=null ? EfaUtil.createColorPieIcon(colors, iconWidth, iconHeight) : null);
    }
    
    private String createTooltipForGroups() {
    	String result = this.getAllowedGroupsAsNameString(System.currentTimeMillis());
    	if (result!=null && !result.isEmpty()) {
    		return International.getString("Gruppen")+":\n   "+result;
    	} else {
    		return null;
    	}
    		
    }
    
    /**
     * Returns the colors of all groups the boat is currently assigned to.
     * Due to performance reasons in the ItemTypeBoatStatusList, an hashtable can be provided 
     * which contains the color for a certain group (identified by uuid).
     * 
     * @param groupColors
     * @return array of colors, or null, if boat is assigned to no group
     */
    public Color[] getBoatGroupsPieColors(Hashtable<UUID, Color> groupColors) {
    
	    // Colors for Groups
	    ArrayList<Color> aColors = new ArrayList<Color>();
        DataTypeList<UUID> grps = this.getAllowedGroupIdList();
        if (grps != null && grps.length() > 0) {
            for (int g=0; g<grps.length(); g++) {
                UUID id = grps.get(g);
                Color c = (groupColors!=null ? groupColors.get(id) : getGroupColor(id));
                if (c != null) {
                    aColors.add(c);
                }
            }
        }
	    return  (aColors.size() > 0 ? aColors.toArray(new Color[0]) : null);
    }
	    
    /**
     * Returns the color of a certain Group
     * @param groupID UUID of the group
     * @return Color of the group (may be null), or null if the specified group does not exist. 
     */
    private Color getGroupColor(UUID groupID) {
    	
    	Groups myGroups= Daten.project.getGroups(false);
    	GroupRecord myRecord = myGroups.findGroupRecord(groupID, System.currentTimeMillis());
    	
    	return (myRecord != null ? EfaUtil.getColor(myRecord.getColor()):null);

    }
    
    public void saveGuiItems(Vector<IItemType> items) {
        BoatStatus boatStatus = getPersistence().getProject().getBoatStatus(false);
        BoatReservations boatReservations = getPersistence().getProject().getBoatReservations(false);
        BoatDamages boatDamages = getPersistence().getProject().getBoatDamages(false);

        Vector<IItemType> boatStatusItems = null; // changed BoatStatus items

        for(IItemType item : items) {
            String name = item.getName();
            String cat = item.getCategory();
            if (name.equals(GUIITEM_BOATTYPES) && item.isChanged()) {
                ItemTypeItemList list = (ItemTypeItemList)item;
                for (int i=0; i<list.deletedSize(); i++) {
                    IItemType[] typeItems = list.getDeletedItems(i);
                    int variant = EfaUtil.stringFindInt(typeItems[0].toString(), -1);
                    int idx = getVariantIndex(variant);
                    deleteTypeVariant(idx);
                }
                for (int i=0; i<list.size(); i++) {
                    IItemType[] typeItems = list.getItems(i);
                    int variant = EfaUtil.stringFindInt(typeItems[0].toString(), -1);
                    if (variant < 0) {
                        continue;
                    }
                    if (variant == 0) {
                        addTypeVariant(typeItems[1].toString(), typeItems[2].toString(), typeItems[3].toString(), 
                                typeItems[4].toString(), typeItems[5].toString(), typeItems[6].toString());
                    } else {
                        int idx = getVariantIndex(variant);
                        if (typeItems[1].isChanged() || typeItems[2].isChanged() || typeItems[3].isChanged() || 
                                typeItems[4].isChanged() || typeItems[5].isChanged() || typeItems[6].isChanged()) {
                            setTypeVariant(idx, typeItems[1].toString(), typeItems[2].toString(), typeItems[3].toString(), 
                                    typeItems[4].toString(), typeItems[5].toString(), typeItems[6].toString());
                        }
                    }
                }
            }
            if (name.equals(GUIITEM_ALLOWEDGROUPIDLIST) && item.isChanged()) {
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
                DataTypeList<UUID> agList = new DataTypeList<UUID>();
                for (String key : keyArr) {
                    agList.add(uuidList.get(key));
                }
                setAllowedGroupIdList(agList);
            }
            if (cat.equals(BoatStatusRecord.CAT_STATUS) && item.isChanged()) {
                if (boatStatusItems == null) {
                    boatStatusItems = new Vector<IItemType>();
                }
                boatStatusItems.add(item);
            }
        }
        if (boatStatus != null && boatStatusItems != null) {
            BoatStatusRecord boatStatusRecord = boatStatus.getBoatStatus(getId());
            if (boatStatusRecord != null) {
                boatStatusRecord.saveGuiItems(boatStatusItems);
                try {
                    boatStatus.data().update(boatStatusRecord);
                } catch(Exception estatus) {
                    Logger.logdebug(estatus);
                }
            }
        }
        super.saveGuiItems(items);
    }

    public void itemListenerActionTable(int actionId, DataRecord[] records) {
        // nothing to do
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = false;
        if (persistence != null && persistence instanceof BoatReservations) {
            if (record == null) {
                BoatReservations boatReservations = (BoatReservations)persistence;
                record = boatReservations.createBoatReservationsRecord(getId());
                newRecord = true;
            }
            if (record == null) {
                return null;
            }
            try {
                return new BoatReservationEditDialog(parent, (BoatReservationRecord) record, newRecord, true, null);
            } catch(Exception e) {
                Dialog.error(e.getMessage());
                return null;
            }
        }

        if (persistence != null && persistence instanceof BoatDamages) {
            if (record == null) {
                BoatDamages boatDamages = (BoatDamages)persistence;
                record = boatDamages.createBoatDamageRecord(getId());
                newRecord = true;
            }
            if (record == null) {
                return null;
            }
            return new BoatDamageEditDialog(parent, (BoatDamageRecord) record, newRecord, null);
        }

        return null;
    }

    /*
     * This is the IItemListenerDataRecordTable method for deleting records.
     * For boat damages there should be a confirmation dialog if the damage should be set as fixed
     * instead of being deleted. This mimics the same behaviour as the "delete" function
     * in BoatDamageListDialog.
     */
    public boolean deleteCallback(JDialog parent,IItemListenerDataRecordTable caller, AdminRecord admin, DataRecord[] records) {
    	boolean onlyDamages=true;
    	for (int i=0; i<records.length; i++) {
    		if (! (records[i] instanceof BoatDamageRecord)) {
    			onlyDamages=false;
    			break;
    		}
    	}
    	if (onlyDamages) {
    		return BoatDamageRecord.deleteCallbackForGUIs(parent, caller, admin, records[0].getPersistence(), records);
    	} else {
            return true;
    	}
    }
    
	/**
	 * Adds a header item in an efa GUI. This header value is not safed within
	 * efaConfig. There is no word-wrap for the caption.
	 * 
	 * The header automatically gets a blue background and white text color; this
	 * cannot be configured as efaConfig cannot refer to its own settings whenn
	 * calling the constructor.
	 * 
	 * @param uniqueName Unique name of the element (as for all of efaConfig
	 *                   elements need unique names)
	 * @param type       TYPE_PUBLIC, TYPE_EXPERT, TYPE_INTERNAL
	 * @param category   Category in which the header is placed
	 * @param caption    Caption
	 * @param gridWidth  How many GridBagLayout cells shall this header be placed
	 *                   in?
	 */
	private IItemType createHeader(String uniqueName, int type, String category, String caption, int gridWidth) {
		// ensure that the header value does not get saved in efaConfig file by adding a
		// special prefix
		IItemType item = new ItemTypeLabelHeader("_" + uniqueName, type, category, " " + caption);
		item.setPadding(0, 0, 10, 10);
		item.setFieldGrid(3, GridBagConstraints.EAST, GridBagConstraints.BOTH);
		return item;
	}
    
}
