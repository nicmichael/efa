package de.nmichael.efa.gui.widgets;

public class WeatherDataHourlyUnits {

    private String time;
    private String temperature2m;
    private String weatherCode;
    private String windSpeed10m;
    private String windDirection10m;
    private String uvIndex;
    private String isDay;
	public String getIsDay() {
		return isDay;
	}
	public void setIsDay(String isDay) {
		this.isDay = isDay;
	}
	public String getUvIndex() {
		return uvIndex;
	}
	public void setUvIndex(String uvIndex) {
		this.uvIndex = uvIndex;
	}
	public String getWindDirection10m() {
		return windDirection10m;
	}
	public void setWindDirection10m(String windDirection10m) {
		this.windDirection10m = windDirection10m;
	}
	public String getWindSpeed10m() {
		return windSpeed10m;
	}
	public void setWindSpeed10m(String windSpeed10m) {
		this.windSpeed10m = windSpeed10m;
	}
	public String getWeatherCode() {
		return weatherCode;
	}
	public void setWeatherCode(String weatherCode) {
		this.weatherCode = weatherCode;
	}
	public String getTemperature2m() {
		return temperature2m;
	}
	public void setTemperature2m(String temperature2m) {
		this.temperature2m = temperature2m;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}

}
