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

import de.nmichael.efa.data.*;
import java.util.Stack;

public class MenuWaters extends MenuData {

    public MenuWaters(CLI cli) {
        super(cli);
        this.storageObject = cli.getPersistence(Waters.class, Project.STORAGEOBJECT_WATERS, Waters.DATATYPE);
        this.storageObjectDescription = "waters";
    }

    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        int ret = super.runCommand(menuStack, cmd, args);
        if (ret < 0) {
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return ret;
        }
    }

}
