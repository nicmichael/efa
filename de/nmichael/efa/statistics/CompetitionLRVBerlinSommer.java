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
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.data.efawett.Zielfahrt;
import de.nmichael.efa.data.efawett.ZielfahrtFolge;
import java.util.Vector;

public class CompetitionLRVBerlinSommer extends Competition {

    // Anzahl der Zielfahrten in einem String[][] ermitteln, bzw. wenn null aus einem String
    protected int countZf(Zielfahrt[] z, ZielfahrtFolge zf) {
        int zfAnz = 0;
        if (zf != null) {
            zfAnz = zf.getAnzZielfahrten();
        }
        if (z == null) {
            if (zf == null) {
                return 0;
            } else {
                return (zfAnz < 4 ? zfAnz : 3); // wenn z==null, dann maximal 3 zurückgeben (weil: zf könnte mehrere Fahrten an einem Tag enthalten)
            }
        }
        for (int c = 0; c < z.length; c++) {
            if (z[c] == null) {
                return c;
            }
        }
        // wenn z != null (also eigentlich erfüllt), dann den größeren der beiden Werte zurückgeben
        if (z.length < zfAnz) {
            return zfAnz;
        } else {
            return z.length;
        }
    }

    // String für Ausgabe der Zielfahrten
    protected String zfAusgabeString(String szf, int izf, boolean bzf, boolean kurz, boolean mitAnford, int geforderteFahrten) {
        if (szf.length() == 0) {
            izf = 0;
        }
        if (!bzf) {
            return null;
        }
        String s;
        if (kurz) {
            // not translated (only Berlin, GER
            if (mitAnford) {
                s = izf + "/" + geforderteFahrten + " Zf";
            } else {
                s = izf + " Zf";
            }
        } else {
            s = izf + " Zielfahrt" + (izf != 1 ? "en" : "");
        }
        return s + (izf == 0 ? "" : ": ") + szf;
    }

// @ZF@
    protected boolean zfErfuellt(Zielfahrt[] zf, int erforderlicheZielfahrten) {
        if (zf == null || zf.length < erforderlicheZielfahrten) {
            return false;
        }
        for (int i=0; i<erforderlicheZielfahrten; i++) {
            if (zf[i] == null) {
                return false;
            }
        }

        // auf zu geringe Km und doppeltes Datum prüfen
        Vector datum = new Vector();
        for (int i = 0; i < erforderlicheZielfahrten; i++) {
            if (EfaUtil.zehntelString2Int(zf[i].getKm()) < 200) {
                return false;
            }
            if (zf[i].getDatum().length() == 0 || datum.contains(zf[i].getDatum())) {
                return false;
            }
            datum.add(zf[i].getDatum());
        }

        // Zielbereiche der einzelnen Fahrten in Arrays umwandeln
        String[] zb0 = erforderlicheZielfahrten > 0 ? zf[0].getBereicheAsArray() : null;
        String[] zb1 = erforderlicheZielfahrten > 1 ? zf[1].getBereicheAsArray() : null;
        String[] zb2 = erforderlicheZielfahrten > 2 ? zf[2].getBereicheAsArray() : null;
        String[] zb3 = erforderlicheZielfahrten > 3 ? zf[3].getBereicheAsArray() : null;

        if (erforderlicheZielfahrten == 2) {
            // hack for Covid (2020)
            for (int b0 = 0; b0 < zb0.length; b0++) {
                for (int b1 = 0; b1 < zb1.length; b1++) {
                    Vector zbs = new Vector();
                    if (!zbs.contains(zb0[b0])) {
                        zbs.add(zb0[b0]);
                    }
                    if (!zbs.contains(zb1[b1])) {
                        zbs.add(zb1[b1]);
                    }
                    if (zbs.size() == erforderlicheZielfahrten) {
                        return true;
                    }
                }
            }
            return false;
        }

        if (erforderlicheZielfahrten == 3) {
            // hack for Covid (2021)
        for (int b0 = 0; b0 < zb0.length; b0++) {
                for (int b1 = 0; b1 < zb1.length; b1++) {
                    for (int b2 = 0; b2 < zb2.length; b2++) {
                        Vector zbs = new Vector();
                        if (!zbs.contains(zb0[b0])) {
                            zbs.add(zb0[b0]);
                        }
                        if (!zbs.contains(zb1[b1])) {
                            zbs.add(zb1[b1]);
                        }
                        if (!zbs.contains(zb2[b2])) {
                            zbs.add(zb2[b2]);
                        }
                        if (zbs.size() == erforderlicheZielfahrten) {
                            return true;
                        }
                    }
                }
            }
        }

        if (erforderlicheZielfahrten != 4) {
            return false; // not supported
        }

        // wurden vier unterschiedliche Zielbereiche in je einer der Fahrten erreicht?
        for (int b0 = 0; b0 < zb0.length; b0++) {
            for (int b1 = 0; b1 < zb1.length; b1++) {
                for (int b2 = 0; b2 < zb2.length; b2++) {
                    for (int b3 = 0; b3 < zb3.length; b3++) {
                        Vector zbs = new Vector();
                        if (!zbs.contains(zb0[b0])) {
                            zbs.add(zb0[b0]);
                        }
                        if (!zbs.contains(zb1[b1])) {
                            zbs.add(zb1[b1]);
                        }
                        if (!zbs.contains(zb2[b2])) {
                            zbs.add(zb2[b2]);
                        }
                        if (!zbs.contains(zb3[b3])) {
                            zbs.add(zb3[b3]);
                        }
                        if (zbs.size() == erforderlicheZielfahrten) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

// @ZF@
    protected Zielfahrt[] getBestZf(Vector zielfahrten, int erforderlicheZielfahrten) {
        int size = zielfahrten.size();
        if (size < erforderlicheZielfahrten) {
            return null;
        }

        // special case if we require less than 4 Zielfahrten (hack for Covid)
        if (erforderlicheZielfahrten == 2) {
            // Covid 2020
            Zielfahrt[] zf = new Zielfahrt[erforderlicheZielfahrten];
            for (int i=0; i<size - 1; i++) {
                for (int j=i+1; j<size; j++) {
                    zf[0] = (Zielfahrt) zielfahrten.get(i);
                    zf[1] = (Zielfahrt) zielfahrten.get(j);
                    if (zfErfuellt(zf, erforderlicheZielfahrten)) {
                        return zf;
                    }
                }
            }
            return null;
        }

        if (erforderlicheZielfahrten == 3) {
            // Covid 2021
            Zielfahrt[] zf = new Zielfahrt[erforderlicheZielfahrten];
            for (int f0 = 0; f0 < size - 2; f0++) {
                for (int f1 = f0 + 1; f1 < size - 1; f1++) {
                    for (int f2 = f1 + 1; f2 < size; f2++) {
                        zf[0] = (Zielfahrt) zielfahrten.get(f0);
                        zf[1] = (Zielfahrt) zielfahrten.get(f1);
                        zf[2] = (Zielfahrt) zielfahrten.get(f2);
                        if (zfErfuellt(zf, erforderlicheZielfahrten)) {
                            return zf;
                        }
                    }
                }
            }
            return null;
        }

        // normal case: 4 Zielfahrten
        Zielfahrt[] zf = new Zielfahrt[4];

        for (int f0 = 0; f0 < size - 3; f0++) {
            for (int f1 = f0 + 1; f1 < size - 2; f1++) {
                for (int f2 = f1 + 1; f2 < size - 1; f2++) {
                    for (int f3 = f2 + 1; f3 < size; f3++) {
                        zf[0] = (Zielfahrt) zielfahrten.get(f0);
                        zf[1] = (Zielfahrt) zielfahrten.get(f1);
                        zf[2] = (Zielfahrt) zielfahrten.get(f2);
                        zf[3] = (Zielfahrt) zielfahrten.get(f3);
                        if (zfErfuellt(zf, erforderlicheZielfahrten)) {
                            return zf;
                        }
                    }
                }
            }
        }

        return null;
    }

    protected Zielfahrt[] getAdditionalZf(Vector zielfahrten, Zielfahrt[] bestZf) {
        Vector additional = new Vector();
        for (int i = 0; i < zielfahrten.size(); i++) {
            Zielfahrt zf = (Zielfahrt) zielfahrten.get(i);
            if (zf == null) {
                continue;
            }
            if (EfaUtil.zehntelString2Int(zf.getKm()) < 200) {
                continue;
            }
            boolean doppelt = false;
            for (int j = 0; bestZf != null && j < bestZf.length; j++) {
                if (zf == bestZf[j]) {
                    doppelt = true;
                    break;
                }
            }
            if (doppelt) {
                continue;
            }
            additional.add(zf);
        }
        if (additional.size() == 0) {
            return null;
        }
        Zielfahrt[] _additional = new Zielfahrt[additional.size()];
        for (int i = 0; i < additional.size(); i++) {
            _additional[i] = (Zielfahrt) additional.get(i);
        }
        return _additional;
    }

    // Ausgabedaten für Kilometerwettbewerbe erstellen (LRV Sommer)
    // @i18n Methode wird nicht internationalisiert
    public void calculate(StatisticsRecord sr, StatisticsData[] sd) {
        WettDef wett = Daten.wettDefs.getWettDef(WettDefs.LRVBERLIN_SOMMER, sr.sCompYear);
        if (wett == null) {
            return;
        }
        sr.pTableColumns = null;
        WettDefGruppe[] gruppen = wett.gruppen;
        int maxZielfahrten = 0;
        for (int g=0; g<gruppen.length; g++) {
            maxZielfahrten = Math.max(gruppen[g].zusatz, maxZielfahrten);
        }
        int jahrgang;
        int anzInGruppe; // wievielter Ruderer in der Gruppe: die ersten 3 brauchen eine Adresse!
        long totalDistanceInDefaultUnit = 0;
        int gesanz = 0;

        // Zielfahrten für alle Ruderer aufbereiten und auswählen
        for (int i = 0; i < sd.length; i++) {
            // suche vier passende Zielfahrten
            sd[i].getAllDestinationAreas();
            sd[i].bestDestinationAreas = getBestZf(sd[i].destinationAreaVector, maxZielfahrten);
            sd[i].additionalDestinationAreas = getAdditionalZf(sd[i].destinationAreaVector, sd[i].bestDestinationAreas);
        }

        if (sr.sIsOutputCompRules) {
            sr.pCompRules = createAusgabeBedingungen(sr, wett.key, sr.pCompRulesBold, sr.pCompRulesItalics);
        }

        if (!checkWettZeitraum(sr.sCompYear, sr.sStartDate, sr.sEndDate, WettDefs.LRVBERLIN_SOMMER)) {
            sr.pCompWarning = "Achtung: Der gewählte Zeitraum entspricht nicht der Ausschreibung!";
        }

        StatisticsData lastParticipant = null;
        sr.pCompGroupNames = new String[gruppen.length][3];
        sr.pCompParticipants = new StatisticsData[gruppen.length];
        for (int g = 0; g < gruppen.length; g++) {
            sr.pCompGroupNames[g][0] = "Gruppe " + gruppen[g].bezeichnung + ")";
            sr.pCompGroupNames[g][1] = "Jahrgänge " + makeJahrgang(sr.sCompYear - gruppen[g].hoechstalter)
                    + " - " + makeJahrgang(sr.sCompYear - gruppen[g].mindalter)
                    + " (" + makeGeschlecht(gruppen[g].geschlecht) + ")";
            sr.pCompGroupNames[g][2] = gruppen[g].km + " Kilometer"
                    + (gruppen[g].zusatz > 0 ? "; mind. " + gruppen[g].zusatz + " Zielfahrten" : "");

            // Alle Teilnehmer in einer gegebenen Gruppe durchlaufen
            anzInGruppe = 0;
            for (int i = 0; i < sd.length; i++) {
                if (!sd[i].sYearOfBirth.equals("")
                        && Daten.wettDefs.inGruppe(WettDefs.LRVBERLIN_SOMMER,
                        sr.sCompYear,
                        g,
                        Integer.parseInt(sd[i].sYearOfBirth),
                        sd[i].gender,
                        sd[i].disabled)) {
                    // Teilnehmer ist in der Gruppe!
                    int countedZf = countZf(sd[i].bestDestinationAreas, sd[i].destinationAreas);
                    boolean erfuellt = Daten.wettDefs.erfuelltGruppe(WettDefs.LRVBERLIN_SOMMER,
                            sr.sCompYear,
                            g,
                            Integer.parseInt(sd[i].sYearOfBirth),
                            sd[i].gender,
                            sd[i].disabled,
                            sd[i].distance,
                            countedZf, 0, 0, 0);

                    if (erfuellt) {
                        gesanz++;
                        totalDistanceInDefaultUnit += sd[i].distance;
                    }

                    // sollen Daten für den Teilnehmer ausgegeben werden?
                    if (erfuellt
                            || ((DataTypeDistance.getDistance(sd[i].distance).getTruncatedValueInKilometers() >=
                            gruppen[g].km * sr.sCompPercentFulfilled / 100) &&
                            sr.sCompPercentFulfilled < 100)) {
                        anzInGruppe++;
                        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett) {
                            // Ausgabe für efaWett
                            if (erfuellt && sd[i].personRecord != null) {
                                EfaWettMeldung ewm = new EfaWettMeldung();
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
                                if (anzInGruppe <= 3) {
                                    ewm.anschrift = sd[i].personRecord.getAddressComplete("; ");
                                }
                                for (int j = 0; sd[i].bestDestinationAreas != null && j < sd[i].bestDestinationAreas.length; j++) {
                                    if (sd[i].bestDestinationAreas[j] != null) {
                                        for (int jj = 0; jj < 4; jj++) {
                                            switch (jj) {
                                                case 0:
                                                    ewm.fahrt[j][jj] = sd[i].bestDestinationAreas[j].getDatum();
                                                    break;
                                                case 1:
                                                    ewm.fahrt[j][jj] = sd[i].bestDestinationAreas[j].getZiel();
                                                    break;
                                                case 2:
                                                    ewm.fahrt[j][jj] = sd[i].bestDestinationAreas[j].getKm();
                                                    break;
                                                case 3:
                                                    ewm.fahrt[j][jj] = sd[i].bestDestinationAreas[j].getBereiche();
                                                    break;
                                            }
                                        }
                                    }
                                }
                                if (sd[i].bestDestinationAreas != null && sd[i].additionalDestinationAreas != null && sd[i].additionalDestinationAreas.length > 0) {
                                    int lengthBefore = sd[i].bestDestinationAreas.length;
                                    for (int j = 0; j < sd[i].additionalDestinationAreas.length; j++) {
                                        if (sd[i].additionalDestinationAreas[j] != null && j + lengthBefore < ewm.fahrt.length) {
                                            for (int jj = 0; jj < 4; jj++) {
                                                switch (jj) {
                                                    case 0:
                                                        ewm.fahrt[j + lengthBefore][jj] = sd[i].additionalDestinationAreas[j].getDatum();
                                                        break;
                                                    case 1:
                                                        ewm.fahrt[j + lengthBefore][jj] = sd[i].additionalDestinationAreas[j].getZiel();
                                                        break;
                                                    case 2:
                                                        ewm.fahrt[j + lengthBefore][jj] = sd[i].additionalDestinationAreas[j].getKm();
                                                        break;
                                                    case 3:
                                                        ewm.fahrt[j + lengthBefore][jj] = sd[i].additionalDestinationAreas[j].getBereiche();
                                                        break;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (efaWett.letzteMeldung() == null) {
                                    efaWett.meldung = ewm;
                                } else {
                                    efaWett.letzteMeldung().next = ewm;
                                }
                            }
                        } else {
                            // normale Ausgabe des Teilnehmers
                            StatisticsData participant = sd[i];
                            participant.sDistance = DataTypeDistance.getDistance(sd[i].distance).getStringValueInKilometers(false, 0, 0);
                            if (!erfuellt && sr.sIsOutputCompAdditionalWithRequirements) {
                                participant.sDistance += "/" + gruppen[g].km;
                            }
                            if (!sr.sIsOutputCompWithoutDetails && erfuellt) {
                                int _ausgabeZfAnzahl = (sd[i].bestDestinationAreas != null ? sd[i].bestDestinationAreas.length : 0)
                                        + (sd[i].additionalDestinationAreas != null && sd[i].additionalDestinationAreas.length > 0
                                        ? (sr.sIsOutputCompAllDestinationAreas ? sd[i].additionalDestinationAreas.length : 1) : 0);
                                participant.sDetailsArray = new String[_ausgabeZfAnzahl][4];
                                for (int j = 0; sd[i].bestDestinationAreas != null && j < sd[i].bestDestinationAreas.length; j++) {
                                    if (sd[i].bestDestinationAreas[j] != null) {
                                        participant.sDetailsArray[j] = sd[i].bestDestinationAreas[j].toStringArray();
                                    }
                                }
                                for (int j = 0; sr.sIsOutputCompAllDestinationAreas && sd[i].additionalDestinationAreas != null && j < sd[i].additionalDestinationAreas.length; j++) {
                                    if (sd[i].additionalDestinationAreas[j] != null) {
                                        participant.sDetailsArray[j + (sd[i].bestDestinationAreas != null ? sd[i].bestDestinationAreas.length : 0)] = sd[i].additionalDestinationAreas[j].toStringArray();
                                    }
                                }
                            } else {
                                participant.sAdditional = zfAusgabeString(sd[i].destinationAreas.toString(),
                                        countedZf,
                                        gruppen[g].zusatz > 0,
                                        sr.sIsOutputCompShort,
                                        sr.sIsOutputCompAdditionalWithRequirements,
                                        gruppen[g].zusatz);
                                if (!erfuellt && countedZf < sd[i].destinationAreas.getAnzZielfahrten()) {
                                    participant.sCompWarning = "möglicherweise mehrere Zielfahrten am selben Tag";
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
                            && Daten.wettDefs.erfuellt(WettDefs.LRVBERLIN_SOMMER, 
                            sr.sCompYear,
                            0,
                            sd[i].gender,
                            sd[i].disabled,
                            sd[i].distance,
                            countZf(sd[i].bestDestinationAreas, sd[i].destinationAreas),
                            0, 0, 0) != null
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
