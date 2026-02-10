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

import de.nmichael.efa.util.EfaUtil;
import java.util.*;

// @i18n complete
public class ZielfahrtFolge {

    public static String SEPARATOR;
    private Vector zff;

    public ZielfahrtFolge() {
        ini();
    }

    public ZielfahrtFolge(String zielfahrten) {
        ini();
        addZielfahrten(zielfahrten);
    }

    private void ini() {
        zff = new Vector();
        if (de.nmichael.efa.efa1.Ziele.zielfahrtSeparatorFahrten.length() == 1) {
            SEPARATOR = de.nmichael.efa.efa1.Ziele.zielfahrtSeparatorFahrten;
        } else {
            SEPARATOR = "/";
        }
    }

    public ZielfahrtFolge addZielfahrten(String zielfahrten) {
        if (zielfahrten == null) {
            return this;
        }

        Vector zf = EfaUtil.split(zielfahrten, SEPARATOR.charAt(0));
        for (int i = 0; i < zf.size(); i++) {
            Zielfahrt z = new Zielfahrt();
            z.setBereich((String) zf.get(i));
            zff.add(z);
        }

        return this;
    }

    public ZielfahrtFolge addZielfahrten(ZielfahrtFolge zielfahrten) {
        if (zielfahrten != null && zielfahrten.getAnzZielfahrten() > 0) {
            for (int i = 0; i < zielfahrten.getAnzZielfahrten(); i++) {
                if (zielfahrten.getZielfahrt(i).getAnzBereiche() > 0) {
                    zff.add(zielfahrten.getZielfahrt(i));
                }
            }
        }
        return this;
    }

    public ZielfahrtFolge addZielfahrt(Zielfahrt zielfahrt) {
        if (zielfahrt != null && zielfahrt.getAnzBereiche() > 0) {
            zff.add(zielfahrt);
        }
        return this;
    }

    public String toString() {
        if (zff == null || zff.size() == 0) {
            return "";
        }
        String s = "";
        for (int i = 0; i < zff.size(); i++) {
            Zielfahrt zf = (Zielfahrt) zff.get(i);
            s += (zf != null ? zf.getBereiche() : "") + (i + 1 < zff.size() ? SEPARATOR : "");
        }
        return s;
    }

    public int getAnzZielfahrten() {
        return zff.size();
    }

    public Zielfahrt getZielfahrt(int i) {
        if (i < 0 || i >= zff.size()) {
            return null;
        }
        return (Zielfahrt) zff.get(i);
    }

    private int[] getZielbereichHaeufigkeit(Vector zff) {
        int[] c = new int[Zielfahrt.ANZ_ZIELBEREICHE + 1]; // 0 ist Dummy-Element
        Arrays.fill(c, 0);
        if (zff.size() == 0) {
            return c;
        }
        for (int i = 0; i < zff.size(); i++) {
            Zielfahrt zf = (Zielfahrt) zff.get(i);
            for (int j = 1; j <= Zielfahrt.ANZ_ZIELBEREICHE; j++) {
                if (zf.isErreicht(j)) {
                    c[j]++;
                }
            }
        }
        return c;
    }

    private int getSeltensterZielbereich(Vector zff, boolean[] erledigt) {
        int[] c = getZielbereichHaeufigkeit(zff);
        int min = 9999;
        int minBereich = -1;
        for (int i = 1; i <= Zielfahrt.ANZ_ZIELBEREICHE; i++) {
            if (c[i] > 0 && c[i] < min && !erledigt[i]) {
                min = c[i];
                minBereich = i;
            }
        }
        return minBereich;
    }

    public void reduceToMinimun() {
        if (zff.size() == 0) {
            return;
        }

        Vector zffAlt = new Vector();
        zffAlt.addAll(zff);
        Vector zffNeu = new Vector();

        boolean[] erledigt = new boolean[Zielfahrt.ANZ_ZIELBEREICHE + 1]; // 0 ist Dummy-Element
        Arrays.fill(erledigt, false);
        int minBereich;
        while ((minBereich = getSeltensterZielbereich(zffAlt, erledigt)) > 0) {
            int fahrt;
            for (fahrt = 0; fahrt < zffAlt.size(); fahrt++) {
                if (((Zielfahrt) zffAlt.get(fahrt)).isErreicht(minBereich)) {
                    zffNeu.add(zffAlt.get(fahrt));
                    zffAlt.remove(fahrt);
                    erledigt[minBereich] = true;
                    break;
                }
            }
        }

        zff = zffNeu;
    }

    public String getUniqueAres() {
        boolean[] erreicht = new boolean[Zielfahrt.ANZ_ZIELBEREICHE + 1]; // 0 ist Dummy-Element
        Arrays.fill(erreicht, false);
        for (int fahrt = 0; fahrt < zff.size(); fahrt++) {
            Zielfahrt z = (Zielfahrt) zff.get(fahrt);
            for (int b = 1; b < erreicht.length; b++) {
                if (z.isErreicht(b)) {
                    erreicht[b] = true;
                }
            }
        }
        StringBuilder s = new StringBuilder();
        for (int b = 1; b < erreicht.length; b++) {
            if (erreicht[b]) {
                s.append( (s.length() > 0 ? ";" : "") + Integer.toString(b));
            }
        }
        return s.toString();
    }

    public int findZielbereich(int zielbereich) {
        for (int i=0; i<getAnzZielfahrten(); i++) {
            Zielfahrt z = getZielfahrt(i);
            if (z.contains(zielbereich)) {
                return i;
            }
        }
        return -1;
    }

}
