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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.UUID;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.core.items.ItemTypeTextArea;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.MessageRecord;
import de.nmichael.efa.data.Messages;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.gui.util.EfaTableCellRenderer;
import de.nmichael.efa.gui.util.TableHeaderCellRendererBold;
import de.nmichael.efa.gui.util.TableSorter;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class ShowLogbookDialog extends BaseDialog implements IItemListener {

	private static final long serialVersionUID = -2271729956595866905L;

	private Logbook logbook;
    private JScrollPane scrollPane;
    private JTable table;
    private TableSorter sorter;
    private Hashtable<Integer,DataTypeIntString> row2entryno;
    private ItemTypeInteger showOnlyNumber;
    private ItemTypeBoolean showAlsoIncomplete;

    public ShowLogbookDialog(Frame parent, Logbook logbook) {
        super(parent, International.getStringWithMnemonic("Fahrtenbuch"), International.getStringWithMnemonic("Schließen"));
        this.logbook = logbook;
    }

    public ShowLogbookDialog(JDialog parent, Logbook logbook) {
        super(parent, International.getStringWithMnemonic("Fahrtenbuch"), International.getStringWithMnemonic("Schließen"));
        this.logbook = logbook;
    }

    protected void iniDialog() throws Exception {
        if (Daten.efaConfig.getValueEfaDirekt_startMaximized()) {
            this.setSize(Dialog.screenSize.width, Dialog.screenSize.height);
            this.setPreferredSize(new Dimension(Dialog.screenSize.width, Dialog.screenSize.height));
        } else {
            this.setSize((Dialog.screenSize.width * 98) / 100, (Dialog.screenSize.height * 95) / 100);
            this.setPreferredSize(new Dimension((Dialog.screenSize.width * 98) / 100, (Dialog.screenSize.height * 95) / 100));
        }

        mainPanel.setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());

        showOnlyNumber = new ItemTypeInteger("SHOWONLYNUMBER",
                (Daten.efaConfig.getValueEfaDirekt_anzFBAnzeigenFahrten() > 0 ? Daten.efaConfig.getValueEfaDirekt_anzFBAnzeigenFahrten() : 50), 1,
                (Daten.efaConfig.getValueEfaDirekt_maxFBAnzeigenFahrten() > 0 ? Daten.efaConfig.getValueEfaDirekt_maxFBAnzeigenFahrten() : 100),
                IItemType.TYPE_PUBLIC, "", International.getString("Anzahl der anzuzeigenden Fahrten"));
        showOnlyNumber.registerItemListener(this);

        showAlsoIncomplete = new ItemTypeBoolean("SHOWALSOINCOMPLETE", Daten.efaConfig.getValueEfaDirekt_FBAnzeigenAuchUnvollstaendige(),
                IItemType.TYPE_PUBLIC, "", International.getString("auch Fahrten von Booten anzeigen, die noch unterwegs sind"));
        showAlsoIncomplete.registerItemListener(this);

        showOnlyNumber.displayOnGui(this, controlPanel, 0, 0);
        showAlsoIncomplete.displayOnGui(this, controlPanel, 0, 1);

        scrollPane = new JScrollPane();

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        updateTable(showOnlyNumber.getValue(), showAlsoIncomplete.getValue());
        table.requestFocus();
    }

    private void updateNestedTableHeight() {
        for (int i = 0; table != null && i < table.getRowCount(); i++) {
            int orgHeight = table.getRowHeight(i);
            int newHeight = 0;
            try {
                newHeight = (int) ((JTable) table.getValueAt(i, 4)).getPreferredSize().getHeight();
            } catch (Exception e) {
                EfaUtil.foo();
            }
            if (newHeight > orgHeight) {
                table.setRowHeight(i, newHeight);
            }
        }
    }

    public void updateTable(int max, boolean alsoIncomplete) {
        if (max < 1) {
            max = 1;
        }
        if (table != null) {
            scrollPane.remove(table);
        }

        Object[] title = new Object[11];
        title[0] = International.getString("LfdNr");
        title[1] = International.getString("Datum");
        title[2] = International.getString("Boot");
        title[3] = International.getString("Steuermann");
        title[4] = International.getString("Mannschaft");
        title[5] = International.getString("Abfahrt");
        title[6] = International.getString("Ankunft");
        title[7] = International.getString("Ziel");
        title[8] = International.getString("Km");
        title[9] = International.getString("Bemerkungen");
        title[10] = "C";

        // count entries to show
        int count = 0;
        try {
            DataKeyIterator it = logbook.data().getStaticIterator();
            DataKey k = it.getLast();
            while (k != null) {
                LogbookRecord r = (LogbookRecord)logbook.data().get(k);
                if (r.getSessionIsOpen() && !alsoIncomplete) {
                    k = it.getPrev();
                    continue;
                }
                if (++count >= max) {
                    break;
                } 
                k = it.getPrev();
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        
        int c = Math.min(count, max);
        Object[][] fahrten = new Object[c][11];
        row2entryno = new Hashtable<Integer,DataTypeIntString>();
        Font f = this.getFont();
        FontMetrics fm = this.getFontMetrics(f);     
        try {
   	
            DataKeyIterator it = logbook.data().getStaticIterator();
            DataKey k = it.getLast();
            while (k != null) {
                LogbookRecord r = (LogbookRecord)logbook.data().get(k);
                if (r.getSessionIsOpen() && !alsoIncomplete) {
                    k = it.getPrev();
                    continue;
                }

                int obmann = r.getBoatCaptainPosition();
                c--;
                row2entryno.put(c, r.getEntryId());

                fahrten[c][0] = r.getEntryId();
                fahrten[c][1] = r.getDate();
                fahrten[c][2] = r.getBoatAsName();
                fahrten[c][3] = new TableItem(r.getCoxAsName(), obmann == 0); // (obmann == 0 ? BOLD : "") + d.get(Fahrtenbuch.STM);

                int mRowCount = r.getNumberOfCrewMembers() 
                        - (r.getCoxAsName().length() > 0 ? 1 : 0); // substract cox, we want only crew
                if (mRowCount == 0) {
                    mRowCount = 1;
                }
                Object[][] mRowData = new Object[mRowCount][1];
                for (int j = 0, i = 1; i <= LogbookRecord.CREW_MAX; i++) {
                    String s = r.getCrewAsName(i);
                    if (s != null && s.length() > 0) {
                        mRowData[j++][0] = new TableItem(s, obmann == i); // (obmann == ii+1 ? BOLD : "") + d.get(i);
                    }
                }
                Object[] mRowTitle = new Object[1];
                mRowTitle[0] = "foo";
                MyNestedJTable mTable = new MyNestedJTable(mRowData, mRowTitle, Daten.efaConfig.getValueEfaDirekt_tabelleShowTooltip()) {
					private static final long serialVersionUID = 4113309999908631888L;

					public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
                // user alternate row coloring on the inner table for crews 
                HighlightTableCellRenderer myInnerRenderer = new HighlightTableCellRenderer();
                if (Daten.efaConfig.getValueEfaDirekt_tabelleAlternierendeZeilenFarben()) {
                	myInnerRenderer.setAlternatingRowColor(Daten.efaConfig.getTableAlternatingRowColor());
                }
                mTable.getColumn("foo").setCellRenderer(myInnerRenderer);
                mTable.setShowGrid(false);
                mTable.setRowHeight(fm.getHeight()+6);
                fahrten[c][4] = mTable;

                fahrten[c][5] = r.getStartTime();
                fahrten[c][6] = r.getEndTime();
                fahrten[c][7] = r.getDestinationAndVariantName(false, false);
                fahrten[c][8] = r.getDistance();
                fahrten[c][9] = r.getComments();

                // EntryID for this column is just used as a reference.
                // This column will display a button to change this entry
                fahrten[c][10] = r.getEntryId().toString();

                k = it.getPrev();
                if (c == 0) {
                    break;
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }

        if (c > 0) {
            Object[][] fahrtentmp = new Object[max - c][10];
            for (int xorg = c, xnew = 0; xorg < max; xorg++, xnew++) {
                for (int y = 0; y < 10; y++) {
                    fahrtentmp[xnew][y] = fahrten[xorg][y];
                }
            }
            fahrten = fahrtentmp;
        }

        sorter = new TableSorter(new DefaultTableModel(fahrten, title));
        table = new MyJTable(sorter, Daten.efaConfig.getValueEfaDirekt_tabelleShowTooltip());
        table.setTableHeader(new TableHeaderWithTooltips(table.getColumnModel()));
        
        HighlightTableCellRenderer mySteuermannRenderer = new HighlightTableCellRenderer();
        if (Daten.efaConfig.getValueEfaDirekt_tabelleAlternierendeZeilenFarben()) {
        	mySteuermannRenderer.setAlternatingRowColor(Daten.efaConfig.getTableAlternatingRowColor());
        }
        table.getColumn(International.getString("Steuermann")).setCellRenderer(mySteuermannRenderer);
        table.getColumn(International.getString("Mannschaft")).setCellRenderer(new TableInTableRenderer());

        ButtonRenderer buttonRenderer = new ButtonRenderer();
        table.getColumn("C").setCellRenderer(buttonRenderer);
        table.getColumn("C").setCellEditor(new ButtonEditor(buttonRenderer));
        
        // Update for standard tables: increase row height for better readability
        table.setRowHeight(fm.getHeight()+6);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Update for standard tables: Replace default header renderer with bold+dark background renderer 
        TableCellRenderer l_originalRenderer = table.getTableHeader().getDefaultRenderer();


        TableHeaderCellRendererBold r = new TableHeaderCellRendererBold(l_originalRenderer);
        r.setBackground(Daten.efaConfig.getTableHeaderBackgroundColor());
        r.setForeground(Daten.efaConfig.getTableHeaderHeaderColor());
        table.getTableHeader().setDefaultRenderer(r);
        
        EfaTableCellRenderer renderer = new  EfaTableCellRenderer();
        if (Daten.efaConfig.getValueEfaDirekt_tabelleAlternierendeZeilenFarben()) {
            renderer.setAlternatingRowColor(Daten.efaConfig.getTableAlternatingRowColor());
        }
        table.setDefaultRenderer(Object.class, renderer);

        
        updateNestedTableHeight();
        sorter.addMouseListenerToHeaderInTable(table);
        scrollPane.getViewport().add(table, null);
        try {
            table.scrollRectToVisible(table.getCellRect(fahrten.length - 1, 0, true));
        } catch (Exception e) {
        }
        table.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancel();
                }
            }
        });

        // intelligente Spaltenbreiten
        int width = table.getSize().width;
        if (width < this.getSize().width - 20 || width > this.getSize().width) { // beim ersten Aufruf steht Tabellenbreite noch nicht (korrekt) zur Verfügung, daher dieser Plausi-Check
            width = this.getSize().width - 10;
        }

        int[] widths = new int[11];
        int remaining = width;
        for (int i = 0; i < 11; i++) {
            switch (i) {
                case 0:
                    widths[i] = 6 * width / 100; // LfdNr
                    if (widths[i] > 50) {
                        widths[i] = 50;
                    }
                    break;
                case 1:
                    widths[i] = 10 * width / 100; // Datum
                    if (widths[i] > 120) {
                        widths[i] = 120;
                    }
                    break;
                case 5:
                    widths[i] = 6 * width / 100; // Abfahrt
                    if (widths[i] > 65) {
                        widths[i] = 65;
                    }
                    break;
                case 6:
                    widths[i] = 6 * width / 100; // Ankunft
                    if (widths[i] > 65) {
                        widths[i] = 65;
                    }
                    break;
                case 8:
                    widths[i] = 7 * width / 100; // Boots-Km
                    if (widths[i] > 70) {
                        widths[i] = 70;
                    }
                    break;
                case 10:
                    widths[i] = 19; // Change Button
                    break;
            }
            remaining -= widths[i];
        }

        for (int i = 0; i < 11; i++) {
            switch (i) {
                case 2:
                    widths[i] = 18 * remaining / 100;
                    break; // Boot
                case 3:
                    widths[i] = 22 * remaining / 100;
                    break; // Stm
                case 4:
                    widths[i] = 22 * remaining / 100;
                    break; // Mannsch
                case 7:
                    widths[i] = 26 * remaining / 100;
                    break; // Ziel
                case 9:
                    widths[i] = 10 * remaining / 100;
                    break; // Bemerkungen
            }
        }

        for (int i = 0; i < 11; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.validate();

    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (event != null && itemType != null) {
            if ((event instanceof KeyEvent && itemType == showOnlyNumber && event.getID() == KeyEvent.KEY_RELEASED) ||
                (event instanceof ActionEvent && itemType == showAlsoIncomplete)) {
                if (event instanceof KeyEvent && ((KeyEvent)event).getKeyCode() == KeyEvent.VK_SHIFT) {
                    return;
                }
                showOnlyNumber.getValueFromGui();
                showAlsoIncomplete.getValueFromGui();
                updateTable(showOnlyNumber.getValue(), showAlsoIncomplete.getValue());
            }
        }
    }

    void changeRequest(DataTypeIntString entryNo) {
        if (entryNo == null) {
            return;
        }
        try {
            LogbookRecord r = this.logbook.getLogbookRecord(entryNo);


            ItemTypeTextArea text = new ItemTypeTextArea("TEXT",
                    EfaUtil.wrapString(International.getMessage("Korrekturwunsch für Fahrtenbuch-Eintrag {entry}",
                    r.getLogbookRecordAsStringDescription()), 80) + "\n\n",
                    IItemType.TYPE_PUBLIC,
                    "", International.getString("Bitte gib eine Beschreibung der gewünschten Änderung ein"));
            text.setCaretPosition(Integer.MAX_VALUE);
            text.setLabelGrid(2, -1, -1);
            text.setNotNull(true);
            text.setFieldGrid(3, GridBagConstraints.EAST, GridBagConstraints.NONE);
            long now = System.currentTimeMillis();
            ItemTypeStringAutoComplete from = new ItemTypeStringAutoComplete("FROM", "",
                    IItemType.TYPE_PUBLIC, "",
                    International.getString("Dein Name"), true);

            from.setAutoCompleteData(new AutoCompleteList(Daten.project.getPersons(false).data(), now, now));
            from.setAlwaysReturnPlainText(true);
            from.setNotNull(true);
            if (SimpleInputDialog.showInputDialog(this,
                    International.getString("Korrekturwunsch"), new IItemType[] { text, from } )) {
                Messages messages = Daten.project.getMessages(false);
                messages.createAndSaveMessageRecord(from.getValueFromField(), MessageRecord.TO_ADMIN,
                        (UUID)from.getId(from.getValueFromField()),
                        International.getMessage("Korrekturwunsch für Fahrtenbuch-Eintrag {entry}", "#" + entryNo),
                        text.getValueFromField());
                Dialog.infoDialog(International.getString("Vielen Dank, dein Korrekturwunsch wurde übermittelt."));
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
    }

    private class MyJTable extends JTable {

		private static final long serialVersionUID = 7627514043061724774L;

		private Boolean tooltipsEnabled=false;
		
		public MyJTable(TableSorter sorter, Boolean showToolTips) {
            super(sorter);
            tooltipsEnabled=showToolTips;
        }

        public boolean isCellEditable(int row, int column) {
            return column == 10;
        }

        public void valueChanged(ListSelectionEvent e) {
            try {
                if (e != null) {
                    int selected = this.getSelectedRow();
                    if (selected >= 0) {
                        JTable nestedTable = (JTable) this.getValueAt(selected, 4);
                        nestedTable.selectAll();
                    }
                    for (int i = 0; i < this.getRowCount(); i++) {
                        if (i != selected) {
                            JTable nestedTable = (JTable) this.getValueAt(i, 4);
                            nestedTable.clearSelection();
                        }
                    }
                }
            } catch (Exception ee) {
            	Logger.logdebug(ee);
            }
            super.valueChanged(e);
        }

        public void tableChanged(TableModelEvent e) {
            super.tableChanged(e);
            updateNestedTableHeight();
        }
        
   	 	public String getToolTipText(MouseEvent event) {
	        try {
	        	if (tooltipsEnabled) {
	                int row = rowAtPoint(event.getPoint());
	                int col = columnAtPoint(event.getPoint());
					
	                // SGB Update for tables: Tooltipp shall be presented only if the value does not fit into row. 
	                if (col!=-1 && row !=-1) {
						
	                	javax.swing.table.TableCellRenderer l_renderer = getCellRenderer(row, col);
						Component l_component = prepareRenderer (l_renderer, row, col);
						Rectangle l_cellRect=getCellRect(row, col, false);

						if (l_cellRect.width >= l_component.getPreferredSize().width) {
							// do not show any tooltip if the column has enough space for the value
							return null;
						} else {
							return getValueAt(row, col).toString();
						}
					}
	            }            
	        } catch (Exception eignore) {
	        	Logger.logdebug(eignore);
	        }
	        return null;
	    }
        
    }

    /** 
     * MyNestedJTable is a table which is placed into a single cell.
     * It is used for the "Crew" column. 
     * 
     * For it's inner cells, it uses the background color of the row it is currently used.
     * 
     */
    private class MyNestedJTable extends JTable {

		private static final long serialVersionUID = -1632568401117917149L;
		
		private boolean startsWithOddRow=true;
		private boolean tooltipsEnabled=false;
		String toText = "";
        Object[][] data = null;
        Object[] title = null;

        public MyNestedJTable(Object[][] data, Object[] title, Boolean showToolTips) {
            super(data, title);
            this.tooltipsEnabled=showToolTips;
            this.data = data;
            this.title = title;
            this.setShowGrid(false);
            this.setShowHorizontalLines(true);
            toText = "";
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    toText += data[i][j];
                }
            }
        }

        public String toString() {
            return toText;
        }

        public Object clone() {
            return new MyNestedJTable(data, title, tooltipsEnabled);
        }
        
        public void setStartWithOddRow(boolean value) {
        	startsWithOddRow=value;
        }
        
        public boolean getStartWithOddRow() {
        	return startsWithOddRow;
        }

        public String getToolTipText(MouseEvent event) {
	        try {
	        	if (tooltipsEnabled) {
	                int row = rowAtPoint(event.getPoint());
	                int col = columnAtPoint(event.getPoint());
					
	                // SGB Update for tables: Tooltipp shall be presented only if the value does not fit into row. 
	                if (col!=-1 && row !=-1) {
						
	                	javax.swing.table.TableCellRenderer l_renderer = getCellRenderer(row, col);
						Component l_component = prepareRenderer (l_renderer, row, col);
						Rectangle l_cellRect=getCellRect(row, col, false);

						if (l_cellRect.width >= l_component.getPreferredSize().width) {
							// do not show any tooltip if the column has enough space for the value
							return null;
						} else {
							return getValueAt(row, col).toString();
						}
					}
	            }            
	        } catch (Exception eignore) {
	        	Logger.logdebug(eignore);
	        }
	        return null;
	    }        
                
    }

    private class TableItem {

        private String txt;
        private boolean bold;

        public TableItem(String txt, boolean bold) {
            this.txt = txt;
            this.bold = bold;
        }

        public String toString() {
            return txt;
        }

        public boolean isBold() {
            return bold;
        }

    }

    private class TableInTableRenderer implements TableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            try {
                if (value == null) {
                    return null;
                }
                // this is neccessary so that the nested table starts with the same alternating 
                // row color as the current line.
                ((MyNestedJTable) value).setStartWithOddRow(row % 2 == 1);
                return (Component) value;
            } catch (Exception e) {
            	Logger.logdebug(e);
                return null;
            }
        }
    }

    private class HighlightTableCellRenderer extends DefaultTableCellRenderer {
    	
		private static final long serialVersionUID = -1661015873516314571L;
		
	    private Color alternateColor = new Color(219,234,249);
	    private boolean useAlternatingColor= false;
	    
		public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            try {


                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	            Color bkgColor = null;
	            Color fgColor = null;
	            
                if (value !=null && ((TableItem) value).isBold()) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }

                if (this.useAlternatingColor) {
                	if (table instanceof MyNestedJTable) {
                		Boolean startsWithOdd=((MyNestedJTable) table).getStartWithOddRow();
                		if (startsWithOdd) {
                			bkgColor = null; //(row % 2 == 0 ? null : alternateColor);
                		} else {
            	            bkgColor = alternateColor; // (row % 2 == 0 ? alternateColor : null);
                		}
                	} else {
                		bkgColor = (row % 2 == 0 ? alternateColor : null);
                		this.setVerticalTextPosition(SwingConstants.TOP);
                	}
                }      
                
                if (isSelected) {
                    bkgColor = table.getSelectionBackground();
                    // Update for standard tables: when selected, we should always use the selection foreground.
                    fgColor= table.getSelectionForeground();
                    if (c.getFont().isBold()) {
                    	c.setFont(c.getFont().deriveFont(Font.BOLD | Font.ITALIC));
                    } else {
                    	c.setFont(c.getFont().deriveFont(Font.BOLD));
                    }
                }
                c.setBackground(bkgColor);    
	            c.setForeground(fgColor);
                return this;
            } catch (Exception e) {
            	Logger.logdebug(e);
                return null;
            }
        }
		
	    public void setAlternatingRowColor(Color c) {
	    	this.alternateColor = c;
	    	this.useAlternatingColor=(c!=null && Daten.efaConfig.getValueEfaDirekt_tabelleAlternierendeZeilenFarben());
	    }
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer {

		private static final long serialVersionUID = -4274374186561493972L;

		public ButtonRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setIcon(BaseDialog.getIcon(BaseDialog.IMAGE_EDIT2));
            return this;
        }

    }

    private class ButtonEditor extends DefaultCellEditor {

		private static final long serialVersionUID = -2250457297780802588L;

		protected JButton button;
        private DataTypeIntString entryNo;

        public ButtonEditor(JButton button) {
            super(new JCheckBox());
            this.button = button;
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    changeRequest(entryNo);
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            int physRow = sorter.getOriginalIndex(row);

            entryNo = (row2entryno != null ? row2entryno.get(physRow) : null);
            return button;
        }

        public Object getCellEditorValue() {
            return button;
        }

        public boolean stopCellEditing() {
            return super.stopCellEditing();
        }

        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }
    
    private class TableHeaderWithTooltips extends JTableHeader {

		private static final long serialVersionUID = 6995328318883617447L;

		TableHeaderWithTooltips(TableColumnModel columnModel) {
          super(columnModel);//do everything a normal JTableHeader does
        }

		/**
		 * return the caption of the clumn the mouse is hovering.
		 * do not check wether a tooltip is neccessary or not
		 */
        public String getToolTipText(MouseEvent e) {
            java.awt.Point p = e.getPoint();
            int index = columnModel.getColumnIndexAtX(p.x);
            //int realIndex = columnModel.getColumn(index).getModelIndex();
			
            return this.getColumnModel().getColumn(index).getHeaderValue().toString();
        }
    }        

}
