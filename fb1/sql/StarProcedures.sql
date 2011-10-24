-- Description: This file contains all stored procedures for the starexec database

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

CREATE PROCEDURE GetPasswordById(IN _id BIGINT)
	BEGIN
		SELECT password
		FROM users
		WHERE users.id = _id;
	END // 

CREATE PROCEDURE GetUserById(IN _id BIGINT)
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.id = _id;
	END //

CREATE PROCEDURE GetUserByEmail(IN _email VARCHAR(64))
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.email = _email;
	END //
CREATE PROCEDURE GetWebsitesById(IN _id BIGINT)
	BEGIN
		SELECT name, url
		FROM website
		WHERE website.user_id = _id;
	END //

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
		SELECT *
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
	
CREATE PROCEDURE UpdateFirstName(IN _id BIGINT, IN _firstname VARCHAR(32))
	BEGIN
		UPDATE users
		SET first_name = _firstname
		WHERE users.id = _id;
	END //

CREATE PROCEDURE UpdateLastName(IN _id BIGINT, IN _lastname VARCHAR(32))
	BEGIN
		UPDATE users
		SET last_name = _lastname
		WHERE users.id = _id;
	END //

CREATE PROCEDURE UpdateInstitution(IN _id BIGINT, IN _institution VARCHAR(64))
	BEGIN
		UPDATE users
		SET institution = _institution
		WHERE users.id = _id;
	END //

CREATE PROCEDURE UpdatePassword(IN _id BIGINT, IN _password VARCHAR(128))
	BEGIN
		UPDATE users
		SET password = _password
		WHERE users.id = _id;
	END //
	
DELIMITER ; -- This should always be at the end of this file