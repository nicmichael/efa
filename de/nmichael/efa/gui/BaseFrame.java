/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.util.ActionHandler;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Help;
import de.nmichael.efa.util.Logger;

// @i18n complete
public abstract class BaseFrame extends JFrame implements ActionListener {

    protected Window _parent;
    protected String _title;
    protected boolean _prepared = false;
    protected boolean _inCancel = false;

    private ActionHandler ah;
    protected String KEYACTION_ESCAPE;
    protected String KEYACTION_F1;
    protected String KEYACTION_F12;
    protected JPanel basePanel = new JPanel();
    protected JScrollPane mainScrollPane = new JScrollPane();
    protected JPanel mainPanel = new JPanel();
    protected JButton closeButton;
    protected String helpTopic1;
    protected String helpTopic2;
    protected IItemType focusItem;
    protected boolean resultSuccess = false;

    public BaseFrame(Window parent, String title) {
        this._parent = parent;
        this._title = title;
    }

    public boolean prepareDialog() {
        if (_prepared) {
            return false;
        }
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            iniDialogCommon(_title);
            iniDialog();
            iniDialogCommonFinish();
            EfaUtil.pack(this);
            _prepared = true;
            return true;
        } catch (Exception e) {
            //e.printStackTrace();
        	Logger.log(e);
            return false;
        }
    }

    public void showMe() {
        showFrame();
    }
    
    public void showFrame() {
        if (!_prepared && !prepareDialog()) {
            Logger.log(Logger.ERROR, "FRAME NOT PREPARED");
        	return;
        }
        Daten.iniSplashScreen(false);
        Dialog.setDlgLocation(this);
        Dialog.frameOpened(this);
        if (focusItem != null) {
            focusItem.requestFocus();
        }
        this.setVisible(true); 
    }

    public void setRequestFocus(IItemType item) {
        if (item != null && isShowing()) {
            item.requestFocus();
        }
        focusItem = item;
    }

    public JDialog getParentJDialog() {
        if (_parent instanceof JDialog) {
            return (JDialog)_parent;
        }
        return null;
    }

    public Frame getParentFrame() {
        if (_parent instanceof Frame) {
            return (Frame)_parent;
        }
        return null;
    }

    public void _keyAction(ActionEvent evt) {
        if (evt == null || evt.getActionCommand() == null) {
            return;
        }
        
        if (evt.getActionCommand().equals(KEYACTION_ESCAPE)) {
            cancel();
        }
        
        if (evt.getActionCommand().equals(KEYACTION_F1)) {
            Help.showHelp(getHelpTopics());
        }
        if (evt.getActionCommand().equals(KEYACTION_F12) && Logger.isDebugLogging()) {
            AccessStatisticsDialog dlg = new AccessStatisticsDialog(this);
            dlg.showDialog();
        }
    }

    public abstract void keyAction(ActionEvent evt);

    public String addKeyAction(String key) {
        if (ah == null) {
            ah = new ActionHandler(this);
        }
        try {
            return ah.addKeyAction(getRootPane(), JComponent.WHEN_IN_FOCUSED_WINDOW, key, "keyAction");
        } catch (NoSuchMethodException e) {
            Logger.log(Logger.ERROR, Logger.MSG_GUI_ERRORACTIONHANDLER, "Error setting up ActionHandler for "+getClass().getCanonicalName()+": "+e.toString()); // no need to translate
            return null;
        }
    }

    protected void iniDialogCommon(String title) throws Exception {
        helpTopic1 = getClass().getCanonicalName();
        helpTopic2 = getClass().getSuperclass().getCanonicalName();
        if (!helpTopic2.startsWith("de.nmichael.efa") || 
             helpTopic2.startsWith(BaseFrame.class.getCanonicalName())) {
            helpTopic2 = null;
        }
        if (Logger.isTraceOn(Logger.TT_HELP, 5)) {
            Logger.log(Logger.DEBUG, Logger.MSG_HELP_DEBUGHELPTOPICFRAMEOPENED, "Dialog Help Topic(s): "+helpTopic1 + (helpTopic2 != null ? " , " + helpTopic2 : ""));
        }

        KEYACTION_ESCAPE = addKeyAction("ESCAPE");
        KEYACTION_F1 = addKeyAction("F1");
        if (Logger.isDebugLogging()) {
            KEYACTION_F12 = addKeyAction("F12");
        }

        if (title != null) {
            setTitle(title);
        }
        basePanel.setLayout(new BorderLayout());
    }

    protected void iniDialogCommonFinish() {
        try {
	    	getContentPane().add(basePanel, null);
	        basePanel.add(mainScrollPane, BorderLayout.CENTER);
	
	        // intelligent sizing of this Dialog:
	        // make it as big as necessary for display without scrollbars (plus some margin),
	        // as long as it does not exceed the configured screen size.
	        Dimension dim = mainPanel.getPreferredSize();
	        Dimension minDim = mainPanel.getMinimumSize();
	        if (minDim.width > dim.width) {
	            dim.width = minDim.width;
	        }
	        if (minDim.height > dim.height) {
	            dim.height = minDim.height;
	        }
	        if (dim.width < 100) {
	            dim.width = 100;
	        }
	        if (dim.height < 50) {
	            dim.height = 50;
	        }
	        dim.width  += mainScrollPane.getVerticalScrollBar().getPreferredSize().getWidth() + 40;
	        dim.height += mainScrollPane.getHorizontalScrollBar().getPreferredSize().getHeight() + 20;
	        mainScrollPane.setPreferredSize(Dialog.getMaxSize(dim));
	
	        mainScrollPane.getViewport().add(mainPanel, null);
	        int borderSize=4;
	        if (Daten.efaConfig.getValueEfaDirekt_startMaximized()) {
	        	borderSize=0;
	        }
	        mainScrollPane.setBorder(BorderFactory.createEmptyBorder(borderSize,borderSize,borderSize,borderSize));
        } catch (Exception e) {
        	Logger.logdebug(e);
        }

    }

    //protected abstract void iniDialog() throws Exception;
    protected void iniDialog() throws Exception {
        
    }

    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            if (cancel()) {
                super.processWindowEvent(e);
                return;
            } else {
                return;
            }
        }
        super.processWindowEvent(e);
    }

    public boolean cancel() {
        _inCancel = true;
        Dialog.frameClosed(this);
        dispose();
        return true;
    }

    public void actionPerformed(ActionEvent e) {
    }

    // may be implemented by subclasses to take action when GUI needs to be set up new
    public void updateGui() {
    }

    protected void setDialogResult(boolean success) {
        this.resultSuccess = success;
    }

    public boolean getDialogResult() {
        return resultSuccess;
    }

    public JScrollPane getScrollPane() {
        return mainScrollPane;
    }

    public static ImageIcon getIcon(String name) {
    	return ImagesAndIcons.getIcon(name);
    }

    protected void setIcon(AbstractButton button, ImageIcon icon) {
        if (icon != null) {
            button.setIcon(icon);
        }
    }

    public String[] getHelpTopics() {
        return (helpTopic2 != null ? new String[] { helpTopic1, helpTopic2 } : new String[] { helpTopic1 });
    }

}
