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
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedLabel;
import de.nmichael.efa.gui.util.RoundedPanel;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;

public class ItemTypeItemList extends ItemType {

    private static final String LIST_SEPARATOR = "{=%||%=}";
    private static final String ITEM_SEPARATOR = "{=%|%=}";

    private Vector<IItemType[]> items = new Vector<IItemType[]>();
    private Vector<IItemType[]> deletedItems = new Vector<IItemType[]>();
    private Hashtable<String,String> itemNameMapping = new Hashtable<String,String>();
    private IItemFactory itemFactory;
    private int minNumberOfItems = 0;
    private boolean changed = false;

    private JScrollPane scrollPane;
    private JLabel titlelabel;
    private JButton addButton;
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
    private Boolean showUpDownButtons = false;
    
    public enum Orientation {
        vertical,
        horizontal
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
        copy.repeatTitle = repeatTitle;
        copy.shortDescription = shortDescription;
        copy.scrollX = scrollX;
        copy.scrollY = scrollY;
        copy.padXafter = padXafter;
        copy.padXbefore = padXbefore;
        copy.padYafter = padYafter;
        copy.padYbefore = padYbefore;
        copy.setXForAddDelButtons(xForAddDelButtons);
        copy.setShowUpDownButtons(showUpDownButtons);
        		
        return copy;
    }

    public void addItems(IItemType[] items) {
        int idx = this.items.size();
        lastItemFocus = null;
        for (IItemType item : items) {
            String internalName = getName() + "_" + idx + "_" + item.getName();
            itemNameMapping.put(internalName, item.getName());
            item.setName(internalName);
            if (item.isVisible() && item.isEnabled() && item.isEditable()) {
                if (lastItemFocus == null) {
                	lastItemFocus = item;
                }
            }
        }
        this.items.add(items);
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

        panel.add(addButton, new GridBagConstraints(x+xForAddDelButtons, y, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 2, padYafter, padXafter), 0, 0));
        curYPos++;

        if (orientation == Orientation.horizontal) {
        	ensureFirstColumnMinWidth(panel, curYPos, firstColumnMinWidth);
            curYPos++;
        }        
        
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
                downButton = new JButton();
                downButton.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.ARROW_DOWN));
                downButton.setMargin(new Insets(0,0,0,0));
                Dialog.setPreferredSize(downButton, 19, 19);
                downButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent e) { downButtonHit(e); }
                });                
                /*
                 * [label] [upbutton][downbutton] [delete]
                 */
                
	            if (label != null) {
	                panel.add(label, new GridBagConstraints(x, y+curYPos, 2, 1, 0.0, 0.0,
	                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(padYbetween, padXbefore, padYbetween, 0), 0, 0));
	            }

	            panel.add(delButton, new GridBagConstraints(x+xForAddDelButtons, y+curYPos, 1, 1, 0.0, 0.0,
	                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets((label==null ? 0 : padYbetween), 2, (label==null ? 0 : padYbetween), 0), 0, 0));
	            
	            panel.add(upButton, new GridBagConstraints(x+xForAddDelButtons+xForAddDelButtons, y+curYPos, 1, 1, 0.0, 0.0,
	                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets((label==null ? 0 : padYbetween), 2, (label==null ? 0 : padYbetween), 0), 0, 0));

	            panel.add(downButton, new GridBagConstraints(x+xForAddDelButtons+xForAddDelButtons+xForAddDelButtons, y+curYPos, 1, 1, 0.0, 0.0,
	                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets((label==null ? 0 : padYbetween), 2, (label==null ? 0 : padYbetween), 0), 0, 0));
	            

	            delButtons.put(delButton, iCurrentItemListIndex);
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
            }
            if (lastItemFocus != null) {
                lastItemFocus.requestFocus();
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

    public void parseValue(String value) {
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
    
}
