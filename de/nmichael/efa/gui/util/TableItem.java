/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.gui.util;

import java.util.Vector;

import javax.swing.ImageIcon;

import de.nmichael.efa.util.International;


/**
 * Used as container for Items to be displayed in *ListDialogs.
 * Usually created by DataRecord*.getGuiTableItems().
 * Can handle 
 * - status of an item (Visible, Disabled, Marked)
 * - multiple Icons (should be of same dimensions)
 * - toolTipText
 */
public class TableItem {

    private String txt;
    private boolean marked = false;
    private boolean disabled = false;
    private boolean invisible = false;
    private Class dataType;
    private Vector <ImageIcon> icons = null;
    private String toolTipText=null;

    public TableItem(String txt) {
        this.txt = (txt != null ? txt : "");
        this.dataType= String.class;
    }

    public TableItem(String txt, boolean marked) {
        this.txt = (txt != null ? txt : "");
        this.marked = marked;
    }

    public TableItem(Object o) {
        this.txt = (o != null && o.toString() != null ? o.toString() : "");
        this.dataType = (o!= null ? o.getClass() : null);
    }

    public String toString() {
    	return this.txt;
    }
    
    public String getToolTipText() {
    	if (!this.invisible) {
    		if (this.getIcons()!=null && this.getIcons().size()>0) {
    			return this.txt +(this.toolTipText==null ? "" : "\n"+this.toolTipText);
    		}
    		return this.toolTipText;
    	} else {
    		// Automatically add a prefix for the actual tooltip, if the record has invisible status.
    		return this.txt + " ("+International.getString("Verstecken")+") "+ (this.toolTipText==null ? "" : "\n"+this.toolTipText);
    	}
    }
    
    public boolean isMarked() {
        return marked;
    }
    
    public boolean isInvisible() {
    	return invisible;
    }

    public void setText(String text) {
        this.txt = text;
    }
    
    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
    
    public void setInvisible(boolean invisible) {
    	this.invisible = invisible;
    }

    public Class getType() {
    	return this.dataType;
    }

    public void addIcon(ImageIcon value) {
    	if (this.icons == null) {
    		this.icons = new Vector<ImageIcon>();
    	}
    	if (value!=null) {
    		this.icons.add(value);
    	}
    }
    
    public Vector<ImageIcon> getIcons() {
    	return this.icons;
    }
    
    public void setToolTipText(String value) {
    	this.toolTipText=value;
    }
    
}
