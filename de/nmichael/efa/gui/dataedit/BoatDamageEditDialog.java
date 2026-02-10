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
import de.nmichael.efa.util.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.ex.InvalidValueException;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import java.util.UUID;
import javax.swing.*;

// @i18n complete
public class BoatDamageEditDialog extends UnversionizedDataEditDialog implements IItemListener {

    private boolean boatWasDamaged = false;
    private boolean fixedWasChanged = false;

    public BoatDamageEditDialog(Frame parent, BoatDamageRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Bootsschaden"), r, newRecord, admin);
        if (!newRecord && r != null && !r.getFixed()) {
            boatWasDamaged = true;
        }
        initListener();
    }

    public BoatDamageEditDialog(JDialog parent, BoatDamageRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Bootsschaden"), r, newRecord, admin);
        if (!newRecord && r != null && !r.getFixed()) {
            boatWasDamaged = true;
        }
        initListener();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    private void initListener() {
        IItemType itemType = null;
        for (IItemType item : allGuiItems) {
            if (item.getName().equals(BoatDamageRecord.FIXED)) {
                ((ItemTypeBoolean)item).registerItemListener(this);
                itemType = item;
            }
        }
        itemListenerAction(itemType, null);
    }

    public void itemListenerAction(IItemType item, AWTEvent event) {
        if (item != null && item.getName().equals(BoatDamageRecord.FIXED)) {
            ((ItemTypeBoolean)item).getValueFromGui();
            boolean fixed = ((ItemTypeBoolean)item).getValue();
            getItem(BoatDamageRecord.GUIITEM_FIXDATETIME).setNotNull(fixed);
            getItem(BoatDamageRecord.FIXEDBYPERSONID).setNotNull(fixed);
            if (fixed) {
                ItemTypeDateTime fixedDate = (ItemTypeDateTime)getItem(BoatDamageRecord.GUIITEM_FIXDATETIME);
                fixedDate.getValueFromGui();
                if (!fixedDate.isSet()) {
                    fixedDate.parseAndShowValue(DataTypeDate.today().toString());
                }
                getItem(BoatDamageRecord.FIXEDBYPERSONID).requestFocus();
            }
        }
    }

    private void sendNotification() {
        BoatDamageRecord r = (BoatDamageRecord)dataRecord;
        Messages messages = r.getPersistence().getProject().getMessages(false);
        messages.createAndSaveMessageRecord(r.getReportedByPersonAsName(),
                MessageRecord.TO_BOATMAINTENANCE,
                r.getReportedByPersonId(),
                International.getString("Neuer Bootsschaden") + " - " + r.getBoatAsName(),
                r.getCompleteDamageInfo() +
                (r.getLogbookText() != null && r.getLogbookText().length() > 0 ?
                    "\n" + International.getString("Fahrt") + ": " + r.getLogbookText() :
                    (this.admin!=null ? International.getString("Admin-Name")+": "+this.admin.getQualifiedName() : ""))
                );
    }

    protected boolean saveRecord() throws InvalidValueException {
        boolean success = super.saveRecord();
        if (success && admin != null && dataRecord != null && boatWasDamaged &&
            ((BoatDamageRecord)dataRecord).getFixed()) {
            BoatDamageRecord r = (BoatDamageRecord) dataRecord;
            Messages messages = r.getPersistence().getProject().getMessages(false);
            messages.createAndSaveMessageRecord(r.getReportedByPersonAsName(),
                    MessageRecord.TO_BOATMAINTENANCE,
                    r.getReportedByPersonId(),
                    International.getString("Bootsschaden behoben") + " - " + r.getBoatAsName(),
                    r.getCompleteDamageInfo() +
                    "\n" +
                    International.getString("behoben von") + ": " + r.getFixedByPersonAsName() + "\n" +
                    International.getString("behoben am") + ": " +
                        (r.getFixDate() != null && r.getFixDate().isSet() ?
                            r.getFixDate().toString() : DataTypeDate.today().toString()) + "\n" +
                    International.getString("Reparaturkosten") + ": " + (r.getRepairCosts() != null ? r.getRepairCosts() : "") + "\n" +
                    International.getString("Versicherungsfall") + ": " + (r.getClaim() ?
                        International.getString("ja") :
                        International.getString("nein") ) + "\n" +
                    International.getString("Bemerkungen") + ": " + (r.getNotes() != null ? r.getNotes() : "")
                    );
        } else if (success && admin != null && dataRecord != null && this.newRecord
        	&& Daten.efaConfig.getValueNotificationNewBoatDamageByAdmin()) {
        	this.sendNotification();
        }
        return success;
    }
    
    public static void newBoatDamage(Window parent, BoatRecord boat) {
        newBoatDamage(parent, boat, null, null);
    }

    public static void newBoatDamage(Window parent, BoatRecord boat, UUID personID, String logbookRecordText) {
        BoatDamages boatDamages = Daten.project.getBoatDamages(false);
        AutoIncrement autoIncrement = Daten.project.getAutoIncrement(false);
        int val = autoIncrement.nextAutoIncrementIntValue(boatDamages.data().getStorageObjectType());
        BoatDamageRecord r = boatDamages.createBoatDamageRecord(boat.getId(), val);
        r.setReportDate(DataTypeDate.today());
        r.setReportTime(DataTypeTime.now());
        r.setShowOnlyAddDamageFields(true);
        if (personID != null) {
            r.setReportedByPersonId(personID);
        }
        if (logbookRecordText != null) {
            r.setLogbookText(logbookRecordText);
        }
        BoatDamageEditDialog dlg = (parent instanceof JDialog ? 
            new BoatDamageEditDialog((JDialog)parent, r, true, null) :
            new BoatDamageEditDialog((JFrame)parent, r, true, null));
        dlg.showDialog();
        if (dlg.getDialogResult()) {
            dlg.sendNotification();
            Dialog.infoDialog(International.getString("Vielen Dank!"),
                              International.getString("Der Bootsschaden wurde gemeldet."));
        }
    }

    public void setFixedWasChanged() {
        fixedWasChanged = true;
    }

    protected void iniDialog() throws Exception {
        super.iniDialog();
        if (fixedWasChanged) {
            ItemTypeBoolean fixed = (ItemTypeBoolean)getItem(BoatDamageRecord.FIXED);
            if (fixed != null) {
                fixed.setChanged();
            }
        }
    }

}
