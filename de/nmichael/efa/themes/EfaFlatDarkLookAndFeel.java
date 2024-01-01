package de.nmichael.efa.themes;

/**
 * This is just a wrapper for the FlatIntelliJLaf,
 * originating from https://www.formdev.com/flatlaf/ 
 * 
 * The purpose of this class is to provide a more suitable name for the 
 * LookAndFeel in efaConfig, as it extracts it's name from the classname.
 * 
 * The customizing of this laf can be found in EfaFlatDarkLookAndFeel.properties.
 * (FlatLafs look for their customization depending on their name)
 * 
 * This class is still a stub, because the work on a dark efa2 look has not yet been finished.
 * Because of this, the efaFlatDarkLookAndFeel is not an user selectable option. 
 * 
 */
public class EfaFlatDarkLookAndFeel extends EfaFlatLookAndFeel {

	private static final long serialVersionUID = 7695849269840279570L;

	public static boolean setup() {
        return setup( new EfaFlatDarkLookAndFeel() );
    }

	public String getName() {
		return "EfaFlatDarkLookAndFeel";
	}
	
	public String getDescription() {
		return "Efa Flat Dark based on FlatIntelliJ LAF";
	}
	
	public boolean isDark() {
		return true;
	}
}
