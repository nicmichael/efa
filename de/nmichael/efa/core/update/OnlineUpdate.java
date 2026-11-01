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
import java.io.File;
import java.io.InputStream;
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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.Backup;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.gui.EfaGuiUtils;
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
    public synchronized static boolean runOnlineUpdateGitHubNewestProductionRelease(JDialog parent) {
        return runOnlineUpdate(parent,
            new GithubProductionReleaseUpdateProviderStrategy(),
            new AutoNewestSelectionStrategy()/*no dialog needed, just select the newest version*/); 
    }
    
    
    /**
	 * Run the old online update process for efaDRV programs using efa.nmichael.de as the source.
	 * Uses the newest release only and does not need a Dialog to be displayed.
	 * 
	 * This method is only called from efaDRV programs.
	 * 
	 * The DRV releases are hosted on efa.nmichael.de and are not available on GitHub.
	 * The DRV releases are not open source as they contain DRV specific features, 
	 * data and branding - functions which should not be used by anyone else but DRV.
	 * 
	 * Until further notice, the DRV releases are only available on efa.nmichael.de and not on GitHub.
	 * 
	 * @param parent The parent dialog for displaying messages. Can be null if running in headless mode.
	 * 
	 */
    public synchronized static boolean runOnlineUpdateDRVEfaNMichael(JDialog parent) {
		return runOnlineUpdate(parent,
			new EfaNMichaelDeUpdateProviderStrategy(Daten.ONLINEUPDATE_INFO_DRV),
			new AutoNewestSelectionStrategy()/*no dialog needed, just select the newest version*/); 
	}
    
    /**
	 * Run the online update process using the specified provider and selection strategies.
	 * @param parent The parent dialog for displaying messages. Can be null if running in headless mode.
	 * @param provider The strategy for fetching available updates.
	 * @param updateSelector The strategy for selecting an update from the available candidates.
	 * @return true if the update process was successful, false otherwise.
	 * */
    public synchronized static boolean runOnlineUpdate(
            JDialog parent,
            UpdateProviderStrategy provider,
            UpdateSelectionStrategy updateSelector) {
        lastError = null;

        List<UpdateCandidate> newerCandidates;
        try {
            newerCandidates = fetchNewerCandidates(parent, provider);
        } catch (Exception e) {
            lastError = International.getString("Fehler beim Abrufen der Update-Informationen!")
                        + "\n" + e.getMessage();
            if (parent != null) {
                Dialog.error(lastError);
            }
            return false;
        }

        // Update check timestamp speichern, damit nicht bei jedem Start von efa ein Online-Update-Check durchgeführt wird
        if (Daten.efaConfig != null) {
            Daten.efaConfig.setValueEfaVersionLastCheck(System.currentTimeMillis());
        }


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

        UpdateCandidate selected = updateSelector.select(parent, provider, newerCandidates, Daten.VERSIONID);
        

        if (selected == null) {
        	// user selected cancel or no newer release available
        	return false;
        }
        
        // Ok, jetzt pruefen, ob Benutzer Schreibrechte im efa-Directory hat
        if (!checkWritePermission(parent)) {
            return false;
        }

        // If the update was triggered remotely through efaRemote, update user statistics
        if (parent == null) {
            OnlineUpdate.submitUserInfos(newestCandidate.getVersionId());
        }

        return downloadAndApplyUpdate(parent, newestCandidate);
    }

	/**
	 * Fetches all available update candidates from the provider and filters them to newer versions.
	 * Shows the wait cursor during both steps.
	 * @param parent The parent dialog, can be null.
	 * @param provider The strategy for fetching available updates.
	 * @return The newer candidates, newest first.
	 * @throws Exception if the provider fails to fetch the update information.
	 */
	private static List<UpdateCandidate> fetchNewerCandidates(JDialog parent, UpdateProviderStrategy provider) throws Exception {
		EfaGuiUtils.setWaitCursor(parent, true);
		try {
			List<UpdateCandidate> allUpdateCandidates = provider.fetchAvailableUpdates();
			return filterNewerCandidates(allUpdateCandidates, Daten.VERSIONID);
		} finally {
			EfaGuiUtils.setWaitCursor(parent, false);
		}
	}

	/**
	 * Checks whether the current user can write into the efa main directory.
	 * Sets lastError or shows an error dialog if not.
	 * @param parent The parent dialog, can be null.
	 * @return true if writing is possible, false otherwise.
	 */
	private static boolean checkWritePermission(JDialog parent) {
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
		}
		return canWrite;
	}

	/**
	 * Downloads the update from the specified candidate and applies it to the local installation.
	 * 
	 * @param parent The parent dialog, can be null.
	 * @param newestCandidate The update candidate to download and apply.
	 * @return true if the download and application were successful, false otherwise.
	 */
	private static boolean downloadAndApplyUpdate(JDialog parent, UpdateCandidate newestCandidate) {
		// Target ZIP file for the downloaded update. Existing file will be overwritten.
		// Downloaded file will NOT be deleted after the update.
        String zipFile = Daten.efaTmpDirectory + "eou.zip";
        
        ExecuteAfterDownload afterDownload = new ExecuteAfterDownloadImpl(parent,
                zipFile, newestCandidate.getDownloadSize(), newestCandidate.getVersionId());
        
        if (parent != null) {
        	// Disable the parent dialog while downloading to prevent user interaction.
            parent.setEnabled(false);
            
            // Download the update file. If the download fails, re-enable the parent dialog and return false.
            // The Downloadthread shows its own progress dialog and handles cancellation and error messages.
            if (!DownloadThread.getFile(parent, newestCandidate.getDownloadUrl(), zipFile, afterDownload)) {
                parent.setEnabled(true);
                return false;
            }
        } else {
        	// Download the update file without a parent dialog. 
        	// This is used for remote updates where no GUI is available.
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
        // The comparator sorts the updates descending by version ID, so the newest version is first in the list.
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

	public static void setLastError(String error) {
		lastError = error;
	}
	
	/**
	 * Collects and formats user information for transmission during the online update process.
	 * @param newVersionName The name of the new version being installed.
	 * @return A formatted string containing user information, or null if the project is not open or the club name is not set.
	 */
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
        
        boolean rowing  = Daten.efaConfig != null && Daten.efaConfig.getValueUseFunctionalityRowing();
        boolean canoeing = Daten.efaConfig != null && Daten.efaConfig.getValueUseFunctionalityCanoeing();
        s.append(International.getString("Sportarten") + ": " +
                (rowing ? International.getString("Rudern") : "") + " " +
                (canoeing ? International.getString("Kanu") : "") + "\n");
        
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
	 * This is done "fire and forget": the actual network transmission happens asynchronously
	 * in a background thread, so the caller is never blocked (e.g. by slow DNS/connect/read
	 * timeouts) and does not need to wait for the result. Any failure is only logged for debugging.
	 *
	 * All data needed for the HTTP request (club name, storage type, Java/efa/efaLive versions, ...)
	 * is read from {@link Daten} / {@link Daten#project} synchronously on the calling thread before
	 * the background thread is started. This avoids race conditions / NullPointerExceptions in case
	 * {@code Daten.project} is closed or modified concurrently while the background submission is
	 * still in progress.
	 *
	 * @param newVersionName The name of the new version being installed.
	 * */
    public static void submitUserInfos(final String newVersionName) {
        final String infos = getTransmitUserInfo(newVersionName);
        final String emailUrl = Daten.INTERNET_EFAMAIL;
        if (infos == null || emailUrl == null || emailUrl.length() == 0) {
            return;
        }

        // Snapshot all values needed for the request NOW (on the calling thread), so the
        // background thread does not depend on mutable global state (Daten.project etc.)
        // which could change or become unavailable while the request is still in flight.
        final String clubName = Daten.project.getClubName();
        final boolean isEfaCloud = Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD;
        final String javaVersion = Daten.javaVersion;
        final String appName = Daten.applName;
        final String efaLiveVersion = Daten.EFALIVE_VERSION;

        Thread t = new Thread(new Runnable() {
            public void run() {
                submitUserInfosBlocking(newVersionName, infos, emailUrl, clubName,
                        isEfaCloud, javaVersion, appName, efaLiveVersion);
            }
        }, "OnlineUpdate-SubmitUserInfos");
        t.setDaemon(true); // does not prevent JVM shutdown if the main thread exits
        t.start();
    }

    /**
	 * Performs the actual (blocking) HTTP submission of user information. Must not be called
	 * directly from UI code; use {@link #submitUserInfos(String)} instead, which runs this in
	 * a background thread ("fire and forget") and provides all the required snapshot data.
	 * @param newVersionName The name of the new version being installed.
	 * @param infos The already-collected user info text.
	 * @param emailUrl The URL to submit the data to.
	 * @param clubName The club name, snapshotted from Daten.project before going async.
	 * @param isEfaCloud Whether the project storage type is efaCloud.
	 * @param javaVersion The Java version string.
	 * @param appName The application name.
	 * @param efaLiveVersion The efaLive version, or null if not running under efaLive.
	 */
    private static void submitUserInfosBlocking(String newVersionName, String infos, String emailUrl,
            String clubName, boolean isEfaCloud, String javaVersion, String appName, String efaLiveVersion) {
        URLConnection connection = null;
        try {
            String body = "subject=User efa - " + newVersionName + " (Online-Update)" +
                    "&comments=" + URLEncoder.encode(infos, "ISO-8859-1") +
                    "&club=" + URLEncoder.encode(clubName != null ? clubName : "", "ISO-8859-1") +
                    "&efa.version=" + URLEncoder.encode(newVersionName, "ISO-8859-1") +
                    (efaLiveVersion != null ? "&efalive.version=" + URLEncoder.encode(efaLiveVersion, "ISO-8859-1") : "") +
                    (isEfaCloud ? "&efacloud=efaCloud" : "") +
                    "&java.version=" + URLEncoder.encode(javaVersion, "ISO-8859-1") +
                    "&app=" + URLEncoder.encode(appName, "ISO-8859-1");

            connection = new URL(emailUrl).openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "ISO-8859-1")) {
                out.write(body);
            }
            // read the response to ensure the request is completed, but we don't need to process it
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[1024];
                while (in.read(buffer) != -1) {
                    // nothing
                }
            }
        } catch (Exception e) {
        	Logger.logdebug(e);
            // transmitting user info failed, but we don't want to interrupt the update process for this
        } finally {
            // release the underlying socket/connection instead of relying on garbage collection
            if (connection instanceof java.net.HttpURLConnection) {
                ((java.net.HttpURLConnection) connection).disconnect();
            }
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
 *             <ChangeItem section="...">...</ChangeItem>
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
    
    // NEW: sets the error locally and globally (also works in the efaRemote case without a dialog)
    private void setError(String err) {
        lastError = err;
        OnlineUpdate.setLastError(err);
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
        if ((fileSize > 0) && (f.length() != fileSize)) {
            setError(International.getString("Der Download ist unvollständig."));
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
                setError(LogString.operationFailed(International.getString("Backup")));
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

        // Entpacken (neue Unzip-Methode)
        UnzipResult result = null;
        if (Daten.isOsLinux()) {
            result = EfaZIPUtil.unzipToResult(zipFile, Daten.efaMainDirectory, ".jar", ".jar.new");
        } else {
            result = EfaZIPUtil.unzipToResult(zipFile, Daten.efaMainDirectory);
        }

        if (result != null) {
            if (result.getStatus() == EfaZIPUtil.UnzipStatus.FAILURE) {
                String errorText = result.getErrorsAsString();
                if (errorText.length() > 1000) {
                    errorText = errorText.substring(0, 1000);
                }
                setError(LogString.operationFailed(International.getString("Installation")) +
                        "\n" + errorText);
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
	 * Called when the download fails. Re-enables the parent window (if present), records the error
	 * and shows an error message (only if a parent window exists).
	 * */
    public void failure(String text) {
        String err = LogString.operationFailed(International.getString("Installation"));
        if (text != null && text.length() > 0) {
            err += "\n" + text;
        }
        OnlineUpdate.setLastError(err);
        if (parent != null) {
            parent.setEnabled(true);
            Dialog.error(err);
        } else {
            Logger.log(Logger.ERROR, Logger.MSG_EVT_REMOTEONLINEUPDATEDOWNLOAD, err);
        }
    }

    

}