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

import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.efa1.DatenListe;
import de.nmichael.efa.util.*;
import java.io.*;
import java.util.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class WettDefs extends DatenListe {

    public static final String KENNUNG150 = "##EFA.150.VEREIN##";
    public static final String KENNUNG170 = "##EFA.170.WETTDEFS##";
    public static final String KENNUNG190 = "##EFA.190.WETTDEFS##";
    public static final int ANZWETT = 8; // Anzahl der Wettbewerbe (s. folgende Konstanten)

    /* Die folgenden Konstanten müssen mit den WETT_* Konstanten aus StatistikDaten übereinstimmen
    (abgesehen davon, daß die Werte von WETT_* genau um 200 größer sind als die Werte hier) */
    public static final int DRV_FAHRTENABZEICHEN = 0;
    public static final int DRV_WANDERRUDERSTATISTIK = 1;
    public static final int LRVBERLIN_SOMMER = 2;
    public static final int LRVBERLIN_WINTER = 3;
    public static final int LRVBERLIN_BLAUERWIMPEL = 4;
    public static final int LRVBRB_WANDERRUDERWETT = 5;
    public static final int LRVBRB_FAHRTENWETT = 6;
    public static final int LRVMVP_WANDERRUDERWETT = 7;
    public static final String STR_DRV_FAHRTENABZEICHEN = "DRV.FAHRTENABZEICHEN";
    public static final String STR_DRV_WANDERRUDERSTATISTIK = "DRV.WANDERRUDERSTATISTIK";
    public static final String STR_LRVBERLIN_SOMMER = "LRVBERLIN.SOMMER";
    public static final String STR_LRVBERLIN_WINTER = "LRVBERLIN.WINTER";
    public static final String STR_LRVBERLIN_BLAUERWIMPEL = "LRVBERLIN.BLAUERWIMPEL";
    public static final String STR_LRVBRB_WANDERRUDERWETT = "LRVBRANDENBURG.WANDERRUDERWETTBEWERB";
    public static final String STR_LRVBRB_FAHRTENWETT = "LRVBRANDENBURG.FAHRTENWETTBEWERB";
    public static final String STR_LRVMVP_WANDERRUDERWETT = "LRVMECKLENBURGVORPOMMERN.WANDERRUDERWETTBEWERB";
    public static final int BEHIND_JA = 0;
    public static final int BEHIND_NEIN = 1;
    public static final int BEHIND_MOEGLICH = 2;
    public static final int DRV_AEQUATOR_KM = 40077;
    public static final String EFW_LETZTE_AKTUALISIERUNG = "EFW_LETZTE_AKTUALISIERUNG";
    public static final String EFW_STAND_DER_DATEN = "EFW_STAND_DER_DATEN";
    public static final String EFW_URL_EINSENDEN = "EFW_URL_EINSENDEN";
    public static final String EFW_URL_ABRUFEN = "EFW_URL_ABRUFEN";
    public static final String EFW_DRV_URL_PUBKEYS = "EFW_DRV_URL_PUBKEYS";
    public static final String EFW_DRV_FA_MELD_ERW = "EFW_DRV_FA_MELD_ERW";
    public static final String EFW_DRV_FA_MELD_JUG = "EFW_DRV_FA_MELD_JUG";
    public static final String EFW_DRV_FA_NADEL_ERW_SILBER = "EFW_DRV_FA_NADEL_ERW_SILBER";
    public static final String EFW_DRV_FA_NADEL_ERW_GOLD = "EFW_DRV_FA_NADEL_ERW_GOLD";
    public static final String EFW_DRV_FA_NADEL_JUG_SILBER = "EFW_DRV_FA_NADEL_JUG_SILBER";
    public static final String EFW_DRV_FA_NADEL_JUG_GOLD = "EFW_DRV_FA_NADEL_JUG_GOLD";
    public static final String EFW_DRV_FA_STOFF_ERW = "EFW_DRV_FA_STOFF_ERW";
    public static final String EFW_DRV_FA_STOFF_JUG = "EFW_DRV_FA_STOFF_JUG";
    public static final String EFW_DRV_KONTO = "EFW_DRV_KONTO";
    public static final String EFW_DRV_ANSCHRIFT = "EFW_DRV_ANSCHRIFT";
    public static final String EFW_LRVBLN_SOMM_MELD_ERW = "EFW_LRVBLN_SOMM_MELD_ERW";
    public static final String EFW_LRVBLN_SOMM_MELD_JUG = "EFW_LRVBLN_SOMM_MELD_JUG";
    public static final String EFW_LRVBLN_WINT_MELD_ERW = "EFW_LRVBLN_WINT_MELD_ERW";
    public static final String EFW_LRVBLN_WINT_MELD_JUG = "EFW_LRVBLN_WINT_MELD_JUG";
    public static final String EFW_LRVBLN_BLWI_MELD_ERW = "EFW_LRVBLN_BLWI_MELD_ERW";
    public static final String EFW_LRVBLN_BLWI_MELD_JUG = "EFW_LRVBLN_BLWI_MELD_JUG";
    public static final String EFW_LRVBLN_KONTO = "EFW_LRVBLN_KONTO";
    // ------------- efaWett Konfiguration ----------
    public String efw_letzte_aktualisierung;
    public String efw_stand_der_daten;
    public String efw_url_einsenden;
    public String efw_url_abrufen;
    public String efw_drv_url_pubkeys;
    public int efw_drv_fa_meld_erw;
    public int efw_drv_fa_meld_jug;
    public int efw_drv_fa_nadel_erw_silber;
    public int efw_drv_fa_nadel_erw_gold;
    public int efw_drv_fa_nadel_jug_silber;
    public int efw_drv_fa_nadel_jug_gold;
    public int efw_drv_fa_stoff_erw;
    public int efw_drv_fa_stoff_jug;
    public String efw_drv_konto;
    public String efw_drv_anschrift;
    public int efw_lrvbln_somm_meld_erw;
    public int efw_lrvbln_somm_meld_jug;
    public int efw_lrvbln_wint_meld_erw;
    public int efw_lrvbln_wint_meld_jug;
    public int efw_lrvbln_blwi_meld_erw;
    public int efw_lrvbln_blwi_meld_jug;
    public String efw_lrvbln_konto;
    // ------------- WettDefs Konfiguration ----------
    private Vector wettDefs = new Vector();

    public WettDefs(String pdat) {
        super(pdat, 0, 0, true);
        kennung = KENNUNG190;
    }

    // Einstellungen aus dem Fahrtenbuch auslesen
    public synchronized boolean readEinstellungen() {
        String s;
        iniDefaultsForEFW();

        try {
            WettDef wettDef = null;
            Vector gruppen = null;
            String block = "";
            boolean newblock = false;
            while ((s = freadLine()) != null) {
                s = s.trim();
                if (s.startsWith("#")) {
                    continue;
                }
                if (s.length() == 0) {
                    continue;
                }

                // neuer Blockanfang?
                if (s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
                    block = s.substring(1, s.length() - 1);
                    newblock = true;
                } else {
                    newblock = false;
                }

                // Abschnitt EFW
                if (block.equals("EFW")) {
                    if (s.startsWith(EFW_LETZTE_AKTUALISIERUNG + "=")) {
                        efw_letzte_aktualisierung = s.substring(EFW_LETZTE_AKTUALISIERUNG.length() + 1, s.length());
                    }
                    if (s.startsWith(EFW_STAND_DER_DATEN + "=")) {
                        efw_stand_der_daten = s.substring(EFW_STAND_DER_DATEN.length() + 1, s.length());
                    }
                    if (s.startsWith(EFW_URL_EINSENDEN + "=")) {
                        efw_url_einsenden = s.substring(EFW_URL_EINSENDEN.length() + 1, s.length());
                    }
                    if (s.startsWith(EFW_URL_ABRUFEN + "=")) {
                        efw_url_abrufen = s.substring(EFW_URL_ABRUFEN.length() + 1, s.length());
                    }
                    if (s.startsWith(EFW_DRV_URL_PUBKEYS + "=")) {
                        efw_drv_url_pubkeys = s.substring(EFW_DRV_URL_PUBKEYS.length() + 1, s.length());
                    }
                    if (s.startsWith(EFW_DRV_FA_MELD_ERW + "=")) {
                        efw_drv_fa_meld_erw = EfaUtil.string2int(s.substring(EFW_DRV_FA_MELD_ERW.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_DRV_FA_MELD_JUG + "=")) {
                        efw_drv_fa_meld_jug = EfaUtil.string2int(s.substring(EFW_DRV_FA_MELD_JUG.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_DRV_FA_NADEL_ERW_SILBER + "=")) {
                        efw_drv_fa_nadel_erw_silber = EfaUtil.string2int(s.substring(EFW_DRV_FA_NADEL_ERW_SILBER.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_DRV_FA_NADEL_ERW_GOLD + "=")) {
                        efw_drv_fa_nadel_erw_gold = EfaUtil.string2int(s.substring(EFW_DRV_FA_NADEL_ERW_GOLD.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_DRV_FA_NADEL_JUG_SILBER + "=")) {
                        efw_drv_fa_nadel_jug_silber = EfaUtil.string2int(s.substring(EFW_DRV_FA_NADEL_JUG_SILBER.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_DRV_FA_NADEL_JUG_GOLD + "=")) {
                        efw_drv_fa_nadel_jug_gold = EfaUtil.string2int(s.substring(EFW_DRV_FA_NADEL_JUG_GOLD.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_DRV_FA_STOFF_ERW + "=")) {
                        efw_drv_fa_stoff_erw = EfaUtil.string2int(s.substring(EFW_DRV_FA_STOFF_ERW.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_DRV_FA_STOFF_JUG + "=")) {
                        efw_drv_fa_stoff_jug = EfaUtil.string2int(s.substring(EFW_DRV_FA_STOFF_JUG.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_DRV_KONTO + "=")) {
                        efw_drv_konto = s.substring(EFW_DRV_KONTO.length() + 1, s.length());
                    }
                    if (s.startsWith(EFW_DRV_ANSCHRIFT + "=")) {
                        efw_drv_anschrift = s.substring(EFW_DRV_ANSCHRIFT.length() + 1, s.length());
                    }
                    if (s.startsWith(EFW_LRVBLN_SOMM_MELD_ERW + "=")) {
                        efw_lrvbln_somm_meld_erw = EfaUtil.string2int(s.substring(EFW_LRVBLN_SOMM_MELD_ERW.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_LRVBLN_SOMM_MELD_JUG + "=")) {
                        efw_lrvbln_somm_meld_jug = EfaUtil.string2int(s.substring(EFW_LRVBLN_SOMM_MELD_JUG.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_LRVBLN_WINT_MELD_ERW + "=")) {
                        efw_lrvbln_wint_meld_erw = EfaUtil.string2int(s.substring(EFW_LRVBLN_WINT_MELD_ERW.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_LRVBLN_WINT_MELD_JUG + "=")) {
                        efw_lrvbln_wint_meld_jug = EfaUtil.string2int(s.substring(EFW_LRVBLN_WINT_MELD_JUG.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_LRVBLN_BLWI_MELD_ERW + "=")) {
                        efw_lrvbln_blwi_meld_erw = EfaUtil.string2int(s.substring(EFW_LRVBLN_BLWI_MELD_ERW.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_LRVBLN_BLWI_MELD_JUG + "=")) {
                        efw_lrvbln_blwi_meld_jug = EfaUtil.string2int(s.substring(EFW_LRVBLN_BLWI_MELD_JUG.length() + 1, s.length()), 0);
                    }
                    if (s.startsWith(EFW_LRVBLN_KONTO + "=")) {
                        efw_lrvbln_konto = s.substring(EFW_LRVBLN_KONTO.length() + 1, s.length());
                    }
                }

                if (block.equals("WETTDEF")) {
                    if (newblock) {
                        if (wettDef != null) {
                            storeWettDef(wettDef, gruppen);
                        }
                        wettDef = new WettDef();
                        gruppen = null;
                    }
                    if (s.startsWith("NAME=")) {
                        wettDef.name = s.substring(5, s.length());
                    }
                    if (s.startsWith("KURZNAME=")) {
                        wettDef.kurzname = s.substring(9, s.length());
                    }
                    if (s.startsWith("KEY=")) {
                        wettDef.key = s.substring(4, s.length());
                        if (wettDef.key.equals(STR_DRV_FAHRTENABZEICHEN)) {
                            wettDef.wettid = DRV_FAHRTENABZEICHEN;
                        }
                        if (wettDef.key.equals(STR_DRV_WANDERRUDERSTATISTIK)) {
                            wettDef.wettid = DRV_WANDERRUDERSTATISTIK;
                        }
                        if (wettDef.key.equals(STR_LRVBERLIN_SOMMER)) {
                            wettDef.wettid = LRVBERLIN_SOMMER;
                        }
                        if (wettDef.key.equals(STR_LRVBERLIN_WINTER)) {
                            wettDef.wettid = LRVBERLIN_WINTER;
                        }
                        if (wettDef.key.equals(STR_LRVBERLIN_BLAUERWIMPEL)) {
                            wettDef.wettid = LRVBERLIN_BLAUERWIMPEL;
                        }
                        if (wettDef.key.equals(STR_LRVBRB_WANDERRUDERWETT)) {
                            wettDef.wettid = LRVBRB_WANDERRUDERWETT;
                        }
                        if (wettDef.key.equals(STR_LRVBRB_FAHRTENWETT)) {
                            wettDef.wettid = LRVBRB_FAHRTENWETT;
                        }
                        if (wettDef.key.equals(STR_LRVMVP_WANDERRUDERWETT)) {
                            wettDef.wettid = LRVMVP_WANDERRUDERWETT;
                        }
                    }
                    if (s.startsWith("GUELTIG_VON=")) {
                        wettDef.gueltig_von = EfaUtil.string2int(s.substring(12, s.length()), 1);
                    }
                    if (s.startsWith("GUELTIG_BIS=")) {
                        wettDef.gueltig_bis = EfaUtil.string2int(s.substring(12, s.length()), 9999);
                    }
                    if (s.startsWith("GUELTIG_PRIO=")) {
                        wettDef.gueltig_prio = EfaUtil.string2int(s.substring(13, s.length()), 0);
                    }
                    if (s.startsWith("VON=")) {
                        wettDef.von = EfaUtil.string2date(s.substring(4, s.length()), 1, 1, 0);
                    }
                    if (s.startsWith("BIS=")) {
                        wettDef.bis = EfaUtil.string2date(s.substring(4, s.length()), 31, 12, 0);
                    }
                    if (s.startsWith("GRUPPEN[")) {
                        int index = -1;
                        String field = null;
                        String value = null;
                        int pos1, pos2;
                        pos2 = s.indexOf("]");
                        if (pos2 > 0) {
                            index = EfaUtil.string2int(s.substring(8, pos2), -1);
                            s = s.substring(pos2 + 1, s.length());
                            pos1 = s.indexOf("[");
                            pos2 = s.indexOf("]");
                            if (pos1 == 0 && pos2 > 1) {
                                field = s.substring(pos1 + 1, pos2);
                            }
                        }
                        pos2 = s.indexOf("=");
                        if (pos2 >= 0) {
                            value = s.substring(pos2 + 1, s.length()).trim();
                        }
                        if (index >= 0 && field != null && field.length() > 0 && value != null && value.length() > 0) {
                            if (gruppen == null) {
                                gruppen = new Vector();
                            }
                            while (gruppen.size() < index + 1) {
                                gruppen.add(new WettDefGruppe());
                            }
                            WettDefGruppe g = (WettDefGruppe) gruppen.get(index);
                            int i_value = EfaUtil.string2int(value, -1);
                            if (field.equals("GRUPPE")) {
                                g.gruppe = i_value;
                            }
                            if (field.equals("UNTERGRUPPE")) {
                                g.untergruppe = i_value;
                            }
                            if (field.equals("BEZEICHNUNG")) {
                                g.bezeichnung = value;
                            }
                            if (field.equals("GESCHLECHT")) {
                                g.geschlecht = i_value;
                            }
                            if (field.equals("HOECHSTALTER")) {
                                g.hoechstalter = i_value;
                            }
                            if (field.equals("MINDALTER")) {
                                g.mindalter = i_value;
                            }
                            if (field.equals("KM")) {
                                g.km = i_value;
                            }
                            if (field.equals("ZUSATZ")) {
                                g.zusatz = i_value;
                            }
                            if (field.equals("ZUSATZ2")) {
                                g.zusatz2 = i_value;
                            }
                            if (field.equals("ZUSATZ3")) {
                                g.zusatz3 = i_value;
                            }
                            if (field.equals("ZUSATZ4")) {
                                g.zusatz4 = i_value;
                            }
                            if (field.equals("BEHINDERUNG")) {
                                g.behinderung = i_value;
                            }
                        }
                    }
                }
            }
            if (wettDef != null) {
                storeWettDef(wettDef, gruppen);
            }
        } catch (IOException e) {
            errReadingFile(dat, e.getMessage());
            return false;
        }
        return true;
    }

    public synchronized boolean writeEinstellungen() {
        if (efw_letzte_aktualisierung == null) {
            iniDefaultsForEFW();
        }
        try {
            fwrite("\n[EFW]\n");
            fwrite(EFW_LETZTE_AKTUALISIERUNG + "=" + efw_letzte_aktualisierung + "\n");
            fwrite(EFW_STAND_DER_DATEN + "=" + efw_stand_der_daten + "\n");
            fwrite(EFW_URL_EINSENDEN + "=" + efw_url_einsenden + "\n");
            fwrite(EFW_URL_ABRUFEN + "=" + efw_url_abrufen + "\n");
            fwrite(EFW_DRV_URL_PUBKEYS + "=" + efw_drv_url_pubkeys + "\n");
            fwrite(EFW_DRV_FA_MELD_ERW + "=" + efw_drv_fa_meld_erw + "\n");
            fwrite(EFW_DRV_FA_MELD_JUG + "=" + efw_drv_fa_meld_jug + "\n");
            fwrite(EFW_DRV_FA_NADEL_ERW_SILBER + "=" + efw_drv_fa_nadel_erw_silber + "\n");
            fwrite(EFW_DRV_FA_NADEL_ERW_GOLD + "=" + efw_drv_fa_nadel_erw_gold + "\n");
            fwrite(EFW_DRV_FA_NADEL_JUG_SILBER + "=" + efw_drv_fa_nadel_jug_silber + "\n");
            fwrite(EFW_DRV_FA_NADEL_JUG_GOLD + "=" + efw_drv_fa_nadel_jug_gold + "\n");
            fwrite(EFW_DRV_FA_STOFF_ERW + "=" + efw_drv_fa_stoff_erw + "\n");
            fwrite(EFW_DRV_FA_STOFF_JUG + "=" + efw_drv_fa_stoff_jug + "\n");
            fwrite(EFW_DRV_KONTO + "=" + efw_drv_konto + "\n");
            fwrite(EFW_DRV_ANSCHRIFT + "=" + efw_drv_anschrift + "\n");
            fwrite(EFW_LRVBLN_SOMM_MELD_ERW + "=" + efw_lrvbln_somm_meld_erw + "\n");
            fwrite(EFW_LRVBLN_SOMM_MELD_JUG + "=" + efw_lrvbln_somm_meld_jug + "\n");
            fwrite(EFW_LRVBLN_WINT_MELD_ERW + "=" + efw_lrvbln_wint_meld_erw + "\n");
            fwrite(EFW_LRVBLN_WINT_MELD_JUG + "=" + efw_lrvbln_wint_meld_jug + "\n");
            fwrite(EFW_LRVBLN_BLWI_MELD_ERW + "=" + efw_lrvbln_blwi_meld_erw + "\n");
            fwrite(EFW_LRVBLN_BLWI_MELD_JUG + "=" + efw_lrvbln_blwi_meld_jug + "\n");
            fwrite(EFW_LRVBLN_KONTO + "=" + efw_lrvbln_konto + "\n");

            for (int i = 0; wettDefs != null && i < wettDefs.size(); i++) {
                fwrite("\n[WETTDEF]\n");
                WettDef w = (WettDef) wettDefs.get(i);
                if (w.name != null) {
                    fwrite("NAME=" + w.name + "\n");
                }
                if (w.kurzname != null) {
                    fwrite("KURZNAME=" + w.kurzname + "\n");
                }
                if (w.key != null) {
                    fwrite("KEY=" + w.key + "\n");
                }
                if (w.gueltig_von != -1) {
                    fwrite("GUELTIG_VON=" + Integer.toString(w.gueltig_von) + "\n");
                }
                if (w.gueltig_bis != -1) {
                    fwrite("GUELTIG_BIS=" + Integer.toString(w.gueltig_bis) + "\n");
                }
                if (w.gueltig_prio != -1) {
                    fwrite("GUELTIG_PRIO=" + Integer.toString(w.gueltig_prio) + "\n");
                }
                if (w.von != null) {
                    fwrite("VON=" + w.von.tag + "," + w.von.monat + "," + w.von.jahr + "\n");
                }
                if (w.bis != null) {
                    fwrite("BIS=" + w.bis.tag + "," + w.bis.monat + "," + w.bis.jahr + "\n");
                }
                for (int j = 0; w.gruppen != null && j < w.gruppen.length; j++) {
                    WettDefGruppe g = w.gruppen[j];
                    if (g.gruppe != -1) {
                        fwrite("GRUPPEN[" + j + "][GRUPPE]=" + g.gruppe + "\n");
                    }
                    if (g.untergruppe != -1) {
                        fwrite("GRUPPEN[" + j + "][UNTERGRUPPE]=" + g.untergruppe + "\n");
                    }
                    if (g.bezeichnung.length() > 0) {
                        fwrite("GRUPPEN[" + j + "][BEZEICHNUNG]=" + g.bezeichnung + "\n");
                    }
                    if (g.geschlecht != -1) {
                        fwrite("GRUPPEN[" + j + "][GESCHLECHT]=" + g.geschlecht + "\n");
                    }
                    if (g.hoechstalter != -1) {
                        fwrite("GRUPPEN[" + j + "][HOECHSTALTER]=" + g.hoechstalter + "\n");
                    }
                    if (g.mindalter != -1) {
                        fwrite("GRUPPEN[" + j + "][MINDALTER]=" + g.mindalter + "\n");
                    }
                    if (g.km != -1) {
                        fwrite("GRUPPEN[" + j + "][KM]=" + g.km + "\n");
                    }
                    if (g.zusatz != -1) {
                        fwrite("GRUPPEN[" + j + "][ZUSATZ]=" + g.zusatz + "\n");
                    }
                    if (g.zusatz2 != -1) {
                        fwrite("GRUPPEN[" + j + "][ZUSATZ2]=" + g.zusatz2 + "\n");
                    }
                    if (g.zusatz3 != -1) {
                        fwrite("GRUPPEN[" + j + "][ZUSATZ3]=" + g.zusatz3 + "\n");
                    }
                    if (g.zusatz4 != -1) {
                        fwrite("GRUPPEN[" + j + "][ZUSATZ4]=" + g.zusatz4 + "\n");
                    }
                    if (g.behinderung != -1) {
                        fwrite("GRUPPEN[" + j + "][BEHINDERUNG]=" + g.behinderung + "\n");
                    }
                }
            }
        } catch (IOException e) {
            errWritingFile(dat);
            return false;
        }
        return true;
    }

    private void storeWettDef(WettDef wettDef, Vector gruppen) {
        if (gruppen != null) {
            WettDefGruppe[] g = new WettDefGruppe[gruppen.size()];
            for (int i = 0; i < gruppen.size(); i++) {
                g[i] = (WettDefGruppe) gruppen.get(i);
            }
            wettDef.gruppen = g;
        }
        wettDefs.add(wettDef);
    }

    public void iniDefaultsForEFW() {
        efw_letzte_aktualisierung = "unbekannt";
        efw_stand_der_daten = "unbekannt";
        efw_url_einsenden = "https://ssl.webpack.de/efa.rudern.de/efw_einsenden.pl";
        efw_url_abrufen = "https://ssl.webpack.de/efa.rudern.de/efw_status.pl";
        efw_drv_url_pubkeys = "http://efa.rudern.de/drv/pubkeys/";
        efw_drv_fa_meld_erw = 200;
        efw_drv_fa_meld_jug = 150;
        efw_drv_fa_nadel_erw_silber = 360;
        efw_drv_fa_nadel_erw_gold = 475;
        efw_drv_fa_nadel_jug_silber = 300;
        efw_drv_fa_nadel_jug_gold = 300;
        efw_drv_fa_stoff_erw = 481;
        efw_drv_fa_stoff_jug = 348;
        efw_drv_konto = "Sparkasse Hannover, Kto. 123 862, BLZ 250 501 80 (IBAN: DE06 2505 0180 0000 123862, BIC: SPKHDE2HXXX)";
        efw_drv_anschrift = "Deutscher Ruderverband, Ferdinand-Wilhelm-Fricke-Weg 10, 30169 Hannover";
        efw_lrvbln_somm_meld_erw = 850;
        efw_lrvbln_somm_meld_jug = 850;
        efw_lrvbln_wint_meld_erw = 100;
        efw_lrvbln_wint_meld_jug = 100;
        efw_lrvbln_blwi_meld_erw = 0;
        efw_lrvbln_blwi_meld_jug = 0;
        efw_lrvbln_konto = "Berliner Volksbank, Kto. 777 999 7000, BLZ 100 900 00 (IBAN: DE74 1009 0000 7779 9970 00, BIC: BEV0DEBB)";
    }

    // Dateiformat überprüfen, ggf. konvertieren
    public boolean checkFileFormat() {
        String s;
        try {
            s = freadLine();
            if (s == null || !s.trim().startsWith(kennung)) {

                // KONVERTIEREN: 150 -> 170
                if (s != null && s.trim().startsWith(KENNUNG150)) {
                    // Datei lesen
                    readEinstellungen();
                    kennung = KENNUNG170;
                    if (closeFile() && writeFile(true) && openFile()) {
                        infSuccessfullyConverted(dat, kennung);
                        s = kennung;
                    } else {
                        errConvertingFile(dat, kennung);
                    }
                }

                // KONVERTIEREN: 170 -> 190
                if (s != null && s.trim().startsWith(KENNUNG170)) {
                    // Datei lesen
                    readEinstellungen();
                    kennung = KENNUNG190;
                    if (closeFile() && writeFile(true) && openFile()) {
                        infSuccessfullyConverted(dat, kennung);
                        s = kennung;
                    } else {
                        errConvertingFile(dat, kennung);
                    }
                }

                // FERTIG MIT KONVERTIEREN
                if (s == null || !s.trim().startsWith(kennung)) {
                    errInvalidFormat(dat, EfaUtil.trimto(s, 20));
                    fclose(false);
                    return false;
                }
            }
        } catch (IOException e) {
            errReadingFile(dat, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean createNewIfDoesntExist() {
        if ((new File(dat)).exists()) {
            return true;
        }
        iniDefaultsForEFW();
        return writeFile(false);
    }

    public WettDef getWettDef(int wettnr, int wettjahr) {
        int found_nr = -1;
        int found_prio = -1;
        for (int i = 0; wettDefs != null && i < wettDefs.size(); i++) {
            WettDef w = (WettDef) wettDefs.get(i);
            if (w.wettid == wettnr
                    && w.gueltig_von <= wettjahr
                    && w.gueltig_bis >= wettjahr
                    && w.gueltig_prio > found_prio) {
                found_nr = i;
                found_prio = w.gueltig_prio;
            }
        }
        if (found_nr >= 0) {
            return (WettDef) wettDefs.get(found_nr);
        } else {
            return null;
        }
    }
   
    public String[] getAllWettDefKeys() {
        String[] wett = new String[ANZWETT];
        for (int i = 0; i < ANZWETT; i++) {
            WettDef w = getWettDef(i, 9999);
            if (w != null) {
                wett[i] = w.key; // Namen der Wettbewerbe für höchstmögliches Jahr (falls sich Namen ändern)!
            } else {
                wett[i] = "";
            }
        }
        return wett;
    }

    public String[] getAllWettDefNames() {
        String[] wett = new String[ANZWETT];
        for (int i = 0; i < ANZWETT; i++) {
            WettDef w = getWettDef(i, 9999);
            if (w != null) {
                wett[i] = w.name; // Namen der Wettbewerbe für höchstmögliches Jahr (falls sich Namen ändern)!
            } else {
                wett[i] = "*** " + International.onlyFor("Keine Wettbewerbsdefinition gefunden.", "de") + " ***";
            }
        }
        return wett;
    }

    private int getGender(String gender) {
        if (gender != null && gender.equals(EfaTypes.TYPE_GENDER_MALE)) {
            return 0;
        }
        if (gender != null && gender.equals(EfaTypes.TYPE_GENDER_FEMALE)) {
            return 1;
        }
        return 2;
    }

    private int get100Meters(long distanceInDefaultUnit) {
        return (int) (DataTypeDistance.getDistance(distanceInDefaultUnit).getValueInMeters() / 100);
    }

    public boolean inGruppe(int wettnr, int wettJahr, int gruppe, int jahrgang, String gender, boolean behind) {
        return inGruppe(wettnr, wettJahr, gruppe, jahrgang, getGender(gender), behind);
    }

    public boolean inGruppe(int wettnr, int wettJahr, int gruppe, int jahrgang, int geschlecht, boolean behind) {
        if (wettnr == LRVBERLIN_BLAUERWIMPEL || wettnr == DRV_WANDERRUDERSTATISTIK) {
            return true;
        }
        if (wettnr < 0 || wettnr >= ANZWETT) {
            return false;
        }
        WettDef wett = getWettDef(wettnr, wettJahr);
        if (wett == null || wett.gruppen == null) {
            return false;
        }
        if (jahrgang < wettJahr - wett.gruppen[gruppe].hoechstalter
                || jahrgang > wettJahr - wett.gruppen[gruppe].mindalter) {
            return false;
        }
        if (wett.gruppen[gruppe].geschlecht != 2 && geschlecht != wett.gruppen[gruppe].geschlecht) {
            return false;
        }
        if ((behind && wett.gruppen[gruppe].behinderung == BEHIND_NEIN) || (!behind && wett.gruppen[gruppe].behinderung == BEHIND_JA)) {
            return false;
        }
        return true;
    }

    public boolean erfuelltGruppe(int wettnr, int wettJahr, int gruppe, int jahrgang, String gender, boolean behind, long distance, int zusatz, int zusatz2, int zusatz3, int zusatz4) {
        return erfuelltGruppe(wettnr, wettJahr, gruppe, jahrgang, getGender(gender), behind, get100Meters(distance), zusatz, zusatz2, zusatz3, zusatz4);
    }

    public boolean erfuelltGruppe(int wettnr, int wettJahr, int gruppe, int jahrgang, int geschlecht, boolean behind, int km, int zusatz, int zusatz2, int zusatz3, int zusatz4) {
        if (wettnr == LRVBERLIN_BLAUERWIMPEL || wettnr == DRV_WANDERRUDERSTATISTIK) {
            return true;
        }
        if (jahrgang != 0 && !inGruppe(wettnr, wettJahr, gruppe, jahrgang, geschlecht, behind)) {
            return false;
        }
        WettDef wett = getWettDef(wettnr, wettJahr);
        if (wett == null || wett.gruppen == null) {
            return false;
        }
        if (km / 10 < wett.gruppen[gruppe].km) {
            return false;
        }
        switch (wettnr) {
            case DRV_FAHRTENABZEICHEN: // zusatz:= wafaKm/10        zusatz2:= wafaAnzMTour     zusatz3:= jumAnz
                int ABC3 = (wettJahr < 2015 ? 2 : 3);
                if (!((wett.gruppen[gruppe].gruppe < 3 && zusatz >= wett.gruppen[gruppe].zusatz) || // Gruppe 1/2: WafaKm erfüllt?
                        (wett.gruppen[gruppe].gruppe == 3 && (zusatz2 >= wett.gruppen[gruppe].zusatz || // Gruppe 3: Anz Wafa erfüllt?
                        wett.gruppen[gruppe].untergruppe <= ABC3 && (zusatz2 == (wett.gruppen[gruppe].zusatz - 1) && zusatz3 >= 2 || // oder bei a/b/c mit Hilfe von JuM erfüllt?
                        zusatz3 >= 4))))) {
                    return false;
                }
                break;
            case LRVBERLIN_SOMMER:     // zusatz:= <Zielfahrtenanzahl>
                if (zusatz < wett.gruppen[gruppe].zusatz) {
                    return false;
                }
                break;
            case LRVBERLIN_WINTER:     // zusatz:= winterAnz   zusatz2:= anzMonate
                if (zusatz < wett.gruppen[gruppe].zusatz || zusatz2 < wett.gruppen[gruppe].zusatz2) {
                    return false;
                }
                break;
            case LRVBRB_WANDERRUDERWETT:
                if (zusatz < wett.gruppen[gruppe].zusatz || zusatz2 < wett.gruppen[gruppe].zusatz2) {
                    return false;
                }
                break;
            case LRVBRB_FAHRTENWETT:
                if (zusatz < wett.gruppen[gruppe].zusatz || zusatz2 < wett.gruppen[gruppe].zusatz2) {
                    return false;
                }
                break;
            case LRVMVP_WANDERRUDERWETT: // zusatz:= Gig-Km; zusatz2:= Gig-Fahrten; zusatz3:= 20Km-Fahrten; zusatz4:= 30Km-Fahrten
                if (zusatz < wett.gruppen[gruppe].zusatz || zusatz2 < wett.gruppen[gruppe].zusatz2
                        || zusatz3 < wett.gruppen[gruppe].zusatz3 || zusatz4 < wett.gruppen[gruppe].zusatz4) {
                    return false;
                }
        }
        return true;
    }

    // prüft, ob der angegebene Teilnummer in einer beliebigen Gruppe erfüllt hat.
    // Rückgabe: Name der Gruppe oder null, wenn nicht erfüllt
    public String erfuellt(int wettnr, int wettJahr, int jahrgang, String gender, boolean behind, long distance, int zusatz, int zusatz2, int zusatz3, int zusatz4) {
        return erfuellt(wettnr, wettJahr, jahrgang, getGender(gender), behind, get100Meters(distance), zusatz, zusatz2, zusatz3, zusatz4);
    }

    public String erfuellt(int wettnr, int wettJahr, int jahrgang, int geschlecht, boolean behind, int km, int zusatz, int zusatz2, int zusatz3, int zusatz4) {
        if (wettnr == LRVBERLIN_BLAUERWIMPEL || wettnr == DRV_WANDERRUDERSTATISTIK) {
            return ""; // erfüllt
        }
        WettDef wett = getWettDef(wettnr, wettJahr);
        if (wett == null || wett.gruppen == null) {
            return null;
        }
        for (int i = 0; i < wett.gruppen.length; i++) {
            if (erfuelltGruppe(wettnr, wettJahr, i, jahrgang, geschlecht, behind, km, zusatz, zusatz2, zusatz3, zusatz4)) {
                return wett.gruppen[i].bezeichnung; // erfüllt
            }
        }
        return null; // nicht erfüllt
    }

    public static String getDRVAbzeichen(boolean erwachsen, int abzeichen, int abzeichenAB, int wettJahr) {
        int abzeichenGesamt = abzeichen;
        if (wettJahr < 2007) {
            abzeichenGesamt = abzeichen - abzeichenAB;
        }
        if (erwachsen) {
            // Erwachsene (Gruppe 1 oder 2)
            if ((abzeichenGesamt + 1) >= 5 && (abzeichenGesamt + 1) % 5 == 0) {
                return EfaWettMeldung.getAbzeichenGold(abzeichenGesamt + 1, true);
            }
            return EfaWettMeldung.ABZEICHEN_ERW_EINF;
        } else {
            // Jugendlicher (Gruppe 3)
            // Ab 2008 erhalten auch Jugendliche ausschließlich Fahrtenabzeichen in Gold für *Erwachsene* (s. mail vom DRV April 2008)!!
            if ((abzeichenGesamt + 1) >= 5 && (abzeichenGesamt + 1) % 5 == 0) {
                return EfaWettMeldung.getAbzeichenGold(abzeichenGesamt + 1, false); // Jugend-Gold für Jugendliche (aber zählen auch für Erwachenen-Abzeichen)
            }
            if ((abzeichen + 1) >= 5 && (abzeichen + 1) % 5 == 0) {
                return EfaWettMeldung.getAbzeichenGold(abzeichen + 1, false);
            }
            return EfaWettMeldung.ABZEICHEN_JUG_EINF;
        }
    }

    public boolean isDataOld() {
        return EfaUtil.getDateDiff(efw_stand_der_daten, EfaUtil.getCurrentTimeStampDD_MM_YYYY()) > 300;
    }
}
