/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.core.items;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.gui.dataedit.VersionizedDataDeleteDialog;
import de.nmichael.efa.gui.dataedit.DataEditDialog;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.ex.*;
import de.nmichael.efa.gui.BaseDialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.*;

// @i18n complete
public class ItemTypeDataRecordTable extends ItemTypeTable implements IItemListener {

    public static final int ACTION_NEW = 0;
    public static final int ACTION_EDIT = 1;
    public static final int ACTION_DELETE = 2;
    public static final int ACTION_OTHER = -1;
    public static final String ACTIONTEXT_NEW = International.getString("Neu");
    public static final String ACTIONTEXT_EDIT = International.getString("Bearbeiten");
    public static final String ACTIONTEXT_DELETE = International.getString("Löschen");
    public static final String BUTTON_IMAGE_CENTERED_PREFIX = "%";
    private static final String[] DEFAULT_ACTIONS = new String[]{
        ACTIONTEXT_NEW,
        ACTIONTEXT_EDIT,
        ACTIONTEXT_DELETE
    };
    protected StorageObject persistence;
    protected long validAt = -1; // configured validAt
    protected long myValidAt = -1; // actually used validAt in updateData(); if validAt == -1, then myValidAt is "now" each time the data is updated
    protected AdminRecord admin;
    protected boolean showAll = false;
    protected boolean showDeleted = false;
    protected String filterFieldName;
    protected String filterFieldValue;
    protected String buttonPanelPosition = BorderLayout.EAST;
    protected Vector<DataRecord> data;
    protected Hashtable<String, DataRecord> mappingKeyToRecord;
    protected IItemListenerDataRecordTable itemListenerActionTable;
    protected ItemTypeString searchField;
    protected ItemTypeBoolean filterBySearch;
    protected JTable aggregationTable = null;
    protected JPanel myPanel;
    protected JPanel tablePanel;
    protected JPanel buttonPanel;
    protected JPanel searchPanel;
    protected Hashtable<ItemTypeButton, String> actionButtons;
    protected static final String ACTION_BUTTON = "ACTION_BUTTON";
    protected String[] actionText;
    protected int[] actionTypes;
    protected String[] actionIcons;
    protected int defaultActionForDoubleclick = ACTION_EDIT;
    protected Color markedCellColor = Color.red;
    protected boolean markedCellBold = false;

    public ItemTypeDataRecordTable(String name,
            TableItemHeader[] tableHeader,
            StorageObject persistence,
            long validAt,
            AdminRecord admin,
            String filterFieldName, String filterFieldValue,
            String[] actions, int[] actionTypes, String[] actionIcons,
            IItemListenerDataRecordTable itemListenerActionTable,
            int type, String category, String description) {
        super(name, tableHeader, null, null, type, category, description);
        setData(persistence, validAt, admin, filterFieldName, filterFieldValue);
        setActions(actions, actionTypes, actionIcons);
        this.itemListenerActionTable = itemListenerActionTable;
        renderer = new de.nmichael.efa.gui.util.TableCellRenderer();
        renderer.setMarkedBold(false);
        renderer.setMarkedForegroundColor(markedCellColor);
        renderer.setMarkedBold(markedCellBold);
        renderer.setMarkedBackgroundColor(null);
    }

    protected void setData(StorageObject persistence, long validAt, AdminRecord admin,
            String filterFieldName, String filterFieldValue) {
        this.persistence = persistence;
        this.validAt = validAt;
        this.admin = admin;
        this.filterFieldName = filterFieldName;
        this.filterFieldValue = filterFieldValue;
    }

    public void setAndUpdateData(long validAt, boolean showAll, boolean showDeleted) {
        this.validAt = validAt;
        this.showAll = showAll;
        this.showDeleted = showDeleted;
        updateData();
    }

    public void setActions(String[] actions, int[] actionTypes, String[] actionIcons) {
        if (actions == null || actionTypes == null) {
            super.setPopupActions(DEFAULT_ACTIONS);
            this.actionText = DEFAULT_ACTIONS;
            this.actionTypes = new int[]{ACTION_NEW, ACTION_EDIT, ACTION_DELETE};
            this.actionIcons = new String[]{
                "button_add.png", "button_edit.png", "button_delete.png"
            };
        } else {
            int popupActionCnt = 0;
            for (int i = 0; i < actionTypes.length; i++) {
                if (actionTypes[i] >= 0) {
                    popupActionCnt++;
                } else {
                    break; // first action with type < 0 (and all others after this) won't be shows as popup actions
                }
            }
            String[] myPopupActions = new String[popupActionCnt];
            for (int i = 0; i < myPopupActions.length; i++) {
                if (actionTypes[i] >= 0) {
                    myPopupActions[i] = actions[i];
                }
            }
            super.setPopupActions(myPopupActions);
            this.actionText = actions;
            this.actionTypes = actionTypes;
            if (actionIcons != null) {
                this.actionIcons = actionIcons;
            } else {
                this.actionIcons = new String[actionTypes.length];
                for (int i = 0; i < this.actionIcons.length; i++) {
                    switch (actionTypes[i]) {
                        case ACTION_NEW:
                            this.actionIcons[i] = BaseDialog.IMAGE_ADD;
                            break;
                        case ACTION_EDIT:
                            this.actionIcons[i] = BaseDialog.IMAGE_EDIT;
                            break;
                        case ACTION_DELETE:
                            this.actionIcons[i] = BaseDialog.IMAGE_DELETE;
                            break;
                    }
                }
            }
        }
    }

    public void setDefaultActionForDoubleclick(int defaultAction) {
        this.defaultActionForDoubleclick = defaultAction;
    }

    protected void iniDisplayActionTable(Window dlg) {
        this.dlg = dlg;
        myPanel = new JPanel();
        myPanel.setLayout(new BorderLayout());
        tablePanel = new JPanel();
        tablePanel.setLayout(new GridBagLayout());
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        buttonPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        searchPanel = new JPanel();
        searchPanel.setLayout(new GridBagLayout());
        myPanel.add(tablePanel, BorderLayout.CENTER);
        myPanel.add(buttonPanel, buttonPanelPosition);
        tablePanel.add(searchPanel, new GridBagConstraints(0, 10, 0, 0, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        actionButtons = new Hashtable<ItemTypeButton, String>();

        JPanel smallButtonPanel = null;
        for (int i = 0; actionText != null && i < actionText.length; i++) {
            if (actionTypes[i] >= 2000) {
                continue; // actions >= 2000 not shown as buttons
            }
            String action = ACTION_BUTTON + "_" + actionTypes[i];
            ItemTypeButton button = new ItemTypeButton(action, IItemType.TYPE_PUBLIC, "BUTTON_CAT",
                    (actionTypes[i] < 1000 ? actionText[i] : null)); // >= 2000 just as small buttons without text
            button.registerItemListener(this);
            if (actionTypes[i] < 1000) {
                button.setPadding(20, 20, (i > 0 && actionTypes[i] < 0 && actionTypes[i - 1] >= 0 ? 20 : 0), 5);
                button.setFieldSize(200, -1);
            } else {
                button.setPadding(5, 5, 5, 5);
                button.setFieldSize(50, -1);
                if (smallButtonPanel == null) {
                    smallButtonPanel = new JPanel();
                    smallButtonPanel.setLayout(new GridBagLayout());
                    buttonPanel.add(smallButtonPanel, new GridBagConstraints(0, i, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 0, 20, 0), 0, 0));
                }
            }
            if (actionIcons != null && i < actionIcons.length && actionIcons[i] != null) {
                String iconName = actionIcons[i];
                if (iconName.startsWith(BUTTON_IMAGE_CENTERED_PREFIX)) {
                    iconName = iconName.substring(1);
                } else {
                    button.setHorizontalAlignment(SwingConstants.LEFT);
                }
                if (iconName != null && iconName.length() > 0) {
                    button.setIcon(BaseDialog.getIcon(iconName));
                }
            }
            if (actionTypes[i] < 1000) {
                button.displayOnGui(dlg, buttonPanel, 0, i);
            } else {
                button.displayOnGui(dlg, smallButtonPanel, i, 0);
            }
            actionButtons.put(button, action);
        }
        searchField = new ItemTypeString("SEARCH_FIELD", "", IItemType.TYPE_PUBLIC, "SEARCH_CAT", International.getString("Suche"));
        searchField.setFieldSize(300, -1);
        searchField.registerItemListener(this);
        searchField.displayOnGui(dlg, searchPanel, 0, 0);
        filterBySearch = new ItemTypeBoolean("FILTERBYSEARCH", false, IItemType.TYPE_PUBLIC, "SEARCH_CAT", International.getString("filtern"));
        filterBySearch.registerItemListener(this);
        filterBySearch.displayOnGui(dlg, searchPanel, 10, 0);
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        iniDisplayActionTable(dlg);
        panel.add(myPanel, new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, 0.0, 0.0,
                fieldGridAnchor, fieldGridFill, new Insets(padYbefore, padXbefore, padYafter, padXafter), 0, 0));
        super.displayOnGui(dlg, tablePanel, 0, 0);
        return 1;
    }

    public int displayOnGui(Window dlg, JPanel panel, String borderLayoutPosition) {
        iniDisplayActionTable(dlg);
        panel.add(myPanel, borderLayoutPosition);
        super.displayOnGui(dlg, tablePanel, 0, 0);
        return 1;
    }

    public void setVisibleButtonPanel(boolean visible) {
        buttonPanel.setVisible(visible);
    }

    public void setVisibleSearchPanel(boolean visible) {
        searchPanel.setVisible(visible);
    }

    public void showValue() {
        items = new Hashtable<String, TableItem[]>();
        mappingKeyToRecord = new Hashtable<String, DataRecord>();
        if (data == null && persistence != null) {
            updateData();
        }
        boolean isVersionized = persistence.data().getMetaData().isVersionized();
        for (int i = 0; data != null && i < data.size(); i++) {
            DataRecord r = data.get(i);
            TableItem[] content = r.getGuiTableItems();

            // mark deleted records
            if (r.getDeleted()) {
                for (TableItem it : content) {
                    it.setMarked(true);
                }
            }

            // mark invalid and invisible records
            if (isVersionized && (!r.isValidAt(myValidAt) || r.getInvisible())) {
                for (TableItem it : content) {
                    it.setDisabled(true);
                }
            }

            items.put(r.getKey().toString(), content);
            mappingKeyToRecord.put(r.getKey().toString(), r);
        }
        keys = items.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        super.showValue();
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (event != null && event instanceof ActionEvent && event.getID() == ActionEvent.ACTION_PERFORMED
                && !(itemType instanceof ItemTypeBoolean)) {
            ActionEvent e = (ActionEvent) event;
            String cmd = e.getActionCommand();
            int actionId = -1;
            if (cmd != null && cmd.startsWith(EfaMouseListener.EVENT_POPUP_CLICKED)) {
                try {
                    actionId = actionTypes[EfaUtil.stringFindInt(cmd, -1)];
                } catch (Exception eignore) {
                }
            }
            if (cmd != null && cmd.startsWith(EfaMouseListener.EVENT_MOUSECLICKED_2x)) {
                actionId = defaultActionForDoubleclick;
            }
            if (itemType != null && itemType instanceof ItemTypeButton) {
                actionId = EfaUtil.stringFindInt(actionButtons.get((ItemTypeButton) itemType), -1);
            }
            if (actionId == -1) {
                return;
            }
            int[] rows = table.getSelectedRows();
            DataRecord[] records = null;
            if (rows != null && rows.length > 0) {
                records = new DataRecord[rows.length];
                for (int i = 0; i < rows.length; i++) {
                    records[i] = mappingKeyToRecord.get(keys[rows[i]]);
                }
            }
            if (persistence != null && itemListenerActionTable != null) {
                DataEditDialog dlg;
                switch (actionId) {
                    case ACTION_NEW:
                        dlg = itemListenerActionTable.createNewDataEditDialog(getParentDialog(), persistence, null);
                        if (dlg == null) {
                            return;
                        }
                        dlg.showDialog();
                        break;
                    case ACTION_EDIT:
                        for (int i = 0; records != null && i < records.length; i++) {
                            if (records[i] != null) {
                                if (records[i].getDeleted()) {
                                    switch (Dialog.yesNoCancelDialog(International.getString("Datensatz wiederherstellen"),
                                            International.getMessage("Der Datensatz '{record}' wurde gelöscht. Möchtest Du ihn wiederherstellen?", records[i].getQualifiedName()))) {
                                        case Dialog.YES:
                                            try {
                                                DataRecord[] rall = persistence.data().getValidAny(records[i].getKey());
                                                for (int j = 0; rall != null && j < rall.length; j++) {
                                                    rall[j].setDeleted(false);
                                                    persistence.data().update(rall[j]);
                                                }
                                            } catch (Exception exr) {
                                                Dialog.error(exr.toString());
                                                return;
                                            }
                                            break;
                                        case Dialog.NO:
                                            continue;
                                        case Dialog.CANCEL:
                                            return;
                                    }
                                }
                                dlg = itemListenerActionTable.createNewDataEditDialog(getParentDialog(), persistence, records[i]);
                                if (dlg == null) {
                                    return;
                                }
                                dlg.showDialog();
                                if (!dlg.getDialogResult()) {
                                    break;
                                }
                            }
                        }
                        break;
                    case ACTION_DELETE:
                        if (records == null || records.length == 0) {
                            return;
                        }
                        if (itemListenerActionTable != null) {
                            if (!itemListenerActionTable.deleteCallback(records)) {
                                updateData();
                                showValue();
                                return;
                            }
                        }
                        int res = -1;
                        if (records.length == 1) {
                            res = Dialog.yesNoDialog(International.getString("Wirklich löschen?"),
                                    International.getMessage("Möchtest Du den Datensatz '{record}' wirklich löschen?", records[0].getQualifiedName()));
                        } else {
                            res = Dialog.yesNoDialog(International.getString("Wirklich löschen?"),
                                    International.getMessage("Möchtest Du {count} ausgewählte Datensätze wirklich löschen?", records.length));
                        }
                        if (res != Dialog.YES) {
                            return;
                        }
                        long deleteAt = Long.MAX_VALUE;
                        if (persistence.data().getMetaData().isVersionized()) {
                            VersionizedDataDeleteDialog ddlg =
                                    new VersionizedDataDeleteDialog(getParentDialog(),
                                    (records.length == 1 ? records[0].getQualifiedName()
                                    : International.getMessage("{count} Datensätze", records.length)));
                            ddlg.showDialog();
                            deleteAt = ddlg.getDeleteAtResult();
                            if (deleteAt == Long.MAX_VALUE || deleteAt < -1) {
                                return;
                            }
                        }
                        try {
                            for (int i = 0; records != null && i < records.length; i++) {
                                if (records[i] != null) {
                                    if (persistence.data().getMetaData().isVersionized()) {
                                        persistence.data().deleteVersionizedAll(records[i].getKey(), deleteAt);
                                        if (deleteAt >= 0) {
                                            Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDDELETEDAT,
                                                    records[i].getPersistence().getDescription() + ": "
                                                    + International.getMessage("{name} hat Datensatz '{record}' ab {date} gelöscht.",
                                                    (admin != null ? International.getString("Admin") + " '" + admin.getName() + "'"
                                                    : International.getString("Normaler Benutzer")),
                                                    records[i].getQualifiedName(),
                                                    EfaUtil.getTimeStampDDMMYYYY(deleteAt)));
                                        } else {
                                            Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDDELETED,
                                                    records[i].getPersistence().getDescription() + ": "
                                                    + International.getMessage("{name} hat Datensatz '{record}' zur vollständigen Löschung markiert.",
                                                    (admin != null ? International.getString("Admin") + " '" + admin.getName() + "'"
                                                    : International.getString("Normaler Benutzer")),
                                                    records[i].getQualifiedName()));
                                        }
                                    } else {
                                        persistence.data().delete(records[i].getKey());
                                        Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDDELETED,
                                                records[i].getPersistence().getDescription() + ": "
                                                + International.getMessage("{name} hat Datensatz '{record}' gelöscht.",
                                                (admin != null ? International.getString("Admin") + " '" + admin.getName() + "'"
                                                : International.getString("Normaler Benutzer")),
                                                records[i].getQualifiedName()));
                                    }
                                }
                            }
                        } catch (EfaModifyException exmodify) {
                            exmodify.displayMessage();
                        } catch (Exception ex) {
                            Logger.logdebug(ex);
                            Dialog.error(ex.toString());
                        }
                        break;
                }
            }
            if (itemListenerActionTable != null) {
                itemListenerActionTable.itemListenerActionTable(actionId, records);
            }
            updateData();
            showValue();
        }
        if (event != null && event instanceof KeyEvent && event.getID() == KeyEvent.KEY_RELEASED && itemType == searchField) {
        	filterTableContents();
        }
        if (event != null
                && (event instanceof KeyEvent && event.getID() == KeyEvent.KEY_RELEASED && itemType == searchField)
                || (event instanceof ActionEvent && event.getID() == ActionEvent.ACTION_PERFORMED && itemType == filterBySearch)) {
            updateFilter();
        }
    }
    
    /**
     * Method is extracted from itemListenerAction.
     * It handles the event when a literal is entered into the searchfield.
     * Refactored: local variables renamed so that the code has a better readability. 
     */
    private void filterTableContents() {
    	
        String sSearchValue = searchField.getValueFromField();
        if (sSearchValue != null && sSearchValue.length() > 0 && keys != null && items != null) {
            sSearchValue = sSearchValue.toLowerCase();
            Vector<String> sSplittedSearchValues = null;
            boolean[] bDidFindValue = null;
            if (sSearchValue.indexOf(" ") > 0) {
                sSplittedSearchValues = EfaUtil.split(sSearchValue, ' ');
                if (sSplittedSearchValues != null && sSplittedSearchValues.size() == 0) {
                    sSplittedSearchValues = null;
                } else {
                    bDidFindValue = new boolean[sSplittedSearchValues.size()];
                }
            }
            int rowFound = -1;
            for (int iCurrentRow = 0; rowFound < 0 && iCurrentRow < keys.length; iCurrentRow++) {
                // search in row iCurrentRow
                for (int j = 0; bDidFindValue != null && j < bDidFindValue.length; j++) {
                    bDidFindValue[j] = false; // matched parts of substring
                }

                TableItem[] row = items.get(keys[iCurrentRow]);
                for (int iCurrentCol = 0; row != null && rowFound < 0 && iCurrentCol < row.length; iCurrentCol++) {
                    // search in row i, column j
                    String t = (row[iCurrentCol] != null ? row[iCurrentCol].toString() : null);
                    t = (t != null ? t.toLowerCase() : null);
                    if (t == null) {
                        continue;
                    }

                    // match entire search string against column
                    if (t.indexOf(sSearchValue) >= 0) {
                        rowFound = iCurrentRow;
                    }

                    if (sSplittedSearchValues != null && rowFound < 0) {
                        // match column agains substrings
                        for (int k = 0; k < sSplittedSearchValues.size(); k++) {
                            if (t.indexOf(sSplittedSearchValues.get(k)) >= 0) {
                                bDidFindValue[k] = true;
                            }
                        }
                    }
                }
                if (bDidFindValue != null && rowFound < 0) {
                    rowFound = iCurrentRow;
                    for (int j = 0; j < bDidFindValue.length; j++) {
                        if (!bDidFindValue[j]) {
                            rowFound = -1;
                        }
                    }
                }
            }
            if (rowFound >= 0) {
                int currentIdx = table.getCurrentRowIndex(rowFound);
                if (currentIdx >= 0) {
                    scrollToRow(currentIdx);
                }
            }
        }
        
    }

    protected void updateFilter() {
        searchField.getValueFromGui();
        filterBySearch.getValueFromGui();
        if (filterBySearch.isChanged() || (filterBySearch.getValue() && searchField.isChanged())) {
            updateData();
            updateAggregations(filterBySearch.getValue());
            showValue();
        }
        filterBySearch.setUnchanged();
        searchField.setUnchanged();
    }

    protected void updateAggregations(boolean create) {
        if (aggregationTable != null) {
            tablePanel.remove(aggregationTable);
        }

        if (create && data != null) {
            int size = data.size();
            if (size > 0) {
                String[] aggregationStrings = new String[header.length];
                for (int i = 0; i < header.length; i++) {
                    aggregationStrings[i] = "";
                }

                HashMap<String, Object> overallInfo = new HashMap<String, Object>();
                for (int i = 0; i < size && aggregationStrings != null; i++) {
                    DataRecord r = data.get(i);
                    aggregationStrings = r.getGuiTableAggregations(aggregationStrings, i, size, overallInfo);
                }

                if (aggregationStrings != null) {
                    int length = 0;
                    for (int i = 0; i < header.length; i++) {
                        if (!aggregationStrings[i].equals("")) {
                            length++;
                        }
                    }

                    //create table
                    TableModel dataModel = new DefaultTableModel(1, length);
                    for (int i = 0, j = 0; i < header.length; i++) {
                        if (!aggregationStrings[i].equals("")) {
                            dataModel.setValueAt(aggregationStrings[i], 0, j++);
                        }
                    }
                    aggregationTable = new JTable(dataModel);

                    tablePanel.add(aggregationTable, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
                    tablePanel.setComponentZOrder(aggregationTable, 0);
                }
            }
        }

        tablePanel.revalidate();
        tablePanel.repaint();
    }

    protected void updateData() {
        if (persistence == null) {
            return;
        }
        try {
            String filterByAnyText = null;
            if (filterBySearch != null && searchField != null) {
                filterBySearch.getValueFromField();
                searchField.getValueFromGui();
                if (filterBySearch.getValue() && searchField.getValue() != null && searchField.getValue().length() > 0) {
                    filterByAnyText = searchField.getValue().toLowerCase();
                }
            }
            myValidAt = (validAt >= 0 ? validAt : System.currentTimeMillis());
            data = new Vector<DataRecord>();
            IDataAccess dataAccess = persistence.data();
            boolean isVersionized = dataAccess.getMetaData().isVersionized();
            DataKeyIterator it = dataAccess.getStaticIterator();
            DataKey key = it.getFirst();
            Hashtable<DataKey, String> uniqueHash = new Hashtable<DataKey, String>();
            while (key != null) {
                // avoid duplicate versionized keys for the same record
                if (isVersionized) {
                    DataKey ukey = dataAccess.getUnversionizedKey(key);
                    if (uniqueHash.get(ukey) != null) {
                        key = it.getNext();
                        continue;
                    }
                    uniqueHash.put(ukey, "");
                }

                DataRecord r;
                if (isVersionized) {
                    r = dataAccess.getValidAt(key, myValidAt);
                    if (r == null && showAll) {
                        r = dataAccess.getValidLatest(key);
                    }
                } else {
                    r = dataAccess.get(key);
                    if (!showAll && !r.isValidAt(myValidAt)) {
                        r = null;
                    }
                }
                if (r == null && showDeleted) {
                    DataRecord[] any = dataAccess.getValidAny(key);
                    if (any != null && any.length > 0 && any[0].getDeleted()) {
                        r = any[0];
                    }
                }
                if (r != null && (!r.getDeleted() || showDeleted)) {

                	if (!removeItemByCustomFilter(r)) {
                	
	                    if (filterFieldName == null || filterFieldValue == null
	                            || filterFieldValue.equals(r.getAsString(filterFieldName))) {
	                    	// Check if field content matches to the searchtext. Also, check if the entry matches for a certain date.
	                    	if (filterByAnyText == null || r.getAllFieldsAsSeparatedText().toLowerCase().indexOf(filterByAnyText) >= 0 || filterFromToAppliesToDate(r, filterByAnyText)) {
	                            data.add(r);
	                        }
	                    }
                	}
                }
                key = it.getNext();
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
    }

    /**
     * Default implementation of the method which returns false for all tables which do not
     * allow date-based filtering. Check derived classes of ItemTypeDataRecordTable which implement this method
     * to find those tables which do.
     * 
     * @param theDataRecord
     * @param filterValue 
     * @return true if the filtervalue (as a date) applies to the datarecord
     */
    protected boolean filterFromToAppliesToDate(DataRecord theDataRecord, String filterValue) {
    	return false;
    }

    /**
     * Use this function to remove items from the table by using custom filters which apply
     * to the special record type.
     * 
     * How to apply custom filters to itemTypeDataRecordTable:
     * - modify the specialized class of DataListDialog.java to add a control panel above the table. 
     *   The actionlistener shall then set a dedicated attribute in the specialized class of ItemTypeDataRecordTable.
     * - override this function in the specialized class of ItemTypeDataRecordTable.
     * 
     * @param theDataRecord
     * @return
     */
    protected boolean removeItemByCustomFilter(DataRecord theDataRecord) {
    	return false;
    }
    
    public void actionPerformed(ActionEvent e) {
        itemListenerAction(this, e);
    }

    public void setButtonPanelPosition(String borderLayoutPosition) {
        this.buttonPanelPosition = borderLayoutPosition;
    }

    public void setMarkedCellColor(Color color) {
        this.markedCellColor = color;
        if (renderer != null) {
            renderer.setMarkedForegroundColor(color);
        }
    }

    public void setMarkedCellBold(boolean bold) {
        this.markedCellBold = bold;
        if (renderer != null) {
            renderer.setMarkedBold(bold);
        }
    }

    public Vector<DataRecord> getDisplayedData() {
        Vector<DataRecord> sortedData = new Vector<DataRecord>();
        for (int i = 0; i < data.size(); i++) {
            sortedData.add(mappingKeyToRecord.get(keys[table.getOriginalIndex(i)]));
        }
        return sortedData;
    }

    public Vector<DataRecord> getSelectedData() {
        String[] keys = getSelectedKeys();
        Vector<DataRecord> selectedData = new Vector<DataRecord>();
        for (int i = 0; keys != null && i < keys.length; i++) {
            selectedData.add(mappingKeyToRecord.get(keys[i]));
        }
        return selectedData;
    }

    public boolean isFilterSet() {
        filterBySearch.getValueFromGui();
        return filterBySearch.getValue();
    }
    
    /**
     * Use this method to programmatically activate the checkbox "filterbysearch" so that
     * only the filtertext-matching items in the table are displayed.
     * 
     * @param value Value to which the internal variable is set
     */
    public void setIsFilterSet(Boolean value) {
    	filterBySearch.setValue(value);
    	updateFilter();
    	updateData();
    }
}
