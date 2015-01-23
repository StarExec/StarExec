package org.starexec.data.to.compare;

import java.util.Comparator;

import org.starexec.data.to.Solver;

public class SolverComparator implements Comparator<Solver> {
	private int column;

	public SolverComparator(int c) {
		column=c;
	}
	
	/**
	 * Compars solvers depending on the given column
	 * 0 name
	 * 1 description
	 * 2 id
	 */
	@Override
	public int compare(Solver o1, Solver o2) {
		if (column==1) {
			return o1.getDescription().compareToIgnoreCase(o2.getDescription());
		} else if (column==2) {
			return Integer.compare(o1.getId(),o2.getId());
		}
		return o1.getName().compareToIgnoreCase(o2.getName());

	}
}
