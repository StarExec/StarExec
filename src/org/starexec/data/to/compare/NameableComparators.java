package org.starexec.data.to.compare;

import org.starexec.data.to.Nameable;

import java.util.Comparator;

/**
 * Class that builds comparators for Nameable objects.
 * @author Albert Giegerich
 */
public class NameableComparators {

	/**
	 * Builds a comparator for Nameable objects that compares two objects alphabetically
	 * insensitive to case.
	 * @return The new Comparator
	 * @author Albert Giegerich
	 */
	public static Comparator<Nameable> getCaseInsensitiveAlphabeticalComparator() {
		return (a, b) -> {
            String aName = a.getName();
            String bName = b.getName();
            return aName.compareToIgnoreCase(bName);
        };
	}

}

