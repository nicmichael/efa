package de.nmichael.efa.themes;

import java.util.HashMap;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.EfaUtil;


/**
 * This class encapsulates all methods which directly affect the FlatLaf libray,
 * so that the rest of efa code is free of it and does not have an import for com.formdev.flatlaf.
 *  
 */
public class EfaFlatLafHelper {
	
	/**
	 * Tells flatlaf library to use an extra customization file to init flatlaf look.
	 * flatlaf then looks for a $LOOKANDFEELNAME$.properties file to load additional settings.
	 */
	public static void installCustomDefaultSource() {
		FlatLaf.registerCustomDefaultsSource( "de.nmichael.efa.themes" );
	}
	
	
	/**
	 * Sets up some system properties which are taken into account when flatlaf library
	 * initializes for the first time.
	 * 
	 * Flatlaf contains and extracs some os-specific libraries (.so/.dll) files 
	 * and tries to invoke them to do some cool looking stuff. 
	 * But for maximum compatibility, we want flatlaf to use plain java functions.
	 * Also, the os-specific libraries would provide no "optical" use for efaBths and efaBase.
	 */
	public static void setupFlatLafSystemSettings() {
    	// We just want plain java functions, for maximum compatibility. 
    	// Otherwise flatlaf would try and install os-specific libraries which could fail, although they provide no use to efa.
    	System.setProperty( "flatlaf.useNativeLibrary", "false" );
    	System.setProperty( "flatlaf.animation", "false" );
    	System.setProperty( "flatlaf.useWindowDecorations" , "false" );
    	System.setProperty( "flatlaf.menuBarEmbedded", "false" );	
	}
	
	
	/**
	 * EfaFlatLightLookAndFeel.properties file sets up the look of flatlaf for efa.
	 * It is customizable by setting some color variables.
	 * 
	 * The user can customize all these color variables in efaConfig, and so we set these colors during startup
	 */
    public static void setupEfaFlatLafDefaults() {
        if (Daten.isEfaFlatLafActive()) {
        	
        	EfaFlatLookAndFeel myLaf = (EfaFlatLookAndFeel) UIManager.getLookAndFeel();
        	
        	HashMap<String, String> myCustomSettings = new HashMap<String, String>();
        	myCustomSettings.put("@background", "#"+EfaUtil.getColor(Daten.efaConfig.getEfaGuiflatLaf_Background()));
        	myCustomSettings.put("@accentBaseColor", "#"+EfaUtil.getColor(Daten.efaConfig.getEfaGuiflatLaf_AccentColor()));
        	myCustomSettings.put("@buttonBackground", "lighten(@background,"+Daten.efaConfig.getEfaGuiflatLaf_BackgroundFieldsLightenPercentage()+"%)");
        	myCustomSettings.put("@componentBackground", "lighten(@background,"+Daten.efaConfig.getEfaGuiflatLaf_BackgroundFieldsLightenPercentage()+"%)");
        	myCustomSettings.put("@menuBackground", "lighten(@background,"+Daten.efaConfig.getEfaGuiflatLaf_BackgroundFieldsLightenPercentage()+"%)");
        	myCustomSettings.put("@efaTableHeaderBackground", "#"+EfaUtil.getColor(Daten.efaConfig.getTableHeaderBackgroundColor()));
        	myCustomSettings.put("@efaTableHeaderForeground", "#"+EfaUtil.getColor(Daten.efaConfig.getTableHeaderHeaderColor()));
        	myCustomSettings.put("@efaFocusColor", "#"+EfaUtil.getColor(Daten.efaConfig.getEfaGuiflatLaf_FocusColor()));
        	if (Daten.efaConfig.getValueEfaDirekt_tabelleAlternierendeZeilenFarben()==false) {
        		// Flatlaf looks cleaner when it uses no lines for tables. 
        		// but then it needs alternating row colors to keep the rows apart.
        		// so if no alternating rowcolors are active, we at least show horizontalLines.
        		myCustomSettings.put("Table.showHorizontalLines", "true");
        	}
        		
        	//setting flatLaf efaTableAlternateRowColor will ENABLE alternate row color styling in flatlaf.
        	//efa itself has a special tableCellRenderer which supports alternate row coloring, and this cell renderer is NOT active
        	//for displaying the big logbook dialogue available in efaBths. 
        	//enabling this setting will lead to alternate row coloring also in the logbook dialogue, which does not look nice due to nested tables.
        	//so we could enable it, but we won't.
        	//myCustomSettings.put("@efaTableAlternateRowColor", "#"+EfaUtil.getColor(Daten.efaConfig.getTableAlternatingRowColor()));
        	
        	if (Daten.efaConfig.getToolTipSpecialColors()) {
        		myCustomSettings.put("@efaToolTipBorderColor", "#"+EfaUtil.getColor(Daten.efaConfig.getToolTipForegroundColor()));
        	}
        	// efaBths uses menu bars as title bar when "window not movable" is used.
        	// in efaBths this needs to be blue so everything looks fine.
        	// all other apps need standard settings
        	if (Daten.isApplEfaBoathouse()) {
        		myCustomSettings.put("MenuBar.background","#0000AA");
        		myCustomSettings.put("Menu.background","#0000AA");    
        		myCustomSettings.put("MenuItem.background","#0000AA");
        	}
        	
        	//inform Flatlaf about custom settings
        	myLaf.setExtraDefaults(myCustomSettings); 
        
        	
        	if (Daten.lookAndFeel.endsWith(Daten.LAF_EFAFLAT_LIGHT)) {
	        	EfaFlatLightLookAndFeel.setup(myLaf);
	        	EfaFlatLightLookAndFeel.updateUILater();
        	} else if (Daten.lookAndFeel.endsWith(Daten.LAF_EFAFLAT_DARK)) {
        		// Dark look of efaflatlaf is not yet ready, but the stub is available.
	        	EfaFlatDarkLookAndFeel.setup(myLaf);
	        	EfaFlatDarkLookAndFeel.updateUILater();
        	}
        	
        }
    	
    }	
    

}
