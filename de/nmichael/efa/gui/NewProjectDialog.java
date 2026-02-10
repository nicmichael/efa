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
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.*;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaConfig;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class NewProjectDialog extends StepwiseDialog implements IItemListener {

	private static final long serialVersionUID = 1601307709992220701L;
	private final static String GUIITEM_CREATE_WATERS_LIST = "GUIITEM_CREATE_WATERS_LIST";
    private final static String GUIITEM_NODATA_LABEL1 = "GUIITEM_NODATA_LABEL1";
    private final static String GUIITEM_NODATA_LABEL2 = "GUIITEM_NODATA_LABEL2";
    private final static String GUIITEM_CREATE_STATUS_LIST = "GUIITEM_CREATE_PERSON_STATUS_LIST";
    private final static String GUIITEM_CREATE_STATUS_LIST_HINT = "GUIITEM_CREATE_PERSON_STATUS_LIST_HINT";
    
    private final static String CATEGORY_STEP_0 = "0";
    private final static String CATEGORY_STEP_1 = "1";
    private final static String CATEGORY_STEP_2 = "2";
    private final static String CATEGORY_STEP_3 = "3";
    private final static String CATEGORY_STEP_4 = "4";
    

    private AdminRecord admin;

    public NewProjectDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Neues Projekt"));
        this.admin = admin;
    }

    public NewProjectDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Neues Projekt"));
        this.admin = admin;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    String[] getSteps() {
        return new String[]{
                    International.getString("Name und Beschreibung"),
                    International.getString("Speichertyp auswählen"),
                    International.getString("Speicherort festlegen"),
                    International.getString("Angaben zum Verein"),
                    International.getString("Verbände")
                };
    }
    
    String getDescription(int step) {
        switch(step) {
            case 0:
                return International.getString("In efa2 werden alle Daten in Projekten zusammengefaßt. Üblicherweise solltest Du für einen Verein "+
                        "genau ein Projekt erstellen, welches dann sämtliche Fahrtenbücher, Mitglieder-, Boots- und Ziellisten sowie sonstige Daten enthält.");
            case 1:
                return International.getString("Bitte wähle, wo die Daten des Projekts gespeichert werden sollen") + ":\n"+
                        "  "+International.getString("lokales Dateisystem") + " - " +
                             International.getString("speichert die Daten lokal auf Deinem Computer")+"\n"+
                        "  "+Daten.EFA_REMOTE + " - " +
                             International.getString("greift auf Daten in einem entfernt laufenden efa zu")+"\n"+
                        "  "+ Daten.EFA_CLOUD + " - " +
                             International.getString("speichert die Daten online in einem efaCloud Server");
            case 2:
                return International.getString("Bitte gib an, wo die Daten gespeichert werden sollen und wie der Zugriff erfolgen soll");
            case 3:
                return International.getString("Bitte vervollständige die Angaben zu Deinem Verein.");
            case 4:
                return International.getString("Bitte gib an, in welchen Dachverbänden Dein Verein Mitglied ist, und (falls vorhanden) die Benutzernamen "+
                        "für elektronische Meldung und ähnliche Dienste.");
        }
        return "";
    }

    void initializeItems() {
        items = new ArrayList<IItemType>();

        ProjectRecord r;

        r = Project.createNewRecordFromStatic(ProjectRecord.TYPE_PROJECT);
        items.addAll(r.getGuiItems(admin, 1, CATEGORY_STEP_0, true));
        items.addAll(r.getGuiItems(admin, 2, CATEGORY_STEP_1, true));
        items.addAll(r.getGuiItems(admin, 3, CATEGORY_STEP_2, true));
        r = Project.createNewRecordFromStatic(ProjectRecord.TYPE_CLUB);
        items.addAll(r.getGuiItems(admin, 1, CATEGORY_STEP_3, true));
        items.addAll(r.getGuiItems(admin, 2, CATEGORY_STEP_4, true));
        if (Waters.getResourceTemplate(International.getLanguageID()) != null) {
            items.add(new ItemTypeBoolean(GUIITEM_CREATE_WATERS_LIST, true,
                    IItemType.TYPE_PUBLIC, CATEGORY_STEP_3,
                    International.getString("Gewässerliste mit Standardgewässern erstellen")));
        }
    }

    boolean checkInput(int direction) {
        boolean ok = super.checkInput(direction);
        if (!ok) {
            return false;
        }
        
        if (step == 0) {
            ItemTypeString item = (ItemTypeString)getItemByName(ProjectRecord.PROJECTNAME);
            String name = item.getValue();
            Project prj = new Project(IDataAccess.TYPE_FILE_XML, Daten.efaDataDirectory, name);
            try {
                if (prj.data().existsStorageObject()) {
                    Dialog.error(LogString.fileAlreadyExists(name, International.getString("Projekt")));
                    item.requestFocus();
                    return false;
                }
            } catch (Exception e) {
            }
        }


        if (step == 1) {
            ItemTypeStringList item = (ItemTypeStringList)getItemByName(ProjectRecord.STORAGETYPE);

            // remove all StorageType-specific config options
            ProjectRecord rPrj = Project.createNewRecordFromStatic(ProjectRecord.TYPE_PROJECT);
            ProjectRecord rClb = Project.createNewRecordFromStatic(ProjectRecord.TYPE_CLUB);
            Vector<IItemType> itemsToBeDeleted = new Vector<IItemType>();
            itemsToBeDeleted.addAll(rPrj.getGuiItems(admin, 3, CATEGORY_STEP_2, true));
            rPrj.setStorageType(IDataAccess.TYPE_EFA_REMOTE);
            itemsToBeDeleted.addAll(rPrj.getGuiItems(admin, 3, CATEGORY_STEP_2, true));
            rPrj.setStorageType(IDataAccess.TYPE_EFA_CLOUD);
            itemsToBeDeleted.addAll(rPrj.getGuiItems(admin, 3, CATEGORY_STEP_2, true));
            
            itemsToBeDeleted.add(new ItemTypeBoolean(GUIITEM_CREATE_STATUS_LIST, false,IItemType.TYPE_PUBLIC, CATEGORY_STEP_3, ""));
            itemsToBeDeleted.add(new ItemTypeLabel(EfaConfig.NOT_STORED_ITEM_PREFIX+GUIITEM_CREATE_STATUS_LIST_HINT, IItemType.TYPE_PUBLIC, CATEGORY_STEP_3, ""));
            
            // delete all Club items
            itemsToBeDeleted.addAll(rClb.getGuiItems(admin, 1, CATEGORY_STEP_3, true));
            itemsToBeDeleted.addAll(rClb.getGuiItems(admin, 2, CATEGORY_STEP_4, true));
            itemsToBeDeleted.add(new ItemTypeBoolean(GUIITEM_CREATE_WATERS_LIST, true, IItemType.TYPE_PUBLIC, CATEGORY_STEP_3, ""));
            itemsToBeDeleted.add(new ItemTypeLabel(GUIITEM_NODATA_LABEL1,  IItemType.TYPE_PUBLIC, CATEGORY_STEP_3, ""));
            itemsToBeDeleted.add(new ItemTypeLabel(GUIITEM_NODATA_LABEL2,  IItemType.TYPE_PUBLIC, CATEGORY_STEP_4, ""));
            

            // delete items we don't want
            for (int i=0; i<items.size(); i++) {
                for (int j=0; j<itemsToBeDeleted.size(); j++) {
                    if (items.get(i).getName().equals(itemsToBeDeleted.get(j).getName())) {
                        items.remove(i--);
                    }
                }
            }


            // add all StorageType-specific config options
            rPrj.setStorageType(IDataAccess.TYPE_FILE_XML);
            if (item.getValue().equals(IDataAccess.TYPESTRING_EFA_REMOTE)) {
                rPrj.setStorageType(IDataAccess.TYPE_EFA_REMOTE);
            }
            if (item.getValue().equals(IDataAccess.TYPESTRING_EFA_CLOUD)) {
                rPrj.setStorageType(IDataAccess.TYPE_EFA_CLOUD);
            }
            items.addAll(rPrj.getGuiItems(admin, 3, CATEGORY_STEP_2, true));

            if (item.getValue().equals(IDataAccess.TYPESTRING_FILE_XML) ||
                item.getValue().equals(IDataAccess.TYPESTRING_EFA_CLOUD)) {
                items.addAll(rClb.getGuiItems(admin, 1, CATEGORY_STEP_3, true));
                items.addAll(rClb.getGuiItems(admin, 2, CATEGORY_STEP_4, true));
                
                if (item.getValue().equals(IDataAccess.TYPESTRING_EFA_CLOUD)) {
                	String hint1 = International.getString("Folgende Optionen nur dann aktivieren, wenn der efaCloud-Server noch keine Daten hat.");
                	items.add(EfaGuiUtils.createHintWordWrap(GUIITEM_CREATE_STATUS_LIST_HINT, IItemType.TYPE_PUBLIC, CATEGORY_STEP_3,
                			hint1,3,10,5,630));
                	items.add(new ItemTypeBoolean(GUIITEM_CREATE_STATUS_LIST, false,
                            IItemType.TYPE_PUBLIC, CATEGORY_STEP_3,
                            International.getString("Status-Einträge für Personen (Gast, Mitglied) erstellen")));
                }

                // Only provide the checkbox to create new waters in efa, 
                // if there is an Waters_xx.txt file in efa jar file (de.nmichael.efa.data.templates)
                // AND if project is neither EFA_REMOTE nor EFA_CLOUD.
                // Adding waters to efacloud project automatically MAY lead to problems with duplicate waters in efacloud server,
                // if we are adding the new project to an existing efaCloud installation.
                if (Waters.getResourceTemplate(International.getLanguageID()) != null) {
                	items.add(new ItemTypeBoolean(GUIITEM_CREATE_WATERS_LIST, item.getValue().equals(IDataAccess.TYPESTRING_FILE_XML) /*default true for local projects*/,
                            IItemType.TYPE_PUBLIC, CATEGORY_STEP_3,
                            International.getString("Gewässerliste mit Standardgewässern erstellen")));
                }                
            } else {
                items.add(new ItemTypeLabel(GUIITEM_NODATA_LABEL1,
                        IItemType.TYPE_PUBLIC, CATEGORY_STEP_3,
                            International.getString("Keine Angaben erforderlich")));
                items.add(new ItemTypeLabel(GUIITEM_NODATA_LABEL2,
                        IItemType.TYPE_PUBLIC, CATEGORY_STEP_4,
                            International.getString("Keine Angaben erforderlich")));
            }
            if (item.getValue().equals(IDataAccess.TYPESTRING_EFA_REMOTE)) {
                IItemType checkbox = getItemByName(ProjectRecord.EFAONLINECONNECT);
                if (checkbox != null) {
                    checkbox.registerItemListener(this);
                    itemListenerAction(checkbox, null);
                }
            }

        }
        return true;
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType.getName().equals(ProjectRecord.EFAONLINECONNECT) && (event == null || event instanceof ActionEvent)) {
            boolean efaOnline = Boolean.parseBoolean(itemType.getValueFromField());
            IItemType item;
            item = getItemByName(ProjectRecord.STORAGELOCATION);
            if (item != null) {
                item.setNotNull(!efaOnline);
                item.setEnabled(!efaOnline);
            }
            item = getItemByName(ProjectRecord.EFAONLINEUSERNAME);
            if (item != null) {
                item.setNotNull(efaOnline);
                item.setEnabled(efaOnline);
            }
            item = getItemByName(ProjectRecord.EFAONLINEPASSWORD);
            if (item != null) {
                item.setNotNull(efaOnline);
                item.setEnabled(efaOnline);
            }
        }
    }

    boolean finishButton_actionPerformed(ActionEvent e) {
        if (!super.finishButton_actionPerformed(e)) {
            return false;
        }

        ItemTypeString prjName = (ItemTypeString)getItemByName(ProjectRecord.PROJECTNAME);

        ItemTypeStringList storType = (ItemTypeStringList)getItemByName(ProjectRecord.STORAGETYPE);
        int storageType = -1;
        if (storType.getValue().equals(IDataAccess.TYPESTRING_FILE_XML)) {
            storageType = IDataAccess.TYPE_FILE_XML;
        }
        if (storType.getValue().equals(IDataAccess.TYPESTRING_EFA_REMOTE)) {
            storageType = IDataAccess.TYPE_EFA_REMOTE;
        }
        if (storType.getValue().equals(IDataAccess.TYPESTRING_EFA_CLOUD)) {
            storageType = IDataAccess.TYPE_EFA_CLOUD;
        }
        
        if (Daten.project != null && Daten.project.isOpen()) {
			try {
				Daten.project.closeAllStorageObjects();
			} catch (Exception e1) {
				Logger.logdebug(e1);
			}
        	Daten.project = null;
        }
        // Note: The storageType of the project file itself is always TYPE_FILE_XML.
        // The storageType of the project's content (set through prj.setProjectStorageType(storageType)) may differ.
        Project prj = new Project(prjName.getValue());
        try {
            prj.create();
            prj.setEmptyProject(prjName.getValue());

            // Project Properties
            prj.setProjectDescription(((ItemTypeString)getItemByName(ProjectRecord.DESCRIPTION)).getValue());
            prj.setProjectStorageType(storageType);
            prj.setAdminName(((ItemTypeString)getItemByName(ProjectRecord.ADMINNAME)).getValue());
            prj.setAdminEmail(((ItemTypeString)getItemByName(ProjectRecord.ADMINEMAIL)).getValue());
            if (getItemByName(ProjectRecord.STORAGELOCATION) != null) {
                prj.setProjectStorageLocation(((ItemTypeString)getItemByName(ProjectRecord.STORAGELOCATION)).getValue());
            }
            if (getItemByName(ProjectRecord.EFACLOUDURL) != null) {
                prj.setProjectEfaCloudURL(((ItemTypeString) getItemByName(ProjectRecord.EFACLOUDURL)).getValue());
            }
            if (getItemByName(ProjectRecord.STORAGEUSERNAME) != null) {
                prj.setProjectStorageUsername(((ItemTypeString)getItemByName(ProjectRecord.STORAGEUSERNAME)).getValue());
            }
            if (getItemByName(ProjectRecord.STORAGEPASSWORD) != null) {
                prj.setProjectStoragePassword(((ItemTypeString)getItemByName(ProjectRecord.STORAGEPASSWORD)).getValue());
            }
            if (getItemByName(ProjectRecord.REMOTEPROJECTNAME) != null) {
                prj.setProjectRemoteProjectName(((ItemTypeString)getItemByName(ProjectRecord.REMOTEPROJECTNAME)).getValue());
            }
            if (getItemByName(ProjectRecord.EFAONLINECONNECT) != null) {
                prj.setProjectEfaOnlineConnect(((ItemTypeBoolean)getItemByName(ProjectRecord.EFAONLINECONNECT)).getValue());
            }
            if (getItemByName(ProjectRecord.EFAONLINEUSERNAME) != null) {
                prj.setProjectEfaOnlineUsername(((ItemTypeString)getItemByName(ProjectRecord.EFAONLINEUSERNAME)).getValue());
            }
            if (getItemByName(ProjectRecord.EFAONLINEPASSWORD) != null) {
                prj.setProjectEfaOnlinePassword(((ItemTypePassword)getItemByName(ProjectRecord.EFAONLINEPASSWORD)).getValue());
            }

            // Club Properties (1)
            if (storageType == IDataAccess.TYPE_FILE_XML || storageType == IDataAccess.TYPE_EFA_CLOUD) {
                prj.setClubName(((ItemTypeString) getItemByName(ProjectRecord.CLUBNAME)).getValue());
                prj.setClubAddressStreet(((ItemTypeString) getItemByName(ProjectRecord.ADDRESSSTREET)).getValue());
                prj.setClubAddressCity(((ItemTypeString) getItemByName(ProjectRecord.ADDRESSCITY)).getValue());
                if (getItemByName(ProjectRecord.AREAID) != null) {
                    prj.setBoathouseAreaId(((ItemTypeInteger) getItemByName(ProjectRecord.AREAID)).getValue());
                }

                // Club Properties (2)
                prj.setClubGlobalAssociationName(((ItemTypeString) getItemByName(ProjectRecord.ASSOCIATIONGLOBALNAME)).getValue());
                prj.setClubGlobalAssociationMemberNo(((ItemTypeString) getItemByName(ProjectRecord.ASSOCIATIONGLOBALMEMBERNO)).getValue());
                prj.setClubGlobalAssociationLogin(((ItemTypeString) getItemByName(ProjectRecord.ASSOCIATIONGLOBALLOGIN)).getValue());
                prj.setClubRegionalAssociationName(((ItemTypeString) getItemByName(ProjectRecord.ASSOCIATIONREGIONALNAME)).getValue());
                prj.setClubRegionalAssociationMemberNo(((ItemTypeString) getItemByName(ProjectRecord.ASSOCIATIONREGIONALMEMBERNO)).getValue());
                prj.setClubRegionalAssociationLogin(((ItemTypeString) getItemByName(ProjectRecord.ASSOCIATIONREGIONALLOGIN)).getValue());
                if (getItemByName(ProjectRecord.MEMBEROFDRV) != null) {
                    prj.setClubMemberOfDRV(((ItemTypeBoolean) getItemByName(ProjectRecord.MEMBEROFDRV)).getValue());
                }
                if (getItemByName(ProjectRecord.MEMBEROFSRV) != null) {
                    prj.setClubMemberOfSRV(((ItemTypeBoolean) getItemByName(ProjectRecord.MEMBEROFSRV)).getValue());
                }
                if (getItemByName(ProjectRecord.MEMBEROFADH) != null) {
                    prj.setClubMemberOfADH(((ItemTypeBoolean) getItemByName(ProjectRecord.MEMBEROFADH)).getValue());
                }
                if (getItemByName(ProjectRecord.KANUEFBUSERNAME) != null) {
                    prj.setClubKanuEfbUsername(((ItemTypeString) getItemByName(ProjectRecord.KANUEFBUSERNAME)).getValue());
                }
                if (getItemByName(ProjectRecord.KANUEFBPASSWORD) != null) {
                    prj.setClubKanuEfbPassword(((ItemTypeString) getItemByName(ProjectRecord.KANUEFBPASSWORD)).getValue());
                }
            }

            prj.close();
            Project.openProject(prjName.getValue(), false);
            if (Daten.project != null) {
                Dialog.infoDialog(LogString.fileSuccessfullyCreated(prjName.getValue(),
                        International.getString("Projekt")));
                try {
                    if (Waters.getResourceTemplate(International.getLanguageID()) != null) {
                        ItemTypeBoolean createWatersList = (ItemTypeBoolean)getItemByName(GUIITEM_CREATE_WATERS_LIST);
                        if (createWatersList != null && createWatersList.getValue() &&
                            storageType != IDataAccess.TYPE_EFA_REMOTE) {
                        	//no waters list for efaRemote (as not necessary) 
                        	//for efaCloud projects, user can specify if waters should be added.
                        	//for efaCloud projects, and efaCloud project (may be faulty if new project is added to an existing efaCloud server with existing Waters...)
                            Daten.project.getWaters(false).addAllWatersFromTemplate(International.getLanguageID());
                        }
                    }
                    
                    // if efaCloud mode AND user chose to create the person statuses, do so. 
                    // for efaRemote this shall not be done; for local projects, this is done in Status.Java when opening the file.
                    ItemTypeBoolean createStatuses = (ItemTypeBoolean)getItemByName(GUIITEM_CREATE_STATUS_LIST);
                    if (createStatuses != null && createStatuses.getValue() && storageType == IDataAccess.TYPE_EFA_CLOUD) {
                    	Status myStatus = Daten.project.getStatus(false);
                    	myStatus.createPredefinedStatuses();
                    }
                    
                } catch(Exception eignore) {
                    Logger.logdebug(eignore);
                }
            }
            setDialogResult(Daten.project != null);
        } catch(EfaException ee) {
            Dialog.error(ee.getMessage());
            ee.log();
            setDialogResult(false);
        }
        return true;
    }

    public String createNewProjectAndLogbook() {
        showDialog();
        if (!getDialogResult()) {
            return null;
        }
        if (Daten.project != null && Daten.project.getProjectStorageType() != IDataAccess.TYPE_FILE_XML &&
            Daten.project.getProjectStorageType() != IDataAccess.TYPE_EFA_CLOUD) {
            return null;
        }
        String logbookName = null;
        switch(Dialog.auswahlDialog(International.getString("Fahrtenbuch erstellen"),
                International.getString("Das Projekt enthält noch keine Daten.") + " " +
                International.getString("Was möchtest Du tun?"),
                International.getString("Neues (leeres) Fahrtenbuch erstellen"),
                International.getString("Daten aus efa 1.x importieren"))) {
            case 0:
                NewLogbookDialog dlg0 = null;
                if (getParentJDialog() != null) {
                    dlg0 = new NewLogbookDialog(getParentJDialog());
                }
                if (getParentFrame() != null) {
                    dlg0 = new NewLogbookDialog(getParentFrame());
                }
                logbookName = dlg0.newLogbookDialog();
                break;
            case 1:
                ImportEfa1DataDialog dlg1 = null;
                if (getParentJDialog() != null) {
                    dlg1 = new ImportEfa1DataDialog(getParentJDialog());
                }
                if (getParentFrame() != null) {
                    dlg1 = new ImportEfa1DataDialog(getParentFrame());
                }
                dlg1.showDialog();
                logbookName = dlg1.getNewestLogbookName();
                break;
            default: // when the user hits ESC create a new logbook
                NewLogbookDialog dlg2 = null;
                if (getParentJDialog() != null) {
                    dlg2 = new NewLogbookDialog(getParentJDialog());
                }
                if (getParentFrame() != null) {
                    dlg2 = new NewLogbookDialog(getParentFrame());
                }
                logbookName = dlg2.newLogbookDialog();
                break;
            	
            	
        }
        return logbookName;
    }

}
