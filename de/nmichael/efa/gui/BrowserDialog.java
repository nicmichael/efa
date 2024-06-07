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
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.data.types.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.io.*;
import java.awt.print.*;
import java.net.*;
import java.beans.*;
import java.util.*;

// @i18n complete
public class BrowserDialog extends BaseDialog {

    JScrollPane htmlScrollPane = new JScrollPane();
    public JEditorPane html = new JEditorPane();
    JPanel northPanel = new JPanel();
    JPanel northWestPanel = new JPanel();
    JButton reloadButton = new JButton();
    JButton nextButton = new JButton();
    JButton backButton = new JButton();
    JPanel northCenterPanel = new JPanel();
    History history = null;
    History current = null;
    JButton printButton = new JButton();
    JButton pageButton = new JButton();
    JButton saveButton = new JButton();

    String url;
    boolean nopage = false;
    String lastDownload = null; // letzten Download merken um "doppelte" zu verhindern
    EfaBoathouseFrame efaBoathouseFrame; // only used for locking
    boolean locked = false; // wenn efa durch Anzeiges des Browsers gelocked ist und nur von Admin entsperrt werden kann
    String docText = null;
    Object highlightTag = null;
    int searchStart = 0;
    boolean fullScreenMode = false;
    JPanel southPanel = new JPanel();
    JPanel searchPanel = new JPanel();
    JLabel searchLabel = new JLabel();
    JTextField search = new JTextField();
    JButton searchPrevButton = new JButton();
    JButton searchNextButton = new JButton();
    JLabel sslLabel = new JLabel();

    private DownloadThread downloadThread;
    TimeoutThread timeoutThread;

    class TimeoutThread extends Thread {

        private BrowserDialog frame;
        private int timeout;
        private boolean locked;
        private boolean running = true;

        public TimeoutThread(BrowserDialog frame, int timeout, boolean locked) {
            this.frame = frame;
            this.timeout = timeout;
            this.locked = locked;
        }

        public void run() {
        	this.setName("BrowserDialog.TimeoutThread");
            try {
                if (!locked) {
                    Thread.sleep(timeout * 1000);
                } else {
                    if (Daten.efaConfig == null) {
                        running = false;
                        return;
                    }
                    GregorianCalendar now;
                    GregorianCalendar unlock;
                    do {
                        unlock = null;
                        DataTypeDate date = Daten.efaConfig.getValueEfaDirekt_lockEfaUntilDatum();
                        DataTypeTime time = Daten.efaConfig.getValueEfaDirekt_lockEfaUntilZeit();
                        if (date != null && date.isSet()) {
                            if (time != null && time.isSet()) {
                                unlock = new GregorianCalendar(
                                        date.getYear(), date.getMonth() - 1, date.getDay(),
                                        time.getHour(), time.getMinute());
                            } else {
                                unlock = new GregorianCalendar(
                                        date.getYear(), date.getMonth() - 1, date.getDay(),
                                        0, 0);
                            }
                        }
                        Thread.sleep(60 * 1000);
                        now = new GregorianCalendar();
                    } while ( (unlock == null || unlock.after(now)) &&
                               Daten.efaConfig.getValueEfaDirekt_locked());
                    unlock();
                }
            } catch (InterruptedException e) {
                running = false;
                return;
            }
            if (Dialog.frameCurrent() == frame) {
                running = false;
            	SwingUtilities.invokeLater(new Runnable() {
            	      public void run() {
                          frame.cancel();
            	      }
              	});
                //frame.cancel();
            }
        }

        public boolean isRunning() {
            return isAlive() && running;
        }
    }

    class History {

        String url = null;
        History next = null;
        History prev = null;
    }

    class LinkFollower implements HyperlinkListener {

        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if ((e.getURL().toString().toLowerCase().startsWith("http://") || e.getURL().toString().toLowerCase().startsWith("https://"))
                        && Daten.applID == Daten.APPL_EFABH && Daten.applMode == Daten.APPL_MODE_NORMAL) {
                    return;
                }
                if (e.getURL().toString().toLowerCase().startsWith("mailto:")
                        && Daten.applID == Daten.APPL_EFABH && Daten.applMode == Daten.APPL_MODE_NORMAL) {
                    return;
                }

                /*
                // Der folgende Code ist ein Workaround für Java-Bug-ID 6222200 (EditorPane GET instead of POST).
                // Zur Zeit funktioniert der Workaround allerdings noch nicht.
                try {
                if (Daten.javaVersion.startsWith("1.5") || Daten.javaVersion.startsWith("1.6")) {
                if (de.nmichael.efa.java15.Java15.editorPaneHandlePostEvent(out,e)) return;
                }
                } catch(UnsupportedClassVersionError ee) {
                }
                 */

                try {
                    URLConnection conn = e.getURL().openConnection();
                    if (conn == null || conn.getContentType() == null) {
                        html.setText(International.getString("FEHLER") + ": "
                                + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", e.getURL().toString(),
                                International.getString("Bist Du online?")));
                        return;
                    }
                    String surl = e.getURL().toString();
                    if (conn.getContentType().equals("text/html") || conn.getContentType().equals("text/plain")) {

                        // check for various email addresses
                        if (Daten.INTERNET_EFAMAIL != null) {
                            if (surl.toLowerCase().equals("mailto:" + Daten.EMAILINFO)) {
                                surl = HtmlFactory.createMailto(Daten.EMAILINFO);
                            }
                            if (surl.toLowerCase().equals("mailto:" + Daten.EMAILBUGS)) {
                                surl = HtmlFactory.createMailto(Daten.EMAILBUGS);
                            }
                            if (surl.toLowerCase().equals("mailto:" + Daten.EMAILHELP)) {
                                surl = HtmlFactory.createMailto(Daten.EMAILHELP);
                            }
                        }

                        if (surl.toLowerCase().startsWith("mailto:")) {
                            Dialog.error(International.getMessage("Bitte benutze ein externes email-Programm, um eine email an {receiver} zu verschicken!",
                                    surl.substring(7, surl.length())));
                        } else {
                            setNewPage(surl);
                        }
                    } else {
                        downloadUrl(conn, e.getURL().toString());
                    }
                } catch (IOException ee) {
                    html.setText(International.getString("FEHLER") + ": "
                            + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", e.getURL().toString(), ee.toString()) + "\n"
                            + International.getString("Eventuell wird efa durch eine Firewall blockiert.") + " "
                            + International.getString("Bitte prüfe Deine Firewall-Einstellungen und erlaube efa den Internet-Zugriff "
                            + "oder benutze einen normalen Webbrowser."));
                }
            }
        }
    }

    public BrowserDialog(Frame parent, String url) {
        super(parent,
                International.getStringWithMnemonic("Browser"),
                International.getStringWithMnemonic("Schließen"));
        this.url = url;
    }

    public BrowserDialog(JDialog parent, String url) {
        super(parent,
                International.getStringWithMnemonic("Browser"),
                International.getStringWithMnemonic("Schließen"));
        this.url = url;
    }

    public void setFullScreenMode(boolean fullScreenMode) {
        this.fullScreenMode = fullScreenMode;
    }

    public void setLocked(EfaBoathouseFrame efaBoathouseFrame, boolean locked) {
        this.efaBoathouseFrame = efaBoathouseFrame;
        this.locked = locked;
        if (locked) {
            _closeButtonText = null;
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());

        htmlScrollPane.setMinimumSize(new Dimension(300, 200));
        htmlScrollPane.setPreferredSize(new Dimension(600, 300));

        northPanel.setPreferredSize(new Dimension(500, 25));
        northPanel.setLayout(new BorderLayout());
        reloadButton.setPreferredSize(new Dimension(45, 22));
        reloadButton.setToolTipText(International.getString("Neu laden"));
        reloadButton.setIcon(BaseFrame.getIcon("browser_reload.gif"));
        reloadButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                reloadButton_actionPerformed(e);
            }
        });
        nextButton.setPreferredSize(new Dimension(45, 22));
        nextButton.setToolTipText(International.getString("Vorwärts"));
        nextButton.setIcon(BaseFrame.getIcon("browser_forward.gif"));
        nextButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                nextButton_actionPerformed(e);
            }
        });
        backButton.setPreferredSize(new Dimension(45, 22));
        backButton.setToolTipText(International.getString("Zurück"));
        backButton.setIcon(BaseFrame.getIcon("browser_back.gif"));
        backButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                backButton_actionPerformed(e);
            }
        });
        northCenterPanel.setLayout(new BorderLayout());
        northWestPanel.setMinimumSize(new Dimension(249, 25));
        northWestPanel.setPreferredSize(new Dimension(295, 25));
        northWestPanel.setLayout(new GridBagLayout());
        printButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                printButton_actionPerformed(e);
            }
        });
        printButton.setNextFocusableComponent(pageButton);
        printButton.setPreferredSize(new Dimension(45, 22));
        printButton.setToolTipText(International.getString("Drucken"));
        printButton.setIcon(BaseFrame.getIcon("browser_print.gif"));
        pageButton.setNextFocusableComponent(html);
        pageButton.setPreferredSize(new Dimension(45, 22));
        pageButton.setToolTipText(International.getString("Seite einrichten"));
        pageButton.setIcon(BaseFrame.getIcon("browser_printsetup.gif"));
        pageButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                pageButton_actionPerformed(e);
            }
        });
        saveButton.setNextFocusableComponent(printButton);
        saveButton.setPreferredSize(new Dimension(45, 22));
        saveButton.setToolTipText(International.getString("Seite speichern"));
        saveButton.setIcon(BaseFrame.getIcon("browser_save.gif"));
        saveButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveButton_actionPerformed(e);
            }
        });
        html.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                out_propertyChange(e);
            }
        });
        southPanel.setLayout(new BorderLayout());
        searchPanel.setLayout(new GridBagLayout());
        searchLabel.setLabelFor(search);
        Mnemonics.setLabel(this, searchLabel, "  " + International.getStringWithMnemonic("Suche") + ": ");
        search.setText("");
        Dialog.setPreferredSize(search, 300, 19);
        search.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                searchfor(e);
            }
        });
        searchNextButton.setPreferredSize(new Dimension(45, 22));
        searchNextButton.setIcon(BaseFrame.getIcon("browser_forward.gif"));
        searchNextButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                search_next(e);
            }
        });

        searchPrevButton.setPreferredSize(new Dimension(45, 22));
        searchPrevButton.setIcon(BaseFrame.getIcon("browser_back.gif"));
        searchPrevButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                search_prev(e);
            }
        });

        sslLabel.setIcon(BaseFrame.getIcon("browser_secure.gif"));
        sslLabel.setVisible(false);
        mainPanel.add(northPanel, BorderLayout.NORTH);
        northPanel.add(northWestPanel, BorderLayout.WEST);
        northWestPanel.add(backButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        northWestPanel.add(nextButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        northWestPanel.add(reloadButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        if (Daten.applID != Daten.APPL_EFABH || Daten.applMode == Daten.APPL_MODE_ADMIN) {
            northWestPanel.add(printButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            northWestPanel.add(pageButton, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            northWestPanel.add(saveButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        }
        northPanel.add(northCenterPanel, BorderLayout.CENTER);
        southPanel.add(searchPanel, BorderLayout.WEST);
        searchPanel.add(searchLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        searchPanel.add(search, new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        searchPanel.add(searchPrevButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        searchPanel.add(searchNextButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        southPanel.add(sslLabel, BorderLayout.EAST);
        mainPanel.add(htmlScrollPane, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        htmlScrollPane.getViewport().add(html, null);

        /*
        // Der folgende Code ist ein Workaround für Java-Bug-ID 6222200 (EditorPane GET instead of POST).
        // Zur Zeit funktioniert der Workaround allerdings noch nicht.
        try {
        if (Daten.javaVersion.startsWith("1.5") || Daten.javaVersion.startsWith("1.6")) de.nmichael.efa.java15.Java15.setEditorPaneAutoFormSubmissionFalse(out);
        } catch(UnsupportedClassVersionError e) {
        }
         */

        if (fullScreenMode) {
            this.setUndecorated(true);
            this.setResizable(false);
            setSize(Dialog.screenSize);
        }
        if (locked) {
            this.setTitle(Daten.EFA_LONGNAME);
            mainPanel.remove(this.northPanel);
            mainPanel.remove(this.southPanel);
        }
        html.addHyperlinkListener(new LinkFollower());
        html.setEditable(false);
        html.setContentType("text/html");
        setNewPage(url);
        html.requestFocus();
    }

    public void setClosingTimeout(int timeout) {
        if (timeout > 0) {
            timeoutThread = new TimeoutThread(this, timeout,
                    locked);
            timeoutThread.start();
        }
    }

    protected void processWindowEvent(WindowEvent e) {
        if (!locked) {
            super.processWindowEvent(e);
        }
    }

    void unlock() {
        locked = false;
        if (Daten.efaConfig != null) {
            Daten.efaConfig.setValueEfaDirekt_locked(false);
            Daten.efaConfig.setValueEfaDirekt_lockEfaUntilDatum(new DataTypeDate());
            Daten.efaConfig.setValueEfaDirekt_lockEfaUntilZeit(new DataTypeTime());

            //this.efaBoathouseFrame.setUnlocked();
        	SwingUtilities.invokeLater(new BthsSetUnlocked(efaBoathouseFrame));

            Logger.log(Logger.INFO,
                    Logger.MSG_EVT_UNLOCKED,
                    International.getString("efa ist wieder entsperrt und für die Benutzung freigegeben."));
        }
    }

    public boolean cancel() {
        if (_inCancel) {
            return false;
        }
        if (locked) {
            AdminRecord admin = AdminLoginDialog.login(this, International.getString("Entsperren von efa"));
            if (admin == null) {
                return false;
            }
            if (!admin.isAllowedLockEfa()) {
                Dialog.error(International.getMessage("Du hast als {user} nicht die Berechtigung, um die Funktion '{function}' auszuführen.",
                        admin.getName(), International.getString("Entsperren von efa")));
                return false;
            }
            unlock();
        }

        try {
            if (timeoutThread != null && timeoutThread.isRunning()) {
                timeoutThread.interrupt();
            }
        } catch (Exception e) {
        }

        return super.cancel();
    }

    void setPage(String url) {
        try {
            url = EfaUtil.correctUrl(url);
            html.setPage(url);
            this.url = url;
            updateSslLabel(url);
            nopage = false;
        } catch (IOException e) {
            html.setText(International.getString("FEHLER") + ": "
                    + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", url, e.toString()));
            nopage = true;
        } catch (Exception ee) {
            // abfangen von Java-Fehlern bei der Darstellung von HTML-Seiten
        }
        nextButton.setEnabled(current.next != null);
        backButton.setEnabled(current.prev != null);
    }

    void storePageInHistory(String url) {
        if (current == null) {
            history = new History();
            history.url = url;
            current = history;
        } else {
            if (current.url != null && current.url.equals(url)) {
                return;
            }
            History h = new History();
            h.url = url;
            h.prev = current;
            current.next = h;
            current = h;
        }
    }

    void setNewPage(String url) {
        if (url == null || url.length() == 0) {
            return;
        }
        url = EfaUtil.correctUrl(url);
        storePageInHistory(url);
        setPage(url);

        if (Dialog.tourRunning && Dialog.frameCurrent() != null) {
            Dialog.frameCurrent().requestFocus();
        }
    }

    void backButton_actionPerformed(ActionEvent e) {
        if (current.prev != null) {
            if (nopage) {
                try {
                    html.setPage(EfaUtil.correctUrl("file:" + HtmlFactory.createReload()));
                } catch (IOException ee) {
                }
            }
            current = current.prev;
            setPage(current.url);
        }
    }

    void nextButton_actionPerformed(ActionEvent e) {
        if (current.next != null) {
            current = current.next;
            setPage(current.url);
        }
    }

    void reloadButton_actionPerformed(ActionEvent e) {
        String pageShowing = url.trim();
        try {
            html.setPage(EfaUtil.correctUrl("file:" + HtmlFactory.createReload()));
        } catch (IOException ee) {
        }
        setPage(pageShowing);
    }

    void printButton_actionPerformed(ActionEvent e) {
        SimpleFilePrinter sfp = new SimpleFilePrinter(html);
        if (sfp.setupPageFormat()) {
            if (sfp.setupJobOptions()) {
                try {
                    sfp.printFile();
                } catch (Exception ee) {
                    Logger.log(Logger.ERROR, ee.toString());
                }
            }
        }
    }

    void pageButton_actionPerformed(ActionEvent e) {
        PageFormat defFormat = PrinterJob.getPrinterJob().defaultPage();
        PageFormat pf = PrinterJob.getPrinterJob().pageDialog(defFormat);
        if (pf != defFormat) {
            SimpleFilePrinter.pageSetup = pf;
        }
    }

    void saveButton_actionPerformed(ActionEvent e) {
        if (Daten.applID == Daten.APPL_EFABH && Daten.applMode == Daten.APPL_MODE_NORMAL) {
            return;
        }
        String quelle = this.url;
        if (!quelle.startsWith("file:")) {
            Dialog.infoDialog(International.getString("Fehler"),
                    International.getString("Es können nur lokale Seiten gespeichert werden!"));
            return;
        }
        quelle = quelle.substring(5, quelle.length());
        String ziel = Dialog.dateiDialog(this, International.getString("Seite speichern"),
                International.getString("HTML-Dateien"), "html", Daten.efaDataDirectory, true);
        if (ziel == null) {
            return;
        }

        int i = quelle.lastIndexOf(".");
        String ext = null;
        if (i > 0) {
            ext = quelle.substring(i + 1, quelle.length());
            if (ext.length() > 0 && !ziel.toLowerCase().endsWith(ext.toLowerCase())) {
                ziel += "." + ext;
            }
        }
        if (!EfaUtil.copyFile(quelle, ziel)) {
            Dialog.error(LogString.fileSavingFailed(ziel, International.getString("HTML-Seite")));
        } else {
            // Datei erfolgreich kopiert

            if (ziel.toLowerCase().endsWith(".html") || ziel.toLowerCase().endsWith(".htm")) {
                // Dennis will unbedingt, daß auch in HTML-Seiten eingebundene Bilder mit gespeichert werden.
                // Daher hier also eine Implementation, die extrem häßlich und nur auf die von efa erzeugten
                // HTML-Seiten zugeschnitten ist, aber immerhin ihren Zweck erfüllt... ;-)

                // Es wird in der Quelldatei nach der Zeichenkette "src=" gesucht (unabh. davon, ob sie in einem
                // <img>-Tag auftritt oder nicht) und kopiert diese Datei, sofern es eine "lokale" Datei ist, d.h.
                // der Pfag nicht mit "http://", mit "/" oder mit "." beginnt....

                String quelldir = EfaUtil.getPathOfFile(quelle);
                if (quelldir.length() > 0) {
                    quelldir += Daten.fileSep;
                }
                String zieldir = EfaUtil.getPathOfFile(ziel);
                if (zieldir.length() > 0) {
                    zieldir += Daten.fileSep;
                }

                BufferedReader f = null;
                try {
                    // Quelldatei lesen
                    f = new BufferedReader(new InputStreamReader(new FileInputStream(quelle), 
                            Daten.ENCODING_UTF)); // @todo (P9) save file in browser: get proper encoding
                    String s;
                    // Zeilenweise lesen
                    while ((s = f.readLine()) != null) {
                        i = -1;
                        do {
                            // Zeichenkette "src=" suchen
                            i = s.indexOf("src=", i + 1);
                            if (i >= 0) {
                                // img-Filename extrahieren
                                String img = s.substring(i + 4, s.length());
                                // Begrenzer des Filenamens ermitteln
                                if (img.length() > 0 && (img.charAt(0) == '"' || img.charAt(0) == '\'')) {
                                    char quote = img.charAt(0);
                                    img = img.substring(1, img.length());
                                    int endquote = img.indexOf(quote);
                                    if (endquote > 0) { // Ende der Anfüngrungsstriche gefunden?
                                        img = img.substring(0, endquote);
                                        if (!img.toLowerCase().startsWith("http://")
                                                && !img.startsWith("/") && !img.startsWith(".")) { // "lokale" Datei?
                                            if (!new File(zieldir + img).exists()) { // kopiere nur Dateien, die im Zielverzeichnis noch nicht existieren
                                                EfaUtil.copyFile(quelldir + img, zieldir + img);
                                            }
                                        }
                                    }
                                }
                            }
                        } while (i >= 0);
                    }
                } catch (Exception ee) {
                    Dialog.error(LogString.fileSavingFailed("...", International.getString("Bilder"), ee.toString()));

                } finally {
                    try {
                        f.close();
                    } catch (Exception eee) {
                        f = null;
                    }
                }
            }
        }
    }

    void downloadUrl(URLConnection conn, String fname) {
        // irgendwie passieren Downloads immer doppelt. Daher: Falls exakt derselbe
        // Download zum zweiten Mal, dann ignorieren
        if (lastDownload != null && lastDownload.equals(conn.toString())) {
            lastDownload = null;
            return;
        }
        lastDownload = conn.toString();


        try {
            conn.connect();

            String localName = Daten.efaTmpDirectory + fname.substring(fname.lastIndexOf("/") + 1, fname.length());

            String dat = Dialog.dateiDialog(this, International.getString("Download"),
                    null, null, localName, localName,
                    International.getString("Download speichern"), true, false);

            if (dat != null) {
                DownloadThread.runDownload(this, conn, fname, dat, false);
            }
        } catch (IOException e) {
            Dialog.error(LogString.operationFailed(International.getString("Download")) + ":\n" + e.toString() + "\n"
                    + International.getString("Eventuell wird efa durch eine Firewall blockiert."));
        }
    }

    void out_propertyChange(PropertyChangeEvent e) {
        try {
            storePageInHistory(html.getPage().toString());
            this.url = html.getPage().toString();
            updateSslLabel(html.getPage().toString());
        } catch (Exception ee) {
            EfaUtil.foo();
        }
    }

    void searchfor(KeyEvent e) {
        try {
            if (docText == null || docText.length() == 0) {
                docText = html.getDocument().getText(0, html.getDocument().getLength()).toLowerCase();
            }
            String s = search.getText().trim().toLowerCase();
            if (s.length() == 0) {
                html.getHighlighter().removeAllHighlights();
                highlightTag = null;
                return;
            }
            int pos = 0;
            if (searchStart <= 0) {
                pos = docText.indexOf(s);
            } else {
                pos = docText.substring(searchStart + 1).indexOf(s);
                if (pos >= 0) {
                    pos += searchStart + 1;
                }
            }
            if (pos >= 0) {
                html.select(pos, pos + s.length());
                if (highlightTag == null) {
                    highlightTag = html.getHighlighter().addHighlight(pos, pos + s.length(), new DefaultHighlighter.DefaultHighlightPainter(Color.yellow));
                }
                html.getHighlighter().changeHighlight(highlightTag, pos, pos + s.length());

            }
        } catch (Exception ee) {
        }
    }

    void search_prev(ActionEvent e) {
        searchStart = 0;
        searchfor(null);
    }

    void search_next(ActionEvent e) {
        searchStart = html.getSelectionStart();
        searchfor(null);
    }

    void updateSslLabel(String url) {
        sslLabel.setVisible(url.startsWith("https"));
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        setDialogResult(true);
        super.closeButton_actionPerformed(e);
    }

    public static BrowserDialog createBrowserDialog(Window parent,
            String title,
            String url,
            int width,
            int height,
            int closingTimeout,
            String closeButtonText,
            String closeButtonIcon
            ) {
        try {
            BrowserDialog dlg = null;
            if (parent != null && parent instanceof BaseDialog) {
                dlg = new BrowserDialog((BaseDialog) parent, url);
            }
            if (parent != null && parent instanceof BaseFrame) {
                dlg = new BrowserDialog((BaseFrame) parent, url);
            }
            if (dlg == null) {
                dlg = new BrowserDialog((BaseFrame) null, url);
            }
            if (width <= 0 || height <= 0) {
                width = (int) Dialog.screenSize.getWidth() - 100;
                height = (int) Dialog.screenSize.getHeight() - 150;
                if (Daten.applID == Daten.APPL_EFABH
                        && Daten.efaConfig.getValueEfaDirekt_startMaximized()) {
                    width = (int) Dialog.screenSize.getWidth();
                    height = (int) Dialog.screenSize.getHeight();
                }
            }
            dlg.setSize(width, height);
            dlg.setPreferredSize(new Dimension(width, height));
            dlg.setClosingTimeout(closingTimeout);
            if (title != null) {
                dlg.setTitle(title);
            }
            if (closeButtonText != null) {
                dlg._closeButtonText = closeButtonText;
            }
            if (closeButtonIcon != null) {
                dlg.setCloseButtonImage(closeButtonIcon);
            }
            dlg.showDialog();
            return dlg;
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public static String openInternalBrowser(Window parent,
            String title,
            String url,
            int width,
            int height,
            int closingTimeout) {
        BrowserDialog dlg = createBrowserDialog(parent, title, url, width, height, closingTimeout, null, null);
        if (dlg != null) {
            return (dlg.html.getPage() != null ? dlg.html.getPage().toString() : null);
        } else {
            return null;
        }
    }

    public static String openInternalBrowser(Window parent, String url) {
        return openInternalBrowser(parent, null, url, 0, 0, 0);
    }

    public static String openInternalBrowser(Window parent, String title, String url) {
        return openInternalBrowser(parent, title, url, 0, 0, 0);
    }

    public static String openInternalBrowser(Window parent, String title, String url, int width, int height) {
        return openInternalBrowser(parent, title, url, width, height, 0);
    }

    public static String openInternalBrowser(Window parent, String title, String url, int closingTimeout) {
        return openInternalBrowser(parent, title, url, 0, 0, closingTimeout);
    }

    public static boolean openInternalBrowserForAction(Window parent, String title, String url, String closeButtonText, String closeButtonImage) {
        BrowserDialog dlg = createBrowserDialog(parent, title, url, 0, 0, 0, closeButtonText, closeButtonImage);
        return dlg.getDialogResult();
    }

    public static void openExternalBrowser(Window parent, String url) {
        if (Daten.efaConfig.getValueBrowser().length() > 0 &&
            !Daten.efaConfig.getValueBrowser().equals("INTERN")) {
            try {
                Runtime.getRuntime().exec(Daten.efaConfig.getValueBrowser() + " " + url);
            } catch (Exception ee) {
                LogString.logWarning_cantExecCommand(Daten.efaConfig.getValueBrowser(), International.getString("für Browser"), ee.toString());
                openInternalBrowser(parent, url);
            }
        } else {
            openInternalBrowser(parent, url);
        }
    }
    
	class BthsSetUnlocked implements Runnable {
		
		private EfaBoathouseFrame myFrame = null;
		
    	public BthsSetUnlocked(EfaBoathouseFrame theFrame) {
    		myFrame=theFrame;
    	}
		
		public void run() {
            myFrame.setUnlocked();
		}
		
	}

}
