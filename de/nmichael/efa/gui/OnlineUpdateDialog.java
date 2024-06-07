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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.OnlineUpdate;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Mnemonics;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;
import javax.swing.*;

public class OnlineUpdateDialog extends BaseDialog {

    private String newVersionName;
    private String newVersionDate;
    private long downloadSize;
    private Vector<String> changes;
    JButton downloadButton = new JButton();
    JScrollPane infoScrollPane = new JScrollPane();
    JEditorPane infoEditorPane = new JEditorPane();
    JCheckBox submitUserInfo = new JCheckBox();

    public OnlineUpdateDialog(JDialog parent, String newVersionName, String newVersionDate, long downloadSize, Vector<String> changes) {
        super(parent,
                International.getStringWithMnemonic("Online-Update"),
                International.getStringWithMnemonic("Abbruch"));
        this.newVersionName = newVersionName;
        this.newVersionDate = newVersionDate;
        this.downloadSize = downloadSize;
        this.changes = changes;
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());

        JPanel versionPanel = new JPanel();
        versionPanel.setLayout(new GridBagLayout());
        JLabel currentVersionLabel = new JLabel();
        currentVersionLabel.setText(International.getString("installierte Version") + ": ");
        JLabel currentVersionValue = new JLabel();
        currentVersionValue.setText(Daten.VERSIONID + " (" + Daten.VERSIONRELEASEDATE + ")");
        JLabel newVersionLabel = new JLabel();
        newVersionLabel.setText(International.getString("verfügbare Version") + ": ");
        JLabel newVersionValue = new JLabel();
        newVersionValue.setText(newVersionName + " (" + newVersionDate + ")");
        newVersionValue.setForeground(Color.blue);
        newVersionValue.setFont(newVersionValue.getFont().deriveFont(Font.BOLD));
        JLabel downloadSizeLabel = new JLabel();
        downloadSizeLabel.setText(International.getString("Downloadgröße") + ": ");
        JLabel downloadSizeValue = new JLabel();
        downloadSizeValue.setText(downloadSize + " byte");
        versionPanel.add(currentVersionLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 0, 0, 0), 0, 0));
        versionPanel.add(currentVersionValue, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 10, 0, 0), 0, 0));
        versionPanel.add(newVersionLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        versionPanel.add(newVersionValue, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
        versionPanel.add(downloadSizeLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 20, 0), 0, 0));
        versionPanel.add(downloadSizeValue, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 20, 0), 0, 0));

        StringBuffer info = new StringBuffer();
        info.append("<html><body>");
        info.append("<p><b>" + International.getString("Änderungen gegenüber der installierten Version") + ":</b></p>");
        info.append("<ul>");
        for (int i = 0; changes != null && i < changes.size(); i++) {
            info.append("<li>" + changes.get(i) + "</li>");
        }
        info.append("</ul>");
        info.append("</body></html>");
        infoEditorPane.setEditable(false);
        infoEditorPane.setContentType("text/html");
        infoEditorPane.setText(info.toString());
        infoEditorPane.setCaretPosition(0);

        Mnemonics.setButton(this, downloadButton,
                International.getString("Version aktualisieren"),
                BaseDialog.IMAGE_DOWNLOAD);
        downloadButton.setHorizontalAlignment(SwingConstants.CENTER);
        downloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                downloadButton_actionPerformed(e);
            }
        });

        infoScrollPane.setMinimumSize(new Dimension(300, 250));
        infoScrollPane.setPreferredSize(new Dimension(600, 250));
        infoScrollPane.getViewport().add(infoEditorPane, null);
        infoEditorPane.scrollRectToVisible(new Rectangle(0, 0, 0, 0));

        JPanel userInfoPanel = new JPanel();
        userInfoPanel.setLayout(new GridBagLayout());
        submitUserInfo.setText(International.getString("Vereinsdaten für Nutzerstatistik übermitteln"));
        submitUserInfo.setSelected(true);
        JLabel detailsUserInfo = new JLabel("(" + International.getString("Details") + ")");
        detailsUserInfo.setForeground(Color.blue);
        detailsUserInfo.setFont(detailsUserInfo.getFont().deriveFont(8));
        detailsUserInfo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                showTransmittedUserInfo();
            }
            public void mouseEntered(MouseEvent e) {
                try {
                    JLabel label = (JLabel) e.getSource();
                    label.setForeground(Color.red);
                } catch (Exception eignore) {
                }
            }
            public void mouseExited(MouseEvent e) {
                try {
                    JLabel label = (JLabel) e.getSource();
                    label.setForeground(Color.blue);
                } catch (Exception eignore) {
                }
            }
        });
        detailsUserInfo.setVisible(OnlineUpdate.getTransmitUserInfo(newVersionName) != null);
        userInfoPanel.add(submitUserInfo, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));
        userInfoPanel.add(detailsUserInfo, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 10, 5, 0), 0, 0));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(infoScrollPane, BorderLayout.CENTER);
        if (Daten.project != null && Daten.project.isOpen()) {
            centerPanel.add(userInfoPanel, BorderLayout.SOUTH);
        }

        mainPanel.add(versionPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(downloadButton, BorderLayout.SOUTH);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    void showTransmittedUserInfo() {
        String s = OnlineUpdate.getTransmitUserInfo(newVersionName);
        if (s != null) {
            Dialog.infoDialog(International.getString("Folgende Daten werden übermittelt") + ":\n" +
                    s);
        }
    }

    void downloadButton_actionPerformed(ActionEvent e) {
        if (submitUserInfo.isSelected()) {
            OnlineUpdate.submitUserInfos(newVersionName);
        }
        setDialogResult(true);
        cancel();
    }



}
