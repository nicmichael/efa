/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.ex;

import de.nmichael.efa.util.Dialog;

public class EfaModifyException extends EfaException {

    public EfaModifyException(String key, String msg, StackTraceElement[] stack) {
        super(key, msg, stack);
    }

    public void displayMessage() {
        Dialog.error(msg);
    }

}
