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

// Es werden genau die Daten ausgegeben, die "!= null" bzw. "!= -1" sind!!

import java.util.UUID;

public class EfaWettMeldung {

    public static final String GESCHLECHT_M = "M";
    public static final String GESCHLECHT_W = "W";
    public static final String ABZEICHEN_ERW_EINF = "EE";
    public static final String ABZEICHEN_JUG_EINF = "JE";
    public static final String ABZEICHEN_ERW_GOLD_PRAEFIX = "EG";
    public static final String ABZEICHEN_JUG_GOLD_PRAEFIX = "JG";
    public static final String[] ABZEICHEN_ERW_GOLD_LIST = {"dummy",
        "EG05",
        "EG10",
        "EG15",
        "EG20",
        "EG25",
        "EG30",
        "EG35",
        "EG40",
        "EG45",
        "EG50",
        "EG55",
        "EG60",
        "EG65",
        "EG70",
        "EG75",
        "EG80",
        "EG85",
        "EG90",
        "EG95"};
    public static final String[] ABZEICHEN_JUG_GOLD_LIST = {"dummy",
        "JG05",
        "JG10",
        "JG15"};
    public static final String JUM = "JUM";
    public static final int FAHRT_ANZ_X = 50;
    public static final int FAHRT_ANZ_Y = 6;
    public UUID personID = null;
    public String nachname = null;
    public String vorname = null;
    public String jahrgang = null;
    public String geschlecht = null;
    public String gruppe = null;
    public String kilometer = null;
    public String restkm = null;
    public String anschrift = null;
    public String abzeichen = null;
    public String drv_teilnNr = null;
    public String drv_anzAbzeichen = null;
    public String drv_gesKm = null;
    public String drv_anzAbzeichenAB = null;
    public String drv_gesKmAB = null;
    public String drv_fahrtenheft = null;
    public String drv_aequatorpreis = null;
    public String drvWS_LfdNr = null;
    public String drvWS_StartZiel = null;
    public String drvWS_Strecke = null;
    public String drvWS_Gewaesser = null;
    public String drvWS_Km = null;
    public String drvWS_Tage = null;
    public String drvWS_Teilnehmer = null;
    public String drvWS_MannschKm = null;
    public String drvWS_MaennerAnz = null;
    public String drvWS_MaennerKm = null;
    public String drvWS_JuniorenAnz = null;
    public String drvWS_JuniorenKm = null;
    public String drvWS_FrauenAnz = null;
    public String drvWS_FrauenKm = null;
    public String drvWS_JuniorinnenAnz = null;
    public String drvWS_JuniorinnenKm = null;
    public String drvWS_Bemerkungen = null;
    public String[][] fahrt = new String[FAHRT_ANZ_X][FAHRT_ANZ_Y];
    public EfaWettMeldung next = null;
    // Bearbeitungsdaten durch den DRV (wird ebenfalls in der Datei gespeichert)
    // Diese Felder müssen unbedingt in EfaWett.resetDrvIntern() zurückgesetzt werden
    public boolean drvint_fahrtErfuellt = false; // betrachte "fahrt" als erfüllt (egal, was dort angegeben ist)
    public boolean drvint_wirdGewertet = false;
    public boolean drvint_wirdGewertetExplizitGesetzt = false; // wird beim Lesen der Datei auf true gesetzt, wenn diese
    // Information ("wirdGewertet" ja/nein) in der Datei steht
    // (wird aber nicht als eigenes Feld in der Datei geführt)
    public String drvint_nichtGewertetGrund = null;
    public String drvint_nachfrage = null;
    public boolean drvint_geprueft = false;
    // Werte, die nicht in die Datei geschrieben werden
    public DRVSignatur drvSignatur = null;
    public boolean sigValid = false;
    public String sigError = null;
    public boolean changed = false;

    // Default Constructor
    public EfaWettMeldung() {
        // nothing to do
    }

    // Copy Constructor
    public EfaWettMeldung(EfaWettMeldung ewm) {
        this.personID = ewm.personID;
        this.nachname = ewm.nachname;
        this.vorname = ewm.vorname;
        this.jahrgang = ewm.jahrgang;
        this.geschlecht = ewm.geschlecht;
        this.gruppe = ewm.gruppe;
        this.kilometer = ewm.kilometer;
        this.restkm = ewm.restkm;
        this.anschrift = ewm.anschrift;
        this.abzeichen = ewm.abzeichen;
        this.drv_teilnNr = ewm.drv_teilnNr;
        this.drv_anzAbzeichen = ewm.drv_anzAbzeichen;
        this.drv_gesKm = ewm.drv_gesKm;
        this.drv_anzAbzeichenAB = ewm.drv_anzAbzeichenAB;
        this.drv_gesKmAB = ewm.drv_gesKmAB;
        this.drv_fahrtenheft = ewm.drv_fahrtenheft;
        this.drv_aequatorpreis = ewm.drv_aequatorpreis;
        this.drvWS_LfdNr = ewm.drvWS_LfdNr;
        this.drvWS_StartZiel = ewm.drvWS_StartZiel;
        this.drvWS_Strecke = ewm.drvWS_Strecke;
        this.drvWS_Gewaesser = ewm.drvWS_Gewaesser;
        this.drvWS_Km = ewm.drvWS_Km;
        this.drvWS_Tage = ewm.drvWS_Tage;
        this.drvWS_Teilnehmer = ewm.drvWS_Teilnehmer;
        this.drvWS_MannschKm = ewm.drvWS_MannschKm;
        this.drvWS_MaennerAnz = ewm.drvWS_MaennerAnz;
        this.drvWS_MaennerKm = ewm.drvWS_MaennerKm;
        this.drvWS_JuniorenAnz = ewm.drvWS_JuniorenAnz;
        this.drvWS_JuniorenKm = ewm.drvWS_JuniorenKm;
        this.drvWS_FrauenAnz = ewm.drvWS_FrauenAnz;
        this.drvWS_FrauenKm = ewm.drvWS_FrauenKm;
        this.drvWS_JuniorinnenAnz = ewm.drvWS_JuniorinnenAnz;
        this.drvWS_JuniorinnenKm = ewm.drvWS_JuniorinnenKm;
        this.drvWS_Bemerkungen = ewm.drvWS_Bemerkungen;
        for (int i = 0; ewm.fahrt != null && this.fahrt != null && i < this.fahrt.length && i < ewm.fahrt.length; i++) {
            for (int j = 0; ewm.fahrt[i] != null && this.fahrt[i] != null && j < this.fahrt[i].length && j < ewm.fahrt[i].length; j++) {
                this.fahrt[i][j] = ewm.fahrt[i][j];
            }
        }
        this.drvint_fahrtErfuellt = ewm.drvint_fahrtErfuellt;
        this.next = ewm.next;

        this.drvSignatur = ewm.drvSignatur;
        this.sigValid = ewm.sigValid;
        this.sigError = ewm.sigError;
        this.drvint_geprueft = ewm.drvint_geprueft;
        this.drvint_wirdGewertet = ewm.drvint_wirdGewertet;
        this.drvint_nichtGewertetGrund = ewm.drvint_nichtGewertetGrund;
        this.drvint_nachfrage = ewm.drvint_nachfrage;

        this.changed = false;
    }

    public static String getAbzeichenGold(int jahre, boolean erwachsene) {
        if (jahre % 5 != 0) {
            return null;
        }
        if (jahre < 5) {
            return null;
        }
        try {
            if (erwachsene) {
                return ABZEICHEN_ERW_GOLD_LIST[jahre / 5];
            } else {
                return ABZEICHEN_JUG_GOLD_LIST[jahre / 5];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public static int getAbzeichenGoldIndex(String abzeichen) {
        for (int i = 1; i < ABZEICHEN_ERW_GOLD_LIST.length; i++) {
            if (ABZEICHEN_ERW_GOLD_LIST[i].equals(abzeichen)) {
                return i;
            }
        }
        for (int i = 1; i < ABZEICHEN_JUG_GOLD_LIST.length; i++) {
            if (ABZEICHEN_JUG_GOLD_LIST[i].equals(abzeichen)) {
                return i;
            }
        }
        return -1;
    }
}
