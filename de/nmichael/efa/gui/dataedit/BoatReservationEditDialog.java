/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;

import javax.swing.JDialog;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeRadioButtons;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.util.International;

// @i18n complete
public class BoatReservationEditDialog extends UnversionizedDataEditDialog implements IItemListener {

    public BoatReservationEditDialog(Frame parent, BoatReservationRecord r, 
            boolean newRecord, boolean allowWeeklyReservation, AdminRecord admin) throws Exception {
        super(parent, International.getString("Reservierung"), r, newRecord, admin);
        initListener();
        setAllowWeeklyReservation(allowWeeklyReservation);
    }

    public BoatReservationEditDialog(JDialog parent, BoatReservationRecord r, 
            boolean newRecord, boolean allowWeeklyReservation, AdminRecord admin) throws Exception {
        super(parent, International.getString("Reservierung"), r, newRecord, admin);
        initListener();
        setAllowWeeklyReservation(allowWeeklyReservation);
    }
    public BoatReservationEditDialog(JDialog parent, BoatReservationRecord r, 
            boolean newRecord, boolean allowWeeklyReservation, AdminRecord admin, String title) throws Exception {
        super(parent, title, r, newRecord, admin);
        initListener();
        setAllowWeeklyReservation(allowWeeklyReservation);
    }    

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }
    
    private void initListener() {
        IItemType itemType = null;
        for (IItemType item : allGuiItems) {
            // Check the type as well as the name: a name match alone would cause a ClassCastException
            // if an item with this name had an unexpected class
            if (BoatReservationRecord.TYPE.equals(item.getName()) && item instanceof ItemTypeRadioButtons) {
                ((ItemTypeRadioButtons) item).registerItemListener(this);
                itemType = item;
            } else if ((BoatReservationRecord.DATEFROM.equals(item.getName())
                    || BoatReservationRecord.DATETO.equals(item.getName()))
                    && item instanceof ItemTypeDate) {
                ((ItemTypeDate) item).registerItemListener(this);
            }
        }
        itemListenerAction(itemType, null);
    }

    /*
     * set the visibility of several date/time fields depending of the type of the reservation
     * ONETIME: FROM_DATE, TO_DATE, FROM_TIME, TO_TIME
     * WEEKLY: WEEKDAY, FROM_TIME, TO_TIME
     * WEEKLY_LIMITED: WEEKDAY, FROM_DATE, TO DATE, FROM_TIME,TO_TIME
     */
    public void itemListenerAction(IItemType item, AWTEvent event) {
        if (item != null && BoatReservationRecord.TYPE.equals(item.getName())) {
            String type = item.getValueFromField();
            if (type == null) {
                return;
            }
            boolean isWeekly = type.equals(BoatReservationRecord.TYPE_WEEKLY);
            for (IItemType it : allGuiItems) {
                if (BoatReservationRecord.DAYOFWEEK.equals(it.getName())) {
                    it.setVisible(type.equals(BoatReservationRecord.TYPE_WEEKLY) ||
                            type.equals(BoatReservationRecord.TYPE_WEEKLY_LIMITED));
                } else if (BoatReservationRecord.DATEFROM.equals(it.getName())
                        || BoatReservationRecord.DATETO.equals(it.getName())) {
                    it.setVisible(type.equals(BoatReservationRecord.TYPE_ONETIME) ||
                            type.equals(BoatReservationRecord.TYPE_WEEKLY_LIMITED));
                }
            }
            // Update the hint only once, after all visibilities are final:
            // every call ends in a full relayout (pack), which is expensive
            showHintIfDateFieldsHaveAutocompletedYear();
        } else if (item != null && (item.getName().equals(BoatReservationRecord.DATEFROM)
                || item.getName().equals(BoatReservationRecord.DATETO))
                && event != null && event instanceof FocusEvent && ((FocusEvent) event).getID() == FocusEvent.FOCUS_LOST) {
            showHintIfDateFieldsHaveAutocompletedYear();
        }
    }

    /**
     * Show a hint if the year element of a date field has been autocompleted to NEXT YEAR.
     * As this is a modification of the value entered by the user,
     * it is important to inform the user about this.
     * Only visible date fields are considered, because a hidden field (e.g. for WEEKLY
     * reservations) must not keep the hint alive.
     */
    private void showHintIfDateFieldsHaveAutocompletedYear() {
        boolean dateFromHasFutureCompleted = false;
        boolean dateToHasFutureCompleted = false;
        for (IItemType it : allGuiItems) {
            if (it instanceof ItemTypeDate && BoatReservationRecord.DATEFROM.equals(it.getName())) {
                ItemTypeDate dateFromItem = (ItemTypeDate) it;
                dateFromHasFutureCompleted = dateFromItem.isVisible() && dateFromItem.isHasBeenFutureCompleted();
            } else if (it instanceof ItemTypeDate && BoatReservationRecord.DATETO.equals(it.getName())) {
                ItemTypeDate dateToItem = (ItemTypeDate) it;
                dateToHasFutureCompleted = dateToItem.isVisible() && dateToItem.isHasBeenFutureCompleted();
            }
        }
        boolean showHint = dateFromHasFutureCompleted || dateToHasFutureCompleted;
        for (IItemType it : allGuiItems) {
            // Null-safe: a missing name must not cause a NullPointerException
            String name = it.getName();
            if (name != null && name.endsWith(BoatReservationRecord.HINT_YEARCHANGED)) {
                it.setDescription(International.getString("Das Jahr wurde automatisch auf das nächste Jahr gesetzt"));
                it.setVisible(showHint);
                break;
            }
        }
        resizeDialogToContent();
    }
    
    private void setAllowWeeklyReservation(boolean allowWeeklyReservation) throws Exception {
        if (!allowWeeklyReservation) {
            if (!newRecord && dataRecord != null &&
                    (BoatReservationRecord.TYPE_WEEKLY.equals(((BoatReservationRecord)dataRecord).getType()) ||
                            BoatReservationRecord.TYPE_WEEKLY_LIMITED.equals(((BoatReservationRecord)dataRecord).getType()))) {
                throw new Exception(International.getString("Diese Reservierung kann nicht bearbeitet werden."));
            }
            for (IItemType it : allGuiItems) {
                if (BoatReservationRecord.TYPE.equals(it.getName())) {
                    it.parseAndShowValue(BoatReservationRecord.TYPE_ONETIME);
                    it.setVisible(false);
                    it.setEditable(false);
                    itemListenerAction(it, null);
                    break;
                }
            }
        }
    }


}