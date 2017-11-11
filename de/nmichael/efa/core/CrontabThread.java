/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core;

import de.nmichael.efa.Daten;
import de.nmichael.efa.cli.CLI;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeCronEntry;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class CrontabThread extends Thread {

    public static final String CRONTAB_THREAD_NAME = "CronTabThread";
    public static final String CRONJOB_THREAD_NAME = "CronJobThread";

    public void run() {
        setName(CRONTAB_THREAD_NAME);
        while(true) {
            try {
                Calendar cal = new GregorianCalendar();
                int second = cal.get(Calendar.SECOND);
                try {
                    // sleep until second 1 of the next minute
                    Thread.sleep((61-second) * 1000);
                } catch(InterruptedException ie) {
                }
                if (Daten.efaConfig != null) {
                    ItemTypeItemList crontab = Daten.efaConfig.getCrontabItems();
                    for (int i=0; crontab != null && i<crontab.size(); i++) {
                        IItemType[] items = crontab.getItems(i);
                        if (items != null && items.length > 0) {
                            ItemTypeCronEntry cronJob = (ItemTypeCronEntry)items[0];
                            if (cronJob != null && cronJob.isNowSelected()) {
                                new Cronjob(cronJob).start();
                            }
                        }
                    }
                }
            } catch(Exception e) {
                Logger.logdebug(e);
            }
        }
    }

    class Cronjob extends Thread {

        private ItemTypeCronEntry job;

        public Cronjob(ItemTypeCronEntry job) {
            this.job = job;
        }

        public void run() {
            setName(CRONJOB_THREAD_NAME);
            String cmd = job.getCommand();
            if (cmd == null || cmd.length() == 0) {
                return;
            }
            Logger.log(Logger.INFO, Logger.MSG_CORE_CRONJOB,
                    "CronJob: " + LogString.operationStarted("\"" + cmd + "\""));
            try {
                CLI cli = new CLI();
                int ret = cli.run(cmd);
                
                Logger.log(Logger.INFO, Logger.MSG_CORE_CRONJOB,
                        "CronJob: " + LogString.operationSuccessfullyCompleted("\"" + cmd + "\""));
                return;
            } catch(Exception e) {
                Logger.logdebug(e);
                Logger.log(Logger.INFO, Logger.MSG_CORE_CRONJOB,
                        "CronJob: " + LogString.operationFailed("\"" + cmd + "\"", e.toString()));
            }
            Logger.log(Logger.INFO, Logger.MSG_CORE_CRONJOB,
                    "CronJob: " + LogString.operationFailed("\"" + cmd + "\""));
        }
    }

}
