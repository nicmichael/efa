/*
  Title:        efa - elektronisches Fahrtenbuch für Ruderer Copyright:    Copyright (c) 2001-2011
  by Nicolas Michael Website:      http://efa.nmichael.de/ License:      GNU General Public License
  v2

  @author Nicolas Michael, Martin Glade (efacloud adaptation)
 * @version 2
 */

package de.nmichael.efa.gui;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.efacloud.EfaCloudSynch;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.gui.util.EfaMenuButton;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Vector;

/**
 * A dialog to trigger efaCloud synchronisation activities.
 */
public class EfaCloudSynchDialog extends BaseTabbedDialog implements IItemListener {

    private static final String CONFIG_INFO = "CONFIG_INFO";
    private static final String DOWNLOAD_BUTTON = "DOWNLOAD_BUTTON";
    private static final String CONFIG_BUTTON = "CONFIG_BUTTON";

    private AdminRecord admin;
    private final JDialog parent;

    public EfaCloudSynchDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getStringWithMnemonic("EfaCloud"),
                International.getStringWithMnemonic("Schließen"), null, true);
        this.parent = parent;
        iniItems(admin);
    }

    /**
     * Forward the key action to the superclass BaseDialog for handling
     *
     * @param evt action which occurred.
     */
    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    /**
     * Initialize all items on theis dialog.
     *
     * @param admin the admin which provides the rights to use. It must have the
     *              "isAllowedAdministerProjectLogbook()" authorisation.
     */
    private void iniItems(AdminRecord admin) {
        this.admin = admin;
        Vector<IItemType> guiItems = new Vector<>();
        String category;
        IItemType item;
        int projectStorageType = (Daten.project == null) ? IDataAccess.TYPE_FILE_XML : Daten.project
                .getProjectStorageType();
        boolean isEfaRemote = projectStorageType == IDataAccess.TYPE_EFA_REMOTE;
        boolean isXML = projectStorageType == IDataAccess.TYPE_FILE_XML;
        boolean isEfaCloud = projectStorageType == IDataAccess.TYPE_EFA_CLOUD;

        // change storage type. If efaCloud is not yet enabled, this will be the only tab
        category = (isEfaCloud) ?
                "%01%" + International.getMessage("{item} deaktivieren", Daten.EFA_CLOUD) :
                "%01%" + International.getMessage("{item} aktivieren", Daten.EFA_CLOUD);

        String advice = (isEfaRemote) ?
                International.getString("Für efaRemote kann der efaCloud-Modus nicht aktiviert werden.") :
                (isXML) ?
                International.getString("Aktuelles Projekt nach efaCloud migrieren.") + "\n" +
                International.getString("Das aktuelle Projekt wird in einen vorbereiteten efaCloud Server hochgeladen.") + "\n" +
                International.getString("ACHTUNG: ALLE DATEN AUF DEM EFACLOUD SERVER WERDEN GELOESCHT UND NEU GESCHRIEBEN.") :
                International.getString("Deaktiviert den efaCloud Modus für das aktuelle Projekt.") + "\n" +
                International.getString("Alle Daten auf dem efaCloud Server und im lokalen Projekt bleiben erhalten.");
        guiItems.add(item = new ItemTypeLabel(CONFIG_INFO, IItemType.TYPE_PUBLIC, category, advice));
        item.setPadding(0, 0, 20, 20);
        item.registerItemListener(this);
        item.setFieldGrid(2, GridBagConstraints.NORTH, GridBagConstraints.NONE);

        if (!isEfaRemote) {
            if (!isEfaCloud) {
                // Add fields to enter the server and credentials to use
                ProjectRecord p = Daten.project.getProjectRecord();
                guiItems.add(item = new ItemTypeString(ProjectRecord.STORAGEUSERNAME,
                        p.getStorageUsername(), IItemType.TYPE_PUBLIC, category,
                        International.getString("Benutzername") + " (efaCloud Client)"));
                item.setPadding(0, 0, 0, 0);
                item.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
                item.setName("efaCloudUsername");
                item.setNotNull(true);
                guiItems.add(item = new ItemTypePassword(ProjectRecord.STORAGEPASSWORD,
                        p.getStoragePassword(), true, IItemType.TYPE_PUBLIC, category,
                        International.getString("Paßwort") + " (efaCloud Client)"));
                item.setPadding(0, 0, 0, 0);
                item.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
                item.setName("efaCloudPassword");
                item.setNotNull(true);
                guiItems.add(item = new ItemTypeString(ProjectRecord.EFACLOUDURL, p.getEfaCoudURL(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("URL des efaCloud Servers")));
                item.setPadding(0, 0, 0, 0);
                item.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
                item.setName("efaCloudURL");
                item.setNotNull(true);
            }
            // Add mode change button.
            guiItems.add(item = new ItemTypeButton(CONFIG_BUTTON, IItemType.TYPE_PUBLIC, category,
                    International
                            .getString((isXML) ?
                                    International.getMessage("{item} aktivieren", Daten.EFA_CLOUD) :
                                    International.getMessage("{item} deaktivieren", Daten.EFA_CLOUD))));
            ((ItemTypeButton) item)
                    .setIcon(getIcon((isXML) ? IMAGE_ACTIVATE_EC : IMAGE_DEACTIVATE_EC));
            item.setPadding(0, 0, 20, 20);
            item.registerItemListener(this);
            item.setFieldGrid(2, GridBagConstraints.SOUTH, GridBagConstraints.NONE);
            // Add download button.
            if (isEfaCloud) {
                guiItems.add(
                        item = new ItemTypeButton(DOWNLOAD_BUTTON, IItemType.TYPE_PUBLIC, category,
                                International.getString("Daten vom efaCloud Server laden")));
                ((ItemTypeButton) item).setIcon(getIcon(IMAGE_DOWNLOAD));
                item.setPadding(0, 0, 20, 20);
                item.registerItemListener(this);
                item.setFieldGrid(2, GridBagConstraints.SOUTH, GridBagConstraints.NONE);
            }
        }
        super.setItems(guiItems);
    }

    /**
     * The action triggered by the superclass on button pressed.
     *
     * @param itemType the item which triggered the event
     * @param event    the event triggered. Only action events lead to execution of up-/download.
     */
    public void itemListenerAction(IItemType itemType, AWTEvent event) {

        if (event instanceof ActionEvent) {
            // authorization check
            if (!admin.isAllowedAdministerProjectLogbook()) {
                EfaMenuButton.insufficientRights(admin, itemType.getDescription());
                return;
            }
            // Project record initialization check
            if (Daten.project == null) return;
            Project pr = Daten.project;

            // if the current storage type is xml, this method is called to make it efacloud
            // That will also trigger server uplaod
            if (pr.getProjectStorageType() == IDataAccess.TYPE_FILE_XML) {
                // read the entered parameters into the project record.
                pr.setProjectEfaCloudURL(
                        ((ItemTypeString) super.getItem("efaCloudURL")).getValue());
                pr.setProjectStorageUsername(
                        ((ItemTypeString) super.getItem("efaCloudUsername")).getValue());
                pr.setProjectStoragePassword(
                        ((ItemTypeString) super.getItem("efaCloudPassword")).getValue());
                pr.setProjectStorageType(IDataAccess.TYPE_EFA_CLOUD);
                // Store the changed settings
                String prjName = pr.getName();
                try {
                    pr.close();
                    Project.openProject(prjName, false);
                    pr = Daten.project;
                    if (parent instanceof EfaBaseFrame) {
                        ((EfaBaseFrame) parent).openLogbook(pr.getCurrentLogbook());
                    }
                } catch (EfaException e) {
                    de.nmichael.efa.util.Dialog.error(LogString.fileSavingFailed(prjName, International.getString("Projekt"), e.toString()));
                    return;
                }
                // Start the server upload.
                EfaCloudSynch.runEfaCloudSynchTask(this, EfaCloudSynch.Mode.upload, true, false, 0L,
                        false);
            }

            // disable efacloud
            else if (pr.getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD) {
                pr.setProjectStorageType(IDataAccess.TYPE_FILE_XML);
                // Store the changed settings
                String prjName = pr.getName();
                try {
                    pr.close();
                    Project.openProject(prjName, false);
                } catch (EfaException e) {
                    de.nmichael.efa.util.Dialog.error(LogString.fileSavingFailed(prjName, International.getString("Projekt"), e.toString()));
                    return;
                }
            }
            this.dispose();
        }
    }
}
