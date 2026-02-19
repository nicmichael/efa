package de.nmichael.efa.gui.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.widgets.WeatherDataForeCast.WDFStatus;
import de.nmichael.efa.util.International;

public class WeatherRendererError extends WeatherRenderer {
	
	
	public static void renderWeather(WeatherDataForeCast wdf, JPanel roundPanel, WeatherWidgetInstance ww) {
		
		JTextArea errorTextArea= new JTextArea();
		JScrollPane scrollPane = new JScrollPane();
		
		Color bg = null;
		Color fg = null;
		Color bgH = null;
		Color fgH = null;
		if (wdf.getStatus() == WDFStatus.Error) {
			bg = ww.getErrorBackground();
			fg = ww.getErrorForeground();
			bgH = ww.getErrorHeaderBackground();
			fgH = ww.getErrorHeaderForeground();
		} else {
			bg = ww.getStandardBackground();
			fg = ww.getStandardForeground();
			bgH = ww.getStandardHeaderBackground();
			fgH = ww.getStandardHeaderForeground();
		}

		errorTextArea.setBackground(bg);
		errorTextArea.setForeground(fg);
		roundPanel.setBackground(bg);
		roundPanel.setForeground(fg);
		
		roundPanel.setOpaque(true);

		errorTextArea.setFont(
				roundPanel.getFont().deriveFont((float) (Daten.efaConfig.getValueEfaDirekt_BthsFontSize())));
		errorTextArea.setFont(errorTextArea.getFont().deriveFont(Font.BOLD));
		String textToDisplay=wdf.getStatusMessage();
		if (wdf.getStatus()==WDFStatus.Initializing) {
			textToDisplay = "\n"+textToDisplay;
		}
		errorTextArea.setText(textToDisplay);
		
		errorTextArea.setPreferredSize(new Dimension(250,(wdf.getStatus()==WDFStatus.Error ? 400 : 100)));
		errorTextArea.setToolTipText((wdf==null ? 
				International.getString("Ein Protokoll ist in der Logdatei (Admin-Modus: Logdatei anzeigen) zu finden.") : wdf.getExceptionText()));
		errorTextArea.setLineWrap(true);
		errorTextArea.setWrapStyleWord(true);
		errorTextArea.setOpaque(true);
		errorTextArea.setEditable(false);
		errorTextArea.setCaretPosition(0);
		
		
		JPanel titlePanel = WeatherRenderer.getLocationHeader(ww.getCaption(), true, ww);
		titlePanel.setBackground(bgH);
		titlePanel.setForeground(fgH);
		
		errorTextArea.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());// we want no border on an inner scroll pane.
        
        scrollPane.setPreferredSize(new Dimension(250,(wdf.getStatus()==WDFStatus.Error ? 150 : 100)));
        scrollPane.getViewport().add(errorTextArea, null);	
        scrollPane.setOpaque(true);
		
		// Build the main panel view

		roundPanel.add(titlePanel, new GridBagConstraints(0, 0, 4, 1, 1.0, 0.0, GridBagConstraints.CENTER,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));	
		
		roundPanel.add(scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(2, 4, 2, 4), 0, 0));
	}	  
}
