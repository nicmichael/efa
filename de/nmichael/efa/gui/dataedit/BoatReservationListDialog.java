/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.UUID;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.BoatStatusRecord;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;


// @i18n complete
public class BoatReservationListDialog extends DataListDialog {

    boolean allowNewReservationsWeekly = true;

    public BoatReservationListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(null, true, true, true);
    }

    public BoatReservationListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(null, true, true, true);
    }

    public BoatReservationListDialog(Frame parent, UUID boatId, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(boatId, true, true, true);
    }

    public BoatReservationListDialog(JDialog parent, UUID boatId, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(boatId, true, true, true);
    }

    public BoatReservationListDialog(Frame parent, UUID boatId, boolean allowNewReservations, boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, null);
        iniValues(boatId, allowNewReservations, allowNewReservationsWeekly, allowEditDeleteReservations);
    }

    public BoatReservationListDialog(JDialog parent, UUID boatId, boolean allowNewReservations, boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, null);
        iniValues(boatId, allowNewReservations, allowNewReservationsWeekly, allowEditDeleteReservations);
    }

   
    // Methode übernommen von DataListDialog - Ausnahme ist, dass wir hier einen
    // expliziten BoatReservationItemTypeDataRecordTable setzen, damit man besser filtern kann.
    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());
        
        JPanel mainTablePanel = new JPanel();
        mainTablePanel.setLayout(new BorderLayout());

        if (filterFieldDescription != null) {
            JLabel filterName = new JLabel();
            filterName.setText(filterFieldDescription);
            filterName.setHorizontalAlignment(SwingConstants.CENTER);
            mainTablePanel.add(filterName, BorderLayout.NORTH);
            mainTablePanel.setBorder(new EmptyBorder(10,0,0,0));
        }

        table = new BoatReservationItemTypeDataRecordTable("TABLE",
                persistence.createNewRecord().getGuiTableHeader(),
                persistence, validAt, admin,
                filterFieldName, filterFieldValue, // defaults are null
                actionText, actionType, actionImage, // default actions: new, edit, delete
                this,
                IItemType.TYPE_PUBLIC, "BASE_CAT", getTitle());
        table.setSorting(sortByColumn, sortAscending);
        table.setFontSize(tableFontSize);
        table.setMarkedCellColor(markedCellColor);
        table.setMarkedCellBold(markedCellBold);
        table.disableIntelligentColumnWidth(!intelligentColumnWidth);
        if (minColumnWidth > 0) {
            table.setMinColumnWidth(minColumnWidth);
        }
        if (minColumnWidths != null) {
            table.setMinColumnWidths(minColumnWidths);
        }
        table.setButtonPanelPosition(buttonPanelPosition);
        table.setFieldSize(600, 500);
        table.setPadding(0, 0, 10, 0);
        table.displayOnGui(this, mainTablePanel, BorderLayout.CENTER);

        boolean hasEditAction = false;
        for (int i=0; actionType != null && i < actionType.length; i++) {
            if (actionType[i] == ItemTypeDataRecordTable.ACTION_EDIT) {
                hasEditAction = true;
            }
        }
        if (!hasEditAction) {
            table.setDefaultActionForDoubleclick(-1);
        }

        iniControlPanel();
        mainPanel.add(mainTablePanel, BorderLayout.CENTER);

        setRequestFocus(table);
        this.validate();
        
        table.setIsFilterSet(true);// in der Bootreservierungsliste immer filtern, statt mit der Eingabe zu einem Zellwert sprinten
    }    
    
    
    private void iniValues(UUID boatId, boolean allowNewReservations, boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
        if (boatId != null) {
            this.filterFieldName  = BoatReservationRecord.BOATID;
            this.filterFieldValue = boatId.toString();
            if (Daten.project != null) {
                Boats boats = Daten.project.getBoats(false);
                if (boats != null) {
                    BoatRecord r = boats.getBoat(boatId, System.currentTimeMillis());
                    if (r != null) {
                        this.filterFieldDescription = International.getString("Boot") + ": " +
                                r.getQualifiedName();
                    }
                }
            }
        }
        if (allowNewReservations && allowEditDeleteReservations) {
            if (admin != null) {
                // default: ADD, EDIT, DELETE, IMPORT, EXPORT
            } else {
                actionText = new String[]{
                            ItemTypeDataRecordTable.ACTIONTEXT_NEW,
                            ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
                            ItemTypeDataRecordTable.ACTIONTEXT_DELETE
                        };
                actionType = new int[]{
                            ItemTypeDataRecordTable.ACTION_NEW,
                            ItemTypeDataRecordTable.ACTION_EDIT,
                            ItemTypeDataRecordTable.ACTION_DELETE
                        };
            }
        } else if (allowNewReservations) {
            actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_NEW };
            actionType = new int[] { ItemTypeDataRecordTable.ACTION_NEW };
        } else if (allowEditDeleteReservations) {
            actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_EDIT, ItemTypeDataRecordTable.ACTIONTEXT_DELETE };
            actionType = new int[] { ItemTypeDataRecordTable.ACTION_EDIT, ItemTypeDataRecordTable.ACTION_DELETE };
        } else {
            actionText = new String[] { };
            actionType = new int[] { };
        }
        this.allowNewReservationsWeekly = allowNewReservationsWeekly;
    }


    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null && persistence != null && filterFieldValue != null) {
            record = ((BoatReservations)persistence).createBoatReservationsRecord(UUID.fromString(filterFieldValue));
        }
        if (record == null) {
            long now = System.currentTimeMillis();
            ItemTypeStringAutoComplete boat = new ItemTypeStringAutoComplete("BOAT", "", IItemType.TYPE_PUBLIC,
                    "", International.getString("Boot"), false);
            boat.setAutoCompleteData(new AutoCompleteList(Daten.project.getBoats(false).data(), now, now));
            if (SimpleInputDialog.showInputDialog(this, International.getString("Boot auswählen"), boat)) {
                String s = boat.toString();
                try {
                    if (s != null && s.length() > 0) {
                        Boats boats = Daten.project.getBoats(false);
                        record = ((BoatReservations)persistence).createBoatReservationsRecord(boats.getBoat(s, now).getId());
                    }
                } catch(Exception e) {
                    Logger.logdebug(e);
                }
            }
        }
        if (record == null) {
            return null;
        }
        if (admin == null) {
            try {
                Boats boats = Daten.project.getBoats(false);
                BoatRecord b = boats.getBoat(((BoatReservationRecord)record).getBoatId(), System.currentTimeMillis());
                if (b.getOwner() != null && b.getOwner().length() > 0 &&
                    !Daten.efaConfig.getValueMembersMayReservePrivateBoats()) {
                    Dialog.error(International.getString("Privatboote dürfen nicht reserviert werden!"));
                    return null;
                }
                BoatStatusRecord bs = b.getBoatStatus();
                if (bs != null && BoatStatusRecord.STATUS_NOTAVAILABLE.equals(bs.getBaseStatus()) &&
                    Dialog.yesNoDialog(International.getString("Boot nicht verfügbar"), 
                            International.getMessage("Das ausgewählte Boot '{boat}' ist derzeit nicht verfügbar:", b.getQualifiedName()) + "\n" +
                            International.getString("Status") + ": " + bs.getComment() + "\n" +
                            International.getString("Möchtest Du das Boot trotzdem reservieren?")) != Dialog.YES) {
                    return null;
                }
            } catch(Exception e) {
                Logger.logdebug(e);
                return null;
            }
        }

        try {
            return new BoatReservationEditDialog(parent, (BoatReservationRecord) record,
                    newRecord, allowNewReservationsWeekly, admin);
        } catch (Exception e) {
            Dialog.error(e.getMessage());
            return null;
        }
    }
}
