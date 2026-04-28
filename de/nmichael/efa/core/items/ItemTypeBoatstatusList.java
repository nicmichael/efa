/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.core.items;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import javax.swing.SwingUtilities;

import de.nmichael.efa.Daten;
import de.nmichael.efa.core.config.EfaConfig;
import de.nmichael.efa.core.config.EfaTypes;
import de.nmichael.efa.data.BoatRecord;
import de.nmichael.efa.data.BoatReservationRecord;
import de.nmichael.efa.data.BoatStatusRecord;
import de.nmichael.efa.data.Boats;
import de.nmichael.efa.data.GroupRecord;
import de.nmichael.efa.data.Groups;
import de.nmichael.efa.data.Logbook;
import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.storage.DataKey;
import de.nmichael.efa.data.storage.DataKeyIterator;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.gui.EfaBoathouseFrame;
import de.nmichael.efa.util.EfaUtil;
import de.nmichael.efa.util.International;
import de.nmichael.efa.util.Logger;

public class ItemTypeBoatstatusList extends ItemTypeList {

    public static final int SEATS_OTHER = 99;
    public static final String TYPE_OTHER = "";
    public static final String RIGGER_OTHER = "";
    private static final String STR_DESTINATION_DELIMITER=     	"     -> ";
    EfaBoathouseFrame efaBoathouseFrame;
    private String STR_RESERVIERT_FUER=International.getString("Reserviert für").toLowerCase();
    private String STR_BOOTSSCHADEN=International.getString("Bootsschaden");
    
    //Cache for special tooltip colors, set up when sorting boatslist items and determining tooltip texts
    private String cacheToolTipBgColorText="";
    private String cacheToolTipFontColorOpeningTag = "";
    private String cacheToolTipFontColorClosingTag = "";

    
    public ItemTypeBoatstatusList(String name,
            int type, String category, String description,
            EfaBoathouseFrame efaBoathouseFrame) {
        super(name, type, category, description);
        this.efaBoathouseFrame = efaBoathouseFrame;
    }

    public ItemTypeBoatstatusList(String name,
            int type, String category, String description,
            EfaBoathouseFrame efaBoathouseFrame, boolean showFilterField) {
        super(name, type, category, description, showFilterField);
        this.efaBoathouseFrame = efaBoathouseFrame;
    }
    
    public void setBoatStatusData(Vector<BoatStatusRecord> v, Logbook logbook, String other, Vector <BoatReservationRecord> todaysReservations) {
        Vector<ItemTypeListData> vdata = sortBootsList(v, logbook, todaysReservations);
        if (other != null) {
            BoatListItem item = new BoatListItem();
            item.text = other;
            vdata.add(0, new ItemTypeListData(other, null, null, null, item, false, -1));//tooltip can be set to null as this function is only called but updateBoatLists for <anderes boot>
            this.other_item_text=other;
        }
        clearIncrementalSearch();
        list.setSelectedIndex(-1);
        setItems(vdata);
        showValue();
        //update the list sometime when the current event is over
        SwingUtilities.invokeLater(new Runnable() {
  	      public void run() {
            list.revalidate();  
  	    	  list.repaint();
  	      }
    	});
    }
    
    /**
	 * Determines the colors for the group icons in front of the boat names in the boat list.
	 * Also if at least one group with a valid color exists, the icon size is set for the boatlist, other wise it is zero.
	 * 
	 * @returns a map with group id as key and the color for the group as value. If no groups exist, an empty map is returned.
	 * 
	 */
    private HashMap<UUID, Color> getGroupColors(long now){
    	HashMap<UUID, Color> result = new HashMap<UUID, Color>();
        
    	try {
	    	Groups groups = Daten.project.getGroups(false);
	
	    	DataKeyIterator it = groups.data().getStaticIterator();
	        for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
	            GroupRecord gr = (GroupRecord)groups.data().get(k);
	            if (gr != null && gr.isValidAt(now)) {
	                String cs = gr.getColor();
	                if (cs != null && cs.length() > 0) {
	                    result.put(gr.getId(), EfaUtil.getColorOrGray(cs));
	                }
	            }
	        }
	        this.iconWidth = (result.size() > 0 ? Daten.efaConfig.getValueEfaDirekt_BthsFontSize() : 0);
	        this.iconHeight = this.iconWidth;
	    } catch(Exception e) {
	        Logger.logdebug(e);
	    }
    	return result;
    }   
    
 // build map of next reservation per boat (first occurrence in sorted todaysReservations)
    private HashMap<UUID, BoatReservationRecord> getNextSingleReservationForBoats(Vector <BoatReservationRecord> todaysReservations){
       if (todaysReservations != null && !todaysReservations.isEmpty()) {
			HashMap<UUID, BoatReservationRecord> result = new HashMap<UUID, BoatReservationRecord>(Math.min(todaysReservations.size(), 256));
			for (BoatReservationRecord r : todaysReservations) {
				UUID bid = r.getBoatId();
				// only store the first (earliest) reservation for this boat
				if (!result.containsKey(bid)) {
				result.put(bid, r);
				}
			}
        return result;
	   }	   
       // if no reservations exist, return empty map
       return new HashMap<UUID, BoatReservationRecord>();	
    }
    
    /**
     * 
     * @param v current BoatStatusRecords
     * @param logbook logbook to look into
     * @param todaysReservations Vector of BoatReservationRecords which are valid today. Sorted by Boat and timestamp when the reservation gets valid.
     * @return sorted Boat list elements which can be put into the boat lists in efaBoatHouse
     * 
     */
    
    /*
     * As there may be several hundreds of boats in a list, this method runs within the main thread of efa boathouse,
     * and efaboathouse is often run on an raspberry pi, this method needs to be optimized for performance.
     * 
     * So any element which is read within a loop is put into a variable - so for instance, the efaConfig values are determined just once
     * instead of for every single BoatStatusRecord element.
     */
    private Vector<ItemTypeListData> sortBootsList(Vector<BoatStatusRecord> v, Logbook logbook,Vector <BoatReservationRecord> todaysReservations) {
    	try {
    	// return empty list if no data available.
    	if (v == null || v.size() == 0 || logbook == null) {
    		return new Vector <ItemTypeListData>();
        }

        long now = System.currentTimeMillis();
        Boats boats = Daten.project.getBoats(false);
        
        // Performance for creating tooltips: cache the current config of tooltip colors
   		this.cacheToolTipBgColorText=(Daten.efaConfig.getToolTipSpecialColors() ? "bgcolor=\"#"+EfaUtil.getColor(Daten.efaConfig.getToolTipHeaderBackgroundColor())+"\"": "");
   		this.cacheToolTipFontColorOpeningTag = (Daten.efaConfig.getToolTipSpecialColors() ? "<font color=\"#"+EfaUtil.getColor(Daten.efaConfig.getToolTipHeaderForegroundColor())+"\">": "");
   		this.cacheToolTipFontColorClosingTag = (Daten.efaConfig.getToolTipSpecialColors()? "</font>": "");

        boolean buildToolTips = Daten.efaConfig.getValueEfaBoathouseExtdToolTips();
       
        boolean showDestination = Daten.efaConfig.getValueEfaDirekt_showZielnameFuerBooteUnterwegs();
        boolean showReservation = Daten.efaConfig.getValueEfaBoathouseBoatListReservationInfo();
        boolean sortByAnzahl =  Daten.efaConfig.getValueEfaDirekt_sortByAnzahl();
        boolean sortByRigger = Daten.efaConfig.getValueEfaDirekt_sortByRigger();
        boolean sortByType = Daten.efaConfig.getValueEfaDirekt_sortByType();
        
        String boatListField1 = Daten.efaConfig.getValueEfaDirektBoathouseExtBoatField1();
        String boatListField2 = Daten.efaConfig.getValueEfaDirektBoathouseExtBoatField2();
        String boatListField1Caption = (!boatListField1.isEmpty() ? EfaConfig.boatExtFields.get(boatListField1) : null);
        String boatListField2Caption = (!boatListField2.isEmpty() ? EfaConfig.boatExtFields.get(boatListField2) : null);
        
        boolean buildExtraInfo = (boatListField1 != null && !boatListField1.isEmpty()) || (boatListField2 != null && !boatListField2.isEmpty());
        
        //Determine whether or not to show the colored icons in front of a boat
        //which visualize which group of persons can use a boat.
        //Colored icons get shown when there exists at least one valid person group.
        
        HashMap<UUID, Color> groupColors = getGroupColors(now);
     // build map of next reservation per boat (first occurrence in sorted todaysReservations)
        HashMap<UUID, BoatReservationRecord> nextSingleReservationForBoats = getNextSingleReservationForBoats(todaysReservations);

        
        // Build the list elements for the boat list.
        ArrayList<BoatString> bsv = new ArrayList<BoatString>(v.size() * 2);
        for (int i = 0; i < v.size(); i++) {
            BoatStatusRecord sr = v.get(i);
            DataTypeIntString srEntryNo = sr.getEntryNo();
            String currentStatus = sr.getCurrentStatus();
            Boolean isCurrentStatusOnTheWater = BoatStatusRecord.STATUS_ONTHEWATER.equals(currentStatus);
            Boolean isCurrentStatusAvailable = (isCurrentStatusOnTheWater ? false : BoatStatusRecord.STATUS_AVAILABLE.equals(currentStatus));
            BoatRecord r = boats.getBoat(sr.getBoatId(), now);
            
            HashMap<Integer,Integer> allSeats = new HashMap<Integer,Integer>(); // seats -> variant
            // find all seat variants to be shown...
        	int numberOfVariants = (r!=null ? r.getNumberOfVariants() :0);
            if (r != null) {
                if (numberOfVariants == 1) {
                    allSeats.put(r.getNumberOfSeats(0, SEATS_OTHER), r.getTypeVariant(0));
                } else {
                    if (isCurrentStatusAvailable) {
                        for (int j = 0; j < numberOfVariants; j++) {
                            // if the boat is available, show the boat in all seat variants
                            allSeats.put(r.getNumberOfSeats(j, SEATS_OTHER), r.getTypeVariant(j));
                        }
                    } else {
                        if (isCurrentStatusOnTheWater) {
                            // if the boat is on the water, show the boat in the variant that it is currently being used in
                            if (srEntryNo != null && srEntryNo.length() > 0) {
                                LogbookRecord lr = logbook.getLogbookRecord(srEntryNo);
                                if (lr != null && lr.getBoatVariant() > 0 && lr.getBoatVariant() <= numberOfVariants) {
                                    allSeats.put(r.getNumberOfSeats(r.getVariantIndex(lr.getBoatVariant()), SEATS_OTHER),
                                            lr.getBoatVariant());
                                }
                            }
                        }
                    }
                }
                if (allSeats.size() == 0) {
                    // just show the boat in any variant
                    int vd = r.getDefaultVariant();
                    if (vd < 1) {
                        vd = r.getTypeVariant(0);
                    }
                    allSeats.put(r.getNumberOfSeats(0, SEATS_OTHER), vd);
                }
            } else {
                if (sr.getUnknownBoat()) {
                    // unknown boat
                    allSeats.put(SEATS_OTHER, -1);
                } else {
                    // BoatRecord not found; may be a boat which has a status, but is invalid at timestamp "now"
                    // don't add seats for this boat; it should *not* appear in the list
                }
            }

            Integer[] seats = allSeats.keySet().toArray(new Integer[0]);
            for (int j=0; j<seats.length; j++) {
                int variant = allSeats.get(seats[j]);

                if (r != null && seats.length < numberOfVariants) {
                    // we have multiple variants, but all with the same number of seats
                	int myDefaultVariant= r.getDefaultVariant();
                    if (myDefaultVariant > 0) {
                        variant = myDefaultVariant;
                    }
                }

                // Seats
                int seat = seats[j];
                if (seat == 0) {
                    seat = SEATS_OTHER;
                } else if (seat < 0) {
                    seat = 0;
                } else if (seat > SEATS_OTHER) {
                    seat = SEATS_OTHER;
                }

                BoatString bs = new BoatString(seat, variant, 
                		(r != null ? r.getTypeType(0) : EfaTypes.TYPE_BOAT_OTHER), // type
                		(r != null ? r.getTypeRigging(0) : EfaTypes.TYPE_RIGGING_OTHER), //rigger)
                		(isCurrentStatusOnTheWater || r == null ? sr.getBoatText() : r.getQualifiedName()), //name
                		sortByAnzahl,
                		sortByRigger,
                		sortByType);

                // Colors for Groups
                Color[] colors = (r!=null && groupColors.size() > 0 ? r.getBoatGroupsPieColors(groupColors) : null); 

                BoatListItem item = new BoatListItem();
                item.list = this;
                item.text = bs.getName();
                item.boat = r; //r is the current boat record 
                item.boatStatus = sr;
                item.boatVariant = bs.getVariant();
                bs.setColors(colors); 
                bs.setRecord(item);

                bsv.add(bs);
                if (!bs.isSortBySeats()) {
                    break;
                }
            }
        }

        Collections.sort(bsv);
        
        ArrayList<ItemTypeListData> tmp = new ArrayList<ItemTypeListData>(bsv.size() + 8);
        int anz = -1;
        String lastSep = null;
        HashMap<Integer, String> seatsKeyCache = createSeatsKeyCache();
        for (BoatString curBS : bsv) {
            String s = null;

            // sort by seats?
            if (sortByAnzahl) {
                switch (curBS.getSeats()) {
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 8:
                        // using the cache for the most common seat numbers (1-6 and 8) to speed up the retrieval of the seat number string 
                    	// for these boats, as this is a very common operation when sorting by seats. 
                    	s = seatsKeyCache.get(curBS.getSeats());
                        break;
                    default:
                    	// For other seat numbers, the string is retrieved directly from efaTypes without caching, 
                    	// as these are less common and caching them would not significantly improve performance.
                        if (curBS.getSeats() < 99) {
                            s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, Integer.toString(curBS.getSeats()));
                        }
                }
            }

            // sort by rigger?
            if (sortByRigger && curBS.getRigger() != null) {
                if (sortByAnzahl) {
                    if (EfaTypes.getSeatsKey(curBS.getSeats(), curBS.getRigger()) != null) {
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.getSeatsKey(curBS.getSeats(), curBS.getRigger()));
                    }
                } else {
                    s = (s == null ? "" : s + " ") + Daten.efaTypes.getValue(EfaTypes.CATEGORY_RIGGING, curBS.getRigger());
                }
            }
            // sort by type?
            if (sortByType && curBS.getType() != null) {
                //TODO: here seems to be a bug: if a boat has several variants, only the type of the default variant gets displayed.
            	//if we want to sort by variant, we should show the type of the current variant, not the default variant.
            	s = (s == null ? "" : s + " ") + Daten.efaTypes.getValue(EfaTypes.CATEGORY_BOAT, curBS.getType());
            }

            if (s == null || s.equals(EfaTypes.getStringUnknown())) {
                /* @todo (P5) Doppeleinträge currently not supported in efa2
                 DatenFelder d = Daten.fahrtenbuch.getDaten().boote.getExactComplete(removeDoppeleintragFromBootsname(a[i].name));
                 if (d != null) {
                 s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, d.get(Boote.ANZAHL));
                 } else {
                 */
                s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_OTHER);
                if (Daten.efaConfig.getValueEfaDirekt_boatListIndividualOthers() && curBS.getType()!= null) {
                    s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_BOAT, curBS.getType());
                }

                //}
            }
            anz = curBS.getSeats();
            if (sortByAnzahl || sortByType) {
                String newSep = LIST_SECTION_STRING_START + s + LIST_SECTION_STRING_END;
                if (!newSep.equals(lastSep)) {
                    tmp.add(new ItemTypeListData(newSep, null, null, null, null, true, anz));
                }
                lastSep = newSep;
            }
            BoatListItem blitem=(BoatListItem) curBS.getRecord();
            NameExtension nameExtension = null;
            
            if (buildExtraInfo) {
            	nameExtension = getBoatNameExtension(curBS, blitem, boatListField1, boatListField2, boatListField1Caption, boatListField2Caption);
            }
            
            tmp.add(new ItemTypeListData(curBS.getName(), 
            		(buildToolTips ? getBoatToolTip(curBS,nextSingleReservationForBoats, showReservation, nameExtension) : ""), 
            		getSecondaryItem(blitem.boatStatus, nextSingleReservationForBoats, showDestination, showReservation),
            		(buildExtraInfo && nameExtension != null ? nameExtension.getAsCommaSeparatedList() : null),
            		blitem, false, -1, null, curBS.getColors()));
        }
        // convert to Vector for API compatibility
        Vector<ItemTypeListData> vv = new Vector<ItemTypeListData>(tmp.size());
        vv.addAll(tmp);
        return vv;
        } catch (Exception ee) {
        	Logger.logdebug(ee);
    		return null;
    	}
    }
    
    

    private HashMap<Integer, String> createSeatsKeyCache() {
    	
    	HashMap<Integer, String> result = new HashMap<Integer, String>();
    	result.put(1,Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_1));
    	result.put(2,Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_2));
    	result.put(3,Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_3));
    	result.put(4,Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_4));
    	result.put(5,Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_5));
    	result.put(6,Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_6));
    	result.put(8,Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_8));   	
    	
		return result;
	}

	/**
     * Returns some additional information on the current Boatstring / Boatrecord.
     * which Boatrecord may point to a boat, but also to a person record.
     * 
     * @param bs Boat String with already some determined strings
     * @param bi the BoatListItem where we can get the boatRecord and personRecord from.
     * @return
     */
    private NameExtension getBoatNameExtension(BoatString bs, BoatListItem bi, String boatListField1, String boatListField2, String boatListField1Caption, String boatListField2Caption) {
    	try {
        	String showInList = bi.boatStatus.getShowInList();//boatstatus is always set 
			String ext1=null;
			String ext2=null; 
			
    		if (showInList.equals(BoatStatusRecord.STATUS_AVAILABLE)){

    			if (bi.boat != null) {// is it a boatrecord?

    				if (boatListField1!=null && !boatListField1.isEmpty()) {
    					ext1 = bi.boat.getStringValueFromField(boatListField1);
    					if (ext1 !=null && boatListField1.equals(BoatRecord.MAXCREWWEIGHT)) {
    						ext1=ext1.concat(Daten.efaConfig.getValueDefaultWeightUnit());
    					}
    				}
    				if (boatListField2!=null && !boatListField2.isEmpty()) {
    					ext2 = bi.boat.getStringValueFromField(boatListField2);
    					if (ext2 !=null	&& boatListField2.equals(BoatRecord.MAXCREWWEIGHT)) {
    						ext2=ext2.concat(Daten.efaConfig.getValueDefaultWeightUnit());
    					}
    				}
    			}
    		} else {
    			return null;
    		}
					
    		return new NameExtension(ext1, ext2, boatListField1Caption, boatListField2Caption);
				
    	} catch (Exception e) {
    		Logger.log(e);
    		return null;
    	}
    }
    
    private NameExtension getPersonNameExtension(BoatString bs, String personListField1, String personListField2, String personListField1Caption, String personListField2Caption) {
    	try {

			String ext1=null;
			String ext2=null; 
			BoatListItem bi=(BoatListItem) bs.getRecord();
			PersonRecord pr = bi.person;
    		if (pr != null){//no, must be a person record, as we only have these two options for the boat list items.

				if (personListField1!=null && !personListField1.isEmpty()) {
					ext1 = pr.getStringValueFromField(personListField1);
				}
				if (personListField2!=null && !personListField2.isEmpty()) {
					ext2 = pr.getStringValueFromField(personListField2);
				}
			}
					
    		return new NameExtension(ext1, ext2, personListField1Caption, personListField2Caption);
				
    	} catch (Exception e) {
    		Logger.log(e);
    		return null;
    	}
    }  
    
/*
 * Creates a tooltip for either
 * - boatlist
 * - boats on water list
 * 
 * Where are the attributes obtained from?
 * 
 * Boat list
 * ---------
 * Boat name    - BoatString->BoatStatus->BoatName  or BoatString.name if boat is not in the boat list
 * Boat variant - BoatString->Boat->getDetailedBoatType
 * Boat groups  - BoatString->Boat->getAllowedGroupsAsNameString
 * 
 * 
 * Boats on water list/Trip
 * ---------
 * Boat name    - If boat on the water, split BoatString->Name into Boat name and destination using the unique delimiter
 * Destination  - If boat on the water, split BoatString->Name into Boat name and destination using the unique delimiter
 * 				  (works flawlessly)
 * lastmodified - BoatString->BoatStatus->getLastModified
 * Start time,
 * Cox & Crew	- BoatString->BoatStatus->Comment
 * 					- remove from comment: boat name, boat destination,  boat status text.
 * 					- replace all ; by , with a line break, so that the crew/cox are shown one per line
 * 				  works flawlessly with any international representation of Boatstatus.comment (built by BoatStatusRecord.createStatusString).
 * 				  works even after changing EFA language from de to en to any other language and starting new trips in each language.
 * 
 * The creation of tooltip text is somewhat prone to nullpointer exceptions, as it is not guaranteed that all of the attributes
 * are set with a content. To avoid those nullpointers to pop up and create an empty boat on water list, a general try-catch statement
 * gets these exceptions, logs them as warning and returns an empty tooltip string.
 * 
 */
    private String getBoatToolTip(BoatString bs, HashMap<UUID, BoatReservationRecord> nextSingleReservationForBoats, Boolean showReservation, NameExtension extension) {

   		try {

   	    	if (bs!=null) {
   	   			String boatName;

   	    		BoatListItem bli = (BoatListItem) bs.getRecord();
   	    		if (bli.boat != null) {
   	    			boatName=bli.boat.getName();//.getBoatNameAsString(System.currentTimeMillis());
   	    		} else {
   	    			boatName=bs.getName();
   	    		}
   	    	
   	    		String boatDestination="";
   	    		String boatVariant="";
   	    		String boatStatus=bli.boatStatus.getCurrentStatus();
   	    		boolean isBoatStatusOnTheWater = BoatStatusRecord.STATUS_ONTHEWATER.equals(boatStatus);
   	    		boolean isBoatStatusAvailable = (isBoatStatusOnTheWater ? false : BoatStatusRecord.STATUS_AVAILABLE.equals(boatStatus));
   	    		String boatRuderErlaubnis="";
   	    		String boatReservation="";

   	    		//data if boat is on the water
   	    		String boatTimeEntry=EfaUtil.getTimeStamp(bli.boatStatus.getLastModified());
   	    		String boatComment=bli.boatStatus.getComment();
   	    		if (boatComment==null) {boatComment="";}
   	    		
   	    		// reservations only relevant if boat is available or NOT available.
   	    		// boats on the water only get destination strings.
   				if (bli.boat!=null && showReservation && (!isBoatStatusOnTheWater)) {
   					boatReservation= getBoatReservationString(bli.boat.getId(), nextSingleReservationForBoats, EfaUtil.getRemainingMinutesToday(), true);
   					if (boatReservation==null) {boatReservation="";}
   				}
   				
   	    		if (isBoatStatusOnTheWater) {
   	        		if(bli.boat != null) {
	   	    			//boatName=bli.boat.getName();
	   	        		boatDestination=bli.boatStatus.getDestination();
	   	        		if (boatDestination==null) {boatDestination="";};
   	        		} 
   	        		
   	    		} else if (isBoatStatusAvailable) {
   	    			boatTimeEntry="";
   	    			if (bli.boat!=null) {
   		    			boatVariant=bli.boat.getDetailedBoatType(bli.boat.getVariantIndex(bli.boatVariant));
   		    			
   		    			String groups = bli.boat.getAllowedGroupsAsNameString(System.currentTimeMillis());
   		                if (groups.length() > 0) {
   		                	boatRuderErlaubnis = (boatRuderErlaubnis.length() > 0 ? boatRuderErlaubnis + ", "
   		                            : groups);
   		                }
   	    			}
   	    		}

   	    		StringBuilder sbResult = new StringBuilder(200);
   	    		//concat is the fastest way to build strings
   	    		sbResult.append("<html><body><table border=\"0\"><tr ");
   	    		sbResult.append(this.cacheToolTipBgColorText);
   	    		sbResult.append("><td align=\"left\"><b>");
   	    		sbResult.append(this.cacheToolTipFontColorOpeningTag);
   	    		sbResult.append(EfaUtil.escapeHtml(boatName));
   	    		sbResult.append(this.cacheToolTipFontColorClosingTag);
   	    		sbResult.append("</b>&nbsp;&nbsp;&nbsp;");
   	    		sbResult.append(this.cacheToolTipFontColorOpeningTag);
   	    		if (boatTimeEntry!=null && !boatTimeEntry.isEmpty()) {
   	    			sbResult.append(EfaUtil.escapeHtml(boatTimeEntry));
   	    		}
   	    		sbResult.append(this.cacheToolTipFontColorClosingTag);
   	    		sbResult.append("</td></tr>"); 
   	    		
   	    		if (!boatReservation.isEmpty()) {
   	    			sbResult.append("<tr><td align=\"left\" colspan=2>");
   	    			sbResult.append(EfaUtil.escapeHtml(boatReservation));
   	    			sbResult.append("</td></tr>");
   	    		}
   	    		
	    		Boolean bSeparateBoatVariant=(!boatComment.isEmpty() || !boatReservation.isEmpty());
	    		
   	    		if (!boatDestination.isEmpty()) {
   	    			//den Text vor der destination entfernen
   	    			if (!boatComment.isEmpty()) {
   	    				int iPos=boatComment.indexOf(boatDestination);
   	    				if (iPos>0) {
   	    					boatComment=boatComment.substring(iPos);
   	    				}
   	    				try {
   	    	   	    		String boatStatusText=BoatStatusRecord.getStatusDescription(boatStatus);   	    					
   	    					boatComment=boatComment.replace(boatName, "").replace(boatStatusText,"")
   	    							.replace(boatDestination, "").replaceAll(";", ";\n");
   	    				} catch (Exception e){
   	    					Logger.log(e);
   	    				}
   	    				
   	    			}
   	    			sbResult.append("<tr><td colspan=2>");
   	    			sbResult.append(EfaUtil.escapeHtml(boatDestination));
   	    			sbResult.append("</td></tr><tr><td align=\"left\" colspan=2>");
   	    			sbResult.append(EfaUtil.escapeHtmlWithLinefeed(boatComment));
   	    			sbResult.append("</td></tr>");
   	    		} else {
   		    		if (boatComment!=null) {
   		    			String boatStatusText="";
   		    			if (bli.boatStatus!=null) {
   		    					boatStatusText=BoatStatusRecord.getStatusDescription(boatStatus);
   		    			}
    					boatComment=boatComment.replace(boatName, "").replace(boatStatusText,"")
   	    							.replaceAll(";", ";\n").trim();
   		    			if (!boatComment.isEmpty()) {
	    					sbResult.append("<tr><td align=\"left\" colspan=2>");
	    					sbResult.append(EfaUtil.escapeHtmlWithLinefeed(boatComment));
	    					sbResult.append("</td></tr>");
   		    			}
   		    		}
   	    		}
   	    		if (bSeparateBoatVariant) {
   	    			sbResult.append("<tr></tr>");
   	    		}
    			if (!boatVariant.isEmpty()) {
   	    			sbResult.append("<tr><td align=\"left\" colspan=2>");
   	    			if (bSeparateBoatVariant) {
   	    				sbResult.append("<b>");
   	    			}
   	    			sbResult.append(EfaUtil.escapeHtml(boatVariant));
   	    			if (bSeparateBoatVariant) {
   	    				sbResult.append("</b>");
   	    			}
   	    			sbResult.append("</td></tr>");
   	    		}
    			
   	    		if (!boatRuderErlaubnis.isEmpty()) {
   	    			sbResult.append("<tr><td align=\"left\">");
   	    			sbResult.append(International.getMessage("nur für {something}", ""))
   	    				.append("</td><td>")
   	    				.append(EfaUtil.escapeHtml(boatRuderErlaubnis));
   	    			sbResult.append("</td></tr>");
   	    		}
   	    		
    			// Extensions only for boats which are available
	    		if (extension!=null && isBoatStatusAvailable) {
    				if (extension.field1Value!=null && !extension.field1Value.isEmpty()) {
    					sbResult.append("<tr><td align=\"left\">")
    					.append(EfaUtil.escapeHtml(extension.field1Caption))
    					.append("</td><td>")
    					.append(EfaUtil.escapeHtml(extension.field1Value))
    					.append("</td></tr>");
    				}
    				if (extension.field2Value!=null && !extension.field2Value.isEmpty()) {
    					sbResult.append("<tr><td align=\"left\">")
						.append(EfaUtil.escapeHtml(extension.field2Caption))
						.append("</td><td>")
						.append(EfaUtil.escapeHtml(extension.field2Value))
						.append("</td></tr>");
					}
	    		}
		    		


   	    	
   	    		sbResult.append("</table></body></html>");
   	    		return sbResult.toString();
   	    		
   	    	} else {//BoatString is null
   	    		return null;
   	    	}
   		} catch (Exception pe) {
   			//just in case some item of the BoatString could not be resolved as they may be 
   			//unexpectedly null
            Logger.log(Logger.WARNING, Logger.MSG_ERROR_EXCEPTION, pe.getMessage()+ " "+ (pe.getCause()));
            return null;
		}    

    }
    // is the current comment beginning with "Bootsschaden" in the corresponding locale? 
    private boolean isCommentBoatDamage(String s) {
        return (s != null && s.startsWith(STR_BOOTSSCHADEN + ": "));
    }

    // is the current comment beginning with "Reserviert für" in the corresponding locale? 
    private boolean isCommentBoardReservation(String s) {
    	return (s != null && s.toLowerCase().startsWith(STR_RESERVIERT_FUER));	
    }
    
    /*
     * Bestimmt die nächste mögliche Reservierung für das genannte Boot.
     * Dabei wird eine gerade aktive Reservierung ebenfalls gewählt, so dass die ermittelte Reservierung nicht zwingend   
     * in der Zukunft liegen muss.
     */
    private BoatReservationRecord getCurrentOrNextUpcomingReservation(UUID boatID, HashMap<UUID, BoatReservationRecord> nextSingleResForBoats) {
    	return (nextSingleResForBoats == null ? null : nextSingleResForBoats.get(boatID));
    }
    
    private String getBoatReservationString(UUID boatID, HashMap<UUID, BoatReservationRecord> nextSingleResForBoats, long lookAheadMinutes, Boolean buildForTooltip) {

    	//ab hier bauen wir die Reservierungsinfo auf.
        DataTypeDate today = new DataTypeDate(System.currentTimeMillis());
        
        BoatReservationRecord res = getCurrentOrNextUpcomingReservation(boatID, nextSingleResForBoats);
        if (res == null) { 
        	return null; // es gibt keine passende Reservierung für das Boot
        } else {
        	String prefix = "";
        	
        	if(buildForTooltip) {
	        	if (lookAheadMinutes>0) {
	        		prefix=International.getString("Reserviert von") + " ";
	            	return prefix + res.getPersonAsName() + " " + International.getMessage("ab {timestamp}", res.getDateTimeFromDescription()) + " ("+ res.getReason()+ ")";
	        	} else {
	        		prefix = International.getString("Reserviert von") + " ";
	            	return prefix + res.getPersonAsName() + " " + International.getMessage("bis {timestamp}", res.getDateTimeToDescription())+ " ("+ res.getReason()+ ")";
	        	}
        	} else { // build for secondary Item in List
        		//search for Reservations in the future
        		if (res.getType().equals(BoatReservationRecord.TYPE_ONETIME)) {
        		
	        		//if (lookAheadMinutes<=0) {//aktuell laufende Reservierungen?
        			if (res.getReservationValidInMinutes(true)<=0) {
	        			if ((res.getDateTo().compareTo(today)==0) && (res.getTimeTo() != null)) {
	        				//Reservierung endet heute? dann nur noch Uhrzeit anzeigen
	        				return International.getMessage("Reserviert(r)_bis_{timestamp}", res.getTimeTo().toString(false)).trim();
	        			} else {
	        				//sonst das vollständige Datum.
	        				return International.getMessage("Reserviert(r)_bis_{timestamp}", res.getDateTimeToDescription()).trim();
	        			}
	        		} else {
	            		return International.getMessage("Reserviert(r)_ab_{timestamp}", res.getTimeFrom().toString(false)).trim();
	        		}
	        	} else if (res.getType().equals(BoatReservationRecord.TYPE_WEEKLY) 
	        			  || res.getType().equals(BoatReservationRecord.TYPE_WEEKLY_LIMITED)){
	        		if (res.getReservationValidInMinutes(true)<=0) {//aktuell laufende Reservierungen? //weekly ist immer am aktuellen Tag..
	        			return International.getMessage("Reserviert(r)_bis_{timestamp}", res.getTimeTo().toString(false)).trim();
	        		} else {
	            		return International.getMessage("Reserviert(r)_ab_{timestamp}", res.getTimeFrom().toString(false)).trim();	
	        		}
	        	} 
	        }
        	return null;

        }
        
    }        
    private String getSecondaryItem(BoatStatusRecord bs, HashMap<UUID, BoatReservationRecord> nextSingleReservationForBoats, Boolean showDestination, Boolean showReservation) {
    	String showInList = bs.getShowInList();

    	if (showReservation && showInList.equals(BoatStatusRecord.STATUS_AVAILABLE)) {
	    		//available list: show next reservation today as secondary item
	    		return getBoatReservationString(bs.getBoatId(), nextSingleReservationForBoats, EfaUtil.getRemainingMinutesToday(), false);

    	} else if (showDestination && showInList.equals(BoatStatusRecord.STATUS_ONTHEWATER) ) {
    		//Boat is on the water: we show the destination as secondary item
    			return bs.getDestination();

    	} else if (showInList.equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
    		//not available list: show "Bootsschaden" for defect boats,
    		//or the end of the current reservation
    		if (isCommentBoatDamage(bs.getComment())) {
    			return International.getString("Bootsschaden");
    			
    		} else if (showReservation && isCommentBoardReservation(bs.getComment())) {
    				return getBoatReservationString(bs.getBoatId(), nextSingleReservationForBoats, 0, false);
    		} else {
    			if (bs.getLogbookRecord()==null) {
    				// A not available boat, which has no logbook record - has been put manually to the not available list.
    				// let's see if there is a comment we can show
    				return bs.getComment(); // may as well be null
    			} else {
	    			if (showDestination) { 
		    			//Boat is not available, but neither damage nor reservation.
		    			//so maybe it's a boat on a multi-day tour, regatta or whatsoever.
		    			//if the current BoatStatus has a destination set, show the destination.
		        		 return bs.getDestination();
	    			}
    			}
    		}

    	}
    	return null;
    }
    
    public void setPersonStatusData(Vector<PersonRecord> v, String other) {
        Vector<ItemTypeListData> vdata = sortMemberList(v);
        if (other != null) {
            vdata.add(0, new ItemTypeListData(other, other, null, null, null, false, -1));
            this.other_item_text=other;
        }
        clearIncrementalSearch();
        list.setSelectedIndex(-1);
        setItems(vdata);
        showValue();
    }

    Vector sortMemberList(Vector<PersonRecord> v) {
        if (v == null || v.size() == 0) {
            return v;
        }

        Boolean buildToolTips = Daten.efaConfig.getValueEfaBoathouseExtdToolTips();
        BoatString[] a = new BoatString[v.size()];
        String name;
        for (int i = 0; i < v.size(); i++) {
            PersonRecord pr = v.get(i);
            name=pr.getQualifiedName();
            // new code for performance
            a[i] = new BoatString(SEATS_OTHER, 0, TYPE_OTHER, RIGGER_OTHER, name,
            		false, false, false //dont sort by seats, rigger, type)
            		);
            BoatListItem item = new BoatListItem();
            item.list = this;
            item.text = name;
            item.person = pr;
            a[i].setRecord(item);
            
        }
        Arrays.sort(a);
        
        this.cacheToolTipBgColorText=(Daten.efaConfig.getToolTipSpecialColors() ? "bgcolor=\"#"+EfaUtil.getColor(Daten.efaConfig.getToolTipHeaderBackgroundColor())+"\"": "");
   		this.cacheToolTipFontColorOpeningTag = (Daten.efaConfig.getToolTipSpecialColors() ? "<font color=\"#"+EfaUtil.getColor(Daten.efaConfig.getToolTipHeaderForegroundColor())+"\">": "");
   		this.cacheToolTipFontColorClosingTag = (Daten.efaConfig.getToolTipSpecialColors()? "</font>": "");
        
        String personListField1 = Daten.efaConfig.getValueEfaDirektBoathouseExtPersonField1();
        String personListField2 = Daten.efaConfig.getValueEfaDirektBoathouseExtPersonField2();
        String personListField1Caption = (!personListField1.isEmpty() ? Daten.efaConfig.personExtFields.get(personListField1) : null);
        String personListField2Caption = (!personListField2.isEmpty() ? Daten.efaConfig.personExtFields.get(personListField2) : null);
  		
        boolean buildExtraInfo = (personListField1 != null && !personListField1.isEmpty()) || (personListField2 != null && !personListField2.isEmpty());
        
        // use arraylist for building the list, as it is more efficient than vector for adding items one by one, 
        // and convert back to vector at the end for compatibility with the rest of the code
        // this is more efficient than simply using a vector from the beginning.

        ArrayList<ItemTypeListData> tmp = new ArrayList<>(a.length + 16);
        char lastChar = ' ';
        char firstChar= ' ';
        for (int i = 0; i < a.length; i++) {
            name = a[i].getName();
            if (name.length() > 0) {
            	firstChar=a[i].getNormName().charAt(0);
                if (firstChar != lastChar) {
                	lastChar = firstChar;
                	tmp.add(new ItemTypeListData("---------- " + Character.toString(lastChar).toUpperCase() + " ----------", null, null, null, null, true, SEATS_OTHER));
                }
                
                NameExtension nameExtension = null;
                if (buildExtraInfo) {
                	nameExtension = getPersonNameExtension((BoatString) a[i],  personListField1, personListField2, personListField1Caption, personListField2Caption);
                }
                tmp.add(new ItemTypeListData(name, 
					    (buildToolTips ? getPersonToolTip(name, (BoatString) a[i], nameExtension) : ""), 
	            		(nameExtension!=null ? nameExtension.getAsCommaSeparatedList() : null),
	            		null, a[i].getRecord(),  false, SEATS_OTHER));
            }
        }
        // Convert back to Vector for compatibility
        Vector<ItemTypeListData> vv = new Vector<ItemTypeListData>(tmp.size());
        vv.addAll(tmp);
        return vv;
    }

    private String getPersonToolTip(String name, BoatString bs, NameExtension nameExtension) {
    	
    	if (bs == null) {
    		return name;
    	} else {
    		BoatListItem bli = (BoatListItem) bs.getRecord(); 
    		
    		if (bli ==null || nameExtension == null ) {
        		if (bli != null && bli.person != null) {
	    			String alias = bli.person.getInputShortcut();
	        		if ((alias != null) && (!alias.isEmpty())) {
	        			return name + " ("+alias+")";
	        		}
        		}
	        	return name;
    		} else {
    			StringBuilder sbResult = new StringBuilder(200);
    			sbResult.append("<html><body><table border=\"0\"><tr ");
   	    		sbResult.append(this.cacheToolTipBgColorText);
   	    		sbResult.append("><td align=\"left\" colspan=2><b>");
   	    		sbResult.append(this.cacheToolTipFontColorOpeningTag);
   	    		sbResult.append(EfaUtil.escapeHtml(name));
   	    		sbResult.append(this.cacheToolTipFontColorClosingTag);
   	    		sbResult.append("</b></td></tr>");
   	    		
   	    		if (nameExtension.field1Value!=null && !nameExtension.field1Value.isEmpty()) {
   	    			sbResult.append("<tr><td align=\"left\">");
   	    			sbResult.append(EfaUtil.escapeHtml(nameExtension.field1Caption));
   	    			sbResult.append("</td><td align=\"left\">&nbsp;&nbsp;&nbsp;");
   	    			sbResult.append(EfaUtil.escapeHtml(nameExtension.field1Value));
   	    			sbResult.append("</td></tr>");
   	    		}
   	    		if (nameExtension.field2Value!=null && !nameExtension.field2Value.isEmpty()) {
   	    			sbResult.append("<tr><td align=\"left\">");
   	    			sbResult.append(EfaUtil.escapeHtml(nameExtension.field2Caption));
   	    			sbResult.append("</td><td align=\"left\">&nbsp;&nbsp;&nbsp;");
   	    			sbResult.append(EfaUtil.escapeHtml(nameExtension.field2Value));
   	    			sbResult.append("</td></tr>");
   	    		}
    			
   	    		return sbResult.append("</table></body></html>").toString();
    		
    		}
    	}

    }
    
    public BoatListItem getSelectedBoatListItem() {
        if (list == null || list.isSelectionEmpty()) {
            return null;
        } else {
            Object o = getSelectedValue();
            if (o != null) {
                return (BoatListItem)o;
            }
            return null;
        }
    }
    
    public static BoatListItem createBoatListItem(int mode) {
        BoatListItem b = new BoatListItem();
        b.mode = mode;
        return b;
    }

  //set text color of label above boathouse lists
    public void setColor(Color c) {
        this.color = c;
        if (label!=null) {
            label.setForeground(color);
            label.repaint();
        }
    }

    // set background color of label above boathouse lists, 
    // set transparent if background is set to null
    public void setBackgroundColor(Color c) {
        this.backgroundColor = c;

        if (label != null) {
            if (backgroundColor != null) {
            	label.setBackground(backgroundColor);
            	label.setOpaque(true);
            } else {
            	label.setOpaque(false);
            }
            label.repaint();
        }
    }    
    
    public static class BoatListItem {
        public int mode;
        public ItemTypeBoatstatusList list;
        public String text;
        public BoatRecord boat;
        public BoatStatusRecord boatStatus;
        public int boatVariant = 0;
        public PersonRecord person;
    }
    
    public static class NameExtension{
    	public String field1Value;
    	public String field2Value;
    	public String field1Caption;
    	public String field2Caption;
		
		public NameExtension(String ext1, String ext2, String field1Caption, String field2Caption) {
			this.field1Value = ext1;
			this.field2Value = ext2;
			this.field1Caption = field1Caption;
			this.field2Caption = field2Caption;
		}
		
		public String getAsCommaSeparatedList() {
			if ((field1Value!=null && !field1Value.isEmpty()) || (field2Value!=null && !field2Value.isEmpty())) {
				return ((field1Value!=null 
						? field1Value.concat((field2Value!=null ? ", " + field2Value : "")) 
						: (field2Value!=null ? field2Value : "")));
			} else {
				return null;
			}
		}
		
    }
}

