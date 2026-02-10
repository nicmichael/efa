package de.nmichael.efa.gui.util;

import java.awt.Graphics;

import javax.swing.JPanel;

public class RoundedPanel extends JPanel {

	private static final long serialVersionUID = 2810604129112946801L;
	private static int ARC = 10;	
	  protected void paintComponent(Graphics g) {
	        if (ui != null) {
	            Graphics scratchGraphics = (g == null) ? null : g.create();
	            try {
	                if (this.isOpaque()) {
	                    g.setColor(this.getBackground());
	                    g.fillRoundRect(0, 0, this.getWidth()-1,this.getHeight()-1,ARC,ARC);
	                }
	                ui.paint(g, this);
	            }
	            finally {
	                scratchGraphics.dispose();
	            }
	        }
	    }
}
