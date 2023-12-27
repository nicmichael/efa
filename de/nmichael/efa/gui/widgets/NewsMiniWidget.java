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

import de.nmichael.efa.*;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.gui.widgets.ClockMiniWidget.MainGuiUpdater;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;

public class NewsMiniWidget {

    private JLabel label = new JLabel();
    private NewsUpdater newsUpdater;

    public NewsMiniWidget() {
        label.setText("+++ News +++");
        label.setForeground(Color.white);
        label.setBackground(Color.red);
        label.setOpaque(true);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setVisible(false);
        newsUpdater = new NewsUpdater();
        newsUpdater.start();
    }

    public void setText(String text) {
        newsUpdater.setText(text);
    }

    public void setScrollSpeed(int scrollSpeed) {
        newsUpdater.setScrollSpeed(scrollSpeed);
    }

    public void stopNews() {
        newsUpdater.stopNews();
    }

    public JComponent getGuiComponent() {
        return label;
    }

    class NewsUpdater extends Thread {

        volatile boolean keepRunning = true;
        private String text;
        private int startPosition;
        private int length;
        private int scrollSpeed;
        private int maxCharsToShow = 50;
        private int maxWidth = 600;
        private int maxCharWidth = 0;
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            while (keepRunning) {
                
                try {
                	//gets maxWidth depending on labels's width 
                	//and gets maxChar, depending on the "X" character's length in the current font
                 	getMaxCharsToShow();

                	//Use invokelater as swing threadsafe ways
                    SwingUtilities.invokeLater(new MainGuiUpdater(label, getText(text, startPosition, maxCharsToShow)));

                    startPosition = (startPosition + 1) % (length + 3);
                    if (length <= maxCharsToShow) {
                        Thread.sleep(60000);
                    } else {
                        Thread.sleep(scrollSpeed);
                    }
                } catch (Exception e) {
                    EfaUtil.foo();
                    Logger.logdebug(e);
                }
            }
        }

        private void getMaxCharsToShow() {

        	int charWidth=0;
    		Dimension dim = label.getSize();

        	if (maxCharWidth==0) {
	            FontMetrics myFontMetrics = label.getFontMetrics(label.getFont());
	            charWidth=dim.height; //default value: Character is as wide as the font's size
	            if (myFontMetrics!=null) {
	            	maxCharWidth=myFontMetrics.charWidth('X');
	            	charWidth=maxCharWidth;
	            }
        	} else {
        		charWidth=maxCharWidth;
        	}
        	//width may be zero, if label is not showing yet
            if (dim.width > 0) {
                maxCharsToShow = (dim.width / charWidth);
            }
            maxWidth = Math.max(dim.width, 600);
        }

        private String getText(String s, int pos, int max) {
            if (max >= length) {
                return s;
            }
            String t;
            if (pos == length + 2) {
                t = " ";
            } else if (pos == length + 1) {
                t = "  ";
            } else if (pos == length) {
                t = "   ";
            } else {
                t = s.substring(pos, Math.min(pos + max, length));
            }
            int l = t.length();
            if (l + 3 < max) {
                t = t + (pos < length ? "   " : "") + s.substring(0, max - l - 3);
            }
            return t;
        }
        
        public void setText(String text) {
            this.text = text;
            this.length = text.length();
            this.startPosition = 0;
            getMaxCharsToShow();
        }

        public void setScrollSpeed(int scrollSpeed) {
            this.scrollSpeed = scrollSpeed;
        }

        public void stopNews() {
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
