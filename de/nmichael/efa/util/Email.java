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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.EmailSenderThread;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.gui.MultiInputDialog;
import java.awt.AWTEvent;
import java.util.Vector;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.swing.JDialog;

public class Email {

    public static final String MAILTO = "mailto:";
    public static final String EMAIL_INDIVIDUAL = "*";

    public static String getEmailAddressFromMailtoString(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.startsWith(MAILTO)) {
            String addr = s.substring(MAILTO.length()).trim();
            if (addr.length() == 0) {
                return null;
            }
            return addr;
        }
        return null;
    }

    public static Vector<String> getAddresses(String addressList) {
        Vector<String> v = new Vector<String>();
        int pos;
        do {
            pos = addressList.indexOf(",");
            if (pos < 0) {
                pos = addressList.indexOf(" ");
            }
            if (pos >= 0) {
                String addr = addressList.substring(0, pos).trim();
                if (addr.length() > 0) {
                    v.add(addr);
                }
                addressList = (pos+1 >= addressList.length() ? "" : addressList.substring(pos+1));
            } else {
                if (addressList.trim().length() > 0) {
                    v.add(addressList.trim());
                }
            }
        } while(pos >= 0);
        return v;
    }

    public static boolean enqueueMessage(String address, String subject,
            String text, String[] attachmentFileNames, boolean deleteAttachmentFiles) {
        Vector<String> addresses = getAddresses(address);
        return enqueueMessage(addresses, subject, text,
                attachmentFileNames, deleteAttachmentFiles);
    }

    public static boolean enqueueMessage(Vector<String> addresses, String subject,
            String text, String[] attachmentFileNames, boolean deleteAttachmentFiles) {
        try {
            javax.mail.Multipart multipart = createMessage(addresses, subject, text,
                    attachmentFileNames, deleteAttachmentFiles);
            if (Daten.emailSenderThread != null) {
                Daten.emailSenderThread.enqueueMessage(multipart, addresses, subject,
                        attachmentFileNames, deleteAttachmentFiles);
            Logger.log(Logger.INFO, Logger.MSG_CORE_MAILENQUEUED,
                    LogString.emailSuccessfullyEnqueued(subject));
            return true;
            }
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_CORE_MAILFAILED,
                    LogString.emailSendFailed(subject, e.toString() + " " + e.getMessage()));
            Logger.logdebug(e);
        }
        return false;
    }

    public static boolean sendMessage(String address, String subject,
            String text, String[] attachmentFileNames, boolean deleteAttachmentFiles) {
        Vector<String> addresses = getAddresses(address);
        return sendMessage(addresses, subject, text,
                attachmentFileNames, deleteAttachmentFiles);
    }

    public static boolean sendMessage(Vector<String> addresses, String subject,
            String text, String[] attachmentFileNames, boolean deleteAttachmentFiles) {
        try {
            javax.mail.Multipart multipart = createMessage(addresses, subject, text,
                    attachmentFileNames, deleteAttachmentFiles);
            if (Daten.emailSenderThread == null) {
                Daten.emailSenderThread = new EmailSenderThread();
                Daten.emailSenderThread.start();
            }
            if (Daten.emailSenderThread != null) {
                return Daten.emailSenderThread.sendMessage(multipart, addresses, subject,
                        attachmentFileNames, deleteAttachmentFiles);
            }
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_CORE_MAILFAILED,
                    LogString.emailSendFailed(subject, e.toString()));
            Logger.logdebug(e);
        }
        return false;
    }

    private static javax.mail.Multipart createMessage(Vector<String> addresses, String subject,
            String text, String[] attachmentFileNames, boolean deleteAttachmentFiles) {
        try {

            javax.mail.Multipart multipart = new javax.mail.internet.MimeMultipart();

            // message body
            javax.mail.internet.MimeBodyPart messageBodyPart =
                    new javax.mail.internet.MimeBodyPart();
            String mailSignature = Daten.efaConfig.getValueEfaDirekt_emailSignatur();
            if (mailSignature != null && mailSignature.trim().length() == 0) {
                mailSignature = null;
            }            messageBodyPart.setText(text + (mailSignature != null ? "\n\n-- \n"
                    + EfaUtil.replace(mailSignature, "$$", "\n", true) : ""), Daten.ENCODING_ISO);
            messageBodyPart.setHeader("Content-Type", "text/plain; charset=" + Daten.ENCODING_ISO);
            multipart.addBodyPart(messageBodyPart);

            // attachments
            for (int i = 0; attachmentFileNames != null && i < attachmentFileNames.length; i++) {
                messageBodyPart = new javax.mail.internet.MimeBodyPart();
                FileDataSource source = new FileDataSource(attachmentFileNames[i]);
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(EfaUtil.getFilenameWithoutPath(attachmentFileNames[i]));
                multipart.addBodyPart(messageBodyPart);
            }
            return multipart;
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public static String getEmailStringGuiDialog(String s) {
        String email = getEmailAddressFromMailtoString(s);
        IItemType[] items = new IItemType[2];
        items[0] = new ItemTypeString("EMAIL", (email != null ? email : ""),
                IItemType.TYPE_PUBLIC, "", International.getString("email-Adresse(n)"));
        items[0].setNotNull(true);
        ((ItemTypeString)items[0]).setEnabled(!(email != null && email.equals(EMAIL_INDIVIDUAL)));
        items[1] = new ItemTypeBoolean("INDIVIDUAL", (email != null && email.equals(EMAIL_INDIVIDUAL)),
                IItemType.TYPE_PUBLIC, "", International.getString("ausgewertete Person (individuelle Statistik)"));
        items[1].registerItemListener(new EmailGuiDlgListener((ItemTypeString)items[0],
                                                              (ItemTypeBoolean)items[1]));
        if (MultiInputDialog.showInputDialog((JDialog)null, International.getString("email-Versand"), items)) {
            email = items[0].toString().trim();
            if (((ItemTypeBoolean)items[1]).getValue()) {
                email = EMAIL_INDIVIDUAL;
            }
            if (email != null && email.length() > 0) {
                return MAILTO + email;
            }
        }
        return null;
    }

    static class EmailGuiDlgListener implements IItemListener {

        ItemTypeString emailField;
        ItemTypeBoolean individualCheckbox;

        public EmailGuiDlgListener(ItemTypeString emailField, ItemTypeBoolean individualCheckbox) {
            this.emailField = emailField;
            this.individualCheckbox = individualCheckbox;
        }

        public void itemListenerAction(IItemType itemType, AWTEvent event) {

            if (itemType.getName().equals(individualCheckbox.getName())) {
                individualCheckbox.getValueFromGui();
                emailField.setEnabled(!individualCheckbox.getValue());
                if (individualCheckbox.getValue()) {
                    emailField.parseAndShowValue(EMAIL_INDIVIDUAL);
                }
            }
        }

    }

}
