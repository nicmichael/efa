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

import de.nmichael.efa.core.OnlineUpdate;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import de.nmichael.efa.*;
import java.util.Vector;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class DRVAdminFrame extends JDialog implements ActionListener {
  Frame parent;
  JPanel jPanel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel jPanel2 = new JPanel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JButton wettJahrButton = new JButton();
  JButton configButton = new JButton();
  JButton closeButton = new JButton();
  JButton keysButton = new JButton();
  JButton updateButton = new JButton();
  JButton datensicherungButton = new JButton();


  public DRVAdminFrame(Frame parent) {
    super(parent);
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    Dialog.frameOpened(this);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    EfaUtil.pack(this);
    this.parent = parent;
    // this.requestFocus();
  }


  // ActionHandler Events
  public void keyAction(ActionEvent evt) {
    if (evt == null || evt.getActionCommand() == null) return;
    if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
      cancel();
    }
  }


  // Initialisierung des Frames
  private void jbInit() throws Exception {
    ActionHandler ah= new ActionHandler(this);
    try {
      ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                       new String[] {"ESCAPE","F1"}, new String[] {"keyAction","keyAction"});
      jPanel1.setLayout(borderLayout1);
      jPanel2.setLayout(gridBagLayout1);
      wettJahrButton.setNextFocusableComponent(configButton);
      wettJahrButton.setMnemonic('W');
      wettJahrButton.setText("Wettbewerbsjahr festlegen");
      wettJahrButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          wettJahrButton_actionPerformed(e);
        }
    });
      configButton.setNextFocusableComponent(keysButton);
      configButton.setMnemonic('K');
      configButton.setText("Konfiguration");
      configButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          configButton_actionPerformed(e);
        }
    });
      closeButton.setNextFocusableComponent(wettJahrButton);
      closeButton.setMnemonic('C');
      closeButton.setText("Schließen");
      closeButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          closeButton_actionPerformed(e);
        }
    });
      keysButton.setNextFocusableComponent(datensicherungButton);
      keysButton.setMnemonic('S');
      keysButton.setText("Schlüsselverwaltung");
      keysButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          keysButton_actionPerformed(e);
        }
    });
      this.setTitle("Administration");
      updateButton.setNextFocusableComponent(closeButton);
      updateButton.setMnemonic('U');
      updateButton.setText("Online-Update");
      updateButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          updateButton_actionPerformed(e);
        }
    });
      datensicherungButton.setNextFocusableComponent(updateButton);
      datensicherungButton.setMnemonic('D');
      datensicherungButton.setText("Datensicherung");
      datensicherungButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          datensicherungButton_actionPerformed(e);
        }
    });
      this.getContentPane().add(jPanel1, BorderLayout.CENTER);
      jPanel1.add(jPanel2, BorderLayout.CENTER);
      jPanel2.add(wettJahrButton,      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 20, 0, 20), 0, 0));
      jPanel2.add(configButton,      new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 20, 0, 20), 0, 0));
      jPanel1.add(closeButton,  BorderLayout.SOUTH);
      jPanel2.add(keysButton,     new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 20, 0, 20), 0, 0));
      jPanel2.add(updateButton,     new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 20, 20, 20), 0, 0));
      jPanel2.add(datensicherungButton,   new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 20, 0, 20), 0, 0));
    } catch(NoSuchMethodException e) {
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

  void wettJahrButton_actionPerformed(ActionEvent e) {
    String jahr = Dialog.inputDialog("Wettbewerbsjahr eingeben",
                                     "Bitte gib das Jahr ein, für welches Meldungen bearbeitet werden sollen!");
    if (jahr == null) return;
    int j = EfaUtil.string2int(jahr,-1);
    if (j <= -1 || j >= 2100) {
      Dialog.error("Der eingegebene Wert stellt kein gültiges Jahr dar!");
      return;
    }
    if (j < 1900) j += 1900;
    if (j < 1980) j += 100;
    if (j >= 2100) {
      Dialog.error("Der eingegebene Wert stellt kein gültiges Jahr dar!");
      return;
    }

    String mdir = Daten.efaDataDirectory+j+Daten.fileSep;
    String mdatFA = mdir+DRVConfig.MELDUNGEN_FA_FILE;
    String mdatWS = mdir+DRVConfig.MELDUNGEN_WS_FILE;

    if (!EfaUtil.canOpenFile(mdatFA) || !EfaUtil.canOpenFile(mdatWS)) {
      // mind. eine der Indexdateien fehlt
      try {
        boolean neuesDir = false;
        if (!(new File(mdir)).isDirectory()) {
          if (!(new File(mdir)).mkdir()) {
            Dialog.error("Das Verzeichnis\n"+mdir+"\n konnte nicht erstellt werden!");
            return;
          }
          Logger.log(Logger.INFO,"Neues Verzeichnis "+mdir+" für Wettbewerbsjahr "+j+" erstellt.");
          neuesDir = true;
        }
        MeldungenIndex mIndex;
        mIndex = new MeldungenIndex(mdatFA);
        if (!mIndex.readFile()) {
          if (!mIndex.writeFile()) {
            Dialog.error("Die Datei\n"+mIndex.getFileName()+"\n konnte nicht erstellt werden!");
            return;
          }
          Logger.log(Logger.INFO,"Neue Meldungen-Indexdatei "+mdatFA+" für Wettbewerbsjahr "+j+" (Fahrtenabzeichen) erstellt.");
        } else {
          Logger.log(Logger.INFO,"Meldungen-Indexdatei "+mdatFA+" für Wettbewerbsjahr "+j+" (Fahrtenabzeichen) geöffnet.");
        }
        mIndex = new MeldungenIndex(mdatWS);
        if (!mIndex.readFile()) {
          if (!mIndex.writeFile()) {
            Dialog.error("Die Datei\n"+mIndex.getFileName()+"\n konnte nicht erstellt werden!");
            return;
          }
          Logger.log(Logger.INFO,"Neue Meldungen-Indexdatei "+mdatWS+" für Wettbewerbsjahr "+j+" (Wanderruderstatistik) erstellt.");
        } else {
          Logger.log(Logger.INFO,"Meldungen-Indexdatei "+mdatFA+" für Wettbewerbsjahr "+j+" (Wanderruderstatistik) geöffnet.");
        }

      } catch(Exception ee) {
        Dialog.error("Es ist ein Fehler aufgetreten: "+e.toString());
        return;
      }
      Logger.log(Logger.INFO,"Neues Wettbewerbsjahr "+j+" ausgewählt.");
    } else {
      // vorhandenes Jahr
      MeldungenIndex mIndex;
      mIndex = new MeldungenIndex(mdatFA);
      if (!mIndex.readFile()) {
        Dialog.error("Die Datei\n"+mIndex.getFileName()+"\n konnte nicht geöffnet werden!");
        return;
      }
      mIndex = new MeldungenIndex(mdatWS);
      if (!mIndex.readFile()) {
        Dialog.error("Die Datei\n"+mIndex.getFileName()+"\n konnte nicht geöffnet werden!");
        return;
      }
      Logger.log(Logger.INFO,"Vorhandenes Wettbewerbsjahr "+j+" ausgewählt.");
    }
    Main.drvConfig.aktJahr = j;
    Main.drvConfig.writeFile();
    Dialog.infoDialog("Wettbewerbsjahr ausgewählt","Das ausgewählte Jahr für die Erfassung von Meldungen ist jetzt "+j+".");
  }

  void configButton_actionPerformed(ActionEvent e) {
    DRVConfigFrame dlg = new DRVConfigFrame(this);
    Dialog.setDlgLocation(dlg,this);
    dlg.setModal(true);
    dlg.show();

  }

  void keysButton_actionPerformed(ActionEvent e) {
    KeysAdminFrame dlg;
    try {
      dlg = new KeysAdminFrame(this);
    } catch(Exception ee) {
      return;
    }
    Dialog.setDlgLocation(dlg,this);
    dlg.setModal(true);
    dlg.show();
  }

  void closeButton_actionPerformed(ActionEvent e) {
    cancel();
  }

  void updateButton_actionPerformed(ActionEvent e) {
    OnlineUpdate.runOnlineUpdate(this,Daten.ONLINEUPDATE_INFO_DRV);
  }

  void datensicherungButton_actionPerformed(ActionEvent e) {
    DatensicherungFrame dlg;
    try {
      Vector directories = new Vector();
      Vector selected = new Vector();
      Vector inclSubdirs = new Vector();
      directories.add(Daten.efaMainDirectory); selected.add(new Boolean(true)); inclSubdirs.add(new Boolean(false));
      directories.add(Daten.efaDataDirectory); selected.add(new Boolean(true)); inclSubdirs.add(new Boolean(false));
      if (Main.drvConfig.aktJahr != 0) {
        directories.add(Daten.efaDataDirectory+Main.drvConfig.aktJahr+Daten.fileSep); selected.add(new Boolean(true)); inclSubdirs.add(new Boolean(true));
      }
      if ((new File(Daten.efaDataDirectory+"CA"+Daten.fileSep)).isDirectory()) {
        directories.add(Daten.efaDataDirectory+"CA"+Daten.fileSep); selected.add(new Boolean(true)); inclSubdirs.add(new Boolean(true));
      }
      directories.add(Daten.efaCfgDirectory); selected.add(new Boolean(true)); inclSubdirs.add(new Boolean(true));
      dlg = new DatensicherungFrame(this,directories,inclSubdirs,selected);
    } catch(Exception ee) {
      return;
    }
    Dialog.setDlgLocation(dlg,this);
    dlg.setModal(true);
    dlg.show();
  }


}
