/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.nmichael.efa.core.config;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.EfaSec;
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemType;
import de.nmichael.efa.core.items.ItemTypeAction;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeColor;
import de.nmichael.efa.core.items.ItemTypeConfigButton;
import de.nmichael.efa.core.items.ItemTypeCronEntry;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeFile;
import de.nmichael.efa.core.items.ItemTypeFontName;
import de.nmichael.efa.core.items.ItemTypeHashtable;
import de.nmichael.efa.core.items.ItemTypeImage;
import de.nmichael.efa.core.items.ItemTypeInteger;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeLong;
import de.nmichael.efa.core.items.ItemTypeMultiSelectList;
import de.nmichael.efa.core.items.ItemTypePassword;
import de.nmichael.efa.core.items.ItemTypeRadioButtons;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.core.items.ItemTypeTime;
import de.nmichael.efa.data.MessageRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataLocks;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.gui.BaseTabbedDialog;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.gui.widgets.AlertWidget;
import de.nmichael.efa.gui.widgets.IWidget;
import de.nmichael.efa.gui.widgets.Widget;
import de.nmichael.efa.themes.EfaFlatLafHelper;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

public class EfaConfig extends StorageObject implements IItemFactory {

	public static final String DATATYPE = "efa2config";

	// Parameter Categories
	public final String CATEGORY_INTERNAL = "%-1%" + International.getString("intern");
	public final String CATEGORY_COMMON = BaseTabbedDialog.CATEGORY_COMMON;
	public final String CATEGORY_INPUT = "%02%" + International.getString("Eingabe");
	public final String CATEGORY_BASE = "%03%" + Daten.EFA_BASE;
	public final String CATEGORY_BOATHOUSE = "%04%" + Daten.EFA_BOATHOUSE;
	public final String CATEGORY_GUI = "%05%" + International.getString("Erscheinungsbild");
	public final String CATEGORY_GUI_WINDOW = "%051%" + International.getString("Fenster");
	public final String CATEGORY_GUI_BOATLIST = "%053%" + International.getString("Bootslisten");
	public final String CATEGORY_GUIBUTTONS = "%06%" + International.getString("Buttons");
	public final String CATEGORY_BACKUP = "%07%" + International.getString("Backup");
	public final String CATEGORY_EXTTOOLS = "%08%" + International.getString("externe Programme");
	public final String CATEGORY_PRINTING = "%09%" + International.getString("Drucken");
	public final String CATEGORY_STARTSTOP = "%10%" + International.getString("Starten und Beenden");
	public final String CATEGORY_PERMISSIONS = "%11%" + International.getString("Berechtigungen");
	public final String CATEGORY_LOCKEFA = "%12%" + International.getString("Sperren");
	public final String CATEGORY_NOTIFICATIONS = "%13%" + International.getString("Benachrichtigungen");
	public final String CATEGORY_TYPES = "%14%" + International.getString("Bezeichnungen");
	public final String CATEGORY_TYPES_SESS = "%141%" + International.getString("Fahrtart");
	public final String CATEGORY_TYPES_BOAT = "%142%" + International.getString("Bootsart");
	public final String CATEGORY_TYPES_SEAT = "%143%" + International.getString("Anzahl Bootsplätze");
	public final String CATEGORY_TYPES_RIGG = "%144%" + International.getString("Riggerung");
	public final String CATEGORY_TYPES_COXD = "%145%" + International.getString("mit/ohne Stm.");
	public final String CATEGORY_TYPES_GEND = "%146%" + International.getString("Geschlecht");
	public final String CATEGORY_TYPES_STAT = "%147%" + International.getString("Status");
	public final String CATEGORY_SYNC = "%15%" + International.getString("Synchronisation");
	public final String CATEGORY_KANUEFB = "%16%" + International.onlyFor("Kanu-eFB", "de");
	public final String CATEGORY_LOCALE = "%17%" + International.getStringWithoutAnyEscaping("Sprache & Region");
	public final String CATEGORY_WIDGETS = "%18%" + International.getString("Widgets");
	public final String CATEGORY_WIDGET_CLOCK = "%181%" + International.getString("Uhr");
	public final String CATEGORY_WIDGET_NEWS = "%182%" + International.getString("Ticker");
	public final String CATEGORY_DATAACCESS = "%19%" + International.getString("Daten");
	public final String CATEGORY_DATAXML = "%191%" + International.getString("lokale Dateien");
	public final String CATEGORY_DATAREMOTE = "%192%" + Daten.EFA_REMOTE;
	public final String CATEGORY_DATACLOUD = "%193%" + Daten.EFA_CLOUD;
	public final String CATEGORY_CRONTAB = "%20%" + International.getString("Automatische Abläufe");

	// Config items starting with a "_" are not to be stored automatically in
	// efaConfig file.
	// This is for descriptive elements like headers or hints in efaConfig.
	// Storage of "_" config items has to be handled separately. This applies
	// specially for efaTYPE values
	// Boat types, gender types, etc.

	public static final String NOT_STORED_ITEM_PREFIX = "_";

	private static final int STRINGLIST_VALUES = 1;
	private static final int STRINGLIST_DISPLAY = 2;

	// Werte für NameFormat
	public static final String NAMEFORMAT_FIRSTLAST = "FIRSTLAST";
	public static final String NAMEFORMAT_LASTFIRST = "LASTFIRST";

	// Default-Obmann für ungesteuerte Boote
	public static final String OBMANN_BOW = "BOW";
	public static final String OBMANN_STROKE = "STROKE";

	// Werte für FontType
	public static final String FONT_PLAIN = "PLAIN";
	public static final String FONT_BOLD = "BOLD";

	// Values for email security
	public static final String SECURITY_STARTTLS = "STARTTLS";
	public static final String SECURITY_SSL = "SSL";

	// some default values
	private static final String[] DEFAULT_BROWSER = { "/usr/bin/firefox", "/usr/bin/mozilla", "/usr/bin/netscape",
			"c:\\Programme\\Mozilla Firefox\\firefox.exe", "c:\\Programme\\Internet Explorer\\iexplore.exe",
			"c:\\Program Files\\Mozilla Firefox\\firefox.exe", "c:\\Program Files\\Internet Explorer\\iexplore.exe" };
	private static final String[] DEFAULT_ACROBAT = { "/usr/bin/acroread",
			"c:\\Programme\\Adobe\\Reader 9.0\\Reader\\AcroRd32.exe",
			"c:\\Program Files\\Adobe\\Reader 9.0\\Reader\\AcroRd32.exe" };

	public static final String WEEKLY_RESERVATION_CONFLICT_IGNORE = "WEEKLY_RESERVATION_CONFLICT_IGNORE";
	public static final String WEEKLY_RESERVATION_CONFLICT_STRICT = "WEEKLY_RESERVATION_CONFLICT_STRICT";
	public static final String WEEKLY_RESERVATION_CONFLICT_PRIORITIZE_WEEKLY = "WEEKLY_RESERVATION_CONFLICT_PRIORITIZE_WEEKLY";

	public static final String FONT_NAME_LAF_DEFAULT_FONT = "--Standard--";
	
	// private configuration data
	private ItemTypeString lastProjectEfaBase;
	private ItemTypeString lastProjectEfaBoathouse;
	private ItemTypeString lastProjectEfaCli;
	private ItemTypeBoolean autogenAlias;
	private ItemTypeBoolean touchscreenSupport;
	private ItemTypeString aliasFormat;
	private ItemTypeFile browser;
	private ItemTypeFile acrobat;
	private ItemTypeInteger printPageWidth;
	private ItemTypeInteger printPageHeight;
	private ItemTypeInteger printLeftMargin;
	private ItemTypeInteger printTopMargin;
	private ItemTypeInteger printPageOverlap;
	private ItemTypeHashtable<String> keys;
	private ItemTypeString defaultValueComments;
	private ItemTypeInteger countEfaStarts;
	private ItemTypeString registeredProgramID;
	private ItemTypeInteger registrationChecks;
	private ItemTypeString efaBoathouseChangeLogbookReminder;
	private ItemTypeBoolean autoStandardmannsch;
	private ItemTypeBoolean manualStandardmannsch;
	private ItemTypeBoolean showObmann;
	private ItemTypeBoolean autoObmann;
	private ItemTypeStringList defaultObmann;
	private ItemTypeStringList weeklyReservationConflictBehaviour;
	private ItemTypeBoolean showDestinationInfoForInput;
	private ItemTypeBoolean additionalWatersInput;
	private ItemTypeBoolean prefixDestinationWithWaters;
	private ItemTypeBoolean postfixPersonsWithClubName;
	private ItemTypeBoolean allowSessionsWithoutDistance;
	private ItemTypeBoolean popupComplete;
	private ItemTypeBoolean popupContainsMode;
	private ItemTypeBoolean popupContainsModeEasyFindEntriesWithSpecialCharacters;
	private ItemTypeBoolean	popupContainsModeSelectPrefixItem;
	private ItemTypeStringList nameFormat;
	private ItemTypeBoolean correctMisspelledNames;
	private ItemTypeBoolean skipUhrzeit;
	private ItemTypeBoolean skipZiel;
	private ItemTypeBoolean skipBemerk;
	private ItemTypeBoolean clubworkRequiresApproval;
	private ItemTypeBoolean fensterZentriert;
	private ItemTypeInteger windowXOffset;
	private ItemTypeInteger windowYOffset;
	private ItemTypeInteger screenWidth;
	private ItemTypeInteger screenHeight;
	private ItemTypeInteger maxDialogHeight;
	private ItemTypeInteger maxDialogWidth;
	private ItemTypeStringList lookAndFeel;
	private ItemTypeColor lafButtonFocusColor;
	private ItemTypeColor efaGuiflatLaf_Background;
	private ItemTypeInteger efaGuiflatLaf_BackgroundFieldsLightenPercentage;
	private ItemTypeColor efaGuiflatLaf_AccentColor;
	private ItemTypeColor efaGuiflatLaf_FocusColor;

	
	// items starting with efaDirekt are designed to be active in efaBths only.
	
	private ItemTypeStringList standardFahrtart;
	private ItemTypeStringList defaultDistanceUnit;
	private ItemTypeStringList dateFormat;
	private ItemTypeBoolean debugLogging;
	private ItemTypeString traceTopic;
	private ItemTypeInteger traceLevel;
	private ItemTypeLong efaVersionLastCheck;
	private ItemTypeLong javaVersionLastCheck;
	private ItemTypeString version;
	private ItemTypeBoolean efaDirekt_zielBeiFahrtbeginnPflicht;
	private ItemTypeBoolean efaDirekt_gewaesserBeiUnbekanntenZielenPflicht;
	private ItemTypeBoolean efaDirekt_eintragErzwingeObmann;
	private ItemTypeBoolean efaDirekt_checkAllowedGroupsForBoat;
	private ItemTypeBoolean efaDirekt_checkAllowedMinGroupForBoat;
	private ItemTypeBoolean efaDirekt_eintragErlaubeNurMaxRudererzahl;
	private ItemTypeBoolean efaDirekt_warnEvenNonCriticalBoatDamages;
	private ItemTypeBoolean efaDirekt_eintragNichtAenderbarUhrzeit;
	private ItemTypeBoolean efaDirekt_eintragNichtAenderbarKmBeiBekanntenZielen;
	private ItemTypeBoolean fixCoxForCoxlessUnknownBoats;
	private ItemTypeBoolean efaBoathouseOnlyEnterKnownBoats;
	private ItemTypeBoolean efaBoathouseOnlyEnterKnownPersons;
	private ItemTypeBoolean efaBoathouseOnlyEnterKnownDestinations;
	private ItemTypeBoolean efaBoathouseOnlyEnterKnownWaters;
	private ItemTypeBoolean efaBoathouseStrictUnknownPersons;
	private ItemTypeBoolean efaHeaderUseHighlightColor;
	private ItemTypeColor efaHeaderBackgroundColor;
	private ItemTypeColor efaHeaderForegroundColor;
	private ItemTypeBoolean efaHeaderUseForTabbedPanes;
	private ItemTypeBoolean efaBoathouseFilterTextfieldStandardLists;
	private ItemTypeBoolean efaBoathouseFilterTextfieldBoatsNotAvailableList;
	private ItemTypeBoolean efaBoathouseFilterTextfieldEasyFindEntriesWithSpecialCharacters;
	private ItemTypeInteger efaBoathouseFilterTextAutoClearInterval;
	private ItemTypeBoolean efaBoathouseFilterTextAutoClearAfterAction;
	private ItemTypeBoolean efaBoathouseTwoColumnList;
	private ItemTypeBoolean efaBoathouseExtdToolTips;
	private ItemTypeInteger efaBoathouseExtdToolTipInitialDelayMsec;
	private ItemTypeInteger efaBoathouseExtdToolTipDismissDelayMsec;
	private ItemTypeBoolean efaBoathouseBoatListWithReservationInfo;
	private ItemTypeString efaBoathouseNonAllowedUnknownPersonNames;
	private ItemTypeBoolean efaDirekt_eintragHideUnnecessaryInputFields;
	
	private ItemTypeBoolean efaDirekt_eintragPresentLastTripOnNewEntry;
	private ItemTypeBoolean efaDirekt_eintragPresentLastTripOnLateEntry;
	private ItemTypeInteger	efaDirekt_eintragPresentLastTripTimeout;
	private ItemTypeBoolean efaDirekt_MultisessionSupportStartSession;
	private ItemTypeBoolean	efaDirekt_MultisessionSupportLateEntry;
	private ItemTypeBoolean efaDirekt_MultisessionLastGuiElemParticipants;
	private ItemTypeInteger efaDirekt_plusMinutenAbfahrt;
	private ItemTypeInteger efaDirekt_minusMinutenAnkunft;
	private ItemTypeBoolean allowEnterEndDate;
	private ItemTypeBoolean membersMayReserveBoats;
	private ItemTypeBoolean membersMayReserveBoatsWeekly;
	private ItemTypeBoolean membersMayReservePrivateBoats;
	private ItemTypeBoolean membersMayEditBoatsReservations;
	private ItemTypeBoolean efaDirekt_mitgliederDuerfenEfaBeenden;
	private ItemTypeBoolean efaDirekt_mitgliederDuerfenNamenHinzufuegen;
	private ItemTypeBoolean efaDirekt_resBooteNichtVerfuegbar;
	private ItemTypeBoolean efaDirekt_wafaRegattaBooteAufFahrtNichtVerfuegbar;
	private ItemTypeBoolean efaDirekt_boatListShowForeignLogbookSessionsAsNotAvailable;
	private ItemTypeInteger efaDirekt_resLookAheadTime;
	private ItemTypeString efaDirekt_execOnEfaExit;
	private ItemTypeTime efaDirekt_exitTime;
	private ItemTypeInteger efaDirekt_exitIdleTime;
	private ItemTypeString efaDirekt_execOnEfaAutoExit;
	private ItemTypeTime efaDirekt_restartTime;
	private ItemTypeConfigButton efaDirekt_butFahrtBeginnen;
	private ItemTypeConfigButton efaDirekt_butFahrtBeenden;
	private ItemTypeConfigButton efaDirekt_butFahrtAbbrechen;
	private ItemTypeConfigButton efaDirekt_butNachtrag;
	private ItemTypeConfigButton efaDirekt_butBootsreservierungen;
	private ItemTypeConfigButton efaDirekt_butFahrtenbuchAnzeigen;
	private ItemTypeConfigButton efaDirekt_butStatistikErstellen;
	private ItemTypeConfigButton efaDirekt_butVereinsarbeit;
	private ItemTypeConfigButton efaDirekt_butNachrichtAnAdmin;
	private ItemTypeConfigButton efaDirekt_butAdminModus;
	private ItemTypeConfigButton efaDirekt_butSpezial;
	private ItemTypeConfigButton efaDirekt_butHelp;
	private ItemTypeString efaDirekt_butSpezialCmd;
	private ItemTypeBoolean efaDirekt_showButtonHotkey;
	private ItemTypeBoolean efaDirekt_sortByAnzahl;
	private ItemTypeBoolean efaDirekt_sortByRigger;
	private ItemTypeBoolean efaDirekt_sortByType;
	private ItemTypeBoolean efaDirekt_boatListIndividualOthers;
	private ItemTypeBoolean efaDirekt_autoPopupOnBoatLists;
	private ItemTypeBoolean efaDirekt_listAllowToggleBoatsPersons;
	private ItemTypeBoolean efaDirekt_showEingabeInfos;
	private ItemTypeBoolean efaDirekt_showBootsschadenButton;
	private ItemTypeBoolean efaDirekt_showBoatNotCleanedButton;
	private ItemTypeInteger efaDirekt_maxFBAnzeigenFahrten;
	private ItemTypeInteger efaDirekt_anzFBAnzeigenFahrten;
	private ItemTypeBoolean efaDirekt_FBAnzeigenAuchUnvollstaendige;
	private ItemTypeInteger efaDirekt_notificationWindowTimeout;
	private ItemTypeInteger efaDirekt_boatsNotAvailableListSize;
	private ItemTypeInteger efaDirekt_BthsFontSize;
	private ItemTypeFontName efaDirekt_BthsFontNameButton;
	private ItemTypeInteger efaDirekt_BthsTableFontSize;
	private ItemTypeStringList efaDirekt_BthsFontStyle;
	private ItemTypeFontName efa_OtherFontNameButton;
	private ItemTypeInteger efa_otherFontSize;
	private ItemTypeInteger efa_otherTableFontSize;
	private ItemTypeStringList efa_otherFontStyle;	
	private ItemTypeBoolean efaDirekt_colorizeInputField;
	private ItemTypeBoolean efaDirekt_showZielnameFuerBooteUnterwegs;
	private ItemTypeString efadirekt_adminLastOsCommand;
	private ItemTypeLong efadirekt_lastBoatDamangeReminder;
	private ItemTypeImage efaDirekt_vereinsLogo;
	private ItemTypeBoolean efaBoathouseShowLastFromWaterNotification;
	private ItemTypeString efaBoathouseShowLastFromWaterNotificationText;
	private ItemTypeBoolean efaDirekt_showUhr;
	private ItemTypeBoolean efaDirekt_showNews;
	private ItemTypeString efaDirekt_newsText;
	private ItemTypeInteger efaDirekt_newsScrollSpeed;
	private ItemTypeBoolean efaDirekt_startMaximized;
	private ItemTypeBoolean efaDirekt_startMaximizedRespectTaskbar;
	private ItemTypeBoolean efaDirekt_fensterNichtVerschiebbar;
	private ItemTypeBoolean efaDirekt_immerImVordergrund;
	private ItemTypeBoolean efaDirekt_immerImVordergrundBringToFront;
	private ItemTypeBoolean efaDirekt_tabelleShowTooltip;
	private ItemTypeBoolean efaDirekt_tabelleAlternierendeZeilenfarben;
	private ItemTypeColor efaGuiTableAlternatingRowColorValue;
	private ItemTypeColor efaGuiTableHeaderBackground;
	private ItemTypeColor efaGuiTableHeaderForeground;
	private ItemTypeColor efaGuiTableSelectionBackground;
	private ItemTypeColor efaGuiTableSelectionForeground;
	private ItemTypeBoolean efaDirekt_tabelleEasyFindEntriesWithSpecialCharacters;
	
	private ItemTypeBoolean efaGuiToolTipSpecialColors;
	private ItemTypeColor efaGuiToolTipBackground; 
	private ItemTypeColor efaGuiToolTipForeground;
	private ItemTypeColor efaGuiToolTipHeaderBackground; 
	private ItemTypeColor efaGuiToolTipHeaderForeground;
	
	private ItemTypeStringList efaDirekt_bnrMsgToAdminDefaultRecipient;
	private ItemTypeBoolean efaDirekt_bnrError_admin;
	private ItemTypeBoolean efaDirekt_bnrError_bootswart;
	private ItemTypeBoolean efaDirekt_bnrWarning_admin;
	private ItemTypeBoolean efaDirekt_bnrWarning_bootswart;
	private ItemTypeBoolean efaDirekt_bnrBootsstatus_admin;
	private ItemTypeBoolean efaDirekt_bnrBootsstatus_bootswart;
	private ItemTypeLong efaDirekt_bnrWarning_lasttime;
	private ItemTypeBoolean notificationMarkReadAdmin;
	private ItemTypeBoolean notificationMarkReadBoatMaintenance;
	private ItemTypeBoolean notificationNewBoatDamageByAdmin;
	private ItemTypeString efaDirekt_emailServer;
	private ItemTypeInteger efaDirekt_emailPort;
	private ItemTypeRadioButtons efaDirekt_emailSecurity;
	private ItemTypeString efaDirekt_emailAbsender;
	private ItemTypeString efaDirekt_emailUsername;
	private ItemTypePassword efaDirekt_emailPassword;
	private ItemTypeString efaDirekt_emailAbsenderName;
	private ItemTypeString efaDirekt_emailBetreffPraefix;
	private ItemTypeString efaDirekt_emailSignatur;
	private ItemTypeString efaDirekt_lockEfaShowHtml;
	private ItemTypeBoolean efaDirekt_lockEfaVollbild;
	private ItemTypeDate efaDirekt_lockEfaFromDatum;
	private ItemTypeTime efaDirekt_lockEfaFromZeit;
	private ItemTypeDate efaDirekt_lockEfaUntilDatum;
	private ItemTypeTime efaDirekt_lockEfaUntilZeit;
	private ItemTypeBoolean efaDirekt_locked;
	private ItemTypeBoolean useFunctionalityRowing;
	private ItemTypeBoolean useFunctionalityRowingGermany;
	private ItemTypeBoolean useFunctionalityRowingBerlin;
	private ItemTypeBoolean useFunctionalityCanoeing;
	private ItemTypeBoolean useFunctionalityCanoeingGermany;
	private ItemTypeBoolean experimentalFunctions;
	private ItemTypeBoolean developerFunctions;
	private ItemTypeFile efaUserDirectory;
	private ItemTypeFile lastExportDirectory;
	private ItemTypeFile lastImportDirectory;
	private ItemTypeStringList language;
	private ItemTypeString translateLanguageWork;
	private ItemTypeString translateLanguageBase;
	private ItemTypeAction typesResetToDefault;
	private ItemTypeAction typesAddAllDefault;
	private ItemTypeAction typesAddAllDefaultRowingBoats;
	private ItemTypeAction typesAddAllDefaultCanoeingBoats;
	private ItemTypeHashtable<String> typesGender;
	private ItemTypeHashtable<String> typesBoat;
	private ItemTypeHashtable<String> typesNumSeats;
	private ItemTypeHashtable<String> typesRigging;
	private ItemTypeHashtable<String> typesCoxing;
	private ItemTypeHashtable<String> typesSession;
	private ItemTypeHashtable<String> typesStatus;
	private ItemTypeString kanuEfb_urlLogin;
	private ItemTypeString kanuEfb_urlRequest;
	private ItemTypeDate kanuEfb_SyncTripsAfterDate;
	private ItemTypeBoolean kanuEfb_Fullsync;
	private ItemTypeBoolean kanuEfb_AlwaysShowKanuEFBFields;
	private ItemTypeMultiSelectList<String> kanuEfb_boatTypes;
	private ItemTypeBoolean kanuEfb_SyncUnknownBoats;
	private ItemTypeBoolean kanuEfb_TidyXML;
	private ItemTypeBoolean kanuEfb_SyncTripTypePrefix;
	private ItemTypeBoolean dataPreModifyRecordCallbackEnabled;
	private ItemTypeBoolean dataAuditCorrectErrors;
	private ItemTypeLong dataFileSaveInterval;
	private ItemTypeLong dataFileLockTimeout;
	private ItemTypeBoolean dataFileSynchronousJournal;
	private ItemTypeFile dataBackupDirectory;
	private ItemTypeFile dataMirrorDirectory;
	private ItemTypeBoolean dataRemoteEfaServerEnabled;
	private ItemTypeInteger dataRemoteEfaServerPort;
	private ItemTypeString dataRemoteEfaOnlineUrl;
	private ItemTypeBoolean dataRemoteEfaOnlineEnabled;
	private ItemTypeString dataRemoteEfaOnlineUsername;
	private ItemTypePassword dataRemoteEfaOnlinePassword;
	private ItemTypeLong dataRemoteEfaOnlineUpdateInterval;
	private ItemTypeLong dataRemoteCacheExpiryTime;
	private ItemTypeLong dataRemoteIsOpenExpiryTime;
	private ItemTypeLong dataRemoteLoginFailureRetryTime;
	private ItemTypeLong dataRemoteClientReceiveTimeout;
	private ItemTypeLong dataRemoteEfaCloudSynchIntervalSecs;
	private Vector<IWidget> widgets;
	private ItemTypeItemList crontab;

	private static Color standardTableSelectionBackgroundColor = new Color(75, 134, 193);
	private static Color standardTableSelectionForegroundColor = Color.WHITE;
	private static Color standardTableHeaderBackgroundColor = new Color(171, 206, 241);// new Color(181, 206, 226);
																						// //ABCEF1
	private static Color standardTableHeaderForegroundColor = Color.BLACK;
	private static Color standardTableAlternatingRowColor = new Color(219, 234, 249);
	private static Color standardFlatLafBackgroundColor = new Color(239, 237, 232);// #EFEDE8 //some yellowish gray
	private static Color standardFlatLafAccentColor = new Color(38, 117, 191); // #2675bf Blue
	private static Color standardFlatLafFocusColor = new Color(255, 153, 0); // #ff9900 Orange
	public static Color hintBackgroundColor = new Color(210,225,239);
	public static Color hintBorderColor = new Color(120,166,213);

	private static Color standardToolTipBackgroundColor = new Color(224,237,249);
	private static Color standardToolTipForegroundColor = new Color(21,65,106);

	private static Color standardToolTipHeaderBackgroundColor = new Color(250,252,254);
	private static Color standardToolTipHeaderForegroundColor = standardToolTipForegroundColor;	
	
	
	// private internal data
	private HashMap<String, IItemType> configValues; // always snychronize on this object!!
	private Vector<String> configValueNames;
	private ConfigValueUpdateThread configValueUpdateThread;
	private EfaTypes myEfaTypes;

	public EfaConfig(int storageType, String storageLocation, String storageUsername, String storagePassword) {
		super(storageType, storageLocation, storageUsername, storagePassword, "configuration", DATATYPE,
				International.getString("Konfiguration"));
		initialize(null);
	}

	public EfaConfig() {
		super(IDataAccess.TYPE_FILE_XML, Daten.efaCfgDirectory, null, null, "configuration", DATATYPE,
				International.getString("Konfiguration"));
		initialize(null);
	}

	public EfaConfig(CustSettings custSettings) {
		super(IDataAccess.TYPE_FILE_XML, Daten.efaCfgDirectory, null, null, "configuration", DATATYPE,
				International.getString("Konfiguration"));
		initialize(custSettings);
	}

	// initialize data structures (one-time only)
	private void initialize(CustSettings custSettings) {
		EfaConfigRecord.initialize();
		dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
		configValues = new HashMap<String, IItemType>();
		configValueNames = new Vector<String>();
		iniParameters(custSettings);
	}

	private EfaTypes getMyEfaTypes() {
		if (myEfaTypes != null && myEfaTypes.data().getStorageType() == data().getStorageType()) {
			return myEfaTypes;
		}
		if (data().getStorageType() == IDataAccess.TYPE_EFA_REMOTE && Daten.project != null) {
			return new EfaTypes(Daten.project.getProjectStorageType(), Daten.project.getProjectStorageLocation(),
					Daten.project.getProjectStorageUsername(), Daten.project.getProjectStoragePassword());
		} else {
			return Daten.efaTypes;
		}
	}

	public DataRecord createNewRecord() {
		return new EfaConfigRecord(this, MetaData.getMetaData(DATATYPE));
	}

	public EfaConfigRecord createEfaConfigRecord(String name, String value) {
		EfaConfigRecord r = new EfaConfigRecord(this, MetaData.getMetaData(DATATYPE));
		r.setName(name);
		r.setValue(value);
		return r;
	}

	public EfaConfigRecord getRecord(String name) {
		try {
			return ((EfaConfigRecord) data().get(EfaConfigRecord.getKey(name)));
		} catch (Exception e) {
			return null;
		}
	}

	public String getValue(String name) {
		try {
			return ((EfaConfigRecord) data().get(EfaConfigRecord.getKey(name))).getValue();
		} catch (Exception e) {
			return null;
		}
	}

	public boolean addValue(String name, String value) {
		try {
			EfaConfigRecord r = createEfaConfigRecord(name, value);
			data().add(r);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean updateValue(String name, String value) {
		try {
			EfaConfigRecord r = (EfaConfigRecord) data().get(EfaConfigRecord.getKey(name));

			// There may be a setting which is not yet present in efaconfigrecord.
			// Then we add this value, instead of updating it.
			// needed for listbox-based config values like efa->efb sync boat types.

			if ((r == null)) {
				// For some reason, configuration values starting with "_" are not to be saved
				// in efa configuration files.
				// So we skip those entries.
				if (!name.startsWith(NOT_STORED_ITEM_PREFIX)) {
					addValue(name, value);
				}
			} else {
				r.setValue(value);
				data().update(r);
			}

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void open(boolean createNewIfNotExists) throws EfaException {
		super.open(createNewIfNotExists);
		synchronized (configValues) {
			for (int i = 0; i < configValueNames.size(); i++) {
				String name = configValueNames.get(i);
				if (!name.startsWith("_") && getValue(name) == null) {
					addValue(name, configValues.get(name).toString());
				}
			}
		}
		if (data().getStorageType() != IDataAccess.TYPE_EFA_REMOTE) {
			(configValueUpdateThread = new ConfigValueUpdateThread(this)).start();
			updateConfigValuesWithPersistence();
		}
	}

	public void close() throws EfaException {
		configValueUpdateThread.stopConfigValueUpdateThread();
		configValueUpdateThread = null;
		super.close();
	}

	public boolean updateConfigValuesWithPersistence() {
		if (configValueUpdateThread != null) {
			return configValueUpdateThread.updateConfigValuesWithPersistence();
		} else {
			return (new ConfigValueUpdateThread(this)).updateConfigValuesWithPersistence();
		}
	}

	// initialize all configuration parameters with their default values
	private void iniParameters(CustSettings custSettings) {
		synchronized (configValues) {
			// ============================= INTERNAL =============================
			addParameter(version = new ItemTypeString("EfaVersion", Daten.VERSIONID, IItemType.TYPE_INTERNAL,
					BaseTabbedDialog.makeCategory(CATEGORY_INTERNAL), "efa version"));
			addParameter(efaVersionLastCheck = new ItemTypeLong("EfaVersionLastCheck", 0, 0, Long.MAX_VALUE,
					IItemType.TYPE_INTERNAL, BaseTabbedDialog.makeCategory(CATEGORY_INTERNAL),
					"efa last checked for new efa version"));
			addParameter(javaVersionLastCheck = new ItemTypeLong("JavaVersionLastCheck", 0, 0, Long.MAX_VALUE,
					IItemType.TYPE_INTERNAL, BaseTabbedDialog.makeCategory(CATEGORY_INTERNAL),
					"efa last checked for new java version"));
			addParameter(countEfaStarts = new ItemTypeInteger("EfaStartsCounter", 0, 0, Integer.MAX_VALUE, false,
					IItemType.TYPE_INTERNAL, BaseTabbedDialog.makeCategory(CATEGORY_INTERNAL), "efa start counter"));
			addParameter(
					registeredProgramID = new ItemTypeString("EfaRegistrationProgramId", "", IItemType.TYPE_INTERNAL,
							BaseTabbedDialog.makeCategory(CATEGORY_INTERNAL), "efa registered programm ID"));
			addParameter(registrationChecks = new ItemTypeInteger("EfaRegistrationChecks", 0, 0, Integer.MAX_VALUE,
					false, IItemType.TYPE_INTERNAL, BaseTabbedDialog.makeCategory(CATEGORY_INTERNAL),
					"efa registration checks counter"));
			addParameter(efaBoathouseChangeLogbookReminder = new ItemTypeString("EfaBoathouseChangeLogbookReminder", "",
					IItemType.TYPE_INTERNAL, BaseTabbedDialog.makeCategory(CATEGORY_INTERNAL),
					"efa Boathouse Change Logbook Reminder"));

			// ============================= COMMON:COMMON =============================
			if (dataAccess != null && dataAccess.getStorageType() != IDataAccess.TYPE_EFA_REMOTE) {
				addParameter(efaUserDirectory = new ItemTypeFile("_EfaUserDirectory",
						Daten.efaBaseConfig.efaUserDirectory, International.getString("Verzeichnis für Nutzerdaten"),
						International.getString("Verzeichnisse"), null, ItemTypeFile.MODE_OPEN, ItemTypeFile.TYPE_DIR,
						IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_COMMON),
						International.getString("Verzeichnis für Nutzerdaten")));
			} else {
				addParameter(new ItemTypeLabel("_EfaRemoteLabel", IItemType.TYPE_PUBLIC,
						BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_COMMON),
						International.getString("efa-Konfiguration") + " (" + International.getString("remote") + ")"));
			}
			addParameter(lastProjectEfaBase = new ItemTypeString("LastProjectEfaBase", "", IItemType.TYPE_INTERNAL,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_COMMON), "Last project opened by efaBase"));
			addParameter(lastProjectEfaCli = new ItemTypeString("LastProjectEfaCli", "", IItemType.TYPE_INTERNAL,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_COMMON), "Last project opened by efaCLI"));
			addParameter(lastExportDirectory = new ItemTypeFile("LastExportDirectory", Daten.userHomeDir,
					"last export directory", International.getString("Verzeichnisse"), null, ItemTypeFile.MODE_OPEN,
					ItemTypeFile.TYPE_DIR, IItemType.TYPE_INTERNAL,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_COMMON), "last export directory"));
			addParameter(lastImportDirectory = new ItemTypeFile("LastImportDirectory", Daten.userHomeDir,
					"last import directory", International.getString("Verzeichnisse"), null, ItemTypeFile.MODE_OPEN,
					ItemTypeFile.TYPE_DIR, IItemType.TYPE_INTERNAL,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_COMMON), "last import directory"));
			addParameter(experimentalFunctions = new ItemTypeBoolean("ExperimentalFunctions", false,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_COMMON), International.getMessage(
							"{type} Funktionalitäten aktivieren", International.getString("Experimentelle"))));
			addParameter(developerFunctions = new ItemTypeBoolean("DeveloperFunctions", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON), International.getMessage(
							"Funktionalitäten aktivieren für {sport}", International.getString("Entwicklung"))));
			addParameter(debugLogging = new ItemTypeBoolean("DebugLogging", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON),
					International.getString("Debug-Logging aktivieren")));
			addParameter(traceTopic = new ItemTypeString("DebugTraceTopic", "", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON), International.getString("Trace-Topic")));
			addParameter(traceLevel = new ItemTypeInteger("DebugTraceLevel", 1, 1, 9, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON), International.getString("Trace-Level")));

			// ============================= COMMON:INPUT =============================
			addHeader("efaCommonInputCommon", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Allgemein"), 3);

			addParameter(nameFormat = new ItemTypeStringList("NameFormat", NAMEFORMAT_LASTFIRST,
					new String[] { NAMEFORMAT_FIRSTLAST, NAMEFORMAT_LASTFIRST },
					new String[] { International.getString("Vorname") + " " + International.getString("Nachname"),
							International.getString("Nachname") + ", " + International.getString("Vorname"), },
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Namensformat")));

			addParameter(standardFahrtart = new ItemTypeStringList("SessionTypeDefault", EfaTypes.TYPE_SESSION_NORMAL,
					EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES),
					EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Standard-Fahrtart")));
			addParameter(defaultObmann = new ItemTypeStringList("BoatCaptainDefault", OBMANN_BOW,
					makeObmannArray(STRINGLIST_VALUES), makeObmannArray(STRINGLIST_DISPLAY), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Standard-Obmann für ungesteuerte Boote")));

			addDescription("weeklyReservationConflictBehaviourDescription", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString(
							"Wenn Reservierungskonflikte zwischen einmaligen und wöchentlichen Reservierungen auftreten..."),
					3, 6, 3);

			addParameter(weeklyReservationConflictBehaviour = new ItemTypeStringList("weeklyReservationBehaviour",
					WEEKLY_RESERVATION_CONFLICT_IGNORE,
					new String[] { WEEKLY_RESERVATION_CONFLICT_IGNORE, WEEKLY_RESERVATION_CONFLICT_STRICT,
							WEEKLY_RESERVATION_CONFLICT_PRIORITIZE_WEEKLY },
					new String[] { International.getString("Konflikt_ignorieren"),
							International.getString("Konflikt_strikt_behandeln"),
							International.getString("Konflikt_woechentliche_Reservierung_hat_Vorrang") },
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Verhalten_bei_Konflikten_einmalige_oder_woechentliche_Reservierungen")));

			addHeader("efaCommonInputInputFields", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Eingabefelder"), 3);

			addParameter(efaDirekt_colorizeInputField = new ItemTypeBoolean("InputColorizeFields", true,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("aktuelles Eingabefeld farblich hervorheben")));
			addParameter(efaDirekt_showEingabeInfos = new ItemTypeBoolean("InputShowHints", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Eingabehinweise anzeigen")));
			addParameter(touchscreenSupport = new ItemTypeBoolean("TouchScreenSupport", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Touchscreen-Support") + " (EXPERIMENTAL!)"));
			addParameter(correctMisspelledNames = new ItemTypeBoolean("SpellingCheckNames", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Eingaben auf Tippfehler prüfen")));
			addParameter(skipUhrzeit = new ItemTypeBoolean("InputSkipTime", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT), International
							.getMessage("Eingabefeld '{field}' überspringen", International.getString("Uhrzeit"))));
			addParameter(skipZiel = new ItemTypeBoolean("InputSkipDestination", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getMessage("Eingabefeld '{field}' überspringen", International.getString("Ziel"))));
			addParameter(skipBemerk = new ItemTypeBoolean("InputSkipComments", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT), International
							.getMessage("Eingabefeld '{field}' überspringen", International.getString("Bemerkungen"))));

			addHeader("efaGuiPopupWindow", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Popup-Fenster zur Elementauswahl"), 3);

			addParameter(popupComplete = new ItemTypeBoolean("AutoCompleteListShow", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Beim Vervollständigen Popup-Liste anzeigen")));
			addParameter(popupContainsMode = new ItemTypeBoolean("AutoCompleteContainsMode", true,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Popup-Liste nach Teilbegriff durchsuchen (statt nach Wortanfang)")));
			addParameter(popupContainsModeEasyFindEntriesWithSpecialCharacters = new ItemTypeBoolean(
					"AutoCompleteContainsModeEasyFindEntriesWithSpecialCharacters", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT), International.getString(
							"In Popup-Liste bei Suche nach Teilbegriff Einträge mit Sonderzeichen einfacher finden")));
			addParameter(popupContainsModeSelectPrefixItem = new ItemTypeBoolean(
					"AutoCompleteContainsModeSelectPrefixItem", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT), International.getString(
							"In Popup-Liste bei Suche nach Teilbegriff den ersten nach Wortanfang passenen Eintrag selektieren")));			

			addHeader("efaCommonInputDestination", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Fahrtziel"), 3);

			addParameter(showDestinationInfoForInput = new ItemTypeBoolean("DestinationInfoShowForInput", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Zielinformationen bei Eingabe anzeigen")));
			addParameter(allowSessionsWithoutDistance = new ItemTypeBoolean("MustEnterDistance", false,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Erlaube Fahrten ohne Entfernungsangabe")));
			addParameter(additionalWatersInput = new ItemTypeBoolean("AdditionalWatersInput", true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Eingabe von Gewässern für unbekannte Ziele und Abstecher")));
			addParameter(prefixDestinationWithWaters = new ItemTypeBoolean("PrefixDestinationWithWaters", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Gewässernamen in Zielliste anzeigen")));

			addHeader("efaCommonInputPersons", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Besatzung"), 3);

			addParameter(fixCoxForCoxlessUnknownBoats = new ItemTypeBoolean("fixCoxForCoxlessUnknownBoats", false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Ein-Personen-Fahrt mit unbekanntem Boot: Person als Crew eintragen (anstatt als Steuermann)")));
			addParameter(showObmann = new ItemTypeBoolean("BoatCaptainShow", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Obmann-Auswahlliste anzeigen")));
			addParameter(autoObmann = new ItemTypeBoolean("BoatCaptainAutoSelect", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Obmann bei Eingabe automatisch auswählen")));
			addParameter(autoStandardmannsch = new ItemTypeBoolean("DefaultCrewAutoSelect", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Standardmannschaft automatisch eintragen")));
			addParameter(manualStandardmannsch = new ItemTypeBoolean("DefaultCrewManualSelect", false,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Manuelle Auswahl einer Standardmannschaft erlauben")));

			addHeader("efaCommonInputPersonsShortCuts", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Eingabe von Personen"), 3);

			addParameter(postfixPersonsWithClubName = new ItemTypeBoolean("PostfixPersonsWithClubName", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Vereinsnamen in Personenliste anzeigen")));
			addParameter(autogenAlias = new ItemTypeBoolean("InputShortcutAutoGenerate", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Eingabe-Kürzel automatisch beim Anlegen neuer Mitglieder generieren")));
			addParameter(aliasFormat = new ItemTypeString("InputShortcutFormat", "{F1}{F2}-{N1}", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Format der Eingabe-Kürzel")));

			addHeader("efaCommonInputComments", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Bemerkungs-Feld"), 3);

			addParameter(defaultValueComments = new ItemTypeString("DefaultValueComments", "", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Vorbelegung Bemerkungs-Feld")));

			addHint("efaCommonInputCommentsHint", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString(
							"Im Bemerkungsfeld kann über die Funktionstasten F6, F7, ... F12 ein Text ergänzt werden."),
					3, 20, 3);

			addDescription("efaCommonInputCommentsDescription", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Bezeichnung = Funktionstaste = 'F6' - 'F12'"), 3, 3, 0);
			addDescription("efaCommonInputCommentsDescription1", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Im Bemerkungsfeld F6 .. F12 betätigen --> Inhalt wird hinzugefügt"), 3, 3,
					0);

			addParameter(keys = new ItemTypeHashtable<String>("InputCommentsHotkeys", "", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_INPUT),
					International.getString("Tastenbelegungen für Bemerkungs-Feld")));

			// ============================= COMMON:GUI =============================
			
			addHeader("efaGuiMainWindowSize", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI_WINDOW),
					International.getString("Hauptfenster Position und Größe"), 3);

			addParameter(windowXOffset = new ItemTypeInteger("WindowOffsetX", 0, Integer.MIN_VALUE, Integer.MAX_VALUE,
					false, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI_WINDOW),
					International.getString("Fenster-Offset") + " X" + " (" + International.getString("Pixel") + ")"));
			addParameter(windowYOffset = new ItemTypeInteger("WindowOffsetY", 0, Integer.MIN_VALUE, Integer.MAX_VALUE,
					false, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI_WINDOW),
					International.getString("Fenster-Offset") + " Y" + " (" + International.getString("Pixel") + ")"));
			addParameter(screenWidth = new ItemTypeInteger("WindowScreenWidth", 0, 0, Integer.MAX_VALUE, false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI_WINDOW),
					International.getString("Bildschirmbreite") + " (" + International.getString("Pixel") + ")"));
			addParameter(screenHeight = new ItemTypeInteger("WindowScreenHeight", 0, 0, Integer.MAX_VALUE, false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI_WINDOW),
					International.getString("Bildschirmhöhe") + " (" + International.getString("Pixel") + ")"));

			addHeader("efaGuiDialogWindowSize", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI_WINDOW),
					International.getString("Dialogfenster-Größe"), 3);

			addParameter(maxDialogWidth = new ItemTypeInteger("WindowDialogMaxWidth", 0, 0, Integer.MAX_VALUE, false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI_WINDOW),
					International.getString("maximale Dialog-Breite") + " (" + International.getString("Pixel") + ")"));
			addParameter(maxDialogHeight = new ItemTypeInteger("WindowDialogMaxHeight", 0, 0, Integer.MAX_VALUE, false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI_WINDOW),
					International.getString("maximale Dialog-Höhe") + " (" + International.getString("Pixel") + ")"));

			addHeader("efaGuiWindowPosition", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI_WINDOW),
					International.getString("Fensterpositionierung"), 3);

			addParameter(fensterZentriert = new ItemTypeBoolean("WindowCentered", false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI_WINDOW),
					International.getString("Alle Fenster in Bildschirmmitte zentrieren")));
			
			addHeader("efaGuiWindowLook", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("LookAndFeel"), 3);

			addParameter(lookAndFeel = new ItemTypeStringList("LookAndFeelNewSetting", getDefaultLookAndFeel(),
					makeLookAndFeelArray(STRINGLIST_VALUES), makeLookAndFeelArray(STRINGLIST_DISPLAY),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Look & Feel")));
			addParameter(lafButtonFocusColor = new ItemTypeColor("LookAndFeel_ButtonFocusColor", "", "",
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					"LookAndFeel ButtonFocusColor", true));

			addHint("efaGuiFlatLafColorsHint", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Diese Einstellungen werden nur vom EFA Flat Look&Feel verwendet."), 3, 32,
					3);

			addParameter(efaGuiflatLaf_Background = new ItemTypeColor("efaGuiflatLaf_Background",
					EfaUtil.getColor(standardFlatLafBackgroundColor), EfaUtil.getColor(standardFlatLafBackgroundColor),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Hintergrundfarbe"), false));

			addParameter(efaGuiflatLaf_BackgroundFieldsLightenPercentage = new ItemTypeInteger(
					"efaGuiflatLaf_BackgroundFieldsLightenPercentage", 10, 1, 100, false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Hintergrund für Eingabefelder aufhellen (%)")));

			addParameter(efaGuiflatLaf_AccentColor = new ItemTypeColor("efaGuiflatLaf_AccentColor",
					EfaUtil.getColor(standardFlatLafAccentColor), EfaUtil.getColor(standardFlatLafAccentColor),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Akzentfarbe"), false));

			addParameter(efaGuiflatLaf_FocusColor = new ItemTypeColor("efaGuiflatLaf_FocusColor",
					EfaUtil.getColor(standardFlatLafFocusColor), EfaUtil.getColor(standardFlatLafFocusColor),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Fokusfarbe"), false));

			addHeader("efaGuiHeaders", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Überschriften-Darstellung"), 3);

			addParameter(efaHeaderUseHighlightColor = new ItemTypeBoolean("efaBoathouseHeaderUseHighlightColor", true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Überschriften hervorheben")));
			addParameter(efaHeaderUseForTabbedPanes = new ItemTypeBoolean("efaBoathouseHeaderUseForTabbedPanes", true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString(
							"Überschriften von Registerkarten hervorheben (Metal+WindowsClassic LookAndFeel))")));
			addParameter(efaHeaderBackgroundColor = new ItemTypeColor("efaBoathouseHeaderBackgroundColor",
					EfaUtil.getColor(standardTableSelectionBackgroundColor),
					EfaUtil.getColor(standardTableSelectionBackgroundColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Überschriften Hintergrundfarbe"), false));
			addParameter(efaHeaderForegroundColor = new ItemTypeColor("efaBoathouseHeaderForegroundColor",
					EfaUtil.getColor(standardTableSelectionForegroundColor),
					EfaUtil.getColor(standardTableSelectionForegroundColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Überschriften Textfarbe"), false));


			addHeader("efaGuiTablesColors", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tabellenfarben"), 3);

			addParameter(efaGuiTableHeaderBackground = new ItemTypeColor("efaGuiTableHeaderBackground",
					EfaUtil.getColor(standardTableHeaderBackgroundColor),
					EfaUtil.getColor(standardTableHeaderBackgroundColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tabellen-Überschriften Hintergrundfarbe"), false));
			addParameter(efaGuiTableHeaderForeground = new ItemTypeColor("efaGuiTableHeaderForeground",
					EfaUtil.getColor(standardTableHeaderForegroundColor),
					EfaUtil.getColor(standardTableHeaderForegroundColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tabellen-Überschriften Textfarbe"), false));
			addParameter(efaGuiTableAlternatingRowColorValue = new ItemTypeColor("efaGuiTableAlternatingRowColorValue",
					EfaUtil.getColor(standardTableAlternatingRowColor),
					EfaUtil.getColor(standardTableAlternatingRowColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Alternierende Zeilen Hintergrundfarbe"), false));

			addParameter(efaGuiTableSelectionBackground = new ItemTypeColor("efaGuiTableSelectionBackground",
					EfaUtil.getColor(standardTableSelectionBackgroundColor),
					EfaUtil.getColor(standardTableSelectionBackgroundColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tabellen selektierte Zeile Hintergrundfarbe"), false));
			addParameter(efaGuiTableSelectionForeground = new ItemTypeColor("efaGuiTableSelectionForeground",
					EfaUtil.getColor(standardTableSelectionForegroundColor),
					EfaUtil.getColor(standardTableSelectionForegroundColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tabellen selektierte Zeile Textfarbe"), false));

			addHeader("efaGuiTables", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tabellendarstellung"), 3);

			addParameter(efaDirekt_tabelleShowTooltip = new ItemTypeBoolean("EfaBoathouseTablesShowTooltip", true,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tabellen mit Tooltipps für zu lange Texte")));
			addParameter(efaDirekt_tabelleAlternierendeZeilenfarben = new ItemTypeBoolean(
					"EfaBoathouseTablesAlternatingRowColor", true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tabellen mit alternierenden Zeilenfarben")));

			addParameter(efaDirekt_tabelleEasyFindEntriesWithSpecialCharacters = new ItemTypeBoolean(
					"EfaBoathouseTablesEasyFindEntriesWithSpecialCharacters", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI), International
							.getString("Tabellen sollen bei Filterung Einträge mit Sonderzeichen einfacher finden")));

			addHeader("efaGuiToolTips", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tooltips"), 3);
			
			addParameter(efaGuiToolTipSpecialColors = new ItemTypeBoolean("EfaGuiToolTipSpecialColors", true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tooltipps mit eigener Farbgebung")));
			
			addParameter(efaGuiToolTipBackground = new ItemTypeColor("efaGuiToolTipBackground",
					EfaUtil.getColor(standardToolTipBackgroundColor),
					EfaUtil.getColor(standardToolTipBackgroundColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tooltipp Hintergrundfarbe"), false));
			addParameter(efaGuiToolTipForeground = new ItemTypeColor("efaGuiToolTipForeground",
					EfaUtil.getColor(standardToolTipForegroundColor),
					EfaUtil.getColor(standardToolTipForegroundColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tooltipp Textfarbe"), false));		

			addParameter(efaGuiToolTipHeaderBackground = new ItemTypeColor("efaGuiToolTipHeaderBackground",
					EfaUtil.getColor(standardToolTipHeaderBackgroundColor),
					EfaUtil.getColor(standardToolTipHeaderBackgroundColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tooltipp-Überschriften Hintergrundfarbe"), false));
			addParameter(efaGuiToolTipHeaderForeground = new ItemTypeColor("efaGuiToolTipHeaderForeground",
					EfaUtil.getColor(standardToolTipHeaderForegroundColor),
					EfaUtil.getColor(standardToolTipHeaderForegroundColor), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tooltipp-Überschriften Textfarbe"), false));					
			
			addHeader("efaGuiOtherFont", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Schriftart für efaBase"), 3);
			
			addHint("efaGuiOtherFontHint", IItemType.TYPE_PUBLIC, 
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Die Schrift von efaBootshaus wird in efaBootshaus->Erscheinungsbild eingestellt."),
					3,3,3);

			String defaultFont=getDefaultFont();
			addParameter(efa_OtherFontNameButton = new ItemTypeFontName("EfaOtherFontNameButton",
					defaultFont, defaultFont,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Schriftart"),false));			
			
			addParameter(efa_otherFontSize = new ItemTypeInteger("EfaOtherFontSize", 12, 6, 32, false,
					IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Schriftgröße in Punkten (6 bis 32, Standard: 12)")));
			addParameter(efa_otherFontStyle = new ItemTypeStringList("EfaOtherFontStyle", "",
					makeFontStyleArray(STRINGLIST_VALUES), makeFontStyleArray(STRINGLIST_DISPLAY),
					IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Schriftstil")));

			addParameter(efa_otherTableFontSize = new ItemTypeInteger("EfaOtherTableFontSize", 12, 6, 24,
					false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_GUI),
					International.getString("Tabellen-Schriftgröße in Punkten (6 bis 20, Standard: 12)")));					
	
			
			// ============================= COMMON:EXTTOOLS =============================
			addParameter(browser = new ItemTypeFile("ProgramWebbrowser", searchForProgram(DEFAULT_BROWSER),
					International.getString("Webbrowser"), International.getString("Windows-Programme") + " (*.exe)",
					"exe", ItemTypeFile.MODE_OPEN, ItemTypeFile.TYPE_FILE, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_EXTTOOLS),
					International.getString("Webbrowser")));
			addParameter(acrobat = new ItemTypeFile("ProgramAcrobatReader", searchForProgram(DEFAULT_ACROBAT),
					International.getString("Acrobat Reader"),
					International.getString("Windows-Programme") + " (*.exe)", "exe", ItemTypeFile.MODE_OPEN,
					ItemTypeFile.TYPE_FILE, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_EXTTOOLS),
					International.getString("Acrobat Reader")));

			// ============================= COMMON:PRINT =============================
			addParameter(printPageWidth = new ItemTypeInteger("PrintPageWidth", 210, 1, Integer.MAX_VALUE, false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_PRINTING),
					International.getString("Seitenbreite")));
			addParameter(printPageHeight = new ItemTypeInteger("PrintPageHeight", 297, 1, Integer.MAX_VALUE, false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_PRINTING),
					International.getString("Seitenhöhe")));
			addParameter(printLeftMargin = new ItemTypeInteger("PrintPageMarginLeftRight", 15, 0, Integer.MAX_VALUE,
					false, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_PRINTING),
					International.getString("linker und rechter Rand")));
			addParameter(printTopMargin = new ItemTypeInteger("PrintPageMarginTopBottom", 15, 0, Integer.MAX_VALUE,
					false, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_PRINTING),
					International.getString("oberer und unterer Rand")));
			addParameter(printPageOverlap = new ItemTypeInteger("PrintPageOverlap", 5, 0, Integer.MAX_VALUE, false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_PRINTING),
					International.getString("Seitenüberlappung")));

			// ============================= BOATHOUSE:COMMON =============================
			addParameter(lastProjectEfaBoathouse = new ItemTypeString("LastProjectEfaBoathouse", "",
					IItemType.TYPE_INTERNAL, BaseTabbedDialog.makeCategory(CATEGORY_COMMON, CATEGORY_COMMON),
					"Last project opened by efaBoathouse"));

			addHeader("efaBthsCommonFahrtbeginn", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Fahrtbeginn"), 3);

			addParameter(efaDirekt_showBootsschadenButton = new ItemTypeBoolean("BoatDamageEnableReporting", true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Melden von Bootsschäden erlauben")));
			addParameter(efaDirekt_showBoatNotCleanedButton = new ItemTypeBoolean("ShowBoatNotCleanedButton", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON), 
					International.getString("Melden von ungeputzten Booten erlauben")));
			
			addDescription("efaCommonInputCommonResLookAheadTime", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON), International.getString(
							"Bei Fahrtbeginn kann auf zeitnah anstehende Reservierungen geprüft werden."),
					3, 3, 0);

			addParameter(efaDirekt_resLookAheadTime = new ItemTypeInteger("ReservationLookAheadTime", 120, 0,
					Integer.MAX_VALUE, false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON), International
							.getString("Bei Fahrtbeginn auf Reservierungen bis zu x Minuten in der Zukunft prüfen")));

			addHeader("efaBthsCommonFahrtEnde", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Fahrtende"), 3);

			addParameter(efaBoathouseShowLastFromWaterNotification = new ItemTypeBoolean(
					"ShowLastFromWaterNotification", true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON), International
							.getString("Nach Beenden letzter Fahrt Erinnerung zum Schließen der Bootshalle anzeigen")));
			addParameter(efaBoathouseShowLastFromWaterNotificationText = new ItemTypeString(
					"ShowLastFromWaterNotificationText",
					International.getString("Alle Boote sind zurück.") + "<br>"
							+ International.getString("Bitte schließe die Hallentore."),
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Erinnerungstext zum Schließen der Bootshalle")));
			addParameter(efaDirekt_notificationWindowTimeout = new ItemTypeInteger("NotificationWindowTimeout", 10, 0,
					Integer.MAX_VALUE, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Timeout für Hinweis-Fenster")));

			addHeader("efaBthsCommonFahrtenbuch", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Fahrtenbuch"), 3);
			addParameter(efaDirekt_maxFBAnzeigenFahrten = new ItemTypeInteger("LogbookDisplayEntriesMaxNumber", 100, 1,
					Integer.MAX_VALUE, false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Fahrtenbuch anzeigen") + ": "
							+ International.getString("maximale Anzahl von Fahrten")));
			addParameter(efaDirekt_anzFBAnzeigenFahrten = new ItemTypeInteger("LogbookDisplayEntriesDefaultNumber", 50,
					1, Integer.MAX_VALUE, false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Fahrtenbuch anzeigen") + ": "
							+ International.getString("Anzahl von Fahrten")));

			addParameter(efaDirekt_FBAnzeigenAuchUnvollstaendige = new ItemTypeBoolean(
					"LogbookDisplayEntriesDefaultAlsoIncomplete", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Fahrtenbuch anzeigen") + ": "
							+ International.getString("auch unvollständige Fahrten")));
			
			addHeader("efaBthsCommonVereinsarbeit", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Vereinsarbeit"), 3);
			addParameter(clubworkRequiresApproval = new ItemTypeBoolean("ClubworkRequiresApproval", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Arbeitsstunden erst nach Bestätigung durch Admin berücksichtigen")));

			addParameter(efaDirekt_locked = new ItemTypeBoolean("LockEfaLocked", false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_LOCKEFA),
					International.getString("efa ist für die Benutzung gesperrt")));
			addParameter(efaDirekt_lockEfaShowHtml = new ItemTypeString("LockEfaPage", "", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_LOCKEFA),
					International.getString("efa sperren") + ": " + International.getString("HTML-Seite anzeigen")));
			addParameter(efaDirekt_lockEfaVollbild = new ItemTypeBoolean("LockEfaFullScreen", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_LOCKEFA),
					International.getString("efa sperren") + ": " + International.getString("Vollbild")));
			addParameter(efaDirekt_lockEfaFromDatum = new ItemTypeDate("LockEfaFromDate", new DataTypeDate(-1, -1, -1),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_LOCKEFA),
					International.getString("efa sperren") + ": "
							+ International.getString("Sperrung automatisch beginnen") + " ("
							+ International.getString("Datum") + ")"));
			addParameter(efaDirekt_lockEfaFromZeit = new ItemTypeTime("LockEfaFromTime", new DataTypeTime(-1, -1, -1),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_LOCKEFA),
					International.getString("efa sperren") + ": "
							+ International.getString("Sperrung automatisch beginnen") + " ("
							+ International.getString("Zeit") + ")"));
			addParameter(efaDirekt_lockEfaUntilDatum = new ItemTypeDate("LockEfaToDate", new DataTypeDate(-1, -1, -1),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_LOCKEFA),
					International.getString("efa sperren") + ": "
							+ International.getString("Sperrung automatisch beenden") + " ("
							+ International.getString("Datum") + ")"));
			addParameter(efaDirekt_lockEfaUntilZeit = new ItemTypeTime("LockEfaToTime", new DataTypeTime(-1, -1, -1),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_LOCKEFA),
					International.getString("efa sperren") + ": "
							+ International.getString("Sperrung automatisch beenden") + " ("
							+ International.getString("Zeit") + ")"));
			addParameter(efadirekt_adminLastOsCommand = new ItemTypeString("AdminLastOsCommand", "",
					IItemType.TYPE_INTERNAL, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON),
					International.getString("Betriebssystemkommando")));
			addParameter(efadirekt_lastBoatDamangeReminder = new ItemTypeLong("LastBoatDamageReminder", 0, 0,
					Long.MAX_VALUE, IItemType.TYPE_INTERNAL,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_COMMON), "Last Boat Damage Reminder"));

			// ============================= BOATHOUSE:INPUT =============================

			addHint("efaGuiBoathouseInputHint", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getMessage(
							"Weitere Einstellungen finden Sie in dem Bereich {Allgemein}->{Erscheinungsbild}",
							International.getString("Allgemein"), International.getString("Eingabe")),
					3, 10, 10);

			addHeader("efaBthsInputCommon", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Allgemein"), 3);
			addParameter(efaDirekt_eintragHideUnnecessaryInputFields = new ItemTypeBoolean("InputHideUnnecessaryFields",
					true, IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Beim Eintrag von Fahrten unnötige Eingabefelder ausblenden")));
			
			addHeader("efaBthsInputUnknownValues", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Umgang mit unbekannten Werten"), 3);

			addParameter(efaBoathouseOnlyEnterKnownBoats = new ItemTypeBoolean("InputAllowOnlyKnownBoats", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getMessage("Beim Eintrag von Fahrten nur bekannte Namen erlauben für {type}",
							International.getString("Boote"))));
			addParameter(efaBoathouseOnlyEnterKnownPersons = new ItemTypeBoolean("InputAllowOnlyKnownPersons", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getMessage("Beim Eintrag von Fahrten nur bekannte Namen erlauben für {type}",
							International.getString("Personen"))));
			addParameter(efaBoathouseStrictUnknownPersons = new ItemTypeBoolean("InputStrictUnknownPersons", false,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International
							.getString("Strenge Prüfung des Namensformats beim Eintrag von unbekannten Personen")));
			addParameter(efaBoathouseNonAllowedUnknownPersonNames = new ItemTypeString("InputNonAllowedPersonNames", "",
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString(
							"Nicht erlaubte Namen beim Eintrag von unbekannten Personen (durch ; getrennt)")));
			addParameter(efaBoathouseOnlyEnterKnownDestinations = new ItemTypeBoolean("InputAllowOnlyKnownDestinatins",
					false, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getMessage("Beim Eintrag von Fahrten nur bekannte Namen erlauben für {type}",
							International.getString("Ziele"))));
			addParameter(efaBoathouseOnlyEnterKnownWaters = new ItemTypeBoolean("InputAllowOnlyKnownWaters", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getMessage("Beim Eintrag von Fahrten nur bekannte Namen erlauben für {type}",
							International.getString("Gewässer"))));

			addHeader("efaBthsInputBoatUsageChecks", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Prüfung vor Bootsnutzung"), 3);
			addParameter(efaDirekt_checkAllowedGroupsForBoat = new ItemTypeBoolean("InputCheckAllowedPersonsInBoat",
					true, IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Bei Bootsbenutzung von nicht erlaubten Personen warnen")));
			addParameter(efaDirekt_checkAllowedMinGroupForBoat = new ItemTypeBoolean("InputCheckMinGroupPersonsInBoat",
					true, IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString(
							"Bei Bootsbenutzung warnen, wenn nicht mindestens eine Person aus geforderter Gruppe")));
			addParameter(efaDirekt_eintragErlaubeNurMaxRudererzahl = new ItemTypeBoolean("InputAllowOnlyMaxCrewNumber",
					true, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Nur für das Boot maximal mögliche Anzahl an Personen erlauben")));
			addParameter(efaDirekt_warnEvenNonCriticalBoatDamages = new ItemTypeBoolean(
					"InputWarnOnlyCriticalBoatDamages", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Bei Bootsbenutzung auch im Fall von unkritischen Bootsschäden warnen")));
			addParameter(efaDirekt_eintragErzwingeObmann = new ItemTypeBoolean("InputMustSelectBoatCaptain", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Obmann muß ausgewählt werden")));

			addHeader("efaBthsInputTime", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Zeit- und Datumsangaben"), 3);

			addParameter(efaDirekt_eintragNichtAenderbarUhrzeit = new ItemTypeBoolean("InputNotEditableTime", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Vorgeschlagene Uhrzeiten können nicht geändert werden")));
			addParameter(efaDirekt_plusMinutenAbfahrt = new ItemTypeInteger("StartSessionTimeAdd", 5, 0,
					Integer.MAX_VALUE, false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Für Abfahrt x Minuten zur aktuellen Zeit hinzuaddieren")));
			addParameter(efaDirekt_minusMinutenAnkunft = new ItemTypeInteger("FinishSessionTimeSubstract", 5, 0,
					Integer.MAX_VALUE, false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Für Ankunft x Minuten von aktueller Zeit abziehen")));
			addParameter(allowEnterEndDate = new ItemTypeBoolean("AllowEnterEndDate", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Eingabe von Enddatum erlauben")));

			addHeader("efaBthsInputDestination", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Angaben zum Fahrtziel"), 3);

			addParameter(efaDirekt_zielBeiFahrtbeginnPflicht = new ItemTypeBoolean("StartSessionMustSelectDestination",
					false, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Ziel muß bereits bei Fahrtbeginn angegeben werden")));
			addParameter(efaDirekt_gewaesserBeiUnbekanntenZielenPflicht = new ItemTypeBoolean(
					"MustEnterWatersForUnknownDestinations", false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Gewässer muß bei unbekannten Zielen angegeben werden")));
			addParameter(efaDirekt_eintragNichtAenderbarKmBeiBekanntenZielen = new ItemTypeBoolean(
					"InputDistanceNotEditableForKnownDestinations", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT), International
							.getString("Vorgeschlagene Kilometer bei bekannten Zielen können nicht geändert werden")));

			addHeader("PresentLastTripValues", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Anlage mehrerer Fahrten hintereinander vereinfachen"), 3);
			
			addDescription("PresentLastTripDescription1", IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					"<html>"+International.getStringWithMnemonic("PRESENT_LAST_TRIP_DESCRIPTION1")+"</html>", 3, 2,10);
			
			addParameter(efaDirekt_MultisessionSupportStartSession = new ItemTypeBoolean("MultiSessionSupportStartsession",
					true, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Fahrtbeginn: Vereinfachte Eingabe für mehrere Einzelfahrten")));
			
			addParameter(efaDirekt_MultisessionSupportLateEntry = new ItemTypeBoolean("MultiSessionSupportLateEntry",
					true, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Nachtrag: Vereinfachte Eingabe für mehrere Einzelfahrten")));

			addParameter(efaDirekt_MultisessionLastGuiElemParticipants = new ItemTypeBoolean("MultiSessionLastGuiElemParticipants",
					false, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Teilnehmer und Boot am Ende des Dialogs erfassen")));
			
			addDescription("PresentLastTripDescription2", IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					"<html>"+International.getStringWithMnemonic("PRESENT_LAST_TRIP_DESCRIPTION2")+"</html>", 3, 20,10);

			addParameter(efaDirekt_eintragPresentLastTripOnNewEntry = new ItemTypeBoolean("PresentLastTripOnNewEntry",
					false, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Bei Eintragung von Fahrten Teile der vorangegangenen Fahrt einblenden")));

			addParameter(efaDirekt_eintragPresentLastTripOnLateEntry = new ItemTypeBoolean("PresentLastTripOnLateEntry",
					false, IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Bei Eintragung von Fahrt-Nachträgen Teile der vorangegangenen Fahrt einblenden")));
			
			// minimum MUST be 1 minute, not zero, as otherwise the code in efaBaseFrame does not work correctly.
			addParameter(efaDirekt_eintragPresentLastTripTimeout = new ItemTypeInteger("PresentLastTripTimeout", 2, 1,
					45, false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_INPUT),
					International.getString("Einblenden der vorhergehenden Fahrt bis maximal X Minuten")));			

			// ============================= BOATHOUSE:GUI =============================

			addHint("efaGuiBoathouseWindowHint", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getMessage(
							"Weitere Einstellungen finden Sie in dem Bereich {Allgemein}->{Erscheinungsbild}",
							International.getString("Allgemein"), International.getString("Erscheinungsbild")),
					3, 10, 10);

			addHeader("efaGuiBoathouseWindow", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Fensterpositionierung"), 3);
			addHint("efaDirekt_startMaximizedHint", 
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Auf Bildschirmgroesse bzw. auf die Breite-Hoehe in Allgemein->Fenster maximieren"),3,0, 6);
			addParameter(efaDirekt_startMaximized = new ItemTypeBoolean("EfaBoathouseWindowMaximized", true,
					IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("efa maximiert starten")));
			addHint("EfaBoathouseWindowMaximizedRespectTaskBarHint", 
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Wirkt nur, wenn Allgemein->Fenster die Fenster-Offsets X und Y beide 0 sind"),3, 10, 6);
			addParameter(efaDirekt_startMaximizedRespectTaskbar = new ItemTypeBoolean("EfaBoathouseWindowMaximizedRespectTaskBar", true,
					IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Beim Maximieren Groesse und Position der Taskleisten beruecksichtigen")));

			addParameter(efaDirekt_fensterNichtVerschiebbar = new ItemTypeBoolean("EfaBoathouseWindowFixedPosition",
					true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Hauptfenster nicht verschiebbar")));
			efaDirekt_fensterNichtVerschiebbar.setPadding(0, 0, 20, 2);
			
			addParameter(efaDirekt_immerImVordergrund = new ItemTypeBoolean("EfaBoathouseWindowAlwaysOnTop", false,
					IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("efa immer im Vordergrund")));
			addParameter(efaDirekt_immerImVordergrundBringToFront = new ItemTypeBoolean(
					"EfaBoathouseWindowAlwaysOnTopBringToFront", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("efa immer im Vordergrund - efa jede Minute in den Vordergrund bringen")));
			
			addHeader("efaGuiBoathouseFont", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Schriftart"), 3);

			addParameter(efaDirekt_BthsFontNameButton = new ItemTypeFontName("EfaBoathouseFontNameButton",
					defaultFont,defaultFont,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Schriftart"),false));
			
			addParameter(efaDirekt_BthsFontSize = new ItemTypeInteger("EfaBoathouseFontSize", 16, 6, 32, false,
					IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Schriftgröße in Punkten (6 bis 32, Standard: 16)")));
			
			addParameter(efaDirekt_BthsFontStyle = new ItemTypeStringList("EfaBoathouseFontStyle", "",
					makeFontStyleArray(STRINGLIST_VALUES), makeFontStyleArray(STRINGLIST_DISPLAY),
					IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Schriftstil")));

			addParameter(efaDirekt_BthsTableFontSize = new ItemTypeInteger("EfaBoathouseTableFontSize", 14, 6, 24,
					false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Tabellen-Schriftgröße in Punkten (6 bis 20, Standard: 14)")));

			addHeader("efaGuiBoathouseOther", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Sonstiges"), 3);

			addParameter(efaDirekt_vereinsLogo = new ItemTypeImage("ClubLogo", "", 320, 200, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI ),
					International.getString("Vereinslogo")));

			addHeader("efaGuiBoathouseBoatListsCommon", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Bootslisten allgemein"), 3);

			addParameter(efaDirekt_listAllowToggleBoatsPersons = new ItemTypeBoolean("BoatListToggleToPersons", false,
					IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI ),
					International.getString("erlaube Auswahl in Bootslisten alternativ auch über Personennamen")));

			addParameter(efaDirekt_autoPopupOnBoatLists = new ItemTypeBoolean("BoatListShowPopup", true,
					IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("automatisches Popup-Menü für Mausclicks in den Bootslisten")));

			addParameter(efaDirekt_resBooteNichtVerfuegbar = new ItemTypeBoolean(
					"BoatListShowReservedBoatsAsNotAvailable", false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Reservierte Boote als 'nicht verfügbar' anzeigen")));
			addParameter(efaDirekt_wafaRegattaBooteAufFahrtNichtVerfuegbar = new ItemTypeBoolean(
					"BoatListShowOnMultiDayOrRegattaBoatsAsNotAvailable", true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString(
							"Boote auf Regatta, Trainingslager oder Mehrtagesfahrt als 'nicht verfügbar' anzeigen")));
			addParameter(efaDirekt_boatListShowForeignLogbookSessionsAsNotAvailable = new ItemTypeBoolean(
					"BoatListShowForeignLogbookSessionsAsNotAvailable", true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString(
							"Boote, die in anderen Fahrtenbüchern unterwegs sind, als 'nicht verfügbar' anzeigen")));
			
			addParameter(efaDirekt_boatsNotAvailableListSize = new ItemTypeInteger("BoatsNotAvailableListSize", 200,
					100, 600, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Listengröße") + " '" + International.getString("nicht verfügbare Boote")
							+ "'"));
			// ===================== BOATHOUSE: Contents and Look of Boat Lists
			// ============================

			addHeader("efaGuiBoathouseBoatListsFilter", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Filter-Felder"), 3);

			addParameter(efaBoathouseFilterTextfieldStandardLists = new ItemTypeBoolean(
					"efaBoathouseFilterTextfieldStandardLists", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Filter-Feld über Standard Listen")));
			addParameter(efaBoathouseFilterTextfieldBoatsNotAvailableList = new ItemTypeBoolean(
					"efaBoathouseFilterTextfieldBoatsNotAvailableList", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Filter-Feld über Liste nicht verfügbarer Boote")));

			addParameter(efaBoathouseFilterTextfieldEasyFindEntriesWithSpecialCharacters = new ItemTypeBoolean(
					"efaBoathouseFilterTextfieldEasyFindEntriesWithSpecialCharacters", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Filter-Feld soll Einträge mit Sonderzeichen einfacher finden")));

			addParameter(efaBoathouseFilterTextAutoClearAfterAction = new ItemTypeBoolean(
					"efaBoathouseFilterTextAutoClearAfterAction", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Filter-Felder leeren nach Abschluss von Aktivitäten")));

			addParameter(efaBoathouseFilterTextAutoClearInterval = new ItemTypeInteger(
					"efaBoathouseFilterTextAutoClearInterval", 2, 0, 1440, true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Filter-Felder leeren nach x Minuten (0 für nie)")));

			addHeader("efaGuiBoathouseBoatListsContent", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Inhalte / Darstellung"), 3);

			addParameter(efaBoathouseTwoColumnList = new ItemTypeBoolean("efaBoathouseTwoColumnList", true,
					IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Bootshaus-Listen mit zwei Spalten darstellen")));
			addParameter(efaBoathouseBoatListWithReservationInfo = new ItemTypeBoolean(
					"efaBoathouseBoatListWithReservationInfo", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Bootshaus-Listen mit Reservierungsdaten")));
			addParameter(efaDirekt_showZielnameFuerBooteUnterwegs = new ItemTypeBoolean(
					"BoatListDisplayDestinationForBoatsOnTheWater", true, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getMessage("Fahrtziel in der Liste {list} anzeigen",
							International.getString("Boote auf Fahrt"))));
			addParameter(efaBoathouseExtdToolTips = new ItemTypeBoolean("efaBoathouseExtdToolTips", true,
					IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Bootshaus-Listen mit Tooltips")));
			addParameter(efaBoathouseExtdToolTipInitialDelayMsec = new ItemTypeInteger(
					"efaBoathouseExtdToolTipInitialDelayMsec", 1250, 0, 60000, false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Verzögerung, bis Tooltip erscheint (msec)")));
			addParameter(efaBoathouseExtdToolTipDismissDelayMsec = new ItemTypeInteger(
					"efaBoathouseExtdToolTipDismissDelayMsec", 3000, 0, 60000, false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Verzögerung, bis Tooltip ausgeblendet wird (msec)")));

			addHeader("efaGuiBoathouseBoatListsSortorder", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("Bootslisten Sortierung"), 3);
			addParameter(
					efaDirekt_sortByAnzahl = new ItemTypeBoolean("BoatListSortBySeats", true, IItemType.TYPE_EXPERT,
							BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE,
									CATEGORY_GUI),
							International.getString("sortiere Boote nach Anzahl der Bootsplätze")));
			addParameter(
					efaDirekt_sortByRigger = new ItemTypeBoolean("BoatListSortByRigger", false, IItemType.TYPE_EXPERT,
							BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE,
									CATEGORY_GUI),
							International.getString("sortiere Boote nach Riggerung")));
			addParameter(efaDirekt_sortByType = new ItemTypeBoolean("BoatListSortByType", false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("sortiere Boote nach Bootstyp")));
			addParameter(efaDirekt_boatListIndividualOthers = new ItemTypeBoolean("BoatListIndividualOthers", false,
					IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUI),
					International.getString("andere Boote in Bootslisten individuell gruppieren")));

			// ============================= BOATHOUSE:GUIBUTTONS
			// =============================
			
			addHint("efaMultiSessinoSupportHintOnButtons", 
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getString("Konfiguration der Schaltflächen hinter 'Fahrt beginnen' und 'Nachtrag' via efa-Bootshaus -> Eingabe -> Vereinfachte Anlage..."),3, 6, 6);
			addParameter(efaDirekt_butFahrtBeginnen = new ItemTypeConfigButton("ButtonStartSession",
					International.getString("Fahrt beginnen"), "CCFFCC", true, false, true, false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Fahrt beginnen"))));
			addParameter(efaDirekt_butFahrtBeenden = new ItemTypeConfigButton("ButtonFinishSession",
					International.getString("Fahrt beenden"), "CCFFCC", true, false, true, false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Fahrt beenden"))));
			addParameter(efaDirekt_butFahrtAbbrechen = new ItemTypeConfigButton("ButtonCancelSession",
					International.getString("Fahrt abbrechen"), "FFCCCC", true, false, true, false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Fahrt abbrechen"))));
			addParameter(efaDirekt_butNachtrag = new ItemTypeConfigButton("ButtonLateEntry",
					International.getString("Nachtrag"), "CCFFFF", true, false, true, false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Nachtrag"))));
			addParameter(efaDirekt_butBootsreservierungen = new ItemTypeConfigButton("ButtonBoatReservations",
					International.getString("Bootsreservierungen"), "FFFFCC", true, false, true, true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Bootsreservierungen"))));
			addParameter(efaDirekt_butFahrtenbuchAnzeigen = new ItemTypeConfigButton("ButtonShowLogbook",
					International.getString("Fahrtenbuch anzeigen"), "CCCCFF", true, false, true, true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Fahrtenbuch anzeigen"))));
			addParameter(efaDirekt_butStatistikErstellen = new ItemTypeConfigButton("ButtonCreateStatistics",
					International.getString("Statistiken erstellen"), "CCCCFF", true, false, true, true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Statistiken erstellen"))));
			addParameter(efaDirekt_butVereinsarbeit = new ItemTypeConfigButton("ButtonClubwork",
					International.getString("Vereinsarbeit"), "CCFFCC", true, false, true, true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Vereinsarbeit erfassen"))));
			addParameter(efaDirekt_butNachrichtAnAdmin = new ItemTypeConfigButton("ButtonMessageToAdmin",
					International.getString("Nachricht an Admin"), "FFF197", true, false, true, true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Nachricht an Admin"))));
			addParameter(efaDirekt_butAdminModus = new ItemTypeConfigButton("ButtonAdminMode",
					International.getString("Admin-Modus"), "CCCCCC", true, false, true, true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Admin-Modus"))));
			addParameter(efaDirekt_butSpezial = new ItemTypeConfigButton("ButtonSpecial",
					International.getString("Spezial-Button"), "CCCCCC", false, true, true, true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Spezial-Button"))));
			addParameter(efaDirekt_butSpezialCmd = new ItemTypeString("ButtonSpecialCommand", "", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS), International.getMessage(
							"Auszuführendes Kommando für '{button}'", International.getString("Spezial-Button"))));
			addParameter(efaDirekt_butHelp = new ItemTypeConfigButton("ButtonHelp",
					International.getString("Hilfe-Button"), null, true, false, false, true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getMessage("Button '{button}'", International.getString("Hilfe-Button"))));
			addParameter(efaDirekt_showButtonHotkey = new ItemTypeBoolean("ButtonShowHotkeys", false,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_GUIBUTTONS),
					International.getString("Hotkeys für Buttons anzeigen")));

			// ============================= BOATHOUSE:STARTSTOP
			// =============================
			addHint("EfaExitRestartTimeHint", 
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_STARTSTOP),
					International.getMessage("Zum Zeitpunkt des Neustarts oder Beendens muss efa mindestens {n} Minuten gelaufen sein.", Daten.AUTO_EXIT_MIN_RUNTIME),3, 6, 6);
			addParameter(efaDirekt_restartTime = new ItemTypeTime("EfaExitRestartTime", new DataTypeTime(4, 0, 0),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_STARTSTOP),
					International.getString("Uhrzeit zum automatischen Neustart von efa")));
			addParameter(efaDirekt_exitTime = new ItemTypeTime("EfaExitExitTime", new DataTypeTime(-1, -1, -1),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_STARTSTOP),
					International.getString("Uhrzeit zum automatischen Beenden von efa")));
			addParameter(efaDirekt_exitIdleTime = new ItemTypeInteger("EfaExitIdleTime", ItemTypeInteger.UNSET, 0,
					Integer.MAX_VALUE, true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_STARTSTOP),
					International.getString("efa automatisch nach Inaktivität beenden") + " ["
							+ International.getString("Minuten") + "]"));
			efaDirekt_exitIdleTime.setPadding(0, 0, 20, 20); // some whitespace before and after
			addParameter(efaDirekt_execOnEfaAutoExit = new ItemTypeString("EfaExitExecOnAutoExit", "",
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_STARTSTOP),
					International.getString("Folgendes Kommando beim automatischen Beenden von efa ausführen")));
			addParameter(efaDirekt_execOnEfaExit = new ItemTypeString("EfaExitExecOnExit", "", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_STARTSTOP),
					International.getString("Folgendes Kommando beim Beenden von efa durch Mitglieder ausführen")));

			// ============================= BOATHOUSE:PERMISSIONS
			// =============================
			addParameter(membersMayReserveBoats = new ItemTypeBoolean("AllowMembersBoatReservation", true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_PERMISSIONS),
					International.getString("Mitglieder dürfen Boote reservieren")));
			addParameter(membersMayReserveBoatsWeekly = new ItemTypeBoolean("AllowMembersBoatReservationWeekly", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_PERMISSIONS),
					International.getString("Mitglieder dürfen Boote reservieren") + " ("
							+ International.getString("wöchentliche Reservierungen") + ")"));
			addParameter(membersMayEditBoatsReservations = new ItemTypeBoolean("AllowMembersBoatReservationEdit", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_PERMISSIONS),
					International.getString("Mitglieder dürfen Bootsreservierungen verändern und löschen")));
			addParameter(membersMayReservePrivateBoats = new ItemTypeBoolean("AllowMembersPrivateBoatReservation", true,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_PERMISSIONS),
					International.getString("Mitglieder dürfen Privatboote reservieren")));
			addParameter(efaDirekt_mitgliederDuerfenNamenHinzufuegen = new ItemTypeBoolean("AllowMembersAddNames",
					false, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_PERMISSIONS),
					International.getString("Mitglieder dürfen Namen zur Mitgliederliste hinzufügen")));
			addParameter(efaDirekt_mitgliederDuerfenEfaBeenden = new ItemTypeBoolean("AllowMembersExitEfa", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_PERMISSIONS),
					International.getString("Mitglieder dürfen efa beenden")));

			// ============================= BOATHOUSE:NOTIFICATIONS
			// =============================
			addParameter(efaDirekt_bnrMsgToAdminDefaultRecipient = new ItemTypeStringList(
					"NotificationMessageToAdminDefaultRecipient", MessageRecord.TO_ADMIN,
					new String[] { MessageRecord.TO_ADMIN, MessageRecord.TO_BOATMAINTENANCE },
					new String[] { International.getString("Administrator"), International.getString("Bootswart") },
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("Standardempfänger für 'Nachricht an Admin'")));
			addParameter(efaDirekt_bnrError_admin = new ItemTypeBoolean("NotificationErrorAdmin", true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getMessage("Benachrichtigungen verschicken an {to} {on_event}",
							International.getString("Admins"), International.getString("bei Fehlern") + " (ERROR)")));
			addParameter(efaDirekt_bnrWarning_admin = new ItemTypeBoolean("NotificationWarningAdmin", true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getMessage("Benachrichtigungen verschicken an {to} {on_event}",
							International.getString("Admins"),
							International.getString("bei Warnungen (WARNING) einmal pro Woche"))));
			addParameter(efaDirekt_bnrBootsstatus_admin = new ItemTypeBoolean("NotificationBoatstatusAdmin", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getMessage("Benachrichtigungen verschicken an {to} {on_event}",
							International.getString("Admins"), International.getString("bei Bootsstatus-Änderungen"))));
			addParameter(efaDirekt_bnrError_bootswart = new ItemTypeBoolean("NotificationErrorBoatMaintenance", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getMessage("Benachrichtigungen verschicken an {to} {on_event}",
							International.getString("Bootswarte"),
							International.getString("bei Fehlern") + " (ERROR)")));
			efaDirekt_bnrError_bootswart.setPadding(0, 0, 10, 0);
			addParameter(efaDirekt_bnrWarning_bootswart = new ItemTypeBoolean("NotificationWarningBoatMaintenance",
					false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getMessage("Benachrichtigungen verschicken an {to} {on_event}",
							International.getString("Bootswarte"),
							International.getString("bei Warnungen (WARNING) einmal pro Woche"))));
			addParameter(efaDirekt_bnrBootsstatus_bootswart = new ItemTypeBoolean(
					"NotificationBoatstatusBoatMaintenance", false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getMessage("Benachrichtigungen verschicken an {to} {on_event}",
							International.getString("Bootswarte"),
							International.getString("bei Bootsstatus-Änderungen"))));
			addParameter(efaDirekt_bnrWarning_lasttime = new ItemTypeLong("NotificationLastWarnings",
					System.currentTimeMillis() - 7l * 24l * 60l * 60l * 1000l, 0, Long.MAX_VALUE, // one week ago
					IItemType.TYPE_INTERNAL, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("letzte Benachrichtigungen")));
			addParameter(notificationNewBoatDamageByAdmin= new ItemTypeBoolean("NotificationNewBoatDamageByAdmin", true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("Benachrichtigung auch für vom Admin erfasste Bootsschäden")
					));
			notificationNewBoatDamageByAdmin.setPadding(0, 0, 10, 0);
			
			addParameter(notificationMarkReadAdmin = new ItemTypeBoolean("NotificationMarkReadAdmin", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getMessage("Nachrichten an {recipient} automatisch als gelesen markieren",
							International.getString("Admin"))));
			addParameter(notificationMarkReadBoatMaintenance = new ItemTypeBoolean(
					"NotificationMarkReadBoatMaintenance", false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getMessage("Nachrichten an {recipient} automatisch als gelesen markieren",
							International.getString("Bootswart"))));
			notificationMarkReadAdmin.setPadding(0, 0, 10, 0);

			addParameter(efaDirekt_emailServer = new ItemTypeString("NotificationEmailServer", "",
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("email") + ": " + International.getString("SMTP-Server")));
			addParameter(efaDirekt_emailPort = new ItemTypeInteger("NotificationEmailPort", 25, 0, 65535, false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("email") + ": " + International.getString("SMTP-Port")));
			addParameter(efaDirekt_emailUsername = new ItemTypeString("NotificationEmailUsername", "",
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("email") + ": " + International.getString("Username")));
			addParameter(efaDirekt_emailPassword = new ItemTypePassword("NotificationEmailPassword", "", true,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("email") + ": " + International.getString("Paßwort")));
			addParameter(efaDirekt_emailAbsenderName = new ItemTypeString("NotificationEmailFromName",
					Daten.EFA_SHORTNAME, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("email") + ": " + International.getString("Absender-Name")));
			addParameter(efaDirekt_emailAbsender = new ItemTypeString("NotificationEmailFromEmail", "",
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("email") + ": " + International.getString("Absender-Adresse")));
			addParameter(efaDirekt_emailBetreffPraefix = new ItemTypeString("NotificationEmailSubjectPrefix",
					Daten.EFA_SHORTNAME, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("email") + ": " + International.getString("Betreff") + " ("
							+ International.getString("Präfix") + ")"));
			addParameter(efaDirekt_emailSignatur = new ItemTypeString("NotificationEmailSignature",
					International.getString("Diese Nachricht wurde von efa verschickt."), IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("email") + ": " + International.getString("Signatur")));
			addParameter(efaDirekt_emailSecurity = new ItemTypeRadioButtons("NotificationEmailSecurity",
					SECURITY_STARTTLS, new String[] { SECURITY_STARTTLS, SECURITY_SSL },
					new String[] { SECURITY_STARTTLS, SECURITY_SSL }, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_NOTIFICATIONS),
					International.getString("email") + ": " + International.getString("Sicherheit")));

			// ============================= SYNC =============================
			addParameter(kanuEfb_AlwaysShowKanuEFBFields = new ItemTypeBoolean("kanuEfb_AlwaysShowKanueEFBFields", false, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_SYNC, CATEGORY_KANUEFB), "KanuEFB-Felder in efa immer einblenden"));
			addParameter(kanuEfb_urlLogin = new ItemTypeString("KanuEfbUrlLogin",
					"https://efb.kanu-efb.de/services/login", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_SYNC, CATEGORY_KANUEFB), "Login URL"));
			addParameter(kanuEfb_urlRequest = new ItemTypeString("KanuEfbUrlRequest",
					"https://efb.kanu-efb.de/services", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_SYNC, CATEGORY_KANUEFB), "Request URL"));
			addParameter(kanuEfb_Fullsync = new ItemTypeBoolean("KanuEfb_FullSync", false, ItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_SYNC, CATEGORY_KANUEFB),
					"Fahrtenbuch immer komplett übertragen (statt nur neue und geänderte Fahrten)"));
			addParameter(kanuEfb_SyncTripsAfterDate = new ItemTypeDate("KanuEfb_SyncTripsAfterDate",
					new DataTypeDate(01, 01, 1970), ItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_SYNC, CATEGORY_KANUEFB),
					"Nur Fahrten synchronisieren mit Beginndatum >="));
			addParameter(kanuEfb_TidyXML = new ItemTypeBoolean("KanuEfb_TidyXML", false, ItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_SYNC, CATEGORY_KANUEFB),
					"XML-Antworten von EFB-Schulungssystem von unzulässigen Daten bereinigen"));

			// ============================= LOCALE =============================
			addParameter(language = new ItemTypeStringList("_Language", Daten.efaBaseConfig.language,
					makeLanguageArray(STRINGLIST_VALUES), makeLanguageArray(STRINGLIST_DISPLAY), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_LOCALE), International.getString("Sprache")));
			addParameter(translateLanguageWork = new ItemTypeString("TranslateLanguageWork", "",
					IItemType.TYPE_INTERNAL, BaseTabbedDialog.makeCategory(CATEGORY_LOCALE), "TranslateLanguageWork"));
			addParameter(translateLanguageBase = new ItemTypeString("TranslateLanguageBase", "",
					IItemType.TYPE_INTERNAL, BaseTabbedDialog.makeCategory(CATEGORY_LOCALE), "TranslateLanguageBase"));

			addParameter(defaultDistanceUnit = new ItemTypeStringList("LocaleDefaultDistanceUnit",
					DataTypeDistance.KILOMETERS, DataTypeDistance.makeDistanceUnitValueArray(),
					DataTypeDistance.makeDistanceUnitNamesArray(), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_LOCALE),
					International.getString("Standardeinheit für Entfernungen")));
			addParameter(dateFormat = new ItemTypeStringList("LocaleDateFormat", DataTypeDate.DAY_MONTH_YEAR,
					DataTypeDate.makeDistanceUnitValueArray(), DataTypeDate.makeDistanceUnitNamesArray(),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_LOCALE),
					International.getString("Datumsformat")));
			addParameter(useFunctionalityRowing = new ItemTypeBoolean("CustUsageRowing",
					(custSettings != null ? custSettings.activateRowingOptions : true), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_LOCALE), International
							.getMessage("Funktionalitäten aktivieren für {sport}", International.getString("Rudern"))));
			addParameter(useFunctionalityRowingGermany = new ItemTypeBoolean("CustUsageRowingGermany",
					(custSettings != null ? custSettings.activateGermanRowingOptions
							: International.getLanguageID().startsWith("de")),
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_LOCALE),
					International.getMessage("Funktionalitäten aktivieren für {sport}",
							International.getString("Rudern")) + " "
							+ International.getMessage("in {region}", International.getString("Deutschland"))));
			addParameter(useFunctionalityRowingBerlin = new ItemTypeBoolean("CustUsageRowingBerlin",
					(custSettings != null ? custSettings.activateBerlinRowingOptions : false), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_LOCALE),
					International.getMessage("Funktionalitäten aktivieren für {sport}",
							International.getString("Rudern")) + " "
							+ International.getMessage("in {region}", International.getString("Berlin"))));
			addParameter(useFunctionalityCanoeing = new ItemTypeBoolean("CustUsageCanoeing",
					(custSettings != null ? custSettings.activateCanoeingOptions : false), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_LOCALE), International.getMessage(
							"Funktionalitäten aktivieren für {sport}", International.getString("Kanufahren"))));
			addParameter(useFunctionalityCanoeingGermany = new ItemTypeBoolean("CustUsageCanoeingGermany",
					(custSettings != null ? custSettings.activateGermanCanoeingOptions : false), IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_LOCALE),
					International.getMessage("Funktionalitäten aktivieren für {sport}",
							International.getString("Kanufahren")) + " "
							+ International.getMessage("in {region}", International.getString("Deutschland"))));

			// ============================= TYPES =============================
			addParameter(typesResetToDefault = new ItemTypeAction("ACTION_TYPES_RESETTODEFAULT",
					ItemTypeAction.ACTION_TYPES_RESETTODEFAULT, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_COMMON),
					International.getString("Alle Standard-Typen zurücksetzen")));
			addParameter(typesAddAllDefault = new ItemTypeAction("ACTION_TYPES_ADDALLDEFAULT",
					ItemTypeAction.ACTION_TYPES_ADDALLDEFAULT, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_COMMON),
					International.getString("Fehlende Standard-Fahrtarten neu generieren")));
			addParameter(typesAddAllDefaultRowingBoats = new ItemTypeAction("ACTION_ADDTYPES_ROWING",
					ItemTypeAction.ACTION_GENERATE_ROWING_BOAT_TYPES, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_TYPES_BOAT),
					International.getMessage("Fehlende Standard-Bootstypen für {rowing_or_canoeing} neu generieren",
							International.getString("Rudern"))));
			addParameter(typesAddAllDefaultCanoeingBoats = new ItemTypeAction("ACTION_ADDTYPES_CANOEING",
					ItemTypeAction.ACTION_GENERATE_CANOEING_BOAT_TYPES, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_TYPES_BOAT),
					International.getMessage("Fehlende Standard-Bootstypen für {rowing_or_canoeing} neu generieren",
							International.getString("Kanufahren"))));
			buildTypes();
			// kanuEfb_SyncUnknownBoats is placed here, as the boat types which can be used
			// for efb sync are
			// built in buildTypes. So this setting does not belong into the --sync--
			// section of efaconfig source.

			addParameter(kanuEfb_SyncUnknownBoats = new ItemTypeBoolean("KanuEfb_SyncUnknownBoats", false,
					ItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_SYNC, CATEGORY_KANUEFB),
					"Fahrten mit unbekannten Booten synchronisieren"));

			addParameter(kanuEfb_SyncTripTypePrefix = new ItemTypeBoolean("KanuEfb_SyncTripTypePrefix", true,
					ItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_SYNC, CATEGORY_KANUEFB),
					"Fahrtbeschreibung für EFB um Fahrtart erweitern"));
			
			// ============================= WIDGETS =============================
			addParameter(efaDirekt_showUhr = new ItemTypeBoolean("WidgetClockEnabled", true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_WIDGETS, CATEGORY_WIDGET_CLOCK),
					International.getMessage("{item} anzeigen", International.getString("Uhr"))));
			addParameter(efaDirekt_showNews = new ItemTypeBoolean("WidgetNewsEnabled", true, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_WIDGETS, CATEGORY_WIDGET_NEWS),
					International.getMessage("{item} anzeigen", International.getString("News"))));
			addParameter(efaDirekt_newsText = new ItemTypeString("WidgetNewsText", "", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_WIDGETS, CATEGORY_WIDGET_NEWS),
					International.getString("News-Text")));
			addParameter(efaDirekt_newsScrollSpeed = new ItemTypeInteger("WidgetNewsScrollSpeed", 250, 100,
					Integer.MAX_VALUE, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_WIDGETS, CATEGORY_WIDGET_NEWS),
					"Scroll Speed"));

			widgets = Widget.getAllWidgets();
			for (int i = 0; widgets != null && i < widgets.size(); i++) {
				IWidget w = widgets.get(i);
				IItemType[] params = w.getParameters();
				for (int j = 0; params != null && j < params.length; j++) {
					if (params[j].getCategory() == null || params[j].getCategory().isEmpty()) {
						params[j].setCategory(BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_WIDGETS,
								"%" + i + "%" + w.getDescription()));
					} else {
						params[j].setCategory(BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_WIDGETS,
								"%" + i + "%" + BaseTabbedDialog.CATEGORY_SEPARATOR + w.getDescription()
										+ params[j].getCategory()));
					}
					addParameter(params[j]);
				}
			}

			// ============================= CRONTAB =============================
			
			addHint("CronTabHint", IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_CRONTAB),
					"<html>"+International.getStringWithMnemonic("Hiermit koennen Sie regelmaessig efaCLI-Kommandos ausfuehren lassen.")+"</html>",3,0,20);
			
			addParameter(crontab = new ItemTypeItemList("CronTab", new Vector<IItemType[]>(), this,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_BOATHOUSE, CATEGORY_CRONTAB),
					International.getString("Automatische Abläufe")));
			crontab.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.BOTH);
			//crontab.setScrollPane(1000, 400);
			crontab.setRepeatTitle(false);

			// ============================= DATA ACCESS =============================
			addParameter(dataPreModifyRecordCallbackEnabled = new ItemTypeBoolean("DataPreModifyRecordCallbackEnabled",
					true, IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_COMMON),
					"PreModifyRecordCallbackEnabled"));
			addParameter(dataAuditCorrectErrors = new ItemTypeBoolean("DataAuditCorrectErrors", true,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_COMMON),
					"DataAuditCorrectErrors"));

			addParameter(dataFileSaveInterval = new ItemTypeLong("DataFileSaveInterval", 10, 1, 3600,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAXML),
					"File Save Interval (sec)"));
			addParameter(dataFileLockTimeout = new ItemTypeLong("DataFileLockTimeout", DataLocks.LOCK_TIMEOUT / 1000,
					10, 120, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAXML), "File Lock Timeout (sec)"));

			addParameter(dataFileSynchronousJournal = new ItemTypeBoolean("DataFileSynchronousJournal", true,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAXML),
					"Flush Journal synchronously"));
			addParameter(dataBackupDirectory = new ItemTypeFile("DataBackupDirectory", "",
					International.getString("Backup-Verzeichnis"), International.getString("Verzeichnisse"), null,
					ItemTypeFile.MODE_OPEN, ItemTypeFile.TYPE_DIR, IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAXML),
					International.getString("Backup-Verzeichnis")));
			addParameter(dataMirrorDirectory = new ItemTypeFile("DataMirrorDirectory", "",
					International.getString("Spiegelverzeichnis für Datenkopie"),
					International.getString("Verzeichnisse"), null, ItemTypeFile.MODE_OPEN, ItemTypeFile.TYPE_DIR,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAXML),
					International.getString("Spiegelverzeichnis für Datenkopie")));

			addHint("dataRemoteEfaServerEnabledDescription", IItemType.TYPE_PUBLIC,
					BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					International.getString("Hinweis: Remote-Zugriff muss aktiv sein, um efaCLI zu benutzen."), 3, 6,
					12);

			addParameter(dataRemoteEfaServerEnabled = new ItemTypeBoolean("DataRemoteEfaServerEnabled", false,
					IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					International.getString("Remote-Zugriff erlauben")));

			addParameter(dataRemoteEfaServerPort = new ItemTypeInteger("DataRemoteEfaServerPort", 0xEFA, 1, 65535,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					International.getString("Server Port")));
			addParameter(dataRemoteCacheExpiryTime = new ItemTypeLong("DataRemoteCacheExpiryTime", 10, 1, 3600,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					"Cache Expiry Time (sec)"));
			addParameter(dataRemoteIsOpenExpiryTime = new ItemTypeLong("DataRemoteIsOpenExpiryTime", 60, 1, 3600,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					"IsStorageObjectOpen Expiry Time (sec)"));
			addParameter(dataRemoteLoginFailureRetryTime = new ItemTypeLong("DataRemoteLoginFailureRetryDelay", 600, 60,
					24 * 3600, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					"Login Failure Retry Delay (sec)"));
			addParameter(dataRemoteClientReceiveTimeout = new ItemTypeLong("DataRemoteClientReceiveTimeout", 60, 10,
					600, IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					"Client Receive Timeout (sec)"));

			addHintWordWrap("dataRemoteEfaOnlineEnabledDescription1", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE), 
					International.getString("efaOnline ist ein dynamischer Namensdienst zum vereinfachten Remote-Zugriff.")
					+ " "+ International.getString("Siehe http://efa.nmichael.de/efaonline.html.de"),
					3, 12, 12,500);

			addParameter(dataRemoteEfaOnlineEnabled = new ItemTypeBoolean("DataRemoteEfaOnlineEnabled", false,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					International.getString("efaOnline aktivieren")));
			addParameter(dataRemoteEfaOnlineUsername = new ItemTypeString("DataRemoteEfaOnlineUsername", "",
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					Daten.EFA_ONLINE + " - " + International.getString("Benutzername") + " ("
							+ International.getString("Server") + ")"));
			addParameter(dataRemoteEfaOnlinePassword = new ItemTypePassword("DataRemoteEfaOnlinePassword", "", true,
					IItemType.TYPE_EXPERT, BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					Daten.EFA_ONLINE + " - " + International.getString("Paßwort") + " ("
							+ International.getString("Server") + ")"));
			addParameter(dataRemoteEfaOnlineUrl = new ItemTypeString("DataRemoteEfaOnlineUrl",
					"http://efa-online.nmichael.de/efa", IItemType.TYPE_INTERNAL,
					BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE), "efaOnline URL"));
			addParameter(dataRemoteEfaOnlineUpdateInterval = new ItemTypeLong("DataRemoteEfaOnlineUpdateInverval", 3600,
					60, 24 * 3600, IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATAREMOTE),
					"efaOnline Update Interval (sec)"));

			addHint("_EfaCloudLabel", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_DATAACCESS, CATEGORY_DATACLOUD),
					International.getString("Bitte konfiguriere efaCloud über das Menü Datei > efaCloud."),3,3,3);

		}
	}

	private void addParameter(IItemType configValue) {
		synchronized (configValues) {
			if (configValues.get(configValue.getName()) != null) {
				// should never happen (program error); no need to translate
				Logger.log(Logger.ERROR, Logger.MSG_ERROR_EXCEPTION,
						"EfaConfig: duplicate parameter: " + configValue.getName());
			} else {
				configValues.put(configValue.getName(), configValue);
				configValueNames.add(configValue.getName());
			}
		}
	}

	/**
	 * Adds a header item in an efa GUI. This header value is not safed within
	 * efaConfig. There is no word-wrap for the caption.
	 * 
	 * The header automatically gets a blue background and white text color; this
	 * cannot be configured as efaConfig cannot refer to its own settings whenn
	 * calling the constructor.
	 * 
	 * @param uniqueName Unique name of the element (as for all of efaConfig
	 *                   elements need unique names)
	 * @param type       TYPE_PUBLIC, TYPE_EXPERT, TYPE_INTERNAL
	 * @param category   Category in which the header is placed
	 * @param caption    Caption
	 * @param gridWidth  How many GridBagLayout cells shall this header be placed
	 *                   in?
	 */
	private IItemType addHeader(String uniqueName, int type, String category, String caption, int gridWidth) {
		// ensure that the header value does not get saved in efaConfig file by adding a
		// special prefix
		IItemType item = EfaGuiUtils.createHeader(uniqueName, type, category, caption, gridWidth);
		addParameter(item);
		return item;
	}

	/**
	 * Adds a description item in an efa GUI. This description value is not safed
	 * within efaConfig. There is no word-wrap for the caption.
	 * 
	 * This is similar to @see addHeader(), but the element does not get a
	 * highlighted background.
	 * 
	 * @param uniqueName Unique name of the element (as for all of efaConfig
	 *                   elements need unique names)
	 * @param type       TYPE_PUBLIC, TYPE_EXPERT, TYPE_INTERNAL
	 * @param category   Category in which the description is placed
	 * @param caption    Caption
	 * @param gridWidth  How many GridBagLayout cells shall this description be
	 *                   placed in?
	 */
	private IItemType addDescription(String uniqueName, int type, String category, String caption, int gridWidth,
			int padBefore, int padAfter) {
		IItemType item = EfaGuiUtils.createDescription(uniqueName, type, category, caption, gridWidth, padBefore, padAfter);
		addParameter(item);
		return item;
	}

	private IItemType addHint(String uniqueName, int type, String category, String caption, int gridWidth,
			int padBefore, int padAfter) {
		IItemType item = EfaGuiUtils.createHint(uniqueName, type, category, caption, gridWidth, padBefore, padAfter);
		addParameter(item);
		return item;
	}
	private IItemType addHintWordWrap(String uniqueName, int type, String category, String caption, int gridWidth,
			int padBefore, int padAfter, int maxWidth) {
		IItemType item = EfaGuiUtils.createHintWordWrap(uniqueName, type, category, caption, gridWidth, padBefore, padAfter, maxWidth);
		addParameter(item);
		return item;
	}

	public void setValue(String name, String newValue) {
		synchronized (configValues) {
			IItemType item = configValues.get(name);
			if (item != null) {
				setValue(item, newValue);
			}
		}
	}

	private void setValue(IItemType item, String newValue) {
		item.parseValue(newValue);
		if (item instanceof ItemTypePassword) {
			updateValue(item.getName(), ((ItemTypePassword) item).getCryptedPassword());
		} else {
			updateValue(item.getName(), newValue);
		}
	}

	public String getLastExportDirectory() {
		return lastExportDirectory.getValue();
	}

	public void setLastExportDirectory(String dir) {
		setValue(lastExportDirectory, dir);
	}

	public String getLastImportDirectory() {
		return lastImportDirectory.getValue();
	}

	public void setLastImportDirectory(String dir) {
		setValue(lastImportDirectory, dir);
	}

	public String getValueLastProjectEfaBase() {
		return lastProjectEfaBase.getValue();
	}

	public void setValueLastProjectEfaBase(String name) {
		setValue(lastProjectEfaBase, name);
	}

	public String getValueLastProjectEfaBoathouse() {
		return lastProjectEfaBoathouse.getValue();
	}

	public void setValueLastProjectEfaBoathouse(String name) {
		setValue(lastProjectEfaBoathouse, name);
	}

	public String getValueLastProjectEfaCli() {
		return lastProjectEfaCli.getValue();
	}

	public void setValueLastProjectEfaCli(String name) {
		setValue(lastProjectEfaCli, name);
	}

	public boolean getValueAutogenAlias() {
		return autogenAlias.getValue();
	}

	public boolean getValueTouchScreenSupport() {
		return touchscreenSupport.getValue();
	}

	public String getValueDefaultValueComments() {
		return defaultValueComments.getValue();
	}

	public String getValueAliasFormat() {
		return aliasFormat.getValue();
	}

	public String getValueBrowser() {
		return browser.getValue();
	}

	public String getValueAcrobat() {
		return acrobat.getValue();
	}

	public int getValuePrintPageWidth() {
		return printPageWidth.getValue();
	}

	public int getValuePrintPageHeight() {
		return printPageHeight.getValue();
	}

	public int getValuePrintLeftMargin() {
		return printLeftMargin.getValue();
	}

	public int getValuePrintTopMargin() {
		return printTopMargin.getValue();
	}

	public int getValuePrintPageOverlap() {
		return printPageOverlap.getValue();
	}

	public ItemTypeHashtable<String> getValueKeys() {
		return keys;
	}

	public int getValueCountEfaStarts() {
		return countEfaStarts.getValue();
	}

	public String getValueRegisteredProgramID() {
		return registeredProgramID.getValue();
	}

	public void setValueRegisteredProgramID(String id) {
		setValue(registeredProgramID, id);
	}

	public int getValueRegistrationChecks() {
		return registrationChecks.getValue();
	}

	public void setValueRegistrationChecks(int checks) {
		setValue(registrationChecks, Integer.toString(checks));
	}

	public String getValueEfaBoathouseChangeLogbookReminder() {
		return efaBoathouseChangeLogbookReminder.getValue();
	}

	public void setValueEfaBoathouseChangeLogbookReminder(String logbook) {
		setValue(efaBoathouseChangeLogbookReminder, logbook);
	}

	public boolean getValueAutoStandardmannsch() {
		return autoStandardmannsch.getValue();
	}

	public boolean getValueManualStandardmannsch() {
		return manualStandardmannsch.getValue();
	}

	public boolean getValueShowObmann() {
		return showObmann.getValue();
	}

	public boolean getValueAutoObmann() {
		return autoObmann.getValue();
	}

	public String getValueDefaultObmann() {
		return defaultObmann.getValue();
	}

	public boolean getValueShowDestinationInfoForInput() {
		return showDestinationInfoForInput.getValue();
	}

	public boolean getValueAdditionalWatersInput() {
		return additionalWatersInput.getValue();
	}

	public boolean getValuePrefixDestinationWithWaters() {
		return prefixDestinationWithWaters.getValue();
	}

	public boolean getValuePostfixPersonsWithClubName() {
		return postfixPersonsWithClubName.getValue();
	}

	public boolean getValueAllowSessionsWithoutDistance() {
		return allowSessionsWithoutDistance.getValue();
	}

	public boolean getValuePopupComplete() {
		return popupComplete.getValue();
	}

	public boolean getValuePopupContainsMode() {
		return popupContainsMode.getValue();
	}

	public boolean getValuePopupContainsModeEasyFindEntriesWithSpecialCharacters() {
		return popupContainsModeEasyFindEntriesWithSpecialCharacters.getValue();
	}

	public boolean getValuePopupContainsModeSelectPrefixItem() {
		return popupContainsModeSelectPrefixItem.getValue();
	}
	
	public String getValueNameFormat() {
		return nameFormat.getValue();
	}

	public boolean getValueNameFormatIsFirstNameFirst() {
		return getValueNameFormat().equals(NAMEFORMAT_FIRSTLAST);
	}

	public boolean getValueCorrectMisspelledNames() {
		return correctMisspelledNames.getValue();
	}

	public boolean getValueSkipUhrzeit() {
		return skipUhrzeit.getValue();
	}

	public boolean getValueSkipZiel() {
		return skipZiel.getValue();
	}

	public boolean getValueSkipBemerk() {
		return skipBemerk.getValue();
	}

	public boolean getValueFensterZentriert() {
		return fensterZentriert.getValue();
	}

	public int getValueWindowXOffset() {
		return windowXOffset.getValue();
	}

	public int getValueWindowYOffset() {
		return windowYOffset.getValue();
	}

	public int getValueScreenWidth() {
		return screenWidth.getValue();
	}

	public int getValueScreenHeight() {
		return screenHeight.getValue();
	}

	public int getValueMaxDialogHeight() {
		return maxDialogHeight.getValue();
	}

	public int getValueMaxDialogWidth() {
		return maxDialogWidth.getValue();
	}

	public String getValueLookAndFeel() {
		return lookAndFeel.getValue();
	}

	public Color getLafButtonFocusColor() {
		return lafButtonFocusColor.getColor();
	}

	public String getValueStandardFahrtart() {
		return standardFahrtart.getValue();
	}

	public String getValueDefaultDistanceUnit() {
		return defaultDistanceUnit.getValue();
	}

	public String getValueDateFormat() {
		return dateFormat.getValue();
	}

	public boolean getValueDebugLogging() {
		return debugLogging.getValue();
	}

	public String getValueTraceTopic() {
		return traceTopic.getValue();
	}

	public int getValueTraceLevel() {
		return traceLevel.getValue();
	}

	public long getValueEfaVersionLastCheck() {
		return efaVersionLastCheck.getValue();
	}

	public void setValueEfaVersionLastCheck(long timestamp) {
		setValue(efaVersionLastCheck, Long.toString(timestamp));
	}

	public long getValueJavaVersionLastCheck() {
		return javaVersionLastCheck.getValue();
	}

	public void setValueJavaVersionLastCheck(long timestamp) {
		setValue(javaVersionLastCheck, Long.toString(timestamp));
	}

	public String getValueVersion() {
		return version.getValue();
	}

	public void setValueVersion(String versionID) {
		setValue(version, versionID);
	}

	public boolean getValueEfaDirekt_zielBeiFahrtbeginnPflicht() {
		return efaDirekt_zielBeiFahrtbeginnPflicht.getValue();
	}

	public boolean getValueEfaDirekt_gewaesserBeiUnbekanntenZielenPflicht() {
		return efaDirekt_gewaesserBeiUnbekanntenZielenPflicht.getValue();
	}

	public boolean getValueEfaDirekt_eintragErzwingeObmann() {
		return efaDirekt_eintragErzwingeObmann.getValue();
	}

	public boolean getValueCheckAllowedPersonsInBoat() {
		return efaDirekt_checkAllowedGroupsForBoat.getValue();
	}

	public boolean getValueCheckMinOnePersonsFromGroupInBoat() {
		return efaDirekt_checkAllowedMinGroupForBoat.getValue();
	}

	public boolean getValueEfaDirekt_eintragErlaubeNurMaxRudererzahl() {
		return efaDirekt_eintragErlaubeNurMaxRudererzahl.getValue();
	}

	public boolean getValueWarnEvenNonCriticalBoatDamages() {
		return efaDirekt_warnEvenNonCriticalBoatDamages.getValue();
	}

	public boolean getValueEfaDirekt_eintragNichtAenderbarUhrzeit() {
		return efaDirekt_eintragNichtAenderbarUhrzeit.getValue();
	}

	public boolean getValueEfaDirekt_eintragNichtAenderbarKmBeiBekanntenZielen() {
		return efaDirekt_eintragNichtAenderbarKmBeiBekanntenZielen.getValue();
	}

	public boolean getValueFixCoxForCoxlessUnknownBoats() {
		return fixCoxForCoxlessUnknownBoats.getValue();
	}	
	
	public boolean getValueEfaDirekt_eintragNurBekannteBoote() {
		return efaBoathouseOnlyEnterKnownBoats.getValue();
	}

	public boolean getValueEfaDirekt_eintragNurBekannteRuderer() {
		return efaBoathouseOnlyEnterKnownPersons.getValue();
	}

	public boolean getValueEfaDirekt_eintragNurBekannteZiele() {
		return efaBoathouseOnlyEnterKnownDestinations.getValue();
	}

	public boolean getValueEfaDirekt_eintragNurBekannteGewaesser() {
		return efaBoathouseOnlyEnterKnownWaters.getValue();
	}

	public boolean getValueBoathouseStrictUnknownPersons() {
		return efaBoathouseStrictUnknownPersons.getValue();
	}

	public String getValueBoathouseNonAllowedUnknownPersonNames() {
		return efaBoathouseNonAllowedUnknownPersonNames.getValue();
	}

	public boolean getValueEfaDirekt_eintragHideUnnecessaryInputFields() {
		return efaDirekt_eintragHideUnnecessaryInputFields.getValue();
	}
	
	public boolean getValueEfaDirekt_eintragPresentLastTripOnNewEntry() {
		return efaDirekt_eintragPresentLastTripOnNewEntry.getValue();
	}
	public boolean getValueEfaDirekt_eintragPresentLastTripOnLateEntry(){
		return efaDirekt_eintragPresentLastTripOnLateEntry.getValue();
	}
	public int 	getValueEfaDirekt_eintragPresentLastTripTimeout(){
		return efaDirekt_eintragPresentLastTripTimeout.getValue();
	}	

	public boolean getValueEfaDirekt_MultisessionSupportStartSession() {
		return efaDirekt_MultisessionSupportStartSession.getValue();
	}
	public boolean getValueEfaDirekt_MultisessionSupportLateEntry(){
		return efaDirekt_MultisessionSupportLateEntry.getValue();
	}	
	public boolean getValueEfaDirekt_MultisessionLastGuiElemParticipants() {
		return efaDirekt_MultisessionLastGuiElemParticipants.getValue();
	}	
	public int getValueEfaDirekt_plusMinutenAbfahrt() {
		return efaDirekt_plusMinutenAbfahrt.getValue();
	}

	public int getValueEfaDirekt_minusMinutenAnkunft() {
		return efaDirekt_minusMinutenAnkunft.getValue();
	}

	public boolean getValueAllowEnterEndDate() {
		return allowEnterEndDate.getValue();
	}

	public boolean getValueEfaDirekt_mitgliederDuerfenReservieren() {
		return membersMayReserveBoats.getValue() || membersMayReserveBoatsWeekly.getValue();
	}

	public boolean getValueEfaDirekt_mitgliederDuerfenReservierenZyklisch() {
		return membersMayReserveBoatsWeekly.getValue();
	}

	public boolean getValueMembersMayReservePrivateBoats() {
		return membersMayReservePrivateBoats.getValue();
	}

	public boolean getValueEfaDirekt_mitgliederDuerfenReservierungenEditieren() {
		return membersMayEditBoatsReservations.getValue();
	}

	public boolean getValueEfaDirekt_mitgliederDuerfenEfaBeenden() {
		return efaDirekt_mitgliederDuerfenEfaBeenden.getValue();
	}

	public boolean getValueEfaDirekt_mitgliederDuerfenNamenHinzufuegen() {
		return efaDirekt_mitgliederDuerfenNamenHinzufuegen.getValue();
	}

	public boolean getValueEfaDirekt_resBooteNichtVerfuegbar() {
		return efaDirekt_resBooteNichtVerfuegbar.getValue();
	}

	public boolean getValueEfaDirekt_wafaRegattaBooteAufFahrtNichtVerfuegbar() {
		return efaDirekt_wafaRegattaBooteAufFahrtNichtVerfuegbar.getValue();
	}

	public boolean getValueEfaDirekt_boatListShowForeignLogbookSessionsAsNotAvailable() {
		return efaDirekt_boatListShowForeignLogbookSessionsAsNotAvailable.getValue();
	}
	
	public int getValueEfaDirekt_resLookAheadTime() {
		return efaDirekt_resLookAheadTime.getValue();
	}

	public String getValueEfaDirekt_execOnEfaExit() {
		return efaDirekt_execOnEfaExit.getValue();
	}

	public DataTypeTime getValueEfaDirekt_exitTime() {
		return efaDirekt_exitTime.getTime();
	}

	public int getValueEfaDirekt_exitIdleTime() {
		return efaDirekt_exitIdleTime.getValue();
	}

	public String getValueEfaDirekt_execOnEfaAutoExit() {
		return efaDirekt_execOnEfaAutoExit.getValue();
	}

	public DataTypeTime getValueEfaDirekt_restartTime() {
		return efaDirekt_restartTime.getTime();
	}

	public ItemTypeConfigButton getValueEfaDirekt_butFahrtBeginnen() {
		return efaDirekt_butFahrtBeginnen;
	}

	public ItemTypeConfigButton getValueEfaDirekt_butFahrtBeenden() {
		return efaDirekt_butFahrtBeenden;
	}

	public ItemTypeConfigButton getValueEfaDirekt_butFahrtAbbrechen() {
		return efaDirekt_butFahrtAbbrechen;
	}

	public ItemTypeConfigButton getValueEfaDirekt_butNachtrag() {
		return efaDirekt_butNachtrag;
	}

	public ItemTypeConfigButton getValueEfaDirekt_butBootsreservierungen() {
		return efaDirekt_butBootsreservierungen;
	}

	public ItemTypeConfigButton getValueEfaDirekt_butFahrtenbuchAnzeigen() {
		return efaDirekt_butFahrtenbuchAnzeigen;
	}

	public ItemTypeConfigButton getValueEfaDirekt_butStatistikErstellen() {
		return efaDirekt_butStatistikErstellen;
	}

	public ItemTypeConfigButton getValueEfaDirekt_butVereinsarbeit() {
		return efaDirekt_butVereinsarbeit;
	}

	public ItemTypeConfigButton getValueEfaDirekt_butNachrichtAnAdmin() {
		return efaDirekt_butNachrichtAnAdmin;
	}

	public ItemTypeConfigButton getValueEfaDirekt_butAdminModus() {
		return efaDirekt_butAdminModus;
	}

	public ItemTypeConfigButton getValueEfaDirekt_butSpezial() {
		return efaDirekt_butSpezial;
	}

	public String getValueEfaDirekt_butSpezialCmd() {
		return efaDirekt_butSpezialCmd.getValue();
	}

	public ItemTypeConfigButton getValueEfaDirekt_butHelp() {
		return efaDirekt_butHelp;
	}

	public boolean getValueEfaDirekt_showButtonHotkey() {
		return efaDirekt_showButtonHotkey.getValue();
	}

	public boolean getValueEfaDirekt_sortByAnzahl() {
		return efaDirekt_sortByAnzahl.getValue();
	}

	public boolean getValueEfaDirekt_sortByRigger() {
		return efaDirekt_sortByRigger.getValue();
	}

	public boolean getValueEfaDirekt_sortByType() {
		return efaDirekt_sortByType.getValue();
	}

	public boolean getValueEfaDirekt_boatListIndividualOthers() {
		return efaDirekt_boatListIndividualOthers.getValue();
	}

	public boolean getValueEfaDirekt_autoPopupOnBoatLists() {
		return efaDirekt_autoPopupOnBoatLists.getValue();
	}

	public boolean getValueEfaDirekt_listAllowToggleBoatsPersons() {
		return efaDirekt_listAllowToggleBoatsPersons.getValue();
	}

	public boolean getValueEfaDirekt_showEingabeInfos() {
		return efaDirekt_showEingabeInfos.getValue();
	}

	public boolean getValueEfaDirekt_showBootsschadenButton() {
		return efaDirekt_showBootsschadenButton.getValue();
	}

	public boolean getValueEfaDirekt_showBoatNotCleanedButton() {
		return efaDirekt_showBoatNotCleanedButton.getValue();
	}

	public int getValueEfaDirekt_maxFBAnzeigenFahrten() {
		return efaDirekt_maxFBAnzeigenFahrten.getValue();
	}

	public int getValueEfaDirekt_anzFBAnzeigenFahrten() {
		return efaDirekt_anzFBAnzeigenFahrten.getValue();
	}

	public boolean getValueEfaDirekt_FBAnzeigenAuchUnvollstaendige() {
		return efaDirekt_FBAnzeigenAuchUnvollstaendige.getValue();
	}

	public int getValueNotificationWindowTimeout() {
		return efaDirekt_notificationWindowTimeout.getValue();
	}

	public int getValueListSizeUnavailableBoats() {
		return efaDirekt_boatsNotAvailableListSize.getValue();
	}

	public int getValueEfaDirekt_BthsFontSize() {
		return efaDirekt_BthsFontSize.getValue();
	}

	public int getValueEfaDirekt_BthsTableFontSize() {
		return efaDirekt_BthsTableFontSize.getValue();
	}

	public String getValueEfaDirekt_BthsFontStyle() {
		return efaDirekt_BthsFontStyle.getValue();
	}
	
	public String getValueEfaDirekt_BthsFontName() {
		return efaDirekt_BthsFontNameButton.getValueFromField();
	}

	public String getValue_OtherFontName() {
		return efa_OtherFontNameButton.getValueFromField();
	}
	
	public int getValue_OtherFontSize() {
		return efa_otherFontSize.getValue();
	}

	public int getValue_OtherTableFontSize() {
		return efa_otherTableFontSize.getValue();
	}

	public String getValue_OtherFontStyle() {
		return efa_otherFontStyle.getValue();
	}

	
	public boolean getValueEfaDirekt_colorizeInputField() {
		return efaDirekt_colorizeInputField.getValue();
	}

	public boolean getValueEfaDirekt_showZielnameFuerBooteUnterwegs() {
		return efaDirekt_showZielnameFuerBooteUnterwegs.getValue();
	}

	public String getValueEfadirekt_adminLastOsCommand() {
		return efadirekt_adminLastOsCommand.getValue();
	}

	public void setValueEfadirekt_adminLastOsCommand(String cmd) {
		setValue(efadirekt_adminLastOsCommand, cmd);
	}

	public long getValueLastBoatDamageReminder() {
		return efadirekt_lastBoatDamangeReminder.getValue();
	}

	public void setValueLastBoatDamageReminder(long timestamp) {
		setValue(efadirekt_lastBoatDamangeReminder, Long.toString(timestamp));
	}

	public String getValueEfaDirekt_vereinsLogo() {
		return efaDirekt_vereinsLogo.getValue();
	}

	public boolean getValueEfaBoathouseShowLastFromWaterNotification() {
		return efaBoathouseShowLastFromWaterNotification.getValue();
	}

	public String getValueEfaBoathouseShowLastFromWaterNotificationText() {
		return efaBoathouseShowLastFromWaterNotificationText.getValue();
	}

	public boolean getValueEfaBoathouseFilterTextfieldStandardLists() {
		return efaBoathouseFilterTextfieldStandardLists.getValue();
	}

	public boolean getValueEfaBoathouseFilterTextfieldBoatsNotAvailableList() {
		return efaBoathouseFilterTextfieldBoatsNotAvailableList.getValue();
	}

	public boolean getValueEfaBoathouseFilterTextfieldEasyFindEntriesWithSpecialCharacters() {
		return efaBoathouseFilterTextfieldEasyFindEntriesWithSpecialCharacters.getValue();
	}

	public int getValueEfaBoathouseFilterTextAutoClearInterval() {
		return efaBoathouseFilterTextAutoClearInterval.getValue();
	}

	public boolean getValueEfaBoathouseFilterTextAutoClearAfterAction() {
		return efaBoathouseFilterTextAutoClearAfterAction.getValue();
	}

	public boolean getValueEfaBoathouseTwoColumnList() {
		return efaBoathouseTwoColumnList.getValue();
	}

	public boolean getValueEfaBoathouseExtdToolTips() {
		return efaBoathouseExtdToolTips.getValue();
	}

	public int getValueEfaBoathouseExtdToolTipInitialDelayMsec() {
		return efaBoathouseExtdToolTipInitialDelayMsec.getValue();
	}

	public int getValueEfaBoathouseExtdToolTipDismissDelayMsec() {
		return efaBoathouseExtdToolTipDismissDelayMsec.getValue();
	}

	public boolean getValueEfaBoathouseBoatListReservationInfo() {
		return efaBoathouseBoatListWithReservationInfo.getValue();
	}

	public boolean getValueEfaDirekt_showUhr() {
		return efaDirekt_showUhr.getValue();
	}

	public boolean getValueEfaDirekt_showNews() {
		return efaDirekt_showNews.getValue();
	}

	public String getValueEfaDirekt_newsText() {
		return efaDirekt_newsText.getValue();
	}

	public int getValueEfaDirekt_newsScrollSpeed() {
		return efaDirekt_newsScrollSpeed.getValue();
	}

	public boolean getValueEfaDirekt_startMaximized() {
		return efaDirekt_startMaximized.getValue();
	}

	public boolean getValueEfaDirekt_startMaximizedRespectTaskbar() {
		return efaDirekt_startMaximizedRespectTaskbar.getValue();
	}
	
	public boolean getValueEfaDirekt_fensterNichtVerschiebbar() {
		return efaDirekt_fensterNichtVerschiebbar.getValue();
	}

	public boolean getValueEfaDirekt_immerImVordergrund() {
		return efaDirekt_immerImVordergrund.getValue();
	}

	public boolean getValueEfaDirekt_immerImVordergrundBringToFront() {
		return efaDirekt_immerImVordergrundBringToFront.getValue();
	}

	public boolean getValueEfaDirekt_tabelleShowTooltip() {
		return efaDirekt_tabelleShowTooltip.getValue();
	}

	public boolean getValueEfaDirekt_tabelleAlternierendeZeilenFarben() {
		return efaDirekt_tabelleAlternierendeZeilenfarben.getValue();
	}

	public boolean getValueEfaDirekt_tabelleEasyfindEntriesWithSpecialCharacters() {
		return efaDirekt_tabelleEasyFindEntriesWithSpecialCharacters.getValue();
	}

	public String getValueEfaDirekt_bnrMsgToAdminDefaultRecipient() {
		return efaDirekt_bnrMsgToAdminDefaultRecipient.getValue();
	}

	public boolean getValueEfaDirekt_bnrError_admin() {
		return efaDirekt_bnrError_admin.getValue();
	}

	public boolean getValueEfaDirekt_bnrError_bootswart() {
		return efaDirekt_bnrError_bootswart.getValue();
	}

	public boolean getValueEfaDirekt_bnrWarning_admin() {
		return efaDirekt_bnrWarning_admin.getValue();
	}

	public boolean getValueEfaDirekt_bnrWarning_bootswart() {
		return efaDirekt_bnrWarning_bootswart.getValue();
	}

	public boolean getValueEfaDirekt_bnrBootsstatus_admin() {
		return efaDirekt_bnrBootsstatus_admin.getValue();
	}

	public boolean getValueEfaDirekt_bnrBootsstatus_bootswart() {
		return efaDirekt_bnrBootsstatus_bootswart.getValue();
	}

	public long getValueEfaDirekt_bnrWarning_lasttime() {
		return efaDirekt_bnrWarning_lasttime.getValue();
	}

	public void setValueEfaDirekt_bnrWarning_lasttime(long lasttime) {
		setValue(efaDirekt_bnrWarning_lasttime, Long.toString(lasttime));
	}

	public boolean getValueNotificationMarkReadAdmin() {
		return notificationMarkReadAdmin.getValue();
	}

	public boolean getValueNotificationMarkReadBoatMaintenance() {
		return notificationMarkReadBoatMaintenance.getValue();
	}

	public boolean getValueNotificationNewBoatDamageByAdmin() {
		return notificationNewBoatDamageByAdmin.getValue();
	}
	
	public String getValueEfaDirekt_emailServer() {
		return efaDirekt_emailServer.getValue();
	}

	public int getValueEfaDirekt_emailPort() {
		return efaDirekt_emailPort.getValue();
	}

	public boolean getValueEmailSSL() {
		return SECURITY_SSL.equals(efaDirekt_emailSecurity.getValue());
	}

	public String getValueEfaDirekt_emailAbsender() {
		return efaDirekt_emailAbsender.getValue();
	}

	public String getValueEfaDirekt_emailUsername() {
		return efaDirekt_emailUsername.getValue();
	}

	public String getValueEfaDirekt_emailPassword() {
		return efaDirekt_emailPassword.getValue();
	}

	public String getValueEfaDirekt_emailAbsenderName() {
		return efaDirekt_emailAbsenderName.getValue();
	}

	public String getValueEfaDirekt_emailBetreffPraefix() {
		return efaDirekt_emailBetreffPraefix.getValue();
	}

	public String getValueEfaDirekt_emailSignatur() {
		return efaDirekt_emailSignatur.getValue();
	}

	public String getValueEfaDirekt_lockEfaShowHtml() {
		return efaDirekt_lockEfaShowHtml.getValue();
	}

	public boolean getValueEfaDirekt_lockEfaVollbild() {
		return efaDirekt_lockEfaVollbild.getValue();
	}

	public DataTypeDate getValueEfaDirekt_lockEfaFromDatum() {
		return efaDirekt_lockEfaFromDatum.getDate();
	}

	public void setValueEfaDirekt_lockEfaFromDatum(DataTypeDate date) {
		setValue(efaDirekt_lockEfaFromDatum, (date != null ? date.toString() : ""));
	}

	public DataTypeTime getValueEfaDirekt_lockEfaFromZeit() {
		return efaDirekt_lockEfaFromZeit.getTime();
	}

	public void setValueEfaDirekt_lockEfaFromZeit(DataTypeTime time) {
		setValue(efaDirekt_lockEfaFromZeit, (time != null ? time.toString() : ""));
	}

	public DataTypeDate getValueEfaDirekt_lockEfaUntilDatum() {
		return efaDirekt_lockEfaUntilDatum.getDate();
	}

	public void setValueEfaDirekt_lockEfaUntilDatum(DataTypeDate date) {
		setValue(efaDirekt_lockEfaUntilDatum, (date != null ? date.toString() : ""));
	}

	public DataTypeTime getValueEfaDirekt_lockEfaUntilZeit() {
		return efaDirekt_lockEfaUntilZeit.getTime();
	}

	public void setValueEfaDirekt_lockEfaUntilZeit(DataTypeTime time) {
		setValue(efaDirekt_lockEfaUntilZeit, (time != null ? time.toString() : ""));
	}

	public boolean getValueEfaDirekt_locked() {
		return efaDirekt_locked.getValue();
	}

	public void setValueEfaDirekt_locked(boolean locked) {
		setValue(efaDirekt_locked, Boolean.toString(locked));
	}

	public boolean getValueUseFunctionalityRowing() {
		return useFunctionalityRowing.getValue();
	}

	public boolean getValueUseFunctionalityRowingGermany() {
		return useFunctionalityRowingGermany.getValue();
	}

	public boolean getValueUseFunctionalityRowingBerlin() {
		return useFunctionalityRowingBerlin.getValue();
	}

	public boolean getValueUseFunctionalityCanoeing() {
		return useFunctionalityCanoeing.getValue();
	}

	public boolean getValueUseFunctionalityCanoeingGermany() {
		return useFunctionalityCanoeingGermany.getValue();
	}

	public boolean getDeveloperFunctionsActivated() {
		return developerFunctions.getValue();
	}

	public boolean getExperimentalFunctionsActivated() {
		return experimentalFunctions.getValue();
	}

	public String getValueEfaUserDirectory() {
		return efaUserDirectory.getValue();
	}

	public String getValueLanguage() {
		return language.getValue();
	}

	public ItemTypeAction getValueTypesResetToDefault() {
		return typesResetToDefault;
	}

	public ItemTypeAction getValueTypesAddAllToDefault() {
		return typesAddAllDefault;
	}

	public ItemTypeAction getValueTypesAddAllDefaultRowingBoats() {
		return typesAddAllDefaultRowingBoats;
	}

	public ItemTypeAction getValueTypesAddAllDefaultCanoeingBoats() {
		return typesAddAllDefaultCanoeingBoats;
	}

	public ItemTypeHashtable<String> getValueTypesGender() {
		return typesGender;
	}

	public ItemTypeHashtable<String> getValueTypesBoat() {
		return typesBoat;
	}

	public ItemTypeHashtable<String> getValueTypesNumSeats() {
		return typesNumSeats;
	}

	public ItemTypeHashtable<String> getValueTypesRigging() {
		return typesRigging;
	}

	public ItemTypeHashtable<String> getValueTypesCoxing() {
		return typesCoxing;
	}

	public ItemTypeHashtable<String> getValueTypesSession() {
		return typesSession;
	}

	public ItemTypeHashtable<String> getValueTypesStatus() {
		return typesStatus;
	}

	public boolean getValueClubworkRequiresApproval() {
		return clubworkRequiresApproval.getValue();
	}

	public String getValueKanuEfb_urlLogin() {
		return kanuEfb_urlLogin.getValue();
	}

	public String getValueKanuEfb_urlRequest() {
		return kanuEfb_urlRequest.getValue();
	}

	public DataTypeDate getValueKanuEfb_SyncTripsAfterDate() {
		return kanuEfb_SyncTripsAfterDate.getDate();
	}

	public Boolean getValueKanuEfb_FullSync() {
		return kanuEfb_Fullsync.getValue();
	}
	
	public Boolean getValueKanuEfb_AlwaysShowKanuEFBFields() {
		return kanuEfb_AlwaysShowKanuEFBFields.getValue();
	}

	public Boolean getValueKanuEfb_SyncUnknownBoats() {
		return kanuEfb_SyncUnknownBoats.getValue();
	}

	public Boolean getValueKanuEfb_TidyXML() {
		return kanuEfb_TidyXML.getValue();
	}
	
	public Boolean getValueKanuEfb_SyncTripTypePrefix() {
		return kanuEfb_SyncTripTypePrefix.getValue();
	}

	public boolean getValueDataPreModifyRecordCallbackEnabled() {
		return dataPreModifyRecordCallbackEnabled.getValue();
	}

	public boolean getValueDataAuditCorrectErrors() {
		return dataAuditCorrectErrors.getValue();
	}

	public long getValueDataFileSaveInterval() {
		return dataFileSaveInterval.getValue();
	}

	public long getValueDataFileLockTimeout() {
		return dataFileLockTimeout.getValue();
	}

	public boolean getValueDataFileSynchronousJournal() {
		return dataFileSynchronousJournal.getValue();
	}

	public String getValueDataBackupDirectory() {
		return dataBackupDirectory.getValue();
	}

	public String getValueDataMirrorDirectory() {
		return dataMirrorDirectory.getValue();
	}

	public boolean getValueDataRemoteEfaServerEnabled() {
		return dataRemoteEfaServerEnabled.getValue();
	}

	public int getValueDataataRemoteEfaServerPort() {
		return dataRemoteEfaServerPort.getValue();
	}

	public String getValueDataRemoteEfaOnlineUrl() {
		return dataRemoteEfaOnlineUrl.getValue();
	}

	public boolean getValueDataRemoteEfaOnlineEnabled() {
		return dataRemoteEfaOnlineEnabled.getValue();
	}

	public String getValueDataRemoteEfaOnlineUsername() {
		return dataRemoteEfaOnlineUsername.getValue();
	}

	public String getValueDataRemoteEfaOnlinePassword() {
		return dataRemoteEfaOnlinePassword.getValue();
	}

	public long getValueDataRemoteEfaOnlineUpdateInterval() {
		return dataRemoteEfaOnlineUpdateInterval.getValue();
	}

	public long getValueDataRemoteCacheExpiryTime() {
		return dataRemoteCacheExpiryTime.getValue();
	}

	public long getValueDataRemoteIsOpenExpiryTime() {
		return dataRemoteIsOpenExpiryTime.getValue();
	}

	public long getValueDataRemoteLoginFailureRetryTime() {
		return dataRemoteLoginFailureRetryTime.getValue();
	}

	public long getValueDataRemoteClientReceiveTimeout() {
		return dataRemoteClientReceiveTimeout.getValue();
	}

	public long getValueDataRemoteEfaCloudSynchIntervalSecs() {
		return dataRemoteEfaCloudSynchIntervalSecs.getValue();
	}

	public Vector<IWidget> getWidgets() {
		return widgets;
	}

	public ItemTypeItemList getCrontabItems() {
		return crontab;
	}

	public String getTranslateLanguageWork() {
		return translateLanguageWork.getValue();
	}

	public void setTranslateLanguageWork(String lang) {
		setValue(translateLanguageWork, lang);
	}

	public String getTranslateLanguageBase() {
		return translateLanguageBase.getValue();
	}

	public void setTranslateLanguageBase(String lang) {
		setValue(translateLanguageBase, lang);
	}

	public IItemType getExternalGuiItem(String name) {
		synchronized (configValues) {
			IItemType item = configValues.get(name);
			if (item == null) {
				return null;
			}
			return item.copyOf();
		}
	}

	public Color getEfaGuiflatLaf_Background() {
		Color myColor = efaGuiflatLaf_Background.getColor();
		return (myColor != null ? myColor : standardTableSelectionBackgroundColor);
	}

	public int getEfaGuiflatLaf_BackgroundFieldsLightenPercentage() {
		return efaGuiflatLaf_BackgroundFieldsLightenPercentage.getValue();
	}

	public Color getEfaGuiflatLaf_AccentColor() {
		Color myColor = efaGuiflatLaf_AccentColor.getColor();
		return (myColor != null ? myColor : standardTableSelectionBackgroundColor);
	}

	public Color getEfaGuiflatLaf_FocusColor() {
		Color myColor = efaGuiflatLaf_FocusColor.getColor();
		return (myColor != null ? myColor : standardFlatLafFocusColor);
	}

	public Color getTableSelectionBackgroundColor() {
		Color myColor = efaGuiTableSelectionBackground.getColor();
		return (myColor != null ? myColor : standardTableSelectionBackgroundColor);
	}

	public Color getTableSelectionForegroundColor() {
		Color myColor = efaGuiTableSelectionForeground.getColor();
		return (myColor != null ? myColor : standardTableSelectionForegroundColor);
	}

	public Color getTableHeaderBackgroundColor() {
		Color myColor = efaGuiTableHeaderBackground.getColor();
		return (myColor != null ? myColor : standardTableHeaderBackgroundColor);
	}

	public Color getTableHeaderHeaderColor() {
		Color myColor = efaGuiTableHeaderForeground.getColor();
		return (myColor != null ? myColor : standardTableHeaderForegroundColor);
	}

	public Color getTableAlternatingRowColor() {
		Color myColor = efaGuiTableAlternatingRowColorValue.getColor();
		return (myColor != null ? myColor : standardTableAlternatingRowColor);
	}

	public Boolean getToolTipSpecialColors() {
		return efaGuiToolTipSpecialColors.getValue();
	}	
	
	public Color getToolTipBackgroundColor() {
		Color myColor = efaGuiToolTipBackground.getColor();
		return (myColor != null ? myColor : standardToolTipBackgroundColor);
	}

	public Color getToolTipForegroundColor() {
		Color myColor = efaGuiToolTipForeground.getColor();
		return (myColor != null ? myColor : standardToolTipForegroundColor);
	}

	public Color getToolTipHeaderBackgroundColor() {
		Color myColor = efaGuiToolTipHeaderBackground.getColor();
		return (myColor != null ? myColor : standardToolTipHeaderBackgroundColor);
	}

	public Color getToolTipHeaderForegroundColor() {
		Color myColor = efaGuiToolTipHeaderForeground.getColor();
		return (myColor != null ? myColor : standardToolTipHeaderForegroundColor);
	}
	
	public Boolean getHeaderUseHighlightColor() {
		return efaHeaderUseHighlightColor.getValue();
	}

	public Color getHeaderBackgroundColor() {
		Color myColor = efaHeaderBackgroundColor.getColor();
		return (myColor != null ? myColor : standardTableSelectionBackgroundColor);
	}

	public Color getHeaderForegroundColor() {
		Color myColor = efaHeaderForegroundColor.getColor();
		return (myColor != null ? myColor : standardTableSelectionForegroundColor);
	}

	public Boolean getHeaderUseForTabbedPanes() {
		return efaHeaderUseForTabbedPanes.getValue();
	}

	public String getWeeklyReservationConflictBehaviour() {
		return weeklyReservationConflictBehaviour.getValue();
	}

	public Vector<IItemType> getGuiItems() {
		Vector<IItemType> items = new Vector<IItemType>();
		for (int i = 0; i < configValueNames.size(); i++) {
			items.add(getExternalGuiItem(configValueNames.get(i)));
		}
		return items;
	}

	public String[] getParameterNames() {
		String[] names = new String[configValueNames.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = configValueNames.get(i);
		}
		return names;
	}

	public void checkNewConfigValues() {
		Hashtable<String, String> changedSettings = new Hashtable<String, String>();
		synchronized (configValues) {
			for (int i = 0; i < configValueNames.size(); i++) {
				IItemType item = configValues.get(configValueNames.get(i));
				if (item != null && item.isChanged()) {
					if (item == efaDirekt_BthsFontSize || item == efaDirekt_BthsTableFontSize || item == efaDirekt_BthsFontNameButton 
							|| item == efaDirekt_BthsFontStyle || item == efaDirekt_listAllowToggleBoatsPersons
							|| item == efaDirekt_autoPopupOnBoatLists || item == efaDirekt_fensterNichtVerschiebbar
							|| item == efaDirekt_startMaximized || item == efaDirekt_startMaximizedRespectTaskbar
							|| item == useFunctionalityRowing || item == useFunctionalityRowingGermany
							|| item == useFunctionalityRowingBerlin || item == useFunctionalityCanoeing
							|| item == useFunctionalityCanoeingGermany || item == developerFunctions
							|| item == experimentalFunctions || item == this.dataRemoteEfaServerEnabled
							|| item == this.dataRemoteEfaServerPort || item == this.popupContainsMode
							|| item == efaBoathouseFilterTextfieldStandardLists
							|| item == efaBoathouseFilterTextfieldBoatsNotAvailableList
							|| item == efaDirekt_boatsNotAvailableListSize || item == windowXOffset
							|| item == windowYOffset || item == screenWidth || item == screenHeight
							|| item == efaHeaderUseForTabbedPanes || item == lookAndFeel
							|| item == efa_otherFontSize || item == efa_otherFontStyle || item == efa_otherTableFontSize
							|| item == efaGuiToolTipSpecialColors) {
						changedSettings.put(item.getDescription(), "foo");
					}
				}
			}
		}
		if (changedSettings.size() > 0) {
			String[] keys = changedSettings.keySet().toArray(new String[0]);
			String s = null;
			for (int i = 0; i < keys.length; i++) {
				s = (s != null ? s + "\n" : "") + "'" + keys[i] + "'";
			}
			Dialog.infoDialog(International.getString("Geänderte Einstellungen"),
					LogString.onlyEffectiveAfterRestart(International.getString("Geänderte Einstellungen")) + "\n" + s);
		}

		if (Daten.isEfaFlatLafActive()) {
			EfaFlatLafHelper.setupEfaFlatLafDefaults();
		}
        
		if (this.getToolTipSpecialColors()) {
        	Dialog.getUiDefaults().put("ToolTip.background", new ColorUIResource(this.getToolTipBackgroundColor()));
        	Dialog.getUiDefaults().put("ToolTip.foreground", new ColorUIResource(this.getToolTipForegroundColor()));
        }		

	}

	public void setExternalParameters(boolean isGuiConfigChange) {
		// first, set all external parameters that have to be set in any case, e.g. also
		// if EfaConfig has been read after startup

		// set Debug Logging and Trace Topic (will only take effect if it has not been
		// set through command line previously!!)
		Logger.setDebugLogging(debugLogging.getValue(), false);
		Logger.setTraceTopic(traceTopic.getValue(), false);
		Logger.setTraceLevel(traceLevel.getValue(), false);

		DataLocks.setLockTimeout(dataFileLockTimeout.getValue());

		// if isGuiConfigChange, i.e. if interactive changes have been made by the user,
		// set other parameters as well
		if (!isGuiConfigChange) {
			if (dataBackupDirectory != null && dataBackupDirectory.getValue() != null) {
				Daten.trySetEfaBackupDirectory(dataBackupDirectory.getValue());
			}
			return;
		}

		// Types
		myEfaTypes = getMyEfaTypes();
		if (myEfaTypes != null) {
			boolean changed = false;
			if (updateTypes(myEfaTypes, EfaTypes.CATEGORY_GENDER, typesGender)) {
				changed = true;
			}
			if (updateTypes(myEfaTypes, EfaTypes.CATEGORY_BOAT, typesBoat)) {
				changed = true;
			}
			if (updateTypes(myEfaTypes, EfaTypes.CATEGORY_NUMSEATS, typesNumSeats)) {
				changed = true;
			}
			if (updateTypes(myEfaTypes, EfaTypes.CATEGORY_RIGGING, typesRigging)) {
				changed = true;
			}
			if (updateTypes(myEfaTypes, EfaTypes.CATEGORY_COXING, typesCoxing)) {
				changed = true;
			}
			if (updateTypes(myEfaTypes, EfaTypes.CATEGORY_SESSION, typesSession)) {
				changed = true;
			}
			if (updateTypes(myEfaTypes, EfaTypes.CATEGORY_STATUS, typesStatus)) {
				changed = true;
			}
			if (changed) {
				Dialog.infoDialog(International.getString("Geänderte Einstellungen"),
						LogString.onlyEffectiveAfterRestart(International.getString("Geänderte Einstellungen")) + "\n"
								+ International.getString("Bezeichnungen"));
			}
		}

		if (data().getStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
			return; // don't allow changes to Language and especially UserDataDir!
		}

		// Language & efa User Data
		String newLang = language.toString();
		String newUserData = efaUserDirectory.toString();
		if (!newUserData.endsWith(Daten.fileSep)) {
			newUserData += Daten.fileSep;
		}
		boolean changedLang = Daten.efaBaseConfig.language == null || !Daten.efaBaseConfig.language.equals(newLang);
		boolean changedDateFormat = DataTypeDate.MONTH_DAY_YEAR.equals(dateFormat) == Daten.dateFormatDMY;
		boolean changedUserDir = Daten.efaBaseConfig.efaUserDirectory == null
				|| !Daten.efaBaseConfig.efaUserDirectory.equals(newUserData);
		if (changedLang || changedUserDir) {
			if (changedLang) {
				Daten.efaBaseConfig.language = newLang;
			}
			if (changedUserDir) {
				if (Daten.efaBaseConfig.efaCanWrite(newUserData, false)) {
					Daten.efaBaseConfig.efaUserDirectory = newUserData;
					EfaSec.createNewSecFile(Daten.efaBaseConfig.efaUserDirectory + Daten.EFA_SECFILE,
							Daten.efaProgramDirectory + Daten.EFA_JAR);
				} else {
					Dialog.infoDialog(International.getString("Verzeichnis für Nutzerdaten"), International.getString(
							"efa kann in dem geänderten Verzeichnis für Nutzerdaten nicht schreiben. Die Änderung wird ignoriert."));
					changedUserDir = false;
				}
			}
			if (changedLang || changedUserDir) {
				Daten.efaBaseConfig.writeFile();
			}
			if (changedLang) {
				myEfaTypes.setToLanguage(newLang);
				Dialog.infoDialog(International.getString("Geänderte Einstellungen"),
						LogString.onlyEffectiveAfterRestart(International.getString("Geänderte Einstellungen")) + "\n"
								+ International.getString("Sprache"));
			}
			if (changedUserDir) {
				Dialog.infoDialog(International.getString("Verzeichnis für Nutzerdaten"),
						LogString.onlyEffectiveAfterRestart(International.getString("Verzeichnis für Nutzerdaten")));
			}
		} else {
			if (changedDateFormat) {
				Dialog.infoDialog(International.getString("Geänderte Einstellungen"),
						LogString.onlyEffectiveAfterRestart(International.getString("Geänderte Einstellungen")) + "\n"
								+ International.getString("Datumsformat"));
			}
		}

		// Backup Directory
		String newBakDir = dataBackupDirectory.toString();
		Daten.trySetEfaBackupDirectory(newBakDir);
	}

	public boolean setToLanguate(String lang) {
		ResourceBundle bundle = null;
		if (lang != null) {
			try {
				bundle = ResourceBundle.getBundle(International.BUNDLE_NAME, new Locale(lang));
			} catch (Exception e) {
				Logger.log(Logger.ERROR, Logger.MSG_CORE_EFACONFIGFAILEDSTVALUES,
						"Failed to set EfaConfig values for language " + lang + ".");
				return false;
			}
		} else {
			bundle = International.getResourceBundle();
		}
		efaDirekt_butFahrtBeginnen.setValueText(International.getString("Fahrt beginnen", bundle));
		efaDirekt_butFahrtBeenden.setValueText(International.getString("Fahrt beenden", bundle));
		return true;
	}

	public void checkForRequiredPlugins() {
	}

	private String searchForProgram(String[] programs) {
		for (int i = 0; i < programs.length; i++) {
			if (new File(programs[i]).isFile()) {
				return programs[i];
			}
		}
		return "";
	}

	public String getRowingAndOrPaddlingString() {
		return (getValueUseFunctionalityRowing() ? International.getString("Ruder", "wie in Ruder-Km") : "")
				+ (getValueUseFunctionalityCanoeing()
						? (getValueUseFunctionalityRowing() ? "/" : "")
								+ International.getString("Paddel", "wie in Paddel-Km")
						: "");
	}

	private String[] makeLookAndFeelArray(int type) {
		UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
		String[] lookAndFeelArray = new String[info.length + 1];
		lookAndFeelArray[0] = (type == STRINGLIST_VALUES ? "" : International.getString("Standard"));
		for (int i = 0; i < info.length; i++) {
			String s = info[i].getClassName();
			if (type == STRINGLIST_DISPLAY) {
				int pos = (s != null ? s.lastIndexOf(".") : -1);
				if (pos > 0 && pos + 1 < s.length()) {
					s = s.substring(pos + 1, s.length());
				}
			}
			lookAndFeelArray[i + 1] = s;
		}
		return lookAndFeelArray;
	}

	private String getDefaultLookAndFeel() {
		String[] laf = makeLookAndFeelArray(STRINGLIST_VALUES);
		for (int i = 0; i < laf.length; i++) {
			if (laf[i].endsWith(Daten.LAF_EFAFLAT_LIGHT)) {
				return laf[i];
			}
		}
		
		// no flatlaf installed? Try MetalLookAndFeel instead.
		for (int i = 0; i < laf.length; i++) {
			if (laf[i].endsWith(Daten.LAF_METAL)) {
				return laf[i];
			}
		}

		return ""; // default
	}
	
/**
 * Gets the default font for the current system, depending on _installed_ fonts. 
 * There are just a few fonts which are really good with efaBoathouse, and this method
 * checks for them in an ordered way. If no special installed font is available, we stick with "Dialog"
 * so that java itself handles which font to use.
 * 
 * @return Font name of the desired default font for this system.
 */
	private String getDefaultFont() {
		// get only installed ui-capable fonts.
		Vector <String> uiFonts = EfaUtil.makeFontFamilyVector(true, null);
		
		String uiFontsString=uiFonts.toString().toLowerCase();
		
		/* the order is optimized for common operating systems:
		 * - Arial, Segoe: -> Windows
		 * - Piboto -> Raspian, efaLive for Raspberry Pi
		 * - Liberation Sans -> Debian, efaLive for x86
		 * - Roboto -> Linux systems like Ubuntu
		 * - Noto Sans Display -> Linux systems like Ubuntu
		 */
		
		if (uiFontsString.matches(".*arial.*")) {
			return "Arial";
		} else if (uiFontsString.matches(".*segoe.ui.*")){
			return "Segoe UI";
		} else if (uiFontsString.matches(".*piboto.*")) {
			return "Piboto";		
		} else if (uiFontsString.matches(".*liberation.sans.*")) {
			return "Liberation Sans";
		} else if (uiFontsString.matches(".*roboto.*")) {
			return "Roboto";				
		} else if (uiFontsString.matches(".*noto.sans.display.*")) {
			return "Noto Sans Display";			
		} else if (uiFontsString.matches(".*noto.sans.*")) {
			return "Noto Sans";		
		}
		return "Dialog";
	}

	private String[] makeLanguageArray(int type) {
		String[] lang = International.getLanguageBundles();
		String[] languages = new String[lang.length + 1];
		languages[0] = (type == STRINGLIST_VALUES ? "" : International.getString("Default"));
		for (int i = 0; i < lang.length; i++) {
			Locale loc = new Locale(lang[i]);
			languages[i + 1] = (type == STRINGLIST_VALUES ? lang[i] : loc.getDisplayName());
		}
		return languages;
	}

	private String[] makeFontStyleArray(int type) {
		String[] styles = new String[3];
		styles[0] = (type == STRINGLIST_VALUES ? "" : International.getString("Default"));
		styles[1] = (type == STRINGLIST_VALUES ? FONT_PLAIN : International.getString("normal"));
		styles[2] = (type == STRINGLIST_VALUES ? FONT_BOLD : International.getString("fett"));
		return styles;
	}
	
	private String[] makeObmannArray(int type) {
		String[] obmann = new String[2];
		obmann[0] = (type == STRINGLIST_VALUES ? OBMANN_BOW : International.getString("Bugmann"));
		obmann[1] = (type == STRINGLIST_VALUES ? OBMANN_STROKE : International.getString("Schlagmann"));
		return obmann;
	}

	public void buildTypes() {
		myEfaTypes = getMyEfaTypes();
		if (myEfaTypes == null) {
			return;
		}
		addParameter(typesGender = new ItemTypeHashtable<String>("_TYPES_GENDER", "", true, IItemType.TYPE_EXPERT,
				BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_TYPES_GEND),
				International.getString("Geschlecht")));
		
		Boolean bCanoeingInGermany= (this.getValueUseFunctionalityCanoeingGermany());
		if (bCanoeingInGermany) {
			addHintWordWrap("BOATS_CANOEING_GERMANY_EFBSYNC_HINT", IItemType.TYPE_EXPERT,
					BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_TYPES_BOAT),
					International.onlyFor("Kanufahren in Deutschland ist aktiv. Nutzen Sie die Kanu-EFB-Synchronisation? Wenn ja, sollten Sie bei Hinzufügen neuer Bootsarten nach einem EFA-Neustart in der Registerkarte SYNCHRONISATION die zu synchronisierenden Bootsarten auf Korrektheit prüfen.", "de"),
					3, 20,10,500);
		}
		addParameter(typesBoat = new ItemTypeHashtable<String>("_TYPES_BOAT", "", true, IItemType.TYPE_EXPERT,
				BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_TYPES_BOAT),
				International.getString("Bootsart")));
		addParameter(typesNumSeats = new ItemTypeHashtable<String>("_TYPES_NUMSEATS", "", true, IItemType.TYPE_EXPERT,
				BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_TYPES_SEAT),
				International.getString("Anzahl Bootsplätze")));
		addParameter(typesRigging = new ItemTypeHashtable<String>("_TYPES_RIGGING", "", true, IItemType.TYPE_EXPERT,
				BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_TYPES_RIGG),
				International.getString("Riggerung")));
		addParameter(typesCoxing = new ItemTypeHashtable<String>("_TYPES_COXING", "", true, IItemType.TYPE_EXPERT,
				BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_TYPES_COXD),
				International.getString("mit/ohne Stm.")));
		addParameter(typesSession = new ItemTypeHashtable<String>("_TYPES_SESSION", "", true, IItemType.TYPE_EXPERT,
				BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_TYPES_SESS),
				International.getString("Fahrtart")));
		addParameter(typesStatus = new ItemTypeHashtable<String>("_TYPES_STATUS", "", true, IItemType.TYPE_EXPERT,
				BaseTabbedDialog.makeCategory(CATEGORY_TYPES, CATEGORY_TYPES_STAT), International.getString("Status")));

		addParameter(kanuEfb_boatTypes = new ItemTypeMultiSelectList<String>("KanuEfbBoatTypes",
				getCanoeBoatTypes(getValue("KanuEfbBoatTypes")),
				EfaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES),
				EfaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
				getValueUseFunctionalityCanoeingGermany() ? IItemType.TYPE_PUBLIC : IItemType.TYPE_EXPERT,
				BaseTabbedDialog.makeCategory(CATEGORY_SYNC, CATEGORY_KANUEFB),
				International.onlyFor("Fahrten mit folgenden Bootstypen mit Kanu-eFB synchronisieren", "de")));

		kanuEfb_boatTypes.setFieldGrid(3, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH);
		kanuEfb_boatTypes.setFieldSize(400, 400);
		kanuEfb_boatTypes.setPadding(0, 0, 20, 0);
		
		typesStatus.setAllowed(false, false);
		iniTypes(typesGender, EfaTypes.CATEGORY_GENDER);
		iniTypes(typesBoat, EfaTypes.CATEGORY_BOAT);
		iniTypes(typesNumSeats, EfaTypes.CATEGORY_NUMSEATS);
		iniTypes(typesRigging, EfaTypes.CATEGORY_RIGGING);
		iniTypes(typesCoxing, EfaTypes.CATEGORY_COXING);
		iniTypes(typesSession, EfaTypes.CATEGORY_SESSION);
		iniTypes(typesStatus, EfaTypes.CATEGORY_STATUS);
		standardFahrtart.setListData(EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES),
				EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY));
		if (widgets != null) {
			for (IWidget w : widgets) {
				if (w.getName().equals(AlertWidget.NAME)) {
					((AlertWidget) w).updateTypes();
				}
			}
		}
	}

	private void iniTypes(ItemTypeHashtable<String> types, String cat) {
		String[] t = myEfaTypes.getTypesArray(cat);
		String[] v = myEfaTypes.getValueArray(cat);
		for (int i = 0; i < t.length && i < v.length; i++) {
			types.put(t[i], v[i]);
		}
	}

	private boolean updateTypes(EfaTypes efaTypes, String cat, ItemTypeHashtable<String> newTypes) {
		if (newTypes == null || newTypes.size() == 0) {
			return false;
		}
		boolean changed = false;

		// check for changes in current types (changed values or removed types)
		String[] oldTypes = efaTypes.getTypesArray(cat);
		for (int i = 0; oldTypes != null && i < oldTypes.length; i++) {
			String newValue = newTypes.get(oldTypes[i]);
			if (newValue != null) {
				String oldValue = efaTypes.getValue(cat, oldTypes[i]);
				if (!newValue.equals(oldValue)) {
					efaTypes.setValue(cat, oldTypes[i], newValue);
					changed = true;
				}
			} else {
				efaTypes.removeValue(cat, oldTypes[i]);
				changed = true;
			}
		}

		// check for added types
		String[] newTypeNames = newTypes.getKeysArray();
		for (int i = 0; newTypeNames != null && i < newTypeNames.length; i++) {
			if (!efaTypes.isConfigured(cat, newTypeNames[i])) {
				efaTypes.setValue(cat, newTypeNames[i], newTypes.get(newTypeNames[i]));
				changed = true;
			}
		}

		return changed;
	}

	public IItemType[] getDefaultItems(String itemName) {
		if (itemName.equals(crontab.getName())) {
			int i = crontab.size() + 1;
			ItemTypeCronEntry[] item = new ItemTypeCronEntry[] { new ItemTypeCronEntry(crontab.getName() + i, "",
					crontab.getType(), crontab.getCategory(), "Task #" + i) };
			return item;
		}
		return null;
	}

	private DataTypeList<String> getCanoeBoatTypes(String myValue) {
		EfaTypes t = getMyEfaTypes();
		if (t == null) {
			return new DataTypeList<String>(new String[0]); // happens during startup
		}

		if (myValue != null && myValue.length() > 0) {
			return new DataTypeList<String>().parseList(myValue, IDataAccess.DATA_STRING);
		} else {
			return new DataTypeList<String>(t.getDefaultCanoeBoatTypes());
		}

	}

	public String getCanoeBoatTypes() {
		return kanuEfb_boatTypes.toString();
	}

	public Object[] getValueKanuEfb_CanoeBoatTypes() {
		return kanuEfb_boatTypes.getValues();
	}

	class ConfigValueUpdateThread extends Thread {

		private EfaConfig efaConfig;
		private long lastScn = -1;
		private volatile boolean keepRunning = true;

		public ConfigValueUpdateThread(EfaConfig efaConfig) {
			this.efaConfig = efaConfig;
		}

		public void run() {
			this.setName("ConfigValueUpdateThread");
			while (keepRunning) {
				// sleep first, then update
				try {
					Thread.sleep(10000);
				} catch (Exception e) {
				}
				updateConfigValuesWithPersistence();
			}
		}

		public boolean updateConfigValuesWithPersistence() {
			try {
				synchronized (efaConfig) {
					long scn = data().getSCN();
					if (scn == lastScn) {
						return true;
					}
					DataKeyIterator it = data().getStaticIterator();
					DataKey k = it.getFirst();
					synchronized (configValues) {
						while (k != null) {
							EfaConfigRecord r = (EfaConfigRecord) data().get(k);
							if (r.getName().startsWith(NOT_STORED_ITEM_PREFIX)) {
								k = it.getNext(); // without this line, this loop would never stop
								continue; // it shouldn't happen that such a value actually made it into the file, but
											// you never know...
							}
							IItemType item = configValues.get(r.getName());
							if (item != null) {
								item.parseValue(r.getValue());
								item.setUnchanged();
							}
							k = it.getNext();
						}
					}
					lastScn = scn;
				}
			} catch (Exception e) {
				Logger.logdebug(e);
				return false;
			}
			return true;
		}

		public void stopConfigValueUpdateThread() {
			keepRunning = false;
		}
	}

}
