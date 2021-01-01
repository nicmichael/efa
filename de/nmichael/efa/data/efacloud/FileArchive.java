package de.nmichael.efa.data.efacloud;

import java.io.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * File archive is a class to handle a set of files which will be stored in a common zip archive rather than one by
 * one.
 */
public class FileArchive {

    private final ArrayList<VFile> files = new ArrayList<VFile>();
    private final String zipFilePath;
    private final String tempFilePath;
    private final String charset;

    /**
     * Constructor. Takes the zip file path, nothing else.
     *
     * @param zipFilePath    the path to the zip file in which the archive is stored on the file system. Must not be
     *                       changed, if set. May be null, to keep all data in memory.
     * @param charsetForText the character set for all text files.
     */
    public FileArchive(String zipFilePath, String charsetForText) {
        this.zipFilePath = zipFilePath;
        if (zipFilePath != null)
            this.tempFilePath = zipFilePath + "." + System.currentTimeMillis();
        else
            this.tempFilePath = "." + System.currentTimeMillis();
        this.charset = charsetForText;
    }

    /**
     * Simple check:
     */
    public boolean isTextFile(String filename) {
        int fileIndex = getIndex(filename);
        return (fileIndex >= 0) && !files.get(fileIndex).isBinary;
    }

    /**
     * Delete a file from the archive. Returns true, if a file was deleted, false, if the filename had no match.
     *
     * @param filename the name of the file to be deleted.
     * @return true, if a file was deleted, false, if the filename had no match.
     */
    public boolean delete(String filename) {
        int i = 0;
        for (VFile vf : files) {
            if (vf.fileLocation.equals(filename)) {
                files.remove(i);
                return true;
            }
            i++;
        }
        return false;
    }

    /**
     * Return all filenames which fit to the given mask. Wildcard is '*'. A wildcard must be at beginning or end, or
     * both. No inner wildcards are allowed.
     *
     * @param mask mask for file selection.
     * @return all filenames which fit to the given mask
     */
    public Vector<String> getFilenames(String mask) {
        Vector<String> ret = new Vector<String>();
        if (!mask.startsWith("*") && !mask.endsWith("*"))
            return ret;
        mask = mask.toLowerCase(Locale.US);
        if (!mask.startsWith("*")) {
            mask = mask.substring(0, mask.length() - 1);
            for (VFile f : files)
                if (f.fileLocation.toLowerCase(Locale.US).startsWith(mask))
                    ret.add(f.fileLocation);
        } else if (!mask.endsWith("*")) {
            mask = mask.substring(1);
            for (VFile f : files)
                if (f.fileLocation.toLowerCase(Locale.US).endsWith(mask))
                    ret.add(f.fileLocation);
        } else {
            mask = mask.substring(0, mask.length() - 1).substring(1);
            for (VFile f : files)
                if (f.fileLocation.toLowerCase(Locale.US).contains(mask))
                    ret.add(f.fileLocation);
        }
        return ret;
    }

    /**
     * Returns a file index for the file with the given name. If the file is not existing, it will return -1.
     *
     * @param filename name of file. Not case sensitive for compatibility with Windows systems.
     * @return a file index for the file with the given name
     */
    public int getIndex(String filename) {
        if (filename.contains("*"))
            return -1;
        int i = 0;
        for (VFile vf : files) {
            if (vf.fileLocation.equalsIgnoreCase(filename))
                return i;
            i++;
        }
        return  - 1;
    }

    /**
     * Returns a file index for the file with the given name. If the file is not existing, it will create a new one and
     * return the index.
     *
     * @param filename name of file. Not case sensitive for compatibility with Windows systems.
     * @param isBinary set true for binary files.
     * @return a file index for the file with the given name
     */
    public int getInstance(String filename, boolean isBinary) {
        int fileIndex = getIndex(filename);
        if (fileIndex >= 0)
            return fileIndex;
        files.add(new VFile(filename, isBinary));
        return files.size() - 1;
    }

    /**
     * Put a content to a virtual file (full file). You need to specify the encoding for text files before you call this
     * method, because the virtual file will only keep bytes, not a String.
     *
     * @param indexOfFile index of file
     * @param content     either a byte[] array or a String.
     */
    public void putContent(int indexOfFile, Object content) {
        if (files.get(indexOfFile).isBinary) {
            if (content instanceof byte[])
                files.get(indexOfFile).binaryContent = (byte[]) content;
        } else {
            if (content instanceof byte[])
                try {
                    files.get(indexOfFile).textContent = new String((byte[]) content, charset);
                } catch (UnsupportedEncodingException e) {
                    files.get(indexOfFile).textContent = e.getMessage();
                }
            else
                files.get(indexOfFile).textContent = (String) content;
        }
    }

    /**
     * Get raw bytes from a virtual file (full file). You need to do the decoding yourself then.
     *
     * @param indexOfFile index of file
     * @return byte[] with file content.
     */
    public byte[] getBytes(int indexOfFile) {
        return files.get(indexOfFile).binaryContent;
    }

    /**
     * Get a String from a text file (full file).
     *
     * @param indexOfFile index of file
     * @return either a byte[] array or a String.
     */
    public Object getText(int indexOfFile) {
        return files.get(indexOfFile).textContent;
    }

    /**
     * @param extension extension of files to be selected. Not case sensitive. Can include the leading dot '.'.
     * @return all locations of files of which the location ends with the extension. Set extension to an empty String to
     * get all file names.
     */
    public Vector<String> getNames(String extension) {
        Vector<String> names = new Vector<String>();
        for (VFile vf : files)
            if (extension.isEmpty() || vf.fileLocation.toLowerCase(Locale.US).endsWith(extension))
                names.add(vf.fileLocation);
        return names;
    }

    /**
     * Zips and stores the whole archive into the previously defined path.
     *
     * @return true for success, else false
     */
    public synchronized boolean store(boolean showProgress) {

        // virtual archive. Cannot be stored
        if (zipFilePath == null)
            return false;

        // prepare compression & store
        final File zipFile = new File(zipFilePath);
        final File tempFile = new File(tempFilePath);
        ZipOutputStream out;
        try {
            out = new ZipOutputStream(new FileOutputStream(tempFile));
        } catch (FileNotFoundException e2) {
            return false;
        }
        if (showProgress)
            System.out.print("Compressing " + zipFilePath.substring(zipFilePath.lastIndexOf(File.separatorChar) + 1) +
                    "\n ");

        // do compression and store
        for (VFile vf : files) {
            if (showProgress)
                System.out.print(".");
            ZipEntry e = new ZipEntry(vf.fileLocation);
            try {
                out.putNextEntry(e);
                byte[] data = vf.binaryContent;
                if (data != null)
                    out.write(data, 0, data.length);
                out.closeEntry();
            } catch (IOException e1) {
                return false;
            }
        }

        // close output stream. This will also be done, if interrupted.
        try {
            out.close();
        } catch (IOException ignored) {
        }

        //noinspection ResultOfMethodCallIgnored
        zipFile.delete();
        //noinspection ResultOfMethodCallIgnored
        tempFile.renameTo(zipFile);
        if (showProgress)
            System.out.println();
        return true;
    }

    /**
     * get the full arcive as byte array.
     * @return
     */
    public byte[] getZipAsBytes() {
        // prepare compression
        final ByteArrayOutputStream bytesCompressed = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bytesCompressed);
        // do compression
        for (VFile vf : files) {
            ZipEntry e = new ZipEntry(vf.fileLocation);
            try {
                out.putNextEntry(e);
                byte[] data = (vf.isBinary) ? vf.binaryContent : vf.textContent.getBytes();
                if (data != null)
                    out.write(data, 0, data.length);
                out.closeEntry();
            } catch (IOException e1) {
                return null;
            }
        }
        try {
            out.close();
        } catch (IOException ignored) {
        }
        return bytesCompressed.toByteArray();
    }
    /**
     * Reads the zip archive which is expected to be within the "zipFilePath" given in the construction.
     *
     * @return true for success, else false
     */
    public synchronized boolean read() {
        if (zipFilePath == null)
            return false;

        // open the zip file stream
        InputStream zipFile;
        try {
            zipFile = new FileInputStream(zipFilePath);
        } catch (FileNotFoundException e) {
            return false;
        }
        ZipInputStream stream = new ZipInputStream(zipFile);
        try {
            // now iterate through each item in the stream. The get next
            // entry call will return a ZipEntry for each file in the
            // stream
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                // create a buffer to improve copy performance later.
                ArrayList<byte[]> allBytes = new ArrayList<byte[]>();
                ArrayList<Integer> allBytesLength = new ArrayList<Integer>();
                final int slice = 8192;
                int len;
                int total = 0;
                int index = 0;
                allBytes.add(new byte[slice]);
                do {
                    len = stream.read(allBytes.get(index));
                    if (len > 0) {
                        total = total + len;
                        allBytesLength.add(len);
                        allBytes.add(new byte[slice]);
                        index++;
                    }
                } while (len > 0);
                byte[] buffer = new byte[total];
                int startAt = 0;
                for (int i = 0; i < index; i++) {
                    System.arraycopy(allBytes.get(i), 0, buffer, startAt, allBytesLength.get(i));
                    startAt = startAt + allBytesLength.get(i);
                }
                index = this.getIndex(entry.getName());
                files.get(index).binaryContent = buffer;
            }
            // we must always close the zip file.
            stream.close();
        } catch (IOException ioe) {
            return false;
        }
        return true;
    }

    /**
     * Interface to wrap both text and binary files.
     */
    private static class VFile {
        final String fileLocation;
        final boolean isBinary;
        byte[] binaryContent;
        String textContent;
        VFile(String fileLocation, boolean isBinary) {
            this.fileLocation = fileLocation;
            this.isBinary = isBinary;
        }
    }

}