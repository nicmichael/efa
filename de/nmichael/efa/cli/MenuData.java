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

import java.io.File;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.data.storage.Audit;
import de.nmichael.efa.data.storage.DataExport;
import de.nmichael.efa.data.storage.DataImport;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Email;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

public class MenuData extends MenuBase {

    private static final String EXPORT_OPTION_EMAIL = "email";
    private static final String EXPORT_OPTION_EMAILSUBJECT = "emailsubject";
	private static final String EXPORT_OPTION_FORMAT = "format";
	private static final String EXPORT_OPTION_CSV_SEPARATOR="csvsep";
	private static final String EXPORT_OPTION_CSV_QUOTE = "csvquote";
	private static final String EXPORT_OPTION_ENCODING = "encoding";
	private static final String EXPORT_OPTION_LOCALE = "csvlocale";
	
	private static final String CSV_DEFAULT_COLUMN_SEPARATOR = "|";
	private static final String CSV_DEFAULT_QUOTE = "\"";
	
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
        printUsage(CMD_EXPORT,      "[-format=xml|csv|csv_bom_utf8] [-encoding=ENCODING] [-csvlocale=LOCALE] [-csvsep=X] [-csvquote=X] [-email=emailadress] [-emailsubject=value] <filename>", "export records to a file and to an email adress");
        printUsage(CMD_IMPORT,      "[-encoding=ENCODING] [-csvsep=X] [-csvquote=X] [-impmode=add|update|addupdate] [-updversion=update|new] [-entryno=dupskip|dupadd|alwaysadd] <filename>", "import records");
        cli.loginfo("");
        cli.loginfo("ENCODING   - Any encoding, e.g. ISO-8859-1 or UTF-8. UTF-8 is default.");
        cli.loginfo("LOCALE     - Any ISO-Code for a country, e.g. DE or EN" );
        cli.loginfo("");
        cli.loginfo("Format	    - csv_bom_utf8 is neccessary for some spreadsheet programs to read UTF8-based CSV files.");
        cli.loginfo("");
        cli.loginfo("csvsep     - CSV field separator. A single character.");
        cli.loginfo("             Default for import and export: "+CSV_DEFAULT_COLUMN_SEPARATOR);
        cli.loginfo("csvquote   - CSV quote for text fields.");
        cli.loginfo("             Default for import and export: "+CSV_DEFAULT_QUOTE);
        cli.loginfo("");
        cli.loginfo("             If you want to set csvquote to empty, specify csvquote=  ");
        cli.loginfo("");
        cli.loginfo("<filename> - filename.ext        -> "+Daten.efaTmpDirectory+"filename.ext");
        cli.loginfo("             path/filename.ext   -> "+Daten.efaConfig.getValueEfaUserDirectory()+"path"+File.separator+"filename.ext");
        cli.loginfo("             ./path/filename.ext -> "+Daten.efaConfig.getValueEfaUserDirectory()+"path"+File.separator+"filename.ext");
        cli.loginfo("             ~/path/filename.ext -> "+Daten.userHomeDir+"path"+File.separator+"filename.ext");
        cli.loginfo("             You can also use absolute paths for the filename. The directories in the path must exist.");
        cli.loginfo("");
        if (File.separator.equals("\\")) {//show hint only on windows systems
        	cli.loginfo("             Windows: In efaCLI INTERACTIVE mode, use double \\\\ as path separators (like c:\\\\temp\\\\filename.ext)");
        	cli.loginfo("");
        }
        	
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

        String csvSeparator=CSV_DEFAULT_COLUMN_SEPARATOR;
        String csvQuote=CSV_DEFAULT_QUOTE;
        Locale csvLocale = null;  // if no locale is specified, the standard of the system is used.
        String emailSubj="";
        String filename = args;

        //if user forgets to specify a filename, we cannot proceed.
        if (filename == null || filename.trim().isEmpty()) {
            cli.loginfo("No filename specified. Cannot export data.");
            return;
        }
        
        filename = extendAndCorrectFilePath(filename);

        //------ get file format. xml is standard, if no other is set.
        DataExport.Format format = DataExport.Format.xml;
        if (options.get(EXPORT_OPTION_FORMAT) != null && options.get(EXPORT_OPTION_FORMAT).equalsIgnoreCase("csv")) {
            format = DataExport.Format.csv;
        }
        
        if (options.get(EXPORT_OPTION_FORMAT) != null && options.get(EXPORT_OPTION_FORMAT).equalsIgnoreCase("csv_bom_utf8")) {
            format = DataExport.Format.csv_bom_utf8;
        }
        
        //------ get csv export options
        //default values of csvSeparator and csvQuote have both been set earlier.
        //now check if the user has specified these parameters.

        if (options.get(EXPORT_OPTION_CSV_SEPARATOR)!=null) {
        	csvSeparator=options.get(EXPORT_OPTION_CSV_SEPARATOR);
        }
        
        if (options.get(EXPORT_OPTION_CSV_QUOTE)!=null) {
        	csvQuote=options.get(EXPORT_OPTION_CSV_QUOTE);
        }

        if (options.get(EXPORT_OPTION_LOCALE)!=null) {
        	csvLocale=Locale.forLanguageTag(options.get(EXPORT_OPTION_LOCALE));
        }

        //------ get Email subject, default and user specified 
        emailSubj="EfaCLI "+EfaUtil.getFilenameWithoutPath(filename)+" export " + ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).toOffsetDateTime().toString();
        if (options.get(EXPORT_OPTION_EMAILSUBJECT)!=null) {
        	emailSubj=options.get(EXPORT_OPTION_EMAILSUBJECT);
        	if (emailSubj.isEmpty()) {
        		emailSubj="EfaCLI Export " + ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).toOffsetDateTime().toString();
        	}
        }

        //set encoding
        String encoding = Daten.ENCODING_UTF;
        if (format == DataExport.Format.csv) {
            String charset = Charset.defaultCharset().toString();
            if (Daten.ENCODING_ISO.toLowerCase().equals(charset.toLowerCase())) {
                encoding = Daten.ENCODING_ISO;
            }
        }
        
        //override encoding, if user set an explicit option
        if (options.get(EXPORT_OPTION_ENCODING)!=null) {
        	encoding=options.get(EXPORT_OPTION_ENCODING);
        	if (encoding.isEmpty()) {
        		encoding = Charset.defaultCharset().toString();
        	}
        }        


        
       //------ export the data         
       cli.loginfo("Exporting data ...");
       DataExport export = new DataExport(storageObject, System.currentTimeMillis(), null,
                storageObject.createNewRecord().getFields(), format,
                encoding, filename, DataExport.EXPORT_TYPE_TEXT, csvSeparator, csvQuote, csvLocale);
        int count = export.runExport();
        cli.loginfo(count + " records exported to " + filename);
        
        //------ check whether we want to send this export via email...

        if (options.get(EXPORT_OPTION_EMAIL) != null) {
        	String addr=options.get(EXPORT_OPTION_EMAIL);

        	if (Email.sendMessage(addr, emailSubj, "",
                    new String[]{ filename }, true)) {
        		cli.loginfo(LogString.emailSuccessfullySend(emailSubj));
            } else {
                cli.logerr(LogString.emailSendFailed(emailSubj, "Error"));
            }

        }        
    }

	private String extendAndCorrectFilePath(String filename) {

		if (filename!=null) {
			String newFilename=EfaUtil.extendFilenameWithRelativePath(filename);
			
	        if (!newFilename.equals(filename)) {
	            cli.loginfo("Filename '"+filename+ "' extended to '"+newFilename+"'");
	            filename=newFilename;
	        }
	
	        newFilename=EfaUtil.correctFilePath(filename);
	        if (!newFilename.equals(filename)) {
	            cli.loginfo("Filename '"+filename+ "' corrected to '"+newFilename+"'");
	            filename=newFilename;
	        }
		}
		return filename;
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
        char csep = '\0';
        char cquo = '\0';
        
        //handle default setting for csvseparator. 
        if (csvSeparator != null) {
        	//-csvsep has been specified by user (as it is not null), but may be empty. 
        	//if empty, set to '\0' so it won't affect the later import; otherwise use first char.
        	csep = (csvSeparator.length() > 0 ? csvSeparator.charAt(0) : '\0'); 
        } else {
        	//-csvsep has not been specified, so default to ;
        	csep = ';';
        }
        
        if (csvQuotes != null) {
        	cquo = (csvQuotes.length() > 0 ? csvQuotes.charAt(0) : '\0'); 
        } else {
        	cquo='"'; //default to " if empty
        }
        
        // we don't have to check for xml or csv format, efa does it on it's own.
        // also, the import routine automatically removes a BOM which may prefix the csv's first line.
        
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

        filename = extendAndCorrectFilePath(filename);
        
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

    protected String removeOptionsFromArgs(String args) {
    	if (args == null || args.isEmpty()) {
    		return null;
    	}
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
