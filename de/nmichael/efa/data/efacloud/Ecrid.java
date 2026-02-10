package de.nmichael.efa.data.efacloud;

import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.Logger;

import java.security.SecureRandom;
import java.util.HashMap;

/**
 * Manage the efacloud record IDs This provides a capability to create an ecrid and an Index of all ecrids known.
 * The class is full static. There is no need to instantiate it.
 * 
 * The ecrid index contains unique ecrids assigned to an actual efa record.
 * So, all records in an efacloud project can easily be accessed by using the ecrid index instead of
 * iterating through a datastorage file.
 * 
 * The ecrid index is a singleton and contains all ecrids of all currently open projects and datastorages.
 * The ecrid index is updated during efacloud communication (insert/update/delete).
 * When closing a datastorage, its records are removed from the ecrid index.
 * 
 */
public class Ecrid {

    public static final String BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final HashMap<String, DataRecord> iEcrids = new HashMap<>();
    public static final String ECRID_FIELDNAME = "ecrid";

    /**
     * generate a random ecrid, i. e. a sequence of 12 random chars out of the BASE62_CHARS secquence.
     * @return generated String of 12 characters length
     */
    public static String generate() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[9];
        String ecrid;
        do {
            random.nextBytes(bytes);
            int repSlashInt = Math.abs(random.nextInt()) % 62;
            int repPlusInt = Math.abs(random.nextInt()) % 62;
            char repSlash = BASE62_CHARS.charAt(repSlashInt);
            char repPlus = BASE62_CHARS.charAt(repPlusInt);
            ecrid = Base64.encodeBytes(bytes).replace('/', repSlash).replace('+', repPlus);
        } while (iEcrids.get(ecrid) != null);
        return ecrid;
    }

    /**
     * Add all ecrids found to the iEcrids index.
     * @param storageObject the storage object to use. It will iterate through all data records.
     */
    public static void addAll(StorageObject storageObject) {
        try {
            DataKeyIterator dataKeyIterator = storageObject.data().getStaticIterator();
            DataKey dataKey = dataKeyIterator.getFirst();
            while (dataKey != null) {
                DataRecord dataRecord = storageObject.data().get(dataKey);
                if (dataRecord.isField(ECRID_FIELDNAME)) {
                    String ecrid = dataRecord.getAsText(ECRID_FIELDNAME);
                    if ((ecrid != null) && (ecrid.length() == 12))
                        iEcrids.put(ecrid, dataRecord);
                    dataKey = dataKeyIterator.getNext();
                }
            }
        } catch (EfaException e) {
            // do nothing
        }
    }
    
    /**
     * Removes all ecrids belonging to records of the respective storage object by iterating through all data records.
     * @param storageObject
     */
    public static void removeAll(StorageObject storageObject) {
        try {
        	long i=0;
        	long datakeyCount=0;
            DataKeyIterator dataKeyIterator = storageObject.data().getStaticIterator();
            DataKey dataKey = dataKeyIterator.getFirst();
            while (dataKey != null) {
            	i++;
            	DataRecord dataRecord = storageObject.data().get(dataKey);
                if (dataRecord.isField(ECRID_FIELDNAME)) {
                    String ecrid = dataRecord.getAsText(ECRID_FIELDNAME);
                    if ((ecrid != null) && (ecrid.length() == 12)) {
                        iEcrids.remove(ecrid);
                        datakeyCount++;
                    }
                    dataKey = dataKeyIterator.getNext();
                }
            }
            if (Logger.isTraceOn(Logger.TT_CLOUD, 1)) {
            	Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFACLOUD, "Removed ecrids from: "+storageObject.getName()+" Removed ecrids: "+ datakeyCount+" / Records in Storageobject :"+i);
            }
            
        } catch (EfaException e) {
            // do nothing
        	Logger.logdebug(e);
        }    	
    }

}
