package org.starexec.data.to;


public class Update {
    private int benchmarkID;
    private int processID;
    
    public Update(int benchmarkID, int processID)
    {
	this.benchmarkID = benchmarkID;
	this.processID = processID;
    }

    /**
      * @return the benchmark id that is being updated.
      */
    public int getBenchmarkID()
    {
	return benchmarkID;
    }
    
     /**
      * @param benchmarkID the benchmark id.
      */
    public void setBenchmarkID(int benchmarkID)
    {
	this.benchmarkID = benchmarkID;
    }
    

    
    /**
      * @return the benchmark id that is being updated.
      */
    public int getProcessID()
    {
	return processID;
    }
    
     /**
      * @param benchmarkID the benchmark id.
      */
    public void setProcessID(int processID)
    {
	this.processID = processID;
    }
    
    
}
