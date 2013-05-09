-- Description: This file contains all user-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Begins the registration process by adding a user to the USERS table
-- Makes their role "unauthorized"
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddUser;
CREATE PROCEDURE AddUser(IN _first_name VARCHAR(32), IN _last_name VARCHAR(32), IN _email VARCHAR(64), IN _institute VARCHAR(64), IN _password VARCHAR(128),  IN _diskQuota BIGINT, IN _archiveType VARCHAR(8), OUT _id INT)
	BEGIN		
		INSERT INTO users(first_name, last_name, email, institution, created, password, disk_quota, pref_archive_type)
		VALUES (_first_name, _last_name, _email, _institute, SYSDATE(), _password, _diskQuota, _archiveType);
		SELECT LAST_INSERT_ID() INTO _id;
		
		INSERT INTO user_roles(email, role)
		VALUES (_email, 'unauthorized');
	END //

-- Adds an association between a user and a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddUserToSpace;
CREATE PROCEDURE AddUserToSpace(IN _userId INT, IN _spaceId INT, IN _proxy INT)
	BEGIN		
		DECLARE _newPermId INT;
		DECLARE _pid INT;
		IF NOT EXISTS(SELECT * FROM user_assoc WHERE user_id = _userId AND space_id = _spaceId) THEN			
			-- Copy the default permission for the community 					
			SELECT default_permission FROM spaces WHERE id=_spaceId INTO _pid;
			CALL CopyPermissions(_pid, _newPermId);
			
			INSERT INTO user_assoc (user_id, space_id, proxy, permission)
			VALUES (_userId, _spaceId, _proxy, _newPermId);
		END IF;
	END //
	
-- Gets the fewest necessary Users in order to service a client's
-- request for the next page of Users in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetNextPageOfUsers;
CREATE PROCEDURE GetNextPageOfUsers(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _query TEXT, IN _publicUserId INT)
	BEGIN
		-- If _query is empty, get next page of Users without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
						
				FROM	users WHERE (id != _publicUserId)
				
				-- Exclude Users that aren't in the specified space
				AND 	id 	IN (SELECT 	user_id
								FROM	user_assoc
								WHERE 	space_id = _spaceId)
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
				(CASE _colSortedOn
					WHEN 0 THEN full_name
					WHEN 1 THEN institution
					WHEN 2 THEN email
				END) ASC
				
				-- Shrink the results to only those required for the next page of Users
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
				FROM	users WHERE (id != _publicUserId)
				AND 	id 	IN (SELECT 	user_id
								FROM	user_assoc
								WHERE 	space_id = _spaceId)
				ORDER BY 
				(CASE _colSortedOn
					WHEN 0 THEN full_name
					WHEN 1 THEN institution
					WHEN 2 THEN email
				END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		-- Otherwise, ensure the target Users contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
				
				FROM	users WHERE (id != _publicUserId)
				
				-- Exclude Users that aren't in the specified space
				AND 	id 	IN (SELECT 	user_id
								FROM	user_assoc
								WHERE 	space_id = _spaceId)
							
				-- Exclude Users whose name and description don't contain the query string
				AND 	(CONCAT(first_name, ' ', last_name)	LIKE	CONCAT('%', _query, '%')
				OR		institution							LIKE 	CONCAT('%', _query, '%')
				OR		email								LIKE 	CONCAT('%', _query, '%'))
								
				-- Order results depending on what column is being sorted on
				ORDER BY 
				(CASE _colSortedOn
					WHEN 0 THEN full_name
					WHEN 1 THEN institution
					WHEN 2 THEN email
				END) ASC
					 
				-- Shrink the results to only those required for the next page of Users
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
				FROM	users WHERE (id != _publicUserId)
				AND	id 	IN (SELECT 	user_id
								FROM	user_assoc
								WHERE 	space_id = _spaceId)
				AND 	(CONCAT(first_name, ' ', last_name)	LIKE	CONCAT('%', _query, '%')
				OR		institution							LIKE 	CONCAT('%', _query, '%')
				OR		email								LIKE 	CONCAT('%', _query, '%'))
				ORDER BY 
				(CASE _colSortedOn
					WHEN 0 THEN full_name
					WHEN 1 THEN institution
					WHEN 2 THEN email
				END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
	
	
-- Returns the (hashed) password of the user with the given user id
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS GetPasswordById;
CREATE PROCEDURE GetPasswordById(IN _id INT)
	BEGIN
		SELECT password
		FROM users
		WHERE users.id = _id;
	END // 

	
-- Returns unregistered user corresponding to the given id
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetUnregisteredUserById;
CREATE PROCEDURE GetUnregisteredUserById(IN _id INT)
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.id = _id 
		AND user_roles.role = 'unauthorized';
	END //
	
-- Returns the number of users in a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetUserCountInSpace;
CREATE PROCEDURE GetUserCountInSpace(IN _spaceId INT)
	BEGIN
		SELECT 	COUNT(*) AS userCount
		FROM 	users
		WHERE 	id	IN (SELECT	user_id
						FROM 	user_assoc
						WHERE 	space_id = _spaceId);
	END //
	
-- Returns the user record with the given email address
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetUserByEmail;
CREATE PROCEDURE GetUserByEmail(IN _email VARCHAR(64))
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.email = _email;
	END //
	
	
-- Returns the user record with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetUserById;
CREATE PROCEDURE GetUserById(IN _id INT)
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.id = _id;
	END //
	
-- Retrieves all users belonging to a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpaceUsersById;
CREATE PROCEDURE GetSpaceUsersById(IN _id INT)
	BEGIN
		SELECT DISTINCT *
		FROM users
		WHERE id IN
				(SELECT user_id
				FROM user_assoc
				WHERE space_id = _id)
		ORDER BY first_name;
	END //
	
	
-- Updates the preferred archive type of the user with the given user
-- id to the given archive type.
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdateArchiveType;
CREATE PROCEDURE UpdateArchiveType(IN _id INT, IN _archiveType VARCHAR(8))
	BEGIN
		UPDATE users
		SET pref_archive_type = _archiveType
		WHERE users.id = id;
	END //

	
-- Updates the email address of the user with the given user id to the
-- given email address. The email address should already be validated
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdateEmail;
CREATE PROCEDURE UpdateEmail(IN _id INT, IN _email VARCHAR(64))
	BEGIN
		UPDATE users
		SET email = _email
		WHERE users.id = _id;
	END //
	
-- Updates the first name of the user with the given user id to the
-- given first name. The first name should already be validated.	
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdateFirstName;
CREATE PROCEDURE UpdateFirstName(IN _id INT, IN _firstname VARCHAR(32))
	BEGIN
		UPDATE users
		SET first_name = _firstname
		WHERE users.id = _id;
	END //
	
-- Updates the last name of the user with the given user id to the
-- given last name. The last name should already be validated
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdateLastName;
CREATE PROCEDURE UpdateLastName(IN _id INT, IN _lastname VARCHAR(32))
	BEGIN
		UPDATE users
		SET last_name = _lastname
		WHERE users.id = _id;
	END //

-- Updates the institution of the user with the given user id to the
-- given institution. The institution should already be validated
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdateInstitution;
CREATE PROCEDURE UpdateInstitution(IN _id INT, IN _institution VARCHAR(64))
	BEGIN
		UPDATE users
		SET institution = _institution
		WHERE users.id = _id;
	END //

-- Updates the password of the user with the given user id to the
-- given (already hashed and validated) password.
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdatePassword;
CREATE PROCEDURE UpdatePassword(IN _id INT, IN _password VARCHAR(128))
	BEGIN
		UPDATE users
		SET password = _password
		WHERE users.id = _id;
	END //
	
-- Sets a new password for a given user
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS SetPasswordByUserId;
CREATE PROCEDURE SetPasswordByUserId(IN _id INT, IN _password VARCHAR(128))
	BEGIN
		UPDATE users
		SET password = _password
		WHERE users.id = _id;
	END //

-- Increments the disk_quota attribute of the users table by the value of _newBytes
-- for the given user
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateUserDiskQuota;
CREATE PROCEDURE UpdateUserDiskQuota(IN _userId INT, IN _newQuota BIGINT)
	BEGIN
		UPDATE users
		SET disk_quota = _newQuota
		WHERE id = _userId;
	END //
	
-- Returns the number of bytes a given user's benchmarks is consuming on disk
-- Author: Eric Burns	
	
DROP PROCEDURE IF EXISTS GetUserBenchmarkDiskUsage;
CREATE PROCEDURE GetUserBenchmarkDiskUsage(IN _userID INT)
	BEGIN
		SELECT sum(benchmarks.disk_size) AS disk_usage
		FROM   benchmarks
		WHERE  benchmarks.user_id = _userId;
	END //		

-- Returns the number of bytes a given user's benchmarks is consuming on disk
-- Author: Eric Burns	
	
DROP PROCEDURE IF EXISTS GetUserSolverDiskUsage;
CREATE PROCEDURE GetUserSolverDiskUsage(IN _userID INT)
	BEGIN
		SELECT sum(solvers.disk_size) AS disk_usage
		FROM   solvers
		WHERE  solvers.user_id = _userId;
	END //	
	

	
-- Returns one record if a given user is a member of a particular space
-- otherwise it returns an empty set
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS IsMemberOfSpace;
CREATE PROCEDURE IsMemberOfSpace(IN _userId INT, IN _spaceId INT)
	BEGIN
		SELECT *
		FROM  user_assoc
		WHERE user_id  = _userId
		AND   space_id = _spaceId;
	END // 



DELIMITER ; -- This should always be at the end of this file