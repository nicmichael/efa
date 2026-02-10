/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import de.nmichael.efa.core.items.*;
import de.nmichael.efa.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

// @i18n complete
public abstract class BaseTabbedDialog extends BaseDialog {

    public static final char CATEGORY_SEPARATOR = ':';
    public static final String CATEGORY_COMMON = "%00%" + International.getString("Allgemein");
    public static final String CATEGORY_NONAME = "%00%NONAME";

    protected JComponent topLevelPane;
    protected JPanel dataPanel;

    protected JPanel dataNorthPanel;
    protected JCheckBox expertMode;
    protected JComponent dataNorthCenterComponent; // may be set by subclass
    protected JComponent dataNorthEastComponent; // may be set by subclass

    protected String _selectedPanel; // selected panel specified in constructor

    protected Vector<IItemType> allGuiItems;
    protected Hashtable<String,Hashtable> categoryHierarchy;
    protected Hashtable<String,Vector<IItemType>> itemsPerCategory;
    protected Vector<IItemType> displayedGuiItems;
    protected Hashtable<JPanel,String> panels;

    protected boolean defaultGetGuiItemsOnUpdateGui = false; // true for EfaConfigDialog (req. by Hashtable); else false

    protected boolean expertModeEnabled = false;
    protected boolean expertModeItems = false;

    public BaseTabbedDialog(Frame parent, String title, String closeButtonText,
            Vector<IItemType> guiItems,
            boolean defaultGetGuiItemsOnUpdateGui) {
        super(parent, title, closeButtonText);
        setItems(guiItems);
        this.defaultGetGuiItemsOnUpdateGui = defaultGetGuiItemsOnUpdateGui;
    }

    public BaseTabbedDialog(JDialog parent, String title, String closeButtonText,
            Vector<IItemType> guiItems,
            boolean defaultGetGuiItemsOnUpdateGui) {
        super(parent, title, closeButtonText);
        setItems(guiItems);
        this.defaultGetGuiItemsOnUpdateGui = defaultGetGuiItemsOnUpdateGui;
    }

    public static String makeCategory(String c1) {
        return (c1 != null ? c1 : CATEGORY_NONAME);
    }
    public static String makeCategory(String c1, String c2) {
        return c1 + CATEGORY_SEPARATOR + c2;
    }
    public static String makeCategory(String c1, String c2, String c3) {
        return c1 + CATEGORY_SEPARATOR + c2 + CATEGORY_SEPARATOR + c3;
    }

    public static String[] getCategoryKeyArray(String keystring) {
        Vector v = EfaUtil.split(keystring, CATEGORY_SEPARATOR);
        String[] a = new String[v.size()];
        for (int i=0; i<v.size(); i++) {
            a[i] = (String)v.get(i);
        }
        return a;
    }

    public static String getCatName(String key) {
        if (key.equals(CATEGORY_NONAME)) {
            return "";
        }
        String catName = key;
        int pos = catName.lastIndexOf(CATEGORY_SEPARATOR);
        if (pos >= 0) {
            catName = catName.substring(pos+1);
        }
        int posFirst = -1;
        while ( (posFirst = catName.indexOf("%")) >= 0) {
            int posNext = catName.indexOf("%", posFirst + 1);
            if (posNext > 0) {
                catName = catName.substring(posNext + 1);
            }
        }
        return catName;
    }

    public void setItems(Vector<IItemType> guiItems) {
        this.allGuiItems = guiItems;
        expertModeItems = false;

        categoryHierarchy = new Hashtable<String,Hashtable>();    // category          -> sub-categories
        itemsPerCategory = new Hashtable<String,Vector<IItemType>>(); // categoryhierarchy -> config items

        if (guiItems == null) {
            return;
        }
        // build category hierarchy
        for (int i=0; i<guiItems.size(); i++) {
            IItemType item = guiItems.get(i);
            if (item.getType() == IItemType.TYPE_EXPERT) {
                expertModeItems = true;
            }
            String[] cats = getCategoryKeyArray(item.getCategory());
            Hashtable<String,Hashtable> h = categoryHierarchy;
            for (int j=0; j<cats.length; j++) {
                Hashtable hnext = h.get(cats[j]);
                if (hnext == null) {
                    hnext = new Hashtable<String,Hashtable>();
                    h.put(cats[j], hnext);
                }
                h = hnext;
            }
        }

        // build config items per category
        for (int i=0; i<guiItems.size(); i++) {
            IItemType item = guiItems.get(i);
            item.setUnchanged();
            String cat = item.getCategory();
            String[] cats = getCategoryKeyArray(cat);
            Hashtable<String,Hashtable> h = categoryHierarchy;
            for (int j=0; j<cats.length; j++) {
                Hashtable hnext = h.get(cats[j]);

                // check whether there are subcategories for the parameter's level
                if (j == cats.length-1 && hnext.size() != 0) {
                    // yes, there are subcategories for this level
                    // --> place this parameter into a subcategory CATEGORY_COMMON
                    cat = makeCategory(cat, CATEGORY_COMMON);

                    // is there already a level CATEGORY_COMMON on this level?
                    if (hnext.get(CATEGORY_COMMON) != null) {
                        // ok, there already is a level CATEGORY_COMMON
                    } else {
                        // there is no level CATEGORY_COMMON yet --> add one
                        hnext.put(CATEGORY_COMMON, new Hashtable<String,Hashtable>());
                    }
                }
                h = hnext;
            }

            // build config items per category
            Vector<IItemType> v = itemsPerCategory.get(cat);
            if (v == null) {
                v = new Vector<IItemType>();
            }
            v.add(item);
            itemsPerCategory.put(cat, v);
        }

    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        // create GUI items
        mainPanel.setLayout(new BorderLayout());
        dataPanel = new JPanel();
        dataPanel.setLayout(new BorderLayout());
        expertMode = new JCheckBox();
        Mnemonics.setButton(this, expertMode, International.getString("Expertenmodus"));
        expertMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { expertModeChanged(e); }
        });
        if (!expertModeItems && expertMode != null) {
            expertMode.setVisible(false);
        }

        // Data North Panel
        dataNorthPanel = new JPanel();
        dataNorthPanel.setLayout(new BorderLayout());
        dataNorthPanel.add(expertMode, BorderLayout.WEST);
        if (dataNorthCenterComponent != null) {
            dataNorthPanel.add(dataNorthCenterComponent, BorderLayout.CENTER);
        }
        if (dataNorthEastComponent != null) {
            dataNorthPanel.add(dataNorthEastComponent, BorderLayout.EAST);
        }

        dataPanel.add(dataNorthPanel, BorderLayout.NORTH);
        mainPanel.add(dataPanel, BorderLayout.CENTER);
        updateGui(false);
    }

    public void updateGui() {
        updateGui(defaultGetGuiItemsOnUpdateGui);
    }

    public void updateGui(boolean readValuesFromGui) {
        if (readValuesFromGui) {
            getValuesFromGui();
        }

        String[] cats = categoryHierarchy.keySet().toArray(new String[0]);
        Arrays.sort(cats);

        String selectedPanel = (topLevelPane instanceof JTabbedPane ?
            getSelectedPanel((JTabbedPane)topLevelPane) : null);

        displayedGuiItems = new Vector<IItemType>();
        if (topLevelPane != null) {
            dataPanel.remove(topLevelPane);
        }
        topLevelPane = (cats.length > 1 ? new JTabbedPane() : new JPanel());
        if (cats.length <= 1) {
            topLevelPane.setLayout(new BorderLayout());
        }
        panels = new Hashtable<JPanel,String>();
        expertModeEnabled = expertMode.isSelected();
        recursiveBuildGui(categoryHierarchy,itemsPerCategory,"",topLevelPane, selectedPanel);
        dataPanel.add(topLevelPane, BorderLayout.CENTER);
        this.validate();

        // check if ItemTypeItemList is displayed. If yes, we need to get all currently
        // displayed values each time ItemTypeItemList calls updateGui() whenever an item
        // is added or removed
        for (IItemType item : displayedGuiItems) {
            if (item instanceof ItemTypeItemList) {
                defaultGetGuiItemsOnUpdateGui = true;
            }
        }



        // select an item to focus
        Vector<IItemType> v = itemsPerCategory.get( (selectedPanel != null ? selectedPanel : cats[0]));
        for (int i=0; v != null && i<v.size(); i++) {
            if (!(v.get(i) instanceof ItemTypeLabel) && v.get(i).isVisible() && v.get(i).isEditable()) {
                if (focusItem == null) {
                    setRequestFocus(v.get(i));
                }
                break;
            }
        }

    }

    protected int recursiveBuildGui(Hashtable<String,Hashtable> categories,
                                   Hashtable<String,Vector<IItemType>> items,
                                   String catKey,
                                   JComponent currentPane,
                                   String selectedPanel) {
        int itmcnt = 0;
        int pos = (selectedPanel != null && selectedPanel.length() > 0 ? selectedPanel.indexOf(CATEGORY_SEPARATOR) : -1);
        String selectThisCat = (pos < 0 ? selectedPanel : selectedPanel.substring(0,pos));
        String selectNextCat = (pos < 0 ? null : selectedPanel.substring(pos+1));

        Object[] cats = categories.keySet().toArray();
        Arrays.sort(cats);
        for (int i=0; i<cats.length; i++) {
            String key = (String)cats[i];
            String thisCatKey = (catKey.length() == 0 ? key : makeCategory(catKey, key));
            String catName = getCatName(thisCatKey);
            Hashtable<String,Hashtable> subCat = categories.get(key);
            if (subCat.size() != 0) {
                JTabbedPane subTabbedPane = new JTabbedPane();
                if (recursiveBuildGui(subCat, items, thisCatKey, subTabbedPane, selectNextCat) > 0) {
                    if (currentPane instanceof JTabbedPane) {
                        currentPane.add(subTabbedPane, catName);
                    } else {
                        currentPane.add(subTabbedPane, BorderLayout.CENTER);
                    }
                    if (key.equals(selectThisCat) && currentPane instanceof JTabbedPane) {
                        ((JTabbedPane)currentPane).setSelectedComponent(subTabbedPane);
                    }
                }
            } else {
                JPanel panel = new JPanel();
                panels.put(panel, thisCatKey);
                panel.setLayout(new GridBagLayout());
                Vector<IItemType> v = items.get(thisCatKey);
                int y = 0;
                for (int j=0; v != null && j<v.size(); j++) {
                    IItemType itm = v.get(j);
                    if (itm.getType() == IItemType.TYPE_PUBLIC ||
                        (itm.getType() == IItemType.TYPE_EXPERT && expertModeEnabled)) {
                        y += itm.displayOnGui(this,panel,y);
                        displayedGuiItems.add(itm);
                        itmcnt++;
                    }
                }
                if (y > 0) {
                    if (currentPane instanceof JTabbedPane) {
                        currentPane.add(panel, catName);
                    } else {
                        currentPane.add(panel, BorderLayout.CENTER);
                    }
                    if (key.equals(selectThisCat) && currentPane instanceof JTabbedPane) {
                        ((JTabbedPane)currentPane).setSelectedComponent(panel);
                    }
                }
            }
        }
        return itmcnt;
    }

    protected boolean getValuesFromGui() {
        if (allGuiItems == null) {
            return false;
        }
        boolean changed = false;
        for (int i=0; i<allGuiItems.size(); i++) {
            IItemType item = allGuiItems.get(i);
            item.getValueFromGui();
            if (item.isChanged()) {
                changed = true;
                if (Logger.isTraceOn(Logger.TT_GUI)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, this.getClass().getCanonicalName()+".getValuesFromGui(): "+item.getName()+" has changed");
                }
            }
        }
        return changed;
   }

    void expertModeChanged(ActionEvent e) {
        if (expertMode.isSelected()) {
            expertMode.setForeground(Color.red);
        } else {
            expertMode.setForeground(Color.black);
        }
        expertMode.setVisible(expertModeItems);
        updateGui();
    }

    protected String getSelectedPanel(JTabbedPane pane) {
        if (_selectedPanel != null) {
            String s = _selectedPanel;
            _selectedPanel = null;
            return s;
        }
        if (pane == null) {
            return null;
        }
        Component c = pane.getSelectedComponent();
        if (c == null) {
            return null;
        }
        try {
            JPanel panel = (JPanel)c;
            return panels.get(panel);
        } catch(Exception e) {
            try {
                return getSelectedPanel((JTabbedPane)c);
            } catch(Exception ee) {
                return null;
            }
        }
    }

    public Vector<IItemType> getItems() {
        return allGuiItems;
    }

    public IItemType getItem(String name) {
        for (int i=0; i<allGuiItems.size(); i++) {
            if (allGuiItems.get(i).getName().equals(name)) {
                return allGuiItems.get(i);
            }
        }
        return null;
    }

}
