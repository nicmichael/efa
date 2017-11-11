/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.cli;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;
import java.lang.String;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

public class MenuData extends MenuBase {

    public static final String CMD_LIST   = "list";
    public static final String CMD_SHOW   = "show";
    public static final String CMD_EXPORT = "export";
    public static final String CMD_IMPORT = "import";

    protected StorageObject storageObject;
    protected String storageObjectDescription;
    protected Hashtable<Integer,DataRecord> lastListResult;

    public MenuData(CLI cli) {
        super(cli);
    }

    public void printHelpContext() {
        printUsage(CMD_LIST,        "[all|invisible|deleted]", "list " + storageObjectDescription);
        printUsage(CMD_SHOW,        "[name|index]", "show record");
        printUsage(CMD_EXPORT,      "[-format=xml|csv] <filename>", "export records");
        printUsage(CMD_IMPORT,      "[-encoding=ENCODING] [-csvsep=X] [-csvquote=X] [-impmode=add|update|addupdate] [-updversion=update|new] [-entryno=dupskip|dupadd|alwaysadd] <filename>", "import records");
    }

    public void list(String args) {
        if (storageObject == null) {
            return;
        }

        boolean all =       (args != null && args.trim().equalsIgnoreCase("all"));
        boolean invisible = (args != null && args.trim().equalsIgnoreCase("invisible"));
        boolean deleted =   (args != null && args.trim().equalsIgnoreCase("deleted"));
        boolean versionized = storageObject.data().getMetaData().isVersionized();
        boolean normal =    (!all && !invisible && !deleted && !versionized);
        long now = System.currentTimeMillis();
        lastListResult = new Hashtable<Integer,DataRecord>();
        int idx = 0;
        try {
            DataKeyIterator it = storageObject.data().getStaticIterator();
            DataKey k = it.getFirst();
            while (k != null) {
                DataRecord r = storageObject.data().get(k);
                if (r != null) {
                    boolean show = all ||
                                   (invisible && r.getInvisible()) ||
                                   (deleted && r.getDeleted()) ||
                                   (versionized && !invisible && !deleted && r.isValidAt(now)) ||
                                   (normal && !r.getInvisible() && !r.getDeleted());
                    if (show) {
                        String name = r.getQualifiedName();
                        String notes = null;
                        if (r.getInvisible()) {
                            notes = (notes != null ? notes + " " : "") + "[invisible]";
                        }
                        if (r.getDeleted()) {
                            notes = (notes != null ? notes + " " : "") + "[deleted]";
                        }
                        if (versionized) {
                            notes = (notes != null ? notes + " " : "") + "[" + r.getValidRangeString() + "]";
                        }
                        String txt = (notes != null ? EfaUtil.getString(name, 40) + " " + notes : name);
                        cli.loginfo(EfaUtil.int2String(++idx, 5, false) + ": " + txt);
                        lastListResult.put(idx, r);
                    }
                }
                k = it.getNext();
            }
        } catch(Exception e) {
            Logger.log(e);
        }
    }


    public void show(String args) {
        if (storageObject == null) {
            return;
        }
        DataRecord r = getRecordFromArgs(args);
        if (r == null) {
            cli.logerr("Record '"+args+"' not found.");
            return;
        }
        Vector<IItemType> items = r.getGuiItems(cli.getAdminRecord());
        for (int i=0; items != null && i<items.size(); i++) {
            IItemType item = items.get(i);
            cli.loginfo(EfaUtil.getString(item.getName(), 25) + ": " + item.toString());
        }
    }


    public void exportData(String args) {
        if (storageObject == null) {
            return;
        }
        Hashtable<String,String> options = getOptionsFromArgs(args);
        args = removeOptionsFromArgs(args);
        DataExport.Format format = DataExport.Format.xml;
        if (options.get("format") != null && options.get("format").equalsIgnoreCase("csv")) {
            format = DataExport.Format.csv;
        }
        String filename = args;
        String encoding = Daten.ENCODING_UTF;
        if (format == DataExport.Format.csv) {
            String charset = Charset.defaultCharset().toString();
            if (Daten.ENCODING_ISO.toLowerCase().equals(charset.toLowerCase())) {
                encoding = Daten.ENCODING_ISO;
            }
        }
       cli.loginfo("Exporting data ...");
       DataExport export = new DataExport(storageObject, System.currentTimeMillis(), null,
                storageObject.createNewRecord().getFields(), format,
                encoding, filename, DataExport.EXPORT_TYPE_TEXT);
        int count = export.runExport();
        cli.loginfo(count + " records exported to " + filename);
    }

    public void importData(String args) {
        if (storageObject == null) {
            return;
        }
        Hashtable<String,String> options = getOptionsFromArgs(args);
        args = removeOptionsFromArgs(args);
        String filename = args;
        String encoding = options.get("encoding");
        if (encoding == null || encoding.length() == 0) {
            encoding = Charset.defaultCharset().toString();
        }
        String csvSeparator = options.get("csvsep");
        String csvQuotes = options.get("csvquote");
        char csep = (csvSeparator != null && csvSeparator.length() > 0 ? csvSeparator.charAt(0) : '\0');
        char cquo = (csvQuotes != null && csvQuotes.length() > 0 ? csvQuotes.charAt(0) : '\0');
        String importMode = options.get("impmode");
        if (importMode != null && importMode.equalsIgnoreCase("add")) {
            importMode = DataImport.IMPORTMODE_ADD;
        } else if (importMode != null && importMode.equalsIgnoreCase("update")) {
            importMode = DataImport.IMPORTMODE_UPD;
        } else if (importMode != null && importMode.equalsIgnoreCase("addupdate")) {
            importMode = DataImport.IMPORTMODE_ADDUPD;
        } else {
            importMode = DataImport.IMPORTMODE_ADD;
        }
        String updMode = options.get("updversion");
        if (updMode != null && updMode.equalsIgnoreCase("update")) {
            updMode = DataImport.UPDMODE_UPDATEVALIDVERSION;
        } else if (updMode != null && updMode.equalsIgnoreCase("new")) {
            updMode = DataImport.UPPMODE_CREATENEWVERSION;
        } else {
            updMode = DataImport.UPDMODE_UPDATEVALIDVERSION;
        }
        String entryNo = options.get("entryno");
        if (entryNo != null && entryNo.equalsIgnoreCase("dupskip")) {
            entryNo = DataImport.ENTRYNO_DUPLICATE_SKIP;
        } else if (entryNo != null && entryNo.equalsIgnoreCase("dupadd")) {
            entryNo = DataImport.ENTRYNO_DUPLICATE_ADDEND;
        } else if (entryNo != null && entryNo.equalsIgnoreCase("alwaysadd")) {
            entryNo = DataImport.ENTRYNO_ALWAYS_ADDEND;
        } else {
            entryNo = DataImport.ENTRYNO_DUPLICATE_SKIP;
        }

        cli.loginfo("Importing data ...");
        DataImport imp = new DataImport(storageObject, filename, encoding, csep, cquo,
                importMode, updMode, entryNo,
                System.currentTimeMillis());
        if (Daten.isGuiAppl()) {
            imp.setProgressDialog(new ProgressDialog() {
                public void logInfo(String s) {
                    cli.loginfo(s);
                }
            }, true);
        }
        int count = 0;
        if (DataImport.isXmlFile(filename)) {
            count = imp.runXmlImport();
        } else {
            count = imp.runCsvImport();
        }
        cli.loginfo(count + " records imported from " + filename);
        if (count > 0) {
            // Start the Audit in the background to find any eventual inconsistencies
            (new Audit(Daten.project)).start();
        }
    }

    protected DataRecord getRecordFromArgs(String args) {
        if (args == null || args.length() == 0) {
            printHelpContext();
            return null;
        }
        if (storageObject == null) {
            return null;
        }

        DataRecord r = null;
        try {
            MetaData meta = storageObject.data().getMetaData();
            DataRecord dummyRecord = storageObject.createNewRecord();
            DataKey[] k;
            if (meta.isVersionized()) {
                k = storageObject.data().getByFields(dummyRecord.getQualifiedNameFields(),
                        dummyRecord.getQualifiedNameValues(args), System.currentTimeMillis());
            } else {
                k = storageObject.data().getByFields(dummyRecord.getQualifiedNameFields(),
                        dummyRecord.getQualifiedNameValues(args));
            }
            r = storageObject.data().get(k[0]);
        } catch(Exception e) {
        }
        if (r == null) {
            if (lastListResult == null || lastListResult.size() == 0) {
                cli.logerr("Please run a list command first.");
                return null;
            }
            int index = EfaUtil.string2int(args, -1);
            if (index < 0) {
                printHelpContext();
                return null;
            }
            r = lastListResult.get(index);
        }
        return r;
    }

    protected Hashtable<String, String> getOptionsFromArgs(String args) {
        Hashtable<String, String> options = new Hashtable<String, String>();
        try {
            StringTokenizer tok = new StringTokenizer(args, " ");
            while (tok.hasMoreTokens()) {
                String s = tok.nextToken().trim();
                if (s.startsWith("-")) {
                    int pos = s.indexOf("=");
                    String name = s.substring(1).toLowerCase();
                    String value = "";
                    if (pos > 0) {
                        name = s.substring(1, pos).toLowerCase();
                        value = s.substring(pos + 1);
                    }
                    options.put(name, value);
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return options;
    }

    protected String removeOptionsFromArgs(String args) {
        StringBuilder sb = new StringBuilder();
        StringTokenizer tok = new StringTokenizer(args, " ");
        if (tok.countTokens() == 0) {
            return args;
        }
        while (tok.hasMoreTokens()) {
            String s = tok.nextToken().trim();
            if (!s.startsWith("-")) {
                sb.append( (sb.length() > 0 ? " " : "") + s);
            }
        }
        return sb.toString();
    }

    public int runCommand(Stack<String> menuStack, String cmd, String args) {
        int ret = super.runCommand(menuStack, cmd, args);
        if (ret < 0) {
            if (cmd.equalsIgnoreCase(CMD_LIST)) {
                list(args);
                return CLI.RC_OK;
            }
            if (cmd.equalsIgnoreCase(CMD_SHOW)) {
                show(args);
                return CLI.RC_OK;
            }
            if (cmd.equalsIgnoreCase(CMD_EXPORT)) {
                exportData(args);
                return CLI.RC_OK;
            }
            if (cmd.equalsIgnoreCase(CMD_IMPORT)) {
                importData(args);
                return CLI.RC_OK;
            }
            return CLI.RC_UNKNOWN_COMMAND;
        } else {
            return ret;
        }
    }

}
