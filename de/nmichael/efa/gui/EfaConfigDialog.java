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

    private Frame getParentFrameRecursive(BaseDialog base) {
    	if (base.getParentFrame()!=null) {
    		return base.getParentFrame();
    	} else if (base.getParentJDialog()!=null) {
    		if (base.getParentJDialog() instanceof BaseDialog) {
    			return getParentFrameRecursive((BaseDialog) base.getParentJDialog());
    		} else {
    			return null;
    		}
    	} else { 
    		return null;
    	}
    		
    }
    
    private Dimension getTabPanelPreferredSize(int numCats) {
    	
    	
		Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
		
		Dimension efaBthsSize = null;
		Frame myParentFrame=getParentFrameRecursive(this);
		if (myParentFrame!=null) {
			efaBthsSize=myParentFrame.getSize();
		}
		
    	int maxDlgW=Daten.efaConfig.getValueMaxDialogWidth();
    	int maxDlgH=Daten.efaConfig.getValueMaxDialogHeight()-20;
    	
    	//no max size for dialogs set? have a look at configured maximum screen width/height
    	if (maxDlgW<=0) {
    		maxDlgW=Daten.efaConfig.getValueScreenWidth();
    	}
    	if (maxDlgH<=0) {
    		maxDlgH=Daten.efaConfig.getValueScreenHeight();
    	}
    	
    	if (maxDlgW<=0 && efaBthsSize!=null) {
    		maxDlgW = efaBthsSize.width-4;
    	}
    	if (maxDlgH<=0 && efaBthsSize!=null) {
    		maxDlgH = efaBthsSize.height-90;
    	}
    	
    	
    	// No size configured for dialogs or even efaBths window? 
    	// then use screen height/width as base
    	if (maxDlgW<=0) {
    		maxDlgW=Math.min(s.width-80,1400);
    	}
    	if (maxDlgH<=0) {
    		maxDlgH=Math.min(s.height-((numCats+1)*25), 900);
    	}
    	
    		return new Dimension(
    				(int) Math.round(maxDlgW*.85), 
    				(int) Math.round(maxDlgH*.70));
    }
    
    private int getSubCatCount(String strCategory) {
    	String[] subCats=strCategory.split(":");
    	return subCats.length;
    }
    
    // Efa Config Dialogue needs its own recursiveBuildGui...
	protected int recursiveBuildGui(Hashtable<String, Hashtable> categories, Hashtable<String, Vector<IItemType>> items,
			String catKey, JComponent currentPane, String selectedPanel) {
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
				if (recursiveBuildGui(subCat, items, thisCatKey, subTabbedPane, selectNextCat) > 0) {
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
				scrollPane.setPreferredSize(getTabPanelPreferredSize(getSubCatCount(thisCatKey)));
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
