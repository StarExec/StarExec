package org.starexec.util;

import java.util.HashMap;

import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.xy.XYDataset;

public class BenchmarkURLGenerator implements XYURLGenerator {

	private HashMap<String,Integer> seriesMap;
	public BenchmarkURLGenerator(HashMap<String,Integer> map) {
		seriesMap=map;
	}
	
	@Override
	public String generateURL(XYDataset dataset, int series, int item) {
		
		String key=series+":"+item;
		return Util.docRoot("secure/details/benchmark.jsp?id="+seriesMap.get(key));
	}

}
