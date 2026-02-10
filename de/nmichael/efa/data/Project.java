/* Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */
package de.nmichael.efa.data;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaCloudUsers;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.efacloud.Ecrid;
import de.nmichael.efa.data.efacloud.TxRequestQueue;
import de.nmichael.efa.data.storage.Audit;
import de.nmichael.efa.data.storage.DataAccess;
import de.nmichael.efa.data.storage.DataFile;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.DataRecord;
import de.nmichael.efa.data.storage.EfaOnlineClient;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.storage.MetaData;
import de.nmichael.efa.data.storage.RemoteEfaClient;
import de.nmichael.efa.data.storage.StorageObject;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.ex.EfaException;
import de.nmichael.efa.ex.EfaModifyException;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaSortStringComparator;
import de.nmichael.efa.util.EfaSortStringDescComparator;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;

// @i18n complete
public class Project extends StorageObject {

    public static final String DATATYPE = "efa2project";
    public static final String STORAGEOBJECT_AUTOINCREMENT = "autoincrement";
    public static final String STORAGEOBJECT_SESSIONGROUPS = "sessiongroups";
    public static final String STORAGEOBJECT_PERSONS = "persons";
    public static final String STORAGEOBJECT_CLUBWORK = "clubwork";
    public static final String STORAGEOBJECT_STATUS = "status";
    public static final String STORAGEOBJECT_GROUPS = "groups";
    public static final String STORAGEOBJECT_FAHRTENABZEICHEN = "fahrtenabzeichen";
    public static final String STORAGEOBJECT_BOATS = "boats";
    public static final String STORAGEOBJECT_CREWS = "crews";
    public static final String STORAGEOBJECT_BOATSTATUS = "boatstatus";
    public static final String STORAGEOBJECT_BOATRESERVATIONS = "boatreservations";
    public static final String STORAGEOBJECT_BOATDAMAGES = "boatdamages";
    public static final String STORAGEOBJECT_DESTINATIONS = "destinations";
    public static final String STORAGEOBJECT_WATERS = "waters";
    public static final String STORAGEOBJECT_STATISTICS = "statistics";
    public static final String STORAGEOBJECT_MESSAGES = "messages";
    public static final String STORAGEOBJECT_EFACLOUDUSERS = "efacloudusers";
    private Hashtable<String, StorageObject> persistenceCache = new Hashtable<String, StorageObject>();
    protected IDataAccess remoteDataAccess; // used for ClubRecord and LogbookRecord, if TYPE_EFA_REMOTE
    private String myIdentifier = null;
    private String myBoathouseName = null;
    private int myBoathouseId = -1;
    private int myBoathouseIdExplicit = -1;
    private int numberOfBoathouses = -1;
    private Hashtable<Integer, String> boathouseIdToNameMapping = null;
    private volatile boolean _inOpeningProject = false;
    private volatile boolean _inDeleteProject = false;
    private boolean isRemoteOpen = false;

    // Note: storageType and storageLocation are only type and location for the project file itself
    // (which is always being stored in the file system). The storageType and storageLocation for
    // the project's content may differ.
    public Project(int storageType, String storageLocation, String storageObjectName) {
        super(storageType, storageLocation, null, null, storageObjectName, DATATYPE,
                International.getString("Projekt") + " " + storageObjectName);
        ProjectRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
        updateMyIdentifier();
    }

    public Project(String projectName) {
        super(IDataAccess.TYPE_FILE_XML, Daten.efaDataDirectory, null, null, projectName, DATATYPE,
                International.getString("Projekt") + " " + projectName);
        ProjectRecord.initialize();
        dataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
        updateMyIdentifier();
    }

    private void updateMyIdentifier() {
        try {
            myIdentifier = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            Logger.logdebug(e);
            myIdentifier = null;
        }
    }

    public String getMyIdentifier() {
        return myIdentifier;
    }

    public String transformFieldName(String fieldName) {
        if (fieldName.equals(ProjectRecord.DEPRECATED_LOGBOOKNAME)) {
            return ProjectRecord.NAME;
        }
        return fieldName;
    }

    public static boolean openProject(String projectName, boolean runAudit) {
        return openProject(new Project(projectName), projectName, runAudit);
    }

    public static boolean openProjectSilent(String projectName, boolean runAudit) {
        return openProject(new Project(projectName), projectName, runAudit, true);
    }

    public static boolean openProject(Project p, String projectName, boolean runAudit) {
        return openProject(p, projectName, runAudit, false);
    }

    public static boolean openProject(Project p, String projectName, boolean runAudit, boolean silent) {
        if (Logger.isTraceOn(Logger.TT_CORE, 1)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Opening Project " + projectName + " ...");
        }
        try {
            p._inOpeningProject = true;
            p.isRemoteOpen = false;
            p.open(false);
            if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Project Description " + projectName + " opened.");
            }
            p.convertInternal();
            Daten.project = p;
            p.openAllData();
            Audit auditTask = null;
            if (p.getProjectStorageType() == IDataAccess.TYPE_FILE_XML && runAudit) {
                auditTask = new Audit(p);
            }
            if (p.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                p.remoteDataAccess = DataAccess.createDataAccess(p, IDataAccess.TYPE_EFA_REMOTE,
                        p.getProjectStorageLocation(),
                        p.getProjectStorageUsername(),
                        p.getProjectStoragePassword(),
                        p.getProjectRemoteProjectName(),
                        p.dataAccess.getStorageObjectType(),
                        International.getString("Projekt") + " " + p.getProjectRemoteProjectName());
                p.remoteDataAccess.setMetaData(MetaData.getMetaData(DATATYPE));
                // since login into remote data is lazy, we should retrieve a project record here
                // to make the login happen. It's important to chose a record which is a remote
                // record, i.e. *not* the project record itself. Therefore we select the club
                // record.
                p.isRemoteOpen = (p.getClubRecord() != null);
            }
            p._inOpeningProject = false;
            if (auditTask!=null) {
	            // Audit checks for p._inOpeningProject=false. So to be sure, 
	            // Audit can only be started after p._inOpeningProject is set to false.
            	auditTask.start(); // AuditTask is a thread that runs in background.
            	auditTask.join(); // this would wait for audit task to complete, before we can proceed.
            }
        } catch (Exception ee) {
            if (!silent) {
                Logger.log(ee);
                Dialog.error(LogString.fileOpenFailed(projectName, International.getString("Projekt"), ee.toString()));
            }
            Daten.project = null;
            return false;
        }
        return true;
    }

    private void convertInternal() {
        try {
            if (getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                return;
            }
        } catch (Exception e) {
            Logger.logdebug(e);
            return;
        }

        // create project id
        try {
            if (isOpen()) {
                DataKeyIterator it = data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    ProjectRecord r = (ProjectRecord) data().get(k);
                    if (r != null && ProjectRecord.TYPE_CLUB.equals(r.getType())
                            && (r.getProjectId() == null)) {
                        setProjectId(r);
                        data().update(r);
                        Logger.log(Logger.INFO, Logger.MSG_CORE_PROJECTIDGENERATED,
                                "Project ID: " + r.getProjectId());
                    }
                }
            }
        } catch (Exception e) {
            if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Deletion of empty Boathouse Records failed.");
            }
            Logger.logdebug(e);
        }

        // delete any "emtpy" boathouse records (bugfix)
        try {
            if (isOpen()) {
                DataKeyIterator it = data().getStaticIterator();
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    ProjectRecord r = (ProjectRecord) data().get(k);
                    if (r != null && ProjectRecord.TYPE_BOATHOUSE.equals(r.getType())
                            && (r.getName() == null || r.getName().length() == 0)) {
                        data().delete(k);
                    }
                }
            }
        } catch (Exception e) {
            if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Deletion of empty Boathouse Records failed.");
            }
            Logger.logdebug(e);
        }

        // create new Boathouse Record from previous Project, Club and Config Records
        try {
            if (isOpen()) {
                if (getNumberOfBoathouses() == 0) {
                    if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Starting Project Conversion ...");
                    }
                    ProjectRecord oldProjectRecord = getRecord(ProjectRecord.getDataKey(ProjectRecord.TYPE_PROJECT, null));
                    ProjectRecord oldClubRecord = getRecord(ProjectRecord.getDataKey(ProjectRecord.TYPE_CLUB, null));
                    ProjectRecord oldConfigRecord = getRecord(ProjectRecord.getDataKey("Config", null));
                    if (oldProjectRecord != null && oldConfigRecord != null) {
                        ProjectRecord boathouseRecord = createNewBoathouseRecord(International.getString("Bootshaus"));
                        String s;
                        boolean oldProjectRecordChanged = false;
                        boolean oldClubRecordChanged = false;

                        s = oldProjectRecord.getCurrentLogbookEfaBase();
                        if (s != null) {
                            boathouseRecord.setCurrentLogbookEfaBase(s);
                            oldProjectRecord.setCurrentLogbookEfaBase(null);
                            oldProjectRecordChanged = true;
                        }

                        s = oldProjectRecord.getCurrentClubworkEfaBase();
                        if (s != null) {
                            boathouseRecord.setCurrentClubworkEfaBase(s);
                            oldProjectRecord.setCurrentClubworkEfaBase(null);
                            oldProjectRecordChanged = true;
                        }

                        s = oldProjectRecord.getCurrentLogbookEfaBoathouse();
                        if (s != null) {
                            boathouseRecord.setCurrentLogbookEfaBoathouse(s);
                            oldProjectRecord.setCurrentLogbookEfaBoathouse(null);
                            oldProjectRecordChanged = true;
                        }

                        s = oldProjectRecord.getCurrentClubworkEfaBoathouse();
                        if (s != null) {
                            boathouseRecord.setCurrentClubworkEfaBoathouse(s);
                            oldProjectRecord.setCurrentClubworkEfaBoathouse(null);
                            oldProjectRecordChanged = true;
                        }

                        int i = oldClubRecord.getAreaId();
                        if (i > 0) {
                            boathouseRecord.setAreaId(i);
                            oldClubRecord.setAreaId(IDataAccess.UNDEFINED_INT);
                            oldClubRecordChanged = true;
                        }

                        DataTypeDate d = oldConfigRecord.getAutoNewLogbookDate();
                        if (d != null && d.isSet()) {
                            boathouseRecord.setAutoNewLogbookDate(d);
                        }
                        d = oldConfigRecord.getAutoNewClubworkDate();
                        if (d != null && d.isSet()) {
                            boathouseRecord.setAutoNewClubworkDate(d);
                        }

                        s = oldConfigRecord.getAutoNewLogbookName();
                        if (s != null) {
                            boathouseRecord.setAutoNewLogbookName(s);
                        }
                        s = oldConfigRecord.getAutoNewClubworkName();
                        if (s != null) {
                            boathouseRecord.setAutoNewClubworkName(s);
                        }

                        IDataAccess data = getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE);
                        if (data != null) {
                            data.add(boathouseRecord);
                            data.delete(oldConfigRecord.getKey());
                        }
                        data = getMyDataAccess(ProjectRecord.TYPE_CLUB);
                        if (data != null && oldClubRecordChanged) {
                            data.update(oldClubRecord);
                        }
                        data = getMyDataAccess(ProjectRecord.TYPE_PROJECT);
                        if (data != null && oldProjectRecordChanged) {
                            data.update(oldProjectRecord);
                        }
                    }
                    if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Finished Project Conversion.");
                    }
                }
            }
        } catch (Exception e) {
            if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Project Conversion failed.");
            }
            Logger.logdebug(e);
        }

        // fix invalid boathouse id in boathouse records
        try {
            if (isOpen()) {
                String[] bhNames = getAllBoathouseNames();
                for (int i=0; bhNames != null && i<bhNames.length; i++) {
                    ProjectRecord r = getBoathouseRecord(bhNames[i]);
                    if (r != null && r.getBoathouseId() < 0) {
                        int lastId = getHighestBoathouseId();
                        if (lastId >= 0) {
                            r.setBoathouseId(lastId + 1);
                            IDataAccess data = getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE);
                            if (data != null) {
                                data.update(r);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Project Conversion failed.");
            }
            Logger.logdebug(e);
        }
    }

    public boolean isRemoteOpen() {
        return isRemoteOpen;
    }

    public boolean openAllData() {
        if (getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
            // in order to speed up initial login to a remote project,
            // we will only open the neccesary files on demand
            return true;
        }
        if (getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD) {
        	//only start TxQueue if we surely want to open this project for further usage
            TxRequestQueue.getInstance(getProjectRecord().getEfaCloudURL(), getProjectStorageUsername(),
                    getProjectStoragePassword(), getProjectStorageLocation());
        }
        try {
            if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Opening all Project Data ...");
            }
            if (!isOpen()) {
                open(false);
            }
            getAutoIncrement(true);
            getSessionGroups(true);
            getPersons(true);
            getStatus(true);
            getGroups(true);
            getFahrtenabzeichen(true);
            getBoats(true);
            getCrews(true);
            getBoatStatus(true);
            getBoatReservations(true);
            getBoatDamages(true);
            getDestinations(true);
            getWaters(true);
            getStatistics(true);
            getMessages(true);
            if (getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD)
                getEfaCloudUsers(true);
            if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "All Project Data opened.");
            }
            return true;
        } catch (Exception e) {
            if (Logger.isTraceOn(Logger.TT_CORE, 3)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Failed to open Project Data.");
            }
            Logger.log(e);
            return false;
        }
    }

    public boolean isInOpeningProject() {
        return _inOpeningProject;
    }

    public boolean deleteProject() {
        // we need to cache this, later it's gone...
        String projectName = getProjectName();
        String projectDir = getProjectStorageLocation();

        try {
            // we might get lots of exceptions, don't log them
            Logger.setLogExceptions(false);

        	/*   
	     	   READ ME!!
	     	   we don't need to load all open data for efaCloud (and, in peticular, SHOULD NOT):
	     	   - first, efaCloud project data files remain on disk. only the project file itself gets deleted.
	     	     so, no need to load all data to be able to delete it for efaCloud projects.
	     	   - second, opening the efaCloud project with openAllData() initiates the singleton TXQueue instance
	     	     with the data of the project to delete. This OVERWRITES ANY TXQueue elements for a currently active efaCloud project,
	     	     leading to very unreasonable behaviour of efa. Also the overwritten TXQueue stays active and creates efaCloud logs
	     	     even for non-efaCloud projects. 
	     	   So, openAllData is only used for non-efaCloud projects 
        	*/ 
            if (getProjectStorageType() == IDataAccess.TYPE_FILE_XML ||
            		getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
	            //make sure that persistenceCache is filled properly
	            try {
	                this.openAllData();
	                String[] logbookNames = getAllLogbookNames();
	                for (String logbookName : logbookNames) {
	                    getLogbook(logbookName, false);
	                }
	                String[] clubworkNames = getAllClubworkNames();
	                for (String clubworkName : clubworkNames) {
	                    getClubwork(clubworkName, false);
	                }
	            } catch (Exception eignore) {
	                Logger.logdebug(eignore);
	            }
	
	            setPreModifyRecordCallbackEnabled(false);
	            _inDeleteProject = true;
	            if (getProjectStorageType() == IDataAccess.TYPE_FILE_XML) {
	                String[] keys = persistenceCache.keySet().toArray(new String[0]);
	                for (String key : keys) {
	                    StorageObject p = persistenceCache.get(key);
	                    try {
	                        p.data().deleteStorageObject();
	                    } catch (Exception eignore) {
	                        Logger.logdebug(eignore);
	                        try {
	                            (new File(((DataFile) p.data()).getFilename())).delete();
	                        } catch (Exception eignore2) {
	                        }
	                    }
	                }
	            }
            } else if (getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD) {
            	// ensure that the data file for the project is loaded
            	this.open(false);
            }
            
            data().deleteStorageObject();
            if (projectDir != null) {
                (new File(projectDir)).delete(); // delete project directory
            }
            if (projectDir == null || (new File(projectDir)).exists()) {
                Dialog.error(International.getMessage("Das Projekt konnte nicht vollständig gelöscht werden. Es befinden sich noch Daten in {directory}.",
                        projectDir));
            }
        } catch (Exception e) {
            _inDeleteProject = false;
            Logger.setLogExceptions(true);
            Logger.log(e);
            Dialog.error(LogString.fileDeletionFailed(projectName, International.getString("Projekt"), e.toString()));
            return false;
        } finally {
            Logger.setLogExceptions(true);
        }
        _inDeleteProject = false;
        return true;
    }

    public Vector<StorageObject> getAllDataAndLogbooks() {
        Vector<StorageObject> data = new Vector<StorageObject>();
        data.add(getAutoIncrement(false));
        data.add(getSessionGroups(false));
        data.add(getPersons(false));
        data.add(getStatus(false));
        data.add(getGroups(false));
        data.add(getFahrtenabzeichen(false));
        data.add(getBoats(false));
        data.add(getCrews(false));
        data.add(getBoatStatus(false));
        data.add(getBoatReservations(false));
        data.add(getBoatDamages(false));
        data.add(getDestinations(false));
        data.add(getWaters(false));
        data.add(getStatistics(false));
        data.add(getMessages(false));
        String[] logbookNames = getAllLogbookNames();
        for (int i = 0; logbookNames != null && i < logbookNames.length; i++) {
            data.add(getLogbook(logbookNames[i], false));
        }
        String[] clubworkNames = getAllClubworkNames();
        for (int i = 0; clubworkNames != null && i < clubworkNames.length; i++) {
            data.add(getClubwork(clubworkNames[i], false));
        }
        return data;
    }

    public static Hashtable<String, String> getProjects() {
        Hashtable<String, String> items = new Hashtable<String, String>();
        try {
            File dir = new File(Daten.efaDataDirectory);
            if (dir.isDirectory()) {
                String[] files = dir.list();
                for (int i = 0; files != null && i < files.length; i++) {
                    if (files[i] != null && files[i].length() > 0
                            && files[i].toLowerCase().endsWith("." + Project.DATATYPE)) {
                        int pos = files[i].lastIndexOf(".");
                        String name = files[i].substring(0, pos);
                        try {
                            Project p = new Project(name);
                            p.open(false);
                            StringBuffer description = new StringBuffer();
                            description.append("<b>" + International.getString("Projekt") + ":</b> <b style=\"color:blue\">" + name + " </b><b> "+getProjectDataType(p)+"</b><br>");
                            
                            if (p.getProjectDescription() != null) {
                                description.append("<i>"+p.getProjectDescription() + "</i><br>");
                            }
                            
                            if (p.getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD) {
                            	description.append("efaCloud-"+International.getString("Benutzername")+": "+p.getProjectStorageUsername() + "<br>");
                            }
                            if (p.getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                                description.append(International.getString("Remote-Projektname") + ": "+p.getProjectRemoteProjectName()+"<br>");
                            } else {

	                            String[] logbooks = p.getAllLogbookNames();
	                            if (logbooks != null) {
	                                description.append(International.getString("Fahrtenbücher") + ": ");
	                                for (int j = 0; j < logbooks.length; j++) {
	                                    description.append((j > 0 ? ", " : "") + logbooks[j]);
	                                }
	                            }
	                            String[] clubworkNames = p.getAllClubworkNames();
	                            if (clubworkNames != null) {
	                                description.append("<br>" +
	                                        International.getString("Vereinsarbeit") + ": ");
	                                for (int j = 0; j < clubworkNames.length; j++) {
	                                    description.append((j > 0 ? ", " : "") + clubworkNames[j]);
	                                }
	                            }
                            }
                            String[] boatHouseNames = p.getAllBoathouseNames();
                            if (boatHouseNames != null && boatHouseNames.length>1) {
                                description.append("<br>" +
                                        International.getString("Bootshäuser") + ": ");
                                for (int j = 0; j < boatHouseNames.length; j++) {
                                    description.append((j > 0 ? ", " : "") + boatHouseNames[j]);
                                }
                            }
                            
                            Boolean currentProject=(Daten.project != null ? p.getName().equals(Daten.project.getName()) : false);
                            
                            //items.put(name, "<html>"+description.toString()+"</html>");
                            //highlight currently loaded project with green background
                            items.put(name, "<html><table width=\"100%\" " 
                            		+ (currentProject ? " bgcolor=\"#ccffcc\"" : "") + "><tr><td>"
                            		+ (currentProject ? "<font color=black>" : "")
                            		+ description.toString()
                            		+ (currentProject ? "</font>" : "")
                            		+"</td></tr></table></html>");
                            if ((p!=null) && (p.isOpen())) {
                            	p.close();// it's a good idea to close a project which has been opened before - just for filehandling issues
                            }
                        } catch (Exception e1) {
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return items;
    }

    private static String getProjectDataType(Project p) {
    	if (p!=null) {
    		switch (p.getProjectStorageType()) {

    		case IDataAccess.TYPE_FILE_XML:
				return "";

    		case IDataAccess.TYPE_EFA_CLOUD:
   				return "("+International.getString("efaCloud")+": "+p.getProjectEfaCloudURL()+")";

    		case IDataAccess.TYPE_EFA_REMOTE:
   				return (p.getProjectEfaOnlineConnect() ? "(efaRemote + efaOnline: " : "(efaRemote: ")+  p.getProjectStorageLocation() +")";

    		default:
  	    		return "";
    		}
    	} else {
    		return "";
    	}

    }
    
    public Hashtable<String, String> getLogbooks() {
        Hashtable<String, String> items = new Hashtable<String, String>();
        String[] logbooks = getAllLogbookNames();
        for (int i = 0; logbooks != null && i < logbooks.length; i++) {
            ProjectRecord r = getLoogbookRecord(logbooks[i]);
            if (r != null) {
                String name = "<b>" + International.getString("Fahrtenbuch") + ":</b> <b><font color=blue>" + logbooks[i] + "</font></b><br>";
                String description = (r.getDescription() != null && r.getDescription().length() > 0 ? "<i>"+r.getDescription() + "</i><br> " : "");
                description += "(" + r.getStartDate().toString() + " - " + r.getEndDate() + ")";
                
                Boolean currentLogbook=(Daten.project != null && Daten.project.getCurrentLogbook() != null 
                		? logbooks[i].equals(Daten.project.getCurrentLogbook().getName() ) 
                		: false);
                
                //items.put(name, "<html>"+description.toString()+"</html>");
                //highlight currently loaded project with green background
                items.put(logbooks[i], "<html><table width=\"100%\" " 
                		+ (currentLogbook ? " bgcolor=\"#ccffcc\"" : "") + "><tr><td>"
                		+ (currentLogbook ? "<font color=black>" : "")
                		+ name + description
                		+ (currentLogbook ? "</font>" : "")
                		+"</td></tr></table></html>");
            }
        }
        return items;
    }

    public Hashtable<String, String> getClubworks() {
        Hashtable<String, String> items = new Hashtable<String, String>();
        String[] clubworks = getAllClubworkNames();
        for (int i = 0; clubworks != null && i < clubworks.length; i++) {
            ProjectRecord r = getClubworkBookRecord(clubworks[i]);
            if (r != null) {
                String name = "<b>" + International.getString("Vereinsarbeit") + ":</b> <b><font color=blue>" + clubworks[i] + "</font></b><br>";
                String description = (r.getDescription() != null && r.getDescription().length() > 0 ? r.getDescription() + " " : "");
                description += "(" + r.getStartDate().toString() + " - " + r.getEndDate() + ")";
                
                Boolean currentClubwork=(Daten.project != null && Daten.project.getCurrentClubwork() != null 
                		? clubworks[i].equals(Daten.project.getCurrentClubwork().getName() ) 
                		: false);
                
                //items.put(name, "<html>"+description.toString()+"</html>");
                //highlight currently loaded project with green background
                items.put(clubworks[i], "<html><table width=\"100%\" " 
                		+ (currentClubwork ? " bgcolor=\"#ccffcc\"" : "") + "><tr><td>"
                		+ (currentClubwork ? "<font color=black>" : "")
                		+ name + description
                		+ (currentClubwork ? "</font>" : "")
                		+"</td></tr></table></html>");                

            }
        }
        return items;
    }

    public static ProjectRecord createNewRecordFromStatic(String type) {
        if (MetaData.getMetaData(DATATYPE) == null) {
            ProjectRecord.initialize();
        }
        return new ProjectRecord(null, MetaData.getMetaData(DATATYPE), type);
    }

    public DataRecord createNewRecord() {
        return new ProjectRecord(this, MetaData.getMetaData(DATATYPE));
    }

    public ProjectRecord createProjectRecord(String type, String name) {
        ProjectRecord p = new ProjectRecord(this, MetaData.getMetaData(DATATYPE), type);
        if (type.equals(ProjectRecord.TYPE_LOGBOOK)) {
            p.setName(name);
        }
        if (type.equals(ProjectRecord.TYPE_CLUBWORK)) {
            p.setName(name);
        }
        if (type.equals(ProjectRecord.TYPE_BOATHOUSE)) {
            p.setName(name);
        }
        return p;
    }

    public ProjectRecord createNewLogbookRecord(String logbookName) {
        return createProjectRecord(ProjectRecord.TYPE_LOGBOOK, logbookName);
    }

    public ProjectRecord createNewClubworkBook(String cluworkName) {
        return createProjectRecord(ProjectRecord.TYPE_CLUBWORK, cluworkName);
    }

    public ProjectRecord createNewBoathouseRecord(String boathouseName) {
        if (boathouseName == null || boathouseName.length() == 0) {
            return null;
        }
        ProjectRecord r = createProjectRecord(ProjectRecord.TYPE_BOATHOUSE, boathouseName);
        int lastid = getHighestBoathouseId();
        if (lastid < 0) {
            return null;
        }
        r.setBoathouseId(lastid + 1);
        return r;
    }

    public IDataAccess getMyDataAccess(String recordType) {
        if (recordType.endsWith(ProjectRecord.TYPE_CLUB)
                || recordType.endsWith(ProjectRecord.TYPE_LOGBOOK)
                || recordType.endsWith(ProjectRecord.TYPE_CLUBWORK)
                || recordType.endsWith(ProjectRecord.TYPE_BOATHOUSE)) {
            if (getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE) {
                return (remoteDataAccess != null ? remoteDataAccess : dataAccess);
            } else {
                return dataAccess;
            }
        } else {
            return dataAccess;
        }

    }

    public IDataAccess getRemoteDataAccess() {
        return remoteDataAccess;
    }

    public AdminRecord getRemoteAdmin() {
        if (getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE
                && getRemoteDataAccess() != null) {
            return ((RemoteEfaClient) getRemoteDataAccess()).getAdminRecord();
        }
        return null;
    }

    public boolean deleteLogbookRecord(String logbookName) {
        try {
            getMyDataAccess(ProjectRecord.TYPE_LOGBOOK).delete(createProjectRecord(ProjectRecord.TYPE_LOGBOOK, logbookName).getKey());
        } catch (Exception e) {
            Logger.logdebug(e);
            if (e instanceof EfaModifyException) {
                ((EfaModifyException) e).displayMessage();
            }
            return false;
        }
        return true;
    }

    private void setProjectId(ProjectRecord r) {
        r.setProjectId(UUID.randomUUID());
    }

    public UUID getProjectId() {
        return getClubRecord().getProjectId();
    }

    public boolean deleteClubworkBook(String clubworkName) {
        try {
            getMyDataAccess(ProjectRecord.TYPE_CLUBWORK).delete(createProjectRecord(ProjectRecord.TYPE_CLUBWORK, clubworkName).getKey());
        } catch (Exception e) {
            Logger.logdebug(e);
            if (e instanceof EfaModifyException) {
                ((EfaModifyException) e).displayMessage();
            }
            return false;
        }
        return true;
    }

    public void setEmptyProject(String name) {
        try {
            dataAccess.truncateAllData();
            ProjectRecord rec;
            rec = (ProjectRecord) createNewRecord();
            rec.setType(ProjectRecord.TYPE_PROJECT);
            rec.setProjectName(name);
            dataAccess.add(rec);
            rec = (ProjectRecord) createNewRecord();
            rec.setType(ProjectRecord.TYPE_CLUB);
            setProjectId(rec);
            dataAccess.add(rec);
            rec = (ProjectRecord) createNewRecord();
            rec.setType(ProjectRecord.TYPE_BOATHOUSE);
            rec.setName(International.getString("Bootshaus"));
            dataAccess.add(rec);
        } catch (Exception e) {
            Logger.log(e);
        }
    }

    public DataKey getProjectRecordKey() {
        return ProjectRecord.getDataKey(ProjectRecord.TYPE_PROJECT, null);
    }

    public DataKey getClubRecordKey() {
        return ProjectRecord.getDataKey(ProjectRecord.TYPE_CLUB, null);
    }

    public DataKey getLoogbookRecordKey(String logbookName) {
        return ProjectRecord.getDataKey(ProjectRecord.TYPE_LOGBOOK, logbookName);
    }

    public DataKey getClubworkBookRecordKey(String clubworkName) {
        return ProjectRecord.getDataKey(ProjectRecord.TYPE_CLUBWORK, clubworkName);
    }

    public DataKey getBoathouseRecordKey(String boathouseName) {
        return ProjectRecord.getDataKey(ProjectRecord.TYPE_BOATHOUSE, boathouseName);
    }

    public ProjectRecord getRecord(DataKey k) {
        try {
            return (ProjectRecord) getMyDataAccess((String) k.getKeyPart1()).get(k);
        } catch (Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public ProjectRecord getProjectRecord() {
        ProjectRecord r = getRecord(getProjectRecordKey());
        if (r == null && isOpen()) {
            r = (ProjectRecord) createNewRecord();
            r.setType(ProjectRecord.TYPE_PROJECT);
            try {
                getMyDataAccess(ProjectRecord.TYPE_PROJECT).add(r);
            } catch (Exception e) {
                Logger.log(e);
                r = null;
            }
        }
        if (r == null) {
            try {
                throw new Exception("No ProjectRecord found!");
            } catch(Exception e) {
                Logger.logdebug(e);
            }
        }
        return r;
    }

    public ProjectRecord getClubRecord() {
        ProjectRecord r = getRecord(getClubRecordKey());
        if (r == null && isOpen()) {
            r = (ProjectRecord) createNewRecord();
            r.setType(ProjectRecord.TYPE_CLUB);
            try {
                getMyDataAccess(ProjectRecord.TYPE_CLUB).add(r);
            } catch (Exception e) {
                Logger.logdebug(e); // happens for remote projects which aren't yet open
                r = null;
            }
        }
        return r;
    }

    public ProjectRecord getLoogbookRecord(String logbookName) {
        return getRecord(getLoogbookRecordKey(logbookName));
    }

    public ProjectRecord getClubworkBookRecord(String clubworkName) {
        return getRecord(getClubworkBookRecordKey(clubworkName));
    }

    public String getBoathouseName(int id) {
        String name = null;
        try {
            if (boathouseIdToNameMapping == null) {
                refreshBoathouseIdToNameMapping();
            }
            if (boathouseIdToNameMapping != null) {
                name = boathouseIdToNameMapping.get(id);
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
        return (name != null && name.length() > 0 ? name : International.getString("Bootshaus") + " " + id);
    }

    public int getBoathouseId(String boathouseName) {
        ProjectRecord pr = getBoathouseRecord(boathouseName);
        return (pr != null ? pr.getBoathouseId() : -1);
    }

    public ProjectRecord getBoathouseRecord(String boathouseName) {
        ProjectRecord r = getRecord(getBoathouseRecordKey(boathouseName));
        return r;
    }

    public ProjectRecord getBoathouseRecord() {
        ProjectRecord r = null;
        if (myBoathouseName != null) {
            r = getRecord(getBoathouseRecordKey(myBoathouseName));
            if (r != null) {
                return r;
            }
        }
        String[] boathouseNames = getAllBoathouseNames();
        if (boathouseNames != null && boathouseNames.length > 0) {
            for (String bn : boathouseNames) {
                r = getRecord(getBoathouseRecordKey(bn));
                if (r != null) {
                    if (r.getBoathouseId() == myBoathouseIdExplicit) {
                        return r;
                    }
                    if (boathouseNames.length == 1 || myIdentifier == null
                            || (myIdentifier.equals(r.getBoathouseIdentifier()))) {
                        myBoathouseName = r.getName();
                        return r;
                    }
                }
            }
        } else {
            r = (ProjectRecord) createNewRecord();
            r.setType(ProjectRecord.TYPE_BOATHOUSE);
            r.setName(International.getString("Bootshaus"));
            try {
                getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).add(r);
            } catch (Exception e) {
                Logger.logdebug(e); // happens for remote projects which aren't yet open
                r = null;
            }
        }
        return r;
    }

    public int getMyBoathouseId() {
        if (myBoathouseId >= 0) {
            return myBoathouseId;
        }
        if (myBoathouseIdExplicit >= 0) {
            myBoathouseId = myBoathouseIdExplicit;
            return myBoathouseId;
        }
        ProjectRecord r = getBoathouseRecord();
        if (r == null) {
            return -1;
        }
        myBoathouseId = r.getBoathouseId();
        return myBoathouseId;
    }

    public String getMyBoathouseName() {
        if (myBoathouseName != null && myBoathouseName.length() > 0) {
            return myBoathouseName;
        }
        ProjectRecord r = getBoathouseRecord();
        if (r == null) {
            return null;
        }
        myBoathouseName = r.getName();
        return myBoathouseName;
    }
    
    public void setMyBoathouseName(String name) {
        ProjectRecord r = getBoathouseRecord(name);
        if (r != null) {
            myBoathouseName = name;
            myBoathouseId = r.getBoathouseId();
            myBoathouseIdExplicit = myBoathouseId;
        }
    }

    public void addRecord(ProjectRecord rec, final String type) throws EfaException {
        if (!rec.getType().equals(type)) {
            throw new EfaException(Logger.MSG_DATA_GENERICEXCEPTION, dataAccess.getUID() + ": Attempt to add a Record as a " + type + " Record which is not a " + type + " Record", Thread.currentThread().getStackTrace());
        }
        getMyDataAccess(type).add(rec);
    }

    public void addLogbookRecord(ProjectRecord rec) throws EfaException {
        addRecord(rec, ProjectRecord.TYPE_LOGBOOK);
    }

    public void addClubworkBookRecord(ProjectRecord rec) throws EfaException {
        addRecord(rec, ProjectRecord.TYPE_CLUBWORK);
    }

    private void closePersistence(StorageObject p) {
        try {
            // It's ok to just close the storage object; if it wasn't open at all, close will do nothing
            p.close();
        } catch (Exception e) {
            Logger.log(Logger.ERROR, Logger.MSG_DATA_CLOSEFAILED,
                    LogString.fileCloseFailed(persistenceCache.toString(), p.getDescription(), e.toString()));
            Logger.log(e);
        }
    }

    public void closeAllStorageObjects() throws Exception {

        Logger.log(Logger.INFO, Logger.MSG_EVT_PROJECTCLOSED,
                LogString.fileClosing(this.getProjectName(), International.getString("Projekt")));    	

        // Close the message queue to the efacloud server
        // this is neccessary at this point as we close the persistence files next.
        // no efacloud action shall take place when files are getting closed.
        if ((getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD) && (TxRequestQueue.getInstance() != null)) {
            TxRequestQueue.getInstance().cancel();
        }        
        
        if (Logger.isTraceOn(Logger.TT_CLOUD, 1)) {
	        //for debug purposes:show number of Ecrids before closing the files.
	        Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFACLOUD, "EcridIndex size before closing persistence:"+Ecrid.iEcrids.size());
        }        
        // close all of this project's storage objects
        Set<String> keys = persistenceCache.keySet();
        for (String key : keys) {
            closePersistence(persistenceCache.get(key));
        }

        if (Logger.isTraceOn(Logger.TT_CLOUD, 1)) {
        	Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFACLOUD, "EcridIndex size after closing persistence:"+Ecrid.iEcrids.size());
        	if (Ecrid.iEcrids.size()>0) {
	        	Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFACLOUD, "EcridIndex contents:");
	            Set<String> myStringSet =Ecrid.iEcrids.keySet();
	            for (String id : myStringSet) {
	                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_EFACLOUD, id + " " + Ecrid.iEcrids.get(id).getKeyAsTextDescription() +" " + Ecrid.iEcrids.get(id).getClass().getName());
	            }
        	}
        }

        // close the project storage object itself
        closePersistence(this);
    }

    private String getPersistenceCacheKey(String storageObjectName, String storageObjectType) {
        return storageObjectName + "." + storageObjectType;
    }

    private StorageObject getPersistence(Class c, String storageObjectName, String storageObjectType, boolean createNewIfDoesntExist, String description) {
        return getPersistence(c, storageObjectName, storageObjectType, createNewIfDoesntExist, description, false);
    }

    private synchronized StorageObject getPersistence(Class c, String storageObjectName, String storageObjectType,
            boolean createNewIfDoesntExist, String description, boolean silent) {
        if (_inDeleteProject) {
            return null;
        }
        if (Logger.isTraceOn(Logger.TT_CORE, (createNewIfDoesntExist ? 5 : 9))) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_DATA, "Opening "
                    + (createNewIfDoesntExist ? "or Creating " : "")
                    + "Persistence for "
                    + storageObjectName + "." + storageObjectType + " ...");
        }
        StorageObject p = null;
        try {
            String key = getPersistenceCacheKey(storageObjectName, storageObjectType);
            p = persistenceCache.get(key);
            if (p != null) {
                if (!p.isOpen()) {
                    p.open(createNewIfDoesntExist);
                }
                return p; // fast path (would happen anyhow a few lines further down, but let's optimize for the most frequent use-case
            }
            if (p == null) {
                if (c == null) {
                    if (storageObjectType.equals(AutoIncrement.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_AUTOINCREMENT)) {
                        c = AutoIncrement.class;
                    }
                    if (storageObjectType.equals(SessionGroups.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_SESSIONGROUPS)) {
                        c = SessionGroups.class;
                    }
                    if (storageObjectType.equals(Persons.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_PERSONS)) {
                        c = Persons.class;
                    }
                    if (storageObjectType.equals(Status.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_STATUS)) {
                        c = Status.class;
                    }
                    if (storageObjectType.equals(Groups.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_GROUPS)) {
                        c = Groups.class;
                    }
                    if (storageObjectType.equals(Fahrtenabzeichen.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_FAHRTENABZEICHEN)) {
                        c = Fahrtenabzeichen.class;
                    }
                    if (storageObjectType.equals(Boats.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_BOATS)) {
                        c = Boats.class;
                    }
                    if (storageObjectType.equals(Crews.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_CREWS)) {
                        c = Crews.class;
                    }
                    if (storageObjectType.equals(BoatStatus.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_BOATSTATUS)) {
                        c = BoatStatus.class;
                    }
                    if (storageObjectType.equals(BoatReservations.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_BOATRESERVATIONS)) {
                        c = BoatReservations.class;
                    }
                    if (storageObjectType.equals(BoatDamages.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_BOATDAMAGES)) {
                        c = BoatDamages.class;
                    }
                    if (storageObjectType.equals(Destinations.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_DESTINATIONS)) {
                        c = Destinations.class;
                    }
                    if (storageObjectType.equals(Waters.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_WATERS)) {
                        c = Waters.class;
                    }
                    if (storageObjectType.equals(Statistics.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_STATISTICS)) {
                        c = Statistics.class;
                    }
                    if (storageObjectType.equals(Messages.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_MESSAGES)) {
                        c = Messages.class;
                    }
                    if (storageObjectType.equals(EfaCloudUsers.DATATYPE) && storageObjectName.equals(STORAGEOBJECT_EFACLOUDUSERS)) {
                        c = EfaCloudUsers.class;
                    }
                    if (storageObjectType.equals(Logbook.DATATYPE)) {
                        c = Logbook.class;
                    }
                    if (storageObjectType.equals(Clubwork.DATATYPE)) {
                        c = Clubwork.class;
                    }
                }
                if (c == null) {
                    return null;
                }
                p = (StorageObject) c.getConstructor(
                        int.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class).newInstance(
                        getProjectStorageType(),
                        getProjectStorageLocation(),
                        getProjectStorageUsername(),
                        getProjectStoragePassword(),
                        storageObjectName);
                p.setProject(this);
            }
            if (!p.isOpen()) {
                p.open(createNewIfDoesntExist);
            }
            if (p.isOpen()) {
                persistenceCache.put(key, p);
            }
            // we only have to do this in the slow path (usually when a new persistence object is created which hasn't been there before)
            p.data().setPreModifyRecordCallbackEnabled(data().isPreModifyRecordCallbackEnabled());
        } catch (Exception e) {
            if (!silent) {
                Logger.log(Logger.ERROR, Logger.MSG_DATA_OPENFAILED,
                        LogString.fileOpenFailed((p != null ? p.toString() : "<?>"), description, e.toString()));
                if (getProjectStorageType() != IDataAccess.TYPE_EFA_REMOTE) {
                    Logger.log(e);
                }
            }
            return null;
        }
        return p;
    }

    public IDataAccess getStorageObjectDataAccess(String storageObjectName, String storageObjectType,
            boolean createNewIfDoesntExist) {
        if (storageObjectType.equals(DATATYPE)) {
            if (getProjectStorageType() != IDataAccess.TYPE_EFA_REMOTE
                    && storageObjectName.equals(data().getStorageObjectName())) {
                return this.dataAccess;
            }
            if (getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE
                    && remoteDataAccess != null
                    && storageObjectName.equals(remoteDataAccess.getStorageObjectName())) {
                return this.remoteDataAccess;
            }
            return null; // it's another project
        }
        StorageObject so = getPersistence(null, storageObjectName, storageObjectType,
                createNewIfDoesntExist, storageObjectName + "." + storageObjectType, true);
        if (so != null) {
            return so.data();
        }
        return null;
    }

    public StorageObject getStorageObject(String storageObjectName, String storageObjectType, boolean createNewIfDoesntExist) {
        // it's important that we compare aginst storageObjectName, not against getProjectName(),
        // as the latter might not be fully initialized
        if (storageObjectName.equals(getName()) && storageObjectType.equals(DATATYPE)) {
            return this;
        }
        return this.getPersistence(null, storageObjectName, storageObjectType, createNewIfDoesntExist, "Remote Request", true);
    }

    public synchronized boolean isLogbookOpen(String logbookName) {
        try {
            String key = getPersistenceCacheKey(logbookName, Logbook.DATATYPE);
            if (key == null) {
                return false;
            }
            StorageObject p = persistenceCache.get(key);
            return (p != null && p.isOpen());
        } catch (Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }

    public synchronized boolean isClubworkOpen(String clubworkName) {
        try {
            String key = getPersistenceCacheKey(clubworkName, Clubwork.DATATYPE);
            if (key == null) {
                return false;
            }
            StorageObject p = persistenceCache.get(key);
            return (p != null && p.isOpen());
        } catch (Exception e) {
            Logger.logdebug(e);
            return false;
        }
    }

    public Logbook getLogbook(String logbookName, boolean createNewIfDoesntExist) {
        ProjectRecord rec = getLoogbookRecord(logbookName);
        if (rec == null) {
            return null;
        }
        Logbook logbook = (Logbook) getPersistence(Logbook.class, logbookName, Logbook.DATATYPE,
                createNewIfDoesntExist, International.getString("Fahrtenbuch"));
        if (logbook != null) {
            logbook.setName(logbookName);
            logbook.setProjectRecord(rec);
            if (Logger.isTraceOn(Logger.TT_CORE, 9)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_LOGBOOK,
                        "Project.getLogbook(" + logbookName + "): hash " + logbook.hashCode());
                Thread.currentThread().dumpStack();
            }
        }
        return logbook;
    }

    public Clubwork getClubwork(String name, boolean createNewIfDoesntExist) {
        ProjectRecord rec = getClubworkBookRecord(name);
        if (rec == null) {
            return null;
        }
        Clubwork clubwork = (Clubwork) getPersistence(Clubwork.class, name, Clubwork.DATATYPE,
                createNewIfDoesntExist, International.getString("Vereinsarbeit"));
        if (clubwork != null) {
            clubwork.setName(name);
            clubwork.setProjectRecord(rec);
            if (Logger.isTraceOn(Logger.TT_CORE, 9)) {
                Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_LOGBOOK,
                        "Project.getCLubwork(" + clubwork + "): hash " + clubwork.hashCode());
                Thread.currentThread().dumpStack();
            }
        }
        return clubwork;
    }

    public String[] getAllLogbookNames() {
        try {
            IDataAccess myAccess = getMyDataAccess(ProjectRecord.TYPE_LOGBOOK);
            if (myAccess == null) {
                return null; // happens for remote projects
            }
            DataKeyIterator it = myAccess.getStaticIterator();
            ArrayList<String> a = new ArrayList<String>();
            DataKey k = it.getFirst();
            while (k != null) {
                ProjectRecord r = (ProjectRecord) getMyDataAccess(ProjectRecord.TYPE_LOGBOOK).get(k);
                if (r != null && r.getType() != null
                        && r.getType().equals(ProjectRecord.TYPE_LOGBOOK)
                        && r.getName() != null && r.getName().length() > 0) {
                	DataTypeDate dat=r.getStartDate();
                	String datString=dat.getYear()+EfaUtil.int2String(dat.getMonth(),2)+EfaUtil.int2String(dat.getDay(),2);
                    a.add(datString+r.getName()); // add a YMD-Date of the startDate as prefix
                }
                k = it.getNext();
            }
            a.sort(new EfaSortStringDescComparator());
            String[] retVal=a.toArray(new String[0]);
            for (int i=0;i<retVal.length;i++) {
            	retVal[i]=retVal[i].substring(8); // cut the date which is prefix
            }
            return retVal;
        } catch (Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public String[] getAllClubworkNames() {
        try {
            IDataAccess myAccess = getMyDataAccess(ProjectRecord.TYPE_CLUBWORK);
            if (myAccess == null) {
                return null; // happens for remote projects
            }
            DataKeyIterator it = myAccess.getStaticIterator();
            ArrayList<String> a = new ArrayList<String>();
            DataKey k = it.getFirst();
            while (k != null) {
                ProjectRecord r = (ProjectRecord) getMyDataAccess(ProjectRecord.TYPE_CLUBWORK).get(k);
                if (r != null && r.getType() != null
                        && r.getType().equals(ProjectRecord.TYPE_CLUBWORK)
                        && r.getName() != null && r.getName().length() > 0) {
                   	DataTypeDate dat=r.getStartDate();
                	String datString=dat.getYear()+EfaUtil.int2String(dat.getMonth(),2)+EfaUtil.int2String(dat.getDay(),2);
                    a.add(datString+r.getName()); // add a YMD-Date of the startDate as prefix
                }
                k = it.getNext();
            }
            a.sort(new EfaSortStringDescComparator());
            String[] retVal=a.toArray(new String[0]);
            for (int i=0;i<retVal.length;i++) {
            	retVal[i]=retVal[i].substring(8); // cut the date which is prefix
            }
            return retVal;
        } catch (Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public int getHighestBoathouseId() {
        int max = 0;
        try {
            IDataAccess myAccess = getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE);
            if (myAccess == null) {
                return -1; // happens for remote projects
            }
            DataKeyIterator it = myAccess.getStaticIterator();
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                ProjectRecord r = (ProjectRecord) getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).get(k);
                if (r != null && r.getType() != null
                        && r.getType().equals(ProjectRecord.TYPE_BOATHOUSE)) {
                    max = Math.max(r.getBoathouseId(), max);
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
            return -1;
        }
        return max;
    }

    public String[] getAllBoathouseNames() {
        try {
            IDataAccess myAccess = getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE);
            if (myAccess == null) {
                return null; // happens for remote projects
            }
            DataKeyIterator it = myAccess.getStaticIterator();
            ArrayList<String> a = new ArrayList<String>();
            DataKey k = it.getFirst();
            while (k != null) {
                ProjectRecord r = (ProjectRecord) getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).get(k);
                if (r != null && r.getType() != null
                        && r.getType().equals(ProjectRecord.TYPE_BOATHOUSE)
                        && r.getName() != null && r.getName().length() > 0) {
                    a.add(r.getName());
                }
                k = it.getNext();
            }
            a.sort(new EfaSortStringComparator());
            return a.toArray(new String[0]);
        } catch (Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public int[] getAllBoathouseIds() {
        try {
            IDataAccess myAccess = getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE);
            if (myAccess == null) {
                return null; // happens for remote projects
            }
            DataKeyIterator it = myAccess.getStaticIterator();
            ArrayList<Integer> a = new ArrayList<Integer>();
            DataKey k = it.getFirst();
            while (k != null) {
                ProjectRecord r = (ProjectRecord) getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).get(k);
                if (r != null && r.getType() != null
                        && r.getType().equals(ProjectRecord.TYPE_BOATHOUSE)
                        && r.getName() != null && r.getName().length() > 0) {
                    a.add(r.getBoathouseId());
                }
                k = it.getNext();
            }
            int[] ia = new int[a.size()];
            for (int i = 0; i < ia.length; i++) {
                ia[i] = a.get(i);
            }
            return ia;
        } catch (Exception e) {
            Logger.logdebug(e);
            return null;
        }
    }

    public int getNumberOfBoathouses() {
        return getNumberOfBoathouses(false);
    }

    private int getNumberOfBoathouses(boolean forceUncached) {
        if (!forceUncached && numberOfBoathouses > 0) {
            return numberOfBoathouses;
        }
        int count = 0;
        try {
            IDataAccess myAccess = getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE);
            if (myAccess == null) {
                return 0;
            }
            DataKeyIterator it = myAccess.getStaticIterator();
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                ProjectRecord r = (ProjectRecord) getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).get(k);
                if (r != null && r.getType() != null
                        && r.getType().equals(ProjectRecord.TYPE_BOATHOUSE)) {
                    count++;
                }
            }
            if (!forceUncached && count > 0) {
                numberOfBoathouses = count;
            }
        } catch (Exception e) {
            Logger.logdebug(e);
            return 0;
        }
        return count;
    }

    private void refreshBoathouseIdToNameMapping() {
        boathouseIdToNameMapping = new Hashtable<Integer, String>();
        try {
            String[] names = getAllBoathouseNames();
            for (String name : names) {
                ProjectRecord r = getBoathouseRecord(name);
                if (r != null && r.getBoathouseId() > 0) {
                    boathouseIdToNameMapping.put(r.getBoathouseId(), name);
                }
            }
        } catch (Exception e) {
            Logger.logdebug(e);
        }
    }

    public AutoIncrement getAutoIncrement(boolean createNewIfDoesntExist) {
        return (AutoIncrement) getPersistence(AutoIncrement.class, STORAGEOBJECT_AUTOINCREMENT, AutoIncrement.DATATYPE,
                createNewIfDoesntExist, "AutoIncrement");
    }

    public SessionGroups getSessionGroups(boolean createNewIfDoesntExist) {
        return (SessionGroups) getPersistence(SessionGroups.class, STORAGEOBJECT_SESSIONGROUPS, SessionGroups.DATATYPE,
                createNewIfDoesntExist, International.getString("Fahrtgruppen"));
    }

    public Persons getPersons(boolean createNewIfDoesntExist) {
        return (Persons) getPersistence(Persons.class, STORAGEOBJECT_PERSONS, Persons.DATATYPE,
                createNewIfDoesntExist, International.getString("Personen"));
    }

    public Status getStatus(boolean createNewIfDoesntExist) {
        return (Status) getPersistence(Status.class, STORAGEOBJECT_STATUS, Status.DATATYPE,
                createNewIfDoesntExist, International.getString("Status"));
    }

    public Groups getGroups(boolean createNewIfDoesntExist) {
        return (Groups) getPersistence(Groups.class, STORAGEOBJECT_GROUPS, Groups.DATATYPE,
                createNewIfDoesntExist, International.getString("Gruppen"));
    }

    public Fahrtenabzeichen getFahrtenabzeichen(boolean createNewIfDoesntExist) {
        return (Fahrtenabzeichen) getPersistence(Fahrtenabzeichen.class, STORAGEOBJECT_FAHRTENABZEICHEN, Fahrtenabzeichen.DATATYPE,
                createNewIfDoesntExist, International.onlyFor("Fahrtenabzeichen", "de"));
    }

    public Boats getBoats(boolean createNewIfDoesntExist) {
        return (Boats) getPersistence(Boats.class, STORAGEOBJECT_BOATS, Boats.DATATYPE,
                createNewIfDoesntExist, International.getString("Boote"));
    }

    public Crews getCrews(boolean createNewIfDoesntExist) {
        return (Crews) getPersistence(Crews.class, STORAGEOBJECT_CREWS, Crews.DATATYPE,
                createNewIfDoesntExist, International.getString("Mannschaften"));
    }

    public BoatStatus getBoatStatus(boolean createNewIfDoesntExist) {
        return (BoatStatus) getPersistence(BoatStatus.class, STORAGEOBJECT_BOATSTATUS, BoatStatus.DATATYPE,
                createNewIfDoesntExist, International.getString("Bootsstatus"));
    }

    public BoatReservations getBoatReservations(boolean createNewIfDoesntExist) {
        return (BoatReservations) getPersistence(BoatReservations.class, STORAGEOBJECT_BOATRESERVATIONS, BoatReservations.DATATYPE,
                createNewIfDoesntExist, International.getString("Bootsreservierungen"));
    }

    public BoatDamages getBoatDamages(boolean createNewIfDoesntExist) {
        return (BoatDamages) getPersistence(BoatDamages.class, STORAGEOBJECT_BOATDAMAGES, BoatDamages.DATATYPE,
                createNewIfDoesntExist, International.getString("Bootsschäden"));
    }

    public Destinations getDestinations(boolean createNewIfDoesntExist) {
        return (Destinations) getPersistence(Destinations.class, STORAGEOBJECT_DESTINATIONS, Destinations.DATATYPE,
                createNewIfDoesntExist, International.getString("Ziele"));
    }

    public Waters getWaters(boolean createNewIfDoesntExist) {
        return (Waters) getPersistence(Waters.class, STORAGEOBJECT_WATERS, Waters.DATATYPE,
                createNewIfDoesntExist, International.getString("Gewässer"));
    }

    public Statistics getStatistics(boolean createNewIfDoesntExist) {
        return (Statistics) getPersistence(Statistics.class, STORAGEOBJECT_STATISTICS, Statistics.DATATYPE,
                createNewIfDoesntExist, International.getString("Statistiken"));
    }

    public Messages getMessages(boolean createNewIfDoesntExist) {
        return (Messages) getPersistence(Messages.class, STORAGEOBJECT_MESSAGES, Messages.DATATYPE,
                createNewIfDoesntExist, International.getString("Nachrichten"));
    }

    public EfaCloudUsers getEfaCloudUsers(boolean createNewIfDoesntExist) {
        return (EfaCloudUsers) getPersistence(EfaCloudUsers.class, STORAGEOBJECT_EFACLOUDUSERS, EfaCloudUsers.DATATYPE,
                createNewIfDoesntExist, International.getString("EfaCloud-Nutzer"));
    }

    public void setProjectDescription(String description) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setDescription(description);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    // set the storageType for this project's content
    public void setProjectStorageType(int storageType) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setStorageType(storageType);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public void setAdminName(String adminName) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setAdminName(adminName);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public void setAdminEmail(String adminEmail) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setAdminEmail(adminEmail);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public void setCurrentLogbookEfaBase(String currentLogbook) {
        try {
            ProjectRecord r = getBoathouseRecord();
            r.setCurrentLogbookEfaBase(currentLogbook);
            getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCurrentClubworkBookEfaBase(String currentClubwork) {
        try {
            ProjectRecord r = getBoathouseRecord();
            r.setCurrentClubworkEfaBase(currentClubwork);
            getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCurrentLogbookEfaBoathouse(String currentLogbook) {
        try {
            ProjectRecord r = getBoathouseRecord();
            r.setCurrentLogbookEfaBoathouse(currentLogbook);
            getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCurrentClubworkEfaBoathouse(String currentClubwork) {
        try {
            ProjectRecord r = getBoathouseRecord();
            r.setCurrentClubworkEfaBoathouse(currentClubwork);
            getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLastLogbookSwitch(String key) {
        try {
            ProjectRecord r = getProjectRecord();
            r.setLastLogbookSwitch(key);
            getMyDataAccess(ProjectRecord.TYPE_PROJECT).update(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLastWatersTemplateHash(String key) {
        try {
            ProjectRecord r = getProjectRecord();
            r.setLastWatersTamplateHash(key);
            getMyDataAccess(ProjectRecord.TYPE_PROJECT).update(r);
        } catch (Exception e) {
            // we suppress logging any exception here since this is
            // called from Audit and the error message window triggered
            // by the exception may not be focussable since we're in the
            // initialization of efa
            // e.printStackTrace();
        }
    }

    public void setAutoNewLogbookDate(DataTypeDate date) {
        try {
            ProjectRecord r = getBoathouseRecord();
            r.setAutoNewLogbookDate(date);
            getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAutoNewClubworkDate(DataTypeDate date) {
        try {
            ProjectRecord r = getBoathouseRecord();
            r.setAutoNewClubworkDate(date);
            getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAutoNewLogbookName(String name) {
        try {
            ProjectRecord r = getBoathouseRecord();
            r.setAutoNewLogbookName(name);
            getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAutoNewClubworkName(String name) {
        try {
            ProjectRecord r = getBoathouseRecord();
            r.setAutoNewClubworkName(name);
            getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setClubName(String clubName) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setClubName(clubName);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubAddressAdditional(String additional) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setAddressAdditional(additional);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubAddressStreet(String street) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setAddressStreet(street);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubAddressCity(String city) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setAddressCity(city);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubRegionalAssociationName(String name) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setRegionalAssociationName(name);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubRegionalAssociationMemberNo(String memberNo) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setRegionalAssociationMemberNo(memberNo);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubRegionalAssociationLogin(String login) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setRegionalAssociationLogin(login);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubGlobalAssociationName(String name) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setGlobalAssociationName(name);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubGlobalAssociationMemberNo(String memberNo) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setGlobalAssociationMemberNo(memberNo);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubGlobalAssociationLogin(String login) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setGlobalAssociationLogin(login);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubMemberOfDRV(boolean member) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setMemberOfDRV(member);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubMemberOfSRV(boolean member) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setMemberOfSRV(member);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubMemberOfADH(boolean member) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setMemberOfADH(member);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setBoathouseAreaId(int areaId) {
        try {
            ProjectRecord r = getBoathouseRecord();
            r.setAreaId(areaId);
            getMyDataAccess(ProjectRecord.TYPE_BOATHOUSE).update(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setClubKanuEfbUsername(String username) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setKanuEfbUsername(username);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubKanuEfbPassword(String password) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setKanuEfbPassword(password);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubKanuEfbLastSync(long lastSync) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            r.setKanuEfbLastSync(lastSync);
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    public void setClubLastDrvFaWsYear(int faYear, int wsYear) {
        long l = 0;
        IDataAccess access = getMyDataAccess(ProjectRecord.TYPE_CLUB);
        if (access == null) {
            return;
        }
        try {
            l = access.acquireLocalLock(getClubRecordKey());
            ProjectRecord r = getClubRecord();
            if (faYear > 0) {
                r.setLastDrvFaYear(faYear);
            }
            if (wsYear > 0) {
                r.setLastDrvWsYear(wsYear);
            }
            access.update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            access.releaseLocalLock(l);
        }
    }

    // set the storageLocation for this project's content
    public void setProjectStorageLocation(String storageLocation) {
        if (getProjectStorageType() == IDataAccess.TYPE_FILE_XML ||
            getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD) {
            // for file-based projects: storageLocation of content is always relative to this project file!
            storageLocation = null;
        }
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setStorageLocation(storageLocation);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public void setProjectStorageUsername(String storageUsername) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setStorageUsername(storageUsername);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public void setProjectStoragePassword(String storagePassword) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setStoragePassword(storagePassword);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public void setProjectEfaCloudURL(String efaCloudURL) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setEfaCloudURL(efaCloudURL);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public void setProjectRemoteProjectName(String projectName) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setRemoteProjectName(projectName);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public String getProjectEfaCloudURL() {
    	return getProjectRecord().getEfaCloudURL();
    }
    
    public void setProjectEfaOnlineConnect(boolean connectThroughEfaOnline) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setEfaOnlineConnect(connectThroughEfaOnline);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public void setProjectEfaOnlineUsername(String username) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setEfaOnlineUsername(username);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public void setProjectEfaOnlinePassword(String password) {
        long l = 0;
        try {
            l = data().acquireLocalLock(getProjectRecordKey());
            ProjectRecord r = getProjectRecord();
            r.setEfaOnlinePassword(password);
            data().update(r, l);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data().releaseLocalLock(l);
        }
    }

    public String getProjectName() {
        try {
            return getProjectRecord().getProjectName();
        } catch (Exception e) {
            // can happen while opening a remote project
            return "";
        }
    }

    public String getProjectDescription() {
        return getProjectRecord().getDescription();
    }

    // get the storageType for this project's content
    public int getProjectStorageType() {
        return getProjectRecord().getStorageType();
    }
    
    public boolean getIsProjectStorageTypeEfaCloud() {
    	return getProjectStorageType() == IDataAccess.TYPE_EFA_CLOUD;
    }

    public String getProjectStorageTypeTypeString() {
        switch (getProjectStorageType()) {
            case IDataAccess.TYPE_FILE_XML:
                return IDataAccess.TYPESTRING_FILE_XML;
            case IDataAccess.TYPE_EFA_REMOTE:
                return IDataAccess.TYPESTRING_EFA_REMOTE;
            case IDataAccess.TYPE_EFA_CLOUD:
                return IDataAccess.TYPESTRING_EFA_CLOUD;
        }
        return null;
    }

    // get the storageLocation for this project's content
    public String getProjectStorageLocation() {
        if (getProjectStorageType() == IDataAccess.TYPE_FILE_XML) {
            // for file-based projects: storageLocation of content is always relative to this project file!
            return dataAccess.getStorageLocation() + getProjectName() + Daten.fileSep;
        }
        if (getProjectStorageType() == IDataAccess.TYPE_EFA_REMOTE
                && getProjectEfaOnlineConnect()) {
            String location = EfaOnlineClient.getRemoteAddress(getProjectEfaOnlineUsername(), getProjectEfaOnlinePassword());
            if (location != null) {
                return location;
            }
        }
        return getProjectRecord().getStorageLocation();
    }

    public String getProjectStorageUsername() {
        return getProjectRecord().getStorageUsername();
    }

    public String getProjectStoragePassword() {
        return getProjectRecord().getStoragePassword();
    }

    public String getProjectRemoteProjectName() {
        return getProjectRecord().getRemoteProjectName();
    }

    public boolean getProjectEfaOnlineConnect() {
        return getProjectRecord().getEfaOnlineConnect();
    }

    public String getProjectEfaOnlineUsername() {
        return getProjectRecord().getEfaOnlineUsername();
    }

    public String getProjectEfaOnlinePassword() {
        return getProjectRecord().getEfaOnlinePassword();
    }

    public String getAdminName() {
        return getProjectRecord().getAdminName();
    }

    public String getAdminEmail() {
        return getProjectRecord().getAdminEmail();
    }

    public String getCurrentLogbookEfaBase() {
        try {
            return getBoathouseRecord().getCurrentLogbookEfaBase();
        } catch (NullPointerException e) {
            Logger.logdebug(e); // can happen when remote project is not yet open
            return null;
        }
    }

    public String getCurrentClubworkEfaBase() {
        try {
            return getBoathouseRecord().getCurrentClubworkEfaBase();
        } catch (NullPointerException e) {
            Logger.logdebug(e); // can happen when remote project is not yet open
            return null;
        }
    }

    public String getCurrentLogbookEfaBoathouse() {
        try {
            return getBoathouseRecord().getCurrentLogbookEfaBoathouse();
        } catch (NullPointerException e) {
            Logger.logdebug(e); // can happen when remote project is not yet open
            return null;
        }
    }

    public String getCurrentClubworkEfaBoathouse() {
        try {
            return getBoathouseRecord().getCurrentClubworkEfaBoathouse();
        } catch (NullPointerException e) {
            Logger.logdebug(e); // can happen when remote project is not yet open
            return null;
        }
    }

    public Logbook getCurrentLogbook() {
        String name = null;
        if (Daten.applID == Daten.APPL_EFABASE) {
            name = getCurrentLogbookEfaBase();
        }
        if (Daten.applID == Daten.APPL_EFABH) {
            name = getCurrentLogbookEfaBoathouse();
        }
        if (name != null && name.length() > 0) {
            return getLogbook(name, false);
        }
        return null;
    }

    public Clubwork getCurrentClubwork() {
        String name = null;
        if (Daten.applID == Daten.APPL_EFABASE) {
            name = getCurrentClubworkEfaBase();
        }
        if (Daten.applID == Daten.APPL_EFABH) {
            name = getCurrentClubworkEfaBoathouse();
        }
        if (name != null && name.length() > 0) {
            return getClubwork(name, false);
        }
        return null;
    }
    
    // If EFACLI wants to get data for clubwork, we have to define wether to use clubwork defined in efabase
    // or in efaboathouse. Although I do not know why there is a difference yet.

    public Clubwork getCurrentClubworkForCLI(boolean useClubworkFromBoathouse) {
        String name = null;
        if (useClubworkFromBoathouse ) {
            name = getCurrentClubworkEfaBoathouse();
        } else {
            name = getCurrentClubworkEfaBase();
        }        

        if (name != null && name.length() > 0) {
            return getClubwork(name, false);
        }
        return null;

    }
    public String getClubName() {
        return getClubRecord().getClubName();
    }

    public String getClubAddressAdditional() {
        return getClubRecord().getAddressAdditional();
    }

    public String getClubAddressStreet() {
        return getClubRecord().getAddressStreet();
    }

    public String getClubAddressCity() {
        return getClubRecord().getAddressCity();
    }

    public String getCompetitionSubmitterName() {
        return getClubRecord().getAdminName();
    }

    public String getCompetitionSubmitterEmail() {
        return getClubRecord().getAdminEmail();
    }

    public String getClubRegionalAssociationName() {
        return getClubRecord().getRegionalAssociationName();
    }

    public String getClubRegionalAssociationMemberNo() {
        return getClubRecord().getRegionalAssociationMemberNo();
    }

    public String getClubRegionalAssociationLogin() {
        return getClubRecord().getRegionalAssociationLogin();
    }

    public String getClubGlobalAssociationName() {
        return getClubRecord().getGlobalAssociationName();
    }

    public String getClubGlobalAssociationMemberNo() {
        return getClubRecord().getGlobalAssociationMemberNo();
    }

    public String getClubGlobalAssociationLogin() {
        return getClubRecord().getGlobalAssociationLogin();
    }

    public boolean getClubMemberOfDRV() {
        return getClubRecord().getMemberOfDRV();
    }

    public boolean getClubMemberOfSRV() {
        return getClubRecord().getMemberOfSRV();
    }

    public boolean getClubMemberOfADH() {
        return getClubRecord().getMemberOfADH();
    }

    public int getBoathouseAreaID() {
        return getBoathouseRecord().getAreaId();
    }

    public double getClubDefaultClubworkTargetHours() {
        return getClubRecord().getDefaultClubworkTargetHours();
    }

    public double getClubTransferableClubworkHours() {
        return getClubRecord().getTransferableClubworkHours();
    }

    public String getClubKanuEfbUsername() {
        return getClubRecord().getKanuEfbUsername();
    }

    public String getClubKanuEfbPassword() {
        return getClubRecord().getKanuEfbPassword();
    }

    public long getClubKanuEfbLastSync() {
        return getClubRecord().getKanuEfbLastSync();
    }

    public DataTypeDate getAutoNewLogbookDate() {
        return getBoathouseRecord().getAutoNewLogbookDate();
    }

    public DataTypeDate getAutoNewClubworkDate() {
        return getBoathouseRecord().getAutoNewLogbookDate();
    }

    public String getAutoNewLogbookName() {
        return getBoathouseRecord().getAutoNewLogbookName();
    }

    public String getLastLogbookSwitch() {
        return getProjectRecord().getLastLogbookSwitch();
    }

    public String getLastWatersTemplateHash() {
        return getProjectRecord().getLastWatersTemplateHash();
    }

    public String getAutoNewClubworkName() {
        return getBoathouseRecord().getAutoNewLogbookName();
    }

    public int getClubLastDrvFaYear() {
        return getClubRecord().getLastDrvFaYear();
    }

    public int getClubLastDrvWsYear() {
        return getClubRecord().getLastDrvWsYear();
    }

    public synchronized void setPreModifyRecordCallbackEnabled(boolean enabled) {
        this.data().setPreModifyRecordCallbackEnabled(enabled);
        Set<String> keys = persistenceCache.keySet();
        for (String key : keys) {
            persistenceCache.get(key).data().setPreModifyRecordCallbackEnabled(enabled);
        }
    }

    public void preModifyRecordCallback(DataRecord record, boolean add, boolean update, boolean delete) throws EfaModifyException {
        myBoathouseName = null;
        myBoathouseId = -1;
        numberOfBoathouses = -1;
        Hashtable<Integer, String> boathouseIdToNameMapping = null;
        if (add || update) {
            assertFieldNotEmpty(record, ProjectRecord.TYPE);
            assertUnique(record, ProjectRecord.PROJECTNAME);
            assertUnique(record, new String[]{ProjectRecord.TYPE, ProjectRecord.NAME});
            assertUnique(record, ProjectRecord.BOATHOUSEID);
            if (((ProjectRecord) record).getType().equals(ProjectRecord.TYPE_LOGBOOK)
                    || ((ProjectRecord) record).getType().equals(ProjectRecord.TYPE_CLUBWORK)
                    || ((ProjectRecord) record).getType().equals(ProjectRecord.TYPE_BOATHOUSE)) {
                assertFieldNotEmpty(record, ProjectRecord.NAME);
            }
            if (((ProjectRecord) record).getType().equals(ProjectRecord.TYPE_PROJECT)) {
                ProjectRecord r = (ProjectRecord) record;
                String email = r.getAdminEmail();
                if (email != null && email.trim().length() > 0 && !EfaUtil.isValidEmail(email)) {
                    throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                            International.getMessage("Ungültige email Adresse '{email}' in Feld '{field}'.",
                                    email, ProjectRecord.ADMINEMAIL),
                            Thread.currentThread().getStackTrace());
                }
            }
            if (((ProjectRecord) record).getType().equals(ProjectRecord.TYPE_BOATHOUSE)) {
                ProjectRecord r = (ProjectRecord) record;
                String lName = r.getAutoNewLogbookName();
                if (lName != null && lName.length() > 0 && getLoogbookRecord(lName) == null) {
                    throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                            "Logbook " + lName + " not found!",
                            Thread.currentThread().getStackTrace());
                }
                if (r.getBoathouseIdentifier() != null && r.getBoathouseIdentifier().length() > 0 &&
                    r.getBoathouseId() < 0) {
                        int lastId = getHighestBoathouseId();
                        if (lastId >= 0) {
                            r.setBoathouseId(lastId + 1);
                        }
                }
            }
        }
        if (delete) {
            if (((ProjectRecord) record).getType().equals(ProjectRecord.TYPE_LOGBOOK)) {
                ProjectRecord r = (ProjectRecord) record;
                String lName = getAutoNewLogbookName();
                if (lName != null && lName.length() > 0 && r.getName().equals(lName)) {
                    throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                            International.getMessage("Der Datensatz kann nicht gelöscht werden, da er noch von {listtype} '{record}' genutzt wird.",
                            International.getString("Fahrtenbuchwechsel"), lName),
                            Thread.currentThread().getStackTrace());
                }
            }
            if (((ProjectRecord) record).getType().equals(ProjectRecord.TYPE_CLUBWORK)) {
                ProjectRecord r = (ProjectRecord) record;
                String lName = getAutoNewClubworkName();
                if (lName != null && lName.length() > 0 && r.getName().equals(lName)) {
                    throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                            International.getMessage("Der Datensatz kann nicht gelöscht werden, da er noch von {listtype} '{record}' genutzt wird.",
                            International.getString("Vereinsarbeitsbuchwechsel"), lName),
                            Thread.currentThread().getStackTrace());
                }
            }
            if (((ProjectRecord) record).getType().equals(ProjectRecord.TYPE_BOATHOUSE)) {
                if (getNumberOfBoathouses(true) == 1) {
                    throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                            International.getMessage("Eintrag kann nicht gelöscht werden, da mindestens {count} Einträge verbleiben müssen.",
                            1),
                            Thread.currentThread().getStackTrace());
                }
                int bid = ((ProjectRecord) record).getBoathouseId();
                try {
                    Destinations dest = getDestinations(false);
                    DataKey[] k = dest.data().getByFields(new String[]{DestinationRecord.ONLYINBOATHOUSEID},
                            new Object[]{Integer.toString(bid)}, System.currentTimeMillis());
                    if (k != null && k.length > 0) {
                        DestinationRecord dr = (DestinationRecord) dest.data().get(k[0]);
                        throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                                International.getMessage("Der Datensatz kann nicht gelöscht werden, da er noch von {listtype} '{record}' genutzt wird.",
                                International.getString("Ziel"), (dr != null ? dr.getQualifiedName() : "<unknown>")),
                                Thread.currentThread().getStackTrace());
                    }
                    BoatStatus status = getBoatStatus(false);
                    k = status.data().getByFields(new String[]{BoatStatusRecord.ONLYINBOATHOUSEID},
                            new Object[]{Integer.toString(bid)});
                    if (k != null && k.length > 0) {
                        BoatStatusRecord br = (BoatStatusRecord) status.data().get(k[0]);
                        throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                                International.getMessage("Der Datensatz kann nicht gelöscht werden, da er noch von {listtype} '{record}' genutzt wird.",
                                International.getString("Bootsstatus"), (br != null ? br.getQualifiedName() : "<unknown>")),
                                Thread.currentThread().getStackTrace());
                    }

                } catch (Exception e) {
                    Logger.logdebug(e);
                    throw new EfaModifyException(Logger.MSG_DATA_MODIFYEXCEPTION,
                            e.toString(),
                            Thread.currentThread().getStackTrace());
                }
            }
        }
    }

    public String[] makeBoathouseArray(int type) {
        String[] bh = Daten.project.getAllBoathouseNames();
        switch (type) {
            case EfaTypes.ARRAY_STRINGLIST_VALUES:
                String[] v = new String[bh == null ? 1 : bh.length + 1];
                v[0] = "";
                for (int i = 0; bh != null && i < bh.length; i++) {
                    ProjectRecord r = Daten.project.getBoathouseRecord(bh[i]);
                    v[i + 1] = (r != null ? Integer.toString(r.getBoathouseId()) : "-1");
                }
                return v;
            case EfaTypes.ARRAY_STRINGLIST_DISPLAY:
                String[] d = new String[bh == null ? 1 : bh.length + 1];
                d[0] = International.getString("alle");
                for (int i = 0; bh != null && i < bh.length; i++) {
                    d[i + 1] = bh[i];
                }
                return d;
        }
        return null;
    }
}
