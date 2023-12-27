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
import de.nmichael.efa.data.efawett.DRVSignatur;
import de.nmichael.efa.data.efawett.EfaWettMeldung;
import de.nmichael.efa.data.efawett.EfaWett;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.*;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.EfaBaseFrame;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class EfaWettSelectAndCompleteDialog extends BaseDialog implements ActionListener {

    static final int T_LABEL = 1;
    static final int T_TEXTFIELD = 2;
    static final int T_CHECKBOX = 3;
    static final int C_MELDEN = 1;
    static final int C_ANSCHRIFT = 2;
    static final int C_BESTELLEN1 = 3;
    static final int C_BESTELLEN2 = 4;

    private String resultMeldegeld;
    private Vector resultPapierFahrtenhefte;
    private static final String LFDNR_UA = ", ...";

    int anzahl;
    EfaWett efaWett = null;
    AdminRecord admin = null;
    StatisticsRecord sr = null;
    Hashtable checkboxes;
    Hashtable textfields;
    Hashtable papierFahrtenheftErforderlich;
    int anzBestellErwEinf = 0;
    int[] anzBestellErwGold = new int[EfaWettMeldung.ABZEICHEN_ERW_GOLD_LIST.length];
    int anzBestellJugEinf = 0;
    int[] anzBestellJugGold = new int[EfaWettMeldung.ABZEICHEN_JUG_GOLD_LIST.length];
    int anzBestellErwStoff = 0;
    int anzBestellJugStoff = 0;
    int meldegeld = 0;
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel jPanel1 = new JPanel();
    JScrollPane teilnehmerScrollPane = new JScrollPane();
    JPanel teilnehmerInfoPanel = new JPanel();
    JLabel jLabelTitel = new JLabel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel teilnLabel = new JLabel();
    JLabel teilnAnz = new JLabel();
    JPanel dataPanel = new JPanel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JLabel teiln1Label = new JLabel();
    JLabel teiln2Label = new JLabel();
    JLabel teiln3Label = new JLabel();
    JLabel teiln4Label = new JLabel();
    JLabel gebLabel = new JLabel();
    JLabel teiln1Anz = new JLabel();
    JLabel teiln2Anz = new JLabel();
    JLabel teiln3Anz = new JLabel();
    JLabel teiln4Anz = new JLabel();
    JLabel meldungenLabel = new JLabel();
    JLabel gebuehrLabel = new JLabel();
    JLabel geb1Label = new JLabel();
    JLabel geb = new JLabel();
    JLabel geb1 = new JLabel();
    JLabel geb2 = new JLabel();
    JLabel geb2Label = new JLabel();
    JLabel bestellungenLabel = new JLabel();
    JLabel bestLabel1 = new JLabel();
    JLabel best = new JLabel();
    JLabel bestLabel2 = new JLabel();
    JLabel bestLabel3 = new JLabel();
    JLabel bestLabel4 = new JLabel();
    JLabel bestLabel5 = new JLabel();
    JLabel best4 = new JLabel();
    JLabel best3 = new JLabel();
    JLabel best2 = new JLabel();
    JLabel best1 = new JLabel();
    JLabel bestLabel6 = new JLabel();
    JLabel bestLabel = new JLabel();
    JLabel best5 = new JLabel();
    JLabel best6 = new JLabel();
    JLabel teiln5Label = new JLabel();
    JLabel teiln5Anz = new JLabel();
    JLabel checkDataLabel = new JLabel();
    GridBagLayout gridBagLayout3 = new GridBagLayout();

    public EfaWettSelectAndCompleteDialog(JDialog parent, EfaWett efaWett, AdminRecord admin,
            StatisticsRecord sr) {
        super(parent,
                International.onlyFor("Meldedaten", "de"),
                International.onlyFor("Meldedatei erstellen", "de"));
        this.efaWett = efaWett;
        this.admin = admin;
        this.sr = sr;
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());
        jLabelTitel.setText("Bitte markiere alle Teilnehmer, die für den Wettbewerb gemeldet werden "
                + "sollen:");
        jPanel1.setLayout(gridBagLayout1);
        teilnLabel.setText("Gesamtanzahl der Teilnehmer: ");
        teilnAnz.setForeground(Color.black);
        teilnAnz.setText("23");
        dataPanel.setLayout(gridBagLayout2);
        teiln1Label.setText("Erwachsene (einfach): ");
        teiln2Label.setText("Erwachsene (gold): ");
        teiln3Label.setText("Jugend (einfach): ");
        teiln4Label.setText("Jugend (gold): ");
        gebLabel.setText("gesamt: ");
        teiln1Anz.setForeground(Color.black);
        teiln1Anz.setText("8");
        teiln2Anz.setForeground(Color.black);
        teiln2Anz.setText("5");
        teiln3Anz.setForeground(Color.black);
        teiln3Anz.setText("5");
        teiln4Anz.setForeground(Color.black);
        teiln4Anz.setText("5");
        meldungenLabel.setHorizontalAlignment(SwingConstants.CENTER);
        meldungenLabel.setText("Meldungen");
        gebuehrLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gebuehrLabel.setText("Meldegebühr");
        geb1Label.setText("Erwachsene: ");
        geb.setForeground(Color.black);
        geb.setText("23,-");
        geb1.setForeground(Color.black);
        geb1.setText("13 x 2,-");
        geb2.setForeground(Color.black);
        geb2.setText("10 x 2,-");
        geb2Label.setText("Jugend: ");
        bestellungenLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bestellungenLabel.setText("weitere Bestellungen");
        bestLabel1.setText("Nadel Erwachsene (silber): ");
        best.setForeground(Color.black);
        best.setText("23,-");
        bestLabel2.setText("Nadel Erwachsene (gold): ");
        bestLabel3.setText("Nadel Jugend (silber): ");
        bestLabel4.setText("Nadel Jugend (gold): ");
        bestLabel5.setText("Stoffabzeichen Erwachsene (gold): ");
        best4.setForeground(Color.black);
        best4.setText("1 x 3,-");
        best3.setForeground(Color.black);
        best3.setText("1 x 3,-");
        best2.setForeground(Color.black);
        best2.setText("1 x 4,75");
        best1.setForeground(Color.black);
        best1.setText("1 x 3,60");
        bestLabel6.setText("Stoffabzeichen Jugend: ");
        bestLabel.setText("gesamt: ");
        best5.setForeground(Color.black);
        best5.setText("1 x 4,81");
        best6.setForeground(Color.black);
        best6.setText("1 x 3,48");
        teiln5Label.setText("Äquatorpreise: ");
        teiln5Anz.setForeground(Color.black);
        teiln5Anz.setText("0");
        checkDataLabel.setForeground(Color.red);
        checkDataLabel.setHorizontalAlignment(SwingConstants.CENTER);
        checkDataLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        checkDataLabel.setText("Bitte alle Daten gründlich auf Richtigkeit prüfen (insb. die rot/orange "
                + "markierten Daten)!");
        teilnehmerInfoPanel.setLayout(gridBagLayout3);
        mainPanel.add(jPanel1, BorderLayout.SOUTH);
        jPanel1.add(teilnLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(teilnAnz, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(teiln1Label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(teiln2Label, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(teiln3Label, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(teiln4Label, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        mainPanel.add(teilnehmerScrollPane, BorderLayout.CENTER);
        teilnehmerScrollPane.getViewport().add(dataPanel, null);
        mainPanel.add(teilnehmerInfoPanel, BorderLayout.NORTH);
        teilnehmerInfoPanel.add(jLabelTitel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
        teilnehmerInfoPanel.add(checkDataLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 5, 0), 0, 0));
        jPanel1.add(gebLabel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jPanel1.add(teiln1Anz, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(teiln2Anz, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(teiln3Anz, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(teiln4Anz, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(meldungenLabel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(gebuehrLabel, new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(geb1Label, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jPanel1.add(geb, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(geb1, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(geb2, new GridBagConstraints(3, 3, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(geb2Label, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jPanel1.add(bestellungenLabel, new GridBagConstraints(5, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(bestLabel1, new GridBagConstraints(5, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jPanel1.add(best, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(bestLabel2, new GridBagConstraints(5, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jPanel1.add(bestLabel3, new GridBagConstraints(5, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jPanel1.add(bestLabel4, new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jPanel1.add(bestLabel5, new GridBagConstraints(5, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jPanel1.add(best4, new GridBagConstraints(6, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(best3, new GridBagConstraints(6, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(best2, new GridBagConstraints(6, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(best1, new GridBagConstraints(6, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(bestLabel6, new GridBagConstraints(5, 7, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jPanel1.add(bestLabel, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        jPanel1.add(best5, new GridBagConstraints(6, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(best6, new GridBagConstraints(6, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(teiln5Label, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jPanel1.add(teiln5Anz, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        labelFelder(efaWett);
        iniFelder(efaWett);
        teilnehmerScrollPane.setMaximumSize(new Dimension((int) Dialog.screenSize.getWidth() - 100, (int) Dialog.screenSize.getHeight() - 300));
        teilnehmerScrollPane.setPreferredSize(new Dimension((int) Dialog.screenSize.getWidth() - 100, (int) Dialog.screenSize.getHeight() - 300));

        closeButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        closeButton.setIconTextGap(10);

        (new WarningWindow()).start();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    void labelFelder(EfaWett ew) {
        // Teilnehmer
        if (ew.wettId == WettDefs.DRV_FAHRTENABZEICHEN) {
            this.teiln1Label.setText("Erwachsene: ");
            this.teiln2Label.setText("Jugendliche: ");
            this.teiln3Label.setVisible(false);
            this.teiln3Anz.setVisible(false);
            this.teiln4Label.setVisible(false);
            this.teiln4Anz.setVisible(false);
        } else if (ew.wettId == WettDefs.DRV_WANDERRUDERSTATISTIK) {
            this.teilnLabel.setText("Anzahl der aktiven Mitglieder: ");
            this.teiln1Label.setText("Aktive Mitglieder (männlich ab 19): ");
            this.teiln2Label.setText("Aktive Mitglieder (männlich bis 18): ");
            this.teiln3Label.setText("Aktive Mitglieder (weiblich ab 19): ");
            this.teiln4Label.setText("Aktive Mitglieder (weiblich bis 18): ");
        } else if (ew.wettId == WettDefs.LRVBERLIN_BLAUERWIMPEL) {
            this.meldungenLabel.setText("Ergebnisse für 'Blauer Wimpel'");
            if (false && EfaUtil.string2int(ew.allg_wettjahr, 0) >= 2015) {
                this.teilnLabel.setVisible(false);
                this.teilnAnz.setVisible(false);
            }
            this.teiln1Label.setText("gewertete Mitglieder: ");
            this.teiln2Label.setText("Gesamtkilometer der gewerteten Mitglieder: ");
            this.teiln3Label.setText("Kilometer pro gewertetes Mitglied: ");
            this.teiln4Label.setVisible(false);
            this.teiln4Anz.setVisible(false);
        } else if (ew.wettId == WettDefs.LRVBERLIN_SOMMER || ew.wettId == WettDefs.LRVBERLIN_WINTER) {
            this.teiln1Label.setText("Gruppe 1: ");
            this.teiln2Label.setText("Gruppe 2: ");
            this.teiln3Label.setText("Gruppe 3: ");
            this.teiln4Label.setText("Gruppe 4: ");
        } else {
            this.meldungenLabel.setVisible(false);
            this.teilnLabel.setVisible(false);
            this.teiln1Label.setVisible(false);
            this.teiln2Label.setVisible(false);
            this.teiln3Label.setVisible(false);
            this.teiln4Label.setVisible(false);
            this.teilnAnz.setVisible(false);
            this.teiln1Anz.setVisible(false);
            this.teiln2Anz.setVisible(false);
            this.teiln3Anz.setVisible(false);
            this.teiln4Anz.setVisible(false);
        }
        if (ew.wettId != WettDefs.DRV_FAHRTENABZEICHEN) {
            if (ew.wettId == WettDefs.DRV_WANDERRUDERSTATISTIK) {
                jLabelTitel.setText("Bitte markiere alle Wanderfahrten, die für den Wettbewerb gemeldet werden sollen:");
            }
            this.teiln5Label.setVisible(false);
            this.teiln5Anz.setVisible(false);

            // Meldegebühr
            if (ew.wettId == WettDefs.LRVBERLIN_BLAUERWIMPEL || ew.wettId == WettDefs.DRV_WANDERRUDERSTATISTIK) {
                this.geb1.setVisible(false);
                this.geb2.setVisible(false);
                this.geb1Label.setVisible(false);
                this.geb2Label.setVisible(false);
            }

            // Bestellungen
            this.bestellungenLabel.setVisible(false);
            this.bestLabel.setVisible(false);
            this.bestLabel1.setVisible(false);
            this.bestLabel2.setVisible(false);
            this.bestLabel3.setVisible(false);
            this.bestLabel4.setVisible(false);
            this.bestLabel5.setVisible(false);
            this.bestLabel6.setVisible(false);
            this.best.setVisible(false);
            this.best1.setVisible(false);
            this.best2.setVisible(false);
            this.best3.setVisible(false);
            this.best4.setVisible(false);
            this.best5.setVisible(false);
            this.best6.setVisible(false);
        }
    }

    void addField(EfaWettMeldung m, int content, int type, String text, int x, int y, int width, int height, int align, Color color, boolean checked) {
        switch (type) {
            case T_LABEL:
                JLabel l = new JLabel();
                l.setText(text);
                if (width > 0 && height > 0) {
                    l.setPreferredSize(new Dimension(width, height));
                }
                if (color != null) {
                    l.setForeground(color);
                }
                dataPanel.add(l, new GridBagConstraints(x, y, 1, 1, 0.0, 0.0, align, GridBagConstraints.NONE, new Insets(0, 10, 0, 10), 0, 0));
                break;
            case T_CHECKBOX:
                JCheckBox c = new JCheckBox();
                c.setText(text);
                if (width > 0 && height > 0) {
                    c.setPreferredSize(new Dimension(width, height));
                }
                if (color != null) {
                    c.setForeground(color);
                }
                c.setSelected(checked);
                c.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        calculateValues();
                    }
                });
                dataPanel.add(c, new GridBagConstraints(x, y, 1, 1, 0.0, 0.0, align, GridBagConstraints.NONE, new Insets(0, 10, 0, 10), 0, 0));
                this.checkboxes.put(c, new Item(m, content));
                break;
            case T_TEXTFIELD:
                JTextField t = new JTextField();
                t.setText(text);
                if (width > 0 && height > 0) {
                    t.setPreferredSize(new Dimension(width, height));
                }
                if (color != null) {
                    t.setForeground(color);
                }
                dataPanel.add(t, new GridBagConstraints(x, y, 1, 1, 0.0, 0.0, align, GridBagConstraints.NONE, new Insets(0, 10, 0, 10), 0, 0));
                this.textfields.put(t, new Item(m, content));
                break;
        }
    }

    void addLink(EfaWettMeldung m, int content, int type, String text, int x, int y, int width, int height, int align, boolean checked) {
        switch (type) {
            case T_LABEL:
                JLabel l = new JLabel();
                l.setText(text);
                l.addMouseListener(new java.awt.event.MouseAdapter() {

                    public void mouseClicked(MouseEvent e) {
                        linkMouseClicked(e);
                    }

                    public void mouseEntered(MouseEvent e) {
                        linkMouseEntered(e);
                    }

                    public void mouseExited(MouseEvent e) {
                        linkMouseExited(e);
                    }
                });
                if (width > 0 && height > 0) {
                    l.setPreferredSize(new Dimension(width, height));
                }
                l.setForeground(Color.blue);
                dataPanel.add(l, new GridBagConstraints(x, y, 1, 1, 0.0, 0.0, align, GridBagConstraints.NONE, new Insets(0, 10, 0, 10), 0, 0));
                break;
        }
    }

    private void linkMouseClicked(MouseEvent e) {
        JLabel label = (JLabel) e.getSource();
        String entryNo = label.getText();
        int pos;
        if ( (pos = entryNo.indexOf(LFDNR_UA)) > 0) {
            entryNo = entryNo.substring(0, pos);
        }
        EfaBaseFrame dlg = new EfaBaseFrame(this, EfaBaseFrame.MODE_ADMIN, admin,
                Daten.project.getCurrentLogbook(), entryNo);
        dlg.showDialog();
    }

    private void linkMouseEntered(MouseEvent e) {
        try {
            JLabel label = (JLabel) e.getSource();
            label.setForeground(Color.red);
        } catch (Exception eignore) {
        }
    }

    private void linkMouseExited(MouseEvent e) {
        try {
            JLabel label = (JLabel) e.getSource();
            label.setForeground(Color.blue);
        } catch (Exception eignore) {
        }
    }

    void iniFelder(EfaWett ew) {
        boolean displayGruppe = false;
        boolean displayLfdNr = false;
        boolean displayStrecke = false;
        boolean displayGewaesser = false;
        boolean displayTage = false;
        boolean displayKm = false;
        boolean displayTeilnehmer = false;
        boolean displayAnschrift = false;
        boolean displayDRVFahrtenheft = false;
        boolean displayAbzeichen = false;

        checkboxes = new Hashtable();
        textfields = new Hashtable();
        papierFahrtenheftErforderlich = new Hashtable();

        EfaWettMeldung m = ew.meldung;
        this.anzahl = 0;
        while (m != null) {
            this.anzahl++;
            if (m.gruppe != null || m.jahrgang != null) {
                displayGruppe = true;
            }
            if (m.drvWS_LfdNr != null) {
                displayLfdNr = true;
            }
            if (m.drvWS_Strecke != null) {
                displayStrecke = true;
            }
            if (m.drvWS_Gewaesser != null) {
                displayGewaesser = true;
            }
            if (m.drvWS_Tage != null) {
                displayTage = true;
            }
            if (m.kilometer != null || m.drvWS_Km != null) {
                displayKm = true;
            }
            if (m.drvWS_Teilnehmer != null) {
                displayTeilnehmer = true;
            }
            if (m.abzeichen != null) {
                displayAbzeichen = true;
            }
            if (m.anschrift != null) {
                displayAnschrift = true;
            }
            if (m.drv_anzAbzeichen != null || m.drv_gesKm != null || m.drv_fahrtenheft != null) {
                displayDRVFahrtenheft = true;
            }
            m = m.next;
        }

        int y = 0;
        int x = 0;
        // Melden Name Gruppe Km Anschrift(2x) AnzAbzeichen GesKm Fahrtenheft Nadel Stoff
        addField(null, 0, T_LABEL, "Melden", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        if (displayLfdNr) {
            addField(null, 0, T_LABEL, "LfdNr", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }
        if (ew.wettId != WettDefs.DRV_WANDERRUDERSTATISTIK) {
            addField(null, 0, T_LABEL, "Name", x++, y, 0, 0, GridBagConstraints.WEST, null, false);
        } else {
            addField(null, 0, T_LABEL, "Bezeichnung", x++, y, 0, 0, GridBagConstraints.WEST, null, false);
        }
        if (displayGruppe) {
            addField(null, 0, T_LABEL, "Gruppe", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }
        if (displayStrecke) {
            addField(null, 0, T_LABEL, "Strecke", x++, y, 0, 0, GridBagConstraints.WEST, null, false);
        }
        if (displayGewaesser) {
            addField(null, 0, T_LABEL, "Gewaesser", x++, y, 0, 0, GridBagConstraints.WEST, null, false);
        }
        if (displayTage) {
            addField(null, 0, T_LABEL, "Tage", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }
        if (displayKm) {
            addField(null, 0, T_LABEL, "Kilometer", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }
        if (displayTeilnehmer) {
            addField(null, 0, T_LABEL, "Teilnehmer", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }
        if (displayAnschrift) {
            addField(null, 0, T_LABEL, "Anschrift", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }
        if (displayDRVFahrtenheft) {
            addField(null, 0, T_LABEL, "Ges.Abz.", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }
        if (displayDRVFahrtenheft) {
            addField(null, 0, T_LABEL, "Ges.Km.", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }
        if (displayAbzeichen) {
            addField(null, 0, T_LABEL, "Anstecknadel", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }
        if (displayAbzeichen) {
            addField(null, 0, T_LABEL, "Stoffabzeichen", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }
        if (ew.wettId == WettDefs.DRV_FAHRTENABZEICHEN) {
            addField(null, 0, T_LABEL, "Bemerkungen", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
        }

        m = ew.meldung;
        while (m != null) {
            x = 0;
            y++;

            DRVSignatur drvSignatur = null;
            if (m.drv_fahrtenheft != null && m.drv_fahrtenheft.length() > 0) {
                drvSignatur = new DRVSignatur(m.drv_fahrtenheft);
            }

            // Melden (Checkbox)
            addField(m, C_MELDEN, T_CHECKBOX, "", x++, y, 0, 0, GridBagConstraints.CENTER, null, true);

            // LfdNr (Label)
            if (displayLfdNr) {
                String lfdnr = m.drvWS_LfdNr;
                int pos;
                if (lfdnr != null && (pos = lfdnr.indexOf(", ")) > 0) {
                    lfdnr = lfdnr.substring(0, pos) + LFDNR_UA;
                }
                if (lfdnr != null && lfdnr.length() > 0 &&
                    admin != null && admin.isAllowedEditLogbook()) {
                    addLink(m, 0, T_LABEL, lfdnr, x++, y, 0, 0, GridBagConstraints.EAST, false);
                } else {
                    addField(m, 0, T_LABEL, (lfdnr != null ? lfdnr : ""), x++, y, 0, 0, GridBagConstraints.EAST, Color.black, false);
                }
            }

            // Name (Label)
            if (ew.wettId != WettDefs.DRV_WANDERRUDERSTATISTIK) {
                addField(m, 0, T_LABEL, (m.vorname != null ? m.vorname + " " : "") + (m.nachname != null ? m.nachname : ""), x++, y, 0, 0, GridBagConstraints.WEST, Color.black, false);
            } else {
                addField(m, 0, T_LABEL, (m.drvWS_StartZiel != null ? EfaUtil.trimto(m.drvWS_StartZiel, 40, true) : ""), x++, y, 0, 0, GridBagConstraints.WEST, Color.black, false);
            }

            // Gruppe (Label)
            if (displayGruppe) {
                String gruppe = m.gruppe;
                if (gruppe != null && gruppe.length() > 10) {
                    gruppe = gruppe.substring(0, 10) + ".";
                    if (gruppe.indexOf("(") >= 0 && gruppe.indexOf(")") < 0) {
                        gruppe += ")";
                    }
                }
                addField(m, 0, T_LABEL, (gruppe != null ? gruppe + " " : "")
                        + (m.jahrgang != null ? "(" + m.jahrgang + (m.geschlecht != null ? "; " + m.geschlecht : "") + ")" : ""),
                        x++, y, 0, 0, GridBagConstraints.WEST, null, false);
            }

            // Strecke (Label)
            if (displayStrecke) {
                addField(m, 0, T_LABEL, (m.drvWS_Strecke != null ? EfaUtil.trimto(m.drvWS_Strecke, 30, true) : ""), x++, y, 0, 0, GridBagConstraints.WEST, Color.black, false);
            }

            // Gewaesser (Label)
            if (displayGewaesser) {
                addField(m, 0, T_LABEL, (m.drvWS_Gewaesser != null ? EfaUtil.trimto(m.drvWS_Gewaesser, 25, true) : ""), x++, y, 0, 0, GridBagConstraints.WEST, Color.black, false);
            }

            // Tage (Label)
            if (displayTage) {
                addField(m, 0, T_LABEL, (m.drvWS_Tage != null ? m.drvWS_Tage : ""), x++, y, 0, 0, GridBagConstraints.CENTER, Color.black, false);
            }

            // Kilometer (Label)
            if (displayKm) {
                String km = m.kilometer;
                if (km == null) {
                    km = m.drvWS_Km;
                }
                addField(m, 0, T_LABEL, (km != null ? km : ""), x++, y, 0, 0, GridBagConstraints.CENTER, Color.black, false);
            }

            // Teilnehmer (Label)
            if (displayTeilnehmer) {
                addField(m, 0, T_LABEL, (m.drvWS_Teilnehmer != null ? m.drvWS_Teilnehmer : ""), x++, y, 0, 0, GridBagConstraints.CENTER, Color.black, false);
            }

            // Anschrift (Textfield)
            if (displayAnschrift) {
                if (m.anschrift != null) {
                    addField(m, C_ANSCHRIFT, T_TEXTFIELD, m.anschrift, x++, y, 200, 19, GridBagConstraints.WEST, null, false);
                } else {
                    x++;
                }
            }

            // DRV Fahrtenheft
            if (displayDRVFahrtenheft) {

                // Abzeichen / Kilometer bisher (Label)
                boolean displayAB = EfaUtil.string2date(ew.allg_wettjahr, 0, 0, 0).tag < 2007;
                String s = null;
                if (drvSignatur != null) {
                    s = Integer.toString(drvSignatur.getAnzAbzeichen());
                    if (drvSignatur.getAnzAbzeichenAB() > 0 && displayAB) {
                        s += " (" + drvSignatur.getAnzAbzeichenAB() + ")";
                    }
                } else {
                    s = (m.drv_anzAbzeichen != null ? m.drv_anzAbzeichen : "0");
                    if (m.drv_anzAbzeichenAB != null && EfaUtil.string2int(m.drv_anzAbzeichenAB, 0) > 0 && displayAB) {
                        s += " (" + m.drv_anzAbzeichenAB + ")";
                    }
                }
                addField(m, 0, T_LABEL, s, x++, y, 0, 0, GridBagConstraints.CENTER, Color.black, false);

                if (drvSignatur != null) {
                    s = Integer.toString(drvSignatur.getGesKm());
                    if (drvSignatur.getGesKmAB() > 0 && displayAB) {
                        s += " (" + drvSignatur.getGesKmAB() + ")";
                    }
                } else {
                    s = (m.drv_gesKm != null ? m.drv_gesKm : "0");
                    if (m.drv_gesKmAB != null && EfaUtil.string2int(m.drv_gesKmAB, 0) > 0 && displayAB) {
                        s += " (" + m.drv_gesKmAB + ")";
                    }
                }
                addField(m, 0, T_LABEL, s, x++, y, 0, 0, GridBagConstraints.CENTER, Color.black, false);
            }

            if (displayAbzeichen) {
                if (m.abzeichen != null) {
                    String s = null;
                    boolean abzSelected = false;
                    if (m.abzeichen.equals(EfaWettMeldung.ABZEICHEN_ERW_EINF)) {
                        s = "Erw. silber";
                    }
                    if (m.abzeichen.startsWith(EfaWettMeldung.ABZEICHEN_ERW_GOLD_PRAEFIX)) {
                        s = "Erw. gold";
                        abzSelected = true;
                    }
                    if (m.abzeichen.equals(EfaWettMeldung.ABZEICHEN_JUG_EINF)) {
                        s = "Jug. silber";
                    }
                    if (m.abzeichen.startsWith(EfaWettMeldung.ABZEICHEN_JUG_GOLD_PRAEFIX)) {
                        s = "Jug. gold";
                        abzSelected = true;
                    }
                    if (m.abzeichen.length() > 0
                            && (drvSignatur == null && EfaUtil.string2int(m.drv_anzAbzeichen, 0) == 0)
                            || (drvSignatur != null && drvSignatur.getAnzAbzeichen() == 0)) {
                        abzSelected = true;
                    }
                    addField(m, C_BESTELLEN1, T_CHECKBOX, s, x++, y, 0, 0, GridBagConstraints.CENTER, null, abzSelected);

                    if (m.abzeichen.startsWith(EfaWettMeldung.ABZEICHEN_ERW_GOLD_PRAEFIX)
                            || m.abzeichen.equals(EfaWettMeldung.ABZEICHEN_JUG_EINF)
                            || m.abzeichen.startsWith(EfaWettMeldung.ABZEICHEN_JUG_GOLD_PRAEFIX)) {
                        addField(m, C_BESTELLEN2, T_CHECKBOX, "bestellen", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
                    } else {
                        addField(null, 0, T_LABEL, "nur bei Gold", x++, y, 0, 0, GridBagConstraints.CENTER, null, false);
                    }
                }
            }

            // Bemerkungen
            if (ew.wettId == WettDefs.DRV_FAHRTENABZEICHEN) {
                // Fahrtenheft (Label)
                String s = null;
                Color c = Color.black;
                if (drvSignatur == null) {
                    if (m.drv_anzAbzeichen != null && m.drv_anzAbzeichen.length() > 0) {
                        s = "Papier-Fahrtenheft erforderlich!";
                        c = Color.red;
                        papierFahrtenheftErforderlich.put(m, "dummy");
                    } else {
                        s = "erstes Fahrtenabzeichen (?)";
                        c = Daten.colorOrange;
                    }
                } else {
                    if (drvSignatur.getJahr() == EfaUtil.string2int(ew.allg_wettjahr, 0) - 1) {
                        s = "elektronisches Fahrtenheft aus " + drvSignatur.getJahr()
                                + " vorhanden";
                        c = Daten.colorGreen;
                    } else {
                        s = "elektronisches Fahrtenheft aus " + drvSignatur.getJahr()
                                + " (?) vorhanden";
                        c = Daten.colorOrange;
                    }

                }
                if (m.drv_aequatorpreis != null) {
                    s += "; " + m.drv_aequatorpreis + ". Äquatorpreis";
                }

                addField(m, 0, T_LABEL, s, x++, y, 0, 0, GridBagConstraints.CENTER, c, false);
            }


            m = m.next;
        }

        calculateValues();
        
        if (sr != null && sr.cWarnings != null && sr.cWarnings.size() > 0) {
            JLabel warningLabel = new JLabel();
            warningLabel.setForeground(Color.red);
            warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
            warningLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            warningLabel.setText("Einige Fahrten wurden aufgrund von Fehlern NICHT GEWERTET (Details siehe Statistik-Ausgabeart 'intern'):");
            teilnehmerInfoPanel.add(warningLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
            String[] warnings = sr.cWarnings.keySet().toArray(new String[0]);
            
            JList warningsList = new JList();
            warningsList.setListData(warnings);
            warningsList.setForeground(Color.red);
            JScrollPane warningsScrollPane = new JScrollPane();
            warningsScrollPane.getViewport().add(warningsList, null);
            warningsScrollPane.setMaximumSize(new Dimension((int) Dialog.screenSize.getWidth() - 100, 100));
            warningsScrollPane.setPreferredSize(new Dimension((int) Dialog.screenSize.getWidth() - 100, 100));
            teilnehmerInfoPanel.add(warningsScrollPane, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        }
    }

    Hashtable getSelectedMeldungen() {
        Object[] checkKeys = this.checkboxes.keySet().toArray();
        Hashtable gemeldete = new Hashtable();
        for (int i = 0; i < checkKeys.length; i++) {
            if (((JCheckBox) checkKeys[i]).isSelected() && ((Item) checkboxes.get(checkKeys[i])).content == C_MELDEN) {
                gemeldete.put(((Item) checkboxes.get(checkKeys[i])).m, "dummy");
            }
        }
        return gemeldete;
    }

    void calculateValues() {
        Object[] checkKeys = this.checkboxes.keySet().toArray();
        Object[] textfKeys = this.textfields.keySet().toArray();

        Hashtable gemeldete = getSelectedMeldungen();

        int anzMelden = 0;
        int anzMelden1 = 0;
        int anzMelden2 = 0;
        int anzMelden3 = 0;
        int anzMelden4 = 0;
        int anzMelden5 = 0;

        anzBestellErwEinf = 0;
        Arrays.fill(anzBestellErwGold, 0);
        anzBestellJugEinf = 0;
        Arrays.fill(anzBestellJugGold, 0);
        anzBestellErwStoff = 0;
        anzBestellJugStoff = 0;

        for (int i = 0; i < checkKeys.length; i++) {
            Item item = (Item) checkboxes.get(checkKeys[i]);
            JCheckBox c = (JCheckBox) checkKeys[i];
            switch (item.content) {
                case C_MELDEN:
                    if (c.isSelected()) {
                        anzMelden++;
                        switch (efaWett.wettId) {
                            case WettDefs.DRV_FAHRTENABZEICHEN:
                                if (item.m.abzeichen != null) {
                                    if (item.m.gruppe.startsWith("1") || item.m.gruppe.startsWith("2")) {
                                        anzMelden1++;
                                    }
                                    if (item.m.gruppe.startsWith("3")) {
                                        anzMelden2++;
                                    }

                                }
                                if (item.m.drv_aequatorpreis != null) {
                                    anzMelden5++;
                                }
                                break;
                            case WettDefs.LRVBERLIN_SOMMER:
                            case WettDefs.LRVBERLIN_WINTER:
                                if (item.m.gruppe != null && item.m.gruppe.length() > 0) {
                                    if (item.m.gruppe.charAt(0) == '1') {
                                        anzMelden1++;
                                    }
                                    if (item.m.gruppe.charAt(0) == '2') {
                                        anzMelden2++;
                                    }
                                    if (item.m.gruppe.charAt(0) == '3') {
                                        anzMelden3++;
                                    }
                                    if (item.m.gruppe.charAt(0) == '4') {
                                        anzMelden4++;
                                    }
                                }
                                break;
                            case WettDefs.LRVBERLIN_BLAUERWIMPEL:
                                anzMelden1++; // Anzahl der zu meldenden Teilnehmer
                                anzMelden2 += EfaUtil.zehntelString2Int(item.m.kilometer); // Gesamtkilometer der zu meldenden Teilnehmer
                                break;
                        }
                    }
                    break;

                case C_BESTELLEN1:
                    if (gemeldete.get(item.m) == null) {
                        c.setSelected(false);
                        c.setEnabled(false);
                    } else {
                        c.setEnabled(true);
                    }
                    if (c.isSelected()) {
                        switch (efaWett.wettId) {
                            case WettDefs.DRV_FAHRTENABZEICHEN:
                                if (item.m.abzeichen != null && item.m.abzeichen.length() >= 2) {
                                    if (item.m.abzeichen.equals(EfaWettMeldung.ABZEICHEN_ERW_EINF)) {
                                        anzBestellErwEinf++;
                                    }
                                    if (item.m.abzeichen.startsWith(EfaWettMeldung.ABZEICHEN_ERW_GOLD_PRAEFIX)) {
                                        anzBestellErwGold[EfaWettMeldung.getAbzeichenGoldIndex(item.m.abzeichen)]++;
                                    }
                                    if (item.m.abzeichen.equals(EfaWettMeldung.ABZEICHEN_JUG_EINF)) {
                                        anzBestellJugEinf++;
                                    }
                                    if (item.m.abzeichen.startsWith(EfaWettMeldung.ABZEICHEN_JUG_GOLD_PRAEFIX)) {
                                        anzBestellJugGold[EfaWettMeldung.getAbzeichenGoldIndex(item.m.abzeichen)]++;
                                    }
                                }
                                break;
                        }
                    }
                    break;

                case C_BESTELLEN2:
                    if (gemeldete.get(item.m) == null) {
                        c.setSelected(false);
                        c.setEnabled(false);
                    } else {
                        c.setEnabled(true);
                    }
                    if (c.isSelected()) {
                        switch (efaWett.wettId) {
                            case WettDefs.DRV_FAHRTENABZEICHEN:
                                if (item.m.gruppe != null) {
                                    if (item.m.gruppe.startsWith("1") || item.m.gruppe.startsWith("2")) {
                                        anzBestellErwStoff++;
                                    }
                                    if (item.m.gruppe.startsWith("3")) {
                                        anzBestellJugStoff++;
                                    }
                                }
                                break;
                        }
                    }
                    break;
            }
        }

        for (int i = 0; i < textfKeys.length; i++) {
            Item item = (Item) textfields.get(textfKeys[i]);
            JTextField t = (JTextField) textfKeys[i];
            if (gemeldete.get(item.m) != null) {
                t.setEnabled(true);
            } else {
                t.setEnabled(false);
            }
        }

        // Label für Teilnehmer-Anzahl (Meldungen)
        this.teilnAnz.setText(Integer.toString(anzMelden));
        this.teiln1Anz.setText(Integer.toString(anzMelden1));
        this.teiln2Anz.setText(Integer.toString(anzMelden2));
        this.teiln3Anz.setText(Integer.toString(anzMelden3));
        this.teiln4Anz.setText(Integer.toString(anzMelden4));
        this.teiln5Anz.setText(Integer.toString(anzMelden5));
        if (efaWett.wettId == WettDefs.LRVBERLIN_BLAUERWIMPEL) {
            this.teilnAnz.setText(Integer.toString(efaWett.wimpel_anzMitglieder));
            this.teiln1Anz.setText(Integer.toString(anzMelden1));
            this.teiln2Anz.setText(EfaUtil.zehntelInt2String(anzMelden2));
            this.teiln3Anz.setText(EfaUtil.zehntelInt2String(EfaUtil.intdiv(anzMelden2, anzMelden1)));
        }
        if (efaWett.wettId == WettDefs.DRV_WANDERRUDERSTATISTIK) {
            this.teilnAnz.setText(Integer.toString(
                    EfaUtil.string2int(efaWett.aktive_M_ab19, 0) + EfaUtil.string2int(efaWett.aktive_M_bis18, 0)
                    + EfaUtil.string2int(efaWett.aktive_W_ab19, 0) + EfaUtil.string2int(efaWett.aktive_W_bis18, 0)));
            this.teiln1Anz.setText(efaWett.aktive_M_ab19);
            this.teiln2Anz.setText(efaWett.aktive_M_bis18);
            this.teiln3Anz.setText(efaWett.aktive_W_ab19);
            this.teiln4Anz.setText(efaWett.aktive_W_bis18);
        }

        // Label für Teilnehmer-Gebühren
        int anzErw = 0;
        int anzJug = 0;
        int gebProErw = 0;
        int gebProJug = 0;
        switch (efaWett.wettId) {
            case WettDefs.DRV_FAHRTENABZEICHEN:
                anzErw = anzMelden1;
                anzJug = anzMelden2;
                gebProErw = Daten.wettDefs.efw_drv_fa_meld_erw;
                gebProJug = Daten.wettDefs.efw_drv_fa_meld_jug;
                break;
            case WettDefs.LRVBERLIN_SOMMER:
                anzErw = anzMelden1 + anzMelden2;
                anzJug = anzMelden3 + anzMelden4;
                gebProErw = Daten.wettDefs.efw_lrvbln_somm_meld_erw;
                gebProJug = Daten.wettDefs.efw_lrvbln_somm_meld_jug;
                break;
            case WettDefs.LRVBERLIN_WINTER:
                anzErw = anzMelden1 + anzMelden2;
                anzJug = anzMelden3 + anzMelden4;
                gebProErw = Daten.wettDefs.efw_lrvbln_wint_meld_erw;
                gebProJug = Daten.wettDefs.efw_lrvbln_wint_meld_jug;
                break;
            case WettDefs.LRVBERLIN_BLAUERWIMPEL:
                break;
        }
        int gebErw = anzErw * gebProErw;
        int gebJug = anzJug * gebProJug;
        this.geb.setText(EfaUtil.cent2euro(gebErw + gebJug, true));
        this.geb1.setText(anzErw + " x " + EfaUtil.cent2euro(gebProErw, true));
        this.geb2.setText(anzJug + " x " + EfaUtil.cent2euro(gebProJug, true));

        // Label für Bestellungen
        int gebProBestell1 = 0;
        int gebProBestell2 = 0;
        int gebProBestell3 = 0;
        int gebProBestell4 = 0;
        int gebProBestell5 = 0;
        int gebProBestell6 = 0;
        switch (efaWett.wettId) {
            case WettDefs.DRV_FAHRTENABZEICHEN:
                gebProBestell1 = Daten.wettDefs.efw_drv_fa_nadel_erw_silber;
                gebProBestell2 = Daten.wettDefs.efw_drv_fa_nadel_erw_gold;
                gebProBestell3 = Daten.wettDefs.efw_drv_fa_nadel_jug_silber;
                gebProBestell4 = Daten.wettDefs.efw_drv_fa_nadel_jug_gold;
                gebProBestell5 = Daten.wettDefs.efw_drv_fa_stoff_erw;
                gebProBestell6 = Daten.wettDefs.efw_drv_fa_stoff_jug;
                break;
        }
        int gebBestell1 = anzBestellErwEinf * gebProBestell1;
        int gebBestell2 = EfaUtil.sumUpArray(anzBestellErwGold) * gebProBestell2;
        int gebBestell3 = anzBestellJugEinf * gebProBestell3;
        int gebBestell4 = EfaUtil.sumUpArray(anzBestellJugGold) * gebProBestell4;
        int gebBestell5 = anzBestellErwStoff * gebProBestell5;
        int gebBestell6 = anzBestellJugStoff * gebProBestell6;
        int gebBestellSumme1 = gebBestell1 + gebBestell2 + gebBestell3 + gebBestell4;
        int gebBestellSumme2 = gebBestell5 + gebBestell6;
        this.best.setText(EfaUtil.cent2euro(gebBestellSumme1, true) + (gebBestellSumme2 > 0 ? " + " + EfaUtil.cent2euro(gebBestellSumme2, true) : ""));
        this.best1.setText(anzBestellErwEinf + " x " + EfaUtil.cent2euro(gebProBestell1, true));
        this.best2.setText(EfaUtil.sumUpArray(anzBestellErwGold) + " x " + EfaUtil.cent2euro(gebProBestell2, true));
        this.best3.setText(anzBestellJugEinf + " x " + EfaUtil.cent2euro(gebProBestell3, true));
        this.best4.setText(EfaUtil.sumUpArray(anzBestellJugGold) + " x " + EfaUtil.cent2euro(gebProBestell4, true));
        this.best5.setText(anzBestellErwStoff + " x " + EfaUtil.cent2euro(gebProBestell5, true));
        this.best6.setText(anzBestellJugStoff + " x " + EfaUtil.cent2euro(gebProBestell6, true));

        this.meldegeld = gebErw + gebJug + gebBestellSumme1;
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS, "EfaWettSelectAndCompleteFrame.okButton_actionPerformed(e) - START");
        }
        Vector papierFahrtenhefte = new Vector();

        // nicht selektierte Meldungen löschen
        Hashtable gemeldete = getSelectedMeldungen();
        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS, "EfaWettSelectAndCompleteFrame.okButton_actionPerformed(e): gemeldete.size() == " + gemeldete.size());
        }
        if (gemeldete.size() == 0) {
            Dialog.error("Es wurden keine Teilnehmer zur Meldung markiert!");
            return;
        }
        EfaWettMeldung m = efaWett.meldung;
        EfaWettMeldung m_prev = null;
        while (m != null) {
            if (gemeldete.get(m) == null) {
                if (m_prev != null) {
                    m_prev.next = m.next;
                } else {
                    efaWett.meldung = m.next;
                }
            } else {
                if (papierFahrtenheftErforderlich.get(m) != null) {
                    papierFahrtenhefte.add(m.vorname + " " + m.nachname);
                }
                m_prev = m;
                if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS, "EfaWettSelectAndCompleteFrame.okButton_actionPerformed(e): Meldung m == " + m.vorname + " " + m.nachname);
                }
            }
            m = m.next;
        }

        // Spezialbehandlungen für die einzelnen Wettbewerbe
        switch (efaWett.wettId) {
            case WettDefs.DRV_FAHRTENABZEICHEN:
                // Bestellungen in Meldedatei vermerken
                efaWett.drv_nadel_erw_silber = Integer.toString(anzBestellErwEinf);
                efaWett.drv_nadel_erw_gold = EfaUtil.arr2KommaList(anzBestellErwGold, 1);
                efaWett.drv_nadel_jug_silber = Integer.toString(anzBestellJugEinf);
                efaWett.drv_nadel_jug_gold = EfaUtil.arr2KommaList(anzBestellJugGold, 1);
                efaWett.drv_stoff_erw = Integer.toString(anzBestellErwStoff);
                efaWett.drv_stoff_jug = Integer.toString(anzBestellJugStoff);
                break;
            case WettDefs.LRVBERLIN_SOMMER:
                // Adressen in Meldedatei vermerken und ggf. in Adreßliste speichern
                Object[] textfKeys = this.textfields.keySet().toArray();
                boolean addrChanged = false;
                for (int i = 0; i < textfKeys.length; i++) {
                    Item item = (Item) textfields.get(textfKeys[i]);
                    JTextField t = (JTextField) textfKeys[i];
                    if (gemeldete.get(item.m) != null) {
                        item.m.anschrift = t.getText().trim();
                    }
                }
                break;
            case WettDefs.LRVBERLIN_WINTER:
                // nothing to do
                break;
            case WettDefs.LRVBERLIN_BLAUERWIMPEL:
                // Gesamtwerte berechnen
                if (true || EfaUtil.string2int(efaWett.allg_wettjahr, 0) < 2015) {
                    efaWett.verein_mitglieder = teilnAnz.getText();
                }
                efaWett.wimpel_mitglieder = teiln1Anz.getText();
                efaWett.wimpel_km = teiln2Anz.getText();
                efaWett.wimpel_schnitt = teiln3Anz.getText();
                break;
            case WettDefs.DRV_WANDERRUDERSTATISTIK:
                // nothing to do
                break;
        }


        String mg = null;
        if (meldegeld > 0) {
            mg = EfaUtil.cent2euro(meldegeld, true);
        }
        this.resultMeldegeld = mg;
        this.resultPapierFahrtenhefte = papierFahrtenhefte;
        if (Logger.isTraceOn(Logger.TT_STATISTICS)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_STATISTICS, "EfaWettSelectAndCompleteFrame.okButton_actionPerformed(e) - END");
        }
        setDialogResult(true);
        super.closeButton_actionPerformed(e);
    }

    public String getResultMeldegeld() {
        return resultMeldegeld;
    }

    public Vector getResultPapierFahrtenhefte() {
        return resultPapierFahrtenhefte;
    }

    class Item {

        public EfaWettMeldung m;
        public int content;

        public Item(EfaWettMeldung m, int content) {
            this.m = m;
            this.content = content;
        }
    }

    class WarningWindow extends Thread {

        public void run() {
        	this.setName("EfaWettSelectAndCompleteDialog.WarningWindowThread");
            try {
                JTextArea t = new JTextArea();
                t.setEditable(false);
                t.setBackground(Color.red);
                t.setForeground(Color.white);
                t.setFont(new java.awt.Font("Dialog", 1, 18));
                t.append("Die Verwendung von efa befreit nicht von der Notwendigkeit,\ndie ausgewerteten Daten gründlich zu prüfen!\n\n"
                        + "Die Auswertung durch efa basiert ausschließlich auf den erfaßten Daten.\nWenn die erfaßten Daten fehlerhaft sind, so sind es auch die ausgewerteten Daten.\n\n"
                        + "Bitte prüfe die Daten vor dem Einsenden gründlich auf Korrektheit!");
                JPanel p = new JPanel();
                p.setBackground(Color.red);
                p.setBorder(BorderFactory.createEtchedBorder());
                p.add(t);

                JWindow w = new JWindow();
                w.getContentPane().add(p);
                w.pack();

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                Dimension frameSize = w.getSize();
                w.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
                w.setVisible(true);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException eee) {
                }
                w.toFront();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException eee) {
                }
                w.setVisible(false);
                w.dispose();
                w = null;
            } catch (Exception e) {
            }
        }
    }

}
