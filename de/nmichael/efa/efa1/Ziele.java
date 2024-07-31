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

import de.nmichael.efa.data.efawett.ZielfahrtFolge;
import de.nmichael.efa.data.efawett.Zielfahrt;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.io.IOException;

// @i18n complete

public class Ziele extends DatenListe {

  public static final int NAME = 0;
  public static final int KM = 1;
  public static final int BEREICH = 2;
  public static final int STEGZIEL = 3; // neu in v1.2.0
  public static final int GEWAESSER = 4; // neu in v1.4.1
  public static final int _ANZFELDER = 5;


  public static final String KENNUNG060 = "##EFA.060.ZIELE##";
  public static final String KENNUNG120 = "##EFA.120.ZIELE##";
  public static final String KENNUNG141 = "##EFA.141.ZIELE##";
  public static final String KENNUNG152 = "##EFA.152.ZIELE##";
  public static final String KENNUNG174 = "##EFA.174.ZIELE##";
  public static final String KENNUNG190 = "##EFA.190.ZIELE##";

  public static final String zielfahrtSeparatorBereiche = ",";
  public static final String zielfahrtSeparatorFahrten = ";";

  // Konstruktor
  public Ziele(String pdat) {
    super(pdat,_ANZFELDER,1,false);
    kennung = KENNUNG190;
  }


  // Dateiformat überprüfen, ggf. konvertieren
  public boolean checkFileFormat() {
    String s;
    try {
      s = freadLine();
      if ( s == null || !s.trim().startsWith(kennung) ) {

        // KONVERTIEREN: 174 -> 190
        if (s != null && s.trim().startsWith(KENNUNG174)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"174");
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
    d.set(KM,EfaUtil.zehntelInt2String(EfaUtil.zehntelString2Int(d.get(KM))));
    if (zielfahrtSeparatorFahrten.length() == 1 &&
        d.get(BEREICH).indexOf(zielfahrtSeparatorFahrten)>=0) d.set(STEGZIEL,"-");
  }


  public void checkAllZielbereiche(String _heimZb) {
    if (_heimZb == null || _heimZb.length()==0) return;
    boolean changes = false;
    int heimZb = EfaUtil.string2int(_heimZb,0);
    if (heimZb < 1 || heimZb > Zielfahrt.ANZ_ZIELBEREICHE) return;
    for (DatenFelder d = getCompleteFirst(); d != null; d = getCompleteNext()) {
      if (d.get(Ziele.STEGZIEL).equals("+")) {
        ZielfahrtFolge zff = new ZielfahrtFolge(d.get(Ziele.BEREICH));
        boolean eigenerBereichErreicht = false;
        for (int i=0; i<zff.getAnzZielfahrten(); i++) {
          if (zff.getZielfahrt(i).isErreicht(heimZb)) eigenerBereichErreicht = true;
        }
        if (eigenerBereichErreicht) {
            // needs no internationalization (just for Berlin, Germany)
          switch (Dialog.auswahlDialog("Fehlerhafter Zieleintrag",
                                       "Name des Ziels: "+d.get(NAME)+" ("+d.get(KM)+" Km)\n"+
                                       "Zielbereiche: "+d.get(BEREICH)+"\n"+
                                       "Steg und Ziel ist eigenes Bootshaus: JA\n\n"+
                                       "Sollte es sich bei dem genannten Ziel tatsächlich um eine Fahrt handeln,\n"+
                                       "deren Start und Zielpunkt das eigene Bootshaus ist, so wird für dieses\n"+
                                       "Ziel der eigene Zielbereich \""+_heimZb+"\" NICHT gewertet und darf\n"+
                                       "folglich auch NICHT angegeben werden.\n\n"+
                                       "Um den Fehler zu beheben, kann efa nun den Zielbereich \""+_heimZb+"\"\n"+
                                       "aus der Aufzählung der Zielbereiche entfernen oder die Eigenschaft\n"+
                                       "'Start und Ziel ist eigenes Bootshaus' auf 'Nein' setzen, falls\n"+
                                       "es sich doch nicht um eine Fahrt vom und zum eigenen Bootshaus handelt.\n",
                                       "Zielbereich "+_heimZb+" entfernen","Start und Ziel ist NICHT Bootshaus","Nichts tun")) {
            case 0: Zielfahrt zf = new Zielfahrt();
                    zf.setBereich(d.get(BEREICH));
                    zf.setBereich(heimZb,false);
                    d.set(BEREICH,zf.getBereiche());
                    changes = true;
                    break;
            case 1: d.set(STEGZIEL,"-");
                    changes = true;
                    break;
            default: // nothing to do
          }
        }
      }
    }
    if (changes) writeFile();
  }

}
