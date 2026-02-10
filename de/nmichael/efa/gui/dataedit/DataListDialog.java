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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemListenerDataRecordTable;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeButton;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeDateTime;
import de.nmichael.efa.core.items.ItemTypeHtmlList;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.DataExportDialog;
import de.nmichael.efa.gui.DataImportDialog;
import de.nmichael.efa.gui.DataPrintListDialog;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.util.EfaMenuButton;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedLabel;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.ProgressTask;

public abstract class DataListDialog extends BaseDialog implements IItemListener, IItemListenerDataRecordTable {

	/* Documentation on action numbers: (see @ItemTypeDataRecordTable.iniDisplayActionTable)
	 * <0  				Do not show this action in the popup menu for an element in the table.
	 * >0 		<1000	Show as standard buttons with caption and icon
	 * >1000	<2000	Show as buttons WITHOUT caption, just icons
	 * >2000		    Do not show as a button
	 */
	
    public static final int ACTION_HIDE      =  100;
    public static final int ACTION_MERGE     =  200;
    public static final int ACTION_IMPORT    = -100; // negative actions will not be shown as popup actions
    public static final int ACTION_EXPORT    = -101; // negative actions will not be shown as popup actions
    public static final int ACTION_PRINTLIST = -102; // negative actions will not be shown as popup actions
    public static final int ACTION_EDITASSISTENT = -103; // negative actions will not be shown as popup actions

    protected StorageObject persistence;
    protected long validAt;
    protected AdminRecord admin;

    protected String[] actionText;
    protected int[] actionType;
    protected String[] actionImage;
    protected String filterFieldName;
    protected String filterFieldValue;
    protected String filterFieldDescription;
    protected ItemTypeDataRecordTable table;
    protected int sortByColumn = 0;
    protected boolean sortAscending = true;
    protected int tableFontSize = -1;
    protected boolean intelligentColumnWidth = true;
    protected int minColumnWidth = -1;
    protected int[] minColumnWidths = null;
    protected String buttonPanelPosition = BorderLayout.EAST;
    private ItemTypeDateTime validAtDateTime;
    private ItemTypeBoolean showAll;
    private ItemTypeBoolean showDeleted;
    private JPanel tablePanel;
    private JPanel buttonPanel;
    private Hashtable<ItemTypeButton,String> actionButtons;
    protected Color markedCellColor = Color.red;
    protected boolean markedCellBold = false;

    public DataListDialog(Frame parent, String title, StorageObject persistence, long validAt, AdminRecord admin) {
        super(parent, title, International.getStringWithMnemonic("Schließen"));
        this.admin = admin;
        setPersistence(persistence, validAt);
        iniActions();
    }

    public DataListDialog(JDialog parent, String title, StorageObject persistence, long validAt, AdminRecord admin) {
        super(parent, title, International.getStringWithMnemonic("Schließen"));
        this.admin = admin;
        setPersistence(persistence, validAt);
        try {
            iniActions();
        } catch(Exception e) {
            Logger.logdebug(e); // can happen if remote project is not reachable
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniActions() {
        Vector<Integer> actions = new Vector<Integer>();
        actions.add(ItemTypeDataRecordTable.ACTION_NEW);
        actions.add(ItemTypeDataRecordTable.ACTION_EDIT);
        actions.add(ItemTypeDataRecordTable.ACTION_DELETE);
        try {
            if (persistence.data().getMetaData().isVersionized()) {
                actions.add(ACTION_HIDE);
            }
        } catch (Exception e) {
            Logger.logdebug(e); // can happen if remote project is not reachable
        }
        if (admin != null && admin.isAllowedAdvancedEdit()) {
            actions.add(ACTION_IMPORT);
        }
        actions.add(ACTION_EXPORT);
        actions.add(ACTION_PRINTLIST);
        if (admin != null && admin.isAllowedAdvancedEdit()) {
            actions.add(ACTION_EDITASSISTENT);
        }

        actionType = new int[actions.size()];
        actionText = new String[actions.size()];
        actionImage = new String[actions.size()];
        for (int i=0; i<actions.size(); i++) {
            actionType[i] = actions.get(i);
            switch(actionType[i]) {
                case ItemTypeDataRecordTable.ACTION_NEW:
                    actionText[i] = ItemTypeDataRecordTable.ACTIONTEXT_NEW;
                    actionImage[i] = BaseDialog.IMAGE_ADD;
                    break;
                case ItemTypeDataRecordTable.ACTION_EDIT:
                    actionText[i] = ItemTypeDataRecordTable.ACTIONTEXT_EDIT;
                    actionImage[i] = BaseDialog.IMAGE_EDIT;
                    break;
                case ItemTypeDataRecordTable.ACTION_DELETE:
                    actionText[i] = ItemTypeDataRecordTable.ACTIONTEXT_DELETE;
                    actionImage[i] = BaseDialog.IMAGE_DELETE;
                    break;
                case ACTION_HIDE:
                    actionText[i] = International.getString("Verstecken");
                    actionImage[i] = BaseDialog.IMAGE_HIDE;
                    break;
                case ACTION_IMPORT:
                    actionText[i] = International.getString("Importieren");
                    actionImage[i] = BaseDialog.IMAGE_IMPORT;
                    break;
                case ACTION_EXPORT:
                    actionText[i] = International.getString("Exportieren");
                    actionImage[i] = BaseDialog.IMAGE_EXPORT;
                    break;
                case ACTION_PRINTLIST:
                    actionText[i] = International.getString("Liste ausgeben");
                    actionImage[i] = BaseDialog.IMAGE_LIST;
                    break;
                case ACTION_EDITASSISTENT:
                    actionText[i] = International.getString("Bearbeitungsassistent");
                    actionImage[i] = BaseDialog.IMAGE_EDITMULTI;
                    break;
            }
        }
    }
/**
 * Adds an Action into the DataListDialog Button list. Action is added at the end of the list.
 * 
 * @param text Displaytext for the button
 * @param type Action Type (see DataListDialog.ACTION_* or ItemTypeDataRecordTable.ACTION_*)
 * @param image Image Name (see BaseDialog.IMAGE_*)
 */
    protected void addAction(String text, int type, String image) {
        if (actionText == null) {
            actionText = new String[0];
        }
        if (actionType == null) {
            actionType = new int[0];
        }
        if (actionImage == null) {
            actionImage = new String[0];
        }
        String[] _actionText = actionText;
        int[] _actionType = actionType;
        String[] _actionImage = actionImage;
        actionText = new String[_actionText.length + 1];
        actionType = new int[_actionType.length + 1];
        actionImage = new String[_actionImage.length + 1];
        // arrays must all be the same length!
        for (int i=0; i<actionType.length - 1; i++) {
            actionText[i] = _actionText[i];
            actionType[i] = _actionType[i];
            actionImage[i] = _actionImage[i];
        }
        actionText[actionText.length - 1] = text;
        actionType[actionType.length - 1] = type;
        actionImage[actionImage.length - 1] = image;
    }
   
    /**
     * Adds an Action into the DataListDialog Button list. Action is added right after the position of insertAfterType.
     * If insertAfterType Action is not found, Action is positioned at the end.
     * 
     * @param text Displaytext for the button
     * @param type Action Type (see DataListDialog.ACTION_* or ItemTypeDataRecordTable.ACTION_*)
     * @param image Image Name (see BaseDialog.IMAGE_*)
     * @param insertAfterType Existing Action Type in the list after which the new action shall be put. 
     */    
    protected void insertAction(String text, int type, String image, int insertAfterType){
        if (actionText == null) {
            actionText = new String[0];
        }
        if (actionType == null) {
            actionType = new int[0];
        }
        if (actionImage == null) {
            actionImage = new String[0];
        }
        String[] _actionText = actionText;
        int[] _actionType = actionType;
        String[] _actionImage = actionImage;
        
        // Extend the new array of actions by one item
        actionText = new String[_actionText.length + 1];
        actionType = new int[_actionType.length + 1];
        actionImage = new String[_actionImage.length + 1];
        
        int insertPosition=0;
        boolean insertAfterTypeIsFound=false;
        // arrays must all be the same length!
        // copy old elements to the new array. 
        // check if the current position is the "insertAfterType" element and then
        // add the new element
        for (int i=0; i<actionType.length - 1; i++) {
            actionText[insertPosition] = _actionText[i];
            actionType[insertPosition] = _actionType[i];
            actionImage[insertPosition] = _actionImage[i];
            insertPosition++; // done with the current element
            
            if (_actionType[i]==insertAfterType) {
            	//we need to add the new action after the current one
            	actionText[insertPosition] = text;
            	actionType[insertPosition] = type;
            	actionImage[insertPosition] = image;
            	insertPosition++; // and increment the position
            	insertAfterTypeIsFound=true;
            }
        }
        if (!insertAfterTypeIsFound) {
        	// we did not find the action type specified, so we add the action at the end of the buttons.
        	actionText[actionText.length - 1] = text;
            actionType[actionType.length - 1] = type;
            actionImage[actionImage.length - 1] = image;    	
        }
    }

    /**
     * Removes an action from the button list of the data list dialog.
     * @param type type of the action to be removed
     */
    protected void removeAction(int type) {
        for (int i=0; actionType != null && i<actionType.length; i++) {
            if (actionType[i] == type) {
                String[] _actionText = actionText;
                int[] _actionType = actionType;
                String[] _actionImage = actionImage;
                actionText = new String[_actionText.length - 1];
                actionType = new int[_actionType.length - 1];
                actionImage = new String[_actionImage.length - 1];
                // arrays must all be the same length!
                for (int j = 0; j < actionType.length; j++) {
                    actionText[j] = _actionText[ (j < i ? j : j+1) ];
                    actionType[j] = _actionType[ (j < i ? j : j+1) ];
                    actionImage[j] = _actionImage[ (j < i ? j : j+1) ];
                }
            }
        }
    }

    protected void addMergeAction() {
        addAction(International.getString("Zusammenfügen"),
                ACTION_MERGE,
                BaseDialog.IMAGE_MERGE);
    }


    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());
        
        JPanel mainTablePanel = new JPanel();
        mainTablePanel.setLayout(new BorderLayout());

        if (filterFieldDescription != null) {
            JLabel filterName = new RoundedLabel();
            filterName.setBorder(new RoundedBorder(Daten.efaConfig.getHeaderForegroundColor()));
            filterName.setBackground(Daten.efaConfig.getHeaderBackgroundColor());
            filterName.setOpaque(true);
            filterName.setForeground(Daten.efaConfig.getHeaderForegroundColor());
            filterName.setText(filterFieldDescription);
            filterName.setFont(filterName.getFont().deriveFont(Font.BOLD));
            filterName.setHorizontalAlignment(SwingConstants.CENTER);
            mainTablePanel.add(filterName, BorderLayout.NORTH);
            mainTablePanel.setBorder(new EmptyBorder(10,0,0,0));
        }
        
        // Instanciates the table variable with the specific type which is needed. 
        createSpecificItemTypeRecordTable();
        
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
        table.setFieldSize(Math.max((buttonPanelPosition==BorderLayout.NORTH ? 850 : 600), getSumColumnWidths()), 500);
        //table shall not have a huge distance from a toolbar above (NORTH position)
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

        iniControlPanel();
        mainPanel.add(mainTablePanel, BorderLayout.CENTER);

        setRequestFocus(table);
        this.validate();
    }

    private int getSumColumnWidths() {
    	int resValue=0;
    	if (minColumnWidths != null && minColumnWidths.length>0)
    		for (int i=0; i<minColumnWidths.length;i++) {
    			if (minColumnWidths[i]>0) {
    				resValue+=minColumnWidths[i];
    			} else if (minColumnWidths[i]<0){
    				resValue+=50;
    			}
    		}
    	return resValue;
    }
    
    /**
     * Instanciates the table variable to the specific subtype of ItemTypeDataRecord that is needed
     * by the subclass of DataListDialog.
     * Default implementation creates a standard ItemTypeDataRecordTable.
     */
	protected void createSpecificItemTypeRecordTable() {
		table = new ItemTypeDataRecordTable("TABLE",
                persistence.createNewRecord().getGuiTableHeader(),
                persistence, validAt, admin,
                filterFieldName, filterFieldValue, // defaults are null
                actionText, actionType, actionImage, // default actions: new, edit, delete
                this,
                IItemType.TYPE_PUBLIC, "BASE_CAT", getTitle());
	}

    protected void iniControlPanel() {
        if (persistence != null && persistence.data().getMetaData().isVersionized()) {
            JPanel mainControlPanel = new JPanel();
            mainControlPanel.setLayout(new GridBagLayout());
            validAtDateTime = new ItemTypeDateTime("VALID_AT",
                    (validAt < 0 ? null : new DataTypeDate(validAt)),
                    (validAt < 0 ? null : new DataTypeTime(validAt)),
                    IItemType.TYPE_PUBLIC, "", International.getString("zeige Datensätze gültig am"));
            validAtDateTime.setPadding(0, 0, 10, 0);
            validAtDateTime.displayOnGui(this, mainControlPanel, 0, 0);
            validAtDateTime.registerItemListener(this);
            showAll = new ItemTypeBoolean("SHOW_ALL",
                    false,
                    IItemType.TYPE_PUBLIC, "", International.getString("auch derzeit ungültige Datensätze zeigen"));
            showAll.setPadding(0, 0, 0, 0);
            showAll.displayOnGui(this, mainControlPanel, 0, 1);
            showAll.registerItemListener(this);
            showDeleted = new ItemTypeBoolean("SHOW_DELETED",
                    false,
                    IItemType.TYPE_PUBLIC, "", International.getString("auch gelöschte Datensätze zeigen"));
            showDeleted.setPadding(0, 0, 0, 0);
            showDeleted.displayOnGui(this, mainControlPanel, 0, 2);
            showDeleted.registerItemListener(this);

            mainPanel.add(mainControlPanel, BorderLayout.NORTH);
        }
    }

    public void setPersistence(StorageObject persistence, long validAt) {
        this.persistence = persistence;
        this.validAt = validAt;
    }
    
    public StorageObject getPersistence() {
        return persistence;
    }

    // @Override
    public void itemListenerActionTable(int actionId, DataRecord[] records) {
        // usually nothing to be done (handled in ItemTypeDataRecordTable itself).
        // override if necessary
        switch(actionId) {
            case ACTION_HIDE:
                if (records == null || records.length == 0 || records[0] == null || !persistence.data().getMetaData().isVersionized()) {
                    return;
                }
                boolean currentlyVisible = !records[0].getInvisible();
                int res = -1;
                if (currentlyVisible) {
                    if (records.length == 1) {
                        res = Dialog.yesNoDialog(International.getString("Wirklich verstecken?"),
                                International.getMessage("Möchtest Du den Datensatz '{record}' wirklich verstecken?", records[0].getQualifiedName()));
                    } else {
                        res = Dialog.yesNoDialog(International.getString("Wirklich verstecken?"),
                                International.getMessage("Möchtest Du {count} ausgewählte Datensätze wirklich verstecken?", records.length));
                    }
                } else {
                    if (records.length == 1) {
                        res = Dialog.yesNoDialog(International.getString("Wirklich sichtbar machen?"),
                                International.getMessage("Möchtest Du den Datensatz '{record}' wirklich sichtbar machen?", records[0].getQualifiedName()));
                    } else {
                        res = Dialog.yesNoDialog(International.getString("Wirklich sichtbar machen?"),
                                International.getMessage("Möchtest Du {count} ausgewählte Datensätze wirklich sichtbar machen?", records.length));
                    }
                }
                if (res != Dialog.YES) {
                    return;
                }
                try {
                    for (int i = 0; records != null && i < records.length; i++) {
                        if (records[i] != null) {
                            if (persistence.data().getMetaData().isVersionized()) {
                                DataRecord[] allVersions = persistence.data().getValidAny(records[i].getKey());
                                for (int j=0; allVersions != null && j<allVersions.length; j++) {
                                    allVersions[j].setInvisible(currentlyVisible);
                                    persistence.data().update(allVersions[j]);
                                }
                            } else {
                                records[i].setInvisible(currentlyVisible);
                                persistence.data().update(records[i]);
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
            case ACTION_MERGE:
                if (records == null || records.length < 2) {
                    Dialog.error("Bitte wähle mindestens zwei Datensätze zum Zusammenfügen aus!");
                    return;
                }
                Hashtable<String,String> items = new Hashtable<String,String>();
                Hashtable<String,DataKey> keyMapping = new Hashtable<String,DataKey>();
                for (DataRecord r : records) {
                    if (r != null) {
                        DataKey k = r.getKey();
                        String s = r.getKeyAsTextDescription();
                        keyMapping.put(k.encodeAsString(), k);
                        items.put(k.encodeAsString(),
                                "<html>ID: " + s + "<br>" +
                                International.getString("Name") + ": " + r.getQualifiedName() +
                                (r.getPersistence().data().getMetaData().isVersionized() ? "<br>" +
                                 International.getString("Gültigkeit") + ": " + r.getValidRangeString() : "")+"</html>");
                    }
                }
                String[] keys = items.keySet().toArray(new String[0]);
                if (keys.length < 2) {
                    return;
                }
                ItemTypeHtmlList list = new ItemTypeHtmlList("LIST", keys, items,
                        keys[0], IItemType.TYPE_PUBLIC, "",
                        International.getString("Bitte wähle den Hauptdatensatz aus, zu dem alle Datensätze zusammengefügt werden sollen!") + "\n");
                if (!SimpleInputDialog.showInputDialog(this,
                        International.getString("Zusammenfügen"),
                        list)) {
                    return;
                }
                String mainKeyString = list.toString();
                DataKey mainKey = (mainKeyString != null ? keyMapping.get(mainKeyString) : null);
                if (mainKey == null) {
                    return;
                }
                DataKey[] mergeKeys = new DataKey[keys.length - 1];
                for (int i=0, j=0; i<keys.length; i++) {
                    DataKey k = keyMapping.get(keys[i]);
                    if (!k.equals(mainKey)) {
                        mergeKeys[j++] = k;
                    }
                }
                ProgressTask progressTask = getMergeProgressTask(mainKey, mergeKeys);
                if (progressTask == null) {
                    return;
                }
                ProgressDialog progressDialog = new ProgressDialog(this,
                        International.getString("Datensätze zusammenfügen"), progressTask, false);
                progressTask.setProgressDialog(progressDialog,false);
                progressTask.start();
                progressDialog.showDialog();
                break;
            case ACTION_IMPORT:
                if (admin == null || !admin.isAllowedAdvancedEdit()) {
                    EfaMenuButton.insufficientRights(admin, International.getString("Import"));
                    break;
                }
                DataImportDialog dlg1 = new DataImportDialog(this, persistence, validAt, admin);
                dlg1.showDialog();
                break;
            case ACTION_EXPORT:
                DataExportDialog dlg2 = new DataExportDialog(this, persistence, validAt, admin,
                        table.getSelectedData(), 
                        (table.isFilterSet() ? table.getDisplayedData() : null));
                dlg2.showDialog();
                break;
            case ACTION_PRINTLIST:
                Vector<DataRecord> data = table.getDisplayedData();
                if (data == null || data.size() == 0) {
                    Dialog.error(International.getString("Auswahl ist leer."));
                    return;
                }
                DataPrintListDialog dlg3 = new DataPrintListDialog(this, persistence, validAt, admin, data);
                dlg3.showDialog();
                break;
            case ACTION_EDITASSISTENT:
                if (admin == null || !admin.isAllowedAdvancedEdit()) {
                    EfaMenuButton.insufficientRights(admin, International.getString("Bearbeitungsassistent"));
                    break;
                }
                BatchEditDialog dlg4 = new BatchEditDialog(this, persistence, validAt, admin);
                dlg4.showDialog();
                break;
        }
    }

    // @Override
    public boolean deleteCallback(JDialog parent,IItemListenerDataRecordTable caller, AdminRecord admin, DataRecord[] records) {
        return true;
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType == validAtDateTime) {
            if (event.getID() == FocusEvent.FOCUS_LOST ||
                (event.getID() == KeyEvent.KEY_PRESSED && ((KeyEvent)event).getKeyCode() == KeyEvent.VK_ENTER)) {
                validAtDateTime.getValueFromGui();
                validAtDateTime.parseAndShowValue(validAtDateTime.getValueFromField());
                long _validAt = (validAtDateTime.isSet() ? validAtDateTime.getTimeStamp() : -1);
                if (_validAt != validAt) {
                    validAt = _validAt;
                    showDeleted.getValueFromGui();
                    showAll.getValueFromGui();
                    table.setAndUpdateData(validAt, showAll.getValue(), showDeleted.getValue());
                    table.showValue();
                }
            }
        }
        if (itemType == showAll || itemType == showDeleted) {
            if (event.getID() == ActionEvent.ACTION_PERFORMED) {
                showAll.getValueFromGui();
                showDeleted.getValueFromGui();
                if (showAll.getValue()) {
                    showAll.saveColor();
                    showAll.setColor(Color.gray);
                } else {
                    showAll.restoreColor();
                }
                if (showDeleted.getValue()) {
                    showDeleted.saveColor();
                    showDeleted.setColor(Color.red);
                } else {
                    showDeleted.restoreColor();
                }
                table.setAndUpdateData(validAt, showAll.getValue(), showDeleted.getValue());
                table.showValue();
            }
        }
    }

    protected ProgressTask getMergeProgressTask(DataKey mainKey, DataKey[] mergeKeys) {
        return null; // to be overridden
    }

}
