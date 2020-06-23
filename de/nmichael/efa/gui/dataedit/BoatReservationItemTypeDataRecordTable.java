package de.nmichael.efa.gui.dataedit;

import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemListenerDataRecordTable;
import de.nmichael.efa.core.items.ItemTypeDataRecordTable;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.data.BoatReservationRecord;

public class BoatReservationItemTypeDataRecordTable extends ItemTypeDataRecordTable {

	public BoatReservationItemTypeDataRecordTable(String name, TableItemHeader[] tableHeader, StorageObject persistence,
			long validAt, AdminRecord admin, String filterFieldName, String filterFieldValue, String[] actions,
			int[] actionTypes, String[] actionIcons, IItemListenerDataRecordTable itemListenerActionTable, int type,
			String category, String description) {
		super("TABLE", persistence.createNewRecord().getGuiTableHeader(), persistence, validAt, admin, filterFieldName,
				filterFieldValue, // defaults are null
				actions, actionTypes, actionIcons, // default actions: new, edit, delete
				itemListenerActionTable, type, category, description);
	}

	protected boolean filterFromToAppliesToDate(DataRecord theDataRecord, String filterValue) {

		if (filterValue == null) {
			return false;
		}

		DataTypeDate curDate = DataTypeDate.parseDate(filterValue.trim());
		if (!curDate.isSet()) {
			// Die Eingabe ist kein Datum. Damit können wir hier keine zutreffenden Zeilen ermitteln.
			return false;
		}

		if (theDataRecord.getAsString(BoatReservationRecord.TYPE).equals(BoatReservationRecord.TYPE_ONETIME)) {

			String theDateFrom = theDataRecord.getAsString(BoatReservationRecord.DATEFROM);
			String theDateTo = theDataRecord.getAsString(BoatReservationRecord.DATETO);

			if ((theDateFrom == null) || (theDateTo == null)) {
				return false;
			} else {

				DataTypeDate fromDate = DataTypeDate.parseDate(theDateFrom);
				DataTypeDate toDate = DataTypeDate.parseDate(theDateTo);

				if ((fromDate.isSet() && toDate.isSet())) {

					// true, wenn das eingegebene Datum zwischen den beiden Datumswerten der Reservierung ist

					return (curDate.isAfterOrEqual(fromDate) && curDate.isBeforeOrEqual(toDate));

				} else {
					return false;
				}
			}

		} else {
			// weekly reservation -
			// wir ermitteln den Wochentag aus dem eingegebenen Datum, und schauen, 
			// ob es zu diesem Wochentag eine Reservierung gibt.
			
			String strWeekday = curDate.getWeekdayAsEfaType();
			String strWeekdayFromRecord = theDataRecord.getAsString(BoatReservationRecord.DAYOFWEEK);
			
			return strWeekday.equals(strWeekdayFromRecord);
			
		}

	}

}
