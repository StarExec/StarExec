-- Description: This file contains all user-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Begins the registration process by adding a user to the USERS table
-- Makes their role "unauthorized"
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS AddUser;
CREATE PROCEDURE AddUser(IN _firstName VARCHAR(32), IN _lastName VARCHAR(32), IN _email VARCHAR(64), IN _institute VARCHAR(64), IN _password VARCHAR(128),  IN _diskQuota BIGINT(20), OUT _id INT)
	BEGIN		
		INSERT INTO users(email, first_name, last_name, institution, created, password, disk_quota)
		VALUES (_email, _firstName, _lastName, _institute, SYSDATE(), _password, _diskQuota);
		
		SELECT LAST_INSERT_ID() INTO _id;
		
		INSERT INTO user_roles(email, role)
		VALUES (_email, 'unauthorized');
	END //
	
DROP PROCEDURE IF EXISTS AddUserAuthorized;
CREATE PROCEDURE AddUserAuthorized(IN _firstName VARCHAR(32), IN _lastName VARCHAR(32), IN _email VARCHAR(64), IN _institute VARCHAR(64), IN _password VARCHAR(128), IN _diskQuota BIGINT(20),IN _role VARCHAR(24), OUT _id INT)
	BEGIN
		INSERT INTO users(email, first_name, last_name, institution, created, password, disk_quota)
		VALUES (_email, _firstName, _lastName, _institute, SYSDATE(), _password, _diskQuota);
		SELECT LAST_INSERT_ID() INTO _id;
		
		INSERT INTO user_roles(email, role)
		VALUES (_email, _role);
	END //

-- Adds a user to a community directly (used through admin interface)
-- Skips the community request stage
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS AddUserToCommunity;
CREATE PROCEDURE AddUserToCommunity(IN _userId INT, IN _communityId INT)
	BEGIN
		DECLARE _newPermId INT;
		DECLARE _pid INT;
		
		-- Copy the default permission for the community 					
		SELECT default_permission FROM spaces WHERE id=_communityId INTO _pid;
		CALL CopyPermissions(_pid, _newPermId);
		
		INSERT INTO user_assoc(user_id, space_id, permission)
		VALUES(_userId, _communityId, _newPermId);
		
	END //

-- Adds an association between a user and a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddUserToSpace;
CREATE PROCEDURE AddUserToSpace(IN _userId INT, IN _spaceId INT)
	BEGIN		
		DECLARE _newPermId INT;
		DECLARE _pid INT;
		IF NOT EXISTS(SELECT * FROM user_assoc WHERE user_id = _userId AND space_id = _spaceId) THEN			
			-- Copy the default permission for the community 					
			SELECT default_permission FROM spaces WHERE id=_spaceId INTO _pid;
			CALL CopyPermissions(_pid, _newPermId);
			
			INSERT INTO user_assoc (user_id, space_id, permission)
			VALUES (_userId, _spaceId, _newPermId);
		END IF;
	END //
		
	
-- Gets the fewest necessary Users in order to service a client's
-- request for the next page of Users in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNextPageOfUsersAdmin;
CREATE PROCEDURE GetNextPageOfUsersAdmin(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _query TEXT, IN _publicUserId INT)
	BEGIN
		-- If _query is empty, get next page of Users without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name,
						role
						
				FROM	users NATURAL JOIN user_roles WHERE (id != _publicUserId)

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
						CONCAT(first_name, ' ', last_name) AS full_name,
						role
				FROM	users NATURAL JOIN user_roles WHERE (id != _publicUserId)
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
						CONCAT(first_name, ' ', last_name) AS full_name,
						role
				
				FROM	users NATURAL JOIN user_roles WHERE (id != _publicUserId)
							
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
						CONCAT(first_name, ' ', last_name) AS full_name,
						role
				FROM	users NATURAL JOIN user_roles WHERE (id != _publicUserId)
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
	
-- Returns the number of users in the entire system
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetUserCount;
CREATE PROCEDURE GetUserCount()
	BEGIN
		SELECT COUNT(*) as userCount
		FROM users;
	END //
	
-- Returns the number of users in a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetUserCountInSpace;
CREATE PROCEDURE GetUserCountInSpace(IN _spaceId INT)
	BEGIN
		SELECT 	COUNT(*) AS userCount
		FROM 	user_assoc
		WHERE 	space_id=_spaceId;
	END //
-- Returns the number of users in a given space that match a given query
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetUserCountInSpaceWithQuery;
CREATE PROCEDURE GetUserCountInSpaceWithQuery(IN _spaceId INT, IN _query TEXT)
	BEGIN
		SELECT 	COUNT(*) AS userCount
		FROM 	user_assoc
			JOIN users ON users.id=user_id
		WHERE 	space_id=_spaceId AND
				(CONCAT(users.first_name, ' ', users.last_name)	LIKE	CONCAT('%', _query, '%')
				OR		users.institution							LIKE 	CONCAT('%', _query, '%')
				OR		users.email								LIKE 	CONCAT('%', _query, '%')); 
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
		FROM user_assoc
			JOIN users ON users.id=user_assoc.user_id
		WHERE _id=user_assoc.space_id
		ORDER BY first_name;
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

-- Sets the user disk quota limit to the value of _newBytes
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
	
-- Gets the user that owns the given job
DROP PROCEDURE IF EXISTS GetNameofUserByJob;
CREATE PROCEDURE GetNameofUserByJob(IN _jobId INT)
	BEGIN
		SELECT *
		FROM users
			INNER JOIN jobs AS owner ON users.id = owner.user_id
		WHERE owner.id = _jobId;
	END //
	
DROP PROCEDURE IF EXISTS GetAdmins;
CREATE PROCEDURE GetAdmins()
	BEGIN
		SELECT *
		FROM users
			INNER JOIN user_roles AS roles ON users.email = roles.email
		WHERE roles.role = "admin";
	END //

-- Checks to see whether the given user is a member of the given community
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS IsMemberOfCommunity;
CREATE PROCEDURE IsMemberOfCommunity(IN _userId INT, IN communityId INT)
	BEGIN
		SELECT COUNT(*) AS spaceCount FROM closure 
			JOIN user_assoc AS assoc ON assoc.space_id=descendant
		WHERE assoc.user_id=_userId AND ancestor=communityId;
	END //
	
	
-- Deletes a user from the database. Right now, this is only used to get rid of temporary test users
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS DeleteUser;
CREATE PROCEDURE DeleteUser(IN _userId INT)
	BEGIN
		DELETE FROM users WHERE id=_userId;
	END //
	
-- Suspends a user
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS SuspendUser;
CREATE PROCEDURE SuspendUser(IN _userEmail VARCHAR(64))
	BEGIN
		UPDATE user_roles
		SET role = "suspended"
		WHERE email = _userEmail;
	END //
	
-- Suspends a suspended user
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS ReinstateUser;
CREATE PROCEDURE ReinstateUser(IN _userEmail VARCHAR(64))
	BEGIN
		UPDATE user_roles
		SET role = "user"
		WHERE email = _userEmail;
	END //
	

DELIMITER ; -- This should always be at the end of this file