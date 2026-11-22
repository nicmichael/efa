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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.BaseTabbedDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

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
    
    /**
	 * Resize the dialog to the preferred size of the content. 
	 * This is needed when the visibility of some components has changed.
	 * But it needs to be triggered manually, 
	 * because the preferred size of the content is not automatically recalculated
	 */
    protected void resizeDialogToContent() {
        // Während des Konstruktors übernimmt prepareDialog() die Erstgröße
        if (!_prepared) {
            return;
        }
        // Neue Preferred Size des Inhalts ermitteln und die ScrollPane darauf abstimmen
        mainPanel.revalidate();
        Dimension dim = mainPanel.getPreferredSize();
        mainScrollPane.setPreferredSize(Dialog.getMaxSize(
                new Dimension(dim.width + 40, dim.height + 20)));
        EfaUtil.setMinimumSize(mainPanel);
        EfaUtil.pack(this);
    }
}
