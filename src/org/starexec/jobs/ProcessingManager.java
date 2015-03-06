package org.starexec.jobs;

import java.util.HashMap;
import java.util.List;

import org.starexec.constants.R;
import org.starexec.data.database.JobPairs;
import org.starexec.data.to.pipelines.PairStageProcessorTriple;

public class ProcessingManager {
	/**
     * Checks to see which pairs need to be processed and runs the correct 
     * processors on them
     * @return
     */
    
    public synchronized static boolean checkProcessingPairs(){
    	List<PairStageProcessorTriple> triples=JobPairs.getAllPairsForProcessing();
    	int num_processed = 0;
    	
    	for (PairStageProcessorTriple triple : triples) {
    		if (num_processed > R.NUM_REPOSTPROCESS_AT_A_TIME) {
				break;

    		}
	    
	   
	    JobPairs.postProcessPair(triple.getPairId(),triple.getStageId(), triple.getProcessorId());
	    num_processed++;
    	}
    	
    	return true;
    }
}
