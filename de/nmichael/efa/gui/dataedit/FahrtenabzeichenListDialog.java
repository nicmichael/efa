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
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.efawett.EfaWettClient;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import javax.swing.*;


// @i18n complete
public class FahrtenabzeichenListDialog extends DataListDialog {

    public static final int ACTION_GETFAHRTENHEFTE = 901; // negative actions will not be shown as popup actions

    public FahrtenabzeichenListDialog(Frame parent, AdminRecord admin) {
        super(parent, International.onlyFor("Fahrtenabzeichen","de"), 
                Daten.project.getFahrtenabzeichen(false), 0, admin);
        ini();
    }

    public FahrtenabzeichenListDialog(JDialog parent, AdminRecord admin) {
        super(parent, International.onlyFor("Fahrtenabzeichen","de"), 
                Daten.project.getFahrtenabzeichen(false), 0, admin);
        ini();
    }

    private void ini() {
        actionText = new String[]{
                    ItemTypeDataRecordTable.ACTIONTEXT_NEW,
                    ItemTypeDataRecordTable.ACTIONTEXT_EDIT,
                    ItemTypeDataRecordTable.ACTIONTEXT_DELETE,
                    International.onlyFor("Fahrtenhefte downloaden", "de"),
                    International.getString("Importieren"),
                    International.getString("Exportieren"),
                    International.getString("Liste ausgeben")
                };
        actionType = new int[]{
                    ItemTypeDataRecordTable.ACTION_NEW,
                    ItemTypeDataRecordTable.ACTION_EDIT,
                    ItemTypeDataRecordTable.ACTION_DELETE,
                    ACTION_GETFAHRTENHEFTE,
                    ACTION_IMPORT,
                    ACTION_EXPORT,
                    ACTION_PRINTLIST
                };
            actionImage = new String[] {
                BaseDialog.IMAGE_ADD,
                BaseDialog.IMAGE_EDIT,
                BaseDialog.IMAGE_DELETE,
                BaseDialog.IMAGE_DOWNLOAD,
                BaseDialog.IMAGE_IMPORT,
                BaseDialog.IMAGE_EXPORT,
                BaseDialog.IMAGE_LIST
                    };

    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public DataEditDialog createNewDataEditDialog(JDialog parent, StorageObject persistence, DataRecord record) {
        boolean newRecord = (record == null);
        if (record == null) {
            record = Daten.project.getFahrtenabzeichen(false).createFahrtenabzeichenRecord(null);
        }
        return new FahrtenabzeichenEditDialog(parent, (FahrtenabzeichenRecord)record, newRecord, admin);
    }

    public void itemListenerActionTable(int actionId, DataRecord[] records) {
        super.itemListenerActionTable(actionId, records);
        switch(actionId) {
            case ACTION_GETFAHRTENHEFTE:
                getSignierteFahrtenhefte();
                break;
        }
    }

    public boolean getSignierteFahrtenhefte() {
        String localFile = null;
        switch (Dialog.auswahlDialog("Bestätigungsdatei abrufen",
                "Eine 'Bestätigungsdatei' wird jedes Jahr vom DRV nach der Bearbeitung der Meldung zum\n"
                + "DRV-Fahrtenabzeichen erstellt und enthält die in diesem Jahr vom DRV signierten Fahrtenhefte.\n"
                + "Damit efa die Nachweise der elektronisch erbrachten Fahrtenabzeichen erhält, müssen die\n"
                + "Bestätigungsdateien nach der Bearbeitung durch den DRV in efa eingespielt werden.\n"
                + "\n"
                + "efa kann die Bestätigungsdateien selbst aus dem Internet herunterladen\n"
                + "oder eine bereits heruntergeladene Bestätigungsdatei einlesen.\n"
                + "Was möchtest Du tun?",
                "Bestätigungsdatei aus Internet herunterladen", "vorhandene Bestätigungsdatei einlesen")) {
            case 0:

                // erstmal alle Quittungsnummern abfragen
                String request = EfaWettClient.makeScriptRequestString(EfaWettClient.VERBAND_DRV, EfaWettClient.ACTION_QNRLIST, null, null);
                if (request == null) {
                    Dialog.error("Es konnten keine Bestätigungsdateien heruntergeladen werden!");
                    return false;
                }
                if (!Dialog.okAbbrDialog("Verbindung mit Internet herstellen",
                        "Bitte stelle nun eine Verbindung mit dem Internet her und klicke OK.")) {
                    return false;
                }
                localFile = Daten.efaTmpDirectory + "drvSigFahrtenhefte.qnrlist";
                if (!DownloadThread.getFile((JDialog) Dialog.frameCurrent(), request, localFile, true)) {
                    Dialog.error("Es konnten keine Bestätigungsdateien heruntergeladen werden!");
                    return false;
                }
                try {
                    BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(localFile), Daten.ENCODING_ISO));
                    String s;
                    String[] qnr = null;
                    String[] wett = null;
                    while ((s = f.readLine()) != null) {
                        if (s.startsWith("ERROR")) {
                            Dialog.error(s);
                            return false;
                        }
                        if (s.startsWith("QNR=")) {
                            qnr = EfaUtil.kommaList2Arr(s.substring(4), ';');
                        }
                        if (s.startsWith("WETT=")) {
                            wett = EfaUtil.kommaList2Arr(s.substring(5), ';');
                        }
                    }
                    f.close();
                    EfaUtil.deleteFile(localFile);
                    if (qnr == null || wett == null) {
                        Dialog.error("Es konnten keine Bestätigungsdateien gefunden werden!\n"
                                + "Möglicherweise sind noch keine Meldungen eingesandt oder durch den DRV bearbeitet worden.");
                        return false;
                    }
                    return Daten.project.getFahrtenabzeichen(false).downloadFahrtenhefte(qnr, wett, null);
                } catch (Exception e) {
                    Dialog.error("Die heruntergeladene Liste der Quittungsnummern konnte nicht geöffnet werden: " + e.toString());
                    return false;
                }

            case 1:
                localFile = Dialog.dateiDialog(Dialog.frameCurrent(), "Bestätigungsdatei auswählen",
                        "Bestätigungsdatei (*.efwsig)", "efwsig", Daten.efaMainDirectory, false);
                if (localFile == null) {
                    return false;
                }
                return Daten.project.getFahrtenabzeichen(false).downloadFahrtenhefte(null, null, localFile);
            default:
                return false;
        }
    }
}
