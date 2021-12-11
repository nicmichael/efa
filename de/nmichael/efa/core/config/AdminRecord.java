/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.config;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.types.DataTypePasswordHashed;
import de.nmichael.efa.gui.AdminPasswordChangeDialog;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.util.*;
import java.awt.AWTEvent;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.util.*;
import javax.swing.JDialog;

// @i18n complete

public class AdminRecord extends DataRecord implements IItemListener {

    public static final int MIN_PASSWORD_LENGTH = 6;

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String NAME                  = "Name";
    public static final String PASSWORD              = "Password";
    public static final String EMAIL                 = "Email";
    public static final String EDITADMINS            = "EditAdmins";
    public static final String CHANGEPASSWORD        = "ChangePassword";
    public static final String CONFIGURATION         = "Configuration";
    public static final String ADMINPROJECTLOGBOOK   = "AdministerProjectLogbook";
    public static final String ADMINPROJECTCLUBWORK  = "AdministerProjectClubwork";
    public static final String EDITLOGBOOK           = "EditLogbook";
    public static final String EDITBOATSTATUS        = "EditBoatStatus";
    public static final String EDITBOATRESERVATION   = "EditBoatReservation";
    public static final String EDITBOATDAMAGES       = "EditBoatDamages";
    public static final String EDITBOATS             = "EditBoats";
    public static final String EDITPERSONS           = "EditPersons";
    public static final String EDITCLUBWORK          = "EditClubwork";
    public static final String EDITDESTINATIONS      = "EditDestinations";
    public static final String EDITGROUPS            = "EditGroups";
    public static final String EDITCREWS             = "EditCrews";
    public static final String EDITFAHRTENABZEICHEN  = "EditFahrtenabzeichen";
    public static final String MSGREADADMIN          = "MsgReadAdmin";
    public static final String MSGREADBOATMAINT      = "MsgReadBoatMaintenance";
    public static final String MSGMARKREADADMIN      = "MsgMarkReadAdmin";
    public static final String MSGMARKREADBOATMAINT  = "MsgMarkReadBoatMaintenance";
    public static final String MSGAUTOREADADMIN      = "MsgAutoMarkReadAdmin";
    public static final String MSGAUTOREADBOATMAINT  = "MsgAutoMarkReadBoatMaintenance";
    public static final String EDITSTATISTICS        = "EditStatistics";
    public static final String ADVANCEDEDIT          = "AdvancedEdit";
    public static final String REMOTEACCESS          = "RemoteAccess";
    public static final String SYNCKANUEFB           = "SyncKanuEfb";
    public static final String SHOWLOGFILE           = "ShowLogfile";
    public static final String EXITEFA               = "ExitEfa";
    public static final String LOCKEFA               = "LockEfa";
    public static final String UPDATEEFA             = "UpdateEfa";
    public static final String EXECCOMMAND           = "ExecCommand";
    public static final String BACKUP                = "Backup";
    public static final String RESTORE               = "Restore";

    // from efa 2.3.1 onwards admin privileges will be managed at the cloud server side using two 32 bit bitmaps
    // They are mapped to the local flags using the sequence as defined below. You MUST NOT CHANGE THE SEQUENCE.
    private static final String[] workflowNames = new String[] {
            EDITADMINS,
            CHANGEPASSWORD,
            ADMINPROJECTLOGBOOK,
            ADMINPROJECTCLUBWORK,
            EDITLOGBOOK,
            EDITBOATS,
            EDITBOATSTATUS,
            EDITBOATRESERVATION,
            EDITBOATDAMAGES,
            EDITPERSONS,
            EDITCLUBWORK,
            EDITGROUPS,
            EDITCREWS,
            EDITFAHRTENABZEICHEN,
            EDITDESTINATIONS,
            ADVANCEDEDIT,
            CONFIGURATION,
            EDITSTATISTICS,
            SYNCKANUEFB,
            REMOTEACCESS,
            SHOWLOGFILE,
            EXITEFA,
            LOCKEFA,
            BACKUP,
            RESTORE,
            UPDATEEFA,
            EXECCOMMAND
    };
    private static final String[] concessionNames = new String[] {
            MSGREADADMIN,
            MSGREADBOATMAINT,
            MSGMARKREADADMIN,
            MSGMARKREADBOATMAINT,
            MSGAUTOREADADMIN,
            MSGAUTOREADBOATMAINT
    };

    private boolean isRemoteAdmin = false;

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(NAME);                              t.add(IDataAccess.DATA_STRING);
        f.add(PASSWORD);                          t.add(IDataAccess.DATA_PASSWORDH);
        f.add(EMAIL);                             t.add(IDataAccess.DATA_STRING);
        f.add(EDITADMINS);                        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(CHANGEPASSWORD);                    t.add(IDataAccess.DATA_BOOLEAN);
        f.add(CONFIGURATION);                     t.add(IDataAccess.DATA_BOOLEAN);
        f.add(ADMINPROJECTLOGBOOK);               t.add(IDataAccess.DATA_BOOLEAN);
        f.add(ADMINPROJECTCLUBWORK);              t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITLOGBOOK);                       t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITBOATSTATUS);                    t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITBOATRESERVATION);               t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITBOATDAMAGES);                   t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITBOATS);                         t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITPERSONS);                       t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITCLUBWORK);                      t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITDESTINATIONS);                  t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITGROUPS);                        t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITCREWS);                         t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITFAHRTENABZEICHEN);              t.add(IDataAccess.DATA_BOOLEAN);
        f.add(MSGREADADMIN);                      t.add(IDataAccess.DATA_BOOLEAN);
        f.add(MSGREADBOATMAINT);                  t.add(IDataAccess.DATA_BOOLEAN);
        f.add(MSGMARKREADADMIN);                  t.add(IDataAccess.DATA_BOOLEAN);
        f.add(MSGMARKREADBOATMAINT);              t.add(IDataAccess.DATA_BOOLEAN);
        f.add(MSGAUTOREADADMIN);                  t.add(IDataAccess.DATA_BOOLEAN);
        f.add(MSGAUTOREADBOATMAINT);              t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EDITSTATISTICS);                    t.add(IDataAccess.DATA_BOOLEAN);
        f.add(ADVANCEDEDIT);                      t.add(IDataAccess.DATA_BOOLEAN);
        f.add(REMOTEACCESS);                      t.add(IDataAccess.DATA_BOOLEAN);
        f.add(SYNCKANUEFB);                       t.add(IDataAccess.DATA_BOOLEAN);
        f.add(SHOWLOGFILE);                       t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EXITEFA);                           t.add(IDataAccess.DATA_BOOLEAN);
        f.add(LOCKEFA);                           t.add(IDataAccess.DATA_BOOLEAN);
        f.add(UPDATEEFA);                         t.add(IDataAccess.DATA_BOOLEAN);
        f.add(EXECCOMMAND);                       t.add(IDataAccess.DATA_BOOLEAN);
        f.add(BACKUP);                            t.add(IDataAccess.DATA_BOOLEAN);
        f.add(RESTORE);                           t.add(IDataAccess.DATA_BOOLEAN);
        MetaData metaData = constructMetaData(Admins.DATATYPE, f, t, false);
        metaData.setKey(new String[] { NAME });
    }

    public AdminRecord(Admins admins, MetaData metaData) {
        super(admins, metaData);
    }

    public AdminRecord(Admins admins, EfaCloudUserRecord ecr) {
        super(admins, MetaData.getMetaData(Admins.DATATYPE));
        setName(ecr.getAdminName());
        setEmail(ecr.getEmail());
        mapEfaCloudWorkflowsAndConcessions(ecr.getWorkflows(), ecr.getConcessions(), ecr.getRole());
        makeSurePermissionsAreCorrect();
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public DataKey getKey() {
        return new DataKey<String,String,String>(getName(),null,null);
    }

    public static DataKey getKey(String name) {
        return new DataKey<String,String,String>(name,null,null);
    }

    protected void setName(String name) {
        setString(NAME, name);
    }
    public String getName() {
        return getString(NAME);
    }

    public void setPassword(String password) {
        setPasswordHashed(PASSWORD, password);
    }
    public DataTypePasswordHashed getPassword() {
        return getPasswordHashed(PASSWORD);
    }

    public void setEmail(String email) {
        setString(EMAIL, email);
    }
    public String getEmail() {
        return getString(EMAIL);
    }

    public void setAllowedEditAdmins(boolean allowed) {
        setBool(EDITADMINS, allowed);
    }
    public Boolean isAllowedEditAdmins() {
        return getBool(EDITADMINS);
    }

    public void setAllowedChangePassword(boolean allowed) {
        setBool(CHANGEPASSWORD, allowed);
    }
    public Boolean isAllowedChangePassword() {
        return getBool(CHANGEPASSWORD);
    }

    public void setAllowedConfiguration(boolean allowed) {
        setBool(CONFIGURATION, allowed);
    }
    public Boolean isAllowedConfiguration() {
        return getBool(CONFIGURATION);
    }

    public void setAllowedAdministerProjectLogbook(boolean allowed) {
        setBool(ADMINPROJECTLOGBOOK, allowed);
    }
    public Boolean isAllowedAdministerProjectLogbook() {
        return getBool(ADMINPROJECTLOGBOOK);
    }

    public void setAllowedEditLogbook(boolean allowed) {
        setBool(EDITLOGBOOK, allowed);
    }
    public Boolean isAllowedEditLogbook() {
        return getBool(EDITLOGBOOK);
    }

    public void setAllowedEditBoatStatus(boolean allowed) {
        setBool(EDITBOATSTATUS, allowed);
    }
    public Boolean isAllowedEditBoatStatus() {
        return getBool(EDITBOATSTATUS);
    }

    public void setAllowedEditBoatReservation(boolean allowed) {
        setBool(EDITBOATRESERVATION, allowed);
    }
    public Boolean isAllowedEditBoatReservation() {
        return getBool(EDITBOATRESERVATION);
    }

    public void setAllowedEditBoatDamages(boolean allowed) {
        setBool(EDITBOATDAMAGES, allowed);
    }
    public Boolean isAllowedEditBoatDamages() {
        return getBool(EDITBOATDAMAGES);
    }

    public void setAllowedEditBoats(boolean allowed) {
        setBool(EDITBOATS, allowed);
    }
    public Boolean isAllowedEditBoats() {
        return getBool(EDITBOATS);
    }

    public void setAllowedEditPersons(boolean allowed) {
        setBool(EDITPERSONS, allowed);
    }
    public Boolean isAllowedEditPersons() {
        return getBool(EDITPERSONS);
    }

    public void setAllowedAdministerProjectClubwork(boolean allowed) {
        setBool(ADMINPROJECTCLUBWORK, allowed);
    }

    public Boolean isAllowedAdministerProjectClubwork() {
        return getBool(ADMINPROJECTCLUBWORK);
    }
    
    public void setAllowedEditClubwork(boolean allowed) {
        setBool(EDITCLUBWORK, allowed);
    }
    public Boolean isAllowedEditClubwork() {
        return getBool(EDITCLUBWORK);
    }

    public void setAllowedEditDestinations(boolean allowed) {
        setBool(EDITDESTINATIONS, allowed);
    }
    public Boolean isAllowedEditDestinations() {
        return getBool(EDITDESTINATIONS);
    }

    public void setAllowedEditGroups(boolean allowed) {
        setBool(EDITGROUPS, allowed);
    }
    public Boolean isAllowedEditGroups() {
        return getBool(EDITGROUPS);
    }

    public void setAllowedEditCrews(boolean allowed) {
        setBool(EDITCREWS, allowed);
    }
    public Boolean isAllowedEditCrews() {
        return getBool(EDITCREWS);
    }

    public void setAllowedEditFahrtenabzeichen(boolean allowed) {
        setBool(EDITFAHRTENABZEICHEN, allowed);
    }
    public Boolean isAllowedEditFahrtenabzeichen() {
        return getBool(EDITFAHRTENABZEICHEN);
    }

    public void setAllowedMsgReadAdmin(boolean allowed) {
        setBool(MSGREADADMIN, allowed);
    }
    public Boolean isAllowedMsgReadAdmin() {
        return getBool(MSGREADADMIN);
    }

    public void setAllowedMsgReadBoatMaintenance(boolean allowed) {
        setBool(MSGREADBOATMAINT, allowed);
    }
    public Boolean isAllowedMsgReadBoatMaintenance() {
        return getBool(MSGREADBOATMAINT);
    }

    public void setAllowedMsgMarkReadAdmin(boolean allowed) {
        setBool(MSGMARKREADADMIN, allowed);
    }
    public Boolean isAllowedMsgMarkReadAdmin() {
        return getBool(MSGMARKREADADMIN);
    }

    public void setAllowedMsgMarkReadBoatMaintenance(boolean allowed) {
        setBool(MSGMARKREADBOATMAINT, allowed);
    }
    public Boolean isAllowedMsgMarkReadBoatMaintenance() {
        return getBool(MSGMARKREADBOATMAINT);
    }

    public void setAllowedMsgAutoMarkReadAdmin(boolean allowed) {
        setBool(MSGAUTOREADADMIN, allowed);
    }
    public Boolean isAllowedMsgAutoMarkReadAdmin() {
        return getBool(MSGAUTOREADADMIN);
    }

    public void setAllowedMsgAutoMarkReadBoatMaintenance(boolean allowed) {
        setBool(MSGAUTOREADBOATMAINT, allowed);
    }
    public Boolean isAllowedMsgAutoMarkReadBoatMaintenance() {
        return getBool(MSGAUTOREADBOATMAINT);
    }

    public void setAllowedEditStatistics(boolean allowed) {
        setBool(EDITSTATISTICS, allowed);
    }
    public Boolean isAllowedEditStatistics() {
        return getBool(EDITSTATISTICS);
    }

    public void setAllowedAdvancedEdit(boolean allowed) {
        setBool(ADVANCEDEDIT, allowed);
    }
    public Boolean isAllowedAdvancedEdit() {
        return getBool(ADVANCEDEDIT);
    }

    public void setAllowedRemoteAccess(boolean allowed) {
        setBool(REMOTEACCESS, allowed);
    }
    public Boolean isAllowedRemoteAccess() {
        return getBool(REMOTEACCESS);
    }

    public void setAllowedSyncKanuEfb(boolean allowed) {
        setBool(SYNCKANUEFB, allowed);
    }
    public Boolean isAllowedSyncKanuEfb() {
        return getBool(SYNCKANUEFB);
    }

    public void setAllowedShowLogfile(boolean allowed) {
        setBool(SHOWLOGFILE, allowed);
    }
    public Boolean isAllowedShowLogfile() {
        return getBool(SHOWLOGFILE);
    }

    public void setAllowedExitEfa(boolean allowed) {
        setBool(EXITEFA, allowed);
    }
    public Boolean isAllowedExitEfa() {
        return getBool(EXITEFA);
    }

    public void setAllowedLockEfa(boolean allowed) {
        setBool(LOCKEFA, allowed);
    }
    public Boolean isAllowedLockEfa() {
        return getBool(LOCKEFA);
    }

    public void setAllowedCreateBackup(boolean allowed) {
        setBool(BACKUP, allowed);
    }
    public Boolean isAllowedCreateBackup() {
        return getBool(BACKUP);
    }

    public void setAllowedRestoreBackup(boolean allowed) {
        setBool(RESTORE, allowed);
    }
    public Boolean isAllowedRestoreBackup() {
        return getBool(RESTORE);
    }

    public void setAllowedUpdateEfa(boolean allowed) {
        setBool(UPDATEEFA, allowed);
    }
    public Boolean isAllowedUpdateEfa() {
        return getBool(UPDATEEFA);
    }

    public void setAllowedExecCommand(boolean allowed) {
        setBool(EXECCOMMAND, allowed);
    }
    public Boolean isAllowedExecCommand() {
        return getBool(EXECCOMMAND);
    }

    public void mapEfaCloudWorkflowsAndConcessions(int workflows, int concessions, String role) {
        HashMap<String, Integer> roleToWorkflow = new HashMap<String, Integer>();
        if (role.equalsIgnoreCase("board")) {
            workflows = workflows | 25187808;   // some selected
            concessions = concessions | 7;      // only for messages to admin
        }
        else
            if (role.equalsIgnoreCase("admin")) {
                workflows = workflows | 134217727;  // just all
                concessions = concessions | 63;     // just all
            }
        int flag = 1;
        for (String workflowName : workflowNames) {
            setBool(workflowName, ((flag & workflows) > 0));
            flag = flag * 2;
        }
        flag = 1;
        for (String concessionName : concessionNames) {
            setBool(concessionName, ((flag & concessions) > 0));
            flag = flag * 2;
        }
    }

    public String[] getQualifiedNameFields() {
        return new String[] { NAME };
    }

    public String getQualifiedName() {
        return getName();
    }

    public void makeSurePermissionsAreCorrect() {
        boolean changed = false;
        if (getName() != null && getName().equals(Admins.SUPERADMIN)) {
            if (!isAllowedEditAdmins()
                    || !isAllowedChangePassword()
                    || !isAllowedConfiguration()
                    || !isAllowedAdministerProjectLogbook()
                    || !isAllowedAdministerProjectClubwork()
                    || !isAllowedEditLogbook()
                    || !isAllowedEditBoatStatus()
                    || !isAllowedEditBoatReservation()
                    || !isAllowedEditBoatDamages()
                    || !isAllowedEditBoats()
                    || !isAllowedEditPersons()
                    || !isAllowedEditClubwork()
                    || !isAllowedEditDestinations()
                    || !isAllowedEditGroups()
                    || !isAllowedEditCrews()
                    || !isAllowedEditFahrtenabzeichen()
                    || !isAllowedMsgReadAdmin()
                    || !isAllowedMsgReadBoatMaintenance()
                    || !isAllowedMsgMarkReadAdmin()
                    || !isAllowedMsgMarkReadBoatMaintenance()
                    || !isAllowedEditStatistics()
                    || !isAllowedAdvancedEdit()
                    || !isAllowedRemoteAccess()
                    || !isAllowedSyncKanuEfb()
                    || !isAllowedShowLogfile()
                    || !isAllowedExitEfa()
                    || !isAllowedLockEfa()
                    || !isAllowedUpdateEfa()
                    || !isAllowedExecCommand()
                    || !isAllowedCreateBackup()
                    || !isAllowedRestoreBackup()) {
                setAllowedEditAdmins(true);
                setAllowedChangePassword(true);
                setAllowedConfiguration(true);
                setAllowedAdministerProjectLogbook(true);
                setAllowedAdministerProjectClubwork(true);
                setAllowedEditLogbook(true);
                setAllowedEditBoatStatus(true);
                setAllowedEditBoatReservation(true);
                setAllowedEditBoatDamages(true);
                setAllowedEditBoats(true);
                setAllowedEditPersons(true);
                setAllowedEditClubwork(true);
                setAllowedEditDestinations(true);
                setAllowedEditGroups(true);
                setAllowedEditCrews(true);
                setAllowedEditFahrtenabzeichen(true);
                setAllowedMsgReadAdmin(true);
                setAllowedMsgReadBoatMaintenance(true);
                setAllowedMsgMarkReadAdmin(true);
                setAllowedMsgMarkReadBoatMaintenance(true);
                setAllowedEditStatistics(true);
                setAllowedAdvancedEdit(true);
                setAllowedRemoteAccess(true);
                setAllowedSyncKanuEfb(true);
                setAllowedShowLogfile(true);
                setAllowedExitEfa(true);
                setAllowedLockEfa(true);
                setAllowedUpdateEfa(true);
                setAllowedExecCommand(true);
                setAllowedCreateBackup(true);
                setAllowedRestoreBackup(true);
                changed = true;
            }
        } else {
            if (isAllowedEditAdmins()) {
                setAllowedEditAdmins(false);
                changed = true;
            }
        }
        if (changed && getPersistence() != null && getPersistence().data() != null) {
            try {
                getPersistence().data().update(this);
            } catch(Exception eignore) {
                Logger.logdebug(eignore);
            }
        }
    }

    public boolean isSuperAdmin() {
        return getName() != null && getName().equals(Admins.SUPERADMIN);
    }

    public boolean isRemoteAdminRecord() {
        return isRemoteAdmin || this.getPersistence().data().getStorageType() == IDataAccess.TYPE_EFA_REMOTE;
    }

    public void setRemoteAdminRecord(boolean isRemoteAdmin) {
        this.isRemoteAdmin = isRemoteAdmin;
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        makeSurePermissionsAreCorrect();
        
        String CAT_BASEDATA     = "%01%" + International.getString("Administrator");
        String CAT_PERMISSIONS  = "%02%" + International.getString("Berechtigungen");
        String CAT_MESSAGES     = "%03%" + International.getString("Nachrichten");
        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();

        v.add(item = new ItemTypeString(NAME, getName(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Name")));
        item.setEnabled(getName() == null || getName().length() == 0);
        ((ItemTypeString)item).setNotNull(true);
        ((ItemTypeString)item).setToLowerCase(true);
        ((ItemTypeString)item).setAllowedCharacters("abcdefghijklmnopqrstuvwxyz1234567890");
        if (getPassword() != null && getPassword().isSet()) {
            v.add(item = new ItemTypeButton("PASSWORDBUTTON",
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Paßwort ändern")));
            ((ItemTypeButton)item).setFieldGrid(2, GridBagConstraints.EAST, GridBagConstraints.NONE);
            ((ItemTypeButton)item).registerItemListener(this);
        } else {
            v.add(item = new ItemTypePassword(PASSWORD, "", 
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Paßwort")));
            ((ItemTypePassword)item).setNotNull(true);
            ((ItemTypePassword)item).setMinCharacters(MIN_PASSWORD_LENGTH);
            v.add(item = new ItemTypePassword(PASSWORD + "_REPEAT", "", 
                    IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Paßwort") +
                    " (" + International.getString("Wiederholung") + ")"));
            ((ItemTypePassword)item).setNotNull(true);
            ((ItemTypePassword)item).setMinCharacters(MIN_PASSWORD_LENGTH);
        }
        v.add(item = new ItemTypeString(EMAIL, getEmail(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("email-Adresse")));

        v.add(item = new ItemTypeBoolean(EDITADMINS, isAllowedEditAdmins(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Admins verwalten")));
        ((ItemTypeBoolean)item).setEnabled(false); // no one can ever change this: Super-Admin is always allowed, all others are not
        v.add(item = new ItemTypeBoolean(CHANGEPASSWORD, isAllowedChangePassword(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Paßwort ändern")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(ADMINPROJECTLOGBOOK, isAllowedAdministerProjectLogbook(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Projekte und Fahrtenbücher administrieren")));
		((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
		v.add(item = new ItemTypeBoolean(ADMINPROJECTCLUBWORK, isAllowedAdministerProjectClubwork(),
        IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Vereinsarbeitsbücher administrieren")));
		((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EDITLOGBOOK, isAllowedEditLogbook(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Fahrtenbuch bearbeiten")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EDITBOATS, isAllowedEditBoats(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Boote bearbeiten")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EDITBOATSTATUS, isAllowedEditBoatStatus(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Bootsstatus bearbeiten")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EDITBOATRESERVATION, isAllowedEditBoatReservation(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Bootsreservierungen bearbeiten")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EDITBOATDAMAGES, isAllowedEditBoatDamages(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Bootsschäden bearbeiten")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EDITPERSONS, isAllowedEditPersons(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Personen und Status bearbeiten")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EDITCLUBWORK, isAllowedEditClubwork(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Vereinsarbeit bearbeiten")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EDITGROUPS, isAllowedEditGroups(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Gruppen bearbeiten")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EDITCREWS, isAllowedEditCrews(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Mannschaften bearbeiten")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        if (Daten.efaConfig.getValueUseFunctionalityRowingGermany()) {
            v.add(item = new ItemTypeBoolean(EDITFAHRTENABZEICHEN, isAllowedEditFahrtenabzeichen(),
                    IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.onlyFor("Fahrtenabzeichen bearbeiten","de")));
            ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        }
        v.add(item = new ItemTypeBoolean(EDITDESTINATIONS, isAllowedEditDestinations(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Ziele und Gewässer bearbeiten")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(ADVANCEDEDIT, isAllowedAdvancedEdit(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Erweiterte Bearbeitungsfunktionen") + ": " +
                International.getString("Import") + " && " +
                International.getString("Bearbeitungsassistent")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(CONFIGURATION, isAllowedConfiguration(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("efa konfigurieren")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EDITSTATISTICS, isAllowedEditStatistics(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Statistiken erstellen")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(REMOTEACCESS, isAllowedRemoteAccess(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Remote-Zugriff über efaRemote")));
        ((ItemTypeBoolean) item).setEnabled(!isSuperAdmin());
        if (Daten.efaConfig.getValueUseFunctionalityCanoeingGermany()) {
            v.add(item = new ItemTypeBoolean(SYNCKANUEFB, isAllowedSyncKanuEfb(),
                    IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.onlyFor("mit KanuEfb synchonisieren","de")));
            ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        }
        v.add(item = new ItemTypeBoolean(SHOWLOGFILE, isAllowedShowLogfile(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Logdatei anzeigen")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EXITEFA, isAllowedExitEfa(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("efa beenden")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(LOCKEFA, isAllowedLockEfa(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("efa entsperren")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(BACKUP, isAllowedCreateBackup(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Backup erstellen")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(RESTORE, isAllowedRestoreBackup(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Backup einspielen")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(UPDATEEFA, isAllowedUpdateEfa(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Online-Update")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(EXECCOMMAND, isAllowedExecCommand(),
                IItemType.TYPE_PUBLIC, CAT_PERMISSIONS, International.getString("Betriebssystemkommando ausführen")));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());

        v.add(item = new ItemTypeBoolean(MSGREADADMIN, isAllowedMsgReadAdmin(),
                IItemType.TYPE_PUBLIC, CAT_MESSAGES, International.getMessage("Nachrichten an {recipient} lesen",
                International.getString("Admin"))));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(MSGMARKREADADMIN, isAllowedMsgMarkReadAdmin(),
                IItemType.TYPE_PUBLIC, CAT_MESSAGES, International.getMessage("Nachrichten an {recipient} als gelesen markieren",
                International.getString("Admin"))));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(MSGAUTOREADADMIN, isAllowedMsgAutoMarkReadAdmin(),
                IItemType.TYPE_PUBLIC, CAT_MESSAGES, International.getMessage("Nachrichten an {recipient} automatisch als gelesen markieren",
                International.getString("Admin"))));
        v.add(item = new ItemTypeBoolean(MSGREADBOATMAINT, isAllowedMsgReadBoatMaintenance(),
                IItemType.TYPE_PUBLIC, CAT_MESSAGES, International.getMessage("Nachrichten an {recipient} lesen",
                International.getString("Bootswart"))));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(MSGMARKREADBOATMAINT, isAllowedMsgMarkReadBoatMaintenance(),
                IItemType.TYPE_PUBLIC, CAT_MESSAGES, International.getMessage("Nachrichten an {recipient} als gelesen markieren",
                International.getString("Bootswart"))));
        ((ItemTypeBoolean)item).setEnabled(!isSuperAdmin());
        v.add(item = new ItemTypeBoolean(MSGAUTOREADBOATMAINT, isAllowedMsgAutoMarkReadBoatMaintenance(),
                IItemType.TYPE_PUBLIC, CAT_MESSAGES, International.getMessage("Nachrichten an {recipient} automatisch als gelesen markieren",
                International.getString("Bootswart"))));
        return v;
    }


    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[1];
        header[0] = new TableItemHeader(International.getString("Name"));
        return header;
    }

    public TableItem[] getGuiTableItems() {
        TableItem[] items = new TableItem[1];
        items[0] = new TableItem(getName());
        return items;
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType != null && itemType.getName().equals("PASSWORDBUTTON") &&
            event != null && event instanceof ActionEvent) {
            AdminPasswordChangeDialog dlg = new AdminPasswordChangeDialog((JDialog)null, this, false);
            dlg.showDialog();
        }
    }

}
