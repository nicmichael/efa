package de.nmichael.efa.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EfaZIPUtil {
    private static final int ZIP_BUFFER = 2048;

    /**
     * Maximum number of ZIP entries that may be extracted.
     * Protects against archives with an excessive number of files.
     */
    private static final int UNZIP_MAX_ENTRIES = 10000;

    /**
     * Maximum total number of uncompressed bytes that may be extracted.
     * Protects against ZIP bombs with extreme expansion ratios.
     */
    private static final long UNZIP_MAX_TOTAL_UNCOMPRESSED_BYTES = 1024L * 1024L * 1024L; // 1 GiB

    /**
     * Maximum number of uncompressed bytes allowed for a single entry.
     */
    private static final long UNZIP_MAX_ENTRY_UNCOMPRESSED_BYTES = 250L * 1024L * 1024L; // 250 MiB

    /**
     * Maximum allowed compression ratio per entry.
     * Example: compressed 1 KB -> uncompressed max 100 MB if ratio is 100000.
     */
    private static final long UNZIP_MAX_COMPRESSION_RATIO = 1000L;

    /**
     * Maximum number of warning messages to collect.
     * Prevents unbounded memory usage in case of many warnings.
     */
    private static final int UNZIP_MAX_COLLECTED_MESSAGES = 1000;

    /**
     * Describes the overall outcome of an unzip operation.
     */
    public static enum UnzipStatus {
        SUCCESS,
        SUCCESS_WITH_WARNINGS,
        FAILURE
    }

    /**
     * Result object for unzip operations.
     * Contains the outcome status as well as collected errors and warnings.
     */
    public static class UnzipResult {
        private final UnzipStatus status;
        private final ArrayList<String> errors;
        private final ArrayList<String> warnings;

        public UnzipResult(UnzipStatus status, ArrayList<String> errors, ArrayList<String> warnings) {
            this.status = status;
            this.errors = errors;
            this.warnings = warnings;
        }

        public UnzipStatus getStatus() {
            return status;
        }

        public ArrayList<String> getErrors() {
            return errors;
        }

        public ArrayList<String> getWarnings() {
            return warnings;
        }
        
        /**
         * Convenience method for legacy callers that expect a single message string.
         * Returns {@code null} if no errors or warnings were collected.
         */
        public String getWarningsAsString() {
            StringBuilder sb = new StringBuilder();

            if (warnings != null) {
                for (int i = 0; i < warnings.size(); i++) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(warnings.get(i));
                }
            }

            return sb.length() == 0 ? null : sb.toString();
        }

        /**
         * Convenience method for legacy callers that expect a single message string.
         * Returns {@code null} if no errors or warnings were collected.
         */
        public String getErrorsAsString() {
            StringBuilder sb = new StringBuilder();

            if (errors != null) {
                for (int i = 0; i < errors.size(); i++) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(errors.get(i));
                }
            }

            return sb.length() == 0 ? null : sb.toString();
        }

        
        public boolean isSuccess() {
            return status == UnzipStatus.SUCCESS || status == UnzipStatus.SUCCESS_WITH_WARNINGS;
        }

        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }

        /**
         * Convenience method for legacy callers that expect a single message string.
         * Returns {@code null} if no errors or warnings were collected.
         */
        public String toLegacyMessageString() {
            StringBuilder sb = new StringBuilder();

            if (errors != null) {
                for (int i = 0; i < errors.size(); i++) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(errors.get(i));
                }
            }

            if (warnings != null) {
                for (int i = 0; i < warnings.size(); i++) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(warnings.get(i));
                }
            }

            return sb.length() == 0 ? null : sb.toString();
        }
    }
    
    /**
     * Adds a warning message to the warning list while keeping the list bounded.
     *
     * @param warnings warning target list
     * @param message warning text
     */
    private static void addWarning(ArrayList<String> warnings, String message) {
        if (warnings.size() < UNZIP_MAX_COLLECTED_MESSAGES) {
            warnings.add(message);
        }
    }
    
    /**
     * Extracts a ZIP archive into the given destination directory.
     * <p>
     * The returned {@link UnzipResult} contains the overall status, a list of
     * errors and a list of warnings.
     * </p>
     *
     * @param zipFile path to the ZIP archive
     * @param destDir destination directory where the archive content will be extracted
     * @return unzip result object with status, errors and warnings
     */
    public static UnzipResult unzipToResult(String zipFile, String destDir) {
        return unzipToResult(zipFile, destDir, null, null);
    }

    /**
     * Extracts a ZIP archive into the given destination directory.
     * <p>
     * Optionally renames extracted files by replacing a filename postfix.
     * Example: when {@code replaceFilePostfixSource} is {@code ".jar"} and
     * {@code replaceFilePostfixDest} is {@code ".jar.new"}, every extracted file
     * ending with {@code ".jar"} will be written with the new postfix.
     * </p>
     * <p>
     * Security features:
     * </p>
     * <ul>
     *   <li>Prevents ZIP path traversal attacks ("Zip Slip")</li>
     *   <li>Limits the number of entries</li>
     *   <li>Limits total uncompressed size</li>
     *   <li>Limits per-entry uncompressed size</li>
     *   <li>Limits suspicious compression ratios</li>
     * </ul>
     *
     * @param zipFile path to the ZIP archive
     * @param destDir destination directory where the archive content will be extracted
     * @param replaceFilePostfixSource optional source postfix to replace
     * @param replaceFilePostfixDest optional replacement postfix
     * @return unzip result object with status, errors and warnings
     */
    public static UnzipResult unzipToResult(String zipFile, String destDir,
            String replaceFilePostfixSource, String replaceFilePostfixDest) {
        ArrayList<String> errors = new ArrayList<String>();
        ArrayList<String> warnings = new ArrayList<String>();

        File zipArchive = new File(zipFile);
        if (!zipArchive.isFile()) {
            errors.add(LogString.fileNotFound(zipFile, International.getString("ZIP-Archiv")));
            return new UnzipResult(UnzipStatus.FAILURE, errors, warnings);
        }

        File destinationDirectory = new File(destDir);
        if (!destinationDirectory.isDirectory()) {
            errors.add(LogString.directoryDoesNotExist(destDir, International.getString("Ziel-Verzeichnis")));
            return new UnzipResult(UnzipStatus.FAILURE, errors, warnings);
        }

        int processedEntries = 0;
        long totalExtractedBytes = 0L;

        try {
            // Resolve the canonical destination path once.
            // Every extracted entry must remain inside this directory.
            final String destinationCanonicalPath =
                    destinationDirectory.getCanonicalPath() + File.separator;

            try (ZipFile zip = new ZipFile(zipArchive)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    processedEntries++;

                    if (processedEntries > UNZIP_MAX_ENTRIES) {
                        errors.add(LogString.operationFailed(
                                International.getString("Entpacken"),
                                "ZIP archive exceeds the maximum number of allowed entries (" + UNZIP_MAX_ENTRIES + ")."));
                        return new UnzipResult(UnzipStatus.FAILURE, errors, warnings);
                    }

                    String filename = entry.getName();

                    // Optionally extract selected files under a different postfix.
                    if (replaceFilePostfixSource != null
                            && replaceFilePostfixDest != null
                            && filename.endsWith(replaceFilePostfixSource)) {
                        int pos = filename.lastIndexOf(replaceFilePostfixSource);
                        filename = filename.substring(0, pos) + replaceFilePostfixDest;
                    }

                    try {
                        File targetFile = new File(destinationDirectory, filename);
                        String targetCanonicalPath = targetFile.getCanonicalPath();

                        // Protect against Zip Slip / path traversal attacks.
                        if (!targetCanonicalPath.startsWith(destinationCanonicalPath)) {
                            throw new IOException("Illegal ZIP entry outside target directory: " + filename);
                        }

                        if (entry.isDirectory()) {
                            // Ensure directory entries exist before continuing.
                            if (!targetFile.isDirectory() && !targetFile.mkdirs()) {
                                throw new IOException("Could not create directory: " + targetFile.getPath());
                            }

                            // Preserve directory timestamp if possible.
                            if (entry.getTime() > 0 && !targetFile.setLastModified(entry.getTime())) {
                                addWarning(warnings, "Could not preserve directory timestamp for: " + targetFile.getPath());
                            }
                            continue;
                        }

                        // Some ZIP archives do not contain explicit directory entries,
                        // so we always create the parent directory for file entries.
                        File parentDir = targetFile.getParentFile();
                        if (parentDir != null && !parentDir.isDirectory() && !parentDir.mkdirs()) {
                            throw new IOException("Could not create directory: " + parentDir.getPath());
                        }

                        long entryExtractedBytes = 0L;
                        long compressedSize = entry.getCompressedSize();

                        try (BufferedInputStream in = new BufferedInputStream(zip.getInputStream(entry));
                             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile))) {
                            byte[] buf = new byte[ZIP_BUFFER];
                            int read;

                            while ((read = in.read(buf, 0, buf.length)) != -1) {
                                entryExtractedBytes += read;
                                totalExtractedBytes += read;

                                // Limit per-entry size to prevent oversized extracted files.
                                if (entryExtractedBytes > UNZIP_MAX_ENTRY_UNCOMPRESSED_BYTES) {
                                    throw new IOException("ZIP entry exceeds maximum uncompressed size: " + filename);
                                }

                                // Limit total extracted size to prevent archive-wide ZIP bombs.
                                if (totalExtractedBytes > UNZIP_MAX_TOTAL_UNCOMPRESSED_BYTES) {
                                    throw new IOException("ZIP archive exceeds maximum total uncompressed size.");
                                }

                                out.write(buf, 0, read);
                            }
                        } catch (Exception e) {
                            // Best-effort cleanup of partially extracted files.
                            if (targetFile.exists() && !targetFile.delete()) {
                                addWarning(warnings, "Could not delete partially extracted file: " + targetFile.getPath());
                            }
                            throw e;
                        }

                        // If compressed size is known, verify that the entry does not
                        // expand suspiciously beyond the configured compression ratio.
                        if (compressedSize > 0) {
                            long ratio = entryExtractedBytes / compressedSize;
                            if (ratio > UNZIP_MAX_COMPRESSION_RATIO) {
                                if (targetFile.exists() && !targetFile.delete()) {
                                    addWarning(warnings, "Could not delete suspicious file after ZIP bomb detection: " + targetFile.getPath());
                                }
                                throw new IOException("ZIP entry has suspicious compression ratio: " + filename);
                            }
                        } else if (compressedSize == 0 && entryExtractedBytes > 0) {
                            // A zero-byte compressed size with non-zero extracted output is suspicious.
                            if (targetFile.exists() && !targetFile.delete()) {
                                addWarning(warnings, "Could not delete suspicious file after ZIP bomb detection: " + targetFile.getPath());
                            }
                            throw new IOException("ZIP entry has invalid compressed size metadata: " + filename);
                        }

                        // Preserve original file timestamp if present in the ZIP metadata.
                        if (entry.getTime() > 0 && !targetFile.setLastModified(entry.getTime())) {
                            addWarning(warnings, "Could not preserve file timestamp for: " + targetFile.getPath());
                        }
                    } catch (Exception e) {
                        errors.add(LogString.operationFailed(
                                International.getString("Entpacken"),
                                LogString.fileExtractFailed(
                                        filename,
                                        International.getString("Datei"),
                                        e.toString())));
                    }
                }
            }
        } catch (Exception e) {
            errors.add(LogString.operationFailed(International.getString("Entpacken"), e.toString()));
            errors.add(LogString.operationAborted(International.getString("Entpacken")));
        }

        UnzipStatus status;
        if (!errors.isEmpty()) {
            status = UnzipStatus.FAILURE;
        } else if (!warnings.isEmpty()) {
            status = UnzipStatus.SUCCESS_WITH_WARNINGS;
        } else {
            status = UnzipStatus.SUCCESS;
        }

        return new UnzipResult(status, errors, warnings);
    }
}
