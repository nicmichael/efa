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
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;

// @i18n complete

public class DownloadFrame extends JDialog implements ActionListener {
  Vector fnames;
  Vector fsizes;
  String plugname;
  boolean buttonClose = false;
  JDialog parent;
  boolean exit = false;
  String progname;
  BorderLayout borderLayout1 = new BorderLayout();
  JButton button = new JButton();
  JScrollPane jScrollPane1 = new JScrollPane();
  JTextArea out = new JTextArea();


  public DownloadFrame(String progname, JDialog parent, String plugname, Vector fnames, Vector fsizes, boolean exit) {
    super(parent);
    iniDownloadFrame(progname, plugname,fnames,fsizes,exit);
    button.requestFocus();
  }

  public DownloadFrame(String progname, String plugname, Vector fnames, Vector fsizes, boolean exit) {
    iniDownloadFrame(progname, plugname,fnames,fsizes,exit);
    button.requestFocus();
  }

  public void iniDownloadFrame(String progname, String plugname, Vector fnames, Vector fsizes, boolean exit) {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    Dialog.frameOpened(this);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    EfaUtil.pack(this);
    this.progname = progname;
    this.plugname = plugname;
    this.fnames = fnames;
    this.fsizes = fsizes;
    this.exit = exit;

    int size=0;
    for (int c=0; c<fnames.size(); c++)
      if (fnames.get(c) != null) size += ((Integer)fsizes.get(c)).intValue();

    out.append(International.getMessage("Folgende Dateien werden jetzt installiert (Gesamtgröße: {size} Bytes):",size)+"\n");
    for (int c=0; c<fnames.size(); c++)
      if (fnames.get(c) != null) {
        out.append((String)fnames.get(c)+" ("+((Integer)fsizes.get(c)).intValue()+" byte)\n");
        size += ((Integer)fsizes.get(c)).intValue();
      }
    out.append("\n");
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

    button.setText(International.getString("Plugin-Download starten"));
    button.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        button_actionPerformed(e);
      }
    });
    this.getContentPane().setLayout(borderLayout1);
    out.setFont(new java.awt.Font("Dialog", 1, 12));
    out.setEditable(false);
    this.setTitle(International.getString("Plugin-Installation"));
    this.getContentPane().add(button, BorderLayout.SOUTH);
    this.getContentPane().add(jScrollPane1, BorderLayout.CENTER);
    this.setSize(new Dimension(607, 300));
    jScrollPane1.getViewport().add(out, null);
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
    if (AfterDownloadImpl.fileCount > 0) {
      if (Dialog.yesNoDialog(International.getString("Offene Downloads"),
                             International.getString("Es sind noch nicht alle Downloads beendet. "+
                             "Möchtest Du wirklich abbrechen?")) != Dialog.YES) return;
    }
    Dialog.frameClosed(this);
    dispose();

    // Fertig
    String s="";
    File f;
    boolean ok = true;
    for (int i=0; i<fnames.size(); i++) {
      if (fnames.get(i) != null) {
        f = new File(Daten.efaPluginDirectory+(String)fnames.get(i));
        if (f.isFile()) {
          if (f.length() != ((Integer)fsizes.get(i)).intValue()) { 
              s += International.getMessage("{file}: ungültige Dateigröße (erwartet war: {size})", (String)fnames.get(i), ((Integer)fsizes.get(i)).intValue()) + "\n";
              ok=false;
          }
        } else {
            s+= (String)fnames.get(i)+": "+International.getString("nicht installiert")+"\n";
            ok=false;
        }
      }
    }

    if (ok && plugname.startsWith("JAXP"))
      Dialog.infoDialog(plugname,
                        "JAXP Java API for XML Processing\n"+
                        "This product includes software developed by the\nApache Software Foundation (http://www.apache.org/).\n\n"+
                        "The DOM bindings are published under the W3C Software Copyright Notice and License.\n"+
                        "Copyright (c) 2002 World Wide Web Consortium (http://www.w3.org/),\n"+
                        "(Massachusetts Institute of Technology (http://www.lcs.mit.edu/),\n"+
                        "Institut National de Recherche en Informatique et en Automatique\n"+
                        "(http://www.inria.fr/), Keio University (http://www.keio.ac.jp/)).\n"+
                        "All Rights Reserved. http://www.w3.org/Consortium/Legal/");
    if (ok && plugname.startsWith("FOP"))
      Dialog.infoDialog(plugname,
                        "FOP Formatting Objects Processor\nThis product includes software developed by the\nApache Software Foundation (http://www.apache.org/).");
    if (ok && plugname.startsWith("FTP"))
      Dialog.infoDialog(plugname,
                        "Java FTP client library\nCopyright (C) 2000  Enterprise Distributed Technologies Ltd\nwww.enterprisedt.com");
    if (ok && plugname.startsWith("EMAIL"))
      Dialog.infoDialog(plugname,
                        "JavaMail API 1.3.3 Release\nCopyright (C) SUN Microsystems\njava.sun.com");
    if (ok && plugname.startsWith("JSUNTIMES"))
      Dialog.infoDialog(plugname,
                        "jSunTimes v0.3\nCopyright (C) Jonathan Stott.\nwww.jstott.me.uk/jsuntimes");
    if (ok) {
      Dialog.infoDialog(International.getMessage("Das {plugin} wurde erfolgreich installiert. "+
                        "Bitte starte nun {program} neu, um die neuen Funktionen nutzen zu können!",plugname,progname));
      if (exit) {
          Daten.haltProgram(0);
      }
    } else {
      Dialog.error(International.getMessage("Das {plugin} konnte NICHT erfolgreich installiert werden!",plugname)+"\n"+s);
      if (exit) {
          Daten.haltProgram(Daten.HALT_INSTALLATION);
      }
    }
  }

  /**Close the dialog on a button event*/
  public void actionPerformed(ActionEvent e) {
  }


  void runDownload() {
      // Download der Plugin-Files
      AfterDownloadImpl.fileCount = fnames.size();
      for (int c=0; c<fnames.size(); c++) {
        if (fnames.get(c) != null) {
          AfterDownloadImpl after = new AfterDownloadImpl((String)fnames.get(c),out);
          out.append(International.getMessage("Starte Download von {file} ...",(String)fnames.get(c)));
          out.doLayout();
          if (EfaUtil.getFile(this,Daten.pluginWWWdirectory+(String)fnames.get(c),Daten.efaPluginDirectory+(String)fnames.get(c),after)) {
            out.append(" "+International.getString("gestartet")+"!\n");
          } else {
              out.append(" "+International.getString("FEHLER")+"!");
          }
        }
      }
  }

  void button_actionPerformed(ActionEvent e) {
    if (!buttonClose) {
      buttonClose = true;
      runDownload();
      button.setText(International.getString("Schließen"));
    } else cancel();
  }



  public static boolean getPlugin(String progname, String pname, String pfile, String phtml, String classError, JDialog frame, boolean exit) {
      if (Dialog.yesNoDialog(International.getString("Fehlendes Plugin"),
              International.getMessage("Das erforderliche {plugin} konnte nicht gefunden werden",pname) +
              ":\n" + classError + "\n"+
              International.getMessage("Möchtest Du das {plugin} jetzt installieren?",pname)
              ) != Dialog.YES) return false;
      int x = Dialog.auswahlDialog(International.getString("Art der Installation"),
              International.getString("efa kann die Plugin-Dateien automatisch aus dem Intnernet laden "+
                                                  "oder eine Anleitung für die manuelle Installation anzeigen.")+"\n"+
                                                  International.getString("Bitte wähle die Art der Installation."),
                                                  International.getString("Automatische Installation"),
                                                  International.getString("Manuelle Installation"));
      if (x == -1 || x == 2) return false;
      if (x == 1) { // manuelle Installation
        Dialog.neuBrowserDlg(frame,pname,"file:"+Daten.efaProgramDirectory+"html"+Daten.fileSep+phtml);
        return false;
      }

      // automatische Installation
      if (!Dialog.okAbbrDialog(International.getString("Automatische Installation"),
                               International.getString("Bitte stelle nun eine Verbindung zum Internet her. "+
                               "Sobald Du online bist, klicke bitte OK."))) return false;

      // aktuelle Plugin-Download-URL besorgen
      String infoFile = Daten.efaTmpDirectory+"plugins.url";
      if (!EfaUtil.getFile(frame,Daten.PLUGIN_WWW_URL,infoFile,true)) return false;
      try {
        BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(infoFile),Daten.ENCODING_ISO));
        String s;
        if ( (s = f.readLine()) != null) {
          Daten.pluginWWWdirectory = s.trim(); // dies ist die aktuelle Plugin-URL
        }
        f.close();
        new File(infoFile).delete();
      } catch(IOException ee) {
        Dialog.error(International.getMessage("Bestimmung der Plugin-URL schlug fehl: {error}",ee.toString()));
        return false;
      }

      // Informationen über Plugin besorgen
      infoFile = Daten.efaTmpDirectory+pfile;
      if (!EfaUtil.getFile(frame,Daten.pluginWWWdirectory+pfile,infoFile,true)) return false;

      // Informationen über das Plugin auswerten
      if (!EfaUtil.canOpenFile(infoFile)) {
        Dialog.error(International.getMessage("Plugin-Infodatei {file} konnte nicht geöffnet werden.",infoFile));
        return false;
      }

      int c=0;
      int size=0; // gesamtgröße des Plugins
      Vector _fnames = new Vector();
      Vector _sizes  = new Vector();
      try {
        BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(infoFile),Daten.ENCODING_ISO));
        String s;
        while ( (s = f.readLine()) != null) {
          s = s.trim();
          if (s.indexOf("|") > 0 && c<10) {
            String fname = s.substring(0,s.indexOf("|"));
            int fsize = EfaUtil.string2int(s.substring(s.indexOf("|")+1,s.length()),0);

            File ff = new File(Daten.efaPluginDirectory+fname);
            if (!ff.isFile() || ff.length() != fsize) {
              _fnames.add(fname);
              _sizes.add(new Integer(fsize));
              size += ((Integer)_sizes.lastElement()).intValue();
              c++;
            }
          }
        }
        f.close();
        new File(infoFile).delete();
      } catch(IOException ee) {
        Dialog.error(LogString.logstring_fileReadFailed(infoFile,International.getString("Plugin-Infordatei"),ee.toString()));
        return false;
      }
//      String[] fnames = (String[])_fnames.toArray();
//      int[] sizes =     new int   [_sizes.size()];
//      for (int i=0; i<sizes.length; i++) sizes[i] = ((Integer)_sizes.get(i)).intValue();

      // Download der Plugin-Files
      DownloadFrame dlg;
      if (frame != null) dlg = new DownloadFrame(progname,frame,pname,_fnames,_sizes,exit);
      else dlg = new DownloadFrame(progname,pname,_fnames,_sizes,exit);
      Dimension dlgSize = new Dimension(600,300);
      Dialog.setDlgLocation(dlg,frame);
      dlg.setModal(!Dialog.tourRunning);
      dlg.setSize(dlgSize);
      dlg.show();
      return true;
  }



}

class AfterDownloadImpl implements ExecuteAfterDownload {
  private String fname;
  private JTextArea out;
  public static int fileCount = 0;

  public AfterDownloadImpl(String fname, JTextArea out) {
    this.fname = fname;
    this.out = out;
  }

  public void success() {
    out.append(International.getMessage("Download von {file} erfolgreich beendet.",fname)+"\n");
    fileCount--;
  }

  public void failure(String text) {
    out.append(International.getMessage("Download von {file} fehlgeschlagen: {error}",fname,text)+"\n");
    fileCount--;
  }

}
