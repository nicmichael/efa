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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedLabel;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;

// @i18n complete
public abstract class BaseTabbedDialog extends BaseDialog {

    private static final String CARD_EMPTY = "__empty__";
	public static final char CATEGORY_SEPARATOR = ':';
    public static final String CATEGORY_SEPARATOR_STRING = "" + CATEGORY_SEPARATOR;
    public static final String CATEGORY_COMMON = "%00%" + International.getString("Allgemein");
    public static final String CATEGORY_NONAME = "%00%NONAME";
    
    // Navigation Mode Konstanten
    protected static final int MODE_TABBED_PANE = 0;
    protected static final int MODE_LEFT_NAVIGATION = 1;
    protected static final int NAVIGATIONLIST_WIDTH = 220;
    private static final String HIGHLIGHT_STYLE = "background-color:#fff176; color:#000000;";
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final int FILTER_DELAY_MS = 500;
    private static final int MIN_FILTER_LENGTH = 2;
    private static final String BREADCRUMB_SEPARATOR = "  >  ";//" \u203A ";
    private static final String ACTION_NAV_ACTIVATE = "nav.activate";
    private static final String ACTION_NAV_FOCUS_FILTER = "nav.focusFilter";
    private static final String ACTION_FILTER_ARROW_UP = "filter.arrowUp";
    private static final String ACTION_FILTER_ARROW_DOWN = "filter.arrowDown";
    private static final String GROUP_CARD_PREFIX = "__group__::";

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
    
    protected Dimension contentDimension=null;
    
    // Navigation-spezifische Felder
    protected int navigationMode = MODE_TABBED_PANE; // Standard: TabbedPane
    protected JList<NavEntry> navigationList;
    protected DefaultListModel<NavEntry> navigationModel;
    protected List<NavEntry> allNavigationEntries;
    protected JTextField navigationFilterField;
    protected Timer navigationFilterTimer;
    protected CardLayout cardLayout;
    protected JPanel cardPanel;
    protected String lastSelectedCardKey;
    protected String persistedFilterText = "";
    protected String persistedSelectedCardKey = null;
    protected String persistedSelectedPanelKey = null;
    protected final List<JPanel> leafCardPanels = new ArrayList<JPanel>();
    protected final Map<JLabel, String> originalLabelTexts = new HashMap<JLabel, String>();
    protected final Map<AbstractButton, String> originalButtonTexts = new HashMap<AbstractButton, String>();
    private Color navigationFilterFieldBackground = Color.white;
    protected JLabel breadcrumbLabel;  

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
    
    /**
     * Setzt den Navigationsmodus (TabbedPane oder LeftNavigation)
     * @param mode MODE_TABBED_PANE (0) oder MODE_LEFT_NAVIGATION (1)
     */
    protected void setNavigationMode(int mode) {
        this.navigationMode = mode;
    }

    protected void setContentDimension(Dimension value) {
    	contentDimension = value;
    }
    
    protected Dimension getContentDimension() {
    	return contentDimension;
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

    /**
     * Updates the the GUI as additional elements may have been placed on it 
     * (by ItemTypeItemList for instance). 
     */
    public void updateGui() {
        updateGui(defaultGetGuiItemsOnUpdateGui);
        //enableAutoScrollOnFocus is initially called in BaseDialog.prepareDialog()
        //and should be called only once on each dialog.
        //But as updateGUI removes all elements from the GUI and re-adds them,
        //we need to re-initialize the AutoScroll on focus.
        EfaGuiUtils.enableAutoScrollOnFocus(this);
    }

    public void updateGui(boolean readValuesFromGui) {
        if (readValuesFromGui) {
            getValuesFromGui();
        }

        String[] cats = categoryHierarchy.keySet().toArray(new String[0]);
        Arrays.sort(cats);

        String selectedPanel = null;
        if (navigationMode == MODE_TABBED_PANE && topLevelPane instanceof JTabbedPane) {
            selectedPanel = getSelectedPanel((JTabbedPane)topLevelPane);
        }

        displayedGuiItems = new Vector<IItemType>();
        if (topLevelPane != null) {
            dataPanel.remove(topLevelPane);
        }
        
        if (navigationMode == MODE_LEFT_NAVIGATION) {
            topLevelPane = new JPanel();
            topLevelPane.setLayout(new BorderLayout());
        } else {
            topLevelPane = (cats.length > 1 ? new JTabbedPane() : new JPanel());
            if (cats.length <= 1) {
                topLevelPane.setLayout(new BorderLayout());
            }
        }
        
        panels = new Hashtable<JPanel,String>();
        expertModeEnabled = expertMode.isSelected();
        recursiveBuildGui(categoryHierarchy,itemsPerCategory,"",topLevelPane, selectedPanel, this.reduceInnerScrollPaneHeight());
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

        if (navigationMode == MODE_TABBED_PANE) {
            Vector<IItemType> v = itemsPerCategory.get( (selectedPanel != null ? selectedPanel : cats[0]));
            for (int i=0; v != null && i<v.size(); i++) {
                if (!(v.get(i) instanceof ItemTypeLabel) && v.get(i).isVisible() && v.get(i).isEditable() && v.get(i).isEnabled() ) {
                    if (focusItem == null) {
                        setRequestFocus(v.get(i));
                    }
                    break;
                }
            }
        }

    }

    /**
	 * Recursively builds the GUI with the configured items.
	 * Either as the classic tabbedpane, or as the navigation list and card panels.
	 *
	 * @param categories      The hashtable of categories to process.
	 * @param items           The hashtable of items associated with each category.
	 * @param catKey          The current category key being processed.
	 * @param currentPane     The current pane to which the navigation and cards will be added.
	 * @param selectedPanel   The key of the panel that should be selected initially.
	 * @param otherPanelHeight The height of other panels, used for layout calculations.
	 * @return The total number of selectable items processed.
	 */
    protected int recursiveBuildGui(Hashtable<String,Hashtable> categories,
                                   Hashtable<String,Vector<IItemType>> items,
                                   String catKey,
                                   JComponent currentPane,
                                   String selectedPanel, int otherPanelHeight) {
        
        if (navigationMode == MODE_LEFT_NAVIGATION) {
            return buildGuiWithLeftNavigation(categories, items, catKey, currentPane, selectedPanel, otherPanelHeight);
        } else {
            return buildGuiWithTabbedPane(categories, items, catKey, currentPane, selectedPanel, otherPanelHeight);
        }
    }

    /**
	 * Recursively builds the GUI with the configured items as the classic tabbedPane.
	 * Same code as before, except for panel creation is delegated to buildLeafPanel()
	 *
	 * @param categories      The hashtable of categories to process.
	 * @param items           The hashtable of items associated with each category.
	 * @param catKey          The current category key being processed.
	 * @param currentPane     The current pane to which the navigation and cards will be added.
	 * @param selectedPanel   The key of the panel that should be selected initially.
	 * @param otherPanelHeight The height of other panels, used for layout calculations.
	 * @return The total number of selectable items processed.
	 */
    protected int buildGuiWithTabbedPane(Hashtable<String,Hashtable> categories,
                                   Hashtable<String,Vector<IItemType>> items,
                                   String catKey,
                                   JComponent currentPane,
                                   String selectedPanel, int otherPanelHeight) {
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
                if (buildGuiWithTabbedPane(subCat, items, thisCatKey, subTabbedPane, selectNextCat, otherPanelHeight ) > 0) {
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
                JPanel panel = buildLeafPanel(items, thisCatKey);
                if (panel != null) {
                    panels.put(panel, thisCatKey);
                    if (currentPane instanceof JTabbedPane) {
                        currentPane.add(panel, catName);
                    } else {
                        currentPane.add(panel, BorderLayout.CENTER);
                    }
                    if (key.equals(selectThisCat) && currentPane instanceof JTabbedPane) {
                        ((JTabbedPane)currentPane).setSelectedComponent(panel);
                    }
                    itmcnt++;
                }
            }
        }
        return itmcnt;
    }

    /**
	 * Recursively builds the GUI with the configured items as the left navigation bar.
	 * 
	 * @param categories      The hashtable of categories to process.
	 * @param items           The hashtable of items associated with each category.
	 * @param catKey          The current category key being processed.
	 * @param currentPane     The current pane to which the navigation and cards will be added.
	 * @param selectedPanel   The key of the panel that should be selected initially.
	 * @param otherPanelHeight The height of other panels, used for layout calculations.
	 * 
	 * @return The total number of selectable items processed.
	 */    
    protected int buildGuiWithLeftNavigation(Hashtable<String,Hashtable> categories,
                                          Hashtable<String,Vector<IItemType>> items,
                                          String catKey,
                                          JComponent currentPane,
                                          String selectedPanel,
                                          int otherPanelHeight) {
        navigationModel = new DefaultListModel<NavEntry>();
        allNavigationEntries = new ArrayList<NavEntry>();
        leafCardPanels.clear();
        originalLabelTexts.clear();
        originalButtonTexts.clear();

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(createEmptyCard(), CARD_EMPTY);

        int itmcnt = collectNavAndCards(categories, items, catKey, otherPanelHeight, 0);

        navigationList = new JList<NavEntry>(navigationModel);
        navigationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navigationList.setCellRenderer(new NavEntryRenderer());
        addNavigationListListeners(navigationList);

        navigationFilterField = new JTextField();
        navigationFilterField.setToolTipText(International.getString("STRG+F für Suche"));

        navigationFilterTimer = new Timer(FILTER_DELAY_MS, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyNavigationFilterDeferred();
            }
        });
        navigationFilterTimer.setRepeats(false);

        addNavigationFilterFieldListeners();

        JPanel navPanel = new JPanel(new BorderLayout(0, 4));
        navPanel.add(navigationFilterField, BorderLayout.NORTH);
        JScrollPane myScroller = new JScrollPane(navigationList);
        navPanel.add(myScroller, BorderLayout.CENTER);
        navPanel.setPreferredSize(new Dimension(NAVIGATIONLIST_WIDTH, 10));

        JScrollPane cardScrollWrapper = new JScrollPane(cardPanel);
        cardScrollWrapper.getVerticalScrollBar().setUnitIncrement(12);
        cardScrollWrapper.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        breadcrumbLabel = new RoundedLabel();
        breadcrumbLabel.setBorder(new RoundedBorder(Daten.efaConfig.getHeaderForegroundColor()));
        breadcrumbLabel.setOpaque(true);
        breadcrumbLabel.setForeground(Daten.efaConfig.getHeaderForegroundColor());
        breadcrumbLabel.setBackground(Daten.efaConfig.getHeaderBackgroundColor());//.darker());
        breadcrumbLabel.setFont(breadcrumbLabel.getFont().deriveFont(Font.BOLD));
        breadcrumbLabel.setText(" ");
        
        JPanel rightContentPanel = new JPanel(new BorderLayout(0, 6));
        rightContentPanel.add(breadcrumbLabel, BorderLayout.NORTH);
        rightContentPanel.add(cardScrollWrapper, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout(8, 0));
        contentPanel.add(navPanel, BorderLayout.WEST);
        contentPanel.add(rightContentPanel, BorderLayout.CENTER);

        currentPane.setLayout(new BorderLayout(8, 0));
        currentPane.add(contentPanel, BorderLayout.CENTER);

        KeyStroke findKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
        currentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(findKey, ACTION_NAV_FOCUS_FILTER);
        currentPane.getActionMap().put(ACTION_NAV_FOCUS_FILTER, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                focusFilterField();
            }
        });

        if (persistedFilterText != null && persistedFilterText.length() > 0) {
            navigationFilterField.setText(persistedFilterText);
        }

        applyNavigationFilterWithMinLength();

        if (!restoreSelectionAfterRebuild()) {
            selectInitialEntry(selectedPanel);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                focusFilterField();
            }
        });

        return itmcnt;
    }
    
    /**
	 * Builds a leaf panel for the specified category key, containing the items associated with that category
	 * and their corresponding GUI components. Used from both recursiveBuildGUITabbedPane as well
	 * as the recursiveBuildGuiNAvigationbar.
	 * 
	 * @param items The hashtable of items associated with each category.
	 * @param thisCatKey The category key for which to build the leaf panel.
	 * 
	 * @return A JPanel representing the leaf panel for the specified category, or null if there are no items to display.
	 */   
    private JPanel buildLeafPanel(Hashtable<String, Vector<IItemType>> items, String thisCatKey) {
        JPanel panel = new JPanel();
        JPanel innerPanel = new JPanel();

        JScrollPane scrollPane = new JScrollPane(innerPanel);
        if (this.getContentDimension()!=null) {
        	scrollPane.setPreferredSize(getContentDimension());
        }
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        innerPanel.setLayout(new GridBagLayout());
        innerPanel.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 6));
        panel.setLayout(new GridBagLayout());
        panel.add(scrollPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        Vector<IItemType> v = items.get(thisCatKey);
        int y = 1;
        for (int j = 0; v != null && j < v.size(); j++) {
            IItemType itm = v.get(j);
            if (itm.getType() == IItemType.TYPE_PUBLIC
                    || (itm.getType() == IItemType.TYPE_EXPERT && expertModeEnabled)) {
                y += itm.displayOnGui(this, innerPanel, y);
                displayedGuiItems.add(itm);
            }
        }

        // Push remaining vertical space below all real items so content stays top-aligned.
        // but only if we are in left naviation mode.
        if (this.navigationMode == MODE_LEFT_NAVIGATION) {
	        JPanel filler = new JPanel();
	        filler.setOpaque(false);
	        innerPanel.add(filler, new GridBagConstraints(
	                0, y, 12, 1, 1.0, 1.0,
	                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
	                new Insets(0, 0, 0, 0), 0, 0));
        }
        return y > 1 ? panel : null;
    }

    /**
	 * Creates an empty card panel with a message indicating that the user can use Ctrl+F for search.
	 * 
	 * @return A JPanel representing the empty card.
	 */
    private JPanel createEmptyCard() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(International.getString("STRG+F für Suche"), SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.ITALIC));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    /**
	 * Recursively collects navigation entries and card panels for the given categories and items.
	 * 
	 * @param categories      The hashtable of categories to process.
	 * @param items           The hashtable of items associated with each category.
	 * @param catKey          The current category key being processed.
	 * @param otherPanelHeight The height of other panels, used for layout calculations.
	 * @param level           The current level of recursion, used for indentation in the navigation list.
	 * 
	 * @return The total number of selectable items processed.
	 */
    private int collectNavAndCards(Hashtable<String, Hashtable> categories,
                                   Hashtable<String, Vector<IItemType>> items,
                                   String catKey,
                                   int otherPanelHeight,
                                   int level) {
        int itmcnt = 0;
        Object[] cats = categories.keySet().toArray();
        Arrays.sort(cats);

        for (int i = 0; i < cats.length; i++) {
            String key = (String) cats[i];
            String thisCatKey = (catKey.length() == 0 ? key : makeCategory(catKey, key));
            String catName = getCatName(thisCatKey);
            Hashtable<String, Hashtable> subCat = categories.get(key);

            if (subCat != null && subCat.size() != 0) {
                String parentKey = getParentKey(thisCatKey);
                String groupSearchText = normalizeSearchText(catName);
                String groupCardKey = GROUP_CARD_PREFIX + thisCatKey;
                
                int groupIndex = allNavigationEntries.size();
                allNavigationEntries.add(new NavEntry(
                        thisCatKey, catName, level, true, false, null, parentKey, groupSearchText));

                int before = itmcnt;
                itmcnt += collectNavAndCards(subCat, items, thisCatKey, otherPanelHeight, level + 1);

                boolean hasSelectableChild = itmcnt > before;
                if (!hasSelectableChild) {
                    cardPanel.add(createGroupPlaceholderPanel(catName), groupCardKey);
                }

                allNavigationEntries.set(groupIndex, new NavEntry(
                        thisCatKey,
                        catName,
                        level,
                        true,
                        !hasSelectableChild,
                        hasSelectableChild ? null : groupCardKey,
                        parentKey,
                        groupSearchText));
            } else {
                JPanel panel = buildLeafPanel(items, thisCatKey);
                if (panel != null) {
                    cardPanel.add(panel, thisCatKey);
                    leafCardPanels.add(panel);
                    panels.put(panel, thisCatKey);

                    String cardContentSearch = buildSearchText(panel);
                    String leafSearchText = normalizeSearchText(catName + " " + cardContentSearch);

                    allNavigationEntries.add(new NavEntry(
                            thisCatKey, catName, level, false, true, thisCatKey, getParentKey(thisCatKey), leafSearchText));
                    itmcnt++;
                }
            }
        }

        return itmcnt;
    }
    
    /** 
     * Adds listeners to the navigation list to handle selection changes and mouse clicks.
     * @param navigationList
     */
    private void addNavigationListListeners(JList<NavEntry> navigationList) {
        navigationList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            NavEntry entry = navigationList.getSelectedValue();
            if (entry != null && entry.selectable) {
                persistedSelectedPanelKey = entry.key;
                showCard(entry.cardKey, entry.key);
            } else {
                showCard(CARD_EMPTY, null);
            }
        });

        /*
        navigationList.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("ENTER"), "nav.activate");
        navigationList.getActionMap().put("nav.activate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int idx = navigationList.getSelectedIndex();
                if (idx >= 0 && idx < navigationList.getModel().getSize()) {
                    NavEntry entry = navigationList.getModel().getElementAt(idx);
                    if (entry != null && entry.selectable) {
                        persistedSelectedPanelKey = entry.key;
                        showCard(entry.cardKey, entry.key);
                    }
                }
            }
        });*/
        navigationList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = navigationList.locationToIndex(e.getPoint());
                if (idx < 0) {
                    return;
                }
                navigationList.setSelectedIndex(idx);
                activateNavigationEntryAt(idx);
            }
        });

        navigationList.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("ENTER"), ACTION_NAV_ACTIVATE);
        navigationList.getActionMap().put(ACTION_NAV_ACTIVATE, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                activateNavigationEntryAt(navigationList.getSelectedIndex());
            }
        });
    }
    
    /**
	 * Activates the navigation entry at the specified index in the navigation list.
	 * 
	 * @param idx The index of the navigation entry to activate.
	 */
    private void activateNavigationEntryAt(int idx) {
        if (idx < 0 || idx >= navigationList.getModel().getSize()) {
            return;
        }

        NavEntry entry = navigationList.getModel().getElementAt(idx);
        if (entry == null) {
            return;
        }

        if (entry.selectable) {
            persistedSelectedPanelKey = entry.key;
            showCard(entry.cardKey, entry.key);
            return;
        }

        /*if (entry.group) {
            int childIndex = findFirstVisibleSelectableChildIndex(entry.key);
            if (childIndex >= 0 && childIndex != idx) {
                navigationList.setSelectedIndex(childIndex);
                if (navigationList.getFirstVisibleIndex()>childIndex || navigationList.getLastVisibleIndex()<childIndex) {
                	navigationList.ensureIndexIsVisible(childIndex);
                }
            }
        }*/
    }
    
    /** 
	 * Adds listeners to the navigation filter field to handle text changes and key events.
	 *
	 * @param navigationFilterField2 The JTextField used for filtering navigation entries.
	 */
    private void addNavigationFilterFieldListeners() {
    	
        navigationFilterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                scheduleNavigationFilter();
            }
            public void removeUpdate(DocumentEvent e) {
                scheduleNavigationFilter();
            }
            public void changedUpdate(DocumentEvent e) {
                scheduleNavigationFilter();
            }
        });

        navigationFilterField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), ACTION_FILTER_ARROW_DOWN);
        navigationFilterField.getActionMap().put(ACTION_FILTER_ARROW_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveFocusFromFilterToList(true);
            }
        });

        navigationFilterField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), ACTION_FILTER_ARROW_UP);
        navigationFilterField.getActionMap().put(ACTION_FILTER_ARROW_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveFocusFromFilterToList(false);
            }
        });

        navigationFilterFieldBackground = navigationFilterField.getBackground();

        navigationFilterField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(FocusEvent e) {
                navigationFilterField.setBackground(Color.YELLOW);
            }
            public void focusLost(FocusEvent e) {
                if (!navigationFilterField.getText().isEmpty()) {
                    navigationFilterField.setBackground(new Color(255,255,204));
                } else {
                    navigationFilterField.setBackground(navigationFilterFieldBackground);
                }
            }
        });
    }
    
    /**
	 * Moves the focus from the navigation filter field to the navigation list.
	 * 
	 * @param toNext If true, moves focus to the next item; if false, moves focus to the previous item.
	 */
    private void moveFocusFromFilterToList(boolean toNext) {
        if (navigationList == null || navigationModel == null || navigationModel.getSize() == 0) {
            return;
        }
        int idx = navigationList.getSelectedIndex();
        if (idx < 0) {
            idx = toNext ? 0 : (navigationModel.getSize() - 1);
        } else if (toNext && idx < navigationModel.getSize() - 1) {
            idx++;
        } else if (!toNext && idx > 0) {
            idx--;
        }
        navigationList.setSelectedIndex(idx);
        if (navigationList.getFirstVisibleIndex()>idx || navigationList.getLastVisibleIndex()<idx) {
            navigationList.ensureIndexIsVisible(idx);
        }
        navigationList.requestFocusInWindow();
    }

    /**
	 * Sets the focus to the navigation filter field and selects all text within it.
	 * 
	 */
    private void focusFilterField() {
        if (navigationFilterField != null) {
            navigationFilterField.requestFocusInWindow();
            navigationFilterField.selectAll();
        }
    }

    /**
	 * Persists the current text in the navigation filter field to the persistedFilterText variable.
	 * This is needed as the filter action on the navigation list is deferred and may be executed 
	 * after the user has already changed the filter text.
	 */
    private void persistCurrentFilterText() {
        persistedFilterText = navigationFilterField != null ? navigationFilterField.getText() : "";
    }

    /**
	 * Schedules the application of the navigation filter after a delay.
	 * This method is called whenever the text in the navigation filter field changes.
	 */ 
    private void scheduleNavigationFilter() {
        persistCurrentFilterText();
        if (navigationFilterTimer != null) {
            navigationFilterTimer.restart();
        }
    }

    /**
	 * Applies the navigation filter to the navigation list and card panels.
	 * This method is executed on the Event Dispatch Thread (EDT) to ensure thread safety.
	 * If called from a non-EDT thread, it will schedule itself to run on the EDT.
	 */
    private void applyNavigationFilterDeferred() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    applyNavigationFilterDeferred();
                }
            });
            return;
        }
        CursorState cursorState = setWaitCursor(true);
        try {
            applyNavigationFilterWithMinLength();
        } finally {
            restoreCursor(cursorState);
        }
    }

    /**
 	 * Applies the navigation filter to the navigation list and card panels, enforcing a minimum filter length
 	 * of MIN_FILTER_LENGTH characters. If the filter text is shorter than this length, it will be treated as empty.
 	 */ 
    private void applyNavigationFilterWithMinLength() {
        String raw = navigationFilterField != null ? navigationFilterField.getText() : "";
        String trimmed = raw == null ? "" : raw.trim();
        String filter = normalizeSearchText(trimmed);

        if (filter.length() > 0 && filter.length() < MIN_FILTER_LENGTH) {
            filter = "";
        }

        applyNavigationFilter(filter);
    }

    /**
	 * Sets the cursor to a wait cursor or restores it to the default cursor.
	 * 
	 * @param wait If true, sets the cursor to a wait cursor; if false, restores it to the default cursor.
	 * @return A CursorState object containing the previous cursor states of the dialog, filter field, navigation list, and window.
	 */ 
    private CursorState setWaitCursor(boolean wait) {
        Cursor cursor = wait
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor();

        CursorState state = new CursorState();
        state.dialog = this;
        state.dialogCursor = this.getCursor();

        if (navigationFilterField != null) {
            state.filterField = navigationFilterField;
            state.filterFieldCursor = navigationFilterField.getCursor();
        }

        if (navigationList != null) {
            state.navigationList = navigationList;
            state.navigationListCursor = navigationList.getCursor();
        }

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            state.window = window;
            state.windowCursor = window.getCursor();
            window.setCursor(cursor);
        }

        this.setCursor(cursor);

        if (navigationFilterField != null) {
            navigationFilterField.setCursor(cursor);
        }
        if (navigationList != null) {
            navigationList.setCursor(cursor);
        }

        return state;
    }

    /**
	 * Restores the cursor to its previous state based on the provided CursorState object.
	 * 
	 * @param state The CursorState object containing the previous cursor states to restore.
	 */ 
    private void restoreCursor(CursorState state) {
        if (state == null) {
            return;
        }

        if (state.window != null) {
            state.window.setCursor(state.windowCursor != null
                    ? state.windowCursor
                    : Cursor.getDefaultCursor());
        }

        if (state.dialog != null) {
            state.dialog.setCursor(state.dialogCursor != null
                    ? state.dialogCursor
                    : Cursor.getDefaultCursor());
        }

        if (state.filterField != null) {
            state.filterField.setCursor(state.filterFieldCursor != null
                    ? state.filterFieldCursor
                    : Cursor.getDefaultCursor());
        }

        if (state.navigationList != null) {
            state.navigationList.setCursor(state.navigationListCursor != null
                    ? state.navigationListCursor
                    : Cursor.getDefaultCursor());
        }
    }

    /**
	 * Attempts to restore the selection in the navigation list after a rebuild of the GUI.
	 * It first tries to match the persisted selected panel key, and if not found, it falls back 
	 * to matching the persisted selected card key.
	 */
    private boolean restoreSelectionAfterRebuild() {
        if (navigationModel == null || navigationModel.getSize() == 0) {
            return false;
        }

        String desiredCard = persistedSelectedCardKey != null ? persistedSelectedCardKey : lastSelectedCardKey;
        String desiredPanel = persistedSelectedPanelKey;

        if (desiredPanel != null && desiredPanel.length() > 0) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable && desiredPanel.equals(e.key)) {
                    navigationList.setSelectedIndex(i);
                    if (navigationList.getFirstVisibleIndex()>i || navigationList.getLastVisibleIndex()<i) {
                        navigationList.ensureIndexIsVisible(i);
                    }
                    showCard(e.cardKey, e.key);
                    return true;
                }
            }
        }

        if (desiredCard != null && desiredCard.length() > 0) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable && desiredCard.equals(e.cardKey)) {
                    navigationList.setSelectedIndex(i);
                    if (navigationList.getFirstVisibleIndex()>i || navigationList.getLastVisibleIndex()<i) {
                        navigationList.ensureIndexIsVisible(i);
                    }
                    showCard(e.cardKey, e.key);
                    return true;
                }
            }
        }

        return false;
    }
    
    /**
	 * Selects the initial entry in the navigation list based on the provided selected panel key.
	 * If the selected panel key is not found, it selects the first selectable entry in the list.
	 * 
	 * @param selectedPanel The key of the panel to select initially, or null to select the first selectable entry.
	 */  
    private void selectInitialEntry(String selectedPanel) {
        int indexToSelect = -1;

        if (selectedPanel != null && selectedPanel.length() > 0) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable && selectedPanel.equals(e.key)) {
                    indexToSelect = i;
                    break;
                }
            }
        }

        if (indexToSelect < 0) {
        	if (navigationModel.size()>0) {
    			indexToSelect=0;        		
        	}
        	/* alternate code. select the first selectable element.*/
            /*for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable){
	                indexToSelect = i;
	                break;
                }
            }*/
        }

        if (indexToSelect >= 0) {
            navigationList.setSelectedIndex(indexToSelect);
            if (navigationList.getFirstVisibleIndex()>indexToSelect || navigationList.getLastVisibleIndex()<indexToSelect) {
                navigationList.ensureIndexIsVisible(indexToSelect);
            }
            NavEntry entry = navigationModel.get(indexToSelect);
            persistedSelectedPanelKey = entry.key;
            showCard(entry.cardKey, entry.key);
        } else {
            showCard(CARD_EMPTY, null);
        }
    }
    
    /**
	 * Displays the card panel corresponding to the specified card key and updates the last selected card key
	 * and persisted selected card key accordingly. If the card key is null, it shows an empty card panel.
	 * @param cardKey The key of the card panel to display, or null to show an empty card panel.
	 * @param fullCategoryKey The full category key associated with the card panel, used for breadcrumb display.
	 * */
    private void showCard(String cardKey, String fullCategoryKey) {
        if (cardKey == null) {
            cardLayout.show(cardPanel, CARD_EMPTY);
            lastSelectedCardKey = CARD_EMPTY;
            persistedSelectedCardKey = CARD_EMPTY;
            updateBreadcrumb(null);
            return;
        }
        cardLayout.show(cardPanel, cardKey);
        lastSelectedCardKey = cardKey;
        persistedSelectedCardKey = cardKey;
        updateBreadcrumb(fullCategoryKey);
    }

    private void updateBreadcrumb(String fullCategoryKey) {
        if (breadcrumbLabel == null) {
            return;
        }

        if (fullCategoryKey == null || fullCategoryKey.length() == 0) {
            breadcrumbLabel.setText(" ");
            return;
        }

        breadcrumbLabel.setText(getBreadcrumbFromFullCategory(fullCategoryKey));
    }
    /**
	 * Generates a breadcrumb string from the full category key by splitting it into parts and retrieving
	 * the corresponding category names for each part. The breadcrumb is constructed by joining the category names with a separator.
	 *
	 * @param fullCategoryKey The full category key to generate the breadcrumb from.
	 *
	 * @return A string representing the breadcrumb for the specified full category key.
	 */
    private String getBreadcrumbFromFullCategory(String fullCategoryKey) {
        List<String> parts = splitCategoryKey(fullCategoryKey);
        StringBuilder sb = new StringBuilder();
        String partialKey = "";
        
        for (int i = 0; i < parts.size(); i++) {
            partialKey = (i == 0) ? parts.get(i) : makeCategory(partialKey, parts.get(i));
            String name = getCatName(partialKey);
            if (name == null || name.length() == 0) {
                name = parts.get(i);
            }
            if (sb.length() > 0) {
                sb.append(BREADCRUMB_SEPARATOR);
            } else {
                sb.append(" "); // Einrückung
            }
            sb.append(name);
        }

        return sb.toString();
    }

    private List<String> splitCategoryKey(String fullCategoryKey) {
        ArrayList<String> parts = new ArrayList<String>();
        if (fullCategoryKey == null || fullCategoryKey.length() == 0) {
            return parts;
        }
        if (CATEGORY_SEPARATOR_STRING == null || CATEGORY_SEPARATOR_STRING.length() == 0) {
            parts.add(fullCategoryKey);
            return parts;
        }

        int start = 0;
        int sepLen = CATEGORY_SEPARATOR_STRING.length();
        int pos = fullCategoryKey.indexOf(CATEGORY_SEPARATOR, start);
        while (pos >= 0) {
            parts.add(fullCategoryKey.substring(start, pos));
            start = pos + sepLen;
            pos = fullCategoryKey.indexOf(CATEGORY_SEPARATOR, start);
        }
        parts.add(fullCategoryKey.substring(start));
        return parts;
    }


    private void applyNavigationFilter(String filter) {
        String effectiveFilter = filter == null ? "" : filter;

        applyHighlightsToCards(effectiveFilter);

        String keepCard = lastSelectedCardKey;
        if (keepCard == null && navigationList != null && navigationList.getSelectedValue() != null) {
            keepCard = navigationList.getSelectedValue().cardKey;
        }

        navigationModel.clear();

        if (effectiveFilter.length() == 0) {
            for (int i = 0; i < allNavigationEntries.size(); i++) {
                navigationModel.addElement(allNavigationEntries.get(i));
            }
        } else {
            HashSet<String> visibleKeys = new HashSet<String>();

            for (int i = 0; i < allNavigationEntries.size(); i++) {
                NavEntry e = allNavigationEntries.get(i);
                if (e.searchText != null && e.searchText.contains(effectiveFilter)) {
                    visibleKeys.add(e.key);
                    String p = e.parentKey;
                    while (p != null) {
                        visibleKeys.add(p);
                        p = getParentKey(p);
                    }
                }
            }

            for (int i = 0; i < allNavigationEntries.size(); i++) {
                NavEntry e = allNavigationEntries.get(i);
                if (visibleKeys.contains(e.key)) {
                    navigationModel.addElement(e);
                }
            }
        }

        int selectedIndex = -1;
        if (keepCard != null) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                if (e.selectable && keepCard.equals(e.cardKey)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        if (selectedIndex < 0) {
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                //if (e.selectable) {
                    selectedIndex = i;
                    break;
                //}
            }
        }

        if (selectedIndex >= 0) {
            navigationList.setSelectedIndex(selectedIndex);
            if (navigationList.getFirstVisibleIndex()>selectedIndex || navigationList.getLastVisibleIndex()<selectedIndex) {
                navigationList.ensureIndexIsVisible(selectedIndex);
            }
            NavEntry entry = navigationModel.get(selectedIndex);
            persistedSelectedPanelKey = entry.key;
            showCard(entry.cardKey, entry.key);
        } else {
            showCard(CARD_EMPTY, null);
        }
    }

    private void applyHighlightsToCards(String normalizedFilter) {
        String f = normalizedFilter == null ? "" : normalizedFilter.trim();

        if (f.length() == 0) {
            for (Map.Entry<JLabel, String> e : originalLabelTexts.entrySet()) {
                e.getKey().setText(e.getValue());
            }
            for (Map.Entry<AbstractButton, String> e : originalButtonTexts.entrySet()) {
                e.getKey().setText(e.getValue());
            }
            return;
        }

        for (int i = 0; i < leafCardPanels.size(); i++) {
            applyHighlightsRecursive(leafCardPanels.get(i), f);
        }
    }

    private void applyHighlightsRecursive(Component c, String normalizedFilter) {
        if (c == null) {
            return;
        }

        if (c instanceof JLabel) {
            JLabel label = (JLabel) c;
            if (!originalLabelTexts.containsKey(label)) {
                originalLabelTexts.put(label, label.getText());
            }
            String original = originalLabelTexts.get(label);
            label.setText(highlightForDisplay(original, normalizedFilter));
        } else if (c instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) c;
            if (!originalButtonTexts.containsKey(button)) {
                originalButtonTexts.put(button, button.getText());
            }
            String original = originalButtonTexts.get(button);
            button.setText(highlightForDisplay(original, normalizedFilter));
        }

        if (c instanceof Container) {
            Component[] children = ((Container) c).getComponents();
            for (int i = 0; i < children.length; i++) {
                applyHighlightsRecursive(children[i], normalizedFilter);
            }
        }
    }
    
    /**
	 * Highlights occurrences of the normalized filter string within the original text for display purposes.
	 * @param originalText The original text to be displayed, which may contain HTML tags.
	 * @param normalizedFilter The normalized filter string used to determine which parts of the text to highlight.
	 *
	 * @return The modified text with highlighted occurrences of the filter string, or the original text if no highlights are applied.
	 */
    private String highlightForDisplay(String originalText, String normalizedFilter) {
        if (originalText == null) {
            return null;
        }

        String filter = normalizedFilter == null ? "" : normalizedFilter.trim().toLowerCase();
        if (filter.length() == 0) {
            return originalText;
        }

        String source = originalText;
        int from = 0;
        int to = source.length();

        while (from < to && Character.isWhitespace(source.charAt(from))) {
            from++;
        }

        int contentStart = from;
        if (contentStart < to && source.charAt(contentStart) == '<') {
            int gt = source.indexOf('>', contentStart);
            if (gt > contentStart) {
                String openTag = source.substring(contentStart + 1, gt).trim();
                if (openTag.length() >= 4 && openTag.regionMatches(true, 0, "html", 0, 4)) {
                    contentStart = gt + 1;

                    int end = to;
                    while (end > contentStart && Character.isWhitespace(source.charAt(end - 1))) {
                        end--;
                    }

                    if (end - 7 >= contentStart && source.charAt(end - 7) == '<' && source.charAt(end - 6) == '/') {
                        if (source.regionMatches(true, end - 5, "html", 0, 4) && source.charAt(end - 1) == '>') {
                            to = end - 7;
                        } else {
                            to = end;
                        }
                    } else {
                        to = end;
                    }

                    from = contentStart;
                } else {
                    from = 0;
                    to = source.length();
                }
            } else {
                from = 0;
                to = source.length();
            }
        } else {
            from = 0;
            to = source.length();
        }

        source = source.substring(from, to);

        ArrayList<String> chunks = new ArrayList<String>(16);
        ArrayList<Boolean> isTag = new ArrayList<Boolean>(16);

        Matcher m = TAG_PATTERN.matcher(source);
        int p = 0;
        while (m.find()) {
            if (m.start() > p) {
                chunks.add(source.substring(p, m.start()));
                isTag.add(Boolean.FALSE);
            }
            chunks.add(source.substring(m.start(), m.end()));
            isTag.add(Boolean.TRUE);
            p = m.end();
        }
        if (p < source.length()) {
            chunks.add(source.substring(p));
            isTag.add(Boolean.FALSE);
        }

        StringBuilder visible = new StringBuilder(source.length());
        ArrayList<Integer> runChunkIdx = new ArrayList<Integer>(chunks.size());
        ArrayList<Integer> runStart = new ArrayList<Integer>(chunks.size());
        ArrayList<Integer> runEnd = new ArrayList<Integer>(chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            if (!isTag.get(i).booleanValue()) {
                String t = chunks.get(i);
                if (t.length() > 0) {
                    int s = visible.length();
                    visible.append(t);
                    int e = visible.length();
                    runChunkIdx.add(Integer.valueOf(i));
                    runStart.add(Integer.valueOf(s));
                    runEnd.add(Integer.valueOf(e));
                }
            }
        }

        if (visible.length() == 0) {
            return originalText;
        }

        String visibleLower = visible.toString().toLowerCase();
        int flen = filter.length();
        int firstHit = visibleLower.indexOf(filter);
        if (firstHit < 0) {
            return originalText;
        }

        ArrayList<Integer> hitStarts = new ArrayList<Integer>(8);
        ArrayList<Integer> hitEnds = new ArrayList<Integer>(8);
        int searchPos = 0;
        while (true) {
            int h = visibleLower.indexOf(filter, searchPos);
            if (h < 0) {
                break;
            }
            hitStarts.add(Integer.valueOf(h));
            hitEnds.add(Integer.valueOf(h + flen));
            searchPos = h + flen;
        }

        int hitPtr = 0;
        for (int r = 0; r < runChunkIdx.size(); r++) {
            int cidx = runChunkIdx.get(r).intValue();
            String text = chunks.get(cidx);
            int rs = runStart.get(r).intValue();
            int re = runEnd.get(r).intValue();

            while (hitPtr < hitStarts.size() && hitEnds.get(hitPtr).intValue() <= rs) {
                hitPtr++;
            }
            if (hitPtr >= hitStarts.size() || hitStarts.get(hitPtr).intValue() >= re) {
                continue;
            }

            StringBuilder out = new StringBuilder(text.length() + 32);
            int localPos = 0;
            int hp = hitPtr;

            while (hp < hitStarts.size()) {
                int hs = hitStarts.get(hp).intValue();
                int he = hitEnds.get(hp).intValue();
                if (hs >= re) {
                    break;
                }
                if (he <= rs) {
                    hp++;
                    continue;
                }

                int os = hs > rs ? hs : rs;
                int oe = he < re ? he : re;
                int ls = os - rs;
                int le = oe - rs;

                if (ls > localPos) {
                    out.append(EfaUtil.escapeHtml(text.substring(localPos, ls)));
                }

                out.append("<span style='").append(HIGHLIGHT_STYLE).append("'><b>")
                   .append(EfaUtil.escapeHtml(text.substring(ls, le)))
                   .append("</b></span>");

                localPos = le;
                hp++;
            }

            if (localPos < text.length()) {
                out.append(EfaUtil.escapeHtml(text.substring(localPos)));
            }

            chunks.set(cidx, out.toString());
        }

        StringBuilder html = new StringBuilder(source.length() + 64);
        html.append("<html>");
        for (int i = 0; i < chunks.size(); i++) {
            if (isTag.get(i).booleanValue()) {
                html.append(chunks.get(i));
            } else {
                String t = chunks.get(i);
                if (t.indexOf("<span style='") >= 0) {
                    html.append(t);
                } else {
                    html.append(EfaUtil.escapeHtml(t));
                }
            }
        }
        html.append("</html>");
        return html.toString();
    }

    private String buildSearchText(Component c) {
        StringBuilder sb = new StringBuilder();
        appendSearchText(c, sb);
        return sb.toString();
    }

    private void appendSearchText(Component c, StringBuilder sb) {
        if (c == null) {
            return;
        }

        if (c instanceof JLabel) {
            String t = ((JLabel) c).getText();
            if (t != null && t.length() > 0) {
                sb.append(' ').append(t);
            }
        } else if (c instanceof AbstractButton) {
            String t = ((AbstractButton) c).getText();
            if (t != null && t.length() > 0) {
                sb.append(' ').append(t);
            }
        } else if (c instanceof JTextComponent) {
            String t = ((JTextComponent) c).getText();
            if (t != null && t.length() > 0) {
                sb.append(' ').append(t);
            }
        } else if (c instanceof JList) {
        	JList a=(JList)c;
            for (int i=0; i<a.getModel().getSize();i++) {
            	if (a.isSelectedIndex(i)) {
	            	String t = (String)a.getModel().getElementAt(i).toString();
	                if (t != null && t.length() > 0) {
	                    sb.append(' ').append(t);
	                }
            	}
            }            
        } else if (c instanceof JComboBox) {
        	JComboBox a = (JComboBox)c;
        	
        	Object x = a.getSelectedItem();
        	if (x!=null) {
        		String t = x.toString();
                if (t != null && t.length() > 0) {
                    sb.append(' ').append(t);
                }
        	}

        }

        if (c instanceof Container) {
            Component[] children = ((Container) c).getComponents();
            for (int i = 0; i < children.length; i++) {
                appendSearchText(children[i], sb);
            }
        }
    }

    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }
        String noTags = stripHtmlTags(text);
        return noTags.toLowerCase();
    }

    private String stripHtmlTags(String text) {
        if (text == null) {
            return "";
        }
        Matcher m = TAG_PATTERN.matcher(text);
        return m.replaceAll(" ");
    }

    private String getParentKey(String fullKey) {
        if (fullKey == null || fullKey.length() == 0) {
            return null;
        }
        int pos = fullKey.lastIndexOf(CATEGORY_SEPARATOR);
        if (pos < 0) {
            return null;
        }
        return fullKey.substring(0, pos);
    }

    /**
 	 * Creates a placeholder panel for a group category that has no selectable child items.
 	 * @param groupName 
 	 */ 
    private JPanel createGroupPlaceholderPanel(String groupName) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(groupName, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, BorderLayout.CENTER);
        return panel;
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
    
    protected int reduceInnerScrollPaneHeight() { 
        int height = 0;
        if (dataNorthPanel != null && dataNorthPanel.isVisible()) {
            height = dataNorthPanel.getPreferredSize().height;
        }
        if (topLevelPane != null && topLevelPane.isVisible() ) {
            height += topLevelPane.getPreferredSize().height;
        }
        return height;
    }

    private static class CursorState {
        private Window window;
        private Cursor windowCursor;

        private Component dialog;
        private Cursor dialogCursor;

        private Component filterField;
        private Cursor filterFieldCursor;

        private Component navigationList;
        private Cursor navigationListCursor;
    }    
    
    // Inner Classes for Navigation
    private static class NavEntry {
        private final String key;
        private final String label;
        private final int level;
        private final boolean group;
        private final boolean selectable;
        private final String cardKey;
        private final String parentKey;
        private final String searchText;

        private NavEntry(String key, String label, int level, boolean group, boolean selectable,
                         String cardKey, String parentKey, String searchText) {
            this.key = key;
            this.label = label;
            this.level = level;
            this.group = group;
            this.selectable = selectable;
            this.cardKey = cardKey;
            this.parentKey = parentKey;
            this.searchText = searchText;
        }

        public String toString() {
            return label;
        }
    }

    private static class NavEntryRenderer extends DefaultListCellRenderer {
        private static final Color TOP_GROUP_BG = new Color(230, 230, 230);
        private static final int NAV_INDENT_PER_LEVEL = 10;

        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (!(value instanceof NavEntry)) {
                return this;
            }

            NavEntry entry = (NavEntry) value;

            setText(entry.label);
            setBorder(BorderFactory.createEmptyBorder(4, 8 + (entry.level * NAV_INDENT_PER_LEVEL), 4, 8));

            Font f = getFont();
            if (entry.group) {
                setFont(f.deriveFont(Font.BOLD));
            } else {
                setFont(f.deriveFont(Font.PLAIN));
            }

            if (!isSelected && entry.group) {
                setBackground(TOP_GROUP_BG);
            }

            return this;
        }
    }

}
