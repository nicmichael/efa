package de.nmichael.efa.gui.widgets;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.Logger;

public class WeatherRendererForeCastSimple extends WeatherRenderer {

	public static void renderWeather(WeatherDataForeCast wdf, JPanel roundPanel, WeatherWidgetInstance ww) {
		String tempLabel = ww.getTempLabel(false);
		
		// Build the main panel view
		
		int hourlyIndex = wdf.getHourly().getIndexForCurrentTime();
		
		roundPanel.add(getLocationHeader(ww.getCaption(),!ww.getHtmlPopupURL().isEmpty(), ww), new GridBagConstraints(0, 0, 4, 1, 1.0, 0.0, GridBagConstraints.CENTER,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));		
		
		roundPanel.add(
				addForeCastPanel(WeatherRenderer.getHourlyHourRendering(wdf, hourlyIndex), 
						WeatherRenderer.getHourlyWeatherIcon(wdf,hourlyIndex),
						WeatherRenderer.getHourlyDescription(wdf,hourlyIndex),
						WeatherRenderer.getHourlyTemp(wdf, hourlyIndex, tempLabel), roundPanel, ww),
						new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
						new Insets(4, 2, 2, 4), 0, 0));
		roundPanel.add(
				addForeCastPanel(WeatherRenderer.getHourlyHourRendering(wdf, hourlyIndex+1) , 
						WeatherRenderer.getHourlyWeatherIcon(wdf,hourlyIndex+1),
						WeatherRenderer.getHourlyDescription(wdf,hourlyIndex+1),
						WeatherRenderer.getHourlyTemp(wdf, hourlyIndex+1, tempLabel), roundPanel, ww),
						new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
						new Insets(4, 2, 2, 4), 0, 0));
		roundPanel.add(
				addForeCastPanel(WeatherRenderer.getHourlyHourRendering(wdf, hourlyIndex+2), 
						WeatherRenderer.getHourlyWeatherIcon(wdf,hourlyIndex+2),
						WeatherRenderer.getHourlyDescription(wdf,hourlyIndex+2),
						WeatherRenderer.getHourlyTemp(wdf, hourlyIndex+2, tempLabel), roundPanel, ww),
						new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
						new Insets(4, 2, 2, 4), 0, 0));
		roundPanel.add(
				addForeCastPanel(WeatherRenderer.getHourlyHourRendering(wdf, hourlyIndex+3), 
						WeatherRenderer.getHourlyWeatherIcon(wdf,hourlyIndex+3),
						WeatherRenderer.getHourlyDescription(wdf,hourlyIndex+3),
						WeatherRenderer.getHourlyTemp(wdf, hourlyIndex+3, tempLabel), roundPanel, ww),
						new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
						new Insets(4, 2, 2, 4), 0, 0));
	}


	private static JPanel addForeCastPanel(String time, ImageIcon weatherIcon, String description, String temp, JPanel roundPanel,
			WeatherWidgetInstance ww) {
		JPanel myPanel = initializePanel(roundPanel);
		JLabel timeLabel = initializeLabel(roundPanel);
		JLabel weatherIconLabel = initializeLabel(roundPanel);
		JLabel tempLabel = initializeLabel(roundPanel);
		
		myPanel.setForeground(ww.getStandardForeground());
		myPanel.setBackground(ww.getStandardBackground());

		timeLabel.setText(time);
		timeLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD));
		timeLabel.setForeground(ww.getStandardForeground());

		weatherIconLabel.setIcon(weatherIcon);
		weatherIconLabel.setIconTextGap(0);
		weatherIconLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		weatherIconLabel.setToolTipText(description);
		tempLabel.setText(temp);
		tempLabel.setForeground(ww.getStandardForeground());
		tempLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		
   	    myPanel.add(timeLabel,   		new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, 		new Insets(1,3,1,3), 0, 0));
   	    myPanel.add(weatherIconLabel, 	new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, 	    new Insets(1,3,1,3), 0, 0)); 
   	    myPanel.add(tempLabel, 			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, 		new Insets(1,3,1,3), 0, 0));
   	    
/*
		myPanel.setLayout(new BorderLayout(0, 0));
		myPanel.add(timeLabel, BorderLayout.NORTH);
		myPanel.add(weatherIconLabel, BorderLayout.CENTER);
		myPanel.add(tempLabel, BorderLayout.SOUTH);
*/
   	    
		// Klick auf das Icon wird an Parent weitergeleitet
   		weatherIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Simuliere Klick auf Parent
            	try {
                MouseEvent parentClick = new MouseEvent(
                    roundPanel.getParent(),
                    MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(),
                    e.getModifiersEx(),
                    e.getX() + weatherIconLabel.getX(),
                    e.getY() + weatherIconLabel.getY(),
                    e.getClickCount(),
                    e.isPopupTrigger(),
                    e.getButton()
                );
                for (MouseListener ml : roundPanel.getParent().getMouseListeners()) {
                    ml.mouseClicked(parentClick);
                }
            	} catch (Exception e1) {
            		//should not occurr..
            		Logger.logdebug(e1);
            	}
            }
        });  
		return myPanel;

	}
	

	
	
	
}
