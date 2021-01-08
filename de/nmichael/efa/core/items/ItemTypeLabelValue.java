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
import de.nmichael.efa.gui.BaseDialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete

public abstract class ItemTypeLabelValue extends ItemType {

    public static final int ACTIONID_FIELD_EXPANDED = 38341;
    public static final int ACTIONID_FIELD_COLLAPSED = 38342;

    protected JLabel label;
    protected int labelGridWidth = 1;
    protected int labelGridAnchor = GridBagConstraints.WEST;
    protected int labelGridFill = GridBagConstraints.NONE;
    protected Font labelFont;
    protected Font fieldFont;
    protected Color fieldColor = null;
    protected Color savedFieldColor = null;
    protected boolean isShowOptional = false;
    protected String optionalButtonText = "+";
    protected JButton expandButton;
    protected boolean itemOnNewRow = false;
    protected int xOffset = 0;
    protected int yOffset = 0;

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

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        this.dlg = dlg;
        iniDisplay();
        x += xOffset;
        y += yOffset;
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
    
}
