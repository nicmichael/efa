/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Properties;
import javax.swing.*;

public class TranslateDialog extends BaseDialog implements IItemListener, ITableEditListener {

    private static String SELECT_ALL = "ALL";
    private static String SELECT_TRANSLATED = "TRANSLATED";
    private static String SELECT_UNTRANSLATED = "UNTRANSLATED";

    private static int ISELECT_ALL = 0;
    private static int ISELECT_TRANSLATED = 1;
    private static int ISELECT_UNTRANSLATED = 2;

    private ItemTypeLabel notesUrl;
    private ItemTypeStringList selectFilter;
    private ItemTypeButton fillEmptyWithBaseText;
    private ItemTypeTable languageTable;

    private String baseLangId;
    private String workLangId;
    private String baseLangDescription;
    private String workLangDescription;
    private String baseLangFile;
    private String workLangFile;
    private Properties baseLang;
    private Properties workLang;
    private Properties fullLang;

    public TranslateDialog(Frame parent) {
        super(parent, International.getString("Übersetzen"),
                International.getStringWithMnemonic("Speichern"));
    }

    public TranslateDialog(JDialog parent) {
        super(parent, International.getString("Übersetzen"),
                International.getStringWithMnemonic("Speichern"));
    }

    private boolean iniLanguages() {
        ItemTypeStringList workLanguage;
        ItemTypeStringList baseLanguage;
        ItemTypeLabel notes1;
        ItemTypeLabel notes2;

        notes1 = new ItemTypeLabel("NOTES1",
                IItemType.TYPE_PUBLIC, "", International.getString("Dies ist eine Entwickler-Funktion!"));
        notes1.setPadding(0, 0, 10, 10);
        notes1.setColor(Color.red);
        notes1.setHorizontalAlignment(SwingConstants.CENTER);
        notes1.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
        notes2 = new ItemTypeLabel("NOTES2",
                IItemType.TYPE_PUBLIC, "", "Please carefully read the instructions at:");
        notesUrl = new ItemTypeLabel("NOTESURL",
                IItemType.TYPE_PUBLIC, "", Daten.EFATRANSLATEWIKI);
        notesUrl.setColor(Color.blue);
        notesUrl.setPadding(0, 0, 0, 10);
        notesUrl.activateMouseClickListener();
        notesUrl.registerItemListener(this);

        String[] lang = International.getLanguageBundles();
        String[] allLang = Locale.getISOLanguages();
        if (lang == null || lang.length == 0) {
            return false;
        }
        String[] desc = new String[lang.length];
        for (int i=0; i<lang.length; i++) {
            desc[i] = International.getLanguageDescription(lang[i]);
        }
        String[] allDesc = new String[allLang.length];
        for (int i=0; i<allLang.length; i++) {
            allDesc[i] = International.getLanguageDescription(allLang[i]);
        }
        String lastWorkLang = Daten.efaConfig.getTranslateLanguageWork();
        if (lastWorkLang == null || lastWorkLang.length() == 0) {
            lastWorkLang = allLang[0];
        }
        String lastBaseLang = Daten.efaConfig.getTranslateLanguageBase();
        if (lastBaseLang == null || lastBaseLang.length() == 0) {
            lastBaseLang = International.getLanguageID();
        }
        workLanguage = new ItemTypeStringList("TRANSLATE_LANGUAGE", lastWorkLang,
                allLang, allDesc,
                IItemType.TYPE_PUBLIC, "", "Translation into Language");
        workLanguage.setPadding(0, 0, 10, 0);
        baseLanguage = new ItemTypeStringList("BASE_LANGUAGE", lastBaseLang,
                lang, desc,
                IItemType.TYPE_PUBLIC, "", "Based on Language");
        baseLanguage.setPadding(0, 0, 0, 10);

        IItemType[] items = new IItemType[5];
        items[0] = notes1;
        items[1] = notes2;
        items[2] = notesUrl;
        items[3] = workLanguage;
        items[4] = baseLanguage;
        if (MultiInputDialog.showInputDialog(_parent, International.getString("Übersetzen"), items)) {
            baseLangId = baseLanguage.getValueFromField();
            workLangId = workLanguage.getValueFromField();
            baseLangDescription = International.getLanguageDescription(baseLangId);
            workLangDescription = International.getLanguageDescription(workLangId);
            baseLangFile = International.getBundleFilename(baseLangId);
            workLangFile = International.getBundleFilename(workLangId);
            baseLang = readProperties(baseLangFile);
            if (baseLang == null) {
                Dialog.error("Cannot open Base Language File:\n"+
                        baseLangFile);
                return false;
            }
            workLang = readProperties(workLangFile);
            if (workLang == null) {
                if (Dialog.yesNoCancelDialog("Create new Language",
                        LogString.fileNotFound(workLangFile,
                        "Properties File for Language " + workLangDescription)
                        + "\nCreate new?") != Dialog.YES) {
                    return false;
                }
                workLang = createProperties(workLangFile, workLangDescription);
                if (workLang == null) {
                    Dialog.error(LogString.fileCreationFailed(workLangFile,
                            "Properties File for Language " + workLangDescription,
                            "Do you have write permission in this Directory?"));
                    return false;
                }
            }

            String fullLangFile = International.getBundleFilename("de");
            fullLang = readProperties(fullLangFile);
            if (fullLang == null) {
                Dialog.error("Cannot open Main Language File:\n"
                        + fullLangFile);
                return false;
            }
        } else {
            return false;
        }
        Daten.efaConfig.setTranslateLanguageWork(workLangId);
        Daten.efaConfig.setTranslateLanguageBase(baseLangId);
        return true;
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridBagLayout());

        selectFilter = new ItemTypeStringList("SELECT_FILTER", SELECT_ALL,
                new String[] { SELECT_ALL, SELECT_UNTRANSLATED, SELECT_TRANSLATED },
                new String[] { "all", "untranslated", "translated" },
                IItemType.TYPE_PUBLIC, "", "Show");
        selectFilter.registerItemListener(this);
        selectFilter.setPadding(0, 0, 10, 0);
        selectFilter.displayOnGui(this, filterPanel, 0, 0);

        fillEmptyWithBaseText = new ItemTypeButton("FILL_EMPTY_WITH_BASE_TEXT",
                IItemType.TYPE_PUBLIC, "", "Fill empty Fields with Reference Text");
        fillEmptyWithBaseText.registerItemListener(this);
        fillEmptyWithBaseText.setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
        fillEmptyWithBaseText.setPadding(0, 0, 5, 10);
        fillEmptyWithBaseText.displayOnGui(this, filterPanel, 0, 1);

        mainPanel.add(filterPanel, BorderLayout.NORTH);

        languageTable = new ItemTypeTable("LANGUAGE_TABLE",
                new String[] { "Key", baseLangDescription + " (Reference)",
                                      workLangDescription + " (Translation)" },
                null, null,
                IItemType.TYPE_PUBLIC, "", "Strings");
        languageTable.setFieldSize(1000, 600);
        languageTable.setEditableColumns(new boolean[] { false, false, true });
        languageTable.setToolTipsEnabled(true);
        languageTable.disableIntelligentColumnWidth(true);
        languageTable.registerTableEditListener(this);
        languageTable.displayOnGui(this, mainPanel, BorderLayout.CENTER);

        closeButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        closeButton.setIconTextGap(10);

        updateLanguageTable(false);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (notesUrl != null && itemType.getName().equals(notesUrl.getName()) &&
            event instanceof MouseEvent) {
            BrowserDialog.openExternalBrowser(_parent, Daten.EFATRANSLATEWIKI);
        }
        if (selectFilter != null && itemType.getName().equals(selectFilter.getName()) &&
            event instanceof ItemEvent && event.getID() == ItemEvent.ITEM_STATE_CHANGED) {
            this.updateLanguageTable(true);
        }
        if (fillEmptyWithBaseText != null && itemType.getName().equals(fillEmptyWithBaseText.getName()) &&
            event instanceof ActionEvent && event.getID() == ActionEvent.ACTION_PERFORMED) {
            fillEmptyFieldsWithBaseText();
        }
    }

    public void tableEditListenerAction(IItemType itemType, TableItem[] items, int row, int col) {
        if (items != null && items.length == 3 && 
            items[0] != null && items[1] != null && items[2] != null) {
            boolean translated = this.isTranslated(items[0].toString(), items[1].toString(), items[2].toString());
            items[0].setMarked(!translated);
            items[1].setMarked(!translated);
            items[2].setMarked(!translated);
        }
    }

    private Properties readProperties(String filename) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(filename));
            return prop;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    private Properties createProperties(String filename, String description) {
        try {
            Properties prop = new Properties();
            prop.store(new FileOutputStream(filename), description);
            return prop;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    private boolean isTranslated(String key, String base, String work) {
        String full = this.fullLang.getProperty(key);
        if (full == null || base == null || work == null ||
            work.equals(base) || work.equals(full) || work.length() == 0) {
            return false;
        } else {
            return true;
        }
    }

    private void getDataFromTable() {
        TableItem[][] tableData = languageTable.getTableData();
        if (tableData != null) {
            for (int i=0; i<tableData.length; i++) {
                String key = tableData[i][0].toString();
                String value = tableData[i][2].toString();
                if (key != null && value != null) {
                    if (value.trim().length() > 0) {
                        workLang.put(key, value);
                    } else {
                        workLang.remove(key);
                    }
                }
            }
        }
    }

    private void removeOldData() {
        String[] keysWork = workLang.keySet().toArray(new String[0]);
        for (int i=0; keysWork != null && i<keysWork.length; i++) {
            String key = keysWork[i];
            if (fullLang.get(key) == null) {
                workLang.remove(key);
            }
        }
    }

    private void fillEmptyFieldsWithBaseText() {
        getDataFromTable();
        String[] keysFull = fullLang.keySet().toArray(new String[0]);
        String[] keysBase = baseLang.keySet().toArray(new String[0]);
        for (int i=0; keysFull != null && i<keysFull.length; i++) {
            String key = keysFull[i];
            String txtFull = fullLang.getProperty(key);
            if (txtFull == null) {
                continue; // shouldn't happen
            } else {
                txtFull = txtFull.trim();
            }
            String txtBase = baseLang.getProperty(key);
            if (txtBase == null) {
                txtBase = txtFull;
            } else {
                txtBase = txtBase.trim();
            }
            String txtWork = workLang.getProperty(key);
            if (txtWork == null || txtWork.trim().length() == 0) {
                workLang.put(key, txtBase);
            }
        }
        updateLanguageTable(false);
    }

    private void updateLanguageTable(boolean readFromGuiFirst) {
        String selectString = selectFilter.getValueFromField();
        int select = ISELECT_ALL;
        if (selectString != null && selectString.equals(SELECT_TRANSLATED)) {
            select = ISELECT_TRANSLATED;
        }
        if (selectString != null && selectString.equals(SELECT_UNTRANSLATED)) {
            select = ISELECT_UNTRANSLATED;
        }

        if (readFromGuiFirst) {
            getDataFromTable();
        }

        Hashtable<String,TableItem[]> tableItems = new Hashtable<String,TableItem[]>();
        String[] keysFull = fullLang.keySet().toArray(new String[0]);
        String[] keysBase = baseLang.keySet().toArray(new String[0]);
        String[] keysWork = workLang.keySet().toArray(new String[0]);

        for (int i=0; keysFull != null && i<keysFull.length; i++) {
            String key = keysFull[i];
            String txtFull = fullLang.getProperty(key);
            if (txtFull == null) {
                continue; // shouldn't happen
            } else {
                txtFull = txtFull.trim();
            }
            String txtBase = baseLang.getProperty(key);
            if (txtBase == null) {
                txtBase = txtFull;
            } else {
                txtBase = txtBase.trim();
            }
            String txtWork = workLang.getProperty(key);
            if (txtWork == null) {
                txtWork = "";
            } else {
                txtWork = txtWork.trim();
            }

            TableItem[] tableItem = new TableItem[3];
            tableItem[0] = new TableItem(key);
            tableItem[1] = new TableItem(txtBase);
            tableItem[2] = new TableItem(txtWork);

            if (txtWork.equals(txtBase) || txtWork.equals(txtFull) ||
                txtWork.length() == 0) {
                // untranslated
                if (select == ISELECT_TRANSLATED) {
                    continue;
                }
                tableItem[0].setMarked(true);
                tableItem[1].setMarked(true);
                tableItem[2].setMarked(true);
            } else {
                // translated
                if (select == ISELECT_UNTRANSLATED) {
                    continue;
                }
            }
            tableItems.put(key, tableItem);
        }

        languageTable.setValues(tableItems);
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        getDataFromTable();
        removeOldData();
        try {
            workLang.store(new FileOutputStream(workLangFile), workLangDescription);
            IItemType[] items = new IItemType[6];
            items[0] = new ItemTypeLabel("L0", IItemType.TYPE_PUBLIC, "",
                    "Properties File successfully saved:");
            items[0].setPadding(0, 0, 10, 0);
            items[0].setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
            ((ItemTypeLabel)items[0]).setHorizontalAlignment(SwingConstants.CENTER);
            items[1] = new ItemTypeLabel("L1", IItemType.TYPE_PUBLIC, "",
                    workLangFile);
            items[1].setColor(Color.blue);
            items[1].setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
            ((ItemTypeLabel)items[1]).setHorizontalAlignment(SwingConstants.CENTER);

            items[2] = new ItemTypeLabel("L2", IItemType.TYPE_PUBLIC, "",
                    "To have this file included in official efa releases, please mail it to:");
            items[2].setPadding(0, 0, 10, 0);
            items[2].setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
            ((ItemTypeLabel)items[2]).setHorizontalAlignment(SwingConstants.CENTER);
            items[3] = new ItemTypeLabel("L3", IItemType.TYPE_PUBLIC, "",
                    Daten.EMAILDEV);
            items[3].setColor(Color.blue);
            items[3].setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
            ((ItemTypeLabel)items[3]).setHorizontalAlignment(SwingConstants.CENTER);

            items[4] = new ItemTypeLabel("L4", IItemType.TYPE_PUBLIC, "",
                    "The next update installation of efa may overwrite your file if you don't copy it somewhere else!");
            items[4].setPadding(0, 0, 10, 10);
            items[4].setColor(Color.red);
            items[4].setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
            ((ItemTypeLabel)items[4]).setHorizontalAlignment(SwingConstants.CENTER);

            items[5] = new ItemTypeLabel("L5", IItemType.TYPE_PUBLIC, "",
                    "New Translation will only be effective after a restart of efa.");
            items[5].setPadding(0, 0, 10, 10);
            items[5].setFieldGrid(2, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
            ((ItemTypeLabel)items[5]).setHorizontalAlignment(SwingConstants.CENTER);
            MultiInputDialog.showInputDialog(this, "Property File saved", items);
        } catch(Exception ex) {
            Dialog.error(LogString.fileSavingFailed(workLangFile, "Properties File", ex.toString()));
            return;
        }
        super.closeButton_actionPerformed(e);
    }

    public static void openTranslateDialog(Frame parent) {
        TranslateDialog dlg = new TranslateDialog(parent);
        if (dlg.iniLanguages()) {
            dlg.showDialog();
        }
    }

    public static void openTranslateDialog(JDialog parent) {
        TranslateDialog dlg = new TranslateDialog(parent);
        if (dlg.iniLanguages()) {
            dlg.showDialog();
        }
    }
}
