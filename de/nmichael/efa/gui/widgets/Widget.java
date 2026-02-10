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

import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.util.*;
import java.util.*;
import java.awt.*;

import javax.swing.*;


public abstract class Widget implements IWidget {

    public static final String PARAM_ENABLED        = "Enabled";
    public static final String PARAM_POSITION       = "Position";
    public static final String PARAM_UPDATEINTERVAL = "UpdateInterval";
    public static final String NOT_STORED_ITEM_PREFIX ="_";
    private static Color hintBackgroundColor= new Color(171,206,241);
    
    String name;
    String description;
    boolean ongui;
    Vector<IItemType> parameters = new Vector<IItemType>();
    JPanel myPanel;
    
    public Widget(String name, String description, boolean ongui) {
        this.name = name;
        this.description = description;
        this.ongui = ongui;

        
        addHeader("WidgetCommon_"+name,IItemType.TYPE_PUBLIC, "", International.getString("Widget Allgemein"), 3);
        addParameterInternal(new ItemTypeBoolean(PARAM_ENABLED, false,
                IItemType.TYPE_PUBLIC, "",
                (ongui ?
                    International.getMessage("{item} anzeigen", name) :
                    International.getMessage("{item} aktivieren", description))));

        if (ongui) {
            addParameterInternal(new ItemTypeStringList(PARAM_POSITION, POSITION_BOTTOM,
                    new String[]{POSITION_TOP, POSITION_BOTTOM, POSITION_LEFT, POSITION_RIGHT, POSITION_CENTER},
                    new String[]{International.getString("oben"),
                        International.getString("unten"),
                        International.getString("links"),
                        International.getString("rechts"),
                        International.getString("mitte")
                    },
                    IItemType.TYPE_PUBLIC, "",
                    International.getString("Position")));

            addParameterInternal(new ItemTypeInteger(PARAM_UPDATEINTERVAL, 3600, 1, Integer.MAX_VALUE, false,
                    IItemType.TYPE_PUBLIC, "",
                    International.getString("Aktualisierungsintervall")
                    + " (s)"));
        }
    }

    public String getParameterName(String internalName) {
        return "Widget" + this.name + internalName;
    }

    void addParameterInternal(IItemType p) {
        p.setName(getParameterName(p.getName()));
        parameters.add(p);
    }
    
    /**
     * Adds a header item in an efa widget config. This header value is not safed within efaConfig.
     * There is no word-wrap for the caption.
     * 
     * The header automatically gets a blue background and white text color; this cannot be configured
     * as efaConfig cannot refer to its own settings when calling the constructor.
     * 
     * @param uniqueName Unique name of the element (as for all of efaConfig elements need unique names)
     * @param type TYPE_PUBLIC, TYPE_EXPERT, TYPE_INTERNAL
     * @param category Category in which the header is placed
     * @param caption Caption
     * @param gridWidth How many GridBagLayout cells shall this header be placed in?
     */
    protected IItemType addHeader(String uniqueName, int type, String category, String caption, int gridWidth) {
    	IItemType item = EfaGuiUtils.createHeader(uniqueName, type, category, caption, gridWidth);
        addParameterInternal(item);
        return item;
    }

    /**
     * Adds a description item in an efa widget config. This description value is not safed within efaConfig.
     * There is no word-wrap for the caption.
     * 
     * This is similar to @see addHeader(), but the element does not get a highlighted background.
     * 
     * @param uniqueName Unique name of the element (as for all of efaConfig elements need unique names)
     * @param type TYPE_PUBLIC, TYPE_EXPERT, TYPE_INTERNAL
     * @param category Category in which the description is placed
     * @param caption Caption
     * @param gridWidth How many GridBagLayout cells shall this description be placed in?
     */      
    protected IItemType addDescription(String uniqueName, int type, String category, String caption, int gridWidth, int padBefore, int padAfter) {
    	IItemType item = EfaGuiUtils.createDescription(uniqueName, type, category, caption, gridWidth, padBefore, padAfter);
        addParameterInternal(item);
        return item;
    }
    
    protected IItemType addHint(String uniqueName, int type, String category, String caption, int gridWidth, int padBefore, int padAfter) {
    	IItemType item = EfaGuiUtils.createHint(uniqueName, type, category, caption, gridWidth, padBefore, padAfter);
        addParameterInternal(item);
		return item;
    }
    

    IItemType getParameterInternal(String internalName) {
        String name = getParameterName(internalName);
        for (int i=0; i<parameters.size(); i++) {
            if (parameters.get(i).getName().equals(name)) {
                return parameters.get(i);
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public IItemType[] getParameters() {
        IItemType[] a = new IItemType[parameters.size()];
        for (int i=0; i<parameters.size(); i++) {
            a[i] = parameters.get(i);
        }
        return a;
    }

    public void setParameter(IItemType param) {
        for (int i=0; i<parameters.size(); i++) {
            IItemType p = parameters.get(i);
            if (p.getName().equals(param.getName())) {
                p.parseValue(param.toString());
            }
        }
    }

    public void setEnabled(boolean enabled) {
        ((ItemTypeBoolean)getParameterInternal(PARAM_ENABLED)).setValue(enabled);
    }
    public boolean isEnabled() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_ENABLED)).getValue();
    }

    public void setPosition(String p) {
        try {
            getParameterInternal(PARAM_POSITION).parseValue(p);
        } catch(NullPointerException eignoremissingparameter) {
        }
    }

    public String getPosition() {
        try {
            return getParameterInternal(PARAM_POSITION).toString();
        } catch(NullPointerException eignoremissingparameter) {
            return null;
        }
    }

    public void setUpdateInterval(int seconds) {
        ((ItemTypeInteger)getParameterInternal(PARAM_UPDATEINTERVAL)).setValue(seconds);
    }

    public int getUpdateInterval() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_UPDATEINTERVAL)).getValue();
    }

    abstract void construct();
    public abstract JComponent getComponent();

    public void show(JPanel panel, int x, int y) {
        if (!ongui) {
            return;
        }
        myPanel = panel;
        construct();
        JComponent comp = getComponent();
        if (comp != null) {
            panel.add(comp, new GridBagConstraints(x, y, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        }
    }

    public void show(JPanel panel, String orientation) {
        if (!ongui) {
            return;
        }
        myPanel = panel;
        construct();
        JComponent comp = getComponent();
        if (comp != null) {
            panel.add(comp, orientation);
        }
    }

    public static String[] getAllWidgetClassNames() {
        return new String[] {
            HTMLWidget.class.getCanonicalName(),
            MeteoAstroWidget.class.getCanonicalName(),
            AlertWidget.class.getCanonicalName()
        };
    }

    public static Vector<IWidget> getAllWidgets() {
        String[] classNames = getAllWidgetClassNames();
        Vector<IWidget> widgets = new Vector<IWidget>();
        for (int i=0; i<classNames.length; i++) {
            try {
                IWidget w = (IWidget)Widget.class.forName(classNames[i]).newInstance();
                if (w != null) {
                    widgets.add(w);
                }
            } catch(Exception e) {
                Logger.logdebug(e);
            }
        }
        return widgets;
    }
}
