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
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

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
    
    protected int recursiveBuildGui(Hashtable<String,Hashtable> categories,
            Hashtable<String,Vector<IItemType>> items,
            String catKey,
            JComponent currentPane,
            String selectedPanel, int otherPanelHeight) {
		int itmcnt = 0;
		int pos = (selectedPanel != null && selectedPanel.length() > 0 ? selectedPanel.indexOf(CATEGORY_SEPARATOR) : -1);
		String selectThisCat = (pos < 0 ? selectedPanel : selectedPanel.substring(0,pos));
		String selectNextCat = (pos < 0 ? null : selectedPanel.substring(pos+1));
		
		Object[] cats = categories.keySet().toArray();
		Arrays.sort(cats);
		for (int i=0; i<cats.length; i++) {
			String key = (String)cats[i];
			String thisCatKey = (catKey.length() == 0 ? key : makeCategory(catKey, key));
			String catName = getCatName(thisCatKey);
			Hashtable<String,Hashtable> subCat = categories.get(key);
			
			if (subCat.size() != 0) {
				JTabbedPane subTabbedPane = new JTabbedPane();
					if (recursiveBuildGui(subCat, items, thisCatKey, subTabbedPane, selectNextCat, otherPanelHeight) > 0) {
						if (currentPane instanceof JTabbedPane) {
						 currentPane.add(subTabbedPane, catName);
						} else {
						 currentPane.add(subTabbedPane, BorderLayout.CENTER);
						}
						if (key.equals(selectThisCat) && currentPane instanceof JTabbedPane) {
						 ((JTabbedPane)currentPane).setSelectedComponent(subTabbedPane);
						}
				}
			} else {
			JPanel panel = new JPanel();
			panels.put(panel, thisCatKey);
			
			boolean needsInnerPanel = (cats.length > 1 || subCat.size() > 0);
			JPanel innerPanel = new JPanel();
			
			if (needsInnerPanel) {
				//This puts the scrollbar INSIDE the tabbedPane, so that config panes can have more elements
				//than the current screen size allows.
				JScrollPane scrollPane = new JScrollPane(innerPanel);
				scrollPane.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
				scrollPane.setPreferredSize(EfaGuiUtils.getTabPanelPreferredSize(otherPanelHeight ,  this, 700, 700));
				scrollPane.getVerticalScrollBar().setUnitIncrement(12);
				innerPanel.setLayout(new GridBagLayout());
				panel.setLayout(new BorderLayout());
				panel.add(scrollPane,BorderLayout.CENTER);
			} else {
				panel.setLayout(new GridBagLayout());
			}
			
			Vector<IItemType> v = items.get(thisCatKey);
			int y = 0;
			for (int j=0; v != null && j<v.size(); j++) {
				IItemType itm = v.get(j);
				if (itm.getType() == IItemType.TYPE_PUBLIC ||
				 (itm.getType() == IItemType.TYPE_EXPERT && expertModeEnabled)) {
					 y += itm.displayOnGui(this,(needsInnerPanel ? innerPanel: panel),y);
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
				 ((JTabbedPane)currentPane).setSelectedComponent(panel);
				}
			}
		}
	}
	return itmcnt;
	}
    
}
