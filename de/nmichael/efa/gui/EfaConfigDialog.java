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

import de.nmichael.efa.core.items.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.core.config.*;
import de.nmichael.efa.data.storage.IDataAccess;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

// @i18n complete
public class EfaConfigDialog extends BaseTabbedDialog {

    private EfaConfig myEfaConfig;

    public EfaConfigDialog(Frame parent, EfaConfig efaConfig) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this.myEfaConfig = efaConfig;
    }

    public EfaConfigDialog(JDialog parent, EfaConfig efaConfig) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this.myEfaConfig = efaConfig;
    }

    public EfaConfigDialog(JDialog parent, EfaConfig efaConfig, String selectedPanel) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this._selectedPanel = selectedPanel;
        this.myEfaConfig = efaConfig;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }
    
    protected void iniDialog() throws Exception {
        super.iniDialog();
        closeButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        closeButton.setIconTextGap(10);
    }
    public void closeButton_actionPerformed(ActionEvent e) {
        getValuesFromGui();
        synchronized (myEfaConfig) {
            for (int i = 0; i < allGuiItems.size(); i++) {
                IItemType item = allGuiItems.get(i);
                if (item.isChanged()) {
                    myEfaConfig.setValue(item.getName(), item.toString());
                }
            }
        }
        myEfaConfig.checkNewConfigValues();
        myEfaConfig.setExternalParameters(true);
        myEfaConfig.checkForRequiredPlugins();
        super.closeButton_actionPerformed(e);
        setDialogResult(true);
    }

    /*
     * The following methods will return the current working items (needed by ItemTypeAction to
     * generate new types), by first fetching the name of the item from the real EfaConfig, and
     * then find the current working item by this name.
     */
    public ItemTypeHashtable<String> getTypesBoat() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesBoat().getName());
    }

    public ItemTypeHashtable<String> getTypesNumSeats() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesNumSeats().getName());
    }

    public ItemTypeHashtable<String> getTypesRigging() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesRigging().getName());
    }

    public ItemTypeHashtable<String> getTypesCoxing() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesCoxing().getName());
    }

    public ItemTypeHashtable<String> getTypesGender() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesGender().getName());
    }

    public ItemTypeHashtable<String> getTypesSession() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesSession().getName());
    }

    public ItemTypeHashtable<String> getTypesStatus() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesStatus().getName());
    }

}
