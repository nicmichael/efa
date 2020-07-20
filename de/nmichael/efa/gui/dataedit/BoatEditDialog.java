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
import de.nmichael.efa.ex.InvalidValueException;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete
public class BoatEditDialog extends VersionizedDataEditDialog {

    public BoatEditDialog(Frame parent, BoatRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Boot"), r, newRecord, admin);
        ini4Permissions(admin);
    }

    public BoatEditDialog(JDialog parent, BoatRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Boot"), r, newRecord, admin);
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
        if (admin != null && !admin.isAllowedEditBoats()) {
            setForbidChanges();
        }
    }

    protected boolean saveRecord() throws InvalidValueException {
        boolean success = super.saveRecord();
        if (success) {
            if (newRecord && dataRecord != null) {
                BoatStatus boatStatus = dataRecord.getPersistence().getProject().getBoatStatus(false);
                if (boatStatus != null) {
                    try {
                        boatStatus.data().add(boatStatus.createBoatStatusRecord(((BoatRecord)dataRecord).getId(), null));
                    } catch(Exception e) {
                        Logger.log(e);
                    }
                }
            }
        }
        return success;
    }
}
