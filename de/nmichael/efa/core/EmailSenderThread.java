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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.util.*;
import java.util.*;
import javax.mail.Address;
import javax.mail.internet.InternetAddress;

public class EmailSenderThread extends Thread {

    private long lastScnAdmins = -1;
    private Vector<String> emailAddressesAdmin;
    private Vector<String> emailAddressesBoatMaintenance;
    private Vector<MultiPartMessage> multipartMessages =
            new Vector<MultiPartMessage>();

    private long lastScnEfaConfig = -1;
    private String serverUrl;
    private String serverPort;
    private String serverUsername;
    private String serverPassword;
    private String mailFromEmail;
    private String mailFromName;
    private String mailSubjectPrefix;
    private String mailSignature;

    // Constructor just for Plugin Check
    public EmailSenderThread() {
        javax.mail.Session session = javax.mail.Session.getInstance(new Properties(), null); // just dummy statement
    }

    public void enqueueMessage(javax.mail.Multipart message,
            Vector addresses, String subject,
            String[] attachmentFileNames, boolean deleteAttachmentFiles) {
        multipartMessages.add(new MultiPartMessage(message, addresses, subject,
                attachmentFileNames, deleteAttachmentFiles));
        interrupt();
    }

    public boolean sendMessage(javax.mail.Multipart message,
            Vector addresses, String subject,
            String[] attachmentFileNames, boolean deleteAttachmentFiles) {
        return sendMail(new MultiPartMessage(message, addresses, subject,
                attachmentFileNames, deleteAttachmentFiles));
    }

    private boolean updateMailProperties() {
        try {
            if (Daten.efaConfig == null || Daten.project == null || Daten.admins == null
                    || !Daten.efaConfig.isOpen() || !Daten.project.isOpen() || !Daten.admins.isOpen()
                    || Daten.project.isInOpeningProject()) {
                Logger.log(Logger.WARNING, Logger.MSG_WARN_SENDMAIL, 
                        International.getString("email-Konfiguration konnte nicht ermittelt werden."));
                return false;
            }
            long scn = Daten.efaConfig.data().getSCN();
            if (scn == lastScnEfaConfig) {
                return true;
            }
            serverUrl = Daten.efaConfig.getValueEfaDirekt_emailServer();
            serverPort = Integer.toString(Daten.efaConfig.getValueEfaDirekt_emailPort());
            serverUsername = Daten.efaConfig.getValueEfaDirekt_emailUsername();
            serverPassword = Daten.efaConfig.getValueEfaDirekt_emailPassword();
            mailFromEmail = Daten.efaConfig.getValueEfaDirekt_emailAbsender();
            mailFromName = Daten.efaConfig.getValueEfaDirekt_emailAbsenderName();
            mailSubjectPrefix = Daten.efaConfig.getValueEfaDirekt_emailBetreffPraefix();
            mailSignature = Daten.efaConfig.getValueEfaDirekt_emailSignatur();

            if (serverUrl != null && serverUrl.trim().length() == 0) {
                serverUrl = null;
            }
            if (serverPort != null && serverPort.trim().length() == 0) {
                serverPort = null;
            }
            if (serverUsername != null && serverUsername.trim().length() == 0) {
                serverUsername = null;
            }
            if (serverPassword != null && serverPassword.trim().length() == 0) {
                serverPassword = null;
            }
            if (mailFromEmail != null && mailFromEmail.trim().length() == 0) {
                mailFromEmail = null;
            }
            if (mailFromName != null && mailFromName.trim().length() == 0) {
                mailFromName = Daten.EFAEMAILNAME;
            }
            if (mailSubjectPrefix != null && mailSubjectPrefix.trim().length() == 0) {
                mailSubjectPrefix = null;
            }
            if (mailSignature != null && mailSignature.trim().length() == 0) {
                mailSignature = null;
            }
            lastScnEfaConfig = scn;
            return true;
        } catch (Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }

    private boolean updateAdminEmailAddresses() {
        try {
            if (Daten.efaConfig == null || Daten.project == null || Daten.admins == null
                    || !Daten.efaConfig.isOpen() || !Daten.project.isOpen() || !Daten.admins.isOpen()
                    || Daten.project.isInOpeningProject()) {
                Logger.log(Logger.WARNING, Logger.MSG_WARN_SENDMAIL, 
                        International.getString("email-Konfiguration konnte nicht ermittelt werden."));
                return false;
            }
            long scn = Daten.admins.data().getSCN();
            if (scn == lastScnAdmins) {
                return true;
            }
            emailAddressesAdmin = new Vector<String>();
            emailAddressesBoatMaintenance = new Vector<String>();
            DataKeyIterator it = Daten.admins.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                AdminRecord admin = (AdminRecord) Daten.admins.data().get(k);
                if (admin != null && admin.getEmail() != null && admin.getEmail().length() > 0) {
                    if (admin.isAllowedMsgReadAdmin() && !emailAddressesAdmin.contains(admin.getEmail())) {
                        emailAddressesAdmin.add(admin.getEmail());
                    }
                    if (admin.isAllowedMsgReadBoatMaintenance() && !emailAddressesBoatMaintenance.contains(admin.getEmail())) {
                        emailAddressesBoatMaintenance.add(admin.getEmail());
                    }
                }
                k = it.getNext();
            }
            if (emailAddressesAdmin.size() == 0) {
                emailAddressesAdmin = null;
            }
            if (emailAddressesBoatMaintenance.size() == 0) {
                emailAddressesBoatMaintenance = null;
            }
            lastScnAdmins = scn;
            return true;
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }

    private boolean sendMail(MessageRecord msg, Vector addresses) {
        try {
            if (!updateMailProperties() || !updateAdminEmailAddresses()) {
                Logger.log(Logger.WARNING, Logger.MSG_ERR_SENDMAILFAILED_CFG,
                        International.getString("Kein email-Versand möglich!") + " " +
                        International.getString("email-Konfiguration konnte nicht ermittelt werden."));
            }
            if (serverUrl == null || serverPort == null ||
                mailFromEmail == null || mailFromName == null) {
                Logger.log(Logger.WARNING, Logger.MSG_ERR_SENDMAILFAILED_CFG,
                        International.getString("Kein email-Versand möglich!") + " "
                        + International.getString("Mail-Konfiguration unvollständig."));
                return false;
            }
            StringBuilder recipients = new StringBuilder();
            for (int i=0; i<addresses.size(); i++) {
                recipients.append( (recipients.length() > 0 ? ", " : "") + addresses.get(i));
            }
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SENDMAIL,
                        "Trying to send message " + msg.getMessageId() + " to " + recipients.toString() + " ...");
            }
            boolean auth = (serverUsername != null && serverPassword != null);
            String protocol = Daten.efaConfig.getValueEmailSSL() ? "smtps" : "smtp";
            Properties props = new Properties();
            props.put("mail." + protocol + ".host", serverUrl);
            props.put("mail." + protocol + ".port", serverPort);
            if (auth) {
                props.put("mail." + protocol + ".auth", "true");
            }
            if (Daten.efaConfig.getValueEmailSSL()) {
                props.put("mail." + protocol + ".ssl.enable", "true");
            } else {
                if (Daten.efaConfig.getValueEfaDirekt_emailPort() != 25) {
                    props.put("mail." + protocol + ".starttls.enable", "true");
                }
            }
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
                props.put("mail.debug", "true");
            }
            MailAuthenticator ma = null;
            if (auth) {
                ma = new MailAuthenticator(serverUsername, serverPassword);
            }
            String charset = Daten.ENCODING_ISO;
            javax.mail.Session session = javax.mail.Session.getInstance(props, ma);
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
                session.setDebugOut(Logger.getPrintStream());
            }
            com.sun.mail.smtp.SMTPMessage mail = new com.sun.mail.smtp.SMTPMessage(session);
            mail.setAllow8bitMIME(true);
            mail.setHeader("X-Mailer", Daten.EFA_SHORTNAME + " " + Daten.VERSIONID);
            mail.setHeader("Content-Type", "text/plain; charset=" + charset);
            mail.setFrom(new javax.mail.internet.InternetAddress(mailFromName + " <" + mailFromEmail + ">"));
            mail.setRecipients(com.sun.mail.smtp.SMTPMessage.RecipientType.TO, javax.mail.internet.InternetAddress.parse(recipients.toString()));
            if (msg.getReplyTo() != null && msg.getReplyTo().length() > 0) {
                try {
                    mail.setReplyTo(new Address[] { new InternetAddress(msg.getReplyTo()) });
                } catch(Exception e) {
                    Logger.logdebug(e);
                }
            }
            mail.setSubject((mailSubjectPrefix != null ? "[" + mailSubjectPrefix + "] " : "") + msg.getSubject(), charset);
            mail.setSentDate(new Date());
            mail.setText("## " + International.getString("Absender") + ": " + msg.getFrom() + "\n"
                    + "## " + International.getString("Betreff") + " : " + msg.getSubject() + "\n\n"
                    + msg.getText()
                    + (mailSignature != null ? "\n\n-- \n"
                    + EfaUtil.replace(mailSignature, "$$", "\n", true) : ""), charset);
            com.sun.mail.smtp.SMTPTransport t = (com.sun.mail.smtp.SMTPTransport) 
                    session.getTransport(protocol);
            if (auth) {
                t.connect(serverUrl, serverUsername, serverPassword);
            } else {
                t.connect();
            }
            t.sendMessage(mail, mail.getAllRecipients());
            return true;
        } catch (Exception e) {
            Logger.log(Logger.WARNING, Logger.MSG_ERR_SENDMAILFAILED_ERROR,
                    International.getString("email-Versand fehlgeschlagen") + ": " +
                    e.toString() + " " + e.getMessage());
            Logger.logdebug(e);
            return false;
        }
    }

    private boolean sendMail(MultiPartMessage msg) {
        try {
            if (!updateMailProperties() || !updateAdminEmailAddresses()) {
                Logger.log(Logger.WARNING, Logger.MSG_ERR_SENDMAILFAILED_CFG,
                        International.getString("Kein email-Versand möglich!") + " " +
                        International.getString("email-Konfiguration konnte nicht ermittelt werden."));
            }

            if (serverUrl == null || serverPort == null ||
                mailFromEmail == null || mailFromName == null) {
                Logger.log(Logger.WARNING, Logger.MSG_ERR_SENDMAILFAILED_CFG,
                        International.getString("Kein email-Versand möglich!") + " "
                        + International.getString("Mail-Konfiguration unvollständig."));
                return false;
            }
            StringBuilder recipients = new StringBuilder();
            for (int i=0; i<msg.addresses.size(); i++) {
                recipients.append( (recipients.length() > 0 ? ", " : "") + msg.addresses.get(i));
            }
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SENDMAIL,
                        "Trying to send multipart message to " + recipients.toString() + " ...");
            }
            boolean auth = (serverUsername != null && serverPassword != null);
            String protocol = Daten.efaConfig.getValueEmailSSL() ? "smtps" : "smtp";
            Properties props = new Properties();
            props.put("mail." + protocol + ".host", serverUrl);
            props.put("mail." + protocol + ".port", serverPort);
            if (auth) {
                props.put("mail." + protocol + ".auth", "true");
            }
            if (Daten.efaConfig.getValueEmailSSL()) {
                props.put("mail." + protocol + ".ssl.enable", "true");
            } else {
                if (Daten.efaConfig.getValueEfaDirekt_emailPort() != 25) {
                    props.put("mail." + protocol + ".starttls.enable", "true");
                }
            }
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
                props.put("mail.debug", "true");
            }
            MailAuthenticator ma = null;
            if (auth) {
                ma = new MailAuthenticator(serverUsername, serverPassword);
            }
            String charset = Daten.ENCODING_ISO;
            javax.mail.Session session = javax.mail.Session.getInstance(props, ma);
            if (Logger.isTraceOn(Logger.TT_BACKGROUND, 5)) {
                session.setDebugOut(Logger.getPrintStream());
            }
            com.sun.mail.smtp.SMTPMessage mail = new com.sun.mail.smtp.SMTPMessage(session);
            mail.setAllow8bitMIME(true);
            mail.setHeader("X-Mailer", Daten.EFA_SHORTNAME + " " + Daten.VERSIONID);
            mail.setHeader("Content-Type", "text/plain; charset=" + charset);
            mail.setFrom(new javax.mail.internet.InternetAddress(mailFromName + " <" + mailFromEmail + ">"));
            mail.setRecipients(com.sun.mail.smtp.SMTPMessage.RecipientType.TO, javax.mail.internet.InternetAddress.parse(recipients.toString()));
            mail.setSubject((mailSubjectPrefix != null ? "[" + mailSubjectPrefix + "] " : "") + msg.subject, charset);
            mail.setSentDate(new Date());
            mail.setContent(msg.message);
            com.sun.mail.smtp.SMTPTransport t = (com.sun.mail.smtp.SMTPTransport) session.getTransport(protocol);
            if (auth) {
                t.connect(serverUrl, serverUsername, serverPassword);
            } else {
                t.connect();
            }
            t.sendMessage(mail, mail.getAllRecipients());
            if (msg.deleteAttachmentFiles) {
                for (int i=0; msg.attachmentFileNames != null && i<msg.attachmentFileNames.length; i++) {
                    EfaUtil.deleteFile(msg.attachmentFileNames[i]);
                }
            }
            Logger.log(Logger.INFO, Logger.MSG_CORE_MAILSENT,
                    LogString.emailSuccessfullySend(msg.subject));
            return true;
        } catch (Exception e) {
            Logger.log(Logger.WARNING, Logger.MSG_ERR_SENDMAILFAILED_ERROR,
                    International.getString("email-Versand fehlgeschlagen") + ": " +
                    e.toString() + " " + e.getMessage());
            Logger.logdebug(e);
            return false;
        }
    }

    public void run() {
    	this.setName("EmailSenderThread");
        int errorCount = 0;
        while(true) {
            try {
                if (Daten.efaConfig != null && Daten.project != null && Daten.admins != null &&
                    Daten.efaConfig.isOpen() && Daten.project.isOpen() && Daten.admins.isOpen() &&
                    !Daten.project.isInOpeningProject()) {

                    updateMailProperties();
                    updateAdminEmailAddresses();

                    if (Daten.project.getProjectStorageType() != IDataAccess.TYPE_EFA_REMOTE) {
                        int countToBeSent = 0;
                        int countSuccess = 0;
                        if (Daten.project == null || !Daten.project.isOpen()) {
                        	continue;
                        }
                        Messages messages = Daten.project.getMessages(false);
                        if (messages == null || messages.data() == null
                                || messages.data().getStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                            continue; // EmailSenderThread must only run for local messages!
                        }
                        DataKeyIterator it = messages.data().getStaticIterator();
                        DataKey k = it.getFirst();
                        while (k != null) {
                            MessageRecord msg = (MessageRecord) messages.data().get(k);
                            if (msg != null && msg.getToBeMailed()) {
                                // new message found
                                countToBeSent++;
                                boolean markDone = false;
                                if ((emailAddressesAdmin != null || emailAddressesBoatMaintenance != null) &&
                                     (serverUrl != null || serverPort != null
                                      || mailFromEmail != null || mailFromName != null)) {
                                    // recipient email addresses configured
                                    if (serverUrl != null && serverPort != null
                                            && mailFromEmail != null && mailFromName != null) {
                                        // server properly configured
                                        if (MessageRecord.TO_ADMIN.equals(msg.getTo()) && emailAddressesAdmin != null) {
                                            markDone = sendMail(msg, emailAddressesAdmin);
                                            if (markDone) {
                                                countSuccess++;
                                            }
                                        }
                                        if (MessageRecord.TO_BOATMAINTENANCE.equals(msg.getTo()) && emailAddressesBoatMaintenance != null) {
                                            markDone = sendMail(msg, emailAddressesBoatMaintenance);
                                            if (markDone) {
                                                countSuccess++;
                                            }
                                        }
                                    } else {
                                        Logger.log(Logger.WARNING, Logger.MSG_ERR_SENDMAILFAILED_CFG,
                                                International.getString("Kein email-Versand möglich!") + " "
                                                + International.getString("Mail-Konfiguration unvollständig."));
                                    }
                                } else {
                                    markDone = true; // no email recipients configured - mark this message as done
                                }
                                if (markDone) {
                                    msg.setToBeMailed(false);
                                    messages.data().update(msg);
                                    errorCount = 0;
                                } else {
                                    if (errorCount < 10) {
                                        errorCount++;
                                        break;
                                    }
                                }
                            }
                            k = it.getNext();
                        }
                        if (Logger.isTraceOn(Logger.TT_BACKGROUND)) {
                            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_SENDMAIL, "EmailSenderThread: " + countToBeSent + " unsent messages found; " + countSuccess + " messages successfully sent.");
                        }
                    }

                    for (int i=0; i<multipartMessages.size(); i++) {
                        if (sendMail(multipartMessages.get(i))) {
                            multipartMessages.remove(i--);
                        }
                    }
                }
               
                Thread.sleep((1+(errorCount*10)) * 60 * 1000);
            } catch(Exception e) {
                Logger.log(Logger.ERROR, International.getString("Exception im emailSenderThread aufgetreten. Aktivieren Sie den DebugModus, um mehr Informationen zu erhalten.")+" "+e.toString());
                // if an hard error occurs, sleep some time. 
                // otherwise, efa.log file may grow very fast in a very short time.
                try {
					Thread.sleep((1+(errorCount*10)) * 60 * 1000);
				} catch (InterruptedException e1) {
					EfaUtil.foo(); // do notihnig
				}

            }
        }
    }

    class MailAuthenticator extends javax.mail.Authenticator {

        String username;
        String password;

        public MailAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public javax.mail.PasswordAuthentication getPasswordAuthentication() {
            return new javax.mail.PasswordAuthentication(username, password);
        }
    }

    class MultiPartMessage {

        javax.mail.Multipart message;
        Vector<String> addresses;
        String subject;
        String[] attachmentFileNames;
        boolean deleteAttachmentFiles;

        public MultiPartMessage(javax.mail.Multipart message,
            Vector<String> addresses, String subject,
            String[] attachmentFileNames, boolean deleteAttachmentFiles) {
            this.message = message;
            this.addresses = addresses;
            this.subject = subject;
            this.attachmentFileNames = attachmentFileNames;
            this.deleteAttachmentFiles = deleteAttachmentFiles;
        }
    }
}
