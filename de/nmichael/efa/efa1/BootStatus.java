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
import java.util.*;
import java.io.IOException;

// @i18n complete


public class BootStatus extends DatenListe {

  public static final int _FELDANZ = 7;

  public static final int NAME = 0;
  public static final int STATUS = 1;
  public static final int LFDNR = 2;
  public static final int BEMERKUNG = 3;
  public static final int UNBEKANNTESBOOT = 4;
  public static final int RESERVIERUNGEN = 5;
  public static final int BOOTSSCHAEDEN = 6;                // neu in 1.6.0

  public static final int STAT_HIDE = 0;
  public static final int STAT_VERFUEGBAR = 1;
  public static final int STAT_UNTERWEGS = 2;
  public static final int STAT_NICHT_VERFUEGBAR = 3;
  public static final int STAT_VORUEBERGEHEND_VERSTECKEN = 4; // wird intern für Kombiboote verwendet
  private static final String[] STATUSKEYS  = { "HIDE", "AVAILABLE", "ONTHEWATER", "NOTAVAILABLE", "CURRENTLYHIDDEN" };
  private static String[] STATUSDESCR;

  public static final String KENNUNG120 = "##EFA.120.BOOTSTATUS##";
  public static final String KENNUNG160 = "##EFA.160.BOOTSTATUS##";
  public static final String KENNUNG170 = "##EFA.170.BOOTSTATUS##";
  public static final String KENNUNG190 = "##EFA.190.BOOTSTATUS##";

  public static final String RES_LFDNR = "RES";


  // Konstruktor
  public BootStatus(String pdat) {
    super(pdat,_FELDANZ,1,false);
    if (STATUSDESCR == null) {
        STATUSDESCR = new String[STATUSKEYS.length];
        STATUSDESCR[STAT_HIDE]                      = "nicht anzeigen";
        STATUSDESCR[STAT_VERFUEGBAR]                = "verfügbar";
        STATUSDESCR[STAT_UNTERWEGS]                 = "unterwegs";
        STATUSDESCR[STAT_NICHT_VERFUEGBAR]          = "nicht verfügbar";
        STATUSDESCR[STAT_VORUEBERGEHEND_VERSTECKEN] = "vorübergehend verstecken";
    }
    kennung = KENNUNG190;
  }


  public Vector getBoote(int status) {
    Vector v = new Vector();
    for (DatenFelder d = (DatenFelder)this.getCompleteFirst(); d != null; d = (DatenFelder)this.getCompleteNext()) {
      if (getStatusID(d.get(STATUS)) == status) v.add(d.get(NAME));
    }
    return v;
  }

  public static Vector getReservierungen(DatenFelder boot, int version) {
    Vector v = new Vector();
    String s = boot.get(RESERVIERUNGEN);
    if (s == null) return v;
    StringTokenizer tok = new StringTokenizer(s,";");
    int pos=0;
    BoatReservation r = null;
    while (tok.hasMoreTokens()) {
      if (pos == 0) {
        r = new BoatReservation();
        r.setOneTimeReservation(true);
      }
      switch(pos) {
        case 0: r.setDateFrom(tok.nextToken()); break;
        case 1: r.setTimeFrom(tok.nextToken()); break;
        case 2: r.setDateTo(tok.nextToken()); break;
        case 3: r.setTimeTo(tok.nextToken()); break;
        case 4: r.setForName(tok.nextToken()); break;
        case 5: r.setReason(tok.nextToken()); break;
        case 6: r.setOneTimeReservation(tok.nextToken().equals("+")); break;
      }
      pos++;
      if ((version == 160 && pos>5) || (version >= 170 && pos>6)) {
          if (version == 170) {
              // do not translate (these are the former keys for version <= 170)
              if (r.getDateFrom().equals("Montag"))     r.setDateFrom(BoatReservation.WEEKDAYKEYS[0]);
              if (r.getDateFrom().equals("Dienstag"))   r.setDateFrom(BoatReservation.WEEKDAYKEYS[1]);
              if (r.getDateFrom().equals("Mittwoch"))   r.setDateFrom(BoatReservation.WEEKDAYKEYS[2]);
              if (r.getDateFrom().equals("Donnerstag")) r.setDateFrom(BoatReservation.WEEKDAYKEYS[3]);
              if (r.getDateFrom().equals("Freitag"))    r.setDateFrom(BoatReservation.WEEKDAYKEYS[4]);
              if (r.getDateFrom().equals("Samstag"))    r.setDateFrom(BoatReservation.WEEKDAYKEYS[5]);
              if (r.getDateFrom().equals("Sonntag"))    r.setDateFrom(BoatReservation.WEEKDAYKEYS[6]);
              if (r.getDateTo().equals("Montag"))       r.setDateTo(BoatReservation.WEEKDAYKEYS[0]);
              if (r.getDateTo().equals("Dienstag"))     r.setDateTo(BoatReservation.WEEKDAYKEYS[1]);
              if (r.getDateTo().equals("Mittwoch"))     r.setDateTo(BoatReservation.WEEKDAYKEYS[2]);
              if (r.getDateTo().equals("Donnerstag"))   r.setDateTo(BoatReservation.WEEKDAYKEYS[3]);
              if (r.getDateTo().equals("Freitag"))      r.setDateTo(BoatReservation.WEEKDAYKEYS[4]);
              if (r.getDateTo().equals("Samstag"))      r.setDateTo(BoatReservation.WEEKDAYKEYS[5]);
              if (r.getDateTo().equals("Sonntag"))      r.setDateTo(BoatReservation.WEEKDAYKEYS[6]);
          }
          v.add(r);
          pos = 0;
      }
    }
    return v;
  }

  public static Vector getReservierungen(DatenFelder boot) {
    return getReservierungen(boot,190);
  }

  // gibt die Reservierung zurück, die zum Zeitpunkt now gültig ist oder max. minutesAhead beginnt
  // null, wenn keine Reservierung diese Kriterien erfüllt
  public static BoatReservation getReservierung(DatenFelder boot, long now, long minutesAhead) {
    if (boot == null) return null;
    Vector res = getReservierungen(boot);
    if (res.size() == 0) return null;

    GregorianCalendar cal = new GregorianCalendar();
    cal.setTimeInMillis(now);
    int weekday = cal.get(Calendar.DAY_OF_WEEK);

    for (int i=0; i<res.size(); i++) {
      BoatReservation r = (BoatReservation)res.get(i);
      TMJ vonTag  = EfaUtil.string2date(r.getDateFrom(),0,0,0);
      TMJ vonZeit = EfaUtil.string2date(r.getTimeFrom(),0,0,0);
      TMJ bisTag  = EfaUtil.string2date(r.getDateTo(),0,0,0);
      TMJ bisZeit = EfaUtil.string2date(r.getTimeTo(),0,0,0);
      if (!r.isOneTimeReservation()) {
        switch(weekday) {
          case Calendar.MONDAY:    if (!r.getDateFrom().equals(BoatReservation.WEEKDAYKEYS[0])) continue; break;
          case Calendar.TUESDAY:   if (!r.getDateFrom().equals(BoatReservation.WEEKDAYKEYS[1])) continue; break;
          case Calendar.WEDNESDAY: if (!r.getDateFrom().equals(BoatReservation.WEEKDAYKEYS[2])) continue; break;
          case Calendar.THURSDAY:  if (!r.getDateFrom().equals(BoatReservation.WEEKDAYKEYS[3])) continue; break;
          case Calendar.FRIDAY:    if (!r.getDateFrom().equals(BoatReservation.WEEKDAYKEYS[4])) continue; break;
          case Calendar.SATURDAY:  if (!r.getDateFrom().equals(BoatReservation.WEEKDAYKEYS[5])) continue; break;
          case Calendar.SUNDAY:    if (!r.getDateFrom().equals(BoatReservation.WEEKDAYKEYS[6])) continue; break;
        }
        vonTag.tag   = bisTag.tag   = cal.get(Calendar.DAY_OF_MONTH);
        vonTag.monat = bisTag.monat = cal.get(Calendar.MONTH)+1;
        vonTag.jahr  = bisTag.jahr  = cal.get(Calendar.YEAR);
      }

      long von = EfaUtil.dateTime2Cal(vonTag,vonZeit).getTimeInMillis();
      long bis = EfaUtil.dateTime2Cal(bisTag,bisZeit).getTimeInMillis();

      // ist die vorliegende Reservierung jetzt gültig
      if (now >= von && now <= bis) {
        r.validInMinutes = 0;
        return r;
      }

      // ist die vorliegende Reservierung innerhalb von minutesAhead gültig
      if (now < von && now + minutesAhead*60*1000 >= von) {
        r.validInMinutes = (von-now)/(60*1000);
        return r;
      }
    }
    return null;
  }


  // alle verfallenen Reservierungen löschen;
  // gibt true zurück, falls Reservierungen gelöscht wurden; sonst false.
  public static boolean deleteObsoleteReservierungen(DatenFelder boot) {
    if (boot == null) return false;
    Vector res = getReservierungen(boot);
    if (res.size() == 0) return false;

    long now = System.currentTimeMillis();

    boolean geloscht = false;
    for (int i=0; i<res.size(); i++) {
      BoatReservation r = (BoatReservation)res.get(i);
      if (!r.isOneTimeReservation()) continue; // zyklische Reservierungen werden nicht gelöscht

      TMJ bisTag  = EfaUtil.string2date(r.getDateTo(),0,0,0);
      TMJ bisZeit = EfaUtil.string2date(r.getTimeTo(),0,0,0);
      long bis = EfaUtil.dateTime2Cal(bisTag,bisZeit).getTimeInMillis();

      // ist die vorliegende Reservierung verfallen?
      if (now > bis) {
        res.remove(i);
        i--;
        geloscht = true;
      }
    }
    if (geloscht) setReservierungen(boot,res);
    return geloscht;
  }

  public static void clearReservierungen(DatenFelder boot) {
    boot.set(RESERVIERUNGEN,"");
  }

  public static void addReservierung(DatenFelder boot, BoatReservation r) {
    Vector v = getReservierungen(boot);
    v.add(r);
    setReservierungen(boot,v);
  }

  public static void setReservierungen(DatenFelder boot, Vector v) {
    String s = "";
    BoatReservation[] a = new BoatReservation[v.size()];
    for (int i=0; i<v.size(); i++) a[i] = (BoatReservation)v.get(i);
    Arrays.sort(a);
    for (int i=0; i<a.length; i++) {
      BoatReservation r = a[i];
      s += EfaUtil.removeSepFromString(
             EfaUtil.removeSepFromString(r.getDateFrom(),";")+";"+
             EfaUtil.removeSepFromString(r.getTimeFrom(),";")+";"+
             EfaUtil.removeSepFromString(r.getDateTo(),";")+";"+
             EfaUtil.removeSepFromString(r.getTimeTo(),";")+";"+
             EfaUtil.removeSepFromString(r.getForName(),";")+";"+
             EfaUtil.removeSepFromString(r.getReason(),";")+";"+
             (r.isOneTimeReservation() ? "+" : "-")+";"
           );
    }
    boot.set(RESERVIERUNGEN,s);
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
          iniList(this.dat,7,1,false); // Rahmenbedingungen von v1.9.0 schaffen
          // Datei lesen
          try {
            while ((s = freadLine()) != null) {
              s = s.trim();
              if (s.equals("") || s.startsWith("#")) continue; // Kommentare ignorieren
              
              DatenFelder d = constructFields(s);

              // convert status names to keys
              int status = EfaUtil.string2int(d.get(STATUS),STAT_HIDE);
              d.set(STATUS, STATUSKEYS[status]);
              
              //convert bemerkungen to selected language
              String bemerk = d.get(BEMERKUNG);
              /* @efa1
              if (bemerk.equals("nicht anzeigen"))           bemerk = International.getString("nicht anzeigen");
              if (bemerk.equals("verfügbar"))                bemerk = International.getString("verfügbar");
              if (bemerk.equals("unterwegs"))                bemerk = International.getString("unterwegs");
              if (bemerk.equals("nicht verfügbar"))          bemerk = International.getString("nicht verfügbar");
              if (bemerk.equals("vorübergehend verstecken")) bemerk = International.getString("vorübergehend verstecken");
               */
              d.set(BEMERKUNG, bemerk);

              // convert weekday names to keys
              setReservierungen(d, getReservierungen(d,170));
              
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

  public static int getNumberOfStati() {
      return STATUSKEYS.length;
  }

  public static String getStatusName(int status) {
    if (STATUSDESCR != null && status >= 0 && status < STATUSDESCR.length) return STATUSDESCR[status];
    return International.getString("unbekannt");
  }

  public static String getStatusKey(int status) {
    if (status >= 0 && status < STATUSKEYS.length) return STATUSKEYS[status];
    return null;
  }

  public static int getStatusID(String key) {
      for (int i=0; i<STATUSKEYS.length; i++) {
          if (STATUSKEYS[i].equals(key)) return i;
      }
      return STAT_HIDE;
  }

}
