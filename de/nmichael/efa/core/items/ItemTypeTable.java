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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import de.nmichael.efa.Daten;

import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.gui.util.EfaTableCellRenderer;
import de.nmichael.efa.gui.util.Table;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;

// @i18n complete

public class ItemTypeTable extends ItemType implements ActionListener, ITableEditListener {

    protected String value;

    protected Table table;
    protected EfaTableCellRenderer renderer;
    protected JScrollPane scrollPane;
    protected EfaMouseListener mouseListener;
    protected JPopupMenu popup;
    protected TableItemHeader[] header;
    protected String[] keys;
    protected Hashtable<String,TableItem[]> items; // keys -> columns for key
    protected String[] popupActions;
    protected String[] popupIcons;
    protected int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
    protected boolean sortingEnabled = true;
    protected int sortByColumn = 0;
    protected boolean ascending = true;
    protected int fontSize = -1;
    private boolean[] columnEditable;
    private ITableEditListener tableEditListener;
    private boolean toolTipsEnabled = false;
    private boolean intelligentColumnWidthDisabled = false;
    private int minColumnWidth = -1;
    private int[] minColumnWidths = null;
    private int _moveRowSelectionUponNextRefresh = 0;
    private Vector<Integer> permanentSecondarySortingColumns=new Vector<Integer>();

    public ItemTypeTable(String name, TableItemHeader[] header, Hashtable<String,TableItem[]> items, String value,
            int type, String category, String description) {
        ini(name, header, items, value, type, category, description);
    }

    public ItemTypeTable(String name, String[] header, Hashtable<String,TableItem[]> items, String value,
            int type, String category, String description) {
        ini(name, createTableHeader(header), items, value, type, category, description);
    }

    public IItemType copyOf() {
        return new ItemTypeTable(name, header.clone(), (Hashtable<String,TableItem[]>)items.clone(), value, type, category, description);
    }


    private void ini(String name, TableItemHeader[] header, Hashtable<String,TableItem[]> items, String value,
            int type, String category, String description) {
        this.name = name;
        this.header = header;
        this.items = items;
        if (items != null) {
            this.keys = items.keySet().toArray(new String[0]);
            Arrays.sort(keys);
        }
        this.value = value;
        this.type = type;
        this.category = category;
        this.description = description;
        fieldWidth = 600;
        fieldHeight = 300;
        fieldGridAnchor = GridBagConstraints.CENTER;
        fieldGridFill = GridBagConstraints.BOTH;
        this.toolTipsEnabled= Daten.efaConfig.getValueEfaDirekt_tabelleShowTooltip();
        
        padXbefore=10;
        padXafter=10;
        		
    }

    private static TableItemHeader[] createTableHeader(String[] header) {
        TableItemHeader[] h = new TableItemHeader[header.length];
        for (int i=0; i<h.length; i++) {
            h[i] = new TableItemHeader(header[i]);
        }
        return h;
    }

    private TableItem[][] createTableData(String[][] data) {
        TableItem[][] d = new TableItem[data.length][];
        for (int i=0; i<d.length; i++) {
            d[i] = new TableItem[data[i].length];
            for (int j=0; j<data[i].length; j++) {
                d[i][j] = new TableItem(data[i][j], false);
            }
        }
        return d;
    }

    public void showValue() {
        Rectangle currentVisibleRect = null;
        int currentSortingColumn = -1;
        boolean currentSortingAscending = true;
        int currentSelectedRow = -1;
        if (table != null) {
            currentVisibleRect = table.getVisibleRect();
            currentSortingColumn = table.getSortingColumn();
            currentSortingAscending = table.getSortingAscending();
            if (table.getSelectedRowCount() == 1) {
                currentSelectedRow = table.getCurrentRowIndex(table.getSelectedRow()) + _moveRowSelectionUponNextRefresh;

            }
        }

        if (keys != null && items != null) {
            TableItem[][] data = new TableItem[keys.length][];
            for (int i = 0; i < keys.length; i++) {
                data[i] = items.get(keys[i]);
            }
            if (scrollPane != null && table != null) {
                scrollPane.remove(table);
            }
            table = Table.createTable(null, renderer, header, data, sortingEnabled);
            permanentSecondarySortingColumns.forEach((n) -> table.addPermanentSecondarySortingColumn(n));
            if (fontSize > 0) {
                table.getRenderer().setFontSize(fontSize);
                table.setRowHeight(fontSize*2);
            }
            table.setSelectionMode(selectionMode);
            if (sortingEnabled) {
                if (currentSortingColumn < 0) {
                    table.sortByColumn(this.sortByColumn, this.ascending);
                } else {
                    table.sortByColumn(currentSortingColumn, currentSortingAscending);
                }
            }
            if (columnEditable != null) {
                table.setEditableColumns(columnEditable, this);
            }
            table.setToolTipsEnabled(toolTipsEnabled);
            table.disableIntelligentColumnWidth(intelligentColumnWidthDisabled);
            if (minColumnWidth > 0) {
                table.setMinColumnWidth(minColumnWidth);
            }
            if (minColumnWidths != null) {
                table.setMinColumnWidths(minColumnWidths);
            }
        }
        if (scrollPane != null && table != null) {
            scrollPane.getViewport().add(table, null);

            if (popupActions != null) {
                popup = new JPopupMenu();
                for (int i = 0; i < popupActions.length; i++) {
                    JMenuItem menuItem = new JMenuItem(popupActions[i]);
                    menuItem.setActionCommand(EfaMouseListener.EVENT_POPUP_CLICKED + "_" + i);
                    menuItem.addActionListener(this);
                    if (popupIcons != null && popupIcons[i]!=null) {
                    	// small button icons may start with %, so we remove them
                    	// see de.nmichael.efa.core.items.ItemTypeDataRecordTable.BUTTON_IMAGE_CENTERED_PREFIX
                    	if (popupIcons[i].startsWith("%")) {
                			menuItem.setIcon(ImagesAndIcons.getIcon(popupIcons[i].substring(1)));
                		} else {
                			menuItem.setIcon(ImagesAndIcons.getIcon(popupIcons[i]));
                		}
                    }
                    popup.add(menuItem);
                }
            } else {
                popup = null;
            }

            for (int i = 0; keys != null && value != null && i < keys.length; i++) {
                if (value.equals(keys[i])) {
                    scrollToRow(i);
                    break;
                }
            }
            table.addMouseListener(mouseListener = new EfaMouseListener(table, popup, this, false));
            table.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusGained(FocusEvent e) {
                    field_focusGained(e);
                }

                public void focusLost(FocusEvent e) {
                    field_focusLost(e);
                }
            });

            this.field = table;
        }

        if (currentSelectedRow >= 0 && currentSelectedRow < table.getRowCount()) {
            table.setRowSelectionInterval(currentSelectedRow, currentSelectedRow);
        }
        if (value == null && table != null && currentVisibleRect != null) {
            table.scrollRectToVisible(currentVisibleRect);
        }
        _moveRowSelectionUponNextRefresh = 0;
    }

    public void scrollToRow(int i) {
        table.setRowSelectionInterval(i, i);
        table.scrollRectToVisible(table.getCellRect(i, 0, true));
    }

    public void selectAll() {
        table.selectAll();
    }

    public String[] getAllKeys() {
        if (table == null || keys == null) {
            return null;
        }
        return keys;
    }

    public String[] getSelectedKeys() {
        if (table == null) {
            return null;
        }
        int[] rows = table.getSelectedRows();
        if (rows == null) {
            return null;
        }
        String[] keys = new String[rows.length];
        for (int i=0; i<rows.length; i++) {
            keys[i] = this.keys[rows[i]];
        }
        return keys;
    }

    public TableItem[][] getTableData() {
        return (table != null ? table.getTableData() : null);
    }

    public void selectValue(String value) {
        for (int i = 0; keys != null && value != null && i < keys.length; i++) {
            if (value.equals(keys[i])) {
                int currentIdx = table.getCurrentRowIndex(i);
                if (currentIdx >= 0) {
                    scrollToRow(currentIdx);
                }
                break;
            }
        }
    }

    public void selectValues(boolean[] selected) {
        table.clearSelection();
        for (int i=0; i<selected.length; i++) {
            if (selected[i]) {
                table.addRowSelectionInterval(i, i);
            }
        }
    }

    protected void iniDisplay() {
        scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        scrollPane.setMinimumSize(new Dimension(fieldWidth, fieldHeight));
        showValue();
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        this.dlg = dlg;
        iniDisplay();
        //the two 1.0 values make the panel grow with the window.
        panel.add(scrollPane, new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, 1.0, 1.0,
                fieldGridAnchor, fieldGridFill, new Insets(padYbefore, padXbefore+10, padYafter, padXafter+10), 0, 0));
        return 1;
    }

    public int displayOnGui(Window dlg, JPanel panel, String borderLayoutPosition) {
        this.dlg = dlg;
        iniDisplay();
        panel.add(scrollPane, borderLayoutPosition);
        return 1;
    }

    public void actionPerformed(ActionEvent e) {
        actionEvent(e);
    }

    public void setValues(Hashtable<String,TableItem[]> items) {
        this.items = items;
        if (items != null) {
            keys = items.keySet().toArray(new String[0]);
            Arrays.sort(keys);
        }
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
        if (table != null && keys != null && table.getSelectedRow() >= 0) {
            value = keys[table.getSelectedRow()];
        }
    }

    public String getValueFromField() {
        if (table != null && keys != null && table.getSelectedRow() >= 0) {
            return keys[table.getSelectedRow()];
        }
        return toString(); // otherwise a hidden field in expert mode might return null
    }

    public boolean isValidInput() {
        return true;
    }

    public void setPopupActions(String[] actions) {
        this.popupActions = actions;
    }
    
    public void setPopupIcons(String[] actionIconNames) {
    	this.popupIcons=actionIconNames;
    }

    public void setVisible(boolean visible) {
        table.setVisible(visible);
        scrollPane.setVisible(visible);
        super.setVisible(visible);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        table.setEnabled(enabled);
        scrollPane.setEnabled(enabled);
    }

    public void setSelectionMode(int selectionMode) {
        this.selectionMode = selectionMode;
    }

    public void setSortingEnabled(boolean enabled) {
        this.sortingEnabled = enabled;
    }

    public void setSorting(int sortByColumn, boolean ascending) {
        this.sortByColumn = sortByColumn;
        this.ascending = ascending;
    }
    
    public void addPermanentSecondarySortingColumn(int sortByColumn) {
    	this.permanentSecondarySortingColumns.add(new Integer(sortByColumn));
    	if (this.table != null) {
    		this.table.addPermanentSecondarySortingColumn(sortByColumn);
    	}
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public void setEditableColumns(boolean[] columns) {
        columnEditable = columns;
    }

    public void registerTableEditListener(ITableEditListener listener) {
        this.tableEditListener = listener;
    }

    public void tableEditListenerAction(IItemType itemType, TableItem[] items, int row, int col) {
        if (tableEditListener != null) {
            tableEditListener.tableEditListenerAction(this, items, row, col);
        }
    }

    public void setToolTipsEnabled(boolean enabled) {
        toolTipsEnabled = enabled;
    }

    public void disableIntelligentColumnWidth(boolean disabled) {
        intelligentColumnWidthDisabled = disabled;
    }

    public void setMinColumnWidth(int minColumnWidth) {
        this.minColumnWidth = minColumnWidth;
    }

    public void setMinColumnWidths(int[] minColumnWidths) {
        this.minColumnWidths = minColumnWidths;
    }

    public void setMoveRowSelectionUponNextRefresh(int direction) {
        _moveRowSelectionUponNextRefresh = direction;
    }
}
