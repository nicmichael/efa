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

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class Credentials {

    private String filename;
    private Hashtable<String,String> credentials = new Hashtable<String,String>();
    private String defaultAdmin = null;

    public Credentials() {
        this.filename = Daten.EFACREDFILE;
    }

    //public Credentials(String filename) {
    //    this.filename = filename;
    //}

    public String getPassword(String username) {
        return credentials.get(username);
    }

    public String getDefaultAdmin() {
        return defaultAdmin;
    }

    public void addCredentials(String username, String password) {
        credentials.put(username, password);
    }

    public boolean readCredentials() {
        credentials = new Hashtable<String,String>();
        defaultAdmin = null; // Reset defaultAdmin to null before reading new credentials if credentials file is read again
        try {
            if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_CORE_CREDENTIALS, "Trying to access credentials file: " + filename);
            }

            File credfile = new File(filename);
            if (credfile.exists()) {
                int lineCount = 0;
                int commentLineCount = 0;

                // try-with-resources statement to ensure the BufferedReader is closed automatically
                try (BufferedReader f = new BufferedReader(
                        new InputStreamReader(new FileInputStream(filename), Daten.ENCODING_UTF))) {

                    String s;
                    while ((s = f.readLine()) != null) {
                        lineCount++;

                        // Normalize whitespace: replace tabs with spaces, trim leading/trailing whitespace,
                        // and replace multiple spaces with a single space
                        s = s.replaceAll("\t", " ").trim();
                        s = s.replaceAll(" +", " ");

                        if (s.startsWith("#") || s.length() == 0) {
                            commentLineCount++;
                            continue;
                        }

                        StringTokenizer tok = new StringTokenizer(s, " ");
                        if (tok.countTokens() >= 2) {
                            String username = tok.nextToken();
                            String password = tok.nextToken();
                            if (username.startsWith("+")) {
                                username = username.substring(1);
                                defaultAdmin = (defaultAdmin == null ? username : defaultAdmin);
                            }
                            if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                                Logger.log(Logger.DEBUG, Logger.MSG_CORE_CREDENTIALS, "found credentials for: " + username
                                        + (s.startsWith("+") ? " (default)" : ""));
                            }
                            credentials.put(username, password);
                        }
                    }
                } catch (Exception e) {
                    Logger.logdebug(e);
                    return false;
                }

                if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_CORE_CREDENTIALS, "Total number of lines in credentials file: " + lineCount);
                    Logger.log(Logger.DEBUG, Logger.MSG_CORE_CREDENTIALS, "Number of comment/empty lines in credentials file: " + commentLineCount);
                }
                return true;
            } else {
                if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_CORE_CREDENTIALS, "credentials file not found: " + filename);
                }
                return false;
            }
        } catch (Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }


    public boolean writeCredentials() {
        try {
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(filename), Daten.ENCODING_UTF));
            String[] usernames = credentials.keySet().toArray(new String[0]);
            for (String username : usernames) {
                if (defaultAdmin != null && defaultAdmin.equals(username)) {
                    f.write("+" + username + " " + credentials.get(username) + "\n");
                } else {
                    f.write(username + " " + credentials.get(username) + "\n");
                }
            }
            f.close();
            return true;
        } catch (Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }
}
