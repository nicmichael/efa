/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.emil;

import de.nmichael.efa.*;
import de.nmichael.efa.util.Dialog;
import java.awt.Dimension;


// @i18n complete (needs no internationalization -- only relevant for Germany)

public class Main extends Program {

    public final static String EMIL_VERSION = Daten.VERSION; // Version
    public final static String EMIL_KENNUNG = "EMIL.198";

    public Main(String[] args) {
        super(Daten.APPL_EMIL, args);

        EmilFrame frame = new EmilFrame();
        frame.validate();
        //Center the window
        Dimension frameSize = frame.getSize();
        if (frameSize.height > Dialog.screenSize.height) {
            frameSize.height = Dialog.screenSize.height;
        }
        if (frameSize.width > Dialog.screenSize.width) {
            frameSize.width = Dialog.screenSize.width;
        }
        Dialog.setDlgLocation(frame);
        frame.setVisible(true);
        Daten.iniSplashScreen(false);
    }

    public void printUsage(String wrongArgument) {
        super.printUsage(wrongArgument);
        System.exit(0);
    }

    public void checkArgs(String[] args) {
        super.checkArgs(args);
        checkRemainingArgs(args);
    }

    public static void main(String[] args) {
        new Main(args);
    }

}

