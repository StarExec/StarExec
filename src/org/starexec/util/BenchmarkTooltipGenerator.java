package org.starexec.util;

import java.util.HashMap;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

public class BenchmarkTooltipGenerator implements XYToolTipGenerator {
	
	private HashMap<String,String> names;
	
	public BenchmarkTooltipGenerator(HashMap<String,String> data) {
		super();
		names=data;
	}
	
	@Override
	public String generateToolTip(XYDataset dataset, int series, int item) {
		String key=series+":"+item;
		return names.get(key);
	}

}
