package org.starexec.data.database;

import java.awt.Color;
import java.awt.Font;
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
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.starexec.constants.R;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Status;
import org.starexec.util.BenchmarkTooltipGenerator;
import org.starexec.util.BenchmarkURLGenerator;
import org.starexec.util.Util;
import org.starexec.data.to.Space;
import org.starexec.data.to.pipelines.JoblineStage;

import com.google.gson.JsonObject;
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

    private static PieDataset createCommunityDataset(List<Space> communities, HashMap<Integer,HashMap<String,Long>> communityInfo, String type) {
        DefaultPieDataset dataset = new DefaultPieDataset();

	String name;
	int id;
	Long data;

	Double total = new Double(0);

	HashMap<String,Long> commMap = new HashMap<String,Long>();

	for(Space c : communities){
	    id = c.getId();
	    name = c.getName();
	    data = communityInfo.get(id).get(type);
	    commMap.put(name,data);

	    total = total + data;
	    
	}

	if(total.equals(0)){
	    return null;
	}
	else{
	    for(String n : commMap.keySet()){
		dataset.setValue(n,(new Double(commMap.get(n)))/total);
	    }
	    return dataset;
	}
       
    }

	/**
	 * Creates test chart
	 * @return A list of strings of size 2, where the first string is the path to the new graph
	 * and the second string is an HTML image map. Returns null on failure.
	 * @author Julio Cervantes
	 */
	
    public static JsonObject makeCommunityGraphs(List<Space> communities, HashMap<Integer,HashMap<String,Long>> communityInfo) {
		try {
				

			String[] infoTypes = {"users","solvers","benchmarks","jobs","job_pairs","disk_usage"};

			PieDataset dataset;
			JFreeChart chart;
			PiePlot plot;
			File output;
			String filename;
			List<String> filenames = new ArrayList<String>();

			Color backgroundColor =new Color(0,0,0,0); //makes the background clear
			Color titleColor = new Color(255,255,255);

			for(int i = 0; i < infoTypes.length; i++){
			    dataset = createCommunityDataset(communities,communityInfo,infoTypes[i]);
			    chart = ChartFactory.createPieChart(
									   "Comparing Communities by " + infoTypes[i],  // chart title
									   dataset,             // data
									   true,               // include legend
									   true,
									   false
									   );

			    chart.setBackgroundPaint(backgroundColor);
			    chart.getTitle().setPaint(titleColor);

			    plot = (PiePlot) chart.getPlot();
			    plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
			    plot.setNoDataMessage("No data available");
			    plot.setCircular(true);
			    plot.setLabelGap(0.02);

			    filename="community" + infoTypes[i] + "Comparison.png";
			    output = new File(new File(R.STAREXEC_ROOT, R.JOBGRAPH_FILE_DIR), filename);
			 
			    ChartUtilities.saveChartAsPNG(output, chart, 400, 400);

			    filenames.add(filename);
			}
			
			JsonObject graphs = new JsonObject();
			for(int i = 0 ; i < infoTypes.length ; i++){
			    graphs.addProperty(infoTypes[i],Util.docRoot(R.JOBGRAPH_FILE_DIR+"/"+filenames.get(i)));
			}

			 return graphs;
			

		} catch (Exception e) {
			log.error("makeTestChart says "+e.getMessage(),e);
		}
		return null;
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
	
	public static List<String> makeSolverComparisonChart(int jobId, int configId1, int configId2, int jobSpaceId, boolean large, int stageNumber) {
		try {
			List<JobPair> pairs1=Jobs.getJobPairsForSolverComparisonGraph(jobSpaceId, configId1,stageNumber);
			if ((pairs1.size())>R.MAXIMUM_DATA_POINTS ) {
				List<String> answer=new ArrayList<String>();
				answer.add("big");
				return answer;
			}
			List<JobPair> pairs2=Jobs.getJobPairsForSolverComparisonGraph(jobSpaceId,configId2,stageNumber);
			if ((pairs2.size())>R.MAXIMUM_DATA_POINTS ) {
				List<String> answer=new ArrayList<String>();
				answer.add("big");
				return answer;
			}
			return makeSolverComparisonChart(pairs1,pairs2, large,stageNumber);
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
	public static List<String> makeSolverComparisonChart(List<JobPair> pairs1, List<JobPair> pairs2, boolean large, int stageNumber) {
		try {
			
			//there are no points if either list of pairs is empty
			if (pairs1.size()==0 || pairs2.size()==0) {
				return null;
			}
			HashMap<Integer, JobPair> pairs2Map=new HashMap<Integer,JobPair>();
			for (JobPair jp : pairs2) {
				pairs2Map.put(jp.getBench().getId(), jp);
			}
			JoblineStage stage1=null;
			JoblineStage stage2=null;
			if(stageNumber<=0) {
				stage1=pairs1.get(0).getPrimaryStage();
				stage2=pairs2.get(0).getPrimaryStage();
			} else {
				stage1=pairs1.get(0).getStages().get(stageNumber-1);
				stage2=pairs2.get(0).getStages().get(stageNumber-1);
			}
			log.debug("making solver comparison chart");
			
			String xAxisName=stage1.getSolver().getName()+"/"+stage1.getConfiguration().getName()+" time(s)";
			String yAxisName=stage2.getSolver().getName()+"/"+stage2.getConfiguration().getName()+" time(s)";
			//data in these hashmaps is needed to create the image map
			HashMap<String,Integer> urls=new HashMap<String,Integer>();
			HashMap<String,String> names=new HashMap<String,String>();
			int series=0;
			int item=0;
			XYSeries d=new XYSeries("points",false);
			XYSeriesCollection dataset=new XYSeriesCollection();
			//for now, we are not including error pairs in this chart
			int debugItem=0;
			int debugSeries=0;
			for (JobPair jp : pairs1) {
				if (jp.getStatus().getCode()==Status.StatusCode.STATUS_COMPLETE) {
					JobPair jp2=pairs2Map.get(jp.getBench().getId());
					
					//if we can find a second pair with this benchmark
					if (jp2!=null && jp2.getStatus().getCode()==Status.StatusCode.STATUS_COMPLETE) {
						//points are identified by their series and item number
						String key=series+":"+item;
						
						//put the id in urls so we can link to the benchmark details page
						urls.put(key, jp.getBench().getId());
						
						//put the name in names so we can create a tooltip of the name
						//when hovering over the point in the image map
						names.put(key, jp.getBench().getName());
						item+=1;
							
						
						if(stageNumber<=0) {
							stage1=jp.getPrimaryStage();
							stage2=jp2.getPrimaryStage();
						} else {
							stage1=jp.getStages().get(stageNumber-1);
							stage2=jp2.getStages().get(stageNumber-1);
						}
						
						d.add(stage1.getWallclockTime(),stage2.getWallclockTime());
						
					}
				}
			}
			
			dataset.addSeries(d);

			String key=debugSeries+":"+debugItem;
			log.debug(urls.get(key));
			JFreeChart chart=ChartFactory.createScatterPlot("Solver Comparison Plot",xAxisName, yAxisName, dataset, PlotOrientation.VERTICAL, true, true,false);
			Color color=new Color(0,0,0,0); //makes the background clear
			chart.setBackgroundPaint(color);
			chart.getTitle().setPaint(new Color(255,255,255)); //makes the title white
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
	 * @return A String filepath to the newly created graph, or null if there was an error. Returns the string
	 * "big" if there are too many job pairs to display
	 * @author Eric Burns
	 */
	
	public static String makeSpaceOverviewChart(int jobSpaceId, boolean logX, boolean logY, List<Integer> configIds, int stageNumber) {
		try {
			if (configIds.size()==0) {
				return null;
			}
			
			List<JobPair> pairs=Jobs.getJobPairsForSolverComparisonGraph(jobSpaceId, configIds.get(0), stageNumber);
			if (pairs.size()>R.MAXIMUM_DATA_POINTS) {
				return "big";
			}
			for (int x=1;x<configIds.size();x++) {
				pairs.addAll(Jobs.getJobPairsForSolverComparisonGraph(jobSpaceId, configIds.get(x),stageNumber));
				if (pairs.size()>R.MAXIMUM_DATA_POINTS) {
					return "big";
				}
			}
			
			return makeSpaceOverviewChart(pairs, logX,logY,stageNumber);
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
	
	public static String makeSpaceOverviewChart(List<JobPair> pairs, boolean logX, boolean logY, int stageNumber) {
		try {
			log.debug("Making space overview chart with logX = "+logX +" and logY = "+logY +" and pair # = "+pairs.size());
			HashMap<Solver,HashMap<Configuration,List<Double>>> data=processJobPairData(pairs,stageNumber);
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
			chart.getTitle().setPaint(new Color(255,255,255)); //makes the title white

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
	 * Given a list of completed job pairs in a given job, arranges them by solver and configuration and sorts them
	 * by CPU run time. Used by the space overview graph
	 * @param pairs
	 * @return A HashMap for which solvers map to another HashMap mapping configurations to a sorted list of doubles
	 * representing the CPU usage of every completed, correct job pair produced by that solver/configuration pair
	 * @author Eric Burns
	 */
	
	private static HashMap<Solver,HashMap<Configuration,List<Double>>> processJobPairData(List<JobPair> pairs, int stageNumber) {
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
			//variable that will contain the single relevant stage for this pair, corresponding to stageNumber
			JoblineStage stage=null;
			if(stageNumber<=0) {
				stage=jp.getPrimaryStage();
			} else {
				stage=jp.getStages().get(stageNumber-1);
			}
			
			Solver s=stage.getSolver();
			if (!solvers.containsKey(s.getId())) {
				solvers.put(s.getId(), s);
				answer.put(solvers.get(s.getId()), new HashMap<Configuration,List<Double>>());
			}
			HashMap<Configuration,List<Double>>configMap=answer.get(solvers.get(s.getId()));
			Configuration c=stage.getConfiguration();
			if (!configs.containsKey(c.getId())) {
				configs.put(c.getId(), c);
				configMap.put(configs.get(c.getId()), new ArrayList<Double>());
			}
			configMap.get(configs.get(c.getId())).add(stage.getWallclockTime());
			
		}
		for (HashMap<Configuration,List<Double>> h : answer.values()) {
			for (List<Double> l : h.values()) {
				Collections.sort(l);
			}
		}
		
		return answer;
	}			
}
