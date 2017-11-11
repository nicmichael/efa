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

import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.core.EfaSec;
import de.nmichael.efa.*;
import java.io.*;

// @i18n complete (needs no translation, just for developers)

public class SecFileCreator {

  // usage: SecFileCreator <cfgDir> <efa.jar>
  public static void main(String[] args) {
    try {
      String secFile = args[0]+"/"+Daten.EFA_SECFILE;
      String efaJar = args[1];
      String sha = EfaUtil.getSHA(new File(efaJar));
      EfaSec efaSec = new EfaSec(secFile);
      efaSec.writeSecFile(sha,false);
      System.out.println("Success: SHA from "+efaJar+" written to "+secFile+". Value="+sha);
      System.exit(0);
    } catch(Exception e) {
      System.out.println("Error: "+e.toString());
      System.exit(1);
    }
  }
}