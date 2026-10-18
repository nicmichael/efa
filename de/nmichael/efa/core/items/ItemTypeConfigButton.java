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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;

// @i18n complete

/**
 * This class represents a button that can be configured in the configuration dialog.
 * 
 * The button can have a text, a background color and a visibility state.
 * The text, color and visibility can be changeable or not.
 * The button can be displayed in the configuration dialog with a label and a checkbox for visibility.
 */
public class ItemTypeConfigButton extends ItemType {

    private String text;
    private String bcolor;
    private boolean show;
    private boolean isChangeableText;
    private boolean isChangeableColor;
    private boolean isChangeableShow;

    protected JLabel label;
    protected JCheckBox checkbox;


    public ItemTypeConfigButton(String name, String text, String bcolor, boolean show,
            boolean isChangeableText, boolean isChangeableColor, boolean isChangeableShow,
            int type, String category, String description) {
        this.name = name;
        this.text = text;
        this.bcolor = bcolor;
        this.show = show;
        this.isChangeableText = isChangeableText;
        this.isChangeableColor = isChangeableColor;
        this.isChangeableShow = isChangeableShow;
        this.type = type;
        this.category = category;
        this.description = description;
    }

    public IItemType copyOf() {
        ItemTypeConfigButton copy = new ItemTypeConfigButton(name, text, bcolor, show, isChangeableText, isChangeableColor, isChangeableShow, 
        		type, category, description);
        copy.setFieldSize(fieldWidth,  fieldHeight);
        copy.setIndent(this.isIndent());
        copy.setVisible(this.isVisible());
        copy.setFieldGrid(fieldGridWidth, fieldGridHeight, fieldGridAnchor, fieldGridFill);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        return copy;
    }

    /**
     * Parses a serialized value string in the format "text|color|show".
     *
     * Fields are separated by '|'. Empty fields are preserved (split with limit -1),
     * so the position of each field always stays the same. A field is only applied
     * if the corresponding "changeable" flag is set. Unchangeable fields are written
     * as "#" by toString() and are therefore ignored here.
     *
     * @param value the serialized value, or null (then nothing is changed)
     */
    public void parseValue(String value) {
        // Nothing to parse
        if (value == null) {
            return;
        }

        try {
            // Remove surrounding whitespace and split into fields.
            // A limit of -1 keeps trailing and empty fields, so positions do not shift.
            String[] parts = value.trim().split("\\|", -1);

            // Field 0: button text
            if (parts.length > 0 && isChangeableText) {
                text = parts[0];
            }

            // Field 1: background color
            if (parts.length > 1 && isChangeableColor) {
                bcolor = parts[1];
            }

            // Field 2: visibility ("+" = visible, anything else = hidden)
            if (parts.length > 2 && isChangeableShow) {
                show = parts[2].equals("+");
            }
        } catch (Exception e) {
            // Log unexpected parsing errors instead of failing silently
            Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
                    "Invalid value for parameter " + name + ": " + value);
        }
    }

    public String toString() {
        return  (isChangeableText ? EfaUtil.removeSepFromString(text, "|") : "#") +
                "|" +
                (isChangeableColor ? bcolor : "#") +
                "|" +
                (isChangeableShow ? (show ? "+" : "-") : "#");
    }

    protected void iniDisplay() {
        JButton button = new JButton();
        Dialog.setPreferredSize(button, fieldWidth, 21);
        button.setText(text);
        button.setBackground(EfaUtil.getColorOrGray(bcolor));
        button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { buttonHit(e); }
        });
        
        EfaUtil.handleButtonOpaqueForLookAndFeels(button);        
        
        if (isChangeableShow) {
            checkbox = new JCheckBox();
            checkbox.setText(International.getString("anzeigen"));
            checkbox.setSelected(show);
        }
        label = new JLabel();
        Mnemonics.setLabel(dlg, label, getDescription() + ": ");
        label.setLabelFor(button);
        label.setHorizontalTextPosition(SwingConstants.RIGHT);
        if (type == IItemType.TYPE_EXPERT) {
            label.setForeground(Color.red);
        }
        if (color != null) {
            label.setForeground(color);
        }
        this.field = button;
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        this.dlg = dlg;
        iniDisplay();
        /* we ignore the x parameter and the indent property , because we always want to display the label in column 0, 
         * the field in column 1 and the checkbox in column 2 */
        panel.add(label, new GridBagConstraints(0, y, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(padYbefore, padXbefore, padYafter, 0), 0, 0));
        panel.add(field, new GridBagConstraints(1, y, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 0, padYafter, (checkbox == null ? padXafter : 0)), 0, 0));
        if (checkbox != null) {
            panel.add(checkbox, new GridBagConstraints(2, y, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 0, padYafter, padXafter), 0, 0));
        }
        return 1;
    }

    public void getValueFromGui() {
        if (isChangeableText) {
        	if (field != null) {
        		text = ((JButton)field).getText();
			}
        }
        if (isChangeableColor) {
        	if (field != null) {
        		bcolor = EfaUtil.getColor(field.getBackground());
        	}
        }
        if (isChangeableShow) {
            if (checkbox != null) {
            	show = checkbox.isSelected();
            }
        }
    }

    public String getValueFromField() {
        return null;
    }

    public void showValue() {
    }

    private void buttonHit(ActionEvent e) {
        if (!isChangeableText && !isChangeableColor) return;
        if (!isChangeableColor) {
            chooseText();
            return;
        }
        if (!isChangeableText) {
            chooseColor();
            return;
        }
        switch(Dialog.auswahlDialog(International.getString("Auswahl"),
                International.getString("Was möchtest Du ändern?"),
                International.getString("Text"),
                International.getString("Farbe"))) {
            case 0:
                chooseText();
                break;
            case 1:
                chooseColor();
                break;
        }
    }

    private void chooseText() {
        if (!isChangeableText) return;
        String s = Dialog.inputDialog(getDescription(),
                International.getString("Text") + ":",
                ((JButton)field).getText());
        if (s != null) {
            ((JButton)field).setText(s.trim());
        }
    }

    private void chooseColor() {
        if (!isChangeableColor) return;
        Color tmpColor = JColorChooser.showDialog(dlg,
                International.getMessage("{item} auswählen",
                International.getString("Farbe")),
                field.getBackground());
        if (tmpColor != null) {
            field.setBackground(tmpColor);
        }

    }

    public String getValueText() {
        return text;
    }

    public String getValueColor() {
        return bcolor;
    }

    public boolean getValueShow() {
        return show;
    }

    public void setValueText(String text) {
        this.text = text;
    }

    public void setValueColor(String color) {
        this.bcolor = color;
    }

    public void setValueShow(boolean show) {
        this.show = show;
    }

    public boolean isValidInput() {
        return true;
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
    	if (label != null) {
			label.setVisible(visible);
		}
    	if (field != null) {
    		field.setVisible(visible);
    	}
        if (checkbox != null) {
            checkbox.setVisible(visible);
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (label != null) {
            label.setForeground((enabled ? (new JLabel()).getForeground() : Color.gray));
		}
        if (field != null) {
            field.setEnabled(enabled);
		}

        if (checkbox != null) {
            checkbox.setEnabled(enabled);
        }
    }
    
    @Override 
    public void setFieldSize(int width, int height) {
    	super.setFieldSize(width, height);
    	if (field!=null) {
            Dialog.setPreferredSize(field, fieldWidth, 21);
    	}
    }

}
