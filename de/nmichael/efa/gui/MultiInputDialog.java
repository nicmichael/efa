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


// @i18n complete
public class MultiInputDialog extends BaseDialog {

	private String KEYACTION_ENTER;
    protected IItemType[] items;

    public MultiInputDialog(Frame parent, String title, IItemType[] items) {
        super(parent, title, International.getStringWithMnemonic("OK"));
        this.items = items;
    }

    public MultiInputDialog(JDialog parent, String title, IItemType[] items) {
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
        KEYACTION_ENTER = addKeyAction("ENTER");

        // create GUI items
        mainPanel.setLayout(new GridBagLayout());

        int y=0;
        for (int i=0; i<items.length; i++) {
            y += items[i].displayOnGui(this, mainPanel, 0, y);
        }
        this.setRequestFocus(items[0]);
        items[0].requestFocus();

        if (closeButton != null) {
            closeButton.setIcon(getIcon(IMAGE_ACCEPT));
        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        for (int i = 0; i < items.length; i++) {
            items[i].getValueFromGui();
            if (!items[i].isValidInput()) {
                Dialog.error(International.getMessage("Ungültige Eingabe im Feld '{field}'", items[i].getDescription()));
                items[i].requestFocus();
                return;
            }
        }
        setDialogResult(true);
        super.closeButton_actionPerformed(e);
    }

    public static boolean showInputDialog(JDialog parent, String title, IItemType[] items) {
        MultiInputDialog dlg = new MultiInputDialog(parent, title, items);
        dlg.showDialog();
        return dlg.resultSuccess;
    }

    public static boolean showInputDialog(JFrame parent, String title, IItemType[] items) {
        MultiInputDialog dlg = new MultiInputDialog(parent, title, items);
        dlg.showDialog();
        return dlg.resultSuccess;
    }

    public static boolean showInputDialog(Window parent, String title, IItemType[] items) {
        if (parent instanceof JDialog) {
            return showInputDialog((JDialog)parent, title, items);
        } else {
            return showInputDialog((JFrame)parent, title, items);
        }
    }

}
