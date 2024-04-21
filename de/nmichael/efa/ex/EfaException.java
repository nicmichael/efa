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

import de.nmichael.efa.util.Logger;

public class EfaException extends Exception {

    String key;
    String msg;
    StackTraceElement[] stack;

    public EfaException(String key, String msg, StackTraceElement[] stack) {
        this.key = key;
        this.msg = msg;
        this.stack = stack;
    }

    public void log() {
        Logger.log(Logger.ERROR, key, msg);
    }

    public String getStackTraceAsString() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; stack != null && i < stack.length; i++) {
            s.append(" -> " + stack[i].toString());
        }
        return s.toString();
    }

    public String toString() {
        return getClass().getCanonicalName()+": " + key + " (" + msg + ")";
        //return getClass().getCanonicalName()+": " + key + " (" + msg + ") at " + getStackTraceAsString();
    }

    public String getMessage() {
        return msg;
    }

}
