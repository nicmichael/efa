/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedLabel;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;

/**
 * ItemTypeList is a list of items that can be displayed in the GUI.
 * It supports filtering, separators, and custom rendering.
 */
public class ItemTypeList extends ItemType implements ActionListener, DocumentListener, KeyListener {

    JPanel mypanel;
    JLabel label;
    JScrollPane scrollPane;
    JList<ItemTypeListData> list = new JList<ItemTypeListData>();
    JTextField filterTextField;
    EfaMouseListener popupListener; 
    JPopupMenu popup;
    volatile Long lastFilterChange=0L;
    public DefaultListModel<ItemTypeListData> data; // no longer Vector as we need a DefaultListModel for filtering
    String[] actions;
    String incrementalSearch = "";
    private int iconWidth = 0;
    private int iconHeight = 0;
    private ItemTypeListThreeColumnCellRenderer listCellRenderer = null;

    protected static final String LIST_SECTION_STRING = "";//"----";

    private double gridWeightX = 0.0;
    private double gridWeightY = 0.0;

    private Color _separatorBackground = new Color(240,240,240);
	private Color _filterTextfieldDefaultBackground = Color.WHITE;
    private boolean showFilterField = false;
    protected String other_item_text=""; //item text of the element for <other boat> or <other person>
        

    /** 
	 * Inner class to hold the data for each item in the list.
	 * There are three elements for each item: primary, secondary, and tertiary.
	 * Primary is the main text, secondary is right-aligned, and tertiary is left-aligned after primary.
	 * An item can also be a separator, which is a non-selectable item that visually separates groups of items.
	 * The colors array defines the colors of the icon (pie chart) of the item, wich is created by the FastTwoColumnListCellRenderer.
	 */
    class ItemTypeListData {

        String text;
        String toolTipText;
        String toolTipFilterText;
        String secondaryElement;
        String tertiaryElement;
        Object object;
        boolean separator;
        int section;
        String image;
        Color[] colors;
        public ItemTypeListData(String text, String toolTipText, String secondaryElement, String tertiaryElement, Object object, boolean separator, int section) {
            ini(text, toolTipText, secondaryElement, tertiaryElement, object, separator, section, null, null);
        }
        public ItemTypeListData(String text, String toolTipText, String secondaryElement, String tertiaryElement, Object object, boolean separator, int section, 
                String image, Color[] colors) {
            ini(text, toolTipText, secondaryElement, tertiaryElement, object, separator, section, image, colors);
        }
        private void ini(String text, String toolTipText, String secondaryElement, String tertiaryElement, Object object, boolean separator, int section, 
                String image, Color[] colors) {
            this.text = text;
            this.toolTipText = toolTipText;
            if (toolTipText!=null) {
            	try {
            		//this removes all html tags from the tooltiptext. 
            		//although it is told it may be unsafe to remove tags by regular expressions, it works.
            		this.toolTipFilterText = EfaGuiUtils.HTML_TAG.matcher(toolTipText).replaceAll("");
            	} catch (Exception e) {
            		Logger.log(e);
            		this.toolTipFilterText=null; // just in case the removal of html tags fails, set tooltippfiltertext to null.
            	}
            } 
            this.object = object;
            this.separator = separator;
            this.section = section;
            this.image = image;
            this.colors = colors;
            this.secondaryElement = secondaryElement;
            this.tertiaryElement = tertiaryElement;
        }
        public String toString() {
            return text;
        }
 
    	public String getFilterableText() {
    	    if (toolTipFilterText == null) return text;
    	    return text + " " + toolTipFilterText;   // use a space to separate the two parts for filtering
    	}
    }

    public ItemTypeList(String name,
            int type, String category, String description, boolean showFilterField) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.description = description;
        this.showFilterField = showFilterField;
        data = new DefaultListModel<ItemTypeListData>();
        //overwrite standard separator color with efaFlatBackgroundColor if efaFlat is the current look
        if (Daten.isEfaFlatLafActive()) {
        	this._separatorBackground=Daten.efaConfig.getEfaGuiflatLaf_Background();
       }
    }
    
    public ItemTypeList(String name,
            int type, String category, String description) {
        this(name, type, category, description, false);
    }

    /**
	 * Creates a copy of this ItemTypeList with the same properties and items.
	 * (but for ItemTypeList: not the list items).
	 * @return a new ItemTypeList that is a copy of this one
	 */
    public IItemType copyOf() {
        ItemTypeList copy= new ItemTypeList(name, type, category, description, this.showFilterField);
        copy.setFieldSize(fieldWidth,  fieldHeight);
        copy.setIndent(this.isIndent());
        copy.setVisible(this.isVisible());
        copy.setFieldGrid(fieldGridWidth, fieldGridHeight, fieldGridAnchor, fieldGridFill);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setIconSize(iconWidth, iconHeight);
        if (actions!=null) { copy.setPopupActions(actions.clone());}
        copy.other_item_text=this.other_item_text;
        copy.setSeparatorBackground(this._separatorBackground);
        
        return copy;
    }

    private void setSeparatorBackground(Color _separatorBackground2) {
		this._separatorBackground = _separatorBackground2;
	}

	/**
	 * Add an item to the list. The list will be filtered after adding the item.
	 * This is rather expensive if you add many items, as the filter is applied after each item.
	 * Use addItems() to add several items at once and filter only once.
	 * 
	 * @param text The primary text of the item
	 * @param toolTipText The tooltip text of the item
	 * @param secondaryItem The secondary text of the item (right-aligned)
	 * @param tertiaryItem The tertiary text of the item (left-aligned, after primary)
	 * @param object The associated object of the item
	 * @param separator Whether this item is a separator
	 * @param separatorHotkey The hotkey for this separator (if applicable)
	 */
    public void addItem(String text, String toolTipText, String secondaryItem, String tertiaryItem, Object object, boolean separator, char separatorHotkey) {
        data.addElement(new ItemTypeListData(text, toolTipText, secondaryItem, tertiaryItem, object, separator, separatorHotkey));
        filter();
    }

    /**
	 * Add an item to the list. The list will be filtered after adding the item.
	 * This is rather expensive if you add many items, as the filter is applied after each item.
	 * Use addItems() to add several items at once and filter only once.
	 * 
	 * @param text The primary text of the item
	 * @param toolTipText The tooltip text of the item
	 * @param secondaryItem The secondary text of the item (right-aligned)
	 * @param tertiaryItem The tertiary text of the item (left-aligned, after primary)
	 * @param object The associated object of the item
	 * @param separator Whether this item is a separator
	 * @param separatorHotkey The hotkey for this separator (if applicable)
	 * @param image The image path for the item icon
	 * @param colors The colors for the item icon (if applicable)
	 * 
	 */
    public void addItem(String text, String toolTipText, String secondaryItem, String tertiaryItem, Object object, boolean separator, char separatorHotkey,
            String image, Color[] colors) {
		data.addElement(new ItemTypeListData(text, toolTipText, secondaryItem, tertiaryItem, object, separator,
				separatorHotkey, image, colors));
        filter();
    }
    
    /**
     * Adds several items at once and filters the list only once.
     * Unlike setItems(), the existing items are kept.
     *
     * @param items the items to append
     */
    public void addItems(List<ItemTypeListData> items) {
        if (items == null) {
            return;
        }
        /*TODO: Currently efa is based on Java8, so we cannot use data.addAll(items) directly. 
          Instead, we add each item individually and then filter once at the end.
          Using data.addAll(items) would be more efficient (throws one event instead of n), 
          but it requires Java11 or later.*/
        for (ItemTypeListData item : items) {
            data.addElement(item);   // no filter() here, to avoid O(n^2) work
        }
        filter();                    // filter once after all items were added
    }
 
    /**
	 * Removes an item from the list by index and filters the list afterwards.
	 *
	 * @param idx the index of the item to remove, referring to the unfiltered data (all items),
	 *            not to the filtered view
	 */
    public void removeItem(int idx) {
        if (idx >= 0 && idx < data.size()) {
            data.remove(idx);
        }
        filter();
    }

    /**
	 * Removes all items from the list and filters the list afterwards.
	 */
    public void removeAllItems() {
        data.clear(); // keep the DefaultListModel, just clear it
        filter();
    }

    /**
	 * Sets the items of the list, replacing any existing items, and filters the list afterwards
	 * @param items the new items to set
	 */
    public void setItems(Vector<ItemTypeListData> items) {
        data.clear();
        if (items != null) {
            for (ItemTypeListData item : items) {
                data.addElement(item);
            }
        }
        filter();
    }

    /** 
     * Returns the number of items in the list.
     * not all may be visible. If you want to get the size of the filtered list,
     * use @see #filteredSize() instead.
     */
    public int size() {
        if (data == null) {
            return 0;
        }
        return data.size();
    }

    /**
     * Returns the size of the list when the filter text field is applied.
     * @return the number of items that match the current filter, or the total number of items if no filter is applied
     */
    public int filteredSize() {
    	ListModel<ItemTypeListData> theList = null;
    	
        if (showFilterField==true) {
        	theList = list.getModel();
        } else {
        	theList = data;
        }
        
        if (theList==null) {
        	return 0;
        } else {
        	return theList.getSize();
        }
    }

    /** 
     * Returns the text of the item at the given index.
     * @param idx
     * @return the text of the item at the given index, or null if the index is out of bounds
     * Index space: the index refers to the unfiltered data (all items), not to the filtered view.
     * Use getSelectedIndex() for the filtered view, as the filter can hide items and shift indices.
     */
    public String getItemText(int idx) {
        if (idx >= 0 && idx < data.size()) {
            return data.get(idx).text;
        }
        return null;
    }

    /**
	 * Returns the associated object of the item at the given index.
	 * @param idx
	 * @return the associated object of the item at the given index, or null if the index is out of bounds
	 */
    public Object getItemObject(int idx) {
        if (idx >= 0 && idx < data.size()) {
            return data.get(idx).object;
        }
        return null;
    }

    /**
     * Clears the incremental search string and resets the tooltip of the list if necessary.
     * 
     */
    void clearIncrementalSearch() {
            incrementalSearch = "";
            if (list != null) {
            	if (Daten.efaConfig.getValueEfaBoathouseExtdToolTips()==false) {
            		list.setToolTipText(null);
            	}
            }
    }

    /**
     * Returns the current incremental search string, which is built from the keys pressed by the user.
     * @return the current incremental search string
     */
    public String getIncrementalSearchString() {
        return incrementalSearch;
    }

    /**
	 * Sets the actions for the popup menu of the list. If a popup menu already exists, it will be rebuilt with the new actions.
	 * @param actions an array of action strings for the popup menu
	 */
    public void setPopupActions(String[] actions) {
        this.actions = actions;
        // if a popup menu already exists, remove all elements and rebuild the popup menu.
        if (popup!=null) {
	        popup.removeAll();
	        buildPopupMenu();
	        if (popupListener!=null) {
	        	popupListener.setPopupMenu(popup);
	        }
        }
    }

    /** 
     * Initializes the display of the list. This method is not used, as everything is done in displayOnGui(...).
     */
    protected void iniDisplay() {
        // not used, everything done in displayOnGui(...)
    }

    /**
	 * Displays the list on the given panel at the specified grid position (x, y).
	 * If the list is indented, it will be displayed with an additional offset.
	 * The list will be filtered after being displayed.
	 *
	 * @param dlg the parent window for the list
	 * @param panel the panel to display the list on
	 * @param x the grid x position
	 * @param y the grid y position
	 * @return 1 if the list was displayed successfully
	 */
    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {

        //respect the indent property of the item, if set
        x=(this.isIndent() ? x+1 : x);

        panel.add(setupPanel(dlg), new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, gridWeightX, gridWeightY,
                fieldGridAnchor, fieldGridFill, new Insets(padYbefore, 0, padYafter, padXafter), 0, 0));
        showValue();
        return 1;
    }

    /**
	 * Displays the list on the given panel at the specified border layout orientation (e.g.,
	 * BorderLayout.NORTH, BorderLayout.SOUTH, BorderLayout.CENTER, etc.). The list will be filtered after being displayed.
	 * 
	 * @param dlg the parent window for the list
	 * @param panel the panel to display the list on
	 * @param borderLayoutOrientation the border layout orientation to place the list in
	 * @return 1 if the list was displayed successfully
	 */
    public int displayOnGui(Window dlg, JPanel panel, String borderLayoutOrientation) {
        panel.add(setupPanel(dlg), borderLayoutOrientation);
        showValue();
        return 1;
    }

    /** 
     * Sets up the panel for displaying the list, including the label, filter text field, scroll pane, and list component.
     * @param dlg the parent window for the list
     * @return the panel containing the list and its components
     */
    private JPanel setupPanel(Window dlg) {
        this.dlg = dlg;

        // Detach the previous list from the shared model before it is replaced. Otherwise the
        // old JList stays registered as a ListDataListener on the model and receives every
        // change event (one per added item), so the cost grows with each display switch.
        if (list != null) {
            list.setModel(new DefaultListModel<ItemTypeListData>());
        }
        list = createNewListWithTooltippSupport();
        
        if (this.showFilterField) {
	        filterTextField =new JTextField();
	        filterTextField.getDocument().addDocumentListener(this);
	        filterTextField.addKeyListener(this);
	        filterTextField.putClientProperty("caretWidth", 3);
	        filterTextField.setMargin(new Insets(0,2,0,0));
	        Dialog.setPreferredSize(filterTextField, 150, 21);
	        updateLastFilterChange();
        }
        popup = new JPopupMenu();
        
        //Vertical scrollbar shall be shown always, because better ListCellRendering does not work optically well when
        //scrollbar is shown only when needed
        scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED/*ALWAYS*/, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.field = scrollPane;

        if (getDescription() != null) {
            if (Daten.efaConfig.getHeaderUseHighlightColor()) {
            	label = new RoundedLabel();            
                label.setBorder(new RoundedBorder(this.color));
            }else {
            	label = new JLabel();
            	label.setBorder(new EmptyBorder(4,0,4,0));//4 pixel space before and after the label
            }
            Mnemonics.setLabel(dlg, label, " " + getDescription() + ": ");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (type == IItemType.TYPE_EXPERT) {
                label.setForeground(Color.red);
            }
            if (color != null) {
                label.setForeground(color);
            }
            if (backgroundColor != null) {
            	label.setBackground(backgroundColor);
            	label.setOpaque(true);
            }
            label.setFont(label.getFont().deriveFont(Font.BOLD));

            if (this.showFilterField) {
            	label.setLabelFor(filterTextField);
            } else {
            	label.setLabelFor(list);
            }
            
            Dialog.setPreferredSize(label, fieldWidth, 20);
            
        }

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //leave some space on top and on bottom
        list.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        buildPopupMenu(); //popupItems has been initialized earlier
        
        // KeyListeners entfernen, damit unter Java 1.4.x nicht automatisch gescrollt wird, sondern durch den eigenen Algorithmus
        try {
            KeyListener[] kl = list.getKeyListeners();
            for (int i = 0; i < kl.length; i++) {
                list.removeKeyListener(kl[i]);
            }
        } catch (Exception e) { 
        	Logger.logdebug(e);
        }
        
        list.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                list_keyReleased(e);
            }
        });
    	if (Daten.efaConfig.getValueEfaBoathouseExtdToolTips()==false) { 
			list.addMouseListener(new java.awt.event.MouseAdapter() {
	            public void mouseEntered(MouseEvent e) {
	            	try {
            			((JList<?>) e.getSource()).setToolTipText(null); // remove tool tip from scrolling/searching
	            	} catch (Exception ee) {
	            		Logger.logdebug(ee);
	            	}}});
        }

        popupListener = new EfaMouseListener(list, popup, this, Daten.efaConfig.getValueEfaDirekt_autoPopupOnBoatLists());
        list.addMouseListener(popupListener);

        list.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(FocusEvent e) {
                list_focusGained(e);
            }
        });

        scrollPane.getViewport().add(list, null);
        
        //mypanel
        //--> panelDescriptionAndFilter (NORTH)
        //    --> label (for description) (NORTH)
        //    --> filterTextfield (SOUTH)
        //--> Scrollpane (CENTER)
        //    --> JList
        
        mypanel = new JPanel();
        mypanel.setLayout(new BorderLayout());
        mypanel.setBorder(new EmptyBorder(0,4,0,4));// 4 pixel space on the left and the right side of the panel
        
        JPanel panelDescriptionAndFilter=new JPanel();
        panelDescriptionAndFilter.setLayout(new BorderLayout());
        panelDescriptionAndFilter.setBorder(new EmptyBorder(4,0,4,0));//4 pix space before and after
        if (getDescription() != null) {
            panelDescriptionAndFilter.add(label, BorderLayout.NORTH);
        }
        
        if (this.showFilterField) {
	        JPanel filterPanel=new JPanel();
	        filterPanel.setLayout(new BorderLayout());
	        filterPanel.setBorder(new EmptyBorder(4,0,4,0));//4 pix space before and after
	        JLabel myFilterLabel = new JLabel(International.getString("Filter")+":");
	        myFilterLabel.setBorder(new EmptyBorder(0,0,0,4));//4 pix space on the right
	        filterPanel.add(myFilterLabel, BorderLayout.WEST);
	        filterPanel.add(filterTextField, BorderLayout.CENTER);
	        panelDescriptionAndFilter.add(filterPanel, BorderLayout.SOUTH);
	        this.field=filterTextField; // by this, when the boat status list receives focus, and the filter text field is visible, the filter text field gets the focus.
	        _filterTextfieldDefaultBackground = filterTextField.getBackground();
	        
	        filterTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusGained(FocusEvent e) {
	                filterTextField.setBackground(Color.YELLOW);
	            }
	        });
	        filterTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusLost(FocusEvent e) {
	            	if (!filterTextField.getText().isEmpty()) {
	            		filterTextField.setBackground(new Color(255,255,204)); //some white yellow so it is clear that some stuff is in this field.

	            	} else {	
	            		// use standard background color. for flatlaf, this is not white as it is for the other lafs.
	            		filterTextField.setBackground(_filterTextfieldDefaultBackground); 
	            	}
	            }
	        });
        }

        mypanel.add(panelDescriptionAndFilter, BorderLayout.NORTH);
        mypanel.add(scrollPane, BorderLayout.CENTER);

        if (listCellRenderer == null) {
            listCellRenderer = new ItemTypeListThreeColumnCellRenderer();
        }
        list.setCellRenderer(listCellRenderer);
        
        return mypanel;
    }

    /** 
	 * Builds the popup menu for the list based on the actions array.
	 * If a popup menu already exists, it will be cleared and rebuilt.
	 */
	private void buildPopupMenu() {
		if (popup!=null) {
			popup.removeAll();
		}
			
		for (int i = 0; actions != null && i < actions.length; i++) {
        	if (actions[i].equals(EfaGuiUtils.MENU_SEPARATOR)) {
        		popup.addSeparator();
        	} else {
	            JMenuItem menuItem = new JMenuItem(actions[i].substring(1));
	            menuItem.setActionCommand(EfaMouseListener.EVENT_POPUP_CLICKED + "_" + actions[i].substring(0, 1));
	            menuItem.addActionListener(this);
	            popup.add(menuItem);
	            menuItem.setIcon(getIconFromActionID(actions[i].substring(0, 1)));
        	}
        }
	}

	/** 
	 * Returns the icon corresponding to the given action ID. This is used for the popup menu items.
	 * @param actionID the action ID string (e.g., "1", "2", "3", etc.)
	 * @return the Icon object corresponding to the action ID, or null if no icon is defined for that action
	 */
    private Icon getIconFromActionID(String actionID) {
    	if (actionID.equals("1")) return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_ACTION_START_SESSION);
    	if (actionID.equals("2")) return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_ACTION_FINISH_SESSION);	
    	if (actionID.equals("3")) return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_ACTION_LATE_ENTRY);
    	if (actionID.equals("5")) return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_ACTION_ABORT_SESSION);
    	if (actionID.equals("6")) return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_ACTION_BOAT_RESERVATIONS);
    	
    	return null;
    }
    
    /** 
     * Handles action events from the popup menu items. When a menu item is clicked, 
     * this method is called and it notifies the listener of the action.
     * 
     * @param e the ActionEvent triggered by the menu item click
     */
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.itemListenerAction(this, e);
        }
    }

    /** 
	 * Clears the popup menu by hiding it if it is currently visible.
	 */
    public void clearPopup() {
        if (popup != null) {
            popup.setVisible(false);
        }
    }
    
    /** 
     * Shows the popup menu by making it visible if it is not already visible.
     */
    public void showPopup() {
        if (popup != null) {
            popup.setVisible(true);
        }	
    }

    /** 
	 * Clears the selection in the list by deselecting any selected items.
	 */
    public void clearSelection() {
        try {
            //list.setSelectedIndices(new int[0]);
            list.clearSelection();
        } catch(Exception e) {
        	Logger.logdebug(e);
        }
    }

    /**
     * Returns whether the list currently has focus in the GUI.
     * @return
     */
    public boolean isFocusOwner() {
        return list.isFocusOwner();
    }

    /** 
     * Handles key released events in the list. This method is called 
     * when a key is released while the list has focus.
     * @param e the KeyEvent triggered by the key release
     * 
     */
    private void list_keyReleased(KeyEvent e) {
        clearPopup();
        if (e != null) {
        	if ((e.getKeyCode()==KeyEvent.VK_ESCAPE) || (e.getKeyCode()==KeyEvent.VK_F && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0))){
        		if (this.showFilterField) {
        			filterTextField.requestFocus();
        		}
        	} else {
        		scrollToEntry(String.valueOf(e.getKeyChar()), 15, (e != null && e.getKeyCode() == 38 ? -1 : 1));  // KeyCode 38 == Cursor Up
        	}
        }
        if (listener != null) {
            listener.itemListenerAction(this, e);
        }
    }

    /** 
     * Handles focus gained events for the list. When the list gains focus, this method is called.
     *  
     * @param e the FocusEvent triggered by the list gaining focus
     * 
     */
    private void list_focusGained(FocusEvent e) {
        if (list != null && list.getFirstVisibleIndex() >= 0 && list.getSelectedIndex() < 0) {
            list.setSelectedIndex(0);
        }
        //clearIncrementalSearch(); // it's better to comment this out, cannot hold 100% backwards compatibility with incremental search
        if (listener != null) {
            listener.itemListenerAction(this, e);
        }
    }
    
    /** 
     * Clears the filter text field if it has been unchanged for a specified interval.
     * (Default is 2 minutes, but can be configured in efaConfig.)
     */
    public void clearFilterTextByInterval() {
    	
    	// get the reset filter interval in minutes from efaConfig.
    	long filterResetInterval= (Daten.efaConfig.getValueEfaBoathouseFilterTextAutoClearInterval() * 60L * 1000L); 
    	
    	
    	if (filterResetInterval>0) {
    		//reduce filterResetInterval by 5 seconds.
    		//this is because clearFilterText gets called regularily by efaBoatHouseBackgroundTask, in the interval of every minute.
    		//so if we have exactly an minute interval, and the boathousetask also calls this every minute,
    		//chances are high that we miss the first call. So... we handle this by reducing the configured interval by 5 seconds.
    		filterResetInterval = filterResetInterval-(5*1000);
	    	if ((System.currentTimeMillis()>(lastFilterChange+filterResetInterval))) {
	    		clearFilterText();
	    	}
    	}
    }

    /**
	 * Clears the filter text field and resets the filter. 
	 * This method ensures that it runs on the Event Dispatch Thread (EDT) to avoid threading issues.
	 */
    public void clearFilterText() {

    	// be sure to run on EDT, as this method may be called from a background thread
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::clearFilterText);   
            return;
        }
        
    	if (this.showFilterField && (this.filterTextField != null)) {
			this.filterTextField.setText(""); // this also triggers the DocumentListener, which will call filter() automatically
        	updateLastFilterChange();
    		if (!this.filterTextField.hasFocus()) {
    			this.filterTextField.setBackground(_filterTextfieldDefaultBackground);
    		}
    	}
    }
    
    /**
	 * Scrolls the list to the entry that matches the search string and selects it.
	 * If the entry is a separator, it will skip to the next selectable item in the specified direction.
	 * 
	 * Remarks: This method is used for incremental search directly in the list. 
	 * It can be triggered by key events or other search actions.
	 * 
	 * This is the original incremental search method, which is kept for backwards compatibility.
	 * It is not used for the new filter text field, which has its own filtering mechanism and shall replace this method in the future.
	 *  
	 * @param search the search string to find in the list
	 * @param plus the number of additional entries to show below the selected entry
	 * @param direction the direction to move if the found entry is a separator (-1 for up, 1 for down)
	 */

    private void scrollToEntry(String search, int plus, int direction) {

        if (list == null || search == null || search.length() == 0) {
            return;
        }
		DefaultListModel <ItemTypeListData> theData= (DefaultListModel<ItemTypeListData>)list.getModel();
		
        try {
            int start = 0;

            int index = -1;

            /* if the first character of the search string is a digit, 
             * we interpret it as a section number and search for the corresponding section in the list.
             * 
             * the intended use is that the user click in the list and then presses a number key to jump to the corresponding section.
             * so the user can quickly jump to a section of two-seaters by pressing "2", or to a section of singles by pressing "1", etc.
             * 
             * this is rather outdated, as the list now has a filter text field, but we keep it for backwards compatibility.
             */
            if (search.charAt(0) >= '0' && search.charAt(0) <= '9') {
                int isearch = search.charAt(0) - '0';
                // search for a section with the corresponding number
                for (int i=0; i<theData.size(); i++) {
                    if (theData.get(i).section == isearch) {
                        index = i;
                        break;
                    }
                }
                incrementalSearch = "";
            } else {
                // search for names within the list
                start = Math.max(list.getSelectedIndex(), 0);

                if (incrementalSearch == null || incrementalSearch.length() == 0) {
                    // if we haven't searched for anything before, jump to the start of this section
                    while (start > 0 && !(theData.get(start).separator)) {
                        start--;
                    }
                }
                // build new search string depending of previous search
                char c = search.charAt(0);
                if (Character.isLetter(c) || Character.isSpaceChar(c)
                        || c == '.' || c == '-' || c == '_' || c == ':' || c == ',' || c == ';') {
                    search = (incrementalSearch != null ? incrementalSearch : "") + search;
                } else {
                    if (c == 0x8) {
                        search = (incrementalSearch != null && incrementalSearch.length() > 0
                                ? incrementalSearch.substring(0, incrementalSearch.length() - 1)
                                : "");
                    } else {
                        return; // no valid search character
                    }
                }
                incrementalSearch = search;
                search = search.toLowerCase();

                boolean startsWith = search.length() == 1; // for single-character search, match strings starting with this search string; otherwise, match somewhere
                for (int run = 0; run < 2; run++) { // 2 search runs: 1st - start from "start"; 2nd - if no result, restart from 0
                    for (int i = start; i < theData.size(); i++) {
                        String item = (theData.get(i).text != null ? theData.get(i).text : "").toLowerCase();
                        boolean matches = startsWith ? item.startsWith(search) : item.contains(search);
                        if (matches) {
                            index = i;
                            break;
                        }
                    }
                    if (index != -1) {
                        break;
                    } else {
                        start = 0;
                    }
                }
            }

            // check whether we should really select this item
            while (index >= 0 && index < theData.size() && theData.get(index).separator) {
                index += direction;
            }

            // Item found?
            if (index >= 0 && index < theData.size()) {
                list.setSelectedIndex(index);
                Rectangle rect = list.getCellBounds(index, (index + plus >= theData.size() ? theData.size() - 1 : index + plus));
                list.scrollRectToVisible(rect);
            }

            if (Daten.efaConfig.getValueEfaBoathouseExtdToolTips()==false) {
	            Rectangle rect = list.getVisibleRect();
	                list.setToolTipText((search.length() > 0 ? search : null));
	                int origDelay = ToolTipManager.sharedInstance().getInitialDelay();
	                ToolTipManager.sharedInstance().setInitialDelay(0);
	                ToolTipManager.sharedInstance().mouseMoved(
	                        new MouseEvent(list, 0, 0, 0,
	                        10, rect.y + rect.height - 50,
	                        0, false));
	                ToolTipManager.sharedInstance().setInitialDelay(origDelay);
            }

        } catch (Exception ee) { Logger.logdebug(ee);/* just to be sure */ }
    }

    /**
	 * Selects the first item in the list that is not a separator and not "<other boat>" or "<other person>".
	 * If no such item exists, it selects the first item in the list regardless.
	 */
    private void selectFirstMatchingElement() {
		DefaultListModel <ItemTypeListData> theData= (DefaultListModel<ItemTypeListData>)list.getModel();

    	int index = 0;
	    if (filterTextField.getText().trim().length() > 0
	          && theData.getSize() > 0
	          && other_item_text.equals(theData.get(0).text)) {
	      index = 1;
	    }
    	
    	// check whether we should really select this item
		if (filterTextField.getText().trim().length()==0){
			index=0;
		} else { 
			while (index >= 0 && index < theData.size() && (theData.get(index).separator)) {
	            index += 1;
	        }
		}
		if (index>=theData.size()) {
        	//no element could be found which is not a separator and not "<other boat>" or "<other person>"
        	//so we select the first element nonetheless
        	index=0;
        }

        if (index >= 0 && index < theData.size()) {
            list.setSelectedIndex(index);
            Rectangle rect = list.getCellBounds(index, (index + 15 >= theData.size() ? theData.size() - 1 : index + 15));
            list.scrollRectToVisible(rect);
        }

    }

    /** 
	 * Returns the associated object of the currently selected item in the list.
	 * @return the associated object of the selected item, or null if no item is selected
	 * */
    public Object getSelectedValue() {
        ItemTypeListData item = getSelectedData();
        return (item != null) ? item.object : null;
    }

    /** 
	 * Returns whether the currently selected item in the list is a separator.
	 */
    public boolean getSelectedItemIsSeparator() {
        ItemTypeListData item = getSelectedData();
        return (item != null) && item.separator;
    }
    
    /**
     * Returns the text of the currently selected item in the list.
     * @return the text of the selected item, or null if no item is selected
     */
    public String getSelectedText() {
        ItemTypeListData item = getSelectedData();
        return (item != null) ? item.text : null;
    }

    /**
     * Returns the data of the currently selected item, shared by all getSelected*() methods.
     * Centralizing this keeps the selection lookup in one place, so a change to it
     * only has to be made once.
     *
     * @return the selected item data, or null if nothing is selected or the lookup fails
     */
    private ItemTypeListData getSelectedData() {
        try {
            if (list == null || list.isSelectionEmpty()) {
                return null;
            }
            // JList is typed as JList<ItemTypeListData>, so no cast is needed here
            return list.getSelectedValue();
        } catch (Exception e) {
            // Log instead of failing silently, so unexpected states remain visible in the debug log
            Logger.logdebug(e);
            return null;
        }
    }

    /**
	 * Returns whether the input in the list is valid. 
	 * This method always returns true, as the list does not have invalid input.
	 * @return true
	 */
    public boolean isValidInput() {
        return true;
    }

    /** 
	 * Returns the value from the field. This method is not used, as the list does
	 * not have a single value to return. It always returns null.
	 * 
	 * return null
	 */
    public String getValueFromField() {
        return null;
    }

    /** 
	 * Shows the current value of the list by updating the cell renderer configuration,
	 * setting the list model to the data, and applying the filter.
	 */
    public void showValue() {
    	updateCellRendererConfig();
    	list.setModel(data);
        filter();
    }
    
    /**
     * Passes the current layout values (icon size, field width, separator color) to the cell renderer.
     * Must be called whenever one of these values may have changed.
     */
    private void updateCellRendererConfig() {
        if (listCellRenderer != null) {
            listCellRenderer.configure(iconWidth, iconHeight, fieldWidth, _separatorBackground);
        }
    }

    /**
     * Sets the size of the group icons shown in front of the list entries.
     * 0 means no icons are shown. The cell renderer is updated immediately.
     *
     * @param width  icon width in pixels
     * @param height icon height in pixels
     */
    public void setIconSize(int width, int height) {
        this.iconWidth = width;
        this.iconHeight = height;
        updateCellRendererConfig();
    }
    
    /**
     * Does nothing, as the list does not have a single value to get from the GUI. 
     * This method is required by the IItemType interface.
	 */
    public void getValueFromGui() {
    }
    
    /**
	 * Does nothing, as the list does not have a single value to parse. 
	 * This method is required by the IItemType interface.
	 */
    public void parseValue(String value) {
        // nothing to do
    }

    /** 
	 * Sets the size of the list field in the GUI. If a scroll pane is used
	 * 
	 * @param width  the width of the field in pixels
	 * @param height the height of the field in pixels
	 */
    public void setFieldSize(int width, int height) {
        super.setFieldSize(width, height);
        if (scrollPane != null) {
            scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        }
        updateCellRendererConfig();
    }

    /** 
	 * Sets the description text of the list and updates the label if it exists.
	 * @param s the description text to set
	 */
    public void setDescription(String s) {
        super.setDescription(s);
        if (label != null) {
            Mnemonics.setLabel(this.dlg, label, " " + s + ": ");
        }
    }

    /**
     * Sets the selected index of the list to the given index. If the index is out of bounds, no selection is made.
     * @param i
     */
    public void setSelectedIndex(int i) {
        list.setSelectedIndex(i);
    }

    /**
	 * Returns the index of the currently selected item in the list.
	 * @return the index of the selected item in the filtered view, or -1 if no item is selected.
	 * Index space: this index refers to the filtered view, not to the unfiltered data used by getItemText() and getItemObject().
	 */
    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }

    /** 
	 * Returns the panel containing the list and its components (label, filter text field, scroll
	 * pane). This panel can be added to a GUI container.
	 * @return the JPanel containing the list and its components
	 * */
    public JPanel getPanel() {
        return mypanel;
    }

    // functions for documentListener for the filter text field
    /** 
     * Called when the text in the filter text field is inserted or updated. 
     * This method triggers the filtering of the list items based on the current text in the filter text field.
     */
    public void insertUpdate(DocumentEvent e) {
        filter();
    }

    /**
     * Called when the text in the filter text field is removed or updated.
     * This method triggers the filtering of the list items based on the current text in the filter text field.
     */
    public void removeUpdate(DocumentEvent e) {
        filter();
    }

    /**
	 * Called when the text in the filter text field is changed. 
	 * This method triggers the filtering of the list items based on the current text in the filter text field.
	 */
    public void changedUpdate(DocumentEvent e) {
        filter();
    }

    /** 
     * Filters the list items based on the text entered in the filter text field.
     * if the filter text field is empty, the list is reset to show all data.
     */
    private void filter() {
        
		// this.filterTextField actually CAN be null also if this.showFilterField is true
		// this is the fact for personsAvailableList which is only shown after first user interaction - 
		// before the user interaction of showing the personsAvailableList, the filterTextfield is null.
		// so let's avoid a nullpointerexception here. 

    	if (this.showFilterField && this.filterTextField!=null) {

    		boolean ignoreSpecialCharacters = Daten.efaConfig.getValueEfaBoathouseFilterTextfieldEasyFindEntriesWithSpecialCharacters();
    		
    		DefaultListModel<ItemTypeListData> theModel = new DefaultListModel<ItemTypeList.ItemTypeListData>();
			String searchString = filterTextField.getText().trim().toLowerCase();
			boolean searchStringWithUmlaut = EfaUtil.containsUmlaut(searchString);
			
	        if (!searchString.isEmpty()) {
	        	
	        	for (int i=0; i< data.getSize();i++) {
	        		ItemTypeListData item = data.getElementAt(i);

	        		if (item.separator || other_item_text.equals(item.text)) {
		                theModel.addElement(item);	        			
	        		} else {
	        			// this is the slow part
	        			boolean hit=false;
	        			if (ignoreSpecialCharacters) {
	        				if (searchStringWithUmlaut) {
	        					//search string contains a special character. this indicates user is definitely looking
	        					//for an entry that contains this character. So we switch to standard contains mode.
	        					hit = item.getFilterableText().toLowerCase().contains(searchString);
	        				} else {
	        					//search string has no umlaut --> we also want to find entries which contain special characters
		        				hit = EfaUtil.replaceAllUmlautsLowerCaseFast(item.getFilterableText()).contains(searchString);	        					
	        				}

	        			} else {
	        				hit = item.getFilterableText().toLowerCase().contains(searchString);
	        			}

	        			if (hit) {
	        				theModel.addElement(item);
	        			}
	        		  
	        	  }

	        	}
	        	
	        	// we have a problem if there are section strings at the end of the list
	        	// remove all entries from the bottom which start with LIST_SECTION_STRING
	
	        	for (int i= theModel.getSize()-1; i>=0;i--) {
	        		if (theModel.getElementAt(i).separator){
	        			theModel.removeElementAt(i);
	        		}
	        		else {
	        			//we have found a non-section item: break out of the for loop
	        			break;
	        		}
	        	}
	        	
	        	//we also want to remove all consecutive section strings on top of each other, 
	        	//as they do not make sense without entries in between
	        	for (int i = theModel.getSize()-1; i>=0; i--) {
	        		if (theModel.getElementAt(i).separator){
	        			if ((i<theModel.getSize()-1) && (theModel.getElementAt(i+1).separator)) {
	        					//we have found two section strings in a row: remove the first one
	        					theModel.removeElementAt(i);
	        			}
	        		}
	        	}	        	
	        	
		        list.setModel(theModel);
		     } else {
		    	list.setModel(data);
		     }
	        selectFirstMatchingElement();
        } //if showFilterTextField

    }

    /** 
	 * Updates the timestamp of the last filter change to the current system time.
	 * 
	 * @param e the KeyEvent that triggered the update (not used in this method)
	 */
    public void keyPressed(KeyEvent e) {
       	updateLastFilterChange();   	
    }
    
    /**
     * Handles key released events in the filter text field. 
     * If the Enter, Down, or Up keys are released, the focus is moved to the list. 
     * If the Escape key is released, the filter text field is cleared and focus is returned to it.
     */
    public void keyReleased(KeyEvent e) {
		updateLastFilterChange();
	
	    if (e.getKeyCode()== KeyEvent.VK_ENTER 
	    		|| e.getKeyCode() == KeyEvent.VK_DOWN 
	    		|| e.getKeyCode() == KeyEvent.VK_UP) {
	    		list.requestFocus();
	    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
    		if (this.showFilterField) {
	    		filterTextField.setText("");
	    		filterTextField.requestFocus();
    		}
	    }
		    
    }
    
    /**
	 * Updates the timestamp of the last filter change to the current system time.
	 * This method is called when a key is typed in the filter text field.
	 */
    public void keyTyped(KeyEvent e) {
    	updateLastFilterChange();
    }

    /** 
	 * Updates the timestamp of the last filter change to the current system time.
	 * This method is called whenever the filter text field is modified, either by typing, deleting, or pasting text.
	 */
    private void updateLastFilterChange() {
    	lastFilterChange=System.currentTimeMillis(); 
    }
    
    /**
     * Creates a new JList with support for tooltips. 
     * The tooltip text is determined by the ItemTypeListData associated with each list item.
     * 
     * @return a new JList<ItemTypeListData> instance with tooltip support
     * 
     */
    private JList<ItemTypeListData> createNewListWithTooltippSupport() {
    	return new JList<ItemTypeListData>() {

    		public String getToolTipText(MouseEvent me) {
	          try {
		        	if ((this.getToolTipText()!=null) && !this.getToolTipText().isEmpty()){
		        		return this.getToolTipText();
		            } else {     	
			        	int index = locationToIndex(me.getPoint());
			            if (index > -1) {
			            	ItemTypeListData item = (ItemTypeListData) getModel().getElementAt(index);
			               return item.toolTipText;
			            }
	          }
	        	}
	          catch (Exception e) {
	        	  Logger.logdebug(e);
	            return "";
	            }
	          return "";
	        }
    	};
    }
    
    /**
	 * Updates the separator color from the EFA configuration if the flat look and feel is active.
	 * This method should be called whenever the EFA configuration changes or when the look and feel is updated.
	 */
    public void updateSeparatorColorFromEfaConfig() {
        if (Daten.isEfaFlatLafActive()) {
        	this._separatorBackground=Daten.efaConfig.getEfaGuiflatLaf_Background();
        	updateCellRendererConfig();
       }
    }

    /** 
	 * Returns the grid weight in the X direction for layout purposes.
	 * @return the grid weight in the X direction
	 */
	public double getGridWeightX() {
		return gridWeightX;
	}

	/** Sets the grid weight in the X direction for layout purposes.
	 * @param gridWeightX the grid weight in the X direction to set
	 */
	public void setGridWeightX(double gridWeightX) {
		this.gridWeightX = gridWeightX;
	}

	/** Returns the grid weight in the Y direction for layout purposes.
	 * @return the grid weight in the Y direction
	 */
	public double getGridWeightY() {
		return gridWeightY;
	}

	/** Sets the grid weight in the Y direction for layout purposes.
	 * @param gridWeightY the grid weight in the Y direction to set
	 */
	public void setGridWeightY(double gridWeightY) {
		this.gridWeightY = gridWeightY;
	}
}
