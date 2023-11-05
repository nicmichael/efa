/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.util;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.Admins;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.RemoteCommand;
import de.nmichael.efa.data.sync.KanuEfbSyncTask;
import de.nmichael.efa.gui.*;
import de.nmichael.efa.gui.dataedit.*;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.Help;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.core.OnlineUpdate;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.ImageIcon;

public class EfaMenuButton {

	public final static String SEPARATOR                = "SEPARATOR";

    public final static String MENU_FILE                = "FILE";
    public final static String BUTTON_PROJECTS          = "PROJECTS";
    public final static String BUTTON_LOGBOOKS          = "LOGBOOKS";
    public final static String BUTTON_BACKUP            = "BACKUP";
    public final static String BUTTON_EFACLOUD          = "EFACLOUD";
    public final static String BUTTON_UPDATE            = "UPDATE";
    public final static String BUTTON_PLUGINS           = "PLUGINS";
    public final static String BUTTON_OSCOMMAND         = "OSCOMMAND";
    public final static String BUTTON_EXIT              = "EXIT";

    public final static String MENU_ADMINISTRATION      = "ADMINISTRATION";
    public final static String BUTTON_LOGBOOK           = "LOGBOOK";
    public final static String BUTTON_CLUBWORKBOOK      = "CLUBWORKBOOK";
    public final static String BUTTON_LOGBOOKLIST       = "LOGBOOKLIST";
    public final static String BUTTON_SESSIONGROUPS     = "SESSIONGROUPS";
    public final static String BUTTON_BOATS             = "BOATS";
    public final static String BUTTON_BOATSTATUS        = "BOATSTATUS";
    public final static String BUTTON_BOATRESERVATIONS  = "BOATRESERVATIONS";
    public final static String BUTTON_BOATDAMAGES       = "BOATDAMAGES";
    public final static String BUTTON_PERSONS           = "PERSONS";
    public final static String BUTTON_STATUS            = "STATUS";
    public final static String BUTTON_GROUPS            = "GROUPS";
    public final static String BUTTON_CREWS             = "CREWS";
    public final static String BUTTON_FAHRTENABZEICHEN  = "FAHRTENABZEICHEN";
    public final static String BUTTON_DESTINATIONS      = "DESTINATIONS";
    public final static String BUTTON_WATERS            = "WATERS";
    public final static String BUTTON_CLUBWORK          = "CLUBWORK";

    public final static String MENU_MANAGEMENT          = "MANAGEMENT";
    public final static String BUTTON_CONFIGURATION     = "CONFIGURATION";
    public final static String BUTTON_MESSAGES          = "MESSAGES";
    public final static String BUTTON_ADMINS            = "ADMINS";
    public final static String BUTTON_PASSWORD          = "PASSWORD";

    public final static String MENU_OUTPUT              = "OUTPUT";
    public final static String BUTTON_STATISTICS        = "STATISTICS";
    public final static String BUTTON_SYNCKANUEFB       = "SYNCKANUEFB";

    public final static String MENU_INFO                = "INFO";
    public final static String BUTTON_HELP              = "HELP";
    public final static String BUTTON_LOGFILE           = "LOGFILE";
    public final static String BUTTON_ABOUT             = "ABOUT";

    public final static String MENU_DEVELOPMENT         = "DEVELOPMENT";
    public final static String BUTTON_TRANSLATE         = "TRANSLATE";

    public enum MenuMode {
        all,
        efaBaseGui,
        efaBthsGui,
        efaBthsLogbookGui
    }

    private static Hashtable<String,String> actionMapping;
    private static boolean lastBooleanValue;

    private String menuName;
    private String menuText;
    private String buttonName;
    private String buttonText;
    private ImageIcon icon;

    public EfaMenuButton(String menuName, String buttonName, String menuText, String buttonText, ImageIcon icon) {
        this.menuName = menuName;
        this.buttonName = buttonName;
        this.menuText = menuText;
        this.buttonText = buttonText;
        this.icon = icon;
    }

    public String getMenuName() {
        return menuName;
    }

    public String getMenuText() {
        return menuText;
    }

    public String getButtonName() {
        return buttonName;
    }

    public String getButtonText() {
        return buttonText;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public boolean isSeparator() {
        return buttonName.equals(SEPARATOR);
    }

    public static synchronized Vector<EfaMenuButton> getAllMenuButtons(AdminRecord admin,
                                                                       boolean adminMode) {
        Vector<EfaMenuButton> v = new Vector<EfaMenuButton>();

        if (admin == null || admin.isAllowedAdministerProjectLogbook() ||
                Daten.applID == Daten.APPL_EFABASE) {
            v.add(new EfaMenuButton(MENU_FILE, BUTTON_PROJECTS,
                    International.getStringWithMnemonic("Datei"),
                    International.getStringWithMnemonic("Projekte") + " ...",
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_PROJECTS)));
        }
        if (admin == null || admin.isAllowedAdministerProjectLogbook()) {
            v.add(new EfaMenuButton(MENU_FILE, BUTTON_LOGBOOKS,
                    International.getStringWithMnemonic("Datei"),
                    International.getStringWithMnemonic("Fahrtenbücher") + " ...",
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_LOGBOOKS)));
        }
        if (admin == null || admin.isAllowedAdministerProjectLogbook()) {
            v.add(new EfaMenuButton(MENU_FILE, BUTTON_CLUBWORKBOOK,
                    International.getStringWithMnemonic("Datei"),
                    International.getStringWithMnemonic("Vereinsarbeitsbücher") + " ...",
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_CLUBWORKBOOKS)));
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_FILE) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_FILE, SEPARATOR,
                    null, null, null));
        }
        if ((admin == null || admin.isAllowedAdministerProjectLogbook()) && Daten.efaConfig.getExperimentalFunctionsActivated()) {
            v.add(new EfaMenuButton(MENU_FILE, BUTTON_EFACLOUD,
                    International.getStringWithMnemonic("Datei"),
                    International.getStringWithMnemonic("efaCloud"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_EFACLOUD)));
        }
        if (Daten.efaConfig.getValueUseFunctionalityCanoeingGermany()) {
            if (admin == null || admin.isAllowedSyncKanuEfb()) {
                v.add(new EfaMenuButton(MENU_FILE, BUTTON_SYNCKANUEFB,
                        International.getStringWithMnemonic("Datei"),
                        International.onlyFor("Mit Kanu-eFB synchronisieren", "de"),
                        BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_EFBSYNC)));
            }
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_FILE) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_FILE, SEPARATOR,
                    null, null, null));
        }
        if (admin == null || admin.isAllowedCreateBackup()) {
            v.add(new EfaMenuButton(MENU_FILE, BUTTON_BACKUP,
                    International.getStringWithMnemonic("Datei"),
                    International.getStringWithMnemonic("Backups"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_BACKUP)));
        }
        if (admin == null || admin.isAllowedUpdateEfa()) {
            v.add(new EfaMenuButton(MENU_FILE, BUTTON_UPDATE,
                    International.getStringWithMnemonic("Datei"),
                    International.getStringWithMnemonic("Online-Update"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_UPDATE)));
        }
        if (admin == null || admin.isAllowedUpdateEfa()) {
            v.add(new EfaMenuButton(MENU_FILE, BUTTON_PLUGINS,
                    International.getStringWithMnemonic("Datei"),
                    International.getStringWithMnemonic("Plugins"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_PLUGINS)));
        }
        if (admin == null || admin.isAllowedExecCommand()) {
            v.add(new EfaMenuButton(MENU_FILE, BUTTON_OSCOMMAND,
                    International.getStringWithMnemonic("Datei"),
                    International.getStringWithMnemonic("Kommando ausführen"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_COMMAND)));
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_FILE) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_FILE, SEPARATOR,
                    null, null, null));
        }
        if (admin == null || admin.isAllowedExitEfa() || !adminMode) {
            v.add(new EfaMenuButton(MENU_FILE, BUTTON_EXIT,
                    International.getStringWithMnemonic("Datei"),
                    International.getStringWithMnemonic("Beenden"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_EXIT)));
        }

        if (admin == null || (admin.isAllowedEditLogbook() && adminMode)) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_LOGBOOK,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Fahrtenbuch"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_LOGBOOK)));
        }
        if (admin == null || (admin.isAllowedEditLogbook() && adminMode)) { // we have the same menu again at the end for non-admin mode...
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_LOGBOOKLIST,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Fahrtenbuch") +
                            " (" + International.getString("Liste") + ")",
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_LOGBOOK_LIST)));
        }
        if (admin == null || admin.isAllowedEditLogbook() && adminMode) { // we have the same menu again at the end for non-admin mode...
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_SESSIONGROUPS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Fahrtgruppen"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_SESSIONGROUPS)));
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_ADMINISTRATION) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, SEPARATOR,
                    null, null, null));
        }
        if (admin == null || admin.isAllowedEditBoats()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_BOATS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Boote"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_BOATS)));
        }
        if (admin == null || admin.isAllowedEditBoatStatus()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_BOATSTATUS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Bootsstatus"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_BOATSTATUS)));
        }
        if (admin == null || admin.isAllowedEditBoatReservation()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_BOATRESERVATIONS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Bootsreservierungen"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_BOATRESERVATIONS)));
        }
        if (admin == null || admin.isAllowedEditBoatDamages()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_BOATDAMAGES,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Bootsschäden"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_BOATDAMAGES)));
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_ADMINISTRATION) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, SEPARATOR,
                    null, null, null));
        }
        if (admin == null || admin.isAllowedEditPersons()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_PERSONS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Personen"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_PERSONS)));
        }
        if (admin == null || admin.isAllowedEditPersons()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_STATUS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Status"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_STATUS)));
        }
        if (admin == null || admin.isAllowedEditGroups()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_GROUPS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Gruppen"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_GROUPS)));
        }
        if (admin == null || admin.isAllowedEditCrews()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_CREWS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Mannschaften"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_CREWS2)));
        }
        if (Daten.efaConfig.getValueUseFunctionalityRowingGermany()) {
            if (admin == null || admin.isAllowedEditFahrtenabzeichen()) {
                v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_FAHRTENABZEICHEN,
                        International.getStringWithMnemonic("Administration"),
                        International.onlyFor("Fahrtenabzeichen", "de"),
                        BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_FAHRTENABZEICHEN)));
            }
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_ADMINISTRATION) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, SEPARATOR,
                    null, null, null));
        }
        if (admin == null || admin.isAllowedEditDestinations()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_DESTINATIONS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Ziele") + " / " +
                            International.getString("Strecken"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_DESTINATIONS)));
        }
        if (admin == null || admin.isAllowedEditDestinations()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_WATERS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Gewässer"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_WATERS)));
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_ADMINISTRATION) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, SEPARATOR,
                    null, null, null));
        }
        if (admin == null || admin.isAllowedEditClubwork()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_CLUBWORK,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Vereinsarbeit"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_CLUBWORK)));
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_ADMINISTRATION) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, SEPARATOR,
                    null, null, null));
        }
        if (admin == null || (admin.isAllowedEditLogbook() && !adminMode)) { // we have the same menu again at the end for non-admin mode...
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_LOGBOOKLIST,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Fahrtenbuch") +
                            " (" + International.getString("Liste") + ")",
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_LOGBOOK_LIST)));
        }
        if (admin == null || admin.isAllowedEditLogbook() && !adminMode) { // we have the same menu again at the beginning for admin mode...
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, BUTTON_SESSIONGROUPS,
                    International.getStringWithMnemonic("Administration"),
                    International.getStringWithMnemonic("Fahrtgruppen"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_SESSIONGROUPS)));
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_ADMINISTRATION) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_ADMINISTRATION, SEPARATOR,
                    null, null, null));
        }
        if (admin == null || admin.isAllowedConfiguration()) {
            v.add(new EfaMenuButton(MENU_MANAGEMENT, BUTTON_CONFIGURATION,
                    International.getStringWithMnemonic("Verwaltung"),
                    International.getStringWithMnemonic("Konfiguration"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_CONFIGURATION)));
        }
        if (admin == null || admin.isAllowedMsgReadAdmin() || admin.isAllowedMsgReadBoatMaintenance()) {
            v.add(new EfaMenuButton(MENU_MANAGEMENT, BUTTON_MESSAGES,
                    International.getStringWithMnemonic("Verwaltung"),
                    International.getStringWithMnemonic("Nachrichten"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_MESSAGES)));
        }
        if (admin == null || admin.isAllowedEditAdmins()) {
            v.add(new EfaMenuButton(MENU_MANAGEMENT, BUTTON_ADMINS,
                    International.getStringWithMnemonic("Verwaltung"),
                    International.getStringWithMnemonic("Administratoren"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_ADMINS)));
        }
        if (admin == null || admin.isAllowedChangePassword()) {
            v.add(new EfaMenuButton(MENU_MANAGEMENT, BUTTON_PASSWORD,
                    International.getStringWithMnemonic("Verwaltung"),
                    International.getStringWithMnemonic("Paßwort ändern"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_PASSWORD)));
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_MANAGEMENT) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_MANAGEMENT, SEPARATOR,
                    null, null, null));
        }

        if (admin == null || admin.isAllowedEditStatistics()) {
            v.add(new EfaMenuButton(MENU_OUTPUT, BUTTON_STATISTICS,
                    International.getStringWithMnemonic("Ausgabe"),
                    International.getStringWithMnemonic("Statistiken"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_STATISTICS)));
        }

        if (Daten.efaConfig.getDeveloperFunctionsActivated() && admin != null && admin.isSuperAdmin()) {
            v.add(new EfaMenuButton(MENU_DEVELOPMENT, BUTTON_TRANSLATE,
                    International.getStringWithMnemonic("Entwicklung"),
                    International.getStringWithMnemonic("Übersetzen"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_TRANSLATE)));
        }

        v.add(new EfaMenuButton(MENU_INFO, BUTTON_HELP,
                International.getStringWithMnemonic("Info"),
                International.getStringWithMnemonic("Hilfe"),
                BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_HELP)));
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_INFO) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_INFO, SEPARATOR,
                    null, null, null));
        }
        if (admin == null || admin.isAllowedShowLogfile()) {
            v.add(new EfaMenuButton(MENU_INFO, BUTTON_LOGFILE,
                    International.getStringWithMnemonic("Info"),
                    International.getStringWithMnemonic("Logdatei"),
                    BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_LOGFILE)));
        }
        if (v.size() > 0 && v.get(v.size()-1).getMenuName().equals(MENU_INFO) && !v.get(v.size()-1).isSeparator()) {
            v.add(new EfaMenuButton(MENU_INFO, SEPARATOR,
                    null, null, null));
        }
        v.add(new EfaMenuButton(MENU_INFO, BUTTON_ABOUT,
                International.getStringWithMnemonic("Info"),
                International.getStringWithMnemonic("Über"),
                BaseFrame.getIcon(ImagesAndIcons.IMAGE_MENU_ABOUT)));

        if (actionMapping == null) {
            actionMapping = new Hashtable<String,String>();
        }
        for (EfaMenuButton b : v) {
            if (b.getButtonName() != null && b.getButtonText() != null) {
                actionMapping.put(b.getButtonName(), b.getButtonText());
            }
        }

        return v;
    }

    public static boolean menuAction(BaseFrame parent, String action, AdminRecord admin, Logbook logbook) {
        return menuAction(parent, null, action, admin, logbook);
    }

    public static boolean menuAction(BaseDialog parent, String action, AdminRecord admin, Logbook logbook) {
        return menuAction(null, parent, action, admin, logbook);
    }

    private static boolean menuAction(BaseFrame parentFrame, BaseDialog parentDialog, String action, AdminRecord admin, Logbook logbook) {
        if (action == null) {
            return false;
        }

        if (action.equals(BUTTON_PROJECTS)) {
            if (admin == null || (!admin.isAllowedAdministerProjectLogbook())) {
                if (Daten.applID == Daten.APPL_EFABASE) {
                    return true; // always allow for efaBase
                }
                insufficientRights(admin, action);
                return false;
            }
            return true; // Projects have to handled individually by the caller
        }

        if (action.equals(BUTTON_LOGBOOKS)) {
            if (admin == null || (!admin.isAllowedAdministerProjectLogbook())) {
                insufficientRights(admin, action);
                return false;
            }
            return true; // Logbooks have to handled individually by the caller
        }

        if (action.equals(BUTTON_CLUBWORKBOOK)) {
            if (admin == null || (!admin.isAllowedAdministerProjectClubwork())) {
                insufficientRights(admin, action);
                return false;
            }
            return true; // ClubworkBooks have to handled individually by the caller
        }

        if (action.equals(BUTTON_BACKUP)) {
            if (admin == null ||
                    (!admin.isAllowedCreateBackup() && !admin.isAllowedRestoreBackup())) {
                insufficientRights(admin, action);
                return false;
            }
            BackupDialog dlg = (parentFrame != null ?
                    new BackupDialog(parentFrame, admin) :
                    new BackupDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_EFACLOUD)) {
            if (admin == null || !admin.isAllowedAdministerProjectLogbook()) {
                insufficientRights(admin, action);
                return false;
            }
            // The only parent for this button is the EfaCloudSynchDialog.
            EfaCloudConfigDialog dlg = null;
            if (Daten.project == null)
                Dialog.infoDialog(International
                        .getString("Zur efaCloud-Konfiguration bitte erst ein Projekt öffnen."));
            try {
                dlg = new EfaCloudConfigDialog(parentDialog, admin);
            } catch (Exception ignored) {
            }
            if (dlg != null) dlg.showDialog();
            else Dialog.infoDialog(International
                    .getString("efaCloud-Konfiguration konnte nicht geöffnet werden."));
        }

        if (action.equals(BUTTON_UPDATE)) {
            if (admin == null || (!admin.isAllowedUpdateEfa())) {
                insufficientRights(admin, action);
                return false;
            }

            boolean remoteEfa = false;
            if (Daten.project != null && Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                switch (Dialog.auswahlDialog(International.getString("Online-Update"),
                        International.getString("Lokales oder entferntes efa aktualisieren?"),
                        International.getString("Lokal"),
                        International.getString("Remote"), true)) {
                    case 0:
                        break;
                    case 1:
                        remoteEfa = true;
                        break;
                    default:
                        return false;
                }
            }

            if (remoteEfa) {
                RemoteCommand cmd = new RemoteCommand(Daten.project);
                String result = cmd.onlineUpdate();
                if (result == null) {
                    Dialog.infoDialog(LogString.operationSuccessfullyCompleted(
                            International.getString("Online-Update")));
                } else {
                    Dialog.error(LogString.operationFailed(
                            International.getString("Online-Update"),
                            result));
                }
            } else {
                OnlineUpdate.runOnlineUpdate(parentDialog, Daten.ONLINEUPDATE_INFO);
            }
            return false; // nothing to do for caller of this method
        }

        if (action.equals(BUTTON_PLUGINS)) {
            if (admin == null || (!admin.isAllowedUpdateEfa())) {
                insufficientRights(admin, action);
                return false;
            }

            PluginDialog pdlg = new PluginDialog(parentDialog);
            pdlg.showDialog();
            return false; // nothing to do for caller of this method
        }

        if (action.equals(BUTTON_OSCOMMAND)) {
            if (admin == null || (!admin.isAllowedExecCommand())) {
                insufficientRights(admin, action);
                return false;
            }
            String cmd = Daten.efaConfig.getValueEfadirekt_adminLastOsCommand();
            if (cmd == null) {
                cmd = "";
            }
            ItemTypeString item = new ItemTypeString("CMD", cmd,
                    IItemType.TYPE_PUBLIC, "", International.getString("Kommando"));
            if ( (parentFrame != null ?
                    SimpleInputDialog.showInputDialog(parentFrame,
                            International.getString("Betriebssystemkommando ausführen"), item) :
                    SimpleInputDialog.showInputDialog(parentDialog,
                            International.getString("Betriebssystemkommando ausführen"), item) ) ) {

                cmd = item.getValueFromField().trim();
                Daten.efaConfig.setValueEfadirekt_adminLastOsCommand(cmd);
                Logger.log(Logger.INFO, Logger.MSG_ADMIN_ACTION_EXECCMD,
                        International.getMessage("Starte Kommando: {cmd}", cmd));
                try {
                    Runtime.getRuntime().exec(cmd);
                } catch (Exception ee) {
                    Logger.log(Logger.ERROR, Logger.MSG_ADMIN_ACTION_EXECCMDFAILED,
                            LogString.cantExecCommand(cmd, International.getString("Kommando")));
                }
            }
        }

        if (action.equals(BUTTON_EXIT)) {
            if (Daten.applID == Daten.APPL_EFABH &&  // check permissions only for efaBths; other programs may be exited by anyone
                    (admin == null || (!admin.isAllowedExitEfa()))) {
                insufficientRights(admin, action);
                return false;
            }

            boolean remoteEfa = false;
            if (Daten.project != null && Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                switch (Dialog.auswahlDialog(International.getString("Beenden"),
                        International.getString("Lokales oder entferntes efa beenden?"),
                        International.getString("Lokal"),
                        International.getString("Remote"), true)) {
                    case 0:
                        break;
                    case 1:
                        remoteEfa = true;
                        break;
                    default:
                        return false;
                }
            }

            boolean restart = false;
            String opName = null;
            if (remoteEfa || Daten.applID == Daten.APPL_EFABH) {
                switch (Dialog.auswahlDialog(International.getString("Beenden"),
                        International.getString("efa beenden oder neu starten?"),
                        International.getString("Beenden"),
                        International.getString("Neustart"), true)) {
                    case 0:
                        opName = International.getString("Beenden");
                        break;
                    case 1:
                        opName = International.getString("Neustart");
                        restart = true;
                        break;
                    default:
                        return false;
                }
            }

            if (remoteEfa) {
                RemoteCommand cmd = new RemoteCommand(Daten.project);
                boolean result = cmd.exitEfa(restart);
                if (result) {
                    Dialog.infoDialog(LogString.operationSuccessfullyCompleted(opName));
                } else {
                    Dialog.error(LogString.operationFailed(opName));
                }
                return false; // nothing to do for caller of this method
            }

            lastBooleanValue = restart;

        }

        if (action.equals(BUTTON_LOGBOOK)) {
            if (Daten.project == null || logbook == null) {
                Dialog.error(International.getString("Kein Fahrtenbuch geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditLogbook())) {
                insufficientRights(admin, action);
                return false;
            }
            EfaBaseFrame dlg = new EfaBaseFrame(parentDialog, EfaBaseFrame.MODE_ADMIN);
            dlg.setDataForAdminAction(logbook, admin, (AdminDialog)parentDialog);
            dlg.efaBoathouseShowEfaFrame();
        }

        if (action.equals(BUTTON_LOGBOOKLIST)) {
            if (Daten.project == null || logbook == null) {
                Dialog.error(International.getString("Kein Fahrtenbuch geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditLogbook())) {
                insufficientRights(admin, action);
                return false;
            }
            LogbookListDialog dlg = (parentFrame != null ?
                    new LogbookListDialog(parentFrame, admin, logbook) :
                    new LogbookListDialog(parentDialog, admin, logbook));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_SESSIONGROUPS)) {
            if (Daten.project == null || logbook == null) {
                Dialog.error(International.getString("Kein Fahrtenbuch geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditLogbook())) {
                insufficientRights(admin, action);
                return false;
            }
            SessionGroupListDialog dlg = (parentFrame != null ?
                    new SessionGroupListDialog(parentFrame, logbook.getName(), admin) :
                    new SessionGroupListDialog(parentDialog, logbook.getName(), admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_BOATS)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditBoats())) {
                insufficientRights(admin, action);
                return false;
            }
            BoatListDialog dlg = (parentFrame != null ?
                    new BoatListDialog(parentFrame, -1, admin) :
                    new BoatListDialog(parentDialog, -1, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_BOATSTATUS)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditBoatStatus())) {
                insufficientRights(admin, action);
                return false;
            }
            BoatStatusListDialog dlg = (parentFrame != null ?
                    new BoatStatusListDialog(parentFrame, admin) :
                    new BoatStatusListDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_BOATRESERVATIONS)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditBoatReservation())) {
                insufficientRights(admin, action);
                return false;
            }
            BoatReservationListDialog dlg = (parentFrame != null ?
                    new BoatReservationListDialog(parentFrame, admin) :
                    new BoatReservationListDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_BOATDAMAGES)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditBoatDamages())) {
                insufficientRights(admin, action);
                return false;
            }
            BoatDamageListDialog dlg = (parentFrame != null ?
                    new BoatDamageListDialog(parentFrame, admin) :
                    new BoatDamageListDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_PERSONS)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditPersons())) {
                insufficientRights(admin, action);
                return false;
            }
            PersonListDialog dlg = (parentFrame != null ?
                    new PersonListDialog(parentFrame, -1, admin) :
                    new PersonListDialog(parentDialog, -1, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_STATUS)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditPersons())) {
                insufficientRights(admin, action);
                return false;
            }
            StatusListDialog dlg = (parentFrame != null ?
                    new StatusListDialog(parentFrame, admin) :
                    new StatusListDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_GROUPS)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditGroups())) {
                insufficientRights(admin, action);
                return false;
            }
            GroupListDialog dlg = (parentFrame != null ?
                    new GroupListDialog(parentFrame, -1, admin) :
                    new GroupListDialog(parentDialog, -1, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_CREWS)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditCrews())) {
                insufficientRights(admin, action);
                return false;
            }
            CrewListDialog dlg = (parentFrame != null ?
                    new CrewListDialog(parentFrame, admin) :
                    new CrewListDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_FAHRTENABZEICHEN)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditFahrtenabzeichen())) {
                insufficientRights(admin, action);
                return false;
            }
            FahrtenabzeichenListDialog dlg = (parentFrame != null ?
                    new FahrtenabzeichenListDialog(parentFrame, admin) :
                    new FahrtenabzeichenListDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_DESTINATIONS)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditDestinations())) {
                insufficientRights(admin, action);
                return false;
            }
            DestinationListDialog dlg = (parentFrame != null ?
                    new DestinationListDialog(parentFrame, -1, admin) :
                    new DestinationListDialog(parentDialog, -1, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_WATERS)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditDestinations())) {
                insufficientRights(admin, action);
                return false;
            }
            WatersListDialog dlg = (parentFrame != null ?
                    new WatersListDialog(parentFrame, admin) :
                    new WatersListDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_CLUBWORK)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (Daten.project.getCurrentClubwork() == null) {
                Dialog.error(International.getString("Kein Vereinsarbeitsbuch geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditClubwork())) {
                insufficientRights(admin, action);
                return false;
            }
            ClubworkListDialog dlg = (parentFrame != null ?
                    new ClubworkListDialog(parentFrame, admin) :
                    new ClubworkListDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_CONFIGURATION)) {
            if (admin == null || (!admin.isAllowedConfiguration())) {
                insufficientRights(admin, action);
                return false;
            }
            EfaConfig myEfaConfig = Daten.efaConfig;
            if (Daten.project != null && Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                switch(Dialog.auswahlDialog(International.getString("efa-Konfiguration"),
                        International.getString("Lokale oder remote Konfiguration bearbeiten?"),
                        International.getString("Lokal"),
                        International.getString("Remote"),
                        true)) {
                    case 0:
                        break;
                    case 1:
                        myEfaConfig = new EfaConfig(Daten.project.getProjectStorageType(),
                                Daten.project.getProjectStorageLocation(),
                                Daten.project.getProjectStorageUsername(),
                                Daten.project.getProjectStoragePassword());
                        myEfaConfig.updateConfigValuesWithPersistence();
                        break;
                    default:
                        return false;
                }
            }

            EfaConfigDialog dlg = (parentFrame != null ? new EfaConfigDialog(parentFrame, myEfaConfig) : new EfaConfigDialog(parentDialog, myEfaConfig));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_ADMINS)) {
            if (admin == null || !admin.isAllowedEditAdmins()) {
                insufficientRights(admin, action);
                return false;
            }
            Admins myAdmins = Daten.admins;
            if (Daten.project != null && Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                switch(Dialog.auswahlDialog(International.getString("Administratoren"),
                        International.getString("Lokale oder remote Administratoren bearbeiten?"),
                        International.getString("Lokal"),
                        International.getString("Remote"),
                        true)) {
                    case 0:
                        break;
                    case 1:
                        myAdmins = new Admins(Daten.project.getProjectStorageType(),
                                Daten.project.getProjectStorageLocation(),
                                Daten.project.getProjectStorageUsername(),
                                Daten.project.getProjectStoragePassword());
                        break;
                    default:
                        return false;
                }
            }

            AdminListDialog dlg = (parentFrame != null ?
                    new AdminListDialog(parentFrame, myAdmins, admin) :
                    new AdminListDialog(parentDialog, myAdmins, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_PASSWORD)) {
            if (admin == null || (!admin.isAllowedChangePassword())) {
                insufficientRights(admin, action);
                return false;
            }
            AdminPasswordChangeDialog dlg = (parentFrame != null ? new AdminPasswordChangeDialog(parentFrame, admin) : new AdminPasswordChangeDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_STATISTICS)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedEditStatistics())) {
                insufficientRights(admin, action);
                return false;
            }
            StatisticsListDialog dlg = (parentFrame != null ? new StatisticsListDialog(parentFrame, admin) : new StatisticsListDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_SYNCKANUEFB)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (logbook == null) {
                Dialog.error(International.getString("Kein Fahrtenbuch geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedSyncKanuEfb())) {
                insufficientRights(admin, action);
                return false;
            }
            KanuEfbSyncTask syncTask = new KanuEfbSyncTask(logbook, admin, true);//always verbose mode when running sync in GUI
            ProgressDialog progressDialog = (parentFrame != null ?
                    new ProgressDialog(parentFrame, International.onlyFor("Mit Kanu-eFB synchronisieren", "de"), syncTask, false) :
                    new ProgressDialog(parentDialog, International.onlyFor("Mit Kanu-eFB synchronisieren", "de"), syncTask, false) );
            syncTask.startSynchronization(progressDialog);
        }

        if (action.equals(BUTTON_HELP)) {
            Help.showHelp((parentFrame != null ? parentFrame.getHelpTopics() : parentDialog.getHelpTopics()));
        }

        if (action.equals(BUTTON_MESSAGES)) {
            if (Daten.project == null) {
                Dialog.error(International.getString("Kein Projekt geöffnet."));
                return false;
            }
            if (admin == null || (!admin.isAllowedMsgReadAdmin() && !admin.isAllowedMsgReadBoatMaintenance())) {
                insufficientRights(admin, action);
                return false;
            }
            MessageListDialog dlg = (parentFrame != null ? new MessageListDialog(parentFrame, admin) : new MessageListDialog(parentDialog, admin));
            dlg.showDialog();
        }

        if (action.equals(BUTTON_LOGFILE)) {
            if (admin == null || (!admin.isAllowedShowLogfile())) {
                insufficientRights(admin, action);
                return false;
            }
            LogViewDialog dlg = (parentFrame != null ? new LogViewDialog(parentFrame) : new LogViewDialog(parentDialog));
            dlg.showDialog();
        }
        if (action.equals(BUTTON_TRANSLATE)) {
            if (admin == null || (!admin.isSuperAdmin())) {
                insufficientRights(admin, action);
                return false;
            }
            if (parentFrame != null) {
                TranslateDialog.openTranslateDialog(parentFrame);
            } else {
                TranslateDialog.openTranslateDialog(parentDialog);
            }
        }

        if (action.equals(BUTTON_ABOUT)) {
            EfaAboutDialog dlg = (parentFrame != null ? new EfaAboutDialog(parentFrame) : new EfaAboutDialog(parentDialog));
            dlg.showDialog();
        }

        return true;
    }

    public static String insufficientRights(AdminRecord admin, String action) {
        String actionText = (actionMapping != null ? actionMapping.get(action) : action);
        if (actionText == null) {
            actionText = action;
        }
        String msg = International.getMessage("Du hast als {user} nicht die Berechtigung, um die Funktion '{function}' auszuführen.",
                (admin != null ?
                        International.getString("Admin") + " '" + admin.getName() + "'" :
                        International.getString("normaler Nutzer")),
                actionText
        );
        if (Daten.isGuiAppl()) {
            Dialog.error(msg);
        }
        return msg;
    }

    public static boolean getLastBooleanValue() {
        return lastBooleanValue;
    }
}
