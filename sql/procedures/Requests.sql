-- Description: This file contains all stored procedures used for requesting membership in a community, registering, and the resetting of passwords
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement



-- Adds an activation code for a specific user
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddCode;
CREATE PROCEDURE AddCode(IN _id INT, IN _code VARCHAR(36))
	BEGIN
		INSERT INTO verify(user_id, code, created)
		VALUES (_id, _code, SYSDATE());
	END //
	
-- Adds a request to join a community, provided the user isn't already a part of that community
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddCommunityRequest;
CREATE PROCEDURE AddCommunityRequest(IN _id INT, IN _community INT, IN _code VARCHAR(36), IN _message VARCHAR(512))
	BEGIN
		IF NOT EXISTS(SELECT * FROM user_assoc WHERE user_id = _id AND space_id = _community) THEN
			INSERT INTO community_requests(user_id, community, code, message, created)
			VALUES (_id, _community, _code, _message, SYSDATE());
		END IF;
	END //

-- Adds a user to USER_ASSOC, deletes their entry in INVITES, and makes their
-- role 'user' if not so already
-- Author: Todd Elvers & Skylar Stark
DROP PROCEDURE IF EXISTS ApproveCommunityRequest;
CREATE PROCEDURE ApproveCommunityRequest(IN _id INT, IN _community INT)
	BEGIN
		DECLARE _newPermId INT;
		DECLARE _pid INT;	
		
		IF EXISTS(SELECT * FROM community_requests WHERE user_id = _id AND community = _community) THEN
			DELETE FROM community_requests 
			WHERE user_id = _id and community = _community;			
			
			-- Copy the default permission for the community 					
			SELECT default_permission FROM spaces WHERE id=_community INTO _pid;
			CALL CopyPermissions(_pid, _newPermId);
			
			INSERT INTO user_assoc(user_id, space_id, permission)
			VALUES(_id, _community, _newPermId);
			
			-- make the user a 'user' if they are currently 'unauthorized'
			IF EXISTS(SELECT email FROM user_roles WHERE email = (SELECT email FROM users WHERE users.id = _id) AND role = 'unauthorized') THEN
				UPDATE user_roles
				JOIN users ON users.email=user_roles.email
				SET role = 'user'
				WHERE users.id = _id;
			END IF;
		END IF;
	END //
	
-- Adds a new entry to pass_reset_request for a given user (also deletes previous
-- entries for the same user)
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddPassResetRequest;
CREATE PROCEDURE AddPassResetRequest(IN _id INT, IN _code VARCHAR(36))
	BEGIN
		IF EXISTS(SELECT * FROM pass_reset_request WHERE user_id = _id) THEN
			DELETE FROM pass_reset_request
			WHERE user_id = _id;
		END IF;
		INSERT INTO pass_reset_request(user_id, code, created)
		VALUES(_id, _code, SYSDATE());
	END //
	
-- Deletes a user's entry in INVITES, and if the user is unregistered
-- (i.e. has a role of 'unauthorized') then they are completely
-- deleted from the system
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS DeclineCommunityRequest;
CREATE PROCEDURE DeclineCommunityRequest(IN _id INT, IN _community INT)
	BEGIN
		DELETE FROM community_requests 
		WHERE user_id = _id and community = _community;

		DELETE users FROM users 
		JOIN user_roles ON user_roles.email=users.email
		WHERE users.id = _id
		AND role = 'unauthorized';
	END //
	
-- Returns the community request associated with given user id
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetCommunityRequestById;
CREATE PROCEDURE GetCommunityRequestById(IN _id INT)
	BEGIN
		SELECT *
		FROM community_requests
		WHERE user_id = _id;
	END //
	
-- Returns the community request associated with the given activation code
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetCommunityRequestByCode;
CREATE PROCEDURE GetCommunityRequestByCode(IN _code VARCHAR(36))
	BEGIN
		SELECT *
		FROM community_requests
		WHERE code = _code;
	END //

-- Looks for an activation code, and if successful, removes it from VERIFY,
-- then adds an entry to USER_ROLES
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RedeemActivationCode;
CREATE PROCEDURE RedeemActivationCode(IN _code VARCHAR(36), OUT _id INT)
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
DROP PROCEDURE IF EXISTS RedeemPassResetRequestByCode;
CREATE PROCEDURE RedeemPassResetRequestByCode(IN _code VARCHAR(36), OUT _id INT)
	BEGIN
		SELECT user_id INTO _id
		FROM pass_reset_request
		WHERE code = _code;
		DELETE FROM pass_reset_request
		WHERE code = _code;
	END //

-- Gets the number of community requests waiting approval
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetCommunityRequestCount;
CREATE PROCEDURE GetCommunityRequestCount()
	BEGIN
		SELECT count(*) AS requestCount
		FROM community_requests;
	END //

-- Gets the number of community requests waiting approval for the specified community.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetCommunityRequestCountForCommunity;
CREATE PROCEDURE GetCommunityRequestCountForCommunity(IN _communityId INT)
	BEGIN
		SELECT count(*) AS requestCount
		FROM community_requests
		WHERE community = _communityId;
	END //

-- Creates a change email request for user with _userId.
-- The email the the user is requesting to change to is _newEmail.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS AddChangeEmailRequest;
CREATE PROCEDURE AddChangeEmailRequest(IN _userId INT, IN _newEmail VARCHAR(64), IN _code VARCHAR(36))
	BEGIN
		INSERT INTO change_email_requests (user_id, new_email, code)
		VALUES (_userId, _newEmail, _code)
		ON DUPLICATE KEY UPDATE user_id=_userId, new_email=_newEmail, code=_code;
	END //


-- Gets a change email request for user with id _userId
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetChangeEmailRequest;
CREATE PROCEDURE GetChangeEmailRequest(IN _userId INT)
	BEGIN
		SELECT * FROM change_email_requests
		WHERE user_id=_userId;
	END //

-- Deletes the change email request associated with the user with id _userId.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS DeleteChangeEmailRequest;
CREATE PROCEDURE DeleteChangeEmailRequest(IN _userId INT)
	BEGIN
		DELETE FROM change_email_requests
		WHERE user_id=_userId;
	END //

DELIMITER ; -- This should always be at the end of this file
