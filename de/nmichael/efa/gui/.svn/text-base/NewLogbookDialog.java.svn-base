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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.JDialog;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class NewLogbookDialog extends StepwiseDialog {

    private static final String LOGBOOKNAME        = "LOGBOOKNAME";
    private static final String LOGBOOKDESCRIPTION = "LOGBOOKDESCRIPTION";
    private static final String DATEFROM           = "DATEFROM";
    private static final String DATETO             = "DATETO";
    private static final String AUTOMATICLOGSWITCH = "AUTOMATICLOGSWITCH";
    private static final String LOGSWITCHBOATHOUSE = "LOGSWITCHBOATHOUSE";

    private String newLogbookName;

    public NewLogbookDialog(JDialog parent) {
        super(parent, International.getString("Neues Fahrtenbuch"));
    }

    public NewLogbookDialog(Frame parent) {
        super(parent, International.getString("Neues Fahrtenbuch"));
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    String[] getSteps() {
        return new String[] {
            International.getString("Name und Beschreibung"),
            International.getString("Zeitraum für Fahrtenbuch"),
            International.getString("Fahrtenbuchwechsel")
        };
    }

    String getDescription(int step) {
        switch(step) {
            case 0:
                return International.getString("Ein Fahrtenbuch sollte üblicherweise alle Fahrten eines Jahr enthalten. "+
                        "Vereine mit mehreren Bootshäusern sollten pro Bootshaus ein eigenes Fahrtenbuch (in demselben Projekt) anlegen.");
            case 1:
                return International.getString("Bitte wähle den Zeitraum für Fahrten dieses Fahrtenbuches aus. efa wird später nur Fahrten "+
                        "innerhalb dieses Zeitraums für dieses Fahrtenbuch zulassen.");
            case 2:
                return International.getString("Durch die Konfiguration eines Fahrtenbuchwechsels ist es möglich, ein neues Fahrtenbuch "+
                        "automatisch zu einem konfigurierten Zeitpunkt (zum Beispiel Jahreswechsel) zu öffnen. efa-Bootshaus beendet dann im aktuellen " +
                        "Fahrtenbuch alle noch offenen Fahrten, schließt das aktuelle Fahrtenbuch und wechselt zum neuen Fahrtenbuch, ohne daß " +
                        "weitere Eingriffe eines Administrators nötig wären.");
        }
        return "";
    }

    void initializeItems() {
        items = new ArrayList<IItemType>();
        IItemType item;

        // Items for Step 0
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        String year = Integer.toString( cal.get(Calendar.MONTH)+1 <= 10 ?
            cal.get(Calendar.YEAR) : cal.get(Calendar.YEAR) + 1); // current year until October, year+1 else

        item = new ItemTypeString(LOGBOOKNAME, year, IItemType.TYPE_PUBLIC, "0", International.getString("Name des Fahrtenbuchs"));
        ((ItemTypeString)item).setAllowedCharacters("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
        ((ItemTypeString)item).setReplacementCharacter('_');
        ((ItemTypeString)item).setNotNull(true);
        items.add(item);
        item = new ItemTypeString(LOGBOOKDESCRIPTION, "", IItemType.TYPE_PUBLIC, "0", International.getString("Beschreibung"));
        items.add(item);

        // Items for Step 1
        item = new ItemTypeDate(DATEFROM, new DataTypeDate(1, 1, EfaUtil.string2int(year, 2010)), IItemType.TYPE_PUBLIC, "1", International.getString("Beginn des Zeitraums"));
        ((ItemTypeDate)item).setNotNull(true);
        items.add(item);
        item = new ItemTypeDate(DATETO, new DataTypeDate(31, 12, EfaUtil.string2int(year, 2010)), IItemType.TYPE_PUBLIC, "1", International.getString("Ende des Zeitraums"));
        ((ItemTypeDate)item).setNotNull(true);
        items.add(item);

        // Items for Step 2
        item = new ItemTypeBoolean(AUTOMATICLOGSWITCH, false, IItemType.TYPE_PUBLIC, "2",
                International.getMessage("Fahrtenbuchwechsel automatisch zum {datum}", "?"));
        items.add(item);
        if (Daten.project.getNumberOfBoathouses() > 1) {
            String[] descr  = Daten.project.getAllBoathouseNames();
            String[] values = new String[descr.length];
            for (int i=0; i<values.length; i++) {
                values[i] = Integer.toString(Daten.project.getBoathouseId(descr[i]));
            }
            item = new ItemTypeStringList(LOGSWITCHBOATHOUSE,
                    Integer.toString(Daten.project.getMyBoathouseId()),
                    values, descr,
                    IItemType.TYPE_PUBLIC, "2",
                    International.getString("Bootshaus"));
            items.add(item);
        }
    }

    boolean checkInput(int direction) {
        boolean ok = super.checkInput(direction);
        if (!ok) {
            return false;
        }

        if (step == 0) {
            ItemTypeString item = (ItemTypeString)getItemByName(LOGBOOKNAME);
            String name = item.getValue();
            if (Daten.project.getLoogbookRecord(name) != null) {
                    Dialog.error(LogString.fileAlreadyExists(name, International.getString("Fahrtenbuch")));
                    item.requestFocus();
                    return false;
            }

            int year = EfaUtil.stringFindInt(name, -1);
            if (year > 1980 && year < 2100) {
                ItemTypeDate logFromDate = (ItemTypeDate) getItemByName(DATEFROM);
                ItemTypeDate logFromTo = (ItemTypeDate) getItemByName(DATETO);
                if (logFromDate != null) {
                    logFromDate.setValueDate(new DataTypeDate(1, 1, year));
                }
                if (logFromTo != null) {
                    logFromTo.setValueDate(new DataTypeDate(31, 12, year));
                }
            }
        }

        if (step == 1) {
            ItemTypeDate logFromDate = (ItemTypeDate)getItemByName(DATEFROM);
            ItemTypeBoolean logswitch = (ItemTypeBoolean)getItemByName(AUTOMATICLOGSWITCH);
            if (logFromDate != null && logswitch != null) {
                logswitch.setDescription(International.getMessage("Fahrtenbuchwechsel automatisch zum {datum}",
                        logFromDate.toString()));
            }
        }

        return true;
    }

    boolean finishButton_actionPerformed(ActionEvent e) {
        if (!super.finishButton_actionPerformed(e)) {
            return false;
        }

        ItemTypeString logName = (ItemTypeString)getItemByName(LOGBOOKNAME);
        ItemTypeString logDescription = (ItemTypeString)getItemByName(LOGBOOKDESCRIPTION);
        ItemTypeDate logFromDate = (ItemTypeDate)getItemByName(DATEFROM);
        ItemTypeDate logFromTo = (ItemTypeDate)getItemByName(DATETO);
        ItemTypeBoolean logswitch = (ItemTypeBoolean)getItemByName(AUTOMATICLOGSWITCH);
        ItemTypeStringList logswitchBoathouse = (ItemTypeStringList)getItemByName(LOGSWITCHBOATHOUSE);

        ProjectRecord rec = Daten.project.createNewLogbookRecord(logName.getValue());
        rec.setDescription(logDescription.getValue());
        rec.setStartDate(logFromDate.getDate());
        rec.setEndDate(logFromTo.getDate());
        
        try {
            Daten.project.addLogbookRecord(rec);
            newLogbookName = logName.getValue();
            Daten.project.getLogbook(newLogbookName, true);
            Dialog.infoDialog(LogString.fileSuccessfullyCreated(logName.getValue(),
                    International.getString("Fahrtenbuch")));
            if (logswitch.getValue()) {
                boolean switchok = false;
                String bh = (logswitchBoathouse == null ? Daten.project.getMyBoathouseName() :
                    Daten.project.getBoathouseName(EfaUtil.string2int(logswitchBoathouse.getValue(), -1)));
                if (bh != null) {
                    ProjectRecord r = Daten.project.getBoathouseRecord(bh);
                    if (r != null) {
                        r.setAutoNewLogbookDate(logFromDate.getDate());
                        r.setAutoNewLogbookName(logName.getValue());
                        Daten.project.getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
                        switchok = true;
                    }
                }
                if (switchok) {
                    Dialog.infoDialog(International.getMessage("Fahrtenbuchwechsel zum {datum} konfiguriert.",
                            logFromDate.getDate().toString()));
                } else {
                    Dialog.error(International.getString("Fahrtenbuchwechsel konnte nicht konfiguriert werden.") + "\n" +
                            "BoathouseRecord not found - please check your boathouse configuration in the project");
                }
            }
            setDialogResult(true);
        } catch(EfaException ee) {
            newLogbookName = null;
            Dialog.error(ee.getMessage());
            ee.log();
            setDialogResult(false);
        }
        
        return true;
    }

    public String getNewLogbookName() {
        return newLogbookName;
    }

    public String newLogbookDialog() {
        showDialog();
        return getNewLogbookName();
    }

}
