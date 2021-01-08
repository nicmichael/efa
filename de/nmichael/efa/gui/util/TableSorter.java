/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

/**
 * A sorter for TableModels. The sorter has a model (conforming to TableModel)
 * and itself implements TableModel. TableSorter does not store or copy
 * the data in the TableModel, instead it maintains an array of
 * integers which it keeps the same size as the number of rows in its
 * model. When the model changes it notifies the sorter that something
 * has changed eg. "rowsAdded" so that its internal array of integers
 * can be reallocated. As requests are made of the sorter (like
 * getValueAt(row, col) it redirects them to its model via the mapping
 * array. That way the TableSorter appears to hold another copy of the table
 * with the rows in a different order. The sorting algorthm used is stable
 * which means that it does not move around rows when its comparison
 * function returns 0 to denote that they are equivalent.
 *
 * @version 1.5 12/17/97
 * @author Philip Milne
 */
package de.nmichael.efa.gui.util;

import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.TMJ;
import de.nmichael.efa.util.TableMap;
import java.util.Date;
import java.util.Vector;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;

//Imports for picking up mouse events from the JTable.
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

// @i18n complete
public class TableSorter extends TableMap {

    int indexes[];
    Vector sortingColumns = new Vector();
    boolean ascending = true;
    int compares;

    public TableSorter() {
        indexes = new int[0]; // for consistency
    }

    public TableSorter(TableModel model) {
        setModel(model);
    }

    public int getSortingColumn() {
        return (sortingColumns.size() < 1 ? -1 : ((Integer)sortingColumns.get(0)).intValue());
    }
    
    public boolean getSortingAscending() {
        return ascending;
    }

    public void setModel(TableModel model) {
        super.setModel(model);
        reallocateIndexes();
    }

    private int compareStringDates(String s1, String s2) {
        // added by Nicolas Michael, to compare Strings containing dates
        TMJ t1 = EfaUtil.string2date(s1, 0, 0, 0);
        TMJ t2 = EfaUtil.string2date(s2, 0, 0, 0);
        if (t1.tag > 0 && t1.monat > 0 && t1.jahr > 0
                && t2.tag > 0 && t2.monat > 0 && t2.jahr > 0
                && EfaUtil.countCharInString(s1, '.') == 2
                && EfaUtil.countCharInString(s2, '.') == 2) {
            if (t1.jahr < t2.jahr) {
                return -1;
            } else if (t1.jahr > t2.jahr) {
                return 1;
            } else if (t1.monat < t2.monat) {
                return -1;
            } else if (t1.monat > t2.monat) {
                return 1;
            } else if (t1.tag < t2.tag) {
                return -1;
            } else if (t1.tag > t2.tag) {
                return 1;
            } else {
                int p1 = s1.indexOf(" ");
                int p2 = s2.indexOf(" ");
                if (p1 > 0 && p2 > 0) {
                    s1 = s1.substring(p1 + 1);
                    s2 = s2.substring(p2 + 1);
                    t1 = EfaUtil.string2date(s1, -1, -1, -1);
                    t2 = EfaUtil.string2date(s2, -1, -1, -1);
                    if (t1.tag >= 0 && t1.monat >= 0 && t1.jahr >= 0
                            && t2.tag >= 0 && t2.monat >= 0 && t2.jahr >= 0
                            && EfaUtil.countCharInString(s1, ':') == 2
                            && EfaUtil.countCharInString(s2, ':') == 2) {
                        // seems we've got dates and times ... let's try to compare times as well
                        if (t1.tag < t2.tag) {
                            return -1;
                        } else if (t1.tag > t2.tag) {
                            return 1;
                        } else if (t1.monat < t2.monat) {
                            return -1;
                        } else if (t1.monat > t2.monat) {
                            return 1;
                        } else if (t1.jahr < t2.jahr) {
                            return -1;
                        } else if (t1.jahr > t2.jahr) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                } else {
                    return 0;
                }
            }
        }
        return 99;
    }

    public int compareRowsByColumn(int row1, int row2, int column) {
        Class type = model.getColumnClass(column);
        TableModel data = model;

        // Check for nulls.

        Object o1 = data.getValueAt(row1, column);
        Object o2 = data.getValueAt(row2, column);

        // If both values are null, return 0.
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) { // Define null less than everything.
            return -1;
        } else if (o2 == null) {
            return 1;
        }

        /*
         * We copy all returned values from the getValue call in case
         * an optimised model is reusing one object to return many
         * values.  The Number subclasses in the JDK are immutable and
         * so will not be used in this way but other subclasses of
         * Number might want to do this to save space and avoid
         * unnecessary heap allocation.
         */

        if (type.getSuperclass() == java.lang.Number.class) {
            Number n1 = (Number) data.getValueAt(row1, column);
            double d1 = n1.doubleValue();
            Number n2 = (Number) data.getValueAt(row2, column);
            double d2 = n2.doubleValue();

            if (d1 < d2) {
                return -1;
            } else if (d1 > d2) {
                return 1;
            } else {
                return 0;
            }
        } else if (type == java.util.Date.class) {
            Date d1 = (Date) data.getValueAt(row1, column);
            long n1 = d1.getTime();
            Date d2 = (Date) data.getValueAt(row2, column);
            long n2 = d2.getTime();

            if (n1 < n2) {
                return -1;
            } else if (n1 > n2) {
                return 1;
            } else {
                return 0;
            }
        } else if (type == String.class) {
            String s1 = (String) data.getValueAt(row1, column);
            String s2 = (String) data.getValueAt(row2, column);

            // added by Nicolas Michael, to compare Strings containing dates
            int ret = compareStringDates(s1, s2);
            if (ret != 99) {
                return ret;
            }

            // added by Nicolas Michael, to compare Strings containing numbers
            if (s1.length() > 0 && s1.charAt(0) >= '0' && s1.charAt(0) <= '9'
                    && s2.length() > 0 && s2.charAt(0) >= '0' && s2.charAt(0) <= '9') {
                TMJ t1 = EfaUtil.string2date(s1, 0, 0, 0);
                TMJ t2 = EfaUtil.string2date(s2, 0, 0, 0);
                int i1 = t1.tag * 10000 + t1.monat * 100 + t1.jahr;
                int i2 = t2.tag * 10000 + t2.monat * 100 + t2.jahr;
                if (i1 < i2) {
                    return -1;
                } else if (i1 > i2) {
                    return 1;
                } else {
                    return 0;
                }
            }

            int result = s1.compareToIgnoreCase(s2);


            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        } else if (type == Boolean.class) {
            Boolean bool1 = (Boolean) data.getValueAt(row1, column);
            boolean b1 = bool1.booleanValue();
            Boolean bool2 = (Boolean) data.getValueAt(row2, column);
            boolean b2 = bool2.booleanValue();

            if (b1 == b2) {
                return 0;
            } else if (b1) { // Define false < true
                return 1;
            } else {
                return -1;
            }
        } else {
            Object v1 = data.getValueAt(row1, column);
            String s1 = v1.toString();
            Object v2 = data.getValueAt(row2, column);
            String s2 = v2.toString();

            int ret = compareStringDates(s1, s2);
            if (ret != 99) {
                return ret;
            }

            // added by Nicolas Michael, to compare Strings containing numbers
            if (s1.length() > 0 && s1.charAt(0) >= '0' && s1.charAt(0) <= '9'
                    && s2.length() > 0 && s2.charAt(0) >= '0' && s2.charAt(0) <= '9') {
                TMJ t1 = EfaUtil.string2date(s1, 0, 0, 0);
                TMJ t2 = EfaUtil.string2date(s2, 0, 0, 0);
                int i1 = t1.tag * 10000 + t1.monat * 100 + t1.jahr;
                int i2 = t2.tag * 10000 + t2.monat * 100 + t2.jahr;
                if (i1 < i2) {
                    return -1;
                } else if (i1 > i2) {
                    return 1;
                } else {
                    return 0;
                }
            }

            int result = s1.compareToIgnoreCase(s2);

            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public int compare(int row1, int row2) {
        compares++;
        for (int level = 0; level < sortingColumns.size(); level++) {
            Integer column = (Integer) sortingColumns.elementAt(level);
            int result = compareRowsByColumn(row1, row2, column.intValue());
            if (result != 0) {
                return ascending ? result : -result;
            }
        }
        return 0;
    }

    public void reallocateIndexes() {
        int rowCount = model.getRowCount();

        // Set up a new array of indexes with the right number of elements
        // for the new data model.
        indexes = new int[rowCount];

        // Initialise with the identity mapping.
        for (int row = 0; row < rowCount; row++) {
            indexes[row] = row;
        }
    }

    public void tableChanged(TableModelEvent e) {
        reallocateIndexes();

        super.tableChanged(e);
    }

    public void checkModel() {
        if (indexes.length != model.getRowCount()) {
            System.err.println("Sorter not informed of a change in model.");
        }
    }

    // work around for original code to avoid NoClassDefFoundError
    private int[] clone(int[] a) {
        if (a == null) {
            return null;
        }
        int[] b = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

    public void sort(Object sender) {
        checkModel();

        compares = 0;
        // n2sort();
        // qsort(0, indexes.length-1);

        // Original code:
        // shuttlesort((int[])indexes.clone(), indexes, 0, indexes.length);
        // produces a "java.lang.NoClassDefFoundError: jint1Array"

        shuttlesort(clone(indexes), indexes, 0, indexes.length);
    }

    public void n2sort() {
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = i + 1; j < getRowCount(); j++) {
                if (compare(indexes[i], indexes[j]) == -1) {
                    swap(i, j);
                }
            }
        }
    }

    // This is a home-grown implementation which we have not had time
    // to research - it may perform poorly in some circumstances. It
    // requires twice the space of an in-place algorithm and makes
    // NlogN assigments shuttling the values between the two
    // arrays. The number of compares appears to vary between N-1 and
    // NlogN depending on the initial order but the main reason for
    // using it here is that, unlike qsort, it is stable.
    public void shuttlesort(int from[], int to[], int low, int high) {
        if (high - low < 2) {
            return;
        }
        int middle = (low + high) / 2;
        shuttlesort(to, from, low, middle);
        shuttlesort(to, from, middle, high);

        int p = low;
        int q = middle;

        /* This is an optional short-cut; at each recursive call,
        check to see if the elements in this subset are already
        ordered.  If so, no further comparisons are needed; the
        sub-array can just be copied.  The array must be copied rather
        than assigned otherwise sister calls in the recursion might
        get out of sinc.  When the number of elements is three they
        are partitioned so that the first set, [low, mid), has one
        element and and the second, [mid, high), has two. We skip the
        optimisation when the number of elements is three or less as
        the first compare in the normal merge will produce the same
        sequence of steps. This optimisation seems to be worthwhile
        for partially ordered lists but some analysis is needed to
        find out how the performance drops to Nlog(N) as the initial
        order diminishes - it may drop very quickly.  */

        if (high - low >= 4 && compare(from[middle - 1], from[middle]) <= 0) {
            for (int i = low; i < high; i++) {
                to[i] = from[i];
            }
            return;
        }

        // A normal merge.

        for (int i = low; i < high; i++) {
            if (q >= high || (p < middle && compare(from[p], from[q]) <= 0)) {
                to[i] = from[p++];
            } else {
                to[i] = from[q++];
            }
        }
    }

    public void swap(int i, int j) {
        int tmp = indexes[i];
        indexes[i] = indexes[j];
        indexes[j] = tmp;
    }

    // The mapping only affects the contents of the data rows.
    // Pass all requests to these rows through the mapping array: "indexes".
    public Object getValueAt(int aRow, int aColumn) {
        checkModel();
        return model.getValueAt(indexes[aRow], aColumn);
    }

    public void setValueAt(Object aValue, int aRow, int aColumn) {
        checkModel();
        model.setValueAt(aValue, indexes[aRow], aColumn);
    }

    public int getOriginalIndex(int row) {
        return indexes[row];
    }

    public int getCurrentIndex(int row) {
        for (int i=0; i<indexes.length; i++) {
            if (row == indexes[i]) {
                return i;
            }
        }
        return -1;
    }

    public void sortByColumn(int column) {
        sortByColumn(column, true);
    }

    public void sortByColumn(int column, boolean ascending) {
        this.ascending = ascending;
        sortingColumns.removeAllElements();
        sortingColumns.addElement(new Integer(column));
        sort(this);
        super.tableChanged(new TableModelEvent(this));
    }

    // There is no-where else to put this.
    // Add a mouse listener to the Table to trigger a table sort
    // when a column heading is clicked in the JTable.
    public void addMouseListenerToHeaderInTable(JTable table) {
        final TableSorter sorter = this;
        final JTable tableView = table;
        tableView.setColumnSelectionAllowed(false);
        MouseAdapter listMouseListener = new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {
                    int shiftPressed = e.getModifiers() & InputEvent.SHIFT_MASK;
                    boolean ascending = (shiftPressed == 0);
                    sorter.sortByColumn(column, ascending);
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }
}
