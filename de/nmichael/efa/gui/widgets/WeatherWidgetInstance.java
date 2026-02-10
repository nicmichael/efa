package de.nmichael.efa.gui.widgets;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.util.RoundedBorder;
import de.nmichael.efa.gui.util.RoundedPanel;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class WeatherWidgetInstance extends WidgetInstance implements IWidgetInstance {
	private volatile JPanel mainPanel = new JPanel();
	private RoundedPanel roundPanel = new RoundedPanel();
	private WeatherUpdater weatherUpdater;
	
	private String caption;
	private String longitude;
	private String latitude;
	private String layout;
	private String tempScale;
	private String speedScale;
	private String source;
	private String popupExecCommand;
	private String htmlPopupURL;
	private int htmlPopupWidth;
	private int htmlPopupHeight;
	private int updateInterval;
	
	private Color standardBackground;
	private Color standardForeground;
	private Color standardHeaderBackground;
	private Color standardHeaderForeground;
	
	private Color errorBackground;
	private Color errorForeground;
	private Color errorHeaderBackground;
	private Color errorHeaderForeground;
	
	@Override
	public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
		// Nothing to do here
	}

	@Override
	public void stop() {
        try {
        	// stopHTML also lets the thread die, and efaBths is responsible to set up a new thread.
        	weatherUpdater.stopRunning();
        } catch(Exception eignore) {
            // nothing to do, might not be initialized
        }
	}

	@Override
	public JComponent getComponent() {
		// TODO Auto-generated method stub
		return mainPanel;
	}
	
	@Override
	public void construct() {
		// we are in Swing Main Thread here, so we don't need to use swingutils.invokelater...
		mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		mainPanel.setName("WeatherWidget-MainPanel");
		roundPanel = new RoundedPanel();
		
		roundPanel.setLayout(new GridBagLayout());
		roundPanel.setBackground(getStandardBackground());
		roundPanel.setForeground(getStandardForeground());
		roundPanel.setBorder(new RoundedBorder(getStandardForeground()));
		roundPanel.setName("WeatherWidget-RoundPanel");
		//grow in horizontal width
		mainPanel.add(roundPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		addInfoPanel();
		roundPanel.setMinimumSize(new Dimension(240, 120));
		roundPanel.revalidate();
		
        // show a hand cursor on sunrise/sunset/weather widget only if an html popup is set up.
        if (getHtmlPopupURL() != null && getHtmlPopupURL().length() > 0) {
        	mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        
        // HTML-Popup
        roundPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (getHtmlPopupURL() != null && getHtmlPopupURL().length() > 0) {
                    new HtmlPopupDialog(getCaption(),
                    		getHtmlPopupURL(),
                            getPopupExecCommand(),
                            getHtmlPopupWidth(), getHtmlPopupHeight(), 60).showDialog();
                }
            }
        });

	   	try {
	   		weatherUpdater = new WeatherUpdater(roundPanel, this, this.getCaption());
	   		weatherUpdater.start();
            
        } catch(Exception e) {
            Logger.log(e);
        }		
	}

	private void addInfoPanel() {
		JTextArea infoLabel= new JTextArea();
		infoLabel.setFont(
				mainPanel.getFont().deriveFont((float) (Daten.efaConfig.getValueEfaDirekt_BthsFontSize())));
		infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD));
		infoLabel.setText(International.getString("Ermittle Wetterdaten..."));
		infoLabel.setLineWrap(true);
		infoLabel.setWrapStyleWord(true);
		infoLabel.setOpaque(false);
		infoLabel.setEditable(false);
		
		JPanel titlePanel = WeatherRenderer.getLocationHeader(this.getCaption(), !this.getHtmlPopupURL().isEmpty(), this);
		titlePanel.setBackground(getStandardHeaderBackground());
		titlePanel.setForeground(getStandardHeaderForeground());
		
		// Build the main panel view

		roundPanel.add(titlePanel, new GridBagConstraints(0, 0, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));	
		
		roundPanel.add(infoLabel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(2, 4, 2, 4), 0, 0));
	}

	public String getTempLabel(boolean withUnit) {
		if (!withUnit) {
			return "°";
		} else {
			return (getTempScale().equals(WeatherWidget.TEMP_CELSIUS) ? "°C" : "°F");
		}
	}	

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public int getHtmlPopupHeight() {
		return htmlPopupHeight;
	}

	public void setHtmlPopupHeight(int htmlPopupHeight) {
		this.htmlPopupHeight = htmlPopupHeight;
	}

	public int getHtmlPopupWidth() {
		return htmlPopupWidth;
	}

	public void setHtmlPopupWidth(int htmlPopupWidth) {
		this.htmlPopupWidth = htmlPopupWidth;
	}

	public String getHtmlPopupURL() {
		return htmlPopupURL;
	}

	public void setHtmlPopupURL(String htmlPopupURL) {
		this.htmlPopupURL = htmlPopupURL;
	}

	public String getPopupExecCommand() {
		return popupExecCommand;
	}

	public void setPopupExecCommand(String popupExecCommand) {
		this.popupExecCommand = popupExecCommand;
	}

	public String getSpeedScale() {
		return speedScale;
	}

	public void setSpeedScale(String speedScale) {
		this.speedScale = speedScale;
	}

	public String getTempScale() {
		return tempScale;
	}

	public void setTempScale(String tempScale) {
		this.tempScale = tempScale;
	}

	public String getLayout() {
		return layout;
	}

	public void setLayout(String layout) {
		this.layout = layout;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latiude) {
		this.latitude = latiude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}

	public Color getStandardBackground() {
		return standardBackground;
	}

	public void setStandardBackground(Color standardBackground) {
		this.standardBackground = standardBackground;
	}

	public Color getStandardForeground() {
		return standardForeground;
	}

	public void setStandardForeground(Color standardForeground) {
		this.standardForeground = standardForeground;
	}

	public Color getStandardHeaderBackground() {
		return standardHeaderBackground;
	}

	public void setStandardHeaderBackground(Color standardHeaderBackground) {
		this.standardHeaderBackground = standardHeaderBackground;
	}

	public Color getStandardHeaderForeground() {
		return standardHeaderForeground;
	}

	public void setStandardHeaderForeground(Color standardHeaderForeground) {
		this.standardHeaderForeground = standardHeaderForeground;
	}

	public Color getErrorBackground() {
		return errorBackground;
	}

	public void setErrorBackground(Color errorBackground) {
		this.errorBackground = errorBackground;
	}

	public Color getErrorForeground() {
		return errorForeground;
	}

	public void setErrorForeground(Color errorForeground) {
		this.errorForeground = errorForeground;
	}

	public Color getErrorHeaderBackground() {
		return errorHeaderBackground;
	}

	public void setErrorHeaderBackground(Color errorHeaderBackground) {
		this.errorHeaderBackground = errorHeaderBackground;
	}

	public Color getErrorHeaderForeground() {
		return errorHeaderForeground;
	}

	public void setErrorHeaderForeground(Color errorHeaderForeground) {
		this.errorHeaderForeground = errorHeaderForeground;
	}

	/**
	 * The WeatherUpdate obtains Weather Data in a separate thread, so that
	 * the time for getting weather data does not affect the main thread,
	 * and efaBoathouse is still ready for interaction with the user.
	 */
   class WeatherUpdater extends Thread {

        volatile boolean keepRunning = true;
        private JPanel panel;
        private WeatherWidgetInstance ww = null;
        private WeatherDataForeCast wdf = null;
        private long lastWeatherUpdate = 0;
        private String theName="";
        
        public WeatherUpdater(JPanel thePanel, WeatherWidgetInstance ww, String theName) {
        	this.panel=thePanel;
        	this.ww = ww;
        	this.theName=theName;
        }

        private boolean needsToUpdateWeather() {
        	return (this.wdf == null || (System.currentTimeMillis() >= lastWeatherUpdate+(ww.getUpdateInterval()*1000)));
        }
        
        public void run() {
        	this.setName("WeatherWidget.WeatherUpdater"+" "+theName+" "+DataTypeTime.now().toString());
            
            while (keepRunning) {
            	
            	try {
            		//WeatherDataCache handles itself if the interval for loading new weather data is exceeded.
           			wdf = WeatherDataCache.getInstance().getWeatherData(ww.getSource(), ww.getLongitude(), ww.getLatitude());
	            	
	            	//Use invokelater as swing threadsafe ways
	            	SwingUtilities.invokeLater(new UpdateWeatherRunner(this.panel, wdf, ww));
	
	            	// check every minute if we need to update Weather data.
	            	// this also implements that the panel gets a refresh with the already downloaded WeatherData 
	            	// every minute. This is possibly neccessary when using weather forecast which has data for multiple timecodes a day.
	                Thread.sleep(EfaUtil.getMilliSecondsToFullMinute()+1000);

            	} catch (InterruptedException e) {
                	//This is when the thread gets interrupted when it is sleeping.
                	EfaUtil.foo();            
                } catch (Exception e) {
                	Throwable t = e.getCause();
                	if (t.getClass().getName().equalsIgnoreCase("java.lang.InterruptedException")) {
                		EfaUtil.foo();
                	} else {
                		Logger.logdebug(e);
                	}
                }
	                
            }
        }
        
        public synchronized void stopRunning() {
            keepRunning = false;
            interrupt(); // wake up thread
        }

    }

    private class UpdateWeatherRunner implements Runnable {
        
    	private JPanel uwrPanel=null;
    	private JPanel uwrInnerPanel=null;
    	private WeatherDataForeCast uwrWdf=null;
    	private WeatherWidgetInstance uwrWW=null;
    	
    	public UpdateWeatherRunner(JPanel targetPanel, WeatherDataForeCast wdf, WeatherWidgetInstance ww) {
    		this.uwrPanel = targetPanel;
    		this.uwrWdf = wdf;
    		this.uwrWW = ww;
    	}
    	
    	public void run() {
    		try {

    			getInnerPannel();
    			
    			uwrPanel.removeAll();
    			uwrPanel.add(uwrInnerPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
    					GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    			uwrPanel.revalidate();
    			uwrPanel.updateUI();
    			uwrPanel.invalidate();
    			
    		} catch (Exception e){
    			Logger.log(e);
    		}
    	}
    	
    	private void getInnerPannel() {
			
			uwrInnerPanel = new JPanel();
			uwrInnerPanel.setLayout(new GridBagLayout());
			uwrInnerPanel.setBackground(uwrWW.getStandardBackground());
			uwrInnerPanel.setForeground(uwrWW.getStandardForeground());
			uwrInnerPanel.setBorder(BorderFactory.createEmptyBorder());
			uwrInnerPanel.setName("WeatherWidget-InnerPanel");
			uwrInnerPanel.setOpaque(false);
			
    		if (uwrWdf != null && uwrWdf.getStatus() == true) {
        		if (getLayout().equalsIgnoreCase(WeatherWidget.WEATHER_LAYOUT_CURRENT_CLASSIC)) {
					WeatherRendererCurrentClassic.renderWeather(uwrWdf, uwrInnerPanel, uwrWW);
				} else if (getLayout().equalsIgnoreCase(WeatherWidget.WEATHER_LAYOUT_CURRENT_WIND)) {
					WeatherRendererCurrentWind.renderWeather(uwrWdf, uwrInnerPanel, uwrWW);
				} else if (getLayout().equalsIgnoreCase(WeatherWidget.WEATHER_LAYOUT_CURRENT_UVINDEX)) {
					WeatherRendererCurrentUVIndex.renderWeather(uwrWdf, uwrInnerPanel, uwrWW);
				} else if (getLayout().equalsIgnoreCase(WeatherWidget.WEATHER_LAYOUT_FORECASTSIMPLE)){
					WeatherRendererForeCastSimple.renderWeather(uwrWdf, uwrInnerPanel, uwrWW);
				} else {
					WeatherRendererForeCastComplex.renderWeather(uwrWdf, uwrInnerPanel, uwrWW);
				}
			} else {
        		WeatherRendererError.renderWeather(uwrWdf, uwrInnerPanel, uwrWW);
			}
    	}
    }

	
}
