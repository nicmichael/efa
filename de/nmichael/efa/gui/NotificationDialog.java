/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class NotificationDialog extends BaseDialog {

    private String text;
    private String image;
    private String textcolor;
    private String bkcolor;
    private volatile int closeTimeout;
    private JLabel closeInfoLabel;
    private boolean _canceled = false;

    public NotificationDialog(Frame parent, String text, String image, String textcolor, String bgcolor, int closeTimeout) {
        super(parent, "", International.getStringWithMnemonic("Schließen"));
        this.text = text;
        this.image = image;
        this.textcolor = textcolor;
        this.bkcolor = bgcolor;
        this.closeTimeout = closeTimeout;
        this._closeButtonText = null;
    }

    public NotificationDialog(JDialog parent, String text, String image, String textcolor, String bgcolor, int closeTimeout) {
        super(parent, "", International.getStringWithMnemonic("Schließen"));
        this.text = text;
        this.image = image;
        this.textcolor = textcolor;
        this.bkcolor = bgcolor;
        this.closeTimeout = closeTimeout;
        this._closeButtonText = null;
    }

    protected void iniDialog() throws Exception {
        this.setUndecorated(true);
        mainPanel.setLayout(new BorderLayout());

        JTextPane t = new JTextPane();
        t.setContentType("text/html");
        if (Daten.isEfaFlatLafActive()) {
            t.putClientProperty("html.disable", Boolean.TRUE); 
        	t.setFont(t.getFont().deriveFont(Font.PLAIN,14));
        }
        t.setEditable(false);
        t.setText("<html><body bgcolor=\"#" + bkcolor + "\">" +
                "<table cellpadding=\"20\" align=\"center\"><tr>" +
                "<td><img src=\"" + EfaUtil.saveImage(image, "png", Daten.efaTmpDirectory,
                true, false, true) + "\"></td>" +
                "<td align=\"center\" valign=\"middle\" style=\"font-family:sans-serif; font-size:24pt; font-weight:bold; color:#ffffff\">" + text + "</td>" +
                "</tr></table>" +
                "</html");
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                cancel();
            }
        });

        t.setPreferredSize(new Dimension(600, 175));
        mainPanel.add(t, BorderLayout.CENTER);
        closeInfoLabel = new JLabel();
        closeInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        closeInfoLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        mainPanel.add(closeInfoLabel, BorderLayout.SOUTH);
        if (closeTimeout > 0) {
            (new CloseTimeoutThread()).start();
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public boolean cancel() {
        if (!_canceled) {
            _canceled = true;
            closeTimeout = 0;
            return super.cancel();
        }
        return true;
    }

    class CloseTimeoutThread extends Thread {
        public void run() {
        	this.setName("CloseTimeoutThread");
            for (int i=0; i<closeTimeout;i++) {
    	    	String value= International.getMessage("Dieses Fenster schließt automatisch in {sec} Sekunden ...",
                          Math.max(closeTimeout-i, 0));
    	    	
            	SwingUtilities.invokeLater(new Runnable() {
          	      public void run() {
                      closeInfoLabel.setText(value);
          	      }
            	});

                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                }
            }

        	SwingUtilities.invokeLater(new Runnable() {
        	      public void run() {
        	            cancel();
        	      }
          	});

        }
    }

}
