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
import java.util.Vector;
import java.io.*;

// @i18n complete

public class Gruppen extends DatenListe {

  public static final int GRUPPE = 0;
  public static final int VORNAME = 1;
  public static final int NACHNAME = 2;
  public static final int VEREIN = 3;
  public static final int _ANZ_FELDER = 4;

  public static final String KENNUNG170 = "##EFA.170.GRUPPEN##";
  public static final String KENNUNG190 = "##EFA.190.GRUPPEN##";

  // Konstruktor
  public Gruppen(String pdat) {
    super(pdat,_ANZ_FELDER,1,false);
    kennung = KENNUNG190;
  }

  // Key-Wert ermitteln
  public String constructKey(DatenFelder d) {
    return d.get(GRUPPE)+"#"+d.get(NACHNAME)+"#"+d.get(VORNAME)+"#"+d.get(VEREIN);
  }

  public Vector getGruppen() {
    Vector v = new Vector();
    DatenFelder d = getCompleteFirst();
    while (d != null) {
      if (!v.contains(d.get(GRUPPE))) v.add(d.get(GRUPPE));
      d = getCompleteNext();
    }
    return v;
  }

  public Vector getGruppenMitglieder(String gruppe) {
    Vector v = new Vector();
    DatenFelder d = getCompleteFirst();
    while (d != null) {
      if (d.get(GRUPPE).equals(gruppe)) {
        GruppenMitglied m = new GruppenMitglied(d.get(VORNAME),d.get(NACHNAME),d.get(VEREIN));
        v.add(m);
      }
      d = getCompleteNext();
    }
    if (v.size() == 0) return null;
    return v;
  }

  public void setGruppenMitglieder(String gruppe, Vector mitglieder) {
    for (int i=0; i<mitglieder.size(); i++) {
      GruppenMitglied m = (GruppenMitglied)mitglieder.get(i);
      DatenFelder d = new DatenFelder(_ANZ_FELDER);
      d.set(GRUPPE,gruppe);
      d.set(VORNAME,m.vorname);
      d.set(NACHNAME,m.nachname);
      d.set(VEREIN,m.verein);
      delete(constructKey(d));
      add(d);
    }
  }

  public void deleteGruppenMitglied(String gruppe, GruppenMitglied mitglied) {
    Vector m = getGruppenMitglieder(gruppe);
    if (m == null) return;
    for (int i=0; i<m.size(); i++) {
      GruppenMitglied gm = (GruppenMitglied)m.get(i);
      if (gm.vorname != null && gm.vorname.equals(mitglied.vorname) &&
          gm.nachname != null && gm.nachname.equals(mitglied.nachname) &&
          gm.verein != null && gm.verein.equals(mitglied.verein)) {
        m.remove(i);
        i--;
      }
    }
    deleteGruppe(gruppe);
    setGruppenMitglieder(gruppe,m);
  }

  public void addGruppenMitglied(String gruppe, GruppenMitglied mitglied) {
    Vector m = getGruppenMitglieder(gruppe);
    if (m == null) m = new Vector();
    for (int i=0; i<m.size(); i++) {
      GruppenMitglied gm = (GruppenMitglied)m.get(i);
      if (gm.vorname != null && gm.vorname.equals(mitglied.vorname) &&
          gm.nachname != null && gm.nachname.equals(mitglied.nachname) &&
          gm.verein != null && gm.verein.equals(mitglied.verein)) {
        return; // ist bereits Mitglied
      }
    }
    m.add(mitglied);
    setGruppenMitglieder(gruppe,m);
  }

  public void deleteGruppe(String gruppe) {
    Vector delete = new Vector();
    DatenFelder d = getCompleteFirst();
    while (d != null) {
      if (d.get(GRUPPE).equals(gruppe)) {
        delete.add(constructKey(d));
      }
      d = getCompleteNext();
    }
    for (int i=0; i<delete.size(); i++) {
      this.delete((String)delete.get(i));
    }
  }

  public boolean isInGroup(String gruppe, String vorname, String nachname, String verein) {
    return getExact(gruppe+"#"+nachname+"#"+vorname+"#"+verein) != null;
  }

  public boolean checkFileFormat() {
    String s;
    try {
      s = freadLine();
      if ( s == null || !s.trim().startsWith(kennung) ) {
        // KONVERTIEREN: 170 -> 190
        if (s != null && s.trim().startsWith(KENNUNG170)) {
          // @efa1 if (Daten.backup != null) Daten.backup.create(dat,Efa1Backup.CONV,"170");
          iniList(this.dat,4,1,false); // Rahmenbedingungen von v1.9.0 schaffen
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
