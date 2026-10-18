package de.nmichael.efa.core.update;

import java.awt.Window;
import java.io.File;
import java.util.List;
import java.util.Vector;
import javax.swing.JDialog;

import org.xml.sax.XMLReader;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.OnlineUpdateDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

/**
 * An implementation of UpdateSelectionStrategy that presents a dialog to the user 
 * for selecting an update candidate.
 */
public class DialogSelectionStrategy implements UpdateSelectionStrategy {

    public UpdateCandidate select(JDialog parent, UpdateProviderStrategy provider, 
    		List<UpdateCandidate> newerCandidates, String currentVersion) {
        if (newerCandidates == null || newerCandidates.isEmpty()) {
            return null;
        }
        Vector<OnlineUpdateInfo> eouVersionInfo = null;
        // Get the newest version information from the EOU file so that we can display the changes and download size to the user.
        // we always use the newest candidate to get the EOU file, because it contains all newer versions and their changes.
        eouVersionInfo = getEOUVersionInfo(parent, provider, newerCandidates);

        // Ok, es gibt eine neue Version --> Infos über diese Version einlesen
        Vector<String> changes = new Vector<String>();
        for (int i=0; i<eouVersionInfo.size(); i++) {
            OnlineUpdateInfo version = eouVersionInfo.get(i);
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

        UpdateCandidate newestCandidate=(newerCandidates!=null && newerCandidates.size()>0 ? newerCandidates.get(0) : null);
        
        if (newestCandidate != null) {
	        // Ok, Informationen gelesen: Jetzt auf dem Bildschirm anzeigen
	        if (parent != null) {
	            OnlineUpdateDialog dlg = new OnlineUpdateDialog(parent,
	                    newestCandidate.getVersionId(), newestCandidate.getReleaseDate(),
	                    newestCandidate.getDownloadSize(), changes);
	            dlg.showDialog();
	            if (!dlg.getDialogResult()) {
	                return null;
	            }
	        } else {
	        	return newestCandidate;
	        }
        } 
        
        return null;
        
    }
    
    
    /**
	 * Fetches and parses the EOU version information from the specified provider.
	 * @param parent The parent dialog for displaying messages. Can be null if running in headless mode.
	 * @param provider The strategy for fetching available updates.
	 * @param newerCandidates The list of update candidates that are newer than the current version.
	 * 
	 * @return A vector of OnlineUpdateInfo objects containing the parsed version information, 
	 * or null if an error occurred.
	 */
	private static Vector<OnlineUpdateInfo> getEOUVersionInfo(JDialog parent, UpdateProviderStrategy provider, List<UpdateCandidate> newerCandidates) {
		
		Vector<OnlineUpdateInfo> eouVersionInfo=null;
		
		// aktuelle Versionsnummer aus dem Internet besorgen
        File versionFile = null;
        String lastError = "";
        try {
			versionFile = provider.fetchNewestEOUFile(parent, newerCandidates);
		} catch (Exception e) {
			lastError = International.getString("Fehler beim Abrufen der Update-Informationen!")
					+ "\n" + e.getMessage();
			if (parent != null) {
				Dialog.error(lastError);
			}
			return null;
		}

        try {
        	eouVersionInfo = parseEOUFile(versionFile, parent);
        } catch (Exception e) {
			lastError = International.getString("Fehler beim Einlesen der Update-Informationen!")
					+ "\n" + e.getMessage();
			if (parent != null) {
				Dialog.error(lastError);
			}
			return null;
        } finally {
        	if (provider.isEOUFileTemporary() ) {
        		EfaUtil.deleteFile(versionFile.getAbsolutePath());
        	}
		}
        
		return eouVersionInfo;
	}

    private static Vector<OnlineUpdateInfo> parseEOUFile(File eouFile, Window parent) throws Exception {
		if (eouFile == null || !eouFile.exists()) {
			throw new Exception(International.getString("Die Datei mit den Update-Informationen konnte nicht gefunden werden."));
		}

		Vector<OnlineUpdateInfo> eouVersionInfo = null;
		String lastError="";
		
        try {
            XMLReader parser = EfaUtil.getXMLReader();
            OnlineUpdateFileParser ou = new OnlineUpdateFileParser();
            parser.setContentHandler(ou);
            parser.parse(eouFile.getAbsolutePath());
            eouVersionInfo = ou.getVersions();
        } catch (Exception ee) {
            lastError = International.getString("Keine neue Version gefunden!")
                        + "\n" + ee.getMessage();
            if (parent != null) {
                Dialog.error(lastError);
            }
            EfaUtil.deleteFile(eouFile.getAbsolutePath());
            return null;
        }
        if (eouVersionInfo == null || eouVersionInfo.size() == 0) {
            lastError = International.getString("Keine neue Version gefunden!");
            if (parent != null) {
                Dialog.error(lastError);
            }
            EfaUtil.deleteFile(eouFile.getAbsolutePath());
            return null;
        }

		return eouVersionInfo;
	}
}
