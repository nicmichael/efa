package de.nmichael.efa.data.efacloud;

import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.ex.EfaException;

import java.security.SecureRandom;
import java.util.HashMap;

/**
 * Manage the efacloud record IDs This provides a capability to create an ecrid and an Index of all ecrids known.
 * The class is full static. There is no need to instantiate it.
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
     * @param storageObject the storage object to use. It will iterate though all data records.
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

}
