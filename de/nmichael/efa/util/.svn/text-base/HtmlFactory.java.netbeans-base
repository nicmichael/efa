/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.util;

import de.nmichael.efa.*;
import java.io.*;

// @i18n complete

public class HtmlFactory {

    public static BufferedWriter createFile(String filename) {
        try {
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,false), Daten.ENCODING_UTF));
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public static String getHeader(String title, boolean withH1) {
        StringBuilder s = new StringBuilder();
        s.append("<html>\n");
        s.append("<head>\n");
        s.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=" + Daten.ENCODING_UTF + "\">\n");
        s.append("<title>" + title + "</title>\n");
        s.append("</head>\n");
        s.append("<body>\n");
        if (withH1) {
            s.append("<h1 align=\"center\">" + title + "</h1>\n");
        }
        return s.toString();
    }

    public static void writeHeader(BufferedWriter f, String title, boolean withH1) throws IOException {
        f.write(getHeader(title, withH1));
    }

    public static String getFooter() {
        StringBuilder s = new StringBuilder();
        s.append("</body>\n");
        s.append("</html>\n");
        return s.toString();
    }

    public static void writeFooter(BufferedWriter f) throws IOException {
        f.write(getFooter());
    }

    public static String createMailto(String email) {
        String filename = Daten.efaTmpDirectory+"mailto.html";
        try {
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename),Daten.ENCODING_UTF));
            writeHeader(f, International.getMessage("email an {receiver}", email), true);
            f.write("<form method=\"post\" action=\"" + Daten.INTERNET_EFAMAIL + "\">\n");
            f.write("<input type=\"hidden\" name=\"reply_thanks\" value=\"" + International.getString("Danke") + "\">\n");
            f.write("<input type=\"hidden\" name=\"reply_mailsent\" value=\"" + International.getString("email erfolgreich versandt.") + "\">\n");
            f.write("<table align=\"center\">\n");
            f.write("<tr><td><b>" + International.getString("Von") +
                    " (" + International.getString("Name") + "):</b></td><td><input type=\"text\" name=\"absender\" size=\"30\"></td></tr>\n");
            f.write("<tr><td><b>" + International.getString("Von") +
                    " (" + International.getString("email") + "):</b></td><td><input type=\"text\" name=\"email\" size=\"30\"></td></tr>\n");
            f.write("<tr><td><b>" + International.getString("Von") +
                    " (" + International.getString("Verein") + "):</b></td><td><input type=\"text\" name=\"verein\" size=\"30\"></td></tr>\n");
            f.write("<tr><td><b>" + International.getString("An") +
                    ":</b></td><td><tt>" + Daten.EFAEMAILNAME + " &lt;" +email + "&gt;</tt></td></tr>\n");
            f.write("<tr><td><b>" + International.getString("Betreff") +
                    ":</b></td><td><input type=\"text\" name=\"betreff\" size=\"30\"></td></tr>\n");
            f.write("<tr><td colspan=\"2\"><textarea name=\"nachricht\" cols=\"50\" rows=\"10\" wrap=\"physical\"></textarea></td></tr>\n");
            f.write("<tr><td colspan=\"2\" align=\"center\"><input type=\"submit\" value=\"" +
                    International.getString("Abschicken") + "\"><br>\n");
            f.write("<font color=\"red\"><b>" +
                    International.getString("Bitte stelle eine Verbindung zum Internet her.") +
                    "</b></font></td></tr>\n");
            f.write("</table>\n");
            f.write("</form>\n");
            writeFooter(f);
            f.close();
        } catch(Exception e) {
            return null;
        }
        return filename;
    }

    public static String createReload() {
        String filename = Daten.efaTmpDirectory+"reload.html";
        try {
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename),Daten.ENCODING_UTF));
            writeHeader(f, "Reloading Page ...", true);
            writeFooter(f);
            f.close();
        } catch(Exception e) {
            return null;
        }
        return filename;
    }

    public static String createRegister() {
        String filename = Daten.efaTmpDirectory+"register.html";
        try {
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename),Daten.ENCODING_UTF));
            writeHeader(f, International.getString("Registrieren"), true);
            f.write("<p>" +
                    International.getString("Bitte unterstütze die Weiterentwicklung von efa, indem Du Dich kurz als Nutzer "+
                    "von efa registrierst.") +
                    "</p>\n<br><br>");
            f.write("<form method=\"post\" action=\"" + Daten.INTERNET_EFAMAIL + "\">\n");
            f.write("<input type=\"hidden\" name=\"reply_thanks\" value=\"" + International.getString("Danke") + "\">\n");
            f.write("<input type=\"hidden\" name=\"reply_clubdata\" value=\"" + International.getString("Deine Daten werden überprüft und demnächst aktualisiert.") + "\">\n");
            f.write("<input type=\"hidden\" name=\"subject\" value=\"User efa - " + Daten.VERSIONID + "\">\n");
            f.write("<input type=\"hidden\" name=\"addUserList\" value=\"yes\">\n");
            f.write("<input type=\"hidden\" name=\"efa.version\" value=\""+ Daten.VERSIONID + "\">\n");
            if (Daten.EFALIVE_VERSION != null) {
                f.write("<input type=\"hidden\" name=\"efalive.version\" value=\"" + Daten.EFALIVE_VERSION + "\">\n");
            }
            if (Daten.VERSIONID.compareTo("1.9.9") < 0) {
                f.write("<input name=\"useEfa1\" type=\"hidden\" value=\"yes\"/>\n");
            }
            if (Daten.VERSIONID.compareTo("1.9.9") >= 0) {
                f.write("<input name=\"useEfa2\" type=\"hidden\" value=\"yes\"/>\n");
            }
            if (Daten.EFALIVE_VERSION != null) {
                f.write("<input name=\"useEfaLive\" type=\"hidden\" value=\"yes\"/>\n");
            }
            f.write("<table align=\"center\">\n");

            // the following names are not determined by efa, since this check is
            // currently being called at a time where no project is open. well...
            String clubName = (Daten.project != null && Daten.project.getClubName() != null
                    ? EfaUtil.replace(Daten.project.getClubName(), "\"", "'") : "");
            String adminName = (Daten.project != null && Daten.project.getAdminName() != null
                    ? EfaUtil.replace(Daten.project.getAdminName(), "\"", "'") : "");
            String adminEmail = (Daten.project != null && Daten.project.getAdminEmail() != null
                    ? EfaUtil.replace(Daten.project.getAdminEmail(), "\"", "'") : "");
            String checkedRowing = (Daten.efaConfig.getValueUseFunctionalityRowing() ? "checked" : "");
            String checkedCanoeing = (Daten.efaConfig.getValueUseFunctionalityCanoeing() ? "checked" : "");

            f.write("<tr><td><b>" + International.getString("Vereinsname") + ":</b></td>");
            f.write("<td colspan=\"3\"><input name=\"club\" type=\"text\" value=\"" + clubName
                    + "\" size=\"40\"/></td></tr>\n");

            f.write("<tr><td><b>" + International.getString("Homepage") + ":</b></td>");
            f.write("<td colspan=\"3\"><input name=\"homepage\" type=\"text\" size=\"40\"/></td></tr>\n");

            f.write("<tr><td><b>" + International.getString("Land") + ":</b></td>");
            f.write("<td colspan=\"3\"><input name=\"country_new\" type=\"text\" size=\"40\"/></td></tr>\n");

            f.write("<tr><td><b>" + International.getString("Region") + ":</b></td>");
            f.write("<td colspan=\"3\"><input name=\"region_new\" type=\"text\" size=\"40\"/></td></tr>\n");


            f.write("<tr><td><b>" + International.getString("Benutzung seit (Jahr)") + ":</b></td>");
            f.write("<td colspan=\"3\"><input name=\"useSince\" type=\"text\" size=\"40\"/></td></tr>\n");

            f.write("<tr><td rowspan=\"2\" valign=\"top\"><b>" + International.getString("Benutzt für Sportarten") + ":</b></td>");
            f.write("<td colspan=\"3\"><input name=\"useRowing\" type=\"checkbox\" value=\"yes\" " + checkedRowing + " /> "
                    + International.getString("Rudern") + "</td></tr>\n");
            f.write("<tr><td colspan=\"3\"><input name=\"useCanoeing\" type=\"checkbox\" value=\"yes\" " + checkedCanoeing + " />"
                    + International.getString("Kanu") + "</td></tr>\n");


            f.write("<tr><td rowspan=\"3\" valign=\"top\"><b>"
                    + International.getString("Nutzungsart") + ":</b></td>");
            f.write("<td colspan=\"3\"><input name=\"useClubHome\" type=\"checkbox\" value=\"yes\"/> "
                    + International.getString("Papier-Fahrtenbuch mit Übertrag nach efa") + "</td></tr>\n");
            f.write("<tr><td colspan=\"3\"><input name=\"useClubDirect\" type=\"checkbox\" value=\"yes\"/> "
                    + International.getString("im Bootshaus") + "</td></tr>\n");
            f.write("<tr><td colspan=\"3\"><input name=\"useEvaluate\" type=\"checkbox\" value=\"yes\"/> "
                    + International.getString("Evaluierung") + "</td></tr>\n");

            f.write("<tr><td><b>" + International.getString("Name") + ":</b></td>");
            f.write("<td colspan=\"3\"><input name=\"name\" type=\"text\" value=\"" + adminName + "\" size=\"40\"/></td></tr>\n");

            f.write("<tr><td><b>" + International.getString("email-Adresse") + ":</b></td>");
            f.write("<td colspan=\"3\"><input name=\"email\" type=\"text\" value=\"" + adminEmail + "\" size=\"40\"/></td></tr>\n");

            f.write("<tr><td><b>" + International.getString("Bemerkungen") + ":</b></td>");
            f.write("<td colspan=\"3\"><textarea name=\"comments\" cols=\"40\" rows=\"3\"/></textarea></td></tr>\n");

            f.write("<tr><td><b>" + International.getString("Mailingliste") + ":</b></td>");
            f.write("<td colspan=\"3\"><input name=\"addMailingList\" type=\"checkbox\" value=\"yes\" checked /> "
                    + International.getString("Ich möchte über Neuigkeiten per email informiert werden.") + "</td></tr>\n");
            f.write("<tr><td colspan=\"4\" align=\"center\"><br><input type=\"submit\" value=\""
                    + International.getString("Abschicken") + "\"></td></tr>\n");
            f.write("<tr><td colspan=\"4\" align=\"center\"><font color=\"red\"><b>"
                    + International.getString("Bitte stelle eine Verbindung zum Internet her.")
                    + "</b></font></td></tr>\n");
            f.write("</table>\n");
            f.write("</form>\n");
            writeFooter(f);
            f.close();
        } catch(Exception e) {
            return null;
        }
        return filename;
    }

}
