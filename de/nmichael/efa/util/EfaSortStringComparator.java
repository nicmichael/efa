package de.nmichael.efa.util;

import java.util.Comparator;

public class EfaSortStringComparator implements Comparator {

	public int compare(Object o1, Object o2) {
        String s1 = EfaUtil.replaceAllUmlautsLowerCaseFast((String)o1);
        String s2 = EfaUtil.replaceAllUmlautsLowerCaseFast((String)o2);
        
        return s1.compareTo(s2);
    }
}

