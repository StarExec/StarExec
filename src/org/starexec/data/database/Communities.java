package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Space;

/**
 * Handles all database interaction for communities (closely tied with the Spaces class)
 * @see Spaces
 */
public class Communities {
	private static final Logger log = Logger.getLogger(Communities.class);
	
	/**
	 * @return A list of child spaces belonging to the root space (community spaces)
	 * @author Todd Elvers
	 */
	public static List<Space> getAll() {
		Connection con = null;			
		CallableStatement procedure = null;		
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetSubSpacesOfRoot}");
			results = procedure.executeQuery();
			List<Space> commSpaces = new LinkedList<Space>();
			
			while(results.next()){
				Space s = new Space();
				s.setName(results.getString("name"));
				s.setId(results.getInt("id"));
				s.setDescription(results.getString("description"));
				s.setLocked(results.getBoolean("locked"));				
				commSpaces.add(s);
			}			
						
			return commSpaces;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(results);
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return null;
	}
	
	public static List<Space> getCommsWithPublicSolvers(){
		List<Space> commSpaces = Communities.getAll();
		List<Space> returnSpaces = new LinkedList<Space>();	
		for (Space comm:commSpaces){
			if (Solvers.getPublicSolversByCommunity(comm.getId()).size() > 0){
				returnSpaces.add(comm);
			}	
		}	
		return returnSpaces;
	}
	
	/**
	 * Gets a space with minimal information (only details about the space itself)
	 * @param spaceId The id of the space to get information for
	 * @param userId The id of the user requesting the space (used for permissions check)
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
			
			if(results.next()){
				Space s = new Space();
				s.setName(results.getString("name"));
				s.setId(results.getInt("id"));
				s.setDescription(results.getString("description"));
				s.setLocked(results.getBoolean("locked"));
				s.setCreated(results.getTimestamp("created"));	
				return s;
			}			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(results);
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Removes a user's association to a given space, thereby leaving that space
	 * @param userId the id of the user to remove from the space
	 * @param commId the id of the space to remove the user from
	 * @return true iff the user was successfully removed from the community referenced by 'commId',
	 * false otherwise
	 * @author Todd Elvers
	 */
	public static boolean leave(int userId, int commId) {
		if (userId==R.PUBLIC_USER_ID){
			return false;
		}
		
		Connection con = null;			
		CallableStatement procedure = null;		
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL LeaveCommunity(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setInt(2, commId);
			
			procedure.executeUpdate();			
			log.debug(String.format("User [id=%d] successfully left community [id=%d].", userId, commId));
			return true;
		} catch (Exception e){			
		    log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		log.debug(String.format("User [id=%d] failed to leave community [id=%d].", userId, commId));
		return false;
	
	}

	
	/**
	 * Checks to see if the space with the given space ID is a community or not
	 * (the space is a child of the root space)
	 * 
	 * @param spaceId the ID of the space to check
	 * @return true iff the space is a community
	 * @author Skylar Stark
	 */
	public static boolean isCommunity(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet result = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL IsCommunity(?)}");
			procedure.setInt(1, spaceId);

			result = procedure.executeQuery();

			return result.next();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
		    Common.safeClose(result);
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}

		return false;
	}
	
	/**
	 * Get the default setting of the community given by the id.
	 * 
	 * @param id the space id of the community
	 * @return a list of string containing the default settings
	 * @author Ruoyu Zhang
	 */
	public static List<String> getDefaultSettings(int id) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		List<String> listOfDefaultSettings = Arrays.asList("id","no_type","1","1","0","0","0");
		
		try {			
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetCommunityOfSpace(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();
			
			if (results.next()) {
				procedure = con.prepareCall("{CALL GetSpaceDefaultSettingsById(?)}");
				procedure.setInt(1, results.getInt("community"));
				results = procedure.executeQuery();
			}
			
			if(results.next()){
				listOfDefaultSettings.set(0, results.getString("space_id"));
				listOfDefaultSettings.set(1, results.getString("name"));
				listOfDefaultSettings.set(2, results.getString("cpu_timeout"));
				listOfDefaultSettings.set(3, results.getString("clock_timeout"));
				listOfDefaultSettings.set(4, results.getString("post_processor"));
				listOfDefaultSettings.set(5, results.getString("dependencies_enabled"));
				listOfDefaultSettings.set(6, results.getString("default_benchmark"));
			}
			else {
				procedure = con.prepareCall("{CALL InitSpaceDefaultSettingsById(?, ?, ?, ?, ?, ?)}");
				procedure.setInt(1, id);
				procedure.setInt(2, 1);
				procedure.setInt(3, 1);
				procedure.setInt(4, 1);
				procedure.setInt(5, 0);
				procedure.setObject(6, null);
				procedure.executeUpdate();
			}
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(results);
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return listOfDefaultSettings;
	}
	
	/**
	 * Set the default settings for a community given by the id.
	 * @param id The space id of the community
	 * @param num Indicates which attribute needs to be set
	 * @param setting The new value of the setting
	 * @return True if the operation is successful
	 * @author Ruoyu Zhang
	 */
	public static boolean setDefaultSettings(int id, int num, int setting) {
		Connection con = null;			
		CallableStatement procedure = null;
		try {			
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL SetSpaceDefaultSettingsById(?, ?, ?)}");
			procedure.setInt(1, id);
			procedure.setInt(2, num);
			if ((num==1 || num==5) && setting==-1) {
				procedure.setObject(3,null);
			} else {
				procedure.setInt(3, setting);
			}
			
			
			procedure.executeUpdate();

		
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		
		return true;
	}
}