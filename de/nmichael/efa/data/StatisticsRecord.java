/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.data;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.Plugins;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.efawett.WettDef;
import de.nmichael.efa.data.efawett.WettDefs;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.BaseTabbedDialog;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.gui.EnterPasswordDialog;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.gui.util.TableItem;
import de.nmichael.efa.gui.util.TableItemHeader;
import de.nmichael.efa.statistics.Competition;
import de.nmichael.efa.statistics.StatOutputLines;
import de.nmichael.efa.statistics.StatisticsData;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;

// @i18n complete

public class StatisticsRecord extends DataRecord implements IItemListener {

    // =========================================================================
    // Field Names
    // =========================================================================

    public static final String ID                        = "Id";
    public static final String NAME                      = "Name";
    public static final String POSITION                  = "Position";
    public static final String PUBLICLYAVAILABLE         = "PubliclyAvailable";
    public static final String DATEFROM                  = "DateFrom";
    public static final String DATETO                    = "DateTo";
    public static final String STATISTICCATEGORY         = "StatisticCategory";
    public static final String STATISTICTYPE             = "StatisticType";
    public static final String STATISTICKEY              = "StatisticKey";
    public static final String FILTERGENDER              = "FilterGender";
    public static final String FILTERGENDERALL           = "FilterGenderAll";
    public static final String FILTERSTATUS              = "FilterStatus";
    public static final String FILTERSTATUSALL           = "FilterStatusAll";
    public static final String FILTERSESSIONTYPE         = "FilterSessionType";
    public static final String FILTERSESSIONTYPEALL      = "FilterSessionTypeAll";
    public static final String FILTERBOATTYPE            = "FilterBoatType";
    public static final String FILTERBOATTYPEALL         = "FilterBoatTypeAll";
    public static final String FILTERBOATSEATS           = "FilterBoatSeats";
    public static final String FILTERBOATSEATSALL        = "FilterBoatSeatsAll";
    public static final String FILTERBOATRIGGING         = "FilterBoatRigging";
    public static final String FILTERBOATRIGGINGALL      = "FilterBoatRiggingAll";
    public static final String FILTERBOATCOXING          = "FilterBoatCoxing";
    public static final String FILTERBOATCOXINGALL       = "FilterBoatCoxingAll";
    public static final String FILTERBOATOWNER           = "FilterBoatOwner";
    public static final String FILTERBOATOWNERALL        = "FilterBoatOwnerAll";
    public static final String FILTERBYPERSONID          = "FilterByPersonId";
    public static final String FILTERBYPERSONTEXT        = "FilterByPersonText";
    public static final String FILTERBYBOATID            = "FilterByBoatId";
    public static final String FILTERBYBOATTEXT          = "FilterByBoatText";
    public static final String FILTERBYGROUPID           = "FilterByGroupId";
    public static final String FILTERPROMPTPERSON        = "FilterPromptPerson";
    public static final String FILTERPROMPTBOAT          = "FilterPromptBoat";
    public static final String FILTERPROMPTGROUP         = "FilterPromptGroup";
    public static final String FILTERFROMTOBOATHOUSE     = "FilterFromToBoathouse";
    public static final String FILTERNAMECONTAINS        = "FilterNameContains";
    public static final String FILTERCOMMENTSINCLUDE     = "FilterCommentsInclude";
    public static final String FILTERCOMMENTSEXCLUDE     = "FilterCommentsExclude";
    public static final String FILTERMINSESSIONDISTANCE  = "FilterMinSessionDistance";
    public static final String FILTERONLYOPENDAMAGES     = "FilterOnlyOpenDamages";
    public static final String FILTERALSOOPENSESSIONS    = "FilterAlsoOpenSessions";
    public static final String FILTERONLYLOGBOOK         = "FilterOnlyLogbook";
    public static final String SHOWFIELDS                = "ShowFields";  // like Name, Status, Gender, BoatType, ...
    public static final String SHOWLOGBOOKFIELDS         = "ShowLogbookFields";  // like EntryNo, Date, Boat, Cox, Crew, ...
    public static final String SHOWOTHERFIELDS           = "ShowOtherFields";  // like boat damages, reservations, ...
    public static final String AGGREGATIONS              = "Aggregations"; // like Distance, Sessions, AvgDistance, ...
    public static final String AGGRDISTANCEBARSIZE       = "AggregationDistanceBarSize";
    public static final String AGGRROWDISTANCEBARSIZE    = "AggregationRowDistanceBarSize";
    public static final String AGGRCOXDISTANCEBARSIZE    = "AggregationCoxDistanceBarSize";
    public static final String AGGRSESSIONSBARSIZE       = "AggregationSessionsBarSize";
    public static final String AGGRAVGDISTBARSIZE        = "AggregationAvgDistanceBarSize";
    public static final String AGGRDURATIONBARSIZE       = "AggregationDurationBarSize";
    public static final String AGGRSPEEDBARSIZE          = "AggregationSpeedBarSize";
    public static final String COMPYEAR                  = "CompYear";
    public static final String COMPPERCENTFULFILLED      = "CompPercentFulfilled";
    public static final String COMPOUTPUTSHORT           = "CompOutputShort";
    public static final String COMPOUTPUTRULES           = "CompOutputRules";
    public static final String COMPOUTPUTADDITIONALWITHREQUIREMENTS = "CompOutputAdditionalWithRequirements";
    public static final String COMPOUTPUTWITHOUTDETAILS  = "CompOutputWithoutDetails";
    public static final String COMPOUTPUTALLDESTINATIONAREAS = "CompOutputAllDestinationAreas";
    public static final String SORTINGCRITERIA           = "SortingCriteria";
    public static final String SORTINGORDER              = "SortingOrder";
    public static final String OUTPUTTYPE                = "OutputType";
    public static final String OUTPUTFILE                = "OutputFile";
    public static final String OUTPUTENCODING            = "OutputEncoding";
    public static final String OUTPUTHTMLUPDATETABLE     = "OutputHtmlUpdateTable";
    public static final String OUTPUTCSVSEPARATOR        = "OutputCsvSeparator";
    public static final String OUTPUTCSVQUOTES           = "OutputCsvQuotes";
    public static final String OPTIONDISTANCEWITHUNIT    = "OptionDistanceWithUnit";
    public static final String OPTIONTRUNCATEDIST        = "OptionTruncateDistance";
    public static final String OPTIONLISTALLNULLENTRIES  = "OptionListAllNullEntries";
    public static final String OPTIONIGNORENULLVALUES    = "OptionIgnoreNullValues";
    public static final String OPTIONONLYMEMBERSWITHINSUFFICIENTCLUBWORK = "OptionOnlyMembersWithInsufficientClubwork";
    public static final String OPTIONSUMGUESTSANDOTHERS  = "OptionSumGuestsAndOthers";
    public static final String OPTIONSUMGUESTSBYCLUB     = "OptionSumGuestsByClub";
    public static final String OPTIONSHOWVALIDLASTTRIP   = "OptionShowValidLastTrip";


    public static final String[] IDX_NAME = new String[] { NAME };

    // =========================================================================
    // Various GUI-related Fields
    // =========================================================================
    public static boolean TABLE_HEADER_LONG = true;

    // =========================================================================
    // Field Value Constants
    // =========================================================================

    public static final String SCAT_LIST            = "List";
    public static final String SCAT_MATRIX          = "Matrix";
    public static final String SCAT_LOGBOOK         = "Logbook";
    public static final String SCAT_COMPETITION     = "Competition";
    public static final String SCAT_OTHER           = "Other";


    public static final String STYPE_PERSONS          = "Persons";
    public static final String STYPE_BOATS            = "Boats";
    public static final String STYPE_BOATSTATUS       = "BoatStatus";
    public static final String STYPE_BOATRESERVATIONS = "BoatReservations";
    public static final String STYPE_BOATDAMAGES      = "BoatDamages";
    public static final String STYPE_BOATDAMAGESTAT   = "BoatDamageStat";
    public static final String STYPE_CLUBWORK         = "Clubwork";

    public static final String SKEY_NAME            = "Name";            // based on Persons or Boats
    public static final String SKEY_STATUS          = "Status";          // based on Persons
    public static final String SKEY_YEAROFBIRTH     = "YearOfBirth";     // based on Persons
    public static final String SKEY_GENDER          = "Gender";          // based on Persons
    public static final String SKEY_BOATTYPE        = "BoatType";        // based on Boats
    public static final String SKEY_BOATSEATS       = "BoatSeats";       // based on Boats
    public static final String SKEY_BOATTYPEDETAIL  = "BoatTypeDetail";  // based on Boats
    public static final String SKEY_DESTINATION     = "Destination";     // based on Persons or Boats
    public static final String SKEY_WATERS          = "Waters";          // based on Persons or Boats
    public static final String SKEY_DISTANCE        = "Distance";        // based on Persons or Boats
    public static final String SKEY_MONTH           = "Month";           // based on Persons or Boats
    public static final String SKEY_WEEKDAY         = "Weekday";         // based on Persons or Boats
    public static final String SKEY_TIMEOFDAY       = "TimeOfDay";       // based on Persons or Boats
    public static final String SKEY_SESSIONTYPE     = "SessionType";     // based on Persons or Boats
    public static final String SKEY_YEAR            = "Year";            // based on Persons or Boats

    public static final String OTYPE_INTERNAL       = "Internal";
    public static final String OTYPE_INTERNALTXT    = "InternalTxt";
    public static final String OTYPE_HTML           = "Html";
    public static final String OTYPE_PDF            = "Pdf";
    public static final String OTYPE_CSV            = "Csv";
    public static final String OTYPE_XML            = "Xml";
    public static final String OTYPE_EFAWETT        = "EfaWett";

    public static final String FIELDS_POSITION     = "Position";
    public static final String FIELDS_NAME         = "Name";
    public static final String FIELDS_GENDER       = "Gender";
    public static final String FIELDS_STATUS       = "Status";
    public static final String FIELDS_CLUB         = "Club";
    public static final String FIELDS_YEAROFBIRTH  = "YearOfBirth";
    public static final String FIELDS_MEMBERNO     = "MemberNo";
    public static final String FIELDS_BOATTYPE     = "BoatType";

    public static final String LFIELDS_ENTRYNO     = "EntryNo";
    public static final String LFIELDS_DATE        = "Date";
    public static final String LFIELDS_ENDDATE     = "EndDate";
    public static final String LFIELDS_BOAT        = "Boat";
    public static final String LFIELDS_COX         = "Cox";
    public static final String LFIELDS_CREW        = "Crew";
    public static final String LFIELDS_STARTTIME   = "StartTime";
    public static final String LFIELDS_ENDTIME     = "EndTime";
    public static final String LFIELDS_WATERS      = "Waters";
    public static final String LFIELDS_DESTINATION = "Destination";
    public static final String LFIELDS_DESTDETAILS = "DestinationDetails";
    public static final String LFIELDS_DESTAREAS   = "DestAreas";
    public static final String LFIELDS_DISTANCE    = "Distance";
    public static final String LFIELDS_MULTIDAY    = "MultiDay";
    public static final String LFIELDS_SESSIONTYPE = "SessionType";
    public static final String LFIELDS_NOTES       = "Notes";

    public static final String OFIELDS_BASESTATUS     = "BaseStatus";
    public static final String OFIELDS_CURRENTSTATUS  = "CurrentStatus";
    public static final String OFIELDS_COMMENTS       = "Comments";
    public static final String OFIELDS_RESERVEDFROM   = "ReservedFrom";
    public static final String OFIELDS_RESERVEDTO     = "ReservedTo";
    public static final String OFIELDS_RESERVEDFOR    = "ReservedFor";
    public static final String OFIELDS_REASON         = "Reason";
    public static final String OFIELDS_CONTACT        = "Contact";
    public static final String OFIELDS_DAMAGE         = "Damage";
    public static final String OFIELDS_DAMAGESEVERITY = "DamageSeverity";
    public static final String OFIELDS_REPORTEDON     = "ReportedOn";
    public static final String OFIELDS_FIXEDON        = "FixedOn";

    public static final String AGGR_DISTANCE       = "Distance";
    public static final String AGGR_ROWDISTANCE    = "RowDistance";
    public static final String AGGR_COXDISTANCE    = "CoxDistance";
    public static final String AGGR_SESSIONS       = "Sessions";
    public static final String AGGR_AVGDISTANCE    = "AvgDistance";
    public static final String AGGR_DURATION       = "Duration";
    public static final String AGGR_SPEED          = "Speed";
    public static final String AGGR_ZIELFAHRTEN    = "Zielfahrten";
    public static final String AGGR_WANDERFAHRTEN  = "Wanderfahrten";
    public static final String AGGR_DAMAGECOUNT    = "DamageCount";
    public static final String AGGR_DAMAGEDURATION = "DamageDuration";
    public static final String AGGR_DAMAGEAVGDUR   = "DamageAvgDuration";
    public static final String AGGR_CLUBWORK       = "Clubwork";
    public static final String AGGR_CLUBWORKTARGET = "ClubworkTarget";
    public static final String AGGR_CBRELTOTARGET  = "ClubworkRelativeToTarget";
    public static final String AGGR_CBOVERUNDERCARRYOVER = "ClubworkRelativeToTargetOverUnder";
    public static final String AGGR_CLUBWORKCREDIT = "ClubworkCredit";

    public static final String SORTING_DISTANCE    = "Distance";
    public static final String SORTING_ROWDISTANCE = "RowDistance";
    public static final String SORTING_COXDISTANCE = "CoxDistance";
    public static final String SORTING_SESSIONS    = "Sessions";
    public static final String SORTING_AVGDISTANCE = "AvgDistance";
    public static final String SORTING_DURATION    = "Duration";
    public static final String SORTING_SPEED       = "Speed";
    public static final String SORTING_NAME        = "Name";
    public static final String SORTING_GENDER      = "Geschlecht";
    public static final String SORTING_STATUS      = "Status";
    public static final String SORTING_YEAROFBIRTH = "YearOfBirth";
    public static final String SORTING_MEMBERNO    = "MemberNo";
    public static final String SORTING_BOATTYPE    = "BoatType";
    public static final String SORTING_ENTRYNO     = "EntryNo";
    public static final String SORTING_DATE        = "Date";
    public static final String SORTING_DAMAGECOUNT = "DamageCount";
    public static final String SORTING_DAMAGEDUR   = "DamageDuration";
    public static final String SORTING_DAMAGEAVGDUR= "DamageAvgDuration";
    public static final String SORTING_CLUBWORK    = "Clubwork";

    public static final String SORTINGORDER_ASC    = "Ascending";
    public static final String SORTINGORDER_DESC   = "Descending";

    public static final String BOWNER_OWN     = "OwnBoat";
    public static final String BOWNER_OTHER   = "OtherBoat";
    public static final String BOWNER_UNKNOWN = "Unknown";
    
    public static final String SHOWDATAVALID_STATENDTIME = "StatEndTime";
    public static final String SHOWDATAVALID_LASTTRIPTIME = "LastTripTime";
    public static final String ECRID = "ecrid";


    private static final int ARRAY_STRINGLIST_VALUES = 1;
    private static final int ARRAY_STRINGLIST_DISPLAY = 2;
    
    private static final String DEFAULT_GUI_CSV_COLUMN_SEPARATOR = "|";
    private static final String DEFAULT_GUI_CSV_QUOTE = "";

    public enum StatisticCategory {
        UNKNOWN,
        list,
        matrix,
        logbook,
        competition,
        other
    }

    public enum StatisticType {
        persons,
        boats,
        boatstatus,
        boatreservations,
        boatdamages,
        boatdamagestat,
        clubwork,
        anythingElse // if this is selected, it's most likely a competition; refer to the String value
    }

    public enum StatisticKey {
        name,
        status,
        yearOfBirth,
        gender,
        boatType,
        boatSeats,
        boatTypeDetail,
        destination,
        waters,
        distance,
        month,
        weekday,
        timeOfDay,
        sessionType,
        year
    }

    public enum SortingCriteria {
        UNKNOWN,
        distance,
        rowdistance,
        coxdistance,
        sessions,
        avgDistance,
        duration,
        days,
        speed,
        name,
        gender,
        status,
        yearOfBirth,
        memberNo,
        boatType,
        entryNo,
        date,
        damageCount,
        damageDuration,
        damageAvgDuration,
        clubwork
    }

    public enum OutputTypes {
        UNKNOWN,
        internal,
        internaltxt,
        html,
        csv,
        xml,
        pdf,
        efawett
    }

    // =========================================================================
    // Internal Variables for various purposes
    // =========================================================================
    private static final String GUIITEM_OUTPUTFTP = "GUIITEM_OUTPUTFTP";
    private static final String GUIITEM_OUTPUTEMAIL = "GUIITEM_OUTPUTEMAIL";
    private ItemTypeStringList itemStatisticCategory;
    private ItemTypeStringList itemStatisticType;
    private ItemTypeStringList itemStatisticKey;
    private ItemTypeDate itemDateFrom;
    private ItemTypeDate itemDateTo;
    private ItemTypeInteger itemCompYear;
    private ItemTypeMultiSelectList<String> itemFilterGender;
    private ItemTypeMultiSelectList<String> itemFilterStatus;
    private ItemTypeMultiSelectList<String> itemFilterSessionType;
    private ItemTypeMultiSelectList<String> itemFilterBoatType;
    private ItemTypeMultiSelectList<String> itemFilterBoatSeats;
    private ItemTypeMultiSelectList<String> itemFilterBoatRigging;
    private ItemTypeMultiSelectList<String> itemFilterBoatCoxing;
    private ItemTypeMultiSelectList<String> itemFilterBoatOwner;
    private ItemTypeMultiSelectList<String> itemShowOtherFields;
    private ItemTypeMultiSelectList<String> itemAggrFields;
    private ItemTypeFile itemOutputFile;
    private ItemTypeLabel itemOutputFileHINT;
    private ItemTypeStringList itemOutputEncoding;
    private ItemTypeBoolean itemOutputHtmlUpdateTable;
    private ItemTypeString itemOutputCsvSeparator;
    private ItemTypeString itemOutputCsvQuotes;
    private ItemTypeButton itemOutputFtpButton;
    private ItemTypeButton itemOutputEmailButton;
    private ItemTypeStringList itemTypeSortingCriteria;
    private ItemTypeStringList itemTypeSortingOrder;

    // =========================================================================
    // Statistic Settings (for easier access)
    // =========================================================================

    // filled by StatisticsRecord.prepareStatisticSettings()
    // --- Statistic Settings
    public AdminRecord sAdmin;
    public boolean sPublicStatistic;
    public DataTypeDate sStartDate;
    public DataTypeDate sEndDate;
    public long sTimestampBegin;
    public long sTimestampEnd;
    public long sValidAt;
    public StatisticCategory sStatisticCategory;
    public String sStatisticType;
    public StatisticType sStatisticTypeEnum;
    public StatisticKey sStatistikKey;
    // --- Filter Settings
    public Hashtable<String,String> sFilterGender;
    public boolean sFilterGenderAll;
    public Hashtable<UUID,String> sFilterStatus;
    public boolean sFilterStatusOther;
    public boolean sFilterStatusAll;
    public Hashtable<String,String> sFilterSessionType;
    public boolean sFilterSessionTypeAll;
    public Hashtable<String,String> sFilterBoatType;
    public boolean sFilterBoatTypeAll;
    public Hashtable<String,String> sFilterBoatSeats;
    public boolean sFilterBoatSeatsAll;
    public Hashtable<String,String> sFilterBoatRigging;
    public boolean sFilterBoatRiggingAll;
    public Hashtable<String,String> sFilterBoatCoxing;
    public boolean sFilterBoatCoxingAll;
    public Hashtable<String,String> sFilterBoatOwner;
    public boolean sFilterBoatOwnerAll;
    public UUID sFilterByPersonId;
    public String sFilterByPersonText;
    public UUID sFilterByBoatId;
    public String sFilterByBoatText;
    public UUID sFilterByGroupId;
    public boolean sFilterFromToBoathouse;
    public String sFilterNameContains;
    public String sFilterCommentsInclude;
    public String sFilterCommentsExclude;
    public DataTypeDistance sFilterMinSessionDistance;
    public boolean sFilterOnlyOpenDamages;
    public boolean sFilterAlsoOpenSessions;
    public String sFilterOnlyLogbook;
    // --- Field Settings
    public boolean sIsFieldsPosition;
    public boolean sIsFieldsName;
    public boolean sIsFieldsGender;
    public boolean sIsFieldsStatus;
    public boolean sIsFieldsClub;
    public boolean sIsFieldsYearOfBirth;
    public boolean sIsFieldsMemberNo;
    public boolean sIsFieldsBoatType;
    public boolean sIsLFieldsEntryNo;
    public boolean sIsLFieldsDate;
    public boolean sIsLFieldsEndDate;
    public boolean sIsLFieldsBoat;
    public boolean sIsLFieldsCox;
    public boolean sIsLFieldsCrew;
    public boolean sIsLFieldsStartTime;
    public boolean sIsLFieldsEndTime;
    public boolean sIsLFieldsWaters;
    public boolean sIsLFieldsDestination;
    public boolean sIsLFieldsDestinationDetails;
    public boolean sIsLFieldsDestinationAreas;
    public boolean sIsLFieldsDistance;
    public boolean sIsLFieldsMultiDay;
    public boolean sIsLFieldsSessionType;
    public boolean sIsLFieldsNotes;
    public boolean sIsOFieldsBaseStatus;
    public boolean sIsOFieldsCurrentStatus;
    public boolean sIsOFieldsComments;
    public boolean sIsOFieldsReservedFrom;
    public boolean sIsOFieldsReservedTo;
    public boolean sIsOFieldsReservedFor;
    public boolean sIsOFieldsReason;
    public boolean sIsOFieldsContact;
    public boolean sIsOFieldsDamage;
    public boolean sIsOFieldsDamageSeverity;
    public boolean sIsOFieldsReportedOn;
    public boolean sIsOFieldsFixedOn;
    public boolean sIsAggrDistance;
    public boolean sIsAggrRowDistance;
    public boolean sIsAggrCoxDistance;
    public boolean sIsAggrSessions;
    public boolean sIsAggrAvgDistance;
    public boolean sIsAggrDuration;
    public boolean sIsAggrDays;
    public boolean sIsAggrSpeed;
    public boolean sIsAggrZielfahrten;
    public boolean sIsAggrWanderfahrten;
    public boolean sIsAggrWinterfahrten;
    public boolean sIsAggrGigfahrten;
    public boolean sIsAggrDamageCount;
    public boolean sIsAggrDamageDuration;
    public boolean sIsAggrDamageAvgDuration;
    public boolean sIsAggrClubwork;
    public boolean sIsAggrClubworkTarget;
    public boolean sIsAggrClubworkRelativeToTarget;
    public boolean sIsAggrClubworkOverUnderCarryOver;
    public boolean sIsAggrClubworkCredit;
    public int sAggrDistanceBarSize;
    public int sAggrRowDistanceBarSize;
    public int sAggrCoxDistanceBarSize;
    public int sAggrSessionsBarSize;
    public int sAggrAvgDistanceBarSize;
    public int sAggrDurationBarSize;
    public int sAggrSpeedBarSize;
    public int sLFieldDistancePos = -1;
    // --- Sorting Settings
    public SortingCriteria sSortingCriteria;
    public boolean sSortingOrderAscending;
    // --- Output Settings
    public OutputTypes sOutputType;
    public String sOutputDir;
    public String sOutputFile;
    public String sOutputEncoding;
    public boolean sOutputHtmlUpdateTable;
    public String sOutputCsvSeparator;
    public String sOutputCsvQuotes;
    public FTPClient sOutputFtpClient;
    public String sEmailAddresses;
    public String sFileExecBefore;
    public String sFileExecAfter;
    // --- Competition Settings
    public int sCompYear;
    public int sCompPercentFulfilled;
    public boolean sIsOutputCompShort;
    public boolean sIsOutputCompRules;
    public boolean sIsOutputCompAdditionalWithRequirements;
    public boolean sIsOutputCompWithoutDetails;
    public boolean sIsOutputCompAllDestinationAreas;
    // --- Options
    public boolean sDistanceWithUnit;
    public boolean sTruncateDistanceToFullValue;
    public boolean sListAllNullEntries;
    public boolean sIgnoreNullValues;
    public boolean sSumGuestsAndOthers;
    public boolean sSumGuestsByClub;
    public boolean sShowValidLastTrip;
    // --- Clubwork-Options
    public DataTypeDate sClubworkStartDate;
    public DataTypeDate sClubworkEndDate;
    public double sDefaultClubworkTargetHours;
    public double sClubworkTargetHoursForStatistic;
    public double sTransferableClubworkHours;
    public double sFineForTooLittleClubwork;
    public boolean sOnlyMembersWithInsufficientClubwork;

    // filled during statistics creation in StatistikTask
    public int cNumberOfEntries = 0;
    public DataTypeIntString cEntryNoFirst;
    public DataTypeIntString cEntryNoLast;
    public DataTypeDate cEntryDateFirst;
    public DataTypeDate cEntryDateLast;
    public Hashtable<String,String> cWarnings;
    public Competition cCompetition;

    // filled by StatistikTask.runPostprocessing()
    public BaseDialog pParentDialog;
    public String pStatTitle;
    public String pStatCreationDate;
    public String pStatCreatedByUrl;
    public String pStatCreatedByName;
    public String pStatDescription;
    public String pStatDateRange;
    public String pStatConsideredEntries;
    public String pStatFilter;
    public Hashtable<String,String> pStatIgnored;
    public Vector<String> pTableColumns;
    public Hashtable<String,Object> pMatrixColumns;
    public int pMatrixColumnFirst;

    // filled by Competition.calculate()
    public String[] pCompRules;
    public Hashtable pCompRulesBold = new Hashtable();
    public Hashtable pCompRulesItalics = new Hashtable();
    public String pCompWarning;
    public String[][] pCompGroupNames;
    public StatisticsData[] pCompParticipants;
    public boolean pCompAbortEfaWett = false;

    public String[][] pAdditionalTable1;
    public String[]   pAdditionalTable1Title;
    public boolean    pAdditionalTable1FirstRowBold = false;
    public boolean    pAdditionalTable1LastRowBold = false;
    public String[][] pAdditionalTable2;
    public String[]   pAdditionalTable2Title;
    public boolean    pAdditionalTable2FirstRowBold = false;
    public boolean    pAdditionalTable2LastRowBold = false;
    public StatOutputLines pOutputLinesAbove;
    public StatOutputLines pOutputLinesBelow;
    public String     pComments;



    public static void initialize() {
        Vector<String> f = new Vector<String>();
        Vector<Integer> t = new Vector<Integer>();

        f.add(ID);                                t.add(IDataAccess.DATA_UUID);
        f.add(NAME);                              t.add(IDataAccess.DATA_STRING);
        f.add(POSITION);                          t.add(IDataAccess.DATA_INTEGER);
        f.add(PUBLICLYAVAILABLE);                 t.add(IDataAccess.DATA_BOOLEAN);
        f.add(DATEFROM);                          t.add(IDataAccess.DATA_DATE);
        f.add(DATETO);                            t.add(IDataAccess.DATA_DATE);
        f.add(STATISTICCATEGORY);                 t.add(IDataAccess.DATA_STRING);
        f.add(STATISTICTYPE);                     t.add(IDataAccess.DATA_STRING);
        f.add(STATISTICKEY);                      t.add(IDataAccess.DATA_STRING);
        f.add(FILTERGENDER);                      t.add(IDataAccess.DATA_LIST_STRING);
        f.add(FILTERGENDERALL);                   t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERSTATUS);                      t.add(IDataAccess.DATA_LIST_UUID);
        f.add(FILTERSTATUSALL);                   t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERSESSIONTYPE);                 t.add(IDataAccess.DATA_LIST_STRING);
        f.add(FILTERSESSIONTYPEALL);              t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERBOATTYPE);                    t.add(IDataAccess.DATA_LIST_STRING);
        f.add(FILTERBOATTYPEALL);                 t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERBOATSEATS);                   t.add(IDataAccess.DATA_LIST_STRING);
        f.add(FILTERBOATSEATSALL);                t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERBOATRIGGING);                 t.add(IDataAccess.DATA_LIST_STRING);
        f.add(FILTERBOATRIGGINGALL);              t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERBOATCOXING);                  t.add(IDataAccess.DATA_LIST_STRING);
        f.add(FILTERBOATCOXINGALL);               t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERBOATOWNER);                   t.add(IDataAccess.DATA_LIST_STRING);
        f.add(FILTERBOATOWNERALL);                t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERBYPERSONID);                  t.add(IDataAccess.DATA_UUID);
        f.add(FILTERBYPERSONTEXT);                t.add(IDataAccess.DATA_STRING);
        f.add(FILTERBYBOATID);                    t.add(IDataAccess.DATA_UUID);
        f.add(FILTERBYBOATTEXT);                  t.add(IDataAccess.DATA_STRING);
        f.add(FILTERBYGROUPID);                   t.add(IDataAccess.DATA_UUID);
        f.add(FILTERPROMPTPERSON);                t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERPROMPTBOAT);                  t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERPROMPTGROUP);                 t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERFROMTOBOATHOUSE);             t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERNAMECONTAINS);                t.add(IDataAccess.DATA_STRING);
        f.add(FILTERCOMMENTSINCLUDE);             t.add(IDataAccess.DATA_STRING);
        f.add(FILTERCOMMENTSEXCLUDE);             t.add(IDataAccess.DATA_STRING);
        f.add(FILTERMINSESSIONDISTANCE);          t.add(IDataAccess.DATA_DISTANCE);
        f.add(FILTERONLYOPENDAMAGES);             t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERALSOOPENSESSIONS);            t.add(IDataAccess.DATA_BOOLEAN);
        f.add(FILTERONLYLOGBOOK);                 t.add(IDataAccess.DATA_STRING);
        f.add(SHOWFIELDS);                        t.add(IDataAccess.DATA_LIST_STRING);
        f.add(SHOWLOGBOOKFIELDS);                 t.add(IDataAccess.DATA_LIST_STRING);
        f.add(SHOWOTHERFIELDS);                   t.add(IDataAccess.DATA_LIST_STRING);
        f.add(AGGREGATIONS);                      t.add(IDataAccess.DATA_LIST_STRING);
        f.add(AGGRDISTANCEBARSIZE);               t.add(IDataAccess.DATA_INTEGER);
        f.add(AGGRROWDISTANCEBARSIZE);            t.add(IDataAccess.DATA_INTEGER);
        f.add(AGGRCOXDISTANCEBARSIZE);            t.add(IDataAccess.DATA_INTEGER);
        f.add(AGGRSESSIONSBARSIZE);               t.add(IDataAccess.DATA_INTEGER);
        f.add(AGGRAVGDISTBARSIZE);                t.add(IDataAccess.DATA_INTEGER);
        f.add(AGGRDURATIONBARSIZE);               t.add(IDataAccess.DATA_INTEGER);
        f.add(AGGRSPEEDBARSIZE);                  t.add(IDataAccess.DATA_INTEGER);
        f.add(COMPYEAR);                          t.add(IDataAccess.DATA_INTEGER);
        f.add(COMPPERCENTFULFILLED);              t.add(IDataAccess.DATA_INTEGER);
        f.add(COMPOUTPUTSHORT);                   t.add(IDataAccess.DATA_BOOLEAN);
        f.add(COMPOUTPUTRULES);                   t.add(IDataAccess.DATA_BOOLEAN);
        f.add(COMPOUTPUTADDITIONALWITHREQUIREMENTS); t.add(IDataAccess.DATA_BOOLEAN);
        f.add(COMPOUTPUTWITHOUTDETAILS);          t.add(IDataAccess.DATA_BOOLEAN);
        f.add(COMPOUTPUTALLDESTINATIONAREAS);     t.add(IDataAccess.DATA_BOOLEAN);
        f.add(SORTINGCRITERIA);                   t.add(IDataAccess.DATA_STRING);
        f.add(SORTINGORDER);                      t.add(IDataAccess.DATA_STRING);
        f.add(OUTPUTTYPE);                        t.add(IDataAccess.DATA_STRING);
        f.add(OUTPUTFILE);                        t.add(IDataAccess.DATA_STRING);
        f.add(OUTPUTENCODING);                    t.add(IDataAccess.DATA_STRING);
        f.add(OUTPUTHTMLUPDATETABLE);             t.add(IDataAccess.DATA_BOOLEAN);
        f.add(OUTPUTCSVSEPARATOR);                t.add(IDataAccess.DATA_STRING);
        f.add(OUTPUTCSVQUOTES);                   t.add(IDataAccess.DATA_STRING);
        f.add(OPTIONDISTANCEWITHUNIT);            t.add(IDataAccess.DATA_BOOLEAN);
        f.add(OPTIONTRUNCATEDIST);                t.add(IDataAccess.DATA_BOOLEAN);
        f.add(OPTIONLISTALLNULLENTRIES);          t.add(IDataAccess.DATA_BOOLEAN);
        f.add(OPTIONIGNORENULLVALUES);            t.add(IDataAccess.DATA_BOOLEAN);
        f.add(OPTIONONLYMEMBERSWITHINSUFFICIENTCLUBWORK); t.add(IDataAccess.DATA_BOOLEAN);
        f.add(OPTIONSUMGUESTSANDOTHERS);          t.add(IDataAccess.DATA_BOOLEAN);
        f.add(OPTIONSUMGUESTSBYCLUB);             t.add(IDataAccess.DATA_BOOLEAN);
        f.add(OPTIONSHOWVALIDLASTTRIP);           t.add(IDataAccess.DATA_STRING);
        f.add(ECRID);                             t.add(IDataAccess.DATA_STRING);
        MetaData metaData = constructMetaData(Statistics.DATATYPE, f, t, false);
        metaData.setKey(new String[] { ID });
        metaData.addIndex(IDX_NAME);
    }

    public StatisticsRecord(Statistics statistics, MetaData metaData) {
        super(statistics, metaData);
    }

    public DataRecord createDataRecord() { // used for cloning
        return getPersistence().createNewRecord();
    }

    public void setDefaults() {
        setStatisticCategory(SCAT_LIST);
        setStatisticType(this.STYPE_PERSONS);
        setStatisticKey(SKEY_NAME);

        setFilterGender(new DataTypeList<String>(Daten.efaTypes.makeGenderArray(EfaTypes.ARRAY_STRINGLIST_VALUES)));
        setFilterGenderAll(true);
        setFilterStatus(new DataTypeList<UUID>(getFilterStatusListValues(false)));
        setFilterStatusAll(false);
        setFilterSessionType(new DataTypeList<String>(Daten.efaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES)));
        setFilterSessionTypeAll(true);
        setFilterBoatType(new DataTypeList<String>(Daten.efaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES)));
        setFilterBoatTypeAll(true);
        setFilterBoatSeats(new DataTypeList<String>(Daten.efaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES)));
        setFilterBoatSeatsAll(true);
        setFilterBoatRigging(new DataTypeList<String>(Daten.efaTypes.makeBoatRiggingArray(EfaTypes.ARRAY_STRINGLIST_VALUES)));
        setFilterBoatRiggingAll(true);
        setFilterBoatCoxing(new DataTypeList<String>(Daten.efaTypes.makeBoatCoxingArray(EfaTypes.ARRAY_STRINGLIST_VALUES)));
        setFilterBoatCoxingAll(true);
        setFilterBoatOwner(new DataTypeList<String>(getFilterBoatOwnerArray(EfaTypes.ARRAY_STRINGLIST_VALUES)));
        setFilterBoatOwnerAll(true);
        setShowFields(new DataTypeList<String>(new String[] { FIELDS_POSITION, FIELDS_NAME }));
        setShowLogbookFields(new DataTypeList<String>(new String[] {
                LFIELDS_ENTRYNO,
                LFIELDS_DATE,
                LFIELDS_BOAT,
                LFIELDS_COX,
                LFIELDS_CREW,
                LFIELDS_STARTTIME,
                LFIELDS_ENDTIME,
                LFIELDS_DESTINATION,
                LFIELDS_DISTANCE
        }));
        setShowOtherFields(new DataTypeList<String>(new String[0]));
        setAggregations(new DataTypeList<String>(new String[] { AGGR_DISTANCE, AGGR_SESSIONS, AGGR_AVGDISTANCE,
             AGGR_DAMAGECOUNT, AGGR_DAMAGEDURATION, AGGR_CLUBWORK, AGGR_CLUBWORKTARGET, AGGR_CBRELTOTARGET }));
        setAggrDistanceBarSize(200);
        setAggrRowDistanceBarSize(0);
        setAggrCoxDistanceBarSize(0);
        setAggrSessionsBarSize(0);
        setAggrAvgDistanceBarSize(0);
        setAggrDurationBarSize(0);
        setAggrSpeedBarSize(0);
        setSortingCriteria(SORTING_DISTANCE);
        setSortingOrder(SORTINGORDER_DESC);
        setCompYear(DataTypeDate.today().getMonth() > 2 ? DataTypeDate.today().getYear() : DataTypeDate.today().getYear() - 1);
        setCompPercentFulfilled(100);
        setOptionDistanceWithUnit(true);
        setOptionTruncateDistance(true);
        setOptionListAllNullEntries(false);
        setOptionIgnoreNullValues(true);
        setOptionSumGuestsAndOthers(true);
        setOptionSumGuestsByClub(false);
        setOptionShowValidLastTrip(false);
    }

    public DataKey getKey() {
        return new DataKey<UUID,String,String>(getId(),null,null);
    }

    public static DataKey getKey(UUID id) {
        return new DataKey<UUID,String,String>(id,null,null);
    }

    public void setId(UUID id) {
        setUUID(ID, id);
    }
    public UUID getId() {
        return getUUID(ID);
    }

    public void setName(String name) {
        setString(NAME, name);
    }
    public String getName() {
        String s = getString(NAME);
        if (s == null || s.length() == 0) {
            return International.getString("Standard");
        }
        return s;
    }

    public void setPosition(int position) {
        setInt(POSITION, position);
    }
    public int getPosition() {
        int position = getInt(POSITION);
        if (position < 0) {
            return 0;
        }
        return position;
    }

    public void setPubliclyAvailable(boolean publiclyAvailable) {
        setBool(PUBLICLYAVAILABLE, publiclyAvailable);
    }

    public boolean getPubliclyAvailable() {
        return getBool(PUBLICLYAVAILABLE);
    }

    public void setDateFrom(DataTypeDate date) {
        setDate(DATEFROM, date);
    }

    public DataTypeDate getDateFrom() {
        return getDate(DATEFROM);
    }

    public void setDateTo(DataTypeDate date) {
        setDate(DATETO, date);
    }

    public DataTypeDate getDateTo() {
        return getDate(DATETO);
    }

    public void setStatisticCategory(String type) {
        setString(STATISTICCATEGORY, type);
    }

    public String getStatisticCategory() {
        String s = getString(STATISTICCATEGORY);
        if (s == null || s.length() == 0) {
            return SCAT_LIST;
        }
        return s;
    }

    public static StatisticCategory getStatisticCategoryEnum(String type) {
        if (type == null) {
            return StatisticCategory.UNKNOWN;
        } else if (type.equals(SCAT_LIST)) {
            return StatisticCategory.list;
        } else if (type.equals(SCAT_MATRIX)) {
            return StatisticCategory.matrix;
        } else if (type.equals(SCAT_LOGBOOK)) {
            return StatisticCategory.logbook;
        } else if (type.equals(SCAT_COMPETITION)) {
            return StatisticCategory.competition;
        } else if (type.equals(SCAT_OTHER)) {
            return StatisticCategory.other;
        }
        return StatisticCategory.UNKNOWN;
    }

    public StatisticCategory getStatisticCategoryEnum() {
        String type = getStatisticCategory();
        return getStatisticCategoryEnum(type);
    }

    public String getStatisticCategoryDescription() {
        switch(getStatisticCategoryEnum()) {
            case list:
                return International.getString("Kilometerliste");
            case matrix:
                return International.getString("Matrix");
            case logbook:
                return International.getString("Fahrtenbuch");
            case competition:
                return International.getString("Wettbewerb");
            case other:
                return International.getString("Weitere");
            case UNKNOWN:
            	return EfaTypes.TEXT_UNKNOWN;
        }
        return EfaTypes.TEXT_UNKNOWN;
    }

    public String[] getStatisticCategories(int valuesOrDisplay) {
        if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
            return new String[] {
                    SCAT_LIST,
                    SCAT_MATRIX,
                    SCAT_LOGBOOK,
                    SCAT_COMPETITION,
                    SCAT_OTHER
            };
        } else {
            return new String[] {
                    International.getString("Kilometerliste"),
                    International.getString("Matrix"),
                    International.getString("Fahrtenbuch"),
                    International.getString("Wettbewerb"),
                    International.getString("Weitere")
            };
        }
    }

    public void setStatisticType(String type) {
        setString(STATISTICTYPE, type);
    }

    public String getStatisticType() {
        String s = getString(STATISTICTYPE);
        if (s == null || s.length() == 0) {
            return "";
        }
        String [] possibleTypes = getStatisticTypes(getStatisticCategoryEnum(), ARRAY_STRINGLIST_VALUES);
        for (int i=0; possibleTypes != null && i<possibleTypes.length; i++) {
            if (s.equals(possibleTypes[i])) {
                return s;
            }
        }
        return "";
    }

    public StatisticType getStatisticTypeEnum() {
        return getStatisticTypeEnum(getStatisticType());
    }

    public StatisticType getStatisticTypeEnum(String s) {
        if (s.equals(STYPE_PERSONS)) {
            return StatisticType.persons;
        }
        if (s.equals(STYPE_BOATS)) {
            return StatisticType.boats;
        }
        if (s.equals(STYPE_BOATSTATUS)) {
            return StatisticType.boatstatus;
        }
        if (s.equals(STYPE_BOATRESERVATIONS)) {
            return StatisticType.boatreservations;
        }
        if (s.equals(STYPE_BOATDAMAGES)) {
            return StatisticType.boatdamages;
        }
        if (s.equals(STYPE_BOATDAMAGESTAT)) {
            return StatisticType.boatdamagestat;
        }
        if (s.equals(STYPE_CLUBWORK)) {
            return StatisticType.clubwork;
        }
        return StatisticType.anythingElse;
    }

    public String getStatisticTypeDefault(StatisticCategory cat) {
        String[] types = getStatisticTypes(cat, ARRAY_STRINGLIST_VALUES);
        if (types != null && types.length > 0) {
            return types[0];
        }
        return null;
    }

    public String[] getStatisticTypes(StatisticCategory category, int valuesOrDisplay) {
        if (category == StatisticCategory.list || category == StatisticCategory.matrix) {
            if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                return new String[]{
                        STYPE_PERSONS,
                        STYPE_BOATS
                };
            } else {
                return new String[]{
                        International.getString("Personen"),
                        International.getString("Boote")
                };
            }
        }
        if (category == StatisticCategory.logbook) {
            if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                return new String[]{ "" };
            } else {
                return new String[]{ "---" };
            }
        }
        if (category == StatisticCategory.competition) {
            if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                return (Daten.wettDefs != null
                        ? Daten.wettDefs.getAllWettDefKeys() : new String[0]);
            } else {
                return (Daten.wettDefs != null
                        ? Daten.wettDefs.getAllWettDefNames() : new String[0]);

            }
        }
        if (category == StatisticCategory.other) {
            if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                return new String[]{
                        STYPE_BOATSTATUS,
                        STYPE_BOATRESERVATIONS,
                        STYPE_BOATDAMAGES,
                        STYPE_BOATDAMAGESTAT,
                        STYPE_CLUBWORK
                };
            } else {
                return new String[]{
                        International.getString("Bootsstatus"),
                        International.getString("Bootsreservierungen"),
                        International.getString("Bootsschäden"),
                        International.getString("Bootsschäden-Statistik"),
                        International.getString("Vereinsarbeit")
                };
            }
        }
        return new String[]{};
    }

    public String getStatisticTypeDescription() {
        String type = getStatisticType();
        String[] allTypes = getStatisticTypes(getStatisticCategoryEnum(), ARRAY_STRINGLIST_VALUES);
        for (int i=0; allTypes != null && type != null && i < allTypes.length; i++) {
            if (type.equals(allTypes[i])) {
                String[] allDisplay = getStatisticTypes(getStatisticCategoryEnum(), ARRAY_STRINGLIST_DISPLAY);
                if (allDisplay != null && i<allDisplay.length) {
                    return allDisplay[i];
                }
                return null;
            }
        }
        return null;
    }

    public void setStatisticKey(String key) {
        setString(STATISTICKEY, key);
    }

    public String getStatisticKey() {
        String s = getString(STATISTICKEY);
        if (s == null || s.length() == 0) {
            return "";
        }
        String [] possibleTypes = getStatisticKeys(getStatisticType(), ARRAY_STRINGLIST_VALUES);
        for (int i=0; possibleTypes != null && i<possibleTypes.length; i++) {
            if (s.equals(possibleTypes[i])) {
                return s;
            }
        }
        return "";
    }

    public static StatisticKey getStatisticKeyEnum(String key) {
        if (key == null) {
            return StatisticKey.name;
        } else if (key.equals(SKEY_NAME)) {
            return StatisticKey.name;
        } else if (key.equals(SKEY_STATUS)) {
            return StatisticKey.status;
        } else if (key.equals(SKEY_YEAROFBIRTH)) {
            return StatisticKey.yearOfBirth;
        } else if (key.equals(SKEY_GENDER)) {
            return StatisticKey.gender;
        } else if (key.equals(SKEY_BOATTYPE)) {
            return StatisticKey.boatType;
        } else if (key.equals(SKEY_BOATSEATS)) {
            return StatisticKey.boatSeats;
        } else if (key.equals(SKEY_BOATTYPEDETAIL)) {
            return StatisticKey.boatTypeDetail;
        } else if (key.equals(SKEY_DESTINATION)) {
            return StatisticKey.destination;
        } else if (key.equals(SKEY_WATERS)) {
            return StatisticKey.waters;
        } else if (key.equals(SKEY_DISTANCE)) {
            return StatisticKey.distance;
        } else if (key.equals(SKEY_MONTH)) {
            return StatisticKey.month;
        } else if (key.equals(SKEY_WEEKDAY)) {
            return StatisticKey.weekday;
        } else if (key.equals(SKEY_TIMEOFDAY)) {
            return StatisticKey.timeOfDay;
        } else if (key.equals(SKEY_SESSIONTYPE)) {
            return StatisticKey.sessionType;
        } else if (key.equals(SKEY_YEAR)) {
            return StatisticKey.year;
        }
        return StatisticKey.name;
    }

    public StatisticKey getStatisticKeyEnum() {
        String key = getStatisticKey();
        return getStatisticKeyEnum(key);
    }

    public String getStatisticKeyDescription() {
        String key = getStatisticKey();
        String[] allKeys = getStatisticKeys(null, ARRAY_STRINGLIST_VALUES);
        for (int i=0; allKeys != null && key != null && i < allKeys.length; i++) {
            if (key.equals(allKeys[i])) {
                String[] allDisplay = getStatisticKeys(null, ARRAY_STRINGLIST_DISPLAY);
                if (allDisplay != null && i<allDisplay.length) {
                    return allDisplay[i];
                }
                return null;
            }
        }
        return null;
    }

    public String getStatisticKeyDescriptionPlural() {
        switch(getStatisticKeyEnum()) {
            case name:
                return International.getString("Namen");
            case status:
                return International.getString("Status");
            case yearOfBirth:
                return International.getString("Jahrgänge");
            case gender:
                return International.getString("Geschlechter");
            case boatType:
                return International.getString("Bootstypen");
            case boatSeats:
                return International.getString("Bootsplätze");
            case boatTypeDetail:
                return International.getString("Bootstypen") + " (" +
                        International.getString("Detail") + ")";
            case destination:
                return International.getString("Ziele");
            case waters:
                return International.getString("Gewässer");
            case distance:
                return International.getString("Entfernungen");
            case month:
                return International.getString("Monate");
            case weekday:
                return International.getString("Wochentage");
            case timeOfDay:
                return International.getString("Tageszeiten");
            case sessionType:
                return International.getString("Fahrtarten");
            case year:
                return International.getString("Jahre");
        }
        return "";
    }

    public String getStatisticKeyDefault(String sType) {
        String[] keys = getStatisticKeys(sType, ARRAY_STRINGLIST_VALUES);
        if (keys != null && keys.length > 0) {
            return keys[0];
        }
        return null;
    }

    public String[] getStatisticKeys(String sType, int valuesOrDisplay) {
        Hashtable<String,String> allKeys = new Hashtable<String,String>();
        allKeys.put(SKEY_NAME, International.getString("Name"));
        allKeys.put(SKEY_STATUS, International.getString("Status"));
        allKeys.put(SKEY_YEAROFBIRTH, International.getString("Jahrgang"));
        allKeys.put(SKEY_GENDER, International.getString("Geschlecht"));
        allKeys.put(SKEY_BOATTYPE, International.getString("Bootstyp"));
        allKeys.put(SKEY_BOATSEATS, International.getString("Bootsplätze"));
        allKeys.put(SKEY_BOATTYPEDETAIL, International.getString("Bootstyp") + " (" +
                International.getString("Detail") + ")");
        allKeys.put(SKEY_DESTINATION, International.getString("Ziel"));
        allKeys.put(SKEY_WATERS, International.getString("Gewässer"));
        allKeys.put(SKEY_DISTANCE, International.getString("Entfernung"));
        allKeys.put(SKEY_MONTH, International.getString("Monat"));
        allKeys.put(SKEY_WEEKDAY, International.getString("Wochentag"));
        allKeys.put(SKEY_TIMEOFDAY, International.getString("Tageszeit"));
        allKeys.put(SKEY_SESSIONTYPE, International.getString("Fahrtart"));
        allKeys.put(SKEY_YEAR, International.getString("Jahr"));

        Vector<String> selectedKeys = new Vector<String>();
        if (sType == null || sType.equals(STYPE_PERSONS) || sType.equals(STYPE_BOATS) ||
                sType.equals(STYPE_BOATDAMAGES) || sType.equals(STYPE_BOATDAMAGESTAT) ||
                sType.equals(STYPE_BOATRESERVATIONS) || sType.equals(STYPE_BOATSTATUS) ||
                sType.equals(STYPE_CLUBWORK)) {
            selectedKeys.add(SKEY_NAME);
        }
        if (sType == null || sType.equals(STYPE_PERSONS)) {
            selectedKeys.add(SKEY_STATUS);
        }
        if (sType == null || sType.equals(STYPE_PERSONS)) {
            selectedKeys.add(SKEY_YEAROFBIRTH);
        }
        if (sType == null || sType.equals(STYPE_PERSONS)) {
            selectedKeys.add(SKEY_GENDER);
        }
        if (sType == null || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_BOATTYPE);
        }
        if (sType == null || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_BOATSEATS);
        }
        if (sType == null || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_BOATTYPEDETAIL);
        }
        if (sType == null || sType.equals(STYPE_PERSONS) || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_DESTINATION);
        }
        if (sType == null || sType.equals(STYPE_PERSONS) || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_WATERS);
        }
        if (sType == null || sType.equals(STYPE_PERSONS) || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_DISTANCE);
        }
        if (sType == null || sType.equals(STYPE_PERSONS) || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_MONTH);
        }
        if (sType == null || sType.equals(STYPE_PERSONS) || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_WEEKDAY);
        }
        if (sType == null || sType.equals(STYPE_PERSONS) || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_TIMEOFDAY);
        }
        if (sType == null || sType.equals(STYPE_PERSONS) || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_SESSIONTYPE);
        }
        if (sType == null || sType.equals(STYPE_PERSONS) || sType.equals(STYPE_BOATS)) {
            selectedKeys.add(SKEY_YEAR);
        }
        if (selectedKeys.size() == 0) {
            if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                return new String[]{ "" };
            } else {
                return new String[]{ "---" };
            }
        }
        String[] result = new String[selectedKeys.size()];
        for (int i=0; i<result.length; i++) {
            if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                result[i] = selectedKeys.get(i);
            } else {
                result[i] = allKeys.get(selectedKeys.get(i));
            }
        }
        return result;
    }

    public void setOutputType(String type) {
        setString(OUTPUTTYPE, type);
    }

    public String getOutputType() {
        String s = getString(OUTPUTTYPE);
        if (s == null || s.length() == 0) {
            return OTYPE_INTERNAL;
        }
        return s;
    }

    public OutputTypes getOutputTypeEnumFromString(String type) {
        if (type == null) {
            return OutputTypes.UNKNOWN;
        } else if (type.equals(OTYPE_INTERNAL)) {
            return OutputTypes.internal;
        } else if (type.equals(OTYPE_INTERNALTXT)) {
            return OutputTypes.internaltxt;
        } else if (type.equals(OTYPE_HTML)) {
            return OutputTypes.html;
        } else if (type.equals(OTYPE_CSV)) {
            return OutputTypes.csv;
        } else if (type.equals(OTYPE_XML)) {
            return OutputTypes.xml;
        } else if (type.equals(OTYPE_PDF)) {
            return OutputTypes.pdf;
        } else if (type.equals(OTYPE_EFAWETT)) {
            return OutputTypes.efawett;
        }
        return OutputTypes.UNKNOWN;
    }

    public OutputTypes getOutputTypeEnum() {
        return getOutputTypeEnumFromString(getOutputType());
    }

    public String getOutputTypeDescription() {
        switch(getOutputTypeEnum()) {
            case internal:
                return International.getString("intern");
            case internaltxt:
                return International.getString("intern") + " (" +
                        International.getString("einfach") + ")";
            case html:
                return "HTML";
            case csv:
                return "CSV";
            case xml:
                return "XML";
            case pdf:
                return "PDF";
            case efawett:
                return International.onlyFor("Meldedatei", "de") + " (" + Daten.EFA_WETT + ")";
            case UNKNOWN:
            	return EfaTypes.TEXT_UNKNOWN;
        }
        return EfaTypes.TEXT_UNKNOWN;
    }

    public String getOutputTypeFileExtensionForEnum(OutputTypes output) {
        switch(output) {
            case internal:
                return "html";
            case internaltxt:
                return "csv";
            case html:
                return "html";
            case csv:
                return "csv";
            case xml:
                return "xml";
            case pdf:
                return "pdf";
            case efawett:
                return "efw";
            case UNKNOWN:
            	return "out";
        }
        return "out";
    }
    public String getOutputTypeFileExtension() {
        return getOutputTypeFileExtensionForEnum(getOutputTypeEnum());
    }

    public String[] getOutputTypes(int valuesOrDisplay) {
        if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
            return new String[] {
                    OTYPE_INTERNAL,
                    OTYPE_INTERNALTXT,
                    OTYPE_HTML,
                    OTYPE_PDF,
                    OTYPE_CSV,
                    OTYPE_XML,
                    OTYPE_EFAWETT
            };
        } else {
            return new String[] {
                    International.getString("intern"),
                    International.getString("intern") + " (" +
                            International.getString("einfach") + ")",
                    "HTML",
                    "PDF",
                    "CSV",
                    "XML",
                    International.onlyFor("Meldedatei", "de") + " (" + Daten.EFA_WETT + ")",

            };
        }
    }

    public void setFilterGender(DataTypeList<String> list) {
        setList(FILTERGENDER, list);
    }

    public DataTypeList<String> getFilterGender() {
        if (getFilterGenderAll()) {
            return new DataTypeList<String>(Daten.efaTypes.makeGenderArray(EfaTypes.ARRAY_STRINGLIST_VALUES));
        }
        return getList(FILTERGENDER, IDataAccess.DATA_STRING);
    }

    public void setFilterGenderAll(boolean all) {
        setBool(FILTERGENDERALL, all);
    }

    public boolean getFilterGenderAll() {
        return getBool(FILTERGENDERALL);
    }

    public boolean isFilterGenderAllSelected() {
        DataTypeList list = getFilterGender();
        return getFilterGenderAll() || (list != null && list.length() == Daten.efaTypes.makeGenderArray(EfaTypes.ARRAY_STRINGLIST_VALUES).length);
    }

    private String getFilterEfaTypesSelectedListAsText(DataTypeList<String> list, String cat) {
        if (list == null || list.length() == 0) {
            return "<" + International.getString("leer") + ">";
        }
        String slist = null;
        for (int i=0; i<list.length(); i++) {
            slist = (slist != null ? slist + "; " : "") + Daten.efaTypes.getValue(cat, list.get(i));
        }
        return slist;
    }

    public String getFilterGenderSelectedListAsText() {
        return getFilterEfaTypesSelectedListAsText(getFilterGender(), EfaTypes.CATEGORY_GENDER);
    }

    public void setFilterStatus(DataTypeList<UUID> list) {
        setList(FILTERSTATUS, list);
    }

    public DataTypeList<UUID> getFilterStatus() {
        if (getFilterStatusAll()) {
            return new DataTypeList<UUID>(getFilterStatusListValues());
        }
        return getList(FILTERSTATUS, IDataAccess.DATA_UUID);
    }

    public void setFilterStatusAll(boolean all) {
        setBool(FILTERSTATUSALL, all);
    }

    public boolean getFilterStatusAll() {
        return getBool(FILTERSTATUSALL);
    }

    public UUID[] getFilterStatusListValues(boolean withGuestAndOther) {
        return getPersistence().getProject().getStatus(false).makeStatusArrayUUID(withGuestAndOther);
    }

    public UUID[] getFilterStatusListValues() {
        return getFilterStatusListValues(true);
    }

    public String[] getFilterStatusListDisplay() {
        return getPersistence().getProject().getStatus(false).makeStatusArray(Status.ARRAY_STRINGLIST_DISPLAY);
    }

    public boolean isFilterStatusAllSelected() {
        DataTypeList list = getFilterStatus();
        return getFilterStatusAll() || (list != null && list.length() == getFilterStatusListValues().length);
    }

    public String getFilterStatusSelectedListAsText() {
        DataTypeList<UUID> list = getFilterStatus();
        if (list == null || list.length() == 0) {
            return "<" + International.getString("leer") + ">";
        }
        Status status = getPersistence().getProject().getStatus(false);
        String slist = null;
        for (int i=0; i<list.length(); i++) {
            StatusRecord statusRecord = status.getStatus(list.get(i));
            if (statusRecord == null) {
                continue;
            }
            slist = (slist != null ? slist + "; " : "") + statusRecord.getQualifiedName();
        }
        return slist;
    }

    public void setFilterSessionType(DataTypeList<String> list) {
        setList(FILTERSESSIONTYPE, list);
    }

    public DataTypeList<String> getFilterSessionType() {
        if (getFilterSessionTypeAll()) {
            return new DataTypeList<String>(Daten.efaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES));
        }
        return getList(FILTERSESSIONTYPE, IDataAccess.DATA_STRING);
    }

    public void setFilterSessionTypeAll(boolean all) {
        setBool(FILTERSESSIONTYPEALL, all);
    }

    public boolean getFilterSessionTypeAll() {
        return getBool(FILTERSESSIONTYPEALL);
    }

    public boolean isFilterSessionTypeAllSelected() {
        DataTypeList list = getFilterSessionType();
        return getFilterSessionTypeAll() || (list != null && list.length() == Daten.efaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES).length);
    }

    public String getFilterSessionTypeSelectedListAsText() {
        return getFilterEfaTypesSelectedListAsText(getFilterSessionType(), EfaTypes.CATEGORY_SESSION);
    }
    
    private boolean setCompetitionFilterAllPredefinedSessionTypes() {
        DataTypeList<String> selected = getFilterSessionType();
        DataTypeList<String> allknown = new DataTypeList<String>(Daten.efaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES));
        boolean changed = false;
        if (selected == null) {
            selected = new DataTypeList<String>();
            changed = true;
        }
        for (String s : EfaTypes.PREDEFINED_SESSION_TYPES) {
            if (allknown.contains(s) && EfaTypes.couoldBeRowingSession(s) && !selected.contains(s)) {
                selected.add(s);
                sFilterSessionType.put(s, s);
                changed = true;
            }
        }
        if (changed) {
            setFilterSessionType(selected);
        }
        return changed;
    }

    private boolean setCompetitionFilterAllPredefinedBoatTypes() {
        DataTypeList<String> selected = getFilterBoatType();
        DataTypeList<String> allknown = new DataTypeList<String>(Daten.efaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES));
        boolean changed = false;
        if (selected == null) {
            selected = new DataTypeList<String>();
            changed = true;
        }
        for (String s : EfaTypes.PREDEFINED_BOAT_TYPES) {
            if (allknown.contains(s) && EfaTypes.couldBeRowingBoot(s) && !selected.contains(s)) {
                selected.add(s);
                sFilterBoatType.put(s, s);
                changed = true;
            }
        }
        if (changed) {
            setFilterBoatType(selected);
        }
        return changed;
    }

    public void setFilterBoatType(DataTypeList<String> list) {
        setList(FILTERBOATTYPE, list);
    }

    public DataTypeList<String> getFilterBoatType() {
        if (getFilterBoatTypeAll()) {
            return new DataTypeList<String>(Daten.efaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES));
        }
        return getList(FILTERBOATTYPE, IDataAccess.DATA_STRING);
    }

    public void setFilterBoatTypeAll(boolean all) {
        setBool(FILTERBOATTYPEALL, all);
    }

    public boolean getFilterBoatTypeAll() {
        return getBool(FILTERBOATTYPEALL);
    }

    public boolean isFilterBoatTypeAllSelected() {
        DataTypeList list = getFilterBoatType();
        return getFilterBoatTypeAll() || (list != null && list.length() == Daten.efaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES).length);
    }

    public String getFilterBoatTypeSelectedListAsText() {
        return getFilterEfaTypesSelectedListAsText(getFilterBoatType(), EfaTypes.CATEGORY_BOAT);
    }

    public void setFilterBoatSeats(DataTypeList<String> list) {
        setList(FILTERBOATSEATS, list);
    }

    public DataTypeList<String> getFilterBoatSeats() {
        if (getFilterBoatSeatsAll()) {
            return new DataTypeList<String>(Daten.efaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES));
        }
        return getList(FILTERBOATSEATS, IDataAccess.DATA_STRING);
    }

    public void setFilterBoatSeatsAll(boolean all) {
        setBool(FILTERBOATSEATSALL, all);
    }

    public boolean getFilterBoatSeatsAll() {
        return getBool(FILTERBOATSEATSALL);
    }

    public boolean isFilterBoatSeatsAllSelected() {
        DataTypeList list = getFilterBoatSeats();
        return getFilterBoatSeatsAll() || (list != null && list.length() == Daten.efaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES).length);
    }

    public String getFilterBoatSeatsSelectedListAsText() {
        return getFilterEfaTypesSelectedListAsText(getFilterBoatSeats(), EfaTypes.CATEGORY_NUMSEATS);
    }

    public void setFilterBoatRigging(DataTypeList<String> list) {
        setList(FILTERBOATRIGGING, list);
    }

    public DataTypeList<String> getFilterBoatRigging() {
        if (getFilterBoatRiggingAll()) {
            return new DataTypeList<String>(Daten.efaTypes.makeBoatRiggingArray(EfaTypes.ARRAY_STRINGLIST_VALUES));
        }
        return getList(FILTERBOATRIGGING, IDataAccess.DATA_STRING);
    }

    public void setFilterBoatRiggingAll(boolean all) {
        setBool(FILTERBOATRIGGINGALL, all);
    }

    public boolean getFilterBoatRiggingAll() {
        return getBool(FILTERBOATRIGGINGALL);
    }

    public boolean isFilterBoatRiggingAllSelected() {
        DataTypeList list = getFilterBoatRigging();
        return getFilterBoatRiggingAll() || (list != null && list.length() == Daten.efaTypes.makeBoatRiggingArray(EfaTypes.ARRAY_STRINGLIST_VALUES).length);
    }

    public String getFilterBoatRiggingSelectedListAsText() {
        return getFilterEfaTypesSelectedListAsText(getFilterBoatRigging(), EfaTypes.CATEGORY_RIGGING);
    }

    public void setFilterBoatCoxing(DataTypeList<String> list) {
        setList(FILTERBOATCOXING, list);
    }

    public DataTypeList<String> getFilterBoatCoxing() {
        if (getFilterBoatCoxingAll()) {
            return new DataTypeList<String>(Daten.efaTypes.makeBoatCoxingArray(EfaTypes.ARRAY_STRINGLIST_VALUES));
        }
        return getList(FILTERBOATCOXING, IDataAccess.DATA_STRING);
    }

    public void setFilterBoatCoxingAll(boolean all) {
        setBool(FILTERBOATCOXINGALL, all);
    }

    public boolean getFilterBoatCoxingAll() {
        return getBool(FILTERBOATCOXINGALL);
    }

    public boolean isFilterBoatCoxingAllSelected() {
        DataTypeList list = getFilterBoatCoxing();
        return getFilterBoatCoxingAll() || (list != null && list.length() == Daten.efaTypes.makeBoatCoxingArray(EfaTypes.ARRAY_STRINGLIST_VALUES).length);
    }

    public String getFilterBoatCoxingSelectedListAsText() {
        return getFilterEfaTypesSelectedListAsText(getFilterBoatCoxing(), EfaTypes.CATEGORY_COXING);
    }

    public String[] getFilterBoatOwnerArray(int valuesOrDisplay) {
        if (valuesOrDisplay == EfaTypes.ARRAY_STRINGLIST_VALUES) {
            return new String[] {
                    BOWNER_OWN,
                    BOWNER_OTHER,
                    BOWNER_UNKNOWN
            };
        } else {
            return new String[] {
                    International.getString("Vereinsboote"),
                    International.getString("fremde Boote"),
                    International.getString("unbekannte Boote")
            };
        }
    }

    public String getFilterBoatOwnerText(String value) {
        String[] values = getFilterBoatOwnerArray(EfaTypes.ARRAY_STRINGLIST_VALUES);
        String[] text   = getFilterBoatOwnerArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY);
        for (int i=0; i<values.length; i++) {
            if (value.equals(values[i])) {
                return text[i];
            }
        }
        return null;
    }

    public void setFilterBoatOwner(DataTypeList<String> list) {
        setList(FILTERBOATOWNER, list);
    }

    public DataTypeList<String> getFilterBoatOwner() {
        if (getFilterBoatOwnerAll()) {
            return new DataTypeList<String>(getFilterBoatOwnerArray(EfaTypes.ARRAY_STRINGLIST_VALUES));
        }
        return getList(FILTERBOATOWNER, IDataAccess.DATA_STRING);
    }

    public void setFilterBoatOwnerAll(boolean all) {
        setBool(FILTERBOATOWNERALL, all);
    }

    public boolean getFilterBoatOwnerAll() {
        return getBool(FILTERBOATOWNERALL);
    }

    public boolean isFilterBoatOwnerAllSelected() {
        DataTypeList list = getFilterBoatOwner();
        return getFilterBoatOwnerAll() || (list != null && list.length() == getFilterBoatOwnerArray(EfaTypes.ARRAY_STRINGLIST_VALUES).length);
    }

    public String getFilterBoatOwnerSelectedListAsText() {
        DataTypeList<String> list = getFilterBoatOwner();
        if (list == null || list.length() == 0) {
            return "<" + International.getString("leer") + ">";
        }
        String slist = null;
        for (int i=0; i<list.length(); i++) {
            slist = (slist != null ? slist + "; " : "") + getFilterBoatOwnerText(list.get(i));
        }
        return slist;
    }

    public void setFilterByPersonId(UUID id) {
        setUUID(FILTERBYPERSONID, id);
    }
    public UUID getFilterByPersonId() {
        return getUUID(FILTERBYPERSONID);
    }

    public String getFilterByPersonIdAsString(long validAt) {
        try {
            return getPersistence().getProject().getPersons(false).getPerson(sFilterByPersonId, validAt).getQualifiedName();
        } catch (Exception e) {
            return null;
        }
    }

    public void setFilterByPersonText(String name) {
        setString(FILTERBYPERSONTEXT, name);
    }
    public String getFilterByPersonText() {
        return getString(FILTERBYPERSONTEXT);
    }

    public void setFilterByBoatId(UUID id) {
        setUUID(FILTERBYBOATID, id);
    }
    public UUID getFilterByBoatId() {
        return getUUID(FILTERBYBOATID);
    }

    public String getFilterByBoatIdAsString(long validAt) {
        try {
            return getPersistence().getProject().getBoats(false).getBoat(sFilterByBoatId, validAt).getQualifiedName();
        } catch (Exception e) {
            return null;
        }
    }

    public void setFilterByBoatText(String name) {
        setString(FILTERBYBOATTEXT, name);
    }
    public String getFilterByBoatText() {
        return getString(FILTERBYBOATTEXT);
    }

    public void setFilterByGroupId(UUID id) {
        setUUID(FILTERBYGROUPID, id);
    }
    public UUID getFilterByGroupId() {
        return getUUID(FILTERBYGROUPID);
    }

    public void setFilterPromptPerson(boolean prompt) {
        setBool(FILTERPROMPTPERSON, prompt);
    }
    public boolean getFilterPromptPerson() {
        return getBool(FILTERPROMPTPERSON);
    }

    public void setFilterPromptBoat(boolean prompt) {
        setBool(FILTERPROMPTBOAT, prompt);
    }
    public boolean getFilterPromptBoat() {
        return getBool(FILTERPROMPTBOAT);
    }

    public void setFilterPromptGroup(boolean prompt) {
        setBool(FILTERPROMPTGROUP, prompt);
    }
    public boolean getFilterPromptGroup() {
        return getBool(FILTERPROMPTGROUP);
    }

    public void setFilterFromToBoathouse(boolean startIsBoathouse) {
        setBool(FILTERFROMTOBOATHOUSE, startIsBoathouse);
    }
    public boolean getFilterFromToBoathouse() {
        return getBool(FILTERFROMTOBOATHOUSE);
    }

    public void setFilterNameContains(String namepart) {
        setString(FILTERNAMECONTAINS, namepart);
    }
    public String getFilterNameContains() {
        return getString(FILTERNAMECONTAINS);
    }

    public void setFilterCommentsInclude(String comments) {
        setString(FILTERCOMMENTSINCLUDE, comments);
    }
    public String getFilterCommentsInclude() {
        return getString(FILTERCOMMENTSINCLUDE);
    }

    public void setFilterCommentsExclude(String comments) {
        setString(FILTERCOMMENTSEXCLUDE, comments);
    }
    public String getFilterCommentsExclude() {
        return getString(FILTERCOMMENTSEXCLUDE);
    }

    public void setFilterMinDessionDistance(DataTypeDistance distance) {
        setDistance(FILTERMINSESSIONDISTANCE, distance);
    }
    public DataTypeDistance getFilterMinDessionDistance() {
        return getDistance(FILTERMINSESSIONDISTANCE);
    }

    public void setFilterOnlyOpenDamages(boolean openDamages) {
        setBool(FILTERONLYOPENDAMAGES, openDamages);
    }
    public boolean getFilterOnlyOpenDamages() {
        return getBool(FILTERONLYOPENDAMAGES);
    }

    public void setFilterAlsoOpenSessions(boolean openSessions) {
        setBool(FILTERALSOOPENSESSIONS, openSessions);
    }
    public boolean getFilterAlsoOpenSessions() {
        return getBool(FILTERALSOOPENSESSIONS);
    }

    public void setFilterOnlyLogbook(String logbookName) {
        setString(FILTERONLYLOGBOOK, logbookName);
    }
    public String getFilterOnlyLogbook() {
        return getString(FILTERONLYLOGBOOK);
    }

    public String getFilterByGroupIdAsString(long validAt) {
        try {
            return getPersistence().getProject().getGroups(false).findGroupRecord(sFilterByGroupId, validAt).getQualifiedName();
        } catch (Exception e) {
            return null;
        }
    }

    public String getFilterCriteriaAsStringDescription() {
        String filter = null;
        if (getStatisticCategoryEnum() != StatisticCategory.other ||
                getStatisticTypeEnum() == StatisticType.clubwork) {
            if (!isFilterGenderAllSelected()) {
                filter = (filter == null ? "" : filter + "\n")
                        + International.getString("Geschlecht") + ": " + getFilterGenderSelectedListAsText();
            }
            if (!isFilterStatusAllSelected()) {
                filter = (filter == null ? "" : filter + "\n")
                        + International.getString("Status") + ": " + getFilterStatusSelectedListAsText();
            }
        }
        if (!isFilterSessionTypeAllSelected()) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Fahrtart") + ": " + getFilterSessionTypeSelectedListAsText();
        }
        if (!isFilterBoatTypeAllSelected()) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Bootstyp") + ": " + getFilterBoatTypeSelectedListAsText();
        }
        if (!isFilterBoatSeatsAllSelected()) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Bootsplätze") + ": " + getFilterBoatSeatsSelectedListAsText();
        }
        if (!isFilterBoatRiggingAllSelected()) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Riggerung") + ": " + getFilterBoatRiggingSelectedListAsText();
        }
        if (!isFilterBoatCoxingAllSelected()) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Steuerung") + ": " + getFilterBoatCoxingSelectedListAsText();
        }
        if (!isFilterBoatOwnerAllSelected()) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Eigentümer") + ": " + getFilterBoatOwnerSelectedListAsText();
        }
        if (sFilterByPersonId != null) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Person") + ": " + getFilterByPersonIdAsString(sValidAt);
        }
        if (sFilterByPersonText != null) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Person") + ": " + sFilterByPersonText;
        }
        if (sFilterByBoatId != null) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Boot") + ": " + getFilterByBoatIdAsString(sValidAt);
        }
        if (sFilterByBoatText != null) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Boot") + ": " + sFilterByBoatText;
        }
        if (sFilterByGroupId != null) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Gruppe") + ": " + getFilterByGroupIdAsString(sValidAt);
        }
        if (sFilterFromToBoathouse) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("nur Fahrten") + ": " +
                    International.getString("Start und Ziel ist Bootshaus");
        }
        if (sFilterNameContains != null) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Name enthält") + ": " +
                    sFilterNameContains;
        }
        if (sFilterCommentsInclude != null) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Bemerkungsfeld enthält") + ": " +
                    sFilterCommentsInclude;
        }
        if (sFilterCommentsExclude != null) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Bemerkungsfeld enthält nicht") + ": " +
                    sFilterCommentsExclude;
        }
        if (sFilterMinSessionDistance != null && sFilterMinSessionDistance.isSet()) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("Mindestentfernung der Fahrt") + ": " +
                    sFilterMinSessionDistance.getAsFormattedString();
        }
        if (sFilterAlsoOpenSessions) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("inkl. offener Fahrten");
        }
        if (sFilterOnlyOpenDamages) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getString("nur offene Bootsschäden");
        }
        if (sFilterOnlyLogbook != null) {
            filter = (filter == null ? "" : filter + "\n") +
                    International.getMessage("nur Fahrtenbuch {logbook}", sFilterOnlyLogbook);
        }
        return filter;
    }

    public void setShowFields(DataTypeList<String> list) {
        setList(SHOWFIELDS, list);
    }

    public DataTypeList<String> getShowFields() {
        return getList(SHOWFIELDS, IDataAccess.DATA_STRING);
    }

    public void setShowLogbookFields(DataTypeList<String> list) {
        setList(SHOWLOGBOOKFIELDS, list);
    }

    public DataTypeList<String> getShowLogbookFields() {
        return getList(SHOWLOGBOOKFIELDS, IDataAccess.DATA_STRING);
    }

    public void setShowOtherFields(DataTypeList<String> list) {
        setList(SHOWOTHERFIELDS, list);
    }

    public DataTypeList<String> getShowOtherFields() {
        return getList(SHOWOTHERFIELDS, IDataAccess.DATA_STRING);
    }

    public String[] getFieldsList(int valuesOrDisplay) {
        if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
            return new String[]{
                    FIELDS_POSITION,
                    FIELDS_NAME,
                    FIELDS_GENDER,
                    FIELDS_STATUS,
                    FIELDS_CLUB,
                    FIELDS_YEAROFBIRTH,
                    FIELDS_MEMBERNO,
                    FIELDS_BOATTYPE
            };
        } else {
            return new String[]{
                    International.getString("Position"),
                    International.getString("Name"),
                    International.getString("Geschlecht"),
                    International.getString("Status"),
                    International.getString("Verein"),
                    International.getString("Jahrgang"),
                    International.getString("Mitgliedsnummer"),
                    International.getString("Bootstyp")
            };
        }
    }

    public String[] getLogbookFieldsList(int valuesOrDisplay) {
        Vector<String> strings = new Vector<String>();
        if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
            strings.add(LFIELDS_ENTRYNO);
            strings.add(LFIELDS_DATE);
            strings.add(LFIELDS_ENDDATE);
            strings.add(LFIELDS_BOAT);
            strings.add(LFIELDS_COX);
            strings.add(LFIELDS_CREW);
            strings.add(LFIELDS_STARTTIME);
            strings.add(LFIELDS_ENDTIME);
            strings.add(LFIELDS_WATERS);
            strings.add(LFIELDS_DESTINATION);
            strings.add(LFIELDS_DESTDETAILS);
            if (Daten.efaConfig.getValueUseFunctionalityRowingBerlin()) {
                strings.add(LFIELDS_DESTAREAS);
            }
            strings.add(LFIELDS_DISTANCE);
            strings.add(LFIELDS_MULTIDAY);
            strings.add(LFIELDS_SESSIONTYPE);
            strings.add(LFIELDS_NOTES);
        } else {
            strings.add(International.getString("Lfd. Nr."));
            strings.add(International.getString("Datum"));
            strings.add(International.getString("Enddatum"));
            strings.add(International.getString("Boot"));
            strings.add(International.getString("Steuermann"));
            strings.add(International.getString("Mannschaft"));
            strings.add(International.getString("Abfahrt"));
            strings.add(International.getString("Ankunft"));
            strings.add(International.getString("Gewässer"));
            strings.add(International.getString("Ziel"));
            strings.add(International.getString("Ziel") + " (" +
                    International.getString("Details")  + ")");
            if (Daten.efaConfig.getValueUseFunctionalityRowingBerlin()) {
                strings.add(International.onlyFor("Zielgebiete", "de"));
            }
            strings.add(International.getString("Kilometer"));
            strings.add(International.getString("Wanderfahrten"));
            strings.add(International.getString("Fahrtart"));
            strings.add(International.getString("Bemerkungen"));
        }
        String[] a = new String[strings.size()];
        for (int i=0; i<a.length; i++) {
            a[i] = strings.get(i);
        }
        return a;
    }

    public String[] getOtherFieldsListDefaults(StatisticType type, int valuesOrDisplay) {
        String[] fields = getOtherFieldsList(type, ARRAY_STRINGLIST_VALUES);
        int count = 0;
        for (int i=0; i<fields.length; i++) {
            if (!fields[i].equals(FIELDS_BOATTYPE)) {
                count++;
            }
        }
        String[] deffields = new String[count];
        for (int idx=0, i=0; i<fields.length; i++) {
            if (!fields[i].equals(FIELDS_BOATTYPE)) {
                deffields[idx++] = fields[i];
            }
        }
        return deffields;
    }

    public String[] getOtherFieldsList(StatisticType type, int valuesOrDisplay) {
        switch (type) {
            case boatstatus:
                if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                    return new String[]{
                            FIELDS_NAME,
                            FIELDS_BOATTYPE,
                            OFIELDS_BASESTATUS,
                            OFIELDS_CURRENTSTATUS,
                            OFIELDS_COMMENTS};
                } else {
                    return new String[]{
                            International.getString("Name"),
                            International.getString("Bootstyp"),
                            International.getString("Basis-Status"),
                            International.getString("aktueller Status"),
                            International.getString("Bemerkungen")
                    };
                }
            case boatreservations:
                if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                    return new String[]{
                            FIELDS_NAME,
                            FIELDS_BOATTYPE,
                            OFIELDS_RESERVEDFROM,
                            OFIELDS_RESERVEDTO,
                            OFIELDS_RESERVEDFOR,
                            OFIELDS_REASON,
                            OFIELDS_CONTACT
                    };
                } else {
                    return new String[]{
                            International.getString("Name"),
                            International.getString("Bootstyp"),
                            International.getString("Reserviert von"),
                            International.getString("Reserviert bis"),
                            International.getString("Reserviert für"),
                            International.getString("Grund"),
                            International.getString("Kontakt")
                    };
                }
            case boatdamages:
                if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                    return new String[]{
                            FIELDS_NAME,
                            FIELDS_BOATTYPE,
                            OFIELDS_DAMAGE,
                            OFIELDS_DAMAGESEVERITY,
                            OFIELDS_REPORTEDON,
                            OFIELDS_FIXEDON};
                } else {
                    return new String[]{
                            International.getString("Name"),
                            International.getString("Bootstyp"),
                            International.getString("Schaden"),
                            International.getString("Schwere des Schadens"),
                            International.getString("gemeldet am"),
                            International.getString("behoben am")
                    };
                }
            case boatdamagestat:
                if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                    return new String[]{
                            FIELDS_POSITION,
                            FIELDS_NAME,
                            FIELDS_BOATTYPE};
                } else {
                    return new String[]{
                            International.getString("Position"),
                            International.getString("Name"),
                            International.getString("Bootstyp")
                    };
                }
            default:
                return new String[0];
        }
    }

    public void setAggregations(DataTypeList<String> list) {
        setList(AGGREGATIONS, list);
    }

    public DataTypeList<String> getAggregations() {
        return getList(AGGREGATIONS, IDataAccess.DATA_STRING);
    }

    public String[] getAggregationList(StatisticType type, int valuesOrDisplay) {
        Hashtable<String,String> allKeys = new Hashtable<String,String>();
        allKeys.put(AGGR_DISTANCE, DataTypeDistance.getDefaultUnitName());
        allKeys.put(AGGR_ROWDISTANCE, getRowingKmString());
        allKeys.put(AGGR_COXDISTANCE, getCoxingKmString());
        allKeys.put(AGGR_SESSIONS, International.getString("Fahrten"));
        allKeys.put(AGGR_AVGDISTANCE, DataTypeDistance.getDefaultUnitAbbrevation(true) + "/" + International.getString("Fahrt"));
        allKeys.put(AGGR_DURATION, International.getString("Dauer"));
        allKeys.put(AGGR_SPEED, International.getString("Geschwindigkeit"));
        allKeys.put(AGGR_WANDERFAHRTEN, International.onlyFor("Wafa-Km", "de"));
        allKeys.put(AGGR_ZIELFAHRTEN, International.onlyFor("Zielfahrten", "de"));
        allKeys.put(AGGR_DAMAGECOUNT, International.getString("Schäden"));
        allKeys.put(AGGR_DAMAGEDURATION, International.getString("Reparaturdauer") + " (" +
                International.getString("Tage") + ")");
        allKeys.put(AGGR_DAMAGEAVGDUR, International.getString("Reparaturdauer") + "/" +
                International.getString("Schaden"));
        allKeys.put(AGGR_CLUBWORK, International.getString("Vereinsarbeit"));
        allKeys.put(AGGR_CLUBWORKTARGET, International.getString("Vereinsarbeit")+"-"+International.getString("Soll"));
        allKeys.put(AGGR_CBRELTOTARGET, International.getString("Vereinsarbeit")
                + " (" + International.getString("relativ zum Soll") + ")");
        allKeys.put(AGGR_CBOVERUNDERCARRYOVER, International.getString("Vereinsarbeit")
                + " (" + International.getString("über/unter Jahresübertrag") + ")");
        allKeys.put(AGGR_CLUBWORKCREDIT, International.getString("Vereinsarbeit")
                + " (" + International.getString("nur Gutschriften") + ")");

        Vector<String> selectedKeys = new Vector<String>();
        if (type == StatisticType.persons || type == StatisticType.boats) {
            selectedKeys.add(AGGR_DISTANCE);
            selectedKeys.add(AGGR_ROWDISTANCE);
            selectedKeys.add(AGGR_COXDISTANCE);
            selectedKeys.add(AGGR_SESSIONS);
            selectedKeys.add(AGGR_AVGDISTANCE);
            selectedKeys.add(AGGR_DURATION);
            selectedKeys.add(AGGR_SPEED);
            if (Daten.efaConfig.getValueUseFunctionalityRowingGermany()) {
                selectedKeys.add(AGGR_WANDERFAHRTEN);
            }
            if (Daten.efaConfig.getValueUseFunctionalityRowingBerlin()) {
                selectedKeys.add(AGGR_ZIELFAHRTEN);
            }
        }
        if (type == StatisticType.boatdamagestat) {
            selectedKeys.add(AGGR_DAMAGECOUNT);
            selectedKeys.add(AGGR_DAMAGEDURATION);
            selectedKeys.add(AGGR_DAMAGEAVGDUR);
        }
        if (type == StatisticType.clubwork) {
            selectedKeys.add(AGGR_CLUBWORK);
            selectedKeys.add(AGGR_CLUBWORKTARGET);
            selectedKeys.add(AGGR_CBRELTOTARGET);
            selectedKeys.add(AGGR_CBOVERUNDERCARRYOVER);
            selectedKeys.add(AGGR_CLUBWORKCREDIT);
            selectedKeys.add(AGGR_DISTANCE);
        }

        String[] result = new String[selectedKeys.size()];
        for (int i=0; i<result.length; i++) {
            if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
                result[i] = selectedKeys.get(i);
            } else {
                result[i] = allKeys.get(selectedKeys.get(i));
            }
        }
        return result;
    }

    public void setAggrDistanceBarSize(int size) {
        setInt(AGGRDISTANCEBARSIZE, size);
    }
    public int getAggrDistanceBarSize() {
        int size = getInt(AGGRDISTANCEBARSIZE);
        return (size > 0 ? size : 0);
    }

    public void setAggrRowDistanceBarSize(int size) {
        setInt(AGGRROWDISTANCEBARSIZE, size);
    }
    public int getAggrRowDistanceBarSize() {
        int size = getInt(AGGRROWDISTANCEBARSIZE);
        return (size > 0 ? size : 0);
    }

    public void setAggrCoxDistanceBarSize(int size) {
        setInt(AGGRCOXDISTANCEBARSIZE, size);
    }
    public int getAggrCoxDistanceBarSize() {
        int size = getInt(AGGRCOXDISTANCEBARSIZE);
        return (size > 0 ? size : 0);
    }

    public void setAggrSessionsBarSize(int size) {
        setInt(AGGRSESSIONSBARSIZE, size);
    }
    public int getAggrSessionsBarSize() {
        int size = getInt(AGGRSESSIONSBARSIZE);
        return (size > 0 ? size : 0);
    }

    public void setAggrAvgDistanceBarSize(int size) {
        setInt(AGGRAVGDISTBARSIZE, size);
    }
    public int getAggrAvgDistanceBarSize() {
        int size = getInt(AGGRAVGDISTBARSIZE);
        return (size > 0 ? size : 0);
    }

    public void setAggrDurationBarSize(int size) {
        setInt(AGGRDURATIONBARSIZE, size);
    }
    public int getAggrDurationBarSize() {
        int size = getInt(AGGRDURATIONBARSIZE);
        return (size > 0 ? size : 0);
    }

    public void setAggrSpeedBarSize(int size) {
        setInt(AGGRSPEEDBARSIZE, size);
    }
    public int getAggrSpeedBarSize() {
        int size = getInt(AGGRSPEEDBARSIZE);
        return (size > 0 ? size : 0);
    }

    public String getSortingCriteria() {
        return getString(SORTINGCRITERIA);
    }
    public void setSortingCriteria(String sorting) {
        setString(SORTINGCRITERIA, sorting);
    }

    public static SortingCriteria getSortingCriteriaEnum(String sort) {
        if (sort == null) {
            return SortingCriteria.UNKNOWN;
        } else if (sort.equals(SORTING_DISTANCE)) {
            return SortingCriteria.distance;
        } else if (sort.equals(SORTING_ROWDISTANCE)) {
            return SortingCriteria.rowdistance;
        } else if (sort.equals(SORTING_COXDISTANCE)) {
            return SortingCriteria.coxdistance;
        } else if (sort.equals(SORTING_SESSIONS)) {
            return SortingCriteria.sessions;
        } else if (sort.equals(SORTING_AVGDISTANCE)) {
            return SortingCriteria.avgDistance;
        } else if (sort.equals(SORTING_DURATION)) {
            return SortingCriteria.duration;
        } else if (sort.equals(SORTING_SPEED)) {
            return SortingCriteria.speed;
        } else if (sort.equals(SORTING_NAME)) {
            return SortingCriteria.name;
        } else if (sort.equals(SORTING_GENDER)) {
            return SortingCriteria.gender;
        } else if (sort.equals(SORTING_STATUS)) {
            return SortingCriteria.status;
        } else if (sort.equals(SORTING_YEAROFBIRTH)) {
            return SortingCriteria.yearOfBirth;
        } else if (sort.equals(SORTING_MEMBERNO)) {
            return SortingCriteria.memberNo;
        } else if (sort.equals(SORTING_BOATTYPE)) {
            return SortingCriteria.boatType;
        } else if (sort.equals(SORTING_ENTRYNO)) {
            return SortingCriteria.entryNo;
        } else if (sort.equals(SORTING_DATE)) {
            return SortingCriteria.date;
        } else if (sort.equals(SORTING_DAMAGECOUNT)) {
            return SortingCriteria.damageCount;
        } else if (sort.equals(SORTING_DAMAGEDUR)) {
            return SortingCriteria.damageDuration;
        } else if (sort.equals(SORTING_DAMAGEAVGDUR)) {
            return SortingCriteria.damageAvgDuration;
        } else if (sort.equals(SORTING_CLUBWORK)) {
            return SortingCriteria.clubwork;
        }
        return SortingCriteria.UNKNOWN;
    }

    public SortingCriteria getSortingCriteriaEnum() {
        String sort = getSortingCriteria();
        return getSortingCriteriaEnum(sort);
    }

    public String getSortingCriteriaDescription() {
        switch(getSortingCriteriaEnum()) {
            case distance:
                return International.getString("Kilometer");
            case rowdistance:
                return getRowingKmString();
            case coxdistance:
                return getCoxingKmString();
            case sessions:
                return International.getString("Fahrten");
            case avgDistance:
                return International.getString("Km/Fahrt");
            case duration:
                return International.getString("Dauer");
            case speed:
                return International.getString("Geschwindigkeit");
            case name:
                return International.getString("Name");
            case status:
                return International.getString("Status");
            case yearOfBirth:
                return International.getString("Jahrgang");
            case boatType:
                return International.getString("Bootstyp");
            case entryNo:
                return International.getString("LfdNr");
            case date:
                return International.getString("Datum");
            case damageCount:
                return International.getString("Anzahl") + " " +
                        International.getString("Schäden");
            case damageDuration:
                return International.getString("Reparaturdauer");
            case damageAvgDuration:
                return International.getString("Reparaturdauer") + "/" +
                        International.getString("Schaden");
            case clubwork:
                return International.getString("Vereinsarbeit");
            
            // obviously non-supported sort orders
            case memberNo:
            	return EfaTypes.TEXT_UNKNOWN;
            case gender:
            	return EfaTypes.TEXT_UNKNOWN;
            case days:
            	return EfaTypes.TEXT_UNKNOWN;
            case UNKNOWN:
            	return EfaTypes.TEXT_UNKNOWN;
            
        }
        return EfaTypes.TEXT_UNKNOWN;
    }

    public String[] getSortingCriteria(int valuesOrDisplay) {
        if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
            return new String[] {
                    SORTING_DISTANCE,
                    SORTING_SESSIONS,
                    SORTING_NAME,
                    SORTING_ROWDISTANCE,
                    SORTING_COXDISTANCE,
                    SORTING_AVGDISTANCE,
                    SORTING_DURATION,
                    SORTING_SPEED,
                    SORTING_GENDER,
                    SORTING_STATUS,
                    SORTING_YEAROFBIRTH,
                    SORTING_MEMBERNO,
                    SORTING_BOATTYPE,
                    SORTING_ENTRYNO,
                    SORTING_DATE,
                    SORTING_DAMAGECOUNT,
                    SORTING_DAMAGEDUR,
                    SORTING_DAMAGEAVGDUR,
                    SORTING_CLUBWORK
            };
        } else {
            return new String[] {
                    International.getString("Kilometer"),
                    International.getString("Fahrten"),
                    International.getString("Name"),
                    Daten.efaConfig.getRowingAndOrPaddlingString() +
                            International.getSpaceOrDash() +
                            International.getString("Kilometer"),
                    International.getString("Steuer", "wie in Steuer-Km") +
                            International.getSpaceOrDash() +
                            International.getString("Kilometer"),
                    International.getString("Km/Fahrt"),
                    International.getString("Dauer"),
                    International.getString("Geschwindigkeit"),
                    International.getString("Geschlecht"),
                    International.getString("Status"),
                    International.getString("Jahrgang"),
                    International.getString("Mitgliedsnummer"),
                    International.getString("Bootstyp"),
                    International.getString("LfdNr"),
                    International.getString("Datum"),
                    International.getString("Anzahl") + " " +
                            International.getString("Schäden"),
                    International.getString("Reparaturdauer"),
                    International.getString("Reparaturdauer") + "/" +
                            International.getString("Schaden"),
                    International.getString("Vereinsarbeit")
            };
        }
    }

    public String getSortingOrder() {
        return getString(SORTINGORDER);
    }
    public void setSortingOrder(String order) {
        setString(SORTINGORDER, order);
    }

    public static boolean getSortingOrderAscending(String sort) {
        return (sort == null || sort.equals(SORTINGORDER_ASC));
    }

    public boolean getSortingOrderAscending() {
        String sort = getSortingOrder();
        return getSortingOrderAscending(sort);
    }

    public String getSortingOrderDescription() {
        if(getSortingOrderAscending()) {
            return International.getString("aufsteigend");
        } else {
            return International.getString("absteigend");
        }
    }

    public String[] getSortingOrders(int valuesOrDisplay) {
        if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
            return new String[] {
                    SORTINGORDER_ASC,
                    SORTINGORDER_DESC
            };
        } else {
            return new String[] {
                    International.getString("aufsteigend"),
                    International.getString("absteigend")
            };
        }
    }

    public String getOutputFile() {
        String fname = getString(OUTPUTFILE);
        if (fname == null || fname.length() == 0) {
            fname = Daten.efaTmpDirectory + "output." + getOutputTypeFileExtension();
        }
        return fname;
    }
    public void setOutputFile(String file) {
        setString(OUTPUTFILE, file);
    }

    public boolean getOutputHtmlUpdateTable() {
        return getBool(OUTPUTHTMLUPDATETABLE);
    }
    public void setOutputHtmlUpdateTable(boolean updateOnlyTable) {
        setBool(OUTPUTHTMLUPDATETABLE, updateOnlyTable);
    }

    public String getOutputEncoding() {
        String encoding = getString(OUTPUTENCODING);
        if (encoding == null || encoding.length() == 0) {
            return Daten.ENCODING_UTF;
        }
        return encoding;
    }
    public void setOutputEncoding(String encoding) {
        setString(OUTPUTENCODING, encoding);
    }

    public String getOutputCsvSeparator() {
        String separator = getString(OUTPUTCSVSEPARATOR);
        if (separator == null || separator.length() == 0) {
            return DEFAULT_GUI_CSV_COLUMN_SEPARATOR;
        }
        return separator;
    }
    public void setOutputCsvSeparator(String separator) {
        setString(OUTPUTCSVSEPARATOR, separator);
    }

    public String getOutputCsvQuotes() {
        String quotes = getString(OUTPUTCSVQUOTES);
        if (quotes == null) {
            return DEFAULT_GUI_CSV_QUOTE;
        }
        return quotes;
    }
    public void setOutputCsvQuotes(String quotes) {
        setString(OUTPUTCSVQUOTES, quotes);
    }

    public int getCompYear() {
        return getInt(COMPYEAR);
    }
    public void setCompYear(int year) {
        setInt(COMPYEAR, year);
    }

    public int getCompPercentFulfilled() {
        return getInt(COMPPERCENTFULFILLED);
    }
    public void setCompPercentFulfilled(int pct) {
        setInt(COMPPERCENTFULFILLED, pct);
    }

    public boolean getCompOutputShort() {
        return getBool(COMPOUTPUTSHORT);
    }
    public void setCompOutputShort(boolean bool) {
        setBool(COMPOUTPUTSHORT, bool);
    }

    public boolean getCompOutputRules() {
        return getBool(COMPOUTPUTRULES);
    }
    public void setCompOutputRules(boolean bool) {
        setBool(COMPOUTPUTRULES, bool);
    }

    public boolean getCompOutputAdditionalWithRequirements() {
        return getBool(COMPOUTPUTADDITIONALWITHREQUIREMENTS);
    }
    public void setCompOutputAdditionalWithRequirements(boolean bool) {
        setBool(COMPOUTPUTADDITIONALWITHREQUIREMENTS, bool);
    }

    public boolean getCompOutputWithoutDetails() {
        return getBool(COMPOUTPUTWITHOUTDETAILS);
    }
    public void setCompOutputWithoutDetails(boolean bool) {
        setBool(COMPOUTPUTWITHOUTDETAILS, bool);
    }

    public boolean getCompOutputAllDestinationAreas() {
        return getBool(COMPOUTPUTALLDESTINATIONAREAS);
    }
    public void setCompOutputAllDestinationAreas(boolean bool) {
        setBool(COMPOUTPUTALLDESTINATIONAREAS, bool);
    }

    public boolean getOptionDistanceWithUnit() {
        return getBool(OPTIONDISTANCEWITHUNIT);
    }
    public void setOptionDistanceWithUnit(boolean enabled) {
        setBool(OPTIONDISTANCEWITHUNIT, enabled);
    }

    public boolean getOptionTruncateDistance() {
        return getBool(OPTIONTRUNCATEDIST);
    }
    public void setOptionTruncateDistance(boolean enabled) {
        setBool(OPTIONTRUNCATEDIST, enabled);
    }

    public boolean getOptionListAllNullEntries() {
        return getBool(OPTIONLISTALLNULLENTRIES);
    }
    public void setOptionListAllNullEntries(boolean allNullEntries) {
        setBool(OPTIONLISTALLNULLENTRIES, allNullEntries);
    }

    public boolean getOptionIgnoreNullValues() {
        return getBool(OPTIONIGNORENULLVALUES);
    }
    public void setOptionIgnoreNullValues(boolean ignore) {
        setBool(OPTIONIGNORENULLVALUES, ignore);
    }

    public boolean getOptionSumGuestsAndOthers() {
        return getBool(OPTIONSUMGUESTSANDOTHERS);
    }
    public void setOptionSumGuestsAndOthers(boolean sumGuests) {
        setBool(OPTIONSUMGUESTSANDOTHERS, sumGuests);
    }

    public boolean getOptionSumGuestsByClub() {
        return getBool(OPTIONSUMGUESTSBYCLUB);
    }
    public void setOptionSumGuestsByClub(boolean byClub) {
        setBool(OPTIONSUMGUESTSBYCLUB, byClub);
    }

    public boolean getOptionShowValidLastTrip() {
        return SHOWDATAVALID_LASTTRIPTIME.equals(getString(OPTIONSHOWVALIDLASTTRIP));
    }
    public void setOptionShowValidLastTrip(boolean validLastTrip) {
        setString(OPTIONSHOWVALIDLASTTRIP, validLastTrip ? SHOWDATAVALID_LASTTRIPTIME : StatisticsRecord.SHOWDATAVALID_STATENDTIME);
    }

    public boolean getOptionOnlyMembersWithInsufficientClubwork() {
        return getBool(OPTIONONLYMEMBERSWITHINSUFFICIENTCLUBWORK);
    }

    public void setOptionOnlyMembersWithInsufficientClubwork(boolean ignore) {
        setBool(OPTIONONLYMEMBERSWITHINSUFFICIENTCLUBWORK, ignore);
    }

    public int getLogbookFieldCount() {
        DataTypeList l = getShowLogbookFields();
        return (l != null ? l.length() : 0);
    }

    public String[] getQualifiedNameFields() {
        return IDX_NAME;
    }

    public Object getUniqueIdForRecord() {
        return getId();
    }

    public String getQualifiedName() {
        String name = getName();
        return (name != null ? name : "");
    }

    public static String getRowingKmString() {
        return Daten.efaConfig.getRowingAndOrPaddlingString() +
                International.getSpaceOrDash() +
                DataTypeDistance.getDefaultUnitAbbrevation(true);
    }

    public static String getCoxingKmString() {
        return International.getString("Steuer", "wie in Steuer-Km") +
                International.getSpaceOrDash() +
                DataTypeDistance.getDefaultUnitAbbrevation(true);
    }

    public Vector<IItemType> getGuiItems(AdminRecord admin) {
        String CAT_BASEDATA     = "%01%" + International.getString("Statistik");
        String CAT_FILTER       = "%02%" + International.getString("Filter");
        String CAT_FILTERGENDER        = "%021%" + International.getString("Geschlecht");
        String CAT_FILTERSTATUS        = "%022%" + International.getString("Status");
        String CAT_FILTERSESSIONTYPE   = "%023%" + International.getString("Fahrtart");
        String CAT_FILTERBOATTYPE      = "%024%" + International.getString("Bootstyp");
        String CAT_FILTERBOATSEAT      = "%025%" + International.getString("Bootsplätze");
        String CAT_FILTERBOATRIGG      = "%026%" + International.getString("Riggerung");
        String CAT_FILTERBOATCOXING    = "%027%" + International.getString("Steuerung");
        String CAT_FILTERBOATOWNER     = "%028%" + International.getString("Eigentümer");
        String CAT_FILTERINDIVIDUAL    = "%029%" + International.getString("Individuell");
        String CAT_FILTERVARIOUS       = "%02A%" + International.getString("Weitere");
        String CAT_FIELDS       = "%03%" + International.getString("Ausgabefelder");
        String CAT_FIELDSCALC   = "%031%" + International.getString("Berechnung");
        String CAT_FIELDSLIST   = "%032%" + International.getString("Kilometerliste");
        String CAT_FIELDSLOGBOOK= "%033%" + International.getString("Fahrtenbuch");
        String CAT_FIELDSOTHER  = "%034%" + International.getString("Weitere");
        String CAT_FIELDSBARS   = "%035%" + International.getString("Balken");
        String CAT_SORTING      = "%04%" + International.getString("Sortierung");
        String CAT_COMP         = "%05%" + International.getString("Wettbewerbe");
        String CAT_OUTPUT       = "%06%" + International.getString("Ausgabe");
        String CAT_OPTIONS      = "%07%" + International.getString("Optionen");
        IItemType item;
        Vector<IItemType> v = new Vector<IItemType>();

        // CAT_BASEDATA
        v.add(item = new ItemTypeString(StatisticsRecord.NAME, getName(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Name")));
        item.setNotNull(true);
        v.add(item = new ItemTypeBoolean(StatisticsRecord.PUBLICLYAVAILABLE, getPubliclyAvailable(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Statistik allgemein verfügbar")));
        ((ItemTypeBoolean)item).registerItemListener(this);
        v.add(item = new ItemTypeDate(StatisticsRecord.DATEFROM, getDateFrom(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Von")));
        itemDateFrom = (ItemTypeDate)item;
        v.add(item = new ItemTypeDate(StatisticsRecord.DATETO, getDateTo(),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA, International.getString("Bis")));
        ((ItemTypeDate)item).setMustBeAfter(itemDateFrom, true);
        itemDateTo = (ItemTypeDate)item;
        v.add(item = new ItemTypeStringList(StatisticsRecord.STATISTICCATEGORY, getStatisticCategory(),
                getStatisticCategories(ARRAY_STRINGLIST_VALUES), getStatisticCategories(ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                International.getString("Statistiktyp")));
        item.registerItemListener(this);
        this.itemStatisticCategory = (ItemTypeStringList)item;
        v.add(item = new ItemTypeStringList(StatisticsRecord.STATISTICTYPE, getStatisticType(),
                getStatisticTypes(getStatisticCategoryEnum(), ARRAY_STRINGLIST_VALUES),
                getStatisticTypes(getStatisticCategoryEnum(), ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                International.getString("Statistikart")));
        item.registerItemListener(this);
        this.itemStatisticType = (ItemTypeStringList)item;
        v.add(item = new ItemTypeStringList(StatisticsRecord.STATISTICKEY, getStatisticKey(),
                getStatisticKeys(getStatisticType(), ARRAY_STRINGLIST_VALUES),
                getStatisticKeys(getStatisticType(), ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, CAT_BASEDATA,
                International.getString("Statistikschlüssel")));
        this.itemStatisticKey = (ItemTypeStringList)item;

        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.FILTERGENDER, getFilterGender(),
                Daten.efaTypes.makeGenderArray(EfaTypes.ARRAY_STRINGLIST_VALUES), Daten.efaTypes.makeGenderArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERGENDER),
                International.getString("Geschlecht")));
        itemFilterGender = (ItemTypeMultiSelectList<String>)item;
        itemFilterGender.setEnabled(!getFilterGenderAll());
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERGENDERALL, getFilterGenderAll(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERGENDER),
                International.getString("alle")));
        item.registerItemListener(this);
        v.add(item = new ItemTypeMultiSelectList<UUID>(StatisticsRecord.FILTERSTATUS, getFilterStatus(),
                getFilterStatusListValues(), getFilterStatusListDisplay(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERSTATUS),
                International.getString("Status")));
        itemFilterStatus = (ItemTypeMultiSelectList<String>)item;
        itemFilterStatus.setEnabled(!getFilterStatusAll());
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERSTATUSALL, getFilterStatusAll(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERSTATUS),
                International.getString("alle")));
        item.registerItemListener(this);
        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.FILTERSESSIONTYPE, getFilterSessionType(),
                Daten.efaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES), Daten.efaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERSESSIONTYPE),
                International.getString("Fahrtart")));
        itemFilterSessionType = (ItemTypeMultiSelectList<String>)item;
        itemFilterSessionType.setEnabled(!getFilterSessionTypeAll());
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERSESSIONTYPEALL, getFilterSessionTypeAll(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERSESSIONTYPE),
                International.getString("alle")));
        item.registerItemListener(this);
        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.FILTERBOATTYPE, getFilterBoatType(),
                Daten.efaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES), Daten.efaTypes.makeBoatTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERBOATTYPE),
                International.getString("Bootstyp")));
        itemFilterBoatType = (ItemTypeMultiSelectList<String>)item;
        itemFilterBoatType.setEnabled(!getFilterBoatTypeAll());
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERBOATTYPEALL, getFilterBoatTypeAll(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERBOATTYPE),
                International.getString("alle")));
        item.registerItemListener(this);
        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.FILTERBOATSEATS, getFilterBoatSeats(),
                Daten.efaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_VALUES), Daten.efaTypes.makeBoatSeatsArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERBOATSEAT),
                International.getString("Bootsplätze")));
        itemFilterBoatSeats = (ItemTypeMultiSelectList<String>)item;
        itemFilterBoatSeats.setEnabled(!getFilterBoatSeatsAll());
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERBOATSEATSALL, getFilterBoatSeatsAll(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERBOATSEAT),
                International.getString("alle")));
        item.registerItemListener(this);
        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.FILTERBOATRIGGING, getFilterBoatRigging(),
                Daten.efaTypes.makeBoatRiggingArray(EfaTypes.ARRAY_STRINGLIST_VALUES), Daten.efaTypes.makeBoatRiggingArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERBOATRIGG),
                International.getString("Riggerung")));
        itemFilterBoatRigging = (ItemTypeMultiSelectList<String>)item;
        itemFilterBoatRigging.setEnabled(!getFilterBoatRiggingAll());
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERBOATRIGGINGALL, getFilterBoatRiggingAll(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERBOATRIGG),
                International.getString("alle")));
        item.registerItemListener(this);
        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.FILTERBOATCOXING, getFilterBoatCoxing(),
                Daten.efaTypes.makeBoatCoxingArray(EfaTypes.ARRAY_STRINGLIST_VALUES), Daten.efaTypes.makeBoatCoxingArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERBOATCOXING),
                International.getString("Steuerung")));
        itemFilterBoatCoxing = (ItemTypeMultiSelectList<String>)item;
        itemFilterBoatCoxing.setEnabled(!getFilterBoatCoxingAll());
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERBOATCOXINGALL, getFilterBoatCoxingAll(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERBOATCOXING),
                International.getString("alle")));
        item.registerItemListener(this);
        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.FILTERBOATOWNER, getFilterBoatOwner(),
                getFilterBoatOwnerArray(EfaTypes.ARRAY_STRINGLIST_VALUES), getFilterBoatOwnerArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERBOATOWNER),
                International.getString("Eigentümer")));
        itemFilterBoatOwner = (ItemTypeMultiSelectList<String>)item;
        itemFilterBoatOwner.setEnabled(!getFilterBoatOwnerAll());
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERBOATOWNERALL, getFilterBoatOwnerAll(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERBOATOWNER),
                International.getString("alle")));
        item.registerItemListener(this);

        v.add(item = getGuiItemTypeStringAutoComplete(StatisticsRecord.FILTERBYPERSONID, null,
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERINDIVIDUAL),
                getPersistence().getProject().getPersons(false), System.currentTimeMillis(), System.currentTimeMillis(),
                International.getString("nur Person")));
        if (getFilterByPersonId() != null) {
            ((ItemTypeStringAutoComplete)item).setId(getFilterByPersonId());
        } else {
            ((ItemTypeStringAutoComplete)item).parseAndShowValue(getFilterByPersonText());
        }
        ((ItemTypeStringAutoComplete)item).setAlternateFieldNameForPlainText(StatisticsRecord.FILTERBYPERSONTEXT);
        v.add(item = getGuiItemTypeStringAutoComplete(StatisticsRecord.FILTERBYBOATID, null,
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERINDIVIDUAL),
                getPersistence().getProject().getBoats(false), System.currentTimeMillis(), System.currentTimeMillis(),
                International.getString("nur Boot")));
        if (getFilterByBoatId() != null) {
            ((ItemTypeStringAutoComplete)item).setId(getFilterByBoatId());
        } else {
            ((ItemTypeStringAutoComplete)item).parseAndShowValue(getFilterByBoatText());
        }
        ((ItemTypeStringAutoComplete)item).setAlternateFieldNameForPlainText(StatisticsRecord.FILTERBYBOATTEXT);
        v.add(item = getGuiItemTypeStringAutoComplete(StatisticsRecord.FILTERBYGROUPID, getFilterByGroupId(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERINDIVIDUAL),
                getPersistence().getProject().getGroups(false), System.currentTimeMillis(), System.currentTimeMillis(),
                International.getString("nur Gruppe")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERPROMPTPERSON, getFilterPromptPerson(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERINDIVIDUAL),
                International.getMessage("{item} interaktiv abfragen",
                        International.getString("Person"))));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERPROMPTBOAT, getFilterPromptBoat(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERINDIVIDUAL),
                International.getMessage("{item} interaktiv abfragen",
                        International.getString("Boot"))));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERPROMPTGROUP, getFilterPromptGroup(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERINDIVIDUAL),
                International.getMessage("{item} interaktiv abfragen",
                        International.getString("Gruppe"))));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERFROMTOBOATHOUSE, getFilterFromToBoathouse(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERVARIOUS),
                International.getString("Start und Ziel ist Bootshaus")));
        v.add(item = new ItemTypeString(StatisticsRecord.FILTERNAMECONTAINS, getFilterNameContains(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERVARIOUS),
                International.getString("Name enthält")));
        v.add(item = new ItemTypeString(StatisticsRecord.FILTERCOMMENTSINCLUDE, getFilterCommentsInclude(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERVARIOUS),
                International.getString("Bemerkungsfeld enthält")));
        v.add(item = new ItemTypeString(StatisticsRecord.FILTERCOMMENTSEXCLUDE, getFilterCommentsExclude(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERVARIOUS),
                International.getString("Bemerkungsfeld enthält nicht")));
        v.add(item = new ItemTypeDistance(StatisticsRecord.FILTERMINSESSIONDISTANCE, getFilterMinDessionDistance(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERVARIOUS),
                International.getString("Mindestentfernung")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERALSOOPENSESSIONS, getFilterAlsoOpenSessions(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERVARIOUS),
                International.getString("auch offene Fahrten berücksichtigen")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.FILTERONLYOPENDAMAGES, getFilterOnlyOpenDamages(),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERVARIOUS),
                International.getString("nur offene Bootsschäden")));
        v.add(item = new ItemTypeStringList(StatisticsRecord.FILTERONLYLOGBOOK, getFilterOnlyLogbook(),
                getLogbookNames(ARRAY_STRINGLIST_VALUES), getLogbookNames(ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FILTER,CAT_FILTERVARIOUS),
                International.getString("nur Fahrtenbuch")));

        // CAT_FIELDS
        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.AGGREGATIONS, getAggregations(),
                getAggregationList(getStatisticTypeEnum(), ARRAY_STRINGLIST_VALUES),
                getAggregationList(getStatisticTypeEnum(), ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSCALC),
                International.getString("Berechnung")));
        ((ItemTypeMultiSelectList)item).setFieldSize(400, 300);
        itemAggrFields = ((ItemTypeMultiSelectList)item);
        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.SHOWFIELDS, getShowFields(),
                getFieldsList(ARRAY_STRINGLIST_VALUES), getFieldsList(ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSLIST),
                International.getString("Ausgabe")));
        ((ItemTypeMultiSelectList)item).setFieldSize(400, 300);
        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.SHOWLOGBOOKFIELDS, getShowLogbookFields(),
                getLogbookFieldsList(ARRAY_STRINGLIST_VALUES), getLogbookFieldsList(ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSLOGBOOK),
                International.getString("Ausgabe")));
        ((ItemTypeMultiSelectList)item).setFieldSize(400, 300);
        v.add(item = new ItemTypeMultiSelectList<String>(StatisticsRecord.SHOWOTHERFIELDS, getShowOtherFields(),
                getOtherFieldsList(getStatisticTypeEnum(), ARRAY_STRINGLIST_VALUES),
                getOtherFieldsList(getStatisticTypeEnum(), ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSOTHER),
                International.getString("Ausgabe")));
        ((ItemTypeMultiSelectList)item).setFieldSize(400, 300);
        itemShowOtherFields = ((ItemTypeMultiSelectList)item);

        v.add(item = new ItemTypeLabel("GUIITEM_BARSIZE_LABEL",
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSBARS),
                International.getString("Maximale Balkengrößen (in Pixeln)") + ": "));
        item.setFieldGrid(2, GridBagConstraints.CENTER, -1);
        v.add(item = new ItemTypeInteger(StatisticsRecord.AGGRDISTANCEBARSIZE, getAggrDistanceBarSize(), 0, 1000,
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSBARS),
                International.getString("Kilometer")));
        v.add(item = new ItemTypeInteger(StatisticsRecord.AGGRROWDISTANCEBARSIZE, getAggrRowDistanceBarSize(), 0, 1000,
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSBARS),
                getRowingKmString()));
        v.add(item = new ItemTypeInteger(StatisticsRecord.AGGRCOXDISTANCEBARSIZE, getAggrCoxDistanceBarSize(), 0, 1000,
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSBARS),
                getCoxingKmString()));
        v.add(item = new ItemTypeInteger(StatisticsRecord.AGGRSESSIONSBARSIZE, getAggrSessionsBarSize(), 0, 1000,
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSBARS),
                International.getString("Fahrten")));
        v.add(item = new ItemTypeInteger(StatisticsRecord.AGGRAVGDISTBARSIZE, getAggrAvgDistanceBarSize(), 0, 1000,
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSBARS),
                International.getString("Km/Fahrt")));
        v.add(item = new ItemTypeInteger(StatisticsRecord.AGGRDURATIONBARSIZE, getAggrDurationBarSize(), 0, 1000,
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSBARS),
                International.getString("Dauer")));
        v.add(item = new ItemTypeInteger(StatisticsRecord.AGGRSPEEDBARSIZE, getAggrSpeedBarSize(), 0, 1000,
                IItemType.TYPE_PUBLIC, BaseTabbedDialog.makeCategory(CAT_FIELDS,CAT_FIELDSBARS),
                International.getString("Geschwindigkeit")));

        // CAT_SORTING
        v.add(item = new ItemTypeStringList(StatisticsRecord.SORTINGCRITERIA, getSortingCriteria(),
                getSortingCriteria(ARRAY_STRINGLIST_VALUES), getSortingCriteria(ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, CAT_SORTING,
                International.getString("Sortierkriterium")));
        itemTypeSortingCriteria = (ItemTypeStringList)item;
        v.add(item = new ItemTypeStringList(StatisticsRecord.SORTINGORDER, getSortingOrder(),
                getSortingOrders(ARRAY_STRINGLIST_VALUES), getSortingOrders(ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, CAT_SORTING,
                International.getString("Sortierreihenfolge")));
        itemTypeSortingOrder = (ItemTypeStringList)item;

        // CAT_COMP
        v.add(item = new ItemTypeInteger(StatisticsRecord.COMPYEAR, getCompYear(),
                1900, 2100,
                IItemType.TYPE_PUBLIC, CAT_COMP,
                International.getString("Wettbewerbsjahr")));
        item.registerItemListener(this);
        itemCompYear = (ItemTypeInteger)item;

        v.add(item = new ItemTypeInteger(StatisticsRecord.COMPPERCENTFULFILLED, getCompPercentFulfilled(),
                0, 100,
                IItemType.TYPE_PUBLIC, CAT_COMP,
                International.getString("Prozent erfüllt")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.COMPOUTPUTRULES, getCompOutputRules(),
                IItemType.TYPE_PUBLIC, CAT_COMP,
                International.getString("Wettbewerbsbedingungen ausgeben")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.COMPOUTPUTSHORT, getCompOutputShort(),
                IItemType.TYPE_PUBLIC, CAT_COMP,
                International.getString("Ausgabe im Kurzformat")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.COMPOUTPUTWITHOUTDETAILS, getCompOutputWithoutDetails(),
                IItemType.TYPE_PUBLIC, CAT_COMP,
                International.getString("Ausgabe ohne Details")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.COMPOUTPUTADDITIONALWITHREQUIREMENTS, getCompOutputAdditionalWithRequirements(),
                IItemType.TYPE_PUBLIC, CAT_COMP,
                International.getString("Ausgabe zusätzlich mit Anforderungen")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.COMPOUTPUTALLDESTINATIONAREAS, getCompOutputAllDestinationAreas(),
                IItemType.TYPE_PUBLIC, CAT_COMP,
                International.onlyFor("Alle Zielbereiche ausgeben", "de")));

        // CAT_OUTPUT
        v.add(item = EfaGuiUtils.createHint("HINT_"+StatisticsRecord.OUTPUTFILE, IItemType.TYPE_PUBLIC, CAT_OUTPUT, "<html>"+International.getStringWithMnemonic("STATISTICS_RELATIVE_PATHS_HINT")+"</html>", 3, 0, 10));
        this.itemOutputFileHINT = (ItemTypeLabel) item;
        v.add(item = new ItemTypeStringList(StatisticsRecord.OUTPUTTYPE, getOutputType(),
                getOutputTypes(ARRAY_STRINGLIST_VALUES), getOutputTypes(ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, CAT_OUTPUT,
                International.getString("Ausgabeart")));
        item.registerItemListener(this);
        item.setNotNull(true);
        v.add(item = new ItemTypeFile(StatisticsRecord.OUTPUTFILE, getOutputFile(),
                International.getString("Ausgabedatei"),
                International.getString("alle Dateien"),
                null, ItemTypeFile.MODE_SAVE, ItemTypeFile.TYPE_FILE,
                IItemType.TYPE_PUBLIC, CAT_OUTPUT, International.getString("Ausgabedatei")));
        item.setNotNull(true);
        this.itemOutputFile = (ItemTypeFile)item;
        v.add(item = new ItemTypeStringList(StatisticsRecord.OUTPUTENCODING, getOutputEncoding(),
                new String[] { Daten.ENCODING_UTF, Daten.ENCODING_ISO },
                new String[] { Daten.ENCODING_UTF, Daten.ENCODING_ISO },
                IItemType.TYPE_PUBLIC, CAT_OUTPUT,
                International.getStringWithMnemonic("Zeichensatz")
        ));
        item.setNotNull(true);
        this.itemOutputEncoding = (ItemTypeStringList)item;
        v.add(item = new ItemTypeBoolean(StatisticsRecord.OUTPUTHTMLUPDATETABLE, getOutputHtmlUpdateTable(),
                IItemType.TYPE_PUBLIC, CAT_OUTPUT,
                International.getString("in existierenden HTML-Dateien nur Tabelle ersetzen")));
        this.itemOutputHtmlUpdateTable = (ItemTypeBoolean)item;
        v.add(item = new ItemTypeString(StatisticsRecord.OUTPUTCSVSEPARATOR, getOutputCsvSeparator(),
                IItemType.TYPE_PUBLIC, CAT_OUTPUT,
                International.getString("Feldtrenner") + " (CSV)"));
        item.setNotNull(true);
        this.itemOutputCsvSeparator = (ItemTypeString)item;
        v.add(item = new ItemTypeString(StatisticsRecord.OUTPUTCSVQUOTES, getOutputCsvQuotes(),
                IItemType.TYPE_PUBLIC, CAT_OUTPUT,
                International.getString("Texttrenner") + " (CSV)"));
        this.itemOutputCsvQuotes = (ItemTypeString)item;
        v.add(item = new ItemTypeButton(GUIITEM_OUTPUTFTP,
                IItemType.TYPE_PUBLIC, CAT_OUTPUT,
                International.getString("FTP-Upload") + " ..."));
        ((ItemTypeButton)item).setFieldGrid(2, GridBagConstraints.EAST, GridBagConstraints.NONE);
        ((ItemTypeButton)item).registerItemListener(this);
        this.itemOutputFtpButton = (ItemTypeButton)item;
        v.add(item = new ItemTypeButton(GUIITEM_OUTPUTEMAIL,
                IItemType.TYPE_PUBLIC, CAT_OUTPUT,
                International.getString("email-Versand") + " ..."));
        ((ItemTypeButton)item).setFieldGrid(2, GridBagConstraints.EAST, GridBagConstraints.NONE);
        ((ItemTypeButton)item).registerItemListener(this);
        this.itemOutputEmailButton = (ItemTypeButton)item;

        // CAT_OPTIONS
        v.add(item = new ItemTypeBoolean(StatisticsRecord.OPTIONDISTANCEWITHUNIT, getOptionDistanceWithUnit(),
                IItemType.TYPE_PUBLIC, CAT_OPTIONS,
                International.getString("Entfernungen mit Längeneinheit ausgeben")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.OPTIONTRUNCATEDIST, getOptionTruncateDistance(),
                IItemType.TYPE_PUBLIC, CAT_OPTIONS,
                International.getString("Nachkommastellen bei Ausgabe von Entfernungen abschneiden")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.OPTIONLISTALLNULLENTRIES, getOptionListAllNullEntries(),
                IItemType.TYPE_PUBLIC, CAT_OPTIONS,
                International.getString("Alle Einträge ausgeben")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.OPTIONIGNORENULLVALUES, getOptionIgnoreNullValues(),
                IItemType.TYPE_PUBLIC, CAT_OPTIONS,
                International.getString("Nullwerte nicht ausgeben")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.OPTIONSUMGUESTSANDOTHERS, getOptionSumGuestsAndOthers(),
                IItemType.TYPE_PUBLIC, CAT_OPTIONS,
                International.getString("Gäste und andere zusammenfassen")));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.OPTIONSUMGUESTSBYCLUB, getOptionSumGuestsByClub(),
                IItemType.TYPE_PUBLIC, CAT_OPTIONS,
                International.getString("Gäste/Fremdboote vereinsweise zusammenfassen")));
        v.add(item = new ItemTypeStringList(StatisticsRecord.OPTIONSHOWVALIDLASTTRIP, 
                getOptionShowValidLastTrip() ? SHOWDATAVALID_LASTTRIPTIME: SHOWDATAVALID_STATENDTIME,
                new String[] { SHOWDATAVALID_STATENDTIME, SHOWDATAVALID_LASTTRIPTIME },
                new String[] { International.getString("Ende des Auswertungszeitraums"), 
                               International.getString("Letzte jeweilige ausgewertete Fahrt") },
                IItemType.TYPE_PUBLIC, CAT_OPTIONS,
                International.getStringWithMnemonic("Gültigkeitszeitpunkt für Anzeige von Daten")
        ));
        v.add(item = new ItemTypeBoolean(StatisticsRecord.OPTIONONLYMEMBERSWITHINSUFFICIENTCLUBWORK, getOptionOnlyMembersWithInsufficientClubwork(),
                IItemType.TYPE_PUBLIC, CAT_OPTIONS,
                International.getString("Für Vereinsarbeit nur Mitglieder ausgeben, die Sollstunden noch nicht erfüllt haben")));

        setVisibleItems(getOutputTypeEnum());
        return v;
    }

    public TableItemHeader[] getGuiTableHeader() {
        TableItemHeader[] header = new TableItemHeader[(TABLE_HEADER_LONG ? 6 : 2)];
        header[0] = new TableItemHeader(International.getString("Nr."));
        header[1] = new TableItemHeader(International.getString("Name"));
        if (TABLE_HEADER_LONG) {
            header[2] = new TableItemHeader(International.getString("Statistiktyp"));
            header[3] = new TableItemHeader(International.getString("Statistikart"));
            header[4] = new TableItemHeader(International.getString("Von"));
            header[5] = new TableItemHeader(International.getString("Bis"));
        }
        return header;
    }

    public TableItem[] getGuiTableItems() {
        TableItem[] items = new TableItem[(TABLE_HEADER_LONG ? 6 : 2)];
        items[0] = new TableItem(getPosition());
        items[1] = new TableItem(getName());
        if (TABLE_HEADER_LONG) {
            items[2] = new TableItem(getStatisticCategoryDescription());
            items[3] = new TableItem(getStatisticTypeDescription());
            items[4] = new TableItem(getDateFrom());
            items[5] = new TableItem(getDateTo());
            if (getPubliclyAvailable()) {
                items[0].setMarked(true);
                items[1].setMarked(true);
                items[2].setMarked(true);
                items[3].setMarked(true);
                items[4].setMarked(true);
                items[5].setMarked(true);
            }
        }
        return items;
    }

    public void resetStatisticValues() {
        cNumberOfEntries = 0;
        cEntryNoFirst = null;
        cEntryNoLast = null;
        cEntryDateFirst = null;
        cEntryDateLast = null;
        cCompetition = null;

        pParentDialog = null;
        pStatTitle = null;
        pStatCreationDate = null;
        pStatCreatedByUrl = null;
        pStatCreatedByName = null;
        pStatDescription = null;
        pStatDateRange = null;
        pStatConsideredEntries = null;
        pStatFilter = null;
        pStatIgnored = new Hashtable<String, String>();
        pTableColumns = null;
        pCompRules = null;
        pCompRulesBold = new Hashtable();
        pCompRulesItalics = new Hashtable();
        pCompWarning = null;
        pCompGroupNames = null;
        pCompParticipants = null;
        pCompAbortEfaWett = false;

        pAdditionalTable1 = null;
        pAdditionalTable1Title = null;
        pAdditionalTable1FirstRowBold = false;
        pAdditionalTable1LastRowBold = false;
        pAdditionalTable2 = null;
        pAdditionalTable2Title = null;
        pAdditionalTable2FirstRowBold = false;
        pAdditionalTable2LastRowBold = false;
        pOutputLinesAbove = null;
        pOutputLinesBelow = null;
    }

    public boolean prepareStatisticSettings(AdminRecord admin) {
        cWarnings = new Hashtable<String, String>();

        sAdmin = admin;

        sIsFieldsPosition = false;
        sIsFieldsName = false;
        sIsFieldsGender = false;
        sIsFieldsStatus = false;
        sIsFieldsClub = false;
        sIsFieldsYearOfBirth = false;
        sIsFieldsMemberNo = false;
        sIsFieldsBoatType = false;
        sIsOFieldsBaseStatus = false;
        sIsOFieldsCurrentStatus = false;
        sIsOFieldsComments = false;
        sIsOFieldsReservedFrom = false;
        sIsOFieldsReservedTo = false;
        sIsOFieldsReservedFor = false;
        sIsOFieldsReason = false;
        sIsOFieldsContact = false;
        sIsOFieldsDamage = false;
        sIsOFieldsDamageSeverity = false;
        sIsOFieldsReportedOn = false;
        sIsOFieldsFixedOn = false;
        sIsLFieldsEntryNo = false;
        sIsLFieldsDate = false;
        sIsLFieldsEndDate = false;
        sIsLFieldsBoat = false;
        sIsLFieldsCox = false;
        sIsLFieldsCrew = false;
        sIsLFieldsStartTime = false;
        sIsLFieldsEndTime = false;
        sIsLFieldsWaters = false;
        sIsLFieldsDestination = false;
        sIsLFieldsDestinationDetails = false;
        sIsLFieldsDestinationAreas = false;
        sIsLFieldsDistance = false;
        sIsLFieldsMultiDay = false;
        sIsLFieldsSessionType = false;
        sIsLFieldsNotes = false;
        sIsAggrDistance = false;
        sIsAggrRowDistance = false;
        sIsAggrCoxDistance = false;
        sIsAggrSessions = false;
        sIsAggrAvgDistance = false;
        sIsAggrDuration = false;
        sIsAggrDays = false;
        sIsAggrSpeed = false;
        sIsAggrZielfahrten = false;
        sIsAggrWanderfahrten = false;
        sIsAggrWinterfahrten = false;
        sIsAggrGigfahrten = false;
        sIsAggrDamageCount = false;
        sIsAggrDamageDuration = false;
        sIsAggrDamageAvgDuration = false;
        sIsAggrClubwork = false;
        sIsAggrClubworkTarget = false;
        sIsAggrClubworkRelativeToTarget = false;
        sIsAggrClubworkOverUnderCarryOver = false;
        sIsAggrClubworkCredit = false;

        // we should consider publicly available ("blue") statistics even then
        // as publicly available when calculated through an Admin (this might be
        // the case also for efaCLI-based statistics to be uploaded to the homepage)
        //if (admin != null) {
        //    sPublicStatistic = false;
        //} else {
            sPublicStatistic = getPubliclyAvailable();
        //}

        sFilterOnlyLogbook = getFilterOnlyLogbook();
        if (sFilterOnlyLogbook != null && sFilterOnlyLogbook.trim().length() == 0) {
            sFilterOnlyLogbook = null;
        }

        sStartDate = getDateFrom();
        if (sStartDate == null || !sStartDate.isSet()) {
            if (sFilterOnlyLogbook != null) {
                Logbook l = Daten.project.getLogbook(sFilterOnlyLogbook, false);
                if (l != null) {
                    sStartDate = l.getStartDate();
                }
            } else {
                sStartDate = DataTypeDate.today();
                sStartDate.setDay(1);
                sStartDate.setMonth(1);
            }
        }

        sEndDate = getDateTo();
        if (sEndDate == null || !sEndDate.isSet()) {
            if (sFilterOnlyLogbook != null) {
                Logbook l = Daten.project.getLogbook(sFilterOnlyLogbook, false);
                if (l != null) {
                    sEndDate = l.getEndDate();
                }
            } else {
                sEndDate = DataTypeDate.today();
            }
        }

        sTimestampBegin = sStartDate.getTimestamp(new DataTypeTime(0,0,0));
        sTimestampEnd   = sEndDate.getTimestamp(new DataTypeTime(23,59,59));
        sValidAt = sEndDate.getTimestamp(new DataTypeTime(23,59,59));

        sStatisticCategory = getStatisticCategoryEnum();
        sStatisticType = getStatisticType();
        sStatisticTypeEnum = getStatisticTypeEnum();
        sStatistikKey = getStatisticKeyEnum();

        sFilterGender = new Hashtable<String,String>();
        DataTypeList<String> listString = getFilterGender();
        for (int i=0; listString != null && i<listString.length(); i++) {
            sFilterGender.put(listString.get(i), "foo");
        }
        sFilterGenderAll = isFilterGenderAllSelected();

        sFilterStatus = new Hashtable<UUID,String>();
        DataTypeList<UUID> listUUID = getFilterStatus();
        for (int i=0; listUUID != null && i<listUUID.length(); i++) {
            if (sStatisticCategory == StatisticCategory.competition) {
                try {
                    StatusRecord status = Daten.project.getStatus(false).getStatus(listUUID.get(i));
                    if (status == null || !status.isMember()) {
                        listUUID.remove(i--);
                        setFilterStatus(listUUID);
                        continue; // for Competitions, only consider members
                    }
                } catch(Exception eignore) {
                    continue;
                }
            }
            sFilterStatus.put(listUUID.get(i), "foo");
        }
        try {
            sFilterStatusOther = sFilterStatus.containsKey(getPersistence().getProject().getStatus(false).getStatusOther().getId());
        } catch(Exception eignore) {
            sFilterStatusOther = false;
        }
        sFilterStatusAll = isFilterStatusAllSelected();
        if (sStatisticCategory == StatisticCategory.competition && sFilterStatusAll) {
            sFilterStatusAll = false;
            setFilterStatusAll(false);
        }
        
        sFilterSessionType = new Hashtable<String,String>();
        listString = getFilterSessionType();
        for (int i=0; listString != null && i<listString.length(); i++) {
            sFilterSessionType.put(listString.get(i), "foo");
        }
        sFilterSessionTypeAll = isFilterSessionTypeAllSelected();

        sFilterBoatType = new Hashtable<String,String>();
        listString = getFilterBoatType();
        for (int i=0; listString != null && i<listString.length(); i++) {
            sFilterBoatType.put(listString.get(i), "foo");
        }
        sFilterBoatTypeAll = isFilterBoatTypeAllSelected();

        sFilterBoatSeats = new Hashtable<String,String>();
        listString = getFilterBoatSeats();
        for (int i=0; listString != null && i<listString.length(); i++) {
            sFilterBoatSeats.put(listString.get(i), "foo");
        }
        sFilterBoatSeatsAll = isFilterBoatSeatsAllSelected();

        sFilterBoatRigging = new Hashtable<String,String>();
        listString = getFilterBoatRigging();
        for (int i=0; listString != null && i<listString.length(); i++) {
            sFilterBoatRigging.put(listString.get(i), "foo");
        }
        sFilterBoatRiggingAll = isFilterBoatRiggingAllSelected();

        sFilterBoatCoxing = new Hashtable<String,String>();
        listString = getFilterBoatCoxing();
        for (int i=0; listString != null && i<listString.length(); i++) {
            sFilterBoatCoxing.put(listString.get(i), "foo");
        }
        sFilterBoatCoxingAll = isFilterBoatCoxingAllSelected();

        sFilterBoatOwner = new Hashtable<String,String>();
        listString = getFilterBoatOwner();
        for (int i=0; listString != null && i<listString.length(); i++) {
            sFilterBoatOwner.put(listString.get(i), "foo");
        }
        sFilterBoatOwnerAll = isFilterBoatOwnerAllSelected();

        sFilterByPersonId = getFilterByPersonId();
        sFilterByPersonText = (sFilterByPersonId != null ? null : getFilterByPersonText());
        sFilterByBoatId = getFilterByBoatId();
        sFilterByBoatText = (sFilterByBoatId != null ? null : getFilterByBoatText());
        sFilterByGroupId = getFilterByGroupId();
        sFilterFromToBoathouse = getFilterFromToBoathouse();
        sFilterNameContains = getFilterNameContains();
        if (sFilterNameContains != null) {
            sFilterNameContains = sFilterNameContains.trim().toLowerCase();
            if (sFilterNameContains.length() == 0) {
                sFilterNameContains = null;
            }
        }
        sFilterCommentsInclude = getFilterCommentsInclude();
        if (sFilterCommentsInclude != null) {
            sFilterCommentsInclude = sFilterCommentsInclude.trim();
            if (sFilterCommentsInclude.length() == 0) {
                sFilterCommentsInclude = null;
            }
        }
        sFilterCommentsExclude = getFilterCommentsExclude();
        if (sFilterCommentsExclude != null) {
            sFilterCommentsExclude = sFilterCommentsExclude.trim();
            if (sFilterCommentsExclude.length() == 0) {
                sFilterCommentsExclude = null;
            }
        }
        sFilterMinSessionDistance = getFilterMinDessionDistance();
        if (sFilterMinSessionDistance != null && !sFilterMinSessionDistance.isSet()) {
            sFilterMinSessionDistance = null;
        }
        sFilterOnlyOpenDamages = getFilterOnlyOpenDamages();
        sFilterAlsoOpenSessions = getFilterAlsoOpenSessions();

        if (sStatisticCategory == StatisticCategory.competition) {
            boolean filtersChanged = false;
            if (!sFilterSessionTypeAll && setCompetitionFilterAllPredefinedSessionTypes()) {
                filtersChanged = true;
            }
            if (!sFilterBoatTypeAll && setCompetitionFilterAllPredefinedBoatTypes()) {
                filtersChanged = true;
            }
            if (!sFilterBoatSeatsAll) {
                sFilterBoatSeatsAll = true;
                filtersChanged = true;
            }
            if (!sFilterBoatRiggingAll) {
                sFilterBoatRiggingAll = true;
                filtersChanged = true;
            }
            if (!sFilterBoatCoxingAll) {
                sFilterBoatCoxingAll = true;
                filtersChanged = true;
            }
            if (!sFilterBoatOwnerAll) {
                sFilterBoatOwnerAll = true;
                filtersChanged = true;
            }
            if (sFilterFromToBoathouse) {
                sFilterFromToBoathouse = false;
                filtersChanged = true;
            }
            if (sFilterNameContains != null) {
                sFilterNameContains = null;
                filtersChanged = true;
            }
            if (sFilterCommentsInclude != null) {
                sFilterCommentsInclude = null;
                filtersChanged = true;
            }
            if (sFilterCommentsExclude != null) {
                sFilterCommentsExclude = null;
                filtersChanged = true;
            }
            if (sFilterMinSessionDistance != null) {
                sFilterMinSessionDistance = null;
                filtersChanged = true;
            }
            if (sFilterAlsoOpenSessions) {
                sFilterAlsoOpenSessions = false;
                filtersChanged = true;
            }
            if (filtersChanged) {
                cWarnings.put(International.getString("Einige Filtereinstellungen wurden für die Auswertung ignoriert."), "foobar");
            }
        }

        if (getFilterPromptPerson() && Daten.isGuiAppl()) {
            Object o = promptForInput(sFilterByPersonId, sFilterByPersonText,
                    getPersistence().getProject().getPersons(false),
                    International.getString("Person"));
            if (o != null) {
                if (o instanceof UUID) {
                    sFilterByPersonId = (UUID)o;
                    sFilterByPersonText = null;
                    if (sPublicStatistic) {
                        PersonRecord p = getPersistence().getProject().getPersons(false).getPerson(sFilterByPersonId, sValidAt);
                        if (p != null && p.getExcludeFromPublicStatistics()) {
                            Dialog.error(International.getMessage("Statistik für {name} nicht erlaubt.", p.getQualifiedName()));
                            return false;
                        }
                        if (p != null && p.getPassword() != null && p.getPassword().length() > 0) {
                            String pwd = null;
                            do {
                                pwd = EnterPasswordDialog.enterPassword(pParentDialog,
                                        International.getMessage("Paßwort für {name}", p.getQualifiedName()) + "?",
                                        false);
                                if (pwd != null && !pwd.equals(p.getPassword())) {
                                    Dialog.error(International.getString("Paßwort ist falsch."));
                                }
                            } while(pwd != null && !pwd.equals(p.getPassword()));
                            if (pwd == null) {
                                return false;
                            }
                        }
                    }
                } else {
                    sFilterByPersonText = (String)o;
                    sFilterByPersonId = null;
                }
            } else {
                return false;
            }
        }
        if (getFilterPromptBoat() && Daten.isGuiAppl()) {
            Object o = promptForInput(sFilterByBoatId, sFilterByBoatText,
                    getPersistence().getProject().getBoats(false),
                    International.getString("Boot"));
            if (o != null) {
                if (o instanceof UUID) {
                    sFilterByBoatId = (UUID)o;
                    sFilterByBoatText = null;
                    if (sPublicStatistic) {
                        BoatRecord b = getPersistence().getProject().getBoats(false).getBoat(sFilterByBoatId, sValidAt);
                        if (b != null && b.getExcludeFromPublicStatistics()) {
                            Dialog.error(International.getMessage("Statistik für {name} nicht erlaubt.", b.getQualifiedName()));
                            return false;
                        }
                    }
                } else {
                    sFilterByBoatText = (String)o;
                    sFilterByBoatId = null;
                }
            }
        }
        if (getFilterPromptGroup() && Daten.isGuiAppl()) {
            Object o = promptForInput(sFilterByGroupId, null,
                    getPersistence().getProject().getGroups(false),
                    International.getString("Gruppe"));
            if (o != null) {
                if (o instanceof UUID) {
                    sFilterByGroupId = (UUID)o;
                } else {
                    sFilterByGroupId = null;
                }
            }
        }

        DataTypeList<String> fields = (sStatisticCategory != StatisticCategory.other ?
                getShowFields() : getShowOtherFields());
        for (int i=0; fields != null && i<fields.length(); i++) {
            String s = fields.get(i);
            if (s.equals(FIELDS_POSITION)) {
                sIsFieldsPosition = true;
            } else if (s.equals(FIELDS_NAME)) {
                sIsFieldsName = true;
            } else if (s.equals(FIELDS_GENDER)) {
                sIsFieldsGender = true;
            } else if (s.equals(FIELDS_STATUS)) {
                sIsFieldsStatus = true;
            } else if (s.equals(FIELDS_CLUB)) {
                sIsFieldsClub = true;
            } else if (s.equals(FIELDS_YEAROFBIRTH)) {
                sIsFieldsYearOfBirth = true;
            } else if (s.equals(FIELDS_MEMBERNO)) {
                sIsFieldsMemberNo = true;
            } else if (s.equals(FIELDS_BOATTYPE)) {
                sIsFieldsBoatType = true;
            } else if (s.equals(OFIELDS_BASESTATUS)) {
                sIsOFieldsBaseStatus = true;
            } else if (s.equals(OFIELDS_CURRENTSTATUS)) {
                sIsOFieldsCurrentStatus = true;
            } else if (s.equals(OFIELDS_COMMENTS)) {
                sIsOFieldsComments = true;
            } else if (s.equals(OFIELDS_RESERVEDFROM)) {
                sIsOFieldsReservedFrom = true;
            } else if (s.equals(OFIELDS_RESERVEDTO)) {
                sIsOFieldsReservedTo = true;
            } else if (s.equals(OFIELDS_RESERVEDFOR)) {
                sIsOFieldsReservedFor = true;
            } else if (s.equals(OFIELDS_REASON)) {
                sIsOFieldsReason = true;
            } else if (s.equals(OFIELDS_CONTACT)) {
                sIsOFieldsContact = true;
            } else if (s.equals(OFIELDS_DAMAGE)) {
                sIsOFieldsDamage = true;
            } else if (s.equals(OFIELDS_DAMAGESEVERITY)) {
                sIsOFieldsDamageSeverity = true;
            } else if (s.equals(OFIELDS_REPORTEDON)) {
                sIsOFieldsReportedOn = true;
            } else if (s.equals(OFIELDS_FIXEDON)) {
                sIsOFieldsFixedOn = true;
            }
        }
        if ((sStatisticCategory == StatisticCategory.list || sStatisticCategory == StatisticCategory.other || sStatisticType.equals(STYPE_CLUBWORK)) &&
                (fields == null || fields.length() == 0)) {
            // at least show these fields, if for (whatever reason) no fields were selected
            sIsFieldsPosition = true;
            sIsFieldsName = true;
        }
        if (sStatisticCategory == StatisticCategory.other) {
            sIsFieldsName = true;
        }

        DataTypeList<String> lfields = getShowLogbookFields();
        for (int i=0; lfields != null && i<lfields.length(); i++) {
            String s = lfields.get(i);
            if (s.equals(LFIELDS_ENTRYNO)) {
                sIsLFieldsEntryNo = true;
            } else if (s.equals(LFIELDS_DATE)) {
                sIsLFieldsDate = true;
            } else if (s.equals(LFIELDS_ENDDATE)) {
                sIsLFieldsEndDate = true;
            } else if (s.equals(LFIELDS_BOAT)) {
                sIsLFieldsBoat = true;
            } else if (s.equals(LFIELDS_COX)) {
                sIsLFieldsCox = true;
            } else if (s.equals(LFIELDS_CREW)) {
                sIsLFieldsCrew = true;
            } else if (s.equals(LFIELDS_STARTTIME)) {
                sIsLFieldsStartTime = true;
            } else if (s.equals(LFIELDS_ENDTIME)) {
                sIsLFieldsEndTime = true;
            } else if (s.equals(LFIELDS_WATERS)) {
                sIsLFieldsWaters = true;
            } else if (s.equals(LFIELDS_DESTINATION)) {
                sIsLFieldsDestination = true;
            } else if (s.equals(LFIELDS_DESTDETAILS)) {
                sIsLFieldsDestinationDetails = true;
            } else if (s.equals(LFIELDS_DESTAREAS)) {
                sIsLFieldsDestinationAreas = true;
            } else if (s.equals(LFIELDS_DISTANCE)) {
                sIsLFieldsDistance = true;
                sLFieldDistancePos = i;
            } else if (s.equals(LFIELDS_MULTIDAY)) {
                sIsLFieldsMultiDay = true;
            } else if (s.equals(LFIELDS_SESSIONTYPE)) {
                sIsLFieldsSessionType = true;
            } else if (s.equals(LFIELDS_NOTES)) {
                sIsLFieldsNotes = true;
            }
        }
        if (sStatisticCategory == StatisticCategory.logbook &&
                (lfields == null || lfields.length() == 0)) {
            // at least show these fields, if for (whatever reason) no fields were selected
            sIsLFieldsEntryNo = true;
            sIsLFieldsDate = true;
        }

        DataTypeList<String> aggr = getAggregations();
        for (int i = 0; aggr != null && i < aggr.length(); i++) {
            String s = aggr.get(i);
            if (s.equals(AGGR_DISTANCE) && (sStatisticCategory != StatisticCategory.other || sStatisticTypeEnum == StatisticType.clubwork)) {
                sIsAggrDistance = true;
            } else if (s.equals(AGGR_ROWDISTANCE) && sStatisticCategory != StatisticCategory.other) {
                sIsAggrRowDistance = true;
            } else if (s.equals(AGGR_COXDISTANCE) && sStatisticCategory != StatisticCategory.other) {
                sIsAggrCoxDistance = true;
            } else if (s.equals(AGGR_SESSIONS) && sStatisticCategory != StatisticCategory.other) {
                sIsAggrSessions = true;
            } else if (s.equals(AGGR_AVGDISTANCE) && sStatisticCategory != StatisticCategory.other) {
                sIsAggrAvgDistance = true;
            } else if (s.equals(AGGR_DURATION) && sStatisticCategory != StatisticCategory.other) {
                sIsAggrDuration = true;
            } else if (s.equals(AGGR_SPEED) && sStatisticCategory != StatisticCategory.other) {
                sIsAggrSpeed = true;
            } else if (s.equals(AGGR_ZIELFAHRTEN) && sStatisticCategory != StatisticCategory.other) {
                sIsAggrZielfahrten = true;
            } else if (s.equals(AGGR_WANDERFAHRTEN) && sStatisticCategory != StatisticCategory.other) {
                sIsAggrWanderfahrten = true;
            } else if (s.equals(AGGR_DAMAGECOUNT) && sStatisticTypeEnum == StatisticType.boatdamagestat) {
                sIsAggrDamageCount = true;
            } else if (s.equals(AGGR_DAMAGEDURATION) && sStatisticTypeEnum == StatisticType.boatdamagestat) {
                sIsAggrDamageDuration = true;
            } else if (s.equals(AGGR_DAMAGEAVGDUR) && sStatisticTypeEnum == StatisticType.boatdamagestat) {
                sIsAggrDamageAvgDuration = true;
            } else if (s.equals(AGGR_CLUBWORK) && sStatisticTypeEnum == StatisticType.clubwork) {
                sIsAggrClubwork = true;
            } else if (s.equals(AGGR_CLUBWORKTARGET) && sStatisticTypeEnum == StatisticType.clubwork) {
                sIsAggrClubworkTarget = true;
            } else if (s.equals(AGGR_CBRELTOTARGET) && sStatisticTypeEnum == StatisticType.clubwork) {
                sIsAggrClubworkRelativeToTarget = true;
            } else if (s.equals(AGGR_CBOVERUNDERCARRYOVER) && sStatisticTypeEnum == StatisticType.clubwork) {
                sIsAggrClubworkOverUnderCarryOver = true;
            } else if (s.equals(AGGR_CLUBWORKCREDIT) && sStatisticTypeEnum == StatisticType.clubwork) {
                sIsAggrClubworkCredit = true;
            }
        }

        if (sStatisticCategory != StatisticCategory.other) {
            sAggrDistanceBarSize = getAggrDistanceBarSize();
            sAggrRowDistanceBarSize = getAggrRowDistanceBarSize();
            sAggrCoxDistanceBarSize = getAggrCoxDistanceBarSize();
            sAggrSessionsBarSize = getAggrSessionsBarSize();
            sAggrAvgDistanceBarSize = getAggrAvgDistanceBarSize();
            sAggrDurationBarSize = getAggrDurationBarSize();
            sAggrSpeedBarSize = getAggrSpeedBarSize();
        }

        sSortingCriteria = getSortingCriteriaEnum();
        sSortingOrderAscending = getSortingOrderAscending();

        sCompYear = getCompYear();
        sCompPercentFulfilled = getCompPercentFulfilled();
        sIsOutputCompShort = getCompOutputShort();
        sIsOutputCompRules = getCompOutputRules();
        sIsOutputCompAdditionalWithRequirements = getCompOutputAdditionalWithRequirements();
        sIsOutputCompWithoutDetails = getCompOutputWithoutDetails();
        sIsOutputCompAllDestinationAreas = this.getCompOutputAllDestinationAreas();
        if (sStatisticCategory == StatisticCategory.competition) {
            this.sIsAggrDistance = true;
            this.sIsAggrSessions = true;
            this.sIsFieldsName = true;
            this.sIsFieldsGender = true;
            this.sIsFieldsStatus = true;
            this.sIsFieldsYearOfBirth = true;
            if (sStatisticType.equals(WettDefs.STR_LRVBERLIN_SOMMER)) {
                this.sIsAggrZielfahrten = true;
            }
            if (sStatisticType.equals(WettDefs.STR_DRV_FAHRTENABZEICHEN)) {
                this.sIsAggrWanderfahrten = true;
            }
            if (sStatisticType.equals(WettDefs.STR_LRVBERLIN_WINTER)) {
                this.sIsAggrWinterfahrten = true;
            }
            if (sStatisticType.equals(WettDefs.STR_LRVBRB_FAHRTENWETT) ||
                    sStatisticType.equals(WettDefs.STR_LRVBRB_WANDERRUDERWETT) ||
                    sStatisticType.equals(WettDefs.STR_LRVMVP_WANDERRUDERWETT)) {
                this.sIsAggrGigfahrten = true;
            }
        }

        sOutputType = getOutputTypeEnum();
        if (sPublicStatistic && sOutputType == OutputTypes.efawett) {
            sOutputType = OutputTypes.internal;
        }
        if (sOutputType == OutputTypes.efawett) {
            if (sStatisticCategory == StatisticCategory.competition) {
                sCompPercentFulfilled = 100;
                sIsOutputCompShort = false;
                sIsOutputCompRules = false;
                sIsOutputCompAdditionalWithRequirements = false;
                sIsOutputCompWithoutDetails = false;
                sIsOutputCompAllDestinationAreas = false;
            } else {
                sOutputType = OutputTypes.internal;
            }
        }
        if (sOutputType == OutputTypes.internal) {
            sOutputFile = Daten.efaTmpDirectory + "output.html";
        } else {
            sOutputFile = getOutputFile();
        }
        try {
            if (FTPClient.isFTP(sOutputFile)) {
                sOutputFtpClient = new FTPClient(sOutputFile, Daten.efaTmpDirectory + "output.ftp");
                sOutputFile = Daten.efaTmpDirectory + "output.ftp";
            }
        } catch (NoClassDefFoundError e) {
            Dialog.error(International.getString("Fehlendes Plugin") + ": " + Plugins.PLUGIN_FTP);
            return false;
        }
        if (Email.getEmailAddressFromMailtoString(sOutputFile.toLowerCase()) != null) {
            sEmailAddresses = Email.getEmailAddressFromMailtoString(sOutputFile.toLowerCase());
            sOutputFile = Daten.efaTmpDirectory + "output_" + System.currentTimeMillis() + getOutputExtension();
        }
        
        // if the filename is present, check for symbolic links at the beginning of the path
        // ~/output.txt    ->/home/username/output.txt 
        // ./output.txt    -> efa_data_directory/output.txt
        // just like in efaCLI data import/export function (MenuData.java)
        
        sOutputFile = EfaUtil.extendFilenameWithRelativePath(sOutputFile);
        sOutputFile = EfaUtil.correctFilePath(sOutputFile);
        
        sOutputDir = (new File(sOutputFile)).getParent();
        if (sOutputDir == null || sOutputDir.length() == 0) { // shouldn't happen, just in case...
            sOutputDir = Daten.efaTmpDirectory;
        }
        sOutputEncoding = getOutputEncoding();
        sOutputHtmlUpdateTable = getOutputHtmlUpdateTable() && sOutputType == OutputTypes.html;
        sOutputCsvSeparator = getOutputCsvSeparator();
        sOutputCsvQuotes = getOutputCsvQuotes();

        sDistanceWithUnit = getOptionDistanceWithUnit();
        sTruncateDistanceToFullValue = getOptionTruncateDistance();
        sListAllNullEntries = getOptionListAllNullEntries();
        sIgnoreNullValues = getOptionIgnoreNullValues();
        sSumGuestsAndOthers = getOptionSumGuestsAndOthers();
        sSumGuestsByClub = getOptionSumGuestsByClub();
        sShowValidLastTrip = getOptionShowValidLastTrip();
        sOnlyMembersWithInsufficientClubwork = getOptionOnlyMembersWithInsufficientClubwork();

        resetStatisticValues();
        return true;
    }

    public String getOutputExtension() {
        switch(sOutputType) {
            case internal:
            case html:
                return ".html";
            case internaltxt:
            case csv:
                return ".csv";
            case xml:
                return ".xml";
            case pdf:
                return ".pdf";
            case efawett:
                return ".efw";
            case UNKNOWN:
            	return ".out";
        }
        return ".out";
    }

    private Object promptForInput(UUID id, String text, StorageObject so, String description) {
        ItemTypeStringAutoComplete item = (ItemTypeStringAutoComplete)
                getGuiItemTypeStringAutoComplete("FIELD", null,
                        IItemType.TYPE_PUBLIC, "",
                        so, System.currentTimeMillis(), System.currentTimeMillis(),
                        International.getMessage("Statistik für {item}",
                                description));
        if (id != null) {
            item.setId(id);
        } else {
            item.parseAndShowValue(text);
        }
        if (SimpleInputDialog.showInputDialog(pParentDialog, description, item)) {
            item.getValueFromGui();
            String resS = item.toString();
            UUID resID = (UUID)item.getId(resS);
            if (resID != null) {
                return resID;
            } else {
                return resS;
            }
        }
        return null;
    }

    private static String stripKey(String s) {
        if (s.startsWith(StatisticsData.SORTTOEND_PREFIX)) {
            s = s.substring(StatisticsData.SORTTOEND_PREFIX.length());
        }
        if (s.startsWith(StatisticsData.SORTING_PREFIX)) {
            int pos = s.indexOf(StatisticsData.SORTING_POSTFIX);
            if (pos > 0) {
                s = s.substring(pos + StatisticsData.SORTING_POSTFIX.length());
            }
        }
        return s;
    }

    public void prepareTableColumns(Hashtable<Object, StatisticsData> data) {
        if (pTableColumns != null) {
            // already set explicitly, e.g. for "others" statistics
            return;
        }
        pTableColumns = new Vector<String>();
        if (sStatisticCategory == StatisticCategory.list ||
                sStatisticCategory == StatisticCategory.matrix ||
                sStatisticCategory == StatisticCategory.other) {
            if (sIsFieldsPosition) {
                //pTableColumns.add(International.getString("Platz"));
                // The following line is useless, but added with intention to keep the translation for "Platz"
                International.getString("Platz");
                pTableColumns.add(International.getString("Position"));
            }
            if (sIsFieldsName) {
                String s = getStatisticKeyDescription();
                pTableColumns.add( (s != null ? s : International.getString("Name")) );
            }
            if (sIsFieldsGender) {
                pTableColumns.add(International.getString("Geschlecht"));
            }
            if (sIsFieldsStatus) {
                pTableColumns.add(International.getString("Status"));
            }
            if (sIsFieldsClub) {
                pTableColumns.add(International.getString("Verein"));
            }
            if (sIsFieldsYearOfBirth) {
                pTableColumns.add(International.getString("Jahrgang"));
            }
            if (sIsFieldsMemberNo) {
                pTableColumns.add(International.getString("Mitgliedsnr."));
            }
            if (sIsFieldsBoatType) {
                pTableColumns.add(International.getString("Bootstyp"));
            }
            if (sIsOFieldsBaseStatus) {
                pTableColumns.add(International.getString("Basis-Status"));
            }
            if (sIsOFieldsCurrentStatus) {
                pTableColumns.add(International.getString("aktueller Status"));
            }
            if (sIsOFieldsComments) {
                pTableColumns.add(International.getString("Bemerkungen"));
            }
            if (sIsOFieldsReservedFrom) {
                pTableColumns.add(International.getString("Reserviert von"));
            }
            if (sIsOFieldsReservedTo) {
                pTableColumns.add(International.getString("Reserviert bis"));
            }
            if (sIsOFieldsReservedFor) {
                pTableColumns.add(International.getString("Reserviert für"));
            }
            if (sIsOFieldsReason) {
                pTableColumns.add(International.getString("Grund"));
            }
            if (sIsOFieldsContact) {
                pTableColumns.add(International.getString("Kontakt"));
            }
            if (sIsOFieldsDamage) {
                pTableColumns.add(International.getString("Schaden"));
            }
            if (sIsOFieldsDamageSeverity) {
                pTableColumns.add(International.getString("Schwere des Schadens"));
            }
            if (sIsOFieldsReportedOn) {
                pTableColumns.add(International.getString("gemeldet am"));
            }
            if (sIsOFieldsFixedOn) {
                pTableColumns.add(International.getString("behoben am"));
            }
            if (sIsAggrDistance) {
                pTableColumns.add(DataTypeDistance.getDefaultUnitName());
            }
            if (sIsAggrRowDistance) {
                pTableColumns.add(getRowingKmString());
            }
            if (sIsAggrCoxDistance) {
                pTableColumns.add(getCoxingKmString());
            }
            if (sIsAggrSessions) {
                pTableColumns.add(International.getString("Fahrten"));
            }
            if (sIsAggrAvgDistance) {
                pTableColumns.add(DataTypeDistance.getDefaultUnitAbbrevation() + "/" + International.getString("Fahrt"));
            }
            if (sIsAggrDuration) {
                pTableColumns.add(International.getString("Dauer"));
            }
            if (sIsAggrSpeed) {
                pTableColumns.add(International.getString("Geschwindigkeit"));
            }
            if (sIsAggrZielfahrten) {
                pTableColumns.add(International.onlyFor("Zielfahrten", "de"));
            }
            if (sIsAggrWanderfahrten) {
                pTableColumns.add(International.onlyFor("Wafa-Km", "de"));
            }
            if (sIsAggrDamageCount) {
                pTableColumns.add(International.getString("Schäden"));
            }
            if (sIsAggrDamageDuration) {
                pTableColumns.add(International.getString("Reparaturdauer") + " (" +
                        International.getString("Tage") + ")");
            }
            if (sIsAggrDamageAvgDuration) {
                pTableColumns.add(International.getString("Reparaturdauer") + "/" +
                        International.getString("Schaden"));
            }
            if (sIsAggrClubwork) {
                pTableColumns.add(International.getString("Vereinsarbeit"));
            }
            if (sIsAggrClubworkTarget) {
                pTableColumns.add(International.getString("Soll"));
            }
            if (sIsAggrClubworkRelativeToTarget) {
                pTableColumns.add(International.getString("relativ zum Soll"));
            }
            if (sIsAggrClubworkOverUnderCarryOver) {
                pTableColumns.add(International.getString("abzgl. Übertrag"));
            }
            if (sIsAggrClubworkCredit) {
                pTableColumns.add(International.getString("Gutschriften"));
            }
        }
        if (sStatisticCategory == StatisticCategory.matrix) {
            pMatrixColumns = new Hashtable<String,Object>();
            Persons persons = Daten.project.getPersons(false);
            Object[] keys = data.keySet().toArray();
            for (Object k : keys) {
                StatisticsData sd = data.get(k);
                Object[] matrixkeys = (sd.matrixData != null ? sd.matrixData.keySet().toArray() : null);
                if (matrixkeys != null) {
                    for (Object mk : matrixkeys) {
                        if (mk == null) {
                            continue;
                        }
                        if (mk instanceof UUID) {
                            PersonRecord pr = persons.getPerson((UUID) mk, sTimestampBegin, sTimestampEnd, sValidAt);
                            if (pr != null) {
                                pMatrixColumns.put(pr.getQualifiedName(), mk);
                            } else {
                                pMatrixColumns.put("*** " + International.getString("ungültiger Eintrag") + " ***", mk);
                            }
                        } else if (mk instanceof Hashtable) {
                            @SuppressWarnings("unchecked")
							Hashtable<Object,Long> hash = (Hashtable<Object,Long>) mk;
                            Object[] hkeys = hash.keySet().toArray();
                            for (Object hk : hkeys) {
                                pMatrixColumns.put(hk.toString(), mk);
                            }
                        } else {
                            pMatrixColumns.put(mk.toString(), mk);
                        }
                    }
                }
            }
            String[] cNames = pMatrixColumns.keySet().toArray(new String[0]);
            Arrays.sort(cNames);
            pMatrixColumnFirst = pTableColumns.size();
            for (String cName : cNames) {
                String sName = stripKey(cName);
                pTableColumns.add(sName);
                if (!sName.equals(cName)) {
                    Object k = pMatrixColumns.get(cName);
                    pMatrixColumns.remove(cName);
                    pMatrixColumns.put(sName, k);
                }
            }
        }
        if (sStatisticCategory == StatisticCategory.logbook) {
            if (sIsLFieldsEntryNo) {
                pTableColumns.add(International.getString("Lfd. Nr."));
            }
            if (sIsLFieldsDate) {
                pTableColumns.add(International.getString("Datum"));
            }
            if (sIsLFieldsEndDate) {
                pTableColumns.add(International.getString("Enddatum"));
            }
            if (sIsLFieldsBoat) {
                pTableColumns.add(International.getString("Boot"));
            }
            if (sIsLFieldsCox) {
                pTableColumns.add(International.getString("Steuermann"));
            }
            if (sIsLFieldsCrew) {
                pTableColumns.add(International.getString("Mannschaft"));
            }
            if (sIsLFieldsStartTime) {
                pTableColumns.add(International.getString("Abfahrt"));
            }
            if (sIsLFieldsEndTime) {
                pTableColumns.add(International.getString("Ankunft"));
            }
            if (sIsLFieldsWaters) {
                pTableColumns.add(International.getString("Gewässer"));
            }
            if (sIsLFieldsDestination) {
                pTableColumns.add(International.getString("Ziel"));
            }
            if (sIsLFieldsDestinationDetails) {
                pTableColumns.add(International.getString("Ziel") + " (" +
                        International.getString("Details")  + ")");
            }
            if (sIsLFieldsDestinationAreas) {
                pTableColumns.add(International.onlyFor("Zielgebiete", "de"));
            }
            if (sIsLFieldsDistance) {
                pTableColumns.add(International.getString("Kilometer"));
            }
            if (sIsLFieldsMultiDay) {
                pTableColumns.add(International.onlyFor("Wanderfahrten", "de"));
            }
            if (sIsLFieldsSessionType) {
                pTableColumns.add(International.getString("Fahrtart"));
            }
            if (sIsLFieldsNotes) {
                pTableColumns.add(International.getString("Bemerkungen"));
            }
        }
    }

    private void setDefaultSorting(StatisticCategory cat) {
        if (itemTypeSortingCriteria == null || itemTypeSortingOrder == null) {
            return;
        }
        String crit  = itemTypeSortingCriteria.getValueFromField();
        String order = itemTypeSortingOrder.getValueFromField();
        if (cat == StatisticCategory.logbook) {
            if (!SORTING_ENTRYNO.equals(crit) && !SORTING_DATE.equals(crit)) {
                itemTypeSortingCriteria.parseAndShowValue(SORTING_ENTRYNO);
                itemTypeSortingOrder.parseAndShowValue(SORTINGORDER_ASC);
            }
        } else if (cat == StatisticCategory.other) {
            itemTypeSortingCriteria.parseAndShowValue(SORTING_NAME);
            itemTypeSortingOrder.parseAndShowValue(SORTINGORDER_ASC);
        } else {
            if (SORTING_ENTRYNO.equals(crit) || SORTING_DATE.equals(crit)) {
                itemTypeSortingCriteria.parseAndShowValue(this.SORTING_DISTANCE);
                itemTypeSortingOrder.parseAndShowValue(SORTINGORDER_DESC);
            }
        }
    }

    private void setDefaultDates(StatisticCategory cat, String statType) {
        if (itemDateFrom == null || itemDateTo == null || itemCompYear == null) {
            return;
        }
        if (cat == null) {
            String cats = itemStatisticCategory.getValueFromField();
            cat = getStatisticCategoryEnum(cats);
        }
        if (statType == null) {
            statType = itemStatisticType.getValueFromField();
        }

        itemDateFrom.setEditable(cat != StatisticCategory.competition);
        itemDateTo.setEditable(cat != StatisticCategory.competition);
        if (cat == StatisticCategory.competition) {
            Competition comp = Competition.getCompetition(statType);
            int year = EfaUtil.stringFindInt(itemCompYear.getValueFromField(), -1);
            if (comp != null && year > 0) {
                WettDef wett = Daten.wettDefs.getWettDef(comp.getCompId(), year);
                if (wett != null && wett.von != null) {
                    itemDateFrom.parseAndShowValue(wett.von.tag + "." + wett.von.monat + "." + (wett.von.jahr+year));
                }
                if (wett != null && wett.bis != null) {
                    itemDateTo.parseAndShowValue(wett.bis.tag + "." + wett.bis.monat + "." + (wett.bis.jahr+year));
                }
            }

        }
    }

    private void selectedStatisticsCategory(IItemType itemType) {
        String cats = itemType.getValueFromField();
        StatisticCategory cat = getStatisticCategoryEnum(cats);
        String defaultStatisticType = getStatisticTypeDefault(cat);
        String defaultStatisticKey = getStatisticKeyDefault(defaultStatisticType);
        StatisticType typeEnum = getStatisticTypeEnum(defaultStatisticType);
        if (itemStatisticType != null) {
            itemStatisticType.setListData(getStatisticTypes(cat, ARRAY_STRINGLIST_VALUES),
                    getStatisticTypes(cat, ARRAY_STRINGLIST_DISPLAY));
            if (defaultStatisticType != null) {
                itemStatisticType.parseAndShowValue(defaultStatisticType);
            }
        }
        if (itemStatisticKey != null) {
            if (defaultStatisticType == null) {
                defaultStatisticType = "other"; // null means we get all, but we want none!
            }
            itemStatisticKey.setListData(getStatisticKeys(defaultStatisticType, ARRAY_STRINGLIST_VALUES),
                    getStatisticKeys(defaultStatisticType, ARRAY_STRINGLIST_DISPLAY));
            if (defaultStatisticKey != null) {
                itemStatisticKey.parseAndShowValue(defaultStatisticKey);
            }
        }
        setDefaultSorting(cat);
        setDefaultDates(cat, defaultStatisticType);
        if (itemAggrFields != null) {
            itemAggrFields.setListData(getAggregationList(typeEnum, ARRAY_STRINGLIST_VALUES),
                    getAggregationList(typeEnum, ARRAY_STRINGLIST_DISPLAY));
            DataTypeList<String> selected = new DataTypeList<String>(getAggregationList(typeEnum, ARRAY_STRINGLIST_VALUES));
            setAggregations(selected);
            itemAggrFields.showValue();
        }
        if (itemShowOtherFields != null) {
            itemShowOtherFields.setListData(getOtherFieldsList(typeEnum, ARRAY_STRINGLIST_VALUES),
                    getOtherFieldsList(typeEnum, ARRAY_STRINGLIST_DISPLAY));
            DataTypeList<String> selected = new DataTypeList<String>(getOtherFieldsListDefaults(typeEnum, ARRAY_STRINGLIST_VALUES));
            setShowOtherFields(selected);
            itemShowOtherFields.setValue(selected);
            itemShowOtherFields.showValue();
        }
    }

    private void selectedStatisticsType(IItemType itemType) {
        String type = itemType.getValueFromField();
        StatisticType typeEnum = getStatisticTypeEnum(type);
        String defaultStatisticKey = getStatisticKeyDefault(type);
        if (itemStatisticKey != null) {
            itemStatisticKey.setListData(getStatisticKeys(type, ARRAY_STRINGLIST_VALUES),
                    getStatisticKeys(type, ARRAY_STRINGLIST_DISPLAY));
            if (defaultStatisticKey != null) {
                itemStatisticKey.parseAndShowValue(defaultStatisticKey);
            }
        }
        setDefaultDates(null, type);
        if (itemAggrFields != null) {
            itemAggrFields.setListData(getAggregationList(typeEnum, ARRAY_STRINGLIST_VALUES),
                    getAggregationList(typeEnum, ARRAY_STRINGLIST_DISPLAY));
            DataTypeList<String> selected = new DataTypeList<String>(getAggregationList(typeEnum, ARRAY_STRINGLIST_VALUES));
            setAggregations(selected);
            itemAggrFields.showValue();
        }
        if (itemShowOtherFields != null) {
            itemShowOtherFields.setListData(getOtherFieldsList(typeEnum, ARRAY_STRINGLIST_VALUES),
                    getOtherFieldsList(typeEnum, ARRAY_STRINGLIST_DISPLAY));
            DataTypeList<String> selected = new DataTypeList<String>(getOtherFieldsListDefaults(typeEnum, ARRAY_STRINGLIST_VALUES));
            setShowOtherFields(selected);
            itemShowOtherFields.setValue(selected);
            itemShowOtherFields.showValue();
        }
    }

    public void itemListenerAction(IItemType itemType, AWTEvent event) {
        if (itemType.getName().equals(STATISTICCATEGORY) &&
                event instanceof ItemEvent &&
                ((ItemEvent)event).getStateChange() == ItemEvent.SELECTED) {
            selectedStatisticsCategory(itemType);
        }
        if (itemType.getName().equals(STATISTICTYPE) &&
                event instanceof ItemEvent  &&
                ((ItemEvent)event).getStateChange() == ItemEvent.SELECTED) {
            selectedStatisticsType(itemType);
        }
        if (itemType.getName().equals(FILTERGENDERALL) && event instanceof ActionEvent) {
            if (itemFilterGender != null && itemType.getValueFromField() != null) {
                itemFilterGender.setEnabled(itemType.getValueFromField().equals(Boolean.toString(false)));
            }
        }
        if (itemType.getName().equals(FILTERSTATUSALL) && event instanceof ActionEvent) {
            if (itemFilterStatus != null && itemType.getValueFromField() != null) {
                itemFilterStatus.setEnabled(itemType.getValueFromField().equals(Boolean.toString(false)));
            }
        }
        if (itemType.getName().equals(FILTERSESSIONTYPEALL) && event instanceof ActionEvent) {
            if (itemFilterSessionType != null && itemType.getValueFromField() != null) {
                itemFilterSessionType.setEnabled(itemType.getValueFromField().equals(Boolean.toString(false)));
            }
        }
        if (itemType.getName().equals(FILTERBOATTYPEALL) && event instanceof ActionEvent) {
            if (itemFilterBoatType != null && itemType.getValueFromField() != null) {
                itemFilterBoatType.setEnabled(itemType.getValueFromField().equals(Boolean.toString(false)));
            }
        }
        if (itemType.getName().equals(FILTERBOATSEATSALL) && event instanceof ActionEvent) {
            if (itemFilterBoatSeats != null && itemType.getValueFromField() != null) {
                itemFilterBoatSeats.setEnabled(itemType.getValueFromField().equals(Boolean.toString(false)));
            }
        }
        if (itemType.getName().equals(FILTERBOATRIGGINGALL) && event instanceof ActionEvent) {
            if (itemFilterBoatRigging != null && itemType.getValueFromField() != null) {
                itemFilterBoatRigging.setEnabled(itemType.getValueFromField().equals(Boolean.toString(false)));
            }
        }
        if (itemType.getName().equals(FILTERBOATCOXINGALL) && event instanceof ActionEvent) {
            if (itemFilterBoatCoxing != null && itemType.getValueFromField() != null) {
                itemFilterBoatCoxing.setEnabled(itemType.getValueFromField().equals(Boolean.toString(false)));
            }
        }
        if (itemType.getName().equals(FILTERBOATOWNERALL) && event instanceof ActionEvent) {
            if (itemFilterBoatOwner != null && itemType.getValueFromField() != null) {
                itemFilterBoatOwner.setEnabled(itemType.getValueFromField().equals(Boolean.toString(false)));
            }
        }
        if (itemType.getName().equals(OUTPUTTYPE) && event instanceof ItemEvent) {
            OutputTypes newOutputType = getOutputTypeEnumFromString(itemType.getValueFromField());
            setVisibleItems(newOutputType);
            if (itemOutputFile != null) {
                String fname = itemOutputFile.getValueFromField();
                int pos = (fname != null ? fname.lastIndexOf(".") : -1);
                if (pos > 0) {
                    fname = fname.substring(0, pos) + "." + this.getOutputTypeFileExtensionForEnum(newOutputType);
                    itemOutputFile.parseAndShowValue(fname);
                }
            }
        }
        if (itemType.getName().equals(PUBLICLYAVAILABLE) && event instanceof ActionEvent) {
            ((ItemTypeBoolean)itemType).setColor(
                    (itemType.getValueFromField().equals(Boolean.toString(true)) ?
                            Color.blue : Color.black));
        }
        if (itemType.getName().equals(COMPYEAR) &&
                event instanceof FocusEvent &&
                ((FocusEvent)event).getID() == FocusEvent.FOCUS_LOST) {
            setDefaultDates(null, null);
        }
        if (itemType.getName().equals(GUIITEM_OUTPUTFTP) &&
                event instanceof ActionEvent) {
            String f = itemOutputFile.getValueFromField();
            f = FTPClient.getFtpStringGuiDialog(f);
            if (f != null) {
                itemOutputFile.parseAndShowValue(f);
            }
        }
        if (itemType.getName().equals(GUIITEM_OUTPUTEMAIL) &&
                event instanceof ActionEvent) {
            String f = itemOutputFile.getValueFromField();
            f = Email.getEmailStringGuiDialog(f);
            if (f != null) {
                itemOutputFile.parseAndShowValue(f);
            }
        }
    }

    private void setVisibleItems(OutputTypes output) {
        if (itemOutputFile != null) {
            itemOutputFile.setVisible(output != OutputTypes.internal &&
                    output != OutputTypes.internaltxt &&
                    output != OutputTypes.efawett);
            itemOutputFileHINT.setVisible(itemOutputFile.isVisible());
        }
        if (itemOutputFtpButton != null) {
            itemOutputFtpButton.setVisible(output != OutputTypes.internal &&
                    output != OutputTypes.internaltxt &&
                    output != OutputTypes.efawett);
        }
        if (itemOutputEmailButton != null) {
            itemOutputEmailButton.setVisible(output != OutputTypes.internal &&
                    output != OutputTypes.internaltxt &&
                    output != OutputTypes.efawett);
        }
        if (itemOutputEncoding != null) {
            itemOutputEncoding.setVisible(output == OutputTypes.csv || output == OutputTypes.html);
        }
        if (itemOutputHtmlUpdateTable != null) {
            itemOutputHtmlUpdateTable.setVisible(output == OutputTypes.html);
        }
        if (itemOutputCsvSeparator != null) {
            itemOutputCsvSeparator.setVisible(output == OutputTypes.csv);
        }
        if (itemOutputCsvQuotes != null) {
            itemOutputCsvQuotes.setVisible(output == OutputTypes.csv);
        }
    }

    public String[] getLogbookNames(int valuesOrDisplay) {
        String[] lb = (Daten.project != null ? Daten.project.getAllLogbookNames() : null);
        if (lb == null) {
            lb = new String[0];
        }
        String[] all = new String[lb.length + 1];
        if (valuesOrDisplay == ARRAY_STRINGLIST_VALUES) {
            all[0] = "";
        } else {
            all[0] = "--- " + International.getString("alle") + " ---";
        }
        for (int i=0; i<lb.length; i++) {
            all[i+1] = lb[i];
        }
        return all;
    }

}
