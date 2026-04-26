package de.nmichael.efa.gui.widgets;

public class WeatherWindDirectionConverter {
    private static final String[] DIRECTIONS = {
        "Nord", "Nordost", "Ost", "Südost",
        "Süd", "Südwest", "West", "Nordwest"
    };

    /**
     * Wandelt eine Gradzahl (0–360) in eine Himmelsrichtung um.
     *
     * @param degrees Windrichtung in Grad
     * @return Himmelsrichtung als String
     */
    public static String toCompassDirection(double degrees) {
        if (degrees < 0 || degrees >= 360) {
            // Normalisieren auf 0–360
            degrees = ((degrees % 360) + 360) % 360;
        }
        // Jede Richtung deckt 45° ab, mit +/- 22.5° Toleranz
        int index = (int) Math.round(degrees / 45.0) % 8;
        return DIRECTIONS[index];
    }
   
}