package org.starexec.data.database;

import org.apache.log4j.Logger;
import org.starexec.data.to.DefaultSettings;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
public class Settings {
	private static Logger log=Logger.getLogger(Settings.class);
	protected static boolean addNewSettingsProfile(DefaultSettings settings, String type) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure = con.prepareCall("{CALL CreateDefaultSettings(?, ?, ?, ?, ?, ?,?,?,?,?,?,?)}");
			procedure.setInt(1, settings.getId());
			procedure.setObject(2, settings.getPostProcessorId());
			procedure.setInt(3, settings.getCpuTimeout());
			procedure.setInt(4, settings.getWallclockTimeout());
			procedure.setBoolean(5, settings.isDependenciesEnabled());
			procedure.setObject(6, settings.getBenchId());
			procedure.setLong(7,settings.getMaxMemory()); //memory initialized to 1 gigabyte
			procedure.setObject(8,settings.getSolverId());
			procedure.setObject(9, settings.getBenchProcessorId());
			procedure.setObject(10,settings.getPreProcessorId());
			procedure.setString(11, type);
			procedure.setString(12,settings.getName());
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
	public static DefaultSettings resultsToSettings(ResultSet results) {
		try {
			DefaultSettings settings=new DefaultSettings();
			settings.setId(results.getInt("id"));
			settings.setPreProcessorId(results.getInt("pre_processor"));
			settings.setWallclockTimeout(results.getInt("clock_timeout"));
			settings.setCpuTimeout(results.getInt("cpu_timeout"));
			settings.setPostProcessorId(results.getInt("post_processor"));
			settings.setDependenciesEnabled(results.getBoolean("dependencies_enabled"));
			settings.setBenchId(results.getInt("default_benchmark"));
			settings.setSolverId(results.getInt("default_solver"));
			settings.setBenchProcessorId(results.getInt("bench_processor"));
			settings.setMaxMemory(results.getLong("maximum_memory"));
			settings.setName(results.getString("name"));
			return settings;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
		
	}
	
	/**
	 * Gets all of the defaultSettings profiles that this user has
	 * @param userId
	 * @return
	 */
	public static List<DefaultSettings> getUserProfiles(int userId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		
		try {
			List<DefaultSettings> settings=new ArrayList<DefaultSettings>();
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL getProfilesByUser(?)}");
			procedure.setInt(1,userId);
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
	 * Gets the DefaultSettings profile for the given user with the given name
	 * @param userId
	 * @param name
	 * @return
	 */
	public static DefaultSettings getUserProfileByIdAndName(int userId, String name) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL getProfileByIdAndName(?,?,?)}");
			procedure.setInt(1,userId);
			procedure.setString(2, name);
			procedure.setString(3,"user");
			results=procedure.executeQuery();
			if (results.next()) {
				return resultsToSettings(results);
			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null; //error;
	}
}
