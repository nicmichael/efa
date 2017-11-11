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

import de.nmichael.efa.data.storage.DataKey;
import java.awt.Color;
import java.awt.Window;
import java.awt.AWTEvent;
import javax.swing.*;


// @i18n complete

public interface IItemType {

    public static final int TYPE_INTERNAL = 0;
    public static final int TYPE_EXPERT = 1;
    public static final int TYPE_PUBLIC = 2;

    /**
     * Perform a copy of this Item.
     * This is not really a deep copy. For most items, this copy method will only copy those
     * values which are passed to the object in the constructor.
     * Other properties may not be copied.
     * The only purpose of this method is to copy GUI items used in EfaConfig, to
     * be displayed in the EfaConfigDialog.
     * @return
     */
    public IItemType copyOf();

    public String getName();
    public void setName(String name);
    public int getType();
    public void setCategory(String s);
    public String getCategory();
    public void setDescription(String description);
    public String getDescription();

    public void parseValue(String value);
    public String toString();
    public void getValueFromGui();
    public void parseAndShowValue(String value);
    public void showValue();
    public String getValueFromField();
    
    public int displayOnGui(Window dlg, JPanel panel, int y);
    public int displayOnGui(Window dlg, JPanel panel, int x, int y);

    public void requestFocus();
    public boolean hasFocus();

    public void setColor(Color c);
    public void setBackgroundColor(Color c);
    public void saveBackgroundColor(boolean force);
    public void restoreBackgroundColor();
    public void setVisible(boolean visible);
    public void setEnabled(boolean enabled);
    public boolean isEnabled();
    public void setEditable(boolean editable);
    public boolean isEditable();
    public boolean isVisible();

    public void setPadding(int padXbefore, int padXafter, int padYbefore, int padYafter);
    public void setFieldSize(int width, int height);
    public void setFieldGrid(int gridWidth, int gridAnchor, int gridFill);
    public void setFieldGrid(int gridWidth, int gridHeight, int gridAnchor, int gridFill);

    public boolean isValidInput();
    public String getInvalidErrorText();
    public void setNotNull(boolean notNull);
    public boolean isNotNullSet();

    public void setUnchanged();
    public void setChanged();
    public boolean isChanged();

    public void registerItemListener(IItemListener listener);
    public void actionEvent(AWTEvent e);

    // methods which allow to store a DataKey inside an IItemType (only for special purposes)
    public void setDataKey(DataKey k);
    public DataKey getDataKey();

    public JComponent getComponent();

    public void setReferenceObject(Object o);
    public Object getReferenceObject();


}
