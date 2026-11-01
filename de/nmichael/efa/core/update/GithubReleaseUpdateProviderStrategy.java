package de.nmichael.efa.core.update;

import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.util.DownloadThread;
import de.nmichael.efa.util.Logger;

/**
 * Abstract base class for fetching and parsing GitHub releases into UpdateCandidate objects.
 *
 * At the current time, efa provides two concrete implementations: 
 * * GithubProductionReleaseUpdateProviderStrategy 
 *   only fetches production releases (non-draft, non-prerelease) and is used for standard update checks.
 *   
 * * GithubBetaReleaseUpdateProviderStrategy
 *   only fetches beta releases (prerelease and draft) and is used for beta update checks.
 * 
 * Loads releases from GitHub API and maps them to UpdateCandidate.
 * Expected API endpoint: https://api.github.com/repos/{owner}/{repo}/releases
 */
public abstract class GithubReleaseUpdateProviderStrategy implements UpdateProviderStrategy {

    private final String owner;
    private final String repo;
    private final boolean includePrereleases;
    private final boolean includeDrafts;
    private final boolean includeProductionReleases;
    private final String assetNameMustContain; // optional, e.g. "efa"
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    public static final String GITHUB = "GitHub";

    public GithubReleaseUpdateProviderStrategy(String owner, String repo,
            boolean includePrereleases, boolean includeDrafts, boolean includeProductionReleases,
            String assetNameMustContain, int connectTimeoutMs, int readTimeoutMs) {
        this.owner = owner;
        this.repo = repo;
        this.includePrereleases = includePrereleases;
        this.includeDrafts = includeDrafts;
        this.includeProductionReleases = includeProductionReleases;
        this.assetNameMustContain = assetNameMustContain;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public String getName() {
        return GITHUB;
    }
    
    public String getSource() {
		return getApiUrl();
	}

    /**
	 * Fetches available releases from GitHub API and maps them to UpdateCandidate.
	 * Filters out drafts and pre-releases based on configuration.
	 * Sorts the result by version descending (newest first).
	 */
    public List<UpdateCandidate> fetchAvailableUpdates() throws Exception {
        JSONArray releases = loadAllReleases();

        List<UpdateCandidate> result = new ArrayList<UpdateCandidate>();
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.getJSONObject(i);

            boolean isDraft = release.optBoolean("draft", false);
            boolean isPrerelease = release.optBoolean("prerelease", false);
            boolean isProductionRelease = !isDraft && !isPrerelease;
            
            if (!includeDrafts && isDraft) {
                continue;
            }
            if (!includePrereleases && isPrerelease) {
                continue;
            }
			if (!includeProductionReleases && isProductionRelease) {
				continue;
			}
			
            String tag = normalizeTag(release.optString("tag_name", ""));
            String versionId = tag;
            if (versionId.length() == 0) {
                continue;
            }
            // on github the tag name contains -beta. instead of a "#" in the version string, 
            // so we replace it here to match the versioning scheme used in EFA.

            versionId = versionId.replace("-beta.", "_#");
            
            JSONObject asset = findZipAsset(release.optJSONArray("assets"));
            if (asset == null) {
                // Skip releases without downloadable zip asset.
                continue;
            }
            
            JSONObject eouXmlAsset = findEouXMLAsset(release.optJSONArray("assets"));
            if (eouXmlAsset == null) {
                // Skip releases without downloadable eou.xml asset.
                continue;
            }
            String eouXmlDownloadUrl = eouXmlAsset != null
                    ? eouXmlAsset.optString("browser_download_url", "")
                    : "";


            String downloadUrl = asset.optString("browser_download_url", "");
            long downloadSize = asset.optLong("size", 0L);
            String publishedAt = release.optString("published_at", "");
            String releaseDate = simplifyIsoDate(publishedAt);
            List<String> changes = bodyToChanges(release.optString("body", ""));

            result.add(new UpdateCandidate(
                    versionId,
                    releaseDate,
                    downloadUrl,
                    downloadSize,
                    changes,
                    getName(),
                    isDraft,
                    isPrerelease,
                    eouXmlDownloadUrl, 
                    tag));
        }

        // Ensure newest first even if API order changes.
        Collections.sort(result, new Comparator<UpdateCandidate>() {
            @Override
            public int compare(UpdateCandidate a, UpdateCandidate b) {
                // descending (newest first)
                return VersionIdComparator.compare(a.getVersionId(), b.getVersionId());
            }
        });

        return result;
    }

    /**
     * Loads the first 100 releases (newest first) from the GitHub API in a single request.
     * The assumtion is that performing an online update, the user is interested in the latest releases.
     * and 100 releases should be sufficient to cover all recent updates.
     * 
     * There is the edge case that we are looking for a production release update, 
     * but it is not found in the first 100 releases (which must all  be draft or prereleases to get to
     * this edge case). This is a very unlikely edge case, but if it happens, the user will not be able to update to the latest production release.
     * 
     * On the other hand, if we would load all releases, it could be a very large number of releases, 
     * and the request could take a long time to complete.
     * 
     * The request is made to the GitHub API endpoint: https://api.github.com/repos/{owner}/{repo}/releases?per_page=60&page=1
     * And this class requests gzip compression for the response, which is supported by the GitHub API,
     * and should dramatically reduce the amount of data transferred, especially for large release notes.
     * 
     * So also on slow connections, this should be a reasonable approach to get the latest releases.
     * 
     * @return JSONArray of release objects.
     * @throws Exception if the request or parsing fails.
     */
    private JSONArray loadAllReleases() throws Exception {
        String pageUrl = getApiUrl() + "?per_page=100&page=1";
        return new JSONArray(loadReleasesJson(pageUrl));
    }

    /**
	 * Loads the releases JSON from GitHub API.
	 * @return JSON string of releases.
	 * @throws Exception if an error occurs during HTTP request or reading response.
	 */
    private String loadReleasesJson(String urlString) throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL(urlString);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(connectTimeoutMs);
            con.setReadTimeout(readTimeoutMs);
            con.setUseCaches(false);
            con.setRequestProperty("Accept", "application/vnd.github+json");
            con.setRequestProperty("Accept-Encoding", "gzip");
            con.setRequestProperty("User-Agent", buildUserAgent());

            int code = con.getResponseCode();
            if (code < 200 || code >= 300) {
                String errText = "";
                InputStream err = decodeIfGzip(con, con.getErrorStream());
                if (err != null) {
                    try (InputStream errIn = new BufferedInputStream(err)) {
                        errText = readFully(errIn);
                    }
                }
                throw new IllegalStateException(
                        "GitHub API HTTP " + code + " for " + urlString + "\n" + errText);
            }

            boolean gzip = "gzip".equalsIgnoreCase(con.getContentEncoding());
            byte[] rawBytes;
            try (InputStream in = new BufferedInputStream(con.getInputStream())) {
                rawBytes = readBytes(in);
            }

            byte[] bodyBytes = rawBytes;
            if (gzip) {
                try (InputStream gz = new GZIPInputStream(new ByteArrayInputStream(rawBytes))) {
                    bodyBytes = readBytes(gz);
                }
            }

            Logger.log(Logger.DEBUG, Logger.MSG_EVT_ONLINEUPDATEINFO,
                    "GitHub API response from " + urlString + ": gzip=" + gzip
                    + ", compressed length=" + rawBytes.length + " bytes"
                    + ", decompressed length=" + bodyBytes.length + " bytes");

            return new String(bodyBytes, "UTF-8");
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    /**
	 * Finds the first zip asset in the given assets array that matches the optional name filter.
	 * @param assets JSONArray of asset objects from GitHub release.
	 * @return JSONObject of the matching zip asset, or null if none found.
	 */
    private JSONObject findZipAsset(JSONArray assets) {
        if (assets == null) {
            return null;
        }

        JSONObject fallbackZip = null;
        String needle = assetNameMustContain == null ? "" : assetNameMustContain.toLowerCase(Locale.ROOT);

        for (int j = 0; j < assets.length(); j++) {
            JSONObject asset = assets.optJSONObject(j);
            if (asset == null) {
                continue;
            }

            String name = asset.optString("name", "");
            String url = asset.optString("browser_download_url", "");

            if (!isZip(name, url)) {
                continue;
            }

            if (fallbackZip == null) {
                fallbackZip = asset;
            }

            String lowered = name.toLowerCase(Locale.ROOT);
            if (needle.length() == 0 || lowered.contains(needle)) {
                // First best matching zip.
                return asset;
            }
        }

        return fallbackZip;
    }

    /**
	 * Finds the first eou.xml asset in the given assets array that matches the optional name filter.
	 * @param assets JSONArray of asset objects from GitHub release.
	 * @return JSONObject of the matching zip asset, or null if none found.
	 */
    private JSONObject findEouXMLAsset(JSONArray assets) {
        if (assets == null) {
            return null;
        }

        for (int j = 0; j < assets.length(); j++) {
            JSONObject asset = assets.optJSONObject(j);
            if (asset == null) {
                continue;
            }

            String name = asset.optString("name", "").toLowerCase(Locale.ROOT).trim();
            if ("eou.xml".equals(name)) {
                return asset;
            }
        }

        return null;
    }

    
    /**
	 * Checks if the given name or URL indicates a zip file.
	 * @param name Asset name.
	 * @param url Asset download URL.
	 * @return true if either name ends with .zip or URL contains .zip, false otherwise.
	 * */
    private boolean isZip(String name, String url) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String u = url == null ? "" : url.toLowerCase(Locale.ROOT);
        return n.endsWith(".zip") || u.contains(".zip");
    }
    

    /**
	 * Converts the release body text into a list of change entries.
	 * Removes common markdown bullets and checkboxes.
	 * @param body Release body text.
	 * @return List of change entries.
	 * */
    private List<String> bodyToChanges(String body) {
        List<String> changes = new ArrayList<String>();
        if (body == null) {
            return changes;
        }

        String[] lines = body.replace("\r", "").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            if (raw.length() == 0) {
                continue;
            }

            // Remove common markdown bullets and checkboxes.
            String s = raw;
            if (s.startsWith("- [ ] ")) {
                s = s.substring(6).trim();
            } else if (s.startsWith("- [x] ") || s.startsWith("- [X] ")) {
                s = s.substring(6).trim();
            } else if (s.startsWith("- ") || s.startsWith("* ") || s.startsWith("+ ")) {
                s = s.substring(2).trim();
            } else if (s.matches("^\\d+\\.\\s+.*")) {
                int p = s.indexOf('.');
                s = s.substring(p + 1).trim();
            }

            if (s.length() == 0) {
                continue;
            }
            changes.add(s);
        }

        return changes;
    }

    /**
	 * Normalizes a GitHub tag name to a version string.
	 * Removes "refs/tags/" prefix and leading "v" or "V".
	 * @param tag GitHub tag name.
	 * @return Normalized version string.
	 * */
    private String normalizeTag(String tag) {
        if (tag == null) {
            return "";
        }
        String t = tag.trim();
        if (t.startsWith("refs/tags/")) {
            t = t.substring("refs/tags/".length());
        }
        if (t.startsWith("v") || t.startsWith("V")) {
            t = t.substring(1);
        }
        return t.trim();
    }

    /**
	 * Simplifies an ISO 8601 date string to "YYYY-MM-DD" format.
	 * If the input is null or not in expected format, returns empty string or original string.
	 * @param iso ISO 8601 date string.
	 * @return Simplified date string.
	 * */
    private String simplifyIsoDate(String iso) {
        if (iso == null) {
            return "";
        }
        // Example: 2026-09-20T12:34:56Z => 2026-09-20
        if (iso.length() >= 10 && iso.charAt(4) == '-' && iso.charAt(7) == '-') {
            return iso.substring(0, 10);
        }
        return iso;
    }

    /**
	 * Builds a User-Agent string for GitHub API requests.
	 * @return User-Agent string in the format "efa/{version} ({owner}/{repo})".
	 * */
    private String buildUserAgent() {
        String version = Daten.VERSIONID != null ? Daten.VERSIONID : "unknown";
        return "efa/" + version + " (" + owner + "/" + repo + ")";
    }

    /**
	 * Reads the entire InputStream into a byte array.
	 * @param in InputStream to read.
	 * @return raw bytes of the InputStream.
	 * @throws IOException if an error occurs during reading.
	 * */
    private byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            if (n > 0) {
                out.write(buf, 0, n);
            }
        }
        return out.toByteArray();
    }

    /**
	 * Reads the entire InputStream into a UTF-8 string.
	 * @param in InputStream to read.
	 * @return String content of the InputStream.
	 * @throws Exception if an error occurs during reading.
	 * */
    private String readFully(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            if (n > 0) {
                out.write(buf, 0, n);
            }
        }
        return new String(out.toByteArray(), "UTF-8");
    }

    /**
     * Wraps the stream in a GZIPInputStream if the server responded with gzip content encoding.
     * @param con The HTTP connection.
     * @param raw The raw response stream (may be null).
     * @return The decoded stream, or raw if no decoding is needed.
     * @throws IOException if the gzip header cannot be read.
     */
    private InputStream decodeIfGzip(HttpURLConnection con, InputStream raw) throws IOException {
        if (raw == null) {
            return null;
        }
        if ("gzip".equalsIgnoreCase(con.getContentEncoding())) {
            return new GZIPInputStream(raw);
        }
        return raw;
    }
    /**
	 * Constructs the GitHub API URL for releases based on owner and repo.
	 * @return API URL string.
	 * */
    public String getApiUrl() {
        return "https://api.github.com/repos/" + owner + "/" + repo + "/releases";
    }
    
    /**
	 * Fetches the newest EOU file from the provider.
	 * This method is used to obtain the latest EOU file, which contains information about available updates.
	 * @param parent The parent window for any dialogs that may be displayed during the download process.
	 * @param newerCandidates A sorted list (newest first) of update candidates that are newer than the current version.
	 * @return The newest EOU file or null if the download fails.
	 * @throws Exception
	 */
    public File fetchNewestEOUFile(Window parent, List<UpdateCandidate> newerCandidates) throws Exception {
        if (newerCandidates == null || newerCandidates.isEmpty()) {
            Logger.log(Logger.INFO, Logger.MSG_EVT_ONLINEUPDATEINFO, "No newer candidates found.");
            return null;
        }
        
		EfaGuiUtils.setWaitCursor(parent, true);
		try {
	        UpdateCandidate newestCandidate = newerCandidates.get(0);
	        String eouUrl = newestCandidate.getEouURL();
	        if (eouUrl == null || eouUrl.isEmpty()) {
	            Logger.log(Logger.INFO, Logger.MSG_EVT_ONLINEUPDATEINFO,
	                    "No EOU download URL available for " + newestCandidate.getVersionId());
	            return null;
	        }
	
	        String versionFile = Daten.efaTmpDirectory + "eou.xml";
	        if (!DownloadThread.getFile(parent, eouUrl, versionFile, true)) {
	            Logger.log(Logger.INFO, Logger.MSG_EVT_ONLINEUPDATEINFO, "Download of EOU file failed: " + eouUrl);
	            return null;
	        }
	        Logger.log(Logger.INFO, Logger.MSG_EVT_ONLINEUPDATEINFO, "Downloaded EOU file from: " + eouUrl);
	        return new File(versionFile);
		} finally {
			EfaGuiUtils.setWaitCursor(parent, false);
		}
    }

    @Override
    public boolean isEOUFileTemporary() {
        return true; // the EOU file is always a temporary file that should be deleted after parsing
    }
}
