package de.nmichael.efa.gui.util;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;

public class RoundedLabel extends JLabel {
	private static int ARC = 10;

  protected void paintComponent(Graphics g) {
        if (ui != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
           
                if (this.isOpaque()) {
                	g2d.setColor(new JLabel().getBackground());
                	g2d.fillRect(0, 0, this.getWidth()-1, this.getHeight()-1);
                    g2d.setColor(this.getBackground());
                    g2d.fillRoundRect(0, 0, this.getWidth()-1, this.getHeight()-1, ARC, ARC);
                }
                ui.paint(g, this);
        } else {
            super.paintComponent(g);        	
        }
    }
}
