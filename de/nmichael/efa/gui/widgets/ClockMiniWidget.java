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

import java.awt.Font;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;

public class ClockMiniWidget {

    private JLabel label = new JLabel();
    private ClockUpdater clockUpdater;

    public ClockMiniWidget() {
        label.setText("12:34");
        //always show clock in bold font. 
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        clockUpdater = new ClockUpdater();
        clockUpdater.start();
    }

    public void stopClock() {
        clockUpdater.stopClock();
    }

    public JComponent getGuiComponent() {
        return label;
    }

    public void setVisible(Boolean value) {
    	SwingUtilities.invokeLater(new Runnable() {
    		public void run() {
                label.setVisible(value);
    		}
    	});     
    	clockUpdater.setVisible(value);
    }
    
    class ClockUpdater extends Thread {

        volatile boolean keepRunning = true;
        volatile boolean visible=true;

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
            return millis;
        };
        
        public void run() {
        	this.setName("ClockUpdater");
            while (keepRunning) {
                try {
                	// simply setting label text is not thread safe with swing.
                	//label.setText(EfaUtil.getCurrentTimeStampHHMM());
                	if (visible) {
	                	//Use invokelater as swing threadsafe ways
	                	SwingUtilities.invokeLater(new MainGuiClockUpdater(label, EfaUtil.getCurrentTimeStampHHMM().toString()));
	                	
	                	//wait until next full minute plus one sec. this is more accurate than just waiting 60.000 msec
	                	//from a random offset.
	                	long waitTime=getMilliSecondsToFullMinute()+1000;
	                    Thread.sleep(waitTime);
                	} else {
                		Thread.sleep(60*60*1000); // not visible, so sleep an hour
                	}
                	
                } catch (InterruptedException e) {
                	EfaUtil.foo();
                } 
                catch (Exception e) {
                    Logger.logdebug(e);
                }
            }
        }

        public void stopClock() {
            keepRunning = false;
            interrupt();
        }

        public synchronized void setVisible(Boolean value) {
        	visible=value;
        	interrupt();//wake up thread
        }
        
    }
    
    /**
     * Update clock label on efaBths main GUI. Called via SwingUtilities.invokeLater()
     */
    private class MainGuiClockUpdater implements Runnable {
        
    	private String text = null;
    	private JLabel mylabel=null;
    	
    	public MainGuiClockUpdater(JLabel theLabel, String theData) {
    		text = theData;
    		mylabel = theLabel;
    	}
    	
    	public void run() {
    		mylabel.setText(text);
	      }
	}

}
