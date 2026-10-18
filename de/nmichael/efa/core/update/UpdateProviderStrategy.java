package de.nmichael.efa.core.update;

import java.awt.Window;
import java.io.File;
import java.util.List;

/**
 * Interface for update provider strategies that fetch available updates.
 * By this interface, efa can adopt multiple sources for online updates,
 * without changing the basic pattern of obtaining and applying an update.
 * 
 * Efa provides three UpdateProviders
 * - efaNMichaelDE 
 *   classic update where eou.xml is obtained from efa.nmichael.de, 
 *   and the efa zips are downloaded from a location specified in the eou.xml
 *   
 * - GitHubRelease
 *   Get the eou.xml and the zip updates from Github
 *   https://api.github.com/repos/{owner}/{repo}/releases
 *   
 * - LocalFile
 *   The idea is that you update a station using an usb stick,  
 *   which has at least a current eou.xml and at least an efa*.zip file.
 *   
 *   So you can use the standard update functionality of efa,
 *   without having to use an online connection. This may help with
 *   remote/offline efaLive stations where a manual update using unix system commands
 *   is not too easy to perform.
 * 
 */
public interface UpdateProviderStrategy {
    /**
	 * Returns the name of the update provider.
	 * @return Name of the update provider.
	 */
	String getName();
	
    /**
	 * Returns the source of the update provider.
	 * @return Source of the update provider.
	 */
	String getSource();

	/**
	 * Fetches available updates from the provider.
	 * @return List of available updates, sorted by newest first.
	 * @throws Exception if an error occurs while fetching updates.
	 */
    List<UpdateCandidate> fetchAvailableUpdates() throws Exception; // newest first
    
    /**
     * Fetches the newest EOU file from the provider.
	 * This method is used to obtain the latest EOU file, which contains information about available updates.
     * @return The newest EOU file.
     * @throws Exception
     */
    File fetchNewestEOUFile(Window parent, List<UpdateCandidate> newerCandidates) throws Exception;
    
    /** 
	 * Checks if the EOU file is temporary.
	 * This is the case when it is NOT downloaded from a local file, but from an online source.
	 */
    boolean isEOUFileTemporary();
}
