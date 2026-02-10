/**
g * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.mail.internet.InternetAddress;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.CrontabThread;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.efa1.DatenFelder;
import de.nmichael.efa.efa1.Synonyme;

// @i18n complete
public class EfaUtil {

    private static final int ZIP_BUFFER = 2048;
    private static java.awt.Container java_awt_Container = new java.awt.Container();

	private static String UMLAUTS 		= "åàáâăäāąćçčèéêęëėěēìíîįīïďđģķĺļłńňñņòóôőõöōøřŕůùúûűüųūýÿšśşťţżžź";
	private static String REPLACEMENT 	= "aaaaaaaaccceeeeeeeeiiiiiiddgklllnnnnoooooooorruuuuuuuuyysssttzzz"; 

    private static String UMLAUTSEXTEND = UMLAUTS + "ßæœ";// those umlauts get translated to two characters

    public static String escapeXml(String str) {
        str = replaceString(str, "&", "&amp;");
        str = replaceString(str, "<", "&lt;");
        str = replaceString(str, ">", "&gt;");
        str = replaceString(str, "\"", "&quot;");
        str = replaceString(str, "'", "&apos;");
        return str;
    }

    public static String escapeHtml(String str) {
        str = replaceString(str, "&", "&amp;");
        str = replaceString(str, "<", "&lt;");
        str = replaceString(str, ">", "&gt;");
        str = replaceString(str, "\"", "&quot;");
        str = replaceString(str, "\u2026","&hellip;");
        return str;
    }

    public static String escapeHtmlWithLinefeed(String str) {
        return replaceString(escapeHtml(str),"\n","<br>");
    }
    

    public static String escapeHtmlGetString(String str) {
        if (str == null) {
            return null;
        }
        str = EfaUtil.replace(str, "Ä", "AE");
        str = EfaUtil.replace(str, "Ö", "Oe");
        str = EfaUtil.replace(str, "Ü", "Ue");
        str = EfaUtil.replace(str, "ä", "ae");
        str = EfaUtil.replace(str, "ö", "oe");
        str = EfaUtil.replace(str, "ü", "ue");
        str = EfaUtil.replace(str, "ß", "ss");
        str = EfaUtil.replaceNotAllowedCharacters(str.trim(),
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.", null, "_");
        return str;
    }

    // from StringW
    public static String replaceString(String text, String repl, String with) {
        return replaceString(text, repl, with, -1);
    }

    /**
     * Replace a string with another string inside a larger string, for
     * the first n values of the search string.
     *
     * @param text String to do search and replace in
     * @param repl String to search for
     * @param with String to replace with
     * @param n    int    values to replace
     *
     * @return String with n values replacEd
     */
    public static String replaceString(String text, String repl, String with, int max) {
        if (text == null) {
            return null;
        }

        StringBuffer buffer = new StringBuffer(text.length());
        int start = 0;
        int end = 0;
        while ((end = text.indexOf(repl, start)) != -1) {
            buffer.append(text.substring(start, end)).append(with);
            start = end + repl.length();

            if (--max == 0) {
                break;
            }
        }
        buffer.append(text.substring(start));

        return buffer.toString();
    }
    
    @Deprecated
    public static String replaceListByList(String string, String searchList, String replaceList) {
        if (searchList.length() != replaceList.length()) {
            return string;
        }
        for (int i=0; i<searchList.length(); i++) {
            char search = searchList.charAt(i);
            int pos;
            while ( (pos = string.indexOf(search)) >= 0 ) {
                string = string.substring(0, pos) + 
                         replaceList.charAt(i) +
                         string.substring(pos+1);
            }
        }
        return string;
    }

    /**
     * Replaces special characters in a string by a replacement.
     * Replacing is done character-by-character, so it is not possible to replace a single character "ä" by a multi-character string "ae". 
	 *
	 * searchList and replaceList MUST be of equal length.
	 *
     * The method is declarated as "fast" as it uses standard java replace function instead of old self-written code.
     * This method is like 15 times faster than the former replaceListByList function, and more suitable for sorting algorithms.
     * 
     * @param strData	String containing characters to be replaced
     * @param searchList  String containing all characters that shall be replaced one-by-one
     * @param replaceList String containing all replacement characters
     * @return String containing all replacements.
     */
    public static String replaceListByListFast(String strData, String searchList, String replaceList) {
        if (searchList.length() != replaceList.length()) {
            return strData;
        }
        for (int i=0; i<searchList.length(); i++) {
        	strData = strData.replace(searchList.charAt(i),replaceList.charAt(i));
        }
        return strData;
    }
    
    /**
     * Replaces all umlauts of western character set (German, French, Spanish, Danish) to a simple latin character, e.g. "ä"->"a".
     * This method can be used for sorting lists in efa or for String comparison.
	 *
	 * The former replaceAllUmlauts was used only for sorting Boatlist/Personlist in efaBths (myStringComperator used this code);
	 * other sort algorithms hat other umlaut handling (Autocompletelist) or even none (Tablesorter)
	 *
     * Today it is used for sorting in the following lists: BoatList/Personlist, AutoCompleteList, spellcheck for autocomplete, tablesorter.
     * 
     * Addendum: 
     * Sorting lists in efa is sort of old-fashioned. It does not yet use collator and locales, but
     * it replaces common umlauts to get some better sorting of boats, persons which have umlauts in their names.
     * This is subject to further refactoring.
	 *
     * @param data
     * @return
     */
    public static String replaceAllUmlautsLowerCaseFast(String data) {
	    String s1 = data.toLowerCase();
	    s1 = EfaUtil.replaceListByListFast(s1, UMLAUTS, REPLACEMENT);
	
	    if (s1.indexOf("ß") >= 0) {
	        s1 = EfaUtil.replace(s1, "ß", "ss", true);
	    }
	    
	    if (s1.indexOf("æ") >= 0) {
	        s1 = EfaUtil.replace(s1, "æ", "ae", true);
	    }

	    if (s1.indexOf("œ") >= 0) {
	        s1 = EfaUtil.replace(s1, "œ", "oe", true);
	    }
	    
	    
	    return s1;
    }    
    
    public static boolean containsUmlaut(String data) {
    	return data.toLowerCase().matches(".*["+UMLAUTSEXTEND+"]+.*");
    }
    
    public static String getString(String s, int length) {
        while (s.length() < length) {
            s = s + " ";
        }
        return s;
    }

    public static String getStringPadLeft(String s, int length) {
        while (s.length() < length) {
            s = " " + s;
        }
        return s;
    }

    public static String getHHMMstring(long minutes) {
        long hh = minutes / 60;
        long mm = minutes % 60;
        return hh + ":" + long2String(mm, 2);
    }

    // überprüfen, ob "c" ein "echtes" Zeichen (Buchstabe, Ziffer, Whitespace) ist
    public static boolean isRealChar(KeyEvent e) {
        char c = e.getKeyChar();
        return (Character.isLetter(c) || Character.isDigit(c) || Character.isWhitespace(c)
                || c == '.' || c == '-' || c == '+' || c == '(' || c == ')' || c == '!' || c == '"' || c == '§' || c == '$' || c == '%'
                || c == '&' || c == '/' || c == '=' || c == '?' || c == ';' || c == ':' || c == ',' || c == '_' || c == '#' || c == '*'
                || c == '|' || c == '>' || c == '<');
    }

    // überprüfen, ob angegebenes Datum ein Wochenende ist
    public static boolean woEnd(String s) {
        SimpleDateFormat df = new SimpleDateFormat();
        Calendar cal = new GregorianCalendar();
        int tag;
        try {
            Date d = df.parse(s);
            cal.setTime(d);
            if ((tag = cal.get(Calendar.DAY_OF_WEEK)) == Calendar.SATURDAY || tag == Calendar.SUNDAY) {
                return true;
            }
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_GENERIC_ERROR, "Parse error");
        }
        return false;
    }

    // Integer-Division mit einer Nachf,ae,-Stelle (und Rundung)
    public static float div(int a, int b) {
        if (b == 0) {
            return 0;
        }
        return ((float) intdiv(a * 10, b)) / 10;
    }

    // Integer-Division mit Rundung
    public static int intdiv(int a, int b) {
        if (b == 0) {
            return 0;
        }
        int c = (a * 10) / b;
        if (c % 10 >= 5) {
            c = c + 10 - (c % 10);
        }
        return c / 10;
    }

    // float-Division mit "div 0" Sicherung
    public static float fdiv(int a, int b) {
        if (b == 0) {
            return 0;
        }
        return ((float) a / (float) b);
    }

    // Einzelne Werte aus Datumsstring ermitteln, wobei tt, mm und yy die
    // Vorgabewerte für unvollständige Angaben sind
    public static TMJ string2date(String s, int tt, int mm, int yy) {
        String t = "";
        int nr = 0;
        int[] a = new int[3];
        a[0] = tt;
        a[1] = mm;
        a[2] = yy;
        for (int i = 0; i < s.length(); i++) {
            boolean inNumber = false;
            if (Character.isDigit(s.charAt(i))) {
                inNumber = true;
            }
            if (s.charAt(i) == '-' && t.length() == 0 && i + 1 < s.length() && Character.isDigit(s.charAt(i + 1))) {
                inNumber = true;
            }

            if (inNumber) {
                t = t + s.charAt(i);
            }
            if (!inNumber || i + 1 == s.length()) {
                if (!t.equals("")) {
                    try {
                        a[nr++] = Integer.parseInt(t);
                    } catch (Exception e) {
                    }
                }
                t = "";
            }
        }
        return new TMJ(a[0], a[1], a[2]);
    }

    public static int stringFindInt(String s, int def) {
        if (s == null) {
            return def;
        }
        return string2date(s, def, 0, 0).tag;
    }

    // aus einem TMJ einen Datumsstring machen
    public static String tmj2datestring(TMJ tmj) {
        return tmj.tag + "." + tmj.monat + "." + tmj.jahr;
    }

    // Aus einem String s (mit Referenzwerten tt.mm.yyyy) ein korrektes (gültiges) Datum errechnen
    public static TMJ correctDate(String s, int tt, int mm, int yy) {
        TMJ c = string2date(s, tt, mm, yy);
        int tag = c.tag;
        int monat = c.monat;
        int jahr = c.jahr;
        boolean vierstellig = jahr >= 1000 && jahr <= 2100;
        if (jahr < 0 || jahr > 2100) {
            jahr = yy;
        }
        if (jahr < 0 || jahr > 2100) {
            jahr = 0;
        }
        if (jahr < 1900) {
            jahr += 1900;
        }
        if (jahr < 1980 && !vierstellig) {
            jahr += 100; // nur ursprünglich nicht vierstellige Jahre als größer als 1980 interpretieren
        }
        if (monat < 1 || monat > 12) {
            monat = mm;
        }
        if (monat < 1 || monat > 12) {
            monat = 1;
        }
        if (tag < 1 || tag > 31) {
            tag = tt;
        }
        if (tag < 1 || tag > 31) {
            tag = 1;
        }
        switch (monat) {
            case 4:
            case 6:
            case 9:
            case 11:
                if (tag > 30) {
                    tag = 30;
                }
                break;
            case 2:
                if (tag > 29) {
                    tag = 29;
                }
                if (tag > 28 && jahr % 4 != 0) {
                    tag = 28;
                }
                break;
            default:
                ;
        }
        return new TMJ(tag, monat, jahr);
    }
    
    public static String dateToDMY(String date) {
        boolean fmtDMY = Daten.dateFormatDMY;
        if (date != null && date.indexOf("/") > 0) {
            fmtDMY = false;
        } else if (date != null && date.indexOf(".") > 0) {
            fmtDMY = true;
        }
        if (!fmtDMY) {
            TMJ tmj = EfaUtil.string2date(date, 0, 0, 0);
            if (tmj.tag > 0 && tmj.monat > 0) {
                return tmj.monat + "." + tmj.tag +
                        (tmj.jahr > 0 ? ":" + tmj.jahr : "");
            }
        }
        return date;
    }

    // Aus einem String s ein korrektes (gültiges) Datum machen, ggf. aktuelles Datum als Referenzdatum verwenden
    public static String correctDate(String s) {
        Calendar cal = new GregorianCalendar();
        TMJ tmj = correctDate(s, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
        return tmj.tag + "." + tmj.monat + "." + tmj.jahr;
    }

    // Aus einem String s eine korrekte (gültige) Zeit machen
    public static String correctTime(String s, boolean withSeconds) {
        return correctTime(s, 0, 0, 0, withSeconds);
    }

    public static String correctTime(String s, int hdef, int mdef, int sdef, boolean withSeconds) {
        return correctTime(s, hdef, mdef, sdef, withSeconds, false);
    }

    public static String correctTime(String s, int hdef, int mdef, int sdef, boolean withSeconds,
            boolean useZeroAsMinuteAndSecondIfHourWasGivenInS) {
        if (s.length() == 0) {
            return "";
        }
        TMJ hhmmss;
        if (useZeroAsMinuteAndSecondIfHourWasGivenInS) {
            hhmmss = EfaUtil.string2date(s, -1, -1, -1); // TMJ mißbraucht für die Auswertung von Uhrzeiten
            if (hhmmss.tag != -1) {
                mdef = 0;
                sdef = 0;
            }
        }
        hhmmss = EfaUtil.string2date(s, hdef, mdef, sdef); // TMJ mißbraucht für die Auswertung von Uhrzeiten
        int hh = hhmmss.tag;
        int mm = hhmmss.monat;
        int ss = hhmmss.jahr;
        if (hh > 100 && mm == 0) {
            mm = hh % 100;
            hh = hh / 100;
        }
        if (hh < 0) {
            hh = 0;
        }
        if (mm < 0) {
            mm = 0;
        }
        if (ss < 0) {
            ss = 0;
        }
        if (hh > 23) {
            hh = 23;
        }
        if (mm > 59) {
            mm = 59;
        }
        if (ss > 59) {
            ss = 59;
        }
        if (withSeconds) {
            return int2String(hh, 2) + ":" + int2String(mm, 2) + ":" + int2String(ss, 2);
        } else {
            return int2String(hh, 2) + ":" + int2String(mm, 2);
        }
    }
    
    public static String correctHours(String s, int hdef, int mdef, int sdef, boolean withSeconds) {
        return correctHours(s, hdef, mdef, sdef, withSeconds, false);
    }
    
    public static String correctHours(String s, int hdef, int mdef, int sdef, boolean withSeconds,
            boolean useZeroAsMinuteAndSecondIfHourWasGivenInS) {
        if (s.length() == 0) {
            return "";
        }
        TMJ hhmmss;
        if (useZeroAsMinuteAndSecondIfHourWasGivenInS) {
            hhmmss = EfaUtil.string2date(s, -1, -1, -1); // TMJ mißbraucht für die Auswertung von Uhrzeiten
            if (hhmmss.tag != -1) {
                mdef = 0;
                sdef = 0;
            }
        }
        hhmmss = EfaUtil.string2date(s, hdef, mdef, sdef); // TMJ mißbraucht für die Auswertung von Uhrzeiten
        int hh = hhmmss.tag;
        int mm = hhmmss.monat;
        int ss = hhmmss.jahr;
        if (hh > 100 && mm == 0) {
            mm = hh % 100;
            hh = hh / 100;
        }
        if (withSeconds) {
            return int2String(hh, 2) + ":" + int2String(mm, 2) + ":" + int2String(ss, 2);
        } else {
            return int2String(hh, 2) + ":" + int2String(mm, 2);
        }
    }

    // Aus einem String s eine korrekte (gültige) Zeit machen
    public static String correctTime(String s) {
        return correctTime(s, false);
    }

    // Aus einer Nachkommazahl eine Ziffer machen, so daß faktisch nur noch eine
    // Nachkommastelle existiert (90 -> 9; 10 -> 1; 55 -> 6)
    public static int makeDigit(int z) {
        if (z < 0) {
            z = z * (-1);
        }
        while (z > 9) {
            z = z / 10;
        }
        return z;
    }

    // Wandelt einen int-Wert in einen String um, wobei der int-Wert zuvor durch 10
    // geteilt wird (d.h. 123 -> "12.3")
    public static String zehntelInt2String(int i) {
        String s = Integer.toString(i);
        if (s.length() == 1 && i != 0) {
            s = "0" + s;
        }
        if (s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        } else {
            s = s.substring(0, s.length() - 1) + International.getDecimalSeparator() + s.substring(s.length() - 1, s.length());
        }
        if (s.length() == 0) {
            s = "0";
        }
        return s;
    }

    // Wandelt einen String in einen int-Wert, wobei der String-Wert mit 10 multipliziert wird
    // (d.h. "12.3" -> 123)
    public static int zehntelString2Int(String s) {
        if (s == null) {
            return 0;
        }
        TMJ tmj = string2date(s, 0, 0, 0);
        while (tmj.monat > 9) {
            tmj.monat /= 10;
        }
        return tmj.tag * 10 + tmj.monat;
    }

    public static String correctZehntelString(String s) {
        return zehntelInt2String(zehntelString2Int(s));
    }

    // Split: Einen String anhand von Trennzeichen in einen Vector aufspalten
    public static Vector<String> split(String s, char sep) {
        if (s == null) {
            return null;
        }
        Vector <String>v = new Vector<String>();
        while (s.length() != 0) {
            int pos = s.indexOf(sep);
            if (pos >= 0) {
                v.add(s.substring(0, pos));
                s = s.substring(pos + 1, s.length());
                if (s.length() == 0) {
                    v.add(""); // letztes (leeres) Element hinter letztem Trennzeichen
                }
            } else if (s.length() > 0) {
                v.add(s);
                s = "";
            }
        }
        return v;
    }

    public static String[] split(String s, String sep) {
        if (s == null) {
            return null;
        }
        ArrayList<String> list = new ArrayList<String>();
        while (s.length() != 0) {
            int pos = s.indexOf(sep);
            if (pos >= 0) {
                list.add(s.substring(0, pos));
                s = s.substring(pos + sep.length(), s.length());
                if (s.length() == 0) {
                    list.add(""); // letztes (leeres) Element hinter letztem Trennzeichen
                }
            } else if (s.length() > 0) {
                list.add(s);
                s = "";
            }
        }
        return list.toArray(new String[0]);
    }

    /*
    // Eine Komma-Liste in ein Array of String umwandeln
    public static String[] kommaList2Arr(String s, char sep, boolean addAndere) {
    if (s == null) return null;
    Vector v;
    if (s.length()>0) v = split(s,sep);
    else v = new Vector();
    String[] aa = new String[v.size()+ (addAndere ? 1 : 0) ];
    for (int ii=0; ii<v.size(); ii++)
    aa[ii] = (String)v.get(ii);
    if (addAndere) aa[aa.length-1] = "andere"; // nur für diese Listen soll ein "andere" angef
    return aa;
    }
    public static String[] statusList2Arr(String s) {
    return kommaList2Arr(s,',',true);
    }
     */
    // Eine Komma-Liste in ein Array of String umwandeln
    public static String[] kommaList2Arr(String s, char sep) {
        if (s == null) {
            return null;
        }
        Vector <String> v;
        if (s.length() > 0) {
            v = split(s, sep);
        } else {
            v = new Vector<String>();
        }
        String[] aa = new String[v.size()];
        for (int ii = 0; ii < v.size(); ii++) {
            aa[ii] = (String) v.get(ii);
        }
        return aa;
    }

    public static String[] statusList2Arr(String s) {
        if (s == null) {
            return null;
        }
        s = correctStatusList(s);
        String[] stati = kommaList2Arr(s.trim(), ',');
        if (Daten.efaTypes == null
                || !Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_STATUS, EfaTypes.TYPE_STATUS_GUEST)
                || !Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_STATUS, EfaTypes.TYPE_STATUS_OTHER)) {
            return stati;
        }
        Vector <String>stati2 = new Vector<String>();
        for (int i = 0; i < stati.length; i++) {
            if (!stati[i].toLowerCase().equals(Daten.efaTypes.getValue(EfaTypes.CATEGORY_STATUS, EfaTypes.TYPE_STATUS_GUEST).toLowerCase())
                    && !stati[i].toLowerCase().equals(Daten.efaTypes.getValue(EfaTypes.CATEGORY_STATUS, EfaTypes.TYPE_STATUS_OTHER).toLowerCase())) {
                stati2.add(stati[i]);
            }
        }
        stati2.add(Daten.efaTypes.getValue(EfaTypes.CATEGORY_STATUS, EfaTypes.TYPE_STATUS_GUEST));
        stati2.add(Daten.efaTypes.getValue(EfaTypes.CATEGORY_STATUS, EfaTypes.TYPE_STATUS_OTHER));
        String[] a = new String[stati2.size()];
        for (int i = 0; i < stati2.size(); i++) {
            a[i] = (String) stati2.get(i);
        }
        return a;
    }

    // Eine Komma-Liste in ein Array of int umwandeln
    public static int[] kommaList2IntArr(String s, char sep) {
        if (s == null) {
            return null;
        }
        Vector <String> v;
        if (s.length() > 0) {
            v = split(s, sep);
        } else {
            v = new Vector <String>();
        }
        int[] aa = new int[v.size()];
        for (int ii = 0; ii < v.size(); ii++) {
            aa[ii] = string2int((String) v.get(ii), 0);
        }
        return aa;
    }

    public static String arr2string(String[] a) {
        String s = "";
        if (a == null) {
            return s;
        }
        for (int i = 0; i < a.length; i++) {
            s = s + a[i];
            if (i + 1 < a.length) {
                s = s + " ";
            }
        }
        return s;
    }

    // Array of String in eine Komma-Liste umwandeln
    public static String arr2KommaList(String[] a) {
        String s = "";
        if (a == null) {
            return s;
        }
        for (int i = 0; i < a.length; i++) {
            s = s + a[i];
            if (i + 1 < a.length) {
                s = s + ",";
            }
        }
        return s;
    }

    // Array of int in eine Komma-Liste umwandeln
    public static String arr2KommaList(int[] a, int start) {
        String s = "";
        if (a == null) {
            return s;
        }
        for (int i = start; i < a.length; i++) {
            s = s + a[i];
            if (i + 1 < a.length) {
                s = s + ",";
            }
        }
        return s;
    }

    // Prüft, ob die angegebene Datei geöffnet werden kann
    public static boolean canOpenFile(String d) {
        FileReader f;
        try {
            f = new FileReader(d);
            f.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    
    public static boolean renameFile(String from, String to, String baksuffix) {
        try {
            File ffrom = new File(from);
            if (!ffrom.isFile()) {
                return false;
            }
            File fto = new File(to);
            if (fto.isFile()) {
                File fbak = new File(to + baksuffix);
                if (fbak.isFile()) {
                    fbak.delete();
                }
                fto.renameTo(fbak);
            }
            return ffrom.renameTo(fto);
        } catch(Exception e) {
            return false;
        }
    }

    // Suchen und Ersetzen in Strings
    public static String replace(String org, String such, String ers) {
        int wo;
        String s = org;
        if ((wo = org.indexOf(such)) >= 0) {
            if (wo > 0) {
                if (wo + such.length() < org.length()) {
                    s = org.substring(0, wo) + ers + org.substring(wo + such.length(), org.length());
                } else {
                    s = org.substring(0, wo) + ers;
                }
            } else if (wo + such.length() < org.length()) {
                s = ers + org.substring(wo + such.length(), org.length());
            } else {
                s = ers;
            }
        }
        return s;
    }

    public static String replace(String org, String such, String ers, boolean alle) {
        if (org == null || such == null || ers == null) {
            return null;
        }
        if (!alle || (ers.length() > 0 && ers.indexOf(such) >= 0)) {
            return replace(org, such, ers);
        }
        String s = org;
        while (s.indexOf(such) >= 0) {
            s = replace(s, such, ers);
        }
        return s;
    }

    // Vergleich zweier Strings auf numerische Weise, wobei an die Zahl noch eine Buchstabenfolge
    // angehängt sein darf (--> LfdNr). Bsp: 12<34; 12A<12B usw.
    public static int compareIntString(String a, String b) {
        TMJ aa = string2date(a, 1, 1, 1); // TMJ mißbraucht für die Auswertung von Zahlen
        TMJ bb = string2date(b, 1, 1, 1); // TMJ mißbraucht für die Auswertung von Zahlen
        if (aa.tag == bb.tag) {
            return a.compareTo(b);
        } else if (aa.tag < bb.tag) {
            return -1;
        } else {
            return 1;
        }
    }

    // Differenz (Anzahl der Tage) zwischen zwei TMJ-Datumsangaben berechnen (von und bis mitgerechnet)
    public static int getDateDiff(TMJ v, TMJ b) {
        GregorianCalendar von = new GregorianCalendar(v.jahr, v.monat - 1, v.tag);
        GregorianCalendar bis = new GregorianCalendar(b.jahr, b.monat - 1, b.tag);
        int tmp = Math.round((((float) bis.getTime().getTime() - (float) von.getTime().getTime()) / (float) 86400000) + 1);
        if (tmp > 0) {
            return tmp;
        } else {
            return -1 * tmp;
        }
    }
    // Differenz (Anzahl der Tage) zwischen zwei String-Datumsangaben berechnen (von und bis mitgerechnet)

    public static int getDateDiff(String sv, String sb) {
        return getDateDiff(string2date(sv, 1, 1, 1), string2date(sb, 1, 1, 1));
    }

    // Differenz (Anzahl der Tage) zwischen zwei TMJ-Datumsangaben berechnen (von und bis *nicht* mitgerechnet),
    // anhängig von der Reihenfolge
    public static int getRealDateDiff(TMJ d1, TMJ d2) {
        GregorianCalendar c1 = new GregorianCalendar(d1.jahr, d1.monat - 1, d1.tag);
        GregorianCalendar c2 = new GregorianCalendar(d2.jahr, d2.monat - 1, d2.tag);
        return Math.round((((float) c2.getTime().getTime() - (float) c1.getTime().getTime()) / (float) 86400000));
    }

    public static boolean secondDateIsAfterFirst(String sv, String sb) {
        TMJ v = string2date(sv, 1, 1, 1);
        TMJ b = string2date(sb, 1, 1, 1);
        GregorianCalendar von = new GregorianCalendar(v.jahr, v.monat - 1, v.tag);
        GregorianCalendar bis = new GregorianCalendar(b.jahr, b.monat - 1, b.tag);
        return bis.after(von);
    }

    public static boolean secondDateIsEqualOrAfterFirst(String sv, String sb) {
        TMJ v = string2date(sv, 1, 1, 1);
        TMJ b = string2date(sb, 1, 1, 1);
        GregorianCalendar von = new GregorianCalendar(v.jahr, v.monat - 1, v.tag);
        GregorianCalendar bis = new GregorianCalendar(b.jahr, b.monat - 1, b.tag);
        return bis.equals(von) || bis.after(von);
    }

    public static boolean secondTimeIsAfterFirst(String sv, String sb) {
        TMJ v = string2date(sv, 1, 1, 1);
        TMJ b = string2date(sb, 1, 1, 1);
        if (b.tag > v.tag) {
            return true;
        }
        if (b.tag < v.tag) {
            return false;
        }
        if (b.monat > v.monat) {
            return true;
        }
        if (b.monat < v.monat) {
            return false;
        }
        if (b.jahr > v.jahr) {
            return true;
        }
        if (b.jahr > v.jahr) {
            return false;
        }
        return false;
    }

    public static TMJ incDate(TMJ tmj, int diff) {
        GregorianCalendar cal = new GregorianCalendar(tmj.jahr, tmj.monat - 1, tmj.tag);
        cal.add(GregorianCalendar.DATE, diff);
        return new TMJ(cal.get(GregorianCalendar.DAY_OF_MONTH), cal.get(GregorianCalendar.MONTH) + 1, cal.get(GregorianCalendar.YEAR));
    }

    // Anzahl eines Zeichens in einem String ermitteln
    public static int countCharInString(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    // Anzahl einer Zeichenfolge in einem String ermitteln
    public static int countCharInString(String s, String c) {
        int n = 0;
        int pos;
        while ( (pos = s.indexOf(c)) >= 0) {
            n++;
            s = s.substring(pos+c.length());
        }
        return n;
    }


    // Dateinamen "fileName" ggf. um Pfadangabe "basePath" vervollständigen, falls "fileName" kein absoluter Pfad
    public static String makeFullPath(String basePath, String fileName) {
        if (basePath == null || fileName == null) {
            return null;
        }
        String s = fileName;
        try {
            String olddir = System.getProperty("user.dir");
            System.setProperty("user.dir", basePath);
            File f = new File(fileName);
            s = f.getAbsolutePath();
            System.setProperty("user.dir", olddir);
        } catch (SecurityException e) {
        }
        return s;
    }

    // Dateinamen "fileName" relativ zum Namen "efbName" machen, d.h. falls gleiches Verzeichnis, dieses weglassen
    public static String makeRelativePath(String fileName, String efbName) {
        String t;
        if ((t = EfaUtil.getPathOfFile(fileName)).length() > 0) {
            if (t.equals(EfaUtil.getPathOfFile(efbName))) {
                return fileName.substring(t.length() + 1, fileName.length());
            } else {
                return fileName;
            }
        } else {
            return fileName;
        }
    }

    // Pfadangabe aus einem Dateinamen ermitteln
    public static String getPathOfFile(String fileName) {
        if (fileName == null) {
            return null;
        }
        String s = new File(fileName).getName();
        int wo;
        if (s != null && (wo = fileName.lastIndexOf(s)) >= 0) {
            if (wo > 0) {
                s = fileName.substring(0, wo - 1);
                if (fileName.charAt(wo - 1) == ':' && Daten.fileSep.equals("\\")) {
                    s += ":"; // Windows Fall "a:2002.efb"
                }
            } else {
                return "";
            }
        } else {
            s = System.getProperty("file.separator");
        }
        if (s.length() == 2 && s.charAt(1) == ':') {
            s = s + System.getProperty("file.separator");
        }
        return s;
    }

    // lokalen Dateinamen aus kompletter Pfadangabe ermitteln
    public static String getNameOfFile(String fileName) {
        if (fileName == null) {
            return null;
        }
        int l = getPathOfFile(fileName).length();
        return (l >= fileName.length() ? "" : fileName.substring((l == 0 ? 0 : l + 1)));
    }

    // Dateinamen ohne jegliche Pfadangaben erhalten
    public static String getFilenameWithoutPath(String fileName) {
        if (fileName == null || fileName.length() == 0) {
            return fileName;
        }
        int pos = fileName.lastIndexOf(Daten.fileSep);
        if (pos < 0) {
            return fileName;
        }
        return fileName.substring(pos + 1);
    }

    // Alle Senkrechtstriche (|) aus String entfernen
    public static String removeSepFromString(String s) {
        return removeSepFromString(s, "|");
    }
    // Alle Separatoren aus String entfernen

    public static String removeSepFromString(String s, String sep) {
        if (s == null) {
            return null;
        }
        s = replace(s, sep, "", true);
        return s;
    }

    // Alle runde Klammern aus String entfernen
    public static String removeBracketsFromString(String s) {
        if (s == null) {
            return null;
        }
        s = EfaUtil.replace(s, "(", "", true);
        s = EfaUtil.replace(s, ")", "", true);
        return s;
    }

    public static String replaceNotAllowedCharacters(String s,
            String allowedCharacters, String notAllowedCharacters,
            String replacementCharacter) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if ((allowedCharacters != null && allowedCharacters.indexOf(c) < 0)
                    || (notAllowedCharacters != null && notAllowedCharacters.indexOf(c) >= 0)) {
                s = (i > 0 ? s.substring(0, i) : "")
                        + (replacementCharacter != null ? replacementCharacter : "")
                        + (i + 1 < s.length() ? s.substring(i + 1) : "");
                if (replacementCharacter != null) {
                    i++;
                }
            } else {
                i++;
            }
        }
        return s;
    }

    // Dateinamen in Großbuchstaben umwandelt, falls Windows-System
    public static String upcaseFileName(String s) {
        if (System.getProperty("file.separator").equals("\\")) {
            return s.toUpperCase();
        } else {
            return s;
        }
    }

    // Einen String in ein int umwandeln und bei Fehler "vorgabe" zurückliefern
    public static int string2int(String s, int vorgabe) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return vorgabe;
        }
    }

    public static String int2String(int i, int digits) {
        return int2String(i, digits, true);
    }

    public static String int2String(int i, int digits, boolean leadingZero) {
        StringBuilder s = new StringBuilder(digits);
        s.append(Integer.toString(i));
        while (s.length() < digits) {
            s.insert(0, (leadingZero ? "0" : " "));
        }
        return s.toString();
    }

    public static String long2String(long l, int minDigits) {
        StringBuilder s = new StringBuilder(minDigits);
        s.append(Long.toString(l));
        while (s.length() < minDigits) {
            s.insert(0, "0");
        }
        return s.toString();
    }

    // Einen String in ein long umwandeln und bei Fehler "vorgabe" zurückliefern
    public static long string2long(String s, int vorgabe) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return vorgabe;
        }
    }

    // Methode zum kopieren einer Datei
    public static boolean copyFile(String quelle, String ziel) {
        if ((new File(quelle)).equals(new File(ziel))) {
            return false; // Quelle und Ziel sind dieselbe Datei!
        }
        final int BUFSIZE = 4096;
        FileInputStream f1;
        FileOutputStream f2;
        byte[] buf = new byte[BUFSIZE];
        int n;
        if (!canOpenFile(quelle)) {
            return false;
        }
        try {
            f1 = new FileInputStream(quelle);
            f2 = new FileOutputStream(ziel);
            while ((n = f1.read(buf, 0, BUFSIZE)) > 0) {
                f2.write(buf, 0, n);
            }
            f1.close();
            f2.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    // korrekte Laufende Nummer kreieren
    public static String getLfdNr(String t) {
        t = t.trim().toUpperCase();
        TMJ hhmm = EfaUtil.string2date(t, 1, 1, 1); // TMJ mißbraucht für die Auswertung von Zahlen
        String s = Integer.toString(hhmm.tag);
        int wo = t.indexOf(s);
        int l = s.length();
        if (wo >= 0 && wo + l < t.length() && t.charAt(wo + l) >= 'A' && t.charAt(wo + l) <= 'Z') // auch Buchstaben als Ergänzung zur LfdNr zulassen
        {
            s = s + t.charAt(wo + l);
        }
        return s;
    }

    // zu einem gegebenen (Synonym-)Namen s aus einer Datenliste l den passenden Originalnamen heraussuchen
    public static String syn2org(Synonyme l, String s) {
        if (l == null) {
            return s;
        }
        if (l.getExact(s) != null) {
            DatenFelder d = (DatenFelder) l.getComplete();
            String orgs = null;
            if (d != null) {
                orgs = d.get(Synonyme.ORIGINAL);
            }
            if (orgs != null && !orgs.equals("")) {
                return orgs;
            }
        }
        return s;
    }

    // zu einem gegebenen OriginalNamen s aus einer Datenliste l alle passenden Synonymnamen heraussuchen
    public static Vector <String> org2syn(Synonyme l, String s) {
        if (l == null || s == null) {
            return null;
        }
        Vector <String>v = new Vector<String>();
        for (DatenFelder d = l.getCompleteFirst(); d != null; d = l.getCompleteNext()) {
            if (d.get(Synonyme.ORIGINAL).equals(s)) {
                v.add(d.get(Synonyme.SYNONYM));
            }
        }
        if (v.size() == 0) {
            return null;
        }
        return v;
    }

    // aus einer Zahl 0<=i<=15 eine Hex-Ziffer machen
    public static char getHexDigit(int i) {
        if (i < 10) {
            return (char) (i + '0');
        } else {
            return (char) ((i - 10) + 'A');
        }
    }

    // aus einer Zahl 0<=i<=255 eine zweistellige Hex-Zahl machen
    public static String hexByte(int i) {
        return getHexDigit(i / 16) + "" + getHexDigit(i % 16);
    }

    public static String hexByte(byte b) {
        int i = b;
        if (i < 0) {
            i += 256;
        }
        return getHexDigit(i / 16) + "" + getHexDigit(i % 16);
    }

    // Auf String s den SHA-Algorithmus anwenden und Ergebnis Hex-Codiert zurückliefern
    public static String getSHA(String z) {
        if (z == null || z.length() == 0) {
            return null;
        }
        return getSHA(z.getBytes());
    }

    // Auf Byte[] message den SHA-Algorithmus anwenden und Ergebnis Hex-Codiert zurückliefern
    public static String getSHA(byte[] message) {
        return getSHA(message, message.length);
    }

    public static String getSHA(byte[] message, int length) {
        if (message == null || length <= 0) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(message, 0, length);
            byte[] output = md.digest();
            String s = "";
            for (int i = 0; i < output.length; i++) {
                s += hexByte(output[i] + 128);
            }
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // SHA als long-Wert liefern (die letzten x bytes)
    public static long getSHAlong(byte[] message, int x) {
        return getSHAlong(message, message.length, x);
    }

    public static long getSHAlong(byte[] message, int length, int x) {
        if (message == null || length <= 0) {
            return -1;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(message, 0, length);
            byte[] output = md.digest();
            long l = 0;
            for (int i = output.length - 1; i >= 0 && output.length - i <= x; i--) {
                l = l * 256 + (((long) output[i]) + 128);
            }
            return l;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Auf File f (nur max. erste 2 MB) den SHA-Algorithmus anwenden und Ergebnis Hex-Codiert zurückliefern
    public static String getSHA(File f) {
        return getSHA(f, (int) f.length());
    }

    public static String getSHA(File f, int length) {
        if (!f.isFile() || length <= 0) {
            return null;
        }
        FileInputStream f1;
        byte[] buf = new byte[length];
        @SuppressWarnings("unused")
		int n;
        try {
            f1 = new FileInputStream(f);
            n = f1.read(buf, 0, length);
            f1.close();
        } catch (IOException e) {
            return null;
        }
        return getSHA(buf);
    }
    
    public static String getInterfaceInfo(NetworkInterface intf) {
        try {
            StringBuilder s = new StringBuilder();
            s.append(intf.getDisplayName());
            
            s.append(" (MAC ");
            byte[] mac = intf.getHardwareAddress();
            for (int i=0; i<mac.length; i++) {
                s.append( (i > 0 ? ":" : "") + EfaUtil.hexByte(mac[i]));
            }
            
            s.append(" MTU " + intf.getMTU());
            s.append(intf.isUp() ? " UP" : " DOWN");
            
            s.append(")");
            return s.toString();
        } catch(Exception e) {
            return "";
        }
    }

    // aus einem zweistelligen Jahresdatum ggf. ein vierstelliges machen
    public static int yy2yyyy(int jahr) {
        if (jahr < 100) {
            jahr += 1900;
        }
        if (jahr < 1850) {
            while (jahr < 1900) {
                jahr += 100;
            }
        }
        return jahr;
    }

    // aus einem String einen String generieren, der eine korrekte Status-Liste enthält
    private static String correctStatusList(String s) {
        s = EfaUtil.replace(s, " ,", ",", true); // Leerzeichen vor und nach Einträgen entfernen
        s = EfaUtil.replace(s, ", ", ",", true); // Leerzeichen vor und nach Einträgen entfernen
        s = EfaUtil.replace(s, ",,", ",", true); // Leerzeichen vor und nach Einträgen entfernen
        return s.trim();
    }

    private static String makeTimeString(int value, int chars) {
        String s = Integer.toString(value);
        while (s.length() < chars) {
            s = "0" + s;
        }
        return s;
    }

    public static int getCurrentHour() {
        Calendar cal = new GregorianCalendar();
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    public static String getCurrentTimeStamp() {
        Calendar cal = new GregorianCalendar();
        return makeTimeString(cal.get(Calendar.DAY_OF_MONTH), 2) + "."
                + makeTimeString(cal.get(Calendar.MONTH) + 1, 2) + "."
                + makeTimeString(cal.get(Calendar.YEAR), 4) + " "
                + makeTimeString(cal.get(Calendar.HOUR_OF_DAY), 2) + ":"
                + makeTimeString(cal.get(Calendar.MINUTE), 2) + ":"
                + makeTimeString(cal.get(Calendar.SECOND), 2);
    }

    public static String getCurrentTimeStampHHMMSS() {
        Calendar cal = new GregorianCalendar();
        return makeTimeString(cal.get(Calendar.HOUR_OF_DAY), 2) + ":"
                + makeTimeString(cal.get(Calendar.MINUTE), 2) + ":"
                + makeTimeString(cal.get(Calendar.SECOND), 2);
    }

    public static String getCurrentTimeStampHHMM() {
        Calendar cal = new GregorianCalendar();
        return makeTimeString(cal.get(Calendar.HOUR_OF_DAY), 2) + ":"
                + makeTimeString(cal.get(Calendar.MINUTE), 2);
    }

    public static String getTimeStamp(long l) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(l);
        return makeTimeString(cal.get(Calendar.DAY_OF_MONTH), 2) + "."
                + makeTimeString(cal.get(Calendar.MONTH) + 1, 2) + "."
                + makeTimeString(cal.get(Calendar.YEAR), 4) + " "
                + makeTimeString(cal.get(Calendar.HOUR_OF_DAY), 2) + ":"
                + makeTimeString(cal.get(Calendar.MINUTE), 2) + ":"
                + makeTimeString(cal.get(Calendar.SECOND), 2);
    }

    public static String getTimeStampDDMMYYYY(long l) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(l);
        return makeTimeString(cal.get(Calendar.DAY_OF_MONTH), 2) + "."
                + makeTimeString(cal.get(Calendar.MONTH) + 1, 2) + "."
                + makeTimeString(cal.get(Calendar.YEAR), 4);
    }

    public static String getCurrentTimeStampYYYYMMDD_HHMMSS() {
        Calendar cal = new GregorianCalendar();
        return makeTimeString(cal.get(Calendar.YEAR), 4)
                + makeTimeString(cal.get(Calendar.MONTH) + 1, 2)
                + makeTimeString(cal.get(Calendar.DAY_OF_MONTH), 2)
                + "_"
                + makeTimeString(cal.get(Calendar.HOUR_OF_DAY), 2)
                + makeTimeString(cal.get(Calendar.MINUTE), 2)
                + makeTimeString(cal.get(Calendar.SECOND), 2);
    }

    public static String getCurrentTimeStampYYYY_MM_DD_HH_MM_SS() {
        Calendar cal = new GregorianCalendar();
        return makeTimeString(cal.get(Calendar.YEAR), 4)
                + "/"
                + makeTimeString(cal.get(Calendar.MONTH) + 1, 2)
                + "/"
                + makeTimeString(cal.get(Calendar.DAY_OF_MONTH), 2)
                + " "
                + makeTimeString(cal.get(Calendar.HOUR_OF_DAY), 2)
                + ":"
                + makeTimeString(cal.get(Calendar.MINUTE), 2)
                + ":"
                + makeTimeString(cal.get(Calendar.SECOND), 2);
    }

    public static String getCurrentTimeStampDD_MM_HH_MM() {
        Calendar cal = new GregorianCalendar();
        return makeTimeString(cal.get(Calendar.DAY_OF_MONTH), 2) + "."
                + makeTimeString(cal.get(Calendar.MONTH) + 1, 2) + ". "
                + makeTimeString(cal.get(Calendar.HOUR_OF_DAY), 2) + ":"
                + makeTimeString(cal.get(Calendar.MINUTE), 2);
    }

    public static String getCurrentTimeStampDD_MM_YYYY() {
        Calendar cal = new GregorianCalendar();
        return makeTimeString(cal.get(Calendar.DAY_OF_MONTH), 2) + "."
                + makeTimeString(cal.get(Calendar.MONTH) + 1, 2) + "."
                + makeTimeString(cal.get(Calendar.YEAR), 4);
    }

    public static String getCurrentTimeStampMM_DD_YYYY() {
        Calendar cal = new GregorianCalendar();
        return makeTimeString(cal.get(Calendar.MONTH) + 1, 2) + "/"
                + makeTimeString(cal.get(Calendar.DAY_OF_MONTH), 2) + "/"
                + makeTimeString(cal.get(Calendar.YEAR), 4);
    }

    public static String getCurrentTimeStampInDateFormat() {
        return (Daten.dateFormatDMY ?
                getCurrentTimeStampDD_MM_YYYY() :
                getCurrentTimeStampMM_DD_YYYY());
    }

    public static String getCurrentTimeStampYYYY() {
        Calendar cal = new GregorianCalendar();
        return makeTimeString(cal.get(Calendar.YEAR), 4);
    }

    public static String date2String(Date date) {
        return date2String(date, true);
    }

    public static String date2String(Date date, boolean printTime) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) > 10000) {
            return "---";
        }
        return makeTimeString(cal.get(Calendar.DAY_OF_MONTH), 2) + "."
                + makeTimeString(cal.get(Calendar.MONTH) + 1, 2) + "."
                + makeTimeString(cal.get(Calendar.YEAR), 4)
                + (printTime
                ? " "
                + makeTimeString(cal.get(Calendar.HOUR_OF_DAY), 2) + ":"
                + makeTimeString(cal.get(Calendar.MINUTE), 2) + ":"
                + makeTimeString(cal.get(Calendar.SECOND), 2)
                : "");
    }

    public static String getWoTag(String datum) {
        if (datum == null || datum.equals("")) {
            return "";
        }
        TMJ tmj = correctDate(datum, 0, 0, 0);
        Calendar cal = new GregorianCalendar();
        cal.set(tmj.jahr, tmj.monat - 1, tmj.tag);
        switch (cal.get(GregorianCalendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return International.getString("Montag");
            case Calendar.TUESDAY:
                return International.getString("Dienstag");
            case Calendar.WEDNESDAY:
                return International.getString("Mittwoch");
            case Calendar.THURSDAY:
                return International.getString("Donnerstag");
            case Calendar.FRIDAY:
                return International.getString("Freitag");
            case Calendar.SATURDAY:
                return International.getString("Samstag");
            case Calendar.SUNDAY:
                return International.getString("Sonntag");
            default:
                return "";
        }
    }
    
    public static int getCalendarWeekDayFromEfaWeekDay(String efaWeekDay) {
    	if (efaWeekDay.equals(EfaTypes.TYPE_WEEKDAY_MONDAY)) {return 2;}
    	if (efaWeekDay.equals(EfaTypes.TYPE_WEEKDAY_TUESDAY)) {return 3;}
    	if (efaWeekDay.equals(EfaTypes.TYPE_WEEKDAY_WEDNESDAY)) {return 4;}
    	if (efaWeekDay.equals(EfaTypes.TYPE_WEEKDAY_THURSDAY)) {return 5;}
    	if (efaWeekDay.equals(EfaTypes.TYPE_WEEKDAY_FRIDAY)) {return 6;}
    	if (efaWeekDay.equals(EfaTypes.TYPE_WEEKDAY_SATURDAY)) {return 7;}
    	if (efaWeekDay.equals(EfaTypes.TYPE_WEEKDAY_SUNDAY)) {return 1;}
    	return 1; //Default use Sunday 
    }

    // prüft, ob im String s das Zeichen pos ein "+" ist. Falls der String zu kurz ist, wird false geliefert
    public static boolean isOptionSet(String s, int pos) {
        if (s == null || pos < 0 || pos >= s.length()) {
            return false; // out of range == not set
        }
        return s.charAt(pos) == '+';
    }

    public static GregorianCalendar dateTime2Cal(String dateTTMMJJ, String timeHHMMSS) {
        TMJ datum = EfaUtil.correctDate(dateTTMMJJ, 1, 1, 1980);
        TMJ zeit = EfaUtil.string2date(timeHHMMSS, 0, 0, 0);
        return new GregorianCalendar(datum.jahr, datum.monat - 1, datum.tag, zeit.tag, zeit.monat, zeit.jahr);
    }

    public static GregorianCalendar dateTime2Cal(TMJ datum, TMJ zeit) {
        return new GregorianCalendar(datum.jahr, datum.monat - 1, datum.tag, zeit.tag, zeit.monat, zeit.jahr);
    }

    public static Color getColorOrGray(String s) {
        int cint;
        try {
            cint = Integer.parseInt(s, 16);
        } catch (Exception ee) {
            cint = 204 * 65536 + 204 * 256 + 204;
        }
        return new Color(cint);
    }

    public static Color getColor(String s) {
        try {
            return new Color(Integer.parseInt(s, 16));
        } catch (Exception ee) {
            return null;
        }
    }

    public static String getColor(Color c) {
        String s = "";
        float[] rgb = c.getRGBColorComponents(null);
        for (int i = 0; i < rgb.length; i++) {
            s += EfaUtil.hexByte((int) (rgb[i] * 255));
        }
        return s;
    }

    public static void gc() {
        long totalMem = Runtime.getRuntime().totalMemory();
        long freeMem = Runtime.getRuntime().freeMemory();
        Stopwatch w = new Stopwatch();
        w.start();
        System.gc();
        w.stop();
        // do not translate!
        Dialog.infoDialog("GarbageCollection", "GarbageCollection finished in " + w.diff() + "ms.\n"
                + "Before GarbageCollection:\n"
                + "    total VM Heap: " + totalMem + " Bytes\n"
                + "    free  VM Heap: " + freeMem + " Bytes\n"
                + "After GarbageCollection:\n"
                + "    total VM Heap: " + Runtime.getRuntime().totalMemory() + " Bytes\n"
                + "    free  VM Heap: " + Runtime.getRuntime().freeMemory() + " Bytes");
    }

    static class Stopwatch {

        private long time_start, time_stop;

        public void start() {
            time_start = System.currentTimeMillis();
        }

        public void stop() {
            time_stop = System.currentTimeMillis();
        }

        public long diff() {
            return time_stop - time_start;
        }
    }

    // does nothing.... ;-) Used for Exception-Hanling, so that FindBugs is happy!
    public static void foo() {
    }

    // Entpacken eines Ziparchivs zipFile in einem Verzeichnis destDir
    // Rückgabe: null, wenn erfolgreich; String != null mit Fehlermeldungen, sonst
    public static String unzip(String zipFile, String destDir) {
        return unzip(zipFile, destDir, null, null);
    }
    public static String unzip(String zipFile, String destDir,
            String replaceFilePostfixSource, String replaceFilePostfixDest) {
        if (!(new File(zipFile)).isFile()) {
            return LogString.fileNotFound(zipFile, International.getString("ZIP-Archiv"));
        }
        if (!(new File(destDir)).isDirectory()) {
            return LogString.directoryDoesNotExist(destDir, International.getString("Ziel-Verzeichnis"));
        }

        if (!destDir.endsWith(Daten.fileSep)) {
            destDir += Daten.fileSep;
        }
        String result = null;

        try {
            ZipFile zip = new ZipFile(zipFile);
            Enumeration files = zip.entries();
            ZipEntry file = (ZipEntry) files.nextElement();
            while (file != null) {
                String filename = file.getName();
                if (file.isDirectory()) {
                    // Verzeichnis
                    if (!(new File(destDir + filename)).isDirectory()) {
                        if (!(new File(destDir + filename)).mkdirs()) {
                            result = (result == null ? "" : result + "\n")
                                    + LogString.operationFailed(International.getString("Entpacken"),
                                        LogString.directoryCreationFailed(destDir + filename, International.getString("Verzeichnis")) + "\n"
                                        + LogString.operationAborted(International.getString("Entpacken")));
                        }
                    }
                } else {
                    // normale Datei
                    try {
                        // enthälter Dateiname einen Verzeichnisnamen eines Verzeichnisses, das möglicherweise noch nicht existiert?
                        String dir = null;
                        if (filename.lastIndexOf("/") >= 0) {
                            dir = filename.substring(0, filename.lastIndexOf("/"));
                        }
                        if (dir == null && filename.lastIndexOf("\\") >= 0) {
                            dir = filename.substring(0, filename.lastIndexOf("\\"));
                        }
                        if (dir != null && !(new File(destDir + dir)).isDirectory()) {
                            if (!(new File(destDir + dir)).mkdirs()) {
                            result = (result == null ? "" : result + "\n")
                                    + LogString.operationFailed(International.getString("Entpacken"),
                                        LogString.directoryCreationFailed(destDir + dir, International.getString("Verzeichnis")) + "\n"
                                        + LogString.operationAborted(International.getString("Entpacken")));
                            }
                        }

                        // check whether we should extract the file under a different name
                        if (replaceFilePostfixSource != null && filename.endsWith(replaceFilePostfixSource) &&
                            replaceFilePostfixDest != null) {
                            int pos = filename.lastIndexOf(replaceFilePostfixSource);
                            filename = filename.substring(0, pos) + replaceFilePostfixDest;
                        }

                        // jetzt die Datei entpacken
                        BufferedInputStream stream = new BufferedInputStream(zip.getInputStream(file));
                        BufferedOutputStream f = new BufferedOutputStream(new FileOutputStream(destDir + filename));
                        byte[] buf = new byte[ZIP_BUFFER];
                        int read;
                        while ((read = stream.read(buf, 0, ZIP_BUFFER)) != -1) {
                            f.write(buf, 0, read);
                        }
                        f.close();
                        stream.close();
                    } catch (Exception e) {
                        result = (result == null ? "" : result + "\n")
                                + LogString.operationFailed(International.getString("Entpacken"),
                                    LogString.fileExtractFailed(filename,
                                        International.getString("Datei"), e.toString()));
                    }
                }
                file = (files.hasMoreElements() ? (ZipEntry) files.nextElement() : null);
            }
        } catch (Exception ee) {
            result = (result == null ? "" : result + "\n") +
                    LogString.operationFailed(International.getString("Entpacken"), ee.toString())
                    + "\n" +
                    LogString.operationAborted(International.getString("Entpacken"));
        }
        return result;
    }

    public static String createZipArchive(Vector sourceDirs, Vector inclSubdirs, String zipFile) {
        try {
            String warnings = "";
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zipFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            Hashtable <String,String>processedDirectories = new Hashtable<String,String>();
            byte data[] = new byte[ZIP_BUFFER];
            for (int j = 0; j < sourceDirs.size(); j++) {
                // get a list of files from current directory
                String dir = (String) sourceDirs.get(j);
                if (!dir.endsWith(Daten.fileSep)) {
                    dir += Daten.fileSep;
                }
                if (processedDirectories.get(dir) != null) {
                    continue;
                }
                processedDirectories.put(dir, "foobar");
                File f = new File(dir);
                String files[] = f.list();

                // relative directory (for storing file in zipfile)
                String reldir = dir;
                if (Daten.efaMainDirectory != null && reldir.startsWith(Daten.efaMainDirectory)) {
                    reldir = reldir.substring(Daten.efaMainDirectory.length(), reldir.length());
                }
                if (reldir.startsWith(Daten.fileSep)) {
                    reldir = reldir.substring(1, reldir.length());
                }
                if (reldir.length() > 2 && reldir.charAt(1) == ':' && reldir.charAt(2) == Daten.fileSep.charAt(0)) {
                    reldir = reldir.substring(3, reldir.length());
                }
                if (!Daten.fileSep.equals("/")) {
                    reldir = EfaUtil.replace(reldir, Daten.fileSep, "/", true); // Bugfix: in <=160 konnten die unter Windows erstellten ZIP-Archive unter Linux nicht richtig gelesen werden
                }
                for (int i = 0; i < files.length; i++) {
                    if ((new File(dir + files[i])).isDirectory()) {
                        if (j >= inclSubdirs.size() || // j >= inclSubdirs.size() == true, wenn das Verzeichnis zuvor durch folgende Zeile dynamisch hinzugefügt wurde
                                ((Boolean) inclSubdirs.get(j)).booleanValue()) {
                            sourceDirs.add(dir + files[i]);
                        }
                    } else {
                        try {
                            FileInputStream fi = new FileInputStream(dir + files[i]);
                            origin = new BufferedInputStream(fi, ZIP_BUFFER);
                            ZipEntry entry = new ZipEntry(reldir + files[i]);
                            out.putNextEntry(entry);
                            int count;
                            while ((count = origin.read(data, 0, ZIP_BUFFER)) != -1) {
                                out.write(data, 0, count);
                            }
                        } catch (Exception se) {
                            warnings += dir + files[i] + "\n";
                        }
                        origin.close();
                    }
                }
            }
            out.close();
            if (warnings.length() > 0) {
                return International.getString("Folgende Dateien konnten nicht gesichert werden:") +
                        " " + warnings + "\n";
            }
        } catch (Exception e) {
            return e.toString();
        }
        return null; // erfolgreich
    }

    public static String moveAndEmptyFile(String fname, String dstDir) {
        try {
            if (fname == null || dstDir == null) {
                return null;
            }
            File f = new File(fname);
            if (!f.isFile()) {
                return null;
            }
            if (!(new File(dstDir)).isDirectory()) {
                return null;
            }
            String newname = dstDir
                    + getNameOfFile(fname)
                    + getCurrentTimeStampYYYYMMDD_HHMMSS();
            if (!copyFile(fname, newname)) {
                return null;
            }
            if (!f.delete()) {
                return null;
            }
            if (!f.createNewFile()) {
                return null;
            }
            return newname;
        } catch (IOException e) {
            return null;
        }
    }

    public static String cent2euro(int cent, boolean currency) {
        String s = Integer.toString(cent);
        while (s.length() < 3) {
            s = "0" + s;
        }
        s = s.substring(0, s.length() - 2) + "," + s.substring(s.length() - 2, s.length());
        if (s.endsWith(",00")) {
            s = s.substring(0, s.length() - 2) + "-";
        }
        if (currency) {
            return s + " EUR";
        } else {
            return s;
        }
    }
    /*
    public static String getCertInfos(String keyStore, String alias, char[] password) {
    PrintStream stdOut = System.out;
    CaptureOutputPrintStream cops = new CaptureOutputPrintStream(new PipedOutputStream());
    System.setOut(cops);
    try {
    sun.security.tools.KeyTool.main(EfaUtil.kommaList2Arr("-list -v -alias "+alias+
    " -storepass "+password+
    " -keystore "+keyStore,' ',false));
    } catch(Throwable e) {
    return "";
    }
    System.setOut(stdOut);
    String s = "";
    Vector v = cops.getLines();
    for (int i=0; i<v.size(); i++) s += v.get(i).toString()+"\n";
    return s;
    }
     */

    public static boolean deleteFile(String filename) {
        if (filename == null) {
            return false;
        }
        try {
            File f = new File(filename);
            return f.delete();
        } catch (Exception e) {
            return false;
        }
    }

    public static String leadingZeroString(int i, int length) {
        String s = Integer.toString(i);
        while (s.length() < length) {
            s = "0" + s;
        }
        return s;
    }

    public static String leadingZeroString(long l, int length) {
        String s = Long.toString(l);
        while (s.length() < length) {
            s = "0" + s;
        }
        return s;
    }

    public static String getRightBoundNumber(long l, int length) {
        String s = Long.toString(l);
        while (s.length() < length) {
            s = " " + s;
        }
        return s;
    }

    public static int sumUpArray(int[] array) {
        if (array == null) {
            return 0;
        }
        int sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }

    public static String vector2string(AbstractList v, String sep) {
        if (v == null) {
            return null;
        }
        String s = "";
        for (int i = 0; i < v.size(); i++) {
            s += (i > 0 ? sep : "") + v.get(i);
        }
        return s;
    }

    // wenn das Datum zwischen 15.07. und 31.07. liegt (Geburtstag),
    // gibt diese Methode zurück, der wievielte Geburtstag es ist.
    // wenn das Datum nicht im o.g. Zeitraum liegt, gibt diese Methode -1 zurück.
    public static int getEfaBirthday() {
        Calendar cal = new GregorianCalendar();
        boolean birthday = cal.get(Calendar.MONTH) + 1 == 7
                && cal.get(Calendar.DAY_OF_MONTH) >= 15;
        if (birthday) {
            return cal.get(Calendar.YEAR) - 2001;
        } else {
            return -1;
        }
    }

    public static void pack(Window frame) {
        // Bugfix/Workaround für Problem, daß Fenster manchmal zu klein
        try {
            setMinimumSize(frame);
            frame.pack();
            Dimension size = frame.getSize();
            size.setSize(size.getWidth() + 2, size.getHeight() + 2);
            frame.setSize(Dialog.getMaxSize(size));
        } catch (Exception e) {
        	Logger.logdebug(e);
        }
    }

    // Setzt für die Komponente c und alle ihre Subkomponenten die MinimumSize so, daß sie der PreferredSize entspricht
    public static void setMinimumSize(Component c) {
        try {
            if (java_awt_Container.getClass().isInstance(c)) {
                Container container = (Container) c;
                Component[] components = container.getComponents();
                for (int i = 0; components != null && i < components.length; i++) {
                    setMinimumSize(components[i]);
                }
            }
            JComponent jcomponent = (JComponent) c;
            jcomponent.setMinimumSize(Dialog.getMaxSize(jcomponent.getPreferredSize()));
        } catch (Exception e) {
        }
    }

    public static String getInputShortcut(String firstName, String lastName) {
        int i = 0;
        String aliasFormat = Daten.efaConfig.getValueAliasFormat();
        if (aliasFormat == null) {
            return "";
        }
        String s = "";
        while (i < aliasFormat.length()) {
            if (aliasFormat.charAt(i) == '{') {
                if (aliasFormat.length() < i + 4 || aliasFormat.charAt(i + 3) != '}') {
                    if (Logger.isTraceOn(Logger.TT_OTHER)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GENERIC,
                                "Error Parsing InputShortcutFormat"); // no need to translate
                    }
                    return "";
                }
                String feld = null;
                switch (aliasFormat.charAt(i + 1)) {
                    case 'f':
                    case 'F':
                    case 'v':
                    case 'V':
                        feld = firstName.trim().toLowerCase();
                        break;
                    case 'l':
                    case 'L':
                    case 'n':
                    case 'N':
                        feld = lastName.trim().toLowerCase();
                        break;
                }
                if (feld != null && aliasFormat.charAt(i + 2) > '0' && aliasFormat.charAt(i + 2) <= '9') {
                    int pos = aliasFormat.charAt(i + 2) - '0';
                    if (feld.length() >= pos) {
                        s += feld.charAt(pos - 1);
                    }
                }
                i += 3;
            } else {
                s += aliasFormat.charAt(i);
            }
            i++;
        }
        return s;
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            foo();
        }
    }

    public static String trimto(String s, int maxchar) {
        return trimto(s, maxchar, false);
    }

    public static String trimto(String s, int maxchar, boolean addDots) {
        if (s == null) {
            return null;
        }
        if (s.length() <= maxchar) {
            return s;
        }
        return (addDots ?
            s.substring(0, maxchar-3) + "..."
            : s.substring(0, maxchar));
    }

    public static String wrapString(String s, int maxchar) {
        StringBuffer str = new StringBuffer();
        while (s.length() > 0) {
            s = s.trim();
            int pos = -1;
            for (int i=0; i<s.length() && (i < maxchar || pos < 0); i++) {
                if (s.charAt(i) == ' ') {
                    pos = i;
                }
            }
            if (pos > 0) {
                str.append((str.length() > 0 ? "\n" : "") + s.substring(0, pos));
                s = s.substring(pos+1);
            } else {
                str.append(s);
                s = "";
            }
        }
        return str.toString();
    }

    public static XMLReader tryToGetXMLReader(String classname) {
        XMLReader parser = null;
        try {
            if (classname != null) {
                if (Logger.isTraceOn(Logger.TT_XMLFILE)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLPARSER,
                            "Trying to load XML-Parser " + classname + " ...");
                }
                parser = XMLReaderFactory.createXMLReader(classname);
            } else {
                if (Logger.isTraceOn(Logger.TT_XMLFILE)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLPARSER,
                            "Trying to load default XML-Parser ...");
                }
                parser = XMLReaderFactory.createXMLReader();
            }
        } catch (Exception e) {
            if (Logger.isTraceOn(Logger.TT_XMLFILE)) {
                Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLPARSER,
                        "Parser Exception: " + e.toString());
            }
            if (e.getClass().toString().indexOf("java.lang.ClassNotFoundException") > 0) {
                if (Logger.isTraceOn(Logger.TT_XMLFILE)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLPARSER,
                            classname + " not found.");
                }
                parser = null;
            }
        }
        if (Logger.isTraceOn(Logger.TT_XMLFILE)) {
            Logger.log(Logger.DEBUG, Logger.MSG_FILE_XMLPARSER,
                    "XML-Parser successfully loaded.");
        }
        return parser;
    }

    public synchronized static XMLReader getXMLReader() {
        XMLReader parser = null;
        parser = tryToGetXMLReader(null);
        if (parser == null) {
            parser = tryToGetXMLReader("org.apache.xerces.parsers.SAXParser");
        }
        if (parser == null) {
            parser = tryToGetXMLReader("javax.xml.parsers.SAXParser"); // since Java 1.5
        }
        if (parser == null) {
            parser = tryToGetXMLReader("org.apache.crimson.parser.XMLReaderImpl");
        }
        if (parser == null) {
            Logger.log(Logger.ERROR, Logger.MSG_ERR_NOXMLPARSER,
                    "No XML-Parser found!");
        }
        return parser;
    }

    public static boolean execCmd(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static String correctUrl(String url) {
        int pos = url.indexOf(":");
        if (pos < 4 || pos > 10) { // http, https, file, mailto, ftp, ...
            url = "file:" + url;
        }
        return EfaUtil.replace(url, "\\", "/", true);
    }

    public static String saveImage(String image, String format, String dir, 
            boolean urlNotation, boolean forceOverwrite, boolean useAbsolutePath) {
        String fname = dir + (dir.endsWith(Daten.fileSep) ? "" : Daten.fileSep) + image;
        if (forceOverwrite || !EfaUtil.canOpenFile(fname)) {
            try {
                BufferedImage img = javax.imageio.ImageIO.read(EfaUtil.class.getResource(Daten.IMAGEPATH + image));
                javax.imageio.ImageIO.write(img, format, new File(fname));
            } catch (Exception e) {
                Logger.logdebug(e);
            }
        }
        if (!useAbsolutePath) {
            fname = image;
        }
        if (urlNotation) {
            if (Daten.fileSep.equals("\\")) {
                fname = (useAbsolutePath ? "/" : "") + EfaUtil.replace(fname, "\\", "/", true);
            }
            return (useAbsolutePath ? "file:" : "") + fname;
        } else {
            return fname;
        }
    }

    public static String saveFile(String file, String dir,
            boolean urlNotation, boolean forceOverwrite, boolean useAbsolutePath) {
        String fname = dir + (dir.endsWith(Daten.fileSep) ? "" : Daten.fileSep) + file;
        if (forceOverwrite || !EfaUtil.canOpenFile(fname)) {
            try {
                InputStream in = EfaUtil.class.getResourceAsStream(Daten.FILEPATH + file);
                FileOutputStream out = new FileOutputStream(new File(fname));
                byte[] data = new byte[8192];
                int read;
                while ( (read = in.read(data)) > 0) {
                    out.write(data, 0, read);
                }
                in.close();
                out.close();
            } catch (Exception e) {
                Logger.logdebug(e);
            }
        }
        if (!useAbsolutePath) {
            fname = file;
        }
        if (urlNotation) {
            if (Daten.fileSep.equals("\\")) {
                fname = (useAbsolutePath ? "/" : "") + EfaUtil.replace(fname, "\\", "/", true);
            }
            return (useAbsolutePath ? "file:" : "") + fname;
        } else {
            return fname;
        }
    }

    public static void setThreadName(Thread t, String name) {
        if (CrontabThread.CRONJOB_THREAD_NAME.equals(Thread.currentThread().getName())) {
            t.setName(CrontabThread.CRONJOB_THREAD_NAME);
        } else {
            t.setName(name);
        }
    }

    public static String readFile(String filename, String encoding) {
        StringBuilder txt = new StringBuilder();
        try {
            BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
            String s;
            while ( (s = f.readLine()) != null) {
                txt.append(s + "\n");
            }
            f.close();
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return txt.toString();
    }

    public static String getStackTrace(Thread t) {
        if (t == null) {
            return null;
        }
        StringBuilder s = new StringBuilder();
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; stack != null && i < stack.length; i++) {
            s.append( (s.length() > 0 ? " -> " : "") + stack[i].toString());
        }
        return s.toString();
    }
    
    public static boolean isValidEmail(String email) {
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
            String domain = email.substring(email.indexOf("@")+1);
            if (domain.indexOf(".") < 0) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            return false;
        }        
    }
    
    public static String transformNameParts(String s) {
        StringBuilder x = new StringBuilder();
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c) || c == '.' || c == '-') {
                x.append("X"); // regular letter or name component character
            } else x.append(c);
        }
        return x.toString();
    }
    
    public static String getBodyFromURL(String url) {
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.connect();
            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            StringBuilder s = new StringBuilder();
            while (in.available() > 0) {
                byte[] data = new byte[in.available()];
                in.read(data);
                s.append(new String(data));
            }
            in.close();
            Pattern p = Pattern.compile(".*<body[^>]*>(.+)</body>.*", Pattern.DOTALL);
            Matcher m = p.matcher(s.toString());
            if (m.matches()) {
                return m.group(1);
            }
        } catch(Exception e) {
            Logger.log(e);
        }
        return null;
    }

    
    /*
     * Calculates the remaining minutes until today, 23:59:00
     */
    public static long getRemainingMinutesToday() {
    	
    	long value = 0;
    	
    	DataTypeTime nowTime = DataTypeTime.now();

    	value = (23-nowTime.getHour())*60; // 60 minutes per Hour
    	value = value + (59 - nowTime.getMinute());
    	
    	return value;
    	
    }    

    /**
     * Efa uses buttons which are filled with a color. 
     * Not all LookAndFeels support this natively. This method ensures that color-filled buttons 
     * are shown correctly in each standard LookAndFeel.
     * @param button
     */
    public static void handleButtonOpaqueForLookAndFeels(JButton button) {
        if (!Daten.lookAndFeel.endsWith(Daten.LAF_METAL) &&
        		!Daten.lookAndFeel.endsWith(Daten.LAF_WINDOWS_CLASSIC) && 
        		!Daten.isEfaFlatLafActive()) {
        	button.setContentAreaFilled(true);       
        }
    	
    	if (Daten.lookAndFeel.endsWith(Daten.LAF_WINDOWS)||Daten.lookAndFeel.endsWith(Daten.LAF_LINUX_GTK)) {
        	button.setBorderPainted(true);// leads to full display of the color on the button canvas
        	button.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        	button.setContentAreaFilled(false);   
        	button.setOpaque(true);
        }                	
    }    
    
    public static void handleTabbedPaneBackgroundColorForLookAndFeels() {
	    if ( Daten.efaConfig.getHeaderUseForTabbedPanes()==true &&		    
	    		(Daten.lookAndFeel.endsWith(Daten.LAF_METAL)||
	    		 Daten.lookAndFeel.endsWith(Daten.LAF_WINDOWS_CLASSIC))){
			UIManager.put("TabbedPane.selectedForeground", Daten.efaConfig.getHeaderForegroundColor());
		    UIManager.put("TabbedPane.selectedBackground", Daten.efaConfig.getHeaderBackgroundColor());
		    UIManager.put("TabbedPane.selected", Daten.efaConfig.getHeaderBackgroundColor());
	    }
    }
    


	/**
	 * Creates a string array containing all font family names _installed_ on the current system
	 * 
	 * @param showAllFonts determines whether all fonts shall be returned (true), or just a list of  
	 * well-known UI-suitable font names. 
	 * 
	 * @param DEFAULT_FONT_NAME if != null, it is added to the font families list.
	 * 
	 * @return String array with all installed font family names on the current system.
	 */
	public static String[] makeFontFamilyArray(Boolean showAllFonts, String DEFAULT_FONT_NAME) {
    	Vector <String>fontFamilies = makeFontFamilyVector(showAllFonts, DEFAULT_FONT_NAME);
    	String[] fontFamiliesArray =new String[fontFamilies.size()];

    	fontFamilies.toArray(fontFamiliesArray);
        return fontFamiliesArray;
	}    

	/**
	 * Creates a String vector containing all font family names _installed_ on the current system
	 * 
	 * @param showAllFonts determines whether all fonts shall be returned (true), or just a list of  
	 * well-known UI-suitable font names. 
	 * 
	 * @param DEFAULT_FONT_NAME if != null, it is added to the font families list.
	 * 
	 * @return Vector with all installed font family names on the current system.
	 */	
	public static Vector <String>makeFontFamilyVector (Boolean showAllFonts, String DEFAULT_FONT_NAME) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] allFonts = graphicsEnvironment.getAllFonts();
        String guiFontRegexp="arial.*|dialog|roboto.*|tahoma.*|trebuchet.*|.*inter.*|.*sansserif|segoe.ui.*|verdana.*|cantarell.*|dejavu.*|liberation.*|.*piboto.*|noto.sans|noto.sans.display.*|quicksand.*";        
        Vector <String>fontFamilies = new Vector<String>();
        
        for (Font font : allFonts) {
            //avoid duplicates, as allFonts contains all permutations of installed font families and their style.
        	if (!fontFamilies.contains(font.getFamily())) {
            	if (showAllFonts==true) {
            		fontFamilies.add(font.getFamily());
            	} else {
            		String curFamily=font.getFamily();
            		if (curFamily.toLowerCase().matches(guiFontRegexp)) {
            			fontFamilies.add(curFamily);
            		}
            	}
            }
        }
        if (DEFAULT_FONT_NAME!=null) {
        	fontFamilies.add(DEFAULT_FONT_NAME);
        }
    	Collections.sort(fontFamilies,new EfaSortStringComparator());
    	return fontFamilies;
	}

	/**
	 * Creates a colored pie chart icon. Each specified color takes 1/nth of the pie
	 * @param colors Array of Colors to be used. Any color item must not be null.
	 * @param iconWidth Width of the icon. Must not be 0.
	 * @param iconHeight Height of the icon. Must not be 0. 
	 * @return Icon 
	 */
	public static ImageIcon createColorPieIcon(Color[] colors, int iconWidth, int iconHeight) {
	    BufferedImage image = new BufferedImage(iconWidth, iconHeight,
	            BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = image.createGraphics();
	    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (colors.length == 1) {
	        g.setColor(colors[0]);
	        g.fillOval(0, 0, iconWidth, iconHeight);
	    } else {
	        int currentAngle = 90;
	        int anglePerColor = 360 / colors.length;
	        for (int i=0; i<colors.length; i++) {
	            g.setColor(colors[i]);
	            g.fillArc(0, 0, iconWidth, iconHeight,
	                    currentAngle % 360, anglePerColor);
	            currentAngle += anglePerColor;
	        }
	    }
		return new ImageIcon(image);
	}	
	
    /**
     * Helper class to display a notification message.
     * If this is a GUI application, we asynchronously display a dialog through
     * SwingUtilities.invokeLater in a separate thread. If this is not a GUI application,
     * we will synchronously in the calling thread invoke the logging method.
     */
    public static abstract class UserMessage {

        public abstract void run();

        public static void show(UserMessage m) {
            if (Daten.isGuiAppl()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        m.run();
                    }
                });
            } else {
                m.run();
            }
        }

    }	
    public static Color lighter(Color theColor, float percent) {
    	int red=Math.round(theColor.getRed()*(1+(percent/100)));
    	int green=Math.round(theColor.getGreen()*(1+(percent/100)));
    	int blue=Math.round(theColor.getBlue()*(1+(percent/100)));
        return new Color(red, green, blue);	
    }

    public static Color darker(Color theColor, float percent) {
    	int red=theColor.getRed();
    	red = Math.max(0, red-(int)Math.round(red*percent/100));
    	int green=theColor.getGreen();
    	green=Math.max(0, green-(int)Math.round(green*percent/100));
    	int blue=theColor.getBlue();
    	blue=Math.max(0, blue-(int)Math.round(green*percent/100));
        return new Color(red, green, blue);	
    }

    /**
     * If a filename starts with a symbolic item, this function extends the filename's path
     * to the absolute path of the corresponding directory.
     * 
     * ./   --> efaDataDirectory
     * ~/   --> user home direcotry
     * 
     * @param filename
     * @return filename with symbolic links replaced by absolute paths. If no symbolic link is at the start of the filename, the filename is returned unchanged. 
     */
    public static String extendFilenameWithRelativePath(String filename) {
    	if (filename!=null) {
		    if (filename.startsWith("./") || filename.startsWith(".\\")) {
		    	// ./ or .\ specify relative path to data directory.
		    	filename=Daten.efaConfig.getValueEfaUserDirectory()+filename.substring(2);
		    }
		    
		    if (filename.startsWith("~/") || filename.startsWith("~\\")) {
		    	filename=Daten.userHomeDir + filename.substring(2);
		    }
    	}
	    return filename;
    }
    
    /**
     * Repairs filenames. 
     * If a filename does not contain a path element, the efa tmp directory path is added as prefix.
     * If the filename points to a windows d:filename.txt, the path is corrected to d:\filename.txt.
     * @param filename
     * @return
     */
	public static String correctFilePath(String filename) {
		if (filename!=null) {

			String filePath=EfaUtil.getPathOfFile(filename);
			if (filePath == null || filePath.trim().isEmpty()){
	        	//filename does not contain a path element? use efa temp dir as path.
	        	return Daten.efaTmpDirectory+filename; //efaTmpDirecory always has a path separator char as suffix. so we don't need one here.
			}	

			if (!new File(filePath).isAbsolute()) {
				//relative Pfade zum efa-Datenverzeichnis erstellen.
				return Daten.efaConfig.getValueEfaUserDirectory()+filePath+File.separator+getNameOfFile(filename);
			} 
			
			if (!filePath.endsWith(File.separator)) {
        		filePath = filePath+File.separator;
        		return filePath +new File(filename).getName();
			}
    	}
       
		return filename;
	}    
    
    public static void main(String args[]) {
        String text = "abc & def";
        System.out.println(text + " -> EfaUtil.escapeXml() = " + EfaUtil.escapeXml(text));
        System.out.println(replaceListByList("xÄxÖxÜxäxöxüxßx","äöüÄÖÜß","aouAOUs"));
        System.out.println("test@domain: " + isValidEmail("test@domain"));
        System.out.println("test@domain.com: " + isValidEmail("test@domain.com"));
    }
}
