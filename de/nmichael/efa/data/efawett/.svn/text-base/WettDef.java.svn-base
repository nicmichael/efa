/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.efawett;

import de.nmichael.efa.util.TMJ;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class WettDef {
  public int wettid=-1;
  public String name=null;
  public String kurzname=null;
  public String key=null;
  public int gueltig_von=-1;
  public int gueltig_bis=-1;
  public int gueltig_prio=0;
  public TMJ von=null, bis=null; // von/bis.jahr wird relativ zu StatistikDaten.wettJahr angegeben: Bsp: DRV: 0/0; LRV Winter: 0/1 usw.
  public WettDefGruppe[] gruppen = null;
}
