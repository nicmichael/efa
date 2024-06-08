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

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class EmilConfigFrame extends JDialog implements ActionListener {
  
  private static int FIELD_HEIGHT = 24;
  EmilFrame parent;
  BorderLayout borderLayout1 = new BorderLayout();
  JButton saveButton = new JButton();
  JPanel jPanel1 = new JPanel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JLabel jLabel1 = new JLabel();
  JTextField dir_efw = new JTextField();
  JButton efwdirButton = new JButton();
  JButton cvsdirButton = new JButton();
  JButton stdcvsButton = new JButton();
  JTextField dir_csv = new JTextField();
  JTextField std_csv = new JTextField();
  JLabel jLabel2 = new JLabel();
  JLabel jLabel3 = new JLabel();
  JCheckBox nurExportErfuellt = new JCheckBox();


  public EmilConfigFrame(EmilFrame parent) {
    super(parent);
    this.parent = parent;
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    EfaUtil.pack(this);
    ini();
    saveButton.requestFocus();
  }


  // ActionHandler Events
  public void keyAction(ActionEvent evt) {
    if (evt == null || evt.getActionCommand() == null) return;
    if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
      cancel();
    }
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

    saveButton.setNextFocusableComponent(dir_efw);
    saveButton.setActionCommand("saveButton");
    saveButton.setMnemonic('S');
    saveButton.setText("Speichern");
    saveButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveButton_actionPerformed(e);
      }
    });
    this.getContentPane().setLayout(borderLayout1);
    jPanel1.setLayout(gridBagLayout1);
    jLabel1.setDisplayedMnemonic('E');
    jLabel1.setLabelFor(dir_efw);
    jLabel1.setText("EFW Verzeichnis: ");
    efwdirButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        efwdirButton_actionPerformed(e);
      }
    });
    efwdirButton.setMinimumSize(new Dimension(59, 25));
    efwdirButton.setNextFocusableComponent(dir_csv);
    efwdirButton.setPreferredSize(new Dimension(59, 25));
    efwdirButton.setIcon(new ImageIcon(EmilConfigFrame.class.getResource("/de/nmichael/efa/img/prog_open.gif")));
    cvsdirButton.setMinimumSize(new Dimension(59, 25));
    cvsdirButton.setNextFocusableComponent(std_csv);
    cvsdirButton.setPreferredSize(new Dimension(59, 25));
    cvsdirButton.setToolTipText("");
    cvsdirButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cvsdirButton_actionPerformed(e);
      }
    });
    cvsdirButton.setIcon(new ImageIcon(EmilConfigFrame.class.getResource("/de/nmichael/efa/img/prog_open.gif")));
    stdcvsButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        stdcvsButton_actionPerformed(e);
      }
    });
    stdcvsButton.setMinimumSize(new Dimension(59, 25));
    stdcvsButton.setNextFocusableComponent(nurExportErfuellt);
    stdcvsButton.setPreferredSize(new Dimension(59, 25));
    stdcvsButton.setIcon(new ImageIcon(EmilConfigFrame.class.getResource("/de/nmichael/efa/img/prog_save.gif")));
    jLabel2.setDisplayedMnemonic('C');
    jLabel2.setLabelFor(dir_csv);
    jLabel2.setText("CSV-Verzeichnis: ");
    jLabel3.setDisplayedMnemonic('T');
    jLabel3.setLabelFor(std_csv);
    jLabel3.setText("Standard CSV-Datei: ");
    std_csv.setNextFocusableComponent(stdcvsButton);
    std_csv.setPreferredSize(new Dimension(400, FIELD_HEIGHT));
    dir_csv.setNextFocusableComponent(cvsdirButton);
    dir_csv.setPreferredSize(new Dimension(400, FIELD_HEIGHT));
    dir_efw.setNextFocusableComponent(efwdirButton);
    dir_efw.setPreferredSize(new Dimension(400, FIELD_HEIGHT));
    nurExportErfuellt.setNextFocusableComponent(saveButton);
    nurExportErfuellt.setText("beim Export nur Teilnehmer exportieren, die die Bedingungen erfüllt " +
    "haben");
    this.setTitle("emil Einstellungen");
    this.getContentPane().add(saveButton, BorderLayout.SOUTH);
    this.getContentPane().add(jPanel1, BorderLayout.CENTER);
    jPanel1.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(dir_efw, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(efwdirButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(cvsdirButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(stdcvsButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(dir_csv, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(std_csv, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(jLabel2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(jLabel3, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(nurExportErfuellt, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
  }

  void ini() {
    dir_efw.setText(parent.cfg.getDirEfw());
    dir_csv.setText(parent.cfg.getDirCsv());
    std_csv.setText(parent.cfg.getStdCsv());
    nurExportErfuellt.setSelected(parent.cfg.getExportNurErfuellt());
  }

  /**Overridden so we can exit when window is closed*/
  protected void processWindowEvent(WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      cancel();
    }
    super.processWindowEvent(e);
  }

  public void actionPerformed(ActionEvent e) {}

  /**Close the dialog*/
  void cancel() {
    dispose();
  }

  void stdcvsButton_actionPerformed(ActionEvent e) {
    String dir = parent.cfg.getDirCsv();
    if (dir.equals("") || !new File(dir).isDirectory()) dir = System.getProperty("user.dir");

    String dat = Dialog.dateiDialog(this,"Standard CSV-Datei","CSV-Dateien (*.csv)","csv",dir,true);
    if (dat != null) this.std_csv.setText(dat);
  }

  void efwdirButton_actionPerformed(ActionEvent e) {
    String dir = parent.cfg.getDirEfw();
    if (dir.equals("") || !new File(dir).isDirectory()) dir = System.getProperty("user.dir");

    String dat = Dialog.dateiDialog(this,"EFW-Verzeichnis",null,null,dir,null,null,false,true);
    if (dat != null) {
      if (! new File(dat).isDirectory()) {
        int pos = dat.lastIndexOf(Daten.fileSep);
        if (pos>0) dat = dat.substring(0,pos);
      }
      if (!dat.endsWith(Daten.fileSep)) dat += Daten.fileSep;
      this.dir_efw.setText(dat);
    }
  }

  void cvsdirButton_actionPerformed(ActionEvent e) {
    String dir = parent.cfg.getDirCsv();
    if (dir.equals("") || !new File(dir).isDirectory()) dir = System.getProperty("user.dir");

    String dat = Dialog.dateiDialog(this,"CSV-Verzeichnis",null,null,dir,null,null,false,true);
    if (dat != null) {
      if (! new File(dat).isDirectory()) {
        int pos = dat.lastIndexOf(Daten.fileSep);
        if (pos>0) dat = dat.substring(0,pos);
      }
      if (!dat.endsWith(Daten.fileSep)) dat += Daten.fileSep;
      this.dir_csv.setText(dat);
    }
  }

  void saveButton_actionPerformed(ActionEvent e) {
    parent.cfg.setDirEfw(dir_efw.getText().trim());
    parent.cfg.setDirCsv(dir_csv.getText().trim());
    parent.cfg.setStdCsv(std_csv.getText().trim());
    parent.cfg.setExportNurErfuellt(nurExportErfuellt.isSelected());
    if (!parent.cfg.writeFile())
      Dialog.infoDialog("Fehler","Datei\n'"+parent.cfg.getFilename()+"'\nkonnte nicht geschrieben werden!");
    else cancel();
  }


}