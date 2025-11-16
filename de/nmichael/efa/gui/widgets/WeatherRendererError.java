package de.nmichael.efa.gui.widgets;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.util.International;

public class WeatherRendererError extends WeatherRenderer {
	
	
	public static void renderWeather(WeatherDataForeCast wdf, JPanel roundPanel, WeatherWidgetInstance ww) {
		JTextArea errorLabel1= new JTextArea();
		errorLabel1.setBackground(Daten.efaConfig.getErrorBackgroundColor());
		errorLabel1.setForeground(Daten.efaConfig.getErrorForegroundColor());
		errorLabel1.setFont(
				roundPanel.getFont().deriveFont((float) (Daten.efaConfig.getValueEfaDirekt_BthsFontSize())));
		errorLabel1.setFont(errorLabel1.getFont().deriveFont(Font.BOLD));
		errorLabel1.setText(International.getString("Fehler beim Abruf der Wetterdaten."));
		
		errorLabel1.setToolTipText((wdf==null ? 
				International.getString("Ein Protokoll ist in der Logdatei (Admin-Modus: Logdatei anzeigen) zu finden.") : wdf.getStatusMessage()));
		errorLabel1.setLineWrap(true);
		errorLabel1.setWrapStyleWord(true);
		errorLabel1.setOpaque(false);
		errorLabel1.setEditable(false);
		
		roundPanel.setBackground(Daten.efaConfig.getErrorBackgroundColor());
		roundPanel.setForeground(Daten.efaConfig.getErrorForegroundColor());
		roundPanel.setBorder(new RoundedBorder(Daten.efaConfig.getErrorForegroundColor()));
		
		JPanel titlePanel = WeatherRenderer.getLocationHeader(ww.getCaption(), true);
		titlePanel.setBackground(Daten.efaConfig.getErrorHeaderBackgroundColor());
		titlePanel.setForeground(Daten.efaConfig.getErrorHeaderForegroundColor());
		
		// Build the main panel view

		roundPanel.add(titlePanel, new GridBagConstraints(0, 0, 4, 1, 1.0, 0.0, GridBagConstraints.CENTER,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));	
		
		roundPanel.add(errorLabel1, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(2, 4, 2, 4), 0, 0));
	}	  
}
