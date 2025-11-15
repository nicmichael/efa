package de.nmichael.efa.gui.widgets;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import de.nmichael.efa.Daten;
import de.nmichael.efa.gui.util.RoundedPanel;

public abstract class WeatherRenderer {

	public static void renderWeather(WeatherDataForeCast wdf, JPanel roundPanel, WeatherWidget ww) {
	}
	
	protected static JPanel initializePanel(JPanel mainPanel) {
		JPanel ret = new JPanel();
		ret.setOpaque(false);
		ret.setForeground(mainPanel.getForeground());
		ret.setLayout(new GridBagLayout());		
		return ret;
	}
	
	protected static JLabel initializeLabel(JPanel mainPanel) {
		JLabel ret = new JLabel();
		ret.setOpaque(false);
		ret.setForeground(mainPanel.getForeground());
		return ret;
	}	
	
	protected static JPanel getLocationHeader(String caption, Boolean isError) {
		RoundedPanel titlePanel = new RoundedPanel();
		titlePanel.setLayout(new GridBagLayout());
		titlePanel.setBackground(isError ? Daten.efaConfig.getErrorBackgroundColor() : Daten.efaConfig.getToolTipHeaderBackgroundColor());
		titlePanel.setForeground(isError ? Daten.efaConfig.getErrorForegroundColor() : Daten.efaConfig.getToolTipHeaderForegroundColor());
	
		JLabel titleLabel = new JLabel();
		titleLabel.setText(caption);
		titleLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		titleLabel.setForeground(titlePanel.getForeground());
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		
		titlePanel.add(titleLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		
		return titlePanel;
	}	
	
	protected static JPanel getLocationHeader(String caption) {
		return getLocationHeader(caption, false);
	}
	
	protected static ImageIcon getHourlyWeatherIcon(WeatherDataForeCast wdf, int hourlyIndex) {
		return WeatherIcons.getWeatherIconForCode(wdf.getHourly().getIconcode().get(hourlyIndex), 48, wdf.getHourly().getIsDay().get(hourlyIndex)==1, true);
	}
	
	protected static String getHourlyDescription (WeatherDataForeCast wdf, int hourlyIndex) {
		return wdf.getHourly().getDescription().get(hourlyIndex);
	}
	
	protected static String getHourlyTemp(WeatherDataForeCast wdf, int hourlyIndex, String tempLabel) {
		return oneDecimal(wdf.getHourly().getTemperature2m().get(hourlyIndex)) + tempLabel;
	}
	
	protected static String getHourlyUVIndexVal(WeatherDataForeCast wdf, int hourlyIndex) {
		return oneDecimal(wdf.getHourly().getUvIndex().get(hourlyIndex));
	}

	protected static ImageIcon getHourlyUVIndexIcon(WeatherDataForeCast wdf, int hourlyIndex) {
		return wdf.getHourly().getUv_index_icon().get(hourlyIndex);
	}
	
	protected static String getHourlyRain(WeatherDataForeCast wdf, int hourlyIndex) {
		return oneDecimal(wdf.getHourly().getPrecipitation().get(hourlyIndex));
	}
	
	protected static String getHourlyRainPercentage(WeatherDataForeCast wdf, int hourlyIndex) {
		DecimalFormat df = new DecimalFormat("#");
		return df.format(wdf.getHourly().getPrecipitationProb().get(hourlyIndex));
	}

	
	protected static String getHourlyHourRendering(WeatherDataForeCast wdf, int hourlyIndex) {
		long timestamp = wdf.getHourly().getTime().get(hourlyIndex)*1000;
		
		Instant instant = Instant.ofEpochMilli(timestamp);

		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
		return localDateTime.format(formatter);
	}
	
	protected static String oneDecimal(double value) {
		DecimalFormat df = new DecimalFormat("#.#");
		return df.format(value);
	}
	
}
