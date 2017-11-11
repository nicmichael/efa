/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.data;

import de.nmichael.efa.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.core.config.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.util.*;

import java.awt.AWTEvent;
import java.awt.event.FocusEvent;
import java.util.*;
import java.util.regex.*;

// @i18n complete
public class PersonRecord extends DataRecord implements IItemFactory {

    // =========================================================================
    // Field Names
    // =========================================================================
    public static final String ID = "Id";
    public static final String EFBID = "EfbId";
    public static final String FIRSTNAME = "FirstName";
    public static final String LASTNAME = "LastName";
    public static final String FIRSTLASTNAME = "FirstLastName";
    public static final String NAMEAFFIX = "NameAffix";
    public static final String TITLE = "Title";
    public static final String GENDER = "Gender";
    public static final String BIRTHDAY = "Birthday";
    public static final String ASSOCIATION = "Association";
    public static final String STATUSID = "StatusId";
    public static final String ADDRESSSTREET = "AddressStreet";
    public static final String ADDRESSADDITIONAL = "AddressAdditional";
    public static final String ADDRESSCITY = "AddressCity";
    public static final String ADDRESSZIP = "AddressZip";
    public static final String ADDRESSCOUNTRY = "AddressCountry";
    public static final String EMAIL = "Email";
    public static final String MEMBERSHIPNO = "MembershipNo";
    public static final String PASSWORD = "Password";
    public static final String EXTERNALID = "ExternalId";
    public static final String DISABILITY = "Disability";
    public static final String EXCLUDEFROMSTATISTIC = "ExcludeFromStatistics";
    public static final String EXCLUDEFROMCOMPETE = "ExcludeFromCompetition";
    public static final String EXCLUDEFROMCLUBWORK = "ExcludeFromClubwork";
    public static final String BOATUSAGEBAN = "BoatUsageBan";
    public static final String INPUTSHORTCUT = "InputShortcut";
    public static final String DEFAULTBOATID = "DefaultBoatId";
    public static final String FREEUSE1 = "FreeUse1";
    public static final String FREEUSE2 = "FreeUse2";
    public static final String FREEUSE3 = "FreeUse3";
    public static final String[] IDX_NAME_NAMEAFFIX = new String[]{FIRSTLASTNAME, NAMEAFFIX};
    private static String GUIITEM_GROUPS = "GUIITEM_GROUPS";
    private static String CAT_BASEDATA = "%01%" + International.getString("Basisdaten");
    private static String CAT_MOREDATA = "%02%" + International.getString("Weitere Daten");
    private static String CAT_ADDRESS = "%03%" + International.getString("Adresse");
    private static String CAT_GROUPS = "%04%" + International.getString("Gruppen");
    private static String CAT_FREEUSE = "%05%" + International.getString("Freie Verwendung");
    private static Pattern qnamePattern = Pattern.compile("(.+) \\(([^\\(\\)]+)\\)");

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(ID);
        t.add(IDataAccess.DATA_UUID);
        f.add(FIRSTNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(LASTNAME);
        t.add(IDataAccess.DATA_STRING);
        f.add(FIRSTLASTNAME);
        t.add(IDataAccess.DATA_VIRTUAL);
        f.add(NAMEAFFIX);
        t.add(IDataAccess.DATA_STRING);
        f.add(TITLE);
        t.add(IDataAccess.DATA_STRING);
        f.add(GENDER);
        t.add(IDataAccess.DATA_STRING);
        f.add(BIRTHDAY);
        t.add(IDataAccess.DATA_DATE);
        f.add(ASSOCIATION);
        t.add(IDataAccess.DATA_STRING);
        f.add(STATUSID);
        t.add(IDataAccess.DATA_UUID);
        f.add(ADDRESSSTREET);
        t.add(IDataAccess.DATA_STRING);
        f.add(ADDRESSADDITIONAL);
        t.add(IDataAccess.DATA_STRING);
        f.add(ADDRESSCITY);
        t.add(IDataAccess.DATA_STRING);
        f.add(ADDRESSZIP);
        t.add(IDataAccess.DATA_STRING);
        f.add(ADDRESSCOUNTRY);
        t.add(IDataAccess.DATA_STRING);
        f.add(EMAIL);
        t.add(IDataAccess.DATA_STRING);
        f.add(MEMBERSHIPNO);
        t.add(IDataAccess.DATA_STRING);
        f.add(PASSWORD);
        t.add(IDataAccess.DATA_STRING);
        f.add(EXTERNALID);
        t.add(IDataAccess.DATA_STRING);
        f.add(DISABILITY);
        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EXCLUDEFROMSTATISTIC);
        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EXCLUDEFROMCOMPETE);
        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EXCLUDEFROMCLUBWORK);
        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(BOATUSAGEBAN);
        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(INPUTSHORTCUT);
        t.add(IDataAccess.DATA_STRING);
        f.add(DEFAULTBOATID);
        t.add(IDataAccess.DATA_UUID);
        f.add(FREEUSE1);
        t.add(IDataAccess.DATA_STRING);
        f.add(FREEUSE2);
        t.add(IDataAccess.DATA_STRING);
        f.add(FREEUSE3);
        t.add(IDataAccess.DATA_STRING);
        f.add(EFBID);
        t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(Persons.DATATYPE, f, t, true);
        metaData.setKey(new String[]{ID}); // plus VALID_FROM
        metaData.addIndex(IDX_NAME_NAMEAFFIX);
    }

    public PersonRecord(Persons persons, MetaData metaData) {
        super(persons, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<UUID, Long, String>(getId(), getValidFrom(), null);
    }

    public static DataKey getKey(UUID id, long validFrom) {
        return new DataKey<UUID, Long, String>(id, validFrom, null);
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

    public void setFirstName(String name) {
        setString(FIRSTNAME, name);
    }

    public String getFirstName() {
        return getString(FIRSTNAME);
    }

    public void setLastName(String name) {
        setString(LASTNAME, name);
    }

    public String getLastName() {
        return getString(LASTNAME);
    }

    public void setFirstLastName(String name) {
        // nothing to do (this column in virtual)
    }

    public String getFirstLastName() {
        return getFirstLastName(true);
    }

    public String getFirstLastName(boolean alwaysFirstFirst) {
        return getFullName(getString(FIRSTNAME), getString(LASTNAME), null,
                (alwaysFirstFirst ? true : Daten.efaConfig.getValueNameFormatIsFirstNameFirst()));
    }

    public void setNameAffix(String affix) {
        setString(NAMEAFFIX, affix);
    }

    public String getNameAffix() {
        return getString(NAMEAFFIX);
    }

    public void setTitle(String title) {
        setString(TITLE, title);
    }

    public String getTitle() {
        return getString(TITLE);
    }

    public void setGender(String gender) {
        setString(GENDER, gender);
    }

    public String getGender() {
        return getString(GENDER);
    }

    public String getGenderAsString() {
        String s = getGender();
        return (s != null ? Daten.efaTypes.getValue(EfaTypes.CATEGORY_GENDER, s) : null);
    }

    public void setBirthday(DataTypeDate date) {
        setDate(BIRTHDAY, date);
    }

    public DataTypeDate getBirthday() {
        return getDate(BIRTHDAY);
    }

    public void setAssocitation(String name) {
        setString(ASSOCIATION, name);
    }

    public String getAssocitation() {
        return getString(ASSOCIATION);
    }

    public void setStatusId(UUID id) {
        setUUID(STATUSID, id);
    }

    public UUID getStatusId() {
        return getUUID(STATUSID);
    }

    public StatusRecord getStatusRecord() {
        UUID id = getStatusId();
        if (id != null) {
            return getPersistence().getProject().getStatus(false).getStatus(id);
        }
        return null;
    }

    public String getStatusName() {
        StatusRecord r = getStatusRecord();
        if (r != null) {
            return r.getStatusName();
        }
        return null;
    }

    public boolean isStatusMember() {
        StatusRecord r = getStatusRecord();
        return (r != null && r.isMember());
    }

    public void setAddressStreet(String street) {
        setString(ADDRESSSTREET, street);
    }

    public String getAddressStreet() {
        return getString(ADDRESSSTREET);
    }

    public void setAddressAdditional(String addressAdditional) {
        setString(ADDRESSADDITIONAL, addressAdditional);
    }

    public String getAddressAdditional() {
        return getString(ADDRESSADDITIONAL);
    }

    public void setAddressCity(String city) {
        setString(ADDRESSCITY, city);
    }

    public String getAddressCity() {
        return getString(ADDRESSCITY);
    }

    public void setAddressZip(String zip) {
        setString(ADDRESSZIP, zip);
    }

    public String getAddressZip() {
        return getString(ADDRESSZIP);
    }

    public void setAddressCountry(String country) {
        setString(ADDRESSCOUNTRY, country);
    }

    public String getAddressCountry() {
        return getString(ADDRESSCOUNTRY);
    }

    public void setEmail(String email) {
        setString(EMAIL, email);
    }

    public String getEmail() {
        return getString(EMAIL);
    }

    public String getAddressComplete(String lineSep) {
        String street = getAddressStreet();
        String additional = getAddressAdditional();
        String city = getAddressCity();
        String zip = getAddressZip();
        String country = getAddressCountry();
        StringBuilder addr = new StringBuilder();
        if (street != null && street.length() > 0) {
            addr.append(street);
        }
        if (additional != null && additional.length() > 0) {
            if (addr.length() > 0) {
                addr.append(lineSep);
            }
            addr.append(additional);
        }
        if ((city != null && city.length() > 0) || (zip != null && zip.length() > 0)) {
            if (addr.length() > 0) {
                addr.append(lineSep);
            }
            if (International.getLanguageID().startsWith("de")) {
                if (zip != null && zip.length() > 0) {
                    addr.append(zip + " ");
                }
                if (city != null && city.length() > 0) {
                    addr.append(city);
                }
            } else {
                if (city != null && city.length() > 0) {
                    addr.append(city + ", ");
                }
                if (zip != null && zip.length() > 0) {
                    addr.append(zip);
                }
            }
        }
        if (country != null && country.length() > 0) {
            if (addr.length() > 0) {
                addr.append(lineSep);
            }
            addr.append(country);
        }
        return addr.toString();
    }

    public void setMembershipNo(String no) {
        setString(MEMBERSHIPNO, no);
    }

    public String getMembershipNo() {
        return getString(MEMBERSHIPNO);
    }

    public void setPassword(String password) {
        setString(PASSWORD, password);
    }

    public String getPassword() {
        return getString(PASSWORD);
    }

    public void setExternalId(String id) {
        setString(EXTERNALID, id);
    }

    public String getExternalId() {
        return getString(EXTERNALID);
    }

    public void setDisability(boolean disabled) {
        setBool(DISABILITY, disabled);
    }

    public boolean getDisability() {
        return getBool(DISABILITY);
    }

    public void setExcludeFromPublicStatistics(boolean exclude) {
        setBool(EXCLUDEFROMSTATISTIC, exclude);
    }

    public boolean getExcludeFromPublicStatistics() {
        return getBool(EXCLUDEFROMSTATISTIC);
    }

    public void setExcludeFromCompetition(boolean exclude) {
        setBool(EXCLUDEFROMCOMPETE, exclude);
    }

    public boolean getExcludeFromCompetition() {
        return getBool(EXCLUDEFROMCOMPETE);
    }

    public void setExcludeFromClubwork(boolean exclude) {
        setBool(EXCLUDEFROMCLUBWORK, exclude);
    }

    public boolean getExcludeFromClubwork() {
        return getBool(EXCLUDEFROMCLUBWORK);
    }

    public void setBoatUsageBan(boolean banned) {
        setBool(BOATUSAGEBAN, banned);
    }

    public boolean getBoatUsageBan() {
        return getBool(BOATUSAGEBAN);
    }

    public void setInputShortcut(String shortcut) {
        setString(INPUTSHORTCUT, shortcut);
    }

    public String getInputShortcut() {
        return getString(INPUTSHORTCUT);
    }

    public void setDefaultBoatId(UUID id) {
        setUUID(DEFAULTBOATID, id);
    }

    public UUID getDefaultBoatId() {
        return getUUID(DEFAULTBOATID);
    }

    public String getDefaultBoatAsName() {
        UUID id = getUUID(DEFAULTBOATID);
        Boats boats = getPersistence().getProject().getBoats(false);
        if (boats != null) {
            DataRecord r = boats.getBoat(id, System.currentTimeMillis());
            if (r != null) {
                return r.getQualifiedName();
            }
        }
        return null;
    }

    public GroupRecord[] getGroupList() {
        Groups groups = getPersistence().getProject().getGroups(false);
        return groups.getGroupsForPerson(getId(), getValidFrom(), getInvalidFrom() - 1);
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

    protected Object getVirtualColumn(int fieldIdx) {
        if (getFieldName(fieldIdx).equals(FIRSTLASTNAME)) {
            return getFirstLastName();
        }
        return null;
    }

    public static String getFullName(String first, String last, String affix, boolean firstFirst) {
        String s = "";
        if (firstFirst) {
            if (first != null && first.length() > 0) {
                s = first.trim();
            }
            if (last != null && last.length() > 0) {
                s = s + (s.length() > 0 ? " " : "") + last.trim();
            }
        } else {
            if (last != null && last.length() > 0) {
                s = last.trim();
            }
            if (first != null && first.length() > 0) {
                s = s + (s.length() > 0 ? ", " : "") + first.trim();
            }
        }
        if (affix != null && affix.length() > 0) {
            s = s + " (" + affix + ")";
        }
        return s;
    }

    public static String getFirstLastName(String name) {
        if (name == null || name.length() == 0) {
            return "";
        }
        int pos = name.indexOf(", ");
        if (pos < 0) {
            return name;
        }
        return (name.substring(pos + 2) + " " + name.substring(0, pos)).trim();
    }

    public String getQualifiedName(boolean firstFirst) {
        return getFullName(getFirstName(), getLastName(), getNameAffix(), firstFirst);
    }

    public String getQualifiedName() {
        return getQualifiedName(Daten.efaConfig.getValueNameFormatIsFirstNameFirst());
    }

    public String[] getQualifiedNameFields() {
        return IDX_NAME_NAMEAFFIX;
    }

    public String[] getQualifiedNameFieldsTranslateVirtualToReal() {
        return new String[]{FIRSTNAME, LASTNAME, NAMEAFFIX};
    }

    public String[] getQualifiedNameValues(String qname) {
        Matcher m = qnamePattern.matcher(qname);
        if (m.matches()) {
            return new String[]{
                getFirstLastName(m.group(1).trim()),
                m.group(2).trim()
            };
        } else {
            return new String[]{
                getFirstLastName(qname.trim()),
                null
            };
        }
    }

    public static String[] tryGetFirstLastNameAndAffix(String s) {
        Matcher m = qnamePattern.matcher(s);
        String name = s.trim();
        String affix = null;
        if (m.matches()) {
            name = m.group(1).trim();
            affix = m.group(2).trim();
        }

        boolean firstFirst = Daten.efaConfig.getValueNameFormatIsFirstNameFirst();
        String firstName = (firstFirst ? name : null); // if first and last name cannot be found, ...
        String lastName = (firstFirst ? null : name);  // ... use full name as either first or last
        int pos = name.indexOf(", ");
        if (pos < 0) {
            pos = name.indexOf(" ");
            if (pos >= 0) {
                firstName = name.substring(0, pos).trim();
                lastName = name.substring(pos + 1).trim();
            }
        } else {
            firstName = name.substring(pos + 2);
            lastName = name.substring(0, pos).trim();
        }
        return new String[]{firstName, lastName, affix};
    }

    public static String[] tryGetNameAndAffix(String s) {
        Matcher m = qnamePattern.matcher(s);
        String name = s.trim();
        String affix = null;
        if (m.matches()) {
            name = m.group(1).trim();
            affix = m.group(2).trim();
        }
        return new String[]{name, affix};
    }

    public Object getUniqueIdForRecord() {
        return getId();
    }

    public String getAsText(String fieldName) {
        if (fieldName.equals(FIRSTLASTNAME)) {
            return getFirstLastName(false);
        }
        if (fieldName.equals(GENDER)) {
            String s = getAsString(fieldName);
            if (s != null) {
                return Daten.efaTypes.getValue(EfaTypes.CATEGORY_GENDER, s);
            }
            return null;
        }
        if (fieldName.equals(STATUSID)) {
            return getStatusName();
        }
        if (fieldName.equals(DEFAULTBOATID)) {
            return getDefaultBoatAsName();
        }
        return super.getAsText(fieldName);
    }

    public boolean setFromText(String fieldName, String value) {
        if (fieldName.equals(GENDER)) {
            String s = Daten.efaTypes.getTypeForValue(EfaTypes.CATEGORY_GENDER, value);
            if (s != null) {
                set(fieldName, s);
            }
        } else if (fieldName.equals(STATUSID)) {
            Status status = getPersistence().getProject().getStatus(false);
            StatusRecord sr = status.findStatusByName(value);
            if (sr != null) {
                set(fieldName, sr.getId());
            }
        } else if (fieldName.equals(DEFAULTBOATID)) {
            Boats boats = getPersistence().getProject().getBoats(false);
            BoatRecord br = boats.getBoat(value, System.currentTimeMillis());
            if (br != null) {
                set(fieldName, br.getId());
            }
        } else {
            return super.setFromText(fieldName, value);
        }
        return (value.equals(getAsText(fieldName)));
    }

    public Integer getPersonMemberMonth(DataTypeDate startDate, DataTypeDate endDate) {
        long fromLong = this.getValidFrom();
        if (fromLong == 0) {
            return 0;
        }
        DataTypeDate from = new DataTypeDate(fromLong);

        long toLong = this.getInvalidFrom();
        DataTypeDate to;
        if (toLong == Long.MAX_VALUE) {
            to = endDate;
        } else {
            to = new DataTypeDate(toLong);
        }

        return DataTypeDate.getMonthIntersect(from, to, startDate, endDate);
    }
    
    public static String getAssociationPostfix(PersonRecord p) {
        if (p != null && p.getAssocitation() != null && p.getAssocitation().length() > 0) {
            return " [" + p.getAssocitation() + "]";
        }
        return "";
    }

    public String getAssociationPostfix() {
        return getAssociationPostfix(this);
    }

    public static String trimAssociationPostfix(String s) {
        int pos = s.lastIndexOf(" [");
        if (pos > 0) {
            return s.substring(0, pos);
        }
        return s;
    }

    public IItemType[] getDefaultItems(String itemName) {
        if (itemName.equals(PersonRecord.GUIITEM_GROUPS)) {
            IItemType[] items = new IItemType[1];
            String CAT_USAGE = "%04%" + International.getString("Gruppen");
            items[0] = getGuiItemTypeStringAutoComplete(PersonRecord.GUIITEM_GROUPS, null,
                    IItemType.TYPE_PUBLIC, CAT_GROUPS,
                    getPersistence().getProject().getGroups(false), getValidFrom(), getInvalidFrom() - 1,
                    International.getString("Gruppe"));
            items[0].setFieldSize(300, -1);
            return items;
        }
        return null;

    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        Status status = getPersistence().getProject().getStatus(false);
        Boats boats = getPersistence().getProject().getBoats(false);
        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();
        v.add(item = new ItemTypeString(PersonRecord.FIRSTNAME, getFirstName(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Vorname")));
        ((ItemTypeString) item).setNotAllowedCharacters(",");
        v.add(item = new ItemTypeString(PersonRecord.LASTNAME, getLastName(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Nachname")));
        ((ItemTypeString) item).setNotAllowedCharacters(",");
        v.add(item = new ItemTypeString(PersonRecord.NAMEAFFIX, getNameAffix(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Namenszusatz")));
        ((ItemTypeString) item).setNotAllowedCharacters(",");
        v.add(item = new ItemTypeString(PersonRecord.TITLE, getTitle(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Titel")));
        ((ItemTypeString) item).setNotAllowedCharacters(",");
        v.add(item = new ItemTypeStringList(PersonRecord.GENDER, getGender(),
                EfaTypes.makeGenderArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes.makeGenderArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Geschlecht")));
        v.add(item = new ItemTypeDate(PersonRecord.BIRTHDAY, getBirthday(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Geburtstag")));
        ((ItemTypeDate) item).setAllowYearOnly(true);

        if (admin != null && admin.isAllowedEditPersons()) {
            v.add(item = new ItemTypeStringList(PersonRecord.STATUSID, (getStatusId() != null ? getStatusId().toString() : status.getStatusOther().getId().toString()),
                    status.makeStatusArray(Status.ARRAY_STRINGLIST_VALUES), status.makeStatusArray(Status.ARRAY_STRINGLIST_DISPLAY),
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Status")));

            v.add(item = new ItemTypeString(PersonRecord.ASSOCIATION, getAssocitation(),
                    IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Verein")));
            v.add(item = new ItemTypeString(PersonRecord.MEMBERSHIPNO, getMembershipNo(),
                    IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Mitgliedsnummer")));
            v.add(item = new ItemTypeString(PersonRecord.PASSWORD, getPassword(),
                    IItemType.TYPE_EXPERT, CAT_MOREDATA, International.getString("Paßwort")));
            v.add(item = new ItemTypeBoolean(PersonRecord.DISABILITY, getDisability(),
                    IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("50% oder mehr Behinderung")));
            v.add(item = new ItemTypeBoolean(PersonRecord.EXCLUDEFROMSTATISTIC, getExcludeFromPublicStatistics(),
                    IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("von allgemein verfügbaren Statistiken ausnehmen")));
            v.add(item = new ItemTypeBoolean(PersonRecord.EXCLUDEFROMCOMPETE, getExcludeFromCompetition(),
                    IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("von Wettbewerbsmeldungen ausnehmen")));
            v.add(item = new ItemTypeBoolean(PersonRecord.EXCLUDEFROMCLUBWORK, getExcludeFromClubwork(),
                    IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("von Vereinsarbeit ausnehmen")));
            v.add(item = new ItemTypeBoolean(PersonRecord.BOATUSAGEBAN, getBoatUsageBan(),
                    IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Bootsbenutzungs-Sperre")));
            v.add(item = new ItemTypeString(PersonRecord.INPUTSHORTCUT, getInputShortcut(),
                    IItemType.TYPE_PUBLIC, CAT_MOREDATA, International.getString("Eingabekürzel")));
            v.add(item = getGuiItemTypeStringAutoComplete(PersonRecord.DEFAULTBOATID, getDefaultBoatId(),
                    IItemType.TYPE_PUBLIC, CAT_MOREDATA,
                    boats, getValidFrom(), getInvalidFrom() - 1,
                    International.getString("Standard-Boot")));
            item.setFieldSize(300, -1);
            v.add(item = new ItemTypeString(PersonRecord.EXTERNALID, getExternalId(),
                    IItemType.TYPE_EXPERT, CAT_MOREDATA, International.getString("Externe ID")));
            if (Daten.efaConfig.getValueUseFunctionalityCanoeingGermany()) {
                v.add(item = new ItemTypeString(PersonRecord.EFBID, getEfbId(),
                        IItemType.TYPE_EXPERT, CAT_MOREDATA, International.onlyFor("Kanu-eFB ID", "de")));
            }

            v.add(item = new ItemTypeString(PersonRecord.ADDRESSSTREET, getAddressStreet(),
                    IItemType.TYPE_PUBLIC, CAT_ADDRESS, International.getString("Straße")));
            v.add(item = new ItemTypeString(PersonRecord.ADDRESSADDITIONAL, getAddressAdditional(),
                    IItemType.TYPE_PUBLIC, CAT_ADDRESS, International.getString("weitere Adreßzeile")));
            v.add(item = new ItemTypeString(PersonRecord.ADDRESSCITY, getAddressCity(),
                    IItemType.TYPE_PUBLIC, CAT_ADDRESS, International.getString("Stadt")));
            v.add(item = new ItemTypeString(PersonRecord.ADDRESSZIP, getAddressZip(),
                    IItemType.TYPE_PUBLIC, CAT_ADDRESS, International.getString("Postleitzahl")));
            v.add(item = new ItemTypeString(PersonRecord.ADDRESSCOUNTRY, getAddressCountry(),
                    IItemType.TYPE_PUBLIC, CAT_ADDRESS, International.getString("Land")));
            v.add(item = new ItemTypeString(PersonRecord.EMAIL, getEmail(),
                    IItemType.TYPE_PUBLIC, CAT_ADDRESS, International.getString("email")));

            // CAT_GROUPS
            if (getId() != null && admin != null && admin.isAllowedEditGroups()) {
                Vector<IItemType[]> itemList = new Vector<IItemType[]>();
                GroupRecord[] groupList = getGroupList();
                DataTypeList<UUID> agList = new DataTypeList<UUID>();
                for (int i = 0; groupList != null && i < groupList.length; i++) {
                    agList.add(groupList[i].getId());
                }
                for (int i = 0; agList != null && i < agList.length(); i++) {
                    IItemType[] items = getDefaultItems(GUIITEM_GROUPS);
                    ((ItemTypeStringAutoComplete) items[0]).setId(agList.get(i));
                    itemList.add(items);
                }
                v.add(item = new ItemTypeItemList(GUIITEM_GROUPS, itemList, this,
                        IItemType.TYPE_PUBLIC, CAT_GROUPS, International.getString("Gruppenzugehörigkeit")));
                ((ItemTypeItemList) item).setXForAddDelButtons(3);
                ((ItemTypeItemList) item).setPadYbetween(0);
                ((ItemTypeItemList) item).setRepeatTitle(false);
                ((ItemTypeItemList) item).setAppendPositionToEachElement(true);
            }

            v.add(item = new ItemTypeString(PersonRecord.FREEUSE1, getFreeUse1(),
                    IItemType.TYPE_PUBLIC, CAT_FREEUSE, International.getString("Freie Verwendung") + " 1"));
            v.add(item = new ItemTypeString(PersonRecord.FREEUSE2, getFreeUse2(),
                    IItemType.TYPE_PUBLIC, CAT_FREEUSE, International.getString("Freie Verwendung") + " 2"));
            v.add(item = new ItemTypeString(PersonRecord.FREEUSE3, getFreeUse3(),
                    IItemType.TYPE_PUBLIC, CAT_FREEUSE, International.getString("Freie Verwendung") + " 3"));
        }

        // hidden parameter, just for BatchEditDialog
        v.add(item = getGuiItemTypeStringAutoComplete(PersonRecord.FIRSTLASTNAME, null,
                IItemType.TYPE_INTERNAL, "",
                getPersistence(), getValidFrom(), getInvalidFrom() - 1,
                International.getString("Name")));

        return v;
    }

    public void saveGuiItems(Vector<IItemType> items) {
        for (IItemType item : items) {
            String name = item.getName();
            if (name.equals(GUIITEM_GROUPS) && item.isChanged()) {
                ItemTypeItemList list = (ItemTypeItemList) item;
                Groups groups = getPersistence().getProject().getGroups(false);
                Hashtable<UUID, String> groupIds = new Hashtable<UUID, String>();
                for (int i = 0; i < list.size(); i++) {
                    ItemTypeStringAutoComplete l = (ItemTypeStringAutoComplete) list.getItems(i)[0];
                    UUID id = (UUID) l.getId(l.getValue());
                    if (id != null) {
                        groupIds.put(id, "foo");
                    }
                }
                groups.setGroupsForPerson(getId(),
                        groupIds.keySet().toArray(new UUID[0]),
                        getInvalidFrom() - 1);
            }
        }
        super.saveGuiItems(items);
    }

    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[4];
        if (Daten.efaConfig.getValueNameFormatIsFirstNameFirst()) {
            header[0] = new TableItemHeader(International.getString("Vorname"));
            header[1] = new TableItemHeader(International.getString("Nachname"));
            header[2] = new TableItemHeader(International.getString("Geburtstag"));
            header[3] = new TableItemHeader(International.getString("Status"));
        } else {
            header[0] = new TableItemHeader(International.getString("Nachname"));
            header[1] = new TableItemHeader(International.getString("Vorname"));
            header[2] = new TableItemHeader(International.getString("Geburtstag"));
            header[3] = new TableItemHeader(International.getString("Status"));
        }
        return header;
    }

    public TableItem[] getGuiTableItems() {
        TableItem[] items = new TableItem[4];
        if (Daten.efaConfig.getValueNameFormatIsFirstNameFirst()) {
            items[0] = new TableItem(getFirstName());
            items[1] = new TableItem(getLastName());
            items[2] = new TableItem(getBirthday());
            items[3] = new TableItem(getStatusName());
        } else {
            items[0] = new TableItem(getLastName());
            items[1] = new TableItem(getFirstName());
            items[2] = new TableItem(getBirthday());
            items[3] = new TableItem(getStatusName());
        }
        return items;
    }
}
