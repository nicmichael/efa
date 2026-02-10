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

import de.nmichael.efa.data.*;

public abstract class StatisticWriter {

    public static final String TEXTMARK_BOLDSTART = "%%BOLD-START%%";
    public static final String TEXTMARK_BOLDEND   = "%%BOLD-END%%";

    protected StatisticTask statisticTask;
    protected StatisticsRecord sr;
    protected StatisticsData[] sd;
    protected String resultMessage;

    public StatisticWriter(StatisticsRecord sr, StatisticsData[] sd) {
        this.sd = sd;
        this.sr = sr;
    }

    public static StatisticWriter getWriter(StatisticTask statisticTask, StatisticsRecord sr, StatisticsData[] sd) {
        StatisticWriter sw = null;
        switch(sr.sOutputType) {
            case internal:
                sw = new StatisticInternalWriter(sr, sd);
                break;
            case internaltxt:
                sw = new StatisticInternalTxtWriter(sr, sd);
                break;
            case html:
                sw = new StatisticHTMLWriter(sr, sd);
                break;
            case pdf:
                sw = new StatisticPDFWriter(sr, sd);
                break;
            case csv:
                sw = new StatisticCSVWriter(sr, sd);
                break;
            case xml:
                sw = new StatisticXMLWriter(sr, sd);
                break;
            case efawett:
                if (sr.cCompetition != null) {
                    sw = new StatisticEfaWettWriter(sr, sd);
                }
                break;
        }
        if (sw == null) {
            sw = new StatisticInternalWriter(sr, sd);
        }
        sw.statisticTask = statisticTask;
        return sw;
    }

    protected String getMatrixString(StatisticsData sdm) {
        StringBuilder s = new StringBuilder();
        if (sdm != null) {
            if (sdm.sDistance != null) {
                s.append((s.length() > 0 ? "; " : "") + sdm.sDistance);
            }
            if (sdm.sRowDistance != null) {
                s.append((s.length() > 0 ? "; " : "") + sdm.sRowDistance);
            }
            if (sdm.sCoxDistance != null) {
                s.append((s.length() > 0 ? "; " : "") + sdm.sCoxDistance);
            }
            if (sdm.sSessions != null) {
                s.append((s.length() > 0 ? "; " : "") + sdm.sSessions);
            }
            if (sdm.sAvgDistance != null) {
                s.append((s.length() > 0 ? "; " : "") + sdm.sAvgDistance);
            }
            if (sdm.sDuration != null) {
                s.append((s.length() > 0 ? "; " : "") + sdm.sDuration);
            }
            if (sdm.sSpeed != null) {
                s.append((s.length() > 0 ? "; " : "") + sdm.sSpeed);
            }
            if (sdm.sDestinationAreas != null) {
                s.append((s.length() > 0 ? "; " : "") + sdm.sDestinationAreas);
            }
            if (sdm.sWanderfahrten != null) {
                s.append((s.length() > 0 ? "; " : "") + sdm.sWanderfahrten);
            }
        }
        return s.toString();
    }

    public abstract boolean write();

    public String getResultMessage() {
        return resultMessage;
    }

}