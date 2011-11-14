-- Description: This file contains all stored procedures for the starexec database

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Returns the (hashed) password of the user with the given user id
-- Author: Skylar Stark
CREATE PROCEDURE GetPasswordById(IN _id BIGINT)
	BEGIN
		SELECT password
		FROM users
		WHERE users.id = _id;
	END // 

-- Returns the user record with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE GetUserById(IN _id BIGINT)
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
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
CREATE PROCEDURE GetSolverWebsitesById(IN _id BIGINT)
	BEGIN
		SELECT id, name, url
		FROM website
		WHERE website.solver_id = _id;
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

-- Returns basic space information for the space with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE GetSpaceById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM spaces
		WHERE id = _id;
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

-- Retrieves all benchmarks belonging to a space
-- Author: Tyler Jensen
CREATE PROCEDURE GetSpaceBenchmarksById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM benchmarks
		WHERE id IN
				(SELECT bench_id
				FROM bench_assoc
				WHERE space_id = _id)
		ORDER BY name;
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

-- Updates the first name of the user with the given user id to the
-- given first name. The first name should already be validated
-- Author: Skylar Stark
-- Retrieves the benchmark with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE GetBenchmarkById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM benchmarks
		WHERE id = _id;
	END //	
	
-- Retrieves the solver with the given id
-- Author: Tyler Jensen
CREATE PROCEDURE GetSolverById(IN _id BIGINT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE id = _id;
	END //	
	
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
	
CREATE PROCEDURE AddUser(IN _first_name VARCHAR(32), IN _last_name VARCHAR(32), IN _email VARCHAR(64), IN _institute VARCHAR(64), IN _password VARCHAR(128),  OUT id BIGINT, OUT stamp TIMESTAMP)
	BEGIN		
		SELECT SYSDATE() INTO stamp;
		INSERT INTO users(first_name, last_name, email, institution, created, password)
		VALUES (_first_name, _last_name, _email, _institute, stamp, _password);
		SELECT LAST_INSERT_ID() INTO id;
		INSERT INTO user_roles(email, role) 
		VALUES(_email, 'user');
	END //
	
CREATE PROCEDURE AddCode(IN _id BIGINT, IN _code VARCHAR(36), IN _created TIMESTAMP)
	BEGIN
		INSERT INTO verify(user_id, code, created)
		VALUES (_id, _code, _created);
	END //
		
CREATE PROCEDURE GetCode(IN _id BIGINT)
	BEGIN
		SELECT code
		FROM verify
		WHERE user_id = _id;
	END //	
			
CREATE PROCEDURE VerifyUser(IN _id BIGINT)
	BEGIN
		UPDATE users
		SET verified = 1
		WHERE id = _id;
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

-- Adds a website that is associated with a user
-- Author: Skylar Stark	
CREATE PROCEDURE AddUserWebsite(IN _userId BIGINT, IN _url TEXT, IN _name VARCHAR(64))
	BEGIN
		INSERT INTO website(user_id, url, name)
		VALUES(_userId, _url, _name);
	END //

-- Deletes the website with the given website id
-- Author: Skylar Stark
CREATE PROCEDURE DeleteUserWebsite(IN _id BIGINT, IN _userId BIGINT)
	BEGIN
		DELETE FROM website
		WHERE id = _id AND user_id = _userId;
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
	
-- Adds a new space with the given information
-- Author: Tyler Jensen
CREATE PROCEDURE AddSpace(IN _name VARCHAR(32), IN _desc TEXT, IN _locked TINYINT(1), IN _permission BIGINT, OUT id BIGINT)
	BEGIN		
		INSERT INTO spaces (name, created, description, locked, default_permission)
		VALUES (_name, SYSDATE(), _desc, _locked, _permission);
		SELECT LAST_INSERT_ID() INTO id;
	END //

-- Adds an association between a user and a space
-- Author: Tyler Jensen
CREATE PROCEDURE AddUserToSpace(IN _userId BIGINT, IN _spaceId BIGINT, IN _proxy BIGINT, IN _permission BIGINT)
	BEGIN		
		INSERT INTO user_assoc (user_id, space_id, proxy, permission)
		VALUES (_userId, _spaceId, _proxy, _permission);		
	END //
	
-- Adds an association between two spaces
-- Author: Tyler Jensen
CREATE PROCEDURE AssociateSpaces(IN _parentId BIGINT, IN _childId BIGINT, IN _permission BIGINT)
	BEGIN		
		INSERT INTO set_assoc
		VALUES (_parentId, _childId, _permission);
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

-- Adds a new historical record to the logins table which tracks all user logins
-- Author: Tyler Jensen
CREATE PROCEDURE LoginRecord(IN _userId BIGINT, IN _ipAddress VARCHAR(15), IN _agent TEXT)
	BEGIN		
		INSERT INTO logins (user_id, login_date, ip_address, browser_agent)
		VALUES (_userId, SYSDATE(), _ipAddress, _agent);
	END //
	
DELIMITER ; -- This should always be at the end of this file