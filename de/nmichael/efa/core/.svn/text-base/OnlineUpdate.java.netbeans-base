/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.Backup;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.gui.OnlineUpdateDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.DownloadThread;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.ExecuteAfterDownload;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.XmlHandler;
import java.awt.Window;
import java.io.File;
import java.util.Vector;
import javax.swing.JDialog;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;

public class OnlineUpdate {

    private static String lastError;

    public synchronized static boolean runOnlineUpdate(JDialog parent, String eouFile) {
        Vector<OnlineUpdateInfo> versions = null;
        lastError = null;

        // Online Update
        if (parent != null &&
            !Dialog.okAbbrDialog(International.getString("Online-Update"),
                International.getString("Prüfen auf neue Programmversion") + "\n\n" +
                International.getString("Bitte stelle eine Verbindung zum Internet her."))) {
            return false;
        }

        // aktuelle Versionsnummer aus dem Internet besorgen
        String versionFile = Daten.efaTmpDirectory + "eou.xml";
        if (!DownloadThread.getFile(parent, eouFile, versionFile, true)) {
            lastError = International.getString("Keine neue Version gefunden!");
            return false;
        }

        try {
            XMLReader parser = EfaUtil.getXMLReader();
            OnlineUpdateFileParser ou = new OnlineUpdateFileParser();
            parser.setContentHandler(ou);
            parser.parse(versionFile);
            versions = ou.getVersions();
        } catch (Exception ee) {
            lastError = International.getString("Keine neue Version gefunden!")
                        + "\n" + ee.getMessage();
            if (parent != null) {
                Dialog.error(lastError);
            }
            EfaUtil.deleteFile(versionFile);
            return false;
        }
        if (versions == null || versions.size() == 0) {
            lastError = International.getString("Keine neue Version gefunden!");
            if (parent != null) {
                Dialog.error(lastError);
            }
            EfaUtil.deleteFile(versionFile);
            return false;
        }

        if (Daten.efaConfig != null) {
            Daten.efaConfig.setValueEfaVersionLastCheck(System.currentTimeMillis());
        }

        // ist die installierte Version aktuell?
        OnlineUpdateInfo newestVersion = versions.get(0); // first version is always newest one!
        if (Daten.VERSIONID.equals(newestVersion.versionId) ||
            Daten.VERSIONID.compareTo(newestVersion.versionId) > 0) {
            if (parent != null) {
                Dialog.infoDialog(International.getString("Es liegt derzeit keine neuere Version von efa vor.") + "\n"
                        + International.getMessage("Die von Dir benutzte Version {version} ist noch aktuell.",
                        Daten.VERSIONID));
            } else {
                lastError = International.getString("Es liegt derzeit keine neuere Version von efa vor.");
            }
            EfaUtil.deleteFile(versionFile);
            return false;
        }

        // Ok, es gibt eine neue Version --> Infos über diese Version einlesen
        Vector<String> changes = new Vector<String>();
        for (int i=0; i<versions.size(); i++) {
            OnlineUpdateInfo version = versions.get(i);
            if (Daten.VERSIONID.compareTo(version.versionId) >= 0) {
                break;
            }
            Vector<String> moreChanges = version.getChanges();
            for (String s : moreChanges) {
                if (!changes.contains(s)) {
                    changes.add(s);
                }
            }
        }

        // Ok, Informationen gelesen: Jetzt auf dem Bildschirm anzeigen
        if (parent != null) {
            OnlineUpdateDialog dlg = new OnlineUpdateDialog(parent,
                    newestVersion.versionId, newestVersion.releaseDate,
                    newestVersion.downloadSize, changes);
            dlg.showDialog();
            if (!dlg.getDialogResult()) {
                return false;
            }
        }

        // Ok, jetzt pruefen, ob Benutzer Schreibrechte im efa-Directory hat
        String writeTestFile = Daten.efaMainDirectory + "writetest.tmp";
        boolean canWrite = true;
        try {
            EfaUtil.deleteFile(writeTestFile); // just to make sure there is no such file
            if (!(new File(writeTestFile)).createNewFile()) {
                canWrite = false;
            }
        } catch (Exception e) {
            canWrite = false;
        } finally {
            EfaUtil.deleteFile(writeTestFile);
        }
        if (!canWrite) {
            if (parent != null) {
                Dialog.error(LogString.directoryNoWritePermission(Daten.efaMainDirectory, International.getString("Verzeichnis")) +
                        "\n\n" +
                        International.getMessage("Bitte wiederhole das Online-Update als {osname}-Administrator.", Daten.osName));
            } else {
                lastError = LogString.directoryNoWritePermission(Daten.efaMainDirectory, International.getString("Verzeichnis"));
            }
            return false;
        }

        // Download des Updates
        String zipFile = Daten.efaTmpDirectory + "eou.zip";
        ExecuteAfterDownload afterDownload = new ExecuteAfterDownloadImpl(parent,
                zipFile, newestVersion.downloadSize, newestVersion.versionId);
        if (parent != null) {
            parent.setEnabled(false);
            if (!DownloadThread.getFile(parent, newestVersion.downloadUrl, zipFile, afterDownload)) {
                parent.setEnabled(true);
                return false;
            }
        } else {
            Logger.log(Logger.INFO, Logger.MSG_EVT_REMOTEONLINEUPDATEDOWNLOAD,
                    International.getMessage("Download von {file}", newestVersion.downloadUrl) + " ...");
            if (!DownloadThread.getFile(parent, newestVersion.downloadUrl, zipFile, true)) {
                lastError = LogString.operationFailed(International.getString("Download"));
                return false;
            }
            Logger.log(Logger.INFO, Logger.MSG_EVT_REMOTEONLINEUPDATEDOWNLOAD,
                    LogString.operationSuccessfullyCompleted(
                        International.getMessage("Download von {file}", newestVersion.downloadUrl)));
            afterDownload.success();
        }
        return true;
    }

    public static String getLastError() {
        return lastError;
    }

}

class OnlineUpdateFileParser extends XmlHandler {

    public static String XML_ONLINEUPDATE = "efaOnlineUpdate";
    public static String XML_VERSION = "Version";
    public static String XML_VERSION_ID = "VersionID";
    public static String XML_RELEASE_DATE = "ReleaseDate";
    public static String XML_DOWNLOAD_URL = "DownloadUrl";
    public static String XML_DOWNLOAD_SIZE = "DownloadSize";
    public static String XML_CHANGES = "Changes";
    public static String XML_CHANGES_PROPERTY_LANG = "lang";
    public static String XML_CHANGE_ITEM = "ChangeItem";

    private OnlineUpdateInfo version;
    private String changeItemLang;
    private Vector<OnlineUpdateInfo> versions = new Vector<OnlineUpdateInfo>();

    public OnlineUpdateFileParser() {
        super(XML_ONLINEUPDATE);
    }

    public Vector<OnlineUpdateInfo> getVersions() {
        return versions;
    }

    public void startElement(String uri, String localName, String qname, Attributes atts) {
        super.startElement(uri, localName, qname, atts);

        if (localName.equals(XML_VERSION)) {
            version = new OnlineUpdateInfo();
        }

        if (localName.equals(XML_CHANGES)) {
            changeItemLang = atts.getValue(XML_CHANGES_PROPERTY_LANG);
        }
    }

    public void endElement(String uri, String localName, String qname) {
        super.endElement(uri, localName, qname);

        if (version != null) {
            // end of field
            if (fieldName.equals(XML_VERSION_ID)) {
                version.versionId = getFieldValue();
            }
            if (fieldName.equals(XML_RELEASE_DATE)) {
                version.releaseDate = getFieldValue();
            }
            if (fieldName.equals(XML_DOWNLOAD_URL)) {
                version.downloadUrl = getFieldValue();
            }
            if (fieldName.equals(XML_DOWNLOAD_SIZE)) {
                version.downloadSize = EfaUtil.stringFindInt(getFieldValue(), 0);
            }

            if (localName.equals(XML_CHANGE_ITEM)) {
                if (changeItemLang == null || changeItemLang.length() == 0) {
                    changeItemLang = "en";
                }
                Vector<String> changes = version.changeItems.get(changeItemLang);
                if (changes == null) {
                    changes = new Vector<String>();
                }
                changes.add(getFieldValue());
                version.changeItems.put(changeItemLang, changes);
            }
            if (fieldName.equals(XML_VERSION)) {
                versions.add(version);
            }
        }

    }

}


class ExecuteAfterDownloadImpl implements ExecuteAfterDownload {

    Window parent;
    String zipFile;
    long fileSize;
    String versionId;
    String lastError;

    public ExecuteAfterDownloadImpl(Window parent, String zipFile, long fileSize, String versionId) {
        this.parent = parent;
        this.zipFile = zipFile;
        this.fileSize = fileSize;
        this.versionId = versionId;
    }

    public void success() {
        if (parent != null) {
            parent.setEnabled(true);
        }
        File f = new File(zipFile);
        if (f.length() != fileSize) {
            lastError = International.getString("Der Download ist unvollständig.");
            if (parent != null) {
                Dialog.error(LogString.operationAborted(International.getString("Update")) + "\n" + lastError);
            }
            return;
        }

        // Download war erfolgreich
        if (parent != null) {
            Dialog.infoDialog(
                    LogString.operationSuccessfullyCompleted(International.getString("Download"))
                    + (Daten.applID != Daten.APPL_DRV ? "\n" +
                       International.getString("Es werden jetzt alle Daten gesichert und anschließend die neue Version installiert.")
                       : ""));
        }

        // ZIP-Archiv mit bisherigen Daten sichern
        if (Daten.applID != Daten.APPL_DRV) {
            boolean backupProject = true;
            if (Daten.project == null || !Daten.project.isOpen()
                    || Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                backupProject = false;
            }
            Backup backup = new Backup(Daten.efaBakDirectory, null, backupProject, true);
            if (backup.runBackup(null) != 0) {
                lastError = LogString.operationFailed(International.getString("Backup"));
                if (parent == null) {
                    return;
                }
                if (Dialog.yesNoDialog(lastError,
                        backup.getLastErrorMessage()
                        + "\n"
                        + International.getString("Soll der Update-Vorgang trotzdem fortgesetzt werden?")) != Dialog.YES) {
                    return;
                }
            }
        }

        // Neue Version entpacken
       // if (Date)
        String result = null;
        if (Daten.isOsLinux()) {
            result= EfaUtil.unzip(zipFile, Daten.efaMainDirectory, ".jar", ".jar.new");
        } else {
            result= EfaUtil.unzip(zipFile, Daten.efaMainDirectory);
        }
        if (result != null) {
            if (result.length() > 1000) {
                result = result.substring(0, 1000);
            }
            lastError = LogString.operationFailed(International.getString("Installation")) +
                    "\n" + result;
            if (parent != null) {
                Dialog.error(lastError);
            }
            return;
        }

        // Erfolgreich
        if (parent != null) {
            Dialog.infoDialog(International.getString("Version aktualisiert"),
                    LogString.operationSuccessfullyCompleted(International.getString("Installation des Updates"))
                    + "\n" +
                    International.getString("efa wird nun neu gestartet."));
        }
        Logger.log(Logger.INFO, Logger.MSG_EVT_ONLINEUPDATEFINISHED,
                LogString.operationSuccessfullyCompleted(
                International.getMessage("Online-Update auf Version {version}", versionId)));
        if (parent != null) {
            if (Daten.program != null) {
                Daten.haltProgram(Daten.program.restart());
            } else {
                Daten.haltProgram(Daten.HALT_SHELLRESTART);
            }
        }
    }

    public void failure(String text) {
        parent.setEnabled(true);
        Dialog.infoDialog(LogString.operationFailed(International.getString("Installation")));
    }
}
