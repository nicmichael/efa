/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.items.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

// @i18n complete
public class SimpleInputDialog extends BaseDialog {

	private String KEYACTION_ENTER;
    protected IItemType[] items;

    SimpleInputDialog(Frame parent, String title, IItemType[] items) {
        super(parent, title, International.getStringWithMnemonic("OK"));
        this.items = items;
    }

    SimpleInputDialog(JDialog parent, String title, IItemType[] items) {
        super(parent, title, International.getStringWithMnemonic("OK"));
        this.items = items;
    }

    public void _keyAction(ActionEvent evt) {
        if (evt.getActionCommand().equals(KEYACTION_ENTER)) {
            closeButton_actionPerformed(evt);
        }
        super._keyAction(evt);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
   		//better than addKeyAction("ENTER") as the default button is handled earlier
    	//and provides better user experience
    	this.getRootPane().setDefaultButton(closeButton);

        // create GUI items
        mainPanel.setLayout(new GridBagLayout());

        int y = 0;
        for (IItemType item : items) {
            String s = item.getDescription();
            if (s.endsWith("?") || s.indexOf("\n") >= 0) {
                Vector<String> v = EfaUtil.split(s, '\n');
                for (int i = 0; i < v.size(); i++) {
                    ItemTypeLabel label = new ItemTypeLabel("LABEL" + i, IItemType.TYPE_PUBLIC, "", v.get(i));
                    y += label.displayOnGui(this, mainPanel, 0, y);
                }
                item.setDescription(null);
            }

            y += item.displayOnGui(this, mainPanel, 0, y);
            if (item instanceof ItemTypeString) {
                String val = ((ItemTypeString) item).getValue();
                if (val != null && val.length() > 0) {
                    ((ItemTypeString) item).setSelection(0, val.length());
                }
            }
        }
        items[0].requestFocus();

        if (closeButton != null) {
            closeButton.setIcon(getIcon(IMAGE_ACCEPT));
        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        for (IItemType item : items) {
            item.getValueFromGui();
            if (!item.isValidInput()) {
                String error = item.getInvalidErrorText();
                Dialog.error(error != null && error.length() > 0 ? 
                        "\n" + error : 
                        International.getMessage("Ungültige Eingabe im Feld '{field}'", item.getDescription()));
                item.requestFocus();
                return;
            }
        }
        setDialogResult(true);
        super.closeButton_actionPerformed(e);
    }

    public static boolean showInputDialog(JDialog parent, String title, IItemType[] items) {
        SimpleInputDialog dlg = new SimpleInputDialog(parent, title, items);
        dlg.showDialog();
        return dlg.resultSuccess;
    }

    public static boolean showInputDialog(JDialog parent, String title, IItemType item) {
        SimpleInputDialog dlg = new SimpleInputDialog(parent, title, new IItemType[] { item });
        dlg.showDialog();
        return dlg.resultSuccess;
    }

    public static boolean showInputDialog(JFrame parent, String title, IItemType item) {
        SimpleInputDialog dlg = new SimpleInputDialog(parent, title, new IItemType[] { item });
        dlg.showDialog();
        return dlg.resultSuccess;
    }

    public static boolean showInputDialog(JFrame parent, String title, IItemType[] items) {
        SimpleInputDialog dlg = new SimpleInputDialog(parent, title, items);
        dlg.showDialog();
        return dlg.resultSuccess;
    }    

    public static boolean showInputDialog(Window parent, String title, IItemType item) {
        if (parent instanceof JDialog) {
            return showInputDialog((JDialog)parent, title, item);
        } else {
            return showInputDialog((JFrame)parent, title, item);
        }
    }
    
}
