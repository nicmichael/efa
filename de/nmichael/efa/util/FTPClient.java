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

import javax.swing.JDialog;

import com.enterprisedt.net.ftp.FTPTransferType;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.gui.MultiInputDialog;


/*
 * FTPClient Class
 * 
 * In this class has been refactored to support the following protocols: (upload and download)
 * - Standard FTP 
 *   still using edtFTPj/free library. Active and passive mode. 
 * 
 * - SFTP (File transfer over ssh, like winSCP)
 *   Implemented using the jsch library. I abadoned a SSHJ based solution, as SSHJ needs some log4j logging functions,
 *   which are not yet implemented in EFA, and I did not want to have two different logger methods within efa.
 *   Additionally, it was a hard time fiddling around to get SSHJ up and running without log4j, and I did not succeed.
 *   So jsch library it is.
 *   
 * Also, the port can be defined for each of these protocols, a port number 0 targets the standard ports for
 * the respective protocols.
 * 
 * The storage of the new protocols and the new port attributes are downward compatible to the ftp strings
 * of older versions of this class. 
 * 
 * So what about FTPs (FTP over TLS)?
 *    
 * - FTPs (FTP over TLS) 
 *   This java class is PREPARED to use FTPs once the Apache Commons Net library supports FTPs correctly.
 *   
 *   Using Apache Commons Net FTPSClient. This is the only free library for using FTPs (TLS) transfers.
 *   FTPs using Apache Commons Net Library does not work with FTPs Servers which require session resume logic.
 *    
 *   Session resume is a security measure to ensure that the session which sends the commands (like change directory)
 *   is the same as the one transferring data (files). This session resume logic is broken in 
 *   Apache Commons Net (see https://issues.apache.org/jira/browse/NET-408 ). The current fixes rely on reflection,
 *   which is no longer usable from jdk17 on.
 *   
 *   So, FTPs is currently not supported. This may change with an Apache Commons Net library which
 *   supports session resume logic.
 *   
 */

// @i18n complete
public class FTPClient {

	private static final String PREFIX_FTP_PASV = "ftp-pasv:";
	private static final String PREFIX_SFTP_SSH = "sftp:";
	private static final String PREFIX_FTP = "ftp:";
	private static final String PREFIX_FTPS_TLS = "ftps:";
	
	private static final String PROTOCOL_FTP = "FTP"; 
	private static final String PROTOCOL_FTP_PASV = "FTP-PASV";
	private static final String PROTOCOL_FTPS_TLS = "FTPS (TLS)"; // remember, this protocol does not work yet, see class comment
	private static final String PROTOCOL_SFTP_SSH = "SFTP (SSH)";
	
	/*  FTP String patterns EFA 2.3.2 and earlier: 
	 *  ftp:username:password@127.0.0.1:/path/filename.suffix
	 *  ftp-pasv:username:password@127.0.0.1:/path/filename.suffix
	 *
	 * Additional ftp string patterns EFA 2.3.4 and up:
	 *  ftp:username:password@127.0.0.1|port:/path/filename.suffix
	 *  ftp-pasv:username:password@127.0.0.1|port:/path/filename.suffix
	 *  
	 *  sftp:username:password@127.0.0.1:/path/filename.suffix
	 *  sftp:username:password@127.0.0.1|port:/path/filename.suffix
     *
	 *  ftps:username:password@127.0.0.1:/path/filename.suffix
	 *  ftps:username:password@127.0.0.1|port:/path/filename.suffix
	 *  
	 */
    private String ftpString;
    private String server;
    private String username;
    private String password;
    private String localFileWithPath;
    private String remoteDirectory;
    private String remoteFile;
    private int port=0;
    private boolean isSFTPSSHMode = false;
    private boolean isFTPsTLSMode = false;
    private boolean isFTPPasvMode = false;
    private boolean validFormat = false;
    
    public static boolean isFTP(String s) {
        return s != null && (s.toLowerCase().startsWith(PREFIX_FTP) ||
        					 s.toLowerCase().startsWith(PREFIX_SFTP_SSH) ||        		
                             s.toLowerCase().startsWith(PREFIX_FTP_PASV) ||
                             s.toLowerCase().startsWith(PREFIX_FTPS_TLS));
    }

    public FTPClient(String ftpString, String localFileWithPath) {
        this.ftpString = ftpString;
        this.localFileWithPath = localFileWithPath;
        if (isFTP(ftpString)) {
            if (ftpString.toLowerCase().startsWith(PREFIX_FTP)) {
                ftpString = ftpString.substring(4);
            }
            if (ftpString.toLowerCase().startsWith(PREFIX_FTP_PASV)) {
                ftpString = ftpString.substring(9);
                isFTPPasvMode = true;
                isSFTPSSHMode=false;
                isFTPsTLSMode=false;
            }
            if (ftpString.toLowerCase().startsWith(PREFIX_SFTP_SSH)) {
                ftpString = ftpString.substring(5);
                isFTPPasvMode = false;
                isSFTPSSHMode=true;
                isFTPsTLSMode=false;
            }
            if (ftpString.toLowerCase().startsWith(PREFIX_FTPS_TLS)) {
                ftpString = ftpString.substring(5);
                isFTPPasvMode = false;
                isSFTPSSHMode=false;
                isFTPsTLSMode=true;
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
                        server = getServerFromFTPString(ftpString.substring(0, pos));
                        port = getPortFromFTPString(ftpString.substring(0, pos));
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

    /*
     * The server part of an ftpString contains
     * - servername
     * - optional 
     *     |portnumber
     * this method returns only the server name
     */
    private String getServerFromFTPString(String ftpString) {
    	int posDelimiter=ftpString.lastIndexOf("|");
    	if (posDelimiter >=0) {
    		return ftpString.substring(0,posDelimiter);
    	} else {
    		return ftpString;// no delimiter found in ftpString --> so no port number specified
    	}
    }
    
    /**
     * The server part of an ftpString contains
     * - servername
     * - optional 
     *     |portnumber
     * @return  port number, or 0 if no port number is configured in ftp string
     */
    private int getPortFromFTPString(String ftpString) {
    	int posDelimiter=ftpString.lastIndexOf("|");
    	if (posDelimiter >=0) {
    		int result = 0;
    		try {
    			String temp = ftpString.substring(posDelimiter+1);
    			result=Integer.parseInt(temp);
    		} catch (Exception e) {
    			result = 0;
    		}
    		return result;
    	} else {
    		return 0;
    	}
    }
    
    	
    public FTPClient(String server, String username, String password,
            String localFileWithPath, String remoteDirectory, String remoteFile, int port) {
        this.ftpString = username + ":" + password + "@" + server + ":" + remoteDirectory + 
                (remoteDirectory != null && remoteDirectory.endsWith("/") ? "" : "/") + remoteFile;
        this.server = server;
        this.port = (port>0 ? port : 0); // set port to an empty string, if port is null
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
        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_FTP, "port: " + port);
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
        return (isFTPPasvMode ? PREFIX_FTP_PASV : (isSFTPSSHMode ? PREFIX_SFTP_SSH : (isFTPsTLSMode ? PREFIX_FTPS_TLS : PREFIX_FTP))) + 
                username + (withPassword ? ":" + password : "") +
                "@" + server + 
                //server may also contain port, if port is specified
                (port >0 ? "|"+port : "")+
                ":" +
                remoteDirectory + 
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
        return isFTPPasvMode;
    }

    public int getPort() {
    	return port;
    }
    
    public String runUpload() {
    	if (isSFTPSSHMode) {
    		return runUploadSFTPViaSSH();
    	} else if (isFTPsTLSMode) {
			return runUploadFTPSViaTLS();
    	} else {
    		return runUploadFTP();
    	}
    }
    
    /**
     * Runs an upload via plain old FTP (using an old free FTP library)
     * Uses the credentials provided by ftpClient class.
     * 
     * @return null if successful, String containing error message if not successful. 
     */    
    private String runUploadFTP() {
        try {
            com.enterprisedt.net.ftp.FTPClient ftpClient = new com.enterprisedt.net.ftp.FTPClient(server);
            if (server == null) {
                return null; // happens for test whether FTP Plugin in installed
            }
            if (port>0) {
                ftpClient.setRemotePort(port);
            }
            
            ftpClient.setConnectMode(isFTPPasvMode ? com.enterprisedt.net.ftp.FTPConnectMode.PASV :
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
            return e.toString();
        }
    }

    /**
     * Runs an upload via SFTP (using ssh and the according JSch library).
     * Uses the credentials provided by ftpClient class.
     * 
     * @return null if successful, String containing error message if not successful. 
     */
    private String runUploadSFTPViaSSH() {
    	String result=null;
    	
    	JSch jsch = new JSch();
        Session session = null;
        try {
           session = jsch.getSession(this.username, this.server, (port>0 ? port : 22));

           session.setConfig("StrictHostKeyChecking", "no");
           session.setPassword(this.password);
           session.connect();

           Channel channel = session.openChannel("sftp");
           channel.connect();
           ChannelSftp sftpChannel = (ChannelSftp) channel;
           if (this.remoteDirectory.equalsIgnoreCase("") || this.remoteDirectory.equals("/") || this.remoteDirectory.equals("\\")) {
               sftpChannel.put(this.localFileWithPath,this.remoteFile, ChannelSftp.OVERWRITE);
           } else {
        	   sftpChannel.put(this.localFileWithPath,this.remoteDirectory+"/"+this.remoteFile, ChannelSftp.OVERWRITE);
           }
           sftpChannel.exit();

       } catch (JSchException | SftpException e) {
    	   result = e.getMessage();
       } finally {
           if (session != null) {
               session.disconnect();
           }
       }
    	    
  	   return result;
    }
    

    /**
     * Runs an upload via FTPS (using TLS and the according Apache Net library).
     * Uses the credentials provided by ftpClient class.
     * 
     * THIS FUNCTION DOES NOT WORK CORRECTLY WITH SERVERS WHICH REQUIRE SESSION REUSE.
     * SEE CLASS COMMENT FOR MORE INFO.
     * 
     * @return null if successful, String containing error message if not successful. 
     */   
    
    private String runUploadFTPSViaTLS() {
    	return "Apache Commons Net FTPs support broken - so not implemented";
    	/*
    	String result = "";
    	FTPSClient ftpClient = new FTPSClient(false);//no implicit FTPs
    	if (port>0) {
    		ftpClient.setDefaultPort(port);
    	}
    	try {
    		
    		InputStream localFile=new FileInputStream(this.localFileWithPath);

			try {
	    		
	    		ftpClient.setEnabledSessionCreation(true);
				ftpClient.connect(this.server);
	    		ftpClient.getReplyCode();
	    		ftpClient.execPBSZ(0);	    		
	    		ftpClient.execPROT("P");// Prot P means: also data transfers shall be encrypted.
	    		ftpClient.login(this.username, this.password);
	            if (remoteDirectory != null) {
	                ftpClient.changeWorkingDirectory((remoteDirectory.length() == 0 ? "/" : remoteDirectory));
	            }
	            
	            ftpClient.setFileType(FTPSClient.BINARY_FILE_TYPE);
	            ftpClient.storeFile(this.remoteFile, localFile);
	            ftpClient.quit();
	    	} catch (IOException e) {
	    		result = e.getMessage();
	    	} finally {
	    		localFile.close();
	    	}
    	} catch (IOException e) {

    		result = e.getMessage();
    	}
    	return result;
    	*/
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
		if (isSFTPSSHMode) {
			return runDownloadSFTPViaSSH();
		} else if (isFTPsTLSMode) {
			return runDownloadFTPSViaTLS();
		} else {
			return runDownloadFTP(); //active and passive
		}    
    }
    
    private String runDownloadFTP() {
        try {
            com.enterprisedt.net.ftp.FTPClient ftpClient = new com.enterprisedt.net.ftp.FTPClient(server);
            if (server == null) {
                return null; // happens for test whether FTP Plugin in installed
            }
            if (port>0) {
                ftpClient.setRemotePort(port);
            }
            //obviously download can only be run in active Mode
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

    private String runDownloadSFTPViaSSH() {
    	
    	String result=null;
    	
    	JSch jsch = new JSch();
        Session session = null;
        try {
           session = jsch.getSession(this.username, this.server, (port>0 ? port : 22));

           session.setConfig("StrictHostKeyChecking", "no");
           session.setPassword(this.password);
           session.connect();

           Channel channel = session.openChannel("sftp");
           channel.connect();
           ChannelSftp sftpChannel = (ChannelSftp) channel;
           if (this.remoteDirectory.equalsIgnoreCase("") || this.remoteDirectory.equals("/") || this.remoteDirectory.equals("\\")) {
               sftpChannel.get(this.remoteFile, this.localFileWithPath);
           } else {
        	   sftpChannel.get(this.remoteDirectory+"/"+this.remoteFile, this.localFileWithPath);
           }
           sftpChannel.exit();

       } catch (JSchException | SftpException e) {
    	   result = e.getMessage();
           Logger.log(Logger.WARNING, Logger.MSG_WARN_DOWNLOADFAILED, 
                   "Download of file '" + remoteFile + "' failed: " + e.toString());
           Logger.logdebug(e);
       } finally {
           if (session != null) {
               session.disconnect();
           }
       }
    	    
  	   return result;    	
    }
    
    private String runDownloadFTPSViaTLS() {
    	return "Apache Commons Net FTPs support broken - so not implemented";
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

    private static String getProtocolFromFTPClient(FTPClient client) {
    	if (client.isFTPPasvMode) {
    		return PROTOCOL_FTP_PASV;
    	} else if (client.isSFTPSSHMode) {
    		return PROTOCOL_SFTP_SSH;
    	} else if (client.isFTPsTLSMode) {
    		return PROTOCOL_FTPS_TLS;
    	} else {
    		return PROTOCOL_FTP;
    	}
    }
    
    /**
     * Shows an MultiInputDialog on screen, providing edit fields for all elements
     * of an ftpString.
     * 
     * @param s ftpString 
     * @return new ftpString with changes from the user
     */
    public static String getFtpStringGuiDialog(String s) {
        
    	final int ITEM_PROTOCOL =0;
    	final int ITEM_SERVER =1;
    	final int ITEM_PORT =2;
    	final int ITEM_USERNAME =3;
    	final int ITEM_PASSWORD =4;
    	final int ITEM_FILE =5;
    	
    	
    	FTPClient ftp = new FTPClient(s, null);
        IItemType[] items = new IItemType[6];
        
        items[ITEM_PROTOCOL] = new ItemTypeStringList("PROTOCOL",
				getProtocolFromFTPClient(ftp), new String[] { PROTOCOL_FTP, PROTOCOL_FTP_PASV, /*PROTOCOL_FTPS_TLS,*/ PROTOCOL_SFTP_SSH},
				new String[] { PROTOCOL_FTP, PROTOCOL_FTP_PASV, /*PROTOCOL_FTPS_TLS,*/ PROTOCOL_SFTP_SSH }, IItemType.TYPE_PUBLIC,
				"",International.getString("Protokoll"));        
        items[ITEM_SERVER] = new ItemTypeString("SERVER", (ftp.getServer() != null ? ftp.getServer() : ""),
                IItemType.TYPE_PUBLIC, "", International.getString("FTP-Server"));
        items[ITEM_SERVER].setNotNull(true);
        items[ITEM_PORT] = new ItemTypeInteger("PORT", ftp.getPort(), 0, 65535, true,
                IItemType.TYPE_PUBLIC, "", International.getString("FTP-Port"));
        items[ITEM_USERNAME] = new ItemTypeString("USERNAME", (ftp.getUsername() != null ? ftp.getUsername() : ""),
                IItemType.TYPE_PUBLIC, "", International.getString("Benutzername"));
        items[ITEM_PASSWORD] = new ItemTypeString("PASSWORD", (ftp.getPassword() != null ? ftp.getPassword() : ""),
                IItemType.TYPE_PUBLIC, "", International.getString("Paßwort"));
        items[ITEM_FILE] = new ItemTypeString("FILE", (ftp.getFile() != null ? ftp.getFile() : ""),
                IItemType.TYPE_PUBLIC, "", International.getString("Dateiname"));

        
        if (MultiInputDialog.showInputDialog((JDialog)null, International.getString("FTP-Upload"), items)) {
        	//Check, if the values build a valid ftpString...
        	String file = items[ITEM_FILE].toString();
            if (file != null && !file.startsWith("/")) {
                file = "/" + file;
            }
            
            boolean pasv = items[ITEM_PROTOCOL].getValueFromField().equals(PROTOCOL_FTP_PASV);
            boolean isSFTP = items[ITEM_PROTOCOL].getValueFromField().equals(PROTOCOL_SFTP_SSH);
            boolean isFTPs = items[ITEM_PROTOCOL].getValueFromField().equals(PROTOCOL_FTPS_TLS);
            
            int port = ((ItemTypeInteger)items[ITEM_PORT]).getValue();
            		
            ftp = new FTPClient(
            		(pasv ? PREFIX_FTP_PASV : (isSFTP ? PREFIX_SFTP_SSH : (isFTPs ? PREFIX_FTPS_TLS : PREFIX_FTP))) + //FTP is default
            		items[ITEM_USERNAME].toString() + ":" + items[ITEM_PASSWORD].toString() +
                    "@" + items[ITEM_SERVER].toString() + 
                    //server may also contain port, if port is specified
                    (port >0 ? "|"+port : "")+
                    ":" + file, null);
            if (ftp.isValidFormat()) {
                return ftp.getFtpString(true);
            }
        }
        return null;
    }
    
    public static void main(String[] args) {
        if (args == null || args.length != 6) {
            System.out.println("usage: FTPClient <server> <user> <pass> <localFileWithPath> <remoteDir> <remoteFile> <remotePort>");
            System.exit(1);
        }
        FTPClient ftp = new FTPClient(args[0], args[1], args[2], args[3], args[4], args[5], Integer.parseInt(args[6]));
        System.out.println(ftp.write());
        System.exit(0);
    }

}
