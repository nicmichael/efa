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

/**
 * ItemTypeBoatstatusList is a specialized ItemTypeList for displaying boat status information in the EfaBoathouseFrame.
 * It provides methods to set and sort boat status data, as well as to handle person status data.
 * 
 * It is used for 
 * - available boats,
 * - boats on the water,
 * - unavailable boats, and
 * - persons (if activated in efaConfig).
 * 
 */
public class ItemTypeBoatstatusList extends ItemTypeList {
    public static final int SEATS_OTHER = 99;
    public static final String TYPE_OTHER = "";
    public static final String RIGGER_OTHER = "";
    EfaBoathouseFrame efaBoathouseFrame;
    private final String STR_RESERVIERT_FUER_PREFIX=International.getString("Reserviert für").toLowerCase();
    private final String STR_BOOTSSCHADEN=International.getString("Bootsschaden");
    private final String STR_RESERVIERT_FUER = International.getString("Reserviert für");
    
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
    
    /**
	 * Sets the person status data for the list, sorting it and adding an "other" entry if provided.
	 * 
	 * @param v Vector of PersonRecord objects representing the persons to be displayed.
	 * @param other Optional string for an "other" entry to be added at the top of the list.
	 */
    public void setPersonStatusData(Vector<PersonRecord> v, String other) {
        setIconSize(0, 0); // person list shows no group icons
        Vector<ItemTypeListData> vdata = sortMemberList(v);
        if (other != null) {
        	BoatListItem item= new BoatListItem();
        	item.text = other;
        	vdata.add(0, new ItemTypeListData(other, null, null, null, item, false, -1));
            this.other_item_text=other;
        }
        clearIncrementalSearch();
        list.setSelectedIndex(-1);
        setItems(vdata);
        showValue();
    }
    
    /**
     * Sets the boat status data for the list, sorting it and adding an "other" entry if provided.
     * @param v Vector of BoatStatusRecord objects representing the boats to be displayed.
     * @param logbook Logbook object used to retrieve additional information about the boats.
     * @param other Optional string for an "other" entry to be added at the top of the list.
     * @param todaysReservations Vector of BoatReservationRecord objects representing the reservations for today, used to determine reservation information for each boat.
     */
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
	 * Also if at least one group with a valid color exists, the icon size is set for the boatlist, 
	 * otherwise it is zero.
	 * 
	 * @return a map with group id as key and the color for the group as value.
	 *          If no groups exist, an empty map is returned.
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
	        int iconSize = (result.size() > 0 ? Daten.efaConfig.getValueEfaDirekt_BthsFontSize() : 0);
	        setIconSize(iconSize, iconSize);
	    } catch(Exception e) {
	        Logger.logdebug(e);
	    }
    	return result;
    }   
    
 /**
	 * Returns a map of the next reservation for each boat from the provided list of today's reservations.
	 * The map contains the boat ID as the key and the corresponding BoatReservationRecord as the value.
	 * If there are no reservations, an empty map is returned.
	 *
	 * @param todaysReservations Vector of BoatReservationRecord objects representing today's reservations.
	 * @return HashMap with boat ID as key and the next BoatReservationRecord as value.
	 */
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
    

    // As there may be several hundreds of boats in a list, this method runs within the main thread of efa boathouse,
    // and efaboathouse is often run on an raspberry pi, this method needs to be optimized for performance.
    //
    // So any element which is read within a loop is put into a variable - so for instance, the efaConfig values are determined just once
    // instead of for every single BoatStatusRecord element.
    /**
     * Sorts the provided list of BoatStatusRecords and returns a sorted list of ItemTypeListData elements
     * for display in the boat lists in efaBoatHouse.
     * @param v current BoatStatusRecords
     * @param logbook logbook to look into
     * @param todaysReservations Vector of BoatReservationRecords which are valid today. Sorted by Boat and timestamp when the reservation gets valid.
     * @return sorted Boat list elements which can be put into the boat lists in efaBoatHouse
     */
    private Vector<ItemTypeListData> sortBootsList(Vector<BoatStatusRecord> v, Logbook logbook,Vector <BoatReservationRecord> todaysReservations) {
    	try {
    	// return empty list if no data available.
    	if (v == null || v.size() == 0 || logbook == null) {
    		return new Vector <ItemTypeListData>(0);
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
        // Null-check before isEmpty(), consistent with buildExtraInfo below and with sortMemberList
        String boatListField1Caption = (boatListField1 != null && !boatListField1.isEmpty() ? EfaConfig.boatExtFields.get(boatListField1) : null);
        String boatListField2Caption = (boatListField2 != null && !boatListField2.isEmpty() ? EfaConfig.boatExtFields.get(boatListField2) : null);
        // Read once per list build; the weight unit is the same for every boat in this list
        String weightUnit = Daten.efaConfig.getValueDefaultWeightUnit();
        // Read once per list build instead of for every boat without a type
        boolean boatListIndividualOthers = Daten.efaConfig.getValueEfaDirekt_boatListIndividualOthers();
        
        boolean buildExtraInfo = (boatListField1 != null && !boatListField1.isEmpty()) || (boatListField2 != null && !boatListField2.isEmpty());
        
        //Determine whether or not to show the colored icons in front of a boat
        //which visualize which group of persons can use a boat.
        //Colored icons get shown when there exists at least one valid person group.
        
        HashMap<UUID, Color> groupColors = getGroupColors(now);
     // build map of next reservation per boat (first occurrence in sorted todaysReservations)
        HashMap<UUID, BoatReservationRecord> nextSingleReservationForBoats = getNextSingleReservationForBoats(todaysReservations);
        // Time-dependent and translated values are determined once per list build and passed down,
        // instead of being re-read for every single boat (this runs on the GUI thread of a Raspberry Pi).
        long remainingMinutesToday = EfaUtil.getRemainingMinutesToday();
        DataTypeDate today = new DataTypeDate(now);


        
        // Build the list elements for the boat list.
        ArrayList<BoatString> bsv = new ArrayList<BoatString>(v.size() * 2);
        for (int i = 0; i < v.size(); i++) {
        	try {
            BoatStatusRecord sr = v.get(i);
            DataTypeIntString srEntryNo = sr.getEntryNo();
            String currentStatus = sr.getCurrentStatus();
            boolean isCurrentStatusOnTheWater = BoatStatusRecord.STATUS_ONTHEWATER.equals(currentStatus);
            boolean isCurrentStatusAvailable = (isCurrentStatusOnTheWater ? false : BoatStatusRecord.STATUS_AVAILABLE.equals(currentStatus));
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

                /* Old Code
               
				   BoatString bs = new BoatString(seat, variant, 
                		(r != null ? r.getTypeType(0) : EfaTypes.TYPE_BOAT_OTHER), // type
                		(r != null ? r.getTypeRigging(0) : EfaTypes.TYPE_RIGGING_OTHER), //rigger)
                		(isCurrentStatusOnTheWater || r == null ? sr.getBoatText() : r.getQualifiedName()), //name
                		sortByAnzahl,
                		sortByRigger,
                		sortByType);
                		*/
                
                // Type and rigging must describe the variant that is displayed, not always list position 0
                int variantIdx = (r != null ? r.getVariantIndex(variant) : -1);
                String boatType = (r != null && variantIdx >= 0 ? r.getTypeType(variantIdx) : EfaTypes.TYPE_BOAT_OTHER);
                String boatRigging = (r != null && variantIdx >= 0 ? r.getTypeRigging(variantIdx) : EfaTypes.TYPE_RIGGING_OTHER);

                BoatString bs = new BoatString(seat, variant, boatType, boatRigging,
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
        } catch (Exception e) {
			Logger.log(Logger.WARNING, "Error while sorting boat list: " + e.getMessage(), e);
		}
		}// for all boatstatus records

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
                    // Compute the key once; it is needed for both the check and the lookup, and this runs for every boat
                    String seatsKey = EfaTypes.getSeatsKey(curBS.getSeats(), curBS.getRigger());
                    if (seatsKey != null) {
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, seatsKey);
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
                // TODO: Duplicate boat names are not supported in efa2 yet.
                // The lookup below is kept disabled until duplicate entries are handled.
                /*

                 DatenFelder d = Daten.fahrtenbuch.getDaten().boote.getExactComplete(removeDoppeleintragFromBootsname(a[i].name));
                 if (d != null) {
                 s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, d.get(Boote.ANZAHL));
                 } else {
                 */
                s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_OTHER);
                if (boatListIndividualOthers && curBS.getType() != null) {
                    s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_BOAT, curBS.getType());
                }

                //}
            }
            anz = curBS.getSeats();
            if (sortByAnzahl || sortByType) {
            	if (s == null) { s=""; }
                String newSep = s; //LIST_SECTION_STRING_START + s + LIST_SECTION_STRING_END;
                if (!newSep.equals(lastSep)) {
                    tmp.add(new ItemTypeListData(newSep, null, null, null, null, true, anz));
                }
                lastSep = newSep;
            }
            BoatListItem blitem=(BoatListItem) curBS.getRecord();

            // Resolve the secondary item per boat: a failure for one boat must not
            // hide all other boats, as the outer catch would return an empty list.
            String secondaryItem;
            try {
                secondaryItem = getSecondaryItem(blitem.boatStatus, nextSingleReservationForBoats,
                        showDestination, showReservation, remainingMinutesToday, today);
            } catch (Exception e) {
                Logger.log(e);
                secondaryItem = null;
            }
            NameExtension nameExtension = null;
            
            if (buildExtraInfo) {
            	nameExtension = getBoatNameExtension(curBS, blitem, boatListField1, boatListField2, boatListField1Caption, boatListField2Caption, weightUnit);
            }
            
            tmp.add(new ItemTypeListData(curBS.getName(), 
            		(buildToolTips ? getBoatToolTip(curBS,nextSingleReservationForBoats, showReservation, nameExtension, now, remainingMinutesToday, today) : ""), 
            		secondaryItem,
            		(buildExtraInfo && nameExtension != null ? nameExtension.getAsCommaSeparatedList() : null),
            		blitem, false, -1, null, curBS.getColors()));
        }
        // convert to Vector for API compatibility
        Vector<ItemTypeListData> vv = new Vector<ItemTypeListData>(tmp.size());
        vv.addAll(tmp);
        return vv;
        } catch (Exception ee) {
            Logger.log(Logger.WARNING, Logger.MSG_ERROR_EXCEPTION, ee.getMessage()+ " "+ (ee.getCause()));
        	return new Vector <ItemTypeListData>(0);
    	}
    }
    
    
    /**
	 * Creates a cache for the most common seat numbers (1-6 and 8) to speed up the retrieval of the seat number string 
	 * for these boats, as this is a very common operation when sorting by seats. 
	 * For other seat numbers, the string is retrieved directly from efaTypes without caching, 
	 * as these are less common and caching them would not significantly improve performance.
	 * 
	 * @return HashMap with seat number as key and corresponding string representation as value.
	 */
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
    private NameExtension getBoatNameExtension(BoatString bs, BoatListItem bi, String boatListField1, String boatListField2, String boatListField1Caption, String boatListField2Caption, String weightUnit) {
    	try {
        	String showInList = bi.boatStatus.getShowInList();//boatstatus is always set 
			String ext1=null;
			String ext2=null; 
			
    		if (showInList.equals(BoatStatusRecord.STATUS_AVAILABLE)){

    			if (bi.boat != null) {// is it a boatrecord?

    				if (boatListField1!=null && !boatListField1.isEmpty()) {
    					ext1 = bi.boat.getStringValueFromField(boatListField1);
    					// weightUnit may be null if not configured; concat(null) would throw and silently drop the extension
    					if (ext1 != null && boatListField1.equals(BoatRecord.MAXCREWWEIGHT) && weightUnit != null) {
    						ext1=ext1.concat(weightUnit);
    					}
    				}
    				if (boatListField2!=null && !boatListField2.isEmpty()) {
    					ext2 = bi.boat.getStringValueFromField(boatListField2);
    					if (ext2 != null && boatListField2.equals(BoatRecord.MAXCREWWEIGHT) && weightUnit != null) {
    						ext2=ext2.concat(weightUnit);
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
    
    /**
	 * Returns some additional information on the current Boatstring / Personrecord.
	 * which Boatrecord may point to a boat, but also to a person record.
	 * 
	 * @param bs Boat String with already some determined strings
	 * @param bi the BoatListItem where we can get the boatRecord and personRecord from.
	 * @param personListField1 the first field to retrieve from the PersonRecord
	 * @param personListField2 the second field to retrieve from the PersonRecord
	 * @param personListField1Caption the caption for the first field
	 * @param personListField2Caption the caption for the second field
	 * @return a NameExtension object containing the values of the specified fields from the PersonRecord, or null if the BoatListItem does not contain a PersonRecord.
	 */
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
    
	/**
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
	 * @param bs BoatString with already some determined strings
	 * @param nextSingleReservationForBoats map of next reservations for boats, to be able to show reservation information in the tooltip
	 * @param showReservation boolean to determine whether or not to show reservation information in the tooltip
	 * @param extension NameExtension object containing additional information to be shown in the tooltip
	 * @param now the timestamp of the current list build, used for group validity
	 * @param remainingMinutesToday minutes left today, determined once per list build
	 * @param today the current date, determined once per list build
	 * 
	 * @return String containing the tooltip text for the boat, or an empty string if an exception occurs.
	 * 
	 */
    private String getBoatToolTip(BoatString bs, HashMap<UUID, BoatReservationRecord> nextSingleReservationForBoats, boolean showReservation, NameExtension extension, long now, long remainingMinutesToday, DataTypeDate today) {

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
   	    		// Timestamp is only shown for boats that are not available; skip the formatting work otherwise
   	    		String boatTimeEntry = (isBoatStatusAvailable ? "" : EfaUtil.getTimeStamp(bli.boatStatus.getLastModified()));
   	    		String boatComment=bli.boatStatus.getComment();
   	    		if (boatComment==null) {boatComment="";}
   	    		
   	    		// reservations only relevant if boat is available or NOT available.
   	    		// boats on the water only get destination strings.
   				if (bli.boat!=null && showReservation && (!isBoatStatusOnTheWater)) {
    				boatReservation= getBoatReservationString(bli.boat.getId(), nextSingleReservationForBoats, remainingMinutesToday, true, today);
    				if (boatReservation==null) {boatReservation="";}
   				}
   				
   	    		if (isBoatStatusOnTheWater) {
   	        		if(bli.boat != null) {
	   	    			//boatName=bli.boat.getName();
	   	        		boatDestination=bli.boatStatus.getDestination();
	   	        		if (boatDestination==null) {boatDestination="";};
   	        		} 
   	        		
   	    		} else if (isBoatStatusAvailable) {
   	    			
   	    			if (bli.boat!=null) {
   		    			boatVariant=bli.boat.getDetailedBoatType(bli.boat.getVariantIndex(bli.boatVariant));
   		    			
   		    			String groups = bli.boat.getAllowedGroupsAsNameString(now);
    		                if (groups.length() > 0) {
    		                	// boatRuderErlaubnis is always empty at this point, so a plain assignment is sufficient
    		                	boatRuderErlaubnis = groups;
    		                }
   	    			}
   	    		}

   	    		StringBuilder sbResult = new StringBuilder(300);
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
   	    			sbResult.append(EfaUtil.escapeHtmlWithLinefeed(boatReservation));
   	    			sbResult.append("</td></tr>");
   	    		}
   	    		
	    		boolean bSeparateBoatVariant=(!boatComment.isEmpty() || !boatReservation.isEmpty());
	    		
   	    		if (!boatDestination.isEmpty()) {
   	    			//den Text vor der destination entfernen
   	    			if (!boatComment.isEmpty()) {
   	    				int iPos=boatComment.indexOf(boatDestination);
   	    				if (iPos>0) {
   	    					boatComment=boatComment.substring(iPos);
   	    				}
   	    				try {
   	    					String boatStatusText=BoatStatusRecord.getStatusDescription(boatStatus);
    	    				// A null status text must not abort the comment cleanup: String.replace(null, ...) would throw
    	    				if (boatStatusText == null) {
    							boatStatusText = "";
    						}
    						boatComment=boatComment.replace(boatName, "").replace(boatStatusText,"")
    							.replace(boatDestination, "").replace(";", ";\n");
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
   	    			// Comment is never null here, so no outer null check is needed
   	    			
   	    			// Remove the "Reserviert für" prefix from the comment if it starts with it, 
   	    			// to avoid redundancy in the tooltip (otherwise, the reservation info would be shown twice)
   	    			if (boatComment.toLowerCase().startsWith(STR_RESERVIERT_FUER_PREFIX)) {
   	    				boatComment="";
   	    			}
   	    			
	    			String boatStatusText=BoatStatusRecord.getStatusDescription(boatStatus);
	    			// A null status text must not abort the whole tooltip: String.replace(null, ...) would throw
	    			if (boatStatusText == null) {
		    			boatStatusText = "";
	    			}
	    			boatComment=boatComment.replace(boatName, "").replace(boatStatusText,"")
	    						.replace(";", ";\n").trim();
   	    			if (!boatComment.isEmpty()) {
	   					sbResult.append("<tr><td align=\"left\" colspan=2>");
	   					sbResult.append(EfaUtil.escapeHtmlWithLinefeed(boatComment));
	   					sbResult.append("</td></tr>");
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
    
    /**
	 * Checks if the given string starts with the localized "Bootsschaden" prefix.
	 *
	 * @param s the string to check
	 * @return true if the string starts with "Bootsschaden: ", false otherwise
	 */
    private boolean isCommentBoatDamage(String s) {
        return (s != null && s.startsWith(STR_BOOTSSCHADEN + ": "));
    }

    /** 
     * Checks if the given string starts with the localized "Reserviert für" prefix.
     * 
     * @param s the string to check
     * @return true if the string starts with "Reserviert für", false otherwise
     */
    private boolean isCommentBoardReservation(String s) {
    	return (s != null && s.toLowerCase().startsWith(STR_RESERVIERT_FUER_PREFIX));	
    }
    
    /**
	 * Retrieves the current or next upcoming reservation for the specified boat ID from the provided map of reservations.
	 * If the map is null or does not contain a reservation for the given boat ID, it returns null.
	 *
	 * @param boatID the UUID of the boat for which to retrieve the reservation
	 * @param nextSingleResForBoats a HashMap containing the next single reservations for boats, keyed by boat UUID
	 * @return the BoatReservationRecord for the specified boat ID, or null if no reservation is found
	 */
    private BoatReservationRecord getCurrentOrNextUpcomingReservation(UUID boatID, HashMap<UUID, BoatReservationRecord> nextSingleResForBoats) {
    	return (nextSingleResForBoats == null ? null : nextSingleResForBoats.get(boatID));
    }
    
    /**
	 * Builds a string representation of the boat reservation information for the specified boat ID.
	 * It retrieves the current or next upcoming reservation from the provided map and formats it based on the look-ahead time and tooltip flag.
	 *
	 * @param boatID the UUID of the boat for which to build the reservation string
	 * @param nextSingleResForBoats a HashMap containing the next single reservations for boats, keyed by boat UUID
	 * @param lookAheadMinutes the number of minutes to look ahead for upcoming reservations
	 * @param buildForTooltip a boolean indicating whether to build the string for a tooltip (true) or for a secondary item in a list (false)
	 * @param today the current date, passed in so it is not re-created for every boat
	 * @return a formatted string representing the reservation information, or null if no reservation is found
	 */
    private String getBoatReservationString(UUID boatID, HashMap<UUID, BoatReservationRecord> nextSingleResForBoats, long lookAheadMinutes, boolean buildForTooltip, DataTypeDate today) {

    	//ab hier bauen wir die Reservierungsinfo auf.
        // today is passed in by the caller, so it is not re-created for every boat
        
        BoatReservationRecord res = getCurrentOrNextUpcomingReservation(boatID, nextSingleResForBoats);
        if (res == null) { 
        	return null; // es gibt keine passende Reservierung für das Boot
        } else {
        	String prefix = "";
        	
        	if(buildForTooltip) {
        		return  EfaUtil.capitalizeFirst(
        				International.getMessage("reserviert für {name} ({reason}) {from_to}",
                                    res.getPersonAsName(),
                                    res.getReason(),
                                    "\n"+res.getReservationTimeDescription())
        				);
        	} else { // build for secondary Item in List
        		//search for Reservations in the future
        		if (BoatReservationRecord.TYPE_ONETIME.equals(res.getType())) {
        		
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
	        	} else if (BoatReservationRecord.TYPE_WEEKLY.equals(res.getType()) 
	        			  || BoatReservationRecord.TYPE_WEEKLY_LIMITED.equals(res.getType())){
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
    
    /**
	 * Determines the secondary item to display for a boat based on its status and the provided flags
	 * @param bs the BoatStatusRecord for the boat
	 * @param nextSingleReservationForBoats a map of the next single reservations for boats
	 * @param showDestination whether to show the destination for boats on the water
	 * @param showReservation whether to show reservation information for available boats
	 * 
	 * @return a string representing the secondary item to display, or null if none is applicable
	 */
    private String getSecondaryItem(BoatStatusRecord bs, HashMap<UUID, BoatReservationRecord> nextSingleReservationForBoats, 
    		boolean showDestination, boolean showReservation, long remainingMinutesToday, DataTypeDate today) {
    	String showInList = bs.getShowInList();

    	if (showReservation && showInList.equals(BoatStatusRecord.STATUS_AVAILABLE)) {
	    		//available list: show next reservation today as secondary item
	    		return getBoatReservationString(bs.getBoatId(), nextSingleReservationForBoats, remainingMinutesToday, false, today);

    	} else if (showDestination && showInList.equals(BoatStatusRecord.STATUS_ONTHEWATER) ) {
    		//Boat is on the water: we show the destination as secondary item
    			return bs.getDestination();

    	} else if (showInList.equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
    		//not available list: show "Bootsschaden" for defect boats,
    		//or the end of the current reservation
    		if (isCommentBoatDamage(bs.getComment())) {
    			return International.getString("Bootsschaden");
    			
    		} else if (showReservation && isCommentBoardReservation(bs.getComment())) {
    				return getBoatReservationString(bs.getBoatId(), nextSingleReservationForBoats, 0, false, today);
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

    /**
	 * Sorts a list of PersonRecords and returns a Vector of ItemTypeListData for display in the UI.
	 * The sorting is done based on the qualified name of the PersonRecords.
	 * Additional information can be included in the tooltip and extra info fields based on configuration.
	 *
	 * @param v Vector of PersonRecord objects to be sorted
	 * @return Vector of ItemTypeListData containing sorted person information
	 */
    Vector <ItemTypeListData>sortMemberList(Vector<PersonRecord> v) {
        if (v == null || v.size() == 0) {
        	return new Vector<ItemTypeListData>(0);
        }

        boolean buildToolTips = Daten.efaConfig.getValueEfaBoathouseExtdToolTips();
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
        // get the captions for the fields from the configuration, if they are not null or empty.
        String personListField1Caption = (personListField1 != null && !personListField1.isEmpty() ? EfaConfig.personExtFields.get(personListField1) : null);
        String personListField2Caption = (personListField2 != null && !personListField2.isEmpty() ? EfaConfig.personExtFields.get(personListField2) : null);

  		
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
	            		null, a[i].getRecord(), false, SEATS_OTHER));
            }
        }
        // Convert back to Vector for compatibility
        Vector<ItemTypeListData> vv = new Vector<ItemTypeListData>(tmp.size());
        vv.addAll(tmp);
        return vv;
    }

    /**
	 * Generates a tooltip for a person based on their name and additional information.
	 * 
	 * @param name The name of the person.
	 * @param bs The BoatString associated with the person.
	 * @param nameExtension Additional information to be displayed in the tooltip.
	 * 
	 * @return A string containing the HTML-formatted tooltip for the person.
	 */
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
    /**
	 * Checks if either the list or the filter has focus.
	 * Used for efaBoathouseFrame to determine if the list or filter is currently active.
	 * It is used to detect if the GUI needs to be reset to standard mode (where the available boat list gets the focus)
	 * 
	 * @return true if either the list or the filter has focus, false otherwise
	 */
    public boolean hasListOrFilterFocus() {
    	// list may not be initialized yet when the frame checks focus; guard against NPE
    	return this.hasFocus() || (list != null && list.hasFocus()); 
    }
    
    /**
	 * Retrieves the currently selected BoatListItem from the list.
	 * @return the selected BoatListItem, or null if no item is selected or the list is empty
	 * */
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
    
    /**
	 * Creates a new BoatListItem with the specified mode.
	 * 
	 * @param mode the mode to set for the new BoatListItem
	 * 
	 * @return a new BoatListItem instance with the specified mode
	 */
    public static BoatListItem createBoatListItem(int mode) {
        BoatListItem b = new BoatListItem();
        b.mode = mode;
        return b;
    }

    /**
	 * Sets the text color of the label above the boathouse lists.
	 * If the label is not null, it updates the foreground color and repaints the label.
	 *
	 * @param c the Color to set for the label's text
	 */
    public void setColor(Color c) {
        this.color = c;
        if (label!=null) {
            label.setForeground(color);
            label.repaint();
        }
    }

    /**
	 * Sets the background color of the label above the boathouse lists.
	 * If the label is not null, it updates the background color and opacity based on the provided color.
	 */
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
    
    /**
	 * Inner class representing an item in the boat list.
	 */
    public static class BoatListItem {
        public int mode;
        public ItemTypeBoatstatusList list;
        public String text;
        public BoatRecord boat;
        public BoatStatusRecord boatStatus;
        public int boatVariant = 0;
        public PersonRecord person;
    }
    
    /**
	 * Inner class representing additional name information for boats or persons.
	 */
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
		    // Empty strings are treated like null, so no leading or trailing comma can appear
		    boolean has1 = field1Value != null && !field1Value.isEmpty();
		    boolean has2 = field2Value != null && !field2Value.isEmpty();
		    if (has1 && has2) {
		        return field1Value + ", " + field2Value;
		    } else if (has1) {
		        return field1Value;
		    } else if (has2) {
		        return field2Value;
		    }
		    return null;
		}
		
    }
}
