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

import de.nmichael.efa.util.EfaUtil;
import java.util.Hashtable;

// @i18n complete

// Ein Element von SortedList
class Element {
  public String key;
  public Object value;
  public Element next,prev;

  // Konstruktor: String-Key "k" und restliche Daten "v"
  public Element(String k, Object v) {
    key = k;
    value = v;
    next = prev = null;
  }

}





public class SortedList {

  private Element head;        // erstes Element (Dummy-Element)
  private Element tail;        // letztes Element
  private Element lastElement; // letztes bei Suche geliefertes Element
  private String lastString;   // Suchmuster der letzten Suche
  private boolean numeric;     // Key-Feld als numerischen Wert betrachten
  private boolean ignoreCase=false;  // Groß- und Kleinschreibung ignorieren
  private Hashtable hash;      // Hashtable für schnelleren Zugriff auf "Element"-Elemente
  private Element lastElementSaved=null; // gespeichertes letztes Element zum Wiederherstellen


  // Konstruktor
  public SortedList(boolean num) {
    head = tail = new Element("",null); // Dummy-Element
    lastElement = null;
    lastString = "";
    numeric = num;
    hash = new Hashtable();
  }


  // Liste löschen
  public void clear() {
    // Referenzen löschen (ggf. sonst memory leak)
    for (Element e = head; e != null; e = e.next) {
      if (e.prev != null) e.prev.next = null;
      e.prev = null;
    }

    head = tail = new Element("",null); // neues Dummy-Element
    lastElement = null;
    lastString = "";
    hash = new Hashtable();
  }


  // Eintrag zur Liste hinzufügen
  // @returns true, wenn das eingefügte Element als letztes Element der Liste hinzugefügt wurde
  public boolean put(String k, Object v) {
    Element e = new Element(k,v);
    Element c = tail;

    // an der richtigen Stelle einfügen
    // von hinten beginnen --> bessere Performance, da Daten i.d.R. bereits sortiert vorliegen!!
    if (!numeric) // Key als String betrachten
      while (c.prev != null && e.key.compareTo(c.key)<0) c = c.prev;
    else          // Key als int-Wert betrachten
      while (c.prev != null && EfaUtil.compareIntString(e.key,c.key) < 0) c = c.prev;

    if ( (!numeric && e.key.compareTo(c.key)==0) || (numeric && EfaUtil.compareIntString(e.key,c.key)==0) ) {
      // Elemente sind gleich: Element ersetzen!
      e.next = c.next;
      e.prev = c.prev;
      if (c.next != null) c.next.prev = e;
      if (c.prev != null) c.prev.next = e;
    } else {
      // ungleiches Element einfügen
      e.next = c.next;
      e.prev = c;
      if (c.next != null) c.next.prev = e;
      c.next = e;
    }
    if (e.next == null) tail = e;

    hash.put(k,e);

    return tail == e; // true, wenn eingefügtes Element das letzte Element ist
  }


  // Element s löschen;
  // true, wenn erfolgreich; sonst false
  public boolean delete(String k) {
    Element c = head;

    // Element suchen
    while (c.next != null && !k.equals(c.key)) c = c.next;

    // Element löschen
    if (c.key.equals(k)) {
      if (c == tail) tail = c.prev;
      c.prev.next = c.next;
      if (c.next != null) c.next.prev = c.prev;
      hash.remove(k);
      return true;
    }
    return false;
  }


  // Anzahl der Elemente ermitteln
  public int countElements() {
    /*
    Element c = head;
    int i = 0;
    while (c.next != null) {
      c = c.next;
      i++;
    }
    return i;
    */
    return hash.size();
  }


  // Element suchen, das mit "such" anfängt, beginnend mit Element "e"
  // "vorwaerts" bestimmt die Suchrichtung, "nurAnfang" bedeutet, daß es genügt,
  // wenn der Treffer entsprechend anfängt
  public String search(Element e, String such, boolean vorwaerts, boolean nurAnfang) {
    if (ignoreCase) such = such.toLowerCase();
    while (e != null) {
      if (ignoreCase) {
        if ((e.key.toLowerCase().startsWith(such) && nurAnfang) || (e.key.toLowerCase().equals(such) && !nurAnfang)) {
  	  lastElement = e;
  	  return e.key;
        }
      } else {
        if ((e.key.startsWith(such) && nurAnfang) || (e.key.equals(such) && !nurAnfang)) {
	  lastElement = e;
	  return e.key;
        }
      }
      if (vorwaerts) e = e.next;
      else e = e.prev;
    }
    return null;
  }


  // erstes Element suchen, das mit "such" anfängt
  // wie startsWith(such), nur daß "such" nicht als letzter Suchbegriff nicht gespeichert wird und somit
  // kein Weitersuchen möglich ist, aber insb. auch nicht das nächste Weitersuchen durch diesen Ruf beeinflußt wird
  public String selectStartsWith(String such) {
    Element e = head.next;
    return search(e,such,true,true);
  }

  // erstes Element suchen, das mit "such" anfängt
  public String startsWith(String such) {
    Element e = head.next;
    lastString = such;  // für's Weitersuchen nötig
    lastElement = null; // für's Weitersuchen nötig
    return search(e,such,true,true);
  }


  // nächstes Element suchen, das mit "such" anfängt
  public String nextStartsWith(String such) {
    if (lastElement == null || !lastString.equals(such)) return startsWith(such);
    Element e = lastElement.next;
    return search(e,such,true,true);
  }


  // vorheriges Element suchen, das mit "such" anfängt
  public String prevStartsWith(String such) {
    if (lastElement == null || !lastString.equals(such)) return startsWith(such);
    Element e = lastElement.prev;
    return search(e,such,false,true);
  }


  // letztes Element suchen, das mit "such" anfängt
  public String lastStartsWith(String such) {
    Element e = tail;
    lastString = such;  // für's Weitersuchen nötig
    lastElement = null; // für's Weitersuchen nötig
    return search(e,such,false,true);
  }


  // nächstes Element suchen
  public String next() {
    if (lastElement == null) lastElement = head.next;
    else lastElement = lastElement.next;
    if (lastElement == null) lastElement = tail;
    if (lastElement != null) return lastElement.key;
    return null;
  }


  // vorheriges Element suchen
  public String prev() {
    if (lastElement == null) lastElement = tail;
    else lastElement = lastElement.prev;
    if (lastElement == null || lastElement == head) lastElement = head.next;
    if (lastElement != null) return lastElement.key;
    return null;
  }


  // ohne Suchkriterien das nächste Element aus der Liste liefern
  // (um einzeln der Reihe nach alle Elemente auszugeben)
  public String get() {
    if (!lastString.equals("")) lastElement = null;
    lastString = "";
    if (lastElement == null) lastElement = head;
    lastElement = lastElement.next;
    if (lastElement != null) {
      return lastElement.key;
    }
    return null;
  }


  // aktuellen Datensatz komplett liefern
  public Object getComplete() {
    if (lastElement == null) return null;
    return lastElement.value;
  }


  // ersten Datensatz komplett liefern
  public Object getCompleteFirst() {
    lastElement = null;
    return getCompleteNext();
  }


  // komplette Datensätze der Reihe nach liefern - vorwärts
  public Object getCompleteNext() {
//    if (!lastString.equals("")) lastElement = null; // ????
    lastString = "";
    if (lastElement == null) lastElement = head;
    lastElement = lastElement.next;
    if (lastElement != null) {
      return lastElement.value;
    }
    return null;
  }


  // komplette Datensätze der Reihe nach liefern - rückwärts
  public Object getCompletePrev() {
//    if (!lastString.equals("")) lastElement = null; // ????
    lastString = "";
    if (lastElement == null) lastElement = head.next; // v0.81: ... = head;
    lastElement = lastElement.prev; // v0.81: ohne if
    if (lastElement != null && lastElement != head) {
      return lastElement.value;
    }
    return null;
  }


  // den letzten Datensatz komplett liefern
  public Object getCompleteLast() {
    lastElement = tail;
    lastString = "";
    if (tail == head) return null;
    else return lastElement.value;
  }


  // einen exakten Treffer liefern
  public String getExact(String such) {
    lastString = such;  // für's Weitersuchen nötig
    lastElement = null; // für's Weitersuchen nötig

    Element h = (Element)hash.get(such);
    if (h != null) {
      lastElement = h;
      return such;
    }

    Element e = head.next;
    return search(e,such,true,false);
  }


  // zu einem bestimmten Datensatz springen
  public void goTo(String such) {
    startsWith(such);
    lastString = "";
  }


    // Suchstrings löschen, damit nächste Suche eine neue Suche ist
  public void clearSearch() {
    lastString = "";
    lastElement = null;
  }



  // legt fest, ob Groß- und Kleinschreibung beim Suchen ignoriert werden soll
  public void ignoreCase(boolean ic) {
    this.ignoreCase = ic;
  }


  // gibt zurück, ob die Liste leer ist
  public boolean isEmpty() {
    return (head == tail);
  }


  // speichere lastElement, um es später mit restoreLastElement wiederherzustellen
  public void saveLastElement() {
    lastElementSaved = lastElement;
  }


  // stelle den Wert von lastElement, wie er zuvor mittels saveLastElement gesichert wurde, wieder her
  public void restoreLastElement() {
    lastElement = lastElementSaved;
  }


}
