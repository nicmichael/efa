/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import de.nmichael.efa.gui.BaseFrame;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;

// @i18n complete

public class ItemTypeFile extends ItemTypeString {
    
    public static final int MODE_OPEN = 1;
    public static final int MODE_SAVE = 2;
    public static final int TYPE_FILE = 1;
    public static final int TYPE_DIR  = 2;

    private JButton button;
    private String fileItem;
    private String fileTypes;
    private String fileExtensions;
    private int fileOpenSave;
    private int fileOrDir;
    private String fileDialogBaseDirectory;

    public ItemTypeFile(String name, String value,
            String fileItem, String fileTypes, String fileExtensions, int fileOpenSave, int fileOrDir,
            int type, String category, String description) {
        super(name,value,type,category,description);
        this.fileItem = fileItem;
        this.fileTypes = fileTypes;
        this.fileExtensions = fileExtensions;
        this.fileOpenSave = fileOpenSave;
        this.fileOrDir = fileOrDir;
    }

    public IItemType copyOf() {
        ItemTypeFile newItem = new ItemTypeFile(name, value, fileItem, fileTypes, fileExtensions, fileOpenSave, fileOrDir, type, category, description);
        newItem.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        newItem.setIcon((label == null ? null : label.getIcon()));
        return newItem;
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        super.displayOnGui(dlg, panel, x, y);

        button = new JButton();
        if (fileOpenSave == MODE_OPEN) {
            button.setIcon(BaseFrame.getIcon("menu_open.gif"));
        } else {
            button.setIcon(BaseFrame.getIcon("menu_save.gif"));
        }
        button.setMargin(new Insets(0,0,0,0));
        Dialog.setPreferredSize(button, 19, 19);
        button.setVisible(isVisible);
        button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { buttonHit(e); }
        });
        panel.add(button, new GridBagConstraints(x+1+fieldGridWidth, y, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 0, padYafter, padXafter), 0, 0));
        return 1;
    }

    private void buttonHit(ActionEvent e) {
        String startDirectory = null;
        String selectedFile = null;
        String currentValue = getValueFromField();

        if (currentValue.length() > 0) {
            startDirectory = EfaUtil.getPathOfFile(currentValue);
            selectedFile = EfaUtil.getNameOfFile(currentValue);
        } else {
            startDirectory = fileDialogBaseDirectory;
        }

        String file = Dialog.dateiDialog(dlg,
                International.getMessage("{item} auswählen", fileItem),
                fileTypes, fileExtensions, startDirectory, selectedFile, null, 
                fileOpenSave == MODE_SAVE, fileOrDir == TYPE_DIR);
        if (file != null) {
            value = file;
            showValue();
            actionEvent(e);
        }
        
    }

    protected void field_focusLost(FocusEvent e) {
        getValueFromGui();
        String filename = toString().trim();
        if (fileOpenSave == MODE_OPEN && filename.length() > 0) {
            if (fileOrDir == TYPE_FILE && !(new File(filename)).isFile()) {
                Dialog.error(International.getMessage("{filedescription} '{filename}' existiert nicht",
                        International.getString("Datei"),filename)+".");
            }
            if (fileOrDir == TYPE_DIR && !(new File(filename)).isDirectory()) {
                Dialog.error(International.getMessage("{directorydescription} '{directoryname}' existiert nicht",
                        International.getString("Verzeichnis"),filename)+".");
            }
        }
        value = filename;
        showValue();
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (button != null) {
            button.setVisible(isVisible);
        }
    }

    public void setFileDialogBaseDirectory(String dir) {
        fileDialogBaseDirectory = dir;
    }

}
