/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.gui;

import de.nmichael.efa.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.text.*;

public class EfaAboutDialog extends BaseDialog {

    JPanel infoPanel = new JPanel();
    JLabel nameLabel = new JLabel();
    JLabel versionLabel = new JLabel();
    JLabel languageLabel = new JLabel();
    JLabel copyLabel = new JLabel();
    JLabel urlLabel0 = new JLabel();
    JLabel urlLabel = new JLabel();
    JLabel supportLabel0 = new JLabel();
    JLabel supportLabel = new JLabel();
    JLabel emailLabel0 = new JLabel();
    JLabel emailLabel = new JLabel();
    JLabel gpl1Label = new JLabel();
    JLabel logoLabel = new JLabel();
    JLabel gplLabel = new JLabel();
    JTabbedPane tabbedPane = new JTabbedPane();
    JPanel detailPanel = new JPanel();
    JScrollPane jScrollPane1 = new JScrollPane();
    JTextArea efaInfos = new JTextArea();
    BorderLayout borderLayout3 = new BorderLayout();
    JLabel efaBirthdayLabel = new JLabel();
    JPanel dankePanel = new JPanel();
    JPanel languagePanel = new JPanel();
    BorderLayout borderLayout4 = new BorderLayout();
    BorderLayout borderLayout5 = new BorderLayout();
    JScrollPane jScrollPane2 = new JScrollPane();
    JScrollPane jScrollPane3 = new JScrollPane();
    JTextArea danke = new JTextArea();
    JTextArea languages = new JTextArea();
    JLabel devNoteLabel = new JLabel();

    public EfaAboutDialog(Frame parent) {
        super(parent, Daten.EFA_LONGNAME, International.getStringWithMnemonic("Schließen"));
    }

    public EfaAboutDialog(JDialog parent) {
        super(parent, Daten.EFA_LONGNAME, International.getStringWithMnemonic("Schließen"));
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());
        nameLabel.setFont(new java.awt.Font("Dialog", 1, (Dialog.getFontSize() > 0 ? Dialog.getFontSize() + 6 : 18)));
        nameLabel.setForeground(Color.black);
        nameLabel.setText(Daten.EFA_LONGNAME);
        versionLabel.setText("Version 0.1"); // do not internationalize
        languageLabel.setText(International.getString("Sprache") + ": "
                + International.getLanguageDescription());
        copyLabel.setText("Copyright (c) 2001-" + Daten.COPYRIGHTYEAR + " by Nicolas Michael"); // do not internationalize
        urlLabel0.setText(International.getString("Homepage") + ": ");
        urlLabel.setForeground(Color.blue);
        urlLabel.setText(Daten.EFAURL);
        urlLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                urlLabel_mouseClicked(e);
            }

            public void mouseEntered(MouseEvent e) {
                label_mouseEntered(e);
            }

            public void mouseExited(MouseEvent e) {
                label_mouseExited(e);
            }
        });
        supportLabel0.setText(International.getString("Hilfe und Support") + ": ");
        supportLabel.setForeground(Color.blue);
        supportLabel.setText(Daten.EFASUPPORTURL);
        supportLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                supportLabel_mouseClicked(e);
            }

            public void mouseEntered(MouseEvent e) {
                label_mouseEntered(e);
            }

            public void mouseExited(MouseEvent e) {
                label_mouseExited(e);
            }
        });
        infoPanel.setLayout(new GridBagLayout());
        //infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 10));
        /*
        emailLabel0.setText(International.getString("email") + ": ");
        emailLabel.setForeground(Color.blue);
        emailLabel.setText(Daten.EMAILINFO);
        emailLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseEntered(MouseEvent e) {
                label_mouseEntered(e);
            }

            public void mouseExited(MouseEvent e) {
                label_mouseExited(e);
            }

            public void mouseClicked(MouseEvent e) {
                emailLabel_mouseClicked(e);
            }
        });
        */
        versionLabel.setForeground(Color.black);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        logoLabel.setIcon(getIcon(Daten.getEfaImage(2)));
        gpl1Label.setText(International.getString("efa unterliegt den") + " ");
        gplLabel.setForeground(Color.blue);
        gplLabel.setText(International.getMessage("Lizenzbestimmungen der {license}", "GPL v2"));
        gplLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                gplLabel_mouseClicked(e);
            }

            public void mouseEntered(MouseEvent e) {
                label_mouseEntered(e);
            }

            public void mouseExited(MouseEvent e) {
                label_mouseExited(e);
            }
        });
        devNoteLabel.setText(International.getMessage("Diese Version ist eine Entwicklerversion in {status}-Qualität!",
                "Beta"));
        devNoteLabel.setForeground(Color.red);
        devNoteLabel.setVisible(false);
        detailPanel.setLayout(borderLayout3);
        jScrollPane1.setPreferredSize(new Dimension(400, 300));
        efaBirthdayLabel.setForeground(Color.red);
        efaBirthdayLabel.setText("efaBirthdayLabel"); // do not internationalize
        dankePanel.setLayout(borderLayout4);
        languagePanel.setLayout(borderLayout5);
        infoPanel.add(logoLabel, new GridBagConstraints(0, 0, 1, 7, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(0, 0, 0, 20), 0, 0));
        infoPanel.add(nameLabel, new GridBagConstraints(1, 0, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 93, 0));
        infoPanel.add(versionLabel, new GridBagConstraints(1, 1, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 230, 0));
        infoPanel.add(languageLabel, new GridBagConstraints(1, 2, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 230, 0));
        infoPanel.add(copyLabel, new GridBagConstraints(1, 3, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 65, 0));
        infoPanel.add(urlLabel0, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 1, 0));
        infoPanel.add(urlLabel, new GridBagConstraints(2, 4, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 1, 0));
        infoPanel.add(supportLabel0, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 1, 0));
        infoPanel.add(supportLabel, new GridBagConstraints(2, 5, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 1, 0));
        //infoPanel.add(emailLabel0, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 0, 0));
        //infoPanel.add(emailLabel, new GridBagConstraints(2, 6, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 0, 0));
        infoPanel.add(gpl1Label, new GridBagConstraints(1, 7, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 0, 0));
        infoPanel.add(gplLabel, new GridBagConstraints(3, 7, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 20), 0, 0));
        infoPanel.add(devNoteLabel, new GridBagConstraints(1, 8, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 20), 0, 0));
        infoPanel.add(efaBirthdayLabel, new GridBagConstraints(0, 11, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        tabbedPane.add(infoPanel, International.getString("Über efa"));
        tabbedPane.add(detailPanel, International.getString("Systeminformationen"));
        tabbedPane.add(languagePanel, International.getString("Sprachen"));
        tabbedPane.add(dankePanel, International.getString("Danksagungen"));
        detailPanel.add(jScrollPane1, BorderLayout.CENTER);
        dankePanel.add(jScrollPane2, BorderLayout.CENTER);
        jScrollPane2.getViewport().add(danke, null);
        languagePanel.add(jScrollPane3, BorderLayout.CENTER);
        jScrollPane3.getViewport().add(languages, null);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        versionLabel.setText(International.getString("Version") + " " + Daten.VERSION + "   (" + Daten.VERSIONID + ")");
        Vector infos = Daten.getEfaInfos();
        for (int i = 0; infos != null && i < infos.size(); i++) {
            efaInfos.append((String) infos.get(i) + "\n");
        }
        if (infos == null) {
            efaInfos.append(International.getString("Keine Systeminformationen verfügbar."));
        }
        jScrollPane1.getViewport().add(efaInfos, null);
        efaInfos.setCaretPosition(0);
        efaInfos.setEditable(false);

        if (EfaUtil.getEfaBirthday() == 5) {
            efaBirthdayLabel.setText(International.getMessage("{n} Jahre efa", 5) + ": "
                    + International.getString("Erste Veröffentlichung am 15.07.2001"));
            efaBirthdayLabel.setVisible(true);
        } else {
            efaBirthdayLabel.setVisible(false);
        }

        danke.setEditable(false);
        danke.append(International.getString("Folgenden Personen und Organisationen gilt Dank für die Unterstützung von efa:") + "\n"
                + "\n"
                + International.getString("Mitwirkende") + ":\n"
                + "* Dennis Klopke (efa Logo)\n"
                + "* Jonas Binding (Development)\n"
                + "* Kay Hannay (efaLive)\n"
                + "* Martin Grüning (Documentation)\n"
                + "* Thilo Coblenzer (efa Kiosk Howto)\n"
                + "* Velten Heyn (Vereinsarbeit)\n"
                + "\n"
                + International.getString("Unterstützung, Zusammenarbeit und Technologie") + ":\n"
                + "* Apache Software Foundation (FOP Plugin, XML Plugin)\n"
                + "* Deutscher Kanu-Verband (Kanu-eFB)\n"
                + "* Deutscher Ruderverband (Fahrtenwettbewerbe)\n"
                + "* Enterprise Distributed Technologies (FTP Plugin)\n"
                + "* Jonathan Stott (JSunrise Plugin)\n"
                + "* KDE-Team (Icons)\n"
                + "* Landesruderverband Berlin (Fahrtenwettbewerbe)\n"
                + "* Matthias Käppler (Signpost OAuth)\n"
                + "* Open Icon Library (Icons)\n"
                + "* Ralf Ludwig (efa Evangelist)\n"
                + "* Robert Harder (Base64 Implementation)\n"
                + "* Sun Microsystems & Oracle (Java Technology)\n"
                + "* Wolfgang Krutzke (Waters List for Germany)\n"
                + "* Yahoo! (Weather Information)\n"
                + "* World Wide Web Consortium (XML Plugin)");

        String translations = "";
        try {
            String[] bundles = International.getLanguageBundles();
            for (int i = 0; bundles != null && i < bundles.length; i++) {
                Locale loc = new Locale(bundles[i]);
                ResourceBundle bundle = ResourceBundle.getBundle(International.BUNDLE_NAME, loc);
                International.getString("+++TRANSLATED_BY+++"); // dummy, just to make make_i18n_keys.pl find this key ;)
                try {
                    translations += loc.getDisplayName() + ": " + bundle.getString("+++TRANSLATED_BY+++") + "\n";
                } catch (Exception translationNotFound) {
                    translations += loc.getDisplayName() + "\n";
                }
            }
        } catch (Exception e) {
            translations = "Could not get any language information."; // no need to translate
        }
        languages.setEditable(false);
        languages.append(International.getString("efa wurde in die folgenden Sprachen übersetzt:") + "\n\n" + translations + "\n"
                + International.getString("Bitte unterstütze uns bei der Übersetzung in weitere Sprachen!") + "\n" + Daten.EFADEVURL);
    }

    void label_mouseEntered(MouseEvent e) {
        try {
            JLabel label = (JLabel) e.getSource();
            label.setForeground(Color.red);
        } catch (Exception eignore) {
        }
    }

    void label_mouseExited(MouseEvent e) {
        try {
            JLabel label = (JLabel) e.getSource();
            label.setForeground(Color.blue);
        } catch (Exception eignore) {
        }
    }

    void urlLabel_mouseClicked(MouseEvent e) {
        if (Daten.applID == Daten.APPL_EFABH) {
            return;
        }
        BrowserDialog.openExternalBrowser(this, Daten.EFAURL);
    }

    void supportLabel_mouseClicked(MouseEvent e) {
        if (Daten.applID == Daten.APPL_EFABH) {
            return;
        }
        BrowserDialog.openExternalBrowser(this, Daten.EFASUPPORTURL);
    }

    void emailLabel_mouseClicked(MouseEvent e) {
        if (Daten.applID == Daten.APPL_EFABH) {
            return;
        }
        if (Daten.INTERNET_EFAMAIL != null) {
            BrowserDialog.openInternalBrowser(this, "Browser", "file:" + HtmlFactory.createMailto(Daten.EMAILINFO), 700, 600);
        }
    }

    void gplLabel_mouseClicked(MouseEvent e) {
        BrowserDialog.openInternalBrowser(this, "Browser", "file:" + Daten.efaDocDirectory + Daten.EFA_LICENSE, 700, 600);
    }

}
