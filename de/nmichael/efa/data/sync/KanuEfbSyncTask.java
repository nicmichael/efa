/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 * 
 * Update 06/2022  - 07/2023
 * 
 * Die Anmerkungen sind auf Deutsch, da dieser Code nur für die Synchronisation mit dem deutschen Kanu-EFB
 * enutzt werden kann.
 *   
 * - SyncWaters and SyncBoats sind von der EFB-Schnittstelle nicht mehr unterstützt und daher entfallen.
 *   --------------------
 *   EFB ignorierte die Daten, die über die Methoden gesendet wurden, und lieferte leere Antworten.
 *   
 * - SyncUsers ist wie in früheren Versionen von EFA nur eine "leere" Hülle und wurde entfernt.
 *   --------------------
 *   Hier wurden in früheren Versionen keine Nutzerdaten übertragen, der entsprechende Nutzcode war in der 
 *   Methode auskommentiert. Die Synchronisation EFA->EFB funktioniert trotzdem, da sich EFA ausschließlich 
 *   auf die bei der Person eingetragenen Kanu-EFB-ID verlässt.
 *    
 *   SyncUsers wurde im aktuellen Arbeitsstand aus dem Code entfernt, da es beim Synchronisieren der Fahrten
 *   nicht mehr zum Einsatz kommen wird. 
 *   Derzeit würde SyncUsers ein DSGVO-Problem darstellen, denn die dahinter stehende Logik würde derzeit 
 *   alle Personendaten ohne EFB-ID in das EFB übertragen. Auch wenn hier keine stillen Accounts mehr angelegt werden,
 *   ist die Übertragung von Personendaten in ein anderes System "Cloud" ohne Einverständnis der Person
 *   nach DSGVO ein Datenschutzverstoss. 
 *   
 * - kanuEfb_Fullsync - neuer EXPERT Konfigurationsparameter, default false
 *   --------------------
 *   Hierüber kann der Anwender anfordern, dass EFA grundsätzlich alle Fahrten im Fahrtenbuch als Grundlage
 *   für die Übermittlung an EFB nutzt. Ist der Parameter nicht gesetzt, werden nur neue oder aktualisierte
 *   Fahrten für die Übermittlung genutzt.
 *   
 *   Das Setzen dieses Parameters ist (bedarfsweise) sinnvoll, wenn ein Clubmitglied nachträglich eine EFB-ID
 *   eingetragen bekommt. Denn dann werden zwar klassischerweise alle Fahrten neu synchronisiert, bei denen
 *   dieses Clubmitglied alleine im Boot gesessen hat. Hat das Clubmitglied aber bei einem mehrsitzigen Boot
 *   eine Fahrt mitgemacht, dann wurde diese Fahrt nicht mehr als zu synchronisieren erkannt.
 *   Durch Setzen dieses Parameters werden auch solche Mehr-Personen-Fahrten nachträglich mit synchronisiert.
 *   
 * - kanuEfB_SyncTripsAfterDate - neuer EXPERT Konfigurationsparameter, default 01.01.1970
 *   --------------------
 *   EFA berücksichtigt nur Fahrten aus dem aktuellen Fahrtenbuch, deren Beginndatum größer oder 
 *   gleich dem hier eingetragenen Datum sind.
 *  
 *   Beginnt man als Club erstmalig mit der Synchronisation des EFA-Fahrtenbuchs an EFB, so muss man
 *   aufpassen, welchen Zeitraum das Fahrtenbuch abdeckt. Die Kanu-Saison geht vom 01.10. bis zum 30.09. des
 *   Folgejahres. Wenn nun aber das EFA-Fahrtenbuch klassischerweise vom 01.01.-31.12. geht, kommen auch Fahrten
 *   außerhalb der Saison bei der EFA-EFB-Synchronisation in Betracht. Dies ist nicht zwingend erwünscht.
 *   
 *   Da beim erstmaligen Start das EFA noch nie Fahrten in das EFB übertragen hat, sind alle Fahrten aus
 *   dem Fahrtenbuch neue Fahrten. Und kommen daher für die Übertragung in das EFB in Betracht.
 *   Hat ein Mitglied aber schon eigenständig das persönliche EFB-Fahrtenbuch gepflegt, und werden 
 *   durch Eintragung einer efb-Kanu-ID für dieses Mitglied dessen Fahrten in das EFB übertragen, dann 
 *   entstehen im EFB Fahrtdubletten, die das Mitglied selbst im EFB auflösen muss.
 *   
 *   Um dies bei der Einführung der EFA-EFB-Synchronisation zu verhindern, oder zumindest die Anzahl der 
 *   doppelten Einträge zu beschränken, kann man hier dasjenige Datum eintragen, ab dem die Fahrten synchronisiert
 *   werden sollen.
 *  
 * - kanuEfb_boatTypes - PUBLIC Konfigurationsparameter Default "Kanu-Bootstypen"
 *   --------------------
 *   Dieser Parameter gibt an, auf welchem Bootstyp eine Fahrt hat stattfinden müssen, damit sie für eine
 *   Synchronisation in das EFB in Betracht kommt.
 *   
 *   Dies ist nützlich für Vereine, die sowohl Kanu wie z.B. auch Rudern anbieten - und wo dann in das 
 *   EFB-Fahrtenbuch ausschließlich Kanufahrten übertragen werden sollen.
 *   
 *   In früheren Versionen von EFA wurde dieser Parameter nicht gespeichert. Das Setzen ist aber erforderlich,
 *   um neue Kanutypen wie z.B. SUP zu unterstützen.
 *   
 *  - kanuEfb_SyncUnknownBoats - PUBLIC Konfigurationsparameter Default false
 *    --------------------
 *    Die Synchronisation versucht anhand der Bootstypen zu erkennen, ob eine Synchronisation der Fahrt
 *    mit dem EFB erfolgen soll, oder nicht. Das bedeutet im Umkehrschluss, dass nur Fahrten auf bekannten Booten
 *    mit dem EFB synchronisiert werden.
 *    
 *    Nutzt nun ein Vereinsmitglied ein Boot, das nicht im Verein gelagert ist, und trägt die Fahrt ein,
 *    dann wird diese standardmäßig nicht mit synchronisert, und das EFB Fahrtenbuch ist unvollständig.
 *   
 *    Über diesen Parameter kann bestimmt werden, dass Fahrten mit "unbekannten", d.h. nicht in der EFB-Datenbank   
 *    enthaltenen Booten synchronisiert werden. Dies passiert allerdings nur dann, wenn mindestens eine Person
 *    der Crew über eine KanuEFB-ID verfügt, und nur für die Personen mit KanuEFB-ID werden die Fahrten ins EFB übertragen.
 *   
 *  - kanuEfb_tidyXML - EXPERT Konfigurationsparameter Default false
 *    -------------------- 
 *    Das EFB-Schulungssystem wird immer mal wieder in einen Debugmodus gesetzt.
 *    Wann das passiert, darauf hat der Nutzer des EFB-Schulungs-System keinen Einfluss. 
 *    Wenn der Debugmodus im EFB aktiv ist, dann funktioniert die Synchronisation der Fahrten aus EFA 
 *    nicht und bricht ab. Dies stört bei der Entwicklung oder auch beim Test der Synchronisation von 
 *    Vereinen auf dem Schulungssystem erheblich.
 *    
 *    Grund für den Abbruch: In diesem Debugmodus schickt das EFB bei der Antwort auf syncTrips() vor dem 
 *    eigentlichen XML-Datenstrom weitere Texte, die die Verarbeitung durch EFA stören. 
 *    Daher wurde tidyXML in getResponse eingebaut; die Bereinigung findet nur dann statt, 
 *    wenn der Konfigurationsparameter für tidyXML TRUE ist.
 *           
 * - syncTrips 
 *    -------------------- 
 *   - Gibt beim Start die drei Konfigurationsparameter aus.
 *   - Gibt zum Abschluss eine Statistik aus, wieviele Fahrten den einzelnen Bedingungen für eine Synchronisation  
 *     entsprechen. So ist für EFA-Admins besser nachvollziehbar, warum bestimmte Fahrten (nicht) in das EFB
 *     übertragen wurden.
 *   - Unterstützt nun tatsächlich einen wählbaren Differential-Modus für die Synchronisation
 *     In früheren Versionen war dies zwar beabsichtigt, kam aber wegen einem Logik-Fehler nicht zum Tragen.
 *   - Synchronisiert KEINE Fahrten
 *   	- die zwar eine Start-Uhrzeit, aber keine End-Uhrzeit haben
 *        Dies sind im EFA alle noch nicht beendeten Fahrten. Das EFB weist solche Fahrten mit einem Fehler ab.
 *        Daher wurden diese Fahrten vom Synchronisationsversuch ausgeschlossen.
 *      - die auf einem Motorboot oder Ergometer stattgefunden haben.
 *        Dies ist unabhängig von der Konfiguration der Bootstypen, für die die Synchronisation explizit gewünscht ist.
 *      - deren Startdatum < Konfigurationsparameter kanuEfB_SyncTripsAfterDate ist.
 *      - bei denen keines der Crewmitglieder eine Kanu-EFB-ID hat.
 *        (wird als INFO geloggt, wenn Verbose-Mode aktiv)
 *      - bei denen das boot keinen Bootstyp hat (dann ist das Boot nicht in der Bootsliste, oder es ist dort nicht vollständig erfasst.
 *        Der Bootstyp ist relevant, da man ja nur Boote mit bestimmten Typen synchronisieren will.
 *        (wird als INFO geloggt, wenn Verbose-Mode aktiv).
 *        
 *    - speichert bei jeder synchronisierten Fahrt den Zeitstempel, bei dem die letzte erfolgreiche Synchronisation
 *      stattgefunden hat.
 *      
 *    - kann keine gelöschten Fahrten aus dem EFB entfernen. 
 *      Die EFB-Schnittstelle gäbe dies zwar her, aber EFA kann sich keine gelöschten Fahrten merken.
 *      Daher können diese aus dem EFB auch nicht wieder entfernt werden.
 *      Es gibt auch (noch) keinen Audit-Modus, in dem man die im EFB gespeicherten Fahrten abrufen
 *      und gegen die im EFA gespeicherten Fahrten abgleichen könnte.
 *      
 *  - isCanoeBoatType(BoatRecord) 
 *    -------------------- 
 *    wurde aus efaConfig in die EFB-Synchronisation verschoben, um aus efaConfig eine Abhängigkeit
 *    zu BoatRecord zu vermeiden.
 *    
 *  - EFA CLI EFB Sync
 *    --------------------   
 *    - Parameter -verbose kann angegeben werden, um nicht synchronisierbare Fahrten als Info-Meldungen in das Log auszugeben.
 *    
 *      
 *  Allgemeine Fragen und Antworten:
 *  a) Muss man im EFB eine Bootsliste pflegen, oder im EFA die Kanu-EFB-ID für das Boot eintragen?
 *     
 *     Nein. Bei der Synchronisation werden die Bootsnamen im Klartext übersendet.    
 *     In früheren Versionen von EFA war vorgesehen, dass die Kanu-EFB-ID für das Boot im Rahmen des 
 *     Synchronisationsvorgang von EFA selbst automatisch übernommen wird.
 *     
 *     Das gleiche gilt für den Namen des Gewässers, der bei jeder Fahrtsynchronisation im Klartext
 *     in das EFB übertragen wird.
 *     
 *  b) Welche Informationen muss man für Personen im EFA pflegen, damit diese bei einer Synchronisation
 *     -> EFB berücksichtigt werden?
 *     
 *     Es ist für die aktuelle Version lediglich die Eintragung der EFB-ID für die Person erforderlich,
 *     damit sie für die EFB-Synchronisation in Frage kommt. Fehlt diese EFB-ID, oder wird sie wieder entfernt,
 *     werden keine neue Fahrten für die Person im EFA mehr in das EFB übertragen.
 *     
 *     Es findet bei der Synchronisation derzeit KEIN Abgleich gegen den Namen, Vornamen oder Geburtsdatum statt.
 *     Dementsprechend ist es wichtig, bei der Eintragung der EFB-ID keine Fehler zu machen, oder 
 *     die EFB-ID bei mehreren Personen zeitgleich einzutragen.
 *     
 *     Es ist empfehlenswert, für mögliche spätere Ausbaustufen der EFA->EFB Synchronisation 
 *     trotzdem Name, Vorname und Geburtsdatum im EFA einzutragen, denn es ist ein Audit Task geplant,
 *     der die Namen und EFB-IDs gegeneinander abgleicht.
 *     
 *  c) Das EFB zeigt auch eine EFB-ID für Personen an, die ein stilles EFB-Konto haben. 
 *     Können auch Fahrten für solche Personen in das EFB übertragen werden?
 *     
 *     Nein. Man kann zwar die EFB-ID für diese stillen Konten im EFA bei der betreffenden Person eintragen,
 *     und EFA wird versuchen, diese Fahrt zu synchronisieren. Diese Fahrt wird aber vom EFB mit einem 
 *     Fehler abgewiesen.
 */

package de.nmichael.efa.data.sync;

// @i18n complete
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JDialog;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.AdminRecord;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.DestinationRecord;
import de.nmichael.efa.data.Destinations;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.Persons;
import de.nmichael.efa.data.ProjectRecord;
import de.nmichael.efa.data.SessionGroupRecord;
import de.nmichael.efa.data.Waters;
import de.nmichael.efa.data.WatersRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.storage.IDataAccess;
import de.nmichael.efa.data.types.DataTypeList;
import de.nmichael.efa.gui.BaseTabbedDialog;
import de.nmichael.efa.gui.EfaConfigDialog;
import de.nmichael.efa.gui.ProgressDialog;
import de.nmichael.efa.gui.dataedit.ProjectEditDialog;
import de.nmichael.efa.util.Dialog;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.LogString;
import de.nmichael.efa.util.Logger;
import de.nmichael.efa.util.ProgressTask;


public class KanuEfbSyncTask extends ProgressTask {
    
    private static final int DEBUG_MARK_SIZE = 10*1024*1024;

    private AdminRecord admin;
    private Logbook logbook;
    private String loginurl;
    private String cmdurl;
    private String username;
    private String password;
    private HttpCookie sessionCookie;
    private long lastSync;
    private long thisSync;
    private boolean loggedIn = false;
    private boolean successfulCompleted = false;
    private int countSyncUsers = 0;
    private int countSyncTrips = 0;
    private int countWarnings = 0;
    private int countErrors = 0;
    private boolean verboseMode=false;

    TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                //No need to implement.
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                //No need to implement.
            }
        }
    };

    public KanuEfbSyncTask(Logbook logbook, AdminRecord admin, boolean verbose) {
        super();
        this.admin = admin;
        getConfigValues();
        this.logbook = logbook;
        this.verboseMode=verbose;
    }

    private void getConfigValues() {
        this.loginurl = Daten.efaConfig.getValueKanuEfb_urlLogin();
        this.cmdurl = Daten.efaConfig.getValueKanuEfb_urlRequest();
        this.username = Daten.project.getClubKanuEfbUsername();
        this.password = Daten.project.getClubKanuEfbPassword();
        this.lastSync = Daten.project.getClubKanuEfbLastSync();
        if (this.lastSync == IDataAccess.UNDEFINED_LONG) {
            this.lastSync = 0;
        }
    }

    private void buildRequestHeader(StringBuilder s, String requestName) {
        s.append("<?xml version='1.0' encoding='UTF-8' ?>\n");
        s.append("<xml>\n");
        s.append("<request command=\""+requestName+"\">\n");
    }

    private void buildRequestFooter(StringBuilder s) {
        s.append("</request>\n");
        s.append("</xml>\n");
    }

    private KanuEfbXmlResponse sendRequest(String request, boolean expectResponse) throws Exception {
        if (Logger.isTraceOn(Logger.TT_SYNC)) {
            logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Sende Synchronisierungs-Anfrage an "+cmdurl+":\n"+request);
        }
        URL url = new URL(this.cmdurl);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Cookie", (sessionCookie != null ? sessionCookie.toString() : "null"));
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        out.write("xmlCode=" + URLEncoder.encode(request, "UTF-8"));
        out.flush();
        out.close();

        if (expectResponse) {
            try {
                return getResponse(connection, new BufferedInputStream(connection.getInputStream()));
            } catch(Exception e) {
                logInfo(Logger.ERROR, Logger.MSG_SYNC_SYNCDEBUG,
                            "Fehler bei Kommunikation mit "+cmdurl+": "+e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    private KanuEfbXmlResponse getResponse(URLConnection connection, BufferedInputStream in) {
        if (Logger.isTraceOn(Logger.TT_SYNC)) {
            try {
                in.mark(DEBUG_MARK_SIZE);
                logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Antwort von Kanu-eFB:");
                logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "    -- HEADER START --");
                Map<String, List<String>> m = connection.getHeaderFields();
                for (String header : m.keySet()) {
                    logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "    " + header + "=" + connection.getHeaderField(header));
                }
                logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "    -- HEADER END --");
                BufferedReader buf = new BufferedReader(new InputStreamReader(in));
                String s;
                logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "    -- RESPONSE START --");
                while ((s = buf.readLine()) != null) {
                    logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "   " + s);
                }
                logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "    -- RESPONSE END --");
                in.reset();
            } catch (Exception e) {
                Logger.log(e);
            }
        }

        // Auf Schulungssystemen, wo im EFB der Debugmodus aktiv ist, 
        // liefert die Response weitere (nicht verarbeitbare) Daten vor dem eigentlichen XML-Datenstrom.
        // Abhängig von der Konfiguration bereinigen wir diese Daten.
        if (Daten.efaConfig.getValueKanuEfb_TidyXML()) {
        	in=tidyXML(in);
        }
        
        KanuEfbXmlResponse response = null;
        try {
            XMLReader parser = EfaUtil.getXMLReader();
            response = new KanuEfbXmlResponse(this);
            parser.setContentHandler(response);
            parser.parse(new InputSource(in));
        } catch(Exception e) {
            Logger.log(e);
            if (Logger.isTraceOn(Logger.TT_SYNC)) {
                logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Exception:" + e.toString());
            }
            response = null;
        }

        if (Logger.isTraceOn(Logger.TT_SYNC) && response != null) {
            response.printAll();
        }
        
        return response;
    }

    /**
     * On EFB training sites, sometimes debug mode is active. This enriches the XML response stream
     * with some annoying HTML entities at the beginning, describing the response output.
     * As this is not conform to any standards, EFB synchronisation fails when debug mode is active on the EFB training site.
     * 
     * This code simply ignores everything within the XML response stream which is located before the opening xml or ?xml tags.
     *  
     * @param in InputStream containing the XML response
     * @return  InputStream with the pure XML response.
     */
    private BufferedInputStream tidyXML(BufferedInputStream in) {
    	
        StringBuilder sb= new StringBuilder(250);
    	try {
    		in.mark(DEBUG_MARK_SIZE); //memorize current position in stream
	    	BufferedReader buf = new BufferedReader(new InputStreamReader(in));
	        String s;
	  
	        boolean inXML=false;
	        while ((s = buf.readLine()) != null) {
	        	if (s.trim().toLowerCase().startsWith("<xml")||s.trim().toLowerCase().startsWith("<?xml")) {
	        		inXML=true;
	        	}
	        	else {
	        		inXML=inXML; // just for debug purposes
	        	}
	        	if (inXML) {
	        		sb.append(s);
	        	}
	        }
	        in.reset(); // and reset reading position to memorized position.
    	} catch(Exception e) {
            Logger.log(e);
            if (Logger.isTraceOn(Logger.TT_SYNC)) {
                logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Exceptione:" + e.toString());
            }
            return in;
        }

        return new BufferedInputStream(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        
    }
    
    private boolean login() {
        try {
            loggedIn = false;
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCDEBUG, "Login auf " + this.loginurl+ " mit Benutzername " + this.username + " ...");
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);
            URL url = new URL(this.loginurl);
            URLConnection connection = url.openConnection();

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(true);
            connection.setRequestProperty ("Content-Type","application/x-www-form-urlencoded");
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            UUID projectId;
            try {
                projectId = Daten.project.getProjectId();
            } catch(Exception e) {
                logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORCONFIG, "No project ID found: " + e.toString());
                projectId = null;
            }
            String projectName = EfaUtil.escapeHtmlGetString(Daten.project.getProjectName());
            String clubName = EfaUtil.escapeHtmlGetString(Daten.project.getClubName());
            String loginText="username=" + username +  
                    "&password=" + password +
                    (projectId != null ? "&project=" + projectId.toString() : "") +
                    (projectName != null && projectName.length() > 0 ? "&projectname=" + projectName : "") +
                    (clubName != null && clubName.length() > 0 ? "&clubname=" + clubName : "");
            out.write(loginText);
            out.flush();
            out.close();

            KanuEfbXmlResponse response = getResponse(connection, new BufferedInputStream(connection.getInputStream()));
            CookieStore cookieJar = manager.getCookieStore();
            List<HttpCookie> cookies = cookieJar.getCookies();
            for (HttpCookie cookie : cookies) {
                if (Logger.isTraceOn(Logger.TT_SYNC)) {
                    logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Session Cookie: " + cookie);
                }
                sessionCookie = cookie;
            }

            int retCode = (response == null ? -1 : EfaUtil.stringFindInt(response.getValue(0, "code"), -1));
            if (retCode != 1) {
                String msg = (response == null ? "unbekannt" : response.getValue(0, "message"));
                logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORLOGIN, "Login fehlgeschlagen: Code "+retCode+" ("+msg+")");
                return false;
            }

        } catch (Exception e) {
            Logger.logdebug(e);
            logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORLOGIN, "Login fehlgeschlagen: "+e.toString());
            return false;
        }
        loggedIn = true;
        if (Logger.isTraceOn(Logger.TT_SYNC)) {
            logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCDEBUG, "Login erfolgreich.");
        }
        return true;
    }
  
    private int countNumberOfPersonsWithEfbIds() {
        int count = 0;
        try {
            Persons persons = Daten.project.getPersons(false);
            DataKeyIterator it = persons.data().getStaticIterator();
            for(DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                PersonRecord r = (PersonRecord)persons.data().get(k);
                if (r != null && r.isValidAt(thisSync) &&
                    r.getEfbId() != null && r.getEfbId().length() > 0) {
                    count++;
                }
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return count;
    }
        
    private boolean syncTrips() {
        try {
        	       	
        	logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisiere Fahrten...");
        	logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Bootstypen, für die Fahrten synchronisiert werden können: "+ Daten.efaConfig.getCanoeBoatTypes());
            if (Daten.efaConfig.getValueKanuEfb_FullSync() ) {
            	logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Modus FullSync: Es wird das vollständige Fahrtenbuch übertragen.");           	
            } else {
            	logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Modus DifferentialSync: Es werden nur neue und geänderte Fahrten übertragen.");           	
            }
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Es werden Fahrten ignoriert, deren Beginndatum früher ist als: "+ Daten.efaConfig.getValueKanuEfb_SyncTripsAfterDate().toString());
        	if (Daten.efaConfig.getValueKanuEfb_SyncUnknownBoats()) {
        		logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten mit unbekannten Booten werden synchronisiert.");
        	} else {
        		logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten mit unbekannten Booten werden ignoriert.");
        	}
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Personen mit eFB-IDs, für die Fahrten synchronisiert werden: "+countNumberOfPersonsWithEfbIds());
            
            Hashtable<String,LogbookRecord> efaEntryIds = new Hashtable<String,LogbookRecord>();

            boolean isRowingOrCanoeingSession =false;
            boolean isAlreadySyncedTrip=false;
            boolean isUnfinishedTrip=false;
            boolean isTooEarlyTrip=false;
            boolean isUpdatedTrip=false;
            boolean isKnownBoatButNonSupportedCanoeBoatType=false;
            boolean isEmptyBoatRecordTrip=false;
            boolean isEmptyBoatRecordTrip_SyncAnyway=false;
            boolean useTripTypeAsCommentPrefix=Daten.efaConfig.getValueKanuEfb_SyncTripTypePrefix();

            KanuEfbStatistics kStatistics=new KanuEfbStatistics(logbook.data().getNumberOfRecords());
            
            StringBuilder request = new StringBuilder();
            buildRequestHeader(request, "SyncTrips");

            DataKeyIterator it = logbook.data().getStaticIterator();
            DataKey k = it.getFirst();
            
            while (k != null) {
                LogbookRecord r = (LogbookRecord)logbook.data().get(k);
                
                // Determine session state
                if (r!= null) {
                	isAlreadySyncedTrip=false;
                	// we can only sync trips which took place on a boat, not on a ergometer or a motor boat.
                	isRowingOrCanoeingSession=r.isRowingOrCanoeingSession();
                	
                    // only brand new or updated record shall be sent to EFB.
                    // we cannot compare r.getLastModified to r.getSyncTime() as r.LastModified gets updated when the SyncTime attribute is set.
                    // so we compare r.getSyncTime() against lastsync instead, which contains the timestamp AFTER the last successful synchronization.                
                	if (Daten.efaConfig.getValueKanuEfb_FullSync()) {
                		isAlreadySyncedTrip=false;
                		isUpdatedTrip=false;
                	} else {
                		isAlreadySyncedTrip=!(r.getLastModified() > lastSync || r.getSyncTime() <= 0);
                		// Trip has already been synced, but changed after so it gets updated.
                    	isUpdatedTrip = r.getSyncTime()>0 && r.getLastModified() > lastSync;
                	}

                	// EFB denies synchronization for trips which have a start time, but no end time. So we need to detect those.
                	isUnfinishedTrip = (r.getStartTime()!= null && r.getEndTime()==null);
                	
                   	/*
                	 * Feature: 
                	 * If a club begins to activate synchronisation of efa trips to efb, some or all of the club members will already have
                	 * active EFB accounts (so: a kanu_efbID for a person record). These active EFB accounts may already contain trips.
                	 * 
                	 * The first time efa tries to sync to efb, it finds all trips in the current logbook need to be synced.
                	 * And so, for the club members who already have an EFB account with trips, there may be duplicate records created due to
                	 * the activated sync.  
                	 * 
                	 * This may not be a desired scenario. So a new configuration parameter has been established defining the 
                	 * date a trip has to be started on so that it gets synchronized to efb.
                	 */
                	isTooEarlyTrip = r.getDate().isBefore(Daten.efaConfig.getValueKanuEfb_SyncTripsAfterDate());
                	
                	isEmptyBoatRecordTrip = r.getBoatRecord(r.getValidAtTimestamp()) == null;
                	isEmptyBoatRecordTrip_SyncAnyway = isEmptyBoatRecordTrip && Daten.efaConfig.getValueKanuEfb_SyncUnknownBoats();
                	
                	// We only support EFB synchronization for special boat types, which are set in efaConfig 
                	// this is only true if boat is in our boat database and has a boat type that is not in our list of syncable types
                	isKnownBoatButNonSupportedCanoeBoatType = !isCanoeBoatType(r.getBoatRecord(r.getValidAtTimestamp())) && !isEmptyBoatRecordTrip;

                	// let's get some statistics...
                	kStatistics.incrementNonCanoeingTripCntIfTrue(!isRowingOrCanoeingSession);
                	kStatistics.incrementAlreadySyncedTripCntIfTrue(isAlreadySyncedTrip);                	
                	if (!isAlreadySyncedTrip) {

                		kStatistics.incrementUnfinishedTripCntIfTrue(isUnfinishedTrip);

                		if (!isUnfinishedTrip) {

                			kStatistics.incrementTooEarlyTripCntIfTrue(isTooEarlyTrip);

                			if (!isTooEarlyTrip) {
                				//only one of these values can be true
                				kStatistics.incrementKnownBoatNonSupportedBoatTypeTripCntIfTrue(isKnownBoatButNonSupportedCanoeBoatType);
                				kStatistics.incrementEmptyBoatRecordTripCntIfTrue(isEmptyBoatRecordTrip && !isEmptyBoatRecordTrip_SyncAnyway);
                				
            					if (isEmptyBoatRecordTrip && !isEmptyBoatRecordTrip_SyncAnyway && verboseMode) {
            						// only log about unknown boats in verbose mode as this can happen any synchronisation
            						logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "  Fahrt " +  r.getQualifiedName()+ " - Bootstyp nicht gesetzt/Boot unbekannt: " + r.getBoatAsName());
            					} else if (isEmptyBoatRecordTrip && isEmptyBoatRecordTrip_SyncAnyway) {
            						// do nothing here. next step is to create requests if at least one person in the crew has an EFB ID.
            						// this will be done in createRequestsWithStatistics
            					}
                			}
                		}
                	}
                }
                
                
                if (r != null 
                		// Die Fahrt darf noch nicht synchronisiert sein, und muss eine echte Fahrt sein (kein ERGO oder so)
                		&& (!isAlreadySyncedTrip) && isRowingOrCanoeingSession  
                		// nur Fahrten mit Enddatum synchronisieren, wobei Enddatum auch überschritten sein muss
                		&& !isUnfinishedTrip && !isTooEarlyTrip 
                		// wenn es eine Fahrt mit einem bekannten Boot ist, oder wir die Fahrt
                		// mit dem unbekannten Boot synchronisieren sollen
                		&& (!isEmptyBoatRecordTrip || isEmptyBoatRecordTrip_SyncAnyway)
                		// es ist eine Fahrt mit einem bekannten Boot ist, und der Bootstyp für das Boot gesetzt ist
                		&& (!isKnownBoatButNonSupportedCanoeBoatType)
                		) {
                	
                	createRequestWithStatistics(request, r, efaEntryIds, kStatistics, 
                								isUpdatedTrip, isEmptyBoatRecordTrip, isEmptyBoatRecordTrip_SyncAnyway,
                								useTripTypeAsCommentPrefix);
                	
                } else {
                    if (r != null) {
                        if (Logger.isTraceOn(Logger.TT_SYNC)) {
                            logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO, "  keine Synchronisierungs-Anfrage für unveränderte Fahrt: " + r.getQualifiedName());
                        }
                    }
                }
                k = it.getNext();
            }
            
            buildRequestFooter(request);

            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "-----------");
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten in DB: "+kStatistics.getTotalTripCnt());
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten zu synchronisieren (neu): "+kStatistics.getSyncTripCnt());
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten zu synchronisieren (aktualisiert): "+kStatistics.getUpdatedTripCnt());
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten zu ignorieren (bereits synchronisiert): "+kStatistics.getAlreadySyncedTripCnt());
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten zu ignorieren (älter als Startdatum EFB-Sync in Konfig): "+kStatistics.getTooEarlyTripCnt());
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten zu ignorieren (nicht beendet): "+kStatistics.getUnfinishedTripCnt());
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten zu ignorieren (Motorboot/Ergo): "+kStatistics.getNonCanoeingTripCnt());
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten zu ignorieren (kein unterstützter Bootstyp): "+kStatistics.getNonSupportedBoatTypeTripCnt());
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten zu ignorieren (leerer Bootstyp (unbekanntes Boot?)): "+kStatistics.getEmptyBoatRecordTripCnt()); 
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Fahrten zu ignorieren (keines der Crew-Mitglieder hat Kanu-EFB-ID): "+kStatistics.getPersonWithoutEFBIDTripCnt());
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "-----------");
            
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Sende Synchronisierungs-Anfrage für " + kStatistics.getRequestCnt() + " Datensätze.");
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Hinweis: Bei mehrsitzigen Booten wird ein Datensatz je Crewmitglied synchronisiert.");
            
            KanuEfbXmlResponse response = sendRequest(request.toString(), true);
            
            return handleSyncTripsResponse (response, efaEntryIds, kStatistics);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    private boolean handleSyncTripsResponse(KanuEfbXmlResponse response, Hashtable<String,LogbookRecord> efaEntryIds, KanuEfbStatistics statistics) {
    	
    	try {
	        if (response != null && response.isResponseOk("SyncTrips")) {
	            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Synchronisierungs-Antwort erhalten für " + response.getNumberOfRecords() + " Datensätze ...");
	            for (int i=0; i<response.getNumberOfRecords(); i++) {
	                Hashtable<String,String> fields = response.getFields(i);
	                boolean ok = false;
	                String tripId = fields.get("tripid");
	                int result = EfaUtil.string2int(fields.get("result"), -1);
	                LogbookRecord r = null;
	                if (tripId != null) {
	                    tripId = tripId.trim();
	                    r = efaEntryIds.get(tripId);
	                }
	                String resultText = fields.get("resulttext");
	                if (r != null) {
	                    if (result == 0 || // 0 - ok - new trip accepted
	                        result == 1 || // 1 - ok - existing trip updated
	                        result == 2) { // 2 - ok - existing trip deleted
	                        try {
	                        	r.setSyncTime(thisSync);
	                        	logbook.data().update(r);
		                        ok = true;
	                        } catch (Exception e) {
	                        	ok = false;
	                            logInfo(Logger.WARNING, Logger.MSG_SYNC_WARNINCORRECTRESPONSE, "Fehler beim Synchronisieren von Fahrt: Trip ID "+tripId+": Exception." + e.getMessage());
	                        }
	                        
	                    }
	                } else {
	                    logInfo(Logger.WARNING, Logger.MSG_SYNC_WARNINCORRECTRESPONSE, "Fehler beim Synchronisieren von Fahrt: Trip ID "+tripId+" unbekannt (Code "+result+" - "+resultText+")");
	                }
	                if (ok) {
	                    countSyncTrips++;
	                    if (Logger.isTraceOn(Logger.TT_SYNC)) {
	                        logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO, "  Fahrt erfolgreich synchronisiert: "+r.toString());
	                    }
	                } else {
	                    logInfo(Logger.WARNING, Logger.MSG_SYNC_WARNINCORRECTRESPONSE, "Fehler beim Synchronisieren von Fahrt: "+tripId+" (Code "+result+" - "+resultText+")");
	                }
	            }//next trip
	            
	            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, countSyncTrips + "/"+ statistics.getRequestCnt() +  " Datensätze synchronisiert.");
	        } else {
	            logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORINVALIDRESPONSE, "Ungültige Synchronisierungs-Antwort.");
	            logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORINVALIDRESPONSE, "Auf EFB-Schulungssystemen kann ggfs. die Konfigurationsoption 'XML-Antworten auf EFB-Schulungssystemen bereinigen' weiterhelfen.");
	            return false;
	        }    	
	        //wenn alles geklappt hat - ok zurückgeben
	        return true;
    	 } catch (Exception e) {
             e.printStackTrace();
             return false;
         }
    }
    
    private void createRequestWithStatistics(StringBuilder request,  LogbookRecord r, Hashtable<String,LogbookRecord> efaEntryIds, KanuEfbStatistics statistics, 
    		boolean isUpdatedTrip, boolean isEmptyBoatRecordTrip, boolean isEmptyBoatRecordTrip_SyncAnyway, boolean useTripTypePrefixForComment) {
    	//we want to check if the current trip leads to at least one request. 
    	//if not, the trip is ignored due to the fact that none of the crew members has an EfbID.
    	long oldRequestCnt=statistics.getRequestCnt(); 
    	boolean isTripWithAtLeastOneCrewMemberWithEFBID=false;
    	boolean isTripWithIdentifiedCrewMember=false;
    	String unidentifiedCrewMembers="";
    	
        Boats boats = Daten.project.getBoats(false);
        Persons persons = Daten.project.getPersons(false);
        Destinations destinations = Daten.project.getDestinations(false);
        Waters waters = Daten.project.getWaters(false);
    	
    	
    	//Steuermann zuzüglich bis zu 24 Crewmitglieder auf eine EFB-ID prüfen.
    	for (int i=0; i<=LogbookRecord.CREW_MAX; i++) {
            UUID pId = r.getCrewId(i);

            if (pId != null) {
            	isTripWithIdentifiedCrewMember=true;
            	PersonRecord p = persons.getPerson(pId, thisSync);
                if (p != null && p.getEfbId() != null && p.getEfbId().length() > 0 &&
                    r.getDate() != null) {
                    if (Logger.isTraceOn(Logger.TT_SYNC)) {
                        logInfo(Logger.DEBUG, Logger.MSG_SYNC_SYNCINFO, "  erstelle Synchronisierungs-Anfrage für Fahrt: " + r.getQualifiedName()+
                                "; Person: "+p.getQualifiedName());
                    }
                    
                    isTripWithAtLeastOneCrewMemberWithEFBID=true;
                    
                    BoatRecord b = (r.getBoatId() != null ? boats.getBoat(r.getBoatId(), thisSync) : null);
                    DestinationRecord d = (r.getDestinationId() != null ? destinations.getDestination(r.getDestinationId(), thisSync): null);
                    String startDate = r.getDate().getDateString("YYYY-MM-DD");
                    String endDate = (r.getEndDate() != null ? r.getEndDate().getDateString("YYYY-MM-DD") : startDate);
                    String tripId = logbook.getName()+"_"+r.getEntryId().toString();
                    request.append("<trip>");
                    request.append("<tripID>" + tripId + "</tripID>");
                    request.append("<userID>" + p.getEfbId() + "</userID>");
                    if (b != null && b.getEfbId() != null && b.getEfbId().length() > 0) {
                        request.append("<boatID>" + b.getEfbId() + "</boatID>");
                    } else {
                        request.append("<boatText><![CDATA[" + (b != null ? b.getQualifiedName() : r.getBoatName()) + "]]></boatText>");
                    }
                    request.append("<begdate>" + startDate + "</begdate>");
                    request.append("<enddate>" + endDate + "</enddate>");
                    if (r.getStartTime() != null) {
                        request.append("<begtime>" + r.getStartTime().toString() + "</begtime>");
                    }
                    if (r.getEndTime() != null) {
                        request.append("<endtime>" + r.getEndTime().toString() + "</endtime>");
                    }
                    
                    SessionGroupRecord sg = r.getSessionGroup();
                    String triptype = r.getSessionType();
                    if (triptype == null || triptype.length() == 0) {
                        triptype = EfaTypes.TYPE_SESSION_NORMAL;
                    }
                    request.append("<triptype>" + triptype + "</triptype>");
                    if (sg != null) {
                        request.append("<tripgroup>");
                        request.append("<name><![CDATA[" + sg.getName() + "]]></name>");
                        if (sg.getOrganizer() != null && sg.getOrganizer().length() > 0) {
                            request.append("<organizer><![CDATA[" + sg.getOrganizer() + "]]></organizer>");
                        }
                        request.append("</tripgroup>");
                    }

                    // build waters
                    // TODO this needs refactoring in the future.
                    // Currently synWaters() is not supported by KanuEFB any more
                    // and has been removed in the sync task. So, no water has an EFB id any more, and the water name is used
                    // for syncronisation anyway. So the checking for the water.efbId is useless here.
                    ArrayList<String> waterText = new ArrayList<String>();
                    ArrayList<String> waterID = new ArrayList<String>();
                    DataTypeList<UUID> waterList = (d != null ? d.getWatersIdList() : null);
                    DataTypeList<UUID> waterListMore = r.getWatersIdList();
                    if (waterListMore != null) {
                        if (waterList == null) {
                            waterList = new DataTypeList<UUID>();
                        }
                        waterList.addAll(waterListMore);
                    }
                    for (int di=0; waterList != null && di<waterList.length(); di++) {
                        WatersRecord w = waters.getWaters(waterList.get(di));
                        if (w != null) {
                            if (w.getEfbId() != null && w.getEfbId().length() > 0) {
                                waterID.add(w.getEfbId());
                            } else {
                                waterText.add(w.getName());
                            }
                        }
                    }
                    DataTypeList<String> waterListText = r.getWatersNameList();
                    for (int di=0; waterListText != null && di<waterListText.length(); di++) {
                        waterText.add(waterListText.get(di));
                    }
                    String wIDs = (waterID.size() > 0 ?
                        EfaUtil.arr2KommaList(waterID.toArray(new String[0])) : null);
                    String wTxt = (wIDs == null && waterText.size() > 0 ?
                        EfaUtil.arr2KommaList(waterText.toArray(new String[0])) : null);

                    request.append("<lines>");
                    request.append("<line>");
                    if (wIDs != null) {
                        request.append("<waterID>" +wIDs + "</waterID>");
                    } else if (wTxt != null) {
                        request.append("<waterText><![CDATA[" + wTxt + "]]></waterText>");
                    }
                    if (d != null && d.getStart() != null && d.getStart().length() > 0) {
                        request.append("<fromText><![CDATA[" + d.getStart() + "]]></fromText>");
                    }
                    if (d != null && d.getEnd() != null && d.getEnd().length() > 0) {
                        request.append("<toText><![CDATA[" + d.getEnd() + "]]></toText>");
                    } else {
                        if (r.getDestinationId() != null || r.getDestinationName() != null) {
                            request.append("<toText><![CDATA["+ (r.getDestinationId() != null ? r.getDestinationAndVariantName() : r.getDestinationName()) + "]]></toText>");
                        }
                    }
                    request.append("<kilometers>" + (r.getDistance() != null ? r.getDistance().getStringValueInKilometers() : "0") + "</kilometers>");
                    request.append("</line>");
                    request.append("</lines>");

                    //Kommentar vorhanden, oder soll die Fahrtart dem Kommentar vorangesetzt werden?
                    if (((r.getComments() != null && r.getComments().length() > 0)) || useTripTypePrefixForComment) {
                    	String prefix="";
                    	// wenn konfiguriert, für alles außer "normale Fahrt" einen Präfix setzen beim Kommentar
                    	if (useTripTypePrefixForComment && (!triptype.equalsIgnoreCase(EfaTypes.TYPE_SESSION_NORMAL))) {
                    		prefix = Daten.efaTypes.getValue(EfaTypes.CATEGORY_SESSION, triptype)+": ";
                    	} 
                    	request.append("<comment><![CDATA[" + prefix + (r.getComments() == null ? "" : r.getComments()) + "]]></comment>");

                    }

                    request.append("<changeDate>" + r.getLastModified() + "</changeDate>");
                    request.append("<status>" + "1" + "</status>");
                    request.append("<deleted>" + "0" + "</deleted>");
                    request.append("</trip>\n");
                    efaEntryIds.put(tripId, r);
                    statistics.incrementRequestCnt();
                    
                }
            }
            else if (pId==null) {
            	unidentifiedCrewMembers = (unidentifiedCrewMembers+ " "+ r.getCrewAsName(i)).trim();
            }
        } // end of for each Crew Member
    	
    	
    	// check if the boat was unknown, but at least one trip could be synced anyway.
    	// do appropiate logging and statistics.
    	if (isEmptyBoatRecordTrip && isEmptyBoatRecordTrip_SyncAnyway) {
    		//Boat is unknown (means:not in the database), but config says whe shall sync such trips anyway.
    		if (isTripWithAtLeastOneCrewMemberWithEFBID) {
    			//if there was at least one person with an efbID, the trip got synced.
    			//we log this fact and document that the boat was unknown. 
    			//logging is done also if verbose mode is off, as this log entry gets only written once per trip (so it does not fill the efa log with unneccesary messages)
    			logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "  Fahrt " +  r.getQualifiedName()+ " - Boot unbekannt: " + r.getBoatAsName() + " - Fahrt wird trotzdem synchronisiert");
    		} else {
    			// Boot unbekannt, sollte aber trotzdem synchronisiert werden - aber kein crewmitglied mit EFBID.
    			// die Fahrt gilt als nicht synchronisiert weil kein crewmitglied eine EFB ID hatte, und NICHT weil das Boot unbekannt war.
    			// daher müssen wir an dieser Stelle keine Statistik pflegen.
    			// sehr wohl dokumentieren wir im Verbose Mode, dass der Bootsname unbekannt war, und die Fahrt nicht synchronisiert wurde.
    			// so hat der Admin die Möglichkeit, das Boot in die Bootsliste aufzunehmen, und die Fahrt in einem späteren Synchronisationsversuch doch noch zu synchronisieren.
    			if (verboseMode) {
					// only log about unknown boats with unknown members in verbose mode as this can happen any synchronisation
					logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "  Fahrt " +  r.getQualifiedName()+ " - Bootstyp nicht gesetzt/Boot unbekannt und keine Person hat EFB-ID: " + r.getBoatAsName());
    			}
    		}
    	}
	    	
      	// if the number of requests has not changed, none of the crew members has an EFB ID, so the trip is ignored...
    	statistics.incrementPersonWithoutEFBIDTripCntIfTrue(oldRequestCnt==statistics.getRequestCnt());
       	statistics.incrementUpdatedtripCntIfTrue(isTripWithAtLeastOneCrewMemberWithEFBID && isUpdatedTrip);
       	statistics.incrementSyncTripCntIfTrue(isTripWithAtLeastOneCrewMemberWithEFBID && !isUpdatedTrip);

       	if (!isTripWithIdentifiedCrewMember && verboseMode){
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "  Fahrt " +  r.getQualifiedName()+ " - Keines der Crewmitglieder in der Personenliste: "+ unidentifiedCrewMembers);
        }
    }
    
    public boolean isCanoeBoatType(BoatRecord r) {
        Object[] types = Daten.efaConfig.getValueKanuEfb_CanoeBoatTypes();
        for (int i=0; r != null && i<r.getNumberOfVariants(); i++) {
            for (int j=0; types != null && j<types.length; j++) {
                if (types[j] != null && types[j].toString().equals(r.getTypeType(i))) {
                    return true;
                }
            }
        }
        return false;
    }    
    
    private boolean syncDone() {
        try {
            if (loggedIn) {
                logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Logout ...");

                StringBuilder request = new StringBuilder();
                buildRequestHeader(request, "SyncDone");
                buildRequestFooter(request);

                KanuEfbXmlResponse response = sendRequest(request.toString(), false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void run() {
        setRunning(true);
        try {
            Thread.sleep(1000);
        } catch(Exception e) {
        }
        int i = 0;
        thisSync = System.currentTimeMillis();
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Beginne Synchronisierung mit Kanu-eFB ...");
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Startzeit der Synchronisierung: " +
                EfaUtil.getTimeStamp(thisSync) + " (" + thisSync + ")");
        logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Letzte Synchronisierung: " +
                (lastSync == 0 ? "noch nie" : EfaUtil.getTimeStamp(lastSync)) + " (" + lastSync + ")");
        while(true) {
            if (!login()) {
                break;
            }
            setCurrentWorkDone(++i);

            if (!syncTrips()) {
                break;
            }
            setCurrentWorkDone(++i);
            
            break;
        }
        syncDone();
        setCurrentWorkDone(++i);
        
        if (i == getAbsoluteWork()) {
        
        	// We need to store the timestamp AFTER the last successful synchronization to "lastSync"
        	// so that we can compare the record's lastModified timestamp against it in syncTrips().
        	
        	Daten.project.setClubKanuEfbLastSync(System.currentTimeMillis());
            
        	StringBuilder msg = new StringBuilder();
            if (countErrors == 0) {
                if (countWarnings == 0) {
                    msg.append("Synchronisierung mit Kanu-eFB erfolgreich beendet.");
                } else {
                    msg.append("Synchronisierung mit Kanu-eFB mit Warnungen beendet.");
                }
            } else {
                msg.append("Synchronisierung mit Kanu-eFB mit Fehlern beendet.");
            }
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, "Endzeit der Synchronisierung: " +  EfaUtil.getTimeStamp(thisSync) + " (" + thisSync + ")");
            msg.append(" [");
            msg.append(countSyncTrips  + " Fahrten synchronisiert] [");
            msg.append(countWarnings   + " Warnungen, ");
            msg.append(countErrors     + " Fehler");
            msg.append("]");
            logInfo(Logger.INFO, Logger.MSG_SYNC_SYNCINFO, msg.toString());
            successfulCompleted = true;
        } else {
            logInfo(Logger.ERROR, Logger.MSG_SYNC_ERRORABORTSYNC, "Synchronisierung mit Kanu-eFB wegen Fehlern abgebrochen.");
            successfulCompleted = false;
        }
        setDone();
    }

    public int getAbsoluteWork() {
        return 3; //login, sync trips, logout.
    }

    public String getSuccessfullyDoneMessage() {
        if (successfulCompleted) {
            return LogString.operationSuccessfullyCompleted(International.getString("Synchronisation")) +
                   "\n"   + countSyncTrips + " Fahrten synchronisiert." +
                   "\n\n" + countWarnings + " Warnungen" +
                   "\n"   + countErrors + " Fehler";
        } else {
            return LogString.operationFailed(International.getString("Synchronisation"));
        }
    }

    private void logInfo(String type, String key, String msg) {
        Logger.log(type, key, msg);
        // this has been commented out in the base version of KanuEfbSyncTask.
        //if (!type.equals(Logger.DEBUG)) {
            logInfo(msg+"\n");
        //}
        if (Logger.WARNING.equals(type)) {
            countWarnings++;
        }
        if (Logger.ERROR.equals(type)) {
            countErrors++;
        }
    }

    public void startSynchronization(ProgressDialog progressDialog) {
        if (Daten.isGuiAppl()) {
            if (Dialog.yesNoDialog(International.onlyFor("Mit Kanu-eFB synchronisieren", "de"),
                    International.onlyFor("Es werden alle Fahrten aus dem aktuellen Fahrtenbuch mit dem Kanu-eFB synchronisiert.", "de") + "\n" +
                    International.getString("Bitte stelle eine Verbindung zum Internet her.") + "\n" +
                    International.getString("Möchtest Du fortfahren?")) != Dialog.YES) {
                return;
            }
        }
        while (loginurl == null || loginurl.length() == 0 ||
            cmdurl == null || cmdurl.length() == 0) {
            String msg = International.getString("Fehlende Konfigurationseinstellungen");
            if (!Daten.isGuiAppl()) {
                Logger.log(Logger.ERROR, Logger.MSG_SYNC_ERRORABORTSYNC, msg);
                Daten.haltProgram(Daten.HALT_MISCONFIG);
            }
            Dialog.infoDialog(msg, International.getString("Bitte vervollständige die Konfigurationseinstellungen!"));
            EfaConfigDialog dlg = new EfaConfigDialog((JDialog)null, Daten.efaConfig, BaseTabbedDialog.makeCategory(Daten.efaConfig.CATEGORY_SYNC, Daten.efaConfig.CATEGORY_KANUEFB));
            dlg.showDialog();
            if (!dlg.getDialogResult()) {
                return;
            }
            getConfigValues();
        }
        while (username == null || username.length() == 0 ||
            password == null || password.length() == 0) {
            String msg = International.getString("Fehlende Konfigurationseinstellungen");
            if (!Daten.isGuiAppl()) {
                Logger.log(Logger.ERROR, Logger.MSG_SYNC_ERRORABORTSYNC, msg);
                Daten.haltProgram(Daten.HALT_MISCONFIG);
            }
            ProjectEditDialog dlg = new ProjectEditDialog((JDialog)null, Daten.project, null, ProjectRecord.GUIITEMS_SUBTYPE_KANUEFB, admin);
            dlg.showDialog();
            if (!dlg.getDialogResult()) {
                return;
            }
            getConfigValues();
        }
        
        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            java.lang.System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        } catch (Exception e) {
            System.out.println(e);
        }

        this.start();
        if (progressDialog != null) {
            progressDialog.showDialog();
        }
    }
    
    public boolean isSuccessfullyCompleted() {
        return successfulCompleted;
    }

	public boolean getVerboseMode() {
		return verboseMode;
	}

	public void setVerboseMode(boolean verboseMode) {
		this.verboseMode = verboseMode;
	}

}