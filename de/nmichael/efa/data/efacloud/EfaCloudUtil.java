package de.nmichael.efa.data.efacloud;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;

public class EfaCloudUtil {
	
	/**
	 * This function cancels a currently running synchronisation on an efaCloud project.
	 * This is neccessary when opening the dialog for 
	 * - projects
	 * - logbooks
	 * - clubwork
	 * as those dialogs do open and close a lot of files.
	 * This makes an active efaCloud synchronisation fail with multiple scenarios, including a deadlock.
	 * 
	 * Registering a state change for stopping sync does not stop the sync at once.
	 * Instead, the currently running transaction is performed, and THEN the sync stops.
	 * So it is no use to stop the synchronisation via RegisterStateChange(), 
	 * because we'd had to wait until the sync ACTUALLY stops before proceeding. 
	 * 
	 * So, this method simply checks for an active efaCloud state and shows a message,
	 * leaving all other things up to the user.
	 */
	public static boolean isEfaCloudTXQueueActive() {
        if (Daten.project != null && Daten.project.getIsProjectStorageTypeEfaCloud()) {
        	int txState = TxRequestQueue.getInstance().getState();

        	if (txState == TxRequestQueue.QUEUE_IS_SYNCHRONIZING || txState == TxRequestQueue.QUEUE_IS_AUTHENTICATING || txState == TxRequestQueue.QUEUE_IS_WORKING) {
        	   Dialog.infoDialog(
        			   International.getString("Es läuft noch eine efaCloud-Synchronisation im Hintergrund.")+"\n\n"+
        	           International.getString("Währenddessen können keine Projekte, Fahrtenbücher oder Vereinsarbeitsbücher gewechselt werden.")+"\n\n"+
        		       International.getString("Warten Sie auf das Ende der Synchronisation, oder verwenden Sie das efaCloud-Menü, um die Synchronisation abzubrechen."));
        	   return true;
        	}
        }
        return false;
	}
	
}
