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
import java.io.OutputStream;

public abstract class XMLWriter {

    private OutputStream out;
    private int indent;

    protected void writeHeader(OutputStream out) throws Exception {
        this.out = out;
        this.indent = 0;
        write("<?xml version=\"1.0\" encoding=\"" + Daten.ENCODING_UTF + "\"?>\n");
    }

    private String space() {
        StringBuilder space = new StringBuilder();
        for (int i=0; i<indent; i++) {
            space.append("  ");
        }
        return space.toString();
    }

    protected void write(String s) throws Exception {
        out.write(s.getBytes(Daten.ENCODING_UTF));
    }

    protected void xmltagStart(String tag) throws Exception {
        write(space() + "<" + tag + ">\n");
        indent++;
    }

    protected void xmltagEnd(String tag) throws Exception {
        indent--;
        write(space() + "</" + tag + ">\n");
    }

    protected void xmltag(String tag, String value) throws Exception {
        write(space() +
                "<" + tag + ">" +
                EfaUtil.escapeXml(value) +
                "</" + tag + ">\n");
    }

}
