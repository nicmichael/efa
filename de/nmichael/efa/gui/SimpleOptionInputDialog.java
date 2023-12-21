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

import de.nmichael.efa.util.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.gui.util.EfaMouseListener;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

// @i18n complete
public class SimpleOptionInputDialog extends SimpleInputDialog implements IItemListener {

	public static final int OPTION_OK = 0;
    public static final int OPTION_CANCEL = 1;
    public static final int OPTION_NONE = 2;


    private String[] optionButtonText;
    private int[] optionButtonAction;
    private String[] optionButtonIcons;
    private Hashtable<JButton,Integer> buttonActions = new Hashtable<JButton,Integer>();

    public SimpleOptionInputDialog(Frame parent, String title, IItemType item,
            String[] optionButtonText, int[] optionButtonAction,
            String[] optionButtonIcons) {
        super(parent, title, new IItemType[] { item });
        ini(item, optionButtonText, optionButtonAction, optionButtonIcons);
    }

    public SimpleOptionInputDialog(JDialog parent, String title, IItemType item,
            String[] optionButtonText, int[] optionButtonAction,
            String[] optionButtonIcons) {
        super(parent, title, new IItemType[] { item });
        ini(item, optionButtonText, optionButtonAction, optionButtonIcons);
    }

    private void ini(IItemType item, String[] optionButtonText, int[] optionButtonAction,
            String[] optionButtonIcons) {
        this.optionButtonText = optionButtonText;
        this.optionButtonAction = optionButtonAction;
        if (optionButtonIcons == null) {
            optionButtonIcons = new String[optionButtonAction.length];
            for (int i=0; i<optionButtonAction.length; i++) {
                switch(optionButtonAction[i]) {
                    case OPTION_OK:
                        optionButtonIcons[i] = IMAGE_ACCEPT;
                        break;
                    case OPTION_CANCEL:
                        optionButtonIcons[i] = IMAGE_CANCEL;
                        break;
                }
            }
        }
        this.optionButtonIcons = optionButtonIcons;
        this._closeButtonText = null;
         item.registerItemListener(this);

    }


    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        super.iniDialog();
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        for (int i=0; i<optionButtonText.length; i++) {
            JButton button = new JButton();
            button.setText(optionButtonText[i]);
            button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    optionButtonAction(e);
                }
            });
            buttonPanel.add(button);
            buttonActions.put(button, optionButtonAction[i]);
            
            // choose the last button in the list with OPTION_OK as default button 
            if (optionButtonAction[i]==OPTION_OK) {
            	this.getRootPane().setDefaultButton(button);
            }
            
            if (optionButtonIcons != null && optionButtonIcons.length > i &&
                optionButtonIcons[i] != null) {
                button.setIcon(getIcon(optionButtonIcons[i]));
            }
        }
        basePanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void optionButtonAction(ActionEvent e) {
        if (e != null) {
            try {
                int action = buttonActions.get(e.getSource());
                switch(action) {
                    case OPTION_OK:
                        closeButton_actionPerformed(e);
                        return;
                    case OPTION_CANCEL:
                        cancel();
                        return;
                }
            } catch(Exception eignore) {
                Logger.logdebug(eignore);
            }
        }
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType == super.items[0] && event != null &&
            event instanceof ActionEvent) {
            if (((ActionEvent)event).getActionCommand().equals(EfaMouseListener.EVENT_MOUSECLICKED_2x)) {
                closeButton_actionPerformed((ActionEvent)event);
            }
        }
    }


    public static boolean showOptionInputDialog(JDialog parent, String title, IItemType item,
            String[] optionButtonText, int[] optionButtonAction, String[] optionButtonIcons) {
        SimpleOptionInputDialog dlg = new SimpleOptionInputDialog(parent, title, item,
                optionButtonText, optionButtonAction, optionButtonIcons);
        dlg.showDialog();
        return dlg.resultSuccess;
    }

    public static boolean showOptionInputDialog(JFrame parent, String title, IItemType item,
            String[] optionButtonText, int[] optionButtonAction, String[] optionButtonIcons) {
        SimpleOptionInputDialog dlg = new SimpleOptionInputDialog(parent, title, item,
                optionButtonText, optionButtonAction, optionButtonIcons);
        dlg.showDialog();
        return dlg.resultSuccess;
    }

    public static boolean showOptionInputDialog(Window parent, String title, IItemType item,
            String[] optionButtonText, int[] optionButtonAction, String[] optionButtonIcons) {
        if (parent instanceof JDialog) {
            return showOptionInputDialog((JDialog)parent, title, item,
                optionButtonText, optionButtonAction, optionButtonIcons);
        } else {
            return showOptionInputDialog((JFrame)parent, title, item,
                optionButtonText, optionButtonAction, optionButtonIcons);
        }
    }

}
