/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.ItemType;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.statistics.StatisticTask;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import java.util.UUID;
import javax.swing.*;

// @i18n complete
public class StatisticsEditDialog extends UnversionizedDataEditDialog {

	public StatisticsEditDialog(Frame parent, StatisticsRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Statistik"), r, newRecord, admin);
        initialize(false);
    }

    public StatisticsEditDialog(JDialog parent, StatisticsRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Statistik"), r, newRecord, admin);
        initialize(false);
    }

    public StatisticsEditDialog(JDialog parent, StatisticsRecord r, boolean newRecord, boolean dontSaveButRun, AdminRecord admin) {
        super(parent, International.getString("Statistik"), r, newRecord, admin);
        initialize(dontSaveButRun);
    }

    public void initialize(boolean dontSaveButRun) {
        mainPanel.setMinimumSize(new Dimension(1100, 400));
        _dontSaveRecord = dontSaveButRun;
        if (dontSaveButRun) {
            _closeButtonText = International.getString("Statistik erstellen");
            getItem(StatisticsRecord.NAME).setVisible(false);
            getItem(StatisticsRecord.PUBLICLYAVAILABLE).setVisible(false);
        }
        if (admin != null) {
            setSaveAsButton();
        }
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void setSaveAsButton() {
        JButton saveAsButton = new JButton();
        saveAsButton.setIcon(BaseDialog.getIcon(ImagesAndIcons.IMAGE_BUTTON_SAVEAS));
        saveAsButton.setMargin(new Insets(2,2,2,2));
        saveAsButton.setSize(35, 20);
        saveAsButton.setToolTipText(International.getString("Speichern als ..."));
        saveAsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { saveAsNewStatistic(); }
        });
        this.addComponentToNortheastPanel(saveAsButton);
    }

    private void saveAsNewStatistic() {
        StatisticsRecord r = (StatisticsRecord)dataRecord.cloneRecord();
        ItemTypeString guiName = (ItemTypeString)getItem(StatisticsRecord.NAME);
        String currentName = (guiName != null ? guiName.getValueFromField() : r.getName());
        ItemTypeString name = new ItemTypeString("NAME", currentName,
                ItemType.TYPE_PUBLIC, "", International.getString("Name"));
        name.setNotNull(true);
        if (SimpleInputDialog.showInputDialog(this, International.getString("Speichern als ..."), name)) {
            String sName = name.getValueFromField().trim();
            r.setId(UUID.randomUUID());
            r.setName(sName);
            r.setPosition(((Statistics)r.getPersistence()).getHighestPosition() + 1);
            try {
                _dontSaveRecord = false;
                r.getPersistence().data().add(r);
                dataRecord = r;
                newRecord = false;
                if (guiName != null) {
                    guiName.parseAndShowValue(sName);
                }
                saveRecord();
            } catch(Exception e) {
                Dialog.error(e.getMessage());
            }
        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        if (_dontSaveRecord) {
            try {
                getValuesFromGui();
                if (saveRecord()) {
                    StatisticTask.createStatisticsTask(null, this,
                            new StatisticsRecord[]{(StatisticsRecord) dataRecord},
                            admin);
                }
            } catch (Exception ex) {
                Logger.logdebug(ex);
                Dialog.error(ex.toString());
            }
        } else {
            super.closeButton_actionPerformed(e);
        }
    }


}
