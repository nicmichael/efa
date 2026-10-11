package de.nmichael.efa.core.items;

import java.util.Hashtable;

import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.International;

public class ItemTypeInternationalReplacement extends ItemTypeStringHashtable {

	public ItemTypeInternationalReplacement(String name, String value, boolean fieldsEditable, int type, String category,
			String description) {
		super(name, value, fieldsEditable, type, category, description);
	}

	@Override
	protected String getNewKeyFromGUI() {

		return Dialog.inputDialog(International.getString("Neuen Eintrag hinzufügen"),
				International.getString("Key aus efa_de.properties") + ": ");
	}

	public IItemType copyOf() {
		ItemTypeInternationalReplacement item = new ItemTypeInternationalReplacement(name, value, isEditable, type,
				category, description);
		String[] myKeys = this.getKeysArray();
		for (int i = 0; i < myKeys.length; i++) {
			String value = get(myKeys[i]);
			item.put(myKeys[i], value);
		}
		return item;
	}

	public Hashtable<String, String> getMap() {
		Hashtable<String, String> res = new Hashtable<String, String>();

		String[] keys = this.getKeysArray();

		for (int i = 0; i < keys.length; i++) {
			res.put(keys[i], this.get(keys[i]));
		}

		return res;
	}

}
