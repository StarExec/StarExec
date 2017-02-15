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
CREATE PROCEDURE AddUserAuthorized(IN _firstName VARCHAR(32), IN _lastName VARCHAR(32), IN _email VARCHAR(64), IN _institute VARCHAR(64), IN _password VARCHAR(128), IN _diskQuota BIGINT(20),IN _role VARCHAR(24), IN _pairQuota INT, OUT _id INT)
	BEGIN
		INSERT INTO users(email, first_name, last_name, institution, created, password, disk_quota, job_pair_quota)
		VALUES (_email, _firstName, _lastName, _institute, SYSDATE(), _password, _diskQuota, _pairQuota);
		SELECT LAST_INSERT_ID() INTO _id;
		
		INSERT INTO user_roles(email, role)
		VALUES (_email, _role);
	END //


	
-- Removes the user given by _userId from every space in the hierarchy rooted at _spaceId that _requestUserId can see
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS RemoveUserFromSpaceHierarchy;
CREATE PROCEDURE RemoveUserFromSpaceHierarchy(IN _userId INT, IN _spaceId INT, IN _requestUserId INT)
	BEGIN
		-- Remove the permission associated with this user/community
		DELETE FROM permissions
			WHERE id IN (SELECT permission FROM user_assoc 
						JOIN closure ON descendant=_spaceId
						JOIN user_assoc AS ua ON ( (ua.user_id = _requestUserId OR spaces.public_access) AND ua.space_id=descendant)
						WHERE user_id = _userId);
			
		DELETE FROM user_assoc
		WHERE user_id=_userId AND space_id IN (SELECT descendant
		FROM closure JOIN spaces ON descendant=spaces.id
		JOIN user_assoc ON ( (user_assoc.user_id = _requestUserId OR spaces.public_access) AND user_assoc.space_id=descendant)
		WHERE ancestor=_spaceId);
		
		
	END // 	
	
-- Adds the user given by _userId to every space in the hierarchy rooted at _spaceId that _requestUserId can see
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS AddUserToSpaceHierarchy;
CREATE PROCEDURE AddUserToSpaceHierarchy(IN _userId INT, IN _spaceId INT, IN _requestUserId INT)
	BEGIN
		DECLARE _newPermId INT;
		DECLARE _pid INT;
		
		-- Copy the default permission for the community 					
		SELECT default_permission FROM spaces WHERE id=_spaceId INTO _pid;
		CALL CopyPermissions(_pid, _newPermId);
		
		INSERT IGNORE INTO user_assoc (user_id, space_id, permission)
		SELECT _userId, descendant, _newPermId
		FROM closure JOIN spaces ON descendant=spaces.id
		JOIN user_assoc ON ( (user_assoc.user_id = _requestUserId OR spaces.public_access) AND user_assoc.space_id=descendant)
		WHERE ancestor=_spaceId;
		
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
		FROM users JOIN user_roles ON users.email = user_roles.email
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
				OR		users.institution						LIKE 	CONCAT('%', _query, '%')
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

DROP PROCEDURE IF EXISTS UnsubscribeUserFromErrorLogs;
CREATE PROCEDURE UnsubscribeUserFromErrorLogs(IN _id INT)
	BEGIN
		UPDATE users
		SET subscribed_to_error_logs=FALSE
		WHERE id=_id;
	END //

DROP PROCEDURE IF EXISTS SubscribeUserToErrorLogs;
CREATE PROCEDURE SubscribeUserToErrorLogs(IN _id INT)
	BEGIN
		UPDATE users
		SET subscribed_to_reports=TRUE
		WHERE id=_id;
	END //

DROP PROCEDURE IF EXISTS GetAllUsersSubscribedToErrorLogs;
CREATE PROCEDURE GetAllUsersSubscribedToErrorLogs(IN _id INT)
	BEGIN
		SELECT *
		FROM users
		WHERE subscribed_to_error_logs=TRUE;
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
	
-- Gets the default page size for a given user
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetDefaultPageSize;
CREATE PROCEDURE GetDefaultPageSize(IN _id INT)
	BEGIN
		SELECT default_page_size AS pageSize
		FROM users
		WHERE id=_id;
	END //

-- Sets the default page size for a user, which is the number of rows per datatable they see by default
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS SetDefaultPageSize;
CREATE PROCEDURE SetDefaultPageSize(IN _id INT, IN _size INT)
	BEGIN
		UPDATE users
		SET default_page_size=_size
		WHERE id=_id;
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
	
-- Sets the user pair quota limit for the given user
DROP PROCEDURE IF EXISTS UpdateUserPairQuota;
CREATE PROCEDURE UpdateUserPairQuota(IN _userId INT, IN _newQuota INT)
	BEGIN
		UPDATE users
		SET job_pair_quota = _newQuota
		WHERE id = _userId;
	END //
	
-- Gets the total disk usage for a given user.
DROP PROCEDURE IF EXISTS GetUserDiskUsage;
CREATE PROCEDURE GetUserDiskUsage(IN _userID INT)
	BEGIN
		SELECT disk_size FROM users WHERE id=_userID;
	END //

-- Sums up the disk_size columns of solvers, benchmarks, and jobs and places that value in the
-- the user disk_size column. Returns the difference between the old and new values in _sizeDelta
DROP PROCEDURE IF EXISTS UpdateUserDiskUsage;
CREATE PROCEDURE UpdateUserDiskUsage(IN _userID INT, OUT _sizeDelta BIGINT)
	BEGIN
		DECLARE _sumDiskSize BIGINT;
		DECLARE _userDiskSize BIGINT;
		SELECT COALESCE(SUM(disk_size),0) AS disk_usage FROM
		(SELECT disk_size FROM solvers WHERE user_id=_userID AND deleted=false
		UNION ALL 
		SELECT disk_size FROM benchmarks WHERE user_id=_userID AND deleted=false
		UNION ALL
		SELECT disk_size FROM jobs WHERE user_id=_userID AND deleted=false) AS tmp INTO _sumDiskSize;
		
		SELECT disk_size FROM users WHERE id=_userID INTO _userDiskSize;
		
		SELECT (_userDiskSize-_sumDiskSize) INTO _sizeDelta;
		
		UPDATE users SET disk_size=_sumDiskSize WHERE id=_userID;
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

-- Gets every user subscribed to the weekly reports
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetAllUsersSubscribedToReports;
CREATE PROCEDURE GetAllUsersSubscribedToReports()
	BEGIN
		SELECT *
		FROM users
			INNER JOIN user_roles AS roles on users.email = roles.email
		WHERE subscribed_to_reports = TRUE;
	END //

-- Gets every user whose role is 'admin'
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
		DELETE FROM logins WHERE user_id=_userId;
		DELETE FROM users WHERE id=_userId;
	END //
	
-- Sets the role of the given user to the given value
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS ChangeUserRole;
CREATE PROCEDURE ChangeUserRole(IN _userId INT, IN _role VARCHAR(24))
	BEGIN
		UPDATE user_roles
		JOIN users ON users.email=user_roles.email
		SET role=_role
		WHERE id=_userId;
	END //

DROP PROCEDURE IF EXISTS SetUserReportSubscription;
CREATE PROCEDURE SetUserReportSubscription(IN _userId INT, IN _willBeSubscribed BOOLEAN)
	BEGIN
		UPDATE users
		SET subscribed_to_reports = _willBeSubscribed
		WHERE id = _userId;
	END //
	
	

DELIMITER ; -- This should always be at the end of this file
