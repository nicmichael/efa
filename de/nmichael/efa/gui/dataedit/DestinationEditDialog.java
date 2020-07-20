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

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete
public class DestinationEditDialog extends VersionizedDataEditDialog {

    public DestinationEditDialog(Frame parent, DestinationRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, 
                International.getString("Ziel") + " / " +
                International.getString("Strecke"),
                r, newRecord, admin);
        ini4Permissions(admin);
    }

    public DestinationEditDialog(JDialog parent, DestinationRecord r, boolean newRecord, AdminRecord admin) {
        super(parent,
                International.getString("Ziel") + " / " +
                International.getString("Strecke"),
                r, newRecord, admin);
        ini4Permissions(admin);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    private void ini4Permissions(AdminRecord admin) {
        if (admin == null || !admin.isAllowedEditPersons()) {
            setShowVersionPanel(false);
            setPromptToEnterValidity(false);
            allowConflicts = false;
        }
        if (admin != null && !admin.isAllowedEditDestinations()) {
            setForbidChanges();
        }
    }

    protected void iniDefaults() {
        if (newRecord) {
            ((DestinationRecord)dataRecord).setStartIsBoathouse(true);
            ((DestinationRecord)dataRecord).setRoundtrip(true);
            
        }
    }


}
