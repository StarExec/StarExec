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
	
DROP PROCEDURE IF EXISTS GetNameofUserByJob;
CREATE PROCEDURE GetNameofUserByJob(IN _jobId INT)
	BEGIN
		SELECT *
		FROM users
			INNER JOIN jobs AS owner ON users.id = owner.user_id
		WHERE owner.id = _jobId;
	END //



DELIMITER ; -- This should always be at the end of this file