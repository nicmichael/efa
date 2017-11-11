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

import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.BaseFrame;

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
    private JComponent lastItemStart;
    private IItemType lastItemFocus;
    private int xForAddDelButtons = 2;
    private int padYbetween = 10;
    private boolean repeatTitle = true;
    private String shortDescription = null;
    private int scrollX = -1;
    private int scrollY = -1;
    private boolean appendPositionToEachElement = false;
    private Orientation orientation = Orientation.vertical;

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
        return copy;
    }

    public void addItems(IItemType[] items) {
        int idx = this.items.size();
        for (IItemType item : items) {
            String internalName = getName() + "_" + idx + "_" + item.getName();
            itemNameMapping.put(internalName, item.getName());
            item.setName(internalName);
            if (item.isVisible() && item.isEnabled() && item.isEditable()) {
                lastItemFocus = item;
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
        
        int myY = 0;
        titlelabel = new JLabel();
        Mnemonics.setLabel(dlg, titlelabel, getDescription() + ": ");
        if (type == IItemType.TYPE_EXPERT) {
            titlelabel.setForeground(Color.red);
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

        panel.add(titlelabel, new GridBagConstraints(x, y, 2, 1, 0.0, 0.0,
                  GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, padXbefore, (items.size() == 0 ? padYafter : 0), 0), 0, 0));
        panel.add(addButton, new GridBagConstraints(x+xForAddDelButtons, y, 2, 1, 0.0, 0.0,
                  GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 2, (items.size() == 0 ? padYafter : 0), padXafter), 0, 0));
        myY++;

        delButtons = new Hashtable<JButton,Integer>();
        for (int i=0; i<items.size(); i++) {
            JLabel label = null;
            if (repeatTitle) {
                label = new JLabel();
                Mnemonics.setLabel(dlg, label, getShortDescription() + " [" + (i + 1) + "]: ");
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

            if (label != null) {
                panel.add(label, new GridBagConstraints(x, y+myY, 2, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(padYbetween, padXbefore, 0, 0), 0, 0));
            }
            panel.add(delButton, new GridBagConstraints(x+xForAddDelButtons, y+myY, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbetween, 2, 0, 0), 0, 0));
            delButtons.put(delButton, i);
            lastItemStart = (label != null ? label : delButton);
            if (repeatTitle) {
                myY++;
            }
            
            IItemType[] myItems = items.get(i);
            int myX = x;
            for (IItemType item : myItems) {
                if (item.getType() != IItemType.TYPE_INTERNAL) {
                    if (appendPositionToEachElement) {
                        String descr = item.getDescription();
                        Matcher m = Pattern.compile("(.*) [0-9]+").matcher(descr);
                        if (m.matches()) {
                            descr = m.group(1);
                        }
                        item.setDescription(descr + " " + (i+1));
                    }
                    int plusY = item.displayOnGui(dlg, panel, myX, y+myY);
                    switch (orientation) {
                        case vertical:
                            myY += plusY;
                            break;
                        case horizontal:
                            myX++;
                            break;
                    }
                }
            }
            if (orientation == Orientation.horizontal) {
                myY++; // after each list item we need to increment myY
            }

        }
        return myY;
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

}
