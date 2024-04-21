/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.drv;

import de.nmichael.efa.efa1.DatenListe;
import de.nmichael.efa.util.*;
import java.io.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class Meldestatistik extends DatenListe {

  public static final int _ANZFELDER = 27;

  public static final int KEY = 0; // VEREINSMITGLNR#VORNAME#NACHNAME#JAHRGANG
  public static final int VEREINSMITGLNR = 1;
  public static final int VEREIN = 2;
  public static final int VORNAME = 3;
  public static final int NACHNAME = 4;
  public static final int JAHRGANG = 5;
  public static final int GESCHLECHT = 6;
  public static final int KILOMETER = 7;
  public static final int GRUPPE = 8;
  public static final int ANZABZEICHEN = 9;
  public static final int ANZABZEICHENAB = 10;
  public static final int AEQUATOR = 11;
  public static final int GESKM = 12;
  public static final int WS_BUNDESLAND = 13;
  public static final int WS_MITGLIEDIN = 14;
  public static final int WS_GEWAESSER = 15;
  public static final int WS_TEILNEHMER = 16;
  public static final int WS_MANNSCHKM = 17;
  public static final int WS_MAENNERKM = 18;
  public static final int WS_JUNIORENKM = 19;
  public static final int WS_FRAUENKM = 20;
  public static final int WS_JUNIORINNENKM = 21;
  public static final int WS_AKT18M = 22;
  public static final int WS_AKT19M = 23;
  public static final int WS_AKT18W = 24;
  public static final int WS_AKT19W = 25;
  public static final int WS_VEREINSKILOMETER = 26;


  public static final String KENNUNG151 = "##EFA.151.MELDESTATISTIK##";
  public static final String KENNUNG160 = "##EFA.160.MELDESTATISTIK##";
  public static final String KENNUNG183 = "##EFA.183.MELDESTATISTIK##";
  public static final String KENNUNG190 = "##EFA.190.MELDESTATISTIK##";
  public static final String KENNUNG221 = "##EFA.221.MELDESTATISTIK##";

  // Konstruktor
  public Meldestatistik(String pdat) {
    super(pdat,_ANZFELDER,1,false);
    kennung = KENNUNG221;
  }


  // Dateiformat überprüfen, ggf. konvertieren
  public boolean checkFileFormat() {
    String s;
    try {
      s = freadLine();
      if ( s == null || !s.trim().startsWith(kennung) ) {

        // KONVERTIEREN: 151 -> 160
        if (s != null && s.trim().startsWith(KENNUNG151)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"151");
          iniList(this.dat,22,1,true); // Rahmenbedingungen von v160 schaffen
          try {
            while ((s = freadLine()) != null) {
              s = s.trim();
              if (s.equals("") || s.startsWith("#")) continue; // Kommentare ignorieren
              s += "|||||||||";
              add(s);
            }
          } catch(IOException e) {
             errReadingFile(dat,e.getMessage());
             return false;
          }
          kennung = KENNUNG160;
          if (closeFile() && writeFile(true) && openFile()) {
            infSuccessfullyConverted(dat,kennung);
            s = kennung;
          } else errConvertingFile(dat,kennung);
        }

        // KONVERTIEREN: 160 -> 183
        if (s != null && s.trim().startsWith(KENNUNG160)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"160");
          iniList(this.dat,26,1,true); // Rahmenbedingungen von v183 schaffen
          try {
            while ((s = freadLine()) != null) {
              s = s.trim();
              if (s.equals("") || s.startsWith("#")) continue; // Kommentare ignorieren
              s += "||||";
              add(s);
            }
          } catch(IOException e) {
             errReadingFile(dat,e.getMessage());
             return false;
          }
          kennung = KENNUNG183;
          if (closeFile() && writeFile(true) && openFile()) {
            infSuccessfullyConverted(dat,kennung);
            s = kennung;
          } else errConvertingFile(dat,kennung);
        }

        // KONVERTIEREN: 182 -> 190
        if (s != null && s.trim().startsWith(KENNUNG183)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"183");
          iniList(this.dat,26,1,true); // Rahmenbedingungen von v190 schaffen
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
          if (closeFile() && writeFile(true) && openFile()) {
            infSuccessfullyConverted(dat,kennung);
            s = kennung;
          } else errConvertingFile(dat,kennung);
        }

        // KONVERTIEREN: 190 -> 221
        if (s != null && s.trim().startsWith(KENNUNG190)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"183");
          iniList(this.dat,27,1,true); // Rahmenbedingungen von v221 schaffen
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
          kennung = KENNUNG221;
          if (closeFile() && writeFile(true) && openFile()) {
            infSuccessfullyConverted(dat,kennung);
            s = kennung;
          } else errConvertingFile(dat,kennung);
        }

        // FERTIG MIT KONVERTIEREN
        if (s == null || !s.trim().startsWith(kennung)) {
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




}
