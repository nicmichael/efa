/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.drv;

import java.util.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class SortedStatistic {

  private Vector data = null;
  private SortedItem[] arr = null;
  public static boolean ASCENDING = true;

  public SortedStatistic() {
    data = new Vector();
  }

  public void add(int sortkrit_i, String sortkrit_s, String field1, String field2, String field3) {
    data.add(new SortedItem(sortkrit_i,sortkrit_s,field1,field2,field3));
  }

  public void sort(boolean ascending) {
    arr = new SortedItem[data.size()];
    for (int i=0; i<data.size(); i++) {
      arr[i] = (SortedItem)data.get(i);
    }
    ASCENDING = ascending;
    Arrays.sort(arr);
  }

  public int sortedSize() {
    if (arr != null) return arr.length;
    return -1;
  }

  public String[] getSorted(int i) {
    return arr[i].data;
  }

}

class SortedItem implements Comparable {
  private int sortkrit_i;
  private String sortkrit_s;
  public String[] data;

  public SortedItem(int sortkrit_i, String sortkrit_s, String field1, String field2, String field3) {
    this.sortkrit_i = sortkrit_i;
    this.sortkrit_s = sortkrit_s;
    data = new String[3];
    data[0] = field1;
    data[1] = field2;
    data[2] = field3;
  }

  public int compareTo(Object o) throws ClassCastException {
    SortedItem b = (SortedItem)o;

    if (SortedStatistic.ASCENDING) {
      if (this.sortkrit_i != -1 && b.sortkrit_i != -1) return this.sortkrit_i - b.sortkrit_i;
      return (this.sortkrit_s.compareTo(b.sortkrit_s));
    } else {
      if (this.sortkrit_i != -1 && b.sortkrit_i != -1) return b.sortkrit_i - this.sortkrit_i;
      return (b.sortkrit_s.compareTo(this.sortkrit_s));
    }
  }


}