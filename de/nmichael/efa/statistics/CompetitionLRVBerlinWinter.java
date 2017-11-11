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
import de.nmichael.efa.data.efawett.EfaWettMeldung;
import de.nmichael.efa.data.efawett.WettDef;
import de.nmichael.efa.data.efawett.WettDefGruppe;
import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.util.EfaUtil;

public class CompetitionLRVBerlinWinter extends Competition {

    // updated in getWinterFahrten() - not best-style programming, but who cares...
    int winterAnz;
    int anzMonate;
    String[][][] winterfahrten;

    static boolean mayBeWinterfahrt(LogbookRecord r) {
        if (r.getDate() == null) {
            return false;
        }
        int monthId = (r.getDate().getMonth() + 1) % 12;
        if (monthId >= 6) {
            return false;
        }
        return true;
    }

    // Winterfahrt für Auswertung der Kilometerwettbewerbe zum Array hinzufügen
    void getWinterFahrten(StatisticsData sd) {
        winterAnz = 0;
        anzMonate = 0;
        winterfahrten = new String[6][31][3];

        for (int i=0; sd.sessionHistory != null && i<sd.sessionHistory.size(); i++) {
            LogbookRecord r = sd.sessionHistory.get(i);

            // akt Monat (relative Reihenfolge) ermitteln
            int monthId = (r.getDate().getMonth() + 1) % 12;
            
            if (monthId >= winterfahrten.length) {
                return;
            }
            if (winterfahrten[monthId][0][0] == null) {
                anzMonate++; // Anzahl der gefundenen (verschiedenen) Monate
            }


            // nicht zwei Fahrten an demselben Tag erlauben!
            boolean doppelt = false;
            for (int j = 0; j < winterfahrten[monthId].length; j++) {
                if (winterfahrten[monthId][j][0] != null) {
                    if (DataTypeDate.parseDate(winterfahrten[monthId][j][0]).equals(r.getDate())) {
                        doppelt = true;
                    }
                }
            }

            if (!doppelt) {
                for (int j = 0; j < winterfahrten[monthId].length; j++) {
                    if (winterfahrten[monthId][j][0] == null) {
                        winterAnz++;
                        winterfahrten[monthId][j][0] = r.getDate().toString();
                        winterfahrten[monthId][j][1] = r.getDestinationAndVariantName();
                        winterfahrten[monthId][j][2] = r.getDistance().getStringValueInKilometers(false, 0, 1);
                        break;
                    }
                }
            }
        }
    }


    // Ausgabedaten für Kilometerwettbewerbe erstellen
    // @i18n Methode wird nicht internationalisiert
    public void calculate(StatisticsRecord sr, StatisticsData[] sd) {
        WettDef wett = Daten.wettDefs.getWettDef(WettDefs.LRVBERLIN_WINTER, sr.sCompYear);
        if (wett == null) {
            return;
        }
        sr.pTableColumns = null;
        WettDefGruppe[] gruppen = wett.gruppen;
        int jahrgang;
        long totalDistanceInDefaultUnit = 0;
        int gesanz = 0;

        if (sr.sIsOutputCompRules) {
            sr.pCompRules = createAusgabeBedingungen(sr, wett.key, sr.pCompRulesBold, sr.pCompRulesItalics);
        }

        if (!checkWettZeitraum(sr.sCompYear, sr.sStartDate, sr.sEndDate, WettDefs.LRVBERLIN_WINTER)) {
            sr.pCompWarning = "Achtung: Der gewählte Zeitraum entspricht nicht der Ausschreibung!";
        }

        sr.pCompGroupNames = new String[gruppen.length][3];
        sr.pCompParticipants = new StatisticsData[gruppen.length];
        StatisticsData lastParticipant = null;

        for (int g = 0; g < gruppen.length; g++) {
            sr.pCompGroupNames[g][0] = "Gruppe " + gruppen[g].bezeichnung + ")";
            sr.pCompGroupNames[g][1] = "Jahrgänge " + makeJahrgang(sr.sCompYear - gruppen[g].hoechstalter)
                    + " - " + makeJahrgang(sr.sCompYear - gruppen[g].mindalter);
            sr.pCompGroupNames[g][2] = gruppen[g].km + " Kilometer"
                    + (gruppen[g].zusatz > 0 ? "; mind. " + gruppen[g].zusatz + " Fahrten" : "")
                    + (gruppen[g].zusatz2 > 0 ? " in " + gruppen[g].zusatz2 + " Monaten" : "");

            // Alle Teilnehmer in einer gegebenen Gruppe durchlaufen
            for (int i = 0; i < sd.length; i++) {
                if (!sd[i].sYearOfBirth.equals("")
                        && Daten.wettDefs.inGruppe(WettDefs.LRVBERLIN_WINTER,
                        sr.sCompYear,
                        g,
                        Integer.parseInt(sd[i].sYearOfBirth),
                        sd[i].gender,
                        sd[i].disabled)) {

                    // Teilnehmer ist in der Gruppe!

                    // Winterfahrten ermitteln
                    getWinterFahrten(sd[i]);

                    boolean erfuellt = Daten.wettDefs.erfuelltGruppe(WettDefs.LRVBERLIN_WINTER,
                            sr.sCompYear,
                            g,
                            Integer.parseInt(sd[i].sYearOfBirth),
                            sd[i].gender,
                            sd[i].disabled,
                            sd[i].distance,
                            winterAnz,
                            anzMonate, 0, 0);

                    if (erfuellt) {
                        gesanz++;
                        totalDistanceInDefaultUnit += sd[i].distance;
                    }

                    // sollen Daten für den Teilnehmer ausgegeben werden?
                    if (erfuellt
                            || ((DataTypeDistance.getDistance(sd[i].distance).getTruncatedValueInKilometers() >=
                            gruppen[g].km * sr.sCompPercentFulfilled / 100) &&
                            sr.sCompPercentFulfilled < 100)) {

                        EfaWettMeldung ewm = null;


                        // bereits geruderte Monate ermitteln (für Ausgabe, wenn nicht erfüllt)
                        String monate = "";
                        for (int m = 0; m < winterfahrten.length; m++) {
                            if (winterfahrten[m][0][0] != null) {
                                switch (m) {
                                    case 0:
                                        monate = monate + (monate.equals("") ? "" : ", ") + (sr.sIsOutputCompShort ? "Nov" : "November");
                                        break;
                                    case 1:
                                        monate = monate + (monate.equals("") ? "" : ", ") + (sr.sIsOutputCompShort ? "Dez" : "Dezember");
                                        break;
                                    case 2:
                                        monate = monate + (monate.equals("") ? "" : ", ") + (sr.sIsOutputCompShort ? "Jan" : "Januar");
                                        break;
                                    case 3:
                                        monate = monate + (monate.equals("") ? "" : ", ") + (sr.sIsOutputCompShort ? "Feb" : "Februar");
                                        break;
                                    case 4:
                                        monate = monate + (monate.equals("") ? "" : ", ") + (sr.sIsOutputCompShort ? "Mar" : "März");
                                        break;
                                    case 5:
                                        monate = monate + (monate.equals("") ? "" : ", ") + (sr.sIsOutputCompShort ? "Apr" : "April");
                                        break;
                                }
                            }
                        }

                        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett) {
                            // Ausgabe für efaWett
                            if (erfuellt && sd[i].personRecord != null) {
                                ewm = new EfaWettMeldung();
                                ewm.personID = sd[i].personRecord.getId();
                                ewm.nachname = sd[i].personRecord.getLastName();
                                ewm.vorname = sd[i].personRecord.getFirstName();
                                ewm.jahrgang = sd[i].sYearOfBirth;
                                ewm.gruppe = gruppen[g].bezeichnung;
                                if (sd[i].gender.equals(EfaTypes.TYPE_GENDER_MALE)) {
                                    ewm.geschlecht = EfaWettMeldung.GESCHLECHT_M;
                                } else if (sd[i].gender.equals(EfaTypes.TYPE_GENDER_FEMALE)) {
                                    ewm.geschlecht = EfaWettMeldung.GESCHLECHT_W;
                                } else {
                                    ewm.geschlecht = "X";
                                }
                                ewm.kilometer = DataTypeDistance.getDistance(sd[i].distance).getStringValueInKilometers(false, 0, 0);
                            }
                        }

                        // normale Ausgabe des Teilnehmers
                        StatisticsData participant = sd[i];
                        participant.sDistance = DataTypeDistance.getDistance(sd[i].distance).getStringValueInKilometers(false, 0, 0);
                        if (!erfuellt && sr.sIsOutputCompAdditionalWithRequirements) {
                            participant.sDistance += "/" + gruppen[g].km;
                        }
                        if (!sr.sIsOutputCompWithoutDetails && erfuellt) {
                            participant.sDetailsArray = new String[gruppen[g].zusatz
                                    + (winterAnz > gruppen[g].zusatz ? 1 : 0)][3]; // eine Fahrt mehr für den Hinweis "weitere Fahrten"
                        } else {
                            // Warnung, wenn Fahrten nicht gewertet wurden
                            boolean warnung = !erfuellt && sd[i].count > winterAnz;

                            if (sr.sIsOutputCompShort) {
                                if (sr.sIsOutputCompAdditionalWithRequirements) {
                                    participant.sAdditional = sd[i].count
                                            + (warnung ? " (" + winterAnz + ")" : "")
                                            + "/" + gruppen[g].zusatz + " F in "
                                            + anzMonate + "/" + gruppen[g].zusatz2 + " M";
                                } else {
                                    participant.sAdditional = sd[i].count
                                            + (warnung ? " (" + winterAnz + ")" : "")
                                            + " F in " + anzMonate + " M";
                                }
                            } else {
                                participant.sAdditional = sd[i].count + " Fahrten in " + anzMonate + " Monaten";
                            }
                            if (!erfuellt) {
                                participant.sAdditional += (monate.equals("") ? "" : " (" + monate + ")");
                            }

                            if (!sr.sIsOutputCompShort && warnung) {
                                participant.sAdditional += " (davon nur " + winterAnz + " wertbare Fahrten, da mehrere Fahrten am selben Tag)";
                            }
                        }
                        participant.compFulfilled = erfuellt;

                        // Berechnung der Winterfahrten (beide Ausgabemodi)
                        if (erfuellt && !sr.sIsOutputCompWithoutDetails) {
                            int c = 0;
                            int teilkm = 0; // Km in den gruppen[g][WettDefs.G_ZUSATZ] (8) Fahrten
                            int gefm = 0;  // Anzahl der bereits gefundenen Monate, in denen Fahrten vorlegen
                            for (int m = 0; m < winterfahrten.length; m++) {
                                for (int j = 0; j < winterfahrten[m].length; j++) {
                                    if (j == 0 && winterfahrten[m][j][0] != null) {
                                        gefm++;
                                    }
                                    if (c < gruppen[g].zusatz && gruppen[g].zusatz - c > anzMonate - gefm && winterfahrten[m][j][0] != null) {
                                        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett) {
                                            ewm.fahrt[c] = winterfahrten[m][j];
                                        } else {
                                            participant.sDetailsArray[c] = winterfahrten[m][j];
                                        }
                                        c++;
                                        teilkm += EfaUtil.zehntelString2Int(winterfahrten[m][j][2]);
                                    }
                                }
                            }
                        }

                        // Eintrag hinzufügen

                        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett) {
                            if (efaWett.letzteMeldung() == null) {
                                efaWett.meldung = ewm;
                            } else {
                                efaWett.letzteMeldung().next = ewm;
                            }
                        } else {
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
                            && Daten.wettDefs.erfuellt(WettDefs.LRVBERLIN_WINTER, sr.sCompYear, 0, sd[i].gender, sd[i].disabled, sd[i].distance, 9999, 9999, 0, 0) != null
                            && nichtBeruecksichtigt.get(sd[i].sName) == null) {
                        nichtBeruecksichtigt.put(sd[i].sName,
                                "Wegen fehlenden Jahrgangs ignoriert (" +
                                DataTypeDistance.getDistance(sd[i].distance).getStringValueInKilometers(true, 0, 0) + ")");
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
