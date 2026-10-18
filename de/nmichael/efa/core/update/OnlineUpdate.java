/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.update;

import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;

import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.Backup;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.gui.OnlineUpdateDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.DownloadThread;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.EfaZIPUtil;
import de.nmichael.efa.util.EfaZIPUtil.UnzipResult;
import de.nmichael.efa.util.ExecuteAfterDownload;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.XmlHandler;

/**
 * The OnlineUpdate class provides functionality to check for and perform online updates for the efa application.
 * It handles fetching update information, displaying update dialogs, downloading updates, and applying them.
 */

public class OnlineUpdate {

    private static String lastError;

    /**
	 * Run the old online update process using GitHub as the source.
	 * Uses the newest release only and does not need a Dialog to be displayed.
	 * @param parent The parent dialog for displaying messages. Can be null if running in headless mode.
	 * @param eouFile The URL of the EOU XML file containing update information.
	 * @return true if the update process was successful, false otherwise.
	 */
    public synchronized static boolean runOnlineUpdateGitHubNewestProd(JDialog parent, boolean allowBetaVersions) {
        return runOnlineUpdate(parent,
            new GithubReleaseUpdateProviderStrategy(Daten.ONLINEUPDATE_INFO_GITHUB_OWNER, 
            		Daten.ONLINEUPDATE_INFO_GITHUB_REPOSITORY, allowBetaVersions),
            new DialogSelectionStrategy()); 
    }
    
    /**
	 * Run the online update process using the specified provider and selection strategies.
	 * @param parent The parent dialog for displaying messages. Can be null if running in headless mode.
	 * @param provider The strategy for fetching available updates.
	 * @param selection The strategy for selecting an update from the available candidates.
	 * @return true if the update process was successful, false otherwise.
	 * */
    public synchronized static boolean runOnlineUpdate(
            JDialog parent,
            UpdateProviderStrategy provider,
            UpdateSelectionStrategy selection) {
        Vector<OnlineUpdateInfo> eouVersionInfo = null;
        lastError = null;

        List<UpdateCandidate> allUpdateCandidates = null;
        try {
           allUpdateCandidates = provider.fetchAvailableUpdates();
        } catch (Exception e) {
			lastError = International.getString("Fehler beim Abrufen der Update-Informationen!")
						+ "\n" + e.getMessage();
			if (parent != null) {
				Dialog.error(lastError);
			}
			return false;
        }
        List<UpdateCandidate> newerCandidates = filterNewerCandidates(allUpdateCandidates, Daten.VERSIONID);

        if (newerCandidates==null || newerCandidates.isEmpty()) {
			if (parent != null) {
				Dialog.infoDialog(International.getString("Es liegt derzeit keine neuere Version von efa vor.") + "\n"
						+ International.getMessage("Die von Dir benutzte Version {version} ist noch aktuell.",
						Daten.VERSIONID));
			} else {
				lastError = International.getString("Es liegt derzeit keine neuere Version von efa vor.");
			}
			return true;
		}
        
        // Update check timestamp speichern, damit nicht bei jedem Start von efa ein Online-Update-Check durchgeführt wird
        if (Daten.efaConfig != null) {
            Daten.efaConfig.setValueEfaVersionLastCheck(System.currentTimeMillis());
        }
        
    	UpdateCandidate newestCandidate = newerCandidates.get(0); // first candidate is always the newest one!

        if (!VersionIdComparator.isNewer(newestCandidate.getVersionId(), Daten.VERSIONID)) {
            if (parent != null) {
                Dialog.infoDialog(International.getString("Es liegt derzeit keine neuere Version von efa vor.") + "\n"
                        + International.getMessage("Die von Dir benutzte Version {version} ist noch aktuell.",
                        Daten.VERSIONID));
            } else {
                lastError = International.getString("Es liegt derzeit keine neuere Version von efa vor.");
            }

            return true;
        }

        UpdateCandidate selected = selection.select(parent, provider, newerCandidates, Daten.VERSIONID);
        

        if (selected == null) {
        	// user selected cancel or no newer release available
        	return false;
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

        // If the update was triggered remotely through efaRemote, update user statistics
        if (parent == null) {
            OnlineUpdate.submitUserInfos(newestCandidate.getVersionId());
        }

        return downloadAndApplyUpdate(parent, newestCandidate);
    }

	private static boolean downloadAndApplyUpdate(JDialog parent, UpdateCandidate newestCandidate) {
		// Download des Updates
        String zipFile = Daten.efaTmpDirectory + "eou.zip";
        ExecuteAfterDownload afterDownload = new ExecuteAfterDownloadImpl(parent,
                zipFile, newestCandidate.getDownloadSize(), newestCandidate.getVersionId());
        if (parent != null) {
            parent.setEnabled(false);
            if (!DownloadThread.getFile(parent, newestCandidate.getDownloadUrl(), zipFile, afterDownload)) {
                parent.setEnabled(true);
                return false;
            }
        } else {
            Logger.log(Logger.INFO, Logger.MSG_EVT_REMOTEONLINEUPDATEDOWNLOAD,
                    International.getMessage("Download von {file}", newestCandidate.getDownloadUrl()) + " ...");
            if (!DownloadThread.getFile(parent, newestCandidate.getDownloadUrl(), zipFile, true)) {
                lastError = LogString.operationFailed(International.getString("Download"));
                return false;
            }
            Logger.log(Logger.INFO, Logger.MSG_EVT_REMOTEONLINEUPDATEDOWNLOAD,
                    LogString.operationSuccessfullyCompleted(
                        International.getMessage("Download von {file}", newestCandidate.getDownloadUrl())));
            afterDownload.success();
        }
        return true;
	}
    
    /**
	 * Filters the list of all update candidates to only include those that are newer than the specified version ID.
	 * @param allUpdateCandidates The list of all available update candidates.
	 * @param versionid The current version ID to compare against.
	 * @return A list of update candidates that are newer than the specified version ID.
	 */
    private static List<UpdateCandidate> filterNewerCandidates(List<UpdateCandidate> allUpdateCandidates,
			String versionid) {
    	
        List<UpdateCandidate> newer = new Vector<UpdateCandidate>();
        if (allUpdateCandidates == null || allUpdateCandidates.isEmpty()) {
            return newer;
        }

        String current = (versionid == null) ? "" : versionid.trim();

        for (UpdateCandidate candidate : allUpdateCandidates) {
            if (candidate == null || candidate.getVersionId() == null) {
                continue;
            }

            String candidateVersion = candidate.getVersionId().trim();
            if (candidateVersion.length() == 0) {
                continue;
            }

            // Keep only versions strictly newer than current version.
            if (VersionIdComparator.isNewer(candidateVersion, current)) {
                newer.add(candidate);
            }
        }

        // Ensure newest first even if API order changes.
        Collections.sort(newer, new Comparator<UpdateCandidate>() {
            @Override
            public int compare(UpdateCandidate a, UpdateCandidate b) {
                // descending (newest first)
                return VersionIdComparator.compare(a.getVersionId(), b.getVersionId());
            }
        });
        
        return newer;
    }

    /**
	 * Returns the last error message encountered during the online update process.
	 * @return The last error message, or null if no error has occurred.
	 */
	public static String getLastError() {
        return lastError;
    }

	/**
	 * Collects and formats user information for transmission during the online update process.
	 * @param newVersionName The name of the new version being installed.
	 * @return A formatted string containing user information, or null if the project is not open or the club name is not set.
	 * */
	
    public static String getTransmitUserInfo(String newVersionName) {
        if (Daten.project == null || !Daten.project.isOpen()) {
            return null;
        }
        if (Daten.project.getClubName() == null || Daten.project.getClubName().length() == 0) {
            return null;
        }
        StringBuilder s = new StringBuilder();
        s.append(International.getString("Verein") + ": " + Daten.project.getClubName() + "\n");
        s.append(International.getString("Sprache") + ": " + International.getLanguageDescription() + "\n");
        s.append(International.getString("Sportarten") + ": " +
                (Daten.efaConfig.getValueUseFunctionalityRowing() ? International.getString("Rudern") : "") + " " +
                (Daten.efaConfig.getValueUseFunctionalityCanoeing() ? International.getString("Kanu") : "") + "\n");
        s.append(International.getString("efa Version") + ": " + newVersionName + "\n");
        if (Daten.EFALIVE_VERSION != null) {
            s.append(International.getString("efaLive Version") + ": " + Daten.EFALIVE_VERSION + "\n");
        }
        s.append(International.getString("Java Version") + ": " + Daten.javaVersion + "\n");
        if (Daten.applID == Daten.APPL_EFABH || Daten.EFALIVE_VERSION != null) {
            s.append(International.getString("Verwendung") + ": " +
                    (Daten.applID == Daten.APPL_EFABH ? International.getString("efa-Bootshaus") : "") + " " +
                    (Daten.EFALIVE_VERSION != null ? "efaLive " + Daten.EFALIVE_VERSION : "") + "\n");
        }
        return s.toString();
    }

    /**
	 * Submits user information to the specified server during the online update process.
	 * @param newVersionName The name of the new version being installed.
	 * */
    public static void submitUserInfos(String newVersionName) {
        String infos = getTransmitUserInfo(newVersionName);
        if (infos == null || Daten.INTERNET_EFAMAIL == null) {
            return;
        }
        try {
            URL url = new URL(Daten.INTERNET_EFAMAIL);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write("subject=User efa - " + newVersionName + " (Online-Update)" +
                    "&comments=" + URLEncoder.encode(infos, "ISO-8859-1") +
                    "&club=" + URLEncoder.encode(Daten.project.getClubName(), "ISO-8859-1") +
                    "&efa.version=" + URLEncoder.encode(newVersionName, "ISO-8859-1") +
                    (Daten.EFALIVE_VERSION != null ? "&efalive.version=" + URLEncoder.encode(Daten.EFALIVE_VERSION, "ISO-8859-1") : "") +
                    (Daten.project != null && Daten.project.isOpen() && Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD ? "efacloud=efaCloud" : "")  +
                    "&java.version=" + URLEncoder.encode(Daten.javaVersion, "ISO-8859-1") +
                    "&app=" + URLEncoder.encode(Daten.applName, "ISO-8859-1"));
            out.flush();
            out.close();
            InputStream in = new BufferedInputStream(connection.getInputStream());
            BufferedReader buf = new BufferedReader(new InputStreamReader(in));
            @SuppressWarnings("unused")
			String s;
            while ((s = buf.readLine()) != null) {
                // nothing
            }
        } catch (Exception e) {
            return;
        }
    }
}

/**	
 * Parses the XML file containing online update information and extracts relevant details.
 * The expected XML structure is as follows:
 * <efaOnlineUpdate>
 *     <Version>
 *         <VersionID>...</VersionID>
 *         <ReleaseDate>...</ReleaseDate>
 *         <DownloadUrl>...</DownloadUrl>
 *         <DownloadSize>...</DownloadSize>
 *         <Changes lang="...">
 *             <ChangeItem>...</ChangeItem>
 *             ...
 *         </Changes>
 *     </Version>
 *     ...
 * </efaOnlineUpdate>
 */
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
    
    /**
	 * Called when the parser encounters the start of an element.
	 * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
	 * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qname The qualified name (with prefix), or the empty string if qualified names are not available.
	 * @param atts The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
	 */
    public void startElement(String uri, String localName, String qname, Attributes atts) {
        super.startElement(uri, localName, qname, atts);

        if (localName.equals(XML_VERSION)) {
            version = new OnlineUpdateInfo();
        }

        if (localName.equals(XML_CHANGES)) {
            changeItemLang = atts.getValue(XML_CHANGES_PROPERTY_LANG);
        }
    }

    /**
	 * Called when the parser encounters the end of an element.
	 * @param uri The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
	 * @param localName The local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qname The qualified name (with prefix), or the empty string if qualified names are not available.
	 * */
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

/**
 * Implements the ExecuteAfterDownload interface to handle the actions to be taken after a successful or failed download.
 * This includes verifying the downloaded file size, backing up existing data, unzipping the downloaded update, and restarting the application if necessary.
 */
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
    /**
	 * Called when the download is successful. This method verifies the downloaded file size, backs up
	 * existing data, unzips the downloaded update, and restarts the application if necessary.
	 * */
    public void success() {
        if (parent != null) {
            parent.setEnabled(true);
        }
        File f = new File(zipFile);
        if ((fileSize>0) && (f.length() != fileSize)) {
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
            Backup backup = new Backup(Daten.efaBakDirectory, null, backupProject, true, true); // do not include efa log during onlineupdate backup
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

        //Using new unzip method
        UnzipResult result = null;
        if (Daten.isOsLinux()) {
            result= EfaZIPUtil.unzipToResult(zipFile, Daten.efaMainDirectory, ".jar", ".jar.new");
        } else {
            result= EfaZIPUtil.unzipToResult(zipFile, Daten.efaMainDirectory);
        }
        
        if (result != null) {
        	if (result.getStatus()==EfaZIPUtil.UnzipStatus.FAILURE) {
        		String errorText = result.getErrorsAsString();
        		if (errorText.length() > 1000) {
        			errorText = errorText.substring(0, 1000);
        		}
        		
	            lastError = LogString.operationFailed(International.getString("Installation")) +
	                    "\n" + errorText;
	            if (parent != null) {
	                Dialog.error(lastError);
	            }
	            return;
        	}
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

    /**
	 * Called when the download fails. This method enables the parent window and displays an error message
	 * indicating that the operation failed.
	 * */
    public void failure(String text) {
        parent.setEnabled(true);
        Dialog.infoDialog(LogString.operationFailed(International.getString("Installation")));
    }
}
