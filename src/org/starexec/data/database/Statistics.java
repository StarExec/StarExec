package org.starexec.data.database;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.StandardToolTipTagFragmentGenerator;
import org.jfree.chart.imagemap.StandardURLTagFragmentGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.starexec.constants.R;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Status;
import org.starexec.util.BenchmarkTooltipGenerator;
import org.starexec.util.BenchmarkURLGenerator;
import org.starexec.util.Util;

import com.mysql.jdbc.ResultSetMetaData;

/**
 * Handles all statistics related database interaction
 * @author Tyler Jensen
 */
public class Statistics {
	private static final Logger log = Logger.getLogger(Jobs.class);
	
	
	/**
	 * @param con The connection to make the query on
	 * @param jobId The job to get the pair overview for
	 * @return A hashmap that contains the statistic's name to value mapping 
	 * (this method includes completePairs, pendingPairs, errorPairs, totalPairs and runtime)
	 */
	protected static HashMap<String, String> getJobPairOverview(Connection con, int jobId) throws Exception {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			 procedure = con.prepareCall("{CALL GetJobPairOverview(?)}");				
			procedure.setInt(1, jobId);		
			 results = procedure.executeQuery();		
			
			if(results.first()) {
				return Statistics.getMapFromResult(results);
			}
			
			return null;
		} catch (Exception e) {
			log.error("getJobPairOverview says "+e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}
	
	/**	 
	 * @param jobId The job to get the pair overview for
	 * @return A hashmap that contains the statistic's name to value mapping 
	 * (this method includes completePairs, pendingPairs, errorPairs, totalPairs and runtime)
	 */
	public static HashMap<String, String> getJobPairOverview(int jobId) throws Exception {
		Connection con = null;
		
		try {
			con = Common.getConnection();
			return Statistics.getJobPairOverview(con, jobId);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		
		return null;						
	}
	
	/**
	 * @param jobs The list of jobs to get overviews for (id required for each job)
	 * @return A hashmap where each key is the job ID and each value is a hashmap 
	 * that contains the statistic's name to value mapping 
	 * (includes completePairs, pendingPairs, errorPairs, totalPairs and runtime)
	 */
	public static HashMap<Integer, HashMap<String, String>> getJobPairOverviews(List<Job> jobs) {
		Connection con = null;
		
		try {
			con = Common.getConnection();
			
			// Create the return map
			HashMap<Integer, HashMap<String, String>> map = new HashMap<Integer, HashMap<String,String>>();
			
			// For each job...
			for(Job j : jobs) {
				// If it has an actual id...
				if(j.getId() > 0) {
					// Put a mapping from the job id to the pair overview for that job into the map
					map.put(j.getId(), Statistics.getJobPairOverview(con, j.getId()));
				}
			}
			
			return map;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Takes in a ResultSet with the cursor on the desired row to convert, and
	 * returns a hashmap where each key/value pair is the column name and the column
	 * value for the record the cursor is pointing to. This DOES NOT iterate through all
	 * records in the resultset
	 * @param result The ResultSet with the cursor pointing to the desired record
	 * @return A hashmap of <Column name, Column Value> pairs
	 */
	protected static HashMap<String, String> getMapFromResult(ResultSet result) throws Exception {
		// Get resultset's metadata
		ResultSetMetaData meta = (ResultSetMetaData) result.getMetaData();
		// Create the map to return
		HashMap<String, String> map = new HashMap<String, String>();
		// For each column in the record...
		for(int i = 1; i <= meta.getColumnCount(); i++) {
			// Add key=column name, value=column value to the map
			map.put(meta.getColumnName(i), result.getString(i));
		}
		return map;
	}
	
	/**
	 * Creates a chart that compares the running times of two solver/configuration pairs
	 * on benchmarks that they both completed for a given job in a given space
	 * @param jobId The job ID to look at
	 * @param configId1 The first configuration ID (which is used to get the first solver)
	 * @param configId2 The second configuration ID (which is used to get the second solver)
	 * @param spaceId The ID  of the space containing all the jobs
	 * @return A list of strings of size 2, where the first string is the path to the new graph
	 * and the second string is an HTML image map. Returns null on failure.
	 * @author Eric Burns
	 */
	
	public static List<String> makeSolverComparisonChart(int jobId, int configId1, int configId2, int jobSpaceId, boolean large) {
		try {
			List<JobPair> pairs1=Jobs.getJobPairsShallowByConfigInJobSpace(jobSpaceId, configId1,true,true);
			if ((pairs1.size())>R.MAXIMUM_DATA_POINTS ) {
				List<String> answer=new ArrayList<String>();
				answer.add("big");
				return answer;
			}
			List<JobPair> pairs2=Jobs.getJobPairsShallowByConfigInJobSpace(jobSpaceId,configId2,true,true);
			if ((pairs2.size())>R.MAXIMUM_DATA_POINTS ) {
				List<String> answer=new ArrayList<String>();
				answer.add("big");
				return answer;
			}
			return makeSolverComparisonChart(pairs1,pairs2, large);
		} catch (Exception e) {
			log.error("makeJobPairComparisonChart says "+e.getMessage(),e);
		}
		return null;
	}
	
	/**
	 * Given two lists of job pairs, each list representing a single solver/configuration pair,
	 * creates a .png image file comparing the running times of the two pairs on all benchmarks
	 * that they both solved.
	 * @param pairs1 A list of job pairs from a single solver/configuration pair
	 * @param pairs2 A list of job pairs from a single solver/configuration pair
	 * @param large Whether the graph will be the larger or smaller graph on Starexec (needs to be changed to a pixel size + an Axis color, this is a very bad abstraction)
	 * @return A size 2 List of String objects, with the first string being the path
	 * to the new graph and the second string being an HTML image map for the graph.
	 * Returns null on error
	 * @author Eric Burns
	 */
	
	@SuppressWarnings("deprecation")
	public static List<String> makeSolverComparisonChart(List<JobPair> pairs1, List<JobPair> pairs2, boolean large) {
		try {
			
			//there are no points if either list of pairs is empty
			if (pairs1.size()==0 || pairs2.size()==0) {
				return null;
			}
			log.debug("making solver comparison chart");
			
			String xAxisName=pairs1.get(0).getSolver().getName()+"/"+pairs1.get(0).getConfiguration().getName() +" time(s)";
			String yAxisName=pairs2.get(0).getSolver().getName() +"/"+pairs2.get(0).getConfiguration().getName()+" time(s)";
			HashMap<Integer,List<Double>> times=new HashMap<Integer,List<Double>>();
			
			//data in these hashmaps is needed to create the image map
			HashMap<String,Integer> urls=new HashMap<String,Integer>();
			HashMap<String,String> names=new HashMap<String,String>();
			int series=0;
			int item=0;
			//for now, we are not including error pairs in this chart
			for (JobPair jp : pairs1) {
				if (jp.getStatus().getCode()==Status.StatusCode.STATUS_COMPLETE) {
					times.put(jp.getBench().getId(), new ArrayList<Double>());
					times.get(jp.getBench().getId()).add(jp.getWallclockTime());
				}
				
			}
			for(JobPair jp : pairs2) {
				if (jp.getStatus().getCode()!=Status.StatusCode.STATUS_COMPLETE) {
					continue;
				}
				//if we haven't seen this benchmark, then it wasn't in pairs1 and
				//there is no comparison to make on it
				if (times.containsKey(jp.getBench().getId())) {
					//if, for some reason, this job included runs of the same bench with the same config,
					//we only want to use the first one we see.
					if (times.get(jp.getBench().getId()).size()<2) {
						times.get(jp.getBench().getId()).add(jp.getWallclockTime());
					}
					//points are identified by their series and item number
					String key=series+":"+item;
					
					//put the id in urls so we can link to the benchmark details page
					urls.put(key, jp.getBench().getId());
					
					//put the name in names so we can create a tooltip of the name
					//when hovering over the point in the image map
					names.put(key, jp.getBench().getName());
					item+=1;
				}
			}
			
			XYSeries d=new XYSeries("points");
			XYSeriesCollection dataset=new XYSeriesCollection();
			for(List<Double> time : times.values()) {
				if (time.size()==2) {
					d.add(time.get(0),time.get(1));
				}
			}
			dataset.addSeries(d);
			
			JFreeChart chart=ChartFactory.createScatterPlot("Solver Comparison Plot",xAxisName, yAxisName, dataset, PlotOrientation.VERTICAL, true, true,false);
			Color color=new Color(0,0,0,0); //makes the background clear
			chart.setBackgroundPaint(color);
			
			XYPlot plot = (XYPlot) chart.getPlot();
			
			//make both axes identical, and make them span from 0
			//to 110% of the maximum value
			double maxX=dataset.getDomainUpperBound(false)*1.1;
			double maxY=dataset.getRangeUpperBound(false)*1.1;
			Range range=new Range(0,Math.max(maxX, maxY));
			
			
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
			
			XYURLGenerator customURLGenerator = new BenchmarkURLGenerator(urls);
	        
	        renderer.setURLGenerator(customURLGenerator);
	        
	        XYToolTipGenerator tooltips=new BenchmarkTooltipGenerator(names);
	        renderer.setToolTipGenerator(tooltips);
	        
			LegendTitle legend=chart.getLegend();
			legend.setVisible(false);
			
			
			plot.getDomainAxis().setAutoRange(false);
			plot.getDomainAxis().setRange(range);
			plot.getRangeAxis().setAutoRange(false);
			plot.getRangeAxis().setRange(range);
			
			
			String filename=UUID.randomUUID().toString()+".png";
			File output = new File(new File(R.STAREXEC_ROOT, R.JOBGRAPH_FILE_DIR), filename);
			
			
			ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
			if (!large) {
				//we're displaying the small graph on black, so we want white axes
				plot.getDomainAxis().setTickLabelPaint(new Color(255,255,255));
				plot.getDomainAxis().setLabelPaint(new Color(255,255,255));
				plot.getRangeAxis().setTickLabelPaint(new Color(255,255,255));
				plot.getRangeAxis().setLabelPaint(new Color(255,255,255));
				ChartUtilities.saveChartAsPNG(output, chart, 300, 300,info);
			} else {
				//the large graph is getting displayed on white, so we need black axes
				plot.getDomainAxis().setTickLabelPaint(new Color(0,0,0));
				plot.getRangeAxis().setTickLabelPaint(new Color(0,0,0));
				plot.getDomainAxis().setLabelPaint(new Color(0,0,0));
				plot.getRangeAxis().setLabelPaint(new Color(0,0,0));
				ChartUtilities.saveChartAsPNG(output, chart, 800, 800,info);
			}
		
			StandardURLTagFragmentGenerator url=new StandardURLTagFragmentGenerator();
			StandardToolTipTagFragmentGenerator tag=new StandardToolTipTagFragmentGenerator();
			String map;
			if (!large) {
				map=ChartUtilities.getImageMap("solverComparisonMap", info,tag,url);
			} else {
				map=ChartUtilities.getImageMap("bigSolverComparisonMap", info,tag,url);
			}
			
			log.debug("solver comparison chart created succesfully, returning filepath ");
			List<String> answer=new ArrayList<String>();
			answer.add(Util.docRoot(R.JOBGRAPH_FILE_DIR + "/"+ filename));
			answer.add(map);
			return answer;
		} catch (Exception e) {
			log.error("makeJobPairComparisonChart says "+e.getMessage(),e);
		}
		return null;
	}
	
	/**
	 * Draws a graph comparing solvers operating in a single job in a single space, saves
	 * the chart as a png file, and returns a string containing the absolute filepath of the chart
	 * @param jobId The job id of the job to do the comparison for
	 * @param spaceId The space that should contain all of the job pairs to compare
	 * @param logX Whether to use a log scale on the X axis
	 * @param logY Whether to use a log scale on the Y axis
	 * @param configIds The IDs of the configurations that should be included in this graph
	 * @return A String filepath to the newly created graph, or null if there was an error.
	 * @author Eric Burns
	 */
	
	public static String makeSpaceOverviewChart(int jobSpaceId, boolean logX, boolean logY, List<Integer> configIds) {
		try {
			if (configIds.size()==0) {
				return null;
			}
			
			List<JobPair> pairs=Jobs.getJobPairsShallowByConfigInJobSpace(jobSpaceId, configIds.get(0), true,false);
			if (pairs.size()>R.MAXIMUM_DATA_POINTS) {
				return "big";
			}
			for (int x=1;x<configIds.size();x++) {
				pairs.addAll(Jobs.getJobPairsShallowByConfigInJobSpace(jobSpaceId, configIds.get(x), true,false));
				if (pairs.size()>R.MAXIMUM_DATA_POINTS) {
					return "big";
				}
			}
			
			return makeSpaceOverviewChart(pairs, logX,logY);
		} catch (Exception e) {
			log.error("makeSpaceOverviewChart says "+e.getMessage(),e);
		}
		
		return null;
	}
	
	/**
	 * Draws a graph comparing solvers operating on the given set of pairs
	 * @param jobId The job id of the job to do the comparison for
	 * @param spaceId The space that should contain all of the job pairs to compare
	 * @return A String filepath to the newly created graph, or null if there was an error.
	 * @author Eric Burns
	 */
	
	public static String makeSpaceOverviewChart(List<JobPair> pairs, boolean logX, boolean logY) {
		try {
			log.debug("Making space overview chart with logX = "+logX +" and logY = "+logY +" and pair # = "+pairs.size());
			HashMap<Solver,HashMap<Configuration,List<Double>>> data=processJobPairData(pairs);
			XYSeries d;
			XYSeriesCollection dataset=new XYSeriesCollection();
			for(Solver s : data.keySet()) {
				for (Configuration c : data.get(s).keySet()) {
					d=new XYSeries(s.getName()+"(" +s.getId()+ ") config = "+c.getName());
					int counter=1;
					for (Double time : data.get(s).get(c)) {
						d.add(counter,time);
						counter++;
					}
					dataset.addSeries(d);
					
				}
			}


			JFreeChart chart=ChartFactory.createScatterPlot("Space Overview Plot", "# solved", "time (s)", dataset, PlotOrientation.VERTICAL, true, true,false);
			Color color=new Color(0,0,0,0); //makes the background clear
			chart.setBackgroundPaint(color);
			
			XYPlot plot = (XYPlot) chart.getPlot();
			if (logX) {
				LogAxis xAxis=new LogAxis("# solved");
				plot.setDomainAxis(xAxis);
			}
			//logarithmic axes and manually-set ranges seem to be incompatible
			if (logY) {
				LogAxis yAxis=new LogAxis("time (s)");
				plot.setRangeAxis(yAxis);
				
			} else {
				plot.getRangeAxis().setAutoRange(false);
				plot.getRangeAxis().setRange(new Range(0,dataset.getRangeUpperBound(false)*1.1));
			}
			
			plot.getDomainAxis().setTickLabelPaint(new Color(255,255,255));
			
			plot.getRangeAxis().setTickLabelPaint(new Color(255,255,255));
			plot.getDomainAxis().setLabelPaint(new Color(255,255,255));
			plot.getRangeAxis().setLabelPaint(new Color(255,255,255));
			if (pairs.size()>10000)  {
				SamplingXYLineRenderer renderer=new SamplingXYLineRenderer();
				plot.setRenderer(renderer);
			} else {
				XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
				renderer.setSeriesLinesVisible(0, true);
				plot.setRenderer(renderer);

			}
				
			String filename=UUID.randomUUID().toString()+".png";
			


			
			File output = new File(new File(R.STAREXEC_ROOT, R.JOBGRAPH_FILE_DIR), filename);
			ChartUtilities.saveChartAsPNG(output, chart, 300, 300);

			plot.getDomainAxis().setTickLabelPaint(new Color(0,0,0));
			plot.getRangeAxis().setTickLabelPaint(new Color(0,0,0));
			plot.getDomainAxis().setLabelPaint(new Color(0,0,0));
			plot.getRangeAxis().setLabelPaint(new Color(0,0,0));
			output = new File(new File(R.STAREXEC_ROOT, R.JOBGRAPH_FILE_DIR), filename+"600");
			ChartUtilities.saveChartAsPNG(output, chart, 800, 800);

			log.debug("Chart created succesfully, returning filepath " );
			return Util.docRoot(R.JOBGRAPH_FILE_DIR+"/" + filename);
		} catch (IOException e) {
			log.error("MakeSpaceOverviewChart says "+e.getMessage(),e);
		}
		return null;
	}
	/**
	 * Given a list of completed job pairs in a given job, arranges them by solver and configuraiton and sorts them
	 * by CPU run time
	 * @param pairs
	 * @return A HashMap for which solvers map to another HashMap mapping configurations to a sorted list of doubles
	 * representing the CPU usage of every completed, correct job pair produced by that solver/configuration pair
	 * @author Eric Burns
	 */
	
	private static HashMap<Solver,HashMap<Configuration,List<Double>>> processJobPairData(List<JobPair> pairs) {
		//we need to store solvers and configs by ID and only put items into answer from these two HashMaps.
		//We can't compare to solvers to see if they are equal directly, so we have to compare IDs
		HashMap <Integer, Solver> solvers=new HashMap<Integer, Solver> ();
		HashMap <Integer,Configuration> configs=new HashMap<Integer,Configuration>();
		HashMap<Solver,HashMap<Configuration,List<Double>>> answer=new HashMap<Solver,HashMap<Configuration,List<Double>>>();
		for (JobPair jp : pairs) {
			if (jp.getStatus().getCode()!=Status.StatusCode.STATUS_COMPLETE) {
				// we don't want to consider incomplete pairs
				continue;
			}
			Solver s=jp.getSolver();
			if (!solvers.containsKey(s.getId())) {
				solvers.put(s.getId(), s);
				answer.put(solvers.get(s.getId()), new HashMap<Configuration,List<Double>>());
			}
			HashMap<Configuration,List<Double>>configMap=answer.get(solvers.get(s.getId()));
			Configuration c=jp.getConfiguration();
			if (!configs.containsKey(c.getId())) {
				configs.put(c.getId(), c);
				configMap.put(configs.get(c.getId()), new ArrayList<Double>());
			}
			configMap.get(configs.get(c.getId())).add(jp.getWallclockTime());
			
		}
		for (HashMap<Configuration,List<Double>> h : answer.values()) {
			for (List<Double> l : h.values()) {
				Collections.sort(l);
			}
		}
		
		return answer;
	}			
}
