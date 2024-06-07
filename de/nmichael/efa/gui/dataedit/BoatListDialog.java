/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import de.nmichael.efa.*;
import de.nmichael.efa.core.config.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.util.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


// @i18n complete
public class BoatListDialog extends DataListDialog {

    public BoatListDialog(Frame parent, long validAt, AdminRecord admin) {
        super(parent, International.getString("Boote"), Daten.project.getBoats(false), validAt, admin);
        if (admin != null && admin.isAllowedEditBoats()) {
            addMergeAction();
        }
    }

    public BoatListDialog(JDialog parent, long validAt, AdminRecord admin) {
        super(parent, International.getString("Boote"), Daten.project.getBoats(false), validAt, admin);
        if (admin != null && admin.isAllowedEditBoats()) {
            addMergeAction();
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null) {
            record = Daten.project.getBoats(false).createBoatRecord(UUID.randomUUID());
            ((BoatRecord)record).addTypeVariant("", EfaTypes.TYPE_BOAT_OTHER, EfaTypes.TYPE_NUMSEATS_OTHER, 
                    EfaTypes.TYPE_RIGGING_OTHER, EfaTypes.TYPE_COXING_OTHER, Boolean.toString(true));
        }
        return new BoatEditDialog(parent, (BoatRecord)record, newRecord, admin);
    }

    protected ProgressTask getMergeProgressTask(DataKey mainKey, DataKey[] mergeKeys) {
        Boats boats = (Boats)persistence;
        return boats.getMergeBoatsProgressTask(mainKey, mergeKeys);
    }
    
	protected void createSpecificItemTypeRecordTable() {
		
		super.createSpecificItemTypeRecordTable();
    
		table.addPermanentSecondarySortingColumn(BoatRecord.COLUMN_ID_BOAT_TYPE);
		table.addPermanentSecondarySortingColumn(BoatRecord.COLUMN_ID_BOAT_OWNER);
		table.addPermanentSecondarySortingColumn(BoatRecord.COLUMN_ID_BOAT_NAME);    
	}        

}
