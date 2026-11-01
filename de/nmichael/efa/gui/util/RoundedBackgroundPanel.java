package de.nmichael.efa.gui.util;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/**
 * A simple, non-opaque {@link JPanel} that paints its background as a
 * rounded rectangle before delegating to the normal Swing paint pipeline.
 * Used to give the palette and recent-colors areas rounded corners.
 */
public class RoundedBackgroundPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final int arcSize;

    /**
     * Creates a rounded background panel.
     *
     * @param layout  the layout manager for the panel's children
     * @param arcSize the corner arc diameter, in pixels
     */
    public RoundedBackgroundPanel(LayoutManager layout, int arcSize) {
        super(layout);
        this.arcSize = arcSize;
        setOpaque(false);
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arcSize, arcSize);
        g2.dispose();
        super.paintComponent(g);
    }
}