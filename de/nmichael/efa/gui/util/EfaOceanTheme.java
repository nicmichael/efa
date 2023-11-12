package de.nmichael.efa.gui.util;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.OceanTheme;

public class EfaOceanTheme extends OceanTheme {
	private static ColorUIResource EFABG=new ColorUIResource(0xf5F4F3);
    protected ColorUIResource getSecondary3() {
        return EFABG;
    }
}
