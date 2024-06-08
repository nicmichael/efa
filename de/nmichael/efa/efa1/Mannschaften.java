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
import java.io.IOException;

// @i18n complete

public class Mannschaften extends DatenListe {

  public static final int BOOT      =  0;
  public static final int STM       =  1; // Felder STM bis MANNSCH16 müssen fortlaufende Nummern haben!!! (s. MannschaftFrame.show())
  public static final int MANNSCH1  =  2;
  public static final int MANNSCH2  =  3;
  public static final int MANNSCH3  =  4;
  public static final int MANNSCH4  =  5;
  public static final int MANNSCH5  =  6;
  public static final int MANNSCH6  =  7;
  public static final int MANNSCH7  =  8;
  public static final int MANNSCH8  =  9;
  public static final int MANNSCH9  = 10;
  public static final int MANNSCH10 = 11;
  public static final int MANNSCH11 = 12;
  public static final int MANNSCH12 = 13;
  public static final int MANNSCH13 = 14;
  public static final int MANNSCH14 = 15;
  public static final int MANNSCH15 = 16;
  public static final int MANNSCH16 = 17;
  public static final int MANNSCH17 = 18; // neu in v1.4.0
  public static final int MANNSCH18 = 19; // neu in v1.4.0
  public static final int MANNSCH19 = 20; // neu in v1.4.0
  public static final int MANNSCH20 = 21; // neu in v1.4.0
  public static final int MANNSCH21 = 22; // neu in v1.4.0
  public static final int MANNSCH22 = 23; // neu in v1.4.0
  public static final int MANNSCH23 = 24; // neu in v1.4.0
  public static final int MANNSCH24 = 25; // neu in v1.4.0
  public static final int ZIEL      = 26; // vor 1.4.0: 18
  public static final int FAHRTART  = 27; // vor 1.4.0: 19
  public static final int OBMANN    = 28; // neu in v1.7.3

  public static       String NO_FAHRTART = "--- keine Auswahl ---"; // wird im Konstruktor gesetzt
  public static       String NO_OBMANN   = "--- keine Auswahl ---"; // wird im Konstruktor gesetzt

  public static final String KENNUNG120 = "##EFA.120.MANNSCHAFTEN##";
  public static final String KENNUNG135 = "##EFA.135.MANNSCHAFTEN##";
  public static final String KENNUNG173 = "##EFA.173.MANNSCHAFTEN##";
  public static final String KENNUNG190 = "##EFA.190.MANNSCHAFTEN##";

  // Konstruktor
  public Mannschaften(String pdat) {
    super(pdat,29,1,false);
    kennung = KENNUNG190;
    NO_FAHRTART = "--- " + International.getString("keine Auswahl") + " ---";
    NO_OBMANN   = "--- " + International.getString("keine Auswahl") + " ---";
  }



  // Dateiformat überprüfen, ggf. konvertieren
  public boolean checkFileFormat() {
    String s;
    try {
      s = freadLine();
      if ( s == null || !s.trim().startsWith(kennung) ) {

        // KONVERTIEREN 173 -> 190
        if ( s != null && s.trim().startsWith(KENNUNG173)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"173");
          iniList(this.dat,29,1,true); // Rahmenbedingungen von v1.9.0 schaffen
          try {
            while ((s = freadLine()) != null) {
              s = s.trim();
              if (s.equals("") || s.startsWith("#")) continue; // Kommentare ignorieren
              DatenFelder d = constructFields(s);
              String fa = d.get(FAHRTART);
              if (fa.length() == 0) {
                  // noting to do (no default fahrtart selected)
              } else {
                  fa = Daten.efaTypes.getTypeForValue(EfaTypes.CATEGORY_SESSION, d.get(FAHRTART));
                  if (fa == null) {
                      fa = "";
                  }
              }
              d.set(FAHRTART, fa);
              add(d);
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



}
