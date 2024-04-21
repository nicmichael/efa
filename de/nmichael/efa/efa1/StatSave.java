/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.efa1;

import de.nmichael.efa.*;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.util.*;
import java.io.*;

// @i18n complete

public class StatSave extends DatenListe {

  public static final int NAMESTAT = 0;
  public static final int ART = 1;
  public static final int STAT = 2;
  public static final int AUSGABEDATEI = 3;        // geändert in v1.0 (ehemals DATEIHTML)
  public static final int AUSGABEOVERWRITE = 4;    // geändert in v1.0 (ehemals DATEITXT)
  public static final int AUSGABEART = 5;          // geändert in v1.0 (ehemals OUTPUT)
  public static final int TABELLEHTML = 6;
  public static final int VON = 7;
  public static final int BIS = 8;
  public static final int GESCHLECHT = 9;
  public static final int STATUS = 10;
  public static final int FAHRTART = 11;           // neu in v1.0
  public static final int BART = 12;
  public static final int BANZAHL = 13;
  public static final int BRIGGER = 14;
  public static final int BSTM = 15;
  public static final int BVEREIN = 16;
  public static final int NAME = 17;
  public static final int NAMETEIL = 18;
  public static final int AUSGEBEN = 19;
  public static final int GRAPHISCH = 20;
  public static final int NUMERIERE = 21;
  public static final int SORTIERKRITERIUM = 22;
  public static final int SORTIERFOLGE = 23;
  public static final int SORTVORNACHNAME = 24;
  public static final int GRASIZEKM = 25;
  public static final int GRASIZESTMKM = 26;
  public static final int GRASIZERUDKM = 27;
  public static final int GRASIZEFAHRTEN = 28;
  public static final int GRASIZEKMFAHRT = 29;
  public static final int ZUSAMMENADDIEREN = 30;
  public static final int WW_OPTIONS = 31;          // geänderte Bedeutung in v1.0 (jetzt: ww_horiz_alle)
  public static final int AUCHNULLWERTE = 32;       // geändert in v1.0 (ehemals WARNUNG_OVERWRITE)
  public static final int KMFAHRT_GRUPPIERT = 33;   // neu in v0.80
  public static final int STYLESHEET = 34;          // geändert in v1.0 (ehemals TXTFORMATIERT (neu in v0.80))
  public static final int ZIELEGRUPPIERT = 35;      // neu in v0.80
  public static final int ZEITFBUEBERGREIFEND = 36; // neu in v0.85
  public static final int AUSWETTBEDINGUNGEN = 37;  // neu in v0.85
  public static final int WETTPROZENT = 38;         // neu in v0.90
  public static final int WETTFAHRTEN = 39;         // neu in v0.90
  public static final int GAESTEALSEIN = 40;        // neu in v0.90
  public static final int WETTOHNEDETAIL = 41;      // neu in v0.90
  public static final int WETTJAHR = 42;            // neu in v0.90
  public static final int CROPTOMAXSIZE = 43;       // neu in v0.91
  public static final int MAXSIZEKM = 44;           // neu in v0.91
  public static final int MAXSIZERUDKM = 45;        // neu in v0.91
  public static final int MAXSIZESTMKM = 46;        // neu in v0.91
  public static final int MAXSIZEFAHRTEN = 47;      // neu in v0.91
  public static final int MAXSIZEKMFAHRT = 48;      // neu in v0.91
  public static final int MAXSIZEDAUER = 49;        // neu in v1.00
  public static final int MAXSIZEKMH = 50;          // neu in v1.00
  public static final int GRASIZEDAUER = 51;        // neu in v1.00
  public static final int GRASIZEKMH = 52;          // neu in v1.00
  public static final int NURBEMERK = 53;           // neu in v1.1.0
  public static final int NURBEMERKNICHT = 54;      // neu in v1.1.0
  public static final int ZUSWETT1 = 55;            // neu in v1.2.0
  public static final int ZUSWETT2 = 56;            // neu in v1.2.0
  public static final int ZUSWETT3 = 57;            // neu in v1.2.0
  public static final int ZUSWETTJAHR1 = 58;        // neu in v1.2.0
  public static final int ZUSWETTJAHR2 = 59;        // neu in v1.2.0
  public static final int ZUSWETTJAHR3 = 60;        // neu in v1.2.0
  public static final int ZUSWETTMITANFORD = 61;    // neu in v1.2.0
  public static final int NURSTEGKM = 62;           // neu in v1.2.0
  public static final int ZEITVORJAHRESVERGLEICH=63;// neu in v1.3.0
  public static final int NURMINDKM=64;             // neu in v1.3.0
  public static final int AUCHINEFADIREKT=65;       // neu in v1.3.1
  public static final int FAHRTENBUCHFELDER=66;     // neu in v1.4.0
  public static final int ZUSAMMENGEFASSTEWERTEOHNEBALKEN=67; // neu in v1.4.1
  public static final int NAME_ODER_GRUPPE=68;      // neu in v1.7.0
  public static final int FILE_EXEC_BEFORE=69;      // neu in v1.7.0
  public static final int FILE_EXEC_AFTER=70;       // neu in v1.7.0
  public static final int NUR_FB=71;                // neu in v1.7.2
  public static final int NURBOOTEFUERGRUPPE=72;    // neu in v1.8.1
  public static final int ALLEZIELFAHRTEN = 73;     // neu in v1.8.2
  public static final int NURGANZEKM = 74;          // neu in v1.8.3


  public static final String KENNUNG070 = "##EFA.070.STATISTIK##";
  public static final String KENNUNG080 = "##EFA.080.STATISTIK##";
  public static final String KENNUNG085 = "##EFA.085.STATISTIK##";
  public static final String KENNUNG090 = "##EFA.090.STATISTIK##";
  public static final String KENNUNG091 = "##EFA.091.STATISTIK##";
  public static final String KENNUNG100 = "##EFA.100.STATISTIK##";
  public static final String KENNUNG110 = "##EFA.110.STATISTIK##";
  public static final String KENNUNG120 = "##EFA.120.STATISTIK##";
  public static final String KENNUNG130 = "##EFA.130.STATISTIK##";
  public static final String KENNUNG131 = "##EFA.131.STATISTIK##";
  public static final String KENNUNG140 = "##EFA.140.STATISTIK##";
  public static final String KENNUNG141 = "##EFA.141.STATISTIK##";
  public static final String KENNUNG160 = "##EFA.160.STATISTIK##";
  public static final String KENNUNG170 = "##EFA.170.STATISTIK##";
  public static final String KENNUNG172 = "##EFA.172.STATISTIK##";
  public static final String KENNUNG180 = "##EFA.180.STATISTIK##";
  public static final String KENNUNG181 = "##EFA.181.STATISTIK##";
  public static final String KENNUNG182 = "##EFA.182.STATISTIK##";
  public static final String KENNUNG190 = "##EFA.190.STATISTIK##";

  // Konstruktor
  public StatSave(String pdat) {
    super(pdat,75,1,false);
    kennung = KENNUNG190;
  }


  // Dateiformat überprüfen, ggf. konvertieren
  public boolean checkFileFormat() {
    String s;
    try {
      s = freadLine();
      if ( s == null || !s.trim().startsWith(kennung) ) {

        // KONVERTIEREN: 182 -> 190
        if (s != null && s.trim().startsWith(KENNUNG182)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"182");
          iniList(this.dat,74,1,false); // Rahmenbedingungen von v1.9.0 schaffen
          // Datei lesen
          try {
            while ((s = freadLine()) != null) {
              s = s.trim();
              if (s.equals("") || s.startsWith("#")) continue; // Kommentare ignorieren
              add(constructFields(s));
            }
          } catch(IOException e) {
             errReadingFile(dat,e.getMessage());
             return false;
          }
          kennung = KENNUNG190;
          if (closeFile()) {
            infSuccessfullyConverted(dat,kennung);
            s = kennung;
          } else errConvertingFile(dat,kennung);
        }

        // FERTIG MIT KONVERTIEREN
        if (s == null || !s.trim().startsWith(KENNUNG190)) {
          errInvalidFormat(dat, EfaUtil.trimto(s, 20));
          fclose(false);
          return false;
        }
      }
    } catch(IOException e) {
      errReadingFile(dat,e.getMessage());
      return false;
    }
    return true;
  }

  // Einträge auf Gültigkeit prüfen
  public void validateValues(DatenFelder d) {

    // Überprüfen, ob die in bezeichnungen.cfg konfigurierten Fahrtarten mit denen in den gespeicherten Statistikeinstellungen übereinstimmen
    if (d != null && Daten.efaTypes != null) {
      String fahrtart = d.get(FAHRTART);
      if (fahrtart != null && Daten.efaTypes.size(EfaTypes.CATEGORY_SESSION) != fahrtart.length()) {
        if (fahrtart.length() < Daten.efaTypes.size(EfaTypes.CATEGORY_SESSION)) {
          if (fahrtart.indexOf("-") < 0) {
            // fehlende Fahrtarten, aber vorher alle selektiert
            while (fahrtart.length() < Daten.efaTypes.size(EfaTypes.CATEGORY_SESSION)) fahrtart += "+";
            d.set(FAHRTART, fahrtart);
          } else {
            // fehlende Fahrtarten, aber vorher nicht alle selektiert
            // Nothing we can do about it!
          }
        } else {
          // zu viele Fahrtarten
          // Nothing we should do in this case!
        }
      }
    }
  }


}
