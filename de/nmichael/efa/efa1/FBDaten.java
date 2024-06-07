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

// @i18n complete

// Fahrtenbuch-Zusatzdaten (individuell für jedes FB verwaltet!)
public class FBDaten {

  public String bootDatei;        // Boote-Dateiname
  public String mitgliederDatei;  // Mitglieder-Dateiname
  public String zieleDatei;       // Ziele-Dateiname
  public String statistikDatei;   // gespeicherte Statistikeinstellungen
  public Boote boote;             // Boote
  public Mitglieder mitglieder;   // Mitglieder
  public Ziele ziele;             // Ziele
  public StatSave statistik;      // gespeicherte Statistikeinstellungen
  public boolean erstVorname;     // "Vorname Nachname" oder "Nachname, Vorname"
  public String[] status;         // Liste von "Status"
  public int anzMitglieder;       // Anzahl der Mitglieder am 01.01. des Jahres

  public FBDaten() {
    bootDatei="";
    mitgliederDatei="";
    zieleDatei="";
    statistikDatei="";
    boote=null;
    mitglieder=null;
    ziele=null;
    statistik=null;
    erstVorname=true;
    status = new String[2];
    status[0] = Daten.efaTypes.getValue(EfaTypes.CATEGORY_STATUS, EfaTypes.TYPE_STATUS_GUEST);
    status[1] = Daten.efaTypes.getValue(EfaTypes.CATEGORY_STATUS, EfaTypes.TYPE_STATUS_OTHER);
    anzMitglieder=0;
  }

  public FBDaten(FBDaten d) {
    bootDatei = d.bootDatei;
    mitgliederDatei = d.mitgliederDatei;
    zieleDatei = d.zieleDatei;
    statistikDatei = d.statistikDatei;
    boote = d.boote;
    mitglieder = d.mitglieder;
    ziele = d.ziele;
    statistik = d.statistik;
    erstVorname = d.erstVorname;
    status = d.status;
    anzMitglieder = d.anzMitglieder;
  }

}
