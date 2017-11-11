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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class NewClubworkBookDialog extends StepwiseDialog {

    private static final String CLUBWORKNAME        = "CLUBWORKNAME";
    private static final String CLUBWORKDESCRIPTION = "CLUBWORKDESCRIPTION";
    private static final String DATEFROM           = "DATEFROM";
    private static final String DATETO             = "DATETO";
//    private static final String AUTOMATICCLUBWORKSWITCH = "AUTOMATICCLUBWORKSWITCH";
//    private static final String CLUBWORKSWITCHBOATHOUSE = "CLUBWORKSWITCHBOATHOUSE";

    private String newClubworkName;

    public NewClubworkBookDialog(JDialog parent) {
        super(parent, International.getString("Neues Vereinsarbeitsbuch"));
    }

    public NewClubworkBookDialog(Frame parent) {
        super(parent, International.getString("Neues Vereinsarbeitsbuch"));
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    String[] getSteps() {
        return new String[] {
            International.getString("Name und Beschreibung"),
            International.getString("Details für Vereinsarbeitsbuch"),
//            International.getString("Vereinsarbeitsbuchwechsel")
        };
    }

    String getDescription(int step) {
        switch(step) {
            case 0:
                return International.getString("Ein Vereinsarbeitsbuch sollte üblicherweise alle Arbeiten eines Jahr enthalten. "+
                        "Vereine mit mehreren Bootshäusern sollten pro Bootshaus ein eigenes Vereinsarbeitsbuch (in demselben Projekt) anlegen.");
            case 1:
                return International.getString("Bitte wähle den Zeitraum für dieses Vereinsarbeitsbuch aus. efa wird später nur Arbeit "+
                        "innerhalb dieses Zeitraums für dieses Vereinsarbeitsbuch zulassen.");
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

        item = new ItemTypeString(CLUBWORKNAME, year, IItemType.TYPE_PUBLIC, "0", International.getString("Name des Vereinsarbeitsbuch"));
        ((ItemTypeString)item).setAllowedCharacters("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_");
        ((ItemTypeString)item).setReplacementCharacter('_');
        ((ItemTypeString)item).setNotNull(true);
        items.add(item);
        item = new ItemTypeString(CLUBWORKDESCRIPTION, "", IItemType.TYPE_PUBLIC, "0", International.getString("Beschreibung"));
        items.add(item);

        // Items for Step 1
        item = new ItemTypeDate(DATEFROM, new DataTypeDate(1, 1, EfaUtil.string2int(year, 2010)), IItemType.TYPE_PUBLIC, "1", International.getString("Beginn des Zeitraums"));
        ((ItemTypeDate)item).setNotNull(true);
        items.add(item);
        item = new ItemTypeDate(DATETO, new DataTypeDate(31, 12, EfaUtil.string2int(year, 2010)), IItemType.TYPE_PUBLIC, "1", International.getString("Ende des Zeitraums"));
        ((ItemTypeDate)item).setNotNull(true);
        items.add(item);

		items.add(item = new ItemTypeDouble(ProjectRecord.DEFAULTCLUBWORKTARGETHOURS, 0, 0, ItemTypeDouble.MAX,
				IItemType.TYPE_PUBLIC, "1",
				International.getString("Sollstunden für Vereinsarbeit")));

		items.add(item = new ItemTypeDouble(ProjectRecord.TRANSFERABLECLUBWORKHOURS, 0, 0, ItemTypeDouble.MAX,
				IItemType.TYPE_PUBLIC, "1",
				International.getString("Übertragbare Vereinsarbeitsstunden pro Zeitraum")));

		items.add(item = new ItemTypeDouble(ProjectRecord.FINEFORTOOLITTLECLUBWORK, 0, 0, ItemTypeDouble.MAX,
				IItemType.TYPE_PUBLIC, "1",
				International.getString("Bußgeld für Vereinsarbeit unter Sollstunden")));

        // Items for Step 2
//        item = new ItemTypeBoolean(AUTOMATICCLUBWORKSWITCH, false, IItemType.TYPE_PUBLIC, "2",
//                International.getMessage("Vereinsarbeitsbuchwechsel automatisch zum {datum}", "?"));
//        items.add(item);
//        if (Daten.project.getNumberOfBoathouses() > 1) {
//            String[] descr  = Daten.project.getAllBoathouseNames();
//            String[] values = new String[descr.length];
//            for (int i=0; i<values.length; i++) {
//                values[i] = Integer.toString(Daten.project.getBoathouseId(descr[i]));
//            }
//            item = new ItemTypeStringList(CLUBWORKSWITCHBOATHOUSE,
//                    Integer.toString(Daten.project.getMyBoathouseId()),
//                    values, descr,
//                    IItemType.TYPE_PUBLIC, "2",
//                    International.getString("Bootshaus"));
//            items.add(item);
//        }
    }

    boolean checkInput(int direction) {
        boolean ok = super.checkInput(direction);
        if (!ok) {
            return false;
        }

        if (step == 0) {
            ItemTypeString item = (ItemTypeString)getItemByName(CLUBWORKNAME);
            String name = item.getValue();
            if (Daten.project.getClubworkBookRecord(name) != null) {
                    Dialog.error(LogString.fileAlreadyExists(name, International.getString("Vereinsarbeit")));
                    item.requestFocus();
                    return false;
            }

            int year = EfaUtil.stringFindInt(name, -1);
            if (year > 1980 && year < 2100) {
                ItemTypeDate clubworkFromDate = (ItemTypeDate) getItemByName(DATEFROM);
                ItemTypeDate clubworkFromTo = (ItemTypeDate) getItemByName(DATETO);
                if (clubworkFromDate != null) {
                    clubworkFromDate.setValueDate(new DataTypeDate(1, 1, year));
                }
                if (clubworkFromTo != null) {
                    clubworkFromTo.setValueDate(new DataTypeDate(31, 12, year));
                }
            }
        }

//        if (step == 1) {
//            ItemTypeDate clubworkFromDate = (ItemTypeDate)getItemByName(DATEFROM);
//            ItemTypeBoolean clubworkswitch = (ItemTypeBoolean)getItemByName(AUTOMATICCLUBWORKSWITCH);
//            if (clubworkFromDate != null && clubworkswitch != null) {
//                clubworkswitch.setDescription(International.getMessage("Vereinsarbeitsbuchwechsel automatisch zum {datum}",
//                        clubworkFromDate.toString()));
//            }
//        }

        return true;
    }

    boolean finishButton_actionPerformed(ActionEvent e) {
        if (!super.finishButton_actionPerformed(e)) {
            return false;
        }

        ItemTypeString clubworkName = (ItemTypeString)getItemByName(CLUBWORKNAME);
        ItemTypeString clubworkDescription = (ItemTypeString)getItemByName(CLUBWORKDESCRIPTION);
        ItemTypeDate clubworkFromDate = (ItemTypeDate)getItemByName(DATEFROM);
        ItemTypeDate clubworkFromTo = (ItemTypeDate)getItemByName(DATETO);
//        ItemTypeBoolean clubworkswitch = (ItemTypeBoolean)getItemByName(AUTOMATICCLUBWORKSWITCH);
//        ItemTypeStringList clubworkSwitchBoathouse = (ItemTypeStringList)getItemByName(CLUBWORKSWITCHBOATHOUSE);

		ItemTypeDouble clubworkTargetHours = (ItemTypeDouble)getItemByName(ProjectRecord.DEFAULTCLUBWORKTARGETHOURS);
		ItemTypeDouble clubworkTransferableHours = (ItemTypeDouble)getItemByName(ProjectRecord.TRANSFERABLECLUBWORKHOURS);
		ItemTypeDouble clubworkFine = (ItemTypeDouble)getItemByName(ProjectRecord.FINEFORTOOLITTLECLUBWORK);

        ProjectRecord rec = Daten.project.createNewClubworkBook(clubworkName.getValue());
        rec.setDescription(clubworkDescription.getValue());
        rec.setStartDate(clubworkFromDate.getDate());
        rec.setEndDate(clubworkFromTo.getDate());
		rec.setDefaultClubworkTargetHours(clubworkTargetHours.getValue());
		rec.setTransferableClubworkHours(clubworkTransferableHours.getValue());
		rec.setFineForTooLittleClubwork(clubworkFine.getValue());
        
        try {
            Daten.project.addClubworkBookRecord(rec);
            newClubworkName = clubworkName.getValue();
            Daten.project.getClubwork(newClubworkName, true);
            Daten.project.getClubwork(newClubworkName, true);
            Dialog.infoDialog(LogString.fileSuccessfullyCreated(clubworkName.getValue(),
                    International.getString("Vereinsarbeitsbuch")));
//            if (clubworkswitch.getValue()) {
//                boolean switchok = false;
//                String bh = (clubworkSwitchBoathouse == null ? Daten.project.getMyBoathouseName() :
//                    Daten.project.getBoathouseName(EfaUtil.string2int(clubworkSwitchBoathouse.getValue(), -1)));
//                if (bh != null) {
//                    ProjectRecord r = Daten.project.getBoathouseRecord(bh);
//                    if (r != null) {
//                        r.setAutoNewClubworkDate(clubworkFromDate.getDate());
//                        r.setAutoNewClubworkName(clubworkName.getValue());
//                        Daten.project.data().update(r);
//                        switchok = true;
//                    }
//                }
//                if (switchok) {
//                    Dialog.infoDialog(International.getMessage("Vereinsarbeitsbuch zum {datum} konfiguriert.",
//                            clubworkFromDate.getDate().toString()));
//                } else {
//                    Dialog.error(International.getString("Vereinsarbeitsbuch konnte nicht konfiguriert werden.") + "\n" +
//                            "BoathouseRecord not found - please check your boathouse configuration in the project");
//                }
//            }
            setDialogResult(true);
        } catch(EfaException ee) {
            newClubworkName = null;
            Dialog.error(ee.getMessage());
            ee.log();
            setDialogResult(false);
        }
        
        return true;
    }

    public String getNewClubworkName() {
        return newClubworkName;
    }

    public String newClubworkBookDialog() {
        showDialog();
        return getNewClubworkName();
    }

}
