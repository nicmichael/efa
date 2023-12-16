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

import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;

// @i18n complete

public abstract class ItemType implements IItemType {

    protected String name;
    protected int type;
    protected String category;
    protected String description;

    protected Window dlg;
    protected JComponent field;
    protected IItemListener listener;
    protected String lastValue;
    protected String lastInvalidErrorText = "";
    protected DataKey dataKey; // no purpose other than storing it inside an ItemType, if needed by external class
    protected Object referenceObject; // just a reference to any user-defined object

    protected Color color = null;
    protected Color savedFgColor = null;
    protected Color backgroundColor = null;
    protected Color savedBkgColor = null;
    protected Color backgroundColorWhenFocused = null;
    protected Border border = null;
    protected int padXbefore = 0;
    protected int padXafter = 0;
    protected int padYbefore = 0;
    protected int padYafter = 2; //one pixel of space after every item on the gui. better readability.
    protected boolean notNull = false;
    protected int fieldWidth = 300;
    protected int fieldHeight = 21;
    protected int fieldGridWidth = 1;
    protected int fieldGridHeight = 1;
    protected int fieldGridAnchor = GridBagConstraints.WEST;
    protected int fieldGridFill = GridBagConstraints.NONE;
    protected int hAlignment = -1;
    protected boolean isVisible = true;
    protected boolean isEnabled = true;
    protected boolean isEditable = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String s) {
        category = s;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String s) {
        description = s;
    }

    public void setColor(Color c) {
        this.color = c;
        if (field != null) {
            field.setForeground(c);
        }
    }

    public void saveColor() {
        if (field != null) {
            savedFgColor = field.getForeground();
        } else {
            savedFgColor = Color.black;
        }
    }

    public void restoreColor() {
        if (field != null && savedFgColor != null) {
            field.setForeground(savedFgColor);
        }
    }

    public void setBackgroundColor(Color c) {
        this.backgroundColor = c;
        if (field != null) {
            field.setBackground(c);
        }
    }

    public Color getBackgroundColor() {
    	return this.backgroundColor;
    }
    
    public Color getColor() {
    	return this.color;
    }
    
    public void saveBackgroundColor(boolean force) {
        if (field != null && (savedBkgColor == null || force)) {
            savedBkgColor = field.getBackground();
        }
    }

    public void restoreBackgroundColor() {
        if (field != null && savedBkgColor != null) {
            field.setBackground(savedBkgColor);
        }
    }

    public void setBackgroundColorWhenFocused(Color color) {
        backgroundColorWhenFocused = color;
    }

    public void requestFocus() {
        if (field != null) {
            field.requestFocus();
        }
    }

    public boolean hasFocus() {
        return (field != null && field.hasFocus());
    }

    public void setPadding(int padXbefore, int padXafter, int padYbefore, int padYafter) {
        this.padXbefore = padXbefore;
        this.padXafter = padXafter;
        this.padYbefore = padYbefore;
        this.padYafter = padYafter;
    }

    public void setFieldSize(int width, int height) {
        fieldWidth = (width > 0 ? width : fieldWidth);
        fieldHeight = (height > 0 ? height : fieldHeight);
    }

    public void setFieldGrid(int gridWidth, int gridAnchor, int gridFill) {
        if (gridWidth >= 0) {
            fieldGridWidth = gridWidth;
        }
        if (gridAnchor >= 0) {
            fieldGridAnchor = gridAnchor;
        }
        if (gridFill >= 0) {
            fieldGridFill = gridFill;
        }
    }

    public void setBorder(Border border) {
        this.border = border;
        if (field != null) {
            field.setBorder(border);
        }
    }

    public void setFieldGrid(int gridWidth, int gridHeight, int gridAnchor, int gridFill) {
        if (gridWidth >= 0) {
            fieldGridWidth = gridWidth;
        }
        if (gridHeight >= 0) {
            fieldGridHeight = gridHeight;
        }
        if (gridAnchor >= 0) {
            fieldGridAnchor = gridAnchor;
        }
        if (gridFill >= 0) {
            fieldGridFill = gridFill;
        }
    }

    public void setHorizontalAlignment(int hAlignment) {
        this.hAlignment = hAlignment;
    }

    protected abstract void iniDisplay();

    public int displayOnGui(Window dlg, JPanel panel, int y) {
        return displayOnGui(dlg, panel, 0, y);
    }

    public void parseAndShowValue(String value) {
        if (value != null) {
            parseValue(value);
        } else {
            parseValue("");
        }
        showValue();
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    public boolean isNotNullSet() {
        return notNull;
    }

    protected void field_focusGained(FocusEvent e) {
        if (backgroundColorWhenFocused != null) {
            saveBackgroundColor(false);
            setBackgroundColor(backgroundColorWhenFocused);
        }
        actionEvent(e);
    }
    protected void field_focusLost(FocusEvent e) {
        if (backgroundColorWhenFocused != null) {
            restoreBackgroundColor();
        }
        actionEvent(e);
    }

    public void setChanged() {
        lastValue = null;
    }

    public void setUnchanged() {
        lastValue = toString();
    }

    public boolean isChanged() {
        String s = toString();
        if ((s == null || s.length() == 0) && (lastValue == null || lastValue.length() == 0)) {
            return false;
        }
        if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
            if (s != null && !s.equals(lastValue)) {
                Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI,
                    getName() + ": old=" + lastValue + "; new=" + s);
            }
        }
        return (s != null && !s.equals(lastValue)) ||
               (lastValue != null && !lastValue.equals(s)) ;
    }

    public void registerItemListener(IItemListener listener) {
        this.listener = listener;
    }

    public void actionEvent(AWTEvent e) {
        if (listener != null && e != null) {
            listener.itemListenerAction(this, e);
        }
    }

    public JDialog getParentDialog() {
        if (dlg != null && dlg instanceof JDialog) {
            return (JDialog)dlg;
        }
        return null;
    }

    public JFrame getParentFrame() {
        if (dlg != null && dlg instanceof JFrame) {
            return (JFrame)dlg;
        }
        return null;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEditable(boolean editable) {
        isEditable = editable;
    }

    public boolean isEditable() {
        return isEditable;
    }

    // methods which allow to store a DataKey inside an IItemType (only for special purposes)
    public void setDataKey(DataKey k) {
        this.dataKey = k;
    }
    public DataKey getDataKey() {
        return this.dataKey;
    }

    public JComponent getComponent() {
        return field;
    }

    public void setReferenceObject(Object o) {
        this.referenceObject = o;
    }

    public Object getReferenceObject() {
        return referenceObject;
    }


    public String getInvalidErrorText() {
        return International.getMessage("Ungültige Eingabe im Feld '{field}'",
                        getDescription()) + ": " + lastInvalidErrorText;
    }

}
