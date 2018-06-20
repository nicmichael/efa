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

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.gui.util.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete

public class ItemTypeHtmlList extends ItemType implements ActionListener {

    protected String value;

    protected JList list;
    protected JScrollPane scrollPane;
    protected EfaMouseListener mouseListener;
    protected JPopupMenu popup;
    protected String[] keys;
    protected Hashtable<String,String> items;
    protected String[] popupActions;

    public ItemTypeHtmlList(String name, String[] keys, Hashtable<String,String> items, String value,
            int type, String category, String description) {
        this.name = name;
        this.keys = keys;
        this.items = items;
        this.value = value;
        this.type = type;
        this.category = category;
        this.description = description;
        fieldWidth = 600;
        fieldHeight = 300;
        fieldGridAnchor = GridBagConstraints.CENTER;
        fieldGridFill = GridBagConstraints.NONE;
    }

    public IItemType copyOf() {
        return new ItemTypeHtmlList(name, keys.clone(), (Hashtable<String,String>)items.clone(), value, type, category, description);
    }

    public void showValue() {
        if (list != null) {
            list.removeAll();
            if (keys != null && items != null) {
                String[] elements = new String[keys.length];
                for (int i = 0; i < keys.length; i++) {
                    StringBuffer s = new StringBuffer();
                    s.append(items.get(keys[i]));
                    elements[i] = s.toString();
                    if (Logger.isTraceOn(Logger.TT_GUI, 6)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_ELEMENTS,
                                getClass().getName() + ".showValue() elements[" + i + "] = " + s);
                    }
            }
                list.setListData(elements);
            } else {
                list.setListData(new Object[0]);
            }
            for (int i=0; keys != null && value != null && i<keys.length; i++) {
                if (value.equals(keys[i])) {
                    list.setSelectedIndex(i);
                    list.scrollRectToVisible(list.getCellBounds(i, i));
                    break;
                }
            }
        }
    }

    protected void iniDisplay() {
        if (Logger.isTraceOn(Logger.TT_GUI, 6)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_ELEMENTS, 
                    getClass().getName() + ".iniDisplay() fieldWidth=" + fieldWidth +
                    ", fieldHeight=" + fieldHeight);
        }
        list = new JList();
        list.setCellRenderer(new MyCellRenderer());
        scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        scrollPane.setMinimumSize(new Dimension(fieldWidth, fieldHeight));
        scrollPane.getViewport().add(list, null);

        if (popupActions != null) {
            popup = new JPopupMenu();
            for (int i = 0; i < popupActions.length; i++) {
                JMenuItem menuItem = new JMenuItem(popupActions[i]);
                menuItem.setActionCommand(EfaMouseListener.EVENT_POPUP_CLICKED + "_" + i);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }
        } else {
            popup = null;
        }
        list.addMouseListener(mouseListener = new EfaMouseListener(list, popup, this, false));
        list.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(FocusEvent e) { field_focusGained(e); }
            public void focusLost(FocusEvent e) { field_focusLost(e); }
        });

        showValue();
        this.field = list;
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        this.dlg = dlg;
        iniDisplay();
        panel.add(scrollPane, new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, 0.0, 0.0,
                fieldGridAnchor, fieldGridFill, new Insets(padYbefore, padXbefore, padYafter, 0), 0, 0));
        return 1;
    }

    public int displayOnGui(Window dlg, JPanel panel, String borderLayoutPosition) {
        this.dlg = dlg;
        iniDisplay();
        panel.add(field, borderLayoutPosition);
        return 1;
    }

    public void actionPerformed(ActionEvent e) {
        actionEvent(e);
    }

    public void setValues(String[] keys, Hashtable<String,String> items) {
        this.keys = keys;
        this.items = items;
        showValue();
    }

    public void parseValue(String value) {
        if (value != null) {
            value = value.trim();
        }
        this.value = value;
    }

    public String toString() {
        return value;
    }

    public void getValueFromGui() {
        if (list != null && keys != null && list.getSelectedIndex() >= 0) {
            value = keys[list.getSelectedIndex()];
        }
    }

    public String getValueFromField() {
        if (list != null && keys != null && list.getSelectedIndex() >= 0) {
            return keys[list.getSelectedIndex()];
        }
        return toString(); // otherwise a hidden field in expert mode might return null
    }

    public boolean isValidInput() {
        return true;
    }

    public void setPopupActions(String[] actions) {
        this.popupActions = actions;
    }

    class MyCellRenderer extends JEditorPane implements ListCellRenderer {

        public Component getListCellRendererComponent(
                JList list, // the list
                Object value, // value to display
                int index, // cell index
                boolean isSelected, // is the cell selected
                boolean cellHasFocus) // does the cell have focus
        {
            String s = value.toString();
            setContentType("text/html");
            setText(s);
            if (Logger.isTraceOn(Logger.TT_GUI, 6)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_ELEMENTS,
                        getClass().getName() + ".MyCellRenderer preferred height = " + 
                        getPreferredSize().getHeight() + ", s = " + s);
            }
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            int height = (int) getPreferredSize().getHeight();
            if (height < 25) {
                // some environments have display problems with this list and only show the
                // first item properly, but miscompute the height for subsequent list elements
                // to something very small (e.g. 6). If we get a rediculously small preferred height,
                // we compute one ourselves
                height = Math.max(height, (EfaUtil.countCharInString(s, "<br>") + 1) * 25);
                if (Logger.isTraceOn(Logger.TT_GUI, 6)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_ELEMENTS,
                            getClass().getName() + ".MyCellRenderer setting preferred height = " + height);
                }
                setPreferredSize(new Dimension(fieldWidth, height));
            }
            return this;
        }
    }

    public void setVisible(boolean visible) {
        list.setVisible(visible);
        scrollPane.setVisible(visible);
        super.setVisible(visible);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        list.setEnabled(enabled);
        scrollPane.setEnabled(enabled);
    }

}
