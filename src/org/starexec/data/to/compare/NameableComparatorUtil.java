package org.starexec.data.to.compare;

import java.util.Comparator;
import org.starexec.data.to.Nameable;

/**
 * Class that builds comparators for Nameable objects.
 * @author Albert Giegerich
 */
public class NameableComparatorUtil {

	/**
	 * Builds a comparator for Nameable objects that compares two objects alphabetically
	 * insensitive to case.
	 * @return The new Comparator
	 * @author Albert Giegerich
	 */
	public static Comparator<Nameable> getCaseInsensitiveAlphabeticalComparator() {
		return new Comparator<Nameable>() {
			public int compare(Nameable a, Nameable b) {
				String aName = a.getName();
				String bName = b.getName();
				return aName.compareToIgnoreCase(bName);
			}
		};
	}

}

