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

import de.nmichael.efa.data.efawett.EfaWettMeldung;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.beans.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class WSFahrtenUebersichtFrame extends JDialog implements ActionListener {
  JDialog parent;
  Vector data;
  private int result = -1;
  boolean firstclick=false;

  JPanel jPanel1 = new JPanel();
  BorderLayout borderLayout1 = new BorderLayout();
  JButton closeButton = new JButton();
  JPanel jPanel2 = new JPanel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JButton editButton = new JButton();
  JScrollPane scrollPane = new JScrollPane();
  JTable table;


  public WSFahrtenUebersichtFrame(JDialog parent, Vector data) {
    super(parent);
    this.data = data;
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    Dialog.frameOpened(this);
    try {
      jbInit();
      initTable();
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
      cancel(-1);
    }
  }


  // Initialisierung des Frames
  private void jbInit() throws Exception {
    ActionHandler ah= new ActionHandler(this);
    try {
      ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                       new String[] {"ESCAPE","F1"}, new String[] {"keyAction","keyAction"});
      jPanel1.setLayout(borderLayout1);
      closeButton.setText("Schließen");
      closeButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          closeButton_actionPerformed(e);
        }
    });
      jPanel2.setLayout(gridBagLayout1);
      editButton.setText("Bearbeiten");
      editButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editButton_actionPerformed(e);
        }
    });
      this.setTitle("Übersicht der Wanderfahrten");
      scrollPane.setPreferredSize(new Dimension(500, 400));
      this.getContentPane().add(jPanel1, BorderLayout.CENTER);
      jPanel1.add(closeButton, BorderLayout.SOUTH);
      jPanel1.add(jPanel2, BorderLayout.EAST);
      jPanel2.add(editButton,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      jPanel1.add(scrollPane, BorderLayout.CENTER);
    } catch(NoSuchMethodException e) {
      System.err.println("Error setting up ActionHandler");
    }
  }

  /**Overridden so we can exit when window is closed*/
  protected void processWindowEvent(WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      cancel(-1);
    }
    super.processWindowEvent(e);
  }

  /**Close the dialog*/
  void cancel(int result) {
    this.result = result;
    Dialog.frameClosed(this);
    dispose();
  }

  /**Close the dialog on a button event*/
  public void actionPerformed(ActionEvent e) {
  }

  private void initTable() {
    String[] title = new String[3];
    String[][] content = new String[data.size()][3];
    title[0] = "Fahrt";
    title[1] = "Tage";
    title[2] = "Kilometer";
    for (int i=0; i<data.size(); i++) {
      EfaWettMeldung ewm = (EfaWettMeldung)data.get(i);
      content[i][0] = ewm.drvWS_StartZiel;
      content[i][1] = ewm.drvWS_Tage;
      content[i][2] = ewm.drvWS_Km;
    }
    table = new JTable(content,title);
    table.getColumnModel().getColumn(0).setPreferredWidth(350);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    scrollPane.getViewport().add(table, null);
    table.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        table_mouseClicked(e);
      }
    });
    table.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        table_propertyChange(e);
      }
    });
  }

  // Edit bei Doppelklick
  void table_mouseClicked(MouseEvent e) {
    firstclick=true;
  }

  // komisch, manchmal scheine diese Methode irgendwie nicht zu ziehen.....
  void table_propertyChange(PropertyChangeEvent e) {
    if (table.isEditing()) {
      if (firstclick) {
        firstclick=false;
        editButton_actionPerformed(null);
      }
    }
  }

  void closeButton_actionPerformed(ActionEvent e) {
    cancel(-1);
  }

  void editButton_actionPerformed(ActionEvent e) {
    int selected = table.getSelectedRow();
    if (selected < 0) {
      Dialog.error("Bitte wähle zuerst eine Fahrt aus!");
      return;
    }
    cancel(selected);
  }

  public int getResult() {
    return result;
  }


}
