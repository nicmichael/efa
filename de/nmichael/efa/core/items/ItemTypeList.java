/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.util.EfaMouseListener;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedLabel;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.Mnemonics;
import de.nmichael.efa.gui.ImagesAndIcons;

public class ItemTypeList extends ItemType implements ActionListener, DocumentListener, KeyListener {

    JPanel mypanel;
    JLabel label;
    JScrollPane scrollPane;
    JList list = new JList();
    JTextField filterTextField;
    EfaMouseListener popupListener; 
    JPopupMenu popup;
    Long lastFilterChange=0l;
    DefaultListModel<ItemTypeListData> data; // no longer Vector as we need a DefaultListModel for filtering
    String[] actions;
    String incrementalSearch = "";
    int iconWidth = 0;
    int iconHeight = 0;

    protected static final String LIST_SECTION_STRING = "----";
    //Spacings for pretty rendering
    private static final int SPACING_BOATNAME_SECONDPART  = 60; //60 pixels
	private static final int HORZ_SINGLE_BORDER=5;
	private static Border _emptyBorder = new EmptyBorder(2, HORZ_SINGLE_BORDER, 2, HORZ_SINGLE_BORDER);
	private Color _separatorBackground = new Color(240,240,240);
	private Color _filterTextfieldDefaultBackground = Color.WHITE;
    private boolean showFilterField = false;
  	private boolean showTwoColumnList=false;
    protected String other_item_text=""; //item text of the element for <other boat> or <other person>
    private ImageIcon defaultIconForCellRenderer;
    
    class ListDataCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean chf) {
            super.getListCellRendererComponent(list, value, index, isSelected, chf);

            if (iconWidth > 0 && iconHeight > 0) {
                try {
                	BuildIcon(value);
                } catch(Exception eignore) {
                	Logger.log(eignore);
                }
            }

            try {
            	this.setBorder(_emptyBorder);
            	
            	if (showTwoColumnList) {
            		/* Two Column Lists
            		 * Data is put together in ItemTypeBoatStatuslist.sortBootsList and sortMemberList
            		 * 
            		 * - Center separator texts, with grey background
					 * - left side: original data
					 * 
            		 * Available boats: (right side)
            		 * - show today's next reservation of a boat 
            		 * 
            		 * Boats on water: (right side)
            		 * - show destination of a boat 
            		 * 
            		 * Unavailable boats
            		 * - show "boat damage" if the boatcomment begins with this text on the l
            		 * - show destination of boat
            		 * - show reservation end if boat is reserved
            		 * 
            		 * Performance
            		 * - only create a HTML table when neccessary. Creation of HTML tables consume a lot of 
            		 *   time, due to string concatenation.
            		 */
            		
		            //at startup, the efa boathouse frame is not visible, but the renderer is invoked.
		            //then, the list width may be zero. Do nothing then.  
		            if (list.getParent().getWidth()>0) {
			            ItemTypeListData item = (ItemTypeListData)value;

		            	if (item.separator) {
			                if (!isSelected) { setBackground(_separatorBackground); }
		                    this.setHorizontalAlignment(JLabel.CENTER);
		                    this.setFont(this.getFont().deriveFont(Font.BOLD)); 
			            } else { // not a separator
			            	if (item.secondaryElement!=null) {
			            		//only build the p
			            		this.setText(getHTMLTableFor(item.text, item.secondaryElement, isSelected));
			            	} 
		            		setHorizontalAlignment(JLabel.LEFT);
		            
			            } //if not a separator
		            }//if parent width>0
	            }// if showTowColumnList
            	else {
		            ItemTypeListData item = (ItemTypeListData)value;            		
	            	if (item.separator) {
		                if (!isSelected) { setBackground(_separatorBackground); }
	                    //why don't we put this on center?
		                //because in "boats on the water list", the list items can get very long
		                //when they also contain a destination. Then the "center" mode may look bad
		                //for very long 
		                this.setHorizontalAlignment(JLabel.LEFT);
	            	} else {
	                    this.setHorizontalAlignment(JLabel.LEFT);
	            	}
            	}

	            
            } catch(Exception eignore) {
            	Logger.log(eignore);
            }
		            
        return this;
    }


        
    /* 
     * Set icon for a boat depending on the groups who can row with this boat
     */
    private void BuildIcon(Object value) {
    	
        ItemTypeListData item = (ItemTypeListData)value;
        ImageIcon icon = null;
        if (item.image != null) {
            icon = BaseDialog.getIcon(item.image);
        }
        if (icon == null) {
            if (item.colors != null && item.colors.length > 0) {
            	icon = EfaUtil.createColorPieIcon(item.colors, iconWidth, iconHeight);
            } else {
                if (!item.separator) {

                	icon=buildDefaultIcon(iconWidth, iconHeight);

                }
            }
        }
        
        if (icon != null && (icon.getIconWidth() > iconWidth || icon.getIconHeight() > iconHeight)) {
            icon = new ImageIcon(icon.getImage().getScaledInstance(iconWidth, iconHeight,
                    Image.SCALE_SMOOTH));
        }
    	if (list.getParent() != null && list.getParent().getWidth()>0) {
    		// this.setIcon causes nullpointer exceptions, when efaBoathouseFrame gets initialized,
    		// but is not yet visible. So we only set an icon if the parent of the list has a width>0. 
    		this.setIcon(icon);
        }
    }
    
    private synchronized ImageIcon buildDefaultIcon(int iconWidth, int iconHeight) {
    	if (defaultIconForCellRenderer==null) {
            BufferedImage image = new BufferedImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color (230,230,230));
    	    g.fillOval(0, 0, iconWidth, iconHeight);
            defaultIconForCellRenderer= new ImageIcon(image);    		
    	}
    	return defaultIconForCellRenderer;
    }

    /*
     * Creates a HTML table consisting of two rows.
     * - left row (usually boat name) gets all neccessary space
     * - right row gets remaining space and is truncated on the right side, if contents do not fit.
     *   also, right row contents are rendered in grey text color.
     */
    private String getHTMLTableFor(String firstPart, String secondPart, boolean isSelected) {
    	
		// we only build an HTML table, if there is a secondPart to be displayed.
	    // building html tables takes a lot of time on a raspberry pi 3b with a boat list
		// of around 200 Boats. So skipping html tables saves a lot of performance.
    	
    	if (secondPart== null) {
    		return firstPart;
    	} else if ((secondPart.trim().isEmpty())) {
    		return firstPart;
    	}

    	try {
    	//es gibt einen Secondpart, jetzt lohnt sich eine HTML-Tabelle
    	
			long listWidth=Math.max(80,list.getParent().getWidth()-2*HORZ_SINGLE_BORDER-2);
			FontMetrics myFontMetrics = label.getFontMetrics(label.getFont());
			
			long firstPartLength= myFontMetrics.stringWidth(firstPart);
			long maxStringWidth = listWidth-SPACING_BOATNAME_SECONDPART-(iconWidth)-firstPartLength-4;
	
			if (maxStringWidth>0) {
				// determine the maximum string length of the secondPart to be displayed...
		        int stringWidth = 0;
		        char[] readText = secondPart.toCharArray();
		        int stringLength= secondPart.length();
		    	boolean cutText = false;
		        
		        for (int currentChar=0; currentChar< stringLength; currentChar++) {
		        	stringWidth += myFontMetrics.charWidth(readText[currentChar]);
		        	if (stringWidth > maxStringWidth) {
		        		cutText=true;
		        		if (currentChar>0) {
		        			secondPart=secondPart.substring(0,currentChar)+("\u2026");
		        		} else {
		        			secondPart="\u2026";
		        		}
		        		break;
		        	}
		        }
			} else {
				secondPart="";
				}	        
	       /* old code
	        //listWidth can be zero directly after start of efaBoatHouse, so we stop under this condition.
	        while (listWidth>0 &&(stringWidth>maxStringWidth) && (secondPart.length()>0)) {
	        	// Performance: Strings which are very much longer than maxStringWitdh must be reduced faster than just
	        	//one character per iteration.
	        	int cutChars=(int)Math.abs((int)(maxStringWidth-stringWidth)/characterWidth); 
	        	cutChars=(int)Math.min(secondPart.length(), cutChars); // limit cut items to length of second part
	        	
	        	secondPart=secondPart.substring(0,secondPart.length()-(int)Math.max(((cutChars)),1)).trim();
	        	stringWidth = myFontMetrics.stringWidth(secondPart);
	        	cutText=true;
	        }
	        
	*/
	        Integer tableWidth =new Integer((int)listWidth-Math.max(iconWidth,0)-6);
	       
	        // font color only shall apply if the item is not selected.
	        String fontColorTag="color=#888888>";
	        if (isSelected) {fontColorTag=">";}
	        
	        return  "<html><table border=0 cellpadding=0 cellspacing=0 width='"
	        		.concat(tableWidth.toString())
	        		.concat("'><tr><td align=left>")
	        		.concat(EfaUtil.escapeHtml(firstPart))
	        		.concat("</td><td align=right><font ").concat(fontColorTag)
	        		.concat(EfaUtil.escapeHtml(secondPart))
	        		.concat("</font></td></tr></table></html>");
    	} catch (Exception e) {
    		Logger.logdebug(e);
    		return firstPart;
    	}
    }
}


    class ItemTypeListData {
        String text;
        String toolTipText;
        String toolTipFilterText;
        String secondaryElement;
        Object object;
        boolean separator;
        int section;
        String image;
        Color[] colors;
        public ItemTypeListData(String text, String toolTipText, String secondaryElement, Object object, boolean separator, int section) {
            ini(text, toolTipText, secondaryElement, object, separator, section, null, null);
        }
        public ItemTypeListData(String text, String toolTipText, String secondaryElement, Object object, boolean separator, int section, 
                String image, Color[] colors) {
            ini(text, toolTipText, secondaryElement, object, separator, section, image, colors);
        }
        private void ini(String text, String toolTipText, String secondaryElement, Object object, boolean separator, int section, 
                String image, Color[] colors) {
            this.text = text;
            this.toolTipText = toolTipText;
            if (toolTipText!=null) {
            	try {
            		//this removes all html tags from the tooltiptext. 
            		//although it is told it may be unsafe to remove tags by regular expressions, it works.
            		this.toolTipFilterText = toolTipText.replaceAll("<[^\\P{Graph}>]+(?: [^\\P{Graph}>]*)*>", "");
            	} catch (Exception e) {
            		Logger.log(e);
            		this.toolTipFilterText=null; // just in case the removal of html tags fails, set tooltippfiltertext to null.
            	}
            } 
            this.object = object;
            this.separator = separator;
            this.section = section;
            this.image = image;
            this.colors = colors;
            this.secondaryElement = secondaryElement;
        }
        public String toString() {
            return text;
        }
        public String getFilterableText() {
        	if (toolTipFilterText!=null) {
        		return text.concat(toolTipFilterText);
        	} else {
        		return text;
        	}

        }
    }

    public ItemTypeList(String name,
            int type, String category, String description, boolean showFilterField, boolean showTwoColumnList) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.description = description;
        this.showFilterField = showFilterField;
        this.showTwoColumnList= showTwoColumnList;
        data = new DefaultListModel<ItemTypeListData>();
        //overwrite standard separator color with efaFlatBackgroundColor if efaFlat is the current look
        if (Daten.isEfaFlatLafActive()) {
        	this._separatorBackground=Daten.efaConfig.getEfaGuiflatLaf_Background();
       }
    }
    
    public ItemTypeList(String name,
            int type, String category, String description) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.description = description;
        this.showFilterField = false;
        data = new DefaultListModel<ItemTypeListData>();
    }

    public IItemType copyOf() {
        return new ItemTypeList(name, type, category, description, this.showFilterField, this.showTwoColumnList);
    }

    public void addItem(String text, String toolTipText, String secondaryItem, Object object, boolean separator, char separatorHotkey) {
        data.addElement(new ItemTypeListData(text, toolTipText, secondaryItem, object, separator, separatorHotkey));
        filter();
    }

    public void addItem(String text, String toolTipText, String secondaryItem, Object object, boolean separator, char separatorHotkey,
            String image, Color[] colors) {
        data.addElement(new ItemTypeListData(text, toolTipText, secondaryItem, object, separator, separatorHotkey, image, colors));
        filter();
    }

    public void removeItem(int idx) {
        if (idx >= 0 && idx < data.size()) {
            data.remove(idx);
        }
        filter();
    }

    public void removeAllItems() {
        data = new DefaultListModel<ItemTypeListData>();
        filter();
    }

    public void setItems(Vector<ItemTypeListData> items) {
        if (data == null) {
            data = new DefaultListModel<ItemTypeListData>();
            filter();
        }
        if (items != null) {
        	data = new DefaultListModel<ItemTypeListData>();
            for (ItemTypeListData item:items) {
            	data.addElement(item);
            }
            filter();        	
        } else {
            data = new DefaultListModel<ItemTypeListData>();
            filter();
        }
    }

    /* returns the number of items in the list.
     * not all may be visible. If you want to get the size of the filtered list,
     * use @see filteredSize() instead.
     */
    public int size() {
        if (data == null) {
            return 0;
        }
        return data.size();
    }

    // returns the size of the list when the filter text field is applied.
    // if no filter is set, the list data size is returned.
    public int filteredSize() {
    	ListModel theList = null;
    	
        if (showFilterField==true) {
        	theList = list.getModel();
        } else {
        	theList = data;
        }
        
        if (theList==null) {
        	return 0;
        } else {
        	return theList.getSize();
        }
    }
    
    public String getItemText(int idx) {
        if (idx >= 0 && idx < data.size()) {
            return data.get(idx).text;
        }
        return null;
    }

    public Object getItemObject(int idx) {
        if (idx >= 0 && idx < data.size()) {
            return data.get(idx).object;
        }
        return null;
    }

    void clearIncrementalSearch() {
            incrementalSearch = "";
            if (list != null) {
            	if (Daten.efaConfig.getValueEfaBoathouseExtdToolTips()==false) {
            		list.setToolTipText(null);
            	}
            }
    }

    public String getIncrementalSearchString() {
        return incrementalSearch;
    }

    public void setPopupActions(String[] actions) {
        this.actions = actions;
        // if a popup menu already exists, remove all elements and rebuild the popup menu.
        if (popup!=null) {
	        popup.removeAll();
	        for (int i = 0; actions != null && i < actions.length; i++) {
	            JMenuItem menuItem = new JMenuItem(actions[i].substring(1));
	            menuItem.setActionCommand(EfaMouseListener.EVENT_POPUP_CLICKED + "_" + actions[i].substring(0, 1));
	            menuItem.addActionListener(this);
	            popup.add(menuItem);
	            menuItem.setIcon(getIconFromActionID(actions[i].substring(0, 1)));
	        }
	        if (popupListener!=null) {
	        	popupListener.setPopupMenu(popup);
	        }
        }
    }

    protected void iniDisplay() {
        // not used, everything done in displayOnGui(...)
    }

    public int displayOnGui(Window dlg, JPanel panel, int x, int y) {
        panel.add(setupPanel(dlg), new GridBagConstraints(x, y, fieldGridWidth, fieldGridHeight, 0.0, 0.0,
                fieldGridAnchor, fieldGridFill, new Insets(padYbefore, 0, padYafter, padXafter), 0, 0));
        showValue();
        return 1;
    }

    public int displayOnGui(Window dlg, JPanel panel, String borderLayoutOrientation) {
        panel.add(setupPanel(dlg), borderLayoutOrientation);
        showValue();
        return 1;
    }

    private JPanel setupPanel(Window dlg) {
        this.dlg = dlg;

        list = createNewListWithTooltippSupport();
        
        if (this.showFilterField) {
	        filterTextField =new JTextField();
	        filterTextField.getDocument().addDocumentListener(this);
	        filterTextField.addKeyListener(this);
	        filterTextField.putClientProperty("caretWidth", 3);
	        filterTextField.setMargin(new Insets(0,2,0,0));
	        updateLastFilterChange();
        }
        popup = new JPopupMenu();
        //Vertical scrollbar shall be shown always, because better ListCellRendering does not work optically well when
        //scrollbar is shown only when needed
        scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.field = scrollPane;
        
        mypanel = new JPanel();
        mypanel.setLayout(new BorderLayout());

        if (getDescription() != null) {
            if (Daten.efaConfig.getHeaderUseHighlightColor()) {
            	label = new RoundedLabel();            
                label.setBorder(new RoundedBorder(this.color));
            }else {
            	label = new JLabel();
            	label.setBorder(new EmptyBorder(4,0,4,0));//4 pixel space before and after the label
            }
            Mnemonics.setLabel(dlg, label, " " + getDescription() + ": ");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (type == IItemType.TYPE_EXPERT) {
                label.setForeground(Color.red);
            }
            if (color != null) {
                label.setForeground(color);
            }
            if (backgroundColor != null) {
            	label.setBackground(backgroundColor);
            	label.setOpaque(true);
            }
            label.setFont(label.getFont().deriveFont(Font.BOLD));

            if (this.showFilterField) {
            	label.setLabelFor(filterTextField);
            } else {
            	label.setLabelFor(list);
            }
            
            Dialog.setPreferredSize(label, fieldWidth, 20);
            
        }

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        for (int i = 0; actions != null && i < actions.length; i++) {
            JMenuItem menuItem = new JMenuItem(actions[i].substring(1));
            menuItem.setActionCommand(EfaMouseListener.EVENT_POPUP_CLICKED + "_" + actions[i].substring(0, 1));
            menuItem.addActionListener(this);
            popup.add(menuItem);
            menuItem.setIcon(getIconFromActionID(actions[i].substring(0, 1)));
        }
        // KeyListeners entfernen, damit unter Java 1.4.x nicht automatisch gescrollt wird, sondern durch den eigenen Algorithmus
        try {
            KeyListener[] kl = list.getKeyListeners();
            for (int i = 0; i < kl.length; i++) {
                list.removeKeyListener(kl[i]);
            }
        } catch (NoSuchMethodError e) { /* Java 1.3 kennt diese Methode nicht */ }
        list.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                list_keyReleased(e);
            }
        });
    	if (Daten.efaConfig.getValueEfaBoathouseExtdToolTips()==false) { 
			list.addMouseListener(new java.awt.event.MouseAdapter() {
	            public void mouseEntered(MouseEvent e) {
	            	try {
            			((JList) e.getSource()).setToolTipText(null); // remove tool tip from scrolling/searching
	            	} catch (Exception ee) {
	            		Logger.logdebug(ee);
	            	}}});
        }

        popupListener = new EfaMouseListener(list, popup, this, Daten.efaConfig.getValueEfaDirekt_autoPopupOnBoatLists());
        list.addMouseListener(popupListener);

        list.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(FocusEvent e) {
                list_focusGained(e);
            }
        });

        scrollPane.getViewport().add(list, null);
        
        //mypanel
        //--> panelDescriptionAndFilter (NORTH)
        //    --> label (for description) (NORTH)
        //    --> filterTextfield (SOUTH)
        //--> Scrollpane (CENTER)
        //    --> JList
        
        mypanel.setLayout(new BorderLayout());
        mypanel.setBorder(new EmptyBorder(0,4,0,4));// 4 pixel space on the left and the right side of the panel
        
        JPanel panelDescriptionAndFilter=new JPanel();
        panelDescriptionAndFilter.setLayout(new BorderLayout());
        panelDescriptionAndFilter.setBorder(new EmptyBorder(4,0,4,0));//4 pix space before and after
        if (getDescription() != null) {
            panelDescriptionAndFilter.add(label, BorderLayout.NORTH);
        }
        
        if (this.showFilterField) {
	        JPanel filterPanel=new JPanel();
	        filterPanel.setLayout(new BorderLayout());
	        filterPanel.setBorder(new EmptyBorder(4,0,4,0));//4 pix space before and after
	        JLabel myFilterLabel = new JLabel("Filter:");
	        myFilterLabel.setBorder(new EmptyBorder(0,0,0,4));//4 pix space on the right
	        filterPanel.add(myFilterLabel, BorderLayout.WEST);
	        filterPanel.add(filterTextField, BorderLayout.CENTER);
	        panelDescriptionAndFilter.add(filterPanel, BorderLayout.SOUTH);
	        this.field=filterTextField; // by this, when the boat status list receives focus, and the filter text field is visible, the filter text field gets the focus.
	        _filterTextfieldDefaultBackground = filterTextField.getBackground();
	        
	        filterTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusGained(FocusEvent e) {
	                filterTextField.setBackground(Color.YELLOW);
	            }
	        });
	        filterTextField.addFocusListener(new java.awt.event.FocusAdapter() {
	            public void focusLost(FocusEvent e) {
	            	if (!filterTextField.getText().isEmpty()) {
	            		filterTextField.setBackground(new Color(255,255,204)); //some white yellow so it is clear that some stuff is in this field.

	            	} else {	
	            		// use standard background color. for flatlaf, this is not white as it is for the other lafs.
	            		filterTextField.setBackground(_filterTextfieldDefaultBackground); 
	            	}
	            }
	        });
        }

        mypanel.add(panelDescriptionAndFilter, BorderLayout.NORTH);
        mypanel.add(scrollPane, BorderLayout.CENTER);

        return mypanel;
    }

    private Icon getIconFromActionID(String actionID) {
    	if (actionID.equals("1")) return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_ACTION_START_SESSION);
    	if (actionID.equals("2")) return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_ACTION_FINISH_SESSION);	
    	if (actionID.equals("3")) return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_ACTION_LATE_ENTRY);
    	if (actionID.equals("5")) return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_ACTION_ABORT_SESSION);
    	if (actionID.equals("6")) return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_ACTION_BOAT_RESERVATIONS);
    	
    	return null;
    }
    
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.itemListenerAction(this, e);
        }
    }

    public void clearPopup() {
        if (popup != null) {
            popup.setVisible(false);
        }
    }

    public void clearSelection() {
        try {
            //list.setSelectedIndices(new int[0]);
            list.clearSelection();
        } catch(Exception e) {
        }
    }

    public boolean isFocusOwner() {
        return list.isFocusOwner();
    }

    private void list_keyReleased(KeyEvent e) {
        clearPopup();
        if (e != null) {
        	if ((e.getKeyCode()==KeyEvent.VK_ESCAPE) || (e.getKeyCode()==KeyEvent.VK_F && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0))){
        		if (this.showFilterField) {
        			filterTextField.requestFocus();
        		}
        	} else {
        		scrollToEntry(String.valueOf(e.getKeyChar()), 15, (e != null && e.getKeyCode() == 38 ? -1 : 1));  // KeyCode 38 == Cursor Up
        	}
        }
        if (listener != null) {
            listener.itemListenerAction(this, e);
        }
    }

    private void list_focusGained(FocusEvent e) {
        if (list != null && list.getFirstVisibleIndex() >= 0 && list.getSelectedIndex() < 0) {
            list.setSelectedIndex(0);
        }
        //clearIncrementalSearch(); // it's better to comment this out, cannot hold 100% backwards compatibility with incremental search
        if (listener != null) {
            listener.itemListenerAction(this, e);
        }
    }
    
    /* clear filtertextfield, if it has been unchanged more than two minutes */
    public void clearFilterTextByInterval() {
    	
    	// get the reset filter interval in minutes from efaConfig.
    	long filterResetInterval= (Daten.efaConfig.getValueEfaBoathouseFilterTextAutoClearInterval()*60*1000); 
    	
    	
    	if (filterResetInterval>0) {
    		//reduce filterResetInterval by 5 seconds.
    		//this is because clearFilterText gets called regularily by efaBoatHouseBackgroundTask, in the interval of every minute.
    		//so if we have exactly an minute interval, and the boathousetask also calls this every minute,
    		//chances are high that we miss the first call. So... we handle this by reducing the configured interval by 5 seconds.
    		filterResetInterval = filterResetInterval-(5*1000);
	    	if ((System.currentTimeMillis()>(lastFilterChange+filterResetInterval))) {
	    		clearFilterText();
	    	}
    	}
    }

    /* clear filter text field intentionally, if it is displayed */
    public void clearFilterText() {

    	if (this.showFilterField && (this.filterTextField != null)) {
			this.filterTextField.setText("");
        	updateLastFilterChange();
    		if (!this.filterTextField.hasFocus()) {
    			this.filterTextField.setBackground(_filterTextfieldDefaultBackground);
    		}
    		filter();    
    	}
    }
    
    // scrolle in der Liste list (deren Inhalt der Vector entries ist), zu dem Eintrag
    // mit dem Namen such und selektiere ihn. Zeige unterhalb des Boote bis zu plus weitere Einträge.
    private void scrollToEntry(String search, int plus, int direction) {
    	@SuppressWarnings("unchecked")
		DefaultListModel <ItemTypeListData> theData= (DefaultListModel)list.getModel();
    	
        if (list == null || search == null || search.length() == 0) {
            return;
        }
        try {
            int start = 0;

            int index = -1;

            if (search.charAt(0) >= '0' && search.charAt(0) <= '9') {
                int isearch = search.charAt(0) - '0';
                // search for a section with the corresponding number
                for (int i=0; i<theData.size(); i++) {
                    if (theData.get(i).section == isearch) {
                        index = i;
                        break;
                    }
                }
                incrementalSearch = "";
            } else {
                // search for names within the list
                start = Math.max(list.getSelectedIndex(), 0);

                if (incrementalSearch == null || incrementalSearch.length() == 0) {
                    // if we haven't searched for anything before, jump to the start of this section
                    while (start > 0 && !(theData.get(start).separator)) {
                        start--;
                    }
                }
                // build new search string depending of previous search
                char c = search.charAt(0);
                if (Character.isLetter(c) || Character.isSpaceChar(c)
                        || c == '.' || c == '-' || c == '_' || c == ':' || c == ',' || c == ';') {
                    search = (incrementalSearch != null ? incrementalSearch : "") + search;
                } else {
                    if (c == 0x8) {
                        search = (incrementalSearch != null && incrementalSearch.length() > 0
                                ? incrementalSearch.substring(0, incrementalSearch.length() - 1)
                                : "");
                    } else {
                        return; // no valid search character
                    }
                }
                incrementalSearch = search;
                search = search.toLowerCase();

                boolean startsWith = search.length() == 1; // for single-character search, match strings starting with this search string; otherwise, match somewhere
                for (int run = 0; run < 2; run++) { // 2 search runs: 1st - start from "start"; 2nd - if no result, restart from 0
                    for (int i = start; i < theData.size(); i++) {
                        String item = ((String) theData.get(i).text).toLowerCase();
                        if (startsWith && item.startsWith(search) || !startsWith && item.contains(search)) {
                            index = i;
                            break;
                        }
                    }
                    if (index != -1) {
                        break;
                    } else {
                        start = 0;
                    }
                }
            }

            // check whether we should really select this item
            while (index >= 0 && index < theData.size() && theData.get(index).separator) {
                index += direction;
            }

            // Item found?
            if (index >= 0 && index < theData.size()) {
                list.setSelectedIndex(index);
                Rectangle rect = list.getCellBounds(index, (index + plus >= theData.size() ? theData.size() - 1 : index + plus));
                list.scrollRectToVisible(rect);
            }

            if (Daten.efaConfig.getValueEfaBoathouseExtdToolTips()==false) {
	            Rectangle rect = list.getVisibleRect();
	            if (search.startsWith(LIST_SECTION_STRING)) {
	                list.setToolTipText(null);
	            } else {
	                list.setToolTipText((search.length() > 0 ? search : null));
	                int origDelay = ToolTipManager.sharedInstance().getInitialDelay();
	                ToolTipManager.sharedInstance().setInitialDelay(0);
	                ToolTipManager.sharedInstance().mouseMoved(
	                        new MouseEvent(list, 0, 0, 0,
	                        10, rect.y + rect.height - 50,
	                        0, false));
	                ToolTipManager.sharedInstance().setInitialDelay(origDelay);
	            }
            }

        } catch (Exception ee) { Logger.logdebug(ee);/* just to be sure */ }
    }

    private void selectFirstMatchingElement() {
    	int index=1; //start with item 1 as item 0 is always "<other boat>" or "<other person>"
		DefaultListModel <ItemTypeListData> theData= (DefaultListModel)list.getModel();
    	  // check whether we should really select this item

		if (filterTextField.getText().trim().length()==0){
			index=0;
		} else { 
			while (index >= 0 && index < theData.size() && (theData.get(index).separator)) {
	            index += 1;
	        }
		}
		if (index>=theData.size()) {
        	//no element could be found which is not a separator and not "<other boat>" or "<other person>"
        	//so we select the first element nonetheless
        	index=0;
        }

        if (index >= 0 && index < theData.size()) {
            list.setSelectedIndex(index);
            Rectangle rect = list.getCellBounds(index, (index + 15 >= theData.size() ? theData.size() - 1 : index + 15));
            list.scrollRectToVisible(rect);
        }

    }

    public Object getSelectedValue() {
        try {
            if (list == null || list.isSelectionEmpty()) {
                return null;
            }
            ItemTypeListData item = (ItemTypeListData)list.getSelectedValue();
            return item.object;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean getSelectedItemIsSeparator() {
        try {
            if (list == null || list.isSelectionEmpty()) {
                return false;
            }
            ItemTypeListData item = (ItemTypeListData)list.getSelectedValue();
            return item.separator;
        } catch (Exception e) {
            return false;
        }    	
    }
    
    public String getSelectedText() {
        try {
            if (list == null || list.isSelectionEmpty()) {
                return null;
            }
            ItemTypeListData item = (ItemTypeListData)list.getSelectedValue();
            return item.text;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isValidInput() {
        return true;
    }

    public String getValueFromField() {
        return null;
    }

    public void showValue() {
    	list.setModel(data);
        list.setCellRenderer(new ListDataCellRenderer());
        filter();
    }
    
    public void getValueFromGui() {
    }
    
    public void parseValue(String value) {
        // nothing to do
    }

    public void setFieldSize(int width, int height) {
        super.setFieldSize(width, height);
        if (scrollPane != null) {
            scrollPane.setPreferredSize(new Dimension(fieldWidth, fieldHeight));
        }
    }

    public void setDescription(String s) {
        super.setDescription(s);
        if (label != null) {
            label.setText(s);
        }
    }

    public void setSelectedIndex(int i) {
        list.setSelectedIndex(i);
    }

    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }

    public JPanel getPanel() {
        return mypanel;
    }

    // functions for documentListener for the filter text field
    public void insertUpdate(DocumentEvent e) {
        filter();
    }


    public void removeUpdate(DocumentEvent e) {
        filter();
    }

    public void changedUpdate(DocumentEvent e) {
        filter();
    }

    //filter items depending on text entered  in the filterTextField.
    //if filtertextfield is empty, set list Data to the alldata.
    private void filter() {
        
		// this.filterTextField actually CAN be null also if this.showFilterField is true
		// this is the fact for personsAvailableList which is only shown after first user interaction - 
		// before the user interaction of showing the personsAvailableList, the filterTextfield is null.
		// so let's avoid a nullpointerexception here. 

    	if (this.showFilterField && this.filterTextField!=null) {

    		boolean ignoreSpecialCharacters = Daten.efaConfig.getValueEfaBoathouseFilterTextfieldEasyFindEntriesWithSpecialCharacters();
    		
    		DefaultListModel<ItemTypeListData> theModel = new DefaultListModel<ItemTypeList.ItemTypeListData>();
			String searchString = filterTextField.getText().trim().toLowerCase();
			Boolean searchStringWithUmlaut = EfaUtil.containsUmlaut(searchString);
			
	        if (!searchString.isEmpty()) {
	        	
	        	for (int i=0; i< data.getSize();i++) {
	        		ItemTypeListData item = data.getElementAt(i);

	        		if (item.separator || item.text.equals(other_item_text)) {
		                theModel.addElement(item);	        			
	        		} else {
	        			// this is the slow part
	        			boolean hit=false;
	        			if (ignoreSpecialCharacters) {
	        				if (searchStringWithUmlaut) {
	        					//search string contains a special character. this indicates user is definitely looking
	        					//for an entry that contains this character. So we switch to standard contains mode.
	        					hit = item.getFilterableText().toLowerCase().contains(searchString);
	        				} else {
	        					//search string has no umlaut --> we also want to find entries which contain special characters
		        				hit = EfaUtil.replaceAllUmlautsLowerCaseFast(item.getFilterableText()).contains(searchString);	        					
	        				}

	        			} else {
	        				hit = item.getFilterableText().toLowerCase().contains(searchString);
	        			}

	        			if (hit) {
	        				theModel.addElement(item);
	        			}
	        		  
	        	  }

	        	}
	        	
	        	// we have a problem if there are section strings at the end of the list
	        	// remove all entries from the bottom which start with LIST_SECTION_STRING
	
	        	for (int i= theModel.getSize()-1; i>=0;i--) {
	        		if (theModel.getElementAt(i).separator){
	        			theModel.removeElementAt(i);
	        		}
	        		else {
	        			//we have found a non-section item: break out of the for loop
	        			break;
	        		}
	        	}
		        list.setModel(theModel);
		     } else {
		    	list.setModel(data);
		     }
	        selectFirstMatchingElement();
        } //if showFilterTextField

    }
    
    public void keyPressed(KeyEvent e) {
       	updateLastFilterChange();   	
    }
    
    public void keyReleased(KeyEvent e) {
    	
    	if (e.getComponent().equals(filterTextField)) {
        	updateLastFilterChange();

        if (e.getKeyCode()== KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP) {
	    		list.requestFocus();
	    	} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
	    		if (this.showFilterField) {
		    		filterTextField.setText("");
		    		filterTextField.requestFocus();
	    		}
	    	}
	    }
    }
    
    public void keyTyped(KeyEvent e) {
    	updateLastFilterChange();
    }

    private void updateLastFilterChange() {
    	lastFilterChange=System.currentTimeMillis(); 
    }
    
    private JList createNewListWithTooltippSupport() {
    	return new JList() {

    		public String getToolTipText(MouseEvent me) {
	          try {
		        	if ((this.getToolTipText()!=null) && !this.getToolTipText().isEmpty()){
		        		return this.getToolTipText();
		            } else {     	
			        	int index = locationToIndex(me.getPoint());
			            if (index > -1) {
			            	ItemTypeListData item = (ItemTypeListData) getModel().getElementAt(index);
			               return item.toolTipText;
			            }
	          }
	        	}
	          catch (Exception e) {
	        	  Logger.logdebug(e);
	            return "";
	            }
	          return "";
	        }
    	};
    }
    
    public Boolean getShowTwoColumnList() {
    	return showTwoColumnList;
    }
    
    public void setShowTwoColumnList(boolean twoColumns) {
    	showTwoColumnList=twoColumns;
    }
    
    public void updateSeparatorColorFromEfaConfig() {
        if (Daten.isEfaFlatLafActive()) {
        	this._separatorBackground=Daten.efaConfig.getEfaGuiflatLaf_Background();
       }
    }
}
