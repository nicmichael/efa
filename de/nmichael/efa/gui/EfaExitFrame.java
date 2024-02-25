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
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class EfaExitFrame extends BaseFrame {

	private static final long serialVersionUID = 7311812898221459649L;

	private static EfaExitFrame dlg = null;
    private boolean restart;
    private int who;
    private CountdownThread thread;
    EfaBoathouseFrame efaBoathouseFrame;
    JPanel jPanel1 = new JPanel();
    JLabel reasonLabel = new JLabel();
    JLabel shutDownLabelPart1 = new JLabel();
    JLabel shutDownLabelSeconds = new JLabel();
    JLabel shutDownLabelPart2 = new JLabel();
    JButton dontExitButton = new JButton();

    public EfaExitFrame(EfaBoathouseFrame efaBoathouseFrame) {
        super(efaBoathouseFrame, Daten.EFA_LONGNAME);
        this.efaBoathouseFrame = efaBoathouseFrame;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        jPanel1.setBackground(Color.red);
        jPanel1.setLayout(new GridBagLayout());
        reasonLabel.setFont(new java.awt.Font("Dialog", 1, 18));
        reasonLabel.setForeground(Color.black);
        shutDownLabelPart1.setFont(new java.awt.Font("Dialog", 1, 18));
        shutDownLabelPart1.setForeground(Color.black);
        String t = International.getMessage("efa wird in {sec} Sekunden automatisch beendet ...", 10);
        if (t != null && t.length() > 0) {
            int pos = t.indexOf("10");
            if (pos >= 0) {
                String t1 = t.substring(0, pos);
                String t2 = t.substring(pos + 2);
                shutDownLabelPart1.setText(t1);
                shutDownLabelPart2.setText(t2);
            }
        }
        shutDownLabelSeconds.setFont(new java.awt.Font("Dialog", 1, 18));
        shutDownLabelSeconds.setForeground(Color.black);
        shutDownLabelSeconds.setText("10");
        shutDownLabelPart2.setFont(new java.awt.Font("Dialog", 1, 18));
        shutDownLabelPart2.setForeground(Color.black);
        reasonLabel.setText(" --- " + International.getString("Grund") + " --- ");
        Mnemonics.setButton(this, dontExitButton, International.getStringWithMnemonic("efa noch nicht beenden"));
        dontExitButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dontExitButton_actionPerformed(e);
            }
        });
        this.setTitle(International.getString("Automatisches Beenden von efa"));
        mainPanel.add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(reasonLabel, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 20, 0, 20), 0, 0));
        jPanel1.add(shutDownLabelPart1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
        jPanel1.add(shutDownLabelSeconds, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
        jPanel1.add(shutDownLabelPart2, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 0, 20), 0, 0));
        jPanel1.add(dontExitButton, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(50, 20, 20, 20), 0, 0));

        thread = new CountdownThread(this);
    }

    void cancel(boolean _exit) {
        thread.stopRunning = true;
        try {
            thread.interrupt();
            thread.join();
        } catch (InterruptedException e) {
        }
        Dialog.frameClosed(this);
        this.setVisible(false);
        if (_exit) {
            thread = null;
            Logger.log(Logger.INFO, Logger.MSG_EVT_EFAEXIT,
                    International.getString("efa beendet sich jetzt")
                    + (restart ? " " + International.getString("und wird anschließend neu gestartet") + "."
                    : "."));
            // disable window stack checks: EfaBoathouseFrame may not be top of stack; yet we're closing it
            Dialog.IGNORE_WINDOW_STACK_CHECKS = true;
            efaBoathouseFrame.cancel(null, who, null, restart);
        } else {
            thread = new CountdownThread(this); // Thread für's nächste Mal initialisieren
            Logger.log(Logger.WARNING, Logger.MSG_EVT_EFAEXITABORTED,
                    International.getString("Beenden von efa wurde durch Benutzer abgebrochen."));
            Daten.DONT_SAVE_ANY_FILES_DUE_TO_OOME = false;
        }
    }

    void dontExitButton_actionPerformed(ActionEvent e) {
        cancel(false);
    }

    private void activateExitFrame(String reason) {
        this.dontExitButton.requestFocus();
        dlg.showFrame();
        // the reason must be set AFTER running dlg.showFrame() as showframe may run the first time
        // and sets the reasonLabel to a static text.
        reasonLabel.setText(reason);        
        if (thread == null) {
            thread = new CountdownThread(this);
        }
        thread.start();
    }

    public static void initExitFrame(EfaBoathouseFrame frame) {
        dlg = new EfaExitFrame(frame);
    }

    public static void exitEfa(String reason, boolean restart, int who) {
        if (dlg == null) {
            return;
        }
        if (dlg.thread != null && dlg.thread.isAlive()) {
            return; // doppelter Aufruf
        }
        dlg.restart = restart;
        dlg.who = who;
        dlg.activateExitFrame(reason);
    }

    class CountdownThread extends Thread {

        public boolean stopRunning;
        private String secondsLeftText;
        private EfaExitFrame frame;

        public CountdownThread(EfaExitFrame frame) {
            this.frame = frame;
            this.stopRunning = false;
        }

        public void run() {
        	this.setName("CountDownThread");
            for (int remainingSeconds = 10; remainingSeconds >0; remainingSeconds--) {
            	
            	//okay, this is a hack, but it does the trick for converting remainingSeconds into a String.
            	secondsLeftText=""+remainingSeconds;
            	SwingUtilities.invokeLater(new Runnable() {
          	      public void run() {
                      frame.shutDownLabelSeconds.setText(secondsLeftText);
          	      }
            	});
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (stopRunning) {
                    return;
                }
            }
        	SwingUtilities.invokeLater(new Runnable() {
        	      public void run() {
        	            frame.cancel(true);
        	      }
          	});

        }
    }
}
