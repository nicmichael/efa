/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.gui.util;

import de.nmichael.efa.core.items.ITableEditListener;
import de.nmichael.efa.gui.*;
import de.nmichael.efa.util.Logger;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;

public class Table extends JTable {

    BaseDialog dlg;
    TableSorter sorter;
    TableCellRenderer renderer;
    TableItemHeader[] header;
    TableItem[][] data;
    private boolean dontResize = false;
    private boolean[] columnEditable;
    private ITableEditListener editListener;
    private boolean toolTipsEnabled = false;
    private boolean intelligentColumnWidthDisabled = false;
    private int minColumnWidth = 50;
    private int[] minColumnWidths = null;

    public Table(BaseDialog dlg, TableSorter sorter, TableCellRenderer renderer, 
            TableItemHeader[] header, TableItem[][] data, boolean allowSorting) {
        super(sorter);
        this.dlg = dlg;
        this.sorter = sorter;
        this.renderer = renderer;
        this.header = header;
        this.data = data;

        if (renderer == null) {
            renderer = new TableCellRenderer();
        }
        setDefaultRenderer(Object.class, renderer);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        if (allowSorting) {
            sorter.addMouseListenerToHeaderInTable(this);
        }
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancel();
                }
            }
        });
        addMouseListener(new TableMouseListener());

        validate();
    }

    public boolean isCellEditable(int row, int column) {
        if (columnEditable == null || column < 0 || column >= columnEditable.length) {
            return false;
        }
        return columnEditable[column];
    }

    public void setEditableColumns(boolean[] columns, ITableEditListener editListener) {
        columnEditable = columns;
        this.editListener = editListener;
    }

    public boolean editCellAt(int row, int column, EventObject e) {
        return super.editCellAt(row, column, e);
    }

    /**
     * This is certainly a very messy method, and it's not very elegant.
     * But it works.
     * DON'T CHANGE IT!!
     */
    public void editingStopped(ChangeEvent e) {
        int row = editingRow;
        int col = editingColumn;
        super.editingStopped(e);
        sortByColumn(getSortingColumn());
        try {
            int origRow = getOriginalIndex(row);
            TableItem[] items = new TableItem[data[origRow].length];
            for (int i=0; i<items.length; i++) {
                items[i] = getTableItem(origRow, i);
            }
            items[col].setText(getValueAt(row, col).toString());
            getModel().setValueAt(items[col], row, col);
            sortByColumn(getSortingColumn());
            editListener.tableEditListenerAction(null, items, origRow, col);
            doLayout();
        } catch (Exception ex) {
            Logger.logdebug(ex);
        }
    }

    public void doLayout() {
        super.doLayout();
        if (!dontResize) {
            dontResize = true;
            if (!intelligentColumnWidthDisabled) {
                setIntelligentColumnWidth();
            }
            validate();
            dontResize = false;
        }
    }

    private void setIntelligentColumnWidth() {
        int width = getSize().width;
        if (width < this.getSize().width - 20 || width > this.getSize().width) { // beim ersten Aufruf steht Tabellenbreite noch nicht (korrekt) zur Verfügung, daher dieser Plausi-Check
            width = this.getSize().width - 10;
        }
        
        if (header == null) {
            return;
        }

        int absoluteWidth = 0;
        for (int i=0; i<header.length; i++) {
            absoluteWidth += header[i].getMaxColumnWidth();
        }

        int[] widths = new int[header.length];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = (int) Math.floor((((float)header[i].getMaxColumnWidth()) / ((float)absoluteWidth)) * ((float)width));
            if (widths[i] < minColumnWidth) {
                widths[i] = minColumnWidth;
            }
            if (minColumnWidths != null && i < minColumnWidths.length &&
                widths[i] < minColumnWidths[i]) {
                widths[i] = minColumnWidths[i];
            }
        }

        for (int i = 0; i < widths.length; i++) {
            getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public int getOriginalIndex(int currentIndex) {
        return sorter.getOriginalIndex(currentIndex);
    }
    
    public int getCurrentRowIndex(int originalIndex) {
        return sorter.getCurrentIndex(originalIndex);
    }

    public int getSelectedRow() {
        int row = super.getSelectedRow();
        if (row >= 0) {
            return sorter.getOriginalIndex(row);
        } else {
            return row;
        }
    }

    public int[] getSelectedRows() {
        int[] rows = super.getSelectedRows();
        for (int i=0; rows != null && i<rows.length; i++) {
            rows[i] = sorter.getOriginalIndex(rows[i]);
        }
        return rows;
    }

    public TableItem getTableItem(int row, int col) {
        return data[row][col];
    }

    public TableItem[][] getTableData() {
        return data;
    }

    private void cancel() {
        if (dlg != null) {
            dlg.cancel();
        }
    }
    
    public static Table createTable(BaseDialog dlg, TableItemHeader[] header, TableItem[][] data) {
        return createTable(dlg, null, header, data);
    }

    public static Table createTable(BaseDialog dlg, TableCellRenderer renderer, TableItemHeader[] header, TableItem[][] data) {
        return createTable(dlg, renderer, header, data, true);
    }

    public static Table createTable(BaseDialog dlg, TableCellRenderer renderer, TableItemHeader[] header,
            TableItem[][] data, boolean allowSorting) {
        for (int i=0; i<data.length; i++) {
            for (int j=0; j<data[i].length; j++) {
                header[j].updateColumnWidth(data[i][j].toString());
            }
        }
        TableSorter sorter = new TableSorter(new DefaultTableModel(data, header));
        Table t = new Table(dlg, sorter, renderer, header, data, allowSorting);
        return t;
    }

    public void sortByColumn(int column) {
        sortByColumn(column, true);
    }

    public void sortByColumn(int column, boolean ascending) {
        sorter.sortByColumn(column, ascending);
    }

    public int getSortingColumn() {
        return sorter.getSortingColumn();
    }

    public boolean getSortingAscending() {
        return sorter.getSortingAscending();
    }

    public TableCellRenderer getRenderer() {
        return renderer;
    }

    class TableMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.getSource() instanceof Table) {
                Table t = (Table)e.getSource();
                if (e.getButton() == 3 && e.getClickCount() == 1) {
                    int row = t.rowAtPoint(new Point(e.getX(), e.getY()));
                    if (row >= 0) {
                        t.setRowSelectionInterval(row, row);
                    }
                }
            }
            super.mousePressed(e);
        }
    }

    public String getToolTipText(MouseEvent event) {
        try {
            if (toolTipsEnabled) {
                int row = rowAtPoint(event.getPoint());
                int col = columnAtPoint(event.getPoint());
                return getValueAt(row, col).toString();
            }
        } catch (Exception eignore) {
        }
        return null;
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
}
