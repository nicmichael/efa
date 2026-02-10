/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.gui.dataedit;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JDialog;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.ex.InvalidValueException;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import java.awt.AWTEvent;

// @i18n complete
public class ProjectEditDialog extends UnversionizedDataEditDialog implements IItemListener {

    Project project;
    String logbookName;
    String clubworkBookName;

    public enum Type {

        project,
        logbook,
        clubwork
    }

    public static String getInternationalProjectTypeString(ProjectRecord p) {
        String PROJECT_TYPE = (p != null ? p.getType() : null);
        if (ProjectRecord.TYPE_PROJECT.equals(PROJECT_TYPE)) {
            return International.getString("Projekt");
        } else if (ProjectRecord.TYPE_CLUB.equals(PROJECT_TYPE)) {
            return International.getString("Verein");
        } else if (ProjectRecord.TYPE_BOATHOUSE.equals(PROJECT_TYPE)) {
            return International.getString("Bootshaus");
        } else if (ProjectRecord.TYPE_LOGBOOK.equals(PROJECT_TYPE)) {
            return International.getString("Fahrtenbuch");
        } else if (ProjectRecord.TYPE_CLUBWORK.equals(PROJECT_TYPE)) {
            return International.getString("Vereinsarbeit");
        } else {
            return International.getString("Projekt");
        }
    }

    public ProjectEditDialog(Frame parent, Project p, int subtype, AdminRecord admin) {
        super(parent, International.getString("Projekt"), null, false, admin);
        iniItems(p, null, subtype);
    }

    public ProjectEditDialog(JDialog parent, Project p, int subtype, AdminRecord admin) {
        super(parent, International.getString("Projekt"), null, false, admin);
        iniItems(p, null, subtype);
    }

    public ProjectEditDialog(Frame parent, Project p, Type type, String projectRecordName, int subtype, AdminRecord admin) {
        super(parent, International.getString("Projekt"), null, false, admin);
        iniItems(p, projectRecordName, type, subtype);
    }

    public ProjectEditDialog(JDialog parent, Project p, Type type, String projectRecordName, int subtype, AdminRecord admin) {
        super(parent, International.getString("Projekt"), null, false, admin);
        iniItems(p, projectRecordName, type, subtype);
    }

    public ProjectEditDialog(Frame parent, Project p, ProjectRecord projectRecord, int subtype, AdminRecord admin) {
        super(parent, getInternationalProjectTypeString(projectRecord), null, false, admin);
        iniItems(p, (projectRecord != null ? projectRecord.getName() : null), typeMapping(projectRecord), subtype);
    }

    public ProjectEditDialog(JDialog parent, Project p, ProjectRecord projectRecord, int subtype, AdminRecord admin) {
        super(parent, getInternationalProjectTypeString(projectRecord), null, false, admin);
        iniItems(p, (projectRecord != null ? projectRecord.getName() : null), typeMapping(projectRecord), subtype);
    }

    public Type typeMapping(ProjectRecord p) {
        String strType = (p != null ? p.getType() : null);
        Type type;
        if (ProjectRecord.TYPE_LOGBOOK.equals(strType)) {
            type = Type.logbook;
        } else if (ProjectRecord.TYPE_CLUBWORK.equals(strType)) {
            type = Type.clubwork;
        } else {
            type = Type.project;
        }
        return type;
    }

    public ProjectEditDialog(JDialog parent, Project p, String logbookName, int subtype,
            String compName, AdminRecord admin) {
        super(parent, International.getString("Projekt"), null, false, admin);
        iniItems(p, logbookName, subtype);
        if (compName != null
                && (compName.equals(WettDefs.STR_DRV_FAHRTENABZEICHEN)
                || compName.equals(WettDefs.STR_DRV_WANDERRUDERSTATISTIK))
                && getItem(ProjectRecord.ASSOCIATIONGLOBALLOGIN) != null) {
            IItemType item = getItem(ProjectRecord.ASSOCIATIONGLOBALLOGIN);
            item.setNotNull(true);
            _alwaysCheckValues = true;
        }
        if (compName != null
                && (compName.equals(WettDefs.STR_LRVBERLIN_SOMMER)
                || compName.equals(WettDefs.STR_LRVBERLIN_WINTER)
                || compName.equals(WettDefs.STR_LRVBERLIN_BLAUERWIMPEL))
                && getItem(ProjectRecord.ASSOCIATIONREGIONALLOGIN) != null) {
            IItemType item = getItem(ProjectRecord.ASSOCIATIONREGIONALLOGIN);
            item.setNotNull(true);
            _alwaysCheckValues = true;
        }
    }

    private void iniItems(Project p, String logbookName, int subtype) {
        iniItems(p, logbookName, null, subtype);
    }

    private void iniItems(Project p, String projectRecordName, Type type, int subtype) {
        this.project = p;
        if (type == null || type == Type.logbook) {
            this.logbookName = projectRecordName;
        } else if (type == Type.clubwork) {
            this.clubworkBookName = projectRecordName;
        }
        removePrintButton();

        Vector<IItemType> guiItems = new Vector<IItemType>();
        try {
            ProjectRecord r;
            if (type == Type.clubwork) {
                r = p.getClubworkBookRecord(clubworkBookName);
                if (r != null) {
                    guiItems.addAll(r.getGuiItems(admin, subtype, null, false));
                }
            } else if (type == Type.logbook) {
                r = p.getLoogbookRecord(logbookName);
                if (r != null) {
                    guiItems.addAll(r.getGuiItems(admin, subtype, null, false));
                }
            } else {
                r = p.getProjectRecord();
                if (r != null) {
                    guiItems.addAll(r.getGuiItems(admin, subtype, null, false));
                }
                r = p.getClubRecord();
                if (r != null) {
                    guiItems.addAll(r.getGuiItems(admin, subtype, null, false));
                }
                String[] logbooks = p.getAllLogbookNames();
                for (int i = 0; logbooks != null && i < logbooks.length; i++) {
                    r = p.getLoogbookRecord(logbooks[i]);
                    Vector<IItemType> v = r.getGuiItems(admin, subtype, null, false);
                    for (int j = 0; j < v.size(); j++) {
                        IItemType item = v.get(j);
                        item.setName(r.getKey().toString() + ":" + item.getName());
                        guiItems.add(item);
                    }
                }
                String[] boathouses = p.getAllBoathouseNames();
                for (int i = 0; boathouses != null && i < boathouses.length; i++) {
                    r = p.getBoathouseRecord(boathouses[i]);
                    Vector<IItemType> v = r.getGuiItems(admin, subtype, null, false);
                    for (int j = 0; j < v.size(); j++) {
                        IItemType item = v.get(j);
                        item.setName(r.getKey().toString() + ":" + item.getName());
                        guiItems.add(item);
                    }
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }

        this.setItems(guiItems);

        for (IItemType item : getItems()) {
            if (item.getName().endsWith(ProjectRecord.GUIITEM_BOATHOUSE_ADD)) {
                item.registerItemListener(this);
            }
            if (item.getName().endsWith(ProjectRecord.GUIITEM_BOATHOUSE_DELETE)) {
                item.registerItemListener(this);
            }
            if (item.getName().endsWith(ProjectRecord.GUIITEM_BOATHOUSE_SETDEFAULT)) {
                item.registerItemListener(this);
            }
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType.getName().endsWith(ProjectRecord.GUIITEM_BOATHOUSE_ADD)
                && event.getID() == ActionEvent.ACTION_PERFORMED) {
            String boathouseName = Dialog.inputDialog(International.getString("Bootshaus hinzufügen"),
                    International.getString("Name des Bootshauses"));
            if (boathouseName != null) {
                ProjectRecord r = project.createNewBoathouseRecord(boathouseName);
                if (r == null) {
                    return;
                }
                try {
                    int boathousesBefore = project.getNumberOfBoathouses();
                    ProjectRecord curBths = project.getBoathouseRecord();
                    project.addRecord(r, ProjectRecord.TYPE_BOATHOUSE);
                    Vector<IItemType> items = getItems();
                    Vector<IItemType> itemsNew = r.getGuiItems(admin);
                    for (IItemType item : itemsNew) {
                        item.setName(r.getKey().toString() + ":" + item.getName());
                        item.registerItemListener(this);
                    }
                    items.addAll(itemsNew);

                    if (boathousesBefore == 1 && curBths != null) {
                        // now also add additional fields for previous boathouse
                        itemsNew = curBths.getGuiItems(admin);
                        for (IItemType item : itemsNew) {
                            item.setName(curBths.getKey().toString() + ":" + item.getName());
                            if (getItem(item.getName()) == null) {
                                item.registerItemListener(this);
                                items.add(item);
                            }
                        }
                    }

                    setItems(items);
                    updateGui();
                } catch (EfaException e) {
                    Dialog.error(e.getMessage());
                    return;
                }
            }
        }
        if (itemType.getName().endsWith(ProjectRecord.GUIITEM_BOATHOUSE_DELETE)
                && event.getID() == ActionEvent.ACTION_PERFORMED) {
            ProjectRecord r = project.getRecord(itemType.getDataKey());
            if (r != null && Dialog.yesNoDialog(International.getString("Bootshaus entfernen"),
                    International.getMessage("Möchtest Du das Bootshaus '{name}' wirklich entfernen?",
                    r.getName())) != Dialog.YES) {
                return;
            }
            try {
                project.data().delete(r.getKey());
                String cat = itemType.getCategory();
                Vector<IItemType> items = getItems();
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).getCategory().equals(cat)) {
                        items.remove(i--);
                    }
                }
                setItems(items);
                updateGui();
            } catch (EfaException e) {
                Dialog.error(e.getMessage());
                return;
            }
        }
        if (itemType.getName().endsWith(ProjectRecord.GUIITEM_BOATHOUSE_SETDEFAULT)
                && event.getID() == ActionEvent.ACTION_PERFORMED) {
            String name = itemType.getName();
            int pos = name.indexOf(":");
            if (pos > 0) {
                name = name.substring(0, pos) + ":" + ProjectRecord.BOATHOUSE_IDENTIFIER;
                IItemType item = this.getItem(name);
                if (item != null) {
                    ((ItemTypeString) item).parseAndShowValue(project.getMyIdentifier());
                }
            }

        }
    }

    protected boolean saveRecord() throws InvalidValueException {
        for (IItemType item : getItems()) {
            if (!item.isValidInput() && item.isVisible()) {
                throw new InvalidValueException(item, item.getInvalidErrorText());
                // @todo (P4) make sure that if dates of logbook is changed, that all sessions are still within the range!
            }
        }
        try {
            // find all DataKey's of records to be updated
            Hashtable<DataKey, String> dataKeys = new Hashtable<DataKey, String>();
            for (IItemType item : getItems()) {
                dataKeys.put(item.getDataKey(), "foo");
            }
            DataKey[] keys = dataKeys.keySet().toArray(new DataKey[0]);

            // find all records and update them
            for (int i = 0; i < keys.length; i++) {
                // get all items with this key
                DataKey k = keys[i];
                Vector<IItemType> ki = new Vector<IItemType>();
                for (IItemType item : getItems()) {
                    if (item.getDataKey().equals(k)) {
                        int pos = item.getName().indexOf(":");
                        if (pos >= 0) {
                            item.setName(item.getName().substring(pos + 1));
                        }
                        ki.add(item);
                    }
                }
                ProjectRecord r = project.getRecord(k);
                if (r != null) {
                    // r can be null for remote projects which aren't yet open
                    r.saveGuiItems(ki);
                    if (k.equals(r.getKey())) {
                        project.getMyDataAccess(r.getType()).update(r);
                    } else {
                        IDataAccess _dataAccess = project.getMyDataAccess(r.getType());
                        if (_dataAccess != null) {
                            try {
                                _dataAccess.setPreModifyRecordCallbackEnabled(false);
                                _dataAccess.delete(k);
                                _dataAccess.add(r);
                            } catch (Exception e) {
                                _dataAccess.add(r);
                            } finally {
                                _dataAccess.setPreModifyRecordCallbackEnabled(true);
                            }
                        }
                    }
                }
            }
            for (IItemType item : getItems()) {
                item.setUnchanged();
            }
            return true;
        } catch (EfaModifyException emodify) {
            emodify.displayMessage();
            return false;
        } catch (Exception e) {
            Logger.logdebug(e);
            Dialog.error("Die Änderungen konnten nicht gespeichert werden." + "\n" + e.toString());
            return false;
        }
    }
}
