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
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.util.EfaUtil;
import java.util.Hashtable;

public class CompetitionLRVBrandenburgWanderruderwett extends Competition {

    // Ausgabedaten für Kilometerwettbewerbe erstellen
    // @i18n Methode wird nicht internationalisiert
    public void calculate(StatisticsRecord sr, StatisticsData[] sd) {
        WettDef wett = Daten.wettDefs.getWettDef(WettDefs.LRVBRB_WANDERRUDERWETT, sr.sCompYear);
        if (wett == null) {
            return;
        }
        sr.pTableColumns = null;
        WettDefGruppe[] gruppen = wett.gruppen;

        if (sr.sIsOutputCompRules) {
            sr.pCompRules = createAusgabeBedingungen(sr, wett.key, sr.pCompRulesBold, sr.pCompRulesItalics);
        }

        if (!checkWettZeitraum(sr.sCompYear, sr.sStartDate, sr.sEndDate, WettDefs.LRVBRB_WANDERRUDERWETT)) {
            sr.pCompWarning = "Achtung: Der gewählte Zeitraum entspricht nicht der Ausschreibung!";
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
                    + "; davon " + gruppen[g].zusatz + " Gigboot-Kilometer und " + gruppen[g].zusatz2 + " Wanderfahrt-Tage";

            // Alle Teilnehmer in einer gegebenen Gruppe durchlaufen
            for (int i = 0; i < sd.length; i++) {
                if (!sd[i].sYearOfBirth.equals("")) {
                    jahrgang = Integer.parseInt(sd[i].sYearOfBirth);
                } else {
                    jahrgang = -1;
                }

                if (!sd[i].sYearOfBirth.equals("")
                        && Daten.wettDefs.inGruppe(WettDefs.LRVBRB_WANDERRUDERWETT,
                        sr.sCompYear,
                        g,
                        jahrgang, sd[i].gender,
                        sd[i].disabled)) {
                    // Teilnehmer ist in der Gruppe!

                    // Wanderfahrten zusammenstellen
                    Hashtable<String,DRVFahrt> wanderfahrten = CompetitionDRVFahrtenabzeichen.getWanderfahrten(sd[i], false);
                    int anzWafaTage = 0;
                    int anzWafa = 0; // max. 3 ausgeben
                    Object[] keys = wanderfahrten.keySet().toArray(); // Keys ermitteln
                    String[][] wafa = new String[gruppen[g].zusatz2][6]; // 3 Einträge mit jeweils LfdNr/Abfahrt/Ankunft/Ziel/Km/Bemerk
                    int fahrtnr = 0;
                    for (int nr = 0; nr < wafa.length; nr++) { // max. für 3 auszufüllende Felder Fahrten suchen
                        DRVFahrt drvel = null;
                        if (fahrtnr < keys.length) {
                            do {
                                drvel = wanderfahrten.get(keys[fahrtnr]);
                                // wenn ein Ruderer an einer Mehrtagesfahrt (als einzelne Etappen eingetragen) nur einen Tag
                                // mitgerudert ist, werden nur 30 Km gefordert (Dennis, 02.05.03)
                                if (drvel != null && drvel.jum == false && 
                                        drvel.distanceInMeters/1000 >= CompetitionDRVFahrtenabzeichen.WAFA_MINDISTANCE_ONEDAY &&
                                        drvel.days == 1) {
                                    drvel.ok = true;
                                }
                                fahrtnr++;
                            } while ((drvel == null || !drvel.ok) && fahrtnr < keys.length);
                        }
                        if (drvel != null && drvel.ok) {
                            wafa[nr][0] = drvel.entryNo;
                            wafa[nr][1] = drvel.dateStart.toString();
                            wafa[nr][2] = (drvel.dateEnd != null ? drvel.dateEnd.toString() : drvel.dateStart.toString());
                            wafa[nr][3] = drvel.destination;
                            wafa[nr][4] = DataTypeDistance.getDistanceFromMeters(drvel.distanceInMeters).getStringValueInKilometers(false, 0, 1);
                            wafa[nr][5] = drvel.comments;
                            anzWafaTage += drvel.days;
                            anzWafa++;
                        }
                    }
                    for (int sx = 0; sx < wafa.length; sx++) {
                        for (int sy = sx + 1; sy < wafa.length; sy++) {
                            if (wafa[sx][0] != null && wafa[sy][0] != null
                                    && EfaUtil.compareIntString(wafa[sy][0], wafa[sx][0]) < 0) {
                                String[] tmp = wafa[sx];
                                wafa[sx] = wafa[sy];
                                wafa[sy] = tmp;
                            }
                        }
                    }

                    // Anzahl der geforderten Gig-Fahrten ermitteln
                    getGigFahrten(sd[i], 0);

                    // sollen für den Teilnehmer Daten ausgegeben werden?
                    boolean erfuellt = Daten.wettDefs.erfuelltGruppe(WettDefs.LRVBRB_WANDERRUDERWETT,
                            sr.sCompYear,
                            g,
                            jahrgang,
                            sd[i].gender,
                            sd[i].disabled,
                            sd[i].distance,
                            sd[i].compData.gigbootmeters / 1000,
                            anzWafaTage, 0, 0);

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
                                participant.sAdditional = "davon " + DataTypeDistance.getDistanceFromMeters(sd[i].compData.gigbootmeters).getStringValueInKilometers() + " Gigboot-Km";
                                participant.sDetailsArray = new String[anzWafa][6];
                                for (int j = 0; j < wafa.length && j < anzWafa; j++) {
                                    participant.sDetailsArray[j] = wafa[j];
                                }
                            } else {
                                if (sr.sIsOutputCompShort) {
                                    if (sr.sIsOutputCompAdditionalWithRequirements) {
                                        participant.sAdditional = DataTypeDistance.getDistanceFromMeters(sd[i].compData.gigbootmeters).getStringValueInKilometers() + "/" + gruppen[g].zusatz + " Gig-Km; "
                                                + anzWafaTage + "/" + gruppen[g].zusatz2 + " WafaTage";
                                    } else {
                                        participant.sAdditional = DataTypeDistance.getDistanceFromMeters(sd[i].compData.gigbootmeters).getStringValueInKilometers() + " Gig-Km; " + anzWafaTage + " WafaTage";
                                    }
                                } else {
                                    participant.sAdditional = "davon " + DataTypeDistance.getDistanceFromMeters(sd[i].compData.gigbootmeters).getStringValueInKilometers() + " Gigboot-Km und " + anzWafaTage + " Wanderfahrt-Tage";
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
                    // Teilnehmer ohne Jahrgang
                    if (sd[i].sYearOfBirth.equals("")
                            && Daten.wettDefs.erfuellt(WettDefs.LRVBRB_WANDERRUDERWETT, sr.sCompYear, 0, sd[i].gender, sd[i].disabled, sd[i].distance, 9999, 9999, 9999, 0) != null
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
