package de.nmichael.efa.gui.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.concurrent.ScheduledFuture;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.gui.EfaGuiUtils;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedPanel;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.HttpCachedFetcher;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class HTMLWidgetInstance extends WidgetInstance implements IWidgetInstance {

	private static String TEXT_PLAIN = "text/plain";
	private static String TEXT_HTML = "text/html";
	
    private JScrollPane scrollPane = new JScrollPane();
    private JEditorPane htmlPane;
    private HTMLUpdater htmlUpdater;
    private RoundedPanel roundPanel;
    private JPanel titlePanel;
    
    private String caption;

    private int width;
    private int height;
    private double scale;
    private String url;    
    private int updateInterval;
    private boolean useMaximizeButton=true;
    
    private boolean colorsActive=false;
    private Color backgroundColor;
    private Color foregroundColor;
    private Color headerbackgroundColor;
    private Color headerforegroundColor;
	private boolean useHttpCaching=false;
    
	@Override
	 public void construct() {

        htmlPane = new JEditorPane() {
            public void paint(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                AffineTransform old = g2d.getTransform();
                g2d.scale(scale, scale);
                super.paint(g2d);
                g2d.setTransform(old);
            }
        };

        if (colorsActive) {
        	htmlPane.setBackground(backgroundColor);
        	htmlPane.setForeground(foregroundColor);
        }
        
        htmlPane.setContentType(TEXT_HTML);
        
        if (Daten.isEfaFlatLafActive()) {
            htmlPane.putClientProperty("html.disable", Boolean.TRUE); 
        	htmlPane.setFont(htmlPane.getFont().deriveFont(Font.PLAIN,14));
        }
        htmlPane.setEditable(false);
        // following hyperlinks is automatically "disabled" (if no HyperlinkListener is taking care of it)
        // But we also need to disable submiting of form data:
        HTMLEditorKit kit = (HTMLEditorKit)htmlPane.getEditorKit();
        kit.setAutoFormSubmission(false);
        
        EfaGuiUtils.addHyperlinkAction(htmlPane);
        
        //now the htmlPane is set up, check if user wants to use a caption.
        if (isCaptionActive()) {
        	createRoundPanelWithCaption();
            addGeneralPopupAction();
            scrollPane.setBorder(BorderFactory.createEmptyBorder());// we want no border on an inner scroll pane.
        }
        
        if (getWidth() > 0 && getHeight() > 0) {
            scrollPane.setPreferredSize(new Dimension(getWidth(), getHeight()));
        }
        scrollPane.getViewport().add(htmlPane, null);

        if (htmlUpdater == null) {
            htmlUpdater = new HTMLUpdater();
        }
        htmlUpdater.setUseHttpCaching(this.useHttpCaching);
        htmlUpdater.start();
        htmlUpdater.setPage(url, updateInterval);
    }

	private void addGeneralPopupAction() {

		//popup only when a click appears on any component of the header panel
		//and useMaximizeButton is false (only) for already maximized component
		if (titlePanel!=null && this.useMaximizeButton) {
			titlePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			titlePanel.addMouseListener(new MouseAdapter() {
	            public void mouseClicked(MouseEvent e) {
	            	Cursor old = titlePanel.getCursor();
	            	titlePanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	                new WidgetPopupDialog(getCaption(),
	                		getCopy(),
	                        540, 540, 90).showDialog();
	                titlePanel.setCursor(old);
	            }
	        });
			
			for (int curComp=0; curComp <titlePanel.getComponentCount(); curComp++) {
				Component comp = titlePanel.getComponent(curComp);
				if (comp!=null) {
					comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					comp.addMouseListener(new MouseAdapter() {
			            public void mouseClicked(MouseEvent e) {
			            	Cursor old = comp.getCursor();
			            	comp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			                new WidgetPopupDialog(getCaption(),
			                		getCopy(),
			                        540, 540, 90).showDialog();
			                comp.setCursor(old);
			            }
			        });					
				}
			}
		}
	}
	
	private void createRoundPanelWithCaption() {

		roundPanel = new RoundedPanel();
		
		roundPanel.setLayout(new GridBagLayout());
		roundPanel.setBackground(this.getBackgroundColor());
		roundPanel.setForeground(this.getForegroundColor());
		roundPanel.setBorder(new RoundedBorder(this.getForegroundColor()));
		roundPanel.setName("HTMLWidget-RoundPanel");
		roundPanel.setLayout(new GridBagLayout());
		
		titlePanel= getHTMLCaptionHeader(this.getCaption(),useMaximizeButton);
		
		roundPanel.add(titlePanel, new GridBagConstraints(0, 0, 4, 1, 1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));	
			
		roundPanel.add(scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
					GridBagConstraints.BOTH, new Insets(2, 4, 2, 4), 0, 0));
		
		scrollPane.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		
		roundPanel.setMinimumSize(new Dimension(240, 120));
		roundPanel.revalidate();
	}

	private JPanel getHTMLCaptionHeader(String caption, Boolean showMaximize) {
		return WidgetInstance.getLocationHeader(caption, false, showMaximize, 
				(this.isColorsActive() ? this.getHeaderBackgroundColor() : null), 
				(this.isColorsActive() ? this.getHeaderForegroundColor() : null));
	}	
	
	@Override
	public JComponent getComponent() {
        return (isCaptionActive() ? roundPanel : scrollPane);
    }

    public void stop() {
        if (htmlUpdater != null) {
            htmlUpdater.stopHTML();
        }
    }

    public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
        // nothing to do
    }
    
    public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}

	public boolean isColorsActive() {
		return colorsActive;
	}

	public void setColorsActive(boolean colorsActive) {
		this.colorsActive = colorsActive;
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public Color getForegroundColor() {
		return foregroundColor;
	}

	public void setForegroundColor(Color foregroundColor) {
		this.foregroundColor = foregroundColor;
	}

	private class HTMLUpdater extends Thread {

		private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
	            Thread t = new Thread(r, "HTMLWidget.HtmlUpdater");
	            t.setDaemon(true);
	            return t;
	        });

        private volatile String url = null;
        private volatile int updateIntervalInSeconds = 24*3600;
        private volatile ScheduledFuture<?> future;
        private final HttpCachedFetcher fetcher = new HttpCachedFetcher();
        private volatile boolean useHttpCaching = true;


        private void schedule() {
            if (future != null) {
                future.cancel(false);
            }
            int interval = (updateIntervalInSeconds <= 0 ? 24*3600 : updateIntervalInSeconds);
            future = scheduler.scheduleAtFixedRate(this::updateOnceSafe, 0, interval, java.util.concurrent.TimeUnit.SECONDS);
        }

        private void updateOnceSafe() {
            try {
                updateOnce();
            } catch (Throwable e) {
                Logger.logdebug(new Exception(e));
            }
        }

        private void updateOnce() {
            String u = this.url;
            if (u == null || u.trim().isEmpty()) {
                return;
            }
            u = EfaUtil.correctUrl(u);
            final String urlToLoad = u;
            try {
                java.net.URL urlObj = new java.net.URL(urlToLoad);
                String protocol = urlObj.getProtocol();
                // For local files or unsupported protocols, delegate to JEditorPane directly on EDT
                if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            htmlPane.setPage(urlObj);
                            htmlPane.setCaretPosition(0);
                        } catch (IOException ee) {
                            htmlPane.setText(International.getString("FEHLER") + ": "
                                    + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", urlToLoad, ee.toString()));
                        }
                    });
                    return;
                }

                if (useHttpCaching) {
                    // Use HttpCachedFetcher for HTTP(S) with conditional requests
                    try {
                        HttpCachedFetcher.FetchResult res = fetcher.fetch();
                        if (res.isNotModified()) {
                            Logger.log(Logger.INFO, Logger.MSG_GENERIC, International.getMessage("HTMLWidget: Inhalt unverändert (304) für '{url}'.",urlToLoad));
                            return;
                        }
                        if (res.isOk() && res.body != null) {
                            String charset = (res.charset != null ? res.charset : "UTF-8");
                            java.io.Reader reader = new java.io.InputStreamReader(new java.io.ByteArrayInputStream(res.body), charset);
                            HTMLEditorKit kit = new HTMLEditorKit();
                            final HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
                            doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
                            doc.setBase(res.baseUrl);
                            try {
                                kit.read(reader, doc, 0);
                            } finally {
                                try { reader.close(); } catch (Exception ignore) {}
                            }
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    htmlPane.setDocument(doc);
                                } catch (Exception ee) {
                                    htmlPane.setText(International.getString("FEHLER") + ": "
                                            + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", urlToLoad, ee.toString()));
                                }
                            });
                        } else {
                            final String msg = "HTTP " + res.httpStatus + " for " + urlToLoad;
                            SwingUtilities.invokeLater(() -> htmlPane.setText(International.getString("FEHLER") + ": " + msg));
                        }
                    } catch (UnsupportedOperationException uoe) {
                        // Should not happen because we guard by protocol above, but just in case
                        SwingUtilities.invokeLater(() -> htmlPane.setText(International.getString("FEHLER") + ": " + uoe.getMessage()));
                    }
                } else {
                    // No ETag-based caching: let JEditorPane handle the HTTP URL directly
                    SwingUtilities.invokeLater(() -> {
                        try {
                            htmlPane.setPage(urlObj);
                        } catch (IOException ee) {
                            htmlPane.setText(International.getString("FEHLER") + ": "
                                    + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", urlToLoad, ee.toString()));
                        }
                    });
                }
            } catch (Exception ee) {
                SwingUtilities.invokeLater(() -> {
                    htmlPane.setText(International.getString("FEHLER") + ": "
                            + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", urlToLoad, ee.toString()));
                });
            }
        }

        public void setUseHttpCaching(boolean useHttpCaching) {
			this.useHttpCaching=useHttpCaching;
		}

        public synchronized void setPage(String url, int updateIntervalInSeconds) {
            this.url = url;
            if (updateIntervalInSeconds <= 0) {
                updateIntervalInSeconds = 24*3600;
            }
            this.updateIntervalInSeconds = updateIntervalInSeconds;
            // set URL in fetcher (resets validators internally)
            fetcher.setUrl(this.url);
            schedule();
        }

        public synchronized void stopHTML() {
            try {
                if (future != null) {
                    future.cancel(true);
                }
            } catch (Exception ignore) {}
            scheduler.shutdownNow();
        }

    }

	public void setUseHttpCaching(boolean httpCacheActive) {
		this.useHttpCaching = httpCacheActive;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}
	
	private boolean isCaptionActive() {
		return !((caption == null) || caption.isEmpty() || caption.length()==0);
	}

	public void setHeaderBackgroundColor(Color headerBackgroundColor) {
		this.headerbackgroundColor=headerBackgroundColor;	
	}

	public void setHeaderForegroundColor(Color headerForegroundColor) {
		this.headerforegroundColor=headerForegroundColor;
	}
	
	public Color getHeaderBackgroundColor() {
		return this.headerbackgroundColor;
	}
	
	public Color getHeaderForegroundColor() {
		return this.headerforegroundColor;
	}
	
	public void setUseMaximizeButton(Boolean value) {
		this.useMaximizeButton=value;
	}
	
	public WidgetInstance getCopy() {
		HTMLWidgetInstance wi = new HTMLWidgetInstance();
		wi.setHeight(this.getHeight());
		wi.setScale(this.getScale());
		wi.setUpdateInterval(this.getUpdateInterval());
		wi.setUrl(this.getUrl());
		wi.setWidth(this.getWidth());
		wi.setColorsActive(colorsActive);
		wi.setBackgroundColor(this.getBackgroundColor());
		wi.setForegroundColor(this.getForegroundColor());
		wi.setHeaderBackgroundColor(this.getHeaderBackgroundColor());
		wi.setHeaderForegroundColor(this.getHeaderForegroundColor());
		wi.setUseHttpCaching(useHttpCaching);
		wi.setCaption(this.getCaption());
		wi.setUseMaximizeButton(false);
		return wi;
	}
}
