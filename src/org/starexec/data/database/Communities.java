package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.DefaultSettings.SettingType;
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
		CallableStatement procedure= null;
		ResultSet results=null;
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
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
	}


    public static boolean commAssocExpired(){
		long timeNow = System.currentTimeMillis();
	
		if(R.COMM_ASSOC_LAST_UPDATE == null){
	
		    return true;
		}
	
		
		Long timeElapsed = timeNow - R.COMM_ASSOC_LAST_UPDATE;
	
		log.info("timeElapsed since last comm_assoc update: " + timeElapsed);
		if(timeElapsed > R.COMM_ASSOC_UPDATE_PERIOD){
		    
		    return true;
		}
		
		log.info("not yet expired");
		return false;

    }

    public static void dropCommunityAssoc(){
	    Connection con = null;			
		CallableStatement procedure= null;
		try {
		    con = Common.getConnection();
			
		    procedure = con.prepareCall("{CALL DropCommunityAssoc()}");

		    procedure.executeQuery();

		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
    }
    public static void updateCommunityAssoc(){

	    Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
		    
			log.info("updated comm_assoc");
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL UpdateCommunityAssoc()}");

			results = procedure.executeQuery();
			while(results.next()){
			    //TODO: What goes in this block?
			}
		    
		    
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

    }

    /**
     *Helper function for updateCommunityMapIf
     * @return A HashMap mapping keys for primitives to 0
     *
     **/
    public static HashMap<String,Long> initializeCommInfo(){
		HashMap<String,Long> stats = new HashMap<String, Long>();
	
		stats.put("users",new Long(0));
		stats.put("jobs",new Long(0));
		stats.put("benchmarks",new Long(0));
		stats.put("solvers",new Long(0));
		stats.put("job_pairs", new Long(0));
		stats.put("disk_usage",new Long(0));
	
		return stats;

    }

    /**
     * retrieves information for community stats page if current information has expired
     * @author Julio Cervantes
     **/
    public synchronized static void updateCommunityMapIf(){
	    Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
		    if(commAssocExpired()){
				List<Space> communities = Communities.getAll();
		    
	
				HashMap<Integer,HashMap<String,Long>> commInfo = new HashMap<Integer,HashMap<String,Long>>();

				for(Space c : communities){
				    commInfo.put(c.getId(),initializeCommInfo());
				}

		        updateCommunityAssoc();

				Integer commId;
				Long infoCount, infoExtra;
			
			
			
				con = Common.getConnection();
	
				procedure = con.prepareCall("{CALL GetCommunityStatsUsers()}");
				results = procedure.executeQuery();
				
				while(results.next()){
				    
				    commId = results.getInt("comm_id");
				    infoCount = results.getLong("userCount");
	
				    commInfo.get(commId).put("users",infoCount);
					
				    log.info("commId: " + commId + " | userCount: " + infoCount);
				}
	
				Common.safeClose(con);
				Common.safeClose(results);
				Common.safeClose(procedure);
			

			
				con = Common.getConnection();
	
				procedure = con.prepareCall("{CALL GetCommunityStatsSolvers()}");
				results = procedure.executeQuery();
				
				while(results.next()){
	
				    commId = results.getInt("comm_id");
				    infoCount = results.getLong("solverCount");
				    infoExtra = results.getLong("solverDiskUsage");
	
				    commInfo.get(commId).put("solvers",infoCount);
				    commInfo.get(commId).put("disk_usage",commInfo.get(commId).get("disk_usage") + infoExtra);
	
				    log.info("commId: " + commId + " | solverCount: " + infoCount + " | solverDisk: " + infoExtra);
				    
				}
	
				Common.safeClose(con);
				Common.safeClose(results);
				Common.safeClose(procedure);

				
				con = Common.getConnection();
	
				procedure = con.prepareCall("{CALL GetCommunityStatsBenches()}");
				results = procedure.executeQuery();
				
				while(results.next()){
	
				    commId = results.getInt("comm_id");
				    infoCount = results.getLong("benchCount");
				    infoExtra = results.getLong("benchDiskUsage");
	
				    commInfo.get(commId).put("benchmarks",infoCount);
				    commInfo.get(commId).put("disk_usage",commInfo.get(commId).get("disk_usage") + infoExtra);
					
					
				    log.info("commId: " + commId + " | benchCount: " + infoCount + " | benchDisk: " + infoExtra);
				    
				}
	
				Common.safeClose(con);
				Common.safeClose(results);
				Common.safeClose(procedure);
			
	
				con = Common.getConnection();
	
				procedure = con.prepareCall("{CALL GetCommunityStatsJobs()}");
				results = procedure.executeQuery();
				
				while(results.next()){
	
				    commId = results.getInt("comm_id");
				    infoCount = results.getLong("jobCount");
				    infoExtra = results.getLong("jobPairCount");
	
				    commInfo.get(commId).put("jobs",infoCount);
				    commInfo.get(commId).put("job_pairs",infoExtra);
					
				    log.info("commId: " + commId + " | jobCount: " + infoCount + " | jobPairCount: " + infoExtra);
				    
				}
	
				
	
				
				dropCommunityAssoc();
	
				R.COMM_INFO_MAP = commInfo;
				R.COMM_ASSOC_LAST_UPDATE = System.currentTimeMillis();
			 }
		} catch (Exception e){			
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
    }

  
 
	
	public static int getDefaultCpuTimeout(int id) {
		return getDefaultSettings(id).getCpuTimeout();
	}
	
	public static int getDefaultWallclockTimeout(int id) {
		return getDefaultSettings(id).getWallclockTimeout();
	}
	
	public static int getDefaultPostProcessorId(int id) {
		return getDefaultSettings(id).getPostProcessorId();
	}
	
	public static long getDefaultMaxMemory(int id) {
		return getDefaultSettings(id).getMaxMemory();
		
	}
	
	/**
	 * Given a DefaultSettings object with all of its fields yet, adds the
	 * settings object to the database
	 * @param d
	 * @return
	 */
	
	public static int createNewDefaultSettings(DefaultSettings d) {
		d.setType(SettingType.COMMUNITY);
		return Settings.addNewSettingsProfile(d);
	}
	
	
	/**
	 * Get the default setting of the community given by the id.
	 * If the default settings do not already exist, they are initialized to default values and returned
	 * 
	 * @param id the space id of the community
	 * @return DefaultSettings object 
	 * @author Ruoyu Zhang
	 */
	public static DefaultSettings getDefaultSettings(int id) {
		Connection con = null;			
		CallableStatement procedure= null;
		ResultSet results=null;
		//if the current space is the root, we just want to return the default profile
		if (id==1) {
			return new DefaultSettings();
		}
		try {			
			//first, find the ID of the community this space is a part of
			con = Common.getConnection();
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
			    if (community<=0) {
			    	log.debug("no default settings profile set for space = "+id);
			    	return null;
			    }
			    
			    List<DefaultSettings> settings=Settings.getDefaultSettingsByPrimIdAndType(community, SettingType.COMMUNITY);
				
				if(settings.size()>0){
					return settings.get(0);
				}
				else {
					//no settings existed, so create one for this community and return that
					log.debug("unable to find any default settings for community id = "+community);
					DefaultSettings d=new DefaultSettings();
					String name=Spaces.getName(community);
					if (name.length()>R.SETTINGS_NAME_LEN) {
						name=name.substring(0,R.SETTINGS_NAME_LEN); //make sure it isn't too large
					}
					d.setName(name);
					d.setPrimId(community);
					log.debug("calling createNewDefaultSettings on community with id = "+community);
				    int newId=createNewDefaultSettings(d);
				    if (newId>0) {
					    return d;

				    } else {
				    	//failed to create new profile
				    	log.error("error creating new default settings profile");
				    	return null;
				    }
				}
			   
			} else {
				log.error("We were unable to find the community for the space ="+id);
				return null;
			}

		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
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
		CallableStatement procedure= null;
		ResultSet results=null;
		try {			
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetCommunityById(?)}");
			procedure.setInt(1, id);					
			results = procedure.executeQuery();		
			
			if(results.next()){
				Space s = new Space();
				s.setName(results.getString("space.name"));
				s.setId(results.getInt("space.id"));
				s.setDescription(results.getString("space.description"));
				s.setLocked(results.getBoolean("space.locked"));
				s.setCreated(results.getTimestamp("space.created"));	
				return s;
			}			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return null;
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
		CallableStatement procedure= null;
		ResultSet results=null;
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
	
	
	
	public static Space getTestCommunity() {
		Space s=Communities.getDetails(R.TEST_COMMUNITY_ID);
		if (s==null) {
			log.warn("getTestCommunity could not retrieve the test community--please set one up in the configuration");
		}
		return s;
	}
}