-- Description: This file contains all benchmark stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement



-- Adds a benchmark into the system and associates it with a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddBenchmark;
CREATE PROCEDURE AddBenchmark(IN _name VARCHAR(256), IN _path TEXT, IN _downloadable TINYINT(1), IN _userId INT, IN _typeId INT, IN _spaceId INT, IN _diskSize BIGINT, OUT _benchId INT)
	BEGIN	
		INSERT INTO benchmarks (user_id, name, bench_type, uploaded, path, downloadable, disk_size)
		VALUES (_userId, _name, _typeId, SYSDATE(), _path, _downloadable, _diskSize);
		
		SELECT LAST_INSERT_ID() INTO _benchId;		
		INSERT INTO bench_assoc VALUES (_spaceId, _benchId);
	END //	
		
-- Adds a new attribute to a benchmark 
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddBenchAttr;
CREATE PROCEDURE AddBenchAttr(IN _benchmarkId INT, IN _key VARCHAR(128), IN _val VARCHAR(128))
	BEGIN
		INSERT INTO bench_attributes VALUES (_benchmarkId, _key, _val);
	END //
	
-- Adds a new dependency for a benchmark 
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS AddBenchDependency;
CREATE PROCEDURE AddBenchDependency(IN _primary_bench_id INT, IN _secondary_benchId INT, IN _include_path TEXT)
	BEGIN
		INSERT INTO bench_dependency (primary_bench_id, secondary_bench_id, include_path) VALUES (_primary_bench_id, _secondary_benchId, _include_path);
	END //	
	
-- Associates the given benchmark with the given space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AssociateBench;
CREATE PROCEDURE AssociateBench(IN _benchId INT, IN _spaceId INT)
	BEGIN		
		INSERT IGNORE INTO bench_assoc VALUES (_spaceId, _benchId);
	END //
	
-- Finds the spaces associated with a given benchmark
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetBenchAssoc;
CREATE PROCEDURE GetBenchAssoc(IN _benchId INT)
	BEGIN
		SELECT space_id
		FROM bench_assoc
		WHERE _benchId = bench_id;
	END //	
	
-- Retrieves all attributes for a benchmark 
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetBenchAttrs;
CREATE PROCEDURE GetBenchAttrs(IN _benchmarkId INT)
	BEGIN
		SELECT *
		FROM bench_attributes 
		WHERE bench_id=_benchmarkId
		ORDER BY attr_key ASC;
	END //
	
DROP PROCEDURE IF EXISTS GetBenchByName;
CREATE PROCEDURE GetBenchByName(IN _id INT, IN _name VARCHAR(256))
	BEGIN
		SELECT *
		FROM benchmarks AS bench
		WHERE deleted=false and bench.id IN
				(SELECT bench_id
				FROM bench_assoc
				WHERE space_id = _id)
		AND bench.name = _name		
		ORDER BY bench.name;
	END //	
	
-- Retrieves all benchmark dependencies for a given primary benchmark id
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetBenchmarkDependencies;
CREATE PROCEDURE GetBenchmarkDependencies(IN _pBenchId INT)
	BEGIN
		SELECT *
		FROM bench_dependency
		WHERE primary_bench_id = _pBenchId;
	END //

-- Deletes a benchmark given that benchmark's id
-- Author: Todd Elvers	+ Eric Burns
DROP PROCEDURE IF EXISTS DeleteBenchmarkById;
CREATE PROCEDURE DeleteBenchmarkById(IN _benchmarkId INT, OUT _path TEXT)
	BEGIN
		SELECT path INTO _path FROM benchmarks WHERE id = _benchmarkId;
		UPDATE benchmarks
		SET deleted=true
		WHERE id = _benchmarkId;
		UPDATE benchmarks
		SET path=""
		WHERE id = _benchmarkId;
		UPDATE benchmarks
		SET disk_size=0
		WHERE id = _benchmarkId;
		-- if the benchmark is associated with no spaces or job pairs, we can delete it from the database
		IF ((SELECT COUNT(*) FROM bench_assoc WHERE bench_id=_benchmarkId)=0) THEN
			IF ((SELECT COUNT(*) FROM job_pairs WHERE bench_id=_benchmarkId)=0) THEN			
				DELETE FROM benchmarks
				WHERE id=_benchmarkId;
			END IF;
		END IF;
	END //	
	
-- Retrieves the benchmark with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetBenchmarkById;
CREATE PROCEDURE GetBenchmarkById(IN _id INT)
	BEGIN
		SELECT *
		FROM benchmarks AS bench
			LEFT OUTER JOIN processors AS types
			ON bench.bench_type=types.id
		WHERE bench.id = _id and deleted=false;
	END //
	
-- Retrieves the benchmark with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetBenchmarkByIdIncludeDeleted;
CREATE PROCEDURE GetBenchmarkByIdIncludeDeleted(IN _id INT)
	BEGIN
		SELECT *
		FROM benchmarks AS bench
			LEFT OUTER JOIN processors AS types
			ON bench.bench_type=types.id
		WHERE bench.id = _id;
	END //
	
-- Retrieves the upload status with the given id
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetUploadStatusById;
CREATE PROCEDURE GetUploadStatusById(IN _id INT)
	BEGIN
		SELECT *
		FROM benchmark_uploads 
		WHERE id = _id;
	END //	
-- Returns the number of benchmarks in a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetBenchmarkCountInSpace;
CREATE PROCEDURE GetBenchmarkCountInSpace(IN _spaceId INT)
	BEGIN
		SELECT 	COUNT(*) AS benchCount
		FROM 	bench_assoc
		WHERE 	_spaceId=space_id;
	END //

-- Returns the number of benchmarks in a given space that match a given query
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetBenchmarkCountInSpaceWithQuery;
CREATE PROCEDURE GetBenchmarkCountInSpaceWithQuery(IN _spaceId INT, IN _query TEXT)
	BEGIN
		SELECT 	COUNT(*) AS benchCount
		FROM 	bench_assoc
			JOIN	benchmarks AS benchmarks ON benchmarks.id = bench_assoc.bench_id	
			JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id
		WHERE 	_spaceId=space_id AND
				(benchmarks.name LIKE	CONCAT('%', _query, '%')
				OR		benchType.name	LIKE 	CONCAT('%', _query, '%'));
	END //
		
-- Retrieves all benchmarks belonging to a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpaceBenchmarksById;
CREATE PROCEDURE GetSpaceBenchmarksById(IN _id INT)
	BEGIN
		SELECT *
		FROM benchmarks AS bench
			LEFT OUTER JOIN processors AS types
			ON bench.bench_type=types.id
		WHERE bench.deleted=false and bench.id IN
				(SELECT bench_id
				FROM bench_assoc
				WHERE space_id = _id)
		ORDER BY bench.name;
	END //

-- Returns the number of public spaces a benchmark is in
-- Benton McCune
DROP PROCEDURE IF EXISTS IsBenchPublic;
CREATE PROCEDURE IsBenchPublic(IN _benchId INT)
	BEGIN
		SELECT count(*) as benchPublic
		FROM bench_assoc
		WHERE bench_id = _benchId
		AND IsPublic(space_id);
	END //
	
DROP PROCEDURE IF EXISTS IsBenchmarkDeleted;
CREATE PROCEDURE IsBenchmarkDeleted(IN _benchId INT)
	BEGIN
		SELECT count(*) AS benchDeleted
		FROM benchmarks
		WHERE deleted=true AND id=_benchId;
	END //
	
-- Removes the association between a benchmark and a given space;
-- Author: Todd Elvers + Eric Burns
DROP PROCEDURE IF EXISTS RemoveBenchFromSpace;
CREATE PROCEDURE RemoveBenchFromSpace(IN _benchId INT, IN _spaceId INT)
	BEGIN
		IF _spaceId >= 0 THEN
			DELETE FROM bench_assoc
			WHERE space_id = _spaceId
			AND bench_id = _benchId;
		END IF;
		-- Ensure the solver isn't being used in any other space
		IF NOT EXISTS(SELECT * FROM bench_assoc WHERE bench_id =_benchId) THEN
			-- if the solver has been deleted already, remove it from the database
			IF NOT EXISTS(SELECT * FROM benchmarks WHERE _benchId=id AND deleted=false) THEN
				IF ((SELECT COUNT(*) FROM job_pairs WHERE bench_id=_benchmarkId)=0) THEN
					DELETE FROM benchmarks
					WHERE id=_benchId;
				END IF;
			END IF;
		END IF;
	END //
	
	
-- Updates the details associated with a given benchmark
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateBenchmarkDetails;
CREATE PROCEDURE UpdateBenchmarkDetails(IN _benchmarkId INT, IN _name VARCHAR(256), IN _description TEXT, IN _downloadable BOOLEAN, IN _type INT)
	BEGIN
		UPDATE benchmarks
		SET name = _name,
		description = _description,
		downloadable = _downloadable,
		bench_type = _type
		WHERE id = _benchmarkId;
	END //

-- Creates a new UpdateStatus entry when user uploads a benchmark
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS CreateUploadStatus;
CREATE PROCEDURE CreateUploadStatus(IN _spaceId INT, IN _userId INT, OUT id INT)
	BEGIN
		INSERT INTO benchmark_uploads (space_id, user_id, upload_time) VALUES (_spaceId, _userId, NOW());
		SELECT LAST_INSERT_ID() INTO id;
	END //

-- Updates status when file upload is complete
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS FileUploadComplete;
CREATE PROCEDURE FileUploadComplete(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET file_upload_complete = 1
		WHERE id = _id;
	END //
	
-- Updates status when file extraction is complete
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS FileExtractComplete;
CREATE PROCEDURE FileExtractComplete(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET file_extraction_complete = 1
		WHERE id = _id;
	END //
	
-- Updates status when java object is created and processing/entering of benchmarks in db has begun
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS ProcessingBegun;
CREATE PROCEDURE ProcessingBegun(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET processing_begun = 1
		WHERE id = _id;
	END //
	
-- Updates status when the entire upload benchmark process has comploeted
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS EverythingComplete;
CREATE PROCEDURE EverythingComplete(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET everything_complete = 1
		WHERE id = _id;
	END //	
	
-- Updates status when a directory is encountered when traversing extracted file
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementTotalSpaces;
CREATE PROCEDURE IncrementTotalSpaces(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET total_spaces = total_spaces + 1
		WHERE id = _id;
	END //	
	
-- Updates status when a file is encountered when traversing extracted file
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementTotalBenchmarks;
CREATE PROCEDURE IncrementTotalBenchmarks(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET total_benchmarks = total_benchmarks + 1
		WHERE id = _id;
	END //

-- Indicates a space is completely added to the db.
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementCompletedSpaces;
CREATE PROCEDURE IncrementCompletedSpaces(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET completed_spaces = completed_spaces + 1
		WHERE id = _id;
	END //
	
-- Updates status when a benchmark is completed and entered into the db
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementCompletedBenchmarks;
CREATE PROCEDURE IncrementCompletedBenchmarks(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET completed_benchmarks = completed_benchmarks + 1
		WHERE id = _id;
	END //
	
-- Updates status when a benchmark is validated
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementValidatedBenchmarks;
CREATE PROCEDURE IncrementValidatedBenchmarks(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET validated_benchmarks = validated_benchmarks + 1
		WHERE id = _id;
	END //	
	
-- Updates status when a benchmark fails validation
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS IncrementFailedBenchmarks;
CREATE PROCEDURE IncrementFailedBenchmarks(IN _id INT)
	BEGIN
		UPDATE benchmark_uploads
		SET failed_benchmarks = failed_benchmarks + 1
		WHERE id = _id;
	END //		
	

-- Updates status when an error occurs
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS SetErrorMessage;
CREATE PROCEDURE SetErrorMessage(IN _id INT, IN _message VARCHAR(512))
	BEGIN
		UPDATE benchmark_uploads
		SET error_message = _message
		WHERE id = _id;
	END //	
	
-- Updates status when  benchmark fails validation
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS AddUnvalidatedBenchmark;
CREATE PROCEDURE AddUnvalidatedBenchmark(IN _id INT, IN _name VARCHAR(256))
	BEGIN
		INSERT INTO unvalidated_benchmarks (status_id, bench_name)
		VALUES (_id, _name);
	END //	
	
-- Gets direct count of unvalidated benchmarks if there are no more than maximum
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS UnvalidatedBenchmarkCount;
CREATE PROCEDURE UnvalidatedBenchmarkCount(IN _status_id INT)
	BEGIN
		select count(*) from unvalidated_benchmarks 
		WHERE status_id = _status_id;
	END //	
	
-- Gets unvalidated benchmark names
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetUnvalidatedBenchmarks;
CREATE PROCEDURE GetUnvalidatedBenchmarks(IN _status_id INT)
	BEGIN
		select bench_name from unvalidated_benchmarks 
		WHERE status_id = _status_id;
	END //		
	
-- Get the total count of the benchmarks belong to a specific user
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetBenchmarkCountByUser;
CREATE PROCEDURE GetBenchmarkCountByUser(IN _userId INT)
	BEGIN
		SELECT COUNT(*) AS benchCount
		FROM benchmarks
		WHERE user_id = _userId AND deleted=false;
	END //
-- Returns the number of benchmarks a given user has that match the query
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetBenchmarkCountByUserWithQuery;
CREATE PROCEDURE GetBenchmarkCountByUserWithQuery(IN _userId INT, IN _query TEXT)
	BEGIN
		SELECT 	COUNT(*) AS benchCount
		FROM 	benchmarks
			JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id
		WHERE 	benchmarks.user_id=_userId AND deleted=false AND
				(benchmarks.name LIKE	CONCAT('%', _query, '%')
				OR		benchType.name	LIKE 	CONCAT('%', _query, '%'));
	END //

	
DELIMITER ; -- This should always be at the end of this file