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
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import javax.swing.SwingUtilities;

import de.nmichael.efa.Daten;
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
            EfaBoathouseFrame efaBoathouseFrame, boolean showFilterField, boolean showTwoColumnList) {
        super(name, type, category, description, showFilterField, showTwoColumnList);
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

        Groups groups = Daten.project.getGroups(false);
        boolean buildToolTips = Daten.efaConfig.getValueEfaBoathouseExtdToolTips();
        boolean buildExtraInfo = true;
        boolean showDestination = Daten.efaConfig.getValueEfaDirekt_showZielnameFuerBooteUnterwegs();
        boolean showReservation = Daten.efaConfig.getValueEfaBoathouseBoatListReservationInfo();
        boolean sortByAnzahl =  Daten.efaConfig.getValueEfaDirekt_sortByAnzahl();
        boolean sortByRigger = Daten.efaConfig.getValueEfaDirekt_sortByRigger();
        boolean sortByType = Daten.efaConfig.getValueEfaDirekt_sortByType();
        
        String boatListField1 = Daten.efaConfig.getValueEfaDirektBoathouseExtBoatField1();
        String boatListField2 = Daten.efaConfig.getValueEfaDirektBoathouseExtBoatField2();
        String personListField1 = Daten.efaConfig.getValueEfaDirektBoathouseExtPersonField1();
        String personListField2 = Daten.efaConfig.getValueEfaDirektBoathouseExtPersonField2();
        
        //Determine whether or not to show the colored icons in front of a boat
        //which visualize which group of persons can use a boat.
        //Colored icons get shown when there exists at least one valid person group.
        
        Hashtable<UUID, Color> groupColors = new Hashtable<UUID, Color>();
        try {
            DataKeyIterator it = groups.data().getStaticIterator();
            for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                GroupRecord gr = (GroupRecord)groups.data().get(k);
                if (gr != null && gr.isValidAt(now)) {
                    String cs = gr.getColor();
                    if (cs != null && cs.length() > 0) {
                        groupColors.put(gr.getId(), EfaUtil.getColorOrGray(cs));
                    }
                }
            }
            this.iconWidth = (groupColors.size() > 0 ? Daten.efaConfig.getValueEfaDirekt_BthsFontSize() : 0);
            this.iconHeight = this.iconWidth;
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        
        // Build the list elements for the boat list.
        Vector<BoatString> bsv = new Vector<BoatString>();
        for (int i = 0; i < v.size(); i++) {
            BoatStatusRecord sr = v.get(i);

            BoatRecord r = boats.getBoat(sr.getBoatId(), now);
            Hashtable<Integer,Integer> allSeats = new Hashtable<Integer,Integer>(); // seats -> variant
            // find all seat variants to be shown...
            if (r != null) {
                if (r.getNumberOfVariants() == 1) {
                    allSeats.put(r.getNumberOfSeats(0, SEATS_OTHER), r.getTypeVariant(0));
                } else {
                    if (sr.getCurrentStatus().equals(BoatStatusRecord.STATUS_AVAILABLE)) {
                        for (int j = 0; j < r.getNumberOfVariants(); j++) {
                            // if the boat is available, show the boat in all seat variants
                            allSeats.put(r.getNumberOfSeats(j, SEATS_OTHER), r.getTypeVariant(j));
                        }
                    } else {
                        if (sr.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
                            // if the boat is on the water, show the boat in the variant that it is currently being used in
                            DataTypeIntString entry = sr.getEntryNo();
                            if (entry != null && entry.length() > 0) {
                                LogbookRecord lr = logbook.getLogbookRecord(sr.getEntryNo());
                                if (lr != null && lr.getBoatVariant() > 0 && lr.getBoatVariant() <= r.getNumberOfVariants()) {
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

                if (r != null && seats.length < r.getNumberOfVariants()) {
                    // we have multiple variants, but all with the same number of seats
                    if (r.getDefaultVariant() > 0) {
                        variant = r.getDefaultVariant();
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
                		(sr.getCurrentStatus().equals(BoatStatusRecord.STATUS_ONTHEWATER) || r == null ? sr.getBoatText() : r.getQualifiedName()), //name
                		sortByAnzahl,
                		sortByRigger,
                		sortByType);

                // Colors for Groups
                Color[] colors = (r!=null ? r.getBoatGroupsPieColors(groupColors) : null); 

                BoatListItem item = new BoatListItem();
                item.list = this;
                item.text = bs.getName();
                item.boat = r; //r is the current boat record 
                item.boatStatus = sr;
                item.boatVariant = bs.getVariant();
                bs.setColors(colors); 
                bs.setRecord(item);

                // destination is only shown for boats on the water, and if efaConfig says that destination shall be shown
                // for boats on the water list.

                // we only have to put the destination in the item text if the two column layout is _in_active.
                // if two column layout is active, the destination is put into the BoatListItem.secondaryItem right at the end of this method.
                if (showDestination && (!this.getShowTwoColumnList())  &&
                    BoatStatusRecord.STATUS_ONTHEWATER.equals(sr.getCurrentStatus()) &&
                    sr.getEntryNo() != null && sr.getEntryNo().length() > 0) {
                    LogbookRecord lr = logbook.getLogbookRecord(sr.getEntryNo());
                    if (lr != null) {
                        String dest = lr.getDestinationAndVariantName();
                        if (dest != null && dest.length() > 0) {
                            bs.setName(bs.getName()+ STR_DESTINATION_DELIMITER  + dest);
                        }
                    }
                }

                bsv.add(bs);
                if (!bs.isSortBySeats()) {
                    break;
                }
            }
        }

        Collections.sort(bsv);
        
        Vector<ItemTypeListData> vv = new Vector<ItemTypeListData>();
        int anz = -1;
        String lastSep = null;
        for (BoatString curBS : bsv) {
            String s = null;

            // sort by seats?
            if (sortByAnzahl) {
                switch (curBS.getSeats()) {
                    case 1:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_1);
                        break;
                    case 2:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_2);
                        break;
                    case 3:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_3);
                        break;
                    case 4:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_4);
                        break;
                    case 5:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_5);
                        break;
                    case 6:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_6);
                        break;
                    case 8:
                        s = Daten.efaTypes.getValue(EfaTypes.CATEGORY_NUMSEATS, EfaTypes.TYPE_NUMSEATS_8);
                        break;
                    default:
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
                String newSep = LIST_SECTION_STRING +" "+ s  + " " + LIST_SECTION_STRING;
                if (!newSep.equals(lastSep)) {
                    vv.add(new ItemTypeListData(newSep, null, null, null, null, true, anz));
                }
                lastSep = newSep;
            }
            BoatListItem bi=(BoatListItem) curBS.getRecord();

            vv.add(new ItemTypeListData(curBS.getName(), 
            		(buildToolTips ? buildToolTipText(curBS,todaysReservations,showReservation) : ""), 
            		getSecondaryItem(bi.boatStatus, todaysReservations, showDestination, showReservation),
            		(buildExtraInfo ? getBoatNameExtension(curBS, bi, boatListField1,boatListField2) : null),
            		bi, false, -1, null, curBS.getColors()));
        }
        return vv;
        } catch (Exception ee) {
        	Logger.logdebug(ee);
    		return null;
    	}
    }
    
    

    /**
     * Returns some additional information on the current Boatstring / Boatrecord.
     * which Boatrecord may point to a boat, but also to a person record.
     * 
     * @param bs Boat String with already some determined strings
     * @param bi the BoatListItem where we can get the boatRecord and personRecord from.
     * @return
     */
    private String getBoatNameExtension(BoatString bs, BoatListItem bi, String boatListField1, String boatListField2) {
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
    		} else if (bi.person != null){//no, must be a person record, as we only have these two options for the boat list items.

				if (boatListField1!=null && !boatListField1.isEmpty()) {
					ext1 = bi.person.getStringValueFromField(boatListField1);
				}
				if (boatListField2!=null && !boatListField2.isEmpty()) {
					ext2 = bi.person.getStringValueFromField(boatListField1);
				}
			}
					
			if ((ext1!=null && !ext1.isEmpty()) || (ext2!=null && !ext2.isEmpty())) {
				return ((ext1!=null 
						? ext1.concat((ext2!=null ? ", " + ext2 : "")) 
						: (ext2!=null ? ext2 : "")));
				} 

    		return null;
				
    	} catch (Exception e) {
    		return "mäh";
    	}
    }
    
    private String getPersonNameExtension(BoatString bs, String personListField1, String personListField2) {
    	try {

			String ext1=null;
			String ext2=null; 
			BoatListItem bi=(BoatListItem) bs.getRecord();

    		if (bi.person != null){//no, must be a person record, as we only have these two options for the boat list items.

				if (personListField1!=null && !personListField1.isEmpty()) {
					ext1 = bi.person.getStringValueFromField(personListField1);
				}
				if (personListField2!=null && !personListField2.isEmpty()) {
					ext2 = bi.person.getStringValueFromField(personListField2);
				}
			}
					
			if ((ext1!=null && !ext1.isEmpty()) || (ext2!=null && !ext2.isEmpty())) {
				return ((ext1!=null 
						? ext1.concat((ext2!=null ? ", " + ext2 : "")) 
						: (ext2!=null ? ext2 : "")));
				} 

    		return null;
				
    	} catch (Exception e) {
    		return "mäh";
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
    private String buildToolTipText(BoatString bs, Vector <BoatReservationRecord> rTodayCache, Boolean showReservation) {

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
   	    		String boatRuderErlaubnis="";
   	    		String boatReservation="";

   	    		//data if boat is on the water
   	    		String boatTimeEntry=EfaUtil.getTimeStamp(bli.boatStatus.getLastModified());
   	    		String boatComment=bli.boatStatus.getComment();
   	    		if (boatComment==null) {boatComment="";}
   	    		
   	    		// reservations only relevant if boat is available or NOT available.
   	    		// boats on the water only get destination strings.
   				if (bli.boat!=null && showReservation && (!boatStatus.equals(BoatStatusRecord.STATUS_ONTHEWATER))) {
   					boatReservation= getBoatReservationString(bli.boat.getId(), rTodayCache, EfaUtil.getRemainingMinutesToday(), true);
   					if (boatReservation==null) {boatReservation="";}
   				}
   				
   	    		if (boatStatus.equals(BoatStatusRecord.STATUS_ONTHEWATER)) {
   	        		if(bli.boat != null) {
	   	    			//boatName=bli.boat.getName();
	   	        		boatDestination=bli.boatStatus.getDestination();
	   	        		if (boatDestination==null) {boatDestination="";};
   	        		} 
   	        		
   	    		} else if (boatStatus.equals(BoatStatusRecord.STATUS_AVAILABLE)) {
   	    			boatTimeEntry="";
   	    			if (bli.boat!=null) {
   		    			boatVariant=bli.boat.getDetailedBoatType(bli.boat.getVariantIndex(bli.boatVariant));
   		    			
   		    			String groups = bli.boat.getAllowedGroupsAsNameString(System.currentTimeMillis());
   		                if (groups.length() > 0) {
   		                	boatRuderErlaubnis = (boatRuderErlaubnis.length() > 0 ? boatRuderErlaubnis + ", "
   		                            : International.getMessage("nur für {something}", groups));
   		                }
   	    			}
   	    		}

   	    		StringBuilder sbResult = new StringBuilder(100);
   	    		//concat is the fastest way to build strings
   	    		sbResult.append("<html><body><table border=\"0\"><tr ");
   	    		sbResult.append(this.cacheToolTipBgColorText);
   	    		sbResult.append("><td align=\"left\"><b>");
   	    		sbResult.append(this.cacheToolTipFontColorOpeningTag);
   	    		sbResult.append(EfaUtil.escapeHtml(boatName));
   	    		sbResult.append(this.cacheToolTipFontColorClosingTag);
   	    		sbResult.append(this.cacheToolTipFontColorOpeningTag);
   	    		sbResult.append(EfaUtil.escapeHtml(boatTimeEntry));
   	    		sbResult.append(this.cacheToolTipFontColorClosingTag);
   	    		sbResult.append("</td></tr>"); 

   	    		if (!boatReservation.isEmpty()) {
   	    			sbResult.append("<tr><td align=\"left\" colspan=2>");
   	    			sbResult.append(EfaUtil.escapeHtml(boatReservation));
   	    			sbResult.append("</td></tr>");
   	    		}
   	    		if (!boatVariant.isEmpty()) {
   	    			sbResult.append("<tr><td align=\"left\" colspan=2>");
   	    			sbResult.append(EfaUtil.escapeHtml(boatVariant));
   	    			sbResult.append("</td></tr>");
   	    		}
   	    		if (!boatRuderErlaubnis.isEmpty()) {
   	    			sbResult.append("<tr><td align=\"left\" colspan=2>");
   	    			sbResult.append(EfaUtil.escapeHtml(boatRuderErlaubnis));
   	    			sbResult.append("</td></tr>");
   	    		}
   	    		
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
   	    							.replaceAll(";", ";\n");
   		    			
    					sbResult.append("<tr><td align=\"left\" colspan=2>");
    					sbResult.append(EfaUtil.escapeHtmlWithLinefeed(boatComment));
    					sbResult.append("</td></tr>");
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
     * 
     * Damit die Methode funktioniert, muss rTodayCache aufsteigend nach dem nächsten Auftreten der Reservierung sortiert sein.
     * 
     */
    private BoatReservationRecord getCurrentOrNextUpcomingReservation(UUID boatID, Vector <BoatReservationRecord> rTodayCache) {
    	BoatReservationRecord curRes = null;
    	
    	Iterator iRes= rTodayCache.iterator();
    	while (iRes.hasNext()) {
    		curRes = (BoatReservationRecord) iRes.next();
    		if (curRes.getBoatId().equals(boatID)) {
    			//boot gefunden. wegen der aufsteigenden sortierung wissen wir:
    			//das ist die nächstmögliche Reservierung für das genannte Boot.
    			return curRes;
    		}
    	}
    	return null;
    }
    
    private String getBoatReservationString(UUID boatID, Vector <BoatReservationRecord> rTodayCache, long lookAheadMinutes, Boolean buildForTooltip) {

    	//ab hier bauen wir die Reservierungsinfo auf.
        DataTypeDate today = new DataTypeDate(System.currentTimeMillis());
        
        BoatReservationRecord res = getCurrentOrNextUpcomingReservation(boatID, rTodayCache);
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
    private String getSecondaryItem(BoatStatusRecord bs, Vector <BoatReservationRecord> rTodayCache, Boolean showDestination, Boolean showReservation) {
    	String showInList = bs.getShowInList();

    	if (showReservation && showInList.equals(BoatStatusRecord.STATUS_AVAILABLE)) {
	    		//available list: show next reservation today as secondary item
	    		return getBoatReservationString(bs.getBoatId(), rTodayCache, EfaUtil.getRemainingMinutesToday(), false);

    	} else if (showDestination && showInList.equals(BoatStatusRecord.STATUS_ONTHEWATER) ) {
    		//Boat is on the water: we show the destination as secondary item
    			return bs.getDestination();

    	} else if (showInList.equals(BoatStatusRecord.STATUS_NOTAVAILABLE)) {
    		//not available list: show "Bootsschaden" for defect boats,
    		//or the end of the current reservation
    		if (isCommentBoatDamage(bs.getComment())) {
    			return International.getString("Bootsschaden");
    			
    		} else if (showReservation && isCommentBoardReservation(bs.getComment())) {
    				return getBoatReservationString(bs.getBoatId(), rTodayCache, 0, false);
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
        
        String personListField1 = Daten.efaConfig.getValueEfaDirektBoathouseExtPersonField1();
        String personListField2 = Daten.efaConfig.getValueEfaDirektBoathouseExtPersonField2();

        Vector<ItemTypeListData> vv = new Vector<ItemTypeListData>();
        char lastChar = ' ';
        char firstChar= ' ';
        for (int i = 0; i < a.length; i++) {
            name = a[i].getName();
            if (name.length() > 0) {
            	firstChar=EfaUtil.replaceAllUmlautsLowerCaseFast(name.substring(0, 1)).charAt(0);
                if (firstChar != lastChar) {
                	lastChar = firstChar;
                    vv.add(new ItemTypeListData("---------- " + Character.toString(lastChar) .toUpperCase() + " ----------", null, null, null, null, true, SEATS_OTHER));
                }
                vv.add(new ItemTypeListData(name, 
					    (buildToolTips ? getPersonToolTip(name, (BoatString) a[i]) : ""), 
	            		(true ? getPersonNameExtension((BoatString) a[i],  personListField1, personListField2) : null),
	            		null, a[i].getRecord(),  false, SEATS_OTHER));
            }
        }
        return vv;
    }

    private String getPersonToolTip(String name, BoatString bs) {
    	
    	if (bs == null) {
    		return name;
    	} else {
    		BoatListItem bli = (BoatListItem) bs.getRecord(); 
    		
    		if (bli ==null) {
    			return name;
    		} else {
        		String alias = bli.person.getInputShortcut();
        		if ((alias != null) && (!alias.isEmpty())) {
        			return name + " ("+alias+")";
        		}
    		}
    	}
    	return name;
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
}

