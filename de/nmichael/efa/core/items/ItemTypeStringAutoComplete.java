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
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.SimpleOptionInputDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.gui.util.AutoCompletePopupWindow;
import de.nmichael.efa.gui.util.AutoCompletePopupWindowCallback;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaSortStringComparator;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

/*
 * Input field for Strings, auto-completing using a AutocompleteList/AutoCompletePopupWindow 
 * 
 * Via EfaConfig user can set which style is applied in AutoComplete String items.
 * 
 * Old-Fashioned AutoComplete style
 * - user enters some text. this is regarded as a prefix of the desired entry.
 * - when an item is found which applies to this prefix (case-insensitive), the item is presented in the text field.
 *   The selected text of the text field selects that part which is NOT the text the user entered.
 *   So it is possible to distinguish between text entered by user and text autocompleted by the system.
 * - User interaction
 * 		- KEY_DOWN	select another item within the dropdownlist (next item). may or may not apply to the prefix entered by the user.
 * 		- KEY_UP	select another item within the dropdownlist (previous item). may or may not apply to the prefix entered by the user.
 *        user can scroll endlessly through the list with up and down keys, as KEY_DOWN selects the first element in the dropdown list,  
 *        if the last element of the list is currently selected. KEY_UP works likewise
 * 		- KEY_ENTER Selects the current entry and proceeds to the step where the system calculates wether the text
 *        entered by the user matches one item in the dropdownlist exactly.
 *      - KEY_ESC closes the dropdownlist
 * 
 * Filtered_List style
 * - user enters some text. this text is regarded as a filter which is applied to all entries in the dropdown list.
 * - dropdown list opens automatically when typing text, displaying only matching items.
 * - if an item in the list matches by prefix, it gets selected.
 *    - the user may choose other items of the list which are not matching by prefix using the arrow keys.
 * - user MUST use ENTER or TAB key to select the desired entry. 
 * - there is no auto completion by the system.
 * - User interaction
 * 		- KEY_DOWN	Select another item within the dropdownlist (next item). Item must apply to the filter entered by the user.
 *	   	- KEY_UP	Select another item within the dropdownlist (previous item). Item must apply to the filter entered by the user.
 *      	user can scroll endlessly through the filtered list with up and down keys, as KEY_DOWN selects the first element 
 *      	in the dropdown list, if the last element of the list is currently selected. KEY_UP works likewise
 * 		- KEY_ENTER Selects the current entry of the dropdown list and proceeds to the step where the system calculates wether the text
 *        entered by the user matches one item in the dropdown list exactly.
 *      - KEY_ESC closes the dropdownlist.
 *       
 */
public class ItemTypeStringAutoComplete extends ItemTypeString implements AutoCompletePopupWindowCallback {

    private enum Mode {
        none,
        normal,
        up,
        delete,
        enter,
        escape
    }

    protected boolean showButton; // is the status button (green/orange/red) visible?
    protected boolean showButtonFocusable; // is the status button (green/orange/red) focusable with keyboard?
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
    protected boolean onChoosenDeleteFromList = false; // @todo - added by Velten
    // two ItemTypeStringAutoComplete can be connected, for instance in an ItemTypeItemList.
    protected ItemTypeStringAutoComplete otherField; 

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

    public void setValidAt(ItemTypeDate validAtDate, ItemTypeTime validAtTime) {
        this.validAtDateItem = validAtDate;
        this.validAtTimeItem = validAtTime;
    }

    public void iniDisplay() {
        if (showButton) {
            button = new JButton();
            originalButtonColor = button.getBackground();
            button.setFocusable(showButtonFocusable);
            Dialog.setPreferredSize(button, fieldHeight-4, fieldHeight-8);
            button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) { 
                    buttonPressed(e);
                }
            });
            button.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    field_focusLost(e);
                }
                public void focusGained(FocusEvent e) {
                    field_focusGained(e);
                }
            });
        }
        super.iniDisplay();
        
        ((JTextField)field).addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent e) { autoComplete(e); }
        });

        ((JTextField)field).addKeyListener(new java.awt.event.KeyAdapter() {
        	//needed for dialogs where some button is the default button and reacts to ENTER.
        	//and if the autocomplete list is defined as filtered list.
        	public void keyPressed(KeyEvent e) { 
            	if ((e!=null) && e.getKeyCode()==KeyEvent.VK_ENTER && Daten.efaConfig.getValuePopupContainsMode()) {
            		autoComplete(e);
        		}
        	}
        });   
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        int plusy = super.displayOnGui(dlg, panel, x, y);
        if (button != null) {
            panel.add(button, new GridBagConstraints(x+labelGridWidth+fieldGridWidth, y, 1, fieldGridHeight, 0.0, 0.0,
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
        };
    }

    // used to hide input fields in EfaBoathouseFrame that remain invisible, even if the
    // setCrewRangeSelection(i) would want to make them visible again
    public void setVisibleSticky(boolean visible) {
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
        };
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


    // the following methods are for bypassing autoCompleteList and misusing
    // ItemTypeStringAutoComplete as an item to store an id for the text it is
    // displaying, without using any auto complete functionality
    public void setRememberedId(Object id) {
        rememberedId = id;
    }

    public Object getRememberedId() {
        return rememberedId;
    }



    protected void field_focusLost(FocusEvent e) {
        if (e != null && e.isTemporary()) {
            // avoid that the popup window disappears when showing on a JDialog: with JDialog, we receive temporary focusLost events all the time...
            return;
        }
        if (useAutocompleteList) {
        	if (Daten.efaConfig.getValuePopupContainsMode()) {
        		this.autoComplete(new KeyEvent(e.getComponent(),e.getID(), System.currentTimeMillis(), 0, KeyEvent.VK_TAB));
        	}
            AutoCompletePopupWindow.hideWindow();
        }
        if (isCheckSpelling && Daten.efaConfig != null && Daten.efaConfig.getValueCorrectMisspelledNames()) {
            checkSpelling();
        }
        super.field_focusLost(e);
        if(onChoosenDeleteFromList && valueIsKnown && !value.isEmpty()) {
            Vector<String> vis = autoCompleteList.getDataVisible();
            if(vis.remove(value)) {
                autoCompleteList.setDataVisible(vis);
            }
        }
    }

    protected void field_focusGained(FocusEvent e) {
        if(onChoosenDeleteFromList && valueIsKnown && !value.isEmpty()) {
             Vector<String> vis = autoCompleteList.getDataVisible();
            if(!vis.contains(value) && vis.add(value)) {
                autoCompleteList.setDataVisible(vis);
            }
        }
        super.field_focusGained(e);
     }

    public void showOrRemoveAutoCompletePopupWindow() {
        if (useAutocompleteList) {
            JTextField f = (JTextField)field;
            if (f.isEnabled() && f.isEditable()) {
                if (!AutoCompletePopupWindow.isShowingAt(f)) {
                    AutoCompletePopupWindow.hideWindow();
                    try {
                        Thread.sleep(50);
                    } catch(InterruptedException eignore) {}
                    AutoCompletePopupWindow.showAndSelect(f, autoCompleteList, f.getText(), null);
                } else {
                    autoComplete(null);
                    AutoCompletePopupWindow.hideWindow();
                }
            }
        }
    }

    private void buttonPressed(ActionEvent e) {
        showOrRemoveAutoCompletePopupWindow();
        actionEvent(e);
    }

    private void autoComplete(KeyEvent e) {

    	//Check if contains filtering mode is wanted.
    	//this is the most efficient place to switch to the new method
    	if (Daten.efaConfig.getValuePopupContainsMode()) {
    		handleFilteredList(e);
    		return;
    	}

    	//else, use classic prefix search mode
    	
    	if (field == null) {
            return;
        }
        JTextField field = (JTextField)this.field;
        //System.out.println("autoComplete("+e+") on "+getName()+" with text '"+field.getText()+"'");

        AutoCompleteList list = getAutoCompleteList();
        if (list == null) {
            setButtonColor(null);
            return;
        } else {
            list.update();
        }

        if (e != null && e.getKeyCode() == -23) {
            return; // dieses Key-Event wurde von AutoCompletePopupWindow generiert
        }

        if (field.getText().trim().length() == 0) {
            setButtonColor(null);
        }

        String complete = null;
        String prefix = null;
        String base = null;

        Mode mode = Mode.none; // 0
        if (e == null || ((EfaUtil.isRealChar(e) && (e.getKeyCode() != KeyEvent.VK_ENTER) && (e.getKeyCode() != KeyEvent.VK_TAB)) 
        		|| (e.getKeyCode() == KeyEvent.VK_DOWN))){ 
        	mode = Mode.normal; // 1
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            mode = Mode.up; // 2
        } else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            mode = Mode.delete; // 3
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            mode = Mode.enter; // 4
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            mode = Mode.escape; // 5
        }

        //System.out.println("autoComplete("+e+") on "+getName()+" with text '"+field.getText()+"' in mode "+mode);
        if (e == null || mode == Mode.enter || mode == Mode.escape) {
            field.setText(field.getText().trim());
        }

        boolean matching = false;

        if (mode == Mode.normal
                || ((mode == Mode.enter || mode == Mode.escape || mode == Mode.none) && field.getText().length() > 0)) {

            // remove leading spaces
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
                prefix = field.getText().toLowerCase().substring(0, field.getSelectionStart());
            } else {
                prefix = field.getText().toLowerCase();
            }

            if (e != null && e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (withPopup && useAutocompleteList && AutoCompletePopupWindow.isShowingAt(field)) {
                    complete = list.getNext();
                } else {
                    complete = list.getNext(prefix);
                }
                if (complete == null) {
                    complete = list.getFirst(prefix);
                }
            } else {

                if (e != null) {
                    complete = list.getFirst(prefix); // Taste gedrückt --> OK, Wortanfang genügt
                } else {
                    complete = list.getExact(field.getText().toLowerCase()); // keine Taste gedrückt --> nur richtig, wenn gesamtes Feld exakt vorhanden!
                }
                if (list.getAlias(prefix) != null) {
                    complete = list.getAlias(prefix);
                }

            }
            if (e == null && complete != null) {
                complete = list.getExact(complete);
            }
            if (complete != null) {
                if (e != null && mode != Mode.none) { // nur bei wirklichen Eingaben
                    field.setText(complete);
                    field.select(prefix.length(), complete.length());
                }
                matching = true;
            }
            if (withPopup && useAutocompleteList && e != null && mode != Mode.none) {
                AutoCompletePopupWindow.showAndSelect(field, list, (complete != null ? complete : ""), null);
            }
        }

        if (mode == Mode.up) {
            if (field.getSelectedText() != null) {
                prefix = field.getText().toLowerCase().substring(0, field.getSelectionStart());
            } else {
                prefix = field.getText().toLowerCase();
            }

            if (withPopup && useAutocompleteList && AutoCompletePopupWindow.isShowingAt(field)) {
                complete = list.getPrev();
            } else {
                complete = list.getPrev(prefix);
            }

            if (complete == null) {
                complete = list.getLast(prefix); // liste.getFirst(anf);
            }
            if (complete != null) {
                field.setText(complete);
                field.select(prefix.length(), complete.length());
                matching = true;
            }
            if (withPopup && useAutocompleteList) {
                AutoCompletePopupWindow.showAndSelect(field, list, (complete != null ? complete : ""), null);
            }
        }
        
        if (mode == Mode.delete) {
            if ((complete = list.getFirst(field.getText().toLowerCase().trim())) != null
                    && (complete.equals(field.getText()))) {
                matching = true;
            }
        }

        // make sure to accept value as known which have a known base part (everything before ignoreEverythingAfter)
        String ignoredString = null;
        int ignorePos = -1;
        for (int i=0; ignoreEverythingAfter != null && i<ignoreEverythingAfter.length(); i++) {
            ignorePos = (prefix != null ? prefix.indexOf(ignoreEverythingAfter.charAt(i)) :
                field.getText().indexOf(ignoreEverythingAfter.charAt(i)));
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

        // make sure to accept any values with trailing spaces as well
        if (prefix != null && !matching && prefix.endsWith(" ")) {
            String firstInList = list.getFirst(prefix.trim());
            if (firstInList != null && field.getText().startsWith(firstInList)) {
                matching = true;
            }
        }

        // in case of versionized data, make sure it also valid
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
            setButtonColor( (ignoredString == null ? Color.green : Color.yellow) );
        } else {
            setButtonColor( (valid ? Color.red :
                Color.orange ) ); // @todo should be green or orange? used to be orange instead of green; used for hidden records. Color.orange) );
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
        //System.out.println("autoComplete("+e+") on "+getName()+" with text '"+field.getText()+"' -> matching="+matching);
        
        if (mode == Mode.enter) {
            field.select(-1, -1);
            field.setCaretPosition(field.getText().length());
            if (withPopup && useAutocompleteList) {
                AutoCompletePopupWindow.hideWindow();
            }
        }

        if (mode == Mode.escape) {
            if (withPopup && useAutocompleteList) {
                AutoCompletePopupWindow.hideWindow();
            }
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
        for (int i=0; ignoreEverythingAfter != null && i<ignoreEverythingAfter.length(); i++) {
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
        
        if (Daten.efaConfig.getValuePopupContainsMode()) {
        	//if the autocomplete list shall support full text search, so should the spell checking
        	//it is a quite common mistake that the user enters some characters into the field,
        	//selects an item and presses TAB (which leads to a focusLost event, and the selected item is not activated)
        	//so all elements *containing* the entered value should be added.

        	if (list.getExact(name)==null) {
        		//no exact match --> possibly misspelled	

	        	if (neighbours==null) {
	        		neighbours=new Vector<String>();
	        	}
	        	list.setFilterText(name);
	        	addButAvoidDuplicates(neighbours, list.getDataVisibleFiltered());
	        	//neighbours.addAll(list.getDataVisibleFiltered());
	        	neighbours.remove(name);
	        	Collections.sort(neighbours,new EfaSortStringComparator());
	        	list.setFilterText(null);
        	}
        }
        
        if (neighbours != null && neighbours.size() > 0) {
            ItemTypeList item = new ItemTypeList("NAME", IItemType.TYPE_PUBLIC, "",
                    LogString.itemIsUnknown(name, International.getString("Name")) + "\n" +
                   International.getString("Meintest Du ...?"));
            for (int i=0; i<neighbours.size(); i++) {
                item.addItem(neighbours.get(i), neighbours.get(i), null, neighbours.get(i), false, '\0');
            }
            item.setFieldSize(300, 200);

            if (field == null || !field.isValid()) {
                // field is invalid if the entire input dialog has already been closed.
                // This can happen in case of SimpleInputDialog, where this check, triggered
                // by a focusLost event, is actually called when the user hits ENTER at the
                // end of the input. In this case, also the window is closed and the control
                // already returns to the calling thread. This is too late for us to check spelling
                // then.
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
    
    private void addButAvoidDuplicates (Vector <String> target, Vector <String> source) {
    	String value;
    	for (int i=0; i<source.size(); i++) {
    		value = source.get(i);
    		if (!target.contains(value)) {
    			target.add(value);
    		}
    	}
    }
    
    /**
     * Handles keyboard events for AutoCompleteLists when search for partial matches is active.
     * The meaning of arrow up/down events is very different to the one in the historic prefix mode list.
     * @param e KeyEvent
     */
    private void handleFilteredList(KeyEvent e) {

    	if (field == null) {
            return;
        }
        JTextField textField = (JTextField)this.field;	
        
        AutoCompleteList list = getAutoCompleteList();

        if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, this.getName());
        	Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "KeyEvent :"+(e==null? "null": e.toString()));
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "AutoCompleteList.size() = "+(list==null?"null":list.getSizes()));
        }
        
        
        if (list == null) {
            setButtonColor(null);
            return;
        } else {
        	list.update(); 
        	list.setFilterText(textField.getText().trim());
        }

        if (e != null && e.getKeyCode() == -23) {
            return; // dieses Key-Event wurde von AutoCompletePopupWindow generiert
        }

        if (textField.getText().trim().length() == 0) {
            setButtonColor(null);
        }

        Mode mode = Mode.none; // 0
        if (e == null || ((EfaUtil.isRealChar(e) && (e.getKeyCode() != KeyEvent.VK_ENTER) && (e.getKeyCode() != KeyEvent.VK_TAB))
        		|| e.getKeyCode() == KeyEvent.VK_DOWN)
        		|| (e.getKeyCode()==KeyEvent.VK_F && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) 
        		) {
            mode = Mode.normal; // 1
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            mode = Mode.up; // 2
        } else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            mode = Mode.delete; // 3
        } else if ((e.getKeyCode() == KeyEvent.VK_ENTER) || e.getKeyCode() == KeyEvent.VK_TAB) {
            mode = Mode.enter; // 4
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            mode = Mode.escape; // 5
        } 

        if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "Mode "+mode);
        }
        
        if (e == null || mode == Mode.enter || mode == Mode.escape) {
        	textField.setText(textField.getText().trim());
        }
        boolean matching = false;

        String searchFor=textField.getText().trim().toLowerCase();
        String complete="";

        if (mode == Mode.normal || ((mode == Mode.enter || mode == Mode.escape || mode == Mode.none))) {

        	// Down Arrow or STRG+F opens the poup list.
            if (e != null && ((e.getKeyCode() == KeyEvent.VK_DOWN)
            		|| (e.getKeyCode()==KeyEvent.VK_F && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)))) {
                complete = list.getNext(searchFor);
                if (complete == null) {
                    complete = list.getFirst(searchFor);
                }

            } else if (e != null && e.getKeyCode() == KeyEvent.VK_UP){
            	setButtonColor(Color.RED);
                complete = list.getPrev(searchFor);
                if (complete == null) {
                    complete = list.getLast(searchFor);
                }
            } else {
                if ((e != null) && e.getKeyCode() != KeyEvent.VK_DOWN) {
                    if (Daten.efaConfig.getValuePopupContainsModeSelectPrefixItem()) {
                		complete = list.getFirstByPrefix(textField.getText());
                		if (complete==null) {
                    		complete = list.getFirst(textField.getText()); // Taste gedrückt --> OK, Wortanfang genügt
                		}
                	} else {
                		complete = list.getFirst(textField.getText()); // Taste gedrückt --> OK, Wortanfang genügt
                	}         
                }                	
            }

            if (Logger.isTraceOn(Logger.TT_GUI, 5)) {
                Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "User entered text="+searchFor+" matching item="+complete);
                Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "AutoCompleteWindow showing at "+this.getName()+"="+AutoCompletePopupWindow.isShowingAt(textField));
            }
            
            if (e != null && (mode != Mode.normal && (
				            							(e.getKeyCode() == KeyEvent.VK_ENTER) || 
				            							((e.getKeyCode() == KeyEvent.VK_TAB) && !searchFor.isEmpty())
            										  ) && 
            		AutoCompletePopupWindow.isShowingAt(textField))) { 

            	// A key has been pressed, and we are in normal mode.
            	// if the user pressed ENTER, there is no need for text in the actual textfield.
            	// if the user pressed TAB (which is a focuschanged event to another component), the user MUST have entered some search criteria
            	// to use the selected item of the popup list as value. 
            	// This is because the lookup lists do NOT contain an empty value, and always
            	// have selected the first matching value - if the user is just tabbing through the fields, this would be inconvinient if we always
            	// took the first value of the popup field.
            	complete = AutoCompletePopupWindow.getWindow().getSelectedEintrag();
            	if (complete!=null && !complete.isEmpty()) {textField.setText(complete);}
                matching = true;
            }

            // we do not want to do another showandselect if we are just getting and loosing focus...
            if (withPopup && useAutocompleteList && e != null && mode != Mode.none && e.getKeyCode() != KeyEvent.VK_TAB) {
            	AutoCompletePopupWindow.showAndSelect(textField, list, (complete != null ? complete : ""), null);
    	       }
        }

        if (mode == Mode.up) {
            if (withPopup && useAutocompleteList && AutoCompletePopupWindow.isShowingAt(textField)) {
                complete = list.getPrev(searchFor);
            } else {
                complete = list.getPrev(searchFor);
            }

            if (complete == null) {
                complete = list.getLast(searchFor); 
            }
            if (withPopup && useAutocompleteList) {
                AutoCompletePopupWindow.showAndSelect(textField, list, (complete != null ? complete : ""), null);
            }
        }


       if (mode == Mode.delete) {
            if (withPopup && useAutocompleteList && e != null && mode != Mode.none) {
                if (Daten.efaConfig.getValuePopupContainsModeSelectPrefixItem()) {
            		complete = list.getFirstByPrefix(textField.getText());
            		if (complete==null) {
                		complete = list.getFirst(textField.getText()); // Taste gedrückt --> OK, Wortanfang genügt
            		}
            	} else {
            		complete = list.getFirst(textField.getText()); // Taste gedrückt --> OK, Wortanfang genügt
            	}            	
                AutoCompletePopupWindow.showAndSelect(textField, list, (complete != null ? complete : textField.getText()), null);
            }
        }

       matching = (list.getExact(textField.getText())!=null);


        // in case of versionized data, make sure it also valid
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
            setButtonColor( Color.green );
        } else {
        	setButtonColor( (valid ? Color.red : Color.orange ) ); // @todo should be green or orange? used to be orange instead of green; used for hidden records. Color.orange) );        
        }
        
        if (mode == Mode.enter) {
        	textField.select(-1, -1);
        	textField.setCaretPosition(textField.getText().length());
            if (withPopup && useAutocompleteList) {
                AutoCompletePopupWindow.hideWindow();
            }
        }

        if (mode == Mode.escape) {
            if (withPopup && useAutocompleteList) {
                AutoCompletePopupWindow.hideWindow();
            }
        }

        if (textField.getText().length() == 0) {
            setButtonColor(null);
        }
    }

    /**
     * Checks if the currently selected item in ItemTypeStringAutoComplete field is matching to the text the user entered.
     * @return boolean
     */
    public boolean isCurrentTextMatching() {
        AutoCompleteList list = getAutoCompleteList();
        JTextField textField = (JTextField)this.field;	
        if (list != null && textField != null) {
        	return list.getExact(textField.getText())!=null;
        } 
        return false;
    }

    
    /**
     * Checks if the currently selected item in ItemTypeStringAutoComplete field is valid at the time the session takes place
     * (the session type is obtained by validAtDateItem and validAtTimeItem).
     * @return boolean
     */
    public boolean isCurrentTextValid(){
        boolean valid = false;
        JTextField textField = (JTextField)this.field;	
        
        if (textField != null && isCurrentTextMatching() && validAtDateItem != null) {
            long t = LogbookRecord.getValidAtTimestamp(validAtDateItem.getDate(),
                    (validAtTimeItem != null ? validAtTimeItem.getTime() : null));
            valid = autoCompleteList.isValidAt(textField.getText(), t);
        } else {
            valid = false;
        }
        
        return valid;
        
    }
    
    public void acpwCallback(JTextField field) {
        autoComplete(null);
    }

    private AutoCompleteList getAutoCompleteList() {
        return autoCompleteList;
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
            // make sure the entered value is a valid ID
            if (autoCompleteList.getId(value) == null) {
                lastInvalidErrorText = International.getString("Unbekannter Name nicht erlaubt");
                return false;
            }
        }
        return super.isValidInput();
    }
    
    public boolean isAutoCompleteWindowShowing() {
    	if (field!=null) {
    		return AutoCompletePopupWindow.isShowingAt((JTextField) field);
    	}
    	return false;
    			
    }
    
    public void setShowButtonFocusable(Boolean value) {
    	this.showButtonFocusable=value;
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
    	this.otherField=other;
    }

    /**
     * Removes a value from the visible list.
     * This is necessary for efaBaseFrame: when you enter multiple crew members,
     * any assigned person for the session gets removed from the visible list 
     * of the personAutoCompleteList. The original list still contains the value.
     * 
     * Use reset() on the actual AutoCompleteList behind this ItemTypeStringAutoComplete field
     * to un-hide all hidden items.
     * 
     * @param value Value to be removed from the AutoCompleteList
     */
    public void removeFromVisible(String value) {

        if(onChoosenDeleteFromList && !value.isEmpty()) {
            Vector<String> vis = autoCompleteList.getDataVisible();
            if(vis.remove(value)) {
                autoCompleteList.setDataVisible(vis);
            }
        }    	
    }
}
