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

// @i18n complete

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.gui.dataedit.DataEditDialog;
import de.nmichael.efa.data.storage.*;
import javax.swing.*;

public interface IItemListenerDataRecordTable {

    public void itemListenerActionTable(int actionId, DataRecord[] records);
    public boolean deleteCallback(DataRecord[] records);
    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record);

}
