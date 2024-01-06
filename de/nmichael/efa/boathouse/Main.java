/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.boathouse;

import javax.swing.SwingUtilities;

import de.nmichael.efa.Daten;
import de.nmichael.efa.Program;
import de.nmichael.efa.gui.EfaBoathouseFrame;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class Main extends Program {

    public static String STARTARGS = "";

    //Construct the application
    public Main(String[] args) {
    	
    	// An unprepossessing call, but most of the initializations, including creation of a super admin
    	// during first run is done here. 
        super(Daten.APPL_EFABH, args);

        EfaBoathouseFrame frame = new EfaBoathouseFrame();
        
        //Screen size settings are calculated already in Program initialisation  
        //somewhere in the super() call above. But at that time, no Swing window has been initialized.
        //We cannot determine the actual available screen area which are not covered by task bars and other stuff,
        //as lang there is no java GUI present. So now, we can determine the screen size and provide it 
        //with a window handle.
        Dialog.initializeScreenSize(frame); // we do it again, here

        /*
         * During "showing" the efaBths frame, a lot of GUI and background tasks are initialized.
         * Unfortunately, some of the background tasks try to update the GUI.
         * So there are at least three threads which are running in parallel:
         * - this thread of main program, where most of the initialization takes place
         * - AWT Thread for swing (not thread safe)
         * - background task(s) which tries to update the GUI.
         * 
         * This leads to problems during startup. 
         * - MetalLookAndFeel seems mostly stable, as it is very lightweight. 
         * - NimbusLookAndFeel as a Synth look&feel is a bit heavier, meaning it takes longer  
         *   for the controls to repaint. Every 30th restart of efa on a raspberry pi 3b leads to an exception in Nimbus LAF.
         * - efaFlatLaf is very prone to problems during startup, as it does a lot of threading itself.
         *   Every fifth startup of efa on a rasperry pi 3b makes efaBths hang during the initialisation of efaBaseFrame.
         *   It simply does not show up.
         *   
         * Other threads have been updated in the past to do thread-safe calls to update swing components
         * (via SwingUtilities.invokelater).
         * 
         * So this main thread as well needs to update swing components in a thread-safe way.
         * This is done by SwingUtilities.invokeAndWait, so that the splash screen gets invisible _after_
         * showing the efaBths main window. InvokeAndWait is okay, as any other functions which try to update
         * GUI use invokelater() which avoids deadlocks between different tasks.
         * 
         */
        try {
        	SwingUtilities.invokeAndWait(new Runnable() {
      	      public void run() {
      	        frame.showFrame();
      	      }
        	});            
        } catch (Exception e) {
        	Logger.logdebug(e);
        }    
        Logger.log(Logger.INFO, Logger.MSG_EVT_EFAREADY, International.getString("BEREIT"));         
        Daten.iniSplashScreen(false);
    }

    public void printUsage(String wrongArgument) {
        super.printUsage(wrongArgument);
        System.exit(0);
    }

    public void checkArgs(String[] args) {
        super.checkArgs(args);
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue; // argument already handled by super class
            }
        }
        checkRemainingArgs(args);
    }

    //Main method
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            STARTARGS += " " + args[i];
        }
        new Main(args);
    }
}
