package org.starexec.data.database;

import org.apache.commons.io.FileUtils;
import org.starexec.constants.PaginationQueries;
import org.starexec.constants.R;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.DefaultSettings.SettingType;
import org.starexec.data.to.Job;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.exceptions.StarExecSecurityException;
import org.starexec.logger.StarLogger;
import org.starexec.util.*;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles all database interaction for users
 */
public class Users {
	private static final StarLogger log = StarLogger.getLogger(Users.class);

	/**
	 * Associates a user with a space (i.e. adds the user to the space)
	 *
	 * @param con The connection to perform the database operation on
	 * @param userId The id of the user to add to the space
	 * @param spaceId The space to add the user to
	 * @author Tyler Jensen
	 */
	private static void associate(Connection con, int userId, int spaceId) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL AddUserToSpace(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, spaceId);


			procedure.executeUpdate();
			log.info("User [" + userId + "] added to space [" + spaceId + "]");
		} catch (Exception e) {
			log.error("associate", e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds a new DefaultSettings object to the database. The type of the DefaultSettings object will be a USER
	 * settings
	 * object
	 *
	 * @param d The object to store in the database
	 * @return The new ID of the setting
	 */
	public static int createNewDefaultSettings(DefaultSettings d) {
		d.setType(SettingType.USER);
		return Settings.addNewSettingsProfile(d);
	}

	/**
	 * Gets the user preference for a default dataTables page size
	 *
	 * @param userId The ID of the user having the setting changed
	 * @param newSize the number of elements in a default table page
	 * @return True on success and false otherwise
	 */
	public static boolean setDefaultPageSize(int userId, int newSize) {
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL SetDefaultPageSize(?,?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, newSize);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Gets the user preference for a default dataTables page size
	 *
	 * @param userId
	 * @return The integer default page size for the given user, or 10 (the system default) if none could be found.
	 */
	public static int getDefaultPageSize(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetDefaultPageSize(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("pageSize");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);

			Common.safeClose(results);
		}
		return 10;
	}

	/**
	 * Adds an association between a list of users and a space
	 *
	 * @param con the database transaction to use
	 * @param userIds the ids of the users to add to a space
	 * @param spaceId the id of the space to add the users to
	 * @throws Exception
	 * @author Todd Elvers
	 */
	private static void associate(Connection con, List<Integer> userIds, int spaceId) {
		for (int uid : userIds) {
			Users.associate(con, uid, spaceId);
		}
	}

	/**
	 * Associates a user with a space (i.e. adds the user to the space)
	 *
	 * @param userId The id of the user to add to the space
	 * @param spaceId The space to add the user to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(int userId, int spaceId) {
		Connection con = null;

		try {
			con = Common.getConnection();
			Users.associate(con, userId, spaceId);

			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * Associates the given users with the given space
	 *
	 * @param userIds The users to add to the space. Nothing will happen if they are already present
	 * @param spaceId The ID of the space to add users to
	 * @param hierarchy True to add users to every space in the hierarchy rooted at spaceId, and false to only do the
	 * single space
	 * @param requestUserId The ID of the user making the request. If hierarchy is true, this is used to determine
	 * which
	 * subspaces to impact
	 * @return True on success and false otherwise
	 */
	public static boolean associate(List<Integer> userIds, int spaceId, boolean hierarchy, int requestUserId) {
		if (!hierarchy) {
			return associate(userIds, spaceId);
		} else {
			List<Space> subspaces =
					Spaces.trimSubSpaces(requestUserId, Spaces.getSubSpaceHierarchy(spaceId, requestUserId));
			List<Integer> subspaceIds = new LinkedList<>();

			// Add the destination space to the list of spaces to associate the user(s) with
			subspaceIds.add(spaceId);

			// Iterate once through all subspaces of the destination space to ensure the user has addUser permissions
			// in each
			for (Space subspace : subspaces) {
				subspaceIds.add(subspace.getId());
			}
			return associate(userIds, subspaceIds);
		}
	}

	/**
	 * Associates a group of users user with a space (i.e. adds the user to the space)
	 *
	 * @param userIds The id's of the users to add to the space
	 * @param spaceId The space to add the users to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(List<Integer> userIds, int spaceId) {
		List<Integer> space = new ArrayList<>();
		space.add(spaceId);
		return associate(userIds, space);
	}

	/**
	 * Adds an association between a list of users and a list of spaces, in an all-or-none fashion
	 *
	 * @param userIds the ids of the users to add to the spaces
	 * @param spaceIds the ids of the spaces to add the users to
	 * @return true iff all spaces in spaceIds successfully have all users in userIds add to them, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean associate(List<Integer> userIds, List<Integer> spaceIds) {
		Connection con = null;

		try {
			con = Common.getConnection();
			Common.beginTransaction(con);

			// For each space id in spaceIds, add all the users to it
			for (int spaceId : spaceIds) {
				Users.associate(con, userIds, spaceId);
			}

			log.info("Successfully added users " + userIds.toString() + " to spaces " + spaceIds.toString());
			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		log.error("Failed to add users " + userIds.toString() + " to spaces " + spaceIds.toString());
		return false;
	}

	/**
	 * Given a ResultSet currently pointing at a row with a user in it, returns that user.
	 *
	 * @param results
	 * @return
	 * @throws SQLException
	 */
	private static List<User> resultsToUsers(ResultSet results) throws SQLException {
		List<User> users = new ArrayList<>();
		while (results.next()) {
			User u = new User();
			u.setId(results.getInt("id"));
			u.setEmail(results.getString("email"));
			u.setFirstName(results.getString("first_name"));
			u.setLastName(results.getString("last_name"));
			u.setInstitution(results.getString("institution"));
			u.setCreateDate(results.getTimestamp("created"));
			u.setDiskQuota(results.getLong("disk_quota"));
			u.setSubscribedToReports(results.getBoolean("subscribed_to_reports"));
			u.setRole(results.getString("role"));
			u.setPairQuota(results.getInt("job_pair_quota"));
			u.setDiskUsage(results.getLong("disk_size"));
			u.setSubscribedToErrorLogs(results.getBoolean("subscribed_to_error_logs"));
			users.add(u);
		}
		return users;
	}

	/**
	 * Retrieves a user from the database given the user's id
	 *
	 * @param id the id of the user to get
	 * @return The user object associated with the user
	 * @author Tyler Jensen
	 */
	public static User get(int id) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return Users.get(con, id);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	public static User get(Connection con, int id) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetUserById(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();

			List<User> users = resultsToUsers(results);
			if (!users.isEmpty()) {
				return users.get(0);
			}
			log.debug("Could not find user with id = " + id);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Retrieves a user from the database given the email address
	 *
	 * @param email The email of the user to retrieve
	 * @return The user object associated with the user
	 * @author Tyler Jensen
	 */
	public static User get(String email) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUserByEmail(?)}");
			procedure.setString(1, email);
			results = procedure.executeQuery();

			List<User> users = resultsToUsers(results);
			if (!users.isEmpty()) {
				return users.get(0);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**
	 * @return A list of all users that will receive the Starexec report email
	 */
	public static List<User> getAllUsersSubscribedToReports() {
		return getUserListFromQuery("{CALL GetAllUsersSubscribedToReports()}");
	}

	/**
	 * @return A list of all the admins in the system
	 */
	public static List<User> getAdmins() {
		return getUserListFromQuery("{CALL GetAdmins()}");
	}

	/**
	 * NOTE: Make sure your your queries result-set has all the columns that resultsToUser checks for or it will throw
	 * an exception. Returns a list of users based on the sql database stored procedure that is input.
	 *
	 * @param sql An sql statement used for calling a database stored procedure.
	 * @return A List of users based on the sql procedure that was input.
	 * @author Albert Giegerich
	 */
	private static List<User> getUserListFromQuery(String sql) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall(sql);
			results = procedure.executeQuery();

			return resultsToUsers(results);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Gets the number of Users in the whole system
	 *
	 * @return The integer number of users in the system
	 * @author Wyatt Kaiser
	 */

	public static int getCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUserCount()}");
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("userCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return 0;
	}

	/**
	 * Gets the number of Users in a given space
	 *
	 * @param spaceId the id of the space to count the Users in
	 * @return the number of Users
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId) {
		return getCountInSpace(spaceId, "");
	}

	/**
	 * Gets the number of Users in a given space that match a given query
	 *
	 * @param spaceId the id of the space to count the Users in
	 * @param query The query to match the users against
	 * @return the number of Users
	 * @author Eric Burns
	 */
	public static int getCountInSpace(int spaceId, String query) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUserCountInSpaceWithQuery(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2, query);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("userCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return 0;
	}

	public static boolean isDiskQuotaExceeded(int userId) {
		return Users.get(userId).getDiskQuota() <= Users.getDiskUsage(userId);
	}

	/**
	 * Iterates through every user in the DB, updating their disk_size fields in the DB.
	 *
	 * @return
	 */
	public static boolean updateAllUserDiskSizes() {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			for (User u : Users.getUsersForNextPageAdmin(new DataTablesQuery(0, Integer.MAX_VALUE, 0, false, ""))) {
				procedure = con.prepareCall("{CALL UpdateUserDiskUsage(?,?)}");
				procedure.setInt(1, u.getId());
				procedure.registerOutParameter(2, java.sql.Types.BIGINT);
				procedure.executeUpdate();
				long difference = procedure.getLong(2);
				Common.safeClose(procedure);

				if (difference != 0) {
					log.info(
							"Disk usage did not match between users table " + "and other tables for user " + u.getId
									() +
									". Difference was " + difference);
				}
			}
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Gets the number of bytes a user is consuming on disk by returning the disk_usage column from the users table.
	 *
	 * @param userId the id of the user to get the disk usage of
	 * @return the disk usage of the given user
	 */
	public static long getDiskUsage(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUserDiskUsage(?)}");
			procedure.setInt(1, userId);

			results = procedure.executeQuery();
			if (results.next()) {
				return results.getLong("disk_size");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;
	}

	/**
	 * Returns the (hashed) password of the given user.
	 *
	 * @param userId the user ID of the user to get the password of
	 * @return The (hashed) password for the user
	 * @author Skylar Stark
	 */
	public static String getPassword(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetPasswordById(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getString("password");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**
	 * Retrieves an unregistered user from the database given their user_id. This is a helper method for user
	 * registration and shouldn't be used anywhere else.
	 *
	 * @param id The id of the unregistered user to retrieve
	 * @return the unregistered User object if one exists, null otherwise
	 * @author Todd Elvers
	 */
	public static User getUnregistered(int id) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUnregisteredUserById(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();

			List<User> users = resultsToUsers(results);
			if (!users.isEmpty()) {
				return users.get(0);
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
	 * Subscribes a user to error log reports.
	 *
	 * @param userId the user to subscribe.
	 * @throws SQLException on database error.
	 */
	public static void subscribeToErrorLogs(int userId) throws SQLException {
		Common.update("{CALL SubscribeUserToErrorLogs(?)}", procedure -> procedure.setInt(1, userId));
	}

	/**
	 * Unsubscribes a user from error log reports.
	 *
	 * @param userId the user to unsubscribe.
	 * @throws SQLException on database error.
	 */
	public static void unsubscribeUserFromErrorLogs(int userId) throws SQLException {
		Common.update("{CALL UnsubscribeUserFromErrorLogs(?)}", procedure -> procedure.setInt(1, userId));
	}

	public static List<User> getUsersSubscribedToErrorLogs() throws SQLException {
		return Common.query("{CALL GetAllUsersSubscribedToErrorLogs()}", procedure -> {} // no parameters to set
				, Users::resultsToUsers);
	}

	/**
	 * Returns true if a user is already registered with a given e-mail
	 *
	 * @param email - the email of the user to search for
	 * @return true if user exists; false if no user with that e-mail
	 * @author Wyatt Kaiser
	 */

	public static boolean getUserByEmail(String email) {
		log.debug("email = " + email);
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUserByEmail(?)}");
			procedure.setString(1, email);
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

	private static String getUserOrderColumn(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return "full_name";
		case 1:
			return "institution";
		case 2:
			return "email";
		}

		return "full_name";
	}

	/**
	 * Gets the minimal number of Users necessary in order to service the client's request for the next page of
	 * Users in
	 * their DataTables object
	 *
	 * @param query a DataTablesQuery object
	 * @param spaceId the id of the space to get the Users from
	 * @return a list of 10, 25, 50, or 100 Users containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */
	public static List<User> getUsersForNextPage(DataTablesQuery query, int spaceId) {
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_USERS_IN_SPACE_QUERY,
			                                                            getUserOrderColumn(query.getSortColumn()),
			                                                            query
			);

			procedure = new NamedParameterStatement(con, builder.getSQL());
			procedure.setInt("spaceId", spaceId);
			procedure.setString("query", query.getSearchQuery());
			results = procedure.executeQuery();
			List<User> users = new LinkedList<>();

			while (results.next()) {
				User u = new User();
				u.setId(results.getInt("id"));
				u.setInstitution(results.getString("institution"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setEmail(results.getString("email"));

				//Prevents public user from appearing in table.
				users.add(u);
			}

			return users;
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
	 * Gets the minimal number of Users necessary in order to service the client's request for the next page of
	 * Users in
	 * their DataTables object
	 *
	 * @param query A DataTablesQuery object
	 * @return a list of 10, 25, 50, or 100 Users containing the minimal amount of data necessary
	 * @author Wyatt Kaiser
	 **/
	public static List<User> getUsersForNextPageAdmin(DataTablesQuery query) {
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_USERS_ADMIN_QUERY,
			                                                            getUserOrderColumn(query.getSortColumn()),
			                                                            query
			);

			procedure = new NamedParameterStatement(con, builder.getSQL());
			procedure.setString("query", query.getSearchQuery());
			results = procedure.executeQuery();
			List<User> users = new LinkedList<>();

			while (results.next()) {
				User u = new User();
				u.setId(results.getInt("id"));
				u.setInstitution(results.getString("institution"));
				u.setFirstName(results.getString("first_name"));
				u.setLastName(results.getString("last_name"));
				u.setEmail(results.getString("email"));
				u.setRole(results.getString("role"));
				u.setSubscribedToReports(results.getBoolean("subscribed_to_reports"));
				users.add(u);
			}

			return users;
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
	 * Checks to see whether the given user is in the given community
	 *
	 * @param userId The ID of the user in question
	 * @param communityId The ID of the community in question
	 * @return True if the user is in the community, false if not or on error
	 * @author Eric Burns
	 */

	public static boolean isMemberOfCommunity(int userId, int communityId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return isMemberOfCommunity(con, userId, communityId);
		} catch (Exception e) {
			log.error("isMemberOfCommunity", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	protected static boolean isMemberOfCommunity(Connection con, int userId, int communityId) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL IsMemberOfCommunity(?,?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, communityId);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("spaceCount") > 0;
			}
		} catch (Exception e) {
			log.error("isMemberOfCommunity", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Checks if a given user is a member of a particular space
	 *
	 * @param userId the id of the user to check for membership in a particular space
	 * @param spaceId the id of the space to check for a given user's membership
	 * @return true iff the given user is a member of the given space, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean isMemberOfSpace(int userId, int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL IsMemberOfSpace(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, spaceId);
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
	 * Adds the specified user to the database. This method will hash the user's password for them, so it must be
	 * supplied in plaintext.
	 *
	 * @param user The user to add
	 * @param communityId the id of the community to add this user wants to join
	 * @param message the message from the user to the leaders of a community
	 * @param code the unique code to add to the database for this user
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean register(User user, int communityId, String code, String message) {
		log.debug("begin register..");
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);

			String hashedPass = Hash.hashPassword(user.getPassword());

			procedure = con.prepareCall("{CALL AddUser(?, ?, ?, ?, ?, ?, ?)}");
			procedure.setString(1, user.getFirstName());
			procedure.setString(2, user.getLastName());
			procedure.setString(3, user.getEmail());
			procedure.setString(4, user.getInstitution());
			procedure.setString(5, hashedPass);
			procedure.setLong(6, R.DEFAULT_DISK_QUOTA);

			// Register output of ID the user is inserted under
			procedure.registerOutParameter(7, java.sql.Types.INTEGER);

			// Add user to the users table and check to be sure 1 row was modified
			procedure.executeUpdate();

			// Extract id from OUT parameter
			user.setId(procedure.getInt(7));

			boolean successfulRegistration = false;

			// Add unique activation code to VERIFY table under new user's id
			if (Requests.addActivationCode(con, user, code)) {
				// Add user's request to join a community to COMMUNITY_REQUESTS
				successfulRegistration = Requests.addCommunityRequest(con, user, communityId, code, message);
			}

			if (successfulRegistration) {
				Common.endTransaction(con);
				log.info("New user [" + user + "] successfully registered");
				return true;
			} else {
				Common.doRollback(con);
				log.info("New user [" + user + "] failed to register");
				return false;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return false;
	}

	/**
	 * Sets a new pair quota for a given user
	 *
	 * @param userId the user to set the new pair quota for
	 * @param newPairQuota The new number of job pairs
	 * @return true iff the new pair quota is successfully set, false otherwise
	 */
	public static boolean setPairQuota(int userId, int newPairQuota) {
		log.debug("in set pairquotas");
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateUserPairQuota(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, newPairQuota);

			procedure.executeUpdate();

			return true;
		} catch (Exception e) {
			log.error("there was a problem setting pair quota" + e.getMessage());
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Sets a new disk quota for a given user (input should always be bytes)
	 *
	 * @param userId the user to set the new disk quota for
	 * @param newDiskQuota the new disk quota, in bytes, to set for the given user
	 * @return true iff the new disk quota is successfully set, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean setDiskQuota(int userId, long newDiskQuota) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateUserDiskQuota(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setLong(2, newDiskQuota);

			procedure.executeUpdate();

			log.info("Disk quota changed to [" +
					FileUtils.byteCountToDisplaySize(newDiskQuota) +
					"] for user [" + userId + "]"
			);

			return true;
		} catch (Exception e) {
			log.error("setDiskQuota", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		log.warn("Failed to change disk quota to [" +
				FileUtils.byteCountToDisplaySize(newDiskQuota) +
				"] for user [" + userId + "]"
		);
		return false;
	}


	//We should not be using this right now, since our login setup can't handle changing email

	/**
	 * Updates the email address of a user in the database with the given user ID
	 *
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the email address will be updated to
	 * @author Skylar Stark
	 */
	public static void updateEmail(int userId, String newValue) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateEmail(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, newValue);

			procedure.executeUpdate();
			log.info("User [" + userId + "] updated e-mail address to [" + newValue + "]");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Updates the first name of a user in the database with the given user ID
	 *
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the first name will be updated to
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	public static boolean updateFirstName(int userId, String newValue) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateFirstName(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, newValue);

			procedure.executeUpdate();
			log.info("User [" + userId + "] updated first name to [" + newValue + "]");
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return false;
	}

	/**
	 * Updates the institution of a user in the database with the given user ID
	 *
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the institution will be updated to
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	public static boolean updateInstitution(int userId, String newValue) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateInstitution(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, newValue);

			procedure.executeUpdate();
			log.info("User [" + userId + "] updated institution to [" + newValue + "]");
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return false;
	}

	/**
	 * Updates the last name of a user in the database with the given user ID
	 *
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the last name will be updated to
	 * @return true iff the update succeeds on exactly one entry
	 * @author Skylar Stark
	 */
	public static boolean updateLastName(int userId, String newValue) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateLastName(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, newValue);

			procedure.executeUpdate();
			log.info("User [" + userId + "] updated last name to [" + newValue + "]");
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return false;
	}

	/**
	 * Updates the password of a user in the database with the given user ID. Hashes the password before updating, so
	 * the password should be supplied in plain-text.
	 *
	 * @param userId the user ID of the user we want to update
	 * @param newValue what the password will be updated to
	 * @return True if the operation was a success, false otherwise
	 * @author Skylar Stark
	 */
	public static boolean updatePassword(int userId, String newValue) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdatePassword(?, ?)}");
			procedure.setInt(1, userId);
			String hashedPassword = Hash.hashPassword(newValue);
			procedure.setString(2, hashedPassword);

			procedure.executeUpdate();
			log.info("User [" + userId + "] updated password");
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return false;
	}

	/**
	 * Completely deletes a user from the database.
	 *
	 * @param userToDeleteId The ID of the user to delete
	 * @return True on success, false on error
	 * @throws StarExecSecurityException if user making request cannot delete user.
	 */
	public static boolean deleteUser(int userToDeleteId) {
		log.debug("User with id=" + userToDeleteId + " is about to be deleted");
		Connection con = null;
		CallableStatement procedure = null;
		try {

			// Delete the users primitive directories. This must occur before we delete the user
			// so we can still get the users job id's from the database.
			deleteUsersPrimitiveDirectories(userToDeleteId);


			// Delete the user from the database, this should delete all benchmarks and solvers and jobs
			// from the database using cascading deletes.
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL DeleteUser(?)}");
			procedure.setInt(1, userToDeleteId);
			procedure.executeQuery();

			log.debug("Successfully deleted user with id=" + userToDeleteId);
			return true;
		} catch (Exception e) {
			log.error("deleteUse", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		log.debug("internal error trying to delete user with id = " + userToDeleteId);
		return false;
	}

	/**
	 * Deletes a user's benchmark and solver directory in the data directory.
	 *
	 * @param userId Id of user whose benchmark and solver directories are to be deleted.
	 * @author Albert Giegerich
	 */
	private static void deleteUsersPrimitiveDirectories(int userId) {
		log.debug("Deleting primitive directories of user with id=" + userId);
		deleteUsersSolverDirectory(userId);
		deleteUsersBenchmarkDirectory(userId);
		deleteUsersJobDirectories(userId);
	}

	/**
	 * Deletes the given jobs' directories.
	 *
	 * @author Albert Giegerich
	 */
	private static void deleteUsersJobDirectories(int userId) {
		final String method = "deleteUsersJobDirectories";
		log.entry(method);
		List<Job> jobs = Jobs.getByUserId(userId);
		for (Job job : jobs) {
			final String jobDirectory = Jobs.getDirectory(job.getId());
			log.debug(method, "User is being deleted, deleting job directory with path: " + jobDirectory);
			Util.safeDeleteDirectory(jobDirectory);
		}
	}

	/**
	 * Deletes a user's solver directory in the data directory.
	 *
	 * @param userId Id of user whose solver directory is to be deleted.
	 * @author Albert Giegerich
	 */
	private static void deleteUsersSolverDirectory(int userId) {
		String pathToSolverDirectory = R.getSolverPath() + "/" + userId;
		Util.safeDeleteDirectory(pathToSolverDirectory);
	}

	/**
	 * Deletes a user's benchmark directory in the data directory.
	 *
	 * @param userId Id of user whose benchmark directory is to be deleted.
	 * @author Albert Giegerich
	 */
	private static void deleteUsersBenchmarkDirectory(int userId) {
		String pathToBenchmarkDirectory = R.getBenchmarkPath() + "/" + userId;
		Util.safeDeleteDirectory(pathToBenchmarkDirectory);
	}

	/**
	 * Checks to see whether the given user is an admin
	 *
	 * @param userId
	 * @return True if the user is an admin and false otherwise (including if there was an error)
	 */
	public static boolean isAdmin(int userId) {
		User u = Users.get(userId);
		return u != null && u.getRole().equals(R.ADMIN_ROLE_NAME);
	}

	public static boolean isAdmin(Connection con, int userId) {
		User u = Users.get(con, userId);
		return u != null && u.getRole().equals(R.ADMIN_ROLE_NAME);
	}

	/**
	 * Checks to see whether the given user is a developer
	 *
	 * @param userId
	 * @return True if the user is a developer and false otherwise (including if there was an error)
	 */

	public static boolean isDeveloper(int userId) {
		User u = Users.get(userId);
		return u != null && u.getRole().equals(R.DEVELOPER_ROLE_NAME);
	}

	public static boolean isDeveloper(Connection con, int userId) {
		User u = Users.get(con, userId);
		return u != null && u.getRole().equals(R.DEVELOPER_ROLE_NAME);
	}

	/**
	 * Checks to see whether the given user is the public user
	 *
	 * @param userId
	 * @return True if the given user is the public (guest) user
	 */

	public static boolean isPublicUser(int userId) {
		return userId == R.PUBLIC_USER_ID;
	}

	/**
	 * Checks to see whether the given user is unauthorized
	 *
	 * @param userId
	 * @return True if the user has yet to be accepted by a community and false otherwise
	 */
	public static boolean isUnauthorized(int userId) {
		User u = Users.get(userId);
		return u != null && u.getRole().equals(R.UNAUTHORIZED_ROLE_NAME);
	}

	/**
	 * Checks to see whether the given user is suspended
	 *
	 * @param userId
	 * @return True if the user has been suspended by an admin and false otherwise
	 */
	public static boolean isSuspended(int userId) {
		User u = Users.get(userId);
		return u != null && u.getRole().equals(R.SUSPENDED_ROLE_NAME);
	}

	/**
	 * Checks to see whether the given user is a normal user
	 *
	 * @param userId
	 * @return True if the use has the 'user' role and false for any other role
	 */
	public static boolean isNormalUser(int userId) {
		User u = Users.get(userId);
		return u != null && u.getRole().equals(R.DEFAULT_USER_ROLE_NAME);
	}

	/**
	 * Adds the given user to the database
	 *
	 * @param user The user to add. The user's password should be in plaintext and will be hashed before being added
	 * @return The ID of the new user
	 */
	public static int add(User user) {
		log.debug("beginning to add user...");
		log.debug("pass = " + user.getPassword());
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			String hashedPass = Hash.hashPassword(user.getPassword());
			log.debug("hashedPass = " + hashedPass);
			procedure = con.prepareCall("{CALL AddUserAuthorized(?, ?, ?, ?, ?, ?, ?,?,?)}");
			procedure.setString(1, user.getFirstName());
			procedure.setString(2, user.getLastName());
			procedure.setString(3, user.getEmail());
			procedure.setString(4, user.getInstitution());
			procedure.setString(5, hashedPass);
			procedure.setLong(6, user.getDiskQuota());
			procedure.setString(7, user.getRole());
			procedure.setInt(8, user.getPairQuota());
			// Register output of ID the user is inserted under
			procedure.registerOutParameter(9, java.sql.Types.INTEGER);

			// Add user to the users table and check to be sure 1 row was modified
			procedure.executeUpdate();
			// Extract id from OUT parameter
			user.setId(procedure.getInt(9));
			log.debug("newid = " + user.getId());
			return user.getId();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return -1;
	}

	/**
	 * Sets the role of the given user to the given role
	 *
	 * @param userId The ID of the user to affect
	 * @param role The role to give the user
	 * @return True on success and false otherwise
	 */
	public static boolean changeUserRole(int userId, String role) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL ChangeUserRole(?,?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, role);
			procedure.executeUpdate();

			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Updates a user's preferences regarding whether to receive weekly emails containing Starexec reports
	 *
	 * @param userId
	 * @param willBeSubscribed True to subscribe and false to unsubscribe
	 * @return True on success and false otherwise
	 */
	private static boolean setUserReportSubscription(int userId, Boolean willBeSubscribed) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL SetUserReportSubscription(?,?)}");
			procedure.setInt(1, userId);
			procedure.setBoolean(2, willBeSubscribed);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Sets the role of the given user to 'suspended' NOTE: The old role of the user is not stored, so if they are
	 * later
	 * reinstated, they will always be set to 'user,' regardless of their old role!
	 *
	 * @param userId
	 * @return True on success or false otherwise
	 */
	public static boolean suspend(int userId) {
		return changeUserRole(userId, R.SUSPENDED_ROLE_NAME);
	}

	/**
	 * Sets the role of the given user back to 'user'
	 *
	 * @param userId
	 * @return True on success and false otherwise
	 */
	public static boolean reinstate(int userId) {
		return changeUserRole(userId, R.DEFAULT_USER_ROLE_NAME);
	}

	/**
	 * Sign up the given user to receive weekly email Starexec reports
	 *
	 * @param userId
	 * @return True on success and false otherwise
	 */
	public static boolean subscribeToReports(int userId) {
		return setUserReportSubscription(userId, true);
	}

	/**
	 * Unsubscribe the given user from receiving weekly email Starexec reports
	 *
	 * @param userId
	 * @return True on success and false otherwise
	 */
	public static boolean unsubscribeFromReports(int userId) {
		return setUserReportSubscription(userId, false);
	}

	/**
	 * Returns the list of community IDs for every community this user is a part of
	 *
	 * @param userId The ID of the user in question
	 * @return A list of community IDs for every community this user is a member of
	 */
	public static List<Integer> getCommunities(int userId) {

		try {
			List<Integer> comms = new ArrayList<>();
			for (Space s : Communities.getAll()) {
				if (Users.isMemberOfCommunity(userId, s.getId())) {
					comms.add(s.getId());
				}
			}

			return comms;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}
}
