/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.JDialog;
import javax.swing.SwingConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;

public class NewLogbookDialog extends StepwiseDialog implements IItemListener {

	private static final long serialVersionUID = -6041787195199421231L;

	private static final String LOGBOOKNAME        = "LOGBOOKNAME";
    private static final String LOGBOOKNAMEHINT    = "LOGBOOKNAMEHINT";    
    private static final String LOGBOOKDESCRIPTION = "LOGBOOKDESCRIPTION";
    private static final String LOGBOOKNAMESTEP1   = "LOGBOOKNAMESTEP1";
    private static final String LOGBOOKNAMESTEP2   = "LOGBOOKNAMESTEP2";
    private static final String AUTOCALC_DATETO    = "AUTOCALC_DATETO";
    private static final String DATEFROM           = "DATEFROM";
    private static final String DATETO             = "DATETO";
    private static final String DATETOHINT		   = "DATETOHINT";
    private static final String AUTOMATICLOGSWITCH = "AUTOMATICLOGSWITCH";
    private static final String LOGSWITCHBOATHOUSE = "LOGSWITCHBOATHOUSE";

    private static final String CATEGORY_STEP_0    = "0";
    private static final String CATEGORY_STEP_1    = "1";
    private static final String CATEGORY_STEP_2    = "2";
    
    private ItemTypeDate itemDateFrom;
    private ItemTypeDate itemDateTo;
    private ItemTypeBoolean itemAutoCalcDateTo;
    
    private String newLogbookName;

    public NewLogbookDialog(JDialog parent) {
        super(parent, International.getString("Neues Fahrtenbuch"));
    }

    public NewLogbookDialog(Frame parent) {
        super(parent, International.getString("Neues Fahrtenbuch"));
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    String[] getSteps() {
        return new String[] {
            International.getString("Name und Beschreibung"),
            International.getString("Zeitraum für Fahrtenbuch"),
            International.getString("Fahrtenbuchwechsel")
        };
    }

    String getDescription(int step) {
    	String val = "";
        switch(step) {
            case 0:
                val= International.getString("Ein Fahrtenbuch sollte üblicherweise alle Fahrten eines Jahr enthalten. "+
                        "Vereine mit mehreren Bootshäusern sollten pro Bootshaus ein eigenes Fahrtenbuch (in demselben Projekt) anlegen.");
            	if (Daten.project.getNumberOfBoathouses()>1 || Daten.project.getIsProjectStorageTypeEfaCloud()) {
            		val=val+"\n"+International.getString("Bei Nutzung von efaCloud oder mehreren Bootshäusern MUSS der Fahrtenbuchname dem Aufbau JJJJ_Freitext entsprechen, z.B. 2025_Bootshausname");
            	}  
            	if (Daten.project.getIsProjectStorageTypeEfaCloud()) {
            		val=val+"\n"+International.getString("Bei Nutzung von efaCloud mit mehreren Bootshäusern muss das Fahrtenbuch auch beim Referenzuser angelegt sein. Sonst ist kein Upload von Fahrten möglich.");
            	}
            	return val;
            case 1:
            	return International.getString("Bitte wähle den Zeitraum für Fahrten dieses Fahrtenbuches aus. efa wird später nur Fahrten "+
                        "innerhalb dieses Zeitraums für dieses Fahrtenbuch zulassen.");
            case 2:
                return International.getString("Durch die Konfiguration eines Fahrtenbuchwechsels ist es möglich, ein neues Fahrtenbuch "+
                        "automatisch zu einem konfigurierten Zeitpunkt (zum Beispiel Jahreswechsel) zu öffnen. efa-Bootshaus beendet dann im aktuellen " +
                        "Fahrtenbuch alle noch offenen Fahrten, schließt das aktuelle Fahrtenbuch und wechselt zum neuen Fahrtenbuch, ohne daß " +
                        "weitere Eingriffe eines Administrators nötig wären.");
        }
        return "";
    }

    void initializeItems() {
        items = new ArrayList<IItemType>();
        IItemType item;

        // Items for Step 0
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        String year = Integer.toString( cal.get(Calendar.MONTH)+1 <= 10 ?
            cal.get(Calendar.YEAR) : cal.get(Calendar.YEAR) + 1); // current year until October, year+1 else

        items.add(EfaGuiUtils.createHintWordWrap(LOGBOOKNAMEHINT, IItemType.TYPE_PUBLIC, CATEGORY_STEP_0, 
        		International.getString("Der Fahrtenbuchname sollte dem Aufbau JJJJ_Freitext entsprechen z.B. 2025_Bootshausname")
        		,2,10,10,500));
            
        if (Daten.project.getNumberOfBoathouses()>1 || Daten.project.getIsProjectStorageTypeEfaCloud()) {
            items.add(EfaGuiUtils.createHintWordWrap(LOGBOOKNAMEHINT+"1", IItemType.TYPE_PUBLIC, CATEGORY_STEP_0, 
            		International.getString("Achten Sie auf weitere Hinweise im Beschreibungs-Bereich dieses Dialogs.")
            		,2,10,10,500));
        }     
        
        item = new ItemTypeString(LOGBOOKNAME, year, IItemType.TYPE_PUBLIC, CATEGORY_STEP_0, International.getString("Name des Fahrtenbuchs"));
        ((ItemTypeString)item).setAllowedCharacters("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
        ((ItemTypeString)item).setReplacementCharacter('_');
        ((ItemTypeString)item).setNotNull(true);
        items.add(item);
        item = new ItemTypeString(LOGBOOKDESCRIPTION, "", IItemType.TYPE_PUBLIC, CATEGORY_STEP_0, International.getString("Beschreibung"));
        items.add(item);
        
        // Items for Step 1
        ItemTypeLabel newLogbookNameLabel = new ItemTypeLabel(LOGBOOKNAMESTEP1, IItemType.TYPE_PUBLIC, CATEGORY_STEP_1, buildLogbookName());
        newLogbookNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        newLogbookNameLabel.setFieldGrid(3, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
        newLogbookNameLabel.setBoldFont(true);
        items.add(newLogbookNameLabel);
        
        item = new ItemTypeBoolean(AUTOCALC_DATETO,true, IItemType.TYPE_PUBLIC, CATEGORY_STEP_1, International.getString("Endedatum automatisch berechnen"));
        item.registerItemListener(this);
        this.itemAutoCalcDateTo = (ItemTypeBoolean) item;
        items.add(item);

        
        item = new ItemTypeDate(DATEFROM, new DataTypeDate(1, 1, EfaUtil.string2int(year, 2010)), IItemType.TYPE_PUBLIC, CATEGORY_STEP_1, International.getString("Beginn des Zeitraums"));
        ((ItemTypeDate)item).setNotNull(true);
        item.registerItemListener(this);
        items.add(item);
        this.itemDateFrom=(ItemTypeDate)item;
        
        item = new ItemTypeDate(DATETO, new DataTypeDate(31, 12, EfaUtil.string2int(year, 2010)), IItemType.TYPE_PUBLIC, CATEGORY_STEP_1, International.getString("Ende des Zeitraums"));
        ((ItemTypeDate)item).setNotNull(true);
        items.add(item);
        this.itemDateTo=(ItemTypeDate)item;
        
        if (Daten.project.getIsProjectStorageTypeEfaCloud()) {
            items.add(EfaGuiUtils.createHintWordWrap(DATETOHINT, IItemType.TYPE_PUBLIC, CATEGORY_STEP_1,
            		International.getString("Bei Nutzung von efaCloud muss der Startzeitpunkt mit dem in efaCloud hinterlegten Startzeitpunkt übereinstimmen.")
            		,2,10,10,500));
        }          

        // Items for Step 2 name, type, category, description
        
        ItemTypeLabel newLogbookNameLabelStep2 = new ItemTypeLabel(LOGBOOKNAMESTEP2, IItemType.TYPE_PUBLIC, CATEGORY_STEP_2, buildLogbookName());
        newLogbookNameLabelStep2.setHorizontalAlignment(SwingConstants.CENTER);
        newLogbookNameLabelStep2.setFieldGrid(3, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
        newLogbookNameLabelStep2.setBoldFont(true);
        items.add(newLogbookNameLabelStep2);
        
        item = new ItemTypeBoolean(AUTOMATICLOGSWITCH, false, IItemType.TYPE_PUBLIC, CATEGORY_STEP_2,
                International.getMessage("Fahrtenbuchwechsel automatisch zum {datum}", "?"));
        items.add(item);
        if (Daten.project.getNumberOfBoathouses() > 1) {
            String[] descr  = Daten.project.getAllBoathouseNames();
            String[] values = new String[descr.length];
            for (int i=0; i<values.length; i++) {
                values[i] = Integer.toString(Daten.project.getBoathouseId(descr[i]));
            }
            item = new ItemTypeStringList(LOGSWITCHBOATHOUSE,
                    Integer.toString(Daten.project.getMyBoathouseId()),
                    values, descr,
                    IItemType.TYPE_PUBLIC, CATEGORY_STEP_2,
                    International.getString("Bootshaus"));
            items.add(item);
        }
    }

    boolean checkInput(int direction) {
        boolean ok = super.checkInput(direction);
        if (!ok) {
            return false;
        }

        if (step == 0) {
            ItemTypeString item = (ItemTypeString)getItemByName(LOGBOOKNAME);
            item.getValueFromGui();
            String name = item.getValue();
            if (Daten.project.getLoogbookRecord(name) != null) {
                    Dialog.error(LogString.fileAlreadyExists(name, International.getString("Fahrtenbuch")));
                    item.requestFocus();
                    return false;
            }

            int year = EfaUtil.stringFindInt(name, -1);
            if (year > 1980 && year < 2100) {
                ItemTypeDate logFromDate = (ItemTypeDate) getItemByName(DATEFROM);
                ItemTypeDate logFromTo = (ItemTypeDate) getItemByName(DATETO);
                
                if (logFromDate != null) {
                    logFromDate.setValueDate(new DataTypeDate(getStartDayFromCurrentLogbook(), getStartMonthFromCurrentLogbook(), year));
                }
                if (logFromTo != null) {
                    logFromTo.setValueDate(calculateDateTo(logFromDate.getDate()));
                }
            }
            
            //update logbookname for next gui step
            ItemTypeLabel logbookname = (ItemTypeLabel)getItemByName(LOGBOOKNAMESTEP1);
            logbookname.setDescription(buildLogbookName());
            
            logbookname = (ItemTypeLabel)getItemByName(LOGBOOKNAMESTEP2);
            logbookname.setDescription(buildLogbookName());
            
        }

        if (step == 1) {
            ItemTypeDate logFromDate = (ItemTypeDate)getItemByName(DATEFROM);
            ItemTypeBoolean logswitch = (ItemTypeBoolean)getItemByName(AUTOMATICLOGSWITCH);
            if (logFromDate != null && logswitch != null) {
                logswitch.setDescription(International.getMessage("Fahrtenbuchwechsel automatisch zum {datum}",
                        logFromDate.toString()));
            }
        }

        return true;
    }

    /**
     * Gets the number of the day of the start date of a currently loaded logbook.
     * @return number of the day, or 1 if no logbook/project is open.
     */
    private int getStartDayFromCurrentLogbook() {
    	if (Daten.project != null &&  Daten.project.getCurrentLogbook() != null) {
    		return Daten.project.getCurrentLogbook().getStartDate().getDay();
    	} else {
    		return 1;
    	}
    }

    /**
     * Gets the number of the month of the start date of a currently loaded logbook.
     * @return number of the month, or 1 if no logbook/project is open.
     */
    private int getStartMonthFromCurrentLogbook() {
    	if (Daten.project != null &&  Daten.project.getCurrentLogbook() != null) {
    		return Daten.project.getCurrentLogbook().getStartDate().getMonth();
    	} else {
    		return 1;
    	}
    }
    /**
     * Calculates the end date of the logbook period, being given a start date.
     * The end date is calculated by adding a year and substracting a day.
     * @param from start date (not null)
     * @return end date of the period. null, if there had been an error.
     */
    private DataTypeDate calculateDateTo(DataTypeDate from) {
		try {
			DataTypeDate targetDateTo = from;
			//add a year and reduce by one day
			targetDateTo.setDate(targetDateTo.getDay(), targetDateTo.getMonth(),targetDateTo.getYear()+1);
			targetDateTo.addDays(-1);
			return targetDateTo;
		} catch (Exception e) {
			return null;
		}    	
    }
    
    boolean finishButton_actionPerformed(ActionEvent e) {
        if (!super.finishButton_actionPerformed(e)) {
            return false;
        }

        ItemTypeString logName = (ItemTypeString)getItemByName(LOGBOOKNAME);
        ItemTypeString logDescription = (ItemTypeString)getItemByName(LOGBOOKDESCRIPTION);
        ItemTypeDate logFromDate = (ItemTypeDate)getItemByName(DATEFROM);
        ItemTypeDate logFromTo = (ItemTypeDate)getItemByName(DATETO);
        ItemTypeBoolean logswitch = (ItemTypeBoolean)getItemByName(AUTOMATICLOGSWITCH);
        ItemTypeStringList logswitchBoathouse = (ItemTypeStringList)getItemByName(LOGSWITCHBOATHOUSE);

        ProjectRecord rec = Daten.project.createNewLogbookRecord(logName.getValue());
        rec.setDescription(logDescription.getValue());
        rec.setStartDate(logFromDate.getDate());
        rec.setEndDate(logFromTo.getDate());
        
        try {
            Daten.project.addLogbookRecord(rec);
            newLogbookName = logName.getValue();
            Daten.project.getLogbook(newLogbookName, true);
            Dialog.infoDialog(LogString.fileSuccessfullyCreated(logName.getValue(),
                    International.getString("Fahrtenbuch")));
            if (logswitch.getValue()) {
                boolean switchok = false;
                String bh = (logswitchBoathouse == null ? Daten.project.getMyBoathouseName() :
                    Daten.project.getBoathouseName(EfaUtil.string2int(logswitchBoathouse.getValue(), -1)));
                if (bh != null) {
                    ProjectRecord r = Daten.project.getBoathouseRecord(bh);
                    if (r != null) {
                        r.setAutoNewLogbookDate(logFromDate.getDate());
                        r.setAutoNewLogbookName(logName.getValue());
                        Daten.project.getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
                        switchok = true;
                    }
                }
                if (switchok) {
                    Dialog.infoDialog(International.getMessage("Fahrtenbuchwechsel zum {datum} konfiguriert.",
                            logFromDate.getDate().toString()));
                } else {
                    Dialog.error(International.getString("Fahrtenbuchwechsel konnte nicht konfiguriert werden.") + "\n" +
                            "BoathouseRecord not found - please check your boathouse configuration in the project");
                }
            }
            setDialogResult(true);
        } catch(EfaException ee) {
            newLogbookName = null;
            Dialog.error(ee.getMessage());
            ee.log();
            setDialogResult(false);
        }
        
        return true;
    }

    public String getNewLogbookName() {
        return newLogbookName;
    }

    public String newLogbookDialog() {
        showDialog();
        return getNewLogbookName();
    }
    
    private String buildLogbookName() {
        ItemTypeString newLogbook = (ItemTypeString)getItemByName(LOGBOOKNAME);
        newLogbook.getValueFromGui();
        String newLogbookName = newLogbook.getValue();
    	return International.getString("Fahrtenbuch")+": \""+newLogbookName+"\"";
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
    	Boolean calculateField=false;
    	if (itemAutoCalcDateTo!=null)  {
    		itemAutoCalcDateTo.getValueFromGui();//Read checkbox from Gui into the field
    		calculateField=itemAutoCalcDateTo.getValue();
    	}
    	if (calculateField && itemType.getName().equalsIgnoreCase(DATEFROM)) {
    		if ((itemDateFrom!=null && itemDateTo!=null) && (event instanceof KeyEvent)){
    			//if (event. is KeyEvent) 
    			itemDateFrom.getValueFromGui();//get the value into the field
    			itemDateTo.setValueDate(calculateDateTo(itemDateFrom.getDate()));
    			itemDateTo.showValue();    			
    		}
    	} else if (calculateField && itemType.getName().equalsIgnoreCase(AUTOCALC_DATETO)) {
    		if ((itemDateFrom!=null && itemDateTo!=null)){
    			//if (event. is KeyEvent) 
    			itemDateFrom.getValueFromGui();//get the value into the field
    			itemDateTo.setValueDate(calculateDateTo(itemDateFrom.getDate()));
    			itemDateTo.showValue();
    		}    		
    	}
    }
    
    protected boolean nextButton_actionPerformed(ActionEvent e) {
    	Boolean val = super.nextButton_actionPerformed(e);
    	if (step == 1) {
    		// usability: if entering step 1, take care that the itemDateFrom gets the focus.
    		// otherwise AUTOCALC_DATETO checkbox would be focused, this is not optimal
    		itemDateFrom.requestFocus();
    	}
    	return val;
    }

    protected boolean backButton_actionPerformed(ActionEvent e) {
    	Boolean val = super.backButton_actionPerformed(e);
    	if (step == 1) {
    		// usability: if entering step 1, take care that the itemDateFrom gets the focus.
    		// otherwise AUTOCALC_DATETO checkbox would be focused, this is not optimal    		
    		itemDateFrom.requestFocus();
    	}
    	return val;
    }
    
}
