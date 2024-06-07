/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.util;

import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.util.EfaUtil.UserMessage;
import java.io.*;

public abstract class ProgressTask extends Thread {

    protected ProgressDialog progressDialog;
    protected volatile boolean running = false;
    protected int currentWorkDone = 0;
    protected BufferedWriter f;
    protected boolean autoCloseDialogWhenDone;
    protected boolean resultSuccess = true;

    public void setProgressDialog(ProgressDialog progressDialog, boolean autoCloseDialogWhenDone) {
        this.progressDialog = progressDialog;
        this.autoCloseDialogWhenDone = autoCloseDialogWhenDone;
    }

    public boolean setLogfile(String filename) {
        try {
            f = new BufferedWriter(new FileWriter(filename));
        } catch(Exception e) {
            return false;
        }
        return true;
    }

    public void abort() {
        running = false;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setDone() {
        setRunning(false);
        setCurrentWorkDone(getAbsoluteWork());
        if (resultSuccess) {
            if (getSuccessfullyDoneMessage() != null) {
                UserMessage.show(new UserMessage() {
                    public void run() {
                        Dialog.infoDialog(getSuccessfullyDoneMessage());
                    }
                });
            } 
        } else {
            if (getErrorDoneMessage() != null) {
                UserMessage.show(new UserMessage() {
                    public void run() {
                        Dialog.error(getErrorDoneMessage());
                    }
                });

            } else {
                UserMessage.show(new UserMessage() {
                    public void run() {
                        Dialog.error(International.getString("Vorgang mit Fehlern abgeschlossen."));
                    }
                });
            }
        }
        if (f != null) {
            try {
                f.close();
            } catch(Exception e) {
            }
        }
        if (autoCloseDialogWhenDone && progressDialog != null) {
            UserMessage.show(new UserMessage() {
                public void run() {
                    progressDialog.cancel();
                }
            });
        }
    }

    public boolean isRunning() {
        return running;
    }

    public abstract int getAbsoluteWork();

    public int getCurrentWorkDone() {
        return currentWorkDone;
    }

    public void logInfo(String s) {
        logInfo(s, true, true);
    }

    public void logInfo(String s, boolean toScreen, boolean toFile) {
        if (toScreen && progressDialog != null) {
            UserMessage.show(new UserMessage() {
                public void run() {
                    progressDialog.logInfo(s);
                }
            });
        }
        if (toFile && f != null) {
            try {
                f.write(s);
            } catch(Exception e) {
            }
        }
    }

    public void setCurrentWorkDone(int i) {
        this.currentWorkDone = i;
        if (progressDialog != null) {
            UserMessage.show(new UserMessage() {
                public void run() {
                    progressDialog.setCurrentWorkDone(i);
                }
            });
        }
    }

    public abstract String getSuccessfullyDoneMessage();

    public String getErrorDoneMessage() {
        return null;
    }



}