package de.nmichael.efa.data.efacloud;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Vector;

/**
 * A container class for data modifications passed to the efaClod Server.
 */
public class Transaction {

    // the MESSAGE_SEPARATOR_STRING must not contain any regex special character
    // because it is used in "split()" calls as "regex" argument.
    public static final String MESSAGE_SEPARATOR_STRING = "\n|-eFa-|\n";
    public static final String MESSAGE_SEPARATOR_STRING_REGEX = "\n\\|\\-eFa\\-\\|\n";
    public static final String MS_REPLACEMENT_STRING = "\n|-efa-|\n";
    // transaction ID
    public final int ID;
    // status control
    private long sentAt = 0;
    private long retries = 0;
    private long resultAt = 0;
    private long closedAt = 0;
    // transaction request
    public final String type;
    public final String tablename;
    /**
     * lock the transaction fpr synchronous operation. That will prevent retry, but return a failure
     * after timeout
     */
    public volatile Boolean lock = false;
    /**
     * per transaction one event listener can be set which will be notified on transaction events.
     * See the TxEventListener interface for the events available.
     */
    public Object sender = null;
    /**
     * The Strings within record are csv encoded: field;value. They can just be appended to a
     * csv-encoded String.
     */
    protected final String[] record;
    // transaction result
    private int resultCode = 0;
    private String resultMessage = "";
    // transaction container result. This is put to the transaction when the container is aborted.
    private int cresultCode = 0;
    private String cresultMessage = "";

    /**
     * Constructor. Creates the transaction and adds a new transaction ID.
     *
     * @param ID     ID of transaction to be created. Set -1 to auto increment the ID
     * @param type   type of transaction to be created
     * @param record record data of the transaction to be created
     */
    public Transaction(int ID, String type, String tablename, String[] record) {
        this.ID = (ID == -1) ? TxRequestQueue.getTxId() : ID;
        this.type = type;
        this.tablename = tablename;
        this.record = record;
    }

    /**
     * Create an internet access request for the InternetAccessManager for a bundle of transaction
     * messages.
     *
     * @param txs the transactions to be encoded.
     * @return the properly formatted IAM message.
     */
    public static TaskManager.RequestMessage createIamRequest(Vector<Transaction> txs) {

        if (txs == null || txs.size() == 0) return null;
        char d = TxRequestQueue.TX_REQ_DELIMITER;
        // Create the transaction container.
        TxRequestQueue txq = TxRequestQueue.getInstance();
        StringBuilder txContainer = new StringBuilder();
        txContainer.append(TxRequestQueue.EFA_CLOUD_VERSION).append(d)
                .append(TxRequestQueue.getTxcId()).append(d).append(txq.credentials);
        for (Transaction tx : txs) {
            Transaction.appendTxPostString(txContainer, tx);
            txContainer.append(MESSAGE_SEPARATOR_STRING);
        }
        String txContainerStr = txContainer.toString();
        txContainerStr = txContainerStr
                .substring(0, txContainerStr.length() - MESSAGE_SEPARATOR_STRING.length());
        // encode the transaction message container
        String txContainerBase64;
        txContainerBase64 = Base64.getEncoder()
                .encodeToString(txContainerStr.getBytes(StandardCharsets.UTF_8));

        // create the txContainerBase64efa which needs no further URL encoding.
        String txContainerBase64efa = txContainerBase64.replace('/', '-').replace('+', '*')
                .replace('=', '_');
        // send it to the internet access manager.
        String postURLplus = txq.efaCloudUrl + "?txc=" + txContainerBase64efa;
        // For the InternetAccessManager semantics of a RequestMessage, see InternetAccessManager
        // class information.
        return new TaskManager.RequestMessage(postURLplus, "",
                InternetAccessManager.TYPE_POST_PARAMETERS, 0.0, txq);
    }

    /**
     * Append a transaction record to the provided StringBuilder.
     *
     * @param toAppendTo StringBuilder to append the transaction to
     * @param tx         transaction for which the record shall be appended.
     */
    private static void appendTxRecord(StringBuilder toAppendTo, Transaction tx) {
        char d = TxRequestQueue.TX_REQ_DELIMITER;
        int k = 0;
        for (String r : tx.record) {   // request record
            // cut value to max length.
            ArrayList<String> fnv = CsvCodec.splitEntries(r);
            String field = fnv.get(0);
            String value;
            if (fnv.size() < 2) value = "";
            else value = fnv.get(1);
            // cut the value to the maximum length for data write transactions
            if (tx.type.equalsIgnoreCase("insert") || tx.type.equalsIgnoreCase("update"))
                value = Daten.tableBuilder.adjustForEfaCloudStorage(value, tx.tablename, field);
            // ensure the message separator String will not be contained.
            if (value != null && value.contains(Transaction.MESSAGE_SEPARATOR_STRING)) value = value
                    .replace(Transaction.MESSAGE_SEPARATOR_STRING,
                            Transaction.MS_REPLACEMENT_STRING);
            // append value
            toAppendTo.append(field).append(";").append(CsvCodec
                    .encodeElement(value, CsvCodec.DEFAULT_DELIMITER, CsvCodec.DEFAULT_QUOTATION));
            // do not add a delimiter at the very end.
            k++;
            if (k < tx.record.length) toAppendTo.append(d);
        }
    }

    /**
     * append the full transaction String to the provided StringBuilder.
     *
     * @param toAppendTo StringBuilder to append the transaction to
     * @param tx         transaction to append
     */
    private static void appendTxPostString(StringBuilder toAppendTo, Transaction tx) {
        char d = TxRequestQueue.TX_REQ_DELIMITER;
        // Request header
        toAppendTo.append(tx.ID).append(d)        //
                .append(tx.retries).append(d)     //
                .append(tx.type).append(d)        //
                .append(tx.tablename).append(d);  //
        // Request record
        appendTxRecord(toAppendTo, tx);
    }

    /**
     * append the full transaction String to the provided StringBuilder.
     *
     * @param toAppendTo StringBuilder to append the transaction to
     * @param tx         transaction to append
     */
    private static void appendFullTxString(StringBuilder toAppendTo, Transaction tx) {
        char d = CsvCodec.DEFAULT_DELIMITER;
        // Request header
        toAppendTo.append(tx.ID).append(d)        //
                .append(tx.sentAt).append(d)      // transaction control, all numeric.
                .append(tx.retries).append(d)     //
                .append(tx.resultAt).append(d)    //
                .append(tx.closedAt).append(d)    //
                .append(tx.type).append(d)        // the remainder of the request header.
                .append(tx.tablename).append(d);  //
        // Request record
        appendTxRecord(toAppendTo, tx);
        // Request result
        toAppendTo.append(d).append(tx.resultCode).append(d)  // result code
                .append(CsvCodec.encodeElement(tx.resultMessage, d,
                        CsvCodec.DEFAULT_QUOTATION));  // and result message.
    }

    /**
     * Get the full transaction String for permanent storage in the retry queue.
     *
     * @return full transaction String: ID/retries, type, parameters. 'csv'-encoded using the
     * URL_DELIMITER and URL_QUOTATION
     */
    protected String getFullTxString() {
        StringBuilder toAppendTo = new StringBuilder();
        appendFullTxString(toAppendTo, this);
        return toAppendTo.toString();
    }

    /**
     * Get the full transaction String for permanent storage in the retry queue.
     *
     * @return full transaction String: ID/retries, type, parameters. 'csv'-encoded using the
     * URL_DELIMITER and URL_QUOTATION
     */
    protected static Transaction parseFullTxString(String txString) {
        Transaction tx;
        try {
            // Parse csv-String. Returns all decoded elements.
            ArrayList<String> txParts = CsvCodec.splitEntries(txString);
            // Parse elements. Sequence of read (parseFullTxString) and write (appendFullTxString)
            // must be the same.
            int ID = Integer.parseInt(txParts.get(0));
            long sentAt = Long.parseLong(txParts.get(1));
            int retries = Integer.parseInt(txParts.get(2));
            long resultAt = Long.parseLong(txParts.get(3));
            long closedAt = Long.parseLong(txParts.get(4));
            String type = txParts.get(5);
            String tablename = txParts.get(6);
            // read the record. The record elements are csv encoded. They can just be appended to
            // a csv-encoded transaction message String
            ArrayList<String> record = new ArrayList<>();
            for (int i = 7; i < txParts.size() - 2; i = i + 2)
                record.add(txParts.get(i) + ";" + CsvCodec
                        .encodeElement(txParts.get(i + 1), CsvCodec.DEFAULT_DELIMITER,
                                CsvCodec.DEFAULT_QUOTATION));
            String[] rArray = new String[record.size()];
            for (int i = 0; i < record.size(); i++) rArray[i] = record.get(i);
            // add the result. The result code and result message are always the last two elements.
            int resultCode = Integer.parseInt(txParts.get(txParts.size() - 2));
            String resultMessage = txParts.get(txParts.size() - 1);
            // Build the transaction
            tx = new Transaction(ID, type, tablename, rArray);
            tx.sentAt = sentAt;
            tx.retries = retries;
            tx.resultAt = resultAt;
            tx.closedAt = closedAt;
            tx.resultCode = resultCode;
            tx.resultMessage = resultMessage;
        } catch (Exception e) {
            String[] record = new String[]{"record;lost"};
            tx = new Transaction(0, "nop", "ignored", record);
            tx.resultCode = 500;
            tx.resultMessage =
                    "Message could not be retrieved from permanent message queue. It's " +
                            "contents is lost.";
        }
        // return result
        return tx;
    }

    /**
     * Append the transaction to the pending queue and return only after a response was received.
     */
    public synchronized void executeSynchronously(TxRequestQueue txq) {
        lock = true;
        long startAt = System.currentTimeMillis();
        long timeOut = (InternetAccessManager.TIME_OUT_MONITOR_PERIODS + 1) * InternetAccessManager.MONITOR_PERIOD;
        txq.appendTransactionPending(this);
        while (lock && (System.currentTimeMillis() - startAt) < timeOut) try {
            // wait at least one retry cycle.
            wait((InternetAccessManager.TIME_OUT_MONITOR_PERIODS + 1) * InternetAccessManager.MONITOR_PERIOD);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if ((System.currentTimeMillis() - startAt) > timeOut) {
            // time out happened. Register event in transaction.
            Logger.log(Logger.ERROR, Logger.MSG_EFACLOUDSYNCH_ERROR,
                    "Timeout on synchronous transaction: " + this.toString());
            this.resultCode = 408;
            this.resultMessage = "Time out of synchronous transaction.";
            this.resultAt = System.currentTimeMillis();
        }
    }

    /**
     * Append the transaction to the pending queue and return only after a response was received.
     */
    public synchronized void confirmExecution() {
        lock = false;
        notifyAll();
    }

    /**
     * Create a String for Debug purposes
     *
     * @return a String for Debug purposes
     */
    public String toString() {
        return ID + ":" + type + " " + tablename;
    }

    public long getSentAt() {
        return sentAt;
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public void setSentAt(long sentAt) { this.sentAt = sentAt; }

    public long getRetries() {
        return retries;
    }

    public void setRetries(long retries) {
        this.retries = retries;
    }

    public void setResultAt(long resultAt) {
        this.resultAt = resultAt;
    }

    public long getResultAt() {
        return resultAt;
    }

    public void setClosedAt(long closedAt) {
        this.closedAt = closedAt;
    }

    public int getCresultCode() {
        return cresultCode;
    }

    public void setCresultCode(int cresultCode) {
        this.cresultCode = cresultCode;
    }

    public String getCresultMessage() {
        return cresultMessage;
    }

    public void setCresultMessage(String cresultMessage) {
        this.cresultMessage = cresultMessage;
    }

    /**
     * A compact container to keep the result of transactions in memory for statistics.
     */
    public static class TxReceipt {
        public final int ID;
        public final long sentAt;
        public final long retries;
        public final long resultAt;
        public final long closedAt;
        public final String type;
        public final String tablename;
        public final int resultCode;
        public final Object sender;

        TxReceipt(Transaction tx) {
            this.ID = tx.ID;
            this.sentAt = tx.sentAt;
            this.retries = tx.retries;
            this.resultAt = tx.resultAt;
            this.closedAt = tx.closedAt;
            this.type = tx.type;
            this.tablename = tx.tablename;
            this.resultCode = (tx.resultCode == 0) ? tx.cresultCode : tx.resultCode;
            this.sender = tx.sender;
        }
    }
}
