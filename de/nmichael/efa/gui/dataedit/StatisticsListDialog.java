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
import java.awt.Color;
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
import de.nmichael.efa.data.Statistics;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.statistics.StatisticTask;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;


// @i18n complete
public class StatisticsListDialog extends DataListDialog {

    public static final int ACTION_CREATESTATISTICS = 901; // negative actions will not be shown as popup actions
    public static final int ACTION_ONETIMESTATISTIC = 902; // negative actions will not be shown as popup actions
    public static final int ACTION_MOVEUP          = 1001; // positive actions > 2000 will be shown as popups + small buttons without text
    public static final int ACTION_MOVEDOWN        = 1002; // positive actions > 2000 will be shown as popups + small buttons without text

    private AdminRecord admin;

    public StatisticsListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Statistiken"), 
                Daten.project.getStatistics(false), 0, admin);
        this.admin = admin;
        ini();
    }

    public StatisticsListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Statistiken"), 
                Daten.project.getStatistics(false), 0, admin);
        this.admin = admin;
        ini();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    private void ini() {
        if (admin == null) {
            this.filterFieldName  = StatisticsRecord.PUBLICLYAVAILABLE;
            this.filterFieldValue = Boolean.toString(true);
            this.tableFontSize = Dialog.getFontSize();
            this.buttonPanelPosition = BorderLayout.SOUTH;
            StatisticsRecord.TABLE_HEADER_LONG = false;
        } else {
            StatisticsRecord.TABLE_HEADER_LONG = true;
            this.markedCellColor = Color.blue;
            //this.markedCellBold = true;
        }

        if (admin != null && admin.isAllowedEditStatistics()) {
            actionText = new String[]{
                        ItemTypeDataRecordTable.ACTIONTEXT_NEW,
                        ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
                        ItemTypeDataRecordTable.ACTIONTEXT_DELETE,
                        International.getString("Statistik erstellen"),
                        International.getString("Einmalige Statistik"),
                        International.getString("eine Position hoch"),
                        International.getString("eine Position runter")
                    };
            actionType = new int[]{
                        ItemTypeDataRecordTable.ACTION_NEW,
                        ItemTypeDataRecordTable.ACTION_EDIT,
                        ItemTypeDataRecordTable.ACTION_DELETE,
                        ACTION_CREATESTATISTICS,
                        ACTION_ONETIMESTATISTIC,
                        ACTION_MOVEUP,
                        ACTION_MOVEDOWN
                    };
            actionImage = new String[]{
                        IMAGE_ADD,
                        IMAGE_EDIT,
                        IMAGE_DELETE,
                        IMAGE_RUN,
                        IMAGE_STAT,
                        ItemTypeDataRecordTable.BUTTON_IMAGE_CENTERED_PREFIX + IMAGE_ARROWUP,
                        ItemTypeDataRecordTable.BUTTON_IMAGE_CENTERED_PREFIX + IMAGE_ARROWDOWN
                    };
        } else {
            actionText = new String[]{
                        International.getString("Statistik erstellen")
                    };
            actionType = new int[]{
                        ACTION_CREATESTATISTICS
                    };
            actionImage = new String[]{
                        ItemTypeDataRecordTable.BUTTON_IMAGE_CENTERED_PREFIX + IMAGE_STAT
                    };
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

        table = new StatisticsItemTypeDataRecordTable("TABLE",
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
    	
        //übernommen aus der ursprünglichen iniDialog() Methode.
        table.setDefaultActionForDoubleclick(ACTION_CREATESTATISTICS);
        table.setIsFilterSet(true);
        if (admin == null) {
            table.setVisibleSearchPanel(false);
        }
    }

    public void itemListenerActionTable(int actionId, DataRecord[] records) {
        super.itemListenerActionTable(actionId, records);
        StatisticsRecord[] sr;
        switch(actionId) {
            case ACTION_CREATESTATISTICS:
                if (records == null || records.length == 0 || records[0] == null) {
                    return;
                }
                sr = new StatisticsRecord[records.length];
                for (int i=0; i<records.length; i++) {
                    sr[i] = (StatisticsRecord)records[i];
                }
                StatisticTask.createStatisticsTask(null, this, sr, admin);
                break;
            case ACTION_ONETIMESTATISTIC:
                StatisticsRecord srtmp = (StatisticsRecord)Daten.project.getStatistics(false).createStatisticsRecord(UUID.randomUUID());
                StatisticsEditDialog dlg = new StatisticsEditDialog(this, srtmp, true, true, admin);
                dlg.showDialog();
                break;
            case ACTION_MOVEUP:
            case ACTION_MOVEDOWN:
                if (records == null || records.length == 0 || records[0] == null) {
                    return;
                }
                if (records.length > 1) {
                    Dialog.error(International.getString("Bitte wähle nur einen Datensatz aus!"));
                    return;
                }
                ((Statistics)persistence).moveRecord((StatisticsRecord)records[0],
                        (actionId == ACTION_MOVEUP ? -1 : 1));
                table.setMoveRowSelectionUponNextRefresh((actionId == ACTION_MOVEUP ? -1 : 1));
                break;
        }
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null) {
            record = Daten.project.getStatistics(false).createStatisticsRecord(UUID.randomUUID());
        }
        return new StatisticsEditDialog(parent, (StatisticsRecord)record, newRecord, admin);
    }
}
