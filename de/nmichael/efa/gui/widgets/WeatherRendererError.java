package de.nmichael.efa.gui.widgets;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.util.International;

public class WeatherRendererError extends WeatherRenderer {
	
	
	public static void renderWeather(WeatherDataForeCast wdf, JPanel roundPanel, WeatherWidgetInstance ww) {
		
		JTextArea errorTextArea= new JTextArea();
		JScrollPane scrollPane = new JScrollPane();
		
		errorTextArea.setBackground(ww.getErrorBackground());
		errorTextArea.setForeground(ww.getErrorForeground());
		errorTextArea.setFont(
				roundPanel.getFont().deriveFont((float) (Daten.efaConfig.getValueEfaDirekt_BthsFontSize())));
		errorTextArea.setFont(errorTextArea.getFont().deriveFont(Font.BOLD));
		errorTextArea.setText(wdf.getStatusMessage());
		errorTextArea.setPreferredSize(new Dimension(250,400));
		errorTextArea.setToolTipText((wdf==null ? 
				International.getString("Ein Protokoll ist in der Logdatei (Admin-Modus: Logdatei anzeigen) zu finden.") : wdf.getExceptionText()));
		errorTextArea.setLineWrap(true);
		errorTextArea.setWrapStyleWord(true);
		errorTextArea.setOpaque(true);
		errorTextArea.setEditable(false);
		errorTextArea.setCaretPosition(0);
		
		roundPanel.setBackground(ww.getErrorBackground());
		roundPanel.setForeground(ww.getErrorForeground());
		roundPanel.setBorder(new RoundedBorder(ww.getErrorForeground()));
		
		JPanel titlePanel = WeatherRenderer.getLocationHeader(ww.getCaption(), true, ww);
		titlePanel.setBackground(ww.getErrorHeaderBackground());
		titlePanel.setForeground(ww.getErrorHeaderForeground());
		
		errorTextArea.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());// we want no border on an inner scroll pane.
        
        scrollPane.setPreferredSize(new Dimension(250,150));
        scrollPane.getViewport().add(errorTextArea, null);	
        scrollPane.setOpaque(true);
		
		// Build the main panel view

		roundPanel.add(titlePanel, new GridBagConstraints(0, 0, 4, 1, 1.0, 0.0, GridBagConstraints.CENTER,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));	
		
		roundPanel.add(scrollPane, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(2, 4, 2, 4), 0, 0));
	}	  
}
