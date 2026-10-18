package de.nmichael.efa.gui.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

public class RoundedPanel extends JPanel {

	private static final long serialVersionUID = 2810604129112946801L;
	private static int ARC = 10;	
	  protected void paintComponent(Graphics g) {
	        if (ui != null) {
	                Graphics2D g2d = (Graphics2D) g;
	                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	                if (this.isOpaque()) {
	                	// draw background in standard color, then draw the rounded rectangle 
	                	// in the background color to create the rounded effect
	                	Color bgColor=(this.getParent()!=null ? getParent().getBackground() : new JPanel().getBackground());
	                	g2d.setColor(bgColor);
	                	g2d.setBackground(bgColor);
	                	g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
	                    g2d.setColor(this.getBackground());
	                    g2d.fillRoundRect(0, 0, this.getWidth()-1, this.getHeight()-1, ARC, ARC);
	                }
	                ui.paint(g, this);
	        } else {
	            super.paintComponent(g);        	
	        }
	    }
}
