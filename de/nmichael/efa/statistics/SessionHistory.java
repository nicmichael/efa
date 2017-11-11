/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.statistics;

import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.util.International;
import de.nmichael.efa.data.efawett.Zielfahrt;
import java.util.*;

public class SessionHistory {

    private Vector<LogbookRecord> sessions = new Vector<LogbookRecord>();

    public void SessionHistory() {
    }

    public void addSession(LogbookRecord r) {
        sessions.add((LogbookRecord)r.cloneRecord());
    }

    public void addSession(LogbookRecord r, Zielfahrt zf) {
        addSession(r, 0, null, null, zf);
    }

    public void addSession(LogbookRecord r,
            int dayNumber, DataTypeDate date, DataTypeDistance distance,
            Zielfahrt zf) {
        LogbookRecord r2 = (LogbookRecord)r.cloneRecord();
        date = (date != null ? new DataTypeDate(date) : null);
        distance = (distance != null ? new DataTypeDistance(distance) : null);
        if (date != null) {
            r2.setDate(date);
        }
        r2.setEndDate(null);
        if (distance != null) {
            r2.setDistance(distance);
        }
        if (dayNumber != 0) {
            String comments = r.getComments();
            r2.setComments( (comments != null && comments.length() > 0 ? comments + " " : "") +
                    "(" + International.getMessage("Tag {n}", dayNumber));
        }
        if (r2.getDistance().getValueInMeters() >= 20000) {
            r2.zielfahrt = zf;
        } else {
            r2.zielfahrt = null;
        }
        sessions.add(r2);
    }

    public int size() {
        return sessions.size();
    }

    public LogbookRecord get(int idx) {
        return sessions.get(idx);
    }

}
