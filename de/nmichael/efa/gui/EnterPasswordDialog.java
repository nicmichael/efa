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

import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypePassword;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class EnterPasswordDialog extends BaseDialog {

    static final int MIN_PASSWORD_LENGTH = 8;
    
    private String KEYACTION_ENTER;
    private String reason;
    private boolean newPassword;
    private ItemTypePassword password;
    private ItemTypePassword passwordRepeat;
    private String resultPassword;

    public EnterPasswordDialog(Frame parent, String reason, boolean newPassword) {
        super(parent, International.getStringWithMnemonic("Paßworteingabe"), 
                International.getStringWithMnemonic("OK"));
        this.reason = reason;
        this.newPassword = newPassword;
    }

    public EnterPasswordDialog(JDialog parent, String reason, boolean newPassword) {
        super(parent, International.getStringWithMnemonic("Paßworteingabe"),
                International.getStringWithMnemonic("OK"));
        this.reason = reason;
        this.newPassword = newPassword;
    }

    protected void iniDialog() throws Exception {
        KEYACTION_ENTER      = addKeyAction("ENTER");

        mainPanel.setLayout(new GridBagLayout());

        JLabel reasonLabel = new JLabel();
        reasonLabel.setText(reason);
        reasonLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (reason != null && reason.length() > 0) {
            mainPanel.add(reasonLabel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
        }

        password = new ItemTypePassword("PASSWORD", "", IItemType.TYPE_PUBLIC, "",
                International.getStringWithMnemonic("Paßwort"));
        password.setMinCharacters( (newPassword ? MIN_PASSWORD_LENGTH : 1) );
        password.setNotAllowedCharacters(" ");
        password.setFieldSize(120, -1);
        password.displayOnGui(this, mainPanel, 0, 1);

        if (newPassword) {
            passwordRepeat = new ItemTypePassword("PASSWORDREPEAT", "", IItemType.TYPE_PUBLIC, "",
                    International.getStringWithMnemonic("Paßwort") +
                    " (" + International.getString("Wiederholung") + ")");
            passwordRepeat.setFieldSize(120, -1);
            passwordRepeat.displayOnGui(this, mainPanel, 0, 2);
        }

        
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void _keyAction(ActionEvent evt) {
        if (evt.getActionCommand().equals(KEYACTION_ENTER)) {
            closeButton_actionPerformed(evt);
        }
        super._keyAction(evt);
    }

    public void closeButton_actionPerformed(ActionEvent e) {
        password.getValueFromGui();
        if (passwordRepeat != null) {
            passwordRepeat.getValueFromGui();
        }

        if (!password.isValidInput()) {
            String s = password.getValue();
            if (s.length() == 0) {
                Dialog.error(International.getString("Kein Paßwort eingegeben!"));
            }
            if (newPassword) {
                if (s.length() < MIN_PASSWORD_LENGTH) {
                    Dialog.error(International.getMessage("Das Paßwort muß mindestens {n} Zeichen lang sein!", MIN_PASSWORD_LENGTH));
                }
                if (s.indexOf(" ") >= 0) {
                    Dialog.error(International.getString("Das Paßwort darf keine Leerzeichen enthalten!"));
                }

                // Test, ob mindestens drei Zeichengruppen vorkommen
                boolean klein = false;
                boolean gross = false;
                boolean ziffer = false;
                boolean sonst = false;
                for (char c = 'a'; c <= 'z'; c++) {
                    if (s.indexOf(String.valueOf(c)) >= 0) {
                        klein = true;
                    }
                }
                for (char c = 'A'; c <= 'Z'; c++) {
                    if (s.indexOf(String.valueOf(c)) >= 0) {
                        gross = true;
                    }
                }
                for (char c = '0'; c <= '9'; c++) {
                    if (s.indexOf(String.valueOf(c)) >= 0) {
                        ziffer = true;
                    }
                }
                for (int i = 0; i < s.length(); i++) {
                    if (!((s.charAt(i) >= 'a' && s.charAt(i) <= 'z')
                            || (s.charAt(i) >= 'A' && s.charAt(i) <= 'Z')
                            || (s.charAt(i) >= '0' && s.charAt(i) <= '9'))) {
                        sonst = true;
                    }
                }
                int merkmale = (klein ? 1 : 0) + (gross ? 1 : 0) + (ziffer ? 1 : 0) + (sonst ? 1 : 0);
                if (merkmale < 3) {
                    Dialog.error(International.getString("Das Paßwort muß mindestens Zeichen aus drei der insgesamt vier Zeichengruppen "
                            + "'Kleinbuchstaben', 'Großbuchstaben', 'Ziffern' und 'sonstige Zeichen' enthalten!"));
                    password.requestFocus();
                    return;
                }
            }
            password.requestFocus();
            return;
        }

        if (newPassword) {
            String s1 = password.getValue();
            String s2 = passwordRepeat.getValue();
            if (!s1.equals(s2)) {
                Dialog.error(International.getString("Die beiden eingegebenen Paßwörter sind verschieden."));
                passwordRepeat.requestFocus();
                return;
            }
        }
        resultPassword = password.getValue();
        super.closeButton_actionPerformed(e);
    }
    
    private String getResultPassword() {
        return resultPassword;
    }
    
    private static String enterPassword(EnterPasswordDialog dlg) {
        dlg.showDialog();
        return dlg.getResultPassword();
    }

    public static String enterPassword(BaseDialog parent, String reason, boolean newPassword) {
        EnterPasswordDialog dlg = new EnterPasswordDialog(parent, reason, newPassword);
        return enterPassword(dlg);
    }

    public static String enterPassword(BaseFrame parent, String reason, boolean newPassword) {
        EnterPasswordDialog dlg = new EnterPasswordDialog(parent, reason, newPassword);
        return enterPassword(dlg);
    }

    public static String enterPassword(Window parent, String reason, boolean newPassword) {
        EnterPasswordDialog dlg =
                (parent == null ?
                    new EnterPasswordDialog((JDialog)null, reason, newPassword) :
                (parent instanceof Frame ?
                    new EnterPasswordDialog((Frame)parent, reason, newPassword) :
                (parent instanceof JDialog ?
                    new EnterPasswordDialog((JDialog)parent, reason, newPassword) :
                    new EnterPasswordDialog((JDialog)null, reason, newPassword) )));
        return enterPassword(dlg);
    }
}
