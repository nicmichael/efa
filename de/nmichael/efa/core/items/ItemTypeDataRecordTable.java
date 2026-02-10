/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.core.items;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.gui.dataedit.VersionizedDataDeleteDialog;
import de.nmichael.efa.gui.dataedit.DataEditDialog;
import de.nmichael.efa.gui.dataedit.UnversionizedDataEditDialog;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.ex.*;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.ImagesAndIcons;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.batik.ext.swing.GridBagConstants;

import java.util.*;

// @i18n complete
public class ItemTypeDataRecordTable extends ItemTypeTable implements IItemListener, KeyListener {

	/* Documentation on action numbers: (see @iniDisplayActionTable)
	 * <0  				Do not show this action in the popup menu for an element in the table.
	 * >=0 		<1000	Show as standard buttons with caption and icon
	 * >=1000	<2000	Show as buttons WITHOUT caption, just icons
	 * >=2000		    Do not show as a button
	 */    
	public static final int ACTIONTYPE_SHOW_AS_POPUPMENU_ELEMENT_ONLY=-1;
	public static final int ACTIONTYPE_SHOW_AS_STANDARD_BUTTONS=0;
	public static final int ACTIONTYPE_SHOW_AS_SMALL_BUTTONS=1000;
	public static final int ACTIONTYPE_DO_NOT_SHOW_AS_BUTTONS=2000;
	
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
        renderer = new de.nmichael.efa.gui.util.EfaTableCellRenderer();
        renderer.setAlternatingRowColor(Daten.efaConfig.getTableAlternatingRowColor());
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
            		ImagesAndIcons.IMAGE_BUTTON_ADD, ImagesAndIcons.IMAGE_BUTTON_EDIT, ImagesAndIcons.IMAGE_BUTTON_DELETE};
            super.setPopupIcons(this.actionIcons);
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
            super.setPopupIcons(this.actionIcons);
        }
    }

    public void setDefaultActionForDoubleclick(int defaultAction) {
        this.defaultActionForDoubleclick = defaultAction;
    }

    /*
     * Creates the table including button bar and filter field.
     * Depending on buttonPanelPosition, there are two ways to position the button bar:
     *   
     * - buttonPanelPosition=BorderLayout.EAST
     *   Good for tables for a small number of columns. Also good, if there are a lot of buttons for the table,  
     *   like in the dataEdit dialogs.
     *   
     * - buttonPanelPosition=BorderLayout.NORTH
     *   Good for tables like Boatreservations, which have a lot of columns and consume a lot of horizontal space.
     *   Does NOT work well with a lot of buttons.
     *    
     */
    protected void iniDisplayActionTable(Window dlg) {
        this.dlg = dlg;
        myPanel = new JPanel();
        myPanel.setLayout(new BorderLayout());
        tablePanel = new JPanel();
        tablePanel.setLayout(new GridBagLayout());
        buttonPanel = new RoundedPanel();
        buttonPanel.setLayout(new GridBagLayout());
        buttonPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        buttonPanel.setFont(buttonPanel.getFont().deriveFont(Font.BOLD));// this is needed as all buttons use bold font - otherwise GetTextLength would not work correctly.
        searchPanel = new JPanel();
        searchPanel.setLayout(new GridBagLayout());
        myPanel.add(tablePanel, BorderLayout.CENTER);
        myPanel.add(buttonPanel, buttonPanelPosition);
        
        // Buttons on the top? Align the buttonpanel with the table with some insets.
        if (buttonPanelPosition.equals(BorderLayout.NORTH)) {
        	JPanel innerPanel=new JPanel();
        	innerPanel.setLayout(new GridBagLayout());
        	innerPanel.add(buttonPanel,new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
        			GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets (0,10,0,10) , 0, 0));
        	myPanel.add(innerPanel, buttonPanelPosition);
        } else {
        	myPanel.add(buttonPanel, buttonPanelPosition);
        }
        tablePanel.add(searchPanel, new GridBagConstraints(0, 10, 0, 0, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        actionButtons = new Hashtable<ItemTypeButton, String>();

        JPanel smallButtonPanel = null;
        for (int i = 0; actionText != null && i < actionText.length; i++) {
            if (actionTypes[i] >= ACTIONTYPE_DO_NOT_SHOW_AS_BUTTONS) {
                continue; // actions >= 2000 not shown as buttons
            }
            String action = ACTION_BUTTON + "_" + actionTypes[i];
            ItemTypeButton button = new ItemTypeButton(action, IItemType.TYPE_PUBLIC, "BUTTON_CAT",
                    (actionTypes[i] < ACTIONTYPE_SHOW_AS_SMALL_BUTTONS ? actionText[i] : null)); // >= 2000 just as small buttons without text
            button.registerItemListener(this);
            button.boldfont = true;
            if (actionTypes[i] < ACTIONTYPE_SHOW_AS_SMALL_BUTTONS) {
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
            if (actionTypes[i] < ACTIONTYPE_SHOW_AS_SMALL_BUTTONS) {
                
            	if (buttonPanelPosition.equals(BorderLayout.NORTH)) {
            		//put all action items in one horizontal line.
            		//ItemTypeButton does not support automatic resizing according to text length.
            		//But we need this if the buttons are on top.
            		//Get the Text length according to the font, add icon width and textIconGap(10 pix) and some pixels depending on the font size.
            		button.setFieldSize(getTextLength(button.getDescription())+16+10+(24-buttonPanel.getFont().getSize()), -1);
            		button.setPadding(4, 0, 4, 4);
                	button.displayOnGui(dlg, buttonPanel, i,0);
            	} else {
                	button.displayOnGui(dlg, buttonPanel, 0, i);
            	}
            } else {
                button.displayOnGui(dlg, smallButtonPanel, i, 0);
            }
            actionButtons.put(button, action);
        }
        //If ButtonPanel is above the table, align it to the right.
        if (buttonPanelPosition.equals(BorderLayout.NORTH)) {
        	JLabel myLabelSpacer=new JLabel();
        	myLabelSpacer.setText(" ");
        	Dimension dim = new Dimension(100,10);
        	myLabelSpacer.setMinimumSize(dim);
        	myLabelSpacer.setPreferredSize(dim);
            buttonPanel.add(myLabelSpacer, new GridBagConstraints(actionText.length,0,1,1,1.0,0,GridBagConstants.EAST, GridBagConstants.HORIZONTAL,new Insets(0,0,0,0),0,0));
            //buttonPanel.setBackground(Daten.efaConfig.getHeaderBackgroundColor());
            buttonPanel.setBackground(EfaUtil.darker(buttonPanel.getBackground(), 18));
        }
        searchField = new ItemTypeString("SEARCH_FIELD", "", IItemType.TYPE_PUBLIC, "SEARCH_CAT", International.getString("Suche"));
        searchField.setFieldSize(300, -1);
        searchField.registerItemListener(this);
        searchField.setPadding(12, 2, 2, 0);
        searchField.displayOnGui(dlg, searchPanel, 0, 0);
        searchField.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        filterBySearch = new ItemTypeBoolean("FILTERBYSEARCH", false, IItemType.TYPE_PUBLIC, "SEARCH_CAT", International.getString("filtern"));
        filterBySearch.registerItemListener(this);
        filterBySearch.displayOnGui(dlg, searchPanel, 10, 0);
        if (buttonPanelPosition.equals(BorderLayout.NORTH)) {
        	JLabel myLabelSpacer=new JLabel();
        	myLabelSpacer.setText(" ");
        	Dimension dim = new Dimension(100,10);
        	myLabelSpacer.setMinimumSize(dim);
        	myLabelSpacer.setPreferredSize(dim);
            searchPanel.add(myLabelSpacer, new GridBagConstraints(11,0,1,1,1.0,0,GridBagConstants.EAST, GridBagConstants.HORIZONTAL,new Insets(0,0,0,0),0,0));        	
        }
    }

    private int getTextLength(String text) {
    	//for very short texts, the text length is too short.
    	return Math.max(40, (int) Math.round(buttonPanel.getFontMetrics(buttonPanel.getFont()).stringWidth(text)));
    }


    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        iniDisplayActionTable(dlg);
        panel.add(myPanel, new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, 1.0, 1.0, // 1.0 means grow with the element size.-
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
                    if (r.getInvisible()) {it.setInvisible(true);}
                }
            }

            items.put(r.getKey().toString(), content);
            mappingKeyToRecord.put(r.getKey().toString(), r);
        }
        keys = items.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        super.showValue();
        table.addKeyListener(this);
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
                        //admin mode must be set in the DataEdit dialog so that messages for
                        //changed or new items get sent.
                        ((UnversionizedDataEditDialog) dlg).setAdmin(admin);
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
                                //admin mode must be set in the DataEdit dialog so that messages for
                                //changed or new items get sent.
                                ((UnversionizedDataEditDialog) dlg).setAdmin(admin);                              
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
                            if (!itemListenerActionTable.deleteCallback(this.getParentDialog(), itemListenerActionTable, this.admin, records)) {
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
			switch (((KeyEvent) event).getKeyCode()) {
				case KeyEvent.VK_UP:
				case KeyEvent.VK_PAGE_UP:
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_PAGE_DOWN:
					table.requestFocus();
					break;
	
				default:
					filterTableContents();
					break;
			}
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
        	
        	boolean easyFindEntriesWithSpecialCharacters = Daten.efaConfig.getValueEfaDirekt_tabelleEasyfindEntriesWithSpecialCharacters();
        	
        	sSearchValue = sSearchValue.trim().toLowerCase();
        	boolean searchValueWithSpecialCharacters = EfaUtil.containsUmlaut(sSearchValue);

        	//split the modified searchstring into an array if it contains spaces.
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
                    if (easyFindEntriesWithSpecialCharacters) {
                    	if (searchValueWithSpecialCharacters) {
                    		//Searchstring contains special characters - so we use contains mode only
                    		//as we are searching for entries that DO contain these special characters.
                        	t = (t != null ? t.toLowerCase() : null);                    		
                    	} else {
                    		//searchstring does not contain special characters - user enters "a" but also
                    		// wants results containing ä, á or other equivalents of "a"
                    		t = (t != null ? EfaUtil.replaceAllUmlautsLowerCaseFast(t) : null);
                    	}
                    } else {
                    	t = (t != null ? t.toLowerCase() : null);
                    }
                    if (t == null) {
                        continue;
                    }

                    // match entire search string against column
                    if (t.indexOf(sSearchValue) >= 0) {
                        rowFound = iCurrentRow;
                    }

                    if (sSplittedSearchValues != null && rowFound < 0) {
                        // match column agains substrings
                    	// no need to check for special character handling here,
                    	// as sSearchvalue and t both are already normalized in earlier places of this code.
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

    public void updateData() {
        if (persistence == null) {
            return;
        }
        try {
            boolean easyFindEntriesWithSpecialCharacters = Daten.efaConfig.getValueEfaDirekt_tabelleEasyfindEntriesWithSpecialCharacters();
            
        	String filterByAnyText = null;
        	Boolean isFilterTextWithUmlauts=false;
            if (filterBySearch != null && searchField != null) {
                filterBySearch.getValueFromField();
                searchField.getValueFromGui();
                if (filterBySearch.getValue() && searchField.getValue() != null && searchField.getValue().length() > 0) {
                    	filterByAnyText = searchField.getValue().trim().toLowerCase();
                }
            }

            if (filterByAnyText!=null) {
            	isFilterTextWithUmlauts=EfaUtil.containsUmlaut(filterByAnyText);
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
	                    	if (easyFindEntriesWithSpecialCharacters) {
	                    		if (isFilterTextWithUmlauts) {
	                    			//filterText has umlauts --> so we are explicitly searching for entries containing these umlauts
			                    	if (filterByAnyText == null || r.getAllFieldsAsSeparatedText().toLowerCase().indexOf(filterByAnyText) >= 0 || filterFromToAppliesToDate(r, filterByAnyText)) {
			                            data.add(r);
			                        }	
	                    		} else {
	                    			//filter text has no umlauts, we also want results containing umlauts
	                    			// e.g. "arger" as search string shall find entries "ärger" or "argér"
	                    			//so we remove all umlauts from the record
			                    	if (filterByAnyText == null || EfaUtil.replaceAllUmlautsLowerCaseFast(r.getAllFieldsAsSeparatedText()).indexOf(filterByAnyText) >= 0 || filterFromToAppliesToDate(r, filterByAnyText)) {
			                            data.add(r);
			                        }	                    			
	                    		}

	                    	} else {// no easyFindEntriesWithSpecialCharacters
		                    	if (filterByAnyText == null || r.getAllFieldsAsSeparatedText().toLowerCase().indexOf(filterByAnyText) >= 0 || filterFromToAppliesToDate(r, filterByAnyText)) {
		                            data.add(r);
		                        }	                    		
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
    
    public ItemTypeString getSearchField(){
    	return searchField;
    };
    
    public ItemTypeBoolean getFilterBySearch() {
    	return filterBySearch;
    }
    
    // Key Listener Interface for table
    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_F && (e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
			// STRG+F in the table: goto searchfield, select all content
			searchField.requestFocus();
			searchField.setSelection(0, 2048);
		}    	
    }
}
