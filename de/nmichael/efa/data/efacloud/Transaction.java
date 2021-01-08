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
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

// import java.nio.charset.StandardCharsets;  Java 8 only
// import java.util.Base64;  Java 8 only

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Date;

/**
 * A container class for data modifications passed to the efaClod Server.
 */
class Transaction {

    enum TX_TYPE {
        CREATETABLE("createtable", false, false, false), ADDCOLUMNS("addcolumns", false, false, false), AUTOINCREMENT(
                "autoincrement", false, false, false), UNIQUE("unique", false, false, false), INSERT("insert", true,
                true, false), UPDATE("update", true, true, false), DELETE("delete", false, true, false), KEYFIXING(
                "keyfixing", false, false, true), SELECT("select", false, false, false), SYNCH("synch", false, false,
                true), LIST("list", false, false, false), NOP("nop", false, false, true), BACKUP("backup", false, false,
                false), UPLOAD("upload", false, false, false);

        final String typeString;
        final boolean isInsertOrUpdate;
        final boolean isWriteAction;
        final boolean isAllowedOnAuthenticate;

        TX_TYPE(String typeString, boolean isInsertOrUpdate, boolean isWriteAction, boolean isAllowedOnAuthenticate) {
            this.typeString = typeString;
            this.isInsertOrUpdate = isInsertOrUpdate;
            this.isWriteAction = isWriteAction;
            this.isAllowedOnAuthenticate = isAllowedOnAuthenticate;
        }

        static TX_TYPE getType(String typeString) {
            for (TX_TYPE type : TX_TYPE.values())
                if (type.typeString.equalsIgnoreCase(typeString))
                    return type;
            return NOP;
        }
    }

    // The transaction response codes as text
    static final HashMap<Integer, String> TX_RESULT_CODES = new HashMap<Integer, String>();

    static {
        TX_RESULT_CODES.put(300, "Transaction completed");
        TX_RESULT_CODES.put(303, "Transaction completed and data key mismatch detected");
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
        TX_RESULT_CODES.put(503, "Could not decode server response");
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
     * Parse the full transaction String as read from permanent storage. The reverse function to appendTxFullString();
     *
     * @return the transaction parsed. Returns null on errors.
     */
    static Transaction parseTxFullString(String txFullString) {
        if (txFullString.trim().isEmpty())
            return null;
        Transaction tx = null;
        try {
            // Parse csv-String. Returns all decoded elements.
            ArrayList<String> txElements = CsvCodec.splitEntries(txFullString);
            // Parse elements.
            int ID = Integer.parseInt(txElements.get(0));
            long sentAt = Long.parseLong(txElements.get(1));
            int retries = Integer.parseInt(txElements.get(2));
            long resultAt = Long.parseLong(txElements.get(3));
            long closedAt = Long.parseLong(txElements.get(4));
            String type = txElements.get(5);
            String tablename = txElements.get(6);
            // parse the record
            String[] record = parseTxRecordString(txElements);
            // add the result. The result code and result message are always the last two elements.
            int resultCode = Integer.parseInt(txElements.get(txElements.size() - 2));
            String resultMessage = txElements.get(txElements.size() - 1);
            // Build the transaction
            tx = new Transaction(ID, TX_TYPE.getType(type), tablename, record);
            tx.sentAt = sentAt;
            tx.retries = retries;
            tx.resultAt = resultAt;
            tx.closedAt = closedAt;
            tx.resultCode = resultCode;
            tx.resultMessage = resultMessage;
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR, International
                    .getString("Fehler beim Lesen einer Transaktion vom permanenten Speicher: '{transaction}'",
                            txFullString));
        }
        // return result
        return tx;
    }

    /**
     * Parse the record part of a full transaction String as read from permanent storage. The reverse function to
     * appendTxRecordString();
     *
     * @param txFullStringElements the fullString split into elements using the csv-parser
     * @return the record as used by the transaction constructor.
     */
    private static String[] parseTxRecordString(ArrayList<String> txFullStringElements) {
        // read the record as the inner elements of the transaction
        final int recordStartOffset = 7;  // The first 7 elements are the transaction header fields.
        ArrayList<String> record = new ArrayList<String>();
        for (int i = recordStartOffset; i < txFullStringElements.size() - 2; i = i + 2)
            record.add(txFullStringElements.get(i) + ";" +
                    CsvCodec.encodeElement(txFullStringElements.get(i + 1), CsvCodec.DEFAULT_DELIMITER,
                            CsvCodec.DEFAULT_QUOTATION));
        String[] rArray = new String[record.size()];
        for (int i = 0; i < record.size(); i++)
            rArray[i] = record.get(i);
        return rArray;
    }

    /**
     * Create an internet access request for the InternetAccessManager for a bundle of transaction messages.
     *
     * @param txs the transactions to be encoded.
     * @return the properly formatted IAM message.
     */
    static TaskManager.RequestMessage createIamRequest(Vector<Transaction> txs) {

        if (txs == null || txs.size() == 0)
            return null;
        char d = TxRequestQueue.TX_REQ_DELIMITER;
        // Create the transaction container.
        TxRequestQueue txq = TxRequestQueue.getInstance();
        StringBuilder txContainer = new StringBuilder();
        txContainer.append(TxRequestQueue.EFA_CLOUD_VERSION).append(d).append(TxRequestQueue.getTxcId()).append(d)
                .append(txq.credentials);
        for (Transaction tx : txs) {
            tx.appendTxPostString(txContainer);
            tx.logMessage("send");
            txContainer.append(MESSAGE_SEPARATOR_STRING);
        }
        String txContainerStr = txContainer.toString();
        txContainerStr = txContainerStr.substring(0, txContainerStr.length() - MESSAGE_SEPARATOR_STRING.length());
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
        this.record = record;
    }

    /**
     * Append a log message to the synch log.
     *
     * @param action the action triggering the log activity
     */
    void logMessage(String action) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String transactionString =
                "#" + ID + ", " + type + " (record length: " + ((record == null) ? "null" : "" + record.length) + ")";
        String dateString = format.format(new Date()) + " [" + action + " for " + tablename + "]: ";
        // truncate log files,
        File f = new File(TxRequestQueue.logFilePaths.get("API activity"));
        if ((f.length() > 200000) &&
                (f.renameTo(new File(TxRequestQueue.logFilePaths.get("API activity") + ".previous"))))
            TextResource.writeContents(TxRequestQueue.logFilePaths.get("API activity"), dateString + transactionString,
                    false);
        else
        TextResource
                .writeContents(TxRequestQueue.logFilePaths.get("API activity"), dateString + transactionString, true);
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
     * @param maxlength maximum length of String. Cut only for logging. Set 0 if you want to get the really full String
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
            if (!record[i].trim().isEmpty()) {
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
     * @param maxlength maximum length of String. Cut only for logging. Set 0 if you want to get the really full String
     */
    private void appendTxRecordString(StringBuilder toAppendTo, int maxlength) {
        char d = TxRequestQueue.TX_REQ_DELIMITER;
        int k = 0;
        for (String r : record) {   // request record
            if (!r.trim().isEmpty()) {
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

    int getResultCode() {
        return resultCode;
    }

    String getResultMessage() {
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

    long getClosedAt() {
        return closedAt;
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

    int getCresultCode() {
        return cresultCode;
    }

    void setCresultCode(int cresultCode) {
        this.cresultCode = cresultCode;
    }

    String getCresultMessage() {
        return cresultMessage;
    }

    void setCresultMessage(String cresultMessage) {
        this.cresultMessage = cresultMessage;
    }

}
