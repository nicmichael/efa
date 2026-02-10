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
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.ex.*;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.DataPrintRecordDialog;
import de.nmichael.efa.gui.ImagesAndIcons;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.Vector;
import javax.swing.*;

// @i18n complete
public class UnversionizedDataEditDialog extends DataEditDialog {

	protected DataRecord dataRecord;
    protected boolean newRecord;
    protected AdminRecord admin;
    protected boolean _dontSaveRecord = false;
    protected boolean _alwaysCheckValues = false;
    protected boolean allowConflicts = true;
    private boolean forbidChanges = false;
    private JButton printButton;

    public UnversionizedDataEditDialog(Frame parent, String title, 
            DataRecord dataRecord, boolean newRecord, AdminRecord admin) {
        super(parent, title, null);
        this.dataRecord = dataRecord;
        this.newRecord = newRecord;
        this.admin = admin;
        iniDlg(title);
    }

    public UnversionizedDataEditDialog(JDialog parent, String title, 
            DataRecord dataRecord, boolean newRecord, AdminRecord admin) {
        super(parent, title, null);
        this.dataRecord = dataRecord;
        this.newRecord = newRecord;
        this.admin = admin;
        iniDlg(title);
    }

    private void iniDlg(String title) {
        if (admin != null) {
            setPrintButton();
        }
        iniDefaults();
        Vector<IItemType> items = (dataRecord != null ? dataRecord.getGuiItems(admin) : null);
        if (items != null && items.size() > 0) {
            String mainCat = items.get(0).getCategory();
            if (admin != null && mainCat != null) {
                ItemTypeLabel item;
                items.add(item = new ItemTypeLabel("_GUIITEM_GENERIC_RECORDID", IItemType.TYPE_PUBLIC, mainCat,
                        International.getString("interne ID") + ": " + dataRecord.getKeyAsTextDescription()));
                item.setColor(Color.gray);
                item.setPadding(0, 0, 20, 0);
                items.add(item = new ItemTypeLabel("_GUIITEM_GENERIC_LASTMODIFIED", IItemType.TYPE_PUBLIC, mainCat,
                        International.getMessage("zuletzt geändert am {datetime}", EfaUtil.date2String(new Date(dataRecord.getLastModified())))));
                item.setColor(Color.gray);
            }
        }
        setItems(items);
        if (dataRecord != null && !newRecord) {
            setTitle(title + ": " + dataRecord.getQualifiedName());
        }
    }

    protected void setPrintButton() {
        printButton = new JButton();
        printButton.setIcon(BaseDialog.getIcon(ImagesAndIcons.IMAGE_BUTTON_PRINT));
        printButton.setMargin(new Insets(2,2,2,2));
        printButton.setSize(35, 20);
        printButton.setToolTipText(International.getString("Drucken"));
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { printRecord(); }
        });
        this.addComponentToNortheastPanel(printButton);
    }

    protected void removePrintButton() {
        removeComponentFromNortheastPanel(printButton);
    }

    protected void iniDefaults() {
        // implement in subclass if necessary
    }

    protected String findSimilarRecordsOfThisName() {
        return null; // may be overwritten by subclass
    }

    protected void warnIfVersionizedRecordOfThatNameAlreadyExists() throws InvalidValueException {
        String conflict = null;
        boolean identical = true;
        try {
            if (!dataRecord.getPersistence().data().getMetaData().isVersionized()) {
                return;
            }
            DataKey[] keys = dataRecord.getPersistence().data().getByFields(dataRecord.getQualifiedNameFields(),
                    dataRecord.getQualifiedNameValues(dataRecord.getQualifiedName()));
            for (int i=0; keys != null && i<keys.length; i++) {
                DataRecord r = dataRecord.getPersistence().data().get(keys[i]);
                if (!r.getDeleted()) {
                    conflict = r.getQualifiedName() + " (" + r.getValidRangeString() + ")";
                }
            }
            if (conflict == null) {
                conflict = findSimilarRecordsOfThisName();
                identical = false;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        if (conflict != null) {
            if (allowConflicts) {
                String warn = (identical ?
                    International.getString("Es existiert bereits ein gleichnamiger Datensatz!") :
                    International.getString("Es existiert bereits ein ähnlicher Datensatz!"));
                if (Dialog.yesNoDialog(International.getString("Warnung"),
                        warn + "\n"
                        + conflict + "\n"
                        + International.getString("Möchtest Du diesen Datensatz trotzdem erstellen?")) != Dialog.YES) {
                    throw new InvalidValueException(null, null);
                }
            } else {
                Dialog.error(International.getString("Es existiert bereits ein gleichnamiger Datensatz!"));
                throw new InvalidValueException(null, null);
            }
        }
    }

    protected void checkValidValues() throws InvalidValueException {
        for (IItemType item : getItems()) {
            if (!item.isValidInput() && item.isVisible()) {
                throw new InvalidValueException(item, item.getInvalidErrorText());
            }
        }
    }

    protected boolean saveRecord() throws InvalidValueException {
        if (forbidChanges) {
            Dialog.infoDialog(International.getString("Du hast keine Berechtigung, diesen Datensatz zu ändern."));
            return false;
        }
        checkValidValues();
        try {
            dataRecord.saveGuiItems(getItems());
            if (!_dontSaveRecord) {
                if (newRecord) {
                    warnIfVersionizedRecordOfThatNameAlreadyExists();
                    dataRecord.getPersistence().data().add(dataRecord);
                    Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDADDED,
                            dataRecord.getPersistence().getDescription() + ": " +
                            International.getMessage("{name} hat neuen Datensatz '{record}' erstellt.",
                            (admin != null ? International.getString("Admin") + " '"  + admin.getName() + "'":
                                International.getString("Normaler Benutzer")),
                            dataRecord.getQualifiedName()));
                } else {
                    dataRecord.getPersistence().data().update(dataRecord);
                    Logger.log(Logger.INFO, Logger.MSG_DATAADM_RECORDUPDATED,
                            dataRecord.getPersistence().getDescription() + ": " +
                            International.getMessage("{name} hat Datensatz '{record}' geändert.",
                            (admin != null ? International.getString("Admin") + " '" + admin.getName() + "'" :
                                International.getString("Normaler Benutzer")),
                            dataRecord.getQualifiedName()));
                }
                for (IItemType item : getItems()) {
                    item.setUnchanged();
                }
            }
            return true;
        } catch(EfaModifyException emodify) {
            emodify.displayMessage();
            return false;
        } catch(Exception e) {
            Logger.logdebug(e);
            if (e.toString() != null) {
                Dialog.error("Die Änderungen konnten nicht gespeichert werden." + "\n" + e.toString());
            }
            return false;
        }
    }

    boolean checkAndSaveChanges(boolean promptForInvalidValues) {
        if (getValuesFromGui()) {
            if (_dontSaveRecord) {
                try {
                    // this looks as if it doesn't make sense... but it's correct;
                    // we have to call saveRecord() to read the GUI values into the data
                    // record; there is another check in saveRecord() which will not
                    // save the record in the persistence media.
                    // Here in checkAndSaveChanges(), we would just like to skip the
                    // user prompt whether we want to save any changes. When we don't do
                    // a physical save, then we don't need to ask - we just get the GUI
                    // values into the record and that's it!
                    return saveRecord();
                } catch (InvalidValueException einv) {
                    if (promptForInvalidValues) {
                        einv.displayMessage();
                        return false;
                    } else {
                        return true;
                    }
                }
            }
            switch (Dialog.yesNoCancelDialog(International.getString("Änderungen speichern"),
                    International.getString("Die Daten wurden verändert.") + "\n"
                    + International.getString("Möchtest Du die Änderungen jetzt speichern?"))) {
                case Dialog.YES:
                    try {
                        return saveRecord();
                    } catch (InvalidValueException einv) {
                        einv.displayMessage();
                        return false;
                    }
                case Dialog.NO:
                    return true;
                default:
                    return false;
            }
        } else {
            return true;
        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        try {
            if (getValuesFromGui() || this.newRecord) {
                if (!saveRecord()) {
                    return;
                }
            } else if (_alwaysCheckValues) {
                checkValidValues();
            }
        } catch (InvalidValueException einv) {
            einv.displayMessage();
            return;
        }
        super.closeButton_actionPerformed(e);
    }

    public boolean cancel() {
        if (!checkAndSaveChanges(false)) {
            return false;
        }
        return super.cancel();
    }

    protected void printRecord() {
        DataPrintRecordDialog dlg = new DataPrintRecordDialog(this, admin, dataRecord);
        dlg.showDialog();
    }

    protected void setForbidChanges() {
        this.forbidChanges = true;
        Vector<IItemType> items = getItems();
        if (items != null) {
            for (IItemType i : items) {
                i.setEditable(false);
            }
        }
    }
    
    public void setAdmin(AdminRecord theAdmin) {
    	this.admin=theAdmin;
    }

}
