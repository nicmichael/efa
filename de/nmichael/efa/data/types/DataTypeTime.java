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

import java.util.*;
import de.nmichael.efa.util.*;

public class DataTypeTime implements Cloneable, Comparable<DataTypeTime> {

    protected int hour, minute, second;
    protected boolean withSeconds = true;

    // Default Constructor
    public DataTypeTime() {
        unset();
    }

    // Regular Constructor
    public DataTypeTime(int hour, int minute, int second) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        ensureCorrectTime();
    }

    public DataTypeTime(long timestamp) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timestamp);
        this.hour = cal.get(Calendar.HOUR_OF_DAY);
        this.minute = cal.get(Calendar.MINUTE);
        this.second = cal.get(Calendar.SECOND);
        ensureCorrectTime();
    }

    // Copy Constructor
    public DataTypeTime(DataTypeTime time) {
        this.hour = time.hour;
        this.minute = time.minute;
        this.second = time.second;
        ensureCorrectTime();
    }

    public static DataTypeTime parseTime(String s) {
        DataTypeTime time = new DataTypeTime();
        time.setTime(s);
        return time;
    }

    public static DataTypeTime now() {
        DataTypeTime time = new DataTypeTime();
        Calendar cal = new GregorianCalendar();
        time.setHour(cal.get(Calendar.HOUR_OF_DAY));
        time.setMinute(cal.get(Calendar.MINUTE));
        time.setSecond(cal.get(Calendar.SECOND));
        return time;
    }

    public static DataTypeTime time000000() {
        DataTypeTime time = new DataTypeTime();
        time.setHour(0);
        time.setMinute(0);
        time.setSecond(0);
        return time;
    }

    public static DataTypeTime time235959() {
        DataTypeTime time = new DataTypeTime();
        time.setHour(23);
        time.setMinute(59);
        time.setSecond(59);
        return time;
    }

    public void enableSeconds(boolean withSeconds) {
        this.withSeconds = withSeconds;
    }

    public void setTime(int hour, int minute, int second) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        ensureCorrectTime();
    }

    public void setTime(DataTypeTime time) {
        this.hour = time.hour;
        this.minute = time.minute;
        this.second = time.second;
        ensureCorrectTime();
    }

    public void setTime(String s) {
        TMJ tmj = EfaUtil.string2date(s, -1, -1, -1);
        this.hour = tmj.tag;
        this.minute = tmj.monat;
        this.second = tmj.jahr;
        ensureCorrectTime();
    }

    public void ensureCorrectTime() {
        if (!isSet()) {
            return;
        }
        if (hour < 0) {
            hour = 0;
        }
        if (minute < 0) {
            minute = 0;
        }
        if (second < -1) { // -1 = unset!
            second = 0;
        }
        if (hour > 23) {
            hour = 23;
        }
        if (minute > 59) {
            minute = 59;
        }
        if (second > 59) {
            second = 59;
        }
        if (!withSeconds) {
            second = -1;
        }
    }

    public String toString(boolean withSeconds) {
        if (hour < 0 || minute < 0) {
            return "";
        }
        if (second < 0 || !withSeconds) {
            return EfaUtil.int2String(hour,2) + ":" + EfaUtil.int2String(minute,2);
        }
        return EfaUtil.int2String(hour,2) + ":" + EfaUtil.int2String(minute,2) + ":" + EfaUtil.int2String(second,2);
    }

    public String toString() {
        return toString(withSeconds);
    }

    public boolean isSet() {
        return hour != -1 && minute != -1;
    }

    public int getHour() {
        return (hour < 0 ? 0 : hour);
    }

    public int getMinute() {
        return (minute < 0 ? 0 : minute);
    }

    public int getSecond() {
        return (second < 0 ? 0 : second);
    }

    public void setHour(int hour) {
        this.hour = hour;
        ensureCorrectTime();
    }

    public void setMinute(int minute) {
        this.minute = minute;
        ensureCorrectTime();
    }

    public void setSecond(int second) {
        this.second = second;
        ensureCorrectTime();
    }

    public void unset() {
        hour = -1;
        minute = -1;
        second = -1;
    }

    public boolean equals(Object o) {
        try {
            return compareTo((DataTypeTime)o) == 0;
        } catch(Exception e) {
            return false;
        }
    }

    public int compareTo(DataTypeTime o) {
        if (hour < o.hour) {
            return -1;
        }
        if (hour > o.hour) {
            return 1;
        }
        if (minute < o.minute) {
            return -1;
        }
        if (minute > o.minute) {
            return 1;
        }
        if (second < o.second) {
            return -1;
        }
        if (second > o.second) {
            return 1;
        }
        return 0;
    }

    public int hashCode() {
        return (new Integer(hour*10000 + minute*100 + second)).hashCode();
    }

    public boolean isBefore(DataTypeTime o) {
        return compareTo(o) < 0;
    }

    public boolean isAfter(DataTypeTime o) {
        return compareTo(o) > 0;
    }

    public boolean isBeforeOrEqual(DataTypeTime o) {
        return compareTo(o) <= 0;
    }

    public boolean isAfterOrEqual(DataTypeTime o) {
        return compareTo(o) >= 0;
    }

    public boolean isInRange(DataTypeTime from, DataTypeTime to) {
        return (compareTo(from) >= 0) && (compareTo(to) <= 0);
    }

    public static boolean isRangeOverlap(DataTypeTime r1From, DataTypeTime r1To, DataTypeTime r2From, DataTypeTime r2To) {
        return r1From.isBeforeOrEqual(r2To) && r1To.isAfterOrEqual(r2From);
/*        return (r2From.isBefore(r1From) && r2To.isAfter(r1From)) ||
               (r2From.isBefore(r1To) && r2To.isAfter(r1To)) ||
               (r2From.isAfter(r1From) && r2To.isBefore(r1To)) ||
               (r1From.isBefore(r2From) && r1To.isAfter(r2From)) ||
               (r1From.isBefore(r2To) && r1To.isAfter(r2To)) ||
               (r1From.isAfter(r2From) && r1To.isBefore(r2To));
*/        
    }

    public void add(int seconds) {
        if (seconds >= 0) {
            if (second >= 0) {
                second += seconds;
            } else {
                minute += seconds/60;
            }
            while(second >= 60) {
                second -= 60;
                minute++;
            }
            while(minute >= 60) {
                minute -= 60;
                hour++;
            }
            hour = hour % 24;
        } else {
            delete(seconds * (-1));
        }
    }

    public void delete(int seconds) {
        if (seconds >= 0) {
            if (second >= 0) {
                second -= seconds;
            } else {
                minute -= seconds/60;
            }
            while(second < 0) {
                second += 60;
                minute--;
            }
            while(minute < 0) {
                minute += 60;
                hour--;
            }
            hour = hour % 24;
        } else {
            add(seconds * (-1));
        }
    }

    public int getTimeAsSeconds() {
        if (isSet()) {
            return hour*3600 + minute*60 + (second >= 0 ? second : 0);
        } else {
            return -1;
        }
    }

}
