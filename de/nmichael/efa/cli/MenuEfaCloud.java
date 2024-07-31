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

import de.nmichael.efa.data.efacloud.Transaction;
import de.nmichael.efa.data.efacloud.TxRequestQueue;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import java.util.Stack;
import java.util.Vector;

public class MenuEfaCloud extends MenuBase {

    public static final String CMD_SYNCH_UPLOAD  = "upload";
    public static final String CMD_BACKUP  = "backup";
    public static final String CMD_CRONJOBS  = "cronjobs";

    public MenuEfaCloud(CLI cli) {
        super(cli);
    }

    public void printHelpContext() {
        printUsage(CMD_SYNCH_UPLOAD,  "[only efaBths]", "trigger upload of last 30 days local data changes to server");
        printUsage(CMD_BACKUP,  "[only efaBths]", "trigger backup at efaCloud server (no local action at all.)");
        printUsage(CMD_CRONJOBS,  "[only efaBths]", "trigger all daily jobs at efaCloud server.");
    }

    private int efaCloudAction(String args, String command) {
        if (!cli.getAdminRecord().isAllowedCreateBackup()) {
            cli.logerr("You don't have permission to access this function. Needed is 'isAllowedCreateBackup'");
            return CLI.RC_NO_PERMISSION;
        }
        Vector<String> options = super.getCommandOptions(args);
        if (options != null && options.size() > 0) {
            printHelpContext();
            return CLI.RC_INVALID_COMMAND;
        }

        TxRequestQueue txq = TxRequestQueue.getInstance();
        if (txq == null) {
            cli.logerr(International.getString("efaCloud Kommandos können nicht remote oder per efaCLI " +
                    "ausgeführt werden, sondern nur als automatische Abläufe in efaBootshaus."));
            return CLI.RC_COMMAND_FAILED;
        }

        if (command.equalsIgnoreCase(CMD_SYNCH_UPLOAD))
            txq.registerStateChangeRequest(TxRequestQueue.RQ_QUEUE_START_SYNCH_UPLOAD);
        else if (command.equalsIgnoreCase(CMD_BACKUP))
            txq.appendTransaction(TxRequestQueue.TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.BACKUP, "@All");
        else if (command.equalsIgnoreCase(CMD_CRONJOBS))
            txq.appendTransaction(TxRequestQueue.TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.CRONJOBS, "@All");
        Logger.log(Logger.INFO, Logger.MSG_CORE_CRONJOB,
                International.getMessage("efaCloud Kommando {command} übergeben.", command));
        return CLI.RC_OK;
    }

    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        int ret = super.runCommand(menuStack, cmd, args);
        if (ret < 0) {
            if (cmd.equalsIgnoreCase(CMD_SYNCH_UPLOAD) || cmd.equalsIgnoreCase(CMD_CRONJOBS)
                    || cmd.equalsIgnoreCase(CMD_BACKUP)) {
                return efaCloudAction(args, cmd);
            }
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return ret;
        }
    }
}
