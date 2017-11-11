/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.elwiz;

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import de.nmichael.efa.*;

// @i18n complete

public class ElwizFrame extends JFrame {
  Vector optionsHTML, optionsPDF;
  Hashtable colorTextfields = new Hashtable();
  Hashtable colorButtons = new Hashtable();
  Hashtable textfieldType = new Hashtable();

  JPanel contentPane;
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel optionHTMLPanel = new JPanel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JButton createButton = new JButton();
  JTabbedPane jTabbedPane1 = new JTabbedPane();
  JScrollPane jScrollPane1 = new JScrollPane();
  JScrollPane jScrollPane2 = new JScrollPane();
  JPanel optionPDFPanel = new JPanel();
  GridBagLayout gridBagLayout2 = new GridBagLayout();
  JMenuBar jMenuBar1 = new JMenuBar();
  JMenu jMenu1 = new JMenu();
  JMenuItem jMenuItem1 = new JMenuItem();
  JMenuItem jMenuItem2 = new JMenuItem();
  JMenu jMenu2 = new JMenu();
  JMenuItem jMenuItem3 = new JMenuItem();
  JMenuItem jMenuItem4 = new JMenuItem();

  //Construct the frame
  public ElwizFrame() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    optionsHTML = XSLTReader.run(Daten.efaFormattingDirectory+"layout"+Daten.fileSep+"html"+Daten.fileSep+"elwiz.xml");
    if (optionsHTML == null) {
      Dialog.error("Es konnte kein XML-Parser geladen werden.");
      Daten.haltProgram(Daten.HALT_MISCONFIG);
    }
    optionsPDF  = XSLTReader.run(Daten.efaFormattingDirectory+"layout"+Daten.fileSep+"pdf"+Daten.fileSep+"elwiz.xml");
    if (optionsPDF == null) {
      Dialog.error("Es konnte kein XML-Parser geladen werden.");
      Daten.haltProgram(Daten.HALT_MISCONFIG);
    }
    iniItems(optionsHTML,optionHTMLPanel);
    iniItems(optionsPDF,optionPDFPanel);
    this.optionHTMLPanel.requestFocus();
  }


  // ActionHandler Events
  public void keyAction(ActionEvent evt) {
    if (evt == null || evt.getActionCommand() == null) return;
//    if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
      // nothing
//    }
  }


  //Component initialization
  private void jbInit() throws Exception  {
    ActionHandler ah= new ActionHandler(this);
    try {
      ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                       new String[] {"ESCAPE","F1"}, new String[] {"keyAction","keyAction"});
    } catch(NoSuchMethodException e) {
      System.err.println("Error setting up ActionHandler");
    }

    setIconImage(Toolkit.getDefaultToolkit().createImage(ElwizFrame.class.getResource("/de/nmichael/efa/img/efa_icon.gif")));
    contentPane = (JPanel) this.getContentPane();
    contentPane.setLayout(borderLayout1);
    this.setSize(new Dimension(600, 600));
    this.setTitle("elwiz - efa Layout Wizard");
    optionHTMLPanel.setLayout(gridBagLayout1);
    Mnemonics.setButton(this, createButton, International.getStringWithMnemonic("Stylesheet erstellen"));
    createButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        createButton_actionPerformed(e);
      }
    });
    optionPDFPanel.setLayout(gridBagLayout2);
    Mnemonics.setButton(this, jMenu1, International.getStringWithMnemonic("Datei"));
    Mnemonics.setButton(this, jMenuItem1, International.getStringWithMnemonic("Stylesheet erstellen"));
    jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuItem1_actionPerformed(e);
      }
    });
    Mnemonics.setButton(this, jMenuItem2, International.getStringWithMnemonic("Beenden"));
    jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuItem2_actionPerformed(e);
      }
    });
    Mnemonics.setButton(this, jMenu2, International.getStringWithMnemonic("Info"));
    Mnemonics.setButton(this, jMenuItem3, International.getStringWithMnemonic("Über"));
    jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuItem3_actionPerformed(e);
      }
    });
    Mnemonics.setButton(this, jMenuItem4, International.getStringWithMnemonic("Hilfe"));
    jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuItem4_actionPerformed(e);
      }
    });
    contentPane.add(createButton,  BorderLayout.SOUTH);
    contentPane.add(jTabbedPane1,  BorderLayout.CENTER);
    jTabbedPane1.add(jScrollPane1,  "HTML");
    jTabbedPane1.add(jScrollPane2,  "PDF");
    jScrollPane2.getViewport().add(optionPDFPanel, null);
//    jTabbedPane1.add(optionPanel,  "HTML");
    jScrollPane1.getViewport().add(optionHTMLPanel, null);
    jMenuBar1.add(jMenu1);
    jMenuBar1.add(jMenu2);
    jMenu1.add(jMenuItem1);
    jMenu1.add(jMenuItem2);
    jMenu2.add(jMenuItem4);
    jMenu2.add(jMenuItem3);
    this.setJMenuBar(jMenuBar1);
  }
  //Overridden so we can exit when window is closed
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      cancel();
    }
  }

  void cancel() {
    Daten.haltProgram(0);;
  }



  void iniItems(Vector options, JPanel optionPanel) {
    int y=0;

    for (int i=0; i<options.size(); i++) {
      ElwizOption o = (ElwizOption)options.get(i);
      switch (o.type) {
        case ElwizOption.O_SELECT:
          JLabel label1 = new JLabel();
          label1.setText(o.descr);
          optionPanel.add(label1, new GridBagConstraints(0, y++, 3, 1, 0.0, 0.0
                  ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
          ButtonGroup butgr = new ButtonGroup();
          if (o.options != null)
            for (int j=0; j<o.options.size(); j++) {
              ElwizSingleOption eso = (ElwizSingleOption)o.options.get(j);
              JRadioButton radio = new JRadioButton();
              radio.setText(eso.descr);
              radio.setSelected(eso.selected);

              optionPanel.add(radio, new GridBagConstraints(0, y++, 3, 1, 0.0, 0.0
                      ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 50, 0, 0), 0, 0));

              butgr.add(radio);

              if (o.components == null) o.components = new Vector();
              o.components.add(radio);
            }
          break;

        case ElwizOption.O_OPTIONAL:
          JLabel label2 = new JLabel();
          label2.setText(o.descr);
          optionPanel.add(label2, new GridBagConstraints(0, y++, 3, 1, 0.0, 0.0
                  ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
          if (o.options != null)
            for (int j=0; j<o.options.size(); j++) {
              ElwizSingleOption eso = (ElwizSingleOption)o.options.get(j);
              JCheckBox check = new JCheckBox();
              check.setText(eso.descr);
              check.setSelected(eso.selected);

              optionPanel.add(check, new GridBagConstraints(0, y++, 3, 1, 0.0, 0.0
                      ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 50, 0, 0), 0, 0));

              if (o.components == null) o.components = new Vector();
              o.components.add(check);
            }
          break;

        case ElwizOption.O_VALUE:
          JLabel label3 = new JLabel();
          label3.setText(o.descr);
          optionPanel.add(label3, new GridBagConstraints(0, y++, 3, 1, 0.0, 0.0
                  ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
          if (o.options != null)
            for (int j=0; j<o.options.size(); j++) {
              ElwizSingleOption eso = (ElwizSingleOption)o.options.get(j);
              JLabel label3a = new JLabel();
              label3a.setText(eso.descr+": ");
              JTextField textfield = new JTextField();
              textfield.setText(eso.value);
              textfield.setPreferredSize(new Dimension(200,20));
              textfield.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusLost(FocusEvent e) {
                  textfield_focusLost(e);
                }
              });
              textfieldType.put(textfield,eso.type);

              optionPanel.add(label3a, new GridBagConstraints(0, y, 1, 1, 0.0, 0.0
                      ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 50, 0, 0), 0, 0));
              optionPanel.add(textfield, new GridBagConstraints(1, y++, 1, 1, 0.0, 0.0
                      ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

              // Color-Textfield
              if (eso.type != null && eso.type.equals("color")) {
                JButton button = new JButton();
                button.setText("Farbe");
                button.setPreferredSize(new Dimension(80,20));
                colorTextfields.put(button,textfield);
                colorButtons.put(textfield,button);
                try {
                  String cstr = textfield.getText().trim();
                  while (cstr.length()<6) cstr += "0";
                  button.setBackground(new Color(Integer.parseInt(cstr.substring(0,6),16)));
                } catch(Exception ee) { }
                optionPanel.add(button, new GridBagConstraints(2, y-1, 1, 1, 0.0, 0.0
                        ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                button.addActionListener(new java.awt.event.ActionListener() {
                  public void actionPerformed(ActionEvent e) {
                    colorButton_actionPerformed(e);
                  }
                });
              }


              if (o.components == null) o.components = new Vector();
              o.components.add(textfield);
            }
          break;

      }
    }

  }

  void colorButton_actionPerformed(ActionEvent e) {
    JTextField textfield = (JTextField)colorTextfields.get(e.getSource());
    String cstr = textfield.getText().trim();
    while (cstr.length()<6) cstr += "0";
    int cint;
    try {
      cint = Integer.parseInt(cstr.substring(0,6),16);
    } catch(Exception ee) { cint = 0; }
    Color color = JColorChooser.showDialog(this,
            International.getMessage("{item} auswählen",
            International.getString("Farbe")),
            new Color(cint));
    if (color != null) {
      cstr = "";
      float[] rgb = color.getRGBColorComponents(null);
      for (int i=0; i<rgb.length; i++)
        cstr += EfaUtil.hexByte((int)(rgb[i]*255));
      textfield.setText(cstr);
      ((JButton)e.getSource()).setBackground(color);
    }
  }

  void textfield_focusLost(FocusEvent e) {
    JTextField textfield = (JTextField)e.getSource();
    if (textfieldType.get(textfield) == null) return;
    String type = (String)textfieldType.get(textfield);

    if (type.equals("string")) {
      String str = textfield.getText().trim();
      str = EfaUtil.replace(str,"<","",true);
      str = EfaUtil.replace(str,">","",true);
      str = EfaUtil.replace(str,"\"","'",true);
      textfield.setText(str);
      return;
    }

    if (type.equals("integer")) {
      textfield.setText(Integer.toString(EfaUtil.string2date(textfield.getText().trim(),0,0,0).tag));
      return;
    }

    if (type.equals("real")) {
      TMJ tmj = EfaUtil.string2date(textfield.getText().trim(),0,0,0);
      String str = Integer.toString(tmj.tag);
      if (tmj.monat != 0) str += "."+tmj.monat;
      textfield.setText(str);
      return;
    }

    if (type.equals("color")) {
      if (colorButtons.get(textfield) == null) return;
      JButton button = (JButton)colorButtons.get(textfield);
      String cstr = textfield.getText().trim();
      while (cstr.length()<6) cstr += "0";
      int cint;
      try {
        cint = Integer.parseInt(cstr.substring(0,6),16);
        Color color = new Color(cint);
        cstr = "";
        float[] rgb = color.getRGBColorComponents(null);
        for (int i=0; i<rgb.length; i++)
          cstr += EfaUtil.hexByte((int)(rgb[i]*255));
        textfield.setText(cstr);
        button.setBackground(color);
      } catch(Exception ee) {
        button.setBackground(new Color(0));
        textfield.setText("000000");
      }
    }
    return;
  }

  void createButton_actionPerformed(ActionEvent e) {
    String infile;
    String outfile;
    if (optionHTMLPanel.isShowing()) {
      infile = Daten.efaFormattingDirectory+"layout/html/elwiz.xml";
      outfile = Dialog.dateiDialog(this,
              International.getString("HTML-Layout erstellen"),
              "XSLT-Stylesheets (*.xsl)","xsl",Daten.efaFormattingDirectory+"layout/html/",true);
      if (outfile != null) XSLTWriter.run(infile,outfile,optionsHTML);
    } else {
      infile = Daten.efaFormattingDirectory+"layout/pdf/elwiz.xml";
      outfile = Dialog.dateiDialog(this,
              International.getString("PDF-Layout erstellen"),
              "XSLT-Stylesheets (*.xsl)","xsl",Daten.efaFormattingDirectory+"layout/pdf/",true);
      if (outfile != null) XSLTWriter.run(infile,outfile,optionsPDF);
    }
  }

  void jMenuItem1_actionPerformed(ActionEvent e) {
    this.createButton_actionPerformed(null);
  }

  void jMenuItem2_actionPerformed(ActionEvent e) {
    cancel();
  }

  void jMenuItem3_actionPerformed(ActionEvent e) {
    ElwizAboutFrame dlg = new ElwizAboutFrame(this);
    Dialog.setDlgLocation(dlg,this);
    dlg.setModal(true);
    dlg.show();
  }


  void jMenuItem4_actionPerformed(ActionEvent e) {
  }


}