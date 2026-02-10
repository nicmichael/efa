/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data.importefa1;

import de.nmichael.efa.efa1.*;
import de.nmichael.efa.util.*;
import java.util.*;

public class ImportAddresses extends ImportBase {

    private ImportMetadata meta;
    private String efa1fname;

    public ImportAddresses(ImportTask task, String efa1fname, ImportMetadata meta) {
        super(task);
        this.meta = meta;
        this.efa1fname = efa1fname;
    }

    public String getDescription() {
        return International.getString("Adressen");
    }

    public boolean runImport() {
        Adressen adr = new Adressen(efa1fname);
        adr.dontEverWrite();
        try {
            Hashtable<String,String> h = new Hashtable<String,String>();
            logInfo(International.getMessage("Importiere {list} aus {file} ...", getDescription(), efa1fname));
            if (!adr.readFile()) {
                logError(LogString.fileOpenFailed(efa1fname, getDescription()));
                return false;
            }

            DatenFelder d = adr.getCompleteFirst();
            while (d != null) {
                String name = d.get(Adressen.NAME).trim();
                String adresse = d.get(Adressen.ADRESSE).trim();
                if (name.length() > 0 && adresse.length() > 0) {
                    h.put(name, adresse);
                    logDetail(International.getMessage("Importiere Eintrag: {entry}", name + ": " + adresse));
                }
                d = adr.getCompleteNext();
            }
            task.setAddresses(h);

        } catch(Exception e) {
            logError(International.getMessage("Import von {list} aus {file} ist fehlgeschlagen.", getDescription(), adr.getFileName()));
            logError(e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
