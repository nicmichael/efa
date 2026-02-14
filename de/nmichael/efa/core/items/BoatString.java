package de.nmichael.efa.core.items;

import java.awt.Color;

import de.nmichael.efa.util.EfaUtil;

public class BoatString implements Comparable {

    private String name;
    private String normName;
    private int seats;
    private boolean sortBySeats;
    private boolean sortByRigger;
    private boolean sortByType;
    private Object record;
    private int variant;
    private Color[] colors;
    private String type;
    private String rigger;
    
    private String sortString;

    public BoatString(int vseat, int vvariant, String vtype, String vrigger, String vname, 
    		boolean vsortByAnzahl, boolean vsortByRigger, boolean vsortByType) {	
    	this.setName(vname);
    	//set other values directly and calculate sort string afterwards 
    	this.seats = (vsortByAnzahl ? vseat : ItemTypeBoatstatusList.SEATS_OTHER);
    	this.variant= vvariant;
    	this.type = (vsortByType ? vtype : ItemTypeBoatstatusList.TYPE_OTHER);
    	this.rigger = (vsortByRigger ? vrigger : ItemTypeBoatstatusList.RIGGER_OTHER);
    	
    	this.setSortBySeats(vsortByAnzahl);
    	this.setSortByRigger(vsortByRigger);
    	this.setSortByType(vsortByType);

    	calcSortString();
    }
    

    public int compareTo(Object o) {
    	return sortString.compareTo (((BoatString) o).getSortString());
    }

    /**
     * Turns a string to lowercase and replaces all western european special characters with simple latin characters (like à -> a).
     * Needed for better sorting of lists.
     * @param s
     * @return
     */
    private String normalizeString(String s) {
    	if (s == null) {
            return "";
        }
        return EfaUtil.replaceAllUmlautsLowerCaseFast(s);
    }

    private void calcSortString() {
    	
        /* Old code from Comparator 
         * String sThis = (sortBySeats ? (seats < 10 ? "0" : "") + seats : "") + 
                (sortByRigger ? rigger : "") + 
                (seats == SEATS_OTHER || sortByType ? type + "#" : "") +
                (normName==null ? "": normName);//normalizeString(name);
         */
        
    	StringBuilder sbThis = new StringBuilder(getName().length());
    	
    	sbThis.append(isSortBySeats() ? (getSeats() < 10 ? "0" : "") + getSeats() : "");
    	sbThis.append(isSortByRigger() ? getRigger() : "");
    	sbThis.append(getSeats() == ItemTypeBoatstatusList.SEATS_OTHER || isSortByType() ? getType() + "#" : "");
    	sbThis.append(getNormName()==null ? "": getNormName());
    	sortString=(sbThis.toString());
    }

	public String getSortString() {
		return sortString;
	}

	public String getRigger() {
		return rigger;
	}

	public void setRigger(String rigger) {
		this.rigger = rigger;
		calcSortString();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
		calcSortString();
	}

	public Color[] getColors() {
		return colors;
	}

	public void setColors(Color[] colors) {
		this.colors = colors;
	}

	public int getVariant() {
		return variant;
	}

	public void setVariant(int variant) {
		this.variant = variant;
	}

	public Object getRecord() {
		return record;
	}

	public void setRecord(Object record) {
		this.record = record;
	}

	public boolean isSortByType() {
		return sortByType;
	}

	public void setSortByType(boolean sortByType) {
		this.sortByType = sortByType;
		calcSortString();
	}

	private boolean isSortByRigger() {
		return sortByRigger;
	}

	public void setSortByRigger(boolean sortByRigger) {
		this.sortByRigger = sortByRigger;
		calcSortString();
	}

	public boolean isSortBySeats() {
		return sortBySeats;
	}

	public void setSortBySeats(boolean sortBySeats) {
		this.sortBySeats = sortBySeats;
		calcSortString();
	}

	public int getSeats() {
		return seats;
	}

	public void setSeats(int seats) {
		this.seats = seats;
		calcSortString();
	}

	public String getNormName() {
		return normName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.normName = normalizeString(name);
	}

}
