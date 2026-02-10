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
import de.nmichael.efa.core.PluginInfo;
import de.nmichael.efa.core.Plugins;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeHtmlList;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class PluginDialog extends BaseDialog implements IItemListener {

    private ItemTypeHtmlList list;
    private Plugins plugins;
    private String[] keys;

    public PluginDialog(Frame parent) {
        super(parent, International.getStringWithMnemonic("Plugins"),
                International.getStringWithMnemonic("Schließen"));
        plugins = Plugins.getPluginInfoFromLocalFile();
    }

    public PluginDialog(JDialog parent) {
        super(parent, International.getStringWithMnemonic("Plugins"),
                International.getStringWithMnemonic("Schließen"));
        plugins = Plugins.getPluginInfoFromLocalFile();
    }

    protected void iniDialog() throws Exception {
        // create GUI items
        mainPanel.setLayout(new GridBagLayout());

        list = new ItemTypeHtmlList("LIST", null, null, null, IItemType.TYPE_PUBLIC, null,
                International.getString("Plugins"));
        String[] actions = {
            International.getString("Installieren")
        };
        list.setPopupActions(actions);
        list.registerItemListener(this);
        list.setFieldGrid(1, 5, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        list.setPadding(10, 10, 0, 10);
        list.displayOnGui(_parent, mainPanel, 0, 0,1.0,1.0);

        JButton installButton = new JButton();
        Mnemonics.setButton(this, installButton, International.getString("Installieren"),
                BaseDialog.IMAGE_DOWNLOAD);
        installButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                installButton_actionPerformed(e);
            }
        });
        mainPanel.add(installButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 10), 0, 0));

        updateGui();
    }

    public void updateGui() {
        Hashtable<String,String> items = (plugins != null ? plugins.getPluginHtmlInfos() : null);
        if (items == null) {
            items = new Hashtable<String,String>();
            Dialog.error(International.getString("Programminformationen konnten nicht ermittelt werden"));
        }
        keys = items.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        list.setValues(keys, items);
    }

    public void itemListenerAction(IItemType item, AWTEvent event) {
        if (item != null && event != null && item == list) {
            if (event instanceof ActionEvent) {
                ActionEvent e = (ActionEvent)event;
                String cmd = e.getActionCommand();
                if (cmd != null && cmd.equals(EfaMouseListener.EVENT_MOUSECLICKED_2x)) {
                    installButton_actionPerformed(e);
                }
                if (cmd != null && cmd.startsWith(EfaMouseListener.EVENT_POPUP_CLICKED)) {
                    int id = EfaUtil.string2date(cmd, -1, -1, -1).tag;
                    switch(id) {
                        case 0:
                            installButton_actionPerformed(e);
                            break;
                    }

                }
            }
        }
    }

    void installButton_actionPerformed(ActionEvent e) {
        String name = list.getValueFromField();
        if (name == null) {
            return;
        }
        PluginInfo p = plugins.getPluginInfo(name);
        if (p == null) {
            return;
        }
        if (p.isInsalled()) {
            Dialog.error(International.getMessage("Plugin {plugin} ist bereits installiert.", name));
            return;
        }
        switch(Dialog.auswahlDialog(International.getString("Plugin installieren"),
                International.getMessage("Plugin {plugin} ({size} byte) jetzt installieren?",
                                         name, p.getTotalSize()),
                International.getString("Automatische Installation"),
                International.getString("Manuelle Installation"))) {
            case 0:
                break;
            case 1:
                Dialog.infoDialog(International.getMessage("Das Plugin kann unter {url} heruntergeladen werden.",
                        Daten.pluginWebpage));
                return;
            default:
                return;
        }
        if (!Dialog.okAbbrDialog(International.getString("Internetverbindung erforderlich"),
                International.getString("Bitte stelle eine Verbindung zum Internet her."))) {
            return;
        }

        DownloadMultipleFilesDialog dlg = new DownloadMultipleFilesDialog(this,
                International.getString("Plugin") + " \"" + p.getName() + "\"",
                p.getDownloadFileNames(), p.getLocalFileNames(), p.getFileSizes(), false);
        dlg.showDialog();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

}
