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
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.starexec.constants.R;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.util.Util;

import com.mysql.jdbc.ResultSetMetaData;

/**
 * Handles all statistics related database interaction
 * @author Tyler Jensen
 */
public class Statistics {
	private static final Logger log = Logger.getLogger(Jobs.class);
	
	
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
	 * @param con The connection to make the query on
	 * @param jobId The job to get the pair overview for
	 * @return A hashmap that contains the statistic's name to value mapping 
	 * (this method includes completePairs, pendingPairs, errorPairs, totalPairs and runtime)
	 */
	protected static HashMap<String, String> getJobPairOverview(Connection con, int jobId) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL GetJobPairOverview(?)}");				
		procedure.setInt(1, jobId);		
		ResultSet results = procedure.executeQuery();		
		
		if(results.first()) {
			return Statistics.getMapFromResult(results);
		}
		
		return null;						
	}
	
	/**
	 * Given a list of completed job pairs in a given job, arranges them by solver and configuraiton and sorts them
	 * by CPU run time
	 * @param pairs
	 * @return A HashMap for which solvers map to another HashMap mapping configurations to a sorted list of doubles
	 * representing the CPU usage of every completed, correct job pair produced by that solver/configuration pair
	 */
	
	private static HashMap<Solver,HashMap<Configuration,List<Double>>> processJobPairData(List<JobPair> pairs) {
		//we need to store solvers and configs by ID and only put items into answer from these two HashMaps.
		//We can't compare to solvers to see if they are equal directly, so we have to compare IDs
		HashMap <Integer, Solver> solvers=new HashMap<Integer, Solver> ();
		HashMap <Integer,Configuration> configs=new HashMap<Integer,Configuration>();
		HashMap<Solver,HashMap<Configuration,List<Double>>> answer=new HashMap<Solver,HashMap<Configuration,List<Double>>>();
		for (JobPair jp : pairs) {
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
			configMap.get(configs.get(c.getId())).add(jp.getCpuUsage());
			
		}
		
		for (HashMap<Configuration,List<Double>> h : answer.values()) {
			for (List<Double> l : h.values()) {
				Collections.sort(l);
			}
		}
		
		return answer;
	}
	
	/**
	 * Draws a graph comparing solvers operating on the given set of pairs
	 * @param jobId The job id of the job to do the comparison for
	 * @param spaceId The space that should contain all of the job pairs to compare
	 * @return A String filepath to the newly created graph, or null if there was an error.
	 * @author Eric Burns
	 */
	
	public static String makeSolverComparisonChart(List<JobPair> pairs) {
		try {
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
			JFreeChart chart=ChartFactory.createScatterPlot("Solver Comparison Plot", "# solved", "time (s)", dataset, PlotOrientation.VERTICAL, true, true,false);
			Color color=new Color(0,0,0,0); //makes the background clear
			chart.setBackgroundPaint(color);
			
			XYPlot plot = (XYPlot) chart.getPlot();
			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
			renderer.setSeriesLinesVisible(0, true);
			plot.setRenderer(renderer);
			String filename=UUID.randomUUID().toString()+".png";
			File output = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), filename);
			ChartUtilities.saveChartAsPNG(output, chart, 300, 300);
			log.debug("Chart created succesfully, returning filepath ");
			return Util.docRoot("secure/files/" + filename);
		} catch (IOException e) {
			log.error("MakeSolverComparisionChart says "+e.getMessage(),e);
		}
		return null;
	}
	
	/**
	 * Draws a graph comparing solvers operating in a single job in a single space, saves
	 * the chart as a png file, and returns a string containing the absolute filepath of the chart
	 * @param jobId The job id of the job to do the comparison for
	 * @param spaceId The space that should contain all of the job pairs to compare
	 * @return A String filepath to the newly created graph, or null if there was an error.
	 * @author Eric Burns
	 */
	
	public static String makeSolverComparisonChart(int jobId, int spaceId) {
		
		try {
			List<JobPair> pairs=Jobs.getCompletedJobPairsInSpace(jobId, spaceId);
			return makeSolverComparisonChart(pairs);
		} catch (Exception e) {
			log.error("makeSolverComparisonChart says "+e.getMessage());
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
}
