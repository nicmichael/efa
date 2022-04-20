package de.nmichael.efa.gui.dataedit;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListenerDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.gui.util.TableItemHeader;

public class StatisticsItemTypeDataRecordTable extends ItemTypeDataRecordTable {

	public StatisticsItemTypeDataRecordTable(String name, TableItemHeader[] tableHeader, StorageObject persistence,
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

		String theDateFrom = theDataRecord.getAsString(StatisticsRecord.DATEFROM);
		String theDateTo = theDataRecord.getAsString(StatisticsRecord.DATETO);

		if ((theDateFrom == null) || (theDateTo == null)) {
			return false;
		} else {

			DataTypeDate fromDate = DataTypeDate.parseDate(theDateFrom);
			DataTypeDate toDate = DataTypeDate.parseDate(theDateTo);

			if ((fromDate.isSet() && toDate.isSet())) {

				// true, wenn das eingegebene Datum ist zwischen den beiden Datumswerten ist

				return (curDate.isAfterOrEqual(fromDate) && curDate.isBeforeOrEqual(toDate));

			} else {
				return false;
			}
		}
	}

}