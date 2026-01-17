/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.widgets;

import javax.swing.JComponent;
import javax.swing.JPanel;

import de.nmichael.efa.data.LogbookRecord;

public interface IWidgetInstance {

    public void show(JPanel panel, String panelPosition, String preferredOrientation);
    public void stop();
    
    public JComponent getComponent();
    public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r);
	
    public String getPosition();
    
}
