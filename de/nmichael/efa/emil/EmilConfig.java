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

import de.nmichael.efa.*;
import java.io.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class EmilConfig {

  private String dat;           // Dateiname
  private String kennung;       // Dateikennung

  private String dir_efw="";
  private String dir_csv="";
  private String std_csv="";
  private boolean exportNurErfuellt=true;


  // Konstruktor
  public EmilConfig(String s) {
    dat = s;
    kennung = "##EMIL.010.KONFIGURATION##";
  }


  // Konfigurationsdatei einlesen
  public synchronized boolean readFile() {
    dir_efw = Daten.efaDataDirectory;
    dir_csv = Daten.efaDataDirectory;
    std_csv = Daten.efaDataDirectory + "standard.csv";
    exportNurErfuellt=true;

    BufferedReader f;
    String s;

    // Datei öffnen
    try {
      f = new BufferedReader(new InputStreamReader(new FileInputStream(dat),Daten.ENCODING_ISO));
    } catch(IOException e) {
      return false;
    }
    // Dateiformat überprüfen
    try {
      s = f.readLine();
      if ( s == null || !s.startsWith(kennung) ) {
        f.close();
        return false;
      }
    } catch(IOException e) {
      return false;
    }
    // Konfiguration lesen
    try {
      while ((s = f.readLine()) != null) {
        s = s.trim();
        if (s.startsWith("DIR_EFW="))
            dir_efw=s.substring(8,s.length()).trim();
        if (s.startsWith("DIR_CSV="))
            dir_csv=s.substring(8,s.length()).trim();
        if (s.startsWith("STD_CSV="))
            std_csv=s.substring(8,s.length()).trim();
        if (s.startsWith("EXPORT_NUR_ERFUELLT="))
            exportNurErfuellt=s.substring(20,s.length()).trim().equals("JA");
      }
    } catch(IOException e) {
      try {
        f.close();
      } catch(Exception ee) {
        return false;
      }
    }
    // Datei schließen
    try {
      f.close();
    } catch(Exception e) {
      return false;
    }
    return true;
  }


  // Konfigurationsdatei speichern
  public synchronized boolean writeFile() {
    BufferedWriter f;

    // Versuchen, die Datei zu öffnen
    try {
      f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dat),Daten.ENCODING_ISO));
    } catch(IOException e) {
      return false;
    }
    // Dateikennung schreiben
    try {
      f.write(kennung+" - Bitte nicht von Hand bearbeiten!\n");
    } catch(IOException e) {
      try {
        f.close();
      } catch(Exception ee) {
        return false;
      }
      return false;
    }
    // Datei schreiben
    try {
      f.write("DIR_EFW="+dir_efw+"\n");
      f.write("DIR_CSV="+dir_csv+"\n");
      f.write("STD_CSV="+std_csv+"\n");
      f.write("EXPORT_NUR_ERFUELLT="+(exportNurErfuellt ? "JA" : "NEIN")+"\n");
    } catch(Exception e) {
      try {
        f.close();
      } catch(Exception ee) {
        return false;
      }
      return false;
    }
    try {
      f.close();
    } catch(IOException e) {
      return false;
    }
    return true;
  }

  public void   setFilename(String s) { dat = s; }
  public String getFilename() { return dat; }

  public void   setDirEfw(String s) { dir_efw = s; }
  public void   setDirCsv(String s) { dir_csv = s; }
  public void   setStdCsv(String s) { std_csv = s; }
  public void   setExportNurErfuellt(boolean b) { exportNurErfuellt = b; }

  public String getDirEfw() { return dir_efw; }
  public String getDirCsv() { return dir_csv; }
  public String getStdCsv() { return std_csv; }
  public boolean getExportNurErfuellt() { return exportNurErfuellt; }

}