package de.nmichael.efa.gui.widgets;

import java.awt.GridBagConstraints;
import java.util.Vector;

import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeFile;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeLabelTextfield;
import de.nmichael.efa.core.items.ItemTypeLongLat;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

/*
 * WeatherWidget
 * In efa Versions 2.5.0 and lower, there was an MeteoAstroWidget, which provided multiple features
 * - Sunrise and Sunset
 * - Weather based on Yahoo Weather API (broken for several years)
 * - showing a popup when user clicks on the element.
 * - showing alerts (customizable) when a new session is started on the edge of day or night.
 * 
 * In efa Versions 2.5.1 and above, this is refactored into several Widgets:
 * - ClockAndSunlightWidget
 *   - Showing the current time and date, configureable
 *   - Including sunrise, sunset times
 *   - showing alerts just like the former MeteoAstroWidget
 *   - is an actual Widget, not a MiniWidget
 *        
 * - WeatherWidget
 *   - Provides multiple weather locations which can be configured individually
 *   - Obtains data from openMeteoApi server (data retrieval available for europe and north america)
 *   - Uses a WeatherData cache, so that multiple weather locations referring the same geo coordinates rely on the same weather data
 *   - Providing multiple rendering types
 *   - Show a popup window if user clicks on weather widget gui
 * 
 */

public class WeatherWidget extends Widget implements IItemFactory {

	private static final String PARAM_WEATHER_SOURCE 		= "MultiWeatherSource";
	public  static final String WEATHER_SOURCE_OPENMETEO = "WeatherSourceOpenMeteoFree";
	public  static final String WEATHER_SOURCE_WEATHERAPI = "WeatherSourceWeatherApi";
	private static final String PARAM_TEMPERATURESCALE = "MultiTemperatureScale";
	private static final String PARAM_SPEEDSCALE = "MultiWindSpeedScale";
	public  static final String SPEEDSCALE_MPH = "mph";
	public  static final String SPEEDSCALE_KMH = "kmh";
	public  static final String TEMP_CELSIUS = "CELSIUS";
	public  static final String TEMP_FAHRENHEIT = "FAHRENHEIT";
	private static final String PARAM_WEATHER_LOCATIONLIST 	= "MultiWeatherLocationList";
	
	//parameters per location item
	
	private static final String PARAM_LATITUDE 				= "Latitude";
	private static final String PARAM_LONGITUDE 			= "Longitude";
	private static final String PARAM_CAPTION 				= "Caption";
	
	private static final String PARAM_POPUPEXECCOMMAND    	= "PopupExecCommand";
	private static final String PARAM_HTMLPOPUPURL        	= "PopupUrl";
	private static final String PARAM_HTMLPOPWIDTH        	= "PopupWidth";
	private static final String PARAM_HTMLPOPHEIGHT       	= "PopupHeight";
	
	private static final String PARAM_WEATHER_LAYOUT 		= "Layout";
	public static final String WEATHER_LAYOUT_CURRENT_CLASSIC = "LayoutCurrentClassic";
	public static final String WEATHER_LAYOUT_CURRENT_WIND 	= "LayoutCurrentWind";
	public static final String WEATHER_LAYOUT_CURRENT_UVINDEX = "LayoutCurrentUVIndex";
	
	public static final String WEATHER_LAYOUT_FORECASTSIMPLE  = "LayoutForecastSimple";
	public static final String WEATHER_LAYOUT_FORECASTCOMPLEX = "LayoutForecastComplex";
	
	
	private static final int WEATHERWIDGET_GRIDWIDTH = 6;
	private static final int SMALL_FIELDWIDTH = 110;
	private ItemTypeItemList locationList;


	/**
	 * @param name
	 * @param description
	 * @param ongui
	 * @param showRefreshInterval
	 */
	public WeatherWidget() {

		super(International.getString("Wetter"), "MWeather", International.getString("Wetter"), true, true,WEATHERWIDGET_GRIDWIDTH);

		addHeader(NOT_STORED_ITEM_PREFIX+"MultiWeatherWidgetLocationHeader", IItemType.TYPE_PUBLIC, "", 
				International.getString("Wetter Daten"), WEATHERWIDGET_GRIDWIDTH);
		IItemType item;
		addParameterInternal(item=new ItemTypeStringList(PARAM_WEATHER_SOURCE, WEATHER_SOURCE_OPENMETEO,
				new String[] { WEATHER_SOURCE_OPENMETEO, WEATHER_SOURCE_WEATHERAPI },
				new String[] { International.getString("OpenMeteo kostenfreie API (Europa/Nord Amerika)"),
						International.getString("WeatherAPI") },
				IItemType.TYPE_PUBLIC, "", International.getString("Quelle für Wetterdaten")));
		item.setFieldGrid(WEATHERWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);

		addParameterInternal(item=new ItemTypeStringList(PARAM_TEMPERATURESCALE, TEMP_CELSIUS,
				new String[] { TEMP_CELSIUS, TEMP_FAHRENHEIT },
				new String[] { International.getString("Celsius"), International.getString("Fahrenheit") },
				IItemType.TYPE_PUBLIC, "", International.getString("Temperaturskala")),10,0);
		item.setFieldGrid(WEATHERWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
		
		addParameterInternal(item=new ItemTypeStringList(PARAM_SPEEDSCALE, SPEEDSCALE_KMH,
				new String[] { SPEEDSCALE_KMH, SPEEDSCALE_MPH },
				new String[] { International.getString("km/h"), International.getString("mph") }, IItemType.TYPE_PUBLIC,
				"", International.getString("Windgeschwindigkeit-Skala")));
		item.setFieldGrid(WEATHERWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);

		addParameterInternal(locationList = new ItemTypeItemList(PARAM_WEATHER_LOCATIONLIST, new Vector<IItemType[]>(), this,
				IItemType.TYPE_PUBLIC, "",
				International.getString("Wetter-Orte"))).setFieldGrid(WEATHERWIDGET_GRIDWIDTH-1);
	    locationList.setShortDescription(International.getString("Wetter-Orte"));		
		locationList.setRepeatTitle(true);
		locationList.setShowUpDownButtons(true);
		locationList.setXForAddDelButtons(WEATHERWIDGET_GRIDWIDTH-1);
		locationList.setStorageType(ItemTypeItemList.StorageType.keyvalue);//important flag: GUI items can change in order and elements, without breaking storage
		super.setEnabled(true);
		super.setPosition(IWidget.POSITION_MULTIWIDGET);
		
		// we have a special layout here. 
		//so we extend the fields for position and interval optically
		
		item = this.getParameterInternal(PARAM_ENABLED);
		item.setFieldGrid(WEATHERWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
		item = this.getParameterInternal(PARAM_POSITION);
		item.setFieldGrid(WEATHERWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
		item = this.getParameterInternal(PARAM_UPDATEINTERVAL);
		item.setFieldGrid(WEATHERWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
		
	}

	

	/*
	 * getDefaultItems is the factory for all location elements
	 */
	public IItemType[] getDefaultItems(String itemName) {
		//which Item do we want to get the elements from?
		if (itemName.endsWith(PARAM_WEATHER_LOCATIONLIST)) {
			
            ItemTypeItemList item = (ItemTypeItemList)getParameterInternal(PARAM_WEATHER_LOCATIONLIST);
            int i = item.size()+1;
			
            // build the GUI
            ItemTypeLabelTextfield curItem; 
            IItemType[] items = new IItemType[9];
            i=0;
            items[i] = new ItemTypeString(PARAM_CAPTION, "Berlin", IItemType.TYPE_PUBLIC, "",
            				International.getString("Beschriftung"));
            items[i].setFieldGrid(WEATHERWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            items[i++].setPadding(0,0,0,2);

            items[i] = new ItemTypeLongLat(PARAM_LATITUDE, ItemTypeLongLat.ORIENTATION_NORTH, 52, 25, 9,
            				IItemType.TYPE_PUBLIC, "", International.getString("geographische Breite"));
            items[i].setFieldGrid(-1, -1, GridBagConstraints.HORIZONTAL);
            items[i++].setFieldSize(SMALL_FIELDWIDTH, -1);

            
            curItem = new ItemTypeLongLat(PARAM_LONGITUDE, ItemTypeLongLat.ORIENTATION_EAST, 13, 10, 15,
            				IItemType.TYPE_PUBLIC, "", International.getString("geographische Länge"));
            curItem.setIsItemOnSameRowAsPreviousItem(true);
            curItem.setFieldSize(SMALL_FIELDWIDTH, -1);
            curItem.setFieldGrid(-1, -1, GridBagConstraints.HORIZONTAL);
            items[i++] = curItem;
           
            items[i] = new ItemTypeStringList(PARAM_WEATHER_LAYOUT, WEATHER_LAYOUT_CURRENT_UVINDEX,
            				new String[] { WEATHER_LAYOUT_CURRENT_CLASSIC, WEATHER_LAYOUT_CURRENT_WIND, WEATHER_LAYOUT_CURRENT_UVINDEX, WEATHER_LAYOUT_FORECASTSIMPLE, WEATHER_LAYOUT_FORECASTCOMPLEX },
            				new String[] { International.getString("Aktuelles Wetter (Klassisch)"), 
            						International.getString("Aktuelles Wetter (Wind)"), 
            						International.getString("Aktuelles Wetter (UV-Index)"), 
            						International.getString("Vorhersage (Einfach)"),
            						International.getString("Vorhersage (Komplex)") },
            				IItemType.TYPE_PUBLIC, "", International.getString("Layout"));
            items[i].setFieldGrid(WEATHERWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            items[i++].setPadding(0, 0, 20, 0);

            items[i++] = EfaGuiUtils.createDescription("WidgetMeteoHTMLPOPUP",IItemType.TYPE_PUBLIC, "", 
            		International.getString("Bei Mausklick auf das Astro/Meteo-Widget kann eine HMTL-Seite angezeigt werden."), WEATHERWIDGET_GRIDWIDTH-1,20,3);
                    
            curItem = new ItemTypeFile(PARAM_HTMLPOPUPURL, "",
                            International.getString("HTML-Seite"),
                            International.getString("HTML-Seite"),
                            null,ItemTypeFile.MODE_OPEN,ItemTypeFile.TYPE_FILE,
                            IItemType.TYPE_PUBLIC, "",
                            International.getString("HTML-Seite"));
            curItem.setFieldGrid(WEATHERWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            items[i++]=curItem;
            
            curItem = new ItemTypeInteger(PARAM_HTMLPOPWIDTH, 400, 1, Integer.MAX_VALUE, false,
                            IItemType.TYPE_PUBLIC, "",
                            International.getString("Breite"));
            
            curItem.setFieldSize(130, -1);
            items[i++] = curItem;
            
            curItem= new ItemTypeInteger(PARAM_HTMLPOPHEIGHT, 200, 1, Integer.MAX_VALUE, false,
                            IItemType.TYPE_PUBLIC, "",
                            International.getString("Höhe"));
            curItem.setIsItemOnSameRowAsPreviousItem(true);
            curItem.setFieldSize(130, -1);
            items[i++] = curItem;

            items[i] = new ItemTypeString(PARAM_POPUPEXECCOMMAND, "",
                            IItemType.TYPE_PUBLIC, "",
                            International.getMessage("Auszuführendes Kommando vor {event}",
                            International.getString("Popup")));
            items[i].setFieldGrid(WEATHERWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            return items;
		}
		return null;
	}	
	
    ItemTypeItemList getLocationList() {
        return (ItemTypeItemList)getParameterInternal(PARAM_WEATHER_LOCATIONLIST);
    }	
	

	@Override
    public Vector <WidgetInstance> createInstances(){
		
		//initialize Weather Data Cache for current widget
		WeatherDataCache.getInstance().setSpeedScale(getWeatherSpeedScale());
		WeatherDataCache.getInstance().setTempScale(getWeatherTempScale());
		WeatherDataCache.getInstance().setUpdateIntervalSeconds(getUpdateInterval());
		
		//now initialize Instances
		Vector <WidgetInstance> returnList = new Vector <WidgetInstance>();
		
		ItemTypeItemList myWList=this.getLocationList();
		if (myWList==null) {
			return returnList;
		}
		
		for (int i = 0; i < myWList.size(); i++) {
			WeatherWidgetInstance wwi = new WeatherWidgetInstance();

			wwi.setUpdateInterval(this.getUpdateInterval());
			wwi.setSource(this.getWeatherSource());
			wwi.setSpeedScale(this.getWeatherSpeedScale());
			wwi.setTempScale(this.getWeatherTempScale());

			wwi.setCaption(this.getWeatherCaption(myWList,i));
			wwi.setLatitude(this.getWeatherLatitude(myWList,i));
			wwi.setLayout(this.getWeatherLayout(myWList,i));
			wwi.setLongitude(this.getWeatherLongitude(myWList,i));
	
			wwi.setHtmlPopupHeight(this.getHtmlPopupHeight(myWList,i));
			wwi.setHtmlPopupURL(this.getHtmlPopupUrl(myWList,i));
			wwi.setHtmlPopupWidth(this.getHtmlPopupWidth(myWList,i));
			wwi.setPopupExecCommand(this.getPopupExecCommand(myWList,i));
		
			returnList.add(wwi);
		}
		
		return returnList;
	};    
    

	public String getWeatherCaption(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeString)list.getItem(i, PARAM_CAPTION)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return "";
        }
	}

	private String getWeatherLongitude(ItemTypeItemList list, int i) {
		return getLongLatTogether(list, i).getLongitude()+"";
	}

	private uk.me.jstott.coordconv.LatitudeLongitude getLongLatTogether(ItemTypeItemList list, int index){

		ItemTypeLongLat efa_longi;
		ItemTypeLongLat efa_lati;
		
        try {
        	efa_longi = (ItemTypeLongLat)list.getItem(index, PARAM_LONGITUDE);
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }

        try {
        	efa_lati = (ItemTypeLongLat)list.getItem(index, PARAM_LATITUDE);
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }

        int lat = uk.me.jstott.coordconv.LatitudeLongitude.NORTH;
        int lon = uk.me.jstott.coordconv.LatitudeLongitude.EAST;
        switch (efa_lati.getValueOrientation()) {
            case ItemTypeLongLat.ORIENTATION_NORTH:
                lat = uk.me.jstott.coordconv.LatitudeLongitude.NORTH;
                break;
            case ItemTypeLongLat.ORIENTATION_SOUTH:
                lat = uk.me.jstott.coordconv.LatitudeLongitude.SOUTH;
                break;
        }
        switch (efa_longi.getValueOrientation()) {
            case ItemTypeLongLat.ORIENTATION_WEST:
                lon = uk.me.jstott.coordconv.LatitudeLongitude.WEST;
                break;
            case ItemTypeLongLat.ORIENTATION_EAST:
                lon = uk.me.jstott.coordconv.LatitudeLongitude.EAST;
                break;
        }

        uk.me.jstott.coordconv.LatitudeLongitude ll =
                new uk.me.jstott.coordconv.LatitudeLongitude(lat,
                efa_lati.getValueCoordinates()[0],
                efa_lati.getValueCoordinates()[1],
                efa_lati.getValueCoordinates()[2],
                lon,
                efa_longi.getValueCoordinates()[0],
                efa_longi.getValueCoordinates()[1],
                efa_longi.getValueCoordinates()[2]);
		
		return ll;
	}

	private String getWeatherLatitude(ItemTypeItemList list, int i) {
		return getLongLatTogether(list, i).getLatitude()+"";
	}

	private String getWeatherLayout(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeStringList)list.getItem(i, PARAM_WEATHER_LAYOUT)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
        	return "";
        }
	}

	private String getWeatherSource() {
		return ((ItemTypeStringList) getParameterInternal(PARAM_WEATHER_SOURCE)).toString();
	}

	public String getWeatherTempScale() {
		return ((ItemTypeStringList) getParameterInternal(PARAM_TEMPERATURESCALE)).toString();
	}

	public String getWeatherSpeedScale() {
		return ((ItemTypeStringList) getParameterInternal(PARAM_SPEEDSCALE)).toString();
	}

    public String getPopupExecCommand(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeString)list.getItem(i, PARAM_POPUPEXECCOMMAND)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return "";
        }
    }

    public String getHtmlPopupUrl(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeString)list.getItem(i, PARAM_HTMLPOPUPURL)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return "";
        }    
    }

    public int getHtmlPopupWidth(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeInteger)list.getItem(i, PARAM_HTMLPOPWIDTH)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return 100;
        }
    }

    public int getHtmlPopupHeight(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeInteger)list.getItem(i, PARAM_HTMLPOPHEIGHT)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return 100;
        }        
    }
	

	

}
