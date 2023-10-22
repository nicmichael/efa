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

import java.awt.Label;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import de.nmichael.efa.util.EfaUtil;

public class ClockMiniWidget {

    private JLabel label = new JLabel();
    private ClockUpdater clockUpdater;

    public ClockMiniWidget() {
        label.setText("12:34");
        clockUpdater = new ClockUpdater();
        clockUpdater.start();
    }

    public void stopClock() {
        clockUpdater.stopClock();
    }

    public JComponent getGuiComponent() {
        return label;
    }

    class ClockUpdater extends Thread {

        volatile boolean keepRunning = true;

        /*
         * Gets the remaining seconds until the next full minute
         */
        private long getMilliSecondsToFullMinute() {
            LocalDateTime start = LocalDateTime.now();
            // Hour + 1, set Minute and Second to 00
            LocalDateTime end = start.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);

            // Get Duration
            Duration duration = Duration.between(start, end);
            long millis = duration.toMillis();
            return 1000;
        };
        
        public void run() {
            while (keepRunning) {
                try {
                	// simply setting label text is not thread safe with swing.
                	//label.setText(EfaUtil.getCurrentTimeStampHHMM());
                	
                	//Use invokelater as swing threadsafe ways
                	SwingUtilities.invokeLater(new MainGuiUpdater(label, EfaUtil.getCurrentTimeStampHHMM().toString()));
                	
                	//wait until next full minute plus one sec. this is more accurate than just waiting 60.000 msec
                	//from a random offset.
                	long waitTime=getMilliSecondsToFullMinute()+1000;
                    Thread.sleep(waitTime);
                    
                } catch (Exception e) {
                    EfaUtil.foo();
                }
            }
        }

        public void stopClock() {
            keepRunning = false;
        }

    }
    
    /**
     * Update clock label on efaBths main GUI. Called via SwingUtilities.invokeLater()
     */
    class MainGuiUpdater implements Runnable {
        
    	private String text = null;
    	private JLabel mylabel=null;
    	
    	public MainGuiUpdater(JLabel theLabel, String theData) {
    		text = theData;
    		mylabel = theLabel;
    	}
    	
    	public void run() {
    		mylabel.setText(text);
	      }
	}

}
