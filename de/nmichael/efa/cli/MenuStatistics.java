/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.cli;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.*;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.statistics.StatisticTask;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Email;
import de.nmichael.efa.util.Logger;
import java.util.Hashtable;
import java.util.Stack;
import java.util.UUID;

public class MenuStatistics extends MenuData {

    public static final String CMD_CREATE  = "create";

    public MenuStatistics(CLI cli) {
        super(cli);
        this.storageObject = cli.getPersistence(Statistics.class, 
                Project.STORAGEOBJECT_STATISTICS, Statistics.DATATYPE);
        this.storageObjectDescription = "statistics";
    }

    public void printHelpContext() {
        printUsage(CMD_CREATE,  "[-all|-status=name|-name=name] [name|index]", "create statistic");
        super.printHelpContext();
    }

    protected int create(String args) {
        Hashtable<String,String> options = getOptionsFromArgs(args);
        args = removeOptionsFromArgs(args);
        StatisticsRecord sr = (StatisticsRecord) getRecordFromArgs(args);
        if (sr == null) {
            sr = (StatisticsRecord) getRecordFromArgs(args);
        }
        if (sr == null) {
            cli.logerr("Record '"+args+"' not found.");
            return CLI.RC_COMMAND_FAILED;
        }
        
        boolean outputOk = false;
        switch (sr.getOutputTypeEnum()) {
            case html:
            case csv:
            case xml:
            case pdf:
                outputOk = true;
                break;
            default:
            	outputOk = false;
        }
        if (!outputOk) {
            cli.logerr("Cannot create statistic with output type '"+sr.getOutputTypeDescription()+"' in CLI.");
            return CLI.RC_COMMAND_FAILED;
        }

        if (sr.getFilterPromptPerson() ||
            sr.getFilterPromptBoat() ||
            sr.getFilterPromptGroup()) {
            int countPrompt = 0;
            countPrompt += (sr.getFilterPromptPerson() ? 1 : 0);
            countPrompt += (sr.getFilterPromptBoat() ? 1 : 0);
            countPrompt += (sr.getFilterPromptGroup() ? 1 : 0);
            if (countPrompt > 1) {
                cli.logerr("Cannot create statistic with more than one individual filters.");
                return CLI.RC_COMMAND_FAILED;
            }
            if (options.get("all") != null) {
                return createBatchStatistic(sr, null);
            }
            if (options.get("status") != null) {
                return createBatchStatistic(sr, options.get("status"));
            }
            if (options.get("name") != null) {
                if (setFilter(sr, null, options.get("name"))) {
                    return createStatistic(sr, options.get("name"));
                } else {
                    cli.logerr("Filter \"" + options.get("name") + "\" not found.");
                    return CLI.RC_COMMAND_FAILED;
                }
            }
            String name = cli.promptForInput("Name for Filter");
            if (name != null && name.trim().length() > 0) {
                if (setFilter(sr, null, name)) {
                    return createStatistic(sr, name);
                } else {
                    cli.logerr("Filter \"" + name + "\" not found.");
                    return CLI.RC_COMMAND_FAILED;
                }
            }
            cli.logerr("You have to specify a name for a statistic with individual filter.");
            return CLI.RC_COMMAND_FAILED;
        }

        return createStatistic(sr, null);
    }

    private boolean setFilter(StatisticsRecord sr, UUID id, String name) {
        try {
            if (sr.getFilterPromptPerson()) {
                if (name != null) {
                    PersonRecord p = Daten.project.getPersons(false).getPerson(name, System.currentTimeMillis());
                    if (p == null) { // try again with "_" replaced by " "
                        p = Daten.project.getPersons(false).getPerson(EfaUtil.replace(name, "_", " ", true), System.currentTimeMillis());
                    }
                    if (p != null) {
                        id = p.getId();
                    }
                }
                if (id != null) {
                    sr.setFilterByPersonId(id);
                } else {
                    sr.setFilterByPersonText(name);
                }
            }
            if (sr.getFilterPromptBoat()) {
                if (name != null) {
                    BoatRecord b = Daten.project.getBoats(false).getBoat(name, System.currentTimeMillis());
                    if (b == null) { // try again with "_" replaced by " "
                        b = Daten.project.getBoats(false).getBoat(EfaUtil.replace(name, "_", " ", true), System.currentTimeMillis());
                    }
                    if (b != null) {
                        id = b.getId();
                    }
                }
                if (id != null) {
                    sr.setFilterByBoatId(id);
                } else {
                    sr.setFilterByBoatText(name);
                }
            }
            if (sr.getFilterPromptGroup()) {
                if (name != null) {
                    GroupRecord g = Daten.project.getGroups(false).findGroupRecord(name, System.currentTimeMillis());
                    if (g == null) { // try again with "_" replaced by " "
                        g = Daten.project.getGroups(false).findGroupRecord(EfaUtil.replace(name, "_", " ", true), System.currentTimeMillis());
                    }
                    if (g != null) {
                        id = g.getId();
                    }
                }
                if (id != null) {
                    sr.setFilterByGroupId(id);
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
            return false;
        }
        return true;
    }

    private int createStatistic(StatisticsRecord sr, String filter) {
        try {
            cli.loginfo("Creating Statistic " + sr.getQualifiedName() +
                    (filter != null ? " for " + filter : "") +
                    " ...");
            StatisticTask.createStatisticsTask(null, null, new StatisticsRecord[] { sr }, cli.getAdminRecord());
            cli.loginfo("Done.");
            return CLI.RC_OK;
        } catch(Exception e) {
            cli.loginfo("Error creating Statistic: " + e.toString());
            Logger.logdebug(e);
            return CLI.RC_COMMAND_FAILED;
        }
    }

    private int createBatchStatistic(StatisticsRecord sr, String status) {
        long now = System.currentTimeMillis();
        try {
            sr.prepareStatisticSettings(cli.getAdminRecord());
            if (sr.getFilterPromptPerson()) {
                Persons persons = Daten.project.getPersons(false);
                DataKeyIterator it = persons.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    PersonRecord p = (PersonRecord)persons.data().get(k);
                    if (!p.isValidAt(now)) {
                        continue;
                    }
                    if (status != null && !status.equals(p.getStatusName())) {
                        continue;
                    }
                    if (sr.sEmailAddresses != null && sr.sEmailAddresses.equals(Email.EMAIL_INDIVIDUAL) &&
                        (p.getEmail() == null || p.getEmail().length() == 0)) {
                        continue;
                    }
                    sr.setFilterByPersonId(p.getId());
                    updateOutputFileName(sr, p.getQualifiedName());
                    createStatistic(sr, p.getQualifiedName());
                }
            }
            if (sr.getFilterPromptBoat()) {
                Boats boats = Daten.project.getBoats(false);
                DataKeyIterator it = boats.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    BoatRecord b = (BoatRecord)boats.data().get(k);
                    if (!b.isValidAt(now)) {
                        continue;
                    }
                    sr.setFilterByBoatId(b.getId());
                    updateOutputFileName(sr, b.getQualifiedName());
                    createStatistic(sr, b.getQualifiedName());
                }
            }
            if (sr.getFilterPromptGroup()) {
                Groups groups = Daten.project.getGroups(false);
                DataKeyIterator it = groups.data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    GroupRecord g = (GroupRecord)groups.data().get(k);
                    if (!g.isValidAt(now)) {
                        continue;
                    }
                    sr.setFilterByGroupId(g.getId());
                    updateOutputFileName(sr, g.getQualifiedName());
                    createStatistic(sr, g.getQualifiedName());
                }
            }
            return CLI.RC_OK;
        } catch(Exception e) {
            cli.loginfo("Error creating Statistic: " + e.toString());
            Logger.logdebug(e);
            return CLI.RC_COMMAND_FAILED;
        }
    }

    private void updateOutputFileName(StatisticsRecord sr, String name) {
        if (sr.sEmailAddresses == null || sr.sEmailAddresses.length() == 0) {
            sr.setOutputFile(sr.sOutputDir + Daten.fileSep
                    + EfaUtil.replaceNotAllowedCharacters(name,
                    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789äöüÄÖÜß-_",
                    null,
                    "_") + sr.getOutputExtension());
        }
    }

    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        if (cmd.equalsIgnoreCase(CMD_CREATE)) {
            return create(args);
        }
        return super.runCommand(menuStack, cmd, args);
    }

}
