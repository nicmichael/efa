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
import de.nmichael.efa.util.DRVSignatur;
import java.io.*;
import java.util.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class ESigFahrtenhefte {

  public static final String KENNUNG="##EFA.150.SIGFAHRTENHEFTE##";

  private String datei=null;

  // Verein
  public String verein_user=null;
  public String verein_name=null;
  public String verein_mitglnr=null;
  public String quittungsnr=null;

  private Vector fahrtenhefte=null;

  public String keyName=null;
  public String keyDataBase64=null;



  // Konstruktor
  public ESigFahrtenhefte(String datei) {
    this.datei = datei;
    setBlank();
  }

  public void setBlank() {
    verein_user=null;
    verein_name=null;
    verein_mitglnr=null;
    quittungsnr=null;
    fahrtenhefte=new Vector();
  }

  public boolean writeFile() throws IOException {
      if (datei == null) return false;
      BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(datei),Daten.ENCODING_ISO));
      f.write(KENNUNG+"\n");

      f.write("\n[VEREIN]\n");
      if (verein_user != null) f.write("VEREIN="+verein_user+"\n");
      if (verein_name != null) f.write("VEREINSNAME="+verein_name+"\n");
      if (verein_mitglnr != null) f.write("MITGLIEDSNUMMER="+verein_mitglnr+"\n");
      if (quittungsnr != null) f.write("QUITTUNGSNUMMER="+quittungsnr+"\n");

      f.write("\n[FAHRTENHEFTE]\n");
      for (int i=0; i<fahrtenhefte.size(); i++) {
        DRVSignatur sig = (DRVSignatur)fahrtenhefte.get(i);
        f.write("FAHRTENHEFT="+sig.toString()+"\n");
      }

      if (keyName != null && keyDataBase64 != null) {
        f.write("\n[SCHLUESSEL]\n");
        f.write("NAME="+keyName+"\n");
        f.write("DATEN="+keyDataBase64+"\n");
      }

      f.close();
      return true;
  }

  // Lesen der Datei "datei"
  public boolean readFile() throws IOException {
    if (datei == null) return false;
    BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(datei),Daten.ENCODING_ISO));

    // Dateiformat prüfen
    String s = f.readLine();
    if (!s.startsWith(KENNUNG)) {
      f.close();
      return false;
    }

    // alle Felder löschen
    setBlank();

    String block="";
    // Datei lesen
    while ( (s = f.readLine()) != null) {
      s = s.trim();
      if (s.equals("")) continue;
      if (s.charAt(0) == '#') continue;

      // neuer Blockanfang?
      if (s.charAt(0) == '[' && s.charAt(s.length()-1) == ']') {
        block = s.substring(1,s.length()-1);
      }

      // Daten aus Blöcken extrahieren
      if (block.equals("VEREIN")) {
        if (s.startsWith("VEREIN=")) verein_user = s.substring(7,s.length());
        if (s.startsWith("VEREINSNAME=")) verein_name = s.substring(12,s.length());
        if (s.startsWith("MITGLIEDSNUMMER=")) verein_mitglnr = s.substring(16,s.length());
        if (s.startsWith("QUITTUNGSNUMMER=")) quittungsnr = s.substring(16,s.length());
      }
      if (block.equals("FAHRTENHEFTE")) {
        if (s.startsWith("FAHRTENHEFT=")) {
          DRVSignatur sig = new DRVSignatur(s.substring(12,s.length()));
          fahrtenhefte.add(sig);
        }
      }
      if (block.equals("SCHLUESSEL")) {
        if (s.startsWith("NAME=")) keyName = s.substring(5,s.length());
        if (s.startsWith("DATEN=")) keyDataBase64 = s.substring(6,s.length());
      }
    }

    f.close();
    return true;
  }

  public Vector getFahrtenhefte() {
    return this.fahrtenhefte;
  }

  public void addFahrtenheft(DRVSignatur sig) {
    this.fahrtenhefte.add(sig);
  }

  public String getDateiname() {
    return this.datei;
  }




}
