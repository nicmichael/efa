package de.nmichael.efa.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.batik.ext.swing.GridBagConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.core.items.IItemFactory;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoatstatusList;
import de.nmichael.efa.core.items.ItemTypeButton;
import de.nmichael.efa.core.items.ItemTypeDate;
import de.nmichael.efa.core.items.ItemTypeDistance;
import de.nmichael.efa.core.items.ItemTypeItemList;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeLabelHeader;
import de.nmichael.efa.core.items.ItemTypeString;
import de.nmichael.efa.core.items.ItemTypeStringAutoComplete;
import de.nmichael.efa.core.items.ItemTypeStringList;
import de.nmichael.efa.core.items.ItemTypeTime;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.DestinationRecord;
import de.nmichael.efa.data.GroupRecord;
import de.nmichael.efa.data.Groups;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.util.AutoCompleteList;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;


/*
 * If a group of persons goes on a trip together, most of the group members go with an individual boat.
 * But the other data stays the same for the group members.
 *
 * This dialog is created to support this style of trips with an efficient way of entering the same
 * data for multiple group members.
 * A user can enter start date/time, type of session  and a set of name/boat pairs.  
 * Also destination, water and comment can be entered.
 * 
 * When saving the item, efa creates a single session entry for each name/boat pair.
 * 
 * The number of automatically added empty name/boat pairs can be configured in efaConfig. 
 * 
 * The dialog takes care for several constraints
 * - a known boat name may only occur once in the name/boat pairs.
 * - a known person name may only occur once in the name/boat pairs.
 * - the autocompletes of names and boats are automatically cleared of items which are already entered on this dialog
 * - if you enter a person's name, and the person has a standard boat, the boat field is automatically filled
 *   (if the boat is not yet taken by another person)
 * 
 * 
 * Design decisions
 * ------------------
 * It was a question how to implement this. Should it be put directly to efaBaseFrame (where all Elements are put)
 * 
 */
public class EfaBaseFrameMultisession extends EfaBaseFrame implements IItemListener, IItemFactory {

	private final static String  NOT_STORED_ITEM_PREFIX = "_";
	private final static String  STR_SPACER = "   ";
	private final static String  STR_NAME_LOOKUP = "NAME_LOOKUP";
	private final static String  STR_BOAT_LOOKUP = "BOOT_LOOKUP";
	private final static int 	 ADD_HEIGHT_TO_DIALOG = 180; // amount of pixels of free space in the dialog to grow (for additional participants/boats)
    private JPanel teilnehmerUndBoot;
	private ItemTypeItemList nameAndBoat;
    

    public EfaBaseFrameMultisession(int mode) {
        super(mode);
    }

    public EfaBaseFrameMultisession(JDialog parent, int mode) {
        super(parent, mode);
    }
    
    public EfaBaseFrameMultisession(JDialog parent, int mode, AdminRecord admin,
            Logbook logbook, String entryNo) {
        super(parent, mode, admin, logbook, entryNo);
    }

    public EfaBaseFrameMultisession(EfaBoathouseFrame efaBoathouseFrame, int mode) {
        super(efaBoathouseFrame, mode);
    }
	

    /**
     * 
     */
    /* mostly same elements, but slightly diffrent order of the values */
    protected void iniGuiMain() {
    	int yPos=0;
    	int HEADER_WIDTH=9;
    	
        JPanel mainInputPanel = new JPanel();
        mainInputPanel.setLayout(new GridBagLayout());
        mainPanel.add(mainInputPanel, BorderLayout.NORTH);

        ItemTypeLabelHeader header = createHeader("CREATE_MULTISESSION", 0, null, 
        		(mode == EfaBaseFrame.MODE_BOATHOUSE_START_MULTISESSION ? International.getString("Mehrere Einzelfahrten beginnen") : International.getString("Mehrere Einzelfahrten nachtragen")),
        				HEADER_WIDTH);
        header.displayOnGui(this,  mainInputPanel, 0, yPos);
        yPos++;

        // Date, Enddate, optionally a button to append end date
        date = new ItemTypeDate(LogbookRecord.DATE, new DataTypeDate(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Datum"));
        date.showWeekday(true);
        date.setFieldSize(100, FIELD_HEIGHT);
        date.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        date.setFieldGrid(1, GridBagConstraints.WEST, GridBagConstraints.NONE);
        date.setWeekdayGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        date.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        date.displayOnGui(this, mainInputPanel, 0, yPos);
        date.registerItemListener(this);
        
        // End Date
        enddate = new ItemTypeDate(LogbookRecord.ENDDATE, new DataTypeDate(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("bis"));
        enddate.setMustBeAfter(date, false);
        enddate.showWeekday(true);
        enddate.setFieldSize(100, FIELD_HEIGHT);
        enddate.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        enddate.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        enddate.setWeekdayGrid(1, GridBagConstraints.WEST, GridBagConstraints.NONE);
        enddate.showOptional(true);
        if (isModeBoathouse()) {
            enddate.setOptionalButtonText("+ " + International.getString("Enddatum"));
        }
        enddate.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        enddate.displayOnGui(this, mainInputPanel, 4, yPos);
        enddate.registerItemListener(this);
        if (isModeBoathouse() && !Daten.efaConfig.getValueAllowEnterEndDate()) {
            enddate.setVisible(false);
        }

        yPos++;

        
        // Start Time, End Time, including according labels  AND Session Type.
        
        // StartTime
        starttime = new ItemTypeTime(LogbookRecord.STARTTIME, new DataTypeTime(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Abfahrt"));
        starttime.setFieldSize(200, FIELD_HEIGHT);
        starttime.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        starttime.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        starttime.enableSeconds(false);
        starttime.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        //starttime.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);
        starttime.displayOnGui(this, mainInputPanel, 0, yPos);
        starttime.registerItemListener(this);

        starttimeInfoLabel = new ItemTypeLabel("GUIITEM_STARTTIME_INFOLABEL",
                IItemType.TYPE_PUBLIC, null, "");
        starttimeInfoLabel.setFieldGrid(5, GridBagConstraints.WEST, GridBagConstraints.NONE);
        starttimeInfoLabel.setVisible(false);
        //starttimeInfoLabel.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);        
        starttimeInfoLabel.displayOnGui(this, mainInputPanel, 3, yPos);
        
        yPos++;

        // EndTime
        endtime = new ItemTypeTime(LogbookRecord.ENDTIME, new DataTypeTime(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Ankunft"));
        endtime.setFieldSize(200, FIELD_HEIGHT);
        endtime.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        endtime.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        endtime.enableSeconds(false);
        endtime.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        endtime.displayOnGui(this, mainInputPanel, 0, yPos);
        endtime.registerItemListener(this);

        endtimeInfoLabel = new ItemTypeLabel("GUIITEM_ENDTIME_INFOLABEL",
                IItemType.TYPE_PUBLIC, null, "");
        endtimeInfoLabel.setFieldGrid(5, GridBagConstraints.WEST, GridBagConstraints.NONE);
        endtimeInfoLabel.setVisible(false);
        endtimeInfoLabel.displayOnGui(this, mainInputPanel, 3, yPos);

        endtime.setVisible(mode == EfaBaseFrame.MODE_BOATHOUSE_LATEENTRY_MULTISESSION);
        endtimeInfoLabel.setVisible(endtime.isVisible());
        
        yPos++;
        
        // Session Type
        sessiontype = new ItemTypeStringList(LogbookRecord.SESSIONTYPE, EfaTypes.TYPE_SESSION_NORMAL,
                EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, null, International.getString("Fahrtart"));
        sessiontype.setFieldSize(200, FIELD_HEIGHT);
        sessiontype.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        sessiontype.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        sessiontype.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        sessiontype.displayOnGui(this, mainInputPanel, 0, yPos);
        sessiontype.registerItemListener(this);
        sessiontype.setReplaceValues(Daten.efaTypes.getSessionTypeReplaceValues());
        
        // Session Type Info
        sessionTypeInfo = new ItemTypeLabel("SESSIONTYPE_LABEL", IItemType.TYPE_PUBLIC, null, "");
        sessionTypeInfo.setFieldGrid(5, GridBagConstraints.WEST, GridBagConstraints.NONE);
        sessionTypeInfo.registerItemListener(this);
        sessionTypeInfo.activateMouseClickListener();
        sessionTypeInfo.displayOnGui(this, mainInputPanel, 5, yPos);
        
        yPos++;
        //---------------------------------------------------------------------
        
        teilnehmerUndBoot=new JPanel();
        teilnehmerUndBoot.setLayout(new GridBagLayout());
        teilnehmerUndBoot.removeAll();
		teilnehmerUndBoot.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        mainInputPanel.add(teilnehmerUndBoot, new GridBagConstraints(0, yPos, HEADER_WIDTH, 1, 0, 0,
                GridBagConstants.WEST, GridBagConstants.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
        
        
       // mainInputPanel.ad
		nameAndBoat = new ItemTypeItemList("NameAndBoat", new Vector<IItemType[]>(), this,
				IItemType.TYPE_PUBLIC, null,
				International.getString("Teilnehmer und Boot"));
		//crontab.setScrollPane(1000, 400);
		nameAndBoat.setRepeatTitle(false);        
		nameAndBoat.setAppendPositionToEachElement(true);
		nameAndBoat.setXForAddDelButtons(6); // two columns, both with name, edit field, autocomplete button
		nameAndBoat.setItemsOrientation(ItemTypeItemList.Orientation.horizontal);
		nameAndBoat.setFieldGrid(8, GridBagConstraints.EAST, GridBagConstraints.BOTH);
		nameAndBoat.setFirstColumnMinWidth(getLongestLabelTextWidth(mainInputPanel));
		nameAndBoat.setPadding(0, 0, 10, 10);
		//nameAndBoat.setFirstColumnMinWidth(mainInputPanelGrid.getLayoutDimensions()[0][0]);
		// Multisession means at least two persons with an individual boat are to go
		addStandardItems(nameAndBoat,4);
		nameAndBoat.displayOnGui(this, teilnehmerUndBoot, 0, 0);
		nameAndBoat.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		
        // Name (crew) and Boat (single boat items only)
                
        yPos++;
        
        header = createHeader("CREATE_DESTINATION", 0, null, International.getString("Ziel und weitere Angaben"),HEADER_WIDTH);
        header.displayOnGui(this,  mainInputPanel, 0, yPos);
        yPos++;

        // Destination
        destination = new ItemTypeStringAutoComplete(LogbookRecord.DESTINATIONNAME, "", IItemType.TYPE_PUBLIC, null, 
                International.getStringWithMnemonic("Ziel") + " / " +
                International.getStringWithMnemonic("Strecke"), true);
        destination.setFieldSize(400, FIELD_HEIGHT);
        destination.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        destination.setFieldGrid(8, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        destination.setAutoCompleteData(autoCompleteListDestinations);
        destination.setChecks(true, false);
        destination.setIgnoreEverythingAfter(DestinationRecord.DESTINATION_VARIANT_SEPARATOR);
        destination.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        destination.displayOnGui(this, mainInputPanel, 0, yPos);
        destination.registerItemListener(this);
        yPos++;
        
        destinationInfo = new ItemTypeString("GUIITEM_DESTINATIONINFO", "",
                IItemType.TYPE_PUBLIC, null, International.getString("Gewässer"));
        destinationInfo.setFieldSize(400, FIELD_HEIGHT);
        destinationInfo.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        destinationInfo.setFieldGrid(8, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        destinationInfo.displayOnGui(this, mainInputPanel, 0, yPos);
        destinationInfo.setEditable(false);
        destinationInfo.setVisible(false);
        yPos++;
        
        // Waters
        waters = new ItemTypeStringAutoComplete(GUIITEM_ADDITIONALWATERS, "", IItemType.TYPE_PUBLIC, null,
                International.getStringWithMnemonic("Gewässer"), true);
        waters.setFieldSize(400, FIELD_HEIGHT);
        waters.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        waters.setFieldGrid(8, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        waters.setAutoCompleteData(autoCompleteListWaters);
        waters.setChecks(true, false);
        waters.setIgnoreEverythingAfter(LogbookRecord.WATERS_SEPARATORS);
        waters.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        waters.displayOnGui(this, mainInputPanel, 0, yPos);
        waters.registerItemListener(this);
        waters.setVisible(false);        
        
        yPos++;
        	
        // Distance
        distance = new ItemTypeDistance(LogbookRecord.DISTANCE, null, IItemType.TYPE_PUBLIC, null,
                DataTypeDistance.getDefaultUnitName());
        distance.setFieldSize(200, FIELD_HEIGHT);
        distance.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        distance.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        distance.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        distance.displayOnGui(this, mainInputPanel, 0, yPos);
        distance.registerItemListener(this);     
        distance.setVisible(mode == EfaBaseFrame.MODE_BOATHOUSE_LATEENTRY_MULTISESSION);
        
        yPos++;
        // Comments
        comments = new ItemTypeString(LogbookRecord.COMMENTS, null, IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Bemerkungen"));
        comments.setFieldSize(400, FIELD_HEIGHT);
        comments.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        comments.setFieldGrid(8, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        comments.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        comments.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);
        comments.displayOnGui(this, mainInputPanel, 0, yPos);
        comments.registerItemListener(this);
        yPos++;
        
        //---------------- old *-----------------------
        
           // Info Label
        infoLabel.setForeground(Color.blue);
        infoLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        infoLabel.setText(" ");
        mainInputPanel.add(infoLabel,
                new GridBagConstraints(0, yPos, 8, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 20, 10, 0), 0, 0));
 
        // Save Button
        saveButton = new ItemTypeButton("SAVE", IItemType.TYPE_PUBLIC, null, (mode == EfaBaseFrame.MODE_BOATHOUSE_START_MULTISESSION ? International.getString("Eintrag speichern") : International.getString("Nachtrag")));
        saveButton.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        saveButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        saveButton.displayOnGui(this, mainPanel, BorderLayout.SOUTH);
        saveButton.registerItemListener(this);

        createAllUnusedElements();
        
        destination.setValidAt(date, starttime);
        
        Dimension dim = mainPanel.getMinimumSize();
        dim.height = dim.height + ADD_HEIGHT_TO_DIALOG;
        mainPanel.setMinimumSize(dim);        
    }    
	
    // elements in efaBaseFrame which are not used by this dialog.
    // but unfortunately, as a subclass of efaBaseFrame needs to instantiate these fields. :-/
    
    private void createAllUnusedElements() {
    
    	entryno = new ItemTypeString(LogbookRecord.ENTRYID, "", IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Lfd. Nr."));
    	entryno.setVisible(false);
       /* 
    	opensession = new ItemTypeLabel(LogbookRecord.OPEN, IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Fahrt offen"));
        opensession.setVisible(false);
        
        closesessionButton = new ItemTypeButton("CloseSessionButton", IItemType.TYPE_PUBLIC, null, 
                International.getStringWithMnemonic("Fahrt offen") + " - " +
                International.getStringWithMnemonic("jetzt beenden"));
        closesessionButton.setVisible(false);
        
        boat = new ItemTypeStringAutoComplete(LogbookRecord.BOATNAME, "", IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Boot"), true);
        boat.setVisible(false);
        boatvariant = new ItemTypeStringList(LogbookRecord.BOATVARIANT, "",
                null, null,
                IItemType.TYPE_PUBLIC, null, International.getString("Variante"));
        boatvariant.setVisible(false);
        
        cox = new ItemTypeStringAutoComplete(LogbookRecord.COXNAME, "", IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Steuermann"), true);
        cox.setVisible(false);
     */   
        crew = new ItemTypeStringAutoComplete[LogbookRecord.CREW_MAX];
        /*
        for (int i=1; i<=crew.length; i++) {
         	 crew[i-1] = new ItemTypeStringAutoComplete(LogbookRecord.getCrewFieldNameName(i), "", IItemType.TYPE_PUBLIC, null,
                     (i == 1 ? International.getString("Mannschaft") + " " : (i < 10 ? "  " :"")) + Integer.toString(i), true);
        	crew[i-1].setVisible(false);
        }
        
        boatcaptain = new ItemTypeStringList(LogbookRecord.BOATCAPTAIN, "",
                LogbookRecord.getBoatCaptainValues(), LogbookRecord.getBoatCaptainDisplay(),
                IItemType.TYPE_PUBLIC, null, International.getString("Obmann"));
        boatcaptain.setVisible(false);   
        
        sessiongroup = new ItemTypeStringAutoComplete(LogbookRecord.SESSIONGROUPID,
                "", IItemType.TYPE_PUBLIC, null,
                International.getStringWithMnemonic("Fahrtgruppe"), true);
        sessiongroup.setVisible(false);      
        
        remainingCrewUpButton = new ItemTypeButton("REMAININGCREWUP", IItemType.TYPE_PUBLIC, null, "");
        remainingCrewDownButton = new ItemTypeButton("REMAININGCREWDOWN", IItemType.TYPE_PUBLIC, null, "");
        remainingCrewUpButton.setVisible(false);  
        remainingCrewDownButton.setVisible(false);  
        
        boatDamageButton = new ItemTypeButton("BOATDAMAGE", IItemType.TYPE_PUBLIC, null, International.getString("Bootsschaden melden"));
        boatNotCleanedButton = new ItemTypeButton("BOATNOTCLEANED", IItemType.TYPE_PUBLIC, null, International.getString("Boot war nicht geputzt"));
        boatDamageButton.setVisible(false);  
        boatNotCleanedButton.setVisible(false);     
        */
    }
    
    /**
     * Initialization of the Dialog
     */
    protected void iniDialog() {
    	iniData();
    	iniGuiBase();
        iniGuiMain();
        iniGuiFieldDefaultValues();
    }    
    
    private void iniGuiFieldDefaultValues() {

        /*public static final int MODE_BOATHOUSE_START_MULTISESSION=9;
        public static final int MODE_BOATHOUSE_LATEENTRY_MULTISESSION=10;
        */
    	
    	if (mode == MODE_BOATHOUSE_START_MULTISESSION) {
	    	// set Date
	        String d = EfaUtil.getCurrentTimeStampDD_MM_YYYY();
	        date.parseAndShowValue(d);
	        updateTimeInfoFields();
	        date.setUnchanged();
	        if (isModeFull()) {
	            date.setSelection(0, Integer.MAX_VALUE);
	        }
	        
	        setTime(starttime, Daten.efaConfig.getValueEfaDirekt_plusMinutenAbfahrt(), null);
	        
	        if (Daten.efaConfig.getValueEfaDirekt_eintragNichtAenderbarUhrzeit()) {
	            setFieldEnabled(false, true, starttime);
	            setFieldEnabled(false, false, endtime);
	        } else {
	            setFieldEnabled(true, true, starttime);
	            setFieldEnabled(false, false, endtime);
	        }
	        
	        setFieldEnabled(true, true, destination);
	        setFieldEnabled(false, false, distance);
	        setFieldEnabled(true, true, comments);
	        
	        updateTimeInfoFields();
    	} else if (mode == MODE_BOATHOUSE_LATEENTRY_MULTISESSION) {
            date.parseAndShowValue(EfaUtil.getCurrentTimeStampDD_MM_YYYY());
            updateTimeInfoFields();
	        if (isModeFull()) {
	            date.setSelection(0, Integer.MAX_VALUE);
	        }
            
            setFieldEnabled(true, true, date);
            setFieldEnabled(true, true, starttime);
            setFieldEnabled(true, true, endtime);
            setFieldEnabled(true, true, destination);
            setFieldEnabled(true, true, distance);
            setFieldEnabled(true, true, comments);

	        updateTimeInfoFields();
            
    	}

        setRequestFocus(date);
    }
    
    
    /**
     * Initialize the EfaBaseFrameMultisession dialog.
     * The base class efaBaseFrame is instantiated ONCE in efaBoatHouse and re-used for several cases. Maybe due to performance issues.
     * The EfaBaseFrameMultisession dialog is instantiated every time it is opened.
     * So it needs an initialisation for the respective autocomplete lists and such. 
     */
    private void iniData() {

        if (Daten.project == null) {
            return;
        } else {
        	this.logbook = Daten.project.getCurrentLogbook();
        	if (!logbook.isOpen()) {
        		return;
        	}
        }
      
        ProjectRecord pr = Daten.project.getLoogbookRecord(logbook.getName());
        if (pr != null) {
            logbookValidFrom = logbook.getValidFrom();
            logbookInvalidFrom = logbook.getInvalidFrom();
        }
        try {
            iterator = logbook.data().getDynamicIterator();
            autoCompleteListBoats.setDataAccess(Daten.project.getBoats(false).data(), logbookValidFrom, logbookInvalidFrom - 1);
            autoCompleteListPersons.setDataAccess(Daten.project.getPersons(false).data(), logbookValidFrom, logbookInvalidFrom - 1);
            autoCompleteListDestinations.setDataAccess(Daten.project.getDestinations(false).data(), logbookValidFrom, logbookInvalidFrom - 1);
            autoCompleteListWaters.setDataAccess(Daten.project.getWaters(false).data(), logbookValidFrom, logbookInvalidFrom - 1);
        } catch (Exception e) {
            Logger.logdebug(e);
            iterator = null;
        }
        if (isModeBoathouse()) {
            autoCompleteListDestinations.setFilterDataOnlyForThisBoathouse(true);
            autoCompleteListDestinations.setPostfixNamesWithBoathouseName(false);
            autoCompleteListBoats.setFilterDataOnlyOneSeaterBoats(true); //we only want boats for a single person.
        }
        autoCompleteListBoats.update();
        autoCompleteListPersons.update();
        autoCompleteListDestinations.update();
        autoCompleteListWaters.update();
        
    }
    
	/**
	 * Creates an Item consisting of Name and Boat for "Teilnehmer und Boot" section
	 * Where boat only contains single person boats.
	 * 
	 * The Name field knows the Boat field as "other" field. 
	 * This is neccesary for the feature "auto fill in the person's standard boat, if available".
	 * And it is used by the itemListenerAction()
	 */
    public IItemType[] getDefaultItems(String itemName) {

    	ItemTypeStringAutoComplete[] items = new ItemTypeStringAutoComplete[2];
        //Name
        	items[0] = getGuiAutoComplete(itemName+STR_NAME_LOOKUP, International.getString("Name"), this.autoCompleteListPersons);
	        items[0].setFieldSize(200, -1);
	        items[0].setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
	        items[0].setValidAt(date, starttime);
	        items[0].registerItemListener(this);
	        
	    // Boat
        	items[1] = getGuiAutoComplete(itemName+STR_BOAT_LOOKUP, STR_SPACER+International.getString("Boot"), this.autoCompleteListBoats);
	        items[1].setFieldSize(200, -1);
	        items[1].setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
	        items[1].setValidAt(date, starttime);

	        items[0].setOtherField(items[1]);
	        
        return items;
        
    }    
    
    /*
     * When leaving the name field, check if a known name is entered.
     * If yes, try to find out the person's standard boat and fill its fully qualified name in the second field,
     * if the boat is not yet assigned to another person in this dialog, and it is not on the water (when starting a new session)
     *
     * Otherwise, use itemListener of efaBaseFrame super class.
     *
     */
	public void itemListenerAction(IItemType item, AWTEvent event) {
        int id = event.getID();
        boolean done = false;
        
        if (id == FocusEvent.FOCUS_LOST) {
        	try {
				if ((item instanceof ItemTypeStringAutoComplete) && (item.getName().contains(STR_NAME_LOOKUP))){
					// focus lost for name field, we want to automatically fill the boat field with the
					// person's standard boat.
	
					ItemTypeStringAutoComplete field = (ItemTypeStringAutoComplete) item;
					done = true;
					if (field.isValidInput()) {//field has a known person's name
	                	String assignedBoatNameForThisEntry = field.getOtherField().getValue(); 
	                	if (assignedBoatNameForThisEntry != null && assignedBoatNameForThisEntry.isEmpty()) {
	
							//Obtain the person's standard boat
	                		PersonRecord person = Daten.project.getPersons(false).getPerson(field.getValue(), System.currentTimeMillis());
							if (person!=null) {
				                BoatRecord r = Daten.project.getBoats(false).getBoat(
				                        person.getDefaultBoatId(), System.currentTimeMillis());	
				                if (r!=null) {
				                	if (field.getOtherField().getAutoCompleteData().getDataVisible().contains(r.getQualifiedName())) {
				                		field.getOtherField().setValue(r.getQualifiedName());
				                	}
			                	}
			                }
	                	}
					}
				} 
        	} catch (Exception e) {
				Logger.log(e);
			}
        	
        }
        if (!done) {
			//otherwise use other Action handler
		    super.itemListenerAction(item, event);
        }
	}	    
    
	/**
	 * Create an ItemTypeAutoComplete 
	 * @param name Name of the field
	 * @param description Caption of the field
	 * @param list AutoCompleteList for the field
	 * @return ItemTypeAutoComplete field
	 */
    protected ItemTypeStringAutoComplete getGuiAutoComplete(String name, String description, AutoCompleteList list) {
        ItemTypeStringAutoComplete item = new ItemTypeStringAutoComplete(name, "", IItemType.TYPE_PUBLIC , null, description, true);
        item.setFieldSize(200, FIELD_HEIGHT); // 21 pixels high for new flatlaf, otherwise chars y and p get cut off 
        //true= automatically remove items from the list, if it is chosen by the user.
        //so that a name or a person cannot be chosen twice in this dialog
        item.setAutoCompleteData(list,true); //automatically remove already chosen items from the list
        item.setChecks(true, true);
        item.setShowButtonFocusable(false);
        return item;
    }        
    
    /**
     * Return the width (in pixels) for the longest caption of some labels.
     * This is used to try to align the name/boatname fields in the dialog with the other fields for better layout
     * @param panel  This panel is used to get font metrics
     * @return maximum withs of the label captions 
     */
    private int getLongestLabelTextWidth(JPanel panel) {
    	int lBemerk = panel.getFontMetrics(panel.getFont()).stringWidth(International.getString("Bemerkungen")+": ");
    	int lSessType = panel.getFontMetrics(panel.getFont()).stringWidth(International.getString("Fahrtart")+": ");
    	int lDest = panel.getFontMetrics(panel.getFont()).stringWidth(International.getStringWithMnemonic("Ziel") + " / " + International.getStringWithMnemonic("Strecke")+": ");
    	
    	return Math.max(lBemerk, Math.max(lSessType, lDest));
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
	private ItemTypeLabelHeader createHeader(String uniqueName, int type, String category, String caption, int gridWidth) {
		// ensure that the header value does not get saved in efaConfig file by adding a
		// special prefix
		ItemTypeLabelHeader item = new ItemTypeLabelHeader(NOT_STORED_ITEM_PREFIX + uniqueName, type, category, " " + caption);
		item.setPadding(0, 0, 10, 10);
		item.setFieldGrid(gridWidth, GridBagConstraints.EAST, GridBagConstraints.BOTH);
		return item;
	}    
    
    private void addStandardItems(ItemTypeItemList target, int numberOfItems) {
	    for (int i = 0; i<numberOfItems; i++) {
	    	target.addItems(this.getDefaultItems(target.getName()));
	    }
    }
    
    /*
     * updateGui()
     * This method is called when an ItemTypeList gets new items or gets items removed.
     * Usually, this happens on a BaseTabbedDialog. But unfortunately, efaBaseFrame is just a BaseDialog.
     * 
     * The only component which grows on this screen is "nameAndBoat". 
     * It is put in a specific panel. So if nameAndBoat changes its items, we just have to remove all components
     * from the container Panel teilnehmerUndBoot, and just re-add nameAndBoat to that panel.
     * This leads to a screen refresh.
     */
    public void updateGui() {
    	
    	teilnehmerUndBoot.removeAll();

    	// reset the autocomplete lists, as items are removed automatically to avoid duplicate persons/boats in the total list.
    	autoCompleteListBoats.reset();
		autoCompleteListPersons.reset();
		// put the remaining list on the GUI
		nameAndBoat.displayOnGui(this, teilnehmerUndBoot, 0, 1);
		this.revalidate();// without this, there will be repainting problems in the gui when removing lines
		
		// remove the already selected items from the autocomplete list, to avoid duplicate entries
		for (int i=0; i<nameAndBoat.getItemCount(); i++) {
			ItemTypeStringAutoComplete [] row = (ItemTypeStringAutoComplete []) nameAndBoat.getItems(i);
			row[0].removeFromVisible(row[0].getValue());
			row[1].removeFromVisible(row[1].getValue());
		}

    }
	

    
    // Datensatz speichern
    // liefert "true", wenn erfolgreich
    protected boolean saveEntry() {
        if (!isLogbookReady()) {
            return false;
        }

        // Da das Hinzufügen eines Eintrags in der Bootshausversion wegen des damit verbundenen
        // Speicherns lange dauern kann, könnte ein ungeduldiger Nutzer mehrfach auf den "Hinzufügen"-
        // Button klicken. "synchronized" hilft hier nicht, da sowieso erst nach Ausführung des
        // Threads der Klick ein zweites Mal registriert wird. Da aber nach Abarbeitung dieser
        // Methode der Frame "EfaFrame" vom Stack genommen wurde und bei der zweiten Methode damit
        // schon nicht mehr auf dem Stack ist, kann eine Überprüfung, ob der aktuelle Frame
        // "EfaFrame" ist, benutzt werden, um eine doppelte Ausführung dieser Methode zu verhindern.
        if (Dialog.frameCurrent() != this) {
            return false;
        }
        
        // make sure to autocomplete all texts once more in the input fields.
        // users have found strange ways of working around completion...
        autocompleteAllFields();

        // run all checks before saving this entry
        if (!checkDate() ||
            !checkTime() ||
            !checkAllowedDateForLogbook() ||
            !checkMultiDayTours() ||
        	!checkMultiSessionMisspelledPersons() ||
            !checkMultiSessionDuplicatePersonsAndBoats() ||
            !checkMultiSessionBoatStatus() ||
            !checkMultiSessionNameAndBoatValuesValid()||
            !checkUnknownNames() || 
            !checkProperUnknownNames() ||
            !checkAllowedPersons() ||
            !checkAllowedPersonsForBoat() ||
            !checkDestinationNameValid() ||
            !checkAllDataEntered() ||
            !checkSessionType()) {
            return false;
        }

        boolean success = saveEntriesInLogbook();

        if (isModeFull()) {
            if (success) {
                setEntryUnchanged();
                boolean createNewRecord = false;
                if (isModeFull()) { // used to be: getMode() == MODE_BASE
                    try {
                        LogbookRecord rlast = (LogbookRecord) logbook.data().getLast();
                        if (currentRecord.getEntryId().equals(rlast.getEntryId())) {
                            createNewRecord = true;
                        }
                    } catch (Exception eignore) {
                    }
                }
                if (createNewRecord) {
                    createNewRecord(false);
                } else {
                    entryno.requestFocus();
                }
            }
        } else {
            finishBoathouseAction(success);
        }
        
        return success;
    }

    private boolean isValidBoatNamePair(int iRow) {
    	try {
	    	ItemTypeStringAutoComplete[] acItem= (ItemTypeStringAutoComplete[])nameAndBoat.getItems(iRow);
	    	ItemTypeStringAutoComplete curName= acItem[0];
	    	ItemTypeStringAutoComplete curBoat = acItem[1];    	
	    	return (!(curName.getValue().trim().isEmpty() && curBoat.getValue().trim().isEmpty()));
    	} catch (Exception e) {
    		Logger.logdebug(e);
    		return false;
    	}
    }
    
    // den Datensatz nun wirklich speichern;
    protected boolean saveEntriesInLogbook() {
        if (!isLogbookReady()) {
            return false;
        }
        long lock = 0;
        Exception myE = null;

        try {
            
        	for (int iCurBoatNamePair=0; iCurBoatNamePair<nameAndBoat.getItemCount(); iCurBoatNamePair++) {
        		if (isValidBoatNamePair(iCurBoatNamePair)) {

                    LogbookRecord rlast = (LogbookRecord) logbook.data().getLast();
                    int nextEntry = rlast.getEntryId().intValue()+1;

                    entryno.setValue(""+nextEntry); // convert incremented last ID to string
                    isNewRecord = true; // always create new records
                    currentRecord = getFields();
                    
                    addBoatNamePair(currentRecord, iCurBoatNamePair);
                    
                    if (mode == MODE_BOATHOUSE_START || mode == MODE_BOATHOUSE_START_CORRECT || mode == MODE_BOATHOUSE_START_MULTISESSION ) {
                        currentRecord.setSessionIsOpen(true);
                    } else {
                        currentRecord.setSessionIsOpen(false); // all other updates to an open entry (incl. Admin Mode) will mark it as finished
                    }

                    logbook.data().add(currentRecord, lock);

                    if (isModeFull()) {
                        logAdminEvent(Logger.INFO, Logger.MSG_ADMIN_LOGBOOK_ENTRYADDED, International.getString("Eintrag hinzugefügt"), currentRecord);
                    }
                    // we need to do this here for any new session
                    updateBoatStatus(true, mode);
        		}
        	}
        	
        } catch (Exception e) {
            Logger.log(e);
            myE = e;
        } finally {
            if (lock != 0) {
                logbook.data().releaseGlobalLock(lock);
            }
        }

        currentRecord = null; // this is neccessary so that the updateBoatStatus invoked later does not try to add new boatstatus 
        
        if (myE != null) {
            Dialog.error(International.getString("Fahrtenbucheintrag konnte nicht gespeichert werden.") + "\n" + myE.toString());
            return false;
        }

        return true;
    }	
    
    private void addBoatNamePair (LogbookRecord theRecord, int iRow) {
    	try {
	    	ItemTypeStringAutoComplete[] acItem= (ItemTypeStringAutoComplete[])nameAndBoat.getItems(iRow);
	    	ItemTypeStringAutoComplete curName= acItem[0];
	    	ItemTypeStringAutoComplete curBoat = acItem[1];    	

	    	// handle Person
	    	PersonRecord p = findPerson(curName, getValidAtTimestamp(theRecord));

            if (p != null) {
            	theRecord.setCrewId(1, p.getId());
            	theRecord.setCrewName(1, null);
            } else {
                String s = curName.getValue().toString().trim();
            	theRecord.setCrewName(1, (s.length() == 0 ? null : s) );
            	theRecord.setCrewId(1, null);
            }	    	
	    	
            // handle Boat & Boat Variant
            BoatRecord b = findBoat(curBoat, getValidAtTimestamp(theRecord));
            if (b != null) {
            	theRecord.setBoatId(b.getId());
            	theRecord.setBoatVariant(getOneSeaterBoatVariant(b));
            	theRecord.setBoatName(null);
            } else {
                String s = curBoat.toString().trim();
                theRecord.setBoatName( (s.length() == 0 ? null : s) );
                theRecord.setBoatId(null);
                theRecord.setBoatVariant(IDataAccess.UNDEFINED_INT);
            }    	            
    	} catch (Exception e) {
    		Logger.logdebug(e);
    		return;
    	}    	
    }
    
    /**
     * Determine if a boat has at least a variant as a one-seater.
     * Does not check if the BoatRecord is valid at the current time.
     * 
     * @param boatRec BoatRecord (not null)
     * @return true if Boat has at least one variant as a One-Seater
     */
    private int getOneSeaterBoatVariant(BoatRecord boatRec) {
    	
        for (int iboatVariant=0; iboatVariant<boatRec.getNumberOfVariants(); iboatVariant++) {
            if (boatRec.getNumberOfSeats(iboatVariant)==1) {
            	return boatRec.getTypeVariant(iboatVariant);
            }
        }
        //none of the variants is a OneSeater
        return 0;
    }    
    
    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }    
    
    public void _keyAction(ActionEvent evt) {
        super._keyAction(evt);
    }
    
    /*
     * AutoComplete fields which are visible: try to autocomplete the item based on the data entered.
     */
    protected void autocompleteAllFields() {
        try {
        	
			if (destination.isVisible()) {
			    destination.acpwCallback(null);
			}
			
			if (waters.isVisible() ) {
				waters.acpwCallback(null);
			}
			
			for (int iCurNameAndBoat =0; iCurNameAndBoat<nameAndBoat.getItemCount(); iCurNameAndBoat++) {
				ItemTypeStringAutoComplete[] acItem= (ItemTypeStringAutoComplete[])nameAndBoat.getItems(iCurNameAndBoat);
				if (acItem[0].isVisible()) {
					acItem[0].acpwCallback(null);
					acItem[1].acpwCallback(null);
				}
			}
        } catch(Exception e) {       
        	Logger.log(e);
        }
    }

    /**
     * Checks all person names for correct naming.
     * @return true if all person Names ok
     */
    protected boolean checkMultiSessionMisspelledPersons() {
	    PersonRecord r;
	    // we go for all items 
	    for (int iCurNameAndBoat =0; iCurNameAndBoat<nameAndBoat.getItemCount(); iCurNameAndBoat++) {
	    	ItemTypeStringAutoComplete[] acItem= (ItemTypeStringAutoComplete[])nameAndBoat.getItems(iCurNameAndBoat);
	    	ItemTypeStringAutoComplete curName= acItem[0];
	    	
	        String s = curName.getValueFromField().trim();
	        if (s.length() == 0) {
	            continue;
	        }
	        r = findPerson(curName, getValidAtTimestamp(null));
	        if (r == null) {
	            // check for comma without blank
	            int pos = s.indexOf(",");
	            if (pos > 0 && pos+1 < s.length() && s.charAt(pos+1) != ' ') {
	                curName.parseAndShowValue(s.substring(0, pos) + ", " + s.substring(pos+1));
	            }
	        }
	    }
	    return true;
    }
    
    /**
     * Check if there are no duplicate Persons or Boats mentioned in the names/boat list.
     * 
     * Problem: does only work good if the boat names are identified correctly by the autocomplete lists.
     * 
     * @return true, if no duplicate boats/names are present.
     */
    protected boolean checkMultiSessionDuplicatePersonsAndBoats() {
        // Ruderer auf doppelte prüfen
        Hashtable<UUID,String> personHash = new Hashtable<UUID,String>();
        Hashtable<UUID,String> boatHash = new Hashtable<UUID,String>();
        
        String doppelt = null; // Ergebnis doppelt==null heißt ok, doppelt!=null heißt Fehler! ;-)
        Boolean isPersonDoppelt = false;
    	ItemTypeStringAutoComplete curName= null;
    	ItemTypeStringAutoComplete curBoat = null;
    	
        while (true) { // Unsauber; aber die Alternative wäre ein goto; dies ist keine Schleife!!

    	    for (int iCurNameAndBoat =0; iCurNameAndBoat<nameAndBoat.getItemCount(); iCurNameAndBoat++) {
    	    	ItemTypeStringAutoComplete[] acItem= (ItemTypeStringAutoComplete[])nameAndBoat.getItems(iCurNameAndBoat);
    	    	curName= acItem[0];
    	    	curBoat = acItem[1];
    	    	
    	    	//check for duplicate entry for person's name
    	        String s = curName.getValueFromField().trim();
    	        if (s.length() == 0) {
    	            continue;
    	        }
    	        PersonRecord pr = findPerson(curName, getValidAtTimestamp(null));
    	        if (pr != null) {
                    UUID id = pr.getId();
                    if (personHash.get(id) == null) {
                    	personHash.put(id, "");
                    } else {
                        doppelt = pr.getQualifiedName();
                        isPersonDoppelt=true;
                        break;
                    }
                }
    	        
    	        //check for duplicate Entry for boat's name
    	        s=curBoat.getValueFromField().trim();
    	        if (s.length() == 0) {
    	            continue;
    	        }
    	        //TODO: Boat name can only be found if entered in correct spelling, including uppercase/lowercase letters.
    	        BoatRecord br = findBoat(curBoat, getValidAtTimestamp(null));
    	        if (br != null) {
                    UUID id = br.getId();
                    if (boatHash.get(id) == null) {
                        boatHash.put(id, "");
                    } else {
                        doppelt = br.getQualifiedName();
                        isPersonDoppelt=false;
                        break;
                    }
                }
            }
            break; // alles ok, keine doppelten --> Pseudoschleife abbrechen
        }
        if (doppelt != null) {
            if (isPersonDoppelt) {
            	Dialog.error(International.getMessage("Die Person '{name}' wurde mehrfach eingegeben!", doppelt));
            	if (curName!= null) { curName.requestFocus(); }
            } else {
            	Dialog.error(International.getMessage("Das Boot '{name}' wurde mehrfach eingegeben!", doppelt));
            	if (curBoat!=null) { curBoat.requestFocus(); }
            }
            return false;
        }
        return true;
    }    
    
    
    /**
     * Checks if any of the boats mentioned are on the water, are not available or have an active reservation.
     * Most of the checks are done by efaBoathouseFrame.getStartSessionForBoat().
     * 
     * @return true if all checks were successful
     */
    private boolean checkMultiSessionBoatStatus() {
    	
	    for (int iCurNameAndBoat =0; iCurNameAndBoat<nameAndBoat.getItemCount(); iCurNameAndBoat++) {
	    	ItemTypeStringAutoComplete[] acItem= (ItemTypeStringAutoComplete[])nameAndBoat.getItems(iCurNameAndBoat);
	    	ItemTypeStringAutoComplete curBoat = acItem[1];    	
    	
	        if (getMode() == MODE_BOATHOUSE_START_MULTISESSION ) { // avoid if mode lateentry
	            int checkMode = 2;

    	        BoatRecord br = findBoat(curBoat, getValidAtTimestamp(null));
    	        if (br != null) {	            
	            
    	        	efaBoathouseAction.boat = br;
	
	                if (efaBoathouseAction.boat != null) {
		                // update boat status (may have changed since we opened the dialog)
		                efaBoathouseAction.boatStatus = efaBoathouseAction.boat.getBoatStatus();
		            }
		            boolean success = efaBoathouseFrame.checkStartSessionForBoat(efaBoathouseAction, "0", checkMode);
		            if (!success) {
		                efaBoathouseAction.boat = null; // otherwise next check would fail
		                curBoat.requestFocus();
		                return false;
		            }
		            
    	        }
	        }

	    }
	    return true;
 
    }
    
    private boolean checkMultiSessionNameAndBoatValuesValid() {
    	
        long preferredValidAt = getValidAtTimestamp(null);
    	
        for (int iCurNameAndBoat =0; iCurNameAndBoat<nameAndBoat.getItemCount(); iCurNameAndBoat++) {
	    	ItemTypeStringAutoComplete[] acItem= (ItemTypeStringAutoComplete[])nameAndBoat.getItems(iCurNameAndBoat);
	    	ItemTypeStringAutoComplete curName = acItem[0];
	    	ItemTypeStringAutoComplete curBoat = acItem[1];
	    	
	    	if (!checkBoatNameValid(curBoat)){
	    		curBoat.requestFocus();
	    		return false;
	    	}
	    	
	    	if (!checkUnknownNamesBoatDetail(curBoat)) {
	    		curBoat.requestFocus();
	    		return false;
	    	}
	    	
            String name = curName.getValueFromField();
            
            if (name != null && name.length() > 0) {
                PersonRecord r = findPerson(curName, preferredValidAt);
                if (r == null) {
                    r = findPerson(curName, -1);
                }
                if (preferredValidAt > 0 && r != null && !r.isValidAt(preferredValidAt)) {
                    if (!ingoreNameInvalid(r.getQualifiedName(), preferredValidAt,
                            International.getString("Person"), curName)) {
                    	curName.requestFocus();
                        return false;
                    }
                }
            }
            
            if (!checkUnknownNamesPersonDetail(curName)) {
            	return false;
            }
        }
        return true;
    }
    
    protected boolean checkUnknownNamesBoat() {
    	// this state is always true as it gets checked earlier 
    	// see checkUnknownNamesBoatDetail / checkMultiSessionNameAndBoatValuesValid
    	return true;
    }

    protected boolean checkUnknownNamesPerson() {
    	// this state is always true as it gets checked earlier 
    	// see checkUnknownNamesPersonDetail / checkMultiSessionNameAndBoatValuesValid
    	return true;
    }
    
    
    private boolean checkUnknownNamesBoatDetail(ItemTypeStringAutoComplete boatItem) {
        if (isModeBoathouse()) {
            if (Daten.efaConfig.getValueEfaDirekt_eintragNurBekannteBoote()) {
                String name = boatItem.getValueFromField();
                if (name != null && name.length() > 0 && findBoat(boatItem, getValidAtTimestamp(null)) == null) {
                    Dialog.error(LogString.itemIsUnknown(name, International.getString("Boot")));
                    boatItem.requestFocus();
                    return false;
                }
            }
        }
        return true;
    	
    }

    private boolean checkUnknownNamesPersonDetail(ItemTypeStringAutoComplete personItem) {
        // Prüfen, ob ggf. nur bekannte Boote/Ruderer/Ziele eingetragen wurden
        if (isModeBoathouse()) {

            if (Daten.efaConfig.getValueEfaDirekt_eintragNurBekannteRuderer()) {
                String name = personItem.getValueFromField();
                if (name != null && name.length() > 0 && findPerson(personItem, getValidAtTimestamp(null)) == null) {
                	Dialog.error(LogString.itemIsUnknown(name, International.getString("Person")));
                	personItem.requestFocus();
                	return false;
                }
            }
        }
        return true;
    }    
    
    /**
     * Checks all cox and crew fields for
     * * correct format of a name
     * * invalid content
     * @return true if all checks were successfully run.
     */
    protected boolean checkProperUnknownNames() {
        // check whether all names of unkown persons are proper and allowed names
        if (isModeBoathouse()) {
            String[] list = createListOfInvalidContent();
            Pattern pname = createPNamePattern();
            Pattern pnameadd = createPNameAddPattern(); 
            Pattern pnameclub = createPNameClubPattern();
            Pattern pnameaddclub = createPNameAddClubPattern();
            
            for (int iCurNameAndBoat =0; iCurNameAndBoat<nameAndBoat.getItemCount(); iCurNameAndBoat++) {
    	    	ItemTypeStringAutoComplete[] acItem= (ItemTypeStringAutoComplete[])nameAndBoat.getItems(iCurNameAndBoat);
    	    	ItemTypeStringAutoComplete curName = acItem[0];
    	    	
                String name = curName.getValueFromField();
                if (name != null && name.length() > 0 && findPerson(curName, getValidAtTimestamp(null)) == null) {
                    if (Daten.efaConfig.getValueBoathouseStrictUnknownPersons()) {
                        String _name = name;
                        name = beautifyNameField(name);
                        if (!name.equals(_name)) {
                            curName.parseAndShowValue(name);
                        }
                        if (!checkNameFormat(name, pname, pnameadd,pnameclub, pnameaddclub, curName)) {
                        	curName.requestFocus();
                        	return false;
                        };
                    }
                    if (!checkNameForInvalidContent(name, list, curName)) {
                    	return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Checks for each Name/Boat pair whether the Person has a BoatUsageBan, Boat may be used by that person,
     * or the single person is sufficient to use the boat.
     * @return true if checks were okay
     */
    protected boolean checkAllowedPersonsForBoat() {
        if (mode == MODE_BOATHOUSE_START || mode == MODE_BOATHOUSE_START_CORRECT || mode == MODE_BOATHOUSE_START_MULTISESSION) {
	        for (int iCurNameAndBoat =0; iCurNameAndBoat<nameAndBoat.getItemCount(); iCurNameAndBoat++) {
		    	ItemTypeStringAutoComplete[] acItem= (ItemTypeStringAutoComplete[])nameAndBoat.getItems(iCurNameAndBoat);
		    	ItemTypeStringAutoComplete curName = acItem[0];
		    	ItemTypeStringAutoComplete curBoat = acItem[1];
	
		    	if (!curBoat.getValue().isEmpty() && !curName.getValue().isEmpty()) {
			    	if (!checkBoatUsageBan(curBoat, curName)) {
			    		return false;
			    	}
			    	if (!checkMinPersonsInBoat(curBoat, curName)) {
			    		return false;
			    	}
		    	}
	        }
        }
        return true;
    }
    
    /**
     * Checks for the Name/Boat pair if the person has a BoatUsageBand, or the Boat may not be used by that person
     * @param theBoat
     * @param theName
     * @return true if checks were okay
     */
    protected boolean checkBoatUsageBan(ItemTypeStringAutoComplete theBoat, ItemTypeStringAutoComplete theName) {
        
	    Groups groups = Daten.project.getGroups(false);
	    long tstmp = System.currentTimeMillis();
	    
	    BoatRecord curBoat = findBoat(theBoat, tstmp);
        PersonRecord curPerson = findPerson(theName, tstmp);
        
        if (curBoat == null) {
        	return true;
        }

        DataTypeList<UUID> groupIdList = curBoat.getAllowedGroupIdList();
        if (groupIdList != null && groupIdList.length() > 0) {
            String nichtErlaubt = null;
            int nichtErlaubtAnz = 0;
            String ptext = theName.getValue().toString();
            
            // if a known person is set, check for usage ban
            if (curPerson != null) { 
	            if (curPerson.getBoatUsageBan()) {
	                switch (Dialog.auswahlDialog(International.getString("Bootsbenutzungs-Sperre"),
	                    International.getMessage("Für {name} liegt zur Zeit eine Bootsbenutzungs-Sperre vor.", curPerson.getQualifiedName()) +
	                    "\n" +
	                    International.getString("Was möchtest Du tun?"),
	                    International.getString("Mannschaft ändern"),
	                    International.getString("Trotzdem benutzen"))) {
	                case 0:
	                    theName.requestFocus();
	                    return false;
	                    
	                case 1:
	                    break; // we want to proceed with the rest of the code
	                    
	                default: //default when the user hits VK_ESCAPE: change crew
	                    theName.requestFocus();
	                    return false;
	                    	
	                }
	            }
            }
            //check if the person is in any group the boat may be used by
            boolean inAnyGroup = false;
            if (curPerson!=null) {
	            for (int j = 0; j < groupIdList.length(); j++) {
	                GroupRecord g = groups.findGroupRecord(groupIdList.get(j), tstmp);
	                if (g != null && g.getMemberIdList() != null && g.getMemberIdList().contains(curPerson.getId())) {
	                    inAnyGroup = true;
	                    break;
	                }
	            }
            }
            
            if (!inAnyGroup) {
                String name = (curPerson != null ? curPerson.getQualifiedName() : ptext);
                nichtErlaubt = (nichtErlaubt == null ? name : nichtErlaubt + "\n" + name);
                nichtErlaubtAnz++;

            }
            
            // a boat may be assigned to a group, but may need a participant of another
            // like used by a special crew, but needs at least a member of group "trainers"
            if (Daten.efaConfig.getValueCheckAllowedPersonsInBoat() &&
                nichtErlaubtAnz > 0 &&
                nichtErlaubtAnz > curBoat.getMaxNotInGroup()) {
                String erlaubteGruppen = null;
                for (int j = 0; j < groupIdList.length(); j++) {
                    GroupRecord g = groups.findGroupRecord(groupIdList.get(j), tstmp);
                    String name = (g != null ? g.getName() : null);
                    if (name == null) {
                        continue;
                    }
                    erlaubteGruppen = (erlaubteGruppen == null ? name : erlaubteGruppen + (j + 1 < groupIdList.length() ? ", " + name : " "
                            + International.getString("und") + " " + name));
                }
                switch (Dialog.auswahlDialog(International.getString("Boot nur für bestimmte Gruppen freigegeben"),
                        International.getMessage("Das Boot [{boatname}] dürfen nur die Gruppen [{list_of_valid_groups}] nutzen.", theBoat.getValue().toString(), erlaubteGruppen) + "\n"
                        + International.getString("Folgende Personen gehören keiner der Gruppen an und dürfen das Boot nicht benutzen:") + " \n"
                        + nichtErlaubt + "\n"
                        + International.getString("Was möchtest Du tun?"),
                        International.getString("Anderes Boot wählen"),
                        International.getString("Mannschaft ändern"),
                        International.getString("Trotzdem benutzen"))) {
                    case 0:
                    	theBoat.requestFocus();
                        return false;
                    case 1:
                        theName.requestFocus();
                        return false;
                    case 2:
                    	logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_UNALLOWEDBOATUSAGE,
                                          International.getString("Unerlaubte Benutzung eines Bootes"),
                                          theBoat.getValue().toString(), theName.getValue().toString());
                        break;
                    default: //default when the user hits VK_ESCAPE: change crew
                        theName.requestFocus();
                        return false;                            
                }
            }
        }
        return true;
    }
    
    /**
     * Checks if the single person suffices to use the boat, if the boat is assigned to groups.
     * @param theBoat
     * @param theName
     * @return true if check is ok
     */
    protected boolean checkMinPersonsInBoat(ItemTypeStringAutoComplete theBoat, ItemTypeStringAutoComplete theName) {
    	
	    Groups groups = Daten.project.getGroups(false);
	    long tstmp = System.currentTimeMillis();
	    
	    BoatRecord curBoat = findBoat(theBoat, tstmp);
        PersonRecord curPerson = findPerson(theName, tstmp);
        
        if (curBoat == null) {
        	return true;
        }
    	
        // Prüfen, ob mind 1 Ruderer (oder Stm) der Gruppe "mind 1 aus Gruppe" im Boot sitzt
        if (Daten.efaConfig.getValueCheckMinOnePersonsFromGroupInBoat() &&
            curBoat.getRequiredGroupId() != null) {
            GroupRecord g = groups.findGroupRecord(curBoat.getRequiredGroupId(), tstmp);
            boolean found = false;
            if (curPerson != null) {
	            if (g != null && g.getMemberIdList() != null) {
	               found = g.getMemberIdList().contains(curPerson.getId());
	            }
            }
            
            if (g != null && !found) {
                switch (Dialog.auswahlDialog(International.getString("Boot erfordert bestimmte Berechtigung"),
                        International.getMessage("In dem Boot [{boatname}] muß mindestens ein Mitglied der Gruppe [{groupname}] sitzen.", theBoat.getValue().toString(), g.getName()) + "\n"
                        + International.getString("Was möchtest Du tun?"),
                        International.getString("Anderes Boot wählen"),
                        International.getString("Mannschaft ändern"),
                        International.getString("Trotzdem benutzen")
                        )) {
                    case 0:
                        theBoat.requestFocus();
                        return false;
                    case 1:
                        theName.requestFocus();
                        return false;
                    case 2:
                        logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_UNALLOWEDBOATUSAGE,
                                          International.getString("Unerlaubte Benutzung eines Bootes"),
                                          theBoat.getValue().toString(), theName.getValue().toString());
                        break;
                        
                    default: //default when the user hits VK_ESCAPE: change crew
                        theName.requestFocus();
                        return false;                        	
                }
            }
        }
        return true;
    }
    
    protected boolean checkAllDataEnteredBoatAndCrew() {
        if (isModeBoathouse()) {
        	// check if for each line either name AND boat are set.

            for (int iCurNameAndBoat =0; iCurNameAndBoat<nameAndBoat.getItemCount(); iCurNameAndBoat++) {
    	    	ItemTypeStringAutoComplete[] acItem= (ItemTypeStringAutoComplete[])nameAndBoat.getItems(iCurNameAndBoat);
    	    	ItemTypeStringAutoComplete curName = acItem[0];
    	    	ItemTypeStringAutoComplete curBoat = acItem[1];
    	    	
    	    	if (!(curName.getValue().trim().isEmpty() && curBoat.getValue().trim().isEmpty())) { 
    	    		// both fields are not empty?
    	    		if (curName.getValue().trim().isEmpty()) {
    	                Dialog.error(International.getString("Bitte trage mindestens eine Person ein!"));
    	                curName.requestFocus();
    	                return false;
    	    		}

    	    		if (curBoat.getValue().trim().isEmpty()) {
    	                Dialog.error(International.getString("Bitte gib einen Bootsnamen ein!"));
    	                curBoat.requestFocus();
    	                return false;
    	    		}
    	    	}
            }
        }
        return true;
    }
    
    /**
     * this method is called from EfaBaseFrame.getfields()
     * for multisession items, we simply set the boat to empty, because
     * it is set to the actual name/boat pair later.
     */
    protected void getFieldsForBoats(LogbookRecord theRecord) {
    	theRecord.setBoatId(null);
    	theRecord.setBoatName(null);
        theRecord.setBoatVariant(IDataAccess.UNDEFINED_INT);
    }
    
    protected void getFieldsForCrew(LogbookRecord theRecord) {
        // Cox and Crew
        for (int i=0; i<=LogbookRecord.CREW_MAX; i++) {
            if (i == 0) {
            	theRecord.setCoxName(null);
            	theRecord.setCoxId(null);
            } else {
            	theRecord.setCrewName(i, null);
            	theRecord.setCrewId(i, null);
            }
        }
      	theRecord.setBoatCaptainPosition(IDataAccess.UNDEFINED_INT);
    }
    
    protected void getSessionGroupID(LogbookRecord theRecord) {
    	// do nothing, field does not need to be set.
    }
}
