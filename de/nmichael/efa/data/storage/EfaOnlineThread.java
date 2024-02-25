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
import de.nmichael.efa.util.Logger;
import java.io.*;
import java.net.*;

public class EfaOnlineThread extends Thread {

    public static final String URL_STATUS = "status.php";

    private void updateStatus(String baseurl, String username, String password, int port) {
        try {
            URL url = new URL(baseurl + "/" + URL_STATUS);
            String request = "username="+username+"&password="+password+"&port="+port;
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
            int pos;
            String errormsg = "";
            while ( (s = in.readLine()) != null) {
                if (s.indexOf("RESULT:OK") >= 0) {
                    ok = true;
                }
                if (s.indexOf("RESULT:ERROR:") >= 0) {
                    errormsg = s.substring(13);
                }
                if ( (pos = s.indexOf("IPADDR:")) >= 0) {
                    ipaddr = s.substring(pos+7);
                }
            }
            if (!ok) {
                Logger.log(Logger.WARNING, Logger.MSG_EONL_WARNING, "efaOnline: Status Update failed: " + errormsg);
            } else {
                if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 1)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_EONL_DEBUG, "efaOnline: Status Update successful: IPADDR="+ipaddr+" PORT="+port);;
                }
            }
        } catch(Exception e) {
            Logger.log(Logger.WARNING, Logger.MSG_EONL_WARNING, "efaOnline status update failed: " + e.getMessage());
            Logger.logdebug(e);
        }
    }

    public void run() {
    	this.setName("EfaOnlineThread");
        while(true) {
            try {
                if (Daten.efaConfig != null ? Daten.efaConfig.getValueDataRemoteEfaOnlineEnabled() : false) {
                    String username = (Daten.efaConfig != null ? Daten.efaConfig.getValueDataRemoteEfaOnlineUsername() : null);
                    String password = (Daten.efaConfig != null ? Daten.efaConfig.getValueDataRemoteEfaOnlinePassword() : null);
                    String url = (Daten.efaConfig != null ? Daten.efaConfig.getValueDataRemoteEfaOnlineUrl() : null);
                    int remoteEfaPort = (Daten.efaConfig != null ? Daten.efaConfig.getValueDataataRemoteEfaServerPort() : -1);
                    if (username == null || username.length() == 0 ||
                        password == null || password.length() == 0 ||
                        url == null || url.length() == 0) {
                        Logger.log(Logger.ERROR, Logger.MSG_EONL_ERROR, "efaOnline misconfiguration");
                    } else {
                        updateStatus(url, username, password, remoteEfaPort);
                    }
                }
                try {
                    long sec = (Daten.efaConfig != null ? Daten.efaConfig.getValueDataRemoteEfaOnlineUpdateInterval() : 3600);
                    Thread.sleep(sec * 1000);
                } catch(InterruptedException eintr) {
                }
            } catch(Exception e) {
                Logger.logdebug(e);
            }
        }
    }

}
