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

// @i18n complete

public class LogString {


  public static String fileOpening(String filename, String description) {
      return International.getMessage("{filedescription} '{filename}' wird geöffnet ...",
                                       description,filename) + ".";
  }

  public static String fileClosing(String filename, String description) {
      return International.getMessage("{filedescription} '{filename}' wird geschlossen ...",
                                       description,filename) + ".";
  }

  public static String fileOpened(String filename, String description) {
      return International.getMessage("{filedescription} '{filename}' geöffnet",
                                       description,filename) + ".";
  }

  public static String fileNewCreated(String filename, String description) {
      return International.getMessage("{filedescription} '{filename}' wurde neu erzeugt",
                                       description,filename) + ".";
  }

  public static String fileSuccessfullyCreated(String filename, String description) {
      return International.getMessage("{filedescription} '{filename}' wurde erfolgreich erstellt",
                                       description,filename) + ".";
  }


  public static String fileCreationFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht erstellt werden",
                                       description,filename) +
                                       (error == null ? "." : ": " + error);
  }

  public static String fileCreationFailed(String filename, String description) {
      return fileCreationFailed(filename, description, null);
  }


  public static String fileDeletionFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht gelöscht werden",
                                       description,filename) +
                                       (error == null ? "." : ": " + error);
  }

  public static String fileDeletionFailed(String filename, String description) {
      return fileDeletionFailed(filename, description, null);
  }

  public static String fileRenameFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht umbenannt werden",
                                       description,filename) +
                                       (error == null ? "." : ": " + error);
  }

  public static String fileRenameFailed(String filename, String description) {
      return fileRenameFailed(filename, description, null);
  }

  public static String fileOpenFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht geöffnet werden",
                                       description,filename) +
                                       (error == null ? "." : ": " + error);
  }

  public static String fileOpenFailed(String filename, String description) {
      return fileOpenFailed(filename, description, null);
  }



  public static String fileReadFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht gelesen werden.",
                                       description,filename) + 
                                       (error == null ? "." : ": " + error);
  }

  public static String fileReadFailed(String filename, String description) {
      return fileReadFailed(filename, description, null);
  }



  public static String fileNotFound(String filename, String description) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht gefunden werden",
                                       description,filename) + ".";
  }



  public static String fileAlreadyExists(String filename, String description) {
      return International.getMessage("{filedescription} '{filename}' existiert bereits",
                                       description,filename) + ".";
  }



  public static String fileWritingFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht geschrieben werden",
                                       description,filename) + 
                                       (error == null ? "." : ": " + error);
  }

  public static String fileWritingFailed(String filename, String description) {
      return fileWritingFailed(filename, description, null);
  }

  public static String fileSavingFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht gespeichert werden",
                                       description,filename) +
                                       (error == null ? "." : ": " + error);
  }

  public static String fileSavingFailed(String filename, String description) {
      return fileSavingFailed(filename, description, null);
  }



  public static String fileCloseFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht geschlossen werden",
                                       description,filename) +
                                       (error == null ? "." : ": " + error);
  }

  public static String fileCloseFailed(String filename, String description) {
      return fileCloseFailed(filename, description, null);
  }

  public static String fileSuccessfullyArchived(String filename, String description) {
      return International.getMessage("{filedescription} '{filename}' wurde erfolgreich gesichert",
                                       description,filename) + ".";
  }

  public static String fileArchivingFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht gesichert werden",
                                       description,filename) + 
                                       (error == null ? "." : ": " + error);
  }
  
  public static String fileArchivingFailed(String filename, String description) {
      return fileArchivingFailed(filename, description, null);
  }

  public static String fileSuccessfullyRestored(String filename, String description) {
      return International.getMessage("{filedescription} '{filename}' wurde erfolgreich wiederhergestellt",
                                       description,filename) + ".";
  }

  public static String fileRestoreFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht wiederhergestellt werden",
                                       description,filename) +
                                       (error == null ? "." : ": " + error);
  }

  public static String fileRestoreFailed(String filename, String description) {
      return fileRestoreFailed(filename, description, null);
  }

    public static String efaCloudSynchInit(String description) {
        return International.getMessage(
                "{filedescription} wird synchronisiert.",
                description);
    }

    public static String efaCloudSynchFailed(String filename, String description, String error) {
        return International.getMessage(
                "{filedescription} '{filename}' konnte nicht mit dem efaCloud Server "
                + "synchronisiert werden",
                description, filename) + (error == null ? "." : ": " + error);
    }

    public static String efaCloudSynchProgress(String filename, int requested, int succeeded, int failed) {
        return International.getMessage(
                "{cnt} Datensätze von '{filename}' synchronisiert: {succeeded} / {requested} ({failed} Fehler).",
                Integer.toString(succeeded + failed),
                filename,
                Integer.toString(succeeded),
                Integer.toString(requested),
                Integer.toString(failed));
    }

    public static String efaCloudSynchSuccessfull(String description, int succeeded, int failed) {
        return International.getMessage(
                "{filedescription} wurde mit dem efaCloud Server "
                + "synchronisiert. {all} Datensätze, davon {failed} mit Fehlern.",
                description, "" + (succeeded + failed), "" + failed);
    }

  public static String fileMoved(String filename, String description, String destination) {
      return International.getMessage("{filedescription} '{filename}' wurde nach '{destination}' verschoben",
                                       description,filename, destination) + ".";
  }

  public static String fileExtractFailed(String filename, String description, String error) {
      return International.getMessage("{filedescription} '{filename}' konnte nicht entpackt werden",
                                       description,filename) +
                                       (error == null ? "." : ": " + error);
  }

  public static String fileExtractFailed(String filename, String description) {
      return fileExtractFailed(filename, description, null);
  }


  public static String directoryDoesNotExist(String dirname, String description) {
      return International.getMessage("{directorydescription} '{directoryname}' existiert nicht",
                                       description,dirname) + ".";
  }


  public static String directoryNoWritePermission(String dirname, String description) {
      return International.getMessage("Keine Schreibberechtigung in {directorydescription} '{directoryname}'",
                                       description,dirname) + ".";
  }

  public static String directoryCreationFailed(String dirname, String description) {
      return fileCreationFailed(dirname, description);
  }


  public static String cantExecCommand(String command, String description, String error) {
      return International.getMessage("{commanddescription} '{command}' kann nicht ausgeführt werden",
                                       description,command) +
                                       (error == null ? "." : ": " + error);
  }

  public static String cantExecCommand(String command, String description) {
      return cantExecCommand(command, description, null);
  }

  public static String timedoutExecCommand(String command, String description, String error) {
      return description + " '" + command + "' timed out" +
                                       (error == null ? "." : ": " + error);
  }

  public static String timedoutExecCommand(String command, String description) {
      return timedoutExecCommand(command, description, null);
  }

  public static String operationStarted(String operationName) {
      return International.getMessage("Starte {operationname} ...", operationName);
  }

  public static String operationSuccessfullyCompleted(String operationName) {
      return International.getMessage("{operationname} erfolgreich abgeschlossen",
                                       operationName) + ".";
  }

  public static String operationFailed(String operationName) {
      return International.getMessage("{operationname} fehlgeschlagen",
                                       operationName) + ".";
  }

  public static String operationFailed(String operationName, String error) {
      return International.getMessage("{operationname} fehlgeschlagen",
                                       operationName) + ": " + error;
  }

  public static String operationFinished(String operationName) {
      return International.getMessage("{operationname} abgeschlossen", operationName) + ".";
  }

  public static String operationFinishedWithErrors(String operationName, int errorCount) {
      return International.getMessage("{operationname} mit {n} Fehlern abgeschlossen",
                                       operationName, errorCount) + ".";
  }

  public static String operationAborted(String operationName) {
      return International.getMessage("{operationname} abgebrochen",
                                       operationName) + ".";
  }


  public static String installationSuccessfullyCompleted(String item) {
      return operationSuccessfullyCompleted(International.getMessage("Installation von {name}",
                                       item));
  }

  public static String installationFailed(String item) {
      return operationFailed(International.getMessage("Installation von {name}",
                                       item));
  }

  public static String onlyEffectiveAfterRestart(String item) {
      return International.getMessage("{name} wird erst nach einem Neustart von efa wirksam.",
                                       item);
  }

  public static String itemIsUnknown(String item, String description) {
      return International.getMessage("{description} {name} unbekannt",
                                       description, item) + ".";
  }

  public static String emailSuccessfullySend(String subject) {
      return International.getMessage("email '{subject}' erfolgreich verschickt",
                                       subject) + ".";
  }

  public static String emailSuccessfullyEnqueued(String subject) {
      return International.getMessage("email '{subject}' wird im Hintergrund versendet ...",
                                       subject) + ".";
  }

  public static String emailSendFailed(String subject, String error) {
      return International.getMessage("email '{subject}' konnte nicht versendet werden",
                                       subject) + ": " + error;
  }

  public static void logInfo_fileOpened(String filename, String description) {
      Logger.log(Logger.INFO, Logger.MSG_FILE_FILENEWCREATED,
                 fileOpened(filename,description));
  }

  public static void logInfo_fileNewCreated(String filename, String description) {
      Logger.log(Logger.INFO, Logger.MSG_FILE_FILENEWCREATED,
                 fileNewCreated(filename,description));
  }

  public static void logInfo_fileSuccessfullyCreated(String filename, String description) {
      Logger.log(Logger.INFO, Logger.MSG_FILE_FILESUCCESSFULLYCREATED,
                 fileSuccessfullyCreated(filename,description));
  }

  public static void logWarning_fileNewCreated(String filename, String description) {
      Logger.log(Logger.WARNING, Logger.MSG_FILE_FILENEWCREATED,
                 fileNewCreated(filename,description));
  }

  public static void logError_fileCreationFailed(String filename, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_FILECREATEFAILED,
              fileCreationFailed(filename,description));
  }

  public static void logError_fileDeletionFailed(String filename, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_FILECREATEFAILED,
              fileDeletionFailed(filename,description));
  }

  public static void logError_fileOpenFailed(String filename, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_FILEOPENFAILED,
              fileOpenFailed(filename,description));
  }

  public static void logError_fileReadFailed(String filename, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_FILEREADFAILED,
              fileReadFailed(filename,description));
  }

  public static void logError_fileNotFound(String filename, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_FILENOTFOUND,
              fileNotFound(filename,description));
  }

  public static void logError_fileAlreadyExists(String filename, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_FILEALREADYEXISTS,
              fileAlreadyExists(filename,description));
  }

  public static void logError_fileWritingFailed(String filename, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_FILEWRITEFAILED,
              fileWritingFailed(filename,description));
  }

  public static void logError_fileCloseFailed(String filename, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_FILECLOSEFAILED,
              fileCloseFailed(filename,description));
  }

  public static void logError_fileArchivingFailed(String filename, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_ARCHIVINGFAILED,
              fileArchivingFailed(filename,description));
  }

  public static void logError_fileArchivingFailed(String filename, String description, String error) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_ARCHIVINGFAILED,
              fileArchivingFailed(filename,description, error));
  }

  public static void logError_directoryDoesNotExist(String dirname, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_DIRECTORYNOTFOUND,
              directoryDoesNotExist(dirname,description));
  }
  
   public static void logError_directoryNoWritePermission(String dirname, String description) {
      Logger.log(Logger.ERROR, Logger.MSG_FILE_DIRECTORYNOTFOUND,
              directoryNoWritePermission(dirname,description));
  }

  public static void logWarning_cantExecCommand(String command, String description, String error) {
      Logger.log(Logger.WARNING, Logger.MSG_WARN_CANTEXECCOMMAND,
              cantExecCommand(command,description,error));
  }

}