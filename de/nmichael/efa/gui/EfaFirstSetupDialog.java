/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.*;
import de.nmichael.efa.core.config.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import org.apache.batik.ext.swing.GridBagConstants;

public class EfaFirstSetupDialog extends StepwiseDialog {

    static final String ADMIN_NAME           = "ADMIN_NAME";
    static final String ADMIN_PASSWORD       = "ADMIN_PASSWORD";

    static final String CUST_ROWING          = "CUST_ROWING";
    static final String CUST_ROWINGGERMANY   = "CUST_ROWINGGERMANY";
    static final String CUST_ROWINGBERLIN    = "CUST_ROWINGBERLIN";
    static final String CUST_CANOEING        = "CUST_CANOEING";
    static final String CUST_CANOEINGGERMANY = "CUST_CANOEINGGERMANY";

    static final String EFALIVE_CREATEADMIN  = "EFALIVE_CREATEADMIN";

    private boolean createSuperAdmin;
    private boolean efaCustomization;
    private boolean efaLiveAdmin;
    private CustSettings custSettings = null;
    private AdminRecord newSuperAdmin = null;

    public EfaFirstSetupDialog(boolean createSuperAdmin, boolean efaCustomization) {
        super((JFrame)null, Daten.EFA_LONGNAME);
        this.createSuperAdmin = createSuperAdmin;
        this.efaCustomization = efaCustomization;
        this.efaLiveAdmin = Daten.EFALIVE_VERSION != null && !Daten.admins.isEfaLiveAdminOk();
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    private int getNumberOfSteps() {
        int stepCnt = 1;
        if (createSuperAdmin) {
            stepCnt++;
        }
        if (efaCustomization) {
            stepCnt++;
        }
        if (efaLiveAdmin) {
            stepCnt++;
        }
        return stepCnt;
    }

    private int getCreateSuperAdminStep() {
        return (createSuperAdmin ? 1 : 99);
    }

    private int getEfaCustomizationStep() {
        return (efaCustomization ? (createSuperAdmin ? 2 : 1) : 99);
    }

    private int getEfaLiveAdminStep() {
        return (efaLiveAdmin ? (efaCustomization ? (createSuperAdmin ? 3 : 2) : 1) : 99);
    }

    String[] getSteps() {
        int i=0;
        String[] steps = new String[getNumberOfSteps()];
        steps[i++] = International.getString("Willkommen!");
        if (createSuperAdmin) {
            steps[i++] = International.getString("Hauptadministrator anlegen");
        }
        if (efaCustomization) {
            steps[i++] = International.getString("Einstellungen");
        }
        if (efaLiveAdmin) {
            steps[i++] = Daten.EFA_LIVE;
        }
        return steps;
    }

    String getDescription(int step) {
        if (step == 0) {
            return International.getString("Willkommen bei efa, dem elektronischen Fahrtenbuch!") + "\n"
                 + International.getString("Dieser Dialog führt Dich durch die ersten Schritte, um efa einzurichten.");
        }
        if (step == getCreateSuperAdminStep()) {
            return International.getString("Alle Administrationsaufgaben in efa erfordern Administratorrechte.") + "\n"
                 + International.getString("Bitte lege ein Paßwort (mindestens 6 Zeichen) für den Hauptadministrator 'admin' fest.");
        }
        if (step == getEfaCustomizationStep()) {
            return International.getString("Welche Funktionen von efa möchtest Du verwenden?") + "\n"
                 + International.getString("Du kannst diese Einstellungen jederzeit in der efa-Konfiguration ändern.");
        }
        if (step == getEfaLiveAdminStep()) {
            return International.getString("Bestimmte Funktionen von efaLive (Erstellen oder Einspielen eines Backups) erfordern, daß efaLive Administrator-Zugriff auf efa hat.") + "\n"
                 + International.getString("Möchtest Du jetzt einen Administrator für efaLive anlegen?") + " "
                 + International.getString("Du kannst diesen Administrator jederzeit in der Verwaltung der Administratoren wieder löschen.");
        }
        return "";
    }

    void initializeItems() {
        items = new ArrayList<IItemType>();
        IItemType item;

        // Items for Step 0
        items.add(item = new ItemTypeLabel("LOGO", IItemType.TYPE_PUBLIC, "0", ""));
        ((ItemTypeLabel)item).setImage(getIcon(Daten.getEfaImage(3)));
        ((ItemTypeLabel)item).setFieldGrid(-1, GridBagConstants.CENTER, GridBagConstants.HORIZONTAL);
        ((ItemTypeLabel)item).setPadding(10, 10, 10, 10);
        items.add(item = new ItemTypeLabel("EFA", IItemType.TYPE_PUBLIC, "0", Daten.EFA_LONGNAME));
        ((ItemTypeLabel)item).setHorizontalAlignment(SwingConstants.CENTER);
        ((ItemTypeLabel)item).setFieldGrid(-1, GridBagConstants.CENTER, GridBagConstants.HORIZONTAL);
        items.add(item = new ItemTypeLabel("VERSION", IItemType.TYPE_PUBLIC, "0", International.getString("Version") + " " + Daten.VERSIONID+ " (" + Daten.VERSIONRELEASEDATE +")"));
        ((ItemTypeLabel)item).setHorizontalAlignment(SwingConstants.CENTER);
        ((ItemTypeLabel)item).setFieldGrid(-1, GridBagConstants.CENTER, GridBagConstants.HORIZONTAL);

        // Items for Step 1 (CreateSuperAdmin)
        items.add(item = new ItemTypeLabel("ADMIN_LABEL", IItemType.TYPE_PUBLIC,
                Integer.toString(getCreateSuperAdminStep()), International.getString("Neuer Hauptadministrator")));
        items.add(item = new ItemTypeString(ADMIN_NAME, Admins.SUPERADMIN, IItemType.TYPE_PUBLIC,
                Integer.toString(getCreateSuperAdminStep()), International.getString("Name")));
        ((ItemTypeString)item).setEditable(false);
        items.add(item = new ItemTypePassword(ADMIN_PASSWORD, "",  IItemType.TYPE_PUBLIC,
                Integer.toString(getCreateSuperAdminStep()), International.getString("Paßwort")));
        ((ItemTypePassword)item).setNotNull(true);
        ((ItemTypePassword)item).setMinCharacters(6);
        items.add(item = new ItemTypePassword(ADMIN_PASSWORD+"_REPEAT", "", IItemType.TYPE_PUBLIC,
                Integer.toString(getCreateSuperAdminStep()), International.getString("Paßwort") +
                " (" + International.getString("Wiederholung") + ")"));
        ((ItemTypePassword)item).setNotNull(true);
        ((ItemTypePassword)item).setMinCharacters(6);

        // Items for Step 2 (EfaCustomization)
        items.add(item = new ItemTypeLabel("CUST_LABEL", IItemType.TYPE_PUBLIC,
                Integer.toString(getEfaCustomizationStep()),
                International.getString("Welche Funktionen von efa möchtest Du verwenden?")));
        items.add(item = new ItemTypeBoolean(CUST_ROWING, true, IItemType.TYPE_PUBLIC,
                Integer.toString(getEfaCustomizationStep()), International.getString("Rudern")));
        items.add(item = new ItemTypeBoolean(CUST_ROWINGGERMANY, International.getLanguageID().startsWith("de"), IItemType.TYPE_PUBLIC,
                Integer.toString(getEfaCustomizationStep()),
                International.getString("Rudern") + " " +
                International.getMessage("in {region}",
                International.getString("Deutschland"))));
        items.add(item = new ItemTypeBoolean(CUST_ROWINGBERLIN, false, IItemType.TYPE_PUBLIC,
                Integer.toString(getEfaCustomizationStep()),
                International.getString("Rudern") + " " +
                International.getMessage("in {region}",
                International.getString("Berlin"))));
        items.add(item = new ItemTypeBoolean(CUST_CANOEING, false, IItemType.TYPE_PUBLIC,
                Integer.toString(getEfaCustomizationStep()),
                International.getString("Kanufahren")));
        items.add(item = new ItemTypeBoolean(CUST_CANOEINGGERMANY, false, IItemType.TYPE_PUBLIC,
                Integer.toString(getEfaCustomizationStep()),
                International.getString("Kanufahren") + " " +
                International.getMessage("in {region}",
                International.getString("Deutschland"))));

        // Items for Step 3 (EfaLiveAdmin)
        items.add(item = new ItemTypeBoolean(EFALIVE_CREATEADMIN, true, IItemType.TYPE_PUBLIC,
                Integer.toString(getEfaLiveAdminStep()),
                International.getMessage("Admin '{name}' erstellen", Admins.EFALIVEADMIN)));
    }

    boolean checkInput(int direction) {
        boolean ok = super.checkInput(direction);
        if (ok && step == getCreateSuperAdminStep())  {
            String pass1 = getItemByName(ADMIN_PASSWORD).toString();
            String pass2 = getItemByName(ADMIN_PASSWORD+"_REPEAT").toString();
            if (!pass1.equals(pass2)) {
                Dialog.error(International.getMessage("Paßwort in Feld '{field}' nicht identisch.", getItemByName(ADMIN_PASSWORD+"_REPEAT").getDescription()));
                return false;
            }
        }
        return ok;
    }


    boolean finishButton_actionPerformed(ActionEvent e) {
        if (!checkInput(0)) {
            return false;
        }
        if (createSuperAdmin) {
            createNewSuperAdmin(((ItemTypePassword)getItemByName(ADMIN_PASSWORD)).getValue());
        }
        if (efaCustomization) {
            custSettings = new CustSettings();
            custSettings.activateRowingOptions = ((ItemTypeBoolean)getItemByName(CUST_ROWING)).getValue();
            custSettings.activateGermanRowingOptions = ((ItemTypeBoolean)getItemByName(CUST_ROWINGGERMANY)).getValue();
            custSettings.activateBerlinRowingOptions = ((ItemTypeBoolean)getItemByName(CUST_ROWINGBERLIN)).getValue();
            custSettings.activateCanoeingOptions = ((ItemTypeBoolean)getItemByName(CUST_CANOEING)).getValue();
            custSettings.activateGermanCanoeingOptions = ((ItemTypeBoolean)getItemByName(CUST_CANOEINGGERMANY)).getValue();
        }
        if (efaLiveAdmin) {
            if (((ItemTypeBoolean)getItemByName(EFALIVE_CREATEADMIN)).getValue()) {
                Daten.admins.createOrFixEfaLiveAdmin();
            }
        }
        setDialogResult(true);
        cancel();
        return true;
    }

    void createNewSuperAdmin(String password) {
        if (password == null || password.length() == 0) {
            return;
        }
        try {
            Daten.admins.open(true);
            // ok, new admin file created (or existing, empty one opened). Now add admin
            AdminRecord r = Daten.admins.createAdminRecord(Admins.SUPERADMIN, password);
            Daten.admins.data().add(r);
            //Now delete sec file
            Daten.efaSec.delete(true);
            newSuperAdmin = r;
        } catch (Exception ee) {
            String msg = LogString.fileCreationFailed(((DataFile) Daten.admins.data()).getFilename(),
                    International.getString("Administratoren"));
            Logger.log(Logger.ERROR, Logger.MSG_CORE_ADMINSFAILEDCREATE, msg);
            if (Daten.isGuiAppl()) {
                Dialog.error(msg);
            }
            Daten.haltProgram(Daten.HALT_ADMIN);
        }
        String msg = LogString.fileNewCreated(((DataFile) Daten.admins.data()).getFilename(),
                International.getString("Administratoren"));
        Logger.log(Logger.WARNING, Logger.MSG_CORE_ADMINSCREATEDNEW, msg);
        Dialog.infoDialog(International.getString("Neuer Hauptadministrator"),
                International.getString("Ein neuer Administrator mit Namen 'admin' wurde angelegt. Bitte notiere Dir Name und Paßwort an einem sicheren Ort."));
    }

    public CustSettings getCustSettings() {
        return custSettings;
    }

    public AdminRecord getNewSuperAdmin() {
        return newSuperAdmin;
    }
}
