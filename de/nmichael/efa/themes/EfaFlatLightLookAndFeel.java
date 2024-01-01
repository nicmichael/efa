package de.nmichael.efa.themes;

/**
 * This is just a wrapper for the FlatIntelliJLaf,
 * originating from https://www.formdev.com/flatlaf/ 
 * 
 * The purpose of this class is to provide a more suitable name for the 
 * LookAndFeel in efaConfig, as it extracts it's name from the classname.
 * 
 * The customizing of this laf can be found in EfaFlatLightLookAndFeel.properties.
 * (FlatLafs look for their customization depending on their name.)
 * 
 */
public class EfaFlatLightLookAndFeel extends EfaFlatLookAndFeel {

	private static final long serialVersionUID = -5167779876123106160L;

		public static boolean setup() {
	        return setup( new EfaFlatLightLookAndFeel() );
	    }
    
		public String getName() {
			return "EfaFlatLightLookAndFeel";
		}
		
		public String getDescription() {
			return "Efa Flat Light based on FlatIntelliJ LAF";
		}
		
		public boolean isDark() {
			return false;
		}		
}
