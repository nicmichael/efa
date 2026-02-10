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
import java.io.*;
import de.nmichael.efa.util.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class Teilnehmer extends DatenListe {

  public static final int _ANZFELDER = 5;

  public static final int TEILNNR = 0;
  public static final int VORNAME = 1;
  public static final int NACHNAME = 2;
  public static final int JAHRGANG = 3;
  public static final int FAHRTENHEFT = 4;

  public static final String KENNUNG151 = "##EFA.151.DRVTEILNEHMER##";
  public static final String KENNUNG190 = "##EFA.190.DRVTEILNEHMER##";

  // Konstruktor
  public Teilnehmer(String pdat) {
    super(pdat,_ANZFELDER,1,false);
    kennung = KENNUNG190;
  }

  public boolean checkFileFormat() {
    String s;
    try {
      s = freadLine();
      if ( s == null || !s.trim().startsWith(kennung) ) {
        // KONVERTIEREN: 151 -> 190
        if (s != null && s.trim().startsWith(KENNUNG151)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"151");
          iniList(this.dat,5,1,false); // Rahmenbedingungen von v1.9.0 schaffen
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
