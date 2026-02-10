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
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.UUID;

import javax.swing.*;


// @i18n complete
public class BoatStatusListDialog extends DataListDialog {

    public BoatStatusListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Bootsstatus"),
                Daten.project.getBoatStatus(false), -1, admin);
        actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_EDIT };
        actionType = new int[] { ItemTypeDataRecordTable.ACTION_EDIT };
        actionImage = new String[] { BaseDialog.IMAGE_EDIT };
        intelligentColumnWidth = false;
        iniValues();
    }

    public BoatStatusListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Bootsstatus"),
                Daten.project.getBoatStatus(false), -1, admin);
        actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_EDIT };
        actionType = new int[] { ItemTypeDataRecordTable.ACTION_EDIT };
        actionImage = new String[] { BaseDialog.IMAGE_EDIT };
        intelligentColumnWidth = false;
        iniValues();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        if (record == null) {
            return null;
        }
        return new BoatStatusEditDialog(parent, (BoatStatusRecord)record, false, admin);
    }
    
	protected void createSpecificItemTypeRecordTable() {
		
		super.createSpecificItemTypeRecordTable();

		table.addPermanentSecondarySortingColumn(BoatStatusRecord.COLUMN_ID_BOAT_BASE_STATUS);
		table.addPermanentSecondarySortingColumn(BoatStatusRecord.COLUMN_ID_BOAT_CURRENT_STATUS);
		table.addPermanentSecondarySortingColumn(BoatStatusRecord.COLUMN_ID_BOAT_NAME);        
		table.addPermanentSecondarySortingColumn(BoatStatusRecord.COLUMN_ID_BOAT_SESSION_LOGBOOK);

	}    
	
    private void iniValues() {
    	//From and to columns should be wider than default
		//Buttons on north, if boat reservations are called from efaBths main screen.
		this.buttonPanelPosition =  BorderLayout.NORTH;
		this.minColumnWidths = new int[] {150,150,150,120,120,120,-1};   
    }
    
}
