/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.java15;

// @i18n complete

public class MyPropertyChangeListener implements java.beans.PropertyChangeListener {
  javax.swing.JEditorPane editorPane;
  
  public void setEditorPane(javax.swing.JEditorPane editorPane) {
    this.editorPane = editorPane;
  }
  
  public void propertyChange(java.beans.PropertyChangeEvent evt) {
    javax.swing.text.EditorKit kit = editorPane.getEditorKit();
    try {
      ((javax.swing.text.html.HTMLEditorKit)kit).setAutoFormSubmission(false);
    } catch(Exception e) {
    }
  }
}

