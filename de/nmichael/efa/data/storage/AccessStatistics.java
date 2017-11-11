/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.storage;

import de.nmichael.efa.util.EfaUtil;
import java.util.Arrays;
import java.util.HashMap;

public class AccessStatistics {

    public static final String COUNTER_REQSENT   = "req-sent";
    public static final String COUNTER_REQRCVD   = "req-rcvd";
    public static final String COUNTER_BYTESSENT = "bytes-sent";
    public static final String COUNTER_BYTESRCVD = "bytes-rcvd";
    public static final String COUNTER_RECSRCVD  = "recs-rcvd";
    public static final String COUNTER_KEYSRCVD  = "keys-rcvd";
    public static final String COUNTER_TIME      = "time";
    public static final String COUNTER_TIMEOUT   = "timeout";
    public static final String COUNTER_RESPOK    = "resp-ok";
    public static final String COUNTER_RESPERR   = "resp-err";

    private static HashMap<String,Long> statistics = new HashMap<String,Long>();

    public static void updateStatistics(DataAccess access, String operation, String counter,
            long value) {
        String key = access.getStorageObjectName() + "." + access.getStorageObjectType() + ":" +
                operation + ":" + counter;
        synchronized(statistics) {
            Long v = statistics.get(key);
            if (v == null) {
                v = new Long(0);
            }
            v = v + value;
            statistics.put(key, v);
        }
    }

    public static String getStatisticsAsString() {
        String[] keys;
        synchronized(statistics) {
            keys = statistics.keySet().toArray(new String[0]);
        }
        Arrays.sort(keys);
        StringBuffer s = new StringBuffer();
        synchronized(statistics) {
            for (String key : keys) {
                long value = statistics.get(key);
                s.append(EfaUtil.getString(key, 60) + " = " + 
                        EfaUtil.getStringPadLeft(Long.toString(value), 10) + "\n");
            }
        }
        return s.toString();
    }

    public static String[][] getStatisticsAsArray() {
        String[] keys;
        synchronized(statistics) {
            keys = statistics.keySet().toArray(new String[0]);
        }
        Arrays.sort(keys);
        String[][] data = new String[keys.length][];
        synchronized(statistics) {
            for (int i=0; i<keys.length; i++) {
                String key = keys[i];
                long value = statistics.get(key);
                String[] keyparts = key.split(":");
                String[] fields = new String[keyparts.length + 1];
                for (int j=0; j<keyparts.length; j++) {
                    fields[j] = keyparts[j];
                }
                fields[fields.length - 1] = Long.toString(value);
                data[i] = fields;
            }
        }
        return data;
    }

    public static String[] getStatisticsAsSimpleArray() {
        String[] keys;
        synchronized(statistics) {
            keys = statistics.keySet().toArray(new String[0]);
        }
        Arrays.sort(keys);
        String[] data = new String[keys.length];
        synchronized(statistics) {
            for (int i=0; i<keys.length; i++) {
                String key = keys[i];
                long value = statistics.get(key);
                data[i] = (EfaUtil.getString(key, 60) + " = " + 
                        EfaUtil.getStringPadLeft(Long.toString(value), 10));
            }
        }
        return data;
    }

    public static String getKey(String[] values) {
        StringBuffer s = new StringBuffer();
        for (int i=0; i<values.length-1; i++) {
            s.append( (i > 0 ? ":" : "") + values[i]);
        }
        return s.toString();
    }

}
