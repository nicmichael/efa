/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.statistics;

import de.nmichael.efa.*;
import java.io.*;

// @i18n complete

public class FTPWriter {

  StatistikDaten sd;

  public FTPWriter(StatistikDaten sd) {
    this.sd = sd;
  }


  public String run() {
    try {
      com.enterprisedt.net.ftp.FTPClient ftpClient = new com.enterprisedt.net.ftp.FTPClient(sd.ftpServer);
      ftpClient.setConnectMode(com.enterprisedt.net.ftp.FTPConnectMode.ACTIVE);
      ftpClient.login(sd.ftpUser,sd.ftpPassword);
      ftpClient.chdir(sd.ftpDirectory);
      ftpClient.put(sd.ausgabeDatei,sd.ftpFilename);
      ftpClient.quit();
      return null; // korrektes Ende!
    } catch(Exception e) {
      return e.toString();
    }
  }

}