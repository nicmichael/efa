package de.nmichael.efa.gui.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.json.JSONArray;
import org.json.JSONObject;

import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class OpenMeteoApiParser {

    public static final Map<Integer, Integer> weatherCodeMap = new HashMap<>();
    public static final Map<Integer, String> weatherDescMap = new HashMap<>();
    public static final Map<Integer, Integer> weatherApiCodeToWeatherApiIconMap = new HashMap<>();

    static {
        // convert weatherapi weathercode to wmo code, and provide description for weatherapi weathercode
        weatherCodeMap.put(0, 1000); weatherDescMap.put(0, International.getString("WC_MAPI_0"));
        weatherCodeMap.put(1, 1003); weatherDescMap.put(1, International.getString("WC_MAPI_1"));
        weatherCodeMap.put(2, 1006); weatherDescMap.put(2, International.getString("WC_MAPI_2"));
        weatherCodeMap.put(3, 1009); weatherDescMap.put(3, International.getString("WC_MAPI_3"));
        weatherCodeMap.put(45, 1030); weatherDescMap.put(45, International.getString("WC_MAPI_45"));
        weatherCodeMap.put(48, 1135); weatherDescMap.put(48, International.getString("WC_MAPI_48"));
        weatherCodeMap.put(51, 1150); weatherDescMap.put(51, International.getString("WC_MAPI_51"));
        weatherCodeMap.put(53, 1153); weatherDescMap.put(53, International.getString("WC_MAPI_53"));
        weatherCodeMap.put(55, 1153); weatherDescMap.put(55, International.getString("WC_MAPI_55"));
        weatherCodeMap.put(56, 1168); weatherDescMap.put(56, International.getString("WC_MAPI_56"));
        weatherCodeMap.put(57, 1171); weatherDescMap.put(57, International.getString("WC_MAPI_57"));
        weatherCodeMap.put(61, 1183); weatherDescMap.put(61, International.getString("WC_MAPI_61"));
        weatherCodeMap.put(63, 1189); weatherDescMap.put(63, International.getString("WC_MAPI_63"));
        weatherCodeMap.put(65, 1195); weatherDescMap.put(65, International.getString("WC_MAPI_65"));
        weatherCodeMap.put(66, 1204); weatherDescMap.put(66, International.getString("WC_MAPI_66"));
        weatherCodeMap.put(67, 1207); weatherDescMap.put(67, International.getString("WC_MAPI_67"));
        weatherCodeMap.put(71, 1210); weatherDescMap.put(71, International.getString("WC_MAPI_71"));
        weatherCodeMap.put(73, 1216); weatherDescMap.put(73, International.getString("WC_MAPI_73"));
        weatherCodeMap.put(75, 1225); weatherDescMap.put(75, International.getString("WC_MAPI_75"));
        weatherCodeMap.put(77, 1237); weatherDescMap.put(77, International.getString("WC_MAPI_77"));
        weatherCodeMap.put(80, 1180); weatherDescMap.put(80, International.getString("WC_MAPI_80"));
        weatherCodeMap.put(81, 1186); weatherDescMap.put(81, International.getString("WC_MAPI_81"));
        weatherCodeMap.put(82, 1192); weatherDescMap.put(82, International.getString("WC_MAPI_82"));
        weatherCodeMap.put(85, 1255); weatherDescMap.put(85, International.getString("WC_MAPI_85"));
        weatherCodeMap.put(86, 1258); weatherDescMap.put(86, International.getString("WC_MAPI_86"));
        weatherCodeMap.put(95, 1273); weatherDescMap.put(95, International.getString("WC_MAPI_95"));
        weatherCodeMap.put(96, 1261); weatherDescMap.put(96, International.getString("WC_MAPI_96"));
        weatherCodeMap.put(99, 1276); weatherDescMap.put(99, International.getString("WC_MAPI_99"));
    	weatherApiCodeToWeatherApiIconMap.put(1000, 113);
    	weatherApiCodeToWeatherApiIconMap.put(1003, 116);
    	weatherApiCodeToWeatherApiIconMap.put(1006, 119);
    	weatherApiCodeToWeatherApiIconMap.put(1009, 122);
    	weatherApiCodeToWeatherApiIconMap.put(1030, 143);
    	weatherApiCodeToWeatherApiIconMap.put(1063, 176);
    	weatherApiCodeToWeatherApiIconMap.put(1066, 179);
    	weatherApiCodeToWeatherApiIconMap.put(1069, 182);
    	weatherApiCodeToWeatherApiIconMap.put(1072, 185);
    	weatherApiCodeToWeatherApiIconMap.put(1087, 200);
    	weatherApiCodeToWeatherApiIconMap.put(1114, 227);
    	weatherApiCodeToWeatherApiIconMap.put(1117, 230);
    	weatherApiCodeToWeatherApiIconMap.put(1135, 248);
    	weatherApiCodeToWeatherApiIconMap.put(1147, 260);
    	weatherApiCodeToWeatherApiIconMap.put(1150, 263);
    	weatherApiCodeToWeatherApiIconMap.put(1153, 266);
    	weatherApiCodeToWeatherApiIconMap.put(1168, 281);
    	weatherApiCodeToWeatherApiIconMap.put(1171, 284);
    	weatherApiCodeToWeatherApiIconMap.put(1180, 293);
    	weatherApiCodeToWeatherApiIconMap.put(1183, 296);
    	weatherApiCodeToWeatherApiIconMap.put(1186, 299);
    	weatherApiCodeToWeatherApiIconMap.put(1189, 302);
    	weatherApiCodeToWeatherApiIconMap.put(1192, 305);
    	weatherApiCodeToWeatherApiIconMap.put(1195, 308);
    	weatherApiCodeToWeatherApiIconMap.put(1198, 311);
    	weatherApiCodeToWeatherApiIconMap.put(1201, 314);
    	weatherApiCodeToWeatherApiIconMap.put(1204, 317);
    	weatherApiCodeToWeatherApiIconMap.put(1207, 320);
    	weatherApiCodeToWeatherApiIconMap.put(1210, 323);
    	weatherApiCodeToWeatherApiIconMap.put(1213, 326);
    	weatherApiCodeToWeatherApiIconMap.put(1216, 329);
    	weatherApiCodeToWeatherApiIconMap.put(1219, 332);
    	weatherApiCodeToWeatherApiIconMap.put(1222, 335);
    	weatherApiCodeToWeatherApiIconMap.put(1225, 338);
    	weatherApiCodeToWeatherApiIconMap.put(1237, 350);
    	weatherApiCodeToWeatherApiIconMap.put(1240, 353);
    	weatherApiCodeToWeatherApiIconMap.put(1243, 356);
    	weatherApiCodeToWeatherApiIconMap.put(1246, 359);
    	weatherApiCodeToWeatherApiIconMap.put(1249, 362);
    	weatherApiCodeToWeatherApiIconMap.put(1252, 365);
    	weatherApiCodeToWeatherApiIconMap.put(1255, 368);
    	weatherApiCodeToWeatherApiIconMap.put(1258, 371);
    	weatherApiCodeToWeatherApiIconMap.put(1261, 374);
    	weatherApiCodeToWeatherApiIconMap.put(1264, 377);
    	weatherApiCodeToWeatherApiIconMap.put(1273, 386);
    	weatherApiCodeToWeatherApiIconMap.put(1276, 389);
    	weatherApiCodeToWeatherApiIconMap.put(1279, 392);
    	weatherApiCodeToWeatherApiIconMap.put(1282, 395);
    }

    public static WeatherDataForeCast parseFromOpenMeteo(JSONObject json) {
    	
        JSONObject root = json;
        // main object with coordinates, current_weather and hourly_data
        WeatherDataForeCast wdf = new WeatherDataForeCast();
        
        try {

	        wdf.setLatitude(root.getDouble("latitude"));
	        wdf.setLongitude(root.getDouble("longitude"));
	        wdf.setElevation(root.getDouble("elevation"));    	
	    	
	    	JSONObject current = json.getJSONObject("current_weather");
	
	        int openMeteoCode = current.getInt("weathercode");
	        double temp = current.getDouble("temperature");
	        double windspeed = current.getDouble("windspeed");
	        double winddirection = current.getDouble("winddirection");
	        		
	        // Current Weather -------------------------------
	        WeatherDataCurrent wd = new WeatherDataCurrent();
	        wd.setTemperature(temp);
	        wd.setWindSpeed(windspeed);
	        wd.setWindDirection(winddirection);
	        wd.setWindDirectionText(WeatherWindDirectionConverter.toCompassDirection(winddirection));
	        wd.setOpenMeteoCode(openMeteoCode);
	        wd.setIsDay(current.getInt("is_day"));
	        wd.setWeatherApiCode(weatherCodeMap.getOrDefault(openMeteoCode, 1000));
	        wd.setIconCode(weatherApiCodeToWeatherApiIconMap.getOrDefault(wd.getWeatherApiCode(), 113));
	        wd.setDescription(weatherDescMap.getOrDefault(openMeteoCode, "Unknown"));
	
	        wdf.setCurrentWeather(wd);
	        
	        //Daily Weather -------------------------------
	        JSONObject daily = json.getJSONObject("daily");
	        
	        WeatherDataDaily wdd = new WeatherDataDaily();
	        wdd.setPrecipitation_sum(daily.getJSONArray("precipitation_sum").getDouble(0));
	        wdd.setSunshine_duration(daily.getJSONArray("sunshine_duration").getDouble(0));
	        wdd.setTemperature_2m_max(daily.getJSONArray("temperature_2m_max").getDouble(0));
	        wdd.setTemperature_2m_min(daily.getJSONArray("temperature_2m_min").getDouble(0));
	        wdd.setUv_index_clear_sky_max(daily.getJSONArray("uv_index_clear_sky_max").getDouble(0));
	        wdd.setUv_index_max(daily.getJSONArray("uv_index_max").getDouble(0));
	
	        openMeteoCode = daily.getJSONArray("weather_code").getInt(0);
	        wdd.setOpenMeteoCode(openMeteoCode);
	        wdd.setWeatherApiCode(weatherCodeMap.getOrDefault(openMeteoCode, 1000));
	        wdd.setIconCode(weatherApiCodeToWeatherApiIconMap.getOrDefault(wdd.getWeatherApiCode(), 113));
	        wdd.setDescription(weatherDescMap.getOrDefault(openMeteoCode, "Unknown"));
			wdd.setUv_index_icon(getUVIndexIcon(wdd.getUv_index_max()));
	
	        wdf.setDaily(wdd);
	        
	        // Hourly Units -------------------------------
	        JSONObject hu = root.getJSONObject("hourly_units");
	        WeatherDataHourlyUnits units = new WeatherDataHourlyUnits();
	        units.setTime(hu.getString("time"));
	        units.setTemperature2m(hu.getString("temperature_2m"));
	        units.setWeatherCode(hu.getString("weather_code"));
	        units.setWindSpeed10m(hu.getString("wind_speed_10m"));
	        units.setWindDirection10m(hu.getString("wind_direction_10m"));
	        units.setUvIndex(hu.getString("uv_index"));
	        units.setIsDay(hu.getString("is_day"));
	        wdf.setHourlyUnits(units);
	
	        // Hourly Data -------------------------------
	        JSONObject hd = root.getJSONObject("hourly");
	        WeatherDataHourly hourly = new WeatherDataHourly();
	        hourly.setTime(toLongList(hd.getJSONArray("time")));
	        hourly.setTemperature2m(toDoubleList(hd.getJSONArray("temperature_2m")));
	        hourly.setWeatherCode(toIntList(hd.getJSONArray("weather_code")));
	        hourly.setWindSpeed10m(toDoubleList(hd.getJSONArray("wind_speed_10m")));
	        hourly.setWindDirection10m(toIntList(hd.getJSONArray("wind_direction_10m")));
	        hourly.setUvIndex(toDoubleList(hd.getJSONArray("uv_index")));
	        hourly.setIsDay(toIntList(hd.getJSONArray("is_day")));
	        hourly.setPrecipitation(toDoubleList(hd.getJSONArray("precipitation")));
	        hourly.setPrecipitationProb(toDoubleList(hd.getJSONArray("precipitation_probability")));
	        calculateHourlyWeatherCodeAndIcons(hourly);
	        wdf.setHourly(hourly);
	        
	        wdf.setStatus(true);
	        wdf.setStatusMessage("");
        } catch (Exception e) {
        	wdf.setStatus(false);
        	wdf.setStatusMessage(e.getLocalizedMessage());
        	Logger.logdebug(e);
        }
	    
        return wdf;
    }
    
	private static void calculateHourlyWeatherCodeAndIcons(WeatherDataHourly data) {
		
		List<Integer> openMeteoCode = new ArrayList<Integer>();
		List<Integer>  weatherApiCode = new ArrayList<Integer>();
		List<Integer>  iconcode = new ArrayList<Integer>();
		List<String>  description = new ArrayList<String>();
		List<ImageIcon> uvicon = new ArrayList<ImageIcon>();
		
		if (data.getWeatherCode()!=null && data.getWeatherCode().size()>0) {
			int index=0;
			for(Integer curWCode: data.getWeatherCode())
			{
	    		int openMeteoCodeSingle = curWCode.intValue();
	    		int weatherApiCodeSingle = weatherCodeMap.getOrDefault(openMeteoCodeSingle, 1000);
	    		int iconCodeSingle = weatherApiCodeToWeatherApiIconMap.getOrDefault(weatherApiCodeSingle, 113);
	    		String descriptionSingle = weatherDescMap.getOrDefault(openMeteoCodeSingle, International.getString("Unbekannt"));
	    		ImageIcon uvIconSingle = getUVIndexIcon(data.getUvIndex().get(index));
	    		openMeteoCode.add(openMeteoCodeSingle);
	    		weatherApiCode.add(weatherApiCodeSingle);
	    		iconcode.add(iconCodeSingle);
	    		description.add(descriptionSingle);
	    		uvicon.add(uvIconSingle);
	    		index++;
			}
		}
		
		data.setOpenMeteoCode(openMeteoCode);
		data.setWeatherApiCode(weatherApiCode);
		data.setIconcode(iconcode);
		data.setDescription(description);
		data.setUv_index_icon(uvicon);

	}    
    
    private static List<String> toStringList(JSONArray arr) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.getString(i));
        }
        return list;
    }

    private static List<Double> toDoubleList(JSONArray arr) {
        List<Double> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.getDouble(i));
        }
        return list;
    }
    
    private static List<Integer> toIntList(JSONArray arr) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.getInt(i));
        }
        return list;
    }
    
    private static List<Long> toLongList(JSONArray arr) {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.getLong(i));
        }
        return list;
    }    
    
	private static ImageIcon getUVIndexIcon(double uv_max) {
		if (uv_max <3) {
			return WeatherIcons.getIcon(WeatherIcons.IMAGE_UV_INDEX_LOW);
		} else if (uv_max <5) {
			return WeatherIcons.getIcon(WeatherIcons.IMAGE_UV_INDEX_MEDIUM);
		} else if (uv_max <7) {
			return WeatherIcons.getIcon(WeatherIcons.IMAGE_UV_INDEX_ABOVE_MEDIUM);
		} else if (uv_max <9) {
			return WeatherIcons.getIcon(WeatherIcons.IMAGE_UV_INDEX_HIGH);
		} else if (uv_max <11) {
			return WeatherIcons.getIcon(WeatherIcons.IMAGE_UV_INDEX_VERY_HIGH);
		} else {
			return WeatherIcons.getIcon(WeatherIcons.IMAGE_UV_INDEX_SEVERE);
		}

	}
}    