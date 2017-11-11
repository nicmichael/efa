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

import java.io.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.gui.statistics.StatisticsTableDialog;
import de.nmichael.efa.util.*;

public class StatisticInternalTxtWriter extends StatisticCSVWriter {

    public static final int WINDOW_CLOSE_TIMEOUT = 300; // 300 seconds

    public StatisticInternalTxtWriter(StatisticsRecord sr, StatisticsData[] sd) {
        super(sr, sd);
    }

    public boolean write() {
        boolean result = super.write();
        if (result) {
            if (!new File(sr.sOutputFile).isFile()) {
                Dialog.error(LogString.fileNotFound(sr.sOutputFile, International.getString("Ausgabedatei")));
            } else {
                StatisticsTableDialog dlg = new StatisticsTableDialog(sr.pParentDialog, sr);
                dlg.showDialog();
            }
        }
        resultMessage = null;
        return result;
    }

}
