package de.nmichael.efa.gui.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import de.nmichael.efa.gui.util.RoundedPanel;

public class NewsMiniWidgetPanel extends RoundedPanel{

	private static final long serialVersionUID = 4614052035348797371L;
	private String text = "";
    private BufferedImage textImage;
    private int offsetX;
    private double widthPercent=0.80;

    public NewsMiniWidgetPanel() {
        setPreferredSize(new Dimension(400, this.getFont().getSize()+10));
        // Startposition rechts außerhalb des Panels
        setOffsetX(0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        // --- MARQUEE-BEREICH ---
        int marqueeWidth = (int) (panelWidth * widthPercent);
        int marqueeX = (panelWidth - marqueeWidth) / 2;

        int imgHeight = this.getHeight();
        int y = (panelHeight - imgHeight) / 2;

        // Clipping aktivieren, damit der Text NUR im Bereich sichtbar ist
        Shape oldClip = g2.getClip();
        g2.setClip(marqueeX, y, marqueeWidth, imgHeight);

        // Text so häufig zeichnen, bis der verfügbare Bereich ausgenutzt ist.
        g2.drawImage(textImage, marqueeX + offsetX, y, null);
        int repeat = (panelWidth / textImage.getWidth())+1;
        for (int i=1; i<=repeat; i++) {
        	g2.drawImage(textImage, marqueeX + offsetX + (textImage.getWidth()*i), y, null);
        }
        g2.setClip(oldClip);

        // Optional: Rahmen um den Bereich
        //g2.setColor(Color.DARK_GRAY);
        //g2.drawRect(marqueeX, y, marqueeWidth-1, imgHeight-1);

        g2.dispose();
       
    }

	public String getText() {
		return text;
	}

	public void setText(String text) {
		if (this.text == null || !this.text.equals(text)) {
			this.text= text;
			createTextImage();
			setOffsetX(0);
		}
	}
	
    private void createTextImage() {
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dummy.createGraphics();

        g.setFont(this.getFont());
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text);
        int h = fm.getHeight();
        g.dispose();

        textImage = new BufferedImage(w + 20, h, BufferedImage.TYPE_INT_ARGB);
        g = textImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(Color.RED);
        g.fillRect(0, 0, textImage.getWidth(), textImage.getHeight());

        g.setFont(this.getFont());
        g.setColor(Color.WHITE);
        g.drawString(text, 20, 0 + fm.getAscent());

        g.dispose();
    }

	private int getOffsetX() {
		return offsetX;
	}

	private void setOffsetX(int offsetX) {
		this.offsetX = offsetX;
	}

	public void calcNextOffset() {
        offsetX -= 4;

        if (offsetX + textImage.getWidth() < 0) {
            offsetX = 0;
        }
	}

	public int getWidthPercent() {
		return (int)widthPercent*100;
	}

	public void setWidthPercent(int widthPercent) {
		this.widthPercent = widthPercent/100.0;
	}
	
	
	
	
	
}
