package de.nmichael.efa.gui.widgets;

/**
 * Class for storing current weather data (no forecast)
 */
public class WeatherDataCurrent {
    private double temperature;
    private double windSpeed;
    private double windDirection;
    private String windDirectionText;
    private int openMeteoCode;  // original openMeteo weather code
    private int weatherApiCode; // openMeteoCode converted to weatherAPI code, as this is the base for efa
    private int weatherApiIconCode; // openMeteoCode converted to weatherAPI Icon code
    private String description; 
    private int isDay; //is it day or night?
    
    // Getter & Setter
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }

    public double getWindDirection() { return windDirection; }
    public void setWindDirection(double windDirection) { this.windDirection = windDirection; }

    public String getWindDirectionText() { return windDirectionText; }
    public void setWindDirectionText(String windDirectionText) { this.windDirectionText = windDirectionText; }
    
    public int getOpenMeteoCode() { return openMeteoCode; }
    public void setOpenMeteoCode(int openMeteoCode) { this.openMeteoCode = openMeteoCode; }

    public int getWeatherApiCode() { return weatherApiCode; }
    public void setWeatherApiCode(int weatherApiCode) { this.weatherApiCode = weatherApiCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getIsDay() { return isDay; }
    public void setIsDay(int isDay) { this.isDay= isDay; }

    public int getIconCode() { return weatherApiIconCode; }
    public void setIconCode(int iconCode) { this.weatherApiIconCode= iconCode; }
    
    @Override
    public String toString() {
        return "WeatherData{" +
                "temperature=" + temperature +
                "windSpeed=" + windSpeed +
                "windDirection=" + windDirection +
                ", openMeteoCode=" + openMeteoCode +
                ", weatherApiCode=" + weatherApiCode +
                ", description='" + description + '\'' +
                '}';
    }
}