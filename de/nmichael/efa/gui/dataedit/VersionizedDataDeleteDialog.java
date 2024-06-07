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

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.gui.BaseDialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete
public class VersionizedDataDeleteDialog extends BaseDialog implements IItemListener {

    private String name;
    private ItemTypeDateTime deleteAt;
    private ItemTypeBoolean deleteAll;
    private long deleteAtResult = Long.MAX_VALUE;

    public VersionizedDataDeleteDialog(Frame parent, String name) {
        super(parent, International.getString("Daten löschen"), International.getStringWithMnemonic("Löschen"));
        this.name = name;
    }

    public VersionizedDataDeleteDialog(JDialog parent, String name) {
        super(parent, International.getString("Daten löschen"), International.getStringWithMnemonic("Löschen"));
        this.name = name;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        // create GUI items
        mainPanel.setLayout(new GridBagLayout());

        DataTypeDate deleteDate = new DataTypeDate(System.currentTimeMillis());
        deleteDate.addDays(1); // suggest to delete from tomorrow on
        deleteAt = new ItemTypeDateTime("DELETE_AT",
                deleteDate, new DataTypeTime(0, 0, 0),
                IItemType.TYPE_PUBLIC, "", International.getString("Datensätze löschen ab") );
        deleteAt.displayOnGui(this, mainPanel, 0, 0);
        deleteAt.requestFocus();

        deleteAll = new ItemTypeBoolean("DELETE_ALL",
                false,
                IItemType.TYPE_PUBLIC, "", International.getString("Datensätze komplett löschen") );
        deleteAll.registerItemListener(this);
        deleteAll.displayOnGui(this, mainPanel, 0, 1);
        closeButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        closeButton.setIconTextGap(10);
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType == deleteAll && event.getID() == ActionEvent.ACTION_PERFORMED) {
            deleteAll.getValueFromGui();
            deleteAt.setEnabled(!deleteAll.getValue());
        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        deleteAt.getValueFromGui();
        deleteAll.getValueFromGui();
        if (deleteAll.getValue()) {
            if (Dialog.yesNoCancelDialog(International.getString("Wirklich löschen?"),
                    International.getString("Wenn der Datensatz vollständig gelöscht wird, können auch " +
                                            "frühere Fahrtenbücher darauf nicht mehr zugreifen und " +
                                            "Eigenschaften des Datensatzes für Statistikzwecke nicht mehr " +
                                            "verwendet werden. Datensätze sollten daher nur in absoluten Ausnahmefällen " +
                                            "komplett gelöscht werden, wenn diese Daten (auch aus der Vergangenheit) " +
                                            "wirklich nie wieder benötigt werden. Normalerweise (z.B. beim Austritt eines "+
                                            "Mitglieds oder Verkauf eines Bootes) sollten Datensätze lediglich ab einem " +
                                            "bestimmten Datum gelöscht werden, so daß sie für den Zeitraum davor noch zur " +
                                            "Verfügung stehen") + "\n\n" +
                    International.getMessage("Möchtest Du den Datensatz '{record}' wirklich vollständig löschen?", name)) != Dialog.YES) {
                return;
            }
        }
        if (deleteAll.getValue()) {
            deleteAtResult = -1;
        } else {
            if (deleteAt.isSet()) {
                deleteAtResult = deleteAt.getTimeStamp();
            }
        }
        super.closeButton_actionPerformed(e);
    }

    public long getDeleteAtResult() {
        return deleteAtResult;
    }

}
