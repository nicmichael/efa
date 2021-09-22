/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa;

import de.nmichael.efa.data.efacloud.TableBuilder;
import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.core.config.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.core.*;
import static de.nmichael.efa.core.config.EfaTypes.CATEGORY_SESSION;
import static de.nmichael.efa.core.config.EfaTypes.TYPE_SESSION_INSTRUCTION;
import static de.nmichael.efa.core.config.EfaTypes.TYPE_SESSION_JUMREGATTA;
import static de.nmichael.efa.core.config.EfaTypes.TYPE_SESSION_NORMAL;
import static de.nmichael.efa.core.config.EfaTypes.TYPE_SESSION_REGATTA;
import static de.nmichael.efa.core.config.EfaTypes.TYPE_SESSION_TOUR;
import static de.nmichael.efa.core.config.EfaTypes.TYPE_SESSION_TRAINING;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.DataFile;
import de.nmichael.efa.data.storage.RemoteEfaServer;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.gui.*;
import java.io.*;
import java.util.jar.*;
import java.util.*;
import java.awt.*;
import javax.swing.UIManager;
import java.lang.management.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import javax.swing.plaf.ColorUIResource;

// @i18n complete
public class Daten {

    public final static String VERSION            = "2.3.0"; // Version für die Ausgabe (z.B. 2.1.0, kann aber auch Zusätze wie "alpha" o.ä. enthalten)
    public final static String VERSIONID          = "2.3.0_dev";   // VersionsID: Format: "X.Y.Z_MM"; final-Version z.B. 1.4.0_00; beta-Version z.B. 1.4.0_#1
    public final static String VERSIONRELEASEDATE = "01.01.2021";  // Release Date: TT.MM.JJJJ
    public final static String MAJORVERSION       = "2";
    public final static String PROGRAMMID         = "EFA.230"; // Versions-ID für Wettbewerbsmeldungen
    public final static String PROGRAMMID_DRV     = "EFADRV.230"; // Versions-ID für Wettbewerbsmeldungen
    public final static String COPYRIGHTYEAR      = "21";   // aktuelles Jahr (Copyright (c) 2001-COPYRIGHTYEAR)
    public final static int REQUIRED_JAVA_VERSION = 8;

    // enable/disable development functions for next version
    public static final boolean NEW_FEATURES = false;

    public final static String EFA     = "efa";                              // efa program name/ID
    public static String EFA_SHORTNAME = "efa";                              // dummy, will be set in International.ininitalize()
    public static String EFA_LONGNAME  = "efa - elektronisches Fahrtenbuch"; // dummy, will be set in International.ininitalize()
    public static String EFA_ONLINE    = "efaOnline";                        // dummy, will be set in International.ininitalize()
    public static String EFA_BASE      = "efaBasis";                         // dummy, will be set in International.ininitalize()
    public static String EFA_BOATHOUSE = "efaBootshaus";                     // dummy, will be set in International.ininitalize()
    public static String EFA_CLI       = "efaCLI";                           // dummy, will be set in International.ininitalize()
    public static String EFA_LIVE      = "efaLive";                          // dummy, will be set in International.ininitalize()
    public static String EFA_WETT      = "efaWett";                          // dummy, will be set in International.ininitalize()
    public static String EFA_REMOTE    = "efaRemote";                        // dummy, will be set in International.ininitalize()
    public static String EFA_CLOUD     = "efaCloud";                         // dummy, will be set in International.ininitalize()
    public final static String EFA_JAVA_ARGUMENTS = "EFA_JAVA_ARGUMENTS"; // Environment Variable Name containing all arguments passed to the "java" command
    public static String efa_java_arguments = null;                 // Environment Variable Contents containing all arguments passed to the "java" command
    public final static String EFADIREKT_MAINCLASS = de.nmichael.efa.boathouse.Main.class.getCanonicalName();
    public final static String EFAURL = "http://efa.nmichael.de";
    public final static String EFASUPPORTURL = "http://efa.nmichael.de/help.html";
    public final static String EFADEVURL = "http://kenai.com/projects/efa";
    public final static String EFATRANSLATEWIKI = "http://kenai.com/projects/efa/pages/TranslatingEfa";
    public final static String EFAWETTURL = "http://efa.rudern.de";
    public final static String NICOLASURL = "http://www.nmichael.de";
    public final static String EFAEMAILNAME = "efa";
    public final static String EMAILINFO = "info@efa.nmichael.de";
    public final static String EMAILBUGS = "bugs@efa.nmichael.de";
    public final static String EMAILHELP = "help@efa.nmichael.de";
    public final static String EMAILDEV  = "dev@efa.nmichael.de";
    public static final String EFA_USERDATA_DIR = "efa2";                // <efauser> = ~/efa2/              Directory for efauser data (if not efa program directory)
    public static final String EFA_RUNNING = "efa.run";                  // <efauser>/efa.run                Indiz, daß efaDirekt läuft (enthält Port#)

    public final static String CONFIGFILE = "efa.cfg";                   // <efauser>/cfg/efa.cfg            Konfigurationsdatei
    public final static String DRVCONFIGFILE = "drv.cfg";                // <efauser>/cfg/drv.cfg            DRV-Konfigurationsdatei
    public static final String EFATYPESFILE = "types.cfg";               // <efauser>/cfg/types.cfg          Konfiguration für EfaTypes (Bezeichnungen)
    public static final String WETTFILE = "wett.cfg";                    // <efauser>/cfg/wett.cfg           Konfiguration für Wettbewerbe
    public static final String WETTDEFS = "wettdefs.cfg";                // <efauser>/cfg/wettdefs.cfg       Wettbewerbs-Definitionen

    public static final String EFALIVE_VERSIONFILE = "/etc/efalive_version"; // file containing efaLive version
    public static       String EFALIVE_VERSION = null;                   // efaLive Version Number
    public static final String EFACREDENVVAR = "EFA_CRED";               // Environment Variable specifying the Credentials File Name
    public static       String EFACREDFILE = ".efacred";                 // <userHomeDir>.efacred             Credentials for CLI Remote Access

    public static final String EFA_LICENSE = "license.html";             // <efa>/doc/license.html
    public static final String EFA_JAR = "efa.jar";                      // <efa>/program/efa.jar
    public static final String EFA_SECFILE = "efa.sec";                  // <efa>/program/efa.sec            Hash von efa.jar: für Erstellen des Admins
    public static final String EFA_HELPSET = "help/efaHelp";

    
    // efa exit codes
    // Note: Codes 99 and higher will cause the shell script to restart efa!
    // Therefore, any errors MUST use smaller codes than 99
    public static final int HALT_BASICCONFIG  = 1;
    public static final int HALT_DIRECTORIES  = 2;
    public static final int HALT_EFACONFIG    = 3;
    public static final int HALT_EFATYPES     = 4;
    public static final int HALT_EFASEC       = 5;
    public static final int HALT_EFARUNNING   = 6;
    public static final int HALT_FILEOPEN     = 7;
    public static final int HALT_EFASECADMIN  = 8;
    public static final int HALT_FILEERROR    = 9;
    public static final int HALT_ERROR        = 10;
    public static final int HALT_INSTALLATION = 11;
    public static final int HALT_ADMIN        = 12;
    public static final int HALT_MISCONFIG    = 12;
    public static final int HALT_FIRSTSETUP   = 13;
    public static final int HALT_PANIC        = 14;
    public static final int HALT_ADMINLOGIN   = 15;
    public static final int HALT_DATALOCK     = 16;
    public static final int HALT_JAVARESTART  = 98;
    public static final int HALT_SHELLRESTART = 99;

    public static final String efaSubdirDATA = "data";
    public static final String efaSubdirCFG = "cfg";

    public static Program program = null;            // this Program
    public static String userHomeDir = null;         // User Home Directory (NOT the efa userdata directoy!! see EfaBaseConfig!)
    public static String userName = null;            // User Name
    public static String efaLogfile = null;          // Logdatei für efa-Konsole
    public static String efaMainDirectory = null;    // Efa-Hauptverzeichnis, immer mit "/" am Ende
    public static String efaProgramDirectory = null; // Programmverzeichnis, immer mit "/" am Ende     ("./program/")
    public static String efaPluginDirectory = null;  // Programmverzeichnis, immer mit "/" am Ende     ("./program/plugins")
    public static String efaDataDirectory = null;    // Efa-Datenverzeichnis, immer mit "/" am Ende    ("./data/")
    public static String efaLogDirectory = null;     // Efa-Log-Verzeichnis, immer mit "/" am Ende     ("./log/")
    public static String efaCfgDirectory = null;     // Efa-Configverzeichnis, immer mit "/" am Ende   ("./cfg/")
    public static String efaDocDirectory = null;     // Efa-Doku-Verzeichnis,  immer mit "/" am Ende   ("./doc/")
    public static String efaFormattingDirectory = null; // Efa-Ausgabe-Verzeichnis, immer mit "/" am Ende ("./fmt/")
    public static String efaBakDirectory = null;     // Efa-Backupverzeichnis, immer mit "/" am Ende   ("./backup/")
    public static String efaTmpDirectory = null;     // Efa-Tempverzeichnis,   immer mit "/" am Ende   ("./tmp/")
    //public static String efaStyleDirectory = null;   // Efa-Stylesheetverzeichnis,   mit "/" am Ende   ("./fmt/layout/")
    public static String fileSep = "/"; // Verzeichnis-Separator (wird in ini() ermittelt)
    public static String efaPreviousVersionID = null; // content of previous VERSIONID when efa was started

    public static String javaVersion = "";
    public static String jvmVersion = "";
    public static String osName = "";
    public static String osVersion = "";
    public static String lookAndFeel = "";

    public final static String PLUGIN_INFO_FILE = "plugins.xml";
    public static String pluginWebpage = "http://efa.nmichael.de/plugins.html"; // wird automatisch auf das in der o.g. Datei stehende gesetzt

    public final static String ONLINEUPDATE_INFO = "http://efa.nmichael.de/eou/eou.xml";
    public final static String ONLINEUPDATE_INFO_DRV = "http://efa.nmichael.de/eou/eoudrv.xml";
    public final static String EFW_UPDATE_DATA = "http://efa.nmichael.de/efw.data";
    public final static String INTERNET_EFAMAIL = "http://efa.rudern.de/efamail.pl"; // was: "http://cgi.snafu.de/nmichael/user-cgi-bin/efamail.pl";
    public final static String IMAGEPATH = "/de/nmichael/efa/img/";
    public final static String FILEPATH = "/de/nmichael/efa/files/";
    public final static String DATATEMPLATEPATH = "/de/nmichael/efa/data/templates/";

    public final static int AUTO_EXIT_MIN_RUNTIME = 60; // Minuten, die efa mindestens gelaufen sein muß, damit es zu einem automatischen Beenden/Restart kommt (60)
    public final static int AUTO_EXIT_MIN_LAST_USED = 5; // Minuten, die efa mindestens nicht benutzt wurde, damit Beenden/Neustart nicht verzögert wird (muß kleiner als AUTO_EXIT_MIN_RUNTIME sein!!!) (5)
    public final static int WINDOWCLOSINGTIMEOUT = 600; // Timeout in Sekunden, nach denen im Direkt-Modus manche Fenster automatisch geschlossen werden
    public final static int MIN_FREEMEM_PERCENTAGE = 90;
    public final static int WARN_FREEMEM_PERCENTAGE = 70;
    public final static int MIN_FREEMEM_COLLECTION_THRESHOLD = 99;
    public static boolean DONT_SAVE_ANY_FILES_DUE_TO_OOME = false;
    public static boolean javaRestart = false;

    public static EfaBaseConfig efaBaseConfig; // efa Base Config
    public static EfaConfig efaConfig;         // Konfigurationsdatei
    public static EfaTypes efaTypes;           // EfaTypes (Bezeichnungen)
    public static Admins admins;               // Admins
    public static Project project;             // Efa Project
    public static WettDefs wettDefs;           // WettDefs
    public static EfaKeyStore keyStore;        // KeyStore
    public static final String PUBKEYSTORE = "keystore_pub.dat";     // <efauser>/data/keystore_pub.dat
    public static final String DRVKEYSTORE = "keystore.dat";         // <efauser>/data/keystore.dat
    public static final String EFAMASTERKEY = "k)fx,R4{Qb:lhTg";
    
    public static EfaSec efaSec;               // efa Security File
    public static EfaRunning efaRunning;       // efa Running (Doppelstarts verhindern)
    public static EmailSenderThread emailSenderThread;

    private static StartLogo splashScreen;     // Efa Splash Screen
    public static boolean firstEfaStart = false; // true wenn efa das erste Mal gestartet wurde und EfaBaseConfig neu erzeugt wurde

    public static Color colorGreen = new Color(0, 150, 0);
    public static Color colorOrange = new Color(255, 100, 0);
    public static String defaultWriteProtectPw = null;
    public static long efaStartTime;
    public static boolean exceptionTest = false; // Exceptions beim Drücken von F1 produzieren (für Exception-Test)
    public static boolean watchWindowStack = false; // Window-Stack überwachen
    public static boolean dateFormatDMY = true;

    public static TableBuilder tableBuilder = new TableBuilder();

    // Encoding zum Lesen und Schreiben von Dateien
    public static final String ENCODING_ISO = "ISO-8859-1";
    public static final String ENCODING_UTF = "UTF-8";

    // Applikations-IDs
    public static int applID = -1;
    public static String applName = "Unknown"; // will be set in iniBase(...)
    public static final int APPL_EFABASE = 1;
    public static final int APPL_EFABH = 2;
    public static final int APPL_CLI = 3;
    public static final int APPL_DRV = 4;
    public static final int APPL_EMIL = 5;
    public static final int APPL_ELWIZ = 6;
    public static final int APPL_EDDI = 7;
    public static final String APPLNAME_EFA = "efaBase";
    public static final String APPLNAME_EFADIREKT = "efaBths";
    public static final String APPLNAME_CLI = "efaCLI";
    public static final String APPLNAME_DRV = "efaDRV";
    public static final String APPLNAME_EMIL = "emil";
    public static final String APPLNAME_ELWIZ = "elwiz";
    public static final String APPLNAME_EDDI = "eddi";

    // Applikations-Mode
    public static final int APPL_MODE_NORMAL = 1;
    public static final int APPL_MODE_ADMIN = 2;
    public static int applMode = APPL_MODE_NORMAL;

    // Applikations- PID
    public static String applPID = "XXXXX"; // will be set in iniBase(...)

    public static AdminRecord initialize() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "initialize()");
            printEfaInfos(false, false, true, false, false);
        }
        AdminRecord newlyCreatedAdminRecord = null;
        iniScreenSize();
        iniMainDirectory();
        iniEfaBaseConfig();
        iniLanguageSupport();
        iniUserDirectory();
        iniLogging();
        iniSplashScreen(true);
        iniEnvironmentSettings();
        iniDirectories();
        iniEfaSec();
        boolean createNewAdmin = iniAdmins();
        Object[] efaFirstSetup = iniEfaFirstSetup(createNewAdmin);
        CustSettings cust = (efaFirstSetup != null ? (CustSettings) efaFirstSetup[0] : null);
        iniEfaConfig(cust);
        iniEfaRunning();
        iniEfaTypes(cust);
        iniCopiedFiles();
        iniAllDataFiles();
        iniRemoteEfaServer();
        iniEmailSenderThread();
        iniGUI();
        iniChecks();
        if (createNewAdmin && efaFirstSetup != null) {
            return (AdminRecord) efaFirstSetup[1];
        }
        return null;
    }

    public static String getCurrentStack() {
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String trace = "";
            for (int i = stack.length - 1; i >= 0; i--) {
                trace = trace + " -> " + stack[i].toString();
                if (stack[i].toString().startsWith(International.class.getCanonicalName())) {
                    break;
                }
            }
            return trace;
        } catch (Exception e) {
            return "";
        }
    }

    public static void haltProgram(int exitCode) {
        if (exitCode == 0 || exitCode == HALT_SHELLRESTART || exitCode == HALT_JAVARESTART) {
            if (project != null && project.isOpen()) {
                try {
                    project.closeAllStorageObjects();
                } catch (Exception e) {
                    Logger.log(Logger.ERROR, Logger.MSG_DATA_CLOSEFAILED,
                            LogString.fileCloseFailed(project.toString(), project.getDescription(), e.toString()));
                }
            }
            if (admins != null && admins.isOpen()) {
                try {
                    admins.close();
                } catch (Exception e) {
                    Logger.log(Logger.ERROR, Logger.MSG_DATA_CLOSEFAILED,
                            LogString.fileCloseFailed(admins.toString(), admins.getDescription(), e.toString()));
                }
            }
            if (efaConfig != null && efaConfig.isOpen()) {
                try {
                    efaConfig.close();
                } catch (Exception e) {
                    Logger.log(Logger.ERROR, Logger.MSG_DATA_CLOSEFAILED,
                            LogString.fileCloseFailed(efaConfig.toString(), efaConfig.getDescription(), e.toString()));
                }
            }
            if (efaTypes != null && efaTypes.isOpen()) {
                try {
                    efaTypes.close();
                } catch (Exception e) {
                    Logger.log(Logger.ERROR, Logger.MSG_DATA_CLOSEFAILED,
                            LogString.fileCloseFailed(efaTypes.toString(), efaTypes.getDescription(), e.toString()));
                }
            }
        }

        if (exitCode != 0) {
            if (exitCode == Daten.HALT_SHELLRESTART || exitCode == Daten.HALT_JAVARESTART) {
                Logger.log(Logger.INFO, Logger.MSG_CORE_HALT,
                        International.getString("PROGRAMMENDE") + " (Exit Code " + exitCode + ")");
            } else {
                if (Daten.applID != Daten.APPL_CLI) {
                    Logger.log(Logger.INFO, Logger.MSG_CORE_HALT, getCurrentStack());
                }
                Logger.log(Logger.ERROR, Logger.MSG_CORE_HALT,
                        International.getString("PROGRAMMENDE") + " (Error Code " + exitCode + ")");
            }
        } else {
            Logger.log(Logger.INFO, Logger.MSG_CORE_HALT,
                    International.getString("PROGRAMMENDE"));
        }
        if (program != null) {
            program.exit(exitCode);
        } else {
            System.exit(exitCode);
        }
    }

    public static void iniBase(int _applID) {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniBase(" + _applID + ")");
        }
        project = null;
        fileSep = System.getProperty("file.separator");
        javaVersion = System.getProperty("java.version");
        jvmVersion = System.getProperty("java.vm.version");
        osName = System.getProperty("os.name");
        osVersion = System.getProperty("os.version");
        userHomeDir = System.getProperty("user.home");
        if (userHomeDir == null) {
            userHomeDir = "";
        }
        if (!userHomeDir.endsWith(fileSep)) {
            userHomeDir += fileSep;
        }
        userName = System.getProperty("user.name");
        applID = _applID;
        switch (applID) {
            case APPL_EFABASE:
                applName = APPLNAME_EFA;
                break;
            case APPL_EFABH:
                applName = APPLNAME_EFADIREKT;
                break;
            case APPL_CLI:
                applName = APPLNAME_CLI;
                break;
            case APPL_DRV:
                applName = APPLNAME_DRV;
                break;
            case APPL_EMIL:
                applName = APPLNAME_EMIL;
                break;
            case APPL_ELWIZ:
                applName = APPLNAME_ELWIZ;
                break;
            case APPL_EDDI:
                applName = APPLNAME_EDDI;
                break;
        }
        efaStartTime = System.currentTimeMillis();

        try {
            // ManagementFactory.getRuntimeMXBean().getName() == "12345@localhost" or similar (not guaranteed by VM Spec!)
            applPID = EfaUtil.int2String(EfaUtil.stringFindInt(ManagementFactory.getRuntimeMXBean().getName(), 0), 5);
        } catch (Exception e) {
            applPID = "00000";
        }
    }

    private static void iniMainDirectory() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniMainDirectory()");
        }
        Daten.efaMainDirectory = System.getProperty("user.dir");
        if (!Daten.efaMainDirectory.endsWith(Daten.fileSep)) {
            Daten.efaMainDirectory += Daten.fileSep;
        }
        if (Daten.efaMainDirectory.endsWith("/program/") && !new File(Daten.efaMainDirectory + "program/").isDirectory()) {
            Daten.efaMainDirectory = Daten.efaMainDirectory.substring(0, Daten.efaMainDirectory.length() - 8);
        }
        if (Daten.efaMainDirectory.endsWith("/classes/") && !new File(Daten.efaMainDirectory + "program/").isDirectory()) {
            Daten.efaMainDirectory = Daten.efaMainDirectory.substring(0, Daten.efaMainDirectory.length() - 8);
        }
        Daten.efaProgramDirectory = Daten.efaMainDirectory + "program" + Daten.fileSep; // just temporary, will be overwritten by iniDirectories()
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            printEfaInfos(true, false, false, false, false);
        }
    }

    private static void iniEfaBaseConfig() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaBaseConfig()");
        }
        String efaBaseConfigFile = Daten.userHomeDir + (Daten.fileSep != null && !Daten.userHomeDir.endsWith(Daten.fileSep) ? Daten.fileSep : "");
        Daten.efaBaseConfig = new EfaBaseConfig(efaBaseConfigFile);
        if (!EfaUtil.canOpenFile(Daten.efaBaseConfig.getFileName())) {
            if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
                Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaBaseConfig(): cannot open: " + Daten.efaBaseConfig.getFileName());
            }
            if (!Daten.efaBaseConfig.writeFile()) {
                if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
                    Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaBaseConfig(): cannot write: " + Daten.efaBaseConfig.getFileName());
                }
                String msg = International.getString("efa can't start") + ": "
                        + LogString.fileCreationFailed(International.getString("Basic Configuration File"), Daten.efaBaseConfig.getFileName());
                Logger.log(Logger.ERROR, Logger.MSG_CORE_BASICCONFIGFAILEDCREATE, msg);
                if (isGuiAppl()) {
                    Dialog.error(msg);
                }
                haltProgram(HALT_BASICCONFIG);
            }
            firstEfaStart = true;
        }
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaBaseConfig(): firstEfaStart=" + firstEfaStart);
        }
        if (!Daten.efaBaseConfig.readFile()) {
            if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
                Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaBaseConfig(): cannot read: " + Daten.efaBaseConfig.getFileName());
            }
            String msg = International.getString("efa can't start") + ": "
                        + LogString.fileOpenFailed(International.getString("Basic Configuration File"), Daten.efaBaseConfig.getFileName());
            Logger.log(Logger.ERROR, Logger.MSG_CORE_BASICCONFIGFAILEDOPEN, msg);
            if (isGuiAppl()) {
                Dialog.error(msg);
            }
            haltProgram(HALT_BASICCONFIG);
        }
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            printEfaInfos(true, false, false, false, false);
        }
    }

    private static void iniLanguageSupport() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniLanguageSupport()");
        }
        International.initialize();
    }

    private static void iniUserDirectory() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniUserDirectory()");
        }
        if (firstEfaStart && isGuiAppl()) {
            while (true) {
                if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
                    Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniUserDirectory(): prompting user for input...");
                }
                ItemTypeFile dir = new ItemTypeFile("USERDIR", Daten.efaBaseConfig.efaUserDirectory,
                        International.getString("Verzeichnis für Nutzerdaten"),
                        International.getString("Verzeichnisse"),
                        null, ItemTypeFile.MODE_OPEN, ItemTypeFile.TYPE_DIR,
                        IItemType.TYPE_PUBLIC, "",
                        International.getString("In welchem Verzeichnis soll efa sämtliche Benutzerdaten ablegen?"));
                dir.setFieldSize(600, 19);
                if (SimpleInputDialog.showInputDialog((Frame)null, International.getString("Verzeichnis für Nutzerdaten"), dir)) {
                    dir.getValueFromGui();
                    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
                        Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniUserDirectory(): input=" + dir.getValue());
                    }
                    if (!efaBaseConfig.trySetUserDir(dir.getValue(), javaRestart)) {
                        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
                            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniUserDirectory(): " +
                                    LogString.directoryNoWritePermission(dir.getValue(), International.getString("Verzeichnis")));
                        }
                        Dialog.error(LogString.directoryNoWritePermission(dir.getValue(), International.getString("Verzeichnis")));
                    } else {
                        efaBaseConfig.writeFile();
                        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
                            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniUserDirectory(): " + efaBaseConfig.getFileName() + " written.");
                        }
                        break;
                    }
                } else {
                    if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
                        Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniUserDirectory(): input aborted.");
                    }
                    Daten.haltProgram(HALT_BASICCONFIG);
                }
            }
        }
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            printEfaInfos(true, false, false, false, false);
        }
    }

    private static void iniLogging() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniLogging()");
        }
        Daten.efaLogDirectory = Daten.efaBaseConfig.efaUserDirectory + "log" + Daten.fileSep;
        if (!checkAndCreateDirectory(Daten.efaLogDirectory)) {
            haltProgram(HALT_DIRECTORIES);
        }
        String lastLogEntry = null;
        if (applID == APPL_EFABH) {
            lastLogEntry = Logger.getLastLogEntry("efa.log");
        }
        String baklog = null; // backup'ed logfile
        switch (applID) {
            case APPL_EFABASE:
            case APPL_EFABH:
            case APPL_DRV:
                baklog = Logger.ini("efa.log", true, false);
                break;
            case APPL_CLI:
                baklog = Logger.ini("efa.log", true, true);
                break;
            default:
                baklog = Logger.ini(null, true, false);
                break;
        }

        Logger.log(Logger.INFO, Logger.MSG_EVT_EFASTART,
                International.getString("PROGRAMMSTART"));
        Logger.log(Logger.INFO, Logger.MSG_INFO_VERSION,
                "Version efa: " + Daten.VERSIONID + " -- Java: " + Daten.javaVersion + " (JVM " + Daten.jvmVersion + ") -- OS: " + Daten.osName + " " + Daten.osVersion);

        if (Logger.isDebugLogging()) {
            Logger.log(Logger.INFO, Logger.MSG_LOGGER_DEBUGACTIVATED,
                    "Debug Logging activated."); // do not internationalize!
        }

        if (baklog != null) {
            Logger.log(Logger.INFO, Logger.MSG_EVT_LOGFILEARCHIVED,
                    International.getMessage("Alte Logdatei wurde nach '{filename}' verschoben.", baklog));
        }

        if (lastLogEntry != null && lastLogEntry.length() > 0 &&
            !lastLogEntry.contains(International.getString("PROGRAMMENDE"))) {
            Logger.log(Logger.WARNING, Logger.MSG_WARN_PREVIOUSEXITIRREGULAR,
                    International.getMessage("efa wurde zuvor nicht korrekt beendet. Letzer Eintrag in Logdatei: {msg}",
                    lastLogEntry));
        }
    }

    private static void iniEnvironmentSettings() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEnvironmentSettings()");
        }
        String s;

        try {
            if (applID == APPL_EFABH) {
                Daten.efa_java_arguments = System.getenv(Daten.EFA_JAVA_ARGUMENTS);
                if (Logger.isTraceOn(Logger.TT_CORE)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GENERIC,
                            Daten.EFA_JAVA_ARGUMENTS + "=" + Daten.efa_java_arguments);
                }
            }
        } catch (Error e) {
            Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTGETEFAJAVAARGS,
                    "Cannot get Environment Variable " + Daten.EFA_JAVA_ARGUMENTS + ": " + e.toString());
        }

        try {
            s = System.getenv(EFACREDENVVAR);
            if (s != null && s.length() > 0) {
                EFACREDFILE = s;
            } else {
                EFACREDFILE = Daten.userHomeDir + EFACREDFILE;
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }

        try {
            if ((new File(EFALIVE_VERSIONFILE).exists())) {
                EFALIVE_VERSION = "";
                BufferedReader f = new BufferedReader(new FileReader(EFALIVE_VERSIONFILE));
                s = f.readLine();
                if (s != null) {
                    EFALIVE_VERSION = s.trim();
                }
                f.close();
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
    }

    private static void iniDirectories() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniDirectories()");
        }
        // ./program
        Daten.efaProgramDirectory = Daten.efaMainDirectory + "program" + Daten.fileSep;
        if (!checkAndCreateDirectory(Daten.efaProgramDirectory)) {
            haltProgram(HALT_DIRECTORIES);
        }

        // ./program/plugins
        Daten.efaPluginDirectory = Daten.efaProgramDirectory + "plugins" + Daten.fileSep;
        if (!checkAndCreateDirectory(Daten.efaPluginDirectory)) {
            haltProgram(HALT_DIRECTORIES);
        }

        // ./daten
        if (applID != APPL_DRV) {
            Daten.efaDataDirectory = Daten.efaBaseConfig.efaUserDirectory + efaSubdirDATA + Daten.fileSep;
        } else {
            Daten.efaDataDirectory = Daten.efaBaseConfig.efaUserDirectory + "daten" + Daten.fileSep;
        }
        if (!checkAndCreateDirectory(Daten.efaDataDirectory)) {
            haltProgram(HALT_DIRECTORIES);
        }

        // ./cfg
        Daten.efaCfgDirectory = Daten.efaBaseConfig.efaUserDirectory + efaSubdirCFG + Daten.fileSep;
        if (!checkAndCreateDirectory(Daten.efaCfgDirectory)) {
            haltProgram(HALT_DIRECTORIES);
        }

        // ./doc
        Daten.efaDocDirectory = Daten.efaMainDirectory + "doc" + Daten.fileSep;
        if (!checkAndCreateDirectory(Daten.efaDocDirectory)) {
            haltProgram(HALT_DIRECTORIES);
        }

        // ./ausgabe
        //Daten.efaAusgabeDirectory = Daten.efaBaseConfig.efaUserDirectory + "fmt" + Daten.fileSep;
        //if (!checkAndCreateDirectory(Daten.efaAusgabeDirectory)) {
        //    haltProgram(HALT_DIRECTORIES);
        //}

        // ./ausgabe/layout
        //Daten.efaStyleDirectory = Daten.efaAusgabeDirectory + "layout" + Daten.fileSep;
        //if (!checkAndCreateDirectory(Daten.efaStyleDirectory)) {
        //    haltProgram(HALT_DIRECTORIES);
        //}

        // ./bak
        if (!trySetEfaBackupDirectory(null)) {
            haltProgram(HALT_DIRECTORIES);
        }

        // ./tmp
        Daten.efaTmpDirectory = Daten.efaBaseConfig.efaUserDirectory + "tmp" + Daten.fileSep;
        if (!checkAndCreateDirectory(Daten.efaTmpDirectory)) {
            haltProgram(HALT_DIRECTORIES);
        }

        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            printEfaInfos(true, false, false, false, false);
        }
    }

    public static void iniSplashScreen(boolean show) {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniSplashScreen(" + show + ")");
        }
        if (!isGuiAppl()) {
            return;
        }
        if (show) {
            splashScreen = new StartLogo(IMAGEPATH + "efaIntro.png");
            splashScreen.show();
            try {
                Thread.sleep(1000); // Damit nach automatischem Restart genügend Zeit vergeht
            } catch (InterruptedException e) {
            }
        } else {
            if (splashScreen != null) {
                splashScreen.remove();
                splashScreen = null;
            }
        }
    }

    public static void iniEfaSec() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaSec()");
        }
        if (firstEfaStart) {
            EfaSec.createNewSecFile(Daten.efaBaseConfig.efaUserDirectory + Daten.EFA_SECFILE, Daten.efaProgramDirectory + Daten.EFA_JAR);
        }
        efaSec = new EfaSec(Daten.efaBaseConfig.efaUserDirectory + Daten.EFA_SECFILE);
        // in efa2 we don't care whether the file is corrupt, just whether it's there
        /*
        if (efaSec.secFileExists() && !efaSec.secValueValid()) {
            String msg = International.getStringXXX("Die Sicherheitsdatei ist korrupt!") + "\n"
                    + International.getXXX("Aus Gründen der Sicherheit verweigert efa den Dienst. "
                    + "Um efa zu reaktivieren, wende Dich bitte an den Entwickler: ") + Daten.EMAILHELP;
            Logger.log(Logger.ERROR, Logger.MSG_CORE_EFASECCORRUPTED, msg);
            if (isGuiAppl()) {
                Dialog.error(msg);
            }
            haltProgram(HALT_EFASEC);
        }
        */
    }

    // returns true if we need to create a new super admin (and are allowed to do so)
    // returns false if we have a super admin and don't need to create one
    // halts efa if there is no super admin, but we're not allowed to create one either
    public static boolean iniAdmins() {
        if (applID == APPL_DRV) {
            return false;
        }
        Daten.admins = new Admins();
        try {
            // try to open admin file
            Daten.admins.open(false);
        } catch (Exception e) {
            if (!isGuiAppl()) {
                // if this is not a GUI appl, then stop here!
                Logger.log(Logger.ERROR, Logger.MSG_CORE_ADMINSFAILEDOPEN,
                        LogString.fileOpenFailed(((DataFile) Daten.admins.data()).getFilename(),
                        International.getString("Administratoren")));
                haltProgram(HALT_ADMIN);
            }
            // check whether admin file exists, and only could not be opened
            boolean exists = true;
            try {
                exists = Daten.admins.data().existsStorageObject();
            } catch (Exception ee) {
                Logger.logdebug(ee);
            }
            if (exists) {
                // admin file exists, but could not be opened. we exit here.
                String msg = LogString.fileOpenFailed(((DataFile) Daten.admins.data()).getFilename(),
                        International.getString("Administratoren"));
                Logger.log(Logger.ERROR, Logger.MSG_CORE_ADMINSFAILEDOPEN, msg);
                if (isGuiAppl()) {
                    Dialog.error(msg);
                }
                haltProgram(HALT_ADMIN);
            }
            // no admin file there, we need to create a new one
            if (Daten.efaSec.secFileExists() && Daten.efaSec.secValueValid()) {
                // ok, sec file is there: we're allowed to create a new one
                return true;
            } else {
                // no sec file there: exit and don't create new admin
                String msg = International.getString("Kein Admin gefunden.") + "\n"
                        + International.getString("Aus Gründen der Sicherheit verweigert efa den Dienst. "
                        + "Hilfe zum Reaktivieren von efa erhälst Du im Support-Forum.");
                Logger.log(Logger.ERROR, Logger.MSG_CORE_ADMINSFAILEDNOSEC, msg);
                if (isGuiAppl()) {
                    Dialog.error(msg);
                }
                haltProgram(HALT_EFASEC);
            }
            return false; // we never reach here, but just to be sure... ;-)
        }
        // we do have a admin file already that we can open. now check whether there's a super admin configured as well
        if (admins.getAdmin(Admins.SUPERADMIN) == null) {
            // we don't have a super admin yet
            if (Daten.efaSec.secFileExists() && Daten.efaSec.secValueValid()) {
                // ok, sec file is there: we're allowed to create a new one
                return true;
            }
            // no sec file there: exit and don't create new admin
            String msg = International.getString("Kein Admin gefunden.") + "\n"
                    + International.getString("Aus Gründen der Sicherheit verweigert efa den Dienst. "
                        + "Hilfe zum Reaktivieren von efa erhälst Du im Support-Forum.");
            Logger.log(Logger.ERROR, Logger.MSG_CORE_ADMINSFAILEDNOSEC, msg);
            if (isGuiAppl()) {
                Dialog.error(msg);
            }
            haltProgram(HALT_EFASEC);
            return false; // we never reach here, but just to be sure... ;-)
        } else {
            // ok, we do have a super admin already
            return false;
        }
    }

    /**
     * @return [0] == CustSettins; [1] == new AdminRecord
     */
    public static Object[] iniEfaFirstSetup(boolean createNewAdmin) {
        if (applID == APPL_DRV) {
            return null;
        }
        if (firstEfaStart || createNewAdmin) {
            if (!isGuiAppl()) {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_BASICCONFIG,
                        "efa is not yet fully set up. Please launch GUI program first.");
                Daten.haltProgram(HALT_BASICCONFIG);
            }
            iniSplashScreen(false);
            EfaFirstSetupDialog dlg = new EfaFirstSetupDialog(createNewAdmin, firstEfaStart);
            dlg.showDialog();
            if (!dlg.getDialogResult()) {
                haltProgram(HALT_FIRSTSETUP);
            }
            Object[] result = new Object[2];
            result[0] = dlg.getCustSettings();
            result[1] = dlg.getNewSuperAdmin();
            return result;
        }
        return null;
    }

    public static void iniEfaConfig(CustSettings custSettings) {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaConfig()");
        }
        if (applID != APPL_DRV) {
            efaConfig = new EfaConfig(custSettings);
            try {
                efaConfig.open(false);
                efaPreviousVersionID = efaConfig.getValueVersion();
                if (!VERSIONID.equals(efaPreviousVersionID)) {
                    efaConfig.setValueVersion(VERSIONID);
                }
            } catch (Exception eopen) {
                try {
                    efaConfig.open(true);
                    String msg = LogString.fileNewCreated(((DataFile) efaConfig.data()).getFilename(),
                            International.getString("Konfigurationsdatei"));
                    Logger.log(Logger.WARNING, Logger.MSG_CORE_EFACONFIGCREATEDNEW, msg);
                } catch (Exception ecreate) {
                    String msg = LogString.fileCreationFailed(((DataFile) efaConfig.data()).getFilename(),
                            International.getString("Konfigurationsdatei"));
                    Logger.log(Logger.ERROR, Logger.MSG_CORE_EFACONFIGFAILEDCREATE, msg);
                    if (isGuiAppl()) {
                        Dialog.error(msg);
                    }
                    haltProgram(HALT_EFACONFIG);
                }
            }
            Daten.efaConfig.setExternalParameters(false);
            Daten.dateFormatDMY = !DataTypeDate.MONTH_DAY_YEAR.equals(Daten.efaConfig.getValueDateFormat());
        }
    }

    public static void iniEfaTypes(CustSettings custSettings) {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaTypes()");
        }
        if (applID == APPL_DRV) {
            return;
        }
        efaTypes = new EfaTypes(custSettings);
        try {
            efaTypes.open(false);
        } catch (Exception eopen) {
            try {
                efaTypes.open(true);
                String msg = LogString.fileNewCreated(((DataFile) efaTypes.data()).getFilename(),
                        International.getString("Bezeichnungen"));
                Logger.log(Logger.WARNING, Logger.MSG_CORE_EFATYPESCREATEDNEW, msg);
            } catch (Exception ecreate) {
                String msg = LogString.fileCreationFailed(((DataFile) efaTypes.data()).getFilename(),
                        International.getString("Bezeichnungen"));
                Logger.log(Logger.ERROR, Logger.MSG_CORE_EFATYPESFAILEDCREATE, msg);
                if (isGuiAppl()) {
                    Dialog.error(msg);
                }
                haltProgram(HALT_EFATYPES);
            }
        }
        if (efaPreviousVersionID != null && 
             (Daten.VERSIONID.compareTo(efaPreviousVersionID) > 0 &&
              "2.2.2".compareTo(efaPreviousVersionID) > 0) ) {
            StringBuilder changes = new StringBuilder();
            if (efaTypes.isConfigured(CATEGORY_SESSION, TYPE_SESSION_TRAINING)) {
                changes.append( (changes.length() > 0 ? ", " : "") +
                        TYPE_SESSION_TRAINING + 
                        "(" + efaTypes.getValue(CATEGORY_SESSION, TYPE_SESSION_TRAINING) + ")");
                efaTypes.removeValue(CATEGORY_SESSION, TYPE_SESSION_TRAINING);
            }
            if (efaTypes.isConfigured(CATEGORY_SESSION, TYPE_SESSION_JUMREGATTA)) {
                changes.append( (changes.length() > 0 ? ", " : "") +
                        TYPE_SESSION_JUMREGATTA + 
                        "(" + efaTypes.getValue(CATEGORY_SESSION, TYPE_SESSION_JUMREGATTA) + ")");
                efaTypes.removeValue(CATEGORY_SESSION, TYPE_SESSION_JUMREGATTA);
            }
            if (efaTypes.isConfigured(CATEGORY_SESSION, TYPE_SESSION_INSTRUCTION)) {
                changes.append( (changes.length() > 0 ? ", " : "") +
                        TYPE_SESSION_INSTRUCTION + 
                        "(" + efaTypes.getValue(CATEGORY_SESSION, TYPE_SESSION_INSTRUCTION) + ")");
                efaTypes.removeValue(CATEGORY_SESSION, TYPE_SESSION_INSTRUCTION);
            }
            if (efaTypes.isConfigured(CATEGORY_SESSION, TYPE_SESSION_TOUR)) {
                changes.append( (changes.length() > 0 ? ", " : "") +
                        TYPE_SESSION_TOUR + 
                        "(" + efaTypes.getValue(CATEGORY_SESSION, TYPE_SESSION_TOUR) + ")");
                efaTypes.removeValue(CATEGORY_SESSION, TYPE_SESSION_TOUR);
            }
            if (changes.length() > 0) {
                Logger.log(Logger.INFO, Logger.MSG_CORE_EFATYPESUPDATED, 
                        International.getMessage("Es wird empfohlen, die Fahrtarten {types} nicht mehr " +
                                "zu verwenden. Diese Fahrtarten wurden daher von efa soeben entfernt. Bei Bedarf " +
                                "können sie manuell wieder hinzugefügt werden. Weitere Hinweise auf http://efa.nmichael.de/help/fahrtarten.html", changes.toString()), true);
            }
        }
        Daten.efaConfig.buildTypes();
    }

    public static void iniEfaRunning() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEfaRunning()");
        }
        if (applID == APPL_CLI) {
            return;
        }
        efaRunning = new EfaRunning();
        if (efaRunning.isRunning()) {
            String msg = International.getString("efa läuft bereits und kann nicht zeitgleich zweimal gestartet werden!");
            Logger.log(Logger.ERROR, Logger.MSG_CORE_EFAALREADYRUNNING, msg);
            if (isGuiAppl()) {
                Dialog.error(msg);
            }
            haltProgram(Daten.HALT_EFARUNNING);
        }
        efaRunning.run();
        efaRunning.runDataLockThread();
    }

    public static void iniCopiedFiles() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniCopiedFiles()");
        }
        String distribCfgDirectory = Daten.efaMainDirectory + "cfg" + Daten.fileSep;
        tryCopy(distribCfgDirectory + Daten.WETTFILE, Daten.efaCfgDirectory + Daten.WETTFILE, true);
        tryCopy(distribCfgDirectory + Daten.WETTDEFS, Daten.efaCfgDirectory + Daten.WETTDEFS, true);
    }

    public static void iniAllDataFiles() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniAllDataFiles()");
        }
        Daten.wettDefs = new WettDefs(Daten.efaCfgDirectory + Daten.WETTDEFS);
        iniDataFile(Daten.wettDefs, true, International.onlyFor("Wettbewerbskonfiguration", "de"));
        Daten.keyStore = (applID != APPL_DRV ?
                new EfaKeyStore(Daten.efaDataDirectory + Daten.PUBKEYSTORE, "efa".toCharArray()) :
                new EfaKeyStore(Daten.efaDataDirectory + Daten.DRVKEYSTORE, "efa".toCharArray()) );
    }

    public static void iniRemoteEfaServer() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniRemoteEfaServer()");
        }
        if (applID != APPL_EFABH) {
            return;
        }
        new RemoteEfaServer(Daten.efaConfig.getValueDataataRemoteEfaServerPort(),
                Daten.efaConfig.getValueDataRemoteEfaServerEnabled());
    }

    public static void iniEmailSenderThread() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniEmailSenderThread()");
        }
        if (applID == APPL_EFABASE || applID == APPL_EFABH) {
            try {
                emailSenderThread = new EmailSenderThread();
                emailSenderThread.start();
            } catch (NoClassDefFoundError e) {
                Logger.log(Logger.WARNING, Logger.MSG_CORE_MISSINGPLUGIN,
                        International.getString("Fehlendes Plugin") + ": " + Plugins.PLUGIN_MAIL + " - "
                        + International.getString("Kein email-Versand möglich!") + " "
                        + International.getMessage("Bitte lade das fehlende Plugin unter der Adresse {url} herunter.", Daten.pluginWebpage));
            }
        }
    }

    public static void iniScreenSize() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniScreenSize()");
        }
        if (!isGuiAppl()) {
            return;
        }
        Dialog.initializeScreenSize();
    }

    public static void iniGUI() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniGUI()");
        }
        if (!isGuiAppl()) {
            return;
        }
        iniScreenSize();

        // Look&Feel
        if (Daten.efaConfig != null) { // is null for applDRV
            try {
                if (Daten.efaConfig.getValueLookAndFeel().length() == 0) {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } else {
                    UIManager.setLookAndFeel(Daten.efaConfig.getValueLookAndFeel());
                }
            } catch (Exception e) {
                Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTSETLOOKANDFEEL,
                        International.getString("Konnte Look&Feel nicht setzen") + ": " + e.toString());
            }
        }

        // Look&Feel specific Work-Arounds
        try {
            lookAndFeel = UIManager.getLookAndFeel().getClass().toString();
            if (!lookAndFeel.endsWith("MetalLookAndFeel")) {
                // to make PopupMenu's work properly and not swallow the next MousePressed Event, see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6753637
                Dialog.getUiDefaults().put("PopupMenu.consumeEventOnClose", false);
            }
            Color buttonFocusColor = (Daten.efaConfig != null ?
                Daten.efaConfig.getLafButtonFocusColor() : null);
            if (buttonFocusColor != null) {
                // colored square around text of selected button
                Dialog.getUiDefaults().put("Button.focus", new ColorUIResource(buttonFocusColor));
            }
            // allow users to press buttons by hitting ENTER (and not just SPACE)
            Dialog.getUiDefaults().put("Button.focusInputMap",
                    new javax.swing.UIDefaults.LazyInputMap(new Object[]{"ENTER", "pressed",
                        "released ENTER", "released",
                        "SPACE", "pressed",
                        "released SPACE", "released"
                    }));
        } catch (Exception e) {
            Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTSETLOOKANDFEEL,
                    "Failed to apply LookAndFeel Workarounds: " + e.toString());
        }

        // Font Size
        if (applID == APPL_EFABH) {
            try {
                Dialog.setGlobalFontSize(Daten.efaConfig.getValueEfaDirekt_fontSize(), Daten.efaConfig.getValueEfaDirekt_fontStyle());
            } catch (Exception e) {
                Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTSETFONTSIZE,
                        International.getString("Schriftgröße konnte nicht geändert werden") + ": " + e.toString());
            }
        }
    }

    public static void iniChecks() {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "iniChecks()");
        }
        checkEfaVersion(true);
        checkJavaVersion(true);
    }

    public static void iniDataFile(de.nmichael.efa.efa1.DatenListe f, boolean autoNewIfDoesntExist, String s) {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION,
                    "iniDataFile("+f.getFileName()+","+autoNewIfDoesntExist+","+s+")");
        }
        if (autoNewIfDoesntExist) {
            f.createNewIfDoesntExist();
        } else {
            if (!EfaUtil.canOpenFile(f.getFileName())) {
                if (f.writeFile()) {
                    LogString.logInfo_fileNewCreated(f.getFileName(), s);
                } else {
                    LogString.logError_fileCreationFailed(f.getFileName(), s);
                }
            }
        }
        if (!f.readFile()) {
            LogString.logError_fileOpenFailed(f.getFileName(), s);
        }


    }

    public static boolean isGuiAppl() {
        return (applID == APPL_EFABASE
                || applID == APPL_EFABH
                || applID == APPL_EMIL
                || applID == APPL_ELWIZ
                || applID == APPL_EDDI
                || applID == APPL_DRV) &&
                !CrontabThread.CRONJOB_THREAD_NAME.equals(Thread.currentThread().getName());
    }
    
    public static boolean isApplEfaBase() {
        return (applID == APPL_EFABASE);
    }
    
    public static boolean isApplEfaBoathouse() {
        return (applID == APPL_EFABH);
    }
    
    public static boolean isAdminMode() {
        return applID != APPL_EFABH || applMode == APPL_MODE_ADMIN;
    }

    public static boolean isOsLinux() {
        return "Linux".equals(osName);
    }

    public static boolean isOsWindows() {
        return (osName != null && osName.startsWith("Windows"));
    }

    private static boolean checkAndCreateDirectory(String dir) {
        File f = new File(dir);
        if (!f.isDirectory()) {
            boolean result = f.mkdirs();
            if (result == true) {
                Logger.log(Logger.WARNING, Logger.MSG_CORE_SETUPDIRS,
                        International.getMessage("Verzeichnis '{directory}' konnte nicht gefunden werden und wurde neu erstellt.", dir));
            } else {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_SETUPDIRS,
                        International.getMessage("Verzeichnis '{directory}' konnte weder gefunden, noch neu erstellt werden.", dir));
            }
            return result;
        }
        return true;
    }

    private static boolean tryCopy(String source, String dest, boolean alwaysCopyWhenNewer) {
        if (source.equals(dest)) {
            return true;
        }
        boolean copy = !(new File(dest)).exists();
        if (!copy) {
            File src = new File(source);
            File dst = new File(dest);
            if (src.exists() && dst.exists() && src.lastModified() > dst.lastModified() && alwaysCopyWhenNewer) {
                copy = true;
            }
        }
        if (copy) {
            if (EfaUtil.copyFile(source, dest)) {
                Logger.log(Logger.INFO, Logger.MSG_CORE_SETUPFILES,
                        International.getMessage("Datei '{file}' wurde aus der Vorlage {template} neu erstellt.", dest, source));
                return true;
            } else {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_SETUPFILES,
                        International.getMessage("Datei '{file}' konnte nicht aus der Vorlage {template} neu erstellt werden.", dest, source));
                return false;
            }
        }
        return true; // nothing to do
    }

    public static boolean trySetEfaBackupDirectory(String dir) {
        if (dir == null || dir.length() == 0) {
            dir = Daten.efaBaseConfig.efaUserDirectory + "backup" + Daten.fileSep;
        }
        if (!dir.endsWith(Daten.fileSep)) {
            dir = dir + Daten.fileSep;
        }
        if (checkAndCreateDirectory(dir)) {
            Daten.efaBakDirectory = dir;
            return true;
        }
        return false;
    }

    public static Vector getEfaInfos() {
        return getEfaInfos(true, true, true, true, false);
    }
    
    public static Vector getEfaInfos(boolean efaInfos, 
            boolean pluginInfos, 
            boolean javaInfos, 
            boolean hostInfos,
            boolean jarInfos) {
        Vector infos = new Vector();

        // efa-Infos
        if (efaInfos) {
            infos.add("efa.version=" + Daten.VERSIONID);
            if (EFALIVE_VERSION != null && EFALIVE_VERSION.length() > 0) {
                infos.add("efalive.version=" + Daten.EFALIVE_VERSION);
            }
            if (applID != APPL_EFABH || applMode == APPL_MODE_ADMIN) {
                if (Daten.efaMainDirectory != null) {
                    infos.add("efa.dir.main=" + Daten.efaMainDirectory);
                }
                if (Daten.efaBaseConfig != null && Daten.efaBaseConfig.efaUserDirectory != null) {
                    infos.add("efa.dir.user=" + Daten.efaBaseConfig.efaUserDirectory);
                }
                if (Daten.efaProgramDirectory != null) {
                    infos.add("efa.dir.program=" + Daten.efaProgramDirectory);
                }
                if (Daten.efaPluginDirectory != null) {
                    infos.add("efa.dir.plugin=" + Daten.efaPluginDirectory);
                }
                if (Daten.efaDocDirectory != null) {
                    infos.add("efa.dir.doc=" + Daten.efaDocDirectory);
                }
                if (Daten.efaDataDirectory != null) {
                    infos.add("efa.dir.data=" + Daten.efaDataDirectory);
                }
                if (Daten.efaCfgDirectory != null) {
                    infos.add("efa.dir.cfg=" + Daten.efaCfgDirectory);
                }
                if (Daten.efaBakDirectory != null) {
                    infos.add("efa.dir.bak=" + Daten.efaBakDirectory);
                }
                if (Daten.efaTmpDirectory != null) {
                    infos.add("efa.dir.tmp=" + Daten.efaTmpDirectory);
                }
            }
        }

        // efa Plugin-Infos
        if (pluginInfos) {
            try {
                File dir = new File(Daten.efaPluginDirectory);
                if ((applID != APPL_EFABH || applMode == APPL_MODE_ADMIN) && Logger.isDebugLogging()) {
                    File[] files = dir.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isFile()) {
                            infos.add("efa.plugin.file=" + files[i].getName() + ":" + files[i].length());
                        }
                    }
                }

                Plugins plugins = Plugins.getPluginInfoFromLocalFile();
                String[] names = plugins.getAllPluginNames();
                for (String name : names) {
                    infos.add("efa.plugin." + name + "=" +
                            (plugins.isPluginInstalled(name) ? "installed" : "not installed"));
                }
            } catch (Exception e) {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_INFOFAILED, International.getString("Programminformationen konnten nicht ermittelt werden") + ": " + e.toString());
                return null;
            }
        }
        
        // Java Infos
        if (javaInfos) {
            infos.add("java.version=" + System.getProperty("java.version"));
            infos.add("java.vendor=" + System.getProperty("java.vendor"));
            infos.add("java.home=" + System.getProperty("java.home"));
            infos.add("java.vm.version=" + System.getProperty("java.vm.version"));
            infos.add("java.vm.vendor=" + System.getProperty("java.vm.vendor"));
            infos.add("java.vm.name=" + System.getProperty("java.vm.name"));
            infos.add("os.name=" + System.getProperty("os.name"));
            infos.add("os.arch=" + System.getProperty("os.arch"));
            infos.add("os.version=" + System.getProperty("os.version"));
            if (applID != APPL_EFABH || applMode == APPL_MODE_ADMIN) {
                infos.add("user.home=" + System.getProperty("user.home"));
                infos.add("user.name=" + System.getProperty("user.name"));
                infos.add("user.dir=" + System.getProperty("user.dir"));
                infos.add("java.class.path=" + System.getProperty("java.class.path"));
            }
        }

        // Host Infos
        if (hostInfos) {
            if (applID != APPL_EFABH || applMode == APPL_MODE_ADMIN) {
                try {
                    infos.add("host.name=" + InetAddress.getLocalHost().getCanonicalHostName());
                    infos.add("host.ip=" + InetAddress.getLocalHost().getHostAddress());
                    infos.add("host.interface=" + EfaUtil.getInterfaceInfo(NetworkInterface.getByInetAddress(InetAddress.getLocalHost())));
                } catch(Exception eingore) {
                }
            }
        }

        // JAR methods
        if (jarInfos && Logger.isDebugLogging()) {
            try {
                String cp = System.getProperty("java.class.path");
                while (cp != null && cp.length() > 0) {
                    int pos = cp.indexOf(";");
                    if (pos < 0) {
                        pos = cp.indexOf(":");
                    }
                    String jarfile;
                    if (pos >= 0) {
                        jarfile = cp.substring(0, pos);
                        cp = cp.substring(pos + 1);
                    } else {
                        jarfile = cp;
                        cp = null;
                    }
                    if (jarfile != null && jarfile.length() > 0 && new File(jarfile).isFile()) {
                        try {
                            infos.add("java.jar.filename=" + jarfile);
                            JarFile jar = new JarFile(jarfile);
                            Enumeration _enum = jar.entries();
                            Object o;
                            while (_enum.hasMoreElements() && (o = _enum.nextElement()) != null) {
                                infos.add("java.jar.content=" + o + ":" + (jar.getEntry(o.toString()) == null ? "null" : Long.toString(jar.getEntry(o.toString()).getSize())));
                            }
                        } catch (Exception e) {
                            Logger.log(Logger.ERROR, Logger.MSG_CORE_INFOFAILED, e.toString());
                            return null;
                        }
                    }
                }
            } catch (Exception e) {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_INFOFAILED, International.getString("Programminformationen konnten nicht ermittelt werden") + ": " + e.toString());
                return null;
            }
        }
        return infos;
    }

    public static void printEfaInfos() {
        printEfaInfos(true, true, true, true, false);
    }

    public static void printEfaInfos(boolean efaInfos, boolean pluginInfos, boolean javaInfos, boolean hostInfos, boolean jarInfos) {
        Vector infos = getEfaInfos(efaInfos, pluginInfos, javaInfos, hostInfos, jarInfos);
        for (int i = 0; infos != null && i < infos.size(); i++) {
            Logger.log(Logger.INFO, Logger.MSG_INFO_CONFIGURATION, (String) infos.get(i));
        }
    }

    public static String getEfaImage(int size) {
        int birthday = EfaUtil.getEfaBirthday();
        switch (size) {
            case 1:
                return IMAGEPATH + "efa_small.png";
            case 2:
                return IMAGEPATH + "efa_logo.png";
            case 3:
                return IMAGEPATH + "efa_large.png";
            default:
                return IMAGEPATH + "efa_logo.png";
        }
    }

    public static void checkEfaVersion(boolean interactive) {
        // @todo (P7) check for outdated efa version
/*
        // Bei 1 Jahr alten Versionen alle 90 Tage prüfen, ob eine neue Version vorliegt
        if (EfaUtil.getDateDiff(Daten.VERSIONRELEASEDATE,EfaUtil.getCurrentTimeStampDD_MM_YYYY()) > 365 &&
        (Daten.efaConfig.efaVersionLastCheck == null || Daten.efaConfig.efaVersionLastCheck.length() == 0 ||
        EfaUtil.getDateDiff(Daten.efaConfig.efaVersionLastCheck,EfaUtil.getCurrentTimeStampDD_MM_YYYY()) > 90) ) {
        if (Dialog.yesNoDialog(InternationalXX.getString("Prüfen, ob neue efa-Version verfügbar"),
        InternationalXX.getMessage("Die von Dir verwendete Version von efa ({versionid}) ist bereits "+
        "über ein Jahr alt. Soll efa jetzt für Dich prüfen, ob eine "+
        "neue Version von efa vorliegt?",Daten.VERSIONID)) == Dialog.YES) {
        OnlineUpdateFrame.runOnlineUpdate(this,Daten.ONLINEUPDATE_INFO);
        }
        Daten.efaConfig.efaVersionLastCheck = EfaUtil.getCurrentTimeStampDD_MM_YYYY();
        }

         */
    }

    public static void checkJavaVersion(boolean interactive) {
        // @todo (P7) check for outdated java version
/*
        if (Daten.javaVersion == null) return;

        TMJ tmj = EfaUtil.string2date(Daten.javaVersion,0,0,0);
        int version = tmj.tag*100 + tmj.monat*10 + tmj.jahr;

        if (version < 140) {
        if (Dialog.yesNoDialog(InternationalXX.getString("Java-Version zu alt"),
        InternationalXX.getMessage("Die von Dir verwendete Java-Version {version} wird von efa "+
        "offiziell nicht mehr unterstützt. Einige Funktionen von efa stehen "+
        "unter dieser Java-Version nicht zur Verfügung oder funktionieren nicht "+
        "richtig. Vom Einsatz von efa mit dieser Java-Version wird dringend abgeraten. "+
        "Für den optimalen Einsatz von efa wird Java-Version 5 oder neuer empfohlen.\n\n"+
        "Sollen jetzt die Download-Anleitung für eine neue Java-Version "+
        "angezeigt werden?",Daten.javaVersion)) == Dialog.YES) {
        showJavaDownloadHints();
        }
        return;
        }

        if (!alsoCheckForOptimalVersion) return;

        if (version < 150) {
        if (Dialog.yesNoDialog(InternationalXX.getString("Java-Version alt"),
        InternationalXX.getMessage("Die von Dir verwendete Java-Version {version} ist bereits relativ alt. "+
        "Für den optimalen Einsatz von efa wird Java 5 (Version 1.5.0) oder neuer empfohlen. "+
        "efa funktioniert zwar auch mit älteren Java-Versionen weiterhin, jedoch gibt es einige "+
        "Funktionen, die nur unter neueren Java-Versionen unterstützt werden. Außerdem werden "+
        "Java-Fehler oft nur noch in den neueren Versionen korrigiert, so daß auch aus diesem "+
        "Grund immer der Einsatz einer möglichst neuen Java-Version empfohlen ist.\n\n"+
        "Sollen jetzt die Download-Anleitung für eine neue Java-Version "+
        "angezeigt werden?",Daten.javaVersion)) == Dialog.YES) {
        showJavaDownloadHints();
        }
        }
         */
    }

    public static void checkRegister() {
        if (PROGRAMMID.equals(Daten.efaConfig.getValueRegisteredProgramID())) {
            return; // already registered
        }
        Daten.efaConfig.setValueRegistrationChecks(Daten.efaConfig.getValueRegistrationChecks() + 1);

        boolean promptForRegistration = false;
        if (Daten.efaConfig.getValueRegisteredProgramID().length() == 0) {
            // never before registered
            if (Daten.efaConfig.getValueRegistrationChecks() <= 30
                    && Daten.efaConfig.getValueRegistrationChecks() % 10 == 0) {
                promptForRegistration = true;
            }
        } else {
            // previous version already registered
            if (Daten.efaConfig.getValueRegistrationChecks() <= 10
                    && Daten.efaConfig.getValueRegistrationChecks() % 10 == 0) {
                promptForRegistration = true;
            }
        }

        if (promptForRegistration && Daten.INTERNET_EFAMAIL != null) {
            if (BrowserDialog.openInternalBrowser(null, Daten.EFA_SHORTNAME,
                    "file:" + HtmlFactory.createRegister(),
                    850, 750).endsWith(".pl")) {
                // registration complete
                Daten.efaConfig.setValueRegisteredProgramID(Daten.PROGRAMMID);
                Daten.efaConfig.setValueRegistrationChecks(0);
            }

        }
    }

    /**
     * Returns the Java version as an integer number representing the "official" Java *minor* version.
     * Newer Versions are guaranteed to have higher numbers than previous versions.
     * e.g.
     * for Java 1.4, this will return "4"
     * for Java 1.5, this will return "5"
     * for Java 1.6, this will return "6"
     * for Java 1.7, this will return "7"
     * @return the Java version
     */
    public static int getJavaVersion() {
        try {
            if (Daten.javaVersion.startsWith("1.")) {
                return Integer.parseInt(Daten.javaVersion.substring(2, 3));
            }
            return 99;
        } catch (Exception e) {
            return 0;
        }
    }

    private static void showJavaDownloadHints() {
        if (Daten.efaDocDirectory == null) {
            return;
        }
    }
    
}
