/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Objects;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.gui.SimpleOptionInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaSortStringComparator;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

/*
 * Input field for Strings, auto-completing using an AutoCompleteList.
 *
 * This version does not depend on AutoCompletePopupWindow or
 * AutoCompletePopupWindowCallback anymore.
 *
 * The popup is implemented using standard Swing components:
 * - JPopupMenu
 * - JList
 * - JScrollPane
 * - custom ListCellRenderer
 *
 * Key handling:
 * - DOWN / UP / PAGE DOWN / PAGE UP: move selection in popup
 * - ENTER: accept selected item
 * - ESC (two-stage): if the popup is visible, the first ESC only closes the
 *   popup and leaves the text field unchanged. If the popup is NOT visible,
 *   ESC clears the text field. So clearing the field normally requires
 *   pressing ESC twice if the popup was open.
 * - typing: updates the popup/filter from the text field
 * - TAB: accept selected item and move focus (Shift+TAB = backward)
 * 
 * The popup has a fixed height of POPUP_FIXED_ROWS rows, independent of the number of matches.
 * The popup is visible once the search term is not empty.
 *
 * The popup is "remote-controlled" from the text field; focus remains in the text field.
 */
public class ItemTypeStringAutoComplete extends ItemTypeString {

    protected boolean showButton; // is the status button (green/orange/red) visible?
    protected boolean showButtonFocusable; // is the status button focusable with keyboard?
    protected boolean useAutocompleteList;
    protected JButton button;
    protected Color originalButtonColor;
    protected AutoCompleteList autoCompleteList;
    protected Object rememberedId;
    protected boolean withPopup = true;
    protected boolean valueIsKnown = false;
    protected boolean isCheckSpelling = false;
    protected boolean isCheckPermutations = false;
    protected boolean isVisibleSticky = true;
    protected String ignoreEverythingAfter = null;
    protected String alternateFieldNameForPlainText = null;
    protected boolean alwaysReturnPlainText = false;
    protected ItemTypeDate validAtDateItem;
    protected ItemTypeTime validAtTimeItem;
    protected boolean onChoosenDeleteFromList = false;
    protected ItemTypeStringAutoComplete otherField;

    private JPopupMenu popup;
    private JList<String> popupList;
    private JScrollPane popupScrollPane;

    /**
     * Number of rows the popup is sized for. Fixed so that the popup size does not depend
     * on the number of matches and therefore does not change while typing.
     */
    private static final int POPUP_FIXED_ROWS = 6;


    /**
     * Some platforms/input methods occasionally deliver a KeyEvent with this
     * (non-standard, negative) key code. It does not correspond to any real key
     * (java.awt.event.KeyEvent only defines non-negative VK_* codes), so such
     * events are ignored here to avoid triggering autoComplete()/handleFilteredList()
     * with meaningless input.
     */
    private static final int KEYCODE_IGNORE_SPURIOUS_EVENT = -23;

    public ItemTypeStringAutoComplete(String name, String value, int type,
            String category, String description, boolean showButton) {
        super(name, value, type, category, description);
        this.showButton = showButton;
        this.useAutocompleteList = Daten.efaConfig == null || Daten.efaConfig.getValuePopupComplete();
    }

    public ItemTypeStringAutoComplete(String name, String value, int type,
            String category, String description, boolean showButton,
            AutoCompleteList autoCompleteList) {
        super(name, value, type, category, description);
        this.showButton = showButton;
        this.useAutocompleteList = Daten.efaConfig == null || Daten.efaConfig.getValuePopupComplete();
        setAutoCompleteData(autoCompleteList);
    }

    public IItemType copyOf() {
        ItemTypeStringAutoComplete copy = new ItemTypeStringAutoComplete(name, value, type, category, description, showButton, autoCompleteList);
        copy.setFieldSize(fieldWidth, fieldHeight);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setIcon((label == null ? null : label.getIcon()));
        copy.setIsItemOnSameRowAsPreviousItem(itemOnSameRowAsPreviousItem);
        copy.setItemOnNewRow(itemOnNewRow);
        copy.setFieldGrid(fieldGridWidth,fieldGridHeight,fieldGridAnchor,fieldGridFill);
        copy.setIndent(this.isIndent());
        //specific to this class
        copy.setOtherField(this.otherField);
        copy.setValidAt(this.validAtDateItem, this.validAtTimeItem);
        copy.setChecks(this.isCheckSpelling, this.isCheckPermutations);
        copy.setIgnoreEverythingAfter(this.ignoreEverythingAfter);
        copy.setAlwaysReturnPlainText(this.alwaysReturnPlainText);
        copy.setAlternateFieldNameForPlainText(this.alternateFieldNameForPlainText);
        copy.setAutoCompleteData(this.autoCompleteList, this.onChoosenDeleteFromList);
        copy.setVisibleSticky(this.isVisibleSticky);
        copy.setShowButtonFocusable(this.showButtonFocusable);
        copy.setRememberedId(this.rememberedId);
        copy.withPopup = this.withPopup;
        return copy;
    }

    public void setValidAt(ItemTypeDate validAtDate, ItemTypeTime validAtTime) {
        this.validAtDateItem = validAtDate;
        this.validAtTimeItem = validAtTime;
    }

    public void iniDisplay() {
        if (showButton) {
            button = new JButton();
            originalButtonColor = button.getBackground();
            button.setFocusable(showButtonFocusable);
            Dialog.setPreferredSize(button, fieldHeight - 4, fieldHeight - 8);
            button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    buttonPressed(e);
                }
            });
            button.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    field_focusLost(e);
                }
                public void focusGained(FocusEvent e) {
                    field_focusGained(e);
                }
            });
        }

        super.iniDisplay();

        ensurePopupCreated();

        final JTextField tf = (JTextField) this.field;

        // TAB aus Focus-Traversal-Keys entfernen, damit KeyListener es bekommt
        tf.setFocusTraversalKeysEnabled(false);

        iniDisplay_KeyListener(tf);

        tf.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                field_focusLost(e);
            }
            public void focusGained(FocusEvent e) {
                field_focusGained(e);
            }
        });
    }
    
    /*
	 * KeyListener for the text field to handle auto-completion and popup navigation.
	 * ENTER, TAB: Use the selected item from the popup if visible, otherwise transfer focus.
	 * ESC (two-stage): 1st ESC while popup is visible only closes the popup (handled in
	 * handlePopupNavigationKey(), consumed there, the block below is NOT reached).
	 * 2nd ESC (popup already closed) clears the text field (handled in the block below).
	 * Other keys: Call autoComplete() to update the text field and popup.
	 */
    private void iniDisplay_KeyListener(JTextField tf) {
        tf.addKeyListener(new KeyAdapter() {

        	/*
        	 * keyPressed works best for keys which are usually hit once.
        	 * all other keys (navigation) are handled in keyReleased()
        	 */
        	public void keyPressed(KeyEvent e) {
        	    if (handlePopupNavigationKey(e)) {
        	        e.consume();
        	        return;
        	    }
        	    
        	    // DOWN key, but invisible popup: open the popup with the current filter
        	    if (e.getKeyCode() == KeyEvent.VK_DOWN && !isPopupVisible() 
        	    		&& useAutocompleteList && withPopup) {
        	        autoComplete(e);
        	        e.consume();
        	        return;
        	    }

        	    
        	    // ENTER: accept popup selection if visible, then move focus. 
        	    // Do not invoke the default button of the dialog if the popup is visible 
        	    // (otherwise the user cannot select an item from the popup with ENTER).
        	    if (e.getKeyCode() == KeyEvent.VK_ENTER && isPopupVisible()) {
        	        acceptPopupSelection();
        	        e.consume();
    	            ((JTextField) field).transferFocus();
        	        return;
        	    } 
        	    
        	 // TAB: accept popup selection if visible, then move focus (Shift+TAB = backward)
        	    if (e.getKeyCode() == KeyEvent.VK_TAB) {
        	        if (isPopupVisible()) {
        	            acceptPopupSelection();
        	        }
        	        e.consume();
        	        if ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
        	            ((JTextField) field).transferFocusBackward();
        	        } else {
        	            ((JTextField) field).transferFocus();
        	        }
        	        return;
        	    }
        	    
        	    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        	    	// This block is only reached if the popup was NOT visible
        	    	// (otherwise handlePopupNavigationKey() already closed the popup
        	    	// and returned true above, so this code is skipped).
        	    	// -> 2nd ESC (popup already closed): now clear the textfield, too.
        	        if (field instanceof JTextField) {
        	            JTextField tf = (JTextField) field;
        	            tf.setText("");
        	            tf.setCaretPosition(0);
        	        }

        	        // Close the popup if neccessary (defensive; popup should already be closed here)
        	        if (isPopupVisible()) {
        	            closePopup();
        	        }

        	        // Do not consume() the event. -> super ESC-handlers shall see the key
        	        return;
        	    }
        	    // ENTER shall not invoke the default button of the dialog if the popup is visible 
        	    //(otherwise the user cannot select an item from the popup with ENTER).
        	    if (e.getKeyCode() == KeyEvent.VK_ENTER && isPopupContainsMode()) {
        	        autoComplete(e);
        	        e.consume();
        	    }
        	}

            public void keyReleased(KeyEvent e) {
                if (e == null) {
                    return;
                }

                int code = e.getKeyCode();

                // Navigationstasten dürfen NICHT nochmal in autoComplete() landen.
                if (code == KeyEvent.VK_UP
                        || code == KeyEvent.VK_DOWN
                        || code == KeyEvent.VK_LEFT
                        || code == KeyEvent.VK_RIGHT
                        || code == KeyEvent.VK_PAGE_UP
                        || code == KeyEvent.VK_PAGE_DOWN
                        || code == KeyEvent.VK_HOME
                        || code == KeyEvent.VK_END
                        || code == KeyEvent.VK_ESCAPE
                        || code == KeyEvent.VK_ENTER
                        || code == KeyEvent.VK_TAB) {
                    return;
                }

                autoComplete(e);
            }
        });
    }


    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
    	// Display the label and text field. super.displayOnGui will respect isIndent() property
        int plusy = super.displayOnGui(dlg, panel, x, y);
        
        //respect the indent property of the item, if set
        x=(this.isIndent() ? x+1 : x);
        if (button != null) {
            panel.add(button, new GridBagConstraints(x + labelGridWidth + fieldGridWidth, y, 1, fieldGridHeight, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(padYbefore, 0, padYafter, 0), 0, 0));
        }
        return plusy;
    }

    public void setAutoCompleteData(AutoCompleteList autoCompleteList) {
        this.autoCompleteList = autoCompleteList;
    }

    public void setAutoCompleteData(AutoCompleteList autoCompleteList, boolean deleteFromList) {
        this.autoCompleteList = autoCompleteList;
        this.onChoosenDeleteFromList = deleteFromList;
    }

    public AutoCompleteList getAutoCompleteData() {
        return this.autoCompleteList;
    }

    public void setChecks(boolean checkSpelling, boolean checkPermutations) {
        this.isCheckSpelling = checkSpelling;
        this.isCheckPermutations = checkPermutations;
    }

    public void setIgnoreEverythingAfter(String s) {
        ignoreEverythingAfter = s;
    }

    public void setVisible(boolean visible) {
        if (visible == true && isVisibleSticky() == false) {
            return;
        }
        super.setVisible(visible);
        if (button != null) {
            button.setVisible(visible);
        }
    }

    public void setVisibleSticky(boolean visible) {
        // Set isVisibleSticky BEFORE calling setVisible(), so the guard clause
        // in the overridden setVisible() below already sees the new value.
        // We intentionally call this.setVisible() (not super.setVisible()) so that
        // our own override (which also toggles the button) is applied exactly once.
        isVisibleSticky = visible;
        setVisible(visible);
    }

    public boolean isVisibleSticky() {
        return isVisibleSticky;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    public void showValue() {
        super.showValue();
        autoComplete(null);
    }

    public void setId(Object id) {
        if (autoCompleteList != null && id != null) {
            value = autoCompleteList.getValueForId(id.toString());
        }
    }

    public Object getId(String qname) {
        return (autoCompleteList != null ? autoCompleteList.getId(qname) : null);
    }

    public void setRememberedId(Object id) {
        rememberedId = id;
    }

    public Object getRememberedId() {
        return rememberedId;
    }

    protected void field_focusLost(FocusEvent e) {
        if (e != null && e.isTemporary()) {
            return;
        }
        if (useAutocompleteList) {
            closePopup();
        }
        if (isCheckSpelling && Daten.efaConfig != null && Daten.efaConfig.getValueCorrectMisspelledNames()) {
            checkSpelling();
        }
        super.field_focusLost(e);
        if ((autoCompleteList!=null && value != null) && onChoosenDeleteFromList && valueIsKnown && !value.isEmpty()) {
            Vector<String> vis = autoCompleteList.getDataVisible();
            if (vis.remove(value)) {
                autoCompleteList.setDataVisible(vis);
            }
        }
    }

    protected void field_focusGained(FocusEvent e) {
        if ((autoCompleteList!=null && value != null) && onChoosenDeleteFromList && valueIsKnown && !value.isEmpty()) {
            Vector<String> vis = autoCompleteList.getDataVisible();
            if (!vis.contains(value) && vis.add(value)) {
                autoCompleteList.setDataVisible(vis);
            }
        }
        super.field_focusGained(e);
    }

    public void showOrRemoveAutoCompletePopupWindow() {
        if (!useAutocompleteList) {
            return;
        }
        JTextField f = (JTextField) field;
        if (f.isEnabled() && f.isEditable()) {
            if (!isPopupVisible()) {
                showPopupForField(f);
            } else {
                closePopup();
            }
        }
    }

    private void buttonPressed(ActionEvent e) {
        showOrRemoveAutoCompletePopupWindow();
        actionEvent(e);
    }

    private void autoComplete(KeyEvent e) {
        if (isPopupContainsMode()) {
            handleFilteredList(e);
            return;
        }

        if (field == null) {
            return;
        }
        JTextField field = (JTextField) this.field;

        AutoCompleteList list = getAutoCompleteList();
        if (list == null) {
            setButtonColor(null);
            closePopup();
            return;
        } else {
            // Note: list.update() is intentionally called again below (indirectly,
            // via showPopupForField()) for most keystrokes. We consciously accept
            // this double update() call per keystroke rather than introducing a
            // time-based throttle inside AutoCompleteList.update(), since such a
            // throttle could introduce a runtime-dependent/timing-dependent bug
            // (e.g. stale data shown right after an external change to the list)
            // that would be much harder to track down than this minor, constant
            // performance overhead.
            list.update();
        }

        if (e != null && e.getKeyCode() == KEYCODE_IGNORE_SPURIOUS_EVENT) {
            return;
        }

        if (field.getText().trim().length() == 0) {
            setButtonColor(null);
        }

        String complete = null;
        String prefix = null;
        String base = null;

        Mode mode = Mode.none;
        if (e == null || ((EfaUtil.isRealChar(e) && (e.getKeyCode() != KeyEvent.VK_ENTER) && (e.getKeyCode() != KeyEvent.VK_TAB))
                || (e.getKeyCode() == KeyEvent.VK_DOWN))) {
            mode = Mode.normal;
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            mode = Mode.up;
        } else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            mode = Mode.delete;
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            mode = Mode.enter;
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            mode = Mode.escape;
        }

        if (e == null || mode == Mode.enter || mode == Mode.escape) {
            field.setText(field.getText().trim());
        }

        boolean matching = false;

        if (mode == Mode.normal
                || ((mode == Mode.enter || mode == Mode.escape || mode == Mode.none) && field.getText().length() > 0)) {

            String spc = field.getText();
            if (spc.startsWith(" ")) {
                int i = 0;
                do {
                    i++;
                } while (i < spc.length() && spc.charAt(i) == ' ');
                if (i >= spc.length()) {
                    field.setText("");
                } else {
                    field.setText(spc.substring(i));
                }
            }

            if (field.getSelectedText() != null) {
            	// use substring first, then toLowerCase as toLowerCase() may change the length of the string (e.g. Turkish I)
            	prefix = field.getText().substring(0, field.getSelectionStart()).toLowerCase();
            } else {
                prefix = field.getText().toLowerCase();
            }

            if (e != null && e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (isPopupVisible() && useAutocompleteList) {
                    complete = list.getNext(); //popup visible, get the next item in the popup list
                } else {
                    complete = list.getNext(prefix); //popup not visible, get the next item in the list based on the prefix
                }
                if (complete == null) {
                    complete = list.getFirst(prefix);
                }
            } else {
                if (e != null) {
                    complete = list.getFirst(prefix);
                } else {
                    complete = list.getExact(field.getText().toLowerCase());
                }
                if (list.getAlias(prefix) != null) {
                    complete = list.getAlias(prefix);
                }
            }

            if (e == null && complete != null) {
                complete = list.getExact(complete);
            }
            if (complete != null) {
                if (e != null && mode != Mode.none) {
                    field.setText(complete);
                    field.select(prefix.length(), complete.length());
                }
                matching = true;
            }

            if (withPopup && useAutocompleteList && e != null && mode != Mode.none) {
                showPopupForField(field);
                updatePopupSelection(complete != null ? complete : field.getText(), prefix);
            }
        }

        if (mode == Mode.up) {
            if (field.getSelectedText() != null) {
                prefix = field.getText().toLowerCase().substring(0, field.getSelectionStart());
            } else {
                prefix = field.getText().toLowerCase();
            }

            if (isPopupVisible() && useAutocompleteList) {
                complete = list.getPrev();
            } else {
                complete = list.getPrev(prefix);
            }

            if (complete == null) {
                complete = list.getLast(prefix);
            }
            if (complete != null) {
                field.setText(complete);
                field.select(prefix.length(), complete.length());
                matching = true;
            }
            if (withPopup && useAutocompleteList) {
                showPopupForField(field);
                updatePopupSelection(complete != null ? complete : field.getText(), prefix);
            }
        }

        if (mode == Mode.delete) {
            if ((complete = list.getFirst(field.getText().toLowerCase().trim())) != null
                    && (complete.equals(field.getText()))) {
                matching = true;
            }
            if (withPopup && useAutocompleteList) {
                showPopupForField(field);
                updatePopupSelection(complete != null ? complete : field.getText(), prefix);
            }
        }

        String ignoredString = null;
        int ignorePos = -1;
        for (int i = 0; ignoreEverythingAfter != null && i < ignoreEverythingAfter.length(); i++) {
            ignorePos = (prefix != null ? prefix.indexOf(ignoreEverythingAfter.charAt(i))
                    : field.getText().indexOf(ignoreEverythingAfter.charAt(i)));
            if (ignorePos >= 0) {
                break;
            }
        }
        if (ignorePos >= 0) {
            String s = (prefix != null ? prefix : field.getText());
            base = s.substring(0, ignorePos).trim();
            ignoredString = s.substring(ignorePos + 1).trim();
            if (ignoredString.length() == 0) {
                ignoredString = null;
            }
        }
        if (base != null && !matching) {
            String firstInList = list.getFirst(base.trim());
            if (firstInList != null && field.getText().startsWith(firstInList)) {
                matching = true;
            }
        }

        if (prefix != null && !matching && prefix.endsWith(" ")) {
            String firstInList = list.getFirst(prefix.trim());
            if (firstInList != null && field.getText().startsWith(firstInList)) {
                matching = true;
            }
        }

        boolean valid = false;
        if (matching && validAtDateItem != null) {
            long t = LogbookRecord.getValidAtTimestamp(validAtDateItem.getDate(),
                    (validAtTimeItem != null ? validAtTimeItem.getTime() : null));
            if (ignoredString == null && complete != null) {
                valid = autoCompleteList.isValidAt(complete, t);
            }
            if (ignoredString != null && base != null) {
                valid = autoCompleteList.isValidAt(base, t);
            }
            if (!valid) {
                matching = false;
            }
        } else {
            valid = true;
        }

        if (matching) {
            setButtonColor((ignoredString == null ? Color.green : Color.yellow));
        } else {
            setButtonColor((valid ? Color.red : Color.orange));
        }

        if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_AUTOCOMPLETE,
                    "field=" + field.getText()
                    + ", complete=" + complete
                    + ", matching=" + matching
                    + ", valid=" + valid
                    + ", validAtDateItem=" + validAtDateItem
                    + ", ignoredString=" + ignoredString);
        }

        if (mode == Mode.enter) {
            field.select(-1, -1);
            field.setCaretPosition(field.getText().length());
            closePopup();
        }

        if (mode == Mode.escape) {
            closePopup();
        }

        if (field.getText().length() == 0) {
            setButtonColor(null);
        }
    }

    private void checkSpelling() {
        String name = getValueFromField().trim();
        if (name.length() == 0) {
            return;
        }

        int ignorePos = -1;
        for (int i = 0; ignoreEverythingAfter != null && i < ignoreEverythingAfter.length(); i++) {
            ignorePos = name.indexOf(ignoreEverythingAfter.charAt(i));
            if (ignorePos >= 0) {
                break;
            }
        }
        if (ignorePos >= 0) {
            name = name.substring(0, ignorePos).trim();
        }

        AutoCompleteList list = getAutoCompleteList();
        if (list == null) {
            return;
        }

        Vector<String> neighbours = null;
        if (list.getExact(name.toLowerCase()) == null) {
            int radius = (name.length() < 6 ? name.length() / 2 : 3);
            neighbours = list.getNeighbours(name, radius, (isCheckPermutations ? 6 : 0));
        }

        if (isPopupContainsMode()) {
            if (list.getExact(name) == null) {
                if (neighbours == null) {
                    neighbours = new Vector<String>();
                }
                list.setFilterText(name);
                addButAvoidDuplicates(neighbours, list.getDataVisibleFiltered());
                neighbours.remove(name);
                Collections.sort(neighbours, new EfaSortStringComparator());
                list.setFilterText(null);
            }
        }

        if (neighbours != null && neighbours.size() > 0) {
            ItemTypeList item = new ItemTypeList("NAME", IItemType.TYPE_PUBLIC, "",
                    LogString.itemIsUnknown(name, International.getString("Name")) + "\n" +
                    International.getString("Meintest Du ...?"));
            for (int i = 0; i < neighbours.size(); i++) {
                item.addItem(neighbours.get(i), neighbours.get(i), null, null, neighbours.get(i), false, '\0');
            }
            item.setFieldSize(350, 200);
            item.setFieldGrid(3, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
            item.setGridWeightX(1.0);
            item.setGridWeightY(1.0);

            if (field == null || !field.isValid()) {
                return;
            }
            if (SimpleOptionInputDialog.showOptionInputDialog(dlg,
                    International.getString("Tippfehler?"), item,
                    new String[] { International.getString("Ersetzen"),
                                   International.getString("Abbruch") },
                    new int[] { SimpleOptionInputDialog.OPTION_OK,
                                SimpleOptionInputDialog.OPTION_CANCEL },
                                null)) {
                String suggestedName = item.getSelectedText();
                if (suggestedName != null && suggestedName.length() > 0) {
                    this.parseAndShowValue(suggestedName);
                }
            }
        }
    }

    /**
	 * Adds all elements from the source vector to the target vector, but avoids adding duplicates.
	 * If an element from the source is already present in the target, it will not be added again.
	 *
	 * @param target The vector to which elements will be added.
	 * @param source The vector from which elements will be taken.
	 */
    private void addButAvoidDuplicates(Vector<String> target, Vector<String> source) {
        String value;
        for (int i = 0; i < source.size(); i++) {
            value = source.get(i);
            if (!target.contains(value)) {
                target.add(value);
            }
        }
    }

    /**
	 * Handles the filtered list for auto-completion based on the key event.
	 * This method updates the auto-complete list and manages the popup display.
	 * 
	 * It is used only if the auto-complete list is in "contains" mode, 
	 * where the filter matches any part of the string.
	 *
	 * @param e The KeyEvent that triggered this method, or null if called programmatically.
	 */
    private void handleFilteredList(KeyEvent e) {
        if (field == null) {
            return;
        }        
        // Check spurious events before touching the list, so no filter state is left behind
        if (e != null && e.getKeyCode() == KEYCODE_IGNORE_SPURIOUS_EVENT) {
            return;
        }
        JTextField textField = (JTextField) this.field;
        AutoCompleteList list = getAutoCompleteList();

        if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, this.getName());
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "KeyEvent :" + (e == null ? "null" : e.toString()));
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "AutoCompleteList.size() = " + (list == null ? "null" : list.getSizes()));
        }

        if (list == null) {
            setButtonColor(null);
            closePopup();
            return;
        } else {
            // Note: list.update() is intentionally called again below (indirectly,
            // via showPopupForField()) for most keystrokes. See autoComplete() for
            // why this (minor) double update() call per keystroke is consciously
            // accepted instead of adding a time-based throttle in AutoCompleteList.
            list.update();
            list.setFilterText(textField.getText().trim());
        }

        if (textField.getText().trim().length() == 0) {
            setButtonColor(null);
        }

        Mode mode = Mode.none;
        if (e == null || ((EfaUtil.isRealChar(e) && (e.getKeyCode() != KeyEvent.VK_ENTER) && (e.getKeyCode() != KeyEvent.VK_TAB))
                || e.getKeyCode() == KeyEvent.VK_DOWN)
                || (e.getKeyCode() == KeyEvent.VK_F && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0))) {
            mode = Mode.normal;
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            mode = Mode.up;
        } else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            mode = Mode.delete;
        } else if ((e.getKeyCode() == KeyEvent.VK_ENTER) || e.getKeyCode() == KeyEvent.VK_TAB) {
            mode = Mode.enter;
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            mode = Mode.escape;
        }

        if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "Mode " + mode);
        }

        if (e == null || mode == Mode.enter || mode == Mode.escape) {
            textField.setText(textField.getText().trim());
        }

        boolean matching = false;

        String searchFor = textField.getText().trim().toLowerCase();
        String complete = "";

        if (mode == Mode.normal || ((mode == Mode.enter || mode == Mode.escape || mode == Mode.none))) {

            if (e != null && ((e.getKeyCode() == KeyEvent.VK_DOWN)
                    || (e.getKeyCode() == KeyEvent.VK_F && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)))) {
                if (isPopupVisible()) {
                    complete = list.getNext(searchFor);       // popup visible: get next item in the popup list
                } else {
                    complete = list.getFirst(searchFor);      // Popup invisible: get first item in the list based on the prefix
                }
                
                if (complete == null) {
                    complete = list.getFirst(searchFor);
                }

            } else if (e != null && e.getKeyCode() == KeyEvent.VK_UP) {
                complete = list.getPrev(searchFor);
                if (complete == null) {
                    complete = list.getLast(searchFor);
                }
            } else {
                if ((e != null) && e.getKeyCode() != KeyEvent.VK_DOWN) {
                    if (isPopupContainsModeSelectPrefixItem()) {
                        complete = list.getFirstByPrefix(textField.getText());
                        if (complete == null) {
                            complete = list.getFirst(textField.getText());
                        }
                    } else {
                        complete = list.getFirst(textField.getText());
                    }
                }
            }

            if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
                Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "User entered text=" + searchFor + " matching item=" + complete);
            }

            if (e != null && (mode != Mode.normal && (
                    (e.getKeyCode() == KeyEvent.VK_ENTER) ||
                    ((e.getKeyCode() == KeyEvent.VK_TAB) && !searchFor.isEmpty())
                    ) &&
            		isPopupVisible())) {
                complete = popupList.getSelectedValue();
                if (complete != null && !complete.isEmpty()) {
                    textField.setText(complete);
                }
                // Note: "matching" is intentionally NOT set here, since it is
                // recalculated uniformly further below via list.getExact(textField.getText())
                // (any assignment here would be overwritten there anyway).
            }

            if (withPopup && useAutocompleteList && e != null && mode != Mode.none && e.getKeyCode() != KeyEvent.VK_TAB) {
                showPopupForField(textField);
                updatePopupSelection(complete != null ? complete : "", searchFor);
            }
        }

        if (mode == Mode.up) {
            // In filter mode AutoCompleteList ignores the prefix, so getPrev() without
            // argument is equivalent to getPrev(searchFor) here and matches autoComplete().
            complete = list.getPrev();
            if (complete == null) {
                complete = list.getLast(searchFor);
            }
            if (withPopup && useAutocompleteList) {
                showPopupForField(textField);
                updatePopupSelection(complete != null ? complete : "", searchFor);
            }
        }


        if (mode == Mode.delete) {
            if (withPopup && useAutocompleteList && e != null && mode != Mode.none) {
                if (isPopupContainsModeSelectPrefixItem()) {
                    complete = list.getFirstByPrefix(textField.getText());
                    if (complete == null) {
                        complete = list.getFirst(textField.getText());
                    }
                } else {
                    complete = list.getFirst(textField.getText());
                }
                showPopupForField(textField);
                updatePopupSelection(complete != null ? complete : textField.getText(), searchFor);
            }
        }

        matching = (list.getExact(textField.getText()) != null);

        boolean valid = false;
        if (matching && validAtDateItem != null) {
            long t = LogbookRecord.getValidAtTimestamp(validAtDateItem.getDate(),
                    (validAtTimeItem != null ? validAtTimeItem.getTime() : null));
            valid = autoCompleteList.isValidAt(textField.getText(), t);
            if (!valid) {
                matching = false;
            }
        } else {
            valid = true;
        }

        if (matching) {
            setButtonColor(Color.green);
        } else {
            setButtonColor((valid ? Color.red : Color.orange));
        }

        if (mode == Mode.enter) {
            textField.select(-1, -1);
            textField.setCaretPosition(textField.getText().length());
            closePopup();
        }

        if (mode == Mode.escape) {
            closePopup();
        }

        if (textField.getText().length() == 0) {
            setButtonColor(null);
        }
    }

    public boolean isCurrentTextMatching() {
        AutoCompleteList list = getAutoCompleteList();
        JTextField textField = (JTextField) this.field;
        if (list != null && textField != null) {
            return list.getExact(textField.getText()) != null;
        }
        return false;
    }

    public boolean isCurrentTextValid() {
        boolean valid = false;
        JTextField textField = (JTextField) this.field;

        if (textField != null && isCurrentTextMatching() && validAtDateItem != null) {
            long t = LogbookRecord.getValidAtTimestamp(validAtDateItem.getDate(),
                    (validAtTimeItem != null ? validAtTimeItem.getTime() : null));
            valid = autoCompleteList.isValidAt(textField.getText(), t);
        } else {
            valid = false;
        }

        return valid;
    }

    private AutoCompleteList getAutoCompleteList() {
        return autoCompleteList;
    }

    /**
     * Returns the value of Daten.efaConfig.getValuePopupContainsMode(),
     * or false if Daten.efaConfig is not (yet) initialized
     * (e.g. in unit tests or during application startup).
     */
    private boolean isPopupContainsMode() {
        return Daten.efaConfig != null && Daten.efaConfig.getValuePopupContainsMode();
    }

    /**
     * Returns the value of Daten.efaConfig.getValuePopupContainsModeSelectPrefixItem(),
     * or false if Daten.efaConfig is not (yet) initialized.
     */
    private boolean isPopupContainsModeSelectPrefixItem() {
        return Daten.efaConfig != null && Daten.efaConfig.getValuePopupContainsModeSelectPrefixItem();
    }

    private void setButtonColor(Color color) {
        valueIsKnown = (color == Color.green || color == Color.yellow);
        if (button != null) {
            if (color != null) {
                EfaUtil.handleButtonOpaqueForLookAndFeels(button);
                button.setBackground(color);
            } else {
                button.setBackground(originalButtonColor);
            }
        }
    }

    public boolean isKnown() {
        return valueIsKnown;
    }

    public void setAlternateFieldNameForPlainText(String fieldName) {
        alternateFieldNameForPlainText = fieldName;
    }

    public String getAlternateFieldNameForPlainText() {
        return alternateFieldNameForPlainText;
    }

    public void setAlwaysReturnPlainText(boolean alwaysReturnPlainText) {
        this.alwaysReturnPlainText = alwaysReturnPlainText;
    }

    public boolean getAlwaysReturnPlainText() {
        return alwaysReturnPlainText;
    }

    public void requestButtonFocus() {
        if (button != null) {
            button.requestFocus();
        }
    }

    public JButton getButton() {
        return button;
    }

    public boolean isValidInput() {
        if (!alwaysReturnPlainText && alternateFieldNameForPlainText == null && value != null && value.length() > 0) {
            if (autoCompleteList!=null && autoCompleteList.getId(value) == null) {
                lastInvalidErrorText = International.getString("Unbekannter Name nicht erlaubt");
                return false;
            }
        }
        return super.isValidInput();
    }

    public void setShowButtonFocusable(boolean value) {
        this.showButtonFocusable = value;
        if (button != null) {
            button.setFocusable(value);
        }
    }

    public boolean getShowButton() {
        return this.showButton;
    }

    public ItemTypeStringAutoComplete getOtherField() {
        return this.otherField;
    }

    public void setOtherField(ItemTypeStringAutoComplete other) {
        this.otherField = other;
    }

    public void removeFromVisible(String value) {
        if ((autoCompleteList!=null && value != null) && onChoosenDeleteFromList && !value.isEmpty()) {
            Vector<String> vis = autoCompleteList.getDataVisible();
            if (vis.remove(value)) {
                autoCompleteList.setDataVisible(vis);
            }
        }
    }

    // ============================================================
    // Popup handling
    // ============================================================

    private enum Mode {
        none,
        normal,
        up,
        delete,
        enter,
        escape
    }

    private void ensurePopupCreated() {
        if (popup != null) {
            return;
        }

        popupList = new JList<String>();
        popupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        popupList.setCellRenderer(new PaintHighlightRenderer());
        popupList.setVisibleRowCount(6);
        popupList.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        popupScrollPane = new JScrollPane(popupList);
        popupScrollPane.setBorder(//UIManager.getBorder("PopupMenu.border"));
        		BorderFactory.createEmptyBorder());
        popupScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        popup = new JPopupMenu();
        popup.setFocusable(false);
        popup.add(popupScrollPane);
        
        popupList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // Index unter dem Mauszeiger selektieren und übernehmen
                    int idx = popupList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        popupList.setSelectedIndex(idx);
                        acceptPopupSelection();
                    }
                }
            }
        });

    }

    private void showPopupForField(JTextField textField) {
        if (!useAutocompleteList || autoCompleteList == null || textField == null 
        		|| !textField.isEnabled() || !textField.isEditable()) {
            closePopup();
            return;
        }

        ensurePopupCreated();

        // Note: this is usually the SECOND call to autoCompleteList.update() for the
        // same keystroke (the first one happened in autoComplete()/handleFilteredList()
        // right before calling this method). This redundancy is intentional/accepted,
        // see the comment in autoComplete() for the reasoning.
        autoCompleteList.update();

        String filterText = textField.getText() == null ? "" : textField.getText().trim();
        if (isPopupContainsMode()) {
            autoCompleteList.setFilterText(filterText);
        } else {
            autoCompleteList.setFilterText(null);
        }

        Vector<String> data = autoCompleteList.getDataVisibleFiltered();
        if (data == null) {
            data = new Vector<String>();
        }

        // Only close the popup when there is nothing to filter on (empty text).
        // For a non-empty search term without matches we keep the popup open with an
        // empty list. Closing and reopening it on every keystroke causes the flicker,
        // because the native popup window is recreated each time.
        if (data.isEmpty() && textField.getText().trim().isEmpty()) {
            closePopup();
            return;
        }
        // performance, less flicker: only update the list model if the content actually changed
        
        // Replace the list model only if the visible content actually changed. Replacing it
        // on every keystroke clears the selection and re-lays out all cells, which makes the
        // popup look restless even when the result set stays the same.
        if (!isSameListContent(popupList.getModel(), data)) {
            popupList.setListData(data);
        }

        int width = Math.max(textField.getWidth(), 240);
        int rowHeight = Math.max(
                18,
                popupList.getFixedCellHeight() > 0
                        ? popupList.getFixedCellHeight()
                        : popupList.getFontMetrics(popupList.getFont()).getHeight() + 6
        );
        
        // The popup always has the height of POPUP_FIXED_ROWS rows, independent of the number
        // of matches. A constant size means the native popup window never has to be resized
        // while the user types. this avoids a flickering popup list while typing.
        // Also, it keeps the popup visible for search terms without any result.
        int height = Math.max(150, POPUP_FIXED_ROWS * rowHeight + 8);

        // Size is only applied when the popup is opened; it does not change while typing.
        if (!isPopupVisible()) {
            popupScrollPane.setPreferredSize(new Dimension(width, height));
            try {
                popup.show(textField, 0, textField.getHeight());
            } catch (Exception ex) {
                Logger.logdebug(ex);
                return;
            }
        }

        updatePopupSelection(textField.getText(), filterText);
    }

    /**
     * Compares the current model of the popup list with the new data element by element.
     * Used to avoid replacing the model when nothing has changed.
     * 
     * @param model The current ListModel of the popup list.
     * @param data The new data to compare against the model.
     * 
     * @return true if the model and data have the same content, false otherwise.
     */
    private boolean isSameListContent(ListModel<String> model, Vector<String> data) {
        if (model.getSize() != data.size()) {
            return false;
        }
        for (int i = 0; i < data.size(); i++) {
            // Objects.equals() is null-safe, so null entries cannot cause an exception
            if (!Objects.equals(data.get(i), model.getElementAt(i))) {
                return false;
            }
        }
        return true;
    }

    /* 
	 * Updates the selection in the popup list based on the given value to select and filter text.
	 * If the value to select is found in the data, it will be selected. 
	 * 
	 * Otherwise, it will try to find a match based on the filter text, 
	 * either by contains or starts with, depending on the configuration.
	 * 
	 * If no match is found, the first item in the list will be selected.
	 */
    private void updatePopupSelection(String valueToSelect, String filterText) {
        if (popupList == null) {
            return;
        }

        Vector<String> data = autoCompleteList != null ? autoCompleteList.getDataVisibleFiltered() : null;
        if (data == null || data.isEmpty()) {
            popupList.clearSelection();
            return;
        }

        int idx = -1;

        if (valueToSelect != null && valueToSelect.length() > 0) {
            idx = data.indexOf(valueToSelect);
        }

        boolean isContainsMode = isPopupContainsMode();
        if (idx < 0 && filterText != null && filterText.length() > 0) {
            String lowerFilter = filterText.toLowerCase();
            for (int i = 0; i < data.size(); i++) {
                String s = data.get(i);
                if (isContainsMode) {
                	//search for the first item which contains the element
	                if (s != null && s.toLowerCase().contains(lowerFilter)) {
	                    idx = i;
	                    break;
	                }
                } else {
                	//search for the first item which starts with element
                    if (s != null && s.toLowerCase().startsWith(lowerFilter)) {
	                    idx = i;
	                    break;
	                }                	
                }
            }
        }

        if (idx < 0) {
            // No match: select the first entry, so the user can accept the top suggestion
            // with ENTER without first having to navigate, and the selection stays predictable.
            idx = 0;
        }

        popupList.setSelectedIndex(idx);
        if (filterText != null && filterText.trim().length() > 0) {
            // With a search term, keep the matching entry in the second row, so the selection bar
            // stays at a fixed position while the user types further characters.
        	
        	// Putting the matching entry in the second row helps the user to understand 
        	// that the list is filtered and that there are more matches above it.
            scrollSelectionToSecondRow(idx);
        } else {
            popupList.ensureIndexIsVisible(idx);
        }
        popupList.repaint();
    }

    /*
     * Scrolls the popup so that the given (matching) item is shown in the second row. The first row
     * shows the entry above it, so the selection bar keeps the same screen position while the user
     * types more characters. Near the start or end of the list the scroll position is clamped.
     */
    private void scrollSelectionToSecondRow(int idx) {
        if (popupList == null || popupScrollPane == null) {
            return;
        }
        Rectangle cell = popupList.getCellBounds(idx, idx);
        JViewport viewport = popupScrollPane.getViewport();
        // Without a laid-out viewport the target position cannot be computed reliably
        if (cell == null || viewport.getExtentSize().height <= 0) {
            popupList.ensureIndexIsVisible(idx);
            return;
        }
        /* Note: popupList.getHeight() is intentionally NOT used here. 
           After setListData() (e.g. triggered by Backspace, which usually widens the result set) 
           Swing only schedules a revalidate(); the component's real bounds/height are only updated
           during the next, asynchronous layout pass. 
           
           At this point getHeight() would still return the OLD (possibly much smaller) height, 
           clamping targetY far too low and scrolling the correct selection out of view.
        
           listHeight is derived directly from the current model size and the (uniform) cell
           height instead of calling getPreferredSize(), which keeps this independent of
           Swing's layout timing. 
           
           This ignores the list's 2px top/bottom EmptyBorder, so the clamp can be off by a few pixels 
           in edge cases - accepted as a minor inaccuracy.
         */
        int listHeight = popupList.getModel().getSize() * cell.height;
        // All rows have the same height, so one row above the selection is cell.y - cell.height
        int maxY = Math.max(0, listHeight - viewport.getExtentSize().height);
        int targetY = Math.max(0, Math.min(cell.y - cell.height, maxY));
        viewport.setViewPosition(new Point(viewport.getViewPosition().x, targetY));

        // Ensure that the selected item is fully visible after scrolling, in case the clamped position
        // This fixes the problem that when deleting chars from the search term, 
        // the selection can be scrolled out of view as the list grows and the clamped position is used.
        Rectangle visibleAfterScroll = viewport.getViewRect();
        if (!visibleAfterScroll.contains(cell)) {
            popupList.ensureIndexIsVisible(idx);
        }
    }

    /*
     * Handles the keys that operate on a visible popup (ESC, UP, DOWN, PAGE_UP, PAGE_DOWN).
     * Returns true if the key was consumed here, so keyPressed() must not process it further.
     * ENTER and TAB are not handled here, keyPressed() accepts the selection for them.
     */
    private boolean handlePopupNavigationKey(KeyEvent e) {
        if (!isPopupVisible()) {
            return false;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                // First ESC only closes the popup, so the typed text is kept.
                // A second ESC (handled in keyPressed) clears the text field.
                closePopup();
                return true;
            case KeyEvent.VK_UP:
                movePopupSelection(-1);
                return true;
            case KeyEvent.VK_DOWN:
                movePopupSelection(1);
                return true;
            case KeyEvent.VK_PAGE_UP:
                movePopupSelection(-POPUP_FIXED_ROWS);
                return true;
            case KeyEvent.VK_PAGE_DOWN:
                movePopupSelection(POPUP_FIXED_ROWS);
                return true;
            default:
                return false;
        }
    }

    /*
     * Moves the selection in the popup list by the given number of entries and keeps it visible.
     * The index is clamped to the list bounds, so the cursor does not jump from the end back to the start.
     */
    private void movePopupSelection(int delta) {
        if (popupList == null) {
            return;
        }
        int size = popupList.getModel().getSize();
        if (size == 0) {
            return;
        }
        int current = popupList.getSelectedIndex();
        int idx = (current < 0 ? 0 : current + delta);
        idx = Math.max(0, Math.min(size - 1, idx));
        popupList.setSelectedIndex(idx);
        ensureSelectionVisibleNaturally(idx);
    }

    /*
     * Only scroll the list if the selected item is not fully visible, mimicking the standard JList behavior.
     */
    private void ensureSelectionVisibleNaturally(int idx) {
        if (popupList == null) {
            return;
        }

        Rectangle cellBounds = popupList.getCellBounds(idx, idx);
        if (cellBounds == null) {
            return;
        }

        Rectangle visible = popupList.getVisibleRect();
        if (visible == null) {
            return;
        }

        if (cellBounds.y < visible.y) {
            popupList.scrollRectToVisible(cellBounds);
        } else if (cellBounds.y + cellBounds.height > visible.y + visible.height) {
            popupList.scrollRectToVisible(cellBounds);
        }
    }

    /* 
	 * Accepts the currently selected value in the popup list and applies it to the text field.
	 * If no value is selected, it does nothing. After applying the value, it closes the popup.
	 */
    private void acceptPopupSelection() {
        if (!isPopupVisible() || popupList == null) {
            return;
        }

        String selected = popupList.getSelectedValue();
        if (selected != null) {
            applyPopupValue(selected);
        }
        closePopup();
    }
    
    /* 
     * Puts the currently selected value from the popup list into the text field and updates the internal state.
     */
    private void applyPopupValue(String selected) {
        if (field == null || selected == null) {
            return;
        }
        JTextField tf = (JTextField) field;

        tf.setText(selected);
        tf.select(-1, -1);
        tf.setCaretPosition(selected.length());

        // Update internal state immediately.
        autoCompleteList.setFilterText(null);
        autoComplete(null);
    }

    /*
     * Closes the popup if it is currently visible and resets the filter text in the autoCompleteList.
     */
    private void closePopup() {
        if (popup != null && isPopupVisible()) {
            popup.setVisible(false);
        }

        if (autoCompleteList != null) {
            autoCompleteList.setFilterText(null);
        }
    }

    private boolean isPopupVisible() {
    	return (popup!=null && popup.isVisible());
    }
    
    /*
     * Callback method for the auto-complete functionality. 
     * apcw=auto-complete popup window
     * This method is called when the text in the associated JTextField changes, and it triggers the auto-complete logic.
     * It is also called from outside (efaBaseFrame, efaBaseFrameMultisession) 
     * to ensure that the value of the text field is checked against the auto-complete list and updated accordingly.
     */
    public void acpwCallback(JTextField field) {
        autoComplete(null);
    }

    /* 
     * A custom ListCellRenderer that highlights the matching part of the text in the popup list based on the current search term.
     * The highlighting is only performed if the "contains mode" is active, as determined by the configuration.
     * If the old search mode is active (search by prefix), the matching part is not highlighted, and the text is displayed normally.
     */
    private class PaintHighlightRenderer extends JComponent implements ListCellRenderer<String> {

        private static final long TEN_SECONDS_IN_MILLIS = 1000*10;
        private static final int SELECTION_ARC = 12;
        private static final int HIGHLIGHT_ARC = 6;
		private String fullText;
        private String search;
        private boolean selectedFlag;
        
        // Cached colors (sollten sich nicht staendig aendern)
        private Color selectionBackground=UIManager.getColor("List.selectionBackground");
        private Color selectionForeground=UIManager.getColor("List.selectionForeground");
        private Color listBackground;
        private Color listForeground;
        private Color highlightColor = new Color(245, 245, 120);
        private Color matchForeground = Color.BLACK;
        private boolean isContainsMode = true;
        private long lastContainsModeCheck=0;

        public PaintHighlightRenderer() {
            setFont(UIManager.getFont("Label.font"));
            setOpaque(true);
            updateCachedColors();
        }
        
        private void updateCachedColors() { 
            listBackground = UIManager.getColor("List.background");
            listForeground = UIManager.getColor("List.foreground");
            // for some LookAndFeels, the selection colors might be null, so we provide a fallback to the default JList colors.
            selectionBackground = (selectionBackground != null ? selectionBackground : new JList().getSelectionBackground());
            selectionForeground = (selectionForeground != null ? selectionForeground : new JList().getSelectionForeground());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            this.fullText = value;
            this.search = (field instanceof JTextField) ? ((JTextField) field).getText().trim().toLowerCase() : "";
            this.selectedFlag = isSelected;
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            String t = (fullText == null ? "" : fullText);
            return new Dimension(fm.stringWidth(t) + 8, fm.getHeight() + 8);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (fullText == null) return;
            Graphics2D g2 = (Graphics2D) g;
            FontMetrics fm = g2.getFontMetrics();
            
            EfaGuiUtils.setStandardRenderingHints(g2);
            int x = 8;
            int y = fm.getAscent() + 2;
            int yText = (this.getHeight()+ fm.getAscent() - fm.getDescent()) / 2;

            // Draw background
            if (selectedFlag) {
                g2.setColor(selectionBackground);
                int selectionBarPadding = 3;
             // Sichtbare Breite des Viewports, nicht die Zellenbreite
                int visibleWidth = getWidth();
                java.awt.Container parent = SwingUtilities.getAncestorOfClass(JViewport.class, popupList);
                if (parent instanceof JViewport) {
                    visibleWidth = Math.min(visibleWidth, ((JViewport) parent).getExtentSize().width);
                }
                g2.fillRoundRect(selectionBarPadding, 1, visibleWidth - selectionBarPadding * 2, getHeight() - 1, SELECTION_ARC, SELECTION_ARC);
            } else {
                g2.setColor(listBackground);
                g2.fillRect(x-2, y, getWidth(), getHeight());
            }
            
            g2.setColor(selectedFlag ? selectionForeground : listForeground);

            int pos = findIgnoreCase(fullText, search);

            if (pos >= 0 && !search.isEmpty()) {
                String before = fullText.substring(0, pos);
                String match = fullText.substring(pos, pos + search.length());
                String after = fullText.substring(pos + search.length());

                // Draw "before" text
                g2.setColor(selectedFlag ? selectionForeground  : listForeground);
                g2.drawString(before, x, yText);
                x += fm.stringWidth(before);

                int w = fm.stringWidth(match);
                if (isContainsMode()) {
	                // Draw highlight background for match only if containsmode is active.
	                g2.setColor(highlightColor);
	                g2.fillRoundRect(x, y - fm.getAscent() + 2, w, fm.getHeight(), HIGHLIGHT_ARC, HIGHLIGHT_ARC);
	                // Draw match text (in black for contrast)
	                g2.setColor(matchForeground);
                }
                g2.drawString(match, x, yText);
                x += w;

                // Draw "after" text
                g2.setColor(selectedFlag ? selectionForeground  : listForeground);
                g2.drawString(after, x, yText);
            } else {
                g2.drawString(fullText, x, yText);
            }
        }
        
        private boolean isContainsMode() {        	
        	if (System.currentTimeMillis() >= this.lastContainsModeCheck+ (TEN_SECONDS_IN_MILLIS)) {
        		this.isContainsMode= isPopupContainsMode();
        		lastContainsModeCheck=System.currentTimeMillis();
        	}
        	return this.isContainsMode;
        }

        /**
         * Searches for "search" inside "text", case-insensitively, WITHOUT creating a
         * toLowerCase() copy of "text". This avoids index mismatches that can occur for
         * certain Unicode characters whose lowercase form has a different length than the
         * original (e.g. Turkish "İ" -> "i̇", which is 2 chars instead of 1).
         * Returns the position in "text" (in the original, unmodified coordinate system),
         * or -1 if not found.
         */
        private int findIgnoreCase(String text, String search) {
            if (text == null || search == null || search.isEmpty()) {
                return -1;
            }
            int maxStart = text.length() - search.length();
            for (int i = 0; i <= maxStart; i++) {
                if (text.regionMatches(true, i, search, 0, search.length())) {
                    return i;
                }
            }
            return -1;
        }
    }
}