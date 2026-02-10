/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.cli;

import java.io.File;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.Backup;
import de.nmichael.efa.core.BackupMetaData;
import de.nmichael.efa.core.BackupMetaDataItem;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Email;

public class MenuBackup extends MenuBase {

    public static final String CMD_BACKUP  = "create";
    public static final String CMD_RESTORE = "restore";
    public static final String CMD_SHOW    = "show";

    public MenuBackup(CLI cli) {
        super(cli);
    }

    public void printHelpContext() {
        printUsage(CMD_BACKUP,  "[project|config|all] [-includeefalog] [directory/file/mailto:emailadress]", "create backup");
        printUsage(CMD_RESTORE, "<zipfile> [objects...]", "restore backup");
        printUsage(CMD_SHOW,    "<zipfile>", "show archive content");
    }

    private int backup(String args) {
        if (!cli.getAdminRecord().isAllowedCreateBackup()) {
            cli.logerr("You don't have permission to access this function.");
            return CLI.RC_NO_PERMISSION;
        }
        
        Hashtable<String,String> parameters = getOptionsFromArgs(args);
        args = removeOptionsFromArgs(args);            
        
        Vector<String> options = super.getCommandOptions(args);
        if (options == null || options.size() < 1 || options.size() > 2) {
            printHelpContext();
            return CLI.RC_INVALID_COMMAND;
        }
        
        boolean backupProject = false;
        boolean backupConfig = false;
        boolean backupEfaLog = false;
        backupEfaLog = parameters.containsKey("includeefalog");
        
        String backupDir = Daten.efaBakDirectory;
        String backupFile = null;
        String backupEmail = null;
        for (int i=0; i<options.size(); i++) {
            String opt = options.get(i).trim();
            switch(i) {
                case 0:
                    if (opt.equalsIgnoreCase("project")) {
                        backupProject = true;
                    }
                    if (opt.equalsIgnoreCase("config")) {
                        backupConfig = true;
                    }
                    if (opt.equalsIgnoreCase("all")) {
                        backupProject = true;
                        backupConfig = true;
                    }
                    break;
                case 1:
                    if (Email.getEmailAddressFromMailtoString(opt) == null) {
                        File f = new File(opt);
                        if (f.isDirectory()) {
                            backupDir = opt;
                        } else {
                            backupDir = EfaUtil.getPathOfFile(opt);
                            backupFile = EfaUtil.getNameOfFile(opt);
                        }
                        if (backupDir == null || backupDir.length() == 0) {
                            backupDir = Daten.efaBakDirectory;
                        }
                        if (backupFile != null && backupFile.length() == 0) {
                            backupFile = null;
                        }
                    } else {
                        backupEmail = Email.getEmailAddressFromMailtoString(opt);
                    }
                    
                    break;
            }
        }
        if (!backupProject && !backupConfig) {
            printHelpContext();
            return CLI.RC_INVALID_COMMAND;
        }

        Backup backup = (backupEmail == null ?
            new Backup(backupDir, backupFile, backupProject, backupConfig, backupEfaLog) :
            new Backup(Daten.efaTmpDirectory, null, backupEmail, backupProject, backupConfig, backupEfaLog) );
        int ret = backup.runBackup(null);
        if (ret > 0) {
            return CLI.RC_COMMAND_COMPLETED_WITH_ERRORS;
        }
        if (ret < 0) {
            return CLI.RC_COMMAND_FAILED;
        }
        return CLI.RC_OK;
    }

    private int restore(String args) {
        if (!cli.getAdminRecord().isAllowedRestoreBackup()) {
            cli.logerr("You don't have permission to access this function.");
            return CLI.RC_NO_PERMISSION;
        }
        Vector<String> options = super.getCommandOptions(args);
        if (options == null || options.size() < 1) {
            printHelpContext();
            return CLI.RC_INVALID_COMMAND;
        }

        String zipFile = options.get(0);
        String[] restoreObjects = (options.size() > 1 ? new String[options.size() - 1] : null);
        for (int i=0; restoreObjects != null && i<restoreObjects.length; i++) {
            restoreObjects[i] = options.get(i+1);
        }

        // Note: CLI does NOT support restore of object in a project which is not open!
        // This would require opening and/or creating of another project in the server
        // efa, which is currently not possible.
        Backup backup = new Backup(zipFile, restoreObjects, false);
        int ret = backup.runRestore(null);
        if (ret > 0) {
            return CLI.RC_COMMAND_COMPLETED_WITH_ERRORS;
        }
        if (ret < 0) {
            return CLI.RC_COMMAND_FAILED;
        }
        return CLI.RC_OK;
    }

    private int show(String args) {
        Vector<String> options = super.getCommandOptions(args);
        if (options == null || options.size() != 1) {
            printHelpContext();
            return CLI.RC_INVALID_COMMAND;
        }

        String zipFile = options.get(0);

        BackupMetaData metaData = new BackupMetaData(null);
        if (!metaData.read(zipFile)) {
            return CLI.RC_COMMAND_FAILED;
        }

        cli.loginfo("Archive      : " + zipFile);
        if (metaData.getProjectName() != null) {
            cli.loginfo("Project      : " + metaData.getProjectName());
        }
        cli.loginfo("Creation Date: " + EfaUtil.getTimeStamp(metaData.getTimeStamp()));
        cli.loginfo("Object                                  Description                          SCN   Records");
        cli.loginfo("------------------------------------------------------------------------------------------");
        for (int i=0; i<metaData.size(); i++) {
            BackupMetaDataItem meta = metaData.getItem(i);
            cli.loginfo(EfaUtil.getString(meta.getNameAndType(), 40) +
                        EfaUtil.getString(meta.getDescription(), 30) +
                        EfaUtil.getRightBoundNumber(meta.getScn(), 10) +
                        EfaUtil.getRightBoundNumber(meta.getNumberOfRecords(), 10) );
        }

        return CLI.RC_OK;
    }

    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        int ret = super.runCommand(menuStack, cmd, args);
        if (ret < 0) {
            if (cmd.equalsIgnoreCase(CMD_BACKUP)) {
                return backup(args);
            }
            if (cmd.equalsIgnoreCase(CMD_RESTORE)) {
                return restore(args);
            }
            if (cmd.equalsIgnoreCase(CMD_SHOW)) {
                return show(args);
            }
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return ret;
        }
    }
}
