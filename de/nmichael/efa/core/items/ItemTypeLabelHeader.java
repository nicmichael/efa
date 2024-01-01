package de.nmichael.efa.core.items;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.border.Border;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedLabel;

/*
 * This is an ItemTypeLabel whose background and foreground color is set to the BoathouseHeaderBackgroudColor and BoathouseHeaderForegroundColor.
 * The ItemTypeLabelHeader class is neccessary, as it is used for efaConfig dialog.
 * 
 * When it is used for BUILDING (not showing) the efaConfig dialogue, efaConfig has not been completely initialized, 
 * so efaConfig.getBoathouseHeaderBackgroundColor() returns null. This is some unwanted behaviour.
 * 
 * As we want the header to use the configured Background/Foreground colors in efaConfig,
 * we have to determine the colors when the field is actually shown on GUI. At that time,
 * efaconfig has been initialized fully and the properties can be get.
 * 
 */
public class ItemTypeLabelHeader extends ItemTypeLabel {

	public ItemTypeLabelHeader (String name, int type, String category, String description) {
		super(name,type,category,description);
		this.setRoundShape(true); // Headers always have round shape
    }	

    public IItemType copyOf() {
    	
        ItemTypeLabel thisCopy=new ItemTypeLabelHeader(name, type, category, description);
        thisCopy.setBackgroundColor(this.backgroundColor);
        thisCopy.setColor(this.color);
        thisCopy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
        thisCopy.setFieldGrid(fieldGridWidth, fieldGridHeight, fieldGridAnchor, fieldGridFill);
        thisCopy.setRoundShape(true);
        return thisCopy;

    }	
	
    public Color getBackgroundColor() {
    	return Daten.efaConfig.getHeaderBackgroundColor();
    }
    
    public Color getColor() {
    	return Daten.efaConfig.getHeaderForegroundColor();
    }
 
    protected boolean isBoldFont() {
    	return true;
    }
    
    protected Border getBorder() {
    	return new RoundedBorder(getColor());
    }
    
    protected JLabel createLabel() {
    	return new RoundedLabel();
    }
      
}
