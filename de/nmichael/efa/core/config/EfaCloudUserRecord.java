package de.nmichael.efa.core.config;

import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;

import java.awt.*;
import java.util.Vector;

public class EfaCloudUserRecord  extends DataRecord implements IItemListener {

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String ID               = "ID";
    public static final String EFACLOUDUSERID   = "efaCloudUserID";
    public static final String EMAIL            = "EMail";
    public static final String EFAADMINNAME     = "efaAdminName";
    public static final String VORNAME          = "Vorname";
    public static final String NACHNAME         = "Nachname";
    public static final String ROLLE            = "Rolle";
    public static final String WORKFLOWS        = "Workflows";
    public static final String CONCESSIONS      = "Concessions";
    public static final String PASSWORT_HASH    = "Passwort_Hash";
    public static final String USECRM           = "UseEcrm";

    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(ID);                                t.add(IDataAccess.DATA_INTEGER);
        f.add(EFACLOUDUSERID);                    t.add(IDataAccess.DATA_INTEGER);
        f.add(EMAIL);                             t.add(IDataAccess.DATA_STRING);
        f.add(EFAADMINNAME);                      t.add(IDataAccess.DATA_STRING);
        f.add(VORNAME);                           t.add(IDataAccess.DATA_STRING);
        f.add(NACHNAME);                          t.add(IDataAccess.DATA_STRING);
        f.add(ROLLE);                             t.add(IDataAccess.DATA_STRING);
        f.add(WORKFLOWS);                         t.add(IDataAccess.DATA_INTEGER);
        f.add(CONCESSIONS);                       t.add(IDataAccess.DATA_INTEGER);
        f.add(PASSWORT_HASH);                     t.add(IDataAccess.DATA_STRING);
        f.add(USECRM);                            t.add(IDataAccess.DATA_STRING);

        MetaData metaData = constructMetaData(EfaCloudUsers.DATATYPE, f, t, false);
        metaData.setKey(new String[] { EFACLOUDUSERID });
    }

    public EfaCloudUserRecord(EfaCloudUsers efaCloudUsers, MetaData metaData) {
        super(efaCloudUsers, metaData);
    }

    @Override
    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    @Override
    public DataKey getKey() {
        return new DataKey<String,String,String>(Integer.toString(getEfaCloudUserID()),null,null);
    }

    public static DataKey getKey(String name) {
        return new DataKey<String,String,String>(name,null,null);
    }

    public void setAdminName(String efaAdminName) {
        setString(EFAADMINNAME, efaAdminName);
    }
    public String getAdminName() {
        return getString(EFAADMINNAME);
    }

    public void setEfaCloudUserID(int userID) {
        setInt(EFACLOUDUSERID, userID);
    }
    public int getEfaCloudUserID() {
        return getInt(EFACLOUDUSERID);
    }

    public void setEmail(String email) {
        setString(EMAIL, email);
    }
    public String getEmail() {
        return getString(EMAIL);
    }

    public void setRole(String email) {
        setString(ROLLE, email);
    }
    public String getRole() {
        return getString(ROLLE);
    }

    public void setWorkflows(int workflows) {
        setInt(WORKFLOWS, workflows);
    }
    public int getWorkflows() {
        return getInt(WORKFLOWS);
    }

    public void setConcessions(int concessions) {
        setInt(CONCESSIONS, concessions);
    }
    public int getConcessions() {
        return getInt(CONCESSIONS);
    }

    @Override
    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        // This record type has no GUI. It will be maintained at the efaCloud server application only
    }

    @Override
    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        // This record type has no GUI. It will be maintained at the efaCloud server application only
        return null;
    }

    @Override
    public TableItemHeader[] getGuiTableHeader() {
        // This record type has no GUI. It will be maintained at the efaCloud server application only
        return new TableItemHeader[0];
    }

    @Override
    public TableItem[] getGuiTableItems() {
        // This record type has no GUI. It will be maintained at the efaCloud server application only
        return new TableItem[0];
    }
}
