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

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.gui.*;
import javax.swing.UIManager;
import java.awt.*;
import de.nmichael.efa.*;
import java.io.*;
import java.util.*;

// @i18n complete
public class Main extends Program {

    public static String STARTARGS = "";

    //Construct the application
    public Main(String[] args) {
        super(Daten.APPL_EFABH, args);

        EfaBoathouseFrame frame = new EfaBoathouseFrame();
        frame.showFrame();
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
