/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.core;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.data.Clubwork;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.Waters;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.gui.AdminDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import javax.swing.JDialog;

/**
 * Task that is executed whenever admin logs in. This task runs in the
 * background whenever an admin logs in interavtively (that is, in GUI mode).
 * After being started, it sleeps for 1 second, then waits for a maximum of 10
 * seconds until a project has successfully been opened. Once that is the case,
 * it runs the configured actions. Once all actions have completed, this task
 * completes. It does NOT run in the background indefinitely. In situations
 * where opening of a project fails or takes very long, this task may not
 * execute any actions at all.
 */
public class AdminTask extends Thread {

    private static AdminTask task;
    private AdminRecord admin;
    private JDialog parent;

    public AdminTask(AdminRecord admin, JDialog parent) {
        this.admin = admin;
        this.parent = parent;
    }

    private void runActions() {
        Logger.log(Logger.DEBUG, Logger.MSG_CORE_ADMINTASK, "running AdminTask ...");
        // Actions to be implemented here!
        // For each action, check whether admin has necessary permissions.
        if (admin.isAllowedUpdateEfa()) {
            checkForJavaVersion();
        }
        if (admin.isAllowedAdministerProjectClubwork()) {
            checkForClubworkCarryOver();
        }
        if (admin.isAllowedEditDestinations()) {
            checkForUpdateWaters();
        }
    }

    public void run() {
        try {
        	this.setName("AdminTask");
            boolean ready = false;
            for (int tries = 0; tries < 11; tries++) {
                // always start task with 1000 ms delay
                try {
                    Thread.sleep(1000);
                } catch (Exception eignore) {
                }
                if (Daten.project != null && Daten.project.isOpen()
                        && !Daten.project.isInOpeningProject()) {
                    ready = true;
                    break;
                }
            }
            if (ready) {
                runActions();
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        task = null;
    }

    public static void startAdminTask(AdminRecord admin, JDialog dlg) {
        if (!Daten.isGuiAppl()) {
            return;
        }
        if (admin == null) {
            return;
        }
        if (task != null && task.isAlive()) {
            return;
        }
        task = new AdminTask(admin, dlg);
        task.start();
    }

    private void checkForJavaVersion() {
        int javaVersionInt = Integer.parseInt(Daten.javaVersion.split("\\.")[0]);
        if ((javaVersionInt == 1) && (Daten.javaVersion.split("\\.").length > 2))   // Windows calls java 8 java 1.8_xxx
            javaVersionInt = Integer.parseInt(Daten.javaVersion.split("\\.")[1]);
        if (javaVersionInt < Daten.REQUIRED_JAVA_VERSION) {
            long lastCheck = System.currentTimeMillis() - Daten.efaConfig.getValueJavaVersionLastCheck();
            if (lastCheck > 7 * 24 * 60 * 60 * 1000) {
                Dialog.infoDialog(International.getString("Java Update erforderlich"),
                    International.getMessage("Die verwendete Java Version {version} ist zu alt und wird demnächst nicht mehr unterstützt.", Daten.javaVersion) + "\n" +
                    International.getMessage("Um efa auch in Zukunft weiter verwenden zu können, installiere bitte Java Version {version} oder neuer.", Daten.REQUIRED_JAVA_VERSION) + "\n" +
                    International.getMessage("Weitere Informationen auf {url}", "http://efa.nmichael.de/download.html"));
                Daten.efaConfig.setValueJavaVersionLastCheck(System.currentTimeMillis());
            }
        }
    }

    private void checkForClubworkCarryOver() {
        String[] clubworkNames = Daten.project.getAllClubworkNames();
        if (clubworkNames != null && clubworkNames.length > 0) {
            Clubwork clubwork = Daten.project.getCurrentClubwork();
            if (clubwork != null) {
	            if (clubwork.getProjectRecord().getClubworkCarryOverDone() == false) {
	                if (clubwork != null && clubwork.isOpen()) {
	                    DataTypeDate date = clubwork.getEndDate();
	                    if (date != null && DataTypeDate.today().isAfterOrEqual(date)) {
	                        int res = Dialog.auswahlDialog(International.getString("Übertrag berechnen"),
	                                International.getMessage("Möchtest Du den Übertrag für das Vereinsarbeitsbuch '{record}' wirklich berechnen?",
	                                        clubwork.getName()),
	                                International.getString("ja"),
	                                International.getString("nein") + 
	                                        " (" + International.getString("nie") + ")",
	                                International.getString("nein") + 
	                                        " (" + International.getString("nicht jetzt") + ")"
	                        );
	                        if (res == 0 /* yes */ || res == 1 /* no */) {
	                            ProjectRecord rec = clubwork.getProjectRecord();
	                            rec.setClubworkCarryOverDone(true);
	                            try {
	                                Daten.project.data().update(rec);
	                            } catch(Exception e) {
	                                Dialog.error(e.toString());
	                            }
	                        }
	                        if (res == 0 /* yes */) {
	                            clubwork.doCarryOver(1, parent);
	                            ((AdminDialog) parent).updateInfos();
	                        }
	                    }
	                } else {
	                    Dialog.error(International.getString("Kein Vereinsarbeitsbuch geöffnet.")
	                            + International.getMessage("Berechnen des {verb}s nicht möglich.", International.getString("Übertrag")));
	                }
	            } else {
	                /*
	                // already done, no error message needed?!
	                Dialog.error(International.getString("Kein Vereinsarbeitsbuch geöffnet.")
	                        + International.getMessage("Berechnen des {verb}s nicht möglich.", International.getString("Übertrag")));
	                */
	            }
            }
        }
    }
    
    private void checkForUpdateWaters() {
        if (Daten.project != null && Daten.project.isOpen()
                && Waters.getResourceTemplate(International.getLanguageID()) != null
                && Waters.hasWaterTemplateChanged(International.getLanguageID())) {
        	
        	String combinedMessage = International.getMessage("Der vom {author} erstellte Gewässerkatalog wurde überarbeitet.",
                    Waters.getWaterTemplateAuthor(International.getLanguageID())) ;
        			
        	if (Daten.project.getIsProjectStorageTypeEfaCloud()) {
        		combinedMessage += "\n\n"
        				+ International.getString("ACHTUNG")+"!\n"
        				+ International.getString("Bei einem EfaCloud-Projekt sollte diese Funktion nur auf einer einzigen Instanz durchgeführt werden. Die anderen Instanzen erhalten die Gewässer automatisch über die efaCloud-Synchronisation.");
        	}
        	
        	combinedMessage += "\n\n" + International.getString("Möchstest Du die Gewässerliste jetzt aktualisieren?");
        			
            if (Dialog.yesNoDialog(International.getString("Gewässerkatalog aktualisiert"),combinedMessage) == Dialog.YES) {
                int count = Daten.project.getWaters(false).addAllWatersFromTemplate(International.getLanguageID());
                if (count > 0) {
                    Dialog.infoDialog(International.getMessage("{count} Gewässer aus Gewässerkatalog erfolgreich hinzugefügt oder aktualisiert.",
                            count));
                } else {
                    Dialog.infoDialog(International.getString("Alle Gewässer aus dem Gewässerkatalog sind bereits vorhanden (keine neuen hinzugefügt)."));
                }
            } else {
                Waters.setWaterTemplateUnchanged(International.getLanguageID());
            }
        }
    }
    
}
