/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.importefa1;

import de.nmichael.efa.util.*;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.*;
import de.nmichael.efa.data.StatusRecord;
import java.util.*;

public class ImportTask extends ProgressTask {

    private HashMap<String, ImportMetadata> importData;
    private Hashtable<String,DataKey> personMainName2Key;
    private Hashtable<String,DataKey> destinationMainName2Key;
    private Hashtable<String,String> synMitglieder;
    private Hashtable<String,String> synBoote;
    private Hashtable<String,String> synZiele;
    private Hashtable<String,UUID> statusKeys;
    private Hashtable<String,String> addresses;
    private Hashtable<DataKey,String> boatsAllowedGroups;
    private Hashtable<DataKey,String> boatsRequiredGroup;
    private Hashtable<String,UUID> groupMapping;

    private String newestLogbookName; // name of the logbook to be opened when this dialog is completed

    public ImportTask(HashMap<String, ImportMetadata> importData) {
        super();
        this.importData = importData;
    }

    public void run() {
        setRunning(true);
        int i = 0;
        int errorCnt = 0;
        int successCnt = 0;
        String logfile = Daten.efaLogDirectory + Daten.fileSep + "import_" + EfaUtil.getCurrentTimeStampYYYYMMDD_HHMMSS() + ".log";
        setLogfile(logfile);
        logInfo(International.getString("Protokoll") + ": " + logfile + "\n");
        logInfo(International.getString("Daten werden importiert ...") + "\n");
        Daten.project.openAllData();
        Daten.project.setPreModifyRecordCallbackEnabled(false);
        String[] keys = importData.keySet().toArray(new String[0]);
        StatusRecord[] statusBeforeImport = Daten.project.getStatus(false).getAllStatus();
        Arrays.sort(keys);
        int totalWarnings = 0;
        int totalErrors = 0;
        for (int run = 1; run <= 4; run++) {
            for (String key : keys) {
                ImportMetadata meta = importData.get(key);
                if (!meta.selected) {
                    continue;
                }
                ImportBase importJob = null;
                if (run == 1) {
                    switch (meta.type) {
                        case ImportMetadata.TYPE_SYNONYME_MITGLIEDER:
                            importJob = new ImportSynonyms(this, meta.filename, meta);
                            break;
                        case ImportMetadata.TYPE_SYNONYME_BOOTE:
                            importJob = new ImportSynonyms(this, meta.filename, meta);
                            break;
                        case ImportMetadata.TYPE_SYNONYME_ZIELE:
                            importJob = new ImportSynonyms(this, meta.filename, meta);
                            break;
                        case ImportMetadata.TYPE_ADRESSEN:
                            importJob = new ImportAddresses(this, meta.filename, meta);
                            break;
                    }
                }
                if (run == 2) {
                    switch (meta.type) {
                        case ImportMetadata.TYPE_FAHRTENBUCH:
                            importJob = new ImportLogbook(this, meta.filename, meta);
                            newestLogbookName = meta.name;
                            break;
                    }
                }
                if (run == 3) {
                    switch (meta.type) {
                        case ImportMetadata.TYPE_GRUPPEN:
                            importJob = new ImportGroups(this, meta.filename, meta);
                            break;
                        case ImportMetadata.TYPE_MANNSCHAFTEN:
                            importJob = new ImportCrews(this, meta.filename, meta);
                            break;
                        case ImportMetadata.TYPE_BOOTSTATUS:
                            importJob = new ImportBoatStatus(this, meta.filename, meta);
                            break;
                        case ImportMetadata.TYPE_FAHRTENABZEICHEN:
                            importJob = new ImportFahrtenabzeichen(this, meta.filename, meta);
                            break;
                    }
                }
                if (run == 4) {
                    // Postprocessing after all data has been imported
                    ImportBoats.runPostprocessing(boatsAllowedGroups, boatsRequiredGroup, groupMapping);

                    break; // exit loop
                }

                boolean result = false;
                if (importJob != null) {
                    result = importJob.runImport();
                    if (result) {
                        successCnt++;
                    } else {
                        errorCnt++;
                    }
                    setCurrentWorkDone(i++);
                    totalWarnings += importJob.getWarningCount();
                    totalErrors += importJob.getErrorCount();
                }
            }
        }

        // remove pre-defined efa2 status that are not used by efa1 imported data
        try {
            String[] importedStatusNames = (statusKeys != null ?
                statusKeys.keySet().toArray(new String[0]) : null);
            for (int j = 0; statusBeforeImport != null && j < statusBeforeImport.length; j++) {
                if (!StatusRecord.TYPE_USER.equals(statusBeforeImport[j].getType())) {
                    continue;
                }
                boolean statusUsedInImport = false;
                for (int k = 0; importedStatusNames != null && k < importedStatusNames.length; k++) {
                    UUID uuid = statusKeys.get(importedStatusNames[k]);
                    if (statusBeforeImport[j].getId().equals(uuid)) {
                        statusUsedInImport = true;
                        break;
                    }
                }
                if (!statusUsedInImport) {
                    Daten.project.getStatus(false).deleteStatus(statusBeforeImport[j].getId());
                }
            }
        } catch(Exception estatus) {
            logInfo("ERROR   - Could not delete unused status - " + estatus.toString() + "\n", true, true);
            Logger.logdebug(estatus);
            totalErrors++;
        }

        try {
            Daten.project.closeAllStorageObjects();
            Daten.project.open(false);
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        Daten.project.setPreModifyRecordCallbackEnabled(true);
        String msg = International.getMessage("{count} Dateien wurden importiert.", successCnt);
        if (errorCnt > 0) {
            msg += "\n" + International.getMessage("Der Import von {count} Dateien wurde wegen Fehlern abgebrochen.", errorCnt);
        }
        msg += "\n" + International.getMessage("Es traten {count} Warnungen und {count} Fehler auf.", totalWarnings, totalErrors);
        logInfo(LogString.operationFinished(International.getString("Import"))+"\n");
        logInfo(msg+"\n");
        Dialog.infoDialog(msg);
        setDone();
    }

    public void setSynonymeMitglieder(Hashtable<String,String> syn) {
        this.synMitglieder = syn;
    }

    public void setSynonymeBoote(Hashtable<String,String> syn) {
        this.synBoote = syn;
    }

    public void setSynonymeZiele(Hashtable<String,String> syn) {
        this.synZiele = syn;
    }

    public String synMitglieder_getMainName(String syn) {
        return ( synMitglieder != null && synMitglieder.get(syn) != null ? synMitglieder.get(syn) : syn);
    }

    public void synMitglieder_setKeyForMainName(String mainName, DataKey key) {
        if (personMainName2Key == null) {
            personMainName2Key = new Hashtable<String,DataKey>();
        }
        personMainName2Key.put(mainName, key);
    }

    public DataKey synMitglieder_getKeyForMainName(String mainName) {
        return (personMainName2Key != null ?
            personMainName2Key.get(mainName): null);
    }

    public String synBoote_getMainName(String syn) {
        return ( synBoote != null && synBoote.get(syn) != null ? synBoote.get(syn) : syn);
    }

    public String synZiele_getMainName(String syn) {
        return ( synZiele != null && synZiele.get(syn) != null ? synZiele.get(syn) : syn);
    }

    public void synZiele_setKeyForMainName(String mainName, DataKey key) {
        if (destinationMainName2Key == null) {
            destinationMainName2Key = new Hashtable<String,DataKey>();
        }
        destinationMainName2Key.put(mainName, key);
    }

    public DataKey synZiele_getKeyForMainName(String mainName) {
        return (destinationMainName2Key != null ?
            destinationMainName2Key.get(mainName): null);
    }

    public void setStatusKey(String statusName, UUID statusId) {
        if (statusKeys == null) {
            statusKeys = new Hashtable<String,UUID>();
        }
        statusKeys.put(statusName, statusId);
    }

    public UUID getStatusKey(String statusName) {
        return (statusKeys != null ? statusKeys.get(statusName) : null);
    }

    public void setAddresses(Hashtable<String,String> addr) {
        this.addresses = addr;
    }

    public String getAddress(String name) {
        return (addresses != null ? addresses.get(name) : null);
    }

    public void setBoatsAllowedGroups(Hashtable<DataKey,String> boatsAllowedGroups) {
        this.boatsAllowedGroups = boatsAllowedGroups;
    }

    public Hashtable<DataKey,String> getBoatsAllowedGroups() {
        return boatsAllowedGroups;
    }

    public void setBoatsRequiredGroup(Hashtable<DataKey,String> boatsRequiredGroup) {
        this.boatsRequiredGroup = boatsRequiredGroup;
    }

    public Hashtable<DataKey,String> getBoatsRequiredGroup() {
        return boatsRequiredGroup;
    }

    public void setGroupMapping(Hashtable<String,UUID> groupMapping) {
        this.groupMapping = groupMapping;
    }

    public int getAbsoluteWork() {
        return importData.size();
    }

    public String getSuccessfullyDoneMessage() {
        return LogString.operationSuccessfullyCompleted(International.getString("Import"));
    }

    public String getNewestLogbookName() {
        return newestLogbookName;
    }

    public ImportMetadata getKeyStoreMetadata() {
        String[] keys = importData.keySet().toArray(new String[0]);
        for(String key : keys) {
            if (importData.get(key).type == ImportMetadata.TYPE_KEYSTORE) {
                return importData.get(key);
            }
        }
        return null;
    }

}
