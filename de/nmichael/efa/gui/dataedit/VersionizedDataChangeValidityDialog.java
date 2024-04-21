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
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.gui.BaseDialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete
public class VersionizedDataChangeValidityDialog extends BaseDialog implements IItemListener {

    private int version;
    private DataRecord rCurrent;
    private DataRecord rPrev;
    private DataRecord rNext;
    private int versionId;
    private ItemTypeDateTime validFrom;
    private ItemTypeDateTime validUntil;
    private long validFromResult = -1;
    private long invalidFromResult = -1;

    public VersionizedDataChangeValidityDialog(Frame parent, int version, DataRecord rCurrent, DataRecord rPrev, DataRecord rNext) {
        super(parent, International.getString("Gültigkeitszeitraum ändern"), International.getStringWithMnemonic("Ändern"));
        this.version = version;
        this.rCurrent = rCurrent;
        this.rPrev = rPrev;
        this.rNext = rNext;
    }

    public VersionizedDataChangeValidityDialog(JDialog parent, int version, DataRecord rCurrent, DataRecord rPrev, DataRecord rNext) {
        super(parent, International.getString("Gültigkeitszeitraum ändern"), International.getStringWithMnemonic("Ändern"));
        this.version = version;
        this.rCurrent = rCurrent;
        this.rPrev = rPrev;
        this.rNext = rNext;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        // create GUI items
        mainPanel.setLayout(new GridBagLayout());

        ItemTypeLabel label1 = new ItemTypeLabel("LABEL1", IItemType.TYPE_PUBLIC, "",
                International.getMessage("Gültigkeitszeitraum von Version {version} ändern", version+1));
        label1.setPadding(0, 0, 0, 10);
        label1.displayOnGui(this, mainPanel, 0, 0);

        validFrom = new ItemTypeDateTime("VALID_FROM",
                (rCurrent.getValidFrom() == 0 ? null : new DataTypeDate(rCurrent.getValidFrom())),
                (rCurrent.getValidFrom() == 0 ? null : new DataTypeTime(rCurrent.getValidFrom())),
                IItemType.TYPE_PUBLIC, "", International.getString("gültig ab") );
        validFrom.registerItemListener(this);
        if (rPrev != null && rPrev.getValidFrom() != 0) {
            validFrom.setMustBeAfter(new ItemTypeDateTime("PREVRECORD_VALID_FROM",
                    new DataTypeDate(rPrev.getValidFrom()), new DataTypeTime(rPrev.getValidFrom()),
                    IItemType.TYPE_INTERNAL, "", ""), false);
        }
        validFrom.setNotNull(rPrev != null);
        validFrom.displayOnGui(this, mainPanel, 0, 1);
        validFrom.requestFocus();
        validUntil = new ItemTypeDateTime("VALID_UNTIL",
                (rCurrent.getInvalidFrom() == Long.MAX_VALUE ? null : new DataTypeDate(rCurrent.getInvalidFrom() - 1)),
                (rCurrent.getInvalidFrom() == Long.MAX_VALUE ? null : new DataTypeTime(rCurrent.getInvalidFrom() - 1)),
                IItemType.TYPE_PUBLIC, "", International.getString("gültig bis") );
        validUntil.registerItemListener(this);
        validUntil.setMustBeAfter(validFrom, true);
        if (rNext != null && rNext.getInvalidFrom() != Long.MAX_VALUE) {
            validUntil.setMustBeBefore(new ItemTypeDateTime("NEXTRECORD_VALID_UNTIL",
                    new DataTypeDate(rNext.getInvalidFrom()-1), new DataTypeTime(rNext.getInvalidFrom()-1),
                    IItemType.TYPE_INTERNAL, "", ""), false);
        }
        validUntil.setNotNull(rNext != null);

        validUntil.displayOnGui(this, mainPanel, 0, 2);

        closeButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        closeButton.setIconTextGap(10);
    }

    boolean checkValidFrom() {
        validFrom.getValueFromGui();
        validUntil.getValueFromGui();
        if (!validFrom.isValidInput()) {
            if (!_inCancel) {
                Dialog.error(validFrom.getDescription() + ":\n" + validFrom.getLastInvalidError());
                validFrom.requestFocus();
            }
            return false;
        }
        return true;
    }

    boolean checkValidUntil() {
        validFrom.getValueFromGui();
        validUntil.getValueFromGui();
        if (!validUntil.isValidInput()) {
            if (!_inCancel) {
                Dialog.error(validUntil.getDescription() + ":\n" + validUntil.getLastInvalidError());
                validUntil.requestFocus();
            }
            return false;
        }
        return true;
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (event.getID() == FocusEvent.FOCUS_LOST) {
            if (itemType == validFrom) {
                checkValidFrom();
            }
            if (itemType == validUntil) {
                checkValidUntil();
            }
        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        if (!checkValidFrom() || !checkValidUntil()) {
            return;
        }
        this.validFromResult = (validFrom.isSet() && validFrom.isValidInput() ? validFrom.getTimeStamp() : 0);
        this.invalidFromResult = (validUntil.isSet() && validUntil.isValidInput() ? validUntil.getTimeStamp() + 1000: Long.MAX_VALUE);
        super.closeButton_actionPerformed(e);
    }

    public long getValidFromResult() {
        return validFromResult;
    }

    public long getInvalidFromResult() {
        return invalidFromResult;
    }

}
