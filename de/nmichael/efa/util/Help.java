/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.util;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.Plugins;
import java.awt.BorderLayout;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Dimension;
import javax.help.*;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JTextArea;

// @i18n complete
public class Help {

    public static final String DEFAULT_TOPIC = "default";

    private static HelpSet helpSet;
    private static HelpBroker helpBroker;

    public static HelpSet getHelpSet() {
        if (helpSet == null) {
            try {
                ClassLoader cl = Help.class.getClassLoader();
                URL helpUrl = HelpSet.findHelpSet(cl, Daten.EFA_HELPSET, International.getLocale());
                helpSet = new HelpSet(null, helpUrl);
            } catch(Exception e) {
                Logger.log(Logger.ERROR, Logger.MSG_HELP_ERRORHELPSET, "Cannot create HelpSet: "+e.toString());
            }
        }
        return helpSet;
    }

    public static HelpBroker getHelpBroker() {
        getHelpSet();
        if (helpBroker == null && helpSet != null) {
            try {
                helpBroker = helpSet.createHelpBroker();
            } catch(Exception e) {
                Logger.log(Logger.ERROR, Logger.MSG_HELP_ERRORHELPBROKER, "Cannot create HelpBroker: "+e.toString());
            }
        }
        return helpBroker;
    }

    public static void showHelp(String[] topics) {
        if (Daten.exceptionTest) {
            String x = null;
            System.out.println(x.length());
        }
        if (topics == null || topics.length == 0) {
            topics = new String[] { DEFAULT_TOPIC };
        }
        try {
            ((javax.help.DefaultHelpBroker) Help.getHelpBroker()).setActivationWindow(Dialog.frameCurrent());
            for (int i = 0; i < topics.length; i++) {
                try {
                    if (Logger.isTraceOn(Logger.TT_HELP, 2)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_HELP_DEBUGHELPTOPICTRYHELP, "Help needed! Trying Help Topic: " + topics[i]);
                    }
                    Help.getHelpBroker().setCurrentID(topics[i]);
                    break;
                } catch (Exception e) {
                    if (Logger.isTraceOn(Logger.TT_HELP, 2)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_HELP_DEBUGHELPTOPICTNOTFOUND, "  -> Help Topic " + topics[i] + " not found.");
                    }
                    if (i+1 == topics.length) {
                        if (Logger.isTraceOn(Logger.TT_HELP, 2)) {
                            Logger.log(Logger.DEBUG, Logger.MSG_HELP_DEBUGHELPTOPICFALLBACK, "Fallback to default Help Topic: " + DEFAULT_TOPIC);
                        }
                        Help.getHelpBroker().setCurrentID(DEFAULT_TOPIC);
                    }
                }
            }
            if (Logger.isTraceOn(Logger.TT_HELP, 1)) {
                Logger.log(Logger.DEBUG, Logger.MSG_HELP_DEBUGHELPTOPICSHOWHELP, "  -> Showing Help: " + Help.getHelpBroker().getCurrentID().getIDString());
            }
            Dimension dim = Dialog.getMaxSize(new Dimension(1000,600));
            Help.getHelpBroker().setSize(dim);
            Help.getHelpBroker().setLocation(Dialog.getLocation(dim, null, null, null));
            Help.getHelpBroker().setDisplayed(true);
            if (Logger.isTraceOn(Logger.TT_HELP, 9)) {
                try {
                    Thread.sleep(300);
                } catch (Exception eintr) {
                }
                JDialog debugDlg = new JDialog();
                debugDlg.setTitle("Help Topics");
                debugDlg.getRootPane().setLayout(new BorderLayout());
                JTextArea text = new JTextArea();
                for (int i = 0; i < topics.length; i++) {
                    text.append("Topic: " + topics[i] + "\n");
                }
                text.append("Showing: " + Help.getHelpBroker().getCurrentID().getIDString() + "\n");
                debugDlg.getRootPane().add(text, BorderLayout.CENTER);
                Dialog.setDlgLocation(debugDlg);
                debugDlg.setPreferredSize(new Dimension(500, 100));
                debugDlg.setMinimumSize(new Dimension(500, 100));
                debugDlg.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
                debugDlg.setVisible(true);
                debugDlg.toFront();
            }
        } catch (Exception e) {
            Dialog.infoDialog(International.getString("Hilfe"),
                              International.getString("Keine Hilfe verfügbar."));
        } catch(NoClassDefFoundError ee) {
            Dialog.error(International.getString("Fehlendes Plugin") + ": " + Plugins.PLUGIN_HELP);
        }
    }

}
