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
import de.nmichael.efa.data.Fahrtenabzeichen;
import de.nmichael.efa.data.FahrtenabzeichenRecord;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.SessionGroupRecord;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.data.efawett.DRVSignatur;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;

public class CompetitionDRVFahrtenabzeichen extends Competition {

    public static final long WAFA_MINDISTANCE_ONEDAY   = 30 * 1000; // 30 km
    public static final long WAFA_MINDISTANCE_MULTIDAY = 40 * 1000; // 40 km
    
    private static final int MAX_WAFA_OUTPUT = 10;


    static long getNumberOfDays(DataTypeDate startDate, DataTypeDate endDate) {
        long days = 1;
        if (startDate != null && startDate.isSet() &&
            endDate != null && endDate.isSet()) {
            days = endDate.getDifferenceDays(startDate) + 1;
        }
        return days;
    }

    static boolean mayBeWafa(LogbookRecord r) {
        String sessionType = r.getSessionType();
        long distanceInMeters = (r.getDistance() == null ? 0 : r.getDistance().getValueInMeters());
        if (sessionType == null) {
            return false;
        }
        if (r.getDate() == null) {
            return false;
        }
        if (distanceInMeters == 0) {
            return false;
        }
        if (sessionType.equals(EfaTypes.TYPE_SESSION_REGATTA) ||
            sessionType.equals(EfaTypes.TYPE_SESSION_JUMREGATTA) ||
            sessionType.equals(EfaTypes.TYPE_SESSION_TRAININGCAMP) ||
            sessionType.equals(EfaTypes.TYPE_SESSION_LATEENTRY) ||
            sessionType.equals(EfaTypes.TYPE_SESSION_MOTORBOAT) ||
            sessionType.equals(EfaTypes.TYPE_SESSION_ERG)
                ) {
            return false; // diese Fahrten zählen nicht
        }
        if (getNumberOfDays(r.getDate(), r.getEndDate()) > 1 || r.getSessionGroupId() != null) {
            // potentielle Mehrtagesfahrt
            // da wir die Kilometer nicht kennen und noch andere Etappen hinzukommen könnten,
            // merken wir uns diesen Eintrag
            return true;
        } else {
            // definitiv eine eintägige Fahrt
            // diese Fahrt muß die Mindestentfernung erfüllen
            if (distanceInMeters >= WAFA_MINDISTANCE_ONEDAY) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean maybeJuMRegatta(LogbookRecord r) {
        String sessionType = r.getSessionType();
        if (sessionType == null) {
            return false;
        }
        return sessionType.equals(EfaTypes.TYPE_SESSION_JUMREGATTA) ||
               sessionType.equals(EfaTypes.TYPE_SESSION_REGATTA);
    }

    public static Hashtable<String, DRVFahrt> getWanderfahrten(StatisticsData sd, boolean gruppe3abc) {
        Hashtable<String, DRVFahrt> wanderfahrten = new Hashtable<String, DRVFahrt>();

        for (int j = 0; sd.sessionHistory != null && j < sd.sessionHistory.size(); j++) {
            LogbookRecord r = sd.sessionHistory.get(j);
            boolean jum = r.getSessionType().equals(EfaTypes.TYPE_SESSION_JUMREGATTA);
            if (!gruppe3abc && r.getSessionType().equals(EfaTypes.TYPE_SESSION_REGATTA)) {
                continue;
            }
            if (gruppe3abc && r.getSessionType().equals(EfaTypes.TYPE_SESSION_REGATTA)) {
                jum = true;
            }
            if (jum && !gruppe3abc) {
                continue;
            }
            SessionGroupRecord sessionGroup = r.getSessionGroup();
            String key = (sessionGroup == null
                    ? "##SE##" + r.getEntryId().toString() + "##" + r.getDate().toString()
                    : "##SG##" + sessionGroup.getLogbook() + "##" + sessionGroup.getName());
            DRVFahrt fahrt = wanderfahrten.get(key);
            if (fahrt == null) {
                fahrt = new DRVFahrt(r.getEntryId().toString(), r.getDate(), r.getEndDate(),
                        r.getDestinationAndVariantName(), r.getComments(), r.getDistance().getValueInMeters());
                fahrt.jum = jum;
            } else {
                if (r.getDate().isBefore(fahrt.dateStart)) {
                    fahrt.dateStart = r.getDate();
                }
                if (fahrt.dateEnd == null || r.getDate().isAfter(fahrt.dateEnd)) {
                    fahrt.dateEnd = r.getDate();
                }
                if (r.getEndDate() != null && (fahrt.dateEnd == null || r.getEndDate().isAfter(fahrt.dateEnd))) {
                    fahrt.dateEnd = r.getEndDate();
                }
                fahrt.distanceInMeters += r.getDistance().getValueInMeters();
                if (!jum) {
                    fahrt.jum = false;
                }
            }
            if (sessionGroup != null) {
                if (sessionGroup.getName() != null) {
                    fahrt.destination = sessionGroup.getName()
                            + (sessionGroup.getRoute() != null ? ": " + sessionGroup.getRoute() : "");
                }
                fahrt.days = getNumberOfDays(fahrt.dateStart, fahrt.dateEnd);
                if (sessionGroup.getActiveDays() < fahrt.days) {
                    fahrt.days = sessionGroup.getActiveDays();
                }
            }
            if (fahrt.jum) {
                fahrt.ok = true;
            } else {
                if (fahrt.days == 1 && fahrt.distanceInMeters >= WAFA_MINDISTANCE_ONEDAY) {
                    fahrt.ok = true;
                }
                if (fahrt.days > 1 && fahrt.distanceInMeters >= WAFA_MINDISTANCE_MULTIDAY) {
                    fahrt.ok = true;
                }
            }
            wanderfahrten.put(key, fahrt);
        }
        return wanderfahrten;
    }

    public static long getWanderfahrtenMeter(StatisticsData sd) {
        Hashtable<String, DRVFahrt> wanderfahrten = getWanderfahrten(sd, false);
        Object[] keys = wanderfahrten.keySet().toArray(); // Keys ermitteln
        long totalWafaMeters = 0; // Wafa-Meter insgesamt
        for (int k = 0; k < keys.length; k++) {
            DRVFahrt drvFahrt = wanderfahrten.get(keys[k]);
            if (drvFahrt.ok && !drvFahrt.jum) {
                totalWafaMeters += drvFahrt.distanceInMeters;
            }
        }
        return totalWafaMeters;
    }

    // Ausgabedaten für Kilometerwettbewerbe erstellen
    // @i18n Methode wird nicht internationalisiert
    public void calculate(StatisticsRecord sr, StatisticsData[] sd) {
        WettDef wett = Daten.wettDefs.getWettDef(WettDefs.DRV_FAHRTENABZEICHEN, sr.sCompYear);
        if (wett == null) {
            return;
        }
        int ABC3 = (sr.sCompYear < 2015 ? 
                            2 : // bis 2014: 3ab
                            3); // ab 2015: 3abc
        sr.pTableColumns = null;
        WettDefGruppe[] gruppen = wett.gruppen;
        int jahrgang;
        Vector ungueltigeFahrtenhefte = new Vector();
        Vector zumErstenMalGemeldet = new Vector();
        int letzteElektronischeMeldung = 0;
        int anzahlGemeldeteTeilnehmer = 0;
        long totalDistanceInDefaultUnit = 0;
        int gesanz = 0;

        if (sr.sIsOutputCompRules) {
            sr.pCompRules = createAusgabeBedingungen(sr, wett.key, sr.pCompRulesBold, sr.pCompRulesItalics);
        }

        if (!checkWettZeitraum(sr.sCompYear, sr.sStartDate, sr.sEndDate, WettDefs.DRV_FAHRTENABZEICHEN)) {
            sr.pCompWarning = "Achtung: Der gewählte Zeitraum entspricht nicht der Ausschreibung!";
        }

        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett) {
            Project p = sr.getPersistence().getProject();
            efaWett.verein_mitglnr = p.getClubGlobalAssociationMemberNo();
            // @todo (P5) Bankverbindung für Meldung efaWett
            //efaWett.meld_kto = Daten.vereinsConfig.meldenderKto;
            //efaWett.meld_bank = Daten.vereinsConfig.meldenderBank;
            //efaWett.meld_blz = Daten.vereinsConfig.meldenderBLZ;
        }

        sr.pCompGroupNames = new String[gruppen.length][3];
        sr.pCompParticipants = new StatisticsData[gruppen.length];

        StatisticsData lastParticipant = null;
        for (int g = 0; g < gruppen.length; g++) {
            if (gruppen[g].gruppe == 1) {
                sr.pCompGroupNames[g][0] = "Männer " + gruppen[g].bezeichnung + ")";
            } else if (gruppen[g].gruppe == 2) {
                sr.pCompGroupNames[g][0] = "Frauen " + gruppen[g].bezeichnung + ")";
            } else {
                sr.pCompGroupNames[g][0] = "Jugend " + gruppen[g].bezeichnung + ")";
            }
            sr.pCompGroupNames[g][1] = "Jahrgänge " + makeJahrgang(sr.sCompYear - gruppen[g].hoechstalter)
                    + " - " + makeJahrgang(sr.sCompYear - gruppen[g].mindalter)
                    + " (" + makeGeschlecht(gruppen[g].geschlecht) + ")";
            sr.pCompGroupNames[g][2] = gruppen[g].km + " Kilometer"
                    + (gruppen[g].zusatz > 0 ? "; davon " + gruppen[g].zusatz
                    + (gruppen[g].gruppe == 3 ? " Tage Wanderfahrten" : " Km Wanderfahrten") : "");

            // Alle Teilnehmer in einer gegebenen Gruppe durchlaufen
            for (int i = 0; i < sd.length; i++) {
                if (!sd[i].sYearOfBirth.equals("")) {
                    jahrgang = Integer.parseInt(sd[i].sYearOfBirth);
                } else {
                    jahrgang = -1;
                }

                if (!sd[i].sYearOfBirth.equals("")
                        && Daten.wettDefs.inGruppe(WettDefs.DRV_FAHRTENABZEICHEN, sr.sCompYear, g, jahrgang, sd[i].gender, sd[i].disabled)) {
                    // Teilnehmer ist in der Gruppe!
                    
                    // is Gruppe 3 a/b/c
                    boolean gruppe3abc = gruppen[g].gruppe == 3 && gruppen[g].untergruppe <= ABC3;

                    // Wanderfahrten zusammenstellen
                    Hashtable<String,DRVFahrt> wanderfahrten = getWanderfahrten(sd[i], gruppe3abc);

                    boolean mehrFahrten = false;
                    String[][] wafa = new String[MAX_WAFA_OUTPUT][6]; // MAX_WAFA_OUTPUT Einträge mit jeweils LfdNr/Abfahrt/Ankunft/Ziel/Km/Bemerk
                    Object[] keys = wanderfahrten.keySet().toArray(); // Keys ermitteln
                    long totalWafaMeters = 0; // Wafa-Meter insgesamt
                    for (int k=0; k<keys.length; k++) {
                        DRVFahrt drvFahrt = wanderfahrten.get(keys[k]);
                        if (drvFahrt.ok && !drvFahrt.jum) {
                            totalWafaMeters += drvFahrt.distanceInMeters;
                        }
                    }
                    String totalWafaKm = DataTypeDistance.getDistanceFromMeters(totalWafaMeters).getStringValueInKilometers(false, 0, 1);
                    boolean[] ausg = new boolean[keys.length]; // merken, welche Fahrt schon zur Ausgabe markiert wurde
                    for (int k = 0; k < ausg.length; k++) {
                        ausg[k] = false; // erstmal: keine Fahrt markiert
                    }
                    long hoechst = 0;
                    int hoechstEl = 0; // zum Ermitteln des höchsten verbleibenden Elements
                    long wafaMeters = 0; // Wafa-Meter aller auszugebenden Fahrten
                    int wafaAnzMTour = 0; // für Gruppe 3: Anzahl der Tage durch Wochenendfahrten (2 Tage ohne Km-Beschränkung)
                    int wafaAnzTTour = 0; // für Gruppe 3: Anzahl der 30 Km Tagesfahrten
                    int jumAnz = 0;       // für Gruppe 3 a/b/c: Anzahl der JuM-Regatten
                    DRVFahrt drvel = null, bestEl = null;
                    for (int nr = 0; nr < wafa.length + 1; nr++) { // max. für MAX_WAFA_OUTPUT auszufüllende Felder Fahrten suchen (plus 1 weitere, die aber nicht gemerkt wird)
                        hoechst = 0; // höchste verbleibende KmZahl oder Tagezahl

                        // nächste geeignete Fahrt heraussuchen (meiste Km (Gruppe<3) oder längste Tour (Gruppe 3))
                        for (int k = 0; k < ausg.length; k++) {
                            drvel = (DRVFahrt) wanderfahrten.get(keys[k]);
                            if (!ausg[k] && // Fahrt, die noch nicht ausgegeben wurde, ...
                                    drvel != null && // und die wirklich vorhanden ist, außerdem:
                                    ((gruppen[g].gruppe != 3 && drvel.ok && drvel.distanceInMeters > hoechst) || // Gruppe 1/2: Fahrt "ok", d.h. >30 bzw. >40 Km
                                    (gruppen[g].gruppe == 3 && drvel.days > hoechst && // Gruppe 3:
                                      ( // diverse Kriterien für Gruppe 3
                                            drvel.days > 1 ||                                       // Wochenend-Fahrt
                                            (drvel.days == 1 && drvel.distanceInMeters >= 30000) || // Tagesfahrt
                                            (drvel.jum && gruppen[g].untergruppe <= ABC3)           // JuM-Regatta
                                      )
                                    ))) {
                                bestEl = drvel;
                                if (gruppen[g].gruppe != 3) {
                                    hoechst = drvel.distanceInMeters;
                                } else {
                                    hoechst = drvel.days;
                                }
                                hoechstEl = k;
                            }
                        }
                        if (hoechst != 0 && nr >= wafa.length) {
                            hoechst = 0;
                            mehrFahrten = true; // merken, daß es mehr Fahrten als die ausgegebenen gibt
                        }
                        /** Gruppe 3:
                         * Mindestbedingungen (eine der angegebenen)
                         * - 1 x Dreitagesfahrt (egal wieviele Km)
                         * oder
                         * - 2 x 1 Wochenendfahrt (egal wieviele Km) (>= 4 Tage insg)
                         * - 2 x 1 Tagesfahrt (mind. 30 Km)
                         * - 2 x 1 JuM Regatten (nur Untergruppen a/b/c)
                         * oder eine Kombination aus Wochenendfahrt, Tagesfahrt, und/oder 2 JuM Regatten
                         * Erfüllt wenn:
                         *     (wafaAnzMTour + 2*wafaAnzTTour) >= 3
                         *        wafaAnzMTour == 3 wenn Dreitagesfahrt
                         *        wafaAnzMTour == 4 wenn 2 Wochenendfahrten (oder längere Fahrten)
                         *        wafaAnzTTour == 2 wenn 2 Tagesfahrten
                         *        wafaAnzMTour == 2 && wafaAnzTTour == 1 wenn 1 Wochenendfahrt und 1 Tagesfahrt
                         */
                        if (hoechst > 0 && // was gefunden?
                                (nr < MAX_WAFA_OUTPUT || // weniger als 5 Einträge, oder ...
                                (wafaMeters / 1000 < gruppen[g].zusatz && gruppen[g].gruppe != 3) || // noch Km nötig
                                ( (wafaAnzMTour + 2*wafaAnzTTour) < gruppen[g].zusatz && gruppen[g].gruppe == 3))) {         // noch Fahrten nötig
                            ausg[hoechstEl] = true;
                            wafa[nr][0] = bestEl.entryNo;
                            wafa[nr][1] = bestEl.dateStart.toString();
                            wafa[nr][2] = (bestEl.dateEnd != null ? bestEl.dateEnd.toString() : bestEl.dateStart.toString());
                            wafa[nr][3] = bestEl.destination;
                            wafa[nr][4] = DataTypeDistance.getDistanceFromMeters(bestEl.distanceInMeters).getStringValueInKilometers(false, 0, 1);
                            wafa[nr][5] = bestEl.comments;
                            if (bestEl.jum) {
                                wafa[nr][5] = (wafa[nr][5] == null || wafa[nr][5].length() == 0 ? EfaWettMeldung.JUM :
                                               wafa[nr][5] + " (" + EfaWettMeldung.JUM + ")");
                            }
                            if (!bestEl.jum && wafa[nr][5] != null && wafa[nr][5].indexOf(EfaWettMeldung.JUM) > 0) {
                                wafa[nr][5] = "";
                            }
                            wafaMeters += bestEl.distanceInMeters;
                            if (!bestEl.jum) {
                                if (bestEl.days > 1) {
                                    wafaAnzMTour += bestEl.days;
                                } else {
                                    if (sr.sCompYear >= 2015) {
                                        wafaAnzTTour++; // erst ab 2015
                                    }
                                }
                            } else {
                                jumAnz++;
                            }
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


                    // sollen für den Teilnehmer Daten ausgegeben werden?
                    boolean erfuellt = Daten.wettDefs.erfuelltGruppe(WettDefs.DRV_FAHRTENABZEICHEN,
                            sr.sCompYear,
                            g,
                            jahrgang,
                            sd[i].gender,
                            sd[i].disabled,
                            sd[i].distance,
                            (int)(totalWafaMeters / 1000),
                            (wafaAnzMTour + 2*wafaAnzTTour),
                            jumAnz,
                            0);

                    if (erfuellt) {
                        gesanz++;
                        totalDistanceInDefaultUnit += sd[i].distance;
                    }

                    if (erfuellt
                            || ((DataTypeDistance.getDistance(sd[i].distance).getTruncatedValueInKilometers() >=
                            gruppen[g].km * sr.sCompPercentFulfilled / 100) &&
                            sr.sCompPercentFulfilled < 100)) {

                        int wafaLength;
                        for (wafaLength = 0; wafaLength < MAX_WAFA_OUTPUT && wafa[wafaLength][0] != null; wafaLength++);

                        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett) {
                            // Ausgabe für efaWett
                            if (erfuellt && sd[i].personRecord != null &&
                                sd[i].gender != null) {
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
                                // Kilometer auf- oder abrunden auf ganze Kilometer!
                                ewm.kilometer = Long.toString(DataTypeDistance.getDistance(sd[i].distance).getRoundedValueInKilometers());

                                // Fahrtenheft
                                boolean goldErw = false;
                                boolean goldJug = false;
                                int gesKm = 0;
                                int gesKmAB = 0;
                                int anzAbzeichen = 0;
                                int anzAbzeichenAB = 0;
                                Fahrtenabzeichen fahrtenabzeichen = Daten.project.getFahrtenabzeichen(false);
                                FahrtenabzeichenRecord fahrtenheft = fahrtenabzeichen.getFahrtenabzeichen((UUID)sd[i].key);
                                    if (fahrtenheft != null) {
                                        if (fahrtenheft.getFahrtenheft() != null && fahrtenheft.getFahrtenheft().length() > 0) {
                                            DRVSignatur drvSignatur = new DRVSignatur(fahrtenheft.getFahrtenheft());
                                            if (drvSignatur.getSignatureState() == DRVSignatur.SIG_UNKNOWN_KEY) {
                                                Dialog.infoDialog("Schlüssel nicht bekannt",
                                                        "efa hat für den Teilnehmer " + ewm.vorname + " " + ewm.nachname + "\n"
                                                        + "ein elektronisches Fahrtenheft gefunden, kann dessen Gültigkeit\n"
                                                        + "aber nicht prüfen, da der Schlüssel unbekannt ist.\n"
                                                        + "Im folgenden Dialog wirst Du daher aufgefordert, den Schlüssel\n"
                                                        + "aus dem Internet herunterzuladen.");
                                                if (fahrtenabzeichen.downloadKey(drvSignatur.getKeyName())) {
                                                    drvSignatur.checkSignature();
                                                }
                                            }
                                            if (drvSignatur.getSignatureState() == DRVSignatur.SIG_VALID) {
                                                ewm.drv_fahrtenheft = drvSignatur.toString();
                                                gesKm = drvSignatur.getGesKm();
                                                gesKmAB = drvSignatur.getGesKmAB();
                                                anzAbzeichen = drvSignatur.getAnzAbzeichen();
                                                anzAbzeichenAB = drvSignatur.getAnzAbzeichenAB();
                                            } else {
                                                ungueltigeFahrtenhefte.add(ewm.vorname + " " + ewm.nachname);
                                            }
                                            if (drvSignatur.getJahr() > letzteElektronischeMeldung) {
                                                letzteElektronischeMeldung = drvSignatur.getJahr();
                                            }
                                        }
                                        if (ewm.drv_fahrtenheft == null) {
                                            anzAbzeichen = fahrtenheft.getAbzeichen();
                                            gesKm = fahrtenheft.getKilometer();
                                            anzAbzeichenAB = fahrtenheft.getAbzeichenAB();
                                            gesKmAB = fahrtenheft.getKilometerAB();
                                            if (anzAbzeichen > 0 && gesKm > 0) {
                                                ewm.drv_anzAbzeichen = Integer.toString(anzAbzeichen);
                                                ewm.drv_gesKm = Integer.toString(gesKm);
                                                ewm.drv_anzAbzeichenAB = Integer.toString(anzAbzeichenAB);
                                                ewm.drv_gesKmAB = Integer.toString(gesKmAB);
                                            } else {
                                                zumErstenMalGemeldet.add(ewm.vorname + " " + ewm.nachname);
                                            }
                                        }
                                    } else {
                                        zumErstenMalGemeldet.add(ewm.vorname + " " + ewm.nachname);
                                    }

                                // Äquatorpreis
                                int aeqKm = gesKm; // - gesKmAB; (seit 2007 zählen auch die Kilometer AB zum Äquatorpreis)
                                int anzAeqBefore = aeqKm / WettDefs.DRV_AEQUATOR_KM;
                                int anzAeqJetzt = (aeqKm + EfaUtil.string2int(ewm.kilometer, 0)) / WettDefs.DRV_AEQUATOR_KM;
                                if (anzAeqJetzt > anzAeqBefore) {
                                    ewm.drv_aequatorpreis = Integer.toString(anzAeqJetzt);
                                }

                                // Abzeichen
                                ewm.abzeichen = WettDefs.getDRVAbzeichen(gruppen[g].gruppe != 3, anzAbzeichen, anzAbzeichenAB, sr.sCompYear);

                                for (int j = 0; j < wafaLength; j++) {
                                    ewm.fahrt[j] = wafa[j];
                                }
                                if (efaWett.letzteMeldung() == null) {
                                    efaWett.meldung = ewm;
                                } else {
                                    efaWett.letzteMeldung().next = ewm;
                                }

                                anzahlGemeldeteTeilnehmer++;
                            }
                        } else {
                            // normale Ausgabe des Teilnehmers
                            StatisticsData participant = sd[i];
                            participant.sDistance = DataTypeDistance.getDistance(sd[i].distance).getStringValueInKilometers(false, 0, 0);
                            if (!erfuellt && sr.sIsOutputCompAdditionalWithRequirements) {
                                participant.sDistance += "/" + gruppen[g].km;
                            }
                            if (!sr.sIsOutputCompWithoutDetails && erfuellt) {
                                participant.sDetailsArray = new String[wafaLength + (mehrFahrten ? 1 : 0)][6]; // ein zusätzliches Arrayelement (nicht gefüllt), wenn weitere Fahrten vorliegen
                                for (int j = 0; j < wafaLength; j++) {
                                    participant.sDetailsArray[j] = wafa[j];
                                }
                            } else {
                                if (gruppen[g].gruppe < 3) {
                                    if (sr.sIsOutputCompShort) {
                                        if (sr.sIsOutputCompAdditionalWithRequirements) {
                                            participant.sAdditional = totalWafaKm + "/" + gruppen[g].zusatz + " Wafa-Km";
                                        } else {
                                            participant.sAdditional = totalWafaKm + " Wafa-Km";
                                        }
                                    } else {
                                        participant.sAdditional = totalWafaKm + " Wanderfahrt-Km";
                                    }
                                } else {
                                    if (sr.sIsOutputCompShort) {
                                        if (sr.sIsOutputCompAdditionalWithRequirements) {
                                            participant.sAdditional = wafaAnzMTour + "/" + gruppen[g].zusatz + " Wafa-Tage";
                                        } else {
                                            participant.sAdditional = wafaAnzMTour + " Wafa-Tage";
                                        }
                                    } else {
                                        participant.sAdditional = wafaAnzMTour + " Tage durch Wanderfahrten";
                                    }
                                    if (wafaAnzTTour > 0 || !sr.sIsOutputCompShort) {
                                        participant.sAdditional += ", " + wafaAnzTTour + " Tagesfahrten";
                                    }
                                }
                                if (gruppen[g].gruppe == 3 && gruppen[g].untergruppe <= ABC3) {
                                    if (sr.sIsOutputCompShort) {
                                        participant.sAdditional += ", " + jumAnz + " JuM";
                                    } else {
                                        participant.sAdditional += ", " + jumAnz + " JuM-Regatten";
                                    }
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
                            && Daten.wettDefs.erfuellt(WettDefs.DRV_FAHRTENABZEICHEN, sr.sCompYear, 0, sd[i].gender, sd[i].disabled, sd[i].distance, 9999, 9999, 9999, 0) != null
                            && nichtBeruecksichtigt.get(sd[i].sName) == null) {
                        nichtBeruecksichtigt.put(sd[i].sName, "Wegen fehlenden Jahrgangs ignoriert (" + DataTypeDistance.getDistance(sd[i].distance).getStringValueInKilometers(true, 0, 0) + ")");
                        continue;
                    }
                }
            }
        }
        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett && anzahlGemeldeteTeilnehmer > 0 && ungueltigeFahrtenhefte.size() > 0) {
            Dialog.infoDialog("Warnung",
                    "Die elektronischen Fahrtenhefte folgender Teilnehmer wurden\n"
                    + "NICHT berücksichtigt, da ihre Gültigkeit nicht überprüft werden\n"
                    + "konnte. Bitte prüfe unter ->Administration->DRV-Fahrtenhefte\n"
                    + "den Grund für die Ungültigkeit der Fahrtenhefte.\n"
                    + "Hinweis: Für die elektronisch Meldung müssen die elektronischen Fahrtenhefte\n"
                    + "aller Teilnehmer gültig sein, da sonst der Nachweis der bereits erworbenen\n"
                    + "Fahrtenabzeichen für diese Teilnehmer auf herkömmlichem Weg (durch Einsenden\n"
                    + "der Papier-Fahrtenhefte) erfolgen muß.\n"
                    + "Teilnehmer mit ungültigen Fahrtenheften:\n"
                    + EfaUtil.vector2string(ungueltigeFahrtenhefte, "\n"));
        }
        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett && anzahlGemeldeteTeilnehmer > 0 && zumErstenMalGemeldet.size() > 0) {
            String info;
            if (anzahlGemeldeteTeilnehmer == zumErstenMalGemeldet.size()) {
                info = "Keiner der gemeldeten Teilnehmer hat jemals zuvor ein\n"
                        + "Fahrtenabzeichen erworben, weder auf elektronische, noch\n"
                        + "auf herkömmliche Weise.";
            } else {
                info = "Folgende Teilnehmer haben bislang noch nie ein\n"
                        + "Fahrtenabzeichen erworben, weder auf elektronische,\n"
                        + "noch auf herkömmliche Weise:\n"
                        + EfaUtil.vector2string(zumErstenMalGemeldet, "\n");
            }
            if (Dialog.yesNoDialog("Erstes Fahrtenabzeichen?",
                    info + "\n"
                    + "Ist dies korrekt?") != Dialog.YES) {
                Dialog.infoDialog("Erworbene Fahrtenabzeichen nachtragen",
                        "Bitte trage unter ->Administration->DRV-Fahrtenabzeichen die\n"
                        + "bereits erworbenen Fahrtenabzeichen aller Teilnehmer ein und\n"
                        + "erstelle anschließend eine neue Meldedatei.");
                sr.pCompAbortEfaWett = true;
                return;
            }
        }
        if (sr.getOutputTypeEnum() == StatisticsRecord.OutputTypes.efawett && anzahlGemeldeteTeilnehmer > 0 && letzteElektronischeMeldung + 1 != sr.sCompYear) {
            if (letzteElektronischeMeldung == 0) {
                if (Dialog.yesNoDialog("Erste elektronische Meldung?",
                        "efa hat keine elektronischen Fahrtenhefte für die Teilnehmer\n"
                        + "finden können. Sollte es sich bei dieser Meldung um die erste\n"
                        + "elektronische Meldung handeln, so ist dies korrekt.\n"
                        + "Ist dies die erste elektronische Meldung des Vereins?") != Dialog.YES) {
                    Dialog.infoDialog("Bestätigungsdatei abrufen",
                            "Wenn Du bereits zuvor elektronisch gemeldet hast, so rufe bitte\n"
                            + "zunächst unter ->Administration->DRV-Fahrtenabzeichen über den Punkt\n"
                            + "'Bestätigungsdatei abrufen' die Bestätigungsdatei der letzten Meldung\n"
                            + "ab. Sie enthält die elektronischen Fahrtenhefte der damals gemeldeten\n"
                            + "Teilnehmer. Anschließend erstelle bitte eine neue Meldedatei.");
                    sr.pCompAbortEfaWett = true;
                    return;
                }
            } else if (letzteElektronischeMeldung + 1 < sr.sCompYear) {
                if (Dialog.yesNoDialog("Letzte elektronische Meldung?",
                        "Die ausgewerteten Teilnehmer haben zum letzten Mal im Jahr " + letzteElektronischeMeldung + " ein\n"
                        + "Fahrtenabzeichen erworben. Eventuell in späteren Jahren gemeldete\n"
                        + "Fahrtenabzeichen liegen efa nicht vor und werden daher NICHT berücksichtigt.\n\n"
                        + "Ist es richtig, daß diese Teilnehmer keine neueren Fahrtenabzeichen haben?") != Dialog.YES) {
                    Dialog.infoDialog("Bestätigungsdatei abrufen",
                            "Anscheinend hast Du nach Deiner letzten elektronischen Meldung vergessen,\n"
                            + "die Bestätigungsdatei abzurufen. Rufe daher bitte zunächst unter\n"
                            + "->Administration->DRV-Fahrtenabzeichen über den Punkt 'Bestätigungsdatei abrufen'\n"
                            + "die Bestätigungsdatei der letzten Meldung ab. Sie enthält die elektronischen\n"
                            + "Fahrtenhefte der damals gemeldeten Teilnehmer. Anschließend erstelle bitte eine\n"
                            + "neue Meldedatei.");
                    sr.pCompAbortEfaWett = true;
                    return;
                }
            } else {
                Dialog.infoDialog("Bereits für dieses Jahr gemeldet",
                        "Es liegen für einige Teilnehmer bereits vom DRV bestätigte Fahrtenhefte aus dem Jahr " + sr.sCompYear + " vor.\n"
                        + "Du kannst daher für das Jahr " + sr.sCompYear + " nicht erneut melden.\n\n"
                        + "Falls Deine Meldung nachträglich vom DRV zurückgewiesen wurde und Du nun eine korrigierte Meldung\n"
                        + "einsenden möchtest, mußt Du zuvor alle elektronischen Fahrtenhefte des Jahres " + sr.sCompYear + " in efa\n"
                        + "löschen und die elektronischen Fahrtenhefte bis " + (sr.sCompYear - 1) + " erneut einspielen. Gehe dazu in\n"
                        + "die Übersicht der DRV-Fahrtenabzeichen unter 'Administration - DRV-Fahrtenabzeichen', lösche dort alle\n"
                        + "Fahrtenabzeichen des Jahres " + sr.sCompYear + " und spiele anschließend alle Bestätiungsdateien bis zum\n"
                        + "Jahr " + (sr.sCompYear - 1) + " erneut ein.");
                sr.pCompAbortEfaWett = true;
                return;
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
