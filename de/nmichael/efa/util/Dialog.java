/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.util;

import de.nmichael.efa.gui.BrowserDialog;
import de.nmichael.efa.core.*;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.io.File;

// @i18n complete
public class Dialog {

    public static final int INVALID = 0;
    public static final int YES = 1;
    public static final int NO = 2;
    public static final int CANCEL = 3;
    public static final int WRITE_IGNORE = 0;
    public static final int WRITE_REMOVE = 1;
    public static volatile boolean SUPPRESS_DIALOGS = false;
    public static volatile boolean IGNORE_WINDOW_STACK_CHECKS = false;
    public static Stack frameStack = null;
    public static TextField appletOut = null;
    public static JTable programOut = null;
    public static JTextArea programOutText = null;
    public static ProgressMonitor progress = null;
    public static Dimension screenSize = new Dimension(1024, 768); // nur Default-Values..... ;-)
    public static boolean tourRunning = false;
    private static int FONT_SIZE = -1;
    private static int ORG_FONT_SIZE = 12;
    private static boolean FONT_SIZE_CHANGED = false;
    private static int FONT_STYLE = -1;
    private static int ORG_FONT_STYLE = -1;
    private static int MAX_DIALOG_WIDTH = 200; // Number of Characters
    private static int MAX_DIALOG_HEIGHT = 50; // Number of Lines

    public static void initializeScreenSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (Daten.isOsLinux()) {
            // Workaround für Linux: Fenster verschwinden oder werden falsch positioniert, wenn die die
            // volle Bildschirmgröße haben
            Dialog.screenSize = new Dimension(screenSize.width - 1, screenSize.height - 1);
        } else {
            Dialog.screenSize = screenSize;
        }
        if (Daten.efaConfig == null) {
            return;
        }
        if (Daten.efaConfig.getValueScreenWidth() > 0) {
            Dialog.screenSize.width = Daten.efaConfig.getValueScreenWidth();
        }
        if (Daten.efaConfig.getValueScreenHeight() > 0) {
            Dialog.screenSize.height = Daten.efaConfig.getValueScreenHeight();
        }
        initializeMaxDialogSizes();
        if ((Daten.efaConfig.getValueMaxDialogWidth() > 0 || Daten.efaConfig.getValueMaxDialogHeight() > 0)) {
            Dialog.setMaxDialogSizes(Daten.efaConfig.getValueMaxDialogWidth(), Daten.efaConfig.getValueMaxDialogHeight());
        }
    }

    // max size that a dialog may have, depending on screen size
    public static Dimension getMaxSize(Dimension d) {
        Dimension newd = new Dimension(d);
        if (d.getWidth() > screenSize.getWidth() || d.getHeight() > screenSize.getHeight()) {
            newd.setSize((int) Math.min(d.getWidth(), screenSize.getWidth()), (int) Math.min(d.getHeight(), screenSize.getHeight()));
        }
        return newd;
    }

    public static Dimension getReducedMaxSize(Dimension d, int minusX, int minusY) {
        Dimension dim = getMaxSize(d);
        dim.width = Math.max(dim.width - minusX, 1);
        dim.height = Math.max(dim.height - minusY, 1);
        return dim;
    }
    
    public static void setMaxDialogSizes(int width, int height) {
        if (width > 0) {
            MAX_DIALOG_WIDTH = width;
        }
        if (height > 0) {
            MAX_DIALOG_HEIGHT = height;
        }
    }

    public static void initializeMaxDialogSizes() {
        if (Dialog.screenSize == null) {
            return; // should never happen
        }
        int fontSize = (FONT_SIZE > 0 ? FONT_SIZE : 12);
        MAX_DIALOG_WIDTH = (int) (Dialog.screenSize.width / (fontSize * 0.7));
        MAX_DIALOG_HEIGHT = (int) (Dialog.screenSize.height / (fontSize * 1.6)) - 5;
    }

    public static float getScalingFactor() {
        int fontSize = (FONT_SIZE > 0 ? FONT_SIZE : 12);
        return (((float) fontSize) / 12f);
    }

    public static int DateiErstellen(String dat) {
        switch (Dialog.yesNoDialog(
                International.getString("Fehler"),
                LogString.fileNotFound(dat, International.getString("Datei")) + "\n"
                + International.getString("Soll die Datei neu erstellt werden?"))) {
            case Dialog.YES:
                return YES;
            case Dialog.NO:
                return NO;
            default:
                return INVALID;
        }
    }

    public static void error(String s) {
        if (Daten.isGuiAppl() && !SUPPRESS_DIALOGS) {
            Dialog.infoDialog(International.getString("Fehler"), s);
        } else {
            System.out.println("ERROR" + ": " + s);
        }
    }

    public static void meldung(String title, String s) {
        if (Daten.isGuiAppl() && !SUPPRESS_DIALOGS) {
            Dialog.infoDialog(title, s);
        } else {
            System.out.println("INFO" + ": " + s);
        }
    }

    public static void exceptionError(String error, String stacktrace) {
        if (!Daten.isGuiAppl()) {
            return;
        }
        int px = -1, py = -1;
        Window w = frameCurrent();
        ExceptionFrame dlg;
        if (w != null && new JFrame().getClass().isAssignableFrom(w.getClass())) {
            dlg = new ExceptionFrame((JFrame) w, error, stacktrace);
        } else if (w != null && new JDialog().getClass().isAssignableFrom(w.getClass())) {
            dlg = new ExceptionFrame((JDialog) w, error, stacktrace);
        } else {
            dlg = new ExceptionFrame(error, stacktrace);
        }
        Dimension dlgSize = dlg.getPreferredSize();
        int width = (int) dlgSize.getWidth() + 50;
        int height = (int) dlgSize.getHeight() + 50;
        dlg.setSize(width, height);
        Dialog.setDlgLocation(dlg);
        dlg.setModal(false);
        dlg.show();
        dlg.toFront();

    }

    public static String chopDialogString(String s) {
        if (s == null) {
            return s;
        }
        if (MAX_DIALOG_WIDTH < 50) {
            MAX_DIALOG_WIDTH = 50;
        }
        if (MAX_DIALOG_HEIGHT < 10) {
            MAX_DIALOG_HEIGHT = 10;
        }

        try {
            int lines = 1;
            int chars = 0;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) != '\n') {
                    if (++chars > MAX_DIALOG_WIDTH) {
                        int splitAt = -1;
                        for (int j = i; j > i - ((int) MAX_DIALOG_WIDTH / 2); j--) {
                            if (s.charAt(j) == ' ') {
                                splitAt = j;
                                break;
                            }
                        }
                        if (splitAt < 0) {
                            splitAt = i;
                        }
                        s = s.substring(0, splitAt) + "\n"
                                + s.substring(splitAt + (s.charAt(splitAt) == ' ' ? 1 : 0));
                        i = splitAt;
                        lines++;
                        chars = 0;
                    }
                } else {
                    lines++;
                    chars = 0;
                }
            }
            if (lines > MAX_DIALOG_HEIGHT) {
                Vector _s = EfaUtil.split(s, '\n');
                s = "";
                for (int i = 0; i < (int) (MAX_DIALOG_HEIGHT / 2); i++) {
                    s += (String) _s.get(i) + "\n";
                }
                s += "...\n";
                int remaining = MAX_DIALOG_HEIGHT - ((int) (MAX_DIALOG_HEIGHT / 2) + 1);
                for (int i = _s.size() - remaining; i < _s.size(); i++) {
                    s += (String) _s.get(i) + (i + 1 < _s.size() ? "\n" : "");
                }
            }
        } catch (Exception e) {
        }
        return s;
    }

    public static void meldung(String s) {
        Dialog.infoDialog(International.getString("Information"), s);
    }

    private static void prepareWindow(Window frame) {
        Daten.iniSplashScreen(false);
        if (frame != null && !frame.isEnabled()) {
            frame.setEnabled(true);
        }
    }

    public static int yesNoDialog(String title, String s) {
        Window frame = frameCurrent();
        prepareWindow(frame);
        if (JOptionPane.showConfirmDialog(frame, chopDialogString(s), title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            return YES;
        } else {
            return NO;
        }
    }

    public static int yesNoCancelDialog(String title, String s) {
        Window frame = frameCurrent();
        prepareWindow(frame);
        switch (JOptionPane.showConfirmDialog(frame, chopDialogString(s), title, JOptionPane.YES_NO_CANCEL_OPTION)) {
            case JOptionPane.YES_OPTION:
                return YES;
            case JOptionPane.NO_OPTION:
                return NO;
            default:
                return CANCEL;
        }
    }

    public static int auswahlDialog(String title, String s, String[] options) {
        Window frame = frameCurrent();
        prepareWindow(frame);
        return JOptionPane.showOptionDialog(frame, chopDialogString(s), title, 0, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
    }

    public static int auswahlDialog(String title, String s, String option1, String option2, String option3) {
        Object[] auswahl = new String[(option3 != null ? 3 : 2)];
        auswahl[0] = option1;
        auswahl[1] = option2;
        if (option3 != null) {
            auswahl[2] = option3;
        }
        Window frame = frameCurrent();
        prepareWindow(frame);
        return JOptionPane.showOptionDialog(frame, chopDialogString(s), title, 0, JOptionPane.QUESTION_MESSAGE, null, auswahl, option1);
    }

    public static int auswahlDialog(String title, String s, String option1, String option2, String option3, String option4) {
        Object[] auswahl = new String[(option4 != null ? 4 : 3)];
        auswahl[0] = option1;
        auswahl[1] = option2;
        auswahl[2] = option3;
        if (option4 != null) {
            auswahl[3] = option4;
        }
        Window frame = frameCurrent();
        prepareWindow(frame);
        return JOptionPane.showOptionDialog(frame, chopDialogString(s), title, 0, JOptionPane.QUESTION_MESSAGE, null, auswahl, option1);
    }

    public static int auswahlDialog(String title, String s, String option1, String option2, String option3, String option4, String option5) {
        Object[] auswahl = new String[5];
        auswahl[0] = option1;
        auswahl[1] = option2;
        auswahl[2] = option3;
        auswahl[3] = option4;
        auswahl[4] = option5;
        Window frame = frameCurrent();
        prepareWindow(frame);
        return JOptionPane.showOptionDialog(frame, chopDialogString(s), title, 0, JOptionPane.QUESTION_MESSAGE, null, auswahl, option1);
    }

    public static int auswahlDialog(String title, String s, String option1, String option2) {
        return auswahlDialog(title, s, option1, option2, International.getString("Abbruch"));
    }

    public static int auswahlDialog(String title, String s, String option1, String option2, boolean abbrButton) {
        return auswahlDialog(title, s, option1, option2, (abbrButton ? International.getString("Abbruch") : null));
    }

    public static boolean okAbbrDialog(String title, String s) {
        Window frame = frameCurrent();
        prepareWindow(frame);
        return JOptionPane.showConfirmDialog(frame, chopDialogString(s), title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    public static void infoDialog(String title, String s) {
        if (Daten.isGuiAppl() && !SUPPRESS_DIALOGS) {
            Window frame = frameCurrent();
            prepareWindow(frame);
            JOptionPane.showConfirmDialog(frame, chopDialogString(s), title, -1);
        } else {
            System.out.println("INFO" + ": " + s);
        }
    }

    public static void infoDialog(String s) {
        Dialog.infoDialog(International.getString("Information"), s);
    }

    public static String inputDialog(String title, String s) {
        Window frame = frameCurrent();
        prepareWindow(frame);
        return JOptionPane.showInputDialog(frame, chopDialogString(s), title, JOptionPane.PLAIN_MESSAGE);
    }

    public static String inputDialog(String title, String s, String vorbelegung) {
        Window frame = frameCurrent();
        prepareWindow(frame);
        return (String) JOptionPane.showInputDialog(frame, chopDialogString(s), title, JOptionPane.PLAIN_MESSAGE, null, null, vorbelegung);
    }


    // muß von jedem Frame gerufen werden, das geöffnet wird!!
    public static void frameOpened(Window w) {
        if (frameStack == null) {
            frameStack = new Stack();
        }
        frameStack.push(w);
        if (Daten.applID == Daten.APPL_EFABH
                && Daten.efaConfig != null && Daten.efaConfig.getValueEfaDirekt_immerImVordergrund()) {
            try {
                de.nmichael.efa.java15.Java15.setAlwaysOnTop(w, true);
            } catch (UnsupportedClassVersionError e) {
                EfaUtil.foo();
            } catch (NoClassDefFoundError e) {
                EfaUtil.foo();
            }
        }
        if (Logger.isDebugLogging() && Logger.isTraceOn(Logger.TT_GUI)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_WINDOWS, "Dialog.frameOpened(" + w.getClass().getCanonicalName() + ")");
        }
    }

    // muß von jedem Frame gerufen werden, das geschlossen wird!!
    public static void frameClosed(Window w) {
        Mnemonics.clearCache(w);
        if (frameStack == null) {
            return;
        }
        if (Logger.isDebugLogging() && Logger.isTraceOn(Logger.TT_GUI)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_WINDOWS, "Dialog.frameClosed(" + w.getClass().getCanonicalName() + ")");
        }
        if (frameStack.isEmpty()) {
            if (!IGNORE_WINDOW_STACK_CHECKS) {
                Logger.log(Logger.WARNING, Logger.MSG_ERR_WINDOWSTACK,
                        "Stack Inconsistency: closed Window: "
                        + w.getClass().toString() + " but stack was empty.");
                if (Daten.watchWindowStack) {
                    Thread.dumpStack();
                    (new Exception("Watch Stack Exception")).printStackTrace();
                }
            }
            return;
        }
        Window wtop = (Window) frameStack.peek();
        if (wtop != w) {
            if (!IGNORE_WINDOW_STACK_CHECKS) {
                String s = "";
                try {
                    for (int i = 0; i < frameStack.size(); i++) {
                        s += (s.length() > 0 ? "; " : "") + frameStack.elementAt(i).getClass().toString();
                    }
                } catch (Exception e) {
                    EfaUtil.foo();
                }
                Logger.log(Logger.WARNING, Logger.MSG_ERR_WINDOWSTACK,
                        "Stack Inconsistency: closed Window: "
                        + w.getClass().toString() + " but top of stack is: "
                        + wtop.getClass().toString() + " (stack: " + s + ")");
                if (Daten.watchWindowStack) {
                    Thread.dumpStack();
                    (new Exception("Watch Stack Exception")).printStackTrace();
                }
            }
        } else {
            frameStack.pop();
        }
    }

    // liefert das aktuell geöffnete Frame
    public static Window frameCurrent() {
        if (frameStack == null || frameStack.isEmpty()) {
            return null;
        }
        return (Window) frameStack.peek();
    }

    // (this,"Fahrtenbuchdatei erstellen","efa Fahrtenbuch (*.efb)","efb",Daten.fahrtenbuch.getFileName(),true);
    public static String dateiDialog(Window frame, String titel, String typen, String extension, String startdir, boolean save) {
        return dateiDialog(frame, titel, typen, extension, startdir, null, null, save, false);
    }

    public static String dateiDialog(Window frame, String titel, String typen, String extension, String startdir, String selectedfile, String buttontxt, boolean save, boolean dirsOnly) {
        JFileChooser dlg;

        try {

            if (startdir != null) {
                dlg = new JFileChooser(startdir);
            } else {
                dlg = new JFileChooser();
            }

            if (typen != null && extension != null) {
                int wo;
                if ((wo = extension.indexOf("|")) >= 0) {
                    String ext1, ext2;
                    ext1 = extension.substring(0, wo);
                    ext2 = extension.substring(wo + 1, extension.length());
                    dlg.setFileFilter((javax.swing.filechooser.FileFilter) new EfaFileFilter(typen, ext1, ext2));
                } else {
                    dlg.setFileFilter((javax.swing.filechooser.FileFilter) new EfaFileFilter(typen, extension));
                }

            }

            if (titel != null) {
                dlg.setDialogTitle(titel);
            }

            if (selectedfile != null) {
                dlg.setSelectedFile(new File(selectedfile));
            }
            if (buttontxt != null) {
                dlg.setApproveButtonText(buttontxt);
            }
            if (dirsOnly) {
                dlg.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            }

            int ret;
            if (save) {
                ret = dlg.showSaveDialog(frame);
            } else {
                ret = dlg.showOpenDialog(frame);
            }

            if (ret == JFileChooser.APPROVE_OPTION) {
                return dlg.getSelectedFile().toString();
            } else {
                return null;
            }
        } catch (Exception e) {
            String input =
                    (String) JOptionPane.showInputDialog(frame,
                    International.getMessage("Bitte gib einen Dateinamen für '{types}' ein", typen) + ":",
                    titel, JOptionPane.QUESTION_MESSAGE,
                    null, null, startdir);
            if (input != null && input.trim().length() == 0) {
                input = null;
            }
            return input;
        }
    }

    // Methoden zum Setzen der Position eines neuen JDialogs
    public static void setDlgLocation(JDialog dlg, Frame parent) {
        dlg.setLocation(getLocation(dlg.getSize(), (parent != null ? parent.getSize() : null), (parent != null ? parent.getLocation() : null)));
    }

    public static void setDlgLocation(JDialog dlg, Window parent) {
        dlg.setLocation(getLocation(dlg.getSize(), (parent != null ? parent.getSize() : null), (parent != null ? parent.getLocation() : null)));
    }

    public static void setDlgLocation(JDialog dlg) {
        dlg.setLocation(getLocation(dlg.getSize(), null, null));
    }

    public static void setDlgLocation(JFrame dlg) {
        dlg.setLocation(getLocation(dlg.getSize(), null, null));
    }

    public static Point getLocation(Dimension dlgSize, Dimension parentSize, Point loc) {
        int x, y;

        // fix dlgSize, if necessary
        if (dlgSize.height > screenSize.height) {
            dlgSize.height = screenSize.height;
        }
        if (dlgSize.width > screenSize.width) {
            dlgSize.width = screenSize.width;
        }

        // calculate position
        if (parentSize != null && loc != null && Daten.efaConfig != null
                && !Daten.efaConfig.getValueFensterZentriert()) {
            // calculate position based on parent dialog
            if (parentSize.width == 0) {
                parentSize.width = screenSize.width;
            }
            if (parentSize.height == 0) {
                parentSize.height = screenSize.height;
            }

            // add offset
            x = (parentSize.width - dlgSize.width) / 2 + loc.x;
            y = (parentSize.height - dlgSize.height) / 2 + loc.y;
        } else {
            // calculate position based on screen size
            x = (screenSize.width - dlgSize.width) / 2;
            y = (screenSize.height - dlgSize.height) / 2;

            // add offset
            if (Daten.efaConfig != null && Daten.efaConfig.getValueWindowXOffset() > 0) {
                x += Daten.efaConfig.getValueWindowXOffset();
            }
            if (Daten.efaConfig != null && Daten.efaConfig.getValueWindowYOffset() > 0) {
                y += Daten.efaConfig.getValueWindowYOffset();
            }
        }

        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }

        return new Point(x, y);
    }

    public static UIDefaults getUiDefaults() {
        try {
            String laf = UIManager.getLookAndFeel().getClass().getCanonicalName();
            if (laf.endsWith("NimbusLookAndFeel")) {
                return UIManager.getLookAndFeelDefaults();
            } else {
                return UIManager.getDefaults();
            }
        } catch (Exception eignore) {
            return null;
        }
    }

    public static void setFontSize(UIDefaults uid, String font, int size, int style) {
        Font orgFont = uid.getFont(font);
        if (orgFont == null) {
            Logger.log(Logger.WARNING, Logger.MSG_WARN_FONTDOESNOTEXIST,
                    International.getMessage("Schriftart {font} exisitert nicht; ihre Größe kann nicht geändert werden!", font));
            return;
        }
        if (!FONT_SIZE_CHANGED) {
            ORG_FONT_SIZE = orgFont.getSize();
            ORG_FONT_STYLE = orgFont.getStyle();
            if (style == -1) {
                ORG_FONT_STYLE = -1; // Bugfix: Weil manche Schriften fett sind und andere nicht ...
            }
        }
        Font newFont;
        if (style == -1) {
            newFont = orgFont.deriveFont((float) size);
        } else if (size <= 0) {
            newFont = orgFont.deriveFont(style);
        } else {
            newFont = orgFont.deriveFont(style, (float) size);
        }
        uid.put(font, newFont);
        FONT_SIZE_CHANGED = true;
    }

    public static void setGlobalFontSize(int size, int style) {
        FONT_SIZE = size;
        FONT_STYLE = style;

        UIDefaults uid = getUiDefaults();

        java.util.Enumeration keys = uid.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = uid.get(key);
            String font = (key == null ? null : key.toString());
            if (font != null
                    && (font.endsWith(".font")
                    || (font.startsWith("OptionPane") && font.endsWith("Font")))) {
                if (!font.equals("TableHeader.font") && !font.equals("Table.font")) {
                    setFontSize(uid, font, size, style);
                }
            }
        }
        initializeMaxDialogSizes();

    }

    public static void setGlobalFontSize(int size, String style) {
        int _style = -1;
        if (style.equals(EfaConfig.FONT_PLAIN)) {
            _style = Font.PLAIN;
        }
        if (style.equals(EfaConfig.FONT_BOLD)) {
            _style = Font.BOLD;
        }
        setGlobalFontSize(size, _style);
    }

    public static void setPreferredSize(JComponent comp, int width, int height) {
        setPreferredSize(comp, width, height, 1);
    }

    public static void setPreferredSize(JComponent comp, int width, int height, float corr) {
        float factor = 1.0f;

        // calculate sizing factor depending on font size
        if (FONT_SIZE > 0) {
            // scale everything based on 12pt font size (default)
            factor = ((float) FONT_SIZE) / 12.0f;
            factor = (factor - 1.0f) * corr + 1.0f;
            width = (int) (((float) width) * factor);
            height = (int) (((float) height) * factor);
        }

        Insets insets = comp.getInsets();

        // workaround for some special components (i.e. JComboBox in NimbusLookAndFeel)
        if (insets != null && (insets.top < 2 || insets.bottom < 2) && comp instanceof javax.swing.JComboBox) {
            insets.top = 6;
            insets.bottom = 6;
        }

        // calculate sizing factor depending of look&feel
        if (insets != null && (insets.top > 2 || insets.bottom > 2)) {
            // scale everything based on 2pt insets (based on MetalLookAndFeel)
            int add = ((insets.top + insets.bottom) - 4) / 2;
            if (add < 0) {
                add = 0;
            }
            height += add;
        }

        comp.setPreferredSize(new Dimension(width, height));
    }

    public static int getFontSize() {
        return FONT_SIZE;
    }

    public static int getFontStyle() {
        return FONT_STYLE;
    }

    public static boolean isFontSizeChanged() {
        return FONT_SIZE_CHANGED;
    }

    public static int getDefaultFontSize() {
        return ORG_FONT_SIZE;
    }

    public static int getDefaultFontStyle() {
        return ORG_FONT_STYLE;
    }
}
