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
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.efawett.DRVSignatur;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete
public class FahrtenabzeichenEditDialog extends UnversionizedDataEditDialog {

    public FahrtenabzeichenEditDialog(Frame parent, FahrtenabzeichenRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.onlyFor("Fahrtenabzeichen","de"), r, newRecord, admin);
        initialize(r);
    }

    public FahrtenabzeichenEditDialog(JDialog parent, FahrtenabzeichenRecord r, boolean newRecord, AdminRecord admin) {
        super(parent, International.onlyFor("Fahrtenabzeichen","de"), r, newRecord, admin);
        initialize(r);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    private void initialize(FahrtenabzeichenRecord r) {
        DRVSignatur sig = (r != null ? r.getDRVSignatur() : null);
        if (sig != null) {
            sig.checkSignature();
            if (sig.getSignatureState() == DRVSignatur.SIG_UNKNOWN_KEY) {
                if (((Fahrtenabzeichen)r.getPersistence()).downloadKey(sig.getKeyName())) {
                    r.updateGuiItems();
                }
            }
        }
    }


}
