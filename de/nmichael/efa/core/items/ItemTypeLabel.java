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

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import de.nmichael.efa.gui.util.RoundedLabel;
import de.nmichael.efa.util.EfaUtil;

public class ItemTypeLabel extends ItemType {

    private JLabel[] labels = null;
    protected ImageIcon icon;
    private boolean mouseClickListener = false;
    private boolean roundShape=false;
    protected int iconPosition=-1;
    private boolean boldFont = false;
    		
    public ItemTypeLabel(String name, int type,
            String category, String description) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.description = description;
        this.fieldGridWidth = 2;
    }

    public IItemType copyOf() {
    	
        ItemTypeLabel copy=new ItemTypeLabel(name, type, category, description);
        copy.setVisible(this.isVisible());
        copy.setFieldSize(fieldWidth,  fieldHeight);
        copy.setBackgroundColor(this.backgroundColor);
        copy.setColor(this.color);
        copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        copy.setFieldGrid(fieldGridWidth, fieldGridHeight, fieldGridAnchor, fieldGridFill);
        copy.setImage(icon);
        copy.setHorizontalAlignment(hAlignment);
        copy.setRoundShape(roundShape);
        copy.setBorder(border);
        copy.setImagePosition(iconPosition);
        copy.setBoldFont(boldFont);
        copy.setIndent(this.isIndent());
        return copy;

    }

    public void parseValue(String value) {
    }

    public String toString() {
        return description;
    }

    /**
	 * Setzt die Beschreibung und verteilt sie auf die Labels.
	 */
    public void setDescription(String s) {
        super.setDescription(s);
        /* TODO:
         * This method works ok if the number of lines in the description does not GROW.
         * If the number of lines increases, the additional lines won't be displayed because the labels array is not big enough.
         *
         * This would need to be fixed by creating a new labels array with the correct size and adding the new labels to the GUI.
         * But it would need some more references to the GUI components to do that. So for now, we just ignore this case and assume that the number of lines does not grow.
         *
         */
        Vector<String> v = EfaUtil.split(description, '\n');
        if (v.size() == 0) {
            v.add("");
        }
        if (labels != null) {
            for (int i = 0; i < labels.length; i++) {
                if (i < v.size()) {
                    labels[i].setText(v.get(i));
                    labels[i].setVisible(isVisible);
                } else {
                    // überzählige Labels leeren und ausblenden
                    labels[i].setText("");
                    labels[i].setVisible(false);
                }
            }
        }
    }

    protected void iniDisplay() {
        Vector<String> v = EfaUtil.split(description, '\n');
        if (v.size() == 0) {
            v.add("");
        }
        labels = new JLabel[v.size()];
        for (int i=0; i<v.size(); i++) {
            JLabel l = createLabel();
            
            l.setText((String)v.get(i));
            if (i == 0 && icon != null) {
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setHorizontalTextPosition(SwingConstants.CENTER);
                l.setIcon(icon);
            }
            if (hAlignment != -1) {
                l.setHorizontalAlignment(hAlignment);
                if (iconPosition==-1) {
                	l.setHorizontalTextPosition(hAlignment);
                } else {
                	l.setHorizontalTextPosition(iconPosition);
                }
            }
            if (this.getColor() != null) {
                l.setForeground(this.getColor());
            }
            if (this.getBackgroundColor() != null) {
            	l.setBackground(this.getBackgroundColor());
            	l.setOpaque(true);
            }            

            if (this.isBoldFont()) {
                l.setFont(l.getFont().deriveFont(Font.BOLD));
            }

            l.setVisible(isVisible);
            l.setBorder(this.getBorder());
            labels[i] = l;
        }
        if (mouseClickListener) {
            addMouseClickListener();
        }
    }

    protected JLabel createLabel() {
    	if (roundShape) {
    		return new RoundedLabel();
    	} else {
    		return new JLabel();
    	}
    }
    
    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        this.dlg = dlg;
        iniDisplay();
        for (int i=0; i<labels.length; i++) {
            panel.add(labels[i], new GridBagConstraints((this.isIndent() ? x+1 : x), y + i, fieldGridWidth, fieldGridHeight, 0.0, 0.0,
                    fieldGridAnchor, fieldGridFill, new Insets((i == 0 ? padYbefore : 0), padXbefore, (i+1 == labels.length ? padYafter : 0), padXafter), 0, 0));
        }
        return labels.length;
    }

    public void getValueFromGui() {
    }

    public void requestFocus() {
    }

    public String getValueFromField() {
        return null;
    }

    public void showValue() {
    }

    public boolean isValidInput() {
        return true;
    }
    
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        for (int i=0; labels != null && i<labels.length; i++) {
            labels[i].setVisible(visible);
        }
        isVisible = visible;
    }

    public boolean isVisible() {
        return isVisible;
    }


    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Color c = (enabled 
        			? (getColor() != null ? getColor() : new JLabel().getForeground()) 
        			: Color.gray);
        for (int i=0; labels != null && i<labels.length; i++) {
            labels[i].setForeground(c);
        }
    }

    public boolean isEditable() {
        return false;
    }
    
    protected boolean isBoldFont() {
    	return boldFont;
    }
    
    public void setBoldFont(boolean value) {
    	boldFont=value;
    }
    
    protected Border getBorder() {
    	return border;
    }
    
    public boolean getRoundShape() {
    	return roundShape;
    }
    
    public void setRoundShape(boolean value) {
    	roundShape=value;
    }
    
    public void setImage(ImageIcon icon) {
        this.icon = icon;
        if (labels != null && labels.length > 0 && labels[0] != null) {
            labels[0].setIcon(icon);
        }
    }

    public void setImagePosition(int pos) {
    	this.iconPosition = pos;
    }
    
    public void activateMouseClickListener() {
        mouseClickListener = true;
    }

    private void addMouseClickListener() {
        for (int i=0; i<labels.length; i++) {
        labels[i].addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                labelMouseClicked(e);
            }

            public void mouseEntered(MouseEvent e) {
                labelMouseEntered(e);
            }

            public void mouseExited(MouseEvent e) {
                labelMouseExited(e);
            }
        });

        }
    }

    private void labelMouseClicked(MouseEvent e) {
        actionEvent(e);
    }

    private void labelMouseEntered(MouseEvent e) {
        try {
            JLabel label = (JLabel) e.getSource();
            label.setForeground(Color.red);
        } catch (Exception eignore) {
        }
    }

    private void labelMouseExited(MouseEvent e) {
        try {
            JLabel label = (JLabel) e.getSource();
            Color c = (isEnabled()
                    ? (getColor() != null ? getColor() : new JLabel().getForeground())
                    : Color.gray);
            label.setForeground(c);
        } catch (Exception eignore) {
        }
    }

   


}
