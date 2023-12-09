package de.nmichael.efa.themes;

import com.formdev.flatlaf.FlatIntelliJLaf;
/*
 * This is just a wrapper for the FlatIntelliJLaf,
 * originating from https://www.formdev.com/flatlaf/ 
 * 
 * The purpose of this class is to provide a more suitable name for the 
 * LookAndFeel in efaConfig, as it extracts it's name from the classname.
 * 
 * The customizing of this laf can be found in EfaFlatDarkLookAndFeel.properties.
 * (FlatLafs look for their customization depending on their name)
 * 
 */
public class EfaFlatDarkLookAndFeel extends EfaFlatLookAndFeel {
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
