package de.nmichael.efa.data.sync;

class KanuEfbStatistics {

    private long requestCnt = 0;
	private long totalTripCnt=0;
    private long nonCanoeingTripCnt=0;
    private long unfinishedTripCnt=0;
    private long tooEarlyTripCnt=0;
    private long alreadySyncedTripCnt=0;
    private long nonSupportedBoatTypeTripCnt=0;
    private long updatedTripCnt=0;
    private long syncTripCnt=0;
    private long personWithoutEFBIDTripCnt=0;
    private long emptyBoatRecordTripCnt=0;
	
    
    public  KanuEfbStatistics (long thetotalTripCount ) {
    	totalTripCnt = thetotalTripCount;
    }
    
    public void incrementNonCanoeingTripCntIfTrue(boolean value) {
    	if (value) {nonCanoeingTripCnt++;}
    }
 
    public void incrementUnfinishedTripCntIfTrue(boolean value) {
    	if (value) {unfinishedTripCnt ++;}
    }
    
    public void incrementTooEarlyTripCntIfTrue(boolean value) {
    	if (value) {tooEarlyTripCnt++;}
    }
    
    public void incrementAlreadySyncedTripCntIfTrue(boolean value) {
    	if (value) {alreadySyncedTripCnt++;}
    }

    public void incrementKnownBoatNonSupportedBoatTypeTripCntIfTrue(boolean value) {
    	if (value) {nonSupportedBoatTypeTripCnt++;}
    }
    
    public void incrementUpdatedtripCntIfTrue(boolean value) {
    	if (value) {updatedTripCnt++;}
    }
    
    public void incrementSyncTripCntIfTrue(boolean value) {
    	if (value) {syncTripCnt++;}
    }

    public void incrementPersonWithoutEFBIDTripCntIfTrue(boolean value) {
    	if (value) {personWithoutEFBIDTripCnt++;}
    }
    
    public void incrementEmptyBoatRecordTripCntIfTrue(boolean value) {
    	if (value) {emptyBoatRecordTripCnt++;}
    }
    
    public void incrementRequestCnt() {
    	requestCnt++;
    }
    
    public long getRequestCnt() {
		return requestCnt;
	}


	public void setRequestCnt(long requestCnt) {
		this.requestCnt = requestCnt;
	}


	public long getTotalTripCnt() {
		return totalTripCnt;
	}


	public void setTotalTripCnt(long totalTripCnt) {
		this.totalTripCnt = totalTripCnt;
	}


	public long getNonCanoeingTripCnt() {
		return nonCanoeingTripCnt;
	}


	public void setNonCanoeingTripCnt(long nonCanoeingTripCnt) {
		this.nonCanoeingTripCnt = nonCanoeingTripCnt;
	}


	public long getUnfinishedTripCnt() {
		return unfinishedTripCnt;
	}


	public void setUnfinishedTripCnt(long unfinishedTripCnt) {
		this.unfinishedTripCnt = unfinishedTripCnt;
	}


	public long getTooEarlyTripCnt() {
		return tooEarlyTripCnt;
	}


	public void setTooEarlyTripCnt(long tooEarlyTripCnt) {
		this.tooEarlyTripCnt = tooEarlyTripCnt;
	}


	public long getAlreadySyncedTripCnt() {
		return alreadySyncedTripCnt;
	}


	public void setAlreadySyncedTripCnt(long alreadySyncedTripCnt) {
		this.alreadySyncedTripCnt = alreadySyncedTripCnt;
	}


	public long getNonSupportedBoatTypeTripCnt() {
		return nonSupportedBoatTypeTripCnt;
	}


	public void setNonSupportedBoatTypeTripCnt(long nonSupportedBoatTypeTripCnt) {
		this.nonSupportedBoatTypeTripCnt = nonSupportedBoatTypeTripCnt;
	}


	public long getUpdatedTripCnt() {
		return updatedTripCnt;
	}


	public void setUpdatedTripCnt(long updatedTripCnt) {
		this.updatedTripCnt = updatedTripCnt;
	}


	public long getSyncTripCnt() {
		return syncTripCnt;
	}


	public void setSyncTripCnt(long syncTripCnt) {
		this.syncTripCnt = syncTripCnt;
	}


	public long getPersonWithoutEFBIDTripCnt() {
		return personWithoutEFBIDTripCnt;
	}


	public void setPersonWithoutEFBIDTripCnt(long personWithoutEFBIDTripCnt) {
		this.personWithoutEFBIDTripCnt = personWithoutEFBIDTripCnt;
	}


	public long getEmptyBoatRecordTripCnt() {
		return emptyBoatRecordTripCnt;
	}


	public void setEmptyBoatRecordTripCnt(long emptyBoatRecordTripCnt) {
		this.emptyBoatRecordTripCnt = emptyBoatRecordTripCnt;
	}
}
