/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.gui.dataedit;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeDouble;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.ex.InvalidValueException;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete
public class ClubworkEditDialog extends UnversionizedDataEditDialog implements IItemListener {

    public ClubworkEditDialog(Frame parent, ClubworkRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Vereinsarbeit"), r, newRecord, admin);
        initListener(newRecord);
    }

    public ClubworkEditDialog(JDialog parent, ClubworkRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Vereinsarbeit"), r, newRecord, admin);
        initListener(newRecord);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected boolean saveRecord() throws InvalidValueException {
        if (newRecord) {
            ((ClubworkRecord) dataRecord).setFlag(ClubworkRecord.Flags.Normal);
        }
        return super.saveRecord();
    }

    private void initListener(boolean newRecord) {
        IItemType item;
        item = getItem(ClubworkRecord.FIRSTNAME);
        if (item != null) {
            item.registerItemListener(this);
        }
        item = getItem(ClubworkRecord.LASTNAME);
        if (item != null) {
            item.registerItemListener(this);
        }
        item = getItem(ClubworkRecord.WORKDATE);
        if (item != null) {
            if (newRecord) {
                item.parseAndShowValue(EfaUtil.getCurrentTimeStampDD_MM_YYYY());
            }
            item.registerItemListener(this);
        }
        item = getItem(ClubworkRecord.HOURS);
        if (item != null) {
            item.registerItemListener(this);
        }
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType.getName().equals(ClubworkRecord.FIRSTNAME)
                || itemType.getName().equals(ClubworkRecord.LASTNAME)) {
            if (newRecord && Daten.efaConfig.getValueAutogenAlias()
                    && event instanceof FocusEvent && event.getID() == FocusEvent.FOCUS_LOST) {
                ItemTypeString firstName = (ItemTypeString) getItem(ClubworkRecord.FIRSTNAME);
                ItemTypeString lastName = (ItemTypeString) getItem(ClubworkRecord.LASTNAME);
                ItemTypeString inputShortcut = (ItemTypeString) getItem(ClubworkRecord.INPUTSHORTCUT);
                if (firstName != null && lastName != null && inputShortcut != null) {
                    String sf = firstName.getValueFromField();
                    String sl = lastName.getValueFromField();
                    inputShortcut.parseAndShowValue(EfaUtil.getInputShortcut(sf, sl));
                }
            }
        }
        if (itemType.getName().equals(ClubworkRecord.WORKDATE) &&
                event instanceof FocusEvent && ((FocusEvent)event).getID() == FocusEvent.FOCUS_GAINED) {
            ItemTypeDate item = (ItemTypeDate)getItem(ClubworkRecord.WORKDATE);
            if (item != null) {
                item.setSelection(0, Integer.MAX_VALUE);
            }
        }
        if (itemType.getName().equals(ClubworkRecord.HOURS) &&
                event instanceof FocusEvent && ((FocusEvent)event).getID() == FocusEvent.FOCUS_GAINED) {
            ItemTypeDouble item = (ItemTypeDouble)getItem(ClubworkRecord.HOURS);
            if (item != null) {
                item.setSelection(0, Integer.MAX_VALUE);
            }
        }
    }

}
