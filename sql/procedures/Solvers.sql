-- Description: This file contains all solver-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

-- Adds a solver and returns the solver ID
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS AddSolver //
CREATE PROCEDURE AddSolver(IN _userId INT, IN _name VARCHAR(128), IN _downloadable BOOLEAN, IN _path TEXT, IN _description TEXT, OUT _id INT, IN _diskSize BIGINT, IN _type INT, IN _build_status INT)
	BEGIN
		UPDATE users SET disk_size=disk_size+_diskSize WHERE id = _userId;
		INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable, disk_size, executable_type, build_status)
		VALUES (_userId, _name, SYSDATE(), _path, _description, _downloadable, _diskSize, _type, _build_status);

		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Gets all solvers that reside in public spaces
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetPublicSolvers //
CREATE PROCEDURE GetPublicSolvers()
	BEGIN
		SELECT * from solvers
		JOIN solver_assoc ON solver_assoc.solver_id=solvers.id
		JOIN spaces ON spaces.id=solver_assoc.space_id
		where public_access=1 AND deleted=false AND recycled=false
		GROUP BY(solvers.id);
	END //

-- Gets the number of conflicting benchmarks a given config was run against for a stage.
-- A conflicting benchmark is a benchmark for which two solvers gave different results.
DROP PROCEDURE IF EXISTS GetConflictsForConfigInJob //
CREATE PROCEDURE GetConflictsForConfigInJob(IN _jobId INT, IN _configId INT, IN _stageNumber INT)
  BEGIN
	SELECT COUNT(DISTINCT jp_o.bench_id) AS conflicting_benchmarks
	FROM jobs j_o JOIN job_pairs jp_o ON j_o.id=jp_o.job_id
		JOIN jobpair_stage_data jpsd_o ON jpsd_o.jobpair_id=jp_o.id
		JOIN job_attributes ja_o ON ja_o.pair_id=jp_o.id
		JOIN
			(SELECT jp.bench_id
			FROM jobs j join job_pairs jp ON j.id=jp.job_id
				JOIN jobpair_stage_data jpsd ON jpsd.jobpair_id=jp.id
				JOIN job_attributes ja ON ja.pair_id=jp.id
			WHERE j.id=_jobId
				AND ja.stage_number=_stageNumber
				AND ja.attr_key='starexec-result'
				AND ja.attr_value!='starexec-unknown'
			GROUP BY jp.bench_id
			HAVING COUNT(DISTINCT ja.attr_value) > 1) AS conflicting
		ON jp_o.bench_id=conflicting.bench_id
	WHERE jpsd_o.config_id=_configId
		AND ja_o.attr_key='starexec-result'
		AND ja_o.attr_value!='starexec-unknown'
	;
  END //

-- Gets the data for conflicting benchmarks in the job.
-- A conflicting benchmark is a benchmark for which two solvers gave different results.
DROP PROCEDURE IF EXISTS GetConflictingBenchmarksForConfigInJob //
CREATE PROCEDURE GetConflictingBenchmarksForConfigInJob(IN _jobId INT, IN _configId INT, IN _stageNumber INT)
	BEGIN
		SELECT b_o.*
		FROM jobs j_o JOIN job_pairs jp_o ON j_o.id=jp_o.job_id
			JOIN jobpair_stage_data jpsd_o ON jpsd_o.jobpair_id=jp_o.id
			JOIN job_attributes ja_o ON ja_o.pair_id=jp_o.id
			JOIN benchmarks b_o ON b_o.id=jp_o.bench_id
			JOIN
			(SELECT jp.bench_id
			 FROM jobs j join job_pairs jp ON j.id=jp.job_id
				 JOIN jobpair_stage_data jpsd ON jpsd.jobpair_id=jp.id
				 JOIN job_attributes ja ON ja.pair_id=jp.id
			 WHERE j.id=_jobId
						 AND ja.stage_number=_stageNumber
						 AND ja.attr_key='starexec-result'
						 AND ja.attr_value!='starexec-unknown'
			 GROUP BY jp.bench_id
			 HAVING COUNT(DISTINCT ja.attr_value) > 1) AS conflicting
				ON jp_o.bench_id=conflicting.bench_id
		WHERE jpsd_o.config_id=_configId
					AND ja_o.attr_key='starexec-result'
					AND ja_o.attr_value!='starexec-unknown'
		GROUP BY b_o.id
		;
	END //

-- Gets the all of the solvers, configs, and results run on a benchmark in a job.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetSolverConfigResultsForBenchmarkInJob //
CREATE PROCEDURE GetSolverConfigResultsForBenchmarkInJob(IN _jobId INT, IN _benchId INT, IN _stageNum INT)
	BEGIN
		SELECT
				s.*, c.*,
				-- solver fields
				/*
				s.id AS s_id, s.user_id AS s_user_id, s.name AS s_name, s.uploaded AS s_uploaded, s.path AS s_path, s.description AS s_description,
				s.downloadable AS s_downloadable, s.disk_size AS s_disk_size, s.deleted AS s_deleted, s.recycled AS s_recycled,
				s.executable_type AS s_exectuable_type, s.build_status AS s_build_status,
				-- configuration fields
				c.id AS c_id, c.solver_id AS c_solver_id, c.name AS c_name, c.description AS c_description, c.updated AS c_updated,
				-- Value of starexec-result attribute
				*/
			 	ja.attr_value
		FROM jobs j JOIN job_pairs jp ON j.id=jp.job_id
				JOIN jobpair_stage_data jpsd ON jpsd.jobpair_id=jp.id
				JOIN solvers s ON jpsd.solver_id=s.id
				JOIN configurations c ON jpsd.config_id=c.id
				JOIN job_attributes ja ON ja.pair_id=jp.id
		WHERE
				j.id = _jobId
				AND jp.bench_id=_benchid
				AND ja.attr_key='starexec-result'
				AND ja.attr_value!='starexec-unknown'
				AND jpsd.stage_number=_stageNum
		;
	END //


-- Adds a Space/Solver association
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS AddSolverAssociation //
CREATE PROCEDURE AddSolverAssociation(IN _spaceId INT, IN _solverId INT)
	BEGIN
		INSERT IGNORE INTO solver_assoc VALUES (_spaceId, _solverId);
	END //

-- Adds a run configuration to the specified solver
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS AddConfiguration //
CREATE PROCEDURE AddConfiguration(IN _solverId INT, IN _name VARCHAR(128), IN _description TEXT, IN _time TIMESTAMP, OUT configId INT)
	BEGIN
		INSERT INTO configurations (solver_id, name, description, updated)
		VALUES (_solverId, _name, _description, _time);

		SELECT LAST_INSERT_ID() INTO configId;
	END //


-- Deletes a configuration given that configuration's id
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS DeleteConfigurationById //
CREATE PROCEDURE DeleteConfigurationById(IN _configId INT)
	BEGIN
		DELETE FROM configurations
		WHERE id = _configId;
	END //


-- Deletes a solver given that solver's id
-- Author: Todd Elvers + Eric Burns
DROP PROCEDURE IF EXISTS SetSolverToDeletedById //
CREATE PROCEDURE SetSolverToDeletedById(IN _solverId INT, OUT _path TEXT)
	BEGIN
		UPDATE users JOIN solvers ON solvers.user_id=users.id
		SET users.disk_size=users.disk_size-solvers.disk_size
		WHERE solvers.id = _solverId;

		SELECT path INTO _path FROM solvers WHERE id = _solverId;
		UPDATE solvers
		SET deleted=true, disk_size=0
		WHERE id = _solverId;
	END //

-- Gets the IDs of all the spaces associated with the given solver
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetAssociatedSpaceIdsBySolver //
CREATE PROCEDURE GetAssociatedSpaceIdsBySolver(IN _solverId INT)
	BEGIN
		SELECT space_id
		FROM solver_assoc
		WHERE solver_id=_solverId;
	END //

-- Retrieves the configurations with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetConfiguration //
CREATE PROCEDURE GetConfiguration(IN _id INT)
	BEGIN
		SELECT *
		FROM configurations
		WHERE id = _id;
	END //

DROP PROCEDURE IF EXISTS GetAllSolversInJob //
CREATE PROCEDURE GetAllSolversInJob(IN _jobId INT)
	BEGIN
		SELECT DISTINCT solver_id, solver_name
		FROM jobpair_stage_data
		INNER JOIN job_pairs ON jobpair_stage_data.jobpair_id=job_pairs.id
		WHERE job_pairs.job_id=_jobId;
	END //

DROP PROCEDURE IF EXISTS GetAllConfigsInJob //
CREATE PROCEDURE GetAllConfigsInJob(IN _jobId INT)
	BEGIN
		SELECT DISTINCT config_id, config_name
		FROM jobpair_stage_data
		INNER JOIN job_pairs ON jobpair_stage_data.jobpair_id=job_pairs.id
		WHERE job_pairs.job_id=_jobId;
	END //

DROP PROCEDURE IF EXISTS GetAllConfigIdsInJob //
CREATE PROCEDURE GetAllConfigIdsInJob( IN _jobId INT)
	BEGIN
		SELECT DISTINCT config_id
		FROM jobpair_stage_data
		INNER JOIN job_pairs ON jobpair_stage_data.jobpair_id=job_pairs.id
		WHERE job_pairs.job_id=_jobId;
	END //


-- Retrieves the configurations that belong to a solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetConfigsForSolver //
CREATE PROCEDURE GetConfigsForSolver(IN _id INT)
	BEGIN
		SELECT *
		FROM configurations
		WHERE solver_id = _id;
	END //


-- Retrieves all solvers belonging to a space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSpaceSolversById //
CREATE PROCEDURE GetSpaceSolversById(IN _id INT)
	BEGIN
		SELECT *
		FROM solvers
		JOIN solver_assoc ON solver_assoc.solver_id=solvers.id
		WHERE deleted=false AND recycled=false AND solver_assoc.space_id=_id;
	END //

-- Retrieves the solver associated with the configuration with the given id
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS GetSolverIdByConfigId //
CREATE PROCEDURE GetSolverIdByConfigId(IN _id INT)
	BEGIN
		SELECT solver_id AS id
		FROM configurations
		WHERE id=_id;
	END //

-- Retrieves the solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSolverById //
CREATE PROCEDURE GetSolverById(IN _id INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE id = _id and deleted=false AND recycled=false;
	END //

-- Retrieves the solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSolverByIdIncludeDeleted //
CREATE PROCEDURE GetSolverByIdIncludeDeleted(IN _id INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE id = _id;
	END //

-- Returns the number of solvers in a given space that match a given query
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolverCountInSpaceWithQuery //
CREATE PROCEDURE GetSolverCountInSpaceWithQuery(IN _spaceId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS solverCount
		FROM solver_assoc
			JOIN solvers AS solvers ON solvers.id=solver_assoc.solver_id
		WHERE _spaceId=solver_assoc.space_id AND
				(solvers.name 	LIKE	CONCAT('%', _query, '%')
				OR		solvers.description	LIKE 	CONCAT('%', _query, '%'));
	END //


-- Retrieves the solvers owned by a given user id
-- Todd Elvers
DROP PROCEDURE IF EXISTS GetSolversByOwner //
CREATE PROCEDURE GetSolversByOwner(IN _userId INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE user_id = _userId and deleted=false AND recycled=false;
	END //

-- Returns the number of public spaces a solver is in
-- Benton McCune
DROP PROCEDURE IF EXISTS IsSolverPublic //
CREATE PROCEDURE IsSolverPublic(IN _solverId INT)
	BEGIN
		SELECT count(*) as solverPublic
		FROM solver_assoc
		WHERE solver_id = _solverId
		AND IsPublic(space_id);
	END //

DROP PROCEDURE IF EXISTS IsSolverDeleted //
CREATE PROCEDURE IsSolverDeleted(IN _solverId INT)
	BEGIN
		SELECT count(*) AS solverDeleted
		FROM solvers
		WHERE deleted=true AND id=_solverId;
	END //

-- Removes the association between a solver and a given space;
-- Author: Todd Elvers + Eric Burns
DROP PROCEDURE IF EXISTS RemoveSolverFromSpace //
CREATE PROCEDURE RemoveSolverFromSpace(IN _solverId INT, IN _spaceId INT)
	BEGIN
		IF _spaceId >= 0 THEN
			DELETE FROM solver_assoc
			WHERE solver_id = _solverId
			AND space_id = _spaceId;
		END IF;
	END //

-- Updates the disk_size attribute of a given solver
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateSolverDiskSize //
CREATE PROCEDURE UpdateSolverDiskSize(IN _solverId INT, IN _newDiskSize BIGINT)
	BEGIN
		UPDATE users JOIN solvers ON solvers.user_id=users.id
		SET users.disk_size=(users.disk_size-solvers.disk_size)+_newDiskSize
		WHERE solvers.id = _solverId;
		UPDATE solvers
		SET disk_size = _newDiskSize
		WHERE id = _solverId;
	END //

-- Updates the details associated with a given configuration
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateConfigurationDetails //
CREATE PROCEDURE UpdateConfigurationDetails(IN _configId INT, IN _name VARCHAR(128), IN _description TEXT, IN _time TIMESTAMP)
	BEGIN
		UPDATE configurations
		SET name = _name,
		description = _description,
		updated = _time
		WHERE id = _configId;
	END //


-- Updates the details associated with a given solver
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateSolverDetails //
CREATE PROCEDURE UpdateSolverDetails(IN _solverId INT, IN _name VARCHAR(128), IN _description TEXT, IN _downloadable BOOLEAN)
	BEGIN
		UPDATE solvers
		SET name = _name,
		description = _description,
		downloadable = _downloadable
		WHERE id = _solverId;
	END //

-- Get the total count of the solvers belong to a specific user
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetSolverCountByUser //
CREATE PROCEDURE GetSolverCountByUser(IN _userId INT)
	BEGIN
		SELECT COUNT(*) AS solverCount
		FROM solvers
		WHERE user_id = _userId AND deleted=false AND recycled=false;
	END //

-- Returns the number of solvers in a given space that match a given query
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolverCountByUserWithQuery //
CREATE PROCEDURE GetSolverCountByUserWithQuery(IN _userId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS solverCount
		FROM solvers
		WHERE user_id=_userId AND deleted=false AND recycled=false AND
				(name 		LIKE	CONCAT('%', _query, '%')
				OR		description	LIKE 	CONCAT('%', _query, '%'));
	END //

-- Sets the recycled attribute to the given value for the given solver
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS SetSolverRecycledValue //
CREATE PROCEDURE SetSolverRecycledValue(IN _solverId INT, IN _recycled BOOLEAN)
	BEGIN
		UPDATE solvers
		SET recycled=_recycled
		WHERE id=_solverId;
	END //

-- Checks to see whether the "recycled" flag is set for the given solver
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS IsSolverRecycled //
CREATE PROCEDURE IsSolverRecycled(IN _solverId INT)
	BEGIN
		SELECT recycled FROM solvers
		WHERE id=_solverId;
	END //

-- Returns the number of solvers in a given space that match a given query
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetRecycledSolverCountByUser //
CREATE PROCEDURE GetRecycledSolverCountByUser(IN _userId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS solverCount
		FROM solvers
		WHERE solvers.user_id=_userId AND recycled=true AND deleted=false AND
				(solvers.name 	LIKE	CONCAT('%', _query, '%')
				OR		solvers.description	LIKE 	CONCAT('%', _query, '%'));
	END //

-- Gets the path to every recycled solver a user has
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetRecycledSolverPaths //
CREATE PROCEDURE GetRecycledSolverPaths(IN _userId INT)
	BEGIN
		SELECT path,id FROM solvers
		WHERE recycled=true AND user_id=_userId AND deleted=false;
	END //

-- Sets all the solvers the user has in the database to "deleted"
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS SetRecycledSolversToDeleted //
CREATE PROCEDURE SetRecycledSolversToDeleted(IN _userId INT)
	BEGIN
		UPDATE users
		SET users.disk_size=users.disk_size-(SELECT COALESCE(SUM(disk_size),0) FROM solvers WHERE user_id=_userId AND recycled=true AND deleted=false)
		WHERE users.id=_userId;
		UPDATE solvers
		SET deleted=true, disk_size=0
		WHERE user_id = _userId AND recycled=true AND deleted=false;
	END //

-- Gets all recycled solver ids a user has in the database
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetRecycledSolverIds //
CREATE PROCEDURE GetRecycledSolverIds(IN _userId INT)
	BEGIN
		SELECT id FROM solvers
		WHERE user_id=_userId AND recycled=true;
	END //

-- Permanently removes a solver from the database
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS RemoveSolverFromDatabase //
CREATE PROCEDURE RemoveSolverFromDatabase(IN _id INT)
	BEGIN
		DELETE FROM solvers
		WHERE id=_id;
	END //

-- Gets all the solver ids of solvers that are in at least one space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolversAssociatedWithSpaces //
CREATE PROCEDURE GetSolversAssociatedWithSpaces()
	BEGIN
		SELECT DISTINCT solver_id AS id FROM solver_assoc;
	END //

-- Gets the solver ids of all solvers associated with at least one pair
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolversAssociatedWithPairs //
CREATE PROCEDURE GetSolversAssociatedWithPairs()
	BEGIN
		SELECT DISTINCT solver_id AS id from jobpair_stage_data;
	END //

-- Gets the solver ids of all deleted solvers
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetDeletedSolvers //
CREATE PROCEDURE GetDeletedSolvers()
	BEGIN
		SELECT * FROM solvers WHERE deleted=true;
	END //

-- Sets the recycled flag for a single solver back to false
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS RestoreSolver //
CREATE PROCEDURE RestoreSolver(IN _solverId INT)
	BEGIN
		UPDATE solvers
		SET recycled=false
		WHERE _solverId=id;
	END //

-- Gets the timestamp of the configuration that was most recently added or updated
-- on this solver
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetMaxConfigTimestamp //
CREATE PROCEDURE GetMaxConfigTimestamp(IN _solverId INT)
	BEGIN
		SELECT MAX(updated) AS recent
		FROM configurations
		WHERE configurations.solver_id=_solverId;
	END //

-- Gets the ids of every orphaned solver a user owns (orphaned meaning the solver is in no spaces
DROP PROCEDURE IF EXISTS GetOrphanedSolverIds //
CREATE PROCEDURE GetOrphanedSolverIds(IN _userId INT)
	BEGIN
		SELECT solvers.id FROM solvers
		LEFT JOIN solver_assoc ON solver_assoc.solver_id=solvers.id
		WHERE solvers.user_id=_userId AND solver_assoc.space_id IS NULL;
	END //

-- returns every solver that shares a space with the given user
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolversInSharedSpaces //
CREATE PROCEDURE GetSolversInSharedSpaces(IN _userId INT)
	BEGIN
		SELECT * FROM solvers
		JOIN solver_assoc ON solver_assoc.solver_id = solvers.id
		JOIN user_assoc ON user_assoc.space_id = solver_assoc.space_id
		WHERE user_assoc.user_id=_userId
		GROUP BY(solvers.id);
	END //

-- Sets the build_status status code of the solver
-- Author: Andrew Lubinus
DROP PROCEDURE IF EXISTS SetSolverBuildStatus //
CREATE PROCEDURE SetSolverBuildStatus(IN _solverId INT, IN _build_status INT)
    BEGIN
        UPDATE solvers
        SET build_status = _build_status
        WHERE id = _solverId;
    END //

-- Updates path to solver
-- Author: Andrew Lubinus
DROP PROCEDURE IF EXISTS SetSolverPath //
CREATE PROCEDURE SetSolverPath(IN _solverId INT, IN _path TEXT)
    BEGIN
        UPDATE solvers
        SET path = _path
        WHERE id = _solverId;
    END //

-- This deletes the dummy config from a solver built on Starexec
DROP PROCEDURE IF EXISTS DeleteBuildConfig //
CREATE PROCEDURE DeleteBuildConfig(IN _solverId INT)
    BEGIN
        DELETE FROM configurations
        WHERE solver_id=_solverId AND name="starexec_build";
    END //

-- Pauses all JobPairs containing Solver and rebuilds Solver
DROP PROCEDURE IF EXISTS RebuildSolver //
CREATE PROCEDURE RebuildSolver(IN _solverId INT)
	BEGIN
		-- Fetch all JobPairs containing solver
		CREATE TEMPORARY TABLE JobPairsContainingSolver AS (
			SELECT DISTINCT jobpair_id FROM (
				SELECT jobpair_id, solver_id
				FROM jobpair_stage_data
				WHERE solver_id = _solverId
			) AS JobPairStagesWithSolver
		);
		-- Pause all jobs containing solver
		UPDATE jobs
		SET paused = TRUE
		WHERE killed = FALSE
		AND deleted = FALSE
		AND completed = 0
		AND buildJob = FALSE
		AND id IN (
			SELECT job_id FROM (
				SELECT job_id, id FROM job_pairs
				WHERE id IN (SELECT * FROM JobPairsContainingSolver)
			) AS jobPairsWithSolver
		)
		;
		-- Pause all JobPairs containing solver
		UPDATE job_pairs
			SET status_code = 20 -- 20 = Paused : Status.java
			WHERE status_code = 1
			AND id IN (SELECT * FROM JobPairsContainingSolver)
		;
		-- Set Solver status to Unbuilt
		UPDATE solvers
			SET build_status = 0, -- 0 = Unbuilt : SolverBuildStatus.java
			    path = CONCAT(path, "_src")
			WHERE id = _solverId
			AND build_status = 2  -- 2 = Built by StarExec
		;
	END //
