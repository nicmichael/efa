package de.nmichael.efa.drv;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.ActionHandler;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.io.File;

/**
 * Title:        efa - Elektronisches Fahrtenbuch
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author Nicolas Michael
 * @version 1.0
 */

public class DatensicherungFrame extends JDialog implements ActionListener {
  Vector directories;
  Vector inclSubdirs;
  Vector selected;

  JPanel jPanel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JButton startButton = new JButton();
  JPanel jPanel2 = new JPanel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JLabel jLabel1 = new JLabel();
  JScrollPane scrollPane = new JScrollPane();
  JTable directoryTable;
  JLabel jLabel2 = new JLabel();
  JRadioButton efaBackupRadioButton = new JRadioButton();
  JRadioButton belVerzRadioButton = new JRadioButton();
  JTextField backupVerzeichnis = new JTextField();
  ButtonGroup buttonGroup = new ButtonGroup();
  JButton dirSelectButton = new JButton();


  public DatensicherungFrame(JDialog parent, Vector directories, Vector inclSubdirs, Vector selected) {
    super(parent);
    constructor(directories,inclSubdirs,selected);
  }

  public DatensicherungFrame(JFrame parent, Vector directories, Vector inclSubdirs, Vector selected) {
    super(parent);
    constructor(directories,inclSubdirs,selected);
  }

  private void constructor(Vector directories, Vector inclSubdirs, Vector selected) {
    this.directories = directories;
    this.inclSubdirs = inclSubdirs;
    this.selected = selected;
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    Dialog.frameOpened(this);
    try {
      jbInit();
      createTable();
      efaBackupRadioButton.setText(Daten.efaBakDirectory);
      efaBackupRadioButton.setSelected(true);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    EfaUtil.pack(this);
    startButton.requestFocus();
  }


  // ActionHandler Events
  public void keyAction(ActionEvent evt) {
    if (evt == null || evt.getActionCommand() == null) return;
    if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
      cancel();
    }
    if (evt.getActionCommand().equals("KEYSTROKE_ACTION_1")) { // F1
      // Help.getHelp(this,this.getClass());
    }
  }


  // Initialisierung des Frames
  private void jbInit() throws Exception {
    ActionHandler ah= new ActionHandler(this);
    try {
      ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                       new String[] {"ESCAPE","F1"}, new String[] {"keyAction","keyAction"});
      jPanel1.setLayout(borderLayout1);
      startButton.setNextFocusableComponent(efaBackupRadioButton);
      startButton.setMnemonic('D');
      startButton.setText("Datensicherung starten");
      startButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          startButton_actionPerformed(e);
        }
    });
      jPanel2.setLayout(gridBagLayout1);
      jLabel1.setText("Zu sichernde Verzeichnisse:");
      scrollPane.setPreferredSize(new Dimension(750, 100));
      this.setTitle("Datensicherung");
      jLabel2.setText("Sicherungsdatei erstellen in:");
      efaBackupRadioButton.setNextFocusableComponent(belVerzRadioButton);
      efaBackupRadioButton.setText("...efaBackupDir...");
      belVerzRadioButton.setNextFocusableComponent(backupVerzeichnis);
      belVerzRadioButton.setText("anderes Verzeichnis: ");
      backupVerzeichnis.setNextFocusableComponent(dirSelectButton);
      backupVerzeichnis.setPreferredSize(new Dimension(500, 17));
      backupVerzeichnis.setText("A:\\");
      dirSelectButton.setNextFocusableComponent(startButton);
      dirSelectButton.setPreferredSize(new Dimension(59, 21));
      dirSelectButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          dirSelectButton_actionPerformed(e);
        }
    });
      buttonGroup.add(efaBackupRadioButton);
      buttonGroup.add(belVerzRadioButton);
      this.getContentPane().add(jPanel1, BorderLayout.CENTER);
      jPanel1.add(startButton, BorderLayout.SOUTH);
      jPanel1.add(jPanel2, BorderLayout.CENTER);
      jPanel2.add(jLabel1,    new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      jPanel2.add(scrollPane,     new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
      jPanel2.add(jLabel2,     new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(15, 0, 0, 0), 0, 0));
      jPanel2.add(efaBackupRadioButton,     new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      jPanel2.add(belVerzRadioButton,   new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      jPanel2.add(backupVerzeichnis,   new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
      jPanel2.add(dirSelectButton,   new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      dirSelectButton.setIcon(new ImageIcon(DatensicherungFrame.class.getResource("/de/nmichael/efa/img/prog_open.gif")));
    } catch(NoSuchMethodException e) {
      System.err.println("Error setting up ActionHandler");
    }
  }

  private void createTable() {
    Object[] title = new Object[3];
    title[0] = "Sichern";
    title[1] = "Verzeichnis";
    title[2] = "Unterverzeichnisse";

    Object[][] verzeichnisse = new Object[directories.size()][3];
    for (int i=0; i<directories.size(); i++) {
      JCheckBox sichernCheck = new JCheckBox();
      sichernCheck.setText("Sichern");
      sichernCheck.setSelected(((Boolean)selected.get(i)).booleanValue());
      verzeichnisse[i][0] = sichernCheck;

      verzeichnisse[i][1] = directories.get(i);

      JCheckBox subdirCheck = new JCheckBox();
      subdirCheck.setText("mit Unterverzeichnissen");
      subdirCheck.setSelected(((Boolean)inclSubdirs.get(i)).booleanValue());
      verzeichnisse[i][2] = subdirCheck;
    }

    directoryTable = new JTable(verzeichnisse,title);
    directoryTable.getColumn("Sichern").setCellRenderer(new CheckboxInTableRenderer());
    directoryTable.getColumn("Sichern").setCellEditor(new CheckboxEditor(new JCheckBox()));
    directoryTable.getColumn("Unterverzeichnisse").setCellRenderer(new CheckboxInTableRenderer());
    directoryTable.getColumn("Unterverzeichnisse").setCellEditor(new CheckboxEditor(new JCheckBox()));
    directoryTable.getColumnModel().getColumn(0).setPreferredWidth(100);
    directoryTable.getColumnModel().getColumn(1).setPreferredWidth(450);
    directoryTable.getColumnModel().getColumn(2).setPreferredWidth(200);
    scrollPane.getViewport().add(directoryTable, null);
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

  void dirSelectButton_actionPerformed(ActionEvent e) {
    String dir = backupVerzeichnis.getText().trim();
    if (dir.length() == 0 || !new File(dir).isDirectory()) dir = Daten.efaMainDirectory;
    dir = Dialog.dateiDialog(this,"Backup-Verzeichnis ausw�hlen",null,null,dir,null,"ausw�hlen",false,true);
    if (dir != null) backupVerzeichnis.setText(dir);
  }

  void startButton_actionPerformed(ActionEvent e) {
    Vector dirs = new Vector();
    Vector inclSubdirs = new Vector();
    for (int i=0; i<directories.size(); i++) {
      if (((JCheckBox)directoryTable.getValueAt(i,0)).isSelected()) {
        dirs.add(directories.get(i));
        inclSubdirs.add(new Boolean(((JCheckBox)directoryTable.getValueAt(i,2)).isSelected()));
      }
    }
    String zipdir = (efaBackupRadioButton.isSelected() ? Daten.efaBakDirectory : backupVerzeichnis.getText().trim());
    if (!(new File(zipdir)).isDirectory()) {
      Dialog.error("Backup-Verzeichnis "+zipdir+" nicht gefunden!");
      return;
    }
    String zipfile = zipdir + (zipdir.endsWith(Daten.fileSep) ? "" : Daten.fileSep)
                     + "Sicherung_" + EfaUtil.getCurrentTimeStampYYYYMMDD_HHMMSS() + ".zip";


    String result = EfaUtil.createZipArchive(dirs,inclSubdirs,zipfile);
    if (result == null) {
      Dialog.infoDialog("Daten erfolgreich in der Datei\n"+zipfile+"\ngesichert.");
      cancel();
    } else {
      Dialog.error("Bei der Datensicherung trat ein Fehler auf:\n"+result);
    }
  }



}

class CheckboxInTableRenderer implements TableCellRenderer {
  public Component getTableCellRendererComponent(JTable table, Object value,
                   boolean isSelected, boolean hasFocus, int row, int column) {
    try {
      if (value==null) return null;
      return (Component)value;
    } catch(Exception e) { return null; }
  }
}

class CheckboxEditor extends DefaultCellEditor implements ItemListener {
  private JCheckBox button;

  public CheckboxEditor(JCheckBox checkBox) {
    super(checkBox);
  }

  public Component getTableCellEditorComponent(JTable table, Object value,
                   boolean isSelected, int row, int column) {
    if (value==null) return null;
    button = (JCheckBox)value;
    button.addItemListener(this);
    return (Component)value;
  }

  public Object getCellEditorValue() {
    button.removeItemListener(this);
    return button;
  }

  public void itemStateChanged(ItemEvent e) {
    super.fireEditingStopped();
  }
}
