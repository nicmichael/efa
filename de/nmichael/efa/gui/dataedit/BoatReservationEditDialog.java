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

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.util.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

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
            if (item.getName().equals(BoatReservationRecord.TYPE)) {
                ((ItemTypeRadioButtons)item).registerItemListener(this);
                itemType = item;
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
        if (item != null && item.getName().equals(BoatReservationRecord.TYPE)) {
            String type = item.getValueFromField();
            if (type == null) {
                return;
            }
            for (IItemType it : allGuiItems) {
                if (it.getName().equals(BoatReservationRecord.DAYOFWEEK)) {
                    it.setVisible(type.equals(BoatReservationRecord.TYPE_WEEKLY) ||
                    		type.equals(BoatReservationRecord.TYPE_WEEKLY_LIMITED));
                }
                if (it.getName().equals(BoatReservationRecord.DATEFROM)) {
                    it.setVisible(type.equals(BoatReservationRecord.TYPE_ONETIME) ||
                    		type.equals(BoatReservationRecord.TYPE_WEEKLY_LIMITED));
                }
                if (it.getName().equals(BoatReservationRecord.DATETO)) {
                    it.setVisible(type.equals(BoatReservationRecord.TYPE_ONETIME) ||
                    		type.equals(BoatReservationRecord.TYPE_WEEKLY_LIMITED));
                }
            }
        }
    }

    private void setAllowWeeklyReservation(boolean allowWeeklyReservation) throws Exception {
        if (!allowWeeklyReservation) {
            if (!newRecord && dataRecord != null &&
                    (BoatReservationRecord.TYPE_WEEKLY.equals(((BoatReservationRecord)dataRecord).getType()) ||
                            BoatReservationRecord.TYPE_WEEKLY_LIMITED.equals(((BoatReservationRecord)dataRecord).getType()))) {
                throw new Exception(International.getString("Diese Reservierung kann nicht bearbeitet werden."));
            }
            for (IItemType it : allGuiItems) {
                if (it.getName().equals(BoatReservationRecord.TYPE)) {
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
