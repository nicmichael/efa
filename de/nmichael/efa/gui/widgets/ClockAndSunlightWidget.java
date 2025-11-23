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

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.core.items.ItemTypeLongLat;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.util.International;  

public class ClockAndSunlightWidget extends Widget {

    static final String PARAM_SHOWCOCK         = "ShowClock";
    static final String PARAM_SHOWDATE         = "ShowDate";
	
    static final String PARAM_SHOWSUNRISE         = "ShowSunrise";
    static final String PARAM_LATITUDE            = "Latitude";
    static final String PARAM_LONGITUDE           = "Longitude";

    static final String PARAM_WARNDARKNESS          = "WarnDarkness";
    static final String PARAM_WARNTIMEBEFORESUNSET  = "WarnTimeBeforeSunset";
    static final String PARAM_WARNTIMEAFTERSUNSET   = "WarnTimeAfterSunset";
    static final String PARAM_WARNTIMEBEFORESUNRISE = "WarnTimeBeforeSunrise";
    static final String PARAM_WARNTEXTDARKSOON      = "WarnTextDarkSoon";
    static final String PARAM_WARNTEXTDARKNOW       = "WarnTextDarkNow";
    
    static final String LAYOUT_HORIZONTAL         = "HORIZONTAL";
    static final String LAYOUT_VERTICAL           = "VERTICAL";

    static final String PARAM_LAYOUT              = "Layout";

    public ClockAndSunlightWidget() {
        super(International.getString("Uhr und Tageslicht"), "MeteoAstro", International.getString("Uhr und Tageslicht"), true,false);

        addParameterInternal(new ItemTypeStringList(PARAM_LAYOUT, LAYOUT_HORIZONTAL,
                new String[] { LAYOUT_HORIZONTAL, LAYOUT_VERTICAL },
                new String[] { International.getString("horizontal"),
                               International.getString("vertikal")
                },
                IItemType.TYPE_PUBLIC, "",
                International.getString("Layout")));
        
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWCOCK, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Uhrzeit anzeigen")));
        
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWDATE, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Datum anzeigen")));
        
        addHeader(NOT_STORED_ITEM_PREFIX+"WidgetMeteoSunrise",IItemType.TYPE_PUBLIC, "", International.getString("Sonnenaufgang/Sonnenuntergang"), 3);        
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWSUNRISE, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Sonnenaufgangs- und -untergangszeit anzeigen")));
        addParameterInternal(new ItemTypeLongLat(PARAM_LATITUDE,
                ItemTypeLongLat.ORIENTATION_NORTH,52,25,9,
                IItemType.TYPE_PUBLIC, "",
                International.getString("geographische Breite")));
        addParameterInternal(new ItemTypeLongLat(PARAM_LONGITUDE,
                ItemTypeLongLat.ORIENTATION_EAST,13,10,15,
                IItemType.TYPE_PUBLIC, "",
                International.getString("geographische Länge")));
        
        addHeader(NOT_STORED_ITEM_PREFIX+"WidgetMeteoAstroWarning",IItemType.TYPE_PUBLIC, "", International.getString("Warnhinweise bei Dunkelheit"), 3);          
        addParameterInternal(new ItemTypeBoolean(PARAM_WARNDARKNESS, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Bei Fahrtbeginn vor Dunkelheit warnen")));
        addParameterInternal(new ItemTypeString(PARAM_WARNTEXTDARKSOON,
                International.getString("Achtung, es wird bald dunkel.") + "<br>" +
                International.getString("Bitte nimm Licht mit!"),
                IItemType.TYPE_PUBLIC, "",
                International.getString("Warntext vor Einbruch der Dunkelheit")));
        addParameterInternal(new ItemTypeString(PARAM_WARNTEXTDARKNOW,
                International.getString("Achtung, es ist dunkel.") + "<br>" +
                International.getString("Bitte nimm Licht mit!"),
                IItemType.TYPE_PUBLIC, "",
                International.getString("Warntext bei Dunkelheit")));
        addParameterInternal(new ItemTypeInteger(PARAM_WARNTIMEBEFORESUNSET, 30, 0, 60, false,
                IItemType.TYPE_EXPERT, "",
                PARAM_WARNTIMEBEFORESUNSET));
        addParameterInternal(new ItemTypeInteger(PARAM_WARNTIMEAFTERSUNSET, 30, 0, 60, false,
                IItemType.TYPE_EXPERT, "",
                PARAM_WARNTIMEAFTERSUNSET));
        addParameterInternal(new ItemTypeInteger(PARAM_WARNTIMEBEFORESUNRISE, 30, 0, 60, false,
                IItemType.TYPE_EXPERT, "",
                PARAM_WARNTIMEBEFORESUNRISE));

        super.setEnabled(true);
        super.setPosition(IWidget.POSITION_CENTER);
        
    }

    public boolean isShowSunrise() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWSUNRISE)).getValue();
    }

    public boolean isShowClock() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWCOCK)).getValue();   	
    }
    
    public boolean isShowDate() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWDATE)).getValue();   	
    }
    
    public ItemTypeLongLat getLatitude() {
        return (ItemTypeLongLat)getParameterInternal(PARAM_LATITUDE);
    }

    public ItemTypeLongLat getLongitude() {
        return (ItemTypeLongLat)getParameterInternal(PARAM_LONGITUDE);
    }

    public boolean isWarnDarkness() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_WARNDARKNESS)).getValue();
    }
    public int getWarnTimeBeforeSunset() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WARNTIMEBEFORESUNSET)).getValue();
    }
    public int getWarnTimeAfterSunset() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WARNTIMEAFTERSUNSET)).getValue();
    }
    public int getWarnTimeBeforeSunrise() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WARNTIMEBEFORESUNRISE)).getValue();
    }
    public String getWarnTextDarkSoon() {
        return ((ItemTypeString)getParameterInternal(PARAM_WARNTEXTDARKSOON)).toString();
    }
    public String getWarnTextDarkNow() {
        return ((ItemTypeString)getParameterInternal(PARAM_WARNTEXTDARKNOW)).toString();
    }

    
    public String getLayout() {
        return ((ItemTypeStringList)getParameterInternal(PARAM_LAYOUT)).toString();   
    }

	@Override
	public Vector<WidgetInstance> createInstances() {
		Vector <WidgetInstance> returnList = new Vector <WidgetInstance>();
		ClockAndSunlightWidgetInstance wi = new ClockAndSunlightWidgetInstance();
		
		wi.setLatitude(getLatitude());
		wi.setLayout(this.getLayout());
		wi.setLongitude(getLongitude());
		wi.setShowClock(isShowClock());
		wi.setShowDate(isShowDate());
		wi.setShowSunrise(isShowSunrise());
		wi.setWarnDarkness(isWarnDarkness());
		wi.setWarnTextDarkNow(this.getWarnTextDarkNow());
		wi.setWarnTextDarkSoon(this.getWarnTextDarkSoon());
		wi.setWarnTimeAfterSunset(getWarnTimeAfterSunset());
		wi.setWarnTimeBeforeSunrise(getWarnTimeBeforeSunrise());
		wi.setWarnTimeBeforeSunset(getWarnTimeBeforeSunset());
		
		returnList.add(wi);
		return returnList;
	}

}
