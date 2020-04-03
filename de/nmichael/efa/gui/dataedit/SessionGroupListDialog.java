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

import de.nmichael.efa.*;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.util.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;


// @i18n complete
public class SessionGroupListDialog extends DataListDialog {

    public static final int ACTION_SELECT  =  300;
    public static final int ACTION_UNSELECT=  301;

    private String logbook;
    private UUID selectedSessionGroupId;
    private boolean modeSelectSessionGroup;
    private SessionGroupRecord selectedRecord;

    public SessionGroupListDialog(Frame parent, String logbook, AdminRecord admin) {
        super(parent, International.getString("Fahrtgruppen"), 
                Daten.project.getSessionGroups(false), 0, admin);
        iniValues(logbook, null, false);
    }

    public SessionGroupListDialog(JDialog parent, String logbook, AdminRecord admin) {
        super(parent, International.getString("Fahrtgruppen"), 
                Daten.project.getSessionGroups(false), 0, admin);
        iniValues(logbook, null, false);
    }

    public SessionGroupListDialog(Frame parent, String logbook, UUID selectedSessionGroupId, AdminRecord admin) {
        super(parent, International.getString("Fahrtgruppen"), 
                Daten.project.getSessionGroups(false), 0, admin);
        iniValues(logbook, selectedSessionGroupId, true);
    }

    public SessionGroupListDialog(JDialog parent, String logbook, UUID selectedSessionGroupId, AdminRecord admin) {
        super(parent, International.getString("Fahrtgruppen"), 
                Daten.project.getSessionGroups(false), 0, admin);
        iniValues(logbook, selectedSessionGroupId, true);
    }

    
    
    
    private void iniValues(String logbook, UUID selectedSessionGroupId, boolean modeSelectSessionGroup) {
        this.logbook = logbook;
        this.selectedSessionGroupId = selectedSessionGroupId;
        this.modeSelectSessionGroup = modeSelectSessionGroup;
        if (logbook != null) {
            this.filterFieldName  = SessionGroupRecord.LOGBOOK;
            this.filterFieldValue = logbook;
        }
        if (modeSelectSessionGroup) {
            actionText = new String[] {
                ItemTypeDataRecordTable.ACTIONTEXT_NEW,
                ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
                ItemTypeDataRecordTable.ACTIONTEXT_DELETE,
                International.getString("Fahrtgruppe auswählen"),
                International.getString("Auswahl aufheben")
            };
            actionType = new int[] {
                ItemTypeDataRecordTable.ACTION_NEW,
                ItemTypeDataRecordTable.ACTION_EDIT,
                ItemTypeDataRecordTable.ACTION_DELETE,
                ACTION_SELECT,
                ACTION_UNSELECT
            };
        }
        intelligentColumnWidth = false;
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

        table = new SessionGroupItemTypeDataRecordTable("TABLE",
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
        
        //uebernommen aus bisheriger iniDialog() Methode von SessionGroupListDialog
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultActionForDoubleclick(ACTION_SELECT);
        if (selectedSessionGroupId != null) {
            table.selectValue(selectedSessionGroupId.toString());
        }
        
        table.setIsFilterSet(true);// in der Fahrtgruppenliste immer filtern, statt mit der Eingabe zu einem Zellwert sprinten

    }        
    

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void itemListenerActionTable(int actionId, DataRecord[] records) {
        super.itemListenerActionTable(actionId, records);
        switch(actionId) {
            case ACTION_SELECT:
                if (records != null && records.length == 1 && records[0] != null) {
                    selectedRecord = (SessionGroupRecord)records[0];
                    setDialogResult(true);
                    cancel();
                }
                break;
            case ACTION_UNSELECT:
                selectedRecord = null;
                setDialogResult(true);
                cancel();
                break;
        }
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null && persistence != null && filterFieldValue != null) {
            record = ((SessionGroups)persistence).createSessionGroupRecord(UUID.randomUUID(), filterFieldValue);
        }
        if (record == null) {
            return null;
        }
        if (logbook != null && logbook.length() > 0) {
            ((SessionGroupRecord)record).setLogbook(logbook);
        }
        return new SessionGroupEditDialog(parent, (SessionGroupRecord)record, newRecord, admin);
    }

    public SessionGroupRecord getSelectedSessionGroupRecord() {
        return selectedRecord;
    }
    
}
