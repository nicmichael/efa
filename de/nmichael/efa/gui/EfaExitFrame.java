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

import de.nmichael.efa.*;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.gui.widgets.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.config.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.data.storage.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;

public class EfaExitFrame extends BaseFrame {

    private static EfaExitFrame dlg = null;
    private boolean restart;
    private int who;
    private CountdownThread thread;
    EfaBoathouseFrame efaBoathouseFrame;
    JPanel jPanel1 = new JPanel();
    JLabel jLabel0 = new JLabel();
    JLabel jLabel1 = new JLabel();
    JLabel sekundenLabel = new JLabel();
    JLabel jLabel3 = new JLabel();
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
        jLabel0.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel0.setForeground(Color.black);
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel1.setForeground(Color.black);
        String t = International.getMessage("efa wird in {sec} Sekunden automatisch beendet ...", 10);
        if (t != null && t.length() > 0) {
            int pos = t.indexOf("10");
            if (pos >= 0) {
                String t1 = t.substring(0, pos);
                String t2 = t.substring(pos + 2);
                jLabel1.setText(t1);
                jLabel3.setText(t2);
            }
        }
        sekundenLabel.setFont(new java.awt.Font("Dialog", 1, 18));
        sekundenLabel.setForeground(Color.black);
        sekundenLabel.setText("10");
        jLabel3.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel3.setForeground(Color.black);
        jLabel0.setText(" --- " + International.getString("Grund") + " --- ");
        Mnemonics.setButton(this, dontExitButton, International.getStringWithMnemonic("efa noch nicht beenden"));
        dontExitButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dontExitButton_actionPerformed(e);
            }
        });
        this.setTitle(International.getString("Automatisches Beenden von efa"));
        mainPanel.add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(jLabel0, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 20, 0, 20), 0, 0));
        jPanel1.add(jLabel1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
        jPanel1.add(sekundenLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
        jPanel1.add(jLabel3, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 0, 20), 0, 0));
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
        jLabel0.setText(reason);
        this.dontExitButton.requestFocus();
        dlg.showFrame();
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
        int left;
        EfaExitFrame frame;
        String[] secLeft = new String[10];

        public CountdownThread(EfaExitFrame frame) {
            this.frame = frame;
            stopRunning = false;
            for (int i = 0; i < 10; i++) {
                secLeft[i] = Integer.toString(10 - i);
            }
        }

        public void run() {
            for (int i = 0; i < 10; i++) {
                frame.sekundenLabel.setText(secLeft[i]);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (stopRunning) {
                    return;
                }
            }
            frame.cancel(true);
        }
    }
}
