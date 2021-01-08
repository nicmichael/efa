/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import de.nmichael.efa.util.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.BaseTabbedDialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

// @i18n complete
public class DataEditDialog extends BaseTabbedDialog {

    private JPanel northeastPanel;
    private int northeastPanelComponentCount = 0;

    public DataEditDialog(Frame parent, String title, Vector<IItemType> items) {
        super(parent, title, International.getStringWithMnemonic("Speichern"),
              items, false);
        initialize();
    }

    public DataEditDialog(JDialog parent, String title, Vector<IItemType> items) {
        super(parent, title, International.getStringWithMnemonic("Speichern"),
              items, false);
        initialize();
    }

    private void initialize() {
        northeastPanel = new JPanel();
        northeastPanel.setLayout(new FlowLayout());
        northeastPanel.setVisible(false);
        northeastPanel.getInsets().set(0, 0, 0, 0);
        super.dataNorthEastComponent = northeastPanel;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        super.iniDialog();
        if (closeButton != null) {
            closeButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        getValuesFromGui();
        setDialogResult(true);
        super.closeButton_actionPerformed(e);
    }

    public void addComponentToNortheastPanel(JComponent c) {
        northeastPanel.add(c);
        northeastPanelComponentCount++;
        northeastPanel.setVisible(true);
    }

    public void removeComponentFromNortheastPanel(JComponent c) {
        try {
            northeastPanel.remove(c);
            northeastPanel.setVisible(--northeastPanelComponentCount > 0);
        } catch(Exception eignore) {
            Logger.logdebug(eignore);
        }
    }
}
