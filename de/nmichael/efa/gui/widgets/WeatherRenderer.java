package de.nmichael.efa.gui.widgets;

import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
	
	protected static JPanel getLocationHeader(String caption, Boolean showMaximize, WeatherWidgetInstance ww) {
		return WidgetInstance.getLocationHeader(caption, false, showMaximize, ww.getStandardHeaderBackground(), ww.getStandardHeaderForeground());
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
		DecimalFormat df = new DecimalFormat("0.0");
		return df.format(value);
	}
	
}
