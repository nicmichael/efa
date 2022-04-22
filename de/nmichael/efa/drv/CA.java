/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.drv;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.EnterPasswordDialog;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.io.*;
import java.lang.reflect.Method;

// @i18n complete (needs no internationalization -- only relevant for Germany)

public class CA {

  public static final String DRVCA = "drvca";

  public CA() throws Exception {
    if (!(new File(Daten.efaDataDirectory+"CA").isDirectory())) throw new Exception("Verzeichnis "+Daten.efaDataDirectory+"CA existiert nicht. Bitte erstelle zunächst eine CA!");

    if (Daten.keyStore.getCertificate(DRVCA) == null) {
      runKeytool("-import"+
                 " -alias "+DRVCA+
                 " -file "+Daten.efaDataDirectory+"CA"+Daten.fileSep+"cacert.pem" +
                 " -trustcacerts -noprompt",null);
    }
  }

    public boolean runKeytool(String cmd, char[] keypass) {
        boolean runExec = true;

        String exec =(runExec ? System.getProperty("java.home") + Daten.fileSep + "bin" + Daten.fileSep + "keytool " : "" );
        String showCmd = exec + cmd + " -keystore " + Daten.efaDataDirectory + Main.drvConfig.KEYSTORE_FILE
                + (keypass != null ? " -keypass ***" : "")
                + " -storepass ***";
        cmd = exec + cmd + " -keystore " + Daten.efaDataDirectory + Main.drvConfig.KEYSTORE_FILE
                + (keypass != null ? " -keypass " + new String(keypass) : "")
                + " -storepass " + new String(Main.drvConfig.keyPassword);
        Logger.log(Logger.INFO, "Starte Keytool in Java Version " + Daten.javaVersion + ": " + showCmd);
        String[] cmdarr = EfaUtil.kommaList2Arr(cmd, ' ');
        for (int i = 0; i < cmdarr.length; i++) {
            cmdarr[i] = EfaUtil.replace(cmdarr[i], "\\s", " ", true);
        }
        try {
            if (runExec) {
                if (!EfaUtil.execCmd(cmd)) {
                    Logger.log(Logger.ERROR, "Failed to run keytool: " + showCmd);
                    Dialog.error("Failed to run keytool: " + showCmd);
                }
            } else {
                // up to Java 7:
                //sun.security.tools.KeyTool.main(cmdarr);
                // new with support for both Java < 8 and Java > 8
                try {
                    // try Java 7 class name
                    Logger.log(Logger.INFO, "Starte Keytool fuer Java 7: " + showCmd);
                    String classname = "sun.security.tools.KeyTool";
                    Class[] args = new Class[1];
                    Class.forName(classname).getMethod("main", String[].class).invoke(null, cmdarr);
                } catch (Throwable t) {
                    // try Java 8 class name
                    Logger.log(Logger.INFO, "Starte Keytool fuer Java 8: " + showCmd);
                    String classname = "sun.security.tools.keytool.Main";
                    Class[] args = new Class[1];
                    Class c = Class.forName(classname);
                    Method m = c.getMethod("main", String[].class);
                    m.invoke(null, cmdarr);
                }
            }
        } catch (Throwable ex) {
            Logger.log(Logger.ERROR, "Konnte Keytool nicht starten: " + ex);
            Dialog.error("Konnte Keytool nicht starten: " + ex);
            return false;
        }
        return true;
    }

  private void printOutput(String stream, InputStream in) {
    try {
      byte[] arr = new byte[16384];
      while (in != null && in.available() > 0) {
        in.read(arr,0,in.available());
        Logger.log(Logger.INFO,"  "+stream+": "+new String(arr));
      }
    } catch(Exception e) {}
  }

  public boolean signRequest(String req, String sigReq, int tage) {
    if (Main.drvConfig.openssl == null || Main.drvConfig.openssl.length() == 0) {
      Dialog.error("Openssl ist nicht konfiguriert!");
      return false;
    }
    try {
      String pwd = EnterPasswordDialog.enterPassword(Dialog.frameCurrent(),
              "Bitte Schlüssel-Paßwort für CA eingeben:", false);
      if (pwd == null) return false;
      String openssl = Main.drvConfig.openssl;
      String sigReqTmp = sigReq+".tmp";
      String cmd = openssl + " ca -config "+Daten.efaDataDirectory+"CA"+Daten.fileSep+"openssl.cnf -policy policy_anything -in "+req+" -out "+sigReqTmp+" -batch -days "+tage+" -key ";
      Logger.log(Logger.INFO,"Starte OpenSSL: "+cmd+"***");
      cmd += pwd;
      Process p = Runtime.getRuntime().exec(cmd,null,new File(Daten.efaDataDirectory+"CA"));
      InputStream stdout = p.getInputStream();
      InputStream stderr = p.getErrorStream();
      try {
        Thread.sleep(1000);
      } catch(InterruptedException ee) { EfaUtil.foo(); }
      printOutput("stdout",stdout);
      printOutput("stderr",stderr);
      p.waitFor();
      if (!EfaUtil.canOpenFile(sigReqTmp)) {
        Dialog.error("Zertifizierungsrequest konnte von openssl nicht erstellt werden.");
        return false;
      }
      BufferedReader f = new BufferedReader(new FileReader(sigReqTmp));
      BufferedWriter ff = new BufferedWriter(new FileWriter(sigReq));
      String s;
      boolean start = false;
      while ( (s = f.readLine()) != null) {
        if (s.startsWith("-----BEGIN CERTIFICATE-----")) start = true;
        if (start) ff.write(s+"\n");
      }
      f.close();
      ff.close();
      (new File(sigReqTmp)).delete();
      return start;
    } catch(Exception e) {
      Dialog.error("Fehler: "+e.toString());
      return false;
    }
  }
}