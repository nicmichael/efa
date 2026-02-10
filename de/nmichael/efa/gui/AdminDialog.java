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
import de.nmichael.efa.core.AdminTask;
import de.nmichael.efa.data.efacloud.TxRequestQueue;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.util.*;
import de.nmichael.efa.core.config.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class AdminDialog extends BaseDialog implements IItemListener {


	private static final long serialVersionUID = -4629950422212915082L;
	private EfaBoathouseFrame efaBoathouseFrame;
    private AdminRecord admin;
    private JLabel projectName;
    private JLabel logbookName;
    private JLabel clubworkName;
    private ItemTypeButton messageButton;

    public AdminDialog(EfaBoathouseFrame parent, AdminRecord admin) {
        super(parent, International.getStringWithMnemonic("Admin-Modus") + " [" + admin.getName() + "]", International.getStringWithMnemonic("Logout"));
        this.efaBoathouseFrame = parent;
        this.admin = admin;
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());
        iniNorthPanel();
        iniCenterPanel();
    }

    private void iniCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        JPanel menuFile = new JPanel();
        menuFile.setLayout(new GridBagLayout());
        JPanel menuAdministration = new JPanel();
        menuAdministration.setLayout(new GridBagLayout());
        JPanel menuConfiguration = new JPanel();
        menuConfiguration.setLayout(new GridBagLayout());
        JPanel menuOutput = new JPanel();
        menuOutput.setLayout(new GridBagLayout());
        JPanel menuInfo = new JPanel();
        menuInfo.setLayout(new GridBagLayout());
        JPanel menuDeveloper = new JPanel();
        menuDeveloper.setLayout(new GridBagLayout());
        JPanel panel = null;

        Vector<EfaMenuButton> menuButtons = EfaMenuButton.getAllMenuButtons(admin, true);
        String lastMenuName = null;
        int y = 0;
        int space = 0;
        String spaceForMenuName="";
        for (EfaMenuButton menuButton : menuButtons) {
            if (!menuButton.getMenuName().equals(lastMenuName)) {
                // New Menu
                panel = null;
                if (menuButton.getMenuName().equals(EfaMenuButton.MENU_FILE)) {
                    panel = menuFile;
                }
                if (menuButton.getMenuName().equals(EfaMenuButton.MENU_ADMINISTRATION)) {
                    panel = menuAdministration;
                }
                if (menuButton.getMenuName().equals(EfaMenuButton.MENU_MANAGEMENT)) {
                    panel = menuConfiguration;
                }
                if (menuButton.getMenuName().equals(EfaMenuButton.MENU_OUTPUT)) {
                    panel = menuOutput;
                }
                if (menuButton.getMenuName().equals(EfaMenuButton.MENU_INFO)) {
                    panel = menuInfo;
                }
                if (menuButton.getMenuName().equals(EfaMenuButton.MENU_DEVELOPMENT)) {
                    panel = menuDeveloper;
                }
                if (panel == null) {
                    continue;
                }
                JLabel label = new RoundedLabel();
                label.setText(Mnemonics.stripMnemonics(menuButton.getMenuText()));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBackground(Daten.efaConfig.getHeaderBackgroundColor());
                label.setForeground(Daten.efaConfig.getHeaderForegroundColor());
                label.setOpaque(true);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                label.setBorder(new RoundedBorder(label.getForeground()));
                
                y = 0;
                panel.add(label,
                        new GridBagConstraints(0, y++, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                new Insets(0, 0, 10, 0), 0, 0));
                lastMenuName = menuButton.getMenuName();
            }
            if (panel == null) {
                continue;
            }
            if (menuButton.getButtonName().equals(EfaMenuButton.SEPARATOR)) {
                space = 10;
                //memorize for which menu the spacer is intended.
                //because the whole menu is within a list, and it could be that 
                //a menu ends with a spacer (due to lacking competences of the current admin).
                spaceForMenuName=menuButton.getMenuName();
            } else {
                ItemTypeButton button = new ItemTypeButton(menuButton.getButtonName(),
                        IItemType.TYPE_PUBLIC, menuButton.getMenuName(), menuButton.getButtonText());
                if (menuButton.getIcon() != null) {
                    button.setIcon(menuButton.getIcon());
                }
                if (menuButton.getButtonName().equals(EfaMenuButton.BUTTON_MESSAGES)) {
                    updateMessageButton(button);
                }
                button.setFieldSize(200, 20);
                button.setHorizontalAlignment(SwingConstants.LEFT);
                button.registerItemListener(this);
                button.setBold(true);
            	//put some space above a button only if the spacer belongs
            	//to the same menu group as the current button.
                if ((space > 0) && (menuButton.getMenuName().equalsIgnoreCase(spaceForMenuName))){
                	button.setPadding(0, 0, space, 0);
                }
	            button.displayOnGui(this, panel, 0, y++);
                space = 0;
            }
        }

        // left side
        centerPanel.add(menuFile,
                new GridBagConstraints(0, 0, 1, 2, 0.0, 0.0,
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        new Insets(10, 10, 10, 10), 0, 0));
        if (Daten.efaConfig.getDeveloperFunctionsActivated()) {
            centerPanel.add(menuDeveloper,
                    new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            new Insets(10, 10, 10, 10), 0, 0));
        }

        // middle
        // menuAdministration has a lot of elements, it takes a full column.
        // if we stick to "CENTER" here, this will also lead to a VERTIAL centation
        // of the panel. But no, we want all topmost panels to be at the same height
        // for optical beauty.
        centerPanel.add(menuAdministration,
                new GridBagConstraints(1, 0, 1, 3, 0.0, 0.0,
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        new Insets(10, 10, 10, 10), 0, 0));

        // right
        centerPanel.add(menuConfiguration,
                new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        new Insets(10, 10, 10, 10), 0, 0));
        centerPanel.add(menuOutput,
                new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        new Insets(10, 10, 10, 10), 0, 0));
        centerPanel.add(menuInfo,
                new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                        new Insets(10, 10, 10, 10), 0, 0));

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        closeButton.setIcon(getIcon(IMAGE_LOGOUT));
        closeButton.setIconTextGap(10);
    }

    private void iniNorthPanel() {
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new GridBagLayout());

        projectName = new JLabel();
        northPanel.add(projectName,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(4, 10, 4, 10), 0, 0));
        projectName.setFont(projectName.getFont().deriveFont(Font.BOLD));
        logbookName = new JLabel();
        northPanel.add(logbookName,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(4, 10, 4, 10), 0, 0));
        logbookName.setFont(logbookName.getFont().deriveFont(Font.BOLD));

        clubworkName = new JLabel();
        northPanel.add(clubworkName,
                new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(4, 10, 4, 10), 0, 0));
        clubworkName.setFont(clubworkName.getFont().deriveFont(Font.BOLD));

        updateInfos();
        mainPanel.add(northPanel, BorderLayout.NORTH);
    }

    protected void preShowCallback() {
        AdminTask.startAdminTask(admin, this);
    }

    public void updateInfos() {
        projectName.setText(International.getString("Projekt") + ": "
                + (Daten.project != null ? Daten.project.getProjectName() : "- " + International.getString("Kein Projekt geöffnet.") + " -"));
        logbookName.setText(International.getString("Fahrtenbuch") + ": "
                + (efaBoathouseFrame.getLogbook() != null && efaBoathouseFrame.getLogbook().isOpen()
                ? efaBoathouseFrame.getLogbook().getName() : "- " + International.getString("Kein Fahrtenbuch geöffnet.") + " -"));
        if (efaBoathouseFrame.getClubwork() != null && efaBoathouseFrame.getClubwork().isOpen()) {
            clubworkName.setText(International.getString("Vereinsarbeitsbuch") + ": " + efaBoathouseFrame.getClubwork().getName());
        } else {
        	clubworkName.setText(International.getString("Kein Vereinsarbeitsbuch geöffnet."));
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
    }

    private void updateMessageButton(ItemTypeButton button) {
        if (button != null) {
            messageButton = button;
            messageButton.saveColor();
        }
        try {
        	long msg = 0;
            messageButton.setDescription(International.getString("Nachrichten"));
            messageButton.restoreColor();
        	if (Daten.project != null && Daten.project.isOpen()) {
        		msg = Daten.project.getMessages(false).countUnreadMessages();
	            if (msg > 0) {
	                messageButton.setDescription(International.getString("Nachrichten") + " (" + msg + ")");
	                messageButton.setColor(Color.red);
	            }
        	}
        } catch (Exception eignore) {
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    @Override
    public void closeButton_actionPerformed(ActionEvent e) {
    	if (Daten.project != null) {//project may be null, after restoring config data items from a backup file.
	        if ((Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD)
	                && (TxRequestQueue.getInstance() != null)) {
	            TxRequestQueue.getInstance().clearAdminCredentials();
	        }
    	}
        super.closeButton_actionPerformed(e);
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (event != null && event instanceof ActionEvent) {
            String action = itemType.getName();

            // now check permissions and perform the menu action
            boolean permission = EfaMenuButton.menuAction(this, action, admin, efaBoathouseFrame.getLogbook());

            if (action.equals(EfaMenuButton.BUTTON_EXIT) && permission) {

                efaBoathouseFrame.cancel(null, EfaBoathouseFrame.EFA_EXIT_REASON_USER, admin, EfaMenuButton.getLastBooleanValue());
                return;
            }

            // Projects and Logbooks are *not* handled within EfaMenuButton
            if (action.equals(EfaMenuButton.BUTTON_PROJECTS) && permission) {
                Project p = efaBoathouseFrame.openProject(admin);
                updateInfos();
                if (p == null) {
                    return;
                }
            }
            if ((action.equals(EfaMenuButton.BUTTON_PROJECTS) || action.equals(EfaMenuButton.BUTTON_LOGBOOKS)) && permission) {
                if (Daten.project == null) {
                    Dialog.error(International.getString("Kein Projekt geöffnet."));
                    return;
                }
                efaBoathouseFrame.openLogbook(admin);
                updateInfos();
                return;
            }

            if ((action.equals(EfaMenuButton.BUTTON_PROJECTS) || action.equals(EfaMenuButton.BUTTON_CLUBWORKBOOK)) && permission) {
                if (Daten.project == null) {
                    Dialog.error(International.getString("Kein Projekt geöffnet."));
                    return;
                }
                efaBoathouseFrame.openClubwork(admin);
                updateInfos();
                return;
            }

            if (action.equals(EfaMenuButton.BUTTON_BACKUP)) {
                // handled in EfaMenuButton; here we just need to update some infos
                if (efaBoathouseFrame.getLogbook() != null && !efaBoathouseFrame.getLogbook().isOpen()
                        &&               Daten.project != null && Daten.project.isOpen() &&
                        Daten.project.getCurrentLogbookEfaBoathouse() != null &&
                        Daten.project.getCurrentLogbookEfaBoathouse().length() > 0) {
                    efaBoathouseFrame.openLogbook(Daten.project.getCurrentLogbookEfaBoathouse());
                }
                updateInfos();
            }
            updateMessageButton(null);
            updateInfos();            
        }

    }

}
