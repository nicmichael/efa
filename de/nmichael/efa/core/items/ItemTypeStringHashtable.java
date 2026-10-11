package de.nmichael.efa.core.items;

import java.awt.Color;
import java.awt.Dimension;
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


/*
 * This class is (almost) a copy of ItemTypeHashtable (which can be parameterized for the actual
 * value type).
 * 
 * This is necessary because
 * a) ItemTypeHashTable for some reason casts the value to string in some cases (not very stringent
 *    on type parameterization)
 * b) it is heavily used by efaConfig concerning efaTypes, and there are some parts in the code
 *    of ItemTypeHashTable which are not easy to understand and should not be messed around with.
 *    
 * So as I need a hashtable for some translations, I made a copy of ItemTypeHashTable and set it up
 * to support only String values. Also, the code has been tidied up (addButton, deleteButton, getValueFromGUI()
 * so that it works cleanly.
 * 
 */
public class ItemTypeStringHashtable extends ItemType {
	
    private static final String DELIM_KEYVALUE = "-->";
    private static final String DELIM_ELEMENTS = "@@@";
    protected Hashtable<String,String> hash;

    private JLabel titlelabel;
    private JButton addButton;
    protected JTextField[] textfield;
    private Hashtable<JButton,String> delButtons;
    private boolean allowedAdd = true;
    private boolean allowedDelete = true;
    protected String value;

	public ItemTypeStringHashtable(String name, String value, boolean fieldsEditable,
            int type, String category, String description) {
        this.name = name;
        setEditable(fieldsEditable);
        this.type = type;
        this.category = category;
        this.description = description;
        this.value=value;
        this.padYbefore = 20;
        this.padYafter = 20;
        iniHash();
	}

	@Override
	public IItemType copyOf() {
		ItemTypeStringHashtable item = new ItemTypeStringHashtable(name, value, isEditable, type, category, description);
        String[] myKeys = this.getKeysArray();
        for (int i=0; i<myKeys.length; i++) {
            String e = get(myKeys[i]);
            item.put(myKeys[i],e);
        }
        return item;
	}

    public void setAllowed(boolean allowedAdd, boolean allowedDelete) {
        this.allowedAdd = allowedAdd;
        this.allowedDelete = allowedDelete;
    }

    private void iniHash() {
        hash = new Hashtable<String,String>();
    }

    public void put(String s, String value) {
        hash.put((s!=null ? s.trim() : null), value);
    }
    
    public void addToHashExternal(String s, String value) {
    	addToHash(hash, s, value);
    }

    public void replace(String s, String value) {
    	hash.replace((s!=null ? s.trim() : null), value);
    }
    
    public void remove(String s) {
        hash.remove(s);
    }

    public String get(String s) {
        return hash.get(s);
    }

    public int size() {
        return hash.size();
    }

    public String[] getKeysArray() {
        String[] keys = new String[size()];
        Object[] a = hash.keySet().toArray();
        Arrays.sort(a);
        int j=0;
        for (int i=0; i<a.length; i++) {
            keys[j++] = (String)a[i];
        }
        return keys;
    }

    protected void addToHash(Hashtable<String,String> hash, String key, String val) {
        hash.put((key!=null ? key.trim():null), val);
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
                addToHash(hash, key, val);
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
            String value = hash.get(keys[i]);
            try {
                String key = Base64.encodeBytes(keys[i].getBytes(Daten.ENCODING_UTF));
                String val = Base64.encodeBytes(value.toString().getBytes(Daten.ENCODING_UTF));
                s += (s.length() > 0 ? DELIM_ELEMENTS : "") +
                     key + DELIM_KEYVALUE + val;
            } catch(Exception e) {
                // should never happen (program error); no need to translate
                Logger.log(Logger.ERROR, Logger.MSG_CORE_DATATYPEINVALIDVALUE,
                         "ItemTypeStringHashtable: cannot create string for value '"+keys[i]+"': "+e.toString());
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
                  GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(padYbefore, padXbefore, (keys.length == 0 ? padYafter : 10), 0), 0, 0));
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
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 0, (keys.length == 0 ? padYafter : 10), padXafter), 0, 0));
        }


        textfield = new JTextField[size()];
        delButtons = new Hashtable();
        for (int i=0; i<keys.length; i++) {
            textfield[i] = new JTextField();
            textfield[i].setEditable(isEditable);
            //Put the Key in the "dummy" field of swing for Names
            textfield[i].setName(keys[i]); 
            textfield[i].setText(get(keys[i]).toString());

            Dialog.setPreferredSize(textfield[i], 200, 21);
            JLabel label = new JLabel();
            Mnemonics.setLabel(dlg, label, keys[i] + ": ");
            label.setToolTipText(label.getText());
            label.setMaximumSize(new Dimension(200,21));
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
        key = getNewKeyFromGUI();
        if (key == null || key.length() == 0 || dlg == null) {
            return;
        }
        if (hash.get(key) != null) {
            Dialog.error(International.getString("Name bereits vergeben"+"!"));
            return;
        }
        getValueFromGui();

        addToHash(hash, key, "");

        if (dlg instanceof BaseDialog) {
            ((BaseDialog)dlg).updateGui();
        }
    }

    /**
     * Read a new key from the GUI, e.g. by presenting a dialog where the user may enter a new string.
     * Can be overridden by subclasses.
     * @return new key or null if none was entered.
     */
    protected String getNewKeyFromGUI() {
    	return Dialog.inputDialog(International.getString("Neuen Eintrag hinzufügen"),
                International.getString("Bezeichnung") + ": ");
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
    
    @Override
	public void getValueFromGui() {
		Hashtable<String, String> newHash = new Hashtable<String, String>();
		String[] keys = getKeysArray();

		// get all keys, if set programmatically or not.
		for (int i = 0; i < keys.length; i++) {
			addToHash(newHash, keys[i], (String) hash.get(keys[i]));
		}
		// now, let's have a look at the fields and take the values from there
		for (int i = 0; i < textfield.length; i++) {
			//getName shows the key, getText the Value
			newHash.replace(textfield[i].getName(), textfield[i].getText());
		}
		hash = newHash;
	}

    @Override
    public void requestFocus() {
        if (textfield != null && textfield.length > 0) {
            textfield[0].requestFocus();
        }
    }
    
    @Override
    public boolean isValidInput() {
        return true;
    }

    @Override
    public String getValueFromField() {
        return null;
    }

    @Override
    public void showValue() {
    }

    @Override
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

    @Override
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
