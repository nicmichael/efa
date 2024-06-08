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

import de.nmichael.efa.*;
import java.io.*;

import javax.swing.SwingUtilities;

// @i18n complete

public class EfaErrorPrintStream extends PrintStream {

  private static final int MAX_STACK_DEPTH_FOR_EFA_ERROR = 5;

  public static volatile boolean ignoreExceptions = false;
  private Object lastErrorObject = null;

  public EfaErrorPrintStream(FileOutputStream f) {
    super(f);
  }

  public void print(Object o) {
    errorPrint(o);
    super.print(o);
  }

  public void println(Object o) {
    errorPrint(o);
    super.println(o);
  }

  private void errorPrint(Object o) {
    if (!ignoreExceptions &&
        (o.getClass().toString().indexOf("Exception")>0 ||
         o.getClass().toString().indexOf("java.lang.NoSuchMethodError")>0 ||
         o.getClass().toString().indexOf("java.lang.NoClassDefFoundError")>0) &&
         o.toString().indexOf("java.lang.Exception: Stack trace") != 0) { // für Stack-Inkonsistenzen keine Fehlermeldung auf dem Bildschirm anzeigen

      if (o == lastErrorObject) return;
      lastErrorObject = o;

      String stacktrace = "";
      boolean efaError = false; // set to true if this exception occurred within efa code (first n stack elements)

      // get the stack trace
      try {
        StackTraceElement[] stack = null;
        try {
          stack = ((Exception)o).getStackTrace();
        } catch(Exception e1) {
          try {
            stack = ((NoSuchMethodError)o).getStackTrace();
          } catch(Exception e2) {
            try {
              stack = ((NoClassDefFoundError)o).getStackTrace();
            } catch(Exception e3) {
            };
          };
        };
        if (stack != null) {
          for (int i=0; stack != null && i<stack.length; i++) {
              String s = stack[i].toString();
              if (i<MAX_STACK_DEPTH_FOR_EFA_ERROR && s != null && s.indexOf("de.nmichael.efa") >= 0) {
                  efaError = true;
              }
              stacktrace += s + "\n";
          }
        }
      } catch(NoSuchMethodError j13) {
        EfaUtil.foo(); // StackTraceElement erst ab Java 1.4
      }
      
      // if the stack trace concerns classes from efa, ask for bug reports
      // (some other purely java (especially awt/swing) related bugs do not necessarily need to be reported...
      String text = International.getString("Unerwarteter Programmfehler")+": "+o.toString();
      if (stacktrace.length() == 0) {
          efaError = true; // assume this is an efa error (e.g. java.lang.ExceptionInInitializerError don't have a stack trace...)
      }
      if (efaError) {
          if (stacktrace.length() > 0) {
              text += "\nStack Trace:\n" + stacktrace;
          }
          text += "\n"+International.getMessage("Bitte melde diesen Fehler an: {efaemail}", Daten.EMAILBUGS);
          Logger.log(Logger.ERROR,Logger.MSG_ERROR_EXCEPTION,text);
          if (Daten.isGuiAppl()) {
              new ErrorThread(o.toString(),stacktrace).start();
          }
      } else {
          text += "\n"+International.getString("Dieser Fehler ist möglicherweise ein Fehler in Java, der durch ein Java-Update behoben werden kann. "+
                  "Meistens führt diese Art von Fehlern nur zu vorübergehenden Darstellungsproblemen und hat keine Auswirkung auf efa und die Daten. "+
                  "Sofern dieser Fehler nur selten auftritt und keine erkennbaren Folgen hat, kann er ignoriert werden.");
          if (stacktrace.length() > 0) {
              text += "\nStack Trace:\n" + stacktrace;
          }
          Logger.log(Logger.WARNING,Logger.MSG_ERROR_EXCEPTION,text);
      }

    }
  }

  public void print(String s) {
    super.print(s);
  }

    class ErrorThread extends Thread {

        String message;
        String stacktrace;

        ErrorThread(String message, String stacktrace) {
            this.message = message;
            this.stacktrace = stacktrace;
        }

        public void run() {
        	this.setName("EfaErrorPrintStream.ErrorThread");
        	SwingUtilities.invokeLater(new Runnable() {
      	      public void run() {
                  Dialog.exceptionError(message, stacktrace);
      	      }
        	});        	
        }
    }
}
