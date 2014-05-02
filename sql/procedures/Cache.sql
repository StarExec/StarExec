-- Description: This file contains all cache-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Returns the path to the cached file for the given space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetCachePath;
CREATE PROCEDURE GetCachePath(IN _id INT, IN _cacheType INT, IN _time TIMESTAMP)
	BEGIN
		UPDATE file_cache SET last_access = _time WHERE id=_id AND cache_type=_cacheType;
		SELECT path FROM file_cache WHERE id=_id AND cache_type=_cacheType;
	END //

-- Adds a new cache entry
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS AddCachePath;
CREATE PROCEDURE AddCachePath(IN _id INT, IN _cacheType INT, IN _path TEXT, IN _time TIMESTAMP)
	BEGIN
		INSERT IGNORE INTO file_cache (id, cache_type, path, last_access) VALUES (_id,_cacheType, _path, _time);
	END //
	
-- Get all entries in the cache that have not been accessed since before _time
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetOldCachePaths;
CREATE PROCEDURE GetOldCachePaths(IN _time TIMESTAMP) 
	BEGIN
		SELECT path FROM file_cache WHERE last_access<_time;
	END //
-- Removes all entries in the cache table that have not been accessed since before _time
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS DeleteOldCachePaths;
CREATE PROCEDURE DeleteOldCachePaths(IN _time TIMESTAMP)
	BEGIN
		DELETE FROM file_cache WHERE last_access<_time;
	END //
-- Removes a cache entry 
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS InvalidateCache;
CREATE PROCEDURE InvalidateCache(IN _id INT, IN _cacheType INT) 
	BEGIN
		DELETE FROM file_cache WHERE id=_id AND cache_type=_cacheType;
	END //

-- Removes all entries of the given type
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS DeleteCachePathsOfType;
CREATE PROCEDURE DeleteCachePathsOfType(IN _type INT)
	BEGIN
		DELETE FROM file_cache WHERE cache_type=_type;
	END //
	
-- Gets all cached entries of the given type
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetPathsOfType;
CREATE PROCEDURE GetPathsOfType(IN _type INT) 
	BEGIN
		SELECT path FROM file_cache WHERE cache_type=_type;
	END //
DELIMITER ; -- This should always be at the end of this file