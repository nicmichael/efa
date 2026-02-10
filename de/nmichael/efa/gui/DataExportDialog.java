/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Locale;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeDateTime;
import de.nmichael.efa.core.items.ItemTypeFile;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.storage.DataExport;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Mnemonics;

public class DataExportDialog extends BaseDialog {

	private static final long serialVersionUID = 7716554593280135453L;
	
	private final String SPACER = "    ";
	private ItemTypeDateTime validAtDateTime;
    private JRadioButton exportSelectAll;
    private JRadioButton exportSelectSelected;
    private JRadioButton exportSelectFiltered;
    private JScrollPane selectedFieldsScrollPane;
    private JList <String>selectedFields;
    private ButtonGroup fileTypeGroup;
    private JRadioButton fileTypeXml;
    private JRadioButton fileTypeCsv;
    private JRadioButton fileTypeCsvUtf8;
    private ItemTypeStringList encoding;
    private ItemTypeFile file;
    private ItemTypeString fileTypeCsvSeparator;
    private ItemTypeString fileTypeCsvQuotes;
    private ItemTypeStringList fileTypeCsvLocale;

    private StorageObject persistence;
    private AdminRecord admin;
    private long validAt;
    private String[] fields;
    private String[] fieldDescription;
    private int[] selectedIndices;
    private Vector<DataRecord> selectedData;
    private Vector<DataRecord> filteredData;

    // @todo (P4) DataExport option: "export to efa", which leaves all keys as they are (only xml, add flag "<exportMode>efa</exportMode>"

    public DataExportDialog(Frame parent, StorageObject persistence, long validAt, AdminRecord admin,
            Vector<DataRecord> selectedData, Vector<DataRecord> filteredData) {
        super(parent, International.getMessage("{data} exportieren", persistence.getDescription()),
                International.getStringWithMnemonic("Export starten"));
        this.admin = admin;
        setPersistence(persistence, validAt);
        this.selectedData = selectedData;
        this.filteredData = filteredData;
    }

    public DataExportDialog(JDialog parent, StorageObject persistence, long validAt, AdminRecord admin,
            Vector<DataRecord> selectedData, Vector<DataRecord> filteredData) {
        super(parent, International.getMessage("{data} exportieren", persistence.getDescription()),
                International.getStringWithMnemonic("Export starten"));
        this.admin = admin;
        setPersistence(persistence, validAt);
        this.selectedData = selectedData;
        this.filteredData = filteredData;
    }

    public void setPersistence(StorageObject persistence, long validAt) {
        this.persistence = persistence;
        this.validAt = validAt;
        this.fields = persistence.createNewRecord().getFieldNamesForTextExport(false);
        this.fieldDescription = new String[fields.length];
        Vector<Integer> indices = new Vector<Integer>();
        Vector<IItemType> items = persistence.createNewRecord().getGuiItems(admin);
        for (int i=0; i<fields.length; i++) {
            IItemType item = null;
            for (int j=0; items != null && j<items.size(); j++) {
                if (items.get(j).getName().equals(fields[i])) {
                    item = items.get(j);
                    break;
                }
            }
            fieldDescription[i] = fields[i] + (item != null ? " (" + item.getDescription() + ")" : "");
            if (!fields[i].equals(DataRecord.LASTMODIFIED) &&
                !fields[i].equals(DataRecord.CHANGECOUNT) &&
                !fields[i].equals(DataRecord.VALIDFROM) &&
                !fields[i].equals(DataRecord.INVALIDFROM) &&
                !fields[i].equals(DataRecord.INVISIBLE) &&
                !fields[i].equals(DataRecord.DELETED) &&
                !fields[i].equals("Id")) {
                indices.add(i);
            }
        }
        selectedIndices = new int[indices.size()];
        for (int i=0; i<selectedIndices.length; i++) {
            selectedIndices[i] = indices.get(i);
        }
    }

    protected void iniDialog() throws Exception {
        mainPanel.setLayout(new BorderLayout());

        JPanel mainControlPanel = new JPanel();
        mainControlPanel.setLayout(new GridBagLayout());
        if (persistence != null && persistence.data().getMetaData().isVersionized()) {
            validAtDateTime = new ItemTypeDateTime("VALID_AT",
                    (validAt < 0 ? DataTypeDate.today() : new DataTypeDate(validAt)),
                    (validAt < 0 ? DataTypeTime.now() : new DataTypeTime(validAt)),
                    IItemType.TYPE_PUBLIC, "", International.getString("exportiere Datensätze gültig am"));
            validAtDateTime.setNotNull(false); // was: true (but users want to export things like boat history including historic boats)
            validAtDateTime.setPadding(0, 0, 10, 0);
            validAtDateTime.displayOnGui(this, mainControlPanel, 0, 0);
        }

        JPanel exportSelectPanel = new JPanel();
        exportSelectPanel.setLayout(new GridBagLayout());
        JLabel exportSelectLabel = new JLabel();
        exportSelectLabel.setText(International.getString("Datensätze") + ":");
        ButtonGroup exportSelectGroup = new ButtonGroup();
        exportSelectAll = new JRadioButton(International.getString("alle"));
        exportSelectSelected = new JRadioButton(International.getString("nur Auswahl"));
        exportSelectFiltered = new JRadioButton(International.getString("nur Filter"));
        exportSelectGroup.add(exportSelectAll);
        exportSelectGroup.add(exportSelectSelected);
        exportSelectGroup.add(exportSelectFiltered);
        exportSelectAll.setSelected(true);
        if (selectedData == null || selectedData.size() == 0) {
            exportSelectSelected.setEnabled(false);
        }
        if (filteredData == null || filteredData.size() == 0) {
            exportSelectFiltered.setEnabled(false);
        }
        
        exportSelectPanel.add(exportSelectLabel, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        exportSelectPanel.add(exportSelectAll, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 10, 10), 0, 0));
        exportSelectPanel.add(exportSelectSelected, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 10, 10), 0, 0));
        exportSelectPanel.add(exportSelectFiltered, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 10, 10), 0, 0));
        mainControlPanel.add(exportSelectPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));

        JLabel fieldsLabel = new JLabel();
        Mnemonics.setLabel(this, fieldsLabel, International.getString("ausgewählte Felder") + ":");
        mainControlPanel.add(fieldsLabel, new GridBagConstraints(0, 2, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 0, 0, 0), 0, 0));
        mainPanel.add(mainControlPanel, BorderLayout.NORTH);

        selectedFields = new JList<String>();
        selectedFields.setListData(fieldDescription);
        selectedFields.setSelectedIndices(selectedIndices);
        selectedFieldsScrollPane = new JScrollPane();
        selectedFieldsScrollPane.setPreferredSize(new Dimension(300,200));
        selectedFieldsScrollPane.getViewport().add(selectedFields);
        mainPanel.add(selectedFieldsScrollPane, BorderLayout.CENTER);

        
		IItemType hint = EfaGuiUtils.createHint("DataExportCSVHint", IItemType.TYPE_PUBLIC,
				null,
				International.getString(
						"CSV-Export für Tabellenkalkulationen: Bei UTF-8 Zeichensatz 'CSV-Datei (mit BOM)' verwenden."),
				3, 20, 3);        
		
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new GridBagLayout());
        hint.displayOnGui(this, filePanel, 1,0);        
        
        JLabel fileTypeLabel = new JLabel();
        Mnemonics.setLabel(this, fileTypeLabel, International.getString("Export als")+": ");
        fileTypeXml = new JRadioButton();
        Mnemonics.setButton(this, fileTypeXml, International.getStringWithMnemonic("XML-Datei"));
        fileTypeXml.setSelected(true);
        fileTypeCsv = new JRadioButton();
        Mnemonics.setButton(this, fileTypeCsv, International.getStringWithMnemonic("CSV-Datei")+SPACER);
        fileTypeCsvUtf8 = new JRadioButton();
        Mnemonics.setButton(this, fileTypeCsvUtf8, International.getStringWithMnemonic("CSV-Datei (mit BOM)")+SPACER);
        fileTypeGroup = new ButtonGroup();
        fileTypeGroup.add(fileTypeCsvUtf8);;
        fileTypeGroup.add(fileTypeXml);
        fileTypeGroup.add(fileTypeCsv);
        
        fileTypeXml.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileTypeChanged();
            }
        });
        fileTypeCsv.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileTypeChanged();
            }
        });
        fileTypeCsvUtf8.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fileTypeChanged();
            }
        });

        fileTypeCsvSeparator=new ItemTypeString(StatisticsRecord.OUTPUTCSVSEPARATOR, ";",
                IItemType.TYPE_PUBLIC, "",
                International.getString("Feldtrenner") + " (CSV)");
        fileTypeCsvSeparator.setEnabled(false);//Not enabled by default, as XML export is standard
        fileTypeCsvSeparator.setMinCharacters(1);
        fileTypeCsvSeparator.setFieldSize(70,21);
        fileTypeCsvSeparator.setFieldGrid(1, GridBagConstraints.WEST, GridBagConstraints.NONE);
        
        fileTypeCsvQuotes = new ItemTypeString(StatisticsRecord.OUTPUTCSVQUOTES, "\"",
                IItemType.TYPE_PUBLIC, "",
                "   "+International.getString("Texttrenner") + " (CSV)");
        fileTypeCsvQuotes.setEnabled(false);//Not enabled by default, as XML export is standard
        fileTypeCsvQuotes.setMinCharacters(1);
        fileTypeCsvQuotes.setFieldSize(70, 21);
        fileTypeCsvQuotes.setFieldGrid(4, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        
        String defaultLanguage=Locale.getDefault().getCountry();
        String myLocaleList[]=Locale.getISOCountries();
        fileTypeCsvLocale = new ItemTypeStringList("CSVLOCALE", defaultLanguage,
        		myLocaleList, 
        		myLocaleList,
        		IItemType.TYPE_PUBLIC, "", 
        		International.getStringWithMnemonic("Regionales Format")
        		);
        fileTypeCsvLocale.setEnabled(false);//Not enabled by default, as XML export is standard        
        fileTypeCsvLocale.setFieldGrid(6, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        
        encoding = new ItemTypeStringList("ENCODING", Daten.ENCODING_UTF,
                new String[] { Daten.ENCODING_UTF, Daten.ENCODING_ISO },
                new String[] { Daten.ENCODING_UTF, Daten.ENCODING_ISO },
                IItemType.TYPE_PUBLIC, "",
                International.getStringWithMnemonic("Zeichensatz")
                );
        encoding.setFieldGrid(6, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
    
        filePanel.add(fileTypeLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));

        filePanel.add(fileTypeCsv, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        
        filePanel.add(fileTypeCsvUtf8, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        
        filePanel.add(fileTypeXml, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        
        

        
        String dir = Daten.efaConfig.getLastExportDirectory();
        if (dir == null || dir.length() == 0 || !(new File(dir)).isDirectory()) {
            dir = Daten.userHomeDir;
        }
        
        file = new ItemTypeFile("FILE",
                dir + (Daten.fileSep != null && !dir.endsWith(Daten.fileSep) ? Daten.fileSep : "") +
                persistence.data().getStorageObjectName() + ".xml",
                    International.getString("Datei"),
                    International.getString("Datei") + " (*.*)",
                    null, ItemTypeFile.MODE_SAVE, ItemTypeFile.TYPE_FILE,
                    IItemType.TYPE_PUBLIC, "",
                    International.getString("Export in Datei"));
        file.setNotNull(true);
        file.setPadding(0, 0, 0, 10);
        file.setFieldGrid(3, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        
        
        fileTypeCsvSeparator.displayOnGui(this, filePanel,  0,5);
        fileTypeCsvQuotes.displayOnGui(this, filePanel,  2,5);
        fileTypeCsvLocale.displayOnGui(this, filePanel,  6);   
        encoding.displayOnGui(this, filePanel, 7);        
        
        file.displayOnGui(this, filePanel, 8);
        mainPanel.add(filePanel, BorderLayout.SOUTH);

        closeButton.setIcon(getIcon(BaseDialog.IMAGE_RUN));
        closeButton.setIconTextGap(10);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void fileTypeChanged() {
        boolean xml = fileTypeXml.isSelected();
        String fname = file.getValueFromField();
        if (fname != null) {
            int pos = fname.lastIndexOf(".");
            if (pos > 0) {
                String ext = fname.substring(pos + 1);
                fname = fname.substring(0, pos);
                if (xml && !ext.equalsIgnoreCase("xml")) {
                    file.parseAndShowValue(fname + ".xml");
                }
                if (!xml && !ext.equalsIgnoreCase("csv")) {
                    file.parseAndShowValue(fname + ".csv");
                }
            }
        }
        
        fileTypeCsvSeparator.setEnabled(!xml);
        fileTypeCsvQuotes.setEnabled(!xml);
        fileTypeCsvLocale.setEnabled(!xml);        

    }

    public void closeButton_actionPerformed(ActionEvent e) {
        long validAt = -1;
        if (validAtDateTime != null) {
            validAtDateTime.getValueFromGui();
            if (validAtDateTime.getValueFromField().length() > 0) {
                validAt = validAtDateTime.getTimeStamp();
            }
        }

        int[] indices = selectedFields.getSelectedIndices();
        String[] fieldNames = new String[indices.length];
        for (int i=0; i<indices.length; i++) {
            fieldNames[i] = fields[indices[i]];
        }

        DataExport.Format format = DataExport.Format.xml; // defaults
        if (fileTypeXml.isSelected()){
        	format=DataExport.Format.xml;
        } else if (fileTypeCsv.isSelected()) {
       		format=DataExport.Format.csv;
        } else if (fileTypeCsvUtf8.isSelected()) {
        	format=DataExport.Format.csv_bom_utf8;
        }
        
        encoding.getValueFromField();
        file.getValueFromField();
        String fname = file.getValue();

        if (fieldNames.length == 0) {
            Dialog.error(International.getString("Keine Felder selektiert."));
            return;
        }
        if (!file.isValidInput()) {
            Dialog.error(file.getInvalidErrorText());
            return;
        }
        
        if (format == DataExport.Format.csv || format == DataExport.Format.csv_bom_utf8) {
        	if (!fileTypeCsvQuotes.isValidInput()) {
        		Dialog.error(fileTypeCsvQuotes.getInvalidErrorText());
        		fileTypeCsvQuotes.requestFocus();
        		return;
        	}
           	if (!fileTypeCsvSeparator.isValidInput()) {
        		Dialog.error(fileTypeCsvQuotes.getInvalidErrorText());
        		fileTypeCsvSeparator.requestFocus();
        		return;
           	}
        }       
        
        Daten.efaConfig.setLastExportDirectory(EfaUtil.getPathOfFile(fname));

        if ((new File(fname).exists())) {
            if (Dialog.yesNoDialog(International.getString("Warnung"),
                    LogString.fileAlreadyExists(fname, International.getString("Datei")) + " " +
                    International.getString("Überschreiben?")) != Dialog.YES) {
                return;
            }
        }

        Vector<DataRecord> selection = null;
        if (exportSelectSelected.isSelected() && selectedData != null && selectedData.size() > 0) {
            selection = selectedData;
        }
        if (exportSelectFiltered.isSelected() && filteredData != null && filteredData.size() > 0) {
            selection = filteredData;
        }
        DataExport export = new DataExport(persistence, validAt, selection,
                fieldNames, format, encoding.getValue(), fname, DataExport.EXPORT_TYPE_TEXT, 
                fileTypeCsvSeparator.getValue(), fileTypeCsvQuotes.getValue(), Locale.forLanguageTag(fileTypeCsvLocale.getValue()));
        int cnt = export.runExport();
        if (cnt >= 0) {
            Dialog.infoDialog(International.getMessage("{count} Datensätze erfolgreich exportiert.", cnt));
        } else {
            Dialog.error(export.getLastError());
        }
        
    }

}
