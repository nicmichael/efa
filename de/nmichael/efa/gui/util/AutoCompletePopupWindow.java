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

import de.nmichael.efa.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.Dialog.ModalExclusionType;
import java.awt.event.*;
import java.util.*;

// @i18n complete
/*
 * Documentation of AutoCompletePopupWindow
 * 
 * AutocompletePopupWindow extends a standard JWindow. It is stay on top and is an overlay to an existing windows.
 * It hides 10 seconds after it popped up, was updated or 10 seconds after the last mouse action of the user within the window.
 * 
 * AutoCompletePopupWindow is a singleton. So there is only one single Instance of AutoCompletePopupWindow
 * which remembers all AutoCompleteLists it handled, and also the timestamp of the last change (SCN) of the data behind the list.
 * 
 * AutoCompletePopupWindow does not get the focus. The focus stays within the ItemTypeStringAutocomplete field, 
 * which handles keyboard user interaction like arrow keys, pageup/pagedown, and filtering by typing. 
 * 
 * The 
 * 
 * Structure
 * JWindow
 * 	  ScrollPane   Standard 200x100pix height			(catches mouseclicks for handling show timeout)
 *       JList	   List Containing (filtered) items
 *       
 * 
 * 	
 * 
 */


public class AutoCompletePopupWindow extends JWindow {

    /**
	 * 
	 */
	private static final long serialVersionUID = -2544348417400727743L;
	private static AutoCompletePopupWindow window = null;
    private Hashtable<AutoCompleteList,String[]> autoCompleteLists = new Hashtable<AutoCompleteList,String[]>();
    private Hashtable<AutoCompleteList,Long> autoCompleteSCN = new Hashtable<AutoCompleteList,Long>();
    private JTextField showingAt;
    private JTextField lastShowingAt;
    private long lastShowingAtTime = 0;
    private HideWindowThread hideWindowThread;
    private AutoCompletePopupWindowCallback callback;
    BorderLayout borderLayout = new BorderLayout();
    JScrollPane scrollPane = new JScrollPane();
    JList list = new JList();

    private AutoCompletePopupWindow(Window parent) {
    	// we do not wand to call super(parent) here as then the list window gets the focus.
    	// we just want the list window to popup, but the parent window shall keep the focus,
    	// was the itemType field controls filtering data.
    	//super(parent);
        this.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        try {
            jbInit();
            setListSize(200, 100); //Preferred size: 200px width 100px height. Gets extended if TouchScreenSupport is active in efaConfig.
            if (Daten.efaConfig != null && Daten.efaConfig.getValueTouchScreenSupport()) {
                scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(25, 1));
            }
            scrollPane.setHorizontalScrollBar(null);
            // Bugfix: AutoCompletePopupWindow muß unter Windows und linux (!) ebenfalls alwaysOnTop sein, 
            // wenn EfaDirektFrame alwaysOnTop ist, da sonst die Popup-Liste nicht erscheint
            if (Daten.efaConfig != null && Daten.efaConfig.getValueEfaDirekt_immerImVordergrund()) {
            	de.nmichael.efa.java15.Java15.setAlwaysOnTop(this, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMouseListeners(JScrollBar scrollBar) {
        if (scrollBar == null) {
            return;
        }
        scrollBar.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                mousePressedEvent(e);
            }
        });
        // neben der Scrollbar selbst auch die Scrollbuttons (Pfeile) mit Listenern versorgen!
        Component[] c = scrollBar.getComponents();
        for (int i = 0; c != null && i < c.length; i++) {
            try {
                scrollBar.getComponent(i).addMouseListener(new java.awt.event.MouseAdapter() {

                    public void mousePressed(MouseEvent e) {
                        mousePressedEvent(e);
                    }
                });
            } catch (Exception e) {
            }
        }
    }

    private void jbInit() throws Exception {
        this.getContentPane().setLayout(borderLayout);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.getContentPane().add(scrollPane, BorderLayout.CENTER);
        scrollPane.getViewport().add(list, null);

        addMouseListeners(scrollPane.getHorizontalScrollBar());
        addMouseListeners(scrollPane.getVerticalScrollBar());
        list.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                listEntrySelected(e);
            }

            public void mousePressed(MouseEvent e) {
                mousePressedEvent(e);
            }
        });
    }

    /**
     * Sets width and height of the AutoCompletePopupWindow
     * @param width Width of the window.
     * @param height Height of the window. If touchscreensupport is on, the actual height is double the value of the given value.
     */
    public void setListSize(int width, int height) {
        if (Daten.efaConfig != null && Daten.efaConfig.getValueTouchScreenSupport()) {
            height *= 2;
        }
        this.scrollPane.setPreferredSize(new Dimension(width, height));
        this.pack();
    }

    /**
     * Sets a certain AutoCompleteList for displaying it's data.
     * The list gets updated, and if the list has a newer lastModified (SCN) timestamp, or has not yet been displayed,
     * the list is put into the Hashtable of known AutoCompleteLists.
     * 
     * However, the AutoCompleteList passed as parameter is shown anyway.
     * 
     * @param list AutoCompleteList to display
     * @return number of elements in the AutoCompleteList after update.
     */
    private int setListData(AutoCompleteList list) {
        list.update();
        String[] data = autoCompleteLists.get(list);
        Long scn = autoCompleteSCN.get(list);
        if (data == null || scn == null || scn.longValue() != list.getSCN()) {
            data = list.getData();
            autoCompleteLists.put(list, data);
            autoCompleteSCN.put(list, new Long(list.getSCN()));
        }
        this.list.setListData(list.getData());
        return data.length;
    }

    /**
     * Shows the single instance of AutoCompletePopupWindow at the position of a certain field.
     * 
     * @param field Textfield where the AutoCompletePopupWindow shall be displayed
     */
    private void showAtTextField(JTextField field) {
    	if (showingAt == field) {

        	//unter windows reicht aber, das fenster wieder sichtbar zu machen, damit es ganz oben angezeigt wird.
            //das machen wir auf jeden Fall, um ggfs. Probleme mit unterschiedlichen  Window Managern aus dem Weg zu gehen.
        	this.setVisible(true);
        	
        	// Unter Windows bewirkt toFront(), daß der ursprüngliche Frame den Fokus verliert, daher muß unter Windows darauf verzichtet werden
            if (!Daten.osName.startsWith("Windows")) {
                this.toFront();
            } 

            return;
        }

        if (lastShowingAt == field && System.currentTimeMillis() - lastShowingAtTime < 250) {
            // We've just been showing at this field; this might be a duplicate call, like
            // a user pressed the button (to minimize the autocomplete list), which caused a
            // focus lost event. The focus lost already minimized the list, so we shouldn't show
            // it again.
            return;
        }

        try {
            int x = (int) field.getLocationOnScreen().getX() + 10;
            int y = (int) field.getLocationOnScreen().getY() + field.getHeight();
            setListSize(field.getWidth()+10, field.getHeight() * 5);
            this.setLocation(x, y);
            this.setVisible(true);
            // Unter Windows bewirkt toFront(), daß der ursprüngliche Frame den Fokus verliert, daher muß unter Windows darauf verzichtet werden
            if (!Daten.osName.startsWith("Windows")) {
                this.toFront();
            }
            showingAt = field;
            lastShowingAt = showingAt;
        } catch (Exception ee) { // nur zur Sicherheit: Es gibt seltene Exceptions in efa, die keiner Stelle im Code zugeordnet werden können und hierher kommen könnten
        	Logger.logdebug(ee);
        }
    }

    /**
     * Hides the singleton AutoCompletePopupWindow instance.
     * Also, the field to which the AutoCompletePopupWindow is shown gets nulled.
     * 
     */
    public void doHide() {
        if (showingAt != null) {
            this.setVisible(false);
            lastShowingAt = showingAt;
            lastShowingAtTime = System.currentTimeMillis();
        }
        showingAt = null;
    }

    /**
     * Ensures that the AutoCompleteListItem representing the value entry is shown and is also scrolled into the visible part of the field.
     * @param entry
     */
    private void selectEintrag(String entry) {
        list.setSelectedValue(entry, true);
        try {
            list.scrollRectToVisible(list.getCellBounds(list.getSelectedIndex() - 1, list.getSelectedIndex() + 1));
        } catch (Exception e) {
        }
    }


    /**
     * Returns the currently selected entry in the popup window.
     * Needed for filtered autocomplete lists.
     * 
     * @return Value of the currently selected entry of the AutoCompleteList.
     */
    public  String getSelectedEintrag() {
    	return (String)list.getSelectedValue();
    }
    
    /**
     * MouseEvent handler which is fired when the user selects an entry within the list.
     * The value of the selected item is used as the new value of the corresponding JTextField.
     * 
     * Also, the AutoCompletePopupWindow gets hidden.
     * 
     * @param e
     */
    private void listEntrySelected(MouseEvent e) {
        if (showingAt != null) {
            try {
                String s = (String) list.getSelectedValue();
                if (s != null) {
                    showingAt.setText(s);
                }

                if (callback != null) {
                    callback.acpwCallback(showingAt);
                }
            } catch (Exception ee) {
            }
            doHide();
            try {
                Dialog.frameCurrent().toFront();
            } catch (Exception ee) {
            }
        }
    }

    private void mousePressedEvent(MouseEvent e) {
        try {
            if (hideWindowThread != null) {
                hideWindowThread.interrupt();
            }
        } catch (Exception ee) {
        }
    }

    /**
     * Shows the singleton AutoCompletePopupWindow at the given field, using the given autocomplete list and
     * automatically selects the entry in the list.
     * 
     * @param field		JTextField to show the AutoCompletePopupWindow at.
     * @param list		AutoCompleteList containing contents to show
     * @param selectedEntry  Value within the AutoCompleteList which shall be selected automatically   
     * @param callback Method which is called when the AutoCompletePopupWindow is hidden (this should induce that the callee takes over a given value as current value).
     */
    public static void showAndSelect(JTextField field, AutoCompleteList list, String selectedEntry, AutoCompletePopupWindowCallback callback) {
    	try {
            if (window == null) {
                window = new AutoCompletePopupWindow(Dialog.frameCurrent());
            }
            window.callback = callback;
            
            // we ignore the return value of setListData - even if there is nothing to show,
            // we show the popup window. An empty window provides the info that there is no matching item.
            // not showing up the popup window may also irritate the user.
            window.setListData(list);
            window.showAtTextField(field);
            window.selectEintrag(selectedEntry);
        } catch (Exception e) {
        	Logger.logdebug(e);
        }
    }

    /**
     * Hides the singleton AutCopmletePopupWindow.
     * Also, it ensures that the corresponding textfield contents get trimmed and an event gets fired
     * so that the trimmed value is used as present valie of the textfield.
     * 
     */
    public static void hideWindow() {
        try {
            if (window != null) {

                // try to trim text if necessary
                if (window.showingAt != null) {
                    String s = window.showingAt.getText();
                    int l = s.length();
                    if (l > 0) {
                        s = s.trim();
                        if (s.length() < l) {
                            window.showingAt.setText(s);
                            if (window.callback != null) {
                                window.callback.acpwCallback(window.showingAt);
                            }
                        }
                    }
                }

                window.hideWindowThread = new HideWindowThread(window);
                window.hideWindowThread.start();
            }
        } catch (Exception e) {
        	Logger.logdebug(e);
        }
    }

    /**
     * Determines if the singleton AutoCompletePopupWindow is currently shown at a textfield.
     * @param field
     * @return true if it is currently shown, false if not, or no window is currently shown.
     */
    public static boolean isShowingAt(JTextField field) {
        try {
            if (window != null) {
                if (window.showingAt == field) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static AutoCompletePopupWindow getWindow() {
        return window;
    }
}

class HideWindowThread extends Thread {

    private AutoCompletePopupWindow window;

    public HideWindowThread(AutoCompletePopupWindow window) {
        this.window = window;
    }

    public void run() {
    	this.setName("AutoCompletePopupWindow.HideWindowThread");
        try {
            Thread.sleep(10); //sleep 10 milliseconds
            //then hide the window.
            //as window hiding is in the main swing thread, we have to use invokeLater method to do so.
            //otherwise, some exceptions may occur ad other occasions within the main swing thread.
        	SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
        			window.doHide();
        		}
        	})
            ;
        } catch (Exception e) {
        }
    }
}
