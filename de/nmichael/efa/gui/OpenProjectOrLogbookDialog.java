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

import java.awt.AWTEvent;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeHtmlList;
import de.nmichael.efa.data.Clubwork;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.efacloud.EfaCloudUtil;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.gui.dataedit.ProjectEditDialog;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedPanel;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaSortStringComparator;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;

// @i18n complete
public class OpenProjectOrLogbookDialog extends BaseDialog implements IItemListener {

	private static final long serialVersionUID = 8095338806279770826L;

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
    
    private JLabel projectName;
    private JLabel logbookName;
    private JLabel clubworkName;

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

        iniNorthPanel();
        
        JLabel label = new JLabel();
        label.setFont(label.getFont().deriveFont(Font.BOLD));;
        if (type == Type.project) {
            label.setText(International.getString("vorhandene Projekte"));
            clubworkName.setVisible(false);
        }
        if (type == Type.logbook) {
            label.setText(International.getString("vorhandene Fahrtenbücher"));
            clubworkName.setVisible(false);
        }
        if (type == Type.clubwork) {
            label.setText(International.getString("vorhandene Vereinsarbeitsbücher"));
            logbookName.setVisible(false);
        }
        int y=1;
        mainPanel.add(label, new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));

        list = new ItemTypeHtmlList("LIST", null, null, null, IItemType.TYPE_PUBLIC, null, label.getText());
        String[] actions = {
                International.getString("Öffnen"),
                International.getString("Einstellungen"),
                International.getString("Löschen")
        };
        list.setPopupActions(actions);
        list.registerItemListener(this);
        list.setFieldGrid(1, 5, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        list.setFieldSize(660, 400);
        list.setPadding(10, 10, 0, 10);
        list.displayOnGui(_parent, mainPanel, 0, y++,1.0,1.0);

        JButton newButton = new JButton();
        Mnemonics.setButton(this, newButton, International.getString("Neu"),
                BaseDialog.IMAGE_ADD);
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                newButton_actionPerformed(e);
            }
        });
        mainPanel.add(newButton, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 10), 0, 0));
        JButton openButton = new JButton();
        Mnemonics.setButton(this, openButton, International.getString("Öffnen"),
                BaseDialog.IMAGE_OPEN);
        openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openButton_actionPerformed(e);
            }
        });
        mainPanel.add(openButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 10), 0, 0));

        JButton configureButton = new JButton();
        Mnemonics.setButton(this, configureButton, International.getString("Einstellungen"),
                BaseDialog.IMAGE_CONFIGURE);
        configureButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                configureButton_actionPerformed(e);
            }
        });
        mainPanel.add(configureButton, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 10), 0, 0));

        JButton deleteButton = new JButton();
        Mnemonics.setButton(this, deleteButton, International.getString("Löschen"),
                BaseDialog.IMAGE_DELETE);
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteButton_actionPerformed(e);
            }
        });
        mainPanel.add(deleteButton, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 10), 0, 0));

        updateGui();
    }

    public void updateGui() {
        Hashtable<String,String> items = null;
        String currentItem=null;
        if (type == Type.project) {
            items = Project.getProjects();
             if (Daten.project != null) {
             	currentItem=Daten.project.getName();
             }
        }
        if (type == Type.logbook && Daten.project != null) {
            items = Daten.project.getLogbooks();
            if (Daten.project.getCurrentLogbook()!=null){
            	currentItem=Daten.project.getCurrentLogbook().getName();
            }
        }
        if(type == Type.clubwork && Daten.project != null) {
            items = Daten.project.getClubworks();
            if (Daten.project.getCurrentClubwork()!=null){
            	currentItem=Daten.project.getCurrentClubwork().getName();
            }
        }

        keys = items.keySet().toArray(new String[0]);
        Arrays.sort(keys,new EfaSortStringComparator());

        list.setValues(keys, items);
        
        //select open project, logbook or clubwork
        if (Daten.project != null && currentItem != null) {
        	try {
        		list.parseAndShowValue(currentItem);
        	} catch (Exception e) {
        		Logger.logdebug(e);
        	}
        }
        updateInfos();
        this.revalidate(); // refresh the whole screen
        this.repaint();
    }
    
    public void updateInfos() {
        projectName.setText(International.getString("Projekt") + ": "
                + (Daten.project != null ? Daten.project.getProjectName() : "- " + International.getString("Kein Projekt geöffnet.") + " -"));
        
        if (Daten.project != null) {
	        logbookName.setText(International.getString("Fahrtenbuch") + ": "
	                + (Daten.project.getCurrentLogbook() != null && Daten.project.getCurrentLogbook().isOpen()
	                ? Daten.project.getCurrentLogbook().getName() : "- " + International.getString("Kein Fahrtenbuch geöffnet.") + " -"));

	        clubworkName.setText(International.getString("Vereinsarbeitsbuch") + ": "
	                + (Daten.project.getCurrentClubwork() != null && Daten.project.getCurrentClubwork().isOpen()
	                ? Daten.project.getCurrentClubwork().getName() : "- " + International.getString("Kein Vereinsarbeitsbuch geöffnet.") + " -"));

        } else {
        	//no project open
	        logbookName.setText(International.getString("Fahrtenbuch") + ": " + International.getString("Kein Fahrtenbuch geöffnet.") + " -");

	        clubworkName.setText(International.getString("Vereinsarbeitsbuch") + ": " + International.getString("Kein Vereinsarbeitsbuch geöffnet.") + " -");
        }
        
        try {
        	if (Daten.project != null) {
        		//only update further information if we have an open project.
        		//use a second line when an automatic logbook or clubwork change is set up.        	
	            ProjectRecord r = Daten.project.getBoathouseRecord();
	            if (r.getAutoNewLogbookName() != null && r.getAutoNewLogbookName().length() > 0
	                    && r.getAutoNewLogbookDate() != null && r.getAutoNewLogbookDate().isSet()) {
	                logbookName.setText("<html><body><center>"+ EfaUtil.escapeHtml(logbookName.getText()) + "<br>"+EfaUtil.escapeHtml("["
	                        + International.getMessage("ab {timestamp}", r.getAutoNewLogbookDate().toString()) + ": "
	                        + r.getAutoNewLogbookName() + "]")+"</center></body></html>");
	            }
	            
	            if (r.getAutoNewClubworkName() != null && r.getAutoNewClubworkName().length() > 0
	                    && r.getAutoNewClubworkDate() != null && r.getAutoNewClubworkDate().isSet()) {
	                clubworkName.setText("<html><body><center>"+ EfaUtil.escapeHtml(clubworkName.getText()) + "<br>"+EfaUtil.escapeHtml("["
	                        + International.getMessage("ab {timestamp}", r.getAutoNewClubworkDate().toString()) + ": "
	                        + r.getAutoNewClubworkName() + "]")+"</center></body></html>");
	            }
        	}
        } catch (Exception eignore) {
            Logger.logdebug(eignore);
        }
        this.revalidate(); // refresh the whole screen
        this.repaint();
    }    
    
    private void iniNorthPanel() {
        JPanel northPanel = new RoundedPanel();
        northPanel.setBackground(Daten.efaConfig.getHeaderBackgroundColor());
        northPanel.setForeground(Daten.efaConfig.getHeaderForegroundColor());
        Border border = new RoundedBorder(northPanel.getForeground());
        northPanel.setBorder(border);
        northPanel.setLayout(new GridBagLayout());

        projectName = new JLabel();
        northPanel.add(projectName,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                        new Insets(4, 10, 4, 10), 0, 0));
        projectName.setFont(projectName.getFont().deriveFont(Font.BOLD));
        logbookName = new JLabel();
        northPanel.add(logbookName,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                        new Insets(4, 10, 4, 10), 0, 0));
        logbookName.setFont(logbookName.getFont().deriveFont(Font.BOLD));

        clubworkName = new JLabel();
        northPanel.add(clubworkName,
                new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                        new Insets(4, 10, 4, 10), 0, 0));
        clubworkName.setFont(clubworkName.getFont().deriveFont(Font.BOLD));

        projectName.setForeground(northPanel.getForeground());
        logbookName.setForeground(northPanel.getForeground());
        clubworkName.setForeground(northPanel.getForeground());
        
        
        updateInfos();
        mainPanel.add(northPanel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
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
                
                Logger.log(Logger.INFO, Logger.MSG_DATA_DELETE_OBJECT, International.getMessage("{typ} {name} wird von admin '{admin}' gelöscht.",
                		International.getString("Projekt"),
                		prj.getProjectName(),
                		admin.getName()));
                
                boolean success = false;
                if (Daten.project != null && Daten.project.getProjectName() != null &&
                        Daten.project.getProjectName().equals(prj.getProjectName())) {
                	// if the currently open project shall be deleted, close it first.
                	Daten.project.closeAllStorageObjects();
                	Daten.project=null;
                }
                
                success = prj.deleteProject();

                updateGui();
            } catch(Exception ex) {
                Dialog.error(ex.toString());
                Logger.logdebug(ex);
            }
        }

        if (type == Type.logbook) {
            try {
            	
                Logger.log(Logger.INFO, Logger.MSG_DATA_DELETE_OBJECT, International.getMessage("{typ} {name} wird von admin '{admin}' gelöscht.",
                		International.getString("Fahrtenbuch"),
                		name,
                		admin.getName()));
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
                Logger.log(Logger.INFO, Logger.MSG_DATA_DELETE_OBJECT, International.getMessage("{typ} {name} wird von admin '{admin}' gelöscht.",
                		International.getString("Vereinsarbeit"),
                		name,
                		admin.getName()));            	
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
    	if (EfaCloudUtil.isEfaCloudTXQueueActive() == false) {
    		showDialog();
    	}
        return name;
    }

}
