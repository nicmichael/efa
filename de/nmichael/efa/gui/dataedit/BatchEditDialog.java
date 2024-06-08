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
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDateTime;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.BaseTabbedDialog;
import de.nmichael.efa.gui.BrowserDialog;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.*;

public class BatchEditDialog extends BaseTabbedDialog implements IItemFactory, IItemListener {

    private static final String GUIITEM_EDITCONDITION = "GUIITEM_EDITCONDITION";
    private static final String GUIITEM_EDITFIELDS    = "GUIITEM_EDITFIELDS";
    private static final String GUIITEM_VERSIONACTION = "GUIITEM_VERSIONACTION";
    private static final String GUIITEM_VERSIONVALID  = "GUIITEM_VERSIONVALID";

    private static final String OP_EQUAL          = "equal";
    private static final String OP_UNEQUAL        = "unequal";
    private static final String OP_CONTAINS       = "contains";
    private static final String OP_LESS           = "less";
    private static final String OP_LESSEQUAL      = "lessequal";
    private static final String OP_GREATER        = "greater";
    private static final String OP_GREATEREQUAL   = "greaterequal";

    private static final String EA_SETVALUE       = "setvalue";
    private static final String EA_ADDTOLIST      = "addtolist";
    private static final String EA_DELETEFROMLIST = "deletefromlist";

    private static final String VA_EDITCURRENT    = "editcurrent";
    private static final String VA_NEWVERSION     = "newversion";

    private static final String[] OPERATOR_VALUES = new String[] {
        OP_EQUAL,
        OP_UNEQUAL,
        OP_CONTAINS,
        OP_GREATER,
        OP_GREATEREQUAL,
        OP_LESS,
        OP_LESSEQUAL
    };
    private static final String[] OPERATOR_DISPLAY = new String[] {
        International.getString("gleich"),
        International.getString("ungleich"),
        International.getString("enthält"),
        ">",
        ">=",
        "<",
        "<="
    };

    private StorageObject persistence;
    private AdminRecord admin;
    private long validAt;
    private boolean isVersionized = false;
    private DataRecord dummyRecord;
    private Hashtable<String,IItemType> dummyRecordGuiItems;
    private String[] fieldsCondition;
    private String[] fieldDescriptionCondition;
    private String[] fieldsEdit;
    private String[] fieldDescriptionEdit;

    private ItemTypeItemList editConditions;
    private ItemTypeItemList editFields;
    private ItemTypeStringList editAction;
    private ItemTypeStringList versionAction;
    private ItemTypeDateTime versionValidFrom;

    public BatchEditDialog(Frame parent, StorageObject persistence, long validAt, AdminRecord admin) {
        super(parent, International.getStringWithMnemonic("Bearbeitungsassistent"),
                International.getStringWithMnemonic("Vorschau der Änderungen") + " ...",
                null, true);
        this.admin = admin;
        setPersistence(persistence, validAt);
    }

    public BatchEditDialog(JDialog parent, StorageObject persistence, long validAt, AdminRecord admin) {
        super(parent, International.getStringWithMnemonic("Bearbeitungsassistent"),
                International.getStringWithMnemonic("Vorschau der Änderungen") + " ...",
                null, true);
        this.admin = admin;
        setPersistence(persistence, validAt);
    }

    public void setPersistence(StorageObject persistence, long validAt) {
        this.persistence = persistence;
        this.validAt = (validAt <= 0 ? System.currentTimeMillis() : validAt);
        this.isVersionized = persistence.data().getMetaData().isVersionized();
        this.dummyRecord = persistence.createNewRecord();
        this.dummyRecordGuiItems = new Hashtable<String,IItemType>();
        Vector <IItemType> _items = dummyRecord.getGuiItems(admin);
        for (int i=0; _items != null && i<_items.size(); i++) {
            dummyRecordGuiItems.put(_items.get(i).getName(), _items.get(i));
        }
        String[] fields = dummyRecord.getFieldNamesForTextExport(true);
        Vector<String> showFieldsCondition = new Vector<String>();
        Vector<String> showDescriptionsCondition = new Vector<String>();
        Vector<String> showFieldsEdit = new Vector<String>();
        Vector<String> showDescriptionsEdit = new Vector<String>();
        Vector<IItemType> items = persistence.createNewRecord().getGuiItems(admin);
        for (int i=0; i<fields.length; i++) {
            IItemType item = null;
            for (int j=0; items != null && j<items.size(); j++) {
                if (items.get(j).getName().equals(fields[i])) {
                    item = items.get(j);
                    break;
                }
            }
            if (!fields[i].equals(DataRecord.LASTMODIFIED) &&
                !fields[i].equals(DataRecord.CHANGECOUNT) &&
                !fields[i].equals(DataRecord.VALIDFROM) &&
                !fields[i].equals(DataRecord.INVALIDFROM) &&
                !fields[i].equals(DataRecord.INVISIBLE) &&
                !fields[i].equals(DataRecord.DELETED) &&
                !fields[i].equals("Id")) {
                showFieldsCondition.add(fields[i]);
                showDescriptionsCondition.add(fields[i] + (item != null ? " (" +  item.getDescription() + ")" : ""));
                try {
                    if (persistence.data().getFieldType(fields[i]) != IDataAccess.DATA_VIRTUAL) {
                        showFieldsEdit.add(fields[i]);
                        showDescriptionsEdit.add(fields[i] + (item != null ? " (" + item.getDescription() + ")" : ""));
                    }
                } catch (Exception eignore) {
                }
            }
        }
        fieldsCondition = showFieldsCondition.toArray(new String[0]);
        fieldDescriptionCondition = showDescriptionsCondition.toArray(new String[0]);
        fieldsEdit = showFieldsEdit.toArray(new String[0]);
        fieldDescriptionEdit = showDescriptionsEdit.toArray(new String[0]);
    }

    protected void iniDialog() throws Exception {
        Vector<IItemType> guiItems = new Vector<IItemType>();
        String versionizedInfo = "";
        if (isVersionized) {
            versionizedInfo = "(" + International.getString("gültig am") + " " +
                    DataTypeDate.getDateTimeString(new DataTypeDate(validAt), new DataTypeTime(validAt)) + ")";
        }

        ItemTypeLabel l1 = new ItemTypeLabel("INFO", IItemType.TYPE_PUBLIC, makeCategory(null),
                International.getString("Bearbeite alle Datensätze") + " " +
                versionizedInfo + " ...");
        guiItems.add(l1);

        Vector<IItemType[]> itemListConditions = new Vector<IItemType[]>();
        itemListConditions.add(getDefaultItems(GUIITEM_EDITCONDITION));
        editConditions = new ItemTypeItemList(GUIITEM_EDITCONDITION, itemListConditions, this,
                IItemType.TYPE_PUBLIC, makeCategory(null), "... " + International.getString("die folgende Bedingungen erfüllen"));
        editConditions.setPadYbetween(0);
        editConditions.setRepeatTitle(false);
        editConditions.setXForAddDelButtons(5);
        editConditions.setItemsOrientation(ItemTypeItemList.Orientation.horizontal);
        editConditions.setPadding(0, 0, 20, 0);
        guiItems.add(editConditions);

        Vector<IItemType[]> itemListFields = new Vector<IItemType[]>();
        itemListFields.add(getDefaultItems(GUIITEM_EDITFIELDS));
        editFields = new ItemTypeItemList(GUIITEM_EDITFIELDS, itemListFields, this,
                IItemType.TYPE_PUBLIC, makeCategory(null), "... " + International.getString("und ändere folgende Felder"));
        editFields.setMinNumberOfItems(1);
        editFields.setPadYbetween(0);
        editFields.setRepeatTitle(false);
        editFields.setXForAddDelButtons(5);
        editFields.setItemsOrientation(ItemTypeItemList.Orientation.horizontal);
        editFields.setPadding(0, 0, 20, 0);
        guiItems.add(editFields);

        if (isVersionized) {
            versionAction = new ItemTypeStringList(GUIITEM_VERSIONACTION, VA_NEWVERSION,
                        new String[] { VA_EDITCURRENT, VA_NEWVERSION },
                        new String[] {
                            International.getString("aktualisiere die zum angegebenen Zeitpunkt gültige Version"),
                            International.getString("erstelle eine neue Version mit angegebenem Gültigkeitsbeginn")
                        },
                    IItemType.TYPE_PUBLIC, makeCategory(null),
                    International.getString("Änderungsmodus"));
            versionAction.registerItemListener(this);
            versionAction.setPadding(0, 0, 20, 0);
            versionAction.setFieldGrid(4, -1, GridBagConstraints.HORIZONTAL);
            guiItems.add(versionAction);
            versionValidFrom = new ItemTypeDateTime(GUIITEM_VERSIONVALID,
                    DataTypeDate.today(), DataTypeTime.now(),
                    IItemType.TYPE_PUBLIC, makeCategory(null),
                    International.getString("Neue Version gültig ab"));
            versionValidFrom.setFieldGrid(4, -1, GridBagConstraints.HORIZONTAL);
            guiItems.add(versionValidFrom);
        }
        super.setItems(guiItems);
        super.iniDialog();
        if (closeButton != null) {
            closeButton.setIcon(getIcon(BaseDialog.IMAGE_PREVIEW));
            closeButton.setIconTextGap(10);
        }

        // allow some vertical room to add more filter or edit elements
        Dimension dim = mainPanel.getMinimumSize();
        dim.height = dim.height + 200;
        mainPanel.setMinimumSize(dim);
    }
    
    public IItemType[] getDefaultItems(String itemName) {
        if (itemName.equals(GUIITEM_EDITCONDITION)) {
            IItemType[] items = new IItemType[3];
            items[0] = new ItemTypeStringList("FIELD", fieldsCondition[0],
                    fieldsCondition, fieldDescriptionCondition,
                    IItemType.TYPE_PUBLIC, "", null);
            items[0].registerItemListener(this);
            items[1] = new ItemTypeStringList("OPERATOR", OP_EQUAL,
                    OPERATOR_VALUES, OPERATOR_DISPLAY,
                    IItemType.TYPE_PUBLIC, "", null);
            items[1].setFieldSize(120, -1);
            items[2] = //new ItemTypeString("VALUE", "", IItemType.TYPE_PUBLIC, "", null);
                    new ItemTypeStringAutoComplete("VALUE", "", IItemType.TYPE_PUBLIC, "", null, false);
            return items;
        }
        if (itemName.equals(GUIITEM_EDITFIELDS)) {
            IItemType[] items = new IItemType[3];
            items[0] = new ItemTypeStringList("FIELD", fieldsEdit[0],
                    fieldsEdit, fieldDescriptionEdit,
                    IItemType.TYPE_PUBLIC, "", null);
            items[0].registerItemListener(this);
            items[1] = new ItemTypeStringList("ACTION", EA_SETVALUE,
                    new String[] { EA_SETVALUE, EA_ADDTOLIST, EA_DELETEFROMLIST },
                    new String[] {
                            International.getString("neuer Wert") + ":",
                            International.getString("Wert zu Liste hinzufügen") + ":",
                            International.getString("Wert in Liste löschen") + ":"
                    },
                    IItemType.TYPE_PUBLIC, "", null);
            items[1].setFieldSize(120, -1);
            items[2] = //new ItemTypeString("VALUE", "", IItemType.TYPE_PUBLIC, "", null);
                    new ItemTypeStringAutoComplete("VALUE", "", IItemType.TYPE_PUBLIC, "", null, false);
            return items;
        }
        return null;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType.getName().equals(GUIITEM_VERSIONACTION)) {
            String value = itemType.getValueFromField();
            versionValidFrom.setEnabled(!value.equals(VA_EDITCURRENT));
            versionValidFrom.setEditable(!value.equals(VA_EDITCURRENT));
        }
        if (itemType.getName().startsWith(GUIITEM_EDITCONDITION + "_") || // GUIITEM_EDITCONDITION_0_FIELD
            itemType.getName().startsWith(GUIITEM_EDITFIELDS + "_")) {    // GUIITEM_EDITFIELDS_0_FIELD
            try {
                AutoCompleteList autoCompleteList = null;
                ItemTypeItemList itemList = null;
                if (itemType.getName().startsWith(GUIITEM_EDITCONDITION)) {
                    itemList = editConditions;
                } else {
                    itemList = editFields;
                }
                int fieldIdx = EfaUtil.stringFindInt(itemType.getName(), -1);
                IItemType[] items = itemList.getItems(fieldIdx);
                String fieldName = items[0].getValueFromField();
                IItemType guiItem = dummyRecordGuiItems.get(fieldName);
                if (guiItem != null && guiItem instanceof ItemTypeStringAutoComplete) {
                    autoCompleteList = ((ItemTypeStringAutoComplete)guiItem).getAutoCompleteData();
                }
                if (guiItem != null && guiItem instanceof ItemTypeStringList) {
                    autoCompleteList = new AutoCompleteList();
                    String[] displayList = ((ItemTypeStringList)guiItem).getDisplayList();
                    for (int i=0; displayList != null && i<displayList.length; i++) {
                        autoCompleteList.add(displayList[i], null, true, null);
                    }
                }

                ((ItemTypeStringAutoComplete)items[2]).setAutoCompleteData(autoCompleteList);
            } catch(Exception eignore) {
                Logger.logdebug(eignore);
            }

        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        getValuesFromGui();

        EditRules rules = new EditRules();
        for (int i=0; i<editConditions.size(); i++) {
            IItemType[] items = editConditions.getItems(i);
            rules.editConditions.add(new EditCondition(items[0].toString(), items[1].toString(), items[2].toString()));
        }
        for (int i=0; i<this.editFields.size(); i++) {
            IItemType[] items = editFields.getItems(i);
            rules.editValues.add(new EditValue(items[0].toString(), items[1].toString(), items[2].toString()));
        }
        if (isVersionized) {
            rules.versionAction = versionAction.toString();
            rules.versionValidFrom = versionValidFrom.getTimeStamp();
        }

        Vector<EditChange> editChanges = runPreprocessing(rules);
        if (editChanges == null) {
            return;
        }
        boolean ok = showEditChanges(editChanges, rules);

        if (ok) {
            makeChanges(editChanges, rules);
        }
    }

    class EditCondition {
        String field;
        String operator;
        String value;
        public EditCondition(String field, String operator, String value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }
    }

    class EditValue {
        String field;
        String action;
        String value;
        public EditValue(String field, String action, String value) {
            this.field = field;
            this.action = action;
            this.value = value;
        }
    }

    class EditRules {
        Vector<EditCondition> editConditions = new Vector<EditCondition>();
        Vector<EditValue> editValues = new Vector<EditValue>();
        String versionAction = null;
        long versionValidFrom = -1;
    }

    class EditChange {
        DataKey k;
        String qualifiedName;
        String field;
        String oldValue;
        String newValue;
        boolean identical;
        public EditChange(DataKey k, String qualifiedName, String field, 
                String oldValue, String newValue, boolean identical) {
            this.k = k;
            this.qualifiedName = qualifiedName;
            this.field = field;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.identical = identical;
        }
    }

    private Vector<EditChange> runPreprocessing(EditRules rules) {
        Vector<EditChange> editChanges = new Vector<EditChange>();
        try {
            DataKeyIterator it = persistence.data().getStaticIterator();
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                DataRecord r = persistence.data().get(k);
                if (r != null) {
                    if (isVersionized && !r.isValidAt(validAt)) {
                        continue;
                    }

                    // check condition
                    boolean conditionFulfilled = true;
                    for (EditCondition condition : rules.editConditions) {
                        String value = r.getAsText(condition.field);
                        if (value == null) {
                            value = "";
                        }
                        if (condition.operator.equals(OP_EQUAL)) {
                            if (!value.equalsIgnoreCase(condition.value)) {
                                conditionFulfilled = false;
                            }
                            continue;
                        }
                        if (condition.operator.equals(OP_UNEQUAL)) {
                            if (value.equalsIgnoreCase(condition.value)) {
                                conditionFulfilled = false;
                            }
                            continue;
                        }
                        if (condition.operator.equals(OP_CONTAINS)) {
                            if (value.toLowerCase().indexOf(condition.value.toLowerCase()) < 0) {
                                conditionFulfilled = false;
                            }
                            continue;
                        }

                        // Comparators: > < >= <=
                        DataRecord rtmp = r.cloneRecord();
                        rtmp.setFromText(condition.field, condition.value);
                        int compResult;
                        try {
                            compResult = r.compareFieldToOtherRecord(condition.field, rtmp);
                        } catch(Exception ecompare) {
                            conditionFulfilled = false;
                            continue;
                        }
                        if (condition.operator.equals(OP_GREATER) &&
                            compResult <= 0) {
                            conditionFulfilled = false;
                            continue;
                        }
                        if (condition.operator.equals(OP_GREATEREQUAL) &&
                            compResult < 0) {
                            conditionFulfilled = false;
                            continue;
                        }
                        if (condition.operator.equals(OP_LESS) &&
                            compResult >= 0) {
                            conditionFulfilled = false;
                            continue;
                        }
                        if (condition.operator.equals(OP_LESSEQUAL) &&
                            compResult > 0) {
                            conditionFulfilled = false;
                            continue;
                        }
                    }
                    if (!conditionFulfilled) {
                        continue;
                    }

                    // fields to be edited
                    for (EditValue editValue : rules.editValues) {
                        String currentValueText = r.getAsText(editValue.field);
                        if (currentValueText == null) {
                            currentValueText = "";
                        }
                        String newValueText = null;
                        DataRecord rtmp = r.cloneRecord();
                        if (editValue.action.equals(EA_SETVALUE)) {
                            rtmp.setFromText(editValue.field, editValue.value);
                            newValueText = rtmp.getAsText(editValue.field);
                        } else {
                            if (editValue.value.trim().length() == 0) {
                                continue;
                            }
                            switch (r.getPersistence().data().getFieldType(editValue.field)) {
                                case IDataAccess.DATA_LIST_STRING:
                                case IDataAccess.DATA_LIST_INTEGER:
                                case IDataAccess.DATA_LIST_UUID:
                                    if (editValue.action.equals(EA_ADDTOLIST)) {
                                        newValueText = r.addTextItemToList(editValue.field, editValue.value);
                                    }
                                    if (editValue.action.equals(EA_DELETEFROMLIST)) {
                                        newValueText = r.removeTextItemFromList(editValue.field, editValue.value);
                                    }
                                    break;
                                default:
                                    throw new Exception(editValue.field + " is not a List Data Type!");
                            }
                        }
                        if (newValueText != null) {
                            editChanges.add(new EditChange(k, r.getQualifiedName(),
                                    editValue.field, currentValueText, newValueText,
                                    currentValueText.equals(newValueText)));
                        } else {
                            throw new Exception("New value '" + editValue.value + "' for field '" + editValue.field + "' cannot be applied!");
                        }
                    }
                }
            }
        } catch(Exception e) {
            Logger.logdebug(e);
            Dialog.error(e.toString());
            return null;
        }
        return editChanges;
    }

    private boolean showEditChanges(Vector<EditChange> editChanges, EditRules editRules) {
        if (editChanges == null || editChanges.size() == 0) {
            Dialog.error(International.getString("Keine Datensätze mit passenden Kriterien gefunden."));
            return false;
        }
        String filename = Daten.efaTmpDirectory + "batcheditchanges.html";
        try {
            BufferedWriter f = HtmlFactory.createFile(filename);
            HtmlFactory.writeHeader(f, International.getStringWithMnemonic("Vorschau der Änderungen"), true);
            f.write("<table align=\"center\" border=\"1\">\n");
            f.write("<tr>" +
                    "<th>" + International.getString("Datensatz") + "</th>" +
                    "<th>" + International.getString("Feld") + "</th>" +
                    "<th>" + International.getString("alter Wert") + "</th>" +
                    "<th>" + International.getString("neuer Wert") + "</th>" +
                    "</tr>\n");
            for (EditChange change : editChanges) {
                String i1 = (change.identical ? "<i>" : "");
                String i2 = (change.identical ? "</i>" : "");
                f.write("<tr>" +
                        "<td>" + i1 + change.qualifiedName + i2 + "</td>" +
                        "<td>" + i1 + change.field + i2 + "</td>" +
                        "<td>" + i1 + change.oldValue + i2 + "</td>" +
                        "<td>" + i1 + change.newValue + i2 + "</td>" +
                        "</tr>\n");
            }
            f.write("</table>\n");

            if (isVersionized) {
                f.write("<p align=\"center\">" + International.getString("Änderungsmodus") + ":<br>" +
                        International.getMessage("basierend auf der Version gültig am {date}",
                        DataTypeDate.getDateTimeString(new DataTypeDate(validAt), new DataTypeTime(validAt))) +
                        ":<br>" +
                        (editRules.versionAction.equals(VA_EDITCURRENT) ?
                            International.getString("aktualisiere diese Version") :
                            International.getMessage("erstelle eine neue Version gültig ab {date}",
                                DataTypeDate.getDateTimeString(new DataTypeDate(editRules.versionValidFrom), new DataTypeTime(editRules.versionValidFrom)))) +
                        "</p>\n");
            }
            
            HtmlFactory.writeFooter(f);
            f.close();
        } catch(Exception e) {
            Logger.logdebug(e);
            Dialog.error(e.toString());
            return false;
        }
        return BrowserDialog.openInternalBrowserForAction(this,
                International.getStringWithMnemonic("Vorschau der Änderungen"),
                EfaUtil.correctUrl(filename),
                International.getString("Änderungen jetzt durchführen"),
                BaseDialog.IMAGE_RUN);
    }
    
    private void makeChanges(Vector<EditChange> editChanges, EditRules editRules) {
        if (editChanges == null || editChanges.size() == 0) {
            Dialog.error(International.getString("Keine Datensätze mit passenden Kriterien gefunden."));
            return;
        }
        ChangeTask changeTask = new ChangeTask(editChanges, editRules);
        ProgressDialog progressDialog = new ProgressDialog(this,
                International.getString("Bearbeitungsassistent"), changeTask, false);
        changeTask.startRunning(progressDialog);
    }

    class ChangeTask extends ProgressTask {
        
        Vector<EditChange> editChanges;
        EditRules editRules;
        boolean success = false;
        int successCount = 0;

        public ChangeTask(Vector<EditChange> editChanges, EditRules editRules) {
            super();
            this.editChanges = editChanges;
            this.editRules = editRules;
        }

        public void startRunning(ProgressDialog progressDialog) {
            this.start();
            if (progressDialog != null) {
                progressDialog.showDialog();
            }
        }

        public void run() {
            setRunning(true);
            success = false;
            try {
                IDataAccess data = persistence.data();
                for (EditChange change : editChanges) {
                    logInfo(change.qualifiedName +
                            " [" + change.field + "]: " +
                            change.oldValue + " -> " + change.newValue + 
                            (change.identical ? " (identical)" : "") +
                            "\n");
                    if (change.identical) {
                        continue;
                    }
                    try {
                        DataRecord r = data.get(change.k);
                        if (r != null) {
                            r.setFromText(change.field, change.newValue);
                            if (editRules.versionAction == null ||
                                editRules.versionAction.equals(BatchEditDialog.VA_EDITCURRENT)) {
                                data.update(r);
                                Logger.log(Logger.INFO, Logger.MSG_DATAADM_BE_RECORDUPDATED,
                                        r.getPersistence().getDescription() + ": "
                                        + International.getMessage("{name} hat Datensatz '{record}' geändert.",
                                        (admin != null ? International.getString("Admin") + " '" + admin.getName() + "'"
                                        : International.getString("Normaler Benutzer")),
                                        r.getQualifiedName()));
                            } else {
                                data.addValidAt(r, editRules.versionValidFrom);
                                Logger.log(Logger.INFO, Logger.MSG_DATAADM_BE_RECORDADDEDVER,
                                        r.getPersistence().getDescription() + ": "
                                        + International.getMessage("{name} hat neue Version von Datensatz '{record}' ab {date} erstellt.",
                                        (admin != null ? International.getString("Admin") + " '" + admin.getName() + "'"
                                        : International.getString("Normaler Benutzer")),
                                        r.getQualifiedName(),
                                        EfaUtil.getTimeStampDDMMYYYY(editRules.versionValidFrom)));
                            }
                            String newVal = r.getAsText(change.field);
                            if (!change.newValue.equals(newVal)) {
                                logInfo("WARNING: " +
                                        "Value '" + change.newValue + "' for Field '"+ change.field + "' corrected to '" + newVal + "'\n");
                            } else {
                                successCount++;
                            }
                        } else {
                            logInfo("ERROR: " +
                                    "Record '" + change.qualifiedName + "' not found.\n");
                        }
                    } catch(Exception echange) {
                        logInfo("ERROR: " +
                                "Update Record '" + change.qualifiedName + "' failed: " + echange.toString() + "\n");
                    }
                }
                success = true;
            } catch (Exception e) {
                logInfo("ERROR: " +
                    LogString.operationFailed(International.getString("Änderungen"),
                    e.toString()) + "\n");
            }
            logInfo(International.getMessage("{count} Änderungen erfolgreich durchgeführt.", successCount) + "\n");
            setDone();
        }

        public int getAbsoluteWork() {
            return editChanges.size();
        }

        public String getSuccessfullyDoneMessage() {
            if (success) {
                return LogString.operationSuccessfullyCompleted(
                        International.getString("Änderungen"));
            } else {
                return null;
            }
        }

        public String getErrorDoneMessage() {
            if (!success) {
                return LogString.operationFailed(International.getString("Änderungen"));
            } else {
                return null;
            }
        }
    }
}
