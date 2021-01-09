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

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * TextResource is a convenient loader for text file resources within the JAR package. The reading is performed once
 * upon instantiation, and the the resource can be read as a String
 */
public class TextResource {

    /**
     * <p>
     * Fetch the entire contents of a text file, and return it in a String. This style of implementation does not throw
     * Exceptions to the caller, but returns a null String, when hitting an IOException.
     * </p>
     *
     * <pre>
     * Charset     Description
     * US-ASCII    Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of
     *             the Unicode character set
     * ISO-8859-1  ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
     * UTF-8       Eight-bit UCS Transformation Format
     * UTF-16BE    Sixteen-bit UCS Transformation Format, big-endian byte order
     * UTF-16LE    Sixteen-bit UCS Transformation Format, little-endian byte order
     * UTF-16      Sixteen-bit UCS Transformation Format, byte order identified by
     * an optional byte-order mark
     *
     * <pre>
     * <p>
     * Snippet from http://www.javapractices.com/topic/TopicAction.do?Id=42
     * modified. Returns false, if not successful.
     * </p>
     *
     * @param aFile File to be read
     * @param charSetName character set to be used. Set null for default character set
     * @return false, if not successful.
     */
    public static String getContents(File aFile, String charSetName) {
        // ...checks on aFile are omitted
        StringBuilder contents = new StringBuilder();
        Charset cs = ((charSetName == null) || charSetName.isEmpty()) ? Charset.defaultCharset() : Charset
                .forName(charSetName);

        try {
            // use buffering, reading one line at a time
            // FileReader always assumes default encoding is OK!
            BufferedReader input = null;
            try {
                input = new BufferedReader(new InputStreamReader(new FileInputStream(aFile), cs));
            } catch (FileNotFoundException ignored) {
            }
            String line; // not declared within while loop
            /*
             * readLine is a bit quirky : it returns the content of a line
             * MINUS the newline. it returns null only for the END of the
             * stream. it returns an empty String if two newlines appear in
             * a row. Texts are using the Linux/Unix notation:
             * \n
             */
            while ((input != null) && (line = input.readLine()) != null) {
                contents.append(line);
                contents.append("\n");
            }
        } catch (IOException ex) {
            return null;
        }
        if (contents.length() == 0)
            return "";
        // remove last "\n" character.
        return contents.substring(0, contents.length() - 1);
    }

    /**
     * <p>
     * Change the contents of text file in its entirety, overwriting any existing text. Encodes UTF-8.
     * </p>
     * <p>
     * If file exists, aContents is appended, if append is set true and overwritten from start if append is set false.
     * Returns false, if an exception was encountered.
     * </p>
     * <p>
     * Method uses a Vector of String. For speed reasons, if the aContents is larger than 20k split into a Vector of
     * multiple Strings < 20k is recommended.
     * </p>
     *
     * @param out       file to put the contents to.
     * @param aContents a Vector of String to be put to the file.
     * @param append    set true to append the contents, false to overwrite the file
     * @param delimiter This String is appended to each element of the Vector when writing the file. Set to null to have
     *                  nothing out between elements. Typical example is "\n" to put an element per line into the file.
     * @return true, if successful.
     */
    public static boolean writeContents(OutputStream out, ArrayList<String> aContents, String charsetName,
                                        boolean append, String delimiter) throws
            IOException {
        if (out == null) {
            return false;
        }
        Charset charset = (charsetName.isEmpty()) ? Charset.defaultCharset() : Charset.forName(charsetName);
        // use buffering
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(out, charset));
        if (append)
            for (String aContent : aContents) {
                output.append(aContent);
                if (delimiter != null)
                    output.append(delimiter);
            }
        else {
            output.write("");
            int lines = 0;
            for (String aContent : aContents) {
                output.append(aContent);
                lines++;
                if (delimiter != null && (lines < aContents.size()))
                    output.append(delimiter);
            }
        }
        output.close();
        return true;
    }

    /**
     * <p>
     * Change the contents of text file in its entirety, overwriting any existing text or appending text to it. Encodes
     * UTF-8. Returns false, if not successful. Convenience shortcut for setLargeContents(aFile, aContents, "UTF-8",
     * append, "\n");
     * </p>
     *
     * @param out       stream to put the contents to.
     * @param aContents a Vector of String to be put to the file.
     * @param append    set true to append the contents, false to overwrite the file
     */
    public static void writeContents(OutputStream out, ArrayList<String> aContents, boolean append) throws IOException {
        writeContents(out, aContents, "UTF-8", append, "\n");
    }

    /**
     * <p>
     * Change the contents of text file in its entirety, overwriting any existing text or appending text to it. Encodes
     * UTF-8. Returns false, if not successful. Convenience variant of other writeContents functions.
     * </p>
     *
     * @param filename  name of file to put the contents to. If the file can not be written to, nothing is done and
     *                  false returned.
     * @param aContents a String to be put to the file.
     * @param append    set true to append the contents, false to overwrite the file
     * @return true, if successful.
     */
    public static boolean writeContents(String filename, String aContents, boolean append) {
        ArrayList<String> aContentsList = new ArrayList<String>();
        aContentsList.add(aContents);
        OutputStream out;
        try {
            out = new FileOutputStream(filename, append);
            return writeContents(out, aContentsList, "UTF-8", append, "\n");
        } catch (Exception e) {
            // e.printStackTrace();
            return false;
        }
    }

}