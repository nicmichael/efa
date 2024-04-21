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

import de.nmichael.efa.Daten;
import java.util.Stack;

public class MenuMain extends MenuBase {
    
    private CLI cli;

    public MenuMain(CLI cli) {
        super(cli);
        this.cli = cli;
    }

    public void printHelpContext() {
        printUsage(CLI.MENU_BOATS,            "", "boats");
        printUsage(CLI.MENU_PERSONS,          "", "persons");
        printUsage(CLI.MENU_DESTINATIONS,     "", "destinations");
        printUsage(CLI.MENU_BOATDAMAGES,      "", "boat damages");
        printUsage(CLI.MENU_BOATRESERVATIONS, "", "boat reservations");
        printUsage(CLI.MENU_BOATSTATUS,       "", "boat status");
        printUsage(CLI.MENU_CREWS,            "", "crews");
        printUsage(CLI.MENU_GROUPS,           "", "groups");
        printUsage(CLI.MENU_STATUS,           "", "status");
        printUsage(CLI.MENU_WATERS,           "", "waters");
        printUsage(CLI.MENU_CLUBWORK_BASE		, "", "club work from efaBase");
        printUsage(CLI.MENU_CLUBWORK_BOATHOUSE	, "", "club work from efaBoatHouse");
        if (Daten.efaConfig.getValueUseFunctionalityRowingGermany()) {
            printUsage(CLI.MENU_FAHRTENABZEICHEN, "", "fahrtenabzeichen");
        }
        printUsage(CLI.MENU_MESSAGES,         "", "messages");
        printUsage(CLI.MENU_STATISTICS  ,     "", "statistics");
        printUsage(CLI.MENU_SYNCEFB         , "", "Kanu-eFB synchronization");
        printUsage(CLI.MENU_BACKUP          , "", "backups");
        printUsage(CLI.MENU_COMMAND         , "", "run external commands");
    }
    
    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        int ret = super.runCommand(menuStack, cmd, args);
        if (ret < 0) {
            if (cmd.equalsIgnoreCase(CLI.MENU_BOATS)) {
                if (!cli.getAdminRecord().isAllowedEditBoats()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_BOATS);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_PERSONS)) {
                if (!cli.getAdminRecord().isAllowedEditPersons()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_PERSONS);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_DESTINATIONS)) {
                if (!cli.getAdminRecord().isAllowedEditDestinations()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_DESTINATIONS);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_BOATDAMAGES)) {
                if (!cli.getAdminRecord().isAllowedEditBoatDamages()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_BOATDAMAGES);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_BOATRESERVATIONS)) {
                if (!cli.getAdminRecord().isAllowedEditBoatReservation()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_BOATRESERVATIONS);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_BOATSTATUS)) {
                if (!cli.getAdminRecord().isAllowedEditBoatStatus()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_BOATSTATUS);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_CLUBWORK_BASE)) {
                if (!cli.getAdminRecord().isAllowedEditClubwork()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_CLUBWORK_BASE);
                return runCommandWithArgs(args);
            }  
            if (cmd.equalsIgnoreCase(CLI.MENU_CLUBWORK_BOATHOUSE)) {
                if (!cli.getAdminRecord().isAllowedEditClubwork()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_CLUBWORK_BOATHOUSE);
                return runCommandWithArgs(args);
            }   
            if (cmd.equalsIgnoreCase(CLI.MENU_CREWS)) {
                if (!cli.getAdminRecord().isAllowedEditCrews()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_CREWS);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_GROUPS)) {
                if (!cli.getAdminRecord().isAllowedEditGroups()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_GROUPS);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_STATUS)) {
                if (!cli.getAdminRecord().isAllowedEditPersons()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_STATUS);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_WATERS)) {
                if (!cli.getAdminRecord().isAllowedEditDestinations()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_WATERS);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_FAHRTENABZEICHEN)) {
                if (!cli.getAdminRecord().isAllowedEditFahrtenabzeichen()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_FAHRTENABZEICHEN);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_MESSAGES)) {
                if (!cli.getAdminRecord().isAllowedMsgReadAdmin()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_MESSAGES);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_STATISTICS)) {
                if (!cli.getAdminRecord().isAllowedEditStatistics()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_STATISTICS);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_SYNCEFB)) {
                if (!cli.getAdminRecord().isAllowedSyncKanuEfb()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_SYNCEFB);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_BACKUP)) {
                if (!cli.getAdminRecord().isAllowedCreateBackup() &&
                    !cli.getAdminRecord().isAllowedRestoreBackup()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_BACKUP);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_EFACLOUD)) {
                if (!cli.getAdminRecord().isAllowedAdministerProjectLogbook()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_EFACLOUD);
                return runCommandWithArgs(args);
            }
            if (cmd.equalsIgnoreCase(CLI.MENU_COMMAND)) {
                if (!cli.getAdminRecord().isAllowedExecCommand()) {
                    cli.logerr("You don't have permission to access this function.");
                    return CLI.RC_NO_PERMISSION;
                }
                menuStack.push(CLI.MENU_COMMAND);
                return runCommandWithArgs(args);
            }
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return CLI.RC_OK;
        }
    }

    private int runCommandWithArgs(String args) {
        if (args == null || args.length() == 0) {
            return CLI.RC_OK;
        }
        int ret = cli.runCommandInCurrentMenu(args);
        //if (cli.runCommandInCurrentMenu(args)) {
        //    cli.runCommandInCurrentMenu(CMD_EXIT); // up one menu again
        //    return true;
        //}
        return ret;
    }

}
