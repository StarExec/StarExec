package org.starexec.data.database;

import org.apache.log4j.Logger;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.DefaultSettings.SettingType;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
public class Settings {
	private static Logger log=Logger.getLogger(Settings.class);
	protected static boolean addNewSettingsProfile(DefaultSettings settings) {
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
			procedure.setInt(11, settings.getType().getValue());
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
			settings.setPrimId(results.getInt("prim_id"));
			settings.setType(results.getInt("setting_type"));
			return settings;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
		
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
	 * Gets all of the defaultSettings profiles that this user has
	 * @param userId
	 * @return
	 */
	public static List<DefaultSettings> getDefaultSettingsByUser(int userId) {
		return getDefaultSettingsByPrimIdAndType(userId, SettingType.USER);
	}
	
	/**
	 * Gets the DefaultSettings profile for the given user with the given name
	 * @param userId
	 * @param name
	 * @return
	 */
	public static DefaultSettings getProfileById(int id) {
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
			procedure = con.prepareCall("{CALL SetMaximumMemorySetting(?, ?, ? )}");
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
	 * Set the default settings for a community given by the id.
	 * @param id The space id of the community
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
	public static boolean setDefaultSettings(int id, int num, long setting) {
		Connection con = null;	
		CallableStatement procedure= null;
		try {			
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL SetDefaultSettingsById(?, ?, ?,?)}");
			procedure.setInt(1, id);
			procedure.setInt(2, num);
			//if we are setting one of the IDs and it is -1, this means there is no setting
			//and we should use null
			if ((num==1 || num==5) && setting==-1) {
				procedure.setObject(3,null);
			} else {
					procedure.setInt(3,(int)setting);
				
			}
			procedure.setString(4, "comm");
			
			
			procedure.executeUpdate();

		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return true;
	}
}
