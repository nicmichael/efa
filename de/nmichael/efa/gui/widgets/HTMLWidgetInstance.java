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
import javax.swing.text.Document;
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

	//private static String TEXT_PLAIN = "text/plain";
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

        
        htmlPane.setContentType(TEXT_HTML);
        htmlPane.putClientProperty("html.disable", Boolean.TRUE); 
      	htmlPane.setFont(htmlPane.getFont().deriveFont(Font.PLAIN,14));
        htmlPane.setEditable(false);
        htmlPane.setOpaque(true);
        
        if (colorsActive) {
        	htmlPane.setBackground(backgroundColor);
        	htmlPane.setForeground(foregroundColor);
            HTMLDocument doc = (HTMLDocument) htmlPane.getDocument();
            doc.getStyleSheet().addRule("body { background-color: transparent; }");
            scrollPane.getViewport().setOpaque(false);
        }
        
        // following hyperlinks is automatically "disabled" (if no HyperlinkListener is taking care of it)
        // But we also need to disable submiting of form data:
        HTMLEditorKit kit = (HTMLEditorKit)htmlPane.getEditorKit();
        kit.setAutoFormSubmission(false);
        
        EfaGuiUtils.addHyperlinkAction(htmlPane);
        
    	//Create a rounded Panel border for better Looks
    	createRoundPanelWithCaption();
        addGeneralPopupAction();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());// we want no border on an inner scroll pane.
        
        if (getWidth() > 0 && getHeight() > 0) {
            scrollPane.setPreferredSize(new Dimension(getWidth(), getHeight()));
        }
        scrollPane.getViewport().add(htmlPane, null);

        if (htmlUpdater == null) {
            htmlUpdater = new HTMLUpdater();
        }
        htmlUpdater.setUseHttpCaching(this.useHttpCaching);
        // set the page to load and the update interval. 
        // this will also start the scheduled updates (which use a timer instead of a thread)
        htmlUpdater.setPage(url, updateInterval);
        scrollPane.revalidate();
    }

	private void addGeneralPopupAction() {

	    // popup only when a click appears on any component of the header panel
		// this only affects the maximize button, but also the caption label and the header background panel,
		// if the component is not maximized yet. 
		// If it is already maximized, the maximize button is not shown and the header panel is not clickable.
	    if (titlePanel != null && this.useMaximizeButton) {
	        final MouseAdapter popupClickListener = new MouseAdapter() {
	            @Override
	            public void mouseClicked(MouseEvent e) {
	                Component source = e.getComponent();
	                Cursor old = source.getCursor();
	                source.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	                try {
	                    new WidgetPopupDialog(
	                            getCaption(),
	                            getCopy(),
	                            540, 540, 90
	                    ).showDialog();
	                } finally {
	                    source.setCursor(old);
	                }
	            }
	        };

	        // Header selbst
	        titlePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	        titlePanel.addMouseListener(popupClickListener);

	        // Alle direkten Unterkomponenten
	        for (int curComp = 0; curComp < titlePanel.getComponentCount(); curComp++) {
	            Component comp = titlePanel.getComponent(curComp);
	            if (comp != null) {
	                comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	                comp.addMouseListener(popupClickListener);
	            }
	        }
	    }
	}

	
	private void createRoundPanelWithCaption() {

		roundPanel = new RoundedPanel();
		
		roundPanel.setLayout(new GridBagLayout());
		roundPanel.setBackground((colorsActive ? this.getBackgroundColor() : roundPanel.getBackground()));
		roundPanel.setForeground((colorsActive ? this.getForegroundColor() : roundPanel.getForeground()));
		roundPanel.setBorder(new RoundedBorder(this.getForegroundColor()));
		roundPanel.setName("HTMLWidget-RoundPanel");
		
		String myCaption = this.getCaption();
		if (myCaption==null) {
			myCaption="";
		}
		titlePanel= getHTMLCaptionHeader(myCaption,useMaximizeButton);
		titlePanel.setVisible(isCaptionActive());
		
		roundPanel.add(titlePanel, new GridBagConstraints(0, 0, 4, 1, 1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));	
			
		roundPanel.add(scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
					GridBagConstraints.BOTH, new Insets(2, 4, 2, 4), 0, 0));
		
		scrollPane.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		
		roundPanel.setPreferredSize(new Dimension(this.getWidth(), this.getHeight()));
		roundPanel.revalidate();

	}

	private JPanel getHTMLCaptionHeader(String caption, boolean showMaximize) {
		return WidgetInstance.getLocationHeader(caption, showMaximize, 
				(this.isColorsActive() ? this.getHeaderBackgroundColor() : null), 
				(this.isColorsActive() ? this.getHeaderForegroundColor() : null));
	}	
	
	@Override
	public JComponent getComponent() {
        return roundPanel;
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

	private class HTMLUpdater {

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

        private String correctUrlHTMLWidget(String url) {
            int pos = url.indexOf(":");
            if (pos < 4 || pos > 10) { // http, https, file, mailto, ftp, ...
                url = "file:///" + url;
            }
            return EfaUtil.replace(url, "\\", "/", true);
        }

        
        private void updateOnce() {
            String u = this.url;
            if (u == null || u.trim().isEmpty()) {
                return;
            }
            u = correctUrlHTMLWidget(u);
            final String urlToLoad = u;
            try {
                java.net.URL urlObj = new java.net.URL(urlToLoad);
                String protocol = urlObj.getProtocol();
                // For local files or unsupported protocols, delegate to JEditorPane directly on EDT
                if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                    loadHtmlFromFileUrlAlwaysFresh(urlObj, urlToLoad);
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
                            // Clear the document URL to force a fresh load on setPage(url)
                        	Document doc = htmlPane.getDocument();
                            doc.putProperty(Document.StreamDescriptionProperty, null);     
                            // Now set the page, which will (re)fetch the content
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

        private void loadHtmlFromFileUrlAlwaysFresh(final java.net.URL fileUrl, final String urlToLoad) {
            java.io.BufferedInputStream in = null;
            java.io.Reader reader = null;
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(fileUrl.toURI());

                // Stream-based loading: no full byte[] in memory (works for very large files).
                in = new java.io.BufferedInputStream(java.nio.file.Files.newInputStream(path), 64 * 1024);

                // Read only a small prefix for BOM/meta sniff; then reset and parse as stream.
                final int sniffLimit = 64 * 1024;
                in.mark(sniffLimit);

                java.nio.charset.Charset cs = detectCharsetFromStreamPrefix(in, sniffLimit, java.nio.charset.StandardCharsets.UTF_8);

                in.reset();

                reader = new java.io.InputStreamReader(in, cs);

                HTMLEditorKit kit = new HTMLEditorKit();
                final HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
                doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
                doc.setBase(fileUrl);

                kit.read(reader, doc, 0);

                SwingUtilities.invokeLater(() -> {
                    try {
                        htmlPane.setDocument(doc);
                        htmlPane.setCaretPosition(0);
                        htmlPane.revalidate();
                        htmlPane.repaint();
                    } catch (Exception ee) {
                        htmlPane.setText(International.getString("FEHLER") + ": "
                                + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", urlToLoad, ee.toString()));
                        htmlPane.setCaretPosition(0);
                    }
                });
            } catch (Exception ee) {
                SwingUtilities.invokeLater(() -> {
                    htmlPane.setText(International.getString("FEHLER") + ": "
                            + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", urlToLoad, ee.toString()));
                    htmlPane.setCaretPosition(0);
                });
            } finally {
                try {
                    if (reader != null) {
                        reader.close(); // closes underlying stream as well
                    } else if (in != null) {
                        in.close();
                    }
                } catch (Exception ignore) {
                    // ignore close errors
                }
            }
        }


        private java.nio.charset.Charset detectCharsetFromStreamPrefix(
                java.io.BufferedInputStream in,
                int maxPrefixBytes,
                java.nio.charset.Charset fallback) throws java.io.IOException {

            byte[] prefix = new byte[maxPrefixBytes];
            int len = in.read(prefix);
            if (len <= 0) {
                return fallback;
            }

            // 1) BOM
            if (len >= 3
                    && (prefix[0] & 0xFF) == 0xEF
                    && (prefix[1] & 0xFF) == 0xBB
                    && (prefix[2] & 0xFF) == 0xBF) {
                return java.nio.charset.StandardCharsets.UTF_8;
            }
            if (len >= 2
                    && (prefix[0] & 0xFF) == 0xFE
                    && (prefix[1] & 0xFF) == 0xFF) {
                return java.nio.charset.StandardCharsets.UTF_16BE;
            }
            if (len >= 2
                    && (prefix[0] & 0xFF) == 0xFF
                    && (prefix[1] & 0xFF) == 0xFE) {
                return java.nio.charset.StandardCharsets.UTF_16LE;
            }

            // 2) HTML meta sniff from prefix only
            String head = new String(prefix, 0, len, java.nio.charset.StandardCharsets.ISO_8859_1);

            java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(
                    "(?i)<meta\\s+[^>]*charset\\s*=\\s*['\\\"]?\\s*([a-zA-Z0-9_\\-:.]+)\\s*['\\\"]?[^>]*>");
            java.util.regex.Matcher m1 = p1.matcher(head);
            if (m1.find()) {
                String csName = m1.group(1).trim();
                try {
                    if (java.nio.charset.Charset.isSupported(csName)) {
                        return java.nio.charset.Charset.forName(csName);
                    }
                } catch (Exception ignore) {}
            }

            java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(
                    "(?i)<meta\\s+[^>]*http-equiv\\s*=\\s*['\\\"]?content-type['\\\"]?[^>]*content\\s*=\\s*['\\\"][^'\\\"]*charset\\s*=\\s*([a-zA-Z0-9_\\-:.]+)[^'\\\"]*['\\\"][^>]*>");
            java.util.regex.Matcher m2 = p2.matcher(head);
            if (m2.find()) {
                String csName = m2.group(1).trim();
                try {
                    if (java.nio.charset.Charset.isSupported(csName)) {
                        return java.nio.charset.Charset.forName(csName);
                    }
                } catch (Exception ignore) {}
            }


            // 3) UTF-8 validity check (ohne BOM)
            java.nio.charset.CharsetDecoder utf8 = java.nio.charset.StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
            try {
                utf8.decode(java.nio.ByteBuffer.wrap(prefix, 0, len));
                return java.nio.charset.StandardCharsets.UTF_8;
            } catch (java.nio.charset.CharacterCodingException ignore) {
                // not valid UTF-8
            }

            // 4 UTF-)16 without BOM heuristic (many 0x00 in even/odd positions)
            if (looksLikeUtf16WithoutBom(prefix,len)) {
                return guessUtf16Endian(prefix,len);
            }

            // last) pragmatic fallback
            try {
                if (Daten.isOsWindows()) {
                    return java.nio.charset.Charset.forName("windows-1252");
                } else {
                    return java.nio.charset.StandardCharsets.ISO_8859_1;
                }
            } catch (Exception ignore) {
                return fallback;
            }
        }
        
        private boolean looksLikeUtf16WithoutBom(byte[] bytes, int maxLength) {
            int sample = Math.min(maxLength, 2000);
            if (sample < 4) {
                return false;
            }

            int zeroEven = 0;
            int zeroOdd = 0;
            int pairs = sample / 2;

            for (int i = 0; i + 1 < sample; i += 2) {
                if (bytes[i] == 0) zeroEven++;
                if (bytes[i + 1] == 0) zeroOdd++;
            }

            double evenRatio = (double) zeroEven / pairs;
            double oddRatio = (double) zeroOdd / pairs;

            return evenRatio > 0.3 || oddRatio > 0.3;
        }

        private java.nio.charset.Charset guessUtf16Endian(byte[] bytes, int maxLength) {
            int sample = Math.min(maxLength, 2000);
            int zeroEven = 0;
            int zeroOdd = 0;
            int pairs = sample / 2;

            for (int i = 0; i + 1 < sample; i += 2) {
                if (bytes[i] == 0) zeroEven++;
                if (bytes[i + 1] == 0) zeroOdd++;
            }

            // For mostly ASCII text in UTF-16:
            // BE have  to tends0x00 on even positions, LE on odd positions.
            return (zeroEven > zeroOdd)
                    ? java.nio.charset.StandardCharsets.UTF_16BE
                    : java.nio.charset.StandardCharsets.UTF_16LE;
        }        
        
        public void setUseHttpCaching(boolean useHttpCaching) {
			this.useHttpCaching=useHttpCaching;
		}

        public synchronized void setPage(String url, int updateIntervalInSeconds) {
            this.url = url;
            if (updateIntervalInSeconds <= 0) {
                updateIntervalInSeconds = 24*3600; // only update once per day if no valid interval is given
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
	
	public void setUseMaximizeButton(boolean value) {
		this.useMaximizeButton=value;
	}
	
	public WidgetInstance getCopy() {
		HTMLWidgetInstance wi = new HTMLWidgetInstance();
		wi.setPosition(this.getPosition());
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
