/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.gui.statistics;

import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.data.efawett.EfaWett;
import de.nmichael.efa.*;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.BrowserDialog;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.Base64;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class EfaWettDoneDialog extends BaseDialog implements ActionListener {

    EfaWett efaWett;
    String meldegeld;
    Vector papierFahrtenhefte;
    private final static String MELDEGELD_TEIL1 = "Bitte überweise das Meldegeld in Höhe von ";
    private final static String MELDEGELD_TEIL2 = " auf folgendes Konto:";
    JTabbedPane jTabbedPane1 = new JTabbedPane();
    JPanel paddedInfoPanel = new JPanel();
    JPanel infoPanel = new JPanel();
    JPanel wettPanel = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel jPanel3 = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    JScrollPane jScrollPane1 = new JScrollPane();
    JTextArea wettDatei = new JTextArea();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabel1 = new JLabel();
    JLabel jLabel10 = new JLabel();
    JLabel dateinameLabel = new JLabel();
    JLabel jLabel2 = new JLabel();
    BorderLayout borderLayout3 = new BorderLayout();
    JLabel jLabel6 = new JLabel();
    JLabel jLabel7 = new JLabel();
    JLabel jLabel8 = new JLabel();
    JLabel jLabel9 = new JLabel();
    JLabel meldegeld1Label = new JLabel();
    JLabel meldegeldLabel = new JLabel();
    JLabel kontoLabel = new JLabel();
    JButton einsendenButton = new JButton();
    JLabel fahrtenhefte1Label = new JLabel();
    JLabel fahrtenhefte2Label = new JLabel();
    JLabel fahrtenhefteAdresseLabel = new JLabel();
    JScrollPane fahrtenhefteScrollPane = new JScrollPane();
    JList fahrtenhefteList = new JList();
    JLabel meldegeld2Label = new JLabel();
    JLabel achtung1Label = new JLabel();
    JLabel achtung2Label = new JLabel();
    JButton saveMeldedateiButton = new JButton();
    JButton anleitungButton = new JButton();

    public EfaWettDoneDialog(JDialog parent, EfaWett efaWett, String meldegeld, Vector papierFahrtenhefte) {
        super(parent,
                International.onlyFor("Meldedatei erstellt", "de"),
                International.onlyFor("Schließen", "de"));

        this.efaWett = efaWett;
        this.meldegeld = meldegeld;
        this.papierFahrtenhefte = papierFahrtenhefte;
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());
        wettPanel.setLayout(borderLayout2);
        wettDatei.setEditable(false);
        paddedInfoPanel.setLayout(new GridBagLayout());
        infoPanel.setLayout(gridBagLayout1);
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 14));
        jLabel1.setForeground(Color.blue);
        jLabel1.setText("Die Meldedatei wurde erfolgreich erstellt!");
        jLabel10.setForeground(Color.black);
        jLabel10.setText("Die Meldedatei wurde an folgendem Ort abgespeichert:");
        dateinameLabel.setForeground(Color.black);
        dateinameLabel.setText("dateiname");
        jLabel2.setText("Damit die Meldung den Verband erreicht und bearbeitet werden kann, "
                + "führe bitte folgende Schritte durch:");
        jLabel9.setText("        zum späteren Einsenden der Meldedatei von einem anderen Computer, "
                + "speichere die Meldedatei jetzt bitte ab!");
        this.setTitle("Meldedatei erstellt");
        jLabel6.setForeground(Color.blue);
        jLabel6.setText("1. Meldedatei einsenden");
        jLabel7.setText("- entweder sofort durch Klicken dieses Buttons:");
        jLabel8.setText("- oder später durch Einsenden der Meldedatei per Webbrowser:");
        jLabel9.setToolTipText("");
        jLabel9.setText("   Um die Meldedatei von einem anderen Computer einzusenden, klicke "
                + "jetzt bitte \'Meldedatei kopieren\'.");
        meldegeld1Label.setForeground(Color.blue);
        meldegeld1Label.setText("2. Meldegeld überweisen");
        meldegeldLabel.setText(MELDEGELD_TEIL1 + "123,45 EUR" + MELDEGELD_TEIL2);
        kontoLabel.setForeground(Color.black);
        kontoLabel.setText("Konto");
        einsendenButton.setBackground(Color.orange);
        einsendenButton.setEnabled(true);
        einsendenButton.setMnemonic('M');
        einsendenButton.setText("Meldedatei jetzt einsenden");
        einsendenButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                einsendenButton_actionPerformed(e);
            }
        });
        fahrtenhefte1Label.setForeground(Color.blue);
        fahrtenhefte1Label.setText("3. Papier-Fahrtenhefte einsenden:");
        fahrtenhefte2Label.setText("Bitte sende unter Angabe der Quittungsnummer alle Papier-Fahrtenhefte "
                + "folgender Personen an:");
        fahrtenhefteAdresseLabel.setForeground(Color.black);
        fahrtenhefteAdresseLabel.setText("Adresse");
        fahrtenhefteScrollPane.setMinimumSize(new Dimension(22, 100));
        meldegeld2Label.setText("Bei der Überweisung unbedingt Vereinsnamen und Mitgliedsnummer angeben!");
        achtung1Label.setForeground(Color.red);
        achtung1Label.setText("Achtung: Die Wettbewerbs-Konfigurationsdaten von efa sind schon recht "
                + "alt!");
        achtung2Label.setForeground(Color.red);
        achtung2Label.setText("Bitte überprüfe die Höhe des Meldegeldes und die Bankverbindung anhand "
                + "der aktuellen Ausschreibung!");
        saveMeldedateiButton.setBackground(Color.orange);
        saveMeldedateiButton.setForeground(Color.black);
        saveMeldedateiButton.setDebugGraphicsOptions(0);
        saveMeldedateiButton.setText("Meldedatei kopieren (z.B. auf USB-Stick)");
        saveMeldedateiButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveMeldedateiButton_actionPerformed(e);
            }
        });
        anleitungButton.setText("Ausführliche Anleitung anzeigen");
        anleitungButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                anleitungButton_actionPerformed(e);
            }
        });
        mainPanel.add(jTabbedPane1, BorderLayout.CENTER);
        jTabbedPane1.add(paddedInfoPanel, "Information");
        paddedInfoPanel.add(infoPanel, new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, GridBagConstraints.REMAINDER, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0));
        infoPanel.add(jLabel1, new GridBagConstraints(0, 0, 9, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        infoPanel.add(jLabel10, new GridBagConstraints(0, 1, 9, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
        infoPanel.add(dateinameLabel, new GridBagConstraints(0, 2, 9, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        infoPanel.add(jLabel2, new GridBagConstraints(0, 3, 9, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(20, 0, 0, 0), 0, 0));
        infoPanel.add(jLabel6, new GridBagConstraints(0, 5, 9, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        infoPanel.add(jLabel7, new GridBagConstraints(0, 6, 8, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        infoPanel.add(jLabel8, new GridBagConstraints(0, 7, 8, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        infoPanel.add(jLabel9, new GridBagConstraints(0, 8, 9, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        infoPanel.add(meldegeld1Label, new GridBagConstraints(0, 9, 9, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
        infoPanel.add(meldegeldLabel, new GridBagConstraints(0, 10, 9, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jTabbedPane1.add(wettPanel, "Meldedatei");
        wettPanel.add(jScrollPane1, BorderLayout.CENTER);
        mainPanel.add(jPanel3, BorderLayout.SOUTH);
        jScrollPane1.getViewport().add(wettDatei, null);
        jScrollPane1.setMaximumSize(new Dimension((int) Dialog.screenSize.getWidth() - 100, (int) Dialog.screenSize.getHeight() - 300));
        jScrollPane1.setPreferredSize(new Dimension((int) Dialog.screenSize.getWidth() - 300, (int) Dialog.screenSize.getHeight() - 400));
        infoPanel.add(kontoLabel, new GridBagConstraints(0, 11, 9, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        infoPanel.add(einsendenButton, new GridBagConstraints(8, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
        infoPanel.add(fahrtenhefte1Label, new GridBagConstraints(0, 14, 9, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
        infoPanel.add(fahrtenhefte2Label, new GridBagConstraints(0, 15, 9, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        infoPanel.add(fahrtenhefteAdresseLabel, new GridBagConstraints(0, 16, 9, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        infoPanel.add(fahrtenhefteScrollPane, new GridBagConstraints(0, 17, 9, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        infoPanel.add(meldegeld2Label, new GridBagConstraints(0, 13, 9, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        infoPanel.add(achtung1Label, new GridBagConstraints(0, 18, 9, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
        infoPanel.add(achtung2Label, new GridBagConstraints(0, 19, 9, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 10, 0), 0, 0));
        infoPanel.add(saveMeldedateiButton, new GridBagConstraints(8, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
        fahrtenhefteScrollPane.getViewport().add(fahrtenhefteList, null);
        infoPanel.add(anleitungButton, new GridBagConstraints(0, 4, 9, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 10, 0), 0, 0));

        iniFelder();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    void iniFelder() {
        dateinameLabel.setText(efaWett.datei);

        // 2. Meldegeld überweisen
        if (meldegeld == null) {
            this.meldegeld1Label.setVisible(false);
            this.meldegeldLabel.setVisible(false);
            this.meldegeld2Label.setVisible(false);
            this.kontoLabel.setVisible(false);
        } else {
            this.meldegeldLabel.setText(MELDEGELD_TEIL1 + meldegeld + MELDEGELD_TEIL2);
            switch (efaWett.wettId) {
                case WettDefs.DRV_FAHRTENABZEICHEN:
                    this.kontoLabel.setText(Daten.wettDefs.efw_drv_konto);
                    // Meldegeld für DRV wird erst nach Bearbeitung überwiesen
                    this.meldegeld1Label.setText("2. Vorraussichtliches Meldegeld");
                    this.meldegeld2Label.setText("Bitte das Meldegeld erst NACH Bearbeitung und Bestätigung durch DRV überweisen.");
                    this.meldegeldLabel.setText("Voraussichtliche Höhe des Meldegeldes: " + meldegeld);
                    this.kontoLabel.setVisible(false);
                    break;
                case WettDefs.LRVBERLIN_SOMMER:
                case WettDefs.LRVBERLIN_WINTER:
                case WettDefs.LRVBERLIN_BLAUERWIMPEL:
                    this.kontoLabel.setText(Daten.wettDefs.efw_lrvbln_konto);
                    break;
            }
        }

        // 3. Papier-Fahrtenhefte einsenden
        if (efaWett.wettId == WettDefs.DRV_FAHRTENABZEICHEN && papierFahrtenhefte != null && papierFahrtenhefte.size() > 0) {
            this.fahrtenhefteAdresseLabel.setText(Daten.wettDefs.efw_drv_anschrift);
            this.fahrtenhefteList.setListData(papierFahrtenhefte);
        } else {
            this.fahrtenhefte1Label.setVisible(false);
            this.fahrtenhefte2Label.setVisible(false);
            this.fahrtenhefteAdresseLabel.setVisible(false);
            this.fahrtenhefteList.setVisible(false);
            this.fahrtenhefteScrollPane.setVisible(false);
        }

        // Warnung
        if (Daten.wettDefs != null && Daten.wettDefs.isDataOld() && false) {
            // nah, don't show this any more
            this.achtung1Label.setVisible(true);
            this.achtung2Label.setVisible(true);
        } else {
            this.achtung1Label.setVisible(false);
            this.achtung2Label.setVisible(false);
        }


        try {
            BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(efaWett.datei), Daten.ENCODING_ISO));
            String z;
            while ((z = f.readLine()) != null) {
                wettDatei.append(z + "\n");
            }
            f.close();
        } catch (FileNotFoundException e) {
            wettDatei.append("DIE MELDEDATEI KONNTE NICHT GEFUNDEN WERDEN.\n");
        } catch (IOException e) {
            wettDatei.append("DIE MELDEDATEI KONNTE NICHT GELESEN WERDEN.\n");
        }
    }

    void einsendenButton_actionPerformed(ActionEvent e) {
        String tmpdatei = Daten.efaTmpDirectory + "einsenden.html";
        String verband = "";
        switch (efaWett.wettId) {
            case WettDefs.DRV_FAHRTENABZEICHEN:
            case WettDefs.DRV_WANDERRUDERSTATISTIK:
                verband = "drv";
                break;
            case WettDefs.LRVBERLIN_SOMMER:
            case WettDefs.LRVBERLIN_WINTER:
            case WettDefs.LRVBERLIN_BLAUERWIMPEL:
                verband = "lrvbln";
                break;
        }
        try {
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpdatei), Daten.ENCODING_ISO));
            f.write("<html>\n");
            f.write("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=" + Daten.ENCODING_ISO + "\"></head>\n");
            f.write("<body>\n");
            f.write("<h1 align=\"center\">Wettbewerbsmeldung einsenden</h1>\n");
            f.write("<table bgcolor=\"#eeeeee\" align=\"center\"><tr><td>\n");
            f.write("<form action=\"" + Daten.wettDefs.efw_url_einsenden + "\" method=\"post\">\n");
            f.write("<input name=\"verband\" type=\"hidden\" value=\"" + verband + "\">\n");
            f.write("<input name=\"datei_base64\" type=\"hidden\" value=\"");
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(efaWett.datei), Daten.ENCODING_ISO));
            StringBuilder s = new StringBuilder();
            String z;
            while ((z = r.readLine()) != null) {
                // Zeilenumbrüche als %#% maskieren, Anführungszeichen als %2% maskieren!
                s.append(EfaUtil.replace(z, "\"", "**2**", true) + "**#**");
            }
            r.close();
            f.write(Base64.encodeBytes(s.toString().getBytes(Daten.ENCODING_ISO)));
            f.write("\">\n");
            f.write("<table>\n");
            f.write("<tr><td>Wettbewerb:</td><td><tt>" + efaWett.allg_wett + " " + efaWett.allg_wettjahr + "</tt></td></tr>\n");
            f.write("<tr><td>Benutzername:</td><td><tt>" + efaWett.verein_user + "</tt></td></tr>\n");
            f.write("<tr><td>Paßwort:</td><td><input name=\"password\" type=\"password\" size=\"15\"></td></tr>\n");
            f.write("<tr><td colspan=\"2\"><input name=\"correctness\" type=\"checkbox\" value=\"x\">Hiermit erkläre ich, daß ich alle Daten der Meldung gewissenhaft auf ihre Richtigkeit überprüft habe.</input></td></tr>\n");
            f.write("</table>\n");
            f.write("<p align=\"center\"><input type=\"submit\" value=\"Meldedatei einsenden\"><input type=\"reset\" value=\"Abbruch\"></p>\n");
            f.write("<p align=\"center\"><b><font color=\"red\">Bitte stelle vor dem Einsenden der Meldedatei<br>eine Internet-Verbindung her!</font></b></p>\n");
            f.write("<p align=\"center\"><b><font color=\"blue\">Das Einsenden kann einige Sekunden dauern.<br>Bitte klicke nur einmal <i>Meldedatei einsenden</i>.</font></b></p>\n");
            f.write("</form>\n");
            f.write("</td></tr></table>\n");
            f.write("</body></html>\n");
            f.close();
        } catch (Exception ee) {
            Dialog.error("Fehler: " + ee.toString());
        }
        if (!Daten.javaVersion.startsWith("1.5.") && !Daten.javaVersion.startsWith("1.6.")) {
            BrowserDialog.openInternalBrowser(this, "Meldeddatei einsenden", "file:" + tmpdatei);
            EfaUtil.deleteFile(tmpdatei);
        } else {
            BrowserDialog.openExternalBrowser(this, "file:" + tmpdatei);
        }
    }

    boolean erstelleMeldeAnleitung(String fname) {
        String einsendenUrl = "";
        switch (efaWett.wettId) {
            case WettDefs.DRV_FAHRTENABZEICHEN:
            case WettDefs.DRV_WANDERRUDERSTATISTIK:
                einsendenUrl = Daten.wettDefs.efw_url_einsenden + "?verband=drv&redirect=yes";
                break;
            case WettDefs.LRVBERLIN_SOMMER:
            case WettDefs.LRVBERLIN_WINTER:
            case WettDefs.LRVBERLIN_BLAUERWIMPEL:
                einsendenUrl = Daten.wettDefs.efw_url_einsenden + "?verband=lrvbln&redirect=yes";
                break;
        }
        try {
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fname), Daten.ENCODING_ISO));
            f.write("<html>\n");
            f.write("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"></head>\n");
            f.write("<body>\n");
            f.write("<h1 align=\"center\">Anleitung zum Einsenden der Meldedatei</h1>\n");

            f.write("<h2>1. Meldedatei einsenden</h2>\n");
            f.write("<p>Damit der Ruderverband die elektronische Meldung erhält, muß die von efa erstellte Meldedatei über das Internet\n");
            f.write("eingesandt werden. Dies kann direkt aus efa heraus über den Button <i>Meldedatei jetzt einsenden</i> erfolgen,\n");
            f.write("oder aber zu einem späteren Zeitpunkt von diesem oder einem anderen Computer aus. In letzterem Fall benutze\n");
            f.write("bitte den Button <i>Meldedatei abspeichern</i>, um die Meldedatei abzuspeichern (z.B. auf einem USB-Stick, um\n");
            f.write("sie zu einem anderen Computer zu tragen).</p>\n");

            f.write("<h3>1.1 Meldedatei direkt aus efa heraus einsenden</h3>\n");
            f.write("<p>Um die Meldedatei direkt aus efa heraus einzusenden, klicke nach dem Erstellen der Meldedatei den Button\n");
            f.write("<i>Meldedatei jetzt einsenden</i>. Es öffnet sich ein Browser-Fenster, in dem Du nun bitte in das Feld\n");
            f.write("<i>Paßwort</i> das efaWett-Paßwort Deines Vereins einträgst (dieses hast Du bei der Registrierung für die\n");
            f.write("elektronische Meldung angegeben). Anschließend klicke bitte den Button <i>Meldedatei einsenden</i>.<br>\n");
            f.write("Sollte das Einsenden fehlschlagen, verfahre bitte wie unter 1.2 beschrieben.</p>\n");

            f.write("<h3>1.2 Meldedatei abspeichern und später über einen Webbrowser einsenden</h3>\n");
            f.write("<p>Wenn Du die Meldedatei nicht aus efa heraus einsenden kannst oder willst (z.B. weil der PC keinen Internet-Anschluß\n");
            f.write("hat), dann klicke bitte den Button <i>Meldedatei abspeichern</i>, um die Meldedatei abzuspeichern. Wähle beispielsweise\n");
            f.write("als Ort einen USB-Stick, um die Meldedatei zu einem anderen Computer mit Internet-Zugang zu tragen.<br>\n");
            f.write("Öffne an diesem Computer die Adresse <a href=\"" + einsendenUrl + "\" target=\"_blank\">" + einsendenUrl + "</a>\n");
            f.write("in einem Webbrowser. Klicke auf der Seite auf den Button <i>Durchsuchen</i>, um die zuvor abgespeicherte Meldedatei\n");
            f.write("<i>" + EfaUtil.getFilenameWithoutPath(efaWett.datei) + "</i>");
            f.write("auszuwählen. Trage dann in das Feld <i>Paßwort</i> das efaWett-Paßwort Deines Vereins ein, welches Du bei der Registrierung\n");
            f.write("für die elektronische Meldung angegeben hast. Anschließend klicke bitte den Button <i>Meldedatei einsenden</i>.</p>\n");

            if (meldegeld != null) {
                f.write("<h2>2. Meldegeld überweisen</h2>\n");
                f.write("<p>Das von efa berechnete Meldegeld beträgt <b>" + meldegeld + "</b>. Der verbindliche Betrag wird Dir unmittelbar\n");
                f.write("nach Einsenden der Meldung angezeigt und zusätzlich per email zugeschickt. Bitte überweise dieses Meldegeld\n");
                f.write("zügig nach Einsenden der Meldung auf folgendes Konto:<br>\n<b>");
                switch (efaWett.wettId) {
                    case WettDefs.DRV_FAHRTENABZEICHEN:
                        f.write(Daten.wettDefs.efw_drv_konto);
                        break;
                    case WettDefs.LRVBERLIN_SOMMER:
                    case WettDefs.LRVBERLIN_WINTER:
                    case WettDefs.LRVBERLIN_BLAUERWIMPEL:
                        f.write(Daten.wettDefs.efw_lrvbln_konto);
                        break;
                }
                f.write("</b></p>\n");
            }

            if (efaWett.wettId == WettDefs.DRV_FAHRTENABZEICHEN && papierFahrtenhefte != null && papierFahrtenhefte.size() > 0) {
                f.write("<h2>3. Papier-Fahrtenhefte einsenden</h3>\n");
                f.write("<p>Für alle Teilnehmer am Wettbewerb, die zum ersten Mal elektronisch gemeldet werden, aber bereits zuvor\n");
                f.write("Fahrtenabzeichen auf herkömmliche Weise erworben haben, müssen einmalig zum Nachweis der bereits erworbenen\n");
                f.write("Fahrtenabzeichen die Papier-Fahrtenhefte eingeschickt werden. Diese Papier-Fahrtenhefte dienen nur zum Nachweis\n");
                f.write("bereits erworbener Abzeichen und sollen <b>nicht</b> für das aktuelle Meldejahr ausgefüllt werden.<br>\n");
                f.write("Bitte schicke in einem Brief die Papier-Fahrtenhefte folgender Mitglieder ein:<br>\n<ul>\n");
                for (int i = 0; i < papierFahrtenhefte.size(); i++) {
                    f.write("<li>" + (String) papierFahrtenhefte.get(i) + "</li>\n");
                }
                f.write("</ul>\n");
                f.write("Bitte gib in dem Brief Vereinsnamen und Quittungsnummer der elektronischen Meldung an. Abgesehen von den\n");
                f.write("Fahrtenheften sind keine weiteren Unterlagen einzusenden. Insbesondere sollen die herkömmlichen Meldebögen\n");
                f.write("<b>nicht</b> ausgefüllt werden.</p>\n");
            } else {
                f.write("<h2>Hinweis</h2>\n");
                f.write("<p>Die Meldung erfolgt ausschließlich elektronisch. Es müssen keine weiteren Daten auf herkömmlichem Weg\n");
                f.write("eingereicht werden.</p>\n");
            }

            f.write("<h2>Fragen und Probleme</h2>\n");
            f.write("<p>Bei technischen Fragen zum Vorgehen für die elektronische Meldung, benutze bitte das Support-Forum unter \n");
            f.write("<a href=\"" + Daten.EFASUPPORTURL + "\">" + Daten.EFASUPPORTURL + "</a>. Bei inhaltlichen Fragen zur Ausschreibung und dem Wettbewerb allgemein,\n");
            f.write("wende Dich bitte an den jeweiligen Ansprechpartner des Ruderverbandes.</p>");

            f.write("</body></html>\n");
            f.close();
        } catch (Exception ee) {
            return false;
        }
        return true;
    }

    void saveMeldedateiButton_actionPerformed(ActionEvent e) {
        String dir = Dialog.dateiDialog(this, "Ordner auswählen zum Speichern der Meldedatei", "Ordner", "", Daten.efaDataDirectory, null, "Ordner auswählen", true, true);
        if (dir == null || dir.length() == 0) {
            return;
        }
        if (!dir.endsWith(Daten.fileSep)) {
            dir += Daten.fileSep;
        }
        if (!(new File(dir)).isDirectory()) {
            Dialog.error("Verzeichnis " + dir + " nicht gefunden. Meldedatei konnte nicht gespeichert werden.");
            return;
        }
        String toFile = dir + EfaUtil.getFilenameWithoutPath(efaWett.datei);
        if (!(new File(efaWett.datei)).equals(new File(toFile)) && !EfaUtil.copyFile(efaWett.datei, toFile)) {
            Dialog.error("Kopieren der Meldedatei nach " + dir + " ist fehlgeschlagen.");
            return;
        }
        String anleitungFile = dir + "Meldedatei_Einsenden_Anleitung.html";
        erstelleMeldeAnleitung(anleitungFile);
        Dialog.infoDialog("Meldedatei erfolgreich gespeichert",
                "Die Meldedatei wurde erfolgreich unter dem Namen\n" + toFile + "\nabgespeichert.\n"
                + "Außerdem wurde eine Anleitung erstellt, die beschreibt, wie die Meldedatei eingesandt\n"
                + "werden soll. Diese Anleitung kann im Webbrowser geöffnet werden:\n" + anleitungFile);
    }

    void anleitungButton_actionPerformed(ActionEvent e) {
        String tmpfile = Daten.efaTmpDirectory + "meldeanleitung.html";
        if (!erstelleMeldeAnleitung(tmpfile)) {
            Dialog.error("Erstellen der Anleitung ist fehlgeschlagen.");
            return;
        }
        BrowserDialog.openInternalBrowser(this, "Anleitung zum Einsenden von Meldedateien", "file:" + tmpfile);
        EfaUtil.deleteFile(tmpfile);
    }
}
