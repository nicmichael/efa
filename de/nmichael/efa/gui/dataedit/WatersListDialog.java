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
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


// @i18n complete
public class WatersListDialog extends DataListDialog {

    public static final int ACTION_CREATEFROMTEMPLATE = 901; // negative actions will not be shown as popup actions

    public WatersListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.getString("Gewässer"), Daten.project.getWaters(false), 0, admin);
        addCreateWatersButton();
        intelligentColumnWidth = false;
    }

    public WatersListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.getString("Gewässer"), Daten.project.getWaters(false), 0, admin);
        addCreateWatersButton();
        intelligentColumnWidth = false;
    }

    private void addCreateWatersButton() {
        try {
            if (Daten.project.getWaters(false).getResourceTemplate(International.getLanguageID()) != null) {
                addAction(International.getString("Gewässer aktualisieren"),
                        ACTION_CREATEFROMTEMPLATE,
                        BaseDialog.IMAGE_SPECIAL);
            }
        } catch(Exception eignore) {
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void itemListenerActionTable(int actionId, DataRecord[] records) {
        super.itemListenerActionTable(actionId, records);
        switch(actionId) {
            case ACTION_CREATEFROMTEMPLATE:
            	
            	if (Daten.project != null && Daten.project.isOpen() && Daten.project.getIsProjectStorageTypeEfaCloud()) {
            		if (Dialog.yesNoDialog(International.getString("Gewässer aktualisieren"), 
            				International.getString("ACHTUNG")+"!\n\n"
            				+International.getString("Das aktuell geladene Projekt basiert auf efaCloud. In efaCloud Umgebungen sollte die Aktualisierung der Gewässer nur auf einer Station erfolgen. Die anderen Stationen erhalten die Aktualisierung über die efaCloud-Synchronisation.")
            				+"\n\n" +International.getString("Gewässer aktualisieren")+"?") !=  Dialog.YES) {
            			// nothing todo when user said no
            			return;
            		}
            	}
            	
                int count = Daten.project.getWaters(false).addAllWatersFromTemplate(International.getLanguageID());
                if (count > 0) {
                    Dialog.infoDialog(International.getMessage("{count} Gewässer aus Gewässerkatalog erfolgreich hinzugefügt oder aktualisiert.",
                            count));
                } else {
                    Dialog.infoDialog(International.getString("Alle Gewässer aus dem Gewässerkatalog sind bereits vorhanden (keine neuen hinzugefügt)."));
                }
                break;
        }
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null) {
            record = Daten.project.getWaters(false).createWatersRecord(UUID.randomUUID());
        }
        return new WatersEditDialog(parent, (WatersRecord)record, newRecord, admin);
    }
}
