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

import de.nmichael.efa.gui.BaseFrame;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete

public class ItemTypeImage extends ItemType {

    private String value;
    private int maxX, maxY;

    JLabel image;
    JLabel label;
    JButton selectButton;
    JButton removeButton;

    public ItemTypeImage(String name, String value, int maxX, int maxY,
            int type, String category, String description) {
        this.name = name;
        this.value = value;
        this.maxX = maxX;
        this.maxY = maxY;
        this.type = type;
        this.category = category;
        this.description = description;
        this.lastValue = value;
    }

    public IItemType copyOf() {
        return new ItemTypeImage(name, value, maxX, maxY, type, category, description);
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

    protected void iniDisplay() {

        image = new JLabel();
        image.setBorder(BorderFactory.createEtchedBorder());
        image.setPreferredSize(new Dimension(maxX+10, maxY+10));
        image.setToolTipText(getDescription());
        image.setHorizontalAlignment(SwingConstants.CENTER);
        image.setHorizontalTextPosition(SwingConstants.CENTER);
        this.field = image;
        setImage(toString());
        label = new JLabel();
        Mnemonics.setLabel(dlg, label, getDescription() + ": ");
        label.setLabelFor(image);
        if (type == IItemType.TYPE_EXPERT) {
            label.setForeground(Color.red);
        }
        if (color != null) {
            label.setForeground(color);
        }
        selectButton = new JButton();
        selectButton.setIcon(BaseFrame.getIcon("menu_open.gif"));
        selectButton.setMargin(new Insets(0,0,0,0));
        Dialog.setPreferredSize(selectButton, 19, 19);
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { selectButtonHit(e); }
        });
        removeButton = new JButton();
        removeButton.setIcon(BaseFrame.getIcon("menu_minus.gif"));
        removeButton.setMargin(new Insets(0,0,0,0));
        Dialog.setPreferredSize(removeButton, 19, 19);
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { removeButtonHit(e); }
        });
        field = image;
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        this.dlg = dlg;
        iniDisplay();
        this.field = image;
        panel.add(label, new GridBagConstraints(x, y, 1, 2, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, padXbefore, 0, 0), 0, 0));
        panel.add(image, new GridBagConstraints(x+1, y, 1, 2, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 0, padYafter, 0), 0, 0));
        panel.add(selectButton, new GridBagConstraints(x+2, y+0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 0, 0, padXafter), 0, 0));
        panel.add(removeButton, new GridBagConstraints(x+2, y+1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, padXafter), 0, 0));
        return 2;
    }

    public void getValueFromGui() {
        // nothing to do (value always has current value!)
    }

    private void setImage(String filename) {
        value = "";
        if (field == null) {
            return;
        }
        if (filename == null || filename.length() == 0) {
            ((JLabel)field).setText(International.getMessage("max. {width} x {height} Pixel",maxX,maxY));
            ((JLabel)field).setIcon(null);
            return;
        }
        try {
            ((JLabel)field).setText("");
            ((JLabel)field).setIcon(new ImageIcon(filename));
            value = filename;
        } catch (Exception ee) {
            EfaUtil.foo();
        }
    }

    private void selectButtonHit(ActionEvent e) {
        String startDirectory = null;
        String selectedFile = null;

        if (value != null && value.length() > 0) {
            startDirectory = EfaUtil.getPathOfFile(value);
            selectedFile = EfaUtil.getNameOfFile(value);
        }

        String file = Dialog.dateiDialog(dlg,
                International.getMessage("{item} auswählen",
                getDescription()),
                International.getString("Bild-Datei")+" (*.gif, *.jpg, *.png)",
                "gif|jpg|png",
                startDirectory, selectedFile, null,
                false, false);
        if (file != null) {
            setImage(file);
        }

    }

    private void removeButtonHit(ActionEvent e) {
        if (value == null) {
            return;
        }
        if (Dialog.yesNoDialog(International.getString("Wirklich entfernen"),
                International.getString("Soll das Bild wirklich entfernt werden?")) == Dialog.YES) {
            setImage(null);
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String filename) {
        this.value = filename;
    }

    public String getValueFromField() {
        return getValue();
    }

    public void showValue() {
        try {
            ((JLabel)field).setText("");
            ((JLabel)field).setIcon(new ImageIcon(value));
        } catch (Exception ee) {
            EfaUtil.foo();
        }
    }

    public void requestFocus() {
        // nothing to do
    }
    
    public boolean isValidInput() {
        if (isNotNullSet()) {
            if (value == null || value.length() == 0) {
                return false;
            }
        }
        return true;
    }

    public void setVisible(boolean visible) {
        image.setVisible(visible);
        label.setVisible(visible);
        selectButton.setVisible(visible);
        removeButton.setVisible(visible);
        super.setVisible(visible);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        image.setForeground((enabled ? (new JLabel()).getForeground() : Color.gray));
        label.setForeground((enabled ? (new JLabel()).getForeground() : Color.gray));
        selectButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

}
