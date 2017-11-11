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
import de.nmichael.efa.util.SwingWorker;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.gui.BaseFrame;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.io.*;
import java.awt.print.*;
import java.net.*;
import java.beans.*;
import java.util.*;

// @i18n complete

public class BrowserFrame extends JDialog implements ActionListener {
  final static int PROGRESS_TIMETOPOPUP   = 1;
  final static int PROGRESS_TIMERINTERVAL = 20;

  BorderLayout borderLayout1 = new BorderLayout();
  JButton closeButton = new JButton();
  JScrollPane jScrollPane1 = new JScrollPane();
  public JEditorPane out = new JEditorPane();
  JPanel northPanel = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  JPanel jPanel2 = new JPanel();
  JButton reloadButton = new JButton();
  JButton nextButton = new JButton();
  JButton backButton = new JButton();
  JPanel jPanel3 = new JPanel();
  JTextField url = new JTextField();
  BorderLayout borderLayout3 = new BorderLayout();
  History history = null;
  History current = null;
  JButton printButton = new JButton();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JButton pageButton = new JButton();
  boolean tour = false;
  boolean nopage = false;
  String lastDownload = null; // letzten Download merken um "doppelte" zu verhindern
  boolean locked = false; // wenn efa durch Anzeiges des Browsers gelocked ist und nur von Admin entsperrt werden kann
  String docText = null;
  Object highlightTag = null;
  int searchStart = 0;

  private ProgressMonitor progressMonitor;
  private javax.swing.Timer timer = new javax.swing.Timer(PROGRESS_TIMERINTERVAL, new TimerListener());;
  private DownloadThread downloadThread;
  JButton saveButton = new JButton();
  TimeoutThread timeoutThread;
  JPanel southPanel = new JPanel();
  BorderLayout borderLayout4 = new BorderLayout();
  JPanel searchPanel = new JPanel();
  GridBagLayout gridBagLayout2 = new GridBagLayout();
  JLabel searchLabel = new JLabel();
  JTextField search = new JTextField();
  JButton searchPrevButton = new JButton();
  JButton searchNextButton = new JButton();
  JLabel sslLabel = new JLabel();


  class TimeoutThread extends Thread {
    private BrowserFrame frame;
    private int timeout;
    private boolean locked;
    private DataTypeDate lockDatum;
    private DataTypeTime lockZeit;

    public TimeoutThread(BrowserFrame frame, int timeout, boolean locked, DataTypeDate lockDatum, DataTypeTime lockZeit) {
      this.frame = frame;
      this.timeout = timeout;
      this.locked = locked;
      this.lockDatum = lockDatum;
      this.lockZeit = lockZeit;
    }

    public void run() {
      try {
        if (!locked) {
          Thread.sleep(timeout * 1000);
        } else {
          if (lockDatum == null) return;
          GregorianCalendar unlock;
          if (lockZeit != null) unlock = new GregorianCalendar(lockDatum.getYear(),lockDatum.getMonth()-1,lockDatum.getDay(),lockZeit.getHour(),lockZeit.getMinute());
          else unlock = new GregorianCalendar(lockDatum.getYear(),lockDatum.getMonth()-1,lockDatum.getDay());
          GregorianCalendar now;
          do {
            Thread.sleep(60*1000);
            now = new GregorianCalendar();
          } while(unlock.after(now));
          unlock();
        }
      } catch(InterruptedException e) {
        return;
      }
      if (Dialog.frameCurrent() == frame) frame.cancel(true);
    }
  }



  class History {
    String url=null;
    History next=null;
    History prev=null;
  }

  class LinkFollower implements HyperlinkListener {
    public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        if ((e.getURL().toString().toLowerCase().startsWith("http://") || e.getURL().toString().toLowerCase().startsWith("https://")) &&
            Daten.applID == Daten.APPL_EFABH && Daten.applMode == Daten.APPL_MODE_NORMAL) {
          return;
        }
        if (e.getURL().toString().toLowerCase().startsWith("mailto:") &&
            Daten.applID == Daten.APPL_EFABH && Daten.applMode == Daten.APPL_MODE_NORMAL) {
          return;
        }

/*
        // Der folgende Code ist ein Workaround für Java-Bug-ID 6222200 (EditorPane GET instead of POST).
        // Zur Zeit funktioniert der Workaround allerdings noch nicht.
        try {
          if (Daten.javaVersion.startsWith("1.5") || Daten.javaVersion.startsWith("1.6")) {
            if (de.nmichael.efa.java15.Java15.editorPaneHandlePostEvent(out,e)) return;
          }
        } catch(UnsupportedClassVersionError ee) {
        }
*/

        try {
          URLConnection conn = e.getURL().openConnection();
          if (conn == null || conn.getContentType() == null) {
            out.setText(International.getString("FEHLER") + ": "+
                    International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}",e.getURL().toString(),
                    International.getString("Bist Du online?")));
            return;
          }
          String surl = e.getURL().toString();
          if (conn.getContentType().equals("text/html") || conn.getContentType().equals("text/plain")) {

            // check for various email addresses
            if (surl.toLowerCase().equals("mailto:"+Daten.EMAILINFO)) {
                surl = HtmlFactory.createMailto(Daten.EMAILINFO);
            }
            if (surl.toLowerCase().equals("mailto:"+Daten.EMAILBUGS)) {
                surl = HtmlFactory.createMailto(Daten.EMAILBUGS);
            }
            if (surl.toLowerCase().equals("mailto:"+Daten.EMAILHELP)) {
                surl = HtmlFactory.createMailto(Daten.EMAILHELP);
            }

            if (surl.toLowerCase().startsWith("mailto:"))
              Dialog.error(International.getMessage("Bitte benutze ein externes email-Programm, um eine email an {receiver} zu verschicken!",
                      surl.substring(7,surl.length())));
            else setNewPage(surl);
          } else downloadUrl(conn,e.getURL().toString());
        } catch(IOException ee) {
          out.setText(International.getString("FEHLER") + ": "+
                  International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}",e.getURL().toString(),ee.toString())+"\n"+
                  International.getString("Eventuell wird efa durch eine Firewall blockiert.") + " " +
                  International.getString("Bitte prüfe Deine Firewall-Einstellungen und erlaube efa den Internet-Zugriff "+
                      "oder benutze einen normalen Webbrowser."));
        }
      }
    }
  }


  public BrowserFrame(JFrame parent, String title, String url, boolean tour) { // für Aufruf aus efaFrame
    super(parent);
    this.tour = tour;
    if (!tour) Dialog.frameOpened(this);
    frIni(title,url);
  }
  public BrowserFrame(JDialog parent, String title, String url, boolean tour) { // für Aufruf aus JDialog
    super(parent);
    this.tour = tour;
    if (!tour) Dialog.frameOpened(this);
    frIni(title,url);
  }
  public BrowserFrame(JDialog parent, String title, String url) { // für Aufruf aus Dialog
    super(parent);
    Dialog.frameOpened(this);
    frIni(title,url);
  }
  public BrowserFrame(String title, String url) { // für Aufruf Efa beim Parametererstellen von Statistiken
    Dialog.frameOpened(this);
    frIni(title,url);
  }
  /* @todo (P4) lock efa
  public BrowserFrame(de.nmichael.efa.direkt.EfaDirektFrame parent, boolean vollbild, String url) { // für Aufruf zum Locken von efa durch Browser-Frame
    super(parent);
    this.efaDirektFrame = parent;
    if (vollbild) {
      this.setUndecorated(true);
      this.setResizable(false);
    }
    this.tour = false;
    this.locked = true;
    Dialog.frameOpened(this);
    frIni(Daten.EFA_LONGNAME,url);
    this.remove(this.northPanel);
    this.remove(this.southPanel);
  }
  */

  public BrowserFrame() { // Objekt, wenn nur die Download-Funktionalität genutzt werden soll
  }

  public void setClosingTimeout(int timeout) {
    if (timeout > 0) {
      timeoutThread = new TimeoutThread(this,timeout,
                                        locked,
                                        Daten.efaConfig.getValueEfaDirekt_lockEfaUntilDatum(),
                                        Daten.efaConfig.getValueEfaDirekt_lockEfaUntilZeit());
      timeoutThread.start();
    }
  }


  void frIni(String title, String url) {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();
      out.setContentType("text/html");
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    pack();
    this.setTitle(title);
    setNewPage(url);
    out.requestFocus();
  }

  // ActionHandler Events
  public void keyAction(ActionEvent evt) {
    if (evt == null || evt.getActionCommand() == null) return;
    if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
      cancel(false);
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

    closeButton.setNextFocusableComponent(backButton);
    Mnemonics.setButton(this, closeButton, "Schließen");
    closeButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeButton_actionPerformed(e);
      }
    });
    this.setTitle(International.getString("Ausgabe"));
    this.getContentPane().setLayout(borderLayout1);
    jScrollPane1.setMinimumSize(new Dimension(300, 200));
    jScrollPane1.setPreferredSize(new Dimension(600, 300));
    northPanel.setPreferredSize(new Dimension(500, 25));
    northPanel.setLayout(borderLayout2);
    reloadButton.setNextFocusableComponent(saveButton);
    reloadButton.setPreferredSize(new Dimension(45, 22));
    reloadButton.setToolTipText(International.getString("Neu laden"));
    reloadButton.setIcon(BaseFrame.getIcon("browser_reload.gif"));
    reloadButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        reloadButton_actionPerformed(e);
      }
    });
    nextButton.setNextFocusableComponent(reloadButton);
    nextButton.setPreferredSize(new Dimension(45, 22));
    nextButton.setToolTipText(International.getString("Vorwärts"));
    nextButton.setIcon(BaseFrame.getIcon("browser_forward.gif"));
    nextButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        nextButton_actionPerformed(e);
      }
    });
    backButton.setNextFocusableComponent(nextButton);
    backButton.setPreferredSize(new Dimension(45, 22));
    backButton.setToolTipText(International.getString("Zurück"));
    backButton.setIcon(BaseFrame.getIcon("browser_back.gif"));
    backButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        backButton_actionPerformed(e);
      }
    });
    jPanel3.setLayout(borderLayout3);
    url.setMaximumSize(new Dimension(2147483647, 19));
    url.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        url_actionPerformed(e);
      }
    });
    jPanel2.setMinimumSize(new Dimension(249, 25));
    jPanel2.setPreferredSize(new Dimension(295, 25));
    jPanel2.setLayout(gridBagLayout1);
    printButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printButton_actionPerformed(e);
      }
    });
    printButton.setNextFocusableComponent(pageButton);
    printButton.setPreferredSize(new Dimension(45, 22));
    printButton.setToolTipText(International.getString("Drucken"));
    printButton.setIcon(BaseFrame.getIcon("browser_print.gif"));
    pageButton.setNextFocusableComponent(out);
    pageButton.setPreferredSize(new Dimension(45, 22));
    pageButton.setToolTipText(International.getString("Seite einrichten"));
    pageButton.setIcon(BaseFrame.getIcon("browser_printsetup.gif"));
    pageButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pageButton_actionPerformed(e);
      }
    });
    saveButton.setNextFocusableComponent(printButton);
    saveButton.setPreferredSize(new Dimension(45, 22));
    saveButton.setToolTipText(International.getString("Seite speichern"));
    saveButton.setIcon(BaseFrame.getIcon("browser_save.gif"));
    saveButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveButton_actionPerformed(e);
      }
    });
    out.setNextFocusableComponent(closeButton);
    out.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        out_propertyChange(e);
      }
    });
    southPanel.setLayout(borderLayout4);
    searchPanel.setLayout(gridBagLayout2);
    searchLabel.setLabelFor(search);
    Mnemonics.setLabel(this, searchLabel, "  "+International.getStringWithMnemonic("Suche")+": ");
    search.setText("");
    Dialog.setPreferredSize(search,300,19);
    search.addKeyListener(new java.awt.event.KeyAdapter() {
    public void keyReleased(KeyEvent e) {
      searchfor(e);
    }
    });
    searchNextButton.setPreferredSize(new Dimension(45, 22));
    searchNextButton.setIcon(BaseFrame.getIcon("browser_forward.gif"));
    searchNextButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        search_next(e);
      }
    });

    searchPrevButton.setPreferredSize(new Dimension(45, 22));
    searchPrevButton.setIcon(BaseFrame.getIcon("browser_back.gif"));
    searchPrevButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        search_prev(e);
      }
    });

    sslLabel.setIcon(BaseFrame.getIcon("browser_secure.gif"));
    sslLabel.setVisible(false);
    this.getContentPane().add(northPanel,  BorderLayout.NORTH);
    northPanel.add(jPanel2, BorderLayout.WEST);
    jPanel2.add(backButton,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel2.add(nextButton,  new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel2.add(reloadButton,   new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    if (Daten.applID != Daten.APPL_EFABH || Daten.applMode == Daten.APPL_MODE_ADMIN) {
      jPanel2.add(printButton,  new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0
              ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      jPanel2.add(pageButton,  new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0
              ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      jPanel2.add(saveButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
              ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    }
    northPanel.add(jPanel3, BorderLayout.CENTER);
    jPanel3.add(url, BorderLayout.SOUTH);
    southPanel.add(closeButton,  BorderLayout.SOUTH);
    southPanel.add(searchPanel,  BorderLayout.WEST);
    searchPanel.add(searchLabel,    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    searchPanel.add(search,     new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    searchPanel.add(searchPrevButton,   new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    searchPanel.add(searchNextButton,  new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    southPanel.add(sslLabel, BorderLayout.EAST);
    this.getContentPane().add(jScrollPane1, BorderLayout.CENTER);
    this.getContentPane().add(southPanel,  BorderLayout.SOUTH);
    jScrollPane1.getViewport().add(out, null);

/*
    // Der folgende Code ist ein Workaround für Java-Bug-ID 6222200 (EditorPane GET instead of POST).
    // Zur Zeit funktioniert der Workaround allerdings noch nicht.
    try {
      if (Daten.javaVersion.startsWith("1.5") || Daten.javaVersion.startsWith("1.6")) de.nmichael.efa.java15.Java15.setEditorPaneAutoFormSubmissionFalse(out);
    } catch(UnsupportedClassVersionError e) {
    }
*/

    out.addHyperlinkListener(new LinkFollower());
    out.setEditable(false);

    url.setVisible(false);
  }
  /**Overridden so we can exit when window is closed*/
  protected void processWindowEvent(WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      cancel(false);
    }
    if (!locked) {
      super.processWindowEvent(e);
    }
  }

  void unlock() {
    locked = false;
    // @todo (P4) lock efa
    //if (efaDirektFrame != null) {
    //  efaDirektFrame.lockEfaAt(null,null);
    //}
    if (Daten.efaConfig != null) {
      Daten.efaConfig.setValueEfaDirekt_locked(false);
      //@efaconfig Daten.efaConfig.writeFile();
      Logger.log(Logger.INFO,
              Logger.MSG_EVT_UNLOCKED,
              International.getString("efa ist wieder entsperrt und für die Benutzung freigegeben."));
    }
  }

  /**Close the dialog*/
  void cancel(boolean timeout) {
    // @todo (P4) lock efa
    /*
    if (locked) {
      de.nmichael.efa.direkt.Admin admin = null;
      do {
        admin = de.nmichael.efa.direkt.AdminLoginFrame.login(this,International.getString("Entsperren von efa"),null);
        if (admin != null && !admin.allowedEfaSperren)
          Dialog.error(International.getString("Du hast nicht die Berechtigung, um efa zu entsperren!"));
      } while (admin != null && !admin.allowedEfaSperren);
      if (admin == null) return;
      unlock();
    }
    */
    if (tour) Dialog.tourRunning = false;
    if (!tour) Dialog.frameClosed(this);

    try {
      if (!timeout && timeoutThread != null && timeoutThread.isAlive()) timeoutThread.interrupt();
    } catch(Exception e) {}

    dispose();
  }

  /**Close the dialog on a button event*/
  public void actionPerformed(ActionEvent e) {
  }

  void closeButton_actionPerformed(ActionEvent e) {
    cancel(false);
  }

  void setPage(String url) {
    try {
      out.setPage(url);
      this.url.setText(url);
      updateSslLabel(url);
      nopage = false;
    } catch (IOException e) {
        out.setText(International.getString("FEHLER") + ": "+
                    International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}",url,e.toString()));
        nopage = true;
    } catch (Exception ee) {
      // abfangen von Java-Fehlern bei der Darstellung von HTML-Seiten
    }
    nextButton.setEnabled(current.next != null);
    backButton.setEnabled(current.prev != null);
  }

  void storePageInHistory(String url) {
    if (current == null) {
      history = new History();
      history.url = url;
      current = history;
    } else {
      if (current.url != null && current.url.equals(url)) return;
      History h = new History();
      h.url = url;
      h.prev = current;
      current.next = h;
      current = h;
    }
  }

  void setNewPage(String url) {
    if (url == null || url.length() == 0) return;
    if (url.indexOf(":")<0) url = "file:" + url;
    storePageInHistory(url);
    setPage(url);

    if (Dialog.tourRunning && Dialog.frameCurrent() != null) {
      Dialog.frameCurrent().requestFocus();
    }
  }

  void backButton_actionPerformed(ActionEvent e) {
    if (current.prev != null) {
      if (nopage) {
        try {
          out.setPage("file:"+HtmlFactory.createReload());
        } catch (IOException ee) {
        }
      }
      current = current.prev;
      setPage(current.url);
    }
  }

  void nextButton_actionPerformed(ActionEvent e) {
    if (current.next != null) {
      current = current.next;
      setPage(current.url);
    }
  }

  void reloadButton_actionPerformed(ActionEvent e) {
    String pageShowing = url.getText().trim();
    try {
      out.setPage("file:"+HtmlFactory.createReload());
    } catch (IOException ee) {
    }
    setPage(pageShowing);
  }

  void url_actionPerformed(ActionEvent e) {
    setNewPage(url.getText().trim());
  }

  void printButton_actionPerformed(ActionEvent e) {
    SimpleFilePrinter sfp = new SimpleFilePrinter(out);
    if (sfp.setupPageFormat()) {
      if (sfp.setupJobOptions()) {
        try {
          sfp.printFile();
        } catch (Exception ee) {
          Logger.log(Logger.ERROR,ee.toString());
        }
      }
    }
  }

  void pageButton_actionPerformed(ActionEvent e) {
    PageFormat defFormat = PrinterJob.getPrinterJob().defaultPage();
    PageFormat pf = PrinterJob.getPrinterJob().pageDialog(defFormat);
    if (pf != defFormat) SimpleFilePrinter.pageSetup = pf;
  }

  void saveButton_actionPerformed(ActionEvent e) {
    if (Daten.applID == Daten.APPL_EFABH && Daten.applMode == Daten.APPL_MODE_NORMAL) return;
    String quelle = this.url.getText();
    if (!quelle.startsWith("file:")) {
      Dialog.infoDialog(International.getString("Fehler"),
              International.getString("Es können nur lokale Seiten gespeichert werden!"));
      return;
    }
    quelle = quelle.substring(5,quelle.length());
    String ziel = Dialog.dateiDialog(this,International.getString("Seite speichern"),
            International.getString("HTML-Dateien"),"html",Daten.efaDataDirectory,true);
    if (ziel == null) return;

    int i = quelle.lastIndexOf(".");
    String ext = null;
    if (i>0) {
      ext = quelle.substring(i+1,quelle.length());
      if (ext.length()>0 && !ziel.toLowerCase().endsWith(ext.toLowerCase())) ziel += "."+ext;
    }
    if (!EfaUtil.copyFile(quelle,ziel)) {
        Dialog.error(International.getMessage("Fehler beim Speichern der Seite unter dem Namen {name}.",ziel));
    } else {
      // Datei erfolgreich kopiert

      if (ziel.toLowerCase().endsWith(".html") || ziel.toLowerCase().endsWith(".htm")) {
        // Dennis will unbedingt, daß auch in HTML-Seiten eingebundene Bilder mit gespeichert werden.
        // Daher hier also eine Implementation, die extrem häßlich und nur auf die von efa erzeugten
        // HTML-Seiten zugeschnitten ist, aber immerhin ihren Zweck erfüllt... ;-)

        // Es wird in der Quelldatei nach der Zeichenkette "src=" gesucht (unabh. davon, ob sie in einem
        // <img>-Tag auftritt oder nicht) und kopiert diese Datei, sofern es eine "lokale" Datei ist, d.h.
        // der Pfag nicht mit "http://", mit "/" oder mit "." beginnt....

        String quelldir = EfaUtil.getPathOfFile(quelle); if (quelldir.length()>0) quelldir += Daten.fileSep;
        String zieldir = EfaUtil.getPathOfFile(ziel);    if (zieldir.length()>0) zieldir += Daten.fileSep;

        BufferedReader f=null;
        try {
          // Quelldatei lesen
          f = new BufferedReader(new InputStreamReader(new FileInputStream(quelle),Daten.ENCODING_UTF)); // @todo (P5) get proper encoding???
          String s;
          // Zeilenweise lesen
          while ( (s=f.readLine()) != null) {
            i = -1;
            do {
              // Zeichenkette "src=" suchen
              i = s.indexOf("src=",i+1);
              if (i>=0) {
                // img-Filename extrahieren
                String img = s.substring(i+4,s.length());
                // Begrenzer des Filenamens ermitteln
                if (img.length()>0 && (img.charAt(0)=='"' || img.charAt(0)=='\'')) {
                  char quote = img.charAt(0);
                  img = img.substring(1,img.length());
                  int endquote = img.indexOf(quote);
                  if (endquote>0) { // Ende der Anfüngrungsstriche gefunden?
                    img = img.substring(0,endquote);
                    if (!img.toLowerCase().startsWith("http://") &&
                        !img.startsWith("/") && !img.startsWith(".")) { // "lokale" Datei?
                      if (!new File(zieldir+img).exists()) { // kopiere nur Dateien, die im Zielverzeichnis noch nicht existieren
                        EfaUtil.copyFile(quelldir+img,zieldir+img);
                      }
                    }
                  }
                }
              }
            } while(i>=0);
          }
        } catch(Exception ee) {
          Dialog.error(International.getMessage("Fehler beim Speichern der eingebetteten Bilder: {message}",ee.toString()));
        } finally {
          try { f.close(); } catch(Exception eee) { f = null; }
        }
      }
    }
  }


  void downloadUrl(URLConnection conn, String fname) {
    // irgendwie passieren Downloads immer doppelt. Daher: Falls exakt derselbe
    // Download zum zweiten Mal, dann ignorieren
    if (lastDownload != null && lastDownload.equals(conn.toString())) {
      lastDownload = null;
      return;
    }
    lastDownload = conn.toString();


    try {
      conn.connect();

      String localName = Daten.efaTmpDirectory+fname.substring(fname.lastIndexOf("/")+1,fname.length());

      String dat = Dialog.dateiDialog(this,International.getString("Download"),
              null,null,localName,localName,
              International.getString("Download speichern"),true,false);

      if (dat != null) {
        runDownload(this,conn,fname,dat,false);
      }
    } catch(IOException e) {
      Dialog.error(International.getString("Download fehlgeschlagen")+":\n"+e.toString()+"\n"+
              International.getString("Eventuell wird efa durch eine Firewall blockiert."));
    }
  }

  private boolean runDownload(ProgressMonitor progressMonitor, URLConnection conn, String local, boolean waitfor) {
    progressMonitor.setProgress(0);
    progressMonitor.setMaximum(conn.getContentLength());
    progressMonitor.setMillisToDecideToPopup(PROGRESS_TIMETOPOPUP);
    downloadThread.go(conn,local);
    timer.start();
    if (waitfor) {
      try {
        downloadThread.thr.join();
      } catch(InterruptedException e) {}
    }
    if (downloadThread.exceptionText != null) {
      Dialog.error(International.getString("Download fehlgeschlagen")+": "+downloadThread.exceptionText+"\n"+
              International.getString("Eventuell wird efa durch eine Firewall blockiert."));
      return false;
    }
    return true;
  }


  public boolean runDownload(JFrame frame, URLConnection conn, String remote, String local, boolean waitfor) {
    downloadThread = new DownloadThread(null);
    progressMonitor = new ProgressMonitor(frame, International.getString("Download") + " " + remote, "", 0, downloadThread.getLengthOfTask());
    return runDownload(progressMonitor, conn, local, waitfor);
  }

  public boolean runDownload(JDialog frame, URLConnection conn, String remote, String local, boolean waitfor) {
    downloadThread = new DownloadThread(null);
    progressMonitor = new ProgressMonitor(frame, International.getString("Download")+" "+remote, "", 0, downloadThread.getLengthOfTask());
    return runDownload(progressMonitor, conn, local, waitfor);
  }

  public boolean runDownload(JFrame frame, URLConnection conn, String remote, String local, ExecuteAfterDownload afterDownload) {
    downloadThread = new DownloadThread(afterDownload);
    progressMonitor = new ProgressMonitor(frame, International.getString("Download")+ " "+remote, "", 0, downloadThread.getLengthOfTask());
    return runDownload(progressMonitor, conn, local, false);
  }
  public boolean runDownload(JDialog frame, URLConnection conn, String remote, String local, ExecuteAfterDownload afterDownload) {
    downloadThread = new DownloadThread(afterDownload);
    progressMonitor = new ProgressMonitor(frame, International.getString("Download")+ " "+remote, "", 0, downloadThread.getLengthOfTask());
    return runDownload(progressMonitor, conn, local, false);
  }

  class DownloadThread {
    Thread thr;
    URLConnection conn;
    String fname;
    int downDone,downTotal;
    InputStream i;
    FileOutputStream o;
    String exceptionText = null;
    ExecuteAfterDownload afterDownload = null;

    public DownloadThread(ExecuteAfterDownload afterDownload) {
      this.afterDownload = afterDownload;
      this.downTotal = 100; // nur ini-Wert
    }

    Thread go(URLConnection conn, String fname) {
      this.conn = conn; this.fname = fname;
      final SwingWorker worker = new SwingWorker() {
        public Object construct() {
          return new ActualTask(afterDownload);
        }
      };
      return (thr = worker.start());
    }

    int getLengthOfTask() {
      return downTotal;
    }

    int getCurrent() {
      return downDone;
    }

    void stop() {
      try {
        i.close();
        o.close();
      } catch(IOException e) { i = null; o = null; }
    }
    boolean done() {
     return (downTotal == downDone);
    }
    String getMessage() {
      return International.getMessage("{bytesDone} Bytes von {bytesTotal} Bytes ...",downDone,downTotal);
    }
    void exit() {
//      thr.destroy();
    }
    class ActualTask {
      ActualTask (ExecuteAfterDownload afterDownload) {
        try {
          int BUFSIZE = 1500;
          byte[] buf = new byte[BUFSIZE];
          i = conn.getInputStream();
          o = new FileOutputStream(fname);
          int c=0;
          downDone=0;
          downTotal=conn.getContentLength();
          while ( (c = i.read(buf,0,BUFSIZE)) > 0) {
            o.write(buf,0,c);
            downDone+=c;
          }
          i.close();
          o.close();
          if (afterDownload != null) afterDownload.success();
        } catch(IOException e) {
          exceptionText = e.getMessage();
          if (afterDownload != null) afterDownload.failure(e.getMessage());
        }
      }
    }
  }


    class TimerListener implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            if (progressMonitor.isCanceled() || downloadThread.done()) {
                progressMonitor.close();
//                downloadThread.stop();
                downloadThread.exit();
                timer.stop();
            } else {
                progressMonitor.setNote(downloadThread.getMessage());
                progressMonitor.setMaximum(downloadThread.getLengthOfTask());
                progressMonitor.setProgress(downloadThread.getCurrent());
            }
        }
    }


  void out_propertyChange(PropertyChangeEvent e) {
    try {
      storePageInHistory(out.getPage().toString());
      this.url.setText(out.getPage().toString());
      updateSslLabel(out.getPage().toString());
    } catch(Exception ee) {
      EfaUtil.foo();
    }
  }

  void searchfor(KeyEvent e) {
    try {
      if (docText == null || docText.length() == 0) {
        docText = out.getDocument().getText(0,out.getDocument().getLength()).toLowerCase();
      }
      String s = search.getText().trim().toLowerCase();
      if (s.length() == 0) {
        out.getHighlighter().removeAllHighlights();
        highlightTag = null;
        return;
      }
      int pos = 0;
      if (searchStart <= 0) {
        pos = docText.indexOf(s);
      } else {
        pos = docText.substring(searchStart + 1).indexOf(s);
        if (pos >= 0) pos += searchStart + 1;
      }
      if (pos >= 0) {
        out.select(pos, pos+s.length());
        if (highlightTag == null) {
          highlightTag = out.getHighlighter().addHighlight(pos, pos+s.length(), new DefaultHighlighter.DefaultHighlightPainter(Color.yellow));
        }
        out.getHighlighter().changeHighlight(highlightTag,pos, pos+s.length());

      }
    } catch(Exception ee) {
    }
  }

  void search_prev(ActionEvent e) {
    searchStart = 0;
    searchfor(null);
  }

  void search_next(ActionEvent e) {
    searchStart = out.getSelectionStart();
    searchfor(null);
  }

  void updateSslLabel(String url) {
    sslLabel.setVisible(url.startsWith("https"));;
  }


}
