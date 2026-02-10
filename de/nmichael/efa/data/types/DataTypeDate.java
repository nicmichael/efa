/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.data.types;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.TMJ;
import java.util.Date;

public class DataTypeDate implements Cloneable, Comparable<DataTypeDate> {

    public static final String DAY_MONTH_YEAR = "DD.MM.YYYY";
    public static final String MONTH_DAY_YEAR = "MM/DD/YYYY";

    private int day, month, year;

    // Default Constructor
    public DataTypeDate() {
        unset();
    }

    // Regular Constructor
    public DataTypeDate(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
        ensureCorrectDate();
    }

    public DataTypeDate(long timestamp) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timestamp);
        this.day = cal.get(Calendar.DAY_OF_MONTH);
        this.month = cal.get(Calendar.MONTH)+1;
        this.year = cal.get(Calendar.YEAR);
        ensureCorrectDate();
    }

    // Copy Constructor
    public DataTypeDate(DataTypeDate date) {
        this.day = date.day;
        this.month = date.month;
        this.year = date.year;
        ensureCorrectDate();
    }

    public static DataTypeDate parseDate(String s) {
        DataTypeDate date = new DataTypeDate();
        date.setDate(s);
        return date;
    }

    public static DataTypeDate today() {
        DataTypeDate date = new DataTypeDate();
        Calendar cal = new GregorianCalendar();
        date.setDay(cal.get(Calendar.DAY_OF_MONTH));
        date.setMonth(cal.get(Calendar.MONTH)+1);
        date.setYear(cal.get(Calendar.YEAR));
        return date;
    }

    public void setDate(DataTypeDate date) {
        this.day = date.day;
        this.month = date.month;
        this.year = date.year;
        ensureCorrectDate();
    }

    public void setDate(Calendar date) {
        this.day = date.get(GregorianCalendar.DAY_OF_MONTH);
        this.month = date.get(GregorianCalendar.MONTH) + 1;
        this.year = date.get(GregorianCalendar.YEAR);
        ensureCorrectDate();
    }

    public void setDate(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
        ensureCorrectDate();
    }

    public void setDate(String s) {
        TMJ tmj = EfaUtil.string2date(s, -1, -1, -1);
        boolean ymd = (tmj.tag > 0 && tmj.jahr > 0 && tmj.tag > 1900 && tmj.jahr < 100);
        if (!ymd) {
            if (s != null && s.indexOf("/") > 0) {
                // month/day/year
                this.day = tmj.monat;
                this.month = tmj.tag;
                this.year = tmj.jahr;
            } else {
                // day.month,year
                this.day = tmj.tag;
                this.month = tmj.monat;
                this.year = tmj.jahr;
            }
        } else {
            // year - month - day
            this.year = tmj.tag;
            this.month = tmj.monat;
            this.day = tmj.jahr;
        }
        if (day > 0 && month == -1 && year == -1) { // String with year only
            year = day;
            day = month = 0;
        }
        if (day > 0 && month > 0 && year == -1) { // String with month and year only
            year = month;
            month = day;
            day = 0;
        }
        ensureCorrectDate();
    }

    public void setMonthAndYear(String s) {
        TMJ tmj = EfaUtil.string2date(s, -1, -1, -1);
        this.day = 0;
        this.month = tmj.tag;
        this.year = tmj.monat;
        ensureCorrectDate();
    }

    public void setYear(String s) {
        TMJ tmj = EfaUtil.string2date(s, -1, -1, -1);
        this.day = 0;
        this.month = 0;
        this.year = tmj.tag;
        ensureCorrectDate();
    }

    public GregorianCalendar toCalendar() {
        int _day = (day > 0 ? day : 1);       // if day is not specified (day==0), assume first of month
        int _month = (month > 0 ? month : 1); // if month is not specified (month==0), assume first of year
        return new GregorianCalendar(year, _month - 1, _day);
    }

    public void ensureCorrectDate() {
    	
        if (!isSet()) {
            return;
        }
        boolean fourdigit = year >= 1000 && year <= 9999;
        if (year < 0 || year > 9999) {
            year = 0;
        }
        if (!fourdigit && year < 1900) {
            year += 1900;
        }
        // see efautil.correctDate() -> everything <1980 will be put to the next century.
        if (!fourdigit && year < 1980) {
            year += 100; 
        }
        if (month < 0 || month > 12) { // treat month==0 as "unset month" and don't correct it
            month = 1;
        }
        if (day < 0 || day > 31) { // treat day==0 as "unset day" and don't correct it
            day = 1;
        }
        switch (month) {
            case 4:
            case 6:
            case 9:
            case 11:
                if (day > 30) {
                    day = 30;
                }
                break;
            case 2:
                if (day > 29) {
                    day = 29;
                }
                if (day > 28 && year % 4 != 0) {
                    day = 28;
                }
                break;
            default:
        }
    }

    public String toString() {
        if (day < 0 || month < 0 || year < 0) {
            return "";
        }
        if (month == 0) {
            return EfaUtil.int2String(year,4);
        }
        if (day == 0) {
            return EfaUtil.int2String(month,2) + "/" + EfaUtil.int2String(year,4);
        }
        return Daten.dateFormatDMY ?
                EfaUtil.int2String(day,2) + "." + EfaUtil.int2String(month,2) + "." + EfaUtil.int2String(year,4) :
                EfaUtil.int2String(month,2) + "/" + EfaUtil.int2String(day,2) + "/" + EfaUtil.int2String(year,4);
    }

    public boolean isSet() {
        return day != -1 && month != -1 && year != -1;
    }

    public Date getDate() {
    	return new Date(year-1900,month-1,day);//that's the initialisation according javadoc
    }
    
    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public void setDay(int day) {
        this.day = day;
        ensureCorrectDate();
    }

    public void setMonth(int month) {
        this.month = month;
        ensureCorrectDate();
    }

    public void setYear(int year) {
        this.year = year;
        ensureCorrectDate();
    }

    public void setDayMonth(int day, int month) {
        this.day = day;
        this.month = month;
        ensureCorrectDate();
    }

    public void addDays(int days) {
        if (!isSet()) {
            return;
        }
        Calendar cal = toCalendar();
        cal.add(GregorianCalendar.DATE, days);
        setDate(cal);
    }

    public void unset() {
        day = -1;
        month = -1;
        year = -1;
    }

    public boolean equals(Object o) {
        try {
            return compareTo((DataTypeDate)o) == 0;
        } catch(Exception e) {
            return false;
        }
    }

    public int compareTo(DataTypeDate o) {
        if (year < o.year) {
            return -1;
        }
        if (year > o.year) {
            return 1;
        }
        if (month < o.month) {
            return -1;
        }
        if (month > o.month) {
            return 1;
        }
        if (day < o.day) {
            return -1;
        }
        if (day > o.day) {
            return 1;
        }
        return 0;
    }

    public int hashCode() {
        return (new Integer(year*10000 + month*100 + day)).hashCode();
    }

    public boolean isBefore(DataTypeDate o) {
        return compareTo(o) < 0;
    }

    public boolean isBeforeOrEqual(DataTypeDate o) {
        return compareTo(o) <= 0;
    }

    public boolean isAfter(DataTypeDate o) {
        return compareTo(o) > 0;
    }

    public boolean isAfterOrEqual(DataTypeDate o) {
        return compareTo(o) >= 0;
    }

    public boolean isInRange(DataTypeDate from, DataTypeDate to) {
        return (compareTo(from) >= 0) && (compareTo(to) <= 0);
    }

    public static boolean isRangeOverlap(DataTypeDate r1From, DataTypeDate r1To, DataTypeDate r2From, DataTypeDate r2To) {
        return (r2From.isBeforeOrEqual(r1From) && r2To.isAfterOrEqual(r1From)) ||
                (r2From.isBeforeOrEqual(r1To) && r2To.isAfterOrEqual(r1To)) ||
                (r2From.isAfterOrEqual(r1From) && r2To.isBeforeOrEqual(r1To)) ||
                (r1From.isBeforeOrEqual(r2From) && r1To.isAfterOrEqual(r2From)) ||
                (r1From.isBeforeOrEqual(r2To) && r1To.isAfterOrEqual(r2To)) ||
                (r1From.isAfterOrEqual(r2From) && r1To.isBeforeOrEqual(r2To));
    }

    public static boolean isRangeOverlap(DataTypeDate r1FromDate, DataTypeTime r1FromTime,
                                         DataTypeDate r1ToDate, DataTypeTime r1ToTime,
                                         DataTypeDate r2FromDate, DataTypeTime r2FromTime,
                                         DataTypeDate r2ToDate,DataTypeTime r2ToTime) {
        long r1From = r1FromDate.getTimestamp(r1FromTime);
        long r1To   = r1ToDate.getTimestamp(r1ToTime);
        long r2From = r2FromDate.getTimestamp(r2FromTime);
        long r2To   = r2ToDate.getTimestamp(r2ToTime);
        return (r2From <= r1From && r2To >= r1From) ||
                (r2From <= r1To && r2To >= r1To) ||
                (r2From >= r1From && r2To <= r1To) ||
                (r1From <= r2From && r1To >= r2From) ||
                (r1From <= r2To && r1To >= r2To) ||
                (r1From >= r2From && r1To <= r2To);
    }

    public static DataTypeDate[] getRangeOverlap(DataTypeDate r1From, DataTypeDate r1To, DataTypeDate r2From, DataTypeDate r2To) throws Exception {
        DataTypeDate[] range = new DataTypeDate[2];
        if(r1From.isAfter(r1To) || r2From.isAfter(r2To)) {
            throw new Exception("DataTypeDate::getRangeOverlap from is bigger than to date");
        }
        range[0] = (r1From.isAfter(r2From) ? r1From : r2From);
        range[1] = (r1To.isBefore(r2To) ? r1To : r2To);
        return range;
    }

    public long getTimestamp(DataTypeTime time) {
        if (isSet()) {
            Calendar cal = toCalendar();
            if (time != null && time.isSet()) {
                cal.set(Calendar.HOUR_OF_DAY, time.getHour());
                cal.set(Calendar.MINUTE, time.getMinute());
                cal.set(Calendar.SECOND, time.getSecond());
            }
            return cal.getTimeInMillis();
        }
        return 0;
    }

    private static long daysBetween(final Calendar startDate, final Calendar endDate) {
        Calendar sDate = (Calendar) startDate.clone();
        long daysBetween = 0;

        int y1 = sDate.get(Calendar.YEAR);
        int y2 = endDate.get(Calendar.YEAR);
        int m1 = sDate.get(Calendar.MONTH);
        int m2 = endDate.get(Calendar.MONTH);

        //**year optimization**
        while (((y2 - y1) * 12 + (m2 - m1)) > 12) {

            //move to Jan 01
            if (sDate.get(Calendar.MONTH) == Calendar.JANUARY
                    && sDate.get(Calendar.DAY_OF_MONTH) == sDate.getActualMinimum(Calendar.DAY_OF_MONTH)) {

                daysBetween += sDate.getActualMaximum(Calendar.DAY_OF_YEAR);
                sDate.add(Calendar.YEAR, 1);
            } else {
                int diff = 1 + sDate.getActualMaximum(Calendar.DAY_OF_YEAR) - sDate.get(Calendar.DAY_OF_YEAR);
                sDate.add(Calendar.DAY_OF_YEAR, diff);
                daysBetween += diff;
            }
            y1 = sDate.get(Calendar.YEAR);
        }

        //** optimize for month **
        //while the difference is more than a month, add a month to start month
        while ((m2 - m1) % 12 > 1) {
            daysBetween += sDate.getActualMaximum(Calendar.DAY_OF_MONTH);
            sDate.add(Calendar.MONTH, 1);
            m1 = sDate.get(Calendar.MONTH);
        }

        // process remainder date
        while (sDate.before(endDate)) {
            sDate.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }

        return daysBetween;
    }

    public long getDifferenceDays(DataTypeDate o) {
        Calendar c1 = toCalendar();
        Calendar c2 = o.toCalendar();
        if (c1.before(c2)) {
            return daysBetween(c1, c2);
        }
        if (c2.before(c1)) {
            return daysBetween(c2, c1);
        }
        return 0;
    }

    public int getMonthsDifference(DataTypeDate o) {
        int m1 = this.getYear() * 12 + this.getMonth();
        int m2 = o.getYear() * 12 + o.getMonth();
        return Math.abs(m2 - m1) + 1;
    }

    public static int getMonthIntersect(DataTypeDate from, DataTypeDate to, DataTypeDate from2, DataTypeDate to2) {
        if(from2.isAfter(from)) {
            from = from2;
        }

        if(to2.isBefore(to)) {
            to = to2;
        }

        int m1 = from.getYear() * 12 + from.getMonth();
        int m2 = to.getYear() * 12 + to.getMonth();
        int month = m2 - m1 + 1;
        return month > 0 ? month : 0;
    }

    public static String getDateTimeString(DataTypeDate date, DataTypeTime time) {
        String s = null;
        if (date != null && date.isSet()) {
            s = date.toString();
        }
        if (time != null && time.isSet()) {
            s = (s == null ? time.toString() : s + " " + time.toString());
        }
        return s;
    }

    /**
     * formats the date as a string
     * @param format the format, which *must* contain DD, MM and YYYY, e.g. "DD.MM.YYYY" or "YYYY-MM-DD"
     * @return
     */
    public String getDateString(String format) {
        if (format == null) {
            return null;
        }
        int posDay = format.indexOf("DD");
        int posMonth = format.indexOf("MM");
        int posYear = format.indexOf("YYYY");
        if (posDay < 0 || posMonth < 0 || posYear < 0) {
            return null;
        }
        String s = format;
        s = EfaUtil.replace(s, "DD", EfaUtil.int2String(day,2));
        s = EfaUtil.replace(s, "MM", EfaUtil.int2String(month,2));
        s = EfaUtil.replace(s, "YYYY", EfaUtil.int2String(year,4));
        return s;
    }

    public String getMonthAsStringWithIntMarking(String prefix, String postfix) {
        return prefix + EfaUtil.int2String(month, 2) + postfix + getMonthAsString();
    }

    public String getMonthAsString() {
        switch(month) {
            case 1:
                return International.getString("Januar");
            case 2:
                return International.getString("Februar");
            case 3:
                return International.getString("März");
            case 4:
                return International.getString("April");
            case 5:
                return International.getString("Mai");
            case 6:
                return International.getString("Juni");
            case 7:
                return International.getString("Juli");
            case 8:
                return International.getString("August");
            case 9:
                return International.getString("September");
            case 10:
                return International.getString("Oktober");
            case 11:
                return International.getString("November");
            case 12:
                return International.getString("Dezember");
        }
        return EfaTypes.TEXT_UNKNOWN;
    }

    public String getWeekdayAsStringWithIntMarking(String prefix, String postfix) {
        int weekday = toCalendar().get(Calendar.DAY_OF_WEEK);
        if (International.getLanguageID().startsWith("de") &&
                weekday == Calendar.SUNDAY) {
            // for German language setting, put Sunday last
            weekday = Calendar.SATURDAY + 1; // what a hack ;-)
        }
        return prefix + EfaUtil.int2String(weekday, 2) + postfix +
                getWeekdayAsString();
    }

    public String getWeekdayAsString() {
        switch (toCalendar().get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return International.getString("Montag");
            case Calendar.TUESDAY:
                return International.getString("Dienstag");
            case Calendar.WEDNESDAY:
                return International.getString("Mittwoch");
            case Calendar.THURSDAY:
                return International.getString("Donnerstag");
            case Calendar.FRIDAY:
                return International.getString("Freitag");
            case Calendar.SATURDAY:
                return International.getString("Samstag");
            case Calendar.SUNDAY:
                return International.getString("Sonntag");
        }
        return EfaTypes.TEXT_UNKNOWN;
    }
    
    /**
     * Converts the Java Weekday of the current instance to an EfaTypes weekday.
     * 
     * @return The string representing the value of EFA type of the day of the week
     */
    public String getWeekdayAsEfaType() {
        switch (toCalendar().get(Calendar.DAY_OF_WEEK)) {
	        case Calendar.MONDAY:
	            return EfaTypes.TYPE_WEEKDAY_MONDAY;
	        case Calendar.TUESDAY:
	            return EfaTypes.TYPE_WEEKDAY_TUESDAY;
	        case Calendar.WEDNESDAY:
	            return EfaTypes.TYPE_WEEKDAY_WEDNESDAY;
	        case Calendar.THURSDAY:
	            return EfaTypes.TYPE_WEEKDAY_THURSDAY;
	        case Calendar.FRIDAY:
	            return EfaTypes.TYPE_WEEKDAY_FRIDAY;
	        case Calendar.SATURDAY:
	            return EfaTypes.TYPE_WEEKDAY_SATURDAY;
	        case Calendar.SUNDAY:
	            return EfaTypes.TYPE_WEEKDAY_SUNDAY;
	    }
        return EfaTypes.TEXT_UNKNOWN;	
    }

    public static String[] makeDistanceUnitValueArray() {
        String[] units = new String[2];
        units[0] = DAY_MONTH_YEAR;
        units[1] = MONTH_DAY_YEAR;
        return units;
    }

    public static String[] makeDistanceUnitNamesArray() {
        String[] units = new String[2];
        units[0] = DAY_MONTH_YEAR;
        units[1] = MONTH_DAY_YEAR;
        return units;
    }

}
