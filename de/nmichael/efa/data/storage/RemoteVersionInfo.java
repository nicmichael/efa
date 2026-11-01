/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.storage;

/**
 * Plain data object holding the version information of a remote efa instance,
 * together with the result code of the remote request that produced it.
 */
public class RemoteVersionInfo {

    private final String version;
    private final String versionId;
    private final String versionReleaseDate;
    private final int resultCode;

    /**
     * @param version            efa version (Daten.VERSION) of the remote instance, may be null
     * @param versionId          efa version id (Daten.VERSIONID) of the remote instance, may be null
     * @param versionReleaseDate release date (Daten.VERSIONRELEASEDATE) of the remote instance, may be null
     * @param resultCode         result code of the remote request (RemoteEfaMessage.RESULT_OK on success,
     *                           -1 if no valid response was received)
     */
    public RemoteVersionInfo(String version, String versionId, String versionReleaseDate, int resultCode) {
        this.version = version;
        this.versionId = versionId;
        this.versionReleaseDate = versionReleaseDate;
        this.resultCode = resultCode;
    }

    public String getVersion() {
        return version;
    }

    public String getVersionId() {
        return versionId;
    }

    public String getVersionReleaseDate() {
        return versionReleaseDate;
    }

    /**
     * @return the result code of the remote request
     */
    public int getResultCode() {
        return resultCode;
    }
}
