package org.starexec.backend;

/**
 * GridEngineR should contain constants for GridEngineBackend
 * Here to further remove the occurence of SGE ideas in Starexec code
 * 
 *
 **/
public class GridEngineR{

    // SGE Configurations, see GridEngineBackend and GridEngineUtil
    public static String QUEUE_LIST_COMMAND = "qconf -sql";					// The SGE command to execute to get a list of all job queues
    public static String QUEUE_DETAILS_COMMAND = "qconf -sq ";				// The SGE command to get configuration details about a queue
    public static String QUEUE_STATS_COMMAND = "qstat -f";				// The SGE command to get stats about all the queues
    public static String NODE_LIST_COMMAND = "qconf -sel";					// The SGE command to execute to get a list of all worker nodes
    public static String NODE_DETAILS_COMMAND = "qconf -se ";				// The SGE command to get hardware details about a node	
    public static String NODE_DETAIL_PATTERN = "[^\\s,][\\w|-]+=[^,\\s]+";  // The regular expression to parse out the key/value pairs from SGE's node detail output
    public static String QUEUE_DETAIL_PATTERN = "[\\w|-]+\\s+[^\t\r\n,]+";  // The regular expression to parse out the key/value pairs from SGE's queue detail output
    public static String QUEUE_ASSOC_PATTERN = "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,16}\\b";  // The regular expression to parse out the nodes that belong to a queue from SGE's qstat -f


}