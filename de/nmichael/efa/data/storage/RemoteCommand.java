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

import de.nmichael.efa.data.Project;
import de.nmichael.efa.util.International;

public class RemoteCommand extends RemoteEfaClient {

    public static final String STORAGEOBJECT_NAME = "RemoteCommand";
    public static final String DATATYPE = "efa2command";

    public RemoteCommand(Project project) {
        super(project.getProjectStorageLocation(),
              project.getProjectStorageUsername(),
              project.getProjectStoragePassword(),
              STORAGEOBJECT_NAME,
              DATATYPE,
              International.getString("Kommando"));
    }


    public boolean exitEfa(boolean restart) {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_CMD_EXITEFA);
        request.addField(RemoteEfaMessage.FIELD_BOOLEAN, Boolean.toString(restart));
        boolean success = runSimpleRequest(request) == RemoteEfaMessage.RESULT_OK;
        return success;
    }

    public String onlineUpdate() {
        RemoteEfaMessage request = RemoteEfaMessage.createRequestData(1, getStorageObjectType(), getStorageObjectName(),
                RemoteEfaMessage.OPERATION_CMD_ONLINEUPDATE);
        RemoteEfaMessage result = runDataRequest(request);
        boolean success = (result != null && result.getResultCode() == RemoteEfaMessage.RESULT_OK);
        if (success) {
            return null;
        } else {
            String rtxt = (result != null ? result.getResultText() : International.getString("Fehler"));
            if (rtxt == null || rtxt.length() == 0) {
                rtxt = International.getString("Fehler");
            }
            return rtxt;
        }
    }

}
