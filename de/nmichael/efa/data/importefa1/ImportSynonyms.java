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

public class ImportSynonyms extends ImportBase {

    private ImportMetadata meta;
    private String efa1fname;

    public ImportSynonyms(ImportTask task, String efa1fname, ImportMetadata meta) {
        super(task);
        this.meta = meta;
        this.efa1fname = efa1fname;
    }

    public String getDescription() {
        return International.getString("Synonyme");
    }

    public boolean runImport() {
        Synonyme syn = new Synonyme(efa1fname);
        try {
            //Hashtable<String,ArrayList<String>> h = new Hashtable<String,ArrayList<String>>();
            Hashtable<String,String> h = new Hashtable<String,String>();
            logInfo(International.getMessage("Importiere {list} aus {file} ...", getDescription(), efa1fname));
            syn.dontEverWrite();
            if (!syn.readFile()) {
                logError(LogString.fileOpenFailed(efa1fname, getDescription()));
                return false;
            }
            
            DatenFelder d = syn.getCompleteFirst();
            while (d != null) {
                String oname = d.get(Synonyme.ORIGINAL).trim();
                String sname = d.get(Synonyme.SYNONYM).trim();
                if (oname.length() > 0 && sname.length() > 0) {
                    h.put(sname, oname);
                    /*
                    ArrayList<String> list = h.get(oname);
                    if (list == null) {
                        list = new ArrayList<String>();
                    }
                    if (!list.contains(sname)) {
                        list.add(sname);
                    }
                    h.put(oname, list);
                     */
                    logDetail(International.getMessage("Importiere Eintrag: {entry}", sname + " -> " + oname));
                }
                d = syn.getCompleteNext();
            }
            switch(meta.type) {
                case ImportMetadata.TYPE_SYNONYME_MITGLIEDER:
                    task.setSynonymeMitglieder(h);
                    break;
                case ImportMetadata.TYPE_SYNONYME_BOOTE:
                    task.setSynonymeBoote(h);
                    break;
                case ImportMetadata.TYPE_SYNONYME_ZIELE:
                    task.setSynonymeZiele(h);
                    break;
            }
            
        } catch(Exception e) {
            logError(International.getMessage("Import von {list} aus {file} ist fehlgeschlagen.", getDescription(), syn.getFileName()));
            logError(e.toString());
            Logger.logdebug(e);
            return false;
        }
        return true;
    }

}
