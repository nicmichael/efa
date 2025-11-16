package de.nmichael.efa.gui.widgets;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONObject;

import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class WeatherDataCache {

	static WeatherDataCache instance = null;
	
	private HashMap<String, WeatherDataForeCast> forecasts = new HashMap<String, WeatherDataForeCast>();
	private String myTempScale;
	private String mySpeedScale;
	private long myUpdateIntervalSeconds=60*60*24;
	
	static WeatherDataCache getInstance() {
		if (instance == null) {
			instance = new WeatherDataCache();
		} 
		return instance;
	}
	
	public void setTempScale(String value) {
		myTempScale=value;
	}
	
	public void setSpeedScale(String value) {
		mySpeedScale=value;
	}
	
	public void setUpdateIntervalSeconds(long value) {
		myUpdateIntervalSeconds = value;
	}
	
	public synchronized WeatherDataForeCast getWeatherData(String source, String longitude, String latitude) {
		WeatherDataForeCast existing = forecasts.get(longitude+latitude);
		if ((existing==null) || existing.needsUpdate()){
			return getNewWeatherData(source, longitude, latitude);
		} else {
			return existing;
		}
	}
	
	private WeatherDataForeCast getNewWeatherData(String source, String longitude, String latitude) {

		if (source.equalsIgnoreCase(WeatherWidget.WEATHER_SOURCE_OPENMETEO)) {
			try {

				WeatherDataForeCast data = fetchMeteoWeather(longitude, latitude);
				data.setSecondsUntilNextUpdate(myUpdateIntervalSeconds);
				forecasts.put(longitude+latitude, data);
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
			Logger.log(Logger.DEBUG, International.getString("Ermittle Wetterdaten von URL:")+" "+urlStr);

			String response = fetchJSonFromURL(urlStr);
			JSONObject json = new JSONObject(response.toString());
			return OpenMeteoApiParser.parseFromOpenMeteo(json);
			
		} catch (Exception e) {
			WeatherDataForeCast tmp=new WeatherDataForeCast();
			tmp.setStatus(false);
			tmp.setStatusMessage(International.getString("Fehler beim Abruf der Wetterdaten.")+"\n\n"+ e.getMessage());
			Logger.log(Logger.WARNING, Logger.MSG_WARN_WEATHERUPDATEFAILED,e);
			return tmp;
		}
	}

	private String fetchJSonFromURL(String urlStr) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(10000);//max 5 seconds for connect
		conn.setReadTimeout(15000); // max 10 seconds for reading data

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				sb.append(line);
			
			int status = conn.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				throw new RuntimeException("WebServer Reply Status " + status + sb.toString());
			}

			return sb.toString();
		}
	}	 
	
}
