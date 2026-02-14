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

public interface IWidget {

    public static final String POSITION_TOP = "TOP";
    public static final String POSITION_BOTTOM = "BOTTOM";
    public static final String POSITION_LEFT = "LEFT";
    public static final String POSITION_RIGHT = "RIGHT";
    public static final String POSITION_CENTER = "CENTER";
    public static final String POSITION_MULTIWIDGET = "MULTIWIDGET";

    public String getName();
    public String getDescription();
    public String getParameterName(String internalName);
    public IItemType[] getParameters();
    public void setParameter(IItemType param);
    public void setEnabled(boolean enabled);
    public boolean isEnabled();
    public void setPosition(String p);
    public String getPosition();
    public void setUpdateInterval(int seconds);
    public int getUpdateInterval();
    public boolean isGuiWidget();

    public Vector <WidgetInstance> createInstances();
    
}
