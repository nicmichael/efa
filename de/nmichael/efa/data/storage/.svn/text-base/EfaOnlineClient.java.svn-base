/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.storage;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import java.io.*;
import java.net.*;

public class EfaOnlineClient {

    public static final String URL_GETADDR = "getaddr.php";

    public static String getRemoteAddress(String username, String password) {
        try {
            String baseurl = Daten.efaConfig.getValueDataRemoteEfaOnlineUrl();
            URL url = new URL(baseurl + "/" + URL_GETADDR);
            String request = "username="+username+"&password="+password;
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(request);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String s;
            boolean ok = false;
            String ipaddr = null;
            String port = null;
            String error = null;
            int pos;
            while ( (s = in.readLine()) != null) {
                if (s.indexOf("RESULT:OK") >= 0) {
                    ok = true;
                }
                if ( (pos = s.indexOf("RESULT:ERROR")) >= 0) {
                    error = s.substring(pos+7);
                }
                if ( (pos = s.indexOf("IPADDR:")) >= 0) {
                    ipaddr = s.substring(pos+7);
                }
                if ( (pos = s.indexOf("PORT:")) >= 0) {
                    port = s.substring(pos+5);
                }
            }
            if (!ok) {
                Logger.log(Logger.ERROR, Logger.MSG_EONL_WARNING, "efaOnline: " + International.getString("Konnte Remote-Adresse nicht ermitteln") + ": " +
                        error);
                return null;
            } else {
                return ipaddr + ":" + port;
            }
        } catch(Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_EONL_WARNING, "efaOnline: " + International.getString("Konnte Remote-Adresse nicht ermitteln") + ": " +
                    e.getMessage());
            Logger.logdebug(e);
            return null;
        }
    }

}
