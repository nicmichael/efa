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

import de.nmichael.efa.data.types.DataTypePasswordCrypted;
import javax.swing.*;

public class ItemTypePassword extends ItemTypeString {

    private boolean encrypted = false;

    public ItemTypePassword(String name, String value, int type,
            String category, String description) {
        super(name, value, type, category, description);
    }
    
    public ItemTypePassword(String name, String value, boolean encrypted, int type,
            String category, String description) {
        super(name, value, type, category, description);
        this.encrypted = encrypted;
    }

    public IItemType copyOf() {
        ItemTypePassword copy = new ItemTypePassword(name, value, encrypted, type, category, description);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setIcon((label == null ? null : label.getIcon()));
        return copy;
    }

    protected JComponent initializeField() {
        JPasswordField f = new JPasswordField();
        return f;
    }

    public void parseValue(String value) {
        if (encrypted) {
            value = DataTypePasswordCrypted.parsePassword(value).getPassword();
        }
        super.parseValue(value);
    }

    // called from EfaConfig for external storage;
    // we need a special solution here because EfaConfig ist not DataType-aware
    public String getCryptedPassword() {
        if (value != null && value.length() > 0) {
            DataTypePasswordCrypted pwd = new DataTypePasswordCrypted(value);
            return pwd.toString();
        }
        return value;
    }

}
