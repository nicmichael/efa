package de.nmichael.efa.core.update;

import java.util.List;
import javax.swing.JDialog;

/**
 * Interface for strategies that select an update candidate from a list of newer candidates.
 */
public interface UpdateSelectionStrategy {
    // return null => aborted by user
	/**
	 * Selects an update candidate from the list of newer candidates.
	 * @param parent The parent dialog for any UI components.
	 * @param newerCandidates List of newer update candidates.
	 * @param currentVersion The current version of the application.
	 * @return The selected update candidate, or null if the selection was aborted by the user.
	 * */
    UpdateCandidate select(JDialog parent, UpdateProviderStrategy provider, 
    		List<UpdateCandidate> newerCandidates, String currentVersion);
}