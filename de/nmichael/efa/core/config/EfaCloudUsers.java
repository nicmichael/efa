/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/ License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.config;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.efacloud.CsvCodec;
import de.nmichael.efa.data.efacloud.InternetAccessManager;
import de.nmichael.efa.data.efacloud.Transaction;
import de.nmichael.efa.data.efacloud.TxRequestQueue;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.International;

import java.util.ArrayList;

// @i18n complete

/**
 * Builds a shadow copy of the server users in order to be able to manage admin privileges at the server side.
 */
public class EfaCloudUsers extends StorageObject {

    public static final String DATATYPE = "efaCloudUsers";

    /**
     * Standard full constructor as used by all StorageObjects
     */
    public EfaCloudUsers(int storageType, String storageLocation, String storageUsername, String storagePassword,
                         String storageObjectName) {
        super(storageType, storageLocation, storageUsername, storagePassword, storageObjectName, DATATYPE,
                International.getString("efaCloud-Nutzer"));
        EfaCloudUserRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    /**
     * Standard empty constructor as used by all StorageObjects
     */
    public EfaCloudUsers() {
        super(IDataAccess.TYPE_FILE_XML, Daten.efaCfgDirectory, null, null, "efaCloudUsers", DATATYPE,
                International.getString("Administratoren"));
        EfaCloudUserRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public void open(boolean createNewIfNotExists) throws EfaException {
        super.open(createNewIfNotExists);
    }

    public DataRecord createNewRecord() {
        return new EfaCloudUserRecord(this, MetaData.getMetaData(DATATYPE));
    }

    /**
     * Verification of credentials using the VERIFY transaction to the server. There is no offline verification and the
     * password of server users is never stored, even not the hash. Note that this methods will timeout after 30 seconds
     * with no response from the server
     * @param name the admin name of the user
     * @param password the password of the user as it was entered by him/her.
     * @return the Efacloud user record of the verified user and null, if the verification fails or a timeout occurred.
     */
    private EfaCloudUserRecord verifiy_credentials(String name, String password) {
        TxRequestQueue txq = TxRequestQueue.getInstance();
        String[] verificationRecord = new String[]{
                "efaAdminName;" + name,
                "password;" + CsvCodec.encodeElement(password, CsvCodec.DEFAULT_DELIMITER, CsvCodec.DEFAULT_QUOTATION)};
        Transaction txv = txq.appendTransaction(TxRequestQueue.TX_PENDING_QUEUE_INDEX, Transaction.TX_TYPE.VERIFY,
                "efaCloudUsers", verificationRecord);
        if (txv == null) {
            txq.logApiMessage("Failed to append VERIFY transaction to the tx pending queue.", 1);
            return null;
        }
        int waitingFor = 0;
        while ((txv.getResultCode() == 0) && (waitingFor <
                ((InternetAccessManager.TIME_OUT_MONITOR_PERIODS - 1) * InternetAccessManager.MONITOR_PERIOD))) {
            int pollPeriod = 200;
            try {
                //noinspection BusyWait
                Thread.sleep(pollPeriod);
                waitingFor += pollPeriod;
            } catch (InterruptedException ignored) {
            }
        }
        int resultCode = txv.getResultCode();
        if (resultCode == 0) {
            txq.logApiMessage("Verification time out.", 1);
            return null;
        }
        String resultMessage = txv.getResultMessage();
        if (resultCode != 300) {
            txq.logApiMessage("Verification failed: " + resultCode + ";" + resultMessage, 1);
            return null;
        } else {
            EfaCloudUserRecord ecr = (EfaCloudUserRecord) createNewRecord();
            if (ecr == null) {
                txq.logApiMessage("Verification failed because the efaCloudUser record could locally not be created.",
                        1);
            } else {
                ArrayList<String> lines = CsvCodec.splitLines(resultMessage, '"');
                if (lines.size() < 2) {
                    txq.logApiMessage("Verification failed: Returned user record is incomplete.", 1);
                    return null;
                }
                ArrayList<String> keys = CsvCodec.splitEntries(lines.get(0));
                ArrayList<String> values = CsvCodec.splitEntries(lines.get(1));
                int i = 0;
                for (String key : keys) {
                    if (key.equalsIgnoreCase(EfaCloudUserRecord.EFAADMINNAME))
                        ecr.setAdminName(values.get(i));
                    else if (key.equalsIgnoreCase(EfaCloudUserRecord.EFACLOUDUSERID))
                        ecr.setEfaCloudUserID(Integer.parseInt(values.get(i)));
                    else if (key.equalsIgnoreCase(EfaCloudUserRecord.EMAIL))
                        ecr.setEmail(values.get(i));
                    else if (key.equalsIgnoreCase(EfaCloudUserRecord.ROLLE))
                        ecr.setRole(values.get(i));
                    else if (key.equalsIgnoreCase(EfaCloudUserRecord.CONCESSIONS))
                        ecr.setConcessions(Integer.parseInt(values.get(i)));
                    else if (key.equalsIgnoreCase(EfaCloudUserRecord.WORKFLOWS))
                        ecr.setWorkflows(Integer.parseInt(values.get(i)));
                    i++;
                }
                txq.logApiMessage("state, []: Verification successful for " + ecr.getAdminName() +
                        " (" + ecr.getEfaCloudUserID() + ")", 0);
            }
            return ecr;
        }
    }

    /**
     * Login procedure in an analogue manner as for admins, but verifying the user by a server check.
     * @param admins the efa local admins
     * @param name the admin name of the user
     * @param password the password of the user as it was entered by him/her.
     * @return the verified user record in the form of an EfaCloudUserRecord record. Null if any error or timeout occurs.
     */
    public EfaCloudUserRecord login(Admins admins, String name, String password) {
        try {
            EfaCloudUserRecord ecr = verifiy_credentials(name, password);
            if (ecr != null) {
                new AdminRecord(admins, ecr);
                return ecr;
            } else
                return null;
        } catch (Exception e) {
            return null;
        }
    }

}
