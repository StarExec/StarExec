package org.starexec.data.database;

import org.apache.log4j.Logger;
import org.starexec.data.to.DefaultSettings;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
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
}
