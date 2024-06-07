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
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.sync.KanuEfbSyncTask;
import java.util.Stack;
import java.util.Vector;

public class MenuSyncEfb extends MenuBase {

    public static final String CMD_RUN  = "run";
    private static String EFBSYNC_OPTION_VERBOSE = "-verbose";

    public MenuSyncEfb(CLI cli) {
        super(cli);
    }

    public void printHelpContext() {
        printUsage(CMD_RUN,  "[logbook -verbose]", "run synchronization with Kanu-eFB");
    }

    private int syncEfb(String args) {
    	boolean bVerboseMode=false;
    	
        if (!cli.getAdminRecord().isAllowedSyncKanuEfb()) {
            cli.logerr("You don't have permission to access this function.");
            return CLI.RC_NO_PERMISSION;
        }
        
        Vector<String> options = super.getCommandOptions(args);
        if (options != null && options.size() > 2) {
            printHelpContext();
            return CLI.RC_INVALID_COMMAND;
        }
        
        args=removeOptionsFromArgs(args);
        
        Vector<String> optionLogBookName = super.getCommandOptions(args);
    
        //open the logbook set in the parameters
        String logbookName = (optionLogBookName != null && optionLogBookName.size() >= 1 ? optionLogBookName.get(0) : 
                Daten.project.getCurrentLogbookEfaBoathouse());
        if (logbookName == null) {
            cli.logerr("Failed to synchronize: No logbook specified.");
            return CLI.RC_COMMAND_FAILED;
        }
                
        Logbook logbook = Daten.project.getLogbook(logbookName, false);
        if (logbook == null) {
            cli.logerr("Failed to synchronize: Could not open logbook '" + logbookName + "'.");
            return CLI.RC_COMMAND_FAILED;
        }
        
        //check if verbose mode is active
        if (options != null && options.contains(EFBSYNC_OPTION_VERBOSE) ) {
            bVerboseMode=true;
        }
        
        cli.loginfo("Running synchronization for logbook '" + logbookName + "' ...");
        KanuEfbSyncTask syncTask = new KanuEfbSyncTask(logbook, cli.getAdminRecord(), bVerboseMode);
        syncTask.startSynchronization(null);
        try {
            syncTask.join();
        } catch(Exception e) {
            cli.logerr("Error during synchronization: " + e.toString());
            return CLI.RC_COMMAND_FAILED;
        }
        if (syncTask.isSuccessfullyCompleted()) {
            return CLI.RC_OK;
        }
        return CLI.RC_COMMAND_FAILED;
    }

    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        int ret = super.runCommand(menuStack, cmd, args);
        if (ret < 0) {
            if (cmd.equalsIgnoreCase(CMD_RUN)) {
                return syncEfb(args);
            }
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return ret;
        }
    }
}