-- Description: This file contains all stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement






/*************************************************************************
********************* BENCHMARK STORED PROCEDURES ************************
*************************************************************************/
	
-- Adds a benchmark into the system and associates it with a space
-- Author: Tyler Jensen
CREATE PROCEDURE AddBenchmark(IN _name VARCHAR(32), IN _path TEXT, IN _downloadable TINYINT(1), IN _userId BIGINT, IN _typeId BIGINT, IN _spaceId BIGINT)
	BEGIN
		DECLARE bid BIGINT DEFAULT -1;
		
		INSERT INTO benchmarks (user_id, name, bench_type, uploaded, path, downloadable)
		VALUES (_userId, _name, _typeId, SYSDATE(), _path, _downloadable);
		
		SELECT LAST_INSERT_ID() INTO bid;
		INSERT INTO bench_assoc VALUES (_spaceId, bid);
	END //	

-- Deletes a benchmark given that benchmark's id
-- Author: Todd Elvers	
CREATE PROCEDURE DeleteBenchmarkById(IN _benchmarkId BIGINT)
	BEGIN
		DELETE FROM benchmarks
		WHERE id = _benchmarkId;
	END //	
	
-- Updates the first name of the user with the given user id to the
-- given first name. The first name should already be validated
-- Author: Skylar Stark
-- Retrieves the benchmark with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE GetBenchmarkById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM benchmarks AS bench
			LEFT OUTER JOIN bench_types AS types
			ON bench.bench_type=types.id
		WHERE bench.id = _id;
	END //
	
-- Retrieves all benchmarks belonging to a space
-- Author: Tyler Jensen
CREATE PROCEDURE GetSpaceBenchmarksById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM benchmarks AS bench
			LEFT OUTER JOIN bench_types AS types
			ON bench.bench_type=types.id
		WHERE bench.id IN
				(SELECT bench_id
				FROM bench_assoc
				WHERE space_id = _id)
		ORDER BY bench.name;
	END //
	
-- Removes the association between a benchmark and a given space
-- Author: Todd Elvers
CREATE PROCEDURE RemoveBenchFromSpace(IN _benchId BIGINT, IN _spaceId BIGINT)
	BEGIN
		DELETE FROM bench_assoc
		WHERE space_id = _spaceId
		AND bench_id = _benchId;
	END //
	
-- Updates the details associated with a given benchmark
-- Author: Todd Elvers
CREATE PROCEDURE UpdateBenchmarkDetails(IN _benchmarkId BIGINT, IN _name VARCHAR(32), IN _description TEXT, IN _downloadable BOOLEAN, IN _type BIGINT)
	BEGIN
		UPDATE benchmarks
		SET name = _name,
		description = _description,
		downloadable = _downloadable,
		bench_type = _type
		WHERE id = _benchmarkId;
	END //
	
	
	
	
	

/*************************************************************************
********************* BENCH TYPE STORED PROCEDURES ***********************
*************************************************************************/

-- Adds a new benchmark type with the given information
-- Author: Tyler Jensen
CREATE PROCEDURE AddBenchmarkType(IN _name VARCHAR(32), IN _desc TEXT, IN _path TEXT, IN _comId BIGINT)
	BEGIN		
		INSERT INTO bench_types (name, description, processor_path, community)
		VALUES (_name, _desc, _path, _comId);
	END //
	
-- Deletes a benchmark type associated with a space
-- Author: Todd Elvers
CREATE PROCEDURE DeleteBenchmarkType(IN _id BIGINT, IN _community BIGINT)
	BEGIN
		DELETE FROM bench_types
		WHERE id = _id AND community = _community;
	END //
	
-- Gets all benchmark types
-- Author: Todd Elvers
CREATE PROCEDURE GetAllBenchTypes()
	BEGIN		
		SELECT *
		FROM bench_types;
	END //
	
-- Retrieves all benchmark types belonging to a community
-- Author: Tyler Jensen
CREATE PROCEDURE GetBenchTypesByCommunity(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM bench_types
		WHERE community=_id
		ORDER BY name;
	END //
	
-- Gets the benchmark type with the given ID
-- Author: Tyler Jensen
CREATE PROCEDURE GetBenchTypeById(IN _id BIGINT)
	BEGIN		
		SELECT *
		FROM bench_types
		WHERE id=_id;
	END //
	
-- Updates a benchmark type's description
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateBenchTypeDescription(IN _id BIGINT, IN _desc TEXT)
	BEGIN		
		UPDATE bench_types
		SET description=_desc
		WHERE id=_id;
	END //
	
-- Updates a benchmark type's name
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateBenchTypeName(IN _id BIGINT, IN _name VARCHAR(32))
	BEGIN		
		UPDATE bench_types
		SET name=_name
		WHERE id=_id;
	END //
	
-- Updates a benchmark type's processor path
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateBenchTypePath(IN _id BIGINT, IN _path TEXT)
	BEGIN		
		UPDATE bench_types
		SET processor_path=_path
		WHERE id=_id;
	END //
	
	

	
	
	

/*************************************************************************
********************** CLUSTER STORED PROCEDURES *************************
*************************************************************************/

-- Adds a worker node to the database and ignores duplicates
-- Author: Tyler Jensen
CREATE PROCEDURE AssociateQueue(IN _queueName VARCHAR(64), IN _nodeName VARCHAR(64))
	BEGIN
		INSERT IGNORE INTO queue_assoc
		VALUES(
			(SELECT id FROM queues WHERE name=_queueName), 
			(SELECT id FROM nodes WHERE name=_nodeName));
	END //
	
-- Adds a worker node to the database and ignores duplicates
-- Author: Tyler Jensen
CREATE PROCEDURE AddNode(IN _name VARCHAR(64))
	BEGIN
		INSERT IGNORE INTO nodes (name)
		VALUES (_name);
	END //
	
-- Adds a SGE queue to the database and ignores duplicates
-- Author: Tyler Jensen
CREATE PROCEDURE AddQueue(IN _name VARCHAR(64))
	BEGIN
		INSERT IGNORE INTO queues (name)
		VALUES (_name);
	END //
	
-- Gets the id, name and status of all nodes in the cluster
-- Author: Tyler Jensen
CREATE PROCEDURE GetAllNodes()
	BEGIN		
		SELECT id, name, status
		FROM nodes
		ORDER BY name;	
	END //
	
-- Gets the id, name and status of all nodes in the cluster that are active
-- Author: Tyler Jensen
CREATE PROCEDURE GetNodesForQueue(IN _id BIGINT)
	BEGIN		
		SELECT id, name, status
		FROM nodes
		WHERE id IN
			(SELECT node_id FROM queue_assoc WHERE queue_id=_id)
		ORDER BY name;	
	END //

-- Gets the id, name and status of all queues in the cluster that are active
-- Author: Tyler Jensen
CREATE PROCEDURE GetAllQueues()
	BEGIN		
		SELECT id, name, status
		FROM queues
		WHERE status="ACTIVE"
		ORDER BY name;	
	END //
	
-- Gets worker node with the given ID
-- Author: Tyler Jensen
CREATE PROCEDURE GetNodeDetails(IN _id BIGINT)
	BEGIN		
		SELECT *
		FROM nodes
		WHERE id=_id;
	END // 
	
-- Gets worker node with the given ID
-- Author: Tyler Jensen
CREATE PROCEDURE GetQueueDetails(IN _id BIGINT)
	BEGIN		
		SELECT *
		FROM queues
		WHERE id=_id;
	END // 
	
-- Updates a node's attribute (assuming the column already exists)
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateNodeAttr(IN _name VARCHAR(64), IN _fieldName VARCHAR(64), IN _fieldVal VARCHAR(64))
	BEGIN	
		SET @updateAttr = CONCAT('UPDATE nodes SET ', _fieldName, '="', _fieldVal,'" WHERE name="', _name, '"');
		PREPARE stmt FROM @updateAttr;
		EXECUTE stmt;		
	END // 
	
-- Updates a queues's attribute (assuming the column already exists)
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateQueueAttr(IN _name VARCHAR(64), IN _fieldName VARCHAR(64), IN _fieldVal VARCHAR(64))
	BEGIN	
		SET @updateAttr = CONCAT('UPDATE queues SET ', _fieldName, '="', _fieldVal,'" WHERE name="', _name, '"');
		PREPARE stmt FROM @updateAttr;
		EXECUTE stmt;		
	END // 
	
-- Updates a queues's usage stats
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateQueueUseage(IN _name VARCHAR(64), IN _total INTEGER, IN _free INTEGER, IN _used INTEGER, IN _reserved INTEGER)
	BEGIN	
		UPDATE queues
		SET slots_total=_total, slots_free=_free, slots_used=_used, slots_reserved=_reserved
		WHERE name=_name;
	END // 

-- Updates all queues status'
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateAllQueueStatus(IN _status VARCHAR(32))
	BEGIN	
		UPDATE queues
		SET status=_status;
	END // 
	
-- Updates a specific queues status
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateQueueStatus(IN _name VARCHAR(64), IN _status VARCHAR(32))
	BEGIN	
		UPDATE queues
		SET status=_status
		WHERE name=_name;
	END // 

-- Updates all nodes status'
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateAllNodeStatus(IN _status VARCHAR(32))
	BEGIN	
		UPDATE nodes
		SET status=_status;
	END // 
	
-- Updates a specific node's status
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateNodeStatus(IN _name VARCHAR(64), IN _status VARCHAR(32))
	BEGIN	
		UPDATE nodes
		SET status=_status
		WHERE name=_name;
	END // 

	
	
	
	
	
/*************************************************************************
********************* COMMUNITY STORED PROCEDURES ************************
*************************************************************************/

-- Returns basic space information for the community with the given id
-- This ensures security by preventing malicious users from getting details about ANY space
-- Author: Tyler Jensen
CREATE PROCEDURE GetCommunityById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM spaces
		WHERE id IN
			(SELECT child_id
			 FROM set_assoc
			 WHERE space_id=1
			 AND child_id = _id);
	END //

-- Removes the association a user has with a given space
-- Author: Todd Elvers
CREATE PROCEDURE LeaveCommunity(IN _userId BIGINT, IN _commId BIGINT)
	BEGIN
		DELETE FROM user_assoc
		WHERE user_id = _userId
		AND space_id = _commId;
	END //
	
	
	
	
	

/*************************************************************************
************************ JOB STORED PROCEDURES ***************************
*************************************************************************/

-- Retrieves basic info about a job from the jobs table
-- Author: Tyler Jensen
CREATE PROCEDURE GetJobById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM jobs
		WHERE id = _id;
	END //
	
-- Retrieves basic info about job pairs for the given job id
-- Author: Tyler Jensen
CREATE PROCEDURE GetJobPairByJob(IN _id BIGINT)
	BEGIN
		SELECT job_pair_attr.status AS status, 
			   job_pair_attr.result AS result,
			   job_pair_attr.id AS id, 
			   job_pair_attr.start AS start,
			   job_pair_attr.stop AS stop,
			   configurations.id AS config_id, 
			   configurations.name AS config_name, 
			   configurations.description AS config_desc, 
			   benchmarks.id AS bench_id, 
			   benchmarks.name AS bench_name, 
			   benchmarks.description AS bench_desc,
			   solvers.id AS solver_id,
			   solvers.name AS solver_name,
			   solvers.description AS solver_desc
		FROM job_pair_attr
			   JOIN job_pairs ON job_pair_attr.pair_id=job_pairs.id
			   JOIN benchmarks ON job_pairs.bench_id=benchmarks.id
			   JOIN configurations ON configurations.id=job_pairs.config_id
			   JOIN solvers ON configurations.solver_id=solvers.id
		WHERE job_pair_attr.job_id = _id;
	END //
	
-- Removes the association between a job and a given space
-- Author: Todd Elvers
CREATE PROCEDURE RemoveJobFromSpace(IN _jobId BIGINT, IN _spaceId BIGINT)
BEGIN
	DELETE FROM job_assoc
	WHERE job_id = _jobId
	AND space_id = _spaceId;
END //
	
-- Retrieves all jobs belonging to a space
-- Author: Tyler Jensen
CREATE PROCEDURE GetSpaceJobsById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM jobs
		WHERE id IN
				(SELECT job_id
				FROM job_assoc
				WHERE space_id = _id)
		ORDER BY name;
	END //


	
	


/*************************************************************************
*********************** MISC STORED PROCEDURES ***************************
*************************************************************************/

-- Adds a new column to the specified table only if it doesn't already exist.
-- Author: Tyler Jensen
CREATE PROCEDURE AddColumnUnlessExists(IN dbName tinytext, IN tableName tinytext, IN fieldName tinytext, IN fieldDef text)
	BEGIN
		IF NOT EXISTS 
			(SELECT * FROM information_schema.COLUMNS
			WHERE column_name=fieldName
			AND table_name=tableName
			AND table_schema=dbName)
		THEN
			SET @addColumn = CONCAT('ALTER TABLE ', dbName, '.', tableName,
			' ADD COLUMN ', fieldName, ' ', fieldDef);
			PREPARE stmt FROM @addColumn;
			EXECUTE stmt;
		END IF;
	END //

-- Adds a new historical record to the logins table which tracks all user logins
-- Author: Tyler Jensen
CREATE PROCEDURE LoginRecord(IN _userId BIGINT, IN _ipAddress VARCHAR(15), IN _agent TEXT)
	BEGIN		
		INSERT INTO logins (user_id, login_date, ip_address, browser_agent)
		VALUES (_userId, SYSDATE(), _ipAddress, _agent);
	END //







/*************************************************************************
********************* PERMISSION STORED PROCEDURES ***********************
*************************************************************************/

-- Adds a new permissions record with the given permissions
-- Author: Tyler Jensen
CREATE PROCEDURE AddPermissions(IN _addSolver TINYINT(1), IN _addBench TINYINT(1), IN _addUser TINYINT(1), 
IN _addSpace TINYINT(1), IN _removeSolver TINYINT(1), IN _removeBench TINYINT(1), IN _removeSpace TINYINT(1), 
IN _removeUser TINYINT(1), IN _isLeader TINYINT(1), OUT id BIGINT)
	BEGIN		
		INSERT INTO permissions 
			(add_solver, add_bench, add_user, add_space, remove_solver, 
			remove_bench, remove_space, remove_user, is_leader)
		VALUES
			(_addSolver, _addBench, _addUser, _addSpace, _removeSolver, 
			_removeBench, _removeSpace, _removeUser, _isLeader);	
		SELECT LAST_INSERT_ID() INTO id;
	END //
	
-- Returns 1 if the given user can somehow see the given solver, 0 otherwise
-- Author: Tyler Jensen
CREATE PROCEDURE CanViewSolver(IN _solverId BIGINT, IN _userId BIGINT)
	BEGIN		
		SELECT IF((				
			SELECT COUNT(*)
			FROM solvers JOIN solver_assoc ON solvers.id=solver_assoc.solver_id -- Get all solvers and find its association to spaces
			JOIN spaces ON solver_assoc.space_id=spaces.id						-- Join on spaces to get all the spaces the solver belongs to
			JOIN user_assoc ON user_assoc.space_id=spaces.id					-- Join on user_assoc to get all the users that belong to those spaces
			WHERE solvers.id=_solverId AND user_assoc.user_id=_userId)			-- But only count those for the solver and user we're looking for
		> 0, 1, 0) AS verified; 												-- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //

	-- Returns 1 if the given user can somehow see the given benchmark, 0 otherwise
-- Author: Tyler Jensen
CREATE PROCEDURE CanViewBenchmark(IN _benchId BIGINT, IN _userId BIGINT)
	BEGIN		
		SELECT IF((
			SELECT COUNT(*)
			FROM benchmarks JOIN bench_assoc ON benchmarks.id=bench_assoc.bench_id  -- Get all benchmarks and find its association to spaces
			JOIN spaces ON bench_assoc.space_id=spaces.id                           -- Join on spaces to get all the spaces the benchmark belongs to
			JOIN user_assoc ON user_assoc.space_id=spaces.id                        -- Join on user_assoc to get all the users that belong to those spaces
			WHERE benchmarks.id=_benchId AND user_assoc.user_id=_userId)            -- But only count those for the benchmark and user we're looking for
		> 0, 1, 0) AS verified; 												    -- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //

-- Returns 1 if the given user can somehow see the given job, 0 otherwise
-- Author: Tyler Jensen	
CREATE PROCEDURE CanViewJob(IN _jobId BIGINT, IN _userId BIGINT)
	BEGIN		
		SELECT IF((
			SELECT COUNT(*)
			FROM jobs JOIN job_assoc ON jobs.id=job_assoc.job_id      -- Get all jobs and find its association to spaces
			JOIN spaces ON job_assoc.space_id=spaces.id               -- Join on spaces to get all the spaces the job belongs to
			JOIN user_assoc ON user_assoc.space_id=spaces.id          -- Join on user_assoc to get all the users that belong to those spaces
			WHERE jobs.id=_jobId AND user_assoc.user_id=_userId)      -- But only count those for the job and user we're looking for
		> 0, 1, 0) AS verified;                                       -- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //

-- Returns 1 if the given user can somehow see the given space, 0 otherwise
-- Author: Tyler Jensen
CREATE PROCEDURE CanViewSpace(IN _spaceId BIGINT, IN _userId BIGINT)
	BEGIN		
		SELECT IF((
			SELECT COUNT(*)
			FROM user_assoc 
			WHERE space_id=_spaceId AND user_id=_userId)	-- Directly find how many times the user belongs to a space
		> 0, 1, 0) AS verified;                             -- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //
	
-- Finds the maximal set of permissions for the given user on the given space
-- Author: Tyler Jensen
CREATE PROCEDURE GetUserPermissions(IN _userId BIGINT, IN _spaceId BIGINT)
	BEGIN		
		SELECT MAX(add_solver) AS add_solver, 
			MAX(add_bench) AS add_bench, 
			MAX(add_user) AS add_user, 
			MAX(add_space) AS add_space, 
			MAX(remove_solver) AS remove_solver, 
			MAX(remove_bench) AS remove_bench, 
			MAX(remove_space) AS remove_space, 
			MAX(remove_user) AS remove_user, 
			MAX(is_leader) AS is_leader
		FROM permissions JOIN user_assoc ON user_assoc.permission=permissions.id
		WHERE user_assoc.user_id=_userId AND user_assoc.space_id=_spaceId;
	END //
	
	
	

/*************************************************************************
********************** REQUEST STORED PROCEDURES *************************
*************************************************************************/

-- Adds an activation code for a specific user
-- Author: Todd Elvers
CREATE PROCEDURE AddCode(IN _id BIGINT, IN _code VARCHAR(36))
	BEGIN
		INSERT INTO verify(user_id, code, created)
		VALUES (_id, _code, SYSDATE());
	END //
	
-- Adds a request to join a community, provided the user isn't already a part of that community
-- Author: Todd Elvers
CREATE PROCEDURE AddCommunityRequest(IN _id BIGINT, IN _community BIGINT, IN _code VARCHAR(36), IN _message VARCHAR(300))
	BEGIN
		IF NOT EXISTS(SELECT * FROM user_assoc WHERE user_id = _id AND space_id = _community) THEN
			INSERT INTO community_requests(user_id, community, code, message, created)
			VALUES (_id, _community, _code, _message, SYSDATE());
		END IF;
	END //
	
-- Adds a user to USER_ASSOC, USER_ROLES and deletes their entry in INVITES
-- Author: Todd Elvers
CREATE PROCEDURE ApproveCommunityRequest(IN _id BIGINT, IN _community BIGINT)
	BEGIN
		IF EXISTS(SELECT * FROM community_requests WHERE user_id = _id AND community = _community) THEN
			DELETE FROM community_requests 
			WHERE user_id = _id and community = _community;
			INSERT INTO user_assoc(user_id, space_id, proxy, permission)
			VALUES(_id, _community, _community, 1);
		END IF;
		
		IF NOT EXISTS(SELECT email FROM user_roles WHERE email = (SELECT email FROM users WHERE users.id = _id)) THEN
			INSERT INTO user_roles(email, role)
				VALUES((SELECT email FROM users WHERE users.id = _id), 'user');
		END IF;
	END //
	
-- Adds a new entry to pass_reset_request for a given user (also deletes previous
-- entries for the same user)
-- Author: Todd Elvers
CREATE PROCEDURE AddPassResetRequest(IN _id BIGINT, IN _code VARCHAR(36))
	BEGIN
		IF EXISTS(SELECT * FROM pass_reset_request WHERE user_id = _id) THEN
			DELETE FROM pass_reset_request
			WHERE user_id = _id;
		END IF;
		INSERT INTO pass_reset_request(user_id, code, created)
		VALUES(_id, _code, SYSDATE());
	END //
	
-- Deletes a user's entry in INVITES, and if the user is unregistered
-- (i.e. doesn't have an entry in USER_ROLES) then they are completely
-- deleted from the system
-- Author: Todd Elvers
CREATE PROCEDURE DeclineCommunityRequest(IN _id BIGINT, IN _community BIGINT)
	BEGIN
		DELETE FROM community_requests 
		WHERE user_id = _id and community = _community;
		DELETE FROM users
		WHERE users.id = _id
		AND users.email NOT IN
			(SELECT email
			FROM user_roles);
	END //
	
-- Returns the community request associated with given user id
-- Author: Todd Elvers
CREATE PROCEDURE GetCommunityRequestById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM community_requests
		WHERE user_id = _id;
	END //
	
-- Returns the community request associated with the given activation code
-- Author: Todd Elvers
CREATE PROCEDURE GetCommunityRequestByCode(IN _code VARCHAR(36))
	BEGIN
		SELECT *
		FROM community_requests
		WHERE code = _code;
	END //

-- Looks for an activation code, and if successful, removes it from VERIFY,
-- then adds an entry to USER_ROLES
-- Author: Todd Elvers
CREATE PROCEDURE RedeemActivationCode(IN _code VARCHAR(36), OUT _id BIGINT)
	BEGIN
		IF EXISTS(SELECT _code FROM verify WHERE code = _code) THEN
			SELECT user_id INTO _id 
			FROM verify
			WHERE code = _code;
			
			DELETE FROM verify
			WHERE code = _code;
		
			
		END IF;
	END // 

-- Redeems a given password reset code by deleting the corresponding entry
-- in pass_reset_request and returning the user_id of that deleted entry
-- Author: Todd Elvers
CREATE PROCEDURE RedeemPassResetRequestByCode(IN _code VARCHAR(36), OUT _id BIGINT)
	BEGIN
		SELECT user_id INTO _id
		FROM pass_reset_request
		WHERE code = _code;
		DELETE FROM pass_reset_request
		WHERE code = _code;
	END //
	
	
	
	
	

/*************************************************************************
********************** SOLVER STORED PROCEDURES **************************
*************************************************************************/

-- Adds a solver and returns the solver ID
-- Author: Skylar Stark
CREATE PROCEDURE AddSolver(IN _userId BIGINT, IN _name VARCHAR(32), IN _downloadable BOOLEAN, IN _path TEXT, IN _description TEXT, OUT _id BIGINT)
	BEGIN
		INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable)
		VALUES (_userId, _name, SYSDATE(), _path, _description, _downloadable);
		
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Adds a Space/Solver association
-- Author: Skylar Stark
CREATE PROCEDURE AddSolverAssociation(IN _spaceId BIGINT, IN _solverId BIGINT)
	BEGIN
		INSERT INTO solver_assoc(space_id, solver_id)
		VALUES (_spaceId, _solverId);
	END // 
	
-- Adds a run configuration to the specified solver
-- Author: Skylar Stark
CREATE PROCEDURE AddConfiguration(IN _solverId BIGINT, IN _name VARCHAR(32))
	BEGIN
		INSERT INTO configurations (solver_id, name)
		VALUES (_solverId, _name);
	END //
	
-- Deletes a solver given that solver's id
-- Author: Todd Elvers	
CREATE PROCEDURE DeleteSolverById(IN _solverId BIGINT)
	BEGIN
		DELETE FROM solvers
		WHERE id = _solverId;
	END //	
	
-- Retrieves all solvers belonging to a space
-- Author: Tyler Jensen
CREATE PROCEDURE GetSpaceSolversById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE id IN
				(SELECT solver_id
				FROM solver_assoc
				WHERE space_id = _id)
		ORDER BY name;
	END //	
	
-- Retrieves the solver with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE GetSolverById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE id = _id;
	END //
	
-- Removes the association between a solver and a given space
-- Author: Todd Elvers
CREATE PROCEDURE RemoveSolverFromSpace(IN _solverId BIGINT, IN _spaceId BIGINT)
	BEGIN
		DELETE FROM solver_assoc
		WHERE solver_id = _solverId
		AND space_id = _spaceId;
	END // 

-- Updates the details associated with a given solver
-- Author: Todd Elvers
CREATE PROCEDURE UpdateSolverDetails(IN _solverId BIGINT, IN _name VARCHAR(32), IN _description TEXT, IN _downloadable BOOLEAN)
	BEGIN
		UPDATE solvers
		SET name = _name,
		description = _description,
		downloadable = _downloadable
		WHERE id = _solverId;
	END //
	
	
	
	

	
/*************************************************************************
*********************** SPACE STORED PROCEDURES **************************
*************************************************************************/

-- Adds a new space with the given information
-- Author: Tyler Jensen
CREATE PROCEDURE AddSpace(IN _name VARCHAR(32), IN _desc TEXT, IN _locked TINYINT(1), IN _permission BIGINT, IN _parent BIGINT, OUT id BIGINT)
	BEGIN		
		INSERT INTO spaces (name, created, description, locked, default_permission)
		VALUES (_name, SYSDATE(), _desc, _locked, _permission);
		SELECT LAST_INSERT_ID() INTO id;
		INSERT INTO closure (ancestor, descendant)	-- Update closure table
			SELECT ancestor, id FROM closure
			WHERE descendant = _parent
			UNION ALL SELECT _parent, id; 
	END //

-- Adds an association between two spaces
-- Author: Tyler Jensen
CREATE PROCEDURE AssociateSpaces(IN _parentId BIGINT, IN _childId BIGINT, IN _permission BIGINT)
	BEGIN		
		INSERT INTO set_assoc
		VALUES (_parentId, _childId, _permission);
	END //
	
-- Gets all the descendants of a space
-- Author: Todd Elvers
CREATE PROCEDURE GetDescendantsOfSpace(IN _spaceId BIGINT)
	BEGIN
		SELECT descendant
		FROM closure
		WHERE ancestor = _spaceId;
	END //
	
-- Gets all the leaders of a space
-- Author: Todd Elvers
CREATE PROCEDURE GetLeadersBySpaceId(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM users
		WHERE email IN
			(SELECT DISTINCT users.email
			FROM spaces
			JOIN user_assoc ON spaces.id=user_assoc.space_id
			JOIN users ON user_assoc.user_id=users.id
			JOIN permissions ON user_assoc.permission=permissions.id
			WHERE spaces.id=_id AND permissions.is_leader=1);
	END //
	
-- Returns basic space information for the space with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE GetSpaceById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM spaces
		WHERE id = _id;
	END //
	
-- Returns all spaces belonging to the space with the given id.
-- Author: Tyler Jensen
CREATE PROCEDURE GetSubSpacesById(IN _spaceId BIGINT, IN _userId BIGINT)
	BEGIN
		IF _spaceId <= 0 THEN	-- If we get an invalid ID, return the root space (the space with the mininum ID
			SELECT *
			FROM spaces
			WHERE id = 
				(SELECT MIN(id)
				FROM spaces);
		ELSE				-- Else find all children spaces that the user is apart of
			SELECT *
			FROM spaces
			WHERE id IN
				(SELECT child_id
				 FROM set_assoc 
					JOIN user_assoc ON set_assoc.child_id=user_assoc.space_id
				 WHERE set_assoc.space_id =_spaceId AND user_assoc.user_id=_userId)
			ORDER BY name;
		END IF;
	END //
	
-- Returns all spaces that are a subspace of the root
-- Author: Todd Elvers
CREATE PROCEDURE GetSubSpacesOfRoot()
	BEGIN
		SELECT *
		FROM spaces
		WHERE id IN
				(SELECT child_id
				 FROM set_assoc
				 WHERE space_id=1)
		ORDER BY name;
	END //
	
-- Removes the association between a space and a subspace and deletes the subspace
-- Author: Todd Elvers
CREATE PROCEDURE RemoveSubspace(IN _subspaceId BIGINT)
	BEGIN
		DELETE FROM spaces
		WHERE id = _subspaceId;
	END //
	
-- Updates the name of the space with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateSpaceName(IN _id BIGINT, IN _name VARCHAR(32))
	BEGIN
		UPDATE spaces
		SET name = _name
		WHERE id = _id;
	END //

-- Updates the name of the spacewith the given id
-- Author: Tyler Jensen
CREATE PROCEDURE UpdateSpaceDescription(IN _id BIGINT, IN _desc TEXT)
	BEGIN
		UPDATE spaces
		SET description = _desc
		WHERE id = _id;
	END //
	

	
	
	
	

/*************************************************************************
*********************** USER STORED PROCEDURES ***************************
*************************************************************************/

-- Begins the registration process by adding a user to the USERS table
-- Author: Todd Elvers
CREATE PROCEDURE AddUser(IN _first_name VARCHAR(32), IN _last_name VARCHAR(32), IN _email VARCHAR(64), IN _institute VARCHAR(64), IN _password VARCHAR(128), OUT _id BIGINT)
	BEGIN		
		INSERT INTO users(first_name, last_name, email, institution, created, password)
		VALUES (_first_name, _last_name, _email, _institute, SYSDATE(), _password);
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Adds an association between a user and a space
-- Author: Tyler Jensen
CREATE PROCEDURE AddUserToSpace(IN _userId BIGINT, IN _spaceId BIGINT, IN _proxy BIGINT, IN _permission BIGINT)
	BEGIN		
		INSERT INTO user_assoc (user_id, space_id, proxy, permission)
		VALUES (_userId, _spaceId, _proxy, _permission);		
	END //
	
-- Returns the (hashed) password of the user with the given user id
-- Author: Skylar Stark
CREATE PROCEDURE GetPasswordById(IN _id BIGINT)
	BEGIN
		SELECT password
		FROM users
		WHERE users.id = _id;
	END // 

-- Returns the user record with the given email address
-- Author: Tyler Jensen
CREATE PROCEDURE GetUserByEmail(IN _email VARCHAR(64))
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.email = _email;
	END //
	
-- Returns unregistered user corresponding to the given id
-- Author: Todd Elvers
CREATE PROCEDURE GetUnregisteredUserById(IN _id BIGINT)
	BEGIN
		SELECT * 
		FROM users 
		WHERE users.id = _id 
		AND users.email NOT IN
			(SELECT email 
			FROM user_roles);
	END //
	
-- Returns the user record with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE GetUserById(IN _id BIGINT)
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.id = _id;
	END //
	
-- Retrieves all users belonging to a space
-- Author: Tyler Jensen
CREATE PROCEDURE GetSpaceUsersById(IN _id BIGINT)
	BEGIN
		SELECT DISTINCT *
		FROM users
		WHERE id IN
				(SELECT user_id
				FROM user_assoc
				WHERE space_id = _id)
		ORDER BY first_name;
	END //
	
-- Updates the email address of the user with the given user id to the
-- given email address. The email address should already be validated
-- Author: Skylar Stark
CREATE PROCEDURE UpdateEmail(IN _id BIGINT, IN _email VARCHAR(64))
	BEGIN
		UPDATE users
		SET email = _email
		WHERE users.id = _id;
	END //
	

	
-- Updates the first name of the user with the given user id to the
-- given first name. The first name should already be validated.	
CREATE PROCEDURE UpdateFirstName(IN _id BIGINT, IN _firstname VARCHAR(32))
	BEGIN
		UPDATE users
		SET first_name = _firstname
		WHERE users.id = _id;
	END //
	
-- Updates the last name of the user with the given user id to the
-- given last name. The last name should already be validated
-- Author: Skylar Stark
CREATE PROCEDURE UpdateLastName(IN _id BIGINT, IN _lastname VARCHAR(32))
	BEGIN
		UPDATE users
		SET last_name = _lastname
		WHERE users.id = _id;
	END //

-- Updates the institution of the user with the given user id to the
-- given institution. The institution should already be validated
-- Author: Skylar Stark
CREATE PROCEDURE UpdateInstitution(IN _id BIGINT, IN _institution VARCHAR(64))
	BEGIN
		UPDATE users
		SET institution = _institution
		WHERE users.id = _id;
	END //

-- Updates the password of the user with the given user id to the
-- given (already hashed and validated) password.
-- Author: Skylar Stark
CREATE PROCEDURE UpdatePassword(IN _id BIGINT, IN _password VARCHAR(128))
	BEGIN
		UPDATE users
		SET password = _password
		WHERE users.id = _id;
	END //
	
-- Sets a new password for a given user
-- Author: Todd Elvers
CREATE PROCEDURE SetPasswordByUserId(IN _id BIGINT, IN _password VARCHAR(128))
	BEGIN
		UPDATE users
		SET password = _password
		WHERE users.id = _id;
	END //

	
	
	
	
	

/*************************************************************************
********************** WEBSITE STORED PROCEDURES *************************
*************************************************************************/
	
-- Adds a website that is associated with a user
-- Author: Skylar Stark	
CREATE PROCEDURE AddUserWebsite(IN _userId BIGINT, IN _url TEXT, IN _name VARCHAR(64))
	BEGIN
		INSERT INTO website(user_id, url, name)
		VALUES(_userId, _url, _name);
	END //
	
-- Adds a website that is associated with a solver
-- Author: Tyler Jensen	
CREATE PROCEDURE AddSolverWebsite(IN _solverId BIGINT, IN _url TEXT, IN _name VARCHAR(64))
	BEGIN
		INSERT INTO website(solver_id, url, name)
		VALUES(_solverId, _url, _name);
	END //
	
-- Adds a website that is associated with a space (community)
-- Author: Tyler Jensen
CREATE PROCEDURE AddSpaceWebsite(IN _spaceId BIGINT, IN _url TEXT, IN _name VARCHAR(64))
	BEGIN
		INSERT INTO website(space_id, url, name)
		VALUES(_spaceId, _url, _name);
	END //
	
-- Deletes the website with the given website id
-- Author: Skylar Stark
CREATE PROCEDURE DeleteUserWebsite(IN _id BIGINT, IN _userId BIGINT)
	BEGIN
		DELETE FROM website
		WHERE id = _id AND user_id = _userId;
	END //

-- Deletes a website associated with a space
-- Author: Todd Elvers
CREATE PROCEDURE DeleteSpaceWebsite(IN _id BIGINT, IN _spaceId BIGINT)
	BEGIN
		DELETE FROM website
		WHERE id = _id AND space_id = _spaceId;
	END //
	
-- Returns all websites associated with the user with the given user id
-- Author: Skylar Stark
CREATE PROCEDURE GetWebsitesByUserId(IN _userid BIGINT)
	BEGIN
		SELECT id, name, url
		FROM website
		WHERE website.user_id = _userid
		ORDER BY name;
	END //

-- Gets all websites that are associated with the solver with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE GetWebsitesBySolverId(IN _id BIGINT)
	BEGIN
		SELECT id, name, url
		FROM website
		WHERE website.solver_id = _id;
	END //

-- Gets all websites that are associated with the space with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE GetWebsitesBySpaceId(IN _id BIGINT)
	BEGIN
		SELECT id, name, url
		FROM website
		WHERE website.space_id = _id;
	END //	

DELIMITER ; -- This should always be at the end of this file