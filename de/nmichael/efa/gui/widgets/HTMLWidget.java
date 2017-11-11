/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.widgets;

import de.nmichael.efa.util.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.LogbookRecord;
import java.awt.*;
import java.awt.geom.AffineTransform;
import javax.swing.*;
import javax.swing.text.html.*;
import java.io.*;
import javax.swing.text.Document;

public class HTMLWidget extends Widget {

    public static final String PARAM_WIDTH          = "Width";
    public static final String PARAM_HEIGHT         = "Height";
    public static final String PARAM_SCALE          = "Scale";
    public static final String PARAM_URL            = "Url";

    private JScrollPane scrollPane = new JScrollPane();
    private JEditorPane htmlPane;
    private HTMLUpdater htmlUpdater;

    public HTMLWidget() {
        super("Html", International.getString("HTML-Widget"), true);

        addParameterInternal(new ItemTypeInteger(PARAM_WIDTH, 200, 1, Integer.MAX_VALUE, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Breite")));

        addParameterInternal(new ItemTypeInteger(PARAM_HEIGHT, 50, 1, Integer.MAX_VALUE, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Höhe")));

        addParameterInternal(new ItemTypeDouble(PARAM_SCALE, 1, 0.1, 10, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Skalierung")));

        addParameterInternal(new ItemTypeFile(PARAM_URL, "",
                International.getString("HTML-Seite"),
                International.getString("HTML-Seite"),
                null,ItemTypeFile.MODE_OPEN,ItemTypeFile.TYPE_FILE,
                IItemType.TYPE_PUBLIC, "",
                "URL"));
    }

    void construct() {
        IItemType iscale = getParameterInternal(PARAM_SCALE);
        final double scale = (iscale != null ? ((ItemTypeDouble)iscale).getValue() : 1.0);
        htmlPane = new JEditorPane() {
            public void paint(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                AffineTransform old = g2d.getTransform();
                g2d.scale(scale, scale);
                super.paint(g2d);
                g2d.setTransform(old);
            }
        };
        htmlPane.setContentType("text/html");
        htmlPane.setEditable(false);
        // following hyperlinks is automatically "disabled" (if no HyperlinkListener is taking care of it)
        // But we also need to disable submiting of form data:
        HTMLEditorKit kit = (HTMLEditorKit)htmlPane.getEditorKit();
        kit.setAutoFormSubmission(false);

        if (getWidth() > 0 && getHeight() > 0) {
            scrollPane.setPreferredSize(new Dimension(getWidth(), getHeight()));
        }
        scrollPane.getViewport().add(htmlPane, null);
        if (htmlUpdater == null) {
            htmlUpdater = new HTMLUpdater();
        }
        htmlUpdater.start();
        htmlUpdater.setPage(getParameterInternal(PARAM_URL).toString(), getUpdateInterval());
    }

    public void setSize(int width, int height) {
        ((ItemTypeInteger)getParameterInternal(PARAM_WIDTH)).setValue(width);
        ((ItemTypeInteger)getParameterInternal(PARAM_HEIGHT)).setValue(height);
    }

    public int getWidth() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WIDTH)).getValue();
    }

    public int getHeight() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_HEIGHT)).getValue();
    }

    public JComponent getComponent() {
        return scrollPane;
    }

    public void stop() {
        if (htmlUpdater != null) {
            htmlUpdater.stopHTML();
        }
    }

    public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
        // nothing to do
    }

    class HTMLUpdater extends Thread {

        volatile boolean keepRunning = true;
        private String url = null;
        private volatile int updateIntervalInSeconds = 24*3600;

        public void run() {
            while (keepRunning) {
                try {
                    try {
                        if (url != null && url.length() > 0) {
                            url = EfaUtil.correctUrl(url);
                            Document doc = new HTMLDocument();
                            doc.putProperty("javax.swing.JEditorPane.postdata", "foobar"); // property must match JEditorPane.PostDataProperty
                            htmlPane.setDocument(doc);
                            htmlPane.setPage(url);
                        }
                    } catch (IOException ee) {
                        htmlPane.setText(International.getString("FEHLER") + ": "
                                + International.getMessage("Kann Adresse '{url}' nicht öffnen: {message}", url, ee.toString()));
                    }
                    Thread.sleep(updateIntervalInSeconds*1000);
                } catch (Exception e) {
                    Logger.logdebug(e);
                }
            }
        }

        public void setPage(String url, int updateIntervalInSeconds) {
            this.url = url;
            if (updateIntervalInSeconds <= 0) {
                updateIntervalInSeconds = 24*3600;
            }
            this.updateIntervalInSeconds = updateIntervalInSeconds;
            this.interrupt();
        }

        public void stopHTML() {
            keepRunning = false;
        }

    }


}
