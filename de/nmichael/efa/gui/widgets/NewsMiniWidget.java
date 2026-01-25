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

import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;

public class NewsMiniWidget {

    private NewsMiniWidgetPanel label = new NewsMiniWidgetPanel();
    private NewsUpdater newsUpdater;

    public NewsMiniWidget() {
        label.setText("+++ News +++");
        label.setForeground(Color.white);
        label.setBackground(Color.red);
        label.setOpaque(true);
        label.setVisible(false);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setBorder(BorderFactory.createLineBorder(label.getForeground(), 1, true));
        newsUpdater = new NewsUpdater();
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
                label.setVisible(value);
    		}
    	});    
    	// update the thread. we do not need to calculate ticket contents if we are not visible.
    	newsUpdater.setVisible(value); 
    }
    public void stopNews() {
        newsUpdater.stopNews();
    }

    public JComponent getGuiComponent() {
        return label;
    }

    class NewsUpdater extends Thread {

        volatile boolean keepRunning = true;
        volatile boolean visible=true;
        private String text;
        private int scrollSpeed;

        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            this.setName("NewsUpdater"+" "+DataTypeTime.now().toString());
            while (keepRunning) {
                
                try {
                	if (visible) {
	                	//Use invokelater as swing threadsafe ways
	                    SwingUtilities.invokeLater(new MainGuiNewsUpdater(label, text));
                        Thread.sleep(scrollSpeed);
                	} else {
                		//not visible. sleep an hour. if someone makes us visible, we get woken up by an interruptedexception.
                		Thread.sleep(60*60*1000);
                	}
                } catch (InterruptedException e) {
                	EfaUtil.foo();
                } catch (Exception e) {
                    Logger.logdebug(e);
                }
            }
        }
        
        /* in the following functions, it is neccessary to interrupt the thread to get the settings get active.
         * because sometimes, the sleep time is very high.
         */
        public synchronized void setText(String text) {
            this.text = "   "+text+"   ";
            interrupt();
        }

        public synchronized void setScrollSpeed(int scrollSpeed) {
            this.scrollSpeed = scrollSpeed;
            interrupt();
        }

        public synchronized void stopNews() {
            keepRunning = false;
            interrupt();
        }
        
        public synchronized void setVisible(boolean value) {
        	visible=value;
        	interrupt();
        }

    }

    /**
     * Update clock label on efaBths main GUI. Called via SwingUtilities.invokeLater()
     */
    private class MainGuiNewsUpdater implements Runnable {
        
    	private String text = null;
    	private NewsMiniWidgetPanel myLabel=null;
    	
    	public MainGuiNewsUpdater(NewsMiniWidgetPanel theLabel, String theData) {
    		text = theData;
    		myLabel = theLabel;
    	}
    	
    	public void run() {
            
    		myLabel.setText(this.text);
    		myLabel.calcNextOffset();
    		myLabel.repaint();

		}
	}
    
}
