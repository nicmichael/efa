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

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.storage.EfaCloudStorage;

import java.io.File;
import java.io.UnsupportedEncodingException;

// import java.nio.charset.StandardCharsets;  Java 8 only
// import java.util.Base64;  Java 8 only

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Vector;

/**
 * A container class for data modifications passed to the efaClod Server.
 */
public class Transaction {

    public enum TX_TYPE {
        INSERT("insert", true, false,true, false, true), UPDATE("update", true, false,true, false,
                true), DELETE("delete", false, false,true, false, true), SELECT("select", false, false,false, false, true), SYNCH("synch", false, false,false, true, true), LIST(
                "list", false, false,false, false, false), NOP("nop", false, false,false, true, false),
                 VERIFY("verify", false, false,false, false, false), BACKUP("backup", false,
                false,false, false, false), UPLOAD("upload", false, false,false, false, false), CRONJOBS("cronjobs", false, false,false,
                false, false);

        final String typeString;
        final boolean isInsertOrUpdate;
        final boolean isTableStructureEdit;
        final boolean isWriteAction;
        final boolean isAllowedOnAuthenticate;
        final boolean addBookName;

        TX_TYPE(String typeString, boolean isInsertOrUpdate, boolean isTableStructureEdit, boolean isWriteAction, boolean isAllowedOnAuthenticate,
                boolean addBookName) {
            this.typeString = typeString;
            this.isInsertOrUpdate = isInsertOrUpdate;
            this.isTableStructureEdit = isTableStructureEdit;
            this.isWriteAction = isWriteAction;
            this.isAllowedOnAuthenticate = isAllowedOnAuthenticate;
            this.addBookName = addBookName;
        }

    }

    // The transaction response codes as text
    static final HashMap<Integer, String> TX_RESULT_CODES = new HashMap<Integer, String>();

    static {
        TX_RESULT_CODES.put(300, "Transaction completed");
        TX_RESULT_CODES.put(301, "Container parsed. User yet to be verified"); // server side internal code
        TX_RESULT_CODES.put(302, "API version of container not supported. Maximum API level exceeded");
        TX_RESULT_CODES.put(303, "Transaction completed with key fixed");
        TX_RESULT_CODES.put(304, "Transaction forbidden");  // semantic data error in valid transaction
        TX_RESULT_CODES.put(400, "XHTTPrequest Error"); // (client side generated error, javascript version only)
        TX_RESULT_CODES.put(401, "Syntax error");
        TX_RESULT_CODES.put(402, "Unknown client");
        TX_RESULT_CODES.put(403, "Authentication failed");
        TX_RESULT_CODES.put(404, "Server side busy");
        TX_RESULT_CODES.put(405, "Wrong transaction ID"); // (client side generated error)
        TX_RESULT_CODES.put(406, "Overload detected");
        TX_RESULT_CODES.put(407, "No data base connection");
        TX_RESULT_CODES.put(500, "Internal server error");
        TX_RESULT_CODES.put(501, "Transaction invalid");
        TX_RESULT_CODES.put(502, "Transaction failed");
        TX_RESULT_CODES.put(503, "No server response in returned transaction response container");
        TX_RESULT_CODES.put(504, "Transaction response container failed");
        TX_RESULT_CODES.put(505, "Server response empty");
        TX_RESULT_CODES.put(506, "Internet connection aborted");
        TX_RESULT_CODES.put(507, "Could not decode server response");
    }

    // the MESSAGE_SEPARATOR_STRING must not contain any regex special character
    // because it is used in "split()" calls as "regex" argument.
    public static final String MESSAGE_SEPARATOR_STRING = "\n|-eFa-|\n";
    public static final String MESSAGE_SEPARATOR_STRING_REGEX = "\n\\|\\-eFa\\-\\|\n";
    public static final String MS_REPLACEMENT_STRING = "\n|-efa-|\n";
    // transaction ID
    public final int ID;
    public final long createdAt;
    // status control
    private long sentAt = 0;
    private long retries = 0;
    private long resultAt = 0;
    private long closedAt = 0;
    // transaction request
    public final TX_TYPE type;
    public final String tablename;
    /**
     * record is an array of n "key;value" pairs, all entries csv encoded. They can just be appended to a csv-encoded
     * String to build a valid transaction String.
     */
    private final String[] record;
    // transaction result
    private int resultCode = 0;
    private String resultMessage = "";
    // transaction container result. This is put to the transaction when the container is aborted.
    private int cresultCode = 0;
    private String cresultMessage = "";

    /**
     * Create an internet access request for the InternetAccessManager for a bundle of transaction messages.
     *
     * @param txs the transactions to be encoded.
     * @param credentials the credentials which shall be used.
     * @return the properly formatted IAM message.
     */
    static TaskManager.RequestMessage createIamRequest(Vector<Transaction> txs, String credentials) {

        if (txs == null || txs.size() == 0)
            return null;
        char d = TxRequestQueue.TX_REQ_DELIMITER;
        // Create the transaction container.
        TxRequestQueue txq = TxRequestQueue.getInstance();
        StringBuilder txContainer = new StringBuilder();
        txContainer.append(TxRequestQueue.efa_cloud_used_api_version).append(d).append(TxRequestQueue.getTxcId()).append(d)
                .append(credentials);
        for (Transaction tx : txs) {
            tx.appendTxPostString(txContainer);
            txq.logApiMessage("tx" + tx.ID + ", " + tx.type + " [" + tx.tablename + "]: Transaction sent. Record length: "
                    + ((tx.record == null) ? "null" : "" + tx.record.length), 0);
            txContainer.append(MESSAGE_SEPARATOR_STRING);
        }
        String txContainerStr = txContainer.toString();
        txContainerStr = txContainerStr.substring(0, txContainerStr.length() - MESSAGE_SEPARATOR_STRING.length());

        // uncomment for debugging
        // TextResource.writeContents("/ramdisk/tmp", txContainerStr, true);

        // encode the transaction message container
        String txContainerBase64 = "";
        try {
            // Java8: txContainerBase64 = Base64.getEncoder().encodeToString(txContainerStr.getBytes("UTF-8"));
            txContainerBase64 = Base64.encodeBytes(txContainerStr.getBytes("UTF-8")); // Java6
        } catch (UnsupportedEncodingException ignored) {
        }

        // create the txContainerBase64efa which needs no further URL encoding.
        String txContainerBase64efa = txContainerBase64.replace('/', '-').replace('+', '*').replace('=', '_');
        // send it to the internet access manager.
        String postURLplus = txq.efaCloudUrl + "?txc=" + txContainerBase64efa;
        // For the InternetAccessManager semantics of a RequestMessage, see InternetAccessManager
        // class information.
        return new TaskManager.RequestMessage(postURLplus, "", InternetAccessManager.TYPE_POST_PARAMETERS, 0.0, txq);
    }

    /**
     * Create a single NOP request used for check of connectivity when activating efaCloud (Does not use the request
     * queue, but runs synchronously.)
     * @param credentials the user credentials, end with a ';'
     * @return the container to be added, already encoded
     */
    public static String createSingleNopRequestContainer(String credentials) {
        char d = TxRequestQueue.TX_REQ_DELIMITER;
        StringBuilder txContainer = new StringBuilder();
        txContainer.append(TxRequestQueue.efa_cloud_used_api_version).append(d)
                .append(TxRequestQueue.getTxcId()).append(d).append(credentials);
        txContainer.append(1).append(d)        //
                .append(0).append(d)     //
                .append(TX_TYPE.NOP).append(d)        //
                .append("@All");  //  if no record is available there must be no dangling ';'
        // encode the transaction message container
        String txContainerBase64 = "";
        try {
            // Java8: txContainerBase64 = Base64.getEncoder().encodeToString(txContainerStr.getBytes("UTF-8"));
            txContainerBase64 = Base64.encodeBytes(txContainer.toString().getBytes("UTF-8")); // Java6
        } catch (UnsupportedEncodingException ignored) {
        }
        // create the txContainerBase64efa which needs no further URL encoding.
        return txContainerBase64.replace('/', '-').replace('+', '*').replace('=', '_');
    }

    /**
     * Constructor. Creates the transaction and adds a new transaction ID.
     *
     * @param ID     ID of transaction to be created. Set -1 to auto increment the ID
     * @param type   type of transaction to be created
     * @param record record data of the transaction to be created. An array of n "key;value" pairs, all entries csv
     *               encoded. They can just be appended to a csv-encoded String to build a valid transaction String.
     */
    Transaction(int ID, TX_TYPE type, String tablename, String[] record) {
        this.ID = (ID == -1) ? TxRequestQueue.getTxId() : ID;
        this.createdAt = System.currentTimeMillis();
        this.type = type;
        this.tablename = tablename;
        // add the logbook's name, if required
        if (type.addBookName && tablename.equalsIgnoreCase("efa2logbook")) {
            EfaCloudStorage ecs = Daten.tableBuilder.getPersistence(tablename);
            String logbookname;
            if (ecs != null) {
                // e.g. /home/efa2/data/project/2021.efa2logbook or \efa2\data\project\2021.efa2logbook
                // default is the name of open storage which may differ from the project records current logbook
                // in particular during the automatic logbook change process at new year.
                String logbookFname = ecs.getFilename().substring(ecs.getFilename().lastIndexOf(File.separatorChar) + 1);
                logbookname = logbookFname.split("\\.")[0];
            } else
                 logbookname = ((Daten.project != null) &&
                    (Daten.project.getCurrentLogbook() != null)) ? Daten.project.getCurrentLogbook().getName() : "nicht_definiert";
            String[] extendedRecord;
            if ((record == null) || (record.length == 0))
                extendedRecord = new String[]{"Logbookname;" + logbookname};
            else {
                extendedRecord = new String[record.length + 1];
                System.arraycopy(record, 0, extendedRecord, 0, record.length);
                extendedRecord[record.length] = "Logbookname;" + logbookname;
            }
            this.record = extendedRecord;
        } else
            // add the clubworkbook's name, if required
            if (type.addBookName && tablename.equalsIgnoreCase("efa2clubwork")) {
                EfaCloudStorage ecs = Daten.tableBuilder.getPersistence(tablename);
                String clubworkbookname;
                if (ecs != null) {
                    // See comments for logbookname above.
                    String clubworkbookFname = ecs.getFilename().substring(ecs.getFilename().lastIndexOf(File.separatorChar) + 1);
                    clubworkbookname = clubworkbookFname.split("\\.")[0];
                } else
                    clubworkbookname = ((Daten.project != null) &&
                        (Daten.project.getCurrentClubwork() != null)) ? Daten.project.getCurrentClubwork().getName() : "nicht_definiert";
                String[] extendedRecord;
                if ((record == null) || (record.length == 0))
                    extendedRecord = new String[]{"ClubworkbookName;" + clubworkbookname};
                else {
                    extendedRecord = new String[record.length + 1];
                    System.arraycopy(record, 0, extendedRecord, 0, record.length);
                    extendedRecord[record.length] = "ClubworkbookName;" + clubworkbookname;
                }
                this.record = extendedRecord;
        } else this.record = record;
    }

    /**
     * Find out whether this transaction has a record
     *
     * @return true, if a record with al least one key;value - pair is contained in the transaction.
     */
    boolean hasRecord() {
        return (this.record != null) && (this.record.length > 0);
    }

    /**
     * <p>Compile a transaction String for permanent storage</p><p>Append the full transaction String including the
     * record part to the provided StringBuilder. Use it to store a transaction to a file. Read it back with
     * parseFullTxString().</p><p>The format is a csv String having as elements #1-#7 ID, sentAt, retries, resultAt,
     * closedAt, type, tablename. The one before last and last elements are the result code and the result message.
     * Inbetween those and element #7 there is an even set of elements representing the record as 'key;value'
     * pairs.</p>
     *
     * @param toAppendTo StringBuilder to append the transaction to
     * @param maxlength  maximum length of String. Cut only for logging. Set 0 if you want to get the really full
     *                   String
     */
    void appendTxFullString(StringBuilder toAppendTo, int maxlength) {
        char d = CsvCodec.DEFAULT_DELIMITER;
        // append request header
        toAppendTo.append(ID).append(d)        //
                .append(sentAt).append(d)      // transaction control, all numeric.
                .append(retries).append(d)     //
                .append(resultAt).append(d)    //
                .append(closedAt).append(d)    //
                .append(type).append(d)        // the remainder of the request header.
                .append(tablename);  //
        // append request record, if available
        if (hasRecord()) {
            toAppendTo.append(d);
            appendTxRecordString(toAppendTo, maxlength);
        }
        // append request result
        toAppendTo.append(d).append(resultCode).append(d)  // result code
                .append(CsvCodec.encodeElement(resultMessage, d, CsvCodec.DEFAULT_QUOTATION));  // and result message.
    }

    /**
     * append the full transaction String to the provided StringBuilder.
     *
     * @param toAppendTo StringBuilder to append the transaction to
     */
    private void appendTxPostString(StringBuilder toAppendTo) {
        char d = TxRequestQueue.TX_REQ_DELIMITER;
        // append request header
        toAppendTo.append(ID).append(d)        //
                .append(retries).append(d)     //
                .append(type).append(d)        //
                .append(tablename);  //  if no record is available there must be no dangling ';'
        // append request record, if available
        if (hasRecord()) {
            toAppendTo.append(d);
            appendTxRecordString(toAppendTo, 0);
        }
    }

    /**
     * Shorten all fields of a record to the given max length
     *
     * @param maxLengthField maximum length of a field's value in the failed queue
     */
    public void shortenRecord(int maxLengthField) {
        if (record == null)
            return;
        for (int i = 0; i < record.length; i++) {   // request record
            if ((record[i] != null) && !record[i].trim().isEmpty()) {
                ArrayList<String> fnv = CsvCodec.splitEntries(record[i].trim());
                String field = fnv.get(0);
                String value;
                if (fnv.size() < 2)
                    value = "";
                else
                    value = fnv.get(1);
                // cut the value to the maximum length for data write transactions
                if (value.length() > maxLengthField) {
                    value = value.substring(0, maxLengthField);
                    record[i] = field + ";" +
                            CsvCodec.encodeElement(value, CsvCodec.DEFAULT_DELIMITER, CsvCodec.DEFAULT_QUOTATION);
                }
            }
        }
    }

    /**
     * Append the transaction record to the provided StringBuilder. The end is a record value, no delimiter.
     *
     * @param toAppendTo StringBuilder to append the transaction to
     * @param maxlength  maximum length of String. Cut only for logging. Set 0 if you want to get the really full
     *                   String
     */
    private void appendTxRecordString(StringBuilder toAppendTo, int maxlength) {
        char d = TxRequestQueue.TX_REQ_DELIMITER;
        int k = 0;
        for (String r : record) {   // request record
            if ((r!= null) && !r.trim().isEmpty()) {
                ArrayList<String> fnv = CsvCodec.splitEntries(r);
                String field = fnv.get(0);
                String value;
                if (fnv.size() < 2)
                    value = "";
                else
                    value = fnv.get(1);
                // cut the value to the maximum length for data write transactions
                if (type.isInsertOrUpdate)
                    value = Daten.tableBuilder.adjustForEfaCloudStorage(value, tablename, field);
                if ((maxlength > 0) && (value.length() > maxlength))
                    value = value.substring(0, maxlength);
                // ensure the message separator String will not be contained.
                if (value != null && value.contains(Transaction.MESSAGE_SEPARATOR_STRING))
                    value = value.replace(Transaction.MESSAGE_SEPARATOR_STRING, Transaction.MS_REPLACEMENT_STRING);
                // append value
                toAppendTo.append(field).append(";")
                        .append(CsvCodec.encodeElement(value, CsvCodec.DEFAULT_DELIMITER, CsvCodec.DEFAULT_QUOTATION));
                k++;
                // do not add a delimiter at the very end.
                if (k < record.length)
                    toAppendTo.append(d);
            }
        }
    }

    /**
     * Create a String for Debug purposes
     *
     * @return a String for Debug purposes
     */
    public String toString() {
        return ID + ":" + type + " " + tablename;
    }

    long getSentAt() {
        return sentAt;
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }

    long getRetries() {
        return retries;
    }

    void setRetries(long retries) {
        this.retries = retries;
    }

    void setResultAt(long resultAt) {
        this.resultAt = resultAt;
    }

    void setClosedAt(long closedAt) {
        this.closedAt = closedAt;
    }

    void setCresultCode(int cresultCode) {
        this.cresultCode = cresultCode;
    }

    void setCresultMessage(String cresultMessage) {
        this.cresultMessage = cresultMessage;
    }

}
