/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.drv;

import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.data.efawett.EfaWettMeldung;
import de.nmichael.efa.data.efawett.EfaWett;
import de.nmichael.efa.data.efawett.ESigFahrtenhefte;
import de.nmichael.efa.gui.util.TableSorter;
import de.nmichael.efa.efa1.DatenFelder;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.io.*;
import java.util.*;
import java.beans.*;
import de.nmichael.efa.*;
import de.nmichael.efa.core.*;
import de.nmichael.efa.gui.BrowserDialog;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.net.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class MeldungenIndexFrame extends JDialog implements ActionListener {

    public static final int MELD_FAHRTENABZEICHEN = 1;
    public static final int MELD_WANDERRUDERSTATISTIK = 2;
    Frame parent;
    int MELDTYP;
    String WETTID;
    boolean firstclick = false;
    JPanel jPanel1 = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel northPanel = new JPanel();
    JPanel eastPanel = new JPanel();
    JPanel southPanel = new JPanel();
    JPanel centerPanel = new JPanel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabel1 = new JLabel();
    JComboBox meldungenAuswahl = new JComboBox();
    JScrollPane jScrollPane1 = new JScrollPane();
    JTable meldungen;
    BorderLayout borderLayout2 = new BorderLayout();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JButton closeButton = new JButton();
    JButton downloadButton = new JButton();
    JButton editButton = new JButton();
    JButton deleteButton = new JButton();
    JButton restoreButton = new JButton();
    JLabel jLabel2 = new JLabel();
    JLabel anzBestaetigte = new JLabel();
    JButton uploadButton = new JButton();
    JButton rejectButton = new JButton();
    JButton meldestatistikButton = new JButton();
    JButton checkFahrtenheftButton = new JButton();
    JButton printOverviewButton = new JButton();
    JButton exportButton = new JButton();
    JButton importButton = new JButton();
    JButton downloadOriginalButton = new JButton();
    JButton addButton = new JButton();

    public MeldungenIndexFrame(Frame parent, int meldTyp) {
        super(parent);
        this.MELDTYP = meldTyp;
        switch (meldTyp) {
            case MELD_FAHRTENABZEICHEN:
                WETTID = "DRV.FAHRTENABZEICHEN";
                break;
            case MELD_WANDERRUDERSTATISTIK:
                WETTID = "DRV.WANDERRUDERSTATISTIK";
                break;
        }
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        Dialog.frameOpened(this);
        try {
            jbInit();
            frameIni();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setTitle("Meldungen für das Jahr " + Main.drvConfig.aktJahr);
        EfaUtil.pack(this);
        this.parent = parent;
        // this.requestFocus();
    }

    // ActionHandler Events
    public void keyAction(ActionEvent evt) {
        if (evt == null || evt.getActionCommand() == null) {
            return;
        }
        if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
            cancel();
        }
    }

    // Initialisierung des Frames
    private void jbInit() throws Exception {
        ActionHandler ah = new ActionHandler(this);
        try {
            this.setTitle("Meldungen für das Jahr ????");
            ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                    new String[]{"ESCAPE", "F1"}, new String[]{"keyAction", "keyAction"});
            jPanel1.setLayout(borderLayout1);
            northPanel.setLayout(gridBagLayout1);
            jLabel1.setText("folgende Meldungen anzeigen: ");
            centerPanel.setLayout(borderLayout2);
            southPanel.setLayout(gridBagLayout2);
            eastPanel.setLayout(gridBagLayout3);
            closeButton.setText("Schließen");
            closeButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    closeButton_actionPerformed(e);
                }
            });
            downloadButton.setText("Meldungen aktualisieren");
            downloadButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    downloadButton_actionPerformed(e);
                }
            });
            editButton.setText("Meldung bearbeiten");
            editButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    editButton_actionPerformed(e);
                }
            });
            deleteButton.setText("Meldung lokal löschen");
            deleteButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    deleteButton_actionPerformed(e);
                }
            });
            restoreButton.setText("Meldung wiederherstellen");
            restoreButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    restoreButton_actionPerformed(e);
                }
            });
            jLabel2.setText("Zu bestätigende Meldungen: ");
            anzBestaetigte.setForeground(Color.blue);
            anzBestaetigte.setText("0");
            uploadButton.setText("Meldungen bestätigen");
            uploadButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    uploadButton_actionPerformed(e);
                }
            });
            rejectButton.setText("Meldung zurückweisen");
            rejectButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    rejectButton_actionPerformed(e);
                }
            });
            meldestatistikButton.setText("Meldestatistik erzeugen");
            meldestatistikButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    meldestatistikButton_actionPerformed(e);
                }
            });
            checkFahrtenheftButton.setText("Einzelnes eFahrtenheft prüfen");
            checkFahrtenheftButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    checkFahrtenheftButton_actionPerformed(e);
                }
            });
            printOverviewButton.setText("Übersicht drucken");
            printOverviewButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    printOverviewButton_actionPerformed(e);
                }
            });
            exportButton.setText("Meldung exportieren");
            exportButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    exportButton_actionPerformed(e);
                }
            });
            downloadOriginalButton.setText("Original-Meldungen herunterladen");
            downloadOriginalButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    downloadOriginalButton_actionPerformed(e);
                }
            });
            
            importButton.setText("Meldung importieren");
            importButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    importButton_actionPerformed(e);
                }
            });
            addButton.setText("Meldung von Hand erfassen");
            addButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    addButton_actionPerformed(e);
                }
            });
            this.getContentPane().add(jPanel1, BorderLayout.CENTER);
            jPanel1.add(northPanel, BorderLayout.NORTH);
            jPanel1.add(eastPanel, BorderLayout.EAST);
            jPanel1.add(southPanel, BorderLayout.SOUTH);
            jPanel1.add(centerPanel, BorderLayout.CENTER);
            northPanel.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            northPanel.add(meldungenAuswahl, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            centerPanel.add(jScrollPane1, BorderLayout.CENTER);
            southPanel.add(closeButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));


            eastPanel.add(downloadButton, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0), 0, 0));

            eastPanel.add(editButton, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            eastPanel.add(rejectButton, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            eastPanel.add(deleteButton, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            eastPanel.add(restoreButton, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0), 0, 0));
            
            eastPanel.add(addButton, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            eastPanel.add(importButton, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            eastPanel.add(exportButton, new GridBagConstraints(0, 7, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            eastPanel.add(downloadOriginalButton, new GridBagConstraints(0, 8, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            eastPanel.add(jLabel2, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(50, 0, 0, 0), 0, 0));
            eastPanel.add(anzBestaetigte, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(50, 0, 0, 0), 0, 0));
            eastPanel.add(uploadButton, new GridBagConstraints(0, 10, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            eastPanel.add(checkFahrtenheftButton, new GridBagConstraints(0, 11, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(50, 0, 0, 0), 0, 0));
            eastPanel.add(meldestatistikButton, new GridBagConstraints(0, 12, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(50, 0, 0, 0), 0, 0));
            eastPanel.add(printOverviewButton, new GridBagConstraints(0, 13, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            meldungenAuswahl.addItemListener(new java.awt.event.ItemListener() {

                public void itemStateChanged(ItemEvent e) {
                    meldungenAuswahl_itemStateChanged(e);
                }
            });
        } catch (NoSuchMethodException e) {
            System.err.println("Error setting up ActionHandler");
        }
    }

    /**Overridden so we can exit when window is closed*/
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            cancel();
        }
        super.processWindowEvent(e);
    }

    /**Close the dialog*/
    void cancel() {
        Dialog.frameClosed(this);
        dispose();
    }

    /**Close the dialog on a button event*/
    public void actionPerformed(ActionEvent e) {
    }

    void meldungenAuswahl_itemStateChanged(ItemEvent e) {
        showMeldungen();
    }

    void frameIni() {
        meldungenAuswahl.addItem("alle");
        meldungenAuswahl.addItem("unbearbeitete");
        meldungenAuswahl.addItem("bearbeitete");
        meldungenAuswahl.addItem("zurückgewiesene");
        meldungenAuswahl.addItem("gelöschte");
        meldungenAuswahl.setSelectedIndex(1);

        if (MELDTYP == MELD_WANDERRUDERSTATISTIK) {
            checkFahrtenheftButton.setVisible(false);
        }

        showMeldungen();
    }

    void showMeldungen() {
        if (meldungen != null) {
            jScrollPane1.getViewport().remove(meldungen);
        }

        Vector m = new Vector();
        int auswahl = meldungenAuswahl.getSelectedIndex();
        if (auswahl < 0) {
            auswahl = 0;
        }

        // zählen der Meldungen, für die eine Bestätigungsdatei vorliegt, die noch nicht hochgeladen wurde
        int countBestaetigung = 0;

        // Alle Meldungen einlesen und relevante Meldungen merken
        for (DatenFelder d = Main.drvConfig.meldungenIndex.getCompleteFirst(); d != null; d = Main.drvConfig.meldungenIndex.getCompleteNext()) {
            int status = EfaUtil.string2int(d.get(MeldungenIndex.STATUS), MeldungenIndex.ST_UNBEKANNT);
            if (d.get(MeldungenIndex.STATUS).equals(Integer.toString(MeldungenIndex.ST_BEARBEITET))
                    && (d.get(MeldungenIndex.BESTAETIGUNGSDATEI).length() > 0 && !MeldungenIndex.MANUELL_ERFASST.equals(d.get(MeldungenIndex.BESTAETIGUNGSDATEI)))) {
                countBestaetigung++;
            }
            if (auswahl == 0 || auswahl == status) {
                m.add(d);
            }
        }

        this.anzBestaetigte.setText(Integer.toString(countBestaetigung));
        this.uploadButton.setEnabled(countBestaetigung > 0);

        int numberOfColumns = 0;
        switch (this.MELDTYP) {
            case MELD_FAHRTENABZEICHEN:
                numberOfColumns = 7;
                break;
            case MELD_WANDERRUDERSTATISTIK:
                numberOfColumns = 6;
                break;
        }

        String[][] tableData = new String[m.size()][numberOfColumns];
        for (int i = 0; i < m.size(); i++) {
            int j = 0;
            tableData[i][j++] = ((DatenFelder) m.get(i)).get(MeldungenIndex.QNR);
            tableData[i][j++] = ((DatenFelder) m.get(i)).get(MeldungenIndex.VEREIN);
            tableData[i][j++] = ((DatenFelder) m.get(i)).get(MeldungenIndex.MITGLNR);
            tableData[i][j++] = ((DatenFelder) m.get(i)).get(MeldungenIndex.DATUM);
            int stat = EfaUtil.string2int(((DatenFelder) m.get(i)).get(MeldungenIndex.STATUS), MeldungenIndex.ST_UNBEKANNT);
            if (stat < 0 || stat >= MeldungenIndex.ST_NAMES.length) {
                stat = MeldungenIndex.ST_UNBEKANNT;
            }
            tableData[i][j++] = MeldungenIndex.ST_NAMES[stat];
            if (this.MELDTYP == this.MELD_FAHRTENABZEICHEN) {
                int fh = EfaUtil.string2int(((DatenFelder) m.get(i)).get(MeldungenIndex.FAHRTENHEFTE), MeldungenIndex.FH_UNBEKANNT);
                switch (fh) {
                    case MeldungenIndex.FH_UNBEKANNT:
                        tableData[i][j++] = "";
                        break;
                    case MeldungenIndex.FH_KEINE:
                        tableData[i][j++] = "keine";
                        break;
                    case MeldungenIndex.FH_PAPIER:
                        tableData[i][j++] = "Papier";
                        break;
                    case MeldungenIndex.FH_ELEKTRONISCH:
                        tableData[i][j++] = "nur elektr.";
                        break;
                    case MeldungenIndex.FH_PAPIER_UND_ELEKTRONISCH:
                        tableData[i][j++] = "elektr. / Papier";
                        break;
                    default:
                        tableData[i][j++] = "";
                }
            }
            String best = "";
            if (((DatenFelder) m.get(i)).get(MeldungenIndex.STATUS).equals(Integer.toString(MeldungenIndex.ST_BEARBEITET))) {
                if (((DatenFelder) m.get(i)).get(MeldungenIndex.BESTAETIGUNGSDATEI).length() == 0) {
                    best = "ja";
                } else {
                    if (MeldungenIndex.MANUELL_ERFASST.equals(((DatenFelder) m.get(i)).get(MeldungenIndex.BESTAETIGUNGSDATEI))) {
                        best = "n/a";
                    } else {
                        best = "noch nicht";
                    }
                }
            }
            tableData[i][j++] = best;
        }

        String[] tableHeaderFA = {"Quittungsnr.", "Verein", "Mitgliedsnr.", "Datum", "Status", "Fahrtenhefte", "Bestätigt"};
        String[] tableHeaderWS = {"Quittungsnr.", "Verein", "Mitgliedsnr.", "Datum", "Status", "Bestätigt"};
        String[] tableHeader = null;
        switch (this.MELDTYP) {
            case MELD_FAHRTENABZEICHEN:
                tableHeader = tableHeaderFA;
                break;
            case MELD_WANDERRUDERSTATISTIK:
                tableHeader = tableHeaderWS;
                break;
        }

        TableSorter sorter = new TableSorter(new DefaultTableModel(tableData, tableHeader));
        meldungen = new JTable(sorter);
        sorter.addMouseListenerToHeaderInTable(meldungen);
        meldungen.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                meldungen_mouseClicked(e);
            }
        });
        meldungen.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                meldungen_propertyChange(e);
            }
        });

        jScrollPane1.getViewport().add(meldungen, null);

        // intelligente Spaltenbreiten
        int width = meldungen.getSize().width;
        if (width < this.centerPanel.getSize().width - 20 || width > this.getSize().width) { // beim ersten Aufruf steht Tabellenbreite noch nicht (korrekt) zur Verfügung, daher dieser Plausi-Check
            width = this.centerPanel.getSize().width - 10;
        }
        for (int i = 0; i < numberOfColumns; i++) {
            TableColumn column = meldungen.getColumnModel().getColumn(i);
            switch (this.MELDTYP) {
                case MELD_FAHRTENABZEICHEN:
                    switch (i) {
                        case 0:
                            column.setPreferredWidth(13 * width / 100);
                            break; // QNr
                        case 1:
                            column.setPreferredWidth(24 * width / 100);
                            break; // Verein
                        case 2:
                            column.setPreferredWidth(10 * width / 100);
                            break; // Mitgliedsnr
                        case 3:
                            column.setPreferredWidth(17 * width / 100);
                            break; // Datum
                        case 4:
                            column.setPreferredWidth(13 * width / 100);
                            break; // Status
                        case 5:
                            column.setPreferredWidth(13 * width / 100);
                            break; // Fahrtenheft
                        case 6:
                            column.setPreferredWidth(10 * width / 100);
                            break; // Bestätigungsdatei
                    }
                    break;
                case MELD_WANDERRUDERSTATISTIK:
                    switch (i) {
                        case 0:
                            column.setPreferredWidth(17 * width / 100);
                            break; // QNr
                        case 1:
                            column.setPreferredWidth(32 * width / 100);
                            break; // Verein
                        case 2:
                            column.setPreferredWidth(11 * width / 100);
                            break; // Mitgliedsnr
                        case 3:
                            column.setPreferredWidth(17 * width / 100);
                            break; // Datum
                        case 4:
                            column.setPreferredWidth(13 * width / 100);
                            break; // Status
                        case 5:
                            column.setPreferredWidth(10 * width / 100);
                            break; // Bestätigungsdatei
                    }
                    break;
            }
        }
    }

    void closeButton_actionPerformed(ActionEvent e) {
        cancel();
    }

    void downloadButton_actionPerformed(ActionEvent e) {
        if (Main.drvConfig.efw_script == null || Main.drvConfig.efw_script.length() == 0) {
            Dialog.error("Kein EFW-Script konfiguriert. Bitte vervollständige zunächst die Konfiguration!");
            return;
        }
        if (Main.drvConfig.efw_user == null || Main.drvConfig.efw_user.length() == 0) {
            Dialog.error("Kein EFW-Nutzer konfiguriert. Bitte vervollständige zunächst die Konfiguration!");
            return;
        }
        if (Main.drvConfig.efw_password == null || Main.drvConfig.efw_password.length() == 0) {
            Dialog.error("Kein EFW-Paßwort konfiguriert. Bitte vervollständige zunächst die Konfiguration!");
            return;
        }
        if (Main.drvConfig.testmode) {
            if (Dialog.yesNoDialog("Testmodus",
                    "Du befindest Dich im Testmodus!\n"
                    + "Dieser Modus ist nur für Testzwecke gedacht.\n"
                    + "Möchtest Du weitermachen?") != Dialog.YES) {
                return;
            }
        }
        if (e != null && !Dialog.okAbbrDialog("Internet-Verbindung", "Bitte stelle eine Verbindung zum Internet her\nund klicke dann OK.")) {
            return;
        }

        String errorLog = "";

        Logger.log(Logger.INFO, "START Synchronisierung mit efaWett");
        String listFile = Daten.efaTmpDirectory + "meldungen.list";
        String url = Main.drvConfig.makeScriptRequestString(DRVConfig.ACTION_LIST, null, null, null, null, null, null);
        if ((new File(listFile)).exists() && !(new File(listFile)).delete()) {
            Dialog.error("Datei\n" + listFile + "\nkann nicht gelöscht werden.");
            Logger.log(Logger.ERROR, "Datei\n" + listFile + "\nkann nicht gelöscht werden.");
            Logger.log(Logger.INFO, "ENDE Synchronisierung mit efaWett");
            return;
        }
        if (!DownloadThread.getFile(this, url, listFile, true) || !EfaUtil.canOpenFile(listFile)) {
            Dialog.error("Download der Meldungen-Indexdatei fehlgeschlagen.");
            Logger.log(Logger.ERROR, "Download der Meldungen-Indexdatei fehlgeschlagen.");
            Logger.log(Logger.INFO, "ENDE Synchronisierung mit efaWett");
            return;
        }

        int count = 0;
        BufferedReader f = null;
        try {
            f = new BufferedReader(new InputStreamReader(new FileInputStream(listFile), Daten.ENCODING_ISO));
            String s;
            while ((s = f.readLine()) != null) {
                if (s.trim().length() == 0) {
                    continue;
                }
                if (s.startsWith("ERROR")) {
                    Dialog.error("Fehler beim Download:\n" + s);
                    Logger.log(Logger.ERROR, "Fehler beim Download: " + s);
                    Logger.log(Logger.INFO, "ENDE Synchronisierung mit efaWett");
                    f.close();
                    return;
                }
                Vector v = EfaUtil.split(s, '|');
                if (v.size() != 8) {
                    Dialog.error("Meldungen-Indexdatei hat ungültiges Format!");
                    Logger.log(Logger.ERROR, "Fehler beim Lesen der Meldungen-Indexdatei: Datei hat ungültiges Format (" + v.size() + " Felder).");
                    Logger.log(Logger.INFO, "ENDE Synchronisierung mit efaWett");
                    f.close();
                    return;
                }
                String wettid = (String) v.get(0);
                if (!wettid.equals(WETTID + "." + Main.drvConfig.aktJahr)) {
                    continue; // nur Meldungen des eingestellten Jahres abrufen
                }
                String qnr = (String) v.get(1);
                String mitglnr = (String)v.get(2);
                String status = (String) v.get(6);
                String edituuid = (String) v.get(7);
                if (edituuid == null || edituuid.length() < 10) {
                    edituuid = "0";
                }
                String fname = Main.drvConfig.meldungenIndex.getFileName();
                DatenFelder dtmp = Main.drvConfig.meldungenIndex.getExactComplete(qnr);
                boolean isRemoteEdited = status.equals("B");
                boolean isLocalEdited = dtmp != null && 
                        dtmp.get(MeldungenIndex.STATUS).equals(Integer.toString(MeldungenIndex.ST_BEARBEITET));
                int remoteStatus = (status.equals("B") ? MeldungenIndex.ST_BEARBEITET :
                                   (status.equals("Z") ? MeldungenIndex.ST_ZURUECKGEWIESEN : MeldungenIndex.ST_UNBEARBEITET));
                String localStatus = dtmp != null ? dtmp.get(MeldungenIndex.STATUS) : "-1";
                String localUUID = (dtmp == null || dtmp.get(MeldungenIndex.EDITUUID) == null ||
                                    dtmp.get(MeldungenIndex.EDITUUID).length() < 10 ? "0" : dtmp.get(MeldungenIndex.EDITUUID));
                
                Logger.log(Logger.INFO, "Meldung " + qnr + ": " +
                        "remote[edited=" + isRemoteEdited + ", status=" + remoteStatus + ", uuid=" + edituuid + "] " +
                        "local[edited=" + isLocalEdited + ", status=" + localStatus + ", uuid=" + localUUID + "]");
                
                if (remoteStatus == MeldungenIndex.ST_ZURUECKGEWIESEN && dtmp == null) {
                    // zurückgewiesene Meldung, die lokal nicht existiert
                    continue;
                }
                
                if (dtmp == null || 
                    dtmp.get(MeldungenIndex.STATUS).equals(Integer.toString(MeldungenIndex.ST_GELOESCHT)) ||
                    (remoteStatus != MeldungenIndex.ST_UNBEARBEITET && remoteStatus != Integer.parseInt(dtmp.get(MeldungenIndex.STATUS))) ||
                    (isRemoteEdited && !edituuid.equals(localUUID))) {
                    url = Main.drvConfig.makeScriptRequestString(DRVConfig.ACTION_GET, "item=" + qnr, "verein=" + (String) v.get(5), null, null, null, null);
                    String localFile = Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efw";
                    Logger.log(Logger.INFO, "Download der neuen Meldung " + qnr + " ...");
                    if (!DownloadThread.getFile(this, url, localFile, true) || !EfaUtil.canOpenFile(localFile)) {
                        errorLog += "Download der Meldung " + qnr + " fehlgeschlagen.\n";
                        Logger.log(Logger.ERROR, "Download der Meldung " + qnr + " fehlgeschlagen.");
                    } else {
                        boolean meldungOk = true;
                        boolean papierFahrtenhefteErforderlich = false;
                        boolean elektronischeFahrtenhefteVorhanden = false;
                        try {
                            EfaWett efw = new EfaWett(localFile);
                            if (!efw.readFile()) {
                                errorLog += "Heruntergeladene Meldung " + qnr + " kann nicht gelesen werden.";
                                Logger.log(Logger.ERROR, "Heruntergeladene Meldung " + qnr + " kann nicht gelesen werden.");
                                meldungOk = false;
                            } else {
                                if (remoteStatus == MeldungenIndex.ST_BEARBEITET) {
                                    efw.durchDRVbearbeitet = true; // we want to have the file as it was edited by another DRV person
                                } else {
                                    efw.resetDrvIntern(); // Interne Felder entsprechend zurücksetzen (zur Sicherheit)
                                }
                                
                                // korrigiere Mitgliedsnr
                                if (efw.verein_mitglnr == null || efw.verein_mitglnr.trim().length() == 0 ||
                                    (efw.verein_mitglnr.length() < 5 && mitglnr != null && mitglnr.length() == 5)) {
                                    efw.verein_mitglnr = mitglnr;
                                }
                                
                                for (EfaWettMeldung ew = efw.meldung; ew != null; ew = ew.next) {
                                    if (ew.drv_fahrtenheft == null || ew.drv_fahrtenheft.length() == 0) {
                                        if (ew.drv_anzAbzeichen != null && ew.drv_anzAbzeichen.length() > 0
                                                && EfaUtil.string2int(ew.drv_anzAbzeichen, 0) > 0) {
                                            papierFahrtenhefteErforderlich = true;
                                        }
                                    }
                                    if (ew.drv_fahrtenheft != null) {
                                        elektronischeFahrtenhefteVorhanden = true;
                                    }
                                }
                                if (!efw.writeFile()) {
                                    errorLog += "Heruntergeladene Meldung " + qnr + " kann nicht geschrieben werden.";
                                    Logger.log(Logger.ERROR, "Heruntergeladene Meldung " + qnr + " kann nicht geschrieben werden.");
                                }
                            }
                        } catch (IOException ee) {
                            errorLog += "Heruntergeladene Meldung " + qnr + " kann nicht gelesen werden.";
                            Logger.log(Logger.ERROR, "Heruntergeladene Meldung " + qnr + " kann nicht gelesen werden: " + ee.toString());
                            meldungOk = false;
                        }
                        if (meldungOk) {
                            String newstatus;
                            if (status.equals("W")) {
                                newstatus = Integer.toString(MeldungenIndex.ST_UNBEARBEITET);
                            } else if (status.equals("I")) {
                                newstatus = Integer.toString(MeldungenIndex.ST_UNBEARBEITET);
                            } else if (status.equals("B")) {
                                newstatus = Integer.toString(MeldungenIndex.ST_BEARBEITET);
                            } else if (status.equals("Z")) {
                                newstatus = Integer.toString(MeldungenIndex.ST_ZURUECKGEWIESEN);
                            } else {
                                newstatus = Integer.toString(MeldungenIndex.ST_UNBEKANNT);
                            }
                            
                            if (isLocalEdited &&
                                !edituuid.equals(localUUID)) {
                                if (Dialog.yesNoDialog("Warnung", 
                                        "Lokal bearbeitete Meldung " + qnr + " (" + v.get(4) + ") wurde auf dem Server verändert.\n" +
                                        "Soll die lokale Kopie verworfen und durch Meldung vom Server ersetzt werden?") != Dialog.YES) {
                                    continue;
                                }
                            }
                            
                            // first delete all old submissions by this club
                            for (DatenFelder d = Main.drvConfig.meldungenIndex.getCompleteFirst();
                                    d != null; d = Main.drvConfig.meldungenIndex.getCompleteNext()) {
                                if (mitglnr.equals(d.get(MeldungenIndex.MITGLNR))) {
                                    Main.drvConfig.meldungenIndex.delete(d.get(MeldungenIndex.QNR));
                                }
                            }
                            
                            DatenFelder d = new DatenFelder(MeldungenIndex._ANZFELDER);
                            d.set(MeldungenIndex.QNR, qnr);
                            d.set(MeldungenIndex.VEREIN, (String) v.get(4));
                            d.set(MeldungenIndex.MITGLNR, (String) mitglnr);
                            d.set(MeldungenIndex.DATUM, (String) v.get(3));
                            d.set(MeldungenIndex.STATUS, newstatus);
                            d.set(MeldungenIndex.EDITUUID, edituuid);
                            if (papierFahrtenhefteErforderlich && !elektronischeFahrtenhefteVorhanden) {
                                d.set(MeldungenIndex.FAHRTENHEFTE, Integer.toString(MeldungenIndex.FH_PAPIER));
                            } else if (!papierFahrtenhefteErforderlich && elektronischeFahrtenhefteVorhanden) {
                                d.set(MeldungenIndex.FAHRTENHEFTE, Integer.toString(MeldungenIndex.FH_ELEKTRONISCH));
                            } else if (papierFahrtenhefteErforderlich && elektronischeFahrtenhefteVorhanden) {
                                d.set(MeldungenIndex.FAHRTENHEFTE, Integer.toString(MeldungenIndex.FH_PAPIER_UND_ELEKTRONISCH));
                            } else if (!papierFahrtenhefteErforderlich && !elektronischeFahrtenhefteVorhanden) {
                                d.set(MeldungenIndex.FAHRTENHEFTE, Integer.toString(MeldungenIndex.FH_KEINE));
                            } else {
                                d.set(MeldungenIndex.FAHRTENHEFTE, Integer.toString(MeldungenIndex.FH_UNBEKANNT));
                            }

                            Main.drvConfig.meldungenIndex.add(d);
                            count++;
                            Logger.log(Logger.INFO, "Meldung " + qnr + " erfolgreich heruntergeladen.");
                        }
                    }
                }
            }
            f.close();
        } catch (Exception ee) {
            Dialog.error("Konnte Meldungs-Indexdatei nicht laden:\n" + ee.getMessage());
            Logger.log(Logger.ERROR, "Konnte Meldungs-Indexdatei nicht laden:\n" + ee.getMessage());
            return;
        }

        if (!Main.drvConfig.meldungenIndex.writeFile()) {
            errorLog += "Meldungen-Indexdatei konnte nicht gespeichert werden.\n";
            Logger.log(Logger.ERROR, "Meldungen-Indexdatei konnte nicht gespeichert werden.");
        }

        if (errorLog.length() > 0) {
            Dialog.error("Folgende Fehler sind aufgetreten:\n\n" + errorLog);
        }
        showMeldungen();
        Dialog.infoDialog(count + " neue Meldungen heruntergeladen!");
        Logger.log(Logger.INFO, count + " neue Meldungen heruntergeladen.");
        Logger.log(Logger.INFO, "ENDE Synchronisierung mit efaWett");
    }

    void exportField(BufferedWriter f, String field, String label) throws Exception {
        if (field != null) {
            f.write(label + ";" + field + "\n");
        }
    }

    void exportButton_actionPerformed(ActionEvent e) {
        int row = meldungen.getSelectedRow();
        if (row < 0) {
            Dialog.error("Bitte wähle zunächst eine Meldung zum Exportieren aus!");
            return;
        }
        String qnr = null;
        try {
            qnr = (String) meldungen.getValueAt(row, 0);
        } catch (Exception ee) {
            return;
        }
        if (qnr == null) {
            return;
        }

        DatenFelder d = Main.drvConfig.meldungenIndex.getExactComplete(qnr);
        if (d == null) {
            Dialog.error("Meldung mit Quittungsnummer " + qnr + " nicht gefunden.");
            return;
        }
        EfaWett efw = new EfaWett(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efw");
        String csv = Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".csv";
        try {
            if (efw.readFile()) {
                BufferedWriter f = new BufferedWriter(new FileWriter(csv));
                exportField(f, efw.allg_wett, "Wettbewerb");
                exportField(f, efw.allg_wettjahr, "Jahr");
                f.write("\n");
                exportField(f, efw.verein_name, "Verein");
                exportField(f, efw.verein_ort, "Ort");
                exportField(f, efw.verein_lrv, "Bundesland");
                exportField(f, efw.verein_mitglnr, "Mitgliedsnummer");
                exportField(f, efw.verein_mitglieder, "Mitglieder");
                exportField(f, efw.verein_mitgl_in, "Verbände");
                exportField(f, efw.verein_user, "Benutzername");
                f.write("\n");
                exportField(f, efw.meld_name, "Meldende Person");
                exportField(f, efw.meld_email, "email");
                exportField(f, efw.meld_kto, "Konto");
                exportField(f, efw.meld_blz, "BLZ");
                exportField(f, efw.meld_bank, "Bank");
                f.write("\n");
                exportField(f, efw.versand_name, "Versand an");
                exportField(f, efw.versand_zusatz, "Adreßzusatz");
                exportField(f, efw.versand_strasse, "Straße");
                exportField(f, efw.versand_ort, "Ort");
                f.write("\n");
                exportField(f, efw.drv_nadel_erw_silber, "Bestellung Nadel Erwachsene Silber");
                exportField(f, efw.drv_nadel_erw_gold, "Bestellung Nadel Erwachsene Gold");
                exportField(f, efw.drv_nadel_jug_silber, "Bestellung Nadel Jugendliche Silber");
                exportField(f, efw.drv_nadel_jug_gold, "Bestellung Nadel Jugendliche Gold");
                exportField(f, efw.drv_stoff_erw, "Bestellung Stoffabzeichen Erwachsene");
                exportField(f, efw.drv_stoff_jug, "Bestellung Stoffabzeichen Jugendliche");
                f.write("\n");
                exportField(f, efw.wimpel_mitglieder, "Gewertete Mitglieder für Blauer Wimpel");
                exportField(f, efw.wimpel_km, "Kilometer für Blauer Wimpel");
                exportField(f, efw.wimpel_schnitt, "Durchschnitt für Blauer Wimpel");
                f.write("\n");
                f.write("\n");

                boolean teilnHeader = true;
                if (WettDefs.STR_DRV_WANDERRUDERSTATISTIK.equals(efw.allg_wett)) {
                    f.write("Start und Ziel;Gewässer;Kilometer;Tage;Teilnehmer;Mannsch-Km;Männer Anz;Männer Km;Junioren Anz;Junioren Km;Frauen Anz;Frauen Km;Juniorinnen Anz;Juniorinnen Km\n");
                    teilnHeader = false;
                }
                int i = 0;
                for (EfaWettMeldung m = efw.meldung; m != null; m = m.next) {
                    if (teilnHeader) {
                        f.write("Meldung " + (++i) + "\n");
                    }
                    exportField(f, m.nachname, "Nachname");
                    exportField(f, m.vorname, "Vorname");
                    exportField(f, m.jahrgang, "Jahrgang");
                    exportField(f, m.geschlecht, "Geschlecht");
                    exportField(f, m.gruppe, "Gruppe");
                    exportField(f, m.kilometer, "Kilometer");
                    exportField(f, m.restkm, "Rest-Km");
                    exportField(f, m.anschrift, "Anschrift");
                    exportField(f, m.abzeichen, "Abzeichen");
                    exportField(f, m.drv_anzAbzeichen, "Abzeichen bisher");
                    exportField(f, m.drv_gesKm, "Kilometer bisher");
                    exportField(f, m.drv_anzAbzeichenAB, "davon Abzeichen A/B bisher");
                    exportField(f, m.drv_gesKmAB, "davon Kilometer A/B bisher");
                    exportField(f, m.drv_fahrtenheft, "Elektronisches Fahrtenheft");
                    exportField(f, m.drv_aequatorpreis, "Äquatorpreis");
                    if (m.drvWS_StartZiel != null) {
                        f.write(m.drvWS_LfdNr + ";"
                                + m.drvWS_StartZiel + ";"
                                + m.drvWS_Gewaesser + ";"
                                + m.drvWS_Km + ";"
                                + m.drvWS_Tage + ";"
                                + m.drvWS_Teilnehmer + ";"
                                + m.drvWS_MannschKm + ";"
                                + m.drvWS_MaennerAnz + ";"
                                + m.drvWS_MaennerKm + ";"
                                + m.drvWS_JuniorenAnz + ";"
                                + m.drvWS_JuniorenKm + ";"
                                + m.drvWS_FrauenAnz + ";"
                                + m.drvWS_FrauenKm + ";"
                                + m.drvWS_JuniorinnenAnz + ";"
                                + m.drvWS_JuniorinnenKm + "\n");
                    }
                    for (int x = 0; m.fahrt != null && x < m.fahrt.length; x++) {
                        String s = "";
                        for (int y = 0; m.fahrt[x] != null && y < m.fahrt[x].length; y++) {
                            if (m.fahrt[x][y] != null) {
                                s += ";" + m.fahrt[x][y];
                            }
                        }
                        if (s.length() > m.fahrt[x].length) {
                            f.write("Fahrt " + (x + 1) + s + "\n");
                        }
                    }
                    if (teilnHeader) {
                        f.write("\n");
                    }
                }
                f.close();
                Dialog.infoDialog("Meldung exportiert", "Die Meldung wurde erfolgreich exportiert:\n" + csv);
            }
        } catch (Exception ee) {
            Dialog.error("Fehler beim Exportieren der Meldung: " + ee.getMessage());
        }
    }

    void editButton_actionPerformed(ActionEvent e) {
        int row = meldungen.getSelectedRow();
        if (row < 0) {
            Dialog.error("Bitte wähle zunächst eine Meldung zum Bearbeiten aus!");
            return;
        }
        String qnr = null;
        try {
            qnr = (String) meldungen.getValueAt(row, 0);
        } catch (Exception ee) {
            return;
        }
        if (qnr == null) {
            return;
        }

        DatenFelder d = Main.drvConfig.meldungenIndex.getExactComplete(qnr);
        if (d == null) {
            Dialog.error("Meldung mit Quittungsnummer " + qnr + " nicht gefunden.");
            return;
        }
        switch (EfaUtil.string2int(d.get(MeldungenIndex.STATUS), -1)) {
            case MeldungenIndex.ST_GELOESCHT:
                Dialog.error("Die gewählte Meldung wurde bereits gelöscht und kann nicht mehr bearbeitet werden.");
                return;
            case MeldungenIndex.ST_ZURUECKGEWIESEN:
                if (Dialog.yesNoCancelDialog("Meldung bereits zurückgewiesen",
                        "Die gewählte Meldung wurde bereits zurückgewiesen.\n"
                        + "Nur in Ausnahmefällen, etwa bei dem versehentlichen Zurückweisen einer Meldung, sollte dies rückgängig\n"
                        + "gemacht werden. Anschließend kann die Meldung neu bearbeitet oder bestätigt werden.\n"
                        + "Möchtest Du das Zurückweisen der Meldung jetzt rückgängig machen?") == Dialog.YES) {
                    if ((new File(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efwsig")).exists()) {
                        d.set(MeldungenIndex.STATUS, Integer.toString(MeldungenIndex.ST_BEARBEITET));
                        if (!MeldungenIndex.MANUELL_ERFASST.equals(d.get(MeldungenIndex.BESTAETIGUNGSDATEI))) {
                            d.set(MeldungenIndex.BESTAETIGUNGSDATEI, Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efwsig");
                        }
                        Logger.log(Logger.INFO, "Zurückweisen von Meldung " + qnr + " rückgängig gemacht. Meldung hat jetzt den Status 'bearbeitet'!");
                        Dialog.infoDialog("Zurückweisen rückgängig gemacht",
                                "Die Meldung hat jetzt den Status 'bearbeitet' und sollte erneut bestätigt werden!");
                    } else {
                        d.set(MeldungenIndex.STATUS, Integer.toString(MeldungenIndex.ST_UNBEARBEITET));
                        Logger.log(Logger.INFO, "Zurückweisen von Meldung " + qnr + " rückgängig gemacht. Meldung hat jetzt den Status 'unbearbeitet'!");
                        Dialog.infoDialog("Zurückweisen rückgängig gemacht",
                                "Die Meldung hat jetzt den Status 'unbearbeitet' und kann neu bearbeitet werden!");
                    }
                    if (!Main.drvConfig.meldungenIndex.writeFile()) {
                        Logger.log(Logger.ERROR, "Meldungen-Indexdatei konnte nicht geschrieben werden!");
                    }
                    showMeldungen();
                }
                return;
        }

        EfaWett efw = new EfaWett(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efw");
        try {
            if (efw.readFile()) {
                if (MELDTYP == MELD_FAHRTENABZEICHEN && !Main.drvConfig.readOnlyMode) {
                    if (Main.drvConfig.keyPassword == null) {
                        KeysAdminFrame.enterKeyPassword();
                    }
                    if (Main.drvConfig.keyPassword == null) {
                        return;
                    }
                    if (!loadKeys()) {
                        return;
                    }
                }

                MeldungEditFrame dlg = new MeldungEditFrame(this, efw, qnr, MELDTYP);
                Dialog.setDlgLocation(dlg, this);
                dlg.setModal(true);
                dlg.show();
            } else {
                Dialog.error("Fehler beim Lesen der Meldungsdatei.");
            }
        } catch (Exception eee) {
            Dialog.error("Fehler beim Lesen der Meldungsdatei: " + eee.toString());
        }
        showMeldungen();
    }

    // Edit bei Doppelklick
    void meldungen_mouseClicked(MouseEvent e) {
        firstclick = true;
    }

    // komisch, manchmal scheint diese Methode irgendwie nicht zu ziehen.....
    void meldungen_propertyChange(PropertyChangeEvent e) {
        if (meldungen.isEditing()) {
            if (firstclick) {
                firstclick = false;
                editButton_actionPerformed(null);
            }
        }
    }

    void deleteButton_actionPerformed(ActionEvent e) {
        if (Main.drvConfig.readOnlyMode) {
            Dialog.error("Diese Funktion ist in diesem Modus nicht erlaubt.");
            return;
        }
        int row = meldungen.getSelectedRow();
        if (row < 0) {
            Dialog.error("Bitte wähle zunächst eine Meldung zum Löschen aus!");
            return;
        }
        String qnr = null;
        try {
            qnr = (String) meldungen.getValueAt(row, 0);
        } catch (Exception ee) {
            return;
        }
        if (qnr == null) {
            return;
        }
        if (Dialog.yesNoDialog("Meldung löschen", "Möchtest Du die Meldung mit Quittungsnummer " + qnr + " wirklich löschen?") != Dialog.YES) {
            return;
        }

        DatenFelder d = Main.drvConfig.meldungenIndex.getExactComplete(qnr);
        if (d == null) {
            Dialog.error("Meldung mit Quittungsnummer " + qnr + " nicht gefunden.");
            return;
        }
        if (EfaUtil.string2int(d.get(MeldungenIndex.STATUS), -1) != MeldungenIndex.ST_UNBEARBEITET
                && EfaUtil.string2int(d.get(MeldungenIndex.STATUS), -1) != MeldungenIndex.ST_ZURUECKGEWIESEN) {
            if (Dialog.yesNoDialog("Meldung wirklich löschen",
                    "Nur unbearbeitete oder zurückgewiesene Meldungen sollten gelöscht werden.\n"
                    + "Eine Meldung, die bereits bearbeitet wurde, sollte nur in Ausnahmefällen\n"
                    + "gelöscht werden (bspw. wenn sie wegen fehlerhafter Bearbeitung erneut aus\n"
                    + "dem Internet heruntergeladen werden soll in der Form, in der sie vom\n"
                    + "Verein eingeschickt wurde).\n"
                    + "Möchtest Du die Meldung wirklich löschen?") != Dialog.YES) {
                return;
            }
        }

        d.set(MeldungenIndex.STATUS, Integer.toString(MeldungenIndex.ST_GELOESCHT));
        if (!Main.drvConfig.meldungenIndex.writeFile()) {
            Logger.log(Logger.ERROR, "Meldungen-Indexdatei konnte nicht geschrieben werden!");
        }
        Logger.log(Logger.INFO, "Meldung " + qnr + " gelöscht!");
        showMeldungen();
    }

    void restoreButton_actionPerformed(ActionEvent e) {
        if (Main.drvConfig.readOnlyMode) {
            Dialog.error("Diese Funktion ist in diesem Modus nicht erlaubt.");
            return;
        }
        int row = meldungen.getSelectedRow();
        if (row < 0) {
            Dialog.error("Bitte wähle zunächst eine Meldung zum Wiederherstellen aus!");
            return;
        }
        String qnr = null;
        try {
            qnr = (String) meldungen.getValueAt(row, 0);
        } catch (Exception ee) {
            return;
        }
        if (qnr == null) {
            return;
        }
        DatenFelder d = Main.drvConfig.meldungenIndex.getExactComplete(qnr);
        if (d == null) {
            return;
        }
        String mitglnr = d.get(MeldungenIndex.MITGLNR);
        String status = d.get(MeldungenIndex.STATUS);
        String verein = d.get(MeldungenIndex.VEREIN);
        if (Dialog.yesNoDialog("Meldung wiederherstellen", "Möchtest Du die Originalmeldung zu Quittungsnummer " + qnr + " wirklich wiederherstellen?\n"+
                "Alle vom DRV vorgenommen Änderungen gehen dabei verloren!") != Dialog.YES) {
            return;
        }
        String url = Main.drvConfig.makeScriptRequestString(DRVConfig.ACTION_GETORIG, "item=" + qnr, "verein=" + verein, null, null, null, null);
        String localFile = Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efw";
        String localRestoreFile = Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efw-restore";
        Logger.log(Logger.INFO, "Download der Meldung " + qnr + " für Wiederherstellung ...");
        if (!DownloadThread.getFile(this, url, localRestoreFile, true) || !EfaUtil.canOpenFile(localRestoreFile)) {
            Dialog.error("Wiederherstellung der Meldung " + qnr + " fehlgeschlagen.");
            Logger.log(Logger.ERROR, "Download der Meldung " + qnr + " fehlgeschlagen.");
            return;
        } else {
            try {
                EfaWett efw = new EfaWett(localRestoreFile);
                StringBuilder serr = new StringBuilder();
                if (!efw.readFile()) {
                    try {
                        BufferedReader f = new BufferedReader(new FileReader(localRestoreFile));
                        String s;
                        while ( (s = f.readLine()) != null) {
                            serr.append((s.length() > 0 ? "\n" : "") + s);
                        }
                        f.close();
                    } catch(Exception ee) {
                        serr.append(ee.toString());
                    }
                    Dialog.error("Heruntergeladene Meldung " + qnr + " kann nicht gelesen werden:\n" + serr);
                    Logger.log(Logger.ERROR, "Heruntergeladene Meldung " + qnr + " kann nicht gelesen werden: " + serr);
                    return;
                } else {
                    efw.durchDRVbearbeitet = true;

                    // korrigiere Mitgliedsnr
                    if (efw.verein_mitglnr == null || efw.verein_mitglnr.trim().length() == 0
                            || (efw.verein_mitglnr.length() < 5 && mitglnr != null && mitglnr.length() == 5)) {
                        efw.verein_mitglnr = mitglnr;
                    }
                    if (Integer.toString(MeldungenIndex.ST_BEARBEITET).equals(status)) {
                        // markiere alle Teilnehmer als "wird gewertet"
                        for (EfaWettMeldung ew = efw.meldung; ew != null; ew = ew.next) {
                            ew.drvint_wirdGewertet = true;
                        }
                    }
                    if (!efw.writeFile()) {
                        Dialog.error("Heruntergeladene Meldung " + qnr + " kann nicht geschrieben werden.");
                        Logger.log(Logger.ERROR, "Heruntergeladene Meldung " + qnr + " kann nicht geschrieben werden.");
                        return;
                    }
                }
            } catch (IOException ee) {
                Dialog.error("Heruntergeladene Meldung " + qnr + " kann nicht gelesen werden.");
                Logger.log(Logger.ERROR, "Heruntergeladene Meldung " + qnr + " kann nicht gelesen werden: " + ee.toString());
                return;
            }
        }
        if (EfaUtil.renameFile(localRestoreFile, localFile, ".bak")) {
            /*
            // Nein, wir lassen der Status der Meldung unverändert; schließlich hat sich
            // ja ihr Status auf dem DRV-Server auch nicht verändert!
            d.set(MeldungenIndex.STATUS, Integer.toString(MeldungenIndex.ST_UNBEARBEITET));
            Main.drvConfig.meldungenIndex.delete(qnr);
            Main.drvConfig.meldungenIndex.add(d);
            if (!Main.drvConfig.meldungenIndex.writeFile()) {
                 Logger.log(Logger.ERROR, "Meldungen-Indexdatei konnte nicht geschrieben werden!");
             }
             */
            Logger.log(Logger.INFO, "Meldung " + qnr + " wiederhergestellt!");
            Dialog.infoDialog("Meldung " + qnr + " wiederhergestellt!");
        } else {
            Logger.log(Logger.ERROR, "Meldung " + qnr + " konnte nicht wiederhergestellt werden: Umbenennen fehlgeschlagen");
            Dialog.error("Meldung " + qnr + " konnte nicht wiederhergestellt werden: Umbenennen fehlgeschlagen");
        }
        showMeldungen();
    }

    void downloadOriginalButton_actionPerformed(ActionEvent e) {
        if (Main.drvConfig.efw_script == null || Main.drvConfig.efw_script.length() == 0) {
            Dialog.error("Kein EFW-Script konfiguriert. Bitte vervollständige zunächst die Konfiguration!");
            return;
        }
        if (Main.drvConfig.efw_user == null || Main.drvConfig.efw_user.length() == 0) {
            Dialog.error("Kein EFW-Nutzer konfiguriert. Bitte vervollständige zunächst die Konfiguration!");
            return;
        }
        if (Main.drvConfig.efw_password == null || Main.drvConfig.efw_password.length() == 0) {
            Dialog.error("Kein EFW-Paßwort konfiguriert. Bitte vervollständige zunächst die Konfiguration!");
            return;
        }
        if (e != null && !Dialog.okAbbrDialog("Internet-Verbindung", "Bitte stelle eine Verbindung zum Internet her\nund klicke dann OK.")) {
            return;
        }
        try {
            String dir = Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep +
                    "orig_" + EfaUtil.getCurrentTimeStampYYYYMMDD_HHMMSS() + Daten.fileSep;
            if (!(new File(dir).mkdir())) {
                Dialog.error("Fehler beim Erstellen von Verzeichnis " + dir);
                return;
            }
            DatenFelder d = Main.drvConfig.meldungenIndex.getCompleteFirst();
            int c = 0;
            while (d != null) {
                EfaWett etmp = new EfaWett(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + d.get(MeldungenIndex.QNR) + ".efw");
                if (etmp.readFile()) {
                    String qnr = d.get(MeldungenIndex.QNR);
                    String verein = etmp.verein_user;
                    if (qnr != null && qnr.length() > 0 && verein != null && verein.length() > 0) {
                        String url = Main.drvConfig.makeScriptRequestString(DRVConfig.ACTION_GETORIG, "item=" + qnr, "verein=" + verein, null, null, null, null);
                        String localRestoreFile = dir + qnr + ".efw";
                        if (!DownloadThread.getFile(this, url, localRestoreFile, true) || !EfaUtil.canOpenFile(localRestoreFile)) {
                            Dialog.error("Download der Meldung " + qnr + " fehlgeschlagen.");
                        } else {
                            c++;
                        }
                    }
                }                
                d = Main.drvConfig.meldungenIndex.getCompleteNext();
            }
            Dialog.infoDialog(c + " Original-Meldungen heruntergeladen und gespeichert in\n" + dir);
        } catch (Exception ex) {
            Dialog.error("Fehler beim Herunterladen: " + e);
        }
    }
    
    void uploadButton_actionPerformed(ActionEvent e) {
        if (Main.drvConfig.readOnlyMode) {
            Dialog.error("Diese Funktion ist in diesem Modus nicht erlaubt.");
            return;
        }
        ArrayList<DatenFelder> bestaetigungen = new ArrayList<DatenFelder>();
        for (DatenFelder d = Main.drvConfig.meldungenIndex.getCompleteFirst(); d != null; d = Main.drvConfig.meldungenIndex.getCompleteNext()) {
            if (d.get(MeldungenIndex.STATUS).equals(Integer.toString(MeldungenIndex.ST_BEARBEITET))
                    && (d.get(MeldungenIndex.BESTAETIGUNGSDATEI).length() > 0 && !MeldungenIndex.MANUELL_ERFASST.equals(d.get(MeldungenIndex.BESTAETIGUNGSDATEI)))) {
                bestaetigungen.add(d);
                /*
                if (MELDTYP == MELD_FAHRTENABZEICHEN) {
                    bestaetigungen.add(d.get(MeldungenIndex.BESTAETIGUNGSDATEI));
                }
                if (MELDTYP == MELD_WANDERRUDERSTATISTIK) {
                    bestaetigungen.add(EfaUtil.getPathOfFile(Main.drvConfig.meldungenIndex.getFileName()) + Daten.fileSep + d.get(MeldungenIndex.QNR) + ".efw");
                }
                */
            }
        }
        if (bestaetigungen.size() == 0) {
            Dialog.error("Es liegen keine Meldungen zum Bestätigen vor.");
            return;
        }
        boolean einzeln = false;
        switch (Dialog.auswahlDialog("Meldungen bestätigen",
                "Es liegen " + bestaetigungen.size() + " Meldungen zum Bestätigen vor.\n"
                + "Möchtest Du jetzt alle Meldungen oder nur einzelne Meldungen bestätigen?",
                "Alle bestätigen", "Einzeln bestätigen", true)) {
            case 0:
                einzeln = false;
                break;
            case 1:
                einzeln = true;
                break;
            default:
                return;
        }
        if (!Dialog.okAbbrDialog("Internet-Verbindung", "Bitte stelle eine Verbindung zum Internet her\nund klicke dann OK.")) {
            return;
        }

        Logger.log(Logger.INFO, "START Meldungen bestätigen");
        String errors = "";
        int cok = 0;
        for (int i = 0; i < bestaetigungen.size(); i++) {
            DatenFelder d = bestaetigungen.get(i);
            String filename = null;
            if (MELDTYP == MELD_FAHRTENABZEICHEN) {
                filename = d.get(MeldungenIndex.BESTAETIGUNGSDATEI);
            }
            if (MELDTYP == MELD_WANDERRUDERSTATISTIK) {
                filename = EfaUtil.getPathOfFile(Main.drvConfig.meldungenIndex.getFileName()) + Daten.fileSep + d.get(MeldungenIndex.QNR) + ".efw";
            }
            if (filename == null || !EfaUtil.canOpenFile(filename)) {
                Logger.log(Logger.ERROR, "(Bestätigungs-)Datei " + filename + " existiert nicht.");
                errors += "(Bestätigungs-)Datei " + filename + " existiert nicht!\n";
                continue;
            }

            String verein = null;
            String vereinsname = null;
            String qnr = null;
            String wettid = WETTID + "." + Main.drvConfig.aktJahr;
            String edituuid = d.get(MeldungenIndex.EDITUUID);
            if (edituuid == null || edituuid.length() < 2) {
                edituuid = UUID.randomUUID().toString();
            }
            try {
                int teilcnt = 0;
                if (MELDTYP == MELD_FAHRTENABZEICHEN) {
                    ESigFahrtenhefte esfh = new ESigFahrtenhefte(filename);
                    if (!esfh.readFile()) {
                        errors += "Bestätigungsdatei " + filename + " kann nicht gelesen werden!\n";
                        Logger.log(Logger.ERROR, "Bestätigungsdatei " + filename + " kann nicht gelesen werden.");
                        continue;
                    }
                    verein = esfh.verein_user;
                    vereinsname = esfh.verein_name;
                    qnr = esfh.quittungsnr;
                    teilcnt = esfh.getFahrtenhefte().size();
                }

                if (MELDTYP == MELD_WANDERRUDERSTATISTIK) {
                    EfaWett efw = new EfaWett(filename);
                    if (!efw.readFile()) {
                        errors += "Meldungsdatei " + filename + " kann nicht gelesen werden!\n";
                        Logger.log(Logger.ERROR, "Meldungsdatei " + filename + " kann nicht gelesen werden.");
                        continue;
                    }
                    verein = efw.verein_user;
                    vereinsname = efw.verein_name;
                    qnr = EfaUtil.getFilenameWithoutPath(filename);
                    qnr = qnr.substring(0, qnr.length() - 4);
                }

                if (einzeln && Dialog.yesNoDialog("Meldung " + qnr + " bestätigen",
                        "Soll die Meldung " + qnr + " des Vereins\n"
                        + vereinsname + " bestätigt werden?") != Dialog.YES) {
                    continue;
                }

                String data = "";
                if (MELDTYP == MELD_FAHRTENABZEICHEN) {
                    BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Daten.ENCODING_ISO));
                    String z;
                    while ((z = f.readLine()) != null) {
                        z = EfaUtil.replace(z, "\"", "**2**", true); // " als **2** maskieren
                        z = EfaUtil.replace(z, "=", "**0**", true);  // = als **0** maskieren
                        z = EfaUtil.replace(z, "&", "**6**", true);  // & als **6** maskieren
                        data += z + "**#**"; // Zeilenumbrüche als **#** maskieren
                    }
                    f.close();
                }
                if (MELDTYP == MELD_WANDERRUDERSTATISTIK) {
                    data = "##DRV.WANDERRUDERSTATISTIK##";
                }

                String changeddata = "";
                String meldfile = filename;
                if (filename.endsWith("sig")) {
                    meldfile = meldfile.substring(0, meldfile.length()-3);
                }
                if (EfaUtil.canOpenFile(meldfile)) {
                    BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(meldfile), Daten.ENCODING_ISO));
                    String z;
                    while ((z = f.readLine()) != null) {
                        z = EfaUtil.replace(z, "\"", "**2**", true); // " als **2** maskieren
                        z = EfaUtil.replace(z, "=", "**0**", true);  // = als **0** maskieren
                        z = EfaUtil.replace(z, "&", "**6**", true);  // & als **6** maskieren
                        changeddata += z + "**#**"; // Zeilenumbrüche als **#** maskieren
                    }
                    f.close();
                    EfaWett efw = new EfaWett(meldfile);
                    if (efw.readFile()) {
                        // over-ride teilcnt: we're interested in the total number of teilnehmer, not just those who fulfilled
                        teilcnt = efw.getNumberOfMeldungen();
                    }
                }
                
                String request = Main.drvConfig.makeScriptRequestString(DRVConfig.ACTION_UPLOAD, 
                        "verein=" + verein, 
                        "qnr=" + qnr, 
                        "wettid=" + wettid, 
                        "data=" + data, 
                        "changeddata=" + changeddata,
                        "edituuid=" + edituuid);
                int pos = request.indexOf("?");
                if (pos < 0) {
                    Logger.log(Logger.ERROR, "efaWett-Anfrage für Bestätigungsdatei " + filename + " kann nicht erstellt werden.");
                    errors += "efaWett-Anfrage für Bestätigungsdatei " + filename + " kann nicht erstellt werden.\n";
                    continue;
                }
                String url = request.substring(0, pos);
                String content = request.substring(pos + 1, request.length());

                HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
                conn.setRequestMethod("POST");
                conn.setAllowUserInteraction(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-length", Integer.toString(content.length()));
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(content);
                out.flush();
                out.close();
                conn.disconnect();
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                boolean ok = true;
                int okcnt = 0;
                String z;
                while ((z = in.readLine()) != null) {
                    if (z.startsWith("OK:")) {
                        okcnt = EfaUtil.stringFindInt(z, 0);
                    } else {
                        Logger.log(Logger.ERROR, "Fehler beim Bestätigen von Meldung " + qnr + ": " + z);
                        errors += "Fehler beim Bestätigen von Meldung " + qnr + ": " + z + "\n";
                        ok = false;
                    }
                }
                String entry = (MELDTYP == MELD_FAHRTENABZEICHEN ? "Teilnehmer" : "Fahrten");
                if (MELDTYP == MELD_FAHRTENABZEICHEN && teilcnt != okcnt) {
                    Logger.log(Logger.ERROR, "Fehler beim Bestätigen von Meldung " + qnr + ": " + 
                            teilcnt + " " + entry + " gesendet, aber nur " + okcnt + " " + entry + " empfangen.");
                    Logger.log(Logger.ERROR, "request: " + request);
                    Logger.log(Logger.ERROR, "url: " + url);
                    Logger.log(Logger.ERROR, "content: " + content);
                    if (EfaUtil.canOpenFile(meldfile)) {
                        BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(meldfile), Daten.ENCODING_ISO));
                        StringBuilder zz = new StringBuilder();
                        while ((z = f.readLine()) != null) {
                            zz.append(z + "\n");
                        }
                        f.close();
                        Logger.log(Logger.RAW, meldfile + ":\n" + zz.toString());
                    }
                    errors += "Fehler beim Bestätigen von Meldung " + qnr + ": " + 
                            teilcnt + " " + entry + " gesendet, aber nur " + okcnt + " " + entry + " empfangen\n";
                    ok = false;
                }
                if (ok) {
                    d.set(MeldungenIndex.BESTAETIGUNGSDATEI, "");
                    d.set(MeldungenIndex.EDITUUID, edituuid);
                    Main.drvConfig.meldungenIndex.delete(qnr);
                    Main.drvConfig.meldungenIndex.add(d);
                    Logger.log(Logger.INFO, "Meldung " + qnr + " von Verein " + verein + " wurde erfolgreich bestätigt (" + 
                            teilcnt + " " + entry + " gesendet, " + okcnt + " " + entry + " empfangen).");
                    cok++;
                }
            } catch (Exception ee) {
                Logger.log(Logger.ERROR, "Fehler beim Bestätigen von Meldung " + qnr + ": " + ee.toString());
                errors += "Fehler beim Bestätigen von Meldung " + qnr + ": " + ee.getMessage() + "\n";
            }
        }
        if (!Main.drvConfig.meldungenIndex.writeFile()) {
            errors += "Meldungen-Indexdatei konnte nicht geschrieben werden!\n";
            Logger.log(Logger.ERROR, "Meldungen-Indexdatei konnte nicht geschrieben werden!");
        }
        if (errors.length() > 0) {
            Dialog.error("Bei der Bestätigung der Meldungen traten folgende Fehler auf:\n" + errors);
        }
        Dialog.infoDialog("Meldungen bestätigt", "Es wurden " + cok + " Meldungen erfolgreich bestätigt.");
        Logger.log(Logger.INFO, "ENDE Meldungen bestätigen");
        showMeldungen();
    }

    void rejectButton_actionPerformed(ActionEvent e) {
        if (Main.drvConfig.readOnlyMode) {
            Dialog.error("Diese Funktion ist in diesem Modus nicht erlaubt.");
            return;
        }
        int row = meldungen.getSelectedRow();
        if (row < 0) {
            Dialog.error("Bitte wähle zunächst eine Meldung zum Zurückweisen aus!");
            return;
        }
        String qnr = null;
        try {
            qnr = (String) meldungen.getValueAt(row, 0);
        } catch (Exception ee) {
            return;
        }
        if (qnr == null) {
            return;
        }

        DatenFelder d = Main.drvConfig.meldungenIndex.getExactComplete(qnr);
        if (d == null) {
            Dialog.error("Meldung mit Quittungsnummer " + qnr + " nicht gefunden.");
            return;
        }
        if (EfaUtil.string2int(d.get(MeldungenIndex.STATUS), -1) != MeldungenIndex.ST_UNBEARBEITET) {
            if (EfaUtil.string2int(d.get(MeldungenIndex.STATUS), -1) == MeldungenIndex.ST_ZURUECKGEWIESEN) {
                Dialog.error("Die Meldung wurde bereits zurückgewiesen. Sie kann nicht nochmals zurückgewiesen werden.");
                return;
            }
            if (Dialog.yesNoDialog("Meldung wirklich zurückweisen",
                    "Nur unbearbeitete Meldungen sollten zurückgewiesen werden.\n"
                    + "Eine Meldung, die bereits bearbeitet wurde, sollte nur in Ausnahmefällen\n"
                    + "zurückgewiesen werden (etwa nach Rücksprache mit dem Verein, falls bspw. eine\n"
                    + "korrigierte Meldung eingeschickt werden soll).\n"
                    + "Möchtest Du die Meldung wirklich zurückweisen?") != Dialog.YES) {
                return;
            }
        }

        String verein = null;
        EfaWett ew = new EfaWett(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efw");
        try {
            if (ew.readFile()) {
                verein = ew.verein_user;
            } else {
                Dialog.error("Fehler beim Lesen der Meldungsdatei.");
                return;
            }
        } catch (Exception eee) {
            Dialog.error("Fehler beim Lesen der Meldungsdatei: " + eee.toString());
            return;
        }
        if (verein == null || verein.length() == 0) {
            Dialog.error("Konnte Vereinsnamen aus Meldedatei nicht ermitteln!");
            return;
        }

        if (Dialog.yesNoDialog("Meldung zurückweisen", "Möchtest Du die Meldung " + qnr + " des Vereins " + verein + " wirklich zurückweisen?") != Dialog.YES) {
            return;
        }
        String grund;
        do {
            grund = Dialog.inputDialog("Grund für Zurückweisen",
                    "Bitte gib den Grund ein, warum die Meldung zurückgewiesen wurde.\n"
                    + "Dieser wird dem Verein per email mitgeteilt.");
            if (grund == null) {
                return;
            }
        } while (grund.length() == 0);
        if (d == null) {
            Dialog.error("Meldung mit Quittungsnummer " + qnr + " nicht gefunden.");
            return;
        }

        if (!Dialog.okAbbrDialog("Internet-Verbindung", "Bitte stelle eine Verbindung zum Internet her\nund klicke dann OK.")) {
            return;
        }

        String url = Main.drvConfig.makeScriptRequestString(DRVConfig.ACTION_REJECT, "verein=" + verein, "qnr=" + qnr, "grund=" + EfaUtil.replace(grund, " ", "+", true), null, null, null);
        String localFile = Daten.efaTmpDirectory + "efwstatus.tmp";
        if (!DownloadThread.getFile(this, url, localFile, true) || !EfaUtil.canOpenFile(localFile)) {
            Logger.log(Logger.ERROR, "Zurückweisen der Meldung " + qnr + " von Verein " + verein + " fehlgeschlagen: Kann efaWett nicht erreichen");
            Dialog.error("Aktion fehlgeschlagen: Kann efaWett nicht erreichen");
            return;
        }
        try {
            BufferedReader f = new BufferedReader(new FileReader(localFile));
            String s = f.readLine();
            if (s == null || !s.equals("OK")) {
                Logger.log(Logger.ERROR, "Zurückweisen der Meldung " + qnr + " von Verein " + verein + " fehlgeschlagen: " + s);
                Dialog.error("Aktion fehlgeschlagen: " + s);
                return;
            }
            f.close();
            EfaUtil.deleteFile(localFile);
        } catch (Exception ee) {
            Logger.log(Logger.ERROR, "Zurückweisen der Meldung " + qnr + " von Verein " + verein + " fehlgeschlagen: " + ee.getMessage());
            Dialog.error("Aktion fehlgeschlagen: " + ee.getMessage());
            return;
        }

        d.set(MeldungenIndex.STATUS, Integer.toString(MeldungenIndex.ST_ZURUECKGEWIESEN));
        if (!MeldungenIndex.MANUELL_ERFASST.equals(d.get(MeldungenIndex.BESTAETIGUNGSDATEI))) {
            d.set(MeldungenIndex.BESTAETIGUNGSDATEI, "");
        }
        if (!Main.drvConfig.meldungenIndex.writeFile()) {
            Logger.log(Logger.ERROR, "Meldungen-Indexdatei konnte nicht geschrieben werden!");
        }
        Logger.log(Logger.INFO, "Meldung " + qnr + " von Verein " + verein + " zurückgewiesen. Grund: " + grund);
        Dialog.infoDialog("Meldung zurückgewiesen!");
        showMeldungen();
    }

    void meldestatistikButton_actionPerformed(ActionEvent e) {
        if (MELDTYP == MELD_FAHRTENABZEICHEN) {
            updateMeldestatistikFA();
            createMeldestatistikFA();
        }
        if (MELDTYP == MELD_WANDERRUDERSTATISTIK) {
            updateMeldestatistikWS(Main.drvConfig.meldungenIndex, true);
            createMeldestatistikWS();
        }
    }
    
    void updateMeldestatistikFA() {
        int countClubs = 0;
        int countPersons = 0;
        int countUnbearbeitet = 0;
        int countInvalid = 0;
        Hashtable<String,String> keyHash = new Hashtable<String,String>();
        ArrayList<String> clubsWithoutPersons = new ArrayList<String>();
        ArrayList<String> conflicts = new ArrayList<String>();
        
        Main.drvConfig.meldestatistik = new Meldestatistik(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + DRVConfig.MELDESTATISTIK_FA_FILE);
        for (DatenFelder list = Main.drvConfig.meldungenIndex.getCompleteFirst(); list != null; list = Main.drvConfig.meldungenIndex.getCompleteNext()) {
            int status = EfaUtil.string2int(list.get(MeldungenIndex.STATUS), MeldungenIndex.ST_UNBEKANNT);
            if (list.get(MeldungenIndex.STATUS).equals(Integer.toString(MeldungenIndex.ST_BEARBEITET))) {
                EfaWett ew = new EfaWett(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + list.get(MeldungenIndex.QNR) + ".efw");
                String mitglnr = list.get(MeldungenIndex.MITGLNR);
                String qnr = list.get(MeldungenIndex.QNR);
                if (qnr == null || qnr.length() == 0) {
                    qnr = "unbekannt";
                }
                String club = list.get(MeldungenIndex.VEREIN);
                if (club == null || club.length() == 0) {
                    club = "unbekannt";
                }
                try {
                    if (ew.readFile()) {
                        countClubs++;
                        int personsPerClub = 0;
                        for (EfaWettMeldung m = ew.meldung; m != null; m = m.next) {
                            if (m.drvint_wirdGewertetExplizitGesetzt && !m.drvint_wirdGewertet) {
                                continue;
                            }
                            // aktuelle Anzahl der Abzeichen
                            int anzAbz = EfaUtil.string2int(m.drv_anzAbzeichen, 0);
                            int anzAbzAB = EfaUtil.string2int(m.drv_anzAbzeichenAB, 0);
                            int gesKm = EfaUtil.string2int(m.drv_gesKm, 0);
                            int gesKmAB = EfaUtil.string2int(m.drv_gesKmAB, 0);
                            String gruppe = MeldungEditFrame.getGruppe(ew, m);
                            anzAbz++;
                            gesKm += EfaUtil.string2int(m.kilometer, 0);
                            boolean isAB = gruppe != null && (gruppe.startsWith("3a") || gruppe.startsWith("3b"));
                            if (isAB) {
                                anzAbzAB++;
                                gesKmAB += EfaUtil.string2int(m.kilometer, 0);
                            }
                            if (ew.verein_mitglnr == null || ew.verein_mitglnr.trim().length() == 0) {
                                ew.verein_mitglnr = mitglnr;
                                if (ew.verein_mitglnr == null) {
                                    ew.verein_mitglnr = "";
                                }
                            }
                            String key = ew.verein_mitglnr + "#" + m.vorname + "#" + m.nachname + "#" + m.jahrgang;
                            if (keyHash.get(key) == null && ew.verein_mitglnr.length() > 0) {
                                keyHash.put(key, key);
                            } else {
                                conflicts.add(ew.verein_mitglnr + ": " + 
                                        m.vorname + " " + m.nachname + " (" + m.jahrgang + ") - " + qnr + ": " + club);
                                continue;
                            }
                            DatenFelder d = new DatenFelder(Meldestatistik._ANZFELDER);
                            d.set(Meldestatistik.KEY, key);
                            d.set(Meldestatistik.VEREINSMITGLNR, ew.verein_mitglnr);
                            d.set(Meldestatistik.VEREIN, ew.verein_name);
                            d.set(Meldestatistik.VORNAME, m.vorname);
                            d.set(Meldestatistik.NACHNAME, m.nachname);
                            d.set(Meldestatistik.JAHRGANG, m.jahrgang);
                            d.set(Meldestatistik.GESCHLECHT, m.geschlecht);
                            d.set(Meldestatistik.KILOMETER, m.kilometer);
                            d.set(Meldestatistik.GRUPPE, m.gruppe);
                            d.set(Meldestatistik.ANZABZEICHEN, Integer.toString(anzAbz));
                            d.set(Meldestatistik.ANZABZEICHENAB, Integer.toString(anzAbzAB));
                            d.set(Meldestatistik.GESKM, Integer.toString(gesKm));
                            if (m.drv_aequatorpreis != null && EfaUtil.string2int(m.drv_aequatorpreis, 0) > 0) { // old:  && (m.drv_aequatorpreis.equals("1") || m.drv_aequatorpreis.equals("3"))
                                d.set(Meldestatistik.AEQUATOR, m.drv_aequatorpreis);
                            }
                            Main.drvConfig.meldestatistik.add(d);
                            countPersons++;
                            personsPerClub++;
                        }
                        if (personsPerClub == 0) {
                            clubsWithoutPersons.add(ew.verein_mitglnr + " (" + qnr + ": " + club + ")");
                        }
                    }
                } catch (Exception e) {
                    Logger.log(e);
                    Dialog.error("Fehler beim Erstellen der Meldestatistik: " + e);
                }
            } else {
                if (list.get(MeldungenIndex.STATUS).equals(Integer.toString(MeldungenIndex.ST_UNBEARBEITET))) {
                    countUnbearbeitet++;
                } else {
                    countInvalid++;
                }
            }
        }
        Main.drvConfig.meldestatistik.writeFile();
        Dialog.infoDialog("Fahrtenabzeichen-Meldestatistik für " + countClubs + " Meldungen (" +
                countPersons + " Personen) aktualisiert." +
                (countUnbearbeitet + countInvalid > 0 ? 
                    "\n" + countUnbearbeitet + " unbearbeitete und " + countInvalid + " gelöschte oder zurückgewiesene Meldungen wurden ignoriert."
                : "") +
                (clubsWithoutPersons.size() > 0 ? 
                    "\nFür folgende Vereine wurden keine wertbaren Teilnehmer gefunden:\n" +
                    EfaUtil.vector2string(clubsWithoutPersons, "\n")
                : "") +
                (conflicts.size() > 0 ? 
                    "\nFolgende Meldungen wurden aufgrund ungültiger Mitgliedsnummern ignoriert:\n" +
                    EfaUtil.vector2string(conflicts, "\n")
                : ""));
        
        
    }
    
    void updateMeldestatistikWS(MeldungenIndex idx, boolean showConfirmation) {
        int countClubs = 0;
        int countSessions = 0;
        int countUnbearbeitet = 0;
        int countInvalid = 0;
        Hashtable<String,String> clubIdHash = new Hashtable<String,String>();
        ArrayList<String> conflicts = new ArrayList<String>();
        
        Main.drvConfig.meldestatistik = new Meldestatistik(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + DRVConfig.MELDESTATISTIK_WS_FILE);
        for (DatenFelder list = idx.getCompleteFirst(); list != null; list = idx.getCompleteNext()) {
            String mitglnr = list.get(MeldungenIndex.MITGLNR);
            String qnr = list.get(MeldungenIndex.QNR);
            if (qnr == null || qnr.length() == 0) {
                qnr = "unbekannt";
            }
            String club = list.get(MeldungenIndex.VEREIN);
            if (club == null || club.length() == 0) {
                club = "unbekannt";
            }
            int status = EfaUtil.string2int(list.get(MeldungenIndex.STATUS), MeldungenIndex.ST_UNBEKANNT);
            if (list.get(MeldungenIndex.STATUS).equals(Integer.toString(MeldungenIndex.ST_BEARBEITET))) {
                EfaWett ew = new EfaWett(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + list.get(MeldungenIndex.QNR) + ".efw");
                try {
                    if (ew.readFile()) {
                        
                        Vector gewaesser = new Vector();
                        int teilnehmer = 0;
                        int maennerkm = 0;
                        int juniorenkm = 0;
                        int frauenkm = 0;
                        int juniorinnenkm = 0;
                        for (EfaWettMeldung m = ew.meldung; m != null; m = m.next) {
                            if (m.drvint_wirdGewertetExplizitGesetzt && !m.drvint_wirdGewertet) {
                                continue;
                            }
                            countSessions++;
                            
                            Vector g = null;
                            if (m.drvWS_Gewaesser != null && m.drvWS_Gewaesser.length() > 0) {
                                g = EfaUtil.split(EfaUtil.replace(m.drvWS_Gewaesser, ";", ",", true), ',');
                            }
                            for (int j = 0; g != null && j < g.size(); j++) {
                                if (!gewaesser.contains(g.get(j))) {
                                    gewaesser.add(g.get(j));
                                }
                            }
                            teilnehmer += EfaUtil.string2int(m.drvWS_Teilnehmer, 0);
                            maennerkm += EfaUtil.zehntelString2Int(m.drvWS_MaennerKm);
                            juniorenkm += EfaUtil.zehntelString2Int(m.drvWS_JuniorenKm);
                            frauenkm += EfaUtil.zehntelString2Int(m.drvWS_FrauenKm);
                            juniorinnenkm += EfaUtil.zehntelString2Int(m.drvWS_JuniorinnenKm);                            
                        }
                        
                        Object[] gewaesser_arr = gewaesser.toArray();
                        Arrays.sort(gewaesser_arr);
                        String gewaesser_string = "";
                        for (int i = 0; i < gewaesser_arr.length; i++) {
                            gewaesser_string += gewaesser_arr[i] + (i + 1 < gewaesser_arr.length ? ", " : "");
                        }

                        maennerkm = maennerkm / 10;
                        juniorenkm = juniorenkm / 10;
                        frauenkm = frauenkm / 10;
                        juniorinnenkm = juniorinnenkm / 10;
                        int mannschkm = maennerkm + juniorenkm + frauenkm + juniorinnenkm;
                        
                        if (ew.verein_mitglnr == null || ew.verein_mitglnr.trim().length() == 0) {
                            ew.verein_mitglnr = mitglnr;
                            if (ew.verein_mitglnr == null) {
                                ew.verein_mitglnr = "";
                            }
                        }
                        if (clubIdHash.get(ew.verein_mitglnr) == null && ew.verein_mitglnr.length() > 0) {
                            clubIdHash.put(ew.verein_mitglnr, qnr + ": " + club);
                        } else {
                            conflicts.add(ew.verein_mitglnr + " (" + qnr + ": " + club + ")");
                            continue;
                        }
                        
                        DatenFelder d = new DatenFelder(Meldestatistik._ANZFELDER);
                        d.set(Meldestatistik.KEY, ew.verein_mitglnr);
                        d.set(Meldestatistik.VEREINSMITGLNR, ew.verein_mitglnr);
                        d.set(Meldestatistik.VEREIN, ew.verein_name);
                        d.set(Meldestatistik.WS_BUNDESLAND, ew.verein_lrv);
                        d.set(Meldestatistik.WS_MITGLIEDIN, ew.verein_mitgl_in);
                        d.set(Meldestatistik.WS_GEWAESSER, gewaesser_string);
                        d.set(Meldestatistik.WS_TEILNEHMER, Integer.toString(teilnehmer));
                        d.set(Meldestatistik.WS_MANNSCHKM, Integer.toString(mannschkm));
                        d.set(Meldestatistik.WS_MAENNERKM, Integer.toString(maennerkm));
                        d.set(Meldestatistik.WS_JUNIORENKM, Integer.toString(juniorenkm));
                        d.set(Meldestatistik.WS_FRAUENKM, Integer.toString(frauenkm));
                        d.set(Meldestatistik.WS_JUNIORINNENKM, Integer.toString(juniorinnenkm));
                        d.set(Meldestatistik.WS_AKT18M, ew.aktive_M_bis18);
                        d.set(Meldestatistik.WS_AKT19M, ew.aktive_M_ab19);
                        d.set(Meldestatistik.WS_AKT18W, ew.aktive_W_bis18);
                        d.set(Meldestatistik.WS_AKT19W, ew.aktive_W_ab19);
                        d.set(Meldestatistik.WS_VEREINSKILOMETER, ew.vereins_kilometer);
                        Main.drvConfig.meldestatistik.add(d);
                        countClubs++;
                    }
                } catch (Exception e) {
                    Logger.log(e);
                    Dialog.error("Fehler beim Erstellen der Meldestatistik: " + e);
                }
            } else {
                if (list.get(MeldungenIndex.STATUS).equals(Integer.toString(MeldungenIndex.ST_UNBEARBEITET))) {
                    countUnbearbeitet++;
                } else {
                    countInvalid++;
                }
            }
        }
        Main.drvConfig.meldestatistik.writeFile();
        if (showConfirmation) {
            Dialog.infoDialog("Wanderruder-Meldestatistik für " + countClubs + " bearbeitete Meldungen ("
                    + countSessions + " Fahrten) aktualisiert."
                    + (countUnbearbeitet + countInvalid > 0
                            ? "\n" + countUnbearbeitet + " unbearbeitete und " + countInvalid + " gelöschte oder zurückgewiesene Meldungen wurden ignoriert."
                            : "")
                    + (conflicts.size() > 0
                            ? "\nFolgende Meldungen wurden aufgrund ungültiger Mitgliedsnummern ignoriert:\n"
                            + EfaUtil.vector2string(conflicts, "\n")
                            : ""));
        }
    }
    
    void createMeldestatistikFA() {
        try {
            String stat_complete = Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + "meldestatistik_komplett.csv";
            BufferedWriter f;
            f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(stat_complete), Daten.ENCODING_ISO));
            f.write("Verein;Vereinsnummer;Vorname;Nachname;Jahrgang;Geschlecht;Kilometer;Gruppe;AnzAbzeichen(ges);AnzAbzeichen(AB);Äquator;Gesamt-Km\n");
            for (DatenFelder d = Main.drvConfig.meldestatistik.getCompleteFirst(); d != null; d = Main.drvConfig.meldestatistik.getCompleteNext()) {
                f.write(d.get(Meldestatistik.VEREIN) + ";" + 
                        d.get(Meldestatistik.VEREINSMITGLNR) + ";" + 
                        d.get(Meldestatistik.VORNAME) + ";" + 
                        d.get(Meldestatistik.NACHNAME) + ";" +
                        d.get(Meldestatistik.JAHRGANG) + ";" + 
                        d.get(Meldestatistik.GESCHLECHT).toLowerCase() + ";" + 
                        d.get(Meldestatistik.KILOMETER) + ";" +
                        d.get(Meldestatistik.GRUPPE) + ";" + 
                        d.get(Meldestatistik.ANZABZEICHEN) + ";" + 
                        d.get(Meldestatistik.ANZABZEICHENAB) + ";" +
                        d.get(Meldestatistik.AEQUATOR) + ";" +
                        d.get(Meldestatistik.GESKM) +
                        "\n");
            }
            f.close();

            String stat_div = Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + "meldestatistik_einzeln.csv";
            f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(stat_div), Daten.ENCODING_ISO));

            // Tabelle 2: Übersicht Männer Frauen Junioren Juniorinnen
            f.write("Statistik 2: Übersicht Männer / Frauen / Junioren / Junioninnen\n");
            f.write("Männer 18-30;Männer 31-60;Männer über 60;Frauen 18-30;Frauen 31-60;Frauen über 60;Junioren 13/14;Junioren 15/16;Junioren 17/18;Juniorinnen 13/14;Juniorinnen 15/16;Juniorinnen 17/18;\n");
            int data[] = new int[12];
            for (DatenFelder d = Main.drvConfig.meldestatistik.getCompleteFirst(); d != null; d = Main.drvConfig.meldestatistik.getCompleteNext()) {
                if (d.get(Meldestatistik.GESCHLECHT).equals(EfaWettMeldung.GESCHLECHT_M)) {
                    int jahrgang = EfaUtil.string2int(d.get(Meldestatistik.JAHRGANG), 0);
                    if (Main.drvConfig.aktJahr - jahrgang >= 18 && Main.drvConfig.aktJahr - jahrgang <= 30) {
                        data[0]++;
                    }
                    if (Main.drvConfig.aktJahr - jahrgang >= 31 && Main.drvConfig.aktJahr - jahrgang <= 60) {
                        data[1]++;
                    }
                    if (Main.drvConfig.aktJahr - jahrgang >= 61) {
                        data[2]++;
                    }
                    if (Main.drvConfig.aktJahr - jahrgang >= 13 && Main.drvConfig.aktJahr - jahrgang <= 14) {
                        data[6]++;
                    }
                    if (Main.drvConfig.aktJahr - jahrgang >= 15 && Main.drvConfig.aktJahr - jahrgang <= 16) {
                        data[7]++;
                    }
                    if (Main.drvConfig.aktJahr - jahrgang >= 17 && Main.drvConfig.aktJahr - jahrgang <= 18) {
                        data[8]++;
                    }
                }
                if (d.get(Meldestatistik.GESCHLECHT).equals(EfaWettMeldung.GESCHLECHT_W)) {
                    int jahrgang = EfaUtil.string2int(d.get(Meldestatistik.JAHRGANG), 0);
                    if (Main.drvConfig.aktJahr - jahrgang >= 18 && Main.drvConfig.aktJahr - jahrgang <= 30) {
                        data[3]++;
                    }
                    if (Main.drvConfig.aktJahr - jahrgang >= 31 && Main.drvConfig.aktJahr - jahrgang <= 60) {
                        data[4]++;
                    }
                    if (Main.drvConfig.aktJahr - jahrgang >= 61) {
                        data[5]++;
                    }
                    if (Main.drvConfig.aktJahr - jahrgang >= 13 && Main.drvConfig.aktJahr - jahrgang <= 14) {
                        data[9]++;
                    }
                    if (Main.drvConfig.aktJahr - jahrgang >= 15 && Main.drvConfig.aktJahr - jahrgang <= 16) {
                        data[10]++;
                    }
                    if (Main.drvConfig.aktJahr - jahrgang >= 17 && Main.drvConfig.aktJahr - jahrgang <= 18) {
                        data[11]++;
                    }
                }
            }
            for (int i = 0; i < data.length; i++) {
                f.write((i > 0 ? ";" : "") + data[i]);
            }
            f.write("\n\n\n");

            // Tabelle 3: 75 Jahre und älter
            SortedStatistic sStat = new SortedStatistic();
            f.write("Statistik 3: Jahrgang " + (Main.drvConfig.aktJahr - 75) + " und älter (75 Jahre und älter)\n");
            f.write("Jahrgang;Name;Verein;Vereins-Nr;Km\n");
            for (DatenFelder d = Main.drvConfig.meldestatistik.getCompleteFirst(); d != null; d = Main.drvConfig.meldestatistik.getCompleteNext()) {
                int jahrgang = EfaUtil.string2int(d.get(Meldestatistik.JAHRGANG), 9999);
                if (jahrgang <= Main.drvConfig.aktJahr - 75) {
                    int key = jahrgang * 100000 + (999999 - EfaUtil.string2int(d.get(Meldestatistik.KILOMETER), 0));
                    sStat.add(key, null,
                            d.get(Meldestatistik.JAHRGANG),
                            d.get(Meldestatistik.NACHNAME) + " " + d.get(Meldestatistik.VORNAME) + ";" + 
                            d.get(Meldestatistik.VEREIN) + ";" +
                            d.get(Meldestatistik.VEREINSMITGLNR),
                            d.get(Meldestatistik.KILOMETER));
                }
            }
            sStat.sort(true);
            for (int i = 0; i < sStat.sortedSize(); i++) {
                String[] sdata = sStat.getSorted(i);
                f.write(sdata[0] + ";" + sdata[1] + ";" + sdata[2] + "\n");
            }
            f.write("\n\n\n");

            // Tabelle 4: Über 4000 Km
            sStat = new SortedStatistic();
            f.write("Statistik 4: Über 4000 Km haben gerudert:\n");
            f.write("Platz;Km;Name;Jahrgang;Verein;Vereins-Nr\n");
            for (DatenFelder d = Main.drvConfig.meldestatistik.getCompleteFirst(); d != null; d = Main.drvConfig.meldestatistik.getCompleteNext()) {
                int km = EfaUtil.string2int(d.get(Meldestatistik.KILOMETER), 0);
                if (km >= 4000) {
                    sStat.add(km, null,
                            d.get(Meldestatistik.KILOMETER) + ";" +
                            d.get(Meldestatistik.NACHNAME) + ", " + d.get(Meldestatistik.VORNAME) + ";" +
                            d.get(Meldestatistik.JAHRGANG) + ";" + 
                            d.get(Meldestatistik.VEREIN) + ";" +
                            d.get(Meldestatistik.VEREINSMITGLNR),
                            null, null);
                }
            }
            sStat.sort(false);
            for (int i = 0; i < sStat.sortedSize(); i++) {
                String[] sdata = sStat.getSorted(i);
                f.write((i + 1) + ".;" + sdata[0] + "\n");
            }
            f.write("\n\n\n");

            // Tabelle 5: Fahrtenabzeichen in Gold
            sStat = new SortedStatistic();
            f.write("Statistik 5: Fahrtenabzeichen in Gold\n");
            f.write("Platz;Km;Name;Jahrgang;Verein;Vereins-Nr\n");
            for (int _anzAbz = 0; _anzAbz <= 95; _anzAbz += 5) {
                for (DatenFelder d = Main.drvConfig.meldestatistik.getCompleteFirst(); d != null; d = Main.drvConfig.meldestatistik.getCompleteNext()) {
                    int anzAbz = EfaUtil.string2int(d.get(Meldestatistik.ANZABZEICHEN), 0);
                    int anzAbzAB = EfaUtil.string2int(d.get(Meldestatistik.ANZABZEICHENAB), 0);
                    boolean erwachsen = !d.get(Meldestatistik.GRUPPE).startsWith("3");
                    if ((_anzAbz == 0 && !erwachsen && anzAbz % 5 == 0) || // Jugend-Gold
                            (_anzAbz > 0 && (anzAbz - anzAbzAB) == _anzAbz)) { // Erw-Gold

                        String abz = null;
                        if (_anzAbz == 0) {
                            abz = "99";
                        } else if (_anzAbz < 10) {
                            abz = "0" + _anzAbz;
                        } else {
                            abz = Integer.toString(_anzAbz);
                        }

                        sStat.add(-1, abz + d.get(Meldestatistik.NACHNAME) + ", " + d.get(Meldestatistik.VORNAME), abz, 
                                d.get(Meldestatistik.KILOMETER) + ";" +
                                d.get(Meldestatistik.NACHNAME) + ", " + d.get(Meldestatistik.VORNAME) + ";" + 
                                d.get(Meldestatistik.JAHRGANG) + ";" + 
                                d.get(Meldestatistik.VEREIN) + ";" +
                                d.get(Meldestatistik.VEREINSMITGLNR),
                                null);
                    }
                }
            }
            sStat.sort(true);
            int c = 0;
            String lastAbz = "xx";
            for (int i = 0; i < sStat.sortedSize(); i++) {
                String[] sdata = sStat.getSorted(i);
                if (!sdata[0].equals(lastAbz)) {
                    c = 0;
                    lastAbz = sdata[0];
                    if (!sdata[0].equals("99")) {
                        f.write("Zum " + sdata[0] + ". Mal erfüllt:\n");
                    } else {
                        f.write("Jugendfahrtenabzeichen in Gold:\n");
                    }
                }
                f.write((++c) + ".;" + sdata[1] + "\n");
            }
            f.write("\n\n\n");

            // Tabelle 6: Äquatorpreisträger
            f.write("Statistik 6: Äquatorpreisträger:\n");
            f.write("Verein;Vereins-Nr;Vorname;Nachname;Kilometer;Äquatorpreis\n");
            sStat = new SortedStatistic();
            for (DatenFelder d = Main.drvConfig.meldestatistik.getCompleteFirst(); d != null; d = Main.drvConfig.meldestatistik.getCompleteNext()) {
                if (d.get(Meldestatistik.AEQUATOR).length() > 0) {
                    sStat.add(-1, d.get(Meldestatistik.VEREIN), 
                            d.get(Meldestatistik.VEREIN) + ";" + 
                            d.get(Meldestatistik.VEREINSMITGLNR) + ";" + 
                            d.get(Meldestatistik.VORNAME) + ";" + 
                            d.get(Meldestatistik.NACHNAME) + ";" + 
                            d.get(Meldestatistik.GESKM) + ";" + 
                            d.get(Meldestatistik.AEQUATOR),
                            null, null);
                }
            }
            sStat.sort(true);
            for (int i = 0; i < sStat.sortedSize(); i++) {
                f.write(sStat.getSorted(i)[0] + "\n");
            }
            f.write("\n\n\n");

            // Tabelle 7: Über 4000 Km
            sStat = new SortedStatistic();
            f.write("Statistik 7: Fahrtenabzeichen pro Verein:\n");
            f.write("Vereins-Nr;Verein;Anzahl\n");
            Hashtable<String,Integer> vereinsAbz = new Hashtable<String,Integer>();
            Hashtable<String,String> vereinsName = new Hashtable<String,String>();
            for (DatenFelder d = Main.drvConfig.meldestatistik.getCompleteFirst(); d != null; d = Main.drvConfig.meldestatistik.getCompleteNext()) {
                String vereinsId = d.get(Meldestatistik.VEREINSMITGLNR);
                Integer anz = vereinsAbz.get(vereinsId);
                if (anz == null) {
                    vereinsAbz.put(vereinsId, 1);
                } else {
                    vereinsAbz.put(vereinsId, anz + 1);
                }
                vereinsName.put(vereinsId, d.get(Meldestatistik.VEREIN));
            }
            for (String vereinsId : vereinsAbz.keySet()) {
                sStat.add(vereinsAbz.get(vereinsId), null,
                        vereinsId + ";" + vereinsName.get(vereinsId),
                        Integer.toString(vereinsAbz.get(vereinsId)),
                        null);
            }
            sStat.sort(false);
            for (int i = 0; i < sStat.sortedSize(); i++) {
                String[] sdata = sStat.getSorted(i);
                f.write(sdata[0] + ";" + sdata[1] + "\n");
            }
            f.write("\n\n\n");

            // Tabelle 8: FA+WS Statistik zusammen
            MeldungenIndex idxWS = new MeldungenIndex(Daten.efaDataDirectory+Main.drvConfig.aktJahr+Daten.fileSep+DRVConfig.MELDUNGEN_WS_FILE);
            if (idxWS.readFile()) {
                updateMeldestatistikWS(idxWS, true);
                sStat = new SortedStatistic();
                f.write("Statistik 8: Wanderruderstatistik und Fahrtenabzeichen:\n");
                f.write("Vereins-Nr;Verein;Aktive;Mannschaftskilometer;Fahrtenabzeichen\n");

                for (DatenFelder d = Main.drvConfig.meldestatistik.getCompleteFirst(); d != null; d = Main.drvConfig.meldestatistik.getCompleteNext()) {
                    String vereinsId = d.get(Meldestatistik.VEREINSMITGLNR);
                    int aktive = EfaUtil.string2int(d.get(Meldestatistik.WS_AKT18M), 0)
                            + EfaUtil.string2int(d.get(Meldestatistik.WS_AKT19M), 0)
                            + EfaUtil.string2int(d.get(Meldestatistik.WS_AKT18W), 0)
                            + EfaUtil.string2int(d.get(Meldestatistik.WS_AKT19W), 0);
                    String mannschKm = d.get(Meldestatistik.WS_MANNSCHKM);
                    int abzeichen = vereinsAbz.get(vereinsId) != null ? vereinsAbz.get(vereinsId) : 0;
                    String vdata = d.get(Meldestatistik.VEREINSMITGLNR) + ";" + d.get(Meldestatistik.VEREIN) + ";" + 
                            aktive + ";" + mannschKm + ";" + abzeichen;
                    sStat.add(-1, vereinsId, vdata, null, null);
                }
                sStat.sort(false);
                for (int i = 0; i < sStat.sortedSize(); i++) {
                    String[] sdata = sStat.getSorted(i);
                    f.write(sdata[0] + "\n");
                }
                f.write("\n\n\n");
            }

            f.close();
            Dialog.infoDialog("Statistiken exportiert",
                    "Die Statistiken wurden erfolgreich erstellt:\n"
                    + stat_complete + "\n"
                    + stat_div + "\n\n"
                    + "Um die Statistik in Excel zu öffnen:\n"
                    + "1. Excel starten\n"
                    + "2. ->Datei->Öffnen wählen\n"
                    + "3. In den Ordner \"" + EfaUtil.getPathOfFile(stat_complete) + "\" wechseln\n"
                    + "4. Bei der Auswahl der anzuzeigenden Dateien \"Alle Dateien\" wählen\n"
                    + "5. Die Dateien \"" + EfaUtil.getFilenameWithoutPath(stat_complete) + "\" " 
                    + "und \"" + EfaUtil.getFilenameWithoutPath(stat_div) + "\" öffnen");
            /*
            if (Dialog.yesNoDialog("Statistiken kopieren",
                    "Sollen die erzeugten Statistiken kopiert\n"
                    + "werden (z.B. auf Diskette)?") == Dialog.YES) {
                String ziel = Dialog.inputDialog("Ziel", "Wohin sollen die Dateien kopiert werden?", "A:\\");
                if (ziel != null && ziel.trim().length() > 0) {
                    ziel = ziel.trim();
                    if (!ziel.endsWith(Daten.fileSep)) {
                        ziel += Daten.fileSep;
                    }
                    if (!(new File(ziel)).isDirectory()) {
                        Dialog.error("Statistiken können nicht kopiert werden:\nZielverzeichnis " + ziel + " existiert nicht.");
                    } else {
                        boolean b1 = EfaUtil.copyFile(stat_complete, ziel + EfaUtil.getFilenameWithoutPath(stat_complete));
                        boolean b2 = EfaUtil.copyFile(stat_div, ziel + EfaUtil.getFilenameWithoutPath(stat_div));
                        if (b1 && b2) {
                            Dialog.infoDialog("Statistiken erfolgreich kopiert.");
                        } else {
                            Dialog.error("Statistiken konnten nicht kopiert werden.");
                        }
                    }
                }
            } */
        } catch (Exception ee) {
            Dialog.error("Fehler beim Erstellen der Statistik: " + ee.toString());
            Logger.log(Logger.ERROR, "Fehler beim Erstellen der Statistik: " + ee.toString());
        }
    }

    void createMeldestatistikWS() {
        try {
            // Wanderruderstatistik (eine Zeile pro Verein)
            String stat_complete = Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + "wanderruderstatistik.csv";
            BufferedWriter f;
            f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(stat_complete), Daten.ENCODING_ISO));
            f.write("Vereinsnummer;Verein;Bundesland;SRV/ADH;Befahrene Gewässer;Teilnehmer insg.;Mannschafts-Km;Männer Km; Junioren Km; Frauen Km; Juniorinnen Km; Aktive M bis 18; Aktive M ab 19; Aktive W bis 18; Aktive W ab 19; Vereinskilometer\n");
            Hashtable mitglnr_hash = new Hashtable();
            for (DatenFelder d = Main.drvConfig.meldestatistik.getCompleteFirst(); d != null; d = Main.drvConfig.meldestatistik.getCompleteNext()) {
                String mitgl_in = "";
                if (d.get(Meldestatistik.WS_MITGLIEDIN).indexOf("SRV") >= 0) {
                    mitgl_in = "SRV";
                }
                if (d.get(Meldestatistik.WS_MITGLIEDIN).indexOf("ADH") >= 0) {
                    mitgl_in += (mitgl_in.length() > 0 ? ", " : "") + "ADH";
                }
                f.write(d.get(Meldestatistik.VEREINSMITGLNR) + ";"
                        + d.get(Meldestatistik.VEREIN) + ";"
                        + d.get(Meldestatistik.WS_BUNDESLAND) + ";"
                        + mitgl_in + ";"
                        + d.get(Meldestatistik.WS_GEWAESSER) + ";"
                        + d.get(Meldestatistik.WS_TEILNEHMER) + ";"
                        + d.get(Meldestatistik.WS_MANNSCHKM) + ";"
                        + d.get(Meldestatistik.WS_MAENNERKM) + ";"
                        + d.get(Meldestatistik.WS_JUNIORENKM) + ";"
                        + d.get(Meldestatistik.WS_FRAUENKM) + ";"
                        + d.get(Meldestatistik.WS_JUNIORINNENKM) + ";"
                        + d.get(Meldestatistik.WS_AKT18M) + ";"
                        + d.get(Meldestatistik.WS_AKT19M) + ";"
                        + d.get(Meldestatistik.WS_AKT18W) + ";"
                        + d.get(Meldestatistik.WS_AKT19W) + ";"
                        + d.get(Meldestatistik.WS_VEREINSKILOMETER)
                        + "\n");
                mitglnr_hash.put(d.get(Meldestatistik.VEREINSMITGLNR), "foo");
            }
            f.close();

            // Gewässerstatistik (eine Zeile pro Gewässer)
            Hashtable<String,WatersStat> gewaesser = new Hashtable<String,WatersStat>();
            for (DatenFelder d = Main.drvConfig.meldungenIndex.getCompleteFirst(); d != null; d = Main.drvConfig.meldungenIndex.getCompleteNext()) {
                String qnr = d.get(MeldungenIndex.QNR);
                if (qnr != null && qnr.length() > 0) {
                    EfaWett efw = new EfaWett(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efw");
                    try {
                        if (efw.readFile()) {
                            for (EfaWettMeldung ewm = efw.meldung; ewm != null; ewm = ewm.next) {
                                if (ewm.drvint_wirdGewertet) {
                                    if (ewm.drvWS_Gewaesser != null && ewm.drvWS_Gewaesser.length() > 0) {
                                        Vector<String> g = EfaUtil.split(EfaUtil.replace(ewm.drvWS_Gewaesser, ";", ",", true), ',');
                                        for (int i=0; g != null && i<g.size(); i++) {
                                            String name = g.get(i).trim();
                                            WatersStat w = gewaesser.get(name);
                                            if (w == null) {
                                                w = new WatersStat();
                                                gewaesser.put(name, w);
                                            }
                                            w.fahrten++;
                                            w.personen += EfaUtil.string2int(ewm.drvWS_Teilnehmer, 0);
                                            w.tage += EfaUtil.string2int(ewm.drvWS_Tage, 0);
                                            w.kilometer += EfaUtil.string2int(ewm.drvWS_Km, 0);
                                            w.mannschkilometer += EfaUtil.string2int(ewm.drvWS_MannschKm, 0);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e1) {
                    }
                }
            }
            String stat_waters = Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + "gewaesser.csv";
            f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(stat_waters), Daten.ENCODING_ISO));
            f.write("Hinweis:;Für Fahrten auf mehreren Gewässern werden die Tage, Kilometer und Mannschafts-Kilometer\n");
            f.write(";der gesamten Fahrt ALLEN Gewässern der Fahrt zugerechnet, da diese in der Meldung nicht\n");
            f.write(";separat ausgewiesen sind. Die Angaben in den Spalten Tage, Kilometer und Mannschafts-Km\n");
            f.write(";sind daher etwas verfälscht und zu hoch und geben nur einen ungefähren Eindruck über die\n");
            f.write(";Ruderaktivität auf diesen Gewässern.\n");
            f.write(";\n");
            f.write("Gewässer;Fahrten;Personen;Tage;Kilometer;Mannschafts-Km\n");
            String[] gnames = gewaesser.keySet().toArray(new String[0]);
            Arrays.sort(gnames);
            for (String name : gnames) {
                WatersStat w = gewaesser.get(name);
                f.write(name + ";" + 
                        w.fahrten + ";" + 
                        w.personen + ";" + 
                        w.tage + ";" + 
                        w.kilometer + ";" + 
                        w.mannschkilometer + "\n");
            }
            f.close();

            Dialog.infoDialog("Statistik exportiert",
                    "Die Statistik wurde erfolgreich erstellt:\n"
                    + stat_complete + "\n"
                    + stat_waters + "\n\n"
                    + "Um die Statistik in Excel zu öffnen:\n"
                    + "1. Excel starten\n"
                    + "2. ->Datei->Öffnen wählen\n"
                    + "3. In den Ordner \"" + EfaUtil.getPathOfFile(stat_complete) + "\" wechseln\n"
                    + "4. Bei der Auswahl der anzuzeigenden Dateien \"Alle Dateien\" wählen\n"
                    + "5. Die Dateien \"" + EfaUtil.getFilenameWithoutPath(stat_complete) + "\" " 
                    + "und \"" + EfaUtil.getFilenameWithoutPath(stat_waters) + "\" öffnen");
        } catch (Exception ee) {
            Dialog.error("Fehler beim Erstellen der Statistik: " + ee.toString());
            Logger.log(Logger.ERROR, "Fehler beim Erstellen der Statistik: " + ee.toString());
        }
    }

    boolean loadKeys() {
        if (Daten.keyStore != null && Daten.keyStore.isKeyStoreReady()) {
            return true;
        }
        Daten.keyStore = new EfaKeyStore(Daten.efaDataDirectory + DRVConfig.KEYSTORE_FILE, Main.drvConfig.keyPassword);
        if (!Daten.keyStore.isKeyStoreReady()) {
            Dialog.error("KeyStore kann nicht geladen werden:\n" + Daten.keyStore.getLastError());
            Main.drvConfig.keyPassword = null;
        }
        return Daten.keyStore.isKeyStoreReady();
    }

    void checkFahrtenheftButton_actionPerformed(ActionEvent e) {
        if (Main.drvConfig.keyPassword == null) {
            KeysAdminFrame.enterKeyPassword();
        }
        if (Main.drvConfig.keyPassword == null) {
            return;
        }
        if (!loadKeys()) {
            return;
        }

        DRVSignaturFrame dlg = new DRVSignaturFrame(this, null);
        dlg.setCloseButtonText("Schließen");
        Dialog.setDlgLocation(dlg, this);
        dlg.setModal(true);
        dlg.show();
    }

    void printOverviewButton_actionPerformed(ActionEvent e) {
        if (MELDTYP == MELD_WANDERRUDERSTATISTIK &&
            Main.drvConfig.meldestatistik == null) {
            Dialog.error("Bitte erstelle zuerst eine Meldestatistik!");
            return;
        }
        int selection = Dialog.auswahlDialog("Meldungen auswählen",
                "Meldeübersicht erzeugen für ...",
                "Nur bearbeite Meldungen", "Alle Meldungen", false);
        String tmpdatei = Daten.efaTmpDirectory + "uebersicht.html";
        try {
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpdatei), Daten.ENCODING_ISO));
            f.write("<html>\n");
            f.write("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=" + Daten.ENCODING_ISO + "\"></head>\n");
            f.write("<body>\n");
            f.write("<h1 align=\"center\">Meldeübersicht</h1>\n");
            f.write("<table align=\"center\" border=\"3\" width=\"100%\">\n");
            f.write("<tr><th>Verein</th><th>Status</th>"
                    + (MELDTYP == MELD_WANDERRUDERSTATISTIK ? "<th>Gewässer</th>" : "<th>Fahrtenhefte</th>") + 
                    "<th>Gewertet</th>" +
                    "<th>Meldender</th>" +
                    "<th>email</th>" +
                    "</tr>\n");

            for (DatenFelder d = Main.drvConfig.meldungenIndex.getCompleteFirst(); d != null; d = Main.drvConfig.meldungenIndex.getCompleteNext()) {
                if (selection == 0 && EfaUtil.string2int(d.get(MeldungenIndex.STATUS), MeldungenIndex.ST_UNBEKANNT) != MeldungenIndex.ST_BEARBEITET) {
                    continue;
                }
                String verein = d.get(MeldungenIndex.VEREIN);
                String status = MeldungenIndex.ST_NAMES[EfaUtil.string2int(d.get(MeldungenIndex.STATUS), MeldungenIndex.ST_UNBEKANNT)];
                String feld3 = "";
                String contact = "";
                String email = "";
                if (MELDTYP == MELD_WANDERRUDERSTATISTIK) {
                    DatenFelder ms = Main.drvConfig.meldestatistik.getExactComplete(d.get(MeldungenIndex.MITGLNR));
                    if (ms != null) {
                        feld3 = ms.get(Meldestatistik.WS_GEWAESSER);
                    }
                } else {
                    int fh = EfaUtil.string2int(d.get(MeldungenIndex.FAHRTENHEFTE), MeldungenIndex.FH_UNBEKANNT);
                    switch (fh) {
                        case MeldungenIndex.FH_UNBEKANNT:
                            feld3 = "";
                            break;
                        case MeldungenIndex.FH_KEINE:
                            feld3 = "keine";
                            break;
                        case MeldungenIndex.FH_PAPIER:
                            feld3 = "Papier";
                            break;
                        case MeldungenIndex.FH_ELEKTRONISCH:
                            feld3 = "nur elektr.";
                            break;
                        case MeldungenIndex.FH_PAPIER_UND_ELEKTRONISCH:
                            feld3 = "elektr. / Papier";
                            break;
                        default:
                            feld3 = "";
                    }
                }
                
                // Anzahl der gewerteten Teilnehmer
                int gewertet = 0;
                int total = 0;
                String fname = EfaUtil.getPathOfFile(Main.drvConfig.meldungenIndex.getFileName()) + Daten.fileSep + d.get(MeldungenIndex.QNR) + ".efw";
                EfaWett efw = new EfaWett(fname);
                if (efw.readFile()) {
                    for (EfaWettMeldung m = efw.meldung; m != null; m = m.next) {
                        total++;
                        if (m.drvint_wirdGewertet) {
                            gewertet++;
                        }
                        
                    }
                    contact = efw.meld_name != null ? efw.meld_name : "";
                    email = efw.meld_email != null ? efw.meld_email : "";
                }
                String count = gewertet + " / " + total;
                
                f.write("<tr><td>" + verein + "</td><td>" + status + 
                        "</td><td>" + feld3 + "</td><td>" + count + 
                        "</td><td>" + contact + "</td><td>" + email + "</td></tr>\n");
            }
            f.write("</table>\n");
            f.write("</body></html>\n");
            f.close();
            JEditorPane out = new JEditorPane();
            out.setContentType("text/html; charset=" + Daten.ENCODING_ISO);
            out.setPage(EfaUtil.correctUrl("file:" + tmpdatei));
            BrowserDialog.openInternalBrowser(this, tmpdatei);
            /*
            SimpleFilePrinter.sizeJEditorPane(out);
            SimpleFilePrinter sfp = new SimpleFilePrinter(out);
            if (sfp.setupPageFormat()) {
                if (sfp.setupJobOptions()) {
                    sfp.printFile();
                }
            }
            */
            EfaUtil.deleteFile(tmpdatei);
        } catch (Exception ee) {
            Logger.log(ee);
            Dialog.error("Druckdatei konnte nicht erstellt werden: " + ee.toString());
            return;
        }


    }

    void importButton_actionPerformed(ActionEvent e) {
        String fname = Dialog.dateiDialog(this, "Meldung importieren",
                "Meldung (*.efw)", "efw", Daten.efaDataDirectory, false);
        if (fname == null || fname.length() == 0 || !EfaUtil.canOpenFile(fname)) {
            return;
        }
        EfaWett efw = new EfaWett(fname);
        try {
            if (efw.readFile()) {
                if (addMeldung(efw.verein_user)) {
                    String qnr = null;
                    DatenFelder d = Main.drvConfig.meldungenIndex.getCompleteFirst();
                    while (d != null) {
                        EfaWett etmp = new EfaWett(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + d.get(MeldungenIndex.QNR) + ".efw");
                        if (etmp.readFile() && efw.verein_user.equals(etmp.verein_user)) {
                            qnr = d.get(MeldungenIndex.QNR);
                            break;
                            
                        }
                        d = Main.drvConfig.meldungenIndex.getCompleteNext();
                    }
                    if (qnr == null) {
                        Dialog.error("Importierte Meldung konnte nicht gefunden werden.");
                        return;
                    }
                    efw.setFileName(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efw");
                    if (efw.writeFile()) {
                        Logger.log(Logger.INFO, "Neue Meldungen mit Quittungsnummer " + qnr + " wurde importiert.");
                        Dialog.infoDialog("Meldung mit Quittungsnummer " + qnr + " für Verein " + efw.verein_user + " erfolgreicht importiert.");
                        showMeldungen();
                    } else {
                        Dialog.error("Importierte Meldung konnte nicht gespeichert werden.");
                    }
                }
            }
        } catch(Exception ex) {
            Dialog.error("Fehler beim Importieren der Meldung: " + ex.getMessage());
        }

        /*
        String qnr = null;
        Pattern p = Pattern.compile(".*[^0-9]([0-9]+).efw.*");
        Matcher m = p.matcher(fname);
        if (m.matches()) {
            qnr = m.group(1);
        }
        if (qnr == null || qnr.length() == 0) {
            qnr = Long.toString((System.currentTimeMillis() / 1000l) * 100l + 99l);
        }
        if (Main.drvConfig.meldungenIndex.getExactComplete(qnr) != null) { // sollte nie passieren
            Dialog.error("Die Quittungsnummer " + qnr + " existiert bereits.");
            return;
        }
        EfaWett efw = new EfaWett(fname);
        try {
            if (efw.readFile()) {
                efw.setFileName(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efw");
                if (efw.writeFile()) {
                    Logger.log(Logger.INFO, "Neue Meldungen mit Quittungsnummer " + qnr + " wird importiert.");
                    DatenFelder d = new DatenFelder(MeldungenIndex._ANZFELDER);
                    d.set(MeldungenIndex.QNR, qnr);
                    d.set(MeldungenIndex.DATUM, EfaUtil.getCurrentTimeStampYYYY_MM_DD_HH_MM_SS());
                    d.set(MeldungenIndex.STATUS, Integer.toString(MeldungenIndex.ST_UNBEARBEITET));
                    d.set(MeldungenIndex.FAHRTENHEFTE, Integer.toString(MeldungenIndex.FH_PAPIER));
                    d.set(MeldungenIndex.VEREIN, efw.verein_name);
                    d.set(MeldungenIndex.MITGLNR, efw.verein_mitglnr);
                    Main.drvConfig.meldungenIndex.add(d);
                    Main.drvConfig.meldungenIndex.writeFile();
                    Dialog.infoDialog("Meldung importiert", "Die Meldung wurde erfolgreich exportiert!");
                }
            }
            showMeldungen();
        } catch (Exception ee) {
            Dialog.error("Fehler beim Importieren der Meldung: " + ee.getMessage());
        }
        */
    }

    void addButton_actionPerformed(ActionEvent e) {
        String verein = Dialog.inputDialog("Neue Meldung erfassen", 
                "efaWett-Benutzername des Vereins, für den die Meldung erfaßt werden soll");
        addMeldung(verein);
    }
    
    boolean addMeldung(String verein) {
        if (verein == null || verein.trim().length() == 0) {
            return false;
        }
        String wettkey = WETTID + "." + Main.drvConfig.aktJahr;
        
        String url = Main.drvConfig.makeScriptRequestString(DRVConfig.ACTION_CREATE, "verein=" + verein, "wettkey=" + wettkey, null, null, null, null);
        String localFile = Daten.efaTmpDirectory + "efwstatus.tmp";
        if (!DownloadThread.getFile(this, url, localFile, true) || !EfaUtil.canOpenFile(localFile)) {
            Logger.log(Logger.ERROR, "Erstellen der Meldung für Verein " + verein + " fehlgeschlagen: Kann efaWett nicht erreichen");
            Dialog.error("Aktion fehlgeschlagen: Kann efaWett nicht erreichen");
            return false;
        }
        try {
            BufferedReader f = new BufferedReader(new FileReader(localFile));
            String s = f.readLine();
            if (s == null || !s.equals("OK")) {
                Logger.log(Logger.ERROR, "Erstellen der Meldung für Verein " + verein + " fehlgeschlagen: " + s);
                Dialog.error("Aktion fehlgeschlagen: " + s);
                return false;
            }
            f.close();
            EfaUtil.deleteFile(localFile);
        } catch (Exception ee) {
            Logger.log(Logger.ERROR, "Erstellen der Meldung für Verein " + verein + " fehlgeschlagen: " + ee.getMessage());
            Dialog.error("Aktion fehlgeschlagen: " + ee.getMessage());
            return false;
        }
        Dialog.infoDialog("Neue Meldung für Verein " + verein + " erfolgreich erstellt.");
        downloadButton_actionPerformed(null);
        return true;
        /*
        String qnr = Long.toString((System.currentTimeMillis() / 1000l) * 100l + 99l);
        if (Main.drvConfig.meldungenIndex.getExactComplete(qnr) != null) { // sollte nie passieren
            Dialog.error("Die Quittungsnummer " + qnr + " existiert bereits.");
            return;
        }
        Logger.log(Logger.INFO, "Neue Meldungen mit Quittungsnummer " + qnr + " wird von Hand erfaßt.");
        DatenFelder d = new DatenFelder(MeldungenIndex._ANZFELDER);
        d.set(MeldungenIndex.QNR, qnr);
        d.set(MeldungenIndex.DATUM, EfaUtil.getCurrentTimeStampYYYY_MM_DD_HH_MM_SS());
        d.set(MeldungenIndex.STATUS, Integer.toString(MeldungenIndex.ST_UNBEARBEITET));
        d.set(MeldungenIndex.FAHRTENHEFTE, Integer.toString(MeldungenIndex.FH_PAPIER));
        d.set(MeldungenIndex.BESTAETIGUNGSDATEI, MeldungenIndex.MANUELL_ERFASST);
        Main.drvConfig.meldungenIndex.add(d);

        EfaWett efw = new EfaWett(Daten.efaDataDirectory + Main.drvConfig.aktJahr + Daten.fileSep + qnr + ".efw");
        try {
            if (MELDTYP == MELD_FAHRTENABZEICHEN && !Main.drvConfig.readOnlyMode) {
                if (Main.drvConfig.keyPassword == null) {
                    KeysAdminFrame.enterKeyPassword();
                }
                if (Main.drvConfig.keyPassword == null) {
                    return;
                }
                if (!loadKeys()) {
                    return;
                }
            }
            efw.allg_programm = Daten.PROGRAMMID_DRV;
            efw.allg_wett = WETTID;
            efw.allg_wettjahr = Integer.toString(Main.drvConfig.aktJahr);
            efw.kennung = EfaWett.EFAWETT;

            MeldungEditFrame dlg = new MeldungEditFrame(this, efw, qnr, MELDTYP);
            Dialog.setDlgLocation(dlg, this);
            dlg.setModal(true);
            dlg.show();
            if (dlg.hasBeenSaved()) {
                d = Main.drvConfig.meldungenIndex.getExactComplete(qnr);
                if (d != null) { // muß immer != null sein
                    d.set(MeldungenIndex.VEREIN, efw.verein_name);
                    d.set(MeldungenIndex.MITGLNR, efw.verein_mitglnr);
                    Main.drvConfig.meldungenIndex.delete(qnr);
                    Main.drvConfig.meldungenIndex.add(d);
                }
            } else {
                Main.drvConfig.meldungenIndex.delete(qnr);
            }
            Main.drvConfig.meldungenIndex.writeFile();
        } catch (Exception eee) {
            Dialog.error("Fehler beim Schreiben der Meldungsdatei: " + eee.toString());
        }
        showMeldungen();
        */
    }
    
    class WatersStat {
        int fahrten = 0;
        int personen = 0;
        int tage = 0;
        long kilometer = 0;
        long mannschkilometer = 0;
    }
}
