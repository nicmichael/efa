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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.Admins;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.storage.IDataAccess;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class AdminPasswordChangeDialog extends BaseDialog {

    private String KEYACTION_ENTER;
    private ItemTypeString name;
    private ItemTypePassword passwordOld;
    private ItemTypePassword passwordNew1;
    private ItemTypePassword passwordNew2;
    private AdminRecord adminRecord;
    private Admins myAdmins;
    private boolean mustEnterOldPass = true;

    public AdminPasswordChangeDialog(Frame parent, AdminRecord adminRecord) {
        super(parent, International.getStringWithMnemonic("Paßwort ändern"), International.getStringWithMnemonic("Paßwort ändern"));
        iniAdminDataAccess(adminRecord);
    }

    public AdminPasswordChangeDialog(JDialog parent, AdminRecord adminRecord) {
        super(parent, International.getStringWithMnemonic("Paßwort ändern"), International.getStringWithMnemonic("Paßwort ändern"));
        iniAdminDataAccess(adminRecord);
    }

    public AdminPasswordChangeDialog(JDialog parent, AdminRecord adminRecord, boolean mustEnterOldPass) {
        super(parent, International.getStringWithMnemonic("Paßwort ändern"), International.getStringWithMnemonic("Paßwort ändern"));
        iniAdminDataAccess(adminRecord);
        this.mustEnterOldPass = mustEnterOldPass;
    }

    private void iniAdminDataAccess(AdminRecord admin) {
        this.adminRecord = admin;
        myAdmins = Daten.admins;
        if (adminRecord.isRemoteAdminRecord()
                && Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
            myAdmins = new Admins(Daten.project.getProjectStorageType(),
                    Daten.project.getProjectStorageLocation(),
                    Daten.project.getProjectStorageUsername(),
                    Daten.project.getProjectStoragePassword());
        }
    }

    protected void iniDialog() throws Exception {
        KEYACTION_ENTER      = addKeyAction("ENTER");

        mainPanel.setLayout(new GridBagLayout());

        name = new ItemTypeString("NAME", adminRecord.getName(), IItemType.TYPE_PUBLIC, "", International.getStringWithMnemonic("Admin-Name"));
        name.setFieldSize(120, 20);
        name.setEditable(false);
        name.displayOnGui(this, mainPanel, 0, 0);

        if (mustEnterOldPass) {
            passwordOld = new ItemTypePassword("PASSWORD_OLD", "",  IItemType.TYPE_PUBLIC, "", International.getStringWithMnemonic("Altes Paßwort"));
            passwordOld.setFieldSize(120, 20);
            passwordOld.setNotNull(true);
            passwordOld.displayOnGui(this, mainPanel, 0, 1);
        }

        passwordNew1 = new ItemTypePassword("PASSWORD_NEW1", "",  IItemType.TYPE_PUBLIC, "", International.getStringWithMnemonic("Neues Paßwort"));
        passwordNew1.setFieldSize(120, 20);
        passwordNew1.setNotNull(true);
        passwordNew1.setMinCharacters(6);
        passwordNew1.displayOnGui(this, mainPanel, 0, 2);

        passwordNew2 = new ItemTypePassword("PASSWORD_NEW2", "",  IItemType.TYPE_PUBLIC, "", International.getStringWithMnemonic("Neues Paßwort")+
                " (" + International.getString("Wiederholung") + ")");
        passwordNew2.setFieldSize(120, 20);
        passwordNew2.setNotNull(true);
        passwordNew2.setMinCharacters(6);
        passwordNew2.displayOnGui(this, mainPanel, 0, 3);

        closeButton.setIcon(getIcon(IMAGE_ACCEPT));
        closeButton.setIconTextGap(10);
        setRequestFocus((passwordOld != null ? passwordOld : passwordNew1));
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void _keyAction(ActionEvent evt) {
        if (evt.getActionCommand().equals(KEYACTION_ENTER)) {
            closeButton_actionPerformed(evt);
        }
        super._keyAction(evt);
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        if (passwordOld != null) {
            passwordOld.getValueFromGui();
        }
        passwordNew1.getValueFromGui();
        passwordNew2.getValueFromGui();
        if (passwordOld != null && !passwordOld.isValidInput()) {
            Dialog.error(passwordOld.getInvalidErrorText());
            passwordOld.requestFocus();
            return;
        }
        if (!passwordNew1.isValidInput()) {
            Dialog.error(passwordNew1.getInvalidErrorText());
            passwordNew1.requestFocus();
            return;
        }
        if (!passwordNew2.isValidInput()) {
            Dialog.error(passwordNew2.getInvalidErrorText());
            passwordNew2.requestFocus();
            return;
        }
        if (passwordOld != null && (myAdmins.login(adminRecord.getName(), passwordOld.getValue())) == null) {
            Dialog.error(International.getString("Altes Paßwort ungültig!"));
            return;
        }
        if (!passwordNew1.getValue().equals(passwordNew2.getValue())) {
            Dialog.error(International.getMessage("Paßwort in Feld '{field}' nicht identisch.", passwordNew2.getDescription()));
            return;
        }
        adminRecord.setPassword(passwordNew1.getValue());
        try {
            myAdmins.data().update(adminRecord);
            Dialog.infoDialog(International.getString("Das Paßwort wurde erfolgreich geändert."));
        } catch(Exception ee) {
            Dialog.error(ee.toString());
        }
        super.closeButton_actionPerformed(e);
    }


}
