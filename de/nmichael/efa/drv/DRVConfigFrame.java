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

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import de.nmichael.efa.*;

// @i18n complete (needs no internationalization -- only relevant for Germany)
public class DRVConfigFrame extends JDialog implements ActionListener {

    JDialog parent;
    JPanel jPanel1 = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    JButton saveButton = new JButton();
    JTabbedPane jTabbedPane1 = new JTabbedPane();
    JPanel baseConfigPanel = new JPanel();
    JPanel meldegeldPanel = new JPanel();
    JPanel efaWettPanel = new JPanel();
    JPanel extProgrammePanel = new JPanel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabel1 = new JLabel();
    JCheckBox darfFAbearbeitenCheckBox = new JCheckBox();
    JCheckBox darfWSbearbeitenCheckBox = new JCheckBox();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JLabel jLabel2 = new JLabel();
    JLabel jLabel3 = new JLabel();
    JLabel jLabel4 = new JLabel();
    JTextField meld_erw = new JTextField();
    JTextField meld_jug = new JTextField();
    JLabel jLabel5 = new JLabel();
    JLabel jLabel6 = new JLabel();
    JLabel jLabel7 = new JLabel();
    JLabel jLabel8 = new JLabel();
    JLabel jLabel9 = new JLabel();
    JLabel jLabel10 = new JLabel();
    JLabel jLabel11 = new JLabel();
    JLabel jLabel12 = new JLabel();
    JTextField nadel_erw_silber = new JTextField();
    JTextField nadel_erw_gold = new JTextField();
    JTextField nadel_jug_silber = new JTextField();
    JTextField nadel_jug_gold = new JTextField();
    JTextField stoff_erw = new JTextField();
    JTextField stoff_jug = new JTextField();
    JLabel jLabel13 = new JLabel();
    JLabel jLabel14 = new JLabel();
    JLabel jLabel15 = new JLabel();
    JLabel jLabel16 = new JLabel();
    JLabel jLabel17 = new JLabel();
    JLabel jLabel18 = new JLabel();
    JLabel jLabel19 = new JLabel();
    JLabel jLabel20 = new JLabel();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JLabel jLabel21 = new JLabel();
    JLabel jLabel22 = new JLabel();
    JLabel jLabel23 = new JLabel();
    JLabel jLabel24 = new JLabel();
    JLabel jLabel_verband = new JLabel();
    JTextField efw_verband = new JTextField();
    JTextField efw_script = new JTextField();
    JTextField efw_username = new JTextField();
    JTextField efw_password = new JTextField();
    JCheckBox efw_testmode = new JCheckBox();
    JCheckBox efw_readonly = new JCheckBox();
    
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    JLabel jLabel25 = new JLabel();
    JLabel jLabel26 = new JLabel();
    JLabel jLabel27 = new JLabel();
    JTextField prog_openssl = new JTextField();
    JTextField prog_acrobat = new JTextField();
    JLabel jLabel28 = new JLabel();
    JLabel jLabel30 = new JLabel();
    JTextField userDirectory = new JTextField();

    public DRVConfigFrame(JDialog parent) {
        super(parent);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        Dialog.frameOpened(this);
        try {
            jbInit();
            frameIni();
        } catch (Exception e) {
            e.printStackTrace();
        }
        EfaUtil.pack(this);
        this.parent = parent;
        // this.requestFocus();
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
        } catch (NoSuchMethodException e) {
            System.err.println("Error setting up ActionHandler");
        }
        jPanel1.setLayout(borderLayout1);
        saveButton.setMnemonic('S');
        saveButton.setText("Speichern");
        saveButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveButton_actionPerformed(e);
            }
        });
        baseConfigPanel.setLayout(gridBagLayout1);
        jLabel1.setText("Benutzer darf die folgenden Wettbewerbe bearbeiten:");
        darfFAbearbeitenCheckBox.setText("DRV-Fahrtenabzeichen");
        darfWSbearbeitenCheckBox.setText("DRV-Wanderruderstatistik");
        meldegeldPanel.setLayout(gridBagLayout2);
        jLabel2.setText("Meldegeld:");
        jLabel3.setText("Erwachsene: ");
        jLabel4.setText("Jugendliche: ");
        jLabel5.setText("Preise für Anstecknadeln:");
        jLabel6.setText("Erwachsene (silber): ");
        jLabel7.setText("Erwachsene (gold): ");
        jLabel8.setText("Jugendliche (silber): ");
        jLabel9.setText("Jugendliche (gold): ");
        jLabel10.setText("Preise für Stoffabzeichen:");
        jLabel11.setText("Erwachsene (gold): ");
        jLabel12.setText("Jugendliche: ");
        meld_erw.setNextFocusableComponent(meld_jug);
        meld_erw.setPreferredSize(new Dimension(50, 17));
        meld_jug.setNextFocusableComponent(nadel_erw_silber);
        meld_jug.setPreferredSize(new Dimension(50, 17));
        nadel_erw_silber.setNextFocusableComponent(nadel_erw_gold);
        nadel_erw_silber.setPreferredSize(new Dimension(50, 17));
        nadel_erw_gold.setNextFocusableComponent(nadel_jug_silber);
        nadel_erw_gold.setPreferredSize(new Dimension(50, 17));
        nadel_jug_silber.setNextFocusableComponent(nadel_jug_gold);
        nadel_jug_silber.setPreferredSize(new Dimension(50, 17));
        nadel_jug_gold.setNextFocusableComponent(stoff_erw);
        nadel_jug_gold.setPreferredSize(new Dimension(50, 17));
        stoff_erw.setNextFocusableComponent(stoff_jug);
        stoff_erw.setPreferredSize(new Dimension(50, 17));
        stoff_jug.setNextFocusableComponent(saveButton);
        stoff_jug.setPreferredSize(new Dimension(50, 17));
        jLabel13.setText(" Cent");
        jLabel14.setText(" Cent");
        jLabel15.setText(" Cent");
        jLabel16.setText(" Cent");
        jLabel17.setText(" Cent");
        jLabel18.setText(" Cent");
        jLabel19.setText(" Cent");
        jLabel20.setText(" Cent");
        efaWettPanel.setLayout(gridBagLayout3);
        jLabel21.setText("Zugangsdaten für efaWett:");
        jLabel22.setText("Script: ");
        jLabel23.setText("Username: ");
        jLabel24.setText("Paßwort: ");
        jLabel_verband.setText("Verband: ");
        efw_testmode.setText("Test Modus");
        efw_readonly.setText("Read-Only Modus");
        efw_verband.setNextFocusableComponent(efw_username);
        efw_verband.setPreferredSize(new Dimension(100, 17));
        efw_username.setNextFocusableComponent(efw_password);
        efw_username.setPreferredSize(new Dimension(100, 17));
        efw_script.setNextFocusableComponent(efw_username);
        efw_script.setPreferredSize(new Dimension(400, 17));
        efw_password.setNextFocusableComponent(saveButton);
        efw_password.setPreferredSize(new Dimension(100, 17));
        extProgrammePanel.setLayout(gridBagLayout4);
        jLabel25.setText("externe Programme: ");
        jLabel26.setText("OpenSSL: ");
        jLabel27.setText("Acrobat Reader: ");
        prog_openssl.setNextFocusableComponent(prog_acrobat);
        prog_openssl.setPreferredSize(new Dimension(300, 17));
        prog_acrobat.setNextFocusableComponent(saveButton);
        prog_acrobat.setPreferredSize(new Dimension(300, 17));
        this.setTitle("Konfiguration");
        jLabel28.setText("Allgemeine Konfiguration");
        jLabel30.setText("Ort für Nutzerdaten: ");
        userDirectory.setText("");
        this.getContentPane().add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(saveButton, BorderLayout.SOUTH);
        jPanel1.add(jTabbedPane1, BorderLayout.CENTER);
        jTabbedPane1.add(baseConfigPanel, "Basiskonfiguration");
        jTabbedPane1.add(meldegeldPanel, "Meldegeld");
        jTabbedPane1.add(efaWettPanel, "efaWett");
        jTabbedPane1.add(extProgrammePanel, "Programme");
        baseConfigPanel.add(jLabel1, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 0, 0, 0), 0, 0));
        baseConfigPanel.add(darfFAbearbeitenCheckBox, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        baseConfigPanel.add(darfWSbearbeitenCheckBox, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        baseConfigPanel.add(jLabel28, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 10, 0), 0, 0));
        baseConfigPanel.add(jLabel30, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        baseConfigPanel.add(userDirectory, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel2, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel3, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel4, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(meld_erw, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(meld_jug, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel5, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel6, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel7, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel8, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel9, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel10, new GridBagConstraints(0, 8, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel11, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel12, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(nadel_erw_silber, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(nadel_erw_gold, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(nadel_jug_silber, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(nadel_jug_gold, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(stoff_erw, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(stoff_jug, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel13, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel14, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel15, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel16, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel17, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel18, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel19, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        meldegeldPanel.add(jLabel20, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(jLabel21, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(jLabel_verband, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(jLabel22, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(jLabel23, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(jLabel24, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(efw_verband, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(efw_script, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(efw_username, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(efw_password, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(efw_testmode, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        efaWettPanel.add(efw_readonly, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        extProgrammePanel.add(jLabel25, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        extProgrammePanel.add(jLabel26, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        extProgrammePanel.add(jLabel27, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        extProgrammePanel.add(prog_openssl, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        extProgrammePanel.add(prog_acrobat, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        meld_erw.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(FocusEvent e) {
                validateIntergerValue(e);
            }
        });
        meld_jug.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(FocusEvent e) {
                validateIntergerValue(e);
            }
        });
        nadel_erw_silber.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(FocusEvent e) {
                validateIntergerValue(e);
            }
        });
        nadel_erw_gold.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(FocusEvent e) {
                validateIntergerValue(e);
            }
        });
        nadel_jug_silber.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(FocusEvent e) {
                validateIntergerValue(e);
            }
        });
        nadel_jug_gold.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(FocusEvent e) {
                validateIntergerValue(e);
            }
        });
        stoff_erw.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(FocusEvent e) {
                validateIntergerValue(e);
            }
        });
        stoff_jug.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(FocusEvent e) {
                validateIntergerValue(e);
            }
        });

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

    void validateIntergerValue(FocusEvent e) {
        try {
            JTextField field = (JTextField) e.getComponent();
            String s = field.getText().trim();
            int i = EfaUtil.string2date(s, 0, 0, 0).tag;
            if (i > 0) {
                s = Integer.toString(i);
                
            } else {
                s = "0";
                
            }
            field.setText(s);
        } catch (Exception ee) {
            EfaUtil.foo();
        }
    }

    void frameIni() {
        this.userDirectory.setText(Daten.efaBaseConfig.efaUserDirectory);
        this.darfFAbearbeitenCheckBox.setSelected(Main.drvConfig.darfFAbearbeiten);
        this.darfWSbearbeitenCheckBox.setSelected(Main.drvConfig.darfWSbearbeiten);

        this.meld_erw.setText(Integer.toString(Main.drvConfig.eur_meld_erw));
        this.meld_jug.setText(Integer.toString(Main.drvConfig.eur_meld_jug));
        this.nadel_erw_silber.setText(Integer.toString(Main.drvConfig.eur_nadel_erw_silber));
        this.nadel_erw_gold.setText(Integer.toString(Main.drvConfig.eur_nadel_erw_gold));
        this.nadel_jug_silber.setText(Integer.toString(Main.drvConfig.eur_nadel_jug_silber));
        this.nadel_jug_gold.setText(Integer.toString(Main.drvConfig.eur_nadel_jug_gold));
        this.stoff_erw.setText(Integer.toString(Main.drvConfig.eur_stoff_erw));
        this.stoff_jug.setText(Integer.toString(Main.drvConfig.eur_stoff_jug));

        this.efw_verband.setText(Main.drvConfig.verband);
        this.efw_script.setText(Main.drvConfig.efw_script);
        this.efw_username.setText(Main.drvConfig.efw_user);
        this.efw_password.setText(Main.drvConfig.efw_password);
        this.efw_testmode.setSelected(Main.drvConfig.testmode);
        this.efw_readonly.setSelected(Main.drvConfig.readOnlyMode);

        this.prog_openssl.setText(Main.drvConfig.openssl);
        this.prog_acrobat.setText(Main.drvConfig.acrobat);
    }

    void saveButton_actionPerformed(ActionEvent e) {
        String newUserHome = this.userDirectory.getText().trim();
        if (newUserHome.length() > 0 && !newUserHome.equals(Daten.efaBaseConfig.efaUserDirectory)) {
            if (Daten.efaBaseConfig.efaCanWrite(newUserHome, true)) {
                Daten.efaBaseConfig.efaUserDirectory = newUserHome;
                if (Daten.efaBaseConfig.writeFile()) {
                    Dialog.infoDialog("Die geänderten Einstellungen werden nach einem Neustart von efa aktiv!");
                } else {
                    Dialog.error("efa kann die Konfigurationsdatei\n'" + Daten.efaBaseConfig.getFileName()+
                            "'\nnicht schreiben. Änderungen ignoriert.");
                }
            } else {
                Dialog.error("efa kann im Verzeichnis\n'" + newUserHome + "'\n nicht schreiben. Änderung ignoriert.");
            }
        }

        Main.drvConfig.darfFAbearbeiten = this.darfFAbearbeitenCheckBox.isSelected();
        Main.drvConfig.darfWSbearbeiten = this.darfWSbearbeitenCheckBox.isSelected();

        Main.drvConfig.eur_meld_erw = EfaUtil.string2int(this.meld_erw.getText().trim(), 0);
        Main.drvConfig.eur_meld_jug = EfaUtil.string2int(this.meld_jug.getText().trim(), 0);
        Main.drvConfig.eur_nadel_erw_silber = EfaUtil.string2int(this.nadel_erw_silber.getText().trim(), 0);
        Main.drvConfig.eur_nadel_erw_gold = EfaUtil.string2int(this.nadel_erw_gold.getText().trim(), 0);
        Main.drvConfig.eur_nadel_jug_silber = EfaUtil.string2int(this.nadel_jug_silber.getText().trim(), 0);
        Main.drvConfig.eur_nadel_jug_gold = EfaUtil.string2int(this.nadel_jug_gold.getText().trim(), 0);
        Main.drvConfig.eur_stoff_erw = EfaUtil.string2int(this.stoff_erw.getText().trim(), 0);
        Main.drvConfig.eur_stoff_jug = EfaUtil.string2int(this.stoff_jug.getText().trim(), 0);

        Main.drvConfig.verband = this.efw_verband.getText().trim();
        Main.drvConfig.efw_script = this.efw_script.getText().trim();
        Main.drvConfig.efw_user = this.efw_username.getText().trim();
        Main.drvConfig.efw_password = this.efw_password.getText().trim();
        Main.drvConfig.testmode = this.efw_testmode.isSelected();
        Main.drvConfig.readOnlyMode = this.efw_readonly.isSelected();

        Main.drvConfig.openssl = this.prog_openssl.getText().trim();
        Main.drvConfig.acrobat = this.prog_acrobat.getText().trim();

        if (!Main.drvConfig.writeFile()) {
            Dialog.error("Das Speichern der Konfiguration ist fehlgeschlagen!");
            return;
        }
        cancel();
    }
}
