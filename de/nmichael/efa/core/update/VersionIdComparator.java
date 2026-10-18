package de.nmichael.efa.core.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/** 
 * Comparator for version strings in the format "major.minor.patch[_suffix]".
 * The suffix can be an underscore followed by a number or an underscore followed by a hash.
 * Versions are compared first by their core parts (major, minor, patch) and then by suffix type and number.
 */
public class VersionIdComparator {

    private static enum SuffixType { UNDERSCORE_NUMBER, NONE, UNDERSCORE_HASH }

    /**
	 * Represents a parsed version with core parts, suffix type, and suffix number.
	 */
    private static class Version {
        int[] core;
        SuffixType type;
        int suffixNumber;

        Version(int[] core, SuffixType type, int suffixNumber) {
            this.core = core;
            this.type = type;
            this.suffixNumber = suffixNumber;
        }
    }

    /**
	 * Checks if the candidate version is newer than the current version.
	 * @param candidate The candidate version string.
	 * @param current The current version string.
	 * @return true if the candidate is newer than the current, false otherwise.
	 */
    public static boolean isNewer(String candidate, String current) {
        return compare(candidate, current) < 0;
    }
    
    /**
	 * Compares two version strings.
	 *  @param a The first version string.
	 *  @param b The second version string.
	 *  @return A negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 *  */
    public static int compare(String a, String b) {
        Version va = parse(a);
        Version vb = parse(b);

        // compare core parts lexicographically (longer cores considered with trailing zeros)
        int maxLen = Math.max(va.core.length, vb.core.length);
        for (int i = 0; i < maxLen; i++) {
            int ca = i < va.core.length ? va.core[i] : 0;
            int cb = i < vb.core.length ? vb.core[i] : 0;
            if (ca != cb) {
                return Integer.compare(cb, ca); // descending
            }
        }

        // same core -> compare suffix type by priority: UNDERSCORE_NUMBER > NONE > UNDERSCORE_HASH
        int typeRankA = rank(va.type);
        int typeRankB = rank(vb.type);
        if (typeRankA != typeRankB) {
            return Integer.compare(typeRankB, typeRankA); // descending by rank
        }

        // same type -> compare suffix numbers (descending)
        return Integer.compare(vb.suffixNumber, va.suffixNumber);
    }

    /**
	 * Returns the rank of the suffix type for comparison.
	 * @param t The suffix type.
	 * @return An integer representing the rank of the suffix type.
	 * */
    private static int rank(SuffixType t) {
        switch (t) {
            case UNDERSCORE_NUMBER: return 3;
            case NONE: return 2;
            case UNDERSCORE_HASH: return 1;
            default: return 0;
        }
    }

    /**
	 * Parses a version string into its core parts, suffix type, and suffix number.
	 * @param s The version string to parse.
	 * @return A Version object representing the parsed version.
	 * */
    private static Version parse(String s) {
        // split core and suffix at first underscore
        String corePart = s;
        String suffixPart = null;
        int idx = s.indexOf('_');
        if (idx >= 0) {
            corePart = s.substring(0, idx);
            suffixPart = s.substring(idx + 1);
        }

        // parse core numbers
        String[] coreTokens = corePart.split("\\.");
        int[] core = new int[coreTokens.length];
        for (int i = 0; i < coreTokens.length; i++) {
            try {
                core[i] = Integer.parseInt(coreTokens[i]);
            } catch (NumberFormatException e) {
                core[i] = 0;
            }
        }

        // parse suffix
        if (suffixPart == null || suffixPart.isEmpty()) {
            return new Version(core, SuffixType.NONE, 0);
        } else {
            if (suffixPart.startsWith("#")) {
                String num = suffixPart.substring(1);
                int n = parseIntSafe(num);
                return new Version(core, SuffixType.UNDERSCORE_HASH, n);
            } else {
                // treat as plain digits (may have leading zeros)
                int n = parseIntSafe(suffixPart);
                return new Version(core, SuffixType.UNDERSCORE_NUMBER, n);
            }
        }
    }

    /**
	 * Safely parses an integer from a string, extracting digits if necessary.
	 * @param s The string to parse.
	 * @return The parsed integer, or 0 if parsing fails.
	 * */
    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            // fallback: extract digits
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (Character.isDigit(c)) sb.append(c);
            }
            if (sb.length() == 0) return 0;
            try {
                return Integer.parseInt(sb.toString());
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
    }

    // Beispiel / Test
    public static void main(String[] args) {
        List<String> versions = new ArrayList<>(Arrays.asList(
            "2.5.3_10",
            "2.5.3_03",
            "2.5.3_2",
            "2.5.3_01",
            "2.5.3_#10",
            "2.5.3_#9",
            "2.5.3_#2",
            "2.5.3_#1",
            "2.5.2_00",
            "2.5.1_05",
            "2.5.1",
            "2.5.1_#5",
            "2.5.1_#4",
            "2.5.0_01",
            "2.5.0",
            "2.4.0_01",
            "2.4.0_00",
            "2.4.0"
        ));

        // shuffle to demonstrate sorting (optional)
        Collections.shuffle(versions);

        versions.sort(VersionIdComparator::compare);
        //Collections.sort(versions, new VersionIdComparator());

        for (String v : versions) {
            System.out.println(v);
        }
    }
}
