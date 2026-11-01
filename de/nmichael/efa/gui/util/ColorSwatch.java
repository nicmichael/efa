package de.nmichael.efa.gui.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

/**
 * A clickable, rounded-rectangle color swatch that displays its own hex color
 * code twice - once in black and once in white, stacked on top of each other in
 * bold - so the code remains legible regardless of the swatch's brightness.
 * <p>
 * Intentionally does <strong>not</strong> use {@code setToolTipText()}: besides
 * being redundant (the hex code is already painted directly onto the swatch),
 * a visible tooltip can swallow the very next mouse click - Swing's
 * {@code ToolTipManager} may use that click only to dismiss the tooltip popup
 * instead of delivering it to this component's {@link java.awt.event.MouseListener}s. That
 * made clicks sporadically appear to do nothing, especially when the user
 * paused over a swatch (triggering the tooltip) before clicking.
 */
public class ColorSwatch extends JComponent {

	private static final long serialVersionUID = 1L;

	private static final BasicStroke BORDER_STROKE = new BasicStroke(1);
	private static final Color BORDER_COLOR = new Color(0, 0, 0, 40);
	private static final float LABEL_SIZE = 11f;

	private Color currentColor;
	private String currentHex;
	private Font labelFont;
	private Font labelFontBase;

	/**
	 * Creates a new swatch for the given color.
	 *
	 * @param color the initial color to display
	 */
	ColorSwatch(Color color) {
		this.currentColor = color;
		this.currentHex = hexOf(color);
		setPreferredSize(new Dimension(70, 44));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	/** @return the color currently displayed by this swatch */
	Color getCurrentColor() {
		return currentColor;
	}

	/**
	 * Updates the displayed color and repaints the swatch. Unchanged colors are
	 * ignored to avoid unnecessary repaints.
	 *
	 * @param c the new color
	 */
	void setCurrentColor(Color c) {
		if (setColorSilently(c)) {
			repaint();
		}
	}

	/**
	 * Updates the color without repainting. Intended for bulk updates where the
	 * caller repaints the parent container once.
	 *
	 * @param c the new color
	 * @return true if the color actually changed
	 */
	boolean setColorSilently(Color c) {
		if (c.equals(currentColor)) {
			return false;
		}
		this.currentColor = c;
		this.currentHex = hexOf(c);
		return true;
	}

	/**
	 * Formats a color as an uppercase "#RRGGBB" hex string.
	 *
	 * @param c the color to format
	 * @return the hex representation
	 */
	private static String hexOf(Color c) {
		return String.format("#%06X", c.getRGB() & 0xFFFFFF);
	}

	/** Returns the bold label font, re-derived only if the component font changed. */
	private Font getLabelFont() {
		Font base = getFont();
		if (labelFont == null || base != labelFontBase) {
			labelFontBase = base;
			labelFont = base.deriveFont(Font.BOLD, LABEL_SIZE);
		}
		return labelFont;
	}

	/** {@inheritDoc} */
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			int w = getWidth();
			int h = getHeight();
			int arc = Math.min(16, Math.min(w, h) / 3);

			g2.setColor(currentColor);
			g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

			g2.setColor(BORDER_COLOR);
			g2.setStroke(BORDER_STROKE);
			g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);

			g2.setFont(getLabelFont());
			FontMetrics fm = g2.getFontMetrics();
			int x = (w - fm.stringWidth(currentHex)) / 2;
			int centerY = h / 2;

			// Black label drawn above the vertical center.
			g2.setColor(Color.BLACK);
			g2.drawString(currentHex, x, centerY - 1);

			// White label drawn below the vertical center.
			g2.setColor(Color.WHITE);
			g2.drawString(currentHex, x, centerY + fm.getAscent());
		} finally {
			g2.dispose();
		}
	}
}
