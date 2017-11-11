/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.util;

import com.enterprisedt.net.ftp.FTPTransferType;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.gui.MultiInputDialog;
import javax.swing.JDialog;

// @i18n complete
public class FTPClient {

    private String ftpString;
    private String server;
    private String username;
    private String password;
    private String localFileWithPath;
    private String remoteDirectory;
    private String remoteFile;
    private boolean validFormat = false;
    private boolean pasv = false;
    
    public static boolean isFTP(String s) {
        return s != null && (s.toLowerCase().startsWith("ftp:") ||
                             s.toLowerCase().startsWith("ftp-pasv:") );
    }

    public FTPClient(String ftpString, String localFileWithPath) {
        this.ftpString = ftpString;
        this.localFileWithPath = localFileWithPath;
        if (isFTP(ftpString)) {
            if (ftpString.toLowerCase().startsWith("ftp:")) {
                ftpString = ftpString.substring(4);
            }
            if (ftpString.toLowerCase().startsWith("ftp-pasv:")) {
                ftpString = ftpString.substring(9);
                pasv = true;
            }
            int pos = ftpString.indexOf(":");
            if (pos >= 0) {
                username = ftpString.substring(0, pos);
                ftpString = ftpString.substring(pos + 1);
                pos = ftpString.indexOf("@");
                if (pos >= 0) {
                    password = ftpString.substring(0, pos);
                    ftpString = ftpString.substring(pos + 1);
                    pos = ftpString.indexOf(":");
                    if (pos >= 0) {
                        server = ftpString.substring(0, pos);
                        ftpString = ftpString.substring(pos + 1);
                        pos = ftpString.lastIndexOf("/");
                        if (pos >= 0) {
                            remoteDirectory = ftpString.substring(0, pos);
                            if (remoteDirectory == null || remoteDirectory.length() == 0) {
                                remoteDirectory = "/";
                            }
                            remoteFile = ftpString.substring(pos + 1);
                            validFormat = true;
                        }
                    }
                }
            }
        }
        if (Logger.isTraceOn(Logger.TT_PRINT_FTP, 5)) {
            printDebugSummary();
        }
    }

    public FTPClient(String server, String username, String password,
            String localFileWithPath, String remoteDirectory, String remoteFile) {
        this.ftpString = username + ":" + password + "@" + server + ":" + remoteDirectory + 
                (remoteDirectory != null && remoteDirectory.endsWith("/") ? "" : "/") + remoteFile;
        this.server = server;
        this.username = username;
        this.password = password;
        this.localFileWithPath = localFileWithPath;
        this.remoteDirectory = remoteDirectory;
        this.remoteFile = remoteFile;
        validFormat = true;
        if (Logger.isTraceOn(Logger.TT_PRINT_FTP, 5)) {
            printDebugSummary();
        }
    }

    private void printDebugSummary() {
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_FTP, "FTP Upload:");
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_FTP, "ftpString: " + ftpString);
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_FTP, "localFileWithPath: " + localFileWithPath);
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_FTP, "server: " + server);
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_FTP, "username: " + username);
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_FTP, "password: " + "<" +
                (password == null ? "null" : password.length() + " characters") + ">");
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_FTP, "remoteDirectory: " + remoteDirectory);
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_FTP, "remoteFile: " + remoteFile);
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_FTP, "validFormat: " + validFormat);
    }

    public boolean isValidFormat() {
        return validFormat;
    }

    public String getFtpString() {
        return ftpString;
    }

    public String getFtpString(boolean withPassword) {
        return (pasv ? "ftp-pasv:" : "ftp:") + 
                username + (withPassword ? ":" + password : "") +
                "@" + server + ":" + remoteDirectory + 
                (remoteDirectory.endsWith("/") ? "" : "/") + remoteFile;
    }

    public String getServer() {
        return server;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFile() {
        return (remoteDirectory != null && remoteFile != null ? remoteDirectory + "/" + remoteFile : null);
    }
    
    public boolean isPasv() {
        return pasv;
    }

    public String runUpload() {
        try {
            com.enterprisedt.net.ftp.FTPClient ftpClient = new com.enterprisedt.net.ftp.FTPClient(server);
            if (server == null) {
                return null; // happens for test whether FTP Plugin in installed
            }
            ftpClient.setConnectMode(pasv ? com.enterprisedt.net.ftp.FTPConnectMode.PASV :
                                            com.enterprisedt.net.ftp.FTPConnectMode.ACTIVE);
            ftpClient.login(username, password);
            if (remoteDirectory != null) {
                ftpClient.chdir( (remoteDirectory.length() == 0 ? "/" : remoteDirectory) );
            }
            ftpClient.setType(FTPTransferType.BINARY);
            ftpClient.put(localFileWithPath, remoteFile);
            ftpClient.quit();
            return null; // korrektes Ende!
        } catch (Exception e) {
            Logger.logdebug(e);
            return e.toString();
        }
    }

    public String write() {
        if (isValidFormat()) {
            String error = runUpload();
            if (error == null) {
                return LogString.fileSuccessfullyCreated(getFtpString(false),
                        International.getString("Statistik"));
            } else {
                return LogString.fileCreationFailed(getFtpString(false),
                        International.getString("Statistik"),
                        error);

            }
        } else {
            return International.getString("Ungültiges Format") + ": " + getFtpString();
        }
    }

    public String runDownload() {
        try {
            com.enterprisedt.net.ftp.FTPClient ftpClient = new com.enterprisedt.net.ftp.FTPClient(server);
            if (server == null) {
                return null; // happens for test whether FTP Plugin in installed
            }
            ftpClient.setConnectMode(com.enterprisedt.net.ftp.FTPConnectMode.ACTIVE);
            ftpClient.login(username, password);
            if (remoteDirectory != null) {
                ftpClient.chdir( (remoteDirectory.length() == 0 ? "/" : remoteDirectory) );
            }
            ftpClient.get(localFileWithPath, remoteFile);
            ftpClient.quit();
            return null; // korrektes Ende!
        } catch (Exception e) {
            Logger.log(Logger.WARNING, Logger.MSG_WARN_DOWNLOADFAILED, 
                    "Download of file '" + remoteFile + "' failed: " + e.toString());
            Logger.logdebug(e);
            return e.toString();
        }
    }

    public String read() {
        if (isValidFormat()) {
            String error = runDownload();
            if (error == null) {
                return LogString.fileSuccessfullyCreated(getFtpString(false),
                        International.getString("Statistik"));
            } else {
                return LogString.fileCreationFailed(getFtpString(false),
                        International.getString("Statistik"),
                        error);

            }
        } else {
            return International.getString("Ungültiges Format") + ": " + getFtpString();
        }
    }

    public static String getFtpStringGuiDialog(String s) {
        FTPClient ftp = new FTPClient(s, null);
        IItemType[] items = new IItemType[5];
        items[0] = new ItemTypeString("SERVER", (ftp.getServer() != null ? ftp.getServer() : ""),
                IItemType.TYPE_PUBLIC, "", International.getString("FTP-Server"));
        items[0].setNotNull(true);
        items[1] = new ItemTypeString("USERNAME", (ftp.getUsername() != null ? ftp.getUsername() : ""),
                IItemType.TYPE_PUBLIC, "", International.getString("Benutzername"));
        items[2] = new ItemTypeString("PASSWORD", (ftp.getPassword() != null ? ftp.getPassword() : ""),
                IItemType.TYPE_PUBLIC, "", International.getString("Paßwort"));
        items[3] = new ItemTypeString("FILE", (ftp.getFile() != null ? ftp.getFile() : ""),
                IItemType.TYPE_PUBLIC, "", International.getString("Dateiname"));
        items[4] = new ItemTypeBoolean("PASV", ftp.isPasv(),
                IItemType.TYPE_PUBLIC, "", International.getString("PASV Mode"));
        if (MultiInputDialog.showInputDialog((JDialog)null, International.getString("FTP-Upload"), items)) {
            String file = items[3].toString();
            if (file != null && !file.startsWith("/")) {
                file = "/" + file;
            }
            items[4].getValueFromField();
            boolean pasv = ((ItemTypeBoolean)items[4]).getValue();
            ftp = new FTPClient( (pasv ? "ftp-pasv:" : "ftp:") +
                    items[1].toString() + ":" + items[2].toString() +
                    "@" + items[0].toString() + ":" + file, null);
            if (ftp.isValidFormat()) {
                return ftp.getFtpString(true);
            }
        }
        return null;
    }
    
    public static void main(String[] args) {
        if (args == null || args.length != 6) {
            System.out.println("usage: FTPClient <server> <user> <pass> <localFileWithPath> <remoteDir> <remoteFile>");
            System.exit(1);
        }
        FTPClient ftp = new FTPClient(args[0], args[1], args[2], args[3], args[4], args[5]);
        System.out.println(ftp.write());
        System.exit(0);
    }

}
