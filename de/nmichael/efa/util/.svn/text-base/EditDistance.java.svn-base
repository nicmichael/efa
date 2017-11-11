/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.util;

// @i18n complete

/**
 * Adapted from 
 * http://www.devdaily.com/java/jwarehouse/jazzy/src/com/swabunga/spell/engine/EditDistance.java.shtml
 *
 */
public class EditDistance {

	 static private int minimum(int a, int b, int c, int d, int e) {
		    int mi = a;
		    if (b < mi)
		      mi = b;
		    if (c < mi)
		      mi = c;
		    if (d < mi)
		      mi = d;
		    if (e < mi)
		      mi = e;

		    return mi;
		  }
	
	public static final int getDistance(String word, String similar) {

	    //get the weights for each possible operation
	    final int costOfDeletingSourceCharacter = 1;
	    final int costOfInsertingSourceCharacter = 1;
	    final int costOfSubstitutingLetters = 1;
	    final int costOfSwappingLetters = 1;
	    final int costOfChangingCase = 1;

	    int a_size = word.length() + 1;
	    int b_size = similar.length() + 1;
	    int[][] matrix = new int[a_size][b_size];
	    matrix[0][0] = 0;

	    for (int i = 1; i != a_size; ++i)
	      matrix[i][0] = matrix[i - 1][0] + costOfInsertingSourceCharacter; //initialize the first column

	    for (int j = 1; j != b_size; ++j)
	      matrix[0][j] = matrix[0][j - 1] + costOfDeletingSourceCharacter; //initalize the first row

	    word = " " + word;
	    similar = " " + similar;

	    for (int i = 1; i != a_size; ++i) {
	      char sourceChar = word.charAt(i);
	      for (int j = 1; j != b_size; ++j) {

	        char otherChar = similar.charAt(j);
	        if (sourceChar == otherChar) {
	          matrix[i][j] = matrix[i - 1][j - 1]; //no change required, so just carry the current cost up
	          continue;
	        }

	        int costOfSubst = costOfSubstitutingLetters + matrix[i - 1][j - 1];
	        //if needed, add up the cost of doing a swap
	        int costOfSwap = Integer.MAX_VALUE;
	        boolean isSwap = (i != 1) && (j != 1) && sourceChar == similar.charAt(j - 1) && word.charAt(i - 1) == otherChar;
	        if (isSwap)
	          costOfSwap = costOfSwappingLetters + matrix[i - 2][j - 2];

	        int costOfDelete = costOfDeletingSourceCharacter + matrix[i][j - 1];
	        int costOfInsertion = costOfInsertingSourceCharacter + matrix[i - 1][j];

	        int costOfCaseChange = Integer.MAX_VALUE;
	        String strSrcChar = "" + sourceChar;
	        String strOtherChar = "" + otherChar;

	        if (strSrcChar.compareToIgnoreCase(strOtherChar) == 0)
	          costOfCaseChange = costOfChangingCase + matrix[i - 1][j - 1];

	        matrix[i][j] = minimum(costOfSubst, costOfSwap, costOfDelete, costOfInsertion, costOfCaseChange);
	      }
	    }
	    int cost = matrix[a_size - 1][b_size - 1];

	    return cost;
	  }
	
}
