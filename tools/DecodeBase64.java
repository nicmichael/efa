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

import de.nmichael.efa.util.Base64;
import java.io.*;

// @i18n complete

public class DecodeBase64 {

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("usage: DecodeBase64 <base64 encoded input file> <decoded output file>");
      System.exit(1);
    }
    try {
      BufferedReader f = new BufferedReader(new FileReader(args[0]));
      String z;
      String data="";
      while ( (z = f.readLine()) != null) {
        data += z;
      }
      f.close();
      byte[] buf = Base64.decode(data);
      FileOutputStream ff = new FileOutputStream(args[1]);
      ff.write(buf);
      ff.close();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}