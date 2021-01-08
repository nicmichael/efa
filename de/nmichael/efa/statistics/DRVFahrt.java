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

// @i18n complete

import de.nmichael.efa.data.types.DataTypeDate;

// Daten für DRV-Fahrten
public class DRVFahrt {

    public String entryNo, destination, comments;
    DataTypeDate dateStart, dateEnd;
    public long days = 1;
    public long distanceInMeters = 0;
    public boolean ok = false; // gültige Wanderfahrt, oder nicht (MTour < 40 Km)
    public boolean jum = false; // ob JuM-Regatta

    public DRVFahrt() {
    }

    public DRVFahrt(String entryNo, DataTypeDate dateStart, DataTypeDate dateEnd, String destination,
            String comments, long distanceInMeters) {
        this.entryNo = entryNo;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.destination = destination;
        this.comments = (comments != null ? comments : "");
        this.distanceInMeters = distanceInMeters;
        this.days = CompetitionDRVFahrtenabzeichen.getNumberOfDays(dateStart, dateEnd);
    }

}
