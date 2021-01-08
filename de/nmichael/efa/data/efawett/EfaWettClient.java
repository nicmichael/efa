/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.data.efawett;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.EnterPasswordDialog;
import de.nmichael.efa.util.Dialog;

public class EfaWettClient {

    public static final int VERBAND_DRV = 1;
    public static final int VERBAND_LRVBLN = 2;
    public static final int ACTION_EINSENDEN = 1;
    public static final int ACTION_ABRUFEN = 2;
    public static final int ACTION_QNRLIST = 3;
    private static boolean _reusePasswordForNextRequest = false;
    private static String password = null;

    public static void reusePasswordForNextRequest() {
        _reusePasswordForNextRequest = true;
    }

    public static String makeScriptRequestString(int _verband, int _action, String param1, String param2) {
        if (Daten.project == null || !Daten.project.isOpen()) {
            Dialog.error("Kein Projekt geöffnet!");
            return null;
        }
        if (Daten.wettDefs == null) {
            Dialog.error("Es ist keine Wettbewerbskonfiguration vorhanden.\n"
                    + "Bitte öffne im Menü 'Administration' den Punkt 'Wettbewerbskonfiguration'\n"
                    + "und aktualisiere die Konfigurationsdaten.");
            return null;
        }
        String verbandName = null;
        String verband = null;
        String user = null;
        switch (_verband) {
            case VERBAND_DRV:
                verbandName = "DRV";
                verband = "drv";
                user = Daten.project.getClubGlobalAssociationLogin();
                break;
            case VERBAND_LRVBLN:
                verbandName = "LRV Berlin";
                verband = "lrvbln";
                user = Daten.project.getClubRegionalAssociationLogin();
                break;
        }
        if (verband == null) {
            return null;
        }
        if (user == null || user.length() == 0) {
            Dialog.error("Es ist kein Benutzername zum Abrufen der Daten konfiguriert.\n"
                    + "Bitte trage in den Projekteinstellungen einen Benutzernamen für efaWett ein.");
            return null;
        }

        String action = null;
        String script = null;
        switch (_action) {
            case ACTION_EINSENDEN:
                action = "efa_uploadMeldung";
                if (_verband == VERBAND_DRV) {
                    script = Daten.wettDefs.efw_url_einsenden;
                }
                break;
            case ACTION_ABRUFEN:
                action = "efa_getBestaetigung";
                if (_verband == VERBAND_DRV) {
                    script = Daten.wettDefs.efw_url_abrufen;
                }
                break;
            case ACTION_QNRLIST:
                action = "efa_getAllQnr";
                if (_verband == VERBAND_DRV) {
                    script = Daten.wettDefs.efw_url_abrufen;
                }
                break;
        }
        if (action == null || script == null) {
            return null;
        }
        if (script.length() == 0) {
            Dialog.error("Es ist keine Adresse zum Abrufen der Daten konfiguriert.\n"
                    + "Bitte öffne im Menü 'Administration' den Punkt 'Wettbewerbskonfiguration'\n"
                    + "und aktualisiere die Konfigurationsdaten.");
            return null;
        }

        if (!_reusePasswordForNextRequest) {
            password = null;
        }
        if (password == null) {
            password = EnterPasswordDialog.enterPassword(Dialog.frameCurrent(),
                    "Bitte gib das efaWett-Paßwort für den Benutzernamen '" + user
                    + "' beim " + verbandName + " an:", false);
        }
        if (password == null) {
            return null;
        }
        String s = script + "?verband=" + verband + "&agent=efa&username=" + user + "&password=" + password + "&action=" + action;
        if (param1 != null) {
            s += "&" + param1;
        }
        if (param2 != null) {
            s += "&" + param2;
        }
        _reusePasswordForNextRequest = false;
        return s;
    }
}
