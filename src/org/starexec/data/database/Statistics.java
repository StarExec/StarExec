package org.starexec.data.database;

import com.google.gson.JsonObject;
import java.sql.ResultSetMetaData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.StandardToolTipTagFragmentGenerator;
import org.jfree.chart.imagemap.StandardURLTagFragmentGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.Range;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.starexec.constants.R;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.to.*;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.logger.StarLogger;
import org.starexec.util.BenchmarkTooltipGenerator;
import org.starexec.util.BenchmarkURLGenerator;
import org.starexec.util.Util;
import org.starexec.data.to.Queue;
import org.starexec.data.to.QueueGraphData;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;
import java.util.List;

/**
 * Handles all statistics related database interaction
 *
 * @author Tyler Jensen
 */
public class Statistics {
	private static final StarLogger log = StarLogger.getLogger(Statistics.class);
	/**
	 * This string is returned in place of a file path whenever there are too many pairs to render a graph.
	 */
	public static final String OVERSIZED_GRAPH_ERROR = "big";
	private static HashMap<Integer, QueueGraphData> queueGraphDataHashMap = new HashMap<Integer, QueueGraphData>();

	/**
	 * Adds a data point to the enqueued pairs dataset and generates a graph of the current dataset.
	 *
	 * I edited this heavily to provide separate graphs for each queue, but this, and the QueueGraphData class,
	 * are based on the work of Andy Swiston. -Alexander Brown 11/20
	 *
	 * @param qId is the identification number of the queue
	 */
	public static void addQueuePlotPoint( int qId ) {
		try {
			// if there is not a QueueGraphData object associated with the qId yet, create one and add it to the HashMap
			if ( !queueGraphDataHashMap.containsKey(qId) ) {
				queueGraphDataHashMap.put( qId, new QueueGraphData(qId) );
			}

			QueueGraphData currQGD = queueGraphDataHashMap.get(qId);
			String qName = currQGD.getQueueName();

			// poll for the number of enqueued job pairs and then create a new data point for QueueGraphData object
			// based on the polled value and the time of the poll
			currQGD.addNewDataPoint( Queues.getSizeOfQueue(qId), System.currentTimeMillis() );

			// create the arrays of the separate data series from the currQGD
			ArrayList<Integer> queue_size = currQGD.getSizeDataList();
			ArrayList<Long> queue_time = currQGD.getTimeDataList();

			log.debug("Started chart making for " + qName + " (" + qId + ") with " + queue_size.size() + " datapoints!");
			XYSeries series = new XYSeries("Number of Enqueued Pairs");
			for(int i = 0; i < queue_size.size(); i++) {
				series.add(queue_time.get(i), queue_size.get(i));
			}

			XYSeriesCollection dataset = new XYSeriesCollection();
			dataset.addSeries(series);

			JFreeChart chart = ChartFactory.createXYLineChart("Enqueued Pairs vs. Time\nfor "+qName+" ("+qId+")", "Time", "# of Enqueued Pairs", dataset, PlotOrientation.VERTICAL, false, false, false);
			chart.setBackgroundPaint(new Color(0, 0, 0, 0));
			chart.getTitle().setPaint(new Color(255, 255, 255));
			XYPlot plot = (XYPlot) chart.getXYPlot();

			plot.getRangeAxis().setAutoRange(false);

			int numNodes = 0;
			for(Queue q : Queues.getQueues(-2)) {
				numNodes += Queues.getNodes(q.getId()).size();
			}
			if (numNodes == 0) {
				/*if we have no nodes, R.NODE_MULTIPLIER * numNodes * 1.1 will be 0.
				This will cause an exception to be raised by setRange. To prevent it,
				we return instantly.
				*/
				return;
			}
			plot.getRangeAxis().setRange(new Range(0, R.NODE_MULTIPLIER * numNodes * 1.1));

			plot.getDomainAxis().setLabelPaint(new Color(255, 255, 255));
			plot.getRangeAxis().setTickLabelPaint(new Color(255, 255, 255));
			plot.getRangeAxis().setLabelPaint(new Color(255, 255, 255));
			plot.getDomainAxis().setTickLabelsVisible(false);

			XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
			renderer.setSeriesPaint(0, new Color(255, 0, 0));
			renderer.setSeriesShapesVisible(0, false);
			plot.setRenderer(renderer);

			File path = new File(R.STAREXEC_ROOT, R.CLUSTER_GRAPH_DIR);
			if(!path.exists()) {
				path.mkdir();
			}
			File output = new File(path, qId+"_queuegraph.png");
			ChartUtilities.saveChartAsPNG(output, chart, 400, 400);
			log.debug( "Finished chart making!: \n" + path.toString() + "/" + qId + "_queuegraph.png" );
		} catch(Exception e) {
			log.error("Error creating queue chart for "+Queues.getNameById(qId)+" ("+qId+"): " + e.getMessage());
		}
	}

	/**
 	 * @param jobId The job to get the pair times from
 	 * @return A list of end times for the job pairs
 	 */
	public static List<Date> getPairTimes(int jobId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetPairTimes(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			List<Date> list = new ArrayList<>();
			while(results.next()) {
				list.add(results.getTimestamp("end_time"));
			}
			return list;
		} catch(Exception e) {
			log.error("getPairTimes", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * @param con The connection to make the query on
	 * @param jobId The job to get the pair overview for
	 * @return A hashmap that contains the statistic's name to value mapping (this method includes completePairs,
	 * pendingPairs, errorPairs, totalPairs and runtime)
	 */
	protected static HashMap<String, String> getJobPairOverview(Connection con, int jobId) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetJobPairOverview(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();

			if (results.first()) {
				return Statistics.getMapFromResult(results);
			}

			return null;
		} catch (Exception e) {
			log.error("getJobPairOverview", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * @param jobId The job to get the pair overview for
	 * @return A hashmap that contains the statistic's name to value mapping (this method includes completePairs,
	 * pendingPairs, errorPairs, totalPairs and runtime)
	 */
	public static HashMap<String, String> getJobPairOverview(int jobId) {
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
	 * @return A hashmap where each key is the job ID and each value is a hashmap that contains the statistic's name to
	 * value mapping (includes completePairs, pendingPairs, errorPairs, totalPairs and runtime)
	 */
	public static HashMap<Integer, HashMap<String, String>> getJobPairOverviews(List<Job> jobs) {
		Connection con = null;

		try {
			con = Common.getConnection();

			// Create the return map
			HashMap<Integer, HashMap<String, String>> map = new HashMap<>();

			// For each job...
			for (Job j : jobs) {
				// If it has an actual id...
				if (j.getId() > 0) {
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
	 * Takes in a ResultSet with the cursor on the desired row to convert, and returns a hashmap where each key/value
	 * pair is the column name and the column value for the record the cursor is pointing to. This DOES NOT iterate
	 * through all records in the resultset
	 *
	 * @param result The ResultSet with the cursor pointing to the desired record
	 * @return A hashmap of <Column name, Column Value> pairs
	 */
	protected static HashMap<String, String> getMapFromResult(ResultSet result) throws Exception {
		// Get resultset's metadata
		ResultSetMetaData meta = (ResultSetMetaData) result.getMetaData();
		// Create the map to return
		HashMap<String, String> map = new HashMap<>();
		// For each column in the record...
		for (int i = 1; i <= meta.getColumnCount(); i++) {
			// Add key=column name, value=column value to the map
			map.put(meta.getColumnName(i), result.getString(i));
		}
		return map;
	}

	private static PieDataset createCommunityDataset(
			List<Space> communities, HashMap<Integer, HashMap<String, Long>> communityInfo, String type
	) {
		DefaultPieDataset dataset = new DefaultPieDataset();

		String name;
		int id;
		Long data;

		Double total = 0d;

		HashMap<String, Long> commMap = new HashMap<>();

		for (Space c : communities) {
			id = c.getId();
			name = c.getName();
			data = communityInfo.get(id).get(type);
			commMap.put(name, data);

			total = total + data;
		}

		if (total.equals(0.0)) {
			return null;
		} else {
			for (String n : commMap.keySet()) {
				dataset.setValue(n, (Double.valueOf(commMap.get(n))) / total);
			}
			return dataset;
		}
	}

	public static String makeJobTimeGraph(int jobId) {
		final String methodName = "makeJobTimeGraph";
		try {
			log.entry(methodName);
			List<Date> times = getPairTimes(jobId);
			Collections.sort(times, new Comparator<Date>() {
				@Override
				public int compare(Date d1, Date d2) {
					if(d1 == null && d2 != null) {
						return 1;
					}
					if(d1 != null && d2 == null) {
						return -1;
					}
					if(d1 == null && d2 == null) {
						return 0;
					}
					if(d1.getTime() < d2.getTime()) {
						return -1;
					} else if(d1.getTime() == d2.getTime()) {
						return 0;
					}
					return 1;
				}
			});
			
			//Times is now a list of dates ordered from earliest to latest, with all nulls at the end of the list.	
			XYSeries series = new XYSeries("Number of completed pairs");
			for(int i = 0; i < times.size(); i++) {
				Date d = times.get(i);
				if(d != null) {
					series.add(d.getTime() / 1000, i+1);

				}
			}
			
			XYSeriesCollection dataset = new XYSeriesCollection();
			dataset.addSeries(series);
			JFreeChart chart = ChartFactory.createXYLineChart("# Pairs Completed vs Time", "Time", "# of Completed Pairs", dataset, PlotOrientation.VERTICAL, false, false, false);

			chart.setBackgroundPaint(new Color(0, 0, 0, 0));
			chart.getTitle().setPaint(new Color(255, 255, 255));
			XYPlot plot = (XYPlot)chart.getXYPlot();
			plot.getRangeAxis().setAutoRange(false);
			plot.getRangeAxis().setRange(new Range(0, times.size()));
			plot.getRangeAxis().setLabelPaint(new Color(255, 255, 255));
			plot.getRangeAxis().setTickLabelPaint(new Color(255, 255, 255));
			plot.getDomainAxis().setLabelPaint(new Color(255, 255, 255));
			plot.getDomainAxis().setTickLabelsVisible(false);

			XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer();
			rend.setSeriesShapesVisible(0, false);
			plot.setRenderer(rend);

			String fileName = UUID.randomUUID().toString() + ".png";
			File output = new File(new File(R.STAREXEC_ROOT, R.CLUSTER_GRAPH_DIR), fileName);
			ChartUtilities.saveChartAsPNG(output, chart, 300, 300);
			
			plot.getRangeAxis().setLabelPaint(new Color(0, 0, 0));
                        plot.getRangeAxis().setTickLabelPaint(new Color(0, 0, 0));
                        plot.getDomainAxis().setLabelPaint(new Color(0, 0, 0));
                        plot.getDomainAxis().setTickLabelsVisible(false);

			output = new File(new File(R.STAREXEC_ROOT, R.CLUSTER_GRAPH_DIR), fileName + "800");
			ChartUtilities.saveChartAsPNG(output, chart, 800, 800);
			
			return Util.docRoot(R.CLUSTER_GRAPH_DIR + "/" + fileName);
		} catch(Exception e) {
			log.error("Error generating job pair chart for job_id=" + jobId, e);
		}
		return null;
	}


	/**
	 * Creates graphs for analyzing community statistics on Starexec
	 *
	 * @param communities The communities to get data for
	 * @param communityInfo A mapping from communities IDs to stats for those communities
	 * @return A JsonObject mapping from 'infoTypes' like users, solvers, and so on to filepaths to the graphs for
	 * those
	 * types
	 */
	public static JsonObject makeCommunityGraphs(
			List<Space> communities, HashMap<Integer, HashMap<String, Long>> communityInfo
	) {
		final String methodName = "makeCommunityGraphs";
		try {
			log.entry(methodName);


			String[] infoTypes = {"users", "solvers", "benchmarks", "jobs", "job_pairs", "disk_usage"};

			PieDataset dataset;
			JFreeChart chart;
			PiePlot plot;
			File output;
			String filename;
			List<String> filenames = new ArrayList<>();

			Color backgroundColor = new Color(0, 0, 0, 0); //makes the background clear
			Color titleColor = new Color(255, 255, 255);

			log.debug(methodName, "Creating " + infoTypes.length + " community graphs...");
			for (int i = 0; i < infoTypes.length; i++) {
				log.debug(methodName, "\tCreating graph " + i);
				dataset = createCommunityDataset(communities, communityInfo, infoTypes[i]);
				chart = ChartFactory.createPieChart("Comparing Communities by " + infoTypes[i],  // chart title
				                                    dataset,             // data
				                                    true,               // include legend
				                                    true, false
				);

				chart.setBackgroundPaint(backgroundColor);
				chart.getTitle().setPaint(titleColor);

				plot = (PiePlot) chart.getPlot();
				plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
				plot.setNoDataMessage("No data available");
				plot.setCircular(true);
				plot.setLabelGap(0.02);

				filename = "community" + infoTypes[i] + "Comparison.png";
				output = new File(new File(R.STAREXEC_ROOT, R.JOBGRAPH_FILE_DIR), filename);

				log.debug(methodName, "\tsaving chart as PNG");
				ChartUtilities.saveChartAsPNG(output, chart, 400, 400);
				log.debug(methodName, "\tsaved chart as PNG");

				filenames.add(filename);
			}
			log.debug(methodName, "Finished generating graphs.");

			JsonObject graphs = new JsonObject();
			for (int i = 0; i < infoTypes.length; i++) {
				graphs.addProperty(infoTypes[i], Util.docRoot(R.JOBGRAPH_FILE_DIR + "/" + filenames.get(i)));
			}

			return graphs;
		} catch (Exception e) {
			log.error(methodName, e);
		}
		return null;
	}

	/**
	 * Creates a chart that compares the running times of two solver/configuration pairs on benchmarks that they both
	 * completed for a given job in a given space
	 *
	 * @param configId1 The first configuration ID (which is used to get the first solver)
	 * @param configId2 The second configuration ID (which is used to get the second solver)
	 * @param edgeLengthInPixels Side length of the graph, which is square
	 * @param axisColor The color to make the axis titles and labels
	 * @param jobSpaceId The ID  of the space containing all the jobs
	 * @param stageNumber The stage to analyze for all the pairs in this graph.
	 * @return A list of strings of size 2, where the first string is the path to the new graph and the second
	 * string is
	 * an HTML image map. Returns null on failure.
	 * @author Eric Burns
	 */

	public static List<String> makeSolverComparisonChart(
			int configId1, int configId2, int jobSpaceId, int edgeLengthInPixels, Color axisColor, int stageNumber,
			PrimitivesToAnonymize primitivesToAnonymize
	) {
		final String methodName = "makeSolverComparisonChart";
		log.entry(methodName);

		try {
			List<Integer> configIds = new ArrayList<>();
			configIds.add(configId1);
			configIds.add(configId2);
			List<List<JobPair>> pairs =
					Jobs.getJobPairsForSolverComparisonGraph(jobSpaceId, configIds, stageNumber,
					                                         primitivesToAnonymize);

			List<JobPair> pairs1 = pairs.get(0);
			List<JobPair> pairs2 = pairs.get(1);

			if ((pairs1.size()) > R.MAXIMUM_DATA_POINTS || pairs2.size() > R.MAXIMUM_DATA_POINTS) {
				List<String> answer = new ArrayList<>();
				answer.add(Statistics.OVERSIZED_GRAPH_ERROR);
				return answer;
			}
			return makeSolverComparisonChart(pairs1, pairs2, jobSpaceId, edgeLengthInPixels, axisColor, stageNumber,
			                                 primitivesToAnonymize
			);
		} catch (Exception e) {
			log.error(methodName+ e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Given two lists of job pairs, each list representing a single solver/configuration pair, creates a .png image
	 * file comparing the running times of the two pairs on all benchmarks that they both solved.
	 *
	 * @param pairs1 A list of job pairs from a single solver/configuration pair
	 * @param pairs2 A list of job pairs from a single solver/configuration pair
	 * @param edgeLengthInPixels Side length of the graph, which is square
	 * @param axisColor The color to make the axis titles and labels
	 * @param stageNumber Stage number to get data for
	 * @return A size 2 List of String objects, with the first string being the path to the new graph and the second
	 * string being an HTML image map for the graph. Returns null on error
	 * @author Eric Burns
	 */
	@SuppressWarnings("deprecation")
	public static List<String> makeSolverComparisonChart(
			List<JobPair> pairs1, List<JobPair> pairs2, int jobSpaceId, int edgeLengthInPixels, Color axisColor,
			int stageNumber, PrimitivesToAnonymize primitivesToAnonymize
	) {
		try {

			//there are no points if either list of pairs is empty
			if (pairs1.isEmpty() || pairs2.isEmpty()) {
				log.debug("An input list has no jobpairs, returning null");
				return null;
			}
			HashMap<Integer, JobPair> pairs2Map = new HashMap<>();
			for (JobPair jp : pairs2) {
				pairs2Map.put(jp.getBench().getId(), jp);
			}
			JoblineStage stage1 = pairs1.get(0).getStageFromNumber(stageNumber);
			JoblineStage stage2 = pairs2.get(0).getStageFromNumber(stageNumber);

			log.debug("making solver comparison chart");

			String xAxisName = null;
			String yAxisName = null;

			/* TODO
			if ( AnonymousLinks.areSolversAnonymized( primitivesToAnonymize )) {
				int jobId = Spaces.getJobSpace( jobSpaceId ).getJobId();
				// Use anonymous solver names for the axis titles.
				Map<Integer, String> solverIdToAnonymizedName = AnonymousLinks.getAnonymizedSolverNames(jobId,
				stageNumber);
				xAxisName = solverIdToAnonymizedName.get( stage1.getSolver().getId() );
				yAxisName = solverIdToAnonymizedName.get( stage2.getSolver().getId() );
			} else {
			*/
			xAxisName = stage1.getSolver().getName() + "/" + stage1.getConfiguration().getName() + " time(s)";
			yAxisName = stage2.getSolver().getName() + "/" + stage2.getConfiguration().getName() + " time(s)";
			//}
			//data in these hashmaps is needed to create the image map
			HashMap<String, Integer> urls = new HashMap<>();
			HashMap<String, String> names = new HashMap<>();
			int series = 0;
			int item = 0;
			XYSeries d = new XYSeries("points", false);
			XYSeriesCollection dataset = new XYSeriesCollection();
			//for now, we are not including error pairs in this chart
			int debugItem = 0;
			int debugSeries = 0;
			// TODO
			//Map<Integer, String> benchmarkIdToAnonymizedName = AnonymousLinks.getAnonymizedBenchmarkNames( jobId );
			for (JobPair jp : pairs1) {
				if (jp.getStatus().getCode() == Status.StatusCode.STATUS_COMPLETE) {
					JobPair jp2 = pairs2Map.get(jp.getBench().getId());

					//if we can find a second pair with this benchmark
					if (jp2 != null && jp2.getStatus().getCode() == Status.StatusCode.STATUS_COMPLETE) {
						//points are identified by their series and item number
						String key = series + ":" + item;

						//put the id in urls so we can link to the benchmark details page
						urls.put(key, jp.getBench().getId());

						//put the name in names so we can create a tooltip of the name
						//when hovering over the point in the image map
						/* TODO
						if ( AnonymousLinks.areBenchmarksAnonymized( primitivesToAnonymize )) {
							names.put(key, benchmarkIdToAnonymizedName.get( jp.getBench().getId() ));
						} else {
						*/
						names.put(key, jp.getBench().getName());
						//}
						item += 1;

						stage1 = jp.getStageFromNumber(stageNumber);
						stage2 = jp2.getStageFromNumber(stageNumber);


						d.add(stage1.getWallclockTime(), stage2.getWallclockTime());
					}
				}
			}

			dataset.addSeries(d);

			{ // I don't know what this is trying to debug
				final String key = debugSeries + ":" + debugItem;
				final Integer value = urls.get(key);
				if (value != null) {
					log.debug(value.toString());
				}
			}

			JFreeChart chart = ChartFactory.createScatterPlot("Solver Comparison Plot", xAxisName, yAxisName, dataset,
			                                                  PlotOrientation.VERTICAL, true, true, false
			);
			Color color = new Color(0, 0, 0, 0); //makes the background clear
			chart.setBackgroundPaint(color);
			chart.getTitle().setPaint(new Color(255, 255, 255)); //makes the title white
			XYPlot plot = (XYPlot) chart.getPlot();

			//make both axes identical, and make them span from 0
			//to 110% of the maximum value
			double maxX = dataset.getDomainUpperBound(false) * 1.1;
			double maxY = dataset.getRangeUpperBound(false) * 1.1;
			Range range = new Range(0, Math.max(0.1, Math.max(maxX, maxY)));


			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

			XYURLGenerator customURLGenerator = new BenchmarkURLGenerator(urls);
			if (!AnonymousLinks.areBenchmarksAnonymized(primitivesToAnonymize)) {
				renderer.setURLGenerator(customURLGenerator);
			}

			XYToolTipGenerator tooltips = new BenchmarkTooltipGenerator(names);
			renderer.setToolTipGenerator(tooltips);

			LegendTitle legend = chart.getLegend();
			legend.setVisible(false);


			plot.getDomainAxis().setAutoRange(false);
			plot.getDomainAxis().setRange(range);
			plot.getRangeAxis().setAutoRange(false);
			plot.getRangeAxis().setRange(range);


			String filename = UUID.randomUUID().toString() + ".png";
			log.debug("The filename for the graph is: " + filename);
			File output = new File(new File(R.STAREXEC_ROOT, R.JOBGRAPH_FILE_DIR), filename);


			ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
			//we're displaying the small graph on black, so we want white axes
			plot.getDomainAxis().setTickLabelPaint(axisColor);
			plot.getDomainAxis().setLabelPaint(axisColor);
			plot.getRangeAxis().setTickLabelPaint(axisColor);
			plot.getRangeAxis().setLabelPaint(axisColor);
			ChartUtilities.saveChartAsPNG(output, chart, edgeLengthInPixels, edgeLengthInPixels, info);
			StandardURLTagFragmentGenerator url = new StandardURLTagFragmentGenerator();
			StandardToolTipTagFragmentGenerator tag = new StandardToolTipTagFragmentGenerator();
			String map;

			// Don't include the links to the benchmark pages if we're sending this to an anonymous page.
			if (AnonymousLinks.areBenchmarksAnonymized(primitivesToAnonymize)) {
				map = ChartUtilities.getImageMap("solverComparisonMap" + edgeLengthInPixels, info);
			} else {
				map = ChartUtilities.getImageMap("solverComparisonMap" + edgeLengthInPixels, info, tag, url);
			}


			log.debug("solver comparison chart created succesfully, returning filepath ");
			List<String> answer = new ArrayList<>();
			answer.add(Util.docRoot(R.JOBGRAPH_FILE_DIR + "/" + filename));
			answer.add(map);
			return answer;
		} catch (Exception e) {
			log.error("makeSolverComparisonChart", e);
		}
		return null;
	}

	/**
	 * Draws a graph comparing solvers operating in a single job in a single space, saves the chart as a png file, and
	 * returns a string containing the absolute filepath of the chart
	 *
	 * @param jobSpaceId The space that should contain all of the job pairs to compare
	 * @param logX Whether to use a log scale on the X axis
	 * @param logY Whether to use a log scale on the Y axis
	 * @param configIds The IDs of the configurations that should be included in this graph
	 * @param stageNumber the stage to analyze for all job pairs in the graph
	 * @return A String filepath to the newly created graph, or null if there was an error. Returns the string
	 * Statistics.OVERSIZED_GRAPH_ERROR if there are too many job pairs to display
	 * @author Eric Burns
	 */

	public static String makeSpaceOverviewChart(
			int jobSpaceId, boolean logX, boolean logY, List<Integer> configIds, int stageNumber,
			PrimitivesToAnonymize primitivesToAnonymize
	) {

		try {
			if (configIds.isEmpty()) {
				return null;
			}

			List<List<JobPair>> pairLists =
					Jobs.getJobPairsForSolverComparisonGraph(jobSpaceId, configIds, stageNumber,
					                                         primitivesToAnonymize);
			List<JobPair> pairs = new ArrayList<>();
			for (List<JobPair> pairList : pairLists) {
				pairs.addAll(pairList);
			}
			if (pairs.size() > R.MAXIMUM_DATA_POINTS) {
				return OVERSIZED_GRAPH_ERROR;
			}

			log.debug("Number of pairs after add all for primitivesToAnonymize=" +
					          AnonymousLinks.getPrimitivesToAnonymizeName(primitivesToAnonymize) + ": " + pairs.size
					());
			return makeSpaceOverviewChart(pairs, logX, logY, stageNumber, primitivesToAnonymize);
		} catch (Exception e) {
			log.error("makeSpaceOverviewChart", e);
		}

		return null;
	}

	/**
	 * Creates a graph for plotting the peformance of solvers against a set of job pairs
	 *
	 * @param pairs The pairs to plot results of
	 * @param logX Whether to use a log scale for the X axis
	 * @param logY Whether to use a log scale for the Y axis
	 * @param stageNumber The stage number of analyze for all the job pairs
	 * @param primitivesToAnonymize an enum describing which (if any) primitives to anonymize.
	 * @return The absolute filepath to the chart that was created
	 */
	public static String makeSpaceOverviewChart(
			List<JobPair> pairs, boolean logX, boolean logY, int stageNumber,
			PrimitivesToAnonymize primitivesToAnonymize
	) {
		try {
			log.debug("Making space overview chart with logX = " + logX + " and logY = " + logY + " and pair # = " +
					          pairs.size());
			HashMap<Solver, HashMap<Configuration, List<Double>>> data = processJobPairData(pairs, stageNumber);
			XYSeries d;
			XYSeriesCollection dataset = new XYSeriesCollection();
			for (Solver s : data.keySet()) {
				for (Configuration c : data.get(s).keySet()) {
					String label = null;
					if (AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
						label = s.getName() + " config = " + c.getName();
					} else {
						label = s.getName() + "(" + s.getId() + ") config = " + c.getName();
					}
					d = new XYSeries(label);
					int counter = 1;
					for (Double time : data.get(s).get(c)) {
						d.add(counter, time);
						counter++;
					}
					dataset.addSeries(d);
				}
			}

			log.debug("making space overview template");
			JFreeChart chart = ChartFactory
					.createScatterPlot("Space Overview Plot", "# solved", "time (s)", dataset, PlotOrientation
							                   .VERTICAL, true, true, false
					);
			Color color = new Color(0, 0, 0, 0); //makes the background clear
			chart.setBackgroundPaint(color);
			chart.getTitle().setPaint(new Color(255, 255, 255)); //makes the title white

			XYPlot plot = (XYPlot) chart.getPlot();
			if (logX) {
				LogAxis xAxis = new LogAxis("# solved");
				plot.setDomainAxis(xAxis);
			}
			//logarithmic axes and manually-set ranges seem to be incompatible
			if (logY) {
				LogAxis yAxis = new LogAxis("time (s)");
				plot.setRangeAxis(yAxis);
			} else {
				plot.getRangeAxis().setAutoRange(false);
				plot.getRangeAxis().setRange(new Range(0, Math.max(0.1, dataset.getRangeUpperBound(false) * 1.1)));
			}

			plot.getDomainAxis().setTickLabelPaint(new Color(255, 255, 255));

			plot.getRangeAxis().setTickLabelPaint(new Color(255, 255, 255));
			plot.getDomainAxis().setLabelPaint(new Color(255, 255, 255));
			plot.getRangeAxis().setLabelPaint(new Color(255, 255, 255));
			if (pairs.size() > 100) {
				SamplingXYLineRenderer renderer = new SamplingXYLineRenderer();
				plot.setRenderer(renderer);
			} else {
				XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
				renderer.setSeriesLinesVisible(0, true);
				plot.setRenderer(renderer);
			}

			String filename = UUID.randomUUID().toString() + ".png";
			File output = new File(new File(R.STAREXEC_ROOT, R.JOBGRAPH_FILE_DIR), filename);
			log.debug("saving smaller space overview chart");
			ChartUtilities.saveChartAsPNG(output, chart, 300, 300);

			plot.getDomainAxis().setTickLabelPaint(new Color(0, 0, 0));
			plot.getRangeAxis().setTickLabelPaint(new Color(0, 0, 0));
			plot.getDomainAxis().setLabelPaint(new Color(0, 0, 0));
			plot.getRangeAxis().setLabelPaint(new Color(0, 0, 0));
			output = new File(new File(R.STAREXEC_ROOT, R.JOBGRAPH_FILE_DIR), filename + "800");
			log.debug("saving larger space overview chart");
			ChartUtilities.saveChartAsPNG(output, chart, 800, 800);

			log.debug("Chart created succesfully, returning filepath ");
			return Util.docRoot(R.JOBGRAPH_FILE_DIR + "/" + filename);
		} catch (IOException e) {
			log.error("makeSpaceOverviewChart", e);
		}
		return null;
	}

	/**
	 * Given a list of completed job pairs in a given job, arranges them by solver and configuration and sorts them by
	 * CPU run time. Used by the space overview graph
	 *
	 * @param pairs
	 * @return A HashMap for which solvers map to another HashMap mapping configurations to a sorted list of doubles
	 * representing the CPU usage of every completed, correct job pair produced by that solver/configuration pair
	 * @author Eric Burns
	 */

	private static HashMap<Solver, HashMap<Configuration, List<Double>>> processJobPairData(
			List<JobPair> pairs, int stageNumber
	) {
		//we need to store solvers and configs by ID and only put items into answer from these two HashMaps.
		//We can't compare to solvers to see if they are equal directly, so we have to compare IDs
		HashMap<Integer, Solver> solvers = new HashMap<>();
		HashMap<Integer, Configuration> configs = new HashMap<>();
		HashMap<Solver, HashMap<Configuration, List<Double>>> answer = new HashMap<>();
		for (JobPair jp : pairs) {
			if (jp.getStatus().getCode() != Status.StatusCode.STATUS_COMPLETE) {
				// we don't want to consider incomplete pairs
				continue;
			}
			//variable that will contain the single relevant stage for this pair, corresponding to stageNumber
			JoblineStage stage = jp.getStageFromNumber(stageNumber);

			Solver s = stage.getSolver();
			if (!solvers.containsKey(s.getId())) {
				solvers.put(s.getId(), s);
				answer.put(solvers.get(s.getId()), new HashMap<>());
			}
			HashMap<Configuration, List<Double>> configMap = answer.get(solvers.get(s.getId()));
			Configuration c = stage.getConfiguration();
			if (!configs.containsKey(c.getId())) {
				configs.put(c.getId(), c);
				configMap.put(configs.get(c.getId()), new ArrayList<>());
			}
			configMap.get(configs.get(c.getId())).add(stage.getWallclockTime());
		}
		for (HashMap<Configuration, List<Double>> h : answer.values()) {
			for (List<Double> l : h.values()) {
				Collections.sort(l);
			}
		}

		return answer;
	}
}
