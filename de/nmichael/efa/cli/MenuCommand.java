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

import de.nmichael.efa.util.Logger;
import java.io.File;
import java.util.Stack;

public class MenuCommand extends MenuBase {

    public static final String CMD_RUN  = "run";

    public MenuCommand(CLI cli) {
        super(cli);
    }

    public void printHelpContext() {
        printUsage(CMD_RUN,  "<command> [options...] [>file]", "run a command");
    }

    public int runExternalCommand(String args) {
        try {
            String outfile = null;
            int pos = args.indexOf(">");
            if (pos > 0) {
                outfile = args.substring(pos+1).trim();
                if (outfile.length() == 0) {
                    outfile = null;
                }
                args = args.substring(0, pos).trim();
            }
            String[] argsarr = args.split(" +");
            ProcessBuilder pb = new ProcessBuilder(argsarr);

            if (outfile != null) {
                try {
                    pb.redirectOutput(new File(outfile));
                    pb.redirectError(new File(outfile));
                } catch (NoSuchMethodError ej7) { // requires Java 7
                    Logger.log(Logger.WARNING, Logger.MSG_CLI_ERROR,
                            "Redirecting output for command '" + args + "' requires Java 7");
                }
            }
            
            Process p = pb.start();
            p.getInputStream().close();
            if (outfile == null) {
                p.getOutputStream().close();
                p.getErrorStream().close();
            }
            cli.loginfo("Command '" + args + "' successfully started" +
                    (outfile != null ? " (output to " + outfile + ")" : "") + ".");
            return CLI.RC_OK;
        } catch(Exception e) {
            cli.logerr("Command '" + args + "' failed to start: " + e.toString());
           return CLI.RC_COMMAND_FAILED;
        }
    }

    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        int ret = super.runCommand(menuStack, cmd, args);
        if (ret < 0) {
            if (cmd.equalsIgnoreCase(CMD_RUN)) {
                return runExternalCommand(args);
            }
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return ret;
        }
    }
}
