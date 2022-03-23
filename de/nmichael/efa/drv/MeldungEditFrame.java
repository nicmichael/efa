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
import de.nmichael.efa.data.efawett.DRVSignatur;
import de.nmichael.efa.data.efawett.EfaWettMeldung;
import de.nmichael.efa.data.efawett.EfaWett;
import de.nmichael.efa.data.efawett.ESigFahrtenhefte;
import de.nmichael.efa.efa1.DatenFelder;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;
import de.nmichael.efa.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.*;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.core.items.ItemTypeTextArea;
import static de.nmichael.efa.data.Waters.getResourceTemplate;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.Base64;
import javax.swing.event.*;
import java.security.PrivateKey;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class MeldungEditFrame extends JDialog implements ActionListener {

    static final int MAX_FAHRTEN = 7;
    private static boolean _hasBeenSaved = false;
    JDialog parent;
    EfaWett ew;
    String qnr;
    int MELDTYP;
    int meldegeld;
    Vector data;
    JTextField[][] mFahrten;
    EfaWettMeldung ewmCur = null;
    int ewmNr = -1;
    boolean changed = false;
    boolean vBlocked = true;
    boolean mBlocked = true;
    Hashtable hGeschlecht = new Hashtable();
    Hashtable hGruppe = new Hashtable();
    Hashtable hAbzeichen = new Hashtable();
    Hashtable hAequator = new Hashtable();
    AutoCompleteList waterList;
    JPanel mainPanel = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel northPanel = new JPanel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel infoQnrLabel = new JLabel();
    JLabel labelVerein = new JLabel();
    JLabel labelQnr = new JLabel();
    JLabel infoVereinLabel = new JLabel();
    JPanel southPanel = new JPanel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JTabbedPane jTabbedPane1 = new JTabbedPane();
    JPanel vereinsdatenPanel = new JPanel();
    JPanel teilnehmerPanel = new JPanel();
    JButton bestaetigenButton = new JButton();
    JButton closeButton = new JButton();
    JButton nachfrageButton = new JButton();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JPanel vereinPanel = new JPanel();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    TitledBorder titledBorderVerein;
    JLabel labelMitglNr = new JLabel();
    JTextField vMitgliedsnr = new JTextField();
    JLabel labelVereinsname = new JLabel();
    JTextField vVereinsname = new JTextField();
    JTextField vNutzername = new JTextField();
    JLabel labelBenutzername = new JLabel();
    JLabel labelLandesverband = new JLabel();
    JTextField vLandesverband = new JTextField();
    JPanel meldenderPanel = new JPanel();
    TitledBorder titledBorderMeldender;
    GridBagLayout gridBagLayout5 = new GridBagLayout();
    JLabel vMeldenderKontoLabel = new JLabel();
    JTextField vMeldenderKonto = new JTextField();
    JLabel labelName = new JLabel();
    JLabel vMeldenderBankLabel = new JLabel();
    JTextField vMeldenderBank = new JTextField();
    JLabel vMeldenderBlzLabel = new JLabel();
    JTextField vMeldenderBlz = new JTextField();
    JTextField vMeldenderName = new JTextField();
    JLabel labelEmail = new JLabel();
    JTextField vMeldenderEmail = new JTextField();
    JPanel versandPanel = new JPanel();
    TitledBorder titledBorderVersand;
    TitledBorder titledBorderBestellungen;
    GridBagLayout gridBagLayout6 = new GridBagLayout();
    JLabel vLabelOrt = new JLabel();
    JLabel vLabelStrasse = new JLabel();
    JLabel vLabelName = new JLabel();
    JTextField vVersandName = new JTextField();
    JLabel vLabelZusatz = new JLabel();
    JTextField vVersandZusatz = new JTextField();
    JTextField vVersantStrasse = new JTextField();
    JTextField vVersandOrt = new JTextField();
    JPanel bestellungenPanel = new JPanel();
    GridBagLayout gridBagLayout7 = new GridBagLayout();
    JLabel vLabelErwGold = new JLabel();
    JLabel vLabelAnstecknadeln = new JLabel();
    JLabel vLabelErwSilber = new JLabel();
    JTextField vBestNadelErwSilber = new JTextField();
    JLabel vLabelErwGold2 = new JLabel();
    JTextField vBestNadelErwGold = new JTextField();
    JLabel vLabelJugSilber = new JLabel();
    JTextField vBestNadelJugSilber = new JTextField();
    JLabel jLabel19 = new JLabel();
    JTextField vBestNadelJugGold = new JTextField();
    JLabel jLabel20 = new JLabel();
    JTextField vBestStoffErw = new JTextField();
    JLabel vLabelJug = new JLabel();
    JTextField vBestStoffJug = new JTextField();
    JButton printStoffBestellButton = new JButton();
    JButton vUnblockButton = new JButton();
    JPanel zusammenfassungPanel = new JPanel();
    TitledBorder titledBorderZusammenfassung;
    JPanel notesPanel = new JPanel();
    TitledBorder titledBorderNotes;
    JTextField vNotes = new JTextField();
    GridBagLayout gridBagLayout8 = new GridBagLayout();
    JLabel vZusGemTeilnehmer = new JLabel();
    JTextField vZusammenfassungAnzTeilnehmer = new JTextField();
    JLabel vZusTeilnehmerErfuellt = new JLabel();
    JTextField vZusammenfassungAnzTeilnehmerErfuellt = new JTextField();
    JLabel vZusTeilnehmerUngueltig = new JLabel();
    JTextField vZusammenfassungAnzTeilnehmerUngueltig = new JTextField();
    JLabel vBetragMeldLabel = new JLabel();
    JTextField vZusammenfassungMeldegebuehr = new JTextField();
    JLabel vBetragStoffLabel = new JLabel();
    JTextField vZusammenfassungStoffabzeichen = new JTextField();
    JLabel papierFahrtenhefteErforderlichLabel = new JLabel();
    JLabel vBetragGesamtLabel = new JLabel();
    JTextField vZusammenfassungEurGesamt = new JTextField();
    JLabel vBetragLabel = new JLabel();
    JLabel vZusTeilnehmer = new JLabel();
    GridBagLayout gridBagLayout9 = new GridBagLayout();
    JPanel tailnNaviPanel = new JPanel();
    GridBagLayout gridBagLayout10 = new GridBagLayout();
    JButton deleteButton = new JButton();
    JButton newButton = new JButton();
    JButton lastButton = new JButton();
    JButton nextButton = new JButton();
    JButton prevButton = new JButton();
    JButton firstButton = new JButton();
    JPanel teilnDataPanel = new JPanel();
    JPanel fahrtenDataPanel = new JPanel();
    GridBagLayout gridBagLayout11 = new GridBagLayout();
    JLabel jLabel31 = new JLabel();
    JLabel jLabel32 = new JLabel();
    JLabel jLabel33 = new JLabel();
    JLabel jLabel34 = new JLabel();
    JLabel jLabel35 = new JLabel();
    JLabel jLabel36 = new JLabel();
    JLabel jLabel37 = new JLabel();
    JLabel jLabel38 = new JLabel();
    JLabel jLabel39 = new JLabel();
    JLabel jLabel40 = new JLabel();
    JLabel jLabel41 = new JLabel();
    JTextField mNachname = new JTextField();
    JTextField mVorname = new JTextField();
    JTextField mJahrgang = new JTextField();
    JComboBox mGeschlecht = new JComboBox();
    JComboBox mGruppe = new JComboBox();
    JTextField mKilometer = new JTextField();
    JTextField mAbzeichen = new JTextField();
    JTextField mAequatorpreis = new JTextField();
    JTextField mAnzAbzeichen = new JTextField();
    JTextField mGesKm = new JTextField();
    TitledBorder titledBorderTeilnehmer;
    TitledBorder titledBorderFahrten;
    JLabel jLabel30 = new JLabel();
    JLabel jLabel42 = new JLabel();
    JLabel jLabel43 = new JLabel();
    JLabel jLabel44 = new JLabel();
    JLabel jLabel45 = new JLabel();
    JLabel jLabel46 = new JLabel();
    JLabel jLabel47 = new JLabel();
    JLabel jLabel48 = new JLabel();
    JPanel teilnWarnPanel = new JPanel();
    TitledBorder titledBorderWarnungen;
    JScrollPane jScrollPane1 = new JScrollPane();
    JTextArea mWarnungen = new JTextArea();
    BorderLayout borderLayout2 = new BorderLayout();
    JPanel dateiPanel = new JPanel();
    GridBagLayout gridBagLayout12 = new GridBagLayout();
    TitledBorder titledBorderDatei;
    JLabel jLabel49 = new JLabel();
    JTextField vKennung = new JTextField();
    JLabel jLabel50 = new JLabel();
    JTextField vProgramm = new JTextField();
    //@AB JLabel jLabel51 = new JLabel();
    //@AB JLabel jLabel52 = new JLabel();
    //@AB JTextField mAnzAbzeichenAB = new JTextField();
    //@AB JTextField mGesKmAB = new JTextField();
    JButton mUnblockButton = new JButton();
    JLabel mFahrtenheft = new JLabel();
    JCheckBox mWirdGewertet = new JCheckBox();
    JLabel jLabel53 = new JLabel();
    JTextField mTeilnNr = new JTextField();
    JButton mTeilnSuchenButton = new JButton();
    JButton aequatorButton = new JButton();
    JLabel mNichtGewertetGrundLabel = new JLabel();
    JTextField mNichtGewertetGrund = new JTextField();
    JCheckBox vMeldegeldEingegangen = new JCheckBox();
    JLabel vEingPapierhefteLabel = new JLabel();
    JTextField vAnzahlPapierFahrtenhefte = new JTextField();
    JLabel jLabelLfdNr = new JLabel();
    JTextField fLfdNr = new JTextField();
    JLabel jLabel8 = new JLabel();
    JTextField fStartZiel = new JTextField();
    JLabel jLabelStrecke = new JLabel();
    JTextField fStrecke = new JTextField();
    JLabel jLabel10 = new JLabel();
    JTextField fGewaesser = new JTextField();
    JButton addGewaesser = new JButton();
    JLabel jLabel11 = new JLabel();
    JTextField fKilometer = new JTextField();
    JLabel jLabel55 = new JLabel();
    JTextField fTage = new JTextField();
    JLabel jLabel56 = new JLabel();
    JTextField fTeilnehmer = new JTextField();
    JTextField fMannschKm = new JTextField();
    JTextField fBemerkungen = new JTextField();
    JLabel jLabel58 = new JLabel();
    JLabel jLabel59 = new JLabel();
    JLabel jLabel60 = new JLabel();
    JLabel jLabel61 = new JLabel();
    JLabel jLabel62 = new JLabel();
    JLabel jLabel63 = new JLabel();
    JLabel jLabel64 = new JLabel();
    JLabel jLabel65 = new JLabel();
    JTextField fMaennerAnz = new JTextField();
    JTextField fJuniorenAnz = new JTextField();
    JTextField fFrauenAnz = new JTextField();
    JTextField fJuniorinnenAnz = new JTextField();
    JTextField fMaennerKm = new JTextField();
    JTextField fJuniorenKm = new JTextField();
    JTextField fFrauenKm = new JTextField();
    JTextField fJuniorinnenKm = new JTextField();
    JLabel jLabel66 = new JLabel();
    JLabel jLabel67 = new JLabel();
    JLabel jLabel68 = new JLabel();
    JLabel jLabel69 = new JLabel();
    JLabel jLabel57 = new JLabel();
    JLabel jLabel70 = new JLabel();
    JButton fUnblockButton = new JButton();
    JCheckBox fWirdGewertet = new JCheckBox();
    JLabel fNichtGewertetGrundLabel = new JLabel();
    JTextField fNichtGewertetGrund = new JTextField();
    JCheckBox fNachfrageCheckBox = new JCheckBox();
    JTextField fNachfrage = new JTextField();
    JLabel jLabel22 = new JLabel();
    JButton wsListButton = new JButton();
    JCheckBox mFahrtnachweisErbracht = new JCheckBox();
    JLabel vAktiveLabel = new JLabel();
    JLabel vAktiveMbis18Label = new JLabel();
    JLabel vAktiveMab19Label = new JLabel();
    JLabel vAktiveWbis18Label = new JLabel();
    JLabel vAktiveWab19Label = new JLabel();
    JLabel vVereinskilometerLabel = new JLabel();
    JTextField vAktiveMbis18 = new JTextField();
    JTextField vAktiveMab19 = new JTextField();
    JTextField vAktiveWbis18 = new JTextField();
    JTextField vAktiveWab19 = new JTextField();
    JTextField vVereinskilometer = new JTextField();

    public MeldungEditFrame(JDialog parent, EfaWett ew, String qnr, int meldTyp) {
        super(parent);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        Dialog.frameOpened(this);
        this.parent = parent;
        this.ew = ew;
        this.qnr = qnr;
        this.MELDTYP = meldTyp;
        log(false, "START Bearbeiten der Meldung");
        _hasBeenSaved = false;
        try {
            jbInit();
            iniFields();
            iniWaterList();
            ew.durchDRVbearbeitet = true;
            readMeldedatei();
            setVFields();
            this.changed = false;
            calcOverallValues();
            setMFields(0, false);

            if (ew.allg_programm != null && ew.allg_programm.equals(Daten.PROGRAMMID_DRV)
                    && (ew.verein_name == null || ew.verein_name.length() == 0)) {
                vBlock(false);
                mBlock(true);
                this.vVereinsname.requestFocus();
                this.changed = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        EfaUtil.pack(this);
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
            titledBorderVerein = new TitledBorder("Verein");
            titledBorderMeldender = new TitledBorder("Meldender");
            titledBorderVersand = new TitledBorder("Versand");
            titledBorderBestellungen = new TitledBorder("Bestellungen");
            titledBorderZusammenfassung = new TitledBorder("Zusammenfassung");
            titledBorderNotes = new TitledBorder("Notizen");
            titledBorderTeilnehmer = new TitledBorder("Teilnehmer");
            titledBorderFahrten = new TitledBorder("Fahrten");
            titledBorderWarnungen = new TitledBorder("Warnungen / Fehler");
            titledBorderDatei = new TitledBorder("Datei");
            ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                    new String[]{"ESCAPE", "F1"}, new String[]{"keyAction", "keyAction"});
            mainPanel.setLayout(borderLayout1);
            this.setTitle("Meldung bearbeiten");
            northPanel.setLayout(gridBagLayout1);
            infoQnrLabel.setForeground(Color.black);
            infoQnrLabel.setText("1234567890");
            labelVerein.setText("Verein: ");
            labelQnr.setText("Quittungsnummer: ");
            infoVereinLabel.setForeground(Color.black);
            infoVereinLabel.setText("Vereinsname");
            southPanel.setLayout(gridBagLayout2);
            bestaetigenButton.setActionCommand("Bearbeitung abschließen");
            bestaetigenButton.setText("Bearbeitung abschließen");
            bestaetigenButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    bestaetigenButton_actionPerformed(e);
                }
            });
            closeButton.setText("Bearbeitung unterbrechen");
            closeButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    closeButton_actionPerformed(e);
                }
            });
            nachfrageButton.setText("Nachfragen ...");
            nachfrageButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    nachfrageButton_actionPerformed(e);
                }
            });
            vereinsdatenPanel.setLayout(gridBagLayout3);
            vereinPanel.setLayout(gridBagLayout4);
            vereinPanel.setBorder(titledBorderVerein);
            vereinPanel.setName("Verein");
            labelMitglNr.setText("Mitgliedsnummer: ");
            labelVereinsname.setText("Vereinsname: ");
            labelBenutzername.setText("Benutzername: ");
            labelLandesverband.setText("LRV: ");
            vMitgliedsnr.setMinimumSize(new Dimension(70, 17));
            vMitgliedsnr.setPreferredSize(new Dimension(100, 17));
            vVereinsname.setMinimumSize(new Dimension(200, 17));
            vVereinsname.setPreferredSize(new Dimension(300, 17));
            vNutzername.setMinimumSize(new Dimension(50, 17));
            vNutzername.setPreferredSize(new Dimension(100, 17));
            vLandesverband.setMinimumSize(new Dimension(200, 17));
            vLandesverband.setPreferredSize(new Dimension(300, 17));
            meldenderPanel.setBorder(titledBorderMeldender);
            meldenderPanel.setLayout(gridBagLayout5);
            vMeldenderKontoLabel.setText("Konto: ");
            labelName.setText("Name: ");
            vMeldenderBankLabel.setText("Bank: ");
            vMeldenderBlzLabel.setText("BLZ: ");
            labelEmail.setText("email: ");
            vMeldenderName.setMinimumSize(new Dimension(200, 17));
            vMeldenderName.setPreferredSize(new Dimension(200, 17));
            vMeldenderKonto.setMinimumSize(new Dimension(200, 17));
            vMeldenderKonto.setPreferredSize(new Dimension(200, 17));
            vMeldenderBank.setMinimumSize(new Dimension(200, 17));
            vMeldenderBank.setPreferredSize(new Dimension(200, 17));
            vMeldenderBlz.setMinimumSize(new Dimension(150, 17));
            vMeldenderBlz.setPreferredSize(new Dimension(150, 17));
            vMeldenderEmail.setMinimumSize(new Dimension(150, 17));
            vMeldenderEmail.setPreferredSize(new Dimension(150, 17));
            versandPanel.setBorder(titledBorderVersand);
            versandPanel.setLayout(gridBagLayout6);
            vLabelOrt.setText("Ort: ");
            vLabelStrasse.setText("Straße: ");
            vLabelName.setText("Name: ");
            vLabelZusatz.setText("Adresszusatz: ");
            vVersandZusatz.setMinimumSize(new Dimension(300, 17));
            vVersandZusatz.setPreferredSize(new Dimension(300, 17));
            vVersantStrasse.setMinimumSize(new Dimension(300, 17));
            vVersantStrasse.setPreferredSize(new Dimension(300, 17));
            vVersandOrt.setMinimumSize(new Dimension(300, 17));
            vVersandOrt.setPreferredSize(new Dimension(300, 17));
            bestellungenPanel.setLayout(gridBagLayout7);
            bestellungenPanel.setBorder(titledBorderBestellungen);
            vLabelErwGold.setText("Erw. (gold): ");
            vLabelAnstecknadeln.setText("Anstecknadeln: ");
            vLabelErwSilber.setText("Erw. (silber): ");
            vLabelErwGold2.setText("Erw. (gold): ");
            vLabelJugSilber.setText("Jug. (silber): ");
            vAnzahlPapierFahrtenhefte.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            vBestNadelJugSilber.setMinimumSize(new Dimension(70, 17));
            vBestNadelJugSilber.setPreferredSize(new Dimension(70, 17));
            vBestNadelJugSilber.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            jLabel19.setText("Jug. (gold): ");
            vBestNadelJugGold.setMinimumSize(new Dimension(70, 17));
            vBestNadelJugGold.setPreferredSize(new Dimension(70, 17));
            vBestNadelJugGold.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            jLabel20.setText("Stoffabzeichen: ");
            vBestStoffErw.setMinimumSize(new Dimension(70, 17));
            vBestStoffErw.setPreferredSize(new Dimension(70, 17));
            vBestStoffErw.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            vLabelJug.setText("Jugend: ");
            vBestStoffJug.setMinimumSize(new Dimension(70, 17));
            vBestStoffJug.setPreferredSize(new Dimension(70, 17));
            vBestStoffJug.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            printStoffBestellButton.setBackground(Color.orange);
            printStoffBestellButton.setText("Soffabzeichen-Bestellungen drucken");
            printStoffBestellButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    printStoffBestellButton_actionPerformed(e);
                }
            });
            vBestNadelErwSilber.setMinimumSize(new Dimension(70, 17));
            vBestNadelErwSilber.setPreferredSize(new Dimension(70, 17));
            vBestNadelErwSilber.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            vBestNadelErwGold.setMinimumSize(new Dimension(70, 17));
            vBestNadelErwGold.setPreferredSize(new Dimension(70, 17));
            vBestNadelErwGold.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            vUnblockButton.setText("Felder zum Bearbeiten freigeben");
            vUnblockButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    vUnblockButton_actionPerformed(e);
                }
            });
            zusammenfassungPanel.setBorder(titledBorderZusammenfassung);
            zusammenfassungPanel.setLayout(gridBagLayout8);
            vZusGemTeilnehmer.setText("gemeldete Teilnehmer: ");
            vZusammenfassungAnzTeilnehmer.setPreferredSize(new Dimension(90, 17));
            vZusammenfassungAnzTeilnehmer.setEditable(false);
            vZusTeilnehmerErfuellt.setText("... davon erfüllt: ");
            vZusammenfassungAnzTeilnehmerErfuellt.setPreferredSize(new Dimension(90, 17));
            vZusammenfassungAnzTeilnehmerErfuellt.setEditable(false);
            vZusTeilnehmerUngueltig.setText("... davon ungültig: ");
            vZusammenfassungAnzTeilnehmerUngueltig.setForeground(Color.red);
            vZusammenfassungAnzTeilnehmerUngueltig.setPreferredSize(new Dimension(90, 17));
            vZusammenfassungAnzTeilnehmerUngueltig.setEditable(false);
            vBetragMeldLabel.setText("... davon Meldegebühr+Anstecknadeln: ");
            vZusammenfassungMeldegebuehr.setPreferredSize(new Dimension(90, 17));
            vZusammenfassungMeldegebuehr.setEditable(false);
            vBetragStoffLabel.setText("... davon Stoffabzeichen: ");
            vZusammenfassungStoffabzeichen.setPreferredSize(new Dimension(90, 17));
            vZusammenfassungStoffabzeichen.setEditable(false);
            papierFahrtenhefteErforderlichLabel.setForeground(Color.red);
            papierFahrtenhefteErforderlichLabel.setHorizontalAlignment(SwingConstants.CENTER);
            papierFahrtenhefteErforderlichLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            papierFahrtenhefteErforderlichLabel.setText("Papier-Fahrtenhefte erforderlich!");
            vBetragGesamtLabel.setText("gesamt: ");
            vZusammenfassungEurGesamt.setPreferredSize(new Dimension(90, 17));
            vZusammenfassungEurGesamt.setEditable(false);
            vBetragLabel.setHorizontalAlignment(SwingConstants.CENTER);
            vBetragLabel.setText("zu zahlender Betrag:");
            vZusTeilnehmer.setHorizontalAlignment(SwingConstants.CENTER);
            vZusTeilnehmer.setText("Teilnehmer");
            notesPanel.setBorder(titledBorderNotes);
            notesPanel.setLayout(new BorderLayout());
            vNotes.setPreferredSize(new Dimension(600,19));
            teilnehmerPanel.setLayout(gridBagLayout9);
            tailnNaviPanel.setLayout(gridBagLayout10);
            deleteButton.setPreferredSize(new Dimension(140, 23));
            deleteButton.setText("Löschen");
            deleteButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    deleteButton_actionPerformed(e);
                }
            });
            newButton.setPreferredSize(new Dimension(140, 23));
            newButton.setText("Neu");
            newButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    newButton_actionPerformed(e);
                }
            });
            lastButton.setPreferredSize(new Dimension(140, 23));
            lastButton.setText("Letzter");
            lastButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    lastButton_actionPerformed(e);
                }
            });
            nextButton.setPreferredSize(new Dimension(140, 23));
            nextButton.setText("Nächster >>");
            nextButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    nextButton_actionPerformed(e);
                }
            });
            prevButton.setPreferredSize(new Dimension(140, 23));
            prevButton.setText("<< Vorheriger");
            prevButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    prevButton_actionPerformed(e);
                }
            });
            firstButton.setPreferredSize(new Dimension(140, 23));
            firstButton.setText("Erster");
            firstButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    firstButton_actionPerformed(e);
                }
            });
            teilnDataPanel.setLayout(gridBagLayout11);
            fahrtenDataPanel.setLayout(gridBagLayout11);
            jLabel31.setText("Nachname: ");
            jLabel32.setText("Vorname: ");
            jLabel33.setText("Jahrgang: ");
            jLabel34.setText("Kilomter bisher: ");
            jLabel35.setText("Fahrtenheft: ");
            jLabel36.setText("Geschlecht: ");
            jLabel37.setText("Gruppe: ");
            jLabel38.setText("Kilometer: ");
            jLabel39.setText("Abzeichen: ");
            jLabel40.setText("Abzeichen bisher: ");
            jLabel41.setText("Äquatorpreis: ");
            mNachname.setPreferredSize(new Dimension(200, 17));
            mVorname.setPreferredSize(new Dimension(200, 17));
            mJahrgang.setPreferredSize(new Dimension(200, 17));
            mKilometer.setPreferredSize(new Dimension(200, 17));
            mAnzAbzeichen.setPreferredSize(new Dimension(70, 17));
            mGesKm.setPreferredSize(new Dimension(70, 17));
            mGeschlecht.setPreferredSize(new Dimension(200, 22));
            mGruppe.setPreferredSize(new Dimension(200, 22));
            mAbzeichen.setEditable(false);
            mAbzeichen.setPreferredSize(new Dimension(200, 22));
            mAequatorpreis.setEditable(false);
            mAequatorpreis.setPreferredSize(new Dimension(200, 22));
            teilnDataPanel.setBorder(titledBorderTeilnehmer);
            fahrtenDataPanel.setBorder(titledBorderFahrten);
            jLabel30.setText(" ");
            jLabel42.setHorizontalAlignment(SwingConstants.CENTER);
            jLabel42.setHorizontalTextPosition(SwingConstants.CENTER);
            jLabel42.setText("Nachweis der Fahrten");
            jLabel43.setText("LfdNr.");
            jLabel44.setText("Startdatum");
            jLabel45.setText("Enddatum");
            jLabel46.setText("Ziel");
            jLabel47.setText("Km");
            jLabel48.setText("Bemerk.");
            mFahrtnachweisErbracht.setText("Fahrtnachweis erbracht (keine Prüfung)");
            mFahrtnachweisErbracht.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    mFahrtnachweisErbracht_actionPerformed(e);
                }
            });
            teilnWarnPanel.setBorder(titledBorderWarnungen);
            teilnWarnPanel.setLayout(borderLayout2);
            mWarnungen.setFont(new java.awt.Font("Dialog", 1, 12));
            mWarnungen.setForeground(Color.red);
            mWarnungen.setCaretColor(Color.red);
            mWarnungen.setEditable(false);
            jScrollPane1.setMinimumSize(new Dimension(22, 50));
            jScrollPane1.setPreferredSize(new Dimension(80, 50));
            dateiPanel.setLayout(gridBagLayout12);
            dateiPanel.setBorder(titledBorderDatei);
            jLabel49.setText("Datei-Kennung: ");
            vKennung.setMinimumSize(new Dimension(150, 17));
            vKennung.setPreferredSize(new Dimension(150, 17));
            vKennung.setEditable(false);
            jLabel50.setText("Programm: ");
            vProgramm.setMinimumSize(new Dimension(150, 17));
            vProgramm.setPreferredSize(new Dimension(150, 17));
            vProgramm.setEditable(false);
            //@AB jLabel51.setText("davon Jug A/B: ");
            //@AB jLabel52.setText("davon Jug A/B: ");
            //@AB mAnzAbzeichenAB.setPreferredSize(new Dimension(70, 17));
            //@AB mGesKmAB.setPreferredSize(new Dimension(70, 17));
            mUnblockButton.setText("Felder zum Bearbeiten freigeben");
            mUnblockButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    mUnblockButton_actionPerformed(e);
                }
            });
            mFahrtenheft.setText("Fahrtenheft ist ...");
            mWirdGewertet.setText("Dieser Teilnehmer wird gewertet");
            mWirdGewertet.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    mWirdGewertet_actionPerformed(e);
                }
            });
            jLabel53.setText("Teilnehmer-Nr: ");
            mTeilnSuchenButton.setPreferredSize(new Dimension(155, 22));
            mTeilnSuchenButton.setText("Teilnehmer-Nr. suchen");
            mTeilnSuchenButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    mTeilnSuchenButton_actionPerformed(e);
                }
            });
            aequatorButton.setForeground(Color.blue);
            aequatorButton.setHorizontalTextPosition(SwingConstants.CENTER);
            aequatorButton.setText("5 Äquatorpreisträger");
            aequatorButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    aequatorButton_actionPerformed(e);
                }
            });
            jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    jTabbedPane1_stateChanged(e);
                }
            });
            mNichtGewertetGrundLabel.setText("Grund bei Nicht-Wertung: ");
            mNichtGewertetGrund.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    mNichtGewertetGrund_focusLost(e);
                }
            });
            vMeldegeldEingegangen.setForeground(Color.red);
            vMeldegeldEingegangen.setText("eingegangen");
            vMeldegeldEingegangen.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    vMeldegeldEingegangen_actionPerformed(e);
                }
            });
            vEingPapierhefteLabel.setText("Eingesandte Papier-Fahrtenhefte: ");
            jLabelLfdNr.setText("LfdNr: ");
            jLabel8.setText("Bezeichnung der Fahrt: ");
            jLabelStrecke.setText("Strecke: ");
            jLabel10.setText("Befahrene Gewässer: ");
            JLabel jLabelBemerk = new JLabel("Bemerkungen: ");
            jLabel11.setToolTipText("");
            jLabel11.setText("Dauer und Länge: ");
            jLabel55.setText("Tage: ");
            jLabel56.setText("Anzahl: ");
            fKilometer.setMinimumSize(new Dimension(50, 17));
            fKilometer.setPreferredSize(new Dimension(60, 17));
            fTage.setMinimumSize(new Dimension(60, 17));
            fTage.setPreferredSize(new Dimension(60, 17));
            fTeilnehmer.setMinimumSize(new Dimension(60, 17));
            fTeilnehmer.setPreferredSize(new Dimension(60, 17));
            fMannschKm.setMinimumSize(new Dimension(60, 17));
            fMannschKm.setPreferredSize(new Dimension(60, 17));
            fBemerkungen.setMinimumSize(new Dimension(60, 17));
            fBemerkungen.setPreferredSize(new Dimension(60, 17));
            jLabel58.setText("Männer: ");
            jLabel59.setText("Junioren: ");
            jLabel60.setText("Frauen: ");
            jLabel61.setText("Juniorinnen: ");
            jLabel62.setText("Anzahl: ");
            jLabel63.setText("Anzahl: ");
            jLabel64.setText("Anzahl: ");
            jLabel65.setText("Anzahl: ");
            fJuniorinnenAnz.setMinimumSize(new Dimension(60, 17));
            fJuniorinnenAnz.setPreferredSize(new Dimension(60, 17));
            fFrauenAnz.setMinimumSize(new Dimension(60, 17));
            fFrauenAnz.setPreferredSize(new Dimension(60, 17));
            fJuniorenAnz.setMinimumSize(new Dimension(60, 17));
            fJuniorenAnz.setPreferredSize(new Dimension(60, 17));
            fMaennerAnz.setMinimumSize(new Dimension(60, 17));
            fMaennerAnz.setPreferredSize(new Dimension(60, 17));
            jLabel66.setText("Kilometer: ");
            jLabel67.setText("Kilometer: ");
            jLabel68.setText("Kilometer: ");
            jLabel69.setText("Kilometer: ");
            fJuniorinnenKm.setMinimumSize(new Dimension(60, 17));
            fJuniorinnenKm.setPreferredSize(new Dimension(60, 17));
            fFrauenKm.setMinimumSize(new Dimension(60, 17));
            fFrauenKm.setPreferredSize(new Dimension(60, 17));
            fJuniorenKm.setMinimumSize(new Dimension(60, 17));
            fJuniorenKm.setPreferredSize(new Dimension(60, 17));
            fMaennerKm.setMinimumSize(new Dimension(60, 17));
            fMaennerKm.setPreferredSize(new Dimension(60, 17));
            jLabel57.setText("Kilometer: ");
            jLabel70.setText("Streckenlänge (Kilomter): ");
            fLfdNr.setPreferredSize(new Dimension(600, 17));
            fStartZiel.setPreferredSize(new Dimension(600, 17));
            fStrecke.setPreferredSize(new Dimension(600, 17));
            fGewaesser.setPreferredSize(new Dimension(600, 17));
            addGewaesser.setText("+");
            addGewaesser.setMargin(new Insets(0,0,0,0));
            addGewaesser.setPreferredSize(new Dimension(20, 17));
            addGewaesser.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addGewaesser_actionPerformed(e);
                }
            });
            fUnblockButton.setText("Felder zum Bearbeiten freigeben");
            fUnblockButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    fUnblockButton_actionPerformed(e);
                }
            });
            fWirdGewertet.setText("Diese Fahrt wird gewertet");
            fWirdGewertet.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    fWirdGewertet_actionPerformed(e);
                }
            });
            fNachfrageCheckBox.setText("Nachfrage");
            fNachfrageCheckBox.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    fNachfrage_actionPerformed(e);
                }
            });
            fNachfrage.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    fNachfrage_focusLost(e);
                }
            });


            fNichtGewertetGrundLabel.setText("Grund bei Nicht-Wertung: ");
            fNichtGewertetGrund.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    fNichtGewertetGrund_focusLost(e);
                }
            });
            fTage.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            fKilometer.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumberWithComma_focusLost(e);
                }
            });
            fMaennerAnz.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            fMaennerKm.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumberWithComma_focusLost(e);
                }
            });
            fJuniorenAnz.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            fJuniorenKm.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumberWithComma_focusLost(e);
                }
            });
            fFrauenAnz.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            fFrauenKm.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumberWithComma_focusLost(e);
                }
            });
            fJuniorinnenAnz.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            fJuniorinnenKm.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumberWithComma_focusLost(e);
                }
            });
            fTeilnehmer.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumber_focusLost(e);
                }
            });
            fMannschKm.addFocusListener(new java.awt.event.FocusAdapter() {

                public void focusLost(FocusEvent e) {
                    makeSureIsANumberWithComma_focusLost(e);
                }
            });

            jLabel22.setText("Teilnehmer insgesamt:");
            wsListButton.setText("Liste aller Wanderfahrten anzeigen");
            wsListButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    wsListButton_actionPerformed(e);
                }
            });
            vAktiveLabel.setText("Aktive Ruderer:");
            vAktiveMbis18Label.setText("männlich bis 18: ");
            vAktiveMab19Label.setText("männlich ab 19: ");
            vAktiveWbis18Label.setText("weiblich bis 18: ");
            vAktiveWab19Label.setText("weiblich ab 19: ");
            vVereinskilometerLabel.setText("Vereinskilometer: ");
            vAktiveMbis18.setPreferredSize(new Dimension(60, 19));
            vAktiveMbis18.setEditable(false);
            vAktiveMab19.setPreferredSize(new Dimension(60, 19));
            vAktiveMab19.setEditable(false);
            vAktiveWbis18.setPreferredSize(new Dimension(60, 19));
            vAktiveWbis18.setEditable(false);
            vAktiveWab19.setPreferredSize(new Dimension(60, 19));
            vAktiveWab19.setEditable(false);
            vVereinskilometer.setPreferredSize(new Dimension(60, 19));
            vVereinskilometer.setEditable(false);
            this.getContentPane().add(mainPanel, BorderLayout.CENTER);
            mainPanel.add(northPanel, BorderLayout.NORTH);
            northPanel.add(infoQnrLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            northPanel.add(labelVerein, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            northPanel.add(labelQnr, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            northPanel.add(infoVereinLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            mainPanel.add(southPanel, BorderLayout.SOUTH);
            mainPanel.add(jTabbedPane1, BorderLayout.CENTER);
            jTabbedPane1.add(vereinsdatenPanel, "Vereinsdaten");
            vereinsdatenPanel.add(vereinPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
            String teilnehmerPanelTitle = null;
            switch (MELDTYP) {
                case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                    teilnehmerPanelTitle = "Teilnehmer";
                    break;
                case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                    teilnehmerPanelTitle = "Fahrten";
                    break;
            }
            jTabbedPane1.add(teilnehmerPanel, teilnehmerPanelTitle);
            teilnehmerPanel.add(tailnNaviPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            tailnNaviPanel.add(deleteButton, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            tailnNaviPanel.add(newButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            tailnNaviPanel.add(lastButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            tailnNaviPanel.add(nextButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            tailnNaviPanel.add(prevButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            tailnNaviPanel.add(firstButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            tailnNaviPanel.add(wsListButton, new GridBagConstraints(4, 1, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            southPanel.add(bestaetigenButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            southPanel.add(closeButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            switch (MELDTYP) {
                case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                    southPanel.add(nachfrageButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                    break;
            }
            vereinPanel.add(labelMitglNr, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            vereinPanel.add(vMitgliedsnr, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            vereinPanel.add(labelVereinsname, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            vereinPanel.add(vVereinsname, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            vereinPanel.add(vNutzername, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            vereinPanel.add(labelBenutzername, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            vereinPanel.add(vLandesverband, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            vereinPanel.add(labelLandesverband, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            vereinsdatenPanel.add(meldenderPanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
            meldenderPanel.add(labelName, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            meldenderPanel.add(vMeldenderKonto, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            meldenderPanel.add(vMeldenderKontoLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            meldenderPanel.add(vMeldenderBankLabel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            meldenderPanel.add(vMeldenderBank, new GridBagConstraints(3, 1, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            meldenderPanel.add(vMeldenderBlzLabel, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            meldenderPanel.add(vMeldenderBlz, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            meldenderPanel.add(vMeldenderName, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            meldenderPanel.add(labelEmail, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            meldenderPanel.add(vMeldenderEmail, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            vereinsdatenPanel.add(versandPanel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
            versandPanel.add(vLabelName, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            versandPanel.add(vVersandName, new GridBagConstraints(1, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            versandPanel.add(vLabelZusatz, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            versandPanel.add(vVersandZusatz, new GridBagConstraints(1, 1, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            versandPanel.add(vLabelStrasse, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            versandPanel.add(vVersantStrasse, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            versandPanel.add(vLabelOrt, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            versandPanel.add(vVersandOrt, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            vereinsdatenPanel.add(bestellungenPanel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
            bestellungenPanel.add(vLabelErwGold, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            bestellungenPanel.add(vLabelAnstecknadeln, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            bestellungenPanel.add(vLabelErwSilber, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            bestellungenPanel.add(vBestNadelErwSilber, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            bestellungenPanel.add(vLabelErwGold2, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            bestellungenPanel.add(vBestNadelErwGold, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            bestellungenPanel.add(vLabelJugSilber, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            bestellungenPanel.add(vBestNadelJugSilber, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            bestellungenPanel.add(jLabel19, new GridBagConstraints(7, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            bestellungenPanel.add(vBestNadelJugGold, new GridBagConstraints(8, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            bestellungenPanel.add(jLabel20, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            bestellungenPanel.add(vBestStoffErw, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            bestellungenPanel.add(vLabelJug, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            bestellungenPanel.add(vBestStoffJug, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            bestellungenPanel.add(printStoffBestellButton, new GridBagConstraints(5, 2, 4, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            vereinsdatenPanel.add(vUnblockButton, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(20, 0, 10, 0), 0, 0));
            vereinsdatenPanel.add(zusammenfassungPanel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
            zusammenfassungPanel.add(vZusGemTeilnehmer, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vZusammenfassungAnzTeilnehmer, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vZusTeilnehmerErfuellt, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vZusammenfassungAnzTeilnehmerErfuellt, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vZusTeilnehmerUngueltig, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vZusammenfassungAnzTeilnehmerUngueltig, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vBetragMeldLabel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
            zusammenfassungPanel.add(vZusammenfassungMeldegebuehr, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vBetragStoffLabel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
            zusammenfassungPanel.add(vZusammenfassungStoffabzeichen, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(papierFahrtenhefteErforderlichLabel, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 5, 0));
            zusammenfassungPanel.add(vBetragGesamtLabel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
            zusammenfassungPanel.add(vZusammenfassungEurGesamt, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vBetragLabel, new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vZusTeilnehmer, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(aequatorButton, new GridBagConstraints(0, 6, 6, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vMeldegeldEingegangen, new GridBagConstraints(6, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
            zusammenfassungPanel.add(vEingPapierhefteLabel, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vAnzahlPapierFahrtenhefte, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vAktiveLabel, new GridBagConstraints(7, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vAktiveMbis18Label, new GridBagConstraints(7, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
            zusammenfassungPanel.add(vAktiveMab19Label, new GridBagConstraints(7, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
            zusammenfassungPanel.add(vAktiveWbis18Label, new GridBagConstraints(7, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
            zusammenfassungPanel.add(vAktiveWab19Label, new GridBagConstraints(7, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
            zusammenfassungPanel.add(vVereinskilometerLabel, new GridBagConstraints(7, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
            zusammenfassungPanel.add(vAktiveMbis18, new GridBagConstraints(8, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vAktiveMab19, new GridBagConstraints(8, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vAktiveWbis18, new GridBagConstraints(8, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vAktiveWab19, new GridBagConstraints(8, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            zusammenfassungPanel.add(vVereinskilometer, new GridBagConstraints(8, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            vereinsdatenPanel.add(notesPanel, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
            vereinsdatenPanel.add(dateiPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 10, 10), 0, 0));
            notesPanel.add(vNotes, BorderLayout.CENTER);
            dateiPanel.add(jLabel49, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            dateiPanel.add(vKennung, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            dateiPanel.add(jLabel50, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            dateiPanel.add(vProgramm, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            switch (MELDTYP) {
                case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                    teilnehmerPanel.add(teilnDataPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 0, 0, 0), 0, 0));
                    wsListButton.setVisible(false);
                    break;
                case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                    teilnehmerPanel.add(fahrtenDataPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 0, 0, 0), 0, 0));
                    break;
            }

            fahrtenDataPanel.add(jLabelLfdNr, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fLfdNr, new GridBagConstraints(1, 0, 6, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel8, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fStartZiel, new GridBagConstraints(1, 1, 6, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabelStrecke, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fStrecke, new GridBagConstraints(1, 2, 6, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel10, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fGewaesser, new GridBagConstraints(1, 3, 6, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(addGewaesser, new GridBagConstraints(7, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabelBemerk, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fBemerkungen, new GridBagConstraints(1, 4, 7, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel11, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fKilometer, new GridBagConstraints(5, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fTage, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel55, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));

            fahrtenDataPanel.add(jLabel58, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel59, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel60, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel61, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel62, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel63, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel64, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel65, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));

            fahrtenDataPanel.add(jLabel56, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            fahrtenDataPanel.add(fTeilnehmer, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fMannschKm, new GridBagConstraints(4, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fMaennerAnz, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fJuniorenAnz, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fFrauenAnz, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fJuniorinnenAnz, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fMaennerKm, new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fJuniorenKm, new GridBagConstraints(4, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fFrauenKm, new GridBagConstraints(4, 8, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fJuniorinnenKm, new GridBagConstraints(4, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel66, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel67, new GridBagConstraints(3, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel68, new GridBagConstraints(3, 8, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel69, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel57, new GridBagConstraints(3, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            fahrtenDataPanel.add(jLabel70, new GridBagConstraints(3, 5, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            fahrtenDataPanel.add(fUnblockButton, new GridBagConstraints(0, 11, 7, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fWirdGewertet, new GridBagConstraints(0, 12, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fNichtGewertetGrundLabel, new GridBagConstraints(3, 12, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fNichtGewertetGrund, new GridBagConstraints(5, 12, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

            fahrtenDataPanel.add(fNachfrageCheckBox, new GridBagConstraints(0, 13, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            fahrtenDataPanel.add(fNachfrage, new GridBagConstraints(1, 13, 6, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

            fahrtenDataPanel.add(jLabel22, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel31, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel32, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel33, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel34, new GridBagConstraints(0, 10, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel35, new GridBagConstraints(0, 11, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel36, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel37, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel38, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel39, new GridBagConstraints(0, 7, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel40, new GridBagConstraints(0, 9, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel41, new GridBagConstraints(0, 8, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mNachname, new GridBagConstraints(2, 1, 5, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mVorname, new GridBagConstraints(2, 2, 5, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mJahrgang, new GridBagConstraints(2, 3, 5, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mGeschlecht, new GridBagConstraints(2, 4, 5, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mGruppe, new GridBagConstraints(2, 5, 5, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mKilometer, new GridBagConstraints(2, 6, 5, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mAbzeichen, new GridBagConstraints(2, 7, 5, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mAequatorpreis, new GridBagConstraints(2, 8, 5, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mAnzAbzeichen, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mGesKm, new GridBagConstraints(2, 10, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel30, new GridBagConstraints(7, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 10), 0, 0));
            teilnDataPanel.add(jLabel42, new GridBagConstraints(7, 0, 6, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel43, new GridBagConstraints(7, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel44, new GridBagConstraints(8, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel45, new GridBagConstraints(9, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel46, new GridBagConstraints(10, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel47, new GridBagConstraints(11, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel48, new GridBagConstraints(12, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            //@AB teilnDataPanel.add(jLabel51, new GridBagConstraints(3, 9, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            //@AB teilnDataPanel.add(jLabel52, new GridBagConstraints(4, 10, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
            //@AB teilnDataPanel.add(mAnzAbzeichenAB, new GridBagConstraints(5, 9, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            //@AB teilnDataPanel.add(mGesKmAB, new GridBagConstraints(6, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mUnblockButton, new GridBagConstraints(1, 12, 13, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mFahrtenheft, new GridBagConstraints(2, 11, 8, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mWirdGewertet, new GridBagConstraints(0, 13, 4, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(jLabel53, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mTeilnNr, new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnehmerPanel.add(teilnWarnPanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
            teilnWarnPanel.add(jScrollPane1, BorderLayout.CENTER);
            jScrollPane1.getViewport().add(mWarnungen, null);
            teilnDataPanel.add(mTeilnSuchenButton, new GridBagConstraints(4, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mNichtGewertetGrundLabel, new GridBagConstraints(4, 13, 4, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            teilnDataPanel.add(mNichtGewertetGrund, new GridBagConstraints(8, 13, 5, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
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
        if (!checkAndSaveChangedMeldung()) {
            return;
        }
        if (changed) {
            switch (Dialog.yesNoCancelDialog("Änderungen speichern", "Sollen die Änderungen an der Meldedatei gespeichert werden?")) {
                case Dialog.YES:
                    if (!saveMeldedatei()) {
                        Logger.log(Logger.ERROR, "Speichern der Meldedatei ist fehlgeschlagen!");
                        Dialog.error("Speichern der Meldedatei ist fehlgeschlagen!");
                        return;
                    }
                case Dialog.NO:
                    break;
                default:
                    return;
            }
        }
        log(false, "ENDE Bearbeiten der Meldung");
        Dialog.frameClosed(this);
        dispose();
    }

    /**Close the dialog on a button event*/
    public void actionPerformed(ActionEvent e) {
    }

    void iniFields() {
        switch (MELDTYP) {
            case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                this.mGeschlecht.addItem("ungültig");
                hGeschlecht.put("", new Integer(0));
                this.mGeschlecht.addItem("männlich");
                hGeschlecht.put(EfaWettMeldung.GESCHLECHT_M, new Integer(1));
                this.mGeschlecht.addItem("weiblich");
                hGeschlecht.put(EfaWettMeldung.GESCHLECHT_W, new Integer(2));

                this.mGruppe.addItem("ungültig");
                hGruppe.put("", new Integer(0));
                this.mGruppe.addItem("1a (Männer 19-30)");
                hGruppe.put("1a", new Integer(1));
                this.mGruppe.addItem("1b (Männer 31-60)");
                hGruppe.put("1b", new Integer(2));
                this.mGruppe.addItem("1c (Männer 61-75)");
                hGruppe.put("1c", new Integer(3));
                this.mGruppe.addItem("1d (Männer 76-??)");
                hGruppe.put("1d", new Integer(4));
                this.mGruppe.addItem("1x (Männer 50% Beh.)");
                hGruppe.put("1 (50% Behinderung)", new Integer(5));
                this.mGruppe.addItem("2a (Frauen 19-30)");
                hGruppe.put("2a", new Integer(6));
                this.mGruppe.addItem("2b (Frauen 31-60)");
                hGruppe.put("2b", new Integer(7));
                this.mGruppe.addItem("2c (Frauen 61-75)");
                hGruppe.put("2c", new Integer(8));
                this.mGruppe.addItem("2d (Frauen 76-??)");
                hGruppe.put("2d", new Integer(9));
                this.mGruppe.addItem("2x (Frauen 50% Beh.)");
                hGruppe.put("2 (50% Behinderung)", new Integer(10));
                this.mGruppe.addItem("3a (Jugend 8-10)");
                hGruppe.put("3a", new Integer(11));
                this.mGruppe.addItem("3b (Jugend 11-12)");
                hGruppe.put("3b", new Integer(12));
                this.mGruppe.addItem("3c (Jugend 13-14)");
                hGruppe.put("3c", new Integer(13));
                this.mGruppe.addItem("3d (Jugend 15-16)");
                hGruppe.put("3d", new Integer(14));
                this.mGruppe.addItem("3e (Jugend 17-18)");
                hGruppe.put("3e", new Integer(15));
                this.mGruppe.addItem("3f (Jugend 50% Beh.)");
                hGruppe.put("3f (50% Behinderung)", new Integer(16));

                hAbzeichen.put("", "ungültig");
                hAbzeichen.put(EfaWettMeldung.ABZEICHEN_ERW_EINF, "Erwachsene einfach");
                hAbzeichen.put(EfaWettMeldung.ABZEICHEN_ERW_GOLD_PRAEFIX, "Erwachsene gold");
                hAbzeichen.put(EfaWettMeldung.ABZEICHEN_JUG_EINF, "Jugend einfach");
                hAbzeichen.put(EfaWettMeldung.ABZEICHEN_JUG_GOLD_PRAEFIX, "Jugend gold");
                for (int i = 1; i < EfaWettMeldung.ABZEICHEN_ERW_GOLD_LIST.length; i++) {
                    hAbzeichen.put(EfaWettMeldung.ABZEICHEN_ERW_GOLD_LIST[i], "Erwachsene gold (" + (i * 5) + ")");
                }
                for (int i = 1; i < EfaWettMeldung.ABZEICHEN_JUG_GOLD_LIST.length; i++) {
                    hAbzeichen.put(EfaWettMeldung.ABZEICHEN_JUG_GOLD_LIST[i], "Jugend gold (" + (i * 5) + ")");
                }

                hAequator.put("", "nein");
                hAequator.put("1", "1. Erreichen");
                hAequator.put("2", "2. Erreichen");
                hAequator.put("3", "3. Erreichen");
                hAequator.put("4", "4. Erreichen");
                hAequator.put("5", "5. Erreichen");
                hAequator.put("6", "6. Erreichen");
                hAequator.put("7", "7. Erreichen");
                hAequator.put("8", "8. Erreichen");
                hAequator.put("9", "9. Erreichen");
                hAequator.put("10", "10. Erreichen");
                JTextField t;
                mFahrten = new JTextField[MAX_FAHRTEN][6];
                for (int i = 0; i < MAX_FAHRTEN; i++) {
                    t = new JTextField();
                    t.setPreferredSize(new Dimension(40, 17));
                    teilnDataPanel.add(t, new GridBagConstraints(7, 2 + i, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                    mFahrten[i][0] = t;

                    t = new JTextField();
                    t.setPreferredSize(new Dimension(80, 17));
                    teilnDataPanel.add(t, new GridBagConstraints(8, 2 + i, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                    mFahrten[i][1] = t;

                    t = new JTextField();
                    t.setPreferredSize(new Dimension(80, 17));
                    teilnDataPanel.add(t, new GridBagConstraints(9, 2 + i, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                    mFahrten[i][2] = t;

                    t = new JTextField();
                    t.setPreferredSize(new Dimension(200, 17));
                    teilnDataPanel.add(t, new GridBagConstraints(10, 2 + i, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                    mFahrten[i][3] = t;

                    t = new JTextField();
                    t.setPreferredSize(new Dimension(50, 17));
                    teilnDataPanel.add(t, new GridBagConstraints(11, 2 + i, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                    mFahrten[i][4] = t;

                    t = new JTextField();
                    t.setPreferredSize(new Dimension(70, 17));
                    teilnDataPanel.add(t, new GridBagConstraints(12, 2 + i, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                    mFahrten[i][5] = t;
                }

                teilnDataPanel.add(mFahrtnachweisErbracht, new GridBagConstraints(7, 2 + MAX_FAHRTEN, 6, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

                vAktiveLabel.setVisible(false);
                vAktiveMbis18Label.setVisible(false);
                vAktiveMab19Label.setVisible(false);
                vAktiveWbis18Label.setVisible(false);
                vAktiveWab19Label.setVisible(false);
                vVereinskilometerLabel.setVisible(false);
                vAktiveMbis18.setVisible(false);
                vAktiveMab19.setVisible(false);
                vAktiveWbis18.setVisible(false);
                vAktiveWab19.setVisible(false);
                vVereinskilometer.setVisible(false);
                break;
            case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                bestellungenPanel.setVisible(false);

                vMeldenderKonto.setVisible(false);
                vMeldenderKontoLabel.setVisible(false);
                vMeldenderBank.setVisible(false);
                vMeldenderBankLabel.setVisible(false);
                vMeldenderBlz.setVisible(false);
                vMeldenderBlzLabel.setVisible(false);

                this.vZusTeilnehmer.setText("Fahrten");
                this.vZusGemTeilnehmer.setText("gemeldete Fahrten: ");
                this.papierFahrtenhefteErforderlichLabel.setVisible(false);
                this.vBetragLabel.setVisible(false);
                this.vBetragGesamtLabel.setVisible(false);
                this.vBetragMeldLabel.setVisible(false);
                this.vBetragStoffLabel.setVisible(false);
                this.vZusammenfassungEurGesamt.setVisible(false);
                this.vZusammenfassungMeldegebuehr.setVisible(false);
                this.vZusammenfassungStoffabzeichen.setVisible(false);
                this.vEingPapierhefteLabel.setVisible(false);
                this.vAnzahlPapierFahrtenhefte.setVisible(false);
                this.aequatorButton.setVisible(false);
                this.vMeldegeldEingegangen.setVisible(false);

                break;
        }
    }
    
    void iniWaterList() {
        try {
            waterList = new AutoCompleteList();
            BufferedReader f = new BufferedReader(
                    new InputStreamReader(
                    getResourceTemplate("de").openStream(),
                    Daten.ENCODING_UTF));
            String s;
            String water = null;
            ArrayList<String> waters = new ArrayList<String>();
            while ((s = f.readLine()) != null) {
                s = s.trim();
                if (s.length() == 0 || s.startsWith("#")) {
                    continue;
                }
                int pos = s.indexOf(";");
                if (pos <= 0) {
                    continue;
                }
                if (pos > 0) {
                    water = s.substring(0, pos);
                } else {
                    water = s;
                }
                if (water != null) {
                    waters.add(water);
                }
            }
            String [] wa = waters.toArray(new String[0]);
            Arrays.sort(wa);
            for (String w : wa) {
                waterList.add(w, null, true, null);
            }
        } catch (Exception e) {

        }
    }

    void log(boolean teilnehmerspezifisch, String s) {
        if (teilnehmerspezifisch && ewmCur != null) {
            switch (MELDTYP) {
                case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                    s = "QNr " + qnr + " - Teilnehmer " + ewmCur.vorname + " " + ewmCur.nachname + ": " + s;
                    break;
                case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                    s = "QNr " + qnr + " - Fahrt " + ewmCur.drvWS_StartZiel + ": " + s;
                    break;
            }
        } else {
            s = "QNr " + qnr + ": " + s;
        }
        Logger.log(Logger.INFO, s);
    }

    void warnung(String s) {
        mWarnungen.append(s + "\n");
        log(true, s);
    }

    String notNull(String s) {
        if (s == null) {
            return "";
        } else {
            return s;
        }
    }

    void printDiff(String field, String olds, String news) {
        if (olds == null && news == null) {
            return;
        }
        if ((olds == null || news == null)
                || !olds.equals(news)) {
            log(true, "Wert für Feld '" + field + "' geändert: alt='" + (olds == null ? "" : olds) + "', neu='" + (news == null ? "" : news) + "'");
        }
    }

    void readMeldedatei() {
        data = new Vector();
        for (EfaWettMeldung ewm = ew.meldung; ewm != null; ewm = ewm.next) {
            try {
                if (ewm.drv_fahrtenheft != null && ewm.drv_fahrtenheft.length() > 0) {
                    ewm.drvSignatur = new DRVSignatur(ewm.drv_fahrtenheft);
                    ewm.drvSignatur.checkSignature();
                    if (ewm.drvSignatur.getSignatureState() == DRVSignatur.SIG_VALID) {
                        ewm.drv_anzAbzeichen = Integer.toString(ewm.drvSignatur.getAnzAbzeichen());
                        ewm.drv_gesKm = Integer.toString(ewm.drvSignatur.getGesKm());
                        //@AB ewm.drv_anzAbzeichenAB = Integer.toString(ewm.drvSignatur.getAnzAbzeichenAB());
                        //@AB ewm.drv_gesKmAB = Integer.toString(ewm.drvSignatur.getGesKmAB());
                        ewm.drv_teilnNr = ewm.drvSignatur.getTeilnNr();
                        if (ewm.drvSignatur.getJahr() >= Main.drvConfig.aktJahr) {
                            ewm.sigError = (ewm.sigError == null ? "" : ewm.sigError + "\n")
                                    + "Fahrtenheft des Teilnehmers wurde für eine Meldung des Jahres " + ewm.drvSignatur.getJahr() + " ausgestellt und kann daher im aktuellen Meldejahr " + Main.drvConfig.aktJahr + " nicht bearbeitet werden.";
                            ewm.sigValid = false;
                        } else {
                            ewm.sigValid = true;
                        }
                    } else {
                        ewm.sigError = (ewm.sigError == null ? "" : ewm.sigError + "\n")
                                + "Signatur des Fahrtenhefts ist ungültig: " + ewm.drvSignatur.getSignatureError();
                        ewm.sigValid = false;
                    }
                }
            } catch (Exception e) {
                ewm.sigError = (ewm.sigError == null ? "" : ewm.sigError + "\n")
                        + "Fehler beim Überprüfen der Signatur des Fahrtenhefts: " + e.getMessage();
                ewm.sigValid = false;
            }
            switch (MELDTYP) {
                case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                    if (!ewm.drvint_wirdGewertetExplizitGesetzt || ewm.drvint_wirdGewertet) {
                        // Die Eigenschaft "wirdGewertet" kann auch vom DRV durch eine frühere Bearbeitung bereits in die Datei
                        // geschrieben worden sein. In diesem Fall hat "wirdGewertetExplizitGesetzt" den Wert "true".
                        // Beim Einlesen der Meldedatei wird die Eigenschaft "wirdGewertet" nur dann von efa neu bestimmt, wenn
                        // sie nicht zuvor explizit gesetzt war, oder wenn sie "true" ist (in diesem Fall bleibt das "true" nur
                        // erhalten, wenn die Meldung wirklich gültig ist).
                        ewm.drvint_wirdGewertet = ((ewm.sigValid || ewm.drvSignatur == null) && isErfuellt(ewm, false));
                    }
                    break;
                case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                    if (!ewm.drvint_wirdGewertetExplizitGesetzt || ewm.drvint_wirdGewertet) {
                        // Die Eigenschaft "wirdGewertet" kann auch vom DRV durch eine frühere Bearbeitung bereits in die Datei
                        // geschrieben worden sein. In diesem Fall hat "wirdGewertetExplizitGesetzt" den Wert "true".
                        // Beim Einlesen der Meldedatei wird die Eigenschaft "wirdGewertet" nur dann von efa neu bestimmt, wenn
                        // sie nicht zuvor explizit gesetzt war, oder wenn sie "true" ist (in diesem Fall bleibt das "true" nur
                        // erhalten, wenn die Meldung wirklich gültig ist).
                        ewm.drvint_wirdGewertet = isErfuellt(ewm, false);
                    }
                    ewm.drvint_geprueft = (checkAndCorrectWSAnz(ewm, false) == null) && (checkAndCorrectWSKm(ewm, false) == null) && ewm.drvint_wirdGewertet;
                    break;
            }
            data.add(ewm);
        }
    }

    void replaceMeldung() {
        ewmCur.changed = false;
        EfaWettMeldung ewmOld = (EfaWettMeldung) data.get(ewmNr);
        if (ewmOld != null) {
            printDiff("Vorname", ewmOld.vorname, ewmCur.vorname);
            printDiff("Nachname", ewmOld.nachname, ewmCur.nachname);
            printDiff("Jahrgang", ewmOld.jahrgang, ewmCur.jahrgang);
            printDiff("Geschlecht", ewmOld.geschlecht, ewmCur.geschlecht);
            printDiff("Gruppe", ewmOld.gruppe, ewmCur.gruppe);
            printDiff("Kilometer", ewmOld.kilometer, ewmCur.kilometer);
            printDiff("Abzeichen", ewmOld.abzeichen, ewmCur.abzeichen);
            printDiff("AnzAbzeichen", ewmOld.drv_anzAbzeichen, ewmCur.drv_anzAbzeichen);
            printDiff("GesKm", ewmOld.drv_gesKm, ewmCur.drv_gesKm);
            //@AB printDiff("AnzAbzeichenAB", ewmOld.drv_anzAbzeichenAB, ewmCur.drv_anzAbzeichenAB);
            //@AB printDiff("GesKmAB", ewmOld.drv_gesKmAB, ewmCur.drv_gesKmAB);
            printDiff("Fahrtenheft", ewmOld.drv_fahrtenheft, ewmCur.drv_fahrtenheft);
            printDiff("Äquatorpreis", ewmOld.drv_aequatorpreis, ewmCur.drv_aequatorpreis);
            printDiff("LfdNr", ewmOld.drvWS_LfdNr, ewmCur.drvWS_LfdNr);
            printDiff("Weg und Ziel der Fahrt", ewmOld.drvWS_StartZiel, ewmCur.drvWS_StartZiel);
            printDiff("Strecke", ewmOld.drvWS_Strecke, ewmCur.drvWS_Strecke);
            printDiff("Gewässer", ewmOld.drvWS_Gewaesser, ewmCur.drvWS_Gewaesser);
            printDiff("Anzahl Teilnehmer", ewmOld.drvWS_Teilnehmer, ewmCur.drvWS_Teilnehmer);
            printDiff("Kilometer", ewmOld.drvWS_Km, ewmCur.drvWS_Km);
            printDiff("Tage", ewmOld.drvWS_Tage, ewmCur.drvWS_Tage);
            printDiff("Mannschafs-Kilometer", ewmOld.drvWS_MannschKm, ewmCur.drvWS_MannschKm);
            printDiff("Männer (Anzahl)", ewmOld.drvWS_MaennerAnz, ewmCur.drvWS_MaennerAnz);
            printDiff("Männer (Kilometer)", ewmOld.drvWS_MaennerKm, ewmCur.drvWS_MaennerKm);
            printDiff("Junioren (Anzahl)", ewmOld.drvWS_JuniorenAnz, ewmCur.drvWS_JuniorenAnz);
            printDiff("Junioren (Kilometer)", ewmOld.drvWS_JuniorenKm, ewmCur.drvWS_JuniorenKm);
            printDiff("Frauen (Anzahl)", ewmOld.drvWS_FrauenAnz, ewmCur.drvWS_FrauenAnz);
            printDiff("Frauen (Kilometer)", ewmOld.drvWS_FrauenKm, ewmCur.drvWS_FrauenKm);
            printDiff("Juniorinnen (Anzahl)", ewmOld.drvWS_JuniorinnenAnz, ewmCur.drvWS_JuniorinnenAnz);
            printDiff("Juniorinnen (Kilometer)", ewmOld.drvWS_JuniorinnenKm, ewmCur.drvWS_JuniorinnenKm);
            printDiff("Bemerkungen", ewmOld.drvWS_Bemerkungen, ewmCur.drvWS_Bemerkungen);
            for (int i = 0; i < ewmOld.fahrt.length && i < ewmCur.fahrt.length; i++) {
                for (int j = 0; j < ewmOld.fahrt[i].length && j < ewmCur.fahrt[i].length; j++) {
                    printDiff("Fahrt[" + i + "][" + j + "]", ewmOld.fahrt[i][j], ewmCur.fahrt[i][j]);
                }
            }
        }
        data.remove(ewmNr);
        data.add(ewmNr, ewmCur);
        changed = true;
    }

    int getGeschlechtIndex(String s) {
        if (s == null) {
            warnung("Ungültiger Wert für Geschlecht: " + s);
            return 0;
        }
        Integer i = (Integer) hGeschlecht.get(s);
        if (i == null) {
            warnung("Ungültiger Wert für Geschlecht: " + s);
            return 0;
        } else {
            return i.intValue();
        }
    }

    int getGruppeIndex(EfaWettMeldung ewm) {
        if (ewm.gruppe == null) {
            warnung("Ungültiger Wert für Gruppe: " + ewm.gruppe);
            return 0;
        }
        Integer i = (Integer) hGruppe.get(ewm.gruppe);
        if (i == null) {
            warnung("Ungültiger Wert für Gruppe: " + ewm.gruppe);
            return 0;
        } else {
            return i.intValue();
        }
    }

    String getAbzeichen(EfaWettMeldung ewm) {
        if (ewm.abzeichen == null) {
            warnung("Ungültiger Wert für Abzeichen: " + ewm.abzeichen);
            return "";
        }
        String s = (String) hAbzeichen.get(ewm.abzeichen);
        if (s == null) {
            warnung("Ungültiger Wert für Abzeichen: " + ewm.abzeichen);
            return "";
        } else {
            return s;
        }
    }

    String getAequator(EfaWettMeldung ewm) {
        if (ewm.drv_aequatorpreis == null) {
            return (String) hAequator.get("");
        }
        String s = (String) hAequator.get(ewm.drv_aequatorpreis);
        if (s == null) {
            return (String) hAequator.get("");
        } else {
            return s;
        }
    }

    void setFahrtenheftState(EfaWettMeldung ewm) {
        if (ewm.drvSignatur == null) {
            if (ewm.drv_anzAbzeichen != null && EfaUtil.string2int(ewm.drv_anzAbzeichen, 0) > 0) {
                mFahrtenheft.setText("Papier-Fahrtenheft erforderlich!");
                mFahrtenheft.setForeground(Color.red);
            } else {
                mFahrtenheft.setText("Kein Fahrtenheft erforderlich!");
                mFahrtenheft.setForeground(Daten.colorGreen);
            }
        } else {
            if (ewm.sigValid) {
                mFahrtenheft.setText("Das elektronische Fahrtenheft ist gültig! (Letzte Meldung: " + ewm.drvSignatur.getJahr() + ")");
                mFahrtenheft.setForeground(Daten.colorGreen);
            } else {
                mFahrtenheft.setText("Das elektronische Fahrtenheft ist ungültig!");
                mFahrtenheft.setForeground(Color.red);
            }
        }
    }

    String findValue(Hashtable h, Object search) {
        if (h == null) {
            return null;
        }
        Object[] keys = h.keySet().toArray();
        for (int j = 0; j < keys.length; j++) {
            Object found = h.get(keys[j]);
            if (found != null && found.equals(search)) {
                return (String) keys[j];
            }
        }
        return null;
    }

    String notEmpty(JTextField t) {
        String s = t.getText().trim();
        if (s.length() == 0) {
            return null;
        }
        return s;
    }

    String field2int(JTextField t) {
        int i = EfaUtil.string2date(t.getText(), -1, 0, 0).tag;
        if (i < 0) {
            return null;
        }
        return Integer.toString(i);
    }

    String field2jahr(JTextField t) {
        int i = EfaUtil.string2date(t.getText(), -1, 0, 0).tag;
        if (i < 0) {
            return null;
        }
        if (i < 100) {
            i += 1900;
        }
        return Integer.toString(i);
    }

    String checkAndCorrectAbzeichen(EfaWettMeldung ewm) {
        if (ewm == null) {
            return "null";
        }
        boolean erwachsen = true;
        if (ewm.gruppe != null && ewm.gruppe.startsWith("3")) {
            erwachsen = false;
        }
        int anzAbzeichen = EfaUtil.string2int(ewm.drv_anzAbzeichen, 0);
        //@AB int anzAbzeichenAB = EfaUtil.string2int(ewm.drv_anzAbzeichenAB, 0);
        //@AB String abzeichen = WettDefs.getDRVAbzeichen(erwachsen, anzAbzeichen, anzAbzeichenAB, Main.drvConfig.aktJahr);
        String abzeichen = WettDefs.getDRVAbzeichen(erwachsen, anzAbzeichen, 0, Main.drvConfig.aktJahr);
        if (abzeichen != null && ewm.abzeichen != null && !abzeichen.equals(ewm.abzeichen)) {
            if (ewm.abzeichen.length() == 2 && abzeichen.startsWith(ewm.abzeichen)) {
                // ok (altes Format), nothing to do
            } else {
                String gem = ewm.abzeichen;
                ewm.abzeichen = abzeichen;
                ewm.changed = true;
                return "Teilnehmer hat für Abzeichen '" + gem + "' gemeldet, hat aber Abzeichen '" + abzeichen + "' erlangt. Abzeichen wurde korrigiert!";
            }
        }
        return null;
    }

    String checkAndCorrectAequator(EfaWettMeldung ewm) {
        int gesKm = EfaUtil.string2int(ewm.drv_gesKm, 0);
        //@AB int gesKmAB = EfaUtil.string2int(ewm.drv_gesKmAB, 0);
        int aeqKm = gesKm; // - gesKmAB; (seit 2007 zählen auch die AB-Kilometer zum Äquatorpreis)
        int anzAeqBefore = aeqKm / WettDefs.DRV_AEQUATOR_KM;
        int anzAeqJetzt = (aeqKm + EfaUtil.string2int(ewm.kilometer, 0)) / WettDefs.DRV_AEQUATOR_KM;
        String aeq = null;
        if (anzAeqJetzt > anzAeqBefore) {
            aeq = Integer.toString(anzAeqJetzt);
        }
        if (ewm.drv_aequatorpreis != null && ewm.drv_aequatorpreis.length() == 0) {
            ewm.drv_aequatorpreis = null;
        }
        if (aeq != null || ewm.drv_aequatorpreis != null) {
            if (aeq == null) {
                String gem = ewm.drv_aequatorpreis;
                ewm.drv_aequatorpreis = aeq;
                ewm.changed = true;
                return "Teilnehmer hat für Äquatorpreis '" + gem + "' gemeldet, hat ihn aber nicht erfüllt. Äquatorpreis wurde korrigiert!";
            } else if (ewm.drv_aequatorpreis == null) {
                ewm.drv_aequatorpreis = aeq;
                ewm.changed = true;
                return "Teilnehmer hat für Äquatorpreis nicht gemeldet, hat ihn aber zum " + aeq + ". Mal erfüllt. Äquatorpreis wurde korrigiert!";
            } else if (!aeq.equals(ewm.drv_aequatorpreis)) {
                String gem = ewm.drv_aequatorpreis;
                ewm.drv_aequatorpreis = aeq;
                ewm.changed = true;
                return "Teilnehmer hat für Äquatorpreis '" + gem + "' gemeldet, hat ihn aber zum " + aeq + ". Mal erfüllt. Äquatorpreis wurde korrigiert!";
            }
        }
        return null;
    }

    String checkAnzAbzeichenUndKm(EfaWettMeldung ewm) {
        if (ewm == null) {
            return "null";
        }
        int anzAbzeichen = EfaUtil.string2int(ewm.drv_anzAbzeichen, 0);
        //@AB int anzAbzeichenAB = EfaUtil.string2int(ewm.drv_anzAbzeichenAB, 0);
        int gesKm = EfaUtil.string2int(ewm.drv_gesKm, 0);
        int gesKmAB = EfaUtil.string2int(ewm.drv_gesKmAB, 0);

        //@AB if (anzAbzeichenAB > anzAbzeichen) {
        //@AB     return "Der Teilnehmer darf nicht mehr A/B-Abzeichen als Gesamt-Abzeichen haben! Bitte überprüfen!";
        //@AB }
        //@AB if (gesKmAB > gesKm) {
        //@AB     return "Der Teilnehmer darf nicht mehr A/B-Kilometer als Gesamt-Kilometer haben! Bitte überprüfen!";
        //@AB }
        return null;
    }

    String checkAndCorrectWSAnz(EfaWettMeldung ewm, boolean korrigiere) {
        if (ewm == null) {
            return "null";
        }

        int maenner = EfaUtil.string2int(ewm.drvWS_MaennerAnz, 0);
        int frauen = EfaUtil.string2int(ewm.drvWS_FrauenAnz, 0);
        int junioren = EfaUtil.string2int(ewm.drvWS_JuniorenAnz, 0);
        int juniorinnen = EfaUtil.string2int(ewm.drvWS_JuniorinnenAnz, 0);
        int gesamt = EfaUtil.string2int(ewm.drvWS_Teilnehmer, 0);
        if (maenner + frauen + junioren + juniorinnen != gesamt) {
            String gem = ewm.drvWS_Teilnehmer;
            if (korrigiere) {
                ewm.drvWS_Teilnehmer = Integer.toString(maenner + frauen + junioren + juniorinnen);
                ewm.changed = true;
            }
            return "Gemeldete Teilnehmer-Anzahl '" + gem + "' stimmt nicht mit Summe der einzelnen Altersklassen überein. Teilnehmer-Anzahl wurde korrigiert!";
        }
        return null;
    }

    String checkAndCorrectWSKm(EfaWettMeldung ewm, boolean korrigiere) {
        if (ewm == null) {
            return "null";
        }

        int maenner = EfaUtil.zehntelString2Int(ewm.drvWS_MaennerKm);
        int frauen = EfaUtil.zehntelString2Int(ewm.drvWS_FrauenKm);
        int junioren = EfaUtil.zehntelString2Int(ewm.drvWS_JuniorenKm);
        int juniorinnen = EfaUtil.zehntelString2Int(ewm.drvWS_JuniorinnenKm);
        int gesamt = EfaUtil.zehntelString2Int(ewm.drvWS_MannschKm);
        if (maenner + frauen + junioren + juniorinnen != gesamt) {
            String gem = ewm.drvWS_MannschKm;
            if (korrigiere) {
                ewm.drvWS_MannschKm = EfaUtil.zehntelInt2String(maenner + frauen + junioren + juniorinnen);
                ewm.changed = true;
            }
            return "Gemeldete Mannschafts-Kilometer '" + gem + "' stimmen nicht mit Summe der einzelnen Altersklassen überein. Mannschafts-Kilometer wurde korrigiert!";
        }
        return null;
    }

    void setMeldegeldEingegangen(boolean eingegangen) {
        if (eingegangen) {
            this.vMeldegeldEingegangen.setForeground(Color.blue);
        } else {
            this.vMeldegeldEingegangen.setForeground(Color.red);
        }
        this.vMeldegeldEingegangen.setSelected(eingegangen);
    }

    void setVFields() {
        this.infoVereinLabel.setText(ew.verein_name);
        this.infoQnrLabel.setText(qnr);

        this.vKennung.setText(ew.kennung);
        this.vProgramm.setText(ew.allg_programm);

        this.vNutzername.setText(notNull(ew.verein_user));
        this.vVereinsname.setText(notNull(ew.verein_name));
        this.vMitgliedsnr.setText(notNull(ew.verein_mitglnr));
        this.vLandesverband.setText(notNull(ew.verein_lrv));

        this.vMeldenderName.setText(notNull(ew.meld_name));
        this.vMeldenderEmail.setText(notNull(ew.meld_email));

        this.vVersandName.setText(notNull(ew.versand_name));
        this.vVersandZusatz.setText(notNull(ew.versand_zusatz));
        this.vVersantStrasse.setText(notNull(ew.versand_strasse));
        this.vVersandOrt.setText(notNull(ew.versand_ort));

        this.vNotes.setText(notNull(ew.drvint_notes));

        switch (MELDTYP) {
            case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                this.vMeldenderKonto.setText(notNull(ew.meld_kto));
                this.vMeldenderBank.setText(notNull(ew.meld_bank));
                this.vMeldenderBlz.setText(notNull(ew.meld_blz));

                this.vBestNadelErwSilber.setText(notNull(ew.drv_nadel_erw_silber));
                if (ew.drv_nadel_erw_gold != null) {
                    this.vBestNadelErwGold.setText(Integer.toString(EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_erw_gold, ','))));
                }
                this.vBestNadelJugSilber.setText(notNull(ew.drv_nadel_jug_silber));
                if (ew.drv_nadel_jug_gold != null) {
                    this.vBestNadelJugGold.setText(Integer.toString(EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_jug_gold, ','))));
                }
                this.vBestStoffErw.setText(notNull(ew.drv_stoff_erw));
                this.vBestStoffJug.setText(notNull(ew.drv_stoff_jug));

                setMeldegeldEingegangen(ew.drvint_meldegeldEingegangen);
                if (ew.drvint_anzahlPapierFahrtenhefte >= 0) {
                    this.vAnzahlPapierFahrtenhefte.setText(Integer.toString(ew.drvint_anzahlPapierFahrtenhefte));
                }
                break;
            case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                this.vAktiveMbis18.setText(notNull(ew.aktive_M_bis18));
                this.vAktiveMab19.setText(notNull(ew.aktive_M_ab19));
                this.vAktiveWbis18.setText(notNull(ew.aktive_W_bis18));
                this.vAktiveWab19.setText(notNull(ew.aktive_W_ab19));
                this.vVereinskilometer.setText(notNull(ew.vereins_kilometer));
                break;
        }
        vBlock(true);
    }

    boolean checkAndSaveChangedMeldung() {
        if (ewmCur != null && (ewmCur.changed || !mBlocked)) {
            switch (Dialog.yesNoCancelDialog("Änderungen speichern",
                    "Änderungen an Eintrag " + (ewmNr + 1) + " wurden noch nicht gespeichert. Jetzt speichern?")) {
                case Dialog.YES:
                    if (!mBlocked) {
                        getMFields(ewmCur);
                    }
                    replaceMeldung();
                    return true;
                case Dialog.NO:
                    // nothing to do
                    return true;
                default: // Cancel
                    return false;
            }
        }
        return true;
    }

    void setMFields(int nr, boolean bestimmeWirdGewertetNeu) {
        if (nr < 0 || nr >= data.size()) {
            return;
        }

        if (!checkAndSaveChangedMeldung()) {
            return;
        }

        mWarnungen.setText("");
        mBlock(true);

        EfaWettMeldung tmpefw = (EfaWettMeldung) data.get(nr);

        if (tmpefw == null) {
            warnung("Meldung #" + (nr + 1) + " ist nicht vorhanden!");
            return;
        }
        tmpefw.drvint_geprueft = true;
        ewmCur = new EfaWettMeldung(tmpefw);
        ewmNr = nr;

        switch (MELDTYP) {
            case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                if (ewmCur.sigError != null) {
                    warnung(ewmCur.sigError);
                }
                if (isErfuellt(ewmCur, true)) {
                    if (bestimmeWirdGewertetNeu && !ewmCur.drvint_wirdGewertetExplizitGesetzt) {
                        ewmCur.drvint_wirdGewertet = true;
                        tmpefw.drvint_wirdGewertet = true;
                        mNichtGewertetGrund.setText("");
                    }
                } else {
                    if (bestimmeWirdGewertetNeu && !ewmCur.drvint_wirdGewertetExplizitGesetzt) {
                        ewmCur.drvint_wirdGewertet = false;
                        tmpefw.drvint_wirdGewertet = false;
                    }
                    if (ewmCur.drvint_nichtGewertetGrund == null) {
                        ewmCur.drvint_nichtGewertetGrund = "Wettbewerbsbedingungen nicht erfüllt";
                        tmpefw.drvint_nichtGewertetGrund = ewmCur.drvint_nichtGewertetGrund;
                    }
                    warnung("Der Teilnehmer hat die Bedingungen für den Wettbewerb nicht erfüllt!");
                }
                String s;
                s = checkAndCorrectAbzeichen(ewmCur);
                if (s != null) {
                    warnung(s);
                }
                s = checkAndCorrectAequator(ewmCur);
                if (s != null) {
                    warnung(s);
                }
                s = checkAnzAbzeichenUndKm(ewmCur);
                if (s != null) {
                    warnung(s);
                }

                this.mTeilnNr.setText(notNull(ewmCur.drv_teilnNr));
                this.mNachname.setText(notNull(ewmCur.nachname));
                this.mVorname.setText(notNull(ewmCur.vorname));
                this.mJahrgang.setText(notNull(ewmCur.jahrgang));
                this.mGeschlecht.setSelectedIndex(getGeschlechtIndex(ewmCur.geschlecht));
                this.mGruppe.setSelectedIndex(getGruppeIndex(ewmCur));
                this.mKilometer.setText(notNull(ewmCur.kilometer));
                this.mAbzeichen.setText(getAbzeichen(ewmCur));
                String aeq = getAequator(ewmCur);
                this.mAequatorpreis.setText(aeq);
                mAequatorpreis.setForeground((!aeq.startsWith("(") && !aeq.equals("nein") ? Color.blue : Color.black));
                this.mAnzAbzeichen.setText(notNull(ewmCur.drv_anzAbzeichen));
                this.mGesKm.setText(notNull(ewmCur.drv_gesKm));
                //@AB this.mAnzAbzeichenAB.setText(notNull(ewmCur.drv_anzAbzeichenAB));
                //@AB this.mGesKmAB.setText(notNull(ewmCur.drv_gesKmAB));
                this.mWirdGewertet.setSelected(ewmCur.drvint_wirdGewertet);
                updateStatusNichtGewertetGrund();
                this.mNichtGewertetGrund.setText((ewmCur.drvint_nichtGewertetGrund == null ? "" : ewmCur.drvint_nichtGewertetGrund));
                setFahrtenheftState(ewmCur);

                if (ewmCur.fahrt != null) {
                    for (int i = 0; i < ewmCur.fahrt.length && i < MAX_FAHRTEN; i++) {
                        for (int j = 0; ewmCur.fahrt[i] != null && j < ewmCur.fahrt[i].length && j < 6; j++) {
                            mFahrten[i][j].setText(ewmCur.fahrt[i][j]);
                        }
                    }
                    mFahrtnachweisErbracht.setSelected(ewmCur.drvint_fahrtErfuellt);
                    int anzFahrten = 0;
                    for (int i = 0; i < ewmCur.fahrt.length; i++) {
                        if (ewmCur.fahrt[i] != null && ewmCur.fahrt[i].length > 0 && ewmCur.fahrt[i][0] != null) {
                            anzFahrten = i;
                        }
                    }

                    if (anzFahrten >= MAX_FAHRTEN) {
                        warnung("Für den Teilnehmer sind mehr als " + MAX_FAHRTEN + " Fahrten angegeben; die restlichen Fahrten werden ignoriert.");
                    }
                }
                this.titledBorderTeilnehmer.setTitle("Teilnehmer " + (nr + 1) + " / " + data.size());
                this.teilnDataPanel.repaint();
                break;
            case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                if (isErfuellt(ewmCur, true)) {
                    if (bestimmeWirdGewertetNeu) {
                        ewmCur.drvint_wirdGewertet = true;
                        tmpefw.drvint_wirdGewertet = true;
                        fNichtGewertetGrund.setText("");
                    }
                } else {
                    if (bestimmeWirdGewertetNeu) {
                        ewmCur.drvint_wirdGewertet = false;
                        tmpefw.drvint_wirdGewertet = false;
                    }
                    if (ewmCur.drvint_nichtGewertetGrund == null) {
                        ewmCur.drvint_nichtGewertetGrund = "Wettbewerbsbedingungen nicht erfüllt";
                        tmpefw.drvint_nichtGewertetGrund = ewmCur.drvint_nichtGewertetGrund;
                    }
                    warnung("Die Fahrt entspricht nicht den geforderten Bedingungen für eine Wanderfahrt!");
                }
                s = checkAndCorrectWSAnz(ewmCur, true);
                if (s != null) {
                    warnung(s);
                }
                s = checkAndCorrectWSKm(ewmCur, true);
                if (s != null) {
                    warnung(s);
                }

                this.fLfdNr.setText(notNull(ewmCur.drvWS_LfdNr));
                this.fStartZiel.setText(notNull(ewmCur.drvWS_StartZiel));
                this.fStrecke.setText(notNull(ewmCur.drvWS_Strecke));
                this.fGewaesser.setText(notNull(ewmCur.drvWS_Gewaesser));
                this.fTage.setText(notNull(ewmCur.drvWS_Tage));
                this.fKilometer.setText(notNull(ewmCur.drvWS_Km));
                this.fMaennerAnz.setText(notNull(ewmCur.drvWS_MaennerAnz));
                this.fMaennerKm.setText(notNull(ewmCur.drvWS_MaennerKm));
                this.fJuniorenAnz.setText(notNull(ewmCur.drvWS_JuniorenAnz));
                this.fJuniorenKm.setText(notNull(ewmCur.drvWS_JuniorenKm));
                this.fFrauenAnz.setText(notNull(ewmCur.drvWS_FrauenAnz));
                this.fFrauenKm.setText(notNull(ewmCur.drvWS_FrauenKm));
                this.fJuniorinnenAnz.setText(notNull(ewmCur.drvWS_JuniorinnenAnz));
                this.fJuniorinnenKm.setText(notNull(ewmCur.drvWS_JuniorinnenKm));
                this.fTeilnehmer.setText(notNull(ewmCur.drvWS_Teilnehmer));
                this.fMannschKm.setText(notNull(ewmCur.drvWS_MannschKm));
                this.fBemerkungen.setText(notNull(ewmCur.drvWS_Bemerkungen));

                this.fWirdGewertet.setSelected(ewmCur.drvint_wirdGewertet);
                updateStatusNichtGewertetGrund();
                this.fNichtGewertetGrund.setText((ewmCur.drvint_nichtGewertetGrund == null ? "" : ewmCur.drvint_nichtGewertetGrund));
                this.fNachfrageCheckBox.setSelected(ewmCur.drvint_nachfrage != null && ewmCur.drvint_nachfrage.length() > 0);
                this.fNachfrage.setText((ewmCur.drvint_nachfrage != null ? ewmCur.drvint_nachfrage : ""));
                updateStatusNachfrage();

                this.titledBorderFahrten.setTitle("Fahrt " + (nr + 1) + " / " + data.size());
                this.fahrtenDataPanel.repaint();
                break;
        }

        ewmNr = nr;
    }

    void getMFields(EfaWettMeldung ewm) {
        switch (MELDTYP) {
            case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                // Teilnnr überprüfen
                if (mTeilnNr.getText().trim().length() > 0 && Main.drvConfig.teilnehmer != null) {
                    DatenFelder d = Main.drvConfig.teilnehmer.getExactComplete(mTeilnNr.getText().trim());
                    if (d != null) {
                        if (!d.get(Teilnehmer.VORNAME).equals(mVorname.getText().trim())
                                || !d.get(Teilnehmer.NACHNAME).equals(mNachname.getText().trim())
                                || !d.get(Teilnehmer.JAHRGANG).equals(mJahrgang.getText().trim())) {
                            if (Dialog.yesNoDialog("Warnung", "Es existiert bereits ein Teilnehmer mit der Nummer " + d.get(Teilnehmer.TEILNNR) + ",\n"
                                    + "jedoch hat dieser Teilnehmer andere Daten:\n"
                                    + "'" + d.get(Teilnehmer.VORNAME) + " " + d.get(Teilnehmer.NACHNAME) + " (" + d.get(Teilnehmer.JAHRGANG) + ")'\n"
                                    + "Soll der aktuell gewählte Teilnehmer dennoch diese Teilnehmernummer erhalten?\n"
                                    + "(Bitte wähle nur JA, wenn es sich bei beiden Teilnehmern um denselben Teilnehmer handelt!)") != Dialog.YES) {
                                return;
                            }
                        }
                    }
                }

                // Felder speichern
                ewm.drvint_wirdGewertet = mWirdGewertet.isSelected();
                if (ewm.drvint_wirdGewertet) {
                    ewm.drvint_nichtGewertetGrund = null;
                } else {
                    ewm.drvint_nichtGewertetGrund = mNichtGewertetGrund.getText().trim();
                }
                ewm.drv_teilnNr = notEmpty(mTeilnNr);
                ewm.nachname = notEmpty(mNachname);
                ewm.vorname = notEmpty(mVorname);
                ewm.jahrgang = field2jahr(mJahrgang);
                ewm.geschlecht = findValue(hGeschlecht, new Integer(mGeschlecht.getSelectedIndex()));
                ewm.gruppe = findValue(hGruppe, new Integer(mGruppe.getSelectedIndex()));
                ewm.kilometer = field2int(mKilometer);
                ewm.drv_aequatorpreis = findValue(hAequator, mAequatorpreis.getText());
                ewm.drv_anzAbzeichen = field2int(mAnzAbzeichen);
                ewm.drv_gesKm = field2int(mGesKm);
                //@AB ewm.drv_anzAbzeichenAB = field2int(mAnzAbzeichenAB);
                //@AB ewm.drv_gesKmAB = field2int(mGesKmAB);

                if (ewm.drvSignatur != null && ewm.drvSignatur.getSignatureState() == DRVSignatur.SIG_VALID) {
                    if (!ewm.drv_anzAbzeichen.equals(Integer.toString(ewm.drvSignatur.getAnzAbzeichen()))
                            || !ewm.drv_gesKm.equals(Integer.toString(ewm.drvSignatur.getGesKm()))
                            //@AB || !ewm.drv_anzAbzeichenAB.equals(Integer.toString(ewm.drvSignatur.getAnzAbzeichenAB()))
                            //@AB || !ewm.drv_gesKmAB.equals(Integer.toString(ewm.drvSignatur.getGesKmAB()))
                            ) {
                        if (Dialog.auswahlDialog("Achtung",
                                "Du hast die durch das elektronische Fahrtenheft nachgewiesene Anzahl an\n"
                                + "bereits erbrachten Abzeichen und Kilometern verändert, so daß diese nicht\n"
                                + "mehr mit dem elektronischen Fahrtenheft des Vorjahres übereinstimmen.\n"
                                + "Wenn Du diese Änderungen übernehmen möchtest, wird das elektronische Fahrtenheft\n"
                                + "des Vorjahres ignoriert.\n\n"
                                + "Was möchtest Du tun?",
                                "Änderungen verwerfen und elektr. Fahrtenheft beibehalten",
                                "Änderungen übernehmen und elektr. Fahrtenheft ignorieren",
                                false) == 0) {
                            // Änderungen verwerfen und elektr. Fahrtenheft beibehalten
                            ewm.drv_anzAbzeichen = Integer.toString(ewm.drvSignatur.getAnzAbzeichen());
                            ewm.drv_gesKm = Integer.toString(ewm.drvSignatur.getGesKm());
                            //@AB ewm.drv_anzAbzeichenAB = Integer.toString(ewm.drvSignatur.getAnzAbzeichenAB());
                            //@AB ewm.drv_gesKmAB = Integer.toString(ewm.drvSignatur.getGesKmAB());
                        } else {
                            // Änderungen übernehmen und elektr. Fahrtenheft ignorieren
                            ewm.drvSignatur = null;
                            ewm.drv_fahrtenheft = null;
                        }
                    }
                }

                if (mAbzeichen.getText().trim().length() > 0) {
                    ewm.abzeichen = findValue(hAbzeichen, mAbzeichen.getText());
                } else {
                    ewm.abzeichen = this.getAbzeichen(ewm);
                    mAbzeichen.setText(ewm.abzeichen);
                }
                ewm.fahrt = new String[EfaWettMeldung.FAHRT_ANZ_X][EfaWettMeldung.FAHRT_ANZ_Y];
                for (int i = 0; i < mFahrten.length; i++) {
                    boolean empty = true;
                    for (int j = 0; j < mFahrten[i].length; j++) {
                        if (mFahrten[i][j].getText().trim().length() > 0) {
                            empty = false;
                        }
                    }
                    if (!empty) {
                        for (int j = 0; j < mFahrten[i].length; j++) {
                            ewm.fahrt[i][j] = mFahrten[i][j].getText().trim();
                        }
                    }
                }
                ewm.drvint_fahrtErfuellt = mFahrtnachweisErbracht.isSelected();

                break;
            case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                ewm.drvint_wirdGewertet = fWirdGewertet.isSelected();
                if (ewm.drvint_wirdGewertet) {
                    ewm.drvint_nichtGewertetGrund = null;
                } else {
                    ewm.drvint_nichtGewertetGrund = fNichtGewertetGrund.getText().trim();
                }
                ewm.drvint_nachfrage = fNachfrage.getText().trim();

                ewm.drvWS_LfdNr = notEmpty(this.fLfdNr);
                ewm.drvWS_StartZiel = notEmpty(this.fStartZiel);
                ewm.drvWS_Strecke = notEmpty(this.fStrecke);
                ewm.drvWS_Gewaesser = notEmpty(this.fGewaesser);
                ewm.drvWS_Tage = notEmpty(this.fTage);
                ewm.drvWS_Km = notEmpty(this.fKilometer);
                ewm.drvWS_MaennerAnz = notEmpty(this.fMaennerAnz);
                ewm.drvWS_MaennerKm = notEmpty(this.fMaennerKm);
                ewm.drvWS_JuniorenAnz = notEmpty(this.fJuniorenAnz);
                ewm.drvWS_JuniorenKm = notEmpty(this.fJuniorenKm);
                ewm.drvWS_FrauenAnz = notEmpty(this.fFrauenAnz);
                ewm.drvWS_FrauenKm = notEmpty(this.fFrauenKm);
                ewm.drvWS_JuniorinnenAnz = notEmpty(this.fJuniorinnenAnz);
                ewm.drvWS_JuniorinnenKm = notEmpty(this.fJuniorinnenKm);
                ewm.drvWS_Teilnehmer = notEmpty(this.fTeilnehmer);
                ewm.drvWS_MannschKm = notEmpty(this.fMannschKm);
                ewm.drvWS_Bemerkungen = notEmpty(this.fBemerkungen);
                break;
        }
        ewm.changed = true;
    }

    void calcOverallValues() {
        if (data == null) {
            return;
        }
        int erfuellt = 0;
        meldegeld = 0;
        int stoffabzeichen = 0;
        int papierFahrtenhefteErforderlich = 0;
        int aequator = 0;

        for (int i = 0; i < data.size(); i++) {
            EfaWettMeldung ewm = (EfaWettMeldung) data.get(i);
            if (isErfuellt(ewm, false)) {
                erfuellt++;
                if (ewm.gruppe != null) {
                    if (ewm.gruppe.startsWith("1") || ewm.gruppe.startsWith("2")) {
                        meldegeld += Main.drvConfig.eur_meld_erw;
                    }
                    if (ewm.gruppe.startsWith("3")) {
                        meldegeld += Main.drvConfig.eur_meld_jug;
                    }
                }
            }
            if (ewm.drv_fahrtenheft == null || ewm.drv_fahrtenheft.length() == 0) {
                if (ewm.drv_anzAbzeichen != null && ewm.drv_anzAbzeichen.length() > 0
                        && EfaUtil.string2int(ewm.drv_anzAbzeichen, 0) > 0) {
                    papierFahrtenhefteErforderlich++;
                }
            }
            if (ewm.drv_aequatorpreis != null && ewm.drv_aequatorpreis.length() > 0 &&
                    EfaUtil.stringFindInt(ewm.drv_aequatorpreis, 0) >= 1) {
                aequator++;
            }
        }

        int ungueltig = data.size() - erfuellt;
        if (ungueltig > 0) {
            this.vZusammenfassungAnzTeilnehmerUngueltig.setForeground(Color.red);
        } else {
            this.vZusammenfassungAnzTeilnehmerUngueltig.setForeground(Color.black);
        }

        this.vZusammenfassungAnzTeilnehmer.setText(Integer.toString(data.size()));
        this.vZusammenfassungAnzTeilnehmerErfuellt.setText(Integer.toString(erfuellt));
        this.vZusammenfassungAnzTeilnehmerUngueltig.setText(Integer.toString(ungueltig));

        switch (MELDTYP) {
            case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                meldegeld += EfaUtil.string2int(ew.drv_nadel_erw_silber, 0) * Main.drvConfig.eur_nadel_erw_silber;
                meldegeld += EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_erw_gold, ',')) * Main.drvConfig.eur_nadel_erw_gold;
                meldegeld += EfaUtil.string2int(ew.drv_nadel_jug_silber, 0) * Main.drvConfig.eur_nadel_jug_silber;
                meldegeld += EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_jug_gold, ',')) * Main.drvConfig.eur_nadel_jug_gold;

                stoffabzeichen += EfaUtil.string2int(ew.drv_stoff_erw, 0) * Main.drvConfig.eur_stoff_erw;
                stoffabzeichen += EfaUtil.string2int(ew.drv_stoff_jug, 0) * Main.drvConfig.eur_stoff_jug;

                if (stoffabzeichen == 0) {
                    this.printStoffBestellButton.setVisible(false);
                }

                this.aequatorButton.setVisible(aequator > 0);
                this.aequatorButton.setText(aequator + " Äquatorpreisträger");
                this.papierFahrtenhefteErforderlichLabel.setVisible(papierFahrtenhefteErforderlich > 0);

                this.vZusammenfassungMeldegebuehr.setText(EfaUtil.cent2euro(meldegeld, true));
                this.vZusammenfassungStoffabzeichen.setText(EfaUtil.cent2euro(stoffabzeichen, true));
                this.vZusammenfassungEurGesamt.setText(EfaUtil.cent2euro(meldegeld + stoffabzeichen, true));

                if (ew.drvint_anzahlPapierFahrtenhefte < 0) {
                    this.vAnzahlPapierFahrtenhefte.setText(Integer.toString(papierFahrtenhefteErforderlich));
                    ew.drvint_anzahlPapierFahrtenhefte = papierFahrtenhefteErforderlich;
                }
                break;
            case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                this.vAktiveMbis18.setText(notNull(ew.aktive_M_bis18));
                this.vAktiveMab19.setText(notNull(ew.aktive_M_ab19));
                this.vAktiveWbis18.setText(notNull(ew.aktive_W_bis18));
                this.vAktiveWab19.setText(notNull(ew.aktive_W_ab19));
                this.vVereinskilometer.setText(notNull(ew.vereins_kilometer));
                break;
        }
    }

    boolean isErfuellt(EfaWettMeldung ewm, boolean testeUndKorrigiere) {
        switch (MELDTYP) {
            case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                String gruppe = getGruppe(ew, ewm);
                if (gruppe != null && ewm.gruppe != null) {
                    if (!ewm.gruppe.startsWith(gruppe) && testeUndKorrigiere) {
                        warnung("Teilnehmer hat für Gruppe '" + ewm.gruppe + "' gemeldet, hat aber für Gruppe '" + gruppe + "' erfüllt. Gruppe wurde korrigiert!");
                        ewm.gruppe = gruppe;
                        ewm.changed = true;
                    }
                }
                return gruppe != null;
            case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                int tage = EfaUtil.string2int(ewm.drvWS_Tage, 0);
                int km = EfaUtil.zehntelString2Int(ewm.drvWS_Km);
                return (tage > 0 && ((tage == 1 && km >= 300) || (tage > 1 && km >= 400)));
        }
        return false;
    }

    public static String getGruppe(EfaWett ew, EfaWettMeldung ewm) {
        int jahrgang = EfaUtil.string2int(ewm.jahrgang, 0);
        if (jahrgang <= 0) {
            return null;
        }

        int geschlecht = -1;
        if (ewm.geschlecht != null && ewm.geschlecht.equals(EfaWettMeldung.GESCHLECHT_M)) {
            geschlecht = 0;
        }
        if (ewm.geschlecht != null && ewm.geschlecht.equals(EfaWettMeldung.GESCHLECHT_W)) {
            geschlecht = 1;
        }
        if (geschlecht < 0) {
            return null;
        }

        boolean behind = ewm.gruppe != null && ewm.gruppe.endsWith("(50% Behinderung)");

        int km = EfaUtil.zehntelString2Int(ewm.kilometer);

        int wafaKm = 0;
        int wafaAnzMehrtages = 0;
        int wafaAnzTages = 0;
        int jumAnz = 0;
        if (ewm.fahrt != null) {
            for (int i = 0; i < ewm.fahrt.length && i < MAX_FAHRTEN; i++) {
                boolean jum = ewm.fahrt[i][5] != null && ewm.fahrt[i][5].equals(EfaWettMeldung.JUM);

                // Bugfix für EFA.150
                if (ew.allg_programm != null && ew.allg_programm.equals("EFA.150")
                        && ewm.fahrt[i][3] != null && ewm.fahrt[i][3].endsWith(" (JuM-Regatta)")) {
                    jum = true;
                }

                if (ewm.fahrt[i][4] != null && !jum) {
                    wafaKm += EfaUtil.zehntelString2Int(ewm.fahrt[i][4]);
                }

                if (ewm.fahrt[i][1] != null && ewm.fahrt[i][2] != null && !jum) {
                    TMJ von = EfaUtil.string2date(ewm.fahrt[i][1], 0, 0, 0);
                    TMJ bis = EfaUtil.string2date(ewm.fahrt[i][2], 0, 0, 0);
                    if (von.tag != 0 && von.monat != 0 && von.jahr != 0 && bis.tag != 0 && bis.monat != 0 && bis.jahr != 0) {
                        int tage = EfaUtil.getDateDiff(von, bis);
                        if (tage > 1) {
                            wafaAnzMehrtages += tage;
                        } else {
                            if (EfaUtil.zehntelString2Int(ewm.fahrt[i][4]) >= 300) {
                                wafaAnzTages++;
                            }
                        }
                    }
                }
                if (jum) {
                    jumAnz++;
                }
            }
        }
        if (ewm.drvint_fahrtErfuellt) { // nicht Zusatzbedingungen prüfen, sondern als erfüllt betrachten
            wafaKm = 99999;
            wafaAnzMehrtages = 99999;
            jumAnz = 99999;
        }
        return Daten.wettDefs.erfuellt(WettDefs.DRV_FAHRTENABZEICHEN, 
                Main.drvConfig.aktJahr, 
                jahrgang, geschlecht, behind, 
                km, 
                wafaKm / 10, 
                wafaAnzMehrtages + 2*wafaAnzTages, 
                jumAnz, 0);
    }

    void bestaetigenButton_actionPerformed(ActionEvent e) {
        if (data == null) {
            return;
        }
        if (!checkAndSaveChangedMeldung()) {
            return;
        }
        if (MELDTYP == MeldungenIndexFrame.MELD_FAHRTENABZEICHEN && Main.drvConfig.teilnehmer == null) {
            Dialog.error("Keine Teilnehmerdatei geladen!");
            return;
        }

        if (MELDTYP == MeldungenIndexFrame.MELD_FAHRTENABZEICHEN && !Main.drvConfig.readOnlyMode) {
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

        // schaue, ob es ungeprüfte Meldungen gibt
        int c = 0;
        int teilnErw = 0;
        int teilnJug = 0;
        for (int i = 0; i < data.size(); i++) {
            EfaWettMeldung ewm = (EfaWettMeldung) data.get(i);
            if (!ewm.drvint_geprueft && !Main.drvConfig.readOnlyMode) {
                Dialog.error("Meldung " + (i + 1) + " wurde noch nicht geprüft! Es müssen zuerst alle Meldungen geprüft werden!");
                setMFields(i, false);
                this.jTabbedPane1.setSelectedIndex(1);
                return;
            } else {
                if (ewm.drvint_wirdGewertet) {
                    c++;
                }
            }
        }

        // schaue, ob es Meldungen gibt, die nicht gewertet werden sollen
        if (c < data.size()) {
            if (Dialog.yesNoDialog("Warnung", "Es sind nur " + c + " von " + data.size() + " Teilnehmern als 'wird gewertet' markiert.\nIst das korrekt?") != Dialog.YES) {
                return;
            }
        }

        // prüfen, ob Signaturdatei bereits existiert
        if (MELDTYP == MeldungenIndexFrame.MELD_FAHRTENABZEICHEN) {
            if ((new File(ew.datei + "sig")).exists()) {
                if (Dialog.yesNoDialog("Warnung", "Eine Bestätigungsdatei für diese Meldung existiert bereits.\nSoll trotzdem eine neue erstellt werden?") != Dialog.YES) {
                    return;
                }
            }
        }

        String errors = "";
        String warnings = "";

        ESigFahrtenhefte f = null;
        Vector nichtGewerteteTeilnehmer = null;
        if (MELDTYP == MeldungenIndexFrame.MELD_FAHRTENABZEICHEN) {
            if (!Main.drvConfig.readOnlyMode) {
                f = new ESigFahrtenhefte(ew.datei + "sig");
                f.verein_user = ew.verein_user;
                f.verein_name = ew.verein_name;
                f.verein_mitglnr = ew.verein_mitglnr;
                f.quittungsnr = this.qnr;

                int itmp = EfaUtil.string2date(Main.drvConfig.schluessel, 0, 0, 0).tag;
                String pubkey_alias = "drv" + (itmp < 10 ? "0" : "") + itmp;
                String certFile = Daten.efaDataDirectory + pubkey_alias + ".cert";
                if (!EfaUtil.canOpenFile(certFile)) {
                    Logger.log(Logger.INFO, String.format("Zertifikatdatei %s nicht gefunden. Lade Datei von efa.rudern.de ...", certFile));
                    String url = Daten.DRV_CERTS_URL + pubkey_alias + ".cert";
                    if (DownloadThread.getFile(this, url , certFile, true)) {
                        Logger.log(Logger.INFO, String.format("Zertifikatdatei %s erfolgreich heruntergeladen!", certFile));
                    } else {
                        Dialog.error(String.format("Zertifikatdatei %s nicht gefunden und Donwload von %s fehlgeschlagen.", certFile, url));
                    }
                }
                if (EfaUtil.canOpenFile(certFile)) {
                    try {
                        int filesize = (int) (new File(certFile)).length();
                        byte[] buf = new byte[filesize];
                        FileInputStream ff = new FileInputStream(certFile);
                        ff.read(buf, 0, filesize);
                        ff.close();
                        String data = Base64.encodeBytes(buf);
                        f.keyName = pubkey_alias;
                        f.keyDataBase64 = EfaUtil.replace(data, "\n", "", true);
                    } catch (Exception ee) {
                        EfaUtil.foo();
                    }
                }
            }

            c = 0;
            teilnErw = 0;
            teilnJug = 0;
            nichtGewerteteTeilnehmer = new Vector();
            for (int i = 0; i < data.size(); i++) {
                EfaWettMeldung m = (EfaWettMeldung) data.get(i);

                // prüfen, ob diese Meldung gewertet werden soll
                if (!m.drvint_wirdGewertet) {
                    nichtGewerteteTeilnehmer.add(m.vorname + " " + m.nachname
                            + (m.drvint_nichtGewertetGrund != null && m.drvint_nichtGewertetGrund.length() > 0 ? " (Grund: " + m.drvint_nichtGewertetGrund + ")" : ""));
                    continue;
                }

                // aktuelle Anzahl der Abzeichen
                int anzAbz = EfaUtil.string2int(m.drv_anzAbzeichen, 0);
                //@AB int anzAbzAB = EfaUtil.string2int(m.drv_anzAbzeichenAB, 0);
                int gesKm = EfaUtil.string2int(m.drv_gesKm, 0);
                //@AB int gesKmAB = EfaUtil.string2int(m.drv_gesKmAB, 0);

                // Gruppe, für die der Teilnehmer erfüllt hat
                String gruppe = getGruppe(ew, m);
                if (gruppe == null) {
                    errors += "Teilnehmer " + (i + 1) + " (" + m.vorname + " " + m.nachname + ") wurde nicht gewertet, da er/sie die Bedingungen nicht erfüllt hat.\n";
                    nichtGewerteteTeilnehmer.add(m.vorname + " " + m.nachname + " (Grund: Wettbewerbsbedingungen nicht erfüllt)");
                    m.drvint_wirdGewertet = false;
                    continue;
                }

                // prüfen, ob Gruppe der Meldung entspricht
                if (!gruppe.equals(m.gruppe)) {
                    warnings += "Teilnehmer " + (i + 1) + " (" + m.vorname + " " + m.nachname + ") wurde für Gruppe '" + m.gruppe + "' gemeldet, hat aber in Gruppe '" + gruppe + "' erfüllt. Gruppe wurde korrigiert.\n";
                }

                // Abzeichen und Kilometer hochzählen
                anzAbz++;
                gesKm += EfaUtil.string2int(m.kilometer, 0);
                //@AB boolean isAB = (gruppe.startsWith("3a") || gruppe.startsWith("3b"));
                //@AB if (isAB) {
                //@AB     anzAbzAB++;
                //@AB     gesKmAB += EfaUtil.string2int(m.kilometer, 0);
                //@AB }

                // Anzahl Abzeichen und Kilometer prüfen
                //@AB if (anzAbz < anzAbzAB || gesKm < gesKmAB) {
                //@AB     errors += "Teilnehmer " + (i + 1) + " (" + m.vorname + " " + m.nachname + ") wurde nicht gewertet, da er/sie ungültige Werte der Abzeichen/Kilometer (mehr AB als normal) hat.\n";
                //@AB     nichtGewerteteTeilnehmer.add(m.vorname + " " + m.nachname + " (Grund: Unstimmige Werte der Abzeichen/Kilometer für Jugend-A/B)");
                //@AB     m.drvint_wirdGewertet = false;
                //@AB     continue;
                //@AB }

                if (!Main.drvConfig.readOnlyMode) {
                    // ggf. neue Teilnehmernummer generieren
                    if (m.drv_teilnNr == null || m.drv_teilnNr.length() == 0) {
                        long l = EfaUtil.getSHAlong((m.vorname + "#" + m.nachname + "#" + m.jahrgang).getBytes(), 3);
                        if (l < 0) {
                            errors += "Für Teilnehmer " + (i + 1) + " (" + m.vorname + " " + m.nachname + ") konnte keine Teilnehmernummer berechnet werden (-1).\n";
                            nichtGewerteteTeilnehmer.add(m.vorname + " " + m.nachname + " (Grund: Es konnte keine Teilnehmernummer berechnet werden)");
                            m.drvint_wirdGewertet = false;
                            continue;
                        }
                        while (Main.drvConfig.teilnehmer.getExact(Long.toString(l)) != null) {
                            l++;
                        }
                        m.drv_teilnNr = Long.toString(l);
                    }
                }

                int jahr = Main.drvConfig.aktJahr;
                byte keynr = (byte) EfaUtil.string2date(Main.drvConfig.schluessel, 0, 0, 0).tag;

                boolean opSuccess = false;
                if (!Main.drvConfig.readOnlyMode) {
                    try {
                        PrivateKey privKey = Daten.keyStore.getPrivateKey(Main.drvConfig.schluessel);
                        if (privKey == null) {
                            errors += "Privater Schlüssel " + Main.drvConfig.schluessel + " nicht gefunden: " + Daten.keyStore.getLastError() + "\n";
                        }
                        DRVSignatur sig = new DRVSignatur(m.drv_teilnNr, 
                                m.vorname, m.nachname, m.jahrgang,
                        //@AB         anzAbz, gesKm, anzAbzAB, gesKmAB, jahr, EfaUtil.string2int(m.kilometer, 0), null,
                                anzAbz, gesKm, 0, 0, jahr, EfaUtil.string2int(m.kilometer, 0), null,        
                                DRVConfig.VERSION, keynr,
                                privKey);
                        sig.checkSignature();
                        if (sig.getSignatureState() != DRVSignatur.SIG_VALID) {
                            errors += "Teilnehmer " + (i + 1) + " (" + m.vorname + " " + m.nachname + ") wurde nicht gewertet, da die für ihr erstellte Signatur ungültig ist: " + sig.getSignatureError() + "\n";
                            nichtGewerteteTeilnehmer.add(m.vorname + " " + m.nachname + " (Grund: Erstellte Signatur ist ungültig)");
                            m.drvint_wirdGewertet = false;
                            continue;
                        } else {
                            // elektronisches Fahrtenheft
                            f.addFahrtenheft(sig, m.personID);
                            Main.drvConfig.teilnehmer.delete(m.drv_teilnNr);
                            DatenFelder d = new DatenFelder(Teilnehmer._ANZFELDER);
                            d.set(Teilnehmer.TEILNNR, m.drv_teilnNr);
                            d.set(Teilnehmer.VORNAME, m.vorname);
                            d.set(Teilnehmer.NACHNAME, m.nachname);
                            d.set(Teilnehmer.JAHRGANG, m.jahrgang);
                            d.set(Teilnehmer.FAHRTENHEFT, sig.toString());
                            Main.drvConfig.teilnehmer.add(d);
                            log(false, "Elektronisches Fahrtenheft erstellt: " + sig.toString());
                            opSuccess = true;
                        }
                    } catch (Exception ee) {
                        errors += "Fehler beim Erstellen des elektronischen Fahrtenhefts für Teilnehmer " + (i + 1) + " (" + m.vorname + " " + m.nachname + "): " + ee.getMessage() + "\n";
                    }
                } else {
                    opSuccess = true;
                }

                if (opSuccess) {
                    c++;
                    if (m.gruppe.startsWith("1") || m.gruppe.startsWith("2")) {
                        teilnErw++;
                    }
                    if (m.gruppe.startsWith("3")) {
                        teilnJug++;
                    }
                }
            }
        }

        try {
            if (c <= 0) {
                errors += "Es wurden keine " + (MELDTYP == MeldungenIndexFrame.MELD_FAHRTENABZEICHEN ? "Teilnehmer" : "Fahrten") + " gewertet!\n";
            } else {
                if (MELDTYP == MeldungenIndexFrame.MELD_FAHRTENABZEICHEN && !Main.drvConfig.readOnlyMode) {
                    f.writeFile();
                }
            }
        } catch (Exception ee) {
            errors += "Fehler beim Erstellen der Bestätigungsdatei: " + e.toString() + "\n";
            c = 0;
        }
        if (MELDTYP == MeldungenIndexFrame.MELD_FAHRTENABZEICHEN && !Main.drvConfig.teilnehmer.writeFile()) {
            errors += "DRV-Teilnehmerdatei konnte nicht geschrieben werden!\n";
        }

        if (errors.length() > 0) {
            Dialog.error("Beim Erstellen der Bestätigungsdatei traten Fehler auf:\n" + errors);
        }
        if (warnings.length() > 0) {
            Dialog.infoDialog("Warnung", "Beim Erstellen der Bestätigungsdatei kam es zu Warnungen:\n" + warnings);
        }

        if (c > 0) {
            if (MELDTYP == MeldungenIndexFrame.MELD_FAHRTENABZEICHEN && !Main.drvConfig.readOnlyMode) {
                Dialog.infoDialog("Es wurden " + c + " von " + data.size() + " Teilnehmern gewertet und ein elektronisches Fahrtenbuch\n"
                        + "für sie generiert.\n"
                        + "Bestätigungsdatei: " + f.getDateiname()
                        + "\n\nEs wird nun ein PDF-Dokument mit den elektronischen Fahrtenheften erzeugt.");
                try {
                    PDFOutput.printPDFbestaetigung(Main.drvConfig, ew, qnr, meldegeld, data.size(), c, teilnErw, teilnJug, f, nichtGewerteteTeilnehmer);
                } catch (NoClassDefFoundError ee) {
                    Dialog.error("Das PDF-Plugin ist nicht installiert.\n"
                            + "Die Meldung kann nicht bestätigt werden.");
                    return;
                }
            }
            if (!saveMeldedatei()) {
                Logger.log(Logger.ERROR, "Speichern der Meldedatei ist fehlgeschlagen!");
                Dialog.error("Speichern der Meldedatei ist fehlgeschlagen!");
                return;
            }
            if (!setMeldedateiBearbeitet((f != null ? f.getDateiname() : "ok"))) {
                Dialog.error("Fehler beim Aktualisieren des Status für die vorliegende Meldedatei");
                return;
            }
            cancel();
        }
    }

    boolean saveMeldedatei() {
        if (data == null) {
            return false;
        }
        for (int i = 0; i < data.size(); i++) {
            EfaWettMeldung ewm = (EfaWettMeldung) data.get(i);
            if (i == 0) {
                this.ew.meldung = ewm;
            }
            if (i + 1 < data.size()) {
                ewm.next = (EfaWettMeldung) data.get(i + 1);
            } else {
                ewm.next = null;
            }
        }
        try {
            if (this.ew.writeFile()) {
                log(false, "Alle Änderungen erfolgreich in der Meldedatei gespeichert.");
                changed = false;
                _hasBeenSaved = true;
                return true;
            } else {
                Logger.log(Logger.ERROR, Logger.MSG_CSVFILE_ERRORWRITEFILE, "Fehler beim Speichern der Meldedatei " + ew.datei + ".");
            }
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_CSVFILE_ERRORWRITEFILE, "Fehler beim Speichern der Meldedatei " + ew.datei + ": " + e);
            Logger.log(e);
            Dialog.error("Fehler beim Speichern der Meldedatei: " + e.getMessage());
        }
        return false;
    }

    boolean setMeldedateiBearbeitet(String bestaetigungsdatei) {
        DatenFelder d = Main.drvConfig.meldungenIndex.getExactComplete(this.qnr);
        if (d == null) {
            return false;
        }
        d.set(MeldungenIndex.STATUS, Integer.toString(MeldungenIndex.ST_BEARBEITET));
        d.set(MeldungenIndex.EDITUUID, UUID.randomUUID().toString());
        if (!MeldungenIndex.MANUELL_ERFASST.equals(d.get(MeldungenIndex.BESTAETIGUNGSDATEI))) {
            d.set(MeldungenIndex.BESTAETIGUNGSDATEI, bestaetigungsdatei);
        }
        if (Main.drvConfig.meldungenIndex.writeFile()) {
            log(false, "Status für Meldung auf 'Bearbeitet' gesetzt.");
            return true;
        }
        return false;
    }

    void closeButton_actionPerformed(ActionEvent e) {
        cancel();
    }

    void nachfrageButton_actionPerformed(ActionEvent e) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; data != null && i < data.size(); i++) {
            EfaWettMeldung m = (EfaWettMeldung) data.get(i);
            if (m != null && m.drvint_nachfrage != null && m.drvint_nachfrage.length() > 0) {
                s.append("Nachfrage zu Fahrt " +
                        (m.drvWS_LfdNr != null && m.drvWS_LfdNr.length() > 0 ? m.drvWS_LfdNr : m.drvWS_StartZiel) +
                        ":\n" +
                        "LfdNr: " + m.drvWS_LfdNr + "\n" +
                        "Bezeichnung: " + m.drvWS_StartZiel + "\n" +
                        "Strecke: " + m.drvWS_Strecke + "\n" +
                        "Gewässer: " + m.drvWS_Gewaesser + "\n" +
                        "Nachfrage: " + m.drvint_nachfrage + "\n\n");
            }
        }
        if (s.length() == 0) {
            Dialog.error("Keine Fahrten für Nachfragen markiert!");
            return;
        }
        StringBuilder h = new StringBuilder();
        h.append("An: " + ew.meld_name + " <" + ew.meld_email + ">\n");
        h.append("Betreff: Rückfrage zur Meldung " + qnr + "\n\n");
        h.append("Zu Ihrer Meldung " + qnr + " (Verein: " + ew.verein_name + ") für die DRV-Wanderruderstatistik " +
                 "haben wir folgende Nachfragen:\n\n");
        ItemTypeTextArea item = new ItemTypeTextArea("TEXT",
                h.toString() + s.toString(),
                IItemType.TYPE_PUBLIC, "", "Text");
        SimpleInputDialog.showInputDialog(this, "Nachfragen", item);
    }

    void vUnblockButton_actionPerformed(ActionEvent e) {
        vBlock(!vBlocked);
    }

    void vBlock(boolean blocked) {
        this.vBestNadelErwGold.setEditable(!blocked);
        this.vBestNadelErwSilber.setEditable(!blocked);
        this.vBestNadelJugGold.setEditable(!blocked);
        this.vBestNadelJugSilber.setEditable(!blocked);
        this.vBestStoffErw.setEditable(!blocked);
        this.vBestStoffJug.setEditable(!blocked);
        this.vMeldenderBank.setEditable(!blocked);
        this.vMeldenderBlz.setEditable(!blocked);
        this.vMeldenderEmail.setEditable(!blocked);
        this.vMeldenderKonto.setEditable(!blocked);
        this.vMeldenderName.setEditable(!blocked);
        this.vMitgliedsnr.setEditable(!blocked);
        this.vNutzername.setEditable(!blocked);
        this.vLandesverband.setEditable(!blocked);
        this.vVereinsname.setEditable(!blocked);
        this.vVersandName.setEditable(!blocked);
        this.vVersandZusatz.setEditable(!blocked);
        this.vVersandOrt.setEditable(!blocked);
        this.vVersantStrasse.setEditable(!blocked);
        this.vAnzahlPapierFahrtenhefte.setEditable(!blocked);
        this.vAktiveMbis18.setEditable(!blocked);
        this.vAktiveMab19.setEditable(!blocked);
        this.vAktiveWbis18.setEditable(!blocked);
        this.vAktiveWab19.setEditable(!blocked);
        this.vVereinskilometer.setEditable(!blocked);
        this.vNotes.setEditable(!blocked);
        if (blocked) {
            this.vUnblockButton.setText("Felder zum Bearbeiten freigeben");
        } else {
            this.vUnblockButton.setText("Änderungen speichern und Felder schützen");
        }
        this.vBlocked = blocked;

        if (blocked) {
            // Änderungen speichern
            if (EfaUtil.string2int(this.vBestNadelErwGold.getText().trim(), 0) != EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_erw_gold, ','))) {
                ew.drv_nadel_erw_gold = this.vBestNadelErwGold.getText().trim();
            }
            ew.drv_nadel_erw_silber = this.vBestNadelErwSilber.getText().trim();
            if (EfaUtil.string2int(this.vBestNadelJugGold.getText().trim(), 0) != EfaUtil.sumUpArray(EfaUtil.kommaList2IntArr(ew.drv_nadel_jug_gold, ','))) {
                ew.drv_nadel_jug_gold = this.vBestNadelJugGold.getText().trim();
            }
            ew.drv_nadel_jug_silber = this.vBestNadelJugSilber.getText().trim();
            ew.drv_stoff_erw = this.vBestStoffErw.getText().trim();
            ew.drv_stoff_jug = this.vBestStoffJug.getText().trim();
            ew.meld_bank = this.vMeldenderBank.getText().trim();
            ew.meld_blz = this.vMeldenderBlz.getText().trim();
            ew.meld_email = this.vMeldenderEmail.getText().trim();
            ew.meld_kto = this.vMeldenderKonto.getText().trim();
            ew.meld_name = this.vMeldenderName.getText().trim();
            ew.verein_mitglnr = this.vMitgliedsnr.getText().trim();
            ew.verein_user = this.vNutzername.getText().trim();
            ew.verein_lrv = this.vLandesverband.getText().trim();
            ew.verein_name = this.vVereinsname.getText().trim();
            ew.versand_name = this.vVersandName.getText().trim();
            ew.versand_zusatz = this.vVersandZusatz.getText().trim();
            ew.versand_ort = this.vVersandOrt.getText().trim();
            ew.versand_strasse = this.vVersantStrasse.getText().trim();
            ew.drvint_anzahlPapierFahrtenhefte = EfaUtil.string2int(this.vAnzahlPapierFahrtenhefte.getText().trim(), -1);
            ew.drvint_notes = vNotes.getText().trim();
            ew.aktive_M_bis18 = Integer.toString(EfaUtil.string2int(this.vAktiveMbis18.getText().trim(), 0));
            ew.aktive_M_ab19 = Integer.toString(EfaUtil.string2int(this.vAktiveMab19.getText().trim(), 0));
            ew.aktive_W_bis18 = Integer.toString(EfaUtil.string2int(this.vAktiveWbis18.getText().trim(), 0));
            ew.aktive_W_ab19 = Integer.toString(EfaUtil.string2int(this.vAktiveWab19.getText().trim(), 0));
            ew.vereins_kilometer = Integer.toString(EfaUtil.string2int(this.vVereinskilometer.getText().trim(), 0));
            ew.drvint_notes = this.vNotes.getText().trim();

            calcOverallValues();
            changed = true;
        }
    }

    void mBlock(boolean blocked) {
        // Fahrtenabzeichen
        this.mTeilnNr.setEditable(!blocked);
        this.mTeilnSuchenButton.setEnabled(!blocked);
        this.mVorname.setEditable(!blocked);
        this.mNachname.setEditable(!blocked);
        this.mJahrgang.setEditable(!blocked);
        this.mGeschlecht.setEnabled(!blocked);
        this.mGruppe.setEnabled(!blocked);
        this.mKilometer.setEditable(!blocked);
        this.mAnzAbzeichen.setEditable(!blocked);
        this.mGesKm.setEditable(!blocked);
        //@AB this.mAnzAbzeichenAB.setEditable(!blocked);
        //@AB this.mGesKmAB.setEditable(!blocked);
        for (int i = 0; this.mFahrten != null && i < this.mFahrten.length; i++) {
            for (int j = 0; this.mFahrten[i] != null && j < this.mFahrten[i].length; j++) {
                this.mFahrten[i][j].setEditable(!blocked);
            }
        }
        this.mFahrtnachweisErbracht.setEnabled(!blocked);

        // Wanderruderstatistik
        this.fLfdNr.setEditable(!blocked);
        this.fStartZiel.setEditable(!blocked);
        this.fStrecke.setEditable(!blocked);
        this.fGewaesser.setEditable(!blocked);
        this.fTage.setEditable(!blocked);
        this.fKilometer.setEditable(!blocked);
        this.fMaennerAnz.setEditable(!blocked);
        this.fMaennerKm.setEditable(!blocked);
        this.fJuniorenAnz.setEditable(!blocked);
        this.fJuniorenKm.setEditable(!blocked);
        this.fFrauenAnz.setEditable(!blocked);
        this.fFrauenKm.setEditable(!blocked);
        this.fJuniorinnenAnz.setEditable(!blocked);
        this.fJuniorinnenKm.setEditable(!blocked);
        this.fTeilnehmer.setEditable(!blocked);
        this.fMannschKm.setEditable(!blocked);
        this.fBemerkungen.setEditable(!blocked);

        if (blocked) {
            this.mUnblockButton.setText("Felder zum Bearbeiten freigeben");
            this.fUnblockButton.setText("Felder zum Bearbeiten freigeben");
        } else {
            this.mUnblockButton.setText("Änderungen speichern und Felder schützen");
            this.fUnblockButton.setText("Änderungen speichern und Felder schützen");
        }
        this.mBlocked = blocked;
    }

    void firstButton_actionPerformed(ActionEvent e) {
        setMFields(0, false);
    }

    void prevButton_actionPerformed(ActionEvent e) {
        setMFields(ewmNr - 1, false);
    }

    void nextButton_actionPerformed(ActionEvent e) {
        setMFields(ewmNr + 1, false);
    }

    void lastButton_actionPerformed(ActionEvent e) {
        setMFields(data.size() - 1, false);
    }

    void newButton_actionPerformed(ActionEvent e) {
        EfaWettMeldung m = new EfaWettMeldung();
        data.add(m);
        setMFields(data.size() - 1, false);
        mBlock(false);
    }

    void deleteButton_actionPerformed(ActionEvent e) {
        if (ewmCur == null || ewmNr < 0) {
            Dialog.error("Es ist kein Eintrag ausgewählt.");
            return;
        }
        if (Dialog.yesNoDialog("Eintrag Löschen", "Möchtest du Eintrag " + (ewmNr+1) + " wirklich löschen?") == Dialog.YES) {
            data.remove(ewmNr);
            changed = true;
            if (ewmNr >= data.size()) {
                ewmNr--;
            }
            if (ewmNr < 0) {
                newButton_actionPerformed(null);
            }
            setMFields(ewmNr, false);
        }
    }

    void mUnblockButton_actionPerformed(ActionEvent e) {
        if (ewmCur == null) {
            Dialog.error("Es ist kein Eintrag ausgewählt. Bitte gehe zu einem vorhandenen Eintrag oder klicke 'Neu'.");
            return;
        }
        if (!mBlocked) {
            getMFields(ewmCur);
            replaceMeldung();
            mBlock(true);
            setMFields(ewmNr, true);
        } else {
            mBlock(!mBlocked);
        }
    }

    void fUnblockButton_actionPerformed(ActionEvent e) {
        if (ewmCur == null) {
            Dialog.error("Es ist kein Eintrag ausgewählt. Bitte gehe zu einem vorhandenen Eintrag oder klicke 'Neu'.");
            return;
        }
        if (!mBlocked) {
            getMFields(ewmCur);
            replaceMeldung();
            mBlock(true);
            setMFields(ewmNr, true);
        } else {
            mBlock(!mBlocked);
        }
    }
    
    void addGewaesser_actionPerformed(ActionEvent e) {
        if (!fGewaesser.isEditable()) {
            return;
        }
        ItemTypeStringAutoComplete item = new ItemTypeStringAutoComplete("WATERS", "",
                IItemType.TYPE_PUBLIC, "", "Gewässer", false, waterList);
        item.setAlwaysReturnPlainText(true);
        if (SimpleInputDialog.showInputDialog(this, "Gewässer", item)) {
            String w = item.getValueFromField();
            if (w != null) {
                w = w.trim();
                if (w.length() > 0) {
                    String s = fGewaesser.getText().trim();
                    s = s + (s.length() > 0 ? "," : "") + w;
                    fGewaesser.setText(s);
                }
            }
        }
    }

    boolean loadKeys() {
        if (Daten.keyStore != null && Daten.keyStore.isKeyStoreReady()) {
            return true;
        }
        Daten.keyStore = new EfaKeyStore(Daten.efaDataDirectory + DRVConfig.KEYSTORE_FILE, Main.drvConfig.keyPassword);
        if (!Daten.keyStore.isKeyStoreReady()) {
            Dialog.error("KeyStore kann nicht geladen werden:\n" + Daten.keyStore.getLastError());
        }
        return Daten.keyStore.isKeyStoreReady();
    }

    void mWirdGewertet_actionPerformed(ActionEvent e) { //@@@!!!
        if (ewmCur != null) {
            ewmCur.drvint_wirdGewertet = mWirdGewertet.isSelected();
            ewmCur.drvint_wirdGewertetExplizitGesetzt = true;
        }
        if (data != null && ewmNr >= 0 && ewmNr < data.size()) {
            EfaWettMeldung m = (EfaWettMeldung) data.get(ewmNr);
            if (m != null) {
                m.drvint_wirdGewertet = mWirdGewertet.isSelected();
                m.drvint_wirdGewertetExplizitGesetzt = true;
            }
        }
        updateStatusNichtGewertetGrund();
        changed = true;
    }

    void fWirdGewertet_actionPerformed(ActionEvent e) {
        if (ewmCur != null) {
            ewmCur.drvint_wirdGewertet = fWirdGewertet.isSelected();
            ewmCur.drvint_wirdGewertetExplizitGesetzt = true;
        }
        if (data != null && ewmNr >= 0 && ewmNr < data.size()) {
            EfaWettMeldung m = (EfaWettMeldung) data.get(ewmNr);
            if (m != null) {
                m.drvint_wirdGewertet = fWirdGewertet.isSelected();
                m.drvint_wirdGewertetExplizitGesetzt = true;
            }
        }
        updateStatusNichtGewertetGrund();
        changed = true;
    }

    void fNachfrage_actionPerformed(ActionEvent e) {
        updateStatusNachfrage();
        if (fNachfrageCheckBox.isSelected()) {
            fNachfrage.requestFocus();
        }
        changed = true;
    }

    void mNichtGewertetGrund_focusLost(FocusEvent e) {
        String s = mNichtGewertetGrund.getText().trim();
        if (s.length() == 0) {
            s = null;
        }
        if (ewmCur != null) {
            ewmCur.drvint_nichtGewertetGrund = s;
        }
        if (data != null && ewmNr >= 0 && ewmNr < data.size()) {
            EfaWettMeldung m = (EfaWettMeldung) data.get(ewmNr);
            if (m != null) {
                m.drvint_nichtGewertetGrund = s;
            }
        }
    }

    void fNichtGewertetGrund_focusLost(FocusEvent e) {
        String s = fNichtGewertetGrund.getText().trim();
        if (s.length() == 0) {
            s = null;
        }
        if (ewmCur != null) {
            ewmCur.drvint_nichtGewertetGrund = s;
        }
        if (data != null && ewmNr >= 0 && ewmNr < data.size()) {
            EfaWettMeldung m = (EfaWettMeldung) data.get(ewmNr);
            if (m != null) {
                m.drvint_nichtGewertetGrund = s;
            }
        }
    }

    void fNachfrage_focusLost(FocusEvent e) {
        String s = fNachfrage.getText().trim();
        if (s.length() == 0) {
            s = null;
        }
        if (ewmCur != null) {
            ewmCur.drvint_nachfrage = s;
        }
        if (data != null && ewmNr >= 0 && ewmNr < data.size()) {
            EfaWettMeldung m = (EfaWettMeldung) data.get(ewmNr);
            if (m != null) {
                m.drvint_nachfrage = s;
            }
        }
    }

    void updateStatusNichtGewertetGrund() {
        switch (MELDTYP) {
            case MeldungenIndexFrame.MELD_FAHRTENABZEICHEN:
                mNichtGewertetGrund.setEnabled(!mWirdGewertet.isSelected());
                mNichtGewertetGrund.setEditable(!mWirdGewertet.isSelected());
                this.mNichtGewertetGrundLabel.setEnabled(!mWirdGewertet.isSelected());
                break;
            case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                fNichtGewertetGrund.setEnabled(!fWirdGewertet.isSelected());
                fNichtGewertetGrund.setEditable(!fWirdGewertet.isSelected());
                this.fNichtGewertetGrundLabel.setEnabled(!fWirdGewertet.isSelected());
                break;
        }
    }

    void updateStatusNachfrage() {
        switch (MELDTYP) {
            case MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK:
                fNachfrage.setEditable(fNachfrageCheckBox.isSelected());
                if (!fNachfrageCheckBox.isSelected()) {
                    fNachfrage.setText("");
                }
                break;
        }
    }

    void mTeilnSuchenButton_actionPerformed(ActionEvent e) {
        if (Main.drvConfig.teilnehmer == null) {
            Dialog.error("Es wurde keine Teilnehmer-Datei geladen!");
            return;
        }

        String vorname = mVorname.getText().trim();
        String nachname = mNachname.getText().trim();
        String jahrgang = mJahrgang.getText().trim();
        if (vorname.length() == 0 || nachname.length() == 0 || jahrgang.length() == 0) {
            Dialog.error("Bitte fülle die Felder 'Vorname', 'Nachname' und 'Jahrgang' aus!");
            return;
        }

        Vector teilnnr = new Vector();
        for (DatenFelder d = Main.drvConfig.teilnehmer.getCompleteFirst(); d != null; d = Main.drvConfig.teilnehmer.getCompleteNext()) {
            if (vorname.equals(d.get(Teilnehmer.VORNAME))
                    && nachname.equals(d.get(Teilnehmer.NACHNAME))
                    && jahrgang.equals(d.get(Teilnehmer.JAHRGANG))) {
                teilnnr.add(d.get(Teilnehmer.TEILNNR));
            }
        }

        if (teilnnr.size() == 0) {
            Dialog.error("Es konnte kein entsprechender Teilnehmer gefunden werden!");
            return;
        }
        if (teilnnr.size() == 1) {
            mTeilnNr.setText((String) teilnnr.get(0));
            return;
        }
        String s = "";
        for (int i = 0; i < teilnnr.size(); i++) {
            s += (i > 0 ? ", " : "") + (String) teilnnr.get(i);
        }
        Dialog.error("Es wurden mehrere Teilnehmer '" + vorname + " " + nachname + " (" + jahrgang + ")' gefunden.\nTeilnehmernummern: " + s);
    }

    void printStoffBestellButton_actionPerformed(ActionEvent e) {
        String tmpdatei = Daten.efaTmpDirectory + "stoffabzeichen.html";
        try {
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpdatei), Daten.ENCODING_UTF));
            f.write("<html>\n");
            f.write("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=" + Daten.ENCODING_UTF + "\"></head>\n");
            f.write("<body>\n");
            f.write("<h1 align=\"center\">Bestellung von Stoffabzeichen</h1>\n");
            f.write("<h2 align=\"center\">" + ew.verein_name + "</h2>\n");
            f.write("<table align=\"center\" border=\"3\" width=\"100%\">\n");
            f.write("<tr><td colspan=\"2\" align=\"center\"><big>Vereinsdaten</big></tt></td></tr>\n");
            f.write("<tr><td>Vereinsname:</td><td><tt><b>" + ew.verein_name + "</b></tt></td></tr>\n");
            f.write("<tr><td>DRV-Mitgliedsnummer:</td><td><tt><b>" + ew.verein_mitglnr + "</b></tt></td></tr>\n");
            f.write("<tr><td colspan=\"2\" align=\"center\"><big>Meldende Person</big></tt></td></tr>\n");
            f.write("<tr><td>Name:</td><td><tt><b>" + ew.meld_name + "</b></tt></td></tr>\n");
            f.write("<tr><td>Kontonr:</td><td><tt><b>" + ew.meld_kto + "</b></tt></td></tr>\n");
            f.write("<tr><td>Bank:</td><td><tt><b>" + ew.meld_bank + "</b></tt></td></tr>\n");
            f.write("<tr><td>BLZ:</td><td><tt><b>" + ew.meld_blz + "</b></tt></td></tr>\n");
            f.write("<tr><td colspan=\"2\" align=\"center\"><big>Versandanschrift</big></tt></td></tr>\n");
            f.write("<tr><td>Name:</td><td><tt><b>" + ew.versand_name + "</b></tt></td></tr>\n");
            if (ew.versand_zusatz != null && ew.versand_zusatz.trim().length() > 0) {
                f.write("<tr><td>Adresszusatz:</td><td><tt><b>" + ew.versand_zusatz + "</b></tt></td></tr>\n");
            }
            f.write("<tr><td>Straße:</td><td><tt><b>" + ew.versand_strasse + "</b></tt></td></tr>\n");
            f.write("<tr><td>Ort:</td><td><tt><b>" + ew.versand_ort + "</b></tt></td></tr>\n");
            f.write("<tr><td colspan=\"2\" align=\"center\"><big>Bestellung</big></tt></td></tr>\n");
            f.write("<tr><td>Stoffabzeichen Jugend:</td><td><tt><b>" + ew.drv_stoff_jug + "</b></tt></td></tr>\n");
            f.write("<tr><td>Stoffabzeichen Erwachsene gold:</td><td><tt><b>" + ew.drv_stoff_erw + "</b></tt></td></tr>\n");
            int cent = EfaUtil.string2int(ew.drv_stoff_erw, 0) * Main.drvConfig.eur_stoff_erw
                    + EfaUtil.string2int(ew.drv_stoff_jug, 0) * Main.drvConfig.eur_stoff_jug;
            f.write("<tr><td>Bestellwert:</td><td><tt><b>" + EfaUtil.cent2euro(cent, true) + "</b></tt></td></tr>\n");
            f.write("</table>\n");
            f.write("</body></html>\n");
            f.close();
            JEditorPane out = new JEditorPane();
            out.setContentType("text/html; charset=" + Daten.ENCODING_UTF);
            out.setPage(EfaUtil.correctUrl("file:" + tmpdatei));
            out.setSize(600, 800);
            out.doLayout();
            SimpleFilePrinter sfp = new SimpleFilePrinter(out);
            if (sfp.setupPageFormat()) {
                if (sfp.setupJobOptions()) {
                    sfp.printFile();
                }
            }
            EfaUtil.deleteFile(tmpdatei);
        } catch (Exception ee) {
            Dialog.error("Druckdatei konnte nicht erstellt werden: " + ee.toString());
            return;
        }

    }

    void jTabbedPane1_stateChanged(ChangeEvent e) {
        if (jTabbedPane1 != null && jTabbedPane1.getSelectedIndex() == 0) {
            this.calcOverallValues();
        }
    }

    void vMeldegeldEingegangen_actionPerformed(ActionEvent e) {
        if (vMeldegeldEingegangen.isSelected()) {
            vMeldegeldEingegangen.setForeground(Color.blue);
        } else {
            vMeldegeldEingegangen.setForeground(Color.red);
        }
        ew.drvint_meldegeldEingegangen = this.vMeldegeldEingegangen.isSelected();
        changed = true;
    }

    void makeSureIsANumber_focusLost(FocusEvent e) {
        if (e == null) {
            return;
        }
        try {
            JTextField f = (JTextField) e.getComponent();
            f.setText(Integer.toString(EfaUtil.string2date(f.getText().trim(), 0, 0, 0).tag));
        } catch (Exception ee) {
            EfaUtil.foo();
        }
    }

    void makeSureIsANumberWithComma_focusLost(FocusEvent e) {
        if (e == null) {
            return;
        }
        try {
            JTextField f = (JTextField) e.getComponent();
            f.setText(EfaUtil.zehntelInt2String(EfaUtil.zehntelString2Int(f.getText().trim())));
        } catch (Exception ee) {
            EfaUtil.foo();
        }
    }

    void wsListButton_actionPerformed(ActionEvent e) {
        if (MELDTYP != MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK) {
            return;
        }
        WSFahrtenUebersichtFrame dlg = new WSFahrtenUebersichtFrame(this, data);
        Dialog.setDlgLocation(dlg, this);
        dlg.setModal(true);
        dlg.show();
        if (dlg.getResult() >= 0) {
            setMFields(dlg.getResult(), false);
        }
    }

    public static boolean hasBeenSaved() {
        return _hasBeenSaved;
    }

    void aequatorButton_actionPerformed(ActionEvent e) {
        Vector aequator = new Vector();
        int warnings = 0;

        for (int i = 0; i < data.size(); i++) {
            EfaWettMeldung ewm = (EfaWettMeldung) data.get(i);
            if (ewm.drv_aequatorpreis != null && ewm.drv_aequatorpreis.length() > 0 &&
                EfaUtil.stringFindInt(ewm.drv_aequatorpreis, 0) >= 1) {
                if (isErfuellt(ewm, false) && ewm.drvint_wirdGewertet) {
                    int km = EfaUtil.string2int(ewm.drv_gesKm, 0) + EfaUtil.string2int(ewm.kilometer, 0);
                    int anzAeq = km / WettDefs.DRV_AEQUATOR_KM;
                    aequator.add("<p><b>" + ewm.vorname + " " + ewm.nachname + "</b><br>Jahrgang: " + ewm.jahrgang + "<br>Kilometer: " + km + "<br>" + anzAeq + ". Äquatorpreis</p>");
                } else {
                    warnings++;
                }
            }
        }
        if (warnings > 0) {
            Dialog.error("Es gibt " + warnings + " Teilnehmer, die den Äquatorpreis bekommen würden, aber die Bedingungen für dieses\n"
                    + "Jahr nicht erfüllt haben oder nicht gewertet werden sollen. Diese Teilnehmer werden NICHT ausgegeben.\n\n"
                    + "Bitte prüfen Sie erst alle Meldungen dieses Vereins auf Gültigkeit und Drucken Sie anschließend erst\n"
                    + "die Äquatorpreisträger aus.");
        }
        if (aequator.size() == 0) {
            Dialog.error("Es gibt keine Äquatorpreisträger in diesem Verein.");
            return;
        }

        String tmpdatei = Daten.efaTmpDirectory + "aequator.html";
        try {
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpdatei), Daten.ENCODING_UTF));
            f.write("<html>\n");
            f.write("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=" + Daten.ENCODING_UTF + "\"></head>\n");
            f.write("<body>\n");
            f.write("<h1 align=\"center\">Äquatorpreisträger</h1>\n");
            f.write("<p><b>Verein: " + ew.verein_name + "</b><br>Mitgliedsnummer: " + ew.verein_mitglnr +
                    "<br><br>Anschrift:<br>" + ew.versand_name + "<br>" + (ew.versand_zusatz != null && ew.versand_zusatz.length() > 0 ? ew.versand_zusatz + "<br>" : "") + ew.versand_strasse + "<br>" + ew.versand_ort + "</p>\n");
            for (int i = 0; i < aequator.size(); i++) {
                f.write(((String) aequator.get(i)) + "\n");
            }
            f.write("</body></html>\n");
            f.close();
            JEditorPane out = new JEditorPane();
            out.setContentType("text/html; charset=" + Daten.ENCODING_UTF);
            out.setPage(EfaUtil.correctUrl("file:" + tmpdatei));
            SimpleFilePrinter.sizeJEditorPane(out);
            SimpleFilePrinter sfp = new SimpleFilePrinter(out);
            if (sfp.setupPageFormat()) {
                if (sfp.setupJobOptions()) {
                    sfp.printFile();
                }
            }
            EfaUtil.deleteFile(tmpdatei);
        } catch (Exception ee) {
            Dialog.error("Druckdatei konnte nicht erstellt werden: " + ee.toString());
            return;
        }


    }

    void mFahrtnachweisErbracht_actionPerformed(ActionEvent e) {
    }
}
