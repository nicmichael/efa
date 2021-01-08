/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Velten Heyn
 * @version 2
 */
package de.nmichael.efa.data.types;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.TMJ;

public class DataTypeHours extends DataTypeTime {

    // Default Constructor
    public DataTypeHours() {
        unset();
    }

    // Regular Constructor
    public DataTypeHours(int hour, int minute) {
    	this.withSeconds = false;
    	this.minute = minute % 60;
        this.hour = hour + minute/60;
    }
    
    public DataTypeHours(int hour, int minute, int second) {
    	this.second = second % 60;
    	this.minute = (minute + second/60) % 60;
        this.hour = hour + (minute + second/60)/60;
    }
    
    public DataTypeHours(int second, boolean withSeconds) {
    	this.withSeconds = withSeconds;
    	this.second = second % 60;
    	this.minute = (second/60) % 60;
        this.hour = (minute + second/60)/60;
    }

    public DataTypeHours(long timestamp) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(timestamp);
        this.hour = cal.get(Calendar.HOUR_OF_DAY);
        this.minute = cal.get(Calendar.MINUTE);
        this.second = cal.get(Calendar.SECOND);
    }

    // Copy Constructor
    public DataTypeHours(DataTypeHours time) {
        this.hour = time.hour;
        this.minute = time.minute;
        this.second = time.second;
    }

    public static DataTypeHours parseTime(String s) {
        DataTypeHours time = new DataTypeHours();
        time.setTime(s);
        return time;
    }

    public static DataTypeHours time000000() {
        DataTypeHours time = new DataTypeHours();
        time.setHour(0);
        time.setMinute(0);
        time.setSecond(0);
        return time;
    }
    
    public boolean isEmpty() {
    	return hour == 0 && minute == 0 && second == 0;
    }
    
    public void setTime(int hour, int minute, int second) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    public void setTime(DataTypeTime time) {
        this.hour = time.hour;
        this.minute = time.minute;
        this.second = time.second;
    }

    public void setTime(String s) {
        TMJ tmj = EfaUtil.string2date(s, -1, -1, -1);
        this.hour = tmj.tag;
        this.minute = tmj.monat;
        this.second = tmj.jahr;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setSecond(int second) {
        this.second = second;
    }
    
    public String toString(boolean withSeconds) {
        if (second < 0 || !withSeconds) {
            return EfaUtil.int2String(hour,2) + ":" + EfaUtil.int2String(minute,2);
        }
        return EfaUtil.int2String(hour,2) + ":" + EfaUtil.int2String(minute,2) + ":" + EfaUtil.int2String(second,2);
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
        } else {
            add(seconds * (-1));
        }
    }
}
