/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.util;

import java.util.GregorianCalendar;

// @i18n complete

public class TMJ implements Cloneable {

  public int tag,monat,jahr;

  public TMJ(int t, int m, int j) {
    tag = t;
    monat = m;
    jahr = j;
  }

  public GregorianCalendar toCalendar() {
    return new GregorianCalendar(jahr,monat-1,tag);
  }
  
  public static TMJ parseTMJ(String s) {
      return EfaUtil.string2date(s, -1, -1, -1);
  }

  public String toString() {
      return tag + "." + monat + "." + jahr;
  }



}
