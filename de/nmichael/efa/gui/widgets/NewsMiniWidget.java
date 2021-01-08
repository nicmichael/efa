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
        private int showing;
        private int length;
        private int scrollSpeed;
        private int maxChar = 50;
        private int maxWidth = 600;

        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            while (keepRunning) {
                getMaxChar();
                try {
                    do {
                        label.setText(getText(text, showing, maxChar));
                    } while (label.getPreferredSize().getWidth() > maxWidth && maxChar-- > 10);
                    showing = (showing + 1) % (length + 3);
                    if (length <= maxChar) {
                        Thread.sleep(60000);
                    } else {
                        Thread.sleep(scrollSpeed);
                    }
                } catch (Exception e) {
                    EfaUtil.foo();
                }
            }
        }

        private void getMaxChar() {
            Dimension dim = label.getSize();
            if (dim.width > 0 && dim.height > 0) {
                maxChar = (dim.width / dim.height) * 2;
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
            this.showing = 0;
            getMaxChar();
        }

        public void setScrollSpeed(int scrollSpeed) {
            this.scrollSpeed = scrollSpeed;
        }

        public void stopNews() {
            keepRunning = false;
        }

    }

}
