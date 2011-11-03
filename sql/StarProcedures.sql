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
CREATE PROCEDURE GetSubSpacesById(IN _id BIGINT)
	BEGIN
		IF _id <= 0 THEN	-- If we get an invalid ID, return the root space (the space with the mininum ID
			SELECT *
			FROM spaces
			WHERE id = 
				(SELECT MIN(id)
				FROM spaces);
		ELSE				-- Else find all children spaces
			SELECT *
			FROM spaces
			WHERE id IN
				(SELECT child_id
				FROM set_assoc
				WHERE space_id = _id)
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
	

DELIMITER ; -- This should always be at the end of this file