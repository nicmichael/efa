package de.nmichael.efa.core.update;

import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.DownloadThread;
import de.nmichael.efa.util.Logger;

/**
 * Loads releases from GitHub API and maps them to UpdateCandidate.
 * Expected API endpoint: https://api.github.com/repos/{owner}/{repo}/releases
 */
public class GithubReleaseUpdateProviderStrategy implements UpdateProviderStrategy {

    private final String owner;
    private final String repo;
    private final boolean includePrereleases;
    private final boolean includeDrafts;
    private final String assetNameMustContain; // optional, e.g. "efa"
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    public static final String GITHUB = "GitHub";

    public GithubReleaseUpdateProviderStrategy(String owner, String repo) {
        this(owner, repo, false, false, "efa", 15000, 30000);
    }
    
    public GithubReleaseUpdateProviderStrategy(String owner, String repo, boolean includePrereleases) {
        this(owner, repo, includePrereleases, false, "efa", 15000, 30000);
    }

    public GithubReleaseUpdateProviderStrategy(String owner, String repo,
            boolean includePrereleases, boolean includeDrafts,
            String assetNameMustContain, int connectTimeoutMs, int readTimeoutMs) {
        this.owner = owner;
        this.repo = repo;
        this.includePrereleases = includePrereleases;
        this.includeDrafts = includeDrafts;
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
        String json = loadReleasesJson();
        Logger.log(Logger.DEBUG, Logger.MSG_STAT_IGNOREDENTRIES, json);
        JSONArray releases = new JSONArray(json);

        List<UpdateCandidate> result = new ArrayList<UpdateCandidate>();
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.getJSONObject(i);

            boolean draft = release.optBoolean("draft", false);
            boolean prerelease = release.optBoolean("prerelease", false);

            if (!includeDrafts && draft) {
                continue;
            }
            if (!includePrereleases && prerelease) {
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
                    draft,
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
	 * Loads the releases JSON from GitHub API.
	 * @return JSON string of releases.
	 * @throws Exception if an error occurs during HTTP request or reading response.
	 */
    private String loadReleasesJson() throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL(getApiUrl());
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(connectTimeoutMs);
            con.setReadTimeout(readTimeoutMs);
            con.setUseCaches(false);
            con.setRequestProperty("Accept", "application/vnd.github+json");
            con.setRequestProperty("User-Agent", buildUserAgent());

            int code = con.getResponseCode();
            if (code < 200 || code >= 300) {
                String errText = "";
                InputStream err = con.getErrorStream();
                if (err != null) {
                    try (InputStream errIn = new BufferedInputStream(err)) {
                        errText = readFully(errIn);
                    }
                }
                throw new IllegalStateException(
                        "GitHub API HTTP " + code + " for " + getApiUrl() + "\n" + errText);
            }

            try (InputStream in = new BufferedInputStream(con.getInputStream())) {
                return readFully(in);
            }
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
	 * Finds the first zip asset in the given assets array that matches the optional name filter.
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
	 * Checks if the given name or URL indicates a zip file.
	 * @param name Asset name.
	 * @param url Asset download URL.
	 * @return true if either name ends with .zip or URL contains .zip, false otherwise.
	 * */
    private boolean isXML(String name, String url) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String u = url == null ? "" : url.toLowerCase(Locale.ROOT);
        return n.endsWith(".xml") || u.contains(".xml");
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
    public File fetchNewestEOUFile(Window parent, List<UpdateCandidate> newerCandidates) throws Exception{
    	UpdateCandidate newestCandidate = null;
    	newestCandidate = (newestCandidate != null) ? newestCandidate : newerCandidates.get(0);

    	if (newestCandidate != null && newestCandidate.getEouURL() != null && !newestCandidate.getEouURL().isEmpty()) {
			String versionFile = Daten.efaTmpDirectory + "eou.xml";
			if (!DownloadThread.getFile(parent, newestCandidate.getEouURL(), versionFile, true)) {
				// No candidate with a valid EOU download URL found.
				Logger.log(Logger.INFO, Logger.MSG_STAT_IGNOREDENTRIES, "No EOU download URL found in newer candidates.");
				return null;
			} else {
				Logger.log(Logger.INFO, Logger.MSG_STAT_IGNOREDENTRIES, "Downloaded EOU file from: " + newestCandidate.getEouURL());
				return new File(versionFile);
			}
    	} else {
    		Logger.log(Logger.INFO, Logger.MSG_STAT_IGNOREDENTRIES, "No newer candidates found or no EOU download URL available.");
			return null;
    	}
    }
    
   @Override
   public boolean isEOUFileTemporary() {
    	return true; // the EOU file is always a temporary file that should be deleted after parsing
   };}
