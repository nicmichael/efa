package de.nmichael.efa.gui.widgets;

import de.nmichael.efa.util.International;

public class WeatherDataForeCast {

	private static int INITIAL_UPDATE_INTERVAL = 5;
	enum WDFStatus {
        Initializing,
        OK,
        Error
    }
    private long lastUpdateTimeStamp;
	private int nextUpdateInSeconds;
	private int regularUpdateIntervalSeconds;
    private double latitude;
    private double longitude;
    private double elevation;
    private WeatherDataCurrent currentWeather;
    private WeatherDataHourlyUnits hourlyUnits;
    private WeatherDataHourly hourly;
    private WeatherDataDaily daily;
    private WDFStatus status = WDFStatus.Initializing;
    private String statusMessage;
    private String exceptionText = "";
    private int errorCount;;
    private String source; 
    
    private String idLongitude;
    private String idLatitude;
    
    public WeatherDataForeCast(String longitude, String latitude, int standardIntervalSeconds) {
    	lastUpdateTimeStamp = System.currentTimeMillis();
    	idLongitude = longitude;
    	idLatitude = latitude; 
    	statusMessage=International.getString("Ermittle Wetterdaten...");
    	status=WDFStatus.Initializing;
    	nextUpdateInSeconds = INITIAL_UPDATE_INTERVAL;
    	regularUpdateIntervalSeconds = standardIntervalSeconds;
    }
    
    public void setSource(String s) {
    	source=s;
    }
    
    public String getSource() {
    	return source;
    }
    
    public int getErrorCount() {
    	return errorCount;
    }
    
    public long getLastUpdateTimeStamp() {
    	return lastUpdateTimeStamp;
    }
    
    public long getNextUpdateInSeconds() {
    	return Math.max(1,
    					(((lastUpdateTimeStamp+(nextUpdateInSeconds*1000))-System.currentTimeMillis())/1000));

    }
    
    public void setNextUpdateInSeconds(int value) {
    	if (value<nextUpdateInSeconds) {
    		nextUpdateInSeconds=value;
    	}
    }
    
    /*
     * Calculates if the weather data is outdated and needs an update.
     * This is so if there had been an error downloading the weather data,
     * or the weather data is older than the update interval.
     */
    public boolean needsUpdate() {
    	return (System.currentTimeMillis()>=(lastUpdateTimeStamp+nextUpdateInSeconds*1000));
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
	
	public void setExceptionText(String value) {
		exceptionText=wrapString(value);
	}
	
	private String wrapString(String s) {
	    StringBuilder sb = new StringBuilder();
	    int count = 0;

	    for (String word : s.split("&")) {
	        if (count + word.length() > 200) {
	            sb.append('\n');
	            count = 0;
	        }
	        sb.append(word).append(' ');
	        count += word.length() + 1;
	    }

	    return sb.toString();
	}

	public String getExceptionText() {
		return exceptionText;
	}
	
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	
	public WDFStatus getStatus() {
		return status;
	}
	
	/*
	 * Set the status of the WeatherDataForecast.
	 * Depending of the status, the update interval is calculated.
	 * 
	 * So that empty weather update forecast updates after 5 seconds,
	 * an errored forecast updates initially with a one minute interval	that increases with the amount of consecutive errors
	 * and a ok forecast updates regulary.
	 */
	
	public void setStatus(WDFStatus status, int previouseErrorCount) {
		this.status = status;
		
		if (status == WDFStatus.OK) {
			this.errorCount=0;
			this.nextUpdateInSeconds = regularUpdateIntervalSeconds;
		} else if (status==WDFStatus.Error) {
			//on the first error, wait a minute.
			//after the first error, wait one minute * errorcount*2 minutes.
			//so 1,3,5,7,9,11... until the standard update interval is reached.
			//we would update every standardUpdateInterval anyways.
			this.nextUpdateInSeconds = Math.min(60+(previouseErrorCount*120), regularUpdateIntervalSeconds); 
			this.errorCount=previouseErrorCount+1;
		} else if (status == WDFStatus.Initializing) {
			this.errorCount=0;
			this.nextUpdateInSeconds = INITIAL_UPDATE_INTERVAL;
		} else {
			this.errorCount=0;
			this.nextUpdateInSeconds = regularUpdateIntervalSeconds;
		}
	}
	
	public void setStatus(WDFStatus status) {
		setStatus(status, 0);
	}

	public String getIdLongitude() {
		return idLongitude;
	}
	
	public String getIdLatitude() {
		return idLatitude;
	}

}


