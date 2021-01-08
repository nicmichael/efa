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

import de.nmichael.efa.*;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.dataedit.*;
import de.nmichael.efa.gui.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

// @i18n complete
public class OpenProjectOrLogbookDialog extends BaseDialog implements IItemListener {

    public enum Type {
        project,
        logbook,
        clubwork
    }

    private String name;
    private Type type;
    private AdminRecord admin;
    private String[] keys;
    private ItemTypeHtmlList list;

    public OpenProjectOrLogbookDialog(Frame parent, Type type, AdminRecord admin) {
        super(parent,
                (type == Type.project ? International.getString("Projekt öffnen") :
                        (type == Type.logbook ? International.getString("Fahrtenbuch öffnen") :
                                International.getString("Vereinsarbeitsbuch öffnen"))
                ),
                International.getStringWithMnemonic("Abbruch"));
        this.admin = admin;
        this.type = type;
    }

    public OpenProjectOrLogbookDialog(JDialog parent, Type type, AdminRecord admin) {
        super(parent,
                (type == Type.project ? International.getString("Projekt öffnen")
                        : (type == Type.logbook ? International.getString("Fahrtenbuch öffnen")
                        : International.getString("Vereinsarbeitsbuch öffnen"))),
                International.getStringWithMnemonic("Abbruch"));
        this.admin = admin;
        this.type = type;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        // create GUI items
        mainPanel.setLayout(new GridBagLayout());

        JLabel label = new JLabel();
        if (type == Type.project) {
            label.setText(International.getString("vorhandene Projekte"));
        }
        if (type == Type.logbook) {
            label.setText(International.getString("vorhandene Fahrtenbücher"));
        }
        if (type == Type.clubwork) {
            label.setText(International.getString("vorhandene Vereinsarbeitsbücher"));
        }
        mainPanel.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));

        list = new ItemTypeHtmlList("LIST", null, null, null, IItemType.TYPE_PUBLIC, null, label.getText());
        String[] actions = {
                International.getString("Öffnen"),
                International.getString("Einstellungen"),
                International.getString("Löschen")
        };
        list.setPopupActions(actions);
        list.registerItemListener(this);
        list.setFieldGrid(1, 5, GridBagConstraints.CENTER, GridBagConstraints.NONE);
        list.setPadding(10, 10, 0, 10);
        list.displayOnGui(_parent, mainPanel, 0, 1);

        JButton newButton = new JButton();
        Mnemonics.setButton(this, newButton, International.getString("Neu"),
                BaseDialog.IMAGE_ADD);
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                newButton_actionPerformed(e);
            }
        });
        mainPanel.add(newButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 10), 0, 0));
        JButton openButton = new JButton();
        Mnemonics.setButton(this, openButton, International.getString("Öffnen"),
                BaseDialog.IMAGE_OPEN);
        openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openButton_actionPerformed(e);
            }
        });
        mainPanel.add(openButton, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 10), 0, 0));

        JButton configureButton = new JButton();
        Mnemonics.setButton(this, configureButton, International.getString("Einstellungen"),
                BaseDialog.IMAGE_CONFIGURE);
        configureButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                configureButton_actionPerformed(e);
            }
        });
        mainPanel.add(configureButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 10), 0, 0));

        JButton deleteButton = new JButton();
        Mnemonics.setButton(this, deleteButton, International.getString("Löschen"),
                BaseDialog.IMAGE_DELETE);
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteButton_actionPerformed(e);
            }
        });
        mainPanel.add(deleteButton, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 10), 0, 0));

        updateGui();
    }

    public void updateGui() {
        Hashtable<String,String> items = null;

        if (type == Type.project) {
            items = Project.getProjects();
        }
        if (type == Type.logbook && Daten.project != null) {
            items = Daten.project.getLogbooks();
        }
        if(type == Type.clubwork && Daten.project != null) {
            items = Daten.project.getClubworks();
        }

        keys = items.keySet().toArray(new String[0]);
        Arrays.sort(keys);

        list.setValues(keys, items);
    }

    public void itemListenerAction(IItemType item, AWTEvent event) {
        if (item != null && event != null && item == list) {
            if (event instanceof ActionEvent) {
                ActionEvent e = (ActionEvent)event;
                String cmd = e.getActionCommand();
                if (cmd != null && cmd.equals(EfaMouseListener.EVENT_MOUSECLICKED_2x)) {
                    openButton_actionPerformed(e);
                }
                if (cmd != null && cmd.startsWith(EfaMouseListener.EVENT_POPUP_CLICKED)) {
                    int id = EfaUtil.string2date(cmd, -1, -1, -1).tag;
                    switch(id) {
                        case 0:
                            openButton_actionPerformed(e);
                            break;
                        case 1:
                            configureButton_actionPerformed(e);
                            break;
                        case 2:
                            deleteButton_actionPerformed(e);
                            break;
                    }

                }
            }
        }
    }

    void newButton_actionPerformed(ActionEvent e) {
        if (type == Type.project) {
            NewProjectDialog dlg = new NewProjectDialog(this, admin);
            dlg.createNewProjectAndLogbook();
            updateGui();
            return;
        }
        if (type == Type.logbook) {
            NewLogbookDialog dlg = new NewLogbookDialog(this);
            dlg.newLogbookDialog();
            updateGui();
            return;
        }
        if (type == Type.clubwork) {
            NewClubworkBookDialog dlg = new NewClubworkBookDialog(this);
            dlg.newClubworkBookDialog();
            updateGui();
            return;
        }
    }

    void openButton_actionPerformed(ActionEvent e) {
        name = list.getValueFromField();
        if (name != null) {
            closeButton_actionPerformed(e);
        }
    }

    void configureButton_actionPerformed(ActionEvent e) {
        String name = list.getValueFromField();
        if (name == null) {
            return;
        }

        if (type == Type.project) {
            Project prj = null;
            try {
                if (Daten.project != null && Daten.project.getProjectName() != null &&
                        Daten.project.getProjectName().equals(name)) {
                    prj = Daten.project;
                } else {
                    prj = new Project(name);
                    prj.open(false);
                }
            } catch (Exception ex) {
                Logger.logdebug(ex);
                Dialog.error(ex.toString());
                return;
            }
            ProjectEditDialog dlg = new ProjectEditDialog(this, prj, ProjectRecord.GUIITEMS_SUBTYPE_ALL, admin);
            dlg.showDialog();
        }

        if (type == Type.logbook) {
            ProjectRecord logbook = Daten.project.getLoogbookRecord(name);
            if (Daten.project == null || logbook == null) {
                return;
            }
            ProjectEditDialog dlg = new ProjectEditDialog(this, Daten.project, logbook, ProjectRecord.GUIITEMS_SUBTYPE_ALL, admin);
            dlg.showDialog();
        }

        if (type == Type.clubwork) {
            ProjectRecord clubwork = Daten.project.getClubworkBookRecord(name);
            if (Daten.project == null || clubwork == null) {
                return;
            }
            ProjectEditDialog dlg = new ProjectEditDialog(this, Daten.project, clubwork, ProjectRecord.GUIITEMS_SUBTYPE_ALL, admin);
            dlg.showDialog();
        }
    }

    void deleteButton_actionPerformed(ActionEvent e) {
        String name = list.getValueFromField();
        if (name == null) {
            return;
        }
        String message = null;
        Project prj = null;
        if (type == Type.project) {
            try {
                prj = new Project(name);
                prj.open(false);
            } catch(Exception ex) {
                Logger.logdebug(ex);
                Dialog.error(ex.toString());
                return;
            }
            message = International.getMessage("Möchtest Du das Projekt '{name}' wirklich löschen?", name) + "\n" +
                    (prj.getProjectStorageType() == IDataAccess.TYPE_FILE_XML ?
                            International.getString("Alle Daten des Projekts gehen damit unwiederbringlich verloren!") :
                            International.getString("Es wird nur die Projektkonfiguration gelöscht. Die Daten selbst bleiben erhalten.") );
        }
        if (type == Type.logbook) {
            message = International.getMessage("Möchtest Du das Fahrtenbuch '{name}' wirklich löschen?", name) + "\n" +
                    International.getString("Alle Fahrten des Fahrtenbuchs gehen damit unwiederbringlich verloren!");
        }
        if (type == Type.clubwork) {
            message = International.getMessage("Möchtest Du das Vereinsarbeitsbuch '{name}' wirklich löschen?", name) + "\n"
                    + International.getString("Alle Einträge des Vereinsarbeitsbuch gehen damit unwiederbringlich verloren!");
        }
        if (message == null) {
            return;
        }
        int res = Dialog.yesNoDialog(International.getString("Wirklich löschen?"), message);
        if (res != Dialog.YES) {
            return;
        }

        if (type == Type.project) {
            try {
                if (prj.getProjectStorageType() == IDataAccess.TYPE_FILE_XML) {
                    res = Dialog.yesNoDialog(International.getString("Bist Du sicher?"),
                            International.getString("Sämtliche Daten dieses Projekts (Fahrtenbücher, Mitglieder, Boote, Ziele etc.) werden unwiederbringlich gelöscht!") + "\n"+
                                    International.getString("Möchtest Du wirklich fortfahren?"));
                    if (res != Dialog.YES) {
                        return;
                    }
                }
                boolean success = false;
                if (Daten.project != null && Daten.project.getProjectName() != null &&
                        Daten.project.getProjectName().equals(prj.getProjectName())) {
                    success = Daten.project.deleteProject();
                    Daten.project = null;
                } else {
                    success = prj.deleteProject();
                }
                updateGui();
            } catch(Exception ex) {
                Dialog.error(ex.toString());
                Logger.logdebug(ex);
            }
        }

        if (type == Type.logbook) {
            try {
                Logbook logbook = Daten.project.getLogbook(name, false);
                if (Daten.project.deleteLogbookRecord(name)) {
                    logbook.data().deleteStorageObject();
                }
                updateGui();
            } catch (Exception ex) {
                Dialog.error(ex.toString());
                Logger.logdebug(ex);
            }
        }

        if (type == Type.clubwork) {
            try {
                Clubwork clubwork = Daten.project.getClubwork(name, false);
                if (Daten.project.deleteClubworkBook(name)) {
                    clubwork.data().deleteStorageObject();
                }
                updateGui();
            } catch (Exception ex) {
                Dialog.error(ex.toString());
                Logger.logdebug(ex);
            }
        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        super.closeButton_actionPerformed(e);
    }

    public String openDialog() {
        showDialog();
        return name;
    }

}
