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

import com.sun.net.httpserver.*;
import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.gui.EfaBoathouseFrame;
import de.nmichael.efa.util.Base64;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.core.OnlineUpdate;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.security.*;
import java.util.concurrent.Executors;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class RemoteEfaServer {

    private static final long SESSION_TIMEOUT = 24 * 60 * 60 * 1000;

    private static final Object syncObject = new Object();

    private static SecureRandom prng;
    private static MessageDigest sha;

    private static String lastLoginAdmin = null;
    private static String lastLoginIp = null;
    private static long lastLoginTime = 0;

    private int serverPort;
    private Hashtable<String,AdminRecord> sessions = new Hashtable<String,AdminRecord>();
    private Hashtable<String,Long> sessionTimeouts = new Hashtable<String,Long>();

    public RemoteEfaServer(int port, boolean acceptRemote) {
        serverPort = port;
        String serverName = (acceptRemote ? Daten.EFA_REMOTE : "efaLocal" );
        try {
            InetSocketAddress addr;
            if (acceptRemote) {
                addr = new InetSocketAddress(port);
            } else {
                addr = new InetSocketAddress(
                        InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), port);
            }
            HttpServer server = HttpServer.create(addr, 0);

            server.createContext("/", new MyHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            Logger.log((acceptRemote ? Logger.INFO : Logger.DEBUG), Logger.MSG_REFA_SERVERSTATUS,
                    International.getMessage("{name} Server läuft auf Port {port}",
                    serverName, serverPort));
            (new EfaOnlineThread()).start();
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_REFA_SERVERERROR,
                    International.getMessage("{name} Server konnte nicht gestartet werden.", serverName)
                    + " " + e.getMessage());
            Logger.logdebug(e);
        }
    }

    class MyHandler implements HttpHandler {

        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equalsIgnoreCase("GET")) {
                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, 0);

                OutputStream responseBody = exchange.getResponseBody();
                responseBody.write(new String("<html><body>").getBytes());
                responseBody.write(new String("<h1 align=\"center\">" + Daten.EFA_LONGNAME + "</h1>").getBytes());
                responseBody.write(new String("<h2 align=\"center\">efaRemote Server running on port " + serverPort).getBytes());
                responseBody.close();
            }
            if (requestMethod.equalsIgnoreCase("POST")) {
                Headers responseHeaders = exchange.getResponseHeaders();
                responseHeaders.set("Content-Type", "application/xml");
                exchange.sendResponseHeaders(200, 0);

                Vector<RemoteEfaMessage> responses = new Vector<RemoteEfaMessage>();
                try {
                    Vector<RemoteEfaMessage> requests = getRequests(
                            RemoteEfaMessage.getBufferedInputStream(exchange.getRequestBody(), 0),
                            exchange.getRemoteAddress());
                    if (requests == null) {
                        return;
                    }
                    responses = handleRequests(requests, exchange.getRemoteAddress());
                } catch(Exception e) {
                    responses.add(RemoteEfaMessage.createResponseResult(0, RemoteEfaMessage.ERROR_UNKNOWN, e.getMessage()));
                    Logger.log(e);
                }
                StringBuilder response = new StringBuilder();
                response.append("<?xml version='1.0' encoding='" + Daten.ENCODING_UTF + "' ?><" + RemoteEfaParser.XML_EFA + ">");
                for (int i=0; i<responses.size(); i++) {
                    response.append(responses.get(i).toString());
                }
                response.append("</" + RemoteEfaParser.XML_EFA + ">");
                if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 5)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "Sending Response [" + exchange.getRemoteAddress().toString() + "]: " + response.toString());
                }
                if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 2)) {
                    for (int i = 0; i < responses.size(); i++) {
                        RemoteEfaMessage msg = responses.get(i);
                        if (msg == null) {
                            continue;
                        }
                        Logger.log(Logger.DEBUG, Logger.MSG_REFA_TRACECOMMUNICATION, "Snd: "
                                + msg.getOperationName()
                                + " (" + msg.toString().length() + " bytes)");
                    }
                }
                OutputStream responseBody = RemoteEfaMessage.getOutputStream(exchange.getResponseBody());
                responseBody.write(response.toString().getBytes(Daten.ENCODING_UTF));
                responseBody.close();
            }
        }
    }

    class SessionTimeoutThread extends Thread {

        public void run() {
        	this.setName("RemoteEfaServer.SessionTimeoutThread");
            while(true) {
                try {
                    Thread.sleep(60 * 1000);
                } catch(InterruptedException eignore) {
                }
                try {
                    synchronized (sessions) {
                        long now = System.currentTimeMillis();
                        String[] keys = sessions.keySet().toArray(new String[0]);
                        for (String key : keys) {
                            Long timeout = sessionTimeouts.get(key);
                            if (timeout >= now) {
                                sessions.remove(key);
                                sessionTimeouts.remove(key);
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.logdebug(e);
                }
            }
        }

    }

    private Vector<RemoteEfaMessage> getRequests(RemoteEfaMessage.EfaMessageInputStream eis, InetSocketAddress peerAddress) {
        if (eis == null || eis.in == null) {
            return null;
        }
        BufferedInputStream in = eis.in;
        if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 5)) {
            try {
                in.mark(1024*1024); // tracing will break messages if they are larger than 1 MB
                if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 9)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "Got RemoteEfaRequest:");
                    BufferedReader buf = new BufferedReader(new InputStreamReader(in));
                    String s;
                    Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "-- REQUEST START --");
                    while ((s = buf.readLine()) != null) {
                        Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "   " + s);
                    }
                    Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "-- REQUEST END --");
                    in.reset();
                } else {
                    BufferedReader buf = new BufferedReader(new InputStreamReader(in));
                    String s;
                    while ((s = buf.readLine()) != null) {
                        Logger.log(Logger.DEBUG, Logger.MSG_REFA_DEBUGCOMMUNICATION, "Got Request [" + peerAddress.toString() + "]:" + s);
                    }
                }
                in.reset();
            } catch (Exception e) {
                Logger.logdebug(e);
            }
        }

        try {
            XMLReader parser = EfaUtil.getXMLReader();
            RemoteEfaParser responseHandler = new RemoteEfaParser(null);
            parser.setContentHandler(responseHandler);
            parser.parse(new InputSource(in));
            if (responseHandler.isDocumentComplete()) {
                return responseHandler.getMessages();
            } else {
                return null;
            }
        } catch(Exception e) {
            Logger.log(e);
            return null;
        }
    }

    private Vector<RemoteEfaMessage> handleRequests(Vector<RemoteEfaMessage> requests, InetSocketAddress peerAddress) {
        if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 2)) {
            for (int i=0; i<requests.size(); i++) {
                RemoteEfaMessage msg = requests.get(i);
                if (msg == null) {
                    continue;
                }
                Logger.log(Logger.DEBUG, Logger.MSG_REFA_TRACECOMMUNICATION, "Rcv: " +
                        msg.getStorageObjectName() + "." + msg.getStorageObjectType() + ":" +
                        msg.getOperationName() +
                        " (" + msg.toString().length() + " bytes)");
            }
        }
        Vector<RemoteEfaMessage> responses = new Vector<RemoteEfaMessage>();
        try {
            for (int i=0; i<requests.size(); i++) {
                RemoteEfaMessage request = requests.get(i);
                if (request == null) {
                    responses.add(RemoteEfaMessage.createResponseResult(0, RemoteEfaMessage.ERROR_INVALIDREQUEST, "Invalid Request: <null>"));
                    break;
                }
                String operation = request.getOperationName();
                int msgId = request.getMsgId();
                if (msgId < 1) {
                    responses.add(RemoteEfaMessage.createResponseResult(0, RemoteEfaMessage.ERROR_INVALIDREQUEST, "Invalid Request ID: " + msgId));
                    break;
                }
                if (operation == null) {
                    responses.add(RemoteEfaMessage.createResponseResult(msgId, RemoteEfaMessage.ERROR_INVALIDREQUEST, "Invalid Operation: <null>"));
                    break;
                }

                String newSessionId = null;
                // is this a login?
                if (request.getSessionId() == null) {
                    RemoteEfaMessage loginResult = requestLogin(request, peerAddress);
                    if (loginResult != null || request.getSessionId() == null) {
                        // login failed
                        responses.add(loginResult);
                        break;
                    } else {
                        // login successful

                        // this is a newly generated sessionID! It was generated by the server
                        // and not provided by the client (the server added it to the request in requestLogin()
                        newSessionId = request.getSessionId();
                    }
                }

                // if not a login, then this request must provide a valid session id
                AdminRecord admin = getSessionAdmin(request.getSessionId());
                if (admin == null) {
                    responses.add(RemoteEfaMessage.createResponseResult(msgId, RemoteEfaMessage.ERROR_INVALIDSESSIONID, "Invalid SessionID"));
                    break;
                }

                // find the storage object referenced in this request
                String storageObjectType = request.getStorageObjectType();
                String storageObjectName = request.getStorageObjectName();
                if (storageObjectType == null || storageObjectType.length() == 0 ||
                    storageObjectName == null || storageObjectName.length() == 0) {
                    responses.add(RemoteEfaMessage.createResponseResult(msgId, RemoteEfaMessage.ERROR_NOSTORAGEOBJECT, "No StorageObject specified"));
                    break;
                }
                
                IDataAccess dataAccess = request.getDataAccesss();
                StorageObject p = (dataAccess != null ? dataAccess.getPersistence() : null);

                if (p == null && !RemoteCommand.DATATYPE.equals(request.getStorageObjectType())) {
                    responses.add(RemoteEfaMessage.createResponseResult(msgId, RemoteEfaMessage.ERROR_UNKNOWNSTORAGEOBJECT,
                            "StorageObject not found: " + storageObjectName + "." + storageObjectType));
                    break;
                }

                while(true) { // not a loop

                    // now try to find the operation for this request
                    if (operation.equals(RemoteEfaMessage.OPERATION_EXISTSSTORAGEOBJECT)) {
                        responses.add(requestExistsStorageObject(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_OPENSTORAGEOBJECT)) {
                        responses.add(requestOpenStorageObject(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_CREATESTORAGEOBJECT)) {
                        responses.add(requestCreateStorageObject(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_ISSTORAGEOBJECTOPEN)) {
                        responses.add(requestIsStorageObjectOpen(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_CLOSESTORAGEOBJECT)) {
                        responses.add(requestCloseStorageObject(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_DELETESTORAGEOBJECT)) {
                        responses.add(requestDeleteStorageObject(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_ACQUIREGLOBALLOCK)) {
                        responses.add(requestAcquireGlobalLock(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_ACQUIRELOCALLOCK)) {
                        responses.add(requestAcquireLocalLock(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_RELEASEGLOBALLOCK)) {
                        responses.add(requestReleaseGlobalLock(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_RELEASELOCALLOCK)) {
                        responses.add(requestReleaseLocalLock(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GETNUMBEROFRECORDS)) {
                        responses.add(requestGetNumberOfRecords(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GETSCN)) {
                        responses.add(requestGetSCN(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_ADD)) {
                        responses.add(requestAdd(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_ADDVALIDAT)) {
                        responses.add(requestAddValidAt(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_ADDALL)) {
                        responses.add(requestAddAll(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GET)) {
                        responses.add(requestGet(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GETALLKEYS)) {
                        responses.add(requestGetAllKeys(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GETBYFIELDS)) {
                        responses.add(requestGetByFields(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GETVALIDANY)) {
                        responses.add(requestGetValidAny(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GETVALIDAT)) {
                        responses.add(requestGetValidAt(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GETVALIDLATEST)) {
                        responses.add(requestGetValidLatest(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GETVALIDNEAREST)) {
                        responses.add(requestGetValidNearest(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_ISVALIDANY)) {
                        responses.add(requestIsValidAny(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_UPDATE)) {
                        responses.add(requestUpdate(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_CHANGEVALIDITY)) {
                        responses.add(requestChangeValidity(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_DELETE)) {
                        responses.add(requestDelete(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_DELETEVERSIONIZED)) {
                        responses.add(requestDeleteVersionized(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_DELETEVERSIONIZEDALL)) {
                        responses.add(requestDeleteVersionizedAll(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_COUNTRECORDS)) {
                        responses.add(requestCountRecords(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_TRUNCATEALLDATA)) {
                        responses.add(requestTruncateAllData(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GETFIRST)) {
                        responses.add(requestGetFirst(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_GETLAST)) {
                        responses.add(requestGetLast(request, admin, p));
                        break;
                    }
                    if (operation.equals(RemoteEfaMessage.OPERATION_SETINOPENINGMODE)) {
                        responses.add(requestSetInOpeningStorageObject(request, admin, p));
                        break;
                    }

                    if (operation.equals(RemoteEfaMessage.OPERATION_CMD_EXITEFA)) {
                        responses.add(requestCmdExitEfa(request, admin));
                        break;
                    }

                    if (operation.equals(RemoteEfaMessage.OPERATION_CMD_ONLINEUPDATE)) {
                        responses.add(requestCmdOnlineUpdate(request, admin));
                        break;
                    }

                    // unknown request
                    responses.add(RemoteEfaMessage.createResponseResult(msgId, RemoteEfaMessage.ERROR_INVALIDREQUEST, "Unsupported Request: " + request.toString()));
                    break;
                }

                if (responses.size() > 0 && newSessionId != null) {
                    RemoteEfaMessage firstResponse = responses.get(0);
                    if (firstResponse != null) {
                        // add new SessionID to response
                        firstResponse.addField(RemoteEfaMessage.FIELD_SESSIONID, newSessionId);
                        firstResponse.setAdminRecord(admin);
                    }
                }
                if (responses.size() > 0 && p != null) {
                    RemoteEfaMessage lastResponse = responses.get(responses.size() - 1);
                    if (lastResponse != null && lastResponse.getResultCode() == RemoteEfaMessage.RESULT_OK) {
                        // add SCN to response
                        lastResponse.addField(RemoteEfaMessage.FIELD_SCN, Long.toString(p.data().getSCN()));
                        // add NumberOfRecords to response
                        lastResponse.addField(RemoteEfaMessage.FIELD_TOTALRECORDCOUNT, Long.toString(p.dataAccess.getNumberOfRecords()));
                    }
                }

            }
        } catch (Exception e) {
            Logger.log(e);
            responses.add(RemoteEfaMessage.createResponseResult(0, RemoteEfaMessage.ERROR_UNKNOWN, e.getMessage()));
        }
        return responses;
    }

    private String createSessionId(AdminRecord admin) {

        String sid = null;
        try {
            synchronized (sessions) {
                if (prng == null) {
                    prng = SecureRandom.getInstance("SHA1PRNG");
                }
                if (sha == null) {
                    sha = MessageDigest.getInstance("SHA-1");
                }
                byte[] idBytes = new byte[8];
                prng.nextBytes(idBytes);
                byte[] result = sha.digest(idBytes);
                sid = Base64.encodeBytes(result);
                sessions.put(sid, admin);
                sessionTimeouts.put(sid, System.currentTimeMillis() + SESSION_TIMEOUT);
            }
        } catch (Exception e) {
            Logger.log(e);
        }
        return sid;
    }

    private AdminRecord getSessionAdmin(String sessionId) {
        if (sessionId == null || sessionId.length() == 0) {
            return null;
        }
        synchronized(sessions) {
            return sessions.get(sessionId);
        }
    }

    // ===================================== Login =====================================

    private RemoteEfaMessage requestLogin(RemoteEfaMessage request, InetSocketAddress peerAddress) {
        String username = request.getUsername();
        String password = request.getPassword();
        String pid = request.getPid();
        if (Daten.applPID.equals(pid)) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_SELFLOGIN, "Self Login Attempt");
        }
        if (Logger.isTraceOn(Logger.TT_REMOTEEFA, 1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_REFA_SERVERLOG,
                    "efaRemote Login Attempt from " + peerAddress + ": Username=" + username + " Password=***");
        }
        if (Daten.admins != null && Daten.admins.isOpen()) {
            AdminRecord admin = Daten.admins.login(username, password);
            if (admin == null) {
                Logger.log(Logger.WARNING, Logger.MSG_REFA_SERVERLOG,
                    International.getMessage("efaRemote Login von {ipaddress} fehlgeschlagen", peerAddress.toString()) +
                    ": " + International.getString("Admin") + "=" + username);
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_INVALIDLOGIN, "Invalid Login");
            } else {
                if (!admin.isAllowedRemoteAccess()) {
                    Logger.log(Logger.WARNING, Logger.MSG_REFA_SERVERLOG,
                            International.getMessage("efaRemote Login von {ipaddress} mit ungenügenden Rechten", peerAddress.toString())
                            + ": " + International.getString("Admin") + "=" + username);
                    return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_NOPERMISSION, "No Permission");
                }

                // avoid duplicate lolastLogging for logins for various storage objects
                String logType = Logger.INFO;
                synchronized (syncObject) {
                    if (lastLoginAdmin != null && lastLoginIp != null
                            && System.currentTimeMillis() - lastLoginTime < 60 * 1000
                            && lastLoginAdmin.equals(username) && lastLoginIp.equals(peerAddress.toString())) {
                        // same login within one minute
                        logType = Logger.DEBUG;
                    } else {
                        lastLoginAdmin = username;
                        lastLoginIp = peerAddress.toString();
                        lastLoginTime = System.currentTimeMillis();
                    }
                }

                if (logType.equals(Logger.INFO) || Logger.isTraceOn(Logger.TT_REMOTEEFA, 2)) {
                    Logger.log(Logger.INFO, Logger.MSG_REFA_SERVERLOG,
                            International.getMessage("efaRemote Login von {ipaddress} erfolgreich", peerAddress.toString())
                            + ": " + International.getString("Admin") + "=" + username);
                }
                request.addField(RemoteEfaMessage.FIELD_SESSIONID, createSessionId(admin));
                request.setAdminRecord(admin);

                return null; // login successful
            }
        } else {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNABLETOCOMPLY, "Unable to comply");
        }
    }


    // =========================== Storage Object Methods ===========================

    private RemoteEfaMessage requestExistsStorageObject(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            if (p.dataAccess.existsStorageObject()) {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            } else {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_FALSE, "StorageObject does not exists");
            }
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestOpenStorageObject(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            p.dataAccess.openStorageObject();
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestCreateStorageObject(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            p.dataAccess.createStorageObject();
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestIsStorageObjectOpen(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            if (p.dataAccess.isStorageObjectOpen()) {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            } else {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_FALSE, "StorageObject is not open");
            }
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestCloseStorageObject(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        // nothing to do: we don't close our storage object just because the client wants us to
        // so we just do nothing and send back an OK
        return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
    }

    private RemoteEfaMessage requestDeleteStorageObject(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_NOTYETSUPPORTED, "Operation not supported");
    }


    // =========================== Lock Methods ===========================

    private RemoteEfaMessage requestAcquireGlobalLock(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            long lockId = p.dataAccess.acquireGlobalLock();
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            response.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockId));
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_FALSE, e.getMessage());
        }
    }

    private RemoteEfaMessage requestAcquireLocalLock(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            DataKey k = request.getKey(0);
            if (k == null) {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_INVALIDREQUEST, "No DataKey given");
            }
            long lockId = p.dataAccess.acquireLocalLock(k);
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            response.addField(RemoteEfaMessage.FIELD_LOCKID, Long.toString(lockId));
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_FALSE, e.getMessage());
        }
    }

    private RemoteEfaMessage requestReleaseGlobalLock(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            long lockId = request.getLockId();
            if (lockId < 0) {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_INVALIDREQUEST, "No Lock given");
            }
            if (p.dataAccess.releaseGlobalLock(lockId)) {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            } else {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_FALSE, "Global Lock not released");
            }
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.getMessage());
        }
    }

    private RemoteEfaMessage requestReleaseLocalLock(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            long lockId = request.getLockId();
            if (lockId < 0) {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_INVALIDREQUEST, "No Lock given");
            }
            if (p.dataAccess.releaseLocalLock(lockId)) {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            } else {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_FALSE, "Local Lock not released");
            }
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.getMessage());
        }
    }


    // =========================== Global Data Methods ===========================

    private RemoteEfaMessage requestGetNumberOfRecords(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            response.addField(RemoteEfaMessage.FIELD_LONGVALUE, Long.toString(p.dataAccess.getNumberOfRecords()));
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestGetSCN(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            response.addField(RemoteEfaMessage.FIELD_SCN, Long.toString(p.dataAccess.getSCN()));
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    // =========================== Data Modification Methods ===========================

    private RemoteEfaMessage requestAdd(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            p.data().add(request.getRecord(0), request.getLockId());
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestAddValidAt(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            DataKey k = p.data().addValidAt(request.getRecord(0), request.getTimestamp(), request.getLockId());
            RemoteEfaMessage response =  RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            if (k != null) {
                response.addKey(k);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestAddAll(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            if (request.getRecords() != null) {
                for (DataRecord r : request.getRecords()) {
                    p.data().add(r, request.getLockId());
                }
            }
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestGet(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            DataRecord r = p.data().get(request.getKey(0));
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            if (r != null) {
                response.addRecord(r);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestGetAllKeys(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            boolean prefetch = request.getPrefetch();
            DataKey[] keys = p.data().getAllKeys();
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            for (int i=0; keys != null && i<keys.length; i++) {
                response.addKey(keys[i]);

                if (prefetch) {
                    // also send record (prefetch for cache)
                    try {
                        DataRecord r = p.data().get(keys[i]);
                        if (r != null) {
                            response.addRecord(r);
                        }
                    } catch (Exception eignore) {
                    }
                }
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestGetByFields(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            DataKey[] keys = p.data().getByFields(request.getFieldArrayNames(), request.getFieldArrayValues(), request.getTimestamp());
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            for (int i=0; keys != null && i<keys.length; i++) {
                response.addKey(keys[i]);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestGetValidAny(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            DataRecord[] records = p.data().getValidAny(request.getKey(0));
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            for (int i=0; records != null && i<records.length; i++) {
                response.addRecord(records[i]);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestGetValidAt(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            DataRecord r = p.data().getValidAt(request.getKey(0), request.getTimestamp());
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            if (r != null) {
                response.addRecord(r);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestGetValidLatest(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            DataRecord r = p.data().getValidLatest(request.getKey(0));
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            if (r != null) {
                response.addRecord(r);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestGetValidNearest(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            DataRecord r = p.data().getValidNearest(request.getKey(0), request.getValidFrom(), request.getInvalidFrom(), request.getTimestamp());
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            if (r != null) {
                response.addRecord(r);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestIsValidAny(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            boolean validAny = p.data().isValidAny(request.getKey(0));
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), (validAny ? RemoteEfaMessage.RESULT_OK : RemoteEfaMessage.RESULT_FALSE), null);
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestUpdate(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            p.data().update(request.getRecord(0), request.getLockId());
            RemoteEfaMessage resp = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            resp.addRecord(p.data().get(request.getRecord(0).getKey())); // return the updated record with the new change_count
            return resp;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestChangeValidity(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            p.data().changeValidity(request.getRecord(0), request.getValidFrom(), request.getInvalidFrom(), request.getLockId());
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestDelete(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            p.data().delete(request.getKey(0), request.getLockId());
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestDeleteVersionized(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            p.data().deleteVersionized(request.getKey(0), request.getMerge(), request.getLockId());
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestDeleteVersionizedAll(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            p.data().deleteVersionizedAll(request.getKey(0), request.getTimestamp(), request.getLockId());
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestCountRecords(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            long recs = p.data().countRecords(request.getFieldArrayNames(), request.getFieldArrayValues());
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            response.addField(RemoteEfaMessage.FIELD_LONGVALUE, Long.toString(recs));
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestTruncateAllData(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            p.data().truncateAllData();
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestGetFirst(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            DataRecord r = p.data().getFirst();
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            if (r != null) {
                response.addRecord(r);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestGetLast(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            DataRecord r = p.data().getLast();
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            if (r != null) {
                response.addRecord(r);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestSetInOpeningStorageObject(RemoteEfaMessage request, AdminRecord admin, StorageObject p) {
        try {
            p.data().setInOpeningStorageObject(request.getBoolean());
            RemoteEfaMessage response = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestCmdExitEfa(RemoteEfaMessage request, AdminRecord admin) {
        try {
            if (!admin.isAllowedExitEfa()) {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_NOPERMISSION, "No Permission to update efa");
            }
            final boolean restart = request.getBoolean();
            Logger.log(Logger.INFO, Logger.MSG_EVT_REMOTEEFAEXIT, International.getString("Remote-Kommando") + ": " +
                    International.getString("Beenden von efa"));
            final AdminRecord _admin = admin;
            RemoteEfaMessage response;
            if (EfaBoathouseFrame.efaBoathouseFrame != null) {
                EfaBoathouseFrame.efaBoathouseFrame.cancelRunInThreadWithDelay(EfaBoathouseFrame.EFA_EXIT_REASON_USER, _admin, restart);
                response  = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
            } else {
                response  = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_FALSE, null);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }

    private RemoteEfaMessage requestCmdOnlineUpdate(RemoteEfaMessage request, AdminRecord admin) {
        try {
            if (!admin.isAllowedUpdateEfa()) {
                return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_NOPERMISSION, "No Permission to update efa");
            }
            Logger.log(Logger.INFO, Logger.MSG_EVT_REMOTEONLINEUPDATE, International.getString("Remote-Kommando") + ": " +
                    International.getString("Online-Update"));

            final AdminRecord _admin = admin;
            RemoteEfaMessage response;
            if (EfaBoathouseFrame.efaBoathouseFrame != null) {
                if (OnlineUpdate.runOnlineUpdate(null, Daten.ONLINEUPDATE_INFO)) {
                    EfaBoathouseFrame.efaBoathouseFrame.cancelRunInThreadWithDelay(EfaBoathouseFrame.EFA_EXIT_REASON_ONLINEUPDATE, _admin, true);
                    response  = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_OK, null);
                } else {
                    response  = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_FALSE, OnlineUpdate.getLastError());
                }
            } else {
                response  = RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.RESULT_FALSE, null);
            }
            return response;
        } catch(Exception e) {
            return RemoteEfaMessage.createResponseResult(request.getMsgId(), RemoteEfaMessage.ERROR_UNKNOWN, e.toString());
        }
    }


    public static void main(String[] args) throws IOException {
        Logger.setDebugLogging(true, true);
        Logger.setTraceTopic("0x4000", true);
        new RemoteEfaServer(3834, true);
    }

}
