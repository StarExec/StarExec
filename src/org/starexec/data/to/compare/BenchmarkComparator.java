package org.starexec.data.to.compare;

import java.util.Comparator;

import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Solver;

public class BenchmarkComparator implements Comparator<Benchmark> {
	private int column;

	public BenchmarkComparator(int c) {
		column=c;
	}

	
	/**
	 * 0 name 
	 * 1 type
	 */
	@Override
	public int compare(Benchmark o1, Benchmark o2) {
		if (column==1) {
			return o1.getType().getName().compareTo(o2.getType().getName());
		} 
		return o1.getName().compareTo(o2.getName());
	}
	
}
