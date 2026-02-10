/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core;

import de.nmichael.efa.*;
import de.nmichael.efa.util.Mnemonics;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.ActionHandler;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete

public class ExceptionFrame extends JDialog implements ActionListener {
  String error;
  String stacktrace;
  BorderLayout borderLayout1 = new BorderLayout();
  JButton jButton1 = new JButton();
  JPanel jPanel1 = new JPanel();
  JLabel jLabel1 = new JLabel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JLabel errorLabel = new JLabel();
  JScrollPane jScrollPane1 = new JScrollPane();
  JTextArea infotext = new JTextArea();
  JTextArea errortext = new JTextArea();

  public ExceptionFrame(JDialog frame, String error, String stacktrace) {
    super(frame);
    ini(error,stacktrace);
  }
  public ExceptionFrame(JFrame frame, String error, String stacktrace) {
    super(frame);
    ini(error,stacktrace);
  }
  public ExceptionFrame(String error, String stacktrace) {
    ini(error,stacktrace);
  }

  void ini(String error, String stacktrace) {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    this.error = error;
    this.stacktrace = stacktrace;
    try {
      jbInit();
    }
    catch(Exception e) {
    }
    EfaUtil.pack(this);
    jButton1.requestFocus();
  }


  // ActionHandler Events
  public void keyAction(ActionEvent evt) {
    if (evt == null || evt.getActionCommand() == null) return;
    if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
      cancel();
    }
  }


  private void jbInit() throws Exception {
    ActionHandler ah= new ActionHandler(this);
    try {
      ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                       new String[] {"ESCAPE","F1"}, new String[] {"keyAction","keyAction"});
    } catch(NoSuchMethodException e) {
      System.err.println("Error setting up ActionHandler");
    }

    Mnemonics.setButton(this, jButton1, International.getStringWithMnemonic("Schließen"));
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jButton1_actionPerformed(e);
      }
    });
    this.getContentPane().setLayout(borderLayout1);
    jPanel1.setLayout(gridBagLayout1);
    jLabel1.setText(International.getString("Ein unerwarteter Programmfehler ist aufgetreten!"));
    errorLabel.setForeground(Color.red);
    errorLabel.setText(error);
    String logfile = Daten.efaLogfile;
    if (logfile != null && Daten.fileSep != null) {
      int pos = logfile.lastIndexOf(Daten.fileSep);
      if (pos>0 && pos+1<logfile.length()) logfile = logfile.substring(pos+1,logfile.length());
      logfile = logfile.toUpperCase();
    }
    infotext.append(Dialog.chopDialogString(International.getMessage("Ein Fehlerprotokoll wurde in '{logfile}' erstellt. " +
            "Damit dieser Fehler korrigiert werden kann, schicke bitte eine email " +
            "mit einer kurzen Beschreibung dessen, was diesen Fehler ausgelöst hat, " +
            "an {email}. Kopiere bitte zusätzlich folgende Informationen " +
            "in die email: -- Danke!", Daten.efaLogfile, Daten.EMAILBUGS)));
    infotext.setForeground(Color.blue);
    errortext.append("#####################################################\n# " +
            International.getString("Unerwarteter Programmfehler")+"!\n# " +
            International.getMessage("Bitte per email an {email} schicken!",Daten.EMAILBUGS) +
            "\n#####################################################\n\n");
    errortext.append(International.getString("Fehler-Information")+":\n============================================\n");
    errortext.append(International.getString("Fehlermeldung")+": "+error+"\n");
    errortext.append(stacktrace);
    errortext.append("\n\n");
    errortext.append(International.getString("Programm-Information")+":\n============================================\n");
    Vector info = Daten.getEfaInfos();
    for (int i=0; info != null && i<info.size(); i++) errortext.append((String)info.get(i)+"\n");
    this.setTitle(International.getString("Unerwarteter Programmfehler"));
    jScrollPane1.setPreferredSize(new Dimension(200, 200));
    this.getContentPane().add(jButton1, BorderLayout.SOUTH);
    this.getContentPane().add(jPanel1, BorderLayout.CENTER);
    jPanel1.add(jLabel1,     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(errorLabel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(infotext,   new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    jPanel1.add(jScrollPane1,   new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    jScrollPane1.getViewport().add(errortext, null);
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
    dispose();
  }

  /**Close the dialog on a button event*/
  public void actionPerformed(ActionEvent e) {
  }


  void jButton1_actionPerformed(ActionEvent e) {
    cancel();
  }


}
