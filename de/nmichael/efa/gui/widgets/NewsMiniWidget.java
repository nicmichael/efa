/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.widgets;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.Logger;
/*
 * NewMiniwidget is not a classical widget. Its config data is set up directly in efaconfig
 * (search for efaDirekt_showNews in efaconfig.) Also, efaBoathouseFrame knows this widget directly,
 * and positions it directly in the lowest possible position of efaBoathouseFrame.
 * Still, the new MiniWidget and 
 */
public class NewsMiniWidget {

    private NewsMiniWidgetPanel mainNewsWidgetPanel = new NewsMiniWidgetPanel();
    private NewsUpdater newsUpdater;

    public NewsMiniWidget() {
        mainNewsWidgetPanel.setText("+++ News +++");
        mainNewsWidgetPanel.setForeground(Color.white);
        mainNewsWidgetPanel.setBackground(Color.red);
        mainNewsWidgetPanel.setOpaque(true);
        mainNewsWidgetPanel.setVisible(false);
        mainNewsWidgetPanel.setFont(mainNewsWidgetPanel.getFont().deriveFont(Font.BOLD));
        mainNewsWidgetPanel.setBorder(BorderFactory.createLineBorder(mainNewsWidgetPanel.getForeground(), 1, true));
        mainNewsWidgetPanel.setWidthPercent(Daten.efaConfig.getValueEfaDirekt_newsWidthPercent());
        newsUpdater = new NewsUpdater(mainNewsWidgetPanel,mainNewsWidgetPanel.getText(), 100);
        newsUpdater.start();
    }

    public void setText(String text) {
        newsUpdater.setText(text);
    }

    public void setScrollSpeed(int scrollSpeed) {
        newsUpdater.setScrollSpeed(scrollSpeed);
    }

    public void setVisible(Boolean value) {
    	SwingUtilities.invokeLater(new Runnable() {
    		public void run() {
                mainNewsWidgetPanel.setVisible(value);
    		}
    	});    
    	// update the thread. we do not need to calculate ticket contents if we are not visible.
    	newsUpdater.setVisible(value); 
    }
    public void stopNews() {
        newsUpdater.stopNews();
    }

    public JComponent getGuiComponent() {
        return mainNewsWidgetPanel;
    }

    public final class NewsUpdater {

        private final NewsMiniWidgetPanel mainNewsWidgetPanel;

        private volatile boolean keepRunning = true;
        private volatile boolean visible = true;
        private volatile String text = "";
        private volatile int scrollSpeed = 1000;

        private Timer timer;

        // 1 Stunde in Millisekunden
        private static final int INVISIBLE_DELAY = 60 * 60 * 1000;

        public NewsUpdater(NewsMiniWidgetPanel panel, String initialText, int initialSpeed) {
            this.mainNewsWidgetPanel = panel;
            this.text = "   " + initialText + "   ";
            this.scrollSpeed = initialSpeed;
        }

        public synchronized void start() {
            if (timer != null) {
                return; // bereits gestartet
            }

            // Startverzögerung wie im Original (1 Sekunde)
            timer = new Timer(1000, e -> tick());
            timer.setRepeats(false);      // wir planen jede Periode neu
            timer.setCoalesce(true);      // verhindert Event-Flut
            timer.start();
        }

        private void tick() {
            if (!keepRunning) {
                return;
            }

            try {
                if (visible) {
                    SwingUtilities.invokeLater(
                            new MainGuiNewsUpdater(mainNewsWidgetPanel, text)
                    );
                    schedule(scrollSpeed);
                } else {
                    schedule(INVISIBLE_DELAY);
                }

            } catch (Exception ex) {
                Logger.logdebug(ex);
                schedule(scrollSpeed);
            }
        }

        private synchronized void schedule(int delayMs) {
            if (!keepRunning || timer == null) {
                return;
            }
            timer.setInitialDelay(delayMs);
            timer.restart();
        }

        // --- API ---

        public synchronized void setText(String newText) {
            this.text = "   " + newText + "   ";
            if (timer != null) {
                timer.restart(); // sofortige Wirkung
            }
        }

        public synchronized void setScrollSpeed(int speed) {
            this.scrollSpeed = speed;
            if (timer != null) {
                timer.restart();
            }
        }

        public synchronized void setVisible(boolean value) {
            this.visible = value;
            if (timer != null) {
                timer.restart();
            }
        }

        public synchronized void stopNews() {
            keepRunning = false;
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        }
    }


    /**
     * Update clock label on efaBths main GUI. Called via SwingUtilities.invokeLater()
     */
     class MainGuiNewsUpdater implements Runnable {
        
    	private String text = null;
    	private NewsMiniWidgetPanel newsPanel=null;
    	
    	public MainGuiNewsUpdater(NewsMiniWidgetPanel theNewsPanel, String theData) {
    		text = theData;
    		newsPanel = theNewsPanel;
    	}
    	
    	public void run() {
            
    		newsPanel.setText(this.text);
    		newsPanel.setWidthPercent(Daten.efaConfig.getValueEfaDirekt_newsWidthPercent());
    		newsPanel.calcNextOffset();
    		newsPanel.repaint();

		}
	}
    
}
