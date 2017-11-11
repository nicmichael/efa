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

class Kapitel {
  String name;
  int topics;
}

public class TourGenerator {

  static Vector kapitel;
  static BufferedWriter allPages = null;


  static void writeFile(String datei, Vector txt) throws IOException {
    if (datei == null) return;

    if (allPages == null) {
      allPages = new BufferedWriter(new FileWriter("tourall.html"));
      allPages.write("<html>\n<head>\n<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n<title>efa Tour</title>\n</head>\n<body>\n");
    }

    BufferedWriter f = new BufferedWriter(new FileWriter(datei));
    f.write("<html>\n<head>\n<title>efa Tour</title>\n</head>\n<body>\n");
    for (int i=0; i<txt.size(); i++) {
      f.write((String)txt.get(i));
      allPages.write((String)txt.get(i));
    }
    f.write("</body>\n</html>\n");
    f.close();
  }

  static void analyzeInput(String datei) throws IOException {
    BufferedReader f = new BufferedReader(new FileReader(datei));
    String s;

    kapitel = new Vector();

    Kapitel kap = null;

    while ( (s = f.readLine()) != null) {
      s = s.trim();
      if (s.length() == 0) continue;

      if (s.startsWith("@kapitel=")) {
        if (kap != null) kapitel.add(kap);
        kap = new Kapitel();
        kap.name = s.substring(9,s.length());
        kap.topics = 0;
      }

      if (kap != null && s.startsWith("@start")) kap.topics++;

    }
    if (kap != null) kapitel.add(kap);

    f.close();
  }

  static void readInput(String dat) throws IOException {
    BufferedReader f = new BufferedReader(new FileReader(dat));

    analyzeInput(dat);

    String s;
    int mode = 0;
    String datei = null;
    Vector txt = null;
    Kapitel kap = null;

    int k=0; // Kapitel
    int t=0; // Topic

    while ( (s = f.readLine()) != null) {
      s = s.trim();
      if (s.length() == 0) continue;

      if (s.equals("@index")) mode = 1;

      if (s.startsWith("@kapitel=")) {
        mode=2;
        t=0;
        kap = (Kapitel)kapitel.get(k++);
      }

      if (s.equals("@start")) {
        t++;
        if (mode == 1) datei = "index.html";
        if (mode == 2) datei = "k" + (k<10?"0":"") + k + "-" + (t<100?"0":"") + (t<10?"0":"") + t + ".html";
        txt = new Vector();

        System.out.println("Schreibe Kapitel "+k+", Topic "+t+"; Datei: "+datei);
      }

      if (s.startsWith("@titel=")) {
        if (k > 0) {
          txt.add("<table width=\"100%\"><tr>");
          txt.add("<td>[Kap "+k+" Seite "+t+"/"+kap.topics+"]</td>\n");
          if (mode == 2) {
            if (t == kap.topics) { // Kapitelende
              if (k < kapitel.size()) txt.add("<td align=\"right\"><a href=\"k"+(k+1<10?"0":"")+(k+1)+"-001.html\">Weiter</a></td>\n");
            } else txt.add("<td align=\"right\"><a href=\"k" + (k<10?"0":"") + k + "-" + (t+1<100?"0":"") + (t+1<10?"0":"") + (t+1) + ".html\">Weiter</a></td>");
          }
          txt.add("</tr></table>\n");
        }
        txt.add("<h1 align=\"center\">"+s.substring(7,s.length())+"</h1>\n");
//        if (k > 0) txt.add("<p align=\"right\">[Kap "+k+" Seite "+t+"/"+kap.topics+"]</p>\n");
      }

      if (s.equals("@kapitelliste")) {
        txt.add("<ul>\n");
        for (int i=0; i<kapitel.size(); i++) {
          Kapitel kk = (Kapitel)kapitel.get(i);
          txt.add("  <li><a href=\"k" + (i+1<10?"0":"") + (i+1) + "-001.html\">Kapitel "+(i+1)+": "+kk.name+"</a></li>\n");
        }
        txt.add("</ul>\n");
      }

      if (s.equals("@ende")) {
        if (mode == 2) {
          if (t == kap.topics) { // Kapitelende
            if (k < kapitel.size()) {
              txt.add("<p><a href=\"k"+(k+1<10?"0":"")+(k+1)+"-001.html\">Weiter mit Kapitel "+(k+1)+": "+ ((Kapitel)kapitel.get(k)).name +"</a></p>\n");
            }
            txt.add("<p><a href=\"index.html\">Zurück zur Kapitelübersicht</a></p>\n");
          } else {
            txt.add("<p><a href=\"k" + (k<10?"0":"") + k + "-" + (t+1<100?"0":"") + (t+1<10?"0":"") + (t+1) + ".html\">Weiter</a></p>\n");
          }
        }
        writeFile(datei,txt);
        txt = null;
      }

      if (!s.startsWith("@") && txt != null) txt.add(s+"\n");
    }
    f.close();
    allPages.close();
  }

/*
  static void readInput(String dat) throws IOException {
    BufferedReader f = new BufferedReader(new FileReader(dat));
    BufferedWriter f2 = new BufferedWriter(new FileWriter("tour2.txt"));
    String s;

    boolean copycontent = false;

    while ( (s = f.readLine()) != null) {
      s = s.trim();
      if (s.length() == 0) continue;

      System.out.println(s);

      if (s.toLowerCase().equals("<html>")) f2.write("@start\n");
      if (s.startsWith("<h1 align=\"center\">")) {
        f2.write("@titel="+s.substring(19,s.indexOf("</h1>"))+"\n");
        copycontent = true;
        continue;
      }
      if (s.indexOf("Weiter</a></p>")>0) copycontent = false;
      if (s.equals("</html>")) {
        copycontent = false;
        f2.write("@ende\n\n");
      }
      if (copycontent) f2.write(s+"\n");
    }
    f.close();
    f2.close();
  }
*/

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