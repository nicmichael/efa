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
import de.nmichael.efa.Daten;

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
            
            //Update for standard tables: indent cell content for better readability
            if (c instanceof JComponent) {
      	 		((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
       	 	}
            
            if (isMarked && markedBold) {
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            }
            Color bkgColor = Color.white;
            Color fgColor = Color.black;
            
            if (Daten.efaConfig.getValueEfaDirekt_tabelleAlternierendeZeilenFarben()) {
	            //Update for standard tables: alternating row color
	            Color alternateColor = new Color(219,234,249);
	            bkgColor = (row % 2 == 0 ? alternateColor : Color.white);
            }
	            
            
            if (isSelected) {
                bkgColor = table.getSelectionBackground();
                // Update for standard tables: when selected, we should always use the selection foreground.
                fgColor= table.getSelectionForeground();
            } else {
                if (isDisabled && disabledBkgColor != null) {
                    bkgColor = disabledBkgColor;
                }
                if (isMarked && markedBkgColor != null) {
                    bkgColor = markedBkgColor;
                }
            }
            if (isDisabled && disabledFgColor != null) {
            	// disabled fgColor only to be used when col is not selected
            	// if disabled AND selection applies, use italic font to display the disabled state.
                if (!isSelected) {
                	fgColor = disabledFgColor;
                } else {
            		c.setFont(c.getFont().deriveFont(Font.ITALIC));
                }
                	
            }
            
            if (isMarked && markedFgColor != null) {
            	if (isSelected) {
                    // SGB Update for standard tables: when selected, we should always use the selection foreground.
                    // Marked FG color is red by default, and does not work well with blue as alternating line color.

            		fgColor = table.getSelectionForeground(); 

            	}
            	else {
            		fgColor = markedFgColor;
            	}
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

