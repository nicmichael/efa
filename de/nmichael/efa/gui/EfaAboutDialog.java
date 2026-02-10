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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.HtmlFactory;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;


public class EfaAboutDialog extends BaseDialog {

	private static final long serialVersionUID = -4809082876158920190L;
	JPanel aboutEfaPanel = new JPanel();
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
    JPanel systemInfoPanel = new JPanel();
    JScrollPane systemInfoScrollPane = new JScrollPane();
    JTextArea efaInfosText = new JTextArea();
    BorderLayout borderLayout3 = new BorderLayout();
    JLabel efaBirthdayLabel = new JLabel();
    JPanel dankePanel = new JPanel();
    JPanel languagePanel = new JPanel();
    JPanel changelogPanel = new JPanel();
    JScrollPane changelogScrollPane = new JScrollPane();
    JEditorPane changelogText = new JEditorPane();
    BorderLayout borderLayout4 = new BorderLayout();
    BorderLayout borderLayout5 = new BorderLayout();
    BorderLayout borderLayout6 = new BorderLayout();
    BorderLayout borderLayout7 = new BorderLayout();
    JScrollPane dankeScrollPane = new JScrollPane();
    JScrollPane languageScrollPane = new JScrollPane();
    JTextArea dankeText = new JTextArea();
    JTextArea languagesText = new JTextArea();
    JLabel devNoteLabel = new JLabel();
    
    JPanel librariesPanel = new JPanel();
    JTextArea librariesText = new JTextArea();
    JScrollPane librariesScrollPane = new JScrollPane();
    
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
        aboutEfaPanel.setLayout(new GridBagLayout());
        systemInfoPanel.setLayout(borderLayout3);
        systemInfoScrollPane.setPreferredSize(new Dimension(400, 290));
        dankePanel.setLayout(borderLayout4);
        languagePanel.setLayout(borderLayout5);
        librariesPanel.setLayout(borderLayout6);
        changelogPanel.setLayout(borderLayout7);
        
        iniAboutEfaPanel();
        iniEfaSystemInfos();
        iniDanksagungenPanel();
        iniTranslationsPanel();
        iniBibliothekenPanel();
        iniChangelogPanel();
        
        
        tabbedPane.add(aboutEfaPanel, International.getString("Über efa"));
        tabbedPane.add(changelogPanel, International.getString("Aenderungsprotokoll"));
        tabbedPane.add(systemInfoPanel, International.getString("Systeminformationen"));
        tabbedPane.add(languagePanel, International.getString("Sprachen"));
        tabbedPane.add(dankePanel, International.getString("Danksagungen"));
        tabbedPane.add(librariesPanel, International.getString("Bibliotheken"));

        systemInfoPanel.add(systemInfoScrollPane, BorderLayout.CENTER);
        dankePanel.add(dankeScrollPane, BorderLayout.CENTER);
        languagePanel.add(languageScrollPane, BorderLayout.CENTER);
        librariesPanel.add(librariesScrollPane, BorderLayout.CENTER);
        changelogPanel.add(changelogScrollPane, BorderLayout.CENTER);
        
        dankeScrollPane.getViewport().add(dankeText, null);
        languageScrollPane.getViewport().add(languagesText, null);
        librariesScrollPane.getViewport().add(librariesText, null);
        librariesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        changelogScrollPane.getViewport().add(changelogText, null);
        changelogScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // this is relevant for sizing the efaAboutDialog with no vertical scrollbar - each scrollpane needs a preferred size
        changelogScrollPane.setPreferredSize(new Dimension(400, 600));
        changelogScrollPane.getVerticalScrollBar().setUnitIncrement(6);
        dankeScrollPane.setPreferredSize(new Dimension(850, 750)); 
        languageScrollPane.setPreferredSize(new Dimension(400, 600));
        librariesScrollPane.setPreferredSize(new Dimension(400, 600));
                
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

    }

	private void iniTranslationsPanel() {
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
        languagesText.setEditable(false);
        languagesText.append(International.getString("efa wurde in die folgenden Sprachen übersetzt:") + "\n\n" + translations + "\n"
                + International.getString("Bitte unterstütze uns bei der Übersetzung in weitere Sprachen!"));
	}

	private void iniDanksagungenPanel() {
		dankeText.setEditable(false);
        dankeText.append(International.getString("Folgenden Personen und Organisationen gilt Dank für die Unterstützung von efa:") + "\n"
                + "\n"
                + International.getString("Mitwirkende") + ":\n"
                + "* Kay Hannay (efaLive)\n"
                + "* Martin Glade (efaCloud)\n"
                + "* Velten Heyn (Vereinsarbeit)\n"
                + "* Stefan Gebers (Development)\n"
                + "* Jonas Binding (Development)\n"
                + "* Dennis Klopke (efa Logo)\n"
                + "* Martin Grüning (Documentation)\n"
                + "* Thilo Coblenzer (efa Kiosk Howto)\n"
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
                + "* World Wide Web Consortium (XML Plugin)");
	}

	private void iniBibliothekenPanel() {
		librariesText.setEditable(false);
		librariesText.append(International.getString("efa setzt folgende Bibliotheken ein:") + "\n\n");
		librariesText.append("Open Icons Library\n\thttps://sourceforge.net/projects/openiconlibrary/\n\n");
		librariesText.append("FlatLaf Look&Feel\n\thttps://www.formdev.com/flatlaf/\n\tFlatLaf is open source licensed under Apache 2.0 license.\n\n");
		librariesText.append("edtFTPj/Free\n\thttps://enterprisedt.com/products/edtftpj/\n\tLicensed under lgpl-2.1 license.\n\n");
		librariesText.append("SignPost OAuth\n\thttps://github.com/mttkay/signpost\n\tLicensed under Apache 2.0 license.\n\n");
		librariesText.append("JavaX Mail\n\thttps://github.com/javaee/javamail\n\tLicensed under COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1\n\n");
		librariesText.append("Apache FOP XML Graphics\n\thttps://xmlgraphics.apache.org/fop/\n\tLicensed under Apache 2.0 license.\n\n");
		librariesText.append("JCraft jsch SSH/SFTP library\n\thttp://www.jcraft.com/jsch/\n\tLicensed under BSD-style license.\n\n");
		librariesText.append("CompoundIcon by Tips4Java\n\thttps://github.com/tips4java/tips4java/blob/main/source/CompoundIcon.java\n\t Licensed under MIT License.\n\n");
	}	
	
	private void iniChangelogPanel() {
		changelogText.setEditable(false);
		changelogText.setContentType("text/html");
		changelogText.setCaretPosition(0);
		
		File changelogfile;
		try {
			changelogfile = new File(Daten.efaDocDirectory+"changelog_"+International.getLanguageID().toLowerCase() +".html");
			if (!changelogfile.canRead()) {
				changelogfile = new File(Daten.efaDocDirectory+"changelog_en.html");
			}
			if (!changelogfile.canRead()) {
				changelogfile=null;
				changelogText.setText("This should show the content of "+Daten.efaDocDirectory+"changelog_en.html");	
			}
			if (changelogfile!=null) {
				changelogText.setPage(changelogfile.toURI().toURL());
			}
		} catch (Exception e) {
			Logger.logdebug(e);
			changelogText.setText("This should show the content of "+Daten.efaDocDirectory+"changelog_en.html");
		}
		
	}
	
	private void iniEfaSystemInfos() {
		Vector <String>infos = Daten.getEfaInfos();
        for (int i = 0; infos != null && i < infos.size(); i++) {
            efaInfosText.append((String) infos.get(i) + "\n");
        }
        if (infos == null) {
            efaInfosText.append(International.getString("Keine Systeminformationen verfügbar."));
        }
        
        //Add Display information
        
    	efaInfosText.append("\n\n\nDisplayInfo\n-------------\n");
        Vector<String> displayInfo = Daten.getDisplayInfos();
        if (displayInfo!=null) {
	        for (int i = 0; displayInfo != null && i < displayInfo.size(); i++) {
	            efaInfosText.append((String) displayInfo.get(i) + "\n");
	        }
        }
        
        
        //Add GUI Debug info, if debug info is activated in efaConfig or by Commandline
        if (Logger.isDebugLogging()||Logger.isDebugLoggingActivatedByCommandLine()) {
	        // Get UI Defaults Properties
        	
        	efaInfosText.append("\n\n\nDisplayFonts\n-------------\n");
	        Vector<String> fonts = EfaUtil.makeFontFamilyVector(false, null);
	        if (fonts!=null) {
	        	efaInfosText.append(fonts.toString());
	        }
	        
        	efaInfosText.append("\n\n\nUIManager.getDefaults()\n-------------\n");
	        Vector <String>lafProperties = Daten.getUIProperties();
	        for (int i = 0; lafProperties != null && i < lafProperties.size(); i++) {
	            efaInfosText.append((String) lafProperties.get(i) + "\n");
	        }
	        
	        // Get HTML CSS Stylesheet Default Rules
	        efaInfosText.append("\n\n\nHTMLEditorKit().getStyleSheet() rules\n-------------\n");
	        Vector <String>htmlCSSRules = Daten.getCSSInfo();
	        for (int i = 0; htmlCSSRules != null && i < htmlCSSRules.size(); i++) {
	            efaInfosText.append((String) htmlCSSRules.get(i) + "\n");
	        }
        }
        systemInfoScrollPane.getViewport().add(efaInfosText, null);
        efaInfosText.setCaretPosition(0);
        efaInfosText.setEditable(false);
	}

	private void iniAboutEfaPanel() {

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

        efaBirthdayLabel.setForeground(Color.red);
        efaBirthdayLabel.setText("efaBirthdayLabel"); // do not internationalize      
        if (EfaUtil.getEfaBirthday() > -1) {
            efaBirthdayLabel.setText(International.getMessage("{n} Jahre efa", EfaUtil.getEfaBirthday()) + ": "
                    + International.getString("Erste Veröffentlichung am 15.07.2001"));
            efaBirthdayLabel.setVisible(true);
        } else {
            efaBirthdayLabel.setVisible(false);
        }        
        
		aboutEfaPanel.add(logoLabel, new GridBagConstraints(0, 0, 1, 7, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(0, 0, 0, 20), 0, 0));
        aboutEfaPanel.add(nameLabel, new GridBagConstraints(1, 0, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 93, 0));
        aboutEfaPanel.add(versionLabel, new GridBagConstraints(1, 1, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 230, 0));
        aboutEfaPanel.add(languageLabel, new GridBagConstraints(1, 2, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 230, 0));
        aboutEfaPanel.add(copyLabel, new GridBagConstraints(1, 3, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 65, 0));
        aboutEfaPanel.add(urlLabel0, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 1, 0));
        aboutEfaPanel.add(urlLabel, new GridBagConstraints(2, 4, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 1, 0));
        aboutEfaPanel.add(supportLabel0, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 1, 0));
        aboutEfaPanel.add(supportLabel, new GridBagConstraints(2, 5, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 1, 0));
        //infoPanel.add(emailLabel0, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 0, 0));
        //infoPanel.add(emailLabel, new GridBagConstraints(2, 6, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 0, 0));
        aboutEfaPanel.add(gpl1Label, new GridBagConstraints(1, 7, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 0, 0));
        aboutEfaPanel.add(gplLabel, new GridBagConstraints(3, 7, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 20), 0, 0));
        aboutEfaPanel.add(devNoteLabel, new GridBagConstraints(1, 8, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 20), 0, 0));
        aboutEfaPanel.add(efaBirthdayLabel, new GridBagConstraints(0, 11, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        
        versionLabel.setText(International.getString("Version") + " " + Daten.VERSION 
        		+ "   (" + Daten.VERSIONID + " / " + Daten.VERSIONRELEASEDATE + ")");
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
