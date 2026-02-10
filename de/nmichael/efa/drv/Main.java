/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.drv;

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import de.nmichael.efa.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class Main extends Program {

    public static DRVConfig drvConfig;

    public Main(String[] args) {
        super(Daten.APPL_DRV, args);

            drvConfig = new DRVConfig(Daten.efaCfgDirectory + Daten.DRVCONFIGFILE);
            if (!EfaUtil.canOpenFile(drvConfig.getFileName())) {
                if (!drvConfig.writeFile()) {
                    String msg = LogString.fileCreationFailed(drvConfig.getFileName(),
                            International.getString("Konfigurationsdatei"));
                    Logger.log(Logger.ERROR, Logger.MSG_CORE_EFACONFIGFAILEDCREATE, msg);
                    Dialog.error(msg);
                    Daten.haltProgram(Daten.HALT_EFACONFIG);
                }
                String msg = LogString.fileNewCreated(drvConfig.getFileName(),
                        International.getString("Konfigurationsdatei"));
                Logger.log(Logger.WARNING, Logger.MSG_CORE_EFACONFIGCREATEDNEW, msg);
            }
            if (!drvConfig.readFile()) {
                String msg = LogString.fileOpenFailed(drvConfig.getFileName(),
                        International.getString("Konfigurationsdatei"));
                Logger.log(Logger.ERROR, Logger.MSG_CORE_EFACONFIGFAILEDOPEN, msg);
                Dialog.error(msg);
                Daten.haltProgram(Daten.HALT_EFACONFIG);
            }

        Logger.log(Logger.INFO, Logger.MSG_INFO_CONFIGURATION,
                "efa.dir.main=" + Daten.efaMainDirectory);
        Logger.log(Logger.INFO, Logger.MSG_INFO_CONFIGURATION,
                "efa.dir.user=" + Daten.efaBaseConfig.efaUserDirectory);
        Logger.log(Logger.INFO, Logger.MSG_INFO_CONFIGURATION,
                "efa.dir.data=" + Daten.efaDataDirectory);
        Logger.log(Logger.INFO, Logger.MSG_INFO_CONFIGURATION,
                "efa.dir.cfg=" + Daten.efaCfgDirectory);
        Logger.log(Logger.INFO, Logger.MSG_INFO_CONFIGURATION,
                "user.home=" + System.getProperty("user.home"));
        Logger.log(Logger.INFO, Logger.MSG_INFO_CONFIGURATION,
                "user.dir=" + System.getProperty("user.dir"));

        EfaDRVFrame frame = new EfaDRVFrame();
        frame.pack();
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