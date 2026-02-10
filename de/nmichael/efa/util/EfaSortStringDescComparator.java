package de.nmichael.efa.util;

import java.util.Comparator;

public class EfaSortStringDescComparator implements Comparator<String> {

	public int compare(String o1, String o2) {
        String s1 = EfaUtil.replaceAllUmlautsLowerCaseFast((String)o1);
        String s2 = EfaUtil.replaceAllUmlautsLowerCaseFast((String)o2);
        //Reverse Order
        return s2.compareTo(s1);
    }

}