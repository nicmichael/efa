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

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.importefa1.*;
import de.nmichael.efa.efa1.*;
import de.nmichael.efa.*;
import de.nmichael.efa.core.EfaKeyStore;
import de.nmichael.efa.core.config.EfaBaseConfig;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ImportEfa1DataDialog extends StepwiseDialog {

    private static final String OLDEFADATADIR        = "OLDEFADATADIR";
    private static final String IMPORTDATA           = "IMPORTDATA";
    private static final String IMPORTDATALABEL      = "IMPORTDATALABEL";
    private static final String LOGBOOKNAME          = "LOGBOOKNAME";
    private static final String LOGBOOKDESCRIPTION   = "LOGBOOKDESCRIPTION";
    private static final String LOGBOOKRANGEFROM     = "LOGBOOKRANGEFROM";
    private static final String LOGBOOKRANGETO       = "LOGBOOKRANGETO";
    private static final String LOGBOOKRANGELABEL    = "LOGBOOKRANGELABEL";
    private HashMap<String, ImportMetadata> importData;
    private ImportTask importTask;

    public ImportEfa1DataDialog(JDialog parent) {
        super(parent, International.getString("Daten aus efa 1.x importieren"));
    }

    public ImportEfa1DataDialog(Frame parent) {
        super(parent, International.getString("Daten aus efa 1.x importieren"));
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    String[] getSteps() {
        return new String[] {
            International.getString("Datenordner von efa 1.x auswählen"),
            International.getString("Zu importierende Daten auswählen"),
            International.getString("Zu importierende Fahrtenbücher auswählen"),
            International.getString("Zeitraum für Fahrtenbücher festlegen")
        };
    }

    String getDescription(int step) {
        switch(step) {
            case 0: return International.getString("Mit der Import-Funktion werden alle Daten von efa 1.x nach efa 2.x importiert. "+
                    "Falls Du mehrere efa-Installationen parallel genutzt hast, wähle bitte diejenige aus, aus der Du die Daten importieren möchtest.");
            case 1: return International.getString("Bitte wähle aus, welche Daten importiert werden sollen.");
            case 2: return International.getString("Bitte wähle aus, welche Fahrtenbücher, Mitglieder-, Boots- und Ziellisten importiert werden sollen.");
            case 3: return International.getString("In efa 2.x hat jedes Fahrtenbuch einen (beliebigen) eindeutigen Namen, eine optionale Beschreibung und einen Zeitraum. Alle Fahrten des Fahrtenbuchs müssen in diesem Zeitraum liegen.") +" \n" +
                    International.getString("Üblicherweise solltest Du pro Jahr genau ein Fahrtenbuch anlegen. Vereine mit mehreren Bootshäusern sollten pro Bootshaus ein Fahrtenbuch pro Jahr verwenden.");
       }
        return "";
    }

    void initializeItems() {
        items = new ArrayList<IItemType>();
        IItemType item;

        // #####################################################################
        // Items for Step 0
        // #####################################################################

        // Find existing efa installations
        ArrayList<String> oldEfaDataDir = new ArrayList<String>();
        ArrayList<String> oldEfaDescription = new ArrayList<String>();
        int longestTextWidth = 0;
        try {
            File dir = new File(Daten.userHomeDir);
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                for (File f : files) {
                    if (f.isFile() && f.getName().startsWith(".efa_")) {
                        try {
                            String name = f.getName();
                            String dataDir = "";
                            boolean isEfa1Version = true;
                            String lastUsed = EfaUtil.getTimeStampDDMMYYYY(f.lastModified());
                            BufferedReader efa1 = new BufferedReader(new InputStreamReader(new FileInputStream(f.getAbsolutePath()),Daten.ENCODING_ISO));
                            String s;
                            while ( (s = efa1.readLine()) != null) {
                                if (s.startsWith("USERHOME=")) {
                                    dataDir = s.substring(9);
                                }
                                if (s.startsWith(EfaBaseConfig.FIELD_VERSION)) {
                                    isEfa1Version = false; // this field is not present in efa1!
                                }
                            }
                            efa1.close();
                            if (isEfa1Version) {
                                File fcfg = new File(dataDir + Daten.fileSep + "cfg" + Daten.fileSep + "efa.cfg");
                                if (fcfg.isFile()) {
                                    lastUsed = EfaUtil.getTimeStampDDMMYYYY(fcfg.lastModified());
                                }
                                String txt = International.getMessage("{datadir} (zuletzt genutzt {date})",
                                        dataDir, lastUsed);
                                oldEfaDataDir.add(dataDir);
                                oldEfaDescription.add(txt);
                                longestTextWidth = Math.max(longestTextWidth, txt.length());
                            }
                        } catch(Exception eignore2) {
                        }
                    }
                }
            }
        } catch(Exception eignore1) {
            // ignore
        }
        // Add existing efa Installations to GUI Selection
        if (oldEfaDataDir.size() > 0) {
            oldEfaDataDir.add("");
            oldEfaDescription.add("--- " + International.getString("Verzeichnis manuell auswählen") + " ---");

            item = new ItemTypeStringList(OLDEFADATADIR, oldEfaDataDir.get(0),
                    oldEfaDataDir.toArray(new String[0]),
                    oldEfaDescription.toArray(new String[0]),
                IItemType.TYPE_PUBLIC, "0", International.getString("Daten importieren von"));
            int width = 450;
            if (longestTextWidth > 60) {
                width += (longestTextWidth-60) * (450/60);
            }

            ((ItemTypeStringList)item).setFieldSize(width, 19);
            ((ItemTypeStringList)item).setItemOnNewRow(true);
            ((ItemTypeStringList)item).setPadding(0, 0, 10, 10);
            ((ItemTypeStringList)item).setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        } else {
            item = new ItemTypeFile(OLDEFADATADIR, "",
                    International.getString("Verzeichnis für Nutzerdaten"),
                    International.getString("Verzeichnisse"),
                null,ItemTypeFile.MODE_OPEN,ItemTypeFile.TYPE_DIR,
                IItemType.TYPE_PUBLIC, "0",
                International.getString("Daten importieren von"));
        }
        items.add(item);

    }

    void reinitializeItems() {
        IItemType item;

        if (step == 0) {
            // #####################################################################
            // Items for Step 1 and 2
            // #####################################################################

            // remove all previous items for step 1 and 2
            int i = 0;
            while (i < items.size()) {
                if (items.get(i).getCategory().equals("1")
                        || items.get(i).getCategory().equals("2")) {
                    items.remove(i);
                } else {
                    i++;
                }
            }

            // get Data Directory
            item = getItemByName(OLDEFADATADIR);
            String dir;
            if (item instanceof ItemTypeStringList) {
                dir = ((ItemTypeStringList) item).getValue();
            } else {
                dir = ((ItemTypeFile) item).getValue();
            }

            if (dir == null || dir.length() == 0) {
                dir = Dialog.dateiDialog(this,
                    International.getMessage("{item} auswählen",
                        International.getString("Verzeichnis für Nutzerdaten")),
                    International.getString("Verzeichnisse"),
                    null,
                    Daten.userHomeDir,
                    null,
                    null,
                    false,
                    true);
            }

            importData = new HashMap<String, ImportMetadata>();
            ImportMetadata meta;

            // find all Data (except Logbooks)
            checkImportData(importData, dir, new Adressen("adressen.efd"), ImportMetadata.TYPE_ADRESSEN, International.getString("Adressen"));
            checkImportData(importData, dir, new Synonyme("boote.efs"), ImportMetadata.TYPE_SYNONYME_BOOTE, International.getString("Synonyme")
                    + " (" + International.getString("Boote") + ")");
            checkImportData(importData, dir, new Synonyme("mitglieder.efs"), ImportMetadata.TYPE_SYNONYME_MITGLIEDER, International.getString("Synonyme")
                    + " (" + International.getString("Personen") + ")");
            checkImportData(importData, dir, new Synonyme("ziele.efs"), ImportMetadata.TYPE_SYNONYME_ZIELE, International.getString("Synonyme")
                    + " (" + International.getString("Ziele") + ")");
            checkImportData(importData, dir, new BootStatus("bootstatus.efdb"), ImportMetadata.TYPE_BOOTSTATUS, International.getString("Bootsstatus"));
            checkImportData(importData, dir, new de.nmichael.efa.efa1.Fahrtenabzeichen("fahrtenabzeichen.eff"), ImportMetadata.TYPE_FAHRTENABZEICHEN, International.onlyFor("Fahrtenabzeichen", "de"));
            checkImportData(importData, dir, new de.nmichael.efa.efa1.DatenListe("keystore_pub.dat", 1, 0, false), ImportMetadata.TYPE_KEYSTORE, "KeyStore"); // DatenList is just dummy to pass filename of keystore!
            checkImportData(importData, dir, new Gruppen("gruppen.efg"), ImportMetadata.TYPE_GRUPPEN, International.getString("Gruppen"));
            checkImportData(importData, dir, new Mannschaften("mannschaften.efm"), ImportMetadata.TYPE_MANNSCHAFTEN, International.getString("Mannschaften"));

            // find all Logbooks
            getAllLogbooks(importData, dir);

            // add items to GUI
            String[] datakeys = importData.keySet().toArray(new String[0]);
            Arrays.sort(datakeys);
            for (String key : datakeys) {
                meta = importData.get(key);
                item = new ItemTypeBoolean(IMPORTDATA + key, meta.numRecords > 0,
                        IItemType.TYPE_PUBLIC, (meta.type != ImportMetadata.TYPE_FAHRTENBUCH ? "1" : "2"), key);

                items.add(item);
                item = new ItemTypeLabel(IMPORTDATALABEL + key,
                        IItemType.TYPE_PUBLIC, (meta.type != ImportMetadata.TYPE_FAHRTENBUCH ? "1" : "2"),
                        meta.toString());
                item.setColor(meta.numRecords < 0 ? Color.red : Color.black);
                item.setPadding(25, 0, 0, 5);
                items.add(item);
            }
        }

        if (step == 2) {
            // #####################################################################
            // Items for Step 3
            // #####################################################################

            // remove all previous items for step 3
            int i = 0;
            while (i < items.size()) {
                if (items.get(i).getCategory().equals("3")) {
                    items.remove(i);
                } else {
                    i++;
                }
            }

            String[] keys  = importData.keySet().toArray(new String[0]);
            Arrays.sort(keys);
            HashMap logNames = new HashMap<String,String>();
            for (String key : keys) {
                ImportMetadata meta = importData.get(key);
                if (meta.type == ImportMetadata.TYPE_FAHRTENBUCH && meta.selected) {
                    DataTypeDate dateFrom = new DataTypeDate();
                    dateFrom.setDate( (meta.firstDate != null ? meta.firstDate.toString() : DataTypeDate.today().toString()) );
                    dateFrom.setDayMonth(1, 1);
                    DataTypeDate dateTo = new DataTypeDate();
                    dateTo.setDate( (meta.lastDate != null ? meta.lastDate.toString() : DataTypeDate.today().toString()) );
                    dateTo.setDayMonth(31, 12);

                    String name = Integer.toString(dateFrom.getYear());
                    int ikey = 1;
                    String skey = name;
                    while (logNames.get(skey) != null) {
                        skey = name + "_" + (++ikey);
                    }
                    name = skey;
                    logNames.put(name, name);

                    item = new ItemTypeLabel(LOGBOOKRANGELABEL + "l0" + key,
                            IItemType.TYPE_PUBLIC, "3",
                            meta.filename);
                    item.setPadding(0, 0, 5, 0);
                    items.add(item);
                    item = new ItemTypeLabel(LOGBOOKRANGELABEL + "l1" + key,
                            IItemType.TYPE_PUBLIC, "3",
                            meta.toString(false));
                    item.setPadding(25, 0, 0, 0);
                    items.add(item);

                    item = new ItemTypeString(LOGBOOKNAME + key,
                            name,
                            IItemType.TYPE_PUBLIC, "3",
                            International.getString("Name des Fahrtenbuchs"));
                    ((ItemTypeString)item).setNotNull(true);
                    item.setPadding(25, 0, 0, 0);
                    items.add(item);
                    item = new ItemTypeString(LOGBOOKDESCRIPTION + key,
                            "",
                            IItemType.TYPE_PUBLIC, "3",
                            International.getString("Beschreibung"));
                    item.setPadding(25, 0, 0, 0);
                    items.add(item);

                    item = new ItemTypeDate(LOGBOOKRANGEFROM + key,
                            dateFrom,
                            IItemType.TYPE_PUBLIC, "3",
                            International.getString("Fahrtenbuch gültig für Fahrten ab"));
                    ItemTypeDate logbookFrom = (ItemTypeDate)item;
                    item.setPadding(25, 0, 0, 0);
                    items.add(item);
                    item = new ItemTypeDate(LOGBOOKRANGETO + key,
                            dateTo,
                            IItemType.TYPE_PUBLIC, "3",
                            International.getString("Fahrtenbuch gültig für Fahrten bis"));
                    ((ItemTypeDate)item).setMustBeAfter(logbookFrom, false);
                    items.add(item);
                    item.setPadding(25, 0, 0, 0);
                }
            }
            
        }
    }

    private void checkImportData(HashMap<String,ImportMetadata> importData, String dir, DatenListe datenListe, int type, String description) {
        datenListe.dontEverWrite();
        Dialog.SUPPRESS_DIALOGS = false;
        ImportMetadata meta = new ImportMetadata(type, datenListe, description);
        String fname = datenListe.getFileName();
        if (dir == null || !dir.endsWith(Daten.fileSep)) {
            dir = (dir != null ? dir : "") + Daten.fileSep;
        }
        if (new File(dir+"daten"+Daten.fileSep+fname).exists()) {
            // old efa 1.x daten folder
            dir = dir+"daten"+Daten.fileSep;
        } else if (new File(dir+"data"+Daten.fileSep+fname).exists()) {
            // new efa 1.9.0 data folder
            dir = dir+"data"+Daten.fileSep;
        } else if (new File(dir+fname).exists()) {
            // user may have manually selected the "daten" or "data" folder, so we're already in it
            // nothing to do
        }
        if (EfaUtil.canOpenFile(dir+fname)) {
            datenListe.setFileName(dir+fname);
            if (type != ImportMetadata.TYPE_KEYSTORE) {
                if (datenListe.readFile()) {
                    meta.filename = datenListe.getFileName();
                    meta.numRecords = datenListe.countElements();
                }
            } else {
                EfaKeyStore keyStore = new EfaKeyStore(datenListe.getFileName(), "efa".toCharArray());
                meta.filename = datenListe.getFileName();
                meta.numRecords = keyStore.size();
            }
        }
        importData.put(datenListe.getFileName(), meta);
        Dialog.SUPPRESS_DIALOGS = false;
    }

    private void getAllLogbooks(HashMap<String,ImportMetadata> importData, String dirname) {
        try {
            File dir = new File(dirname);
            File[] files = dir.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    getAllLogbooks(importData, f.getAbsolutePath());
                } else {
                    if (f.getName().toLowerCase().endsWith(".efb")) {
                        recursiveAddLogbook(importData, f.getAbsolutePath());
                    }
                }
            }
        } catch(Exception eignore0) {
        }
    }

    private String getLogbookKey(ImportMetadata meta, String filename) {
        return (meta.firstDate != null && meta.firstDate.isSet()
                ? meta.firstDate.getDateString("YYYYMMDD") : "00000000") + "-" + filename;
    }

    private void recursiveAddLogbook(HashMap<String, ImportMetadata> importData, String fname) {
        String fnameKey = (Daten.isOsWindows() ? fname.toLowerCase() : fname);
        if (importData.get(fnameKey) != null) {
            return;
        }
        Fahrtenbuch fb = new Fahrtenbuch(fname);
        fb.dontEverWrite();
        Dialog.SUPPRESS_DIALOGS = false;
        if (EfaUtil.canOpenFile(fb.getFileName()) && fb.readFile()) {
            ImportMetadata meta = new ImportMetadata(ImportMetadata.TYPE_FAHRTENBUCH, fb, International.getString("Fahrtenbuch"));
            meta.numRecords = 0;
            DatenFelder d = fb.getCompleteFirst();
            while (d != null) {
                meta.numRecords++;
                if (d.get(Fahrtenbuch.DATUM).length() > 0
                        && (meta.firstDate == null || EfaUtil.secondDateIsAfterFirst(d.get(Fahrtenbuch.DATUM), meta.firstDate.toString()))) {
                    meta.firstDate = DataTypeDate.parseDate(d.get(Fahrtenbuch.DATUM));
                }
                if (d.get(Fahrtenbuch.DATUM).length() > 0
                        && (meta.lastDate == null || EfaUtil.secondDateIsAfterFirst(meta.lastDate.toString(), d.get(Fahrtenbuch.DATUM)))) {
                    meta.lastDate = DataTypeDate.parseDate(d.get(Fahrtenbuch.DATUM));
                }
                d = fb.getCompleteNext();
            }

            // Members, Boats, Destinations, Statistics
            Boote boote = new Boote(EfaUtil.makeFullPath(EfaUtil.getPathOfFile(fname),fb.getDaten().bootDatei));
            boote.dontEverWrite();
            if (EfaUtil.canOpenFile(boote.getFileName()) && boote.readFile()) {
                meta.numRecBoats = boote.countElements();
            }
            Mitglieder mitglieder = new Mitglieder(EfaUtil.makeFullPath(EfaUtil.getPathOfFile(fname),fb.getDaten().mitgliederDatei));
            mitglieder.dontEverWrite();
            if (EfaUtil.canOpenFile(mitglieder.getFileName()) && mitglieder.readFile()) {
                meta.numRecMembers = mitglieder.countElements();
            }
            Ziele ziele = new Ziele(EfaUtil.makeFullPath(EfaUtil.getPathOfFile(fname),fb.getDaten().zieleDatei));
            ziele.dontEverWrite();
            if (EfaUtil.canOpenFile(ziele.getFileName()) && ziele.readFile()) {
                meta.numRecDests = ziele.countElements();
            }
            StatSave stat = new StatSave(EfaUtil.makeFullPath(EfaUtil.getPathOfFile(fname),fb.getDaten().statistikDatei));
            stat.dontEverWrite();
            if (EfaUtil.canOpenFile(stat.getFileName()) && stat.readFile()) {
                meta.numRecStats = stat.countElements();
            }
            importData.put(fnameKey, meta);

            recursiveAddLogbook(importData, fb.getPrevFb(true));
            recursiveAddLogbook(importData, fb.getNextFb(true));
        }
        Dialog.SUPPRESS_DIALOGS = false;
    }

    boolean checkInput(int direction) {
        boolean ok = super.checkInput(direction);
        if (!ok) {
            return false;
        }

        if (step == 0) { // get efa1 installation directory for import
            IItemType item = getItemByName(OLDEFADATADIR);
            String dir;
            if (item instanceof ItemTypeStringList) {
                dir = ((ItemTypeStringList)item).getValue();
            } else {
                dir = ((ItemTypeFile)item).getValue();
            }
            if (dir == null || (dir.length() > 0 && !(new File(dir)).isDirectory())) {
                // dir.length==0 is allowed - we will prompt the user later
                Dialog.error(LogString.directoryDoesNotExist(dir, International.getString("Verzeichnis")));
                item.requestFocus();
                return false;
            } else {
                reinitializeItems();
            }
        }

        if (step == 2) { // get data from step 1 and 2
            String[] datakeys = importData.keySet().toArray(new String[0]);
            for (String key : datakeys) {
                ImportMetadata meta = importData.get(key);

                // get selected data files for import
                IItemType item = getItemByName(IMPORTDATA + key);
                if (item != null && item instanceof ItemTypeBoolean) {
                    meta.selected = ((ItemTypeBoolean)item).getValue();
                }
            }
        }

        if (step == 2 || step == 3) { // get data from step 2 and 3
            String[] datakeys = importData.keySet().toArray(new String[0]);
            Hashtable<String,String> uniqueLogbooks = new Hashtable<String,String>();
            for (String key : datakeys) {
                ImportMetadata meta = importData.get(key);
                // get logbool metadata
                IItemType item = getItemByName(IMPORTDATA + key);
                if (meta.type == ImportMetadata.TYPE_FAHRTENBUCH) {
                    item = getItemByName(LOGBOOKNAME + key);
                    if (item != null && item instanceof ItemTypeString) {
                        meta.name = ((ItemTypeString)item).getValue();
                        if (uniqueLogbooks.get(meta.name) != null) {
                            Dialog.error(International.getMessage("Das Feld '{field}' muß eindeutig sein.",
                                    International.getString("Name des Fahrtenbuchs")));
                            item.requestFocus();
                            return false;
                        }
                        uniqueLogbooks.put(meta.name, "foo");
                    }
                    item = getItemByName(LOGBOOKDESCRIPTION + key);
                    if (item != null && item instanceof ItemTypeString) {
                        meta.description = ((ItemTypeString)item).getValue();
                    }
                    item = getItemByName(LOGBOOKRANGEFROM + key);
                    if (item != null && item instanceof ItemTypeDate) {
                        meta.firstDate = DataTypeDate.parseDate(((ItemTypeDate)item).toString());
                    }
                    item = getItemByName(LOGBOOKRANGETO + key);
                    if (item != null && item instanceof ItemTypeDate) {
                        meta.lastDate = DataTypeDate.parseDate(((ItemTypeDate)item).toString());
                    }
                }
            }
        }

        if (step == 2 && direction == 1) { // going from step 2 -> 3
            reinitializeItems();
        }

        return true;
    }

    private void rearrangeLogbookOrder(HashMap<String, ImportMetadata> importData ) {
        // this is to make sure that logbooks are imported in the order in which the
        // user specified their valid start dates; those dates may be different
        // than these we detected ourselves when scanning the logbooks, so we prefix
        // each key with the selected start date.
        // Import order in ImportTask is based on alphanumerical sorting of the keys.
        String[] keys = importData.keySet().toArray(new String[0]);
        for (String key : keys) {
            ImportMetadata meta = importData.get(key);
            if (meta.selected && meta.type == ImportMetadata.TYPE_FAHRTENBUCH) {
                String newkey = (meta.firstDate != null && meta.firstDate.isSet() ?
                    meta.firstDate.getDateString("YYYYMMDD") : "00000000") + "-" + key;
                importData.remove(key);
                importData.put(newkey, meta);
            }
        }
    }

    boolean finishButton_actionPerformed(ActionEvent e) {
        if (!super.finishButton_actionPerformed(e)) {
            return false;
        }
        rearrangeLogbookOrder(importData);
        importTask = new ImportTask(importData);
        ProgressDialog progressDialog = new ProgressDialog(this, International.getString("Daten importieren"), importTask, false);
        importTask.start();
        progressDialog.showDialog();
        return true;
    }

    public String getNewestLogbookName() {
        return (importTask != null && !importTask.isRunning() ? importTask.getNewestLogbookName() : null);
    }

}
