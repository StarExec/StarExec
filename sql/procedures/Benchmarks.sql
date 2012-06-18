-- Description: This file contains all benchmark stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement



-- Adds a benchmark into the system and associates it with a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddBenchmark;
CREATE PROCEDURE AddBenchmark(IN _name VARCHAR(128), IN _path TEXT, IN _downloadable TINYINT(1), IN _userId INT, IN _typeId INT, IN _spaceId INT, IN _diskSize BIGINT, OUT _benchId INT)
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
	
-- Adds 10 attributes to a benchmark 
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS AddBenchAttrTen;
CREATE PROCEDURE AddBenchAttrTen(IN _benchmarkId1 INT, IN _key1 VARCHAR(128), IN _val1 VARCHAR(128),
	IN _benchmarkId2 INT, IN _key2 VARCHAR(128), IN _val2 VARCHAR(128),
	IN _benchmarkId3 INT, IN _key3 VARCHAR(128), IN _val3 VARCHAR(128),
	IN _benchmarkId4 INT, IN _key4 VARCHAR(128), IN _val4 VARCHAR(128),
	IN _benchmarkId5 INT, IN _key5 VARCHAR(128), IN _val5 VARCHAR(128),
	IN _benchmarkId6 INT, IN _key6 VARCHAR(128), IN _val6 VARCHAR(128),
	IN _benchmarkId7 INT, IN _key7 VARCHAR(128), IN _val7 VARCHAR(128),
	IN _benchmarkId8 INT, IN _key8 VARCHAR(128), IN _val8 VARCHAR(128),
	IN _benchmarkId9 INT, IN _key9 VARCHAR(128), IN _val9 VARCHAR(128),
	IN _benchmarkId10 INT, IN _key10 VARCHAR(128), IN _val10 VARCHAR(128))
	BEGIN
		INSERT INTO bench_attributes 
		VALUES 
			(_benchmarkId1, _key1, _val1),
			(_benchmarkId2, _key2, _val2),
			(_benchmarkId3, _key3, _val3),
			(_benchmarkId4, _key4, _val4),
			(_benchmarkId5, _key5, _val5),
			(_benchmarkId6, _key6, _val6),
			(_benchmarkId7, _key7, _val7),
			(_benchmarkId8, _key8, _val8),
			(_benchmarkId9, _key9, _val9),
			(_benchmarkId10, _key10, _val10);
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
CREATE PROCEDURE GetBenchByName(IN _id INT, IN _name VARCHAR(128))
	BEGIN
		SELECT *
		FROM benchmarks AS bench
		WHERE bench.id IN
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
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS DeleteBenchmarkById;
CREATE PROCEDURE DeleteBenchmarkById(IN _benchmarkId INT, OUT _path TEXT)
	BEGIN
		SELECT path INTO _path FROM benchmarks WHERE id = _benchmarkId;
		DELETE FROM benchmarks
		WHERE id = _benchmarkId;
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
		WHERE bench.id = _id;
	END //
	
-- Returns the number of benchmarks in a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetBenchmarkCountInSpace;
CREATE PROCEDURE GetBenchmarkCountInSpace(IN _spaceId INT)
	BEGIN
		SELECT 	COUNT(*) AS benchCount
		FROM 	benchmarks
		WHERE 	id	IN (SELECT bench_id
						FROM bench_assoc
						WHERE space_id = _spaceId);
	END //
	
-- Retrieves all benchmarks owned by a given user id
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetBenchmarksByOwner;
CREATE PROCEDURE GetBenchmarksByOwner(IN _userId INT)
	BEGIN
		SELECT *
		FROM benchmarks
		WHERE user_id = _userId;
	END //

-- Gets the fewest necessary Benchmarks in order to service a client's
-- request for the next page of Benchmarks in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetNextPageOfBenchmarks;
CREATE PROCEDURE GetNextPageOfBenchmarks(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of benchmarks without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id,
						name,
						description,
						GetBenchmarkTypeName(bench_type) 		AS 	benchTypeName,
						GetBenchmarkTypeDescription(bench_type)	AS	benchTypeDescription
				
				FROM	benchmarks
				
				-- Exclude benchmarks that aren't in the specified space
				WHERE 	id 	IN (SELECT 	bench_id
								FROM	bench_assoc
								WHERE 	space_id = _spaceId)
										
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
				(CASE _colSortedOn
					WHEN 0 THEN name
					WHEN 1 THEN benchTypeName
				END) ASC
				
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						name,
						description,
						GetBenchmarkTypeName(bench_type) 		AS 	benchTypeName,
						GetBenchmarkTypeDescription(bench_type)	AS	benchTypeDescription
				
				FROM	benchmarks
				
				-- Exclude benchmarks that aren't in the specified space
				WHERE 	id 	IN (SELECT 	bench_id
								FROM	bench_assoc
								WHERE 	space_id = _spaceId)
										
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
				(CASE _colSortedOn
					WHEN 0 THEN name
					WHEN 1 THEN benchTypeName
				END) DESC
				
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target benchmarks contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id,
						name,
						description,
						GetBenchmarkTypeName(bench_type) 		AS 	benchTypeName,
						GetBenchmarkTypeDescription(bench_type)	AS	benchTypeDescription
				
				FROM 	benchmarks
				
				-- Query Filtering
				WHERE 	name 									LIKE	CONCAT('%', _query, '%')
				OR		GetBenchmarkTypeDescription(bench_type)	LIKE 	CONCAT('%', _query, '%')
								
										
				-- Exclude benchmarks that aren't in the specified space
				AND 	id 	IN (SELECT 	bench_id
								FROM	bench_assoc
								WHERE 	space_id = _spaceId)
										
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name 
						WHEN 1 THEN benchTypeName
					 END) ASC
					 
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						name,
						description,
						GetBenchmarkTypeName(bench_type) 		AS 	benchTypeName,
						GetBenchmarkTypeDescription(bench_type)	AS	benchTypeDescription
				
				FROM 	benchmarks
				
				-- Query Filtering
				WHERE 	name 									LIKE	CONCAT('%', _query, '%')
				OR		GetBenchmarkTypeDescription(bench_type)	LIKE 	CONCAT('%', _query, '%')
										
				-- Exclude benchmarks that aren't in the specified space
				AND 	id 	IN (SELECT 	bench_id
								FROM	bench_assoc
								WHERE 	space_id = _spaceId)
										
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name 
						WHEN 1 THEN benchTypeName
					 END) DESC
					 
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
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
		WHERE bench.id IN
				(SELECT bench_id
				FROM bench_assoc
				WHERE space_id = _id)
		ORDER BY bench.name;
	END //
	
	
-- Removes the association between a benchmark and a given space;
-- places the path of the benchmark in _path if it has no other
-- associations in bench_assoc, otherwise places NULL in _path
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RemoveBenchFromSpace;
CREATE PROCEDURE RemoveBenchFromSpace(IN _benchId INT, IN _spaceId INT, OUT _path TEXT)
	BEGIN
		IF _spaceId >= 0 THEN
			DELETE FROM bench_assoc
			WHERE space_id = _spaceId
			AND bench_id = _benchId;
		END IF;
		
		IF NOT EXISTS (SELECT * FROM bench_assoc WHERE bench_id = _benchId) THEN
			IF NOT EXISTS (SELECT * FROM job_pairs WHERE bench_id = _benchId) THEN
				SELECT path INTO _path FROM benchmarks WHERE id = _benchId;
				DELETE FROM benchmarks
				WHERE id = _benchId;
			ELSE
				SELECT NULL INTO _path;
			END IF;
		ELSE
			SELECT NULL INTO _path;
		END IF;
	END //
	
	
-- Updates the details associated with a given benchmark
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateBenchmarkDetails;
CREATE PROCEDURE UpdateBenchmarkDetails(IN _benchmarkId INT, IN _name VARCHAR(32), IN _description TEXT, IN _downloadable BOOLEAN, IN _type INT)
	BEGIN
		UPDATE benchmarks
		SET name = _name,
		description = _description,
		downloadable = _downloadable,
		bench_type = _type
		WHERE id = _benchmarkId;
	END //
	
	
DELIMITER ; -- This should always be at the end of this file