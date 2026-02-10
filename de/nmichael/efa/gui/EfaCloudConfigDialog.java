/*
 * <pre>
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael, Martin Glade
 * @version 2</pre>
 */
package de.nmichael.efa.gui;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.efacloud.TxRequestQueue;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.gui.util.EfaMenuButton;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Vector;

/**
 * A dialog to trigger efaCloud synchronisation activities.
 */
public class EfaCloudConfigDialog extends BaseTabbedDialog implements IItemListener {

	private static final String ACTIVATION_INFO = "ACTIVATION_INFO";
    private static final String BUTTON_EFACLOUD_ACTIVATE = "BUTTON_EFACLOUD_ACTIVATE";
    private static final String BUTTON_EFACLOUD_DEACTIVATE = "BUTTON_EFACLOUD_DEACTIVATE";
    private static final String BUTTON_EFACLOUD_START = "BUTTON_EFACLOUD_START";
    private static final String BUTTON_EFACLOUD_SYNCH_UPLOAD = "BUTTON_EFACLOUD_SYNCH_UPLOAD";
    private static final String BUTTON_EFACLOUD_PAUSE = "BUTTON_EFACLOUD_PAUSE";

    private AdminRecord admin;
    private final JDialog parent;
    private final TxRequestQueue txq;

    public EfaCloudConfigDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getStringWithMnemonic("efaCloud konfigurieren"), International.getStringWithMnemonic("Schließen"),
                null, true);
        this.parent = parent;
        this.txq = TxRequestQueue.getInstance();
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
     * @param admin the admin which provides the rights to use. It must have the "isAllowedAdministerProjectLogbook()"
     *              authorisation.
     */
    private void iniItems(AdminRecord admin) {
        this.admin = admin;
        Vector<IItemType> guiItems = new Vector<IItemType>();
        String category;
        IItemType item;
        int projectStorageType = (Daten.project == null) ? IDataAccess.TYPE_FILE_XML : Daten.project
                .getProjectStorageType();
        boolean isEfaRemote = projectStorageType == IDataAccess.TYPE_EFA_REMOTE;
        boolean isXML = projectStorageType == IDataAccess.TYPE_FILE_XML;
        boolean isEfaCloud = projectStorageType == IDataAccess.TYPE_EFA_CLOUD;

        String queueState = (txq == null) ? "UNDEFINED" : TxRequestQueue.QUEUE_STATE.get(txq.getState());
        String serverInfo = ((txq == null) || (txq.serverWelcomeMessage == null)
                || (txq.serverWelcomeMessage.isEmpty())) ? "" :
                "\nServer-Information:\n" + txq.serverWelcomeMessage + txq.adminSessionMessage;
        String advice = (isEfaRemote) ? International
                .getString("Für efaRemote kann der efaCloud-Modus nicht aktiviert werden.") : (isXML) ?
                International.getString("Aktuelles Projekt auf efaCloud umstellen.") + "\n" : International
                .getMessage("Server '{URL}'. Aktueller Status: {state}{serverinfo}",
                        Daten.project.getProjectRecord().getEfaCloudURL(), queueState, serverInfo) + "\n";
        category = "%01%" + International.getString("efaCloud Optionen");
        guiItems.add(item = new ItemTypeLabel(ACTIVATION_INFO, IItemType.TYPE_PUBLIC, category, advice));
        item.setPadding(0, 0, 20, 20);
        item.setFieldGrid(2, GridBagConstraints.NORTH, GridBagConstraints.NONE);

        if (!isEfaRemote) {
            if (!isEfaCloud) {
                // Add fields to enter the server and credentials to use
                ProjectRecord p = Daten.project.getProjectRecord();
                guiItems.add(item = new ItemTypeString(ProjectRecord.STORAGEUSERNAME, p.getStorageUsername(),
                        IItemType.TYPE_PUBLIC, category,
                        International.getString("Benutzername") + " (efaCloud Client)"));
                item.setPadding(0, 0, 0, 0);
                item.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
                item.setName("efaCloudUsername");
                item.setNotNull(true);
                guiItems.add(item = new ItemTypePassword(ProjectRecord.STORAGEPASSWORD, p.getStoragePassword(), true,
                        IItemType.TYPE_PUBLIC, category, International.getString("Paßwort") + " (efaCloud Client)"));
                item.setPadding(0, 0, 0, 0);
                item.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
                item.setName("efaCloudPassword");
                item.setNotNull(true);
                guiItems.add(
                        item = new ItemTypeString(ProjectRecord.EFACLOUDURL, p.getEfaCloudURL(), IItemType.TYPE_PUBLIC,
                                category, International.getString("URL des efaCloud Servers")));
                item.setPadding(0, 0, 0, 0);
                item.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
                item.setName("efaCloudURL");
                item.setNotNull(true);
                // Add activation change button.
                guiItems.add(item = new ItemTypeButton(BUTTON_EFACLOUD_ACTIVATE, IItemType.TYPE_PUBLIC, category,
                        International.getMessage("{item} aktivieren", Daten.EFA_CLOUD)));
                ((ItemTypeButton) item).setIcon(getIcon(IMAGE_EFACLOUD_ACTIVATE));
                item.setPadding(0, 0, 20, 20);
                item.registerItemListener(this);
                item.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
            }

            // Add state change tab, if efaCloud is active
            else if (txq != null) {
                int state = txq.getState();
                // If the queue is working options are pause, reset, and manual synch
                if ((state == TxRequestQueue.QUEUE_IS_WORKING) || (state == TxRequestQueue.QUEUE_IS_IDLE)) {
                    // Pause
                    guiItems.add(item = new ItemTypeButton(BUTTON_EFACLOUD_PAUSE, IItemType.TYPE_PUBLIC, category,
                            International.getString("Kommunikation anhalten")));
                    ((ItemTypeButton) item).setIcon(getIcon(IMAGE_EFACLOUD_PAUSE));
                    item.setPadding(0, 0, 20, 20);
                    item.registerItemListener(this);
                    item.setFieldGrid(2, GridBagConstraints.NORTH, GridBagConstraints.NONE);
                    // manual upload synchronization
                    guiItems.add(
                            item = new ItemTypeButton(BUTTON_EFACLOUD_SYNCH_UPLOAD, IItemType.TYPE_PUBLIC, category,
                                    International.getString("Upload Synchronisation starten")));
                    ((ItemTypeButton) item).setIcon(getIcon(IMAGE_EFACLOUD_SYNCH));
                    item.setPadding(0, 0, 20, 20);
                    item.registerItemListener(this);
                    item.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.NONE);
                    
                    guiItems.add(EfaGuiUtils.createHintWordWrap(BUTTON_EFACLOUD_SYNCH_UPLOAD+"HINT",IItemType.TYPE_PUBLIC, category, 
                    		International.getString("Werden mehrere Fahrtenbücher hintereinander in efaCloud hochgeladen, dann muss nach jedem Wechsel des Fahrtenbuchs ein Neustart von efa durchgeführt werden."),
                    				1,10,10,600));
                }
                // If the queue is paused the options are resume or deactivate
                else if (state == TxRequestQueue.QUEUE_IS_PAUSED) {
                    // Resume
                    guiItems.add(item = new ItemTypeButton(BUTTON_EFACLOUD_START, IItemType.TYPE_PUBLIC, category,
                            International.getString("Kommunikation aufnehmen")));
                    ((ItemTypeButton) item).setIcon(getIcon(IMAGE_EFACLOUD_START));
                    item.setPadding(0, 0, 20, 20);
                    item.registerItemListener(this);
                    item.setFieldGrid(2, GridBagConstraints.NORTH, GridBagConstraints.NONE);
                    // Deactivate
                    guiItems.add(item = new ItemTypeButton(BUTTON_EFACLOUD_DEACTIVATE, IItemType.TYPE_PUBLIC, category,
                            International.getMessage("{item} deaktivieren", Daten.EFA_CLOUD)));
                    ((ItemTypeButton) item).setIcon(getIcon(IMAGE_EFACLOUD_DEACTIVATE));
                    item.setPadding(0, 0, 20, 20);
                    item.registerItemListener(this);
                    item.setFieldGrid(2, GridBagConstraints.SOUTH, GridBagConstraints.NONE);
                }
                // If the queue is disconnected the options are deactivate
                else if (state == TxRequestQueue.QUEUE_IS_DISCONNECTED) {
                    // Deactivate
                    guiItems.add(item = new ItemTypeButton(BUTTON_EFACLOUD_DEACTIVATE, IItemType.TYPE_PUBLIC, category,
                            International.getMessage("{item} deaktivieren", Daten.EFA_CLOUD)));
                    ((ItemTypeButton) item).setIcon(getIcon(IMAGE_EFACLOUD_DEACTIVATE));
                    item.setPadding(0, 0, 20, 20);
                    item.registerItemListener(this);
                    item.setFieldGrid(2, GridBagConstraints.SOUTH, GridBagConstraints.NONE);
                }
                // if stuck either in synchronizing or in authenticating, pause and subsequent deactivation is needed
                // to be able fix the configuration offline.
                else if ((state == TxRequestQueue.QUEUE_IS_SYNCHRONIZING)
                        || (state == TxRequestQueue.QUEUE_IS_AUTHENTICATING)) {
                    // Pause
                    guiItems.add(item = new ItemTypeButton(BUTTON_EFACLOUD_PAUSE, IItemType.TYPE_PUBLIC, category,
                            International.getString("Kommunikation anhalten") + 
                                    " (" +  International.getString("Notfall") + ")"));
                    ((ItemTypeButton) item).setIcon(getIcon(IMAGE_EFACLOUD_PAUSE));
                    item.setPadding(0, 0, 20, 20);
                    item.registerItemListener(this);
                    item.setFieldGrid(2, GridBagConstraints.NORTH, GridBagConstraints.NONE);
                }
                // any other state. No options to control provided.
                else {
                    advice = International
                            .getMessage("Im aktuellen Status {state} gibt es keine efaCloud Option.", queueState);
                    guiItems.add(item = new ItemTypeLabel(ACTIVATION_INFO, IItemType.TYPE_PUBLIC, category, advice
                            + serverInfo));
                    item.setPadding(0, 0, 20, 20);
                    item.setFieldGrid(2, GridBagConstraints.NORTH, GridBagConstraints.NONE);
                }
            } else {
                advice = International
                        .getMessage("Im aktuellen Status {state} gibt es keine efaCloud Option.", queueState);
                guiItems.add(item = new ItemTypeLabel(ACTIVATION_INFO, IItemType.TYPE_PUBLIC, category, advice
                        + serverInfo));
                item.setPadding(0, 0, 20, 20);
                item.setFieldGrid(2, GridBagConstraints.NORTH, GridBagConstraints.NONE);
            }
        }
        super.setItems(guiItems);
    }

    /**
     * Deactivate the efaCloud feature. Will be called either from this dialog on itemListenerAction or when the
     * transaction queue hits an authentication error.
     */
    public static void deactivateEfacloud() {
        Project pr = Daten.project;
        pr.setProjectStorageType(IDataAccess.TYPE_FILE_XML);
        TxRequestQueue txq = TxRequestQueue.getInstance();
        if (txq != null)
            txq.cancel();
        // Store the changed settings
        String prjName = pr.getName();
        try {
            pr.close();
            Project.openProject(prjName, false);
        } catch (EfaException e) {
            de.nmichael.efa.util.Dialog
                    .error(LogString.fileSavingFailed(prjName, International.getString("Projekt"), e.toString()));
        }
        de.nmichael.efa.util.Dialog.infoDialog(International.getString("Bitte starte efa einmal neu."));
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
            if (Daten.project == null)
                return;
            Project pr = Daten.project;

            // if the current storage type is xml, this method is called to make it efaCloud
            // That will also trigger a rebuild of the server side tables.
            if (pr.getProjectStorageType() == IDataAccess.TYPE_FILE_XML) {
                String efaCloudURL = ((ItemTypeString) super.getItem("efaCloudURL")).getValue();
                String storageUsername = ((ItemTypeString) super.getItem("efaCloudUsername")).getValue();
                String storagePassword = ((ItemTypeString) super.getItem("efaCloudPassword")).getValue();
                if (itemType.getName().equalsIgnoreCase(BUTTON_EFACLOUD_ACTIVATE) && (efaCloudURL.length() > 7)
                    && (storageUsername.length() > 1) && (storagePassword.length() > 7)) {
                    // read the entered parameters into the project record.
                    pr.setProjectEfaCloudURL(efaCloudURL);
                    pr.setProjectStorageUsername(storageUsername);
                    pr.setProjectStoragePassword(storagePassword);
                    pr.setProjectStorageType(IDataAccess.TYPE_EFA_CLOUD);
                    // Store the changed settings
                    String prjName = pr.getName();
                    try {
                        // close the project
                        try {
							pr.closeAllStorageObjects();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Logger.logdebug(e);
						}
                        pr.close();
                        Daten.project=null;
                        // if an efaCloud configuration was mistyped, the user will deactivate efaCloud and
                        // reactivate it afterwards. Then the queue is running and must be closed to load the new
                        // confiuguration.
                        if (TxRequestQueue.getInstance() != null)
                            TxRequestQueue.getInstance().cancel();
                        // reopen the project.
                        Project.openProject(prjName, false);
                        pr = Daten.project;
                        if (parent instanceof EfaBaseFrame)
                            ((EfaBaseFrame) parent).openLogbook(pr.getCurrentLogbook());
                        // try to prove the provided login data
                        TxRequestQueue txq = TxRequestQueue.getInstance();
                        if (txq != null) {
                            String txqCheckCredentials = txq.checkCredentials();
                            if (! txqCheckCredentials.isEmpty()) {
                                String error = String.format("%s: %s",
                                        International.getString("Autorisierung am Server fehlgeschlagen"), txqCheckCredentials);
                                de.nmichael.efa.util.Dialog.error(error);
                                pr.setProjectStorageType(IDataAccess.TYPE_FILE_XML);
                                this.cancel();
                                return;
                            }
                            de.nmichael.efa.util.Dialog.infoDialog(International.getString("Konfiguration erfolgreich"),
                                    International.getString("efaCloud wurde erfolgreich aktiviert. Bitte starte efa nun neu."));
                            txq.clearAllQueues();
                            txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_AUTHENTICATE);
                            // Initialize the GUI for efaCloud Status display
                            txq.setEfaGUIrootContainer(this);
                            txq.showStatusAtGUI();
                        }
                    } catch (EfaException e) {
                        // in case of activation failure, set the project back to XML.
                        pr.setProjectStorageType(IDataAccess.TYPE_FILE_XML);
                        de.nmichael.efa.util.Dialog.error(LogString
                                .fileSavingFailed(prjName, International.getString("Projekt"), e.toString()));
                        return;
                    }
                }
                else if (itemType.getName().equalsIgnoreCase(BUTTON_EFACLOUD_ACTIVATE))
                    if (efaCloudURL.length() <= 7)
                        de.nmichael.efa.util.Dialog.infoDialog(International.getString("URL zu kurz"),
                            International.getString("efaCloud wurde nicht aktiviert. Bitte versuche es neu."));
                    else if (storageUsername.length() <= 1)
                        de.nmichael.efa.util.Dialog.infoDialog(International.getString("Benutzername zu kurz"),
                                International.getString("efaCloud wurde nicht aktiviert. Bitte versuche es neu."));
                    else if (storagePassword.length() <= 7)
                        de.nmichael.efa.util.Dialog.infoDialog(International.getString("Paßwort zu kurz"),
                                International.getString("efaCloud wurde nicht aktiviert. Bitte versuche es neu."));
            }

            // other efaCloud actions
            else if (pr.getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD) {
                if (itemType.getName().equalsIgnoreCase(BUTTON_EFACLOUD_DEACTIVATE)) {
                    deactivateEfacloud();
                } else if (itemType.getName().equalsIgnoreCase(BUTTON_EFACLOUD_START)) {
                    txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_START);
                } else if (itemType.getName().equalsIgnoreCase(BUTTON_EFACLOUD_PAUSE)) {
                    txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_PAUSE);
                } else if (itemType.getName().equalsIgnoreCase(BUTTON_EFACLOUD_SYNCH_UPLOAD)) {
                    txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_START_SYNCH_UPLOAD_ALL);
                }
            }
            this.cancel();    // This call ends the efaCloud dialog, it does not cancel any action.
        }
    }
}