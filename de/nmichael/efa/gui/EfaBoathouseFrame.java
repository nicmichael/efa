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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Stack;
import java.util.UUID;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.CrontabThread;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.Admins;
import de.nmichael.efa.core.items.IItemListener;
import de.nmichael.efa.core.items.IItemType;
import de.nmichael.efa.core.items.ItemTypeBoatstatusList;
import de.nmichael.efa.core.items.ItemTypeBoolean;
import de.nmichael.efa.core.items.ItemTypeConfigButton;
import de.nmichael.efa.core.items.ItemTypeList;
import de.nmichael.efa.data.BoatDamageRecord;
import de.nmichael.efa.data.BoatDamages;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatReservations;
import de.nmichael.efa.data.BoatStatus;
import de.nmichael.efa.data.BoatStatusRecord;
import de.nmichael.efa.data.Clubwork;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.MessageRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.Project;
import de.nmichael.efa.data.efacloud.TxRequestQueue;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.dataedit.BoatDamageEditDialog;
import de.nmichael.efa.gui.dataedit.BoatReservationListDialog;
import de.nmichael.efa.gui.dataedit.ClubworkListDialog;
import de.nmichael.efa.gui.dataedit.MessageEditDialog;
import de.nmichael.efa.gui.dataedit.StatisticsListDialog;
import de.nmichael.efa.gui.util.EfaBoathouseBackgroundTask;
import de.nmichael.efa.gui.util.EfaMenuButton;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.gui.widgets.ClockMiniWidget;
import de.nmichael.efa.gui.widgets.IWidget;
import de.nmichael.efa.gui.widgets.NewsMiniWidget;
import de.nmichael.efa.gui.widgets.Widget;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Help;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;

public class EfaBoathouseFrame extends BaseFrame implements IItemListener {

	private static final long serialVersionUID = -382107679365085355L;
	public static EfaBoathouseFrame efaBoathouseFrame;

    public static final int EFA_EXIT_REASON_USER          = 0;
    public static final int EFA_EXIT_REASON_TIME          = 1;
    public static final int EFA_EXIT_REASON_IDLE          = 2;
    public static final int EFA_EXIT_REASON_OOME          = 3;
    public static final int EFA_EXIT_REASON_AUTORESTART   = 4;
    public static final int EFA_EXIT_REASON_ONLINEUPDATE  = 5;
    public static final int EFA_EXIT_REASON_SYSTEM	      = 6;

    public static final int ACTIONID_STARTSESSION        = 1;
    public static final int ACTIONID_FINISHSESSION       = 2;
    public static final int ACTIONID_LATEENTRY           = 3;
    public static final int ACTIONID_STARTSESSIONCORRECT = 4;
    public static final int ACTIONID_ABORTSESSION        = 5;
    public static final int ACTIONID_BOATRESERVATIONS    = 6;
    public static final int ACTIONID_BOATDAMAGES         = 7;
    public static final int ACTIONID_BOATINFOS           = 8;
    public static final int ACTIONID_LASTBOATUSAGE       = 9;

    String KEYACTION_F2;
    String KEYACTION_F3;
    String KEYACTION_F4;
    String KEYACTION_F5;
    String KEYACTION_F6;
    String KEYACTION_F7;
    String KEYACTION_F8;
    String KEYACTION_F9;
    String KEYACTION_altF10;
    String KEYACTION_altF11;
    String KEYACTION_F10;
    String KEYACTION_F11;
    String KEYACTION_F12;
    String KEYACTION_shiftF1;
    String KEYACTION_altX;
    String KEYACTION_shiftF4;

    // Boat List GUI Items
    JPanel boatsAvailablePanel;
    ItemTypeBoatstatusList boatsAvailableList; // booteVerfuegbar
    ItemTypeBoatstatusList personsAvailableList; // booteVerfuegbar
    ItemTypeBoatstatusList boatsOnTheWaterList; // booteAufFahrt
    ItemTypeBoatstatusList boatsNotAvailableList; // booteNichtVerfuegbar
    ButtonGroup toggleAvailableBoats = new ButtonGroup();
    JRadioButton toggleAvailableBoatsToBoats = new JRadioButton();
    JRadioButton toggleAvailableBoatsToPersons = new JRadioButton();

    // Center Panel GUI Items
    JLabel logoLabel = new JLabel();
    JButton startSessionButton = new JButton();
    JButton startSessionButtonMultiple = new JButton();
    JButton finishSessionButton = new JButton();
    JButton lateEntryButton = new JButton();
    JButton lateEntryButtonMultiple = new JButton();
    JButton abortSessionButton = new JButton();
    JButton boatReservationButton = new JButton();
    JButton messageToAdminButton = new JButton();
    JButton adminButton = new JButton();
    JButton specialButton = new JButton();
    JButton efaButton = new JButton();
    JButton helpButton = new JButton();
    JButton showLogbookButton = new JButton();
    JButton statisticsButton = new JButton();
    JButton clubworkButton = new JButton();

    // Widgets
    ClockMiniWidget clock;
    NewsMiniWidget news;
    Vector<IWidget> widgets;
    JPanel widgetTopPanel = new JPanel();
    JPanel widgetBottomPanel = new JPanel();
    JPanel widgetLeftPanel = new JPanel();
    JPanel widgetRightPanel = new JPanel();
    JPanel widgetCenterPanel = new JPanel();

    // South Panel GUI Items
    JLabel statusLabel = new JLabel();

    // Base GUI Items
    JPanel westPanel = new JPanel();
    JPanel eastPanel = new JPanel();
    JPanel centerPanel = new JPanel();
    JPanel northPanel = new JPanel();
    JPanel southPanel = new JPanel();
    

    // Window GUI Items
    JLabel titleLabel = new JLabel();
    JButton closeButton;
    
    // Data
    EfaBoathouseBackgroundTask efaBoathouseBackgroundTask;
    CrontabThread crontabThread;
    EfaBaseFrame efaBaseFrame;
    Logbook logbook;
    Clubwork clubwork;
    BoatStatus boatStatus;
    volatile long lastUserInteraction = System.currentTimeMillis();
    volatile boolean inUpdateBoatList = false;
    byte[] largeChunkOfMemory = new byte[1024*1024];
    boolean isLocked = false;


    public EfaBoathouseFrame() {
        super(null, Daten.EFA_LONGNAME);
        this.efaBoathouseFrame = this;
    }

    public void _keyAction(ActionEvent evt) {
        alive();
        if (evt == null || evt.getActionCommand() == null) {
            return;
        }

        if (evt.getActionCommand().equals(KEYACTION_ESCAPE)) {
            return; // do nothing (and don't invoke _keyAction(evt)!)
        }
        if (evt.getActionCommand().equals(KEYACTION_F2)) {
            actionStartSession(null);
        }
        if (evt.getActionCommand().equals(KEYACTION_F3)) {
            actionFinishSession();
        }
        if (evt.getActionCommand().equals(KEYACTION_F4)) {
            actionAbortSession();
        }
        if (evt.getActionCommand().equals(KEYACTION_F5)) {
            actionLateEntry();
        }
        if (evt.getActionCommand().equals(KEYACTION_F6)) {
            actionBoatReservations();
        }
        if (evt.getActionCommand().equals(KEYACTION_F7)) {
            actionShowLogbook();
        }
        if (evt.getActionCommand().equals(KEYACTION_F8)) {
            actionStatistics();
        }
        if (evt.getActionCommand().equals(KEYACTION_F9)) {
            actionMessageToAdmin();
        }
        if (evt.getActionCommand().equals(KEYACTION_altF10)) {
            actionAdminMode();
        }
        if (evt.getActionCommand().equals(KEYACTION_altF11)) {
            actionSpecial();
        }
        if (evt.getActionCommand().equals(KEYACTION_F10)) {
            if (toggleAvailableBoatsToBoats.isSelected()) {
                boatsAvailableList.requestFocus();
            } else {
                personsAvailableList.requestFocus();
            }
        }
        if (evt.getActionCommand().equals(KEYACTION_F11)) {
            boatsOnTheWaterList.requestFocus();
        }
        if (evt.getActionCommand().equals(KEYACTION_F12)) {
            boatsNotAvailableList.requestFocus();
        }
        if (evt.getActionCommand().equals(KEYACTION_shiftF1)) {
            EfaUtil.gc();
        }
        if (evt.getActionCommand().equals(KEYACTION_altX)) {
            cancel();
        }
        if (evt.getActionCommand().equals(KEYACTION_shiftF4)) {
            cancel();
        }

        super._keyAction(evt);
    }

    public void keyAction(ActionEvent evt) {
        _keyAction(evt);
    }

    protected void iniDialog() throws Exception {
        iniGuiBase();
        iniGuiMain();
        iniApplication();
        iniGuiRemaining();
        iniGuiHeaderColors();
        iniGuiTooltipDelays();
        prepareEfaBaseFrame();
    }

    private void iniGuiBase() {
        setIconImage(Toolkit.getDefaultToolkit().createImage(EfaBaseFrame.class.getResource("/de/nmichael/efa/img/efa_icon.png")));
        mainPanel.setLayout(new BorderLayout());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        //setting resizable to true would enable resizing the boathouse window.
        //but as the boatlists on the left and the right are on east and west, 
        //they do not scale horizontally with screen width.
        //so setResizable(true) would be useless.
        setResizable(false);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                this_windowClosing(e);
            }
            public void windowDeactivated(WindowEvent e) {
                this_windowDeactivated(e);
            }
            public void windowActivated(WindowEvent e) {
                this_windowActivated(e);
            }
            public void windowIconified(WindowEvent e) {
                this_windowIconified(e);
            }
        });

        // Key Actions
        KEYACTION_F2      = addKeyAction("F2");
        KEYACTION_F3      = addKeyAction("F3");
        KEYACTION_F4      = addKeyAction("F4");
        KEYACTION_F5      = addKeyAction("F5");
        KEYACTION_F6      = addKeyAction("F6");
        KEYACTION_F7      = addKeyAction("F7");
        KEYACTION_F8      = addKeyAction("F8");
        KEYACTION_F9      = addKeyAction("F9");
        KEYACTION_altF10  = addKeyAction("alt F10");
        KEYACTION_altF11  = addKeyAction("alt F11");
        KEYACTION_F10     = addKeyAction("F10");
        KEYACTION_F11     = addKeyAction("F11");
        KEYACTION_F12     = addKeyAction("F12");
        KEYACTION_shiftF1 = addKeyAction("shift F1");
        KEYACTION_altX    = addKeyAction("alt X");
        KEYACTION_shiftF4 = addKeyAction("shift F4");

        // Mouse Actions
        this.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                alive();
            }
            public void mouseExited(MouseEvent e) {
                alive();
            }
            public void mouseEntered(MouseEvent e) {
                alive();
            }
            public void mouseReleased(MouseEvent e) {
                alive();
            }
            public void mousePressed(MouseEvent e) {
                alive();
            }
        });
    }

    private void iniGuiMain() {
        iniGuiBoatLists();
        iniGuiCenterPanel();
        iniGuiNorthPanel();
        iniGuiSouthPanel();
        iniGuiPanels();
        updateGuiWidgets();
    }

    public void updateGuiElements() {
        updateGuiWidgets();
        updateGuiClock();
        updateGuiNews();
        updateGuiButtonText();
        updateGuiLogo();
    }

    private void iniGuiPanels() {
        widgetTopPanel.setLayout(new BorderLayout());
        widgetBottomPanel.setLayout(new BorderLayout());
        widgetLeftPanel.setLayout(new BorderLayout());
        widgetRightPanel.setLayout(new BorderLayout());
        widgetCenterPanel.setLayout(new BorderLayout());

        northPanel.add(widgetTopPanel, BorderLayout.CENTER);
        southPanel.add(widgetBottomPanel, BorderLayout.CENTER);
        westPanel.add(widgetLeftPanel, BorderLayout.WEST);
        eastPanel.add(widgetRightPanel, BorderLayout.EAST);
        centerPanel.add(widgetCenterPanel, new GridBagConstraints(1, 100, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 10, 10, 10), 0, 0));

        mainPanel.add(westPanel, BorderLayout.WEST);
        mainPanel.add(eastPanel, BorderLayout.EAST);
        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
    }
    
    private void openProjectLogbookClubwork() {
        if (Daten.project == null) {
            Logger.log(Logger.ERROR, Logger.MSG_ERR_NOPROJECTOPENED, International.getString("Kein Projekt geöffnet."));
        } else {
            openLogbook((AdminRecord) null);
            if (logbook == null) {
                Logger.log(Logger.ERROR, Logger.MSG_ERR_NOLOGBOOKOPENED, International.getString("Kein Fahrtenbuch geöffnet."));
            }
            openClubwork((AdminRecord) null);
        }
    }

    private void iniApplication() {
        openProject((AdminRecord)null);
        openProjectLogbookClubwork();

        updateBoatLists(true, false);

        EfaExitFrame.initExitFrame(this);

        // Speicher-Überwachung
        try {
            de.nmichael.efa.java15.Java15.setMemUsageListener(this, Daten.MIN_FREEMEM_COLLECTION_THRESHOLD);
        } catch (UnsupportedClassVersionError e) {
            EfaUtil.foo();
        } catch (NoClassDefFoundError e) {
            EfaUtil.foo();
        }

        // Background Task
        efaBoathouseBackgroundTask = new EfaBoathouseBackgroundTask(this);
        efaBoathouseBackgroundTask.start();

        // Crontab Task
        crontabThread = new CrontabThread();
        crontabThread.start();

        alive();
        Logger.log(Logger.INFO, Logger.MSG_EVT_EFAREADY, International.getString("PROJEKT_GELADEN"));
    }

    private void iniGuiRemaining() {
        // Fenster nicht verschiebbar
        if (Daten.efaConfig.getValueEfaDirekt_fensterNichtVerschiebbar()) {
            try {
                // must be called before any packing of the frame, since packing makes the frame displayable!
                this.setUndecorated(true);
                Color bgColor = new Color(0, 0, 170);

                EmptyBorder b = new EmptyBorder(2,2,2,2);
                mainPanel.setBackground(bgColor);
                mainPanel.setBorder(b);

                JMenuBar menuBar = new JMenuBar();
                menuBar.setLayout(new BorderLayout());
                if (!Daten.isEfaFlatLafActive()) {
                	//flatLaf has some issues with setting menubar colors manually.
                	//so we do this manual menu background color only if we don't have EFAFLATLAF
                	menuBar.setBackground(bgColor);
                	menuBar.setForeground(Color.white);
                } 
                JLabel efaLabel = new JLabel();
                efaLabel.setIcon(getIcon(ImagesAndIcons.IMAGE_EFA_ICON_SMALL ));
                efaLabel.setBackground(bgColor);
                efaLabel.setOpaque(true);
                titleLabel.setText(Daten.EFA_LONGNAME);
                titleLabel.setForeground(Color.white);
                titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD,12f));
                titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
                titleLabel.setHorizontalTextPosition(SwingConstants.LEFT);
                titleLabel.setIconTextGap(20);
                titleLabel.setBackground(bgColor);
                titleLabel.setOpaque(true);
                closeButton = new JButton();
                closeButton.setIcon(getIcon(ImagesAndIcons.IMAGE_FRAME_CLOSE ));
                closeButton.setBackground(bgColor);
                closeButton.setOpaque(true);
                closeButton.setForeground(Color.white);
                closeButton.setFont(closeButton.getFont().deriveFont(10f));
                closeButton.setBorder(null);
                closeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        cancel();
                    }
                });
                menuBar.add(efaLabel, BorderLayout.WEST);
                menuBar.add(titleLabel, BorderLayout.CENTER);
                menuBar.add(closeButton, BorderLayout.EAST);
                menuBar.setBorder(BorderFactory.createLineBorder(bgColor,2));
                menuBar.validate();
                this.setJMenuBar(menuBar);
                if (!Daten.isEfaFlatLafActive()) {
                	//flatLaf has some issues with setting menubar colors manually.
                	//so we do this manual menu background color only if we don't have EFAFLATLAF
                	menuBar.setBackground(bgColor);
                }
            } catch (NoSuchMethodError e) {
                Logger.log(Logger.WARNING, Logger.MSG_WARN_JAVA_VERSION,
                        "Only supported as of Java 1.4: " +e.toString());
            }
        }

        // Fenster immer im Vordergrund
        try {
            if (Daten.efaConfig.getValueEfaDirekt_immerImVordergrund()) {
                if (!de.nmichael.efa.java15.Java15.setAlwaysOnTop(this, true)) {
                    // Logger.log(Logger.WARNING,"Fenstereigenschaft 'immer im Vordergrund' wird erst ab Java 1.5 unterstützt.");
                    // Hier muß keine Warnung mehr ausgegeben werden, da ab v1.6.0 die Funktionalität auch für Java < 1.5
                    // durch einen Check alle 60 Sekunden nachgebildet wird.
                }
            }
        } catch (UnsupportedClassVersionError e) {
            // Java 1.3 kommt mit der Java 1.5 Klasse nicht klar
            Logger.log(Logger.WARNING, Logger.MSG_WARN_JAVA_VERSION,
                    "Only supported as of Java 5: " +e.toString());
        } catch (NoClassDefFoundError e) {
            Logger.log(Logger.WARNING, Logger.MSG_WARN_JAVA_VERSION,
                    "Only supported as of Java 5: " +e.toString());
        }

        // Fenster maximiert
        if (Daten.efaConfig.getValueEfaDirekt_startMaximized()) {
            try {
                //this.setSize(Dialog.screenSize);
                this.setMinimumSize(Dialog.screenSize);
                Dimension newsize = this.getSize();

                // breite für Scrollpanes ist (Fensterbreite - 20) / 2.
                //int width = (int) ((newsize.getWidth() - this.startSessionButton.getSize().getWidth() - 20) / 2);
                int width = (int) (newsize.getWidth() / 3);
                // die Höhe der Scrollpanes ist, da sie CENTER sind, irrelevant; nur für jScrollPane3
                // ist die Höhe ausschlaggebend.
                boatsAvailableList.setFieldSize(width, 400);
                personsAvailableList.setFieldSize(width, 400);
                boatsOnTheWaterList.setFieldSize(width, 200);
                boatsNotAvailableList.setFieldSize(width, Daten.efaConfig.getValueListSizeUnavailableBoats()); //(int) (newsize.getHeight() / 4));
                int height = (int) (20.0f * (Dialog.getFontSize() < 10 ? 12 : Dialog.getFontSize()) / Dialog.getDefaultFontSize());
                toggleAvailableBoatsToBoats.setPreferredSize(new Dimension(width / 2, height));
                toggleAvailableBoatsToPersons.setPreferredSize(new Dimension(width / 2, height));

                validate();
            } catch (Exception e) {
                Logger.logdebug(e);
            }
        }

        // Lock efa?
        if (Daten.efaConfig.getValueEfaDirekt_locked()) {
            // lock efa NOW
            lockEfa();
        }

        // Update Project Info
        updateProjectLogbookInfo();

        // note: packing must happen at the very end, since it makes the frame "displayable", which then
        // does not allow to change any window settings like setUndecorated()
        packFrame("iniGuiRemaining()");
        boatsAvailableList.requestFocus();
    }

	private void iniGuiBoatLists() {
        // Toggle between Boats and Persons
        Mnemonics.setButton(this, toggleAvailableBoatsToBoats, International.getStringWithMnemonic("Boote"));
        Mnemonics.setButton(this, toggleAvailableBoatsToPersons, International.getStringWithMnemonic("Personen"));
        toggleAvailableBoatsToBoats.setHorizontalAlignment(SwingConstants.RIGHT);
        toggleAvailableBoatsToPersons.setHorizontalAlignment(SwingConstants.LEFT);
        toggleAvailableBoats.add(toggleAvailableBoatsToBoats);
        toggleAvailableBoats.add(toggleAvailableBoatsToPersons);
        toggleAvailableBoatsToBoats.setSelected(true);
        toggleAvailableBoatsToBoats.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleAvailableBoats_actionPerformed(e);
            }
        });
        toggleAvailableBoatsToPersons.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleAvailableBoats_actionPerformed(e);
            }
        });
        // Update GUI Elements for Boat Lists
        toggleAvailableBoatsToBoats.setVisible(Daten.efaConfig.getValueEfaDirekt_listAllowToggleBoatsPersons());
        toggleAvailableBoatsToPersons.setVisible(Daten.efaConfig.getValueEfaDirekt_listAllowToggleBoatsPersons());

        // Boat Lists
        boatsAvailableList = new ItemTypeBoatstatusList("BOATSAVAILABLELIST", IItemType.TYPE_PUBLIC, "", International.getStringWithMnemonic("verfügbare Boote"), this, Daten.efaConfig.getValueEfaBoathouseFilterTextfieldStandardLists(), Daten.efaConfig.getValueEfaBoathouseTwoColumnList());
        personsAvailableList = new ItemTypeBoatstatusList("PERSONSAVAILABLELIST", IItemType.TYPE_PUBLIC, "", International.getStringWithMnemonic("Personen"), this,Daten.efaConfig.getValueEfaBoathouseFilterTextfieldStandardLists(), Daten.efaConfig.getValueEfaBoathouseTwoColumnList());
        boatsOnTheWaterList = new ItemTypeBoatstatusList("BOATSONTHEWATERLIST", IItemType.TYPE_PUBLIC, "", International.getStringWithMnemonic("Boote auf Fahrt"), this,Daten.efaConfig.getValueEfaBoathouseFilterTextfieldStandardLists(), Daten.efaConfig.getValueEfaBoathouseTwoColumnList());
        boatsNotAvailableList = new ItemTypeBoatstatusList("BOATSNOTAVAILABLELIST", IItemType.TYPE_PUBLIC, "", International.getStringWithMnemonic("nicht verfügbare Boote"), this,Daten.efaConfig.getValueEfaBoathouseFilterTextfieldBoatsNotAvailableList(), Daten.efaConfig.getValueEfaBoathouseTwoColumnList());        
        boatsAvailableList.setFieldSize(200, 400);
        personsAvailableList.setFieldSize(200, 400);
        boatsOnTheWaterList.setFieldSize(200, 300);
        boatsNotAvailableList.setFieldSize(200, 100);
        boatsAvailableList.setPopupActions(getListActions(1, null));
        personsAvailableList.setPopupActions(getListActions(101, null));
        boatsOnTheWaterList.setPopupActions(getListActions(2, null));
        boatsNotAvailableList.setPopupActions(getListActions(3, null));
        boatsAvailableList.registerItemListener(this);
        personsAvailableList.registerItemListener(this);
        boatsOnTheWaterList.registerItemListener(this);
        boatsNotAvailableList.registerItemListener(this);
        
        //Highlight for Lists
        iniGuiHeaderColors();        
        
        iniGuiListNames();

        // add Panels to Gui
        boatsAvailablePanel = new JPanel();
        boatsAvailablePanel.setLayout(new BorderLayout());
        boatsAvailableList.displayOnGui(this, boatsAvailablePanel, BorderLayout.CENTER);
        // personsAvailableList.displayOnGui(this, boatsAvailablePanel, BorderLayout.CENTER); // Cannot be displayed here and now, only when toggled to!
        JPanel togglePanel = new JPanel();
        togglePanel.add(toggleAvailableBoatsToBoats, null);
        togglePanel.add(toggleAvailableBoatsToPersons, null);
        togglePanel.setVisible(Daten.efaConfig.getValueEfaDirekt_listAllowToggleBoatsPersons());
        boatsAvailablePanel.add(togglePanel, BorderLayout.NORTH);
        westPanel.setLayout(new BorderLayout());
        westPanel.add(boatsAvailablePanel, BorderLayout.CENTER);

        JPanel boatsNotAvailablePanel = new JPanel();
        boatsNotAvailablePanel.setLayout(new BorderLayout());
        boatsOnTheWaterList.displayOnGui(this, boatsNotAvailablePanel, BorderLayout.CENTER);
        boatsNotAvailableList.displayOnGui(this, boatsNotAvailablePanel, BorderLayout.SOUTH);
        eastPanel.setLayout(new BorderLayout());
        eastPanel.add(boatsNotAvailablePanel, BorderLayout.CENTER);
    }

	private void iniGuiHeaderColors() {
		if (Daten.efaConfig.getHeaderUseHighlightColor()) {
			boatsAvailableList.setColor(Daten.efaConfig.getHeaderForegroundColor());
	        personsAvailableList.setColor(Daten.efaConfig.getHeaderForegroundColor());
	        boatsOnTheWaterList.setColor(Daten.efaConfig.getHeaderForegroundColor());
	        boatsNotAvailableList.setColor(Daten.efaConfig.getHeaderForegroundColor());
	        boatsAvailableList.setBackgroundColor(Daten.efaConfig.getHeaderBackgroundColor());
	        personsAvailableList.setBackgroundColor(Daten.efaConfig.getHeaderBackgroundColor());
	        boatsOnTheWaterList.setBackgroundColor(Daten.efaConfig.getHeaderBackgroundColor());
	        boatsNotAvailableList.setBackgroundColor(Daten.efaConfig.getHeaderBackgroundColor());
	        
	        EfaUtil.handleTabbedPaneBackgroundColorForLookAndFeels();
		} else {
			boatsAvailableList.setColor(null);
	        personsAvailableList.setColor(null);
	        boatsOnTheWaterList.setColor(null);
	        boatsNotAvailableList.setColor(null);
	        boatsAvailableList.setBackgroundColor(null);
	        personsAvailableList.setBackgroundColor(null);
	        boatsOnTheWaterList.setBackgroundColor(null);
	        boatsNotAvailableList.setBackgroundColor(null);
		}
		boatsAvailableList.updateSeparatorColorFromEfaConfig();
		personsAvailableList.updateSeparatorColorFromEfaConfig();
		boatsNotAvailableList.updateSeparatorColorFromEfaConfig();
		boatsOnTheWaterList.updateSeparatorColorFromEfaConfig();
	}
	
	private void iniGuiTooltipDelays() {        
		int initialDelay=Daten.efaConfig.getValueEfaBoathouseExtdToolTipInitialDelayMsec();
	    int dismissDelay=Daten.efaConfig.getValueEfaBoathouseExtdToolTipDismissDelayMsec();
	    if (initialDelay>0) {
	    	javax.swing.ToolTipManager.sharedInstance().setInitialDelay(initialDelay);
	    	javax.swing.ToolTipManager.sharedInstance().setReshowDelay(initialDelay);
	    }
	    if (dismissDelay>0) {
	    	javax.swing.ToolTipManager.sharedInstance().setDismissDelay(dismissDelay);
	    }
	}
    private void iniGuiListNames() {
        boolean fkey = Daten.efaConfig.getValueEfaDirekt_showButtonHotkey();
        if (!Daten.efaConfig.getValueEfaDirekt_listAllowToggleBoatsPersons() || toggleAvailableBoatsToBoats.isSelected()) {
            boatsAvailableList.setDescription(International.getString("verfügbare Boote") + (fkey ? " [F10]" : ""));
        } else {
            personsAvailableList.setDescription(International.getString("Personen") + (fkey ? " [F10]" : ""));
        }
        boatsOnTheWaterList.setDescription(International.getString("Boote auf Fahrt") + (fkey ? " [F11]" : ""));
        boatsNotAvailableList.setDescription(International.getString("nicht verfügbare Boote") + (fkey ? " [F12]" : ""));
    }

    private void iniGuiCenterPanel() {
        updateGuiLogo();
        iniGuiButtons();
        updateGuiClock();

        centerPanel.setLayout(new GridBagLayout());
        int logoTop = (int) (10.0f * (Dialog.getFontSize() < 10 ? 12 : Dialog.getFontSize()) / Dialog.getDefaultFontSize());
        int logoBottom = 5;
        if (Daten.efaConfig.getValueEfaDirekt_startMaximized() && Daten.efaConfig.getValueEfaDirekt_vereinsLogo().length() > 0) {
            logoBottom += (int) ((Dialog.screenSize.getHeight() - 825) / 5);
            if (logoBottom < 0) {
                logoBottom = 0;
            }
        }
        centerPanel.add(logoLabel, new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(logoTop, 0, logoBottom, 0), 0, 0));
        int fahrtbeginnTop = (int) (10.0f * (Dialog.getFontSize() < 10 ? 12 : Dialog.getFontSize()) / Dialog.getDefaultFontSize());
        
        // startSessionButton and startSessionButtonMultiple shall be on the same line, 
        // and take the same width as the other buttons. So we create a panel containing
        // startSessionButton and startSessionButtonMultiple. The latter gets hidden if it shall not be used.
        
        JPanel myPanel=new JPanel();
    	myPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
    	myPanel.setLayout(new GridBagLayout());
    	// StartSession takes 8/9th of width and grows horizontally, multissessionbutton grows vertically as the icon alone makes the button to look smallish
    	myPanel.add(startSessionButton,      new GridBagConstraints(1, 1, 8, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    	myPanel.add(startSessionButtonMultiple, new GridBagConstraints(9, 1, 1, 1, 0.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 2, 0, 0), 0, 0));
    	startSessionButtonMultiple.setVisible(Daten.efaConfig.getValueEfaDirekt_MultisessionSupportLateEntry());    

    	centerPanel.add(myPanel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(fahrtbeginnTop, 0, 0, 0), 0, 0));        
        centerPanel.add(finishSessionButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        centerPanel.add(abortSessionButton, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));

        // same for lateEntryButton and lateEntryButtonMultiple
    	myPanel=new JPanel();
    	myPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
    	myPanel.setLayout(new GridBagLayout());
    	// StartSession takes 8/9th of width and grows horizontally, multissessionbutton grows vertically as the icon alone makes the button to look smallish
    	myPanel.add(lateEntryButton,      new GridBagConstraints(1, 1, 8, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    	myPanel.add(lateEntryButtonMultiple, new GridBagConstraints(9, 1, 1, 1, 0.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 2, 0, 0), 0, 0));
    	lateEntryButtonMultiple.setVisible(Daten.efaConfig.getValueEfaDirekt_MultisessionSupportLateEntry());    

    	centerPanel.add(myPanel, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));        
        
        centerPanel.add(clubworkButton, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        centerPanel.add(boatReservationButton, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        centerPanel.add(showLogbookButton, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        centerPanel.add(statisticsButton, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        centerPanel.add(messageToAdminButton, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        centerPanel.add(adminButton, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        centerPanel.add(specialButton, new GridBagConstraints(1, 12, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 0, 0), 0, 0));
        centerPanel.add(helpButton, new GridBagConstraints(1, 13, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
        centerPanel.add(efaButton, new GridBagConstraints(1, 14, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
        centerPanel.add(clock.getGuiComponent(), new GridBagConstraints(1, 15, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
        
        EfaUtil.handleButtonOpaqueForLookAndFeels(startSessionButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(startSessionButtonMultiple);
        EfaUtil.handleButtonOpaqueForLookAndFeels(finishSessionButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(abortSessionButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(lateEntryButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(lateEntryButtonMultiple);
        EfaUtil.handleButtonOpaqueForLookAndFeels(clubworkButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(boatReservationButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(showLogbookButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(statisticsButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(messageToAdminButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(adminButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(specialButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(helpButton);
        EfaUtil.handleButtonOpaqueForLookAndFeels(efaButton);
    }

    private void updateGuiLogo() {
        if (Daten.efaConfig.getValueEfaDirekt_vereinsLogo() != null &&
            Daten.efaConfig.getValueEfaDirekt_vereinsLogo().length() > 0) {
            try {
                logoLabel.setIcon(new ImageIcon(Daten.efaConfig.getValueEfaDirekt_vereinsLogo()));
                logoLabel.setMinimumSize(new Dimension(200, 80));
                if (logoLabel.getIcon()!=null) {
                	logoLabel.setPreferredSize(new Dimension(
                				Math.max(200, logoLabel.getIcon().getIconWidth()), 
                				Math.max(80, logoLabel.getIcon().getIconHeight())));
                } else {
                	logoLabel.setPreferredSize(new Dimension(200,80));
                }
                logoLabel.setMaximumSize(new Dimension(logoLabel.getWidth(),logoLabel.getHeight()));
                logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
                logoLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            } catch (Exception e) {
                Logger.logdebug(e);
            }
        } else {
            logoLabel.setIcon(null);
        }
    }

    private void iniGuiButtons() {
        Mnemonics.setButton(this, startSessionButton, International.getStringWithMnemonic("Fahrt beginnen"));
        startSessionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionStartSession(null);
            }
        });
        
    	startSessionButtonMultiple.setToolTipText(International.getString("Mehrere Einzelfahrten beginnen"));
    	startSessionButtonMultiple.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionStartSessionMultiple();
            }
        });          

        Mnemonics.setButton(this, finishSessionButton, International.getStringWithMnemonic("Fahrt beenden"));
        finishSessionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionFinishSession();
            }
        });

        Mnemonics.setButton(this, abortSessionButton, International.getStringWithMnemonic("Fahrt abbrechen"));
        abortSessionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionAbortSession();
            }
        });

        Mnemonics.setButton(this, lateEntryButton, International.getStringWithMnemonic("Nachtrag"));
        lateEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionLateEntry();
            }
        });
        
    	lateEntryButtonMultiple.setToolTipText(International.getString("Mehrere Einzelfahrten nachtragen"));
    	lateEntryButtonMultiple.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionLateEntryMultiple();
            }
        });   

        Mnemonics.setButton(this, boatReservationButton, International.getStringWithMnemonic("Bootsreservierungen"));
        boatReservationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionBoatReservations();
            }
        });

        Mnemonics.setButton(this, showLogbookButton, International.getStringWithMnemonic("Fahrtenbuch anzeigen"));
        showLogbookButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionShowLogbook();
            }
        });

        Mnemonics.setButton(this, statisticsButton, International.getStringWithMnemonic("Statistik erstellen"));
        statisticsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionStatistics();
            }
        });

        Mnemonics.setButton(this, clubworkButton, International.getStringWithMnemonic("Vereinsarbeit"));
        clubworkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionClubwork();
            }
        });

        Mnemonics.setButton(this, messageToAdminButton, International.getStringWithMnemonic("Nachricht an Admin"));
        messageToAdminButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionMessageToAdmin();
            }
        });

        Mnemonics.setButton(this, adminButton, International.getStringWithMnemonic("Admin-Modus"));
        adminButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionAdminMode();
            }
        });

        specialButton.setText(International.getString("Spezial-Button"));
        specialButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionSpecial();
            }
        });

        helpButton.setText(International.getMessage("Hilfe mit {key}", "[F1]"));
        helpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hilfeButton_actionPerformed(e);
            }
        });

        efaButton.setPreferredSize(new Dimension(90, 55));
        efaButton.setIcon(getIcon(Daten.getEfaImage(1)));
        efaButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                efaButton_actionPerformed(e);
            }
        });

        updateGuiButtonText();
    }

    private void setButtonLAF(JButton button, ItemTypeConfigButton config, String icon) {
        if (config != null) {
            button.setBackground(EfaUtil.getColorOrGray(config.getValueColor()));
        }
        if (icon != null && button.isVisible()) {
            button.setIcon(getIcon(icon));
            button.setIconTextGap(10);
            button.setHorizontalAlignment(SwingConstants.LEFT);
        }
        //Always use bold font for Buttons.
        button.setFont(button.getFont().deriveFont(Font.BOLD));
    }

    private void updateGuiButtonLAF() {
    	this.startSessionButtonMultiple.setVisible(Daten.efaConfig.getValueEfaDirekt_MultisessionSupportStartSession());
    	this.lateEntryButtonMultiple.setVisible(Daten.efaConfig.getValueEfaDirekt_MultisessionSupportLateEntry());
    	
        this.boatReservationButton.setVisible(Daten.efaConfig.getValueEfaDirekt_butBootsreservierungen().getValueShow());
        this.showLogbookButton.setVisible(Daten.efaConfig.getValueEfaDirekt_butFahrtenbuchAnzeigen().getValueShow());
        this.statisticsButton.setVisible(Daten.efaConfig.getValueEfaDirekt_butStatistikErstellen().getValueShow());
        this.clubworkButton.setVisible(Daten.efaConfig.getValueEfaDirekt_butVereinsarbeit().getValueShow() && clubwork != null && clubwork.isOpen());
        this.messageToAdminButton.setVisible(Daten.efaConfig.getValueEfaDirekt_butNachrichtAnAdmin().getValueShow());
        this.adminButton.setVisible(Daten.efaConfig.getValueEfaDirekt_butAdminModus().getValueShow());
        this.specialButton.setVisible(Daten.efaConfig.getValueEfaDirekt_butSpezial().getValueShow());
        this.helpButton.setVisible(Daten.efaConfig.getValueEfaDirekt_butHelp().getValueShow());

        setButtonLAF(startSessionButton, Daten.efaConfig.getValueEfaDirekt_butFahrtBeginnen(), ImagesAndIcons.IMAGE_ACTION_START_SESSION);
        setButtonLAF(startSessionButtonMultiple, Daten.efaConfig.getValueEfaDirekt_butFahrtBeginnen(), ImagesAndIcons.IMAGE_ACTION_START_SESSION_MULTI);
        setButtonLAF(finishSessionButton, Daten.efaConfig.getValueEfaDirekt_butFahrtBeenden(), ImagesAndIcons.IMAGE_ACTION_FINISH_SESSION);
        setButtonLAF(abortSessionButton, Daten.efaConfig.getValueEfaDirekt_butFahrtAbbrechen(), ImagesAndIcons.IMAGE_ACTION_ABORT_SESSION);
        setButtonLAF(lateEntryButton, Daten.efaConfig.getValueEfaDirekt_butNachtrag(), ImagesAndIcons.IMAGE_ACTION_LATE_ENTRY);
        setButtonLAF(lateEntryButtonMultiple, Daten.efaConfig.getValueEfaDirekt_butNachtrag(),ImagesAndIcons.IMAGE_ACTION_LATE_ENTRY_MULTI);
        setButtonLAF(boatReservationButton, Daten.efaConfig.getValueEfaDirekt_butBootsreservierungen(), ImagesAndIcons.IMAGE_ACTION_BOAT_RESERVATIONS);
        setButtonLAF(showLogbookButton, Daten.efaConfig.getValueEfaDirekt_butFahrtenbuchAnzeigen(), ImagesAndIcons.IMAGE_ACTION_LOGBOOK);
        setButtonLAF(statisticsButton, Daten.efaConfig.getValueEfaDirekt_butStatistikErstellen(), ImagesAndIcons.IMAGE_ACTION_STATISTICS);
        setButtonLAF(clubworkButton, Daten.efaConfig.getValueEfaDirekt_butVereinsarbeit(), ImagesAndIcons.IMAGE_ACTION_CLUBWORK);
        setButtonLAF(messageToAdminButton, Daten.efaConfig.getValueEfaDirekt_butNachrichtAnAdmin(), ImagesAndIcons.IMAGE_ACTION_MESSAGE);
        setButtonLAF(adminButton, Daten.efaConfig.getValueEfaDirekt_butAdminModus(), ImagesAndIcons.IMAGE_ACTION_ADMIN);
        setButtonLAF(specialButton, Daten.efaConfig.getValueEfaDirekt_butSpezial(), ImagesAndIcons.IMAGE_ACTION_SPECIAL);
        setButtonLAF(helpButton, null, ImagesAndIcons.IMAGE_ACTION_HELP);

    }

    public void updateGuiButtonText() {
        boolean fkey = Daten.efaConfig.getValueEfaDirekt_showButtonHotkey();
        this.startSessionButton.setText(Daten.efaConfig.getValueEfaDirekt_butFahrtBeginnen().getValueText() + (fkey ? " [F2]" : ""));
        this.finishSessionButton.setText(Daten.efaConfig.getValueEfaDirekt_butFahrtBeenden().getValueText() + (fkey ? " [F3]" : ""));
        this.abortSessionButton.setText(International.getString("Fahrt abbrechen") + (fkey ? " [F4]" : ""));
        this.lateEntryButton.setText(International.getString("Nachtrag") + (fkey ? " [F5]" : ""));
        this.boatReservationButton.setText(International.getString("Bootsreservierungen") + (fkey ? " [F6]" : ""));
        this.clubworkButton.setText(International.getString("Vereinsarbeit") + (fkey ? " [Alt-F6]" : ""));
        this.showLogbookButton.setText(International.getString("Fahrtenbuch anzeigen") + (fkey ? " [F7]" : ""));
        this.statisticsButton.setText(International.getString("Statistik erstellen") + (fkey ? " [F8]" : ""));
        this.messageToAdminButton.setText(International.getString("Nachricht an Admin") + (fkey ? " [F9]" : ""));
        this.adminButton.setText(International.getString("Admin-Modus") + (fkey ? " [Alt-F10]" : ""));
        this.specialButton.setText(Daten.efaConfig.getValueEfaDirekt_butSpezial().getValueText() + (fkey ? " [Alt-F11]" : ""));
        updateGuiButtonLAF();
        if (!Daten.efaConfig.getValueEfaDirekt_startMaximized() && isDisplayable()) {
            packFrame("iniButtonText()");
        }
    }

    private void updateGuiClock() {
        if (clock == null) {
            clock = new ClockMiniWidget();
        }
        clock.setVisible(Daten.efaConfig.getValueEfaDirekt_showUhr());
    }

    private void updateGuiNews() {
        if (news == null) {
            news = new NewsMiniWidget();
        }
        news.setText(Daten.efaConfig.getValueEfaDirekt_newsText());
        news.setScrollSpeed(Daten.efaConfig.getValueEfaDirekt_newsScrollSpeed());
        news.setVisible(Daten.efaConfig.getValueEfaDirekt_showNews());
        if (isDisplayable()) {
            packFrame("updateGuiNews()");
        }
    }

    private void updateGuiWidgets() {
        try {
            widgetTopPanel.removeAll();
            widgetBottomPanel.removeAll();
            widgetLeftPanel.removeAll();
            widgetRightPanel.removeAll();
            widgetCenterPanel.removeAll();
            
            if (Daten.efaConfig.getWidgets() == null) {
                return;
            }

            // stop all previously started widgets
            try {
                for (int i = 0; widgets != null && i < widgets.size(); i++) {
                    IWidget w = widgets.get(i);
                    w.stop();
                }
            } catch (Exception e) {
                Logger.logdebug(e);
            }
            widgets = new Vector<IWidget>();

            // find all enabled widgets
            Vector<IWidget> allWidgets = Widget.getAllWidgets();
            for (int i = 0; allWidgets != null && i < allWidgets.size(); i++) {
                IWidget w = allWidgets.get(i);
                IItemType enabled = Daten.efaConfig.getExternalGuiItem(w.getParameterName(Widget.PARAM_ENABLED));
                if (enabled != null && enabled instanceof ItemTypeBoolean && ((ItemTypeBoolean) enabled).getValue()) {
                    // set parameters for this enabled widget according to configuration
                    IItemType[] params = w.getParameters();
                    for (int j = 0; j < params.length; j++) {
                        params[j].parseValue(Daten.efaConfig.getExternalGuiItem(params[j].getName()).toString());
                    }
                    widgets.add(w);
                }
            }

            // show all enabled widgets
            for (int i = 0; i < widgets.size(); i++) {
                IWidget w = widgets.get(i);
                String position = w.getPosition();
                if (IWidget.POSITION_TOP.equals(position)) {
                    w.show(widgetTopPanel, BorderLayout.CENTER);
                }
                if (IWidget.POSITION_BOTTOM.equals(position)) {
                    w.show(widgetBottomPanel, BorderLayout.CENTER);
                }
                if (IWidget.POSITION_LEFT.equals(position)) {
                    w.show(widgetLeftPanel, BorderLayout.CENTER);
                }
                if (IWidget.POSITION_RIGHT.equals(position)) {
                    w.show(widgetRightPanel, BorderLayout.CENTER);
                }
                if (IWidget.POSITION_CENTER.equals(position)) {
                    w.show(widgetCenterPanel, BorderLayout.CENTER);
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
    }

    private void iniGuiNorthPanel() {
        updateGuiNews();
        northPanel.setLayout(new BorderLayout());
    }

    private void iniGuiSouthPanel() {
        updateGuiNews();
        southPanel.setLayout(new BorderLayout());

        southPanel.add(statusLabel, BorderLayout.NORTH);
        southPanel.add(news.getGuiComponent(), BorderLayout.SOUTH);
        statusLabelSetText(International.getString("Status"));

    }

    private void statusLabelSetText(String s) {
        statusLabel.setText(" "+ s);
        // wenn Text zu lang, dann PreferredSize verringern, damit bei pack() die zu große Label-Breite nicht
        // zum Vergrößern des Fensters führt!
        if (statusLabel.getPreferredSize().getWidth() > this.getSize().getWidth()) {
            statusLabel.setPreferredSize(new Dimension((int) this.getSize().getWidth() - 20,
                    (int) statusLabel.getPreferredSize().getHeight()));
        }
    }

    public void packFrame(String source) {
        this.pack();
    }

    public void bringFrameToFront() {
        this.toFront();
    }

    // i == 0 - automatically try to find correct list to focus
    // i == 1 - boats/persons available
    // i == 2 - boats on the water
    // i == 3 - boats not available
    public void boatListRequestFocus(int i) {
    	
    	clearListFilterAfterInterval();
    	
        if (i == 0) {
            if (boatsAvailableList != null && boatsAvailableList.getSelectedIndex() >= 0) {
                boatsAvailableList.requestFocus();
            } else if (personsAvailableList != null && personsAvailableList.getSelectedIndex() >= 0) {
                personsAvailableList.requestFocus();
            } else if (boatsOnTheWaterList != null && boatsOnTheWaterList.getSelectedIndex() >= 0) {
                boatsOnTheWaterList.requestFocus();
            } else if (boatsNotAvailableList != null && boatsNotAvailableList.getSelectedIndex() >= 0) {
                boatsNotAvailableList.requestFocus();
            } else if (boatsAvailableList != null) {
                boatsAvailableList.requestFocus();
            }
        }
        if (i == 1) {
            if (toggleAvailableBoatsToBoats.isSelected()) {
                boatsAvailableList.requestFocus();
            } else {
                personsAvailableList.requestFocus();
            }
        }
        if (i == 2) {
            boatsOnTheWaterList.requestFocus();
        }
        if (i == 3) {
            boatsNotAvailableList.requestFocus();
        }
    }

    // Clears filter text of the main lists on efaBoatHouseFrame, if filter text fields
    // are visible. The interval can be configured in efaConfig. The lists themselfves the lists themselves.
    // also, the lists filter text field gets cleared if they get the focus and the last
    // change within the filter is older than the time interval.
    public void clearListFilterAfterInterval() {
    	boatsAvailableList.clearFilterTextByInterval();
    	personsAvailableList.clearFilterTextByInterval();
    	boatsOnTheWaterList.clearFilterTextByInterval();
    	boatsNotAvailableList.clearFilterTextByInterval();
    }
    
    /*
     * Clear filter field 
     */
    public void clearFilterFieldsIfConfigured() {
    	if (Daten.efaConfig.getValueEfaBoathouseFilterTextAutoClearAfterAction()) {
        	boatsAvailableList.clearFilterText();
        	personsAvailableList.clearFilterText();
        	boatsOnTheWaterList.clearFilterText();
        	boatsNotAvailableList.clearFilterText();
    	}
    	boatListRequestFocus(0);//automatically detect the boatlist to get the focus          	
    }


    void alive() {
        lastUserInteraction = System.currentTimeMillis();
    }

    public long getLastUserInteraction() {
        return lastUserInteraction;
    }

    private void this_windowClosing(WindowEvent e) {
        cancel(e, EFA_EXIT_REASON_USER, null, false);
    }

    private void this_windowDeactivated(WindowEvent e) {
        // nothing to do
    }

    private void this_windowActivated(WindowEvent e) {
        try {
            if (!isEnabled() && efaBaseFrame != null) {
                efaBaseFrame.toFront();
            }
        } catch (Exception ee) {
            Logger.logdebug(ee);
        }
    }

    private void this_windowIconified(WindowEvent e) {
        //super.processWindowEvent(e);
        this.setState(Frame.NORMAL);
    }

    public boolean cancel() {
        return cancel(null, EFA_EXIT_REASON_USER, null, false);
    }

    public boolean cancel(WindowEvent e, int reason, AdminRecord admin, boolean restart) {
        Dialog.IGNORE_WINDOW_STACK_CHECKS = true;
        int exitCode = 0;
        String who = "unknown";
        Daten.isShutdownRequested=true;
        switch (reason) {
            case EFA_EXIT_REASON_USER: // manuelles Beenden von efa
                boolean byUser;
                if (admin != null || !Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenEfaBeenden()) {
                    while (admin == null || !admin.isAllowedExitEfa()) {
                        if (admin == null) {
                            admin = AdminLoginDialog.login(this, International.getString("Beenden von efa"));
                            if (admin == null) {
                                Dialog.IGNORE_WINDOW_STACK_CHECKS = false;
                                Daten.isShutdownRequested=false;
                                return false;
                            }
                        }
                        if (admin != null && !admin.isAllowedExitEfa()) {
                            EfaMenuButton.insufficientRights(admin, EfaMenuButton.BUTTON_EXIT);
                            admin = null;
                        }
                    }
                    who = International.getString("Admin") + "=" + admin.getName();
                    byUser = false;
                } else {
                    who = International.getString("Nutzer");
                    byUser = true;
                }
                if (Daten.efaConfig.getValueEfaDirekt_execOnEfaExit().length() > 0 && byUser) {
                    Logger.log(Logger.INFO, Logger.MSG_EVT_EFAEXITEXECCMD,
                            International.getMessage("Programmende veranlaßt; versuche, Kommando '{cmd}' auszuführen...", Daten.efaConfig.getValueEfaDirekt_execOnEfaExit()));
                    try {
                        Runtime.getRuntime().exec(Daten.efaConfig.getValueEfaDirekt_execOnEfaExit());
                    } catch (Exception ee) {
                        Logger.log(Logger.ERROR, Logger.MSG_ERR_EFAEXITEXECCMD_FAILED,
                                LogString.cantExecCommand(Daten.efaConfig.getValueEfaDirekt_execOnEfaExit(), International.getString("Kommando")));
                    }
                }
                break;
            case EFA_EXIT_REASON_TIME:
            case EFA_EXIT_REASON_IDLE:
                who = International.getString("Zeitsteuerung");
                who += " (" + (reason == EFA_EXIT_REASON_TIME ? "time" : "idle") + ")";
                if (Daten.efaConfig.getValueEfaDirekt_execOnEfaAutoExit().length() > 0) {
                    Logger.log(Logger.INFO, Logger.MSG_EVT_EFAEXITEXECCMD,
                            International.getMessage("Programmende veranlaßt; versuche, Kommando '{cmd}' auszuführen...", Daten.efaConfig.getValueEfaDirekt_execOnEfaAutoExit()));
                    try {
                        Runtime.getRuntime().exec(Daten.efaConfig.getValueEfaDirekt_execOnEfaAutoExit());
                    } catch (Exception ee) {
                        Logger.log(Logger.ERROR, Logger.MSG_ERR_EFAEXITEXECCMD_FAILED,
                                LogString.cantExecCommand(Daten.efaConfig.getValueEfaDirekt_execOnEfaAutoExit(), International.getString("Kommando")));
                    }
                }
                break;
            case EFA_EXIT_REASON_OOME:
                who = International.getString("Speicherüberwachung");
                break;
            case EFA_EXIT_REASON_AUTORESTART:
                who = International.getString("Automatischer Neustart");
                break;
            case EFA_EXIT_REASON_ONLINEUPDATE:
                who = International.getString("Online-Update");
                break;
            case EFA_EXIT_REASON_SYSTEM:
            	who = "System Signal";
            	break;
        }
        if (restart) {
            exitCode = Daten.program.restart();
        }

//        if (e != null) {
//            super.processWindowEvent(e);
//        }
        Logger.log(Logger.INFO, Logger.MSG_EVT_EFAEXIT,
                International.getMessage("Programmende durch {originator}", who));
        super.cancel();
        Daten.haltProgram(exitCode);
        return true;
    }

    public void cancelRunInThreadWithDelay(int reason, AdminRecord admin, boolean restart) {
        final int _reason = reason;
        final AdminRecord _admin = admin;
        final boolean _restart = restart;
        new Thread() {
            public void run() {
                this.setName("EfaBoathouseFrame.cancelRunInThreadWithDelay");
            	try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                cancel(null, _reason, _admin, _restart);
            }
        }.start();
    }

    /**
     * Returns all possible actions for a list.
     * Those actions are prefixed with the following numbers representing those actions, which may be processed by processListAction(String, int):
     * 1 - start session
     * 2 - finish session
     * 3 - late entry
     * 4 - change session
     * 5 - cancel session
     * 6 - boat reservations
     * @param listnr
     * @param r
     * @return
     */
    private String[] getListActions(int listnr, DataRecord r) {

    	String currentLogbookName = null;
    	String recordLogbookName = null;
    	BoatStatusRecord rb = null;
    	currentLogbookName = (Daten.project != null ? Daten.project.getCurrentLogbook().getName() : null);
    	
    	// if a BoatStatusRecord is provided, check it's status and choose the appropriate "list"
    	// as Boats on the water may  also be present in 'not available' list.
    	
        if (r != null && r instanceof BoatStatusRecord) {
            // this boat may have been sorted into a "wrong" list... fix its status first
            String s = ((BoatStatusRecord)r).getCurrentStatus();
            if (s != null && s.equals(BoatStatusRecord.STATUS_AVAILABLE)) {
                listnr = 1;
            }
            if (s != null && s.equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
                listnr = 2;
            }
            if (s != null && s.equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
                listnr = 3;
            }
    		rb = (BoatStatusRecord) r;
            recordLogbookName = rb.getLogbook();
        }
      
        String startSession = EfaUtil.replace(Daten.efaConfig.getValueEfaDirekt_butFahrtBeginnen().getValueText(), ">>>", "").trim();
        String finishSession = EfaUtil.replace(Daten.efaConfig.getValueEfaDirekt_butFahrtBeenden().getValueText(), "<<<", "").trim();
        String boatReserve = (Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenReservieren() ?
                International.getString("Boot reservieren") :
                International.getString("Bootsreservierungen"));
        if (listnr == 1 || listnr == 101) { // verfügbare Boote bzw. Personen
            if (listnr == 1) {
                ArrayList<String> listItems = new ArrayList<String>();
                listItems.add(ACTIONID_STARTSESSION + startSession);
                listItems.add(ACTIONID_LATEENTRY + International.getString("Nachtrag"));
                if (Daten.efaConfig.getValueEfaDirekt_butBootsreservierungen().getValueShow()) {
                    listItems.add(ACTIONID_BOATRESERVATIONS + boatReserve);
                }
                if (Daten.efaConfig.getValueEfaDirekt_showBootsschadenButton()) {
                    listItems.add(ACTIONID_BOATDAMAGES + International.getString("Bootsschaden melden"));
                }
                listItems.add(ACTIONID_BOATINFOS + International.getString("Bootsinfos"));
                listItems.add(ACTIONID_LASTBOATUSAGE + International.getString("Letzte Benutzung"));
                return listItems.toArray(new String[0]);
            } else {
                return new String[]{
                        ACTIONID_STARTSESSION + startSession,
                        ACTIONID_LATEENTRY + International.getString("Nachtrag")
                };
            }
        }
        if (listnr == 2) { // Boote auf Fahrt
            ArrayList<String> listItems = new ArrayList<String>();
            listItems.add(ACTIONID_LATEENTRY + International.getString("Nachtrag"));
            if (r==null || (rb!=null && currentLogbookName!=null && recordLogbookName!=null && currentLogbookName.equalsIgnoreCase(recordLogbookName))) {
            	// the boat on the water must be in our own logbook to do something with the session
            	listItems.add(ACTIONID_FINISHSESSION + finishSession);
	            listItems.add(ACTIONID_STARTSESSIONCORRECT + International.getString("Eintrag ändern"));
	            listItems.add(ACTIONID_ABORTSESSION + International.getString("Fahrt abbrechen"));
            }
            if (Daten.efaConfig.getValueEfaDirekt_butBootsreservierungen().getValueShow()) {
                listItems.add(ACTIONID_BOATRESERVATIONS + boatReserve);
            }
            if (Daten.efaConfig.getValueEfaDirekt_showBootsschadenButton()) {
                listItems.add(ACTIONID_BOATDAMAGES + International.getString("Bootsschaden melden"));
            }
            return listItems.toArray(new String[0]);
        }
        if (listnr == 3) { // nicht verfügbare Boote
        	ArrayList<String> listItems = new ArrayList<String>();
        	
        	if (r==null || (rb != null && rb.getCurrentStatus() != BoatStatusRecord.STATUS_ONTHEWATER)) {
        		listItems.add(ACTIONID_STARTSESSION + startSession);
        	}
        	
        	if (r==null || (currentLogbookName!=null && recordLogbookName!=null && currentLogbookName.equalsIgnoreCase(recordLogbookName))) {
        		if (Daten.efaConfig.getValueEfaDirekt_wafaRegattaBooteAufFahrtNichtVerfuegbar()) {
	                listItems.add(ACTIONID_FINISHSESSION + finishSession);
	                listItems.add(ACTIONID_ABORTSESSION + International.getString("Fahrt abbrechen"));
	            }
        	}
            listItems.add(ACTIONID_LATEENTRY + International.getString("Nachtrag"));
            if (Daten.efaConfig.getValueEfaDirekt_butBootsreservierungen().getValueShow()) {
                listItems.add(ACTIONID_BOATRESERVATIONS + boatReserve);
            }
            if (Daten.efaConfig.getValueEfaDirekt_showBootsschadenButton()) {
                listItems.add(ACTIONID_BOATDAMAGES + International.getString("Bootsschaden melden"));
            }
            listItems.add(ACTIONID_BOATINFOS + International.getString("Bootsinfos"));
            listItems.add(ACTIONID_LASTBOATUSAGE + International.getString("Letzte Benutzung"));
            return listItems.toArray(new String[0]);
        }
        return null;
    }

    // ========================================================================================================================================
    // Data-related methods
    // ========================================================================================================================================

    public static void haltProgram(String s, int exitCode) {
        if (s != null) {
            Dialog.error(s);
            Logger.log(Logger.ERROR, Logger.MSG_ERR_GENERIC,
                    EfaUtil.replace(s, "\n", " ", true));
        }
        Daten.haltProgram(exitCode);
    }

    public void updateProjectLogbookInfo() {
        if (Daten.project == null || !Daten.project.isOpen()) {
            titleLabel.setText(Daten.EFA_LONGNAME);
        } else {
            TxRequestQueue txq = TxRequestQueue.getInstance();
            if (txq != null)
                txq.setEfaGUIrootContainer(this);   // is relevant only at startup
            String efaCloudStatus = (txq != null) ? txq.getStateForDisplay(false) : "";
            titleLabel.setText(Daten.EFA_LONGNAME + " [" + Daten.project.getProjectName() +
                    (logbook != null && logbook.isOpen() ? ": " + logbook.getName() : "") + 
                    (Daten.project.getMyBoathouseName() != null ? " - " + Daten.project.getMyBoathouseName() : "") +
                    "] " + efaCloudStatus);
            
            titleLabel.setIcon((txq != null) ? txq.getStateIconForDisplay(): null);
            
            efaCloudStatus = (txq != null) ? txq.getStateForDisplay(true) : "";
            this.setTitle(Daten.EFA_LONGNAME + " [" + Daten.project.getProjectName() +
                    (logbook != null && logbook.isOpen() ? ": " + logbook.getName() : "") + 
                    (Daten.project.getMyBoathouseName() != null ? " - " + Daten.project.getMyBoathouseName() : "") +
                    "]" + efaCloudStatus);
        }
    }

    public Project openProject(String projectName) {
        try {
            if (projectName == null || projectName.length() == 0) {
                return null;
            }

            // close open project now
            if (Daten.project != null) {
                try {
                    Daten.project.closeAllStorageObjects();
                } catch (Exception e) {
                    String msg = LogString.fileCloseFailed(Daten.project.getProjectName(), International.getString("Projekt"), e.getMessage());
                    Logger.log(Logger.ERROR, Logger.MSG_DATA_CLOSEFAILED, msg);
                    Dialog.error(msg);
                }
            }
            Daten.project = null;
            logbook = null;

            if (!Project.openProject(projectName, true)) {
                Daten.project = null;
                return null;
            }

            if (Daten.project != null && Daten.project.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE
                    && !Daten.project.getProjectStorageUsername().equals(Admins.SUPERADMIN)) {
                Daten.project = null;
                String err = International.getString("Nur der Super-Administrator darf im Bootshaus-Modus ein Remote-Projekt öffnen.");
                Logger.log(Logger.ERROR, Logger.MSG_ERR_NOPROJECTOPENED, err);
                Dialog.error(err);
                return null;
            }

            Daten.efaConfig.setValueLastProjectEfaBoathouse(projectName);
            boatStatus = Daten.project.getBoatStatus(false);
            Logger.log(Logger.INFO, Logger.MSG_EVT_PROJECTOPENED, LogString.fileOpened(projectName, International.getString("Projekt")));

            if (efaBoathouseBackgroundTask != null) {
                efaBoathouseBackgroundTask.interrupt();
            }
            return Daten.project;
        } finally {
        	SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
                    updateProjectLogbookInfo();
        		}
        	});             	
        }
    }
    
    public Project openProject(AdminRecord admin) {
        // project to open
        String projectName = null;
        if (admin == null && Daten.efaConfig.getValueLastProjectEfaBoathouse().length() > 0) {
            projectName = Daten.efaConfig.getValueLastProjectEfaBoathouse();
        }
        
        Hashtable<String,String> availableProjects = null;
        availableProjects = Project.getProjects();

        // No projects open, but the last open project name points to a former one.
        // Do not try to open a former project if the list of projects is empty.
        if (projectName == null || projectName.length() == 0 || availableProjects.isEmpty()) {
            if (admin != null && admin.isAllowedAdministerProjectLogbook()) {
                OpenProjectOrLogbookDialog dlg = new OpenProjectOrLogbookDialog(this, OpenProjectOrLogbookDialog.Type.project, admin);
                projectName = dlg.openDialog();
            }
        }

        return openProject(projectName);
    }

    public Logbook openLogbook(AdminRecord admin) {
        try {
            if (Daten.project == null) {
                return null;
            }

            // close any other logbook first
            String logbookNameBefore = (logbook != null ? logbook.getName() : null);
            if (logbook != null) {
                try {
                    logbook.close();
                } catch (Exception e) {
                    String msg = LogString.fileCloseFailed(Daten.project.getProjectName(), International.getString("Fahrtenbuch"), e.toString());
                    Logger.log(Logger.ERROR, Logger.MSG_DATA_CLOSEFAILED, msg);
                    Logger.logdebug(e);
                    Dialog.error(msg);
                    logbook = null;
                }
            }

            // logbook to open
            String logbookName = null;
            if (admin == null && Daten.project.getCurrentLogbookEfaBoathouse() != null) {
                logbookName = Daten.project.getCurrentLogbookEfaBoathouse();
            }

            if (logbookName == null || logbookName.length() == 0) {
                if (admin != null && admin.isAllowedAdministerProjectLogbook()) {
                    OpenProjectOrLogbookDialog dlg = new OpenProjectOrLogbookDialog(this, OpenProjectOrLogbookDialog.Type.logbook, admin);
                    logbookName = dlg.openDialog();
                }
            }
            if ( (logbookName == null || logbookName.length() == 0) &&
                    logbookNameBefore != null && logbookNameBefore.length() > 0 && admin != null) {
                // Admin-Mode: There was a logbook opened before, but admin aborted dialog and didn't
                // open a new one. So let's open the old one again!
                logbookName = logbookNameBefore;
            }
            if (logbookName == null || logbookName.length() == 0) {
                return null;
            }
            if (!openLogbook(logbookName)) {
                logbook = null;
                return null;
            }
            return logbook;
        } finally {
        	SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
                    updateProjectLogbookInfo();
        		}
        	});             	
        }
    }

    public Clubwork openClubwork(AdminRecord admin) {
        try {
            if (Daten.project == null) {
                return null;
            }

            // close any other clubworkBook first
            String clubworkNameBefore = (clubwork != null ? clubwork.getName() : null);
            if (clubwork != null) {
                try {
                    clubwork.close();
                } catch (Exception e) {
                    String msg = LogString.fileCloseFailed(Daten.project.getProjectName(), International.getString("Vereinsarbeit"), e.toString());
                    Logger.log(Logger.ERROR, Logger.MSG_DATA_CLOSEFAILED, msg);
                    Logger.logdebug(e);
                    Dialog.error(msg);
                    clubwork = null;
                }
            }

            // clubwork to open
            String clubworkName = null;
            if (admin == null && Daten.project.getCurrentClubworkEfaBoathouse() != null) {
                clubworkName = Daten.project.getCurrentClubworkEfaBoathouse();
            }
            
            // check whether clubwork exists in project (may have been deleted)
            if (clubworkName != null) {
                Hashtable<String, String> allClubwork = Daten.project.getClubworks();
                if (allClubwork == null || allClubwork.get(clubworkName) == null) {
                    // clubwork was deleted
                    Daten.project.setCurrentClubworkEfaBoathouse(null);
                    clubworkName = null;
                }
            }

            if (clubworkName == null || clubworkName.length() == 0) {
                if (admin != null && admin.isAllowedAdministerProjectClubwork()) {
                    OpenProjectOrLogbookDialog dlg = new OpenProjectOrLogbookDialog(this, OpenProjectOrLogbookDialog.Type.clubwork, admin);
                    clubworkName = dlg.openDialog();
                }
            }
            if ((clubworkName == null || clubworkName.length() == 0)
                    && clubworkNameBefore != null && clubworkNameBefore.length() > 0 && admin != null) {
                // Admin-Mode: There was a clubwork opened before, but admin aborted dialog and didn't
                // open a new one. So let's open the old one again!
                clubworkName = clubworkNameBefore;
            }
            if (clubworkName == null || clubworkName.length() == 0) {
                return null;
            }
            if (!openClubwork(clubworkName)) {
                clubwork = null;
                return null;
            }
            return clubwork;
        } finally {
            // current clubwork not shown yet updateProjectLogbookInfo();
        }
    }

    public boolean openLogbook(String logbookName) {
        try {
            if (Daten.project == null) {
                return false;
            }
            try {
                if (logbook != null && logbook.isOpen()) {
                    logbook.close();
                }
            } catch (Exception e) {
                Logger.log(e);
                Dialog.error(e.toString());
            }
            if (logbookName != null && logbookName.length() > 0) {
                logbook = Daten.project.getLogbook(logbookName, false);
                if (logbook != null) {
                    Daten.project.setCurrentLogbookEfaBoathouse(logbookName);
                    Logger.log(Logger.INFO, Logger.MSG_EVT_LOGBOOKOPENED, LogString.fileOpened(logbookName, International.getString("Fahrtenbuch")));
                    if (efaBoathouseBackgroundTask != null) {
                        efaBoathouseBackgroundTask.interrupt();
                    }
                    return true;
                } else {
                    Dialog.error(LogString.fileOpenFailed(logbookName, International.getString("Fahrtenbuch")));
                }
            }
            return false;
        } finally {
        	//openLogBook can also be called from efaBoatHouseBackgroundTask, so it is secure to call this swing-thread-safe.
        	SwingUtilities.invokeLater(new Runnable() {
        		public void run() {
                    updateProjectLogbookInfo();
        		}
        	});        	
        }
    }

    public boolean openClubwork(String clubworkName) {
        try {
            if (Daten.project == null) {
                return false;
            }
            try {
                if (clubwork != null && clubwork.isOpen()) {
                    clubwork.close();
                }
            } catch (Exception e) {
                Logger.log(e);
                Dialog.error(e.toString());
            }
            if (clubworkName != null && clubworkName.length() > 0) {
                clubwork = Daten.project.getClubwork(clubworkName, false);
                if (clubwork != null) {
                    clubwork.setEfaBoathouseFrame(this);
                    Daten.project.setCurrentClubworkEfaBoathouse(clubworkName);
                    Logger.log(Logger.INFO, Logger.MSG_EVT_CLUBWORKOPENED, LogString.fileOpened(clubworkName, International.getString("Vereinsarbeit")));
                    if (efaBoathouseBackgroundTask != null) {
                        efaBoathouseBackgroundTask.interrupt();
                    }
                    return true;
                } else {
                    Dialog.error(LogString.fileOpenFailed(clubworkName, International.getString("Vereinarbeit")));
                }
            }
            return false;
        } finally {
            // clubwork not shown yet updateProjectLogbookInfo();
        }
    }

    public Logbook getLogbook() {
        return logbook;
    }

    public Clubwork getClubwork() {
        return clubwork;
    }

    /*
     * Updates the contents of efa boathouse lists (if listChanged is true) and sets the focus to one of the lists.
     * 
     * This is automatically triggered by the efaBoathouseBackgroundTask, which looks for the latest change 
     * in the data of reservations or boatstatus (boatstatus is changed when reservations get active, or a session is
     * created, edited, cancelled, finished)).
     * So usually there should be no need to call this method directly.
     * 
     * Technically, this list should be synchronized as it runs in swing's main thread, and is
     * invoked by efaBoathouseBackgroundTask. Instead, it has an inner handling to avoid being run concurrently.
     * 
     * @parameter onlyAvailableBoatsOrPersons true if only the available boats/persons list needs an update.
     *    this parameter is introduced due to performance issues on RASPIs when using two-column lists and
     *    switching between boats and persons.
     */
    // synchronizing this method can cause deadlock!!!!
    public void updateBoatLists(boolean listChanged, boolean onlyAvailableBoatsOrPersons) {
        if (!isEnabled()) {
            return;
        }
        if (inUpdateBoatList) {
            if (Logger.isTraceOn(Logger.TT_GUI, 8)) {
                Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "updateBoatLists(" + listChanged + ") - concurrent call, aborting");
            }
            return;
        }
        inUpdateBoatList = true;
        //Settings in EFA Config about two column Boat Lists shall take place immediately, if lists get updated. 
        this.boatsAvailableList.setShowTwoColumnList(Daten.efaConfig.getValueEfaBoathouseTwoColumnList());
        this.personsAvailableList.setShowTwoColumnList(Daten.efaConfig.getValueEfaBoathouseTwoColumnList());
        this.boatsOnTheWaterList.setShowTwoColumnList(Daten.efaConfig.getValueEfaBoathouseTwoColumnList());
        this.boatsNotAvailableList.setShowTwoColumnList(Daten.efaConfig.getValueEfaBoathouseTwoColumnList());
        
        try {
            if (Logger.isTraceOn(Logger.TT_GUI, 8)) {
                Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "updateBoatLists(" + listChanged + ")");
            }

            if (Daten.project == null || boatStatus == null) {
                boatsAvailableList.setItems(null);
                personsAvailableList.setItems(null);
                boatsOnTheWaterList.setItems(null);
                boatsNotAvailableList.setItems(null);
                if (Daten.project == null) {
                    boatsAvailableList.addItem("*** " + International.getString("Kein Projekt geöffnet.") + " ***", null, null, null, false, '\0');
                    personsAvailableList.addItem("*** " + International.getString("Kein Projekt geöffnet.") + " ***", null, null, null, false, '\0'); 
                }
                boatsAvailableList.showValue();
                personsAvailableList.showValue();
                boatsOnTheWaterList.showValue();
                boatsNotAvailableList.showValue();
            } else {
                if (boatsAvailableList.size() == 0) {
                    listChanged = true;
                }

                if (listChanged) {
                	
                	String strDebugTimes="";
                	long start = System.currentTimeMillis();

                	Vector <BoatReservationRecord> todaysReservations; 
                	//obtain reservation info only if they shall be shown in the boatLists
                	if (Daten.efaConfig.getValueEfaBoathouseBoatListReservationInfo()) {
                    	todaysReservations=getTodaysReservations();
                	} else {
                		 todaysReservations=new Vector <BoatReservationRecord>();
                	}
                	

                	strDebugTimes=strDebugTimes+(System.currentTimeMillis()-start);
                	
                    if (!Daten.efaConfig.getValueEfaDirekt_listAllowToggleBoatsPersons() || toggleAvailableBoatsToBoats.isSelected()) {
                        if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
                            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "updateBoatLists(" + listChanged + ") - setting boatsAvailableList ...");
                        }
                        start= System.currentTimeMillis();
                        boatsAvailableList.setBoatStatusData(boatStatus.getBoats(BoatStatusRecord.STATUS_AVAILABLE, true), logbook, "<" + International.getString("anderes Boot") + ">", todaysReservations);
                        strDebugTimes=strDebugTimes+";"+(System.currentTimeMillis()-start);
                        
                        if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
                            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "updateBoatLists(" + listChanged + ") - setting boatsAvailableList - done");
                        }
                    } else {
                        if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
                            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "updateBoatLists(" + listChanged + ") - setting personsAvailableList ...");
                        }
                        Persons persons = boatStatus.getProject().getPersons(false);
                        personsAvailableList.setPersonStatusData(persons.getAllPersons(System.currentTimeMillis(), false, false), "<" + International.getString("andere Person") + ">");
                        if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
                            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "updateBoatLists(" + listChanged + ") - setting personsAvailableList - done");
                        }
                    }

                    if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "updateBoatLists(" + listChanged + ") - setting boatsOnTheWaterList and boatsNotAvailableList ...");
                    }
                    if (!onlyAvailableBoatsOrPersons) {
	                    start= System.currentTimeMillis();
	                    boatsOnTheWaterList.setBoatStatusData(boatStatus.getBoats(BoatStatusRecord.STATUS_ONTHEWATER, true), logbook, null, todaysReservations);
	                    strDebugTimes=strDebugTimes+";"+(System.currentTimeMillis()-start);
	                    start=System.currentTimeMillis();
	                    boatsNotAvailableList.setBoatStatusData(boatStatus.getBoats(BoatStatusRecord.STATUS_NOTAVAILABLE, true), logbook, null, todaysReservations);
	                    strDebugTimes=strDebugTimes+";"+(System.currentTimeMillis()-start);
                    }
                    if (Logger.isTraceOn(Logger.TT_GUI, 9)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "updateBoatLists(" + listChanged + ") - setting boatsOnTheWaterList and boatsNotAvailableList - done");
                    }
                    Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "Aufrufzeiten: "+ strDebugTimes);
                }
            }

            /*
            Dimension dim = boatsAvailableScrollPane.getSize();
            boatsAvailableScrollPane.setPreferredSize(dim); // to make sure boatsAvailableScrollPane is not resized when toggled between persons and boats
            boatsAvailableScrollPane.setSize(dim);          // to make sure boatsAvailableScrollPane is not resized when toggled between persons and boats
             */

            if (toggleAvailableBoatsToBoats.isSelected()) {
                statusLabelSetText(International.getString("Kein Boot ausgewählt."));
            } else {
                statusLabelSetText(International.getString("Keine Person ausgewählt."));
            }
            boatsAvailableList.clearSelection();
            personsAvailableList.clearSelection();
            boatsOnTheWaterList.clearSelection();
            boatsNotAvailableList.clearSelection();
            if (boatsAvailableList.isFocusOwner()) {
                boatsAvailableList.setSelectedIndex(0);
            } else if (personsAvailableList.isFocusOwner()) {
                personsAvailableList.setSelectedIndex(0);
            } else if (boatsOnTheWaterList.isFocusOwner()) {
                boatsOnTheWaterList.setSelectedIndex(0);
            } else if (boatsNotAvailableList.isFocusOwner()) {
                boatsNotAvailableList.setSelectedIndex(0);
            } else {
            	if (toggleAvailableBoatsToBoats.isSelected()) {
            		//Requesting a focus brings the containing JFrame to front on Linux systems.
            		//On Windows systems, the task bar entry of efaBoatHouse gets a highlight.
            		//We only want to request focus for a list on the main window 
            		//if efaBoatHouse is already focused itself.           		
                	if (this.isFocused()) {
                		boatsAvailableList.requestFocus();
                	}
                    boatsAvailableList.setSelectedIndex(0);
                } else {
                	if (this.isFocused()) {
                		personsAvailableList.requestFocus();
                	}
                    personsAvailableList.setSelectedIndex(0);
                }
            }
            this.revalidate(); // refresh the whole screen
            this.repaint();
            if (Logger.isTraceOn(Logger.TT_GUI, 8)) {
                Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "updateBoatLists(" + listChanged + ") - done");
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        } finally {
            inUpdateBoatList = false;
        }
    }

    /**
     * Returns a vector that contains all reservations which take place within the current day.
     * The list is sorted by boat and valid_in_minutes, so that the next upcoming reservations are the first items in the list.
     * This usually is a much smaller list than the total list of reservations in the db.
     * 
     * If efaConfig is set that no reservation info shall be shown in the boatlists, this methods returns an empty vector.
     * 
     * So the assumption is:
     *   Performance is crucial as we are in swing's main thread (so every other operation is halted until we finished)
     *   and it is very crucial on low-power devices like the Raspberry Pi 3.
     *    
     * - two lists in efaBoatHouseFrame can be configured to show reservation data (of the next reservation of the boat today)
     * - if so, obtaining the reservation info for each boat can be a matter of performance, if looked up within the whole reservation DB.
     *   AND if there are a lot of boats within efa.
     * - using this list as a cache, there is less of a performance bottleneck.

     * 
     */
    private Vector <BoatReservationRecord> getTodaysReservations() {
    	Vector <BoatReservationRecord> result = new Vector <BoatReservationRecord>();
        
        if (Daten.efaConfig.getValueEfaBoathouseBoatListReservationInfo()) {
	    	Long now = System.currentTimeMillis();
	    	Long remainingMinutesToday = EfaUtil.getRemainingMinutesToday();
	      
	        BoatReservations boatReservationDB = (Daten.project != null ? Daten.project.getBoatReservations(false) : null);
	        //get reservations valid within 8 hours        
	      	
	        try {
	        	DataKeyIterator iter = boatReservationDB.data().getStaticIterator();
	            DataKey k = iter.getFirst();
	            while (k != null) {
	                BoatReservationRecord r = (BoatReservationRecord) boatReservationDB.data().get(k);
	                if (r != null) {
	                    boolean show = (!r.getInvisible()) &&
	                                   (!r.getDeleted());
	                    if (show) {
	                        if (r.getReservationValidInMinutes(now, remainingMinutesToday) >= 0) { 
	                        	//store the reservation 
	                        	result.add(r);
	                        }
	                    }
	                }
	                k = iter.getNext();
	            }
	        	
	        } catch (Exception e) {
	        	Logger.log(e);
	        }
	        
	        //sort by time span until reservation gets active, ascending
	        result.sort(new BoatReservationComparatorByNextOccurrence());
        }
        return result;
    	
    }

    // ========================================================================================================================================
    // Callbacks and Events
    // ========================================================================================================================================
    public void setUnreadMessages(boolean admin, boolean boatmaintenance) {
        String iconName = ImagesAndIcons.IMAGE_ACTION_ADMIN;
        if (admin && boatmaintenance) {
            iconName = ImagesAndIcons.IMAGE_ACTION_ADMIN_MAIL_ADMIN_BOAT;
        } else if (admin) {
            iconName = ImagesAndIcons.IMAGE_ACTION_ADMIN_MAIL_ADMIN;
        } else if (boatmaintenance) {
            iconName = ImagesAndIcons.IMAGE_ACTION_ADMIN_MAIL_BOAT;
        }
        adminButton.setIcon(getIcon(iconName));
    }

    public synchronized void exitOnLowMemory(String detector, boolean immediate) {
        largeChunkOfMemory = null;
        Logger.log(Logger.ERROR, Logger.MSG_ERR_EXITLOWMEMORY,
                International.getMessage("Der Arbeitsspeicher wird knapp [{detector}]: "
                                + "efa versucht {jetzt} einen Neustart ...", detector,
                        (immediate ? International.getString("jetzt").toUpperCase() : International.getString("jetzt"))));
        if (immediate) {
            this.cancel(null, EFA_EXIT_REASON_OOME, null, true);
        } else {
            EfaExitFrame.exitEfa(International.getString("Neustart wegen knappen Arbeitsspeichers"), true, EFA_EXIT_REASON_OOME);
        }
    }

    private int getListIdFromItem(IItemType item) {
        try {
            int listID = 0;
            if (item == boatsAvailableList) {
                listID = 1;
            }
            if (item == personsAvailableList) {
                listID = 1;
            }
            if (item == boatsOnTheWaterList) {
                listID = 2;
            }
            if (item == boatsNotAvailableList) {
                listID = 3;
            }
            return listID;
        } catch (Exception ex) {
            return 0;
        }
    }

    public void itemListenerAction(IItemType item, AWTEvent e) {
        int listID = 0;
        ItemTypeBoatstatusList list = null;
        ActionEvent ae = null;
        KeyEvent ke = null;
        try {
            listID = getListIdFromItem(item);
            list = (ItemTypeBoatstatusList)item;
        } catch (Exception eignore) {
        }
        try {
            ae = (ActionEvent)e;
        } catch (Exception eignore) {
        }
        try {
            ke = (KeyEvent)e;
        } catch (Exception eignore) {
        }

        if (listID != 0 && ae != null) {
            if (ae.getActionCommand().equals(EfaMouseListener.EVENT_MOUSECLICKED_1x)
                    || ae.getActionCommand().equals(EfaMouseListener.EVENT_MOUSECLICKED_2x)) {
                showBoatStatus(listID, (ItemTypeBoatstatusList) item, 1);

                if (ae.getActionCommand().equals(EfaMouseListener.EVENT_MOUSECLICKED_2x)) {
                    boatListDoubleClick(listID, list);
                }
            }
            if (ae.getActionCommand().equals(EfaMouseListener.EVENT_BUILD_POPUP_MENU)) {
                ItemTypeBoatstatusList.BoatListItem bli = getSelectedListItem((ItemTypeBoatstatusList) item);
                if (bli!=null) {
                	if (bli.boatStatus != null) {
                		//we do not need to provide the specific list, as the actions get determined by the boatstatus of the selected item.
                		((ItemTypeBoatstatusList)item).setPopupActions(getListActions(-1, bli.boatStatus));
                	} else if (bli.person!= null) {
                		//in the person list, there is no boat item. And the List type is set to 101
                		((ItemTypeBoatstatusList)item).setPopupActions(getListActions(101, null));
                	}
                }            	
            }
            if (ae.getActionCommand().equals(EfaMouseListener.EVENT_POPUP)) {
                showBoatStatus(listID, (ItemTypeBoatstatusList) item, 1);
            }
            // Popup clicked?
            if (ae.getActionCommand().startsWith(EfaMouseListener.EVENT_POPUP_CLICKED)) {
                int subCmd = EfaUtil.stringFindInt(ae.getActionCommand(), -1);
                if (subCmd >= 0) {
                    ItemTypeBoatstatusList.BoatListItem blitem = getSelectedListItem(list);
                    if (blitem != null) {
                        processListAction(blitem, subCmd);
                    }
                }
            }
        }

        if (listID != 0 && ke != null) {
            clearAllPopups();
            if (ke.getKeyCode() == KeyEvent.VK_ENTER || ke.getKeyCode() == KeyEvent.VK_SPACE) {
                // don't react if space was pressed as part of an incremental search string
                if (ke.getKeyCode() == KeyEvent.VK_SPACE) {
                    String s = ((ItemTypeList)list).getIncrementalSearchString();
                    if (s != null && s.length() > 0 && !s.startsWith(" ")) {
                        return;
                    }
                }
                boatListDoubleClick(listID, list);
                return;
            }
            showBoatStatus(listID, list, (ke != null && ke.getKeyCode() == 38 ? -1 : 1));
        }

        if (listID != 0 && e instanceof FocusEvent && e.getID() == FocusEvent.FOCUS_GAINED) {
            showBoatStatus(listID, list, 1);
        }
    }

    private void processListAction(ItemTypeBoatstatusList.BoatListItem blitem, int action) {
        switch (action) {
            case ACTIONID_STARTSESSION: // start session
                actionStartSession(blitem);
                break;
            case ACTIONID_FINISHSESSION: // finish session
                actionFinishSession();
                break;
            case ACTIONID_LATEENTRY: // late entry
                actionLateEntry();
                break;
            case ACTIONID_STARTSESSIONCORRECT: // change session
                actionStartSessionCorrect();
                break;
            case ACTIONID_ABORTSESSION: // cancel session
                actionAbortSession();
                break;
            case ACTIONID_BOATRESERVATIONS: // boat reservations
                actionBoatReservations();
                break;
            case ACTIONID_BOATDAMAGES: // boat damages
                actionBoatDamages();
                break;
            case ACTIONID_BOATINFOS: // boat infos
                actionBoatInfos();
                break;
            case ACTIONID_LASTBOATUSAGE: // boat damages
                actionLastBoatUsage();
                break;
        }
    }

    void boatListDoubleClick(int listnr, ItemTypeBoatstatusList list) {
    	if (list == null || list.getSelectedIndex() < 0) {
            return;
        }
        clearAllPopups();

        ItemTypeBoatstatusList.BoatListItem blitem = getSelectedListItem(list);
        DataRecord r = null;
        String name = null;
        if (blitem != null) {
            if (blitem.boatStatus != null) {
                r = blitem.boatStatus;
                name = blitem.boatStatus.getBoatNameAsString(System.currentTimeMillis());
            } else if (blitem.person != null) {
                r = blitem.person;
                name = blitem.person.getQualifiedName();
            }
        }
        if (r == null) {
            return;
        }

        if (listnr == 1
                && Daten.efaConfig.getValueEfaDirekt_listAllowToggleBoatsPersons()
                && toggleAvailableBoatsToPersons.isSelected()) {
            actionStartSession(blitem);
            return;
        }

        String[] actions = getListActions(listnr, r);
        if (actions == null || actions.length == 0) {
            return;
        }
        String[] myActions = new String[actions.length + 1];
        for (int i = 0; i < actions.length; i++) {
            myActions[i] = actions[i].substring(1);
        }
        myActions[myActions.length - 1] = International.getString("Nichts");
        int selection = Dialog.auswahlDialog(International.getString("Boot") + " " + name,
                International.getMessage("Was möchtest Du mit dem Boot {boat} machen?", name),
                myActions);
        if (selection >= 0 && selection < actions.length) {
            processListAction(blitem, EfaUtil.string2int(actions[selection].substring(0, 1), -1));
        }
    }

    void clearAllPopups() {
        boatsAvailableList.clearPopup();
        personsAvailableList.clearPopup();
        boatsOnTheWaterList.clearPopup();;
        boatsNotAvailableList.clearPopup();
    }

    //displays a text for the selected list element in the status label.
    void showBoatStatus(int listnr, ItemTypeBoatstatusList list, int direction) {
        if (Daten.project == null) {
            return;
        }
        try {
            String name = null;
            Boolean isSeparator = false;

            ItemTypeBoatstatusList.BoatListItem item = null;
            while (item == null) {
                try {
                    item = getSelectedListItem(list);
                    // it is possible that item is null when no item is selected in list.
                    if (item!=null) {
	                    if (list != personsAvailableList) {
	                        name = item.text;
	                    } else {
	                        name = item.person.getQualifiedName();
	                    }
	                    isSeparator=list.getSelectedItemIsSeparator();
                    } else {
                    	name=null;
                    }
                } catch (Exception e) {
                }
                if (name == null || isSeparator ) {
                	//name is not set. So we try to select a single item 
                	//in the list heading forward direction or backward direction
                	//from the current selected index, we want to find an item                	
                    item = null;
                    try {
                        int i = list.getSelectedIndex() + direction;
                        if (i < 0) {
                            i = 1; // i<0 kann nur erreicht werden, wenn vorher i=0 und direction=-1; dann darf nicht auf i=0 gesprungen werden, da wir dort herkommen, sondern auf i=1
                            direction = 1;
                        }
                        //Lists can be filtered. So, we have to check if listindex
                        //exceeds the resulting number of items when the filter is applied
                        if (i >= list.filteredSize()) {
                            return;
                        }
                        list.setSelectedIndex(i);
                    } catch (Exception e) { /* just to be sure */ }
                }
            }
            if (item == null) {
                return;
            }

            if (list != personsAvailableList) {
                if (item.boatStatus != null) {
                    BoatStatusRecord status = boatStatus.getBoatStatus(item.boatStatus.getBoatId());
                    BoatRecord boat = Daten.project.getBoats(false).getBoat(item.boatStatus.getBoatId(), System.currentTimeMillis());
                    name = (boat != null ? boat.getQualifiedName()
                            : (status != null ? status.getBoatText() : International.getString("anderes oder fremdes Boot")));
                    String text = "";
                    if (status != null) {
                    	@SuppressWarnings("static-access")
                    	String s = status.getStatusDescription(status.getCurrentStatus());
                        if (s != null) {
                            text = s;
                        }
                        s = status.getComment();
                        if (s != null && s.length() > 0) {
                            text = s; // if a comment is set, then *don't* display the current status, but only the comment
                        }
                    }
                    String bootstyp = "";
                    String rudererlaubnis = "";
                    if (listnr == 1) {
                        if (boat != null) {
                            bootstyp = " (" + boat.getDetailedBoatType(boat.getVariantIndex(item.boatVariant)) + ")";
                            String groups = boat.getAllowedGroupsAsNameString(System.currentTimeMillis());
                            if (groups.length() > 0) {
                                rudererlaubnis = (rudererlaubnis.length() > 0 ? rudererlaubnis + ", "
                                        : "; " + International.getMessage("nur für {something}", groups));
                            }
                        }
                    }
                    statusLabelSetText(name + ": " + text + bootstyp + rudererlaubnis);
                } else {
                    statusLabelSetText(International.getString("anderes oder fremdes Boot"));
                }
            } else {
                statusLabelSetText(name);
            }
            if (listnr != 1) {
                boatsAvailableList.clearSelection();
                personsAvailableList.clearSelection();
            }
            if (listnr != 2) {
                boatsOnTheWaterList.clearSelection();
            }
            if (listnr != 3) {
                boatsNotAvailableList.clearSelection();
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
    }

    void toggleAvailableBoats_actionPerformed(ActionEvent e) {
        if (!Daten.efaConfig.getValueEfaDirekt_listAllowToggleBoatsPersons()) {
            return;
        }
        alive(); // set last user interaction time
        iniGuiListNames();
        if (Logger.isTraceOn(Logger.TT_GUI, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "toggleAvailableBoats_actionPerformed()");
        }
        try {
              // die Preferredsize des Panels immer merken  
              Dimension size = boatsAvailablePanel.getPreferredSize();           

              // Es nun wird immer versucht, Boots- und PersonenPanel aus der GUI zu entfernen.
              // Dabei muss berücksichtigt werden, dass diese auch NULL sein können - 
              // denn sonst würde eine nullpointerexception auftreten, die zu unerwünschtem
              // Verhalten führt.

              if (personsAvailableList.getPanel() != null) {
                            boatsAvailablePanel.remove(personsAvailableList.getPanel());
              }
              if (boatsAvailableList.getPanel() != null) {
                            boatsAvailablePanel.remove(boatsAvailableList.getPanel());
              }

            if (toggleAvailableBoatsToBoats.isSelected()) {
                boatsAvailableList.displayOnGui(this, boatsAvailablePanel, BorderLayout.CENTER);
            } else {
              personsAvailableList.displayOnGui(this, boatsAvailablePanel, BorderLayout.CENTER);
            }
            boatsAvailablePanel.setPreferredSize(size);
            this.validate();
            this.repaint(); // ist erforderlich, damit auch mnemonics richtig geschrieben werden.
            
            updateBoatLists(true,true);
        } catch (Exception ee) {
        }
        if (Logger.isTraceOn(Logger.TT_GUI, 8)) {
            Logger.log(Logger.DEBUG, Logger.MSG_GUI_DEBUGGUI, "toggleAvailableBoats_actionPerformed() - done");
        }
    }


    private ItemTypeBoatstatusList.BoatListItem getSelectedListItem(ItemTypeBoatstatusList list) {
        ItemTypeBoatstatusList.BoatListItem item = (list != null ?
                list.getSelectedBoatListItem() : null);
        if (item != null && item.boatStatus != null) {
            // update saved boat status in GUI list with current boat status from Persistence
            item.boatStatus = boatStatus.getBoatStatus(item.boatStatus.getBoatId());
            if (item.boatStatus != null) {
                item.boat = item.boatStatus.getBoatRecord(System.currentTimeMillis());
            }
            if (item.boatStatus == null) {
                String s = International.getMessage("Boot {boat} nicht in der Statusliste gefunden!", item.text);
                Dialog.error(s);
                Logger.log(Logger.ERROR, Logger.MSG_ERR_BOATNOTFOUNDINSTATUS, s);
            }
        }
        return item;
    }

    private ItemTypeBoatstatusList.BoatListItem getSelectedListItem() {
        ItemTypeBoatstatusList.BoatListItem item = null;
        if (Daten.efaConfig.getValueEfaDirekt_listAllowToggleBoatsPersons() && toggleAvailableBoatsToPersons.isSelected()) {
            if (item == null && personsAvailableList != null) {
                item = getSelectedListItem(personsAvailableList);
            }
        } else {
            if (item == null && boatsAvailableList != null) {
                item = getSelectedListItem(boatsAvailableList);
            }
        }
        if (item == null && boatsOnTheWaterList != null) {
            item = getSelectedListItem(boatsOnTheWaterList);
        }
        if (item == null && boatsNotAvailableList != null) {
            item = getSelectedListItem(boatsNotAvailableList);
        }
        return item;
    }

    // mode bestimmt die Art der Checks
    // mode==1 - alle Checks durchführen
    // mode==2 - nur solche Checks durchführen, bei denen es egal ist, ob das Boot aus der Liste direkt ausgewählt wurde
    //           oder manuell über <anders Boot> eingegeben wurde. Der Aufruf von checkFahrtbeginnFuerBoot mit mode==2
    //           erfolgt aus EfaFrame.java.
    boolean checkStartSessionForBoat(ItemTypeBoatstatusList.BoatListItem item,
                                     String entryNo,
                                     int mode) {
        if (item == null || item.boatStatus == null || item.boatStatus.getCurrentStatus() == null) {
            return true;
        }
        if (item.boatStatus.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
            if (entryNo != null && item.boatStatus.getEntryNo() != null &&
                    !entryNo.equals(item.boatStatus.getEntryNo().toString())) {
                // Dieses Boot ist bereits auf Fahrt in einem anderen Fahrtenbucheintrag!
                Dialog.error(International.getMessage("Das Boot {boat} ist bereits unterwegs.", item.boatStatus.getBoatText()));
                return false;
            }

            if (mode == 1) {
                actionStartSessionCorrect();
                return false;
            }
        }
        if (mode == 3) {
            return true;
        }
        if (item.boatStatus.getCurrentStatus().equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
            if (Dialog.yesNoCancelDialog(International.getString("Boot gesperrt"),
                    International.getMessage("Das Boot {boat} ist laut Liste nicht verfügbar.", item.boatStatus.getBoatText()) + "\n"
                            + (item.boatStatus.getComment() != null ? International.getString("Bemerkung") + ": " + item.boatStatus.getComment() + "\n" : "")
                            + "\n"
                            + International.getString("Möchtest Du trotzdem das Boot benutzen?"))
                    != Dialog.YES) {
                return false;
            }
        }


        long now = System.currentTimeMillis();
        BoatReservations boatReservations = Daten.project.getBoatReservations(false);
        BoatReservationRecord[] reservations = (item.boatStatus.getBoatId() != null ?
                boatReservations.getBoatReservations(item.boatStatus.getBoatId(), now, Daten.efaConfig.getValueEfaDirekt_resLookAheadTime()) : null);
        if (reservations != null && reservations.length > 0) {
            long validInMinutes = reservations[0].getReservationValidInMinutes(now, Daten.efaConfig.getValueEfaDirekt_resLookAheadTime());
            if (Dialog.yesNoCancelDialog(International.getString("Boot reserviert"),
                    International.getMessage("Das Boot {boat} ist {currently_or_in_x_minutes} für {name} reserviert.",
                            item.boatStatus.getBoatText(),
                            (validInMinutes == 0
                                    ? International.getString("zur Zeit")
                                    : International.getMessage("in {x} Minuten", (int) validInMinutes)),
                            reservations[0].getPersonAsName()) + "\n"
                            + (reservations[0].getReason() != null && reservations[0].getReason().length() > 0 ?
                            International.getString("Grund") + ": " + reservations[0].getReason() + "\n" : "")
                            + (reservations[0].getContact() != null && reservations[0].getContact().length() > 0 ?
                            International.getString("Telefon für Rückfragen") + ": " + reservations[0].getContact() + "\n" : "")
                            + "\n" + International.getMessage("Die Reservierung liegt {from_time_to_time} vor.", reservations[0].getReservationTimeDescription()) + "\n"
                            + International.getString("Möchtest Du trotzdem das Boot benutzen?"))
                    != Dialog.YES) {
                return false;
            }
        }

        if (!checkBoatDamage(item, International.getString("Möchtest Du trotzdem das Boot benutzen?"))) {
            return false;
        }

        return true;
    }

    boolean checkBoatDamage(ItemTypeBoatstatusList.BoatListItem item, String questionText) {
        BoatDamages boatDamages = Daten.project.getBoatDamages(false);
        UUID boatId = null;
        if (item.boatStatus != null && item.boatStatus.getBoatId() != null) {
            boatId = item.boatStatus.getBoatId();
        }
        if (item.boat != null && item.boat.getId() != null) {
            boatId = item.boatStatus.getBoatId();
        }
        BoatDamageRecord[] damages = (boatId != null ?
                boatDamages.getBoatDamages(boatId, true, true) : null);
        if (damages != null && damages.length > 0 && BoatDamages.warnDamage(damages[0])) {
            if (Dialog.yesNoDialog(International.getString("Bootsschaden gemeldet"),
                    International.getMessage("Für das Boot {boat} wurde folgender Bootsschaden gemeldet:", item.boatStatus.getBoatText()) + "\n"
                            + "\""
                            + damages[0].getDescription()
                            + "\"\n"
                            + International.getString("Schwere des Schadens") + ": " + damages[0].getSeverityDescription()
                            + "\n\n"
                            + questionText)
                    != Dialog.YES) {
                return false;
            }
        }
        return true;
    }

    boolean checkBoatStatusOnTheWater(ItemTypeBoatstatusList.BoatListItem item) {
        if (item == null || item.boatStatus == null || item.boatStatus.getCurrentStatus() == null
                || !item.boatStatus.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
            item = null;
        }

        if (item == null) {
            Dialog.error(International.getMessage("Bitte wähle zuerst {from_the_right_list} ein Boot aus, welches unterwegs ist!",
                    (Daten.efaConfig.getValueEfaDirekt_wafaRegattaBooteAufFahrtNichtVerfuegbar()
                            ? International.getString("aus einer der rechten Listen")
                            : International.getString("aus der rechten oberen Liste"))));
            boatsAvailableList.requestFocus();
            efaBoathouseBackgroundTask.interrupt(); // Falls requestFocus nicht funktioniert hat, setzt der Thread ihn richtig!
            return false;
        }

        if (item.boatStatus.getEntryNo() == null || !item.boatStatus.getEntryNo().isSet()) {
            // keine LfdNr eingetragen: Das kann passieren, wenn der Admin den Status der Bootes manuell geändert hat!
            String s = International.getMessage("Es gibt keine offene Fahrt im Fahrtenbuch mit dem Boot {boat}.", item.boatStatus.getBoatText())
                    + " " + International.getString("Die Fahrt kann nicht beendet werden.");
            Logger.log(Logger.ERROR, Logger.MSG_ERR_NOLOGENTRYFORBOAT,
                    s + " " + International.getString("Bitte korrigiere den Status des Bootes im Admin-Modus."));
            Dialog.error(s);
            return false;
        }

        //EFA#134: check if session from the current logbook
        if (item.boatStatus.getLogbook()!=null && 
        		! item.boatStatus.getLogbook().equalsIgnoreCase(Daten.project.getCurrentLogbookEfaBoathouse())) {
        	String s = International.getMessage("Die gewählte Fahrt zum Boot '{boot}' stammt aus einem anderen Fahrtenbuch: Fahrt {session}.", 
        			item.boatStatus.getBoatText(), " #"+item.boatStatus.getEntryNo()+"/"+ item.boatStatus.getLogbook())+
        			" "+International.getString("Die Fahrt kann nicht verändert werden.");
            Logger.log(Logger.ERROR, Logger.MSG_ERR_NOLOGENTRYFORBOAT,
                    s + " " + International.getString("Bitte korrigiere den Status des Bootes im Admin-Modus."));
            Dialog.error(s);
            return false;
        }
        
        
        if (logbook.getLogbookRecord(item.boatStatus.getEntryNo()) == null) {
            String s = International.getMessage("Es gibt keine offene Fahrt im Fahrtenbuch mit dem Boot {boat} und LfdNr {lfdnr}.",
                    item.boatStatus.getBoatText(), (item.boatStatus.getEntryNo() != null ? item.boatStatus.getEntryNo().toString() : "null"))
                    + " " + International.getString("Die Fahrt kann nicht beendet werden.");
            Logger.log(Logger.ERROR, Logger.MSG_ERR_NOLOGENTRYFORBOAT,
                    s + " " + International.getString("Bitte korrigiere den Status des Bootes im Admin-Modus."));
            Dialog.error(s);
            return false;
        }

        return true;
    }

    void prepareEfaBaseFrame() {
        efaBaseFrame = new EfaBaseFrame(this, EfaBaseFrame.MODE_BOATHOUSE);
        efaBaseFrame.prepareDialog();
        efaBaseFrame.setFixedLocationAndSize();
    }

    void showEfaBaseFrame(int mode, ItemTypeBoatstatusList.BoatListItem action) {
        for (IWidget w : widgets) {
            w.runWidgetWarnings(mode, true, null);
        }
        if (efaBaseFrame == null) {
            prepareEfaBaseFrame();
        }
        action.mode = mode;
        if (!efaBaseFrame.setDataForBoathouseAction(action, logbook)) {
            return;
        }
        if (mode != EfaBaseFrame.MODE_BOATHOUSE_ABORT) {
            efaBaseFrame.efaBoathouseShowEfaFrame();
        } else {
            efaBaseFrame.finishBoathouseAction(true);
        }
    }

    // Callback from EfaBaseFrame
    /**
     * This method is called from 
     * - EfaBaseFrame (which is the main  GUI for starting, editing, finishing and lateentry of sessions)
     * - EfaBaseFrameMultiSession (which is the gui for creating/lateentry of multiple single-person sessions.
     * 
     * It takes care that some notifications get shown, and that a list of the EfaBoathouseFrame gets the focus.
     * Also, efaBoatHouseBackgroundTask gets a signal to keep BoatsAvailableList, Personlist, BoatsOntheWaterList and BoatsNotAvailableList up to date,
     * as mostly this method is called when some data has been added or changed.
     *  
     * @param efaBoathouseAction Item of the boathouse list that was selected when starting a boathouse action.
     * @param r LogbookRecord of the newly created record. May be null.
     */
    void showEfaBoathouseFrame(ItemTypeBoatstatusList.BoatListItem efaBoathouseAction, LogbookRecord r) {
        bringFrameToFront();
        updateBoatLists(true,false); // must be explicitly called here! only efaBoathouseBackgroundTask.interrupt() is NOT sufficient.
        efaBoathouseBackgroundTask.interrupt();
        if (focusItem != null) {
            focusItem.requestFocus();
        }
        alive();
        if (efaBoathouseAction != null &&
                (efaBoathouseAction.mode == EfaBaseFrame.MODE_BOATHOUSE_FINISH ||
                        efaBoathouseAction.mode == EfaBaseFrame.MODE_BOATHOUSE_ABORT)) {
            if (!boatStatus.areBoatsOutOnTheWater() &&
                    Daten.efaConfig.getValueEfaBoathouseShowLastFromWaterNotification() &&
                    Daten.efaConfig.getValueNotificationWindowTimeout() > 0) {
                String txt = Daten.efaConfig.getValueEfaBoathouseShowLastFromWaterNotificationText();
                if (txt == null || txt.length() == 0) {
                    txt = International.getString("Alle Boote sind zurück.") + "<br>" +
                            International.getString("Bitte schließe die Hallentore.");
                }
                NotificationDialog dlg = new NotificationDialog(this,
                        txt,
                        BaseDialog.BIGIMAGE_CLOSEDOORS,
                        "ffffff", "ff0000", Daten.efaConfig.getValueNotificationWindowTimeout());
                dlg.showDialog();
            }
        }
        if (efaBoathouseAction != null) {
            for (IWidget w : widgets) {
                w.runWidgetWarnings(efaBoathouseAction.mode, false, r);
            }
        }
    }

    void actionStartSession(ItemTypeBoatstatusList.BoatListItem item) {
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }

        if (item == null) {
            item = getSelectedListItem();
        }
        if (item == null) {
            Dialog.error(International.getString("Bitte wähle zuerst ein Boot aus!"));
            boatListRequestFocus(1);
            efaBoathouseBackgroundTask.interrupt(); // Falls requestFocus nicht funktioniert hat, setzt der Thread ihn richtig!
            return;
        }

        if (!checkStartSessionForBoat(item, null, 1)) {
            return;
        }

        showEfaBaseFrame(EfaBaseFrame.MODE_BOATHOUSE_START, item);
        clearFilterFieldsIfConfigured();        
    }

    /**
     * Starts the action behind StartSessionMultiple.
     */
    private void actionStartSessionMultiple() {
    	actionForMultiple(false);
    }    
    
    private void actionLateEntryMultiple() {
    	actionForMultiple(true);
    }
    
    /**
     * Checks if a boat item in the available Boat list is selected,
     * and shows the efaBaseFrameMultipleSession dialog if so.
     *  
     * @param lateEntry Boolean: Late Entry mode true or false?
     */
    private void actionForMultiple(boolean lateEntry) {
    	alive();
    	clearAllPopups();
        if (Daten.project == null) {
            return;
        }    	

        /*
         * EfaBaseFrame is used for creating, editing, finishing and late-entry of sessions.
         * It is multi-mode, and uses a BoatListItem to store the boat (or person), the mode of the dialog
         * and some more.
         * Due to this fact, efaBaseFrame cannot live without having a BoatListItem.
         * So the user has to select a BoatListItem on his own.
         * 
         * This is also true for efaBaseFrameMultisession as a subclass of efaBaseFrame.
         */
        ItemTypeBoatstatusList.BoatListItem item = getSelectedListItem();
        if (item == null) {
            Dialog.error(International.getString("Bitte wähle zuerst ein Boot aus!"));
            boatListRequestFocus(1);
            efaBoathouseBackgroundTask.interrupt(); // Falls requestFocus nicht funktioniert hat, setzt der Thread ihn richtig!
            return;
        }        

        showStartSessionMultipleDialog(lateEntry, item);
        clearFilterFieldsIfConfigured();   

    }
    
    void actionStartSessionCorrect() {
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }

        ItemTypeBoatstatusList.BoatListItem item = getSelectedListItem();
        if (!checkBoatStatusOnTheWater(item)) {
            return;
        }

        showEfaBaseFrame(EfaBaseFrame.MODE_BOATHOUSE_START_CORRECT, item);
        clearFilterFieldsIfConfigured();
    }


    void actionFinishSession() {
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }

        ItemTypeBoatstatusList.BoatListItem item = getSelectedListItem();
        if (!checkBoatStatusOnTheWater(item)) {
            return;
        }

        showEfaBaseFrame(EfaBaseFrame.MODE_BOATHOUSE_FINISH, item);
        clearFilterFieldsIfConfigured();
    }

    void actionAbortSession() {
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }

        ItemTypeBoatstatusList.BoatListItem item = getSelectedListItem();
        if (!checkBoatStatusOnTheWater(item)) {
            return;
        }

        BoatRecord boat = item.boat;

        switch (Dialog.auswahlDialog(International.getString("Fahrt abbrechen"),
                International.getMessage("Die Fahrt des Bootes {boat} sollte nur abgebrochen werden, "
                                + "wenn sie nie stattgefunden hat.",
                        item.boatStatus.getBoatText())
                        + "\n"
                        + International.getString("Was möchtest Du tun?"),
                International.getString("Fahrt abbrechen"),
                International.getString("Fahrt abbrechen") +
                        " (" + International.getString("Bootsschaden") + ")",
                International.getString("Nichts")
        )) {
            case 0:
                break;
            case 1:
                if (boat != null) {
                    BoatDamageEditDialog.newBoatDamage(this, boat);
                }
                break;
            case 2:
                return;
            default: // if the user hit the ESC key or closed the dialogue
                return;
        }

        showEfaBaseFrame(EfaBaseFrame.MODE_BOATHOUSE_ABORT, item);
        clearFilterFieldsIfConfigured();
    }

    void actionLateEntry() {
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }

        ItemTypeBoatstatusList.BoatListItem item = getSelectedListItem();
        if (item == null) {
            Dialog.error(International.getString("Bitte wähle zuerst ein Boot aus!"));
            boatListRequestFocus(1);
            efaBoathouseBackgroundTask.interrupt(); // Falls requestFocus nicht funktioniert hat, setzt der Thread ihn richtig!
            return;
        }

        showEfaBaseFrame(EfaBaseFrame.MODE_BOATHOUSE_LATEENTRY, item);
        clearFilterFieldsIfConfigured();        
    }

    void actionBoatReservations() {
        if (!Daten.efaConfig.getValueEfaDirekt_butBootsreservierungen().getValueShow()) {
            return;
        }
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }

        ItemTypeBoatstatusList.BoatListItem item = getSelectedListItem();
        if (item == null) {
            Dialog.error(International.getString("Bitte wähle zuerst ein Boot aus!"));
            boatListRequestFocus(1);
            efaBoathouseBackgroundTask.interrupt(); // Falls requestFocus nicht funktioniert hat, setzt der Thread ihn richtig!
            return;
        }
        if (item.boat != null && item.boat.getOwner() != null && item.boat.getOwner().length() > 0 &&
                !Daten.efaConfig.getValueMembersMayReservePrivateBoats()) {
            Dialog.error(International.getString("Privatboote dürfen nicht reserviert werden!"));
            return;
        }

        if (item.boat != null
                && !checkBoatDamage(item, International.getString("Möchtest Du trotzdem das Boot reservieren?"))) {
            return;
        }
        if (item.boat == null || item.boatStatus == null || item.boatStatus.getUnknownBoat() || item.boatStatus.getBoatId() == null) {
            BoatReservationListDialog dlg = new BoatReservationListDialog(this, null,
                    Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenReservieren(),
                    Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenReservierenZyklisch(),
                    Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenReservierungenEditieren());
            dlg.showDialog();
            efaBoathouseBackgroundTask.interrupt(); // Falls requestFocus nicht funktioniert hat, setzt der Thread ihn richtig!
            return;
        }

        BoatReservationListDialog dlg = new BoatReservationListDialog(this, item.boatStatus.getBoatId(),
                Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenReservieren(),
                Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenReservierenZyklisch(),
                Daten.efaConfig.getValueEfaDirekt_mitgliederDuerfenReservierungenEditieren());
        dlg.showDialog();
    	
        // we do not clear the filter fields by calling clearFilterFieldsIfConfigured()
        // because the assumption is that the user reserves more than one boat, if he has filtered for a boat.
        // (as in Efa 2.3.3_01, a user can filter by boat type if tooltips are active in the available boat list)
        
        boatListRequestFocus(0);//automatically detect the boatlist to get the focus    
        efaBoathouseBackgroundTask.interrupt();

    }

    void actionClubwork() {
        if (!Daten.efaConfig.getValueEfaDirekt_butVereinsarbeit().getValueShow()) {
            return;
        }
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }
        ClubworkListDialog dlg = new ClubworkListDialog(this, null);
        dlg.showDialog();
        clearFilterFieldsIfConfigured();
        
    }

    void actionBoatDamages() {
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }

        ItemTypeBoatstatusList.BoatListItem item = getSelectedListItem();
        if (item == null || item.boat == null) {
            Dialog.error(International.getString("Bitte wähle zuerst ein Boot aus!"));
            boatListRequestFocus(1);
            efaBoathouseBackgroundTask.interrupt(); // Falls requestFocus nicht funktioniert hat, setzt der Thread ihn richtig!
            return;
        }
        BoatDamageEditDialog.newBoatDamage(this, item.boat, null, null);
        efaBoathouseBackgroundTask.interrupt();
        clearFilterFieldsIfConfigured();
    }

    void actionShowLogbook() {
        if (!Daten.efaConfig.getValueEfaDirekt_butFahrtenbuchAnzeigen().getValueShow()) {
            return;
        }
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }
        ShowLogbookDialog dlg = new ShowLogbookDialog(this, logbook);
        dlg.showDialog();
        clearFilterFieldsIfConfigured();

    }

    void actionStatistics() {
        if (!Daten.efaConfig.getValueEfaDirekt_butStatistikErstellen().getValueShow()) {
            return;
        }
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }
        StatisticsListDialog dlg = new StatisticsListDialog(this, null);
        dlg.showDialog();
        clearFilterFieldsIfConfigured();           
    }

    void actionMessageToAdmin() {
        if (!Daten.efaConfig.getValueEfaDirekt_butNachrichtAnAdmin().getValueShow()) {
            return;
        }
        alive();
        clearAllPopups();
        if (Daten.project == null) {
            return;
        }

        MessageRecord msg = null;
        try {
            msg = Daten.project.getMessages(false).createMessageRecord();
            msg.setTo(Daten.efaConfig.getValueEfaDirekt_bnrMsgToAdminDefaultRecipient());
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        if (msg != null) {
            MessageEditDialog dlg = new MessageEditDialog(this, msg, true, null);
            dlg.showDialog();
            efaBoathouseBackgroundTask.interrupt();
        }
        clearFilterFieldsIfConfigured();
    }

    void actionAdminMode() {
        alive();
        clearAllPopups();

        // Prüfe, ob bereits ein Admin-Modus-Fenster offen ist
        Stack s = Dialog.frameStack;
        boolean adminOnStack = false;
        try {
            for (int i = 0; i < s.size(); i++) {
                if (s.elementAt(i).getClass().getName().equals(AdminDialog.class.getCanonicalName())) {
                    adminOnStack = true;
                }
            }
        } catch (Exception ee) {
        }
        if (adminOnStack) {
            Dialog.error(International.getString("Es ist bereits ein Admin-Fenster geöffnet."));
        	boatListRequestFocus(0);//automatically detect the boatlist to get the focus     
            return;
        }

        AdminRecord admin = AdminLoginDialog.login(this, International.getString("Admin-Modus"));
        if (admin == null) {
        	boatListRequestFocus(0);//automatically detect the boatlist to get the focus             	
            return;
        }
        Daten.checkRegister();
        AdminDialog dlg = new AdminDialog(this, admin);
        try {
            Daten.applMode = Daten.APPL_MODE_ADMIN;
            dlg.showDialog();
            efaBoathouseBackgroundTask.interrupt();
            if (Daten.project != null) {
                logbook = Daten.project.getCurrentLogbook();
                boatStatus = Daten.project.getBoatStatus(false);
            }
            updateBoatLists(true, false);
            updateGuiElements();
            iniGuiHeaderColors();
            iniGuiTooltipDelays();
            if (Daten.isApplEfaBoathouse() && Daten.isEfaFlatLafActive() && (closeButton!=null)) {
                //EfaFlatLaf gets renitialized when efaConfig Dialog gets closed. 
            	//this makes the closebutton in the blue header to get a border, and thus it grows.
            	//we revoke the border when closing admin mode, then.
                closeButton.setFont(closeButton.getFont().deriveFont(10f));
                closeButton.setBorder(null);
            }
        } finally {
            Daten.applMode = Daten.APPL_MODE_NORMAL;
            efaBoathouseBackgroundTask.interrupt(); //wake up efaBthsBackgroundTask
        }
        clearFilterFieldsIfConfigured();               
    }

    void actionSpecial() {
        if (!Daten.efaConfig.getValueEfaDirekt_butSpezial().getValueShow()) {
            return;
        }
        alive();
        clearAllPopups();
        String cmd = Daten.efaConfig.getValueEfaDirekt_butSpezialCmd().trim();
        if (cmd.length() > 0) {
            try {
                if (cmd.toLowerCase().startsWith("browser:")) {
                    BrowserDialog.openInternalBrowser(_parent, cmd.substring(8));
                } else if (cmd.toLowerCase().startsWith("toggle-logbook:")) {
                    toggleLogbook(cmd.substring(15).trim());
                } else if (cmd.toLowerCase().startsWith("toggle-project:")) {
                    toggleProject(cmd.substring(15).trim());
                } else if (cmd.toLowerCase().startsWith("toggle-boathouse:")) {
                    toggleBoathouse(cmd.substring(17).trim());
                } else {
                    Runtime.getRuntime().exec(cmd);
                }
            } catch (Exception ee) {
                LogString.logWarning_cantExecCommand(cmd, International.getString("Spezial-Button"), ee.toString());
            }
        } else {
            Dialog.error(International.getString("Kein Kommando für diesen Button konfiguriert!"));
        }
    }
    
    void toggleLogbook(String logbookString) {
        if (Daten.project == null || !Daten.project.isOpen() ||
            logbook == null || !logbook.isOpen()) {
            return;
        }
        String[] logbooks = logbookString.split(",");
        for (int i=0; logbooks != null && i<logbooks.length; i++) {
            if (logbooks[i] != null && logbooks[i].equals(logbook.getName())) {
                String newLog = logbooks[(i+1) % logbooks.length];
                if (newLog != null && Daten.project.getLogbooks().get(newLog) != null) {
                    openLogbook(newLog);
                    updateBoatLists(true,false);
                    updateGuiElements();
                }
                return;
            }
        }
    }

    void toggleProject(String projectString) {
        if (Daten.project == null || !Daten.project.isOpen()) {
            return;
        }
        String[] projects = projectString.split(",");
        for (int i=0; projects != null && i<projects.length; i++) {
            if (projects[i] != null && projects[i].equals(Daten.project.getName())) {
                String newProject = projects[(i+1) % projects.length];
                if (newProject != null) {
                    openProject(newProject);
                    openProjectLogbookClubwork();
                    updateBoatLists(true,false);
                    updateGuiElements();
                }
                return;
            }
        }
    }

    void toggleBoathouse(String boathouseString) {
        if (Daten.project == null || !Daten.project.isOpen()) {
            return;
        }
        String[] boathouses = boathouseString.split(",");
        for (int i=0; boathouses != null && i<boathouses.length; i++) {
            if (boathouses[i] != null && boathouses[i].equals(Daten.project.getMyBoathouseName())) {
                String newBoathouse = boathouses[(i+1) % boathouses.length];
                if (newBoathouse != null) {
                    Daten.project.setMyBoathouseName(newBoathouse);
                    openProjectLogbookClubwork();
                    updateBoatLists(true,false);
                    updateGuiElements();
                }
                return;
            }
        }
    }

    void actionBoatInfos() {
        alive();
        clearAllPopups();
        if (Daten.project == null || logbook == null) {
            return;
        }

        ItemTypeBoatstatusList.BoatListItem item = getSelectedListItem();
        if (item == null || item.boat == null) {
            Dialog.error(International.getString("Bitte wähle zuerst ein Boot aus!"));
            boatListRequestFocus(1);
            efaBoathouseBackgroundTask.interrupt(); // Falls requestFocus nicht funktioniert hat, setzt der Thread ihn richtig!
            return;
        }
        try {
            StringBuilder s = new StringBuilder();
            s.append(International.getString("Boot")  + ": " + item.boat.getQualifiedName() + "\n\n");
            for (int i=0; i<item.boat.getNumberOfVariants(); i++) {
                s.append(item.boat.getDetailedBoatType(i) + "\n");
            }
            if (item.boat.getMaxCrewWeight() > 0) {
                s.append("\n" + International.getString("Maximales Mannschaftsgewicht") + ": " +
                        item.boat.getMaxCrewWeight());
            }
            String groups = item.boat.getAllowedGroupsAsNameString(System.currentTimeMillis());
            if (groups.length() > 0) {
                s.append("\n\n" + International.getString("Gruppen, die dieses Boot benutzen dürfen") + ":\n" +
                        groups);
            }
            Dialog.infoDialog(s.toString());
        } catch(Exception e) {
            Logger.logdebug(e);
            Dialog.error(e.getMessage());
        }
        //No clearing of filter as user may want to proceed with an other action on the selected/filtered boat(s)
        boatListRequestFocus(0);
    }

    void actionLastBoatUsage() {
        alive();
        clearAllPopups();
        if (Daten.project == null || logbook == null) {
            return;
        }

        ItemTypeBoatstatusList.BoatListItem item = getSelectedListItem();
        if (item == null || item.boat == null || item.boat.getId() == null) {
            Dialog.error(International.getString("Bitte wähle zuerst ein Boot aus!"));
            boatListRequestFocus(1);
            efaBoathouseBackgroundTask.interrupt(); // Falls requestFocus nicht funktioniert hat, setzt der Thread ihn richtig!
            return;
        }
        LogbookRecord latest = logbook.getLastBoatUsage(item.boat.getId(), null);
        if (latest != null) {
            Dialog.infoDialog(International.getString("Letzte Benutzung") + ":\n"
                    + latest.getLogbookRecordAsStringDescription());
        } else {
            Dialog.infoDialog(International.getString("Keinen Eintrag gefunden!"));
        }
        
        //No clearing of filter as user may want to proceed with an other action on the selected/filtered boat(s)
    	boatListRequestFocus(0);//automatically detect the boatlist to get the focus    
        
    }

    void efaButton_actionPerformed(ActionEvent e) {
        alive();
        clearAllPopups();
        EfaAboutDialog dlg = new EfaAboutDialog(this);
        dlg.showDialog();
    }

    void hilfeButton_actionPerformed(ActionEvent e) {
    	alive();
        clearAllPopups();
        Help.showHelp(getHelpTopics());
    }

    public void lockEfa() {
        if (Daten.efaConfig == null) {
            return;
        }
        if (isLocked) {
            return; // don't lock twice
        }
        isLocked = true;
        Daten.efaConfig.setValueEfaDirekt_lockEfaFromDatum(new DataTypeDate()); // damit nach Entsperren nicht wiederholt gelockt wird
        Daten.efaConfig.setValueEfaDirekt_lockEfaFromZeit(new DataTypeTime());  // damit nach Entsperren nicht wiederholt gelockt wird
        Daten.efaConfig.setValueEfaDirekt_locked(true);

        try {
            final EfaBoathouseFrame frame = this;
            new Thread() {

                public void run() {
                	this.setName("EfaBoathouseFrame.lockEfaThread");
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                    String endeDerSperrung = (Daten.efaConfig.getValueEfaDirekt_lockEfaUntilDatum().isSet() ? " " + International.getString("Ende der Sperrung") + ": "
                            + Daten.efaConfig.getValueEfaDirekt_lockEfaUntilDatum().toString()
                            + (Daten.efaConfig.getValueEfaDirekt_lockEfaUntilZeit().isSet() ? " " + Daten.efaConfig.getValueEfaDirekt_lockEfaUntilZeit().toString() : "") : "");

                    String html = Daten.efaConfig.getValueEfaDirekt_lockEfaShowHtml();
                    if (html == null || !EfaUtil.canOpenFile(html)) {
                        html = Daten.efaTmpDirectory + "locked.html";
                        try {
                            String img = EfaUtil.saveImage("efaLocked.png", "png", Daten.efaTmpDirectory,
                                    true, false, false);
                            BufferedWriter f = new BufferedWriter(new FileWriter(html));
                            f.write("<html><body><h1 align=\"center\">" + International.getString("efa ist für die Benutzung gesperrt") + "</h1>\n");
                            f.write("<p align=\"center\"><img src=\"" + img + "\" align=\"center\" width=\"256\" height=\"256\"></p>\n");
                            f.write("<p align=\"center\">" + International.getString("efa wurde vom Administrator vorübergehend für die Benutzung gesperrt.") + "</p>\n");
                            if (endeDerSperrung.length() > 0) {
                                f.write("<p align=\"center\">" + endeDerSperrung + "</p>\n");
                            }
                            f.write("</body></html>\n");
                            f.close();
                        } catch (Exception e) {
                            EfaUtil.foo();
                        }
                    }
                    de.nmichael.efa.gui.BrowserDialog browser =
                            new BrowserDialog(frame, "file:" + html);
                    browser.setLocked(frame, true);
                    if (Daten.efaConfig.getValueEfaDirekt_lockEfaVollbild()) {
                        browser.setFullScreenMode(true);
                    } else {
                        browser.setSize(650, 420);
                        browser.setPreferredSize(new Dimension(650, 420));
                    }
                    browser.setModal(true);
                    Dialog.setDlgLocation(browser, frame);
                    browser.setClosingTimeout(10); // nur um Lock-Ende zu überwachen
                    Logger.log(Logger.INFO, Logger.MSG_EVT_LOCKED,
                            International.getString("efa wurde vom Administrator vorübergehend für die Benutzung gesperrt.") + endeDerSperrung);
                    browser.showDialog();
                }
            }.start();
        } catch (Exception ee) {
        }
    }

    public void setUnlocked() {
        isLocked = false;
    }
    
    private void showStartSessionMultipleDialog(Boolean isLateEntry, ItemTypeBoatstatusList.BoatListItem item) {

    	EfaBaseFrameMultisession myFrame = new EfaBaseFrameMultisession(this,
    			(isLateEntry ? EfaBaseFrame.MODE_BOATHOUSE_LATEENTRY_MULTISESSION : EfaBaseFrame.MODE_BOATHOUSE_START_MULTISESSION)
    			);

    	try {
            myFrame.efaBoathouseAction=item;
            myFrame.efaBoathouseAction.mode = (isLateEntry ? EfaBaseFrame.MODE_BOATHOUSE_LATEENTRY_MULTISESSION : EfaBaseFrame.MODE_BOATHOUSE_START_MULTISESSION);
            myFrame.showDialog();
    	} catch (Exception e) {
    		Logger.log(e);
    	}

        clearFilterFieldsIfConfigured();            	
        alive();
    }    

}
class BoatReservationComparatorByNextOccurrence implements Comparator<BoatReservationRecord> {
	public int compare(BoatReservationRecord brr1, BoatReservationRecord brr2) {
		return (int)(brr1.getReservationValidInMinutes(true) - brr2.getReservationValidInMinutes(true));
	}
}
