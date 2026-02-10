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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;
import javax.swing.JPanel;

import com.formdev.flatlaf.util.SwingUtils;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.Admins;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;


// @i18n complete
public class AdminListDialog extends DataListDialog {

    public static final int ACTION_EFALIVE_ADMIN = 901; // negative actions will not be shown as popup actions
    public static final String EFACLOUD_HINT = "EFACLOUD_HINT";
    private Admins admins;
    private boolean efaLiveRepair = false;

    public AdminListDialog(Frame parent, Admins admins, AdminRecord admin) {
        super(parent, International.getString("Administratoren"), admins, 0, admin);
        this.admins = admins;
        ini();
    }

    public AdminListDialog(JDialog parent, Admins admins, AdminRecord admin) {
        super(parent, International.getString("Administratoren"), admins, 0, admin);
        this.admins = admins;
        ini();
    }

    private void ini() {
        if (Daten.admins.isEfaLiveAdminOk()) {
            actionText = null; // only ADD, EDIT, DELETE (no IMPORT, EXPORT)
            actionType = null; // only ADD, EDIT, DELETE (no IMPORT, EXPORT)
        } else {
            efaLiveRepair = Daten.admins.isEfaLiveAdminExists();
            //EFA#78/Issue#142: If no project is open, we should not check if the project is efaCloud based.
            if ((Daten.project != null) && (Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD)) {
                actionText = new String[]{
                        ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
                        ItemTypeDataRecordTable.ACTIONTEXT_DELETE,
                        (efaLiveRepair
                                ? International.getMessage("Admin '{name}' reparieren", Admins.EFALIVEADMIN)
                                : International.getMessage("Admin '{name}' erstellen", Admins.EFALIVEADMIN))
                };
                actionType = new int[]{
                        ItemTypeDataRecordTable.ACTION_EDIT,
                        ItemTypeDataRecordTable.ACTION_DELETE,
                        ACTION_EFALIVE_ADMIN
                };
                actionImage = new String[]{
                        IMAGE_EDIT,
                        IMAGE_DELETE,
                        IMAGE_REPAIR
                };
            } else {
                actionText = new String[]{
                        ItemTypeDataRecordTable.ACTIONTEXT_NEW,
                        ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
                        ItemTypeDataRecordTable.ACTIONTEXT_DELETE,
                        (efaLiveRepair
                        ? International.getMessage("Admin '{name}' reparieren", Admins.EFALIVEADMIN)
                        : International.getMessage("Admin '{name}' erstellen", Admins.EFALIVEADMIN))
                        };
                actionType = new int[]{
                        ItemTypeDataRecordTable.ACTION_NEW,
                        ItemTypeDataRecordTable.ACTION_EDIT,
                        ItemTypeDataRecordTable.ACTION_DELETE,
                        ACTION_EFALIVE_ADMIN
                        };
                actionImage = new String[]{
                        IMAGE_ADD,
                        IMAGE_EDIT,
                        IMAGE_DELETE,
                        IMAGE_REPAIR
                        };
            }
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void itemListenerActionTable(int actionId, DataRecord[] records) {
        super.itemListenerActionTable(actionId, records);
        switch(actionId) {
            case ACTION_EFALIVE_ADMIN:
                if (Daten.admins.createOrFixEfaLiveAdmin()) {
                    Dialog.infoDialog( (efaLiveRepair ?
                        International.getMessage("Admin '{name}' erfolgreich repariert", Admins.EFALIVEADMIN) :
                        International.getMessage("Admin '{name}' erfolgreich erstellt", Admins.EFALIVEADMIN) ));
                } else {
                    Dialog.error(LogString.operationFailed(International.getString("Operation")));
                }
                break;
        }
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null) {
            record = admins.createAdminRecord(null, null);
        }
        return new AdminEditDialog(parent, (AdminRecord)record, newRecord, admin);
    }

    protected void iniControlPanel() {
    	// we want to put an additional element after the control panel
    	super.iniControlPanel();
    	this.iniEfaCloudHints();
    }
    
	private void iniEfaCloudHints() {

		if (Daten.project != null && Daten.project.isOpen() && Daten.project.getIsProjectStorageTypeEfaCloud()) {
			JPanel myControlPanel= new JPanel();
	    	
	    	ItemTypeLabel item = EfaGuiUtils.createHintWordWrap(EFACLOUD_HINT, IItemType.TYPE_PUBLIC, "",
	    			International.getString("In efaCloud Instanzen werden die Administratoren auf dem efaCloud Server verwaltet. Daher wird hier nur der Superadmin 'admin' angezeigt. Siehe efa Dokuwiki für mehr Informationen.")
	    			,5,10,10,700);

	    	item.setPadding(10, 10, 10, 10);
	    	item.setFieldGrid(3,1,GridBagConstraints.EAST, GridBagConstraints.BOTH);
	    	item.displayOnGui(this, myControlPanel, 0, 0);
	        mainPanel.add(myControlPanel, BorderLayout.NORTH);
			
		}
	}    
    
}
