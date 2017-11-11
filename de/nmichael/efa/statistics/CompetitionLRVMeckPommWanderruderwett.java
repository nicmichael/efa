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
import de.nmichael.efa.data.efawett.WettDef;
import de.nmichael.efa.data.efawett.WettDefGruppe;
import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.types.DataTypeDistance;
import java.util.Arrays;
import java.util.Hashtable;

public class CompetitionLRVMeckPommWanderruderwett extends Competition {

    public static final int LRVMVP_NEU = 2009;

    // Ausgabedaten für Kilometerwettbewerbe erstellen
    // @i18n Methode wird nicht internationalisiert
    public void calculate(StatisticsRecord sr, StatisticsData[] sd) {
        WettDef wett = Daten.wettDefs.getWettDef(WettDefs.LRVMVP_WANDERRUDERWETT, sr.sCompYear);
        if (wett == null) {
            return;
        }
        sr.pTableColumns = null;
        WettDefGruppe[] gruppen = wett.gruppen;

        if (sr.sIsOutputCompRules) {
            sr.pCompRules = createAusgabeBedingungen(sr, wett.key, sr.pCompRulesBold, sr.pCompRulesItalics);
        }

        if (!checkWettZeitraum(sr.sCompYear, sr.sStartDate, sr.sEndDate, WettDefs.LRVMVP_WANDERRUDERWETT)) {
            sr.pCompWarning = "Achtung: Der gewählte Zeitraum entspricht nicht der Ausschreibung!";
        }
        if (sr.sCompYear < LRVMVP_NEU) {
            sr.pCompWarning = "Es haben nur Personen diesen Wettbewerb erfüllt, die NICHT das Fahrtenabzeichen des DRV erfüllen. Diese Zusatzbedingung wird von efa NICHT überprüft. Die von efa erstellte Liste enthällt alle potentiellen Erfüller.";
        }

        sr.pCompGroupNames = new String[gruppen.length][3];
        sr.pCompParticipants = new StatisticsData[gruppen.length];
        StatisticsData lastParticipant = null;
        int jahrgang;
        long totalDistanceInDefaultUnit = 0;
        int gesanz = 0;

        for (int g = 0; g < gruppen.length; g++) {
            sr.pCompGroupNames[g][0] = gruppen[g].bezeichnung + ")";
            sr.pCompGroupNames[g][1] = "Jahrgänge " + makeJahrgang(sr.sCompYear - gruppen[g].hoechstalter)
                    + " - " + makeJahrgang(sr.sCompYear - gruppen[g].mindalter)
                    + " (" + makeGeschlecht(gruppen[g].geschlecht) + ")";
            sr.pCompGroupNames[g][2] = gruppen[g].km + " Kilometer"
                    + (gruppen[g].zusatz > 0 ? "; davon " + gruppen[g].zusatz + " Gigboot-Kilometer in mind. " + gruppen[g].zusatz2 + " Fahrten" : // bis 2008
                    " in mind. " + gruppen[g].zusatz2 + " Fahrten") + // ab 2009
                    (gruppen[g].zusatz3 > 0 ? " mit " + gruppen[g].zusatz3 + " Fahrten von mind. 20 Km" : "")
                    + (gruppen[g].zusatz3 > 0 && gruppen[g].zusatz4 > 0 ? " und" : (gruppen[g].zusatz4 > 0 ? " mit" : ""))
                    + (gruppen[g].zusatz4 > 0 ? " " + gruppen[g].zusatz4 + " Fahrten von mind. 30 Km" : "");

            // Alle Teilnehmer in einer gegebenen Gruppe durchlaufen
            for (int i = 0; i < sd.length; i++) {
                if (!sd[i].sYearOfBirth.equals("")) {
                    jahrgang = Integer.parseInt(sd[i].sYearOfBirth);
                } else {
                    jahrgang = -1;
                }

                if (!sd[i].sYearOfBirth.equals("")
                        && Daten.wettDefs.inGruppe(WettDefs.LRVMVP_WANDERRUDERWETT,
                        sr.sCompYear,
                        g,
                        jahrgang,
                        sd[i].gender,
                        sd[i].disabled)) {
                    // Teilnehmer ist in der Gruppe!

                    // Wanderfahrten zusammenstellen
                    Hashtable<String,DRVFahrt> wanderfahrten = CompetitionDRVFahrtenabzeichen.getWanderfahrten(sd[i], false);
                    Object[] keys = wanderfahrten.keySet().toArray(); // Keys ermitteln

                    // Überprüfung, ob der Teilnehmer den DRV-Wettbewerb erfüllt hat
                    long totalWafaMeters = 0;
                    int wafaAnzMTour = 0;
                    int jumAnz = 0;
                    if (sr.sCompYear >= LRVMVP_NEU) { // erst ab 2009 haben LRV und DRV den gleichen Zeitraum, sonst ist dieser Abgleich nicht möglich!
                        for (int k = 0; k < keys.length; k++) {
                            DRVFahrt drvFahrt = wanderfahrten.get(keys[k]);
                            if (drvFahrt.ok && !drvFahrt.jum) {
                                totalWafaMeters += drvFahrt.distanceInMeters;
                            }
                            if (drvFahrt.jum) {
                                jumAnz++;
                            } else {
                                wafaAnzMTour++;
                            }
                        }
                    }

                    // Anzahl der geforderten Gig-Fahrten ermitteln
                    getGigFahrten(sd[i], 0);

                    // sollen für den Teilnehmer Daten ausgegeben werden?
                    boolean erfuellt = Daten.wettDefs.erfuelltGruppe(WettDefs.LRVMVP_WANDERRUDERWETT,
                            sr.sCompYear,
                            g,
                            jahrgang,
                            sd[i].gender,
                            sd[i].disabled,
                            sd[i].distance,
                            sd[i].compData.gigbootmeters / 1000,
                            sd[i].compData.gigbootanz,
                            sd[i].compData.gigboot20plus,
                            sd[i].compData.gigboot30plus);
                    if (!erfuellt && sd[i].compData.gigboot30plus > 1) {
                        erfuellt = Daten.wettDefs.erfuelltGruppe(WettDefs.LRVMVP_WANDERRUDERWETT,
                                sr.sCompYear,
                                g,
                                jahrgang,
                                sd[i].gender,
                                sd[i].disabled,
                                sd[i].distance,
                                sd[i].compData.gigbootmeters / 1000,
                                sd[i].compData.gigbootanz,
                                sd[i].compData.gigboot20plus + sd[i].compData.gigboot30plus - 1, 1);
                    }

                    if (sr.sCompYear >= LRVMVP_NEU) {
                        if (Daten.wettDefs.erfuellt(WettDefs.DRV_FAHRTENABZEICHEN,
                                sr.sCompYear, jahrgang,
                                sd[i].gender,
                                sd[i].disabled,
                                sd[i].distance,
                                (int)(totalWafaMeters / 1000),
                                wafaAnzMTour,
                                jumAnz, 0) != null) {
                            erfuellt = false;
                        }
                    }

                    if (erfuellt) {
                        gesanz++;
                        totalDistanceInDefaultUnit += sd[i].distance;
                    }

                    if (erfuellt
                            || ((DataTypeDistance.getDistance(sd[i].distance).getTruncatedValueInKilometers() >=
                            gruppen[g].km * sr.sCompPercentFulfilled / 100) &&
                            sr.sCompPercentFulfilled < 100)) {

                        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett) {
                            // Ausgabe für efaWett
                        } else {
                            // normale Ausgabe des Teilnehmers
                            StatisticsData participant = sd[i];
                            participant.sDistance = DataTypeDistance.getDistance(sd[i].distance).getStringValueInKilometers(false, 0, 0);
                            if (!erfuellt && sr.sIsOutputCompAdditionalWithRequirements) {
                                participant.sDistance += "/" + gruppen[g].km;
                            }
                            if (!sr.sIsOutputCompWithoutDetails && erfuellt) {
                                participant.sAdditional = "davon "
                                        + (sr.sCompYear < LRVMVP_NEU ?
                                            DataTypeDistance.getDistanceFromMeters(sd[i].compData.gigbootmeters).getStringValueInKilometers(false, 0, 0) +
                                            " Gigboot-Km in " + sd[i].compData.gigbootanz + " Fahrten mit " : "")
                                        + sd[i].compData.gigboot20plus + " Fahrten >= 20 Km und " + sd[i].compData.gigboot30plus + " Fahrten >= 30 Km";

                                // Gig-Fahrten ausgeben (exakt gruppen[g].zusatz2 Fahrten ausgeben)
                                // Dazu die n Fahrten mit den meisten Kilometern raussuchen
                                int[] fahrtIDs = new int[gruppen[g].zusatz2];
                                for (int fid = 0; fahrtIDs != null && fid < fahrtIDs.length; fid++) {
                                    long maxmeters = 0;
                                    int maxkmId = -1;
                                    for (int gigid = 0; gigid < sd[i].compData.gigfahrten.size(); gigid++) {
                                        LogbookRecord fahrt = sd[i].compData.gigfahrten.get(gigid);
                                        long meters = fahrt.getDistance().getValueInMeters();
                                        if (meters > maxmeters) {
                                            boolean doppelt = false;
                                            for (int ijk = 0; ijk < fid; ijk++) {
                                                if (gigid == fahrtIDs[ijk]) {
                                                    doppelt = true;
                                                }
                                            }
                                            if (!doppelt) {
                                                maxmeters = meters;
                                                maxkmId = gigid;
                                            }
                                        }
                                    }
                                    if (maxkmId < 0 || maxmeters == 0) { // kann eigentlich nie vorkommen
                                        fahrtIDs = null; // Abbruch; in diesem Fall gar keine Fahrten ausgeben
                                    } else {
                                        fahrtIDs[fid] = maxkmId;
                                    }
                                }
                                if (fahrtIDs != null) {
                                    Arrays.sort(fahrtIDs);

                                    participant.sDetailsArray = new String[fahrtIDs.length][6];
                                    for (int j = 0; j < fahrtIDs.length; j++) {
                                        LogbookRecord r = sd[i].compData.gigfahrten.get(fahrtIDs[j]);
                                        participant.sDetailsArray[j] = new String[6];
                                        participant.sDetailsArray[j][0] = r.getEntryId().toString();
                                        participant.sDetailsArray[j][1] = r.getDate().toString();
                                        participant.sDetailsArray[j][2] = r.getBoatAsName();
                                        participant.sDetailsArray[j][3] = r.getDestinationAndVariantName();
                                        participant.sDetailsArray[j][4] = r.getDistance().getStringValueInKilometers(true, 0, 1);
                                        participant.sDetailsArray[j][5] = (r.getComments() != null ? r.getComments() : "");
                                    }
                                }

                            } else {
                                if (sr.sIsOutputCompShort) {
                                    if (sr.sIsOutputCompAdditionalWithRequirements) {
                                        participant.sAdditional = (sr.sCompYear < LRVMVP_NEU
                                                ? DataTypeDistance.getDistanceFromMeters(sd[i].compData.gigbootmeters).getStringValueInKilometers(false, 0, 0) + "/" + gruppen[g].zusatz + " Gig-Km; " : "")
                                                + sd[i].compData.gigbootanz + "/" + gruppen[g].zusatz2 + " Fahrten; "
                                                + sd[i].compData.gigboot20plus + "/" + gruppen[g].zusatz3 + " >= 20 Km; "
                                                + sd[i].compData.gigboot30plus + "/" + gruppen[g].zusatz4 + " >= 30 Km";

                                    } else {
                                        participant.sAdditional = (sr.sCompYear < LRVMVP_NEU ? DataTypeDistance.getDistanceFromMeters(sd[i].compData.gigbootmeters).getStringValueInKilometers(false, 0, 0)
                                                + " Gig-Km; " : "") + sd[i].compData.gigbootanz + " Fahrten; "
                                                + sd[i].compData.gigboot20plus + " >= 20 Km; "
                                                + sd[i].compData.gigboot30plus + " >= 30 Km";
                                    }
                                } else {
                                    participant.sAdditional = "davon " + (sr.sCompYear < LRVMVP_NEU ?
                                        DataTypeDistance.getDistanceFromMeters(sd[i].compData.gigbootmeters).getStringValueInKilometers(false, 0, 0)
                                        + " Gigboot-Km in " : "") + sd[i].compData.gigbootanz
                                            + " Fahrten mit " + sd[i].compData.gigboot20plus + " Fahrten >= 20 Km und " + sd[i].compData.gigboot30plus + " Fahrten >= 30 Km";
                                }

                            }
                            participant.compFulfilled = erfuellt;

                            // Eintrag hinzufügen
                            if (sr.pCompParticipants[g] == null) {
                                sr.pCompParticipants[g] = participant;
                            } else {
                                lastParticipant.next = participant;
                            }
                            lastParticipant = participant;
                        }
                    }
                } else {
                    // Teilnehmer ohne Jahrgang
                    if (sd[i].sYearOfBirth.equals("")
                            && Daten.wettDefs.erfuellt(WettDefs.LRVMVP_WANDERRUDERWETT, sr.sCompYear, 0, sd[i].gender, sd[i].disabled, sd[i].distance, 9999, 9999, 9999, 0) != null
                            && nichtBeruecksichtigt.get(sd[i].sName) == null) {
                        nichtBeruecksichtigt.put(sd[i].sName, "Wegen fehlenden Jahrgangs ignoriert (" + DataTypeDistance.getDistance(sd[i].distance).getStringValueInKilometers(true, 0, 0) + ")");
                        continue;
                    }
                }
            }
        }
        if (sr.getOutputTypeEnum() != StatisticsRecord.OutputTypes.efawett) {
            sr.pAdditionalTable1 = new String[2][2];
            sr.pAdditionalTable1[0][0] = "Anzahl der Erfüller:";
            sr.pAdditionalTable1[0][1] = Integer.toString(gesanz);
            sr.pAdditionalTable1[1][0] = "Kilometer aller Erfüller:";
            sr.pAdditionalTable1[1][1] = DataTypeDistance.getDistance(totalDistanceInDefaultUnit).getStringValueInKilometers(true, 0, 0);
        }
    }

}
