package org.starexec.data.to.compare;

import org.starexec.data.to.Benchmark;

import java.util.Comparator;

public class BenchmarkComparator implements Comparator<Benchmark> {
	private final int column;
	private final boolean asc;

	public BenchmarkComparator(int c, boolean a) {
		column = c;
		asc = a;
	}

	/**
	 * 0 name 1 type
	 */
	@Override
	public int compare(Benchmark o1, Benchmark o2) {
		if (!asc) {
			Benchmark temp = o1;
			o1 = o2;
			o2 = temp;
		}
		if (column == 1) {

			return o1.getType().getName().compareToIgnoreCase(o2.getType().getName());
		}
		return o1.getName().compareToIgnoreCase(o2.getName());
	}
}
