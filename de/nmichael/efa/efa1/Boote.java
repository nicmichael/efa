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
import de.nmichael.efa.util.*;
import de.nmichael.efa.core.config.EfaTypes;
import java.io.*;
import java.util.*;

// @i18n complete

public class Boote extends DatenListe {

  public static final int NAME = 0;
  public static final int VEREIN = 1;
  public static final int ART = 2;
  public static final int ANZAHL = 3;
  public static final int RIGGER = 4;
  public static final int STM = 5;
  public static final int GRUPPEN = 6;
  public static final int MAX_NICHT_IN_GRUPPE = 7;
  public static final int MIND_1_IN_GRUPPE = 8;
  public static final int FREI1 = 9;
  public static final int FREI2 = 10;
  public static final int FREI3 = 11;

  public static final int _ANZFELDER = 12;

  public static final String KENNUNG060 = "##EFA.060.BOOTE##";
  public static final String KENNUNG170 = "##EFA.170.BOOTE##";
  public static final String KENNUNG190 = "##EFA.190.BOOTE##";

  // Konstruktor
  public Boote(String pdat) {
    super(pdat,_ANZFELDER,1,false);
    kennung = KENNUNG190;
  }


  // Key-Wert ermitteln
  public String constructKey(DatenFelder d) {
    String k = d.get(key);
    if (!d.get(VEREIN).equals("")) k = k+" ("+d.get(VEREIN)+")";
    return k;
  }


  public static String getDetailBezeichnung(String tBoatType, String tNumSeats, String tCoxing) {
    return International.getMessage("{boattype} {numseats} {coxedornot}",
            Daten.efaTypes.getValue(EfaTypes.CATEGORY_BOAT,     tBoatType),
            Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, tNumSeats),
            Daten.efaTypes.getValue(EfaTypes.CATEGORY_COXING,   tCoxing));
  }


  public static String getDetailBezeichnung(DatenFelder boot) {
    if (boot == null || Daten.efaTypes == null) return null;
    return getDetailBezeichnung(boot.get(Boote.ART), boot.get(Boote.ANZAHL), boot.get(Boote.STM));
  }

  public static String getGeneralNumberOfSeatsType(DatenFelder boot) {
      if (boot == null) return null;
      String numSeats = boot.get(Boote.ANZAHL);
      if (numSeats.equals(EfaTypes.TYPE_NUMSEATS_2X)) { return EfaTypes.TYPE_NUMSEATS_2; }
      if (numSeats.equals(EfaTypes.TYPE_NUMSEATS_4X)) { return EfaTypes.TYPE_NUMSEATS_4; }
      if (numSeats.equals(EfaTypes.TYPE_NUMSEATS_6X)) { return EfaTypes.TYPE_NUMSEATS_6; }
      if (numSeats.equals(EfaTypes.TYPE_NUMSEATS_8X)) { return EfaTypes.TYPE_NUMSEATS_8; }
      return numSeats;
  }

  public static String getGeneralNumberOfSeatsValue(DatenFelder boot) {
      return Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, getGeneralNumberOfSeatsType(boot));
  }

  // Einträge auf Gültigkeit prüfen
  public void validateValues(DatenFelder d) {
    String s;
    if ( (s = d.get(NAME)).indexOf("[")>=0 ) d.set(NAME, EfaUtil.replace(s,"[","",true) );
    if ( (s = d.get(NAME)).indexOf("]")>=0 ) d.set(NAME, EfaUtil.replace(s,"]","",true) );
    if ( (s = d.get(VEREIN)).indexOf("[")>=0 ) d.set(VEREIN, EfaUtil.replace(s,"[","",true) );
    if ( (s = d.get(VEREIN)).indexOf("]")>=0 ) d.set(VEREIN, EfaUtil.replace(s,"]","",true) );
  }

  public static String makeGruppen(String gruppe1, String gruppe2, String gruppe3, String gruppe4, String gruppe5) {
    String g = "";
    if (gruppe1.trim().length()>0) g += (g.length()>0 ? "; " : "") + EfaUtil.removeSepFromString(EfaUtil.removeSepFromString(gruppe1.trim(),";"));
    if (gruppe2.trim().length()>0) g += (g.length()>0 ? "; " : "") + EfaUtil.removeSepFromString(EfaUtil.removeSepFromString(gruppe2.trim(),";"));
    if (gruppe3.trim().length()>0) g += (g.length()>0 ? "; " : "") + EfaUtil.removeSepFromString(EfaUtil.removeSepFromString(gruppe3.trim(),";"));
    if (gruppe4.trim().length()>0) g += (g.length()>0 ? "; " : "") + EfaUtil.removeSepFromString(EfaUtil.removeSepFromString(gruppe4.trim(),";"));
    if (gruppe5.trim().length()>0) g += (g.length()>0 ? "; " : "") + EfaUtil.removeSepFromString(EfaUtil.removeSepFromString(gruppe5.trim(),";"));
    return g;
  }

  public static Vector getGruppen(DatenFelder d) {
    Vector v = EfaUtil.split(d.get(Boote.GRUPPEN),';');
    for (int i=0; i<v.size(); i++) v.set(i,((String)v.get(i)).trim());
    return v;
  }

  // Dateiformat überprüfen, ggf. konvertieren
  public boolean checkFileFormat() {
    String s;
    try {
      s = freadLine();
      if ( s == null || !s.trim().startsWith(kennung) ) {

        // KONVERTIEREN: 170 -> 190
        if (s != null && s.trim().startsWith(KENNUNG170)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"170");
          iniList(this.dat,12,1,false); // Rahmenbedingungen von v1.9.0 schaffen
          // Datei lesen
          try {
            while ((s = freadLine()) != null) {
              s = s.trim();
              if (s.equals("") || s.startsWith("#")) continue; // Kommentare ignorieren
              DatenFelder d = constructFields(s);
              String numseats = d.get(ANZAHL);
              if (d.get(ANZAHL).equals("andere")) { numseats = EfaTypes.TYPE_NUMSEATS_OTHER; }
              if (d.get(RIGGER).equals("Skull")) {
                  if (d.get(ANZAHL).equals("1er")) { numseats = EfaTypes.TYPE_NUMSEATS_1; }
                  if (d.get(ANZAHL).equals("2er")) { numseats = EfaTypes.TYPE_NUMSEATS_2X; }
                  if (d.get(ANZAHL).equals("3er")) { numseats = EfaTypes.TYPE_NUMSEATS_3; }
                  if (d.get(ANZAHL).equals("4er")) { numseats = EfaTypes.TYPE_NUMSEATS_4X; }
                  if (d.get(ANZAHL).equals("5er")) { numseats = EfaTypes.TYPE_NUMSEATS_5; }
                  if (d.get(ANZAHL).equals("6er")) { numseats = EfaTypes.TYPE_NUMSEATS_6X; }
                  if (d.get(ANZAHL).equals("8er")) { numseats = EfaTypes.TYPE_NUMSEATS_8X; }
              } else if (d.get(RIGGER).equals("Riemen")) {
                  if (d.get(ANZAHL).equals("2er")) { numseats = EfaTypes.TYPE_NUMSEATS_2; }
                  if (d.get(ANZAHL).equals("4er")) { numseats = EfaTypes.TYPE_NUMSEATS_4; }
                  if (d.get(ANZAHL).equals("6er")) { numseats = EfaTypes.TYPE_NUMSEATS_6; }
                  if (d.get(ANZAHL).equals("8er")) { numseats = EfaTypes.TYPE_NUMSEATS_8; }
              } else {
                  if (d.get(ANZAHL).equals("1er")) { numseats = EfaTypes.TYPE_NUMSEATS_1; }
                  if (d.get(ANZAHL).equals("2er")) { numseats = EfaTypes.TYPE_NUMSEATS_2; }
                  if (d.get(ANZAHL).equals("3er")) { numseats = EfaTypes.TYPE_NUMSEATS_3; }
                  if (d.get(ANZAHL).equals("4er")) { numseats = EfaTypes.TYPE_NUMSEATS_4; }
                  if (d.get(ANZAHL).equals("5er")) { numseats = EfaTypes.TYPE_NUMSEATS_5; }
                  if (d.get(ANZAHL).equals("6er")) { numseats = EfaTypes.TYPE_NUMSEATS_6; }
                  if (d.get(ANZAHL).equals("8er")) { numseats = EfaTypes.TYPE_NUMSEATS_8; }
              }
              String art = Daten.efaTypes.getTypeForValue(EfaTypes.CATEGORY_BOAT, d.get(ART));
              String anz = numseats;
              String rig = Daten.efaTypes.getTypeForValue(EfaTypes.CATEGORY_RIGGING, d.get(RIGGER));
              String stm = Daten.efaTypes.getTypeForValue(EfaTypes.CATEGORY_COXING, d.get(STM));
              if (art == null && d.get(ART).equals("Skiff")) {
                  art = EfaTypes.TYPE_BOAT_RACING;
              }
              if (art == null) {
                  art = EfaTypes.TYPE_BOAT_OTHER;
                  /* @efa1
                  Logger.log(Logger.ERROR, Logger.MSG_CSVFILE_ERRORCONVERTING,
                          getFileName() + ": " +
                          International.getMessage("Fehler beim Konvertieren von Eintrag '{key}'!",constructKey(d)) + " " +
                          International.getMessage("Unbekannte Eigenschaft '{original_property}' korrigiert zu '{new_property}'.",
                          d.get(ART), Daten.efaTypes.getValue(EfaTypes.CATEGORY_BOAT, art)));
                   */
              }
              if (anz == null) {
                  anz = EfaTypes.TYPE_NUMSEATS_OTHER;
                  /* @efa1
                  Logger.log(Logger.ERROR, Logger.MSG_CSVFILE_ERRORCONVERTING,
                          getFileName() + ": " +
                          International.getMessage("Fehler beim Konvertieren von Eintrag '{key}'!",constructKey(d)) + " " +
                          International.getMessage("Unbekannte Eigenschaft '{original_property}' korrigiert zu '{new_property}'.",
                          d.get(ANZAHL), Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, anz)));
                   */
              }
              if (rig == null) {
                  rig = EfaTypes.TYPE_RIGGING_OTHER;
                  /* @efa12
                  Logger.log(Logger.ERROR, Logger.MSG_CSVFILE_ERRORCONVERTING,
                          getFileName() + ": " +
                          International.getMessage("Fehler beim Konvertieren von Eintrag '{key}'!",constructKey(d)) + " " +
                          International.getMessage("Unbekannte Eigenschaft '{original_property}' korrigiert zu '{new_property}'.",
                          d.get(RIGGER), Daten.efaTypes.getValue(EfaTypes.CATEGORY_RIGGING, rig)));
                   */
              }
              if (stm == null) {
                  stm = EfaTypes.TYPE_COXING_OTHER;
                  /* @efa1
                  Logger.log(Logger.ERROR, Logger.MSG_CSVFILE_ERRORCONVERTING,
                          getFileName() + ": " +
                          International.getMessage("Fehler beim Konvertieren von Eintrag '{key}'!",constructKey(d)) + " " +
                          International.getMessage("Unbekannte Eigenschaft '{original_property}' korrigiert zu '{new_property}'.",
                          d.get(STM), Daten.efaTypes.getValue(EfaTypes.CATEGORY_COXING, stm)));
                   */
              }
              d.set(ART, art);
              d.set(ANZAHL, anz);
              d.set(RIGGER, rig);
              d.set(STM, stm);
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
