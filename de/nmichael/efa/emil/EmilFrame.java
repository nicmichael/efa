/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.emil;

import de.nmichael.efa.data.efawett.ZielfahrtFolge;
import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.data.efawett.WettDef;
import de.nmichael.efa.data.efawett.EfaWettMeldung;
import de.nmichael.efa.data.efawett.EfaWett;
import de.nmichael.efa.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.GregorianCalendar;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class EmilFrame extends JFrame {
  final static String PROGRAMMNAME = "emil - elektronischer Meldedatei Editor";
  final static String KURZTITEL = "emil";
  final static int FIELD_HEIGHT=24;
  EmilConfig cfg;

  WettDefs wettDefs = null;
  EfaWett efw = null;
  EfaWettMeldung currentTeilnehmer = null;
  int currentTeilnehmerNummer = 0; // Position des Teilnehmers in der verketteten Liste, 0 == efw.meldung; 1 == efw.meldung.next usw.
  boolean geaendertEintrag = false;
  boolean geaendertDatei = false;
  boolean anStdCsvAngehaengt = false; // true, wenn aktuelle Daten bereits an Standard-CSV-Datei angehängt wurden

  JPanel contentPane;
  JMenuBar jMenuBar1 = new JMenuBar();
  JMenu jMenuFile = new JMenu();
  JMenuItem menuItemBeenden = new JMenuItem();
  JMenu jMenuHelp = new JMenu();
  JMenuItem menuItemUeber = new JMenuItem();
  JPanel wettPanel = new JPanel();
  JPanel vereinPanel = new JPanel();
  JLabel jLabel1 = new JLabel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JComboBox wett = new JComboBox();
  JLabel jLabel2 = new JLabel();
  JTextField wettJahr = new JTextField();
  JLabel jLabel3 = new JLabel();
  GridBagLayout gridBagLayout2 = new GridBagLayout();
  JTextField vereinsname = new JTextField();
  JLabel jLabel4 = new JLabel();
  JTextField benutzername = new JTextField();
  JLabel jLabel5 = new JLabel();
  JTextField mitglieder = new JTextField();
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel jPanel1 = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  JPanel jPanel2 = new JPanel();
  JPanel meldPanel = new JPanel();
  BorderLayout borderLayout3 = new BorderLayout();
  GridBagLayout gridBagLayout3 = new GridBagLayout();
  JLabel jLabel6 = new JLabel();
  JTextField meldName = new JTextField();
  JLabel jLabel7 = new JLabel();
  JTextField meldEmail = new JTextField();
  JPanel teilnehmerPanel = new JPanel();
  BorderLayout borderLayout4 = new BorderLayout();
  JPanel jPanel4 = new JPanel();
  JButton firstButton = new JButton();
  JButton prevButton = new JButton();
  JButton nextButton = new JButton();
  JButton lastButton = new JButton();
  JButton newButton = new JButton();
  TitledBorder titledBorder1;
  Border border1;
  JScrollPane teilnehmerScrollPane = new JScrollPane();
  JPanel teilnehmerDatenPanel = null;
  GridBagLayout gridBagLayout4 = new GridBagLayout();
  JPanel jPanel3 = new JPanel();
  BorderLayout borderLayout5 = new BorderLayout();
  JPanel versandPanel = new JPanel();
  JLabel lblVersandName = new JLabel();
  JLabel lblVersandAdresszusatz = new JLabel();
  GridBagLayout gridBagLayout5 = new GridBagLayout();
  JPanel jPanel5 = new JPanel();
  BorderLayout borderLayout6 = new BorderLayout();
  JPanel jPanel6 = new JPanel();
  JLabel lblAnzahlTeilnehmerErfuelltGesamt = new JLabel();
  JPanel jPanel7 = new JPanel();
  JButton saveButton = new JButton();
  JTextField versandName = new JTextField();
  JTextField versandZusatz = new JTextField();
  JLabel lblVersandStrasse = new JLabel();
  JTextField versandStrasse = new JTextField();
  JLabel lblVersandPlzOrt = new JLabel();
  JTextField versandOrt = new JTextField();
  JLabel teilnehmerAnz = new JLabel();
  JLabel jLabel13 = new JLabel();
  JLabel meldegeld = new JLabel();
  GridBagLayout gridBagLayout6 = new GridBagLayout();
  JButton deleteButton = new JButton();
  JLabel jLabel12 = new JLabel();
  JLabel jLabel14 = new JLabel();
  JLabel jLabel15 = new JLabel();
  JLabel jLabel16 = new JLabel();
  JTextField nachname = new JTextField();
  JTextField vorname = new JTextField();
  JTextField jahrgang = new JTextField();
  JComboBox geschlecht = new JComboBox();
  JLabel jLabel17 = new JLabel();
  JTextField kilometer = new JTextField();

  JLabel[] fahrtLabel = null;
  JTextField[] fahrtDatum = null;
  JTextField[] fahrtZiel =null;
  JTextField[] fahrtKm = null;
  JTextField[] fahrtZf = null;
  JMenuItem menuItemNeu = new JMenuItem();
  JMenuItem menuItemOeffnen = new JMenuItem();
  JMenuItem menuItemSpeichern = new JMenuItem();
  JMenuItem menuItemSpeichernUnter = new JMenuItem();
  JMenuItem menuItemExport = new JMenuItem();
  JLabel jLabel18 = new JLabel();
  JLabel wimpelKm = new JLabel();
  JLabel jLabel20 = new JLabel();
  JLabel wimpelSchnitt = new JLabel();
  JLabel erfuellt = new JLabel();
  JLabel jLabel19 = new JLabel();
  JTextField adresse = new JTextField();
  JLabel jLabel21 = new JLabel();
  JLabel gruppe = new JLabel();
  JMenuItem menuitemAddToCVS = new JMenuItem();
  JMenu jMenu1 = new JMenu();
  JMenuItem menuitemEinstellungen = new JMenuItem();
  JLabel posLabel = new JLabel();
  JLabel jLabel22 = new JLabel();
  JLabel jLabel23 = new JLabel();
  JLabel jLabel24 = new JLabel();
  JLabel jLabel25 = new JLabel();
  JMenuItem menuItemHelp = new JMenuItem();

  /**Construct the frame*/
  public EmilFrame() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    appini();
    iniItems();
    iniFahrten(EfaWettMeldung.FAHRT_ANZ_X);
    menuItemNeu_actionPerformed(null);
    wett.requestFocus();
  }

  void appini() {
    cfg = new EmilConfig(Daten.efaCfgDirectory+"emil.cfg");
    cfg.readFile();

    // WettDefs.cfg
    wettDefs = new WettDefs(Daten.efaCfgDirectory+Daten.WETTDEFS);
    Daten.iniDataFile(wettDefs, true, International.onlyFor("Wettbewerbskonfiguration", "de"));
  }


  // ActionHandler Events
  public void keyAction(ActionEvent evt) {
    if (evt == null || evt.getActionCommand() == null) return;
//    if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
      // nothing
//    }
  }


  /**Component initialization*/
  private void jbInit() throws Exception  {
    ActionHandler ah= new ActionHandler(this);
    try {
      ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                       new String[] {"ESCAPE","F1"}, new String[] {"keyAction","keyAction"});
    } catch(NoSuchMethodException e) {
      System.err.println("Error setting up ActionHandler");
    }

    contentPane = (JPanel) this.getContentPane();
    titledBorder1 = new TitledBorder("");
    border1 = BorderFactory.createLineBorder(Color.black,2);
    contentPane.setLayout(borderLayout1);
    this.setSize(new Dimension(850, 550));
    this.setTitle("emil - elektronischer Meldedatei Editor");
    jMenuFile.setMnemonic('D');
    jMenuFile.setText("Datei");
    menuItemBeenden.setMnemonic('B');
    menuItemBeenden.setText("Beenden");
    menuItemBeenden.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent e) {
        menuItemBeenden_actionPerformed(e);
      }
    });
    jMenuHelp.setMnemonic('I');
    jMenuHelp.setText("Info");
    menuItemUeber.setMnemonic('B');
    menuItemUeber.setText("Über");
    menuItemUeber.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent e) {
        menuItemUeber_actionPerformed(e);
      }
    });
    jLabel1.setText("Wettbewerb: ");
    wettPanel.setLayout(gridBagLayout1);
    jLabel2.setText("Jahr: ");
    wettJahr.setNextFocusableComponent(vereinsname);
    wettJahr.setPreferredSize(new Dimension(100, FIELD_HEIGHT));
    wettJahr.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        dateiGeaendert(e);
      }
    });
    wettJahr.addFocusListener(new java.awt.event.FocusAdapter() {
      public void focusLost(FocusEvent e) {
        wettJahr_focusLost(e);
      }
    });
    wettPanel.setBorder(BorderFactory.createEtchedBorder());
    wettPanel.setPreferredSize(new Dimension(364, 35));
    jLabel3.setText("Vereinsname: ");
    vereinPanel.setBorder(BorderFactory.createEtchedBorder());
    vereinPanel.setPreferredSize(new Dimension(414, 60));
    vereinPanel.setLayout(gridBagLayout2);
    jLabel4.setText("Benutzername: ");
    jLabel5.setText("Mitgliederzahl: ");
    mitglieder.setNextFocusableComponent(meldName);
    mitglieder.setPreferredSize(new Dimension(50, FIELD_HEIGHT));
    mitglieder.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        dateiGeaendert(e);
      }
    });
    mitglieder.addFocusListener(new java.awt.event.FocusAdapter() {
      public void focusLost(FocusEvent e) {
        mitglieder_focusLost(e);
      }
    });
    benutzername.setNextFocusableComponent(mitglieder);
    benutzername.setPreferredSize(new Dimension(150, FIELD_HEIGHT));
    benutzername.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        dateiGeaendert(e);
      }
    });
    jPanel1.setLayout(borderLayout2);
    jPanel2.setLayout(borderLayout3);
    meldPanel.setLayout(gridBagLayout3);
    jLabel6.setText("Meldender: Name: ");
    jLabel7.setText("email: ");
    meldName.setNextFocusableComponent(meldEmail);
    meldName.setPreferredSize(new Dimension(225, FIELD_HEIGHT));
    meldName.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        dateiGeaendert(e);
      }
    });
    meldPanel.setBorder(BorderFactory.createEtchedBorder());
    meldPanel.setMinimumSize(new Dimension(289, 60));
    meldPanel.setPreferredSize(new Dimension(575, 35));
    teilnehmerPanel.setLayout(borderLayout4);
    firstButton.setNextFocusableComponent(prevButton);
    firstButton.setMnemonic('E');
    firstButton.setText("Erster");
    firstButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        firstButton_actionPerformed(e);
      }
    });
    prevButton.setNextFocusableComponent(nextButton);
    prevButton.setMnemonic('V');
    prevButton.setText("<< Vorheriger");
    prevButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        prevButton_actionPerformed(e);
      }
    });
    nextButton.setNextFocusableComponent(lastButton);
    nextButton.setMnemonic('N');
    nextButton.setText("Nächster >>");
    nextButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        nextButton_actionPerformed(e);
      }
    });
    lastButton.setNextFocusableComponent(newButton);
    lastButton.setMnemonic('L');
    lastButton.setText("Letzter");
    lastButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        lastButton_actionPerformed(e);
      }
    });
    newButton.setNextFocusableComponent(deleteButton);
    newButton.setMnemonic('U');
    newButton.setText("Neu");
    newButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        newButton_actionPerformed(e);
      }
    });
    teilnehmerPanel.setBorder(border1);
    jPanel4.setBorder(BorderFactory.createEtchedBorder());
    meldEmail.setNextFocusableComponent(versandName);
    meldEmail.setPreferredSize(new Dimension(200, FIELD_HEIGHT));
    meldEmail.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        dateiGeaendert(e);
      }
    });
    jPanel3.setLayout(borderLayout5);
    versandPanel.setBorder(BorderFactory.createEtchedBorder());
    versandPanel.setPreferredSize(new Dimension(625, 35));
    versandPanel.setLayout(gridBagLayout5);
    lblVersandName.setText("Versand: Name: ");
    lblVersandAdresszusatz.setText("Adresszusatz: ");
    jPanel5.setLayout(borderLayout6);
    lblAnzahlTeilnehmerErfuelltGesamt.setText("Anzahl Teilnehmer (erfüllt/gesamt): ");
    saveButton.setNextFocusableComponent(firstButton);
    saveButton.setPreferredSize(new Dimension(400, 25));
    saveButton.setMnemonic('S');
    saveButton.setText("Änderungen am Eintrag speichern");
    saveButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveButton_actionPerformed(e);
      }
    });
    lblVersandStrasse.setText("Straße: ");
    lblVersandPlzOrt.setText("PLZ, Ort: ");
    versandName.setNextFocusableComponent(versandZusatz);
    versandName.setPreferredSize(new Dimension(200, FIELD_HEIGHT));
    versandName.setMinimumSize(new Dimension(120, FIELD_HEIGHT));
    versandName.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        dateiGeaendert(e);
      }
    });
    versandZusatz.setNextFocusableComponent(versandStrasse);
    versandZusatz.setPreferredSize(new Dimension(200, FIELD_HEIGHT));
    versandZusatz.setMinimumSize(new Dimension(120, FIELD_HEIGHT));
    versandZusatz.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        dateiGeaendert(e);
      }
    });
    versandStrasse.setNextFocusableComponent(versandOrt);
    versandStrasse.setPreferredSize(new Dimension(220, FIELD_HEIGHT));
    versandStrasse.setMinimumSize(new Dimension(120, FIELD_HEIGHT));
    versandStrasse.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        dateiGeaendert(e);
      }
    });
    versandOrt.setNextFocusableComponent(wett);
    versandOrt.setPreferredSize(new Dimension(130, FIELD_HEIGHT));
    versandOrt.setMinimumSize(new Dimension(120, FIELD_HEIGHT));
    versandOrt.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        dateiGeaendert(e);
      }
    });
    teilnehmerAnz.setForeground(Color.black);
    teilnehmerAnz.setText("0/0");
    jLabel13.setText("Meldegeld: ");
    meldegeld.setForeground(Color.black);
    meldegeld.setText("0,- EUR");
    jLabel13.setVisible(false); meldegeld.setVisible(false); // @todo (P9) Emil Meldegeld (deaktiviert)
    jPanel6.setBorder(BorderFactory.createEtchedBorder());
    jPanel6.setPreferredSize(new Dimension(306, 60));
    jPanel6.setLayout(gridBagLayout6);
    deleteButton.setNextFocusableComponent(nachname);
    deleteButton.setMnemonic('C');
    deleteButton.setText("Löschen");
    deleteButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        deleteButton_actionPerformed(e);
      }
    });
    jLabel12.setText("Nachname: ");
    jLabel14.setText("Vorname: ");
    jLabel15.setText("Jahrgang: ");
    jLabel16.setText("Geschlecht: ");
    jLabel17.setText("Kilometer: ");
    nachname.setNextFocusableComponent(vorname);
    nachname.setPreferredSize(new Dimension(150, FIELD_HEIGHT));
    nachname.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        eintragGeaendert(e);
      }
      public void keyReleased(KeyEvent e) {
        validateErfuellt(e);
      }
    });
    vorname.setNextFocusableComponent(jahrgang);
    vorname.setPreferredSize(new Dimension(150, FIELD_HEIGHT));
    vorname.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        eintragGeaendert(e);
      }
      public void keyReleased(KeyEvent e) {
        validateErfuellt(e);
      }
    });
    jahrgang.setNextFocusableComponent(geschlecht);
    jahrgang.setPreferredSize(new Dimension(150, FIELD_HEIGHT));
    jahrgang.addFocusListener(new java.awt.event.FocusAdapter() {
      public void focusLost(FocusEvent e) {
        jahrgang_focusLost(e);
      }
    });
    jahrgang.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        eintragGeaendert(e);
      }
      public void keyReleased(KeyEvent e) {
        validateErfuellt(e);
      }
    });
    geschlecht.setNextFocusableComponent(kilometer);
    geschlecht.setPreferredSize(new Dimension(150, FIELD_HEIGHT));
    geschlecht.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        geschlecht_itemStateChanged(e);
      }
    });
    kilometer.setNextFocusableComponent(adresse);
    kilometer.setPreferredSize(new Dimension(150, FIELD_HEIGHT));
    kilometer.addFocusListener(new java.awt.event.FocusAdapter() {
      public void focusLost(FocusEvent e) {
        kilometer_focusLost(e);
      }
    });
    kilometer.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        eintragGeaendert(e);
      }
      public void keyReleased(KeyEvent e) {
        validateErfuellt(e);
      }
    });
    contentPane.setMinimumSize(new Dimension(830, 600));
    contentPane.setPreferredSize(new Dimension(830, 600));
    menuItemNeu.setMnemonic('N');
    menuItemNeu.setText("Neue Meldedatei");
    menuItemNeu.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        menuItemNeu_actionPerformed(e);
      }
    });
    menuItemOeffnen.setMnemonic('F');
    menuItemOeffnen.setText("Meldedatei öffnen");
    menuItemOeffnen.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        menuItemOeffnen_actionPerformed(e);
      }
    });
    menuItemSpeichern.setMnemonic('S');
    menuItemSpeichern.setText("Meldedatei speichern");
    menuItemSpeichern.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        menuItemSpeichern_actionPerformed(e);
      }
    });
    menuItemSpeichernUnter.setMnemonic('U');
    menuItemSpeichernUnter.setText("Meldedatei speichern unter");
    menuItemSpeichernUnter.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        menuItemSpeichernUnter_actionPerformed(e);
      }
    });
    menuItemExport.setMnemonic('C');
    menuItemExport.setText("Meldedatei als CSV exportieren");
    menuItemExport.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        menuItemExport_actionPerformed(e);
      }
    });
    jLabel18.setText("Blauer Wimpel: Km: ");
    wimpelKm.setForeground(Color.black);
    wimpelKm.setText("0 Km");
    jLabel20.setText("Schnitt: ");
    wimpelSchnitt.setForeground(Color.black);
    wimpelSchnitt.setText("0,0 Km");
    erfuellt.setForeground(Color.black);
    erfuellt.setOpaque(true);
    erfuellt.setHorizontalAlignment(SwingConstants.CENTER);
    erfuellt.setText("nicht erfüllt");
    jLabel19.setText("Adresse: ");
    adresse.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        eintragGeaendert(e);
      }
      public void keyReleased(KeyEvent e) {
        validateErfuellt(e);
      }
    });
    jLabel21.setText("Gruppe: ");
    gruppe.setForeground(Color.black);
    menuitemAddToCVS.setMnemonic('A');
    menuitemAddToCVS.setText("Meldedatei an Standard-CVS anhängen");
    menuitemAddToCVS.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        menuitemAddToCVS_actionPerformed(e);
      }
    });
    jMenu1.setMnemonic('K');
    jMenu1.setText("Konfiguration");
    menuitemEinstellungen.setMnemonic('E');
    menuitemEinstellungen.setText("Einstellungen");
    menuitemEinstellungen.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        menuitemEinstellungen_actionPerformed(e);
      }
    });
    posLabel.setForeground(Color.black);
    posLabel.setPreferredSize(new Dimension(100, 15));
    posLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    vereinsname.addKeyListener(new java.awt.event.KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        dateiGeaendert(e);
      }
    });
    wett.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        wett_itemStateChanged(e);
      }
    });
    jLabel22.setText("Datum");
    jLabel23.setText("Ziel");
    jLabel24.setText("Km");
    jLabel25.setText("Bereich");
    menuItemHelp.setMnemonic('H');
    menuItemHelp.setText("Hilfe");
    menuItemHelp.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        menuItemHelp_actionPerformed(e);
      }
    });
    wett.setNextFocusableComponent(wettJahr);
    vereinsname.setNextFocusableComponent(benutzername);
    adresse.setNextFocusableComponent(saveButton);
    jMenuFile.add(menuItemNeu);
    jMenuFile.add(menuItemOeffnen);
    jMenuFile.add(menuItemSpeichern);
    jMenuFile.add(menuItemSpeichernUnter);
    jMenuFile.addSeparator();
    jMenuFile.add(menuItemExport);
    jMenuFile.add(menuitemAddToCVS);
    jMenuFile.addSeparator();
    jMenuFile.add(menuItemBeenden);
    jMenuHelp.add(menuItemHelp);
    jMenuHelp.add(menuItemUeber);
    jMenuBar1.add(jMenuFile);
    jMenuBar1.add(jMenu1);
    jMenuBar1.add(jMenuHelp);
    contentPane.add(wettPanel, BorderLayout.NORTH);
    wettPanel.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    wettPanel.add(wett, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    wettPanel.add(jLabel2, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
    wettPanel.add(wettJahr, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    contentPane.add(jPanel1, BorderLayout.CENTER);
    jPanel1.add(vereinPanel, BorderLayout.NORTH);
    vereinPanel.add(jLabel3, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    vereinPanel.add(vereinsname, new GridBagConstraints(1, 0, 3, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    vereinPanel.add(jLabel4, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    vereinPanel.add(benutzername, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    vereinPanel.add(jLabel5, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
    vereinPanel.add(mitglieder, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(jPanel2, BorderLayout.CENTER);
    jPanel2.add(meldPanel, BorderLayout.NORTH);
    meldPanel.add(jLabel6, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    meldPanel.add(meldName, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    meldPanel.add(jLabel7, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
    meldPanel.add(meldEmail, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    jPanel2.add(jPanel3, BorderLayout.CENTER);
    jPanel3.add(versandPanel, BorderLayout.NORTH);
    versandPanel.add(lblVersandName, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    versandPanel.add(versandName, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    versandPanel.add(lblVersandAdresszusatz, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    versandPanel.add(versandZusatz, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    versandPanel.add(lblVersandStrasse, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    versandPanel.add(versandStrasse, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    versandPanel.add(lblVersandPlzOrt, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    versandPanel.add(versandOrt, new GridBagConstraints(7, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel3.add(jPanel5, BorderLayout.SOUTH);
    jPanel5.add(jPanel6, BorderLayout.CENTER);
    jPanel6.add(lblAnzahlTeilnehmerErfuelltGesamt,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel6.add(teilnehmerAnz, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel6.add(jLabel13, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
    jPanel6.add(meldegeld, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel6.add(jLabel18,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel6.add(wimpelKm, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel6.add(jLabel20, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
    jPanel6.add(wimpelSchnitt, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerPanel.add(jPanel7, BorderLayout.SOUTH);
    jPanel7.add(saveButton, null);
    jPanel3.add(teilnehmerPanel, BorderLayout.CENTER);
    teilnehmerPanel.add(jPanel4, BorderLayout.NORTH);
    jPanel4.add(firstButton, null);
    jPanel4.add(prevButton, null);
    jPanel4.add(nextButton, null);
    jPanel4.add(lastButton, null);
    jPanel4.add(newButton, null);
    jPanel4.add(deleteButton, null);
    jPanel4.add(posLabel, null);

    jMenu1.add(menuitemEinstellungen);
    this.setJMenuBar(jMenuBar1);
	JScrollBar tns = teilnehmerScrollPane.getVerticalScrollBar();
    if (tns!=null) {
    	tns.setUnitIncrement(12); // faster scrolling
    }

  }

  /**Overridden so we can exit when window is closed*/
  protected void processWindowEvent(WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      menuItemBeenden_actionPerformed(null);
    }
  }


  void iniFahrten(int n) {
    if (teilnehmerDatenPanel != null) {
      teilnehmerScrollPane.getViewport().remove(teilnehmerDatenPanel);
    }
    teilnehmerDatenPanel = new JPanel();
    teilnehmerDatenPanel.setLayout(new GridBagLayout());
    teilnehmerDatenPanel.setBorder(BorderFactory.createEtchedBorder());

    fahrtLabel = new JLabel[n];
    fahrtDatum = new JTextField[n];
    fahrtZiel = new JTextField[n];
    fahrtKm = new JTextField[n];
    fahrtZf = new JTextField[n];

    teilnehmerScrollPane.getViewport().add(teilnehmerDatenPanel,null);
    teilnehmerPanel.add(teilnehmerScrollPane, BorderLayout.CENTER);
    teilnehmerDatenPanel.add(jLabel12,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jLabel14,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jLabel15,  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jLabel16,  new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(nachname,  new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(vorname,  new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jahrgang,  new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(geschlecht,  new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jLabel17,  new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(kilometer,  new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(erfuellt,  new GridBagConstraints(0, 7, 2, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jLabel19,  new GridBagConstraints(0, 3+n, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(adresse,  new GridBagConstraints(1, 3+n, 6, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jLabel21,  new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(gruppe,  new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jLabel22,   new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jLabel23,  new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jLabel24,  new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    teilnehmerDatenPanel.add(jLabel25,  new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

    for (int i=0; i<n; i++) {
      fahrtLabel[i] = new JLabel();
      fahrtLabel[i].setText("Fahrt "+(i+1)+": ");
      teilnehmerDatenPanel.add(fahrtLabel[i], new GridBagConstraints(2, i+1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 30, 0, 0), 0, 0));
    }
    for (int i=0; i<n; i++) {
      fahrtDatum[i] = new JTextField();
      fahrtDatum[i].setPreferredSize(new Dimension(80, FIELD_HEIGHT));
      fahrtDatum[i].addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyTyped(KeyEvent e) {
          eintragGeaendert(e);
        }
        public void keyReleased(KeyEvent e) {
          validateErfuellt(e);
        }
      });
      fahrtDatum[i].addFocusListener(new java.awt.event.FocusAdapter() {
        public void focusLost(FocusEvent e) {
          fahrtDatumValidate(e);
        }
      });
      teilnehmerDatenPanel.add(fahrtDatum[i], new GridBagConstraints(3, i+1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    }
    for (int i=0; i<n; i++) {
      fahrtZiel[i] = new JTextField();
      fahrtZiel[i].setPreferredSize(new Dimension(300, FIELD_HEIGHT));
      fahrtZiel[i].addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyTyped(KeyEvent e) {
          eintragGeaendert(e);
        }
        public void keyReleased(KeyEvent e) {
          validateErfuellt(e);
        }
      });
      teilnehmerDatenPanel.add(fahrtZiel[i], new GridBagConstraints(4, i+1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    }
    for (int i=0; i<n; i++) {
      fahrtKm[i] = new JTextField();
      fahrtKm[i].setPreferredSize(new Dimension(40, FIELD_HEIGHT));
      fahrtKm[i].addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyTyped(KeyEvent e) {
          eintragGeaendert(e);
        }
        public void keyReleased(KeyEvent e) {
          validateErfuellt(e);
        }
      });
      fahrtKm[i].addFocusListener(new java.awt.event.FocusAdapter() {
        public void focusLost(FocusEvent e) {
          fahrtKmValidate(e);
        }
      });
      teilnehmerDatenPanel.add(fahrtKm[i], new GridBagConstraints(5, i+1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    }
    for (int i=0; i<n; i++) {
      fahrtZf[i] = new JTextField();
      fahrtZf[i].setPreferredSize(new Dimension(40, FIELD_HEIGHT));
      fahrtZf[i].addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyTyped(KeyEvent e) {
          eintragGeaendert(e);
        }
        public void keyReleased(KeyEvent e) {
          validateErfuellt(e);
        }
      });
      fahrtZf[i].addFocusListener(new java.awt.event.FocusAdapter() {
        public void focusLost(FocusEvent e) {
          fahrtZfValidate(e);
        }
      });
      teilnehmerDatenPanel.add(fahrtZf[i], new GridBagConstraints(6, i+1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      if (i+1<n) fahrtZf[i].setNextFocusableComponent(fahrtDatum[i+1]);
    }
  }

  void iniItems() {
    wett.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        wett_itemStateChanged(e);
      }
    });
    for (int i=0; i<WettDefs.ANZWETT; i++)
      if (wettDefs != null && wettDefs.getWettDef(i,9999) != null &&
          (i == WettDefs.DRV_FAHRTENABZEICHEN ||
           i == WettDefs.DRV_WANDERRUDERSTATISTIK ||
           i == WettDefs.LRVBERLIN_SOMMER ||
           i == WettDefs.LRVBERLIN_WINTER ||
           i == WettDefs.LRVBERLIN_BLAUERWIMPEL))
        wett.addItem(wettDefs.getWettDef(i,9999).name);
    geschlecht.addItem("--- Bitte auswählen ---");
    geschlecht.addItem("männlich");
    geschlecht.addItem("weiblich");

  }

/*********************************************************************************************************/
/* MENÜ **************************************************************************************************/
/*********************************************************************************************************/

  void menuItemNeu_actionPerformed(ActionEvent e) {
    if (efw != null && !dateiAenderungenGespeichert()) return;

    if (efw != null) this.setTitle(KURZTITEL + " - <neue Datei>");
    efw = new EfaWett();
    currentTeilnehmer = new EfaWettMeldung();
    currentTeilnehmerNummer = 0;
    setBlankFields(); setBlankTeilnehmer();
    geaendertDatei = false;
    geaendertEintrag = false;
    anStdCsvAngehaengt = false;
  }

  void menuItemOeffnen_actionPerformed(ActionEvent e) {
    if (efw != null && !dateiAenderungenGespeichert()) return;

    String dir = cfg.getDirEfw();
    if (dir.equals("") || !new File(dir).isDirectory()) dir = System.getProperty("user.dir");


    String dat = Dialog.dateiDialog(this,"Meldedatei öffnen","efa Meldedatei (*.efw)","efw",dir,false);

    if (dat != null) {
      efw = new EfaWett();
      efw.datei = dat;
      try {
        if (!efw.readFile()) {
          Dialog.infoDialog("Fehler","Datei '"+efw.datei+"' hat ungültiges Format!\n"+
                                             "Es können nur Dateien im Format "+efw.EFAWETT+" gelesen werden!");
          efw = null;
        } else {
          this.setTitle(KURZTITEL + " - " + efw.datei);
          setFields();
          geaendertDatei = false;
          anStdCsvAngehaengt = false;
        }
      } catch(IOException ee) {
        Dialog.infoDialog("Fehler","Datei '"+efw.datei+"' kann nicht gelesen werden!");
        efw = null;
      }
    }
  }
  void menuItemSpeichern_actionPerformed(ActionEvent e) {
    if (eintragAenderungenGespeichert()) speichereDatei(false);
  }

  void menuItemSpeichernUnter_actionPerformed(ActionEvent e) {
    if (eintragAenderungenGespeichert()) speichereDatei(true);
  }

  void menuItemExport_actionPerformed(ActionEvent e) {
    if (efw == null || !eintragAenderungenGespeichert()) return;
    exportCVS();
  }

  void menuitemAddToCVS_actionPerformed(ActionEvent e) {
    if (cfg.getStdCsv() == null || cfg.getStdCsv().equals("")) {
      Dialog.infoDialog("Fehler","Es wurde keine Standard-CSV-Datei angegeben!");
      return;
    }
    if (efw == null || !eintragAenderungenGespeichert()) return;
    if (anStdCsvAngehaengt) {
      Dialog.infoDialog("Warnung","Die aktuellen Daten wurden bereits an die Standard-CSV-Datei angehängt!\n"+
                                         "Um die Daten erneut anzuhängen, öffne die aktuelle Datei neu.");
      return;
    }
    if (Dialog.yesNoCancelDialog("An Standard-CVS-Datei anhängen",
                                 "Sollen alle Teilnehmer" + (cfg.getExportNurErfuellt() ? ", die die Bedingungen erfüllt haben, " : " (auch welche, die die Bedingungen nicht erfüllt haben) " ) + "an die Datei\n'"+cfg.getStdCsv()+"'\nangehängt werden?"
        ) != Dialog.YES) return;

    try {
      BufferedWriter f = new BufferedWriter(new FileWriter(cfg.getStdCsv(),true));
      int c = writeCSVfile(f);
      if (efw.wettId != WettDefs.LRVBERLIN_BLAUERWIMPEL)
        Dialog.infoDialog("Bestätigung","Es wurden "+c+" von insgesamt "+getAnzTeilnehmer()+" Einträgen in die Datei\n'"+cfg.getStdCsv()+"'\ngeschrieben!");
      else
        Dialog.infoDialog("Bestätigung","Es wurden die Daten für den Verein "+efw.verein_name+" in die Datei\n'"+cfg.getStdCsv()+"'\ngeschrieben!");
      anStdCsvAngehaengt = true;
    } catch(IOException ee) {
      Dialog.infoDialog("Fehler","Die Datei\n'"+cfg.getStdCsv()+"'\nkonnte nicht geschrieben werden!");
    }
  }


  public void menuItemBeenden_actionPerformed(ActionEvent e) {
    if (efw != null && !dateiAenderungenGespeichert()) return;
    Daten.haltProgram(0);
  }

  void menuitemEinstellungen_actionPerformed(ActionEvent e) {
    EmilConfigFrame dlg = new EmilConfigFrame(this);
    Dialog.setDlgLocation(dlg,this);
    dlg.setModal(true);
    dlg.show();
  }


  public void menuItemUeber_actionPerformed(ActionEvent e) {
    EmilFrame_AboutBox dlg = new EmilFrame_AboutBox(this);
    Dialog.setDlgLocation(dlg,this);
    dlg.setModal(true);
    dlg.show();
  }

  void menuItemHelp_actionPerformed(ActionEvent e) {
  }



/*********************************************************************************************************/
/* EVENTS ************************************************************************************************/
/*********************************************************************************************************/
  void wettJahr_focusLost(FocusEvent e) {
    TMJ tmj = EfaUtil.string2date(wettJahr.getText(),-1,0,0);
    int year = 2000;
    if (tmj.tag>=0 && tmj.tag<100) tmj.tag += 1900;
    if (tmj.tag>=0 && tmj.tag<1980) tmj.tag += 100;
    if (tmj.tag>=0) year = tmj.tag;
    WettDef wett = wettDefs.getWettDef(this.wett.getSelectedIndex(),year);
    if (wett == null) return;
    if (wett.von.jahr == wett.bis.jahr) {
      wettJahr.setText(Integer.toString(year+wett.von.jahr));
    } else {
      wettJahr.setText((year+wett.von.jahr)+"/"+
                       (year+wett.bis.jahr));
    }

  }

  void firstButton_actionPerformed(ActionEvent e) {
    if (efw == null) return;
    if (!eintragAenderungenGespeichert()) return;
    currentTeilnehmerNummer = 0;
    currentTeilnehmer = efw.meldung;
    setTeilnehmer(currentTeilnehmer);
  }

  void prevButton_actionPerformed(ActionEvent e) {
    if (efw == null) return;
    if (currentTeilnehmerNummer < 1) return;
    if (!eintragAenderungenGespeichert()) return;
    currentTeilnehmerNummer--;
    EfaWettMeldung m = efw.meldung;
    int i=0;
    while (i<currentTeilnehmerNummer && m != null) { i++; m = m.next; }
    currentTeilnehmerNummer = i; // bewirkt nur dann etwas, wenn m == null erreicht
    currentTeilnehmer = m;
    setTeilnehmer(currentTeilnehmer);
  }

  void nextButton_actionPerformed(ActionEvent e) {
    if (efw == null) return;
    if (currentTeilnehmer.next == null) return;
    if (!eintragAenderungenGespeichert()) return;
    currentTeilnehmer = currentTeilnehmer.next;
    currentTeilnehmerNummer++;
    setTeilnehmer(currentTeilnehmer);
  }

  void lastButton_actionPerformed(ActionEvent e) {
    if (efw == null) return;
    if (!eintragAenderungenGespeichert()) return;
    currentTeilnehmer = efw.letzteMeldung();
    currentTeilnehmerNummer = getAnzTeilnehmer()-1;
    setTeilnehmer(currentTeilnehmer);
  }

  void newButton_actionPerformed(ActionEvent e) {
    if (efw == null) return;
    if (!eintragAenderungenGespeichert()) return;
    currentTeilnehmer = new EfaWettMeldung();
    currentTeilnehmerNummer = getAnzTeilnehmer();
    EfaWettMeldung m = efw.letzteMeldung();
    if (m == null) efw.meldung = currentTeilnehmer;
    else m.next = currentTeilnehmer;
    setBlankTeilnehmer();
    geaendertDatei = true;
  }

  void deleteButton_actionPerformed(ActionEvent e) {
    if (efw == null) return;
    if (currentTeilnehmer == null) return;
    if (Dialog.yesNoDialog("Wirklich löschen?","Möchtest Du den aktuellen Eintrag wirklich löschen?") == Dialog.NO) return;

    if (currentTeilnehmerNummer == 0) currentTeilnehmer = efw.meldung = efw.meldung.next;
    else {
      // vorherigen Datensatz ausfindig machen
      EfaWettMeldung m = efw.meldung;
      int i=0;
      while (i<currentTeilnehmerNummer-1 && m != null && m.next != null) { i++; m = m.next; }
      if (m.next != null) m.next = m.next.next;
      currentTeilnehmer = m.next;
      if (currentTeilnehmer == null) { currentTeilnehmer = m; currentTeilnehmerNummer--; }
    }
    if (currentTeilnehmer == null) newButton_actionPerformed(null);
    setTeilnehmer(currentTeilnehmer);
    geaendertDatei = true;
  }

  void saveButton_actionPerformed(ActionEvent e) {
    speichereEintrag();
  }

  void dateiGeaendert(KeyEvent e) {
   if (efw == null) return;
   geaendertDatei = true;
  }

  void eintragGeaendert(KeyEvent e) {
    if (efw == null) return;
    geaendertEintrag = true;
  }

  void validateErfuellt(KeyEvent e) {
    checkErfuellt();
  }

  void geschlecht_itemStateChanged(ItemEvent e) {
    geaendertEintrag = true;
    checkErfuellt();
  }

  void wett_itemStateChanged(ItemEvent e) {
    geaendertDatei = true;
    if (efw != null) {
      efw.wettId = wett.getSelectedIndex();
      if (efw.wettId < 0) efw.wettId = -1;
    }
    updateTeilnehmerMeldegeld();
    checkErfuellt();
  }

  void mitglieder_focusLost(FocusEvent e) {
    if (mitglieder.getText().trim().equals("")) { mitglieder.setText(""); return; }
    mitglieder.setText(Integer.toString(EfaUtil.string2date(mitglieder.getText(),0,0,0).tag));
  }

  void jahrgang_focusLost(FocusEvent e) {
    if (jahrgang.getText().trim().equals("")) { jahrgang.setText(""); return; }
    jahrgang.setText(Integer.toString(EfaUtil.string2date(jahrgang.getText(),0,0,0).tag));
  }

  void kilometer_focusLost(FocusEvent e) {
    if (kilometer.getText().trim().equals("")) { kilometer.setText(""); return; }
    TMJ tmj = EfaUtil.string2date(kilometer.getText(),0,0,0);
    if (tmj.monat != 0) kilometer.setText(tmj.tag + "," + tmj.monat);
    else kilometer.setText(""+tmj.tag);
  }

  void fahrtDatumValidate(FocusEvent e) {
    JTextField feld = (JTextField)e.getComponent();
    if (feld.getText().trim().equals("")) { feld.setText(""); return; }
    TMJ tmj = EfaUtil.correctDate(feld.getText(),0,0,EfaUtil.string2date(wettJahr.getText(),0,0,0).tag);
    feld.setText(tmj.tag+"."+tmj.monat+"."+tmj.jahr);
  }

  void fahrtKmValidate(FocusEvent e) {
    JTextField feld = (JTextField)e.getComponent();
    if (feld.getText().trim().equals("")) { feld.setText(""); return; }
    TMJ tmj = EfaUtil.string2date(feld.getText(),0,0,0);
    if (tmj.monat != 0) feld.setText(tmj.tag + "," + tmj.monat);
    else feld.setText(""+tmj.tag);
  }

  void fahrtZfValidate(FocusEvent e) {
    JTextField feld = (JTextField)e.getComponent();
    if (feld.getText().trim().equals("")) { feld.setText(""); return; }
    feld.setText(new ZielfahrtFolge(feld.getText()).toString());
  }

/*********************************************************************************************************/
/* ROUTINEN **********************************************************************************************/
/*********************************************************************************************************/
  void setBlankFields() {
    wett.setSelectedIndex(0);
    wettJahr.setText("");
    meldegeld.setText("");
    vereinsname.setText("");
    benutzername.setText("");
    mitglieder.setText("");
    meldName.setText("");
    meldEmail.setText("");
    versandName.setText("");
    versandZusatz.setText("");
    versandStrasse.setText("");
    versandOrt.setText("");
    wimpelKm.setText("");
    wimpelSchnitt.setText("");
  }

  void setBlankTeilnehmer() {
    nachname.setText("");
    vorname.setText("");
    jahrgang.setText("");
    geschlecht.setSelectedIndex(0);
    kilometer.setText("");
    adresse.setText("");
    setErfuellt(false);
    for (int i=0; i<fahrtDatum.length; i++)
      if (fahrtDatum[i] != null) fahrtDatum[i].setText("");
    for (int i=0; i<fahrtZiel.length; i++)
      if (fahrtZiel[i] != null) fahrtZiel[i].setText("");
    for (int i=0; i<fahrtKm.length; i++)
      if (fahrtKm[i] != null) fahrtKm[i].setText("");
    for (int i=0; i<fahrtZf.length; i++)
      if (fahrtZf[i] != null) fahrtZf[i].setText("");
    geaendertEintrag = false;

    updateTeilnehmerMeldegeld();
  }

  void setFields() {
    if (efw == null) return;
    setBlankFields();
    if (efw.allg_wett != null)
      for (int i=0; i<wettDefs.ANZWETT; i++)
        if (wettDefs.getWettDef(i,EfaUtil.string2int(efw.allg_wettjahr,9999)) != null &&
            efw.allg_wett.equals(wettDefs.getWettDef(i,EfaUtil.string2int(efw.allg_wettjahr,9999)).key)) {
          efw.wettId = i;
          wett.setSelectedIndex(i);
        }
    if (efw.allg_wettjahr != null)
      wettJahr.setText(efw.allg_wettjahr);

    if (efw.verein_name != null)
      vereinsname.setText(efw.verein_name);
    if (efw.verein_user != null)
      benutzername.setText(efw.verein_user);
    if (efw.verein_mitglieder != null)
      mitglieder.setText(efw.verein_mitglieder);

    if (efw.meld_name != null)
      meldName.setText(efw.meld_name);
    if (efw.meld_email != null)
      meldEmail.setText(efw.meld_email);

    if (efw.versand_name != null)
      versandName.setText(efw.versand_name);
    if (efw.versand_zusatz != null)
      versandZusatz.setText(efw.versand_zusatz);
    if (efw.versand_strasse != null)
      versandStrasse.setText(efw.versand_strasse);
    if (efw.versand_ort != null)
      versandOrt.setText(efw.versand_ort);

    if (efw.wimpel_km != null)
      wimpelKm.setText(efw.wimpel_km);
    if (efw.wimpel_schnitt != null)
      wimpelSchnitt.setText(efw.wimpel_schnitt);

    currentTeilnehmerNummer = 0;
    setTeilnehmer(efw.meldung);

    updateTeilnehmerMeldegeld();
  }

  void setTeilnehmer(EfaWettMeldung m) {
    setBlankTeilnehmer();
    if (m == null) return;
    if (m.nachname != null)
      nachname.setText(m.nachname);
    if (m.vorname != null)
      vorname.setText(m.vorname);
    if (m.jahrgang != null)
      jahrgang.setText(m.jahrgang);
    if (m.geschlecht != null)
      if (m.geschlecht.equals("M")) geschlecht.setSelectedIndex(1);
      else if (m.geschlecht.equals("W")) geschlecht.setSelectedIndex(2);
      else geschlecht.setSelectedIndex(0);
    if (m.kilometer != null)
      kilometer.setText(m.kilometer);
    if (m.anschrift != null)
      adresse.setText(m.anschrift);

    for (int i=0; i<m.fahrt.length; i++)
      if (m.fahrt[i][0] != null)
        for (int j=0; j<m.fahrt[i].length; j++)
          if (m.fahrt[i][j] != null && i<fahrtDatum.length && j<4)
            switch(j) {
              case 0: fahrtDatum[i].setText(m.fahrt[i][j]); break;
              case 1: fahrtZiel[i].setText(m.fahrt[i][j]); break;
              case 2: fahrtKm[i].setText(m.fahrt[i][j]); break;
              case 3: fahrtZf[i].setText(m.fahrt[i][j]); break;
            }

    currentTeilnehmer = m;
    geaendertEintrag = false;

    checkErfuellt();
    updateTeilnehmerMeldegeld();

    posLabel.setText((currentTeilnehmerNummer+1) + " / " + getAnzTeilnehmer());
  }

  void setTeilnehmerAnz(int anzErfuellt, int anzGesamt) {
    teilnehmerAnz.setText(anzErfuellt+"/"+anzGesamt);
  }

  void updateTeilnehmerMeldegeld() {
    int year = EfaUtil.string2date(wettJahr.getText().trim(),0,0,0).tag;
    int geld;
    int anz = getAnzTeilnehmerErfuellt();
    setTeilnehmerAnz(anz,getAnzTeilnehmer());
    if (efw != null && efw.wettId >= 0)
      geld = 0; // @todo (P9) Emil: wettDefs.getWettDef(efw.wettId,year).meldegeld_grund + anz*wettDefs.getWettDef(efw.wettId,year).meldegeld_teiln;
    else geld = 0;
    meldegeld.setText(geld / 100 + "," + (geld % 100 < 10 ? "0"+geld%100 : ""+geld%100) + " EUR");

    if (efw != null && efw.wettId == WettDefs.LRVBERLIN_BLAUERWIMPEL) {
      wimpelKm.setText(getKmTeilnehmer());
      TMJ tmj = EfaUtil.string2date(wimpelKm.getText(),0,0,0);
      if (anz != 0) {
        wimpelSchnitt.setText(EfaUtil.zehntelInt2String((tmj.tag*10 + tmj.monat) / anz));
      } else {
        wimpelSchnitt.setText("");
      }
    }
  }

  void getFields() {
    if (efw == null) return;
    String s;

    efw.wettId = wett.getSelectedIndex();
    if (efw.wettId < 0) efw.wettId = -1;

    int wettjahr = 0;
    try { wettjahr = Integer.parseInt(wettJahr.getText().trim()); } catch(Exception e) {}
    efw.allg_wett = (wettDefs.getWettDef(wett.getSelectedIndex(),wettjahr) != null ? wettDefs.getWettDef(wett.getSelectedIndex(),wettjahr).key : null);
    if (!(s = wettJahr.getText().trim()).equals("")) efw.allg_wettjahr = s;
    else efw.allg_wettjahr = null;

    if (!(s = vereinsname.getText().trim()).equals("")) efw.verein_name = s;
    else efw.verein_name = null;
    if (!(s = benutzername.getText().trim()).equals("")) efw.verein_user = s;
    else efw.verein_user = null;
    if (!(s = mitglieder.getText().trim()).equals("")) {
      int i = EfaUtil.string2date(s,-1,0,0).tag;
      if (i != -1) efw.verein_mitglieder = Integer.toString(i);
      else efw.verein_mitglieder = null;
    } else efw.verein_mitglieder = null;

    if (!(s = meldName.getText().trim()).equals("")) efw.meld_name = s;
    else efw.meld_name = null;
    if (!(s = meldEmail.getText().trim()).equals("")) efw.meld_email = s;
    else efw.meld_email = null;

    if (!(s = versandName.getText().trim()).equals("")) efw.versand_name = s;
    else efw.versand_name = null;
    if (!(s = versandZusatz.getText().trim()).equals("")) efw.versand_zusatz = s;
    else efw.versand_zusatz = null;
    if (!(s = versandStrasse.getText().trim()).equals("")) efw.versand_strasse = s;
    else efw.versand_strasse = null;
    if (!(s = versandOrt.getText().trim()).equals("")) efw.versand_ort = s;
    else efw.versand_ort = null;

    if (efw.wettId == WettDefs.LRVBERLIN_BLAUERWIMPEL) {
      if (!(s = wimpelKm.getText().trim()).equals("")) efw.wimpel_km = s;
      else efw.wimpel_km = null;
      if (!(s = wimpelSchnitt.getText().trim()).equals("")) efw.wimpel_schnitt = s;
      else efw.wimpel_schnitt = null;
      if (!(s = teilnehmerAnz.getText().trim()).equals("")) efw.wimpel_mitglieder = s;
      else efw.wimpel_mitglieder = null;
    } else {
      efw.wimpel_km = null;
      efw.wimpel_schnitt = null;
      efw.wimpel_mitglieder = null;
    }
  }

  EfaWettMeldung getTeilnehmer() {
    if (efw == null || currentTeilnehmer == null) return null;
    EfaWettMeldung neu = new EfaWettMeldung();
    neu.nachname = nachname.getText().trim();
    neu.vorname = vorname.getText().trim();
    neu.jahrgang = jahrgang.getText().trim();
    switch (geschlecht.getSelectedIndex()) {
      case 0: neu.geschlecht = "";  break;
      case 1: neu.geschlecht = "M"; break;
      case 2: neu.geschlecht = "W"; break;
    }
    neu.kilometer = kilometer.getText().trim();
    neu.abzeichen = null;
    neu.anschrift = adresse.getText().trim();
    neu.restkm = null;
    neu.fahrt = new String[getAnzFahrten()][4];
    for (int i=0; i<getAnzFahrten(); i++) {
      neu.fahrt[i][0] = fahrtDatum[i].getText().trim();
      neu.fahrt[i][1] = fahrtZiel[i].getText().trim();
      neu.fahrt[i][2] = fahrtKm[i].getText().trim();
      neu.fahrt[i][3] = fahrtZf[i].getText().trim();
    }

    neu.gruppe = isErfuellt(wett.getSelectedIndex(),wettJahr.getText().trim(),neu);
    return neu;
  }


  // Anzahl der im Formular eingetragenen Fahrten ermitteln
  int getAnzFahrten() {
    int c=0; // Zähler für die Fahrten
    for (int i=0; i<fahrtDatum.length; i++) // Fahrt vollständig
      if (!fahrtDatum[i].getText().trim().equals("") ||
          !fahrtZiel[i].getText().trim().equals("") ||
          !fahrtKm[i].getText().trim().equals("") ||
          !fahrtZf[i].getText().trim().equals("")) c++;
    return c;
  }

/*
  // prüft, ob Bedingungen erfüllt;
  // Rückgabe: Name der Gruppe oder null, wenn nicht erfüllt
  String isErfuellt() {
    if (wett.getSelectedIndex() == 0 || wettJahr.getText().trim().equals("")) return null;
    TMJ tmj = EfaUtil.string2date(wettJahr.getText(),0,0,0);
    int _jahr = tmj.tag;
    tmj = EfaUtil.string2date(jahrgang.getText(),0,0,0);
    int _jahrgang = tmj.tag;
    tmj = EfaUtil.string2date(kilometer.getText(),0,0,0);
    if (tmj.monat<0 || tmj.monat>10) tmj.monat=0;
    int _km = tmj.tag*10 + tmj.monat;
    int _geschlecht = geschlecht.getSelectedIndex()-1;
    if (_jahrgang == 0 || _geschlecht<0) return null;
    int _z1 = 0; int _z2 = 0;
    switch (wett.getSelectedIndex()) {
      case WettDefs.LRVBERLIN_SOMMER:
        _z1 = countFahrten();
        break;
      case WettDefs.LRVBERLIN_WINTER:
        _z1 = countFahrten();
        boolean[] monate = new boolean[13]; // Monate von 1 bis 12 plus Dummy
        _z2 = 0;
        for (int i=1; i<=12; i++) monate[i] = true;
        for (int i=0; i<fahrtDatum.length; i++) {
          tmj = EfaUtil.string2date(fahrtDatum[i].getText(),0,0,0);
          if (tmj.monat>0 && tmj.monat<13 && monate[tmj.monat]) {
            _z2++;
            monate[tmj.monat] = false;
          }
        }
        break;
    }
    return wettDefs.erfuellt(wett.getSelectedIndex(),_jahr,_jahrgang,_geschlecht,_km,_z1,_z2,0);
  }
*/

  // prüft, ob Bedingungen erfüllt;
  // Rückgabe: Name der Gruppe oder null, wenn nicht erfüllt
  String isErfuellt(int wettbewerb, String wettjahr, EfaWettMeldung m) {
    if (m == null) return null;
    if (wettbewerb == 0 || wettjahr == null || wettjahr.trim().equals("")) return null;
    if (wettbewerb == WettDefs.LRVBERLIN_BLAUERWIMPEL)
      if (m.nachname != null && m.vorname != null && m.kilometer != null &&
          m.nachname.length()>0 && m.vorname.length()>0 && EfaUtil.string2date(m.kilometer,0,0,0).tag>0) return "";
      else return null;
    if (m.jahrgang == null || m.kilometer == null || m.geschlecht == null || m.fahrt == null) return null;
    TMJ tmj = EfaUtil.string2date(wettjahr,0,0,0);
    int _jahr = tmj.tag;
    tmj = EfaUtil.string2date(m.jahrgang,0,0,0);
    int _jahrgang = tmj.tag;
    tmj = EfaUtil.string2date(m.kilometer,0,0,0);
    if (tmj.monat<0 || tmj.monat>10) tmj.monat=0;
    int _km = tmj.tag*10 + tmj.monat;
    int _geschlecht = ( m.geschlecht.equals("M") ? 0 : ( m.geschlecht.equals("W") ? 1 : -1 ) );
    boolean _behinderung = false; // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! (wenn DRV implementiert, dann muß hierfür ein Feld eingefügt werden!)
    if (_jahrgang == 0 || _geschlecht<0) return null;
    int _z1 = 0; int _z2 = 0;
    switch (wettbewerb) {
      case WettDefs.LRVBERLIN_SOMMER:
        _z1 = countFahrten(wettbewerb,_jahr,m);
        break;
      case WettDefs.LRVBERLIN_WINTER:
        _z1 = countFahrten(wettbewerb,_jahr,m);
        boolean[] monate = new boolean[13]; // Monate von 1 bis 12 plus Dummy
        _z2 = 0;
        for (int i=1; i<=12; i++) monate[i] = true;
        for (int i=0; i<m.fahrt.length; i++) {
          if (m.fahrt[i][0] == null) continue;
          tmj = EfaUtil.string2date(m.fahrt[i][0],0,0,0);
          if (tmj.monat>0 && tmj.monat<13 && monate[tmj.monat]) {
            _z2++;
            monate[tmj.monat] = false;
          }
        }
        break;
    }
    return wettDefs.erfuellt(wettbewerb,_jahr,_jahrgang,_geschlecht,_behinderung,_km,_z1,_z2,0,0);
  }

  void setErfuellt(boolean erf) {
    if (erf) {
      erfuellt.setText("- erfüllt -");
      erfuellt.setBackground(Color.green);
    } else {
      erfuellt.setText("- nicht erfüllt -");
      erfuellt.setBackground(Color.red);
    }
  }

  void checkErfuellt() {
    if (efw == null || currentTeilnehmer == null) return;
    String gr;
    setErfuellt( (gr = isErfuellt(wett.getSelectedIndex(),wettJahr.getText().trim(),getTeilnehmer())) != null);
    gruppe.setText(gr);
  }

  int getAnzTeilnehmer() {
    if (efw == null) return 0;
    int i=0;
    EfaWettMeldung m = efw.meldung;
    while (m != null) {
      i++;
      m = m.next;
    }
    return i;
  }

  int getAnzTeilnehmerErfuellt() {
    if (efw == null) return 0;
    int i=0;
    EfaWettMeldung m = efw.meldung;
    while (m != null) {
      if (isErfuellt(wett.getSelectedIndex(),wettJahr.getText().trim(),m) != null) i++;
      m = m.next;
    }
    return i;
  }

  String getKmTeilnehmer() {
    int km=0;
    EfaWettMeldung m = efw.meldung;
    while (m != null) {
      TMJ tmj = EfaUtil.string2date(m.kilometer,0,0,0);
      km += tmj.tag*10 + tmj.monat;
      m = m.next;
    }
    return EfaUtil.zehntelInt2String(km);
  }

/*
  // Anzahl der wertbaren Fahrten ermitteln
  int countFahrten() {
    // Zeitraum für Wettbewerb ermitteln
    int wjahr = EfaUtil.string2date(wettJahr.getText(),0,0,0).tag;
    GregorianCalendar vonCal = new GregorianCalendar(wettDefs.wett[wett.getSelectedIndex()].von.jahr+wjahr,wettDefs.wett[wett.getSelectedIndex()].von.monat-1,wettDefs.wett[wett.getSelectedIndex()].von.tag);
    vonCal.set(wettDefs.wett[wett.getSelectedIndex()].von.jahr+wjahr,wettDefs.wett[wett.getSelectedIndex()].von.monat-1,wettDefs.wett[wett.getSelectedIndex()].von.tag);
    GregorianCalendar bisCal = new GregorianCalendar(wettDefs.wett[wett.getSelectedIndex()].bis.jahr+wjahr,wettDefs.wett[wett.getSelectedIndex()].bis.monat-1,wettDefs.wett[wett.getSelectedIndex()].bis.tag);
    bisCal.set(wettDefs.wett[wett.getSelectedIndex()].bis.jahr+wjahr,wettDefs.wett[wett.getSelectedIndex()].bis.monat-1,wettDefs.wett[wett.getSelectedIndex()].bis.tag);

    String tage=" "; // zum ermitteln der doppelten Tage (nur eine Fahrt pro Tag!)
    String zf="";   // Zielfahrten insgesamt
    if (wett.getSelectedIndex()<1) return 0;
    int c=0; // Zähler für die Fahrten

    for (int i=0; i<fahrtDatum.length; i++) {

      // Fahrt vollständig
      if (fahrtDatum[i].getText().trim().equals("") ||
          fahrtKm[i].getText().trim().equals("")) continue;
      if (wett.getSelectedIndex() == WettDefs.LRVBERLIN_SOMMER && fahrtZiel[i].getText().trim().equals("")) continue;
      if (wett.getSelectedIndex() == WettDefs.LRVBERLIN_SOMMER && fahrtZf[i].getText().trim().equals("")) continue;

      // Fahrt im Wettbewerbs-Zeitraum?
      TMJ dateF = EfaUtil.string2date(fahrtDatum[i].getText(),0,0,0);
      GregorianCalendar dateCal = new GregorianCalendar(dateF.jahr,dateF.monat-1,dateF.tag);
      dateCal.set(dateF.jahr,dateF.monat-1+dateCal.getMinimum(GregorianCalendar.MONTH),dateF.tag);
      if (dateCal.before(vonCal) || dateCal.after(bisCal)) {
        continue;
      }

      // Tag doppelt?
      String s = ""+dateF.tag+"-"+dateF.monat+"-"+dateF.jahr;
      if (tage.indexOf(" "+s+" ")>=0)  {
        continue;
      }
      tage = tage + s + " ";

      // LRVSOMMER: Zielfahrt mind. 20 Km lang?
      if (wett.getSelectedIndex() == WettDefs.LRVBERLIN_SOMMER && EfaUtil.string2date(fahrtKm[i].getText(),0,0,0).tag < 20) {
        continue;
      }

      zf = Statistik.makeZf(zf,fahrtZf[i].getText().trim());
      c++;
    }
    int zfc = EfaUtil.countCharInString(zf,';') + 1;
    if (wett.getSelectedIndex() == WettDefs.LRVBERLIN_SOMMER && zfc < c) c = zfc;
    return c;
  }
*/

  // Anzahl der wertbaren Fahrten ermitteln
  int countFahrten(int wettbewerb, int wettjahr, EfaWettMeldung m) {
    WettDef wett = wettDefs.getWettDef(wettbewerb,wettjahr);
    if (wett == null) return 0;

    // Zeitraum für Wettbewerb ermitteln
    GregorianCalendar vonCal = new GregorianCalendar(wett.von.jahr+wettjahr,wett.von.monat-1,wett.von.tag);
    vonCal.set(wett.von.jahr+wettjahr,wett.von.monat-1,wett.von.tag);
    GregorianCalendar bisCal = new GregorianCalendar(wett.bis.jahr+wettjahr,wett.bis.monat-1,wett.bis.tag);
    bisCal.set(wett.bis.jahr+wettjahr,wett.bis.monat-1,wett.bis.tag);

    String tage=" "; // zum ermitteln der doppelten Tage (nur eine Fahrt pro Tag!)
    ZielfahrtFolge zf = new ZielfahrtFolge();   // Zielfahrten insgesamt
    if (wettbewerb<1) return 0;
    int c=0; // Zähler für die Fahrten

    for (int i=0; i<m.fahrt.length; i++) {

      // Fahrt vollständig
      if (m.fahrt[i][0] == null || m.fahrt[i][0].trim().equals("") ||
          m.fahrt[i][2] == null || m.fahrt[i][2].trim().equals("")) continue;
      if (wettbewerb == WettDefs.LRVBERLIN_SOMMER && (m.fahrt[i][1] == null || m.fahrt[i][1].trim().equals(""))) continue;
      if (wettbewerb == WettDefs.LRVBERLIN_SOMMER && (m.fahrt[i][3] == null || m.fahrt[i][3].trim().equals(""))) continue;

      // Fahrt im Wettbewerbs-Zeitraum?
      TMJ dateF = EfaUtil.string2date(m.fahrt[i][0],0,0,0);
      GregorianCalendar dateCal = new GregorianCalendar(dateF.jahr,dateF.monat-1,dateF.tag);
      dateCal.set(dateF.jahr,dateF.monat-1+dateCal.getMinimum(GregorianCalendar.MONTH),dateF.tag);
      if (dateCal.before(vonCal) || dateCal.after(bisCal)) {
        continue;
      }

      // Tag doppelt?
      String s = ""+dateF.tag+"-"+dateF.monat+"-"+dateF.jahr;
      if (tage.indexOf(" "+s+" ")>=0)  {
        continue;
      }
      tage = tage + s + " ";

      // LRVSOMMER: Zielfahrt mind. 20 Km lang?
      if (wettbewerb == WettDefs.LRVBERLIN_SOMMER && EfaUtil.string2date(m.fahrt[i][2],0,0,0).tag < 20) {
        continue;
      }

      zf.addZielfahrten(m.fahrt[i][3]);
      c++;
    }
    zf.reduceToMinimun();
    int zfc = zf.getAnzZielfahrten();
    if (wettbewerb == WettDefs.LRVBERLIN_SOMMER && zfc < c) c = zfc;
    return c;
  }



  void speichereEintrag() {
    EfaWettMeldung neu = getTeilnehmer();
    if (neu == null) return;
    neu.next = currentTeilnehmer.next;

    // vorherigen Datensatz ausfindig machen
    if (efw == null) return;
    EfaWettMeldung m = efw.meldung;
    int i=0;
    while (i<currentTeilnehmerNummer-1 && m != null) { i++; m = m.next; }

    // speichern
    if (currentTeilnehmerNummer > 0) m.next = neu;
    else efw.meldung = neu;
    geaendertEintrag = false;
    geaendertDatei = true;
    updateTeilnehmerMeldegeld();
  }


  boolean fehlendeAngabeAbbruch(String s) {
    return (Dialog.yesNoDialog("Fehlende Angabe",s+"\nTrotzdem speichern?") != Dialog.YES);
  }

  boolean speichereDatei(boolean unter) {
    if (efw == null) return false;
    efw.allg_programm = Main.EMIL_KENNUNG;
    getFields();


    if (efw.allg_wett == null && fehlendeAngabeAbbruch("Es wurde kein Wettbewerb angegeben!")) return false;
    if (efw.allg_wettjahr == null && fehlendeAngabeAbbruch("Es wurde kein Wettbewerbsjahr angegeben!")) return false;
    if (efw.verein_name == null && fehlendeAngabeAbbruch("Es wurde kein Vereinsname angegeben!")) return false;
    if (efw.verein_user == null && fehlendeAngabeAbbruch("Es wurde kein Benutzername angegeben!")) return false;

    if (unter || efw.datei == null) {
      String dir = cfg.getDirEfw();
      if (dir.equals("") || !new File(dir).isDirectory()) dir = System.getProperty("user.dir");


      String dat = Dialog.dateiDialog(this,"Meldedatei speichern unter...","efa Meldedatei (*.efw)","efw",dir,true);
      if (dat != null) {
        efw.datei = dat;
        if (!efw.datei.toLowerCase().endsWith(".efw")) efw.datei = efw.datei + ".efw";
        this.setTitle(KURZTITEL + " - " + efw.datei);
      } else return false;
    }

    try {
      if (efw.writeFile()) {
        geaendertDatei = false;
        return true;
      }
    } catch(IOException e) {}
    Dialog.infoDialog("Fehler","Datei '"+efw.datei+"' konnte nicht gespeichert werden!");
    return false;
  }

  boolean eintragAenderungenGespeichert() {
    if (!geaendertEintrag) return true;
    switch (Dialog.yesNoCancelDialog("Änderungen nicht gespeichert","Änderungen am aktuellen Eintrag wurden noch nicht gespeichert.\nSollen sie jetzt gespeichert werden?")) {
      case Dialog.YES: speichereEintrag(); return true;
      case Dialog.NO: return true;
    }
    return false;
  }

  boolean dateiAenderungenGespeichert() {
    if (!eintragAenderungenGespeichert()) return false;
    if (!geaendertDatei) return true;
    switch (Dialog.yesNoCancelDialog("Änderungen nicht gespeichert","Änderungen an der Meldedatei wurden noch nicht gespeichert.\nSollen sie jetzt gespeichert werden?")) {
      case Dialog.YES: return speichereDatei(false);
      case Dialog.NO: return true;
    }
    return false;
  }


  void exportCVS() {
    String datei;
    String dir = cfg.getDirCsv();
    if (dir.equals("") || !new File(dir).isDirectory()) dir = System.getProperty("user.dir");

    String dat = Dialog.dateiDialog(this,"Meldedatei exportieren als","CSV-Dateien (*.csv)","csv",dir,true);

    if (dat != null) datei = dat;
    else return;
    if (!datei.toLowerCase().endsWith(".csv")) datei += ".csv";

    try {
      BufferedWriter f = new BufferedWriter(new FileWriter(datei));
      int c = writeCSVfile(f);
      if (efw.wettId != WettDefs.LRVBERLIN_BLAUERWIMPEL)
        Dialog.infoDialog("Bestätigung","Es wurden "+c+" von insgesamt "+getAnzTeilnehmer()+" Einträgen in die Datei\n'"+datei+"'\ngeschrieben!");
      else
        Dialog.infoDialog("Bestätigung","Es wurden die Daten für den Verein "+efw.verein_name+" in die Datei\n'"+datei+"'\ngeschrieben!");
    } catch(IOException e) {
      Dialog.infoDialog("Fehler","Die Datei\n'"+datei+"'\nkonnte nicht geschrieben werden!");
    }
  }

  int writeCSVfile(BufferedWriter f) throws IOException {
    getFields();
    if (efw.wettId == WettDefs.LRVBERLIN_BLAUERWIMPEL) {
      f.write(efw.verein_name+"\t"+efw.verein_mitglieder+"\t"+efw.wimpel_km+"\t"+efw.wimpel_schnitt+"\t"+efw.wimpel_mitglieder+"\n");
      f.close();
      return 1;
    }
    EfaWettMeldung m = efw.meldung;
    int c = 0;
    while (m != null) {
      if (cfg.getExportNurErfuellt() && isErfuellt(efw.wettId,efw.allg_wettjahr,m)==null) {
        m = m.next;
        continue;
      }
      f.write( (m.geschlecht == null ? "" : m.geschlecht.toLowerCase()) + "\t" +
               (m.jahrgang   == null ? "" : m.jahrgang) + "\t" +
               (m.nachname   == null ? "" : m.nachname) + "\t" +
               (m.vorname    == null ? "" : m.vorname)  + "\t" +
                vereinsname.getText().trim() + "\t" +
               (m.kilometer  == null ? "" : EfaUtil.replace(m.kilometer,".",",")) + "\n" );
      m = m.next;
      c++;
    }
    f.close();
    return c;
  }




}