/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.statistics;

import java.util.*;
import de.nmichael.efa.util.EfaUtil;

// @i18n complete
public class StatOutputLines {

    public static final int FONT_NORMAL = 0;
    public static final int FONT_BOLD = 1;
    private Vector<String> lines;

    public StatOutputLines() {
        lines = new Vector<String>();
    }

    public void addLine(String s, int columns, int font) {
        if (columns < 1 || columns > 9) {
            columns = 1;
        }
        if (font < 0 || font > 9) {
            font = FONT_NORMAL;
        }
        lines.add("#" + columns + "#" + font + "#" + s);
    }

    public int size() {
        return lines.size();
    }

    public String getLine(int i) {
        if (i < 0 || i >= lines.size()) {
            return null;
        }
        String s = (String) lines.get(i);
        return s.substring(5, s.length());
    }

    public int getLineColumns(int i) {
        if (i < 0 || i >= lines.size()) {
            return 1;
        }
        String s = (String) lines.get(i);
        return EfaUtil.string2int(s.substring(1, 2), 1);
    }

    public int getLineFont(int i) {
        if (i < 0 || i >= lines.size()) {
            return FONT_NORMAL;
        }
        String s = (String) lines.get(i);
        return EfaUtil.string2int(s.substring(3, 4), FONT_NORMAL);
    }
}
