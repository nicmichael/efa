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

import de.nmichael.efa.core.config.*;
import de.nmichael.efa.gui.EfaConfigDialog;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.Daten;
import java.awt.*;
import java.awt.event.*;

// @i18n complete

public class ItemTypeAction extends ItemTypeButton {

    public static final int ACTION_TYPES_RESETTODEFAULT = 1;
    public static final int ACTION_TYPES_ADDALLDEFAULT = 2;
    public static final int ACTION_GENERATE_ROWING_BOAT_TYPES = 3;
    public static final int ACTION_GENERATE_CANOEING_BOAT_TYPES = 4;

    private int action;

    public ItemTypeAction(String name, int action,
            int type, String category, String description) {
        super(name, type, category, description);
        this.action = action;
        fieldGridWidth = 3;
        fieldGridFill = GridBagConstraints.HORIZONTAL;
        fieldWidth = 500;
        fieldHeight = 21;
    }

    public IItemType copyOf() {
        return new ItemTypeAction(name, action, type, category, description);
    }

    public void actionEvent(AWTEvent e) {
        if (e == null || !(e instanceof ActionEvent)) {
            return;
        }
        switch(action) {
            case ACTION_TYPES_RESETTODEFAULT:
                resetTypesToDefault();
                break;
            case ACTION_TYPES_ADDALLDEFAULT:
                addAllDefaultSessionTypes();
                break;
            case ACTION_GENERATE_ROWING_BOAT_TYPES:
                generateTypes(ACTION_GENERATE_ROWING_BOAT_TYPES);
                break;
            case ACTION_GENERATE_CANOEING_BOAT_TYPES:
                generateTypes(ACTION_GENERATE_CANOEING_BOAT_TYPES);
                break;
        }
        super.actionEvent(e);
    }

    private void resetTypesToDefault() {
        if (Dialog.yesNoDialog(International.getString("Frage"),
                International.getString("Möchtest Du alle Typen auf die Standard-Einstellungen zurücksetzen? "+
                "Manuell hinzugefügte Typen bleiben dabei bestehen.")) != Dialog.YES) {
            return;
        }

        // resetTypesToDefault() is only called from buttonHit(ActionEvent) if the configured action is
        // ACTION_TYPES_RESETTODEFAULT.
        // This is (and must!) only be the case if dlg is a EfaConfigFrame!
        EfaConfigDialog efaConfigFrame = null;
        try {
            efaConfigFrame = (EfaConfigDialog)dlg;
        } catch(ClassCastException ee) {
            return;
        }

        Daten.efaTypes.setCustSettings(new CustSettings(Daten.efaConfig));
        Daten.efaTypes.setToLanguage(null);

        int count = 0;
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesBoat(), EfaTypes.CATEGORY_BOAT, true);
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesNumSeats(), EfaTypes.CATEGORY_NUMSEATS, true);
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesRigging(), EfaTypes.CATEGORY_RIGGING, true);
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesCoxing(), EfaTypes.CATEGORY_COXING, true);
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesGender(), EfaTypes.CATEGORY_GENDER, true);
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesSession(), EfaTypes.CATEGORY_SESSION, true);
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesStatus(), EfaTypes.CATEGORY_STATUS, true);

        Dialog.infoDialog(International.getMessage("Es wurden {count} Typen neu generiert (sichtbar im Expertenmodus).", count));
        efaConfigFrame.updateGui(false);
    }

    private void addAllDefaultSessionTypes() {
        if (Dialog.yesNoDialog(International.getString("Frage"),
                International.getString("Möchtest Du alle Standard-Fahrtarten neu generieren? " +
                "Manuell geänderte oder hinzugefügte Fahrtarten bleiben dabei bestehen.")) != Dialog.YES) {
            return;
        }

        // addAllDefaultSessionTypes() is only called from buttonHit(ActionEvent) if the configured action is
        // ACTION_TYPES_RESETTODEFAULT.
        // This is (and must!) only be the case if dlg is a EfaConfigFrame!
        EfaConfigDialog efaConfigFrame = null;
        try {
            efaConfigFrame = (EfaConfigDialog)dlg;
        } catch(ClassCastException ee) {
            return;
        }

        Daten.efaTypes.setToLanguage_Sessions(International.getResourceBundle(), true);
        int count = 0;
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesSession(), EfaTypes.CATEGORY_SESSION, false);

        Dialog.infoDialog(International.getMessage("Es wurden {count} Typen neu generiert (sichtbar im Expertenmodus).", count));
        efaConfigFrame.updateGui(false);
    }

    private void generateTypes(int selection) {
        String sel = null;
        switch(selection) {
            case ACTION_GENERATE_ROWING_BOAT_TYPES:
                sel = International.getString("Rudern");
                break;
            case ACTION_GENERATE_CANOEING_BOAT_TYPES:
                sel = International.getString("Kanufahren");
                break;
        }
        if (sel == null) {
            return;
        }
        if (Dialog.yesNoDialog(International.getString("Frage"),
                International.getMessage("Möchtest Du alle Standard-Bootstypen für {rowing_or_canoeing} jetzt neu hinzufügen? "+
                "Manuell geänderte oder hinzugefügte Bootstypen bleiben dabei bestehen.", sel)) != Dialog.YES) {
            return;
        }

        // generateTypes(int) is only called from buttonHit(ActionEvent) if the configured action is
        // ACTION_GENERATE_ROWING_BOAT_TYPES or ACTION_GENERATE_CANOEING_BOAT_TYPES.
        // This is (and must!) only be the case if dlg is a EfaConfigFrame!
        EfaConfigDialog efaConfigFrame = null;
        try {
            efaConfigFrame = (EfaConfigDialog)dlg;
        } catch(ClassCastException ee) {
            return;
        }

        int efaTypesBoatsToCreate = (selection == ACTION_GENERATE_ROWING_BOAT_TYPES ? EfaTypes.SELECTION_ROWING : EfaTypes.SELECTION_CANOEING);

        Daten.efaTypes.setToLanguage_Boats(International.getResourceBundle(), efaTypesBoatsToCreate, true);

        int count = 0;
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesBoat(), EfaTypes.CATEGORY_BOAT, false);
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesNumSeats(), EfaTypes.CATEGORY_NUMSEATS, false);
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesRigging(), EfaTypes.CATEGORY_RIGGING, false);
        count += addNewTypes(Daten.efaTypes, efaConfigFrame.getTypesCoxing(), EfaTypes.CATEGORY_COXING, false);

        Dialog.infoDialog(International.getMessage("Es wurden {count} Bootstypen neu generiert (sichtbar im Expertenmodus).", count));
        efaConfigFrame.updateGui(false);
    }

    private int addNewTypes(EfaTypes types, ItemTypeHashtable<String> cfgTypes, String cat, boolean overwrite) {
        int count = 0;
        String[] t = types.getTypesArray(cat);
        String[] v = types.getValueArray(cat);
        for (int i=0; i<t.length && i<v.length; i++) {
            String key = t[i];
            String val = v[i];
            if (cfgTypes.get(key) == null || overwrite) {
                cfgTypes.put(key, val);
                count++;
            }
        }
        return count;
    }

}
