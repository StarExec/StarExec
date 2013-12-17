package org.starexec.jobs;

import java.util.HashMap;

import org.starexec.constants.R;
import org.starexec.data.database.JobPairs;

public class ProcessingManager {
	/**
     * Checks to see which pairs need to be processed and runs the correct 
     * processors on them
     * @return
     */
    
    public synchronized static boolean checkProcessingPairs(){
    	HashMap<Integer,Integer> mapping=JobPairs.getAllPairsForProcessing();
    	int num_processed = 0;
    	
    	for (Integer pairId : mapping.keySet()) {
	    if (num_processed > R.NUM_REPOSTPROCESS_AT_A_TIME)
		break;
	    
	    Integer procId=mapping.get(pairId);
	    JobPairs.postProcessPair(pairId, procId);
	    num_processed++;
    	}
    	
    	return true;
    }
}
