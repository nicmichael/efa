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
import de.nmichael.efa.core.config.Credentials;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.storage.IDataAccess;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Hashtable;
import javax.swing.*;

public class AdminLoginDialog extends BaseDialog {

    private static String NO_PROJECT;

    private String KEYACTION_ENTER;
    private String reason;
    private boolean showSelectProject;
    private ItemTypeString name;
    private ItemTypePassword password;
    private ItemTypeStringList projectList;
    private AdminRecord adminRecord;
    private String project;
    private static String selectedProject;

    public AdminLoginDialog(Frame parent, String reason) {
        super(parent, International.getStringWithMnemonic("Admin-Login"), International.getStringWithMnemonic("Login"));
        this.reason = reason;
    }

    public AdminLoginDialog(JDialog parent, String reason) {
        super(parent, International.getStringWithMnemonic("Admin-Login"), International.getStringWithMnemonic("Login"));
        this.reason = reason;
    }

    private void setShowSelectProject(String defaultProject) {
        this.showSelectProject = true;
        this.project = defaultProject;
        NO_PROJECT = "<" + International.getString("kein Projekt") + ">";
    }

    protected void iniDialog() throws Exception {
        KEYACTION_ENTER      = addKeyAction("ENTER");

        mainPanel.setLayout(new GridBagLayout());

        JLabel reasonLabel = new JLabel();
        reasonLabel.setText(reason);
        reasonLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel infoLabel = new JLabel();
        infoLabel.setText(International.getString("Admin-Login erforderlich."));
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        int width = (showSelectProject ? 200 : 120);

        if (reason != null && reason.length() > 0) {
            mainPanel.add(reasonLabel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
        }
        mainPanel.add(infoLabel, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0));

        name = new ItemTypeString("NAME", "", IItemType.TYPE_PUBLIC, "", International.getStringWithMnemonic("Admin-Name"));
        name.setToLowerCase(true);
        name.setAllowedCharacters("abcdefghijklmnopqrstuvwxyz0123456789_");
        name.setFieldSize(width, -1);
        password = new ItemTypePassword("PASSWORD", "", IItemType.TYPE_PUBLIC, "", International.getStringWithMnemonic("Paßwort"));
        password.setFieldSize(width, -1);
        
        name.displayOnGui(this, mainPanel, 0, 2);
        password.displayOnGui(this, mainPanel, 0, 3);

        if (showSelectProject) {
            Hashtable<String,String> projects = Project.getProjects();
            projects.put(NO_PROJECT, "foobar");
            if (projects != null && projects.size() > 0) {
                String[] projectArray = projects.keySet().toArray(new String[0]);
                Arrays.sort(projectArray, new EfaSortStringComparator());
                projectList = new ItemTypeStringList("PROJECT",
                        (project != null && project.length() > 0 ? project : NO_PROJECT),
                        projectArray, projectArray,
                        IItemType.TYPE_PUBLIC, "",
                        International.getString("Projekt"));
                projectList.setFieldSize(width, -1);
                projectList.displayOnGui(this, mainPanel, 0, 4);
            }

        }
        closeButton.setIcon(getIcon(IMAGE_ACCEPT));
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

    public void setLoginOnlyAdmin(String adminName) {
        name.parseAndShowValue(adminName);
        name.setEnabled(false);
        password.requestFocus();
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        name.getValueFromGui();
        password.getValueFromGui();
        if (projectList != null) {
            projectList.getValueFromGui();
        }

        if (!name.isValidInput()) {
            Dialog.error(International.getString("Kein Admin-Name eingegeben!"));
            name.requestFocus();
            return;
        }
        if (!password.isValidInput()) {
            Dialog.error(International.getString("Kein Paßwort eingegeben!"));
            password.requestFocus();
            return;
        }

        String adminName = name.getValue();
        String adminPassword = password.getValue();
        Admins myAdmins = Daten.admins;
        if (Daten.project != null &&
            Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE &&
            !adminName.equals(Admins.SUPERADMIN)) {
            myAdmins = new Admins(Daten.project.getProjectStorageType(),
                    Daten.project.getProjectStorageLocation(),
                    Daten.project.getProjectStorageUsername(),
                    Daten.project.getProjectStoragePassword());
        }
        
        if ((adminRecord = myAdmins.login(adminName, adminPassword)) == null) {
            Dialog.error(International.getString("Admin-Name oder Paßwort ungültig!"));
            Logger.log(Logger.WARNING, Logger.MSG_ADMIN_LOGINFAILURE, International.getString("Admin-Login") + ": "
                    + International.getMessage("Name {name} oder Paßwort ungültig!", adminName));
            password.parseAndShowValue("");
            password.requestFocus();
            adminRecord = null;
            return;
        }
        if (adminRecord != null) {
            Logger.log(Logger.INFO, Logger.MSG_ADMIN_LOGIN, International.getString("Admin-Login") + ": "
                    + International.getString("Name") + ": " + adminRecord.getName());
        } else {
            return;
        }
        super.closeButton_actionPerformed(e);
    }

    public AdminRecord getResult() {
        return adminRecord;
    }

    public String getSelectedProject() {
        if (projectList == null) {
            return null;
        }
        String p = projectList.getValue();
        return (NO_PROJECT.equals(p) ? null : p);
    }

    public static AdminRecord login(Window parent, String reason) {
        return login(parent, reason, null, false, null);
    }

    public static AdminRecord login(Window parent, String reason,
            boolean showSelectProject, String defaultProject) {
        return login(parent, reason, null,
                showSelectProject, defaultProject);
    }

    public static AdminRecord login(Window parent, String reason, String admin,
            boolean showSelectProject, String defaultProject) {

        AdminRecord adminRecord = null;
        try {
            Credentials cred = new Credentials();
            cred.readCredentials();
            if (cred.getDefaultAdmin() != null) {
                adminRecord = Daten.admins.login(cred.getDefaultAdmin(),
                        cred.getPassword(cred.getDefaultAdmin()));
            }
            if (adminRecord != null) {
                return adminRecord;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }

        AdminLoginDialog dlg = null;
        if (parent == null) {
            dlg = new AdminLoginDialog((JDialog)null, reason);
        } else {
            try {
                dlg = new AdminLoginDialog((JDialog) parent, reason);
            } catch (ClassCastException e) {
                dlg = new AdminLoginDialog((JFrame) parent, reason);
            }
        }
        return login(dlg, reason, admin, showSelectProject, defaultProject);
    }

    public static AdminRecord login(Frame parent, String grund, String admin,
            boolean showSelectProject, String defaultProject) {
        AdminLoginDialog dlg = null;
        if (parent != null) {
            dlg = new AdminLoginDialog(parent, grund);
        } else {
            dlg = new AdminLoginDialog((JFrame)null, grund);
        }
        return login(dlg, grund, admin, showSelectProject, defaultProject);
    }

    public static AdminRecord login(AdminLoginDialog dlg, String grund, String adminName,
            boolean showSelectProject, String defaultProject) {
        //dlg.setModal(true);
        if (adminName != null) {
            dlg.setLoginOnlyAdmin(adminName);
        }
        if (showSelectProject) {
            dlg.setShowSelectProject(defaultProject);
        }
        dlg.showDialog();
        selectedProject = (showSelectProject ? dlg.getSelectedProject() : null);
        return dlg.getResult();
    }

    public static String getLastSelectedProject() {
        return selectedProject;
    }




}
