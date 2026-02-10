/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ItemTypeCronEntry extends ItemTypeLabelValue {

    private static final String FIELD_SEPARATOR = ";";

    private boolean[] minute;
    private boolean[] hour;
    private boolean[] day;
    private boolean[] month;
    private boolean[] dayofweek;
    private String command;

    private JTextField fMinute;
    private JTextField fHour;
    private JTextField fDay;
    private JTextField fMonth;
    private JTextField fDayOfWeek;
    private JTextField fCommand;
    private JLabel fFrequency;

    public ItemTypeCronEntry(String name, String value, int type,
            String category, String description) {
        this.name = name;
        parseValue(value);
        this.type = type;
        this.category = category;
        this.description = description;
    }
    
    public IItemType copyOf() {
        ItemTypeCronEntry copy = new ItemTypeCronEntry(name, toString(), type, category, description);
        copy.fieldWidth = fieldWidth;
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);        
        copy.setIcon((label == null ? null : label.getIcon()));
        return copy;
    }

    public void parseValue(String value) {
        if (value != null) {
            value = value.trim();
        }
        for (int i=0; i<6; i++) {
            int pos = value.indexOf(FIELD_SEPARATOR);
            String s = (pos >= 0 ? value.substring(0, pos) : value);
            switch(i) {
                case 0:
                    minute = parseTimeField(s, 59);
                    break;
                case 1:
                    hour = parseTimeField(s, 23);
                    break;
                case 2:
                    day = parseTimeField(s, 31);
                    break;
                case 3:
                    month = parseTimeField(s, 12);
                    break;
                case 4:
                    dayofweek = parseTimeField(s, 7);
                    break;
                case 5:
                    command = s;
            }
            if (pos >= 0 && pos+1 < value.length()) {
                value = value.substring(pos+1);
            } else {
                value = "";
            }
        }
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(timeFieldToString(minute, 0) + FIELD_SEPARATOR);
        s.append(timeFieldToString(hour, 0) + FIELD_SEPARATOR);
        s.append(timeFieldToString(day, 1) + FIELD_SEPARATOR);
        s.append(timeFieldToString(month, 1) + FIELD_SEPARATOR);
        s.append(timeFieldToString(dayofweek, 0) + FIELD_SEPARATOR);
        s.append((command != null ? command : ""));
        return s.toString();
    }

    private boolean[] parseTimeField(String s, int highest) {
        boolean[] b = new boolean[highest+1];
        s = s.trim();
        if (s.length() == 0 || s.equals("*")) {
            Arrays.fill(b, true);
            return b;
        }
        StringTokenizer tok = new StringTokenizer(s, ",");
        while (tok.hasMoreTokens()) {
            String f = tok.nextToken().trim();
            int pos = f.indexOf("-");
            if (pos <= 0 || pos+2 > f.length()) {
                int i = EfaUtil.string2int(f, -1);
                if (i >= 0 && i < b.length) {
                    b[i] = true;
                }
            } else {
                int i1 = EfaUtil.string2int(f.substring(0, pos), -1);
                int i2 = EfaUtil.string2int(f.substring(pos+1), -1);
                for (int i=i1; i<=i2 && i>=0 && i<b.length; i++) {
                    b[i] = true;
                }
            }
        }
        return b;
    }

    private String timeFieldToString(boolean[] b, int startValue) {
        if (b == null) {
            return "";
        }
        boolean allSelected = true;
        for (int i=startValue; i<b.length; i++) {
            if (!b[i]) {
                allSelected = false;
            }
        }
        if (allSelected) {
            return "*";
        }
        
        StringBuilder s = new StringBuilder();
        boolean inrange = false;
        for (int i=startValue; i<=b.length; i++) {
            if (i<b.length && b[i]) {
                if (s.length() == 0) {
                    s.append(Integer.toString(i));
                } else {
                    if (b[i-1]) {
                        if (!inrange) {
                            s.append("-");
                        }
                        inrange = true;
                    } else {
                        s.append("," + Integer.toString(i));
                    }
                }
            } else {
                if (s.length() > 0) {
                    if (inrange) {
                        s.append(Integer.toString(i-1));
                    }
                    inrange = false;
                }
            }
        }
        return s.toString();
    }

    private JTextField initializeField(String labelTxt, int width, JPanel panel, int y, Boolean withToolTip) {
        JLabel l = new JLabel(labelTxt + ":");
        JTextField f = new JTextField();
        Dialog.setPreferredSize(f, width, 19);
        panel.add(l, new GridBagConstraints(0, y, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 2), 0, 0));
        panel.add(f, new GridBagConstraints(1, y, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        f.setEditable(isEditable);
        f.setDisabledTextColor(Color.black);
        f.setEnabled(isEnabled && isEditable);
        if (fieldColor != null) {
            f.setForeground(fieldColor);
            if (!(isEnabled && isEditable)) {
                f.setDisabledTextColor(fieldColor);
            }
        }
        if (withToolTip) {
	        f.addKeyListener(new java.awt.event.KeyAdapter() {
	            public void keyReleased(KeyEvent e) { updateTooltip(f); }
	        });
        }
        f.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateFieldFocusLost((JTextField)e.getSource());
            }
        });
        return f;
    }

    private void updateTooltip(JTextField theField) {
    	theField.setToolTipText(theField.getText());
    }
    
    protected JComponent initializeField() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        fMinute    = initializeField(International.getString("Minute"), 100, p, 0,false);
        fHour      = initializeField(International.getString("Stunde"), 100, p, 1, false);
        fDay       = initializeField(International.getString("Tag"), 100, p, 2,false);
        fMonth     = initializeField(International.getString("Monat"), 100, p, 3,false);
        fDayOfWeek = initializeField(International.getString("Wochentag"), 100, p, 4,false);
        fCommand   = initializeField(International.getString("Kommando"), 380, p, 5,true);
        fFrequency = new JLabel();
        p.add(fFrequency, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 2), 0, 0));
        fFrequency.setForeground(Color.blue);
        Dimension dim = p.getPreferredSize();
        p.doLayout();
        setFieldSize(dim.width+20, dim.height+20);
        return p;
    }

    public void showValue() {
        super.showValue();
        if (fMinute != null) {
            fMinute.setText(timeFieldToString(minute, 0));
        }
        if (fHour != null) {
            fHour.setText(timeFieldToString(hour, 0));
        }
        if (fDay != null) {
            fDay.setText(timeFieldToString(day, 0));
        }
        if (fMonth != null) {
            fMonth.setText(timeFieldToString(month, 0));
        }
        if (fDayOfWeek != null) {
            fDayOfWeek.setText(timeFieldToString(dayofweek, 0));
        }
        if (fCommand != null) {
            fCommand.setText(command != null ? command : "");
            fCommand.setToolTipText(fCommand.getText());//show complete command in tooltip
        }
        showFrequency();
    }

    public String getValueFromField() {
        minute = parseTimeField(fMinute.getText(), 59);
        hour = parseTimeField(fHour.getText(), 23);
        day = parseTimeField(fDay.getText(), 31);
        month = parseTimeField(fMonth.getText(), 12);
        dayofweek = parseTimeField(fDayOfWeek.getText(), 7);
        command = fCommand.getText().trim();
        return toString();
    }

    private void updateFieldFocusLost(JTextField f) {
        if (fMinute == f) {
            f.setText(timeFieldToString(parseTimeField(f.getText(), 59), 0));
        }
        if (fHour == f) {
            f.setText(timeFieldToString(parseTimeField(f.getText(), 23), 0));
        }
        if (fDay == f) {
            f.setText(timeFieldToString(parseTimeField(f.getText(), 31), 1));
        }
        if (fMonth == f) {
            f.setText(timeFieldToString(parseTimeField(f.getText(), 12), 1));
        }
        if (fDayOfWeek == f) {
            f.setText(timeFieldToString(parseTimeField(f.getText(), 7), 0));
        }
        if (fCommand == f) {
            f.setText(f.getText().trim());
            f.setToolTipText(f.getText());//show complete command in tooltip
        }
        showFrequency();
    }

    private int countValues(boolean[] bool) {
        int count = 0;
        for (boolean b : bool) {
            if (b) {
                count++;
            }
        }
        return count;
    }

    private void showFrequency() {
        int minutes = countValues(parseTimeField(fMinute.getText(), 59));
        int hours = countValues(parseTimeField(fHour.getText(), 23));
        int days = countValues(parseTimeField(fDay.getText(), 31));
        int months = countValues(parseTimeField(fMonth.getText(), 12));
        int weekdays = countValues(parseTimeField(fDayOfWeek.getText(), 7));
        int perday = minutes * hours;
        StringBuilder s = new StringBuilder();
        s.append(perday + " " + International.getString("mal"));
        if (weekdays >= 7) {
            if (days >= 31) {
                s.append(" " + International.getString("täglich"));
                if (months < 12) {
                    s.append(" " + International.getMessage("in {count} Monaten", months));
                }
            } else {
                if (days == 1) {
                    if (months >= 12) {
                        s.append(" " + International.getString("im Monat"));
                    } else {
                        s.append(" " + International.getString("am Tag") +
                                 " " + International.getMessage("in {count} Monaten", months));
                    }
                } else {
                    if (days >= 31) {
                        s.append(" " + International.getString("täglich"));
                    } else {
                        s.append(" " + International.getMessage("an {count} Tagen", days));
                    }
                    if (months < 12) {
                        s.append(" " + International.getMessage("in {count} Monaten", months));
                    }
                }
            }
        } else {
            s.append(" " + International.getMessage("an {count} Wochentagen", weekdays));
        }
        fFrequency.setText(s.toString());
    }

    public boolean isValidInput() {
        return true;
    }

    public boolean isNowSelected() {
        Calendar cal = new GregorianCalendar();
        int mm = cal.get(Calendar.MINUTE);
        int hh = cal.get(Calendar.HOUR_OF_DAY);
        int DD = cal.get(Calendar.DAY_OF_MONTH);
        int MM = cal.get(Calendar.MONTH) + 1;
        int WD = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        return (minute != null && minute[mm] &&
                hour != null && hour[hh] &&
                day != null && day[DD] &&
                month != null && month[MM] &&
                dayofweek != null && dayofweek[WD]);
    }

    public String getCommand() {
        return command;
    }

}
