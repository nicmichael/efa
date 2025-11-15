package de.nmichael.efa.gui.widgets;

public class WeatherDataForeCast {
    private long lastUpdateTimeStamp;
	private long secondsUntilNextUpdate=90000;
    private double latitude;
    private double longitude;
    private double elevation;
    private WeatherDataCurrent currentWeather;
    private WeatherDataHourlyUnits hourlyUnits;
    private WeatherDataHourly hourly;
    private WeatherDataDaily daily;
    private boolean status=false;
    private String statusMessage="";
    
    
    public WeatherDataForeCast() {
    	lastUpdateTimeStamp = System.currentTimeMillis();
    }
    
    public long getLastUpdateTimeStamp() {
    	return lastUpdateTimeStamp;
    }
    
    public long getSecondsUntilNextUpdate() {
    	return secondsUntilNextUpdate;
    }
    
    public void setSecondsUntilNextUpdate(long value) {
    	if (value<secondsUntilNextUpdate) {
    		secondsUntilNextUpdate=value;
    	}
    }

    public boolean needsUpdate() {
    	return System.currentTimeMillis()>(lastUpdateTimeStamp+secondsUntilNextUpdate*1000);
    }
    
	public WeatherDataHourly getHourly() {
		return hourly;
	}
	public void setHourly(WeatherDataHourly hourly) {
		this.hourly = hourly;
	}
	public WeatherDataHourlyUnits getHourlyUnits() {
		return hourlyUnits;
	}
	public void setHourlyUnits(WeatherDataHourlyUnits hourlyUnits) {
		this.hourlyUnits = hourlyUnits;
	}
	public WeatherDataCurrent getCurrentWeather() {
		return currentWeather;
	}
	public void setCurrentWeather(WeatherDataCurrent currentWeather) {
		this.currentWeather = currentWeather;
	}
	public double getElevation() {
		return elevation;
	}
	public void setElevation(double elevation) {
		this.elevation = elevation;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public WeatherDataDaily getDaily() {
		return daily;
	}
	public void setDaily(WeatherDataDaily daily) {
		this.daily = daily;
	}
	public String getStatusMessage() {
		return statusMessage;
	}
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	public boolean getStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}
}


