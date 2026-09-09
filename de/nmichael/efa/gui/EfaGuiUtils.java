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

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.items.ItemTypeLabel;
import de.nmichael.efa.core.items.ItemTypeLabelHeader;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.util.Logger;

/**
 * This class provides common code to create GUI Elements like Headers, Hints and Descriptions. 
 */
public class EfaGuiUtils {
	

	public static final String MENU_SEPARATOR = "---------";
	private static Object desktopHints;
	
	public static ItemTypeLabel createHint(String uniqueName, int type, String category, String caption, int gridWidth,
			int padBefore, int padAfter) {
		//if caption starts with html, do not have a blank as a prefix as this will disable html rendering.
		String newCaption=(caption==null ? "" : (caption.startsWith("<html>") ? caption : " "+caption));
		
		ItemTypeLabel item = (ItemTypeLabel) EfaGuiUtils.createDescription(uniqueName, type, category, newCaption, gridWidth,
				padBefore, padAfter);
    	item.setStoreItem(false);//hint for other elements not to store this item (to efaconfig for instance)
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
			if (captions.length>1 && i<captions.length-1) {
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
        if (text == null || text.isEmpty()) {
            return lines;
        }
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            int lineWidth = fontMetrics.stringWidth(testLine);

            if (lineWidth > maxWidth) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // einzelnes Wort passt nicht in eine Linie - füge es trotzdem als eigene Linie hinzu
                    lines.add(word);
                    currentLine = new StringBuilder();
                }
            } else {
                if (currentLine.length() == 0) {
                    currentLine.append(word);
                } else {
                    currentLine.append(" ").append(word);
                }
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
    	item.setStoreItem(false);//hint for other elements not to store this item (to efaconfig for instance)
		item.setPadding(0, 0, padBefore, padAfter);
		item.setFieldGrid(gridWidth, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL);
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
    	item.setStoreItem(false);//hint for other elements not to store this item (to efaconfig for instance)
        item.setPadding(0, 0, 10, 10);
        item.setFieldGrid(gridWidth,GridBagConstraints.EAST, GridBagConstraints.BOTH);
        return item;
    }	
    
    public static Image getEfaMainIcon() {
        return ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_EFA_ICON).getImage();
    }
    
	/**
	 * addHyperLinkAction
	 * 
	 * Reacts to clicks on hyperlinks in the htmlPane.
	 * If a standard webbrowser is defined in efaconfig -> common -> external programs,
	 * this standard webbrowser is used. If not, the standard system webbrowser is used.
	 * if an error occurrs, the internal webbrowser is used.
	 */
	public static void addHyperlinkAction(JEditorPane htmlPane) {
		htmlPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            	Cursor old;
            	old = htmlPane.getCursor();
            	htmlPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            	String urlString;
            	try {
            		urlString = e.getURL().toURI().toString();
            	} catch (Exception eURLExcept) {
            		Logger.log(eURLExcept);
            		return;
            	}
            	
                try {
                	String theBrowserValue = Daten.efaConfig.getValueBrowser().trim();
                	String theBrowser = (theBrowserValue!=null ? theBrowserValue.trim() : null);
                	if (theBrowser!=null && theBrowser.length()>0) {
                		// use internal browser, if browser config says "INTERN", otherwise start to run the external browser
                		if (theBrowser.equalsIgnoreCase(BrowserDialog.INTERNAL_BROWSER)) {
                			BrowserDialog.openInternalBrowser(null, urlString);
                		} else {
                			BrowserDialog.openExternalBrowser(null, urlString);               			
                		}
                	} else {
                		//else use standard System function to run a browser.
                		Desktop.getDesktop().browse(e.getURL().toURI());
                	}
                } catch (IOException eIO) {
            		try {
            			BrowserDialog.openInternalBrowser(null, urlString);
            		} catch (Exception eOther){
            			Logger.log(eOther);
            		}
                } catch (UnsupportedOperationException eUOP) {
            		try {
            			BrowserDialog.openInternalBrowser(null, urlString);
            		} catch (Exception eOther){
            			Logger.log(eOther);
            		}
                }
                catch (Exception ex) {
        			Logger.log(ex);
                }
                htmlPane.setCursor(old);
            }
        });
	}

	/**
	 * Adds a focusGained event listener on any component on the component and all it's children.
	 * Which scrolls the focused element into view, if it is on a Scrollpane.
	 * @param root Root component
	 */
	public static void enableAutoScrollOnFocus(Component root) {
	    if (root instanceof JComponent) {
	        final JComponent jc = (JComponent) root;

	        jc.addFocusListener(new FocusAdapter() {
	            @Override
	            public void focusGained(FocusEvent e) {
	                SwingUtilities.invokeLater(new Runnable() {
	                    @Override
	                    public void run() {
	                    	scrollToVisible(jc);
	                    }
	                });
	            }
	        });
	    }

	    if (root instanceof Container) {
	        Container container = (Container) root;
	        for (Component child : container.getComponents()) {
	            enableAutoScrollOnFocus(child);
	        }
	    }
	}
	
	/**
	 * If the component is on a ScrollPane, scroll the component into the visible view.
	 * Also works on nested Tabbedpanes and Scrollpanes like in efaConfigDialog.
	 * @param comp
	 */
	public static void scrollToVisible(JComponent comp) {
	    JScrollPane scrollPane = findScrollPane(comp);
	    if (scrollPane == null) return;

	    JViewport viewport = scrollPane.getViewport();

	    // Convert rect to the coordinates of the viewport
	    Rectangle rect = SwingUtilities.convertRectangle(
	            comp.getParent(),
	            comp.getBounds(),
	            viewport // viewport.getView() will not work correctly.
	            );

	    viewport.scrollRectToVisible(rect);
	}
	
	/**
	 * Get the parent ScrollPane the component is placed on. Searches for it recursively, parent by parent.
	 * @param c component
	 * @return Returns the parent Scrollpane, if one exists. Null otherwise
	 */
	private static JScrollPane findScrollPane(Component c) {
	    while (c != null) {
	        if (c instanceof JScrollPane) {
	        	return (JScrollPane) c;
	        } 
	        c = c.getParent();
	    }
	    return null;
	}

    public static Frame getParentFrameRecursive(BaseDialog base) {
    	if (base.getParentFrame()!=null) {
    		return base.getParentFrame();
    	} else if (base.getParentJDialog()!=null) {
    		if (base.getParentJDialog() instanceof BaseDialog) {
    			return getParentFrameRecursive((BaseDialog) base.getParentJDialog());
    		} else {
    			return null;
    		}
    	} else { 
    		return null;
    	}
    		
    }
    

    
    public static Dimension getTabPanelPreferredSize(int reduceHeight, BaseDialog base, int intendedMaxWidth, int intendedMaxHeight) {
		Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
		
		Dimension efaBthsSize = null;
		Frame myParentFrame=EfaGuiUtils.getParentFrameRecursive(base);
		if (myParentFrame!=null) {
			efaBthsSize=myParentFrame.getSize();
		}
		
    	int maxDlgW=Daten.efaConfig.getValueMaxDialogWidth();
    	int maxDlgH=Daten.efaConfig.getValueMaxDialogHeight()-60;
    	
    	//no max size for dialogs set? have a look at configured maximum screen width/height
    	if (maxDlgW<=0) {
    		maxDlgW=Daten.efaConfig.getValueScreenWidth();
    	}
    	if (maxDlgH<=0) {
    		maxDlgH=Daten.efaConfig.getValueScreenHeight();
    	}
    	
    	if (maxDlgW<=0 && efaBthsSize!=null) {
    		maxDlgW = efaBthsSize.width-80;
    		if (maxDlgW<=0) {
				maxDlgW=s.width-80;
			}
 			maxDlgW=Math.min(maxDlgW,intendedMaxWidth);
    	}
    	if (maxDlgH<=0 && efaBthsSize!=null) {
    		maxDlgH = efaBthsSize.height-90;
    		if (maxDlgH<=0) {
    			maxDlgH=s.height-90;
    		}
    		maxDlgH=Math.min(maxDlgH,intendedMaxHeight);
    	}
    	
    	// No size configured for dialogs or even efaBths window? 
    	// then use screen height/width as base
    	maxDlgW=Math.min(s.width-80,intendedMaxWidth);

    	maxDlgH=Math.min(s.height-((1+1)*25), intendedMaxHeight);
    	
		return new Dimension(
				(int) Math.round(maxDlgW), 
				(int) Math.round(maxDlgH-reduceHeight));
    }
    
    public static int getSubCatCount(String strCategory) {
    	String[] subCats=strCategory.split(":");
    	return subCats.length;
    }
    
    
    public static void setStandardRenderingHints(Graphics2D g) {
        // Handle font aliasing hints.
    	if (desktopHints==null) {
            if (Daten.javaVersionInt >= 9) {
				// On Java 9 and above, we can rely on the desktop hints for optimal text rendering, which may include subpixel anti-aliasing if supported by the OS and font.
            	desktopHints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            } // else desktophints remain null
    	}
	
        if (desktopHints!=null && desktopHints instanceof Map) {
			// Java 9+ has better font rendering and less bugs on aliasing, so we can rely on the desktop hints for optimal text rendering.

            // RenderingHints.KEY_TEXT_ANTIALIASING would enable standard aliasing without using 
            // the the operation system's special renderings like LCD Subpixel rendering. This would lead to thicker text, which may be desirable for some users, 
            // but it would look inconsistent with the rest of the Swing GUI. 
            // On Java9 and above, "awt.font.desktophints" is applied, which contains text antialiasing hints set by JVM and operating system. 
            
            @SuppressWarnings("unchecked")
            Map<Object,Object> hints = (Map<Object,Object>) desktopHints;
            for (Map.Entry<Object,Object> e : hints.entrySet()) {
                Object key = e.getKey();
                Object value = e.getValue();
                if (key instanceof RenderingHints.Key && value != null) {
                    g.setRenderingHint((RenderingHints.Key) key, value);
                }
            }
        } else {
			// For Java 8 and below, or if desktopHints could not be obtained, 
        	// we enable standard text antialiasing for better text quality, even if it may be a bit thicker. 
			// This is a tradeoff to avoid very bad font rendering on Java 8, especially on Windows.
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
        
        // For graphics, use standard aliasing for smooth shapes (round borders)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
    }
	
}