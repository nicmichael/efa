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
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


// @i18n complete
public class MessageListDialog extends DataListDialog {

    public static final int ACTION_MARKREAD = 901; // negative actions will not be shown as popup actions
    public static final int ACTION_FORWARD  = 902; // negative actions will not be shown as popup actions

    private AdminRecord admin;

    public MessageListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Nachrichten"), Daten.project.getMessages(false), 0, admin);
        this.admin = admin;
        ini();
    }

    public MessageListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Nachrichten"), Daten.project.getMessages(false), 0, admin);
        this.admin = admin;
        ini();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    private void ini() {
        super.sortAscending = false;
        boolean readAdmin = false;
        boolean readBoatMaintenance = false;
        if (admin != null && admin.isAllowedMsgReadAdmin()) {
            readAdmin = true;
        }
        if (admin != null && admin.isAllowedMsgReadBoatMaintenance()) {
            readBoatMaintenance = true;
        }
        if (!readAdmin || !readBoatMaintenance) {
            if (readAdmin) {
                this.filterFieldName = MessageRecord.TO;
                this.filterFieldValue = MessageRecord.TO_ADMIN;
            }
            if (readBoatMaintenance) {
                this.filterFieldName = MessageRecord.TO;
                this.filterFieldValue = MessageRecord.TO_BOATMAINTENANCE;
            }
            if (!readAdmin && !readBoatMaintenance) {
                this.filterFieldName = MessageRecord.TO;
                this.filterFieldValue = "nothing";
            }
        }

        actionText = new String[]{
                    ItemTypeDataRecordTable.ACTIONTEXT_NEW,
                    ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
                    ItemTypeDataRecordTable.ACTIONTEXT_DELETE,
                    International.getString("Weiterleiten"),
                    International.getString("als gelesen markieren"),
                    International.getString("Importieren"),
                    International.getString("Exportieren"),
                    International.getString("Liste ausgeben")
                };
        actionType = new int[]{
                    ItemTypeDataRecordTable.ACTION_NEW,
                    ItemTypeDataRecordTable.ACTION_EDIT,
                    ItemTypeDataRecordTable.ACTION_DELETE,
                    ACTION_FORWARD,
                    ACTION_MARKREAD,
                    ACTION_IMPORT,
                    ACTION_EXPORT,
                    ACTION_PRINTLIST
                };
        actionImage = new String[]{
                    IMAGE_ADD,
                    IMAGE_EDIT,
                    IMAGE_DELETE,
                    IMAGE_MFORWARD,
                    IMAGE_MARKREAD,
                    IMAGE_IMPORT,
                    IMAGE_EXPORT,
                    IMAGE_LIST
                };
    }

    public void itemListenerActionTable(int actionId, DataRecord[] records) {
        super.itemListenerActionTable(actionId, records);
        switch(actionId) {
            case ACTION_FORWARD:
                if (records == null || records.length == 0 || records[0] == null || admin == null) {
                    return;
                }
                boolean origToAdmin = (((MessageRecord)records[0]).getTo() == null || ((MessageRecord)records[0]).getTo().equals(MessageRecord.TO_ADMIN));
                if (Dialog.yesNoCancelDialog(International.getString("Nachrichten weiterleiten"),
                        International.getMessage("Möchtest Du {count} Nachrichten an {recipient} weiterleiten?",
                        records.length,
                        (origToAdmin ?
                            International.getString("Bootswart") :
                            International.getString("Administrator")))) != Dialog.YES) {
                    return;
                }
                try {
                    for (int i = 0; records != null && i < records.length; i++) {
                        MessageRecord r = ((MessageRecord)records[i]);
                        if (r != null && 
                                ( (origToAdmin && (r.getTo() == null || r.getTo().equals(MessageRecord.TO_ADMIN))) ||
                                  (!origToAdmin && r.getTo() != null && r.getTo().equals(MessageRecord.TO_BOATMAINTENANCE)) ) ) {
                            // forward message
                            MessageRecord fwd = ((Messages)persistence).createMessageRecord();
                            fwd.setFrom(r.getFrom());
                            fwd.setTo(origToAdmin ? MessageRecord.TO_BOATMAINTENANCE : MessageRecord.TO_ADMIN);
                            fwd.setSubject("Fwd: " + r.getSubject());
                            fwd.setText(International.getMessage("Weitergeleitet von {name}", admin.getName()) + ":\n\n" + r.getText());
                            fwd.setForceNewMsg(true);
                            /*((Messages)persistence).createAndSaveMessageRecord(r.getFrom(),
                                    (origToAdmin ? MessageRecord.TO_BOATMAINTENANCE : MessageRecord.TO_ADMIN),
                                    (String)null,
                                    r.getSubject(),
                                    International.getMessage("Weitergeleitet von {name}", admin.getName()) + ":\n\n" + r.getText());
                            */
                            DataEditDialog dlg = createNewDataEditDialog(this, persistence, fwd, true);
                            
                            dlg.showDialog();
                            if (dlg.getDialogResult()) {
                                //persistence.data().add(records[i]);
                            }
                            // mark original message as read, if allowed
                            if (!r.getRead() &&
                                 ( ((r.getTo() == null || r.getTo().equals(MessageRecord.TO_ADMIN)) && admin.isAllowedMsgMarkReadAdmin()) ||
                                   ((r.getTo() != null && r.getTo().equals(MessageRecord.TO_BOATMAINTENANCE)) && admin.isAllowedMsgMarkReadBoatMaintenance()) )) {
                                ((MessageRecord) records[i]).setRead(true);
                                persistence.data().update(records[i]);
                            }
                        }
                    }
                } catch (EfaModifyException exmodify) {
                    exmodify.displayMessage();
                } catch (Exception ex) {
                    Logger.logdebug(ex);
                    Dialog.error(ex.toString());
                }
                break;
            case ACTION_MARKREAD:
                if (records == null || records.length == 0 || records[0] == null || admin == null) {
                    return;
                }
                try {
                    for (int i = 0; records != null && i < records.length; i++) {
                        MessageRecord r = ((MessageRecord)records[i]);
                        if (r != null && !r.getRead()) {
                            if ( ((r.getTo() == null || r.getTo().equals(MessageRecord.TO_ADMIN)) && admin.isAllowedMsgMarkReadAdmin()) ||
                                 ((r.getTo() != null && r.getTo().equals(MessageRecord.TO_BOATMAINTENANCE)) && admin.isAllowedMsgMarkReadBoatMaintenance()) ) {
                                ((MessageRecord) records[i]).setRead(true);
                                persistence.data().update(records[i]);
                            }
                        }
                    }
                } catch (EfaModifyException exmodify) {
                    exmodify.displayMessage();
                } catch (Exception ex) {
                    Logger.logdebug(ex);
                    Dialog.error(ex.toString());
                }
                break;
        }
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null) {
            record = Daten.project.getMessages(false).createMessageRecord();
        }
        return new MessageEditDialog(parent, (MessageRecord)record, newRecord, admin);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record, boolean newRecord) {
        if (record == null) {
            record = Daten.project.getMessages(false).createMessageRecord();
        }
        return new MessageEditDialog(parent, (MessageRecord)record, newRecord, admin);
    }

}
