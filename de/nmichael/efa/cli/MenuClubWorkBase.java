package de.nmichael.efa.cli;

import java.util.Stack;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.Clubwork;

public class MenuClubWorkBase extends MenuData {
    public MenuClubWorkBase(CLI cli) {
        super(cli);
        this.storageObject = cli.getPersistence(Clubwork.class, Daten.project.getCurrentClubworkForCLI(false).getName(), Clubwork.DATATYPE);
        this.storageObjectDescription = "club work from efaBase";
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
