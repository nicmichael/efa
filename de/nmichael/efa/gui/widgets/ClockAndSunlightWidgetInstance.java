package de.nmichael.efa.gui.widgets;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.Plugins;
import de.nmichael.efa.core.items.ItemTypeLongLat;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.gui.BaseDialog;
import de.nmichael.efa.gui.EfaBaseFrame;
import de.nmichael.efa.gui.ImagesAndIcons;
import de.nmichael.efa.gui.NotificationDialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.SunRiseSet;
import de.nmichael.efa.util.TMJ;

public class ClockAndSunlightWidgetInstance extends WidgetInstance implements IWidgetInstance {

    private JPanel mainPanel = new JPanel();
    private JLabel timeLabel = new JLabel();
    private JLabel dateLabel = new JLabel();
    private JPanel sunPanel = new JPanel();
    private JLabel sunriseLabel = new JLabel();
    private JLabel sunsetLabel = new JLabel();
    private PanelUpdater panelUpdater;	
    
    private boolean showSunrise = false;
    private boolean showClock = false;
    private boolean showDate = false;
    private ItemTypeLongLat latitude;
    private ItemTypeLongLat longitude;
    private boolean warnDarkness = false;
    private int warnTimeBeforeSunset;
    private int warnTimeAfterSunset;
    private int warnTimeBeforeSunrise;
    private String warnTextDarkSoon;
    private String warnTextDarkNow;
    private String layout;
	
	
    @Override
    public void construct() {

       	/* Layout, created by plain java
    	 
        12:23
   20.03.2024 
(*) 08:20 (o) 18:23

	 */  

	mainPanel = new JPanel();
	mainPanel.setLayout(new GridBagLayout());
	mainPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
	sunPanel.setLayout(new GridBagLayout());
	sunPanel.setOpaque(false);
	Boolean isHorzLayout=getLayout().equals(ClockAndSunlightWidget.LAYOUT_HORIZONTAL);
	
	if (isHorzLayout) {
		/*      
		     20:23     (*) 08:00
		  20.12.2024   (o) 18:00
		  
		    or
		    
		    20:23      (*) 08:00
		               (o) 18:00
		    
		    or
		    
		  20.12.2024   (*) 08:00
		               (o) 18:00
		               
		*/
		
		sunPanel.add(sunriseLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		sunPanel.add(sunsetLabel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(4,0,0,0), 0, 0)); 

		if (isShowClock() && isShowDate()) {
			//date and time in separate rows
			if (isShowSunrise()) {
				mainPanel.add(timeLabel,   new GridBagConstraints(0,0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0));
				mainPanel.add(dateLabel,   new GridBagConstraints(0,1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0));
			} else {
				mainPanel.add(timeLabel,   new GridBagConstraints(0,0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
				mainPanel.add(dateLabel,   new GridBagConstraints(1,0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 40, 0, 0), 0, 0));
			}
		} else if (isShowClock() && ! isShowDate()) {
			//only add timelabel, span over two rows
			mainPanel.add(timeLabel,   new GridBagConstraints(0,0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0));
		} else if (!isShowClock() && isShowDate()) {
			mainPanel.add(dateLabel,   new GridBagConstraints(0,0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 0, 0, 0), 0, 0));
		}

		mainPanel.add(sunPanel,    new GridBagConstraints(1,0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 40, 0, 0), 0, 0));

	} else {
		sunPanel.add(sunriseLabel, new GridBagConstraints(0,0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,4), 0, 0));
		sunPanel.add(sunsetLabel,  new GridBagConstraints(1,0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,4), 0, 0));  
		
		mainPanel.add(timeLabel,   new GridBagConstraints(0,0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
		mainPanel.add(dateLabel,   new GridBagConstraints(0,1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0));
		mainPanel.add(sunPanel,    new GridBagConstraints(0,2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0));
	}

	sunriseLabel.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_SUNRISE));
	sunriseLabel.setIconTextGap(2);
	sunsetLabel.setIcon(ImagesAndIcons.getIcon(ImagesAndIcons.IMAGE_SUNSET));
	sunsetLabel.setIconTextGap(2);
	
	//Time label shall be bold and huge
	timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
	dateLabel.setHorizontalAlignment(SwingConstants.CENTER);    	
    	
    	try {
            panelUpdater = new PanelUpdater(mainPanel);
            panelUpdater.start();
            
        } catch(Exception e) {
            Logger.log(e);
        }
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public void stop() {
        try {
        	// stopHTML also lets the thread die, and efaBths is responsible to set up a new thread.
            panelUpdater.stopRunning();
        } catch(Exception eignore) {
            // nothing to do, might not be initialized
        }
    }

    public void runWidgetWarnings(int mode, boolean actionBegin, LogbookRecord r) {
        try {
            if ((mode == EfaBaseFrame.MODE_BOATHOUSE_START ||
                 mode == EfaBaseFrame.MODE_BOATHOUSE_START_CORRECT ||
                 mode == EfaBaseFrame.MODE_BOATHOUSE_START_MULTISESSION) && !actionBegin &&
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

    public String getLayout() {
		return layout;
	}

	public void setLayout(String layout) {
		this.layout = layout;
	}

	public String getWarnTextDarkNow() {
		return warnTextDarkNow;
	}

	public void setWarnTextDarkNow(String warnTextDarkNow) {
		this.warnTextDarkNow = warnTextDarkNow;
	}

	public String getWarnTextDarkSoon() {
		return warnTextDarkSoon;
	}

	public void setWarnTextDarkSoon(String warnTextDarkSoon) {
		this.warnTextDarkSoon = warnTextDarkSoon;
	}

	public int getWarnTimeBeforeSunrise() {
		return warnTimeBeforeSunrise;
	}

	public void setWarnTimeBeforeSunrise(int warnTimeBeforeSunrise) {
		this.warnTimeBeforeSunrise = warnTimeBeforeSunrise;
	}

	public int getWarnTimeAfterSunset() {
		return warnTimeAfterSunset;
	}

	public void setWarnTimeAfterSunset(int warnTimeAfterSunset) {
		this.warnTimeAfterSunset = warnTimeAfterSunset;
	}

	public int getWarnTimeBeforeSunset() {
		return warnTimeBeforeSunset;
	}

	public void setWarnTimeBeforeSunset(int warnTimeBeforeSunset) {
		this.warnTimeBeforeSunset = warnTimeBeforeSunset;
	}

	public boolean isWarnDarkness() {
		return warnDarkness;
	}

	public void setWarnDarkness(boolean warnDarkness) {
		this.warnDarkness = warnDarkness;
	}

	public ItemTypeLongLat getLongitude() {
		return longitude;
	}

	public void setLongitude(ItemTypeLongLat longitude) {
		this.longitude = longitude;
	}

	public ItemTypeLongLat getLatitude() {
		return latitude;
	}

	public void setLatitude(ItemTypeLongLat latitude) {
		this.latitude = latitude;
	}

	public boolean isShowDate() {
		return showDate;
	}

	public void setShowDate(boolean showDate) {
		this.showDate = showDate;
	}

	public boolean isShowClock() {
		return showClock;
	}

	public void setShowClock(boolean showClock) {
		this.showClock = showClock;
	}

	public boolean isShowSunrise() {
		return showSunrise;
	}

	public void setShowSunrise(boolean showSunrise) {
		this.showSunrise = showSunrise;
	}

	class PanelUpdater extends Thread {

        volatile boolean keepRunning = true;
        private boolean sunriseError = false;
        private JPanel panel;

        public PanelUpdater(JPanel thePanel) {
        	this.panel=thePanel;
        }

        public void run() {
        	this.setName("ClockAndSunlightWidget.PanelUpdater"+" "+DataTypeTime.now().toString());
            String clockValue=null;
            String dateValue=null;
            String sunriseValue=null;
            String sunsetValue=null;
            
            while (keepRunning) {
            	
            	try {
	            	clockValue= (isShowClock() ? EfaUtil.getCurrentTimeStampHHMM().toString() : null);
	            	dateValue=(isShowDate() ? EfaUtil.getCurrentTimeStampInDateFormat().toString() : null);
	
	                // Sunrise and Sunset
	                if (isShowSunrise()) {
	                    try {
	                        String sun[] = SunRiseSet.getSunRiseSet(getLatitude(), getLongitude());
	                        sunriseValue = sun[0];
	                        sunsetValue = sun[1];
	                    } catch (NoClassDefFoundError e) {
	                        sunriseValue = "--:--";
	                        sunsetValue = "--:--";
	                        if (!sunriseError) {
	                            Logger.log(Logger.WARNING, Logger.MSG_CORE_MISSINGPLUGIN,
	                                    International.getString("Fehlendes Plugin") + ": " + Plugins.PLUGIN_JSUNTIMES + " - "
	                                    + International.getString("Die Sonnenaufgangs- und Untergangszeiten können nicht angezeigt werden.") + " "
	                                    + International.getMessage("Bitte lade das fehlende Plugin unter der Adresse {url} herunter.", Daten.pluginWebpage));
	                        }
	                        sunriseError = true;
	                    } catch (Exception ee) {
	                        Logger.logdebug(ee);
	                        sunriseValue = "--:--";
	                        sunsetValue = "--:--";
	                    }
	                }
	            	
	            	//Use invokelater as swing threadsafe ways
	            	SwingUtilities.invokeLater(new UpdateClockAstroRunner(this.panel, clockValue, dateValue, sunriseValue, sunsetValue));
	
	            	
	            	//wait until next full minute plus one sec. this is more accurate than just waiting 60.000 msec
	            	//from a random offset.
	           		long waitTime=EfaUtil.getMilliSecondsToFullMinute()+1000;
	                Thread.sleep(waitTime);

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

    private class UpdateClockAstroRunner implements Runnable {
        
    	private JPanel panel=null;
    	private String clockValue = null;
    	private String dateValue = null;
    	private String sunriseValue = null;
    	private String sunsetValue = null;
    	
    	public UpdateClockAstroRunner(JPanel targetPanel, String sclockValue, String sdateValue, String ssunriseValue, String ssunsetValue) {
    		panel = targetPanel;
    		this.clockValue = sclockValue;
    		this.dateValue = sdateValue;
    		this.sunriseValue = ssunriseValue;
    		this.sunsetValue = ssunsetValue;
    	}
    	
    	public void run() {
    		try {
	    		timeLabel.setVisible(clockValue!=null);
	    		dateLabel.setVisible(dateValue!=null);
	    		sunPanel.setVisible(sunriseValue!=null);
	    		
	    		timeLabel.setText(clockValue);
	    		dateLabel.setText(dateValue);
	    		sunriseLabel.setText(sunriseValue);
	    		sunsetLabel.setText(sunsetValue);
	    		
    			mainPanel.setFont(mainPanel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()));	    		
	    		timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD));
	    		if (Daten.efaConfig != null) {
	    			if (dateValue==null) {
	    				timeLabel.setFont(timeLabel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()+10));
	    			} else {
	    				timeLabel.setFont(timeLabel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()+6));
	    			}
	    			if (clockValue==null) {
	    				dateLabel.setFont(dateLabel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()+6));
	    				dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD));
	    			} else {
	    				if (isShowSunrise()) {
		    				dateLabel.setFont(dateLabel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()));
	    				} else {
		    				dateLabel.setFont(dateLabel.getFont().deriveFont((float)Daten.efaConfig.getValueEfaDirekt_BthsFontSize()+6));
	    				}	    					
	    				dateLabel.setFont(dateLabel.getFont().deriveFont(Font.PLAIN));
		    				
	    			}


	    		}
	
	    		panel.revalidate();
	    		panel.repaint();
	    		
    		} catch (Exception e){
    			Logger.log(e);
    		}
    	}
    }

}
