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

import de.nmichael.efa.util.*;
import java.io.*;

// @i18n complete

public class Fahrtenabzeichen extends DatenListe {

  public static final int _ANZ = 8;            // geändert in v1.5.1 (in 1.5.0: 6)

  public static final int VORNAME = 0;
  public static final int NACHNAME = 1;
  public static final int JAHRGANG = 2;
  public static final int ANZABZEICHEN = 3;
  public static final int GESKM = 4;
  public static final int ANZABZEICHENAB = 5;  // neu in v1.5.1
  public static final int GESKMAB = 6;         // neu in v1.5.1
  public static final int LETZTEMELDUNG = 7;   // geändert in v1.5.1 (in 1.5.0: 5)

  public static final String KENNUNG150 = "##EFA.150.FAHRTENABZEICHEN##";
  public static final String KENNUNG151 = "##EFA.151.FAHRTENABZEICHEN##";
  public static final String KENNUNG190 = "##EFA.190.FAHRTENABZEICHEN##";

  // globale Werte für die Datei
  private String quittungsnummer = null;                     // Quittungsnummer der letzten Meldung
  private int letzteMeldung = 0;                             // Jahr der letzten Meldung
  private boolean bestaetigungsdateiHeruntergeladen = false; // true, wenn die Bestätigungsdatei der letzten Meldung heruntergeladen wurde


  // Konstruktor
  public Fahrtenabzeichen(String pdat) {
    super(pdat,_ANZ,1,false);
    kennung = KENNUNG190;
  }


  // Key-Wert ermitteln
  public String constructKey(DatenFelder d) {
    return d.get(VORNAME)+" "+d.get(NACHNAME);
  }

  // Dateiformat überprüfen, ggf. konvertieren
  public boolean checkFileFormat() {
    String s;
    try {
      s = freadLine();
      if ( s == null || !s.trim().startsWith(kennung) ) {


        // KONVERTIEREN: 151 -> 190
        if (s != null && s.trim().startsWith(KENNUNG151)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"151");
          iniList(this.dat,8,1,false); // Rahmenbedingungen von v1.9.0 schaffen
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

}
