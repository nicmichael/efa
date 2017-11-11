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
import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import java.util.ArrayList;

public class CompetitionLRVBerlinBlauerWimpel extends Competition {

    // Ausgabedaten für Kilometerwettbewerbe erstellen
    // @i18n Methode wird nicht internationalisiert
    public void calculate(StatisticsRecord sr, StatisticsData[] sd) {
        WettDef wett = Daten.wettDefs.getWettDef(WettDefs.LRVBERLIN_BLAUERWIMPEL, sr.sCompYear);
        if (wett == null) {
            return;
        }
        sr.pTableColumns = null;
        int numberOfMembers = -1;
        if (sr.sCompYear < 2015) {
            numberOfMembers = Daten.project.getPersons(false).getNumberOfMembers(sr.sTimestampBegin, false);
        } else {
            numberOfMembers = 0;
            for (int i = 0; i < sd.length; i++) {
                if (sd[i].distance >= 1000) {
                    numberOfMembers++;
                }
            }
        }

        if (sr.sCompYear < 2015) {
            if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett) {
                String s = Dialog.inputDialog("Anzahl der Mitglieder am 1.1. des Jahres",
                        "Wie viele Mitglieder hatte der Verein am " + EfaUtil.getTimeStamp(sr.sTimestampBegin) + "?\n"
                        + "efa hat anhand der Personenliste " + numberOfMembers + " Mitglieder ermittelt.",
                        Integer.toString(numberOfMembers));
                if (s != null) {
                    numberOfMembers = EfaUtil.string2int(s, numberOfMembers);
                }
            }
        }
        int anzWertung = Integer.MAX_VALUE;
        if (sr.sCompYear < 2015) {
            anzWertung = 20 + (int) (0.1 * numberOfMembers); // Anzahl der zu wertenden Mitglieder
            efaWett.wimpel_anzMitglieder = numberOfMembers;
        } else {
            anzWertung = 10 + (int) (0.2 * numberOfMembers); // Anzahl der zu wertenden Mitglieder
            efaWett.wimpel_anzMitglieder = numberOfMembers;
        }

        if (sr.sIsOutputCompRules) {
            sr.pCompRules = createAusgabeBedingungen(sr, wett.key, sr.pCompRulesBold, sr.pCompRulesItalics);
        }

        if (!checkWettZeitraum(sr.sCompYear, sr.sStartDate, sr.sEndDate, WettDefs.LRVBERLIN_BLAUERWIMPEL)) {
            sr.pCompWarning = "Achtung: Der gewählte Zeitraum entspricht nicht der Ausschreibung!";
        }

        long totalDistanceInDefaultUnit = 0;
        
        ArrayList<String[]> pAdditionalTable = new ArrayList<String[]>();

        if (!sr.sIsOutputCompWithoutDetails) {
            sr.pAdditionalTable1Title = new String[3];
            sr.pAdditionalTable1Title[0] = "Nummer";
            sr.pAdditionalTable1Title[1] = "Name";
            sr.pAdditionalTable1Title[2] = "Kilometer";
        }

        sr.pCompParticipants = new StatisticsData[0];
        for (int i = 0; i < sd.length && i < anzWertung; i++) {
            if (!sr.sIsOutputCompWithoutDetails) {
                if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett
                        && sd[i].personRecord != null) {
                    EfaWettMeldung ewm = new EfaWettMeldung();
                    ewm.personID = sd[i].personRecord.getId();
                    ewm.nachname = sd[i].personRecord.getLastName();
                    ewm.vorname = sd[i].personRecord.getFirstName();
                    ewm.kilometer = Long.toString(DataTypeDistance.getDistance(sd[i].distance).getRoundedValueInKilometers());
                    if (efaWett.letzteMeldung() == null) {
                        efaWett.meldung = ewm;
                    } else {
                        efaWett.letzteMeldung().next = ewm;
                    }
                } else {
                    String[] item = new String[3];
                    item[0] = Integer.toString(i+1);
                    item[1] = sd[i].sName;
                    item[2] = DataTypeDistance.getDistance(sd[i].distance).getStringValueInKilometers(false, 0, 0);
                    pAdditionalTable.add(item);
                }

            }
            totalDistanceInDefaultUnit += sd[i].distance;
        }

        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett) {
            // setzen der Gesamtwerte in der efaWett-Datei
            // wird beim OK-Klicken in EfaWettSelectAndCompleteFrame gesetzt
        } else {
            sr.pAdditionalTable2 = new String[3][2];
            sr.pAdditionalTable2[0][0] = "Anzahl der ausgewerteten Ruderer:";
            sr.pAdditionalTable2[0][1] = pAdditionalTable.size() + (numberOfMembers > 0 ? " (von " + numberOfMembers + (sr.sCompYear < 2015 ? "" : "aktiven ") + " Mitgliedern)" : "");
            sr.pAdditionalTable2[1][0] = "Gesamtkilometer" + (numberOfMembers > 0 ? " der ersten " + anzWertung + " Ruderer:" : ":");
            sr.pAdditionalTable2[1][1] = DataTypeDistance.getDistance(totalDistanceInDefaultUnit).getStringValueInKilometers(true, 0, 0);
            sr.pAdditionalTable2[2][0] = "Durchschnittskilometer pro Ruderer:";
            sr.pAdditionalTable2[2][1] = DataTypeDistance.getDistance(totalDistanceInDefaultUnit/anzWertung).getStringValueInKilometers(true, 0, 1);
        }
        
        sr.pAdditionalTable1 = pAdditionalTable.toArray(new String[0][]);
    }
}
