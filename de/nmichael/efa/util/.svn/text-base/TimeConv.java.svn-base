/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.util;

import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class TimeConv {

    public static void main(String args[]) {
        if (args.length == 0) {
            System.err.println("usage: TimeConv <unixtimestamp>");
            System.err.println("usage: TimeConv <date> <time>");
        }

        try {
            long ts = Long.parseLong(args[0]);
            Calendar cal = new GregorianCalendar();
            cal.setTimeInMillis(ts);
            System.out.println(ts + " = " + EfaUtil.date2String(cal.getTime(), true));
        } catch(Exception e) {
        }

        try {
            DataTypeDate d = DataTypeDate.parseDate(args[0]);
            DataTypeTime t = DataTypeTime.parseTime(args[1]);
            System.out.println(DataTypeDate.getDateTimeString(d, t) + " = " +
                    d.getTimestamp(t));
        } catch(Exception e) {
        }
    }

}
