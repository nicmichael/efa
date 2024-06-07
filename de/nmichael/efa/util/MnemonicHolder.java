/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.util;

import javax.swing.*;

// @i18n complete

class MnemonicHolder {

    private AbstractButton b;
    private JLabel l;
    private boolean explicit;

    public MnemonicHolder(AbstractButton b, JLabel l, boolean explicit) {
        this.b = b;
        this.l = l;
        this.explicit = explicit;
    }

    public boolean clearMnemonics() {
        if (explicit) {
            return false;
        }
        if (b != null) {
            b.setMnemonic(0x0);
        }
        if (l != null) {
            l.setDisplayedMnemonic(0x0);
        }
        return true;
    }
}

