package org.starexec.data.database;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
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
	private static boolean deleteCacheFile(int id, CacheType type) {
		try {
			String filePath=Cache.getCache(id, type);
			if (filePath!=null) {
				File cacheFile=new File(filePath);
				if (cacheFile.exists()) {
					cacheFile.delete();
				} else {
					log.debug("deleteCacheFile tried to delete cached file with id = "+id+" and type = "+type.getVal()+", but it did" +
							"not exist");
				}
			}
			return true;
		} catch (Exception e) {
			log.debug("deleteCache says "+e.getMessage());
		} 
		return false;
	}
	/**
	 * Clears the entire cache
	 * @return True on success, and false otherwise
	 */
	public static boolean deleteAllCache() {
		return deleteOldPaths(0);
	}
	
	
	/**
	 * Deletes all the paths to cached items that have not been accessed in the past 
	 * <daysSinceLastAccess> days
	 * @param daysSinceLastAccess The number of days since the last access of a file we consider "old"
	 * @return True if the operation was successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean deleteOldPaths(int daysSinceLastAccess) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			//first, get all the old paths so we can delete them from disk
			List<String> paths=Cache.getOldPaths(daysSinceLastAccess);
			//first, remove the files on disk
			for (String path : paths) {
				File file=new File(path);
				if (file.exists()) {
					file.delete();
				}
			}
			//then, remove everything from the database
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL DeleteOldCachePaths(?)}");
			//get current time minus the number of days to get the time before which files are considered "old"
			procedure.setTimestamp(1, new Timestamp(System.currentTimeMillis()-(TimeUnit.MILLISECONDS.convert(daysSinceLastAccess, TimeUnit.DAYS))));
			procedure.executeQuery();
			return true;
		} catch (Exception e) {
			log.debug("deleteOldPaths says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	
	/**
	 * Deletes all the paths to cached items that have not been accessed in the past 
	 * <daysSinceLastAccess> days
	 * @param daysSinceLastAccess The number of days since the last access of a file we consider "old"
	 * @return True if the operation was successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean deleteCacheOfType(CacheType type) {
		Connection con=null;
		CallableStatement procedure=null;
		try {
			//first, get all the old paths so we can delete them from disk
			List<String> paths=Cache.getPathsOfType(type);
			//first, remove the files on disk
			for (String path : paths) {
				File file=new File(path);
				if (file.exists()) {
					file.delete();
				}
			}
			//then, remove everything from the database
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL DeleteCachePathsOfType(?)}");
			//get current time minus the number of days to get the time before which files are considered "old"
			procedure.setInt(1, type.getVal());
			procedure.executeQuery();
			return true;
		} catch (Exception e) {
			log.debug("deleteCacheOfType says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Gets all the absolute paths to cached items that are of the given type
	 * @param type The type of the cached items to retrieve
	 * @return A list of strings, each string representing an absolute path
	 * @author Eric Burns
	 */
	public static List<String> getPathsOfType(CacheType type) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetPathsOfType(?)}");
			procedure.setInt(1, type.getVal());
			results=procedure.executeQuery();
			List<String> paths=new ArrayList<String>();
			while (results.next()) {
				File f=convertNameToFile(results.getString("path"));
				paths.add(f.getAbsolutePath());
			}
			return paths;
		} catch (Exception e) {
			log.debug("getPathsOfType says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
	
	
	
	/**
	 * Returns the absolute path to a cached space directory. The path is guaranteed to point to a file that actually
	 * exists-- otherwise, null will be returned.
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
			log.debug("calling getCache for type = "+type.toString()+" "+type.getVal());
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetCachePath(?, ?, ?)}");
			procedure.setInt(1,id);
			procedure.setInt(2,type.getVal());
			procedure.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			results= procedure.executeQuery();
			if (results.next()) {
				log.debug("THE PATH OF THE CACHE IS "+results.getString("path"));
				File cachedFile = Cache.convertNameToFile(results.getString("path"));
				if (cachedFile.exists()) {
					return cachedFile.getAbsolutePath();
				} else {
					//file did not exist, so we should invalidate this part of the cache and return null
					Cache.invalidateCache(id, type,con);
					return null;
				}
							
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
	 * Gets all the absolute paths to cached items that have not been accessed in the past 
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
				File f=convertNameToFile(results.getString("path"));
				
				paths.add(f.getAbsolutePath());
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
	 * Invalidates the cache for the hierarchy at the given space ID. Also deletes the cached files on disk 
	 * Also invalidates the cache of every ancestor of this space
	 * @param id The ID of the space which is having its cache invalidated
	 * @param CacheType -- the type of the cached file we are removing
	 * @return True if the invalidation was successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean invalidateAndDeleteCache(int id, CacheType type) {
		log.debug("invalidating cache for id = "+id+" type = "+type.toString());
		Connection con=null;
		Cache.deleteCacheFile(id, type);
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
					Cache.invalidateAndDeleteCache(spaceId, CacheType.CACHE_SPACE_HIERARCHY);
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
			procedure=con.prepareCall("{CALL InvalidateCache(?, ?)}");
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
	 * Invalidates the cache of every space associated with this benchmark
	 * @param benchId The benchmark id in question
	 * @return true on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean invalidateSpacesAssociatedWithBench(int benchId) {
		try {
			List<Integer> spaceIds=Benchmarks.getAssociatedSpaceIds(benchId);
			for (int spaceId : spaceIds) {
				Cache.invalidateAndDeleteCache(spaceId, CacheType.CACHE_SPACE);
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
				Cache.invalidateAndDeleteCache(spaceId, CacheType.CACHE_SPACE);
			}
			return true;
		} catch (Exception e) {
			log.debug("invalidateAssociatedSpaces says "+e.getMessage(),e);
		} 
		return false;
		
	}
	
	/**
	 * Adds a new entry into the file_cache table, and also copies the given file to the cache directory.
	 * Invalid cache requests (for example, requests to cache results for an incomplete job or a space hierarchy
	 * including public spaces) do nothing.
	 * @param id The ID of the primitive in question
	 * @param the type of the cache, which indicates the type of the primitive (solver, space, benchmark, job)
	 * @param archive The archive that is being cached, which needs to be copied to R.CACHED_FILE_DIR
	 * @param destName The destination name of the file
	 * @return True if the update was successful, false otherwise
	 * @author Eric Burns
	 */
	
	public static boolean setCache(int id, CacheType type, File archive, String destName) {
		//if we have anything job related, we first need to make sure the 
		if (type==CacheType.CACHE_JOB_CSV || type==CacheType.CACHE_JOB_CSV_NO_IDS || type==CacheType.CACHE_JOB_OUTPUT ||
				type== CacheType.CACHE_JOB_PAIR) {
			int jobId;
			if (type==CacheType.CACHE_JOB_PAIR) {
				jobId=JobPairs.getPairDetailed(id).getJobId();
			} else {
				jobId=id;
			}
			if (!Jobs.isJobComplete(jobId)) {
				return true; // there were no errors, but we don't want to cache job related things before the job is complete
			}
		}
		if (type==CacheType.CACHE_SPACE_HIERARCHY || type==CacheType.CACHE_SPACE_XML) {
			if (!Spaces.isPublicHierarchy(id)) {
				log.debug("space hierarchy is not public, so no caching was done");
				return true; //don't cache anything for hierarchies that are not entirely public
			}
		}
		log.debug("adding entry to cache type = "+type.toString());
		Connection con=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL AddCachePath(?,?,?,?)}");
			procedure.setInt(1,id);
			procedure.setInt(2,type.getVal());
			procedure.setString(3,destName);
			procedure.setTimestamp(4,new Timestamp(System.currentTimeMillis()));
			procedure.executeUpdate();
			FileUtils.copyFileToDirectory(archive, new File(R.STAREXEC_ROOT, R.CACHED_FILE_DIR+File.separator));
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
	 * Given the name of a cached file from the database, returns a file object pointing to that file
	 * @param fileName The name of hte file
	 * @return
	 */
	private static File convertNameToFile(String fileName) {
		File cachedFile = new File(new File(R.STAREXEC_ROOT, R.CACHED_FILE_DIR + File.separator), fileName);
		return cachedFile;
	}
	
}
