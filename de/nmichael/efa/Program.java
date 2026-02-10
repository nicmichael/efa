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

import javax.swing.UIManager;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;

// @i18n complete
public class Program {

    private AdminRecord newlyCreatedAdminRecord;

    public Program(int applId, String[] args) {
        Daten.program = this;
        if (applId != Daten.APPL_CLI) {
	        try {
	        	//Install the efaFlatLaf by setting it's actual classname by String, and not bei Class.getName().
	        	//This is neccessary as when there is no flatlaf-3.2.5 library present, efa would simply fail to start up
	        	//as yet alone the import of the flatlaf library in this class "Program" would keep efa from starting 
	        	//because of an ClassNotFoundError.
	        	
	        	UIManager.installLookAndFeel("efaFlatLightLaf", "de.nmichael.efa.themes.EfaFlatLightLookAndFeel");

	        	//Dark LAF is prepared but not yet ready.
		        //UIManager.installLookAndFeel("efaFlatDarkLaf", "de.nmichael.efa.themes.EfaFlatDarkLookAndFeel");
	        	
	        } catch (Exception e) {
	        	// efaFlatlaf cannot get installed when a flatLaf-3.2.5.jar is not available in Class path.
	        	// so... if an error occurs, no flatlaf has been installed.
	        	EfaUtil.foo();
	        }
	
        }
        Daten.iniBase(applId);
        checkArgs(args);
        newlyCreatedAdminRecord = Daten.initialize();
    }

    protected AdminRecord getNewlyCreatedAdminRecord() {
        return newlyCreatedAdminRecord;
    }

    public void printOption(String option, String description) {
        while(option.length() < 15) {
            option = option + " ";
        }
        System.out.println("      " + option + "   " + description);
    }

    public void printUsage(String wrongArgument) {
        boolean showHelpDev = false;
        if (wrongArgument != null && wrongArgument.equals("-helpdev")) {
            wrongArgument = null;
            showHelpDev = true;
        }
        System.out.println(Daten.EFA_LONGNAME + " " + Daten.VERSION + " (" + Daten.VERSIONID + ")\n");
        if (wrongArgument != null) {
            System.out.println("ERROR: Unknown Argument" + ": " + wrongArgument+"\n");
        }
        System.out.println("Usage: " +
                           Daten.applName + " [options]");
        System.out.println("    List of options:");
        printOption("-help","Show this help");
        if (showHelpDev) {
            printOption("-javaRestart", "efa restart by Java instead of Shell");
            System.out.println("    Parameters for development use:");
            printOption("-debug","Activate Debug Logging");
            printOption("-debugAll","Activate Debug Logging for all Trace Topics and Levels");
            printOption("-traceTopic <topic>","Set Trace Topic <topic> for Debug Logging");
            printOption("-traceLevel <level>","Set Trace Level <level> for Debug Logging");
            printOption("-logToStdOut","Log all messages also to StdOut");
            printOption("-wws", "Watch Window Stack (report window stack inconsistencies)");
            printOption("-exc", "Exception Test (press [F1] in main window)");
            printOption("-emulateWin", "Emulate Windows Environment");
            printOption("-i18nMarkMissing", "i18n: Mark Missing Keys");
            printOption("-i18nLogMissing", "i18n: Log Missing Keys (requires flag -debug as well)");
            printOption("-i18nTraceMissing", "i18n: Stack Trace Missing Keys");
            printOption("-i18nShowKeys", "i18n: Show Keys instead of Translation");
        }
    }

    private void printArgs(String[] args) {
        for (int i=0; i<args.length; i++) {
            System.out.println("ARGS[" + i + "] = " + args[i]);
        }
    }

    public void checkArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {

            // "official" options
            if (args[i].equals("-help")) {
                printUsage(null);
                args[i] = null;
                continue;
            }
            if (args[i].equals("-javaRestart")) {
                Daten.javaRestart = true;
                args[i] = null;
                continue;
            }

            // developer options
            if (args[i].equals("-debug")) {
                Logger.setDebugLogging(true,true);
                args[i] = null;
                continue;
            }
            if (args[i].equals("-debugAll")) {
                Logger.setDebugLogging(true,true);
                Logger.setTraceLevel(9, true);
                Logger.setTraceTopic("0xFFFFFFFF",true);
                args[i] = null;
                continue;
            }
            if (args[i].equals("-traceTopic")) {
                if (args.length > i+1) {
                    Logger.setTraceTopic(args[i+1],true);
                    args[i] = null;
                    args[++i] = null;
                }
                continue;
            }
            if (args[i].equals("-traceLevel")) {
                if (args.length > i+1) {
                    try {
                        int level = Integer.parseInt(args[i+1]);
                        Logger.setTraceLevel(level,true);
                    } catch(Exception eingore) {
                    }
                    args[i] = null;
                    args[++i] = null;
                }
                continue;
            }
            if (args[i].equals("-logToStdOut")) {
                Logger.setLoggingToStdOut(true);
                args[i] = null;
                continue;
            }
            if (args[i].equals("-helpdev")) {
                printUsage(args[i]);
                args[i] = null;
                continue;
            }
            if (args[i].equals("-wws")) {
                Daten.watchWindowStack = true;
                args[i] = null;
                continue;
            }
            if (args[i].equals("-exc")) {
                Daten.exceptionTest = true;
                args[i] = null;
                continue;
            }
            if (args[i].equals("-printargs")) {
                printArgs(args);
                args[i] = null;
                continue;
            }
            if (args[i].equals("-emulateWin")) {
                System.setProperty("os.name","Windows XP");
                System.setProperty("os.arch","x86");
                System.setProperty("os.version","5.1");
            }
            if (args[i].equals("-i18nMarkMissing")) {
                International.setMarkMissingKeys(true);
                args[i] = null;
                continue;
            }
            if (args[i].equals("-i18nLogMissing")) {
                International.setLogMissingKeys(true);
                args[i] = null;
                continue;
            }
            if (args[i].equals("-i18nTraceMissing")) {
                International.setTraceMissingKeys(true);
                args[i] = null;
                continue;
            }
            if (args[i].equals("-i18nShowKeys")) {
                International.setShowKeys(true);
                args[i] = null;
                continue;
            }
        }
    }

    public void checkRemainingArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                printUsage(args[i]);
            }
        }
    }

    public int restart() {
        if (Daten.javaRestart) {
            return Daten.HALT_JAVARESTART;
        } else {
            return Daten.HALT_SHELLRESTART;
        }
    }

    public void exit(int exitCode) {
        if (Daten.efaRunning != null) {
            Daten.efaRunning.closeServer();
            Daten.efaRunning.stopDataLockThread();
        }
        System.exit(exitCode);
    }

}
