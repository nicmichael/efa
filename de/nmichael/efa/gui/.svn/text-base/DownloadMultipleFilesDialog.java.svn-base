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
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

// @i18n complete
public class DownloadMultipleFilesDialog extends BaseDialog {

    String[] remoteFileNames;
    String[] localFileNames;
    int[] fileSizes;
    String downloadName;
    boolean buttonClose = false;
    JDialog parent;
    boolean exit = false;
    BorderLayout borderLayout1 = new BorderLayout();
    JButton button = new JButton();
    JScrollPane outputScrollPane = new JScrollPane();
    JTextArea output = new JTextArea();

    public DownloadMultipleFilesDialog(JDialog parent, String downloadName, 
            String[] remoteFileNames, String[] localFileNames, int[] fileSizes,
            boolean exit) {
        super(parent, International.getStringWithMnemonic("Download"), International.getStringWithMnemonic("Download starten"));
        this.downloadName = downloadName;
        this.remoteFileNames = remoteFileNames;
        this.localFileNames = localFileNames;
        this.fileSizes = fileSizes;
        this.exit = exit;
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());

        int size = 0;
        for (int c = 0; c < remoteFileNames.length; c++) {
            if (remoteFileNames[c] != null) {
                size += fileSizes[c];
            }
        }

        output.append(International.getMessage("Folgende Dateien werden jetzt installiert (Gesamtgröße: {size} Bytes):", size) + "\n");
        for (int c = 0; c < remoteFileNames.length; c++) {
            if (remoteFileNames[c] != null) {
                output.append(remoteFileNames[c] + " (" + fileSizes[c] + " byte)\n");
                size += fileSizes[c];
            }
        }
        output.append("\n");

        output.setFont(new java.awt.Font("Dialog", 1, 12));
        output.setEditable(false);
        mainPanel.add(outputScrollPane, BorderLayout.CENTER);
        outputScrollPane.setSize(new Dimension(800, 500));
        outputScrollPane.setPreferredSize(new Dimension(800, 500));
        outputScrollPane.getViewport().add(output, null);
        closeButton.setIcon(getIcon(BaseDialog.IMAGE_DOWNLOAD));
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public boolean cancel() {
        if (AfterDownloadImpl.fileCount > 0) {
            if (Dialog.yesNoDialog(International.getString("Offene Downloads"),
                    International.getString("Es sind noch nicht alle Downloads beendet. "
                    + "Möchtest Du wirklich abbrechen?")) != Dialog.YES) {
                return false;
            }
        }

        // Fertig
        String s = "";
        File f;
        boolean ok = true;
        for (int i = 0; i < localFileNames.length; i++) {
            if (localFileNames[i] != null) {
                f = new File(localFileNames[i]);
                if (f.isFile()) {
                    if (f.length() != fileSizes[i]) {
                        s += remoteFileNames[i] + ": " +
                                International.getMessage("ungültige Dateigröße (erwartet war: {size})", fileSizes[i]) + "\n";
                        ok = false;
                    }
                } else {
                    s += remoteFileNames[i] + ": " + International.getString("nicht installiert") + "\n";
                    ok = false;
                }
            }
        }

        if (ok) {
            Dialog.infoDialog(International.getMessage("{name} wurde erfolgreich installiert. ", downloadName)
                    + "\n" +
                    LogString.onlyEffectiveAfterRestart(downloadName));
            if (exit) {
                Daten.haltProgram(0);
            }
        } else {
            Dialog.error(LogString.installationFailed(downloadName) + "\n" + s);
            if (exit) {
                Daten.haltProgram(Daten.HALT_INSTALLATION);
            }
        }

        return super.cancel();
    }

    void runDownload() {
        // Download der Plugin-Files
        AfterDownloadImpl.fileCount = remoteFileNames.length;
        for (int c = 0; c < remoteFileNames.length; c++) {
            if (remoteFileNames[c] != null) {
                AfterDownloadImpl after = new AfterDownloadImpl(remoteFileNames[c], output);
                output.append(International.getMessage("Starte Download von {file} ...", remoteFileNames[c]) + "\n");
                output.doLayout();
                if (DownloadThread.getFile(this, remoteFileNames[c], localFileNames[c], after)) {
                    // statted
                } else {
                    output.append(" " + International.getString("FEHLER") + "!");
                }
            }
        }
        output.append("\n");
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        if (!buttonClose) {
            buttonClose = true;
            closeButton.setIcon(getIcon(BaseDialog.IMAGE_CLOSE));
            runDownload();
            closeButton.setText(International.getString("Schließen"));
        } else {
            cancel();
        }
    }

}

class AfterDownloadImpl implements ExecuteAfterDownload {

    private String fname;
    private JTextArea out;
    public static int fileCount = 0;
    public static int errorCount = 0;

    public AfterDownloadImpl(String fname, JTextArea out) {
        this.fname = fname;
        this.out = out;
    }

    public void notify(boolean ok, String s) {
        out.append(s);
        try {
            Rectangle rect = out.getBounds();
            rect.y = (rect.height > 10 ? rect.height - 1 : 0);
            out.scrollRectToVisible(rect);
        } catch(Exception eignore) {
        }
        if (--fileCount == 0) {
            if (errorCount == 0) {
                Dialog.infoDialog(LogString.operationSuccessfullyCompleted(International.getString("Download")));
            } else {
                Dialog.infoDialog(LogString.operationFailed(International.getString("Download")));
            }
        }
    }

    public void success() {
        notify(true, LogString.operationSuccessfullyCompleted(
                International.getMessage("Download von {file}", fname)) + "\n");
    }

    public void failure(String text) {
        errorCount++;
        notify(false, LogString.operationFailed(
                International.getMessage("Download von {file}", fname), text) + "\n");
    }
}
