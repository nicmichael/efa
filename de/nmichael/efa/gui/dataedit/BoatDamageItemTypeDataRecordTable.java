package de.nmichael.efa.gui.dataedit;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListenerDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.data.BoatDamageRecord;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.gui.util.TableItemHeader;

public class BoatDamageItemTypeDataRecordTable extends ItemTypeDataRecordTable {

	private Boolean showOpenDamagesOnly = true;
	
	public BoatDamageItemTypeDataRecordTable(String name, TableItemHeader[] tableHeader, StorageObject persistence,
			long validAt, AdminRecord admin, String filterFieldName, String filterFieldValue, String[] actions,
			int[] actionTypes, String[] actionIcons, IItemListenerDataRecordTable itemListenerActionTable, int type,
			String category, String description) {

		super("TABLE", persistence.createNewRecord().getGuiTableHeader(), persistence, validAt, admin, filterFieldName,
				filterFieldValue, // defaults are null
				actions, actionTypes, actionIcons, // default actions: new, edit, delete
				itemListenerActionTable, type, category, description);
	}

	// Diese Methode soll die Eingabe als Datum interpretieren,
	// und prüfen, ob die Datensätze der Liste zu dem Datum passen.
	protected boolean filterFromToAppliesToDate(DataRecord theDataRecord, String filterValue) {

		if (filterValue == null) {
			return false;
		}

		DataTypeDate curDate = DataTypeDate.parseDate(filterValue.trim());
		if (!curDate.isSet()) {
			// die Eingabe ist kein Datum. Damit können wir hier keine zutreffenden Zeilen
			// ermitteln.
			return false;
		}

		String theDateFrom = theDataRecord.getAsString(BoatDamageRecord.REPORTDATE);
		String theDateTo = theDataRecord.getAsString(BoatDamageRecord.FIXDATE);

		if ((theDateFrom == null)) {
			return false;
		} else {

			DataTypeDate fromDate = DataTypeDate.parseDate(theDateFrom);
			DataTypeDate toDate = null;

			if ((theDateTo != null)) {
				toDate = DataTypeDate.parseDate(theDateTo);
				if ((fromDate.isSet() && toDate.isSet())) {

					// true, wenn das eingegebene Datum ist zwischen den beiden Datumswerten ist
					return (curDate.isAfterOrEqual(fromDate) && curDate.isBeforeOrEqual(toDate));

				} else {
					return false;
				}
			} else {
				if (fromDate.isSet()) {
					return (curDate.isAfterOrEqual(fromDate));
				} else {
					return false;
				}
			}

		}
	}

    protected boolean removeItemByCustomFilter(DataRecord theDataRecord) {

    	if (this.showOpenDamagesOnly) {
    		//item shall be removed if it is already fixed
    		return ((BoatDamageRecord) theDataRecord).getFixed();//casting is safe here
    	} else {
    		return false;
    	}
    }
	
	public Boolean getShowOpenDamagesOnly() {
		return showOpenDamagesOnly;
	}

	public void setShowOpenDamagesOnly(Boolean bShowOpenDamagesOnly) {
		this.showOpenDamagesOnly = bShowOpenDamagesOnly;
		// When the item is set, apply the filter...
		updateData();
		showValue(); //updateData alone won't suffice
	}
}
