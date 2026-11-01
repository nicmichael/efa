package de.nmichael.efa.gui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.AbstractBorder;
import javax.swing.border.TitledBorder;

// =====================================================================
// Rounded-corner titled border
// =====================================================================

/**
 * A thin, rounded-corner line border (arc radius 8px) used as the decorative
 * frame for {@link TitledBorder}s throughout this chooser, giving every titled
 * group box (saturation, hex code, recently used) softly rounded corners
 * instead of sharp ones.
 */
public class RoundedLineBorder extends AbstractBorder {

	private static final long serialVersionUID = 1L;

	/** Arc radius (in pixels) applied to all four corners. */
	private static final int ARC = 8;

	private final Color lineColor;

	/**
	 * Creates a new rounded line border.
	 *
	 * @param lineColor the color used to stroke the border line
	 */
	public RoundedLineBorder(Color lineColor) {
		this.lineColor = lineColor;
	}

	/** {@inheritDoc} */
	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(lineColor);
		g2.drawRoundRect(x, y, width - 1, height - 1, ARC, ARC);
		g2.dispose();
	}

	/** {@inheritDoc} */
	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(4, 6, 4, 6);
	}

	/** {@inheritDoc} */
	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.set(4, 6, 4, 6);
		return insets;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isBorderOpaque() {
		return false;
	}
}