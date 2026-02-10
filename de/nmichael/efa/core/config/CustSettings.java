/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.config;


// @i18n complete

public class CustSettings {

    public boolean activateRowingOptions = true;
    public boolean activateGermanRowingOptions = true;
    public boolean activateBerlinRowingOptions = false;
    public boolean activateCanoeingOptions = false;
    public boolean activateGermanCanoeingOptions = false;

    public CustSettings() {
    }

    public CustSettings(EfaConfig efaConfig) {
        activateRowingOptions = efaConfig.getValueUseFunctionalityRowing();
        activateGermanRowingOptions = efaConfig.getValueUseFunctionalityRowingGermany();
        activateBerlinRowingOptions = efaConfig.getValueUseFunctionalityRowingBerlin();
        activateCanoeingOptions = efaConfig.getValueUseFunctionalityCanoeing();
        activateGermanCanoeingOptions = efaConfig.getValueUseFunctionalityCanoeingGermany();
    }

    public CustSettings(boolean activateRowingOptions,
                        boolean activateGermanRowingOptions,
                        boolean activateBerlinRowingOptions,
                        boolean activateCanoeingOptions,
                        boolean activateGermanCanoeingOptions) {
        this.activateRowingOptions = activateRowingOptions;
        this.activateGermanRowingOptions = activateGermanRowingOptions;
        this.activateBerlinRowingOptions = activateBerlinRowingOptions;
        this.activateCanoeingOptions = activateCanoeingOptions;
        this.activateGermanCanoeingOptions = activateGermanCanoeingOptions;
    }

}
