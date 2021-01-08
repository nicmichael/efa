/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.data.efawett;

import de.nmichael.efa.*;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.util.*;
import java.io.*;
import java.util.UUID;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class EfaWett {

    public static final String EFAWETT091 = "##EFA.091.WETT##";
    public static final String EFAWETT100 = "##EFA.100.WETT##";
    public static final String EFAWETT150 = "##EFA.150.WETT##";
    public static final String EFAWETT151 = "##EFA.151.WETT##";
    public static final String EFAWETT160 = "##EFA.160.WETT##";
    public static final String EFAWETT = "##EFA.221.WETT##";
    public int wettId = -1; // ID entspricht denen in WettDefs; wird vonr readFile() *nicht* gesetzt!
    public String datei = null;
    public String kennung = null;
    // Allgemein
    public String allg_programm = null;
    public String allg_wett = null;
    public String allg_wettjahr = null;
    // Verein
    public String verein_user = null;
    public String verein_name = null;
    public String verein_mitglnr = null;
    public String verein_mitglieder = null;
    public String verein_ort = null;
    public String verein_lrv = null;
    public String verein_mitgl_in = null;
    // Meldender
    public String meld_name = null;
    public String meld_email = null;
    public String meld_bank = null;
    public String meld_blz = null;
    public String meld_kto = null;
    // Versand
    public String versand_name = null;
    public String versand_zusatz = null;
    public String versand_strasse = null;
    public String versand_ort = null;
    // DRV
    public String drv_nadel_erw_gold = null;
    public String drv_nadel_erw_silber = null;
    public String drv_nadel_jug_gold = null;
    public String drv_nadel_jug_silber = null;
    public String drv_stoff_erw = null;
    public String drv_stoff_jug = null;
    // Blauer Wimpel
    public String wimpel_mitglieder = null;
    public String wimpel_km = null;
    public String wimpel_schnitt = null;
    public int wimpel_anzMitglieder = 0;
    // DRV Wanderruderstatistik
    public String aktive_M_ab19 = null;
    public String aktive_M_bis18 = null;
    public String aktive_W_ab19 = null;
    public String aktive_W_bis18 = null;
    public String vereins_kilometer = null;
    // DRV-intern (müssen unbedingt in EfaWett.resetDrvIntern() zurückgesetzt werden!)
    public boolean drvint_meldegeldEingegangen = false;
    public int drvint_anzahlPapierFahrtenhefte = -1;
    public String drvint_notes = null;
    // Meldedaten
    public EfaWettMeldung meldung = null;
    // interne Verarbeitungsdaten (werden nicht in der Datei gespeichert)
    public boolean durchDRVbearbeitet = false;

    // Konstruktor
    public EfaWett(String datei) {
        this.datei = datei;
    }

    public EfaWett() {
        this.datei = null;
    }

    public void setFileName(String fname) {
        this.datei = fname;
    }

    public EfaWettMeldung letzteMeldung() {
        if (meldung == null) {
            return null;
        }
        EfaWettMeldung m = meldung;
        while (m.next != null) {
            m = m.next;
        }
        return m;
    }

    public void setBlank() {
        wettId = -1; // ID entspricht denen in WettDefs
        allg_programm = null;
        allg_wett = null;
        allg_wettjahr = null;
        verein_user = null;
        verein_name = null;
        verein_mitglnr = null;
        verein_mitglieder = null;
        meld_name = null;
        meld_email = null;
        meld_bank = null;
        meld_blz = null;
        meld_kto = null;
        versand_name = null;
        versand_zusatz = null;
        versand_strasse = null;
        versand_ort = null;
        wimpel_mitglieder = null;
        wimpel_km = null;
        wimpel_schnitt = null;
        aktive_M_ab19 = null;
        aktive_M_bis18 = null;
        aktive_W_ab19 = null;
        aktive_W_bis18 = null;
        vereins_kilometer = null;
        EfaWettMeldung meldung = null;
    }

    public void setProjectSettings(Project prj) {
        switch (wettId) {
            case WettDefs.DRV_FAHRTENABZEICHEN:
            case WettDefs.DRV_WANDERRUDERSTATISTIK:
                verein_user = prj.getClubGlobalAssociationLogin();
                break;
            case WettDefs.LRVBERLIN_SOMMER:
            case WettDefs.LRVBERLIN_WINTER:
            case WettDefs.LRVBERLIN_BLAUERWIMPEL:
                verein_user = prj.getClubRegionalAssociationLogin();
                break;
        }
        verein_name = prj.getClubName();
        meld_name = prj.getCompetitionSubmitterName();
        meld_email = prj.getCompetitionSubmitterEmail();
        versand_name = prj.getClubName();
        versand_zusatz = prj.getClubAddressAdditional();
        versand_strasse = prj.getClubAddressStreet();
        versand_ort = prj.getClubAddressCity();
    }

    public boolean writeFile() throws IOException {
        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFAWETT, "EfaWett.writeFile() - START");
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFAWETT, "EfaWett.writeFile(): datei == " + datei);
        }
        if (datei == null) {
            return false;
        }
        BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(datei), Daten.ENCODING_ISO));
        kennung = EfaWett.EFAWETT;
        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFAWETT, "EfaWett.writeFile(): Start writing Header and Common Data ...");
        }
        f.write(kennung + "\n");

        f.write("\n[ALLGEMEIN]\n");
        if (allg_programm != null) {
            f.write("PROGRAMM=" + allg_programm + "\n");
        }
        if (allg_wett != null) {
            f.write("WETTBEWERB=" + allg_wett + "\n");
        }
        if (allg_wettjahr != null) {
            f.write("WETTJAHR=" + allg_wettjahr + "\n");
        }

        f.write("\n[VEREIN]\n");
        if (verein_user != null) {
            f.write("VEREIN=" + verein_user + "\n");
        }
        if (verein_name != null) {
            f.write("VEREINSNAME=" + verein_name + "\n");
        }
        if (verein_mitglnr != null) {
            f.write("MITGLIEDSNUMMER=" + verein_mitglnr + "\n");
        }
        if (verein_mitglieder != null) {
            f.write("MITGLIEDER=" + verein_mitglieder + "\n");
        }
        if (verein_ort != null) {
            f.write("ORT=" + verein_ort + "\n");
        }
        if (verein_lrv != null) {
            f.write("LRV=" + verein_lrv + "\n");
        }
        if (verein_mitgl_in != null) {
            f.write("MITGLIED_IN=" + verein_mitgl_in + "\n");
        }

        if (meld_name != null || meld_email != null || meld_kto != null
                || meld_bank != null || meld_blz != null) {
            f.write("\n[MELDENDER]\n");
            if (meld_name != null) {
                f.write("NAME=" + meld_name + "\n");
            }
            if (meld_email != null) {
                f.write("EMAIL=" + meld_email + "\n");
            }
            if (meld_kto != null) {
                f.write("KTO=" + meld_kto + "\n");
            }
            if (meld_bank != null) {
                f.write("BANK=" + meld_bank + "\n");
            }
            if (meld_blz != null) {
                f.write("BLZ=" + meld_blz + "\n");
            }
        }

        if (versand_name != null || versand_strasse != null || versand_ort != null) {
            f.write("\n[VERSAND]\n");
            if (versand_name != null) {
                f.write("NAME=" + versand_name + "\n");
            }
            if (versand_zusatz != null) {
                f.write("ZUSATZ=" + versand_zusatz + "\n");
            }
            if (versand_strasse != null) {
                f.write("STRASSE=" + versand_strasse + "\n");
            }
            if (versand_ort != null) {
                f.write("ORT=" + versand_ort + "\n");
            }
        }

        if (drv_nadel_erw_gold != null || drv_nadel_erw_silber != null || drv_nadel_jug_gold != null
                || drv_nadel_jug_silber != null || drv_stoff_erw != null || drv_stoff_jug != null) {
            f.write("\n[DRV_FAHRTENABZEICHEN]\n");
            if (drv_nadel_erw_gold != null) {
                f.write("NADEL_ERW_GOLD=" + drv_nadel_erw_gold + "\n");
            }
            if (drv_nadel_erw_silber != null) {
                f.write("NADEL_ERW_SILBER=" + drv_nadel_erw_silber + "\n");
            }
            if (drv_nadel_jug_gold != null) {
                f.write("NADEL_JUG_GOLD=" + drv_nadel_jug_gold + "\n");
            }
            if (drv_nadel_jug_silber != null) {
                f.write("NADEL_JUG_SILBER=" + drv_nadel_jug_silber + "\n");
            }
            if (drv_stoff_erw != null) {
                f.write("STOFF_ERW=" + drv_stoff_erw + "\n");
            }
            if (drv_stoff_jug != null) {
                f.write("STOFF_JUG=" + drv_stoff_jug + "\n");
            }
        }

        if (wimpel_mitglieder != null || wimpel_km != null || wimpel_schnitt != null) {
            f.write("\n[BLAUER_WIMPEL]\n");
            if (wimpel_mitglieder != null) {
                f.write("MITGLIEDER=" + wimpel_mitglieder + "\n");
            }
            if (wimpel_km != null) {
                f.write("KILOMETER=" + wimpel_km + "\n");
            }
            if (wimpel_schnitt != null) {
                f.write("SCHNITT=" + wimpel_schnitt + "\n");
            }
        }

        if (aktive_M_ab19 != null || aktive_M_bis18 != null || aktive_W_ab19 != null || aktive_W_bis18 != null || vereins_kilometer != null) {
            f.write("\n[DRV_WANDERRUDERPREIS]\n");
            if (aktive_M_ab19 != null) {
                f.write("AKTIVE_M_AB19=" + aktive_M_ab19 + "\n");
            }
            if (aktive_M_bis18 != null) {
                f.write("AKTIVE_M_BIS18=" + aktive_M_bis18 + "\n");
            }
            if (aktive_W_ab19 != null) {
                f.write("AKTIVE_W_AB19=" + aktive_W_ab19 + "\n");
            }
            if (aktive_W_bis18 != null) {
                f.write("AKTIVE_W_BIS18=" + aktive_W_bis18 + "\n");
            }
            if (vereins_kilometer != null) {
                f.write("VEREINS_KILOMETER=" + vereins_kilometer + "\n");
            }
        }

        if (drvint_meldegeldEingegangen || 
            drvint_anzahlPapierFahrtenhefte >= 0 ||
            (drvint_notes != null && drvint_notes.length() > 0)) {
            f.write("\n[DRV_INTERN]\n");
            if (drvint_meldegeldEingegangen) {
                f.write("MELDEGELD_EINGEGANGEN=+\n");
            }
            if (drvint_anzahlPapierFahrtenhefte >= 0) {
                f.write("ANZAHL_PAPIERFAHRTENHEFTE=" + drvint_anzahlPapierFahrtenhefte + "\n");
            }
            if (drvint_notes != null && drvint_notes.length() > 0) {
                f.write("NOTIZEN=" + drvint_notes + "\n");
            }
        }
        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFAWETT, "EfaWett.writeFile(): Done writing Header and Common Data ...");
        }

        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFAWETT, "EfaWett.writeFile(): Start writing Meldungen ...");
        }
        EfaWettMeldung m = meldung;
        int c = 0;
        while (m != null) {
            f.write("\n[MELDUNG#" + (++c) + "]\n");
            if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFAWETT, "EfaWett.writeFile(): Writing Meldung " + c + " == " + m.vorname + " " + m.nachname);
            }

            // Teilnehmerdaten
            if (m.personID != null) {
                f.write("PERSONID=" + m.personID.toString() + "\n");
            }
            if (m.nachname != null) {
                f.write("NACHNAME=" + m.nachname + "\n");
            }
            if (m.vorname != null) {
                f.write("VORNAME=" + m.vorname + "\n");
            }
            if (m.jahrgang != null) {
                f.write("JAHRGANG=" + m.jahrgang + "\n");
            }
            if (m.geschlecht != null) {
                f.write("GESCHLECHT=" + m.geschlecht + "\n");
            }
            if (m.gruppe != null) {
                f.write("GRUPPE=" + m.gruppe + "\n");
            }
            if (m.abzeichen != null) {
                f.write("ABZEICHEN=" + m.abzeichen + "\n");
            }
            if (m.drv_fahrtenheft != null) {
                f.write("DRV_FAHRTENHEFT=" + m.drv_fahrtenheft + "\n");
            }
            if (m.drv_teilnNr != null) {
                f.write("DRV_TEILNEHMERNR=" + m.drv_teilnNr + "\n");
            }
            if (m.drv_anzAbzeichen != null) {
                f.write("DRV_ANZABZEICHEN=" + m.drv_anzAbzeichen + "\n");
            }
            if (m.drv_gesKm != null) {
                f.write("DRV_GESKM=" + m.drv_gesKm + "\n");
            }
            if (m.drv_anzAbzeichenAB != null) {
                f.write("DRV_ANZABZEICHEN_AB=" + m.drv_anzAbzeichenAB + "\n");
            }
            if (m.drv_gesKmAB != null) {
                f.write("DRV_GESKM_AB=" + m.drv_gesKmAB + "\n");
            }
            if (m.drv_aequatorpreis != null) {
                f.write("DRV_AEQUATORPREIS=" + m.drv_aequatorpreis + "\n");
            }
            if (m.kilometer != null) {
                f.write("KILOMETER=" + m.kilometer + "\n");
            }
            if (m.restkm != null) {
                f.write("RESTKM=" + m.restkm + "\n");
            }
            if (m.anschrift != null) {
                f.write("ANSCHRIFT=" + m.anschrift + "\n");
            }
            for (int i = 0; i < m.fahrt.length; i++) {
                if (m.fahrt[i][0] != null) {
                    f.write("FAHRT" + (i + 1) + "=");
                    for (int j = 0; j < m.fahrt[i].length; j++) {
                        if (m.fahrt[i][j] != null) {
                            f.write(m.fahrt[i][j] + "|");
                        }
                        if (j + 1 == m.fahrt[i].length || m.fahrt[i][j + 1] == null) {
                            f.write("\n");
                            break;
                        }
                    }
                }
            }

            // DRV-Wanderruderstatistik
            if (m.drvWS_LfdNr != null) {
                f.write("DRVWS_LFDNR=" + m.drvWS_LfdNr + "\n");
            }
            if (m.drvWS_StartZiel != null) {
                f.write("DRVWS_STARTZIEL=" + m.drvWS_StartZiel + "\n");
            }
            if (m.drvWS_Strecke != null) {
                f.write("DRVWS_STRECKE=" + m.drvWS_Strecke + "\n");
            }
            if (m.drvWS_Gewaesser != null) {
                f.write("DRVWS_GEWAESSER=" + m.drvWS_Gewaesser + "\n");
            }
            if (m.drvWS_Km != null) {
                f.write("DRVWS_KM=" + m.drvWS_Km + "\n");
            }
            if (m.drvWS_Tage != null) {
                f.write("DRVWS_TAGE=" + m.drvWS_Tage + "\n");
            }
            if (m.drvWS_Teilnehmer != null) {
                f.write("DRVWS_TEILNEHMER=" + m.drvWS_Teilnehmer + "\n");
            }
            if (m.drvWS_MannschKm != null) {
                f.write("DRVWS_MANNSCHKM=" + m.drvWS_MannschKm + "\n");
            }
            if (m.drvWS_MaennerAnz != null) {
                f.write("DRVWS_MAENNERANZ=" + m.drvWS_MaennerAnz + "\n");
            }
            if (m.drvWS_MaennerKm != null) {
                f.write("DRVWS_MAENNERKM=" + m.drvWS_MaennerKm + "\n");
            }
            if (m.drvWS_JuniorenAnz != null) {
                f.write("DRVWS_JUNIORENANZ=" + m.drvWS_JuniorenAnz + "\n");
            }
            if (m.drvWS_JuniorenKm != null) {
                f.write("DRVWS_JUNIORENKM=" + m.drvWS_JuniorenKm + "\n");
            }
            if (m.drvWS_FrauenAnz != null) {
                f.write("DRVWS_FRAUENANZ=" + m.drvWS_FrauenAnz + "\n");
            }
            if (m.drvWS_FrauenKm != null) {
                f.write("DRVWS_FRAUENKM=" + m.drvWS_FrauenKm + "\n");
            }
            if (m.drvWS_JuniorinnenAnz != null) {
                f.write("DRVWS_JUNIORINNENANZ=" + m.drvWS_JuniorinnenAnz + "\n");
            }
            if (m.drvWS_JuniorinnenKm != null) {
                f.write("DRVWS_JUNIORINNENKM=" + m.drvWS_JuniorinnenKm + "\n");
            }
            if (m.drvWS_Bemerkungen != null) {
                f.write("DRVWS_BEMERKUNGEN=" + m.drvWS_Bemerkungen + "\n");
            }

            // Bearbeitungsdaten (DRV)
            if (durchDRVbearbeitet) {
                f.write("DRVINTERN_WIRDGEWERTET=" + (m.drvint_wirdGewertet ? "+" : "-") + "\n");
                if (m.drvint_nichtGewertetGrund != null) {
                    f.write("DRVINTER_NICHTGEWERTETGRUND=" + m.drvint_nichtGewertetGrund + "\n");
                }
                m.drvint_wirdGewertetExplizitGesetzt = true;
            }
            if (m.drvint_fahrtErfuellt) {
                f.write("DRVINTERN_FAHRT_ERFUELLT=+\n");
            }
            if (m.drvint_geprueft) {
                f.write("DRVINTERN_GEPRUEFT=+\n");
            }
            if (m.drvint_nachfrage != null && m.drvint_nachfrage.length() > 0) {
                f.write("DRVINTERN_NACHFRAGE=" + m.drvint_nachfrage + "\n");
            }

            m = m.next;
        }
        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFAWETT, "EfaWett.writeFile(): Done writing Meldungen ...");
        }
        f.close();
        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            File ff = new File(datei);
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFAWETT, "EfaWett.writeFile(): Filesize ff.length() == " + ff.length());
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFAWETT, "EfaWett.writeFile() - END");
        }

        return true;
    }

    // Lesen der Datei "datei"
    public boolean readFile() throws IOException {
        if (datei == null) {
            return false;
        }
        BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(datei), Daten.ENCODING_ISO));

        // Dateiformat prüfen
        String s = f.readLine();
        if (s == null || (!s.startsWith(EFAWETT) && !s.startsWith(EFAWETT151) && !s.startsWith(EFAWETT150) && !s.startsWith(EFAWETT100) && !s.startsWith(EFAWETT091))) {
            f.close();
            return false;
        }
        kennung = s;
        if (kennung.indexOf(" ") > 0) {
            kennung = kennung.substring(0, kennung.indexOf(" "));
        }

        // alle Felder löschen
        setBlank();

        EfaWettMeldung m = null; // aktuell bearbeiteter Teilnehmer
        String block = "";
        boolean newblock = false; // true, wenn der Block im aktuellen Schleifendurchlauf erstmals gefunden wurde


        // Datei lesen
        while ((s = f.readLine()) != null) {
            s = s.trim();
            if (s.equals("")) {
                continue;
            }
            if (s.charAt(0) == '#') {
                continue;
            }

            // neuer Blockanfang?
            if (s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
                block = s.substring(1, s.length() - 1);
                newblock = true;
            } else {
                newblock = false;
            }

            // fertigen Teilnehmer speichern
            if (newblock && m != null) {
                if (letzteMeldung() == null) {
                    meldung = m;
                } else {
                    letzteMeldung().next = m;
                }
                m = null;
            }

            // Daten aus Blöcken extrahieren
            if (block.equals("ALLGEMEIN")) {
                if (s.startsWith("PROGRAMM=")) {
                    allg_programm = s.substring(9, s.length());
                }
                if (s.startsWith("WETTBEWERB=")) {
                    allg_wett = s.substring(11, s.length());
                }
                if (s.startsWith("WETTJAHR=")) {
                    allg_wettjahr = s.substring(9, s.length());
                }
            }
            if (block.equals("VEREIN")) {
                if (s.startsWith("VEREIN=")) {
                    verein_user = s.substring(7, s.length());
                }
                if (s.startsWith("VEREINSNAME=")) {
                    verein_name = s.substring(12, s.length());
                }
                if (s.startsWith("MITGLIEDSNUMMER=")) {
                    verein_mitglnr = s.substring(16, s.length());
                }
                if (s.startsWith("MITGLIEDER=")) {
                    verein_mitglieder = s.substring(11, s.length());
                }
                if (s.startsWith("ORT=")) {
                    verein_ort = s.substring(4, s.length());
                }
                if (s.startsWith("LRV=")) {
                    verein_lrv = s.substring(4, s.length());
                }
                if (s.startsWith("MITGLIED_IN=")) {
                    verein_mitgl_in = s.substring(12, s.length());
                }
            }
            if (block.equals("MELDENDER")) {
                if (s.startsWith("NAME=")) {
                    meld_name = s.substring(5, s.length());
                }
                if (s.startsWith("EMAIL=")) {
                    meld_email = s.substring(6, s.length());
                }
                if (s.startsWith("KTO=")) {
                    meld_kto = s.substring(4, s.length());
                }
                if (s.startsWith("BANK=")) {
                    meld_bank = s.substring(5, s.length());
                }
                if (s.startsWith("BLZ=")) {
                    meld_blz = s.substring(4, s.length());
                }
            }
            if (block.equals("VERSAND")) {
                if (s.startsWith("NAME=")) {
                    versand_name = s.substring(5, s.length());
                }
                if (s.startsWith("ZUSATZ=")) {
                    versand_zusatz = s.substring(7, s.length());
                }
                if (s.startsWith("STRASSE=")) {
                    versand_strasse = s.substring(8, s.length());
                }
                if (s.startsWith("ORT=")) {
                    versand_ort = s.substring(4, s.length());
                }
            }
            if (block.equals("DRV_FAHRTENABZEICHEN")) {
                if (s.startsWith("NADEL_ERW_GOLD=")) {
                    drv_nadel_erw_gold = s.substring(15, s.length());
                }
                if (s.startsWith("NADEL_ERW_SILBER=")) {
                    drv_nadel_erw_silber = s.substring(17, s.length());
                }
                if (s.startsWith("NADEL_JUG_GOLD=")) {
                    drv_nadel_jug_gold = s.substring(15, s.length());
                }
                if (s.startsWith("NADEL_JUG_SILBER=")) {
                    drv_nadel_jug_silber = s.substring(17, s.length());
                }
                if (s.startsWith("STOFF_ERW=")) {
                    drv_stoff_erw = s.substring(10, s.length());
                }
                if (s.startsWith("STOFF_JUG=")) {
                    drv_stoff_jug = s.substring(10, s.length());
                }
            }
            if (block.equals("BLAUER_WIMPEL")) {
                if (s.startsWith("MITGLIEDER=")) {
                    wimpel_mitglieder = s.substring(11, s.length());
                }
                if (s.startsWith("KILOMETER=")) {
                    wimpel_km = s.substring(10, s.length());
                }
                if (s.startsWith("SCHNITT=")) {
                    wimpel_schnitt = s.substring(8, s.length());
                }
            }
            if (block.equals("DRV_WANDERRUDERPREIS")) {
                if (s.startsWith("AKTIVE_M_AB19=")) {
                    aktive_M_ab19 = s.substring(14, s.length());
                }
                if (s.startsWith("AKTIVE_M_BIS18=")) {
                    aktive_M_bis18 = s.substring(15, s.length());
                }
                if (s.startsWith("AKTIVE_W_AB19=")) {
                    aktive_W_ab19 = s.substring(14, s.length());
                }
                if (s.startsWith("AKTIVE_W_BIS18=")) {
                    aktive_W_bis18 = s.substring(15, s.length());
                }
                if (s.startsWith("VEREINS_KILOMETER=")) {
                    vereins_kilometer = s.substring(18, s.length());
                }
            }
            if (block.equals("DRV_INTERN")) {
                if (s.startsWith("MELDEGELD_EINGEGANGEN=")) {
                    drvint_meldegeldEingegangen = s.substring(22, s.length()).equals("+");
                }
                if (s.startsWith("ANZAHL_PAPIERFAHRTENHEFTE=")) {
                    drvint_anzahlPapierFahrtenhefte = EfaUtil.string2int(s.substring(26, s.length()), 0);
                }
                if (s.startsWith("NOTIZEN=")) {
                    drvint_notes = s.substring(8);
                }
            }
            if (block.startsWith("MELDUNG#")) {
                if (newblock) {
                    m = new EfaWettMeldung();
                }

                // Teilnehmerdaten
                if (s.startsWith("PERSONID=")) {
                    m.personID = UUID.fromString(s.substring(9, s.length()));
                }
                if (s.startsWith("NACHNAME=")) {
                    m.nachname = s.substring(9, s.length());
                }
                if (s.startsWith("VORNAME=")) {
                    m.vorname = s.substring(8, s.length());
                }
                if (s.startsWith("JAHRGANG=")) {
                    m.jahrgang = s.substring(9, s.length());
                }
                if (s.startsWith("GESCHLECHT=")) {
                    m.geschlecht = s.substring(11, s.length());
                }
                if (s.startsWith("GRUPPE=")) {
                    m.gruppe = s.substring(7, s.length());
                }
                if (s.startsWith("ABZEICHEN=")) {
                    m.abzeichen = s.substring(10, s.length());
                }
                if (s.startsWith("DRV_FAHRTENHEFT=")) {
                    m.drv_fahrtenheft = s.substring(16, s.length());
                }
                if (s.startsWith("DRV_TEILNEHMERNR=")) {
                    m.drv_teilnNr = s.substring(17, s.length());
                }
                if (s.startsWith("DRV_ANZABZEICHEN=")) {
                    m.drv_anzAbzeichen = s.substring(17, s.length());
                }
                if (s.startsWith("DRV_GESKM=")) {
                    m.drv_gesKm = s.substring(10, s.length());
                }
                if (s.startsWith("DRV_ANZABZEICHEN_AB=")) {
                    m.drv_anzAbzeichenAB = s.substring(20, s.length());
                }
                if (s.startsWith("DRV_GESKM_AB=")) {
                    m.drv_gesKmAB = s.substring(13, s.length());
                }
                if (s.startsWith("DRV_AEQUATORPREIS=")) {
                    m.drv_aequatorpreis = s.substring(18, s.length());
                }
                if (s.startsWith("KILOMETER=")) {
                    m.kilometer = s.substring(10, s.length());
                }
                if (s.startsWith("RESTKM=")) {
                    m.restkm = s.substring(7, s.length());
                }
                if (s.startsWith("ANSCHRIFT=")) {
                    m.anschrift = s.substring(10, s.length());
                }
                if (s.startsWith("FAHRT")) {
                    TMJ pos = EfaUtil.string2date(s, -1, -1, -1);
                    pos.tag--;
                    if (pos.tag >= 0 && pos.tag < m.fahrt.length && s.indexOf("=") > 0) {
                        s = s.substring(s.indexOf("=") + 1, s.length());
                        String[] a = EfaUtil.kommaList2Arr(s, '|');
                        for (int i = 0; i < a.length; i++) {
                            if (i < m.fahrt[pos.tag].length) {
                                m.fahrt[pos.tag][i] = a[i];
                            }
                        }
                        s = "-";
                    }
                }

                // DRV-Wanderruderstatistik
                if (s.startsWith("DRVWS_LFDNR=")) {
                    m.drvWS_LfdNr = s.substring(12, s.length());
                }
                if (s.startsWith("DRVWS_STARTZIEL=")) {
                    m.drvWS_StartZiel = s.substring(16, s.length());
                }
                if (s.startsWith("DRVWS_STRECKE=")) {
                    m.drvWS_Strecke = s.substring(14, s.length());
                }
                if (s.startsWith("DRVWS_GEWAESSER=")) {
                    m.drvWS_Gewaesser = s.substring(16, s.length());
                }
                if (s.startsWith("DRVWS_KM=")) {
                    m.drvWS_Km = s.substring(9, s.length());
                }
                if (s.startsWith("DRVWS_TAGE=")) {
                    m.drvWS_Tage = s.substring(11, s.length());
                }
                if (s.startsWith("DRVWS_TEILNEHMER=")) {
                    m.drvWS_Teilnehmer = s.substring(17, s.length());
                }
                if (s.startsWith("DRVWS_MANNSCHKM=")) {
                    m.drvWS_MannschKm = s.substring(16, s.length());
                }
                if (s.startsWith("DRVWS_MAENNERANZ=")) {
                    m.drvWS_MaennerAnz = s.substring(17, s.length());
                }
                if (s.startsWith("DRVWS_MAENNERKM=")) {
                    m.drvWS_MaennerKm = s.substring(16, s.length());
                }
                if (s.startsWith("DRVWS_JUNIORENANZ=")) {
                    m.drvWS_JuniorenAnz = s.substring(18, s.length());
                }
                if (s.startsWith("DRVWS_JUNIORENKM=")) {
                    m.drvWS_JuniorenKm = s.substring(17, s.length());
                }
                if (s.startsWith("DRVWS_FRAUENANZ=")) {
                    m.drvWS_FrauenAnz = s.substring(16, s.length());
                }
                if (s.startsWith("DRVWS_FRAUENKM=")) {
                    m.drvWS_FrauenKm = s.substring(15, s.length());
                }
                if (s.startsWith("DRVWS_JUNIORINNENANZ=")) {
                    m.drvWS_JuniorinnenAnz = s.substring(21, s.length());
                }
                if (s.startsWith("DRVWS_JUNIORINNENKM=")) {
                    m.drvWS_JuniorinnenKm = s.substring(20, s.length());
                }
                if (s.startsWith("DRVWS_BEMERKUNGEN=")) {
                    m.drvWS_Bemerkungen = s.substring(18, s.length());
                }

                // DRV Bearbeitungsstatus
                if (s.startsWith("DRVINTERN_WIRDGEWERTET=")) {
                    m.drvint_wirdGewertet = s.substring(23, s.length()).equals("+");
                    m.drvint_wirdGewertetExplizitGesetzt = true;
                }
                if (s.startsWith("DRVINTER_NICHTGEWERTETGRUND=")) {
                    m.drvint_nichtGewertetGrund = s.substring(28, s.length());
                }
                if (s.startsWith("DRVINTERN_FAHRT_ERFUELLT=+")) {
                    m.drvint_fahrtErfuellt = true;
                }
                if (s.startsWith("DRVINTERN_GEPRUEFT=+")) {
                    m.drvint_geprueft = true;
                }
                if (s.startsWith("DRVINTERN_NACHFRAGE=")) {
                    m.drvint_nachfrage = s.substring(20);
                }
            }
        }

        // Teilnehmerdaten speichern
        if (m != null) {
            if (letzteMeldung() == null) {
                meldung = m;
            } else {
                letzteMeldung().next = m;
            }
        }
        f.close();
        return true;
    }

    public void resetDrvIntern() {
        this.drvint_meldegeldEingegangen = false;
        this.drvint_anzahlPapierFahrtenhefte = -1;
        this.drvint_notes = null;

        EfaWettMeldung m = meldung;
        int c = 0;
        while (m != null) {
            m.drvint_fahrtErfuellt = false;
            m.drvint_wirdGewertet = false;
            m.drvint_wirdGewertetExplizitGesetzt = false;
            m.drvint_nichtGewertetGrund = null;
            m.drvint_geprueft = false;
            m.drvint_nachfrage = null;
            m = m.next;
        }
    }
    
    public int getNumberOfMeldungen() {
        int c = 0;
        EfaWettMeldung m = this.meldung;
        while (m != null) {
            c++;
            m = m.next;
        }
        return c;
    }
}
