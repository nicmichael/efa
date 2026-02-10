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
import de.nmichael.efa.core.AdminTask;
import de.nmichael.efa.core.config.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.efacloud.TxRequestQueue;
import de.nmichael.efa.data.types.*;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.gui.util.*;
import de.nmichael.efa.gui.dataedit.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.util.Dialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This is the efaBaseFrame.
 * It displays all fields for a session. It is used for
 * - efaBase 
 * 	 - main screen, 
 * 	 - with toolbar and menu bar (for all other admin options) 
 *   - for showing/editing sessions)
 *
 * - efaBoatHouse 
 *   - in Main mode (Kiosk): separate dialog screen
 *   	- starting/correcting/finishing/lateentry and aborting sessions
 *   - in admin mode : 
 *   	- separate dialog screen
 *    	- with toolbar
 *
 * It has a subclass EfaBaseFrameMultisession, which handles
 * START_MULTIPLE and LATEENTRY_MULTIPLE for sessions.
 * 
 * The reason for building a subclass for Multisession handling is
 * that this class is quite big, and a lot of code is used by both 
 * single-session and multi-session dialogs.
 * 
 * Due to the subclass, a bunch of methods and fields need to have a "protected" visibility.
 * 
 */
public class EfaBaseFrame extends BaseDialog implements IItemListener {

	private static final long serialVersionUID = -2347795960441110254L;
	
    public static final int MODE_BASE = 0;
    public static final int MODE_BOATHOUSE = 1;
    public static final int MODE_BOATHOUSE_START = 2;
    public static final int MODE_BOATHOUSE_START_CORRECT = 3;
    public static final int MODE_BOATHOUSE_FINISH = 4;
    public static final int MODE_BOATHOUSE_LATEENTRY = 5;
    public static final int MODE_BOATHOUSE_ABORT = 6;
    public static final int MODE_ADMIN = 7;
    public static final int MODE_ADMIN_SESSIONS = 8;
    public static final int MODE_BOATHOUSE_START_MULTISESSION=9;
    public static final int MODE_BOATHOUSE_LATEENTRY_MULTISESSION=10;
    protected int mode;
    protected static final int VERTICAL_WHITESPACE_PADDING_GROUPS=26;

    protected static final int FIELD_HEIGHT=21;
    // =========================================================================
    // GUI Elements
    // =========================================================================

    // Menu Bar
    JMenuBar menuBar = new JMenuBar();

    // Toolbar
    JToolBar toolBar = new JToolBar();
    JButton toolBar_firstButton = new JButton();
    JButton toolBar_prevButton = new JButton();
    JButton toolBar_nextButton = new JButton();
    JButton toolBar_lastButton = new JButton();
    JButton toolBar_newButton = new JButton();
    // @remove JButton toolBar_insertButton = new JButton();
    JButton toolBar_deleteButton = new JButton();
    JButton toolBar_searchButton = new JButton();
    JTextField toolBar_goToEntry = new JTextField();
    JButton toolBar_goToEntryNext = new JButton();

    // Data Fields
    ItemTypeString entryno;
    ItemTypeLabel opensession;
    ItemTypeButton closesessionButton;
    ItemTypeDate date;
    ItemTypeDate enddate;
    ItemTypeStringAutoComplete boat;
    ItemTypeStringList boatvariant;
    ItemTypeStringAutoComplete cox;
    ItemTypeStringAutoComplete[] crew;
    ItemTypeStringList boatcaptain;
    ItemTypeTime starttime;
    ItemTypeTime endtime;
    ItemTypeLabel starttimeInfoLabel;
    ItemTypeLabel endtimeInfoLabel;
    ItemTypeStringAutoComplete destination;
    ItemTypeString destinationInfo;
    ItemTypeStringAutoComplete waters;
    ItemTypeDistance distance;
    ItemTypeString comments;
    ItemTypeStringList sessiontype;
    ItemTypeLabel sessionTypeInfo;
    ItemTypeStringAutoComplete sessiongroup;
    public static final String GUIITEM_ADDITIONALWATERS = "GUIITEM_ADDITIONALWATERS";
    String sessionTypeInfoText = null;

    // Supplementary Elements
    ItemTypeButton remainingCrewUpButton;
    ItemTypeButton remainingCrewDownButton;
    ItemTypeButton boatDamageButton;
    ItemTypeButton boatNotCleanedButton;
    ItemTypeButton saveButton;
    JLabel infoLabel = new JLabel();
    String KEYACTION_F3;
    String KEYACTION_F4;

    // Internal Data Structures
    Logbook logbook;                // this logbook
    AdminRecord admin;
    AdminRecord remoteAdmin;
    DataKeyIterator iterator;       // iterator for this logbook
    LogbookRecord currentRecord;    // aktDatensatz = aktuell angezeigter Datensatz
    LogbookRecord referenceRecord;  // refDatensatz = Referenz-Datensatz (zuletzt angezeigter Datensatz, wenn neuer erstellt wird)
    long logbookValidFrom = 0;
    long logbookInvalidFrom = 0;
    boolean isNewRecord;            // neuerDatensatz = ob akt. Datensatz ein neuer Datensatz, oder ein bearbeiteter ist
    boolean isInsertedRecord;       // neuerDatensatz_einf = ob der neue Datensatz eingefügt wird (dann beim Hinzufügen keine Warnung wegen kleiner Lfd. Nr.!)
    int entryNoForNewEntry = -1;    // lfdNrForNewEntry = LfdNr (zzgl. 1), die für den nächsten per "Neu" erzeugten Datensatz verwendet werden soll; wenn <0, dann wird "last+1" verwendet
    BoatRecord currentBoat;         // aktBoot = aktuelle Bootsdaten (um nächstes Eingabefeld zu ermitteln)
    String currentBoatTypeSeats;    // boat type for currentBoat
    String currentBoatTypeCoxing;   // boat type for currentBoat
    int currentBoatNumberOfSeats;   // boat type for currentBoa
    String lastDestination = "";    // zum Vergleichen, ob Ziel geändert wurde
    int crewRangeSelection = 0;     // mannschAuswahl = 0: 1-8 sichtbar; 1: 9-16 sichtbar; 2: 17-24 sichtbar
    String crew1defaultText = null; // mannsch1_label_defaultText = der Standardtext, den das Label "Mannschaft 1: " normalerweise haben soll (wenn es nicht für Einer auf "Name: " gesetzt wird)
    IItemType lastFocusedItem;
    private volatile boolean _inUpdateBoatVariant = false;
    AutoCompleteList autoCompleteListBoats = new AutoCompleteList();
    AutoCompleteList autoCompleteListPersons = new AutoCompleteList();
    AutoCompleteList autoCompleteListDestinations = new AutoCompleteList();
    AutoCompleteList autoCompleteListWaters = new AutoCompleteList();
    EfaBaseFrameFocusManager efaBaseFrameFocusManager;
    String _jumpToEntryNo;
    boolean showEditWaters = false; // allow to edit waters per session even if disabled in EfaConfig

    // Internal Data Structures for EfaBoathouse
    EfaBoathouseFrame efaBoathouseFrame;
    AdminDialog adminDialog;
    ItemTypeBoatstatusList.BoatListItem efaBoathouseAction;
    BoatStatusRecord correctSessionLastBoatStatus;
    int positionX,positionY;      // Position des Frames, wenn aus efaDirekt aufgerufen


    public EfaBaseFrame(int mode) {
        super((JFrame)null, Daten.EFA_LONGNAME);
        this.mode = mode;
    }

    public EfaBaseFrame(JDialog parent, int mode) {
        super(parent, Daten.EFA_LONGNAME, null);
        this.mode = mode;
    }

    public EfaBaseFrame(JDialog parent, int mode, AdminRecord admin,
            Logbook logbook, String entryNo) {
        super(parent, Daten.EFA_LONGNAME, null);
        this.mode = mode;
        this.logbook = logbook;
        this.admin = admin;
        this._jumpToEntryNo = entryNo;
        this.showEditWaters = true;
    }

    public EfaBaseFrame(EfaBoathouseFrame efaBoathouseFrame, int mode) {
        super(efaBoathouseFrame, Daten.EFA_LONGNAME, null);
        this.efaBoathouseFrame = efaBoathouseFrame;
        this.mode = mode;
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public boolean isModeBase() {
        return getMode() == MODE_BASE;
    }

    public boolean isModeFull() {
        return getMode() == MODE_BASE ||
               getMode() == MODE_ADMIN;
    }

    public boolean isModeBoathouse() {
        return getMode() == MODE_BOATHOUSE ||
               getMode() == MODE_BOATHOUSE_START ||
               getMode() == MODE_BOATHOUSE_START_CORRECT ||
               getMode() == MODE_BOATHOUSE_START_MULTISESSION ||
               getMode() == MODE_BOATHOUSE_FINISH ||
               getMode() == MODE_BOATHOUSE_LATEENTRY ||
               getMode() == MODE_BOATHOUSE_LATEENTRY_MULTISESSION ||
               getMode() == MODE_BOATHOUSE_ABORT;
    }

    public boolean isModeAdmin() {
        return getMode() == MODE_ADMIN ||
               getMode() == MODE_ADMIN_SESSIONS;
    }

    public AdminRecord getAdmin() {
        return (remoteAdmin != null ? remoteAdmin : admin);
    }

    private void checkRemoteAdmin() {
        boolean error = false;
        if (remoteAdmin == null && Daten.project != null &&
                Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
            error = true;
            Dialog.error(International.getString("Login fehlgeschlagen") + ".\n" +
                    International.getString("Bitte überprüfe Remote-Adminnamen und Paßwort in den Projekteinstellungen."));
        }
        if (remoteAdmin != null && !remoteAdmin.isAllowedRemoteAccess()) {
            error = true;
            EfaMenuButton.insufficientRights(remoteAdmin,
                    International.getString("Remote-Zugriff über efaRemote"));
        }
        if (error) {
            Daten.project = null;
            remoteAdmin = null;
        } else {
            // enable some local functions for the admin
            if (remoteAdmin != null) {
                remoteAdmin.setAllowedShowLogfile( (admin != null && admin.isAllowedShowLogfile()) );
            }
        }
    }

    public void _keyAction(ActionEvent evt) {
    	//F3 is the key to search (again) in the logbook.
    	//this makes sens for efaBase where the main screen is the edit screen for logbook items,
    	//and also in admin mode of efaBths. But not in other dialogs which derive from efaBaseFrame.
    	
    	//isModeFull is true if we are running within efabase, oder admin mode is active
    	//fixed Git issue Git#171/EFA#87 
    	
        if (evt.getActionCommand().equals(KEYACTION_F3)  && isModeFull()) {
            SearchLogbookDialog.search();
        }
        if (evt.getActionCommand().equals(KEYACTION_F4)) {
            if (currentBoat != null && currentBoat.getDefaultCrewId() != null) {
                setDefaultCrew(currentBoat.getDefaultCrewId());
            }
        }
        super._keyAction(evt);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    public void packFrame(String method) {
        this.pack();
    }

    public void setFixedLocationAndSize() {
        Dialog.setDlgLocation(this);
        Dimension dlgSize = getSize();
        setMinimumSize(dlgSize);
        setMaximumSize(dlgSize);
    }
    
    protected void iniDialog() {
        if (isModeBase() && admin == null) {
            iniAdmin();
        }
        iniGuiBase();
        if (isModeBase()) {
            iniGuiMenu();
        }
        if (isModeFull()) {
            iniGuiToolbar();
        }
        iniGuiMain();
        iniGuiRemaining();
        iniApplication();
        if (isModeBase()) {
            Daten.iniSplashScreen(false);
        }
        AdminTask.startAdminTask(admin, this);
    }

    public void setAdmin(AdminRecord admin) {
        this.admin = admin;
    }

    private void iniAdmin() {
        if (admin == null) {
            admin = AdminLoginDialog.login(null, Daten.APPLNAME_EFA,
                    true, Daten.efaConfig.getValueLastProjectEfaBase());
            if (admin == null || !admin.isAllowedAdministerProjectLogbook()) {
                if (admin != null) {
                    EfaMenuButton.insufficientRights(admin,
                            International.getString("Projekte und Fahrtenbücher administrieren"));
                }
                super.cancel();
                Daten.haltProgram(Daten.HALT_ADMINLOGIN);
            }
            String p = AdminLoginDialog.getLastSelectedProject();
            if (p != null && p.length() > 0) {
                Daten.efaConfig.setValueLastProjectEfaBase(p);
            }
        }
        Daten.checkRegister();
    }

    protected void iniGuiBase() {
        setIconImage(Toolkit.getDefaultToolkit().createImage(EfaBaseFrame.class.getResource("/de/nmichael/efa/img/efa_icon.png")));
        mainPanel.setLayout(new BorderLayout());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                this_windowClosing(e);
            }
            public void windowIconified(WindowEvent e) {
                this_windowIconified(e);
            }
        });
        KEYACTION_F3 = addKeyAction("F3");
        KEYACTION_F4 = addKeyAction("F4");
    }

    /**
     * Initializes the menu in efaBaseFrame.
     * Menu is only visible in efaBase mode.
     */
    private void iniGuiMenu() {
        if (!isModeBase()) {
            return;
        }
        Vector<EfaMenuButton> menuButtons = EfaMenuButton.getAllMenuButtons(
                getAdmin(), false);
        String lastMenuName = null;
        menuBar.removeAll();
        JMenu menu = null;
        for (EfaMenuButton menuButton : menuButtons) {
            if (!menuButton.getMenuName().equals(lastMenuName)) {
                if (menu != null) {
                    menuBar.add(menu);
                }
                // New Menu
                menu = new JMenu();
                Mnemonics.setButton(this, menu, menuButton.getMenuText());
                lastMenuName = menuButton.getMenuName();
            }
            if (menuButton.getButtonName().equals(EfaMenuButton.SEPARATOR)) {
                menu.addSeparator();
            } else {
                JMenuItem menuItem = new JMenuItem();
                Mnemonics.setMenuButton(this, menuItem, menuButton.getButtonText());
                if (menuButton.getIcon() != null) {
                    setIcon(menuItem, menuButton.getIcon());
                }
                menuItem.setActionCommand(menuButton.getButtonName());
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        menuActionPerformed(e);
                    }
                });
                menu.add(menuItem);
            }
        }
        menuBar.add(menu);
        this.setJMenuBar(menuBar);
    }

    /**
     * Initializes the toolbar in efaBaseFrame.
     * The toolbar is only visible in efaBase or in the logbook editor
     * in admin mode.
     */
    private void iniGuiToolbar() {
        boolean useText = false;

        toolBar_firstButton.setMargin(new Insets(2, 3, 2, 3));
        Mnemonics.setButton(this, toolBar_firstButton, 
                (useText ? International.getStringWithMnemonic("Erster") : null),
                BaseDialog.IMAGE_FIRST);
        toolBar_firstButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navigateInLogbook(Integer.MIN_VALUE);
            }
        });

        toolBar_prevButton.setMargin(new Insets(2, 3, 2, 3));
        Mnemonics.setButton(this, toolBar_prevButton, 
                (useText ? International.getStringWithMnemonic("Vorheriger") : null),
                BaseDialog.IMAGE_PREV);
        toolBar_prevButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navigateInLogbook(-1);
            }
        });

        toolBar_nextButton.setMargin(new Insets(2, 3, 2, 3));
        Mnemonics.setButton(this, toolBar_nextButton, 
                (useText ? International.getStringWithMnemonic("Nächster") : null),
                BaseDialog.IMAGE_NEXT);
        toolBar_nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navigateInLogbook(1);
            }
        });

        toolBar_lastButton.setMargin(new Insets(2, 3, 2, 3));
        Mnemonics.setButton(this, toolBar_lastButton, 
                (useText ? International.getStringWithMnemonic("Letzter") : null),
                BaseDialog.IMAGE_LAST);
        toolBar_lastButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navigateInLogbook(Integer.MAX_VALUE);
            }
        });

        toolBar_newButton.setMargin(new Insets(2, 3, 2, 3));
        Mnemonics.setButton(this, toolBar_newButton, 
                (useText ? International.getStringWithMnemonic("Neu") : null),
                BaseDialog.IMAGE_ADD);
        toolBar_newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createNewRecord(false);
            }
        });

        toolBar_deleteButton.setMargin(new Insets(2, 3, 2, 3));
        Mnemonics.setButton(this, toolBar_deleteButton, 
                (useText ? International.getStringWithMnemonic("Löschen") : null),
                BaseDialog.IMAGE_DELETE);
        toolBar_deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteRecord();
            }
        });

        toolBar_searchButton.setMargin(new Insets(2, 3, 2, 3));
        Mnemonics.setButton(this, toolBar_searchButton, 
                (useText ? International.getStringWithMnemonic("Suchen") : null),
                BaseDialog.IMAGE_SEARCH);
        toolBar_searchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchLogbook();
            }
        });

        Dialog.setPreferredSize(toolBar_goToEntry, 30, FIELD_HEIGHT);
        toolBar_goToEntry.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                goToEntry(toolBar_goToEntry.getText().trim(), false);
            }
        });
        toolBar_goToEntry.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String s = toolBar_goToEntry.getText().trim();
                if (s.length() > 0 && Character.isDigit(s.charAt(0))) {
                    toolBar_goToEntry.setText("");
                }
            }
        });

        toolBar_goToEntryNext.setMargin(new Insets(2, 3, 2, 3));
        toolBar_goToEntryNext.setToolTipText(International.getString("Nächsten Eintrag suchen (F3)"));
        Mnemonics.setButton(this, toolBar_goToEntryNext,
                null,
                BaseDialog.IMAGE_SEARCHNEXT);
        toolBar_goToEntryNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                goToEntry(toolBar_goToEntry.getText().trim(), true);
            }
        });
        

        toolBar.add(toolBar_firstButton, null);
        toolBar.add(toolBar_prevButton, null);
        toolBar.add(toolBar_nextButton, null);
        toolBar.add(toolBar_lastButton, null);
        JLabel toolBar_spaceLabel1 = new JLabel();
        toolBar_spaceLabel1.setText("  ");
        toolBar.add(toolBar_spaceLabel1, null);
        toolBar.add(toolBar_newButton, null);
        toolBar.add(toolBar_deleteButton, null);
        JLabel toolBar_spaceLabel2 = new JLabel();
        toolBar_spaceLabel2.setText("  ");
        toolBar.add(toolBar_spaceLabel2, null);
        toolBar.add(toolBar_searchButton, null);
        JLabel toolBar_goToEntryLabel = new JLabel();
        //toolBar_goToEntryLabel.setText("  \u21B7 "); // \u00BB \u23E9 // EFA#87: this character is not available in all fonts - replaced by an icon.
        toolBar_goToEntryLabel.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_BUTTON_JUMP_TO));
        toolBar_goToEntryLabel.setText(" ");//toolBar_goToEntryLabel.setVerticalAlignment(SwingConstants.CENTER);
        toolBar_goToEntryLabel.setToolTipText(International.getString("Schnellsuche: zum nächsten Eintrag springen, der den folgenden Text enthält"));
        toolBar.add(toolBar_goToEntryLabel, null);
        toolBar.add(toolBar_goToEntry, null);
        toolBar.add(toolBar_goToEntryNext, null);
        

        mainPanel.add(toolBar, BorderLayout.NORTH);
    }

    /**
     * Add all fields for editing a session.
     * The initial values for the fields are set in efaBoatHouseFrame
     */
    protected void iniGuiMain() {
        JPanel mainInputPanel = new JPanel();
        mainInputPanel.setLayout(new GridBagLayout());
        mainPanel.add(mainInputPanel, BorderLayout.CENTER);

        // EntryNo
        entryno = new ItemTypeString(LogbookRecord.ENTRYID, "", IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Lfd. Nr."));
        entryno.setAllowedRegex("[0-9]+[A-Z]?");
        entryno.setToUpperCase(true);
        entryno.setNotNull(true);
        entryno.setFieldSize(200, FIELD_HEIGHT);
        entryno.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        entryno.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        entryno.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        entryno.displayOnGui(this, mainInputPanel, 0, 0);
        entryno.registerItemListener(this);

        // Open Session
        opensession = new ItemTypeLabel(LogbookRecord.OPEN, IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Fahrt offen"));
        opensession.setColor(Color.red);
        opensession.setFieldGrid(4, 1, -1, -1);
        opensession.displayOnGui(this, mainInputPanel, 5, 0);
        opensession.setVisible(false);
        
        closesessionButton = new ItemTypeButton("CloseSessionButton", IItemType.TYPE_PUBLIC, null, 
                International.getStringWithMnemonic("Fahrt offen") + " - " +
                International.getStringWithMnemonic("jetzt beenden"));
        closesessionButton.setColor(Color.red);
        closesessionButton.setFieldSize(50, FIELD_HEIGHT);
        closesessionButton.setFieldGrid(4, 1, -1, -1);
        closesessionButton.displayOnGui(this, mainInputPanel, 5, 0);
        closesessionButton.registerItemListener(this);
        closesessionButton.setVisible(false);

        // Date
        date = new ItemTypeDate(LogbookRecord.DATE, new DataTypeDate(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Datum"));
        date.showWeekday(true);
        date.setFieldSize(100, FIELD_HEIGHT);
        date.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        date.setFieldGrid(1, GridBagConstraints.WEST, GridBagConstraints.NONE);
        date.setWeekdayGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        date.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        date.displayOnGui(this, mainInputPanel, 0, 1);
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
        enddate.displayOnGui(this, mainInputPanel, 4, 1);
        enddate.registerItemListener(this);
        if (isModeBoathouse() && !Daten.efaConfig.getValueAllowEnterEndDate()) {
            enddate.setVisible(false);
        }

        // Boat
        boat = new ItemTypeStringAutoComplete(LogbookRecord.BOATNAME, "", IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Boot"), true);
        boat.setFieldSize(200, FIELD_HEIGHT);
        boat.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        boat.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.BOTH);
        boat.setAutoCompleteData(autoCompleteListBoats);
        boat.setChecks(true, true);
        boat.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        boat.displayOnGui(this, mainInputPanel, 0, 2);
        boat.registerItemListener(this);

        // Boat Variant
        boatvariant = new ItemTypeStringList(LogbookRecord.BOATVARIANT, "",
                null, null,
                IItemType.TYPE_PUBLIC, null, International.getString("Variante"));
        boatvariant.setFieldSize(80, FIELD_HEIGHT);
        boatvariant.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        boatvariant.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        boatvariant.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        boatvariant.displayOnGui(this, mainInputPanel, 0, 3);
        //boatvariant.displayOnGui(this, mainInputPanel, 5, 2);
        boatvariant.registerItemListener(this);

        // Cox
        cox = new ItemTypeStringAutoComplete(LogbookRecord.COXNAME, "", IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Steuermann"), true);
        cox.setFieldSize(200, FIELD_HEIGHT);
        cox.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        cox.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        cox.setAutoCompleteData(autoCompleteListPersons, true);
        cox.setChecks(true, true);
        cox.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        cox.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);
        cox.displayOnGui(this, mainInputPanel, 0, 4);
        cox.registerItemListener(this);


        // Crew
        crew = new ItemTypeStringAutoComplete[LogbookRecord.CREW_MAX];
        for (int i=1; i<=crew.length; i++) {
            int j = i-1;
            boolean left = ((j/4) % 2) == 0;
            crew[j] = new ItemTypeStringAutoComplete(LogbookRecord.getCrewFieldNameName(i), "", IItemType.TYPE_PUBLIC, null,
                    (i == 1 ? International.getString("Mannschaft") + " " : (i < 10 ? "  " :"")) + Integer.toString(i), true);
            crew[j].setPadding( (left ? 0 : 10), 0, 0, 0);
            crew[j].setFieldSize(200, FIELD_HEIGHT);
            crew[j].setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
            crew[j].setFieldGrid((left ? 2 : 3), GridBagConstraints.WEST, GridBagConstraints.NONE);
            crew[j].setAutoCompleteData(autoCompleteListPersons, true);
            crew[j].setChecks(true, true);
            crew[j].setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
            crew[j].displayOnGui(this, mainInputPanel, (left ? 0 : 4), 5 + j%4);
            crew[j].setVisible(j < 8);
            crew[j].registerItemListener(this);
        }
        crew1defaultText = crew[0].getDescription();

        // Boat Captain
        boatcaptain = new ItemTypeStringList(LogbookRecord.BOATCAPTAIN, "",
                LogbookRecord.getBoatCaptainValues(), LogbookRecord.getBoatCaptainDisplay(),
                IItemType.TYPE_PUBLIC, null, International.getString("Obmann"));
        boatcaptain.setFieldSize(80, FIELD_HEIGHT);
        boatcaptain.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        boatcaptain.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        boatcaptain.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        boatcaptain.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);        
        boatcaptain.displayOnGui(this, mainInputPanel, 5, 4);
        boatcaptain.registerItemListener(this);
        if (isModeBoathouse()) {
            boatcaptain.setVisible(Daten.efaConfig.getValueShowObmann());
        }

        // StartTime
        starttime = new ItemTypeTime(LogbookRecord.STARTTIME, new DataTypeTime(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Abfahrt"));
        starttime.setFieldSize(200, FIELD_HEIGHT);
        starttime.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        starttime.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        starttime.enableSeconds(false);
        starttime.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        starttime.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);
        starttime.displayOnGui(this, mainInputPanel, 0, 9);
        starttime.registerItemListener(this);


        // EndTime
        endtime = new ItemTypeTime(LogbookRecord.ENDTIME, new DataTypeTime(), IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Ankunft"));
        endtime.setFieldSize(200, FIELD_HEIGHT);
        endtime.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        endtime.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        endtime.enableSeconds(false);
        endtime.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        endtime.displayOnGui(this, mainInputPanel, 0, 10);
        endtime.registerItemListener(this);

        starttimeInfoLabel = new ItemTypeLabel("GUIITEM_STARTTIME_INFOLABEL",
                IItemType.TYPE_PUBLIC, null, "");
        starttimeInfoLabel.setFieldGrid(5, GridBagConstraints.WEST, GridBagConstraints.NONE);
        starttimeInfoLabel.setVisible(false);
        starttimeInfoLabel.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);        
        starttimeInfoLabel.displayOnGui(this, mainInputPanel, 3, 9);
        endtimeInfoLabel = new ItemTypeLabel("GUIITEM_ENDTIME_INFOLABEL",
                IItemType.TYPE_PUBLIC, null, "");
        endtimeInfoLabel.setFieldGrid(5, GridBagConstraints.WEST, GridBagConstraints.NONE);
        endtimeInfoLabel.setVisible(false);
        endtimeInfoLabel.displayOnGui(this, mainInputPanel, 3, 10);

        // Destination
        destination = new ItemTypeStringAutoComplete(LogbookRecord.DESTINATIONNAME, "", IItemType.TYPE_PUBLIC, null, 
                International.getStringWithMnemonic("Ziel") + " / " +
                International.getStringWithMnemonic("Strecke"), true);
        destination.setFieldSize(400, FIELD_HEIGHT);
        destination.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        destination.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        destination.setAutoCompleteData(autoCompleteListDestinations);
        destination.setChecks(true, false);
        destination.setIgnoreEverythingAfter(DestinationRecord.DESTINATION_VARIANT_SEPARATOR);
        destination.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        destination.displayOnGui(this, mainInputPanel, 0, 11);
        destination.registerItemListener(this);
        destinationInfo = new ItemTypeString("GUIITEM_DESTINATIONINFO", "",
                IItemType.TYPE_PUBLIC, null, International.getString("Gewässer"));
        destinationInfo.setFieldSize(400, FIELD_HEIGHT);
        destinationInfo.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        destinationInfo.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        destinationInfo.displayOnGui(this, mainInputPanel, 0, 12);
        destinationInfo.setEditable(false);
        destinationInfo.setVisible(false);

        // Waters
        waters = new ItemTypeStringAutoComplete(GUIITEM_ADDITIONALWATERS, "", IItemType.TYPE_PUBLIC, null,
                International.getStringWithMnemonic("Gewässer"), true);
        waters.setFieldSize(400, FIELD_HEIGHT);
        waters.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        waters.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        waters.setAutoCompleteData(autoCompleteListWaters);
        waters.setChecks(true, false);
        waters.setIgnoreEverythingAfter(LogbookRecord.WATERS_SEPARATORS);
        waters.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        waters.displayOnGui(this, mainInputPanel, 0, 13);
        waters.registerItemListener(this);
        waters.setVisible(false);

        // Distance
        distance = new ItemTypeDistance(LogbookRecord.DISTANCE, null, IItemType.TYPE_PUBLIC, null,
                DataTypeDistance.getDefaultUnitName());
        distance.setFieldSize(200, FIELD_HEIGHT);
        distance.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        distance.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        distance.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        distance.displayOnGui(this, mainInputPanel, 0, 14);
        distance.registerItemListener(this);

        // Comments
        comments = new ItemTypeString(LogbookRecord.COMMENTS, null, IItemType.TYPE_PUBLIC, null, International.getStringWithMnemonic("Bemerkungen"));
        comments.setFieldSize(400, FIELD_HEIGHT);
        comments.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        comments.setFieldGrid(7, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        comments.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        comments.setPadding(0, 0, VERTICAL_WHITESPACE_PADDING_GROUPS, 0);
        comments.displayOnGui(this, mainInputPanel, 0, 15);
        comments.registerItemListener(this);

        // Session Type
        sessiontype = new ItemTypeStringList(LogbookRecord.SESSIONTYPE, EfaTypes.TYPE_SESSION_NORMAL,
                EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_VALUES), EfaTypes.makeSessionTypeArray(EfaTypes.ARRAY_STRINGLIST_DISPLAY),
                IItemType.TYPE_PUBLIC, null, International.getString("Fahrtart"));
        sessiontype.setFieldSize(200, FIELD_HEIGHT);
        sessiontype.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        sessiontype.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        sessiontype.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        sessiontype.displayOnGui(this, mainInputPanel, 0, 16);
        sessiontype.registerItemListener(this);
        sessiontype.setReplaceValues(Daten.efaTypes.getSessionTypeReplaceValues());

        // Session Group
        sessiongroup = new ItemTypeStringAutoComplete(LogbookRecord.SESSIONGROUPID,
                "", IItemType.TYPE_PUBLIC, null,
                International.getStringWithMnemonic("Fahrtgruppe"), true);
        sessiongroup.setFieldSize(200, FIELD_HEIGHT);
        sessiongroup.setLabelGrid(1, GridBagConstraints.EAST, GridBagConstraints.NONE);
        sessiongroup.setFieldGrid(2, GridBagConstraints.WEST, GridBagConstraints.NONE);
        sessiongroup.setEditable(false);
        sessiongroup.displayOnGui(this, mainInputPanel, 0, 17);
        sessiongroup.registerItemListener(this);
        sessiongroup.setVisible(isModeFull());
        
        // Session Type Info
        sessionTypeInfo = new ItemTypeLabel("SESSIONTYPE_LABEL", IItemType.TYPE_PUBLIC, null, "");
        sessionTypeInfo.setFieldGrid(5, GridBagConstraints.WEST, GridBagConstraints.NONE);
        sessionTypeInfo.registerItemListener(this);
        sessionTypeInfo.activateMouseClickListener();
        sessionTypeInfo.displayOnGui(this, mainInputPanel, 5, 18);



        // Further Fields which are not part of Data Input

        // Remaining Crew Button
        // changed the unicode caracters for arrows into images, as not all fonts provide arrows.
        // this is neccessary as the user can now select the fonts used in efa.
        remainingCrewUpButton = new ItemTypeButton("REMAININGCREWUP", IItemType.TYPE_PUBLIC, null, "");
        remainingCrewUpButton.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.ARROW_UP));
        remainingCrewUpButton.setFieldSize(18, 30);
        remainingCrewUpButton.setPadding(5, 0, 3, 3);
        remainingCrewUpButton.setMargin(1, 1, 1, 1); //otherwise the caption is too wide for button width in metal
        remainingCrewUpButton.setFieldGrid(1, 2, GridBagConstraints.WEST, GridBagConstraints.VERTICAL);
        remainingCrewUpButton.displayOnGui(this, mainInputPanel, 9, 5);
        remainingCrewUpButton.registerItemListener(this);
        remainingCrewDownButton = new ItemTypeButton("REMAININGCREWDOWN", IItemType.TYPE_PUBLIC, null, "");
        remainingCrewDownButton.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.ARROW_DOWN));
        remainingCrewDownButton.setFieldSize(18, 30);
        remainingCrewDownButton.setPadding(5, 0, 3, 3);
        remainingCrewDownButton.setMargin(1, 1, 1, 1); //otherwise the caption is too wide for button width in metal
        remainingCrewDownButton.setFieldGrid(1, 2, GridBagConstraints.WEST, GridBagConstraints.VERTICAL);
        remainingCrewDownButton.displayOnGui(this, mainInputPanel, 9, 7);
        remainingCrewDownButton.registerItemListener(this);

        // Info Label
        infoLabel.setForeground(Color.blue);
        infoLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        infoLabel.setText(" ");
        mainInputPanel.add(infoLabel,
                new GridBagConstraints(0, 18, 8, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(10, 20, 10, 20), 0, 0));

        // Boat Damage Button
        boatDamageButton = new ItemTypeButton("BOATDAMAGE", IItemType.TYPE_PUBLIC, null, International.getString("Bootsschaden melden"));
        boatDamageButton.setFieldSize(200, FIELD_HEIGHT);
        boatDamageButton.setFieldGrid(4, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        boatDamageButton.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        boatDamageButton.setIcon(getIcon(BaseDialog.IMAGE_DAMAGE));
        boatDamageButton.displayOnGui(this, mainInputPanel, 4, 19);
        boatDamageButton.registerItemListener(this);
        boatDamageButton.setVisible(isModeBoathouse() && Daten.efaConfig.getValueEfaDirekt_showBootsschadenButton());
        
        // Boat Not Cleaned Button
        boatNotCleanedButton = new ItemTypeButton("BOATNOTCLEANED", IItemType.TYPE_PUBLIC, null, International.getString("Boot war nicht geputzt"));
        boatNotCleanedButton.setFieldSize(200, FIELD_HEIGHT);
        boatNotCleanedButton.setFieldGrid(4, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL);
        boatNotCleanedButton.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        boatNotCleanedButton.setIcon(getIcon(BaseDialog.IMAGE_SOAP));
        boatNotCleanedButton.displayOnGui(this, mainInputPanel, 4, 20);
        boatNotCleanedButton.registerItemListener(this);
        boatNotCleanedButton.setVisible(isModeBoathouse() && Daten.efaConfig.getValueEfaDirekt_showBoatNotCleanedButton());

        // Save Button
        saveButton = new ItemTypeButton("SAVE", IItemType.TYPE_PUBLIC, null, International.getString("Eintrag speichern"));
        saveButton.setBackgroundColorWhenFocused(Daten.efaConfig.getValueEfaDirekt_colorizeInputField() ? Color.yellow : null);
        saveButton.setIcon(getIcon(BaseDialog.IMAGE_ACCEPT));
        saveButton.displayOnGui(this, mainPanel, BorderLayout.SOUTH);
        saveButton.registerItemListener(this);

        // Set Valid Date and Time Fields for Autocomplete Lists
        boat.setValidAt(date, starttime);
        cox.setValidAt(date, starttime);
        for (int i=0; i<crew.length; i++) {
            crew[i].setValidAt(date, starttime);
        }
        destination.setValidAt(date, starttime);
    }

    /**
     * Initialize other items of the gui
     */
    void iniGuiRemaining() {
    	// FocusManager Concept is pre JDK 1.4. See comments at EfaBaseFrameFocusManager
    	efaBaseFrameFocusManager = new EfaBaseFrameFocusManager(this,FocusManager.getCurrentManager());
        FocusManager.setCurrentManager(efaBaseFrameFocusManager);
        if (isModeBoathouse()) {
            setResizable(false);
        }
        if (isModeAdmin()) {
            this.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowDeactivated(WindowEvent e) {
                    this_windowDeactivated(e);
                }
            });
        }
        EfaUtil.handleTabbedPaneBackgroundColorForLookAndFeels();
    }

    void iniApplication() {
        if (Daten.project == null && isModeBase()) {
            if (Daten.efaConfig.getValueLastProjectEfaBase().length() > 0) {
                Project.openProject(Daten.efaConfig.getValueLastProjectEfaBase(), true);
                remoteAdmin = (Daten.project != null ? Daten.project.getRemoteAdmin() : null);
                checkRemoteAdmin();
                iniGuiMenu();
            }
        }
        if (Daten.project != null && isModeBase() && Daten.project.getCurrentLogbookEfaBase() != null) {
            openLogbook(Daten.project.getCurrentLogbookEfaBase());
        }
        if (Daten.project != null && isModeAdmin() && logbook != null) {
            // What a hack... ;-) openLogbook() will only open a new logbook if it is not identical with the current one.
            // Actually, there isn't really a *current* logbook. It's only the variable which has been set in the constructor.
            // So we just tweak it a bit so that openLogbook() will accept our logbook as a new one...
            Logbook newLogbook = logbook;
            this.logbook = null;
            openLogbook(newLogbook);
            if (_jumpToEntryNo != null && _jumpToEntryNo.length() > 0) {
                goToEntry(_jumpToEntryNo, false);
            }
        }
    }




    public void setTitle() {
        String adminName = (getAdmin() != null ? getAdmin().getName() : null);
        String adminNameString = (adminName != null && adminName.length() > 0 ?
            " [" + adminName + "]" : "");
        if (isModeBoathouse()) {
            setTitle(Daten.EFA_LONGNAME);
        } else {
            if (Daten.project == null) {
                setTitle(Daten.EFA_LONGNAME + adminNameString);
            } else {
                if (!isLogbookReady()) {
                    setTitle(Daten.project.getProjectName() + " - " + Daten.EFA_LONGNAME + adminNameString);
                } else {
                    if ((Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD) &&
                            (TxRequestQueue.getInstance() != null)) {
                        TxRequestQueue txq = TxRequestQueue.getInstance();
                        if (txq != null)
                            txq.setEfaGUIrootContainer(this);   // is relevant only at startup
                        String efaCloudStatus = (txq != null) ? txq.getStateForDisplay(true) : "";
                        setTitle(
                                Daten.project.getProjectName() + ": " + logbook.getName() + " - " + Daten.EFA_LONGNAME +
                                        adminNameString + efaCloudStatus);
                    } else
                        setTitle(Daten.project.getProjectName() + ": " + logbook.getName() + " - " + Daten.EFA_LONGNAME + adminNameString);
                }
            }
        }
    }

    private void clearAllBackgroundColors() {
        entryno.restoreBackgroundColor();
        date.restoreBackgroundColor();
        enddate.restoreBackgroundColor();
        boat.restoreBackgroundColor();
        boatvariant.restoreBackgroundColor();
        cox.restoreBackgroundColor();
        for (int i=0; i<crew.length; i++) {
            crew[i].restoreBackgroundColor();
        }
        boatcaptain.restoreBackgroundColor();
        starttime.restoreBackgroundColor();
        endtime.restoreBackgroundColor();
        destination.restoreBackgroundColor();
        waters.restoreBackgroundColor();
        distance.restoreBackgroundColor();
        comments.restoreBackgroundColor();
        sessiontype.restoreBackgroundColor();
        boatDamageButton.restoreBackgroundColor();
        boatNotCleanedButton.restoreBackgroundColor();
        saveButton.restoreBackgroundColor();
    }

    // =========================================================================
    // Data-related methods
    // =========================================================================

    protected long getValidAtTimestamp(LogbookRecord r) {
        long t = 0;
        if (r != null) {
            t = r.getValidAtTimestamp();
        } else {
            t = LogbookRecord.getValidAtTimestamp(date.getDate(), starttime.getTime());
        }
        if (t == 0) {
            t = System.currentTimeMillis();
        }
        return t;
    }

    protected PersonRecord findPerson(ItemTypeString item, long validAt) {
         try {
            String s = item.getValueFromField().trim();
            if (Daten.efaConfig.getValuePostfixPersonsWithClubName()) {
                s = PersonRecord.trimAssociationPostfix(s);
            }
            if (s.length() > 0) {
                PersonRecord r = Daten.project.getPersons(false).getPerson(s, validAt);

                // If we have not found a valid record, we next try whether we can find
                // any (currently invalid) record for that name (within the validiy range
                // of this lookbook). If we find such a record, we use its ID to find
                // yet another record of the same ID, which might be valid now.
                // Since we search by name, it could be that the user entered name "A", which is
                // not currently valid, but appears in the AutoComplete list because it is valid
                // some other time for this logbook. It might be that "A" is just another name for
                // "B" of the same record, which is valid. If there is a "B", we will use that.
                // That means, even though the user entered "A", we will save the ID, and display "B".
                if (validAt > 0 && r == null) {
                    PersonRecord r2 = Daten.project.getPersons(false).getPerson(s, this.logbookValidFrom, logbookInvalidFrom-1, validAt);
                    if (r2 != null) {
                        r = Daten.project.getPersons(false).getPerson(r2.getId(), validAt);
                    }
                }

                return r;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    protected BoatRecord findBoat(ItemTypeString item, long validAt) {
        try {
            String s = item.getValueFromField().trim();
            if (s.length() > 0) {
                BoatRecord r = Daten.project.getBoats(false).getBoat(s, validAt);

                // If we have not found a valid record, we next try whether we can find
                // any (currently invalid) record for that name (within the validiy range
                // of this lookbook). If we find such a record, we use its ID to find
                // yet another record of the same ID, which might be valid now.
                // Since we search by name, it could be that the user entered name "A", which is
                // not currently valid, but appears in the AutoComplete list because it is valid
                // some other time for this logbook. It might be that "A" is just another name for
                // "B" of the same record, which is valid. If there is a "B", we will use that.
                // That means, even though the user entered "A", we will save the ID, and display "B".
                if (validAt > 0 && r == null) {
                    BoatRecord r2 = Daten.project.getBoats(false).getBoat(s, this.logbookValidFrom, logbookInvalidFrom-1, validAt);
                    if (r2 != null) {
                        r = Daten.project.getBoats(false).getBoat(r2.getId(), validAt);
                    }
                }

                return r;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    private PersonRecord findPerson(int pos, long validAt) {
        return findPerson(getCrewItem(pos), validAt);
    }

    protected DestinationRecord findDestinationFromString(String s, long validAt) {
        return findDestinationFromString(s, null, validAt);
    }

    private DestinationRecord findDestinationFromString(String s, 
            String boathouseName, long validAt) {
        DestinationRecord r = null;
        try {
            String[] dest = LogbookRecord.getDestinationNameAndVariantFromString(s);
            if (dest[0].length() > 0 && dest[1].length() > 0) {
                // this is a destination of the form "base + variant".
                // however, it could be that we have an explicit destination "base & variant" in our database.
                // check for "base & variant" first
                r = Daten.project.getDestinations(false).getDestination(dest[0] + " & " + dest[1],
                        boathouseName, validAt);
                if (r != null) {
                    return r;
                }
            }
            if (dest[0].length() > 0) {
                r = Daten.project.getDestinations(false).getDestination(dest[0],
                        boathouseName, validAt);

                // If we have not found a valid record, we next try whether we can find
                // any (currently invalid) record for that name (within the validiy range
                // of this lookbook). If we find such a record, we use its ID to find
                // yet another record of the same ID, which might be valid now.
                // Since we search by name, it could be that the user entered name "A", which is
                // not currently valid, but appears in the AutoComplete list because it is valid
                // some other time for this logbook. It might be that "A" is just another name for
                // "B" of the same record, which is valid. If there is a "B", we will use that.
                // That means, even though the user entered "A", we will save the ID, and display "B".
                if (validAt > 0 && r == null) {
                    DestinationRecord r2 = Daten.project.getDestinations(false).getDestination(dest[0],
                            this.logbookValidFrom, logbookInvalidFrom-1, validAt);
                    if (r2 != null) {
                        r = Daten.project.getDestinations(false).getDestination(r2.getId(), validAt);
                    }
                }

                return r;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    DestinationRecord findDestination(long validAt) {
        String s = destination.getValueFromField();
        String bths = null;
        if (isModeBoathouse() && Daten.project.getNumberOfBoathouses() > 1) {
            bths = Daten.project.getMyBoathouseName();
        }
        DestinationRecord r = findDestinationFromString(s, bths, validAt);
        if (r == null && s != null && s.length() > 0) {
            // not found; try to find as prefixed with water
            int pos = s.indexOf(DestinationRecord.WATERS_DESTINATION_DELIMITER);
            if (pos > 0 && pos+1 < s.length()) {
                s = s.substring(pos+1);
                r = findDestinationFromString(s, bths, validAt);
            }
        }
        if (r == null && s != null && s.length() > 0) {
            // not found; try to find as postfixed with boathouse name
            String dest = DestinationRecord.getDestinationNameFromPostfixedDestinationBoathouseString(s);
            bths = DestinationRecord.getBoathouseNameFromPostfixedDestinationBoathouseString(s);
            if (dest != null && bths != null && dest.length() > 0 && bths.length() > 0) {
                r = findDestinationFromString(dest, bths, validAt);
            }
        }
        return r;
    }

    DataTypeList[] findWaters(ItemTypeString item) {
        try {
            String s = item.toString().trim();
            if (s.length() == 0) {
                return null;
            }
            s = EfaUtil.replace(s, "+", ",", true);
            s = EfaUtil.replace(s, ";", ",", true);
            Vector<String> wlist = EfaUtil.split(s, ',');
            if (wlist.size() == 0) {
                return null;
            }
            DataTypeList<UUID> watersIdList = new DataTypeList<UUID>();
            DataTypeList<String> watersNameList = new DataTypeList<String>();
            for (int i=0; i<wlist.size(); i++) {
                String ws = wlist.get(i).trim();
                if (ws.length() == 0) {
                    continue;
                }
                WatersRecord w = Daten.project.getWaters(false).findWatersByName(ws);
                if (w != null && w.getId() != null) {
                    watersIdList.add(w.getId());
                } else {
                    watersNameList.add(ws);
                }
            }
            return new DataTypeList[] {
                watersIdList, watersNameList
            };
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }


    String updateBoatVariant(BoatRecord b, int variant) {
        if (_inUpdateBoatVariant) {
            return null;
        }
        if (Logger.isTraceOn(Logger.TT_GUI, 7)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_EFABASEFRAME,
                    "updateBoatVariant(" +
                     (b != null ? b.getQualifiedName() : "") + ", " + variant + ")");
        }
        _inUpdateBoatVariant = true;
        try {
            if (b != null) {
                int numberOfVariants = b.getNumberOfVariants();
                if (numberOfVariants < 0) {
                    boatvariant.setVisible(false);
                    return null;
                }

                if (variant == -1 && numberOfVariants > 1 && boatvariant.isVisible() &&
                    currentBoat != null && currentBoat.getId() != null &&
                    b.getId() != null && currentBoat.getId().equals(b.getId())) {
                    variant = EfaUtil.string2int(boatvariant.getValueFromField(), -1);
                }

                String[] bt = new String[numberOfVariants];
                String[] bv = new String[numberOfVariants];
                for (int i = 0; i < numberOfVariants; i++) {
                    bt[i] = Integer.toString(b.getTypeVariant(i));
                    bv[i] = b.getQualifiedBoatTypeShortName(i);
                }
                boatvariant.setListData(bt, bv);
                if (variant < 0 && b.getDefaultVariant() > 0) {
                    variant = b.getDefaultVariant();
                }
                if (variant > 0) {
                    boatvariant.parseAndShowValue(Integer.toString(variant));
                } else {
                    if (numberOfVariants > 0 && bt != null && bt.length > 0 && bt[0] != null) {
                        boatvariant.parseAndShowValue(bt[0]);
                    }
                }
                boatvariant.setVisible(numberOfVariants > 1);
                return boatvariant.getValue();
            }
            boatvariant.setListData(null, null);
            boatvariant.setVisible(false);
            return null;
        } finally {
            _inUpdateBoatVariant = false;
        }
    }

    void openLogbook(String logbookName) {
        if (!getAdmin().isAllowedEditLogbook()) {
            logbookName = null;
        }
        if (logbookName == null || logbookName.length() == 0) {
            setFields(null);
        } else {
            Logbook newLogbook = Daten.project.getLogbook(logbookName, false);
            if (newLogbook != null) {
                openLogbook(newLogbook);
            } else {
                Dialog.error(LogString.fileOpenFailed(logbookName, International.getString("Fahrtenbuch")));
                setFields(null);
            }
        }
    }

    void openLogbook(Logbook newLogbook) {
        if (Daten.project == null) {
            return;
        }
        if (newLogbook == null) {
            return;
        }
        try {
            if (logbook != null && logbook.isOpen()) {
                if (logbook.getName().equals(newLogbook.getName()) &&
                    logbook.getProject().getProjectName().equals(Daten.project.getProjectName())) {
                    return;
                }
                logbook.close();
            }
        } catch (Exception e) {
            Logger.log(e);
            Dialog.error(e.toString());
        }
        logbook = newLogbook;
        if (!isModeBoathouse()) {
            Daten.project.setCurrentLogbookEfaBase(logbook.getName());
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
        }
        if (isModeBoathouse()) {
            autoCompleteListDestinations.setPostfixNamesWithBoathouseName(false);
        }
        if (isModeFull()) {
            try {
                LogbookRecord r = (LogbookRecord) logbook.data().getLast();
                if (r != null) {
                    setFields(r);
                } else {
                    createNewRecord(false);
                }
                entryno.requestFocus();
            } catch (Exception e) {
                Logger.logdebug(e);
                setFields(null);
            }
        }
        setTitle();
    }

    void openClubwork(String clubworkName) {
        if (!getAdmin().isAllowedEditLogbook()) {
            clubworkName = null;
        }
        if (clubworkName != null && clubworkName.length() > 0) {
            Clubwork newClubwork = Daten.project.getClubwork(clubworkName, false);
            if (newClubwork != null) {
                if (!isModeBoathouse()) {
                    Daten.project.setCurrentClubworkBookEfaBase(newClubwork.getName());
                    Dialog.infoDialog(LogString.fileOpened(clubworkName, International.getString("Vereinsarbeit")));
                }
            } else {
                Dialog.error(LogString.fileOpenFailed(clubworkName, International.getString("Vereinsarbeit")));
            }
        }
    }

    boolean isLogbookReady() {
        return Daten.project != null && Daten.project.isOpen() && logbook != null && logbook.isOpen();
    }

    private String getFieldValue(ItemTypeLabelValue field, LogbookRecord r) {
        try {
            if (field == entryno) {
                return (r != null && r.getEntryId() != null ? r.getEntryId().toString() : "");
            }
            if (field == date) {
                return (r != null ? r.getDate().toString() : "");
            }
            if (field == enddate) {
                return (r != null ? r.getEndDate().toString() : "");
            }
            if (field == boat) {
                return (r != null ? r.getBoatAsName(getValidAtTimestamp(r)) : "");
            }
            if (field == boatvariant) {
                return updateBoatVariant((r != null ? r.getBoatRecord(getValidAtTimestamp(r)) : null), (r != null ? r.getBoatVariant() : 0));
            }
            if (field == cox) {
                return (r != null ? r.getCoxAsName(getValidAtTimestamp(r)) +
                                    (Daten.efaConfig.getValuePostfixPersonsWithClubName() ? PersonRecord.getAssociationPostfix(r.getCrewRecord(0, getValidAtTimestamp(r))) : "")
                        : "");
            }
            for (int i = 0; i < crew.length; i++) {
                if (field == crew[i]) {
                    return (r != null ? r.getCrewAsName(i + 1, getValidAtTimestamp(r)) +
                                    (Daten.efaConfig.getValuePostfixPersonsWithClubName() ? PersonRecord.getAssociationPostfix(r.getCrewRecord(i + 1, getValidAtTimestamp(r))) : "")
                            : "");
                }
            }
            if (field == starttime) {
                return (r != null ? r.getStartTime().toString() : "");
            }
            if (field == endtime) {
                return (r != null ? r.getEndTime().toString() : "");
            }
            if (field == destination) {
                return (r != null ? r.getDestinationAndVariantName(getValidAtTimestamp(r),
                        Daten.efaConfig.getValuePrefixDestinationWithWaters(),
                        !isModeBoathouse()
                        ) : "");
            }
            if (field == waters) {
                return (r != null ? r.getWatersNamesStringList() : "");
            }
            if (field == distance) {
                return (r != null ? r.getDistance().getAsFormattedString() : "");
            }
            if (field == comments) {
                return (r != null ? r.getComments() : "");
            }
            if (field == sessiontype) {
                return (r != null ? r.getSessionType() : Daten.efaConfig.getValueStandardFahrtart());
            }
            if (field == sessiongroup) {
                UUID id = (r != null ? r.getSessionGroupId() : null);
                sessiongroup.setRememberedId(id);
                return Daten.project.getSessionGroups(false).getSessionGroupName(id);
            }
            if (field == boatcaptain) {
                int pos = (r != null ? (r.getBoatCaptainPosition() >= 0 ? r.getBoatCaptainPosition() : -1) : -1);
                if (pos >= 0) {
                    setBoatCaptain(pos, false);
                }
                return (pos != -1 ? Integer.toString(pos) : "");
            }
        } catch (NullPointerException enull) {
            // this happens when field in r has not been set, no need to log this!
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return "";
    }

    void setField(ItemTypeLabelValue field, LogbookRecord r) {
        field.parseAndShowValue(getFieldValue(field, r));
    }

    void setFields(LogbookRecord r) {
        if (!isLogbookReady() && r != null) {
            return;
        }
        referenceRecord = currentRecord;
        currentRecord = r;
        if (iterator != null && r != null) {
            iterator.goTo(r.getKey());
        }
        isNewRecord = r == null;
        isInsertedRecord = false;

        setField(entryno,r);
        setField(date,r);
        setField(enddate,r);
        setField(boat,r);
        setField(boatvariant,r);
        setField(cox,r);
        for (int i=0; i<crew.length; i++) {
            setField(crew[i],r);
        }
        setField(boatcaptain,r);
        setField(starttime,r);
        setField(endtime,r);
        setField(destination,r);
        setDestinationInfo( (r != null ? r.getDestinationRecord(getValidAtTimestamp(r)) : null) );
        setField(waters,r);
        setField(distance,r);
        setField(comments,r);
        setField(sessiontype,r);
        setField(sessiongroup,r);
        // replaced by closesessionButton // opensession.setVisible(isModeFull() && r != null && r.getSessionIsOpen());
        closesessionButton.setVisible(isModeFull() && r != null && r.getSessionIsOpen());
        currentBoatUpdateGui( (r != null && r.getBoatVariant() > 0 ? r.getBoatVariant() : -1) );
        setCrewRangeSelection(0);
        setEntryUnchanged();
        updateSessionTypeInfo();
        entryNoForNewEntry = -1; // -1 bedeutet, daß beim nächsten neuen Datensatz die LfdNr "last+1" vorgegeben wird
        if (r == null) {
            date.requestFocus();
            date.setSelection(0, Integer.MAX_VALUE);
            if (Daten.efaConfig.getValueDefaultValueComments() != null && 
                Daten.efaConfig.getValueDefaultValueComments().trim().length() > 0) {
                comments.parseAndShowValue(Daten.efaConfig.getValueDefaultValueComments().trim());
            }
        }
        updateTimeInfoFields();
        setWarningsForUnsetFields();
    }

    /**
     * sets Warning Icons for fields that shall be non-null in efaBase or Admin Mode.
     */
    private void setWarningsForUnsetFields() {
    	if (isModeBase() || isModeAdmin() ) {
    		Icon warning =  ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_WARNING);
    		date.setIcon     (date.isSet() ? null : warning);
    		starttime.setIcon(starttime.isSet() ? null : warning);
    		endtime.setIcon  (endtime.isSet() ? null : warning);
    		distance.setIcon ((distance.getValue().getRoundedValueInKilometers()>0) ? null : warning );
    		crew[0].setIcon  (crew[0].getValue() != null && !crew[0].getValue().isEmpty() ? null : warning);
    		boat.setIcon(    (boat.getValue() != null && !boat.getValue().isEmpty() ? null : warning));    		
    	}
    }
    
    /**
     * Creates a new LogbookRecord if necessary and set it's attributes from the GUI fields.
     * It works for both efaBaseFrame AND efaBaseFrameMultisession, as the variable parts
     * for these two variants get done by separate methods.
     * @return
     */
    protected LogbookRecord getFields() {
        String s;
        if (!isLogbookReady()) {
            return null;
        }

        // EntryNo
        LogbookRecord r = (isNewRecord || currentRecord == null ?
            logbook.createLogbookRecord(DataTypeIntString.parseString(entryno.getValue())) :
            currentRecord);
        r.setEntryId(DataTypeIntString.parseString(entryno.getValue()));

        // Date
        if (date.isSet()) {
            r.setDate(date.getDate());
        } else {
            r.setDate(null);
        }

        // End Date
        if (enddate.isSet()) {
            r.setEndDate(enddate.getDate());
        } else {
            r.setEndDate(null);
        }

        // Start & End Time
        if (starttime.isSet()) {
            r.setStartTime(starttime.getTime());
        } else {
            r.setStartTime(null);
        }
        if (endtime.isSet()) {
            r.setEndTime(endtime.getTime());
        } else {
            r.setEndTime(null);
        }

        //these methods may get overwritten by subclasses 
        getFieldsForBoats(r);
        getFieldsForCrew(r);
        
        // Destination
        DestinationRecord d = findDestination(getValidAtTimestamp(r));
        if (d != null) {
            r.setDestinationId(d.getId());
            r.setDestinationVariantName(LogbookRecord.getDestinationNameAndVariantFromString(destination.toString())[1]);
            r.setDestinationName(null);
        } else {
            s = destination.toString().trim();
            r.setDestinationName( (s.length() == 0 ? null : s) );
            r.setDestinationId(null);
            r.setDestinationVariantName(null);
        }

        // Waters
        if (waters.isVisible()) {
            DataTypeList[] wlists = findWaters(waters);
            if (wlists == null || wlists.length == 0 ||
                (wlists[0].length() == 0 && wlists[1].length() == 0)) {
                r.setWatersIdList(null);
                r.setWatersNameList(null);
            } else {
                r.setWatersIdList( (wlists[0].length() > 0 ? wlists[0] : null) );
                r.setWatersNameList( (wlists[1].length() > 0 ? wlists[1] : null) );
            }
        }

        // Distance
        if (distance.isSet()) {
            r.setDistance(distance.getValue());
        } else {
            r.setDistance(null);
        }

        // Comments
        s = comments.toString().trim();
        if (s.length() > 0) {
            r.setComments(s);
        } else {
            r.setComments(null);
        }

        // Session Type
        r.setSessionType(sessiontype.toString());

        getSessionGroupID(r);
        
        return r;
    }
    
    /**
     * For any AutoComplete field in this Baseframe, try to use the autocomplete.
     */
    protected void autocompleteAllFields() {
        try {
            if (boat.isVisible()) {
                boat.acpwCallback(null);
            }
            if (cox.isVisible()) {
                cox.acpwCallback(null);
            }
            for (int i=0; i<crew.length; i++) {
                if (crew[i].isVisible()) {
                    crew[i].acpwCallback(null);
                }
            }
            if (destination.isVisible()) {
                destination.acpwCallback(null);
            }
        } catch(Exception e) {     
        	Logger.logdebug(e);
        }
    }

    // Datensatz speichern
    // liefert "true", wenn erfolgreich
    boolean saveEntry() {
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
        if (!checkMisspelledInput() ||
        		!checkDate() ||
        		!checkBoatNameValid(boat) ||
        		!checkBoatStatus() ||
                !checkDuplicatePersons() ||
                !checkPersonsForBoatType() ||
                !checkCrewNamesValid() ||
                !checkUnknownNames() ||
                !checkProperUnknownNames() ||
                !checkAllowedPersons() ||
                !checkPersonsForBoatType() ||
                !checkBoatCaptain() ||
                !checkEntryNo() ||
                !checkMultiDayTours() ||
                !checkTime() ||
                !checkAllowedDateForLogbook() ||
                !checkDestinationNameValid() ||
                !checkAllDataEntered() ||
                !checkSessionType() ||
                !checkDuplicateEntry() 
        		) {
                return false;
            }

        boolean success = saveEntryInLogbook();

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
        autoCompleteListPersons.reset();
        this.setWarningsForUnsetFields();
        return success;
    }

    // den Datensatz nun wirklich speichern;
    protected boolean saveEntryInLogbook() {
        if (!isLogbookReady()) {
            return false;
        }

        long lock = 0;
        Exception myE = null;
        try {
            boolean changeEntryNo = false;
            if (!isNewRecord && currentRecord != null && !currentRecord.getEntryId().toString().equals(entryno.toString())) {
                // Datensatz mit geänderter LfdNr: Der alte Datensatz muß gelöscht werden!
                lock = logbook.data().acquireGlobalLock();
                logbook.data().delete(currentRecord.getKey(), lock);
                changeEntryNo = true;
            }
            currentRecord = getFields();
            
            if (mode == MODE_BOATHOUSE_START || mode == MODE_BOATHOUSE_START_CORRECT || mode == MODE_BOATHOUSE_START_MULTISESSION) {
                currentRecord.setSessionIsOpen(true);
            } else if (mode == MODE_BOATHOUSE_FINISH || mode == MODE_BOATHOUSE_LATEENTRY || mode == MODE_BOATHOUSE_LATEENTRY_MULTISESSION){
                currentRecord.setSessionIsOpen(false); // all other updates to an open entry (incl. Admin Mode) will mark it as finished
            } else if (mode == MODE_BASE || mode == MODE_ADMIN){
            	if (isNewRecord || currentRecord == null) {
            		// created a new record in efaBase or in efaBoathouse in admin mode.
            		// then this record does not have an open session, like a boat on the water or such.
            		// so we set openSession to false
            		currentRecord.setSessionIsOpen(false);
            	} else {
            		//otherwise, leave SessionIsOpen unchanged, 
            		//as the record was just edited for some reason
            		EfaUtil.foo();
            	}
            } else {
            	/* other modes are

				    MODE_BOATHOUSE = 1; // just kiosk mode for efaBoathouse, not a valid efaBaseFrame for editing record
					MODE_BOATHOUSE_ABORT = 6; // abort sessions without a dialog
				    MODE_ADMIN_SESSIONS = 8; // not used anyway
				    
            		//so, leave SessionIsOpen unchanged as there is no good reason to close it. 
				    
            	 */
            	EfaUtil.foo();
            }

            if (isNewRecord || changeEntryNo) {
                logbook.data().add(currentRecord, lock);
            } else {
                DataRecord newRecord = logbook.data().update(currentRecord, lock);
                if (newRecord != null && newRecord instanceof LogbookRecord) {
                    currentRecord = (LogbookRecord)newRecord;
                }
            }
            isNewRecord = false;
        } catch (Exception e) {
            Logger.log(e);
            myE = e;
        } finally {
            if (lock != 0) {
                logbook.data().releaseGlobalLock(lock);
            }
        }
        if (myE != null) {
            Dialog.error(International.getString("Fahrtenbucheintrag konnte nicht gespeichert werden.") + "\n" + myE.toString());
            return false;
        }

        if (isModeFull()) {
            logAdminEvent(Logger.INFO, (isNewRecord ? Logger.MSG_ADMIN_LOGBOOK_ENTRYADDED : Logger.MSG_ADMIN_LOGBOOK_ENTRYMODIFIED),
                    (isNewRecord ? International.getString("Eintrag hinzugefügt") : International.getString("Eintrag geändert")) , currentRecord);
        }
        return true;
    }
    

    void setEntryUnchanged() {
        entryno.setUnchanged();
        date.setUnchanged();
        enddate.setUnchanged();
        boat.setUnchanged();
        boatvariant.setUnchanged();
        cox.setUnchanged();
        for (int i=0; i<crew.length; i++) {
            crew[i].setUnchanged();
        }
        boatcaptain.setUnchanged();
        starttime.setUnchanged();
        endtime.setUnchanged();
        destination.setUnchanged();
        waters.setUnchanged();
        distance.setUnchanged();
        comments.setUnchanged();
        sessiontype.setUnchanged();
        sessiongroup.setUnchanged();
    }

    boolean isEntryChanged() {
        boolean changed =
                entryno.isChanged() ||
                date.isChanged() ||
                enddate.isChanged() ||
                boat.isChanged() ||
                cox.isChanged() ||
                boatcaptain.isChanged() ||
                starttime.isChanged() ||
                endtime.isChanged() ||
                destination.isChanged() ||
                waters.isChanged() ||
                distance.isChanged() ||
                comments.isChanged() ||
                sessiontype.isChanged() ||
                sessiongroup.isChanged();
        for (int i=0; !changed && i<crew.length; i++) {
            changed = crew[i].isChanged();
        }
        return changed;
    }

    boolean promptSaveChangesOk() {
        if (!isLogbookReady() || isModeBoathouse()) {
            return true;
        }
        if (isEntryChanged()) {
            String txt;
            if (isNewRecord) {
                txt = International.getString("Der aktuelle Eintrag wurde verändert und noch nicht zum Fahrtenbuch hinzugefügt.") + "\n" +
                      International.getString("Eintrag hinzufügen?");
            } else {
                txt = International.getString("Änderungen an dem aktuellen Eintrag wurden noch nicht gespeichert.") + "\n" +
                      International.getString("Änderungen speichern?");
            }
            switch (Dialog.yesNoCancelDialog(International.getString("Eintrag nicht gespeichert"), txt)) {
                case Dialog.YES:
                    return saveEntry();
                case Dialog.NO:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    ItemTypeStringAutoComplete getCrewItem(int pos) {
        if (pos == 0) {
            return cox;
        }
        if (pos >= 1 && pos <= LogbookRecord.CREW_MAX) {
            return crew[pos-1];
        }
        return null;
    }

    boolean isCoxOrCrewItem(IItemType item) {
        if (item == cox) {
            return true;
        }
        for (int i=0; i<crew.length; i++) {
            if (item == crew[i]) {
                return true;
            }
        }
        return false;
    }

    int getNumberOfPersonsInBoat() {
        int c = 0;
        if (cox.getValueFromField().trim().length() > 0) {
            c++;
        }
        for (int i = 0; i < LogbookRecord.CREW_MAX; i++) {
            if (crew[i].getValueFromField().trim().length() > 0) {
                c++;
            }
        }
        return c;
    }

    void setTime(ItemTypeTime field, int addMinutes, DataTypeTime notBefore) {
        DataTypeTime now = DataTypeTime.now();

        if (mode == MODE_BOATHOUSE_LATEENTRY && field == this.starttime) {
            // for Late Entries, we default to a start time about 100 minutes ago
            now.delete((100+addMinutes)*60);
        }

        now.setSecond(0);
        now.add(addMinutes*60);
        int m = now.getMinute();
        if (m % 5 != 0) {
            if (m % 5 < 3) {
                now.delete((m % 5) * 60);
            } else {
                now.add((5 - m % 5) * 60);
            }
        }

        if (notBefore != null) {
            // Test: EndTime < StartTime (where EndTime is at most the configured (add+substract)*2 times smaller)
            if (now.isBefore(notBefore) &&
                now.getTimeAsSeconds() + (Daten.efaConfig.getValueEfaDirekt_plusMinutenAbfahrt() +
                Daten.efaConfig.getValueEfaDirekt_minusMinutenAnkunft()) * 60 * 2 >
                notBefore.getTimeAsSeconds()) {
                // use StartTime as EndTime instead (avoid overlapping times)
                now.setHour(notBefore.getHour());
                now.setMinute(notBefore.getMinute());
            }
        }

        field.parseAndShowValue(now.toString());
        field.setSelection(0, Integer.MAX_VALUE);
    }

    void updateTimeInfoFields() {
        JComponent endDateField = enddate.getComponent();
        starttimeInfoLabel.setVisible(endDateField != null && endDateField.isVisible() &&
                starttime.isVisible());
        endtimeInfoLabel.setVisible(endDateField != null && endDateField.isVisible() &&
                endtime.isVisible());
        
        String date1 = date.getValueFromField();
        String date2 = enddate.getValueFromField();
        starttimeInfoLabel.setDescription( (date1 != null && date1.length() > 0 ?
                " (" + International.getMessage("am {date}", date1) + ")" :
            "") );
        endtimeInfoLabel.setDescription( (date2 != null && date2.length() > 0 ?
                " (" + International.getMessage("am {date}", date2) + ")" :
            "") );
    }

    void setDestinationInfo(DestinationRecord r) {
        boolean showDestinationInfo = false;
        boolean showWatersInput = false;

        if (Daten.efaConfig.getValueShowDestinationInfoForInput()) {
            String[] info = (r != null ? r.getWatersAndDestinationAreasAsLabelAndString() : null);
            String infoLabel = (info != null ? info[0] : International.getString("Gewässer"));
            String infoText = (info != null ? info[1] : "");
            destinationInfo.setDescription(infoLabel);
            destinationInfo.parseAndShowValue((infoText.length() > 0 ? infoText : " ")); // intentionally a space and not empty!
            showDestinationInfo = infoText.length() > 0;
        }

        if (!Daten.efaConfig.getValueAdditionalWatersInput() && !Daten.efaConfig.getValueEfaDirekt_gewaesserBeiUnbekanntenZielenPflicht()
                && !showEditWaters) {
            waters.setVisible(false);
        } else {
            String variant = LogbookRecord.getDestinationNameAndVariantFromString(destination.getValueFromField())[1];
            boolean isDestinationUnknownOrVariant = (r == null
                    || (variant != null && variant.length() > 0));
            boolean watersWasVisible = waters.isVisible();
            showWatersInput = (Daten.efaConfig.getValueAdditionalWatersInput() || Daten.efaConfig.getValueEfaDirekt_gewaesserBeiUnbekanntenZielenPflicht() || showEditWaters)
                    && isDestinationUnknownOrVariant && destination.getValueFromField().length() > 0;
            if (showWatersInput) {
                if (r == null) {
                    waters.setDescription(International.getString("Gewässer"));
                } else {
                    waters.setDescription(International.getString("Weitere Gewässer"));
                }
            }
            waters.setVisible(showWatersInput);
            if (showWatersInput && !watersWasVisible && isModeBoathouse()) {
                waters.requestFocus();
            }
        }

        if (showDestinationInfo ||
            (Daten.efaConfig.getValueShowDestinationInfoForInput() && !showWatersInput)) {
            destinationInfo.setVisible(true);
        } else {
            destinationInfo.setVisible(false);
        }
    }

    void truncateWatersFromEnteredDestination() {
        String dest = destination.getValueFromField().trim();
        int pos = dest.indexOf(DestinationRecord.WATERS_DESTINATION_DELIMITER.trim());
        if (pos > 0 && pos+1 < dest.length()) {
            String wtext = dest.substring(0, pos).trim();
            String dtext = dest.substring(pos+1).trim();
            destination.parseAndShowValue(dtext);
            waters.parseAndShowValue(wtext);
        }
    }

    void setDesinationDistance() {
        String newDestination = DestinationRecord.tryGetNameAndVariant(destination.getValueFromField().trim())[0];
        if (isModeBoathouse() && newDestination.length()>0 && distance.getValueFromField().trim().length() == 0) {
            lastDestination = "";
        }
        setFieldEnabledDistance();
        if (!destination.isKnown()) {
            if (!newDestination.equals(lastDestination)) {
                // Das "Leeren" des Kilometerfeldes darf nur im DirectMode erfolgen. Im normalen Modus hätte das
                // den unschönen Nebeneffekt, daß beim korrigieren von unbekannten Zielen die eingegeben Kilometer
                // aus dem Feld verschwinden (ebenso nach der Suche nach unvollständigen Einträgen mit unbekannten
                // Zielen).
                if (this.isModeBoathouse() && (mode == MODE_BOATHOUSE_START || mode == MODE_BOATHOUSE_START_CORRECT || mode == MODE_BOATHOUSE_START_MULTISESSION || 
                		mode == MODE_BOATHOUSE_FINISH || mode == MODE_BOATHOUSE_LATEENTRY || mode == MODE_BOATHOUSE_LATEENTRY_MULTISESSION)) {
                    distance.parseAndShowValue("");
                }
                lastDestination = "";
                if (Daten.efaConfig.getValuePrefixDestinationWithWaters()) {
                    truncateWatersFromEnteredDestination();
                }
            }
            setDestinationInfo(null);
            return;
        }

        DestinationRecord r = findDestination(getValidAtTimestamp(null));
        if (!newDestination.equals(lastDestination) && newDestination.length() != 0 && this.isLogbookReady()) {
            // die folgende Zeile ist korrekt, da diese Methode nur nach "vervollstaendige" und bei
            // "zielButton.getBackground()!=Color.red" aus "ziel_keyReleased" oder "zielButton_focusLost"
            // aufgerufen wird und somit ein gültiger Datensatz bereits gefunden wurde!
            if (r != null && r.getDistance() != null && r.getDistance().isSet()) {
                distance.parseAndShowValue(r.getDistance().getAsFormattedString());
            } else {
                // do NOT clear the distance in Admin Mode. Users complain that a previously entered
                // distance gets lost when editing, for example for unknown destination records
                // (with properly entered distance), when clicking the red button and adding it to
                // the destination list.
                if (isModeBoathouse()) {
                    distance.parseAndShowValue("");
                }
            }
        }

        String currentDistance = distance.getValueFromField();
        if (currentDistance == null || EfaUtil.stringFindInt(currentDistance,0) == 0) {
            // always enable when no distance entered
            setFieldEnabled(true, true, distance);
        }

        setDestinationInfo(r);
    }

    void editBoat(ItemTypeStringAutoComplete item) {
        if (!isLogbookReady()) {
            return;
        }
        String s = item.getValueFromField().trim();
        if (s.length() == 0) {
            return;
        }
        BoatRecord r = findBoat(boat,getValidAtTimestamp(null));
        if (r == null) {
            r = findBoat(boat, -1);
        }
        if (isModeBoathouse() || getMode() == MODE_ADMIN_SESSIONS) {
            if (!Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenNamenHinzufuegen() || r != null) {
                return; // only add new boats (if allowed), but don't edit existing ones
            }
        }
        boolean newRecord = (r == null);
        if (r == null) {
            r = Daten.project.getBoats(false).createBoatRecord(UUID.randomUUID());
            r.addTypeVariant("", EfaTypes.TYPE_BOAT_OTHER, EfaTypes.TYPE_NUMSEATS_OTHER, 
                    EfaTypes.TYPE_RIGGING_OTHER, EfaTypes.TYPE_COXING_OTHER, Boolean.toString(true));
            String[] name = BoatRecord.tryGetNameAndAffix(s);
            if (name != null && name[0] != null) {
                r.setName(name[0]);
            }
            if (name != null && name[1] != null) {
                r.setNameAffix(name[1]);
            }
        }
        BoatEditDialog dlg = new BoatEditDialog(this, r, newRecord, getAdmin());
        dlg.showDialog();
        if (dlg.getDialogResult()) {
            item.parseAndShowValue(r.getQualifiedName());
            item.setChanged();
            currentBoatUpdateGui();
        }
        efaBaseFrameFocusManager.focusNextItem(item, item.getComponent());
    }

    void editPerson(ItemTypeStringAutoComplete item) {
        if (!isLogbookReady()) {
            return;
        }
        String s = item.getValueFromField().trim();
        if (s.length() == 0) {
            return;
        }
        PersonRecord r = findPerson(item, getValidAtTimestamp(null));
        if (r == null) {
            r = findPerson(item, -1);
        }
        if (isModeBoathouse() || getMode() == MODE_ADMIN_SESSIONS) {
            if (!Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenNamenHinzufuegen() || r != null) {
                return; // only add new persons (if allowed), but don't edit existing ones
            }
        }
        boolean notifyAdmin = false;
        boolean newRecord = (r == null);
        if (r == null) {
            r = Daten.project.getPersons(false).createPersonRecord(UUID.randomUUID());
            String[] name = PersonRecord.tryGetFirstLastNameAndAffix(s);
            boolean anyNameSet = false;
            if (name != null && name[0] != null) {
                r.setFirstName(name[0]);
                anyNameSet = true;
            }
            if (name != null && name[1] != null) {
                r.setLastName(name[1]);
                anyNameSet = true;
            }
            if (name != null && name[2] != null) {
                r.setNameAffix(name[2]);
                anyNameSet = true;
            }
            if (!anyNameSet && s != null) {
                r.setFirstName(s);
            }
            if (getAdmin() == null || !getAdmin().isAllowedEditPersons()) {
                r.setStatusId(Daten.project.getStatus(false).getStatusOther().getId());
                notifyAdmin = true;
            }
        }
        PersonEditDialog dlg = (new PersonEditDialog(this, r, newRecord, getAdmin()));
        dlg.showDialog();
        if (dlg.getDialogResult()) {
            if (notifyAdmin) {
                String msgTitle = International.getString("Eine neue Person wurde der Personenliste hinzugefügt.");
                String msg = msgTitle + "\n" +
                        International.getString("Person") + " " + r.getQualifiedName() + ": " + r.toString();
                Logger.log(Logger.INFO, Logger.MSG_EVT_PERSONADDED, msg);
                Daten.project.getMessages(false).createAndSaveMessageRecord(MessageRecord.TO_ADMIN,
                        msgTitle, msg);
            }
            item.parseAndShowValue(r.getQualifiedName());
            item.setChanged();
            
        }
        efaBaseFrameFocusManager.focusNextItem(item, item.getComponent());
    }

    void editDestination(ItemTypeStringAutoComplete item) {
        if (!isLogbookReady()) {
            return;
        }
        String s = item.getValueFromField().trim();
        if (s.length() == 0) {
            return;
        }
        DestinationRecord r = findDestination(getValidAtTimestamp(null));
        if (r == null) {
            r = findDestination(-1);
        }
        if (isModeBoathouse() || getMode() == MODE_ADMIN_SESSIONS) {
            if (!Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenNamenHinzufuegen() || r != null) {
                return; // only add new destinations (if allowed), but don't edit existing ones
            }
        }
        boolean newRecord = (r == null);
        if (r == null) {
            r = Daten.project.getDestinations(false).createDestinationRecord(UUID.randomUUID());
            String[] name = DestinationRecord.tryGetNameAndVariant(s);
            if (name != null && name[0] != null) {
                r.setName(name[0]);
            }
        }
        DestinationEditDialog dlg = new DestinationEditDialog(this, r, newRecord, getAdmin());
        dlg.showDialog();
        if (dlg.getDialogResult()) {
            item.parseAndShowValue(r.getQualifiedName());
            item.setChanged();
            String distBefore = distance.getValueFromField();
            setDesinationDistance();
            if (distance.getValueFromField().length() == 0 && distBefore.length() > 0) {
                // if there was a distance set, but the new/changed destination record does not
                // specify a distance, then keep the distance that was previously set!
                distance.parseAndShowValue(distBefore);
            }
        }
        efaBaseFrameFocusManager.focusNextItem(item, item.getComponent());
    }

    void selectSessionGroup() {
        if (!isLogbookReady()) {
            return;
        }
        UUID id = null;
        if (currentRecord != null) {
            id = currentRecord.getSessionGroupId();
        }
        SessionGroupListDialog dlg = new SessionGroupListDialog(this, logbook.getName(), id, getAdmin());
        dlg.showDialog();
        if (dlg.getDialogResult()) {
            SessionGroupRecord r = dlg.getSelectedSessionGroupRecord();
            if (r == null) {
                sessiongroup.parseAndShowValue("");
                sessiongroup.setRememberedId(null);
            } else {
                sessiongroup.parseAndShowValue(r.getName());
                sessiongroup.setRememberedId(r.getId());
            }
        }
    }
    

    // =========================================================================
    // Save Entry Checks
    // =========================================================================

    private String getLogbookRecordStringWithEntryNo() {
        return International.getMessage("Fahrtenbucheintrag #{entryno}",
                entryno.getValueFromField());
    }

    protected boolean checkMisspelledInput() {
        PersonRecord r;
        for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
            ItemTypeStringAutoComplete field = this.getCrewItem(i);
            String s = field.getValueFromField().trim();
            if (s.length() == 0) {
                continue;
            }
            r = findPerson(i, getValidAtTimestamp(null));
            if (r == null) {
                // check for comma without blank
                int pos = s.indexOf(",");
                if (pos > 0 && pos+1 < s.length() && s.charAt(pos+1) != ' ') {
                    field.parseAndShowValue(s.substring(0, pos) + ", " + s.substring(pos+1));
                }
            }
        }
        return true;
    }

    protected boolean checkDuplicatePersons() {
        // Ruderer auf doppelte prüfen
    	Hashtable<UUID,String> h = new Hashtable<UUID,String>();
    	
        String doppelt = null; // Ergebnis doppelt==null heißt ok, doppelt!=null heißt Fehler! ;-)
        while (true) { // Unsauber; aber die Alternative wäre ein goto; dies ist keine Schleife!!
            PersonRecord r;
            for (int i=0; i <= LogbookRecord.CREW_MAX; i++) {
                r = findPerson(i, getValidAtTimestamp(null));
                if (r != null) {
                    UUID id = r.getId();
                    if (h.get(id) == null) {
                        h.put(id, "");
                    } else {
                        doppelt = r.getQualifiedName();
                        break;
                    }
                }
            }
            break; // alles ok, keine doppelten --> Pseudoschleife abbrechen
        }
        if (doppelt != null) {
            Dialog.error(International.getMessage("Die Person '{name}' wurde mehrfach eingegeben!", doppelt));
            return false;
        }
        return true;
    }

    private boolean checkPersonsForBoatType() {
        // bei steuermannslosen Booten keinen Steuermann eingeben
        if (cox.getValueFromField().trim().length() > 0 && currentBoatTypeCoxing != null) {
            if (currentBoatTypeCoxing.equals(EfaTypes.TYPE_COXING_COXLESS)) {
                int ret = Dialog.yesNoDialog(International.getString("Steuermann"),
                        International.getString("Du hast für ein steuermannsloses Boot einen Steuermann eingetragen.") + "\n" +
                        International.getString("Trotzdem speichern?"));
                if (ret != Dialog.YES) {
                    return false;
                }
            }
        }

        // disabled functionality: for unknown boats: If only one person entered as cox, change to crew1
        if (Daten.efaConfig.getValueFixCoxForCoxlessUnknownBoats() &&
             (getMode() == MODE_BOATHOUSE_START ||
              getMode() == MODE_BOATHOUSE_START_CORRECT ||
              getMode() == MODE_BOATHOUSE_START_MULTISESSION ||
              getMode() == MODE_BOATHOUSE_LATEENTRY ||
              getMode() == MODE_BOATHOUSE_LATEENTRY_MULTISESSION) &&
            currentBoat == null && cox.getValueFromField().trim().length() > 0 &&
            getNumberOfPersonsInBoat() == 1 && crew[0].isVisible()) {
            crew[0].parseAndShowValue(cox.getValueFromField().trim());
            cox.parseAndShowValue("");
        }

        return true;
    }

    private boolean checkDuplicateEntry() {
        // Prüfen, ob ein Doppeleintrag vorliegt
        if (isModeBoathouse()) {
            LogbookRecord duplicate = logbook.findDuplicateEntry(getFields(), 25); // search last 25 logbook entries for potential duplicates
            if (duplicate != null) {
                Vector<String> v = duplicate.getAllCoxAndCrewAsNames();
                String m = "";
                for (int i = 0; i < v.size(); i++) {
                    m += (m.length() > 0 ? "; " : "") + v.get(i);
                }
                switch (Dialog.auswahlDialog(International.getString("Doppeleintrag") + "?",
                        // @todo (P5) make duplicate entry dialog a bit more readable
                        International.getString("efa hat einen ähnlichen Eintrag im Fahrtenbuch gefunden.") + "\n"
                        + International.getString("Eventuell hast Du oder jemand anderes die Fahrt bereits eingetragen.") + "\n\n"
                        + International.getString("Vorhandener Eintrag") + ":\n"
                        + International.getMessage("#{entry} vom {date} mit {boat}",
                        duplicate.getEntryId().toString(), duplicate.getDate().toString(), duplicate.getBoatAsName()) + ":\n"
                        + International.getString("Mannschaft") + ": " + m + "\n"
                        + International.getString("Abfahrt") + ": " + (duplicate.getStartTime() != null ? duplicate.getStartTime().toString() : "") + "; "
                        + International.getString("Ankunft") + ": " + (duplicate.getEndTime() != null ? duplicate.getEndTime().toString() : "") + "; "
                        + International.getString("Ziel") + " / " +
                          International.getString("Strecke") + ": " + duplicate.getDestinationAndVariantName() + " (" + (duplicate.getDistance() != null ? duplicate.getDistance().getAsFormattedString() : "") + " Km)" + "\n\n"
                        + International.getString("Bitte füge den aktuellen Eintrag nur hinzu, falls es sich NICHT um einen Doppeleintrag handelt.") + "\n"
                        + International.getString("Was möchtest Du tun?"),
                        International.getString("Eintrag hinzufügen")
                        + " (" + International.getString("kein Doppeleintrag") + ")",
                        International.getString("Eintrag nicht hinzufügen")
                        + " (" + International.getString("Doppeleintrag") + ")",
                        International.getString("Zurück zum Eintrag"))) {
                    case 0: // kein Doppeleintrag: Hinzufügen
                        break;
                    case 1: // Doppeleintrag: NICHT hinzufügen
                        cancel();
                        return false;
                    default: // Zurück zum Eintrag
                        return false;
                }
            }
        }
        return true;
    }

    private boolean checkEntryNo() {
        DataTypeIntString newEntryNo = DataTypeIntString.parseString(entryno.getValue());
        while ((logbook.getLogbookRecord(newEntryNo) != null && (isNewRecord || !newEntryNo.equals(currentRecord.getEntryId())))
                || newEntryNo.length() == 0) {
            if (isNewRecord && isModeBoathouse()) {
                // duplicate EntryNo's for new sessions in efa-Boathouse can happen in case
                // of simultaneous remote access.
                // if this happens, just increase number by one
                entryno.parseAndShowValue(Integer.toString(newEntryNo.intValue() + 1));
                newEntryNo = DataTypeIntString.parseString(entryno.getValue());
                continue;
            }
            Dialog.error(International.getString("Diese Laufende Nummer ist bereits vergeben!") + " "
                    + International.getString("Bitte korrigiere die laufende Nummer des Eintrags!") + "\n\n"
                    + International.getString("Hinweis") + ": "
                    + International.getString("Um mehrere Einträge unter 'derselben' Nummer hinzuzufügen, "
                    + "füge einen Buchstaben von A bis Z direkt an die Nummer an!"));
            entryno.requestFocus();
            return false;
        }

        if (isNewRecord || currentRecord == null) {
            // erstmal prüfen, ob die Laufende Nummer korrekt ist
            DataTypeIntString highestEntryNo = new DataTypeIntString(" ");
            try {
                LogbookRecord r = (LogbookRecord) (logbook.data().getLast());
                if (r != null) {
                    highestEntryNo = r.getEntryId();
                }
            } catch (Exception e) {
                Logger.logdebug(e);
            }
            if (newEntryNo.compareTo(highestEntryNo) <= 0 && !isInsertedRecord) {
                boolean printWarnung = true;
                if (entryNoForNewEntry > 0 && newEntryNo.intValue() == entryNoForNewEntry + 1) {
                    printWarnung = false;
                }
                if (printWarnung && // nur warnen, wenn das erste Mal eine zu kleine LfdNr eingegeben wurde!
                        Dialog.yesNoDialog(International.getString("Warnung"),
                        International.getString("Die Laufende Nummer dieses Eintrags ist kleiner als die des "
                        + "letzten Eintrags.") + " " +
                        International.getString("Trotzdem speichern?")) == Dialog.NO) {
                    entryno.requestFocus();
                    return false;
                }
            }
            entryNoForNewEntry = EfaUtil.string2date(entryno.getValue(), 1, 1, 1).tag; // lfdNr merken, nächster Eintrag erhält dann per default diese Nummer + 1
        } else { // geänderter Fahrtenbucheintrag
            if (!currentRecord.getEntryId().toString().equals(entryno.toString())) {
                if (Dialog.yesNoDialog(International.getString("Warnung"),
                        International.getString("Du hast die Laufende Nummer dieses Eintrags verändert!") + " " +
                        International.getString("Trotzdem speichern?")) == Dialog.NO) {
                    entryno.requestFocus();
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkBoatCaptain() {
        // falls noch nicht geschehen, ggf. automatisch Obmann auswählen
        if (Daten.efaConfig.getValueAutoObmann() && getBoatCaptain() < 0) {
            autoSelectBoatCaptain();
        }

        // Obmann-Auswahl (Autokorrektur, neu in 1.7.1)
        int boatCaptain = getBoatCaptain();
        if (boatCaptain == 0 && cox.getValue().length() == 0 && crew[0].getValue().length() > 0) {
            setBoatCaptain(1, true);
        }
        if (boatCaptain > 0 && crew[boatCaptain - 1].getValue().length() == 0 && cox.getValue().length() > 0) {
            setBoatCaptain(0, true);
        }
        if (boatCaptain > 0 && crew[boatCaptain - 1].getValue().length() == 0 && crew[0].getValue().length() > 0) {
            setBoatCaptain(1, true);
        }
        boatCaptain = getBoatCaptain();

        // just to be really sure... if we hide boatcaptain field, but the wrong one is selected, fall back to autoselect
        if ((boatCaptain == 0 && cox.getValue().length() == 0)
                || (boatCaptain > 0 && crew[boatCaptain - 1].getValue().length() == 0)) {
            if (!boatcaptain.isVisible()) {
                autoSelectBoatCaptain(true);
            }
        }

        // Obmann-Check
        if ((boatCaptain == 0 && cox.getValue().length() == 0)
                || (boatCaptain > 0 && crew[boatCaptain - 1].getValue().length() == 0)) {
            Dialog.error(International.getString("Bitte wähle als Obmann eine Person aus, die tatsächlich im Boot sitzt!"));
            boatcaptain.setVisible(true);
            boatcaptain.requestFocus();
            return false;
        }

        if (Daten.efaConfig.getValueEfaDirekt_eintragErzwingeObmann() && boatCaptain < 0) {
            Dialog.error(International.getString("Bitte wähle einen Obmann aus!"));
            boatcaptain.setVisible(true);
            boatcaptain.requestFocus();
            return false;
        }

        return true;
    }

    private boolean checkBoatStatus() {
        if (getMode() == MODE_BOATHOUSE_START || getMode() == MODE_BOATHOUSE_START_CORRECT || getMode() == MODE_BOATHOUSE_START_MULTISESSION) {
            int checkMode = 3;
            // checkFahrtbeginnFuerBoot nur bei direkt_boot==null machen, da ansonsten der Check schon in EfaDirektFrame gemacht wurde
            if (efaBoathouseAction != null) {
                if (efaBoathouseAction.boat == null || // when called from EfaBoathouseFrame before boat is entered
                        (currentBoat != null && currentBoat.getId() != null && // boat changed as part of START_CORRECT
                        efaBoathouseAction.boat.getId() != currentBoat.getId())) {
                    checkMode = 2;
                    efaBoathouseAction.boat = currentBoat;
                }
                if (efaBoathouseAction.boat != null) {
                    // update boat status (may have changed since we opened the dialog)
                    efaBoathouseAction.boatStatus = efaBoathouseAction.boat.getBoatStatus();
                }
                boolean success = efaBoathouseFrame.checkStartSessionForBoat(efaBoathouseAction,
                        entryno.getValueFromField(), checkMode);
                if (!success) {
                    efaBoathouseAction.boat = null; // otherwise next check would fail
                    boat.requestFocus();
                    boat.setSelection(0, 255);
                }
                return success;
            }
        }
        return true;
    }

    protected boolean checkMultiDayTours() {
        // Prüfen, ob Eintrag einer Mehrtagesfahrt vorliegt und das Datum in den Zeitraum der Mehrtagesfahrt fällt
        if (isModeBoathouse()) {
            return true;
        }
        UUID sgId = (UUID)sessiongroup.getRememberedId();
        SessionGroupRecord g = (sgId != null ? Daten.project.getSessionGroups(false).findSessionGroupRecord(sgId) : null);
        if (!date.getDate().isSet()) {
            return true; // shouldn't happen
        }
        if (g != null) {
            DataTypeDate entryStartDate = date.getDate();
            DataTypeDate entryEndDate = enddate.getDate();
            DataTypeDate groupStartDate = g.getStartDate();
            DataTypeDate groupEndDate = g.getEndDate();
            if (entryStartDate.isBefore(groupStartDate) || entryStartDate.isAfter(groupEndDate) ||
                (entryEndDate.isSet() && (entryEndDate.isBefore(groupStartDate) || entryEndDate.isAfter(groupEndDate))) ) {
                Dialog.error(International.getMessage("Das Datum des Fahrtenbucheintrags {entry} liegt außerhalb des Zeitraums, "
                        + "der für die ausgewählte Fahrtgruppe '{name}' angegeben wurde.",
                        entryno.getValue(), g.getName()));
                return false;
            }
        }
        return true;
    }
    
    protected boolean checkSessionType() {
        if (!isNewRecord && !isModeBoathouse() && !sessiontype.isChanged()) {
            return true;
        }
        String sessType = (sessiontype.isVisible() ? sessiontype.getValue() : null);
        if (sessType == null) {
            return true;
        }
        String dest = destination.getValue();
        long dist = distance.getValue().getValueInMeters();
        
        String newSessType = null;
        if (dest != null) {
            dest = dest.toLowerCase();
            if (dest.indexOf(International.getString("Regatta").toLowerCase()) >= 0) {
                newSessType = EfaTypes.TYPE_SESSION_REGATTA;
                if (sessType.equals(EfaTypes.TYPE_SESSION_JUMREGATTA)) {
                    newSessType = EfaTypes.TYPE_SESSION_JUMREGATTA; // treat as same
                }
            }
            if (dest.indexOf(International.getString("Trainingslager").toLowerCase()) >= 0) {
                newSessType = EfaTypes.TYPE_SESSION_TRAININGCAMP;
            }
            if (dest.indexOf(International.getString("Nachtrag").toLowerCase()) >= 0) {
                newSessType = EfaTypes.TYPE_SESSION_LATEENTRY;
            }
            if (newSessType != null && !sessType.equals(newSessType)) {
                if (Dialog.yesNoDialog(International.getString("Fahrtart"), 
                        International.getMessage("Ist diese Fahrt ein(e) {sessiontype}?",
                            Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, newSessType))) == Dialog.YES) {
                    sessType = newSessType;
                    sessiontype.parseAndShowValue(newSessType);
                }
            }
        }
        if (dist >= 30000) {
            newSessType = null;
            if (!sessType.equals(EfaTypes.TYPE_SESSION_JUMREGATTA) &&
                !sessType.equals(EfaTypes.TYPE_SESSION_LATEENTRY) &&
                !sessType.equals(EfaTypes.TYPE_SESSION_REGATTA) &&
                !sessType.equals(EfaTypes.TYPE_SESSION_TRAININGCAMP) &&
                !sessType.equals(EfaTypes.TYPE_SESSION_TOUR)    ) {
                if (sessType.equals(EfaTypes.TYPE_SESSION_TRAINING)) {
                    if (Dialog.yesNoDialog(International.getString("Fahrtart"),
                            International.getMessage("Ist diese Fahrt ein(e) {sessiontype}?",
                            Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_TRAININGCAMP))) == Dialog.YES) {
                        newSessType = EfaTypes.TYPE_SESSION_TRAININGCAMP;
                    }
                } else {
                    ArrayList<String> sessTypeSelection = new ArrayList<String>();
                    if (Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_TOUR)) {
                        sessTypeSelection.add(EfaTypes.TYPE_SESSION_TOUR);
                    }
                    if (Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_TRAININGCAMP)) {
                        sessTypeSelection.add(EfaTypes.TYPE_SESSION_TRAININGCAMP);
                    }
                    // auf Wunsch von Gabi vom 07.12.2016 entfernt
                    //if (Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_LATEENTRY)) {
                    //    sessTypeSelection.add(EfaTypes.TYPE_SESSION_LATEENTRY);
                    //}
                    if (Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_REGATTA)) {
                        sessTypeSelection.add(EfaTypes.TYPE_SESSION_REGATTA);
                    }
                    if (Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_SESSION, EfaTypes.TYPE_SESSION_JUMREGATTA)) {
                        sessTypeSelection.add(EfaTypes.TYPE_SESSION_JUMREGATTA);
                    }
                    if (Daten.efaTypes.isConfigured(EfaTypes.CATEGORY_SESSION, sessType)) {
                        sessTypeSelection.add(sessType);
                    }
                    ArrayList<String> sessTypeDisplay = new ArrayList<String>();
                    for (String type : sessTypeSelection) {
                        sessTypeDisplay.add(Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, type));
                    }
                    if (sessTypeDisplay.size()>0) {
                    	if (sessTypeDisplay.size()>1) {
	                    	int res = (Dialog.auswahlDialog(International.getString("Fahrtart"),
		                            International.getMessage("Ist diese Fahrt ein(e) {sessiontype}?", "..."),
		                            sessTypeDisplay.toArray(new String[0])));
		                    if (res >= 0 && res < sessTypeSelection.size()) {
		                        newSessType = sessTypeSelection.get(res);
		                    }
                    	} else {
                    		newSessType=sessTypeDisplay.get(0);
                    	}
                    }
                }
            }
            if (newSessType != null) {
                sessType = newSessType;
                sessiontype.parseAndShowValue(newSessType);
            }
        }
        return true;
    }

    protected boolean checkDate() {
        if (date.isSet() && enddate.isSet() && !date.getDate().isBefore(enddate.getDate())) {
            String msg = International.getString("Das Enddatum muß nach dem Startdatum liegen.");
            Dialog.error(msg);
            enddate.requestFocus();
            return false;
        }
        return true;
    }

    protected boolean checkTime() {
        if (isModeBoathouse()) {
            if (starttime.isVisible() && !starttime.isSet()) {
                setTime(starttime, Daten.efaConfig.getValueEfaDirekt_plusMinutenAbfahrt(), null);
            }
            if (endtime.isVisible() && !endtime.isSet() &&
                (getMode() == MODE_BOATHOUSE_FINISH || getMode() == MODE_BOATHOUSE_LATEENTRY || getMode() == MODE_BOATHOUSE_LATEENTRY_MULTISESSION)) {
                setTime(endtime, -Daten.efaConfig.getValueEfaDirekt_minusMinutenAnkunft(), starttime.getTime());
            }

            // check whether end time is after start time (or multi day tour)
            if (starttime.isVisible() && endtime.isVisible() &&
                starttime.isSet() && endtime.isSet() &&
                starttime.getTime().isAfter(endtime.getTime()) &&
                endtime.isEditable() &&
                !enddate.isSet()) {
                if (Dialog.yesNoDialog(
                        International.getMessage("Ungültige Eingabe im Feld '{field}'",
                        International.getString("Zeit")),
                        International.getString("Bitte überprüfe die eingetragenen Uhrzeiten.") + "\n" +
                        International.getString("Ist dieser Eintrag eine Mehrtagsfahrt?")) == Dialog.YES) {
                    enddate.expandToField();
                    enddate.requestFocus();
                    return false;
                }
                endtime.requestFocus();
                return false;

            }

            // check whether the elapsed time is long enough
            String sType = (sessiontype.isVisible() ? sessiontype.getValue() : null);
            if (!sType.equals(EfaTypes.TYPE_SESSION_LATEENTRY) &&
                !sType.equals(EfaTypes.TYPE_SESSION_TRAININGCAMP) &&
                starttime.isVisible() && endtime.isVisible() && distance.isVisible() &&
                starttime.isSet() && endtime.isSet() && endtime.isEditable() && distance.isSet()) {
                long timediff = Math.abs(endtime.getTime().getTimeAsSeconds() - starttime.getTime().getTimeAsSeconds());
                long dist = distance.getValue().getValueInMeters();
                if (timediff < 15*60 && 
                    timediff < dist/10 &&
                    dist < 100 * 1000) {
                    // if a short elapsed time (anything less than 15 minutes) has been entered,
                    // then check whether it is plausible; plausible times need to be at least
                    // above 1 s per 10 meters; everything else is unplausible
                    // we skip the check if the distance is >= 100 Km (that's probably a late entry
                    // then).
                    String msg = International.getString("Bitte überprüfe die eingetragenen Uhrzeiten.");
                    Dialog.error(msg);
                    endtime.requestFocus();
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean checkAllowedDateForLogbook() {
        long tRec = getValidAtTimestamp(null);
        if (tRec < logbookValidFrom || tRec >= logbookInvalidFrom) {
            if (getMode() != MODE_BOATHOUSE_LATEENTRY) {
            	// Only LateEntry for a single item may save the entry to an xml file for later.
            	// MODE_BOATHOUSE_LATEENTRY_MULTIPLE: the user MUST use the correct date for late entry,
            	// we do not want the admin to have some 2 to unlimited xml records on the file system to import.
                String msg = getLogbookRecordStringWithEntryNo() + ": "
                        + International.getMessage("Der Eintrag kann nicht gespeichert werden, da er außerhalb des gültigen Zeitraums ({startdate} - {enddate}) "
                        + "für dieses Fahrtenbuch liegt.", logbook.getStartDate().toString(), logbook.getEndDate().toString());
                Logger.log(Logger.WARNING, Logger.MSG_EVT_ERRORADDRECORDOUTOFRANGE, msg + " (" + getFields().toString() + ")");
                Dialog.error(msg);
                date.requestFocus();
            } else {
                currentRecord = getFields();
                String msg = International.getMessage("Das Datum {date} liegt außerhalb des Zeitraums " + 
                        "für dieses Fahrtenbuch ({dateFrom} - {dateTo}) und kann daher nicht gespeichert werden. " +
                        "Du kannst diesen Eintrag aber zum Nachtrag an den Administrator senden.",
                        (currentRecord.getDate() != null ? currentRecord.getDate().toString() : "?"),
                        logbook.getStartDate().toString(), logbook.getEndDate().toString()) + "\n" +
                        International.getString("Was möchtest Du tun?");
                switch(Dialog.auswahlDialog(getLogbookRecordStringWithEntryNo(), msg,
                        International.getString("Datum korrigieren"),
                        International.getString("Nachtrag an Admin senden"), true)) {
                    case 0:
                        date.requestFocus();
                        break;
                    case 1:
                        String fname = Daten.efaTmpDirectory + "entry_" +
                                EfaUtil.getCurrentTimeStampYYYYMMDD_HHMMSS() + ".xml";
                        String entryNo = getLogbookRecordStringWithEntryNo();
                        currentRecord.setEntryId(null);
                        if (currentRecord.saveRecordToXmlFile(fname)) {
                            Daten.project.getMessages(false).createAndSaveMessageRecord(
                                    MessageRecord.TO_ADMIN, International.getString("Nachtrag"),
                                    International.getMessage("Ein Nachtrag für {datum} konnte im Fahrtenbuch {logbook} nicht gespeichert werden, " +
                                                             "da sein Datum außerhalb des Zeitraums für dieses Fahrtenbuch liegt ({dateFrom} - {dateTo}).",
                                                             currentRecord.getDate().toString(), logbook.getName(),
                                                             logbook.getStartDate().toString(), logbook.getEndDate().toString()) + "\n\n" +
                                                             currentRecord.getLogbookRecordAsStringDescription() + "\n\n"+
                                                             International.getMessage("Der Eintrag wurde als Importdatei {name} abgespeichert " +
                                                                                      "und kann durch Import dieser Datei zum entsprechenden Fahrtenbuch hinzugefügt werden.",
                                                                                      fname));
                            Dialog.infoDialog(International.getMessage("{entry} wurde zum Nachtrag an den Admin geschickt und wird erst nach der Bestätigung durch den Admin sichtbar sein.",
                                    entryNo));
                            cancel();
                        } else {
                            Dialog.error(LogString.operationFailed(International.getString("Nachtrag an Admin senden")));
                        }
                        break;
                    default:
                        break;
                }
            }
            return false;
        }
        return true;
    }

    protected boolean checkAllDataEnteredBoatAndCrew() {
    	
    	if (isModeBoathouse()) {
            if (boat.getValue().length() == 0) {
                Dialog.error(International.getString("Bitte gib einen Bootsnamen ein!"));
                boat.requestFocus();
                return false;
            }
            if (getNumberOfPersonsInBoat() == 0) {
                Dialog.error(International.getString("Bitte trage mindestens eine Person ein!"));
                if (cox.isEditable()) {
                    cox.requestFocus();
                } else {
                    crew[0].requestFocus();
                }
                return false;
            }	
        }
        return true;
    }
    
    protected boolean checkAllDataEntered() {
        if (isModeBoathouse()) {

        	if (!checkAllDataEnteredBoatAndCrew()) {return false;}

            // Ziel vor Fahrtbeginn eintragen
            if ((mode == MODE_BOATHOUSE_START || mode == MODE_BOATHOUSE_START_CORRECT || mode == MODE_BOATHOUSE_START_MULTISESSION)
                    && Daten.efaConfig.getValueEfaDirekt_zielBeiFahrtbeginnPflicht() && destination.getValue().length() == 0) {
                Dialog.error(International.getString("Bitte trage ein voraussichtliches Fahrtziel/Strecke ein!"));
                destination.requestFocus();
                return false;
            }

            if ((mode == MODE_BOATHOUSE_FINISH || mode == MODE_BOATHOUSE_LATEENTRY || mode == MODE_BOATHOUSE_LATEENTRY_MULTISESSION) &&
                destination.getValue().length() == 0) {
                Dialog.error(International.getString("Bitte trage ein Fahrtziel/Strecke ein!"));
                destination.requestFocus();
                return false;
            }

            // Waters
            if ((mode == MODE_BOATHOUSE_FINISH || mode == MODE_BOATHOUSE_LATEENTRY || mode == MODE_BOATHOUSE_LATEENTRY_MULTISESSION) &&
                Daten.efaConfig.getValueEfaDirekt_gewaesserBeiUnbekanntenZielenPflicht() &&
                waters.isVisible() && waters.getValue().length() == 0) {
                Dialog.error(International.getString("Bitte trage ein Gewässer ein!"));
                waters.requestFocus();
                return false;
            }

            // Distance
            if ((!distance.isSet() || distance.getValue().getValueInDefaultUnit() == 0)) {
                if (mode == MODE_BOATHOUSE_FINISH || mode == MODE_BOATHOUSE_LATEENTRY || mode == MODE_BOATHOUSE_LATEENTRY_MULTISESSION) {
                    if (!Daten.efaConfig.getValueAllowSessionsWithoutDistance()) {
                        Dialog.error(International.getString("Bitte trage die gefahrenen Entfernung ein!"));
                        distance.requestFocus();
                        return false;
                    }
                }
                if (isModeFull()) {
                    if (Dialog.yesNoDialog(International.getString("Warnung"),
                            International.getString("Keine Kilometer eingetragen.") + "\n"
                            + International.getString("Trotzdem speichern?")) == Dialog.NO) {
                        distance.requestFocus();
                        return false;
                    }
                }
            }

        }
        return true;
    }

    protected boolean ingoreNameInvalid(String name, long validAt, String type, IItemType field) {
        String msg = International.getMessage("{type} '{name}' ist zum Zeitpunkt {dateandtime} ungültig.",
                type, name, EfaUtil.getTimeStamp(validAt));
        if (this.isModeBoathouse()) {
            // don't prompt, warn only
            LogbookRecord r = getFields();
            Logger.log(Logger.WARNING, Logger.MSG_EVT_ERRORRECORDINVALIDATTIME,
                    getLogbookRecordStringWithEntryNo() + ": " + msg +
                    (r != null ? " (" + r.toString() + ")" : ""));
            return true;

        }
        if (Dialog.auswahlDialog(International.getString("Warnung"),
                msg,
                International.getString("als unbekannten Namen speichern"),
                International.getString("Abbruch"), false) != 0) {
            field.requestFocus();
            return false;
        } else {
            return true;
        }
    }

    /*
    private boolean checkNamesValid() {
        // Prüfen, ocb ein eingetragener Datensatz zum angegebenen Zeitpunkt ungültig ist
        long preferredValidAt = getValidAtTimestamp(null);

        String name = boat.getValueFromField();
        if (name != null && name.length() > 0) {
            BoatRecord r = findBoat(preferredValidAt);
            if (r == null) {
                r = findBoat(-1);
            }
            if (preferredValidAt > 0 && r != null && !r.isValidAt(preferredValidAt)) {
                if (!ingoreNameInvalid(r.getQualifiedName(), preferredValidAt,
                                       International.getString("Boot"), boat)) {
                    return false;
                }
            }
        }

        for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
            name = (i == 0 ? cox : crew[i - 1]).getValueFromField();
            if (name != null && name.length() > 0) {
                PersonRecord r = findPerson(i, preferredValidAt);
                if (r == null) {
                    r = findPerson(i, -1);
                }
                if (preferredValidAt > 0 && r != null && !r.isValidAt(preferredValidAt)) {
                    if (!ingoreNameInvalid(r.getQualifiedName(), preferredValidAt,
                            International.getString("Person"), (i == 0 ? cox : crew[i-1]))) {
                        return false;
                    }
                }
            }
        }

        name = destination.getValueFromField();
        if (name != null && name.length() > 0) {
            DestinationRecord r = findDestination(preferredValidAt);
            if (r == null) {
                r = findDestination(-1);
            }
            if (preferredValidAt > 0 && r != null && !r.isValidAt(preferredValidAt)) {
                if (!ingoreNameInvalid(r.getQualifiedName(), preferredValidAt,
                                       International.getString("Ziel"), destination)) {
                    return false;
                }
            }
        }

        return true;
    }
    */
    protected boolean checkBoatNameValid(ItemTypeStringAutoComplete boatItem) {
        // Prüfen, ocb ein eingetragener Datensatz zum angegebenen Zeitpunkt ungültig ist
        long preferredValidAt = getValidAtTimestamp(null);

        String name = boatItem.getValueFromField();
        if (name != null && name.length() > 0) {
            BoatRecord r = findBoat(boatItem, preferredValidAt);
            if (r == null) {
                r = findBoat(boatItem, -1);
            }
            if (preferredValidAt > 0 && r != null && !r.isValidAt(preferredValidAt)) {
                if (!ingoreNameInvalid(r.getQualifiedName(), preferredValidAt,
                                       International.getString("Boot"), boatItem)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkCrewNamesValid() {
        long preferredValidAt = getValidAtTimestamp(null);
    	String name;

    	for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
            name = (i == 0 ? cox : crew[i - 1]).getValueFromField();
            if (name != null && name.length() > 0) {
                PersonRecord r = findPerson(i, preferredValidAt);
                if (r == null) {
                    r = findPerson(i, -1);
                }
                if (preferredValidAt > 0 && r != null && !r.isValidAt(preferredValidAt)) {
                    if (!ingoreNameInvalid(r.getQualifiedName(), preferredValidAt,
                            International.getString("Person"), (i == 0 ? cox : crew[i-1]))) {
                        return false;
                    }
                }
            }
        }
    	return true;
    }

    protected boolean checkDestinationNameValid() {
        long preferredValidAt = getValidAtTimestamp(null);
    	String name = destination.getValueFromField();
        if (name != null && name.length() > 0) {
            DestinationRecord r = findDestination(preferredValidAt);
            if (r == null) {
                r = findDestination(-1);
            }
            if (preferredValidAt > 0 && r != null && !r.isValidAt(preferredValidAt)) {
                if (!ingoreNameInvalid(r.getQualifiedName(), preferredValidAt,
                                       International.getString("Ziel"), destination)) {
                    return false;
                }
            }
        }
        return true;
    }    
    
    /*
    private boolean checkUnknownNames() {
        // Prüfen, ob ggf. nur bekannte Boote/Ruderer/Ziele eingetragen wurden
        if (isModeBoathouse()) {
            if (Daten.efaConfig.getValueEfaDirekt_eintragNurBekannteBoote()) {
                String name = boat.getValueFromField();
                if (name != null && name.length() > 0 && findBoat(getValidAtTimestamp(null)) == null) {
                    Dialog.error(LogString.itemIsUnknown(name, International.getString("Boot")));
                    boat.requestFocus();
                    return false;
                }
            }
            if (Daten.efaConfig.getValueEfaDirekt_eintragNurBekannteRuderer()) {
                for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                    String name = (i == 0 ? cox : crew[i-1]).getValueFromField();
                    if (name != null && name.length() > 0 && findPerson(i, getValidAtTimestamp(null)) == null) {
                    Dialog.error(LogString.itemIsUnknown(name, International.getString("Person")));
                        if (i == 0) {
                            cox.requestFocus();
                        } else {
                            crew[i-1].requestFocus();
                        }
                        return false;
                    }
                }
            }
            if (Daten.efaConfig.getValueEfaDirekt_eintragNurBekannteZiele()) {
                String name = destination.getValueFromField();
                if (name != null && name.length() > 0 && findDestination(getValidAtTimestamp(null)) == null) {
                    Dialog.error(LogString.itemIsUnknown(name, International.getString("Ziel/Strecke")));
                    destination.requestFocus();
                    return false;
                }
            }

            if (Daten.efaConfig.getValueEfaDirekt_eintragNurBekannteGewaesser() && waters.isVisible()) {
                DataTypeList[] wlists = findWaters(waters);
                if (wlists != null && wlists.length != 0 && wlists[1].length() > 0) {
                    Dialog.error(LogString.itemIsUnknown(wlists[1].toString(), International.getString("Gewässer")));
                    waters.requestFocus();
                    return false;
                }
            }
        }
        return true;
    }
    */
    protected boolean checkUnknownNamesBoat() {
        // Prüfen, ob ggf. nur bekannte Boote/Ruderer/Ziele eingetragen wurden
        if (isModeBoathouse()) {
            if (Daten.efaConfig.getValueEfaDirekt_eintragNurBekannteBoote()) {
                String name = boat.getValueFromField();
                if (name != null && name.length() > 0 && findBoat(boat, getValidAtTimestamp(null)) == null) {
                    Dialog.error(LogString.itemIsUnknown(name, International.getString("Boot")));
                    boat.requestFocus();
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean checkUnknownNamesPerson() {
        // Prüfen, ob ggf. nur bekannte Boote/Ruderer/Ziele eingetragen wurden
        if (isModeBoathouse()) {

            if (Daten.efaConfig.getValueEfaDirekt_eintragNurBekannteRuderer()) {
                for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                    String name = (i == 0 ? cox : crew[i-1]).getValueFromField();
                    if (name != null && name.length() > 0 && findPerson(i, getValidAtTimestamp(null)) == null) {
                    Dialog.error(LogString.itemIsUnknown(name, International.getString("Person")));
                        if (i == 0) {
                            cox.requestFocus();
                        } else {
                            crew[i-1].requestFocus();
                        }
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    protected boolean checkUnknownNamesDestination() {
        // Prüfen, ob ggf. nur bekannte Boote/Ruderer/Ziele eingetragen wurden
        if (isModeBoathouse()) {        
            if (Daten.efaConfig.getValueEfaDirekt_eintragNurBekannteZiele()) {
                String name = destination.getValueFromField();
                if (name != null && name.length() > 0 && findDestination(getValidAtTimestamp(null)) == null) {
                    Dialog.error(LogString.itemIsUnknown(name, International.getString("Ziel/Strecke")));
                    destination.requestFocus();
                    return false;
                }
            }
        }
        return true;
    }
    
    protected boolean checkUnknownNamesWaters() {
        // Prüfen, ob ggf. nur bekannte Boote/Ruderer/Ziele eingetragen wurden
        if (isModeBoathouse()) {        

            if (Daten.efaConfig.getValueEfaDirekt_eintragNurBekannteGewaesser() && waters.isVisible()) {
                DataTypeList[] wlists = findWaters(waters);
                if (wlists != null && wlists.length != 0 && wlists[1].length() > 0) {
                    Dialog.error(LogString.itemIsUnknown(wlists[1].toString(), International.getString("Gewässer")));
                    waters.requestFocus();
                    return false;
                }
            }
        }
        return true;
    }
    
    protected boolean checkUnknownNames() {

    	return checkUnknownNamesBoat() ||
    			checkUnknownNamesPerson() ||
    			checkUnknownNamesDestination() ||
    			checkUnknownNamesWaters();
    }
    
    /*
    private boolean checkProperUnknownNames() {
        // check whether all names of unkown persons are proper and allowed names
        if (isModeBoathouse()) {
            String slist = Daten.efaConfig.getValueBoathouseNonAllowedUnknownPersonNames();
            String[] list = null;
            if (slist != null && slist.length() > 0) {
                list = slist.split(";");
                for (int i=0; list != null && i<list.length; i++) {
                    list[i] = list[i] != null ? list[i].trim().toLowerCase() : null;
                }
            }
            Pattern pname = Daten.efaConfig.getValueNameFormatIsFirstNameFirst() ?
                    Pattern.compile("[X ]+ [X ]+") : Pattern.compile("[X ]+, [X ]+");
            Pattern pnameadd = Daten.efaConfig.getValueNameFormatIsFirstNameFirst() ?
                    Pattern.compile("[X ]+ [X ]+ \\([X 0-9]+\\)") : Pattern.compile("[X ]+, [X ]+ \\([X 0-9]+\\)");
            Pattern pnameclub = Daten.efaConfig.getValueNameFormatIsFirstNameFirst() ?
                    Pattern.compile("[X ]+ [X ]+ \\[[X 0-9]+\\]") : Pattern.compile("[X ]+, [X ]+ \\[[X 0-9]+\\]");
            Pattern pnameaddclub = Daten.efaConfig.getValueNameFormatIsFirstNameFirst() ?
                    Pattern.compile("[X ]+ [X ]+ \\([X 0-9]+\\) \\[[X 0-9]+\\]") : Pattern.compile("[X ]+, [X ]+ \\([X 0-9]+\\) \\[[X 0-9]+\\]");
            for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                String name = (i == 0 ? cox : crew[i - 1]).getValueFromField();
                if (name != null && name.length() > 0 && findPerson(i, getValidAtTimestamp(null)) == null) {
                    if (Daten.efaConfig.getValueBoathouseStrictUnknownPersons()) {
                        String _name = name;
                        name = EfaUtil.replace(name, ",", ", ", true);
                        name = EfaUtil.replace(name, " ,", ",", true);
                        name = EfaUtil.replace(name, "(", " (", true);
                        name = EfaUtil.replace(name, "( ", "(", true);
                        name = EfaUtil.replace(name, ")", ") ", true);
                        name = EfaUtil.replace(name, " )", ")", true);
                        name = EfaUtil.replace(name, "  ", " ", true);
                        name = name.trim();
                        if (!name.equals(_name)) {
                            (i == 0 ? cox : crew[i - 1]).parseAndShowValue(name);
                        }
                        String xname = EfaUtil.transformNameParts(name);
                        if (!pname.matcher(xname).matches() &&
                            !pnameadd.matcher(xname).matches() &&
                            !pnameclub.matcher(xname).matches() &&
                            !pnameaddclub.matcher(xname).matches()
                            ) {
                            String nameformat = Daten.efaConfig.getValueNameFormatIsFirstNameFirst()
                                    ? International.getString("Vorname") + " "
                                    + International.getString("Nachname")
                                    : International.getString("Nachname") + ", "
                                    + International.getString("Vorname");
                            Dialog.error(International.getString("Ungültiger Name") + ": " + name + "\n"
                                    + International.getString("Personennamen müssen eines der folgenden Formate haben:") + "\n\n"
                                    + nameformat + "\n"
                                    + nameformat + " (" + International.getString("Namenszusatz") + ")\n"
                                    + nameformat + " [" + International.getString("Verein") + "]\n"
                                    + nameformat + " (" + International.getString("Namenszusatz") + ")" +
                                                   " [" + International.getString("Verein") + "]");
                            (i == 0 ? cox : crew[i - 1]).requestFocus();
                            return false;
                        }
                    }
                    if (list != null) {
                        for (int j=0; j<list.length; j++) {
                            if (list[j] != null && list[j].trim().length() > 0 &&
                                name.toLowerCase().indexOf(list[j].trim().toLowerCase()) >= 0) {
                                Dialog.error(International.getString("Ungültiger Name") + ": " + name + "\n"
                                        + International.getMessage("'{string}' ist nicht erlaubt in Personennamen.",
                                                list[j].trim().toUpperCase()));
                                (i == 0 ? cox : crew[i - 1]).requestFocus();
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }*/
    
    protected Pattern createPNamePattern() {
    	return Daten.efaConfig.getValueNameFormatIsFirstNameFirst() ?
                Pattern.compile("[X ]+ [X ]+") : Pattern.compile("[X ]+, [X ]+");
    }

    protected Pattern createPNameAddPattern() {
    	return Daten.efaConfig.getValueNameFormatIsFirstNameFirst() ?
                Pattern.compile("[X ]+ [X ]+ \\([X 0-9]+\\)") : Pattern.compile("[X ]+, [X ]+ \\([X 0-9]+\\)");
    }

    protected Pattern createPNameClubPattern() {
    	return Daten.efaConfig.getValueNameFormatIsFirstNameFirst() ?
                Pattern.compile("[X ]+ [X ]+ \\[[X 0-9]+\\]") : Pattern.compile("[X ]+, [X ]+ \\[[X 0-9]+\\]");
    }

    protected Pattern createPNameAddClubPattern() {
    	return Daten.efaConfig.getValueNameFormatIsFirstNameFirst() ?
                Pattern.compile("[X ]+ [X ]+ \\([X 0-9]+\\) \\[[X 0-9]+\\]") : Pattern.compile("[X ]+, [X ]+ \\([X 0-9]+\\) \\[[X 0-9]+\\]");
    }

    /**
     * 
     * @param value Content of a Name field
     * @return Beautified Name with proper spaces between elements
     */
    protected String beautifyNameField(String value) {
        String name = value;
    	name = EfaUtil.replace(name, ",", ", ", true);
        name = EfaUtil.replace(name, " ,", ",", true);
        name = EfaUtil.replace(name, "(", " (", true);
        name = EfaUtil.replace(name, "( ", "(", true);
        name = EfaUtil.replace(name, ")", ") ", true);
        name = EfaUtil.replace(name, " )", ")", true);
        name = EfaUtil.replace(name, "  ", " ", true).trim();
        return name;        
    }
    
    /**
     * Checks a name field for having the correct format/structure
     * 
     * @param value Content of the name field 
     * @param pname Pattern for Name
     * @param pnameadd Pattern for added Name
     * @param pnameclub Pattern for Club
     * @param pnameaddclub Pattern for added club
     * @param item Item that shall get the focus if the check fails
     * @return True if all checks were performed successfully.
     * 
     */
    protected boolean checkNameFormat(String value, Pattern pname, Pattern pnameadd, Pattern pnameclub, Pattern pnameaddclub, ItemTypeStringAutoComplete item) {
    	String xname = EfaUtil.transformNameParts(value);
        if (!pname.matcher(xname).matches() &&
            !pnameadd.matcher(xname).matches() &&
            !pnameclub.matcher(xname).matches() &&
            !pnameaddclub.matcher(xname).matches()
            ) {
            String nameformat = Daten.efaConfig.getValueNameFormatIsFirstNameFirst()
                    ? International.getString("Vorname") + " "
                    + International.getString("Nachname")
                    : International.getString("Nachname") + ", "
                    + International.getString("Vorname");
            Dialog.error(International.getString("Ungültiger Name") + ": " + value + "\n"
                    + International.getString("Personennamen müssen eines der folgenden Formate haben:") + "\n\n"
                    + nameformat + "\n"
                    + nameformat + " (" + International.getString("Namenszusatz") + ")\n"
                    + nameformat + " [" + International.getString("Verein") + "]\n"
                    + nameformat + " (" + International.getString("Namenszusatz") + ")" +
                                   " [" + International.getString("Verein") + "]");
            item.requestFocus();
            return false;
        }
        return true;
    }
    
    /**
     * Checks a single name field value for invalid content.
     * 
     * @param value  Name field content
     * @param invalidContent StringArray of items which present invalid content
     * @param item The actual field which shall get the focus if the check was not okay. 
     * @return false if check was not ok.
     */
    protected boolean checkNameForInvalidContent(String value, String[] invalidContent, ItemTypeStringAutoComplete item) {
        if (invalidContent != null) {

        	for (int j=0; j<invalidContent.length; j++) {
                if (invalidContent[j] != null && invalidContent[j].trim().length() > 0 &&
                		value.toLowerCase().indexOf(invalidContent[j].trim().toLowerCase()) >= 0) {
                    Dialog.error(International.getString("Ungültiger Name") + ": " + value + "\n"
                            + International.getMessage("'{string}' ist nicht erlaubt in Personennamen.",
                            		invalidContent[j].trim().toUpperCase()));
                    item.requestFocus();
                    return false;
                }
            }
        }
        return true;

    }
    
    /**
     * Creates a StringArray of items which contain invalid content for names (retrieved from efaConfig) 
     * @return 
     */
    protected String[] createListOfInvalidContent() {
    	String slist = Daten.efaConfig.getValueBoathouseNonAllowedUnknownPersonNames();
        String[] list = null;
        if (slist != null && slist.length() > 0) {
            list = slist.split(";");
            for (int i=0; list != null && i<list.length; i++) {
                list[i] = list[i] != null ? list[i].trim().toLowerCase() : null;
            }
        }
        return list;
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

            for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                String name = (i == 0 ? cox : crew[i - 1]).getValueFromField();
                if (name != null && name.length() > 0 && findPerson(i, getValidAtTimestamp(null)) == null) {
                    if (Daten.efaConfig.getValueBoathouseStrictUnknownPersons()) {
                        String _name = name;
                        name = beautifyNameField(name);
                        if (!name.equals(_name)) {
                            (i == 0 ? cox : crew[i - 1]).parseAndShowValue(name);
                        }
                        if (!checkNameFormat(name, pname, pnameadd,pnameclub, pnameaddclub, (i == 0 ? cox : crew[i - 1]))){
                        	return false;
                        }
                    }
                    if (!checkNameForInvalidContent(name, list, (i == 0 ? cox : crew[i - 1]))) {
                    	return false;                        	
                    }
                 }
             }
         }
         return true;
    }

    protected boolean checkAllowedPersons() {
        if (mode == MODE_BOATHOUSE_START || mode == MODE_BOATHOUSE_START_CORRECT || mode == MODE_BOATHOUSE_START_MULTISESSION) {
            if (currentBoat == null) {
                return true;
            }

            LogbookRecord myRecord = this.getFields();
            if (myRecord == null) {
                return true;
            }

            Groups groups = Daten.project.getGroups(false);
            long tstmp = getValidAtTimestamp(myRecord);

            DataTypeList<UUID> groupIdList = currentBoat.getAllowedGroupIdList();
            if (groupIdList != null && groupIdList.length() > 0) {
                String nichtErlaubt = null;
                int nichtErlaubtAnz = 0;
                //Vector g = Boote.getGruppen(b);
                for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                    PersonRecord p = myRecord.getCrewRecord(i, tstmp);
                    String ptext = myRecord.getCrewName(i);
                    if (p == null && ptext == null) {
                        continue;
                    }

                    if (p != null && p.getBoatUsageBan()) {
                        switch (Dialog.auswahlDialog(International.getString("Bootsbenutzungs-Sperre"),
                            International.getMessage("Für {name} liegt zur Zeit eine Bootsbenutzungs-Sperre vor.", p.getQualifiedName()) +
                            "\n" +
                            International.getString("Was möchtest Du tun?"),
                            International.getString("Mannschaft ändern"),
                            International.getString("Trotzdem benutzen"),
                            International.getString("Eintrag abbrechen"))) {
                        case 0:
                            if (i == 0) {
                                cox.requestFocus();
                            } else {
                                crew[i-1].requestFocus();
                            }
                            return false;
                        case 1:
                            break;
                        case 2:
                            cancel();
                            return false;
                        default: //default when the user hits VK_ESCAPE: change crew
                            if (i == 0) {
                                cox.requestFocus();
                            } else {
                                crew[i-1].requestFocus();
                            }
                            return false;                    	
                            	
                        }
                    }

                    boolean inAnyGroup = false;
                    if (p != null) {
                        for (int j = 0; j < groupIdList.length(); j++) {
                            GroupRecord g = groups.findGroupRecord(groupIdList.get(j), tstmp);
                            if (g != null && g.getMemberIdList() != null && g.getMemberIdList().contains(p.getId())) {
                                inAnyGroup = true;
                                break;
                            }
                        }
                    }
                    if (!inAnyGroup) {
                        String name = (p != null ? p.getQualifiedName() : ptext);
                        nichtErlaubt = (nichtErlaubt == null ? name : nichtErlaubt + "\n" + name);
                        nichtErlaubtAnz++;
                    }
                }
                if (Daten.efaConfig.getValueCheckAllowedPersonsInBoat() &&
                    nichtErlaubtAnz > 0 &&
                    nichtErlaubtAnz > currentBoat.getMaxNotInGroup()) {
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
                            International.getMessage("Dieses Boot dürfen nur {list_of_valid_groups} nutzen.", erlaubteGruppen) + "\n"
                            + International.getString("Folgende Personen gehören keiner der Gruppen an und dürfen das Boot nicht benutzen:") + " \n"
                            + nichtErlaubt + "\n"
                            + International.getString("Was möchtest Du tun?"),
                            International.getString("Anderes Boot wählen"),
                            International.getString("Mannschaft ändern"),
                            International.getString("Trotzdem benutzen"),
                            International.getString("Eintrag abbrechen"))) {
                        case 0:
                            setFieldEnabled(true, true, boat);
                            boat.parseAndShowValue("");
                            boat.requestFocus();
                            return false;
                        case 1:
                            crew[0].requestFocus();
                            return false;
                        case 2:
                            logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_UNALLOWEDBOATUSAGE,
                                              International.getString("Unerlaubte Benutzung eines Bootes"),
                                              myRecord);
                            break;
                        case 3:
                            cancel();
                            return false;
                        default: //default when the user hits VK_ESCAPE: change crew
                            crew[0].requestFocus();
                            return false;                            
                    }
                }
            }

            // Prüfen, ob mind 1 Ruderer (oder Stm) der Gruppe "mind 1 aus Gruppe" im Boot sitzt
            if (Daten.efaConfig.getValueCheckMinOnePersonsFromGroupInBoat() &&
                currentBoat.getRequiredGroupId() != null) {
                GroupRecord g = groups.findGroupRecord(currentBoat.getRequiredGroupId(), tstmp);
                boolean found = false;
                if (g != null && g.getMemberIdList() != null) {
                    for (int i = 0; i <= LogbookRecord.CREW_MAX; i++) {
                        PersonRecord p = myRecord.getCrewRecord(i, tstmp);
                        if (p != null && g.getMemberIdList().contains(p.getId())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (g != null && !found) {
                    switch (Dialog.auswahlDialog(International.getString("Boot erfordert bestimmte Berechtigung"),
                            International.getMessage("In diesem Boot muß mindestens ein Mitglied der Gruppe {groupname} sitzen.", g.getName()) + "\n"
                            + International.getString("Was möchtest Du tun?"),
                            International.getString("Anderes Boot wählen"),
                            International.getString("Mannschaft ändern"),
                            International.getString("Trotzdem benutzen"),
                            International.getString("Eintrag abbrechen"))) {
                        case 0:
                            this.setFieldEnabled(true, true, boat);
                            boat.parseAndShowValue("");
                            boat.requestFocus();
                            return false;
                        case 1:
                            crew[0].requestFocus();
                            return false;
                        case 2:
                            logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_UNALLOWEDBOATUSAGE,
                                              International.getString("Unerlaubte Benutzung eines Bootes"),
                                              myRecord);
                            break;
                        case 3:
                            cancel();
                            return false;
                            
                        default: //default when the user hits VK_ESCAPE: change crew
                            crew[0].requestFocus();
                            return false;                        	
                    }
                }
            }
        }
        return true;
    }

    // =========================================================================
    // Menu Actions
    // =========================================================================

    void menuActionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == null) {
            return;
        }

        // check and prompt to save changes (except for Help and About)
        if (!cmd.equals(EfaMenuButton.BUTTON_HELP) &&
            !cmd.equals(EfaMenuButton.BUTTON_ABOUT)) {
            if (!isModeFull() || !promptSaveChangesOk()) {
                return;
            }
        }

        // now check permissions and perform the menu action
        boolean permission = EfaMenuButton.menuAction(this, cmd, getAdmin(), logbook);

        // handle exit
        if (cmd.equals(EfaMenuButton.BUTTON_EXIT) && permission) {
            cancel();
        }

        // Projects and Logbooks are *not* handled within EfaMenuButton
        if (cmd.equals(EfaMenuButton.BUTTON_PROJECTS) && permission) {
            menuFileProjects(e);
        }
        if (cmd.equals(EfaMenuButton.BUTTON_LOGBOOKS) && permission) {
            menuFileLogbooks(e);
        }
        if (cmd.equals(EfaMenuButton.BUTTON_CLUBWORKBOOK) && permission) {
            menuFileClubwork(e);
        }

    }

    void menuFileProjects(ActionEvent e) {
        // for projects, we always use the permissions of the local admin!!
        OpenProjectOrLogbookDialog dlg = new OpenProjectOrLogbookDialog(this, OpenProjectOrLogbookDialog.Type.project, admin); // local admin!!
        String projectName = dlg.openDialog();
        if (projectName == null) {
            return;
        }
        if (Daten.project != null && Daten.project.isOpen()) {
            try {
                Daten.project.closeAllStorageObjects();
            } catch(Exception ee) {
                Logger.log(ee);
                Dialog.error(ee.toString());
                return;
            }
        }
        Daten.project = null;
        Project.openProject(projectName, true);
        remoteAdmin = (Daten.project != null ? Daten.project.getRemoteAdmin() : null);
        checkRemoteAdmin();
        iniGuiMenu();
        if (Daten.project != null && !isModeBoathouse()) {
            Daten.efaConfig.setValueLastProjectEfaBase(Daten.project.getProjectName());
        }
        if (Daten.project != null) {
            if (Daten.project.getCurrentLogbookEfaBase() != null) {
                openLogbook(Daten.project.getCurrentLogbookEfaBase());
            } else {
                menuFileLogbooks(null);
            }
        }
        setTitle();
    }

    void menuFileLogbooks(ActionEvent e) {
        if (Daten.project == null) {
            menuFileProjects(e);
            if (Daten.project == null) {
                return;
            }
        }
        OpenProjectOrLogbookDialog dlg = new OpenProjectOrLogbookDialog(this, OpenProjectOrLogbookDialog.Type.logbook, getAdmin());
        String logbookName = dlg.openDialog();
        if (logbookName != null) {
            openLogbook(logbookName);
        }
        setTitle();
    }
    
    void menuFileClubwork(ActionEvent e) {
        if (Daten.project == null) {
            menuFileProjects(e);
            if (Daten.project == null) {
                return;
            }
        }
        OpenProjectOrLogbookDialog dlg = new OpenProjectOrLogbookDialog(this, OpenProjectOrLogbookDialog.Type.clubwork, getAdmin());
        String clubworkName = dlg.openDialog();
        if (clubworkName != null) {
            openClubwork(clubworkName);
        }
        setTitle();
    }
    
    // =========================================================================
    // Toolbar Button Actions
    // =========================================================================

    void navigateInLogbook(int relative) {
        if (!isLogbookReady() || iterator == null) {
            return;
        }
        if (!promptSaveChangesOk()) {
            return;
        }
        LogbookRecord r = null;
        switch(relative) {
            case Integer.MIN_VALUE:
                r = logbook.getLogbookRecord(iterator.getFirst());
                break;
            case Integer.MAX_VALUE:
                r = logbook.getLogbookRecord(iterator.getLast());
                break;
            case -1:
                r = logbook.getLogbookRecord(iterator.getPrev());
                if (r == null) {
                    r = logbook.getLogbookRecord(iterator.getFirst());;
                }
                break;
            case 1:
                r = logbook.getLogbookRecord(iterator.getNext());
                if (r == null) {
                    r = logbook.getLogbookRecord(iterator.getLast());;
                }
                break;
            case 0:
                r = logbook.getLogbookRecord(iterator.getCurrent());
                if (r == null) {
                    r = logbook.getLogbookRecord(iterator.getLast());;
                }
                break;
        }
        if (r != null) {
            setFields(r);
        }
        autoCompleteListPersons.reset();
    }

    void goToEntry(String entryNo, boolean next) {
        if (!isLogbookReady() || iterator == null) {
            return;
        }
        if (!promptSaveChangesOk()) {
            return;
        }
        if (entryNo == null || entryNo.length() == 0) {
            return;
        }
        if (Character.isDigit(entryNo.charAt(0))) {
            LogbookRecord r = logbook.getLogbookRecord(DataTypeIntString.parseString(entryNo));
            if (r != null) {
                setFields(r);
            }
        } else {
            SearchLogbookDialog.initialize(this, logbook, iterator);
            SearchLogbookDialog.search(entryNo.toLowerCase(), false, SearchLogbookDialog.SearchMode.normal,
                    next, false);
        }
    }

    void createNewRecord(boolean insertAtCurrentPosition) {
        if (!isLogbookReady()) {
            return;
        }
        if (isModeFull() && !promptSaveChangesOk()) {
            return;
        }

        String currentEntryNo = null;
        
        setFields(null);

        // calculate new EntryID for new record
        if (insertAtCurrentPosition) {
            entryno.parseAndShowValue(currentEntryNo);
            entryno.setUnchanged();
        } else {
            String n;
            if (isModeFull() && entryNoForNewEntry > 0) {
                n = Integer.toString(entryNoForNewEntry + 1);
            } else {
                n = logbook.getNextEntryNo().toString();
            }
            entryno.parseAndShowValue(n);
            entryno.setUnchanged();
        }

        // set Date
        String d;
        if (referenceRecord != null && referenceRecord.getDate() != null) {
            d = referenceRecord.getDate().toString();
        } else {
            d = EfaUtil.getCurrentTimeStampDD_MM_YYYY();
        }
        date.parseAndShowValue(d);
        updateTimeInfoFields();
        date.setUnchanged();
        if (isModeFull()) {
            date.setSelection(0, Integer.MAX_VALUE);
        }
    }

    void deleteRecord() {
        if (!isModeFull() || !isLogbookReady()) {
            return;
        }
        String entryNo = null;
        if (currentRecord != null && currentRecord.getEntryId() != null && currentRecord.getEntryId().toString().length() > 0) {
            entryNo = currentRecord.getEntryId().toString();
        }
        if (entryNo == null) {
            return;
        }
        if (Dialog.yesNoDialog(International.getString("Wirklich löschen?"),
                International.getString("Möchtest Du den aktuellen Eintrag wirklich löschen?")) == Dialog.YES) {
            try {
                logbook.data().delete(currentRecord.getKey());
                if (isModeFull()) {
                    logAdminEvent(Logger.INFO, Logger.MSG_ADMIN_LOGBOOK_ENTRYDELETED,
                            International.getString("Eintrag gelöscht"), currentRecord);
                }
            } catch(Exception e) {
                Logger.logdebug(e);
                Dialog.error(e.toString());
            }

            LogbookRecord r = logbook.getLogbookRecord(iterator.getCurrent());
            if (r == null) {
                r = logbook.getLogbookRecord(iterator.getLast());
            }
            setFields(r);
        }
    }

    void searchLogbook() {
        String s = toolBar_goToEntry.getText().trim();
        SearchLogbookDialog.showSearchDialog(this, logbook, iterator, (s.length() > 0 ? s : null));
    }



    // =========================================================================
    // Callback-related methods
    // =========================================================================

    private void this_windowDeactivated(WindowEvent e) {
        try {
            if (isEnabled() && Dialog.frameCurrent() == this) {
                this.toFront();
            }
        } catch (Exception ee) {
            Logger.logdebug(ee);
        }
    }

    public void itemListenerAction(IItemType item, AWTEvent event) {
        int id = event.getID();
        if (id == ActionEvent.ACTION_PERFORMED) {
            if (item == boat) {
                editBoat((ItemTypeStringAutoComplete)item);
            }
            if (item == cox) {
                editPerson((ItemTypeStringAutoComplete)item);
            }
            for (int i=0; i<LogbookRecord.CREW_MAX; i++) {
                if (item == crew[i]) {
                    editPerson((ItemTypeStringAutoComplete)item);
                }
            }
            if (item == destination) {
                editDestination((ItemTypeStringAutoComplete)item);
            }
            if (item == sessiongroup) {
                selectSessionGroup();
            }
            if (item == remainingCrewUpButton) {
                setCrewRangeSelection(crewRangeSelection - 1);
            }
            if (item == remainingCrewDownButton) {
                setCrewRangeSelection(crewRangeSelection + 1);
            }
            if (item == boatDamageButton) {
                if (currentBoat != null && currentBoat.getId() != null) {
                    UUID personID = null;
                    LogbookRecord myRecord = currentRecord;
                    if (myRecord == null) {
                        myRecord = getFields();
                    }
                    if (myRecord != null) {
                        personID = myRecord.getCoxId();
                        if (personID == null) {
                            personID = myRecord.getCrewId(1);
                        }
                    }
                    String logbookRecordText = null;
                    if (myRecord != null) {
                        logbookRecordText = myRecord.getLogbookRecordAsStringDescription();
                    }
                    BoatDamageEditDialog.newBoatDamage(this, currentBoat, personID, logbookRecordText);
                }
            }
            if (item == boatNotCleanedButton) {
                if (currentBoat != null && currentBoat.getId() != null) {
                    String boatName = currentBoat.getQualifiedName();
                    String personName = "";
                    LogbookRecord myRecord = currentRecord;
                    if (myRecord == null) {
                        myRecord = getFields();
                    }
                    if (myRecord != null) {
                        UUID personID = myRecord.getCoxId();
                        if (personID == null) {
                            personID = myRecord.getCrewId(1);
                        }
                        if (personID != null) {
                            PersonRecord r = Daten.project.getPersons(false).getPerson(personID, System.currentTimeMillis());
                            if (r != null) {
                                personName = r.getQualifiedName();
                            }
                        }
                    }
                    String logbookRecordText = null;
                    if (myRecord != null) {
                        logbookRecordText = myRecord.getLogbookRecordAsStringDescription();
                    }
                    IItemType[] items = new IItemType[4];
                    items[0] = new ItemTypeLabel("INFO", IItemType.TYPE_PUBLIC, "",
                            International.getString("Bitte nur ausfüllen, wenn du das Boot ungeputzt vorgefunden hast!"));
                    ((ItemTypeLabel)items[0]).setPadding(0, 0, 0, 20);
                    items[1] = new ItemTypeLabel("BOAT", IItemType.TYPE_PUBLIC, "",
                            International.getString("Boot") + ": " + boatName);
                    items[2] = new ItemTypeString("DESCRIPTION", "", IItemType.TYPE_PUBLIC, "",
                            International.getString("Beschreibung"));
                    ((ItemTypeString)items[2]).setNotNull(true);
                    items[3] = new ItemTypeStringAutoComplete("NAME", personName,
                            IItemType.TYPE_PUBLIC, "",
                            International.getString("Dein Name"), false);
                    ((ItemTypeStringAutoComplete)items[3]).setNotNull(true);
                    ((ItemTypeStringAutoComplete)items[3]).setAutoCompleteData(autoCompleteListPersons);
                    if (SimpleInputDialog.showInputDialog(this, 
                            International.getString("ein ungeputztes Boot melden"), 
                            items)) {
                        String description = items[2].getValueFromField();
                        personName = items[3].getValueFromField();
                        UUID personId = (UUID)(((ItemTypeStringAutoComplete)items[3]).getId(personName));
                        LogbookRecord latest = logbook.getLastBoatUsage(currentBoat.getId(), myRecord);
                        String lastUsage = (latest != null ?
                                latest.getLogbookRecordAsStringDescription() :
                                International.getString("Keinen Eintrag gefunden!"));
                        StringBuilder message = new StringBuilder();
                        message.append(International.getMessage("{person} hat gemeldet, dass das Boot '{boat}' nicht geputzt war.", 
                                personName, boatName) + "\n\n");
                        if (description != null && description.length() > 0) {
                            message.append(International.getString("Beschreibung") + ": " +
                                    description+"\n\n");
                        }
                        message.append(International.getString("gemeldet am") + ": " + EfaUtil.getCurrentTimeStampYYYY_MM_DD_HH_MM_SS() + "\n");
                        message.append(International.getString("gemeldet von") + ": " + personName + 
                                       " (" + logbookRecordText + ")\n\n");
                        message.append(International.getString("Letzte Benutzung") + ":\n" + lastUsage);
                        
                        Daten.project.getMessages(false).createAndSaveMessageRecord(personName,
                                MessageRecord.TO_BOATMAINTENANCE, personId,
                                International.getString("Boot war nicht geputzt") + " - " + boatName, 
                                message.toString());
                        Dialog.infoDialog(International.getString("Danke") + "!");
                    }
                }
            }
            if (item == closesessionButton) {
                if (currentRecord != null) {
                    if (Dialog.yesNoDialog(International.getString("Fahrt beenden"), 
                            International.getString("Möchtest du die Fahrt jetzt beenden und den Status des Boots auf verfügbar setzen?")
                            ) == Dialog.YES) {
                        currentRecord.setSessionIsOpen(false);
                        updateBoatStatus(true, MODE_BOATHOUSE_FINISH);
                        saveEntry();
                        navigateInLogbook(0);
                    }
                }
            }
            if (item == saveButton) {
                saveEntry();
            }            
        }
        if (id == FocusEvent.FOCUS_GAINED) {
            showHint(item.getName());
            if (Daten.efaConfig.getValueTouchScreenSupport() && item instanceof ItemTypeStringAutoComplete) {
                ((ItemTypeStringAutoComplete)item).showOrRemoveAutoCompletePopupWindow();
            }
            if (lastFocusedItem != null && isCoxOrCrewItem(lastFocusedItem) &&
                    !isCoxOrCrewItem(item)) {
                autoSelectBoatCaptain();
            }
            if (item == date) {
                if (isNewRecord && (isModeFull() || mode == MODE_BOATHOUSE_LATEENTRY || mode == MODE_BOATHOUSE_LATEENTRY_MULTISESSION )) {
                    date.setSelection(0, Integer.MAX_VALUE);
                }
            }
            if (item == destination) {
                lastDestination = DestinationRecord.tryGetNameAndVariant(destination.getValueFromField().trim())[0];;
            }
            if (item == distance) {
                if (isModeBoathouse() || (isModeFull() && isNewRecord )) {
                    distance.setSelection(0, Integer.MAX_VALUE);
                }
               if (!distance.isEditable() && distance.hasFocus()) {
                   efaBaseFrameFocusManager.focusNextItem(distance, distance.getComponent());
               }
            }
        }
        if (id == FocusEvent.FOCUS_LOST) {
            showHint(null);
            lastFocusedItem = item;
            if (item == date) {
                updateTimeInfoFields();
            }
            if (item == enddate) {
                updateTimeInfoFields();
                updateSessionTypeInfo();
            }
            if (item == boat || item == boatvariant) {
                currentBoatUpdateGui();
            }
            if (item == cox) {
                if (Daten.efaConfig.getValueAutoObmann() && isNewRecord
                        && cox.getValueFromField().trim().length() > 0 && this.getBoatCaptain() == -1) {
                    this.setBoatCaptain(0, true);
                }

            }
            if (item == crew[0]) {
                if (Daten.efaConfig.getValueAutoObmann() && isNewRecord && getBoatCaptain() == -1) {
                    if (Daten.efaConfig.getValueDefaultObmann().equals(EfaConfig.OBMANN_BOW) && crew[0].getValueFromField().trim().length() > 0) {
                        setBoatCaptain(1, true);
                    }
                }
            }

            if (isModeBoathouse() && (item == starttime || item == endtime)) {
                if (item == starttime && !starttime.isSet()) {
                    setTime(starttime, Daten.efaConfig.getValueEfaDirekt_plusMinutenAbfahrt(), null);
                }
                if (item == endtime && !endtime.isSet()) {
                    setTime(endtime, -Daten.efaConfig.getValueEfaDirekt_minusMinutenAnkunft(), starttime.getTime());
                }
            }
            /*
            if (isModeBoathouse() && (item == starttime || item == endtime)) {
                setTime((ItemTypeTime)item, 0, null);
            }
            */
            if (item == destination) {
                boolean wasEditable = distance.isEditable();
                setDesinationDistance();
                if (distance.isEditable() && !wasEditable) {
                    distance.requestFocus();
                }
               if (!distance.isEditable() && distance.hasFocus()) {
                   efaBaseFrameFocusManager.focusNextItem(distance, distance.getComponent());
               }
            }
            if (item == distance) {
                updateSessionTypeInfo();
            }
        }
        if (id == KeyEvent.KEY_PRESSED && event instanceof KeyEvent) {
            KeyEvent e = (KeyEvent)event;
            if ((e.isControlDown() && e.getKeyCode() == KeyEvent.VK_F) || // Ctrl-F
                (e.getKeyCode() == KeyEvent.VK_F5)) {                     // F5
                if (item instanceof ItemTypeLabelValue) {
                    insertLastValue(e, (ItemTypeLabelValue)item);
                }
            }
            if ((e.isControlDown() && e.getKeyCode() == KeyEvent.VK_O)) { // Ctrl-O
                if (item instanceof ItemTypeLabelValue) {
                    selectBoatCaptain(item.getName());
                }
            }
            if (item == comments) {
                ItemTypeHashtable hash = Daten.efaConfig.getValueKeys();
                String[] k = hash.getKeysArray();
                if (k != null && k.length > 0) {
                    for (int i = 0; i < k.length; i++) {
                        if ( (((String) k[i]).equals("F6")  && e.getKeyCode() == KeyEvent.VK_F6  && hash.get(k[i]) != null ) ||
                             (((String) k[i]).equals("F7")  && e.getKeyCode() == KeyEvent.VK_F7  && hash.get(k[i]) != null ) ||
                             (((String) k[i]).equals("F8")  && e.getKeyCode() == KeyEvent.VK_F8  && hash.get(k[i]) != null ) ||
                             (((String) k[i]).equals("F9")  && e.getKeyCode() == KeyEvent.VK_F9  && hash.get(k[i]) != null ) ||
                             (((String) k[i]).equals("F10") && e.getKeyCode() == KeyEvent.VK_F10 && hash.get(k[i]) != null ) ||
                             (((String) k[i]).equals("F11") && (e.getKeyCode() == KeyEvent.VK_F11 || e.getKeyCode() == KeyEvent.VK_STOP) && hash.get(k[i]) != null ) ||
                             (((String) k[i]).equals("F12") && (e.getKeyCode() == KeyEvent.VK_F12 || e.getKeyCode() == KeyEvent.VK_AGAIN) && hash.get(k[i]) != null )
                                ) {
                            comments.parseAndShowValue(comments.getValueFromField() + hash.get(k[i]));
                        }
                    }
                }
            }
        }
        if (id == KeyEvent.KEY_RELEASED && event instanceof KeyEvent) {
            if (item == distance) {
                updateSessionTypeInfo();
            }
        }
        if (id == MouseEvent.MOUSE_CLICKED) {
            if (item instanceof ItemTypeLabelValue) {
                selectBoatCaptain(item.getName());
            }
            if (item == sessionTypeInfo && sessionTypeInfoText != null) {
                Dialog.infoDialog(sessionTypeInfoText);
            }
        }
        if (id == ItemEvent.ITEM_STATE_CHANGED) {
            if (item == boatcaptain) {
                setBoatCaptain(getBoatCaptain(), false);
            }
            if (item == boatvariant) {
                int variant = EfaUtil.stringFindInt(boatvariant.getValueFromField(), -1);
                currentBoatUpdateGui(variant);
            }
            if (item == sessiontype) {
                updateSessionTypeInfo();
            }
        }
        if (id == ItemTypeDate.ACTIONID_FIELD_EXPANDED && item == enddate) {
            starttimeInfoLabel.setVisible(true);
            endtimeInfoLabel.setVisible(true);
        }
        if (id == ItemTypeDate.ACTIONID_FIELD_COLLAPSED && item == enddate) {
            starttimeInfoLabel.setVisible(false);
            endtimeInfoLabel.setVisible(false);
        }
    }

    void showHint(String s) {
        if (s == null) {
            infoLabel.setText(" ");
            return;
        }
        if (s.equals(LogbookRecord.ENTRYID)) {
            infoLabel.setText(International.getString("Bitte eingeben") + ": "
                    + "<" + International.getString("Laufende Nummer") + ">");
            return;
        }
        if (s.equals(LogbookRecord.DATE) || s.equals(LogbookRecord.ENDDATE)) {
            infoLabel.setText(International.getString("Bitte eingeben") + ": "
                    + "<" + International.getString("Tag") + ">.<"
                    + International.getString("Monat") + ">.<"
                    + International.getString("Jahr") + ">");
            return;
        }
        if (s.equals(LogbookRecord.BOATNAME)) {
            infoLabel.setText(International.getString("Bitte eingeben") + ": "
                    + "<" + International.getString("Bootsname") + ">");
            return;
        }
        if (s.equals(LogbookRecord.BOATVARIANT)) {
            infoLabel.setText(International.getString("Bitte auswählen")
                    + ": " + International.getString("Bootsvariante"));
            return;
        }
        if (LogbookRecord.getCrewNoFromFieldName(s) >= 0) {
            infoLabel.setText(International.getString("Bitte eingeben") + ": "
                    + (Daten.efaConfig.getValueNameFormat().equals(EfaConfig.NAMEFORMAT_FIRSTLAST)
                    ? "<" + International.getString("Vorname") + "> <"
                    + International.getString("Nachname") + ">"
                    : "<" + International.getString("Nachname") + ">,  <"
                    + International.getString("Vorname") + ">"));
            return;
        }
        if (s.equals(LogbookRecord.BOATCAPTAIN)) {
            infoLabel.setText(International.getString("Bitte auswählen")
                    + ": " + International.getString("verantwortlichen Obmann"));
            return;
        }
        if (s.equals(LogbookRecord.STARTTIME) || s.equals(LogbookRecord.ENDTIME)) {
            infoLabel.setText(International.getString("Bitte eingeben") + ": "
                    + "<" + International.getString("Stunde") + ">:<"
                    + International.getString("Minute") + ">");
            return;
        }
        if (s.equals(LogbookRecord.DESTINATIONNAME)) {
            infoLabel.setText(International.getString("Bitte eingeben") + ": "
                    + "<" + International.getString("Fahrtziel oder Strecke") + ">");
            return;
        }
        if (s.equals(GUIITEM_ADDITIONALWATERS)) {
            infoLabel.setText(International.getString("Bitte eingeben") + ": "
                    + "<" + International.getString("Weitere Gewässer") + ">");
            return;
        }
        if (s.equals(LogbookRecord.DISTANCE)) {
            infoLabel.setText(International.getString("Bitte eingeben") + ": "
                    + "<" + International.getString("Länge der Fahrt") + ">"
                    + " (" + DataTypeDistance.getAllUnitAbbrevationsAsString(true) + ")" );
            return;
        }
        if (s.equals(LogbookRecord.COMMENTS)) {
            infoLabel.setText(International.getString("Bemerkungen eingeben oder frei lassen"));
            return;
        }
        if (s.equals(LogbookRecord.SESSIONTYPE)) {
            infoLabel.setText(International.getString("Bitte auswählen")
                    + ": " + International.getString("Art der Fahrt"));
            return;
        }
        if (s.equals("REMAININGCREWUP") || s.equals("REMAININGCREWDOWN")) {
            infoLabel.setText(International.getString("weitere Mannschaftsfelder anzeigen"));
            return;
        }
        if (s.equals("BOATDAMAGE")) {
            infoLabel.setText(International.getString("einen Schaden am Boot melden"));
            return;
        }
        if (s.equals("BOATNOTCLEANED")) {
            infoLabel.setText(International.getString("ein ungeputztes Boot melden"));
            return;
        }
        if (s.equals("SAVE")) {
            infoLabel.setText(International.getString("<Leertaste> drücken, um den Eintrag abzuschließen"));
            return;
        }
        infoLabel.setText(" ");
    }
    
    void updateSessionTypeInfo() {
        if (sessiontype != null && sessiontype.isVisible() && Daten.efaConfig.getValueUseFunctionalityRowingGermany()) {
            String sess = sessiontype.getValueFromField();
            DataTypeDistance dist = DataTypeDistance.parseDistance(distance.getValueFromField(), true);
            long days = 1;
            if (enddate != null && enddate.isVisible() && date != null && date.isVisible() &&
                date.isSet() && enddate.isSet()) {
                days = enddate.getDate().getDifferenceDays(date.getDate()) + 1;
            }
            if (sess != null && sess.length() > 0 && dist != null && 
                (dist.getValueInMeters() >= 40000 || (days == 1 && dist.getValueInMeters() >= 30000))) {
                if (EfaTypes.couldBeDRVWanderfahrt(sess)) {
                    sessionTypeInfo.setImage(getScaledImage("session_drv_wanderfahrt_ok"));
                    sessionTypeInfoText = "Diese Fahrt zählt als Wanderfahrt im Sinne des DRV.";
                } else if (EfaTypes.cannotBeDRVWanderfahrt(sess)) {
                    sessionTypeInfo.setImage(getScaledImage("session_drv_wanderfahrt_nok"));
                    sessionTypeInfoText = "Fahrten mit Fahrtart '" + Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, sess) + "' zählen nicht als Wanderfahrt im Sinne des DRV.";
                } else {
                    sessionTypeInfo.setImage(null);
                    sessionTypeInfoText = null;
                }
            } else {
                sessionTypeInfo.setImage(null);
                sessionTypeInfoText = null;
            }
        }
    }

    void insertLastValue(KeyEvent e, ItemTypeLabelValue item) {
        if (e == null || isModeBoathouse() || referenceRecord == null) {
            return;
        }
        if ((e.isControlDown() && e.getKeyCode() == KeyEvent.VK_F) || // Ctrl-F
                (e.getKeyCode() == KeyEvent.VK_F5)) {                 // F5
            setField(item, referenceRecord);
            //((JTextField) e.getSource()).replaceSelection(refDatensatz.get(field)); // old from efa1
        }
    }

    protected void setFieldEnabled(boolean enabled, boolean visible, IItemType item) {
        if (Daten.efaConfig.getValueEfaDirekt_eintragHideUnnecessaryInputFields()) {
            if (item instanceof ItemTypeStringAutoComplete) {
                ((ItemTypeStringAutoComplete)item).setVisibleSticky(visible);
            } else {
                item.setVisible(visible);
            }
        }
        item.setEditable(enabled);
        item.saveBackgroundColor(true);
    }

    private void setFieldEnabledDistance() {
        if (mode != MODE_BOATHOUSE_FINISH && mode != MODE_BOATHOUSE_LATEENTRY && mode != MODE_BOATHOUSE_LATEENTRY_MULTISESSION) {
            return; // Zielabhängiges Enabled der BootsKm nur bei "Fahrt beenden" und "Nachtrag"
        }
        boolean enabled = !destination.isKnown()
                || !Daten.efaConfig.getValueEfaDirekt_eintragNichtAenderbarKmBeiBekanntenZielen();
        setFieldEnabled(enabled, true, distance);
    }


    private void currentBoatUpdateGuiBoathouse(boolean isCoxed, int numCrew) {
        // Steuermann wird bei steuermannslosen Booten immer disabled (unabhängig von Konfigurationseinstellung)
        setFieldEnabled(isCoxed, isCoxed, cox);
        if (!isCoxed) {
            cox.parseAndShowValue("");
            if (getBoatCaptain() == 0) {
                setBoatCaptain(-1, true);
            }
        }
        if (Daten.efaConfig.getValueEfaDirekt_eintragErlaubeNurMaxRudererzahl()) {
            for (int i = 1; i <= LogbookRecord.CREW_MAX; i++) {
                setFieldEnabled(i <= numCrew, i <= numCrew, crew[i-1]);
                if (i > numCrew) {
                    crew[i-1].parseAndShowValue("");
                    if (getBoatCaptain() == i) {
                        setBoatCaptain(-1, true);
                    }
                }
            }
        }
        setCrewRangeSelection(0);

        // "Weiterere Mannschaft"-Button ggf. ausblenden
        setFieldEnabled(true, numCrew > 8 || !Daten.efaConfig.getValueEfaDirekt_eintragErlaubeNurMaxRudererzahl(), remainingCrewUpButton);
        setFieldEnabled(true, numCrew > 8 || !Daten.efaConfig.getValueEfaDirekt_eintragErlaubeNurMaxRudererzahl(), remainingCrewDownButton);

        // "Obmann" ggf. ausblenden
        setFieldEnabled(true, isCoxed || numCrew > 1, boatcaptain);

        // Bezeichnung für Mannschaftsfelder anpassen
        if (numCrew != 1 || isCoxed) {
            crew[0].setDescription(crew1defaultText);
        } else {
            crew[0].setDescription(International.getString("Name"));
        }
        
        // if the boat changed, or the boatvariant changed, we need to take care that
        // the persons list is accurate. So if we have a 3seat boat with cox, and change to a 2seat without cox,
        // the person who was cox and person3 should be made available in the person list.
        // so we reset the person list and remove all persons from it which are still set.

        //refill autocompletelist for persons with all persons
		autoCompleteListPersons.reset();
		
		// remove currently used persons	
		for (int i=0; i<LogbookRecord.CREW_MAX; i++) {
			//crew(0)=cox crew(1)..crew(24)=crew					
			removeFromAutoCompleteVisible(getCrewItem(i));
		}
    }

    /**
     * Removes a value from the visible items of an autocomplete list of a field
     * @param theField the AutoComplete Field
     */
    private void removeFromAutoCompleteVisible(ItemTypeStringAutoComplete theField) {
    	theField.getValueFromGui();
    	String value=theField.getValue();
    	if (value!=null && value.trim().isEmpty()==false) {
    		theField.removeFromVisible(value);
    	}
    }
    
    // wird von boot_focusLost aufgerufen, sowie vom FocusManager! (irgendwie unsauber, da bei <Tab> doppelt...
    void currentBoatUpdateGui() {
        currentBoatUpdateGui(-1);
    }
    void currentBoatUpdateGui(int newvariant) {
        boat.getValueFromGui();

        if (Logger.isTraceOn(Logger.TT_GUI, 7)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_EFABASEFRAME,
                    "currentBoatUpdateGui("+newvariant+") for boat: " + boat.getValue());
        }

        currentBoat = null;
        currentBoatTypeSeats = null;
        currentBoatTypeCoxing = null;
        currentBoatNumberOfSeats = 0;
        if (!isLogbookReady()) {
            return;
        }

        try {
            BoatRecord b = findBoat(boat, getValidAtTimestamp(null));
            if (b != null) {
                currentBoat = b;
                if (Logger.isTraceOn(Logger.TT_GUI, 7)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_EFABASEFRAME,
                            "currentBoatUpdateGui(" + newvariant + "): b = " +
                            b.getId() + "(" + b.getValidFrom()+"-"+b.getInvalidFrom()+"): " + b.getQualifiedName());
                }
                // Update Boat Type selection
                updateBoatVariant(currentBoat, newvariant);
                int variant = EfaUtil.stringFindInt(boatvariant.toString(), -1);
                int idx = b.getVariantIndex(variant);
                currentBoatTypeSeats = b.getTypeSeats(idx);
                currentBoatTypeCoxing = b.getTypeCoxing(idx);
                currentBoatNumberOfSeats = b.getNumberOfSeats(idx);
                if (Logger.isTraceOn(Logger.TT_GUI, 7)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_EFABASEFRAME,
                            "currentBoatUpdateGui(" + newvariant + "): "
                            + "variant="+variant+", idx="+idx+", seats="+currentBoatTypeSeats
                            +", coxing="+currentBoatTypeCoxing+", noofseats="+currentBoatNumberOfSeats);
                }
                if (isNewRecord) {
                    if (b.getDefaultDestinationId() != null) {
                        Destinations d = logbook.getProject().getDestinations(false);
                        DestinationRecord dr = d.getDestination(b.getDefaultDestinationId(), getValidAtTimestamp(null));
                        destination.parseAndShowValue((dr != null ? dr.getQualifiedName() : ""));
                        setDesinationDistance();
                    }
                    if (b.getDefaultCrewId() != null && Daten.efaConfig.getValueAutoStandardmannsch()) {
                        setDefaultCrew(b.getDefaultCrewId());
                    }
                    if (b.getDefaultSessionType() != null && b.getDefaultSessionType().length() > 0) {
                        sessiontype.parseAndShowValue(b.getDefaultSessionType());
                    }
                }
            } else {
                if (Logger.isTraceOn(Logger.TT_GUI, 7)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_GUI_EFABASEFRAME,
                            "currentBoatUpdateGui(" + newvariant + "): No boat found for boat " + boat.getValue() +
                            " at " + getValidAtTimestamp(null) + ".");
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }


        if (isModeBoathouse()) {
            boolean isCoxed = (currentBoatTypeCoxing == null || currentBoatTypeCoxing.equals(EfaTypes.TYPE_COXING_COXED));
            int numCrew = (currentBoatNumberOfSeats <= 0 ? LogbookRecord.CREW_MAX : currentBoatNumberOfSeats);
            currentBoatUpdateGuiBoathouse(isCoxed, numCrew);
            packFrame("currentBoatUpdateGui()");
        }
    }

    void selectBoatCaptain(String field) {
        int pos = LogbookRecord.getCrewNoFromFieldName(field);
        if (pos >= 0) {
            selectBoatCaptain(pos);
        }
    }

    void selectBoatCaptain(int pos) {
        if (getBoatCaptain() == pos) {
            setBoatCaptain(-1, true);
        } else {
            setBoatCaptain(pos, true);
        }
    }

    void setBoatCaptain(int pos, boolean updateListSelection) {
        ItemTypeStringAutoComplete field;
        for (int i=0; i<= LogbookRecord.CREW_MAX; i++) {
            field = getCrewItem(i);
            if (i == pos) {
                field.setFieldFont(field.getLabelFont().deriveFont(Font.BOLD));
            } else {
                field.restoreFieldFont();
            }
        }
        if (updateListSelection) {
            if (pos >= 0 && pos <= LogbookRecord.CREW_MAX) {
                boatcaptain.parseAndShowValue(Integer.toString(pos));
            } else {
                boatcaptain.parseAndShowValue("");
            }
        }
    }

    int getBoatCaptain() {
        String val = boatcaptain.getValueFromField();
        if (val.length() == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(val);
        } catch(Exception e) {
            Logger.logdebug(e);
            return -1;
        }
    }

    void autoSelectBoatCaptain() {
        autoSelectBoatCaptain(false);
    }

    void autoSelectBoatCaptain(boolean force) {
        if ( (force || (Daten.efaConfig.getValueAutoObmann() && isNewRecord))
                && getBoatCaptain() == -1) {
            if (Daten.efaConfig.getValueDefaultObmann().equals(EfaConfig.OBMANN_STROKE)) {
                try {
                    int anzRud = getNumberOfPersonsInBoat();
                    if (anzRud > 0) {
                        setBoatCaptain(anzRud, true);
                    }
                } catch (Exception ee) {
                    Logger.logdebug(ee);
                }
            }
        }

        // Wenn Angabe eines Obmanns Pflicht ist, soll auch im Einer immer der Obmann automatisch selektiert werden,
        // unabhängig davon, ob Daten.efaConfig.autoObmann aktiviert ist oder nicht
        if ( (force || (Daten.efaConfig.getValueEfaDirekt_eintragErzwingeObmann()
                && isNewRecord ) )
                && getBoatCaptain() == -1
                && cox.getValueFromField().trim().length() == 0
                && getNumberOfPersonsInBoat() == 1) {
            try {
                setBoatCaptain(1, true);
            } catch (Exception ee) {
                Logger.logdebug(ee);
            }
        }
    }

    void setCrewRangeSelection(int nr) {
        if (nr < 0) {
            nr = (LogbookRecord.CREW_MAX / 8) - 1;
        }
        if (nr >= LogbookRecord.CREW_MAX / 8) {
            nr = 0;
        }
        for (int i = 0; i < LogbookRecord.CREW_MAX; i++) {
            crew[i].setVisible(i / 8 == nr);
        }
        crewRangeSelection = nr;
        setCrewRangeSelectionColoring();
        packFrame("setCrewRangeSelection(nr)");
    }

    void setCrewRangeSelectionColoring() {
        boolean hiddenCrewFieldsSet = false;
        for (int i = 0; !hiddenCrewFieldsSet && i < LogbookRecord.CREW_MAX; i++) {
            if (i / 8 != crewRangeSelection) {
                if (crew[i].getValueFromField().trim().length() > 0) {
                    hiddenCrewFieldsSet = true;
                }
            }
        }
        if (hiddenCrewFieldsSet) {
            remainingCrewUpButton.setBackgroundColor(Color.orange);
            remainingCrewDownButton.setBackgroundColor(Color.orange);
        } else {
            remainingCrewUpButton.restoreBackgroundColor();
            remainingCrewDownButton.restoreBackgroundColor();
        }
    }

    void setDefaultCrew(UUID crewId) {
        Crews crews = Daten.project.getCrews(false);
        CrewRecord r = crews.getCrew(crewId);
        if (r != null) {
            if (currentBoatTypeCoxing != null && !currentBoatTypeCoxing.equals(EfaTypes.TYPE_COXING_COXLESS) &&
                r.getCoxId() != null) {
                PersonRecord p = Daten.project.getPersons(false).getPerson(r.getCoxId(), getValidAtTimestamp(null));
                if (p != null) {
                    cox.parseAndShowValue(p.getQualifiedName());
                }
            }
            for (int i=1; i <=currentBoatNumberOfSeats && i<=LogbookRecord.CREW_MAX; i++) {
                UUID id = r.getCrewId(i);
                if (id != null) {
                    PersonRecord p = Daten.project.getPersons(false).getPerson(id, getValidAtTimestamp(null));
                    if (p != null) {
                        crew[i-1].parseAndShowValue(p.getQualifiedName());
                    }
                }
            }
            if ((r.getBoatCaptainPosition() == 0 && currentBoatTypeCoxing != null && !currentBoatTypeCoxing.equals(EfaTypes.TYPE_COXING_COXLESS)) ||
                (r.getBoatCaptainPosition() > 0 && r.getBoatCaptainPosition() <= currentBoatNumberOfSeats)) {
                boatcaptain.parseAndShowValue(Integer.toString(r.getBoatCaptainPosition()));
            }
        }
    }

    protected void getFieldsForBoats(LogbookRecord theRecord) {
        // Boat & Boat Variant
        BoatRecord b = findBoat(boat, getValidAtTimestamp(theRecord));
        if (b != null) {
        	theRecord.setBoatId(b.getId());
        	theRecord.setBoatVariant(EfaUtil.stringFindInt(boatvariant.getValue(), b.getTypeVariant(0)));
        	theRecord.setBoatName(null);
        } else {
            String s = boat.toString().trim();
            theRecord.setBoatName( (s.length() == 0 ? null : s) );
            theRecord.setBoatId(null);
            theRecord.setBoatVariant(IDataAccess.UNDEFINED_INT);
        }    	
    }

    protected void getFieldsForCrew(LogbookRecord theRecord) {
        // Cox and Crew
        for (int i=0; i<=LogbookRecord.CREW_MAX; i++) {
            PersonRecord p = findPerson(i, getValidAtTimestamp(theRecord));
            if (p != null) {
                if (i == 0) {
                	theRecord.setCoxId(p.getId());
                	theRecord.setCoxName(null);
                } else {
                	theRecord.setCrewId(i, p.getId());
                	theRecord.setCrewName(i, null);
                }
            } else {
                String s = getCrewItem(i).toString().trim();
                if (i == 0) {
                	theRecord.setCoxName( (s.length() == 0 ? null : s) );
                	theRecord.setCoxId(null);
                } else {
                	theRecord.setCrewName(i, (s.length() == 0 ? null : s) );
                	theRecord.setCrewId(i, null);
                }
            }
        }

        // Boat Captain
        if (boatcaptain.getValue().length() > 0) {
        	theRecord.setBoatCaptainPosition(EfaUtil.stringFindInt(boatcaptain.getValue(), 0));
        } else {
        	theRecord.setBoatCaptainPosition(IDataAccess.UNDEFINED_INT);
        }
    }    
    
    protected void getSessionGroupID(LogbookRecord theRecord) {
        // Session Group
        theRecord.setSessionGroupId((UUID)sessiongroup.getRememberedId());    	
    }
    
    // =========================================================================
    // Window-related methods
    // =========================================================================
    
    private void this_windowClosing(WindowEvent e) {
        cancel();
    }

    private void this_windowIconified(WindowEvent e) {
        if (isModeBoathouse()) {
            // startBringToFront(true); not needed any more
        }
    }
    
    public boolean cancel() {
    	return cancel(false);
    }

    public boolean cancel(Boolean keyESCAction) {
    	
        if (isModeBoathouse()) {
            efaBoathouseHideEfaFrame();
            return true;
        }

        if (!promptSaveChangesOk()) {
            return false;
        }

        if (isModeAdmin()) {
            super.cancel();
            return true;
        }

        if (isModeBase()) {
        	if (keyESCAction) return true; // we don't want to close the baseframe if in efaBase
        }
        
        //@efaconfig if (!Daten.efaConfig.writeFile()) {
        //@efaconfig     LogString.logError_fileWritingFailed(Daten.efaConfig.getFileName(), International.getString("Konfigurationsdatei"));
        //@efaconfig }
        super.cancel(false);
        Daten.haltProgram(0);
        return true;
    }

    // =========================================================================
    // FocusManager
    // =========================================================================

    /** 2024-06-03
     * The FocusManager Concept is pre-JDK 1.4. 
     * There can be only one single focus manager for ALL dialogs, it is not possible
     * (AFAIK) to use a single focus manager for each dialog. 
     * 
     * This focus manager recognizes the efaBaseFrame class and enables some rather
     * sophisticated focus handling to jump over some non-necessary fields.
     * For instance, if crew1 field is filled, crew2 field is empty and user presses TAB,
     * this focus manager jumps right to the field behind crew24.
     * 
     * This may be difficult to implement in the newer swing focus managing code.
     * So, we keep it. 
     */
    class EfaBaseFrameFocusManager extends DefaultFocusManager {

        private EfaBaseFrame efaBaseFrame;
        private FocusManager fm;
        private int focusItemCnt;

        public EfaBaseFrameFocusManager(EfaBaseFrame efaBaseFrame, FocusManager fm) {
            this.efaBaseFrame = efaBaseFrame;
            this.fm = fm;
        }

        private IItemType getItem(Component c) {
            if (c == null) {
                return null;
            }
            if (c == efaBaseFrame.entryno.getComponent()) {
                return efaBaseFrame.entryno;
            }
            if (c == efaBaseFrame.date.getComponent()) {
                return efaBaseFrame.date;
            }
            if (c == efaBaseFrame.enddate.getComponent()) {
                return efaBaseFrame.enddate;
            }
            if (c == efaBaseFrame.boat.getComponent() ||
                c == efaBaseFrame.boat.getButton()) {
                return efaBaseFrame.boat;
            }
            if (c == efaBaseFrame.boatvariant.getComponent()) {
                return efaBaseFrame.boatvariant;
            }
            if (c == efaBaseFrame.cox.getComponent() ||
                c == efaBaseFrame.cox.getButton()) {
                return efaBaseFrame.cox;
            }
            for (int i=0; i<efaBaseFrame.crew.length; i++) {
                if (c == efaBaseFrame.crew[i].getComponent() ||
                    c == efaBaseFrame.crew[i].getButton()) {
                    return efaBaseFrame.crew[i];
                }
            }
            if (c == efaBaseFrame.boatcaptain.getComponent()) {
                return efaBaseFrame.boatcaptain;
            }
            if (c == efaBaseFrame.starttime.getComponent()) {
                return efaBaseFrame.starttime;
            }
            if (c == efaBaseFrame.endtime.getComponent()) {
                return efaBaseFrame.endtime;
            }
            if (c == efaBaseFrame.destination.getComponent() ||
                c == efaBaseFrame.destination.getButton()) {
                return efaBaseFrame.destination;
            }
            if (c == efaBaseFrame.waters.getComponent() ||
                c == efaBaseFrame.waters.getButton()) {
                return efaBaseFrame.waters;
            }
            if (c == efaBaseFrame.distance.getComponent()) {
                return efaBaseFrame.distance;
            }
            if (c == efaBaseFrame.comments.getComponent()) {
                return efaBaseFrame.comments;
            }
            if (c == efaBaseFrame.sessiontype.getComponent()) {
                return efaBaseFrame.sessiontype;
            }
            if (c == efaBaseFrame.remainingCrewUpButton.getComponent()) {
                return efaBaseFrame.remainingCrewUpButton;
            }
            if (c == efaBaseFrame.remainingCrewDownButton.getComponent()) {
                return efaBaseFrame.remainingCrewDownButton;
            }
            if (c == efaBaseFrame.boatDamageButton.getComponent()) {
                return efaBaseFrame.boatDamageButton;
            }
            if (c == efaBaseFrame.boatNotCleanedButton.getComponent()) {
                return efaBaseFrame.boatNotCleanedButton;
            }
            if (c == efaBaseFrame.saveButton.getComponent()) {
                return efaBaseFrame.saveButton;
            }
            return null;
        }

        private void focusItem(IItemType item, Component cur, int direction) {
            if (focusItemCnt++ == 100) {
                return; // oops, recursion
            }
            // fSystem.out.println("focusItem(" + item.getName() + ")");
            if (item == efaBaseFrame.starttime && Daten.efaConfig.getValueSkipUhrzeit()) {
                focusItem(efaBaseFrame.destination, cur, direction);
            } else if (item == efaBaseFrame.endtime && Daten.efaConfig.getValueSkipUhrzeit()) {
                focusItem(efaBaseFrame.destination, cur, direction);
            } else if (item == efaBaseFrame.destination && Daten.efaConfig.getValueSkipZiel()) {
                focusItem(efaBaseFrame.distance, cur, direction);
            } else if (item == efaBaseFrame.comments && Daten.efaConfig.getValueSkipBemerk()) {
                focusItem(efaBaseFrame.saveButton, cur, direction);
            } else if (item.isEnabled() && item.isVisible() && item.isEditable()) {
                item.requestFocus();
            } else {
                if (direction > 0) {
                    focusNextItem(item, cur);
                } else {
                    focusPreviousItem(item, cur);
                }
            }
        }

        public void focusNextItem(IItemType item, Component cur) {
            //System.out.println("focusNextItem(" + item.getName() + ")");
            focusItemCnt = 0;

            // LFDNR
            if (item == efaBaseFrame.entryno) {
                focusItem(efaBaseFrame.date, cur, 1);
                return;
            }

            // DATUM
            if (item == efaBaseFrame.date) {
                focusItem(efaBaseFrame.boat, cur, 1);
                return;
            }

            // BOOT
            if (item == efaBaseFrame.boat) {
                efaBaseFrame.boat.getValueFromGui();
                efaBaseFrame.currentBoatUpdateGui();
                if (!(cur instanceof JButton) && efaBaseFrame.boat.getValue().length()>0 && !efaBaseFrame.boat.isKnown() && !efaBaseFrame.isModeBoathouse()) {
                    efaBaseFrame.boat.requestButtonFocus();
                } else if (efaBaseFrame.boatvariant.isVisible()) {
                    focusItem(efaBaseFrame.boatvariant, cur, 1);
                } else {
                    if (efaBaseFrame.currentBoatTypeCoxing != null && efaBaseFrame.currentBoatTypeCoxing.equals(EfaTypes.TYPE_COXING_COXLESS)) {
                        focusItem(efaBaseFrame.crew[0], cur, 1);
                    } else {
                        focusItem(efaBaseFrame.cox, cur, 1);
                    }
                }
                return;
            }

            // BOOTVARIANT
            if (item == efaBaseFrame.boatvariant) {
                efaBaseFrame.boatvariant.getValueFromGui();
                efaBaseFrame.currentBoatUpdateGui();
                if (efaBaseFrame.currentBoatTypeCoxing != null && efaBaseFrame.currentBoatTypeCoxing.equals(EfaTypes.TYPE_COXING_COXLESS)) {
                    focusItem(efaBaseFrame.crew[0], cur, 1);
                } else {
                    focusItem(efaBaseFrame.cox, cur, 1);
                }
                return;
            }

            // STEUERMANN
            if (item == efaBaseFrame.cox) {
                efaBaseFrame.cox.getValueFromGui();
                if (!(cur instanceof JButton) && efaBaseFrame.cox.getValue().length()>0 && !efaBaseFrame.cox.isKnown() && !efaBaseFrame.isModeBoathouse()) {
                    efaBaseFrame.cox.requestButtonFocus();
                } else {
                    focusItem(efaBaseFrame.crew[efaBaseFrame.crewRangeSelection * 8], cur, 1);
                }
                return;
            }

            // MANNSCHAFT
            for (int i = 0; i < efaBaseFrame.crew.length; i++) {
                if (item == efaBaseFrame.crew[i]) {
                    efaBaseFrame.crew[i].getValueFromGui();
                    if (!(cur instanceof JButton) && efaBaseFrame.crew[i].getValue().length()>0 && !efaBaseFrame.crew[i].isKnown() && !efaBaseFrame.isModeBoathouse()) {
                        efaBaseFrame.crew[i].requestButtonFocus();
                    } else if (efaBaseFrame.crew[i].getValueFromField().trim().length() == 0) {
                        focusItem(efaBaseFrame.starttime, cur, 1);
                    } else if (efaBaseFrame.currentBoatTypeSeats != null && i+1 < efaBaseFrame.crew.length &&
                            i+1 == EfaTypes.getNumberOfRowers(efaBaseFrame.currentBoatTypeSeats) &&
                            efaBaseFrame.crew[i+1].getValueFromField().trim().length() == 0) {
                        focusItem(efaBaseFrame.starttime, cur, 1);
                    } else if (i+1 < efaBaseFrame.crew.length) {
                        focusItem(efaBaseFrame.crew[i + 1], cur, 1);
                    } else {
                        focusItem(efaBaseFrame.starttime, cur, 1);
                    }
                    return;
                }
            }

            // ABFAHRT
            if (item == efaBaseFrame.starttime) {
                focusItem(efaBaseFrame.endtime, cur, 1);
                return;
            }

            // ANKUNFT
            if (item == efaBaseFrame.endtime) {
                focusItem(efaBaseFrame.destination, cur, 1);
                return;
            }

            // ZIEL
            if (item == efaBaseFrame.destination) {
                if (!(cur instanceof JButton) && efaBaseFrame.destination.getValue().length()>0 && !efaBaseFrame.destination.isKnown() && !efaBaseFrame.isModeBoathouse()) {
                    efaBaseFrame.destination.requestButtonFocus();
                } else {
                    focusItem(efaBaseFrame.waters, cur, 1);
                }
                return;
            }

            // WATERS
            if (item == efaBaseFrame.waters) {
                if (!(cur instanceof JButton) && efaBaseFrame.waters.getValue().length()>0 && !efaBaseFrame.waters.isKnown() && !efaBaseFrame.isModeBoathouse()) {
                    efaBaseFrame.waters.requestButtonFocus();
                } else {
                    focusItem(efaBaseFrame.distance, cur, 1);
                }
                return;
            }

            // BOOTS-KM
            if (item == efaBaseFrame.distance) {
                focusItem(efaBaseFrame.comments, cur, 1);
                return;
            }

            // COMMENTS
            if (item == efaBaseFrame.comments) {
                focusItem(efaBaseFrame.saveButton, cur, 1);
                return;
            }

            // ADD-BUTTON
            if (item == efaBaseFrame.saveButton) {
                focusItem(efaBaseFrame.entryno, cur, 1);
                return;
            }

            // other
            fm.focusNextComponent(cur);
        }

        public void focusPreviousItem(IItemType item, Component cur) {
            focusItemCnt = 0;
            if (item == efaBaseFrame.entryno) {
                focusItem(efaBaseFrame.saveButton, cur, -1);
                return;
            }
            if (item == efaBaseFrame.cox) {
                focusItem(efaBaseFrame.boat, cur, -1);
                return;
            }
            for (int i = 0; i < efaBaseFrame.crew.length; i++) {
                if (item == efaBaseFrame.crew[i]) {
                    focusItem((i == 0 ? efaBaseFrame.cox : efaBaseFrame.crew[i - 1]), cur, -1);
                    return;
                }
            }
            if (item == efaBaseFrame.starttime) {
                for (int i = 0; i < 8; i++) {
                    if (efaBaseFrame.crew[i + efaBaseFrame.crewRangeSelection * 8].getValueFromField().trim().length() == 0 || i == 7) {
                        focusItem(efaBaseFrame.crew[i + efaBaseFrame.crewRangeSelection * 8], cur, -1);
                        return;
                    }
                }
            }
            if (item == efaBaseFrame.waters) {
                focusItem(efaBaseFrame.destination, cur, -1);
                return;
            }
            if (item == efaBaseFrame.distance) {
                focusItem(efaBaseFrame.waters, cur, -1);
                return;
            }
            if (item == efaBaseFrame.comments) {
                focusItem(efaBaseFrame.distance, cur, -1);
                return;
            }
            if (item == efaBaseFrame.saveButton) {
                focusItem(efaBaseFrame.comments, cur, -1);
                return;
            }

            // other
            fm.focusPreviousComponent(cur);
        }

        public void focusNextComponent(Component cur) {
            //System.out.println("focusNextComponent("+cur+")");
            IItemType item = getItem(cur);
            if (item != null) {
                focusNextItem(item, cur);
            } else {
                fm.focusNextComponent(cur);
            }
        }

        public void focusPreviousComponent(Component cur) {
            //System.out.println("focusPreviousComponent("+cur+")");
            IItemType item = getItem(cur);
            if (item != null) {
                focusPreviousItem(item, cur);
            } else {
                fm.focusPreviousComponent(cur);
            }
        }
    }

    // =========================================================================
    // efaBoathouse methods
    // =========================================================================

    public void setDataForAdminAction(Logbook logbook, AdminRecord admin, AdminDialog adminDialog) {
        this.mode = MODE_ADMIN;
        this.logbook = logbook;
        this.admin = admin;
        this.adminDialog = adminDialog;
    }

    boolean setDataForBoathouseAction(ItemTypeBoatstatusList.BoatListItem action, Logbook logbook) {
        this.mode = action.mode;
        openLogbook(logbook);
        if (getMode() == MODE_BOATHOUSE_START_CORRECT) {
            correctSessionLastBoatStatus = action.boatStatus;
        } else {
            correctSessionLastBoatStatus = null;
        }
        this.efaBoathouseAction = action;
        clearAllBackgroundColors();
        // we do not need to handle MODE_BOATHOUSE_START_MULTISESSION and MODE_BOATHOUSE_LATEENTRY_MULTISESSION here
        switch(mode) {
            case MODE_BOATHOUSE_START:
                return efaBoathouseStartSession(action);
            case MODE_BOATHOUSE_START_CORRECT:
                return efaBoathouseCorrectSession(action);
            case MODE_BOATHOUSE_FINISH:
                return efaBoathouseFinishSession(action);
            case MODE_BOATHOUSE_LATEENTRY:
                return efaBoathouseLateEntry(action);
            case MODE_BOATHOUSE_ABORT:
                return efaBoathouseAbortSession(action);
        }
        return false;
    }

    private void efaBoathouseSetPersonAndBoat(ItemTypeBoatstatusList.BoatListItem item) {
        if (item.boat != null) {
            boat.parseAndShowValue(item.boat.getQualifiedName());
            if (item.boatVariant >= 0) {
                updateBoatVariant(item.boat, item.boatVariant);
            }
            currentBoatUpdateGui( ( item.boatVariant >= 0 ? item.boatVariant : -1) );
            if (cox.isEditable()) {
                setRequestFocus(cox);
            } else {
                setRequestFocus(crew[0]);
            }
        } else {
            currentBoatUpdateGui();
            setRequestFocus(boat);
        }
        if (item.person != null) {
            crew[0].parseAndShowValue(item.person.getQualifiedName());
            if (item.person.getDefaultBoatId() != null) {
                BoatRecord r = Daten.project.getBoats(false).getBoat(
                        item.person.getDefaultBoatId(), System.currentTimeMillis());
                if (r != null) {
                    boat.parseAndShowValue(r.getQualifiedName());
                    currentBoatUpdateGui();
                    setRequestFocus(starttime);
                }
            }
        }
    }

    boolean efaBoathouseStartSession(ItemTypeBoatstatusList.BoatListItem item) {
        this.setTitle(International.getString("Neue Fahrt beginnen"));
        saveButton.setDescription(International.getStringWithMnemonic("Fahrt beginnen"));
        createNewRecord(false);
        date.parseAndShowValue(EfaUtil.getCurrentTimeStampDD_MM_YYYY());
        setTime(starttime, Daten.efaConfig.getValueEfaDirekt_plusMinutenAbfahrt(), null);

        setFieldEnabled(false, true, entryno);
        setFieldEnabled(true, true, date);
        setFieldEnabled(item.boat == null, true, boat);
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
        setFieldEnabled(true, Daten.efaConfig.getValueEfaDirekt_showBootsschadenButton(), boatDamageButton);
        setFieldEnabled(true, Daten.efaConfig.getValueEfaDirekt_showBoatNotCleanedButton(), boatNotCleanedButton);

        /* EFA_0015 - Late Entry shall use data from latest session, if boat and person both are null
         * (so the user started "start session" without choosing a boat or a person in advance */
        if (item != null && (item.boat != null || item.person!=null)) {
        	efaBoathouseSetPersonAndBoat(item);
        } else if (Daten.efaConfig.getValueEfaDirekt_eintragPresentLastTripOnNewEntry()) {
	        	efaBoathouseSetDataFromLatestSession();
	            setRequestFocus(boat);
        } else {
        	setRequestFocus(date);
        }


        
        distance.parseAndShowValue("");
        updateTimeInfoFields();
        return true;
    }

    boolean efaBoathouseCorrectSession(ItemTypeBoatstatusList.BoatListItem item) {
        this.setTitle(International.getString("Fahrt korrigieren"));
        saveButton.setDescription(International.getStringWithMnemonic("Fahrt korrigieren"));
        currentRecord = null;
        try {
            currentRecord = logbook.getLogbookRecord(item.boatStatus.getEntryNo());
        } catch(Exception e) {
            Logger.log(e);
        }
        if (currentRecord == null) {
            String msg =               International.getString("Fahrt korrigieren") + ": " +
              International.getMessage("Die gewählte Fahrt #{lfdnr} ({boot}) konnte nicht gefunden werden!",
              (item != null && item.boatStatus != null && item.boatStatus.getEntryNo() != null ? item.boatStatus.getEntryNo().toString(): "null"),
              (item != null && item.boat != null ? item.boat.getQualifiedName() : (item != null ? item.text : "null")));
            logBoathouseEvent(Logger.ERROR, Logger.MSG_ERR_NOLOGENTRYFORBOAT, msg, null);
            return false;
        }
        setFields(currentRecord);

        setFieldEnabled(false, true, entryno);
        setFieldEnabled(true, true, date);
        setFieldEnabled(true, true, boat);
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
        setFieldEnabled(true, Daten.efaConfig.getValueEfaDirekt_showBootsschadenButton(), boatDamageButton);
        setFieldEnabled(true, Daten.efaConfig.getValueEfaDirekt_showBoatNotCleanedButton(), boatNotCleanedButton);

        currentBoatUpdateGui( (currentRecord.getBoatVariant() >= 0 ? currentRecord.getBoatVariant() : -1) );
        updateTimeInfoFields();
        setRequestFocus(boat);

        return true;
    }

    boolean efaBoathouseFinishSession(ItemTypeBoatstatusList.BoatListItem item) {
        this.setTitle(International.getString("Fahrt beenden"));
        saveButton.setDescription(International.getStringWithMnemonic("Fahrt beenden"));
        currentRecord = null;
        try {
            currentRecord = logbook.getLogbookRecord(item.boatStatus.getEntryNo());
            // New implementation in efaCloud. Causes NPE (in non-efaCloud usage). Removed.
            // currentRecord = logbook.getLogbookRecord(item.boat.getBoatStatus().getEntryNo());
        } catch(Exception e) {
            Logger.log(e);
        }
        if (currentRecord == null) {
            String msg =               International.getString("Fahrtende") + ": " +
              International.getMessage("Die gewählte Fahrt #{lfdnr} ({boot}) konnte nicht gefunden werden!",
              (item != null && item.boatStatus != null && item.boatStatus.getEntryNo() != null ? item.boatStatus.getEntryNo().toString(): "null"),
              (item != null && item.boat != null ? item.boat.getQualifiedName() : (item != null ? item.text : "null")));
            logBoathouseEvent(Logger.ERROR, Logger.MSG_ERR_NOLOGENTRYFORBOAT, msg, null);
            return false;
        }
        setFields(currentRecord);
        setTime(endtime, -Daten.efaConfig.getValueEfaDirekt_minusMinutenAnkunft(), currentRecord.getStartTime());
        setDesinationDistance();

        setFieldEnabled(false, true, entryno);
        setFieldEnabled(true, true, date);
        setFieldEnabled(false, true, boat);
        if (Daten.efaConfig.getValueEfaDirekt_eintragNichtAenderbarUhrzeit()) {
            setFieldEnabled(false, true, starttime);
            setFieldEnabled(false, true, endtime);
        } else {
            setFieldEnabled(true, true, starttime);
            setFieldEnabled(true, true, endtime);
        }
        setFieldEnabled(true, true, destination);
        setFieldEnabled(true, true, distance);
        setFieldEnabled(true, true, comments);
        setFieldEnabled(true, Daten.efaConfig.getValueEfaDirekt_showBootsschadenButton(), boatDamageButton);
        setFieldEnabled(true, Daten.efaConfig.getValueEfaDirekt_showBoatNotCleanedButton(), boatNotCleanedButton);

        currentBoatUpdateGui( (currentRecord.getBoatVariant() >= 0 ? currentRecord.getBoatVariant() : -1) );
        updateTimeInfoFields();
        setRequestFocus(destination);

        return true;
    }

    boolean efaBoathouseLateEntry(ItemTypeBoatstatusList.BoatListItem item) {
        this.setTitle(International.getString("Nachtrag"));
        saveButton.setDescription(International.getStringWithMnemonic("Nachtrag"));
        createNewRecord(false);
        date.parseAndShowValue(EfaUtil.getCurrentTimeStampDD_MM_YYYY());

        setFieldEnabled(false, true, entryno);
        setFieldEnabled(true, true, date);
        setFieldEnabled(true, true, boat);
        setFieldEnabled(true, true, starttime);
        setFieldEnabled(true, true, endtime);
        setFieldEnabled(true, true, destination);
        setFieldEnabled(true, true, distance);
        setFieldEnabled(true, true, comments);
        setFieldEnabled(true, Daten.efaConfig.getValueEfaDirekt_showBootsschadenButton(), boatDamageButton);
        setFieldEnabled(true, Daten.efaConfig.getValueEfaDirekt_showBoatNotCleanedButton(), boatNotCleanedButton);

        /* EFA_0015 - Late Entry shall use data from latest session, if boat and person both are null
         * (so the user started "late entry" without choosing a boat or a person in advance */
        if (item != null && (item.boat != null || item.person!=null)) {
        	efaBoathouseSetPersonAndBoat(item);
        } else {
        	if (Daten.efaConfig.getValueEfaDirekt_eintragPresentLastTripOnLateEntry()) {
        		efaBoathouseSetDataFromLatestSession();
        		// no special field to set focus on when presenting last Entry for late entry - we always start with date field in late entry.
        	}
        }
        
        updateTimeInfoFields();
        setRequestFocus(date);
        return true;
    }

    private void efaBoathouseSetDataFromLatestSession() {

    	LogbookRecord myReference = logbook.getLastLogbookRecord();

    	//last entry might be null if the logbook is empty
    	if (myReference != null) {
    		// we want to present the values of the last record only when we want to
    		// add a new entry within N minutes after the last one
    		// efaconfig makes sure that the value is >0
    		if ((System.currentTimeMillis()-myReference.getLastModified())< (Daten.efaConfig.getValueEfaDirekt_eintragPresentLastTripTimeout()*60*1000)) {
	    		setField(date, myReference);
	    		setField(enddate, myReference);
	    		setField(starttime, myReference);
	    		setField(endtime, myReference);
	    		setField(destination, myReference);
	            setDestinationInfo( (myReference != null ? myReference.getDestinationRecord(getValidAtTimestamp(myReference)) : null) );
	    		setField(waters, myReference);
	    		setField(distance,myReference);
	    		setField(comments, myReference);
    		}

    	}

    }
    
    
    boolean efaBoathouseAbortSession(ItemTypeBoatstatusList.BoatListItem item) {
        currentRecord = null;
        try {
            currentRecord = logbook.getLogbookRecord(item.boatStatus.getEntryNo());
        } catch(Exception e) {
            Logger.log(e);
        }
        if (currentRecord == null) {
            String msg =               International.getString("Fahrtende") + ": " +
              International.getMessage("Die gewählte Fahrt #{lfdnr} ({boot}) konnte nicht gefunden werden!",
              (item != null && item.boatStatus != null && item.boatStatus.getEntryNo() != null ? item.boatStatus.getEntryNo().toString(): "null"),
              (item != null && item.boat != null ? item.boat.getQualifiedName() : (item != null ? item.text : "null")));
            logBoathouseEvent(Logger.ERROR, Logger.MSG_ERR_NOLOGENTRYFORBOAT, msg, null);
            return false;
        }
        /* ** the following code has been moved to in 2.0.5_01 to prevent errors in remote access;
         * ** logbook record will now be deleted after status has been updated
        boolean checks = logbook.data().isPreModifyRecordCallbackEnabled();
        try {
            logbook.data().setPreModifyRecordCallbackEnabled(false); // otherwise we couldn't delete the record before we change the status
            logbook.data().delete(currentRecord.getKey());
        } catch(Exception e) {
            Dialog.error(e.toString());
            return false;
        }
        logbook.data().setPreModifyRecordCallbackEnabled(checks);
        */
        return true;
    }

    void updateBoatStatus(boolean success, int mode) {
        // log this action
        if (success) {
            switch(mode) {
                case MODE_BOATHOUSE_START:
                case MODE_BOATHOUSE_START_MULTISESSION:
                    logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_TRIPSTART,
                                      International.getString("Fahrtbeginn"),
                                      currentRecord);
                    break;
                case MODE_BOATHOUSE_START_CORRECT:
                    logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_TRIPSTART_CORR,
                                      International.getString("Fahrtbeginn korrigiert"),
                                      currentRecord);
                    break;
                case MODE_BOATHOUSE_FINISH:
                    logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_TRIPEND,
                                      International.getString("Fahrtende"),
                                      currentRecord);
                    break;
                case MODE_BOATHOUSE_LATEENTRY:
                case MODE_BOATHOUSE_LATEENTRY_MULTISESSION:
                    logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_TRIPLATEREC,
                                      International.getString("Nachtrag"),
                                      currentRecord);
                    break;
                case MODE_BOATHOUSE_ABORT:
                    logBoathouseEvent(Logger.INFO, Logger.MSG_EVT_TRIPABORT,
                                      International.getString("Fahrtabbruch"),
                                      currentRecord);
                    break;
            }
        } else {
            logBoathouseEvent(Logger.ERROR, Logger.MSG_EVT_ERRORSAVELOGBOOKENTRY,
                    International.getString("Fahrtenbucheintrag konnte nicht gespeichert werden."),
                    currentRecord);
        }

        // Update boat status
        if (success && currentRecord != null &&
            mode != MODE_BOATHOUSE_LATEENTRY && mode != MODE_BOATHOUSE_LATEENTRY_MULTISESSION &&
            (efaBoathouseAction != null || this.isModeFull())) {
            long tstmp = currentRecord.getValidAtTimestamp();
            BoatStatus boatStatus = Daten.project.getBoatStatus(false);
            BoatRecord boatRecord = currentRecord.getBoatRecord(tstmp);
            BoatStatusRecord boatStatusRecord = (boatRecord != null ? boatStatus.getBoatStatus(boatRecord.getId()) : null);
           
            // figure out new status information
            String newStatus = null;
            String newShowInList = null; // if not explicitly set, this boat will appear in the list determined by its status
            DataTypeIntString newEntryNo = null;
            String newComment = null;
            if (efaBoathouseAction != null) {
                mode = efaBoathouseAction.mode;
            }
            switch(mode) {
                case EfaBaseFrame.MODE_BOATHOUSE_START:
                case EfaBaseFrame.MODE_BOATHOUSE_START_CORRECT:
                case EfaBaseFrame.MODE_BOATHOUSE_START_MULTISESSION:
                    newStatus = BoatStatusRecord.STATUS_ONTHEWATER;
                    newEntryNo = currentRecord.getEntryId();
                    newComment = BoatStatusRecord.createStatusString(
                            currentRecord.getSessionType(),
                            currentRecord.getDestinationAndVariantName(tstmp),
                            currentRecord.getDate(),
                            currentRecord.getStartTime(),
                            currentRecord.getAllCoxAndCrewAsNameString(),
                            (currentRecord.getEndDate() != null ? currentRecord.getEndDate().toString() : null));
                    if (BoatStatusRecord.isOnTheWaterShowNotAvailable(currentRecord.getSessionType(),
                            currentRecord.getEndDate())) {
                        newShowInList = BoatStatusRecord.STATUS_NOTAVAILABLE;
                    }
                    break;
                case EfaBaseFrame.MODE_BOATHOUSE_FINISH:
                case EfaBaseFrame.MODE_BOATHOUSE_ABORT:
                    if (boatStatusRecord == null || boatStatusRecord.getBaseStatus() == null) {
                        newStatus = BoatStatusRecord.STATUS_AVAILABLE;
                    } else {
                        // this boat has a defined base status: use this status
                        newStatus = boatStatusRecord.getBaseStatus();
                    }             
                    newComment = "";
                    break;

                case EfaBaseFrame.MODE_BOATHOUSE_LATEENTRY:
                case EfaBaseFrame.MODE_BOATHOUSE_LATEENTRY_MULTISESSION:
                    break;
            }

            boolean newBoatStatusRecord = false;
            if (boatRecord != null && boatStatusRecord == null) {
                // oops, this shouldn't happen!
                String msg = International.getMessage("Kein Bootsstatus für Boot {boat} gefunden.",
                        boatRecord.getQualifiedName());
                logBoathouseEvent(Logger.ERROR, Logger.MSG_EVT_ERRORNOBOATSTATUSFORBOAT,
                        msg,currentRecord);
                Dialog.error(msg);
            } else {
                if (boatStatusRecord == null) {
                    // unknown boat
                    boatStatusRecord = (efaBoathouseAction != null ? efaBoathouseAction.boatStatus : null);

                    // it could be that a session has been corrected and efaBoathouseAction.boatStatus
                    // is actually the status of a real boat; if that's the case, then create a new
                    // boat status for the unknown boat
                    if (boatStatusRecord != null && !boatStatusRecord.getUnknownBoat()) {
                        boatStatusRecord = null;
                    }

                    if (boatStatusRecord == null &&
                        (mode == EfaBaseFrame.MODE_BOATHOUSE_START || mode == EfaBaseFrame.MODE_BOATHOUSE_START_CORRECT
                        || mode == EfaBaseFrame.MODE_BOATHOUSE_START_MULTISESSION)) {
                        // create new status record for unknown boat
                        boatStatusRecord = boatStatus.createBoatStatusRecord(UUID.randomUUID(), currentRecord.getBoatAsName());
                        newBoatStatusRecord = true;
                    }
                    if (boatStatusRecord != null) {
                        boatStatusRecord.setUnknownBoat(true);
                    }
                }
            }

            if (boatStatusRecord != null) {
                if (newStatus != null) {
                    boatStatusRecord.setCurrentStatus(newStatus);
                }
                if (newShowInList != null) {
                    boatStatusRecord.setShowInList(newShowInList);
                } else {
                    boatStatusRecord.setShowInList(null);
                }
                if (newEntryNo != null) {
                    boatStatusRecord.setEntryNo(newEntryNo);
                    boatStatusRecord.setLogbook(logbook.getName());
                } else {
                    boatStatusRecord.setEntryNo(null);
                    boatStatusRecord.setLogbook(null);
                }
                if (newComment != null) {
                    boatStatusRecord.setComment(newComment);
                }
                boatStatusRecord.setBoatText(currentRecord.getBoatAsName());
                try {
                    if (boatStatusRecord.getUnknownBoat() && newStatus != null &&
                        !newStatus.equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
                        boatStatus.data().delete(boatStatusRecord.getKey());
                    } else {
                        if (newBoatStatusRecord) {
                            boatStatus.data().add(boatStatusRecord);
                        } else {
                            boatStatus.data().update(boatStatusRecord);
                        }

                        // check whether we have changed the boat during this dialog (e.g. StartCorrect)
                        if (correctSessionLastBoatStatus != null && correctSessionLastBoatStatus.getBoatId() != null
                                && !boatStatusRecord.getBoatId().equals(correctSessionLastBoatStatus.getBoatId())) {
                            correctSessionLastBoatStatus.setCurrentStatus(BoatStatusRecord.STATUS_AVAILABLE);
                            correctSessionLastBoatStatus.setEntryNo(null);
                            correctSessionLastBoatStatus.setLogbook(null);
                            correctSessionLastBoatStatus.setComment("");
                            boatStatus.data().update(correctSessionLastBoatStatus);
                        }
                    }
                } catch(Exception e) {
                    Logger.log(e);
                }
            }

        }

        if (mode == MODE_BOATHOUSE_ABORT) {
            try {
                logbook.data().delete(currentRecord.getKey());
            } catch (Exception e) {
                Dialog.error(e.toString());
            }
        }
    }
    
    void finishBoathouseAction(boolean success) {
        updateBoatStatus(success, mode);
        efaBoathouseHideEfaFrame();
    }

    static String logEventInfoText(String logType, String logKey, String msg, LogbookRecord r) {
        String infoText = null;
        if (r != null) {
            long tstmp = r.getValidAtTimestamp();
            infoText = "#" + r.getEntryId().toString() + " - " + r.getBoatAsName(tstmp) + " " +
                          International.getMessage("mit {crew}", r.getAllCoxAndCrewAsNameString(tstmp));
        }
        return msg + (infoText != null ? ": " + infoText : "");
    }
    
    void logAdminEvent(String logType, String logKey, String msg, LogbookRecord r) {
        Logger.log(logType, logKey,
                International.getString("Admin") + " " + (admin != null ? admin.getName() : "<none>") + ": " +
                logEventInfoText(logType, logKey, msg, r));
    }

    public static void logBoathouseEvent(String logType, String logKey, String msg, LogbookRecord r) {
        Logger.log(logType, logKey, logEventInfoText(logType, logKey, msg, r));
    }

    void efaBoathouseSetFixedLocation(int x, int y) {
        if (x >= 0 && y >= 0) {
            this.positionX = x;
            this.positionY = y;
        }
        this.setLocation(this.positionX, this.positionY);
    }

    public void efaBoathouseShowEfaFrame() {
        if (infoLabel.isVisible() != Daten.efaConfig.getValueEfaDirekt_showEingabeInfos()) {
            infoLabel.setVisible(Daten.efaConfig.getValueEfaDirekt_showEingabeInfos());
        }
        packFrame("efaBoathouseShowEfaFrame(Component)");
        efaBoathouseSetFixedLocation(-1, -1);
        showMe();
        toFront();
        if (focusItem != null) {
            focusItem.requestFocus();
        }
    }

    private void efaBoathouseHideEfaFrame() {
        if (mode != EfaBaseFrame.MODE_BOATHOUSE_ABORT) {
            this.setVisible(false);
            Dialog.frameClosed(this);
            autoCompleteListPersons.reset();
        }
        efaBoathouseFrame.showEfaBoathouseFrame(efaBoathouseAction, currentRecord);
    }

    public static void logBoathouseEvent(String logType, String logKey, String msg, String boatName, String personName) {
        Logger.log(logType, logKey, logEventInfoText(logType, logKey, msg, boatName, personName));
    }

    public static String logEventInfoText(String logType, String logKey, String msg, String boatName, String personName) {
        String infoText; 
        infoText = "#" + "MultiSession" + " - " + boatName + " " +
                          International.getMessage("mit {crew}", personName);
        return msg + (infoText != null ? ": " + infoText : "");
    	
    }
    
}
