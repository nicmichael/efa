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
import de.nmichael.efa.util.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


// @i18n complete
public class DestinationListDialog extends DataListDialog {

    public DestinationListDialog(Frame parent, long validAt, AdminRecord admin) {
        super(parent,
                International.getString("Ziele") + " / " +
                International.getString("Strecken"),
                Daten.project.getDestinations(false), validAt, admin);
        if (admin != null && admin.isAllowedEditDestinations()) {
            addMergeAction();
        }
    }

    public DestinationListDialog(JDialog parent, long validAt, AdminRecord admin) {
        super(parent,
                International.getString("Ziele") + " / " +
                International.getString("Strecken"),
                Daten.project.getDestinations(false), validAt, admin);
        if (admin != null && admin.isAllowedEditDestinations()) {
            addMergeAction();
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null) {
            record = Daten.project.getDestinations(false).createDestinationRecord(UUID.randomUUID());
        }
        return new DestinationEditDialog(parent, (DestinationRecord)record, newRecord, admin);
    }

    protected ProgressTask getMergeProgressTask(DataKey mainKey, DataKey[] mergeKeys) {
        Destinations destinations = (Destinations)persistence;
        return destinations.getMergeDestinationsProgressTask(mainKey, mergeKeys);
    }

}
