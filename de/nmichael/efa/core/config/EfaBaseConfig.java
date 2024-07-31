/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.core.config;

import de.nmichael.efa.*;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.EfaUtil;
import java.io.*;

// @i18n complete

public class EfaBaseConfig {

    public static final String FIELD_USERHOME = "USERHOME";
    public static final String FIELD_LANGUAGE = "LANGUAGE";
    public static final String FIELD_VERSION  = "VERSION";

    private String filename;
    public String efaUserDirectory; // Verzeichnis für alle User-Daten von efa (daten, cfg, tmp)
    public String language;         // Sprache
    public String version;          // only used to distinguish between efa1 and efa2

    private String normalize(String sin) {
        String sout = "";
        for (int i = 0; sin != null && i < sin.length(); i++) {
            char c = sin.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sout += c;
            }
        }
        return sout;
    }

    public void setEfaConfigUserHomeFilename(String dir) {
        filename = dir + ".efa_" + normalize(Daten.efaMainDirectory);
    }

    // Konstruktor
    public EfaBaseConfig(String dir) {
        setEfaConfigUserHomeFilename(dir);
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "EfaBaseConfig(): filename=" + filename);
        }
        reset();
    }

    public boolean efaCanWrite(String path, boolean createDir) {
        if (!path.endsWith(Daten.fileSep)) {
            path += Daten.fileSep;
        }
        String testfile = "efa.test.file";
        try {
            if (createDir) {
                File f = new File(path);
                f.mkdirs();
            }
            BufferedWriter f = new BufferedWriter(new FileWriter(path + testfile));
            f.write("efa can write!");
            f.close();
            EfaUtil.deleteFile(path + testfile);
        } catch (Exception e) {
            if (Logger.isTraceOn(Logger.TT_CORE)) {
                Logger.log(Logger.DEBUG, Logger.MSG_CORE_BASICCONFIG, "efaCanWrite(" + path + ") = false: " + e.toString());
            }
            return false;
        }
        return true;
    }

    public boolean trySetUserDir(String dir, boolean createDir) {
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "trySetUserDir(" + dir + "," + createDir+")");
        }
        if (dir == null || dir.length() == 0) {
            return false;
        }
        dir = dir.trim();
        if (!dir.endsWith(Daten.fileSep)) {
            dir = dir + Daten.fileSep;
        }
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "trySetUserDir(): efa.dir.user=" + efaUserDirectory);
        }
        if (efaCanWrite(dir, createDir)) {
            efaUserDirectory = dir;
            if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
                Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "trySetUserDir(): can write, setting efa.dir.user=" + efaUserDirectory);
            }
            return true;
        }
        if (Logger.isTraceOn(Logger.TT_CORE, 9) || Logger.isDebugLoggingActivatedByCommandLine()) {
            Logger.log(Logger.DEBUG, Logger.MSG_CORE_STARTUPINITIALIZATION, "trySetUserDir(): cannot write: " + dir);
        }
        return false;
    }

    public String getFileName() {
        return filename;
    }

    // Einstellungen zurücksetzen
    void reset() {
        language = null;
        version = null;
        efaUserDirectory = Daten.userHomeDir + (!Daten.userHomeDir.endsWith(Daten.fileSep) ? Daten.fileSep : "") + Daten.EFA_USERDATA_DIR + Daten.fileSep;
        if (!trySetUserDir(efaUserDirectory, true)) {
            efaUserDirectory = Daten.efaMainDirectory;
            if (!trySetUserDir(efaUserDirectory, false)) {
                efaUserDirectory = null;
                if (Logger.isTraceOn(Logger.TT_CORE) || Logger.isDebugLoggingActivatedByCommandLine()) {
                    Logger.log(Logger.DEBUG, Logger.MSG_CORE_BASICCONFIG, "efa.dir.user=<null>");
                }
            }
        }
    }

    public synchronized boolean readFile() {
        reset();

        // Konfiguration lesen
        BufferedReader f = null;
        String s;
        try {
            f = new BufferedReader(new InputStreamReader(new FileInputStream(filename),Daten.ENCODING_UTF));
            while ((s = f.readLine()) != null) {
                s = s.trim();
                if (s.startsWith(FIELD_USERHOME + "=")) {
                    String newUserHome = s.substring(FIELD_USERHOME.length()+1, s.length()).trim();
                    if (efaCanWrite(newUserHome, true)) {
                        efaUserDirectory = newUserHome;
                        if (!efaUserDirectory.endsWith(Daten.fileSep)) {
                            efaUserDirectory += Daten.fileSep;
                        }
                    } else {
                        Logger.log(Logger.ERROR, Logger.MSG_CORE_BASICCONFIGUSERDATACANTWRITE,
                                "Cannot write in configured User Data Directory: " + newUserHome +
                                "; using " + efaUserDirectory + " instead.");
                    }
                }
                if (s.startsWith(FIELD_LANGUAGE+"=")) {
                    language = s.substring(FIELD_LANGUAGE.length()+1).trim();
                }
                if (s.startsWith(FIELD_VERSION+"=")) {
                    version = s.substring(FIELD_VERSION.length()+1).trim();
                }
            }
            f.close();
        } catch (IOException e) {
            Logger.log(e);
            try {
                f.close();
            } catch (Exception ee) {
                return false;
            }
        }
        return true;
    }

    // Konfigurationsdatei speichern
    public synchronized boolean writeFile() {
        // Datei schreiben
        BufferedWriter f = null;
        try {
            f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,false), Daten.ENCODING_UTF ));
            if (efaUserDirectory != null && efaUserDirectory.length() > 0) {
                f.write(FIELD_USERHOME + "=" + efaUserDirectory + "\n");
            }
            if (language != null) {
                f.write(FIELD_LANGUAGE + "=" + language + "\n");
            }
            f.write(FIELD_VERSION + "=" + Daten.MAJORVERSION + "\n");
            f.close();
        } catch (Exception e) {
            Logger.log(e);
            try {
                f.close();
            } catch (Exception ee) {
                return false;
            }
            return false;
        }
        return true;
    }

}
