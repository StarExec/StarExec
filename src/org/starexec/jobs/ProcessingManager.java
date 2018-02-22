package org.starexec.jobs;

import org.starexec.constants.R;
import org.starexec.data.database.JobPairs;
import org.starexec.data.to.pipelines.PairStageProcessorTriple;

import java.util.List;

public class ProcessingManager {
	/**
     * Checks to see which pairs need to be processed and runs the correct 
     * processors on them
     */
    public synchronized static void checkProcessingPairs(){
    	List<PairStageProcessorTriple> triples=JobPairs.getAllPairsForProcessing();
    	int num_processed = 0;
    	
    	for (PairStageProcessorTriple triple : triples) {
    		if (num_processed > R.NUM_REPOSTPROCESS_AT_A_TIME) {
				break;

    		}
		    
		   
		    JobPairs.postProcessPair(triple.getPairId(),triple.getStageNumber(), triple.getProcessorId());
		    num_processed++;
    	}
    }
}
