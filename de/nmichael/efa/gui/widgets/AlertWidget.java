/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.widgets;

import java.util.Vector;

import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeMultiSelectList;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class AlertWidget extends Widget implements IItemFactory {

    public static final String NAME = "Alert-Widget";

    static final String TYPE_INFO = "TINFO";
    static final String TYPE_WARN = "TWARN";

    static final String PARAM_ALERTS        = "Alert";
    
    static final String PARAM_ATSTART       = "AtStart";
    static final String PARAM_ATFINISH      = "AtFinish";
    static final String PARAM_BOATTYPE      = "BoatType";
    static final String PARAM_BOATSEATS     = "BoatSeats";
    static final String PARAM_TYPE          = "Type";
    static final String PARAM_TEXT          = "Text";

    public AlertWidget() {
        super(NAME, International.getString("Hinweis-Widget"), false, true, false, false);
        ItemTypeItemList item;
        addParameterInternal(item = new ItemTypeItemList(PARAM_ALERTS, new Vector<IItemType[]>(), this,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Hinweise bei Fahrtbeginn und Fahrtende")));
        item.setShortDescription(International.getString("Hinweise"));
        super.setEnabled(false);
    }

    ItemTypeItemList getAlertList() {
        return (ItemTypeItemList)getParameterInternal(PARAM_ALERTS);
    }

    public static boolean isShowOnSessionStart(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeBoolean)list.getItem(i, PARAM_ATSTART)).getValue();
        } catch(Exception e) {
            return false;
        }
    }

    public static boolean isShowOnSessionFinish(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeBoolean)list.getItem(i, PARAM_ATFINISH)).getValue();
        } catch(Exception e) {
            return false;
        }
    }

    private static boolean isInSelection(String key, Object[] selection) {
        if (selection == null || selection.length == 0) {
            return true;
        }
        if (key == null) {
            return false;
        }
        for (Object s : selection) {
            if (key.equals(s.toString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isShowForBoat(ItemTypeItemList list, int i, String bType, String bSeats) {
        try {
            return isInSelection(bType, ((ItemTypeMultiSelectList)list.getItem(i, PARAM_BOATTYPE)).getValues()) &&
                   isInSelection(bSeats, ((ItemTypeMultiSelectList)list.getItem(i, PARAM_BOATSEATS)).getValues());
        } catch(Exception e) {
            return false;
        }
    }

    public static String getType(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeStringList)list.getItem(i, PARAM_TYPE)).getValue();
        } catch(Exception e) {
            return TYPE_INFO;
        }
    }

    public static String getText(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeString)list.getItem(i, PARAM_TEXT)).getValue();
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * While we're loading EfaConfig, we don't have loaded EfaTypes yet, so we need to update
     * the list data for boat types and seats after loading EfaTypes.
     */
    public void updateTypes() {
        try {
            ItemTypeItemList list = getAlertList();
            if (list == null) {
                return;
            }
            for (int i = 0; i < list.size(); i++) {
                ((ItemTypeMultiSelectList)list.getItem(i, PARAM_BOATTYPE)).setListData(
                        EfaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES),
                        EfaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY));
                ((ItemTypeMultiSelectList)list.getItem(i, PARAM_BOATSEATS)).setListData(
                        EfaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES),
                        EfaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY));
            }
        } catch (Exception eignore) {
            Logger.logdebug(eignore);
        }
    }



    public IItemType[] getDefaultItems(String itemName) {
        if (itemName.endsWith(PARAM_ALERTS)) {
            ItemTypeItemList item = (ItemTypeItemList)getParameterInternal(PARAM_ALERTS);
            int i = item.size()+1;
            IItemType[] items = new IItemType[6];
            items[0] = new ItemTypeBoolean(PARAM_ATSTART, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("bei Fahrtbeginn"));
            items[1] = new ItemTypeBoolean(PARAM_ATFINISH, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("bei Fahrtende"));
            items[2] = new ItemTypeMultiSelectList<String>(PARAM_BOATTYPE,
                    new DataTypeList<String>(EfaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES)),
                    EfaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES),EfaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                    IItemType.TYPE_PUBLIC, "",
                    International.getString("für Fahrten mit ..."));
            items[3] = new ItemTypeMultiSelectList<String>(PARAM_BOATSEATS,
                    new DataTypeList<String>(EfaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES)),
                    EfaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES),
                    EfaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                    IItemType.TYPE_PUBLIC, "",
                    International.getString("für Fahrten mit ..."));
            ((ItemTypeMultiSelectList)items[3]).setXOffset(1);
            ((ItemTypeMultiSelectList)items[3]).setYOffset(-1);
            items[4] = new ItemTypeStringList(PARAM_TYPE,
                TYPE_INFO,
                new String[] { TYPE_INFO, TYPE_WARN },
                new String[] { 
                    International.getString("Info"),
                    International.getString("Warnung")
                },
                IItemType.TYPE_PUBLIC, "",
                International.getString("Art"));
            items[4].setPadding(0, 0, 5, 0);
            items[5] = new ItemTypeString(PARAM_TEXT,
                "",IItemType.TYPE_PUBLIC, "",
                International.getString("Text"));
            return items;
        }
        return null;
    }

	@Override
	public Vector<WidgetInstance> createInstances() {
		Vector <WidgetInstance> returnList = new Vector <WidgetInstance>();
		AlertWidgetInstance wi = new AlertWidgetInstance();
		wi.setAlertList(this.getAlertList());
		wi.setPosition(IWidget.POSITION_CENTER);
		returnList.add(wi);
		return returnList;
	}

    @Override 
    public boolean isGuiWidget() {
    	return false;
    }
}
