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

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.BaseFrame;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedLabel;
import de.nmichael.efa.util.Base64;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;

public class ItemTypeHashtable<E> extends ItemType {

    public static int TYPE_STRING = 0;
    public static int NUMBER_OF_TYPES = 1;

    private static final String DUMMY = "%%%DUMMY%%%";
    private static final String DELIM_KEYVALUE = "-->";
    private static final String DELIM_ELEMENTS = "@@@";
    private Hashtable<String,E> hash;
    private E e;

    private JLabel titlelabel;
    private JButton addButton;
    private JTextField[] textfield;
    private Hashtable<JButton,String> delButtons;
    private boolean allowedAdd = true;
    private boolean allowedDelete = true;

    public ItemTypeHashtable(String name, E value, boolean fieldsEditable,
            int type, String category, String description) {
        this.name = name;
        this.e = value;
        setEditable(fieldsEditable);
        this.type = type;
        this.category = category;
        this.description = description;
        this.padYbefore = 20;
        this.padYafter = 20;
        iniHash();
    }

    public IItemType copyOf() {
        ItemTypeHashtable item = new ItemTypeHashtable(name, e, isEditable, type, category, description);
        String[] myKeys = this.getKeysArray();
        for (int i=0; i<myKeys.length; i++) {
            E e = get(myKeys[i]);
            item.put(myKeys[i],e);
        }
        return item;
    }

    public void setAllowed(boolean allowedAdd, boolean allowedDelete) {
        this.allowedAdd = allowedAdd;
        this.allowedDelete = allowedDelete;
    }

    private void iniHash() {
        hash = new Hashtable<String,E>();
        hash.put(DUMMY, e);
    }

    public void put(String s, E value) {
        hash.put(s, value);
    }

    public void remove(String s) {
        hash.remove(s);
    }

    public E get(String s) {
        return hash.get(s);
    }

    public int size() {
        return hash.size() - 1; // without dummy element
    }

    public String[] getKeysArray() {
        String[] keys = new String[size()];
        Object[] a = hash.keySet().toArray();
        Arrays.sort(a);
        int j=0;
        for (int i=0; i<a.length; i++) {
            if (!((String)a[i]).equals(DUMMY)) {
                keys[j++] = (String)a[i];
            }
        }
        return keys;
    }

    private void addToHash(Hashtable<String,E> hash, String key, String val) {
        E e = hash.get(DUMMY);
        Class c = e.getClass();
        Object v = null;
        boolean matchingTypeFound = false;
        for (int i = 0; i < NUMBER_OF_TYPES; i++) {
            switch (i) {
                case 0: // TYPE_STRING
                    v = val;
                    break;
            }
            if (v != null && c.isInstance(v)) {
                hash.put(key, (E) v);
                matchingTypeFound = true;
                break;
            }
        }
        if (!matchingTypeFound) {
            // should never happen (program error); no need to translate
            Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
                    "ConfigTypesHashtable: unsupported value type for key " + key + ": " + c.getCanonicalName());
        }
    }

    public void parseValue(String value) {
        if (value != null) {
            value = value.trim();
        }
        iniHash();
        try {
            StringTokenizer tok = new StringTokenizer(value, DELIM_ELEMENTS);
            while (tok.hasMoreTokens()) {
                String t = tok.nextToken();
                int pos = t.indexOf(DELIM_KEYVALUE);
                String key = t.substring(0, pos);
                key = new String(Base64.decode(key), Daten.ENCODING_UTF);
                String val = t.substring(pos + DELIM_KEYVALUE.length());
                val = new String(Base64.decode(val), Daten.ENCODING_UTF);
                addToHash(hash, key,val);
            }
        } catch (Exception e) {
            if (dlg == null) {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
                        "Invalid value for parameter " + name + ": " + value);
            }

        }
    }

    public String toString() {
        String s = "";

        String[] keys = new String[hash.size()];
        keys = hash.keySet().toArray(keys);
        for (int i=0; i<keys.length; i++) {
            E value = hash.get(keys[i]);
            if (keys[i].equals(DUMMY)) {
                continue;
            }
            try {
                String key = Base64.encodeBytes(keys[i].getBytes(Daten.ENCODING_UTF));
                String val = Base64.encodeBytes(value.toString().getBytes(Daten.ENCODING_UTF));
                s += (s.length() > 0 ? DELIM_ELEMENTS : "") +
                     key + DELIM_KEYVALUE + val;
            } catch(Exception e) {
                // should never happen (program error); no need to translate
                Logger.log(Logger.ERROR, Logger.MSG_CORE_DATATYPEINVALIDVALUE,
                         "ConfigTypesHashtable: cannot create string for value '"+keys[i]+"': "+e.toString());
            }
        }
        return s;
    }

    protected void iniDisplay() {
        // not used, everything done in displayOnGui(...)
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        this.dlg = dlg;

        if (Daten.efaConfig.getHeaderUseHighlightColor()) {
	        titlelabel = new RoundedLabel();
	        titlelabel.setBackground(Daten.efaConfig.getHeaderBackgroundColor());
	        titlelabel.setForeground(Daten.efaConfig.getHeaderForegroundColor());
	        titlelabel.setOpaque(true);
	        titlelabel.setFont(titlelabel.getFont().deriveFont(Font.BOLD));
	        titlelabel.setBorder(new RoundedBorder(titlelabel.getForeground()));
        } else {
        	titlelabel=new JLabel();
        }
        Mnemonics.setLabel(dlg, titlelabel, " " + getDescription() + ": ");
        if (type == IItemType.TYPE_EXPERT) {
            if (!Daten.efaConfig.getHeaderUseHighlightColor()) {
            	titlelabel.setForeground(Color.red);
            }
        }
        if (color != null) {
            titlelabel.setForeground(color);
        }
        
        String[] keys = getKeysArray();

        panel.add(titlelabel, new GridBagConstraints(x, y, 2, 1, 0.0, 0.0,
                  GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(padYbefore, padXbefore, (keys.length == 0 ? padYafter : 0), 0), 0, 0));
        if (allowedAdd) {
            addButton = new JButton();
            addButton.setIcon(BaseFrame.getIcon("menu_plus.gif"));
            addButton.setMargin(new Insets(0, 0, 0, 0));
            Dialog.setPreferredSize(addButton, 19, 19);
            addButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addButtonHit(e);
                }
            });
            panel.add(addButton, new GridBagConstraints(x + 2, y, 2, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 0, (keys.length == 0 ? padYafter : 0), padXafter), 0, 0));
        }


        textfield = new JTextField[size()];
        delButtons = new Hashtable();
        for (int i=0; i<keys.length; i++) {
            textfield[i] = new JTextField();
            textfield[i].setText(get(keys[i]).toString());
            textfield[i].setEditable(isEditable);
            Dialog.setPreferredSize(textfield[i], 200, 19);
            JLabel label = new JLabel();
            Mnemonics.setLabel(dlg, label, keys[i] + ": ");
            label.setLabelFor(textfield[i]);
            if (type == IItemType.TYPE_EXPERT) {
                label.setForeground(Color.red);
            }
            if (color != null) {
                label.setForeground(color);
            }
            panel.add(label, new GridBagConstraints(x, y+i+1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, padXbefore, (i+1 == keys.length ? padYafter : 0), 0), 0, 0));
            panel.add(textfield[i], new GridBagConstraints(x+1, y+i+1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, (i+1 == keys.length ? padYafter : 0), 0), 0, 0));
            if (allowedDelete) {
                JButton delButton = new JButton();
                delButton.setIcon(BaseFrame.getIcon("menu_minus.gif"));
                delButton.setMargin(new Insets(0, 0, 0, 0));
                Dialog.setPreferredSize(delButton, 19, 19);
                delButton.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        delButtonHit(e);
                    }
                });
                panel.add(delButton, new GridBagConstraints(x + 2, y + i + 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, (i + 1 == keys.length ? padYafter : 0), 0), 0, 0));
                delButtons.put(delButton, keys[i]);
            }
        }
        return keys.length+1;
    }

    private void addButtonHit(ActionEvent e) {
        if (!allowedAdd) {
            return;
        }

        String key = null;
        key = Dialog.inputDialog(International.getString("Neuen Eintrag hinzufügen"),
                                 International.getString("Bezeichnung") + ": ");
        if (key == null || key.length() == 0 || dlg == null) {
            return;
        }
        if (hash.get(key) != null) {
            Dialog.error(International.getString("Name bereits vergeben"+"!"));
            return;
        }
        getValueFromGui();

        // instead of simply using hash.put(key, hash.get(DUMMY)), we
        // need to invoke addToHash(...) instead because of some special handling
        // implemented there (i.e. making sure for Admin data that the admin's name
        // equals the key value!).
        addToHash(hash, key, hash.get(DUMMY).toString());

        if (dlg instanceof BaseDialog) {
            ((BaseDialog)dlg).updateGui();
        }
    }

    private void delButtonHit(ActionEvent e) {
        if (!allowedDelete) {
            return;
        }
        String key = delButtons.get(e.getSource());
        if (key == null || dlg == null) {
            return;
        }
        if (Dialog.yesNoDialog(International.getString("Eintrag löschen"),
                               International.getMessage("Möchtest Du den Eintrag '{entry}' wirklich löschen?",key)) == Dialog.YES) {
            getValueFromGui();
            hash.remove(key);
            if (dlg instanceof BaseDialog) {
                ((BaseDialog)dlg).updateGui();
            }
        }
    }

    public void getValueFromGui() {
        Hashtable<String,E> newHash = new Hashtable<String,E>();
        newHash.put(DUMMY, hash.get(DUMMY));
        String[] keys = getKeysArray();
        if (textfield == null || keys.length != textfield.length) {
            // This happens when an element has been added or removed from the hash.
            // Therefore, in addButtonHit(e) resp. delButtonHit(e), we first call getValueFromGui()
            // before we add or remove an item, in order to retrieve all current values, then add
            // or remove an item, and then call efaConfigFrame.updateGui(). After that, updateGui()
            // will invoke getValueFromGui() again, this time with a mismatch of keys.length and
            // textfield.length. Since we already got the values, we can abort here.
            return;
        }
        for (int i=0; i<keys.length; i++) {
            if (textfield[i] != null) {
                addToHash(newHash,keys[i],textfield[i].getText().trim());
            }
        }
        hash = newHash;
    }

    public void requestFocus() {
        if (textfield != null && textfield.length > 0) {
            textfield[0].requestFocus();
        }
    }

    public boolean isValidInput() {
        return true;
    }

    public String getValueFromField() {
        return null;
    }

    public void showValue() {
    }

    public void setVisible(boolean visible) {
        titlelabel.setVisible(visible);
        addButton.setVisible(visible);
        for (int i=0; i<textfield.length; i++) {
            textfield[i].setVisible(visible);
        }
        JButton[] b = delButtons.keySet().toArray(new JButton[0]);
        for (int i=0; i<b.length; i++) {
            b[i].setVisible(visible);
        }
        super.setVisible(visible);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        titlelabel.setForeground((enabled ? (new JLabel()).getForeground() : Color.gray));
        addButton.setEnabled(enabled);
        for (int i=0; i<textfield.length; i++) {
            textfield[i].setEnabled(enabled);
        }
        JButton[] b = delButtons.keySet().toArray(new JButton[0]);
        for (int i=0; i<b.length; i++) {
            b[i].setEnabled(enabled);
        }
    }
}
