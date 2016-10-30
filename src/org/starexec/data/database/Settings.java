package org.starexec.data.database;

import org.apache.log4j.Logger;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.DefaultSettings.SettingType;
import org.starexec.data.to.Space;
import org.starexec.util.LogUtil;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class Settings {
	private static Logger log=Logger.getLogger(Settings.class);
	private static LogUtil logUtil = new LogUtil(log);

	protected static int addNewSettingsProfile(DefaultSettings settings) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure = con.prepareCall("{CALL CreateDefaultSettings(?, ?, ?, ?, ?, ?,?,?,?,?,?,?,?)}");
			procedure.setInt(1, settings.getPrimId());
			procedure.setObject(2, settings.getPostProcessorId());
			procedure.setInt(3, settings.getCpuTimeout());
			procedure.setInt(4, settings.getWallclockTimeout());
			procedure.setBoolean(5, settings.isDependenciesEnabled());
			procedure.setObject(6, settings.getBenchId());
			procedure.setLong(7,settings.getMaxMemory()); //memory initialized to 1 gigabyte
			procedure.setObject(8,settings.getSolverId());
			procedure.setObject(9, settings.getBenchProcessorId());
			procedure.setObject(10,settings.getPreProcessorId());
			procedure.setInt(11, settings.getType().getValue());
			procedure.setString(12,settings.getName());
			procedure.registerOutParameter(13, java.sql.Types.INTEGER);	
			procedure.executeUpdate();			

			// Update the job's ID so it can be used outside this method
			settings.setId(procedure.getInt(13));
			return settings.getId();
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return -1;
	
	}
	
	/**
	 * Given a DefaultSettings object with all of its fields set, including id,
	 * updates the default settings profile in the database with all of the new fields.
	 * Does not update name, prim id, or type, which are immutable
	 * @param settings
	 * @return
	 */
	public static boolean updateDefaultSettings(DefaultSettings settings) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateDefaultSettings(?, ?, ?, ?, ?, ?,?,?,?,?)}");
			procedure.setObject(1, settings.getPostProcessorId());
			procedure.setInt(2, settings.getCpuTimeout());
			procedure.setInt(3, settings.getWallclockTimeout());
			procedure.setBoolean(4, settings.isDependenciesEnabled());
			procedure.setObject(5, settings.getBenchId());
			procedure.setLong(6,settings.getMaxMemory()); //memory initialized to 1 gigabyte
			procedure.setObject(7,settings.getSolverId());
			procedure.setObject(8, settings.getBenchProcessorId());
			procedure.setObject(9,settings.getPreProcessorId());
			procedure.setInt(10,settings.getId());
			procedure.executeUpdate();			

			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Given an open ResultSet currently pointing to a row containing a DefaultSettings object,
	 * returns the object
	 * @param results
	 * @return
	 */
	protected static DefaultSettings resultsToSettings(ResultSet results) throws SQLException {
		final String methodName = "resultsToSettings";
		try {
			DefaultSettings settings=new DefaultSettings();
			settings.setId(results.getInt("id"));
			settings.setPreProcessorId(results.getInt("pre_processor"));
			if (results.wasNull()) {
				settings.setPreProcessorId(null);
			}
			settings.setWallclockTimeout(results.getInt("clock_timeout"));
			settings.setCpuTimeout(results.getInt("cpu_timeout"));
			settings.setPostProcessorId(results.getInt("post_processor"));
			if (results.wasNull()) {
				settings.setPostProcessorId(null);
			}
			settings.setDependenciesEnabled(results.getBoolean("dependencies_enabled"));
			settings.setBenchId(results.getInt("default_benchmark"));
			if (results.wasNull()) {
				settings.setBenchId(null);
			}
			settings.setSolverId(results.getInt("default_solver"));
			if (results.wasNull()) {
				settings.setSolverId(null);
			}
			settings.setBenchProcessorId(results.getInt("bench_processor"));
			if (results.wasNull()) {
				settings.setBenchProcessorId(null);
			}
			settings.setMaxMemory(results.getLong("maximum_memory"));
			settings.setName(results.getString("name"));
			settings.setPrimId(results.getInt("prim_id"));
			settings.setType(results.getInt("setting_type"));
			return settings;
		} catch (SQLException e) {
			logUtil.error(methodName, "Caught SQL exception while getting results. Throwing...",e);
			throw e;
		}
	}
	
	/**
	 * Given the ID of a primitive and the type of that primitive (user or community), 
	 * returns all of the defaultsettings profiles associated with that primitive
	 * @param id
	 * @param type
	 * @return
	 */
	public static List<DefaultSettings> getDefaultSettingsByPrimIdAndType(int id, SettingType type) {

		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		
		try {
			List<DefaultSettings> settings=new ArrayList<DefaultSettings>();
			con=Common.getConnection();
		    procedure = con.prepareCall("{CALL GetDefaultSettingsByIdAndType(?,?)}");
		    procedure.setInt(1, id);
		    procedure.setInt(2, type.getValue());
			results=procedure.executeQuery();
			while (results.next()) {
				settings.add(resultsToSettings(results));
			}
			return settings;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null; //error;
	}
	/**
	 * Returns every DefaultSettings profile a user has access to, either by community
	 * or individually
	 * @param userId
	 * @return
	 */
	public static List<DefaultSettings>getDefaultSettingsVisibleByUser(int userId) {
		List<DefaultSettings> listOfDefaultSettings=new ArrayList<DefaultSettings>();
		List<Space> comms=Communities.getAllCommunitiesUserIsIn(userId);
		if (comms.size()>0) {
			for (int i=0;i<comms.size();i++) {
				DefaultSettings s=Communities.getDefaultSettings(comms.get(i).getId());
				listOfDefaultSettings.add(s);

			}
		}
		List<DefaultSettings> userSettings=Settings.getDefaultSettingsOwnedByUser(userId);
		if (userSettings!=null) {
			listOfDefaultSettings.addAll(userSettings);
		}
		return listOfDefaultSettings;
	}

	public static List<DefaultSettings>getDefaultSettingsVisibleByUser(Connection con, int userId) {
		List<DefaultSettings> listOfDefaultSettings=new ArrayList<DefaultSettings>();
		List<Space> comms=Communities.getAllCommunitiesUserIsIn(con, userId);
		if (comms.size()>0) {
			for (int i=0;i<comms.size();i++) {
				DefaultSettings s=Communities.getDefaultSettings(con, comms.get(i).getId());
				listOfDefaultSettings.add(s);

			}
		}
		List<DefaultSettings> userSettings=Settings.getDefaultSettingsOwnedByUser(userId);
		if (userSettings!=null) {
			listOfDefaultSettings.addAll(userSettings);
		}
		return listOfDefaultSettings;
	}
	
	/**
	 * Gets all of the defaultSettings profiles that this user has
	 * @param userId
	 * @return
	 */
	public static List<DefaultSettings> getDefaultSettingsOwnedByUser(int userId) {
		return getDefaultSettingsByPrimIdAndType(userId, SettingType.USER);
	}
	
	/**
	 * Checks whether the given user has access to the given solver through a settings profile
	 * @param userId
	 * @param solverId
	 * @return
	 */
	public static boolean canUserSeeSolverInSettings(int userId, int solverId) {
		final String methodName = "canUserSeeSolverInSettings";
		Connection con = null;
		try {
			con = Common.getConnection();
			return canUserSeeSolverInSettings(con, userId, solverId);
		} catch (Exception e) {
			logUtil.logException(methodName, e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	public static boolean canUserSeeSolverInSettings(Connection con, int userId, int solverId) {
		List<DefaultSettings> settings=Settings.getDefaultSettingsVisibleByUser(con, userId);
		for (DefaultSettings s : settings) {
			if (s.getSolverId()==null) {
				continue;
			}
			if (s.getSolverId()==solverId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the given user has access to the given benchmark through a settings profile
	 * @param userId
	 * @param benchId
	 * @return
	 */
	public static boolean canUserSeeBenchmarkInSettings(int userId, int benchId) {
		List<DefaultSettings> settings=Settings.getDefaultSettingsVisibleByUser(userId);
		for (DefaultSettings s : settings) {
			if (s.getBenchId()==null) {
				continue;
			}
			if (s.getBenchId()==benchId) {
				return true;
			}
		}
		return false;
	} 
	
	/**
	 * Deletes the DefaultSettings profile with the given ID
	 * @param id 
	 * @return True on success and false otherwise
	 */
	public static boolean deleteProfile(int id) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL DeleteDefaultSettings(?)}");
			procedure.setInt(1,id);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false; //error;
	}
	
	/**
	 * Gets the DefaultSettings profile with the given id
	 * @param id 
	 * @return
	 */
	public static DefaultSettings getProfileById(int id) throws SQLException {
		final String methodName = "getProfileById";
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL getProfileById(?)}");
			procedure.setInt(1,id);
			results=procedure.executeQuery();
			if (results.next()) {
				return resultsToSettings(results);
			}
		} catch (SQLException e) {
			logUtil.error(methodName, "SQLException caught while querying database.", e);
			throw e;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	
	
	/**
	 * Updates the maximum memory setting of the given settings object
	 * @param id
	 * @param bytes
	 * @return
	 */
	public static boolean setDefaultMaxMemory(int id, long bytes) {
		Connection con = null;	
		CallableStatement procedure= null;
		try {			
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL SetMaximumMemorySetting(?, ?)}");
			procedure.setInt(1, id);
			procedure.setLong(2, bytes);
			procedure.executeUpdate();
		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return true;
	}
	
	/**
	 * Sets the default settings profile for a given user. This is the profile
	 * that will show up initially on job creation
	 * @param userId The ID of the user having their default updated
	 * @param settingId The setting ID to use
	 * @return True on success and false on error
	 */
	public static boolean setDefaultProfileForUser(int userId, int settingId) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL SetDefaultProfileForUser(?,?)}");
			procedure.setInt(1,userId);
			procedure.setInt(2,settingId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Gets the default settings profile for a given user. This is the profile
	 * that will show up initially on job creation
	 * @param userId 
	 * @return The id of the settings profile a user has as their default, OR null
	 *  	   if the user has no default settings profile. -1 is returned on error
	 */
	public static Integer getDefaultProfileForUser(int userId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetDefaultProfileForUser(?)}");
			procedure.setInt(1,userId);
			results=procedure.executeQuery();
			if (results.next()) {
				int result=results.getInt("default_settings_profile");
				//a value of 0 means the field is null in SQL
				if (result==0) {
					return null;
				}
				return result;
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}
	
	
	/**
	 * Set the default settings for a community given by the id.
	 * @param id The ID of the DefaultSettings object
	 * @param num Indicates which attribute needs to be set
	 * 1 = post_processor_id
	 * 2 = cpu_timeout
	 * 3 = wallclock_timeout
	 * 4 = dependencies_enabled
	 * 5 = default_benchmark_id
	 * 6 = pre_processor_id
	 * 7 = default_solver_id
	 * 8 = bench_processor_id
	 * @param setting The new value of the setting
	 * @return True if the operation is successful
	 * @author Ruoyu Zhang
	 */
	public static boolean updateSettingsProfile(int id, int num, long setting) {
		Connection con = null;	
		CallableStatement procedure= null;
		try {			
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL SetDefaultSettingsById(?, ?, ?)}");
			procedure.setInt(1, id);
			procedure.setInt(2, num);
			//if we are setting one of the IDs and it is -1, this means there is no setting
			//and we should use null
			if ((num==1 || num==5 || num==6 || num==7 || num==8) && setting==-1) {
				procedure.setObject(3,null);
			} else {
				procedure.setInt(3,(int)setting);
			}			
			
			procedure.executeUpdate();
			return true;
		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}
}
