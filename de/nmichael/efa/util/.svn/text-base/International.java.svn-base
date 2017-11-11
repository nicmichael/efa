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

import de.nmichael.efa.*;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.gui.SimpleInputDialog;
import java.util.*;
import java.io.*;
import java.text.*;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

// @i18n complete

public class International {

    private static boolean MARK_MISSING_KEYS = false; // default for production: false
    private static boolean LOG_MISSING_KEYS = false; // default for production: false (requires "-debug" flag for debug logging)
    private static boolean STACKTRACE_MISSING_KEYS = false; // default for production: false
    private static boolean SHOW_KEY_INSTEAD_OF_TRANSLATION = false;  // default for production: false

    public static final String BUNDLE_NAME = "efa";
    private static Locale locale = null;
    private static ResourceBundle bundle = null;
    private static MessageFormat msgFormat = null;
    private static NumberFormat numberFormat = null;
    private static char decimalSeparator = '.';
    private static boolean initializationFailed = false;

    private static void initializeData() {
        try {
            if (Daten.efaBaseConfig != null && Daten.efaBaseConfig.language != null &&
                Daten.efaBaseConfig.language.length() > 0) {
                if (Logger.isTraceOn(Logger.TT_INTERNATIONALIZATION)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_INTERNATIONAL_DEBUG, "Using configured language setting: "+Daten.efaBaseConfig.language);
                }
                locale = new Locale(Daten.efaBaseConfig.language);
            } else {
                if (Logger.isTraceOn(Logger.TT_INTERNATIONALIZATION)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_INTERNATIONAL_DEBUG, "Using default language.");
                }
                locale = Locale.getDefault();
            }
            if (Logger.isTraceOn(Logger.TT_INTERNATIONALIZATION)) {
                Logger.log(Logger.DEBUG, Logger.MSG_INTERNATIONAL_DEBUG, "Language is now: "+locale.getDisplayName());
            }

            bundle = ResourceBundle.getBundle(BUNDLE_NAME,locale);
            numberFormat = NumberFormat.getNumberInstance(locale);
            msgFormat = new MessageFormat("",locale);
            decimalSeparator = ((DecimalFormat)numberFormat).getDecimalFormatSymbols().getDecimalSeparator();
            Daten.EFA_SHORTNAME = "efa";
            Daten.EFA_LONGNAME = Daten.EFA_SHORTNAME + " - " + International.getString("elektronisches Fahrtenbuch");
            Daten.EFA_ONLINE = "efaOnline";
            Daten.EFA_BASE = "efa-" + International.getString("Basis");
            Daten.EFA_BOATHOUSE = "efa-" + International.getString("Bootshaus");
            Daten.EFA_CLI = "efaCLI";
            Daten.EFA_LIVE = "efaLive";
            Daten.EFA_WETT = "efaWett";
            Daten.EFA_REMOTE = "efaRemote";
            EfaTypes.TEXT_UNKNOWN = International.getString("unbekannt");
            JOptionPane.setDefaultLocale(locale);
        } catch(Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_INTERNATIONAL_FAILEDSETUP, "Failed to set up internationalization: "+e.toString()); // no need for translation
            initializationFailed = true;
        }
    }

    public static void initialize() {
        if (initializationFailed) {
            return;
        }

        if (Logger.isTraceOn(Logger.TT_INTERNATIONALIZATION)) {
            Logger.log(Logger.DEBUG, Logger.MSG_INTERNATIONAL_DEBUG, "Initializing Language Support ...");
            Logger.log(Logger.DEBUG, Logger.MSG_INTERNATIONAL_DEBUG, Daten.getCurrentStack());
        }
        initializeData();

        try {
            if (Daten.efaBaseConfig != null && Daten.efaBaseConfig.language == null) {
                if (Logger.isTraceOn(Logger.TT_INTERNATIONALIZATION)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_INTERNATIONAL_DEBUG, "No preferred Language configured!");
                }
                String[] bundles = getLanguageBundles();
                if (Daten.isGuiAppl() && bundles != null && bundles.length > 0) {
                    String[] listDisplay = new String[bundles.length + 1];
                    String[] listValues = new String[bundles.length + 1];
                    listDisplay[0] = International.getString("Default"); // must be in English (in case user's language is not supported)
                    listValues[0] = "";
                    String preselect = "";
                    for (int i=0; i<bundles.length; i++) {
                        Locale loc = new Locale(bundles[i]);
                        listDisplay[i+1] = loc.getDisplayName();
                        listValues[i+1] = bundles[i];
                        if (Locale.getDefault().getLanguage().equals(loc.getLanguage())) {
                            preselect = bundles[i];
                        }
                    }
                    ItemTypeStringList languages = new ItemTypeStringList("LANG", preselect,
                            listValues, listDisplay,
                            IItemType.TYPE_PUBLIC, "",
                            International.getString("Please select your language")
                            );

                    String lang = "";
                    if (SimpleInputDialog.showInputDialog((JFrame)null,
                            International.getString("Select Language"), // must be in English (in case user's language is not supported)
                            languages)) {
                        languages.getValueFromGui();
                        lang = languages.getValue();
                        if (Logger.isTraceOn(Logger.TT_INTERNATIONALIZATION)) {
                            Logger.log(Logger.DEBUG, Logger.MSG_INTERNATIONAL_DEBUG, "Selected Language: " +
                                    (lang.length() > 0 ? lang : "<default>"));
                        }
                    } else {
                        if (Logger.isTraceOn(Logger.TT_INTERNATIONALIZATION)) {
                            Logger.log(Logger.DEBUG, Logger.MSG_INTERNATIONAL_DEBUG, "Selected Language: <none>");
                        }
                    }
                    
                    if (lang != null && Daten.efaBaseConfig != null) {
                        Daten.efaBaseConfig.language = lang;
                        Daten.efaBaseConfig.writeFile();
                        initializeData();
                    }

                } else {
                    if (Daten.efaProgramDirectory != null) {
                        Logger.log(Logger.WARNING, Logger.MSG_CORE_LANGUAGESUPPORT,
                                   "No Language Bundles found in "+Daten.efaProgramDirectory+"!");
                    } else {
                        Logger.log(Logger.INFO, Logger.MSG_CORE_LANGUAGESUPPORT,
                                   "Currently no Language Bundles available at this stage of initialization.");
                    }
                }
            }

        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_CORE_LANGUAGESUPPORT,
                    "Failed to determine available languages: "+e.toString());
            e.printStackTrace(); // this stack trace should stay in here (we don't have a logger yet,
                                 // so we don't have to worry about an ExceptionFrame here)
        }
    }

    public static String getBundleFilename(String languageId) {
        return Daten.efaProgramDirectory + BUNDLE_NAME + "_" + languageId + ".properties";
    }

    public static String[] getLanguageBundles() {
        if (Daten.efaProgramDirectory == null) {
            // not yet initialized
            // This can happen when we try to translate something before Daten.initialize() calls iniLanguageSupport()
            return null;
        }
        if (Logger.isTraceOn(Logger.TT_INTERNATIONALIZATION)) {
            Logger.log(Logger.DEBUG, Logger.MSG_INTERNATIONAL_DEBUG, "Available Languages:");
        }
        File dir = new File(Daten.efaProgramDirectory);
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        ArrayList<String> bundles = new ArrayList<String>();
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(BUNDLE_NAME + "_") && name.endsWith(".properties")) {
                int pos = name.indexOf(".properties");
                String lang = name.substring(BUNDLE_NAME.length() + 1, pos);
                if (lang.length() > 0) {
                    if (Logger.isTraceOn(Logger.TT_INTERNATIONALIZATION)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_INTERNATIONAL_DEBUG, "    " + lang);
                    }
                    bundles.add(lang);
                }
            }
        }
        String[] ba = bundles.toArray(new String[0]);
        Arrays.sort(ba);
        return ba;
    }

    private static String getArrayStrings(Object[] a) {
        String s = "";
        for (int i=1; a != null && i<a.length; i++) {
            if (a[i] != null) {
                s += (s.length() > 0 ? ", " : "") + a[i].toString();
            }
        }
        return s;
    }

    public static String makeKey(String s) {
        if (s == null) return "null";
        
        // To be extended by further characters? And maybe some performance optimization needed.
        char[] key = s.toCharArray();
        for (int i=0; i<key.length; i++) {
            switch(key[i]) {
                case ' ':
                case '=':
                case ':':
                case '#':
                case '\'':
                case '\\':
                case '\n':
                    key[i] = '_';
                    break;
            }
        }
        return new String(key);
    }

    private static String getString(String s, boolean defaultIfNotFound, boolean includingMnemonics, boolean messageString, ResourceBundle useBundle) {
        if (bundle == null) {
            initialize();
        }
        try {
            String key = makeKey(s);
            if (SHOW_KEY_INSTEAD_OF_TRANSLATION) {
                return (MARK_MISSING_KEYS ? "#"+key+"#" : key);
            } else {
                String t;
                if (useBundle == null) {
                    t = bundle.getString(key);
                } else {
                    t = useBundle.getString(key);
                }
                if (!includingMnemonics) {
                    if (Mnemonics.containsMnemonics(t)) {
                        t = Mnemonics.stripMnemonics(t);
                    }
                }
                return t;
            }
        } catch(Exception e) {
            if (defaultIfNotFound) {
                if (LOG_MISSING_KEYS) {
                    Logger.log(Logger.WARNING, Logger.MSG_INTERNATIONAL_MISSINGKEY, "Missing Key: "+makeKey(s)); // no need for translation!
                }
                if (STACKTRACE_MISSING_KEYS) {
                    EfaErrorPrintStream.ignoreExceptions = true;
                    e.printStackTrace();
                    EfaErrorPrintStream.ignoreExceptions = false;

                }
                if (messageString) {
                    // This is a message that has not been translated: --> substitute {variable} by {index}
                    int p1,p2;
                    int i=1;
                    while ( (p1 = s.indexOf("{")) >= 0 && (p2 = s.indexOf("}")) >= 0) {
                        if (p1 >= p2) {
                            break;
                        }
                        if (p1 > 0 && s.charAt(p1-1) == '\'' && p2+1 < s.length() && s.charAt(p2+1) == '\'') {
                            s = s.substring(0, p1) + "'@@[@@" + i++ + "@@]@@'" + s.substring(p2+1); // substitute '{varname}' by ''{1}''
                        } else {
                            s = s.substring(0, p1) + "@@[@@" + i++ + "@@]@@" + s.substring(p2+1); // substitute {varname} by {1}
                        }
                    }
                    s = EfaUtil.replace(s, "@@[@@", "{", true);
                    s = EfaUtil.replace(s, "@@]@@", "}", true);
                }
                return (MARK_MISSING_KEYS ? "#"+s+"#" : s);
            } else {
                return null;
            }
        }
    }

    /**
     * Retrieves an internationalized string.
     * This method strips any mnemonics from the translated string before returning it.
     * @param s key to be retrieved
     * @return translated string
     */
    public static String getString(String s) {
        return getString(s, true, false, false, null);
    }
    public static String getString(String s, ResourceBundle bundle) {
        return getString(s, true, false, false, bundle);
    }
    public static String getStringWithoutAnyEscaping(String s) {
        return EfaUtil.replace(getString(s), "&&", "&", true);
    }

    public static String onlyFor(String s, String lang) {
        if (getLanguageID().startsWith(lang)) {
            return s;
        }
//        return "<" + getMessage("only [{language}]",lang) + ">";
        return International.getMessage("nur für [{language}]",lang);
    }

    /**
     * Retrieves an internationalized variant of a string.
     * This method is intended for situations where the same key may be translated in different ways
     * depending on the context it is being used in. For such situations, an additional variant discriminator
     * may be specified to distinguish (otheriwse same) keys in different contexts. If no translation for
     * this key including its variant discriminator is found, this method will attempt to return a translation for
     * the key without the variant discriminator.
     * This method strips any mnemonics from the translated string before returning it.
     *
     * @param s key to be retrieved
     * @param variant discriminator to specify the context of the key
     * @return the translation for "s___variant", if existing; translation for "s" otherwise.
     */
    public static String getString(String s, String variant) {
        String t = getString(s + "___" + variant, false, false, false, null);
        if (t != null) {
            return t;
        }
        return getString(s, true, false, false, null);
    }

    /**
     * Retrieves an internationalized string.
     * This method keeps all mnemonics in the translated string that is returned.
     * @param s key to be retrieved
     * @return translated string including mnemonics marked with "&"
     */
    public static String getStringWithMnemonic(String s) {
        return getString(s, true, true, false, null);
    }

    /**
     * Retrieves an internationalized variant of a string (see getString(String, String).
     * This method keeps all mnemonics in the translated string that is returned.
     *
     * @param s key to be retrieved
     * @param variant discriminator to specify the context of the key
     * @return the translation for "s___variant", if existing; translation for "s" otherwise -- including mnemonics marked with "&".
     */
    public static String getStringWithMnemonic(String s, String variant) {
        String t = getString(s + "___" + variant, false, true, false, null);
        if (t != null) {
            return t;
        }
        return getString(s, true, true, false, null);
    }

    private static String getMessage(String s, Object[] args) {
        if (msgFormat == null) {
            initialize();
        }
        try {
            msgFormat.applyPattern(getString(s, true, false, true, null));
            return msgFormat.format(args);
        } catch(Exception e) {
            if (LOG_MISSING_KEYS) {
                Logger.log(Logger.WARNING, Logger.MSG_INTERNATIONAL_INCORRECTKEY,"Incorrect Compound Key: "+s); // no need for translation!
            }
            if (STACKTRACE_MISSING_KEYS) {
                EfaErrorPrintStream.ignoreExceptions = true;
                e.printStackTrace();
                EfaErrorPrintStream.ignoreExceptions = false;
            }
            return (MARK_MISSING_KEYS ? "#" : "") + s + ": " + getArrayStrings(args) + (MARK_MISSING_KEYS ? "#" : "");
        }
    }

    /**
     * Retrieves an internationalized compound message.
     * All arguments inside this message have to be masked by "{arg}", where "arg" may be any text
     * that is not further interpreted or used (other than being part of the key). For each of the
     * arguments masked by "{arg}", and argument arg1, arg2, ...., argn has to be supplied.
     * @param s key to be retrieved
     * @param arg1 argument to be placed into compound message
     * @return translated message string
     */
    // ========================= String only arguments =========================
    public static String getMessage(String s, String arg1) {
        Object[] args = { "dummy", arg1 };
        return getMessage(s,args);
    }

    public static String getMessage(String s, String arg1, String arg2) {
        Object[] args = { "dummy", arg1, arg2 };
        return getMessage(s,args);
    }

    public static String getMessage(String s, String arg1, String arg2, String arg3) {
        Object[] args = {"dummy", arg1, arg2, arg3};
        return getMessage(s, args);
    }

    public static String getMessage(String s, String arg1, String arg2, String arg3, String arg4) {
        Object[] args = {"dummy", arg1, arg2, arg3, arg4};
        return getMessage(s, args);
    }

    public static String getMessage(String s, String arg1, String arg2, String arg3, String arg4, String arg5) {
        Object[] args = {"dummy", arg1, arg2, arg3, arg4, arg5};
        return getMessage(s, args);
    }

    public static String getMessage(String s, String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, String arg7) {
        Object[] args = {"dummy", arg1, arg2, arg3, arg4, arg5, arg6, arg7};
        return getMessage(s, args);
    }

    public static String getMessage(String s, String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, String arg7, String arg8) {
        Object[] args = {"dummy", arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8};
        return getMessage(s, args);
    }

    // ========================= Integer only arguments =========================
    public static String getMessage(String s, int arg1) {
        Object[] args = {"dummy", Integer.toString(arg1)};
        return getMessage(s, args);
    }

    public static String getMessage(String s, int arg1, int arg2) {
        Object[] args = { "dummy", Integer.toString(arg1), Integer.toString(arg2) };
        return getMessage(s,args);
    }

    public static String getMessage(String s, int arg1, int arg2, int arg3) {
        Object[] args = { "dummy", Integer.toString(arg1), Integer.toString(arg2), Integer.toString(arg3) };
        return getMessage(s,args);
    }

    public static String getMessage(String s, int arg1, int arg2, int arg3, int arg4) {
        Object[] args = { "dummy", Integer.toString(arg1), Integer.toString(arg2), Integer.toString(arg3), Integer.toString(arg4) };
        return getMessage(s,args);
    }

    public static String getMessage(String s, int arg1, int arg2, int arg3, int arg4, int arg5) {
        Object[] args = { "dummy", Integer.toString(arg1), Integer.toString(arg2), Integer.toString(arg3), Integer.toString(arg4), Integer.toString(arg5) };
        return getMessage(s,args);
    }
    public static String getMessage(String s, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
        Object[] args = { "dummy", Integer.toString(arg1), Integer.toString(arg2), Integer.toString(arg3), Integer.toString(arg4), Integer.toString(arg5), Integer.toString(arg6) };
        return getMessage(s,args);
    }

    // ========================= Long only arguments =========================
    public static String getMessage(String s, long arg1) {
        Object[] args = {"dummy", Long.toString(arg1)};
        return getMessage(s, args);
    }

    // ========================= mixed Type arguments =========================
    public static String getMessage(String s, int arg1, String arg2) {
        Object[] args = {"dummy", Integer.toString(arg1), arg2};
        return getMessage(s, args);
    }

    public static String getMessage(String s, int arg1, String arg2, String arg3) {
        Object[] args = {"dummy", Integer.toString(arg1), arg2, arg3};
        return getMessage(s, args);
    }

    public static String getMessage(String s, String arg1, int arg2) {
        Object[] args = { "dummy", arg1, Integer.toString(arg2) };
        return getMessage(s,args);
    }

    public static String getMessage(String s, String arg1, int arg2, String arg3) {
        Object[] args = {"dummy", arg1, Integer.toString(arg2), arg3};
        return getMessage(s, args);
    }

    public static char getDecimalSeparator() {
        if (numberFormat == null) {
            initialize();
        }
        return decimalSeparator;
    }

    public static char getSpaceOrDash() {
        String l = getLanguageID();
        return (l.equals("de") ? '-' : ' ');
    }

    public static String getLanguageDescription() {
        if (locale == null) {
            initialize();
        }
        try {
            return locale.getDisplayName();
        } catch(Exception e) {
            return "unknown"; // do not internationalize
        }
    }

    public static String getLanguageDescription(String langId) {
        Locale loc = new Locale(langId);
        return loc.getDisplayName();
    }

    public static String getLanguageID() {
        if (locale == null) {
            initialize();
        }
        try {
            return locale.toString();
        } catch(Exception e) {
            return "";
        }
    }

    public static Locale getLocale() {
        if (locale == null) {
            initialize();
        }
        return locale;
    }

    public static boolean isInitialized() {
        return bundle != null;
    }

    public static ResourceBundle getResourceBundle() {
        return bundle;
    }

    public static void setMarkMissingKeys(boolean enabled) {
        MARK_MISSING_KEYS = enabled;
    }
    public static void setLogMissingKeys(boolean enabled) {
        LOG_MISSING_KEYS = enabled;
    }
    public static void setTraceMissingKeys(boolean enabled) {
        STACKTRACE_MISSING_KEYS = enabled;
    }
    public static void setShowKeys(boolean enabled) {
        SHOW_KEY_INSTEAD_OF_TRANSLATION = enabled;
    }

}
