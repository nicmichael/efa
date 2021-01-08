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

// @i18n complete (needs no translation, just for developers)

public class Unixlf2doslf {

  public static boolean copyFile(String quelle, String ziel) {
    FileInputStream f1;
    FileOutputStream f2;
    int c,clast;
    try {
      f1 = new FileInputStream(quelle);
      f2 = new FileOutputStream(ziel);
      clast = 0;
      while ( (c = f1.read()) >= 0) {
          // Standard DOS Format is: \r \n (0x0d 0x0a)
        if (c == 10 && clast!= 13) f2.write(13);
        f2.write(c);
      }
      f1.close();
      f2.close();
    } catch(IOException e) {
      return false;
    }
    return true;
  }


  public static void main(String[] args) {
    if (args.length != 2) {
        System.err.println("usage: Unix2doslf <in> <out>");
        System.exit(1);
    } else if (copyFile(args[0],args[1])) {
        System.out.println("done");
        System.exit(0);
    } else {
        System.err.println("error");
        System.exit(2);
    }
  }
}