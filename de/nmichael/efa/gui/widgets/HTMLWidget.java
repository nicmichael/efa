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
import java.awt.GridBagConstraints;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeColor;
import de.nmichael.efa.core.items.ItemTypeDouble;
import de.nmichael.efa.core.items.ItemTypeFile;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeLabelTextfield;
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
    public static final String HEADER_FIRST_HTML	= NOT_STORED_ITEM_PREFIX+"FirstHTMLHeader";
    
    public static final String PARAM_CAPTION		= "Caption";
    
    public static final String PARAM_COLORSACTIVE   = "ColorsActive";
    public static final String PARAM_COLORBACKGROUND= "BackgroundColor";
    public static final String PARAM_COLORFORECROUND= "ForegroundColor";
    public static final String PARAM_HEADER_COLOR_BACKGROUND = "HeaderBackgroundColor";
    public static final String PARAM_HEADER_COLOR_FOREGROUND = "HeaderForegroundColor";
    public static final String PARAM_HTMLPAGE_VISIBLE = "HTMLPageVisible";
    
	private static final String PARAM_HTML_PAGELIST = "MultiHtmlPageList";
	private static final int HTMLWIDGET_GRIDWIDTH = 6;
	private static final int SMALL_FIELDWIDTH = 110;
	
	private ItemTypeItemList htmlPageList;

    
    public HTMLWidget() {
        super("Html", International.getString("HTML-Widget"), true, true,HTMLWIDGET_GRIDWIDTH);

        IItemType item;

        //we break backward compatibility with the former html widget.
        //so the config data of the old html widget is no longer used,
        //and thus when updating from efa 240 to 251, the old html page is no longer shown. 
        
        addParameterInternal(htmlPageList = new ItemTypeItemList(PARAM_HTML_PAGELIST, new Vector<IItemType[]>(), this,
				IItemType.TYPE_PUBLIC, "",	
				International.getString("HTML-Seiten")));
		htmlPageList.setPadding(0, 0, 10, 10);
		htmlPageList.setShortDescription(International.getString("HTML-Seite"));		
		htmlPageList.setRepeatTitle(true);
		htmlPageList.setShowUpDownButtons(true);
		htmlPageList.setXForAddDelButtons(HTMLWIDGET_GRIDWIDTH-1);
		htmlPageList.setStorageType(ItemTypeItemList.StorageType.keyvalue);//important flag: GUI items can change in order and elements, without breaking storage
		super.setPosition(IWidget.POSITION_MULTIWIDGET);
		
		item = this.getParameterInternal(PARAM_ENABLED);
		item.setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
		item = this.getParameterInternal(PARAM_POSITION);
		item.setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
		item = this.getParameterInternal(PARAM_UPDATEINTERVAL);
		item.setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
    }

   
    public String getCaption(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeString)list.getItem(i, PARAM_CAPTION)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return "";
        }
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

    public Boolean getHTMLPageVisible(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeBoolean)list.getItem(i, PARAM_HTMLPAGE_VISIBLE)).getValue();
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
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
		
		ItemTypeItemList myWList=this.getHtmlPageList();
		if (myWList==null) {
			return returnList;
		}		
		
		for (int i = 0; i < myWList.size(); i++) {

			if (getHTMLPageVisible(myWList,i)) {
				HTMLWidgetInstance wi = new HTMLWidgetInstance();
				wi.setHeight(getHeight(myWList,i));
				wi.setScale(getScale(myWList,i));
				wi.setUpdateInterval(getUpdateInterval());
				wi.setUrl(this.getUrl(myWList,i));
				wi.setWidth(getWidth(myWList,i));
				wi.setColorsActive(this.getColorsActive(myWList,i));
				wi.setBackgroundColor(this.getBackgroundColor(myWList, i));
				wi.setForegroundColor(this.getForegroundColor(myWList, i));
				wi.setHeaderBackgroundColor(this.getHeaderBackgroundColor(myWList,i));
				wi.setHeaderForegroundColor(this.getHeaderForegroundColor(myWList,i));
				wi.setUseHttpCaching(this.getHttpCacheActive(myWList, i));
				wi.setCaption(this.getCaption(myWList, i));
				returnList.add(wi);
			}
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

	private Color getHeaderBackgroundColor(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeColor)list.getItem(i, PARAM_HEADER_COLOR_BACKGROUND)).getColor();
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }	
	}

	private Color getHeaderForegroundColor(ItemTypeItemList list, int i) {
        try {
            return ((ItemTypeColor)list.getItem(i, PARAM_HEADER_COLOR_FOREGROUND)).getColor();
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
           
            IItemType[] items = new IItemType[12];
            i=0;
            
            
            items[i] = new ItemTypeBoolean(PARAM_HTMLPAGE_VISIBLE, true,
                    IItemType.TYPE_PUBLIC, "",
                    International.getString("HTML-Seite anzeigen"));
            items[i].setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            items[i++].setPadding(0,0,0,10); 
            
    		items[i] = new ItemTypeString(PARAM_CAPTION, "", IItemType.TYPE_PUBLIC, "",
    				International.getString("Beschriftung"));
            items[i++].setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            
            items[i] = new ItemTypeFile(PARAM_URL, "",
                    International.getString("HTML-Seite"),
                    International.getString("HTML-Seite"),
                    null,ItemTypeFile.MODE_OPEN,ItemTypeFile.TYPE_FILE,
                    IItemType.TYPE_PUBLIC, "",
                    "URL");
            items[i++].setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            
            items[i] = new ItemTypeBoolean(PARAM_USE_HTTP_CACHE, true,
                    IItemType.TYPE_PUBLIC, "",
                    International.getString("HTTP-Caching verwenden"));
            items[i].setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
    		((ItemTypeBoolean) items[i]).setIndent(true);
    		items[i++].setPadding(0,0,0,10);    

            items[i] = new ItemTypeInteger(PARAM_WIDTH, 200, 1, Integer.MAX_VALUE, false,
                    IItemType.TYPE_PUBLIC, "", International.getString("Breite"));
            items[i].setFieldSize(SMALL_FIELDWIDTH, -1);
            items[i].setFieldGrid(-1, -1, GridBagConstraints.HORIZONTAL);
            items[i++].setPadding(10,0,0,0);

            items[i] = new ItemTypeInteger(PARAM_HEIGHT, 50, 1, Integer.MAX_VALUE, false,
                    IItemType.TYPE_PUBLIC, "", International.getString("Höhe"));
            ((ItemTypeLabelTextfield) items[i]).setIsItemOnSameRowAsPreviousItem(true);
            items[i].setFieldGrid(-1, -1, GridBagConstraints.HORIZONTAL);
            items[i++].setFieldSize(SMALL_FIELDWIDTH, -1);
            
            items[i] = new ItemTypeDouble(PARAM_SCALE, 1, 0.1, 10, false,
                    IItemType.TYPE_PUBLIC, "", International.getString("Skalierung"));
            items[i++].setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            
            items[i] = new ItemTypeBoolean(PARAM_COLORSACTIVE, true,
					IItemType.TYPE_PUBLIC, "",
					International.getString("Eigene Farben verwenden"));
            items[i++].setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            
			items[i] = new ItemTypeColor(PARAM_COLORBACKGROUND,
							EfaUtil.getColor(Daten.efaConfig.getToolTipBackgroundColor()),
							EfaUtil.getColor(Daten.efaConfig.getToolTipBackgroundColor()), IItemType.TYPE_PUBLIC,
							"",
							International.getString("Hintergrundfarbe"), false);
            items[i++].setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            
            items[i] =  new ItemTypeColor(PARAM_COLORFORECROUND,
					EfaUtil.getColor(Daten.efaConfig.getToolTipForegroundColor()),
					EfaUtil.getColor(Daten.efaConfig.getToolTipForegroundColor()), IItemType.TYPE_PUBLIC,
							"",
							International.getString("Textfarbe"), false);							
            items[i++].setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            
			items[i] = new ItemTypeColor(PARAM_HEADER_COLOR_BACKGROUND,
					EfaUtil.getColor(EfaConfig.standardToolTipHeaderBackgroundColor),
					EfaUtil.getColor(EfaConfig.standardToolTipHeaderBackgroundColor), IItemType.TYPE_PUBLIC,
					"",
					International.getString("Überschriften Hintergrundfarbe"), false);
            items[i++].setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            
			items[i] =new ItemTypeColor(PARAM_HEADER_COLOR_FOREGROUND,
					EfaUtil.getColor(EfaConfig.standardToolTipHeaderForegroundColor),
					EfaUtil.getColor(EfaConfig.standardToolTipHeaderForegroundColor), IItemType.TYPE_PUBLIC,
					"",
					International.getString("Überschriften Textfarbe"), false);
            items[i++].setFieldGrid(HTMLWIDGET_GRIDWIDTH-2, -1, GridBagConstraints.HORIZONTAL);
            
            return items;
		}
		return null;
	}	
	
    ItemTypeItemList getHtmlPageList() {
        return (ItemTypeItemList)getParameterInternal(PARAM_HTML_PAGELIST);
    }	
       

}
