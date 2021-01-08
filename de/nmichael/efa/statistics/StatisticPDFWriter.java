/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.statistics;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.Plugins;
import java.io.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.util.*;

public class StatisticPDFWriter extends StatisticXMLWriter {

    private static final String STYLE_SHEET = "efa-pdf-styles.xsl";

    public StatisticPDFWriter(StatisticsRecord sr, StatisticsData[] sd) {
        super(sr, sd);
    }

    public boolean write() {
        String pdfOutputFile = sr.sOutputFile;
        String xmlOutputFile = sr.sOutputFile + ".xml";
        String xslOutputFile = EfaUtil.saveFile(STYLE_SHEET, sr.sOutputDir, false, false, true);
        String foOutputFile = sr.sOutputFile + ".fo";

        // write XML
        if (!super.write(xmlOutputFile, true)) {
            return false;
        }


        // transform XML
        try {
            javax.xml.transform.TransformerFactory trfac = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer trans;
            trans = trfac.newTransformer(new javax.xml.transform.stream.StreamSource(new File(xslOutputFile)));
            trans.transform(new javax.xml.transform.stream.StreamSource(new File(xmlOutputFile)), new javax.xml.transform.stream.StreamResult(new File(foOutputFile)));
        } catch (Exception e) {
            Dialog.error("Could not transform XML using XSLT Stylesheet: " + e.getMessage());
            Logger.logdebug(e);
            resultMessage = null;
            return false;
        }

        // write PDF
        this.statisticTask.logInfo(International.getString("Generiere PDF") + " ...\n");
        PDFWriter pdf = null;
        try {
            pdf = new PDFWriter(foOutputFile, pdfOutputFile);
        } catch(NoClassDefFoundError e) {
            Dialog.error(International.getString("Fehlendes Plugin") + ": " + Plugins.PLUGIN_PDF);
            resultMessage = null;
            return false;
        }
        String pdfErr = pdf.run();
        if (pdfErr != null) {
            Dialog.error("Could not create PDF Document: " + pdfErr);
            resultMessage = null;
            return false;
        }

        // open PDF in Arobat Reader
        if (Daten.efaConfig.getValueAcrobat() != null &&
            Daten.efaConfig.getValueAcrobat().length() > 0 &&
            (sr.sEmailAddresses == null || sr.sEmailAddresses.length() == 0) &&
            (sr.sOutputFtpClient == null) && 
            Daten.isGuiAppl()) {
            try {
                String[] cmd = new String[2];
                cmd[0] = Daten.efaConfig.getValueAcrobat();
                cmd[1] = pdfOutputFile;
                Runtime.getRuntime().exec(cmd);
                resultMessage = null;
            } catch (Exception e) {
                Logger.logdebug(e);
                Dialog.error(LogString.cantExecCommand(Daten.efaConfig.getValueAcrobat(), "Acrobat Reader", e.getLocalizedMessage()));
            }
        }

        EfaUtil.deleteFile(xmlOutputFile);
        EfaUtil.deleteFile(foOutputFile);
        return true;
    }

}
