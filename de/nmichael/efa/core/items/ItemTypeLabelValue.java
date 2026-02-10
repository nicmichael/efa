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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete


/*
 * An item which can be displayed on a screen.
 * Except for the efaBaseFrame dialogs which are used for session start, session finish, late entry,
 * all other records are edited by @see DataEditDialog and descendants.
 * 
 * How it works:
 * - the data record determines its fields in the database
 * 
 * - the data record provides info about the gui by which the data can be edited
 *   (getGuiItems). This methods returns all the information, including
 *   - GUI fields in their corresponding order (in which they shall appear on the screen)
 *   - categories of the GUI fields (which are rendered into tabs in the dialogs)
 *   - swing hints for the layout of the fields, which are stored in properties of ItemTypeLabelValue
 *   
 * - DataEditDialog (or descendant) 
 * 	 - calls getGuiItems from the configured DataRecord
 *   - determines all categories and builds tabs in the dialog, if there are more than one category over all gui items
 *   - puts each gui item in their corresponding order on the respective tab
 *     and this is done by calling ItemTypeLabelValue.displayOnGui() method. 
 *  
 */

public abstract class ItemTypeLabelValue extends ItemType {

	private static final int GUI_SEPARATOR_WIDTH = 30;
	public static final int ACTIONID_FIELD_EXPANDED = 38341;
    public static final int ACTIONID_FIELD_COLLAPSED = 38342;

    protected JLabel label;
    protected int labelGridWidth = 1; // Swing: how many columns shall the label use?
    protected int labelGridAnchor = GridBagConstraints.EAST; // alignment on right looks better than on the left (usability), but we still leave it on the left side
    protected int labelGridFill = GridBagConstraints.NONE; // no extension of the label to column width
    protected Font labelFont;
    protected Font fieldFont;
    protected Color fieldColor = null;
    protected Color savedFieldColor = null;
    protected boolean isShowOptional = false;
    protected String optionalButtonText = "+";
    protected JButton expandButton;
    protected boolean itemOnNewRow = false; // set to true, if label and field (e.g. multiline textfield) shall be in separate rows
    protected boolean itemOnSameRowAsPreviousItem = false; // set to true, if this field shall be on the same line as the previous field (allow two-column-layouts)   
    
    protected int xOffset = 0;
    protected int yOffset = 0;
    protected JLabel separator = null;
    
    protected abstract JComponent initializeField();

    protected void iniDisplay() {
        if (getDescription() != null) {
            label = new JLabel();
            Mnemonics.setLabel(dlg, label, getDescription() + ": ");
            label.setLabelFor(field);
            if (type == IItemType.TYPE_EXPERT) {
                label.setForeground(Color.red);
            }
            if (color != null) {
                label.setForeground(color);
            }
            labelFont = label.getFont();
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(MouseEvent e) { actionEvent(e); }
            });
        } else {
            labelGridWidth = 0;
        }
        field = initializeField();
        Dialog.setPreferredSize(field, fieldWidth, fieldHeight);
        if (fieldColor != null) {
            field.setForeground(fieldColor);
        }
        if (backgroundColor != null) {
            field.setBackground(backgroundColor);
        }
        fieldFont = field.getFont();
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(FocusEvent e) { field_focusGained(e); }
            public void focusLost(FocusEvent e) { field_focusLost(e); }
        });
        if (isShowOptional) {
            expandButton = new JButton();
            if (optionalButtonText.length() == 1) {
                Dialog.setPreferredSize(expandButton, 15, 15);
                expandButton.setFont(expandButton.getFont().deriveFont(Font.PLAIN, 8));
                expandButton.setMargin(new Insets(0, 0, 0, 0));
            } else {
                expandButton.setMargin(new Insets(0, 10, 0, 10));
            }
            expandButton.setText(optionalButtonText);
            expandButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) { expandButton_actionEvent(e); }
            });
        }
        showValue();
    }


    /*
     * displayOnGui shows the item on the panel. 
     * 
     * See class comment concerning infos about the general
     * procedures by which a dialog for DataRecords is built.
     * 
     * @parameter dlg - Window on which the item is put
     * @parameter panel - panel, on which the current item is put
     * @parameter x,y - x and y position of the grid (GridBagLayout) where this item is put.
     * 
     * @return y value of the next (free) line in the GridBagLayout after this item.
     */
    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        this.dlg = dlg;
        iniDisplay();
        //@see setOffsetXY to set these values.
        //add some offset to the target grid position given by the parameters
        x += xOffset;
        y += yOffset;
        
        /*
         * If we want to have a two column layout, we need to trick and take some assumptions.
         * Problem is
         *  - displayOnGui() does only know this one gui element, and does NOT know any other elements on the gui.
         *  	this method is called on each gui element.
         *  - displayOnGui() needs to return the NEXT (free) row (y position) in the gui after the current gui element
		 *
         * So, if you want a two column design for fields, you need to tell the item that shall go into the second column,  
         * that it's ACTUAL desired y position in the grid (row) is prior to the current y position given by parameter.
         * 
         * Assumptions:
         * - First, the general layout of the dialog is 
         *   label|field
         *   label|field
         * - each label|field item is a pair, label and field go in diffrent cells in the grid.
         * - usually x is zero for all fields in a line when a datarecord defines it's gui fields
         * - the position of the second column for the field is calculated automatically if itemOnSameRowAsPreviousItem is true
		 *
         * - if we have a two-colum line, the layout of the dialog is (for this line) 
         *   label|field|separator|label|field
         *   so the swing layout hints within DataRecord.getGuiItems needs to take care of this.
         *   
         * Calculations
         * - if the current item SHALL be in the second column (of the previous item)
         *   we add some small separator label (30px) with an empty text, so that gui layout is fine
         * - also, in the first column, the label is aligned left,
         *   in the second column, the label is aligned right (for beauty of layout and usability reasons)
         * 
         */
        if (itemOnSameRowAsPreviousItem) {
        	x+=2; // the second column begins after a label|field pair, so two cells to the right
        	y-=1; // second column to the previous item --> so position is the prior row

        	// add a small separator label of 30 pix width
        	separator = new JLabel();
        	separator.setText("  ");
        	Dialog.setPreferredSize(separator, GUI_SEPARATOR_WIDTH, fieldHeight);
        	panel.add(separator, new GridBagConstraints(x, y, 1, 1, 0.0, 0.0,
                    fieldGridAnchor, GridBagConstraints.NONE, 
                    new Insets((itemOnNewRow ? 0 : padYbefore), (itemOnNewRow ? padXbefore : 0), padYafter, padXafter), 0, 0));

        	x+=1;// position of the next label|field pair is after the separator
        }        
        // the following code is unchanged to prior releases 
        
        if (label != null) {
            panel.add(label, new GridBagConstraints(x, y, labelGridWidth, fieldGridHeight, 0.0, 0.0,
                    labelGridAnchor, labelGridFill, 
                    new Insets(padYbefore, padXbefore, (itemOnNewRow ? 0 : padYafter), (itemOnNewRow ? padXafter : 0)), 0, 0));
        }
        if (expandButton != null) {
            int gridWidth = labelGridWidth + (optionalButtonText.length() > 1 ? fieldGridWidth : 0);
            panel.add(expandButton, new GridBagConstraints(x, y, gridWidth, fieldGridHeight, 0.0, 0.0,
                    labelGridAnchor, labelGridFill, new Insets(padYbefore, padXbefore, padYafter, 0), 0, 0));
        }

        if (itemOnNewRow) {
            y++;
        } else {
            x += labelGridWidth;
        }
        panel.add(field, new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, 0.0, 0.0,
                fieldGridAnchor, fieldGridFill, 
                new Insets((itemOnNewRow ? 0 : padYbefore), (itemOnNewRow ? padXbefore : 0), padYafter, padXafter), 0, 0));
        if (!isEnabled) {
            setEnabled(isEnabled);
        }
        
        return (itemOnNewRow ? 2 : 1);
    }

    public void getValueFromGui() {
        if (field != null) {
            String s = getValueFromField();
            if (s != null) {
                parseValue(s);
            }
        }
    }

    protected void field_focusLost(FocusEvent e) {
        getValueFromGui();
        showValue();
        super.field_focusLost(e);
    }

    public void setLabelGrid(int gridWidth, int gridAnchor, int gridFill) {
        if (gridWidth != -1) {
            labelGridWidth = gridWidth;
        }
        if (gridAnchor != -1) {
            labelGridAnchor = gridAnchor;
        }
        if (gridFill != -1) {
            labelGridFill = gridFill;
        }
    }

    public Font getLabelFont() {
        return (label != null ? label.getFont() : null);
    }

    public void setLabelFont(Font font) {
        if (label != null) {
            label.setFont(font);
        }
    }

    public void restoreLabelFont() {
        if (label != null) {
            label.setFont(labelFont);
        }
    }

    public void setDescription(String s) {
        super.setDescription(s);
        Mnemonics.setLabel(dlg, label, getDescription() + ": ");
    }

    public Font getFieldFont() {
        return field.getFont();
    }

    public void setFieldFont(Font font) {
        field.setFont(font);
    }

    public void restoreFieldFont() {
        field.setFont(fieldFont);
    }

    public void setFieldColor(Color c) {
        this.fieldColor = c;
        if (field != null) {
            field.setForeground(c);
        }
    }

    public void saveFieldColor(boolean force) {
        if (field != null && (savedFieldColor == null || force)) {
            savedFieldColor = field.getForeground();
        }
    }

    public void restoreFieldColor() {
        if (field != null && savedFieldColor != null) {
            field.setForeground(savedFieldColor);
        }
    }

    private boolean showExpandButton(boolean isExpandButtonHit, boolean calledForExpandButton) {
        if (isShowOptional && toString().length() == 0 && !isExpandButtonHit) {
            if (isShowOptional) {
                actionEvent(new ActionEvent(this, ACTIONID_FIELD_COLLAPSED, "collapsed"));
            }
            return (calledForExpandButton); // show expandButton
        } else {
            if (isShowOptional) {
                actionEvent(new ActionEvent(this, ACTIONID_FIELD_EXPANDED, "expanded"));
            }
            return (!calledForExpandButton); // show label
        }
    }

    public void showValue() {
        setVisibleInternal(false);
    }

    private void setVisibleInternal(boolean isExpandButtonHit) {
        if (label != null) {
            label.setVisible(isVisible && showExpandButton(isExpandButtonHit, false));
        }
        if (expandButton != null) {
            expandButton.setVisible(isVisible && showExpandButton(isExpandButtonHit, true));
        }
        if (field != null) {
            field.setVisible(isVisible && showExpandButton(isExpandButtonHit, false));
        }
    }
    
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        setVisibleInternal(false);
        
        // if we have a separator due to setIsItemOnSameRowAsPreviousItem, it's visibility shall be the same
        if (separator != null) {
        	separator.setVisible(visible);
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (label != null) {
            label.setForeground((enabled ? (new JLabel()).getForeground() : Color.gray));
        }
        if (expandButton != null) {
            expandButton.setEnabled(enabled);
        }
        if (field != null) {
            field.setEnabled(enabled);
        }
    }
    
    public void setIsItemOnSameRowAsPreviousItem(boolean sameRow) {
    	itemOnSameRowAsPreviousItem=sameRow;
    }

    public void showOptional(boolean optional) {
        isShowOptional = optional;
    }

    public void setOptionalButtonText(String text) {
        this.optionalButtonText = text;
    }

    private void expandButton_actionEvent(ActionEvent e) {
        expandToField();
    }

    public void expandToField() {
        setVisibleInternal(true);
        if (field != null) {
            field.requestFocus();
        }
    }

    public void setItemOnNewRow(boolean newRow) {
        itemOnNewRow = newRow;
    }
    
    public void setOffsetXY(int x, int y) {
        this.xOffset = x;
        this.yOffset = y;
    }
    
    public void setIcon(Icon iValue) {
    	if (label!=null) {
	    	label.setIcon(iValue);
	    	label.setIconTextGap(1);
	    	label.setHorizontalTextPosition(SwingConstants.LEFT);
    	}
    }
}
