package de.nmichael.efa.gui.widgets;

import java.util.List;

import javax.swing.ImageIcon;

public class WeatherDataHourly {
	private List<Long> time;
    private List<Double> temperature2m;
    private List<Integer> weatherCode;
    private List<Double> windSpeed10m;
    private List<Integer> windDirection10m;
    private List<Double> uvIndex;
    private List<Integer> isDay;
    private List<Double> precipitation;
    private List<Double> precipitationProb;
	private List<Integer> openMeteoCode;
	private List<Integer> weatherApiCode;
	private List<Integer> iconcode;
	private List<String> description;
	private List<ImageIcon> uv_index_icon;
    
 	public List<Integer> getIsDay() {
		return isDay;
	}
	public void setIsDay(List<Integer> isDay) {
		this.isDay = isDay;
	}
	public List<Double> getUvIndex() {
		return uvIndex;
	}
	public void setUvIndex(List<Double> uvIndex) {
		this.uvIndex = uvIndex;
	}
	public List<Integer> getWindDirection10m() {
		return windDirection10m;
	}
	public void setWindDirection10m(List<Integer> windDirection10m) {
		this.windDirection10m = windDirection10m;
	}
	public List<Double> getWindSpeed10m() {
		return windSpeed10m;
	}
	public void setWindSpeed10m(List<Double> windSpeed10m) {
		this.windSpeed10m = windSpeed10m;
	}
	public List<Integer> getWeatherCode() {
		return weatherCode;
	}
	public void setWeatherCode(List<Integer> weatherCode) {
		this.weatherCode = weatherCode;
	}
	public List<Double> getTemperature2m() {
		return temperature2m;
	}
	public void setTemperature2m(List<Double> temperature2m) {
		this.temperature2m = temperature2m;
	}
	public List<Long> getTime() {
		return time;
	}
	public void setTime(List<Long> time) {
		this.time = time;
	}
	public List<Double> getPrecipitation() {
		return precipitation;
	}
	public void setPrecipitation(List<Double> precipitation) {
		this.precipitation = precipitation;
	}
	public List<Double> getPrecipitationProb() {
		return precipitationProb;
	}
	public void setPrecipitationProb(List<Double> precipitationProb) {
		this.precipitationProb = precipitationProb;
	}  
	
	public List<Integer> getOpenMeteoCode() {
		return openMeteoCode;
	}
	public void setOpenMeteoCode(List<Integer> openMeteoCode) {
		this.openMeteoCode = openMeteoCode;
	}
	public List<Integer> getWeatherApiCode() {
		return weatherApiCode;
	}
	public void setWeatherApiCode(List<Integer> weatherApiCode) {
		this.weatherApiCode = weatherApiCode;
	}
	public List<Integer> getIconcode() {
		return iconcode;
	}
	public void setIconcode(List<Integer> iconcode) {
		this.iconcode = iconcode;
	}
	public List<String> getDescription() {
		return description;
	}
	public void setDescription(List<String> description) {
		this.description = description;
	}	
	
	/**
	 * Returns the index of the array which matches the next forecast item depending of the current systems time. 
	 *  
	 *  @return int array index for hourly data indices.
	 */
    public int getIndexForCurrentTime() {
    	/* 
    	 * OpenMeteo transmits hourly forecasts in one-hour or three-hour intervals.
    	 * All values ​​are passed in a separate array, and the `TIME` array specifies the corresponding time for each index.
    	 * For the forecast widgets, it is now desired to display the forecast for the NEXT occurring three-hourly events.
    	 * To do this, it is necessary to determine at which index in the array the forecast for the NEXT time interval is located.
    	 * This method serves this purpose. It utilizes the fact that OpenMeteo stores the Unix time (seconds since January 1, 1970) 
    	 * based on UTC in the `TIME` array.
    	 */
   	 	int index=0;
    	long compareTimeStamp=System.currentTimeMillis();
    	
    	if (time !=null && time.size()>0) {
	    	for(Long unixStamp : time)
			{
	    		//we are looking for the index BEFORE the first entry that is in the future compared to now.
	    		//unix Stamp is always in GMT, so we don't have to worry about time zones.
	    		
	    		if (unixStamp.longValue()*1000>compareTimeStamp) {
	    			index--; // we want the item before.
	    			if (index<0) {index=0;}
	    			break;
	    		} else {
	    			index++;
	    		}
			}
	    	return index;
    	} else {
    		return -1;
    	}
    }
	public List<ImageIcon> getUv_index_icon() {
		return uv_index_icon;
	}
	public void setUv_index_icon(List<ImageIcon> uv_index_icon) {
		this.uv_index_icon = uv_index_icon;
	}
    



}
