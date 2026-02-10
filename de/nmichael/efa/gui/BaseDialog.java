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

import de.nmichael.efa.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.AutoCompletePopupWindow;
import de.nmichael.efa.gui.util.RoundedBorder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// @i18n complete
public abstract class BaseDialog extends JDialog implements ActionListener {
	
	private static final String NOT_STORED_ITEM_PREFIX = "_";

    public static final String IMAGE_ACCEPT    = ImagesAndIcons.IMAGE_BUTTON_ACCEPT;
    public static final String IMAGE_ADD       = ImagesAndIcons.IMAGE_BUTTON_ADD;
    public static final String IMAGE_CANCEL    = ImagesAndIcons.IMAGE_BUTTON_CANCEL;
    public static final String IMAGE_CLOSE     = ImagesAndIcons.IMAGE_BUTTON_CLOSE;
    public static final String IMAGE_DELETE    = ImagesAndIcons.IMAGE_BUTTON_DELETE;
    public static final String IMAGE_EDIT      = ImagesAndIcons.IMAGE_BUTTON_EDIT;
    public static final String IMAGE_EDIT2     = ImagesAndIcons.IMAGE_BUTTON_EDIT2;
    public static final String IMAGE_PRINT     = ImagesAndIcons.IMAGE_BUTTON_PRINT;
    public static final String IMAGE_SELECT    = ImagesAndIcons.IMAGE_BUTTON_SELECT;
    public static final String IMAGE_HIDE      = ImagesAndIcons.IMAGE_BUTTON_HIDE;
    public static final String IMAGE_IMPORT    = ImagesAndIcons.IMAGE_BUTTON_IMPORT;
    public static final String IMAGE_EXPORT    = ImagesAndIcons.IMAGE_BUTTON_EXPORT;
    public static final String IMAGE_LIST      = ImagesAndIcons.IMAGE_BUTTON_LIST;
    public static final String IMAGE_EDITMULTI = ImagesAndIcons.IMAGE_BUTTON_EDITMULTI;
    public static final String IMAGE_MERGE     = ImagesAndIcons.IMAGE_BUTTON_MERGE;
    public static final String IMAGE_CALENDAR  = ImagesAndIcons.IMAGE_BUTTON_CALENDAR;
    public static final String IMAGE_DOWNLOAD  = ImagesAndIcons.IMAGE_BUTTON_DOWNLOAD;
    public static final String IMAGE_CONFIGURE = ImagesAndIcons.IMAGE_BUTTON_CONFIGURE;
    public static final String IMAGE_SETTINGS  = ImagesAndIcons.IMAGE_BUTTON_SETTINGS;
    public static final String IMAGE_OPEN      = ImagesAndIcons.IMAGE_BUTTON_OPEN;
    public static final String IMAGE_FIRST     = ImagesAndIcons.IMAGE_BUTTON_FIRST;
    public static final String IMAGE_NEXT      = ImagesAndIcons.IMAGE_BUTTON_NEXT;
    public static final String IMAGE_PREV      = ImagesAndIcons.IMAGE_BUTTON_PREV;
    public static final String IMAGE_LAST      = ImagesAndIcons.IMAGE_BUTTON_LAST;
    public static final String IMAGE_BACK      = ImagesAndIcons.IMAGE_BUTTON_BACK;
    public static final String IMAGE_FORWARD   = ImagesAndIcons.IMAGE_BUTTON_FORWARD;
    public static final String IMAGE_RUNEXPORT = ImagesAndIcons.IMAGE_BUTTON_RUNEXPORT;
    public static final String IMAGE_RUNIMPORT = ImagesAndIcons.IMAGE_BUTTON_RUNIMPORT;
    public static final String IMAGE_HELP      = ImagesAndIcons.IMAGE_BUTTON_HELP;
    public static final String IMAGE_RUN       = ImagesAndIcons.IMAGE_BUTTON_RUN;
    public static final String IMAGE_PREVIEW   = ImagesAndIcons.IMAGE_BUTTON_PREVIEW;
    public static final String IMAGE_STAT      = ImagesAndIcons.IMAGE_BUTTON_STAT;
    public static final String IMAGE_MARKREAD  = ImagesAndIcons.IMAGE_BUTTON_MARKREAD;
    public static final String IMAGE_MFORWARD  = ImagesAndIcons.IMAGE_BUTTON_MFORWARD;
    public static final String IMAGE_REPAIR    = ImagesAndIcons.IMAGE_BUTTON_REPAIR;
    public static final String IMAGE_LOGOUT    = ImagesAndIcons.IMAGE_BUTTON_LOGOUT;
    public static final String IMAGE_DAMAGE    = ImagesAndIcons.IMAGE_BUTTON_DAMAGE;
    public static final String IMAGE_SOAP      = ImagesAndIcons.IMAGE_BUTTON_SOAP;
    public static final String IMAGE_SEARCH    = ImagesAndIcons.IMAGE_BUTTON_SEARCH;
    public static final String IMAGE_SEARCHNEXT= ImagesAndIcons.IMAGE_BUTTON_SEARCHNEXT;
    public static final String IMAGE_SPECIAL   = ImagesAndIcons.IMAGE_BUTTON_SPECIAL;
    public static final String IMAGE_ARROWUP   = ImagesAndIcons.IMAGE_BUTTON_ARROWUP;
    public static final String IMAGE_ARROWDOWN = ImagesAndIcons.IMAGE_BUTTON_ARROWDOWN;
    public static final String IMAGE_CORRECTION = ImagesAndIcons.IMAGE_BUTTON_CORRECTION;
    public static final String IMAGE_EFACLOUD_ACTIVATE    = ImagesAndIcons.IMAGE_BUTTON_EFACLOUD_ACTIVATE;
    public static final String IMAGE_EFACLOUD_DEACTIVATE  = ImagesAndIcons.IMAGE_BUTTON_EFACLOUD_DEACTIVATE;
    public static final String IMAGE_EFACLOUD_DELETE      = ImagesAndIcons.IMAGE_BUTTON_EFACLOUD_DELETE;
    public static final String IMAGE_EFACLOUD_START       = ImagesAndIcons.IMAGE_BUTTON_EFACLOUD_START;
    public static final String IMAGE_EFACLOUD_SYNCH       = ImagesAndIcons.IMAGE_BUTTON_EFACLOUD_SYNCH;
    public static final String IMAGE_EFACLOUD_PAUSE       = ImagesAndIcons.IMAGE_BUTTON_EFACLOUD_PAUSE;
    public static final String IMAGE_EFACLOUD_STOP        = ImagesAndIcons.IMAGE_BUTTON_EFACLOUD_STOP;

    public static final String BIGIMAGE_CLOSEDOORS = ImagesAndIcons.BIGIMAGE_CLOSEDOORS;
    public static final String BIGIMAGE_DARKNESS   = ImagesAndIcons.BIGIMAGE_DARKNESS;
    public static final String BIGIMAGE_INFO       = ImagesAndIcons.BIGIMAGE_INFO;
    public static final String BIGIMAGE_WARNING    = ImagesAndIcons.BIGIMAGE_WARNING;

    protected Window _parent;
    protected String _title;
    protected String _closeButtonText;
    protected String _closeButtonImage;
    protected boolean _prepared = false;
    protected boolean _inCancel = false;
    private boolean doWindowStackChecks = true;

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

    public BaseDialog(Frame parent, String title, String closeButtonText) {
        super(parent);
        this._parent = parent;
        this._title = ucTitle(title);
        this._closeButtonText = closeButtonText;
    }

    public BaseDialog(Frame parent, String title) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        this._parent = parent;
        this._title = ucTitle(title);
    }

    public BaseDialog(JDialog parent, String title, String closeButtonText) {
        super(parent);
        this._parent = parent;
        this._title = ucTitle(title);
        this._closeButtonText = closeButtonText;
    }
    
    private String ucTitle(String s) {
        if (s != null && s.length() > 0 && Character.isLowerCase(s.charAt(0))) {
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        return s;
    }

    public void setCloseButtonImage(String imageName) {
        this._closeButtonImage = imageName;
    }

    public boolean prepareDialog() {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            iniDialogCommon(_title, _closeButtonText);
            iniDialog();
            iniDialogCommonFinish();
            EfaUtil.pack(this);
            _prepared = true;
            return true;
        } catch (Exception e) {
            Logger.log(e);
            return false;
        }
    }
    
    public void showMe() {
        showDialog();
    }

    public void setTitle(String title) {
        this._title = title;
        super.setTitle(title);
    }

    protected void enableWindowStackChecks(boolean enable) {
        this.doWindowStackChecks = enable;
    }

    public void showDialog() {
        Daten.iniSplashScreen(false);
        if (!_prepared && !prepareDialog()) {
            return;
        }
        Dialog.setDlgLocation(this, _parent);
        setModal(true);
        if (doWindowStackChecks) {
            Dialog.frameOpened(this);
        }
        if (focusItem != null) {
            focusItem.requestFocus();
        }
        preShowCallback();
        this.setVisible(true);
    }
    
    protected void preShowCallback() {
        // to be implemented in subclass (if needed)
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
        	Component focusedComp=this.getFocusOwner();
        	if (focusedComp instanceof JTextField) {
        		if (AutoCompletePopupWindow.isShowingAt((JTextField) focusedComp)) {
        			// do not hide window if autocomplete window is currently showing
        			return;
        		}
        	}
    		// otherwise, hide basewindow.
        	cancel(true);
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

    protected void iniDialogCommon(String title, String closeButtonText) throws Exception {
        helpTopic1 = getClass().getCanonicalName();
        helpTopic2 = getClass().getSuperclass().getCanonicalName();
        if (!helpTopic2.startsWith("de.nmichael.efa") || 
             helpTopic2.startsWith(BaseDialog.class.getCanonicalName())) {
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
        if (closeButtonText != null) {
            closeButton = new JButton();
            Mnemonics.setButton(this, closeButton, closeButtonText);
            if (closeButtonText.equals(International.getStringWithMnemonic("OK"))) {
                closeButton.setIcon(getIcon(IMAGE_ACCEPT));
            } else {
                closeButton.setIcon(getIcon(IMAGE_CLOSE));
            }
            if (_closeButtonImage != null) {
                closeButton.setIcon(getIcon(_closeButtonImage));
            }
            closeButton.setIconTextGap(10);
            closeButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    closeButton_actionPerformed(e);
                }
            });
            basePanel.add(closeButton, BorderLayout.SOUTH);
        }
    }

    protected void iniDialogCommonFinish() {
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
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(12); // faster scrolling with the mouse
    }

    protected abstract void iniDialog() throws Exception;

    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            if (cancel()) {
                // we don't need to call super.processWindowEvent(e) here!
                // otherwise we risk invoking cancel() a second time
                // super.processWindowEvent(e);
                return;
            } else {
                return;
            }
        }
        super.processWindowEvent(e);
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        cancel();
    }

    public boolean cancel() {
    	return internalCancel();
    }
    
    public boolean cancel(Boolean keyESCAction) {
    	return internalCancel();
    }
    
    private boolean internalCancel() {
        _inCancel = true;
        if (doWindowStackChecks) {
            Dialog.frameClosed(this);
        }
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
    	return ImagesAndIcons.getIcon(name); // use a central function instead of redundant code.
    }

    public static ImageIcon getScaledImage(String name) {
        try {
            if (name.indexOf("/") < 0) {
                name = Daten.IMAGEPATH + name;
            }
                name = name + "_12.png";
            if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_ICONS, "getIcon("+name+")");
            }
            return new ImageIcon(BaseDialog.class.getResource(name));
        } catch(Exception e) {
            if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_ICONS, "getIcon("+name+"): no icon found!");
            }
            Logger.logdebug(e);
            return null;
        }
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
