/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete

public class EfaMouseListener extends MouseAdapter {

    public static final String EVENT_MOUSECLICKED_1x = "efa_mouseClicked1x";
    public static final String EVENT_MOUSECLICKED_2x = "efa_mouseClicked2x";
    public static final String EVENT_POPUP           = "efa_Popup";
    public static final String EVENT_BUILD_POPUP_MENU= "efa_BuildPopupMenu";
    public static final String EVENT_POPUP_CLICKED   = "efa_PopupClicked";

    private Component myComponent;
    private JPopupMenu popup;
    private ActionListener actionListener;
    private boolean showPopupOnLeftMouseClick;
    private boolean popupsEnabled = true;

    public EfaMouseListener(Component myComponent, JPopupMenu popupMenu, ActionListener actionListener, boolean showPopupOnLeftMouseClick) {
        this.myComponent = myComponent;
        this.popup = popupMenu;
        this.actionListener = actionListener;
        this.showPopupOnLeftMouseClick = showPopupOnLeftMouseClick;
    }

    public boolean isPopupsEnabled() {
        return popupsEnabled;
    }

    public void setPopupsEnabled(boolean enabled) {
        popupsEnabled = enabled;
    }

    private void showPopup(MouseEvent e) {
        if (popupsEnabled) {

            // if this is a JList, check whether there are any items currently selected
            try {
                JList list = (JList)myComponent;
                if (list.getSelectedIndex() < 0) {
                    return; // don't show popup on empty list or list without any selected items
                }
            } catch(Exception eignore0) {
            }

            popup.show(e.getComponent(), e.getX() + 10, e.getY() + 5);
            myComponent.requestFocus();
        }
    }

    private void selectListItem(MouseEvent e) {
        // if this is a JList, try to select the item that was right-clicked on
        try {
            JList list = (JList)myComponent;
            list.requestFocus();
            list.setSelectedIndex(list.locationToIndex(new Point(e.getX(), e.getY())));
        } catch(Exception eignore0) {
        }
    }

    public void mousePressed(MouseEvent e) {
        selectListItem(e);
        maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
        selectListItem(e);
        if (e != null && e.getButton() == 1) {
            if (e.getClickCount() == 1) {
                if (showPopupOnLeftMouseClick) {
                    if (actionListener != null) {
                        actionListener.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, EVENT_BUILD_POPUP_MENU));
                    }                 	
                    showPopup(e);
                }
                actionListener.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, EVENT_MOUSECLICKED_1x));
            }
            if (e.getClickCount() == 2) {
                actionListener.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, EVENT_MOUSECLICKED_2x));
            }
        } else {
            maybeShowPopup(e);
        }
    }

    private void maybeShowPopup(MouseEvent e) {
        try {
            if (popupsEnabled && e.isPopupTrigger()) {
                if (actionListener != null) {
                    actionListener.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, EVENT_BUILD_POPUP_MENU));
                } 
                showPopup(e);
                if (actionListener != null) {
                    actionListener.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, EVENT_POPUP));
                }                 
            }
        } catch(Exception eignore) {
        }
    }
    
    public void setPopupMenu(JPopupMenu value) {
    	popup = value;
    }
}
