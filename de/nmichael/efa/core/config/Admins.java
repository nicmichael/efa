/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.config;

import de.nmichael.efa.*;
import de.nmichael.efa.data.efacloud.TxRequestQueue;
import de.nmichael.efa.ex.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.DataTypePasswordHashed;
import java.util.Random;

// @i18n complete

public class Admins extends StorageObject {

    public static final String DATATYPE = "efa2admins";

    public static final String SUPERADMIN = "admin";
    public static final String EFALIVEADMIN = "efalive";

    public Admins(int storageType, 
            String storageLocation,
            String storageUsername,
            String storagePassword) {
        super(storageType, storageLocation, storageUsername, storagePassword, "admins", DATATYPE, International.getString("Administratoren"));
        AdminRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public Admins() {
        super(IDataAccess.TYPE_FILE_XML, Daten.efaCfgDirectory, null, null, "admins", DATATYPE, International.getString("Administratoren"));
        AdminRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
    }

    public void open(boolean createNewIfNotExists) throws EfaException {
        super.open(createNewIfNotExists);
    }

    public String generateRandomPassword(int length) {
        Random rand = new Random();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                            "abcdefghijklmnopqrstuvwxyz" +
                            "0123456789!§$5&/()=?+*~#'-_.:,;";
        StringBuffer password = new StringBuffer();
        for (int i=0; i<length; i++) {
            password.append(characters.charAt(Math.abs(rand.nextInt()) % characters.length()));;
        }
        return password.toString();
    }

    public boolean isEfaLiveAdminExists() {
        if (!isOpen()) {
            return false;
        }
        return getAdmin(EFALIVEADMIN) != null;
    }

    public boolean isEfaLiveAdminOk() {
        if (!isOpen()) {
            return false;
        }
        AdminRecord admin = getAdmin(EFALIVEADMIN);
        if (admin == null) {
            return false;
        }

        Credentials cred = new Credentials();
        if (!cred.readCredentials()) {
            return false; // cred file doesn't exist or can't be read
        }
        String credPassword = cred.getPassword(EFALIVEADMIN);

        if (admin.getPassword() == null || admin.getPassword().getPassword() == null ||
            credPassword == null || credPassword.length() < AdminRecord.MIN_PASSWORD_LENGTH ||
            !admin.getPassword().getPassword().equals(DataTypePasswordHashed.encrypt(credPassword))) {
            return false;
        }
        if (!admin.isAllowedRemoteAccess()) {
            return false;
        }
        if (!admin.isAllowedCreateBackup()) {
            return false;
        }
        if (!admin.isAllowedRestoreBackup()) {
            return false;
        }
        return true;
    }
    
    public boolean createOrFixEfaLiveAdmin() {
        boolean isAdminNew = false;
        boolean isAdminChanged = false;
        boolean isCredFileChanged = false;

        Credentials cred = new Credentials();
        String credPassword = cred.getPassword(EFALIVEADMIN);
        AdminRecord admin = getAdmin(EFALIVEADMIN);
        String newPlainTextPassword = null;

        if (admin == null) {
            newPlainTextPassword = generateRandomPassword(12);
            admin = createAdminRecord(EFALIVEADMIN, newPlainTextPassword);
            admin.setAllowedRemoteAccess(true);
            admin.setAllowedCreateBackup(true);
            admin.setAllowedRestoreBackup(true);
            isAdminNew = true;
            isCredFileChanged = true;
        } else {
            if (admin.getPassword() == null || admin.getPassword().getPassword() == null ||
                credPassword == null || credPassword.length() < AdminRecord.MIN_PASSWORD_LENGTH ||
                !admin.getPassword().getPassword().equals(DataTypePasswordHashed.encrypt(credPassword))) {
                newPlainTextPassword = generateRandomPassword(12);
                admin.setPassword(newPlainTextPassword);
                isAdminChanged = true;
                isCredFileChanged = true;
            }
            if (!admin.isAllowedRemoteAccess()) {
                admin.setAllowedRemoteAccess(true);
                isAdminChanged = true;
            }
            if (!admin.isAllowedCreateBackup()) {
                admin.setAllowedCreateBackup(true);
                isAdminChanged = true;
            }
            if (!admin.isAllowedRestoreBackup()) {
                admin.setAllowedRestoreBackup(true);
                isAdminChanged = true;
            }
        }
        if ((isAdminNew || isAdminChanged) && data() != null) {
            try {
                if (isAdminNew) {
                    data().add(admin);
                } else {
                    data().update(admin);
                }
            } catch (Exception eignore) {
                Logger.logdebug(eignore);
                return false;
            }
        }
        if (isCredFileChanged) {
            cred.readCredentials();
            cred.addCredentials(EFALIVEADMIN, newPlainTextPassword);
            return cred.writeCredentials();
        }
        return true;
    }

    public DataRecord createNewRecord() {
        return new AdminRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public AdminRecord createAdminRecord(String name, String password) {
        AdminRecord r = new AdminRecord(this, MetaData.getMetaData(DATATYPE));
        r.setName(name);
        r.setPassword(password);
        r.makeSurePermissionsAreCorrect();
        r.setAllowedChangePassword(true);
        return r;
    }

    public AdminRecord getAdmin(String name) {
        try {
            AdminRecord r = (AdminRecord)data().get(AdminRecord.getKey(name));
            if (r != null) {
                r.makeSurePermissionsAreCorrect();
            }
            return r;
        } catch(Exception e) {
            return null;
        }
    }

    public AdminRecord login(String name, String password) {
        if (name == null || password == null || password.length() < AdminRecord.MIN_PASSWORD_LENGTH) {
            return null;
        }
        AdminRecord admin = getAdmin(name);
        if (admin == null || admin.getPassword() == null) {
            // for efaCloud configurations the user may also be authenticated using the efaCloud server
            // authentication. On any error admin will be null.
            if ((Daten.project == null) ||
                    (Daten.project.getProjectStorageType() != IDataAccess.TYPE_EFA_CLOUD))
                return null;
            TxRequestQueue txq = TxRequestQueue.getInstance();
            if (txq == null)
                return null;
            if ((txq.getState() == TxRequestQueue.QUEUE_IS_PAUSED) || (txq.getState() == TxRequestQueue.QUEUE_IS_STOPPED)
                || (txq.getState() == TxRequestQueue.QUEUE_IS_DISCONNECTED)) {
                Dialog.error(International.getString(
                        "Admin login am efaCloud-Server zur Zeit nicht möglich, die Verbindung ist unterbrochen."));
                return null;
            }
            if (TxRequestQueue.efa_cloud_used_api_version < 2) {
                Dialog.error(International.getString(
                        "Admin login am efaCloud-Server benötigt dort Version 2.3.1 und höher."));
                return null;
            }
            EfaCloudUsers efaCloudUsers = Daten.project.getEfaCloudUsers(true);
            EfaCloudUserRecord ecr = efaCloudUsers.login(this, name, password);
            if (ecr == null)
                return null;
            admin = new AdminRecord(this, ecr);
            txq = TxRequestQueue.getInstance();
            txq.setAdminCredentials(ecr.getAdminName(),Integer.toString(ecr.getEfaCloudUserID()), password);
            return admin;
        }
        if (admin.getPassword().equals(new DataTypePasswordHashed(password))) {
            // local admin, do not change the efaCloud credentials.
            return admin;
        }
        return null;
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        AdminRecord ar = (AdminRecord) record;
        if (add || update) {
            if (ar.getName() == null || ar.getName().trim().length() == 0) {
                throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                        International.getMessage("Das Feld '{field}' darf nicht leer sein.", AdminRecord.NAME),
                        Thread.currentThread().getStackTrace());
            }
            if (ar.getPassword() == null || !ar.getPassword().isSet()) {
                throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                        International.getMessage("Das Feld '{field}' darf nicht leer sein.", AdminRecord.PASSWORD),
                        Thread.currentThread().getStackTrace());
            }
            ar.makeSurePermissionsAreCorrect();
        }
        if (delete) {
            if (ar.getName() != null && ar.getName().equals(SUPERADMIN)) {
                throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                        International.getString("Dieser Datensatz kann nicht gelöscht werden."),
                        Thread.currentThread().getStackTrace());
            }
        }
    }

}
