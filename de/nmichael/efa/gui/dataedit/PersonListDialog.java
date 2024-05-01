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
public class PersonListDialog extends DataListDialog {

    public PersonListDialog(Frame parent, long validAt, AdminRecord admin) {
        super(parent, International.getString("Personen"), 
                Daten.project.getPersons(false), validAt, admin);
        if (admin != null && admin.isAllowedEditPersons()) {
            addMergeAction();
        }
    }

    public PersonListDialog(JDialog parent, long validAt, AdminRecord admin) {
        super(parent, International.getString("Personen"), 
                Daten.project.getPersons(false), validAt, admin);
        if (admin != null && admin.isAllowedEditPersons()) {
            addMergeAction();
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null) {
            record = Daten.project.getPersons(false).createPersonRecord(UUID.randomUUID());
        }
        return new PersonEditDialog(parent, (PersonRecord)record, newRecord, admin);
    }
    
    protected ProgressTask getMergeProgressTask(DataKey mainKey, DataKey[] mergeKeys) {
        Persons persons = (Persons)persistence;
        return persons.getMergePersonsProgressTask(mainKey, mergeKeys);
    }

	protected void createSpecificItemTypeRecordTable() {
		
		super.createSpecificItemTypeRecordTable();

		table.addPermanentSecondarySortingColumn(PersonRecord.COLUMN_ID_LAST_NAME);        
		table.addPermanentSecondarySortingColumn(PersonRecord.COLUMN_ID_FIRST_NAME);
		table.addPermanentSecondarySortingColumn(PersonRecord.COLUMN_ID_BIRTHDATE);
		
	}    
    
    
}
