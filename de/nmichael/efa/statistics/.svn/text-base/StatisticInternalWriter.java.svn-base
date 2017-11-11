/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.statistics;

import de.nmichael.efa.Daten;
import java.io.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.gui.BrowserDialog;
import de.nmichael.efa.util.*;

public class StatisticInternalWriter extends StatisticHTMLWriter {

    public static final int BROWSER_CLOSE_TIMEOUT = 300; // 300 seconds

    public StatisticInternalWriter(StatisticsRecord sr, StatisticsData[] sd) {
        super(sr, sd);
    }

    public boolean write() {
        boolean result = super.write();
        if (result) {
            if (!new File(sr.sOutputFile).isFile()) {
                Dialog.error(LogString.fileNotFound(sr.sOutputFile, International.getString("Ausgabedatei")));
            } else {
                int timeout = Daten.isAdminMode() ? 0 : BROWSER_CLOSE_TIMEOUT;
                BrowserDialog.openInternalBrowser(sr.pParentDialog,
                        International.getString("Statistik"),
                        "file:" + sr.sOutputFile, timeout);
            }
        }
        resultMessage = null;
        return result;
    }

}
