/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

/*
 * Documentation for BoatReservationListDialog
 *
 * 
 * BoatReservationListDialog gets its GUI Elements from:
 * - Table columns and content: BoatReservationRecord.getGuiTableHeader, getGuiTableItems
 * - Buttons new, edit, delete: ItemTypeDataRecordTable
 * 		handles also the actions for new, edit, delete
 * 	    and delegates all other actions to DataListDialog.itemListenerActionTable
 * 
 * - Buttons import, export, hide, ...: DataListDialog.iniActions
 * 		handles also the actions for import, export, ...
 * 
 * - Buttons for Copy_Reservation and Change_Boatname: this class.
 * 		the handling for these actions is in itemListenerActionTable().
 * 
 * Action handling
 * ------------------------------
 * DataListDialog
 *  |- BoatReservationListDialog
 *  
 *  ItemTypeDataRecordTable.itemListenerAction(){
 *   handle new, edit, delete
 *   call itemListenerActionTable.itemListenerActionTable(actionId, records);
 *   	which usually is in DataListDialog.itemListenerActionTable()
 *      	handle import, export, hide...
 *   	which is overwritten (but calls super) by BoatReservationListDialog.itemListenerActionTable()
 *   		handle Create Reservation, change boat
 * 
 */

package de.nmichael.efa.gui.dataedit;

import de.nmichael.efa.*;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


// @i18n complete
public class BoatReservationListDialog extends DataListDialog {

	private static final long serialVersionUID = 5648707256342719384L;
	
	public static final int ACTION_CHANGE_BOATNAME = 310;
    public static final int ACTION_COPY_RESERVATION = 311;
    public static final String ACTIONTEXT_CHANGE_BOATNAME = International.getString("Boot ändern");
    public static final String ACTIONTEXT_COPY_RESERVATION = International.getString("Kopie anlegen");
    public static final String ACTIONIMAGE_COPY_RESERVATION = ImagesAndIcons.IMAGE_BUTTON_COPY;
    public static final String ACTIONIMAGE_CHANGE_BOATNAME = ImagesAndIcons.IMAGE_BUTTON_BOAT;
	
    boolean allowNewReservationsWeekly = true;
	protected ItemTypeBoolean showTodaysReservationsOnly;

    public BoatReservationListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(null, true, true, true);
    }

    public BoatReservationListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(null, true, true, true);
    }

    public BoatReservationListDialog(Frame parent, UUID boatId, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(boatId, true, true, true);
    }

    public BoatReservationListDialog(JDialog parent, UUID boatId, AdminRecord admin) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, admin);
        iniValues(boatId, true, true, true);
    }

    public BoatReservationListDialog(Frame parent, UUID boatId, boolean allowNewReservations, boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, null);
        iniValues(boatId, allowNewReservations, allowNewReservationsWeekly, allowEditDeleteReservations);
    }

    public BoatReservationListDialog(JDialog parent, UUID boatId, boolean allowNewReservations, boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
        super(parent, International.getString("Bootsreservierungen"), Daten.project.getBoatReservations(false), 0, null);
        iniValues(boatId, allowNewReservations, allowNewReservationsWeekly, allowEditDeleteReservations);
    }

    private void iniValues(UUID boatId, boolean allowNewReservations, boolean allowNewReservationsWeekly, boolean allowEditDeleteReservations) {
        if (boatId != null) {
            this.filterFieldName  = BoatReservationRecord.BOATID;
            this.filterFieldValue = boatId.toString();
            if (Daten.project != null) {
                Boats boats = Daten.project.getBoats(false);
                if (boats != null) {
                    BoatRecord r = boats.getBoat(boatId, System.currentTimeMillis());
                    if (r != null) {
                        this.filterFieldDescription = International.getString("Boot") + ": " +
                                r.getQualifiedName();
                    }
                }
            }
        }
        if (allowNewReservations && allowEditDeleteReservations) {
            if (admin != null) {
                // default: ADD, EDIT, DELETE, IMPORT, EXPORT
            	// but add an button "Copy reservation"
            } else {
                actionText = new String[]{
                            ItemTypeDataRecordTable.ACTIONTEXT_NEW,
                            ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
                            ItemTypeDataRecordTable.ACTIONTEXT_DELETE
                        };
                actionType = new int[]{
                            ItemTypeDataRecordTable.ACTION_NEW,
                            ItemTypeDataRecordTable.ACTION_EDIT,
                            ItemTypeDataRecordTable.ACTION_DELETE
                        };
            }

            insertAction(ACTIONTEXT_COPY_RESERVATION, ACTION_COPY_RESERVATION, ACTIONIMAGE_COPY_RESERVATION, ItemTypeDataRecordTable.ACTION_NEW);
        	insertAction(ACTIONTEXT_CHANGE_BOATNAME, ACTION_CHANGE_BOATNAME, ACTIONIMAGE_CHANGE_BOATNAME, ItemTypeDataRecordTable.ACTION_EDIT);
        	
        } else if (allowNewReservations) {
            actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_NEW };
            actionType = new int[] { ItemTypeDataRecordTable.ACTION_NEW };
            actionImage = new String [] {ImagesAndIcons.IMAGE_BUTTON_ADD };
        	insertAction(ACTIONTEXT_COPY_RESERVATION, ACTION_COPY_RESERVATION, ACTIONIMAGE_COPY_RESERVATION, ItemTypeDataRecordTable.ACTION_NEW);
            
        } else if (allowEditDeleteReservations) {
            actionText = new String[] { ItemTypeDataRecordTable.ACTIONTEXT_EDIT, ItemTypeDataRecordTable.ACTIONTEXT_DELETE };
            actionType = new int[] { ItemTypeDataRecordTable.ACTION_EDIT, ItemTypeDataRecordTable.ACTION_DELETE };
            actionImage = new String [] {ImagesAndIcons.IMAGE_BUTTON_EDIT, ImagesAndIcons.IMAGE_BUTTON_DELETE };

        	insertAction(ACTIONTEXT_CHANGE_BOATNAME, ACTION_CHANGE_BOATNAME, ACTIONIMAGE_CHANGE_BOATNAME, ItemTypeDataRecordTable.ACTION_EDIT);
        } else {
            actionText = new String[] { };
            actionType = new int[] { };
        }
        this.allowNewReservationsWeekly = allowNewReservationsWeekly;
        
		//From and to columns should be wider than default
		//Buttons on north, if boat reservations are called from efaBths main screen.
		this.buttonPanelPosition = (Daten.isAdminMode() ? BorderLayout.EAST : BorderLayout.NORTH);
		this.minColumnWidths = new int[] {150,150,150,120,12,-1};   
    }


    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        /* A dataListDialog can be opened with a parameter which specifies the corresponding boat/... on which the list is shown.
         * this is stored in filterFieldValue. So, we have to check for filterFieldValue and use this base record instead of asking for a boat/... to create the data for 
         */
        if (record == null && persistence != null && filterFieldValue != null) {
            record = ((BoatReservations)persistence).createBoatReservationsRecord(UUID.fromString(filterFieldValue));
        }
        if (record == null) {
            long now = System.currentTimeMillis();
            ItemTypeStringAutoComplete boat = new ItemTypeStringAutoComplete("BOAT", "", IItemType.TYPE_PUBLIC,
                    "", International.getString("Boot"), true);
            boat.setAutoCompleteData(new AutoCompleteList(Daten.project.getBoats(false).data(), now, now));
            if (SimpleInputDialog.showInputDialog(this, International.getString("Boot auswählen"), boat)) {
                String s = boat.toString();
                try {
                    if (s != null && s.length() > 0) {
                        Boats boats = Daten.project.getBoats(false);
                        record = ((BoatReservations)persistence).createBoatReservationsRecord(boats.getBoat(s, now).getId());
                    }
                } catch(Exception e) {
                    Logger.logdebug(e);
                }
            }
        }
        if (record == null) {
            return null;
        }
        if (admin == null) {
            try {
                Boats boats = Daten.project.getBoats(false);
                BoatRecord b = boats.getBoat(((BoatReservationRecord)record).getBoatId(), System.currentTimeMillis());
                if (b.getOwner() != null && b.getOwner().length() > 0 &&
                    !Daten.efaConfig.getValueMembersMayReservePrivateBoats()) {
                    Dialog.error(International.getString("Privatboote dürfen nicht reserviert werden!"));
                    return null;
                }
                BoatStatusRecord bs = b.getBoatStatus();
                if (bs != null && BoatStatusRecord.STATUS_NOTAVAILABLE.equals(bs.getBaseStatus()) &&
                    Dialog.yesNoDialog(International.getString("Boot nicht verfügbar"), 
                            International.getMessage("Das ausgewählte Boot '{boat}' ist derzeit nicht verfügbar:", b.getQualifiedName()) + "\n" +
                            International.getString("Status") + ": " + bs.getComment() + "\n" +
                            International.getString("Möchtest Du das Boot trotzdem reservieren?")) != Dialog.YES) {
                    return null;
                }
            } catch(Exception e) {
                Logger.logdebug(e);
                return null;
            }
        }

        try {
            return new BoatReservationEditDialog(parent, (BoatReservationRecord) record,
                    newRecord, allowNewReservationsWeekly, admin);
        } catch (Exception e) {
            Dialog.error(e.getMessage());
            return null;
        }
    }
	protected void createSpecificItemTypeRecordTable() {
        table = new BoatReservationItemTypeDataRecordTable("TABLE",
                persistence.createNewRecord().getGuiTableHeader(),
                persistence, validAt, admin,
                filterFieldName, filterFieldValue, // defaults are null
                actionText, actionType, actionImage, // default actions: new, edit, delete
                this,
                IItemType.TYPE_PUBLIC, "BASE_CAT", getTitle());
		table.addPermanentSecondarySortingColumn(BoatReservationRecord.COLUMN_ID_START);        
		table.addPermanentSecondarySortingColumn(BoatReservationRecord.COLUMN_ID_NAME);

	}
	
	protected void iniDialog() throws Exception {
		super.iniDialog();
		//show only matching items by default in BoatDamageListDialog 
		table.setIsFilterSet(true);
		this.setRequestFocus(table.getSearchField());
	}
	
    protected void iniControlPanel() {
    	// we want to put an additional element after the control panel
    	super.iniControlPanel();
    	this.iniBoatReservationListFilter();
    }
	
	private void iniBoatReservationListFilter() {
		JPanel myControlPanel= new JPanel();
    	
		showTodaysReservationsOnly = new ItemTypeBoolean("SHOW_TODAYS_RESERVATIONS_ONLY",
                false,
                IItemType.TYPE_PUBLIC, "", International.getString("nur heutige Reservierungen anzeigen"));
		showTodaysReservationsOnly.setPadding(0, 0, 0, 0);
		showTodaysReservationsOnly.displayOnGui(this, myControlPanel, 0, 0);
		showTodaysReservationsOnly.registerItemListener(this);
        mainPanel.add(myControlPanel, BorderLayout.NORTH);
	}
	
    public void itemListenerAction(IItemType itemType, AWTEvent event) {
    	
    	// handle our special filter for today's reservations, else use default item handler
    	if (itemType.equals(showTodaysReservationsOnly)) {
    		
    		 if (event.getID() == ActionEvent.ACTION_PERFORMED) {
    			 showTodaysReservationsOnly.getValueFromGui();
    			 if (showTodaysReservationsOnly.getValue()) {
    				 table.getSearchField().setValue(DataTypeDate.today().toString());
    				 table.getFilterBySearch().setValue(true);
    				 table.updateData();
    				 table.showValue();
    			 } else {
    				 table.getSearchField().setValue("");
    				 table.getFilterBySearch().setValue(true);
    				 table.updateData();
    				 table.showValue();
    			 }
    		 }
    		
    	} else {
    		super.itemListenerAction(itemType, event);
    	}
    }	
    
    
    // @Override
    public void itemListenerActionTable(int actionId, DataRecord[] records)  {

    	super.itemListenerActionTable(actionId, records);
    	
    	switch(actionId) {
            case ACTION_CHANGE_BOATNAME:
            	changeBoatName(records);
            	break;
            case ACTION_COPY_RESERVATION:
            	copyReservation(records);
            	break;
        }
    }
            
    private void changeBoatName(DataRecord[] records) {
        DataEditDialog dlg;
    	
        for (int i = 0; records != null && i < records.length; i++) {
            if (records[i] != null) {

            	if (isCopyOrBoatChangeAllowed(records[i])) {
	                dlg = this.createNewDataDialogForCopyOrChangeBootname(this, persistence, records[i],i+1, records.length, false);
	                if (dlg == null) {
	                    return;
	                }
	                dlg.showDialog();
	                if (!dlg.getDialogResult()) {
	                    //could not save data or user cancelled
	                	break;
	                } else {
	                	//we could save the data. we are changing the boat name.
	                	// so the original record shall be deleted afterwards.
	                	
	                    if (records[i] != null) {
	                        if (persistence.data().getMetaData().isVersionized()) {
	                        	//could not delete. BoatReservationRecord is not versionized.
	                        	//we should never get here. If we do, an programming error has happened.
	                        	// BoatReservationRecord.initialize calls createMetadata with "versionized=true" which is not valid while programming this function-
	                            String msg=International.getString("Programminterner Fehler aufgetreten. BoatReservationRecord ist versionized, darf dies aber nicht sein. Bitte melde diesen Fehler an den EFA-Programmierer.");
	                            Dialog.error(msg);
	                            Logger.log(Logger.ERROR,
	                                    Logger.MSG_DATA_DELETEFAILED,
	                                    msg);
	                        } else {
	                        	try {
	                            persistence.data().delete(records[i].getKey());
	                            Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDDELETED,
	                                    records[i].getPersistence().getDescription() + ": "
	                                    + International.getMessage("{name} hat Datensatz '{record}' gelöscht (Über Änderung Bootsname bei Reservierung).",
	                                    (admin != null ? International.getString("Admin") + " '" + admin.getName() + "'"
	                                    : International.getString("Normaler Benutzer")),
	                                    records[i].getQualifiedName()));
	                        	} catch (Exception e) {
	                                Dialog.error(e.getLocalizedMessage());
	                                Logger.log(Logger.ERROR,
	                                        Logger.MSG_DATA_DELETEFAILED,
	                                        e.getMessage());
	                        	}
	                        }
	                    }
	                	
	                }
            	}
            }        
        }
    }
    
    
    private Boolean isCopyOrBoatChangeAllowed(DataRecord record) {
    	BoatReservationRecord br =((BoatReservationRecord)record); 
    	String boatReservationRecordType=br.getType();
    	
    	if ((boatReservationRecordType.equals(BoatReservationRecord.TYPE_WEEKLY)
    			|| boatReservationRecordType.equals(BoatReservationRecord.TYPE_WEEKLY_LIMITED))
    			&& !this.allowNewReservationsWeekly) {
    		//weekly or weekly limited reservations, and members may not create weekly reservations
    		String msg = International.getString("Diese Reservierung kann nicht bearbeitet werden.")+"\n\n"+br.getBoatName()+" "+ br.getGuiDateTimeFromDescription() +" - "+br.getGuiDateTimeToDescription()+ " ("+br.getPersonAsName()+")";
    		Dialog.error(msg);
    		return false;
    	} else {
    		return true;
    	}
    }
    
    private void copyReservation(DataRecord[] records)  {
        DataEditDialog dlg;
    	
        for (int i = 0; records != null && i < records.length; i++) {
            if (records[i] != null) {


            	if (isCopyOrBoatChangeAllowed(records[i])) {
	                dlg = this.createNewDataDialogForCopyOrChangeBootname(this, persistence, records[i],i+1, records.length, true);
	                if (dlg == null) {
	                    return;
	                }
	                dlg.showDialog();
	                if (!dlg.getDialogResult()) {
	                    break;
	                }
            	}
            }
        }        

    }    
    

    /**
     * Creates a copy of a BoatReservationsRecord. 
     * - asks for a new boat name
     * - creates a new boatReservationRecord for this new boat name
     * - copies all fields from the original to the copy.
     * - shows the Dialog
     * 
     * @param parent Parent Dialog
     * @param persistence Persistence
     * @param record record to create a copy for
     * @return
     */
    public DataEditDialog createNewDataDialogForCopyOrChangeBootname(JDialog parent, StorageObject persistence, DataRecord baseRecord, int iCurrentRecord, int iCountRecords, Boolean bDoCopy) {

        BoatReservationRecord copyRecord = null;
        BoatReservationRecord originalRecord = (BoatReservationRecord) baseRecord;
        if (baseRecord == null) {
        	return null; // we cannot create a copy from an empty record, so exit heere
        }

        long now = System.currentTimeMillis();
        String strCaption = "";
        String strNewBoatCaption = "";
        String oldBoatName=originalRecord.getBoatName();
        
        if (bDoCopy) {
        	strCaption = International.getMessage("Kopie für Reservierung anlegen ({aktuell} von {anzahl})", iCurrentRecord, iCountRecords);
        	strNewBoatCaption = International.getString("Neues Boot");
        } else {
        	strCaption = International.getMessage("Boot für Reservierung ändern ({aktuell} von {anzahl})",iCurrentRecord, iCountRecords);
        	strNewBoatCaption = International.getString("Ändere Boot auf");
        	//strNewBoatCaption = International.getMessage("Ändere \"{alterbootname}\" auf", oldBoatName);       	
        }

        
        //Caption
        ItemTypeLabel caption = new ItemTypeLabelHeader("_GUIITEM_GENERIC_CAPTION", IItemType.TYPE_PUBLIC, null, strCaption);
        caption.setPadding(0, 0, 0, 10);
    	caption.setFieldGrid(3,GridBagConstraints.EAST, GridBagConstraints.BOTH);

        //Show reservation data
        
        ItemTypeString dataBoatName = new ItemTypeString("_GUIITEM_BASE_RESERVATION_BOATNAME", originalRecord.getBoatName(), IItemType.TYPE_PUBLIC,null, International.getString("Boot"));
        dataBoatName.setEditable(false);

        ItemTypeString dataResTime = new ItemTypeString("_GUIITEM_BASE_RESERVATION_RESTIME", originalRecord.getGuiDateTimeFromDescription()+" - "+originalRecord.getGuiDateTimeToDescription(), 
        		IItemType.TYPE_PUBLIC,null, International.getString("Zeitraum"));
        dataResTime.setEditable(false);

        ItemTypeString dataResPerson = new ItemTypeString("_GUIITEM_BASE_RESERVATION_RESPERSON", originalRecord.getPersonAsName(), 
        		IItemType.TYPE_PUBLIC,null, International.getString("Reserviert für"));
        dataResPerson.setEditable(false);

        ItemTypeString dataResReason= new ItemTypeString("_GUIITEM_BASE_RESERVATION_RESREASON", originalRecord.getReason()+" / "+originalRecord.getContact(), 
        		IItemType.TYPE_PUBLIC,null, International.getString("Reservierungsgrund"));
        dataResReason.setEditable(false);
        dataResReason.setPadding(0, 0, 0, 20);
        
        
        // show the boat selection list
        ItemTypeStringAutoComplete newBoat = new ItemTypeStringAutoComplete("BOAT", "", IItemType.TYPE_PUBLIC,
                "", strNewBoatCaption, true);
        newBoat.setAutoCompleteData(new AutoCompleteList(Daten.project.getBoats(false).data(), now, now));

        IItemType[] dialogElements = new IItemType[6];        
        dialogElements[0]=caption;
        dialogElements[1]=dataBoatName;
        dialogElements[2]=dataResTime;
        dialogElements[3]=dataResPerson;
        dialogElements[4]=dataResReason;
        
        dialogElements[5]=newBoat;
        
        
        
        if (SimpleInputDialog.showInputDialog(this, strCaption, dialogElements)) {
            String s = newBoat.toString();
            try {
                if (s != null && s.length() > 0) {
                    Boats boats = Daten.project.getBoats(false);

                    // either copying or changing boat name: we create a copy of the current reservation.
                    // if changing boat name mode, we delete the original record afterwards in the calling method.

                    //create a new reservation
                    copyRecord = ((BoatReservations)persistence).createBoatReservationsRecord(boats.getBoat(s, now).getId());
                    //Copy basic fields
                    copyRecord.setType(originalRecord.getType());

                    copyRecord.setContact(originalRecord.getContact());
                    copyRecord.setDateFrom(originalRecord.getDateFrom());
                    copyRecord.setDateTo(originalRecord.getDateTo());
                    copyRecord.setDayOfWeek(originalRecord.getDayOfWeek());
                   	copyRecord.setPersonName(originalRecord.getPersonName());
                    copyRecord.setPersonId(originalRecord.getPersonId());
                    copyRecord.setReason(originalRecord.getReason());
                    copyRecord.setTimeFrom(originalRecord.getTimeFrom());
                    copyRecord.setTimeTo(originalRecord.getTimeTo());
                }
            } catch(Exception e) {
                Logger.logdebug(e);
            }
        }

        if (copyRecord == null) {
            return null;
        }
        if (admin == null) {
            try {
                Boats boats = Daten.project.getBoats(false);
                BoatRecord b = boats.getBoat(((BoatReservationRecord)copyRecord).getBoatId(), System.currentTimeMillis());
                if (b.getOwner() != null && b.getOwner().length() > 0 &&
                    !Daten.efaConfig.getValueMembersMayReservePrivateBoats()) {
                    Dialog.error(International.getString("Privatboote dürfen nicht reserviert werden!"));
                    return null;
                }
                BoatStatusRecord bs = b.getBoatStatus();
                if (bs != null && BoatStatusRecord.STATUS_NOTAVAILABLE.equals(bs.getBaseStatus()) &&
                    Dialog.yesNoDialog(International.getString("Boot nicht verfügbar"), 
                            International.getMessage("Das ausgewählte Boot '{boat}' ist derzeit nicht verfügbar:", b.getQualifiedName()) + "\n" +
                            International.getString("Status") + ": " + bs.getComment() + "\n" +
                            International.getString("Möchtest Du das Boot trotzdem reservieren?")) != Dialog.YES) {
                    return null;
                }
            } catch(Exception e) {
                Logger.logdebug(e);
                return null;
            }
        }

        try {
            return new BoatReservationEditDialog(parent, (BoatReservationRecord) copyRecord,
                    true /*always create new record*/, allowNewReservationsWeekly, admin, 
                    (bDoCopy ? International.getMessage("Reservierung_f\u00fcr_{boat}",copyRecord.getBoatName()) : International.getMessage("Boot für Reservierung ändern: {alt} -> {neu}",oldBoatName, copyRecord.getBoatName()))
                    );
        } catch (Exception e) {
            Dialog.error(e.getMessage());
            return null;
        }
    }    
    
    

    
}
