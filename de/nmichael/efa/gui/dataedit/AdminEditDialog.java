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
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.ex.InvalidValueException;
import de.nmichael.efa.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete
public class AdminEditDialog extends UnversionizedDataEditDialog {

    public AdminEditDialog(Frame parent, AdminRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Administrator"), r, newRecord, admin);
    }

    public AdminEditDialog(JDialog parent, AdminRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.getString("Administrator"), r, newRecord, admin);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected boolean saveRecord() throws InvalidValueException {
        String password = null;
        for (IItemType item : getItems()) {
            if (item.isVisible() && item.getName().startsWith(AdminRecord.PASSWORD)) {
                if (password == null) {
                    password = item.toString(); // first password
                } else {
                    if (!password.equals(item.toString())) {
                        throw new InvalidValueException(item, International.getMessage("Paßwort in Feld '{field}' nicht identisch.", item.getDescription()));
                    }
                }
            }
        }
        return super.saveRecord();
    }
}
