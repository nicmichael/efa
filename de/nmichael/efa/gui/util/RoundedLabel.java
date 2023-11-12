package de.nmichael.efa.gui.util;

import java.awt.Graphics;

import javax.swing.JLabel;

public class RoundedLabel extends JLabel {

    protected void paintComponent(Graphics g) {
        if (ui != null) {
            Graphics scratchGraphics = (g == null) ? null : g.create();
            try {
                if (this.isOpaque()) {
                    g.setColor(this.getBackground());
                    g.fillRoundRect(0, 0, this.getWidth()-1,this.getHeight()-1,10,10);
                }
                ui.paint(g, this);
            }
            finally {
                scratchGraphics.dispose();
            }
        }
    }
}
