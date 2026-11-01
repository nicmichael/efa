package de.nmichael.efa.core.update;

import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.xml.sax.XMLReader;

import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

/**
 * LocalFileUpdateProviderStrategy is an implementation of the
 * UpdateProviderStrategy interface that fetches update information from a local
 * XML file (eou.xml).
 *
 * The idea is that you update a station using an usb stick, which has at least
 * a current eou.xml and at least an efa*.zip file.
 * 
 * So you can use the standard update functionality of efa, without having to
 * use an online connection. This may help with remote/offline efaLive stations
 * where a manual update using unix system command is not too easy to perform
 * for unexperienced admins.
 *
 * This UpdateProvider Strategy gets the update information from a local XML
 * file and parses it to extract update information. Only versions with a
 * specified download URL and existing download file in the same directory are
 * included in the result. The download file path is constructed by appending
 * the download URL to the canonical path of the EOU file.
 * 
 * In the directory of the EOU file, there can be multiple efa zip files, each
 * corresponding to a different version. The download URL in the EOU file should
 * match the name of the corresponding efa zip file in the same directory.
 * 
 */
public class LocalFileUpdateProviderStrategy implements UpdateProviderStrategy {
	private final String eouFileName;
	public static final String LOCAL_FILE = "LocalFile";

	// Raw (untranslated) texts, extracted as constants so that they are defined
	// exactly once and can be reused both by getDisplayName()/getUserHint() and
	// by the UpdateProviderStrategyRegistry self-registration below.
	private static final String DISPLAY_NAME = "Lokale Datei vom USB-Stick (eou.xml)";
	private static final String USER_HINT = 
			"Zum Update von einem Verzeichnis (z.B. auf einem USB-Stick) müssen sich darin Download-Elemente von GitHub befinden: "
			+ "\n\n1) Die eou.xml des aktuellsten efa-Releases \n2) mindestens ein efa*.zip Programmpaket-Download ihrer Wahl. \nEs können sich auch mehrere efa*.zip Dateien darin befinden.";

	// Self-registration: makes this strategy's metadata available via
	// UpdateProviderStrategyRegistry without requiring an instance to be created
	// (which would not even be possible here without a valid eouFileName).
	static {
		UpdateProviderStrategyRegistry.register(new UpdateProviderStrategyDescriptor(
				LOCAL_FILE,
				LocalFileUpdateProviderStrategy.class,
				() -> International.getString(DISPLAY_NAME),
				() -> International.getString(USER_HINT),
				// config is the mandatory eou.xml file path for this strategy
				config -> new LocalFileUpdateProviderStrategy(config)));
	}

	public LocalFileUpdateProviderStrategy(String eouFileName) {
		this.eouFileName = eouFileName;
	}

	public String getSource() {
		return eouFileName;
	}
	
	public String getName() {
		return LOCAL_FILE;
	}

	public String getDisplayName() {
		return International.getString(DISPLAY_NAME);
	}

	public String getUserHint() {
		return International.getString(USER_HINT);
	}

	/**
	 * Fetches the available updates from the local EOU file.
	 * See class documentation for the general idea behind LocalFileUpdateProviderStrategy. 
	 * @return List of available updates, sorted by newest first.
	 * @throws Exception if an error occurs while fetching updates.
	 */
	public List<UpdateCandidate> fetchAvailableUpdates() throws Exception {
		File eouFile = new File(eouFileName);
		if (!eouFile.exists()) {
			return new ArrayList<UpdateCandidate>();
		}
		try {
			XMLReader parser = EfaUtil.getXMLReader();
			OnlineUpdateFileParser ou = new OnlineUpdateFileParser();
			parser.setContentHandler(ou);
			parser.parse(eouFileName);

			Vector<OnlineUpdateInfo> versions = ou.getVersions();
			List<UpdateCandidate> result = new ArrayList<UpdateCandidate>();
			for (OnlineUpdateInfo v : versions) {

				if (v.downloadUrl == null || v.downloadUrl.isEmpty()) {
					continue; // skip this update candidate if the download URL is not specified
				}
				// strip the efa.nmichael.de/download/ prefix from the download URL to get the
				// actual file name

				String downloadUrl = v.downloadUrl;
				downloadUrl = getLastPartOfUrl(downloadUrl);
				String downloadFilePath = eouFile.getAbsoluteFile().getParent() + File.separator + "" + downloadUrl;

				File downloadFile = new File(downloadFilePath);

				if (!downloadFile.exists()) {
					continue; // skip this update candidate if the download file does not exist
				}
				// file exists, add the update candidate to the result list
				result.add(new UpdateCandidate(v.versionId, v.releaseDate, "file://" + downloadFilePath, v.downloadSize,
						new ArrayList<String>(v.getChanges()), getName(), false, // local file source does not provide
																					// draft releases
						v.versionId.contains("#"), // but it provides beta releases, which can be determined by the
													// version ID containing a "#" character
						eouFileName, v.versionId));

			}
			return result;
		} finally {

		}
	}

	private String getLastPartOfUrl(String downloadUrl) {
		if (downloadUrl == null || downloadUrl.isEmpty()) {
			return "";
		}
		int lastSlashIndex = downloadUrl.lastIndexOf('/');
		if (lastSlashIndex == -1) {
			return downloadUrl; // no slash found, return the whole string
		}
		return downloadUrl.substring(lastSlashIndex + 1); // return the part after the last slash
	}

	public File fetchNewestEOUFile(Window parent, List<UpdateCandidate> newerCandidates) throws Exception {
		// In the case of LocalFileUpdateProviderStrategy, we already have the EOU file
		// locally.
		// So we can simply return the existing EOU file.
		File eouFile = new File(eouFileName);
		if (eouFile.exists()) {
			return eouFile;
		} else {
			throw new Exception("EOU file not found: " + eouFileName);
		}
	}

	@Override
	public boolean isEOUFileTemporary() {
		// in the LocalFileUpdateProviderStrategy, the EOU file is not temporary,
		// as it is expected to be a local file that persists on the filesystem.
		return false;
	};
}
