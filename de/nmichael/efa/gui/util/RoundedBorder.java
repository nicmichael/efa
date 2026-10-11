package de.nmichael.efa.gui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;	



public class RoundedBorder extends AbstractBorder implements Border{

    private final static int MARGIN = 3;
    private final static int ARC = 10;
    private static final long serialVersionUID = 1L;
    private Color color;

    public RoundedBorder(Color clr) {
        color = clr;
    }

    public void setColor(Color clr) {
        color = clr;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;
    	g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setColor(new JPanel().getBackground());
        g2d.drawRect(x, y, width-1, height-1);
        g2d.setColor(color);
        g2d.drawRoundRect(x, y, width-1, height-1, ARC, ARC);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(MARGIN, MARGIN, MARGIN, MARGIN);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = MARGIN;
        insets.top = MARGIN;
        insets.right = MARGIN;
        insets.bottom = MARGIN;
        return insets;
    }
}  
