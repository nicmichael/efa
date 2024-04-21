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

import de.nmichael.efa.*;
import de.nmichael.efa.util.*;

// @i18n complete

public class Main extends Program {
    
    static {
        System.setProperty("java.awt.headless", "true");
    }

    static String username = "admin";
    static String password = null;
    static String hostname = "localhost";
    static String port = null;
    static String project = null;
    static String command = null;

    public Main(String[] args) {
        super(Daten.APPL_CLI, args);
        if (port == null) {
            port = Integer.toString(Daten.efaConfig.getValueDataataRemoteEfaServerPort());
        }
        if (project == null || project.length() == 0) {
            project = Daten.efaConfig.getValueLastProjectEfaCli();
        }
        if (project == null || project.length() == 0) {
            project = Daten.efaConfig.getValueLastProjectEfaBoathouse();
        }

        CLI cli = new CLI(username, password, hostname, port, project);
        int ret = cli.run(command);

        Daten.haltProgram(ret);
    }

    public void printUsage(String wrongArgument) {
        super.printUsage(wrongArgument);
        printOption("[username[:password]@][host[:port]][/project]", "Connect String");
        printOption("[-cmd command]", "Run Command");
        printOption("[-v]", "Verbose Output (Debug Logging)");
        System.exit(0);
    }

    public void checkArgs(String[] args) {
        super.checkArgs(args);
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue; // argument already handled by super class
            }
            if (!args[i].startsWith("-")) {
                String s = args[i];
                int pos;
                pos = s.indexOf("@");
                if (pos > 0) {
                    String userpass = s.substring(0, pos);
                    s = s.substring(pos + 1);
                    pos = userpass.indexOf(":");
                    if (pos > 0) {
                        username = userpass.substring(0, pos);
                        password = userpass.substring(pos + 1);
                    } else {
                        username = userpass;
                    }
                }
                pos = s.indexOf("/");
                if (pos > 0) {
                    project = s.substring(pos + 1);
                    s = s.substring(0, pos);
                }
                pos = s.indexOf(":");
                if (pos > 0) {
                    hostname = s.substring(0, pos);
                    port = s.substring(pos + 1);
                } else {
                    hostname = s;
                }
                args[i] = null;
                continue;
            }
            if (args[i].equals("-cmd")) {
                args[i] = null;
                i++;
                if (i < args.length) {
                    command = args[i];
                    args[i] = null;
                }
                continue;
            }
            if (args[i].equals("-v")) {
                Logger.setDebugLogging(true, true);
                args[i] = null;
                continue;
            }
        }
        checkRemainingArgs(args);
    }

    public static void main(String[] args) {
        new Main(args);
    }

}