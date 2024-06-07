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
import de.nmichael.efa.core.Backup;
import de.nmichael.efa.core.BackupMetaData;
import de.nmichael.efa.core.BackupMetaDataItem;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.gui.util.EfaMenuButton;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class BackupDialog extends BaseTabbedDialog implements IItemListener {

    private static final String CREATE_BUTTON = "CREATE_BUTTON";

    private static final String ZIPFILE_SELECT = "ZIPFILE_SELECT";
    private static final String RESTORE_SELECT_CONFIG = "RESTORE_SELECT_CONFIG";
    private static final String RESTORE_SELECT_PROJECT = "RESTORE_SELECT_PROJECT";
    private static final String RESTORE_BUTTON = "RESTORE_BUTTON";

    private AdminRecord admin;
    private ItemTypeBoolean createSelectProject;
    private ItemTypeBoolean createSelectConfig;
    private ItemTypeBoolean createSelectEfaLog;
    private ItemTypeFile createDirectory;
    private ItemTypeFile restoreZipFile;
    private ItemTypeString restoreInfoProject;
    private ItemTypeString restoreInfoDate;
    private ItemTypeTable restoreArchiveContents;
    private ItemTypeBoolean restoreSelectProject;
    private ItemTypeBoolean restoreSelectConfig;
    private BackupMetaData restoreMetaData;

    public BackupDialog(Frame parent, AdminRecord admin) {
        super(parent,
                International.getStringWithMnemonic("Backup"),
                International.getStringWithMnemonic("Schließen"),
                null, true);
        iniItems(admin);
    }

    public BackupDialog(JDialog parent, AdminRecord admin) {
        super(parent,
                International.getStringWithMnemonic("Backup"),
                International.getStringWithMnemonic("Schließen"),
                null, true);
        iniItems(admin);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    private void iniItems(AdminRecord admin) {
        this.admin = admin;
        Vector<IItemType> guiItems = new Vector<IItemType>();
        String cat;
        IItemType item;

        // Create Backup
        cat = "%01%" + International.getString("Backup erstellen");

        guiItems.add(item = new ItemTypeBoolean("CREATE_SELECT_CONFIG", true,
                IItemType.TYPE_PUBLIC, cat, International.getMessage("{typeOfData} sichern",
                International.getString("Konfigurationsdaten"))));
        createSelectConfig = (ItemTypeBoolean)item;

        if (Daten.project != null) {
            guiItems.add(item = new ItemTypeBoolean("CREATE_SELECT_PROJECT", true,
                    IItemType.TYPE_PUBLIC, cat, International.getMessage("{typeOfData} sichern",
                    International.getMessage("Projekt '{name}'", Daten.project.getProjectName()))));
            createSelectProject = (ItemTypeBoolean)item;
        }

        guiItems.add(item = new ItemTypeBoolean("CREATE_SELECT_EFA_LOG", false,
                IItemType.TYPE_PUBLIC, cat, International.getMessage("{typeOfData} sichern",
                		International.getString("EFA Logdatei"))));

        createSelectEfaLog= (ItemTypeBoolean)item;
        
        guiItems.add(item = new ItemTypeFile("CREATE_DIRECTORY", Daten.efaBakDirectory,
                    International.getString("Verzeichnis"),
                    International.getString("Verzeichnisse"),
                    null, ItemTypeFile.MODE_OPEN, ItemTypeFile.TYPE_DIR,
                    IItemType.TYPE_PUBLIC, cat,
                    International.getString("Backupverzeichnis")));
        createDirectory = (ItemTypeFile)item;
        createDirectory.setPadding(0, 0, 10, 10);

        guiItems.add(item = new ItemTypeButton(CREATE_BUTTON,
                IItemType.TYPE_PUBLIC, cat, International.getString("Backup erstellen")));
        ((ItemTypeButton)item).setPadding(0, 0, 20, 20);
        ((ItemTypeButton)item).setIcon(getIcon(IMAGE_RUNEXPORT));
        ((ItemTypeButton)item).registerItemListener(this);
        item.setFieldGrid(3, GridBagConstraints.CENTER, GridBagConstraints.NONE);

        // Restore Backup
        cat = "%02%" + International.getString("Backup einspielen");

        guiItems.add(item = new ItemTypeFile(ZIPFILE_SELECT, "",
                    International.getString("Backup"),
                    International.getString("ZIP-Archiv") + " (*.zip)",
                    "zip", ItemTypeFile.MODE_OPEN, ItemTypeFile.TYPE_FILE,
                    IItemType.TYPE_PUBLIC, cat,
                    International.getString("Backupdatei")));
        restoreZipFile = (ItemTypeFile)item;
        restoreZipFile.setFieldSize(500, -1);
        restoreZipFile.setPadding(0, 0, 10, 10);
        restoreZipFile.setFileDialogBaseDirectory(Daten.efaBakDirectory);
        restoreZipFile.registerItemListener(this);

        guiItems.add(item = new ItemTypeString("RESTORE_INFOPROJECT", "",
                IItemType.TYPE_PUBLIC, cat, International.getString("Gesichertes Projekt")));
        restoreInfoProject = (ItemTypeString)item;
        restoreInfoProject.setEditable(false);
        guiItems.add(item = new ItemTypeString("RESTORE_INFODATE", "",
                IItemType.TYPE_PUBLIC, cat, International.getString("Datum der Sicherung")));
        restoreInfoDate = (ItemTypeString)item;
        restoreInfoDate.setEditable(false);

        guiItems.add(item = new ItemTypeBoolean(RESTORE_SELECT_CONFIG, false,
                IItemType.TYPE_PUBLIC, cat, International.getMessage("{typeOfData} wiederherstellen",
                International.getString("Konfigurationsdaten"))));
        restoreSelectConfig = (ItemTypeBoolean)item;
        restoreSelectConfig.registerItemListener(this);

        guiItems.add(item = new ItemTypeBoolean(RESTORE_SELECT_PROJECT, true,
                IItemType.TYPE_PUBLIC, cat,
                International.getMessage("{typeOfData} wiederherstellen",
                International.getMessage("Projekt '{name}'",
                International.getString("unbekannt")))));
        restoreSelectProject = (ItemTypeBoolean) item;
        restoreSelectProject.registerItemListener(this);

        guiItems.add(item = new ItemTypeTable("RESTORE_ARCHIVECONTENT",
                    new String[] {
                        International.getString("Objekt"),
                        International.getString("Datensätze"),
                        "SCN",
                    },
                    null, null,
                    IItemType.TYPE_PUBLIC, cat,
                    International.getString("Inhalt")));
        restoreArchiveContents = (ItemTypeTable)item;
        restoreArchiveContents.setSortingEnabled(false);
        restoreArchiveContents.setFieldGrid(3, GridBagConstraints.CENTER, GridBagConstraints.NONE);

        guiItems.add(item = new ItemTypeButton(RESTORE_BUTTON,
                IItemType.TYPE_PUBLIC, cat, International.getString("Ausgewählte Objekte wiederherstellen")));
        ((ItemTypeButton)item).setIcon(getIcon(IMAGE_RUNIMPORT));
        ((ItemTypeButton)item).setPadding(0, 0, 20, 20);
        ((ItemTypeButton)item).registerItemListener(this);
        item.setFieldGrid(3, GridBagConstraints.CENTER, GridBagConstraints.NONE);

        super.setItems(guiItems);
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType.getName().equals(CREATE_BUTTON) && event instanceof ActionEvent) {
            if (!admin.isAllowedCreateBackup()) {
                EfaMenuButton.insufficientRights(admin, itemType.getDescription());
                return;
            }
            getValuesFromGui();
            if (createDirectory.getValue().length() == 0) {
                Dialog.error(International.getString("Feld darf nicht leer sein"));
                createDirectory.requestFocus();
                return;
            }
            Backup.runCreateBackupTask(this,
                        createDirectory.getValue(), null,
                        (createSelectProject != null ? createSelectProject.getValue() : false),
                        createSelectConfig.getValue(),
                        createSelectEfaLog.getValue());
        }

        if (itemType.getName().equals(ZIPFILE_SELECT) && event instanceof ActionEvent) {
            getValuesFromGui();
            readBackupContent();
        }

        if ((itemType.getName().equals(RESTORE_SELECT_CONFIG) ||
             itemType.getName().equals(RESTORE_SELECT_PROJECT)) &&
             event instanceof ActionEvent) {
            restoreSelectConfig.getValueFromGui();
            restoreSelectProject.getValueFromGui();
            selectItems(restoreArchiveContents, restoreSelectConfig.getValue(), restoreSelectProject.getValue());
        }

        if (itemType.getName().equals(RESTORE_BUTTON) && event instanceof ActionEvent) {
            if (!admin.isAllowedRestoreBackup()) {
                EfaMenuButton.insufficientRights(admin, itemType.getDescription());
                return;
            }
            getValuesFromGui();
            if (restoreMetaData == null) {
                readBackupContent();
            }
            if (restoreMetaData == null) {
                return;
            }
            String[] selectedObjects = restoreArchiveContents.getSelectedKeys();
            if (selectedObjects == null || selectedObjects.length == 0) {
                Dialog.error(International.getString("Keine Objekte ausgewählt."));
                return;
            }
            String[] selectedMetaNames = new String[selectedObjects.length];
            for (int i = 0; i < selectedObjects.length; i++) {
                selectedMetaNames[i] = restoreMetaData.getTableItem(selectedObjects[i]).getNameAndType();
            }
            selectedObjects = selectedMetaNames;

            if (Dialog.yesNoCancelDialog(International.getString("Backup einspielen"),
                    International.getMessage("Möchtest Du {count} ausgewählte Objekte wiederherstellen?",
                    selectedObjects.length)) != Dialog.YES) {
                return;
            }

            // check whether any of the objects to be restored is a project object
            boolean openOrCreateProjectForRestore = false;
            boolean containsProjectData = false;
            for (String object : selectedObjects) {
                BackupMetaDataItem meta = restoreMetaData.getItem(object);
                if (meta != null && Backup.isProjectDataAccess(meta.getType())) {
                    containsProjectData = true;
                }
            }

            // check whether all project data has been selected
            if (containsProjectData) {
                ArrayList<String> selection = new ArrayList<String>();
                for (String k : selectedObjects) {
                    selection.add(k);
                }
                for (int i=0; i<restoreMetaData.size(); i++) {
                    BackupMetaDataItem meta = restoreMetaData.getItem(i);
                    if (meta != null && Backup.isProjectDataAccess(meta.getType())
                            && !selection.contains(meta.getNameAndType())) {
                        if (Dialog.yesNoCancelDialog(International.getString("Warnung"),
                                International.getString("Du hast nicht alle Projektdaten zur Wiederherstellung ausgewählt. "
                                + "Eine nur teilweise Wiederherstellung des Projekts kann zu inkonsistenten "
                                + "Daten führen, die später unter Umständen durch den Audit korrigiert oder gelöscht werden.") + "\n"
                                + International.getString("Möchtest Du wirklich fortfahren?")) != Dialog.YES) {
                            return;
                        } else {
                            break;
                        }
                    }
                }
            }

            String prjName = "<unknown>";
            if (Daten.project != null && Daten.project.isOpen()) {
                if (Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                    prjName = Daten.project.getProjectRemoteProjectName();
                } else {
                    prjName = Daten.project.getProjectName();
                }
            }
            if (containsProjectData && prjName != null && !prjName.equals(restoreMetaData.getProjectName())) {
                if (Dialog.yesNoCancelDialog(International.getString("Backup einspielen"),
                        International.getMessage("Daten des Projekts {name} können nur in diesem auch wiederhergestellt werden, aber derzeit ist Projekt {name} geöffnet.",
                        restoreMetaData.getProjectName(),
                        prjName) + "\n"
                        + International.getMessage("Möchtest Du das Projekt {name} öffnen oder neu erstellen?",
                        restoreMetaData.getProjectName())) != Dialog.YES) {
                    return;
                }
                openOrCreateProjectForRestore = true;
            }

            Backup.runRestoreBackupTask(this,
                    restoreMetaData.getZipFileName(), selectedObjects,
                    openOrCreateProjectForRestore);
        }
    }

    private void readBackupContent() {
        if (restoreZipFile.getValue().length() == 0) {
            Dialog.error(International.getString("Feld darf nicht leer sein"));
            restoreZipFile.requestFocus();
            return;
        }
        restoreMetaData = new BackupMetaData(null);
        if (restoreMetaData.read(restoreZipFile.getValue())) {
            Hashtable<String, TableItem[]> items = new Hashtable<String, TableItem[]>();
            for (int i = 0; i < restoreMetaData.size(); i++) {
                BackupMetaDataItem meta = restoreMetaData.getItem(i);
                TableItem[] tItem = new TableItem[3];
                tItem[0] = new TableItem(meta.getDescription());
                tItem[1] = new TableItem(meta.getNumberOfRecords());
                tItem[2] = new TableItem(meta.getScn());
                items.put(meta.getKeyForTable(), tItem);
            }
            restoreArchiveContents.setValues(items);
            restoreArchiveContents.showValue();
            selectItems(restoreArchiveContents, restoreSelectConfig.getValue(), restoreSelectProject.getValue());
            restoreInfoProject.parseAndShowValue( (restoreMetaData.getProjectName() != null ?
                                                   restoreMetaData.getProjectName() : "") );
            restoreInfoDate.parseAndShowValue(EfaUtil.getTimeStamp(restoreMetaData.getTimeStamp()));

            restoreSelectProject.setDescription(International.getMessage("{typeOfData} wiederherstellen",
                International.getMessage("Projekt '{name}'",
                (restoreMetaData.getProjectName() != null ? restoreMetaData.getProjectName() : 
                    International.getString("unbekannt")))));
        } else {
            Dialog.error(International.getString("ZIP-Archiv kann nicht gelesen werden oder ist kein gültiges Backup."));
            restoreMetaData = null;
            restoreArchiveContents.setValues(null);
            restoreArchiveContents.showValue();
            restoreZipFile.requestFocus();
            restoreInfoProject.parseAndShowValue("");
            restoreInfoDate.parseAndShowValue("");
        }
    }

    private void selectItems(ItemTypeTable table, boolean config, boolean project) {
        String[] keys = restoreArchiveContents.getAllKeys();
        if (keys == null) {
            return;
        }
        boolean[] selected = new boolean[keys.length];
        for (int i=0; i<keys.length; i++) {
            selected[i] = false;
            if (keys[i].startsWith("C") && config) {
                selected[i] = true;
            }
            if (keys[i].startsWith("P") && project) {
                selected[i] = true;
            }
        }
        restoreArchiveContents.selectValues(selected);
    }

}
