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
    public static final String CMD_EFA_ONLINEUPDATE = "onlineupdate";
    public static final String CMD_EFA_VERSION = "efaversion";

    public MenuCommand(CLI cli) {
        super(cli);
    }

    public void printHelpContext() {
        printUsage(CMD_RUN,  "<command> [options...] [>file]", "run a command");
        printUsage(CMD_EFA_SHUTDOWN, "", "Shut down efaBoathouse you are currently connected to.");
        printUsage(CMD_EFA_RESTART, "", "Restart efaBoathouse you are currently connected to.");
        printUsage(CMD_EFA_VERSION, "", "Show the version information of the efaBoathouse you are currently connected to.");
        cli.loginfo("   Remote efa needs to be version 2.5.4 or higher to support this command.");
        printUsage(CMD_EFA_ONLINEUPDATE, "", "Request an online update of the efaBoathouse you are currently connected to.");
        cli.loginfo("   Remote online update always uses GitHub as update source, and will only update to the latest production release version.");
        cli.loginfo("   If you want to update to a specific version or a beta version, please use the efaBoathouse GUI.");
        cli.loginfo("");
        cli.loginfo("   OnlineUpdate performs a download of the update files, a backup of the current data, performs the update");
        cli.loginfo("   and restarts efaBoathouse. This may take more than the standard timeout of 60 seconds.");
        cli.loginfo("   If you get a timeout error, please check the remote efaBoathouse log file for the result of the update command.");
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
			} else if (cmd.equalsIgnoreCase(CMD_EFA_ONLINEUPDATE)) {
				return runEfaOnlineUpdate(args);
			} else if (cmd.equalsIgnoreCase(CMD_EFA_VERSION)) {
				return getVersionInfo(args);
			}
            
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return ret;
        }
    }

	private int getVersionInfo(String args) {
		RemoteCommand rcmd = new RemoteCommand(Daten.project);
		de.nmichael.efa.data.storage.RemoteVersionInfo versionInfo = rcmd.getVersionInfo();
		if (versionInfo.getResultCode() == -1) {
			cli.logerr("Failed to get version information from remote instance.");
			return CLI.RC_COMMAND_FAILED;
		} else if (versionInfo.getResultCode() != 0) {
			cli.logerr("Failed to get version information from remote instance: " + versionInfo.getResultCode());
			return CLI.RC_COMMAND_FAILED;
		} else {
			cli.loginfo("Remote efaBoathouse version: " + versionInfo.getVersion() +
					" (" + versionInfo.getVersionId() + "), release date: " + versionInfo.getVersionReleaseDate());
			return CLI.RC_OK;
		}
	}

	private int runEfaOnlineUpdate(String args) {
		RemoteCommand rcmd = new RemoteCommand(Daten.project);
		String result = rcmd.onlineUpdate();
		if (result == null) {
			cli.loginfo("efaBoathouse online update command successfully sent to remote instance.");
			return CLI.RC_OK;
		} else {
			cli.logerr("Failed to send efaBoathouse online update command to remote instance: " + result);
			return CLI.RC_COMMAND_FAILED;
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
