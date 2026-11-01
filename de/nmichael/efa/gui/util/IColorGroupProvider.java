package de.nmichael.efa.gui.util;

/**
 * Provides one color group for the efa-Standardfarben chooser tab. The chooser
 * panel only renders the groups it receives and does not know where the colors
 * come from (efa configuration, widgets, ...).
 */
public interface IColorGroupProvider {

	/**
	 * @return the color group; the colors in use are read at the time of the call
	 */
	ColorGroup getColorGroup();
}
