/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import de.nmichael.efa.core.items.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

// @i18n complete
public class EfaConfigDialog extends BaseTabbedDialog {

    private EfaConfig myEfaConfig;

    public EfaConfigDialog(Frame parent, EfaConfig efaConfig) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this.myEfaConfig = efaConfig;
    }

    public EfaConfigDialog(JDialog parent, EfaConfig efaConfig) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this.myEfaConfig = efaConfig;
    }

    public EfaConfigDialog(JDialog parent, EfaConfig efaConfig, String selectedPanel) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this._selectedPanel = selectedPanel;
        this.myEfaConfig = efaConfig;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }
    
    protected void iniDialog() throws Exception {
        super.iniDialog();
        closeButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        closeButton.setIconTextGap(10);
    }
    public void closeButton_actionPerformed(ActionEvent e) {
        getValuesFromGui();
        synchronized (myEfaConfig) {
            for (int i = 0; i < allGuiItems.size(); i++) {
                IItemType item = allGuiItems.get(i);
                if (item.isChanged()) {
                    myEfaConfig.setValue(item.getName(), item.toString());
                }
            }
        }
        myEfaConfig.checkNewConfigValues();
        myEfaConfig.setExternalParameters(true);
        myEfaConfig.checkForRequiredPlugins();
        super.closeButton_actionPerformed(e);
        setDialogResult(true);
    }

    /*
     * The following methods will return the current working items (needed by ItemTypeAction to
     * generate new types), by first fetching the name of the item from the real EfaConfig, and
     * then find the current working item by this name.
     */
    public ItemTypeHashtable<String> getTypesBoat() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesBoat().getName());
    }

    public ItemTypeHashtable<String> getTypesNumSeats() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesNumSeats().getName());
    }

    public ItemTypeHashtable<String> getTypesRigging() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesRigging().getName());
    }

    public ItemTypeHashtable<String> getTypesCoxing() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesCoxing().getName());
    }

    public ItemTypeHashtable<String> getTypesGender() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesGender().getName());
    }

    public ItemTypeHashtable<String> getTypesSession() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesSession().getName());
    }

    public ItemTypeHashtable<String> getTypesStatus() {
        return (ItemTypeHashtable<String>)getItem(myEfaConfig.getValueTypesStatus().getName());
    }

    
    // Efa Config Dialogue needs its own recursiveBuildGui...
	protected int recursiveBuildGui(Hashtable<String, Hashtable> categories, Hashtable<String, Vector<IItemType>> items,
			String catKey, JComponent currentPane, String selectedPanel, int otherPanelHeight) {
		int itmcnt = 0;
		int pos = (selectedPanel != null && selectedPanel.length() > 0 ? selectedPanel.indexOf(CATEGORY_SEPARATOR)
				: -1);
		String selectThisCat = (pos < 0 ? selectedPanel : selectedPanel.substring(0, pos));
		String selectNextCat = (pos < 0 ? null : selectedPanel.substring(pos + 1));

		Object[] cats = categories.keySet().toArray();
		Arrays.sort(cats);
		for (int i = 0; i < cats.length; i++) {
			String key = (String) cats[i];
			String thisCatKey = (catKey.length() == 0 ? key : makeCategory(catKey, key));
			String catName = getCatName(thisCatKey);
			Hashtable<String, Hashtable> subCat = categories.get(key);
			if (subCat.size() != 0) {
				JTabbedPane subTabbedPane = new JTabbedPane();
				if (recursiveBuildGui(subCat, items, thisCatKey, subTabbedPane, selectNextCat, otherPanelHeight) > 0) {
					if (currentPane instanceof JTabbedPane) {
						currentPane.add(subTabbedPane, catName);
					} else {
						currentPane.add(subTabbedPane, BorderLayout.CENTER);
					}
					if (key.equals(selectThisCat) && currentPane instanceof JTabbedPane) {
						((JTabbedPane) currentPane).setSelectedComponent(subTabbedPane);
					}
				}
			} else {
				JPanel panel = new JPanel();
				panels.put(panel, thisCatKey);
				JPanel innerPanel = new JPanel();

				//This puts the scrollbar INSIDE the tabbedPane, so that config panes can have more elements
				//than the current screen size allows.
				JScrollPane scrollPane = new JScrollPane(innerPanel);
		        scrollPane.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
				scrollPane.setPreferredSize(EfaGuiUtils.getTabPanelPreferredSizeEfaConfig(EfaGuiUtils.getSubCatCount(thisCatKey),this));
				scrollPane.getVerticalScrollBar().setUnitIncrement(12);
				innerPanel.setLayout(new GridBagLayout());
				panel.setLayout(new BorderLayout());
				panel.add(scrollPane,BorderLayout.CENTER);
				Vector<IItemType> v = items.get(thisCatKey);
				int y = 0;
				for (int j = 0; v != null && j < v.size(); j++) {
					IItemType itm = v.get(j);
					if (itm.getType() == IItemType.TYPE_PUBLIC
							|| (itm.getType() == IItemType.TYPE_EXPERT && expertModeEnabled)) {
						y += itm.displayOnGui(this, innerPanel, y);
						displayedGuiItems.add(itm);
						itmcnt++;
					}
				}
				if (y > 0) {
					if (currentPane instanceof JTabbedPane) {
						currentPane.add(panel, catName);
					} else {
						currentPane.add(panel, BorderLayout.CENTER);
					}
					if (key.equals(selectThisCat) && currentPane instanceof JTabbedPane) {
						((JTabbedPane) currentPane).setSelectedComponent(panel);
					}
				}
			}
		}
		return itmcnt;
	}  
    
}
