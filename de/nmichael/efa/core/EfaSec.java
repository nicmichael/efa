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
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import java.io.*;

// @i18n complete
public class EfaSec {

    private String filename;

    public EfaSec(String filename) {
        this.filename = filename;
    }

    public boolean secFileExists() {
        return (new File(filename)).isFile();
    }

    private String read() {
        try {
            BufferedReader fsec = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Daten.ENCODING_UTF));
            String efaSecSHA = fsec.readLine();
            fsec.close();
            return efaSecSHA;
        } catch (Exception e) {
            return null;
        }
    }

    public String getSecValue() {
        String efaSecSHA = read();
        if (efaSecSHA != null && efaSecSHA.startsWith("#")) {
            efaSecSHA = efaSecSHA.substring(1, efaSecSHA.length());
        }
        return efaSecSHA;
    }

    public boolean isDontDeleteSet() {
        String efaSecSHA = read();
        return (efaSecSHA != null && efaSecSHA.startsWith("#"));
    }

    public boolean isSecFileWritable() {
        try {
            BufferedReader fsecr = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Daten.ENCODING_UTF));
            String efaSecSHA = fsecr.readLine();
            fsecr.close();
            BufferedWriter fsecw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), Daten.ENCODING_UTF));
            fsecw.write(efaSecSHA);
            fsecw.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean secValueValid() {
        // in efa2, we don't care whether it's valid or not... just whether it's there
        //String efaJarSHA = EfaUtil.getSHA(new File(Daten.efaProgramDirectory + Daten.EFA_JAR));
        //String efaSecSHA = getSecValue();
        //return efaJarSHA != null && efaSecSHA != null && efaJarSHA.equals(efaSecSHA);
        return true;
    }

    public boolean writeSecFile(String sha, boolean dontDelete) {
        try {
            BufferedWriter fsec = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), Daten.ENCODING_UTF));
            fsec.write((dontDelete ? "#" : "") + sha);
            fsec.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean delete(boolean force) {
        if (force || !isDontDeleteSet()) {
            boolean secDeleted = false;
            try {
                secDeleted = (new File(filename)).delete();
            } catch (Exception e) {
                secDeleted = false;
            }
            return secDeleted;
        }
        return true;
    }

    public String getFilename() {
        return filename;
    }

    public static boolean createNewSecFile(String secFile, String jarFile) {
        EfaSec efaSec = null;
        try {
            String sha = EfaUtil.getSHA(new File(jarFile));
            efaSec = new EfaSec(secFile);
            efaSec.writeSecFile(sha, false);
            Logger.log(Logger.INFO, Logger.MSG_CORE_EFASECCREATED, LogString.fileNewCreated(efaSec.getFilename(), International.getString("Sicherheitsdatei")));
            return true;
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_CORE_EFASECFAILEDCREATE, LogString.fileCreationFailed(efaSec.getFilename(), International.getString("Sicherheitsdatei"), e.toString()));
            return false;
        }
    }

}
