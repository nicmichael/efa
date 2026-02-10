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
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


// @i18n complete
public class BoatDamageListDialog extends DataListDialog {
	
	protected ItemTypeBoolean showOpenDamagesOnly;

    public BoatDamageListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Bootsschäden"), Daten.project.getBoatDamages(false), 0, admin);
        iniValues(null);
    }

    public BoatDamageListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Bootsschäden"), Daten.project.getBoatDamages(false), 0, admin);
        iniValues(null);
    }

    public BoatDamageListDialog(Frame parent, UUID boatId, AdminRecord admin) {
        super(parent, International.getString("Bootsschäden"), Daten.project.getBoatDamages(false), 0, admin);
        iniValues(boatId);
    }

    public BoatDamageListDialog(JDialog parent, UUID boatId, AdminRecord admin) {
        super(parent, International.getString("Bootsschäden"), Daten.project.getBoatDamages(false), 0, admin);
        iniValues(boatId);
    }

    private void iniValues(UUID boatId) {
        if (boatId != null) {
            this.filterFieldName  = BoatReservationRecord.BOATID;
            this.filterFieldValue = boatId.toString();
        }
        super.sortByColumn = 4;
        
        // Table update: Minimum column widths of 95 pix for the timestamp colums 
        // so they show at least the date part fully readable. 
        this.minColumnWidths = new int[] {150,0,150,150,20};         

    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null && persistence != null && filterFieldValue != null) {
            record = ((BoatDamages)persistence).createBoatDamageRecord(UUID.fromString(filterFieldValue));
        }
        if (record == null) {
            long now = System.currentTimeMillis();
            ItemTypeStringAutoComplete boat = new ItemTypeStringAutoComplete("BOAT", "", IItemType.TYPE_PUBLIC,
                    "", International.getString("Boot"), true);//true=use autocomplete list
            boat.setAutoCompleteData(new AutoCompleteList(Daten.project.getBoats(false).data(), now, now));
            if (SimpleInputDialog.showInputDialog(this, International.getString("Boot auswählen"), boat)) {
                String s = boat.toString();
                try {
                    if (s != null && s.length() > 0) {
                        Boats boats = Daten.project.getBoats(false);
                        record = ((BoatDamages)persistence).createBoatDamageRecord(boats.getBoat(s, now).getId());
                        ((BoatDamageRecord)record).setReportDate(DataTypeDate.today());
                        ((BoatDamageRecord)record).setReportTime(DataTypeTime.now());
                    }
                } catch(Exception e) {
                    Logger.logdebug(e);
                }
            }
        }
        if (record == null) {
            return null;
        }
        return new BoatDamageEditDialog(parent, (BoatDamageRecord)record, newRecord, admin);
    }

    // @Override
    public boolean deleteCallback(JDialog parent,IItemListenerDataRecordTable caller, AdminRecord admin, DataRecord[] records) {
    	return BoatDamageRecord.deleteCallbackForGUIs(this, this, this.admin,persistence, records);
    }
    
	protected void createSpecificItemTypeRecordTable() {
        table = new BoatDamageItemTypeDataRecordTable("TABLE",
                persistence.createNewRecord().getGuiTableHeader(),
                persistence, validAt, admin,
                filterFieldName, filterFieldValue, // defaults are null
                actionText, actionType, actionImage, // default actions: new, edit, delete
                this,
                IItemType.TYPE_PUBLIC, "BASE_CAT", getTitle());

		table.addPermanentSecondarySortingColumn(BoatDamageRecord.COLUMN_ID_BOAT_NAME);        
		table.addPermanentSecondarySortingColumn(BoatDamageRecord.COLUMN_ID_REPORTDATE);
		table.addPermanentSecondarySortingColumn(BoatDamageRecord.COLUMN_ID_DAMAGE);	
	}
	
	protected void iniDialog() throws Exception {
		super.iniDialog();
		//show only matching items by default in BoatDamageListDialog 
		table.setIsFilterSet(true);
	}
	
    protected void iniControlPanel() {
    	// we want to put an additional element after the control panel
    	super.iniControlPanel();
    	this.iniBoatDamageListFilter();
    }
	
	private void iniBoatDamageListFilter() {
		JPanel myControlPanel= new JPanel();
    	
    	showOpenDamagesOnly = new ItemTypeBoolean("SHOW_ACTIVE_DAMAGES_ONLY",
                true,
                IItemType.TYPE_PUBLIC, "", International.getString("nur offene Bootsschäden"));
    	showOpenDamagesOnly.setPadding(0, 0, 0, 0);
    	showOpenDamagesOnly.displayOnGui(this, myControlPanel, 0, 0);
    	showOpenDamagesOnly.registerItemListener(this);
        mainPanel.add(myControlPanel, BorderLayout.NORTH);
	}    
	
    public void itemListenerAction(IItemType itemType, AWTEvent event) {
    	
    	// handle our special filter for active damages, else use default item handler
    	if (itemType==showOpenDamagesOnly) {
    		
    		 if (event.getID() == ActionEvent.ACTION_PERFORMED) {
    			 showOpenDamagesOnly.getValueFromGui();
    			 ((BoatDamageItemTypeDataRecordTable) table).setShowOpenDamagesOnly(showOpenDamagesOnly.getValue());	 
    		 }
    		
    	} else {
    		super.itemListenerAction(itemType, event);
    	}
    }
}
