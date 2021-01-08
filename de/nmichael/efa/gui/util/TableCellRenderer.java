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

import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.util.Vector;

public class TableCellRenderer extends DefaultTableCellRenderer {

    private boolean markedBold = true;
    private Color markedBkgColor = new Color(0xff,0xff,0xaa);
    private Color markedFgColor = null;
    private Color disabledBkgColor = null;
    private Color disabledFgColor = Color.gray;
    private int fontSize = -1;

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        try {
            if (value == null) {
                return null;
            }
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            boolean isMarked = value instanceof TableItem && ((TableItem)value).isMarked();
            boolean isDisabled = value instanceof TableItem && ((TableItem)value).isDisabled();
            String txt = value.toString();
            if (isMarked && markedBold) {
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            }
            Color bkgColor = Color.white;
            Color fgColor = Color.black;
            if (isSelected) {
                bkgColor = table.getSelectionBackground();
            } else {
                if (isDisabled && disabledBkgColor != null) {
                    bkgColor = disabledBkgColor;
                }
                if (isMarked && markedBkgColor != null) {
                    bkgColor = markedBkgColor;
                }
            }
            if (isDisabled && disabledFgColor != null) {
                fgColor = disabledFgColor;
            }
            if (isMarked && markedFgColor != null) {
                fgColor = markedFgColor;
            }
            if (fontSize > 0) {
                c.setFont(c.getFont().deriveFont((float)fontSize));
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            }

            c.setBackground(bkgColor);
            c.setForeground(fgColor);
            return this;
        } catch (Exception e) {
            return null;
        }
    }

    public void setMarkedBold(boolean bold) {
        this.markedBold = bold;
    }

    public void setMarkedForegroundColor(Color c) {
        this.markedFgColor = c;
    }

    public void setMarkedBackgroundColor(Color c) {
        this.markedBkgColor = c;
    }

    public void setDisabledForegroundColor(Color c) {
        this.disabledFgColor = c;
    }

    public void setDisabledBackgroundColor(Color c) {
        this.disabledBkgColor = c;
    }

    public void setFontSize(int size) {
        this.fontSize = size;
    }

}

