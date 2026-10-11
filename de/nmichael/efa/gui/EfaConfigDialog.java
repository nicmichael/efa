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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeHashtable;
import de.nmichael.efa.util.International;

// @i18n complete
public class EfaConfigDialog extends BaseTabbedDialog {

    private EfaConfig myEfaConfig;

    public EfaConfigDialog(Frame parent, EfaConfig efaConfig) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this.myEfaConfig = efaConfig;
        this.setNavigationMode(MODE_LEFT_NAVIGATION);
        this.setContentDimension(getTabPanelPreferredSize(this, NAVIGATIONLIST_WIDTH+40, 200));
    }

    public EfaConfigDialog(JDialog parent, EfaConfig efaConfig) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this.myEfaConfig = efaConfig;
        this.setNavigationMode(MODE_LEFT_NAVIGATION);
        this.setContentDimension(getTabPanelPreferredSize(this, NAVIGATIONLIST_WIDTH+40, 200));
    }

    public EfaConfigDialog(JDialog parent, EfaConfig efaConfig, String selectedPanel) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this._selectedPanel = selectedPanel;
        this.myEfaConfig = efaConfig;
        this.setNavigationMode(MODE_LEFT_NAVIGATION);
        this.setContentDimension(getTabPanelPreferredSize(this, NAVIGATIONLIST_WIDTH+40, 200));
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

    public Dimension getTabPanelPreferredSize(BaseDialog base, int reduceWidth, int reduceHeight) {
		Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
		
		Dimension efaBthsSize = null;
		Frame myParentFrame=EfaGuiUtils.getParentFrameRecursive(base);
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
   		maxDlgW=Math.max(maxDlgW-reduceWidth,500);
   		maxDlgH=Math.max(maxDlgH-reduceHeight, 500);
    	
		return new Dimension(
				maxDlgW, maxDlgH);
				//(int) Math.round(maxDlgW*.85), 
				//(int) Math.round(maxDlgH*.70));
    }
    
}
