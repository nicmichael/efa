/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.efa1;

import de.nmichael.efa.util.*;

// @i18n complete


public class BoatReservation implements Comparable {

    // important to keep order of weekdays starting with Monday (as in ReservierungenEditFrame)!
    public static final String[] WEEKDAYKEYS  = { "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" };

    private boolean oneTimeReservation;
    private String dateFrom;
    private String dateTo;
    private String timeFrom;
    private String timeTo;
    private String forName;
    private String reason;
    public long validInMinutes = 0; // internes Feld, wird nicht gespeichert, sondern nur von getReservierung(...) verwendet

    public void setOneTimeReservation(boolean oneTimeReservation) {
        this.oneTimeReservation = oneTimeReservation;
    }

    public boolean isOneTimeReservation() {
        return oneTimeReservation;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public String getWeekdayFrom() {
        return getWeekday(dateFrom);
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public String getWeekdayTo() {
        return getWeekday(dateTo);
    }
     public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public String getForName() {
        return forName;
    }

    public void setForName(String forName) {
        this.forName = forName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getTimeFrom() {
        return timeFrom;
    }

    public void setTimeFrom(String timeFrom) {
        this.timeFrom = timeFrom;
    }

    public String getTimeTo() {
        return timeTo;
    }

    public void setTimeTo(String timeTo) {
        this.timeTo = timeTo;
    }

    private String getWeekday(String date) {
        if (oneTimeReservation) {
            return null;
        }
        for (int i=0; i<WEEKDAYKEYS.length; i++) {
            if (WEEKDAYKEYS[i].equals(date)) {
                switch(i) {
                    case 0: return International.getString("Montag");
                    case 1: return International.getString("Dienstag");
                    case 2: return International.getString("Mittwoch");
                    case 3: return International.getString("Donnerstag");
                    case 4: return International.getString("Freitag");
                    case 5: return International.getString("Samstag");
                    case 6: return International.getString("Sonntag");
                }
            }
        }
        return null;
    }

    public int compareTo(Object o) throws ClassCastException {
        BoatReservation b = (BoatReservation) o;
        if (this.oneTimeReservation != b.oneTimeReservation) {
            return (this.oneTimeReservation ? -1 : 1);
        }
        if (this.oneTimeReservation) { // beides einmalige Reservierungen
            if (!this.dateFrom.equals(b.dateFrom)) {
                return (EfaUtil.secondDateIsAfterFirst(this.dateFrom, b.dateFrom) ? -1 : 1);
            }
            if (!this.timeFrom.equals(b.timeFrom)) {
                return (EfaUtil.secondTimeIsAfterFirst(this.timeFrom, b.timeFrom) ? -1 : 1);
            }
            if (!this.dateTo.equals(b.dateTo)) {
                return (EfaUtil.secondDateIsAfterFirst(this.dateTo, b.dateTo) ? -1 : 1);
            }
            if (!this.timeTo.equals(b.timeTo)) {
                return (EfaUtil.secondTimeIsAfterFirst(this.timeTo, b.timeTo) ? -1 : 1);
            }
            return 0;
        }
        // beide Reservierungen wöchentlich
        if (!this.dateFrom.equals(b.dateFrom)) {
            for (int i = 0; i < WEEKDAYKEYS.length; i++) {
                if (this.dateFrom.equals(WEEKDAYKEYS[i])) {
                    return -1;
                }
                if (b.dateFrom.equals(WEEKDAYKEYS[i])) {
                    return 1;
                }
            }
        }
        if (!this.timeFrom.equals(b.timeFrom)) {
            return (EfaUtil.secondTimeIsAfterFirst(this.timeFrom, b.timeFrom) ? -1 : 1);
        }
        if (!this.timeTo.equals(b.timeTo)) {
            return (EfaUtil.secondTimeIsAfterFirst(this.timeTo, b.timeTo) ? -1 : 1);
        }
        return 0;
    }
}
