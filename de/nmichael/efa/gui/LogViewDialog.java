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
import java.util.*;
import java.io.*;

public class LogViewDialog extends BaseDialog {

    BorderLayout borderLayout1 = new BorderLayout();
    JPanel filterPanel = new JPanel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabel1 = new JLabel();
    JRadioButton zeitHeute = new JRadioButton();
    JLabel jLabel2 = new JLabel();
    JCheckBox artInfo = new JCheckBox();
    JRadioButton zeit7Tage = new JRadioButton();
    JRadioButton zeitAlle = new JRadioButton();
    JCheckBox artWarn = new JCheckBox();
    JCheckBox artErr = new JCheckBox();
    JCheckBox artDbg = new JCheckBox();
    JLabel filterApplLabel = new JLabel();
    JComboBox filterAppl = new JComboBox();
    JLabel filterLabel = new JLabel();
    JTextField filter = new JTextField();
    JScrollPane logScrollPane = new JScrollPane();
    JTextArea log = new JTextArea();
    ButtonGroup buttonGroupZeit = new ButtonGroup();

    public LogViewDialog(Frame parent) {
        super(parent, International.getStringWithMnemonic("Logdatei"), International.getStringWithMnemonic("Schließen"));
    }

    public LogViewDialog(JDialog parent) {
        super(parent, International.getStringWithMnemonic("Logdatei"), International.getStringWithMnemonic("Schließen"));
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());

        filterPanel.setLayout(gridBagLayout1);
        jLabel1.setText(International.getString("Zeitraum") + ": ");
        Mnemonics.setButton(this, zeitHeute, International.getStringWithMnemonic("heute"));
        zeitHeute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLog();
            }
        });
        jLabel2.setText(International.getString("Art der Einträge") + ": ");
        Mnemonics.setButton(this, artInfo, International.getStringWithMnemonic("Informationen") + " (" + Logger.INFO.trim() + ")");
        artInfo.setSelected(true);
        artInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLog();
            }
        });
        Mnemonics.setButton(this, zeit7Tage, International.getStringWithMnemonic("die letzten 7 Tage"));
        zeit7Tage.setSelected(true);
        zeit7Tage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLog();
            }
        });
        Mnemonics.setButton(this, zeitAlle, International.getStringWithMnemonic("alle"));
        zeitAlle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLog();
            }
        });
        filterAppl.addItem(International.getString("alle"));
        filterAppl.addItem(Daten.EFA_BASE);
        filterAppl.addItem(Daten.EFA_BOATHOUSE);
        filterAppl.addItem(Daten.EFA_CLI);
        filterAppl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLog();
            }
        });
        filterApplLabel.setLabelFor(filterAppl);
        Mnemonics.setLabel(this, filterApplLabel, International.getStringWithMnemonic("Programme") + ": ");

        filterLabel.setLabelFor(filter);
        Mnemonics.setLabel(this, filterLabel, International.getStringWithMnemonic("Filter") + ": ");
        filter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                updateLog();
            }
        });
        Dialog.setPreferredSize(filter, 200, 19);
        Mnemonics.setButton(this, artWarn, International.getStringWithMnemonic("Warnungen") + " (" + Logger.WARNING.trim() + ")");
        artWarn.setSelected(true);
        artWarn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLog();
            }
        });
        Mnemonics.setButton(this, artErr, International.getStringWithMnemonic("Fehler") + " (" + Logger.ERROR.trim() + ")");
        artErr.setSelected(true);
        artErr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLog();
            }
        });
        Mnemonics.setButton(this, artDbg, International.getStringWithMnemonic("Debug") + " (" + Logger.DEBUG.trim() + ")");
        artDbg.setSelected(true);
        artDbg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateLog();
            }
        });
        logScrollPane.setPreferredSize(new Dimension(1000, 400));
        this.setTitle(International.getString("Logdatei"));
        filterPanel.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        filterPanel.add(zeitHeute, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        filterPanel.add(jLabel2, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
        filterPanel.add(artInfo, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        filterPanel.add(zeit7Tage, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        filterPanel.add(zeitAlle, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        filterPanel.add(filterApplLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        filterPanel.add(filterAppl, new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        filterPanel.add(filterLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        filterPanel.add(filter, new GridBagConstraints(1, 4, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        filterPanel.add(artWarn, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        filterPanel.add(artErr, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        filterPanel.add(artDbg, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        logScrollPane.getViewport().add(log, null);
        buttonGroupZeit.add(zeitHeute);
        buttonGroupZeit.add(zeit7Tage);
        buttonGroupZeit.add(zeitAlle);

        mainPanel.add(filterPanel, BorderLayout.NORTH);
        mainPanel.add(logScrollPane, BorderLayout.CENTER);

        updateLog();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    void updateLog() {
        String now = EfaUtil.getCurrentTimeStamp().substring(0, 10);
        String search = filter.getText().trim().toLowerCase();
        if (search.length() == 0) {
            search = null;
        }
        BufferedReader f = null;
        try {
            log.setText("");
            f = new BufferedReader(new InputStreamReader(new FileInputStream(Daten.efaLogfile)));
            String s;
            String time = "";
            String applName = "";
            String type = "";

            boolean typeInfo = artInfo.isSelected();
            boolean typeWarning = artWarn.isSelected();
            boolean typeError = artErr.isSelected();
            boolean typeDebug = artDbg.isSelected();

            int selectedAppl = filterAppl.getSelectedIndex();
            boolean applAll = selectedAppl == 0;
            boolean applBase = selectedAppl == 0 || selectedAppl == 1;
            boolean applBoathouse = selectedAppl == 0 || selectedAppl == 2;
            boolean applCLI = selectedAppl == 0 || selectedAppl == 3;

            while ((s = f.readLine()) != null) {
                StringTokenizer tok = new StringTokenizer(s, "-");
                if (tok.countTokens() < 6) {
                    type = Logger.ERROR;
                    if (time.length() > 0) {
                        s = time + " - " + type + " - " + s;
                    }
                } else {
                    try {
                        time = tok.nextToken().trim(); // time
                        applName = tok.nextToken().trim(); // applName
                        tok.nextToken(); // applPid
                        type = tok.nextToken().trim(); // type
                    } catch (Exception ee) {
                    }
                }

                // is this program selected
                boolean applok = false;
                if (applAll) {
                    applok = true;
                } else if (applName.trim().equals(Daten.APPLNAME_EFA) && applBase) {
                    applok = true;
                } else if (applName.trim().equals(Daten.APPLNAME_EFADIREKT) && applBoathouse) {
                    applok = true;
                } else if (applName.trim().equals(Daten.APPLNAME_CLI) && applCLI) {
                    applok = true;
                }

                if (applok) {
                    // ist diese Art von Nachricht ausgewählt?
                    boolean typeok = false;
                    if (type.trim().equals(Logger.INFO)) {
                        if (typeInfo) {
                            typeok = true;
                        }
                    } else if (type.trim().equals(Logger.WARNING)) {
                        if (typeWarning) {
                            typeok = true;
                        }
                    } else if (type.trim().equals(Logger.DEBUG)) {
                        if (typeDebug) {
                            typeok = true;
                        }
                    } else if (typeError) {
                        typeok = true;
                    }

                    if (typeok) {
                        // ist die Nachricht im Zeitraum?
                        boolean timeok = false;
                        if (zeitAlle.isSelected()) {
                            timeok = true;
                        } else {
                            int diff = (time.length() >= 11 ? EfaUtil.getDateDiff(time.substring(1, 11), now) : 0);
                            if (zeit7Tage.isSelected() && diff <= 7) {
                                timeok = true;
                            } else if (diff <= 1) {
                                timeok = true;
                            }
                        }

                        if (timeok) {
                            if (search == null || s.toLowerCase().indexOf(search) >= 0) {
                                log.append(s + "\n");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.append("ERROR READING LOGFILE: " + e.toString());
            e.printStackTrace();
        } finally {
            try {
                f.close();
            } catch (Exception ee) {
                f = null;
            }
        }
    }

}
