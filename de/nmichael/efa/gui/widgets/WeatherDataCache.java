package de.nmichael.efa.gui.widgets;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.zip.GZIPInputStream;

import org.json.JSONObject;

import de.nmichael.efa.gui.widgets.WeatherDataForeCast.WDFStatus;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

/*
 * The WeatherDataCache holds a list of WeatherDataForeCasts. One item per unique location (based on their geocoordinates).
 * 
 * Every time a NEW weather forecast is requested, a new WDF is created in state "initial". This is for the purpose of updating
 * the weather widget in the GUI, with a text like "Getting weather data".
 * 
 * This WDF in initial mode has an update time of 0 seconds, and the WDCUpdater regularily checks the WeatherDataCache items
 * weather they need an update from the internet.
 * 
 * If so, the data is obtained and the resulting WDF can be either ok (update successful) or error.
 * If it is error, we do not want the update thread to wait the standard update time (which is an hour by default).
 * Instead, in error state the update time increases by each error that occurred. 
 * So the first update is after 1 minute, next 3 minutes, next 5 minutes, next 7 minutes. This is somewhat of a retry pattern:
 * after an initial error, we try again fast, and the longer the error situation persists, the longer the intervals get.
 * 
 * Once the weather data has been obtained successfully, the update interval changes to interval configured by the user.
 * 
 * The widgets on the GUI just update themselves like every minute depending on the weather data which is in the cache 
 * at the time they need to update their widget data.
 * 
 */
public class WeatherDataCache {

	static WeatherDataCache instance = null;
	private WDCUpdater updaterThread = null;
	private ConcurrentHashMap<String, WeatherDataForeCast> forecasts = new ConcurrentHashMap<String, WeatherDataForeCast>();
	private String myTempScale;
	private String mySpeedScale;
	private volatile int updateCacheRegularIntervalSeconds=3600;
	
	static synchronized WeatherDataCache getInstance() {
		if (instance == null) {
			instance = new WeatherDataCache();
		} 
		return instance;
	}
	
	public WeatherDataCache() {
		Logger.logdebug(Logger.TT_WIDGETS, 5, Logger.MSG_DEBUG_METEOWIDGET, "Creating WeatherDataCache. "+updateCacheRegularIntervalSeconds);
		updaterThread = new WDCUpdater(updateCacheRegularIntervalSeconds); 
		updaterThread.scheduleNext(1); //Just wait a single second for the first update interval after a startup of efa.
		updaterThread.start();
	}
	public void setTempScale(String value) {
		myTempScale=value;
	}
	
	public void setSpeedScale(String value) {
		mySpeedScale=value;
	}
	
	public void setRegularUpdateIntervalSeconds(int value) {
		updateCacheRegularIntervalSeconds = value;
	}
	
	/**
	 * This is the main method where a WeatherWidgetInstance may request the current weather data
	 * for a location.
	 * @param source Source for the weatherdata (configured by the user)
	 * @param longitude Longitude of the location
	 * @param latitude  Latitude of the location
	 * @return WeatherDataForecast in a special state
	 */
	public synchronized WeatherDataForeCast getWeatherData(String source, String longitude, String latitude) {
		WeatherDataForeCast existing = forecasts.get(getKey(longitude,latitude));
		if (existing==null){
			//only create new weather data if the cache does not contain this weather location.
			//This is a dummy element stating "getting weather data". Obtaining the actual weather data
			//is the job of the WDCUpdater.
			WeatherDataForeCast wdfNew = createInitialWeatherData(source, longitude, latitude);
			forecasts.put(getKey(longitude, latitude), wdfNew);
			updaterThread.scheduleNext(1); //new item: we want to update this item instantly.
			return wdfNew;
		} else {
			return existing;
		}
	}

	private synchronized WeatherDataForeCast createInitialWeatherData(String source, String longitude, String latitude) {
		// this creates a weatherdataforecast in status "initializing"
		Logger.logdebug(Logger.TT_WIDGETS, 5, Logger.MSG_DEBUG_METEOWIDGET, "WeatherDataCache: New forecast request for "+ getKey(longitude, latitude));	
		WeatherDataForeCast wdfNew = new WeatherDataForeCast(longitude,latitude, this.updateCacheRegularIntervalSeconds);
		wdfNew.setNextUpdateInSeconds(0);
		wdfNew.setSource(source);
		return wdfNew;
	}
	
	private String getKey(String first, String second) {
		return first+"#"+second;
	}

	/*
	 * WeatherDataCacheUpdater (short: WDCUpdater)
	 * This is a thread which has no regular update interval, but updates every time the 
	 * WeatherDataForecast items ask it to. The WeatherDataForecast items know - depending on their state -
	 * what the next reasonable update interval should be.
	 * 
	 * So if the cache has two items, the next checkup interval is the minimum refresh interval of the according items in the cache.
	 * 
	 */
	private class WDCUpdater extends Thread {
		
		private int connectTimeout = 15000; // 15 Seconds for slow internet connections
		private int readTimeout = 15000;
		private int standardUpdateIntervalSeconds;  // the standard update interval which has been set by the user
		private final java.util.concurrent.ScheduledExecutorService scheduler = 
				java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
		            Thread t = new Thread(r, "WeatherDataCache.WDCUpdater");
		            t.setDaemon(true);
		            return t;
				});

        private volatile ScheduledFuture<?> futureScheduleEvent;

        WDCUpdater(int refreshSeconds){
        	standardUpdateIntervalSeconds=refreshSeconds;
        }
        
        /**
         * Set the next Event when the WDCUpdater checks the items in the cache wether they need an update from the internet.
         * @param delaySeconds
         */
        public void scheduleNext(long delaySeconds) {
            if (futureScheduleEvent != null) {
                futureScheduleEvent.cancel(false);
            }
            long interval = (delaySeconds <= 0 ? 60 : delaySeconds);
            futureScheduleEvent = scheduler.schedule(this::updateOnceSafe, interval, java.util.concurrent.TimeUnit.SECONDS);
            Logger.logdebug(Logger.TT_WIDGETS, 5, Logger.MSG_DEBUG_METEOWIDGET, "WDCUpdater: next update in "+ delaySeconds+" seconds.");
        }

        /* 
         * Update the cache, and log an exception if one occurs
         */
        private void updateOnceSafe() {
            try {
                updateOnce();
            } catch (Throwable e) {
                Logger.logdebug(new Exception(e));
            }
        }

        public synchronized void stopMe() {
            try {
                if (futureScheduleEvent != null) {
                    futureScheduleEvent.cancel(true);
                }
            } catch (Exception ignore) {}
            scheduler.shutdownNow();
        }

        /*
         * Check the WDC Cache elements wether they need an update.
         * Calculate the next scheduling event when the next check needs to take place (depending on the cache items)
         */
        private void updateOnce() {
        	
        	long nextUpdateInSeconds = updateCacheRegularIntervalSeconds; // maximum wait time for cache updates
        	if (forecasts.size()==0) {
        		nextUpdateInSeconds = 60; //just check every 60 seconds if the cache is empty
    			Logger.logdebug(Logger.TT_WIDGETS, 5, Logger.MSG_DEBUG_METEOWIDGET, "WDCUpdate: No elements in cache.");
        	}
        	
        	for (String key : forecasts.keySet()) { 
        		WeatherDataForeCast current = forecasts.get(key); 
        		Logger.logdebug(Logger.TT_WIDGETS, 5, Logger.MSG_DEBUG_METEOWIDGET, "WDCUpdate: Checking for "+ key + " Needs update: "+ current.needsUpdate());
        	    
        		if (current.needsUpdate()) {
        	    	WeatherDataForeCast wdfNew = getNewWeatherData(current.getSource(),current.getIdLongitude(),current.getIdLatitude());

        	    	if (wdfNew.getStatus()==WDFStatus.Error) {
        	    		//use the error count from the previous attempt, will get increased 
        	    		wdfNew.setStatus(WDFStatus.Error, current.getErrorCount());
        	    		
        	    		Logger.logdebug(Logger.TT_WIDGETS, 5, Logger.MSG_DEBUG_METEOWIDGET, "WDCUpdate: error getting forecast. "+ key + " " + wdfNew.getStatusMessage());
        	    	}
        	    	forecasts.put(key, wdfNew);        	    	
        	    	nextUpdateInSeconds = Math.min(nextUpdateInSeconds, wdfNew.getNextUpdateInSeconds());
        	    } else {
        	    	nextUpdateInSeconds = Math.min(nextUpdateInSeconds, current.getNextUpdateInSeconds());
        	    }
        	}

        	this.scheduleNext(nextUpdateInSeconds+1);//+1 means that we surely have the next update event AFTER at least one of the items is due.
        	
        }        
        
    	/**
    	 * Handles getting the actual Weather Data from the configured data source.
    	 * 
    	 * @param source
    	 * @param longitude
    	 * @param latitude
    	 * @return
    	 */
    	private synchronized WeatherDataForeCast getNewWeatherData(String source, String longitude, String latitude) {

    		if (source.equalsIgnoreCase(WeatherWidget.WEATHER_SOURCE_OPENMETEO)) {
    			try {
    				WeatherDataForeCast data = fetchMeteoWeather(longitude, latitude);
    				data.setSource(source);
    				return data;
    			} catch (Exception e) {
    				Logger.logdebug(e);
    			}
    		}
    		return null;
    	}
        
    	private WeatherDataForeCast fetchMeteoWeather(String longitude, String latitude) {
    		
    		String urlStr = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude
    				+ "&daily=weather_code,sunshine_duration,uv_index_max,uv_index_clear_sky_max,precipitation_sum,temperature_2m_max,temperature_2m_min,wind_speed_10m_max"
    				+ "&hourly=temperature_2m,weather_code,wind_speed_10m,wind_direction_10m,uv_index,is_day,precipitation,precipitation_probability"
    				+ "&t=temperature_2m,is_day,weather_code,wind_speed_10m,wind_direction_10m"
    				+ (myTempScale.equalsIgnoreCase(WeatherWidget.TEMP_FAHRENHEIT) ? "&temperature_unit=fahrenheit" : "")
    				+ (mySpeedScale.equalsIgnoreCase(WeatherWidget.SPEEDSCALE_MPH) ? "&wind_speed_unit=mph" : "")
    				+ "&current_weather=true"
    				+ "&timezone=GMT&timeformat=unixtime"
    				+ "&forecast_days=1&forecast_hours=24&temporal_resolution=hourly_3"
    				//+ "error" // just for testing: create an error while obtaining weather data.
    				;

    		try {
    			Logger.logdebug(Logger.TT_WIDGETS, 5, Logger.MSG_DEBUG_METEOWIDGET, International.getString("Ermittle Wetterdaten von URL:")+" "+urlStr);

    			String response = fetchJSonFromURL(urlStr, this.connectTimeout, this.readTimeout);
    			JSONObject json = new JSONObject(response.toString());
    			return OpenMeteoApiParser.parseFromOpenMeteo(json, longitude,latitude, this.standardUpdateIntervalSeconds);
    			
    		} catch (Exception e) {
    			WeatherDataForeCast tmp=new WeatherDataForeCast(longitude,latitude, this.standardUpdateIntervalSeconds);
    			tmp.setStatus(WDFStatus.Error);
    			tmp.setStatusMessage(International.getString("Fehler beim Abruf der Wetterdaten.")+"\n\n"+
    					e.toString());
    			tmp.setExceptionText(e.toString());
    			Logger.log(Logger.WARNING, Logger.MSG_WARN_WEATHERUPDATEFAILED, International.getString("Fehler beim Abruf der Wetterdaten.")+e.toString());
    			return tmp;
    		} 
    		catch (NoClassDefFoundError e1) {
    			WeatherDataForeCast tmp=new WeatherDataForeCast(longitude,latitude, this.standardUpdateIntervalSeconds);
    			tmp.setStatus(WDFStatus.Error);
    			tmp.setStatusMessage(
    					International.getString("Fehler beim Abruf der Wetterdaten.")+"\n\n"+ 
    					International.getString("Vermutlich wird die JSON-Bibliothek im Java-Classpath nicht gefunden. Rebooten sie den PC (und nicht nur efa), und prüfen Sie ggfs. den Classpath (CP) runefa.sh/runefa.bat")+"\n\n"+
    					e1.getLocalizedMessage()
    					); 
    			tmp.setExceptionText(e1.toString());
    			Logger.log(Logger.WARNING, Logger.MSG_WARN_WEATHERUPDATEFAILED, International.getString("Fehler beim Abruf der Wetterdaten.")+e1.toString());
    			return tmp;
    		}
    	}
    	
    	private String fetchJSonFromURL(String urlStr, int connectTimeout, int readTimeout) throws Exception {
    	    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
    	    conn.setRequestMethod("GET");
    	    conn.setConnectTimeout(connectTimeout);
    	    conn.setReadTimeout(readTimeout);
    	    conn.setRequestProperty("Accept-Encoding", "gzip");

    	    int status = conn.getResponseCode();
    	    if (status != HttpURLConnection.HTTP_OK) {
    	        throw new RuntimeException("HTTP " + status +" / "+conn.getResponseMessage());
    	    }

    	    InputStream in = conn.getInputStream();
    	    if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
    	        in = new GZIPInputStream(in);
    	    }

    	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
    	        StringBuilder sb = new StringBuilder();
    	        String line;
    	        while ((line = reader.readLine()) != null) {
    	            sb.append(line);
    	        }
    	        return sb.toString();
    	    }
    	}
        
        
    }

	
}
