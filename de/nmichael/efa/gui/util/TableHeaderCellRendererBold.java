package de.nmichael.efa.gui.util;


import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


public class TableHeaderCellRendererBold  extends DefaultTableCellRenderer{

	private javax.swing.table.TableCellRenderer original;

	public TableHeaderCellRendererBold(javax.swing.table.TableCellRenderer l_originalRenderer) {
		this.original = l_originalRenderer;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		Component comp = original.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		comp.setFont(comp.getFont().deriveFont(Font.BOLD));
		comp.setBackground(this.getBackground());
		comp.setForeground(this.getForeground());
		return comp;
	}

}