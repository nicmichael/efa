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
import de.nmichael.efa.efa1.DatenFelder;
import de.nmichael.efa.util.*;
import java.io.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class MeldungenIndex extends DatenListe {

  public static final int _ANZFELDER = 8;

  public static final int QNR = 0;
  public static final int VEREIN = 1;
  public static final int MITGLNR = 2;
  public static final int DATUM = 3;
  public static final int STATUS = 4;
  public static final int FAHRTENHEFTE = 5;
  public static final int BESTAETIGUNGSDATEI = 6;
  public static final int EDITUUID = 7;

  public static final int ST_UNBEKANNT = 0;
  public static final int ST_UNBEARBEITET = 1;
  public static final int ST_BEARBEITET = 2;
  public static final int ST_ZURUECKGEWIESEN = 3;
  public static final int ST_GELOESCHT = 4;
  public static final String[] ST_NAMES = { "unbekannt" , "unbearbeitet" , "bearbeitet" , "zurückgewiesen" , "gelöscht" };
  
  public static final String MANUELL_ERFASST = "MANUELL";

  public static final int FH_UNBEKANNT = 0;
  public static final int FH_KEINE = 1;
  public static final int FH_PAPIER = 2;
  public static final int FH_ELEKTRONISCH = 3;
  public static final int FH_PAPIER_UND_ELEKTRONISCH = 4;

  public static final String KENNUNG150 = "##EFA.150.DRVMELDUNGENINDEX##";
  public static final String KENNUNG160 = "##EFA.160.DRVMELDUNGENINDEX##";
  public static final String KENNUNG190 = "##EFA.190.DRVMELDUNGENINDEX##";
  public static final String KENNUNG222 = "##EFA.222.DRVMELDUNGENINDEX##";

  // Konstruktor
  public MeldungenIndex(String pdat) {
    super(pdat,_ANZFELDER,1,false);
    kennung = KENNUNG222;
  }

  // Dateiformat überprüfen, ggf. konvertieren
  public boolean checkFileFormat() {
    String s;
    try {
      s = freadLine();
      if ( s == null || !s.trim().startsWith(kennung) ) {

        // KONVERTIEREN: 150 -> 160
        if (s != null && s.trim().startsWith(KENNUNG150)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"150");
          iniList(this.dat,7,1,false); // Rahmenbedingungen von v1.5.0 schaffen
          // Datei lesen
          try {
            while ((s = freadLine()) != null) {
              s = s.trim();
              if (s.equals("") || s.startsWith("#")) continue; // Kommentare ignorieren
              DatenFelder d = constructFields(s);
              if (d.get(FAHRTENHEFTE).equals("+")) d.set(FAHRTENHEFTE,Integer.toString(FH_PAPIER));
              else d.set(FAHRTENHEFTE,Integer.toString(FH_UNBEKANNT));
              add(d);
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

        // KONVERTIEREN: 160 -> 190
        if (s != null && s.trim().startsWith(KENNUNG160)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"160");
          iniList(this.dat,7,1,false); // Rahmenbedingungen von v1.9.0 schaffen
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
          if (closeFile() && writeFile(true) && openFile()) {
            infSuccessfullyConverted(dat,kennung);
            s = kennung;
          } else errConvertingFile(dat,kennung);
        }

        // KONVERTIEREN: 190 -> 222
        if (s != null && s.trim().startsWith(KENNUNG190)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"160");
          iniList(this.dat,8,1,false); // Rahmenbedingungen von v2.2.2 schaffen
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
          kennung = KENNUNG222;
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
