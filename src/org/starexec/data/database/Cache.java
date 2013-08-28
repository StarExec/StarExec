package org.starexec.data.database;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.CacheType;

public class Cache {
	private static final Logger log = Logger.getLogger(Spaces.class);

	/**
	 * Deletes the cached file associated with the given id and type. Does nothing if it does not exist
	 * @param id The ID of the primitive in question
	 * @param the type of the cache, which indicates the type of the primitive (solver, space, benchmark, job)
	 * @return True on success (file was deleted or does not exist), false on error
	 * @author Eric Burns
	 */
	private static boolean deleteCache(int id, CacheType type) {
		try {
			String filePath=Cache.getCache(id, type);
			if (filePath!=null) {
				File cacheFile=new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR + File.separator), filePath);
				if (cacheFile.exists()) {
					cacheFile.delete();
				}
			}
			return true;
		} catch (Exception e) {
			log.debug("deleteCache says "+e.getMessage());
		} 
		return false;
	}
	
	
	/**
	 * Invalidates the cache for the given space by removing its entry in the 
	 * file_cache table. If there is no entry in the table, this does nothing
	 * @param id The ID of the primitive in question
	 * @param the type of the cache, which indicates the type of the primitive (solver, space, benchmark, job)
	 * @param con The open connection to make the call on
	 * @return True if the call was successful, false otherwise
	 * @author Eric Burns
	 */
	private static boolean invalidateCache(int id, CacheType type, Connection con) {
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("{CALL InvalidateSpaceCache(?, ?)}");
			procedure.setInt(1,id);
			procedure.setInt(2,type.getVal());
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.debug("invalidateCache says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		
		
		return false;
	}
	
	
	
	/**
	 * Invalidates the cache for the hierarchy at the given space ID. Also deletes the cached files on disk 
	 * Also invalidates the cache of every ancestor of this space
	 * @param id The ID of the space which is having its cache invalidated
	 * @param CacheType -- the type of the cached file we are removing
	 * @return True if the invalidation was successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean invalidateCache(int id, CacheType type) {
		log.debug("invalidating cache for id = "+id+" type = "+type.toString());
		Connection con=null;
		Cache.deleteCache(id, type);
		try {
			con=Common.getConnection();
			boolean success=invalidateCache(id,type,con);
			if (!success) {
				return false;
			}
			//if we were deleting the cache of a space, we need to delete the cache of
			//the space hierarchy for this and  every ancestor space
			if (type==CacheType.CACHE_SPACE) {
				int spaceId=id;
				//invalidate up to the root space
				while (spaceId>1) {
					Cache.invalidateCache(spaceId, CacheType.CACHE_SPACE_HIERARCHY);
					int ancestorSpaceId=Spaces.getParentSpace(spaceId);
					if (ancestorSpaceId==spaceId) {
						break;
					}
					spaceId=ancestorSpaceId;
				}
			}
			return true;
		} catch (Exception e) {
			log.debug("invalidateCache says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Returns the relative path to a cached space directory
	 * @param id The ID of the primitive in question
	 * @param the type of the cache, which indicates the type of the primitive (solver, space, benchmark, job)
	 * @return The String path on success, null if there is no path or if there was an error
	 * @author Eric Burns
	 */
	
	public static String getCache(int id, CacheType type) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetCachePath(?, ?, ?)}");
			procedure.setInt(1,id);
			procedure.setInt(2,type.getVal());
			procedure.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			results= procedure.executeQuery();
			if (results.next()) {
				return results.getString("path");
			}
		} catch (Exception e) {
			log.debug("Spaces.setCache says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Deletes all the paths to cached items that have not been accesed in the past 
	 * <daysSinceLastAccess> days
	 * @param daysSinceLastAccess The number of days since the last access of a file we consider "old"
	 * @return True if the operation was successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean deleteOldPaths(int daysSinceLastAccess) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL DeleteOldCachePaths(?)}");
			//get current time minus the number of days to get the time before which files are considered "old"
			procedure.setTimestamp(1, new Timestamp(System.currentTimeMillis()-(TimeUnit.MILLISECONDS.convert(daysSinceLastAccess, TimeUnit.DAYS))));
			procedure.executeQuery();
			return true;
		} catch (Exception e) {
			log.debug("getOldPaths says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	
	/**
	 * Gets all the paths to cached items that have not been accesed in the past 
	 * <daysSinceLastAccess> days
	 * @param daysSinceLastAccess The number of days since the last access of a file we consider "old"
	 * @return A list of strings, each string representing a path relative to R.CACHED_FILE_DIR
	 * @author Eric Burns
	 */
	public static List<String> getOldPaths(int daysSinceLastAccess) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetOldCachePaths(?)}");
			//get current time minus the number of days to get the time before which files are considered "old"
			procedure.setTimestamp(1, new Timestamp(System.currentTimeMillis()-(TimeUnit.MILLISECONDS.convert(daysSinceLastAccess, TimeUnit.DAYS))));
			results=procedure.executeQuery();
			List<String> paths=new ArrayList<String>();
			while (results.next()) {
				paths.add(results.getString("path"));
			}
			return paths;
		} catch (Exception e) {
			log.debug("getOldPaths says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	/**
	 * Adds a new entry into the file_cache table containing the path to a cached 
	 * space archive
	 * @param id The ID of the primitive in question
	 * @param the type of the cache, which indicates the type of the primitive (solver, space, benchmark, job)
	 * @param path The relative filepath to the file (not containing R.DOWNLOAD_FILE_DIR)
	 * @return True if the update was successful, false otherwise
	 * @author Eric Burns
	 */
	
	public static boolean setCache(int id, CacheType type, String path) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL AddSpaceCache(?,?,?)}");
			procedure.setInt(1,id);
			procedure.setInt(2,type.getVal());
			procedure.setString(3,path);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.debug("Spaces.setCache says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Invalidates the cache of every space associated with this benchmark
	 * @param benchId The benchmark id in question
	 * @return true on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean invalidateSpacesAssociatedWithBench(int benchId) {
		try {
			List<Integer> spaceIds=Benchmarks.getAssociatedSpaceIds(benchId);
			for (int spaceId : spaceIds) {
				Cache.invalidateCache(spaceId, CacheType.CACHE_SPACE);
			}
			return true;
		} catch (Exception e) {
			log.error("Benchmarks.invalidateAssociatedSpaces says "+e.getMessage(),e);
		}
		return false;
	}
	
	/**
	 * Invalidates the cache of every space associated with this solver
	 * @param solverId  The ID of the solver in question
	 * @return True if the invalidation was successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean invalidateSpacesAssociatedWithSolver(int solverId) {
		try {
			List<Integer> spaceIds=Solvers.getAssociatedSpaceIds(solverId);
			for (int spaceId : spaceIds) {
				Cache.invalidateCache(spaceId, CacheType.CACHE_SPACE);
			}
			return true;
		} catch (Exception e) {
			log.debug("invalidateAssociatedSpaces says "+e.getMessage(),e);
		} 
		return false;
		
	}
	
}
