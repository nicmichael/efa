package de.nmichael.efa.core.update;

import java.util.List;
import javax.swing.JDialog;

/**
 * An implementation of UpdateSelectionStrategy that automatically selects the newest update candidate.
 */
public class AutoNewestSelectionStrategy implements UpdateSelectionStrategy {
    public UpdateCandidate select(JDialog parent, UpdateProviderStrategy provider,
    		List<UpdateCandidate> newerCandidates, String currentVersion) {
        if (newerCandidates == null || newerCandidates.isEmpty()) {
            return null;
        }
        return newerCandidates.get(0);
    }
}
