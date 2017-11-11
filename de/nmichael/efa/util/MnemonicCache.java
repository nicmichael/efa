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

import java.util.Hashtable;
import java.awt.*;
import javax.swing.*;

// @i18n complete

class MnemonicCache {

    private Hashtable windows = new Hashtable();

    public void put(Window w, char c, AbstractButton b, JLabel l, boolean explicit) {
        Hashtable wc = (Hashtable) windows.get(w);
        if (wc == null) {
            wc = new Hashtable();
        }
        wc.put(Character.valueOf(c), new MnemonicHolder(b, l, explicit));
        windows.put(w, wc);
    }

    public MnemonicHolder get(Window w, char c) {
        Hashtable wc = (Hashtable) windows.get(w);
        if (wc == null) {
            return null;
        }
        return (MnemonicHolder) wc.get(Character.valueOf(c));
    }

    public void clear(Window w) {
        windows.remove(w);
    }
}