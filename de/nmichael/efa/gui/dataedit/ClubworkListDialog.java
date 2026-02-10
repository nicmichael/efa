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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.Clubwork;
import de.nmichael.efa.data.ClubworkRecord;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.UUID;


// @i18n complete
public class ClubworkListDialog extends DataListDialog {

    public static final int ACTION_CARRYOVER = 4;
    public static final int ACTION_APPROVE = 5;

    public ClubworkListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Vereinsarbeit"), Daten.project.getCurrentClubwork(), 0, admin);
        iniValues();
    }

    public ClubworkListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Vereinsarbeit"), Daten.project.getCurrentClubwork(), 0, admin);
        iniValues();
    }

    private void iniValues() {
        super.sortByColumn = 2;
        super.sortAscending = false;
        if(admin == null) {
            super.filterFieldName = "Flag";
            super.filterFieldValue = ""+ClubworkRecord.Flags.Normal.ordinal();
        }
        
		//From and to columns should be wider than default
		this.minColumnWidths = new int[] {170,150,180,0,80,120};  
		this.buttonPanelPosition = (Daten.isAdminMode() ? BorderLayout.EAST : BorderLayout.NORTH);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniActions() {
        if(admin == null) {
            addAction(International.getString("Erfassen"), 
                      ItemTypeDataRecordTable.ACTION_NEW,
                      BaseDialog.IMAGE_ADD);
        } else {
            addAction(ItemTypeDataRecordTable.ACTIONTEXT_NEW,
                      ItemTypeDataRecordTable.ACTION_NEW,
                      BaseDialog.IMAGE_ADD);
            
            addAction(ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
                      ItemTypeDataRecordTable.ACTION_EDIT,
                      BaseDialog.IMAGE_EDIT);
            
            addAction(ItemTypeDataRecordTable.ACTIONTEXT_DELETE,
                      ItemTypeDataRecordTable.ACTION_DELETE,
                      BaseDialog.IMAGE_DELETE);
            
            addAction(International.getString("Importieren"),
                      ACTION_IMPORT,
                      BaseDialog.IMAGE_IMPORT);
            
            addAction(International.getString("Exportieren"),
                      ACTION_EXPORT,
                      BaseDialog.IMAGE_EXPORT);
            
            addAction(International.getString("Liste ausgeben"),
                      ACTION_PRINTLIST,
                      BaseDialog.IMAGE_LIST);
            
            if (Daten.efaConfig.getValueClubworkRequiresApproval()) {
                addAction(International.getString("Einträge bestätigen"),
                        ACTION_APPROVE,
                        BaseDialog.IMAGE_ACCEPT);
            }
            
            addAction(International.getString("Übertrag berechnen"),
                      ACTION_CARRYOVER,
                      BaseDialog.IMAGE_MERGE);
        }
    }

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

        table = new ClubworkItemTypeDataRecordTable("TABLE",
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
        table.setFieldSize((Daten.isAdminMode() ? 700 : 850), 500);
        table.setPadding(0, 0, (buttonPanelPosition.equals(BorderLayout.NORTH) ? 0 : 10), 0);
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

        super.iniControlPanel();
        mainPanel.add(mainTablePanel, BorderLayout.CENTER);

        table.setIsFilterSet(true);
        setRequestFocus(table.getSearchField());
        this.validate();

    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null) {
            record = Daten.project.getClubwork(Daten.project.getCurrentClubwork().getName(), false).createClubworkRecord(UUID.randomUUID());
        }
        return new ClubworkEditDialog(parent, (ClubworkRecord)record, newRecord, admin);
    }

    public void itemListenerActionTable(int actionId, DataRecord[] records) {
        if (actionId == ACTION_CARRYOVER) {
            Clubwork clubwork = (Clubwork)getPersistence();
            clubwork.doCarryOver(this);
        } else if (actionId == ACTION_APPROVE) {
            Clubwork clubwork = (Clubwork)getPersistence();
            if (records == null || records.length == 0) {
                return;
            }
            int res = -1;
            if (records.length == 1) {
                res = de.nmichael.efa.util.Dialog.yesNoDialog(International.getString("Eintrag bestätigen?"),
                        International.getMessage("Möchtest Du den Eintrag '{record}' bestätigen?", records[0].getQualifiedName()));
            } else {
                res = de.nmichael.efa.util.Dialog.yesNoDialog(International.getString("Eintrag bestätigen?"),
                        International.getMessage("Möchtest Du {count} ausgewählte Einträge bestätigen?", records.length));
            }
            if (res != de.nmichael.efa.util.Dialog.YES) {
                return;
            }
                try {
                    for (int i = 0; records != null && i < records.length; i++) {
                        if (records[i] != null) {
                            ((ClubworkRecord)records[i]).setApproved(true);
                            persistence.data().update(records[i]);
                        }
                    }
                } catch (EfaModifyException exmodify) {
                    exmodify.displayMessage();
                } catch (Exception ex) {
                    Logger.logdebug(ex);
                    de.nmichael.efa.util.Dialog.error(ex.toString());
                }
        } else {
            super.itemListenerActionTable(actionId, records);
        }
    }
    
	protected void createSpecificItemTypeRecordTable() {
		
		super.createSpecificItemTypeRecordTable();

		table.addPermanentSecondarySortingColumn(ClubworkRecord.COLUMN_ID_LAST_NAME);        
		table.addPermanentSecondarySortingColumn(ClubworkRecord.COLUMN_ID_FIRST_NAME);
		table.addPermanentSecondarySortingColumn(ClubworkRecord.COLUMN_ID_DATE);
		
	}
}
