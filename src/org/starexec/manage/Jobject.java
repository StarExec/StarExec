package org.starexec.manage;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Holds the flat list of JobTuples that make up a StarExec job. Each job is associated with a particular user.
 * @author Clifton Palmer
 *
 */
public class Jobject implements Iterable<JobTuple>{
	private long userId = -1;
	private ArrayList<JobTuple> tuples;
	
	public Jobject(long userId) {
		tuples = new ArrayList<JobTuple>();
		this.userId = userId;
	}
	
	public void addTuple(JobTuple jt) {
		tuples.add(jt);
	}

	@Override
	public Iterator<JobTuple> iterator() {
		return tuples.iterator();
	}

	public long getUserId() {
		return userId;
	}
}
