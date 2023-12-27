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

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLEditorKit;

public class HtmlPopupDialog extends BaseDialog {

    private String url;
    private int width;
    private int height;
    private int closeTimeoutSeconds;
    private boolean isClosed = false;

    public HtmlPopupDialog(String title, String url, String cmd, int width, int height, int closeTimeoutSeconds) {
        super((JDialog) null, title, International.getStringWithMnemonic("Schließen"));
        this.url = url;
        this.width = width;
        this.height = height;
        this.closeTimeoutSeconds = closeTimeoutSeconds;
        if (cmd != null && cmd.length() > 0) {
            execCommandBeforePopup(cmd);
        }
    }

    private void execCommandBeforePopup(String cmd) {
        cmd = cmd.trim();
        Logger.log(Logger.INFO, Logger.MSG_CORE_RUNNINGCOMMAND,
                International.getMessage("Starte Kommando: {cmd}", cmd));
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            if (p != null) {
                final Thread tcur = Thread.currentThread();
                new Thread() {

                    public void run() {
                        try {
                            Thread.sleep(10000);
                            tcur.interrupt();
                        } catch (Exception eignore) {
                        }
                    }
                }.start();
                try {
                    p.waitFor();
                } catch (InterruptedException eintr) {
                    Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTEXECCOMMAND,
                            LogString.cantExecCommand(cmd, International.getString("Kommando")));
                }
            }
        } catch (Exception ee) {
            Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTEXECCOMMAND,
                    LogString.cantExecCommand(cmd, International.getString("Kommando")));
        }
    }

    protected void iniDialog() throws Exception {
        JScrollPane scrollPane = new JScrollPane();
        JEditorPane htmlPane = new JEditorPane();
        mainPanel.setLayout(new BorderLayout());
        htmlPane.setContentType("text/html");
        if (Daten.isEfaFlatLafActive()) {
            htmlPane.putClientProperty("html.disable", Boolean.TRUE); 
        	htmlPane.setFont(htmlPane.getFont().deriveFont(Font.PLAIN,14));
        }

        htmlPane.setEditable(false);
        // following hyperlinks is automatically "disabled" (if no HyperlinkListener is taking care of it)
        // But we also need to disable submiting of form data:
        ((HTMLEditorKit) htmlPane.getEditorKit()).setAutoFormSubmission(false);
        try {
            if (url != null && url.length() > 0) {
                url = EfaUtil.correctUrl(url);
                htmlPane.setPage(url);
            }
        } catch (IOException ee) {
            htmlPane.setText(International.getString("FEHLER") + ": "
                    + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", url, ee.toString()));
        }

        if (width > 0 && height > 0) {
            scrollPane.setPreferredSize(new Dimension(width, height));
        }
        scrollPane.getViewport().add(htmlPane, null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        new Thread() {

            public void run() {
                try {
                	this.setName("HtmlPopupDialog.Inidialog.CancelThread");
                    Thread.sleep(closeTimeoutSeconds * 1000);

                    SwingUtilities.invokeLater(new Runnable() {
                	      public void run() {
                              cancel();
                	      }
                  	});

                } catch (Exception e) {
                }
            }
        }.start();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public boolean cancel() {
        if (!isClosed) {
            isClosed = true;
            return super.cancel();
        }
        return true;
    }
}
