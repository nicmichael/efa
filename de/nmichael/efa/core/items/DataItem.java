/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import de.nmichael.efa.data.storage.DataKey;

public class DataItem {

    public DataKey key = null;      // the key of the record
    public IItemType item = null;   // the item itself: a "column" within that record
    public boolean changed = false;

    public DataItem() {
    }

    public DataItem(DataKey key, IItemType item) {
        this.key = key;
        this.item = item;
    }

}
