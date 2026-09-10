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
import java.util.Set;
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
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
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
/*
 * BaseTabbedDialog is an abstract class that extends BaseDialog and provides a framework for creating dialogs with tabbed panes or left navigation.
 * It manages the display of items based on their categories and supports expert mode for advanced users.
 * The class handles the layout, navigation, and filtering of items, allowing subclasses to customize the behavior as needed.
 * 
 * There are two navigation modes: MODE_TABBED_PANE (0) and MODE_LEFT_NAVIGATION (1). The default mode is MODE_TABBED_PANE,
 * which is standard for all former efa dialogs. The left navigation mode is used e.g. in EfaConfigDialog and StatisticsEditDialog.
 * 
 * TabbedPane navigation mode displays items in a classic tabbed pane layout, and does not provide a search/filter functionality. 
 * 
 * Left navigation mode displays items in a list on the left side of the dialog, with a filter field for searching items.
 * - The navigation list shows categories and subcategories, and selecting an entry displays the corresponding card panel on the right.
 * 
 * - The filter field allows users to search for items by name or content, and the list updates dynamically based on the filter text.
 * 
 * - Filter field can be focused using Ctrl+F, and the filter is applied after a short delay (500 ms) to avoid excessive updates while typing.
 *   So it is even performant with large numbers of items, even on a Raspberry Pi 3B+ with all of the efaconfig items displayed.
 * 
 * - Filtering is case-insensitive and ignores HTML tags in item labels.
 * 
 * - The filtering looks for matches in both category names and the content of the card panels,
 *   and only displays entries that have matching items or subcategories.
 *   Filtering looks for matches in the JLabels (with highlighting), abstract buttons (with highlighting), 
 *   and without highligtning in the values of JTextComponents (JTextField, JTextArea, JPasswordField) JLists or JComboBoxes in the card panels.
 *   
 * - The navigation list supports keyboard navigation, including arrow keys and Enter to activate the selected entry.
 * 
 * 
 * - The breadcrumb label at the top of the right panel shows the current category path.
 * - The class also manages the persistence of the selected panel and filter text across dialog updates.
 */
public abstract class BaseTabbedDialog extends BaseDialog {

    private static final String CARD_EMPTY = "__empty__";
	public static final char CATEGORY_SEPARATOR = ':';
    public static final String CATEGORY_SEPARATOR_STRING = "" + CATEGORY_SEPARATOR;
    public static final String CATEGORY_COMMON = "%00%" + International.getString("Allgemein");
    public static final String CATEGORY_NONAME = "%00%NONAME";
    
    // Constants for Navigation Mode 
    protected static final int MODE_TABBED_PANE = 0;
    protected static final int MODE_LEFT_NAVIGATION = 1;
    protected static final int NAVIGATIONLIST_WIDTH = 220;
    private static final String HIGHLIGHT_STYLE = "background-color:#fff176; color:#000000;";
    private static final Color HIGHLIGHT_COLOR = new Color(0xFFF176);
    private static final Color FILTERFIELD_FILLED = new Color(255,255,204);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final int FILTER_DELAY_MS = 500;
    private static final int MIN_FILTER_LENGTH = 1;
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
    protected HashMap<String,HashMap> categoryHierarchy;
    protected HashMap<String,Vector<IItemType>> itemsPerCategory;
    protected Vector<IItemType> displayedGuiItems;
    protected HashMap<JPanel,String> panels;

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
        Vector<String> v = EfaUtil.split(keystring, CATEGORY_SEPARATOR);
        return v.toArray(new String[0]);  

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

    /**
	 * Sets the items to be displayed in the dialog and builds the category hierarchy and items per category.
	 * Also determines if there are any expert mode items present.
	 * 
	 * @param guiItems The vector of IItemType items to be displayed in the dialog.
	 */
    public void setItems(Vector<IItemType> guiItems) {
        this.allGuiItems = guiItems;
        expertModeItems = false;

        categoryHierarchy = new HashMap<String,HashMap>();    // category          -> sub-categories
        itemsPerCategory = new HashMap<String,Vector<IItemType>>(); // categoryhierarchy -> config items

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
            HashMap<String,HashMap> h = categoryHierarchy;
            for (int j=0; j<cats.length; j++) {
                HashMap hnext = h.get(cats[j]);
                if (hnext == null) {
                    hnext = new HashMap<String,HashMap>();
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
            HashMap<String,HashMap> h = categoryHierarchy;
            for (int j=0; j<cats.length; j++) {
                HashMap hnext = h.get(cats[j]);

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
    @Override
    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    /**
	 * Initializes the dialog by setting up the main panel layout, creating the data panel,
	 * adding the expert mode checkbox, and adding the data north panel to the data panel.
	 * @throws Exception if an error occurs during initialization.
	 */ 
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

    /**
	 * Updates the GUI by either reading values from the GUI or using existing values,
	 * and then rebuilding the GUI based on the current category hierarchy and items per category.
	 * 
	 * @param readValuesFromGui If true, values will be read from the GUI before rebuilding; otherwise, existing values will be used.
	 */
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
        
        panels = new HashMap<JPanel,String>();
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
    protected int recursiveBuildGui(HashMap<String,HashMap> categories,
                                   HashMap<String,Vector<IItemType>> items,
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
    protected int buildGuiWithTabbedPane(HashMap<String,HashMap> categories,
    		HashMap<String,Vector<IItemType>> items,
                                   String catKey,
                                   JComponent currentPane,
                                   String selectedPanel, int otherPanelHeight) {
        int itmcnt = 0;
        int pos = (selectedPanel != null && !selectedPanel.isEmpty() ? selectedPanel.indexOf(CATEGORY_SEPARATOR) : -1);
        String selectThisCat = (pos < 0 ? selectedPanel : selectedPanel.substring(0,pos));
        String selectNextCat = (pos < 0 ? null : selectedPanel.substring(pos+1));

        Object[] cats = categories.keySet().toArray();
        Arrays.sort(cats);
        for (int i=0; i<cats.length; i++) {
            String key = (String)cats[i];
            String thisCatKey = (catKey.isEmpty() ? key : makeCategory(catKey, key));
            String catName = getCatName(thisCatKey);
            HashMap<String,HashMap> subCat = categories.get(key);
            
            if (subCat.size() != 0) {
                JTabbedPane subTabbedPane = new JTabbedPane();
                // buildGUiWithTabbedPane also calls buildLeafPanel() for leaf panels, so we don't need to do it here
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
    protected int buildGuiWithLeftNavigation(HashMap<String,HashMap> categories,
                                          HashMap<String,Vector<IItemType>> items,
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

        if (persistedFilterText != null && !persistedFilterText.isEmpty()) {
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
    private JPanel buildLeafPanel(HashMap<String, Vector<IItemType>> items, String thisCatKey) {
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
    private int collectNavAndCards(HashMap<String, HashMap> categories,
    							   HashMap<String, Vector<IItemType>> items,
                                   String catKey,
                                   int otherPanelHeight,
                                   int level) {
        int itmcnt = 0;
        Object[] cats = categories.keySet().toArray();
        Arrays.sort(cats);

        for (int i = 0; i < cats.length; i++) {
            String key = (String) cats[i];
            String thisCatKey = (catKey.isEmpty() ? key : makeCategory(catKey, key));
            String catName = getCatName(thisCatKey);
            HashMap<String, HashMap> subCat = categories.get(key);

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
                    navigationFilterField.setBackground(FILTERFIELD_FILLED);
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

        if (!filter.isEmpty() && filter.length() < MIN_FILTER_LENGTH) {
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

        if (desiredPanel != null && !desiredPanel.isEmpty()) {
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

        if (desiredCard != null && !desiredCard.isEmpty()) {
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

        if (selectedPanel != null && !selectedPanel.isEmpty()) {
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

        if (fullCategoryKey == null || fullCategoryKey.isEmpty()) {
            breadcrumbLabel.setText(" "+this.getTitle());
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
        sb.append(" "+this.getTitle());
        for (int i = 0; i < parts.size(); i++) {
            partialKey = (i == 0) ? parts.get(i) : makeCategory(partialKey, parts.get(i));
            String name = getCatName(partialKey);
            if (name == null || name.isEmpty()) {
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

    /**
	 * Splits the full category key into its individual parts based on the defined category separator.
	 *  * @param fullCategoryKey The full category key to split.
	 *  @return A list of strings representing the individual parts of the full category key.
	 *  */
    private List<String> splitCategoryKey(String fullCategoryKey) {
        ArrayList<String> parts = new ArrayList<String>(8);
        if (fullCategoryKey == null || fullCategoryKey.isEmpty()) {
            return parts;
        }
        if (CATEGORY_SEPARATOR_STRING == null || CATEGORY_SEPARATOR_STRING.isEmpty()) {
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

    /**
     * Applies the navigation filter to the navigation list and card panels based on the provided filter string
     * and updates the selection in the navigation list accordingly. If the filter string is null, it is treated as an empty string.
     * 
     * Optimizations:
     * - Uses cached parent keys to avoid repeated lookups
     * - Builds a parent cache in a single pass
     * - Uses ArrayList instead of HashSet for better performance with small collections
     * - Combines selection logic to avoid multiple iterations
     * 
     * @param filter The filter string to apply to the navigation list and card panels.
     */
    private void applyNavigationFilter(String filter) {
        String effectiveFilter = filter == null ? "" : filter;
        
        applyHighlightsToCards(effectiveFilter);
        
        String keepCard = lastSelectedCardKey;
        if (keepCard == null && navigationList != null && navigationList.getSelectedValue() != null) {
            keepCard = navigationList.getSelectedValue().cardKey;
        }
        
        navigationModel.clear();
        
        // If no filter, show all entries
        if (effectiveFilter.isEmpty()) {
        	allNavigationEntries.forEach(navigationModel::addElement);
        } else {
            // Build parent hierarchy cache in one pass to avoid repeated getParentKey() calls
            Map<String, String> parentCache = new HashMap<>(allNavigationEntries.size());
            for (NavEntry e : allNavigationEntries) {
                if (e.parentKey != null ) {
                    parentCache.put(e.key, e.parentKey); //insert or update item in parentCache
                }
            }
            
            // Find matching entries and collect visible keys
            Set<String> visibleKeys = new HashSet<>(allNavigationEntries.size() / 2);
            for (NavEntry e : allNavigationEntries) {
                if (e.searchText != null && e.searchText.contains(effectiveFilter)) {
                    // Add entry and all its ancestors
                    addEntryAndAncestors(e.key, visibleKeys, parentCache);
                }
            }
            
            // Add filtered entries to model
            for (NavEntry e : allNavigationEntries) {
                if (visibleKeys.contains(e.key)) {
                    navigationModel.addElement(e);
                }
            }
        }
        
        // Find and select the appropriate entry
        selectAppropriateEntry(keepCard);
    }

    /**
     * Helper method to add an entry and all its ancestor entries to the visible keys set.
     * Uses a cached parent map to avoid repeated string lookups.
     * 
     * @param key The key to add
     * @param visibleKeys The set to add keys to
     * @param parentCache Map of key -> parentKey for fast lookups
     */
    private void addEntryAndAncestors(String key, Set<String> visibleKeys, Map<String, String> parentCache) {
        String current = key;
        while (current != null) {
            if (!visibleKeys.add(current)) {
                // Already added, stop to avoid cycles
                break;
            }
            current = parentCache.get(current);
        }
    }

    /**
     * Helper method to find and select the appropriate entry in the navigation list.
     * Tries to keep the previous selection, then falls back to first entry.
     * 
     * @param keepCard The card key to try to keep selected
     */
    private void selectAppropriateEntry(String keepCard) {
        int selectedIndex = -1;
        
        // First try to find the previously selected card
        if (keepCard != null && !keepCard.isEmpty()) {
            selectedIndex = findSelectableEntryByCardKey(keepCard);
        }
        
        // If not found, select first entry
        if (selectedIndex < 0 && navigationModel.size() > 0) {
            selectedIndex = 0;
        }
        
        // Apply selection
        if (selectedIndex >= 0) {
            navigationList.setSelectedIndex(selectedIndex);
            ensureIndexIsVisible(selectedIndex);
            NavEntry entry = navigationModel.get(selectedIndex);
            persistedSelectedPanelKey = entry.key;
            showCard(entry.cardKey, entry.key);
        } else {
            showCard(CARD_EMPTY, null);
        }
    }

    /**
     * Helper method to find a selectable entry by card key in the current model.
     * 
     * @param cardKey The card key to search for
     * @return The index of the entry, or -1 if not found
     */
    private int findSelectableEntryByCardKey(String cardKey) {
        for (int i = 0; i < navigationModel.size(); i++) {
            NavEntry e = navigationModel.get(i);
            if (e.selectable && cardKey.equals(e.cardKey)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Helper method to ensure an index is visible in the navigation list.
     * Extracted to reduce code duplication.
     * 
     * @param index The index to make visible
     */
    private void ensureIndexIsVisible(int index) {
        if (navigationList != null && 
            (navigationList.getFirstVisibleIndex() > index || navigationList.getLastVisibleIndex() < index)) {
            navigationList.ensureIndexIsVisible(index);
        }
    }

    
    /**
	 * Applies highlights to the text of JLabel and AbstractButton components within the card panels based on
	 * the provided normalized filter string. If the filter string is empty, it restores the original text of the components.
	 * @param normalizedFilter The normalized filter string used to determine which parts of the text to highlight.
	 * */
    private void applyHighlightsToCards(String normalizedFilter) {
        String f = normalizedFilter == null ? "" : normalizedFilter.trim();

        if (f.isEmpty()) {
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

    /**
	 * Recursively applies highlights to text components and data-containing components (JTextArea, JComboBox, JList).
	 * This method traverses child components within containers and applies highlighting where appropriate.
	 * 
	 * Performance considerations:
	 * - Early termination for null components
	 * - Minimal memory allocation for non-matching components
	 * - Direct component checking without intermediate storage
	 * @param c The component to process for highlights.
	 * @param normalizedFilter The normalized filter string used to determine which parts of the text to highlight.
	 */
    private void applyHighlightsRecursive(Component c, String normalizedFilter) {
        // Early exit für null-Komponenten
        if (c == null) {
            return;
        }

        // Prüfe ob der Komponenten-Text den Filter enthält
        String componentText = getComponentSearchableText(c);
        boolean matchesFilter = componentText != null && 
                               componentText.toLowerCase().contains(normalizedFilter.toLowerCase());

        // Highlights für JLabel anwenden
        if (c instanceof JLabel) {
            JLabel label = (JLabel) c;
            if (!originalLabelTexts.containsKey(label)) {
                originalLabelTexts.put(label, label.getText());
            }
            String original = originalLabelTexts.get(label);
            // Nur hervorheben wenn Filter vorhanden und Match gefunden
            if (matchesFilter) {
                label.setText(highlightForDisplay(original, normalizedFilter));
            } else if (original != null) {
                label.setText(original);
            }
        } 
        // Highlights für AbstractButton anwenden
        else if (c instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) c;
            if (!originalButtonTexts.containsKey(button)) {
                originalButtonTexts.put(button, button.getText());
            }
            String original = originalButtonTexts.get(button);
            // Nur hervorheben wenn Filter vorhanden und Match gefunden
            if (matchesFilter) {
                button.setText(highlightForDisplay(original, normalizedFilter));
            } else if (original != null) {
                button.setText(original);
            }
        } 
        // apply highlights for jtextarea, but not for passwordfield.
        // otherwise, you could determine the contents of the passwortfield by applying filters consecutively.
        else if ((c instanceof JTextField) && (!(c instanceof JPasswordField))) {
            JTextField textfield = (JTextField) c;
            String text = textfield.getText();
            // Visuelle Hervorhebung für JTextArea durch Border oder Hintergrundfarbe
            if (matchesFilter && text != null && !text.isEmpty()) {
                // Highlight durch Hintergrundfarbe
                textfield.setBackground(HIGHLIGHT_COLOR); // Gelber Hintergrund wie bei Labels
                textfield.setOpaque(true);
            } else {
                // Zurücksetzen auf Standard-Hintergrundfarbe
                textfield.setBackground(UIManager.getColor("TextField.background"));
                textfield.setOpaque(true);
            }
        } 
        // Highlights für JTextArea anwenden
        else if (c instanceof JTextArea) {
            JTextArea textArea = (JTextArea) c;
            String text = textArea.getText();
            // Visuelle Hervorhebung für JTextArea durch Border oder Hintergrundfarbe
            if (matchesFilter && text != null && !text.isEmpty()) {
                // Highlight durch Hintergrundfarbe
                textArea.setBackground(HIGHLIGHT_COLOR); // Gelber Hintergrund wie bei Labels
                textArea.setOpaque(true);
            } else {
                // Zurücksetzen auf Standard-Hintergrundfarbe
                textArea.setBackground(UIManager.getColor("TextArea.background"));
                textArea.setOpaque(true);
            }
        } 
        // Highlights für JComboBox anwenden
        else if (c instanceof JComboBox) {
            JComboBox<?> comboBox = (JComboBox<?>) c;
            // Visuelle Hervorhebung für JComboBox durch Hintergrundfarbe
            if (matchesFilter) {
                // Highlight durch Hintergrundfarbe
                comboBox.setBackground(HIGHLIGHT_COLOR); // Gelber Hintergrund
                comboBox.setOpaque(true);
            } else {
                // Zurücksetzen auf Standard-Hintergrundfarbe
                comboBox.setBackground(UIManager.getColor("ComboBox.background"));
                comboBox.setOpaque(true);
            }
        } 
        // Highlights für JList anwenden
        else if (c instanceof JList) {
            JList<?> list = (JList<?>) c;
            
            int[] selectedIndices = list.getSelectedIndices();
            // Visuelle Hervorhebung für JList durch Hintergrundfarbe
            if (matchesFilter && selectedIndices.length > 0) {
                // Highlight durch Hintergrundfarbe
                list.setBackground(HIGHLIGHT_COLOR); // Gelber Hintergrund
                list.setOpaque(true);
            } else {
                // Zurücksetzen auf Standard-Hintergrundfarbe
                list.setBackground(UIManager.getColor("List.background"));
                list.setOpaque(true);
            }
        }

        // Rekursiv alle Kind-Komponenten verarbeiten
        if (c instanceof Container) {
            Component[] children = ((Container) c).getComponents();
            for (int i = 0; i < children.length; i++) {
                applyHighlightsRecursive(children[i], normalizedFilter);
            }
        }
    }

    /**
     * Retrieves the searchable text content from a component.
     * This method extracts text that can be searched from various component types.
     * 
     * Performance optimization:
     * - Single method to get searchable text instead of multiple instanceof checks
     * - Avoids null pointer exceptions through null-safe design
     * 
     * @param c The component to extract searchable text from.
     * @return The searchable text content, or null if the component has no searchable text.
     */
    private String getComponentSearchableText(Component c) {
        // Frühe Rückgabe für null-Komponenten
        if (c == null) {
            return null;
        }

        // JLabel: Text direkt auslesen
        if (c instanceof JLabel) {
        	return originalLabelTexts.get(c);
            //return (JLabel) c).getText();
        } 
        // AbstractButton: Text direkt auslesen
        else if (c instanceof AbstractButton) {
        	return originalButtonTexts.get(c);
            //return ((AbstractButton) c).getText();
        } 
        // JTextComponent (JTextField, JTextArea, JPasswordField): Text auslesen
        else if (c instanceof JTextComponent) {
            return ((JTextComponent) c).getText();
        } 
        // JList: Ausgewählte Elemente auslesen
        else if (c instanceof JList<?>) {
            JList<?> list = (JList<?>) c;
            
            // Wenn keine Elemente enthalten, null zurückgeben (Performance)
            if (list.getModel().getSize() == 0) {
                return null;
            }
            
            // Alle Elemente als Text zusammenfassen
            StringBuilder sb = new StringBuilder();
            int size = list.getModel().getSize();
            for (int i=0; i<size; i++) {
                Object item = list.getModel().getElementAt(i);
                if (item != null) {
                    sb.append(" ").append(item.toString());
                }
            }
            return sb.toString();
        } 
        // JComboBox: Ausgewähltes Element auslesen
        else if (c instanceof JComboBox) {
            JComboBox<?> comboBox = (JComboBox<?>) c;
            Object selectedItem = comboBox.getSelectedItem();
            
            // Wenn nichts ausgewählt, null zurückgeben (Performance)
            if (selectedItem == null) {
                return null;
            }
            
            return selectedItem.toString();
        }

        // Unbekannter Komponenten-Typ
        return null;
    }
    
    /**
	 * Highlights occurrences of the normalized filter string within the original text for display purposes.
	 * @param originalText The original text to be displayed, which may contain HTML tags.
	 * @param normalizedFilter The normalized filter string used to determine which parts of the text to highlight.
	 *
	 * @return The modified text with highlighted occurrences of the filter string, or the original text if no highlights are applied.
	 */
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
        if (filter.isEmpty()) {
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
                if (!t.isEmpty()) {
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
        
        // Finde alle Treffer EINMALIG
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
            searchPos = h + 1; // WICHTIG: Nicht h + flen, sondern h + 1, um überlappende Treffer zu finden
        }

        if (hitStarts.isEmpty()) {
            return originalText;
        }

        // Wende Hervorhebungen auf Chunks an
        for (int r = 0; r < runChunkIdx.size(); r++) {
            int cidx = runChunkIdx.get(r).intValue();
            String text = chunks.get(cidx);
            int rs = runStart.get(r).intValue();
            int re = runEnd.get(r).intValue();

            // Finde Treffer, die in diesen Run fallen
            ArrayList<Integer> relevantHits = new ArrayList<Integer>();
            for (int hp = 0; hp < hitStarts.size(); hp++) {
                int hs = hitStarts.get(hp).intValue();
                int he = hitEnds.get(hp).intValue();
                
                // Treffer überlappt mit diesem Run?
                if (hs < re && he > rs) {
                    relevantHits.add(Integer.valueOf(hp));
                }
            }

            if (relevantHits.isEmpty()) {
                continue;
            }

            StringBuilder out = new StringBuilder(text.length() + 32);
            int localPos = 0;

            for (int hi = 0; hi < relevantHits.size(); hi++) {
                int hp = relevantHits.get(hi).intValue();
                int hs = hitStarts.get(hp).intValue();
                int he = hitEnds.get(hp).intValue();

                // Berechne Positionen relativ zu diesem Run
                int os = Math.max(hs, rs) - rs;
                int oe = Math.min(he, re) - rs;

                if (os > localPos) {
                    out.append(EfaUtil.escapeHtml(text.substring(localPos, os)));
                }

                out.append("<span style='").append(HIGHLIGHT_STYLE).append("'><b>")
                   .append(EfaUtil.escapeHtml(text.substring(os, oe)))
                   .append("</b></span>");

                localPos = oe;
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


    /**
	 * Builds a search text string for the specified component and its child components.
	 *  * @param c The component to build the search text from.
	 *  @return A string containing the concatenated search text from the component and its child components.
	 *  */
    private String buildSearchText(Component c) {
        StringBuilder sb = new StringBuilder();
        appendSearchText(c, sb);
        return sb.toString();
    }

    /**
	 * Appends the search text from the specified component and its child components to the provided String
	 * Builder. It handles JLabel, AbstractButton, JTextComponent, JList, and JComboBox components, as well as containers.
	 * @param c The component to extract search text from.
	 * @param sb The StringBuilder to append the search text to.
	 * */
    private void appendSearchText(Component c, StringBuilder sb) {
        if (c == null) {
            return;
        }

        if (c instanceof JLabel) {
            String t = ((JLabel) c).getText();
            if (t != null && !t.isEmpty()) {
                sb.append(' ').append(t);
            }
        } else if (c instanceof AbstractButton) {
            String t = ((AbstractButton) c).getText();
            if (t != null && !t.isEmpty()) {
                sb.append(' ').append(t);
            }
        } else if (c instanceof JTextComponent) {
        	//use the value of the textcomponent
            String t = ((JTextComponent) c).getText();
            if (t != null && !t.isEmpty()) {
                sb.append(' ').append(t);
            }
        } else if (c instanceof JList<?>) {
        	// use selected indexes of a list
            JList<?> list = (JList<?>) c;
            int[] selectedIndices = list.getSelectedIndices();
            for (int i : selectedIndices) {
                Object item = list.getModel().getElementAt(i);
                if (item != null) {
                    String t = item.toString();
                    if (!t.isEmpty()) {
                        sb.append(' ').append(t);
                    }
                }
            }
        } else if (c instanceof JComboBox) {
        	JComboBox a = (JComboBox)c;
        	Object x = a.getSelectedItem();
        	if (x!=null) {
        		String t = x.toString();
                if (!t.isEmpty()) {
                    sb.append(' ').append(t);
                }
        	}

        }

        if (c instanceof Container) {
            Component[] children = ((Container) c).getComponents();
            for (Component child : children) {
                appendSearchText(child, sb);
            }

        }
    }
    /**
	 * Normalizes the search text by stripping HTML tags and converting it to lowercase.
	 * @param text The input text to normalize.
	 * @return A normalized string suitable for search operations.	
	 * */
    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }
        String noTags = stripHtmlTags(text);
        return noTags.toLowerCase();
    }

    /**
	 * Strips HTML tags from the input text using a regular expression pattern.
	 * @param text The input text from which to remove HTML tags.
	 * @return A string with HTML tags removed, or an empty string if the input is null.
	 * */
    private String stripHtmlTags(String text) {
        if (text == null) {
            return "";
        }
        Matcher m = TAG_PATTERN.matcher(text);
        return m.replaceAll(" ");
    }

    /**
	 * Retrieves the parent key from the provided full category key by finding the last occurrence of the
	 * CATEGORY_SEPARATOR. If the separator is not found, it returns null.
	 *  * @param fullKey The full category key from which to extract the parent key.
	 *  @return The parent key as a string, or null if the full key is null, empty, or has no parent.
	 *  */
    private String getParentKey(String fullKey) {
        if (fullKey == null || fullKey.isEmpty()) {
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
    
    /**
	 * Retrieves values from the GUI components and checks if any of the values have changed.
	 * @return true if any values have changed, false otherwise.
	 * */
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

    /**
	 * Updates the GUI components based on the current values of the underlying data model.
	 * */
    void expertModeChanged(ActionEvent e) {
        if (expertMode.isSelected()) {
            expertMode.setForeground(Color.red);
        } else {
            expertMode.setForeground(Color.black);
        }
        expertMode.setVisible(expertModeItems);
        updateGui();
    }

    /**
	 * Updates the GUI components based on the current values of the underlying data model.
	 * */
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

    /**
	 * Retrieves the list of all GUI items managed by this dialog.
	 * @return A Vector containing all IItemType instances managed by this dialog.
	 * */
    public Vector<IItemType> getItems() {
        return allGuiItems;
    }

    /**
	 * Retrieves the IItemType instance corresponding to the specified name.
	 * @param name The name of the item to retrieve.
	 * @return The IItemType instance with the specified name, or null if no such item exists.
	 * */
    public IItemType getItem(String name) {
        for (int i=0; i<allGuiItems.size(); i++) {
            if (allGuiItems.get(i).getName().equals(name)) {
                return allGuiItems.get(i);
            }
        }
        return null;
    }
    
    /**
	 * Reduces the height of the inner scroll pane based on the visibility and preferred sizes of
	 * the dataNorthPanel and topLevelPane. It calculates the total height to reduce from the scroll pane.
	 * @return The total height to reduce from the inner scroll pane.
	 * */
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

    /**
	 * Inner class to hold the state of cursors for various components in the dialog.
	 * */
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
    
    /**
	 * Custom ListCellRenderer for rendering navigation entries in the navigation list.
	 * */
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
