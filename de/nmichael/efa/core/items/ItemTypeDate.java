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

import de.nmichael.efa.util.*;
import de.nmichael.efa.data.types.DataTypeDate;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete

public class ItemTypeDate extends ItemTypeLabelTextfield {

    private DataTypeDate value;
    private DataTypeDate referenceDate;
    private boolean allowYearOnly = false;
    private boolean allowMonthAndYearOnly = false;
    private boolean forceDayAndMonthOnly = false;
    protected boolean showWeekday;
    protected JLabel weekdayLabel;
    protected int weekdayGridWidth = 1;
    protected int weekdayGridAnchor = GridBagConstraints.WEST;
    protected int weekdayGridFill = GridBagConstraints.NONE;
    protected ItemTypeDate mustBeBefore;
    protected ItemTypeDate mustBeAfter;
    protected boolean mustBeCanBeEqual = false;

    public ItemTypeDate(String name, DataTypeDate value, int type,
            String category, String description) {
        this.name = name;
        this.value = (value != null ? value : new DataTypeDate());
        this.type = type;
        this.category = category;
        this.description = description;
        this.referenceDate = (isSet() ? new DataTypeDate(value) : DataTypeDate.today());
    }

    public IItemType copyOf() {
        ItemTypeDate copy = new ItemTypeDate(name, new DataTypeDate(value), type, category, description);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setIcon((label == null ? null : label.getIcon()));
        return copy;
    }

    public void showWeekday(boolean showWeekday) {
        this.showWeekday = showWeekday;
    }

    public void parseValue(String value) {
        try {
            value = EfaUtil.dateToDMY(value);
            if (value != null && value.trim().length()>0) {
                value = EfaUtil.replaceString(value, "-", " ");
                value = EfaUtil.replaceString(value, "+", " ");
                if (allowYearOnly || allowMonthAndYearOnly || forceDayAndMonthOnly) {
                    TMJ tmj = EfaUtil.string2date(value, -1, -1, -1);
                    if (tmj.tag >= 0 && tmj.monat == -1 && tmj.jahr == -1 && allowYearOnly) {
                        this.value.setDate(0, 0, tmj.tag);
                        return;
                    }
                    if (tmj.tag >= 0 && tmj.monat >= 0 && tmj.jahr == -1 && allowMonthAndYearOnly) {
                        this.value.setDate(0, tmj.tag, tmj.monat);
                        return;
                    }
                    if (forceDayAndMonthOnly) {
                        this.value.setDate(tmj.tag, tmj.monat, 0);
                        return;
                    }
                }
                TMJ tmj = EfaUtil.correctDate(value, referenceDate.getDay(), referenceDate.getMonth(), referenceDate.getYear());
                this.value.setDate(tmj.tag, tmj.monat, tmj.jahr);
            } else {
                unset();
            }
        } catch (Exception e) {
            if (dlg == null) {
                Logger.log(Logger.ERROR, Logger.MSG_CORE_UNSUPPORTEDDATATYPE,
                           "Invalid value for parameter "+name+": "+value);
            }
        }
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        int plusy = super.displayOnGui(dlg, panel, x, y);
        if (showWeekday) {
            weekdayLabel = new JLabel();
            panel.add(weekdayLabel, new GridBagConstraints(x+labelGridWidth+fieldGridWidth, y, weekdayGridWidth, 1, 0.0, 0.0,
                    weekdayGridAnchor, weekdayGridFill, new Insets(padYbefore, 0, padYafter, 0), 0, 0));
        }
        return plusy;
    }

    protected void field_focusLost(FocusEvent e) {
        super.field_focusLost(e);
        if (!isSet() && isNotNullSet()) {
            if (referenceDate.isSet()) {
                parseValue(referenceDate.toString());
            } else {
                parseValue(DataTypeDate.today().toString());
            }
            showValue();
        }
        if (isSet()) {
            referenceDate.setDate(value);
        }
    }

    protected void updateWeekday() {
        if (showWeekday && weekdayLabel != null) {
            if (isSet()) {
                weekdayLabel.setText(" (" + value.getWeekdayAsString() + ")");
            } else {
                weekdayLabel.setText("");
            }
        }
    }

    public void showValue() {
        super.showValue();
        updateWeekday();
    }

    public String toString() {
        return (value != null ? value.toString() : "");
    }

    public boolean isSet() {
        return value != null && value.isSet();
    }

    public int getValueDay() {
        return value.getDay();
    }

    public int getValueMonth() {
        return value.getMonth();
    }

    public int getValueYear() {
        return value.getYear();
    }

    public DataTypeDate getDate() {
        return new DataTypeDate(value);
    }

    public void setValueDate(DataTypeDate date) {
        value = new DataTypeDate(date);
    }

    public void setValueDay(int day) {
        value.setDay(day);
    }

    public void setValueMonth(int month) {
        value.setMonth(month);
    }

    public void setValueYear(int year) {
        value.setYear(year);
    }

    public void unset() {
        value.unset();
    }

    public void setAllowYearOnly(boolean allowYearOnly) {
        this.allowYearOnly = allowYearOnly;
    }

    public void setAllowMonthAndYearOnly(boolean allowMonthAndYearOnly) {
        this.allowMonthAndYearOnly = allowMonthAndYearOnly;
    }

    public void setForceDayAndMonthOnly(boolean forceDayAndMonthOnly) {
        this.forceDayAndMonthOnly = forceDayAndMonthOnly;
    }

    public boolean isValidInput() {
        if (mustBeBefore != null && isSet() && value.isSet() && !value.isBefore(mustBeBefore.value)) {
            return mustBeCanBeEqual && value.equals(mustBeBefore.value);
        }
        if (mustBeAfter != null && isSet() && value.isSet() && !value.isAfter(mustBeAfter.value)) {
            return mustBeCanBeEqual && value.equals(mustBeAfter.value);
        }
        if (isNotNullSet()) {
            return isSet();
        }
        return true;
    }

    public void setMustBeBefore(ItemTypeDate item, boolean mayAlsoBeEqual) {
        mustBeBefore = item;
        mustBeCanBeEqual = mayAlsoBeEqual;
    }

    public void setMustBeAfter(ItemTypeDate item, boolean mayAlsoBeEqual) {
        mustBeAfter = item;
        mustBeCanBeEqual = mayAlsoBeEqual;
    }

    public void setWeekdayGrid(int gridWidth, int gridAnchor, int gridFill) {
        weekdayGridWidth = gridWidth;
        weekdayGridAnchor = gridAnchor;
        weekdayGridFill = gridFill;
    }

    // @override
    public void actionEvent(AWTEvent e) {
        if (e != null && e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED) {
            if (!isSet()) {
                value.setDate(referenceDate);
            }
            switch(((KeyEvent)e).getKeyCode()) {
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_ADD:
                case KeyEvent.VK_UP:
                case KeyEvent.VK_KP_UP:
                    value.addDays(1);
                    showValue();
                    break;
                case KeyEvent.VK_MINUS:
                case KeyEvent.VK_SUBTRACT:
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_KP_DOWN:
                    value.addDays(-1);
                    showValue();
                    break;
            }
        }
        super.actionEvent(e);
    }
    
}
