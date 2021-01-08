/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.importefa1;

import de.nmichael.efa.efa1.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.types.*;

public class ImportMetadata {

    public static final int TYPE_ADRESSEN = 1;
    public static final int TYPE_SYNONYME_BOOTE = 2;
    public static final int TYPE_SYNONYME_MITGLIEDER = 3;
    public static final int TYPE_SYNONYME_ZIELE = 4;
    public static final int TYPE_BOOTSTATUS = 5;
    public static final int TYPE_FAHRTENABZEICHEN = 6;
    public static final int TYPE_GRUPPEN = 7;
    public static final int TYPE_MANNSCHAFTEN = 8;
    public static final int TYPE_NACHRICHTEN = 9;
    public static final int TYPE_FAHRTENBUCH = 10;
    public static final int TYPE_KEYSTORE = 11;
    
    public int type;
    public String typedescription;
    public boolean selected = true;
    public String name = null;
    public String filename = null;
    public String description = null;
    public DatenListe datenListe;
    public int numRecords = -1;
    // for Logbooks
    public DataTypeDate firstDate = null;
    public DataTypeDate lastDate = null;
    public int numRecBoats = -1;
    public int numRecMembers = -1;
    public int numRecDests = -1;
    public int numRecStats = -1;

    public ImportMetadata(int type, DatenListe datenListe, String description) {
        this.type = type;
        this.datenListe = datenListe;
        this.typedescription = description;
        this.filename = datenListe.getFileName();
    }

    public String toString(boolean longtext) {
        if (numRecords < 0) {
            return International.getMessage("{datalist} nicht gefunden", typedescription);
        }
        String s = International.getMessage("{datalist} mit {number} Einträgen", typedescription, numRecords);
        if (type == TYPE_FAHRTENBUCH) {
            if (firstDate != null && lastDate != null) {
                s += " " + International.getMessage("vom {day_from} bis {day_to}", firstDate.toString(), lastDate.toString());
            }
            if (longtext) {
                if (numRecBoats >= 0) {
                    s += "\n" + International.getMessage("{datalist} mit {number} Einträgen",
                            International.getString("Bootsliste"), numRecBoats);
                } else {
                    s += "\n" + International.getMessage("{datalist} nicht gefunden",
                            International.getString("Bootsliste"));
                }
                if (numRecMembers >= 0) {
                    s += "\n" + International.getMessage("{datalist} mit {number} Einträgen",
                            International.getString("Mitgliederliste"), numRecMembers);
                } else {
                    s += "\n" + International.getMessage("{datalist} nicht gefunden",
                            International.getString("Mitgliederliste"));
                }
                if (numRecDests >= 0) {
                    s += "\n" + International.getMessage("{datalist} mit {number} Einträgen",
                            International.getString("Zielliste"), numRecDests);
                } else {
                    s += "\n" + International.getMessage("{datalist} nicht gefunden",
                            International.getString("Zielliste"));
                }
                if (numRecStats >= 0) {
                    s += "\n" + International.getMessage("{datalist} mit {number} Einträgen",
                            International.getString("Statistikeinstellungen"), numRecStats);
                } else {
                    s += "\n" + International.getMessage("{datalist} nicht gefunden",
                            International.getString("Statistikeinstellungen"));
                }
            }
        }
        return s;
    }

    public String toString() {
        return toString(true);
    }

    public String getShortDescription() {
        return name + " (" + description + ")";
    }
}
