/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.dataedit;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeButton;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.util.*;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class FixLogbookDialog extends BaseDialog implements IItemListener {

	private Logbook logbook;
    private AdminRecord admin;
    private Hashtable<String,ChangeItem> changes;
    private String infotitle = "";
    private static final int LAST_STEP = 4;
    private int step = 0;
    private Hashtable<String,String> neighbourCache = new Hashtable<String,String>();
    private String guest = International.getString("Gast");

    private JLabel infoLabel;
    private ItemTypeButton skipButton;
    private ItemTypeButton fixButton;
    private JScrollPane changePane;
    private JPanel changePanel;

    public FixLogbookDialog(JDialog parent, Logbook logbook, AdminRecord admin) {
        super(parent, International.getStringWithMnemonic("Korrekturassistent"),
                International.getStringWithMnemonic("Abbruch"));
        this.logbook = logbook;
        this.admin = admin;
    }
    
    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());

        infoLabel = new JLabel();
        infoLabel.setForeground(Color.blue);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setBorder(new EmptyBorder(10,10,10,10));
        mainPanel.add(infoLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        skipButton = new ItemTypeButton("BUTTON_SKIP",
                IItemType.TYPE_PUBLIC, "", International.getString("Überspringen"));
        skipButton.registerItemListener(this);
        skipButton.setIcon(BaseDialog.getIcon(ImagesAndIcons.IMAGE_BUTTON_SKIP));
        skipButton.setPadding(10, 10, 10, 10);
        skipButton.displayOnGui(this, buttonPanel, 0, 0);
        fixButton = new ItemTypeButton("BUTTON_FIX",
                IItemType.TYPE_PUBLIC, "", International.getString("Korrigieren"));
        fixButton.registerItemListener(this);
        fixButton.setIcon(BaseDialog.getIcon(ImagesAndIcons.IMAGE_BUTTON_CORRECTION));
        skipButton.setPadding(10, 10, 10, 10);
        fixButton.displayOnGui(this, buttonPanel, 1, 0);

        changePane = new JScrollPane();
        changePane.setPreferredSize(Dialog.getReducedMaxSize(Dialog.screenSize, 200, 200));
        changePanel = new JPanel();
        changePanel.setLayout(new GridBagLayout());
        changePane.getViewport().add(changePanel, null);
        mainPanel.add(changePane, BorderLayout.CENTER);

        nextStep();
    }

    private void updateFields() {
        if (step == 1) {
            return;
        }
        if (changePanel != null) {
            synchronized(changePane) {
                changePane.remove(changePanel);
            }
        }
        changePanel = new JPanel();
        changePanel.setLayout(new GridBagLayout());
        String[] names = changes.keySet().toArray(new String[0]);
        Arrays.sort(names);
        int y = 0;
        for (String name : names) {
            ChangeItem change = changes.get(name);
            if (change != null) {
                change.displayOnGui(this, changePanel, y++);
            }
        }
        changePane.getViewport().add(changePanel, null);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    private void nextStep() {
        infotitle = "";
        switch(++step) {
            case 1:
                infotitle = International.getString("Korrekturassistent");
                fixButton.setVisible(false);
                skipButton.setVisible(true);
                skipButton.setDescription(International.getString("Weiter"));
                if (changePanel != null) {
                    synchronized (changePane) {
                        changePane.remove(changePanel);
                    }
                }
                changePanel = new JPanel();
                changePanel.setLayout(new BorderLayout());
                JLabel label = new JLabel(International.getString("Dieser Assistent sucht im Fahrtenbuch nach falsch geschriebenen Namen und bietet diese zur Korrektur an."));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                changePanel.add(label, BorderLayout.CENTER);
                changePane.getViewport().add(changePanel, null);
                break;
            case 2:
                infotitle = International.getString("Unbekannte Personen");
                fixButton.setVisible(true);
                skipButton.setVisible(true);
                skipButton.setDescription(International.getString("Überspringen"));
                break;
            case 3:
                infotitle = International.getString("Unbekannte Boote");
                fixButton.setVisible(true);
                skipButton.setVisible(true);
                skipButton.setDescription(International.getString("Überspringen"));
                break;
            case 4:
                infotitle = International.getString("Unbekannte Ziele");
                fixButton.setVisible(true);
                skipButton.setVisible(true);
                skipButton.setDescription(International.getString("Überspringen"));
                break;
            default:
                infotitle = International.getString("Fertig");
                infoLabel.setText(infotitle);
                changes = new Hashtable<String,ChangeItem>();
                closeButton.setText(International.getString("Schließen"));
                fixButton.setVisible(false);
                skipButton.setVisible(false);
                updateFields();
                return;
        }

        infoLabel.setText(step + ". " + infotitle);
        final ProgressMonitor progressMonitor = new ProgressMonitor(this, infotitle,
                International.getString("Durchsuche Fahrtenbuch") + " ...", 0, 100);
        new Thread() {
            public void run() {
            	this.setName("FixLogBookDialog.NextStepThread");
                switch(step) {
                    case 2:
                        findPersonsToFix(progressMonitor);
                        break;
                    case 3:
                        findBoatsToFix(progressMonitor);
                        break;
                    case 4:
                        findDestinationsToFix(progressMonitor);
                        break;
                }
                progressMonitor.setProgress(100);
                if (progressMonitor.isCanceled()) {
                	  SwingUtilities.invokeLater(new Runnable() {
                  	      public void run() {
                          	cancel();
                  	      }
                    	});                    
                } else {
              	  SwingUtilities.invokeLater(new Runnable() {
              	      public void run() {
                          updateFields();
                          validate();
              	      }
                	});                    	
                }
            }
        }.start();
    }

    private String getNeighbour(String name, AutoCompleteList acList, boolean permutations) {
        String neighbour = neighbourCache.get(name);
        if (neighbour != null) {
            return neighbour;
        }
        int radius = (name.length() < 9 ? name.length() / 3 : 3);
        Vector<String> neighbours = acList.getNeighbours(name, radius, (permutations ? 6 : 0));
        if (neighbours != null && neighbours.size() > 0 && neighbours.get(0) != null) {
            neighbourCache.put(name, neighbours.get(0));
            return neighbours.get(0);
        }
        return null;
    }

    public void findPersonsToFix(ProgressMonitor progressMonitor) {
        changes = new Hashtable<String,ChangeItem>();
        try {
            boolean firstLastName = Daten.efaConfig.getValueNameFormatIsFirstNameFirst();
            AutoCompleteList autoCompleteListPersons = new AutoCompleteList();
            long logbookValidFrom = logbook.getValidFrom();
            long logbookInvalidFrom = logbook.getInvalidFrom();
            autoCompleteListPersons.setDataAccess(Daten.project.getPersons(false).data(),
                    logbookValidFrom, logbookInvalidFrom - 1);
            autoCompleteListPersons.update();
            DataKeyIterator it = logbook.data().getStaticIterator();
            long totalWork = logbook.data().getNumberOfRecords();
            long count = 0;
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                if (progressMonitor.isCanceled()) {
                    break;
                }
                progressMonitor.setProgress((int)((count++ * 100) / totalWork));
                LogbookRecord r = logbook.getLogbookRecord(k);
                String name;

                for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                    name = r.getCrewName(i);
                    if (name != null && name.length() > 0 && r.getCrewId(i) == null) {
                        String newname = name;
                        String neighbour;
                        int pos;

                        do { // dummy loop
                            // did we already fix this name before?
                            if (changes.get(name) != null) {
                                break;
                            }

                            // find close matches
                            neighbour = getNeighbour(newname, autoCompleteListPersons, false);
                            if (neighbour != null && neighbour.length() > 0) {
                                newname = neighbour;
                                break;
                            }

                            // if name contains "Guest", make sure it's a "(Guest)" at the end
                            pos = newname.toLowerCase().indexOf(guest.toLowerCase());
                            if (pos >= 0 && !newname.toLowerCase().equals(guest.toLowerCase()) &&
                                    (pos == 0 || !Character.isLetter(newname.charAt(pos-1))) &&
                                    (pos+guest.length() == newname.length() ||
                                        !Character.isLetter(newname.charAt(pos+guest.length())))) {
                                int length = guest.length();
                                if (pos+length < newname.length() && newname.charAt(pos+length) == ')') {
                                    length++;
                                }
                                if (pos > 0 && newname.charAt(pos-1) == '(') {
                                    pos--;
                                    length++;
                                }
                                newname = (pos > 0 ? newname.substring(0, pos) : "") +
                                          (pos+length < newname.length() ?
                                              newname.substring(pos+length) : "");
                                newname = newname.trim() + " (" + guest + ")";
                            }


                            // check space after comma
                            pos = newname.indexOf(",");
                            if (pos >= 0 && pos != newname.indexOf(", ")) {
                                newname = newname.substring(0, pos) + ", "
                                        + (newname.length() > pos + 1 ? newname.substring(pos + 1) : "");
                            }

                            // check space before bracket
                            pos = newname.indexOf("(");
                            if (pos >= 0 && pos - 1 != newname.indexOf(" (")) {
                                newname = newname.substring(0, pos) + " ("
                                        + (newname.length() > pos + 1 ? newname.substring(pos + 1) : "");
                            }

                            // remove spaces where they don't belong
                            newname = EfaUtil.replace(newname, " ,", ",", true);
                            newname = EfaUtil.replace(newname, "( ", "(", true);
                            newname = EfaUtil.replace(newname, " )", ")", true);
                            newname = EfaUtil.replace(newname, "  ", " ", true);

                            // remove other characters that don't belong there

                            while (newname.length() > 0 && !Character.isLetter(newname.charAt(0))) {
                                newname = newname.substring(1);
                            }
                            while (newname.length() > 0) {
                                char c = newname.charAt(newname.length() - 1);
                                if (Character.isLetter(c) || c == '.' || c == ')') {
                                    break;
                                }
                                newname = newname.substring(0, newname.length() - 1);
                            }

                            // fix capitalization
                            boolean inAffix = false;
                            for (pos = 0; pos < newname.length(); pos++) {
                                char c = newname.charAt(pos);
                                if (c == '(') {
                                    inAffix = true;
                                }
                                if (c == ')') {
                                    inAffix = false;
                                }
                                boolean shouldBeUpper =
                                        pos == 0
                                        || !Character.isLetter(newname.charAt(pos - 1));
                                if (shouldBeUpper && !Character.isUpperCase(c)) {
                                    newname = newname.substring(0, pos)
                                            + Character.toUpperCase(c)
                                            + (newname.length() > pos + 1 ? newname.substring(pos + 1) : "");
                                }
                                if (inAffix) {
                                    continue; // in affix don't convert to lowercase
                                }
                                if (!shouldBeUpper && Character.isUpperCase(c)) {
                                    newname = newname.substring(0, pos)
                                            + Character.toLowerCase(c)
                                            + (newname.length() > pos + 1 ? newname.substring(pos + 1) : "");
                                }
                            }

                            // find close matches (again)
                            neighbour = getNeighbour(newname, autoCompleteListPersons, false);
                            if (neighbour != null && neighbour.length() > 0) {
                                newname = neighbour;
                                break;
                            }

                            // fix order of names
                            pos = newname.indexOf(",");
                            if ((firstLastName && pos > 0)
                                    || (!firstLastName && pos < 0)) {
                                String[] parts = PersonRecord.tryGetFirstLastNameAndAffix(newname);
                                String s = PersonRecord.getFullName(parts[0], parts[1], parts[2], firstLastName);
                                if (s != null && s.length() > 0) {
                                    newname = s;
                                }
                            }

                            // fix leading and trailing characters
                            newname = newname.trim();
                            if (newname.startsWith(",")) {
                                newname = (newname.length() > 0 ? newname.substring(1) : "");
                            }
                            if (newname.endsWith(",")) {
                                newname = (newname.length() > 0 ? newname.substring(0, newname.length() - 1) : "");
                            }
                            newname = newname.trim();

                            // find close matches (again)
                            neighbour = getNeighbour(newname, autoCompleteListPersons, false);
                            if (neighbour != null && neighbour.length() > 0) {
                                newname = neighbour;
                                break;
                            }

                            // try whether swapping order of names helps
                            String[] parts = PersonRecord.tryGetFirstLastNameAndAffix(newname);
                            String s = PersonRecord.getFullName(parts[1], parts[2], parts[2], firstLastName);
                            neighbour = getNeighbour(s, autoCompleteListPersons, false);
                            if (neighbour != null && neighbour.length() > 0) {
                                newname = neighbour;
                                break;
                            }

                        } while(false);
                        
                        // final trim
                        newname = newname.trim();

                        // if we've messed something up, set newname back to name
                        if (newname.length() == 0) {
                            newname = name.trim();
                        }

                        // add field to change list
                        ChangeItem ci = changes.get(name);
                        if (ci == null) {

                            ci = new ChangeItem(name, newname,
                                    autoCompleteListPersons.isValidAt(newname, -1));
                        }
                        ci.addField(r, r.getCrewFieldNameId(i));
                        changes.put(name, ci);

                    }

                }

            }
            unifyReplacements(progressMonitor);
            buildFields(autoCompleteListPersons);
        } catch(Exception e) {
            Logger.logdebug(e);
        }
    }

    public void findBoatsToFix(ProgressMonitor progressMonitor) {
        changes = new Hashtable<String, ChangeItem>();
        try {
            AutoCompleteList autoCompleteListBoats = new AutoCompleteList();
            long logbookValidFrom = logbook.getValidFrom();
            long logbookInvalidFrom = logbook.getInvalidFrom();
            autoCompleteListBoats.setDataAccess(Daten.project.getBoats(false).data(),
                    logbookValidFrom, logbookInvalidFrom - 1);
            autoCompleteListBoats.update();
            DataKeyIterator it = logbook.data().getStaticIterator();
            long totalWork = logbook.data().getNumberOfRecords();
            long count = 0;
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                if (progressMonitor.isCanceled()) {
                    break;
                }
                progressMonitor.setProgress((int) ((count++ * 100) / totalWork));
                LogbookRecord r = logbook.getLogbookRecord(k);
                String name = r.getBoatName();
                if (name != null && name.length() > 0 && r.getBoatId() == null) {
                    String newname = name;
                    String neighbour;
                    int pos;

                    do { // dummy loop
                        // did we already fix this name before?
                        if (changes.get(name) != null) {
                            break;
                        }

                        // find close matches
                        neighbour = getNeighbour(newname, autoCompleteListBoats, false);
                        if (neighbour != null && neighbour.length() > 0) {
                            newname = neighbour;
                            break;
                        }

                        // remove double blanks
                        newname = EfaUtil.replace(newname, "  ", " ", true);

                    } while (false);

                    // final trim
                    newname = newname.trim();

                    // if we've messed something up, set newname back to name
                    if (newname.length() == 0) {
                        newname = name.trim();
                    }

                    // add field to change list
                    ChangeItem ci = changes.get(name);
                    if (ci == null) {

                        ci = new ChangeItem(name, newname,
                                autoCompleteListBoats.isValidAt(newname, -1));
                    }
                    ci.addField(r, LogbookRecord.BOATID);
                    changes.put(name, ci);

                }
            }
            unifyReplacements(progressMonitor);
            buildFields(autoCompleteListBoats);
        } catch (Exception e) {
            Logger.logdebug(e);
        }
    }

    public void findDestinationsToFix(ProgressMonitor progressMonitor) {
        changes = new Hashtable<String, ChangeItem>();
        try {
            AutoCompleteList autoCompleteListDestinations = new AutoCompleteList();
            long logbookValidFrom = logbook.getValidFrom();
            long logbookInvalidFrom = logbook.getInvalidFrom();
            autoCompleteListDestinations.setDataAccess(Daten.project.getDestinations(false).data(),
                    logbookValidFrom, logbookInvalidFrom - 1);
            autoCompleteListDestinations.update();
            DataKeyIterator it = logbook.data().getStaticIterator();
            long totalWork = logbook.data().getNumberOfRecords();
            long count = 0;
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                if (progressMonitor.isCanceled()) {
                    break;
                }
                progressMonitor.setProgress((int) ((count++ * 100) / totalWork));
                LogbookRecord r = logbook.getLogbookRecord(k);
                String name = r.getDestinationName();
                if (name != null && name.length() > 0 && r.getDestinationId() == null) {
                    String newname = name;
                    String neighbour;
                    int pos;

                    do { // dummy loop
                        // did we already fix this name before?
                        if (changes.get(name) != null) {
                            break;
                        }

                        // find close matches
                        neighbour = getNeighbour(newname, autoCompleteListDestinations, false);
                        if (neighbour != null && neighbour.length() > 0) {
                            newname = neighbour;
                            break;
                        }

                        // remove double blanks
                        newname = EfaUtil.replace(newname, "  ", " ", true);

                    } while (false);

                    // final trim
                    newname = newname.trim();

                    // if we've messed something up, set newname back to name
                    if (newname.length() == 0) {
                        newname = name.trim();
                    }

                    // add field to change list
                    ChangeItem ci = changes.get(name);
                    if (ci == null) {

                        ci = new ChangeItem(name, newname,
                                autoCompleteListDestinations.isValidAt(newname, -1));
                    }
                    ci.addField(r, LogbookRecord.DESTINATIONID);
                    changes.put(name, ci);

                }
            }
            unifyReplacements(progressMonitor);
            buildFields(autoCompleteListDestinations);
        } catch (Exception e) {
            Logger.logdebug(e);
        }
    }

    public int fixRecords(ProgressDialog progressDialog) {
        int cntOk = 0;
        int cntErr = 0;
        try {
            String[] oldNames = changes.keySet().toArray(new String[0]);
            long totalWork = oldNames.length;
            long count = 0;
            for (String name : oldNames) {
                progressDialog.setCurrentWorkDone((int)((count++ * 100) / totalWork));
                ChangeItem change = changes.get(name);
                change.fixme.getValueFromGui();
                if (change.fixme.getValue()) {
                    change.item.getValueFromGui();
                    ItemTypeStringAutoComplete newValue = (ItemTypeStringAutoComplete)change.item;
                    UUID id = null;
                    String newtext = newValue.getValue();
                    if (newValue.isKnown()) {
                        id = (UUID)newValue.getId(newtext);
                    }
                    for (int i = 0; i < change.records.size(); i++) {
                        LogbookRecord r = change.records.get(i);
                        // since we might already have updated r, we need to first fetch the current
                        // version of the record so that we don't get conflicts with the change count
                        r = logbook.getLogbookRecord(r.getKey());
                        try {
                            String field = change.fields.get(i);
                            String[] fields = r.getEquivalentFields(field);
                            if (id != null) {
                                r.setLogbookField(fields[0], id);
                                r.setLogbookField(fields[1], null);
                            } else {
                                r.setLogbookField(fields[1], newtext);
                            }
                            logbook.data().update(r);
                            cntOk++;
                        } catch(Exception eupdate) {
                            progressDialog.logInfo("ERROR: " +
                                    LogString.operationFailed(International.getMessage("Korrektur von {oldname} nach {newname} in Eintrag {entry}",
                                        change.oldValue, (id != null ? id.toString() : newtext), r.getEntryId().toString()),
                                        eupdate.getMessage()) + "\n");
                            cntErr++;
                        }
                    }
                }
            }

        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return cntOk;
    }

    private void unifyReplacements(ProgressMonitor progressMonitor) {
        progressMonitor.setNote(International.getString("Nachverarbeitung") + " ...");
        progressMonitor.setProgress(0);

        String[] oldNames = changes.keySet().toArray(new String[0]);
        long totalWork = oldNames.length;

        // get frequency of all new names
        Hashtable<String,Integer> newNamesHash = new Hashtable<String,Integer>();
        for (String name : oldNames) {
            ChangeItem ci = changes.get(name);
            if (ci.isKnown()) {
                continue;
            }
            Integer freq = newNamesHash.get(ci.newValue);
            if (freq == null) {
                newNamesHash.put(ci.newValue, ci.records.size());
            } else {
                newNamesHash.put(ci.newValue, freq.intValue() + ci.records.size());
            }
        }

        // sort NewNames in ascending order (sorted by frequency)
        ArrayList<Map.Entry<String, Integer>> newNames = new ArrayList(newNamesHash.entrySet());
        Collections.sort(newNames, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        // find less frequent names to replace by more common names
        Hashtable<String,String> replacements = new Hashtable<String,String>();
        long count = 0;
        for (int i=newNames.size()-1; i> 0; i--) {
            progressMonitor.setProgress((int)((count++ * 100) / totalWork));
            AutoCompleteList acList = new AutoCompleteList();
            acList.add(newNames.get(i).getKey(), null, true, null);
            for (int j=0; j<i; j++) {
                String name = newNames.get(j).getKey();
                if (replacements.containsKey(name)) {
                    continue; // we already found a more popular replacement
                }
                String neighbour = getNeighbour(name, acList, true);
                if (neighbour != null && !name.equals(neighbour)) {
                    replacements.put(name, neighbour);
                }
            }
        }

        // apply all replacements
        for (String name : oldNames) {
            ChangeItem ci = changes.get(name);
            String repl = replacements.get(ci.newValue);
            if (repl != null) {
                ci.newValue = repl;
            }
        }
    }

    private void buildFields(AutoCompleteList autoCompleteList) {
        String[] oldNames = changes.keySet().toArray(new String[0]);
        Arrays.sort(oldNames);
        for (String name : oldNames) {
            ChangeItem change = changes.get(name);

            ItemTypeBoolean fixme = new ItemTypeBoolean("CHECKBOX:" + name,
                    !change.oldValue.equals(change.newValue),
                    IItemType.TYPE_PUBLIC, "", change.oldValue);
            fixme.setFieldGrid(1, -1, -1);

            ItemTypeLabel labelEntry = new ItemTypeLabel("LABEL1:" + name,
                    IItemType.TYPE_PUBLIC, "", change.getLogbookRecordsString());
            labelEntry.setFieldGrid(1, -1, -1);
            labelEntry.setPadding(10, 0, 0, 0);

            ItemTypeLabel labelCount = new ItemTypeLabel("LABEL2:" + name,
                    IItemType.TYPE_PUBLIC, "", change.getNumberOfEntries());
            labelCount.setFieldGrid(1, -1, -1);
            labelCount.setPadding(10, 0, 0, 0);

            ItemTypeStringAutoComplete textfield = new ItemTypeStringAutoComplete("VALUE:" + name,
                                    change.newValue,
                                    IItemType.TYPE_PUBLIC, "", International.getString("neuer Wert"),
                                    true, autoCompleteList);
            textfield.setPadding(10, 0, 0, 0);
            textfield.setReferenceObject(change);
            textfield.registerItemListener(this);

            change.setGuiFields(fixme, labelEntry, labelCount, textfield);
        }
    }

    private void fixSelectedItems() {
        if (step > LAST_STEP) {
            return;
        }
        ProgressTask task = new ProgressTask() {
            String msg;

            public int getAbsoluteWork() {
                return 100;
            }

            public String getSuccessfullyDoneMessage() {
                return msg;
            }

            public void run() {
                setRunning(true);
                this.logInfo(International.getString("Korrekturen werden durchgeführt ..."));
                int cnt = 0;
                if (step <= LAST_STEP) {
                    cnt = fixRecords(progressDialog);
                    msg = International.getMessage("{count} Einträge erfolgreich korrigiert.", cnt);
                }
                nextStep();
                setDone();
            }

            public void runTask(ProgressDialog progressDialog) {
                this.progressDialog = progressDialog;
                start();
            }

        };

        ProgressDialog progressDialog = new ProgressDialog(this, International.getString("Einträge korrigieren"), task, false);
        task.setProgressDialog(progressDialog, false);
        task.start();
        progressDialog.showDialog();
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType.getName().equals(skipButton.getName()) &&
            event.getID() == ActionEvent.ACTION_PERFORMED) {
            nextStep();
            return;
        }
        if (itemType.getName().equals(fixButton.getName()) &&
            event.getID() == ActionEvent.ACTION_PERFORMED) {
            fixSelectedItems();
            return;
        }
        Object ref = itemType.getReferenceObject();
        if (ref != null) {
            ChangeItem change = (ChangeItem)ref;
            if (!change.oldValue.equals(itemType.getValueFromField())) {
                change.fixme.parseAndShowValue(Boolean.toString(true));
            }
        }
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        cancel();
    }

    public boolean cancel() {
        if (step <= LAST_STEP && Dialog.yesNoDialog(International.getString("Abbruch"),
                International.getString("Wirklich abbrechen?")) != Dialog.YES) {
            return false;
        }
        return super.cancel();
    }

    class ChangeItem {
        private ArrayList<LogbookRecord> records = new ArrayList<LogbookRecord>();
        private ArrayList<String> fields = new ArrayList<String>();
        private String oldValue;
        private String newValue;
        private boolean isKnown;
        private ItemTypeBoolean fixme;
        private ItemTypeLabel infoEntry;
        private ItemTypeLabel infoCount;
        private IItemType item;

        public ChangeItem(String oldValue, String newValue, boolean isKnown) {
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.isKnown = isKnown;
        }

        public void addField(LogbookRecord r, String fieldName) {
            records.add(r);
            fields.add(fieldName);
        }

        public void setGuiFields(ItemTypeBoolean fixme, ItemTypeLabel infoEntry, ItemTypeLabel infoCount, IItemType item) {
            this.fixme = fixme;
            this.infoEntry = infoEntry;
            this.infoCount = infoCount;
            this.item = item;
        }

        public boolean isKnown() {
            return this.isKnown;
        }

        public LogbookRecord[] getLogbookRecords() {
            return records.toArray(new LogbookRecord[0]);
        }

        public String[] getFields() {
            return fields.toArray(new String[0]);
        }

        public String getLogbookRecordsString() {
            if (records.size() == 1) {
                return "#" + records.get(0).getEntryId().toString();
            }
            if (records.size() > 1) {
                return "#" + records.get(0).getEntryId().toString() + ", ...";
            }
            return "";
        }

        public String getNumberOfEntries() {
            return "(" + International.getMessage("{count} Einträge", records.size()) + ")";
        }

        public void displayOnGui(JDialog dlg, JPanel panel, int y) {
            if (fixme != null && infoEntry != null && infoCount != null && item != null) {
                fixme.displayOnGui(dlg, panel, 0, y);
                infoEntry.displayOnGui(dlg, panel, 2, y);
                infoCount.displayOnGui(dlg, panel, 4, y);
                item.displayOnGui(dlg, panel, 6, y);
            }
        }

    }

}
