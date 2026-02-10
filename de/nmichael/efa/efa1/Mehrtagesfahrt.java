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
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.core.config.*;

// @i18n complete

public class Mehrtagesfahrt {

  private String displayName;
  public String name;
  public String start;
  public String ende;
  public int rudertage;
  public String gewaesser;
  public boolean isEtappen;

  public Mehrtagesfahrt(String name, String start, String ende, int rudertage, String gewaesser, boolean isEtappen) {
    this.name = EfaUtil.removeSepFromString(name);
    this.start = EfaUtil.removeSepFromString(start);
    this.ende = EfaUtil.removeSepFromString(ende);
    this.rudertage = rudertage;
    this.gewaesser = EfaUtil.removeSepFromString(gewaesser);
    this.isEtappen = isEtappen;
    this.displayName = this.name; // + " ("+ this.start + " bis " + this.ende + ")";
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public static String getNameFromDisplayName(String displayName) {
    return displayName; // zur Zeit derselbe Name wie der wirkliche
  }

  // prüft, ob es sich bei "s" um eine vordefinierte Fahrtart handelt, d.h. eine, die in dem Daten.fahrtart-Array steht
  public static boolean isVordefinierteFahrtart(String s) {
    if (Daten.efaTypes == null) return false;
    String[] types = Daten.efaTypes.getTypesArray(EfaTypes.CATEGORY_SESSION);
    for (int i=0; i<types.length; i++) {
        if (types[i].equals(s)) {
            return true;
        }
    }
    return false;
  }


}