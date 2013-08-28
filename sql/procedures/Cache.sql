-- Description: This file contains all cache-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Returns the path to the cached file for the given space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetCachePath;
CREATE PROCEDURE GetSpaceCache(IN _id INT, IN _cacheType INT, IN _time TIMESTAMP)
	BEGIN
		UPDATE file_cache SET last_access = _time WHERE space_id=_id AND cache_type=_cacheType;
		SELECT path FROM file_cache WHERE space_id=_id AND cache_type=_cacheType;
	END //

-- Adds a new cache entry
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS AddCachePath;
CREATE PROCEDURE AddSpaceCache(IN _id INT, IN _cacheType INT, IN _path TEXT)
	BEGIN
		INSERT IGNORE INTO file_cache (space_id, cache_type, path) VALUES (_id,cache_type, _path);
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
		DELETE FROM file_cache WHERE last_acces<_time;
	END //
-- Removes a cache entry 
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS InvalidateCache;
CREATE PROCEDURE InvalidateSpaceCache(IN _id INT, IN _cacheType INT) 
	BEGIN
		DELETE FROM file_cache WHERE space_id=_id AND cache_type=_cacheType;
	END //

DELIMITER ; -- This should always be at the end of this file