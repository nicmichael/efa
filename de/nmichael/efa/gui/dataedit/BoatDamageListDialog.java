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
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.data.BoatDamageRecord;
import de.nmichael.efa.data.BoatDamages;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;


// @i18n complete
public class BoatDamageListDialog extends DataListDialog {

    public BoatDamageListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Bootsschäden"), Daten.project.getBoatDamages(false), 0, admin);
        iniValues(null);
    }

    public BoatDamageListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Bootsschäden"), Daten.project.getBoatDamages(false), 0, admin);
        iniValues(null);
    }

    public BoatDamageListDialog(Frame parent, UUID boatId, AdminRecord admin) {
        super(parent, International.getString("Bootsschäden"), Daten.project.getBoatDamages(false), 0, admin);
        iniValues(boatId);
    }

    public BoatDamageListDialog(JDialog parent, UUID boatId, AdminRecord admin) {
        super(parent, International.getString("Bootsschäden"), Daten.project.getBoatDamages(false), 0, admin);
        iniValues(boatId);
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

        table = new BoatDamageItemTypeDataRecordTable("TABLE",
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
        
        table.setIsFilterSet(true);// in der Bootsschadensliste immer filtern, statt mit der Eingabe zu einem Zellwert sprinten
        
    }    
    
    
    private void iniValues(UUID boatId) {
        if (boatId != null) {
            this.filterFieldName  = BoatReservationRecord.BOATID;
            this.filterFieldValue = boatId.toString();
        }
        super.sortByColumn = 4;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null && persistence != null && filterFieldValue != null) {
            record = ((BoatDamages)persistence).createBoatDamageRecord(UUID.fromString(filterFieldValue));
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
                        record = ((BoatDamages)persistence).createBoatDamageRecord(boats.getBoat(s, now).getId());
                        ((BoatDamageRecord)record).setReportDate(DataTypeDate.today());
                        ((BoatDamageRecord)record).setReportTime(DataTypeTime.now());
                    }
                } catch(Exception e) {
                    Logger.logdebug(e);
                }
            }
        }
        if (record == null) {
            return null;
        }
        return new BoatDamageEditDialog(parent, (BoatDamageRecord)record, newRecord, admin);
    }

    // @Override
    public boolean deleteCallback(DataRecord[] records) {
        BoatDamageRecord unfixedDamage = null;
        for (int i=0; records != null && i<records.length; i++) {
            if (records[i] != null && !((BoatDamageRecord)records[i]).getFixed()) {
                unfixedDamage = (BoatDamageRecord)records[i];
                break;
            }
        }
        if (unfixedDamage == null) {
            return true;
        }

        switch(Dialog.auswahlDialog(International.getString("Bootsschaden löschen"),
                International.getString("Möchtest du den Bootsschaden als behoben markieren, oder " +
                                        "einen irrtümlich gemeldeten Schaden komplett löschen?"),
                International.getString("als behoben markieren"),
                International.getString("irrtümlich gemeldeten Schaden löschen"))) {
            case 0:
                BoatDamageEditDialog dlg = (BoatDamageEditDialog)createNewDataEditDialog(this, persistence, unfixedDamage);
                ItemTypeBoolean fixed = (ItemTypeBoolean)dlg.getItem(BoatDamageRecord.FIXED);
                if (fixed != null) {
                    fixed.setValue(true);
                    fixed.setChanged();
                    dlg.itemListenerAction(fixed, null);
                    dlg.setFixedWasChanged();
                }
                IItemType focus = dlg.getItem(BoatDamageRecord.FIXEDBYPERSONID);
                if (focus != null) {
                    dlg.setRequestFocus(focus);
                }
                dlg.showDialog();
                return false;
            case 1:
                return true;
            default:
                return false;
        }
    }

}
