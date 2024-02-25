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
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ProgressDialog extends BaseDialog {

	private static final long serialVersionUID = 4155197013485180926L;

	private ProgressTask progressTask;
    private JTextArea loggingTextArea;
    private JLabel currentStatusLabel;
    private JProgressBar progressBar;
    private boolean hasProgressBar = true;

    public ProgressDialog(Frame parent, String title, ProgressTask progressTask, boolean autoCloseDialogWhenDone) {
        super(parent, title, International.getStringWithMnemonic("Schließen"));
        initialize(progressTask, autoCloseDialogWhenDone, false);
    }

    public ProgressDialog(JDialog parent, String title, ProgressTask progressTask, boolean autoCloseDialogWhenDone) {
        super(parent, title, International.getStringWithMnemonic("Schließen"));
        initialize(progressTask, autoCloseDialogWhenDone, false);
    }

    public ProgressDialog(Frame parent, String title, ProgressTask progressTask, boolean autoCloseDialogWhenDone, boolean minimalDialog) {
        super(parent, title, International.getStringWithMnemonic("Schließen"));
        initialize(progressTask, autoCloseDialogWhenDone, minimalDialog);
    }

    public ProgressDialog(JDialog parent, String title, ProgressTask progressTask, boolean autoCloseDialogWhenDone, boolean minimalDialog) {
        super(parent, title, International.getStringWithMnemonic("Schließen"));
        initialize(progressTask, autoCloseDialogWhenDone, minimalDialog);
    }

    // dummy progress dialog, to be used by CLI
    public ProgressDialog() {
        super((JDialog)null, null, null);
    }

    private void initialize(ProgressTask progressTask, boolean autoCloseDialogWhenDone, boolean minimalDialog) {
        this.progressTask = progressTask;
        super.enableWindowStackChecks(false);
        progressTask.setProgressDialog(this, autoCloseDialogWhenDone);
        if (minimalDialog) {
            currentStatusLabel = new JLabel();
        } else {
            loggingTextArea = new JTextArea();
        }
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());

        if (loggingTextArea != null) {
            JScrollPane loggingScrollPane = new JScrollPane();
            loggingScrollPane.setPreferredSize(new Dimension(550, 200));
            loggingScrollPane.setMinimumSize(new Dimension(550, 200));
            loggingTextArea.setEditable(false);
            //loggingTextArea.setWrapStyleWord(true);
            //loggingTextArea.setLineWrap(true);
            loggingScrollPane.getViewport().add(loggingTextArea, null);
            mainPanel.add(loggingScrollPane, BorderLayout.CENTER);
        }
        if (currentStatusLabel != null) {
            currentStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            mainPanel.add(currentStatusLabel, BorderLayout.CENTER);
        }

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new GridBagLayout());
        JLabel progressLabel = new JLabel();
        progressLabel.setText(International.getString("Fortschritt")+":");
        progressPanel.add(progressLabel,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                    ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 0, 10), 0, 0));
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressPanel.add(progressBar,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                    ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 5, 10), 0, 0));
        mainPanel.add(progressPanel, BorderLayout.SOUTH);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void showNonModalDialog() {
        Daten.iniSplashScreen(false);
        if (!_prepared && !prepareDialog()) return;
        Dialog.setDlgLocation(this, _parent);
        setModal(false);
        if (focusItem != null) focusItem.requestFocus();
        this.setVisible(true);
    }

    public void logInfo(String s) {
        for (int tryi=1; loggingTextArea == null && currentStatusLabel == null && tryi<=10; tryi++) {
            try { Thread.sleep(100*tryi); } catch(Exception e) {} // Dialog may not have been fully initialized when progress thread starts running
        }
        if (loggingTextArea != null) {
            loggingTextArea.append(s);
            loggingTextArea.setCaretPosition(loggingTextArea.getDocument().getLength());
        }
        if (currentStatusLabel != null) {
            currentStatusLabel.setText(s);
        }
    }

    public void setCurrentWorkDone(int i) {
        if (!hasProgressBar || 
            (progressBar == null && Daten.applID == Daten.APPL_CLI)) {
            return;
        }
        for (int tryi=1; progressBar == null && tryi<=10; tryi++) {
            try { Thread.sleep(100*tryi); } catch(Exception e) {} // Dialog may not have been fully initialized when progress thread starts running
        }
        if (progressBar != null) {
            // can be null for dummy ProgressDialog used in CLI
            progressBar.setMaximum(progressTask.getAbsoluteWork());
            progressBar.setValue(i);
        } else {
            // give up: we won't get a progress bar (don't wait each time we update progress)
            hasProgressBar = false;
        }
    }

    public boolean cancel() {
        boolean _cancel = false;
        if (progressTask.isRunning()) {
            if (Dialog.yesNoDialog(International.getString("Abbruch"),
                    International.getString("Möchtest Du den Vorgang wirklich abbrechen?")) == Dialog.YES) {
                _cancel = true;
            }
        } else {
            _cancel = true;
        }
        if (_cancel) {
            progressTask.abort();
            return super.cancel();
        } else {
            return false;
        }
    }

}
