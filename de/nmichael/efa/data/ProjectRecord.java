/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.data;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.efawett.Zielfahrt;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypePasswordCrypted;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.BaseTabbedDialog;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import java.awt.*;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Pattern;

// @i18n complete
public class ProjectRecord extends DataRecord  {

    public static final String TYPE_PROJECT = "Project";
    public static final String TYPE_CLUB = "Club";
    public static final String TYPE_BOATHOUSE = "Boathouse";
    public static final String TYPE_LOGBOOK = "Logbook";
    public static final String TYPE_CLUBWORK = "ClubworkBook";
    //public static final String TYPE_CONFIG     = "Config";
    public static final String TYPE = "Type"; // one of TYPE_XXX constants
    public static final String PROJECTNAME = "ProjectName";
    public static final String PROJECT_ID = "ProjectID";
    public static final String DEPRECATED_LOGBOOKNAME = "LogbookName";
    public static final String NAME = "Name";
    public static final String DESCRIPTION = "Description";
    public static final int GUIITEMS_SUBTYPE_ALL = 0;
    public static final int GUIITEMS_SUBTYPE_KANUEFB = 100;
    public static final int GUIITEMS_SUBTYPE_EFAWETT = 101;
    public static final String GUIITEM_BOATHOUSE_SETDEFAULT = "GUIITEM_BOATHOUSE_SETDEFAULT";
    public static final String GUIITEM_BOATHOUSE_ADD = "GUIITEM_BOATHOUSE_ADD";
    public static final String GUIITEM_BOATHOUSE_DELETE = "GUIITEM_BOATHOUSE_DELETE";
    // Fields for Type=Project
    // PROJECTNAME
    // DESCRIPTION
    public static final String STORAGETYPE = "StorageType";
    public static final String STORAGELOCATION = "StorageLocation";
    public static final String STORAGEUSERNAME = "StorageUsername";
    public static final String STORAGEPASSWORD = "StoragePassword";
    public static final String EFACLOUDURL = "EfaCloudURL";
    public static final String REMOTEPROJECTNAME = "RemoteProjectName";
    public static final String EFAONLINECONNECT = "EfaOnlineConnect";
    public static final String EFAONLINEUSERNAME = "EfaOnlineUsername";
    public static final String EFAONLINEPASSWORD = "EfaOnlinePassword";
    public static final String ADMINNAME = "AdminName";
    public static final String ADMINEMAIL = "AdminEmail";
    public static final String LASTLOGBOOKSWITCH = "LastLogbookSwitch";
    public static final String LASTWATERSTMPLHASH = "LastWatersTemplateHash";
    // Fields for Type=Club
    public static final String CLUBNAME = "ClubName";
    public static final String ADDRESSADDITIONAL = "AddressAdditional";
    public static final String ADDRESSSTREET = "AddressStreet";
    public static final String ADDRESSCITY = "AddressCity";
    public static final String ASSOCIATIONGLOBALNAME = "GlobalAssociationName";
    public static final String ASSOCIATIONGLOBALMEMBERNO = "GlobalAssociationMemberNo";
    public static final String ASSOCIATIONGLOBALLOGIN = "GlobalAssociationLogin";
    public static final String ASSOCIATIONREGIONALNAME = "RegionalAssociationName";
    public static final String ASSOCIATIONREGIONALMEMBERNO = "RegionalAssociationMemberNo";
    public static final String ASSOCIATIONREGIONALLOGIN = "RegionalAssociationLogin";
    public static final String MEMBEROFDRV = "MemberOfDRV";
    public static final String MEMBEROFSRV = "MemberOfSRV";
    public static final String MEMBEROFADH = "MemberOfADH";
    public static final String KANUEFBUSERNAME = "KanuEfbUsername";
    public static final String KANUEFBPASSWORD = "KanuEfbPassword";
    public static final String KANUEFBLASTSYNC = "KanuEfbLastSync";
    public static final String LAST_DRV_FA_YEAR = "LastDrvFaYear";
    public static final String LAST_DRV_WS_YEAR = "LastDrvWsYear";
    // Fields for Type=Boathouse
    // BOATHOUSENAME (StorageObject Name)
    // DESCRIPTION
    public static final String BOATHOUSEID = "BoathouseId";
    public static final String BOATHOUSE_IDENTIFIER = "BoathouseIdentifier";
    public static final String CURRENTLOGBOOKEFABASE = "CurrentLogbookEfaBase"; // previous ProjectRecord
    public static final String CURRENTLOGBOOKEFABOATHOUSE = "CurrentLogbookEfaBoathouse"; // previous ProjectRecord
    public static final String AREAID = "AreaID"; // previous ClubRecord
    public static final String AUTONEWLOGBOOKDATE = "AutoNewLogbookDate"; // previous ConfigRecord
    public static final String AUTONEWLOGBOOKNAME = "AutoNewLogbookName"; // previous ConfigRecord
    public static final String CURRENTCLUBWORKEFABASE = "CurrentClubworkEfaBase";			// previous ProjectRecord
    public static final String CURRENTCLUBWORKEFABOATHOUSE = "CurrentClubworkEfaBoathouse";	// previous ProjectRecord
    public static final String AUTONEWCLUBWORKDATE = "AutoNewClubworkDate"; // previous ConfigRecord
    public static final String AUTONEWCLUBWORKNAME = "AutoNewClubworkName"; // previous ConfigRecord
    // Fields for Type=Logbook
    // LOGBOOKNAME (StorageObject Name)
    // DESCRIPTION
    public static final String STARTDATE = "StartDate";
    public static final String ENDDATE = "EndDate";
    // Fields for Type=ClubworkBook
    // CLUBWORKNAME (StorageObject Name)
    // DESCRIPTION
    // STARTDATE
    // ENDDATE
    public static final String DEFAULTCLUBWORKTARGETHOURS = "DefaultClubworkTargetHours";
    public static final String TRANSFERABLECLUBWORKHOURS = "TransferableClubworkHours";
    public static final String FINEFORTOOLITTLECLUBWORK = "FineForTooLittleClubwork";
    public static final String CLUBWORKCARRYOVERDONE = "ClubworkCarryOverDone";

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(TYPE);
        t.add(IDataAccess.DATA_STRING);
        f.add(PROJECTNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(PROJECT_ID);
        t.add(IDataAccess.DATA_UUID);
        f.add(DEPRECATED_LOGBOOKNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(NAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(DESCRIPTION);
        t.add(IDataAccess.DATA_STRING);
        f.add(STORAGETYPE);
        t.add(IDataAccess.DATA_STRING);
        f.add(STORAGELOCATION);
        t.add(IDataAccess.DATA_STRING);
        f.add(STORAGEUSERNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(STORAGEPASSWORD);
        t.add(IDataAccess.DATA_PASSWORDC);
        f.add(EFACLOUDURL);
        t.add(IDataAccess.DATA_STRING);
        f.add(REMOTEPROJECTNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(EFAONLINECONNECT);
        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EFAONLINEUSERNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(EFAONLINEPASSWORD);
        t.add(IDataAccess.DATA_PASSWORDC);
        f.add(ADMINNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(ADMINEMAIL);
        t.add(IDataAccess.DATA_STRING);
        f.add(LASTLOGBOOKSWITCH);
        t.add(IDataAccess.DATA_STRING);
        f.add(LASTWATERSTMPLHASH);
        t.add(IDataAccess.DATA_STRING);
        f.add(CURRENTLOGBOOKEFABASE);
        t.add(IDataAccess.DATA_STRING);
        f.add(CURRENTLOGBOOKEFABOATHOUSE);
        t.add(IDataAccess.DATA_STRING);
        f.add(AUTONEWLOGBOOKDATE);
        t.add(IDataAccess.DATA_DATE);
        f.add(AUTONEWLOGBOOKNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(AUTONEWCLUBWORKDATE);
        t.add(IDataAccess.DATA_DATE);
        f.add(AUTONEWCLUBWORKNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(CLUBNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(ADDRESSADDITIONAL);
        t.add(IDataAccess.DATA_STRING);
        f.add(ADDRESSSTREET);
        t.add(IDataAccess.DATA_STRING);
        f.add(ADDRESSCITY);
        t.add(IDataAccess.DATA_STRING);
        f.add(ASSOCIATIONGLOBALNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(ASSOCIATIONGLOBALMEMBERNO);
        t.add(IDataAccess.DATA_STRING);
        f.add(ASSOCIATIONGLOBALLOGIN);
        t.add(IDataAccess.DATA_STRING);
        f.add(ASSOCIATIONREGIONALNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(ASSOCIATIONREGIONALMEMBERNO);
        t.add(IDataAccess.DATA_STRING);
        f.add(ASSOCIATIONREGIONALLOGIN);
        t.add(IDataAccess.DATA_STRING);
        f.add(MEMBEROFDRV);
        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(MEMBEROFSRV);
        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(MEMBEROFADH);
        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(AREAID);
        t.add(IDataAccess.DATA_INTEGER);
        f.add(KANUEFBUSERNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(KANUEFBPASSWORD);
        t.add(IDataAccess.DATA_PASSWORDC);
        f.add(KANUEFBLASTSYNC);
        t.add(IDataAccess.DATA_LONGINT);
        f.add(STARTDATE);
        t.add(IDataAccess.DATA_DATE);
        f.add(ENDDATE);
        t.add(IDataAccess.DATA_DATE);
        f.add(DEFAULTCLUBWORKTARGETHOURS);
        t.add(IDataAccess.DATA_DOUBLE);
        f.add(TRANSFERABLECLUBWORKHOURS);
        t.add(IDataAccess.DATA_DOUBLE);
        f.add(FINEFORTOOLITTLECLUBWORK);
        t.add(IDataAccess.DATA_DOUBLE);
        f.add(LAST_DRV_FA_YEAR);
        t.add(IDataAccess.DATA_INTEGER);
        f.add(LAST_DRV_WS_YEAR);
        t.add(IDataAccess.DATA_INTEGER);
        f.add(BOATHOUSEID);
        t.add(IDataAccess.DATA_INTEGER);
        f.add(BOATHOUSE_IDENTIFIER);
        t.add(IDataAccess.DATA_STRING);
        f.add(CURRENTCLUBWORKEFABASE);
        t.add(IDataAccess.DATA_STRING);
        f.add(CURRENTCLUBWORKEFABOATHOUSE);
        t.add(IDataAccess.DATA_STRING);
        f.add(CLUBWORKCARRYOVERDONE);
        t.add(IDataAccess.DATA_BOOLEAN);

        MetaData metaData = constructMetaData(Project.DATATYPE, f, t, false);
        metaData.setKey(new String[]{TYPE, NAME});
    }

    public ProjectRecord(Project project, MetaData metaData) {
        super(project, metaData);
    }

    public ProjectRecord(Project project, MetaData metaData, String type) {
        super(project, metaData);
        setType(type);

        // initialize with default values
        if (type.equals(TYPE_PROJECT)) {
            setStorageType(IDataAccess.TYPE_FILE_XML);
        }

        if (type.equals(TYPE_CLUB)) {
            if (Daten.efaConfig.getValueUseFunctionalityRowingGermany()) {
                setGlobalAssociationName(International.onlyFor("Deutscher Ruderverband", "de"));
                setRegionalAssociationName(International.onlyFor("Landesruderverband Berlin", "de"));
                setMemberOfDRV(true);
            }
            if (Daten.efaConfig.getValueUseFunctionalityCanoeing()) {
                setGlobalAssociationName(International.onlyFor("Deutscher Kanuverband", "de"));
                setRegionalAssociationName(International.onlyFor("Landes-Kanu-Verband Berlin", "de"));
            }
        }
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public static DataKey getDataKey(String type, String name) {
        return new DataKey<String, String, String>(type, name, null);
    }

    public DataKey getKey() {
        return new DataKey<String, String, String>(getType(), getName(), null);
    }

    public void setType(String type) {
        setString(TYPE, type);
    }

    public void setProjectName(String projectName) {
        setString(PROJECTNAME, projectName);
    }

    void setProjectId(UUID id) {
        setUUID(PROJECT_ID, id);
    }

    public void setDescription(String description) {
        setString(DESCRIPTION, description);
    }

    public void setStorageType(int storageType) {
        switch (storageType) {
            case IDataAccess.TYPE_FILE_XML:
                setString(STORAGETYPE, IDataAccess.TYPESTRING_FILE_XML);
                break;
            case IDataAccess.TYPE_EFA_REMOTE:
                setString(STORAGETYPE, IDataAccess.TYPESTRING_EFA_REMOTE);
                break;
            case IDataAccess.TYPE_EFA_CLOUD:
                setString(STORAGETYPE, IDataAccess.TYPESTRING_EFA_CLOUD);
                break;
        }
    }

    public void setStorageLocation(String storageLocation) {
        setString(STORAGELOCATION, storageLocation);
    }

    public String getEfaCloudURL() { 
    	return getString(EFACLOUDURL); 
    }
    public void setEfaCloudURL(String efaCloudURL) {
        setString(EFACLOUDURL, efaCloudURL);
    }

    public void setStorageUsername(String username) {
        setString(STORAGEUSERNAME, username);
    }

    public void setStoragePassword(String password) {
        setPasswordCrypted(STORAGEPASSWORD, password);
    }

    public void setRemoteProjectName(String projectName) {
        setString(REMOTEPROJECTNAME, projectName);
    }

    public void setEfaOnlineConnect(boolean connectThroughEfaOnline) {
        setBool(EFAONLINECONNECT, connectThroughEfaOnline);
    }

    public void setEfaOnlineUsername(String username) {
        setString(EFAONLINEUSERNAME, username);
    }

    public void setEfaOnlinePassword(String password) {
        setPasswordCrypted(EFAONLINEPASSWORD, password);
    }

    public void setAdminName(String adminName) {
        setString(ADMINNAME, adminName);
    }

    public void setAdminEmail(String adminEmail) {
        setString(ADMINEMAIL, adminEmail);
    }

    public void setLastLogbookSwitch(String key) {
        setString(LASTLOGBOOKSWITCH, key);
    }

    public void setLastWatersTamplateHash(String key) {
        setString(LASTWATERSTMPLHASH, key);
    }

    public void setCurrentLogbookEfaBase(String currentLogbook) {
        setString(CURRENTLOGBOOKEFABASE, currentLogbook);
    }

    public void setCurrentLogbookEfaBoathouse(String currentLogbook) {
        setString(CURRENTLOGBOOKEFABOATHOUSE, currentLogbook);
    }

    public void setAutoNewLogbookDate(DataTypeDate date) {
        setDate(AUTONEWLOGBOOKDATE, date);
    }

    public void setAutoNewClubworkDate(DataTypeDate date) {
        setDate(AUTONEWCLUBWORKDATE, date);
    }

    public void setAutoNewLogbookName(String name) {
        setString(AUTONEWLOGBOOKNAME, name);
    }

    public void setAutoNewClubworkName(String name) {
        setString(AUTONEWCLUBWORKNAME, name);
    }

    public void setClubName(String clubName) {
        setString(CLUBNAME, clubName);
    }

    public void setAddressAdditional(String addressAdditional) {
        setString(ADDRESSADDITIONAL, addressAdditional);
    }

    public void setAddressStreet(String addressStreet) {
        setString(ADDRESSSTREET, addressStreet);
    }

    public void setAddressCity(String addressCity) {
        setString(ADDRESSCITY, addressCity);
    }

    public void setRegionalAssociationName(String name) {
        setString(ASSOCIATIONREGIONALNAME, name);
    }

    public void setRegionalAssociationMemberNo(String memberNo) {
        setString(ASSOCIATIONREGIONALMEMBERNO, memberNo);
    }

    public void setRegionalAssociationLogin(String login) {
        setString(ASSOCIATIONREGIONALLOGIN, login);
    }

    public void setGlobalAssociationName(String name) {
        setString(ASSOCIATIONGLOBALNAME, name);
    }

    public void setGlobalAssociationMemberNo(String memberNo) {
        setString(ASSOCIATIONGLOBALMEMBERNO, memberNo);
    }

    public void setGlobalAssociationLogin(String login) {
        setString(ASSOCIATIONGLOBALLOGIN, login);
    }

    public void setMemberOfDRV(boolean member) {
        setBool(MEMBEROFDRV, member);
    }

    public void setMemberOfSRV(boolean member) {
        setBool(MEMBEROFSRV, member);
    }

    public void setMemberOfADH(boolean member) {
        setBool(MEMBEROFADH, member);
    }

    public void setAreaId(int areaId) {
        setInt(AREAID, areaId);
    }

    public void setKanuEfbUsername(String username) {
        setString(KANUEFBUSERNAME, username);
    }

    public void setKanuEfbPassword(String password) {
        setPasswordCrypted(KANUEFBPASSWORD, password);
    }

    public void setKanuEfbLastSync(long lastSync) {
        setLong(KANUEFBLASTSYNC, lastSync);
    }

    public void setStartDate(DataTypeDate startDate) {
        setDate(STARTDATE, startDate);
    }

    public void setEndDate(DataTypeDate endDate) {
        setDate(ENDDATE, endDate);
    }

    public void setDefaultClubworkTargetHours(double defaultHours) {
        setDouble(DEFAULTCLUBWORKTARGETHOURS, defaultHours);
    }

    public void setTransferableClubworkHours(double hours) {
        setDouble(TRANSFERABLECLUBWORKHOURS, hours);
    }

    public void setFineForTooLittleClubwork(double monetaryUnits) {
        setDouble(FINEFORTOOLITTLECLUBWORK, monetaryUnits);
    }

    public boolean getClubworkCarryOverDone() {
        return getBool(CLUBWORKCARRYOVERDONE);
    }
    public void setClubworkCarryOverDone(boolean done) {
        setBool(CLUBWORKCARRYOVERDONE, done);
    }

    public void setLastDrvFaYear(int year) {
        setInt(LAST_DRV_FA_YEAR, year);
    }

    public void setLastDrvWsYear(int year) {
        setInt(LAST_DRV_WS_YEAR, year);
    }

    public void setName(String name) {
        setString(NAME, name);
    }

    public void setBoathouseId(int id) {
        setInt(BOATHOUSEID, id);
    }

    public void setBoathouseIdentifier(String identifier) {
        setString(BOATHOUSE_IDENTIFIER, identifier);
    }

    public void setCurrentClubworkEfaBase(String currentClubwork) {
        setString(CURRENTCLUBWORKEFABASE, currentClubwork);
    }

    public void setCurrentClubworkEfaBoathouse(String currentClubwork) {
        setString(CURRENTCLUBWORKEFABOATHOUSE, currentClubwork);
    }

    public String getType() {
        return getString(TYPE);
    }

    public String getName() {
        return getString(NAME);
    }

    public String getProjectName() {
        return getString(PROJECTNAME);
    }

    public UUID getProjectId() {
        return getUUID(PROJECT_ID);
    }

    public String getDescription() {
        return getString(DESCRIPTION);
    }

    public int getStorageType() {
        String s = getString(STORAGETYPE);
        if (s != null && s.equals(IDataAccess.TYPESTRING_FILE_XML)) {
            return IDataAccess.TYPE_FILE_XML;
        }
        if (s != null && s.equals(IDataAccess.TYPESTRING_EFA_REMOTE)) {
            return IDataAccess.TYPE_EFA_REMOTE;
        }
        if (s != null && s.equals(IDataAccess.TYPESTRING_EFA_CLOUD)) {
            return IDataAccess.TYPE_EFA_CLOUD;
        }
        return -1;
    }

    public String getStorageTypeTypeString() {
        return getString(STORAGETYPE);
    }

    public String getStorageLocation() {
        try {
            if (getStorageType() == IDataAccess.TYPE_FILE_XML || getStorageType() == IDataAccess.TYPE_EFA_CLOUD) {
                // for file-based projects: storageLocation of content is always relative to this project file!
                return getPersistence().data().getStorageLocation() + getProjectName() + Daten.fileSep;
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return getString(STORAGELOCATION);
    }

    public String getStorageUsername() {
        return getString(STORAGEUSERNAME);
    }

    public String getStoragePassword() {
        DataTypePasswordCrypted pwd = getPasswordCrypted(STORAGEPASSWORD);
        if (pwd != null && pwd.isSet()) {
            return pwd.getPassword();
        } else {
            return "";
        }
    }

    public String getRemoteProjectName() {
        return getString(REMOTEPROJECTNAME);
    }

    public boolean getEfaOnlineConnect() {
        return getBool(EFAONLINECONNECT);
    }

    public String getEfaOnlineUsername() {
        return getString(EFAONLINEUSERNAME);
    }

    public String getEfaOnlinePassword() {
        DataTypePasswordCrypted pwd = getPasswordCrypted(EFAONLINEPASSWORD);
        if (pwd != null && pwd.isSet()) {
            return pwd.getPassword();
        } else {
            return "";
        }
    }

    public String getAdminName() {
        return getString(ADMINNAME);
    }

    public String getAdminEmail() {
        return getString(ADMINEMAIL);
    }

    public String getLastLogbookSwitch() {
        return getString(LASTLOGBOOKSWITCH);
    }

    public String getLastWatersTemplateHash() {
        return getString(LASTWATERSTMPLHASH);
    }

    public String getCurrentLogbookEfaBase() {
        return getString(CURRENTLOGBOOKEFABASE);
    }

    public String getCurrentLogbookEfaBoathouse() {
        return getString(CURRENTLOGBOOKEFABOATHOUSE);
    }

    public DataTypeDate getAutoNewLogbookDate() {
        return getDate(AUTONEWLOGBOOKDATE);
    }

    public DataTypeDate getAutoNewClubworkDate() {
        return getDate(AUTONEWCLUBWORKDATE);
    }

    public String getAutoNewLogbookName() {
        return getString(AUTONEWLOGBOOKNAME);
    }

    public String getAutoNewClubworkName() {
        return getString(AUTONEWCLUBWORKNAME);
    }

    public String getClubName() {
        return getString(CLUBNAME);
    }

    public String getAddressAdditional() {
        return getString(ADDRESSADDITIONAL);
    }

    public String getAddressStreet() {
        return getString(ADDRESSSTREET);
    }

    public String getAddressCity() {
        return getString(ADDRESSCITY);
    }

    public String getRegionalAssociationName() {
        return getString(ASSOCIATIONREGIONALNAME);
    }

    public String getRegionalAssociationMemberNo() {
        return getString(ASSOCIATIONREGIONALMEMBERNO);
    }

    public String getRegionalAssociationLogin() {
        return getString(ASSOCIATIONREGIONALLOGIN);
    }

    public String getGlobalAssociationName() {
        return getString(ASSOCIATIONGLOBALNAME);
    }

    public String getGlobalAssociationMemberNo() {
        return getString(ASSOCIATIONGLOBALMEMBERNO);
    }

    public String getGlobalAssociationLogin() {
        return getString(ASSOCIATIONGLOBALLOGIN);
    }

    public boolean getMemberOfDRV() {
        return getBool(MEMBEROFDRV);
    }

    public boolean getMemberOfSRV() {
        return getBool(MEMBEROFSRV);
    }

    public boolean getMemberOfADH() {
        return getBool(MEMBEROFADH);
    }

    public int getAreaId() {
        return getInt(AREAID);
    }

    public String getKanuEfbUsername() {
        return getString(KANUEFBUSERNAME);
    }

    public String getKanuEfbPassword() {
        DataTypePasswordCrypted pwd = getPasswordCrypted(KANUEFBPASSWORD);
        if (pwd != null && pwd.isSet()) {
            return pwd.getPassword();
        } else {
            return "";
        }
    }

    public long getKanuEfbLastSync() {
        return getLong(KANUEFBLASTSYNC);
    }

    public DataTypeDate getStartDate() {
        return getDate(STARTDATE);
    }

    public DataTypeDate getEndDate() {
        return getDate(ENDDATE);
    }

    /**
     * gives the amount of hours to work in a period (which can be a year or only for some month)
     * @return
     */
    public double getDefaultClubworkTargetHours() {
        return getDouble(DEFAULTCLUBWORKTARGETHOURS);
    }

    public double getDefaultMonthlyClubworkTargetHours() {
        return this.getDefaultClubworkTargetHours() / this.getStartDate().getMonthsDifference(this.getEndDate());
    }

    public double getTransferableClubworkHours() {
        return getDouble(TRANSFERABLECLUBWORKHOURS);
    }

    public double getFineForTooLittleClubwork() {
        return getDouble(FINEFORTOOLITTLECLUBWORK);
    }

    public String getCurrentClubworkEfaBase() {
        return getString(CURRENTCLUBWORKEFABASE);
    }

    public String getCurrentClubworkEfaBoathouse() {
        return getString(CURRENTCLUBWORKEFABOATHOUSE);
    }

    public int getLastDrvFaYear() {
        return getInt(LAST_DRV_FA_YEAR);
    }

    public int getLastDrvWsYear() {
        return getInt(LAST_DRV_WS_YEAR);
    }

    public String getBoathouseIdentifier() {
        return getString(BOATHOUSE_IDENTIFIER);
    }

    public int getBoathouseId() {
        return getInt(BOATHOUSEID);
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        return getGuiItems(admin, 0, null, false);
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin, int subtype, String usecategory, boolean newProject) {
        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();

        String category = usecategory;
        if (getType().equals(TYPE_PROJECT)) {
            if (usecategory == null) {
                category = "%01%" + International.getString("Projekt");
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 1) {
                v.add(item = new ItemTypeString(ProjectRecord.PROJECTNAME, getProjectName(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Name des Projekts")));
                ((ItemTypeString) item).setAllowedCharacters("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
                ((ItemTypeString) item).setReplacementCharacter('_');
                ((ItemTypeString) item).setNotNull(true);
                ((ItemTypeString) item).setEditable(newProject);

                v.add(item = new ItemTypeString(ProjectRecord.DESCRIPTION, getDescription(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Beschreibung")));

                v.add(item = new ItemTypeString(ProjectRecord.ADMINNAME, getAdminName(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Dein Name")));

                v.add(item = new ItemTypeString(ProjectRecord.ADMINEMAIL, getAdminEmail(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Deine email-Adresse")));
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 2) {
                v.add(item = new ItemTypeStringList(ProjectRecord.STORAGETYPE, getStorageTypeTypeString(),
                        ProjectRecord.getStorageTypeTypeStrings(),
                        ProjectRecord.getStorageTypeNameStrings(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Speichertyp")));
                ((ItemTypeStringList) item).setEnabled(newProject);
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 3) {

                if (!newProject ||
                     (getStorageType() != IDataAccess.TYPE_FILE_XML && getStorageType() != IDataAccess.TYPE_EFA_CLOUD)) {
                    v.add(item = new ItemTypeString(ProjectRecord.STORAGELOCATION, getStorageLocation(),
                            IItemType.TYPE_PUBLIC, category,
                            (getStorageType() == IDataAccess.TYPE_EFA_REMOTE
                                    ? International.getString("IP-Adresse") + " ("
                                    + International.getString("remote") + ")"
                                    : International.getString("Speicherort"))));
                    ((ItemTypeString) item).setEnabled(getStorageType() != IDataAccess.TYPE_FILE_XML &&
                            getStorageType() != IDataAccess.TYPE_EFA_CLOUD);
                }

                if (getStorageType() != IDataAccess.TYPE_FILE_XML) {
                	
                    if (getStorageType() == IDataAccess.TYPE_EFA_CLOUD) {
	                	v.add(item = EfaGuiUtils.createHintWordWrap(ProjectRecord.STORAGEUSERNAME+"_HINT", IItemType.TYPE_PUBLIC, category, 
	                			International.getString("Jede efa-Installation in einem efaCloud-System benötigt einen eigenen Benutzernamen (efaCloud-UserID). Legen Sie in efaCloud ggfs. einen neuen Benutzer an, und tragen sie seine numerische UserID hier ein. Weitere Informationen finden Sie im efa-Dokuwiki."), 
	                			3, 10, 10,630));
                    }
                	
                    v.add(item = new ItemTypeString(ProjectRecord.STORAGEUSERNAME, getStorageUsername(),
                            IItemType.TYPE_PUBLIC, category,
                            (getStorageType() == IDataAccess.TYPE_EFA_REMOTE
                                    ? International.getString("Admin-Name") + " ("
                                    + International.getString("remote") + ")"
                                    : International.getString("Benutzername"))));
                    ((ItemTypeString) item).setNotNull(true);
                    v.add(item = new ItemTypePassword(ProjectRecord.STORAGEPASSWORD, getStoragePassword(), true,
                            IItemType.TYPE_PUBLIC, category,
                            (getStorageType() == IDataAccess.TYPE_EFA_REMOTE
                                    ? International.getString("Paßwort") + " ("
                                    + International.getString("remote") + ")"
                                    : International.getString("Paßwort"))));
                    if (getStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                        v.add(item = new ItemTypeString(ProjectRecord.REMOTEPROJECTNAME, getRemoteProjectName(),
                                IItemType.TYPE_PUBLIC, category,
                                International.getString("Name des Projekts") + " ("
                                + International.getString("remote") + ")"));
                        ((ItemTypeString) item).setNotNull(true);
                    }
                    if (getStorageType() == IDataAccess.TYPE_EFA_CLOUD) {
                        v.add(item = new ItemTypeString(ProjectRecord.EFACLOUDURL, getEfaCloudURL(),
                                IItemType.TYPE_PUBLIC, category,
                                International.getString("URL des efaCloud Servers")));
                        ((ItemTypeString) item).setNotNull(true);
                    }
                }

                if (getStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                    v.add(item = new ItemTypeBoolean(ProjectRecord.EFAONLINECONNECT, getEfaOnlineConnect(),
                            IItemType.TYPE_PUBLIC, category,
                            International.getString("über efaOnline verbinden")));
                    v.add(item = new ItemTypeString(ProjectRecord.EFAONLINEUSERNAME, getEfaOnlineUsername(),
                            IItemType.TYPE_PUBLIC, category,
                            Daten.EFA_ONLINE + " - "
                                    + International.getString("Benutzername")));
                    v.add(item = new ItemTypePassword(ProjectRecord.EFAONLINEPASSWORD, getEfaOnlinePassword(), true,
                            IItemType.TYPE_PUBLIC, category,
                            Daten.EFA_ONLINE + " - "
                                    + International.getString("Paßwort")));
                }
            }
        }

        if (getType().equals(TYPE_CLUB)) {

            if (usecategory == null) {
                category = "%02A%" + International.getString("Club");
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 1 || subtype == GUIITEMS_SUBTYPE_EFAWETT) {
                v.add(item = new ItemTypeString(ProjectRecord.CLUBNAME, getClubName(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Vereinsname")));
                v.add(item = new ItemTypeString(ProjectRecord.ADDRESSADDITIONAL, getAddressAdditional(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Adresszusatz")));
                v.add(item = new ItemTypeString(ProjectRecord.ADDRESSSTREET, getAddressStreet(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Anschrift") + " - "
                                + International.getString("Straße")));
                v.add(item = new ItemTypeString(ProjectRecord.ADDRESSCITY, getAddressCity(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Anschrift") + " - "
                                + International.getString("Postleitzahl und Ort")));
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 2 || subtype == GUIITEMS_SUBTYPE_EFAWETT) {
                if (usecategory == null) {
                    category = "%02C%" + International.getString("Verbände");
                }
                if (subtype == GUIITEMS_SUBTYPE_EFAWETT) {
                    // this is a dirty trick! Actually, we never use the fields ADMINNAME and ADMINEMAIL
                    // in Club Records, only in ProjectRecords. In all other GUI's, these aren't visible
                    // for Club Records; but here we prompt the user to submit an Online Competition, so
                    // we need those. We simply store them in the Club Record, where they will remain
                    // invisible until the user sends in the next competition...
                    v.add(item = new ItemTypeString(ProjectRecord.ADMINNAME, getAdminName(),
                            IItemType.TYPE_PUBLIC, category,
                            International.getString("Dein Name")));
                    item.setNotNull(true);
                    if (getAdminName() == null || getAdminName().length() == 0) {
                        item.setChanged(); // enforcing the check
                    }
                    String email = getAdminEmail();
                    if (email != null && email.length() > 0 && !EfaUtil.isValidEmail(email)) {
                        email = null;
                    }
                    v.add(item = new ItemTypeString(ProjectRecord.ADMINEMAIL, email,
                            IItemType.TYPE_PUBLIC, category,
                            International.getString("Deine email-Adresse")));
                    item.setNotNull(true);
                    ((ItemTypeString)item).setAllowedRegexWarnIfWrong(Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE));
                    if (email == null || email.length() == 0) {
                        item.setChanged(); // enforcing the check
                    }
                }

                v.add(item = new ItemTypeString(ProjectRecord.ASSOCIATIONGLOBALNAME, getGlobalAssociationName(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Dachverband") + " - "
                                + International.getString("Name")));
                v.add(item = new ItemTypeString(ProjectRecord.ASSOCIATIONGLOBALMEMBERNO, getGlobalAssociationMemberNo(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Dachverband") + " - "
                                + International.getString("Mitgliedsnummer")));
                v.add(item = new ItemTypeString(ProjectRecord.ASSOCIATIONGLOBALLOGIN, getGlobalAssociationLogin(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Dachverband") + " - "
                                + International.getString("Benutzername")
                                + (Daten.efaConfig.getValueUseFunctionalityRowingGermany() ? " (efaWett)" : "")));
                v.add(item = new ItemTypeString(ProjectRecord.ASSOCIATIONREGIONALNAME, getRegionalAssociationName(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Regionalverband") + " - "
                                + International.getString("Name")));
                v.add(item = new ItemTypeString(ProjectRecord.ASSOCIATIONREGIONALMEMBERNO, getRegionalAssociationMemberNo(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Regionalverband") + " - "
                                + International.getString("Mitgliedsnummer")));
                v.add(item = new ItemTypeString(ProjectRecord.ASSOCIATIONREGIONALLOGIN, getRegionalAssociationLogin(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Regionalverband") + " - "
                                + International.getString("Benutzername")
                                + (Daten.efaConfig.getValueUseFunctionalityRowingGermany() ? " (efaWett)" : "")));
                if (Daten.efaConfig.getValueUseFunctionalityRowingGermany()) {
                    v.add(item = new ItemTypeBoolean(ProjectRecord.MEMBEROFDRV, getMemberOfDRV(),
                            IItemType.TYPE_PUBLIC, category,
                            International.onlyFor("Mitglied im Deutschen Ruderverband (DRV)", "de")));
                    v.add(item = new ItemTypeBoolean(ProjectRecord.MEMBEROFSRV, getMemberOfSRV(),
                            IItemType.TYPE_PUBLIC, category,
                            International.onlyFor("Mitglied in einem Schülerruderverband (SRV)", "de")));
                    v.add(item = new ItemTypeBoolean(ProjectRecord.MEMBEROFADH, getMemberOfADH(),
                            IItemType.TYPE_PUBLIC, category,
                            International.onlyFor("Mitglied im Allgemeinen Deutschen Hochschulsportverband (ADH)", "de")));
                }
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 2 || subtype == GUIITEMS_SUBTYPE_KANUEFB) {
                if (Daten.efaConfig.getValueUseFunctionalityCanoeingGermany() || subtype == GUIITEMS_SUBTYPE_KANUEFB) {
                    if (usecategory == null) {
                        category = "%02C%" + International.getString("Verbände");
                    }
                    v.add(item = new ItemTypeString(ProjectRecord.KANUEFBUSERNAME, getKanuEfbUsername(),
                            IItemType.TYPE_PUBLIC, category,
                            International.getString("Benutzername") + " (Kanu-eFB)"));
                    v.add(item = new ItemTypePassword(ProjectRecord.KANUEFBPASSWORD, getKanuEfbPassword(), true,
                            IItemType.TYPE_PUBLIC, category,
                            International.getString("Paßwort") + " (Kanu-eFB)"));
                    v.add(item = new ItemTypeLong(ProjectRecord.KANUEFBLASTSYNC, getKanuEfbLastSync(), 0, Long.MAX_VALUE,
                            IItemType.TYPE_EXPERT, category,
                            "Letzte Synchronisierung  (Kanu-eFB)"));
                }
            }
        }

        if (getType().equals(TYPE_BOATHOUSE)) {
            if (usecategory == null) {
                category = BaseTabbedDialog.makeCategory("%03%" + International.getString("Bootshaus"), getName());
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 1) {
                v.add(item = new ItemTypeString(ProjectRecord.NAME, getName(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Name des Bootshauses")));
                ((ItemTypeString) item).setNotNull(true);
                ((ItemTypeString) item).setNotAllowedCharacters("()");

                v.add(item = new ItemTypeString(ProjectRecord.DESCRIPTION, getDescription(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Beschreibung")));

                if (Daten.efaConfig.getValueUseFunctionalityRowingBerlin()) {
                    int areaId = getAreaId();
                    if (areaId < 1 || areaId > Zielfahrt.ANZ_ZIELBEREICHE) {
                        areaId = 1;
                    }
                    v.add(item = new ItemTypeInteger(ProjectRecord.AREAID, areaId,
                            1, Zielfahrt.ANZ_ZIELBEREICHE, true,
                            IItemType.TYPE_PUBLIC, category,
                            International.onlyFor("eigener Zielbereich", "de")));
                    item.setNotNull(true);
                }
                if (!newProject) {
                    v.add(item = new ItemTypeDate(ProjectRecord.AUTONEWLOGBOOKDATE, getAutoNewLogbookDate(),
                            IItemType.TYPE_EXPERT, category,
                            International.getString("Fahrtenbuchwechsel") + " - "
                                    + International.getString("Datum")));
                    v.add(item = new ItemTypeString(ProjectRecord.AUTONEWLOGBOOKNAME, getAutoNewLogbookName(),
                            IItemType.TYPE_EXPERT, category,
                            International.getString("Fahrtenbuchwechsel") + " - "
                                    + International.getString("Fahrtenbuch")));
                }

                v.add(item = new ItemTypeButton(GUIITEM_BOATHOUSE_ADD,
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Bootshaus hinzufügen")));
                ((ItemTypeButton) item).setIcon(BaseDialog.getIcon(BaseDialog.IMAGE_ADD));
                ((ItemTypeButton) item).setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
                ((ItemTypeButton) item).setPadding(0, 0, 40, 0);
                v.add(item = new ItemTypeButton(GUIITEM_BOATHOUSE_DELETE,
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Bootshaus entfernen")));
                ((ItemTypeButton) item).setIcon(BaseDialog.getIcon(BaseDialog.IMAGE_DELETE));
                ((ItemTypeButton) item).setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
                ((ItemTypeButton) item).setDataKey(getKey());
                if (((Project) getPersistence()).getNumberOfBoathouses() > 1) {
                    v.add(item = new ItemTypeString(ProjectRecord.BOATHOUSE_IDENTIFIER, getBoathouseIdentifier(),
                            IItemType.TYPE_PUBLIC, category,
                            International.getString("Standardcomputer für dieses Bootshaus")));
                    ((ItemTypeString) item).setPadding(0, 0, 20, 0);
                    v.add(item = new ItemTypeButton(GUIITEM_BOATHOUSE_SETDEFAULT,
                            IItemType.TYPE_PUBLIC, category,
                            International.getString("Diesen Computer auswählen")));
                    ((ItemTypeButton) item).setIcon(BaseDialog.getIcon(BaseDialog.IMAGE_SELECT));
                    ((ItemTypeButton) item).setFieldGrid(2, GridBagConstraints.EAST, GridBagConstraints.NONE);
                }

            }
        }

        if (getType().equals(TYPE_LOGBOOK)) {

            if (usecategory == null) {
                category = BaseTabbedDialog.makeCategory("%04%" + International.getString("Fahrtenbuch"), getName());
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 1) {
                v.add(item = new ItemTypeString(ProjectRecord.NAME, getName(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Name des Fahrtenbuchs")));
                ((ItemTypeString) item).setAllowedCharacters("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
                ((ItemTypeString) item).setReplacementCharacter('_');
                ((ItemTypeString) item).setNotNull(true);
                ((ItemTypeString) item).setEditable(newProject);

                v.add(item = new ItemTypeString(ProjectRecord.DESCRIPTION, getDescription(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Beschreibung")));
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 2) {
                // @todo (P4) allow to change time range of logbook (and make sure in ProjectEditDialog that all sessions fit into that range)
                v.add(item = new ItemTypeDate(ProjectRecord.STARTDATE, getStartDate(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Beginn des Zeitraums")));
                ((ItemTypeDate) item).setNotNull(true);
                ((ItemTypeDate) item).setEditable(newProject);
                v.add(item = new ItemTypeDate(ProjectRecord.ENDDATE, getEndDate(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Ende des Zeitraums")));
                ((ItemTypeDate) item).setNotNull(true);
                ((ItemTypeDate) item).setEditable(newProject);
            }
        }

        if (getType().equals(TYPE_CLUBWORK)) {
            if (usecategory == null) {
                category = "%02B%" + International.getString("Vereinsarbeit");
            }
            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 1) {
                v.add(item = new ItemTypeString(ProjectRecord.NAME, getName(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Name des Fahrtenbuchs")));
                ((ItemTypeString) item).setAllowedCharacters("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
                ((ItemTypeString) item).setReplacementCharacter('_');
                ((ItemTypeString) item).setNotNull(true);
                ((ItemTypeString) item).setEditable(newProject);

                v.add(item = new ItemTypeString(ProjectRecord.DESCRIPTION, getDescription(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Beschreibung")));
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 2) {
                // @todo (P4) allow to change time range of logbook (and make sure in ProjectEditDialog that all sessions fit into that range)
                v.add(item = new ItemTypeDate(ProjectRecord.STARTDATE, getStartDate(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Beginn des Zeitraums")));
                ((ItemTypeDate) item).setNotNull(true);
                ((ItemTypeDate) item).setEditable(newProject);
                v.add(item = new ItemTypeDate(ProjectRecord.ENDDATE, getEndDate(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Ende des Zeitraums")));
                ((ItemTypeDate) item).setNotNull(true);
                ((ItemTypeDate) item).setEditable(newProject);
            }

            if (subtype == GUIITEMS_SUBTYPE_ALL || subtype == 3) {
                v.add(item = new ItemTypeDouble(ProjectRecord.DEFAULTCLUBWORKTARGETHOURS, getDefaultClubworkTargetHours(), 0, ItemTypeDouble.MAX,
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Sollstunden für Vereinsarbeit")));

                v.add(item = new ItemTypeDouble(ProjectRecord.TRANSFERABLECLUBWORKHOURS, getTransferableClubworkHours(), 0, ItemTypeDouble.MAX,
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Übertragbare Vereinsarbeitsstunden pro Zeitraum")));

                v.add(item = new ItemTypeDouble(ProjectRecord.FINEFORTOOLITTLECLUBWORK, getFineForTooLittleClubwork(), 0, ItemTypeDouble.MAX,
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Bußgeld für Vereinsarbeit unter Sollstunden")));
            }
        }

        // store this record's key in all items to be able to later update the corresponging record
        // (only used for ProjectEditDialog)
        for (int i = 0; i < v.size(); i++) {
            v.get(i).setDataKey(getKey());
        }

        return v;
    }

    public TableItemHeader[] getGuiTableHeader() {
        return null; // not supported for ProjectRecord
    }

    public TableItem[] getGuiTableItems() {
        return null; // not supported for ProjectRecord
    }

    public static String[] getStorageTypeTypeStrings() {
        return Daten.efaConfig.getExperimentalFunctionsActivated() ?
                new String[]{
                IDataAccess.TYPESTRING_FILE_XML,
                IDataAccess.TYPESTRING_EFA_REMOTE,
                IDataAccess.TYPESTRING_EFA_CLOUD
        } :
                new String[]{
                IDataAccess.TYPESTRING_FILE_XML,
                IDataAccess.TYPESTRING_EFA_REMOTE
        };
    }

    public static String[] getStorageTypeNameStrings() {
        return Daten.efaConfig.getExperimentalFunctionsActivated() ?
                new String[]{
                International.getString("lokales Dateisystem"),
                Daten.EFA_REMOTE,
                Daten.EFA_CLOUD
        } :
                new String[]{
                International.getString("lokales Dateisystem"),
                Daten.EFA_REMOTE
        };
    }

}
