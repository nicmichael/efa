/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.ex.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

// @i18n complete

public class Waters extends StorageObject {

    public static final String DATATYPE = "efa2waters";

    public Waters(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Gewässer"));
        WatersRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public DataRecord createNewRecord() {
        return new WatersRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public WatersRecord createWatersRecord(UUID id) {
        WatersRecord r = new WatersRecord(this, MetaData.getMetaData(DATATYPE));
        r.setId(id);
        return r;
    }

    public WatersRecord getWaters(UUID id) {
        try {
            return (WatersRecord)data().get(WatersRecord.getKey(id));
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public WatersRecord findWatersByName(String name) {
        try {
            DataKey[] keys = data().getByFields(new String[] { WatersRecord.NAME }, new String[] { name });
            if (keys != null && keys.length > 0) {
                return (WatersRecord) data().get(keys[0]);
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public boolean addWatersRecord(String name, String details) {
        try {
            WatersRecord r = createWatersRecord(UUID.randomUUID());
            r.setName(name);
            r.setDetails(details);
            dataAccess.add(r);
            return true;
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }

    public static URL getResourceTemplate(String countryCode) {
        try {
            return Waters.class.getResource(Daten.DATATEMPLATEPATH + "Waters_" + countryCode + ".txt");
        } catch(Exception e) {
            return null;
        }
    }

    public int addAllWatersFromTemplate(String countryCode) {
        int count = 0;
        try {
            BufferedReader f = new BufferedReader(
                    new InputStreamReader(
                        getResourceTemplate(countryCode).openStream(),
                        Daten.ENCODING_UTF));
            String s;
            String water = null;
            StringBuilder details = null;
            while ( (s = f.readLine()) != null) {
                s = s.trim();
                if (s.length() == 0 || s.startsWith("#")) {
                    continue;
                }
                int pos = s.indexOf(";");
                if (pos < 0) {
                    continue;
                }
                if (pos > 0) {
                    if (water != null && details != null) {
                        if (addWatersRecord(water, details.toString())) {
                            count++;
                        }
                    }
                    water = s.substring(0, pos);
                    details = new StringBuilder();
                }
                String detail = s.substring(pos + 1);
                if (details != null) {
                    details.append( (details.length() > 0 ? "; " : "") + detail);
                }
            }
            if (water != null && details != null) {
                if (addWatersRecord(water, details.toString())) {
                    count++;
                }
            }
            return count;
        } catch(Exception e) {
            Logger.logdebug(e);
            return -1;

        }
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        if (add || update) {
            assertFieldNotEmpty(record, WatersRecord.ID);
            assertFieldNotEmpty(record, WatersRecord.NAME);
            assertUnique(record, WatersRecord.NAME);
        }
        if (delete) {
            assertNotReferenced(record, getProject().getDestinations(false), new String[] { DestinationRecord.WATERSIDLIST });
        }
    }

}
