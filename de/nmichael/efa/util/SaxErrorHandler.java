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

import org.xml.sax.*;

// @i18n complete

public class SaxErrorHandler implements ErrorHandler {

    private String filename;
    private int fatalErrors = 0;
    private int errors = 0;
    private int warnings = 0;
    private String lastFatalError = null;
    private String lastError = null;
    private String lastWarning = null;

    public SaxErrorHandler(String filename) {
        this.filename = filename;
    }

    private String getErrorLocation(SAXParseException e) {
        return e.getSystemId() + ":" + e.getLineNumber() + ":" + e.getColumnNumber();
    }

    public void fatalError(SAXParseException e) {
        lastFatalError = "Fatal XML Error on "+filename+" ("+getErrorLocation(e)+"): "+e.toString();
        Logger.log(Logger.ERROR, Logger.MSG_FILE_XMLFALATERROR, lastFatalError);
        fatalErrors++;
    }

    public void error(SAXParseException e) {
        lastError = "XML Error on "+filename+" ("+getErrorLocation(e)+"): "+e.toString();
        Logger.log(Logger.ERROR, Logger.MSG_FILE_XMLERROR, lastError);
        errors++;
    }

    public void warning(SAXParseException e) {
        lastWarning = "XML Warning on "+filename+" ("+getErrorLocation(e)+"): "+e.toString();
        Logger.log(Logger.WARNING, Logger.MSG_FILE_XMLWARNING, lastWarning);
        warnings++;
    }

    public int getNumberOfFatalErrors() {
        return fatalErrors;
    }
    
    public int getNumberOfErrors() {
        return errors;
    }

    public int getNumberOfWarnings() {
        return warnings;
    }

    public String getLastFatalError() {
        return lastFatalError;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLastWarning() {
        return lastWarning;
    }

}
