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

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import de.nmichael.efa.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.gui.*;
import de.nmichael.efa.gui.util.*;
import java.awt.image.BufferedImage;

public class ItemTypeList extends ItemType implements ActionListener {

    JPanel mypanel;
    JLabel label;
    JScrollPane scrollPane;
    JList list = new JList();
    JPopupMenu popup;
    Vector<ItemTypeListData> data;
    String[] actions;
    String incrementalSearch = "";
    int iconWidth = 0;
    int iconHeight = 0;

    class ListDataCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean iss, boolean chf) {
            super.getListCellRendererComponent(list, value, index, iss, chf);

            if (iconWidth > 0 && iconHeight > 0) {
                try {
                    ItemTypeListData item = (ItemTypeListData)value;
                    ImageIcon icon = null;
                    if (item.image != null) {
                        icon = BaseDialog.getIcon(item.image);
                    }
                    if (icon == null) {
                        BufferedImage image = new BufferedImage(iconWidth, iconHeight,
                                BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = image.createGraphics();
                        if (item.colors != null && item.colors.length > 0) {
                            if (item.colors.length == 1) {
                                g.setColor(item.colors[0]);
                                g.fillOval(0, 0, iconWidth, iconHeight);
                            } else {
                                int currentAngle = 90;
                                int anglePerColor = 360 / item.colors.length;
                                for (int i=0; i<item.colors.length; i++) {
                                    g.setColor(item.colors[i]);
                                    g.fillArc(0, 0, iconWidth, iconHeight,
                                            currentAngle % 360, anglePerColor);
                                    currentAngle += anglePerColor;
                                }
                            }
                        } else {
                            if (!item.separator) {
                                g.setColor(new Color (230,230,230));
                                g.fillOval(0, 0, iconWidth, iconHeight);
                            }
                        }
                        icon = new ImageIcon(image);
                    }
                    if (icon.getIconWidth() > iconWidth
                            || icon.getIconHeight() > iconHeight) {
                        icon = new ImageIcon(icon.getImage().getScaledInstance(iconWidth, iconHeight,
                                Image.SCALE_SMOOTH));
                    }
                    setIcon(icon);
                } catch(Exception eignore) {
                }
            }
            return this;
        }
    }

    class ItemTypeListData {
        String text;
        Object object;
        boolean separator;
        int section;
        String image;
        Color[] colors;
        public ItemTypeListData(String text, Object object, boolean separator, int section) {
            ini(text, object, separator, section, null, null);
        }
        public ItemTypeListData(String text, Object object, boolean separator, int section,
                String image, Color[] colors) {
            ini(text, object, separator, section, image, colors);
        }
        private void ini(String text, Object object, boolean separator, int section,
                String image, Color[] colors) {
            this.text = text;
            this.object = object;
            this.separator = separator;
            this.section = section;
            this.image = image;
            this.colors = colors;
        }
        public String toString() {
            return text;
        }
    }

    public ItemTypeList(String name,
            int type, String category, String description) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.description = description;
        data = new Vector<ItemTypeListData>();
    }

    public IItemType copyOf() {
        return new ItemTypeList(name, type, category, description);
    }

    public void addItem(String text, Object object, boolean separator, char separatorHotkey) {
        data.add(new ItemTypeListData(text, object, separator, separatorHotkey));
    }

    public void addItem(String text, Object object, boolean separator, char separatorHotkey,
            String image, Color[] colors) {
        data.add(new ItemTypeListData(text, object, separator, separatorHotkey, image, colors));
    }

    public void removeItem(int idx) {
        if (idx >= 0 && idx < data.size()) {
            data.remove(idx);
        }
    }

    public void removeAllItems() {
        data = new Vector<ItemTypeListData>();
    }

    public void setItems(Vector<ItemTypeListData> items) {
        if (data == null) {
            data = new Vector<ItemTypeListData>();
        }
        if (items != null) {
            data = items;
        } else {
            data = new Vector<ItemTypeListData>();
        }
    }

    public int size() {
        if (data == null) {
            return 0;
        }
        return data.size();
    }

    public String getItemText(int idx) {
        if (idx >= 0 && idx < data.size()) {
            return data.get(idx).text;
        }
        return null;
    }

    public Object getItemObject(int idx) {
        if (idx >= 0 && idx < data.size()) {
            return data.get(idx).object;
        }
        return null;
    }

    void clearIncrementalSearch() {
            incrementalSearch = "";
            if (list != null) {
                list.setToolTipText(null);
            }
    }

    public String getIncrementalSearchString() {
        return incrementalSearch;
    }

    public void setPopupActions(String[] actions) {
        this.actions = actions;
    }

    protected void iniDisplay() {
        // not used, everything done in displayOnGui(...)
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        panel.add(setupPanel(dlg), new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, 0.0, 0.0,
                fieldGridAnchor, fieldGridFill, new Insets(padYbefore, 0, padYafter, padXafter), 0, 0));
        showValue();
        return 1;
    }

    public int displayOnGui(Window dlg, JPanel panel, String borderLayoutOrientation) {
        panel.add(setupPanel(dlg), borderLayoutOrientation);
        showValue();
        return 1;
    }

    private JPanel setupPanel(Window dlg) {
        this.dlg = dlg;

        list = new JList();
        popup = new JPopupMenu();
        scrollPane = new JScrollPane();
        this.field = scrollPane;
        mypanel = new JPanel();
        mypanel.setLayout(new BorderLayout());

        if (getDescription() != null) {
            label = new JLabel();
            Mnemonics.setLabel(dlg, label, getDescription() + ": ");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (type == IItemType.TYPE_EXPERT) {
                label.setForeground(Color.red);
            }
            if (color != null) {
                label.setForeground(color);
            }
            label.setLabelFor(list);
            Dialog.setPreferredSize(label, fieldWidth, 20);
        }

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        for (int i = 0; actions != null && i < actions.length; i++) {
            JMenuItem menuItem = new JMenuItem(actions[i].substring(1));
            menuItem.setActionCommand(EfaMouseListener.EVENT_POPUP_CLICKED + "_" + actions[i].substring(0, 1));
            menuItem.addActionListener(this);
            popup.add(menuItem);
        }
        // KeyListeners entfernen, damit unter Java 1.4.x nicht automatisch gescrollt wird, sondern durch den eigenen Algorithmus
        try {
            KeyListener[] kl = list.getKeyListeners();
            for (int i = 0; i < kl.length; i++) {
                list.removeKeyListener(kl[i]);
            }
        } catch (NoSuchMethodError e) { /* Java 1.3 kennt diese Methode nicht */ }
        list.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                list_keyReleased(e);
            }
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                ((JList) e.getSource()).setToolTipText(null); // remove tool tip from scrolling/searching
            }
        });

        EfaMouseListener listener = new EfaMouseListener(list, popup, this, Daten.efaConfig.getValueEfaDirekt_autoPopupOnBoatLists());
        list.addMouseListener(listener);

        list.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(FocusEvent e) {
                list_focusGained(e);
            }
        });

        scrollPane.getViewport().add(list, null);
        mypanel.setLayout(new BorderLayout());
        if (getDescription() != null) {
            mypanel.add(label, BorderLayout.NORTH);
        }
        mypanel.add(scrollPane, BorderLayout.CENTER);

        return mypanel;
    }

    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.itemListenerAction(this, e);
        }
    }

    public void clearPopup() {
        if (popup != null) {
            popup.setVisible(false);
        }
    }

    public void clearSelection() {
        try {
            //list.setSelectedIndices(new int[0]);
            list.clearSelection();
        } catch(Exception e) {
        }
    }

    public boolean isFocusOwner() {
        return list.isFocusOwner();
    }

    private void list_keyReleased(KeyEvent e) {
        clearPopup();
        if (e != null) {
            scrollToEntry(String.valueOf(e.getKeyChar()), 15, (e != null && e.getKeyCode() == 38 ? -1 : 1));  // KeyCode 38 == Cursor Up
        }
        if (listener != null) {
            listener.itemListenerAction(this, e);
        }
    }

    private void list_focusGained(FocusEvent e) {
        if (list != null && list.getFirstVisibleIndex() >= 0 && list.getSelectedIndex() < 0) {
            list.setSelectedIndex(0);
        }
        clearIncrementalSearch();
        if (listener != null) {
            listener.itemListenerAction(this, e);
        }
    }

    // scrolle in der Liste list (deren Inhalt der Vector entries ist), zu dem Eintrag
    // mit dem Namen such und selektiere ihn. Zeige unterhalb des Boote bis zu plus weitere Einträge.
    private void scrollToEntry(String search, int plus, int direction) {
        if (list == null || search == null || search.length() == 0) {
            return;
        }
        try {
            int start = 0;

            int index = -1;

            if (search.charAt(0) >= '0' && search.charAt(0) <= '9') {
                int isearch = search.charAt(0) - '0';
                // search for a section with the corresponding number
                for (int i=0; i<data.size(); i++) {
                    if (data.get(i).section == isearch) {
                        index = i;
                        break;
                    }
                }
                incrementalSearch = "";
            } else {
                // search for names within the list
                start = Math.max(list.getSelectedIndex(), 0);

                if (incrementalSearch == null || incrementalSearch.length() == 0) {
                    // if we haven't searched for anything before, jump to the start of this section
                    while (start > 0 && !((String) data.get(start).text).startsWith("---------- ")) {
                        start--;
                    }
                }
                // build new search string depending of previous search
                char c = search.charAt(0);
                if (Character.isLetter(c) || Character.isSpaceChar(c)
                        || c == '.' || c == '-' || c == '_' || c == ':' || c == ',' || c == ';') {
                    search = (incrementalSearch != null ? incrementalSearch : "") + search;
                } else {
                    if (c == 0x8) {
                        search = (incrementalSearch != null && incrementalSearch.length() > 0
                                ? incrementalSearch.substring(0, incrementalSearch.length() - 1)
                                : "");
                    } else {
                        return; // no valid search character
                    }
                }
                incrementalSearch = search;
                search = search.toLowerCase();

                boolean startsWith = search.length() == 1; // for single-character search, match strings starting with this search string; otherwise, match somewhere
                for (int run = 0; run < 2; run++) { // 2 search runs: 1st - start from "start"; 2nd - if no result, restart from 0
                    for (int i = start; i < data.size(); i++) {
                        String item = ((String) data.get(i).text).toLowerCase();
                        if (startsWith && item.startsWith(search) || !startsWith && item.contains(search)) {
                            index = i;
                            break;
                        }
                    }
                    if (index != -1) {
                        break;
                    } else {
                        start = 0;
                    }
                }
            }

            // check whether we should really select this item
            while (index >= 0 && index < data.size() && data.get(index).separator) {
                index += direction;
            }

            // Item found?
            if (index >= 0 && index < data.size()) {
                list.setSelectedIndex(index);
                Rectangle rect = list.getCellBounds(index, (index + plus >= data.size() ? data.size() - 1 : index + plus));
                list.scrollRectToVisible(rect);
            }

            Rectangle rect = list.getVisibleRect();
            if (search.startsWith("---")) {
                list.setToolTipText(null);
            } else {
                list.setToolTipText((search.length() > 0 ? search : null));
                int origDelay = ToolTipManager.sharedInstance().getInitialDelay();
                ToolTipManager.sharedInstance().setInitialDelay(0);
                ToolTipManager.sharedInstance().mouseMoved(
                        new MouseEvent(list, 0, 0, 0,
                        10, rect.y + rect.height - 50,
                        0, false));
                ToolTipManager.sharedInstance().setInitialDelay(origDelay);
            }

        } catch (Exception ee) { /* just to be sure */ }
    }

    public Object getSelectedValue() {
        try {
            if (list == null || list.isSelectionEmpty()) {
                return null;
            }
            ItemTypeListData item = (ItemTypeListData)list.getSelectedValue();
            return item.object;
        } catch (Exception e) {
            return null;
        }
    }

    public String getSelectedText() {
        try {
            if (list == null || list.isSelectionEmpty()) {
                return null;
            }
            ItemTypeListData item = (ItemTypeListData)list.getSelectedValue();
            return item.text;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isValidInput() {
        return true;
    }

    public String getValueFromField() {
        return null;
    }

    public void showValue() {
        list.setListData(data);
        list.setCellRenderer(new ListDataCellRenderer());
    }
    
    public void getValueFromGui() {
    }
    
    public void parseValue(String value) {
        // nothing to do
    }

    public void setFieldSize(int width, int height) {
        super.setFieldSize(width, height);
        if (scrollPane != null) {
            scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        }
    }

    public void setDescription(String s) {
        super.setDescription(s);
        if (label != null) {
            label.setText(s);
        }
    }

    public void setSelectedIndex(int i) {
        list.setSelectedIndex(i);
    }

    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }

    public JPanel getPanel() {
        return mypanel;
    }

}
