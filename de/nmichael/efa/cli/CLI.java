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
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.Admins;
import de.nmichael.efa.core.config.Credentials;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.storage.AccessStatistics;
import de.nmichael.efa.data.storage.EfaOnlineClient;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.RemoteEfaClient;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.util.Logger;
import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.util.Stack;

public class CLI {

    public static final String MENU_MAIN             = "main";
    public static final String MENU_BOATS            = "boats";
    public static final String MENU_PERSONS          = "persons";
    public static final String MENU_DESTINATIONS     = "destinations";
    public static final String MENU_STATISTICS       = "statistics";
    public static final String MENU_BOATDAMAGES      = "damages";
    public static final String MENU_BOATRESERVATIONS = "reservations";
    public static final String MENU_BOATSTATUS       = "boatstatus";
    public static final String MENU_CREWS            = "crews";
    public static final String MENU_GROUPS           = "groups";
    public static final String MENU_MESSAGES         = "messages";
    public static final String MENU_STATUS           = "status";
    public static final String MENU_WATERS           = "waters";
    public static final String MENU_FAHRTENABZEICHEN = "fahrtenabzeichen";
    public static final String MENU_SYNCEFB          = "syncefb";
    public static final String MENU_BACKUP           = "backup";
    public static final String MENU_EFACLOUD         = "efacloud";
    public static final String MENU_COMMAND          = "command";
    public static final String MENU_CLUBWORK_BASE	 = "clubworkbase";
    public static final String MENU_CLUBWORK_BOATHOUSE = "clubworkboathouse";

    public static final int RC_OK                            =  0;
    public static final int RC_ERROR_LOGIN                   =  1;
    public static final int RC_ERROR_OPEN_PROJECT            =  2;
    public static final int RC_UNKNOWN_COMMAND               =  3;
    public static final int RC_INVALID_COMMAND               =  4;
    public static final int RC_NO_PERMISSION                 =  5;
    public static final int RC_COMMAND_COMPLETED_WITH_ERRORS = 10;
    public static final int RC_COMMAND_FAILED                = 11;

    enum MODE {
        cli,
        cron
    }

    private MODE mode;
    private String username;
    private String password;
    private String hostname;
    private String port;
    private String project;
    Console console;
    BufferedReader in;

    private EfaConfig remoteEfaConfig;
    private AdminRecord adminRecord;
    private MenuBase lastMenu;
    private Stack<String> menuStack;

    public CLI(String username,
               String password,
               String hostname,
               String port,
               String project) {
        this.mode = MODE.cli;
        this.username = username;
        this.password = password;
        this.hostname = hostname;
        this.port = port;
        this.project = project;
        if (Logger.isTraceOn(Logger.TT_CLI, 1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_CLI_DEBUG, "CLI("+
                    username + "," +
                    (password != null && password.length() > 0 ? "***" : "") + "," +
                    hostname + "," +
                    port + "," +
                    project + ")");
        }
        console = System.console();
        in = new BufferedReader(new InputStreamReader(System.in));
        Credentials cred = new Credentials();
        cred.readCredentials();
        if (username == null || username.length() == 0) {
            username = cred.getDefaultAdmin();
        }
        if (username != null && username.length() > 0 &&
                (password == null || password.length() == 0)) {
            this.password = cred.getPassword(username);
        }
    }

    public CLI() {
        this.mode = MODE.cron;
    }

    private void getIpAndPortFromEfaOnline() {
        if (Logger.isTraceOn(Logger.TT_CLI, 1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_CLI_DEBUG, "getIpAndPortFromEfaOnline()");
        }
        String addr = EfaOnlineClient.getRemoteAddress(hostname, port);
        hostname = null;
        port = null;
        if (addr != null) {
            int pos = addr.indexOf(":");
            if (pos >= 0) {
                hostname = addr.substring(0, pos);
                port = addr.substring(pos + 1);
            }
        }
    }

    public void loginfo(String s) {
        Logger.log(Logger.INFO, Logger.MSG_CLI_INFO, s);
    }

    public void logerr(String s) {
        Logger.log(Logger.ERROR, Logger.MSG_CLI_ERROR, s);
    }

    public void loginput(String s) {
        Logger.log(Logger.INPUT, Logger.MSG_CLI_INPUT, s);
    }

    public void logoutput(String s) {
        Logger.log(Logger.OUTPUT, Logger.MSG_CLI_OUTPUT, s);
    }

    public String promptForInput(String prompt) {
        if (mode == MODE.cron) {
            return "";
        }
        loginput((prompt != null ? prompt + ": " : Daten.EFA_CLI + ":" + menuStack.peek()  + "> "));
        String s = null;
        if (console != null) {
            s = console.readLine();
        } else {
            try {
                s = in.readLine();
            } catch(Exception eignore) {
            }
        }
        if (s == null) {
            System.out.println();
            Daten.haltProgram(0);
        }
        return s;
    }

    public String promptForPassword(String prompt) {
        loginput((prompt != null ? prompt + ": " : Daten.EFA_CLI + "> "));
        String s = null;
        if (console != null) {
            char[] pass = console.readPassword();
            if (pass != null) {
                s = new String(pass);
            }
        } else {
            try {
                s = in.readLine();
            } catch(Exception eignore) {
            }
        }
        if (s == null) {
            System.out.println();
            Daten.haltProgram(0);
        }
        return s;
    }

    public StorageObject getPersistence(Class c, String name, String type) {
        try {
            if (Daten.applID != Daten.APPL_CLI) {
                if (name != null) {
                    return Daten.project.getStorageObject(name, type, false);
                } else {
                    if (EfaConfig.DATATYPE.equals(type)) {
                        return Daten.efaConfig;
                    }
                    if (EfaTypes.DATATYPE.equals(type)) {
                        return Daten.efaTypes;
                    }
                    if (Admins.DATATYPE.equals(type)) {
                        return Daten.admins;
                    }
                }
            }
            StorageObject p;
            if (name != null) {
                p = (StorageObject) c.getConstructor(
                        int.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class).newInstance(
                        IDataAccess.TYPE_EFA_REMOTE,
                        hostname + ":" + port,
                        username,
                        password,
                        name);
            } else {
                p = (StorageObject) c.getConstructor(
                        int.class,
                        String.class,
                        String.class,
                        String.class).newInstance(
                        IDataAccess.TYPE_EFA_REMOTE,
                        hostname + ":" + port,
                        username,
                        password);

            }
            p.setProject(Daten.project);
            return p;
        } catch (Exception e) {
            Logger.log(e);
            return null;
        }
    }

    private int connect() {
        if (mode == MODE.cron) {
            return RC_OK;
        }
        if (Logger.isTraceOn(Logger.TT_CLI, 1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_CLI_DEBUG, "connect("+
                    username + "," +
                    (password != null && password.length() > 0 ? "***" : "") + "," +
                    hostname + "," +
                    port + "," +
                    project + ")");
        }
        if (port != null && port.length() > 0 && !Character.isDigit(port.charAt(0))) {
            getIpAndPortFromEfaOnline();
        }
        if (project == null || project.length() == 0) {
            logerr("Don't know which project to open (no recent project, and no project specified).");
            return RC_ERROR_OPEN_PROJECT;
        }
        if (password == null || password.length() == 0) {
            password = promptForPassword("Password for "+username);
        }
        loginfo("Connecting as "+username+" to " +hostname+":"+port+" ...");
        remoteEfaConfig = (EfaConfig)getPersistence(EfaConfig.class, null, EfaConfig.DATATYPE);
        try {
            if (remoteEfaConfig.isOpen()) {
                loginfo("Connected.");
                adminRecord = ((RemoteEfaClient)remoteEfaConfig.data()).getAdminRecord();
                if (adminRecord == null) {
                    logerr("Could not get Admin Permissions.");
                    return RC_ERROR_LOGIN;
                }
                loginfo("Opening Remote Project " + project + " ...");
                Project prj = new Project(IDataAccess.TYPE_FILE_XML, Daten.efaTmpDirectory, "cli");
                prj.create();
                prj.setEmptyProject("cli");
                prj.setProjectDescription("dummy project created by cli");
                prj.setProjectStorageType(IDataAccess.TYPE_EFA_REMOTE);
                prj.setProjectStorageLocation(hostname + ":" + port);
                prj.setProjectStorageUsername(username);
                prj.setProjectStoragePassword(password);
                prj.setProjectRemoteProjectName(project);
                prj.close();
                Project.openProject(new Project(IDataAccess.TYPE_FILE_XML, Daten.efaTmpDirectory, "cli"), 
                        "cli", false);
                if (Daten.project != null && Daten.project.isRemoteOpen()) {
                    loginfo("Remote Project opened.");
                    Daten.efaConfig.setValueLastProjectEfaCli(project);
                } else {
                    logerr("Failed to open Remote Project " + project + ".");
                    return RC_ERROR_OPEN_PROJECT;
                }
                return RC_OK;
            }
        } catch(Exception e) {
            logerr(e.getMessage());
        }
        return RC_ERROR_LOGIN;
    }

    public AdminRecord getAdminRecord() {
        if (mode == MODE.cli) {
            return adminRecord;
        } else {
            return Daten.admins.getAdmin(Admins.SUPERADMIN);
        }
    }
    
    private String parseCommand(String s, int i) {
        s = s.trim();
        int pos = s.indexOf(" ");
        if (pos >= 0) {
            if (i == 0) {
                return s.substring(0, pos);
            } else {
                return s.substring(pos + 1);
            }
        } else {
            if (i == 0) {
                return s;
            } else {
                return null;
            }
        }
    }

    public void quit(int ret) {
        if (mode == MODE.cli) {
            if (Logger.isDebugLogging()) {
                String[] stats = AccessStatistics.getStatisticsAsSimpleArray();
                for (String s : stats) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_RMTACCESSSTATS, s);
                }
            }
            Daten.haltProgram(ret);
        }
    }

    public Class getMenu() {
        String mymenu = menuStack.peek();
        if (mymenu.equals(MENU_MAIN)) {
            return de.nmichael.efa.cli.MenuMain.class;
        }
        if (mymenu.equals(MENU_BOATS)) {
            return de.nmichael.efa.cli.MenuBoats.class;
        }
        if (mymenu.equals(MENU_BOATDAMAGES)) {
            return de.nmichael.efa.cli.MenuBoatDamages.class;
        }
        if (mymenu.equals(MENU_BOATRESERVATIONS)) {
            return de.nmichael.efa.cli.MenuBoatReservations.class;
        }
        if (mymenu.equals(MENU_BOATSTATUS)) {
            return de.nmichael.efa.cli.MenuBoatStatus.class;
        }
        if (mymenu.equals(MENU_CREWS)) {
            return de.nmichael.efa.cli.MenuCrews.class;
        }
        if (mymenu.equals(MENU_PERSONS)) {
            return de.nmichael.efa.cli.MenuPersons.class;
        }
        if (mymenu.equals(MENU_DESTINATIONS)) {
            return de.nmichael.efa.cli.MenuDestinations.class;
        }
        if (mymenu.equals(MENU_FAHRTENABZEICHEN)) {
            return de.nmichael.efa.cli.MenuFahrtenabzeichen.class;
        }
        if (mymenu.equals(MENU_GROUPS)) {
            return de.nmichael.efa.cli.MenuGroups.class;
        }
        if (mymenu.equals(MENU_MESSAGES)) {
            return de.nmichael.efa.cli.MenuMessages.class;
        }
        if (mymenu.equals(MENU_STATISTICS)) {
            return de.nmichael.efa.cli.MenuStatistics.class;
        }
        if (mymenu.equals(MENU_STATUS)) {
            return de.nmichael.efa.cli.MenuStatus.class;
        }
        if (mymenu.equals(MENU_WATERS)) {
            return de.nmichael.efa.cli.MenuWaters.class;
        }
        if (mymenu.equals(MENU_SYNCEFB)) {
            return de.nmichael.efa.cli.MenuSyncEfb.class;
        }
        if (mymenu.equals(MENU_BACKUP)) {
            return de.nmichael.efa.cli.MenuBackup.class;
        }
        if (mymenu.equals(MENU_EFACLOUD)) {
            return de.nmichael.efa.cli.MenuEfaCloud.class;
        }
        if (mymenu.equals(MENU_COMMAND)) {
            return de.nmichael.efa.cli.MenuCommand.class;
        }
        if (mymenu.equals(MENU_CLUBWORK_BASE)) {
        	return de.nmichael.efa.cli.MenuClubWorkBase.class;
        }
        if (mymenu.equals(MENU_CLUBWORK_BOATHOUSE)) {
        	return de.nmichael.efa.cli.MenuClubWorkBoatHouse.class;
        }
        return null;
    }

    public int run(String initialCommand) {
        int ret = connect();
        if (ret != RC_OK) {
            return ret;
        }
        try {
            if (mode == MODE.cli) {
                Thread.sleep(500);
            }
        } catch(InterruptedException eignore) {
        }
        if (initialCommand != null && initialCommand.length() == 0) {
            initialCommand = null;
        }

        menuStack = new Stack<String>();
        menuStack.push(MENU_MAIN);
        try {
            lastMenu = (MenuBase) MenuMain.class.getConstructor(CLI.class).newInstance(this);
        } catch(Exception e) {
            Logger.logdebug(e);
        }

        while(true) {
            String command = (initialCommand != null ? 
                initialCommand : promptForInput(null));
            if (command == null || command.length() == 0) {
                if (mode == MODE.cron) {
                    break;
                } else {
                    continue;
                }
            }
            ret = runCommandInCurrentMenu(command);
            if (menuStack.isEmpty()) {
                quit(RC_OK);
            }
            if (initialCommand != null) {
                quit(ret);
            }
            if (mode == MODE.cron) {
                break;
            }
        }
        return RC_OK;
    }

    public int runCommandInCurrentMenu(String command) {
        String cmd = parseCommand(command, 0);
        String args = parseCommand(command, 1);
        if (cmd == null || cmd.length() == 0) {
            return RC_OK;
        }
        Class c = getMenu();
        if (c != null) {
            try {
                MenuBase menu;
                boolean menuChanged = false;
                if (lastMenu != null && c == lastMenu.getClass()) {
                    menu = lastMenu;
                } else {
                    menu = (MenuBase) c.getConstructor(CLI.class).newInstance(this);
                    menuChanged = true;
                }
                int ret = menu.runCommand(menuStack, cmd, args);
                if (menuChanged && args != null && args.length() > 0) {
                    menuStack.pop();
                } else {
                    lastMenu = menu;
                }
                return ret;
            } catch (Exception e) {
                Logger.log(e);
                return RC_COMMAND_FAILED;
            }
        } else {
            logerr("Command in unknown Menu");
            return RC_COMMAND_FAILED;
        }
    }
}
