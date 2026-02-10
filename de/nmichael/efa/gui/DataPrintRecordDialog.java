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

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class DataPrintRecordDialog extends BaseDialog {

	private JScrollPane selectedFieldsScrollPane;
    private JList selectedFields;
    private JCheckBox printEmptyFields;

    private DataRecord record;
    private String[] fields;
    private String[] fieldDescription;
    private int[] selectedIndices;

    public DataPrintRecordDialog(Frame parent, AdminRecord admin, DataRecord record) {
        super(parent, International.getString("Datensatz ausgeben"),
                International.getStringWithMnemonic("Datensatz ausgeben"));
        setPersistence(admin, record);
    }

    public DataPrintRecordDialog(JDialog parent, AdminRecord admin, DataRecord record) {
        super(parent, International.getString("Datensatz ausgeben"),
                International.getStringWithMnemonic("Datensatz ausgeben"));
        setPersistence(admin, record);
    }

    public void setPersistence(AdminRecord admin, DataRecord record) {
        this.record = record;
        StorageObject persistence = record.getPersistence();
        this.fields = persistence.data().getFieldNames(false);
        this.fieldDescription = new String[fields.length];
        Vector<Integer> indices = new Vector<Integer>();
        Vector<IItemType> items = record.getGuiItems(admin);
        for (int i=0; i<fields.length; i++) {
            IItemType item = null;
            for (int j=0; items != null && j<items.size(); j++) {
                if (items.get(j).getName().equals(fields[i])) {
                    item = items.get(j);
                    break;
                }
            }
            fieldDescription[i] = (item != null ? item.getDescription() : fields[i]);
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
        JLabel fieldsLabel = new JLabel();
        Mnemonics.setLabel(this, fieldsLabel, International.getString("ausgewählte Felder") + ":");
        mainControlPanel.add(fieldsLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 0, 0, 0), 0, 0));
        mainPanel.add(mainControlPanel, BorderLayout.NORTH);

        selectedFields = new JList();
        selectedFields.setListData(fieldDescription);
        selectedFields.setSelectedIndices(selectedIndices);
        selectedFieldsScrollPane = new JScrollPane();
        selectedFieldsScrollPane.setPreferredSize(new Dimension(300,200));
        selectedFieldsScrollPane.getViewport().add(selectedFields);
        mainPanel.add(selectedFieldsScrollPane, BorderLayout.CENTER);

        printEmptyFields = new JCheckBox();
        Mnemonics.setButton(this, printEmptyFields,
                International.getStringWithMnemonic("auch leere Felder ausgeben"));
        mainPanel.add(printEmptyFields, BorderLayout.SOUTH);

        closeButton.setIcon(BaseDialog.getIcon(IMAGE_LIST));
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        int[] indices = selectedFields.getSelectedIndices();
        String[] selectedFieldNames = new String[indices.length];
        String[] selectedFieldDescriptions = new String[indices.length];
        for (int i=0; i<indices.length; i++) {
            selectedFieldNames[i] = fields[indices[i]];
            selectedFieldDescriptions[i] = fieldDescription[indices[i]];
        }

        if (selectedFieldNames.length == 0) {
            Dialog.error(International.getString("Keine Felder selektiert."));
            return;
        }

        String filename = Daten.efaTmpDirectory
                + "datarecord.html";
        if (printItems(filename, selectedFieldNames, selectedFieldDescriptions,
                printEmptyFields.isSelected())) {
            BrowserDialog.openInternalBrowser(this, "file:" + filename);
            EfaUtil.deleteFile(filename);
        }

    }

    private boolean printItems(String filename, String[] fields, String[] descriptions,
            boolean printEmptyValues) {
        try {
            BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,false), Daten.ENCODING_UTF));
            HtmlFactory.writeHeader(fw, record.getQualifiedName(), true);

            fw.write("<table border align=\"center\">");
            for (int i = 0; i < fields.length; i++) {
                String descr = descriptions[i];
                String value = record.getAsText(fields[i]);
                if ((value == null || value.length() == 0) && !printEmptyValues) {
                    continue;
                }
                fw.write("<tr>");
                fw.write("<td>" + descr + "</td>");
                fw.write("<td>"
                        + (value != null && value.length() > 0 ? EfaUtil.escapeXml(value) : "&nbsp;")
                        + "</td>");
                fw.write("</tr>");
            }
            fw.write("</table>");
            HtmlFactory.writeFooter(fw);
            fw.close();
        } catch(Exception e) {
            Logger.logdebug(e);
            Dialog.error(e.toString());
            return false;
        }
        return true;
    }

}
