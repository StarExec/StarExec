package org.starexec.data.to;

import java.util.ArrayList;
import org.starexec.data.database.Queues;
import org.starexec.logger.StarLogger;

/**
 * QueueGraphData is an object used in the implementation of the cluster graph displays, which can be seen on the Cluster
 * page of StarExec. It organizes data points for an individual queue so that it may be plotted by
 * PeriodicTasks.GENERATE_CLUSTER_GRAPH_TASK.
 * This implementation is based on and extends the previous implementation by Andy Swiston, which had a single cluster
 * graph for all the queues; this object aids in the implementation of individual cluster graphs.
 *
 * @author Alexander Brown
 */
public class QueueGraphData {
    /* -- fields ---------------------------------------------------------------------------------------------------- */
    private static final StarLogger log = StarLogger.getLogger(QueueGraphData.class);
    public static final int MAX_QUEUE_PLOT_POINTS = 50;
    private final int queueId;
    /**
     * sizeDataList is a list of the most recent data values of the count of enqueued jobs in the given queue
     * (the size of the queue)
     */
    private final ArrayList<Integer> sizeDataList;
    /**
     * timeDataList is a list of the most recent data values of the time at which the size of the queue is checked
     */
    private final ArrayList<Long>  timeDataList;

    /* -- constructors ---------------------------------------------------------------------------------------------- */
    /**
     * QueueGraphData() is the constuctor for the QueueGraphData class, and as such instantiates the class fields
     * @param queueId
     */
    public QueueGraphData( int queueId ) {
        this.queueId = queueId;
        this.sizeDataList = new ArrayList<Integer>();
        this.timeDataList = new ArrayList<Long>();
    }

    /* -- getters and setters --------------------------------------------------------------------------------------- */
    public int getQueueId() { return queueId; }

    /** getSizeDataListRef() returns a direct reference to sizeDataList */
    private ArrayList<Integer> getSizeDataListRef() { return sizeDataList; }

    /** getTimeDataListRef() returns a direct reference to timeDataList */
    private ArrayList<Long> getTimeDataListRef() { return timeDataList; }

    /** getSizeDataList() returns a deep copy of sizeDataList */
    public ArrayList<Integer> getSizeDataList() {
        ArrayList<Integer> deepCopy = new ArrayList<Integer>();

        for ( Integer i : sizeDataList ) {
            deepCopy.add( i );
        }

        return deepCopy;
    }

    /** getTimeDataList() returns a deep copy timeDataList */
    public ArrayList<Long> getTimeDataList() {
        ArrayList<Long> deepCopy = new ArrayList<Long>();

        for ( Long l : timeDataList ) {
            deepCopy.add( l );
        }

        return deepCopy;
    }

    /* -- other methods --------------------------------------------------------------------------------------------- */
    public String getQueueName() { return Queues.getNameById( queueId ); }

    /**
     * addNewDataPoint() accepts two data values, one for the size of the queue and one for the time the queue size was
     * polled, and adds them to their respective ArrayList objects to represent a new data point
     *
     * @param sizeValue is the size of the given queue when measured (number of enqueued job pairs)
     * @param timeValue is the time at which the queue size was measured
     */
    public void addNewDataPoint( int sizeValue, long timeValue ) {
        // add the data values to their respective ArrayList objects
        getSizeDataListRef().add( sizeValue );
        getTimeDataListRef().add( timeValue );

        // if there are more data values than the max, specified by MAX_QUEUE_PLOT_POINTS, remove the oldest data values
        if ( getSizeDataListRef().size() > MAX_QUEUE_PLOT_POINTS ) {
            getSizeDataListRef().remove( 0 );
            getTimeDataListRef().remove( 0 );
        }

        // as a safety measure, ensure the two ArrayList objects are of equal length
        if ( getSizeDataListRef().size() != getTimeDataListRef().size() ) {
            log.error( "Error: data arrays of inequal length in QueueGraphData object for " + getQueueName()
                       + ".q (" + getQueueId() + ")" );
        }
    }

}
