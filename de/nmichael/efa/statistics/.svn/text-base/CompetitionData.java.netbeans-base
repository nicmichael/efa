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
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;

public class CompetitionData {

    // Daten für DRV Wanderruderstatistik
    public Hashtable<String,Long> etappen = new Hashtable<String,Long>(); // Etappenname(String) -> Meter(Long)
    public int activeDays = 0;                           // Anzahl der gesamten Rudertage
    public Hashtable<UUID,Long> teilnMueber18 = new Hashtable<UUID,Long>(); // Teilnehmername(String) -> Meter(Long)
    public Hashtable<UUID,Long> teilnMbis18 = new Hashtable<UUID,Long>(); // Teilnehmername(String) -> Meter(Long)
    public Hashtable<UUID,Long> teilnFueber18 = new Hashtable<UUID,Long>(); // Teilnehmername(String) -> Meter(Long)
    public Hashtable<UUID,Long> teilnFbis18 = new Hashtable<UUID,Long>(); // Teilnehmername(String) -> Meter(Long)
    public long totalDistanceInMeters = 0;
    public Hashtable<String,String> waters = new Hashtable<String,String>();                   // befahrene Gewässer

    // Für Brandenburg & Mecklenburg-Vorpommern
    public int gigbootmeters = 0;      // Gigboot-Kilometer
    public int gigbootanz = 0;     // Anzahl der Fahrten im Gigboot
    public int gigboot20plus = 0;  // Anzahl der Fahrten im Gigboot mit mind. 20 Kilometern Länge
    public int gigboot30plus = 0;  // Anzahl der Fahrten im Gigboot mit mind. 30 Kilometern Länge
    public Vector<LogbookRecord> gigfahrten = new Vector<LogbookRecord>(); // Vector of String[6] mit LfdNr, Datum, Boot, Ziel, Km, Bemerkungen
}
