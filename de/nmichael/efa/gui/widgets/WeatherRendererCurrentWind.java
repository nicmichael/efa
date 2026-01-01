package de.nmichael.efa.gui.widgets;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class WeatherRendererCurrentWind extends WeatherRenderer {

	public static void renderWeather(WeatherDataForeCast wdf, JPanel roundPanel, WeatherWidgetInstance ww) {
	
		JLabel curWeather_temp = new JLabel();
		JLabel curWeather_icon = new JLabel();
		JLabel curWeather_minTemp = new JLabel();
		JLabel curWeather_maxTemp = new JLabel();
		JLabel curWeather_sunshine = new JLabel();
		JLabel curWeather_rain = new JLabel();
		JLabel curWeather_sunshineUnit = new JLabel();
		JLabel curWeather_rainUnit = new JLabel();
		
		JLabel curWeather_wind = new JLabel();
		JPanel pnlMinMaxSunRain=new JPanel();
		pnlMinMaxSunRain.setOpaque(false);
		pnlMinMaxSunRain.setForeground(roundPanel.getForeground());
		pnlMinMaxSunRain.setLayout(new GridBagLayout());

		double minTemp = wdf.getDaily().getTemperature_2m_min();
		double maxTemp = wdf.getDaily().getTemperature_2m_max();
		double sunshine = wdf.getDaily().getSunshine_duration()/60/60;
		double rain = wdf.getDaily().getPrecipitation_sum();
		
		String tempLabel = ww.getTempLabel(false);
		
		curWeather_temp = new JLabel();
		curWeather_temp.setForeground(Daten.efaConfig.getToolTipHeaderForegroundColor());
		curWeather_temp.setFont(
				roundPanel.getFont().deriveFont((float) (Daten.efaConfig.getValueEfaDirekt_BthsFontSize() + 10)));
		curWeather_temp.setFont(curWeather_temp.getFont().deriveFont(Font.BOLD));
		curWeather_temp.setText(WeatherRenderer.oneDecimal(wdf.getCurrentWeather().getTemperature()) + ww.getTempLabel(false));

		curWeather_icon.setIcon(WeatherIcons.getWeatherIconForCode(wdf.getCurrentWeather().getIconCode(), 64,
				wdf.getCurrentWeather().getIsDay() == 1, false));
		curWeather_icon.setToolTipText(wdf.getCurrentWeather().getDescription());

		curWeather_minTemp.setText(WeatherRenderer.oneDecimal(minTemp) + tempLabel);
		curWeather_minTemp.setForeground(Daten.efaConfig.getToolTipHeaderForegroundColor());

		curWeather_maxTemp.setText(WeatherRenderer.oneDecimal(maxTemp) + tempLabel);
		curWeather_maxTemp.setForeground(Daten.efaConfig.getToolTipHeaderForegroundColor());

		curWeather_sunshine.setText(WeatherRenderer.oneDecimal(sunshine));
		curWeather_sunshine.setForeground(Daten.efaConfig.getToolTipHeaderForegroundColor());

		curWeather_sunshineUnit.setText("h");
		curWeather_sunshineUnit.setForeground(Daten.efaConfig.getToolTipHeaderForegroundColor());
		curWeather_sunshineUnit.setFont(
				roundPanel.getFont().deriveFont((float) (Daten.efaConfig.getValueEfaDirekt_BthsFontSize() -4)));
		curWeather_sunshineUnit.setVerticalTextPosition(SwingConstants.BOTTOM);
		
		DecimalFormat df = new DecimalFormat("#");
		curWeather_rain.setText(df.format(rain)); 
		curWeather_rain.setForeground(Daten.efaConfig.getToolTipHeaderForegroundColor());

		curWeather_rainUnit.setText("mm");
		curWeather_rainUnit.setForeground(Daten.efaConfig.getToolTipHeaderForegroundColor());
		curWeather_rainUnit.setFont(
				roundPanel.getFont().deriveFont((float) (Daten.efaConfig.getValueEfaDirekt_BthsFontSize() -4)));
		curWeather_rainUnit.setVerticalTextPosition(SwingConstants.BOTTOM);
		/*	roundpanel
		 *  | HEADER                                             |	
		 * 	|CurrentTemp|WeatherIcon|pnlMinMaxSunRain            |
		 *  | WIND                                               |
		 * 
		 * pnlMinMaxSunRain
		 *  |IMG_Max |lblMax|IMG_Sunshine|lblSunshine|lblSunUnit | 
		 *  |IMG_Min |lblMin|IMG_Rain    |lblRain    |lblRainUnit|
		 * 
		 */
		pnlMinMaxSunRain.add(new JLabel (WeatherIcons.getIcon(WeatherIcons.IMAGE_MAX)), 
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(2, 4, 2, 2), 0, 0));
		pnlMinMaxSunRain.add(curWeather_maxTemp, 
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST,GridBagConstraints.VERTICAL, new Insets(2, 2, 2, 2), 0, 0));
		
		pnlMinMaxSunRain.add(new JLabel (WeatherIcons.getIcon(WeatherIcons.IMAGE_SUN)), 
				new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(2, 12, 2, 2), 0, 0));
		pnlMinMaxSunRain.add(curWeather_sunshine, 
				new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,GridBagConstraints.VERTICAL, new Insets(2, 2, 2, 2), 0, 0));		
		pnlMinMaxSunRain.add(curWeather_sunshineUnit, 
				new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,GridBagConstraints.VERTICAL, new Insets(2, 2, 2, 2), 0, 0));		
		
		
		pnlMinMaxSunRain.add(new JLabel (WeatherIcons.getIcon(WeatherIcons.IMAGE_MIN)), 
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(2, 4, 2, 2), 0, 0));
		pnlMinMaxSunRain.add(curWeather_minTemp, 
				new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,GridBagConstraints.VERTICAL, new Insets(2, 2, 2, 2), 0, 0));		
		
		pnlMinMaxSunRain.add(new JLabel (WeatherIcons.getIcon(WeatherIcons.IMAGE_RAIN)), 
				new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(2, 12, 2, 2), 0, 0));
		pnlMinMaxSunRain.add(curWeather_rain, 
				new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,GridBagConstraints.VERTICAL, new Insets(2, 2, 2, 2), 0, 0));		
		pnlMinMaxSunRain.add(curWeather_rainUnit, 
				new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,GridBagConstraints.VERTICAL, new Insets(2, 2, 2, 2), 0, 0));		
		
		
		curWeather_wind.setText(International.getString("Wind") + ": "
				+ International.getString(wdf.getCurrentWeather().getWindDirectionText()) + " "
				+ International.getString("mit") + " " + wdf.getCurrentWeather().getWindSpeed()
				+ ww.getSpeedScale());
		curWeather_wind.setForeground(Daten.efaConfig.getToolTipHeaderForegroundColor());
		curWeather_wind.setHorizontalTextPosition(SwingConstants.CENTER);

		// Build the main panel view

		roundPanel.add(getLocationHeader(ww.getCaption(),!ww.getHtmlPopupURL().isEmpty()), new GridBagConstraints(0, 0, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));		
				
		roundPanel.add(curWeather_temp, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(2, 4, 2, 4), 0, 0));

		roundPanel.add(curWeather_icon, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 4), 0, 0));

		roundPanel.add(pnlMinMaxSunRain, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.BOTH, new Insets(0, 4, 0, 2), 0, 0));

		roundPanel.add(curWeather_wind, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.NORTH,
				GridBagConstraints.VERTICAL, new Insets(0, 2, 0, 2), 0, 0));
		
		// Klick auf das Icon wird an Parent weitergeleitet
        curWeather_icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Simuliere Klick auf Parent
            	try {
                MouseEvent parentClick = new MouseEvent(
                    roundPanel.getParent(),
                    MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(),
                    e.getModifiersEx(),
                    e.getX() + curWeather_icon.getX(),
                    e.getY() + curWeather_icon.getY(),
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
	}
}
