/**
* Title:        efa - elektronisches Fahrtenbuch fuer Ruderer
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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeHashtable;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedLabel;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;

// @i18n complete
public class EfaConfigDialog extends BaseTabbedDialog {

    private static final String CARD_EMPTY = "__empty__";
    private static final String GROUP_CARD_PREFIX = "__group__::";
    private static final int NAV_INDENT_PER_LEVEL = 10;
    private static final String BREADCRUMB_SEPARATOR = "  >  ";//" \u203A ";
    private static final String ACTION_NAV_ACTIVATE = "nav.activate";
    private static final String ACTION_NAV_FOCUS_FILTER = "nav.focusFilter";
    private static final String ACTION_FILTER_ARROW_UP = "filter.arrowUp";
    private static final String ACTION_FILTER_ARROW_DOWN = "filter.arrowDown";
    private static final String HIGHLIGHT_STYLE = "background-color:#fff176; color:#000000;";//"background-color:#fff176; font:bold; color:#000000;";
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final int FILTER_DELAY_MS = 500;
    private static final int MIN_FILTER_LENGTH = 2;
    private static final int NAVIGATIONLIST_WIDTH = 220;

    private EfaConfig myEfaConfig;

    private JList<NavEntry> navigationList;
    private DefaultListModel<NavEntry> navigationModel;
    private List<NavEntry> allNavigationEntries;
    private JTextField navigationFilterField;
    private Timer navigationFilterTimer;
    private RoundedLabel breadcrumbLabel;
    private JPanel rightContentPanel;


    private JPanel cardPanel;
    private CardLayout cardLayout;
    private String lastSelectedCardKey;

    // Persisted UI state across rebuilds
    private String persistedFilterText = "";
    private String persistedSelectedCardKey = null;
    private String persistedSelectedPanelKey = null;

    private final List<JPanel> leafCardPanels = new ArrayList<JPanel>();
    private final Map<JLabel, String> originalLabelTexts = new HashMap<JLabel, String>();
    private final Map<AbstractButton, String> originalButtonTexts = new HashMap<AbstractButton, String>();
	private Color navigationFilterFieldBackground = Color.white;

    public EfaConfigDialog(Frame parent, EfaConfig efaConfig) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this.myEfaConfig = efaConfig;
    }

    public EfaConfigDialog(JDialog parent, EfaConfig efaConfig) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this.myEfaConfig = efaConfig;
    }

    public EfaConfigDialog(JDialog parent, EfaConfig efaConfig, String selectedPanel) {
        super(parent,
              International.getString("Konfiguration"),
              International.getStringWithMnemonic("Speichern"),
              efaConfig.getGuiItems(), true);
        this._selectedPanel = selectedPanel;
        this.myEfaConfig = efaConfig;
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        super.iniDialog();
        closeButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        closeButton.setIconTextGap(10);
    }

    /**
	 * Handles the action performed when the close button is clicked.
	 * Saves the current values from the GUI to the EfaConfig object and performs necessary checks.
	 * Also clears cached component-to-text mappings when the dialog is closing.
	 * 
	 * @param e The ActionEvent triggered by clicking the close button.
	 */
    public void closeButton_actionPerformed(ActionEvent e) {
        getValuesFromGui();
        synchronized (myEfaConfig) {
            for (int i = 0; i < allGuiItems.size(); i++) {
                IItemType item = allGuiItems.get(i);
                if (item.isChanged()) {
                    myEfaConfig.setValue(item.getName(), item.toString());
                }
            }
        }
        myEfaConfig.checkNewConfigValues();
        myEfaConfig.setExternalParameters(true);
        myEfaConfig.checkForRequiredPlugins();
        // Release cached component->text mappings when dialog is closing.
        originalLabelTexts.clear();
        originalButtonTexts.clear();

        super.closeButton_actionPerformed(e);
        setDialogResult(true);
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesBoat() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesBoat().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesNumSeats() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesNumSeats().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesRigging() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesRigging().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesCoxing() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesCoxing().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesGender() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesGender().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesSession() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesSession().getName());
    }

    @SuppressWarnings("unchecked")
    public ItemTypeHashtable<String> getTypesStatus() {
        return (ItemTypeHashtable<String>) getItem(myEfaConfig.getValueTypesStatus().getName());
    }

    /**
	 * Recursively builds the navigation list and card panels for the configuration GUI.
	 *
	 * @param categories      The hashtable of categories to process.
	 * @param items           The hashtable of items associated with each category.
	 * @param catKey          The current category key being processed.
	 * @param currentPane     The current pane to which the navigation and cards will be added.
	 * @param selectedPanel   The key of the panel that should be selected initially.
	 * @param otherPanelHeight The height of other panels, used for layout calculations.
	 * @return The total number of selectable items processed.
	 */
    protected int recursiveBuildGui(Hashtable<String, Hashtable> categories,
                                    Hashtable<String, Vector<IItemType>> items,
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

        //Recursively build navigation entries and card panels for all categories and items.
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
        //myScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
        
        rightContentPanel = new JPanel(new BorderLayout(0, 6));
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

        // Restore filter text before applying filter model
        if (persistedFilterText != null && persistedFilterText.length() > 0) {
            navigationFilterField.setText(persistedFilterText);
        }

        applyNavigationFilterWithMinLength();

        // Try to restore the exact panel/card after filter is active.
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
        });
        
        navigationFilterField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(FocusEvent e) {
            	if (!navigationFilterField.getText().isEmpty()) {
            		navigationFilterField.setBackground(new Color(255,255,204)); //some white yellow so it is clear that some stuff is in this field.

            	} else {	
            		// use standard background color. for flatlaf, this is not white as it is for the other lafs.
            		navigationFilterField.setBackground(navigationFilterFieldBackground); 
            	}
            }
        });		
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

        // 1) Try exact panel key first
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

        // 2) Fallback: match by card key
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
	 * Rebuilds the configuration GUI by clearing the displayed GUI items, persisting the current
	 * filter text, stopping any running navigation filter timer, and removing all components from the dialog.
	 * This method is typically called when the configuration has changed and the GUI needs to be refreshed.
	 */ 
    public void rebuildConfigGui() {
        displayedGuiItems.clear();

        // Capture current state before components are disposed/rebuilt.
        persistCurrentFilterText();

        if (navigationFilterTimer != null && navigationFilterTimer.isRunning()) {
            navigationFilterTimer.stop();
        }

        persistedSelectedCardKey = lastSelectedCardKey;
        persistedSelectedPanelKey = null;
        if (navigationList != null) {
            NavEntry selected = navigationList.getSelectedValue();
            if (selected != null) {
                persistedSelectedPanelKey = selected.key;
                if (selected.cardKey != null) {
                    persistedSelectedCardKey = selected.cardKey;
                }
            }
        }

        removeAll();
        revalidate();
        repaint();
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

                    String parentKey = getParentKey(thisCatKey);
                    String cardContentSearch = buildSearchText(panel);
                    String leafSearchText = normalizeSearchText(catName + " " + cardContentSearch);

                    allNavigationEntries.add(new NavEntry(
                            thisCatKey, catName, level, false, true, thisCatKey, parentKey, leafSearchText));
                    itmcnt++;
                }
            }
        }

        return itmcnt;
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
	 * Builds a leaf panel for the specified category key, containing the items associated with that category
	 * and their corresponding GUI components.
	 * 
	 * @param items The hashtable of items associated with each category.
	 * @param thisCatKey The category key for which to build the leaf panel.
	 * 
	 * @return A JPanel representing the leaf panel for the specified category, or null if there are no items to display.
	 */   
    private JPanel buildLeafPanel(Hashtable<String, Vector<IItemType>> items, String thisCatKey) {
        JPanel panel = new JPanel();
        panels.put(panel, thisCatKey);
        JPanel innerPanel = new JPanel();

        JScrollPane scrollPane = new JScrollPane(innerPanel);
        scrollPane.setPreferredSize(EfaGuiUtils.getTabPanelPreferredSizeEfaConfig(this, NAVIGATIONLIST_WIDTH+50, 200));
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
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        innerPanel.add(filler, new GridBagConstraints(
                0, y, 12, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        
        return y > 1 ? panel : null;
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
	 * Applies the navigation filter to the navigation list and card panels based on the provided filter string
	 * and updates the selection in the navigation list accordingly.
	 * 
	 * @param filter The filter string to apply to the navigation entries and card panels.
	 */
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
                if (e.selectable) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        if ((filter!=null&& filter.trim().length()>0 && selectedIndex >= 1)
        		|| ((filter == null || (filter !=null && filter.trim().length()==0) && selectedIndex>1))){
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

    /**
	 * Applies highlights to the text of all JLabel and AbstractButton components within the leaf card panels
	 * based on the provided normalized filter string. If the filter is empty, it restores the original text without highlights.
	 *
	 *  @param normalizedFilter The normalized filter string used to determine which parts of the text to highlight.
	 *
	 */
    private void applyHighlightsToCards(String normalizedFilter) {
        String f = normalizedFilter == null ? "" : normalizedFilter.trim();

        // If filter is empty, restore original texts and clear highlight markup.
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

    /**
	 * Recursively applies highlights to the text of JLabel and AbstractButton components within the given component
	 * based on the provided normalized filter string. If the filter is empty, it restores the original text without highlights.
	 * 
	 * @param c The component to process, which may contain JLabel and AbstractButton components.
	 * @param normalizedFilter The normalized filter string used to determine which parts of the text to highlight.
	 * */
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

        // Remove outer <html>...</html> wrapper if present (case-insensitive), keep content only.
        String source = originalText;
        int from = 0;
        int to = source.length();

        // do skip leading whitespace
        while (from < to && Character.isWhitespace(source.charAt(from))) {
            from++;
        }

        // detect opening <html ...>
        int contentStart = from;
        if (contentStart < to && source.charAt(contentStart) == '<') {
            int gt = source.indexOf('>', contentStart);
            if (gt > contentStart) {
                String openTag = source.substring(contentStart + 1, gt).trim(); // without '<' '>'
                if (openTag.length() >= 4 && openTag.regionMatches(true, 0, "html", 0, 4)) {
                    contentStart = gt + 1;

                    // trim trailing whitespace first
                    int end = to;
                    while (end > contentStart && Character.isWhitespace(source.charAt(end - 1))) {
                        end--;
                    }

                    // detect closing </html>
                    if (end - 7 >= contentStart && source.charAt(end - 7) == '<' && source.charAt(end - 6) == '/') {
                        // quick case-insensitive check for "</html>"
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

        // Tokenize into tag/text chunks, preserve tags as-is.
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

        // Build visible text and map text-runs back to chunk indices.
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

        // Collect hit ranges in visible coordinates.
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

        // Rewrite only overlapping text runs.
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

        // Reassemble once, with exactly one outer html wrapper.
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
            for (int i = 0; i < navigationModel.size(); i++) {
                NavEntry e = navigationModel.get(i);
                //if (e.selectable) {
                    indexToSelect = i;
                    break;
                //}
            }
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
	            sb.append(" "); // indent the label text by one space
	        }
	        sb.append(name);
	    }
	
	    return sb.toString();
    }
    
    /** 
     * Splits the full category key into its individual parts based on the defined category separator.
     * @param fullCategoryKey
     * @return A list of strings representing the individual parts of the full category key.
     */
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

    /**
     * Retrieves the parent key of the given full category key by finding the last occurrence of the category separator.
     * @param fullKey
     * @return The parent key of the given full category key, or null if there is no parent.
     */
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
     * Builds a search text string by recursively traversing the given component and its children,
     * extracting text from JLabel, AbstractButton, and JTextComponent instances, and concatenating it into a single string.
     * 
     * @param c The component to traverse and extract text from.
     * @return A string containing the concatenated text from the component and its children, suitable for search purposes.
     */
    private String buildSearchText(Component c) {
        StringBuilder sb = new StringBuilder();
        appendSearchText(c, sb);
        return sb.toString();
    }

    /**
	 * Recursively appends the text from the given component and its children to the provided StringBuilder.
	 * 
	 * @param c The component to traverse and extract text from.
	 * @param sb The StringBuilder to append the extracted text to.
	 */
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
        }

        if (c instanceof Container) {
            Component[] children = ((Container) c).getComponents();
            for (int i = 0; i < children.length; i++) {
                appendSearchText(children[i], sb);
            }
        }
    }

    /**
     * Normalizes the given search text by stripping HTML tags and converting it to lowercase.
     * @param text The search text to normalize.
     * @return A normalized version of the search text, suitable for case-insensitive searching.
     */
    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }
        String noTags = stripHtmlTags(text);
        return noTags.toLowerCase();
    }

    /**
     * Strips HTML tags from the given text using a regular expression pattern.
     * 
     * @param text The text from which to remove HTML tags.
     * 
     * @return The text with HTML tags removed, or an empty string if the input text is null.
     */
    private String stripHtmlTags(String text) {
        if (text == null) {
            return "";
        }
        Matcher m = TAG_PATTERN.matcher(text);
        return m.replaceAll(" ");
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
	 * It handles indentation based on the entry level, bold font for group entries,
	 * and background color for top-level group entries.
	 */
    private static class NavEntryRenderer extends DefaultListCellRenderer {
        private static final Color TOP_GROUP_BG = new Color(230, 230, 230);

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

            if (!isSelected && entry.group /*&& entry.level == 0*/) {
                setBackground(TOP_GROUP_BG);
            }

            return this;
        }
    }
}
