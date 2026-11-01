package de.nmichael.efa.gui.util;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * A {@link RoundedBackgroundPanel} that additionally implements
 * {@link Scrollable} so that, when placed inside a {@link JScrollPane}, it
 * stretches to fill the full height of the viewport whenever the surrounding
 * window is resized larger than the panel's natural (preferred) size - instead
 * of leaving blank space below it, as a plain {@link JPanel} would.
 * <p>
 * If the viewport becomes smaller than the panel's preferred size again (e.g.
 * the window is shrunk, or the palette has many rows), the panel reverts to its
 * natural size and the scroll pane's vertical scrollbar reappears as needed.
 */
public class ScrollableRoundedPanel extends RoundedBackgroundPanel implements Scrollable {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a scrollable rounded background panel.
	 *
	 * @param layout  the layout manager for the panel's children
	 * @param arcSize the corner arc diameter, in pixels
	 */
	ScrollableRoundedPanel(LayoutManager layout, int arcSize) {
		super(layout, arcSize);
	}

	/** {@inheritDoc} */
	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	/** {@inheritDoc} */
	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 16;
	}

	/** {@inheritDoc} */
	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
	}

	/**
	 * The grid never needs to scroll horizontally, so it always tracks (matches)
	 * the viewport's width.
	 */
	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	/**
	 * Tracks (stretches to fill) the viewport's height only while the viewport is
	 * at least as tall as this panel's preferred height; otherwise reports
	 * {@code false} so the scroll pane falls back to the panel's natural size and
	 * offers a vertical scrollbar.
	 */
	@Override
	public boolean getScrollableTracksViewportHeight() {
		Container parent = getParent();
		if (parent instanceof JViewport) {
			return ((JViewport) parent).getHeight() > getPreferredSize().height;
		}
		return false;
	}
}