package org.starexec.util;

import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.xy.XYDataset;

import java.util.HashMap;

public class BenchmarkURLGenerator implements XYURLGenerator {

	private final HashMap<String, Integer> seriesMap;

	public BenchmarkURLGenerator(HashMap<String, Integer> map) {
		seriesMap = map;
	}

	@Override
	public String generateURL(XYDataset dataset, int series, int item) {

		String key = series + ":" + item;
		return Util.docRoot("secure/details/benchmark.jsp?id=" + seriesMap.get(key));
	}

}
