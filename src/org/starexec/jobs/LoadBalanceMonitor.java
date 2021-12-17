package org.starexec.jobs;

import org.starexec.data.database.Users;
import org.starexec.data.to.User;
import org.starexec.logger.StarLogger;
import org.starexec.constants.R;

import java.util.*;

public class LoadBalanceMonitor {
	private static final StarLogger log = StarLogger.getLogger(LoadBalanceMonitor.class);
	static class UserLoadData implements Comparable<UserLoadData> {
		final int userId;
		
		/* Whenever a user is added to the LoadBalanceMonitor, they are initialized
		 * with a 'basis' equal to the minimum value at the time they were added.
		 * From then on, if the minimum value in the monitor ever drops below the basis
		 * for a user, that user's load is decreased by the value (user-basis - new-min),
		 * and their basis is updated. This is done to avoid penalizing new users that come
		 * into the monitor at a time when other users may have inflated load times due
		 * to running pairs with high timeouts that finish quickly.
		 **/
		Long minBasis;
		Long load;
		
		// If this is null, the user is active. Otherwise, it is the time
		// at which the user became inactive.
		private Date inactiveDateTime = null;
		public UserLoadData(int u, long m, long l) {
			userId = u;
			minBasis = m;
			load = l;
		}

		// comparisons are done based on the user's load value
		@Override
		public int compareTo(UserLoadData arg0) {
			return load.compareTo(arg0.load);
		}
		
		//equality and hashing are done based on userId only
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof UserLoadData)) {
				return false;
			}
			UserLoadData that = (UserLoadData) o;
			return this.userId==that.userId;
		}
		
		@Override
		public int hashCode() {
			return userId;
		}
		
		public boolean active() {
			return inactiveDateTime == null;
		}
		
		/**
		 * If this user is active, inactivates them, setting the current time.
		 */
		public void inactivate() {
			if (this.active()) {
				// dates are initialized to the current time
				inactiveDateTime = new Date();
			}
		}
		
		/**
		 * Calculates what a user's load would be right now if they were to 
		 * be activated. If they are already active, this is just their load value.
		 * Otherwise, it is their load value decayed in a linear fashion over time
		 * since they were inactivated
		 * @return Long load value
		 */
		public long calculateLoadDecay() {
			if (this.active()) {
				return load;
			}
			Date now = new Date();
			Date then = inactiveDateTime;
			// gets the number of hours between the two times, truncated down
			// to the nearest hour.
			Long hours = (now.getTime() - then.getTime())/ (1000*60*60);
			if (hours<=2) {
				return load;
			}
			// simple linear decay function that will reduce load to 0 after one week
			Long loadDecay = this.load - (this.load *  hours / (24*7));
			if (loadDecay<0) {
				return 0;
			}
			return loadDecay;
			
		}
		
		/**
		 * If this user is inactive, activates them and updates their
		 * load accordingly
		 */
		public void activate() {
			if (!this.active()) {
				this.load = calculateLoadDecay();
				inactiveDateTime = null;
			}
		}
	}

	// The basic operations we will be the following
	// Add / Find / Remove users by userIds: All O(1) with a HashSet
	// Find min load in set: O(n) with a HashSet
	// This can be improved on with more complex data structures, but here n is going
	// to get the number of users simultaneously running jobs on a single queue, which
	// is always very small. Constant time overheads of more complex structures are
	// substantial, and so for now this is a very simple and fast option. If we want
	// to support very large numbers of simultaneous users, we likely want another structure
	// such as a combination HashMap / PriorityQueue structure.
	private HashMap<Integer, UserLoadData> loads = new HashMap<>();


	/**
	 * Gets the minimum load value among all active users. Inactive users
	 * are excluded.
	 * @return Minimum value among all active users. Returns null if there are no active users
	 */
	public Long getMin() {
		List<UserLoadData> activeUsers = new ArrayList<>();
		for (UserLoadData d : loads.values()) {
			if (d.active()) {
				activeUsers.add(d);
			}
		}
		if (!activeUsers.isEmpty()) {
			return Collections.min(activeUsers).load;
		}
		return null;
	}
	
	/**
	 * 
	 * This function is only used by testing right now, and it is likely not useful for
	 * production code, as the load values are manipulated internally by functions like
	 * setUsers.
	 * @param userId
	 * @return Long load value for the given user. Null if that user does not exist.
	 */
	public Long getLoad(int userId) {
		UserLoadData d = loads.get(userId);
		if (d!=null) {
			return d.load;
		}
		return null;
	}
	
	
	
	
	/**
	 * Adds a new user to the set of users being managed. The initial load
	 * for the user is the minimum + defaultLoad. Nothing is done if the user is 
	 * already present.
	 * @param userId
	 * @param defaultLoad
	 */
	private void addUser(int userId, long defaultLoad, long basis) {
		if (loads.containsKey(userId)) {
			UserLoadData d = loads.get(userId);
			if (!d.active()) {
				d.activate();
				if (d.load < defaultLoad) {
					d.load = defaultLoad;
					d.minBasis = basis;
				}
			}
			return;
		}
		loads.put(userId, new UserLoadData(userId, basis, defaultLoad));
	}
	
	/**
	 * Completely removes a user from the monitor.
	 * @param userId
	 */
	private void removeUser(int userId) {
		UserLoadData u = loads.get(userId);
		if (u!=null && u.active()) {
			u.inactivate();
		}
	}

	/**
	 * Completely resets the monitor.
	 */
	public void reset() {
		loads = new HashMap<>();
	}
	
	/**
	 * Sets the list of users managed by this monitor to the given set
	 * of users.
	 * @param userIdsToDefaults Mapping of user ids to values to add to their default load.
	 */
	public void setUsers(Map<Integer, Long> userIdsToDefaults) {
		/*boolean noneActive = loads.values().stream().noneMatch(UserLoadData::active);
		if (noneActive) {
			this.reset();
		}*/

		for (Integer i : loads.keySet()) {
			if (!userIdsToDefaults.containsKey(i)) {
				removeUser(i);
			}
		}
		// all new users are set to their default load plus the minimum
		// at the time this was called. The min is added to prevent
		// new users from having an advantage over existing users.
		Long m = getMin();
		if (m==null) {
			m=0L;
		}
		for (Integer i : userIdsToDefaults.keySet()) {
			addUser(i, userIdsToDefaults.get(i) + m, m);
		}
	}
	
	/**
	 * Updates the load associated with a given user
	 * @param userId ID of user to affect. Nothing happens if the user does not already exist.
	 * @param load Increases user load if positive, decreases user load if negative.
	 */
	public void changeLoad(int userId, long load) {
		if (!loads.containsKey(userId)) {
			return;
		}
		// the minimum may change only if this user has the current minimum load
		loads.get(userId).load = loads.get(userId).load + load;
		if (loads.get(userId).load < 0) {
			log.warn("User "+userId +" has load value set to less than 0!");
			loads.get(userId).load = 0L;
		}
	}
	
	/**
	 * 
	 * @param newBasis
	 */
	private void setNewBasis(long newBasis) {
		for (UserLoadData d : loads.values()) {
			if (newBasis < d.minBasis) {
				d.load = d.load - (d.minBasis - newBasis);
				d.load = Math.max(0, d.load);
				d.minBasis = newBasis;
			}
		}
	}
	
	/**
	 * Given a map from userids to loads, calls changeLoad once per entry
	 * in the map.
	 * @param users A mapping from users to load values to update by.
	 */
	public void subtractTimeDeltas(HashMap<Integer, Integer> users) {
		Long oldMin = getMin();
		for (Integer i : users.keySet()) {
			log.debug("user "+i+" is being credited "+users.get(i));
			
			changeLoad(i, -users.get(i));
		}
		Long newMin = getMin();
		if (oldMin!=null && newMin!=null && newMin < oldMin) {
			setNewBasis(newMin);
		}
	}
	
	/**
	 * Determines whether a given user should be skipped, meaning they should not
	 * be allowed to enqueue any more job pairs for the time being. A user is 
	 * skipped whenever their load is substantially greater than the minimum load.
	 * @param userId The ID of the user to check
	 * @return True if the user should be skipped and false if not.
	 */
	public boolean skipUser(int userId) {
		Long userLoad = this.getLoad(userId);
		return userLoad - getMin() > R.LOAD_DIFFERENCE_THRESHOLD;
	}
	
	private String stringRepresentation = null;
	
	
	private String userLoadDataAsString(UserLoadData d) {
		Long loadDecay = d.calculateLoadDecay();
		if (loadDecay==0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		User u = Users.get(d.userId);
		if (u==null) {
			sb.append("unknown user");
		} else {
			sb.append(u.getFullName());
		}
		sb.append(" ");
		if (!d.active()) {
			sb.append("(inactive) ");
		}
		sb.append(": load = ").append(loadDecay);
		return sb.toString();
	}
	
	private List<UserLoadData> getSortedDataList() {
		List<UserLoadData> data = new ArrayList<>();
		data.addAll(loads.values());
		Collections.sort(data);

		return data;
	}
	
	/**
	 * Gets all user load data for every queue as a single formatted string, which
	 * can be displayed on the front end.
	 */
	public void setUserLoadDataFormattedString() {
		StringBuilder sb = new StringBuilder();
		sb.append("minimum = ").append(this.getMin());
		sb.append("\n\n");
		// updates user load values to take into account actual job pair runtimes.
		
		for (UserLoadData d : getSortedDataList()) {
			String loadData = userLoadDataAsString(d);
			if (!loadData.isEmpty()) {
				sb.append(userLoadDataAsString(d));
				sb.append("\n");
			}
		}
		sb.append("\n");		
		stringRepresentation = sb.toString();
	}
	
	public String toString() {
		return stringRepresentation;
	}
	
}
