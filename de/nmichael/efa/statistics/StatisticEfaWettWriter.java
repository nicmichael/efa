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
import de.nmichael.efa.gui.statistics.EfaWettDoneDialog;
import de.nmichael.efa.gui.statistics.EfaWettSelectAndCompleteDialog;
import de.nmichael.efa.data.efawett.EfaWett;
import java.io.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.dataedit.ProjectEditDialog;
import de.nmichael.efa.util.*;
import java.util.Vector;

public class StatisticEfaWettWriter extends StatisticWriter {

    private EfaWett efaWett;
    private String meldegeld;
    private Vector papierFahrtenhefte;

    public StatisticEfaWettWriter(StatisticsRecord sr, StatisticsData[] sd) {
        super(sr, sd);
        this.efaWett = sr.cCompetition.getEfaWett();
    }

    public boolean runEfaWettCompletion() {
        // ================================ STEP 1 ================================
        if (Logger.isTraceOn(Logger.TT_STATISTICS, 1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS, 
                    "StatisticEfaWettWriter.runEfaWettCompletion(...) - START Step 1");
        }
        if (efaWett.meldung == null) {
            Dialog.infoDialog("Keine Meldungen", "Im gewählten Zeitraum haben keine Teilnehmer die Bedingungen erfüllt!");
            return false;
        }
        efaWett.datei =
                Daten.efaDataDirectory + 
                sr.getPersistence().getProject().getProjectName() + "_" +
                EfaUtil.replace(efaWett.allg_wett, ".", "_", true) + "_" +
                EfaUtil.replace(efaWett.allg_wettjahr, "/", "-", true) + ".EFW";

        ProjectEditDialog dlg1 = new ProjectEditDialog((BaseDialog)Dialog.frameCurrent(),
                sr.getPersistence().getProject(), null, ProjectRecord.GUIITEMS_SUBTYPE_EFAWETT,
                sr.getStatisticType(), null);
        dlg1.showDialog();
        if (Logger.isTraceOn(Logger.TT_STATISTICS, 1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS,
                    "StatisticEfaWettWriter.runEfaWettCompletion(...) - END Step 1");
        }
        if (!dlg1.getDialogResult()) {
            return false;
        }
        efaWett.setProjectSettings(sr.getPersistence().getProject());



        // ================================ STEP 2 ================================
        if (Logger.isTraceOn(Logger.TT_STATISTICS, 1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS,
                    "StatisticEfaWettWriter.runEfaWettCompletion(...) - START Step 2");
        }
        EfaWettSelectAndCompleteDialog dlg2 = new EfaWettSelectAndCompleteDialog((BaseDialog)Dialog.frameCurrent(), efaWett, sr.sAdmin, sr);
        dlg2.showDialog();
        if (!dlg2.getDialogResult()) {
            return false;
        }
        meldegeld = dlg2.getResultMeldegeld();
        papierFahrtenhefte = dlg2.getResultPapierFahrtenhefte();
        if (Logger.isTraceOn(Logger.TT_STATISTICS, 1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS,
                    "StatisticEfaWettWriter.runEfaWettCompletion(...) - END Step 1");
        }

        if (Logger.isTraceOn(Logger.TT_STATISTICS, 1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS,
                    "StatisticEfaWettWriter.efaWettCompleteStep3(...) - START");
        }
        return true;
    }


    public boolean write() {
        if (!runEfaWettCompletion()) {
            return false;
        }
        boolean success = false;
        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS, "StatisticEfaWettWriter.write(...) - START");
        }
        try {
            efaWett.writeFile();
            success = true;
            if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS, "StatisticEfaWettWriter.write(...): Done with writing without Exception");
            }
        } catch (IOException e) {
            if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS, "StatisticEfaWettWriter.write(...): Exception Handler: " + e.toString());
            }
            Dialog.error("Fehler beim Schreiben der Datei: " + efaWett.datei);
        }
        if (success) {
            EfaWettDoneDialog dlg3 = new EfaWettDoneDialog((BaseDialog)Dialog.frameCurrent(), efaWett, meldegeld, papierFahrtenhefte);
            dlg3.showDialog();
        }
        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS, "StatisticEfaWettWriter.write(...) - END");
        }
        return success;
    }
}
