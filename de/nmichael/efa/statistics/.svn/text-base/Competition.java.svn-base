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
import de.nmichael.efa.data.efawett.EfaWett;
import de.nmichael.efa.data.efawett.WettDef;
import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;

public abstract class Competition {

    protected int compId;
    protected Hashtable nichtBeruecksichtigt = new Hashtable(); // Bei Wettbewerben nicht berücksichtigte Mitglieder (z.B. weil Jahrgang fehlt oder Wettbewerbsmeldungen deaktiviert sind)
    protected EfaWett efaWett; // Zusammenstellung aller Wettbewerbsdaten für Erstellung einer Meldedatei
    protected StatisticsRecord sr;

    public static Competition getCompetition(String statType) {
        Competition comp = null;
        if (statType.equals(WettDefs.STR_DRV_FAHRTENABZEICHEN)) {
            comp = new CompetitionDRVFahrtenabzeichen();
            comp.compId = WettDefs.DRV_FAHRTENABZEICHEN;
        }
        if (statType.equals(WettDefs.STR_DRV_WANDERRUDERSTATISTIK)) {
            comp = new CompetitionDRVWanderruderstatistik();
            comp.compId = WettDefs.DRV_WANDERRUDERSTATISTIK;
        }
        if (statType.equals(WettDefs.STR_LRVBERLIN_SOMMER)) {
            comp = new CompetitionLRVBerlinSommer();
            comp.compId = WettDefs.LRVBERLIN_SOMMER;
        }
        if (statType.equals(WettDefs.STR_LRVBERLIN_WINTER)) {
            comp = new CompetitionLRVBerlinWinter();
            comp.compId = WettDefs.LRVBERLIN_WINTER;
        }
        if (statType.equals(WettDefs.STR_LRVBERLIN_BLAUERWIMPEL)) {
            comp = new CompetitionLRVBerlinBlauerWimpel();
            comp.compId = WettDefs.LRVBERLIN_BLAUERWIMPEL;
        }
        if (statType.equals(WettDefs.STR_LRVBRB_WANDERRUDERWETT)) {
            comp = new CompetitionLRVBrandenburgWanderruderwett();
            comp.compId = WettDefs.LRVBRB_WANDERRUDERWETT;
        }
        if (statType.equals(WettDefs.STR_LRVBRB_FAHRTENWETT)) {
            comp = new CompetitionLRVBrandenburgFahrtenwett();
            comp.compId = WettDefs.LRVBRB_FAHRTENWETT;
        }
        if (statType.equals(WettDefs.STR_LRVMVP_WANDERRUDERWETT)) {
            comp = new CompetitionLRVMeckPommWanderruderwett();
            comp.compId = WettDefs.LRVMVP_WANDERRUDERWETT;
        }
        return comp;
    }

    public static Competition getCompetition(StatisticsRecord sr) {
        String sType = sr.getStatisticType();
        if (sType == null) {
            return null;
        }
        Competition comp = getCompetition(sType);
        if (comp != null) {
            comp.sr = sr;
            comp.iniEfaWett(comp.compId);
        }
        return comp;
    }

    public int getCompId() {
        return compId;
    }

    private void iniEfaWett(int wettId) {
        WettDef wett = Daten.wettDefs.getWettDef(wettId, sr.sCompYear);
        efaWett = new EfaWett();
        efaWett.wettId = wettId;
        efaWett.allg_programm = Daten.PROGRAMMID;
        if (wett.von.jahr == wett.bis.jahr) {
            efaWett.allg_wettjahr = Integer.toString(sr.sCompYear + wett.von.jahr);
        } else {
            efaWett.allg_wettjahr = Integer.toString(sr.sCompYear + wett.von.jahr) + "/"
                    + Integer.toString(sr.sCompYear + wett.bis.jahr);
        }
        efaWett.allg_wett = wett.key;
        Project prj = sr.getPersistence().getProject();
        efaWett.setProjectSettings(prj);

        // correct evaluation period to competition period
        //if (sr.sOutputType == StatisticsRecord.OutputTypes.efawett) {
            sr.sStartDate.setDate(wett.von.tag, wett.von.monat, sr.sCompYear + wett.von.jahr);
            sr.sEndDate.setDate(wett.bis.tag, wett.bis.monat, sr.sCompYear + wett.bis.jahr);
        //}
    }

    public abstract void calculate(StatisticsRecord sr, StatisticsData[] sd);

  // Eine int-Zahl (Jahrgang) in einen String umwandeln
    protected String makeJahrgang(int jahr) {
        if (jahr <= 0) {
            return "????";
        } else {
            return Integer.toString(jahr);
        }
    }

    // Eine int-Zahl (Geschlecht) in einen String umwandeln
    protected String makeGeschlecht(int g) {
        switch (g) {
            case 0:
                return International.getString("m", "gender");
            case 1:
                return International.getString("w", "gender");
            default:
                return International.getString("m/w", "gender");
        }
    }

    // Prüfen, ob gewählter Zeitraum tatsächlich den Wettbewerbsbedingungen entspricht; true, falls korrekt
    protected boolean checkWettZeitraum(int wettJahr, DataTypeDate from, DataTypeDate to, int wettnr) {
        WettDef wett = Daten.wettDefs.getWettDef(wettnr, wettJahr);
        if (wett == null) {
            return false;
        }
        return (from.getDay() == wett.von.tag
                && from.getMonth() == wett.von.monat
                && to.getDay() == wett.bis.tag
                && to.getMonth() == wett.bis.monat
                && from.getYear() == wettJahr + wett.von.jahr
                && to.getYear() == wettJahr + wett.bis.jahr);
    }

    protected String[] createAusgabeBedingungen(StatisticsRecord sr, String bezeich, Hashtable fett, Hashtable kursiv) {
        if (!sr.sIsOutputCompRules) {
            return null;
        }

        Vector _zeil = new Vector(); // Zeilen

        BufferedReader f;
        String s;
        String dir = Daten.efaCfgDirectory;
        try {
            if (!new File(dir + Daten.WETTFILE).isFile() && new File(Daten.efaProgramDirectory + Daten.WETTFILE).isFile()) {
                dir = Daten.efaProgramDirectory;
            }
            f = new BufferedReader(new InputStreamReader(new FileInputStream(dir + Daten.WETTFILE), Daten.ENCODING_ISO));
            while ((s = f.readLine()) != null) {
                if (s.startsWith("[" + bezeich + "]")) {
                    while ((s = f.readLine()) != null) {
                        if (s.length() > 0 && s.charAt(0) == '[') {
                            break;
                        }
                        if (s.length() > 0 && s.charAt(0) == '*') {
                            fett.put(new Integer(_zeil.size()), "fett");
                            s = s.substring(1, s.length());
                        }
                        if (s.length() > 0 && s.charAt(0) == '#') {
                            kursiv.put(new Integer(_zeil.size()), "kursiv");
                            s = s.substring(1, s.length());
                        }
                        if (s.length() > 0) {
                            s = EfaUtil.replace(s, "%Y+", Integer.toString(sr.sCompYear + 1), true);
                        }
                        if (s.length() > 0) {
                            s = EfaUtil.replace(s, "%Y", Integer.toString(sr.sCompYear), true);
                        }
                        _zeil.add(s);
                    }
                }
            }
            f.close();
        } catch (FileNotFoundException e) {
            Dialog.error(LogString.fileNotFound(dir + Daten.WETTFILE, International.getString("Wettbewerbskonfiguration")));
        } catch (IOException e) {
            Dialog.error(LogString.fileReadFailed(dir + Daten.WETTFILE, International.getString("Wettbewerbskonfiguration")));
        }
        String[] zeilen = new String[_zeil.size()];
        _zeil.toArray(zeilen);
        return zeilen;
    }

    public static void getGigFahrten(StatisticsData sd, long minMeters) {
        sd.compData = new CompetitionData();
        for (int i = 0; sd.sessionHistory != null && i < sd.sessionHistory.size(); i++) {
            LogbookRecord r = sd.sessionHistory.get(i);
            BoatRecord b = r.getBoatRecord(r.getValidAtTimestamp());
            if (b != null) {
                int boatVariant = r.getBoatVariant();
                int vidx = -1;
                if (b.getNumberOfVariants() == 1) {
                    vidx = 0;
                } else {
                    vidx = b.getVariantIndex(boatVariant);
                }
                if (vidx >= 0) {
                    String boatType = b.getTypeType(vidx);
                    if (boatType == null || !EfaTypes.isGigBoot(boatType)) {
                        continue;
                    }
                }
                if (r.getDistance() != null && r.getDistance().getValueInMeters() >= minMeters) {
                    int meters = (int) r.getDistance().getValueInMeters();
                    sd.compData.gigfahrten.add(r);
                    sd.compData.gigbootanz++;
                    sd.compData.gigbootmeters += meters;
                    if (meters >= 20*1000) {
                        sd.compData.gigboot20plus++;
                    }
                    if (meters >= 30*1000) {
                        sd.compData.gigboot30plus++;
                    }

                }
            }
        }
    }

    public EfaWett getEfaWett() {
        return efaWett;
    }

}
