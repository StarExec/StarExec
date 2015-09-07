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
	// Find min / max load in set: O(n) with a HashSet
	// This can be improved on with more complex data structures, but here n is going
	// to get the number of users simultaneously running jobs on a single queue, which
	// is always very small. Constant time overheads of more complex structures are
	// substantial, and so for now this is a very simple and fast option. If we want
	// to support very large numbers of simultaneous users, we likely want another structure
	// such as a combination HashMap / PriorityQueue structure.
	private HashMap<Integer, Long> loads = new HashMap<Integer, Long>();
	

	private Long minimum = null;
	
	private Long loadDifferenceThreshold = 10l;
	
	public LoadBalanceMonitor(long threshold) {
		loadDifferenceThreshold = threshold;
	}
	
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
	
	/**
	 * Decrements every load by the given value.
	 * @param val
	 */
	private void decrementAll(Long val) {
		for (Integer i : loads.keySet()) {
			loads.put(i, loads.get(i) - val);
		}
		if (minimum!=null) {
			minimum-=val;
		}
	}
	
	private void invalidateMin(long val) {
		if (minimum!=null && minimum==val) {
			minimum = null;
		}
	}
	
	/**
	 * Adds a new user to the map, giving them an initial load value of 0.
	 * If the user is already present, nothing is done
	 * @param userId
	 */
	public void addUser(int userId, long defaultLoad) {
		if (loads.containsKey(userId)) {
			return;
		}
		if (minimum==null || defaultLoad < minimum) {
			minimum = defaultLoad;
		}
		loads.put(userId, defaultLoad);
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
		
		// This block of code decrements all the existing load values
		// by the current minimum load value. This is done so that
		// new users (who are added next) are not given preference
		// over the users that were running jobs before.
		Long min = getMin();
		if ( min!= null && min > 0) {
			decrementAll(min);
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
	public void increaseLoad(int userId, long load) {
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
