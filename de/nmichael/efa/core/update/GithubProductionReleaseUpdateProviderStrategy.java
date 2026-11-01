package de.nmichael.efa.core.update;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.International;

/**
 * Loads releases from GitHub API and maps them to UpdateCandidate.
 * 
 * Production releases only, i.e. no pre-releases or drafts. 
 * Pre-releases and drafts are only loaded by GithubBetaReleaseUpdateProviderStrategy.
 * 
 * Expected API endpoint: https://api.github.com/repos/{owner}/{repo}/releases
 */
public class GithubProductionReleaseUpdateProviderStrategy extends GithubReleaseUpdateProviderStrategy {

    public static final String GITHUBPROD = "GitHubProd";

    // Raw (untranslated) texts, extracted as constants so that they are defined
    // exactly once and can be reused both by getDisplayName()/getUserHint() and
    // by the UpdateProviderStrategyRegistry self-registration below.
    private static final String DISPLAY_NAME = "GitHub Produktion Releases (Standard)";
    private static final String USER_HINT = 
    				 "Diese Update-Quelle lädt ausschließlich Produktions-Releases von GitHub."
    				+"\n\nVerwenden Sie diese Quelle, um Ihre Installation"
    				+" auf eine neuere PRODUKTIONS-Version zu aktualisieren.";

    // Self-registration: makes this strategy's metadata available via
    // UpdateProviderStrategyRegistry without requiring an instance to be created.
    static {
        UpdateProviderStrategyRegistry.register(new UpdateProviderStrategyDescriptor(
                GITHUBPROD,
                GithubProductionReleaseUpdateProviderStrategy.class,
                () -> International.getString(DISPLAY_NAME),
                () -> International.getString(USER_HINT),
                // this strategy needs no configuration, so the config argument is ignored
                config -> new GithubProductionReleaseUpdateProviderStrategy()));
    }

    public GithubProductionReleaseUpdateProviderStrategy() {
    	super(Daten.ONLINEUPDATE_INFO_GITHUB_OWNER,
    			Daten.ONLINEUPDATE_INFO_GITHUB_REPOSITORY,
				// no pre-releases. Github can mark a release as pre-realese.
				// we rely on that, and don't check the version number for a "beta" or "alpha" string.
    			false, // no pre-releases
    			false, // no drafts
    			true, // only production releases
    			null, 15000, 30000);
    }
    
    public String getName() {
        return GITHUBPROD;
    }
    
    public String getDisplayName() {
		return International.getString(DISPLAY_NAME);
    }
    
    public String getUserHint() {
		return International.getString(USER_HINT);
	}
}