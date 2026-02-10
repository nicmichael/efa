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

import java.util.Hashtable;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;

public abstract class MenuBase {

    public static final String CMD_HELP = "help";
    public static final String CMD_EXIT = "exit";
    public static final String CMD_QUIT = "quit";

    public CLI cli;

    public MenuBase(CLI cli) {
        this.cli = cli;
    }

    public void printUsage(String cmd, String args, String description) {
        cli.loginfo(EfaUtil.getString(cmd, 17) + " " + EfaUtil.getString(args, 35) + " " + description);
    }

    public void printHelpHeader(String menu) {
        cli.loginfo("Help for Menu: " + menu);
        cli.loginfo("==========================================================================");
    }

    public abstract void printHelpContext();

    public void printHelpCommon() {
        printUsage(CMD_HELP, "", "print this help page");
        printUsage(CMD_EXIT, "", "exit this menu");
        printUsage(CMD_QUIT, "", "quit " + Daten.APPLNAME_CLI);
    }

    public void printHelp(String menu) {
        printHelpHeader(menu);
        printHelpContext();
        printHelpCommon();
    }

    protected Vector<String> getCommandOptions(String args) {
        if (args == null || args.length() == 0) {
            return null;
        }
        return EfaUtil.split(args, ' ');
    }

    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        if (cmd == null || cmd.length() == 0) {
            return CLI.RC_OK;
        }
        if (cmd.equalsIgnoreCase(CMD_HELP)) {
            printHelp(menuStack.peek());
            return CLI.RC_OK;
        }
        if (cmd.equalsIgnoreCase(CMD_EXIT)) {
            menuStack.pop();
            return CLI.RC_OK;
        }
        if (cmd.equalsIgnoreCase(CMD_QUIT)) {
            cli.quit(CLI.RC_OK);
            return CLI.RC_OK;
        }
        return -1; // to be handled by subclass
    }
        
    protected String removeOptionsFromArgs(String args) {
        if (args!= null && !args.isEmpty()) {
	    	StringBuilder sb = new StringBuilder();
	        StringTokenizer tok = new StringTokenizer(args, " ");
	        if (tok.countTokens() == 0) {
	            return args;
	        }
	        while (tok.hasMoreTokens()) {
	            String s = tok.nextToken().trim();
	            if (!s.startsWith("-")) {
	                sb.append( (sb.length() > 0 ? " " : "") + s);
	            }
	        }
	        return sb.toString();
        } else {
        	return "";
        }
    }
    
    protected Hashtable<String, String> getOptionsFromArgs(String args) {
        Hashtable<String, String> options = new Hashtable<String, String>();
        try {
            StringTokenizer tok = new StringTokenizer(args, " ");
            while (tok.hasMoreTokens()) {
                String s = tok.nextToken().trim();
                if (s.startsWith("-")) {
                    int pos = s.indexOf("=");
                    String name = s.substring(1).toLowerCase();
                    String value = "";
                    if (pos > 0) {
                        name = s.substring(1, pos).toLowerCase();
                        value = s.substring(pos + 1);
                    }
                    options.put(name, value);
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return options;
    }
}
