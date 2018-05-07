/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.widgets;

import de.nmichael.efa.*;
import de.nmichael.efa.core.Plugins;
import de.nmichael.efa.gui.*;
import de.nmichael.efa.util.*;
import de.nmichael.efa.core.items.*;
import de.nmichael.efa.data.LogbookRecord;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.html.*;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import oauth.signpost.OAuthConsumer;  
import oauth.signpost.basic.DefaultOAuthConsumer;  

public class MeteoAstroWidget extends Widget {

    static final String TEMP_CELSIUS              = "CELSIUS";
    static final String TEMP_FAHRENHEIT           = "FAHRENHEIT";

    static final String LAYOUT_COMPACT            = "COMPACT";
    static final String LAYOUT_HORIZONTAL         = "HORIZONTAL";
    static final String LAYOUT_VERTICAL           = "VERTICAL";

    static final String PARAM_LAYOUT              = "Layout";
    static final String PARAM_SHOWSUNRISE         = "ShowSunrise";
    static final String PARAM_LATITUDE            = "Latitude";
    static final String PARAM_LONGITUDE           = "Longitude";

    static final String PARAM_SHOWWEATHER         = "ShowWeather";
    static final String PARAM_WEATHERLOCATION     = "WeatherLocation";
    static final String PARAM_TEMPERATURESCALE    = "TemperatureScale";
    static final String PARAM_SHOWWEATHERLOCATION = "ShowWeatherLocation";
    static final String PARAM_SHOWWEATHERCURTEXT  = "ShowWeatherCurText";
    static final String PARAM_SHOWWEATHERCURIMG   = "ShowWeatherCurImage";
    static final String PARAM_SHOWWEATHERCURTEMP  = "ShowWeatherCurTemp";
    static final String PARAM_SHOWWEATHERCURWIND  = "ShowWeatherCurWind";
    static final String PARAM_SHOWWEATHERFCTEXT   = "ShowWeatherFcText";
    static final String PARAM_SHOWWEATHERFCIMG    = "ShowWeatherFcImg";
    static final String PARAM_SHOWWEATHERFCTEMP   = "ShowWeatherFcTemp";

    static final String PARAM_POPUPEXECCOMMAND    = "PopupExecCommand";
    static final String PARAM_HTMLPOPUPURL        = "HtmlPopupUrl";
    static final String PARAM_HTMLPOPWIDTH        = "HtmlPopupWidth";
    static final String PARAM_HTMLPOPHEIGHT       = "HtmlPopupHeight";

    static final String PARAM_WARNDARKNESS          = "WarnDarkness";
    static final String PARAM_WARNTIMEBEFORESUNSET  = "WarnTimeBeforeSunset";
    static final String PARAM_WARNTIMEAFTERSUNSET   = "WarnTimeAfterSunset";
    static final String PARAM_WARNTIMEBEFORESUNRISE = "WarnTimeBeforeSunrise";
    static final String PARAM_WARNTEXTDARKSOON      = "WarnTextDarkSoon";
    static final String PARAM_WARNTEXTDARKNOW       = "WarnTextDarkNow";

    static final String PARAM_INCLUDEHTMLTEXT       = "IncludeHtmlText";
    static final String PARAM_INCLUDEHTMLPAGE       = "IncludeHtmlPage";
    
    enum WeatherApi {
        google,
        yahoo
    }

    static final WeatherApi weatherApi = WeatherApi.yahoo;

    static final String GOOGLE_API = "http://www.google.com/ig/api";
    static final String GOOGLE_URL = "http://www.google.com";

    /* Documentation: http://developer.yahoo.com/weather/ */
    /* should limit rate to 2,000 requests per day */
    //static final String YAHOO_API = "http://weather.yahooapis.com/forecastrss";
    static final String YAHOO_API = "https://weather.yahooapis.com/forecastrss";
    static final String YAHOO_API2 = "https://query.yahooapis.com/v1/public/yql";
    
    private static WeatherXmlResponse cachedResponse;
    private static long lastResponseTime = 0;

    private JEditorPane htmlPane = new JEditorPane();
    private HTMLUpdater htmlUpdater;

    public MeteoAstroWidget() {
        super("MeteoAstro", International.getString("Meteo-Astro-Widget"), true);
        addParameterInternal(new ItemTypeStringList(PARAM_LAYOUT, LAYOUT_COMPACT,
                new String[] { LAYOUT_COMPACT, LAYOUT_HORIZONTAL, LAYOUT_VERTICAL },
                new String[] { International.getString("kompakt"),
                               International.getString("horizontal"),
                               International.getString("vertikal")
                },
                IItemType.TYPE_PUBLIC, "",
                International.getString("Layout")));

        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWSUNRISE, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Sonnenaufgangs- und -untergangszeit anzeigen")));
        addParameterInternal(new ItemTypeLongLat(PARAM_LATITUDE,
                ItemTypeLongLat.ORIENTATION_NORTH,52,25,9,
                IItemType.TYPE_PUBLIC, "",
                International.getString("geographische Breite")));
        addParameterInternal(new ItemTypeLongLat(PARAM_LONGITUDE,
                ItemTypeLongLat.ORIENTATION_EAST,13,10,15,
                IItemType.TYPE_PUBLIC, "",
                International.getString("geographische Länge")));

        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWWEATHER, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Wetterdaten anzeigen") +
                " (" + International.getString("Internetverbindung erforderlich") + ")"));
        addParameterInternal(new ItemTypeString(PARAM_WEATHERLOCATION,
                "638242", // "14109-Germany",
                IItemType.TYPE_PUBLIC, "",
                International.getString("Ort für Wetterdaten")));
        addParameterInternal(new ItemTypeStringList(PARAM_TEMPERATURESCALE, TEMP_CELSIUS,
                new String[] { TEMP_CELSIUS, TEMP_FAHRENHEIT },
                new String[] { International.getString("Celsius"),
                               International.getString("Fahrenheit")
                },
                IItemType.TYPE_PUBLIC, "",
                International.getString("Temperaturskala")));
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWWEATHERLOCATION, false,
                IItemType.TYPE_PUBLIC, "",
                International.getMessage("{item} anzeigen",
                International.getString("Ort"))));
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWWEATHERCURTEXT, false,
                IItemType.TYPE_PUBLIC, "",
                International.getMessage("{item} anzeigen",
                International.getString("aktuelles Wetter") +
                " (" + International.getString("Text") + ")")));
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWWEATHERCURIMG, false,
                IItemType.TYPE_PUBLIC, "",
                International.getMessage("{item} anzeigen",
                International.getString("aktuelles Wetter") +
                " (" + International.getString("Bild") + ")")));
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWWEATHERCURTEMP, true,
                IItemType.TYPE_PUBLIC, "",
                International.getMessage("{item} anzeigen",
                International.getString("aktuelle Temperatur"))));
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWWEATHERCURWIND, true,
                IItemType.TYPE_PUBLIC, "",
                International.getMessage("{item} anzeigen",
                International.getString("Wind"))));
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWWEATHERFCTEXT, false,
                IItemType.TYPE_PUBLIC, "",
                International.getMessage("{item} anzeigen",
                International.getString("Wettervorhersage") +
                " (" + International.getString("Text") + ")")));
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWWEATHERFCIMG, true,
                IItemType.TYPE_PUBLIC, "",
                International.getMessage("{item} anzeigen",
                International.getString("Wettervorhersage") +
                " (" + International.getString("Bild") + ")")));
        addParameterInternal(new ItemTypeBoolean(PARAM_SHOWWEATHERFCTEMP, true,
                IItemType.TYPE_PUBLIC, "",
                International.getMessage("{item} anzeigen",
                International.getString("Höchst- und Tiefst-Temperatur"))));

        addParameterInternal(new ItemTypeFile(PARAM_HTMLPOPUPURL, "",
                International.getString("HTML-Seite"),
                International.getString("HTML-Seite"),
                null,ItemTypeFile.MODE_OPEN,ItemTypeFile.TYPE_FILE,
                IItemType.TYPE_PUBLIC, "",
                International.getString("HTML-Popup") + ": " +
                International.getString("HTML-Seite")));
        addParameterInternal(new ItemTypeInteger(PARAM_HTMLPOPWIDTH, 400, 1, Integer.MAX_VALUE, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("HTML-Popup") + ": " +
                International.getString("Breite")));
        addParameterInternal(new ItemTypeInteger(PARAM_HTMLPOPHEIGHT, 200, 1, Integer.MAX_VALUE, false,
                IItemType.TYPE_PUBLIC, "",
                International.getString("HTML-Popup") + ": " +
                International.getString("Höhe")));
        addParameterInternal(new ItemTypeString(PARAM_POPUPEXECCOMMAND, "",
                IItemType.TYPE_PUBLIC, "",
                International.getMessage("Auszuführendes Kommando vor {event}",
                International.getString("Popup"))));

        addParameterInternal(new ItemTypeBoolean(PARAM_WARNDARKNESS, true,
                IItemType.TYPE_PUBLIC, "",
                International.getString("Bei Fahrtbeginn vor Dunkelheit warnen")));
        addParameterInternal(new ItemTypeString(PARAM_WARNTEXTDARKSOON,
                International.getString("Achtung, es wird bald dunkel.") + "<br>" +
                International.getString("Bitte nimm Licht mit!"),
                IItemType.TYPE_PUBLIC, "",
                International.getString("Warntext vor Einbruch der Dunkelheit")));
        addParameterInternal(new ItemTypeString(PARAM_WARNTEXTDARKNOW,
                International.getString("Achtung, es ist dunkel.") + "<br>" +
                International.getString("Bitte nimm Licht mit!"),
                IItemType.TYPE_PUBLIC, "",
                International.getString("Warntext bei Dunkelheit")));
        addParameterInternal(new ItemTypeInteger(PARAM_WARNTIMEBEFORESUNSET, 30, 0, 60, false,
                IItemType.TYPE_EXPERT, "",
                PARAM_WARNTIMEBEFORESUNSET));
        addParameterInternal(new ItemTypeInteger(PARAM_WARNTIMEAFTERSUNSET, 30, 0, 60, false,
                IItemType.TYPE_EXPERT, "",
                PARAM_WARNTIMEAFTERSUNSET));
        addParameterInternal(new ItemTypeInteger(PARAM_WARNTIMEBEFORESUNRISE, 30, 0, 60, false,
                IItemType.TYPE_EXPERT, "",
                PARAM_WARNTIMEBEFORESUNRISE));
        addParameterInternal(new ItemTypeString(PARAM_INCLUDEHTMLTEXT, "",
                IItemType.TYPE_PUBLIC, "",
                International.getString("HTML-Text einbinden")));
        addParameterInternal(new ItemTypeString(PARAM_INCLUDEHTMLPAGE, "",
                IItemType.TYPE_PUBLIC, "",
                International.getString("HTML-Seite einbinden")));

        super.setEnabled(true);
        super.setPosition(IWidget.POSITION_CENTER);
    }

    public String getLayout() {
        return ((ItemTypeStringList)getParameterInternal(PARAM_LAYOUT)).toString();
    }

    public boolean isShowSunrise() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWSUNRISE)).getValue();
    }

    public ItemTypeLongLat getLatitude() {
        return (ItemTypeLongLat)getParameterInternal(PARAM_LATITUDE);
    }

    public ItemTypeLongLat getLongitude() {
        return (ItemTypeLongLat)getParameterInternal(PARAM_LONGITUDE);
    }

    public boolean isShowWeather() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWWEATHER)).getValue();
    }

    public String getWeatherLocation() {
        return ((ItemTypeString)getParameterInternal(PARAM_WEATHERLOCATION)).toString();
    }

    public String getTemperatureScale() {
        return ((ItemTypeStringList)getParameterInternal(PARAM_TEMPERATURESCALE)).toString();
    }

    public boolean isShowWeatherLocation() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWWEATHERLOCATION)).getValue();
    }

    public boolean isShowCurrentWeatherText() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWWEATHERCURTEXT)).getValue();
    }

    public boolean isShowCurrentWeatherImage() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWWEATHERCURIMG)).getValue();
    }

    public boolean isShowCurrentWeatherTemperature() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWWEATHERCURTEMP)).getValue();
    }

    public boolean isShowCurrentWeatherWind() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWWEATHERCURWIND)).getValue();
    }

    public boolean isShowForecastWeatherText() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWWEATHERFCTEXT)).getValue();
    }

    public boolean isShowForecastWeatherImage() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWWEATHERFCIMG)).getValue();
    }

    public boolean isShowForecastWeatherTemperature() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_SHOWWEATHERFCTEMP)).getValue();
    }

    public String getPopupExecCommand() {
        return ((ItemTypeString)getParameterInternal(PARAM_POPUPEXECCOMMAND)).toString();
    }

    public String getHtmlPopupUrl() {
        return ((ItemTypeFile)getParameterInternal(PARAM_HTMLPOPUPURL)).toString();
    }

    public int getHtmlPopupWidth() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_HTMLPOPWIDTH)).getValue();
    }

    public int getHtmlPopupHeight() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_HTMLPOPHEIGHT)).getValue();
    }

    public boolean isWarnDarkness() {
        return ((ItemTypeBoolean)getParameterInternal(PARAM_WARNDARKNESS)).getValue();
    }
    public int getWarnTimeBeforeSunset() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WARNTIMEBEFORESUNSET)).getValue();
    }
    public int getWarnTimeAfterSunset() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WARNTIMEAFTERSUNSET)).getValue();
    }
    public int getWarnTimeBeforeSunrise() {
        return ((ItemTypeInteger)getParameterInternal(PARAM_WARNTIMEBEFORESUNRISE)).getValue();
    }
    public String getWarnTextDarkSoon() {
        return ((ItemTypeString)getParameterInternal(PARAM_WARNTEXTDARKSOON)).toString();
    }
    public String getWarnTextDarkNow() {
        return ((ItemTypeString)getParameterInternal(PARAM_WARNTEXTDARKNOW)).toString();
    }
    public String getIncludeHtmlText() {
        return ((ItemTypeString)getParameterInternal(PARAM_INCLUDEHTMLTEXT)).toString();
    }
    public String getIncludeHtmlPage() {
        return ((ItemTypeString)getParameterInternal(PARAM_INCLUDEHTMLPAGE)).toString();
    }
    
    

    void construct() {
        htmlPane.setContentType("text/html");
        htmlPane.setEditable(false);
        htmlPane.setBorder(null);

        // following hyperlinks is automatically "disabled" (if no HyperlinkListener is taking care of it)
        // But we also need to disable submiting of form data:
        ((HTMLEditorKit)htmlPane.getEditorKit()).setAutoFormSubmission(false);

        // HTML-Popup
        htmlPane.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (getHtmlPopupUrl() != null && getHtmlPopupUrl().length() > 0) {
                    new HtmlPopupDialog(getDescription(),
                            getHtmlPopupUrl(),
                            getPopupExecCommand(),
                            getHtmlPopupWidth(), getHtmlPopupHeight(), 60).showDialog();
                }
            }
        });

        try {
            htmlUpdater = new HTMLUpdater(getUpdateInterval());
            htmlUpdater.start();
        } catch(NoClassDefFoundError e) {
            Logger.log(Logger.WARNING, Logger.MSG_WARN_WEATHERUPDATEFAILED, 
                    International.getString("Wetterinformationen können erst nach Neustart angezeigt werden."));
        }
    }

    public JComponent getComponent() {
        return htmlPane;
    }

    public void stop() {
        try {
            htmlUpdater.stopHTML();
        } catch(Exception eignore) {
            // nothing to do, might not be initialized
        }
    }

    public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
        try {
            if ((mode == EfaBaseFrame.MODE_BOATHOUSE_START ||
                 mode == EfaBaseFrame.MODE_BOATHOUSE_START_CORRECT) && !actionBegin &&
                 isWarnDarkness()) {
                Calendar cal = new GregorianCalendar();
                int now = cal.get(Calendar.HOUR_OF_DAY)*60 + cal.get(Calendar.MINUTE);
                String sun[] = SunRiseSet.getSunRiseSet(getLatitude(), getLongitude());
                TMJ tmj = EfaUtil.string2date(sun[0], -1, -1, 0);
                int sunrise = (tmj.tag >= 0 && tmj.monat >= 0 ? tmj.tag*60 + tmj.monat : -1);
                tmj = EfaUtil.string2date(sun[1], -1, -1, 0);
                int sunset = (tmj.tag >= 0 && tmj.monat >= 0 ? tmj.tag*60 + tmj.monat : -1);
                if (sunset < 0 || sunrise < 0 || now < 0) {
                    return;
                }
                String warnText = null;
                if (now <= sunrise-getWarnTimeBeforeSunrise() || now >= sunset+getWarnTimeAfterSunset()) {
                    warnText = getWarnTextDarkNow();
                } else if (now >= sunset-getWarnTimeBeforeSunset() && now < sunset+getWarnTimeAfterSunset()) {
                    warnText = getWarnTextDarkSoon();
                }
                if (warnText != null && Daten.efaConfig.getValueNotificationWindowTimeout() > 0) {
                    NotificationDialog dlg = new NotificationDialog((JFrame)null,
                            warnText,
                            BaseDialog.BIGIMAGE_DARKNESS,
                            "ffffff", "ff0000", Daten.efaConfig.getValueNotificationWindowTimeout());
                    dlg.showDialog();
                }
            }
        } catch(Exception eignore) {
            Logger.logdebug(eignore);
        }
    }

    class HTMLUpdater extends Thread {

        volatile boolean keepRunning = true;
        private boolean sunriseError = false;
        private boolean weatherError = false;
        private int weatherSlowDownUpdateFactor = 1;
        private int updateIntervalInSeconds = 24*3600;

        public HTMLUpdater(int updateIntervalInSeconds) {
            this.updateIntervalInSeconds = updateIntervalInSeconds;
        }

        public void run() {
            String bgcolor = EfaUtil.getColor(myPanel.getBackground());

            int weatherCnt = 0;
            while (keepRunning) {
                Vector<Cell> data = new Vector<Cell>();
                int row = 1;
                int column = 1;

                // Weather
                if (isShowWeather() && (++weatherCnt % weatherSlowDownUpdateFactor) == 0) {
                    long availBytes = 0;
                    String url = null;
                    InputStream in = null;
                    URLConnection conn = null;
                    XMLReader parser = null;
                    try {
                        WeatherXmlResponse response = null;
                        if (cachedResponse != null &&
                            (System.currentTimeMillis() - lastResponseTime < 3600*1000 ||
                             EfaUtil.getCurrentHour() >= 20 || EfaUtil.getCurrentHour() <= 5)) {
                            // use cached response for at least one hour, and always during night
                            response = cachedResponse;
                        } else {
                            switch (weatherApi) {
                                case google:
                                    url = GOOGLE_API + "?weather=" + getWeatherLocation() + "&hl=" + International.getLanguageID() + "&ie=utf-8&oe=utf-8";
                                    conn = new URL(url).openConnection();
                                    conn.connect();
                                    in = conn.getInputStream();
                                    availBytes = in.available();
                                    in.mark(1024 * 1024);
                                    parser = EfaUtil.getXMLReader();
                                    response = new GoogleXmlResponse();
                                    parser.setContentHandler(response);
                                    parser.parse(new InputSource(in));
                                    break;
                                case yahoo:
                                    /* old Yahoo API (EOL)
                                    url = YAHOO_API + "?w=" + getWeatherLocation() + "&u="
                                            + (getTemperatureScale().equals(TEMP_CELSIUS) ? "c" : "f");
                                    */
                                    url = YAHOO_API2 + "?q=select%20*%20from%20weather.forecast%20where%20woeid%3D%22" + getWeatherLocation() + "%22" +
                                            "%20and%20u%3D%22" + (getTemperatureScale().equals(TEMP_CELSIUS) ? "c" : "f") + "%22"
                                            + "&format=xml&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
                                    OAuthConsumer consumer = new DefaultOAuthConsumer("dj0yJmk9TDh2Q05KY0dIZUxmJmQ9WVdrOVZVRnZNbVpRTkRnbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1iNA--", "07b26654a24b6ada504616a50b27bff74a08975e");
                                    conn = new URL(url).openConnection();
                                    consumer.sign((HttpsURLConnection) conn);
                                    conn.connect();
                                    in = conn.getInputStream();
                                    availBytes = in.available();
                                    in.mark(1024 * 1024);
                                    parser = EfaUtil.getXMLReader();
                                    response = new YahooXmlResponse();
                                    parser.setContentHandler(response);
                                    parser.parse(new InputSource(in));
                                    break;
                            }
                            cachedResponse = response;
                            lastResponseTime = System.currentTimeMillis();
                        }

                        if (getLayout().equals(LAYOUT_HORIZONTAL)) {
                            if (isShowWeatherLocation()) {
                                data.add(new Cell(column++, row, response.city + ":", true));
                            }
                            if (isShowCurrentWeatherText() || isShowCurrentWeatherWind()) {
                                data.add(new Cell(column++, row,
                                        (isShowCurrentWeatherText() ? response.currentCondition : "") +
                                        (isShowCurrentWeatherText() && isShowCurrentWeatherWind() ? "<br>" : "") +
                                        (isShowCurrentWeatherWind() ? response.currentWind : "")
                                        , false));
                            }
                            if (isShowCurrentWeatherImage()) {
                                data.add(new Cell(column++, row, "<img src=\"" + response.currentImg + "\">", false));
                            }
                            if (isShowCurrentWeatherTemperature()) {
                                data.add(new Cell(column++, row, "<big><b>" + (getTemperatureScale().equals(TEMP_CELSIUS) ? response.currentTempC + " °C" : response.currentTempF + " °F") + "</b></big>", false));
                            }
                            if (isShowForecastWeatherText()) {
                                data.add(new Cell(column++, row, International.getMessage("Vorhersage für {day}", checkIfToday(response.forecastDay)) + ":<br>"
                                        + response.forecastCondifion, false));
                            }
                            if (isShowForecastWeatherImage()) {
                                data.add(new Cell(column++, row, "<img src=\"" + response.forecastIcon + "\">", false));
                            }
                            if (isShowForecastWeatherTemperature()) {
                                data.add(new Cell(column++, row, International.getString("Min") + ": " + response.forecastLow + " °" + "<br>"
                                        + International.getString("Max") + ": " + response.forecastHigh + " °", false));
                            }
                        }

                        if (getLayout().equals(LAYOUT_VERTICAL)) {
                            if (isShowWeatherLocation()) {
                                data.add(new Cell(column, row++, response.city + ":", true));
                            }
                            if (isShowCurrentWeatherText() || isShowCurrentWeatherWind()) {
                                data.add(new Cell(column, row++,
                                        (isShowCurrentWeatherText() ? response.currentCondition : "") +
                                        (isShowCurrentWeatherText() && isShowCurrentWeatherWind() ? "<br>" : "") +
                                        (isShowCurrentWeatherWind() ? response.currentWind : "")
                                        , true));
                            }
                            if (isShowCurrentWeatherImage()) {
                                data.add(new Cell(column, row++, "<img src=\"" + response.currentImg + "\">", true));
                            }
                            if (isShowCurrentWeatherTemperature()) {
                                data.add(new Cell(column, row++, "<big><b>" + (getTemperatureScale().equals(TEMP_CELSIUS) ? response.currentTempC + " °C" : response.currentTempF + " °F") + "</b></big>", true));
                            }
                            if (isShowForecastWeatherText()) {
                                data.add(new Cell(column, row++, International.getMessage("Vorhersage für {day}", checkIfToday(response.forecastDay)) + ":<br>"
                                        + response.forecastCondifion, true));
                            }
                            if (isShowForecastWeatherImage()) {
                                data.add(new Cell(column, row++, "<img src=\"" + response.forecastIcon + "\">", true));
                            }
                            if (isShowForecastWeatherTemperature()) {
                                data.add(new Cell(column, row++, International.getString("Min") + ": " + response.forecastLow + " °" + "<br>"
                                        + International.getString("Max") + ": " + response.forecastHigh + " °", true));
                            }
                        }

                        if (getLayout().equals(LAYOUT_COMPACT)) {
                            if (isShowWeatherLocation()) {
                                data.add(new Cell(column, row++, response.city + ":", true));
                            }
                            if ((isShowCurrentWeatherText() || isShowForecastWeatherText())) {
                                data.add(new Cell(column++, row,
                                        (isShowCurrentWeatherText() ? response.currentCondition : "") +
                                        (isShowCurrentWeatherText() && isShowCurrentWeatherWind() ? "<br>" : "") +
                                        (isShowCurrentWeatherWind() ? response.currentWind : "")
                                        , false));
                            }
                            if (isShowCurrentWeatherImage()) {
                                data.add(new Cell(column++, row, "<img src=\"" + response.currentImg + "\">", false));
                            }
                            if (isShowCurrentWeatherTemperature()) {
                                data.add(new Cell(column++, row, "<big><b>" + (getTemperatureScale().equals(TEMP_CELSIUS) ? response.currentTempC + " °C" : response.currentTempF + " °F") + "</b></big>", false));
                            }

                            if (isShowCurrentWeatherText() || isShowForecastWeatherText()) {
                                row++;
                                column = 1;
                            }

                            if (isShowForecastWeatherText()) {
                                data.add(new Cell(column++, row, International.getMessage("Vorhersage für {day}", checkIfToday(response.forecastDay)) + ":<br>"
                                        + response.forecastCondifion, false));
                            }
                            if (isShowForecastWeatherImage()) {
                                data.add(new Cell(column++, row, "<img src=\"" + response.forecastIcon + "\">", false));
                            }
                            if (isShowForecastWeatherTemperature()) {
                                data.add(new Cell(column++, row, International.getString("Min") + ": " + response.forecastLow + " °" + "<br>"
                                        + International.getString("Max") + ": " + response.forecastHigh + " °", false));
                            }

                            if (isShowCurrentWeatherWind() && !isShowCurrentWeatherText() && !isShowForecastWeatherText()) {
                                row++;
                                column = 1;
                                data.add(new Cell(column, row++, response.currentWind, true));
                            }
                        }
                        //if (weatherApi == WeatherApi.yahoo) {
                        //    data.add(new Cell(0, row++, "<font size=\"1\">Yahoo! Weather.</font>", false, true));
                        //}

                        weatherError = false;
                    } catch (Exception e) {
                        String firstResultLine = null;
                        try {
                            in.reset();
                            BufferedReader buf = new BufferedReader(new InputStreamReader(in));
                            firstResultLine = buf.readLine();
                        } catch (Exception eignore) {
                        }
                        // log as WARNING for first weatherError; log as DEBUG for every next weatherError
                        String errMsg = e.toString();
                        if (availBytes == 0) {
                            errMsg = "no data received from " + url;
                        }
                        if (firstResultLine != null && firstResultLine.length() > 0) {
                            if (firstResultLine.length() > 1000) {
                                firstResultLine = firstResultLine.substring(0, 1000);
                            }
                            if (firstResultLine.indexOf("We're sorry") > 0) {
                                weatherSlowDownUpdateFactor = 3;
                                weatherCnt = 0;
                            }
                            errMsg = "invalid response received from " + url + ": " + firstResultLine;
                        }
                        Logger.log( (weatherError ? Logger.DEBUG : Logger.WARNING), Logger.MSG_WARN_WEATHERUPDATEFAILED,
                                International.getString("Wetterdaten konnten nicht geladen werden") + ": " + errMsg);
                        Logger.logdebug(e);
                        weatherError = true;
                    } catch (NoClassDefFoundError e) {
                        if (!weatherError) {
                            Logger.log(Logger.WARNING, Logger.MSG_CORE_MISSINGPLUGIN,
                                    International.getString("Fehlendes Plugin") + ": " + Plugins.PLUGIN_WEATHER + " - "
                                    + International.getString("Die Wetterdaten können nicht angezeigt werden.") + " "
                                    + International.getMessage("Bitte lade das fehlende Plugin unter der Adresse {url} herunter.", Daten.pluginWebpage));
                        }
                        weatherError = true;
                    }
                }

                // Sunrise and Sunset
                if (isShowSunrise()) {
                    String sunriseTime;
                    String sunsetTime;
                    try {
                        String sun[] = SunRiseSet.getSunRiseSet(getLatitude(), getLongitude());
                        sunriseTime = sun[0];
                        sunsetTime = sun[1];
                    } catch (NoClassDefFoundError e) {
                        sunriseTime = "--:--";
                        sunsetTime = "--:--";
                        if (!sunriseError) {
                            Logger.log(Logger.WARNING, Logger.MSG_CORE_MISSINGPLUGIN,
                                    International.getString("Fehlendes Plugin") + ": " + Plugins.PLUGIN_JSUNTIMES + " - "
                                    + International.getString("Die Sonnenaufgangs- und Untergangszeiten können nicht angezeigt werden.") + " "
                                    + International.getMessage("Bitte lade das fehlende Plugin unter der Adresse {url} herunter.", Daten.pluginWebpage));
                        }
                        sunriseError = true;
                    } catch (Exception ee) {
                        Logger.logdebug(ee);
                        sunriseTime = "--:--";
                        sunsetTime = "--:--";
                    }
                    if (getLayout().equals(LAYOUT_HORIZONTAL) || getLayout().equals(LAYOUT_VERTICAL)) {
                        data.add(new Cell(column++, row, "<img src=\"" + saveImage("sunrise.gif", "gif") + "\">&nbsp;" + sunriseTime + "<br>"
                                + "<img src=\"" + saveImage("sunset.gif", "gif") + "\">&nbsp;" + sunsetTime, getLayout().equals(LAYOUT_VERTICAL)));
                    }
                    if (getLayout().equals(LAYOUT_COMPACT)) {
                        if (column > 1) {
                            row++;
                            column = 1;
                        }
                        data.add(new Cell(column++, row, "<img src=\"" + saveImage("sunrise.gif", "gif") + "\">&nbsp;" + sunriseTime + "&nbsp;&nbsp;"
                                + "<img src=\"" + saveImage("sunset.gif", "gif") + "\">&nbsp;" + sunsetTime, true));
                    }
                }

                try {
                    int maxCols = 0;
                    for (int i=0; i<data.size(); i++) {
                        if (data.get(i).col > maxCols) {
                            maxCols = data.get(i).col;
                        }
                    }

                    StringBuffer htmlDoc = new StringBuffer();
                    htmlDoc.append("<html>\n<body bgcolor=\"#" + bgcolor + "\">\n");
                    htmlDoc.append("<table align=\"center\" cellspacing=\"" + (getLayout().equals(LAYOUT_COMPACT) ? 0 : 8) + "\">\n");

                    row = 0;
                    column = 0;
                    for (int i=0; i<data.size(); i++) {
                        Cell cell = data.get(i);
                        if (cell.row > row) {
                            if (row > 0) {
                                htmlDoc.append("</tr>\n");
                            }
                            htmlDoc.append("<tr>");
                            row = cell.row;
                        }
                        boolean lastCellInRow = (i + 1 == data.size() || data.get(i + 1).row > row);
                        int colspan = (lastCellInRow && cell.col < maxCols ? 1 + maxCols - cell.col : 1);
                        htmlDoc.append("<td colspan=\"" + colspan + 
                                "\" align=\"" + (cell.alignCenter ? "center" : (cell.alignRight ? "right" : "left"))  + "\">" +
                                cell.content + "</td>");
                    }
                    htmlDoc.append("</tr>\n");

                    htmlDoc.append("</table>\n");
                    
                    if (getIncludeHtmlText() != null && getIncludeHtmlText().length() > 0) {
                        htmlDoc.append(getIncludeHtmlText());
                    }
                    if (getIncludeHtmlPage() != null && getIncludeHtmlPage().length() > 0) {
                        String includeText = EfaUtil.getBodyFromURL(getIncludeHtmlPage());
                        if (includeText != null && includeText.trim().length() > 0) {
                            htmlDoc.append(includeText.trim());
                        }
                    }
                    
                    htmlDoc.append("</body>\n</html>\n");

                    if (Logger.isTraceOn(Logger.TT_WIDGETS, 9)) {
                        BufferedWriter f = new BufferedWriter(new FileWriter("/tmp/efa_widget.html"));
                        f.write(htmlDoc.toString());
                        f.close();
                    }
                    // System.out.println(htmlDoc.toString());
                    htmlPane.setText(htmlDoc.toString());
                } catch(Exception e) {
                    Logger.logdebug(e);
                }

                try {
                    Thread.sleep(Math.max(updateIntervalInSeconds, 3600) * 1000);
                } catch(Exception e) {
                    Logger.logdebug(e);
                }
            }
        }
        
        public void stopHTML() {
            keepRunning = false;
        }

        private String saveImage(String image, String format) {
            String fname = Daten.efaTmpDirectory + image;
            if (!EfaUtil.canOpenFile(fname)) {
                try {
                    BufferedImage img = javax.imageio.ImageIO.read(MeteoAstroWidget.class.getResource(Daten.IMAGEPATH + image));
                    javax.imageio.ImageIO.write(img, format, new File(fname));
                } catch (Exception e) {
                    Logger.logdebug(e);
                }
            }
            if (Daten.fileSep.equals("\\")) {
                fname = "/" + EfaUtil.replace(fname, "\\", "/", true);
            }
            return "file://" + fname;
        }

        // check whether the day returned from Google is today
        private String checkIfToday(String day) {
            GregorianCalendar cal = new GregorianCalendar();
            switch(cal.get(Calendar.DAY_OF_WEEK)) {
                case Calendar.MONDAY:
                    if (day.equals("Mo.") || day.equals("Mon")) {
                        return International.getString("heute");
                    }
                case Calendar.TUESDAY:
                    if (day.equals("Di.") || day.equals("Tue")) {
                        return International.getString("heute");
                    }
                case Calendar.WEDNESDAY:
                    if (day.equals("Mi.") || day.equals("Wed")) {
                        return International.getString("heute");
                    }
                case Calendar.THURSDAY:
                    if (day.equals("Do.") || day.equals("Thu")) {
                        return International.getString("heute");
                    }
                case Calendar.FRIDAY:
                    if (day.equals("Fr.") || day.equals("Fri")) {
                        return International.getString("heute");
                    }
                case Calendar.SATURDAY:
                    if (day.equals("Sa.") || day.equals("Sat")) {
                        return International.getString("heute");
                    }
                case Calendar.SUNDAY:
                    if (day.equals("So.") || day.equals("Sun")) {
                        return International.getString("heute");
                    }
            }
            return day;
        }

    }

    class Cell {
        public int col,row;
        public String content;
        boolean alignCenter;
        boolean alignRight;
        public Cell(int col, int row, String content, boolean alignCenter) {
            this.col = col;
            this.row = row;
            this.content = content;
            this.alignCenter = alignCenter;
            this.alignRight = false;
        }
        public Cell(int col, int row, String content, boolean alignCenter, boolean alignRight) {
            this.col = col;
            this.row = row;
            this.content = content;
            this.alignCenter = alignCenter;
            this.alignRight = alignRight;
        }
    }

    class WeatherXmlResponse extends DefaultHandler {

        int forecastConditions = 0;
        String city;
        String currentCondition;
        String currentImg;
        String currentWind;
        String currentTempF;
        String currentTempC;
        String forecastDay;
        String forecastLow;
        String forecastHigh;
        String forecastIcon;
        String forecastCondifion;

    }

    class GoogleXmlResponse extends WeatherXmlResponse {

        Stack<String> elementStack = new Stack<String>();

        private String getValue(Attributes atts) {
            String s = atts.getValue("data");
            return EfaUtil.replace(s, " ", "&nbsp;", true);
        }

        public void startElement(String uri, String localName, String qname, Attributes atts) {
            String parentElement = (!elementStack.isEmpty() ? elementStack.peek() : null);
            elementStack.push(localName);

            if (parentElement != null && parentElement.equals("forecast_information")) {
                if (localName.equals("city")) {
                    city = getValue(atts);
                }
            }

            if (parentElement != null && parentElement.equals("current_conditions")) {
                if (localName.equals("condition")) {
                    currentCondition = getValue(atts);
                }
                if (localName.equals("temp_f")) {
                    currentTempF = getValue(atts);
                }
                if (localName.equals("temp_c")) {
                    currentTempC = getValue(atts);
                }
                if (localName.equals("icon")) {
                    currentImg = GOOGLE_URL + getValue(atts);
                }
                if (localName.equals("wind_condition")) {
                    currentWind = getValue(atts);
                }
            }

            if (localName.equals("forecast_conditions")) {
                forecastConditions++;
            }

            if (parentElement != null && parentElement.equals("forecast_conditions") && forecastConditions == 1) {
                if (localName.equals("day_of_week")) {
                    forecastDay = getValue(atts);
                }
                if (localName.equals("low")) {
                    forecastLow = getValue(atts);
                }
                if (localName.equals("high")) {
                    forecastHigh = getValue(atts);
                }
                if (localName.equals("icon")) {
                    forecastIcon = GOOGLE_URL + getValue(atts);
                }
                if (localName.equals("condition")) {
                    forecastCondifion = getValue(atts);
                }
            }

        }

        public void endElement(String uri, String localName, String qname) {
            elementStack.pop();
        }
    }

    class YahooXmlResponse extends WeatherXmlResponse {

        String speedUnit = "km/h";

        public void startElement(String uri, String localName, String qname, Attributes atts) {
            if (qname.equals("yweather:location")) {
                city = atts.getValue("city");
            }
            if (qname.equals("yweather:units")) {
                speedUnit = atts.getValue("speed");
            }
            if (qname.equals("yweather:wind")) {
                String direction = "";
                try {
                    int dir = Integer.parseInt(atts.getValue("direction"));
                    for (int angle = 22; angle <= 22+360; angle += 45) {
                        if (dir < angle) {
                            switch(angle) {
                                case 22:  
                                    direction = International.getString("N", "direction"); break;
                                case 67:  
                                    direction = International.getString("NO", "direction"); break;
                                case 112: 
                                    direction = International.getString("O", "direction"); break;
                                case 157: 
                                    direction = International.getString("SO", "direction"); break;
                                case 202: 
                                    direction = International.getString("S", "direction"); break;
                                case 247: 
                                    direction = International.getString("SW", "direction"); break;
                                case 292: 
                                    direction = International.getString("W", "direction"); break;
                                case 337: 
                                    direction = International.getString("NW", "direction"); break;
                                case 382: 
                                    direction = International.getString("N", "direction"); break;
                            }
                            break;
                        }
                    }
                } catch(Exception eignore) {}
                String speed = atts.getValue("speed");
                if (speed != null && speed.indexOf(".") > 0) {
                    speed = speed.substring(0, speed.indexOf("."));
                }
                currentWind = International.getString("Wind") + ": " + 
                        direction + " " + speed + " " + speedUnit;
            }
            if (qname.equals("yweather:condition")) {
                currentCondition = atts.getValue("text");
                if (getTemperatureScale().equals(TEMP_CELSIUS)) {
                    currentTempC = atts.getValue("temp");
                } else {
                    currentTempF = atts.getValue("temp");
                }
                currentImg = "http://l.yimg.com/a/i/us/we/52/" + atts.getValue("code") + ".gif";
            }

            if (qname.equals("yweather:forecast") && ++forecastConditions == 1) {
                forecastDay = atts.getValue("day");
                forecastLow = atts.getValue("low");
                forecastHigh = atts.getValue("high");
                forecastIcon = "http://l.yimg.com/a/i/us/we/52/" + atts.getValue("code") + ".gif";
                forecastCondifion = atts.getValue("text");

            }

        }

    }


}
