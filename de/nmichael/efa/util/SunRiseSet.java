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

import java.util.*;
import de.nmichael.efa.core.items.ItemTypeLongLat;
import de.nmichael.efa.Daten;

// @i18n complete
public class SunRiseSet {

    // Constructor usually not needed (everything static), except for Plungin-Test in Daten.java
    public SunRiseSet() {
        uk.me.jstott.coordconv.LatitudeLongitude ll =
                new uk.me.jstott.coordconv.LatitudeLongitude(uk.me.jstott.coordconv.LatitudeLongitude.NORTH, 0, 0, 0,
                uk.me.jstott.coordconv.LatitudeLongitude.EAST, 0, 0, 0); // just dummy statement
    }

    public static boolean sunrisePluginInstalled() {
        try {
            uk.me.jstott.coordconv.LatitudeLongitude ll =
                    new uk.me.jstott.coordconv.LatitudeLongitude(uk.me.jstott.coordconv.LatitudeLongitude.NORTH, 0, 0, 0,
                    uk.me.jstott.coordconv.LatitudeLongitude.EAST, 0, 0, 0);
            return true;
        } catch (NoClassDefFoundError e1) {
            return false;
        }
    }

    public static String[] getSunRiseSet() throws Exception {
        return getSunRiseSet(null, null, Calendar.getInstance());
    }

    public static String[] getSunRiseSet(ItemTypeLongLat latitude, ItemTypeLongLat longitude) throws Exception {
        return getSunRiseSet(latitude, longitude, Calendar.getInstance());
    }

    public static String[] getSunRiseSet(Calendar cal) throws Exception {
        return getSunRiseSet(null, null, cal);
    }

    public static String[] getSunRiseSet(ItemTypeLongLat latitude, ItemTypeLongLat longitude, Calendar cal) throws Exception {
        int lat = uk.me.jstott.coordconv.LatitudeLongitude.NORTH;
        int lon = uk.me.jstott.coordconv.LatitudeLongitude.EAST;
        switch (latitude.getValueOrientation()) {
            case ItemTypeLongLat.ORIENTATION_NORTH:
                lat = uk.me.jstott.coordconv.LatitudeLongitude.NORTH;
                break;
            case ItemTypeLongLat.ORIENTATION_SOUTH:
                lat = uk.me.jstott.coordconv.LatitudeLongitude.SOUTH;
                break;
        }
        switch (longitude.getValueOrientation()) {
            case ItemTypeLongLat.ORIENTATION_WEST:
                lon = uk.me.jstott.coordconv.LatitudeLongitude.WEST;
                break;
            case ItemTypeLongLat.ORIENTATION_EAST:
                lon = uk.me.jstott.coordconv.LatitudeLongitude.EAST;
                break;
        }

        uk.me.jstott.coordconv.LatitudeLongitude ll =
                new uk.me.jstott.coordconv.LatitudeLongitude(lat,
                latitude.getValueCoordinates()[0],
                latitude.getValueCoordinates()[1],
                latitude.getValueCoordinates()[2],
                lon,
                longitude.getValueCoordinates()[0],
                longitude.getValueCoordinates()[1],
                longitude.getValueCoordinates()[2]);

        TimeZone gmt = TimeZone.getDefault();
        double julian = uk.me.jstott.util.JulianDateConverter.dateToJulian(cal);

        boolean dst = gmt.inDaylightTime(new Date(cal.getTimeInMillis())); // gmt.getDSTSavings() > 0;

        uk.me.jstott.sun.Time rise = uk.me.jstott.sun.Sun.sunriseTime(julian, ll, gmt, dst);
        uk.me.jstott.sun.Time set = uk.me.jstott.sun.Sun.sunsetTime(julian, ll, gmt, dst);
        String[] riseset = new String[2];
        riseset[0] = EfaUtil.leadingZeroString(rise.getHours(), 2) + ":"
                + EfaUtil.leadingZeroString(rise.getMinutes(), 2);
        riseset[1] = EfaUtil.leadingZeroString(set.getHours(), 2) + ":"
                + EfaUtil.leadingZeroString(set.getMinutes(), 2);
        return riseset;
    }

    public static void printAllDays(int year) {
        Calendar cal = Calendar.getInstance();
        System.out.println("efa - Sunrise and Sunset for " + "\n=============================================================\n");
        System.out.println("Day          Sunrise    Sunset\n------------------------------");
        for (int month = 1; month <= 12; month++) {
            for (int day = 1; day <= 31; day++) {
                if ((day > 30 && (month == 4 || month == 6 || month == 9 || month == 11)) || (day > 29 && month == 2) || (day > 28 && month == 2 && year % 4 != 0)) {
                    continue;
                }
                cal.set(year, month - 1, day);
                try {
                    String[] riseset = getSunRiseSet(cal);
                    if (riseset != null) {
                        System.out.println(EfaUtil.leadingZeroString(day, 2) + "."
                                + EfaUtil.leadingZeroString(month, 2) + "." + year
                                + "     " + riseset[0] + "     " + riseset[1]);
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    public static void main(String[] args) {
        Daten.iniBase(-1);
        Daten.initialize();
        printAllDays(2009);
    }
}
