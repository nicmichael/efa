/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.drv;

import de.nmichael.efa.data.efawett.CertInfos;
import de.nmichael.efa.*;
import de.nmichael.efa.core.*;
import de.nmichael.efa.gui.EnterPasswordDialog;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.Base64;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.security.cert.*;
import java.net.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class KeysAdminFrame extends JDialog implements ActionListener {

    JDialog parent;
    Object[] keys;
    JPanel jPanel1 = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel jPanel2 = new JPanel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JButton closeButton = new JButton();
    JPanel jPanel3 = new JPanel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JButton newButton = new JButton();
    JButton editButton = new JButton();
    JButton deleteButton = new JButton();
    JScrollPane jScrollPane1 = new JScrollPane();
    JTable keyTable;
    JButton exportCertButton = new JButton();
    JButton setDefaultButton = new JButton();
    JButton importCertButton = new JButton();

    public KeysAdminFrame(JDialog parent) throws Exception {
        super(parent);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        Dialog.frameOpened(this);
        this.parent = parent;
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!EfaUtil.canOpenFile(Daten.efaDataDirectory + Main.drvConfig.KEYSTORE_FILE)) {
            enterNewKeyPassword();
            if (Main.drvConfig.keyPassword == null) {
                cancel();
                throw new Exception("Falsches Paßwort!");
            }
        }
        if (Main.drvConfig.keyPassword == null) {
            enterKeyPassword();
        }
        if (Main.drvConfig.keyPassword == null || !loadKeys()) {
            cancel();
            Main.drvConfig.keyPassword = null;
            throw new Exception("Falsches Paßwort!");
        }
        displayKeys();

        EfaUtil.pack(this);
        // this.requestFocus();
    }

    public static void enterKeyPassword() {
        String pwd = EnterPasswordDialog.enterPassword(Dialog.frameCurrent(),
                "Bitte Schlüssel-Paßwort eingeben:", false);
        if (pwd != null) {
            Main.drvConfig.keyPassword = pwd.toCharArray();
        } else {
            Main.drvConfig.keyPassword = null;
        }

    }

    public static void enterNewKeyPassword() {
        String pwd = EnterPasswordDialog.enterPassword(Dialog.frameCurrent(),
                "Damit die erstellten Schlüssel vor unbefugten Zugriffen sicher sind,\n"
                + "werden sie durch ein Paßwort geschützt. Dieses Paßwort muß unter\n"
                + "allen Umständen geheim bleiben, da von ihm die Sicherheit des\n"
                + "Gesamtsystems abhängt.\n"
                + "Gib daher bitte jetzt ein Paßwort ein, das möglichst lang und\n"
                + "vor allem nicht zu erraten ist (kein Wort der deutschen Sprache!).\n"
                + "Das Paßwort muß mindestens 8 Zeichen lang sein und muß von den vier\n"
                + "Zeichengruppen 'Kleinbuchstaben', 'Großbuchstaben', 'Ziffern' und\n"
                + "'sonstige Zeichen' mindestens drei Gruppen enthalten.", true);
        if (pwd != null) {
            Main.drvConfig.keyPassword = pwd.toCharArray();
        } else {
            Main.drvConfig.keyPassword = null;
        }
        if (Main.drvConfig.keyPassword != null) {
            Logger.log(Logger.INFO, "Neues Paßwort für Schlüsselspeicher festgelegt.");
        }
    }

    boolean loadKeys() {
        if (Daten.keyStore != null && Daten.keyStore.isKeyStoreReady()) {
            return true;
        }
        Daten.keyStore = new EfaKeyStore(Daten.efaDataDirectory + Main.drvConfig.KEYSTORE_FILE, Main.drvConfig.keyPassword);
        if (!Daten.keyStore.isKeyStoreReady()) {
            Dialog.error("KeyStore kann nicht geladen werden:\n" + Daten.keyStore.getLastError());
        }
        return Daten.keyStore.isKeyStoreReady();
    }

    void displayKeys() {
        try {
            String alias;
            Vector _keys = new Vector();
            for (Enumeration e = Daten.keyStore.getAliases(); e.hasMoreElements();) {
                alias = (String) e.nextElement();
                _keys.add(alias);
            }

            keys = _keys.toArray();
            Arrays.sort(keys);

            String[][] tableData = new String[keys.length][5];
            for (int i = 0; i < keys.length; i++) {
                X509Certificate cert = Daten.keyStore.getCertificate((String) keys[i]);
                tableData[i][0] = (String) keys[i];
                tableData[i][1] = CertInfos.getValidityYears(cert);
                tableData[i][2] = EfaUtil.date2String(cert.getNotBefore());
                tableData[i][3] = EfaUtil.date2String(cert.getNotAfter());
                tableData[i][4] = (Main.drvConfig.schluessel.equals(keys[i]) ? "Standard" : "");
            }
            String[] tableHeader = new String[5];
            tableHeader[0] = "Schlüssel-ID";
            tableHeader[1] = "gültig für";
            tableHeader[2] = "gültig von";
            tableHeader[3] = "gültig bis";
            tableHeader[4] = "Status";

            if (keyTable != null) {
                jScrollPane1.getViewport().remove(keyTable);
            }
            keyTable = new JTable(tableData, tableHeader);
            jScrollPane1.getViewport().add(keyTable, null);
        } catch (Exception e) {
            Dialog.error("Kann die Schlüsselliste nicht anzeigen: " + e.toString());
        }
    }

    void setDefaultKey(String alias) {
        try {
            X509Certificate cert = Daten.keyStore.getCertificate(alias);
            Date date = (new GregorianCalendar()).getTime();
            if (date.after(cert.getNotAfter()) || date.before(cert.getNotBefore())) {
                Dialog.error("Der Schlüssel ist zum aktuellen Datum ungültig\n"
                        + "und kann nicht als Standardschlüssel festgelegt werden!");
                return;
            }
        } catch (Exception ee) {
            Dialog.error(ee.toString());
            return;
        }

        Main.drvConfig.schluessel = alias;
        if (!Main.drvConfig.writeFile()) {
            Dialog.error("Konfigurationsdatei\n" + Main.drvConfig.getFileName() + "\nkann nicht geschrieben werden!");
            Main.drvConfig.schluessel = "";
            Logger.log(Logger.WARNING, "Kein Schlüssel als Standardschlüssel ausgewählt.");
        } else {
            Logger.log(Logger.INFO, "Schlüssel " + alias + " als neuer Standardschlüssel ausgewählt.");
        }
    }

    // ActionHandler Events
    public void keyAction(ActionEvent evt) {
        if (evt == null || evt.getActionCommand() == null) {
            return;
        }
        if (evt.getActionCommand().equals("KEYSTROKE_ACTION_0")) { // Escape
            cancel();
        }
    }

    // Initialisierung des Frames
    private void jbInit() throws Exception {
        ActionHandler ah = new ActionHandler(this);
        try {
            ah.addKeyActions(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW,
                    new String[]{"ESCAPE", "F1"}, new String[]{"keyAction", "keyAction"});
            jPanel1.setLayout(borderLayout1);
            jPanel2.setLayout(gridBagLayout1);
            closeButton.setText("Schließen");
            closeButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    closeButton_actionPerformed(e);
                }
            });
            jPanel3.setLayout(gridBagLayout2);
            newButton.setText("Neuen Schlüssel erstellen");
            newButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    newButton_actionPerformed(e);
                }
            });
            editButton.setText("Zertifikatinfos anzeigen");
            editButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    editButton_actionPerformed(e);
                }
            });
            deleteButton.setText("Schlüssel sperren");
            deleteButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    deleteButton_actionPerformed(e);
                }
            });
            exportCertButton.setText("Zertifikat exportieren");
            exportCertButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    exportCertButton_actionPerformed(e);
                }
            });
            setDefaultButton.setText("Als Standard festlegen");
            setDefaultButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setDefaultButton_actionPerformed(e);
                }
            });
            this.setTitle("Schlüsselverwaltung");
            importCertButton.setText("Zertifikat importieren");
            importCertButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    importCertButton_actionPerformed(e);
                }
            });
            this.getContentPane().add(jPanel1, BorderLayout.CENTER);
            jPanel1.add(jPanel2, BorderLayout.SOUTH);
            jPanel2.add(closeButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            jPanel1.add(jPanel3, BorderLayout.EAST);
            jPanel3.add(newButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            jPanel3.add(editButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            jPanel3.add(deleteButton, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            jPanel3.add(exportCertButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            jPanel3.add(setDefaultButton, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            jPanel3.add(importCertButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            jPanel1.add(jScrollPane1, BorderLayout.CENTER);
        } catch (NoSuchMethodException e) {
            System.err.println("Error setting up ActionHandler");
        }
    }

    /**Overridden so we can exit when window is closed*/
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            cancel();
        }
        super.processWindowEvent(e);
    }

    /**Close the dialog*/
    void cancel() {
        Dialog.frameClosed(this);
        dispose();
    }

    /**Close the dialog on a button event*/
    public void actionPerformed(ActionEvent e) {
    }

    void newButton_actionPerformed(ActionEvent e) {
        String _nr = Dialog.inputDialog("Schlüsselnummer", "Bitte gib eine Nummer für den neu zu erstellenden Schlüssel ein:");
        if (_nr == null) {
            return;
        }
        int nr = EfaUtil.string2int(_nr, -1);
        if (nr < 1 || nr > 99) {
            Dialog.error("Ungültige Nummer: Die Nummer muß zwischen 1 und 99 liegen.");
            return;
        }
        _nr = Integer.toString(nr);
        if (_nr.length() < 2) {
            _nr = "0" + _nr;
        }

        String alias = "drv" + _nr;
        boolean existiert = false;
        for (int i = 0; keys != null && i < keys.length; i++) {
            if (keys[i].equals(alias)) {
                existiert = true;
            }
        }
        if (existiert) {
            Dialog.error("Es existiert bereits ein Schlüssel mit dieser Nummer!");
            return;
        }

        String _validity = Dialog.inputDialog("Gültigkeitsdauer", "Bitte gib an, für welche Wettbewerbsjahre der Schlüssel gelten soll (JJJJ-JJJJ):");
        if (_validity == null) {
            return;
        }
        TMJ tmj = EfaUtil.string2date(_validity, 0, 0, 0);
        if (tmj.tag < 2000) {
            Dialog.error("Ungültiges Jahr: " + tmj.tag);
            return;
        }
        if (tmj.monat > 0 && tmj.monat < 2000) {
            Dialog.error("Ungültiges Jahr: " + tmj.monat);
            return;
        }
        if (tmj.monat > 0 && tmj.tag > tmj.monat) {
            Dialog.error("Das Startjahr " + tmj.tag + " muß vor dem Endjahr " + tmj.monat + " liegen!");
            return;
        }
        if (tmj.monat == 0) {
            tmj.monat = tmj.tag;
        }
        String cn = alias + "_" + tmj.tag;
        if (tmj.monat > tmj.tag) {
            cn += "-" + tmj.monat;
        }

        int tage = EfaUtil.getDateDiff(EfaUtil.getCurrentTimeStampDD_MM_YYYY(), "31.12." + (tmj.monat + 1)) - 1;

        try {
            CA ca;

            try {
                ca = new CA();
            } catch (Exception ee) {
                Dialog.error(ee.toString());
                return;
            }

            ca.runKeytool("-genkey"
                    + " -alias " + alias + "_priv"
                    + " -keyalg DSA -keysize 1024 -sigalg SHA1withDSA"
                    + " -validity " + tage
                    + " -dname CN=\"Deutscher_Ruderverband,O=" + cn + ",C=DE\"", Main.drvConfig.keyPassword);

            ca.runKeytool("-certreq -alias " + alias + "_priv"
                    + " -file " + Daten.efaTmpDirectory + "certreq.csr", Main.drvConfig.keyPassword);

            if (!ca.signRequest(Daten.efaTmpDirectory + "certreq.csr", Daten.efaTmpDirectory + "certreq.pem", tage)) {
                Dialog.error("Fehler beim Signieren des Zertifikats durch die CA.");
                Dialog.infoDialog("Der erstellte Schlüssel wird nun wieder gelöscht.");
                ca.runKeytool("-delete"
                        + " -alias " + alias + "_priv", Main.drvConfig.keyPassword);
                Dialog.infoDialog("Schlüssel wurde gelöscht", "Der erstellte Schlüssel wurde erfolgreich gelöscht.");
                return;
            }

            ca.runKeytool("-import -alias " + alias
                    + " -file " + Daten.efaTmpDirectory + "certreq.pem", null);

            Logger.log(Logger.INFO, "Neuer Schlüssel " + alias + " (und privater Schlüssel " + alias + "_priv) erstellt.");

            (new File(Daten.efaTmpDirectory + "certreq.csr")).delete();
            (new File(Daten.efaTmpDirectory + "certreq.pem")).delete();
            Daten.keyStore.reload();
            if (keys == null || keys.length == 0) {
                // neu erzeugten Schlüssel als Standardschlüssel festlegen
                setDefaultKey(alias + "_priv");
            }
        } catch (Exception ee) {
            Dialog.error(ee.toString());
        }

        displayKeys();
    }

    void editButton_actionPerformed(ActionEvent e) {
        if (keyTable == null) {
            return;
        }
        if (keyTable.getSelectedRow() < 0) {
            Dialog.error("Bitte wähle zuerst einen Schlüssel aus!");
            return;
        }
        if (keyTable.getSelectedRow() >= keys.length) {
            Dialog.error("Oops! Der ausgewählte Schlüssel existiert nicht!");
            return;
        }
        String alias = (String) keys[keyTable.getSelectedRow()];

        try {
            String s = CertInfos.getCertInfos(Daten.keyStore.getCertificate(alias), null);
            Dialog.infoDialog("Informationen zum Zertifikat für " + alias, s);
        } catch (Exception ee) {
            Dialog.error(ee.toString());
        }
    }

    void deleteButton_actionPerformed(ActionEvent e) {
        Dialog.infoDialog("Funktion noch nicht implementiert.");
    }

    void exportCertButton_actionPerformed(ActionEvent e) {
        if (keyTable == null) {
            return;
        }
        if (keyTable.getSelectedRow() < 0) {
            Dialog.error("Bitte wähle zuerst einen Schlüssel aus!");
            return;
        }
        if (keyTable.getSelectedRow() >= keys.length) {
            Dialog.error("Oops! Der ausgewählte Schlüssel existiert nicht!");
            return;
        }
        String alias = (String) keys[keyTable.getSelectedRow()];
        if (alias == null) {
            Dialog.error("Oops! Der ausgewählte Schlüssel ist NULL!");
            return;
        }
        if (alias.endsWith("_priv")) {
            Dialog.error("Zertifikate werden nur für öffentliche Schlüssel ausgestellt!\nBitte wähle einen Schlüssel mit Namen drvXX.");
            return;
        }

        String certFile = Daten.efaDataDirectory + alias + ".cert";
        if ((new File(certFile)).isFile()
                && Dialog.yesNoDialog("Datei existiert bereits",
                "Die Zertifikatsdatei\n"
                + certFile + "\n"
                + "existiert bereits.\n"
                + "Soll sie überschrieben werden?") != Dialog.YES) {
            return;
        }
        CA ca;
        try {
            ca = new CA();
        } catch (Exception ee) {
            Dialog.error(ee.toString());
            return;
        }
        ca.runKeytool("-export -alias " + alias
                + " -file " + certFile, null);
        Dialog.infoDialog("Zertifikat exportiert",
                "Das Zertifikat für " + alias + " wurde erfolgreich in die Datei\n"
                + certFile + "\n"
                + "exportiert.");
        if (Dialog.yesNoDialog("Zertifikat in efaWett hinterlegen",
                "Soll das Zertifikat jetzt hochgeladen\n"
                + "und in efaWett hinterlegt werden?") != Dialog.YES) {
            return;
        }
        if (!Dialog.okAbbrDialog("Internet-Verbindung",
                "Bitte stelle eine Verbindung zum Internet her.")) {
            return;
        }
        try {
            int filesize = (int) (new File(certFile)).length();
            byte[] buf = new byte[filesize];
            FileInputStream f = new FileInputStream(certFile);
            f.read(buf, 0, filesize);
            f.close();
            String data = Base64.encodeBytes(buf);
            data = EfaUtil.replace(data, "=", "**0**", true); // "=" als "**0**" maskieren

            String request = Main.drvConfig.makeScriptRequestString(DRVConfig.ACTION_UPLCERT, "cert=" + alias + ".cert64", "data=" + data, null, null, null, null);
            int pos = request.indexOf("?");
            if (pos < 0) {
                Dialog.error("efaWett-Anfrage zum Hochladen des Zertifikats konnte nicht erstellt werden.");
                return;
            }
            String url = request.substring(0, pos);
            String content = request.substring(pos + 1, request.length());

            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
            conn.setRequestMethod("POST");
            conn.setAllowUserInteraction(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-length", Integer.toString(content.length()));
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(content);
            out.flush();
            out.close();
            conn.disconnect();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            boolean ok = true;
            String z;
            while ((z = in.readLine()) != null) {
                if (!z.equals("OK")) {
                    Dialog.error("Fehler beim Hochladen des Zertifikats: " + z);
                    return;
                } else {
                    Dialog.infoDialog("Zertifikat erfolgreich hochgeladen.");
                }
            }
        } catch (Exception ee) {
            Dialog.error("Fehler beim Hochladen des Zertifikats: " + ee.toString());
        }
    }

    void setDefaultButton_actionPerformed(ActionEvent e) {
        if (keyTable == null) {
            return;
        }
        if (keyTable.getSelectedRow() < 0) {
            Dialog.error("Bitte wähle zuerst einen Schlüssel aus!");
            return;
        }
        if (keyTable.getSelectedRow() >= keys.length) {
            Dialog.error("Oops! Der ausgewählte Schlüssel existiert nicht!");
            return;
        }
        String alias = (String) keys[keyTable.getSelectedRow()];
        if (alias == null) {
            Dialog.error("Oops! Der ausgewählte Schlüssel ist NULL!");
            return;
        }
        if (!alias.endsWith("_priv")) {
            Dialog.error("Nur private Schlüssel können als Standardschlüssel markiert werden!\nBitte wähle einen privaten Schlüssel aus.");
            return;
        }

        setDefaultKey(alias);
        displayKeys();
    }

    void closeButton_actionPerformed(ActionEvent e) {
        cancel();
    }

    void importCertButton_actionPerformed(ActionEvent e) {
        String keyfile = Dialog.dateiDialog(Dialog.frameCurrent(), "Öffentlichen Schlüssel auswählen",
                "Öffentlicher Schlüssel (*.cert)", "cert", Daten.efaDataDirectory, false);
        if (keyfile == null) {
            return;
        }
        DRVSignaturFrame.importKey(keyfile);
        displayKeys();
    }
}
