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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;

import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

public class StatisticHTMLWriter extends StatisticWriter {

    private static final String HTML_FILE_EFA_END_TAG = "<!--EFA-ENDE-->";
	private static final String HTML_FILE_EFA_START_TAG = "<!--EFA-START-->";
	private static String EFA_STYLES = "efa-html-styles.css";

    public StatisticHTMLWriter(StatisticsRecord sr, StatisticsData[] sd) {
        super(sr, sd);
    }

    public boolean write() {
        BufferedWriter f = null;
        BufferedReader fo = null;
        String tmpFile = sr.sOutputFile + ".efatmp";

        if (sr.sFileExecBefore != null && sr.sFileExecBefore.length() > 0) {
            EfaUtil.execCmd(sr.sFileExecBefore);
        }
        try {

            // Nur Tabelle ersetzen?
            if (sr.sOutputHtmlUpdateTable && !new File(sr.sOutputFile).isFile()) {
                Logger.log(Logger.WARNING, Logger.MSG_STAT_OUTPUTREPLHTMLNOTFOUND, 
                        "Cannot replace table only in statistics file: File '" + sr.sOutputFile + "' not found.");
                sr.sOutputHtmlUpdateTable = false;
            }
            if (sr.sOutputHtmlUpdateTable) {
                File bak = new File(sr.sOutputFile);
                bak.renameTo(new File(tmpFile));
                fo = new BufferedReader(new InputStreamReader(new FileInputStream(tmpFile), sr.getOutputEncoding()));
            }

            // Datei erstellen und Kopf schreiben
            f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sr.sOutputFile), sr.getOutputEncoding()));
            if (sr.sOutputHtmlUpdateTable) {
                String zz;
                while ((zz = fo.readLine()) != null && !zz.trim().equals(HTML_FILE_EFA_START_TAG)) {
                    f.write(zz + "\n");
                }
            } else {
                f.write("<html>\n");
                f.write("<head>\n");
                f.write("<meta http-equiv=\"content-type\" content=\"text/html; charset=" + sr.getOutputEncoding() + "\" />\n");
                f.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + 
                        EfaUtil.saveFile(EFA_STYLES, sr.sOutputDir, true, false, false) +
                        "\" media=\"all\" />\n");
                f.write("<title>" + sr.pStatTitle + "</title>\n");

                f.write("</head>\n");
                f.write("<body>\n");
                f.write("<h1>" + sr.pStatTitle + "</h1>\n");
            }

            // Start des eigentlichen Bereichs
            f.write(HTML_FILE_EFA_START_TAG+"\n");

            f.write("<table class=\"header\">\n");
            int rowspan = 5;

            f.write("<tr><td class=\"header_name\">"
                    + EfaUtil.replace(International.getString("Auswertung erstellt am"), " ", "&nbsp;", true)
                    + ":</td><td class=\"header_value\">" + sr.pStatCreationDate + "</td></tr>\n");
            f.write("<tr><td class=\"header_name\">"
                    + EfaUtil.replace(International.getString("Auswertung erstellt von"), " ", "&nbsp;", true)
                    + ":</td><td class=\"header_value\"><a href=\"" + sr.pStatCreatedByUrl + "\">" + sr.pStatCreatedByName + "</a></td></tr>\n");
            f.write("<tr><td class=\"header_name\">"
                    + EfaUtil.replace(International.getString("Art der Auswertung"), " ", "&nbsp;", true)
                    + ":</td><td class=\"header_value\">" + sr.pStatDescription + "</td></tr>\n");
            f.write("<tr><td class=\"header_name\">"
                    + EfaUtil.replace(International.getString("Zeitraum für Auswertung"), " ", "&nbsp;", true)
                    + ":</td><td class=\"header_value\">" + sr.pStatDateRange + "</td></tr>\n");
            f.write("<tr><td class=\"header_name\">"
                    + EfaUtil.replace(International.getString("Ausgewertete Einträge"), " ", "&nbsp;", true)
                    + ":</td><td class=\"header_value\">" + sr.pStatConsideredEntries + "</td></tr>\n");
            if (sr.pStatFilter != null) {
                f.write("<tr><td class=\"header_name\">"
                        + EfaUtil.replace(International.getString("Filter"), " ", "&nbsp;", true)
                        + ":</td><td class=\"header_value\">" + EfaUtil.replace(sr.pStatFilter,"\n","<br>",true) + "</td></tr>\n");
            }
            if (sr.pStatIgnored != null && sr.pStatIgnored.size() > 0) {
                f.write("<tr><td class=\"header_name\" colspan=\"2\">"
                        + International.getMessage("{count} Personen oder Boote wurden von der Auswertung explizit ausgenommen.",
                        sr.pStatIgnored.size())
                        + "</td></tr>\n");
            }
            f.write("</table>\n<br><br>\n");
            
            // Warnings
            if (sr.cWarnings != null && sr.cWarnings.size() > 0) {
                String[] keys = sr.cWarnings.keySet().toArray(new String[0]);
                Arrays.sort(keys);
                StringBuilder warning = new StringBuilder();
                for (String s : keys) {
                    warning.append( (warning.length() > 0 ? "<br>\n" : "") + s);
                }
                f.write("<p class=\"warning\">" + warning.toString() + "</p><br clear=\"all\">");
            }

            // Auswertung von Wettbewerbseinträgen
            // Wettbewerbsbedingungen

            if (sr.pCompRules != null && sr.sStatisticCategory == StatisticsRecord.StatisticCategory.competition) {
                f.write("<table class=\"comp_rules\"><tr><td>\n");
                for (int i = 0; i < sr.pCompRules.length; i++) {
                    if (sr.pCompRulesBold.get(new Integer(i)) != null) {
                        f.write("<b>");
                    }
                    if (sr.pCompRulesItalics.get(new Integer(i)) != null) {
                        f.write("<i>");
                    }
                    f.write(sr.pCompRules[i] + "<br>");
                    if (sr.pCompRulesItalics.get(new Integer(i)) != null) {
                        f.write("</i>");
                    }
                    if (sr.pCompRulesBold.get(new Integer(i)) != null) {
                        f.write("</b>");
                    }
                }
                f.write("</table>\n<br><br>\n");
            }

            if (sr.sIsOutputCompWithoutDetails && 
                    sr.sStatisticCategory == StatisticsRecord.StatisticCategory.competition &&
                    sr.pCompParticipants != null && sr.pCompParticipants.length > 0) {
                f.write("<table class=\"comp_legend\">\n");
                f.write("<tr><th class=\"comp_legend\" colspan=\"2\">Legende</th></tr>\n");
                f.write("<tr><td class=\"comp_legend_ok\">"
                        + International.getString("Bedingungen erfüllt") + "</td>");
                f.write("<td class=\"comp_legend_nok\">"
                        + International.getString("Bedingungen noch nicht erfüllt") + "</td></tr>\n");
                f.write("</table>\n<br><br>\n");
            }

            if (sr.pCompGroupNames != null && sr.pCompParticipants != null
                    && sr.sStatisticCategory == StatisticsRecord.StatisticCategory.competition) {
                if (sr.pCompWarning != null) {
                    f.write("<p class=\"warning\">" + sr.pCompWarning + "</p>\n");
                }

                f.write("<table class=\"comp_data\">\n");
                for (int i = 0; i < sr.pCompGroupNames.length; i++) {
                    f.write("<tr><th class=\"comp_group\" colspan=\"3\">"
                            + sr.pCompGroupNames[i][0] + " " + sr.pCompGroupNames[i][1]
                            + " (<i>gefordert: " + sr.pCompGroupNames[i][2] + "</i>)</th></tr>\n");
                    if (sr.pCompParticipants != null && sr.pCompParticipants.length > i) {
                        for (StatisticsData participant = sr.pCompParticipants[i]; participant != null; participant = participant.next) {
                            f.write("<tr><td class=\"comp_space\">&nbsp;</td>\n");
                            if (participant.sDetailsArray == null || sr.sIsOutputCompWithoutDetails) {
                                // kurze Ausgabe
                                if (sr.sIsOutputCompWithoutDetails) {
                                    f.write("<td " + (participant.compFulfilled ? "class=\"comp_entry_ok_name\"" : "class=\"comp_entry_nok_name\"") + "><b>" + participant.sName + "</b></td>"
                                          + "<td " + (participant.compFulfilled ? "class=\"comp_entry_ok_details\"" : "class=\"comp_entry_nok_details\"") + ">" + participant.sDistance + " Km"
                                          + (participant.sAdditional == null || participant.sAdditional.equals("") ? "" : "; " + participant.sAdditional) + "</td>\n");
                                } else {
                                    String additional = (participant.sAdditional == null || participant.sAdditional.equals("") ? "" : participant.sAdditional)
                                            + (participant.sCompWarning == null ? "" : "; <font color=\"red\">" + participant.sCompWarning + "</font>");
                                    f.write("<td class=\"comp_entry_all\" colspan=\"2\">" + (participant.compFulfilled
                                            ? International.getString("erfüllt") + ": "
                                            : International.getString("noch nicht erfüllt") + ": ") + "<b>" + participant.sName + "</b>"
                                            + (participant.sYearOfBirth != null ? " (" + participant.sYearOfBirth + ")" : "")
                                            + ": " + participant.sDistance + " Km"
                                            + (additional.length() > 0 ? " (" + additional + ")" : "")
                                            + "</td>\n");
                                }
                            } else {
                                // ausführliche Ausgabe
                                f.write("<td class=\"comp_entry_all\" colspan=\"2\">\n");
                                int colspan = 1;
                                if (participant.sDetailsArray.length > 0) {
                                    colspan = participant.sDetailsArray[0].length;
                                }
                                f.write("<table class=\"comp_entry_details\">\n<tr><td class=\"comp_entry_details\" colspan=\"" + colspan + "\"><b>" + participant.sName + " (" + participant.sYearOfBirth + "): "
                                        + participant.sDistance + " Km" + (participant.sAdditional != null ? "; " + participant.sAdditional : "") + "</b></td></tr>\n");
                                if (participant.sDetailsArray.length > 0) {
                                    for (int j = 0; j < participant.sDetailsArray.length; j++) {
                                        f.write("<tr>");
                                        if (participant.sDetailsArray[j] != null && participant.sDetailsArray[j][0] != null) {
                                            for (int k = 0; k < participant.sDetailsArray[j].length; k++) {
                                                f.write("<td class=\"comp_entry_details\">" + participant.sDetailsArray[j][k] + "</td>");
                                            }
                                        } else {
                                            f.write("<td class=\"comp_entry_details\" colspan=\"" + colspan + "\">und weitere Fahrten</td>");
                                        }
                                        f.write("</tr>\n");
                                    }
                                }
                                f.write("</table>\n</td>\n");
                            }
                            f.write("</tr>\n");
                        }
                    }
                    f.write("<tr colspan=\"3\"><td>&nbsp;</td></tr>\n");
                }
                f.write("</table>\n");
            }
            

            // Auswertung normaler Einträge
            if (sr.pTableColumns != null && sr.pTableColumns.size() > 0) {
                StatisticsData sdMaximum = sd[sd.length-1]; // Maximum is always last
                if (!sdMaximum.isMaximum) {
                    sdMaximum = null;
                }
                f.write("<table class=\"data\">\n<tr>\n");
                for (int i=0; i<sr.pTableColumns.size(); i++) {
                    f.write("<th class=\"data\">" + sr.pTableColumns.get(i) + "</th>");
                }
                f.write("</tr>\n");

                // Einträge auswerten
                for (int i=0; i<sd.length; i++) {
                    if (sd[i].isMaximum) {
                        continue;
                    }
                    if (!sd[i].isSummary) {
                        f.write("<tr class=\"" + (sd[i].absPosition % 2 == 0 ? "line1" : "line2") + "\">\n");
                    } else {
                        f.write("<tr>\n");
                    }

                    if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.list ||
                        sr.sStatisticCategory == StatisticsRecord.StatisticCategory.matrix ||
                        sr.sStatisticCategory == StatisticsRecord.StatisticCategory.other) {
                        outHTML(f, sd[i].sPosition, "entry_pos");
                        outHTML(f, sd[i].sName, "entry_data");
                        outHTML(f, sd[i].sGender, "entry_data");
                        outHTML(f, sd[i].sStatus, "entry_data");
                        outHTML(f, sd[i].sClub, "entry_data");
                        outHTML(f, sd[i].sYearOfBirth, "entry_data");
                        outHTML(f, sd[i].sMemberNo, "entry_data");
                        outHTML(f, sd[i].sBoatType, "entry_data");
                        outHTML(f, sd[i].sDistance, sd[i].distance,
                                (sdMaximum != null && !sd[i].isSummary ? sdMaximum.distance : 0),
                                "entry_distance", sr.sAggrDistanceBarSize);
                        outHTML(f, sd[i].sRowDistance, sd[i].rowdistance,
                                (sdMaximum != null && !sd[i].isSummary ? sdMaximum.rowdistance : 0),
                                "entry_rowdistance", sr.sAggrRowDistanceBarSize);
                        outHTML(f, sd[i].sCoxDistance, sd[i].coxdistance,
                                (sdMaximum != null && !sd[i].isSummary ? sdMaximum.coxdistance : 0),
                                "entry_coxdistance", sr.sAggrCoxDistanceBarSize);
                        outHTML(f, sd[i].sSessions, sd[i].count,
                                (sdMaximum != null && !sd[i].isSummary ? sdMaximum.count : 0),
                                "entry_sessions", sr.sAggrSessionsBarSize);
                        outHTML(f, sd[i].sAvgDistance, sd[i].avgDistance,
                                (sdMaximum != null && !sd[i].isSummary ? sdMaximum.avgDistance : 0),
                                "entry_avgdistance", sr.sAggrAvgDistanceBarSize);
                        outHTML(f, sd[i].sDuration, sd[i].duration,
                                (sdMaximum != null && !sd[i].isSummary ? sdMaximum.duration : 0),
                                "entry_duration", sr.sAggrDurationBarSize);
                        outHTML(f, sd[i].sDays, sd[i].days,
                                (sdMaximum != null && !sd[i].isSummary ? sdMaximum.days : 0),
                                "entry_duration", 0);
                        outHTML(f, sd[i].sSpeed, sd[i].speed,
                                (sdMaximum != null && !sd[i].isSummary ? sdMaximum.speed : 0),
                                "entry_speed", sr.sAggrSpeedBarSize);
                        outHTML(f, sd[i].sDestinationAreas, "entry_data");
                        outHTML(f, sd[i].sWanderfahrten, 0, 0, null, 0);
                        outHTML(f, sd[i].sDamageCount, 0, 0, null, 0);
                        outHTML(f, sd[i].sDamageDuration, 0, 0, null, 0);
                        outHTML(f, sd[i].sDamageAvgDuration, 0, 0, null, 0);
                        outHTML(f, sd[i].sClubwork, "entry_data");
                        outHTML(f, sd[i].sClubworkTarget, "entry_data");
                        if (sd[i].sClubworkRelativeToTarget != null) {
                            outHTML(f, sd[i].sClubworkRelativeToTarget,
                                    (!sd[i].sClubworkRelativeToTarget.equals("") && !sd[i].isSummary
                                    && sd[i].clubworkRelativeToTarget < -sr.sTransferableClubworkHours
                                    ? "entry_nok" : "entry_ok"));
                        }
                        if (sd[i].sClubworkOverUnderCarryOver != null) {
                            outHTML(f, sd[i].sClubworkOverUnderCarryOver,
                                    (!sd[i].sClubworkOverUnderCarryOver.equals("") && !sd[i].isSummary
                                    && sd[i].clubworkOverUnderCarryOver < 0
                                    ? "entry_nok" : "entry_ok"));
                        }
                        outHTML(f, sd[i].sClubworkCredit, "entry_data");
                        if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.matrix) {
                            for (int j = sr.pMatrixColumnFirst; j < sr.pTableColumns.size(); j++) {
                                StatisticsData sdm = (sd[i].matrixData != null ?
                                    sd[i].matrixData.get(sr.pMatrixColumns.get(sr.pTableColumns.get(j))) : null);
                                outHTML(f, EfaUtil.escapeHtml(getMatrixString(sdm)), "entry_data");
                            }
                        }
                    }
                    if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.logbook) {
                        if (sd[i].logbookFields != null) {
                            for (int j = 0; j < sd[i].logbookFields.length; j++) {
                                outHTML(f, (sd[i].logbookFields[j] != null
                                        ? sd[i].logbookFields[j] : ""), "entry_data");
                            }
                        }
                    }
                    if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.other) {
                        if (sd[i].otherFields != null) {
                            for (int j = 0; j < sd[i].otherFields.length; j++) {
                                outHTML(f, (sd[i].otherFields[j] != null
                                        ? sd[i].otherFields[j] : ""), "entry_data");
                            }
                        }
                    }
                    f.write("</tr>\n");
                }

                f.write("</table>\n");
            }

            // Zusatzdaten
            if (sr.pAdditionalTable1 != null) {
                printTable(f, sr.pAdditionalTable1Title, sr.pAdditionalTable1, 
                        sr.pAdditionalTable1FirstRowBold, sr.pAdditionalTable1LastRowBold);
            }
            if (sr.pAdditionalTable2 != null) {
                printTable(f, sr.pAdditionalTable2Title, sr.pAdditionalTable2,
                        sr.pAdditionalTable2FirstRowBold, sr.pAdditionalTable2LastRowBold);
            }

            // Ende des eigentlichen Bereichs
            if (sr.sOutputHtmlUpdateTable) {
                String zz;
                while ((zz = fo.readLine()) != null && !zz.trim().equals(HTML_FILE_EFA_END_TAG));
                f.write("\n"+HTML_FILE_EFA_END_TAG+"\n");
                while ((zz = fo.readLine()) != null) {
                    f.write(zz + "\n");
                }
                fo.close();
                File bak = new File(tmpFile);
                bak.delete();
            } else {
                f.write("\n"+HTML_FILE_EFA_END_TAG+"\n");
                f.write("</body>\n");
                f.write("</html>\n");
            }
        } catch (IOException e) {
            Dialog.error(LogString.fileCreationFailed(sr.sOutputFile, International.getString("Ausgabedatei")));
            Logger.log(Logger.WARNING, Logger.MSG_ERR_ERRORCREATINGSTATISTIC,
                    LogString.fileCreationFailed(sr.sOutputFile, International.getString("Ausgabedatei")));
            
            resultMessage = LogString.fileCreationFailed(sr.sOutputFile, International.getString("Statistik"));
            return false;
        } finally {
            try {
                f.close();
            } catch (Exception ee) {
                f = null;
            }
        }
        if (sr.sFileExecAfter != null && sr.sFileExecAfter.length() > 0) {
            EfaUtil.execCmd(sr.sFileExecAfter);
        }
        resultMessage = LogString.fileSuccessfullyCreated(sr.sOutputFile, International.getString("Statistik"));
        return true;
    }

    void outHTML(BufferedWriter f, String s, String cssclass) throws IOException {
        if (s != null) {
            s = (s.length() > 0 ? EfaUtil.escapeHtml(s) : "&nbsp;");
            if (s.indexOf(StatisticWriter.TEXTMARK_BOLDSTART) >= 0) {
                s = EfaUtil.replace(s, StatisticWriter.TEXTMARK_BOLDSTART, "<b>", true);
                s = EfaUtil.replace(s, StatisticWriter.TEXTMARK_BOLDEND, "</b>", true);
            }
            f.write("<td"
                    + (cssclass != null ? " class=\"" + cssclass + "\"" : "")
                    + ">"
                    + s
                    + "</td>\n");
        }
    }

    void outHTML(BufferedWriter f, String s, long value, long maximum, String cssclass, long barSize) throws IOException {
        if (s != null) {
            f.write("<td" + (cssclass == null || barSize == 0 ? " class=\"entry_value\"" : " class=\"entry_bar\"") + ">");
            if (barSize != 0 && value != 0 && maximum > 0) {
                long width = value*barSize / maximum;
                if (width <= 0) {
                    width = 1;
                }
                if (width > barSize) {
                    width = barSize;
                }
                String image = "bar.gif";
                if (sr.sOutputType == StatisticsRecord.OutputTypes.internal) {
                    image = "color_blue.gif";
                    if ("entry_distance".equals(cssclass)) {
                        image = "color_blue.gif";
                    } else if ("entry_rowdistance".equals(cssclass)) {
                        image = "color_darkblue.gif";
                    } else if ("entry_coxdistance".equals(cssclass)) {
                        image = "color_lightblue.gif";
                    } else if ("entry_sessions".equals(cssclass)) {
                        image = "color_red.gif";
                    } else if ("entry_avgdistance".equals(cssclass)) {
                        image = "color_green.gif";
                    } else if ("entry_duration".equals(cssclass)) {
                        image = "color_cyan.gif";
                    } else if ("entry_speed".equals(cssclass)) {
                        image = "color_magenta.gif";
                    }
                }
                f.write("<img" + (cssclass != null ? " class=\"" + cssclass + "\"" : "")
                        + " src=\"" +
                        EfaUtil.saveImage(image, "gif", sr.sOutputDir, true, false, false) +
                        "\" width=\"" + width + "\" height=\"20\" alt=\"\">&nbsp;");
            }
            if (s.length() > 0) {
                f.write(EfaUtil.escapeHtml(s));
            }
            f.write("</td>\n");
        }
    }
/*
    void outHTML(BufferedWriter f, String s, long value, long maximum, String colorBar, long barSize) throws IOException {
        if (s != null) {
            f.write("<td" + (colorBar == null || barSize == 0 ? " align=\"right\"" : "") + ">");
            if (colorBar != null && barSize != 0 && value != 0 && maximum > 0) {
                long width = value*barSize / maximum;
                if (width <= 0) {
                    width = 1;
                }
                f.write("<img src=\"" + 
                        EfaUtil.saveImage("color_" + colorBar + ".gif", "gif", sr.sOutputDir, true, false, false) +
                        "\" width=\"" + width + "\" height=\"20\" alt=\"\">&nbsp;");
            }
            if (s.length() > 0) {
                f.write(EfaUtil.escapeHtml(s));
            }
            f.write("</td>\n");
        }
    }
*/
    private void printTable(BufferedWriter f, String[] header, String[][] data,
            boolean firstRowBold, boolean lastRowBold) throws IOException {
        f.write("<br><table class=\"additional\">\n");
        if (header != null) {
            f.write("<tr>");
            for (int i = 0; header != null && i < header.length; i++) {
                f.write("<th class=\"additional\">" + header[i] + "</th>");
            }
            f.write("</tr>\n");
        }
        for (int i = 0; i < data.length; i++) {
            if (data[i] == null) {
                continue;
            }
            f.write("<tr>");
            for (int j = 0; j < data[i].length; j++) {
                boolean isBold = (i == 0 && firstRowBold) || (i == data.length - 1 && lastRowBold);
                f.write("<td class=\"additional\" " + (isBold ? " align=\"center\"" : "") + ">"
                        + (isBold ? "<b>" : "")
                        + data[i][j]
                        + (isBold ? "</b>" : "")
                        + "</td>");
            }
            f.write("</tr>\n");
        }
        f.write("</table><br>\n");
    }
}
