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

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import javax.swing.*;

// @i18n complete

public class ItemTypeStringList extends ItemTypeLabelValue {

    private String value;
    private String[] valueList;
    private String[] displayList;
    private volatile boolean ignoreItemStateChanges = false;
    private Hashtable<String,String> replaceValues;
    private DefaultListCellRenderer cellRenderer = null;

    public ItemTypeStringList(String name, String value,
            String[] valueList, String[] displayList,
            int type, String category, String description) {
        this.name = name;
        this.value = value;
        this.valueList = valueList;
        this.displayList = displayList;
        this.type = type;
        this.category = category;
        this.description = description;
        this.lastValue = (value != null ? value.toString() : null);
    }

    public IItemType copyOf() {
    	ItemTypeStringList retValue = new ItemTypeStringList(name, value, (valueList != null ? valueList.clone() : null), (displayList != null ? displayList.clone() : null), type, category, description);
    	if (this.cellRenderer!=null) {
    		((ItemTypeStringList) retValue).setCellRenderer(this.cellRenderer);
    	}
    	retValue.setIcon((label == null ? null : label.getIcon()));
    	return retValue;
    }

    protected JComponent initializeField() {
        JComboBox f = new JComboBox();
        for (int i=0; displayList != null && i<displayList.length; i++) {
            f.addItem(displayList[i]);
        }
        f.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(ItemEvent e) { actionEvent(e); }
        });
        f.setVisible(isVisible);
        f.setEnabled(isEnabled);
        if (cellRenderer!=null) {
        	f.setRenderer(cellRenderer);
        }
        showValue();
        return f;
    }

    public String getValueFromField() {
        if (field != null) {
            JComboBox c = (JComboBox)field;
            int idx = c.getSelectedIndex();
            if (idx >= 0 && idx < valueList.length) {
                return valueList[idx];
            }
        }
        return toString(); // otherwise a hidden field in expert mode might return null
    }

    public void showValue() {
        super.showValue();
        for (int i=0; valueList != null && value != null && field != null && i<valueList.length; i++) {
            if (value.equals(valueList[i])) {
                ignoreItemStateChanges = true;
                try {
                    ((JComboBox)field).setSelectedIndex(i);
                } catch(Exception e) {
                }
                ignoreItemStateChanges = false;
                return;
            }
        }
    }

    public void parseValue(String value) {
        if (value != null) {
            value = value.trim();
            if (replaceValues != null && replaceValues.get(value) != null) {
                value = replaceValues.get(value);
            }
        }
        for (int i=0; valueList != null && i<valueList.length; i++) {
            if (valueList[i].equals(value)) {
                this.value = value;
                return;
            }
        }
    }

    public void setListData(String[] valueList, String[] displayList) {
        this.valueList = valueList;
        this.displayList = displayList;
        ignoreItemStateChanges = true;
        try {
            ((JComboBox)field).removeAllItems();
            for (int i=0; displayList != null && i<displayList.length; i++) {
                ((JComboBox)field).addItem(displayList[i]);
            }
        } catch(Exception e) {
        }
        ignoreItemStateChanges = false;
        showValue();
    }

    public String toString() {
        return (value != null ? value : "");
    }

    public String getValue() {
        return value;
    }

    protected void field_focusLost(FocusEvent e) {
        getValueFromGui();
        showValue();
        super.field_focusLost(e);
    }

    public boolean isValidInput() {
        if (isNotNullSet()) {
            if (value == null || value.length() == 0) {
                return false;
            }
        }
        return true;
    }

    public void actionEvent(AWTEvent e) {
        if (!ignoreItemStateChanges) {
            super.actionEvent(e);
        }
    }

    public String[] getValueList() {
        return valueList;
    }

    public String[] getDisplayList() {
        return displayList;
    }
    
    public void setReplaceValues(Hashtable<String,String> replaceValues) {
        this.replaceValues = replaceValues;
    }

    public void setCellRenderer(DefaultListCellRenderer theRenderer) {
    	this.cellRenderer= theRenderer;
    	if (field != null && theRenderer!=null ) {
    		((JComboBox)field).setRenderer(theRenderer);
    	}
    }
}
