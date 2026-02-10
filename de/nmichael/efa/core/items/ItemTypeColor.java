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

import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete

/*
 * Label   [Button for choosing color] [button for color reset]
 * 
 * Label: Description of the item
 * Button for choosing color: Static text "Choose color"
 *    invokes a JColorChooser
 *    button color is the color that the user chose in JColorChooser
 * Button for color reset
 *    resets the button color to empty.
 *    
 * canBeNull
 *    Can the color be null? if yes, button for color reset sets the button color to null.
 *    Otherwise, the button color is set to defaultColor.
 * 	  
 * 
 */
public class ItemTypeColor extends ItemTypeLabelValue {

    private Color origButtonColor;
    private JButton butdel;
    
    private String color;
    private String defaultColor;
    private Boolean canBeNull;

    public ItemTypeColor(String name, String color, String defaultColor,
            int type, String category, String description, Boolean canBeNull) {
        this.name = name;
        this.color = color;
        this.defaultColor = defaultColor;
        this.type = type;
        this.category = category;
        this.description = description;
        this.canBeNull = canBeNull;
    }

    public IItemType copyOf() {
         ItemTypeColor copy = new ItemTypeColor(name, color, defaultColor, type, category, description, canBeNull);
         copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
         copy.setIcon((label == null ? null : label.getIcon()));
         return copy;
    }

    public void parseValue(String value) {
        this.color = value;
    }

    public String toString() {
        return color;
    }

    protected JComponent initializeField() {
        JButton f = new JButton();

        EfaUtil.handleButtonOpaqueForLookAndFeels(f);

        return f;
    }
    protected void iniDisplay() {
        super.iniDisplay();
        JButton f = (JButton)field;
        origButtonColor = f.getBackground();
        f.setEnabled(isEnabled);
        Dialog.setPreferredSize(f, fieldWidth, fieldHeight);
        f.setText(International.getMessage("{item} auswählen",
                International.getString("Farbe")));
        f.setBackground(EfaUtil.getColorOrGray(color));
        f.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { buttonHit(e); }
        });

        butdel = new JButton();
        butdel.setEnabled(isEnabled);
        Dialog.setPreferredSize(butdel, fieldHeight, fieldHeight);
        butdel.setIcon(BaseDialog.getIcon(BaseDialog.IMAGE_DELETE));
        butdel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { buttonDel(e); }
        });
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        int count = super.displayOnGui(dlg, panel, x, y);
        panel.add(butdel, new GridBagConstraints(x+fieldGridWidth+1, y, 1, fieldGridHeight, 0.0, 0.0,
                fieldGridAnchor, fieldGridFill,
                new Insets((itemOnNewRow ? 0 : padYbefore), (itemOnNewRow ? padXbefore : 0), padYafter, padXafter), 0, 0));
        return count;
    }

    public void getValueFromGui() {
        if (field != null) {
            color = EfaUtil.getColor(field.getBackground());
        }
    }

    public Color getColor() {
        return EfaUtil.getColor(color);
    }

    public String getValueFromField() {
        if (field != null) {
            return EfaUtil.getColor(field.getBackground());
        }
        return color;
    }

    public void showValue() {
    }

    private void buttonHit(ActionEvent e) {
        Color color = JColorChooser.showDialog(dlg,
                International.getMessage("{item} auswählen",
                International.getString("Farbe")),
                field.getBackground());
        if (color != null) {
            field.setBackground(color);
        }
    }

    private void buttonDel(ActionEvent e) {
        if (canBeNull) {
        	this.color = null;
        	field.setBackground(origButtonColor);
        } else {
        	this.color = defaultColor;
        	field.setBackground(EfaUtil.getColor(defaultColor));
        }
    }

    public boolean isValidInput() {
        return true;
    }
    
}
