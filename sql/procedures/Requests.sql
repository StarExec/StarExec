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
CREATE PROCEDURE AddCommunityRequest(IN _id INT, IN _community INT, IN _code VARCHAR(36), IN _message VARCHAR(300))
	BEGIN
		IF NOT EXISTS(SELECT * FROM user_assoc WHERE user_id = _id AND space_id = _community) THEN
			INSERT INTO community_requests(user_id, community, code, message, created)
			VALUES (_id, _community, _code, _message, SYSDATE());
		END IF;
	END //
	
-- Adds a request to reserve a queue
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS AddQueueRequest;
CREATE PROCEDURE AddQueueRequest(IN _userId INT, IN _spaceId INT, IN _queueName VARCHAR(64), IN _message VARCHAR(300), IN _wall INT, IN _cpu INT, OUT _id INT)
	BEGIN
		INSERT INTO queue_request(user_id, space_id, queue_name, message, created, clockTimeout,cpuTimeout)
		VALUES (_userId, _spaceId, _queueName, _message,SYSDATE(), _wall, _cpu);
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Associates a given date with an existing queue reservation request 
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS AssociateDateWithQueueRequest;
CREATE PROCEDURE AssociateDateWithQueueRequest(IN _request_id INT, IN _date DATE, IN _nodes INT)
	BEGIN
		INSERT IGNORE INTO queue_request_assoc (request_id, reserve_date, request_id)
		VALUES (_request_id, _date, _nodes);
	END //
	
-- Drops all of the queue_request_assoc entries for a given queue request
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS DeleteQueueRequestAssocEntries;
CREATE PROCEDURE DeleteQueueRequestAssocEntries(IN _request_id INT)
	BEGIN
		DELETE FROM queue_request_assoc
		WHERE request_id=_request_id;
	END //
	
DROP PROCEDURE IF EXISTS UpdateQueueRequest;
CREATE PROCEDURE UpdateQueueRequest(IN _id INT, IN _userId INT, IN _spaceId INT, IN _queueName VARCHAR(64), IN _message VARCHAR(300),  IN _created TIMESTAMP, IN _cpu INT, IN _wall INT)
	BEGIN
		UPDATE queue_request
		SET user_id=_userId,
		space_id=_spaceId,
		queue_name=_queueName,
		message=_message,
		created=_created,
		cpuTimeout=_cpu,
		clockTimeout=_wall
		WHERE id=_id;
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
	
	
-- Returns the queue request associated with the given id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetQueueRequestById;
CREATE PROCEDURE GetQueueRequestById(IN _id INT)
	BEGIN
		SELECT *
		FROM queue_request
		WHERE id=_id;
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
	
	
-- Removes the request with the given id from the queue reservation table
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS RemoveQueueReservation;
CREATE PROCEDURE RemoveQueueReservation(IN _id INT)
	BEGIN
		DELETE FROM queue_request 
		WHERE id = _id;
		
		DELETE FROM queue_request_assoc
		WHERE request_id=_id;
	END //
	

-- Gets the number of queue reservations waiting approval
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetQueueRequestCount;
CREATE PROCEDURE GetQueueRequestCount()
	BEGIN
		SELECT DISTINCT count(DISTINCT user_id, space_id, queue_name) AS requestCount
		FROM queue_request;
	END //
	
-- Gets the number of community requests waiting approval
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetCommunityRequestCount;
CREATE PROCEDURE GetCommunityRequestCount()
	BEGIN
		SELECT count(*) AS requestCount
		FROM community_requests;
	END //
	
-- Gets the number of queue reservations
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetQueueReservationCount;
CREATE PROCEDURE GetQueueReservationCount()
	BEGIN
		SELECT count(*) AS reservationCount
		FROM comm_queue;
	END //
	
-- Gets the number of historic queue reservations
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetHistoricReservationCount;
CREATE PROCEDURE GetHistoricReservationCount()
	BEGIN
		-- we do a left join and then get only the rows with "null," which are the entries for which no date is after
		-- or on today. In other words, we get reservations for which every date was in the past
		SELECT count(distinct id) AS reservationCount
		FROM queue_request
		LEFT JOIN queue_request_assoc ON (queue_request_assoc.request_id=queue_request.id AND CURDATE()>=reserve_date)
		WHERE queue_request_assoc.request_id IS NULL;
	END //
	
-- Deletes a queue reservation by removing it from comm_queue table
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS CancelQueueReservation;
CREATE PROCEDURE CancelQueueReservation(IN _queueId INT)
	BEGIN
		DELETE FROM comm_queue
		WHERE queue_id = _queueId;

	END //

-- Get All the queue reservations
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetAllQueueReservations;
CREATE PROCEDURE GetAllQueueReservations()
	BEGIN
		SELECT space_id, queue_id, MIN(node_count), MAX(node_count), MIN(reserve_date), MAX(reserve_date), message, cpuTimeout, clockTimeout
		FROM queue_request_assoc
		JOIN queue_requests ON queue_requests.id=queue_request_assoc.request_id
		GROUP BY space_id, queue_id;
	END //
		
-- Returns the nodeCount for a particular queue request on a particular date
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetRequestNodeCountOnDate;
CREATE PROCEDURE GetRequestNodeCountOnDate(IN _request_id INT, IN _reserveDate DATE)
	BEGIN
		SELECT node_count AS count
		FROM queue_request_assoc
		WHERE request_id = _request_id AND reserve_date = _reserveDate;
	END //

	
-- Returns the the space_id associated with a given queue_name
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetQueueRequestSpaceId;
CREATE PROCEDURE GetQueueRequestSpaceId ( IN _queueName VARCHAR(64)) 
	BEGIN
		SELECT DISTINCT space_id
		FROM queue_request
		WHERE queue_name = _queueName;
	END //
	
DROP PROCEDURE IF EXISTS GetQueueRequestForReservation;
CREATE PROCEDURE GetQueueRequestForReservation( IN _queueId INT)
	BEGIN
		SELECT space_id, queue_id, MAX(node_count), MIN(reserve_date), MAX(reserve_date), message, cpuTimeout, clockTimeout
		FROM queue_request_assoc
		JOIN queue_request ON  queue_request.id=queue_request_assoc.request_id
		WHERE queue_id = _queueId;
	END //
	
-- Gets the earliest end date of all reserved queues where the end date is after the given date
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetEarliestEndDate;
CREATE PROCEDURE GetEarliestEndDate(IN _date DATE)
	BEGIN
		SELECT min(endDate)
		FROM (
			SELECT MAX(reserve_date) AS endDate
			FROM queue_request_assoc
			JOIN queue_request ON queue_request.id=request_id
			WHERE reserve_date >= _date AND approved=true
			GROUP BY request_id
		) AS allEndDates;
	END //
	
-- Decreases the node count of a reservation by 1
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS DecreaseNodeCount;
CREATE PROCEDURE DecreaseNodeCount(IN _queueId INT)
	BEGIN
		UPDATE queue_request_assoc
		SET node_count = node_count - 1
		WHERE queue_id = _queueId
		AND node_count > 0;
	END //
	
	
DELIMITER ; -- This should always be at the end of this file