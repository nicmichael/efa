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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.util.Logger;


/**
 * EfaTableCellRenderer 
 * 
 * This renderer uses a panel consisting of two labels:
 * - textlabel (left-aligned) for the text
 * - iconLabel (right-aligned) for icons.
 * 
 * The iconlabel can show multiple icons, which can be specified with addIcon() method.
 * Icons are displayed in the order they are added.
 *   
 */
public class EfaTableCellRenderer implements TableCellRenderer {

	private boolean markedBold = true;
    private Color markedBkgColor = new Color(0xff,0xff,0xaa);
    private Color alternateColor = new Color(219,234,249);
    private Color markedFgColor = null;
    private Color disabledBkgColor = null;
    private Color disabledFgColor = Color.gray;
    private int fontSize = -1;
    private Icon hiddenIcon = ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_BUTTON_HIDE);
    private boolean useAlternatingColor= false;
    
    private JComponent c;
    private JLabel textLabel;
    private JLabel iconLabel;
    
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        try {
        	Vector <Icon> iconList=null; // do not instantiate yet, due to performance issues
        	
        	// first we create the textlabel. if there are no icons to be shown, the
        	// the whole cell renderer just consists of the textlabel.
        	textLabel = new JLabel();
            textLabel.setHorizontalAlignment(SwingConstants.LEFT);
            textLabel.setText(value!=null ? value.toString() : "");
            
            TableItem myItem = (value instanceof TableItem ? ((TableItem) value) : null);
            boolean isMarked = myItem != null && myItem.isMarked();
            boolean isDisabled = myItem != null && myItem.isDisabled();
            boolean isInvisible = myItem != null && myItem.isInvisible();
            
        	// Set Icon for first column only if item is hidden.
            // So users get an idea why this entry is displayed in disabled color.
            // The tooltip is set in TableItem.java and shown in Table.java

            if (column==0 && (isInvisible)) {
                iconList = new Vector<Icon>();
            	iconList.add(hiddenIcon);
            }
            
            if (column==0 && myItem!=null && myItem.getIcons()!=null) {
            	if (iconList == null) { 
            		iconList = new Vector<Icon>(); 
            	}
            	iconList.addAll(myItem.getIcons());
            }
            
            if (myItem!=null && iconList != null && iconList.size()>0) {
            	CompoundIcon myIcon=  new CompoundIcon(CompoundIcon.Axis.X_AXIS,2,CompoundIcon.RIGHT, CompoundIcon.CENTER,iconList);
                iconLabel = new JLabel();            
                iconLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            	iconLabel.setIcon(myIcon);
                c = new JPanel();
                c.setLayout(new GridBagLayout());
                //textabel left-aligned, may grow. iconlabel right-alignet, may not grow. Insets will be set for the whole component afterwards
                c.add(textLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            	c.add(iconLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            } else {
            	c = textLabel;
            }            
            
            //Update for standard tables: indent cell content for better readability
            c.setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
            try {
            	//c.setFont(UIManager.getFont("Table.font"));
            	textLabel.setFont(UIManager.getFont("Table.font"));
            } catch (Exception e) {
            	Logger.logdebug(e);
            }
            
            if (isMarked && markedBold) {
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            }

            Color bkgColor = null;
            Color fgColor = Color.black;

            if (this.useAlternatingColor) {
	            bkgColor = (row % 2 == 0 ? alternateColor : null);
            }  
            
            if (isSelected) {
                bkgColor = table.getSelectionBackground();
                // Update for standard tables: when selected, we should always use the selection foreground.
                fgColor= table.getSelectionForeground();
                if (c.getFont().isBold()) {
                	c.setFont(c.getFont().deriveFont(Font.BOLD | Font.ITALIC));
                } else {
                	c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
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
            }

            c.setBackground(bkgColor);
            c.setForeground(fgColor);
            c.setOpaque(true);
            textLabel.setForeground(fgColor);
            textLabel.setBackground(bkgColor);
            textLabel.setOpaque(true);
            
            return c;
        } catch (Exception e) {
            Logger.logdebug(e);
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

    public void setAlternatingRowColor(Color c) {
    	this.alternateColor = c;
    	this.useAlternatingColor=(c!=null && Daten.efaConfig.getValueEfaDirekt_tabelleAlternierendeZeilenFarben());
    }
    public void setFontSize(int size) {
        this.fontSize = size;
    }

}

