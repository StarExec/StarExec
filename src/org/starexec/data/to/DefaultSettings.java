package org.starexec.data.to;

import org.apache.log4j.Logger;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Solvers;
import org.starexec.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultSettings extends Identifiable {
	public enum SettingType {
		USER(0), COMMUNITY(1);
		
	    private final int value;
		private SettingType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
		
	public static SettingType toStatusCode(int code) {
			
		    switch (code) {
		    case 0:
		    	return SettingType.USER;
		    case 1:
		    	return SettingType.COMMUNITY;
		
		}
		return null;
	}

}

	private Integer primId;
	private Integer preProcessorId;
	private Integer postProcessorId;
	private Integer benchProcessorId;
	private final List<Integer> benchIds;
	private Integer solverId;
	private int wallclockTimeout;
	private int cpuTimeout;
	private long maxMemory;
	private boolean dependenciesEnabled;
	private String name;
	private SettingType type;
	/**
	 * Initializes a new DefaultSettings object with every field set to the system default.
	 */
	public DefaultSettings() {
		postProcessorId=null;
		wallclockTimeout=10;
		cpuTimeout=10;
		benchIds=new ArrayList<>();
		solverId=null;
		maxMemory=1073741824;
		preProcessorId=null;
		name="settings";
		dependenciesEnabled=false;
		benchProcessorId=Processors.getNoTypeProcessor().getId();
		type=null;
		setPrimId(-1);
	}

	public static void DefaultSettings(DefaultSettings settingsToCopy) {

	}

	private DefaultSettings(
			final Integer primId,
			final Integer preProcessorId,
			final Integer postProcessorId,
			final Integer benchProcessorId,
			final List<Integer> benchIds,
			final Integer solverId,
			final int wallclockTimeout,
			final int cpuTimeout,
			final long maxMemory,
			final boolean dependenciesEnabled,
			final String name,
			final SettingType type
	) {
		this.primId=primId;
		this.preProcessorId=preProcessorId;
		this.postProcessorId=postProcessorId;
		this.benchProcessorId=benchProcessorId;
		this.benchIds=new ArrayList<>(benchIds);
		this.solverId=solverId;
		this.wallclockTimeout=wallclockTimeout;
		this.cpuTimeout=cpuTimeout;
		this.maxMemory=maxMemory;
		this.dependenciesEnabled=dependenciesEnabled;
		this.name=name;
		this.type=type;
	}

	public void setPreProcessorId(Integer preProcessorId) {
		this.preProcessorId = preProcessorId;
	}
	public Integer getPreProcessorId() {
		return preProcessorId;
	}
	public void setPostProcessorId(Integer postProcessorId) {
		this.postProcessorId = postProcessorId;
	}
	public Integer getPostProcessorId() {
		return postProcessorId;
	}
	public void setBenchProcessorId(Integer benchProcessorId) {
		this.benchProcessorId = benchProcessorId;
	}
	public Integer getBenchProcessorId() {
		return benchProcessorId;
	}
	public void addBenchId(Integer benchId) {
		benchIds.add(benchId);
	}
	public List<Integer> getBenchIds() {
		return this.benchIds;
	}
	public void setSolverId(Integer solverId) {
		this.solverId = solverId;
	}
	public Integer getSolverId() {
		return solverId;
	}
	public void setWallclockTimeout(int wallclockTimeout) {
		this.wallclockTimeout = wallclockTimeout;
	}
	public int getWallclockTimeout() {
		return wallclockTimeout;
	}
	public void setCpuTimeout(int cpuTimeout) {
		this.cpuTimeout = cpuTimeout;
	}
	public int getCpuTimeout() {
		return cpuTimeout;
	}
	public void setMaxMemory(long mem) {
		this.maxMemory = mem;
	}
	public long getMaxMemory() {
		return this.maxMemory;
	}
	public double getRoundedMaxMemoryAsDouble() {
		return Util.bytesToGigabytes(maxMemory);
	}
	public void setDependenciesEnabled(boolean dependenciesEnabled) {
		this.dependenciesEnabled = dependenciesEnabled;
	}
	public boolean isDependenciesEnabled() {
		return dependenciesEnabled;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		
		return name;
	}

	
	public String getSolverName() {
		if (solverId==null) {
			return "None";
		}
		Solver s=Solvers.get(solverId);
		if (s==null) {
			return "None";
		}
		return s.getName();
	}
	
	public String getBenchmarkName(Integer index) {
		Integer benchId = benchIds.get(index);
		if (benchId==null) {
			return "None";
		}
		Benchmark b=Benchmarks.get(benchId);
		if (b==null) {
			return "None";
		}
		return b.getName();
		
	}
	public void setPrimId(Integer primId) {
		this.primId = primId;
	}
	public Integer getPrimId() {
		return primId;
	}
	public void setType(SettingType type) {
		this.type = type;
	}
	public void setType(int type) {
		this.type = SettingType.toStatusCode(type);
	}
	public SettingType getType() {
		return type;
	}
	public String getTypeString() {
		return type.name();
	}
	
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append(this.getId());
		sb.append(" | ");
		sb.append(this.getName());
		sb.append(" | ");
		sb.append(this.getPrimId());
		sb.append(" | ");
		sb.append(this.getPreProcessorId());
		sb.append(" | ");
		sb.append(this.getPostProcessorId());
		sb.append(" | ");
		sb.append(this.getBenchProcessorId());
		sb.append(" | ");
		sb.append(this.getSolverId());
		sb.append(" | ");
		for(int i = 0; i < benchIds.size(); i++) {
			sb.append(benchIds.get(i));
			if (i != benchIds.size() - 1) {
				sb.append(", ");
			}
		}
		sb.append(" | ");
		sb.append(this.getCpuTimeout());
		sb.append(" | ");
		sb.append(this.getWallclockTimeout());
		sb.append(" | ");
		sb.append(this.getMaxMemory());
		return sb.toString();
	}
	
	/**
	 * Checks for deep equality between this object and another DefaultSettings profile
	 */

	@Override
	public boolean equals(Object s) {
		if (!(s instanceof DefaultSettings)) {
            return false;
		}
		DefaultSettings set=(DefaultSettings) s;

		List<Integer> thisBenchIds = this.getBenchIds();
		List<Integer> setBenchIds = set.getBenchIds();
		if (this.getBenchIds().size() != set.getBenchIds().size()) {
			return false;
		}

		Collections.sort(thisBenchIds);
		Collections.sort(setBenchIds);

		// If we make it past this loop we've established that the default bench ids are the same.
		for (int i = 0; i < thisBenchIds.size(); i++) {
			if (thisBenchIds.get(i) != setBenchIds.get(i)) {
				return false;
			}
		}



		return (this.getId()==set.getId() &&
				Util.objectsEqual(this.getName(), set.getName()) &&
				Util.objectsEqual(this.getPrimId(),set.getPrimId()) &&
				Util.objectsEqual(this.getPreProcessorId(),set.getPreProcessorId()) &&
				Util.objectsEqual(this.getBenchProcessorId(),set.getBenchProcessorId()) &&
				Util.objectsEqual(this.getPostProcessorId(),set.getPostProcessorId()) &&
				this.getCpuTimeout()==set.getCpuTimeout() &&
				this.getWallclockTimeout()==set.getWallclockTimeout()&&
				this.getMaxMemory()==set.getMaxMemory() &&
				Util.objectsEqual(this.getSolverId(),set.getSolverId()));
	}
}
