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
import de.nmichael.efa.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;

// @i18n complete

public class Waters extends StorageObject {

    public static final String DATATYPE = "efa2waters";
    private static final String HASH_DELIMITER = "|"; // shall not be # as it conflicts with version field in Daten.java

    public Waters(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword,
            String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE, International.getString("Gewässer"));
        WatersRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }
    
    private Hashtable<UUID,Boolean> changedWatersRecords;

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
    
    private boolean updateWatersRecord(String waterNew, String waterOld, String detailsNew, String detailsOld) {
        try {
            WatersRecord r = null;
            boolean addedWater = false;
            boolean changedWater = false;
            boolean changedDetails = false;
            
            if (waterOld != null) {
                r = findWatersByName(waterOld);
                if (r != null) {
                    r.setName(waterNew);
                    changedWater = true;
                }
            } 
            if (r == null) {
                r = findWatersByName(waterNew);
            }
            if (r == null) {
                r = createWatersRecord(UUID.randomUUID());
                r.setName(waterNew);
                addedWater = true;
            }

            String details = r.getDetails();
            Hashtable<String,String> detailsH = new Hashtable<String,String>();
            if (details != null) {
                for (String d : details.split(";")) {
                    detailsH.put(d.trim(), d.trim());
                }
            }
            
            if (detailsOld != null && detailsH.get(detailsOld) != null) {
                detailsH.remove(detailsOld);
                if (detailsNew != null) {
                    detailsH.put(detailsNew, detailsNew);
                }
                changedDetails = true;
            }
            
            if (detailsNew != null && detailsH.get(detailsNew) == null) {
                detailsH.put(detailsNew, detailsNew);
                changedDetails = true;
            }
            
            if (changedDetails) {
                StringBuilder s = new StringBuilder();
                String[] detailsA = detailsH.keySet().toArray(new String[0]);
                Arrays.sort(detailsA);
                for (String d : detailsA) {
                    s.append(s.length() > 0 ? "; " +d : d);
                }
                r.setDetails(s.toString());
            }
            
            if (addedWater) {
                dataAccess.add(r);
                changedWatersRecords.put(r.getId(), Boolean.TRUE);
            } else if (changedWater || changedDetails) {
                dataAccess.update(r);
                changedWatersRecords.put(r.getId(), Boolean.TRUE);
            }
            return true;
        } catch(Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }
    
    public static String getWatersTemplateHash(String countryCode) {
        try {
            DigestInputStream f = new DigestInputStream(getResourceTemplate(countryCode).openStream(), 
                    MessageDigest.getInstance("MD5"));
            byte[] chunk = new byte[4096];
            while (f.read(chunk) > 0);
            f.close();
            return Base64.encodeBytes(f.getMessageDigest().digest());
        } catch(Exception e) {
            Logger.logdebug(e);
            return "";
        }
    }
    
    public static String getWaterTemplateAuthor(String countryCode) {
        try {
            BufferedReader f = new BufferedReader(
                    new InputStreamReader(
                        getResourceTemplate(countryCode).openStream(),
                        Daten.ENCODING_UTF));
            String s = f.readLine();
            if (s.trim().startsWith("# AUTHOR=")) {
                return s.trim().substring(9);
            }
            return null;
        } catch(Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }
    
    public static boolean hasWaterTemplateChanged(String countryCode) {
        String oldHash = Daten.project != null ? Daten.project.getLastWatersTemplateHash() : null;
        if (oldHash == null || oldHash.length() == 0) {
            return true; // never checked
        }
        String oldv = oldHash.indexOf(HASH_DELIMITER) > 0 ? oldHash.substring(0, oldHash.indexOf(HASH_DELIMITER)) : oldHash;
        if (Daten.VERSIONID.compareTo(oldv) <= 0) {
            return false; // already checked for this version, don't check again
        }
        String newh = getWatersTemplateHash(countryCode);
        String oldh = oldHash.indexOf(HASH_DELIMITER) > 0 ? oldHash.substring(oldHash.indexOf(HASH_DELIMITER)+1) : oldHash;
        return !oldh.equals(newh);
    }
    
    public static void setWaterTemplateUnchanged(String countryCode) {
        Daten.project.setLastWatersTemplateHash(Daten.VERSIONID + HASH_DELIMITER + getWatersTemplateHash(countryCode));
    }

    public synchronized int addAllWatersFromTemplate(String countryCode) {
        try {
            changedWatersRecords = new Hashtable<UUID,Boolean>();
            BufferedReader f = new BufferedReader(
                    new InputStreamReader(
                        getResourceTemplate(countryCode).openStream(),
                        Daten.ENCODING_UTF));
            String s;
            String water = null;
            while ( (s = f.readLine()) != null) {
                s = s.trim();
                if (s.length() == 0 || s.startsWith("#")) { //# in this code is used for delimiting multiple items for a water.
                    continue;
                }
                
                try {
                    String[] newold = s.split("#");
                    String[] wNew = newold[0].split(";");
                    String[] wOld = newold.length > 1 ? newold[1].split(";") : null;
                    String waterNew = wNew[0];
                    String detailsNew = wNew.length > 1 ? wNew[1] : null;
                    String waterOld = wOld != null ? wOld[0] : null;
                    String detailsOld = wOld != null && wOld.length > 1 ? wOld[1] : null;
                    waterNew = waterNew != null && waterNew.trim().length() > 0 ? waterNew.trim() : null;
                    waterOld = waterOld != null && waterOld.trim().length() > 0 ? waterOld.trim() : null;
                    detailsNew = detailsNew != null && detailsNew.trim().length() > 0 ? detailsNew.trim() : null;
                    detailsOld = detailsOld != null && detailsOld.trim().length() > 0 ? detailsOld.trim() : null;
                    waterOld = waterOld != null && waterOld.equals(waterNew) ? null : waterOld;
                    detailsOld = detailsOld != null && detailsOld.equals(detailsNew) ? null : detailsOld;
                    water = waterNew != null ? waterNew : water;
                    if (water != null) {
                        updateWatersRecord(water, waterOld, detailsNew, detailsOld);
                    }
                } catch(Exception e) {
                    Logger.log(Logger.WARNING, Logger.MSG_WARN_WATERSTEMPLATE, 
                            "Failed to parse waters template line (" + e + "): " + s);
                }
            }
            setWaterTemplateUnchanged(countryCode);
            return changedWatersRecords.size();
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
