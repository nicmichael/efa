/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.storage;

import de.nmichael.efa.*;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.util.*;
import de.nmichael.efa.ex.EfaException;
import java.util.*;
import java.io.*;
import java.net.*;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class RemoteEfaClient extends DataAccess {

    private String sessionId;
    private AdminRecord adminRecord;

    private DataCache cache;
    private long lastIsOpenTs = -1;
    private boolean lastIsOpen = false;
    private boolean loggedIn = false;
    private int loginAttempts = 0;
    private long lastLoginFailed = -1;

    private long lastPrintStatistics = 0;

    public RemoteEfaClient(String location, String username, String password, String name, String extension, String description) {
        setStorageLocation(location);
        setStorageObjectName(name);
        setStorageObjectType(extension);
        setStorageObjectDescription(description);
        setStorageUsername(username);
        setStoragePassword(password);
        cache = new DataCache(this, Daten.efaConfig.getValueDataRemoteCacheExpiryTime() * 1000);
    }

    public int getStorageType() {
        return IDataAccess.TYPE_EFA_REMOTE;
    }

    public String getUID() {
        return "remote:" + getStorageUsername() + "@" + getStorageLocation() + "/" + getStorageObjectName() + "." + getStorageObjectType();
    }

    public AdminRecord getAdminRecord() {
        return adminRecord;
    }

    // =========================== Communication Methods ===========================

    public URL getURL() {
        String url = getStorageLocation();
        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }
        if (url.lastIndexOf(":") < 7) {
            url = url + ":" + Daten.efaConfig.getValueDataataRemoteEfaServerPort();
        }
        try {
            return new URL(url);
        } catch(Exception e) {
            Logger.log(e);
            return null;
        }
    }

    private Vector<RemoteEfaMessage> sendRequest(RemoteEfaMessage request) throws Exception {
        Vector<RemoteEfaMessage> requests = new Vector<RemoteEfaMessage>();
        requests.add(request);
        return sendRequest(requests);
    }

    private Vector<RemoteEfaMessage> sendRequest(Vector<RemoteEfaMessage> requests) throws Exception {
        URL url = getURL();
        if (requests.size() == 0) {
            return null;
        }

        // login if we don't yet have a session id
        if (this.sessionId == null && lastLoginFailed > 0 &&
            System.currentTimeMillis() - lastLoginFailed < Daten.efaConfig.getValueDataRemoteLoginFailureRetryTime() * 1000) {
            loggedIn = false;
            return null;
        }
       
        // provide session id or username/password for all requests
        for (int i = 0; i < requests.size(); i++) {
            RemoteEfaMessage r = requests.get(i);
            if (this.sessionId != null) {
                r.addField(RemoteEfaMessage.FIELD_SESSIONID, sessionId);
            } else {
                r.addField(RemoteEfaMessage.FIELD_USERNAME, getStorageUsername());
                r.addField(RemoteEfaMessage.FIELD_PASSWORD, getStoragePassword());
                r.addField(RemoteEfaMessage.FIELD_PID, Daten.applPID);
            }
            AccessStatistics.updateStatistics(this, requests.get(i).getOperationName(),
                    AccessStatistics.COUNTER_REQSENT, 1);
        }

        StringBuffer request = new StringBuffer();
        request.append("<?xml version='1.0' encoding='" + Daten.ENCODING_UTF + "' ?>");
        request.append("<" + RemoteEfaParser.XML_EFA + ">");
        for (int i=0; i<requests.size(); i++) {
            request.append(requests.get(i).toString());
        }
        request.append("</" + RemoteEfaParser.XML_EFA + ">");
        for (int i = 0; i < requests.size(); i++) {
            AccessStatistics.updateStatistics(this, requests.get(i).getOperationName(),
                    AccessStatistics.COUNTER_BYTESSENT, request.length() / requests.size());
        }

        if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "Sending Request [" + url.toString() + "]: " + request.toString());
        }
        if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 2)) {
            for (int i=0; i<requests.size(); i++) {
                RemoteEfaMessage msg = requests.get(i);
                if (msg == null) {
                    continue;
                }
                Logger.log(Logger.DEBUG, Logger.MSG_REFA_TRACECOMMUNICATION, "Snd: " +
                        msg.getStorageObjectName() + "." + msg.getStorageObjectType() + ":" + msg.getOperationName() +
                        " (" + msg.toString().length() + " bytes)");
            }
        }

        long reqStartTs = System.currentTimeMillis();
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(true);
        connection.setRequestProperty("Content-Type", "application/xml"); //"application/x-www-form-urlencoded");
        OutputStreamWriter out = new OutputStreamWriter(
                RemoteEfaMessage.getOutputStream(connection.getOutputStream()),
                Daten.ENCODING_UTF);
        out.write(request.toString());
        out.flush();
        out.close();

        Vector<RemoteEfaMessage> responses = getResponse(connection, 
                RemoteEfaMessage.getBufferedInputStream(connection.getInputStream(),
                Daten.efaConfig.getValueDataRemoteClientReceiveTimeout()));
        if (responses == null) {
            for (int i = 0; i < requests.size(); i++) {
                AccessStatistics.updateStatistics(this, requests.get(i).getOperationName(),
                        AccessStatistics.COUNTER_TIMEOUT, 1);
            }
            throw new Exception("Receive Timeout");
        }
        long reqEndTs = System.currentTimeMillis();
        for (int i = 0; i < requests.size(); i++) {
            AccessStatistics.updateStatistics(this, requests.get(i).getOperationName(),
                    AccessStatistics.COUNTER_TIME, reqEndTs-reqStartTs);
        }

        for (int i = 0; i < responses.size(); i++) {
            RemoteEfaMessage req = (i < requests.size() ? requests.get(i) : null);
            RemoteEfaMessage msg = responses.get(i);
            if (msg == null) {
                continue;
            }
            AccessStatistics.updateStatistics(this, req.getOperationName(),
                    AccessStatistics.COUNTER_BYTESRCVD, msg.getMessageSizeEstimate());
            AccessStatistics.updateStatistics(this, req.getOperationName(),
                    AccessStatistics.COUNTER_REQRCVD, 1);
            if (msg.getNumberOfRecords() > 0) {
                AccessStatistics.updateStatistics(this, req.getOperationName(),
                        AccessStatistics.COUNTER_RECSRCVD, msg.getNumberOfRecords());
            }
            if (msg.getNumberOfKeys() > 0) {
                AccessStatistics.updateStatistics(this, req.getOperationName(),
                        AccessStatistics.COUNTER_RECSRCVD, msg.getNumberOfKeys());
            }


            if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 2)) {
                Logger.log(Logger.DEBUG, Logger.MSG_REFA_TRACECOMMUNICATION, "Rcv: "
                        + (req != null ? req.getStorageObjectName() + "." + req.getStorageObjectType() + ":" + req.getOperationName()
                        : msg.getOperationName())
                        + " (" + msg.toString().length() + " bytes) ["
                        + (reqEndTs - reqStartTs) + " ms]");
            }
        }
        getGeneralDataFromResponses(responses);
        return responses;
    }

    private Vector<RemoteEfaMessage> getResponse(URLConnection connection, RemoteEfaMessage.EfaMessageInputStream eis) {
        if (eis == null || eis.in == null) {
            return null;
        }
        BufferedInputStream in = eis.in;
        
        if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 5)) {
            try {
                in.mark(1024 * 1024);
                if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 9)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "Got RemoteEfaResponse:");
                    Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "    -- HEADER START --");
                    Map<String, List<String>> m = connection.getHeaderFields();
                    for (String header : m.keySet()) {
                        Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "    " + header + "=" + connection.getHeaderField(header));
                    }
                    Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "    -- HEADER END --");
                    BufferedReader buf = new BufferedReader(new InputStreamReader(in));
                    String s;
                    Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "    -- RESPONSE START --");
                    while ((s = buf.readLine()) != null) {
                        Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "   " + s);
                    }
                    Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "    -- RESPONSE END --");
                } else {
                    BufferedReader buf = new BufferedReader(new InputStreamReader(in));
                    String s;
                    while ((s = buf.readLine()) != null) {
                        Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "Got response: " + s);
                    }
                }
                in.reset();
            } catch (Exception e) {
            }
        }

        try {
            XMLReader parser = EfaUtil.getXMLReader();
            RemoteEfaParser responseHandler = new RemoteEfaParser(this);
            parser.setContentHandler(responseHandler);
            parser.parse(new InputSource(in));
            if (responseHandler.isDocumentComplete()) {
                Vector<RemoteEfaMessage> responses = responseHandler.getMessages();
                if (responses != null && responses.size() > 0 && responses.get(0) != null) {
                    if (this.sessionId != null) {
                        // we have a session id, but we need to check whether the server still accepts it
                        if (responses.get(0).getResultCode() == RemoteEfaMessage.ERROR_INVALIDSESSIONID) {
                            // it seems our session id became invalid
                            this.sessionId = null;
                        }
                    } else {
                        // we don't have a session id, so this might be the response for a login: get the session id
                        this.sessionId = responses.get(0).getSessionId();
                        this.adminRecord = responses.get(0).getAdminRecord();
                        if (this.adminRecord != null) {
                            this.adminRecord.setRemoteAdminRecord(true);
                        }
                    }
                    for (int i=0; i<responses.size(); i++) {
                        responses.get(i).setMessageSizeEstimate(eis.size/responses.size());
                    }
                }
                AccessStatistics.updateStatistics(this, "All",
                        AccessStatistics.COUNTER_RESPOK, 1);
                return responses;
            } else {
                AccessStatistics.updateStatistics(this, "All",
                        AccessStatistics.COUNTER_RESPERR, 1);
                return null;
            }
        } catch(Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_REFA_INVALIDRESPONSE, "Get Response failed: " + e.toString());
            Logger.logdebug(e);
            AccessStatistics.updateStatistics(this, "All",
                    AccessStatistics.COUNTER_RESPERR, 1);
            return null;
        }
    }

    private void getGeneralDataFromResponses(Vector<RemoteEfaMessage> responses) {
        if (responses == null) {
            return;
        }
        for (int i=0; i<responses.size(); i++) {
            RemoteEfaMessage response = responses.get(i);
            if (sessionId == null && response != null && response.getSessionId() != null) {
                // this was a login request
                sessionId = response.getSessionId();
                lastLoginFailed = 0;
                loggedIn = true;
            }
            if (response != null && response.getResultCode() == RemoteEfaMessage.RESULT_OK) {
                long scn = response.getScn();
                long totalRecordCount = response.getRecCnt();
                DataRecord[] records = response.getRecords();
                if (records != null) {
                    for (int j=0; j<records.length; j++) {
                        if (records[j] != null) {
                            cache.updateCache(records[j], scn, totalRecordCount);
                        }
                    }
                } else {
                    cache.updateScn(scn, totalRecordCount);
                }
                DataKey[] keys = response.getKeys();
            }

            if (response != null &&
                    (response.getResultCode() == RemoteEfaMessage.ERROR_INVALIDLOGIN ||
                     response.getResultCode() == RemoteEfaMessage.ERROR_NOPERMISSION ||
                     response.getResultCode() == RemoteEfaMessage.ERROR_SELFLOGIN)) {
                Logger.log(Logger.ERROR, Logger.MSG_REFA_LOGINFAILURE,
                        International.getString("Login fehlgeschlagen") + ": " + response.getResultText() +
                        " (Code " + response.getResultCode() + ")");
                lastLoginFailed = System.currentTimeMillis();
                loggedIn = false;
            }
        }
    }

    private String getErrorLogstring(RemoteEfaMessage request, String msg, int code) {
        String requestName = (request != null ? request.getOperationName() : "Unknown");
        return getErrorLogstring(requestName, msg, code);
    }

    private String getErrorLogstring(String requestName, String msg, int code) {
        return getStorageObjectName() + "." + getStorageObjectType() + ": " +
               International.getMessage("efaRemote-Anfrage {request} fehlgeschlagen: {reason}", requestName, msg + " (Code " + code + ")");
    }

    protected int runSimpleRequest(RemoteEfaMessage request) {
        try {
            int myRequestId = request.getMsgId();
            Vector<RemoteEfaMessage> responses = sendRequest(request);
            if (responses == null || responses.size() == 0 ||
                responses.get(0) == null) {
                if (!loggedIn) {
                    return -1;
                }
                Logger.log(Logger.ERROR, Logger.MSG_REFA_UNEXPECTEDRESPONSE, getErrorLogstring(request, "empty response", -1));
                return -1;
            }
            if (responses.size() > 1) {
                Logger.log(Logger.ERROR, Logger.MSG_REFA_UNEXPECTEDRESPONSE, getErrorLogstring(request,
                           "unexpected number of responses for simple request: " + responses.size(), -1));
                return -1;
            }
            RemoteEfaMessage response = responses.get(0);
            if (response.getResultCode() != 0) {
                Logger.log(Logger.ERROR, Logger.MSG_REFA_REQUESTFAILED, getErrorLogstring(request,
                           response.getResultText(), response.getResultCode()), false);
                return response.getResultCode();
            }
            return RemoteEfaMessage.RESULT_OK;
        } catch(Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_REFA_REQUESTFAILED, getErrorLogstring(request,
                       e.getMessage(), -1), false);
            return -1;
        }
    }

    protected RemoteEfaMessage runDataRequest(RemoteEfaMessage request) {
        try {
            int myRequestId = request.getMsgId();
            Vector<RemoteEfaMessage> responses = sendRequest(request);
            if (responses == null || responses.size() == 0 ||
                responses.get(0) == null) {
                if (!loggedIn) {
                    return null;
                }
                Logger.log(Logger.ERROR, Logger.MSG_REFA_UNEXPECTEDRESPONSE, getErrorLogstring(request, "empty response", -1));
                return null;
            }
            if (responses.size() > 1) {
                Logger.log(Logger.ERROR, Logger.MSG_REFA_UNEXPECTEDRESPONSE, getErrorLogstring(request,
                           "unexpected number of responses for data request: " + responses.size(), -1));
                return null;
            }
            return responses.get(0);
        } catch(Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_REFA_REQUESTFAILED, getErrorLogstring(request,
                       e.getMessage(), -1), false);
            return null;
        }
    }

    // =========================== Storage Object Methods ===========================

    public boolean existsStorageObject() throws EfaException {
        return runSimpleRequest(RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_EXISTSSTORAGEOBJECT)) == RemoteEfaMessage.RESULT_OK;
    }

    public void openStorageObject() throws EfaException {
        if (runSimpleRequest(RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_OPENSTORAGEOBJECT)) != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_OPENSTORAGEOBJECT, "unknown", -1),
                    Thread.currentThread().getStackTrace());
        }
    }

    public void createStorageObject() throws EfaException {
        if (runSimpleRequest(RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_CREATESTORAGEOBJECT)) != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_CREATESTORAGEOBJECT, "unknown", -1),
                    Thread.currentThread().getStackTrace());
        }
    }

    public boolean isStorageObjectOpen() {
        if (lastIsOpen && System.currentTimeMillis() - lastIsOpenTs < Daten.efaConfig.getValueDataRemoteIsOpenExpiryTime() * 1000) {
            return true;
        }
        lastIsOpen = runSimpleRequest(RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_ISSTORAGEOBJECTOPEN)) == RemoteEfaMessage.RESULT_OK;
        lastIsOpenTs = System.currentTimeMillis();
        return lastIsOpen;
    }

    public void closeStorageObject() throws EfaException {
        // we shouldn't do anything here... this method is called often when we exit,
        // and we just delay the exit of efa by doing a real close.
        // also, the server won't react on this method anyway, so it's useless sending
        // it from the client.
    }

    public void deleteStorageObject() throws EfaException {
        // we will never delete a remote storage object!
    }



    // =========================== Lock Methods ===========================

    public long acquireGlobalLock() throws EfaException {
        RemoteEfaMessage response = runDataRequest(RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_ACQUIREGLOBALLOCK));
        if (response != null) {
            return response.getLockId();
        } else {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_ACQUIREGLOBALLOCK, "unknown", -1),
                    Thread.currentThread().getStackTrace());
        }
    }

    public long acquireLocalLock(DataKey key) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_ACQUIRELOCALLOCK);
        request.addKey(key);
        RemoteEfaMessage response = runDataRequest(request);
        if (response != null) {
            return response.getLockId();
        } else {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_ACQUIRELOCALLOCK, "unknown", -1),
                    Thread.currentThread().getStackTrace());
        }
    }

    public boolean releaseGlobalLock(long lockID) {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_RELEASEGLOBALLOCK);
        request.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockID));
        RemoteEfaMessage response = runDataRequest(request);
        return (response != null && response.getResultCode() == RemoteEfaMessage.RESULT_OK);
    }

    public boolean releaseLocalLock(long lockID) {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_RELEASELOCALLOCK);
        request.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockID));
        RemoteEfaMessage response = runDataRequest(request);
        return (response != null && response.getResultCode() == RemoteEfaMessage.RESULT_OK);
    }



    // =========================== Global Data Methods ===========================

    public long getNumberOfRecords() throws EfaException {
        // fetch SCN from Cache or remotely to make sure we have up-to-date data (< MAX_AGE)
        getSCN();
        long totalNumberOfRecords = cache.getTotalNumberOfRecordsIfNotTooOld();
        if (totalNumberOfRecords >= 0) {
            return totalNumberOfRecords;
        }
        
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GETNUMBEROFRECORDS);
        RemoteEfaMessage response = runDataRequest(request);
        if (response != null) {
            return response.getLongValue();
        } else {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GETNUMBEROFRECORDS, "unknown", -1),
                    Thread.currentThread().getStackTrace());
        }
    }

    public long getSCN() throws EfaException {
        long scn = cache.getScnIfNotTooOld();
        if (scn >= 0) {
            return scn;
        }
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GETSCN);
        RemoteEfaMessage response = runDataRequest(request);
        if (response != null) {
            return response.getScn();
        } else {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GETSCN, "unknown", -1),
                    Thread.currentThread().getStackTrace());
        }
    }

    public void createIndex(String[] fieldNames) throws EfaException {
        // nothing to be done
    }



    // =========================== Data Modification Methods ===========================

    public void add(DataRecord record) throws EfaException {
        add(record, -1);
    }

    public void add(DataRecord record, long lockID) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_ADD);
        request.addRecord(record);
        if (lockID > 0) {
            request.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockID));
        }
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_ADD,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
    }

    public DataKey addValidAt(DataRecord record, long t) throws EfaException {
        return addValidAt(record, t, -1);
    }

    public DataKey addValidAt(DataRecord record, long t, long lockID) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_ADDVALIDAT);
        request.addRecord(record);
        request.addField(RemoteEfaMessage.FIELD_TIMESTAMP, Long.toString(t));
        if (lockID > 0) {
            request.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockID));
        }
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_ADDVALIDAT,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getKey(0);
    }

    public void addAll(DataRecord[] records, long lockID) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_ADDALL);
        for (DataRecord r : records) {
            request.addRecord(r);
        }
        if (lockID > 0) {
            request.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockID));
        }
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_ADDALL,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
    }

    public DataRecord get(DataKey key) throws EfaException {
        DataRecord r = cache.get(key);
        if (r != null) {
            return r;
        }
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GET);
        request.addKey(key);
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GET,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getRecord(0);
    }

    public DataKey[] getAllKeys() throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GETALLKEYS);
        request.addField(RemoteEfaMessage.FIELD_PREFETCH, Boolean.toString(true)); // prefetch all records as well
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GETALLKEYS,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getKeys();
    }

    public DataKey[] getByFields(String[] fieldNames, Object[] values) throws EfaException {
        return getByFields(fieldNames, values, -1);
    }

    public DataKey[] getByFields(String[] fieldNames, Object[] values, long validAt) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GETBYFIELDS);
        for (int i=0; i<fieldNames.length; i++) {
            request.addFieldArrayElement(i, fieldNames[i], (values[i] != null ? values[i].toString() : ""));
        }
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GETBYFIELDS,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getKeys();
    }

    public DataRecord[] getValidAny(DataKey key) throws EfaException {
        DataRecord[] r = cache.getValidAny(key);
        // the new implementation will always find a record in the cache if there is one
        // (by updating the cache first); therefore, if the cache returns null, there is no
        // record and we don't have to fetch it remotely
        if (true || r != null) {
            return r;
        }
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GETVALIDANY);
        request.addKey(key);
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GETVALIDANY,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getRecords();
    }

    public DataRecord getValidAt(DataKey key, long t) throws EfaException {
        DataRecord r = cache.getValidAt(key, t);
        // the new implementation will always find a record in the cache if there is one
        // (by updating the cache first); therefore, if the cache returns null, there is no
        // record and we don't have to fetch it remotely
        if (true || r != null) {
            return r;
        }
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GETVALIDAT);
        request.addKey(key);
        request.addField(RemoteEfaMessage.FIELD_TIMESTAMP, Long.toString(t));
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GETVALIDAT,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getRecord(0);
    }

    public DataRecord getValidLatest(DataKey key) throws EfaException {
        DataRecord r = cache.getValidLatest(key);
        // the new implementation will always find a record in the cache if there is one
        // (by updating the cache first); therefore, if the cache returns null, there is no
        // record and we don't have to fetch it remotely
        if (true || r != null) {
            return r;
        }
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GETVALIDLATEST);
        request.addKey(key);
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GETVALIDLATEST,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getRecord(0);
    }

    public DataRecord getValidNearest(DataKey key, long earliestValidAt, long latestValidAt, long preferredValidAt) throws EfaException {
        // indirect implementation - copied from DataFile
        DataRecord r = getValidAt(key, preferredValidAt);
        if (r != null) {
            return r;
        }
        DataRecord[] records = getValidAny(key);
        long minDistance = Long.MAX_VALUE;
        for (int i = 0; records != null && i < records.length; i++) {
            if (records[i].isInValidityRange(earliestValidAt, latestValidAt)) {
                long myDist = Long.MAX_VALUE;
                if (records[i].getInvalidFrom() - 1 < preferredValidAt) {
                    myDist = preferredValidAt - records[i].getInvalidFrom() - 1;
                }
                if (records[i].getValidFrom() > preferredValidAt) {
                    myDist = records[i].getValidFrom() - preferredValidAt;
                }
                if (myDist < minDistance) {
                    minDistance = myDist;
                    r = records[i];
                }
            }
        }
        return r;

        // direct implementation - retrieve remotely
        /*
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GETVALIDNEAREST);
        request.addKey(key);
        request.addField(RemoteEfaMessage.FIELD_VALIDFROM, Long.toString(earliestValidAt));
        request.addField(RemoteEfaMessage.FIELD_INVALIDFROM, Long.toString(latestValidAt));
        request.addField(RemoteEfaMessage.FIELD_TIMESTAMP, Long.toString(preferredValidAt));
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GETVALIDNEAREST,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getRecord(0);
        */
    }

    public boolean isValidAny(DataKey key) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_ISVALIDANY);
        request.addKey(key);
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null ||
            (response.getResultCode() != RemoteEfaMessage.RESULT_OK && response.getResultCode() != RemoteEfaMessage.RESULT_FALSE)) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_ISVALIDANY,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getResultCode() == RemoteEfaMessage.RESULT_OK;
    }

    public DataRecord update(DataRecord record) throws EfaException {
        return update(record, -1);
    }

    public DataRecord update(DataRecord record, long lockID) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_UPDATE);
        request.addRecord(record);
        if (lockID > 0) {
            request.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockID));
        }
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_UPDATE,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        } else {
            if (response.getNumberOfRecords() == 1 && response.getRecord(0) != null) {
                cache.updateCache(response.getRecord(0), -1, -1);
                return response.getRecord(0);
            }
        }
        return null;
    }

    public void changeValidity(DataRecord record, long validFrom, long invalidFrom) throws EfaException {
        changeValidity(record, validFrom, invalidFrom, -1);
    }

    public void changeValidity(DataRecord record, long validFrom, long invalidFrom, long lockID) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_CHANGEVALIDITY);
        request.addRecord(record);
        request.addField(RemoteEfaMessage.FIELD_VALIDFROM, Long.toString(validFrom));
        request.addField(RemoteEfaMessage.FIELD_INVALIDFROM, Long.toString(invalidFrom));
        if (lockID > 0) {
            request.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockID));
        }
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_CHANGEVALIDITY,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
    }

    public void delete(DataKey key) throws EfaException {
        delete(key, -1);
    }

    public void delete(DataKey key, long lockID) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_DELETE);
        request.addKey(key);
        if (lockID > 0) {
            request.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockID));
        }
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_DELETE,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
    }

    public void deleteVersionized(DataKey key, int merge) throws EfaException {
        deleteVersionized(key, merge, -1);
    }

    public void deleteVersionized(DataKey key, int merge, long lockID) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_DELETEVERSIONIZED);
        request.addKey(key);
        request.addField(RemoteEfaMessage.FIELD_MERGE, Integer.toString(merge));
        if (lockID > 0) {
            request.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockID));
        }
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_DELETEVERSIONIZED,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
    }

    public void deleteVersionizedAll(DataKey key, long deleteAt) throws EfaException {
        deleteVersionizedAll(key, deleteAt, -1);
    }

    public void deleteVersionizedAll(DataKey key, long deleteAt, long lockID) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_DELETEVERSIONIZEDALL);
        request.addKey(key);
        request.addField(RemoteEfaMessage.FIELD_TIMESTAMP, Long.toString(deleteAt));
        if (lockID > 0) {
            request.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockID));
        }
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_DELETEVERSIONIZEDALL,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
    }

    public long countRecords(String[] fieldNames, Object[] values) throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_COUNTRECORDS);
        for (int i=0; i<fieldNames.length; i++) {
            request.addFieldArrayElement(i, fieldNames[i], (values[i] != null ? values[i].toString() : ""));
        }
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_COUNTRECORDS,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getLongValue();
    }

    public void truncateAllData() throws EfaException {
        if (runSimpleRequest(RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_TRUNCATEALLDATA)) != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_OPENSTORAGEOBJECT, "unknown", -1),
                    Thread.currentThread().getStackTrace());
        }
    }

    // =========================== Data Iterator Methods ===========================

    public DataKeyIterator getStaticIterator() throws EfaException {
        DataKey[] keys = cache.getAllKeys();
        if (keys == null) {
            keys = getAllKeys();
        }
        return new DataKeyIterator(this, keys, false);
    }

    public DataKeyIterator getDynamicIterator() throws EfaException {
        DataKey[] keys = cache.getAllKeys();
        if (keys == null) {
            keys = getAllKeys();
        }
        return new DataKeyIterator(this, keys, true);
    }

    public DataRecord getFirst() throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GETFIRST);
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GETFIRST,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getRecord(0);
    }

    public DataRecord getLast() throws EfaException {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_GETLAST);
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            throw new EfaException(Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_GETLAST,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)),
                    Thread.currentThread().getStackTrace());
        }
        return response.getRecord(0);
    }

    public void setInOpeningStorageObject(boolean inOpening) {
        super.setInOpeningStorageObject(inOpening);
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_SETINOPENINGMODE);
        request.addField(RemoteEfaMessage.FIELD_BOOLEAN, Boolean.toString(inOpening));
        RemoteEfaMessage response = runDataRequest(request);
        if (response == null || response.getResultCode() != RemoteEfaMessage.RESULT_OK) {
            Logger.log(Logger.DEBUG, Logger.MSG_REFA_REQUESTFAILED,
                    getErrorLogstring(RemoteEfaMessage.OPERATION_SETINOPENINGMODE,
                    (response != null ? response.getResultText() : "unknown"),
                    (response != null ? response.getResultCode() : -1)));
        }
    }

}
