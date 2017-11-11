package tools;

/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

import java.io.*;
import java.util.*;

public class HelpGenerator {

  static final int NICHTS = 0;
  static final int BESCHREIBUNG = 1;
  static final int ERKLAERUNG = 2;
  static final int SIEHE = 3;

  static String split(String s, int c) {
    if (s == null) return "";
    if (s.indexOf("|")<0)
      if (c==1) return s;
      else return "";
    if (c==1) return s.substring(0,s.indexOf("|"));
    else return s.substring(s.indexOf("|")+1,s.length());
  }

  static void writeFile(String datei, String titel, Vector beschreibung, Vector erklaerung, Vector siehe) throws IOException {
    if (datei == null) return;

    String s;

    BufferedWriter f = new BufferedWriter(new FileWriter(datei));
    f.write("<html>\n");
    f.write("<head>\n");
    f.write("<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
    f.write("<title>efa - elektronisches Fahrtenbuch</title>\n");
    f.write("</head>\n");
    f.write("<body bgcolor=\"#ffffff\">\n");
    f.write("<h1 align=\"center\">"+titel+"</h1>\n");
    f.write("\n");

    if (beschreibung != null) {
      f.write("<br>\n");
      f.write("<table bgcolor=\"#0000ff\" width=\"100%\">\n");
      f.write("<tr><td><font color=\"#ffffff\"><b><i>Beschreibung</i></b></font></td></tr>\n");
      f.write("</table>\n");
      f.write("\n");
      for (int i=0; i<beschreibung.size(); i++)
        f.write("<p>"+beschreibung.get(i)+"</p>\n");
      f.write("\n");
    }

    if (erklaerung != null) {
      f.write("<br clear=\"all\">\n");
      f.write("<table bgcolor=\"#0000ff\" width=\"100%\">\n");
      f.write("<tr><td><font color=\"#ffffff\"><b><i>Erklï¿½rung der Buttons und Felder</i></b></font></td></tr>\n");
      f.write("</table>\n");
      f.write("\n");
      f.write("<table width=\"100%\">\n");

      String tstart,tende,td1opt,td2opt;
      for (int i=0; i<erklaerung.size(); i++) {
        tstart = tende = td1opt = td2opt = "";
        s = split((String)erklaerung.get(i),1);
        if (s.toLowerCase().startsWith("[but]")) {
          td1opt = " bgcolor=\"#dddddd\"";
          s = s.substring(5,s.length());
        }
        if (s.toLowerCase().startsWith("[field]")) {
          td1opt = " bgcolor=\"#dddddd\"";
          tstart = "<font color=\"#7777dd\">";
          tende  = "</font>";
          s = s.substring(7,s.length());
        }
        if (s.toLowerCase().startsWith("[reg]")) {
          td1opt = " bgcolor=\"#aaffaa\"";
          td2opt = " width=\"*\" bgcolor=\"#ddffdd\"";
          s = s.substring(5,s.length());
        }
        if (s.toLowerCase().startsWith("[sep]")) {
          td1opt = " bgcolor=\"#aaffaa\"";
          s = s.substring(5,s.length());
        }

        f.write("<tr>\n");
        if (((String)erklaerung.get(i)).indexOf("|")<0) f.write("  <td colspan=\"2\""+td1opt+">"+s+"</td>\n");
        else {
          f.write("  <td valign=\"top\"><table width=\"100%\"><tr><td"+td1opt+"><b>"+tstart+s+tende+"</b></td></tr></table></td>\n");
          f.write("  <td"+td2opt+">"+split((String)erklaerung.get(i),2)+"</td>\n");
        }
        f.write("</tr>\n");
      }
      f.write("</table>\n");
      f.write("\n");
    }

    if (siehe != null)  {
      f.write("<br clear=\"all\"><br>\n");
      f.write("<table bgcolor=\"#0000ff\" width=\"100%\">\n");
      f.write("<tr><td><font color=\"#ffffff\"><b><i>siehe auch:</i></b></font></td></tr>\n");
      f.write("</table>\n");
      f.write("<ul>\n");
      for (int i=0; i<siehe.size(); i++) {
        f.write("  <li><a href=\""+split((String)siehe.get(i),1)+"\">"+split((String)siehe.get(i),2)+"</a></li>\n");
      }
      f.write("</ul>\n");
      f.write("\n");
    }

    f.write("<p><a href=\"Frames.html\">Zurï¿½ck zur ï¿½bersicht der Hilfeseiten</a></p>\n");
    f.write("<br clear=\"all\"><hr>\n");
    f.write("<small><a href=\"index.html\">efa - elektronisches Fahrtenbuch</a>,\n");
    f.write("Copyright &copy; 2001-09 by <a href=\"http://www.nmichael.de/\" target=\"_new\">Nicolas Michael</a>\n");
    f.write("&lt;<a href=\"mailto:info@efa.nmichael.de\">info@efa.nmichael.de</a>&gt;</small>\n");
    f.write("</body>\n");
    f.write("</html>\n");
    f.close();
  }

  static void readInput(String dat) throws IOException {
    BufferedReader f = new BufferedReader(new FileReader(dat));
    String s;

    Vector files = new Vector();

    int mode = NICHTS;
    String datei = null;
    String titel = null;
    Vector beschreibung = null;
    Vector erklaerung = null;
    Vector siehe = null;

    while ( (s = f.readLine()) != null) {
      s = s.trim();
      if (s.length()==0) continue;

      if (s.startsWith("@")) {
        if (s.toLowerCase().startsWith("@datei=")) datei = s.substring(7,s.length());
        if (s.toLowerCase().startsWith("@titel=")) titel = s.substring(7,s.length());
        if (s.toLowerCase().startsWith("@beschreibung")) {
          mode = BESCHREIBUNG;
          beschreibung = new Vector();
        }
        if (s.toLowerCase().startsWith("@erklaerung")) {
          mode = ERKLAERUNG;
          erklaerung = new Vector();
        }
        if (s.toLowerCase().startsWith("@siehe")) {
          mode = SIEHE;
          siehe = new Vector();
        }
        if (s.toLowerCase().startsWith("@ende")) {
          writeFile(datei,titel,beschreibung,erklaerung,siehe);
          files.add(titel+"|"+datei);
          datei = titel = null;
          beschreibung = erklaerung = siehe = null;
        }
        if (s.toLowerCase().startsWith("@gruppe=")) {
          files.add(s);
        }
      } else {
        switch(mode) {
          case BESCHREIBUNG:
               beschreibung.add(s);
               break;
          case ERKLAERUNG:
               erklaerung.add(s);
               break;
          case SIEHE:
               siehe.add(s);
               break;
        }
      }
    }

    files.add("@"); // Dummy Element
    writeIndex(files);
  }

  public static void writeIndex(Vector v) throws IOException {
    BufferedWriter f = new BufferedWriter(new FileWriter("Frames.html"));
    f.write("<html>\n");
    f.write("<head>\n");
    f.write("<title>efa - elektronisches Fahrtenbuch</title>\n");
    f.write("</head>\n");
    f.write("<body bgcolor=\"#ffffff\">\n");
    f.write("<h1 align=\"center\">Hilfeseiten zu den efa-Fenstern</h1>\n");
    f.write("<p>Um eine dieser Hilfeseiten direkt aus efa heraus aufzurufen, drï¿½cke in dem entsprechenden Fenster &lt;F1&gt;. Du erhï¿½lst dann die Hilfeseite fï¿½r das aktuelle Fenster.</p>\n");
    f.write("\n");

    Hashtable h = null;
    for (int i=0; i<v.size(); i++) {
      String s = (String)v.get(i);
      if (s.startsWith("@")) {
        if (h != null) {
          Object[] a = h.keySet().toArray();
          Arrays.sort(a,0,a.length);
          f.write("<ul>\n");
          for (int j=0; j<a.length; j++) {
            f.write("  <li><a href=\""+h.get(a[j])+"\">"+a[j]+"</a></li>\n");
          }
          f.write("</ul>\n");
          h = null;
        }
        if (s.startsWith("@gruppe=")) {
          f.write("<h3>"+s.substring(8)+"</h3>\n");
          h = new Hashtable();
        }
      } else {
        if (h != null) h.put(split(s,1),split(s,2));
      }
    }



    f.write("<p><a href=\"index.html\">Zurï¿½ck zur ï¿½bersicht</a></p>\n");
    f.write("<br><hr>\n");
    f.write("<small><a href=\"index.html\">efa - elektronisches Fahrtenbuch</a>,\n");
    f.write("Copyright &copy; 2001-09 by <a href=\"http://www.nmichael.de/\" target=\"_new\">Nicolas Michael</a>\n");
    f.write("&lt;<a href=\"mailto:info@efa.nmichael.de\">info@efa.nmichael.de</a>&gt;</small>\n");
    f.write("</body>\n");
    f.write("</html>\n");
    f.close();
 }

  public static void main(String[] args) {
    try {
      readInput(args[0]);
      System.exit(0);
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}