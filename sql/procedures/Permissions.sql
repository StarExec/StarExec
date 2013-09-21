-- Description: This file contains all permissions-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Adds a new permissions record with the given permissions
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddPermissions;
CREATE PROCEDURE AddPermissions(IN _addSolver TINYINT(1), IN _addBench TINYINT(1), IN _addUser TINYINT(1), 
IN _addSpace TINYINT(1), IN _addJob TINYINT(1), IN _removeSolver TINYINT(1), IN _removeBench TINYINT(1), IN _removeSpace TINYINT(1), 
IN _removeUser TINYINT(1), IN _removeJob TINYINT(1), IN _isLeader TINYINT(1), OUT id INT)
	BEGIN		
		INSERT INTO permissions 
			(add_solver, add_bench, add_user, add_space, add_job, remove_solver, 
			remove_bench, remove_space, remove_user, remove_job, is_leader)
		VALUES
			(_addSolver, _addBench, _addUser, _addSpace, _addJob, _removeSolver, 
			_removeBench, _removeSpace, _removeUser, _removeJob, _isLeader);	
		SELECT LAST_INSERT_ID() INTO id;
	END //
	
-- Returns 1 if the given user can somehow see the given solver, 0 otherwise
-- Author: Tyler Jensen + Eric Burns
DROP PROCEDURE IF EXISTS CanViewSolver;
CREATE PROCEDURE CanViewSolver(IN _solverId INT, IN _userId INT)
	BEGIN		
		SELECT IF((				
			SELECT COUNT(*)
			FROM solvers JOIN solver_assoc ON solvers.id=solver_assoc.solver_id -- Get all solvers and find its association to spaces
			JOIN user_assoc ON user_assoc.space_id=solver_assoc.space_id					-- Join on user_assoc to get all the users that belong to those spaces
			WHERE solvers.id=_solverId AND user_assoc.user_id=_userId)			-- But only count those for the solver and user we're looking for
		> 0, 1, (SELECT COUNT(*) FROM solvers WHERE solvers.id=_solverId AND solvers.user_id=_userId)) -- If there were more than 0 results, return 1, else check to see if the user owns the solver, and return under the name 'verified'
		 AS verified; 	
	END //

-- Author: Tyler Jensen + Eric Burns
DROP PROCEDURE IF EXISTS CanViewBenchmark;
CREATE PROCEDURE CanViewBenchmark(IN _benchId INT, IN _userId INT)
	BEGIN		
		SELECT IF((
			SELECT COUNT(*)
			FROM benchmarks JOIN bench_assoc ON benchmarks.id=bench_assoc.bench_id  -- Get all benchmarks and find its association to spaces
			JOIN user_assoc ON user_assoc.space_id=bench_assoc.space_id             -- Join on user_assoc to get all the users that belong to those spaces
			WHERE benchmarks.id=_benchId AND user_assoc.user_id=_userId)            -- But only count those for the benchmark and user we're looking for
		> 0, 1, (SELECT COUNT(*) FROM benchmarks WHERE benchmarks.id=_benchId AND benchmarks.user_id=_userId)) AS verified; 												    -- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //

-- Returns 1 if the given user can somehow see the given job, 0 otherwise
-- Author: Tyler Jensen	+ Eric Burns
DROP PROCEDURE IF EXISTS CanViewJob;
CREATE PROCEDURE CanViewJob(IN _jobId INT, IN _userId INT)
	BEGIN		
		SELECT IF((
			SELECT COUNT(*)
			FROM jobs JOIN job_assoc ON jobs.id=job_assoc.job_id      -- Get all jobs and find its association to spaces
			JOIN user_assoc ON user_assoc.space_id=job_assoc.space_id -- Join on user_assoc to get all the users that belong to those spaces
			WHERE jobs.id=_jobId AND user_assoc.user_id=_userId)      -- But only count those for the job and user we're looking for
		> 0, 1, (SELECT COUNT(*) FROM jobs WHERE jobs.id=_jobId AND jobs.user_id=_userId )) AS verified;
	END //

-- Returns 1 if the given user can somehow see the given space, 0 otherwise
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS CanViewSpace;
CREATE PROCEDURE CanViewSpace(IN _spaceId INT, IN _userId INT)
	BEGIN
			SELECT IF(
				(SELECT COUNT(*)
				FROM user_assoc 
				WHERE space_id=_spaceId AND user_id=_userId)	-- Directly find how many times the user belongs to a space
			> 0, 1, 0) AS verified;                             -- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //
	
-- Returns 1 if the given user can somehow see the given upload status, 0 otherwise
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS CanViewStatus;
CREATE PROCEDURE CanViewStatus(IN _statusId INT, IN _userId INT)
	BEGIN		
		SELECT IF((
			SELECT COUNT(*)
			FROM benchmark_uploads 
			WHERE id=_statusId AND user_id=_userId)	> 0, 1, 0) AS verified; -- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //	
	
-- Finds the maximal set of permissions for the given user on the given space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetUserPermissions;
CREATE PROCEDURE GetUserPermissions(IN _userId INT, IN _spaceId INT)
	BEGIN		
		SELECT MAX(add_solver) AS add_solver, 
			MAX(add_bench) AS add_bench, 
			MAX(add_user) AS add_user, 
			MAX(add_space) AS add_space, 
			MAX(add_job) AS add_job,
			MAX(remove_solver) AS remove_solver, 
			MAX(remove_bench) AS remove_bench, 
			MAX(remove_space) AS remove_space, 
			MAX(remove_user) AS remove_user,
			MAX(remove_job) AS remove_job,
			MAX(is_leader) AS is_leader
		FROM permissions JOIN user_assoc ON user_assoc.permission=permissions.id
		WHERE user_assoc.user_id=_userId AND user_assoc.space_id=_spaceId;
	END //
	
-- Finds the default user permissions for the given space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpacePermissions;
CREATE PROCEDURE GetSpacePermissions(IN _spaceId INT)
	BEGIN		
		SELECT permissions.*
		FROM permissions JOIN spaces ON spaces.default_permission=permissions.id
		WHERE spaces.id=_spaceId;
	END //	
	
-- Copies one set of permissions into another with a new ID
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS CopyPermissions;
CREATE PROCEDURE CopyPermissions(IN _permId INT, OUT _newId INT)
	BEGIN
		INSERT INTO permissions (add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader)
		(SELECT add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader
		FROM permissions
		WHERE id=_permId);
		SELECT LAST_INSERT_ID() INTO _newId;
	END //	
	

-- Sets a user's permissions for a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS SetUserPermissions;
CREATE PROCEDURE SetUserPermissions(IN _userId INT, IN _spaceId INT,IN _addSolver TINYINT(1), IN _addBench TINYINT(1), IN _addUser TINYINT(1), 
IN _addSpace TINYINT(1), IN _addJob TINYINT(1), IN _removeSolver TINYINT(1), IN _removeBench TINYINT(1), IN _removeSpace TINYINT(1), 
IN _removeUser TINYINT(1), IN _removeJob TINYINT(1), IN _isLeader TINYINT(1))
	BEGIN
		UPDATE permissions JOIN user_assoc ON permissions.id=user_assoc.permission
		SET add_user      = _addUser,
			add_solver    = _addSolver, 
			add_bench     = _addBench,
			add_job       = _addJob,
			add_space     = _addSpace,
			remove_user   = _removeUser,
			remove_solver = _removeSolver,
			remove_bench  = _removeBench,
			remove_job    = _removeJob,
			remove_space  = _removeSpace,
			is_leader     = _isLeader			
		WHERE user_id = _userId
		AND space_id = _spaceId;
	END //
	
-- Updates the permission set with the given id
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdatePermissions;
CREATE PROCEDURE UpdatePermissions(IN _id INT, IN _addSolver BOOLEAN, IN _addBench BOOLEAN, IN _addUser BOOLEAN, 
IN _addSpace BOOLEAN, IN _addJob BOOLEAN, IN _removeSolver BOOLEAN, IN _removeBench BOOLEAN, IN _removeSpace BOOLEAN,
IN _removeUser BOOLEAN, IN _removeJob BOOLEAN)
	BEGIN
		UPDATE permissions
		SET add_user      = _addUser,
			add_solver    = _addSolver, 
			add_bench     = _addBench,
			add_job       = _addJob,
			add_space     = _addSpace,
			remove_user   = _removeUser,
			remove_solver = _removeSolver,
			remove_bench  = _removeBench,
			remove_job    = _removeJob,
			remove_space  = _removeSpace
		WHERE id = _id;
	END //
	

-- Sets a user's permissions for a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS SetUserPermissions2;
CREATE PROCEDURE SetUserPermissions2(IN _userId INT, IN _spaceId INT,IN _permissionId INT)
	BEGIN
		UPDATE user_assoc	
		SET	permission	= _permissionId
		WHERE user_id = _userId && space_id = _spaceId;
	END //



DELIMITER ; -- This should always be at the end of this file