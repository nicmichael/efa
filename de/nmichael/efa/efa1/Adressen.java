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

import java.io.*;
import de.nmichael.efa.util.*;

// @i18n complete

public class Adressen extends DatenListe {


  public static final int NAME = 0;
  public static final int ADRESSE = 1;

  public static final String KENNUNG091 = "##EFA.091.ADRESSEN##";
  public static final String KENNUNG190 = "##EFA.190.ADRESSEN##";


  // Konstruktor
  public Adressen(String pdat) {
    super(pdat,2,1,false);
    kennung = KENNUNG190;
  }


  // Key-Wert ermitteln
  public String constructKey(DatenFelder d) {
    return d.get(NAME);
  }

  public boolean checkFileFormat() {
    String s;
    try {
      s = freadLine();
      if ( s == null || !s.trim().startsWith(kennung) ) {
        // KONVERTIEREN: 091 -> 190
        if (s != null && s.trim().startsWith(KENNUNG091)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"091");
          iniList(this.dat,2,1,false); // Rahmenbedingungen von v1.9.0 schaffen
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