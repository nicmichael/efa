package de.nmichael.efa.gui.widgets;

import javax.swing.ImageIcon;

public class WeatherDataDaily {

	private double uv_index_clear_sky_max;
	private double uv_index_max;
	private double wind_speed_10m_max;
	private double sunshine_duration;
	private double temperature_2m_max;
	private double temperature_2m_min;
	private double precipitation_sum;
	private int openMeteoCode;
	private int weatherApiCode; // openMeteoCode converted to weatherAPI code, as this is the base for efa
    private int weatherApiIconCode; // openMeteoCode converted to weatherAPI Icon code
    private String description; 
    private ImageIcon uv_index_icon=null;

	public double getPrecipitation_sum() {
		return precipitation_sum;
	}

	public void setPrecipitation_sum(double precipitation_sum) {
		this.precipitation_sum = precipitation_sum;
	}

	public double getTemperature_2m_min() {
		return temperature_2m_min;
	}

	public void setTemperature_2m_min(double temperature_2m_min) {
		this.temperature_2m_min = temperature_2m_min;
	}

	public double getTemperature_2m_max() {
		return temperature_2m_max;
	}

	public void setTemperature_2m_max(double temperature_2m_max) {
		this.temperature_2m_max = temperature_2m_max;
	}

	public double getSunshine_duration() {
		return sunshine_duration;
	}

	public void setSunshine_duration(double sunshine_duration) {
		this.sunshine_duration = sunshine_duration;
	}

	public double getWind_speed_10m_max() {
		return wind_speed_10m_max;
	}

	public void setWind_speed_10m_max(double wind_speed_10m_max) {
		this.wind_speed_10m_max = wind_speed_10m_max;
	}

	public double getUv_index_max() {
		return uv_index_max;
	}

	public void setUv_index_max(double uv_index_max) {
		this.uv_index_max = uv_index_max;
	}

	public double getUv_index_clear_sky_max() {
		return uv_index_clear_sky_max;
	}

	public void setUv_index_clear_sky_max(double uv_index_clear_sky_max) {
		this.uv_index_clear_sky_max = uv_index_clear_sky_max;
	}

	public int getOpenMeteoCode() {
		return openMeteoCode;
	}

	public void setOpenMeteoCode(int openMeteoCode) {
		this.openMeteoCode = openMeteoCode;
	}

	public int getWeatherApiCode() {
		return weatherApiCode;
	}

	public void setWeatherApiCode(int weatherApiCode) {
		this.weatherApiCode = weatherApiCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getIconCode() {
		return weatherApiIconCode;
	}

	public void setIconCode(int iconCode) {
		this.weatherApiIconCode = iconCode;
	}

	public ImageIcon getUv_index_icon() {
		return uv_index_icon;
	}

	public void setUv_index_icon(ImageIcon uv_index_icon) {
		this.uv_index_icon = uv_index_icon;
	}
	
}
