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

import de.nmichael.efa.data.efawett.DRVSignatur;
import de.nmichael.efa.data.efawett.EfaWett;
import de.nmichael.efa.data.efawett.ESigFahrtenhefte;
import de.nmichael.efa.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.io.*;
import java.util.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class PDFOutput {

    private static String getAbzeichenList(String abzeichenListe) {
        int[] a = EfaUtil.kommaList2IntArr(abzeichenListe, ',');
        int gold = EfaUtil.sumUpArray(a);
        String einzeln = "";
        int numberOfItems = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > 0) {
                numberOfItems++;
            }
        }
        String abzeichenText = (numberOfItems <= 3 ? "Abzeichen" : "Abz.");
        for (int i = 0; i < a.length; i++) {
            if (a[i] > 0) {
                einzeln += (einzeln.length() > 0 ? "; " : "") + a[i] + " x " + ((i + 1) * 5) + ". " + abzeichenText;
            }
        }
        if (a.length == 1) {
            einzeln = null; // wenn noch altes Format, d.h. keine Information über Anzahl der einzelnen Abzeichen
        }
        return gold + (einzeln != null ? " (" + einzeln + ")" : "");
    }

    private static void writeRow(BufferedWriter f, String field, String value, String fontSize) throws IOException {
        writeRow(f, field, value, fontSize, null);
    }

    private static void writeRow(BufferedWriter f, String field, String value, String fontSize, String fontFamily) throws IOException {
        f.write("                  <fo:table-row>\n");
        f.write("                    <fo:table-cell border=\"0.5pt #000000 solid\" padding-left=\"1pt\"><fo:block>" + field + "</fo:block></fo:table-cell>\n");
        f.write("                    <fo:table-cell border=\"0.5pt #000000 solid\" padding-left=\"1pt\"><fo:block font-size=\"" + fontSize + "\"" +
                (fontFamily != null ? " font-family=\"" + fontFamily + "\""  : "") + ">" + value + "</fo:block></fo:table-cell>\n");
        f.write("                  </fo:table-row>\n");
    }
    
    private static void writeTitlePageHeader(BufferedWriter f, EfaWett ew, String qnr, String title, boolean rechnung) throws IOException {
        String header = EfaUtil.saveImage("DRV_Briefkopf_header.gif", "gif", Daten.efaTmpDirectory, true, true, true);
        String footer = EfaUtil.saveImage("DRV_Briefkopf_footer.gif", "gif", Daten.efaTmpDirectory, true, true, true);
        String fontSize = "10pt";
        String fontSizeSm = "8pt";

        f.write("  <fo:page-sequence master-reference=\"titelseite\" force-page-count=\"no-force\">\n");

        f.write("    <fo:static-content flow-name=\"xsl-region-before\" >\n");
        f.write("        <fo:block>\n");
        f.write("              <fo:external-graphic src=\"url('" + header + "')\" content-width=\"210mm\" content-height=\"42mm\" />\n");
        f.write("        </fo:block>\n");
        f.write("    </fo:static-content>\n");

        f.write("    <fo:static-content flow-name=\"xsl-region-after\" >\n");
        f.write("        <fo:block>\n");
        f.write("              <fo:external-graphic src=\"url('" + footer + "')\" content-width=\"210mm\" content-height=\"50mm\" />\n");
        f.write("        </fo:block>\n");
        f.write("    </fo:static-content>\n");

        f.write("    <fo:flow font-family=\"Helvetica\" font-size=\"" + fontSize + "\" flow-name=\"xsl-region-body\">\n");

        f.write("      <fo:table>\n");
        f.write("        <fo:table-column column-width=\"115mm\"/>\n");
        f.write("        <fo:table-column column-width=\"45mm\"/>\n");
        f.write("	 <fo:table-body>\n");
        f.write("	   <fo:table-row>\n");
        f.write("            <fo:table-cell height=\"50mm\">\n");
        f.write("              <fo:block></fo:block>\n");
        f.write("              <fo:block space-before=\"5mm\" font-size=\"7.5pt\">Deutscher Ruderverband, Ferdinand-Wilhelm-Fricke-Weg 10, 30169 Hannover</fo:block>\n");
        f.write("              <fo:block space-before=\"5mm\">" + ew.versand_name + "</fo:block>\n");
        if (ew.versand_zusatz != null && ew.versand_zusatz.trim().length() > 0) {
            f.write("              <fo:block>" + ew.versand_zusatz + "</fo:block>\n");
        }
        f.write("              <fo:block>" + ew.versand_strasse + "</fo:block>\n");
        f.write("              <fo:block>" + ew.versand_ort + "</fo:block>\n");
        f.write("            </fo:table-cell>\n");
        f.write("            <fo:table-cell height=\"50mm\">\n");
        f.write("              <fo:block></fo:block>\n");
        f.write("              <fo:block space-before=\"15mm\" font-size=\"7.5pt\">Fachressort Wanderrudern &amp; Breitensport</fo:block>\n");
        f.write("              <fo:block space-before=\"5mm\" font-size=\"7.5pt\">" + EfaUtil.getCurrentTimeStampDD_MM_YYYY() + "</fo:block>\n");
        f.write("            </fo:table-cell>\n");
        f.write("	   </fo:table-row>\n");
        f.write("	 </fo:table-body>\n");
        f.write("      </fo:table>\n");

        f.write("      <fo:block font-size=\"" + fontSize + "\" font-weight=\"bold\"  space-after=\"5mm\">" + title + "</fo:block>\n");

        f.write("      <fo:table>\n");
        f.write("        <fo:table-column column-width=\"50mm\"/>\n");
        f.write("        <fo:table-column column-width=\"110mm\"/>\n");
        f.write("	 <fo:table-body>\n");
        f.write("	   <fo:table-row>\n");
        f.write("            <fo:table-cell height=\"25mm\">\n");
        f.write("               <fo:block font-size=\"" + fontSize + "\" font-weight=\"bold\">Verein:</fo:block>\n");
        f.write("               <fo:block font-size=\"" + fontSize + "\" font-weight=\"bold\">Mitgliedsnummer:</fo:block>\n");
        f.write("               <fo:block font-size=\"" + fontSize + "\" font-weight=\"bold\">" + (rechnung ? "Rechnungsnummer" : "Bestätigungsnummer") + ":</fo:block>\n");
        f.write("            </fo:table-cell>\n");
        f.write("            <fo:table-cell height=\"25mm\">\n");
        f.write("               <fo:block font-size=\"" + (ew.verein_name.length() < 55 ? fontSize : fontSizeSm) + "\" font-weight=\"bold\">" + ew.verein_name + "</fo:block>\n");
        f.write("               <fo:block font-size=\"" + fontSize + "\" font-weight=\"bold\">" + ew.verein_mitglnr + "</fo:block>\n");
        f.write("               <fo:block font-size=\"" + fontSize + "\" font-weight=\"bold\">" + qnr + "</fo:block>\n");
        f.write("            </fo:table-cell>\n");
        f.write("	   </fo:table-row>\n");
        f.write("	 </fo:table-body>\n");
        f.write("      </fo:table>\n");
    }

    private static void writeTitlePageFooter(BufferedWriter f) throws IOException {
        f.write("      <fo:block space-before=\"3mm\">Dieses Schriftstück wurde per EDV erstellt und ist daher ohne Unterschrift.</fo:block>\n");
        f.write("      <fo:block space-before=\"3mm\">Mit freundlichen Grüßen,</fo:block>\n");
        f.write("      <fo:block space-before=\"2mm\">Deutscher Ruderverband</fo:block>\n");
        f.write("    </fo:flow>\n");
        f.write("  </fo:page-sequence>\n");
    }

    public static void printPDFbestaetigung(DRVConfig drvConfig, EfaWett ew, String qnr, int meldegeld,
            int gemeldet, int gewertet, int erwachsene, int jugendliche, ESigFahrtenhefte fh,
            Vector nichtGewerteteTeilnehmer) {
        String xslfo = Daten.efaTmpDirectory + "esigfahrtenhefte.fo";

        try {
            String header = EfaUtil.saveImage("DRV_Briefkopf_header.gif", "gif", Daten.efaTmpDirectory, true, true, true);
            String footer = EfaUtil.saveImage("DRV_Briefkopf_footer.gif", "gif", Daten.efaTmpDirectory, true, true, true);
            String fontSize = "10pt";
            String smallFontSize = "9pt";

            int netto = (int) ((meldegeld * 100) / 107);
            int mwst = meldegeld - netto;
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xslfo), Daten.ENCODING_UTF));
            f.write("<?xml version=\"1.0\" encoding=\"" + Daten.ENCODING_UTF + "\"?>\n");
            f.write("<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">\n");
            f.write("  <fo:layout-master-set>\n");
            f.write("    <fo:simple-page-master page-height=\"297mm\" page-width=\"210mm\" master-name=\"titelseite\">\n");
            f.write("      <fo:region-body margin-right=\"25mm\" margin-left=\"25mm\" margin-bottom=\"30mm\" margin-top=\"45mm\"/>\n");
            f.write("      <fo:region-before extent=\"42mm\" />\n");
            f.write("      <fo:region-after extent=\"40mm\" />\n");
            f.write("    </fo:simple-page-master>\n");
            f.write("    <fo:simple-page-master page-height=\"297mm\" page-width=\"210mm\" master-name=\"fahrtenhefte\">\n");
            f.write("      <fo:region-body margin-right=\"10mm\" margin-left=\"10mm\" margin-bottom=\"10mm\" margin-top=\"10mm\"/>\n");
            f.write("    </fo:simple-page-master>\n");
            f.write("  </fo:layout-master-set>\n");

            // =============================== ANSCHREIBEN ===============================
            writeTitlePageHeader(f, ew, qnr, "Bestätigung Ihrer Meldung für das DRV-Fahrtenabzeichen " + drvConfig.aktJahr, false);

            f.write("      <fo:block>Sehr geehrte Damen und Herren,</fo:block>\n");
            f.write("      <fo:block space-before=\"3mm\">zu der erfolgreichen Teilnahme " + (gewertet == 1 ? "Ihres Mitglieds" : "Ihrer Mitglieder")
                    + " am Fahrtenwettbewerb " + drvConfig.aktJahr + " gratulieren wir Ihnen sehr herzlich.</fo:block>\n");

            //f.write("      <fo:block space-before=\"3mm\">Ihre Meldedaten:</fo:block>\n");
            f.write("      <fo:list-block space-before=\"3mm\">\n");
            f.write("          <fo:list-item>\n");
            f.write("            <fo:list-item-label><fo:block>Gemeldete Teilnehmer:</fo:block></fo:list-item-label>\n");
            f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + gemeldet + "</fo:block></fo:list-item-body>\n");
            f.write("          </fo:list-item>\n");
            f.write("          <fo:list-item>\n");
            f.write("            <fo:list-item-label><fo:block>Gewertete Teilnehmer:</fo:block></fo:list-item-label>\n");
            f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + gewertet + " (" + erwachsene + " Erwachsene, " + jugendliche + " Jugendliche)</fo:block></fo:list-item-body>\n");
            f.write("          </fo:list-item>\n");
            f.write("      </fo:list-block>\n");

            if (gewertet < gemeldet && nichtGewerteteTeilnehmer.size() > 0) {
                f.write("      <fo:block space-before=\"3mm\">Folgende Teilnehmer wurden nicht gewertet:</fo:block>\n");
                f.write("      <fo:list-block>\n");
                for (int i = 0; i < nichtGewerteteTeilnehmer.size(); i++) {
                    f.write("          <fo:list-item>\n");
                    f.write("            <fo:list-item-label><fo:block>-</fo:block></fo:list-item-label>\n");
                    f.write("            <fo:list-item-body start-indent=\"5mm\"><fo:block>" + nichtGewerteteTeilnehmer.get(i) + "</fo:block></fo:list-item-body>\n");
                    f.write("          </fo:list-item>\n");
                }
                f.write("      </fo:list-block>\n");
            }

            f.write("      <fo:block space-before=\"3mm\">Mit diesem Schreiben erhalten Sie:</fo:block>\n");
            f.write("      <fo:list-block>\n");
            if (gewertet > 0) {
                f.write("          <fo:list-item>\n");
                f.write("            <fo:list-item-label><fo:block>-</fo:block></fo:list-item-label>\n");
                f.write("            <fo:list-item-body start-indent=\"5mm\"><fo:block>Ausdrucke der elektronischen Fahrtenhefte der gewerteten Teilnehmer</fo:block></fo:list-item-body>\n");
                f.write("          </fo:list-item>\n");
            }
            if (ew.drvint_anzahlPapierFahrtenhefte > 0) {
                f.write("          <fo:list-item>\n");
                f.write("            <fo:list-item-label><fo:block>-</fo:block></fo:list-item-label>\n");
                f.write("            <fo:list-item-body start-indent=\"5mm\"><fo:block>eingesandte Papier-Fahrtenhefte (" + ew.drvint_anzahlPapierFahrtenhefte + " Hefte)</fo:block></fo:list-item-body>\n");
                f.write("          </fo:list-item>\n");
            }
            String nadelnText = "";
            int nadelnCount = EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_erw_gold, ',')) + EfaUtil.string2int(ew.drv_nadel_erw_silber, 0)
                    + EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_jug_gold, ',')) + EfaUtil.string2int(ew.drv_nadel_jug_silber, 0);
            if (nadelnCount > 0) {
                f.write("          <fo:list-item>\n");
                f.write("            <fo:list-item-label><fo:block>-</fo:block></fo:list-item-label>\n");
                f.write("            <fo:list-item-body start-indent=\"5mm\"><fo:block>" + nadelnCount + " Anstecknadeln:</fo:block></fo:list-item-body>\n");
                f.write("          </fo:list-item>\n");
                if (EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_erw_gold, ',')) > 0) {
                    String abzeichenList = getAbzeichenList(ew.drv_nadel_erw_gold);
                    f.write("          <fo:list-item>\n");
                    f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>Erwachsene (gold):</fo:block></fo:list-item-label>\n");
                    f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + abzeichenList + "</fo:block></fo:list-item-body>\n");
                    f.write("          </fo:list-item>\n");
                }
                if (EfaUtil.string2int(ew.drv_nadel_erw_silber, 0) > 0) {
                    f.write("          <fo:list-item>\n");
                    f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>Erwachsene (silber):</fo:block></fo:list-item-label>\n");
                    f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + ew.drv_nadel_erw_silber + "</fo:block></fo:list-item-body>\n");
                    f.write("          </fo:list-item>\n");
                }
                if (EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_jug_gold, ',')) > 0) {
                    String abzeichenList = getAbzeichenList(ew.drv_nadel_jug_gold);
                    f.write("          <fo:list-item>\n");
                    f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>Jugend (gold):</fo:block></fo:list-item-label>\n");
                    f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + abzeichenList + "</fo:block></fo:list-item-body>\n");
                    f.write("          </fo:list-item>\n");
                }
                if (EfaUtil.string2int(ew.drv_nadel_jug_silber, 0) > 0) {
                    f.write("          <fo:list-item>\n");
                    f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>Jugend (silber):</fo:block></fo:list-item-label>\n");
                    f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + ew.drv_nadel_jug_silber + "</fo:block></fo:list-item-body>\n");
                    f.write("          </fo:list-item>\n");
                }
                nadelnText = " und die bestellten Anstecknadeln";
            }
            f.write("      </fo:list-block>\n");

            f.write("      <fo:block space-before=\"3mm\">Im Meldesystem efaWett (http://efa.rudern.de) liegen"
                    + " die elektronischen Fahrtenhefte für Sie zum Abruf bereit. Bitte laden Sie diese in efa herunter, "
                    + " da sie für die nächste elektronische Meldung benötigt werden "
                    + "(Administration - DRV-Fahrtenabzeichen - Bestätigungsdatei abrufen).</fo:block>\n");

            writeTitlePageFooter(f);

            // =============================== RECHNUNG ===============================
            writeTitlePageHeader(f, ew, qnr, "Rechnung zu Ihrer Meldung für das DRV-Fahrtenabzeichen " + drvConfig.aktJahr, true);

            f.write("      <fo:block>Sehr geehrte Damen und Herren,</fo:block>\n");
            f.write("      <fo:block space-before=\"3mm\">für Ihre Meldung zum Fahrtenwettbewerb " + drvConfig.aktJahr + " berechnen wir Ihnen nachfolgende Positionen:</fo:block>\n");

            f.write("      <fo:list-block space-before=\"3mm\">\n");
            f.write("          <fo:list-item>\n");
            f.write("            <fo:list-item-label><fo:block>Gewertete Teilnehmer:</fo:block></fo:list-item-label>\n");
            f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + gewertet + "</fo:block></fo:list-item-body>\n");
            f.write("          </fo:list-item>\n");
            if (nadelnCount > 0) {
                f.write("          <fo:list-item>\n");
                f.write("            <fo:list-item-label><fo:block>Bestellte Anstecknadeln:</fo:block></fo:list-item-label>\n");
                f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + nadelnCount + "</fo:block></fo:list-item-body>\n");
                f.write("          </fo:list-item>\n");
                if (EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_erw_gold, ',')) > 0) {
                    String abzeichenList = getAbzeichenList(ew.drv_nadel_erw_gold);
                    f.write("          <fo:list-item>\n");
                    f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>Erwachsene (gold):</fo:block></fo:list-item-label>\n");
                    f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + abzeichenList + "</fo:block></fo:list-item-body>\n");
                    f.write("          </fo:list-item>\n");
                }
                if (EfaUtil.string2int(ew.drv_nadel_erw_silber, 0) > 0) {
                    f.write("          <fo:list-item>\n");
                    f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>Erwachsene (silber):</fo:block></fo:list-item-label>\n");
                    f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + ew.drv_nadel_erw_silber + "</fo:block></fo:list-item-body>\n");
                    f.write("          </fo:list-item>\n");
                }
                if (EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_jug_gold, ',')) > 0) {
                    String abzeichenList = getAbzeichenList(ew.drv_nadel_jug_gold);
                    f.write("          <fo:list-item>\n");
                    f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>Jugend (gold):</fo:block></fo:list-item-label>\n");
                    f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + abzeichenList + "</fo:block></fo:list-item-body>\n");
                    f.write("          </fo:list-item>\n");
                }
                if (EfaUtil.string2int(ew.drv_nadel_jug_silber, 0) > 0) {
                    f.write("          <fo:list-item>\n");
                    f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>Jugend (silber):</fo:block></fo:list-item-label>\n");
                    f.write("            <fo:list-item-body start-indent=\"50mm\"><fo:block>" + ew.drv_nadel_jug_silber + "</fo:block></fo:list-item-body>\n");
                    f.write("          </fo:list-item>\n");
                }
            }
            f.write("      </fo:list-block>\n");

            f.write("      <fo:block space-before=\"3mm\">Für das Meldegeld" + nadelnText + " ergibt sich eine Summe in Höhe von " + EfaUtil.cent2euro(meldegeld, true)
                    + " (Nettobetrag " + EfaUtil.cent2euro(netto, true) + " zzgl. " + EfaUtil.cent2euro(mwst, true) + " gesetzl. MwSt. 7% gem. § 12 Abs. 8 UStG). "
                    + "Der Betrag ist innerhalb von 14 Tagen unter Angabe der Vereins-Nr., der Rechnungs-Nr. und dem Hinweis \"Fahrtenwettbewerb\" auf das Konto</fo:block>\n");
            f.write("      <fo:list-block space-before=\"3mm\">\n");
            f.write("          <fo:list-item>\n");
            f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>IBAN:</fo:block></fo:list-item-label>\n");
            f.write("            <fo:list-item-body start-indent=\"22mm\"><fo:block>DE06 2505 0180 0000 123862</fo:block></fo:list-item-body>\n");
            f.write("          </fo:list-item>\n");            
            f.write("          <fo:list-item>\n");
            f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>BIC: </fo:block></fo:list-item-label>\n");
            f.write("            <fo:list-item-body start-indent=\"22mm\"><fo:block>SPKHDE2HXXX</fo:block></fo:list-item-body>\n");
            f.write("          </fo:list-item>\n");            
            f.write("          <fo:list-item>\n");
            f.write("            <fo:list-item-label start-indent=\"10mm\"><fo:block>Sparkasse Hannover</fo:block></fo:list-item-label>\n");
            f.write("            <fo:list-item-body start-indent=\"22mm\"><fo:block></fo:block></fo:list-item-body>\n");
            f.write("          </fo:list-item>\n");            
            f.write("      </fo:list-block>\n");
            f.write("      <fo:block space-before=\"3mm\">zu überweisen. Ist dies bereits erfolgt, betrachten Sie diese Rechnung als gegenstandslos.</fo:block>\n");

            f.write("      <fo:block space-before=\"3mm\">Bitte geben Sie diese Rechnung zwecks Bezahlung zeitnah an den Zuständigen in Ihrem Verein weiter. Danke.</fo:block>\n");

            writeTitlePageFooter(f);

            // =============================== FAHRTENHEFTE ===============================

            f.write("  <fo:page-sequence initial-page-number=\"1\" master-reference=\"fahrtenhefte\">\n");
            f.write("    <fo:flow font-family=\"Helvetica\" font-size=\"11pt\" flow-name=\"xsl-region-body\">\n");

            Vector v = fh.getFahrtenhefte();
            int c = 0;
            for (int i = 0; i < v.size(); i++) {
                DRVSignatur sig = (DRVSignatur) v.get(i);
                c++;
                if (c > 4) {
                    c = 1;
                }

                if (c == 1) {
                    f.write("      <fo:table space-after=\"20cm\">\n");
                    f.write("        <fo:table-column column-width=\"95mm\"/>\n");
                    f.write("	   <fo:table-column column-width=\"95mm\"/>\n");
                    f.write("	   <fo:table-body>\n");
                }

                if (c == 1 || c == 3) {
                    f.write("	     <fo:table-row>\n");
                }

                f.write("            <fo:table-cell border=\"1pt #000000 solid\" height=\"130mm\" padding-top=\"3mm\" padding-bottom=\"3mm\" padding-left=\"3mm\" padding-right=\"3mm\">\n");
                f.write("              <fo:block font-size=\"14pt\" font-weight=\"bold\" text-align=\"center\">elektronisches Fahrtenheft</fo:block>\n");
                f.write("              <fo:block font-size=\"14pt\" font-weight=\"bold\" text-align=\"center\">für " + sig.getVorname() + " " + sig.getNachname() + "</fo:block>\n");
                f.write("              <fo:table space-before=\"3mm\">\n");
                f.write("                <fo:table-column column-width=\"40mm\"/>\n");
                f.write("                <fo:table-column column-width=\"49mm\"/>\n");
                f.write("                <fo:table-body>\n");
                writeRow(f, "Teilnehmernummer:", sig.getTeilnNr(), fontSize);
                writeRow(f, "Vorname:", sig.getVorname(), fontSize);
                writeRow(f, "Nachname:", sig.getNachname(), fontSize);
                writeRow(f, "Jahrgang:", sig.getJahrgang(), fontSize);
                writeRow(f, "Fahrtenabzeichen:", Integer.toString(sig.getAnzAbzeichen()), fontSize);
                writeRow(f, "Kilometer:", Integer.toString(sig.getGesKm()), fontSize);
                //@AB writeRow(f, "Kilometer (ges.):", Integer.toString(sig.getGesKm()), fontSize);
                //@AB writeRow(f, "FAbzeichen (Jug A/B):", Integer.toString(sig.getAnzAbzeichenAB()), fontSize);
                //@AB writeRow(f, "Kilometer (Jug A/B):", Integer.toString(sig.getGesKmAB()), fontSize);
                writeRow(f, "Meldejahr:", Integer.toString(sig.getJahr()), fontSize);
                writeRow(f, "Kilometer " + Integer.toString(sig.getJahr()) + ":", Integer.toString(sig.getLetzteKm()), fontSize);
                writeRow(f, "Ausstellungsdatum:", sig.getSignaturDatum(true), fontSize);
                writeRow(f, "Version:", Byte.toString(sig.getVersion()), fontSize);
                writeRow(f, "Schlüssel:", sig.getKeyName(), fontSize);
                writeRow(f, "Signatur:", sig.getSignaturString(), fontSize);
                writeRow(f, "elektronisches Fahrtenheft (zur Eingabe):", 
                        EfaUtil.replace(EfaUtil.replace(sig.toString(), ";", "~~~~~", true), "~~~~~", "; ", true),
                        smallFontSize, null); // Consolas, Courier
                f.write("                </fo:table-body>\n");
                f.write("              </fo:table>\n");
                f.write("            </fo:table-cell>\n");

                if (c == 2 || c == 4 || i + 1 == v.size()) {
                    f.write("	  </fo:table-row>\n");
                }

                if (c == 4 || i + 1 == v.size()) {
                    f.write("      </fo:table-body>\n");
                    f.write("    </fo:table>\n");
                }
            }

            f.write("    </fo:flow>\n");
            f.write("  </fo:page-sequence>\n");
            f.write("</fo:root>\n");
            f.close();
        } catch (Exception e) {
            Dialog.error("Beim Erstellen des Ausdrucks trat ein Fehler auf: " + e.getMessage());
            return;
        }

        String pdffile = Daten.efaDataDirectory + drvConfig.aktJahr + Daten.fileSep + qnr + ".pdf";
        PDFWriter pdf = new PDFWriter(xslfo, pdffile);
        String res = pdf.run();

        if (res != null) {
            Dialog.error("Beim Erstellen des PDF-Dokuments trat ein Fehler auf: " + res);
        } else {
            if (drvConfig.acrobat.length() > 0) {
                try {
                    String[] cmd = new String[2];
                    cmd[0] = drvConfig.acrobat;
                    cmd[1] = pdffile;
                    Runtime.getRuntime().exec(cmd);
                } catch (Exception ee) {
                    Dialog.error("Fehler: Acrobat Reader '" + drvConfig.acrobat + "' konnte nicht gestartet werden!");
                }
            } else {
                Dialog.error("Kein Acrobat Reader konfiguriert.");
            }
        }
    }
}
