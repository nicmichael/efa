/*
 * <pre>
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael, Martin Glade
 * @version 2</pre>
 */

package de.nmichael.efa.data.efacloud;

import java.util.ArrayList;

public class CsvCodec {

    public static final char DEFAULT_DELIMITER = ';';
    public static final char DEFAULT_QUOTATION = '"';

    public static char delimiter = DEFAULT_DELIMITER; // the delimiter of the csv-line, default: ';'
    public static char quotation = DEFAULT_QUOTATION; // the quotation of the list, default: '"'
    /*
     * set true to removes enclosing quotes which are not needed to mask inner carriage returns, delimiters or quotes.
     */
    public static boolean removeSuperfluousQuotes = true;

    /**
     * <p>Static generic method to convert a String to a csv value by replacing by providing
     * quoting, if the String does contain quotes, line breaks or semicolons. Inner quotes are replaced by "". The csv
     * value may contain new line characters.</p>
     *
     * @param input String to be encoded
     * @return encoded String. If the input is null an empty Sting is returned.
     */
    public static String encodeElement(String input, char delimiter, char quotation) {
        if (input == null)
            return "";
        if (((input.indexOf(delimiter) >= 0) || (input.indexOf(quotation) >= 0) || input.contains("\n")))
            return quotation + input.replace("" + quotation, "" + quotation + quotation) + quotation;
        return input;
    }

    /**
     * <p> Decode an element into a String. If the element is quoted, it will be checked, whether
     * this is needed, because of inner quotes, line breaks or delimiters. If so, the element is decoded appropriately.
     * If not, quotes are preserved for all delimiters except " " (arguments have optional quotes).</p>
     * <p>
     * Escape sequences are only recognized for the '\n’ character. If a quoted elementStr contains any other escape
     * sequence, e. g. \h (not \\h), then the control character '\u007f' is returned together with the following. This
     * is used for uBase table entry history tagging. Other uses can be imagined.</p>
     *
     * @param elementStr              String to be parsed
     * @param delimiter               the delimiter of the list, from which the element is taken: for lists: ';', for
     *                                Vectors ',', for argument sets ' '.
     * @param removeSuperfluousQuotes set true to removes enclosing quotes which are not needed to mask inner cariage
     *                                returns, delimiters or quotes.
     * @return decoding result as String
     */
    private static String decodeElement(String elementStr, char delimiter, char quotation,
                                        boolean removeSuperfluousQuotes) {
        if (elementStr.length() < 2)
            return elementStr;
        String decoded;
        // decode element, if quoted
        if ((elementStr.charAt(0) == quotation) && (elementStr.charAt(elementStr.length() - 1) == quotation)) {
            String inStrNQ = elementStr.substring(1, elementStr.length() - 1);
            int lp = inStrNQ.length();
            // decode only if quotes were needed to cover a delimiter, inner quote or line break
            if ((lp > 0) && ((inStrNQ.indexOf(delimiter) >= 0) || (inStrNQ.indexOf(quotation) >= 0) ||
                    (inStrNQ.contains("\n")))) {
                StringBuilder d = new StringBuilder();
                for (int lc = 0; lc < lp; lc++) {
                    char c = inStrNQ.charAt(lc);
                    d.append(c);
                    if (c == quotation)
                        lc++;
                }
                decoded = d.toString();
            } else
                decoded = elementStr;
        } else
            decoded = elementStr;
        if (removeSuperfluousQuotes && (decoded.length() > 1) && (decoded.charAt(0) == quotation) &&
                (decoded.charAt(decoded.length() - 1) == quotation) && (decoded.indexOf(delimiter) < 0) &&
                !decoded.contains("\n") && (decoded.substring(1, decoded.length() - 1).indexOf(quotation) < 0))
            return decoded.substring(1, decoded.length() - 1);
        return decoded;
    }

    /**
     * <p>Splits a csv-line to an array of String. The input is a sequence of
     * literals, separated by a delimiter which may itself be followed by none, one or multiple ' ' or '\n' characters.
     * </p><p>
     * Consecutive delimiter characters like ";;;" or "; ; ;" include empty elements.
     * </p><p>
     * Returns all elements as an ArrayList of String.
     * </p><p>If you want to use other than the default delimiter and quote or want to keep superfluous quotes, set the
     * respective CsvCode fields</p>
     *
     * @param inStr String to be split
     * @return an ArrayList with all entries of the csv-line.
     */
    public static ArrayList<String> splitEntries(String inStr) {
        if (inStr == null)
            return null;
        if (inStr.isEmpty())
            return new ArrayList<String>();
        /*
         * add an empty element here and remove it at the end to simplify the algorithm
         */
        String delimiterStr = String.valueOf(delimiter);
        String parseStr = inStr + delimiterStr + "X";
        /*
         * prepare parsing.
         */
        ArrayList<String> list = new ArrayList<String>();
        StringBuilder element = new StringBuilder();
        int lc = 0;
        char b = ' ';
        char c;
        boolean inBetweenQuotes = false;
        /*
         * parsing loop.
         */
        while (lc < parseStr.length()) {
            c = parseStr.charAt(lc);
            /*
             * add current entry and all following empty entries to list vector.
             * Ignore space and new line characters following the delimiter.
             */
            if ((c == delimiter) && !inBetweenQuotes) {
                // flag to avoid adding empty elements for delimiter being blank
                // or new line
                boolean added = false;
                while ((lc < parseStr.length()) && ((c == delimiter) || (c == '\n'))) {
                    if ((c == delimiter) && !(added && (c == '\n'))) {
                        list.add(decodeElement(element.toString(), delimiter, quotation, removeSuperfluousQuotes));
                        element = new StringBuilder();
                        added = true;
                    }
                    lc++;
                    if (lc < parseStr.length())
                        c = parseStr.charAt(lc);
                    if (lc > 0)
                        b = parseStr.charAt(lc - 1);
                }
            }
            /*
             * add character to tmpList. Close String literal or Vector literal
             */
            else {
                element.append(c);
                // toggle inBetweenQuotes at not escaped '"'
                if ((c == '"') && (b != '\\'))
                    inBetweenQuotes = !inBetweenQuotes;
                lc++;
                b = c;
            }
        }
        // the last element is always the appended "X" character.
        // It will therefore not be appended to the list.
        return list;
    }

    /**
     * Splits a String into csv-lines, by detecting the non-quoted '\n' characters. Returns all lines as an array of
     * String.
     *
     * @param inStr     String to be split
     * @param quotation the quotation of the list, typically '"'.
     * @return an array with all elements of the set.
     */
    public static ArrayList<String> splitLines(String inStr, char quotation) {
        ArrayList<String> lines = new ArrayList<String>();
        boolean inQuotes = false;
        int lastLineStart = 0;
        int endIndex = inStr.length() - 1;
        for (int i = 0; i <= endIndex; i++) {
            char c = inStr.charAt(i);
            if (c == quotation)
                inQuotes = !inQuotes;
            if (!inQuotes && ((c == '\n') || (i == endIndex))) {
                if (i == endIndex)
                    lines.add(inStr.substring(lastLineStart, i + 1));
                else
                    lines.add(inStr.substring(lastLineStart, i));
                lastLineStart = i + 1;
            }
        }
        return lines;
    }

}
