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
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.BaseFrame;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedLabel;
import de.nmichael.efa.gui.util.RoundedPanel;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;


/** ItemTypeItemList

Sometimes it's necessary to group several attributes into a logical unit,
and to display multiple elements of these logical units on a single screen.

This is the case, for example, with the "automatic processes" in efaConfig, 
or where multiple boot types can be specified.
This is where the ItemTypeItemList class comes in.

Important properties of this class
----------------------------------------
setStorageType()
By default, EFA stores the data in the exact order it is programmed in the user interface - 
without specifying which configuration element each value is intended for.

This works well as long as the programmer doesn't change the order of the elements. 
If the order of existing elements in the user interface is changed, the stored data cannot be restored to the correct fields.

Therefore, the StorageType "KEYVALUE" can be set for lists. This type stores the respective keys in addition to the values, 
making it independent of the order of the elements in the user interface.

WARNING:

It is NO good idea to convert an existing list (where data has already been written in previous EFA versions) to KEYVALUE.

For ItemTypeItemLists in DataRecords (Boats: Boat types, People: Group membership, Groups: Members, Destinations/Routes: water), 
you should absolutely not change the StorageType to KeyVALUE, as this will make it difficult to restore backups in newer EFA versions 
to older ones.

Similarly, in efaCloud scenarios, the central distribution of master data for boats, people, etc., to the participating EFA stations
would cause major problems if all EFA stations have not been synchronized to the new EFA version. So, having an efaCloud installation
with different versions of efa would then be a problem.

The same applies to storing configuration data in efaConfig, e.g., for automated processes (CronJobs).

setItemsOrientation()
The elements can be aligned vertically or horizontally.
If you want to combine vertical and horizontal layout, use vertical layout and 
additional rendering hints.

setRepeatTitle()
Defines whether the title should be repeated above each new item in the list in the GUI.

setShowUpDownButtons()
Defines whether the order of the items in the GUI can be changed.

Using this class
--------------------------
The using GUI must implement the `IItemFactory` interface.

The `getDefaultItems(String itemName)` method defines the "record," i.e., the logical unit of an
entry's attributes, their order, and their display on the user interface.

Secondly, the associated GUI must implement the `updateGUI()` method.

If the using GUI is a derivative of `BaseTabbedDialog`, no further action is required.
For other GUIs, the `updateGUI()` method must be implemented manually. To avoid repainting problems
when removing items from the list at runtime, this.revalidate() should be called from the updateGUI() method.

*/

public class ItemTypeItemList extends ItemType {

    private static final String LIST_SEPARATOR = "{=%||%=}";
    private static final String ITEM_SEPARATOR = "{=%|%=}";
    private static final String KEY_SEPARATOR = "{=%%=}";

    protected Vector<IItemType[]> items = new Vector<IItemType[]>();
    protected Vector<IItemType[]> deletedItems = new Vector<IItemType[]>();
    private Hashtable<String,String> itemNameMapping = new Hashtable<String,String>();
    private IItemFactory itemFactory;
    private int minNumberOfItems = 0;
    private boolean changed = false;

    private JScrollPane scrollPane;
    private JLabel titlelabel;
    private JButton addButton;
    private Hashtable<JButton,Integer> addButtons;
    private Hashtable<JButton,Integer> delButtons;
    private Hashtable<JButton,Integer> upButtons;
    private Hashtable<JButton,Integer> downButtons;
    private JComponent lastItemStart;
    private IItemType lastItemFocus;
    private int xForAddDelButtons = 2;
    private int padYbetween = 10;
    private boolean repeatTitle = true;
    private String shortDescription = null;
    private int scrollX = -1;
    private int scrollY = -1;
    private int firstColumnMinWidth=0;
    private boolean appendPositionToEachElement = false;
    private Orientation orientation = Orientation.vertical;
    private StorageType storageType = StorageType.classic;
    private Boolean showUpDownButtons = false;
    
    public enum Orientation {
        vertical,
        horizontal
    }
    
    public enum StorageType{
    	classic,
    	keyvalue
    }

    public ItemTypeItemList(String name, Vector<IItemType[]> items, IItemFactory itemFactory,
            int type, String category, String description) {
        this.name = name;
        for (int i=0; items != null && i < items.size(); i++) {
            IItemType[] myItems = items.get(i);
            for (IItemType item : myItems) {
                item.setUnchanged();
            }
            addItems(items.get(i));
        }
        this.itemFactory = itemFactory;
        this.type = type;
        this.category = category;
        this.description = description;
    }

    public IItemType copyOf() {
        ItemTypeItemList copy = new ItemTypeItemList(name, (Vector<IItemType[]>)items.clone(), itemFactory, type, category, description);
        copy.setRepeatTitle(repeatTitle);
        copy.setShortDescription(shortDescription);
        copy.setScrollPane(scrollX, scrollY);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setFirstColumnMinWidth(firstColumnMinWidth);
        copy.setAppendPositionToEachElement(appendPositionToEachElement);
        copy.setXForAddDelButtons(xForAddDelButtons);
        copy.setShowUpDownButtons(showUpDownButtons);
        copy.setStorageType(storageType);
        		
        return copy;
    }

    /**
     * Add items at the end of the list.
     * @param items Items to be added.
     */
    public void addItems(IItemType[] items) {
    	addItems(items, this.items.size());
    }
    
    /** 
     * Add Items below an item on the list.
     * @param items Items to be added.
     * @param afterIndex
     */
    public void addItems(IItemType[] items, int afterIndex) {
        int idxForName = this.items.size();
        lastItemFocus = null;
        for (IItemType item : items) {
        	// check if the subitem's name already consists of the prefixes of the CURRENT itemlist.
        	// only add them if neccessary. this avoids very long item names when additems is used
        	// in the context of copyOf method.
        	String internalName = item.getName();
            if (!item.getName().startsWith(this.getName())){
            	internalName = getName() + "_" + idxForName + "_" + item.getName();
            }
            itemNameMapping.put(internalName, item.getName());
            item.setName(internalName);
            if (item.isVisible() && item.isEnabled() && item.isEditable()) {
                if (lastItemFocus == null) {
                	lastItemFocus = item;
                }
            }
        }
        if (afterIndex==0 || afterIndex>idxForName) {
        	this.items.add(items);
        } else {
        	this.items.add(afterIndex,items);
        }
    }

    public void removeItems(int idx) {
        items.remove(idx);
    }

    public IItemType[] getItems(int idx) {
        return items.get(idx);
    }

    public IItemType getItem(int idx, String name) {
        for (IItemType item : getItems(idx)) {
            if (name.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }

    public IItemType[] getDeletedItems(int idx) {
        return deletedItems.get(idx);
    }

    public String getOriginalItemName(IItemType item) {
        return itemNameMapping.get(item.getName());
    }

    public int size() {
        return items.size();
    }

    public int deletedSize() {
        return deletedItems.size();
    }

    public void setMinNumberOfItems(int min) {
        minNumberOfItems = min;
    }

    public void setXForAddDelButtons(int x) {
        xForAddDelButtons = x;
    }

    public void setPadYbetween(int padding) {
        padYbetween = padding;
    }

    public void setRepeatTitle(boolean repeat) {
        repeatTitle = repeat;
    }

    public void setShortDescription(String s) {
        this.shortDescription = s;
    }

    public String getShortDescription() {
        return (shortDescription != null ? shortDescription : getDescription());
    }

    public void setAppendPositionToEachElement(boolean append) {
        appendPositionToEachElement = append;
    }

    public void setItemsOrientation(Orientation orientation) {
        this.orientation = orientation;
    }
    
    public void setScrollPane(int width, int height) {
        scrollX = width;
        scrollY = height;
    }

    public boolean isChanged() {
        if (changed) {
            return true;
        }
        for (int i=0; i<items.size(); i++) {
            IItemType[] myItems = items.get(i);
            for (IItemType item : myItems) {
                if (item.isChanged()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void iniDisplay() {
        // not used, everything done in displayOnGui(...)
    }
    
    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        this.dlg = dlg;
        
        // is a scrollpane set? then add a scrollpane and put the elements within.
        if (scrollX > 0 && scrollY > 0) {
            scrollPane = new JScrollPane();
            scrollPane.setPreferredSize(new Dimension(scrollX, scrollY));
            scrollPane.setMinimumSize(new Dimension(scrollX, scrollY));
            panel.add(scrollPane, new GridBagConstraints(x, y, 1, 1, 0.0, 0.0,
                  GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            scrollPane.getViewport().add(panel, null);
        }

        //add a description
        titlelabel = new JLabel();
        titlelabel.setBackground(Daten.efaConfig.getHeaderBackgroundColor());
        titlelabel.setForeground(Daten.efaConfig.getHeaderForegroundColor());
        titlelabel.setOpaque(true);
        titlelabel.setFont(titlelabel.getFont().deriveFont(Font.BOLD));
        titlelabel.setText(" " + getDescription());
        
        // we use a roundedPanel as base element so that we can add the caption on the left,
        // and highlight the add button on the right with some prominent text "new" and an extra arrow icon.
        int curYPos = 0;
      	JPanel titlePanel=new RoundedPanel();
    	titlePanel.setLayout(new BorderLayout(5,2));
        titlePanel.setBorder(new RoundedBorder(Daten.efaConfig.getHeaderForegroundColor()));
        titlePanel.setBackground(Daten.efaConfig.getHeaderBackgroundColor());
        titlePanel.setForeground(Daten.efaConfig.getHeaderForegroundColor());
        titlePanel.setOpaque(true);
        titlePanel.setFont(titlelabel.getFont().deriveFont(Font.BOLD));
    
        JLabel iconLabel=new JLabel();
        iconLabel.setText("        "+International.getString("Neu")); // put some gap between the caption and the "new", neccessary in special when itemTypeItemList has no rows.
        iconLabel.setFont(titlelabel.getFont());
        iconLabel.setIconTextGap(4);
        iconLabel.setBackground(Daten.efaConfig.getHeaderBackgroundColor());
        iconLabel.setForeground(Daten.efaConfig.getHeaderForegroundColor());
        iconLabel.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.ARROW_RIGHT_WHITE));
        iconLabel.setHorizontalTextPosition(SwingConstants.LEADING);

        titlePanel.add(titlelabel, BorderLayout.WEST);
        titlePanel.add(iconLabel, BorderLayout.EAST);
        panel.add(titlePanel, new GridBagConstraints(x, y, xForAddDelButtons, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(padYbefore, padXbefore, padYafter , 0), 0, 0));	        
	        
        if (type == IItemType.TYPE_EXPERT) {
            if (!Daten.efaConfig.getHeaderUseHighlightColor()) {
            	titlelabel.setForeground(Color.red);
            }
        }
        if (color != null) {
            titlelabel.setForeground(color);
        }
        
        addButton = new JButton();
        addButton.setIcon(BaseFrame.getIcon("menu_plus.gif"));
        addButton.setMargin(new Insets(0,0,0,0));
        Dialog.setPreferredSize(addButton, 19, 19);
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { addButtonHit(e); }
        });
        EfaGuiUtils.enableAutoScrollOnFocus(addButton);

        panel.add(addButton, new GridBagConstraints(x+xForAddDelButtons, y, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 2, padYafter, padXafter), 0, 0));
        curYPos++;

        if (orientation == Orientation.horizontal) {
        	ensureFirstColumnMinWidth(panel, curYPos, firstColumnMinWidth);
            curYPos++;
        }        
        
        addButtons = new Hashtable<JButton, Integer>();
        delButtons = new Hashtable<JButton,Integer>();
        upButtons = new Hashtable<JButton,Integer>();
        downButtons = new Hashtable<JButton,Integer>();
       
        for (int iCurrentItemListIndex=0; iCurrentItemListIndex<items.size(); iCurrentItemListIndex++) {
            JLabel label = null;
            if (repeatTitle) {
                label = new RoundedLabel();
                label.setBackground(Daten.efaConfig.getTableHeaderBackgroundColor());
                label.setBorder(new RoundedBorder(Daten.efaConfig.getTableHeaderHeaderColor()));
                label.setOpaque(true);
                label.setFont(titlelabel.getFont().deriveFont(Font.BOLD));
                Mnemonics.setLabel(dlg, label, " "+getShortDescription() + " [" + (iCurrentItemListIndex + 1) + "]: ");
                if (type == IItemType.TYPE_EXPERT) {
                    label.setForeground(Color.red);
                }
                if (color != null) {
                    label.setForeground(color);
                }
            }
            JButton delButton = new JButton();
            delButton.setIcon(BaseFrame.getIcon("menu_minus.gif"));
            delButton.setMargin(new Insets(0,0,0,0));
            Dialog.setPreferredSize(delButton, 19, 19);
            delButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) { delButtonHit(e); }
            });
            EfaGuiUtils.enableAutoScrollOnFocus(delButton);
            
            JButton addButtonAtItem = new JButton();
            addButtonAtItem.setIcon(BaseFrame.getIcon("menu_plus.gif"));
            addButtonAtItem.setMargin(new Insets(0,0,0,0));
            Dialog.setPreferredSize(addButtonAtItem, 19, 19);
            addButtonAtItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) { addButtonAfterPositionHit(e); }
            });
            EfaGuiUtils.enableAutoScrollOnFocus(addButtonAtItem);

            JButton upButton =null;
            JButton downButton = null;
            //Only instantiate these buttons if we are to show them
            if (showUpDownButtons) {
                upButton = new JButton();
                upButton.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.ARROW_UP));
                upButton.setMargin(new Insets(0,0,0,0));
                Dialog.setPreferredSize(upButton, 19, 19);
                upButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent e) { upButtonHit(e); }
                });
                EfaGuiUtils.enableAutoScrollOnFocus(upButton);

                downButton = new JButton();
                downButton.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.ARROW_DOWN));
                downButton.setMargin(new Insets(0,0,0,0));
                Dialog.setPreferredSize(downButton, 19, 19);
                downButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent e) { downButtonHit(e); }
                });                
                EfaGuiUtils.enableAutoScrollOnFocus(downButton);

                /*
                 * [label] [upbutton][downbutton] [delete]
                 */
                
	            if (label != null) {
	                panel.add(label, new GridBagConstraints(x, y+curYPos, xForAddDelButtons, 1, 0.0, 0.0,
	                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(padYbetween, padXbefore, padYbetween, 0), 0, 0));
	            }

	            panel.add(delButton, new GridBagConstraints(x+xForAddDelButtons, y+curYPos, 1, 1, 0.0, 0.0,
	                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets((label==null ? 0 : padYbetween), 2, (label==null ? 0 : padYbetween), 0), 0, 0));

	            panel.add(addButtonAtItem, new GridBagConstraints(x+(xForAddDelButtons*2), y+curYPos, 1, 1, 0.0, 0.0,
	                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets((label==null ? 0 : padYbetween), 2, (label==null ? 0 : padYbetween), 0), 0, 0));
	            
	            panel.add(upButton, new GridBagConstraints(x+(xForAddDelButtons*3), y+curYPos, 1, 1, 0.0, 0.0,
	                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets((label==null ? 0 : padYbetween), 2, (label==null ? 0 : padYbetween), 0), 0, 0));

	            panel.add(downButton, new GridBagConstraints(x+(xForAddDelButtons*4), y+curYPos, 1, 1, 0.0, 0.0,
	                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets((label==null ? 0 : padYbetween), 2, (label==null ? 0 : padYbetween), 0), 0, 0));
	            

	            delButtons.put(delButton, iCurrentItemListIndex);
	            addButtons.put(addButtonAtItem, iCurrentItemListIndex);
	            upButtons.put(upButton,  iCurrentItemListIndex);
	            downButtons.put(downButton, iCurrentItemListIndex);
                
            } else { 
	            /*
	             * [label] [delbutton]
	             */
	            if (label != null) {
	                panel.add(label, new GridBagConstraints(x, y+curYPos, 2, 1, 0.0, 0.0,
	                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(padYbetween, padXbefore, padYbetween, 0), 0, 0));
	            }
	            panel.add(delButton, new GridBagConstraints(x+xForAddDelButtons, y+curYPos, 1, 1, 0.0, 0.0,
	                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets((label==null ? 0 : padYbetween), 2, (label==null ? 0 : padYbetween), 0), 0, 0));
	            delButtons.put(delButton, iCurrentItemListIndex);

	            panel.add(addButtonAtItem, new GridBagConstraints(x+(xForAddDelButtons*2), y+curYPos, 1, 1, 0.0, 0.0,
	                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets((label==null ? 0 : padYbetween), 2, (label==null ? 0 : padYbetween), 0), 0, 0));
	            addButtons.put(addButtonAtItem, iCurrentItemListIndex);
	            
            }

            lastItemStart = (label != null ? label : delButton);
            if (repeatTitle) {
                curYPos++;
            }
            
            IItemType[] myItems = items.get(iCurrentItemListIndex);
            int myX = x;
            for (IItemType item : myItems) {
                if (item.getType() != IItemType.TYPE_INTERNAL) {
                    if (appendPositionToEachElement) {
                        String descr = item.getDescription();
                        Matcher m = Pattern.compile("(.*) [0-9]+").matcher(descr);
                        if (m.matches()) {
                            descr = m.group(1);
                        }
                        item.setDescription(descr + " " + (iCurrentItemListIndex+1));
                    }
                    int plusY = item.displayOnGui(dlg, panel, myX, y+curYPos);
                    EfaGuiUtils.enableAutoScrollOnFocus(item.getComponent());
                    if (item instanceof ItemTypeLabelTextfield) { //neccessary for efaBaseFrameMultisession
                    	((ItemTypeLabelTextfield) item).restoreBackgroundColor();
                    }
                    switch (orientation) {
                        case vertical:
                            curYPos += plusY;
                            break;
                        case horizontal:
                            myX+=2; // a label plus the edit field.
                            if (item instanceof ItemTypeStringAutoComplete) {
                            	if (((ItemTypeStringAutoComplete) item).getShowButton()) {
                            		myX++; // additional space for autocomplete button
                            	}
                            }
                            break;
                    }
                }
            }
            if (orientation == Orientation.horizontal) {
                curYPos++; // after each list item we need to increment myY
            }

        }
        return curYPos;
    }

    private void addButtonHit(ActionEvent e) {
        IItemType[] items = itemFactory.getDefaultItems(getName());
        addItems(items);
        changed = true;
        if (dlg instanceof BaseDialog) {
            ((BaseDialog)dlg).updateGui();
            if (lastItemStart != null) {
                ((BaseDialog)dlg).getScrollPane().scrollRectToVisible(lastItemStart.getBounds());
                if (lastItemFocus != null) {
                	lastItemFocus.requestFocus();
                }
            }
        }
        
    }

    private void delButtonHit(ActionEvent e) {
        int idx = delButtons.get(e.getSource());
        if (idx < 0 || idx >= items.size()) {
            return;
        }
        if (items.size()-1 < minNumberOfItems) {
            Dialog.error(International.getMessage("Eintrag kann nicht gelöscht werden, da mindestens {count} Einträge verbleiben müssen.", minNumberOfItems));
            return;
        }
        if (Dialog.yesNoDialog(International.getString("Eintrag löschen"),
                               International.getMessage("Möchtest Du den Eintrag '{entry}' wirklich löschen?",idx+1)) == Dialog.YES) {
            getValueFromGui();
            deletedItems.add(items.get(idx));
            items.remove(idx);
            changed = true;
            if (dlg instanceof BaseDialog) {
                ((BaseDialog)dlg).updateGui();
            }
        }
    }

    private void addButtonAfterPositionHit(ActionEvent e) {
    	//no up action neccessary if there is no element
    	if (items.size()<1) {return;}
    	
        int idx = addButtons.get(e.getSource());
        if (idx < 0 || idx >= items.size()) {
            return;
        }
        
        getValueFromGui(); //save the current element status
    	
        //add the new item at the bottom
        IItemType[] newitems = itemFactory.getDefaultItems(getName());
        addItems(newitems,idx+1);

        changed = true;
        if (dlg instanceof BaseDialog) {
            ((BaseDialog)dlg).updateGui();
            if (lastItemStart != null) {
                ((BaseDialog)dlg).getScrollPane().scrollRectToVisible(lastItemStart.getBounds());
                if (lastItemFocus != null) {
                	lastItemFocus.requestFocus();
                }
            }
        }        
    }
    
    
    private void upButtonHit(ActionEvent e) {
    	//no up action neccessary if there is only one element
    	if (items.size()<=1) {return;}
    	
        int idx = upButtons.get(e.getSource());
        //no up action, if upButton index is invalid or is the topmost element
        if (idx < 0 || idx >= items.size() || idx == 0) {
            return;
        }
        
        getValueFromGui();
        Collections.swap(items, idx, idx-1);
        changed = true;
        if (dlg instanceof BaseDialog) {
            ((BaseDialog)dlg).updateGui();
        }
        
        try {
        	lastItemFocus = ((IItemType[])items.get(idx))[0];
        } catch (Exception e1) {
        	Logger.logdebug(e1);
        	lastItemFocus = null;
        } 
        if (lastItemFocus != null) {
        	lastItemFocus.requestFocus();
        }   
        
    }
    
    private void downButtonHit(ActionEvent e) {
    	//no down action neccessary if there is only one element
    	if (items.size()<=1) {return;}
    	
        int idx = downButtons.get(e.getSource());
        //no up action, if downButton index is invalid or is the lowest element
        if (idx < 0 || idx >= items.size() || idx == items.size()-1) {
            return;
        }
        
        getValueFromGui();
        Collections.swap(items, idx, idx+1);
        changed = true;
        

        if (dlg instanceof BaseDialog) {
        	((BaseDialog)dlg).updateGui();
        }

        try {
        	lastItemFocus = ((IItemType[])items.get(idx+1))[0];
        } catch (Exception e1) {
        	Logger.logdebug(e1);
        	lastItemFocus = null;
        }        
        
        if (lastItemFocus != null) {
        	lastItemFocus.requestFocus();
        }   
    }
    
    public void getValueFromGui() {
        for (int i=0; i<items.size(); i++) {
            IItemType[] myItems = items.get(i);
            for (IItemType item : myItems) {
                item.getValueFromGui();
            }
        }
    }

    public void requestFocus() {
        if (items.size() > 0) {
            IItemType[] myItems = items.get(0);
            if (myItems.length > 0) {
                myItems[0].requestFocus();
            }
        }
    }

    public boolean isValidInput() {
        return true;
    }

    public String getValueFromField() {
        return null;
    }

    public void showValue() {
    }

    public void setVisible(boolean visible) {
        titlelabel.setVisible(visible);
        addButton.setVisible(visible);
        for (int i=0; i<items.size(); i++) {
            IItemType[] myItems = items.get(i);
            for (IItemType item : myItems) {
                item.setVisible(visible);
            }
        }
        JButton[] b = delButtons.keySet().toArray(new JButton[0]);
        for (int i=0; i<b.length; i++) {
            b[i].setVisible(visible);
        }
        super.setVisible(visible);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        titlelabel.setForeground((enabled ? (new JLabel()).getForeground() : Color.gray));
        addButton.setEnabled(enabled);
        for (int i=0; i<items.size(); i++) {
            IItemType[] myItems = items.get(i);
            for (IItemType item : myItems) {
                item.setEnabled(enabled);
            }
        }
        JButton[] b = delButtons.keySet().toArray(new JButton[0]);
        for (int i=0; i<b.length; i++) {
            b[i].setEnabled(enabled);
        }
    }

    public void setUnchanged() {
        changed = false;
        for (int i=0; items != null && i < items.size(); i++) {
            for (IItemType item : items.get(i)) {
                item.setUnchanged();
            }
        }
        super.setUnchanged();
    }

    public String toString() {
    	if (this.storageType==StorageType.classic) {
    		return getClassicStorageString();
    	} else
    	{
    		return getKeyValueStorageString();
    	}
    }

    /**
     * Creates Classic Storage (classic efa) for ItemTypeItemList which is dependent on the order of the items in the GUI.
     * Changing the order of the items on the GUI affects serialization.
     * 
     * @return String with serialization 
     */
    private String getClassicStorageString() {
    	
    	//Build a list 
    	// each List item separated by LIST_SEPARATOR
    	// and each value for a list item separated by ITEM_SEPARATOR
    	StringBuilder s1 = new StringBuilder();
        for (int i=0; items != null && i<items.size(); i++) {
            IItemType[] arr = items.get(i);
            StringBuilder s2 = new StringBuilder();
            for (int j=0; arr != null && j<arr.length; j++) {
                s2.append( (s2.length() > 0 ? ITEM_SEPARATOR : "") + arr[j].toString());
            }
            s1.append( (s1.length() > 0 ? LIST_SEPARATOR : "") + s2.toString());
        }
        return s1.toString();
    }
    /**
     * Creates a serialization where the values have the name of their property as a prefix, separated by KEY_SERPARATOR
     * @return String with Serialization
     * 
     */
    private String getKeyValueStorageString() {
        StringBuilder s1 = new StringBuilder();
        
        for (int curParamItem=0; items != null && curParamItem<items.size(); curParamItem++) {
            IItemType[] curParamFields = items.get(curParamItem);
            StringBuilder s2 = new StringBuilder();
            for (int curField=0; curParamFields != null && curField<curParamFields.length; curField++) {
            	//isstoreItem is set automatically to false for hints, descriptions, headers created by EfaGUIUtils
            	//so we won't store descriptions, hints and such.
            	if (curParamFields[curField].isStoreItem()) {
            		s2.append( (s2.length() > 0 ? ITEM_SEPARATOR : "") + getKey(curParamFields[curField].getName())+KEY_SEPARATOR+curParamFields[curField].toString());
            	}
            }
            s1.append( (s1.length() > 0 ? LIST_SEPARATOR : "") + s2.toString());
        }
        return s1.toString();    	
    }
    
    public void parseValue(String value) {
    	if (this.storageType==StorageType.classic) {
    		parseClassicStorageValue(value);
    	} else
    	{
    		parseKeyValueStorageValue(value);
    	}
    }
    
    /** 
     * Get the items from classic storage format. The items in the value string
     * must be in the same order as the items in the GUI to be successful.
     * 
     * @param value Serialization of the item list.
     */
    private void parseClassicStorageValue(String value) {
        items = new Vector<IItemType[]>();
        if (value == null) {
            return;
        }
        String[] list = EfaUtil.split(value, LIST_SEPARATOR);
        for (int i=0; list != null && i<list.length; i++) {
            if (list[i] == null) {
                continue;
            }
            String[] values = EfaUtil.split(list[i], ITEM_SEPARATOR);
            IItemType[] arr = itemFactory.getDefaultItems(getName());
            for (int j=0; values != null && arr != null && j<values.length && j<arr.length; j++) {
                arr[j].parseValue(values[j]);
            }
            items.add(arr);
        }
    }
    
    /**
     * Get the items in the ItemTypeList from a String serialisation, which
     * contains keys (names of the items) AND their value. 
     * Restores all items whose names can be found in the GUI item list.
     * Order of the items in the serialisation and GUI can differ without causing any harm.
     * @param value Serialisation of the itemlist
     */
    private void parseKeyValueStorageValue(String value) {
    	//very important to clear item list as otherwise the items in the value parameter will get added.
        items = new Vector<IItemType[]>(); //clear subitems
    	if (value == null) {
            return;
        }
        
        if (!value.contains(KEY_SEPARATOR)){
    		parseClassicStorageValue(value);
    		return;
    	}
    	
        //get list elements (which consist of key(name) and value of an item=
        String[] elementsArray = EfaUtil.split(value, LIST_SEPARATOR);
        for (int i=0; elementsArray != null && i<elementsArray.length; i++) {
            if (elementsArray[i] == null) {
                continue;
            }
            
            //separate current list element subitems and put them in a hashmap
            String[] keyValueString = EfaUtil.split(elementsArray[i], ITEM_SEPARATOR);
            HashMap<String, String> kvPairsHash=new HashMap<String, String>();

            for (int curKVPair=0; keyValueString != null && curKVPair<keyValueString.length; curKVPair++) {
                String[] kvPair=EfaUtil.split(keyValueString[curKVPair], KEY_SEPARATOR);
                if (kvPair.length>1) {
                	kvPairsHash.put(kvPair[0], kvPair[1]); 
                }
            }
            
            //now run through the target items on the GUI and ask each for their name.
            //look up their name in the hashmap and fill them with the value for their name.
            //This is stable to changes in the GUI layout, e.g. changing the order, or adding/removing fields.

            //get all configuration items from the factory for this list (by name) to fill them with values
            IItemType[] guiItems = itemFactory.getDefaultItems(getName());
            for (int curGUIItemNo=0; guiItems != null && curGUIItemNo<guiItems.length; curGUIItemNo++) {
                IItemType cur=guiItems[curGUIItemNo];
                try {
                	//for some unknown reason, sometimes the item's name just is the single item's name,
                	//and sometimes it has the full path like  ITEMTYPELISTNAME_0_ITEMNAME.
                	//to avoid this confusion, we simply get the true simple name of the item by truncating
                	//everything before the last "_"
                	String val=kvPairsHash.get(getKey(cur.getName()));
                	if (val!=null) {
                		cur.parseValue(val); //set it from the serialization
                	}
                } catch(Exception e) {
                	Logger.log(e);
                }
            }
            items.add(guiItems);//after deserialisation, add it to the item list
        }
    }
    
    private String getKey(String value) {
    	int posTiefstrich=value.lastIndexOf("_");
    	if (posTiefstrich>0) {
    		return value.substring(posTiefstrich+1, value.length());
    	} else {
    		return value;
    	}
    }
    
    public void setFirstColumnMinWidth(int width) {
        firstColumnMinWidth=width;
    }

    public int getFirstColumnMinWidth() {
    	return firstColumnMinWidth;
    }
    
    private void ensureFirstColumnMinWidth(JPanel panel, int yPos, int minWidth) {
    	if (minWidth>0) {
	    	JPanel spacer = new JPanel();
	    	Dimension dim = new Dimension(minWidth, 0);
	    	spacer.setMinimumSize(dim);
	    	spacer.setPreferredSize(dim);
	    	panel.add(spacer, new GridBagConstraints(0, yPos, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0));
    	}
    }

    public int getItemCount() {
    	return items.size();
    }

	public Boolean getShowUpDownButtons() {
		return showUpDownButtons;
	}

	public void setShowUpDownButtons(Boolean showUpDownButtons) {
		this.showUpDownButtons = showUpDownButtons;
	}
	
	public void setStorageType(StorageType value) {
		this.storageType = value;
	}
    
}
