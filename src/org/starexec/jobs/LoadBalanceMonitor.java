package org.starexec.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.starexec.constants.R;

public class LoadBalanceMonitor {


	// The basic operations we will be the following
	// Add / Find / Remove users by userIds: All O(1) with a HashSet
	// Find min load in set: O(n) with a HashSet
	// This can be improved on with more complex data structures, but here n is going
	// to get the number of users simultaneously running jobs on a single queue, which
	// is always very small. Constant time overheads of more complex structures are
	// substantial, and so for now this is a very simple and fast option. If we want
	// to support very large numbers of simultaneous users, we likely want another structure
	// such as a combination HashMap / PriorityQueue structure.
	private HashMap<Integer, Long> loads = new HashMap<Integer, Long>();

	private Long minimum = null;
	
	// ten minutes
	private Long loadDifferenceThreshold = 600l;
	
	public Long getMin() {
		if (minimum == null && loads.size()>0) {
			minimum = Collections.min(loads.values());
		}
		return minimum;
	}
	
	/**
	 * 
	 * This function is only used by testing right now, and it is likely not useful for
	 * production code, as the load values are manipulated internally by functions like
	 * setUsers.
	 * @param userId
	 * @return
	 */
	public Long getLoad(int userId) {
		return loads.get(userId);
	}
	
	
	private void invalidateMin(long val) {
		if (minimum!=null && minimum==val) {
			minimum = null;
		}
	}
	
	/**
	 * Adds a new user to the set of users being managed. The initial load
	 * for the user is the minimum + defaultLoad. Nothing is done if the user is 
	 * already present.
	 * @param userId
	 * @param defaultLoad
	 */
	public void addUser(int userId, long defaultLoad) {
		if (loads.containsKey(userId)) {
			return;
		}
		getMin();
		if (minimum == null) {
			minimum = defaultLoad;
		}
		if (loads.size()>0) {
			loads.put(userId, defaultLoad + minimum);
		} else {
			loads.put(userId, defaultLoad);
		}
	}
	
	/**
	 * Completely removes a user from the monitor.
	 * @param userId
	 */
	public void removeUser(int userId) {
		invalidateMin(loads.remove(userId));
	}
	
	/**
	 * Sets the list of users managed by this monitor to the given set
	 * of users.
	 * @param userIds
	 */
	public void setUsers(HashMap<Integer, Integer> userIdsToDefaults) {
		List<Integer> usersToRemove = new ArrayList<Integer>();
		for (Integer i : loads.keySet()) {
			if (!userIdsToDefaults.containsKey(i)) {
				// the user cannot be removed directly on this line because
				// it would cause a concurrent modification exception.
				usersToRemove.add(i);
			}
		}
		for (Integer i : usersToRemove) {
			removeUser(i);
		}
		for (Integer i : userIdsToDefaults.keySet()) {
			addUser(i, userIdsToDefaults.get(i));
		}
	}
	
	/**
	 * Increases the load associated with a given user
	 * @param userId
	 * @param load
	 */
	public void changeLoad(int userId, long load) {
		// the minimum may change only if this user has the current minimum load
		invalidateMin(loads.get(userId));
		loads.put(userId, loads.get(userId) + load);
	}
	
	/**
	 * Determines whether a given user should be skipped, meaning they should not
	 * be allowed to enqueue any more job pairs for the time being. A user is 
	 * skipped whenever their load is substantially greater than the minimum load.
	 * @param userId
	 * @return
	 */
	public boolean skipUser(int userId) {
		Long userLoad = this.getLoad(userId);
		return userLoad - getMin() > loadDifferenceThreshold;	
	}
	
}
