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

import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import de.nmichael.efa.util.International;

import java.util.Vector;

public class TableItem {

    private String txt;
    private boolean marked = false;
    private boolean disabled = false;
    private boolean invisible = false;

    public TableItem(String txt) {
        this.txt = (txt != null ? txt : "");
    }

    public TableItem(String txt, boolean marked) {
        this.txt = (txt != null ? txt : "");
        this.marked = marked;
    }

    public TableItem(Object o) {
        this.txt = (o != null && o.toString() != null ? o.toString() : "");
    }

    public String toString() {
    	return this.txt;
    }
    
    public String getToolTipText() {
    	if (!this.invisible) {
    		return null;
    	} else {
    		return International.getString("Verstecken")+": "+this.txt;
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

}
