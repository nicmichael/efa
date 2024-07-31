/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.gui.util;

import de.nmichael.efa.Daten;
import de.nmichael.efa.data.storage.*;
import de.nmichael.efa.data.*;
import de.nmichael.efa.util.*;
import java.util.*;

/*
 * AutoCompleteList
 * 
 * Holds the data behind ItemTypeStringAutoComplete.
 * 
 * Connects to a data access (table) which implies the data to hold.
 * AutoCompleteList elements are updated, when the dataaccess has a newer timestamp (SCN), or the efaconfig has been updated.
 *  
 * When updating AutoCompleteList, items from the database can be
 * - invalid at the current time (so they should not be shown in a dropdown list). Nonetheless, invalid items once were valid, so they can be autocompleted.
 * - marked as invisible in the db (so they should not be shown in a dropdown list)
 * - can have an alias, when it comes to persondb entries.
 * 
 * Class contains several lists
 * - dataVisible: visible items from the data access. 
 * 		items in this list are also avaliable in lower2realvisible.
 * - dataVisibleFiltered: when a filterText has been provided, this list contains all items which *contain* the filtertext case-insensitive.
 * 		ItemTypeAutoComplete and AutoCompletePopupWindow mostly work with this filtered list.
 * - lower2realVisible: lowerCase representation of dataVisible items.
 * - lower2realInvisible: items can be valid, but in the past, or they are marked as invisible. 
 *      These items should not be in the visible list, and shall only be found on special occasions. This list contains such items.    
 * - aliases2realVisible: A person can have an alias (provided in person list master data). The mapping alias<>person is contained in this list (supposing the autocompletelist contains person items).
 * - name2valid: contains the valid dates of an item which is in one of the (datavisible, dataVisibleFiltered, lower2realVisible, lower2realInvisible, aliases2realVisible) lists.
 * 
 * Major update for filtered lists
 * A request from users is that autocomplete lists should only show items in the dropdown list which contain the text of the corresponding textfield.
 * This does not comply with an autocomplete function, which always tries to match by using the text of the corresponding textfield as prefix.
 * 
 * So, to support two types of autocomplete lists, this class has been altered.
 * It supports the old-fashioned autocomplete-by-prefix mode, if no filterText has been passed.
 * If a filterText has been passed, any operation works on a filtered list which provided only items which contain the filterText (lowercase).
 * 
 * Whether an autocomplete list is filtered or not, is specified in ItemTypeStringAutoComplete, by setting or not setting a filtertext on the list.
 * When filtered, the autocomplete list also handles the lookup of aliases for persons. So if a filtertext matches an alias, the item matching the alias
 * is added to the result set (if not already in the result set). 
 * 
 */
public class AutoCompleteList {

    private class ValidInfo {
        public ValidInfo(long validFrom, long invalidFrom) {
            this.validFrom = validFrom;
            this.invalidFrom = invalidFrom;
        }
        long validFrom, invalidFrom;

        public String toString() {
            return validFrom + "-" + invalidFrom;
        }
    }

    private IDataAccess dataAccess;
    private long dataAccessSCN = -1;
    private long efaConfigSCN = -1;
    private long validFrom = -1;
    private long validUntil = Long.MAX_VALUE;
    private Vector<String> dataVisible = new Vector<String>();
    private Vector<String> dataVisibleFiltered= new Vector<String>();    
    private Vector<String> dataVisibleBackup = new Vector<String>(); // backup of dataVisible to restore removed entries
    private Hashtable<String,ValidInfo> name2valid = new Hashtable<String,ValidInfo>();
    private Hashtable<String,String> lower2realVisible = new Hashtable<String,String>();;
    private Hashtable<String,String> lower2realInvisible = new Hashtable<String,String>();;
    private Hashtable<String,String> aliases2realVisible = new Hashtable<String,String>();;
    private int pos = 0;
    private String lastPrefix;
    private long scn = 0;
    private String _searchId;
    private String _foundValue;
    private boolean filterDataOnlyForThisBoathouse = false;
    private boolean postfixNamesWithBoathouseName = true;
    private boolean filterDataOnlyOneSeaterBoats = false; //property to only have boats with one seat in this list
    private String filterText=null;

    public AutoCompleteList() {
    }

    //for debug purposes
    public synchronized String getSizes() {
    	return (dataVisible==null ? "visible: null " : " visible: "+dataVisible.size()) +  
    			(dataVisibleFiltered==null ? " dataVisibleFiltered: null " : " dataVisibleFiltered: "+dataVisibleFiltered.size());
    }
    
    public AutoCompleteList(IDataAccess dataAccess) {
        setDataAccess(dataAccess);
    }

    public AutoCompleteList(IDataAccess dataAccess, long validFrom, long validUntil) {
        setDataAccess(dataAccess, validFrom, validUntil);
    }

    public synchronized void setDataAccess(IDataAccess dataAccess) {
        setDataAccess(dataAccess, -1, -1);
    }

    public synchronized void setValidRange(long validFrom, long validUntil) {
        setDataAccess(dataAccess, validFrom, validUntil);
    }

    public synchronized void setDataAccess(IDataAccess dataAccess, long validFrom, long validUntil) {
        this.dataAccess = dataAccess;
        this.dataAccessSCN = -1;
        this.efaConfigSCN = -1;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        scn++;
    }

    public Vector<String> getDataVisible() {
        return dataVisible;
    }
    
    public Vector<String> getDataVisibleFiltered(){
    	return dataVisibleFiltered;
    }
    
    public void setDataVisible(Vector<String> dataVisible) {
        this.dataVisible = dataVisible;
        updateVisibleFilteredList();        
    }

    public void setFilterDataOnlyForThisBoathouse(boolean filterDataOnlyForThisBoathouse) {
        this.filterDataOnlyForThisBoathouse = filterDataOnlyForThisBoathouse;
    }

    public void setPostfixNamesWithBoathouseName(boolean postfixNamesWithBoathouseName) {
        this.postfixNamesWithBoathouseName = postfixNamesWithBoathouseName;
    }

    public synchronized void setFilterText(String v) {
    	if (v!=null && !v.isEmpty()) {
    		this.filterText=v.trim().toLowerCase();
    	} else {
    		this.filterText=null;
    	}
    	updateVisibleFilteredList();
    }

    private synchronized void updateVisibleFilteredList() {
    	if (filterText!=null) {
    		dataVisibleFiltered=new Vector<String>();
    		boolean easyFindEntriesWithSpecialCharacters = Daten.efaConfig.getValuePopupContainsModeEasyFindEntriesWithSpecialCharacters();
    		
    		String lowerFilterText = filterText.toLowerCase();
    		boolean bFilterTexthasSpecialCharacters = EfaUtil.containsUmlaut(lowerFilterText);
    		
    		String filterTextNoSpecialCharacters=EfaUtil.replaceAllUmlautsLowerCaseFast(lowerFilterText);
    		
    		for (int i=0; i<dataVisible.size(); i++) {

    			if (easyFindEntriesWithSpecialCharacters) {
    				if (bFilterTexthasSpecialCharacters) {
    					if (dataVisible.get(i).toLowerCase().contains(lowerFilterText)) {
    						dataVisibleFiltered.add(dataVisible.get(i));
    					}
					} else if (!bFilterTexthasSpecialCharacters){
						// no special characters in filter text -> user enters "a" but also wants 
						// matches for texts which contain "equivalents" like ä oder á
	    				if (EfaUtil.replaceAllUmlautsLowerCaseFast(dataVisible.get(i)).contains(filterTextNoSpecialCharacters)){
	    					dataVisibleFiltered.add(dataVisible.get(i));
	    				}    						
					}
    			
    			} else {
    				// no special handling for special characters needed
    				if (dataVisible.get(i).toLowerCase().contains(lowerFilterText)){
	    				dataVisibleFiltered.add(dataVisible.get(i));
	    			}
    			}
    		
    		}
    		//for entries with aliases, check wether the alias points to an entry that is not yet in the filtered list
    		if (aliases2realVisible.containsKey(filterText.toLowerCase())) {
    			String theValue=aliases2realVisible.get(filterText.toLowerCase());
    			if (!dataVisibleFiltered.contains(theValue)) {
    				dataVisibleFiltered.add(theValue);
    			}
    		}
    	} else { //no applicable filtertext, use unfiltered data
    		dataVisibleFiltered = dataVisible; 
    	}

    	sortFilteredList();
    }    
    
    /**
     * Synchronize this list with the underlying DataAccess, if necessary
     */
    public synchronized void update() {
        try {
            int myBoathouseId = -1;
            String myBoathouseName = null;
            int numberOfBoathouses = 1;
            try {
                if (dataAccess != null) {
                    myBoathouseId = dataAccess.getPersistence().getProject().getMyBoathouseId();
                    numberOfBoathouses = dataAccess.getPersistence().getProject().getNumberOfBoathouses();
                }
                if (numberOfBoathouses > 1) {
                    myBoathouseName = (dataAccess.getPersistence().getProject().getBoathouseRecord() != null
                            ? dataAccess.getPersistence().getProject().getBoathouseRecord().getName() : null);
                    if (myBoathouseName == null || myBoathouseName.length() == 0) {
                        myBoathouseName = International.getString("Bootshaus") + " " + myBoathouseId;
                    }
                }
            } catch(Exception e1) {
                Logger.logdebug(e1);
            }
            _foundValue = null;
            if (dataAccess != null &&
                dataAccess.isStorageObjectOpen() &&
                Daten.efaConfig != null && Daten.efaConfig.data() != null &&
                (dataAccess.getSCN() != dataAccessSCN ||
                 Daten.efaConfig.data().getSCN() != efaConfigSCN)) {
                dataAccessSCN = dataAccess.getSCN();
                efaConfigSCN = Daten.efaConfig.data().getSCN();
                dataVisible = new Vector<String>();
                name2valid = new Hashtable<String,ValidInfo>();
                lower2realVisible = new Hashtable<String,String>();
                lower2realInvisible = new Hashtable<String,String>();
                aliases2realVisible = new Hashtable<String,String>();
                DataKeyIterator it = dataAccess.getStaticIterator();
                ;
                for (DataKey k = it.getFirst(); k != null; k = it.getNext()) {
                    DataRecord r = dataAccess.get(k);
                    if (r != null) {
                        if (_searchId != null && r.getUniqueIdForRecord() != null && _searchId.equals(r.getUniqueIdForRecord().toString())) {
                            _foundValue = r.getQualifiedName();
                        }
                        String s = r.getQualifiedName();
                        if (r instanceof DestinationRecord) {
                            if (filterDataOnlyForThisBoathouse && numberOfBoathouses > 1 &&
                                    ((DestinationRecord)r).getOnlyInBoathouseIdAsInt() >= 0 &&
                                myBoathouseId != ((DestinationRecord)r).getOnlyInBoathouseIdAsInt()) {
                                continue;
                            }
                            if (Daten.efaConfig.getValuePrefixDestinationWithWaters()) {
                                s = ((DestinationRecord)r).getWatersNamesStringListPrefix() + s;
                            }
                            if (!postfixNamesWithBoathouseName && myBoathouseName != null) {
                                // remove postfix from qualified name
                                String dest = DestinationRecord.getDestinationNameFromPostfixedDestinationBoathouseString(s);
                                String bths = DestinationRecord.getBoathouseNameFromPostfixedDestinationBoathouseString(s);
                                if (dest != null && bths != null && myBoathouseName.equals(bths)) {
                                    s = dest;
                                }
                            }
                        }
                        String alias = null;
                        if (r instanceof PersonRecord) {
                            alias = ((PersonRecord)r).getInputShortcut();
                            if (Daten.efaConfig.getValuePostfixPersonsWithClubName()) {
                                s = s + ((PersonRecord)r).getAssociationPostfix();
                            }
                        }

                        if (filterDataOnlyOneSeaterBoats && r instanceof BoatRecord) {
                        	if (!((BoatRecord)r).isOneSeaterBoat()) {continue;}
                        }
                        
                        if (!r.getDeleted()) {
                            if (s.length() > 0) {
                                ValidInfo vi = null;
                                if (dataAccess.getMetaData().isVersionized()) {
                                    vi = new ValidInfo(r.getValidFrom(), r.getInvalidFrom());
                                }
                                add(s, alias, 
                                        r.isInValidityRange(validFrom, validUntil) && !r.getInvisible(), vi);
                            }
                        } else {
                            if (!r.getDeleted()) {
                                add(s, alias, false, null);
                            }
                        }
                    }
                }
                sort();
                dataVisibleBackup = new Vector<String>(dataVisible);
            }
        } catch (Exception e) {
        	Logger.logdebug(e);
        }
        updateVisibleFilteredList();
    }

    public synchronized String getValueForId(String id) {
        _searchId = id;
        update();
        _searchId = null;
        return _foundValue;
    }

    private synchronized void addInternal(String s, String lowers, Hashtable<String, String> lower2real,
            Vector<String> dataVisible, ValidInfo validInfo) {
        if (lower2real.get(lowers) == null) {
            // new name
            if (dataVisible != null) {
                dataVisible.add(s);
            }
            if (validInfo != null) {
                name2valid.put(s, validInfo);
            }
            lower2real.put(lowers, s);
        } else {
            // we already have this name; but it could be, that this name's validity
            // increases the validity of the name we already have, so we need to
            // check and increase it, if necessary.
            if (validInfo != null) {
                ValidInfo prevValidInfo = name2valid.get(s);
                if (prevValidInfo != null) {
                    if (validInfo.validFrom < prevValidInfo.validFrom) {
                        prevValidInfo.validFrom = validInfo.validFrom;
                    }
                    if (validInfo.invalidFrom > prevValidInfo.invalidFrom) {
                        prevValidInfo.invalidFrom = validInfo.invalidFrom;
                    }
                    name2valid.put(s, prevValidInfo);
                } else {
                    name2valid.put(s, validInfo);
                }
            }
        }
    }

    public synchronized void add(String s, String alias, boolean visibleInDropDown,
            ValidInfo validInfo) {
        if (Logger.isTraceOn(Logger.TT_GUI, 7)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_AUTOCOMPLETE,
                    "AutoCompleteList.add(" + s + "," + alias + "," + visibleInDropDown + "," + validInfo + ")");
        }
        String lowers = s.toLowerCase();
        if (visibleInDropDown) {
            if (lower2realInvisible.get(lowers) != null) {
                // if we find this name as a visible name, but have already
                // added it as an invisible name, then remove the invisible one
                lower2realInvisible.remove(lowers);
            }
            addInternal(s, lowers, lower2realVisible, dataVisible, validInfo);
            if (alias != null && alias.length() > 0) {
                aliases2realVisible.put(alias.toLowerCase(), s);
            }
        } else {
            if (lower2realVisible.get(lowers) == null) {
                // add invisble name, but only if we don't yet have it as a visible name
                addInternal(s, lowers, lower2realInvisible, null, validInfo);
            }
        }
        scn++;
    }

    public synchronized void delete(String s) {
        dataVisible.remove(s);
        name2valid.remove(s);
        lower2realVisible.remove(s.toLowerCase());
        lower2realInvisible.remove(s.toLowerCase());
        aliases2realVisible.remove(s.toLowerCase());
        scn++;
    }

    public synchronized void sort() {
    	Collections.sort(dataVisible, new EfaSortStringComparator());
    }
    
    public synchronized void sortFilteredList() {
    	Collections.sort(dataVisibleFiltered, new EfaSortStringComparator());
    }

    public synchronized String getExact(String s) {
        s = s.toLowerCase();
        if (lower2realVisible.containsKey(s)) {
            return lower2realVisible.get(s);
        } else {
            if (lower2realInvisible.containsKey(s)) {
                return lower2realInvisible.get(s);
            }
            return null;
        }
    }

    public synchronized String getNext() {
        if (pos < dataVisibleFiltered.size() - 1) {
            return dataVisibleFiltered.get(++pos);
        } else if (dataVisibleFiltered.size() == 1) {
        	return dataVisibleFiltered.get(0);
        }
        return null;
    }

    public synchronized String getPrev() {
        if (pos > 0) {
            return dataVisibleFiltered.get(--pos);
        } else if (dataVisibleFiltered.size() == 1) {
        	return dataVisibleFiltered.get(0);
        }
        return null;
    }

    public synchronized String getFirst(String prefix) {
        prefix = prefix.toLowerCase();
        lastPrefix = prefix;
        if (filterText==null) 
        {
	        for (pos = 0; pos < dataVisibleFiltered.size(); pos++) {
	            if (dataVisibleFiltered.get(pos).toLowerCase().startsWith(prefix)) {
	                return dataVisibleFiltered.get(pos);
	            }
	        }
        } else {//we already have a filtered list, get the first item no matter what the prefix is
    		if (dataVisibleFiltered.size()>0) {
        		pos=0;
        		return dataVisibleFiltered.get(pos);
        	} else {
        		return null;
        	}
        }
        return null;
    }
    
    public synchronized String getFirstByPrefix(String prefix) {
        prefix = prefix.toLowerCase();
        lastPrefix = prefix;
     
        for (pos = 0; pos < dataVisibleFiltered.size(); pos++) {
            if (dataVisibleFiltered.get(pos).toLowerCase().startsWith(prefix)) {
                return dataVisibleFiltered.get(pos);
            }
        }
        return null;
    }    

    public synchronized String getLast(String prefix) {
        prefix = prefix.toLowerCase();
        lastPrefix = prefix;

        if (filterText==null) {
	        for (pos = dataVisibleFiltered.size()-1; pos >= 0; pos--) {
	            if (dataVisibleFiltered.get(pos).toLowerCase().startsWith(prefix)) {
	                return dataVisibleFiltered.get(pos);
	            }
	        }
        } else {//we already have a filtered list, get the last item no matter what the prefix is
        	if (dataVisibleFiltered.size()>0) {
        		pos=dataVisibleFiltered.size()-1;
        		return dataVisibleFiltered.get(pos);
        	} else {
        		return null;
        	}
        }
        return null;
    }

    public synchronized String getNext(String prefix) {

    	if (filterText==null) {
	    	prefix = prefix.toLowerCase();
	        if (lastPrefix == null || !prefix.equals(lastPrefix)) {
	        	return getFirst(prefix);
	        }

	        if (pos < dataVisibleFiltered.size() - 1) {
	            String s = dataVisibleFiltered.get(++pos);
	            if (s.toLowerCase().startsWith(prefix)) {
	                return s;
	            }
	        }
    	} else {// filterText has been specified so all items match
    		if (pos < dataVisibleFiltered.size() - 1) {
	            return dataVisibleFiltered.get(++pos);
    		}
    	}

	    return null;
    }

    public synchronized String getPrev(String prefix) {
    	if (filterText==null) {
	    	prefix = prefix.toLowerCase();
	        if (lastPrefix == null || !prefix.equals(lastPrefix)) {
	            return getLast(prefix);
	        }

	        if (pos > 0) {
	            String s = dataVisibleFiltered.get(--pos);
	            if (s.toLowerCase().startsWith(prefix)) {
	                return s;
	            }
	        }
    	} else { // filterText has been specified so all items match
	        if (pos > 0) {
	            return dataVisibleFiltered.get(--pos);
	        }
    	}
        return null;
    }

    public synchronized String getAlias(String s) {
        s = s.toLowerCase();
        if (aliases2realVisible.containsKey(s)) {
            return aliases2realVisible.get(s);
        }
        return null;
    }

    public String[] getData() {
        return dataVisibleFiltered.toArray(new String[0]);
    }

    public synchronized Object getId(String qname) {
        try {
            if (dataAccess != null && dataAccess.isStorageObjectOpen()) {
                DataRecord dummyRec = dataAccess.getPersistence().createNewRecord();
                if (dummyRec instanceof DestinationRecord) {
                    if (Daten.efaConfig.getValuePrefixDestinationWithWaters()) {
                        qname = DestinationRecord.trimWatersPrefix(qname);
                    }
                }
                if (dummyRec instanceof PersonRecord) {
                    if (Daten.efaConfig.getValuePostfixPersonsWithClubName()) {
                        qname = PersonRecord.trimAssociationPostfix(qname);
                    }
                }
                DataKey[] keys = dataAccess.getByFields(dummyRec.getQualifiedNameFields(), dummyRec.getQualifiedNameValues(qname));
                if (keys == null || keys.length < 1) {
                    return null;
                }
                for (int i=0; i<keys.length; i++) {
                    DataRecord r = dataAccess.get(keys[i]);
                    if (r.isInValidityRange(validFrom, validUntil)) {
                        return r.getUniqueIdForRecord();
                    }
                }
                return null;
            }
        } catch(Exception e) {
            Logger.logdebug(e);
        }
        return null;
    }

    public synchronized boolean isValidAt(String name, long validAt) {
        ValidInfo vi = name2valid.get(name);
        if (Logger.isTraceOn(Logger.TT_GUI, 6)) {
            Logger.log(Logger.DEBUG, Logger.MSG_DEBUG_AUTOCOMPLETE, "isValidAt(" + name + "): " + vi);
        }
        if (vi == null) {
            String nameWithUpper = lower2realVisible.get(name);
            if (nameWithUpper != null) {
                vi = name2valid.get(nameWithUpper);
            }
        }
        return (vi != null && (validAt < 0 || (validAt >= vi.validFrom && validAt < vi.invalidFrom)));
    }

    public long getSCN() {
        update();
        return scn;
    }

    /**
     * Creates a vector containing all neigbours of a String. The distance
     * is measured by using EditDistance - number of keboard-hits to transform
     * name into neighbour.

     * @param name String who's neighbours are searched
     * @param radius
     * @return Vector containing neighbour strings
     * @author Thilo A. Coblenzer (original implementation)
     */
    public synchronized Vector<String> getNeighbours(String name, int radius, int maxPermutations) {
        Vector<String> neighbours = new Vector<String>();
        name = name.toLowerCase();
        Vector<String> namePerm = null;
        if (maxPermutations > 0) {
            namePerm = getPermutations(name, maxPermutations);
        }

        int lowestDist = Integer.MAX_VALUE;
        for (int i=dataVisible.size()-1; i>=0; i--) {
            String neighbour = dataVisible.get(i);
            String neighbourlc = neighbour.toLowerCase();

            int dist = EditDistance.getDistance(neighbour.toLowerCase(), name);
            if (dist <= radius) {
                if (dist < lowestDist) {
                    neighbours.add(0, neighbour);
                } else {
                    neighbours.add(neighbour);
                }
                lowestDist = dist;
            } else {
                if (namePerm != null) {
                    // check for neighbours for each of the name parts
                    Vector<String> neighbourPerm = getPermutations(neighbourlc, maxPermutations);
                    boolean found = false;
                    for (int x = 0; !found && x < namePerm.size() && x < maxPermutations; x++) {
                        for (int y = 0; !found && y < neighbourPerm.size() && y < maxPermutations; y++) {
                            dist = EditDistance.getDistance(neighbourPerm.get(y).toLowerCase(), namePerm.get(x));
                            if (dist <= radius) {
                                if (dist < lowestDist) {
                                    neighbours.add(0, neighbour);
                                } else {
                                    neighbours.add(neighbour);
                                }
                                lowestDist = dist;
                                found = true;
                            }
                        }
                    }
                }
            }
        }

        if (neighbours.size() == 0) {
            return null;
        } else {
            return neighbours;
        }
    }

    private static Vector<String> getPermutations(String s, int maxPermutations) {
        Vector<String> parts = splitString(s);
        Vector<String> perms = new Vector<String>();
        addPermutation(perms, parts, "", maxPermutations);
        return perms;
    }

    private static void addPermutation(Vector<String> perms, Vector<String> parts, String perm, int maxPermutations) {
        for (int i=0; i<parts.size(); i++) {
            if (perms.size() >= maxPermutations) {
                return;
            }
            String newPerm = (perm.length() > 0 ? perm + " " : "") + parts.get(i);
            if (parts.size() == 1) {
                perms.add(newPerm);
            } else {
                Vector<String> remainingParts = new Vector<String>(parts);
                remainingParts.remove(i);
                addPermutation(perms, remainingParts, newPerm, maxPermutations);
            }
        }
    }

    private static Vector<String> splitString(String s) {
        Vector<String> v = new Vector<String>();
        StringBuilder sb = new StringBuilder();
        for (int i=0; s != null && i<s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                sb.append(c);
            } else {
                if (sb.length() > 0) {
                    v.add(sb.toString());
                    sb = new StringBuilder();
                }
            }
        }
        if (sb.length() > 0) {
            v.add(sb.toString());
        }
        return v;
    }

    public void reset() {
        dataVisible = new Vector<String>(dataVisibleBackup);
        lastPrefix = null;
        sort();
        updateVisibleFilteredList();        
        pos = 0;
    }

    public void setFilterDataOnlyOneSeaterBoats(boolean value) {
    	this.filterDataOnlyOneSeaterBoats=value;
    }    
    
    public static void main(String[] args) {
        Vector<String> v = getPermutations("a b c", 7);
        for (int i=0; i<v.size(); i++) {
            System.out.println(v.get(i));
        }
    }
}
