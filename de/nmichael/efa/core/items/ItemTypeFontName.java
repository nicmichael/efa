package de.nmichael.efa.core.items;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.FontUIResource;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.SimpleInputDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class ItemTypeFontName extends ItemTypeLabelValue implements IItemListener {

	private Color origButtonColor;
	private JButton butdel;

	private String fontName;
	private String defaultFontName;

	private String defaultColor;
	private Boolean canBeNull;
	
	ItemTypeStringList comboFontNameALL;
	ItemTypeStringList comboFontNameGUI; 
	ItemTypeBoolean chkShowOnlyStandardGUIFonts;

	public ItemTypeFontName(String name, String fontName, String defaultFontName, int type, String category,
			String description, Boolean canBeNull) {
		this.name = name;
		this.fontName = fontName;
		this.defaultFontName = defaultFontName;
		this.type = type;
		this.category = category;
		this.description = description;
		this.canBeNull = canBeNull;
	}

	public IItemType copyOf() {
		ItemTypeFontName copy = new ItemTypeFontName(name, fontName, defaultFontName, type, category, description, canBeNull);
		copy.setPadding(padXbefore, padXafter, padYbefore, padYafter);
		copy.setIcon((label == null ? null : label.getIcon()));
		return copy;
	}

	public void parseValue(String value) {
		this.fontName = value;
	}

	public String toString() {
		return fontName;
	}

	protected JComponent initializeField() {
		JButton f = new JButton();

		EfaUtil.handleButtonOpaqueForLookAndFeels(f);
		f.setFont(new FontUIResource(fontName, f.getFont().getStyle(), f.getFont().getSize()));

		return f;
	}

	protected void iniDisplay() {
		super.iniDisplay();
		JButton f = (JButton) field;

		f.setEnabled(isEnabled);

		Dialog.setPreferredSize(f, fieldWidth, fieldHeight);
		f.setText(fontName);
		f.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonHit(e);
			}
		});

		butdel = new JButton();
		butdel.setEnabled(isEnabled);
		Dialog.setPreferredSize(butdel, fieldHeight, fieldHeight);
		butdel.setIcon(BaseDialog.getIcon(BaseDialog.IMAGE_DELETE));
		butdel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				buttonDel(e);
			}
		});
	}

	public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
		int count = super.displayOnGui(dlg, panel, x, y);
		panel.add(butdel, new GridBagConstraints(x + fieldGridWidth + 1, y, 1, fieldGridHeight, 0.0, 0.0,
				fieldGridAnchor, fieldGridFill,
				new Insets((itemOnNewRow ? 0 : padYbefore), (itemOnNewRow ? padXbefore : 0), padYafter, padXafter), 0,
				0));
		return count;
	}

	public void getValueFromGui() {
		if (field != null) {
			JButton theButton = (JButton) field;
			fontName = theButton.getText();
		}
	}

	public String getFontName() {
		return fontName;
	}

	public String getValueFromField() {
        if (field != null) {
            return ((JButton)field).getText();
        }
        return fontName;
	}

	public void showValue() {
	}

	private void buttonHit(ActionEvent e) {

		String chosenFont=showFontDialog(fontName);
		// chosenFont!=null if user committed the dialog
		if (chosenFont!=null) {
			((JButton) field).setText(chosenFont);
			this.fontName=chosenFont;
		}

	}

	private void buttonDel(ActionEvent e) {
		if (canBeNull) {
			this.fontName = null;
		} else {
			this.fontName = Daten.efaConfig.FONT_NAME_LAF_DEFAULT_FONT;
		}
		if (this.field!=null) {
			((JButton) field).setText(this.fontName);
		}

	}

	public boolean isValidInput() {
		return true;
	}

	private String showFontDialog(String useFontName) {

		String[] allFonts= EfaUtil.makeFontFamilyArray(true, Daten.efaConfig.FONT_NAME_LAF_DEFAULT_FONT);
		String[] guiFonts = EfaUtil.makeFontFamilyArray(false, Daten.efaConfig.FONT_NAME_LAF_DEFAULT_FONT);
		
		ItemTypeLabel hint = new ItemTypeLabel("_GUIITEM_GENERIC_HINT", IItemType.TYPE_PUBLIC, null,  " " 
				+ (Daten.isOsLinux() ? International.getString("Probieren Sie die Schriftart Piboto oder Liberation Sans") : International.getString("Probieren Sie die Schriftart Arial oder Segoe UI"))); 
	
        hint.setPadding(0, 0, 12, 12);
    	hint.setFieldGrid(3,GridBagConstraints.EAST, GridBagConstraints.BOTH);
		hint.setBackgroundColor(Daten.efaConfig.hintBackgroundColor);
		hint.setHorizontalAlignment(SwingConstants.LEFT);
		hint.setRoundShape(false);    	
    	
    	// choose which fonts to show
    	
    	chkShowOnlyStandardGUIFonts = new ItemTypeBoolean(
				"_GUIITEM_ONLYGUIFONTS", false, IItemType.TYPE_PUBLIC,
				null,
				International.getString("Liste auf Bildschirmschriften reduzieren"));
    	
    	chkShowOnlyStandardGUIFonts.registerItemListener(this);
    	
    	//two comboboxes with fonts. But only one of them is shown. Switching by IItemListener 
    	comboFontNameALL = new ItemTypeStringList("_GUIITEM_FONTNAME_ALL",
    			useFontName,
    			allFonts, allFonts,
    			IItemType.TYPE_PUBLIC, null,
    			International.getString("Schriftart"));
    	comboFontNameALL.setCellRenderer(new FontListCellRenderer());	    	
    	comboFontNameALL.setVisible(true);

    	comboFontNameGUI = new ItemTypeStringList("_GUIITEM_FONTNAME_GUI",
    			useFontName,
    			guiFonts, guiFonts,
    			IItemType.TYPE_PUBLIC, null,
    			International.getString("Schriftart"));
    	comboFontNameGUI.setCellRenderer(new FontListCellRenderer());	    	
    	comboFontNameGUI.setVisible(false);
    	
    	

        IItemType[] dialogElements = new IItemType[4];        
        dialogElements[0]=hint;
        dialogElements[1]=chkShowOnlyStandardGUIFonts;
        dialogElements[2]=comboFontNameALL;
        dialogElements[3]=comboFontNameGUI;
        
        Boolean userCommittedChoice=false;
        if (this.getParentDialog()!=null) {
        	userCommittedChoice = SimpleInputDialog.showInputDialog(this.getParentDialog(), this.getDescription(), dialogElements);
        } else if (this.getParentFrame()!=null) {
        	userCommittedChoice = SimpleInputDialog.showInputDialog(this.getParentFrame(), this.getDescription(), dialogElements);
        }
	
		if (userCommittedChoice==true) {
			chkShowOnlyStandardGUIFonts.getValueFromGui();
			if (((String)chkShowOnlyStandardGUIFonts.getValueFromField()).equalsIgnoreCase("true")) {
				comboFontNameGUI.getValueFromGui();
				return comboFontNameGUI.getValueFromField();
			} else {
				comboFontNameALL.getValueFromGui();
				return comboFontNameALL.getValueFromField();
			}
		}
		return useFontName;
	
	}
        
	/* ---- helper functions for showFontDialog  ----- */
	
    public void itemListenerAction(IItemType item, AWTEvent event) {
        if (item != null && item.getName().equalsIgnoreCase("_GUIITEM_ONLYGUIFONTS")) {
            item.getValueFromGui();
        	String onlyGUI = item.getValueFromField();
            comboFontNameGUI.setVisible(onlyGUI.equalsIgnoreCase("true"));
            comboFontNameALL.setVisible(!onlyGUI.equalsIgnoreCase("true"));
        }
    }

    
    class FontListCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean chf) {
            super.getListCellRendererComponent(list, value, index, isSelected, chf);


            try {
            	String theFontName=(String)value;
            	this.setFont(new FontUIResource(theFontName, Font.PLAIN, 14));
            	
            } catch(Exception eignore) {
            	Logger.log(eignore);
            }
		            
            return this;
        }
    }
	
	
}
