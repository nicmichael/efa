package de.nmichael.efa.themes;

import com.formdev.flatlaf.FlatIntelliJLaf;


/**
 * Base class for EfaFlatLookAndFeel.
 * Extends to both efaFlatLightLookAndFeel as well as EfaFlatDarkLookAndFeel.
 * 
 * Some comments about efaFlatLightLaf and LookAndFeel issues:
 * - since efa 2.3.4 efaFlatLightLaf is established as the default LookAndFeel.
 * 
 * - Installation process of a fresh instance runs with Metal LookAndFeel,  
 *   but switches to efaFlatLaf when starting up first time after installation.
 *   This is done by efaFlatLaf being the return value of EfaConfig.getDefaultLookAndFeel(),
 *   which is determined during the first initialization of efaConfig.
 *   As efaConfig gets only active AFTER running the first installation routine,
 *   the installation routine starts up with metal, but efa then tries to use efaFlatLaf afterwards.
 *   
 * - Using the same method, the default font is set to a well-known UI-suitable font.
 *   So we don't use the LAF default font any more, but a Font hat depends on the current 
 *   operating system and its installed fonts. However, if no suitable font is found,
 *   we stick to "Dialog", which is handled by Java.
 * 
 * - After an update on an existing efa2 installation, the same procedure takes place.
 *   The efaConfig item name for the LookAndFeel setting got renamed, and thus the   
 *   old LAF configured by the user is ignored, and the new config item gets set up 
 *   with the system's default laf - efaFlatLaf.
 *   
 * - Flatlaf is only present as an import in classes in the package de.nmichael.efa.themes.
 *   This is because starting up efa2 with flatlaf-3.2.5 library missing it will fail with a classnotfounderror 
 *   just because of an import statement of that library.
 *   
 * - If flatlaf-3.2.5 library is missing (this is mostly because the classpath (cp) does not cover the library yet),
 *   efa2 will stick to METAL look and logs some error. 
 *
 */

public class EfaFlatLookAndFeel extends FlatIntelliJLaf {

	private static final long serialVersionUID = 1564679189853076046L;
	
}
