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
import javax.swing.*;
import java.awt.*;
import java.awt.Dialog.ModalExclusionType;
import java.awt.event.*;
import java.util.*;

// @i18n complete
public class AutoCompletePopupWindow extends JWindow {

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
        this.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        try {
            jbInit();
            setListSize(200, 100);
            if (Daten.efaConfig != null && Daten.efaConfig.getValueTouchScreenSupport()) {
                scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(25, 1));
            }
            scrollPane.setHorizontalScrollBar(null);
            // Bugfix: AutoCompletePopupWindow muß unter Windows ebenfalls alwaysOnTop sein, wenn EfaDirektFrame alwaysOnTop ist, da sonst die Popup-Liste nicht erscheint
            if (Daten.osName.startsWith("Windows") && Daten.efaConfig != null &&
                Daten.efaConfig.getValueEfaDirekt_immerImVordergrund()) {
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

    public void setListSize(int x, int y) {
        if (Daten.efaConfig != null && Daten.efaConfig.getValueTouchScreenSupport()) {
            y *= 2;
        }
        this.scrollPane.setPreferredSize(new Dimension(x, y));
        this.pack();
    }

    private int setListData(AutoCompleteList list) {
        list.update();
        String[] data = autoCompleteLists.get(list);
        Long scn = autoCompleteSCN.get(list);
        if (data == null || scn == null || scn.longValue() != list.getSCN()) {
            data = list.getData();
            autoCompleteLists.put(list, data);
            autoCompleteSCN.put(list, new Long(list.getSCN()));
        }
        this.list.setListData(data);
        return data.length;
    }

    private void showAtTextField(JTextField field) {
        if (showingAt == field) {
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
        }
    }

    public void doHide() {
        if (showingAt != null) {
            this.setVisible(false);
            lastShowingAt = showingAt;
            lastShowingAtTime = System.currentTimeMillis();
        }
        showingAt = null;
    }

    private void selectEintrag(String eintrag) {
        list.setSelectedValue(eintrag, true);
        try {
            list.scrollRectToVisible(list.getCellBounds(list.getSelectedIndex() - 1, list.getSelectedIndex() + 1));
        } catch (Exception e) {
        }
    }

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

    public static void showAndSelect(JTextField field, AutoCompleteList list, String eintrag, AutoCompletePopupWindowCallback callback) {
        try {
            if (window == null) {
                window = new AutoCompletePopupWindow(Dialog.frameCurrent());
            }
            window.callback = callback;
            if (window.setListData(list) == 0) {
                return;
            }
            window.showAtTextField(field);
            window.selectEintrag(eintrag);
        } catch (Exception e) {
        }
    }

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
        }
    }

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
        try {
            Thread.sleep(10);
            window.doHide();
        } catch (Exception e) {
        }
    }
}
