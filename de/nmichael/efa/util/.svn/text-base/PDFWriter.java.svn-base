/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.util;

import java.io.*;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class PDFWriter {

    private String inputFile;
    private String outputFile;
    int pageCount = 0;

    public PDFWriter(String inputFile, String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String run() {
        String errorMessage = null;
        try {
            OutputStream out = null;
            try {
                out = new BufferedOutputStream(new FileOutputStream(outputFile));

                if (Logger.isTraceOn(Logger.TT_PDF, 5)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDF, "Initializing up FOP Factory ...");
                }
                org.apache.fop.apps.FopFactory fopFactory = org.apache.fop.apps.FopFactory.newInstance();

                if (Logger.isTraceOn(Logger.TT_PDF, 5)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDF, "Getting FOP Instance ...");
                }
                org.apache.fop.apps.Fop fop = fopFactory.newFop(org.apache.xmlgraphics.util.MimeConstants.MIME_PDF, out);

                if (Logger.isTraceOn(Logger.TT_PDF, 5)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDF, "Setting FOP Event Listener ...");
                }
                fop.getUserAgent().getEventBroadcaster().addEventListener(new PDFEventListener());

                if (Logger.isTraceOn(Logger.TT_PDF, 5)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDF, "Initializing up Transformer Factory ...");
                }
                javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();

                if (Logger.isTraceOn(Logger.TT_PDF, 5)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDF, "Getting Transformer Instance ...");
                }
                javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();

                if (Logger.isTraceOn(Logger.TT_PDF, 5)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDF, "Getting StreamSource ...");
                }
                javax.xml.transform.stream.StreamSource src = new javax.xml.transform.stream.StreamSource(new File(inputFile));

                if (Logger.isTraceOn(Logger.TT_PDF, 5)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDF, "Getting SAXResult ...");
                }
                javax.xml.transform.Result result = new javax.xml.transform.sax.SAXResult(fop.getDefaultHandler());

                if (Logger.isTraceOn(Logger.TT_PDF, 1)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDF, "Creating PDF ...");
                }
                transformer.transform(src, result);

                pageCount = fop.getResults().getPageCount();
                if (Logger.isTraceOn(Logger.TT_PDF, 1)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDF, "Successfully created " + pageCount + " Pages PDF.");
                }
            } catch (Exception e1) {
                errorMessage = e1.getMessage();
                if (Logger.isTraceOn(Logger.TT_PDF, 1)) {
                    Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDF, "Error creating PDF: " + errorMessage);
                }
                Logger.logdebug(e1);
            } finally {
                out.close();
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            Logger.logdebug(e);
        }

        return errorMessage;
    }

    class PDFEventListener implements org.apache.fop.events.EventListener {

        public void processEvent(org.apache.fop.events.Event event) {
            if (Logger.isTraceOn(Logger.TT_PDF, 1)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_PDFFOP,
                        org.apache.fop.events.EventFormatter.format(event));
            }
        }
    }
}
