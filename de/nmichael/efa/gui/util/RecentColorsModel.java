package de.nmichael.efa.gui.util;

import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple observable list of recently selected colors. The most recently used
 * color is always kept at the front of the list, duplicates are removed, and
 * the list size is capped at {@link #maxSize}.
 */
public class RecentColorsModel {
	final int maxSize;
	private final LinkedList<Color> colors = new LinkedList<>();
	private final List<WeakReference<Runnable>> listeners = new CopyOnWriteArrayList<>();

	/**
	 * Creates a new model.
	 *
	 * @param maxSize maximum number of colors kept in the history
	 */
	public RecentColorsModel(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * Adds a color to the front of the history, removing any previous occurrence of
	 * the same color and trimming the list to {@link #maxSize}.
	 *
	 * @param c the color to remember
	 */
	void addColor(Color c) {
		colors.remove(c);
		colors.addFirst(c);
		while (colors.size() > maxSize) {
			colors.removeLast();
		}
		fireChanged();
	}

	/**
	 * @return the current list of recently used colors, most recent first
	 */
	List<Color> getColors() {
		return Collections.unmodifiableList(colors);
	}

	/**
	 * Registers a listener that is notified whenever the history changes. The
	 * model only holds a weak reference, so panels of closed dialogs can be
	 * garbage collected even if they were never uninstalled.
	 *
	 * @param r callback to invoke on change
	 */
	void addListener(Runnable r) {
		removeListener(r);
		listeners.add(new WeakReference<>(r));
	}

	/**
	 * Removes a previously registered listener.
	 *
	 * @param r the listener to remove
	 */
	void removeListener(Runnable r) {
		for (WeakReference<Runnable> ref : listeners) {
			Runnable current = ref.get();
			if (current == null || current == r) {
				listeners.remove(ref);
			}
		}
	}

	/**
	 * Notifies all registered listeners about a change in the history and drops
	 * references to listeners that have been garbage collected.
	 */
	private void fireChanged() {
		for (WeakReference<Runnable> ref : listeners) {
			Runnable r = ref.get();
			if (r == null) {
				listeners.remove(ref);
			} else {
				r.run();
			}
		}
	}
}