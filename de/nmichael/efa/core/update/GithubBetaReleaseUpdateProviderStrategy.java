package de.nmichael.efa.core.update;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.International;

/**
 * Loads releases from GitHub API and maps them to UpdateCandidate.
 * 
 * Beta releases only, i.e. pre-releases.
 * 
 * Drafts are not loaded, as they are not intended for public beta testing.
 * Drafts are only loaded to by a maintainer of the repository, to test the release 
 * before publishing it (either pre-release or production release).
 * 
 * Production releases are only loaded by GithubProductionReleaseUpdateProviderStrategy.
 * 
 * Expected API endpoint: https://api.github.com/repos/{owner}/{repo}/releases
 */

public class GithubBetaReleaseUpdateProviderStrategy extends GithubReleaseUpdateProviderStrategy {

    public static final String GITHUBBETA = "GitHubBeta";

    // Raw (untranslated) texts, extracted as constants so that they are defined
    // exactly once and can be reused both by getDisplayName()/getUserHint() and
    // by the UpdateProviderStrategyRegistry self-registration below.
    private static final String DISPLAY_NAME = "GitHub Beta Releases";
    private static final String USER_HINT = 
    				"Diese Update-Quelle lädt ausschließlich Beta/Pre-Releases von GitHub."
    				+"\n\nVerwenden Sie diese Quelle, um Ihre Installation"+
    				" auf eine neuere BETA-Version zu aktualisieren.";

    // Self-registration: makes this strategy's metadata available via
    // UpdateProviderStrategyRegistry without requiring an instance to be created.
    static {
        UpdateProviderStrategyRegistry.register(new UpdateProviderStrategyDescriptor(
                GITHUBBETA,
                GithubBetaReleaseUpdateProviderStrategy.class,
                () -> International.getString(DISPLAY_NAME),
                () -> International.getString(USER_HINT),
                // this strategy needs no configuration, so the config argument is ignored
                config -> new GithubBetaReleaseUpdateProviderStrategy()));
    }

    public GithubBetaReleaseUpdateProviderStrategy() {
		//just call super constructor with includePrereleases = true, includeDrafts = false
		super(Daten.ONLINEUPDATE_INFO_GITHUB_OWNER, 
				Daten.ONLINEUPDATE_INFO_GITHUB_REPOSITORY, 
				// only pre-releases. Github can mark a release as pre-realese.
				// we rely on that, and don't check the version number for a "beta" or "alpha" string.
    			true, 
    			false, // no drafts
    			false, // no production releases
    			null, 15000, 30000);
	}
    
    public String getName() {
        return GITHUBBETA;
    }
    
    public String getDisplayName() {
		return International.getString(DISPLAY_NAME);
	}
    
    public String getUserHint() {
    	return International.getString(USER_HINT);
    }
}
    
