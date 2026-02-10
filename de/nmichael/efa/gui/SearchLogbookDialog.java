/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.config.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class SearchLogbookDialog extends BaseTabbedDialog implements IItemListener {

    enum SearchMode {
        none,
        normal,
        special
    }

    private static EfaBaseFrame efaBaseFrame;
    private static SearchLogbookDialog searchLogbookDialog = null;
    private static Logbook logbook;
    private static DataKeyIterator it;
    private static SearchMode searchMode = SearchMode.none;
    
    private static final String CAT_NORMAL = "%01%" +  International.getString("normale Suche");
    private ItemTypeString  sSearchText;
    private ItemTypeBoolean  sSearchExact;
    private ItemTypeBoolean sEntryno;
    private ItemTypeBoolean sDate;
    private ItemTypeBoolean sEnddate;
    private ItemTypeBoolean sBoat;
    private ItemTypeBoolean sCox;
    private ItemTypeBoolean sCrew;
    private ItemTypeBoolean sStarttime;
    private ItemTypeBoolean sEndtime;
    private ItemTypeBoolean sDestination;
    private ItemTypeBoolean sDistance;
    private ItemTypeBoolean sComments;
    private ItemTypeBoolean sSessiontype;
    private ItemTypeBoolean sSessiongroup;
    private ItemTypeButton  sbAll;
    private ItemTypeButton  sbNone;

    private static final String CAT_SPECIAL = "%02%" +  International.getString("Spezialsuche");
    private ItemTypeBoolean eIncomplete;
    private ItemTypeBoolean eUnknownBoat;
    private ItemTypeBoolean eUnknownPerson;
    private ItemTypeBoolean eUnknownPersonIgnoreGuest;
    private ItemTypeBoolean eUnknownDestination;
    private ItemTypeBoolean eInvalidBoat;
    private ItemTypeBoolean eInvalidPerson;
    private ItemTypeBoolean eInvalidDestination;
    private ItemTypeBoolean eOpenEntry;
    private ItemTypeBoolean eLargeDistance;
    private ItemTypeDistance eLargeDistanceDistance;
    private ItemTypeButton  eAll;
    private ItemTypeButton  eNone;

    private SearchLogbookDialog(EfaBaseFrame parent) {
        super(parent, International.getString("Suche"), International.getStringWithMnemonic("Suchen"),
                null, false);

        IItemType item;
        Vector<IItemType> items = new Vector<IItemType>();

        items.add(sSearchText = new ItemTypeString("SEARCH_STRING", "",
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Suchbegriff")));
        sSearchText.registerItemListener(this);
        items.add(sSearchExact = new ItemTypeBoolean("SEARCH_EXACT", false,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("exakte Übereinstimmung")));
        items.add(item = new ItemTypeLabel("SEARCH_IN_FIELDS",
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Suche in folgenden Feldern")));
        item.setPadding(0, 0, 10, 0);
        items.add(sEntryno = new ItemTypeBoolean(LogbookRecord.ENTRYID, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Lfd. Nr.")));
        items.add(sDate = new ItemTypeBoolean(LogbookRecord.DATE, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Datum")));
        items.add(sEnddate = new ItemTypeBoolean(LogbookRecord.ENDDATE, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Enddatum")));
        items.add(sBoat = new ItemTypeBoolean(LogbookRecord.BOATNAME, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Boot")));
        items.add(sCox = new ItemTypeBoolean(LogbookRecord.COXNAME, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Steuermann")));
        items.add(sCrew = new ItemTypeBoolean(LogbookRecord.CREW1NAME, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Mannschaft")));
        items.add(sStarttime = new ItemTypeBoolean(LogbookRecord.STARTTIME, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Abfahrt")));
        items.add(sEndtime = new ItemTypeBoolean(LogbookRecord.ENDTIME, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Ankunft")));
        items.add(sDestination = new ItemTypeBoolean(LogbookRecord.DESTINATIONNAME, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Ziel")));
        items.add(sDistance = new ItemTypeBoolean(LogbookRecord.DISTANCE, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Kilometer")));
        items.add(sComments = new ItemTypeBoolean(LogbookRecord.COMMENTS, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Bemerkungen")));
        items.add(sSessiontype = new ItemTypeBoolean(LogbookRecord.SESSIONTYPE, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Fahrtart")));
        items.add(sSessiongroup = new ItemTypeBoolean(LogbookRecord.SESSIONGROUPID, true,
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Fahrtgruppe")));
        items.add(sbAll = new ItemTypeButton("SEARCH_SELECT_ALL",
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("alle")));
        sbAll.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
        sbAll.setPadding(0, 0, 10, 0);
        sbAll.registerItemListener(this);
        items.add(sbNone = new ItemTypeButton("SEARCH_SELECT_NONE",
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("keine")));
        sbNone.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
        sbNone.registerItemListener(this);
        items.add(item = new ItemTypeLabel("CONTINUE_SEARCH1",
                IItemType.TYPE_PUBLIC, CAT_NORMAL, International.getString("Weitersuchen mit F3")));
        item.setPadding(0, 0, 10, 0);

        items.add(eIncomplete = new ItemTypeBoolean("ESEARCH_INCOMPLETE", true,
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("unvollständige Einträge")));
        items.add(eUnknownBoat = new ItemTypeBoolean("ESEARCH_UNKNOWNBOAT", true, 
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("Einträge mit unbekannten Booten")));
        items.add(eUnknownPerson = new ItemTypeBoolean("ESEARCH_UNKNOWNPERSON", true, 
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("Einträge mit unbekannten Personen")));
        items.add(eUnknownPersonIgnoreGuest = new ItemTypeBoolean("ESEARCH_UNKNOWNPERSONIGNOREGUEST", true,
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getMessage("Unbekannte Einträge mit '{guest}' ignorieren",
                Daten.efaTypes.getValue(EfaTypes.CATEGORY_STATUS, EfaTypes.TYPE_STATUS_GUEST))));
        items.add(eUnknownDestination = new ItemTypeBoolean("ESEARCH_UNKNOWNDESTINATION", true, 
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("Einträge mit unbekannten Zielen")));
        items.add(eInvalidBoat = new ItemTypeBoolean("ESEARCH_INVALIDBOAT", true,
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("Einträge mit ungültigen Booten")));
        items.add(eInvalidPerson = new ItemTypeBoolean("ESEARCH_INVALIDPERSON", true,
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("Einträge mit ungültigen Personen")));
        items.add(eInvalidDestination = new ItemTypeBoolean("ESEARCH_INVALIDDESTINATION", true,
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("Einträge mit ungültigen Zielen")));
        items.add(eOpenEntry = new ItemTypeBoolean("ESEARCH_OPENENTRY", true,
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("nicht zurückgetragene Einträge")));
        items.add(eLargeDistance = new ItemTypeBoolean("ESEARCH_LARGEDISTANCE", true,
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("Einträge mit Kilometern größer als") + ": "));
        items.add(eLargeDistanceDistance = new ItemTypeDistance("ESEARCH_LARGEDISTANCEDISTANCE", DataTypeDistance.parseDistance("30 "+DataTypeDistance.KILOMETERS, true),
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, null));
        eLargeDistance.setFieldGrid(1, GridBagConstraints.WEST, GridBagConstraints.NONE);
        eLargeDistanceDistance.setOffsetXY(1, -1); // show behind eLargeDistance Checkbox
        eLargeDistanceDistance.setFieldSize(100, -1);

        items.add(eAll = new ItemTypeButton("ESEARCH_SELECT_ALL",
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("alle")));
        eAll.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
        eAll.setPadding(0, 0, 10, 0);
        eAll.registerItemListener(this);
        items.add(eNone = new ItemTypeButton("ESEARCH_SELECT_NONE",
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("keine")));
        eNone.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
        eNone.registerItemListener(this);
        items.add(item = new ItemTypeLabel("CONTINUE_SEARCH2",
                IItemType.TYPE_PUBLIC, CAT_SPECIAL, International.getString("Weitersuchen mit F3")));
        item.setPadding(0, 0, 10, 0);

        setItems(items);
    }

    public static void initialize(EfaBaseFrame parent, Logbook logbook, DataKeyIterator iterator) {
        efaBaseFrame = parent;
        SearchLogbookDialog.logbook = logbook;
        SearchLogbookDialog.it = iterator;
    }

    public static boolean isInitialized() {
        if (efaBaseFrame == null || logbook == null || it == null) {
            return false;
        }
        return true;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        super.iniDialog();
        closeButton.setIcon(getIcon(BaseDialog.IMAGE_SEARCH));
    }

    public static void showSearchDialog(EfaBaseFrame parent, Logbook logbook, DataKeyIterator iterator,
            String searchString) {
        if (searchLogbookDialog == null || parent != efaBaseFrame) {
            searchLogbookDialog = new SearchLogbookDialog(parent);
        }
        initialize(parent, logbook, iterator);
        if (CAT_NORMAL.equals(searchLogbookDialog.getSelectedPanel((JTabbedPane)searchLogbookDialog.topLevelPane))) {
            searchLogbookDialog.setRequestFocus(searchLogbookDialog.sSearchText);
        }
        if (searchString != null) {
            searchLogbookDialog.sSearchText.parseAndShowValue(searchString);
        }
        searchLogbookDialog.showDialog();
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType == sbAll && event.getID() == ActionEvent.ACTION_PERFORMED) {
            selectAllSearchFields(true, false);
        }
        if (itemType == sbNone && event.getID() == ActionEvent.ACTION_PERFORMED) {
            selectAllSearchFields(false, false);
        }
        if (itemType == eAll && event.getID() == ActionEvent.ACTION_PERFORMED) {
            selectAllSearchFields(true, true);
        }
        if (itemType == eNone && event.getID() == ActionEvent.ACTION_PERFORMED) {
            selectAllSearchFields(false, true);
        }
        if (itemType == sSearchText && event.getID() == KeyEvent.KEY_PRESSED &&
                ((KeyEvent)event).getKeyCode() == KeyEvent.VK_ENTER) {
            closeButton_actionPerformed(null);
        }
    }

    private void selectAllSearchFields(boolean selected, boolean special) {
        if (!special) {
            sEntryno.parseAndShowValue(Boolean.toString(selected));
            sDate.parseAndShowValue(Boolean.toString(selected));
            sEnddate.parseAndShowValue(Boolean.toString(selected));
            sBoat.parseAndShowValue(Boolean.toString(selected));
            sCox.parseAndShowValue(Boolean.toString(selected));
            sCrew.parseAndShowValue(Boolean.toString(selected));
            sStarttime.parseAndShowValue(Boolean.toString(selected));
            sEndtime.parseAndShowValue(Boolean.toString(selected));
            sDestination.parseAndShowValue(Boolean.toString(selected));
            sDistance.parseAndShowValue(Boolean.toString(selected));
            sComments.parseAndShowValue(Boolean.toString(selected));
            sSessiontype.parseAndShowValue(Boolean.toString(selected));
            sSessiongroup.parseAndShowValue(Boolean.toString(selected));
        } else {
            eIncomplete.parseAndShowValue(Boolean.toString(selected));
            eUnknownBoat.parseAndShowValue(Boolean.toString(selected));
            eUnknownPerson.parseAndShowValue(Boolean.toString(selected));
            eUnknownPersonIgnoreGuest.parseAndShowValue(Boolean.toString(selected));
            eUnknownDestination.parseAndShowValue(Boolean.toString(selected));
            eInvalidBoat.parseAndShowValue(Boolean.toString(selected));
            eInvalidPerson.parseAndShowValue(Boolean.toString(selected));
            eInvalidDestination.parseAndShowValue(Boolean.toString(selected));
            eOpenEntry.parseAndShowValue(Boolean.toString(selected));
            eLargeDistance.parseAndShowValue(Boolean.toString(selected));
        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        if (getSelectedPanel((JTabbedPane)topLevelPane).equals(CAT_NORMAL)) {
            searchMode = SearchMode.normal;
        }
        if (getSelectedPanel((JTabbedPane)topLevelPane).equals(CAT_SPECIAL)) {
            searchMode = SearchMode.special;
        }
        getValuesFromGui();
        setDialogResult(true);
        super.closeButton_actionPerformed(e);
        search();
    }

    private static boolean tryMatch(String s, Object o, boolean exact, boolean selected) {
        if (!selected) {
            return false;
        }
        if (o == null) {
            return false;
        }
        String f = o.toString();
        if (!exact) {
            f = f.trim().toLowerCase();
        }
        if (f.length() == 0) {
            return false;
        }
        if ( (o instanceof DataTypeDate && EfaUtil.countCharInString(s,'.') == 2) ||
             (o instanceof DataTypeTime && EfaUtil.countCharInString(s,':') == 1) ) {
            return (f.equals(s));
        }
        if (exact) {
            return f.equals(s);
        } else {
            return (f.indexOf(s) >= 0);
        }
    }

    private static void foundMatch(LogbookRecord r, IItemType item, boolean jumpToField) {
        efaBaseFrame.setFields(r);
        if (jumpToField) {
            item.requestFocus();
            if (item instanceof ItemTypeStringAutoComplete) {
                item.showValue(); // to force autocomplete and detection of color
            }
        }
    }

    public static boolean search() {
        if (searchLogbookDialog == null) {
            return false;
        }
        boolean exact = searchLogbookDialog.sSearchExact.getValue();
        String s = searchLogbookDialog.sSearchText.getValue();
        if (!exact) {
            s = s.trim().toLowerCase();
        }
        efaBaseFrame.toolBar_goToEntry.setText( (searchMode == SearchMode.normal ? s : ""));
        return search(s, exact, searchMode, true, true);
    }

    public static boolean search(String s, boolean exact,
            SearchMode mode, boolean startWithNext, boolean jumpToField) {
        if (mode == SearchMode.normal && s.length() == 0) {
            Dialog.error(International.getString("Bitte gib einen Suchbegriff ein!"));
            return false;
        }
        if (!isInitialized()) {
            return false;
        }
        try {
            Boats boats = Daten.project.getBoats(false);
            Persons persons = Daten.project.getPersons(false);
            Destinations destinations = Daten.project.getDestinations(false);

            DataKey k = (startWithNext ? it.getNext() : it.getCurrent());
            while (true) {
                if (k == null) {
                    if (JOptionPane.showConfirmDialog(efaBaseFrame, International.getString("Keinen Eintrag gefunden!") + ""
                            + "\n" + International.getString("Suche vom Anfang an fortsetzen?"),
                            International.getString("Nicht gefunden"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        k = it.getFirst();
                    } else {
                        break;
                    }
                }

                LogbookRecord r = logbook.getLogbookRecord(k);
                if (r == null) {
                    k = it.getNext();
                    continue;
                }
                if (mode == SearchMode.normal) {
                    if (tryMatch(s, r.getEntryId(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sEntryno.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.entryno, jumpToField);
                        return true;
                    }
                    if (tryMatch(s, r.getDate(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sDate.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.date, jumpToField);
                        return true;
                    }
                    if (tryMatch(s, r.getEndDate(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sEnddate.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.enddate, jumpToField);
                        return true;
                    }
                    if (tryMatch(s, r.getBoatAsName(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sBoat.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.boat, jumpToField);
                        return true;
                    }
                    if (tryMatch(s, r.getCoxAsName(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sCox.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.cox, jumpToField);
                        return true;
                    }
                    for (int i = 0; i < LogbookRecord.CREW_MAX; i++) {
                        if (tryMatch(s, r.getCrewAsName(i + 1), exact,
                                (searchLogbookDialog != null ? searchLogbookDialog.sCrew.getValue() : true))) {
                            foundMatch(r, efaBaseFrame.crew[i], jumpToField);
                            return true;
                        }
                    }
                    if (tryMatch(s, r.getStartTime(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sStarttime.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.starttime, jumpToField);
                        return true;
                    }
                    if (tryMatch(s, r.getEndTime(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sEndtime.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.endtime, jumpToField);
                        return true;
                    }
                    if (tryMatch(s, r.getDestinationAndVariantName(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sDestination.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.destination, jumpToField);
                        return true;
                    }
                    if (tryMatch(s, r.getDistance(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sDistance.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.distance, jumpToField);
                        return true;
                    }
                    if (tryMatch(s, r.getComments(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sComments.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.comments, jumpToField);
                        return true;
                    }
                    if (tryMatch(s, Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, r.getSessionType()), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sSessiontype.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.sessiontype, jumpToField);
                        return true;
                    }
                    if (tryMatch(s, r.getSessionGroupAsName(), exact,
                            (searchLogbookDialog != null ? searchLogbookDialog.sSessiongroup.getValue() : true))) {
                        foundMatch(r, efaBaseFrame.sessiongroup, jumpToField);
                        return true;
                    }
                }
                if (mode == SearchMode.special) {
                    long validAt = r.getValidAtTimestamp();
                    if (searchLogbookDialog != null && searchLogbookDialog.eIncomplete.getValue()) {
                        if (r.getBoatAsName() == null || r.getBoatAsName().length() == 0) {
                            foundMatch(r, efaBaseFrame.boat, jumpToField);
                            return true;
                        }
                        if (r.getDestinationAndVariantName() == null || r.getDestinationAndVariantName().length() == 0) {
                            foundMatch(r, efaBaseFrame.destination, jumpToField);
                            return true;
                        }
                        if (r.getDistance() == null || !r.getDistance().isSet() ||
                            r.getDistance().getValueInDefaultUnit() == 0) {
                            foundMatch(r, efaBaseFrame.distance, jumpToField);
                            return true;
                        }
                        if (r.getAllCoxAndCrewAsNames().size() == 0) {
                            foundMatch(r, efaBaseFrame.crew[0], jumpToField);
                            return true;
                        }
                    }
                    if (searchLogbookDialog != null && searchLogbookDialog.eUnknownBoat.getValue()) {
                        if (r.getBoatId() == null) {
                            foundMatch(r, efaBaseFrame.boat, jumpToField);
                            return true;
                        }
                    }
                    if (searchLogbookDialog != null && searchLogbookDialog.eUnknownPerson.getValue()) {
                        for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                            if (r.getCrewId(i) == null && r.getCrewName(i) != null && r.getCrewName(i).length() > 0) {
                                if (searchLogbookDialog.eUnknownPersonIgnoreGuest.getValue() &&
                                    r.getCrewName(i).toLowerCase().indexOf(Daten.efaTypes.getValue(EfaTypes.CATEGORY_STATUS, EfaTypes.TYPE_STATUS_GUEST).toLowerCase())  >= 0) {
                                    continue;
                                }
                                if (i == 0) {
                                    foundMatch(r, efaBaseFrame.cox, jumpToField);
                                } else {
                                    foundMatch(r, efaBaseFrame.crew[i-1], jumpToField);
                                }
                                return true;
                            }
                        }
                    }
                    if (searchLogbookDialog != null && searchLogbookDialog.eUnknownDestination.getValue()) {
                        if (r.getDestinationId() == null) {
                            foundMatch(r, efaBaseFrame.destination, jumpToField);
                            return true;
                        }
                    }
                    if (searchLogbookDialog != null && searchLogbookDialog.eInvalidBoat.getValue()) {
                        boolean invalid = false;
                        if (r.getBoatId() != null &&
                            boats.getBoat(r.getBoatId(), validAt) == null) {
                            invalid = true; // UUID that is currently invalid
                        }
                        if (r.getBoatId() == null ||
                            r.getBoatName() != null && r.getBoatName().length() > 0) {
                            BoatRecord br = boats.getBoat(r.getBoatName(), 0, Long.MAX_VALUE, validAt);
                            if (br != null && !br.isValidAt(validAt)) {
                                invalid = true;
                            }
                        }
                        if (invalid) {
                            foundMatch(r, efaBaseFrame.boat, jumpToField);
                            return true;
                        }
                    }
                    if (searchLogbookDialog != null && searchLogbookDialog.eInvalidPerson.getValue()) {
                        for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                            boolean invalid = false;
                            if (r.getCrewId(i) != null
                                    && persons.getPerson(r.getCrewId(i), validAt) == null) {
                                invalid = true; // UUID that is currently invalid
                            }
                            if (r.getCrewId(i) == null
                                    || r.getCrewName(i) != null && r.getCrewName(i).length() > 0) {
                                PersonRecord pr = persons.getPerson(r.getCrewName(i), 0, Long.MAX_VALUE, validAt);
                                if (pr != null && !pr.isValidAt(validAt)) {
                                    invalid = true;
                                }
                            }
                            if (invalid) {
                                foundMatch(r, (i == 0 ? efaBaseFrame.cox : efaBaseFrame.crew[i - 1]),
                                        jumpToField);
                                return true;
                            }
                        }
                    }
                    if (searchLogbookDialog != null && searchLogbookDialog.eInvalidDestination.getValue()) {
                        boolean invalid = false;
                        if (r.getDestinationId() != null &&
                            destinations.getDestination(r.getDestinationId(), validAt) == null) {
                            invalid = true; // UUID that is currently invalid
                        }
                        if (r.getDestinationId() == null ||
                            r.getDestinationName() != null && r.getDestinationName().length() > 0) {
                            DestinationRecord br = destinations.getDestination(r.getDestinationName(), 0, Long.MAX_VALUE, validAt);
                            if (br != null && !br.isValidAt(validAt)) {
                                invalid = true;
                            }
                        }
                        if (invalid) {
                            foundMatch(r, efaBaseFrame.destination, jumpToField);
                            return true;
                        }
                    }
                    if (searchLogbookDialog != null && searchLogbookDialog.eOpenEntry.getValue()) {
                        if (r.getDistance() != null && (!r.getDistance().isSet() || r.getDistance().getValueInMeters() == 0)) {
                            foundMatch(r, efaBaseFrame.distance, jumpToField);
                            return true;
                        }
                    }
                    if (searchLogbookDialog != null && searchLogbookDialog.eLargeDistance.getValue() &&
                            searchLogbookDialog.eLargeDistanceDistance.getValue().getValueInMeters() > 0) {
                        if (r.getDistance() != null && r.getDistance().isSet() &&
                            r.getDistance().getValueInMeters() >= searchLogbookDialog.eLargeDistanceDistance.getValue().getValueInMeters()) {
                            foundMatch(r, efaBaseFrame.distance, jumpToField);
                            return true;
                        }
                    }
                }
                k = it.getNext();
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return false;
    }


}
