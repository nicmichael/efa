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

import java.awt.AWTEvent;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.GregorianCalendar;

import javax.swing.JLabel;
import javax.swing.JPanel;

import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.TMJ;

// @i18n complete

public class ItemTypeDate extends ItemTypeLabelTextfield {

    private DataTypeDate value;
    private DataTypeDate referenceDate;
    private boolean allowYearOnly = false;
    private boolean allowMonthAndYearOnly = false;
    private boolean forceDayAndMonthOnly = false;
    private boolean isYearAutocompletionForFuture = false;
    protected boolean showWeekday;
    protected JLabel weekdayLabel;
    protected int weekdayGridWidth = 1;
    protected int weekdayGridAnchor = GridBagConstraints.WEST;
    protected int weekdayGridFill = GridBagConstraints.NONE;
    protected ItemTypeDate mustBeBefore;
    protected ItemTypeDate mustBeAfter;
    protected boolean mustBeCanBeEqual = false;
	private boolean hasBeenFutureCompleted;
    

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
        copy.setFieldSize(fieldWidth, fieldHeight);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setIcon((label == null ? null : label.getIcon()));
        copy.setIsItemOnSameRowAsPreviousItem(itemOnSameRowAsPreviousItem);
        copy.setItemOnNewRow(itemOnNewRow);
        copy.setFieldGrid(fieldGridWidth,fieldGridHeight,fieldGridAnchor,fieldGridFill);
        copy.setIndent(this.isIndent());
        copy.setYearAutocompletionForFuture(this.isYearAutocompletionForFuture());
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
                TMJ userEnteredTMJ = EfaUtil.string2date(value, -1, -1, -1);
                if (!isYearAutocompletionForFuture) { 
                    this.value.setDate(tmj.tag, tmj.monat, tmj.jahr);
                    this.setHasBeenFutureCompleted(false);
				} else {
					//if the resulting tmj is in the future, everything is fine.
					//if it is in the past, we add one year to the resulting tmj, so that it is in the future.
					//this is needed for reservations, where the user might enter a date like "01.01" 
					//and expects it to be in the future, not in the past.
					GregorianCalendar today = new GregorianCalendar();
					today.clear();
					today.set(DataTypeDate.today().getYear(), DataTypeDate.today().getMonth()-1, DataTypeDate.today().getDay());
	                boolean isToday = (tmj.toCalendar().equals(today));
	                boolean isFuture = tmj.toCalendar().after(today);
	                this.setHasBeenFutureCompleted(false);
	                //only adapt the year if the reference date is not set, 
	                //otherwise we assume that the user wants to enter a date in the past or present.
					if (!isToday && userEnteredTMJ.jahr<=0 && !isFuture ) {
	                	tmj.jahr = today.get(GregorianCalendar.YEAR) + 1;
		                this.setHasBeenFutureCompleted(true);
	                }
					this.value.setDate(tmj.tag, tmj.monat, tmj.jahr);

				}
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
            //respect the indent property of the item, if set
            x=(this.isIndent() ? x+1 : x);
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
    	lastInvalidErrorText = "";
        if (mustBeBefore != null && isSet() && value.isSet() && !value.isBefore(mustBeBefore.value)) {
            boolean ok = mustBeCanBeEqual && value.equals(mustBeBefore.value);
            if (!ok) { lastInvalidErrorText = International.getMessage("Das Datum {Datum} muss vor dem {VorDatum} liegen.", value.toString(), mustBeBefore.value.toString());}
        	return ok;
        }
        if (mustBeAfter != null && isSet() && value.isSet() && !value.isAfter(mustBeAfter.value)) {
        	boolean ok = mustBeCanBeEqual && value.equals(mustBeAfter.value);
            if (!ok) { lastInvalidErrorText = International.getMessage("Das Datum {Datum} muss nach dem {NachDatum} liegen.", value.toString(), mustBeAfter.value.toString());}
        	return ok;
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

	public boolean isYearAutocompletionForFuture() {
		return isYearAutocompletionForFuture;
	}

	public void setYearAutocompletionForFuture(boolean isYearAutocompletionForFuture) {
		this.isYearAutocompletionForFuture = isYearAutocompletionForFuture;
	}

	public boolean isHasBeenFutureCompleted() {
		return hasBeenFutureCompleted;
	}

	public void setHasBeenFutureCompleted(boolean hasBeenFutureCompleted) {
		this.hasBeenFutureCompleted = hasBeenFutureCompleted;
	}
    
}
