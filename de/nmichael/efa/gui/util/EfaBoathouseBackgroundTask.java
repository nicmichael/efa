/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.gui.util;

import java.awt.Component;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

import javax.swing.SwingUtilities;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.BoatDamageRecord;
import de.nmichael.efa.data.BoatDamages;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.BoatStatus;
import de.nmichael.efa.data.BoatStatusRecord;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.MessageRecord;
import de.nmichael.efa.data.Messages;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.EfaBaseFrame;
import de.nmichael.efa.gui.EfaBoathouseFrame;
import de.nmichael.efa.gui.EfaExitFrame;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

public class EfaBoathouseBackgroundTask extends Thread {

    private static final int CHECK_INTERVAL = 60;
    private static final int REMOTE_SCN_CHECK_INTERVAL = 5;
    private static final long HOURLY_INTERVAL = 60 * 60 * 1000L; // one hour in ms
    private static final long FOCUS_TIMEOUT = 3 * 60 * 1000L;    // 3 minutes without a list focused
    private static final long BOAT_DAMAGE_REMINDER_INTERVAL = 7*24*60*60*1000;
    private EfaBoathouseFrame efaBoathouseFrame;
    private boolean isProjectOpen = false;
    private boolean isLocalProject = true;
    private long nextHourlyTasksAt; // time-based, replaces the loop counter onceAnHour
    private Calendar lockEfa;
    private volatile long lastEfaConfigScn = -1;
    private long lastBoatStatusScn = -1;
    private long newBoatStatusScn = -1;
    private long lastReservationStatusScn = -1;
    private long newReservationStatusScn =-1;
    private long lastPersonsScn = -1;
    private long newPersonsScn = -1;

    private long lastListUpdate = -1;
	private volatile long nonListFocusSince = -1; // time when the current non-list focus started, replaces the loop counter
	private volatile Component lastFocussedElement;
    // Retry handling for a failed automatic logbook switch (see autoOpenNewLogbook)
    private static final long LOGBOOK_SWITCH_RETRY_INTERVAL = 10 * 60 * 1000; // 10 minutes
    private String failedLogbookSwitchKey = null;
    private long lastLogbookSwitchFailure = -1;

    public EfaBoathouseBackgroundTask(EfaBoathouseFrame efaBoathouseFrame) {
        this.efaBoathouseFrame = efaBoathouseFrame;
        this.nextHourlyTasksAt = System.currentTimeMillis() + 5 * 60 * 1000L; // first run after ~5 minutes, as before
        this.lockEfa = null;
    }

    /** 
     * Sets the time when efa shall be locked. If efa is already locked, this method does nothing.
     * It is used when the background task detects that efa is actually locked to document the time when it was locked.
     * @param datum	
     * @param zeit
     */
    private void setEfaLockBegin(DataTypeDate datum, DataTypeTime zeit) {
        if (Daten.efaConfig.getValueEfaDirekt_locked()) {
            lockEfa = null; // don't lock twice
            return;
        }
        if (datum == null || !datum.isSet()) {
            lockEfa = null;
        } else {
            if (zeit != null && zeit.isSet()) {
                lockEfa = new GregorianCalendar(datum.getYear(), datum.getMonth() - 1, datum.getDay(), zeit.getHour(), zeit.getMinute());
            } else {
                lockEfa = new GregorianCalendar(datum.getYear(), datum.getMonth() - 1, datum.getDay());
            }
        }
    }

    /**
	 * Checks the efa log file for warnings and mails them to the admin if there are any.
	 * The last time this check was done is stored in the efaConfig.
	 */
    private void mailWarnings() {
        try {
            if (Daten.efaLogfile == null || Daten.efaConfig == null) {return;}
            
            // Why count and cap: an efa.log can be about 10 MBytes. Keeping every warning in a list and
            // concatenating them is slow. We count all warnings, but keep only the first ones.
            final int maxWarningsInMail = 50;
            int warningCount = 0;
            
            StringBuilder warningText = new StringBuilder();
            try (BufferedReader f = new BufferedReader(new FileReader(Daten.efaLogfile))){
	            String s;
	            long lasttime = Daten.efaConfig.getValueEfaDirekt_bnrWarning_lasttime();
	            while ((s = f.readLine()) != null) {
	                if (Logger.isWarningLine(s) && Logger.getLineTimestamp(s) > lasttime) {
	                    warningCount++;
	                    // we count all warnings, but only keep the first ones in the mail text, 
	                    // to avoid memory issues with large log files
	                    if (warningCount <= maxWarningsInMail) {
	                        warningText.append(s).append('\n');
	                    }
	                }
	                // an efa.log can be about 10MBytes in size.
	                // if an shutdown is requested, we should not continue to read the whole file, 
	                // but stop reading it and return. mailwarnings will be called again in the next cycle of the background task.
	                if (isAdminModeOrShutdownRequested("mailWarnings")) {
	                    break; //skip the rest of the iterations
	                }
	            }
            } //try-with-resources closes f automatically
            
            if (isAdminModeOrShutdownRequested("mailWarnings")) {
                return; //stop all the rest.
            }
            if (warningCount == 0) {
                Logger.log(Logger.INFO, Logger.MSG_EVT_CHECKFORWARNINGS,
                        International.getMessage("Seit {date} sind keinerlei Warnungen in efa verzeichnet worden.",
                        EfaUtil.getTimeStamp(Daten.efaConfig.getValueEfaDirekt_bnrWarning_lasttime())));
            } else {
                Logger.log(Logger.INFO, Logger.MSG_EVT_CHECKFORWARNINGS,
                        International.getMessage("Seit {date} sind {n} Warnungen in efa verzeichnet worden.",
                        EfaUtil.getTimeStamp(Daten.efaConfig.getValueEfaDirekt_bnrWarning_lasttime()), warningCount));
                String txt = International.getMessage("Folgende Warnungen sind seit {date} in efa verzeichnet worden:",
                        EfaUtil.getTimeStamp(Daten.efaConfig.getValueEfaDirekt_bnrWarning_lasttime())) + "\n"
                        + International.getMessage("{n} Warnungen", warningCount) + "\n\n";
                txt += warningText.toString();
                if (warningCount > maxWarningsInMail) {
                    txt += "\n"+International.getMessage("({additionalwarncount} weitere Warnungen sind in der Logdatei verzeichnet)", (warningCount - maxWarningsInMail))+"\n";
                }
                if (Daten.project != null && Daten.efaConfig != null) {
                    Messages messages = Daten.project.getMessages(false);
                    if (messages != null && Daten.efaConfig.getValueEfaDirekt_bnrWarning_admin()) {
                        messages.createAndSaveMessageRecord(MessageRecord.TO_ADMIN, International.getString("Warnungen"), txt);
                    }
                    if (messages != null && Daten.efaConfig.getValueEfaDirekt_bnrWarning_bootswart()) {
                        messages.createAndSaveMessageRecord(MessageRecord.TO_BOATMAINTENANCE, International.getString("Warnungen"), txt);
                    }
                }
            }
            if (Daten.efaConfig != null) {
                Daten.efaConfig.setValueEfaDirekt_bnrWarning_lasttime(System.currentTimeMillis());
                //@efaconfig Daten.efaConfig.writeFile();
            }

        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_ERR_CHECKFORWARNINGS,
                    "Checking Logfile for Warnings and mailing them to Admin failed: " + e.toString());
        }
    }

    /**
	 * The main loop of the background task. It runs in a separate thread and performs various checks and updates.
	 */
    public void run() {
        // Diese Schleife läuft i.d.R. einmal pro Minute
        setName("EfaBoathouseBackgroundTask");
        while (Daten.isShutdownRequested() == false) {
            try {
                if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK, "EfaBoathouseBackgroundTask: alive!");
                }
                
                //Daten.isAdminMode is true if Application is efaBths AND Admin mode is true.
                //if efaBths is in admin mode, stop the efaBthsBackgroundTask actions, as in admin mode some
                //critical changes can take place like changing the current project, closing the project due to backup/restore functions
                //and more.
                //so, in isAdminMode=true mode we only sleep for a while...
                if (!isAdminModeOrShutdownRequested("main loop")) {
                	
	                // find out whether a project is open, and whether it's local or remote
	                updateProjectInfo();
	
	                // Update GUI on Config Changes
	                runCheck("checkUpdateGui", this::checkUpdateGui);
	
	                // Reservierungs-Checker
	                runCheck("checkBoatStatus", this::checkBoatStatus);
	
	                // Nach ungelesenen Nachrichten für den Admin suchen
	                runCheck("checkForUnreadMessages", this::checkForUnreadMessages);
	
	                // automatisches Beenden von efa
	                checkForExitOrRestart();
	
	                // efa zeitgesteuert sperren
	                runCheck("checkForLockEfa", this::checkForLockEfa);
	
	                // automatisches Beginnen eines neuen Fahrtenbuchs (z.B. zum Jahreswechsel)
	                checkForAutoCreateNewLogbook();
	
	                // immer im Vordergrund
	                runCheck("checkAlwaysInFront", this::checkAlwaysInFront);
	
	                // Fokus-Kontrolle
	                runCheck("checkFocus", this::checkFocus);
	
	                // Filter-Felder leeren nach Zeitintervall
	                runCheck("checkFilterTextFields", this::checkFilterTextFields);
	                
	                // Speicher-Überwachung
	                checkMemory();
	
	                // Aktivitäten einmal pro Stunde (zeitbasiert, damit zusätzliche Schleifendurchläufe durch Interrupts sie nicht auslösen)
	                long nowMs = System.currentTimeMillis();
	                if (nowMs >= nextHourlyTasksAt) {
	                    nextHourlyTasksAt = nowMs + HOURLY_INTERVAL;
	                    System.gc(); // Damit Speicherüberwachung funktioniert (anderenfalls wird CollectionUsage nicht aktualisiert; Java-Bug)
	                    if (Logger.isTraceOn(Logger.TT_BACKGROUND)) {
	                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK, "EfaBoathouseBackgroundTask: alive!");
	                    }
	
	                    runCheck("checkWarnings", this::checkWarnings);
	
	                    runCheck("checkUnfixedBoatDamages", this::checkUnfixedBoatDamages);
	
	                    runCheck("remindAdminOfLogbookSwitch", this::remindAdminOfLogbookSwitch);
	                }
	            }
                
                sleepForAWhile();

            } catch (Exception eglobal) {
                Logger.log(eglobal);
            }
        } // end: while(true)
        EfaUtil.foo();
    } // end: run

    /**
	 * Runs a check in a try-catch block and logs any exceptions that occur.
	 * If the application is in admin mode or a shutdown has been requested, the check is skipped.
	 *
	 * @param name  the name of the check for logging purposes
	 * @param check the Runnable representing the check to be executed
	 */
    private void runCheck(String name, Runnable check) {
        // Why isolate each check: an exception in one check must not silently skip
        // all remaining checks of the same cycle. The failing check's name is logged
        // so that a permanently failing check can be identified in the log.
    	
        // Why check the shutdown flag per check: shutdown can be requested in the middle
        // of a cycle of efaBoathouseBackgroundtask. 
    	// The remaining checks must then not touch efa's data any more.
        if (isAdminModeOrShutdownRequested(name)) {
            return;
        }
        try {
            check.run();
        } catch (RuntimeException e) {
            Logger.log(Logger.ERROR, Logger.MSG_GENERIC_ERROR,
                    "EfaBoathouseBackgroundTask: check '" + name + "' failed");
            Logger.log(e);
        }
    }

    /** 
     * Invokes an event where efaBoathouseFrame clears the filter text fields after a certain interval.
     * Which interval is defined in efaBoathouseFrame.clearListFilterAfterInterval() method.
     */
    private void checkFilterTextFields() {
    	SwingUtilities.invokeLater(new Runnable() {
    	      public void run() {
    	    	  if (efaBoathouseFrame!=null) {
    	    		  efaBoathouseFrame.clearListFilterAfterInterval();
    	    	  }
    	      }
      	});
    }
    
    /**
	 * Updates the information about whether a project is open and whether it is local or remote.
	 * This information is used to determine how to handle certain operations in the background task.
	 */
    private void updateProjectInfo() {
        try {
            if (Daten.project != null) {
                isProjectOpen = true;
                if (Daten.project.getProjectStorageType() == IDataAccess.TYPE_FILE_XML
                        || Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD
                ) {
                    isLocalProject = true;
                } else {
                    isLocalProject = false;
                }
            } else {
                isProjectOpen = false;
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
    }

    /**
	 * Sleeps for a while, either for a fixed interval 
	 * or until a change in boat statu is detected, 
	 * depending on whether a project is open and whether it is in admin mode.
	 *  
	 * If a project is open and app not in admin mode, the method checks for changes in boat status
	 * and boat reservations every REMOTE_SCN_CHECK_INTERVAL seconds, 
	 * and wakes up earlier if a change is detected.
	 *  
	 * If no project is open or if in admin mode, the method sleeps for CHECK_INTERVAL seconds.
	 */
    private void sleepForAWhile() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: sleepForAWhile()");
        }
        // Don't run Backround operations in Admin mode, or if no project is open. 
        // SleepForAWhile DOES make some checks like getting Boatstatuses and Boatreservations, 
        // and this is not good when creating new projects, in special efaCloud projects.
        if (!isProjectOpen || Daten.isAdminMode()) {
            // sleep 60 seconds
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                        "EfaBoathouseBackgroundTask: sleep for " + CHECK_INTERVAL + " seconds ...");
            }
            try {
                Thread.sleep(CHECK_INTERVAL * 1000);
            } catch (Exception e) {
                // wenn unterbrochen, dann versuch nochmal, kurz zu schlafen, und arbeite dann weiter!! ;-)
            	// Note added by Stefan Gebers:
            	// this exception here is thrown when the thread is interrupted, meaning: woken up to Update it's data.
            	//
            	// the former boat lists in efaBoatHouse main screen take some 100 msec to get an update on an raspberry pi 3.
            	// the new two column lists take some 500-800 msec to get an update, depending on number of boats, 
            	// reservations and boats on the water.
            	// the old code told to sleep 100 msec here. in combination with two column lists this lead to some exceptions
            	// in the efa.log file concerning some exceptions in the java swing code (updating jLists).
            	// Increasing this secondary sleep to more than 600 msec resolved the problem.
            	// 
            	// What does sleeping 700 msec at this station mean?
            	// we are in the efaBoathouseBackgroundTask. Due to some changes in the data (like: adding reservations, adding, editing, finishing trips)
            	// we get woken up to force the update of efaBoathouse boat lists. 
            	// the user CAN actually work within efaBths, as this is an background task and does not affect the responsiveness of the swing gui.
            	// 
            	// so after being woken up after some changes in the data, the list contents get updated in less than a second AFTER the data has been changed.
            	// this is fast enough for the common user.
                try {
                    Thread.sleep(600);
                } catch (Exception ee) {
                    EfaUtil.foo();
                }
            }
        } else {
            // sleep at most 60 seconds, but wake up earlier if boat status has changed
            int cnt = CHECK_INTERVAL / REMOTE_SCN_CHECK_INTERVAL;
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                        "EfaBoathouseBackgroundTask: sleep for " + cnt + " times " + REMOTE_SCN_CHECK_INTERVAL + " seconds ...");
            }
            BoatStatus boatStatus = null;
            try {
                boatStatus = (Daten.project != null ? Daten.project.getBoatStatus(false) : null);
            } catch(Exception e) {
                Logger.logdebug(e);
            }
            
            BoatReservations boatReservations=null;
            try {
            	boatReservations = (Daten.project != null ? Daten.project.getBoatReservations(false) : null);
            } catch(Exception e) {
                Logger.logdebug(e);
            }
            
            Persons persons=null;
            try {
            	persons = (Daten.project != null ? Daten.project.getPersons(false) : null);
            } catch(Exception e) {
                Logger.logdebug(e);
            }
            
            for (int i=0; i<cnt; i++) {
                if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                            "EfaBoathouseBackgroundTask: sleep for " + REMOTE_SCN_CHECK_INTERVAL + " seconds ...");
                }
                try {
                    Thread.sleep(REMOTE_SCN_CHECK_INTERVAL * 1000);
                } catch(Exception e) {
                    // wenn unterbrochen, dann versuch nochmal, kurz zu schlafen, und arbeite dann weiter!! ;-)
                    try {
                        // see comment above in this method
                    	Thread.sleep(600);
                    } catch (Exception ee) {
                        EfaUtil.foo();
                    }
                }
                try {
                    newBoatStatusScn = (boatStatus != null ? boatStatus.data().getSCN() : -1);
                    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                                "EfaBoathouseBackgroundTask: BoatStatus scn is " + newBoatStatusScn + " (previously " + lastBoatStatusScn +")");
                    }
                    if (newBoatStatusScn != -1 && newBoatStatusScn != lastBoatStatusScn) {
                        // do NOT set lastBoatStatusScn = scn here! This will be done when boat status is
                        // updated.
                        break;
                    }
                } catch(Exception e) {
                    Logger.logdebug(e);
                }
                try {
                    newReservationStatusScn = (boatReservations!= null ? boatReservations.data().getSCN() : -1);
                    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                                "EfaBoathouseBackgroundTask: BoatReservations scn is " + newReservationStatusScn + " (previously " + lastReservationStatusScn +")");
                    }
                    if (newReservationStatusScn != -1 && newReservationStatusScn != lastReservationStatusScn) {
                        // do NOT set lastReservationStatusScn = scn here! This will be done when boat status is
                        // updated.
                        break;
                    }
                } catch(Exception e) {
                    Logger.logdebug(e);
                }            
                
                try {
                    newPersonsScn = (persons!= null ? persons.data().getSCN() : -1);
                    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                                "EfaBoathouseBackgroundTask: Persons scn is " + newPersonsScn + " (previously " + lastPersonsScn +")");
                    }
                    if (newPersonsScn != -1 && newPersonsScn != lastPersonsScn) {
                        // do NOT set lastPersonsScn = scn here! This will be done when person status is
                        // updated.
                        break;
                    }
                } catch(Exception e) {
                    Logger.logdebug(e);
                }     
            }
        }
    }

    /**
	 * Checks if the GUI needs to be updated based on changes in the efaConfig.
	 * If the SCN (System Change Number) of the efaConfig has changed since the last check,
	 * the GUI elements of the efaBoathouseFrame are updated accordingly.
	 * */
    private void checkUpdateGui() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkUpdateGui()");
        }
        if (Daten.efaConfig != null) {
            try {
                long scn = Daten.efaConfig.data().getSCN();
                if (scn != lastEfaConfigScn) {
                	// This includes swing updates, so we have to use invokeLater to avoid concurrency problems
                	SwingUtilities.invokeLater(new Runnable() {
              	      public void run() {
              	    	  if (efaBoathouseFrame!=null) {
              	    		  efaBoathouseFrame.updateGuiElements();
              	    	  }
              	      }
                	});
                    lastEfaConfigScn = scn;
                }
            } catch (Exception e) {
                Logger.logdebug(e);
            }
        }
    }

/**
     * Marks the currently observed boat status and reservation SCNs as processed.
     * Must only be called after the corresponding change has actually been handled,
     * otherwise the change would not be detected again in the next cycle.
     */
    private void markScnsProcessed() {
        lastBoatStatusScn = newBoatStatusScn;
        lastReservationStatusScn = newReservationStatusScn;
        lastPersonsScn = newPersonsScn;
    }

    /**
     * Checks the status of boats and updates the boat lists in the efaBoathouseFrame
     * if there are any changes in the boat status or reservations.
     */
    private void checkBoatStatus() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkBoatStatus()");
        }

        // Some changes in boat status or reservations? Then the lists in efaBoatHouseFrame need update.
        // The SCNs are NOT consumed here: they are marked as processed only once the change
        // was actually handled (see markScnsProcessed), otherwise a change could be lost.
        
        //we also check for changes in the Persons SCN, as this can also affect the boat lists 
        // (e.g. if a person is inserted, updated, deleted). we do not need a separate persons check here.
        boolean listChanged = (newBoatStatusScn != -1 && newBoatStatusScn != lastBoatStatusScn)
                || (newReservationStatusScn != -1 && newReservationStatusScn != lastReservationStatusScn)
                || (newPersonsScn != -1 && newPersonsScn != lastPersonsScn);
        
        if (isProjectOpen && !isLocalProject) {
            markScnsProcessed(); // remote project: lists are updated from the observed SCNs
        	// This includes swing updates, so we have to use invokeLater to avoid concurrency problems
	        SwingUtilities.invokeLater(new BthsUpdateBoatLists(listChanged,false));
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                        "EfaBoathouseBackgroundTask: checkBoatStatus() - done for remote project");
            }
            return;
        }
        

        if (!isProjectOpen) {
            markScnsProcessed(); // closed project: lists are updated, the change is handled
	        SwingUtilities.invokeLater(new BthsUpdateBoatLists(listChanged,false));
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                        "EfaBoathouseBackgroundTask: checkBoatStatus() - done for closed project");
            }	        
            return;
        }

        BoatStatus boatStatus = (Daten.project != null ? Daten.project.getBoatStatus(false) : null);
        BoatReservations boatReservations = (Daten.project != null ? Daten.project.getBoatReservations(false) : null);
        BoatDamages boatDamages = (Daten.project != null ? Daten.project.getBoatDamages(false) : null);
        // Mark the change as processed in all cases. If data is unavailable, retrying every 5 seconds
        // would not help and would keep the background loop awake; the lists are refreshed on the next change.
        markScnsProcessed();
        if (boatStatus == null || boatReservations == null || boatDamages == null) {
            return;
        }

        long now = System.currentTimeMillis();
        try {
            // The reservation comment template does not depend on the boat, so it is computed once
            // per cycle here instead of being looked up again for every boat in the loop below.
            String reservationCommentPrefix = International.getMessage("reserviert für {name} ({reason}) {from_to}", "", "", "");
            if (reservationCommentPrefix.length() > 10) {
                reservationCommentPrefix = reservationCommentPrefix.substring(0, 10);
            }
            DataKeyIterator it = boatStatus.data().getStaticIterator();
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
            	//check for admin mode or shutdown request - and stop the iteration
				if (isAdminModeOrShutdownRequested("checkBoatStatus")) {
					break; //skip the rest of the iterations
				}
            	
                BoatStatusRecord boatStatusRecord = (BoatStatusRecord) boatStatus.data().get(k);
                if (boatStatusRecord == null) {
                    continue;
                }
                try {
                    String oldCurrentStatus = boatStatusRecord.getCurrentStatus();
                    String oldShowInList = boatStatusRecord.getShowInList();
                    String oldComment = boatStatusRecord.getComment();

                    // set CurrentStatus correctly
                    if (!BoatStatusRecord.STATUS_ONTHEWATER.equals(boatStatusRecord.getCurrentStatus())
                            && !boatStatusRecord.getBaseStatus().equals(boatStatusRecord.getCurrentStatus())) {
                        boatStatusRecord.setCurrentStatus(boatStatusRecord.getBaseStatus());
                    }

                    if (BoatStatusRecord.STATUS_HIDE.equals(boatStatusRecord.getCurrentStatus())) {
                        if (boatStatusRecord.getShowInList() != null && !boatStatusRecord.getShowInList().equals(BoatStatusRecord.STATUS_HIDE)) {
                            boatStatusRecord.setShowInList(null);
                            boatStatus.data().update(boatStatusRecord);
                            listChanged = true;
                        }
                        // do not move to the next boatStatusRecord for hidden Boats. 
                        // reservations and damages for hidden boats shall be taken care of as well.
                        //continue; 
                    }
                    if (boatStatusRecord.getUnknownBoat()) {
                        if (!BoatStatusRecord.STATUS_ONTHEWATER.equals(boatStatusRecord.getCurrentStatus())) {
                            boatStatus.data().delete(boatStatusRecord.getKey());
                            listChanged = true;
                        }
                        continue;
                    }

                    // delete any obsolete revervations
                    int purgedRes = boatReservations.purgeObsoleteReservations(boatStatusRecord.getBoatId(), now);

                    // find all currently valid reservations
                    BoatReservationRecord[] reservations = boatReservations.getBoatReservations(boatStatusRecord.getBoatId(), now, 0);
                    if (reservations == null || reservations.length == 0) {
                        // no reservations at the moment - nothing to do
                        if (!BoatStatusRecord.STATUS_ONTHEWATER.equals(boatStatusRecord.getCurrentStatus())
                                && boatStatusRecord.getShowInList() != null && !boatStatusRecord.getShowInList().equals(boatStatusRecord.getCurrentStatus())) {
                            boatStatusRecord.setShowInList(null);
                        }

                        if (purgedRes > 0) {
                            boatStatusRecord.setComment(null);
                        } else {
                            // wow, now this is a hack!
                            // If there is a comment for this boat that *looks* as if it was a
                            // reservation comment, remove it! This might not work for all languages,
                            // but for some...
                            // Reason for such reservation strings could be reservations that were
                            // explicitly deleted by the Admin while a boat was reserved, and have
                            // never been purged by the background task itself.
                            if (oldComment != null && oldComment.startsWith(reservationCommentPrefix)) {
                                boatStatusRecord.setComment(null);
                            }
                        }
                    } else {
                        // reservations found
                        if (!BoatStatusRecord.STATUS_ONTHEWATER.equals(boatStatusRecord.getCurrentStatus())) {
                            if (Daten.efaConfig.getValueEfaDirekt_resBooteNichtVerfuegbar()) {
                                //Show reserved boats in the "not available" list if configured
                            	//"Hidden" Boats have no list they are shown by default, and thus return NULL as getShowInList.
                            	// but also hidden boats may move to the "not available" list if there is a reservation.
                            	if ((boatStatusRecord.getShowInList()==null) || 
                            			!boatStatusRecord.getShowInList().equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
                                    boatStatusRecord.setShowInList(BoatStatusRecord.STATUS_NOTAVAILABLE);
                                }
                            } else {
                                if ((boatStatusRecord.getShowInList()==null) || 
                                		!boatStatusRecord.getShowInList().equals(boatStatusRecord.getBaseStatus())) {
                                    boatStatusRecord.setShowInList(boatStatusRecord.getBaseStatus());
                                }
                            }
                            // only set reservation comment if boat is not on the water. 
                            // If boat is on the water, the reservation comment would be confusing, as we would expect the comment
                            // to show who is on the water.
                            String s = International.getMessage("reserviert für {name} ({reason}) {from_to}",
                                    reservations[0].getPersonAsName(),
                                    reservations[0].getReason(),
                                    reservations[0].getReservationTimeDescription());
                            boatStatusRecord.setComment(s);
                        }

                    }

                    // find all current damages
                    boolean damaged = false;
                    BoatDamageRecord[] damages = boatDamages.getBoatDamages(boatStatusRecord.getBoatId());
                    for (int i = 0; damages != null && i < damages.length; i++) {
                        if (!damages[i].getFixed() && damages[i].getSeverity() != null
                                && damages[i].getSeverity().equals(BoatDamageRecord.SEVERITY_NOTUSEABLE)) {
                            boatStatusRecord.setComment(damages[i].getShortDamageInfo());
                            damaged = true;
                            if (!BoatStatusRecord.STATUS_NOTAVAILABLE.equals(boatStatusRecord.getShowInList())) {
                                boatStatusRecord.setShowInList(BoatStatusRecord.STATUS_NOTAVAILABLE);
                            }
                            break; // stop after first severe damage
                        }
                    }
                    if (!damaged && boatStatusRecord.getComment() != null &&
                        BoatDamageRecord.isCommentBoatDamage(boatStatusRecord.getComment())) {
                        boatStatusRecord.setComment(null);
                    }

                    // make sure that if the boat is on the water, this status overrides any other list settings
                    if (BoatStatusRecord.STATUS_ONTHEWATER.equals(boatStatusRecord.getCurrentStatus())) {
                        if (boatStatusRecord.isOnTheWaterShowNotAvailable()) {
                            boatStatusRecord.setShowInList(BoatStatusRecord.STATUS_NOTAVAILABLE);
                        } else {
                            boatStatusRecord.setShowInList(BoatStatusRecord.STATUS_ONTHEWATER);
                        }
                    }

                    boolean statusRecordChanged = false;
                    if (oldCurrentStatus == null
                            || !oldCurrentStatus.equals(boatStatusRecord.getCurrentStatus())) {
                        statusRecordChanged = true;
                    }
                    if ((!BoatStatusRecord.STATUS_HIDE.equals(boatStatusRecord.getCurrentStatus())) 
                    	&& (oldShowInList == null || !oldShowInList.equals(boatStatusRecord.getShowInList()))) {
                    	//if the boat is hidden, oldShowInList is always null. 
                    	//if we would not check again for a hidden boat in this location, 
                    	//efa would always update a boat status, leading to updates for the record every 10 Seconds or so.
                    	//as a consequence, the focus would be set to the availableBoatList in efaBoatHouse also every 10 seconds.
                        statusRecordChanged = true;
                    }
                    if ((oldComment == null && boatStatusRecord.getComment() != null && boatStatusRecord.getComment().length() > 0)
                            || (oldComment != null && !oldComment.equals(boatStatusRecord.getComment()))) {
                        statusRecordChanged = true;
                    }

                    if (statusRecordChanged) {
                        boatStatus.data().update(boatStatusRecord);
                        listChanged = true;
                    }
                    if (statusRecordChanged && Logger.isTraceOn(Logger.TT_BACKGROUND, 2)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                                "BoatStatus changed for Boat " + boatStatusRecord.getBoatNameAsString(now)
                                + ", new Status: " + boatStatusRecord.toString());
                    }
                } catch (Exception ee) {
                    Logger.logdebug(ee);
                }
            }
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                        "EfaBoathouseBackgroundTask: checkBoatStatus() - calling updateBoatLists("+listChanged+") ...");
            }
            if (now-lastListUpdate>10*60*1000) {
            	// if Boathouse lists shall contain reservation info, they need to be updated regularly.
            	// reservation info consists of when a boat has its next reservation on the current day,
            	// and this is not computed by this background task. The lists compute this info on their own.
            	// update every 10 minutes, after the last update of the boat lists.
            	// as boat lists compute reservations for the remaining day, and are updated every time a reservation
            	// is added, updated, deleted, the regular update of the boatlists is neccesary at 0:00 am,  
            	// when a new day has begun. 
            	// however, we update every 10 minutes to be sure.
            	
            	boolean showReservationInfo = Daten.efaConfig != null
            	        && Daten.efaConfig.getValueEfaBoathouseBoatListReservationInfo();
            	if (showReservationInfo && now - lastListUpdate > 10 * 60 * 1000) {
            	    listChanged = true;
            	}

            }
            if (listChanged) {
            	lastListUpdate=now;
                
            	// This includes swing updates, so we have to use invokeLater to avoid concurrency problems
            	SwingUtilities.invokeLater(new BthsUpdateBoatLists(listChanged, false));

            }

            
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 9)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                        "EfaBoathouseBackgroundTask: checkBoatStatus() - done");
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
    }
    
    /**
     * Checks for unread messages for the admin and boat maintenance users.
     * It updates the efaBoathouseFrame main screen with the information about unread messages.
	 */
    private void checkForUnreadMessages() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkForUnreadMessages()");
        }
        boolean admin = false;
        boolean boatmaintenance = false;

        Messages messages = (Daten.project != null ? Daten.project.getMessages(false) : null);

        if (messages != null) {
            // durchsuche die letzten 50 Nachrichten nach ungelesenen (aus Performancegründen immer nur die letzen 50)
            int i = 0;
            try {
                DataKeyIterator it = messages.data().getStaticIterator();
                DataKey k = it.getLast();
                while (k != null && !isAdminModeOrShutdownRequested("checkForUnreadMessages")) {
                    MessageRecord msg = (MessageRecord) messages.data().get(k);
                    if (msg != null && !msg.getRead()) {
                        // Compare with the constant first, so a null recipient cannot throw an NPE
                        // and abort the whole scan of the unread messages.
                        if (MessageRecord.TO_ADMIN.equals(msg.getTo())) {
                            admin = true;
                        }
                        if (MessageRecord.TO_BOATMAINTENANCE.equals(msg.getTo())) {
                            boatmaintenance = true;
                        }
                    }
                    if (++i == 50 || (admin && boatmaintenance)) {
                        break;
                    }
                    k = it.getPrev();
                }
            } catch (Exception e) {
                Logger.logdebug(e);
            }
        }

        // This includes swing updates, so we have to use invokeLater to avoid concurrency problems
    	SwingUtilities.invokeLater(new BthsSetUnreadMessages(admin, boatmaintenance));

    }

    /**
	 * Checks if the application should exit or restart based on 
	 * the configured exit time, idle time, or restart time.
	 * 
	 * If the conditions for exit or restart are met, 
	 * it schedules the exit or restart accordingly.
	 */
    private void checkForExitOrRestart() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkForExitOrRestart()");
        }
        
        if (efaBoathouseFrame == null) {
            return; // Sicherheitsprüfung
        }

        // Why return early: efaConfig is loaded asynchronously during startup.
        // Without it, every access below would throw a NullPointerException.
        // The next cycle retries, so no check is lost once the config is available.
        if (Daten.efaConfig == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // automatisches, zeitgesteuertes Beenden von efa ?
        if (Daten.efaConfig.getValueEfaDirekt_exitTime().isSet()
                && currentTime > Daten.efaStartTime + (Daten.AUTO_EXIT_MIN_RUNTIME + 1) * 60 * 1000) {
        	
            Calendar localCal = new GregorianCalendar();
            localCal.setTimeInMillis(currentTime);
            int now = localCal.get(Calendar.HOUR_OF_DAY) * 60 + localCal.get(Calendar.MINUTE);
            int exitTime = Daten.efaConfig.getValueEfaDirekt_exitTime().getHour() * 60 + Daten.efaConfig.getValueEfaDirekt_exitTime().getMinute();
            
            if ((now >= exitTime && now < exitTime + Daten.AUTO_EXIT_MIN_RUNTIME) || 
                (now + (24 * 60) >= exitTime && now + (24 * 60) < exitTime + Daten.AUTO_EXIT_MIN_RUNTIME)) {
            
            	Logger.log(Logger.INFO, Logger.MSG_EVT_TIMEBASEDEXIT,
                        International.getString("Eingestellte Uhrzeit zum Beenden von efa erreicht!"));
                checkUserActivityAndScheduleExit(EfaBoathouseFrame.EFA_EXIT_REASON_TIME, false,
                        International.getString("Zeitgesteuertes Beenden von efa"));
            
            }
        }

        // automatisches Beenden nach Inaktivität ?
        if (Daten.efaConfig.getValueEfaDirekt_exitIdleTime() > 0) {
            long lastUserInteraction = getLastUserInteractionSafely();
            if (lastUserInteraction >= 0 && 
                currentTime - lastUserInteraction > Daten.efaConfig.getValueEfaDirekt_exitIdleTime() * 60 * 1000) {
                Logger.log(Logger.INFO, Logger.MSG_EVT_INACTIVITYBASEDEXIT,
                        International.getString("Eingestellte Inaktivitätsdauer zum Beenden von efa erreicht!"));
                
                checkUserActivityAndScheduleExit(EfaBoathouseFrame.EFA_EXIT_REASON_IDLE, false,
                        International.getString("Zeitgesteuertes Beenden von efa"));
            }
        }

        // automatischer, zeitgesteuerter Neustart von efa ?
        if (Daten.efaConfig.getValueEfaDirekt_restartTime().isSet()
                && currentTime > Daten.efaStartTime + (Daten.AUTO_EXIT_MIN_RUNTIME + 1) * 60 * 1000) {
        	
            Calendar localCal = new GregorianCalendar();
            localCal.setTimeInMillis(currentTime);
            int now = localCal.get(Calendar.HOUR_OF_DAY) * 60 + localCal.get(Calendar.MINUTE);
            int restartTime = Daten.efaConfig.getValueEfaDirekt_restartTime().getHour() * 60 + Daten.efaConfig.getValueEfaDirekt_restartTime().getMinute();
            
            if ((now >= restartTime && now < restartTime + Daten.AUTO_EXIT_MIN_RUNTIME) || (now + (24 * 60) >= restartTime && now + (24 * 60) < restartTime + Daten.AUTO_EXIT_MIN_RUNTIME)) {
                Logger.log(Logger.INFO, Logger.MSG_EVT_TIMEBASEDRESTART, "Automatischer Neustart von efa (einmal täglich).");
                
                checkUserActivityAndScheduleExit(EfaBoathouseFrame.EFA_EXIT_REASON_AUTORESTART, true,
                        International.getString("Automatischer Neustart von efa"));
                
            }
        }
    }

    /**
     * Get the last user interaction time from the efaBoathouseFrame, safely handling any exceptions.
     * This method is EDT-safe as it uses invokeAndWait to access the GUI component from the background thread.
     * 
     * @return the timestamp of the last user interaction in milliseconds, or -1 if not available or an error occurs
     */
    private long getLastUserInteractionSafely() {
        if (efaBoathouseFrame == null) {
            return -1;
        }
        
        final long[] lastInteractionHolder = new long[1];
        lastInteractionHolder[0] = -1;
        
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        if (efaBoathouseFrame != null) {
                            lastInteractionHolder[0] = efaBoathouseFrame.getLastUserInteraction();
                        }
                    } catch (Exception e) {
                        Logger.logdebug(e);
                        lastInteractionHolder[0] = -1;
                    }
                }
            });
        } catch (InterruptedException e) {
            Logger.logdebug(e);
            Thread.currentThread().interrupt();  // Restore interrupt status
            return -1;
        } catch (InvocationTargetException e) {
            Logger.logdebug(e);
            return -1;
        }
        
        return lastInteractionHolder[0];
    }

    
    /**
     * Check if user has been active recently, and if not, schedule an exit or restart of efa.
     * @param exitReason
     * @param restart
     * @param message
     */
    private void checkUserActivityAndScheduleExit(final int exitReason, final boolean restart, final String message) {
        long lastUserInteraction = getLastUserInteractionSafely();
        
        if (lastUserInteraction >= 0 && 
            System.currentTimeMillis() - lastUserInteraction < Daten.AUTO_EXIT_MIN_LAST_USED * 60 * 1000) {
            String delayMsg = restart ? 
                "Neustart von efa wird verzögert, da efa innerhalb der letzten " + Daten.AUTO_EXIT_MIN_LAST_USED + " Minuten noch benutzt wurde ..." :
                International.getMessage("Beenden von efa wird verzögert, da efa innerhalb der letzten {n} Minuten noch benutzt wurde ...",
                        Daten.AUTO_EXIT_MIN_LAST_USED);
            Logger.log(Logger.INFO, restart ? Logger.MSG_EVT_TIMEBASEDRESTARTDELAY : Logger.MSG_EVT_TIMEBASEDEXITDELAY, delayMsg);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    EfaExitFrame.exitEfa(message, restart, exitReason);
                }
            });
        }
    }
    
    /**
	 * Checks if the EFA application is locked based on the configuration.
	 * 
	 * If locked, it invokes the lockEfa() method on the efaBoathouseFrame.
	 * If a lock time is set and has passed, it also locks the application.
	 * 
	 */
    private void checkForLockEfa() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkForLockEfa()");
        }
        if (Daten.efaConfig != null) {
            if (Daten.efaConfig.getValueEfaDirekt_locked()) {
            	SwingUtilities.invokeLater(new Runnable() {
            		public void run() {
            			if (efaBoathouseFrame!=null) {
            				efaBoathouseFrame.lockEfa();
            			}
            		}
            	});
                return;
            }
            setEfaLockBegin(Daten.efaConfig.getValueEfaDirekt_lockEfaFromDatum(),
                    Daten.efaConfig.getValueEfaDirekt_lockEfaFromZeit());
        }

        if (lockEfa != null) {
            Date localDate = new Date(System.currentTimeMillis());
            Calendar localCal = new GregorianCalendar();
            localCal.setTime(localDate);
            
            if (localCal.after(lockEfa) && efaBoathouseFrame != null) {
            	SwingUtilities.invokeLater(new Runnable() {
            		public void run() {
            			if (efaBoathouseFrame!=null) {
            				efaBoathouseFrame.lockEfa();
            			}
            		}
            	});
                lockEfa = null;
            }
        }
    }

    /**
     * Checks if a new logbook should be automatically created based on the configuration.
     * If the current date is after or equal to the configured auto new logbook date, it calls autoOpenNewLogbook().
     * 
     */
    private void checkForAutoCreateNewLogbook() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkForAutoCreateNewLogbook()");
        }
        try {
            if (Daten.project != null && Daten.project.isOpen()) {
                DataTypeDate newLogBookDate = Daten.project.getAutoNewLogbookDate();
                if (newLogBookDate != null && newLogBookDate.isSet()) {
                    DataTypeDate today = DataTypeDate.today();
                    if (today.isAfterOrEqual(newLogBookDate)) {
                        autoOpenNewLogbook();
                        if (today.getDifferenceDays(newLogBookDate) >= 7) {
                            // we only delete the logswitch data after 7 days to also give
                            // all remote clients a chance to change the logbook; otherwise,
                            // they wouldn't be able to see the configured date any more and
                            // would never change the logbook
                            Daten.project.setAutoNewLogbookDate(null);
                            Daten.project.setAutoNewLogbookName(null);
                            
                        }
                    }
                }
            }
        } catch (Exception e) {
            // can crash when project is currently being opened
            Logger.logdebug(e);
        }
    }

    /**
	 * Checks if the EFA application should always be in front based on the configuration.
	 * If the configuration is set to always be in front, it brings the efaBoathouseFrame to the front.
	 */
    private void checkAlwaysInFront() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkAlwaysInFront()");
        }
        // Why guard efaConfig: it is loaded asynchronously during startup (see checkForExitOrRestart).
        if (Daten.efaConfig != null && Daten.efaConfig.getValueEfaDirekt_immerImVordergrund() && this.efaBoathouseFrame != null) {
        		//Run all in the Swing Event Dispatch Thread to avoid concurrency problems with the GUI
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (efaBoathouseFrame == null || Dialog.frameCurrent() != efaBoathouseFrame) {
                            return;
                        }
                        
                        Window[] windows = efaBoathouseFrame.getOwnedWindows();
                        boolean topWindow = true;
                        if (windows != null) {
                            for (int i = 0; i < windows.length; i++) {
                                if (windows[i] != null && windows[i].isVisible()) {
                                    topWindow = false;
                                    break;
                                }
                            }
                        }
                        
                        if (topWindow && Daten.efaConfig.getValueEfaDirekt_immerImVordergrundBringToFront()) {
                        	if (efaBoathouseFrame != null) {
                        		efaBoathouseFrame.bringFrameToFront(true);
                        	}
                        }
                    }
                });
        }

    }
    
    /**
	 * Checks the focus of the EFA application and ensures that a list component has focus.
	 * If the focus is on the main frame or no list is focused, it will request focus for a list component.
	 * */
    private void checkFocus() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkFocus()");
        }
        if (this.efaBoathouseFrame != null) {
        	// run all in the Swing Event Dispatch Thread to avoid concurrency problems with the GUI
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (efaBoathouseFrame == null) return;
                    
                    Component focusOwner = efaBoathouseFrame.getFocusOwner();
                    long now = System.currentTimeMillis();
                    
                    // Lese den aktuellen Zustand einmal ab
                    final long currentNonListFocusSince;
                    final Component currentLastFocussedElement;
                    synchronized (EfaBoathouseBackgroundTask.this) {
                        currentNonListFocusSince = nonListFocusSince;
                        currentLastFocussedElement = lastFocussedElement;
                    }
                    
                    // Bestimme die neue Aktion
                    boolean shouldRequestFocus = false;
                    int focusRequestForList = 0;
                    long newFocusSince = currentNonListFocusSince;
                    Component newLastFocussedElement = currentLastFocussedElement;
                    
                    // Prüfe, ob Frame selbst den Fokus hat - Das soll nicht sein! Gib einer Liste den Fokus!
                    if (focusOwner == efaBoathouseFrame) {
                        newFocusSince = -1;
                        newLastFocussedElement = null;
                        shouldRequestFocus = true;
                        focusRequestForList = 0;
                    } else if (!efaBoathouseFrame.isAnyListFocused()) {
                        // No boatlist is focussed: track how long the same non-list element has the focus.
                        // A change of the focussed element means the user is still active, so the timer restarts.
                        if (currentLastFocussedElement != focusOwner || currentNonListFocusSince < 0) {
                            newFocusSince = now;
                        }
                        newLastFocussedElement = focusOwner;
                        
                        // After FOCUS_TIMEOUT without a boatlist focus, switch to the list of available boats.
                        if (now - newFocusSince >= FOCUS_TIMEOUT) {
                            newFocusSince = -1;
                            newLastFocussedElement = null;
                            shouldRequestFocus = true;
                            focusRequestForList = 1;//select "available boats" list
                        }
                    }
                    
                    // Aktualisiere den globalen Zustand
                    synchronized (EfaBoathouseBackgroundTask.this) {
	                    nonListFocusSince = newFocusSince;
	                    lastFocussedElement = newLastFocussedElement;
                    }
                    
                    // Führe GUI-Operationen aus (außerhalb des Sync-Blocks)
                    if (shouldRequestFocus && efaBoathouseFrame != null) {
                        efaBoathouseFrame.boatListRequestFocus(focusRequestForList);
                    }
                }
            });
        }
    }

    /**
     * Checks the memory usage of the application and exits if memory is low.
     * 
     */
    private void checkMemory() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkMemory()");
        }
        try {
            // System.gc(); // !!! ONLY ENABLE FOR DEBUGGING PURPOSES !!!
            if (de.nmichael.efa.java15.Java15.isMemoryLow(Daten.MIN_FREEMEM_PERCENTAGE, Daten.WARN_FREEMEM_PERCENTAGE)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (efaBoathouseFrame != null) {
                            efaBoathouseFrame.exitOnLowMemory("EfaBoathouseBackgroundTask: MemoryLow", false);
                        }
                    }
                });
            }
        } catch (UnsupportedClassVersionError e) {
            EfaUtil.foo();
        } catch (NoClassDefFoundError e) {
            EfaUtil.foo();
        }
    }

    /**
	 * Checks for warnings in the log file and sends them to admins if necessary.
	 * This is done every 7 days or if the last warning was sent more than 7 days ago.
	 */
    private void checkWarnings() {
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkWarnings()");
        }
        // Why return early: efaConfig may not be loaded yet during startup (see checkForExitOrRestart).
        if (Daten.efaConfig == null) {
            return;
        }
        // WARNINGs aus Logfile an Admins verschicken
        if (System.currentTimeMillis() >= Daten.efaConfig.getValueEfaDirekt_bnrWarning_lasttime() + 7l * 24l * 60l * 60l * 1000l
                && (Daten.efaConfig.getValueEfaDirekt_bnrWarning_admin() || Daten.efaConfig.getValueEfaDirekt_bnrWarning_bootswart()) && Daten.efaLogfile != null) {
            mailWarnings();
        }
    }

    /**
	 * Checks for unfixed boat damages and sends a reminder message if there are any.
	 * This is done every 7 days or if the last reminder was sent more than 7 days ago.
	 */
    private void checkUnfixedBoatDamages() {
        if (!isProjectOpen || !isLocalProject) {
            return;
        }
        if (Logger.isTraceOn(Logger.TT_BACKGROUND, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK,
                    "EfaBoathouseBackgroundTask: checkUnfixedBoatDamages()");
        }
        BoatDamages boatDamages = (Daten.project != null ? Daten.project.getBoatDamages(false) : null);
        Messages messages = (Daten.project != null ? Daten.project.getMessages(false) : null);
        if (boatDamages == null || messages == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long last = (Daten.efaConfig != null ? Daten.efaConfig.getValueLastBoatDamageReminder() : -1);
        if (last == -1 || now - BOAT_DAMAGE_REMINDER_INTERVAL > last) {
            boolean damagesOlderThanAWeek = false;
            ArrayList <BoatDamageRecord> openDamages = new ArrayList<BoatDamageRecord>();
            try {
                DataKeyIterator it = boatDamages.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                	
                	if (isAdminModeOrShutdownRequested("checkUnfixedBoatDamages")) {
                		return; // do not proceed if we need to shutdown or are in admin mode
					}
                    
                	BoatDamageRecord damage = (BoatDamageRecord) boatDamages.data().get(k);
                    if (damage == null) {
                        continue;
                    }
                    if (!damage.getFixed()) {
                        BoatRecord r = damage.getBoatRecord();
                        if (r != null && r.isValidAt(System.currentTimeMillis())) {
                            openDamages.add(damage); // keep the record itself, so it is not read a second time
                            if (damage.getReportDate() != null && damage.getReportTime() != null
                                    && damage.getReportDate().isSet() && damage.getReportTime().isSet()
                                    && damage.getReportDate().getTimestamp(damage.getReportTime()) < now - BOAT_DAMAGE_REMINDER_INTERVAL) {
                            	damagesOlderThanAWeek = true;
                            }
                        }
                    }

                }

                if (damagesOlderThanAWeek) {
                    StringBuilder s = new StringBuilder();
                    s.append(International.getMessage("Es liegen {count} offene Bootsschäden vor:",
                            openDamages.size()) + "\n\n");
                    for (BoatDamageRecord damage : openDamages) {
                        s.append(damage.getCompleteDamageInfo() + "\n");
                    }
                    s.append(International.getString("Sollten die Schäden bereits behoben sein, so markiere sie bitte "
                            + "in efa als behoben."));
                    messages.createAndSaveMessageRecord(MessageRecord.TO_BOATMAINTENANCE,
                            International.getString("Offene Bootsschäden"),
                            s.toString());
                    Daten.efaConfig.setValueLastBoatDamageReminder(now);
                }
            } catch (Exception e) {
                Logger.logdebug(e);
            }
        }
    }

    /**
	 * Reminds the admin to switch logbooks if the current logbook is nearing its end date.
	 * 
	 * This method checks if the current logbook's end date is within 31 days and if a reminder has not already been sent.
	 * If conditions are met, it creates a message for the admin reminding them to switch logbooks.
	 */
    private void remindAdminOfLogbookSwitch() {
        try {
            if (Daten.project != null && Daten.project.isOpen()) {
                DataTypeDate newLogBookDate = Daten.project.getAutoNewLogbookDate();
                
                if (newLogBookDate == null || !newLogBookDate.isSet()) {
                    Logbook currentLogbook = efaBoathouseFrame.getLogbook();
                    
                    if (currentLogbook != null && currentLogbook.getEndDate() != null &&
                        currentLogbook.getEndDate().isSet()) {
                        DataTypeDate today = DataTypeDate.today();
                        
                        if (today.isBefore(currentLogbook.getEndDate()) &&
                            today.getDifferenceDays(currentLogbook.getEndDate()) < 31) {
                        	
	                            String lastReminderForLogbook = Daten.efaConfig.getValueEfaBoathouseChangeLogbookReminder();
	                            if (!currentLogbook.getName().equals(lastReminderForLogbook)) {
	                            	
	                                // ok, it's due for a reminder
	                            	Messages myMessages =  Daten.project.getMessages(false);
	                            	if (myMessages != null) {
	                            	myMessages.createAndSaveMessageRecord(
	                                        MessageRecord.TO_ADMIN,
	                                        International.getString("Erinnerung an Fahrtenbuchwechsel"),
	                                        International.getMessage("Der Gültigkeitszeitraum des aktuellen Fahrtenbuchs {name} endet am {datum}.",
	                                                    currentLogbook.getName(), currentLogbook.getEndDate().toString()) + "\n" +
	                                        International.getString("Um anschließend automatisch ein neues Fahrtenbuch zu öffnen, erstelle bitte " +
	                                                    "im Admin-Modus ein neues Fahrtenbuch und aktiviere den automatischen Fahrtenbuchwechsel."));
	                                Daten.efaConfig.setValueEfaBoathouseChangeLogbookReminder(currentLogbook.getName());
	                            	}
	                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // can crash when project is currently being opened
            Logger.logdebug(e);
        }
    }

    /**
	 * Automatically opens a new logbook if the current date is after or equal to the configured auto new logbook date.
	 * 
	 * It checks if the new logbook name is set and different from the current logbook, and if so, it switches to the new logbook.
	 * It also handles aborting open sessions for local projects and logs any failures during the process.
	 */
    private void autoOpenNewLogbook() {
        if (Daten.project == null || !Daten.project.isOpen()) {
            return;
        }
        String newLogbookName = Daten.project.getAutoNewLogbookName();
        if (newLogbookName == null || newLogbookName.length() == 0) {
            return;
        }
        Logbook currentLogbook = efaBoathouseFrame.getLogbook();
        if (currentLogbook == null) {
            return;
        }
        if (newLogbookName.equals(currentLogbook.getName())) {
            // we have already changed the logbook --> nothing to do
            return;
        }
        
        // Logswitch Key (to identify whether we already switched logbooks)
        String dateString = (Daten.project.getAutoNewLogbookDate() != null && 
                       Daten.project.getAutoNewLogbookDate().isSet() ?
                       Daten.project.getAutoNewLogbookDate().toString() : "");
        String key = newLogbookName + "~" + dateString;
        if (key.equals(Daten.project.getLastLogbookSwitch())) {
            // it seems the admin has explicitly opened another (maybe the previous) logbook
            // again, but we have already completed the switch into the configured logbook
            // in this efa instance.
            return;
        }
        
        // Why back off: a failing switch would otherwise be retried every minute and flood
        // the admin with messages. The same failed switch is retried at most every 10 minutes.
        if (key.equals(failedLogbookSwitchKey)
                && System.currentTimeMillis() - lastLogbookSwitchFailure < LOGBOOK_SWITCH_RETRY_INTERVAL) {
            return;
        }

        Logger.log(Logger.INFO, Logger.MSG_EVT_AUTOSTARTNEWLOGBOOK,
                International.getString("Fahrtenbuchwechsel wird begonnen ..."));

        BoatStatus boatStatus = Daten.project.getBoatStatus(false);
        long lockLogbook = -1;
        long lockStatus = -1;
        try {
            Logbook newLogbook = Daten.project.getLogbook(newLogbookName, false);

            // Step 1: Try to find and open new Logbook
            if (newLogbook == null) {
                Logger.log(Logger.ERROR, Logger.MSG_ERR_AUTOSTARTNEWLOGBOOK,
                        LogString.fileNotFound(newLogbookName, International.getString("Fahrtenbuch")));
                throw new Exception("New Logbook not found");
            }

            // Step 2: Abort open Sessions (only for local project)
            boolean sessionsAborted = false;
            // Why declared here: the admin message is built after the abort block,
            // so the failure list must be visible in both places.
            List<String> abortFailures = new ArrayList<String>();
            if (Daten.project.getProjectStorageType() != IDataAccess.TYPE_EFA_REMOTE) {
            	if (boatStatus!=null) {
            		//only proceed if boatstatus !=null as it actually CAN be null if project is in deletion...
	                Vector<BoatStatusRecord> boatsOnTheWater = boatStatus.getBoats(BoatStatusRecord.STATUS_ONTHEWATER);
	                if (boatsOnTheWater.size() > 0) {
	                    lockStatus = boatStatus.data().acquireGlobalLock();
	                    lockLogbook = currentLogbook.data().acquireGlobalLock();
	                    sessionsAborted = true;
	                    Logger.log(Logger.INFO, Logger.MSG_EVT_AUTOSTARTNEWLBSTEP,
	                            International.getString("Offene Fahrten werden abgebrochen ..."));
	                    try {
		                    for (int i = 0; i < boatsOnTheWater.size(); i++) {
		                        BoatStatusRecord sr = boatsOnTheWater.get(i);
		                        try {
		                            // try/catch: one broken record must not stop the
		                            // abort of all other boats on the water.
		                            LogbookRecord r = null;
		                            if (sr.getEntryNo() != null && sr.getEntryNo().isSet()) {
		                                r = currentLogbook.getLogbookRecord(sr.getEntryNo());
		                                if (r == null) {
		                                    throw new IllegalStateException("Logbook entry not found for boat " + sr.getBoatId());
		                                }
		                                r.setSessionIsOpen(false);
		                                currentLogbook.data().update(r, lockLogbook);
		                            }
		                            sr.setEntryNo(null);
		                            sr.setCurrentStatus(sr.getBaseStatus());
		                            boatStatus.data().update(sr, lockStatus);
		                            EfaBaseFrame.logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_TRIPABORT,
		                                    International.getString("Fahrtabbruch"), r);
		                        } catch (Exception e) {
		                            // the admin must know WHICH trips could not be aborted, and this is added to the message to admin
		                            abortFailures.add(sr.getBoatNameAsString(System.currentTimeMillis()));
		                            Logger.log(e);
		                        }
		                    }
		                } catch (Exception e){
		                	Logger.log(e);
		                } finally {
		                    // moved out of the for loop as the lock seems to be needed for the whole loop, not for the very first item only.
		                	// also handled possible exceptions and will release lock also if exceptions occur.
		                	boatStatus.data().releaseGlobalLock(lockStatus);
		                    lockStatus = -1;
		                    currentLogbook.data().releaseGlobalLock(lockLogbook);
		                    lockLogbook = -1;
		                }
	                }
            	}
            }

            // Step 3: Activate the new Logbook
            // Well. WE SHOULD call openLogBook swing-thread-safe via invokeAndWait() here.
            // Because openLogbook does refresh the gui, and swing is not thread-safe.
            // invokeLater() as an asynchronouse call won't help for this issue, 
            // because we need to ensure that the logbook is open directly after this call.
            // But I am not sure wether an invokeAndWait() in this place may lead to a dead-lock.
            // So instead, openLogBook() handles update of the GUI itself by calling invokeLater() for the updates.
            if (efaBoathouseFrame.openLogbook(newLogbook.getName())) {
                Logger.log(Logger.INFO, Logger.MSG_EVT_AUTOSTARTNEWLBDONE,
                        LogString.operationSuccessfullyCompleted(International.getString("Fahrtenbuchwechsel")));
                Daten.project.setLastLogbookSwitch(key);
                failedLogbookSwitchKey = null; // success: reset the failure state
            } else {
                throw new Exception("Failed to open new Logbook");
            }

            // Step 4: Notify the admin. The logbook switch has already succeeded here, so a
            // failure while building this message must NOT be reported as a failed switch
            // (that is the job of the outer catch block). It is isolated in its own try/catch.
            try {
                Messages messages = Daten.project.getMessages(false);
                if (messages != null) {
                    messages.createAndSaveMessageRecord(MessageRecord.TO_ADMIN,
                            International.getString("Fahrtenbuchwechsel"),
                            International.getString("efa hat soeben wie konfiguriert ein neues Fahrtenbuch geöffnet.") + "\n"
                            + International.getMessage("Das neue Fahrtenbuch heißt {name} und ist gültig vom {fromdate} bis {todate}.",
                            newLogbook.getName(), newLogbook.getStartDate().toString(), newLogbook.getEndDate().toString()) + "\n"
                            + LogString.operationSuccessfullyCompleted(International.getString("Fahrtenbuchwechsel")) + "\n\n"
                            + (sessionsAborted ? International.getString("Zum Zeitpunkt des Fahrtenbuchwechsels befanden sich noch einige Boote "
                            + "auf dem Wasser. Diese Fahrten wurden ABGEBROCHEN. Die abgebrochenen "
                            + "Fahrten sind in der Logdatei verzeichnet.") : "")
                            // add abort failures to the message, if any
                            + (abortFailures.isEmpty() ? "" : "\n\n"
                            + International.getString("Bei folgenden Booten konnte die Fahrt NICHT abgebrochen werden:") + "\n"
                            + String.join(", ", abortFailures)));
                }
            } catch (Exception e) {
                // The logbook switch itself succeeded; only the admin notification failed.
                Logger.log(e);
            }
            EfaUtil.sleep(500);
        	SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
        			if (efaBoathouseFrame!=null) {
        				efaBoathouseFrame.updateBoatLists(true,false);
        			}
        		}
        	});
            EfaUtil.sleep(500);
            interrupt();
            
        } catch (Exception e) {
        	
            Logger.logdebug(e);
            lastLogbookSwitchFailure = System.currentTimeMillis();
            boolean firstFailure = !key.equals(failedLogbookSwitchKey);
            failedLogbookSwitchKey = key;
            
            //log the failure to the log file, so the admin can see it in the log file (and not only in the message)
            Logger.log(Logger.ERROR, Logger.MSG_ERR_AUTOSTARTNEWLOGBOOK,
                    LogString.operationAborted(International.getString("Fahrtenbuchwechsel")));
            
            // only send the mail if this is the first failure for this logbook switch attempt, 
            // otherwise we would flood the admin with mails
            if (firstFailure) {
                // (the failure is already logged above, so it is not logged a second time here)
                Messages messages = (Daten.project != null ? Daten.project.getMessages(false) : null);
                if (messages != null) {
                    messages.createAndSaveMessageRecord(MessageRecord.TO_ADMIN,
                        International.getString("Fahrtenbuchwechsel"),
                        International.getString("efa hat soeben versucht, wie konfiguriert ein neues Fahrtenbuch anzulegen.") + "\n"
                        + International.getString("Bei diesem Vorgang traten jedoch FEHLER auf.") + "\n\n"
                        + International.getString("Ein Protokoll ist in der Logdatei (Admin-Modus: Logdatei anzeigen) zu finden."));
                }
            }
        } finally {
            if (boatStatus != null && lockStatus >= 0) {
                boatStatus.data().releaseGlobalLock(lockStatus);
            }
            if (currentLogbook != null && lockLogbook >= 0) {
                currentLogbook.data().releaseGlobalLock(lockLogbook);
            }
        }

    }
    
    /**
	 * Checks if the application is in admin mode or if a shutdown has been requested.
	 * If either condition is true, it logs the reason and returns true.
	 * 
	 * @param context A string indicating the context in which this check is being performed, for logging purposes.
	 * @return true if admin mode is active or shutdown is requested; false otherwise.
	 */
    private boolean isAdminModeOrShutdownRequested(String context) {
    	boolean admin = Daten.isAdminMode();
    	boolean shutdownRequested = Daten.isShutdownRequested();
    	boolean adminOrShutdown = admin || shutdownRequested;
    	
    	if (admin) {
		    if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
		        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK, "EfaBoathouseBackgroundTask: stopping '"+context+"' as admin mode is active.");
	        }
    	} else if (shutdownRequested) {
	       if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
	           Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFABACKGROUNDTASK, "EfaBoathouseBackgroundTask: stopping  '"+context+"' as shutdown is requested.");
	       }
    	}
    	return adminOrShutdown;	
    }
    
    /**
	 * Runnable class to update the boat lists in the EfaBoathouseFrame.
	 * This is used to update the GUI from a background thread safely.
	 */
    private class BthsUpdateBoatLists implements Runnable {
        
    	private boolean bListChanged=false;
    	private boolean bOnlyAvailablePersons=false;
    	
    	public BthsUpdateBoatLists(boolean listChanged, boolean onlyAvailablePersonsOrBoats) {
    		bListChanged=listChanged;
    		bOnlyAvailablePersons=onlyAvailablePersonsOrBoats;
    	}
    	
    	public void run() {
    		if (efaBoathouseFrame!=null) {
    			efaBoathouseFrame.updateBoatLists(bListChanged, bOnlyAvailablePersons);
    		}
	    }
	}
	
    /**
	 * Runnable class to set the unread messages in the EfaBoathouseFrame.
	 * This is used to update the GUI from a background thread safely.
	 */
	private class BthsSetUnreadMessages implements Runnable {
		
		private boolean bAdmin=false;
		private boolean bBoatMaintenance=false;
		
    	public BthsSetUnreadMessages(boolean admin, boolean boatMaintenance) {
    		bAdmin=admin;
    		bBoatMaintenance=boatMaintenance;
    	}
		
		public void run() {
			if (efaBoathouseFrame!=null) {
				efaBoathouseFrame.setUnreadMessages(bAdmin, bBoatMaintenance);
			}
		}
		
	}


}
