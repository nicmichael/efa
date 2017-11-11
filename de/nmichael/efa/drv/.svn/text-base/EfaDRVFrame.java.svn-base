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

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.data.efawett.WettDefs;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import de.nmichael.efa.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class EfaDRVFrame extends JFrame {
  JPanel contentPane;
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel mainPanel = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  JPanel northPanel = new JPanel();
  JPanel centerPanel = new JPanel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JLabel titleLabel = new JLabel();
  GridBagLayout gridBagLayout2 = new GridBagLayout();
  JButton administrationButton = new JButton();
  JButton meldungenFAButton = new JButton();
  JButton beendenButton = new JButton();
  JLabel versionLabel = new JLabel();
  JButton meldungenWSButton = new JButton();
  JLabel copyLabel = new JLabel();
  JLabel modeLabel = new JLabel();

  //Construct the frame
  public EfaDRVFrame() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
      appIni();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    Dialog.frameOpened(this);
  }


  void cancel() {
    Dialog.frameClosed(this);
    Daten.haltProgram(0);
  }

  // ActionHandler Events
  public void keyAction(ActionEvent evt) {
    if (evt == null || evt.getActionCommand() == null) return;
    if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
      cancel();
    }
  }


  private void appIni() {
    // WettDefs.cfg
    Daten.wettDefs = new WettDefs(Daten.efaCfgDirectory+Daten.WETTDEFS);
    Daten.wettDefs.createNewIfDoesntExist();
    Daten.wettDefs.readFile();

    this.meldungenFAButton.setEnabled(false);
    this.meldungenWSButton.setEnabled(false);
    if (Main.drvConfig.aktJahr != 0) {
      String mdat = Daten.efaDataDirectory+Main.drvConfig.aktJahr+Daten.fileSep+DRVConfig.MELDUNGEN_FA_FILE;
      if (EfaUtil.canOpenFile(mdat)) {
        this.meldungenFAButton.setText("DRV-Fahrtenabzeichen für das Jahr "+Main.drvConfig.aktJahr+" bearbeiten");
        this.meldungenFAButton.setEnabled(true);
      } else {
        Dialog.error("Die Datei\n"+mdat+"\nkonnte nicht gefunden werden.\nVorhandene Fahrtenabzeichen-Meldungen des Jahres "+Main.drvConfig.aktJahr+" können daher nicht bearbeitet werden.");
        Logger.log(Logger.ERROR,"Die Datei\n"+mdat+"\nkonnte nicht gefunden werden.\nVorhandene Fahrtenabzeichen-Meldungen des Jahres "+Main.drvConfig.aktJahr+" können daher nicht bearbeitet werden.");
      }
      mdat = Daten.efaDataDirectory+Main.drvConfig.aktJahr+Daten.fileSep+DRVConfig.MELDUNGEN_WS_FILE;
      if (EfaUtil.canOpenFile(mdat)) {
        this.meldungenWSButton.setText("DRV-Wanderruderstatistik für das Jahr "+Main.drvConfig.aktJahr+" bearbeiten");
        this.meldungenWSButton.setEnabled(true);
      } else {
        Dialog.error("Die Datei\n"+mdat+"\nkonnte nicht gefunden werden.\nVorhandene Wanderruderstatistik-Meldungen des Jahres "+Main.drvConfig.aktJahr+" können daher nicht bearbeitet werden.");
        Logger.log(Logger.ERROR,"Die Datei\n"+mdat+"\nkonnte nicht gefunden werden.\nVorhandene Wanderruderstatistik-Meldungen des Jahres "+Main.drvConfig.aktJahr+" können daher nicht bearbeitet werden.");
      }

    }
    this.meldungenFAButton.setVisible(Main.drvConfig.darfFAbearbeiten);
    this.meldungenWSButton.setVisible(Main.drvConfig.darfWSbearbeiten);
    if (Main.drvConfig.testmode || Main.drvConfig.readOnlyMode) {
      if (Main.drvConfig.testmode) modeLabel.setText(" - Testmode - ");
      if (Main.drvConfig.readOnlyMode) modeLabel.setText((Main.drvConfig.testmode ? modeLabel.getText() : "") + " - ReadOnly-Mode - ");
    } else {
      modeLabel.setText("");
    }
  }

  //Component initialization
  private void jbInit() throws Exception  {
    ActionHandler ah= new ActionHandler(this);
    try {
      ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                       new String[] {"ESCAPE","F1"},
                       new String[] {"keyAction","keyAction"});
    } catch(NoSuchMethodException e) {
      System.err.println("Error setting up ActionHandler");
    }

    //setIconImage(Toolkit.getDefaultToolkit().createImage(EfaDRVFrame.class.getResource("[Your Icon]")));
    contentPane = (JPanel) this.getContentPane();
    contentPane.setLayout(borderLayout1);
    this.setSize(new Dimension(516, 308));
    this.setTitle("elektronischer Fahrtenwettbewerb");
    mainPanel.setLayout(borderLayout2);
    northPanel.setLayout(gridBagLayout1);
    titleLabel.setFont(new java.awt.Font("Dialog", 1, 16));
    titleLabel.setForeground(new Color(102, 102, 255));
    titleLabel.setText("elektronischer Fahrtenwettbewerb");
    centerPanel.setLayout(gridBagLayout2);
    administrationButton.setNextFocusableComponent(beendenButton);
    administrationButton.setMnemonic('A');
    administrationButton.setText("Administration");
    administrationButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        administrationButton_actionPerformed(e);
      }
    });
    meldungenFAButton.setEnabled(false);
    meldungenFAButton.setNextFocusableComponent(meldungenWSButton);
    meldungenFAButton.setMnemonic('F');
    meldungenFAButton.setText("DRV-Fahrtenabzeichen für das Jahr ???? bearbeiten");
    meldungenFAButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        meldungenFAButton_actionPerformed(e);
      }
    });
    meldungenWSButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        meldungenWSButton_actionPerformed(e);
      }
    });
    beendenButton.setNextFocusableComponent(meldungenFAButton);
    beendenButton.setActionCommand("beendenButton");
    beendenButton.setMnemonic('B');
    beendenButton.setText("Beenden");
    beendenButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        beendenButton_actionPerformed(e);
      }
    });
    versionLabel.setText("Version "+Daten.VERSION + " (" + Daten.VERSIONID + ")");
    meldungenWSButton.setEnabled(false);
    meldungenWSButton.setNextFocusableComponent(administrationButton);
    meldungenWSButton.setMnemonic('W');
    meldungenWSButton.setText("DRV-Wanderruderstatistik für das Jahr ???? bearbeiten");
    copyLabel.setText("Copyright (c) 2004-"+Daten.COPYRIGHTYEAR+" by Nicolas Michael");
    modeLabel.setForeground(Color.red);
    modeLabel.setText("Testmode");
    contentPane.add(mainPanel, BorderLayout.CENTER);
    mainPanel.add(northPanel, BorderLayout.NORTH);
    mainPanel.add(centerPanel, BorderLayout.CENTER);
    northPanel.add(titleLabel,     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 10, 0, 10), 0, 0));
    northPanel.add(versionLabel,   new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    northPanel.add(copyLabel,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    northPanel.add(modeLabel,  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    centerPanel.add(administrationButton,      new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 20, 0, 20), 0, 0));
    centerPanel.add(meldungenFAButton,       new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 20, 0, 20), 0, 0));
    centerPanel.add(beendenButton,     new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 20, 20, 20), 0, 0));
    centerPanel.add(meldungenWSButton,   new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 20, 0, 20), 0, 0));
  }
  //Overridden so we can exit when window is closed
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      cancel();
    }
  }

  void administrationButton_actionPerformed(ActionEvent e) {
    Logger.log(Logger.INFO,"START Administrationsmodus");
    DRVAdminFrame dlg = new DRVAdminFrame(this);
    Dialog.setDlgLocation(dlg,this);
    dlg.setModal(true);
    dlg.show();
    Logger.log(Logger.INFO,"ENDE Administrationsmodus");
    appIni();
  }

  void meldungenFAButton_actionPerformed(ActionEvent e) {
    if (Main.drvConfig.aktJahr < 1980) {
      Dialog.error("Es ist kein Wettbewerbsjahr ausgewählt.\nBitte wähle zuerst über den Punkt 'Administration' ein Wettbewerbsjahr aus.");
      return;
    }

    Main.drvConfig.meldungenIndex = new MeldungenIndex(Daten.efaDataDirectory+Main.drvConfig.aktJahr+Daten.fileSep+DRVConfig.MELDUNGEN_FA_FILE);
    if (!Main.drvConfig.meldungenIndex.readFile()) {
      Dialog.error("Die Meldungen-Indexdatei\n"+Main.drvConfig.meldungenIndex.getFileName()+"\nkann nicht gelesen werden!");
      Logger.log(Logger.ERROR,"Die Meldungen-Indexdatei\n"+Main.drvConfig.meldungenIndex.getFileName()+"\nkann nicht gelesen werden!");
      return;
    }

    Main.drvConfig.teilnehmer = new Teilnehmer(Daten.efaDataDirectory+DRVConfig.TEILNEHMER_FILE);
    if (!Main.drvConfig.teilnehmer.readFile()) {
      Dialog.error("Die Teilnehmer-Datei\n"+Main.drvConfig.teilnehmer.getFileName()+"\nkann nicht gelesen werden!");
      Logger.log(Logger.ERROR,"Die Teilnehmer-Datei\n"+Main.drvConfig.teilnehmer.getFileName()+"\nkann nicht gelesen werden!");
      return;
    }


    Logger.log(Logger.INFO,"START Meldungen für "+Main.drvConfig.aktJahr+" bearbeiten");
    MeldungenIndexFrame dlg = new MeldungenIndexFrame(this,MeldungenIndexFrame.MELD_FAHRTENABZEICHEN);
    dlg.setSize((int)Dialog.screenSize.getWidth()-100,(int)Dialog.screenSize.getHeight()-100);
    Dialog.setDlgLocation(dlg,this);
    dlg.setModal(true);
    dlg.show();
    Logger.log(Logger.INFO,"ENDE Meldungen für "+Main.drvConfig.aktJahr+" bearbeiten");
  }

  void meldungenWSButton_actionPerformed(ActionEvent e) {
    if (Main.drvConfig.aktJahr < 1980) {
      Dialog.error("Es ist kein Wettbewerbsjahr ausgewählt.\nBitte wähle zuerst über den Punkt 'Administration' ein Wettbewerbsjahr aus.");
      return;
    }

    Main.drvConfig.meldungenIndex = new MeldungenIndex(Daten.efaDataDirectory+Main.drvConfig.aktJahr+Daten.fileSep+DRVConfig.MELDUNGEN_WS_FILE);
    if (!Main.drvConfig.meldungenIndex.readFile()) {
      Dialog.error("Die Meldungen-Indexdatei\n"+Main.drvConfig.meldungenIndex.getFileName()+"\nkann nicht gelesen werden!");
      Logger.log(Logger.ERROR,"Die Meldungen-Indexdatei\n"+Main.drvConfig.meldungenIndex.getFileName()+"\nkann nicht gelesen werden!");
      return;
    }

    Main.drvConfig.teilnehmer = new Teilnehmer(Daten.efaDataDirectory+DRVConfig.TEILNEHMER_FILE);
    if (!Main.drvConfig.teilnehmer.readFile()) {
      Dialog.error("Die Teilnehmer-Datei\n"+Main.drvConfig.teilnehmer.getFileName()+"\nkann nicht gelesen werden!");
      Logger.log(Logger.ERROR,"Die Teilnehmer-Datei\n"+Main.drvConfig.teilnehmer.getFileName()+"\nkann nicht gelesen werden!");
      return;
    }

    Logger.log(Logger.INFO,"START Meldungen für "+Main.drvConfig.aktJahr+" bearbeiten");
    MeldungenIndexFrame dlg = new MeldungenIndexFrame(this,MeldungenIndexFrame.MELD_WANDERRUDERSTATISTIK);
    dlg.setSize((int)Dialog.screenSize.getWidth()-100,(int)Dialog.screenSize.getHeight()-100);
    Dialog.setDlgLocation(dlg,this);
    dlg.setModal(true);
    dlg.show();
    Logger.log(Logger.INFO,"ENDE Meldungen für "+Main.drvConfig.aktJahr+" bearbeiten");
  }

  void beendenButton_actionPerformed(ActionEvent e) {
    cancel();
  }
}