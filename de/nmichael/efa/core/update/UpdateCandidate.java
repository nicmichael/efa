package de.nmichael.efa.core.update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a candidate for an update, containing information about the version,
 * release date, download URL, size, changes, source name, and whether it is a draft.
 */
public class UpdateCandidate {
    private final String versionId;
    private final String releaseDate;
    private final String downloadUrl;
    private final long downloadSize;
    private final List<String> changes;
    private final String sourceName;
    private final boolean isDraft;
    private final String eouURL;
    private final String tag; // GitHub Tag ID
    
    public UpdateCandidate(String versionId, String releaseDate, String downloadUrl,
            long downloadSize, List<String> changes, String sourceName, boolean isDraft, String eouURL, String tag) {
        this.versionId = versionId;
        this.releaseDate = releaseDate;
        this.downloadUrl = downloadUrl;
        this.downloadSize = downloadSize;
        this.changes = changes == null ? new ArrayList<String>() : new ArrayList<String>(changes);
        this.sourceName = sourceName;
        this.isDraft = isDraft;
        this.eouURL = eouURL;
        this.tag = tag;
    }

    public String getVersionId() { return versionId; }
    public String getReleaseDate() { return releaseDate; }
    public String getDownloadUrl() { return downloadUrl; }
    public long getDownloadSize() { return downloadSize; }
    public List<String> getChanges() { return Collections.unmodifiableList(changes); }
    public String getSourceName() { return sourceName; }
    public boolean isDraft() { return isDraft; }
    public String getEouURL() { return eouURL; }
    public String getTag() { return tag; }
    @Override
    public String toString() {
        return versionId + " (" + sourceName + ")";
    }
}
