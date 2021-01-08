/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core;

import de.nmichael.efa.util.International;
import java.util.Hashtable;
import java.util.Vector;

public class OnlineUpdateInfo {
    public String versionId;
    public String releaseDate;
    public String downloadUrl;
    public long downloadSize;
    // lang_id -> changes
    public Hashtable<String,Vector<String>> changeItems = new Hashtable<String,Vector<String>>();

    public Vector<String> getChanges() {
        Vector<String> changes = changeItems.get(International.getLanguageID());
        if (changes == null) {
            changes = changeItems.get("en");
        }
        if (changes == null) {
            changes = changeItems.get("de");
        }
        return (changes != null ? changes : new Vector<String>());
    }

}
