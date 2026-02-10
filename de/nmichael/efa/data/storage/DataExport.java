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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDecimal;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;

public class DataExport {

    public static final String FIELD_EXPORT = "export";
    public static final String EXPORT_TYPE = "type";
    public static final String EXPORT_TYPE_TEXT = "text";
    public static final String EXPORT_TYPE_ID   = "id";

    public enum Format {
        xml,
        csv,
        csv_bom_utf8
    }

    private StorageObject storageObject;
    private boolean exportAllLatest = false;
    private  long validAt;
    private Vector<DataRecord> selection;
    private String[] fields;
    private Format format;
    private String encoding;
    private String filename;
    private String exportType;
    private boolean versionized;
    private String lastError;
    
    private String csvDelimiter;
    private String csvQuote;
    private Locale csvLocale;

	private DecimalFormat csvDecimalFormat=null;
	private DateFormat csvDateFormat=null;

    public DataExport(StorageObject storageObject, long validAt, Vector<DataRecord> selection,
            String[] fields, Format format, String encoding, String filename, String exportType, 
            String csvDelim, String csvQuote, Locale csvLocale ) {
        this.storageObject = storageObject;
        this.validAt = validAt;
        this.selection = selection;
        this.fields = fields;
        this.format = format;
        this.encoding = encoding;
        this.filename = filename;
        this.exportType = exportType;
        this.versionized = storageObject.data().getMetaData().isVersionized();
        this.exportAllLatest = versionized && selection == null && validAt < 0;
        
        this.csvDelimiter = csvDelim;
        this.csvQuote = csvQuote;
        this.csvLocale= csvLocale;

    	if (this.csvLocale!=null) {  //if a locale is defined, get the formatters for float and date
    		this.csvDecimalFormat= new DecimalFormat("#.##",DecimalFormatSymbols.getInstance(csvLocale));
    		this.csvDateFormat = getFourDigitShortLocaleDateFormat();
    	}  
    }

    private DateFormat getFourDigitShortLocaleDateFormat() {
    	DateFormat result = DateFormat.getDateInstance(DateFormat.SHORT,csvLocale);

    	// now force csvDateFormat to have a four-digit year, everything else messes up data import
		// as data import interprets two digit years (01.01.80) as something within 21st century (01.01.2080)

    	try {
	    	SimpleDateFormat resultSDF = (SimpleDateFormat) result;
	    	String adaptedLocalizedDatePattern = resultSDF.toPattern().replaceAll("y+", "yyyy");
	    	resultSDF.applyPattern(adaptedLocalizedDatePattern);
	    	return resultSDF;
    	} catch (Exception e) {
    		// if an error occurs with SimpleDateFormat conversion, return standard localized Date format
    		return result;
    	}
    }
    
    public int runExport() {
        int count = 0;
        try {
            BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,false), encoding));
            if (format == Format.xml) {
                fw.write("<?xml version=\"1.0\" encoding=\""+encoding+"\"?>\n");
                fw.write("<" + FIELD_EXPORT + " " + EXPORT_TYPE + "=\"" + exportType + "\">\n");
            }
            
            if (format == Format.csv_bom_utf8) {
            	//BOM is needed for CSV files with UTF-8 to be imported with excel
            	fw.write('\ufeff');
            }
            
            // write header for CSV files
            if (format == Format.csv || format == Format.csv_bom_utf8 ) {
                for (int i=0; i<fields.length; i++) {
                    fw.write( (i > 0 ? csvDelimiter : "") + fields[i]);
                }
                fw.write("\n");
            }

            if (exportAllLatest) {
                // complete export of all records that ever existed, in their latest version
                Hashtable<DataKey, DataKey> duplicates = new Hashtable<DataKey, DataKey>();
                DataKeyIterator it = storageObject.data().getStaticIterator();
                DataKey k = it.getFirst();
                while (k != null) {
                    k = storageObject.data().getUnversionizedKey(k);
                    if (duplicates.get(k) == null) {
                        duplicates.put(k, k);
                        DataRecord r = storageObject.data().getValidLatest(k);
                        if (writeRecord(fw, r)) {
                            count++;
                        }
                    }
                    k = it.getNext();
                }
            } else if (selection == null) {
                // export of all records that are valid at a specified time
                DataKeyIterator it = storageObject.data().getStaticIterator();
                DataKey k = it.getFirst();
                while (k != null) {
                    DataRecord r = storageObject.data().get(k);
                    if (writeRecord(fw, r)) {
                        count++;
                    }
                    k = it.getNext();
                }
            } else {
                // export of all selected records
                for (DataRecord r : selection) {
                    if (writeRecord(fw, r)) {
                        count++;
                    }
                }
            }
            if (format == Format.xml) {
                fw.write("</" + FIELD_EXPORT + ">\n");
            }
            fw.close();
        } catch(Exception e) {
            Logger.logdebug(e);
            lastError = e.getMessage();
            return -1;
        }
        return count;
    }

    boolean writeRecord(BufferedWriter fw, DataRecord r) {
        try {
            if (r != null && 
                (exportAllLatest || (!r.getDeleted() && (!versionized || r.isValidAt(validAt)))) ) {
                if (format == Format.xml) {
                    fw.write("<" + DataRecord.ENCODING_RECORD + ">");
                }
                
                for (int i = 0; i < fields.length; i++) {
                    String value = r.getAsText(fields[i]);
                    
                    if (format == Format.xml) {
                        if (value != null && value.length() > 0) {
                            fw.write("<" + fields[i] + ">" + EfaUtil.escapeXml(value) + "</" + fields[i] + ">");
                        }
                    }
                    
                    if (format == Format.csv || format == Format.csv_bom_utf8) {

                    	int iFieldType=getAbstractFieldType(r, fields[i]);

                    	Boolean isTextField= (iFieldType==IDataAccess.DATA_TEXT);
                        Boolean isFloatField = (iFieldType==IDataAccess.DATA_DOUBLE);
                        Boolean isDateField = (iFieldType==IDataAccess.DATA_DATE);    
                        Boolean isDistanceField = (iFieldType==IDataAccess.DATA_DISTANCE);

                    	if (isTextField) {
                    		//if (and only if) we export a string type field, include quotes, als long as the value is not empty 
                            fw.write((i > 0 ? csvDelimiter : "") + 
                            		(value != null ? csvQuote : "") + 
                            		(value != null ? EfaUtil.replace(value, csvDelimiter, "", true) : "") +
                            		(value != null ? csvQuote : ""));

                    	} else if (isFloatField && csvLocale!=null) { 
                    		Object value1=r.get(fields[i]);
                    		value=null;
                    		if (value1!= null) {
                    			if (value1.getClass() == DataTypeDecimal.class) {
                            		value = csvDecimalFormat.format(((DataTypeDecimal) value1).getValue());
                    			} else {
                    				value=csvDecimalFormat.format(value1);
                    			}
                    		}
                            fw.write((i > 0 ? csvDelimiter : "") + (value != null ? EfaUtil.replace(value, csvDelimiter, "", true) : ""));
                    	} else if (isDistanceField && csvLocale!=null) {
                    		Object value1=r.get(fields[i]);
                    		value=null;
                    		if (value1!= null) {
                    			if (value1.getClass() == DataTypeDistance.class) {
                    				// format the decimal value according to the csv locale, and add the unit afterwards.
                            		value = csvDecimalFormat.format(((DataTypeDistance) value1).getValue().getValue()); //double getValue() as we want the decimal Value
                            		value = value + " " + ((DataTypeDistance) value1).getUnit();
                    			} else {
                    				value=value1.toString();
                    			}
                    		}
                            fw.write((i > 0 ? csvDelimiter : "") + (value != null ? EfaUtil.replace(value, csvDelimiter, "", true) : ""));                    		
                    	} else if (isDateField && csvLocale!=null) {
                    		DataTypeDate value2= (DataTypeDate) r.get(fields[i]);
                    		value = (value2!=null ? csvDateFormat.format(value2.getDate()) : null);
                            fw.write((i > 0 ? csvDelimiter : "") + (value != null ? EfaUtil.replace(value, csvDelimiter, "", true) : ""));

                    	} else {
                            fw.write((i > 0 ? csvDelimiter : "") + (value != null ? EfaUtil.replace(value, csvDelimiter, "", true) : ""));                    		
                    	}
                    }
                }
                    
                if (format == Format.xml) {
                    fw.write("</" + DataRecord.ENCODING_RECORD + ">\n");
                }
                if (format == Format.csv || format==Format.csv_bom_utf8) {
                    fw.write("\n");
                }
                return true; // exported
            }
            return false; // not exported
        } catch (IOException e) {
            Logger.logdebug(e);
            return false;
        }
    }

    public String getLastError() {
        return lastError;
    }

    /** 
     * Convert actual field type to a more common one which may need special handling in CSV files. 
     * return IDataAccess.DATA_TEXT for all field types which need to be encapsulated with " in CSV
     *        IDataAccess.DATA_DOUBLE for decimal fields (which need to be rendered correctly by using locales
     *        IDataAccess.DATE for DATE fields 
     *        IDataAccess. actual field Type for all other fieldtypes (which need no special handling in CSV files). 
     */
        private int getAbstractFieldType (DataRecord r, String fieldName) {
        	try {
        		int fieldType;
        		// fieldname may point to a virtual field, like "crew1" in a logbook.
        		// you cannot determine a fieldType for such a virtual field.
        		// this would lead to (intended) nullpointer exceptions in debug log when determining a field type for such virtual fields.
        		// this is disturbing and can be avoided.
        		if (r.metaData.isField(fieldName)) {
        			fieldType=r.getFieldType(fieldName);
        		} else {
        			//it's a virtual field, so data type is text.
        			fieldType=IDataAccess.DATA_TEXT;
        		}

        		// correct fieldtype to a more abstract one
    	    	if (fieldType == IDataAccess.DATA_STRING 
    	    			|| fieldType == IDataAccess.DATA_TEXT 
    	    			|| fieldType == IDataAccess.DATA_UUID
       					|| fieldType == IDataAccess.DATA_INTSTRING
    					|| fieldType == IDataAccess.DATA_PASSWORDH
    					|| fieldType == IDataAccess.DATA_PASSWORDC
    					|| fieldType == IDataAccess.DATA_LIST_STRING
    					|| fieldType == IDataAccess.DATA_LIST_INTEGER
    					|| fieldType == IDataAccess.DATA_LIST_UUID
    					|| fieldType == IDataAccess.DATA_VIRTUAL
    					|| fieldType == IDataAccess.DATA_UNKNOWN) {
    					return IDataAccess.DATA_TEXT;
    			}

    	    	//decimal and double are the same for csv export.
    	    	//but we won't check for distances as data type as they always come along with a unit, and thus need no special handling in csv files.
    			if (fieldType==IDataAccess.DATA_DECIMAL || fieldType == IDataAccess.DATA_DOUBLE) {
    				return IDataAccess.DATA_DOUBLE;
    			}

    			//nothing to correct? then, return the actual fieldType of the field.
    			return fieldType;

        	} catch (Exception e) {
        		//return text field format, if fieldtype cannot be determinded
        		//for instance for virtual fields like "boat" which replaces actual datafields like "boatid" and "boatname"
        		return IDataAccess.DATA_TEXT;
        	}
        }
}
