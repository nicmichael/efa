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

import java.awt.Color;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeColor;
import de.nmichael.efa.core.items.ItemTypeDouble;
import de.nmichael.efa.core.items.ItemTypeFile;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class HTMLWidget extends Widget implements IWidget, IItemFactory {

    public static final String PARAM_WIDTH          = "Width";
    public static final String PARAM_HEIGHT         = "Height";
    public static final String PARAM_SCALE          = "Scale";
    public static final String PARAM_URL            = "Url";
    public static final String PARAM_USE_HTTP_CACHE = "UseHttpCache";
    public static final String HEADER_FIRST_HTML	= "FirstHTMLHeader";
    
    public static final String PARAM_COLORSACTIVE   = "ColorsActive";
    public static final String PARAM_COLORBACKGROUND= "BackgroundColor";
    public static final String PARAM_COLORFORECROUND= "ForegroundColor";
    
	private static final String PARAM_HTML_PAGELIST = "MultiHtmlPageList";
	private ItemTypeItemList htmlPageList;

    
    public HTMLWidget() {
        super("Html", International.getString("HTML-Widget"), true, true);

        /* This is for backward compatibility.
         * The former HTML widget just supported a single HTML page to be shown.
         * The new one supports multiple HTML pages. If we just stuck with the new parameter set
         * based on multiple HTML Pages, the configuration data from old efa versions would be lost.
         * So... we support a very first html page, and multiple add-ons.
         */
        
        addHeader(HEADER_FIRST_HTML, IItemType.TYPE_PUBLIC,"", International.getString("Erste HTML-Seite"), 3);
        
        addParameterInternal(new ItemTypeInteger(PARAM_WIDTH, 200, 1, Integer.MAX_VALUE, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Breite")));

        addParameterInternal(new ItemTypeInteger(PARAM_HEIGHT, 50, 1, Integer.MAX_VALUE, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Höhe")));

        addParameterInternal(new ItemTypeDouble(PARAM_SCALE, 1, 0.1, 10, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Skalierung")));

        addParameterInternal(new ItemTypeFile(PARAM_URL, "",
                International.getString("HTML-Seite"),
                International.getString("HTML-Seite"),
                null,ItemTypeFile.MODE_OPEN,ItemTypeFile.TYPE_FILE,
                IItemType.TYPE_PUBLIC, "",
                "URL"));
        
        addParameterInternal(new ItemTypeBoolean(PARAM_USE_HTTP_CACHE, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("HTTP-Caching verwenden")));
        
        // Support multiple HTML Pages
		addParameterInternal(htmlPageList = new ItemTypeItemList(PARAM_HTML_PAGELIST, new Vector<IItemType[]>(), this,
				IItemType.TYPE_PUBLIC, "",	
				International.getString("Weitere HTML-Seiten")));
		htmlPageList.setShortDescription(International.getString("Weitere HTML-Seite"));		
		htmlPageList.setRepeatTitle(true);
    }

    public void setSize(int width, int height) {
        ((ItemTypeInteger)getParameterInternal(PARAM_WIDTH)).setValue(width);
        ((ItemTypeInteger)getParameterInternal(PARAM_HEIGHT)).setValue(height);
    }

    public int getWidth() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WIDTH)).getValue();
    }

    public int getHeight() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_HEIGHT)).getValue();
    }
    
    public double getScale() {
    	IItemType iscale = getParameterInternal(PARAM_SCALE);
        final double scale = (iscale != null ? ((ItemTypeDouble)iscale).getValue() : 1.0);
        return scale;
    }

    public String getUrl() {
        return ((ItemTypeFile)getParameterInternal(PARAM_URL)).getValue(); 	
    }
    
    public int getWidth(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeInteger)list.getItem(i, PARAM_WIDTH)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return 50;
        }
    }

    public int getHeight(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeInteger)list.getItem(i, PARAM_HEIGHT)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return 50;
        }
    }
    
    public double getScale(ItemTypeItemList list, int i) {
        try {
        	IItemType iscale = ((IItemType)list.getItem(i, PARAM_SCALE));
            final double scale = (iscale != null ? ((ItemTypeDouble)iscale).getValue() : 1.0);
            return scale;
        } catch(Exception e) {
            Logger.logdebug(e);
            return 1.0;
        }    	
    }

    public String getUrl(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeString)list.getItem(i, PARAM_URL)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return "";
        }	
    }

	@Override
	public Vector<WidgetInstance> createInstances() {
		Vector <WidgetInstance> returnList = new Vector <WidgetInstance>();
		// first html page
		HTMLWidgetInstance wi = new HTMLWidgetInstance();
		wi.setHeight(getHeight());
		wi.setScale(getScale());
		wi.setUpdateInterval(getUpdateInterval());
		wi.setUrl(this.getUrl());
		wi.setWidth(getWidth());
		wi.setColorsActive(false);;
		wi.setUseHttpCaching(getHttpCacheActive());
		returnList.add(wi);
		
		//add all other html pages
		
		ItemTypeItemList myWList=this.getHtmlPageList();
		if (myWList==null) {
			return returnList;
		}		
		
		for (int i = 0; i < myWList.size(); i++) {

			wi = new HTMLWidgetInstance();
			wi.setHeight(getHeight(myWList,i));
			wi.setScale(getScale(myWList,i));
			wi.setUpdateInterval(getUpdateInterval());
			wi.setUrl(this.getUrl(myWList,i));
			wi.setWidth(getWidth(myWList,i));
			wi.setColorsActive(this.getColorsActive(myWList,i));
			wi.setBackgroundColor(this.getBackgroundColor(myWList, i));
			wi.setForegroundColor(this.getForegroundColor(myWList, i));
			wi.setUseHttpCaching(this.getHttpCacheActive(myWList, i));
			returnList.add(wi);
		}
		
		return returnList;
	}
    
	private Color getBackgroundColor(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeColor)list.getItem(i, PARAM_COLORBACKGROUND)).getColor();
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }	
	}

	private Color getForegroundColor(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeColor)list.getItem(i, PARAM_COLORFORECROUND)).getColor();
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }	
	}

	private boolean getColorsActive(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeBoolean)list.getItem(i, PARAM_COLORSACTIVE)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }	
	}

    public Boolean getHttpCacheActive() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_USE_HTTP_CACHE)).getValue(); 	
    }
    
	private boolean getHttpCacheActive(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeBoolean)list.getItem(i, PARAM_USE_HTTP_CACHE)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }	
	}

	/*
	 * getDefaultItems is the factory for all location elements
	 */
	public IItemType[] getDefaultItems(String itemName) {
		//which Item do we want to get the elements from?
		if (itemName.endsWith(PARAM_HTML_PAGELIST)) {
			
            ItemTypeItemList item = (ItemTypeItemList)getParameterInternal(PARAM_HTML_PAGELIST);
            int i = item.size()+1;
			
            // build the GUI
           
            IItemType[] items = new IItemType[8];
            i=0;
            items[i] = new ItemTypeInteger(PARAM_WIDTH, 200, 1, Integer.MAX_VALUE, false,
                    IItemType.TYPE_PUBLIC, "", International.getString("Breite"));
            items[i++].setPadding(10,0,0,0);

            items[i++] = new ItemTypeInteger(PARAM_HEIGHT, 50, 1, Integer.MAX_VALUE, false,
                    IItemType.TYPE_PUBLIC, "", International.getString("Höhe"));
            items[i++] = new ItemTypeDouble(PARAM_SCALE, 1, 0.1, 10, false,
                    IItemType.TYPE_PUBLIC, "", International.getString("Skalierung"));

            items[i++] = new ItemTypeBoolean(PARAM_COLORSACTIVE, true,
					IItemType.TYPE_PUBLIC, "",
					International.getString("Standard-Farben definieren"));;
					
			items[i++] = new ItemTypeColor(PARAM_COLORBACKGROUND,
							EfaUtil.getColor(Daten.efaConfig.getToolTipBackgroundColor()),
							EfaUtil.getColor(Daten.efaConfig.getToolTipBackgroundColor()), IItemType.TYPE_PUBLIC,
							"",
							International.getString("Hintergrundfarbe"), false);
			items[i++] =  new ItemTypeColor(PARAM_COLORFORECROUND,
					EfaUtil.getColor(Daten.efaConfig.getToolTipForegroundColor()),
					EfaUtil.getColor(Daten.efaConfig.getToolTipForegroundColor()), IItemType.TYPE_PUBLIC,
							"",
							International.getString("Textfarbe"), false);							

            items[i++] = new ItemTypeFile(PARAM_URL, "",
                    International.getString("HTML-Seite"),
                    International.getString("HTML-Seite"),
                    null,ItemTypeFile.MODE_OPEN,ItemTypeFile.TYPE_FILE,
                    IItemType.TYPE_PUBLIC, "",
                    "URL");

            items[i++] = new ItemTypeBoolean(PARAM_USE_HTTP_CACHE, true,
                    IItemType.TYPE_PUBLIC, "",
                    International.getString("HTTP-Caching verwenden"));
            
            return items;
		}
		return null;
	}	
	
    ItemTypeItemList getHtmlPageList() {
        return (ItemTypeItemList)getParameterInternal(PARAM_HTML_PAGELIST);
    }	
       

}
