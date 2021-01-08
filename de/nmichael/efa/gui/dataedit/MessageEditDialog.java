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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.ex.InvalidValueException;
import java.awt.*;
import java.awt.event.*;
import java.util.UUID;
import javax.swing.*;

// @i18n complete
public class MessageEditDialog extends UnversionizedDataEditDialog {

    public MessageEditDialog(Frame parent, MessageRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Nachricht"), r, newRecord, admin);
        ini(admin);
    }

    public MessageEditDialog(JDialog parent, MessageRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Nachricht"), r, newRecord, admin);
        ini(admin);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    private void ini (AdminRecord admin) {
        ItemTypeBoolean msgRead = (ItemTypeBoolean)getItem(MessageRecord.READ);
        boolean setMsgRead = false;
        MessageRecord r = null;
        if (msgRead != null && !msgRead.getValue()) {
            r = (MessageRecord)dataRecord;
            if (r.getTo() == null || r.getTo().equals(MessageRecord.TO_ADMIN)) {
                msgRead.setEnabled(admin != null && admin.isAllowedMsgMarkReadAdmin());
                if (admin != null && admin.isAllowedMsgAutoMarkReadAdmin()) {
                    setMsgRead = true;
                }
            } else {
                msgRead.setEnabled(admin != null && admin.isAllowedMsgMarkReadBoatMaintenance());
                if (admin != null && admin.isAllowedMsgAutoMarkReadBoatMaintenance()) {
                    setMsgRead = true;
                }
            }
        }
        if (setMsgRead && r != null) {
            msgRead.setValue(true);
            msgRead.showValue();
            try {
                r.setRead(true);
                r.getPersistence().data().update(r);
                msgRead.setUnchanged();
            } catch (Exception eignore) {
                Logger.logdebug(eignore);
            }

        }
    }

    public void updateGui() {
        super.updateGui();
        if (newRecord && getItem(MessageRecord.FROM) != null) {
            this.setRequestFocus(getItem(MessageRecord.FROM));
        }
    }

    public void showDialog() {
        if (newRecord && getItem(MessageRecord.FROM) != null) {
            this.setRequestFocus(getItem(MessageRecord.FROM));
        }
        super.showDialog();
    }

    protected boolean saveRecord() throws InvalidValueException {
        if (newRecord && dataRecord != null) {
            IItemType item = getItem(MessageRecord.TO);
            if (item != null) {
                String to = item.getValueFromField();
                if (Daten.efaConfig.getValueNotificationMarkReadAdmin()
                        && MessageRecord.TO_ADMIN.equals(to)) {
                    ((MessageRecord)dataRecord).setRead(true);
                }
                if (Daten.efaConfig.getValueNotificationMarkReadBoatMaintenance()
                        && MessageRecord.TO_BOATMAINTENANCE.equals(to)) {
                    ((MessageRecord)dataRecord).setRead(true);
                }
                
                try {
                    ItemTypeStringAutoComplete from = (ItemTypeStringAutoComplete)getItem(MessageRecord.FROM);
                    PersonRecord p = Daten.project.getPersons(false).getPerson((UUID)from.getId(from.getValueFromField()), System.currentTimeMillis());
                    if (p != null && p.getEmail() != null && p.getEmail().trim().length() > 0) {
                        ((MessageRecord)dataRecord).setReplyTo(p.getEmail().trim());
                    }
                } catch (Exception e) {
                    Logger.logdebug(e);
                }
                
            }
        }
        return super.saveRecord();
    }

}
