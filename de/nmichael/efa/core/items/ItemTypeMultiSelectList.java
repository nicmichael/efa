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

import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.types.DataTypeList;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;

public class ItemTypeMultiSelectList<T> extends ItemType implements ActionListener {

    DataTypeList<T> value;
    JPanel mypanel;
    JLabel label;
    JScrollPane scrollPane;
    JList list = new JList();
    T[] keyData;
    String[] displayData;
    int xoffset = 0;
    int yoffset = 0;

    public ItemTypeMultiSelectList(String name, DataTypeList<T> value, T[] keyData, String[] displayData,
            int type, String category, String description) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.category = category;
        this.description = description;
        this.keyData = keyData;
        this.displayData = displayData;
        fieldWidth = 300;
        fieldHeight = 100;
    }

    public ItemTypeMultiSelectList copyOf() {
        ItemTypeMultiSelectList mylist = new ItemTypeMultiSelectList(name, new DataTypeList<T>(value), keyData.clone(), displayData.clone(), type, category, description);
        mylist.setFieldGrid(this.fieldGridWidth, this.fieldGridAnchor, this.fieldGridFill);
        mylist.setFieldSize(this.fieldWidth, this.fieldHeight);
        mylist.setPadding(this.padXbefore, this.padXafter, this.padYbefore, this.padYafter);
        return mylist;
    }

    public int size() {
        if (keyData == null) {
            return 0;
        }
        return keyData.length;
    }

    public String getItemText(int idx) {
        if (idx >= 0 && idx < displayData.length) {
            return displayData[idx];
        }
        return null;
    }

    public T getItemKey(int idx) {
        if (idx >= 0 && idx < keyData.length) {
            return keyData[idx];
        }
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.itemListenerAction(this, e);
        }
    }

    protected void iniDisplay() {
        // not used, everything done in displayOnGui(...)
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        panel.add(setupPanel(dlg), new GridBagConstraints(x+xoffset, y+yoffset, fieldGridWidth, fieldGridHeight, 0.0, 0.0,
                fieldGridAnchor, fieldGridFill, new Insets(padYbefore, 0, padYafter, padXafter), 0, 0));
        showValue();
        return 1;
    }

    public int displayOnGui(Window dlg, JPanel panel, String borderLayoutOrientation) {
        panel.add(setupPanel(dlg), borderLayoutOrientation);
        showValue();
        return 1;
    }

    private JPanel setupPanel(Window dlg) {
        this.dlg = dlg;

        list = new JList();
        scrollPane = new JScrollPane();
        mypanel = new JPanel();
        mypanel.setLayout(new BorderLayout());

        if (getDescription() != null) {
            label = new JLabel();
            Mnemonics.setLabel(dlg, label, getDescription() + ": ");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (type == IItemType.TYPE_EXPERT) {
                label.setForeground(Color.red);
            }
            if (color != null) {
                label.setForeground(color);
            }
            label.setLabelFor(list);
            Dialog.setPreferredSize(label, fieldWidth, 20);
        }

        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));

        scrollPane.getViewport().add(list, null);
        mypanel.setLayout(new BorderLayout());
        if (getDescription() != null) {
            mypanel.add(label, BorderLayout.NORTH);
        }
        mypanel.add(scrollPane, BorderLayout.CENTER);
        setEnabled(this.isEnabled);

        return mypanel;
    }

    public boolean isFocusOwner() {
        return list.isFocusOwner();
    }

    public Object[] getSelectedKeys() {
        try {
            if (list == null || list.isSelectionEmpty()) {
                return null;
            }
            int[] indices = list.getSelectedIndices();
            if (indices == null || indices.length == 0) {
                return null;
            }
            Object[] keys = new Object[indices.length];
            for (int i=0; i<indices.length; i++) {
                keys[i] = keyData[indices[i]];
            }
            return keys;
        } catch (Exception e) {
            return null;
        }
    }

    public Object[] getValues() {
        Object[] v = new Object[value.length()];
        for (int i=0; i<v.length; i++) {
            v[i] = value.get(i);
        }
        return v;
    }

    public boolean isValidInput() {
        return true;
    }

    public String getValueFromField() {
        DataTypeList<T> fieldValue = new DataTypeList<T>();
        Object[] values = getSelectedKeys();
        for (int i=0; values != null && i<values.length; i++) {
            fieldValue.add((T)values[i]);
        }
        return fieldValue.toString();
    }

    public void showValue() {
        list.setListData(displayData);
        Vector<Integer> iv = new Vector<Integer>();
        for (int i=0; value != null && i<value.length(); i++) {
            T key = value.get(i);
            int idx = -1;
            for (int j=0; j<keyData.length; j++) {
                if (keyData[j].equals(key)) {
                    idx = j;
                }
            }
            if (idx >= 0) {
                iv.add(idx);
            }
        }
        int[] indices = new int[iv.size()];
        for (int i=0; i<indices.length; i++) {
            indices[i] = iv.get(i);
        }
        list.setSelectedIndices(indices);
    }

    public void getValueFromGui() {
        value = new DataTypeList<T>();
        Object[] values = getSelectedKeys();
        for (int i=0; values != null && i<values.length; i++) {
            value.add((T)values[i]);
        }
    }

    public String toString() {
        return (value != null ? value.toString() : "");
    }

    public void setValue(DataTypeList<T> v) {
        this.value = v;
    }

    public void parseValue(String value) {
        this.value = DataTypeList.parseList(value, IDataAccess.DATA_STRING);
    }

    public void setFieldSize(int width, int height) {
        super.setFieldSize(width, height);
        if (scrollPane != null) {
            scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        }
    }

    public void setDescription(String s) {
        super.setDescription(s);
        if (label != null) {
            label.setText(s);
        }
    }

    public void setSelectedIndex(int i) {
        list.setSelectedIndex(i);
    }

    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }

    public JPanel getPanel() {
        return mypanel;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (label != null) {
            label.setForeground((enabled ? (new JLabel()).getForeground() : Color.gray));
        }
        if (list != null) {
            list.setEnabled(enabled);
        }
    }

    public void setListData(T[] keyData, String[] displayData) {
        this.keyData = keyData;
        this.displayData = displayData;
    }

    public void setXOffset(int xoffset) {
        this.xoffset = xoffset;
    }

    public void setYOffset(int yoffset) {
        this.yoffset = yoffset;
    }
}
