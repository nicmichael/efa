package de.nmichael.efa.gui.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * One titled group of the efa-Standardfarben tab. The standard colors (left
 * column) and the colors actually in use (right column) are stored without
 * duplicates; colors in use that are also standard colors are removed.
 */
public final class ColorGroup {

	public final String title;
	public final List<Color> standard;
	public final List<Color> inUse;

	/**
	 * @param title    the group title
	 * @param standard the standard colors (null entries are ignored, may be null)
	 * @param inUse    the colors in use (null entries are ignored, may be null)
	 */
	public ColorGroup(String title, Collection<Color> standard, Collection<Color> inUse) {
		this.title = title;
		this.standard = distinctNonNull(standard);
		this.inUse = distinctNonNull(inUse);
		this.inUse.removeAll(this.standard);
	}

	/**
	 * @param colors the colors to filter (may be null)
	 * @return the non-null colors in their original order, without duplicates
	 */
	private static List<Color> distinctNonNull(Collection<Color> colors) {
		List<Color> result = new ArrayList<>();
		if (colors == null) {
			return result;
		}
		for (Color c : colors) {
			if (c != null && !result.contains(c)) {
				result.add(c);
			}
		}
		return result;
	}
}
