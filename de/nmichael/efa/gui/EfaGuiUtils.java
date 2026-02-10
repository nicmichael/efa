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

import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeLabelHeader;
import de.nmichael.efa.gui.util.RoundedBorder;

/**
 * This class provides common code to create GUI Elements like Headers, Hints and Descriptions. 
 */
public class EfaGuiUtils {

	public static ItemTypeLabel createHint(String uniqueName, int type, String category, String caption, int gridWidth,
			int padBefore, int padAfter) {
		//if caption starts with html, do not have a blank as a prefix as this will disable html rendering.
		ItemTypeLabel item = (ItemTypeLabel) EfaGuiUtils.createDescription(uniqueName, type, category, (caption.startsWith("<html>") ? caption : " "+caption), gridWidth,
				padBefore, padAfter);
		item.setImage(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_INFO));
		item.setImagePosition(SwingConstants.TRAILING); // info icon should be first, the text trailing.
		item.setBackgroundColor(EfaConfig.hintBackgroundColor);
		item.setBorder(new RoundedBorder(EfaConfig.hintBorderColor));
		item.setHorizontalAlignment(SwingConstants.LEFT);
		item.setRoundShape(true);
		return item;
	}	
	
	// creates a multi-line hint, the captions provided as an string array. Each caption has a single line.
	public static ItemTypeLabel createHint(String uniqueName, int type, String category, String[] captions, int gridWidth,
			int padBefore, int padAfter) {
		
		String resultCaption="<html>";
		for(int i=0;i<captions.length; i++) {
			resultCaption+=captions[i];
			if (captions.length>1 && i<captions.length) {
				resultCaption+="<br>";
			}
		}
		resultCaption+="</html>";
		return createHint(uniqueName,type,category,resultCaption,gridWidth,padBefore,padAfter);
	}

	public static ItemTypeLabel createHintWordWrap(String uniqueName, int type, String category, String caption, int gridWidth,
			int padBefore, int padAfter, int maxPixelWidth) {
		
		JLabel x = new JLabel();
		FontMetrics myFontMetrics = x.getFontMetrics(x.getFont());

        List<String> captions = splitStringByWidth(caption, maxPixelWidth, myFontMetrics);
        String[] a = captions.toArray(new String[0]);
        return createHint(uniqueName, type, category, a, gridWidth, padBefore, padAfter);
	}
	
	
	// Splits a string word-wise into an array. wordwrap when the next word does not fit into maxWidth pixels.
    private static List<String> splitStringByWidth(String text, int maxWidth, FontMetrics fontMetrics) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            int lineWidth = fontMetrics.stringWidth(testLine);

            if (lineWidth > maxWidth) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine.append(currentLine.length() == 0 ? word : " " + word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

	/**
	 * Adds a description item in an efa GUI. This description value is not safed
	 * within efaConfig. There is no word-wrap for the caption.
	 * 
	 * This is similar to @see addHeader(), but the element does not get a
	 * highlighted background.
	 * 
	 * @param uniqueName Unique name of the element (as for all of efaConfig
	 *                   elements need unique names)
	 * @param type       TYPE_PUBLIC, TYPE_EXPERT, TYPE_INTERNAL
	 * @param category   Category in which the description is placed
	 * @param caption    Caption
	 * @param gridWidth  How many GridBagLayout cells shall this description be placed in?
	 * @param padBefore  Vertical space in pixels before this item
	 * @param padAfter	 Vertical space in pixals after this item
	 */
	public static ItemTypeLabel createDescription(String uniqueName, int type, String category, String caption, int gridWidth,
			int padBefore, int padAfter) {
		// ensure that the description value does not get saved in efaConfig file by
		// adding a special prefix
		ItemTypeLabel item = new ItemTypeLabel(EfaConfig.NOT_STORED_ITEM_PREFIX + uniqueName, type, category, caption);
		item.setPadding(0, 0, padBefore, padAfter);
		item.setFieldGrid(gridWidth, GridBagConstraints.EAST, GridBagConstraints.BOTH);
		return item;
	}	

    /**
     * Adds a header item in an efa widget config. This header value is not safed within efaConfig.
     * There is no word-wrap for the caption.
     * 
     * The header automatically gets a blue background and white text color; this cannot be configured
     * as efaConfig cannot refer to its own settings when calling the constructor.
     * 
     * @param uniqueName Unique name of the element (as for all of efaConfig elements need unique names)
     * @param type TYPE_PUBLIC, TYPE_EXPERT, TYPE_INTERNAL
     * @param category Category in which the header is placed
     * @param caption Caption
     * @param gridWidth How many GridBagLayout cells shall this header be placed in?
     */
    public static ItemTypeLabelHeader createHeader(String uniqueName, int type, String category, String caption, int gridWidth) {
    	ItemTypeLabelHeader item = new ItemTypeLabelHeader(EfaConfig.NOT_STORED_ITEM_PREFIX + uniqueName, type, category, " "+caption);
        item.setPadding(0, 0, 10, 10);
        item.setFieldGrid(3,GridBagConstraints.EAST, GridBagConstraints.BOTH);
        return item;
    }	

}