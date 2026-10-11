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
import java.util.Stack;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.storage.RemoteCommand;
import de.nmichael.efa.util.Logger;

public class MenuCommand extends MenuBase {

    public static final String CMD_RUN  = "run";
    public static final String CMD_EFA_SHUTDOWN = "shutdownefa";
    public static final String CMD_EFA_RESTART = "restartefa";

    public MenuCommand(CLI cli) {
        super(cli);
    }

    public void printHelpContext() {
        printUsage(CMD_RUN,  "<command> [options...] [>file]", "run a command");
        printUsage(CMD_EFA_SHUTDOWN, "", "Shut down efaBoathouse you are currently connected to");
        printUsage(CMD_EFA_RESTART, "", "Restart efaBoathouse you are currently connected to");
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
            } else if (cmd.equalsIgnoreCase(CMD_EFA_SHUTDOWN)) {
				return restartEfaBoathouse(false, args);
			} else if (cmd.equalsIgnoreCase(CMD_EFA_RESTART)) {
				return restartEfaBoathouse(true, args);
			}
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return ret;
        }
    }

	private int restartEfaBoathouse(boolean restart, String args) {
		Vector<String> options = getArgs(args);
		
		if (!isArgsOkForEfaInstance(CMD_EFA_RESTART, options)) {
			return CLI.RC_INVALID_ARGUMENT;
		}
		
        if (!cli.getAdminRecord().isAllowedExitEfa()) {
            cli.logerr("You don't have permission to access this function.");
            return CLI.RC_NO_PERMISSION;
        }
        
        RemoteCommand cmd = new RemoteCommand(Daten.project);
        boolean result = cmd.exitEfa(restart);
        if (result) {
        	if (restart) {
        		cli.loginfo("efaBoathouse restart command successfully sent to remote instance. efaCLI will now exit.");
        	} else {
        		cli.loginfo("efaBoathouse shutdown command successfully sent to remote instance. efaCLI will now exit.");
        	}
        	cli.quit(CLI.RC_OK);
			return CLI.RC_OK;
        } else {
            cli.logerr("Failed to send efaBoathouse restart command to remote instance.");
    		return CLI.RC_COMMAND_FAILED;
        }


	}
	
	private Vector<String> getArgs(String args) {
		Vector<String> options = new Vector<String>();
		if (args != null) {
			String[] argsarr = args.split(" ");
			for (String arg : argsarr) {
				if (arg.trim().length() > 0) {
					options.add(arg.trim());
				}
			}
		}
		return options;
	}

	private boolean isArgsOkForEfaInstance(String cmd, Vector<String> args) {
		if (args == null) {
			args = new Vector<String>();
		}
		if (args.size() != 0) {
			printUsage("Too many arguments for command: "+ cmd, "" ,"");
			printHelpContext();
			return false;
		} 
		return true;
		
	}
}
