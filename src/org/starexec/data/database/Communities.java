package org.starexec.data.database;

import org.starexec.constants.DB;
import org.starexec.constants.R;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.DefaultSettings.SettingType;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.logger.StarLogger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles all database interaction for communities (closely tied with the Spaces class)
 *
 * @see Spaces
 */
public class Communities {
	private static final StarLogger log = StarLogger.getLogger(Communities.class);

	/**
	 * @return A list of child spaces belonging to the root space (community spaces)
	 * @author Todd Elvers
	 */
	public static List<Space> getAll() {
		Connection con = null;
		try {
			con = Common.getConnection();
			return getAll(con);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	protected static List<Space> getAll(Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetSubSpacesOfRoot}");
			results = procedure.executeQuery();
			return Spaces.resultsToSpaces(results);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**
	 * Gets every community a user is in.
	 *
	 * @param userId The id of the user.
	 * @return All the community Space's that the user is in.
	 * @author Albert Giegerich
	 */
	public static List<Space> getAllCommunitiesUserIsIn(int userId) {
		List<Space> allCommunities = Communities.getAll();
		List<Space> communitiesUserIsIn = new LinkedList<>();
		for (Space community : allCommunities) {
			int communityId = community.getId();
			if (Users.isMemberOfCommunity(userId, communityId)) {
				// If user is in community add community to list of communities user is in
				communitiesUserIsIn.add(community);
			}
		}
		return communitiesUserIsIn;
	}

	public static List<Space> getAllCommunitiesUserIsIn(Connection con, int userId) {
		List<Space> allCommunities = Communities.getAll(con);
		List<Space> communitiesUserIsIn = new LinkedList<>();
		for (Space community : allCommunities) {
			int communityId = community.getId();
			if (Users.isMemberOfCommunity(con, userId, communityId)) {
				// If user is in community add community to list of communities user is in
				communitiesUserIsIn.add(community);
			}
		}
		return communitiesUserIsIn;
	}

	/**
	 * @return Whether the current time is more than R.COMM_ASSOC_UPDATE_PERIOD after R.COMM_ASSOC_LAST_UPDATE
	 */
	public static boolean commAssocExpired() {
		long timeNow = System.currentTimeMillis();

		if (R.COMM_ASSOC_LAST_UPDATE == null) {
			return true;
		}

		Long timeElapsed = timeNow - R.COMM_ASSOC_LAST_UPDATE;

		log.info("timeElapsed since last comm_assoc update: " + timeElapsed);
		if (timeElapsed > R.COMM_ASSOC_UPDATE_PERIOD) {
			return true;
		}

		log.info("not yet expired");
		return false;
	}

	/**
	 * Helper function for updateCommunityMapIf
	 *
	 * @return A HashMap mapping keys for primitives to 0
	 **/
	public static HashMap<String, Long> initializeCommInfo() {
		HashMap<String, Long> stats = new HashMap<>();

		stats.put("users", 0L);
		stats.put("jobs", 0L);
		stats.put("benchmarks", 0L);
		stats.put("solvers", 0L);
		stats.put("job_pairs", 0L);
		stats.put("disk_usage", 0L);

		return stats;
	}

	/**
	 * Updates R.COMM_INFO_MAP with new data, and sets R.COMM_ASSOC_LAST_UPDATE to the current time
	 */
	public static void updateCommunityMap() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			List<Space> communities = Communities.getAll();
			HashMap<Integer, HashMap<String, Long>> commInfo = new HashMap<>();
			HashMap<String, Long> community;
			Integer commId;
			Long infoCount, infoExtra;

			for (Space c : communities) {
				commInfo.put(c.getId(), initializeCommInfo());
			}

			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetCommunityStatsUsers()}");
			results = procedure.executeQuery();

			while (results.next()) {
				commId = results.getInt("comm_id");
				infoCount = results.getLong("userCount");
				community = commInfo.get(commId);

				community.put("users", infoCount);

				log.info("commId: " + commId + " | userCount: " + infoCount);
			}

			Common.safeClose(results);
			Common.safeClose(procedure);

			procedure = con.prepareCall("{CALL GetCommunityStatsSolvers()}");
			results = procedure.executeQuery();

			while (results.next()) {
				commId = results.getInt("comm_id");
				infoCount = results.getLong("solverCount");
				infoExtra = results.getLong("solverDiskUsage");
				community = commInfo.get(commId);

				community.put("solvers", infoCount);
				community.put("disk_usage", community.get("disk_usage") + infoExtra);

				log.info("commId: " + commId + " | solverCount: " + infoCount + " | solverDisk: " + infoExtra);
			}

			Common.safeClose(results);
			Common.safeClose(procedure);

			procedure = con.prepareCall("{CALL GetCommunityStatsBenches()}");
			results = procedure.executeQuery();

			while (results.next()) {
				commId = results.getInt("comm_id");
				infoCount = results.getLong("benchCount");
				infoExtra = results.getLong("benchDiskUsage");
				community = commInfo.get(commId);

				community.put("benchmarks", infoCount);
				community.put("disk_usage", community.get("disk_usage") + infoExtra);

				log.info("commId: " + commId + " | benchCount: " + infoCount + " | benchDisk: " + infoExtra);
			}

			Common.safeClose(results);
			Common.safeClose(procedure);

			procedure = con.prepareCall("{CALL GetCommunityStatsJobs()}");
			results = procedure.executeQuery();

			while (results.next()) {
				commId = results.getInt("comm_id");
				infoCount = results.getLong("jobCount");
				infoExtra = results.getLong("jobPairCount");
				community = commInfo.get(commId);

				community.put("jobs", infoCount);
				community.put("job_pairs", infoExtra);

				log.info("commId: " + commId + " | jobCount: " + infoCount + " | jobPairCount: " + infoExtra);
			}

			R.COMM_INFO_MAP = commInfo;
			R.COMM_ASSOC_LAST_UPDATE = System.currentTimeMillis();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
	}

	/**
	 * retrieves information for community stats page if current information has expired
	 *
	 * @author Julio Cervantes
	 **/
	public synchronized static void updateCommunityMapIf() {
		if (commAssocExpired()) {
			updateCommunityMap();
		}
	}

	/**
	 * Given a DefaultSettings object with all of its fields yet, adds the settings object to the database
	 *
	 * @param d
	 * @return The ID of the new settings profile, or -1 on error
	 */

	public static int createNewDefaultSettings(DefaultSettings d) {
		d.setType(SettingType.COMMUNITY);
		return Settings.addNewSettingsProfile(d);
	}

	/**
	 * Get the default setting of the community given by the id. If the default settings do not already exist, they are
	 * initialized to default values and returned
	 *
	 * @param id the space id of the community
	 * @return DefaultSettings object
	 * @author Ruoyu Zhang
	 */
	public static DefaultSettings getDefaultSettings(int id) {
		Connection con = null;
		try {
			//first, find the ID of the community this space is a part of
			con = Common.getConnection();
			return getDefaultSettings(con, id);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	protected static DefaultSettings getDefaultSettings(Connection con, int id) {
		CallableStatement procedure = null;
		ResultSet results = null;
		//if the current space is the root, we just want to return the default profile
		if (id == 1) {
			return new DefaultSettings();
		}
		try {
			//first, find the ID of the community this space is a part of
			procedure = con.prepareCall("{CALL GetCommunityOfSpace(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();

			int community;
			if (results.next()) {
				//if we found the community, get the default settings
				community = results.getInt("community");
				Common.safeClose(results);
				Common.safeClose(procedure);

				//this means the community was NULL, which occurs when this is called on the root space.
				if (community <= 0) {
					log.debug("no default settings profile set for space = " + id);
					return null;
				}

				List<DefaultSettings> settings =
						Settings.getDefaultSettingsByPrimIdAndType(community, SettingType.COMMUNITY);

				if (!settings.isEmpty()) {
					return settings.get(0);
				} else {
					//no settings existed, so create one for this community and return that
					log.debug("unable to find any default settings for community id = " + community);
					DefaultSettings d = new DefaultSettings();
					String name = Spaces.getName(community);
					if (name.length() > DB.SETTINGS_NAME_LEN) {
						name = name.substring(0, DB.SETTINGS_NAME_LEN); //make sure it isn't too large
					}
					d.setName(name);
					d.setPrimId(community);
					log.debug("calling createNewDefaultSettings on community with id = " + community);
					int newId = createNewDefaultSettings(d);
					if (newId > 0) {
						return d;
					} else {
						//failed to create new profile
						log.error("error creating new default settings profile");
						return null;
					}
				}
			} else {
				log.error("We were unable to find the community for the space =" + id);
				return null;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**
	 * Gets a space with minimal information (only details about the space itself)
	 *
	 * @param id The id of the space to get information for
	 * @return A space object consisting of shallow information about the space
	 * @author Tyler Jensen
	 */
	public static Space getDetails(int id) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetCommunityById(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();

			if (results.next()) {
				Space s = new Space();
				s.setName(results.getString("space.name"));
				s.setId(results.getInt("space.id"));
				s.setDescription(results.getString("space.description"));
				s.setLocked(results.getBoolean("space.locked"));
				s.setCreated(results.getTimestamp("space.created"));
				return s;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return null;
	}

	/**
	 * Checks to see if the space with the given space ID is a community or not (the space is a child of the root
	 * space)
	 *
	 * @param spaceId the ID of the space to check
	 * @return true iff the space is a community
	 * @author Skylar Stark
	 */
	public static boolean isCommunity(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL IsCommunity(?)}");
			procedure.setInt(1, spaceId);

			results = procedure.executeQuery();

			return results.next();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return false;
	}

	/**
	 * @return The test community as specified in the configuration, or null if none exists
	 */
	public static Space getTestCommunity() {
		Space s = Communities.getDetails(R.TEST_COMMUNITY_ID);
		if (s == null) {
			log.warn("getTestCommunity could not retrieve the test community--please set one up in the configuration");
		}
		return s;
	}

	/**
	 * Creates a new personal space as a subspace of the space the user was admitted to
	 *
	 * @param communityId the id of the space this new personal space will be a subspace of
	 * @param user the user for whom this new personal space is being created
	 * @return true if the personal subspace was successfully created, false otherwise
	 */
	public static void createPersonalSubspace(int communityId, User user) {
		// Generate space name (e.g. IF name = Todd Elvers, THEN personal space name = todd_elvers)
		final String name = (user.getFirstName() + "_" + user.getLastName()).toLowerCase();

		// Set the space's attributes
		Space s = new Space();
		s.setName(name);
		s.setDescription(R.PERSONAL_SPACE_DESCRIPTION);
		s.setLocked(false);
		s.setPermission(new Permission(true));
		s.setParentSpace(getUsersSpace(communityId));

		// If Spaces.add returns -1 it means there was a problem
		// TODO: Just throw an exception
		if (Spaces.add(s, user.getId()) > 0) {
			log.info("createPersonalSubspace",
			         "Personal space successfully created for user [" + user.getFullName() + "]");
		} else {
			log.error("createPersonalSubspace",
			          "Personal space NOT successfully created for user [" + user.getFullName() +
			          "] in community " + communityId);
		}
	}

	/**
	 * Looks up the "Users" subspace for a given Community
	 * @param communityId
	 * @return ID of Users subspace, or communityId if no subspace exists
	 */
	private static int getUsersSpace(int communityId) {
		try {
			return Common.query("{CALL GetUsersSpace(?)}", p -> p.setInt(1, communityId), r -> {
				if (r.next()) {
					return r.getInt("id"); // The "Users" Space for this Community
				} else {
					return communityId; // The root Space for this Community
				}
			});
		} catch (SQLException e) {
			log.error("getUsersSpace", e);
			return communityId;
		}
	}

	/**
	 * Creates a "Users" subspace for a given Community.
	 * @param communityId
	 */
	public static void createUsersSpace(int communityId) throws SQLException {
		/* Bail if a Users space already exists in this Community */
		if (getUsersSpace(communityId) != communityId) {
			return;
		}
		Common.update("{CALL CreateUsersSpace(?)}", p -> p.setInt(1, communityId) );
	}
}
