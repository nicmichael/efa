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
import java.util.StringTokenizer;

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
        return new ItemTypeConfigButton(name, text, bcolor, show, isChangeableText, isChangeableColor, isChangeableShow, type, category, description);
    }

    public void parseValue(String value) {
        if (value == null) return;
        value = value.trim();
        try {
            StringTokenizer tok = new StringTokenizer(value, "|");
            int i = 0;
            while (tok.hasMoreTokens()) {
                String t = tok.nextToken();
                if (t.length() > 0) {
                    switch(i) {
                        case 0:
                            if (isChangeableText) {
                                text = t;
                            }
                            break;
                        case 1:
                            if (isChangeableColor) {
                                bcolor = t;
                            }
                            break;
                        case 2:
                            if (isChangeableShow) {
                                show = t.equals("+");
                            }
                            break;
                    }
                }
                i++;
            }
        } catch (Exception e) {
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
        Dialog.setPreferredSize(button, 300, 21);
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
            text = ((JButton)field).getText();
        }
        if (isChangeableColor) {
            bcolor = EfaUtil.getColor(field.getBackground());
        }
        if (isChangeableShow) {
            show = checkbox.isSelected();
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
        Color color = JColorChooser.showDialog(dlg,
                International.getMessage("{item} auswählen",
                International.getString("Farbe")),
                field.getBackground());
        if (color != null) {
            field.setBackground(color);
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
        label.setVisible(visible);
        field.setVisible(visible);
        if (checkbox != null) {
            checkbox.setVisible(visible);
        }
        super.setVisible(visible);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        label.setForeground((enabled ? (new JLabel()).getForeground() : Color.gray));
        field.setEnabled(enabled);
        if (checkbox != null) {
            checkbox.setEnabled(enabled);
        }
    }

}
