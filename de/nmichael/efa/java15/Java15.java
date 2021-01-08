/**
 * Title: efa - elektronisches Fahrtenbuch für Ruderer Copyright: Copyright (c)
 * 2001-2011 by Nicolas Michael Website: http://efa.nmichael.de/ License: GNU
 * General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.java15;

import java.awt.Window;
import java.lang.management.*;
import javax.management.*;
import java.util.*;

// @i18n complete
class OOMEListener implements javax.management.NotificationListener {

    private de.nmichael.efa.gui.EfaBoathouseFrame frame;

    public OOMEListener(de.nmichael.efa.gui.EfaBoathouseFrame frame) {
        this.frame = frame;
    }

    public void handleNotification(Notification notification, Object handback) {
        de.nmichael.efa.Daten.DONT_SAVE_ANY_FILES_DUE_TO_OOME = true;
    }
}

public class Java15 {

    private static boolean memoryWarningLow = false;

    public static boolean setAlwaysOnTop(Window frame, boolean alwaysOnTop) {
        try {
            frame.setAlwaysOnTop(alwaysOnTop);
        } catch (NoSuchMethodError e) {
            return false;
        }
        return true;
    }

    public static boolean setEditorPaneAutoFormSubmissionFalse(javax.swing.JEditorPane editorPane) {
        try {
            MyPropertyChangeListener propertyChangeListener = new MyPropertyChangeListener();
            propertyChangeListener.setEditorPane(editorPane);
            editorPane.addPropertyChangeListener("editorKit", propertyChangeListener);
        } catch (NoSuchMethodError e) {
            return false;
        }
        return true;
    }

    public static boolean editorPaneHandlePostEvent(javax.swing.JEditorPane editorPane, javax.swing.event.HyperlinkEvent event) {
        try {
            javax.swing.text.html.FormSubmitEvent fevent = (javax.swing.text.html.FormSubmitEvent) event;


            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) (fevent.getURL()).openConnection();
            conn.setRequestMethod("POST");
            conn.setAllowUserInteraction(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-length", Integer.toString(fevent.getData().length()));
            java.io.DataOutputStream out = new java.io.DataOutputStream(conn.getOutputStream());
            out.writeBytes(fevent.getData());
            out.flush();
            out.close();
            conn.disconnect();
        } catch (Exception e) {
            return false;
        } catch (NoSuchMethodError e) {
            return false;
        }
        return true;
    }

    private static String printMemUsage(MemoryUsage usage) {
        if (usage != null) {
            return usage.getUsed() + "/" + usage.getMax() + "(" + (usage.getUsed() * 100 / usage.getMax()) + "%)";
        } else {
            return "null";
        }
    }

    public static boolean isMemoryLow(int percentageHigh, int percentageRelativelyHigh) {
        boolean memoryLow = false;
        try {
            try {
                List pools = ManagementFactory.getMemoryPoolMXBeans();
                for (int i = 0; i < pools.size(); i++) {
                    MemoryPoolMXBean pool = (MemoryPoolMXBean) pools.get(i);
                    if (de.nmichael.efa.util.Logger.isTraceOn(de.nmichael.efa.util.Logger.TT_MEMORYSUPERVISION)) {
                        de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.DEBUG,
                                de.nmichael.efa.util.Logger.MSG_DEBUG_MEMORYSUPERVISOR,
                                "MemorySupervisor: Memory Pool: " + pool.getName());
                        de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.DEBUG,
                                de.nmichael.efa.util.Logger.MSG_DEBUG_MEMORYSUPERVISOR,
                                "MemorySupervisor:   Current Usage      : " + printMemUsage(pool.getUsage()));
                        de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.DEBUG,
                                de.nmichael.efa.util.Logger.MSG_DEBUG_MEMORYSUPERVISOR,
                                "MemorySupervisor:   Usage after last GC: " + printMemUsage(pool.getCollectionUsage()));
                        de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.DEBUG,
                                de.nmichael.efa.util.Logger.MSG_DEBUG_MEMORYSUPERVISOR,
                                "MemorySupervisor:   Peak Usage         : " + printMemUsage(pool.getPeakUsage()));
                    }

                    if (pool.getName().toLowerCase().indexOf("tenured") >= 0
                            || pool.getName().toLowerCase().indexOf("old") >= 0) {
                        MemoryUsage usage = pool.getCollectionUsage();
                        if (usage.getMax() > 0 && (usage.getUsed() * 100) / usage.getMax() >= percentageHigh) {
                            de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.WARNING,
                                    de.nmichael.efa.util.Logger.MSG_WARN_MEMORYSUPERVISOR,
                                    "MemorySupervisor: Memory Pool: " + pool.getName());
                            de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.WARNING,
                                    de.nmichael.efa.util.Logger.MSG_WARN_MEMORYSUPERVISOR,
                                    "MemorySupervisor:   Current Usage      : " + printMemUsage(pool.getUsage()));
                            de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.WARNING,
                                    de.nmichael.efa.util.Logger.MSG_WARN_MEMORYSUPERVISOR,
                                    "MemorySupervisor:   Usage after last GC: " + printMemUsage(pool.getCollectionUsage()));
                            de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.WARNING,
                                    de.nmichael.efa.util.Logger.MSG_WARN_MEMORYSUPERVISOR,
                                    "MemorySupervisor:   Peak Usage         : " + printMemUsage(pool.getPeakUsage()));
                            memoryLow = true;
                            memoryWarningLow = true;
                        } else if (usage.getMax() > 0 && (usage.getUsed() * 100) / usage.getMax() >= percentageRelativelyHigh && !memoryWarningLow) {
                            de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.WARNING,
                                    de.nmichael.efa.util.Logger.MSG_WARN_MEMORYSUPERVISOR,
                                    "MemorySupervisor: "
                                    + de.nmichael.efa.util.International.getString("Der aktuelle Speicherverbrauch ist relativ hoch."));
                            de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.WARNING,
                                    de.nmichael.efa.util.Logger.MSG_WARN_MEMORYSUPERVISOR,
                                    "MemorySupervisor: Memory Pool: " + pool.getName());
                            de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.WARNING,
                                    de.nmichael.efa.util.Logger.MSG_WARN_MEMORYSUPERVISOR,
                                    "MemorySupervisor:   Current Usage      : " + printMemUsage(pool.getUsage()));
                            de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.WARNING,
                                    de.nmichael.efa.util.Logger.MSG_WARN_MEMORYSUPERVISOR,
                                    "MemorySupervisor:   Usage after last GC: " + printMemUsage(pool.getCollectionUsage()));
                            de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.WARNING,
                                    de.nmichael.efa.util.Logger.MSG_WARN_MEMORYSUPERVISOR,
                                    "MemorySupervisor:   Peak Usage         : " + printMemUsage(pool.getPeakUsage()));
                            de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.WARNING,
                                    de.nmichael.efa.util.Logger.MSG_WARN_MEMORYSUPERVISOR,
                                    "MemorySupervisor: "
                                    + de.nmichael.efa.util.International.getString("Der efa zur Verfügung stehende Arbeitsspeicher kann durch eine Konfigurationsdatei hochgesetzt werden (siehe efa-FAQ)."));
                            memoryWarningLow = true;
                        } else {
                            if (usage.getMax() > 0 && (usage.getUsed() * 100) / usage.getMax() < percentageRelativelyHigh && memoryWarningLow) {
                                de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.INFO,
                                        de.nmichael.efa.util.Logger.MSG_EVT_MEMORYSUPERVISOR,
                                        "MemorySupervisor: "
                                        + de.nmichael.efa.util.International.getString("Der aktuelle Speicherverbrauch ist wieder im unkritischen Bereich."));
                                de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.INFO,
                                        de.nmichael.efa.util.Logger.MSG_EVT_MEMORYSUPERVISOR,
                                        "MemorySupervisor: Memory Pool: " + pool.getName());
                                de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.INFO,
                                        de.nmichael.efa.util.Logger.MSG_EVT_MEMORYSUPERVISOR,
                                        "MemorySupervisor:   Current Usage      : " + printMemUsage(pool.getUsage()));
                                de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.INFO,
                                        de.nmichael.efa.util.Logger.MSG_EVT_MEMORYSUPERVISOR,
                                        "MemorySupervisor:   Usage after last GC: " + printMemUsage(pool.getCollectionUsage()));
                                de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.INFO,
                                        de.nmichael.efa.util.Logger.MSG_EVT_MEMORYSUPERVISOR,
                                        "MemorySupervisor:   Peak Usage         : " + printMemUsage(pool.getPeakUsage()));
                                memoryWarningLow = false;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (de.nmichael.efa.util.Logger.isTraceOn(de.nmichael.efa.util.Logger.TT_MEMORYSUPERVISION)) {
                    de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.DEBUG,
                            de.nmichael.efa.util.Logger.MSG_DEBUG_MEMORYSUPERVISOR,
                            "Could not determine Memory Usage: " + e.toString());
                }
            }
        } catch (NoClassDefFoundError ee) {
            if (de.nmichael.efa.util.Logger.isTraceOn(de.nmichael.efa.util.Logger.TT_MEMORYSUPERVISION)) {
                de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.DEBUG,
                        de.nmichael.efa.util.Logger.MSG_DEBUG_MEMORYSUPERVISOR,
                        "Could not determine Memory Usage: " + ee.toString() + " (only supported for Java 1.5 and higher)");
            }
        }
        return memoryLow;
    }
    
    public static boolean isMemoryWarningLow() {
        return memoryWarningLow;
    }

    public static void setMemUsageListener(de.nmichael.efa.gui.EfaBoathouseFrame frame, long threshold) {
        setMemUsageListener(new OOMEListener(frame), threshold);
    }

    public static void setMemUsageListener(OOMEListener listener, long threshold) {
        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        NotificationEmitter emitter = (NotificationEmitter) mbean;
        emitter.addNotificationListener(listener, null, null);

        List pools = ManagementFactory.getMemoryPoolMXBeans();
        for (int i = 0; i < pools.size(); i++) {
            MemoryPoolMXBean pool = (MemoryPoolMXBean) pools.get(i);
            if (pool.getName().toLowerCase().indexOf("tenured") >= 0
                    || pool.getName().toLowerCase().indexOf("old") >= 0) {
                if (pool.isCollectionUsageThresholdSupported()) {
                    long thr = pool.getUsage().getMax() * threshold / 100;
                    if (de.nmichael.efa.util.Logger.isTraceOn(de.nmichael.efa.util.Logger.TT_MEMORYSUPERVISION)) {
                        de.nmichael.efa.util.Logger.log(de.nmichael.efa.util.Logger.DEBUG,
                                de.nmichael.efa.util.Logger.MSG_DEBUG_MEMORYSUPERVISOR,
                                "Setting CollectionUsageThreshold of pool " + pool.getName() + " to " + thr + " ...");
                    }
                    pool.setCollectionUsageThreshold(thr);
                }
            }
        }
    }
}
