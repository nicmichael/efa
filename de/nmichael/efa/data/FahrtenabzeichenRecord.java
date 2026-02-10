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

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.efawett.DRVSignatur;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.util.*;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.*;

// @i18n complete

public class FahrtenabzeichenRecord extends DataRecord implements IItemListener {

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String PERSONID            = "PersonId";
    public static final String ABZEICHEN           = "Abzeichen";
    public static final String ABZEICHENAB         = "AbzeichenAB";
    public static final String KILOMETER = "Kilometer";
    public static final String KILOMETERAB = "KilometerAB";
    public static final String FAHRTENHEFT = "Fahrtenheft";

    public static final String GUI_NAMEMISMATCH = "GUI_NAMEMISMATCH";
    public static final String GUI_YEAROFBIRTH = "GUI_YEAROFBIRTH";
    public static final String GUI_LETZTESFAHRTENABZEICHEN = "GUI_LETZTESFAHRTENABZEICHEN";
    public static final String GUI_TEILNEHMERNUMMER = "GUI_TEILNEHMERNUMMER";
    public static final String GUI_VORNAME = "GUI_VORNAME";
    public static final String GUI_NACHNAME = "GUI_NACHNAME";
    public static final String GUI_JAHRGANG = "GUI_JAHRGANG";
    public static final String GUI_ANZABZEICHEN = "GUI_ANZABZEICHEN";
    public static final String GUI_ANZKM = "GUI_ANZKM";
    public static final String GUI_ANZABZEICHENAB = "GUI_ANZABZEICHENAB";
    public static final String GUI_ANZKMAB = "GUI_ANZKMAB";
    public static final String GUI_LETZTESJAHR = "GUI_LETZTESJAHR";
    public static final String GUI_LETZTEKM = "GUI_LETZTEKM";
    public static final String GUI_LETZTESDATUM = "GUI_LETZTESDATUM";
    public static final String GUI_VERSION = "GUI_VERSION";
    public static final String GUI_SCHLUESSEL = "GUI_SCHLUESSEL";
    public static final String GUI_SIGNATUR = "GUI_SIGNATUR";
    public static final String GUI_STATUS = "GUI_STATUS";
    public static final String ECRID = "ecrid";

    private Vector<IItemType> myItems;
    private DRVSignatur cachedSignature;
    private long cachedSignatureTs = 0;

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(PERSONID);                          t.add(IDataAccess.DATA_UUID);
        f.add(ABZEICHEN);                         t.add(IDataAccess.DATA_INTEGER);
        f.add(ABZEICHENAB);                       t.add(IDataAccess.DATA_INTEGER);
        f.add(KILOMETER);                         t.add(IDataAccess.DATA_INTEGER);
        f.add(KILOMETERAB);                       t.add(IDataAccess.DATA_INTEGER);
        f.add(FAHRTENHEFT);                       t.add(IDataAccess.DATA_STRING);
        f.add(GUI_VORNAME);                       t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_NACHNAME);                      t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_JAHRGANG);                      t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_ANZABZEICHEN);                  t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_ANZKM);                         t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_ANZABZEICHENAB);                t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_ANZKMAB);                       t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_LETZTESJAHR);                   t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_LETZTEKM);                      t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_LETZTESDATUM);                  t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_VERSION);                       t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_SCHLUESSEL);                    t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_SIGNATUR);                      t.add(IDataAccess.DATA_VIRTUAL);
        f.add(GUI_STATUS);                        t.add(IDataAccess.DATA_VIRTUAL);
        f.add(ECRID);                             t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(Fahrtenabzeichen.DATATYPE, f, t, false);
        metaData.setKey(new String[] { PERSONID });
    }

    public FahrtenabzeichenRecord(Fahrtenabzeichen fahrtenabzeichen, MetaData metaData) {
        super(fahrtenabzeichen, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<UUID,String,String>(getPersonId(),null,null);
    }

    public static DataKey getKey(UUID personId) {
        return new DataKey<UUID,String,String>(personId,null,null);
    }

    public boolean getDeleted() {
        return getPersistence().getProject().getPersons(false).isPersonDeleted(getPersonId());
    }

    public void setPersonId(UUID id) {
        setUUID(PERSONID, id);
    }
    public UUID getPersonId() {
        return getUUID(PERSONID);
    }

    public PersonRecord getPersonRecord(UUID id) {
        if (id == null) {
            return null;
        }
        Persons persons = getPersistence().getProject().getPersons(false);
        PersonRecord p = persons.getPerson(id, System.currentTimeMillis());
        if (p == null) {
            try {
                p = (PersonRecord)persons.data().getValidLatest(PersonRecord.getKey(id, System.currentTimeMillis()));
            } catch(Exception e) {
                Logger.logdebug(e);
            }
        }
        return p;
    }

    public boolean existsPersonRecord() {
        try {
            Persons persons = getPersistence().getProject().getPersons(false);
            return persons.isPersonExist(getPersonId());
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }
    
    public PersonRecord getPersonRecord() {
        return getPersonRecord(getPersonId());
    }

    public String getPersonName() {
        PersonRecord p = getPersonRecord();
        return (p != null ? p.getQualifiedName() : "?");
    }

    public String getQualifiedName() {
        return getPersonName();
    }

    public String[] getQualifiedNameFields() {
        return new String[] { PERSONID };
    }

    public int getYearOfBirth() {
        PersonRecord p = getPersonRecord();
        return (p != null && p.getBirthday() != null && p.getBirthday().isSet() ? p.getBirthday().getYear() : 0);
    }


    public void setAbzeichen(int abzeichen) {
        setInt(ABZEICHEN, abzeichen);
    }
    public int getAbzeichen() {
        int sigValue = Integer.parseInt(getDRVSignaturValue(ABZEICHEN));
        return (sigValue > 0 ? sigValue : getInt(ABZEICHEN));
    }

    public void setAbzeichenAB(int abzeichen) {
        setInt(ABZEICHENAB, abzeichen);
    }
    public int getAbzeichenAB() {
        //@AB int sigValue = Integer.parseInt(getDRVSignaturValue(ABZEICHENAB));
        //@AB return (sigValue > 0 ? sigValue : getInt(ABZEICHENAB));
        return 0;
    }

    public void setKilometer(int km) {
        setInt(KILOMETER, km);
    }
    public int getKilometer() {
        int sigValue = Integer.parseInt(getDRVSignaturValue(KILOMETER));
        return (sigValue > 0 ? sigValue : getInt(KILOMETER));
    }

    public void setKilometerAB(int km) {
        setInt(KILOMETERAB, km);
    }
    public int getKilometerAB() {
        //@AB int sigValue = Integer.parseInt(getDRVSignaturValue(KILOMETERAB));
        //@AB return (sigValue > 0 ? sigValue : getInt(KILOMETERAB));
        return 0;
    }

    public void setFahrtenheft(String data) {
        setString(FAHRTENHEFT, data);
    }
    public String getFahrtenheft() {
        return getString(FAHRTENHEFT);
    }

    public DRVSignatur getDRVSignatur() {
        String s = getFahrtenheft();
        if (s == null || s.length() == 0) {
            return null;
        }
        return new DRVSignatur(s);
    }

    public String getDRVSignaturValue(String field) {
        try {
            DRVSignatur sig = cachedSignature;
            if (sig == null || System.currentTimeMillis()-cachedSignatureTs > 100) {
                sig = getDRVSignatur();
                if (sig != null) {
                    // don't check, takes too long... // sig.checkSignature();
                    cachedSignatureTs = System.currentTimeMillis();
                }
            }
            if (sig != null && sig.getSignatureState() == DRVSignatur.SIG_VALID) {
                if (field.equals(ABZEICHEN) || field.equals(GUI_ANZABZEICHEN)) {
                    return Integer.toString(sig.getAnzAbzeichen());
                }
                if (field.equals(ABZEICHENAB) || field.equals(GUI_ANZABZEICHENAB)) {
                    return Integer.toString(sig.getAnzAbzeichenAB());
                }
                if (field.equals(KILOMETER) || field.equals(GUI_ANZKM)) {
                    return Integer.toString(sig.getGesKm());
                }
                if (field.equals(KILOMETERAB) || field.equals(GUI_ANZKMAB)) {
                    return Integer.toString(sig.getGesKmAB());
                }
                if (field.equals(GUI_VORNAME)) {
                    return sig.getVorname();
                }
                if (field.equals(GUI_NACHNAME)) {
                    return sig.getNachname();
                }
                if (field.equals(GUI_JAHRGANG)) {
                    return sig.getJahrgang();
                }
                if (field.equals(GUI_LETZTESJAHR)) {
                    return Integer.toString(sig.getJahr());
                }
                if (field.equals(GUI_LETZTEKM)) {
                    return Integer.toString(sig.getLetzteKm());
                }
                if (field.equals(GUI_LETZTESDATUM)) {
                    return (sig.getSignatureDate() != null ? EfaUtil.date2String(sig.getSignatureDate(), false) : "");
                }
                if (field.equals(GUI_VERSION)) {
                    return Integer.toString(sig.getVersion());
                }
                if (field.equals(GUI_SCHLUESSEL)) {
                    return sig.getKeyName();
                }
                if (field.equals(GUI_SIGNATUR)) {
                    return sig.getSignaturString();
                }
                if (field.equals(GUI_STATUS)) {
                    return sig.getSignatureStateDescription();
                }
            }
        } catch (Exception e) {
            return "-1";
        }
        return "-1";
    }

    public String getLetzteMeldungDescription() {
        DRVSignatur sig = getDRVSignatur();
        if (sig == null) {
            return "";
        }
        return sig.getJahr() + " (" + sig.getLetzteKm() + " Km)";
    }

    public String getAsText(String fieldName) {
        if (fieldName.equals(PERSONID)) {
            return getPersonName();
        }
        return super.getAsText(fieldName);
    }

    public boolean setFromText(String fieldName, String value) {
        if (fieldName.equals(PERSONID)) {
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

    protected Object getVirtualColumn(int fieldIdx) {
        String field = getFieldName(fieldIdx);
        return getDRVSignaturValue(field);
    }
    
    public ItemTypeStringAutoComplete getUnknownPersonInputField(String name) {
        return getGuiItemTypeStringAutoComplete(FahrtenabzeichenRecord.PERSONID, getPersonId(),
                IItemType.TYPE_PUBLIC, "",
                persistence.getProject().getPersons(false), 0, Long.MAX_VALUE,
                International.getMessage("{name} konnte nicht in der Personenliste gefunden werden.", name) + "\n" +
                International.onlyFor("Bitte gib korrekten Namen der Person ein", "de") + ":");
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        String CAT_BASEDATA         = "%01%" + International.onlyFor("Allgemein","de");
        String CAT_FAHRTENABZEICHEN = "%02%" + International.onlyFor("elektronisches Fahrtenheft","de");
        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();
        v.add(item = getGuiItemTypeStringAutoComplete(FahrtenabzeichenRecord.PERSONID, getPersonId(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                persistence.getProject().getPersons(false), 0, Long.MAX_VALUE,
                International.getString("Name")));
        ((ItemTypeStringAutoComplete)item).setFieldSize(300, -1);
        // Since PersonId is the key to this record, it may never be changed.
        // Only when the record is newly created and getPersonRecord() returns null, it's allowed to
        // modify the persons name (and thus, the associated PersonRecord)!
        item.setEditable(getPersonRecord() == null);
        item.registerItemListener(this);
        v.add(item = new ItemTypeLabel(GUI_NAMEMISMATCH,
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                International.onlyFor("Achtung: Der Name stimmt nicht mit dem Namen im Fahrtenheft überein!","de")));
        item.setEditable(false);
        item.setColor(Color.red);
        item.setVisible(false);

        v.add(item = new ItemTypeString(GUI_YEAROFBIRTH, Integer.toString(getYearOfBirth()),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Jahrgang")));
        item.setEditable(false);
        v.add(item = new ItemTypeInteger(FahrtenabzeichenRecord.ABZEICHEN, getAbzeichen(), 0, 99,
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.onlyFor("Anzahl der bereits erfüllten Abzeichen","de")));
        v.add(item = new ItemTypeInteger(FahrtenabzeichenRecord.KILOMETER, getKilometer(), 0, Integer.MAX_VALUE,
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.onlyFor("Insgesamt bereits nachgewiesene Kilometer","de")));
        //@AB v.add(item = new ItemTypeInteger(FahrtenabzeichenRecord.ABZEICHENAB, getAbzeichenAB(), 0, 99,
        //@AB         IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.onlyFor("... davon Abzeichen in den Jugend-Gruppen A/B","de")));
        //@AB v.add(item = new ItemTypeInteger(FahrtenabzeichenRecord.KILOMETERAB, getKilometerAB(), 0, Integer.MAX_VALUE,
        //@AB         IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.onlyFor("... davon Kilometer in den Jugend-Gruppen A/B","de")));
        v.add(item = new ItemTypeString(GUI_LETZTESFAHRTENABZEICHEN, getLetzteMeldungDescription(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.onlyFor("Letztes elektronisches Fahrtenabzeichen","de")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(FahrtenabzeichenRecord.FAHRTENHEFT, getFahrtenheft(),
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("elektronisches Fahrtenheft","de")));
        item.setFieldSize(500, -1);
        item.registerItemListener(this);
        v.add(item = new ItemTypeString(GUI_TEILNEHMERNUMMER, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("DRV-Teilnehmernummer", "de")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_VORNAME, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("Vorname", "de")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_NACHNAME, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("Nachname", "de")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_JAHRGANG, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("Jahrgang", "de")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_ANZABZEICHEN, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("Anzahl der Fahrtenabzeichen", "de")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_ANZKM, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("Insgesamt nachgewiesene Kilometer", "de")));
        item.setEditable(false);
        //@AB v.add(item = new ItemTypeString(GUI_ANZABZEICHENAB, "",
        //@AB         IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("... davon Fahrtenabzeichen Jugend A/B", "de")));
        //@AB item.setEditable(false);
        //@AB v.add(item = new ItemTypeString(GUI_ANZKMAB, "",
        //@AB         IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("... davon Kilometer Jugend A/B", "de")));
        //@AB item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_LETZTESJAHR, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("Jahr der letzten elektronischen Meldung", "de")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_LETZTEKM, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("Kilometer bei der letzten elektronischen Meldung", "de")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_LETZTESDATUM, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("Ausstellungsdatum des Fahrtenhefts", "de")));
        item.setEditable(false);
        item.setPadding(0, 0, 0, 20);
        v.add(item = new ItemTypeString(GUI_VERSION, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("Fahrtenabzeichen-Version", "de")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_SCHLUESSEL, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("öffentlicher DRV-Schlüssel", "de")));
        item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_SIGNATUR, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("DRV-Signatur", "de")));
        item.setFieldSize(500, -1);
        item.setEditable(false);
        v.add(item = new ItemTypeString(GUI_STATUS, "",
                IItemType.TYPE_PUBLIC, CAT_FAHRTENABZEICHEN, International.onlyFor("Status", "de")));
        item.setFieldSize(500, -1);
        item.setEditable(false);

        // update values of derived GUI items
        this.myItems = v;
        updateGuiItems();
        
        return v;
    }

    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[4];
        header[0] = new TableItemHeader(International.getString("Name"));
        header[1] = new TableItemHeader(International.onlyFor("Abzeichen", "de"));
        header[2] = new TableItemHeader(International.getString("Kilometer"));
        header[3] = new TableItemHeader(International.onlyFor("letzte elektr. Meldung", "de"));
        return header;
    }

    public TableItem[] getGuiTableItems() {
        PersonRecord p = getPersonRecord();
        String name = (p != null ? p.getQualifiedName() : "?");
        boolean valid = (p != null ? p.isValidAt(System.currentTimeMillis()) : false);
        TableItem[] items = new TableItem[4];
        items[0] = new TableItem(name);
        items[1] = new TableItem(getAbzeichen());
        items[2] = new TableItem(getKilometer());
        items[3] = new TableItem(getLetzteMeldungDescription());
        if (!valid) {
            items[0].setDisabled(true);
            items[1].setDisabled(true);
            items[2].setDisabled(true);
            items[3].setDisabled(true);
        }
        return items;
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType.getName().equals(PERSONID) &&
             ( (event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_LOST) ||
               (event instanceof KeyEvent && ((KeyEvent)event).getKeyChar() == '\n') ) ) {
            itemType.getValueFromGui();
            updateGuiItems();
        }
        if (itemType.getName().equals(FAHRTENHEFT) &&
             ( (event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_LOST) ||
               (event instanceof KeyEvent && ((KeyEvent)event).getKeyChar() == '\n') ) ) {
            itemType.getValueFromGui();
            updateGuiItems();
        }
    }

    private IItemType getGuiItem(String itemName) {
        for (int i=0; myItems != null && i<myItems.size(); i++) {
            if (myItems.get(i).getName().equals(itemName)) {
                return myItems.get(i);
            }
        }
        return null;
    }

    private void updateGuiItem(String itemName, String value) {
        IItemType item = getGuiItem(itemName);
        if (item == null) {
            return;
        }
        if (item instanceof ItemTypeLabel) {
            item.setDescription(value);
        } else {
            item.parseAndShowValue(value);
        }
    }

    public void updateGuiItems() {
        ItemTypeStringAutoComplete itemPerson = (ItemTypeStringAutoComplete)getGuiItem(PERSONID);
        itemPerson.getValueFromGui();
        PersonRecord person = getPersonRecord((UUID)itemPerson.getId(itemPerson.getValueFromField()));
        String sig = getGuiItem(FAHRTENHEFT).getValueFromField();
        DRVSignatur drvSignatur = (sig != null && sig.trim().length() > 0 ? new DRVSignatur(sig) : null);
        if (drvSignatur != null) {
            drvSignatur.checkSignature();
        }
        String sigName = (drvSignatur != null ? drvSignatur.getVorname() + " " + drvSignatur.getNachname() : null);

        getGuiItem(GUI_NAMEMISMATCH).setVisible(sigName != null && person != null && person.getFirstLastName() != null &&
                        !sigName.equals(person.getFirstLastName()));
        getGuiItem(ABZEICHEN).setEditable(drvSignatur == null);
        getGuiItem(KILOMETER).setEditable(drvSignatur == null);
        //@AB getGuiItem(ABZEICHENAB).setEditable(drvSignatur == null);
        //@AB getGuiItem(KILOMETERAB).setEditable(drvSignatur == null);
        if (drvSignatur != null && drvSignatur.getSignatureState() == DRVSignatur.SIG_VALID) {
            getGuiItem(ABZEICHEN).parseAndShowValue(Integer.toString(drvSignatur.getAnzAbzeichen()));
            getGuiItem(KILOMETER).parseAndShowValue(Integer.toString(drvSignatur.getGesKm()));
            //@AB getGuiItem(ABZEICHENAB).parseAndShowValue(Integer.toString(drvSignatur.getAnzAbzeichenAB()));
            //@AB getGuiItem(KILOMETERAB).parseAndShowValue(Integer.toString(drvSignatur.getGesKmAB()));
        }
        updateGuiItem(GUI_YEAROFBIRTH, (person != null && person.getBirthday() != null && person.getBirthday().isSet() ? 
            Integer.toString(person.getBirthday().getYear()) : ""));
        updateGuiItem(GUI_LETZTESFAHRTENABZEICHEN, 
                (drvSignatur != null ? drvSignatur.getJahr() + " (" + drvSignatur.getLetzteKm() + " Km)" :
                International.onlyFor("- keine elektronisches Fahrtenheft vorhanden -", "de")));
        updateGuiItem(GUI_TEILNEHMERNUMMER, (drvSignatur != null ? drvSignatur.getTeilnNr() : ""));
        updateGuiItem(GUI_VORNAME, (drvSignatur != null ? drvSignatur.getVorname() : ""));
        updateGuiItem(GUI_NACHNAME, (drvSignatur != null ? drvSignatur.getNachname() : ""));
        updateGuiItem(GUI_JAHRGANG, (drvSignatur != null ? drvSignatur.getJahrgang() : ""));
        updateGuiItem(GUI_ANZABZEICHEN, (drvSignatur != null ? Integer.toString(drvSignatur.getAnzAbzeichen()) : ""));
        updateGuiItem(GUI_ANZKM, (drvSignatur != null ? Integer.toString(drvSignatur.getGesKm()) : ""));
        updateGuiItem(GUI_ANZABZEICHENAB, (drvSignatur != null ? Integer.toString(drvSignatur.getAnzAbzeichenAB()) : ""));
        updateGuiItem(GUI_ANZKMAB, (drvSignatur != null ? Integer.toString(drvSignatur.getGesKmAB()) : ""));
        updateGuiItem(GUI_LETZTESJAHR, (drvSignatur != null ? Integer.toString(drvSignatur.getJahr()) : ""));
        updateGuiItem(GUI_LETZTEKM, (drvSignatur != null ? Integer.toString(drvSignatur.getLetzteKm()) : ""));
        updateGuiItem(GUI_LETZTESDATUM, (drvSignatur != null ? drvSignatur.getSignaturDatum(true) : ""));
        updateGuiItem(GUI_VERSION, (drvSignatur != null ? Byte.toString(drvSignatur.getVersion()) : ""));
        updateGuiItem(GUI_SCHLUESSEL, (drvSignatur != null ? Integer.toString(drvSignatur.getKeyNr()) : ""));
        updateGuiItem(GUI_SIGNATUR, (drvSignatur != null ? drvSignatur.getSignaturString() : ""));
        updateGuiItem(GUI_STATUS, (drvSignatur != null ? drvSignatur.getSignatureStateDescription() : ""));
        if (drvSignatur != null) {
            ((ItemTypeString)getGuiItem(GUI_STATUS)).setFieldColor((drvSignatur.getSignatureState() == DRVSignatur.SIG_VALID ? Color.blue : Color.red));
            ((ItemTypeString)getGuiItem(GUI_STATUS)).setUnchanged();
        }
        getGuiItem(GUI_STATUS).setVisible(drvSignatur != null);
    }

}
